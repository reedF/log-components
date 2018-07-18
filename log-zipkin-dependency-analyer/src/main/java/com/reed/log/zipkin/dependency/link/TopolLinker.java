/*
 * Copyright 2015-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.reed.log.zipkin.dependency.link;

import static java.util.logging.Level.FINE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.reed.log.zipkin.dependency.utils.TagsContents;

import zipkin2.Span;
import zipkin2.Span.Kind;
import zipkin2.internal.Nullable;

/**
 * This parses a span tree into dependency links used by Web UI. Ex. http://zipkin/dependency
 *
 * <p>This implementation traverses the tree, and only creates links between {@link Kind#SERVER
 * server} spans. One exception is at the bottom of the trace tree. {@link Kind#CLIENT client} spans
 * that record their {@link Span#remoteEndpoint()} are included, as this accounts for uninstrumented
 * services. Spans with {@link Span#kind()} unset, but {@link Span#remoteEndpoint()} set are treated
 * the same as client spans.
 */
public final class TopolLinker {
	private final Logger logger;
	private final Map<Pair, Long> callCounts = new LinkedHashMap<>();
	private final Map<Pair, Long> errorCounts = new LinkedHashMap<>();
	private final Map<Pair, Double> costMean = new LinkedHashMap<>();
	// 分隔符,child=localServceName + "|" + name
	public static final String LINE = "|";
	// 分隔符, node name = traceType + ":" + url
	public static final String TRACETYPETAG = ":";
	// DB操作标识
	public static final String DB_READ = "DB-READ";

	public static final String DB_WRITE = "DB-WRITE";

	public TopolLinker() {
		this(Logger.getLogger(TopolLinker.class.getName()));
	}

	TopolLinker(Logger logger) {
		this.logger = logger;
	}

	static final Node.MergeFunction<Span> MERGE_RPC = new MergeRpc();

	static final class MergeRpc implements Node.MergeFunction<Span> {
		@Override
		public Span merge(@Nullable Span left, @Nullable Span right) {
			if (left == null)
				return right;
			if (right == null)
				return left;
			if (left.kind() == null) {
				return copyError(left, right);
			}
			if (right.kind() == null) {
				return copyError(right, left);
			}
			Span server = left.kind() == Kind.SERVER ? left : right;
			Span client = left.equals(server) ? right : left;
			if (server.remoteServiceName() != null) {
				return copyError(client, server);
			}
			return copyError(client, server).toBuilder().remoteEndpoint(client.localEndpoint()).build();
		}

		static Span copyError(Span maybeError, Span result) {
			String error = maybeError.tags().get("error");
			if (error != null) {
				return result.toBuilder().putTag("error", error).build();
			}
			return result;
		}
	}

	/**
	 * @param spans spans where all spans have the same trace id
	 */
	public TopolLinker putTrace(Iterator<Span> spans) {
		List<Span> list = new ArrayList<>();
		if (!spans.hasNext())
			return this;
		Span first = spans.next();
		list.add(first);
		if (logger.isLoggable(FINE))
			logger.fine("linking trace " + first.traceId());

		// Build a tree based on spanId and parentId values
		Node.TreeBuilder<Span> builder = new Node.TreeBuilder<>(logger, MERGE_RPC, first.traceId());
		builder.addNode(first.parentId(), first.id(), first);
		while (spans.hasNext()) {
			Span next = spans.next();
			list.add(next);
			builder.addNode(next.parentId(), next.id(), next);
		}

		Node<Span> tree = builder.build();

		if (logger.isLoggable(FINE))
			logger.fine("traversing trace tree, breadth-first");
		for (Iterator<Node<Span>> i = tree.traverse(); i.hasNext();) {
			Node<Span> current = i.next();
			if (current.isSyntheticRootForPartialTree()) {
				logger.fine("skipping synthetic node for broken span tree");
				continue;
			}
			Span currentSpan = current.value();
			if (currentSpan == null) {
				logger.fine("skipping null span in " + first.traceId());
				continue;
			}
			if (logger.isLoggable(FINE)) {
				logger.fine("processing " + currentSpan);
			}

			Kind kind = currentSpan.kind();
			if (Kind.CLIENT.equals(kind) && !current.children().isEmpty()) {
				logger.fine("deferring link to rpc child span");
				continue;
			}

			String serviceName = genNodeName(currentSpan, true);
			String remoteServiceName = genNodeName(currentSpan, false);
			if (kind == null) {
				// Treat unknown type of span as a client span if we know both
				// sides
				if (serviceName != null && remoteServiceName != null) {
					kind = Kind.CLIENT;
				} else {
					logger.fine("non-rpc span; skipping");
					continue;
				}
			}

			String child;
			String parent;
			switch (kind) {
			case SERVER:
			case CONSUMER:
				child = serviceName;
				parent = remoteServiceName;
				if (current == tree) { // we are the root-most span.
					if (parent == null) {
						logger.fine("root's peer is unknown; skipping");
						continue;
					}
				}
				break;
			case CLIENT:
			case PRODUCER:
				parent = serviceName;
				child = remoteServiceName;
				break;
			default:
				logger.fine("unknown kind; skipping");
				continue;
			}

			boolean isError = currentSpan.tags().containsKey("error");
			long duration = currentSpan.durationAsLong();
			if (kind == Kind.PRODUCER || kind == Kind.CONSUMER) {
				if (parent == null || child == null) {
					logger.fine("cannot link messaging span to its broker; skipping");
				} else {
					addLink(parent, child, isError, duration);
				}
				continue;
			}

			if (logger.isLoggable(FINE) && parent == null) {
				logger.fine("cannot determine parent, looking for first server ancestor");
			}

			Span rpcAncestor = findRpcAncestor(current);
			String rpcAncestorName;
			if (rpcAncestor != null && (rpcAncestorName = genNodeName(rpcAncestor, true)) != null) {
				// Some users accidentally put the remote service name on client
				// annotations.
				// Check for this and backfill a link from the nearest remote to
				// that service as necessary.
				if (kind == Kind.CLIENT && serviceName != null && !rpcAncestorName.equals(serviceName)) {
					logger.fine("detected missing link to client span");
					addLink(rpcAncestorName, serviceName, false, duration); // we
																			// don't
					// know if
					// there's
					// an error
					// here
				}

				// Local spans may be between the current node and its remote
				// parent
				if (parent == null)
					parent = rpcAncestorName;

				// When an RPC is split between spans, we skip the child (server
				// side). If our parent is a
				// client, we need to check it for errors.
				if (!isError && Kind.CLIENT.equals(rpcAncestor.kind()) && currentSpan.parentId() != null
						&& currentSpan.parentId().equals(rpcAncestor.id())) {
					isError = rpcAncestor.tags().containsKey("error");
				}
			}

			if (parent == null || child == null) {
				logger.fine("cannot find server ancestor; skipping");
				continue;
			}

			addLink(parent, child, isError, duration);
		}
		return this;
	}

	String genNodeName(Span span, boolean islocal) {
		String s = null;
		if (span != null) {
			if (islocal) {
				s = span.localServiceName();
			} else {
				s = span.remoteServiceName();
			}
			if (StringUtils.isNotBlank(s) && span.tags() != null) {
				String traceType = span.tags().get(TagsContents.TRACE_TYPE);
				if (!span.tags().containsKey(TagsContents.SQL)) {
					s = s + LINE + traceType + TRACETYPETAG + span.name();
				} else {
					// 针对DB类型的span，每次name不同（select,update,insert等），使用DB_*，统一name为数据库操作
					String sql = span.name();
					if (sql != null) {
						sql = sql.toLowerCase();
						if (sql.startsWith("update") || sql.startsWith("insert")) {
							sql = DB_WRITE;
						} else {
							sql = DB_READ;
						}
					}
					s = s + LINE + traceType + TRACETYPETAG + sql;
				}
			}
		}
		return s;
	}

	Span findRpcAncestor(Node<Span> current) {
		Node<Span> ancestor = current.parent();
		while (ancestor != null) {
			if (logger.isLoggable(FINE)) {
				logger.fine("processing ancestor " + ancestor.value());
			}
			if (!ancestor.isSyntheticRootForPartialTree()) {
				Span maybeRemote = ancestor.value();
				if (maybeRemote.kind() != null)
					return maybeRemote;
			}
			ancestor = ancestor.parent();
		}
		return null;
	}

	void addLink(String parent, String child, boolean isError, long duration) {
		if (logger.isLoggable(FINE)) {
			logger.fine("incrementing " + (isError ? "error " : "") + "link " + parent + " -> " + child);
		}
		Pair key = new Pair(parent, child);
		if (callCounts.containsKey(key)) {
			callCounts.put(key, callCounts.get(key) + 1);
		} else {
			callCounts.put(key, 1L);
		}
		if (duration > 0) {
			// cost
			if (costMean.containsKey(key)) {
				costMean.put(key, (costMean.get(key) + duration) / 2.00);
			} else {
				costMean.put(key, duration / 1.00);
			}
		}

		if (!isError)
			return;
		if (errorCounts.containsKey(key)) {
			errorCounts.put(key, errorCounts.get(key) + 1);
		} else {
			errorCounts.put(key, 1L);
		}
	}

	public List<TopolLink> link() {
		return link(callCounts, errorCounts, costMean);
	}

	/** links are merged by mapping to parent/child and summing corresponding links */
	public static List<TopolLink> merge(Iterable<TopolLink> in) {
		Map<Pair, Long> callCounts = new LinkedHashMap<>();
		Map<Pair, Long> errorCounts = new LinkedHashMap<>();
		Map<Pair, Double> costMean = new LinkedHashMap<>();

		for (TopolLink link : in) {
			Pair parentChild = new Pair(link.parent(), link.child());
			long callCount = callCounts.containsKey(parentChild) ? callCounts.get(parentChild) : 0L;
			callCount += link.callCount();
			callCounts.put(parentChild, callCount);
			long errorCount = errorCounts.containsKey(parentChild) ? errorCounts.get(parentChild) : 0L;
			errorCount += link.errorCount();
			errorCounts.put(parentChild, errorCount);

			double cost = costMean.containsKey(parentChild) ? costMean.get(parentChild) : 0.00;
			cost = (cost + link.cost) / 2.00;
			costMean.put(parentChild, cost);
		}

		return link(callCounts, errorCounts, costMean);
	}

	static List<TopolLink> link(Map<Pair, Long> callCounts, Map<Pair, Long> errorCounts, Map<Pair, Double> costMean) {
		List<TopolLink> result = new ArrayList<>(callCounts.size());
		for (Map.Entry<Pair, Long> entry : callCounts.entrySet()) {
			Pair parentChild = entry.getKey();
			result.add(TopolLink.newBuilder().parent(parentChild.left).child(parentChild.right)
					//
					.callCount(entry.getValue())
					.errorCount(errorCounts.containsKey(parentChild) ? errorCounts.get(parentChild) : 0L)
					.cost(costMean.containsKey(parentChild) ? costMean.get(parentChild) : 0.00)
					//
					.build());
		}
		return result;
	}

	static final class Pair {
		final String left, right;

		Pair(String left, String right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Pair))
				return false;
			Pair that = (TopolLinker.Pair) o;
			return left.equals(that.left) && right.equals(that.right);
		}

		@Override
		public int hashCode() {
			int h$ = 1;
			h$ *= 1000003;
			h$ ^= left.hashCode();
			h$ *= 1000003;
			h$ ^= right.hashCode();
			return h$;
		}
	}
}

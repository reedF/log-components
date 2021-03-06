package com.reed.log.zipkin.dependency.link;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * 调用依赖关系
 * extends DependencyLink
 * 仿照DependencyLink，重写DependencyLink、DependencyLinker，添加计算方法级QPS,耗时
 * @author reed
 *
 */
public class TopolLink implements Serializable {
	static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final long serialVersionUID = 0L;

	public static Builder newBuilder() {
		return new Builder();
	}

	/** parent service name (caller) */
	public String parent() {
		return parent;
	}

	/** child service name (callee) */
	public String child() {
		return child;
	}

	public String name() {
		return name;
	}

	/** total traced calls made from {@link #parent} to {@link #child} */
	public long callCount() {
		return callCount;
	}

	/** How many {@link #callCount calls} are known to be errors */
	public long errorCount() {
		return errorCount;
	}

	public double qps() {
		return qps;
	}

	public double cost() {
		return cost;
	}

	public long timestamp() {
		return timestamp;
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static final class Builder {
		String parent, child, name;
		long callCount, errorCount, timestamp;
		double qps, cost;

		Builder() {
		}

		Builder(TopolLink source) {
			this.parent = source.parent;
			this.child = source.child;
			this.name = source.name;
			this.callCount = source.callCount;
			this.errorCount = source.errorCount;
			this.qps = source.qps;
			this.cost = source.cost;
			this.timestamp = source.timestamp;
		}

		public Builder parent(String parent) {
			if (parent == null)
				throw new NullPointerException("parent == null");
			this.parent = parent.toLowerCase(Locale.ROOT);
			return this;
		}

		public Builder child(String child) {
			if (child == null)
				throw new NullPointerException("child == null");
			this.child = child.toLowerCase(Locale.ROOT);
			return this;
		}

		public Builder name(String name) {
			if (name == null)
				throw new NullPointerException("name == null");
			this.name = name.toLowerCase(Locale.ROOT);
			return this;
		}

		public Builder callCount(long callCount) {
			this.callCount = callCount;
			return this;
		}

		public Builder errorCount(long errorCount) {
			this.errorCount = errorCount;
			return this;
		}

		public Builder qps(double qps) {
			this.qps = qps;
			return this;
		}

		public Builder cost(double cost) {
			this.cost = cost;
			return this;
		}

		public Builder timestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public TopolLink build() {
			String missing = "";
			if (parent == null)
				missing += " parent";
			if (child == null)
				missing += " child";
			if (!"".equals(missing))
				throw new IllegalStateException("Missing :" + missing);
			return new TopolLink(this);
		}
	}

	@Override
	public String toString() {
		return new String(TopolLinkBytesEncoder.JSON_V1.encode(this), UTF_8);
	}

	// clutter below mainly due to difficulty working with Kryo which cannot
	// handle AutoValue subclass
	// See https://github.com/openzipkin/zipkin/issues/1879
	final String parent, child, name;
	final long callCount, errorCount, timestamp;
	final double qps, cost;

	TopolLink(Builder builder) {
		parent = builder.parent;
		child = builder.child;
		name = builder.name;
		callCount = builder.callCount;
		errorCount = builder.errorCount;
		qps = builder.qps;
		cost = builder.cost;
		timestamp = builder.timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof TopolLink))
			return false;
		TopolLink that = (TopolLink) o;
		return (parent.equals(that.parent)) && (child.equals(that.child)) && (name.equals(that.name))
				&& (callCount == that.callCount) && (errorCount == that.errorCount) && (cost == that.cost)
				&& (qps == that.qps) && (timestamp == that.timestamp);
	}

	@Override
	public int hashCode() {
		int h = 1;
		h *= 1000003;
		h ^= parent.hashCode();
		h *= 1000003;
		h ^= child.hashCode();
		h *= 1000003;
		h ^= name.hashCode();
		h *= 1000003;
		h ^= (int) ((callCount >>> 32) ^ callCount);
		h *= 1000003;
		h ^= (int) ((errorCount >>> 32) ^ errorCount);
		h *= 1000003;
		h ^= String.valueOf(qps).hashCode();
		h *= 1000003;
		h ^= String.valueOf(cost).hashCode();
		h *= 1000003;
		h ^= (int) ((timestamp >>> 32) ^ timestamp);
		return h;
	}

	// This is an immutable object, and our encoder is faster than java's: use a
	// serialization proxy.
	final Object writeReplace() throws ObjectStreamException {
		return new SerializedForm(TopolLinkBytesEncoder.JSON_V1.encode(this));
	}

	private static final class SerializedForm implements Serializable {
		private static final long serialVersionUID = 0L;

		final byte[] bytes;

		SerializedForm(byte[] bytes) {
			this.bytes = bytes;
		}

		Object readResolve() throws ObjectStreamException {
			try {
				return TopolLinkBytesDecoder.JSON_V1.decodeOne(bytes);
			} catch (IllegalArgumentException e) {
				throw new StreamCorruptedException(e.getMessage());
			}
		}
	}
}
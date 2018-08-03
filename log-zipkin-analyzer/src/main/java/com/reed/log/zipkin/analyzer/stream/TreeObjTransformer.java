package com.reed.log.zipkin.analyzer.stream;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.reed.log.zipkin.analyzer.pojo.TagsContents;
import com.reed.log.zipkin.analyzer.pojo.ZipkinLog;
import com.reed.log.zipkin.analyzer.tree.TreeObj;
import com.reed.log.zipkin.analyzer.tree.TreeParser;

/**
 * 根据TraceID合并同traceId下的全部Span为跟踪链，构造一个链为树
 * 为下游提供一次全链路请求键值对(K:traceId,V:TreeObj 包括链路内全部Span)，下游可继续根据跟踪链路标识（即树的层级结构）分组，将不同traceId但链路标识相同的TreeObj分到同组，统计链路内各级请求
 * @author reed
 *
 */
public class TreeObjTransformer implements Transformer<String, String, KeyValue<String, TreeObj>> {

	public static Logger logger = LoggerFactory.getLogger(TreeObjTransformer.class);
	private ProcessorContext context;
	private KeyValueStore<String, Bytes> state;

	@Override
	public void init(ProcessorContext context) {
		this.context = context;
		this.state = (KeyValueStore<String, Bytes>) context.getStateStore(KafkaStreamsConfig.storesName);
		this.context.schedule(5000); // call #punctuate() each 1000ms
	}

	@Override
	public KeyValue<String, TreeObj> transform(String key, String value) {
		List<KeyValue<String, TreeObj<ZipkinLog>>> r = new ArrayList<>();
		List<ZipkinLog> data = null;
		Map<String, List<TreeObj>> trees = new HashMap<>();
		try {
			data = JSON.parseArray(value, ZipkinLog.class);
		} catch (JSONException e) {
			logger.error("JSON parse error:{},msg is:{}", e.getMessage(), value);
		}

		if (data != null && !data.isEmpty()) {
			for (ZipkinLog msg : data) {
				if (msg != null && (msg.getTags() == null || !msg.getTags().containsKey(TagsContents.SQL))) {
					// make http method name
					if (msg.getTags() != null && msg.getTags().containsKey(TagsContents.HTTP_PATH)) {
						String path = msg.getTags().get(TagsContents.HTTP_PATH);
						if (StringUtils.isNotBlank(path)) {
							msg.setName(path);
						}
					}
					key = msg.getTraceId();
					// String app = msg.getLocalEndpoint() != null ?
					// msg.getLocalEndpoint().getServiceName() : null;
					String app = (msg.getRemoteEndpoint() == null || msg.getRemoteEndpoint().getServiceName() == null)
							? msg.getLocalEndpoint().getServiceName() : msg.getRemoteEndpoint().getServiceName();
					if (app == null) {
						logger.info("---------" + msg.toString());
					}
					List<TreeObj> stored = initFromStore(key);
					stored = stored != null ? stored : new ArrayList<>();
					TreeObj<ZipkinLog> t = new TreeObj<>();
					t.setId(msg.getId());
					t.setApp(app);
					t.setName(msg.getParentId() != null ? (msg.getKind() + KafkaStreamsConfig.M + msg.getName())
							: msg.getName());
					// t.setName(msg.getName());
					t.setParentId(msg.getParentId() != null ? msg.getParentId() : "-1");
					t.setBiz(msg);

					if (!stored.contains(t)) {
						stored.add(t);
					}

					// this.state.put(key, JSON.toJSONString(stored));
					this.state.put(key, Bytes.wrap(JSON.toJSONBytes(stored)));
					trees.put(key, stored);
				}
			}
		}
		// 合并相同traceId的span为完整的跟踪树并发送---后期改为punctuate 定时发送
		if (!trees.isEmpty()) {
			for (Map.Entry<String, List<TreeObj>> entry : trees.entrySet()) {
				if (entry != null) {
					List<TreeObj> roots = TreeParser.getTreeList("-1", entry.getValue());
					roots.forEach(v -> {
						this.context.forward(entry.getKey(), v);
					});
				}
			}
		}

		return null;

	}

	/**
	 * 定时发送
	 */
	@Override
	public KeyValue<String, TreeObj> punctuate(long timestamp) {
		// TODO 缓存一定时间的log，以合并相同traceId的span为完整的跟踪树
		// 停止使用，此情况下存在消息消费不及时，堆积的问题，导致“*-repartition”类的topic的consumer出现timeout而进程退出，持续一段时间后所有consumer都shutdown，将无消费
		// send();
		return null;
	}

	@Override
	public void close() {

	}

	private List<TreeObj> initFromStore(String key) {
		List<TreeObj> r = null;
		// r = JSON.parseArray(this.state.get(key), TreeObj.class);
		if (this.state.get(key) != null) {
			Type type = new TypeReference<List<TreeObj>>() {
			}.getType();
			r = JSON.parseObject(this.state.get(key).get(), type);
		}
		return r;
	}

	/**
	 * 从store内获取缓存的span集合并合并为完整跟踪树
	 * @return
	 */
	private Map<String, List<TreeObj>> getAllFromStore() {
		Map<String, List<TreeObj>> trees = new HashMap<>();
		KeyValueIterator<String, Bytes> iterator = this.state.all();
		if (iterator != null) {
			iterator.forEachRemaining(entry -> {
				if (entry != null) {
					Type type = new TypeReference<List<TreeObj>>() {
					}.getType();
					List<TreeObj> r = TreeParser.getTreeList("-1", JSON.parseObject(entry.value.get(), type));
					if (r != null && !r.isEmpty()) {
						trees.put(entry.key, r);
					}
				}
			});
		}
		return trees;
	}

	private void send() {
		Map<String, List<TreeObj>> trees = getAllFromStore();
		if (!trees.isEmpty()) {
			for (Map.Entry<String, List<TreeObj>> entry : trees.entrySet()) {
				if (entry != null) {
					entry.getValue().forEach(v -> {
						this.context.forward(entry.getKey(), v);
					});
				}
			}
		}
	}
}

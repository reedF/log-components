package com.reed.log.analyzer.kafka.stream;

import java.util.Map;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reed.log.analyzer.kafka.MsgConstants;
import com.reed.log.analyzer.kafka.MsgUtil;

public class UriCountProcessorSupplier implements ProcessorSupplier<String, String> {
	private static Logger logger = LoggerFactory.getLogger(UriCountProcessorSupplier.class);

	@Override
	public Processor get() {
		return new UriCountProcessor();
	}

	private class UriCountProcessor implements Processor<String, String> {

		private ProcessorContext context;

		private KeyValueStore<String, Integer> kvStore;

		@Override
		public void init(ProcessorContext context) {
			this.context = context;
			this.context.schedule(10000);
			this.kvStore = (KeyValueStore<String, Integer>) context.getStateStore(KafkaStreamsConfig.stores);
		}

		@Override
		public void process(String key, String v) {
			//logger.info(v);
			Map<String, Object> msg = MsgUtil.msg2Map(v);
			String appName = MsgUtil.getAppName(msg);
			Map<String, Object> data = MsgUtil.getData(msg);
			String uri = MsgUtil.getFieldValue(data, MsgConstants.URI);
			String k = appName + "-" + uri;
			Integer oldValue = this.kvStore.get(k);
			if (oldValue == null) {
				this.kvStore.put(k, 1);
			} else {
				this.kvStore.put(k, oldValue + 1);
			}

		}

		/**
		 * Perform any periodic operations
		 * 
		 * @param timestamp
		 */
		@Override
		public void punctuate(long timestamp) {
			try (KeyValueIterator<String, Integer> itr = this.kvStore.all()) {
				while (itr.hasNext()) {
					KeyValue<String, Integer> entry = itr.next();
					logger.info("[========" + entry.key + ", " + entry.value + "==========]");
					context.forward(entry.key, entry.value.toString());
				}
				context.commit();
			}
		}

		@Override
		public void close() {
			this.kvStore.close();
		}
	}
}
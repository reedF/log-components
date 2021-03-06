package com.reed.log.analyzer.kafka;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import com.reed.log.analyzer.kafka.stream.KafkaStreamsConfig;
import com.reed.log.analyzer.metric.MetricService;

/**
 * 默认消费kafka topic全部partition,多个实例时，会在实例间自动分配partition消费
 */
public class KafkaLogListener {

	private static Logger logger = LoggerFactory.getLogger(KafkaLogListener.class);

	@Autowired
	private KafkaTemplate<String, String> template;
	@Autowired
	private MetricService metric;

	public void send(String topic, String key, String data) {
		template.send(topic, key, data);
	}

	@KafkaListener(id = "test", topics = KafkaStreamsConfig.stores)
	public void listen(ConsumerRecord<?, ?> cr) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		logger.info("{} - {} : {}  - {}", cr.topic(), cr.key(), cr.value(), formatter.format(cr.timestamp()));

	}

	@KafkaListener(topics = "${kafka.topic}", containerFactory = "batchFactory")
	public void listenBatch(List<ConsumerRecord<?, ?>> list) {
		list.forEach(s -> {
			// logger.info("batch======>{}", s.toString());
			Map<String, Object> msg = MsgUtil.msg2Map(s.value());
			String appName = MsgUtil.getAppName(msg);
			Map<String, Object> data = MsgUtil.getData(msg);
			String uri = MsgUtil.getFieldValue(data, MsgConstants.URI);
			int cost = MsgUtil.getFieldValue(data, MsgConstants.COST);
			int status = MsgUtil.getFieldValue(data, MsgConstants.STATUS);
			Map<String, Object> biz = MsgUtil.getBusinessData(data);
			String code = MsgUtil.getFieldValue(biz, MsgConstants.CODE);
			metric.meter(appName, uri);
			metric.meterError(appName, uri, status);
			metric.meterCode(appName, uri, code);
			metric.histogramCost(appName, uri, cost);
		});
	}

}
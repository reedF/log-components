package com.reed.log.analyzer.kafka;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

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

	// @KafkaListener(id = "test", topics = "logs")
	public void listen(ConsumerRecord<?, ?> cr) {
		logger.info("{} - {} : {}", cr.topic(), cr.key(), cr.value());

	}

	@KafkaListener(topics = "logs", containerFactory = "batchFactory")
	public void listenBatch(List<ConsumerRecord<?, ?>> list) {
		list.forEach(s -> {
			// logger.info("batch======>{}", s.toString());
			Map<String, Object> data = MsgUtil.getData(MsgUtil.msg2Map(s.value()));
			String uri = MsgUtil.getFieldValue(data, MsgConstants.URI);
			int cost = MsgUtil.getFieldValue(data, MsgConstants.COST);
			int status = MsgUtil.getFieldValue(data, MsgConstants.STATUS);
			Map<String, Object> biz = MsgUtil.getBusinessData(data);
			Integer code = MsgUtil.getFieldValue(biz, MsgConstants.CODE);
			metric.meter(uri);
			metric.meterError(uri, status);
			metric.meterCode(uri, code);
			metric.histogramCost(uri, cost);
		});
	}

}
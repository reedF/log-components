package com.reed.log.jobs;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.values.PCollection;

import com.reed.log.beam.BaseBeam;
import com.reed.log.model.KafkaMsg;

import lombok.extern.slf4j.Slf4j;

/**
 * kafka to es
 */
@Slf4j
public class KafkaToEsJob extends BaseBeam {

	public static void main(String[] args) {
		KafkaToEsJob job = new KafkaToEsJob();
		log.info("========Job started======");

		Pipeline pipeline = initPipeline(args);
		runningJob(pipeline, job);
		executePipeline(pipeline);

		log.info("========Job end======");
	}

	@Override
	public void doBusiness(Pipeline pipeline) {
		log.info("========Job business begin......=========");
		PCollection<KafkaMsg> streams = readFromKafka(pipeline,KafkaMsg.class);
		logMsg(streams);
	}

}

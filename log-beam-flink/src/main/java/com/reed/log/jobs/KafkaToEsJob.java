package com.reed.log.jobs;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import com.reed.log.beam.BaseBeam;
import com.reed.log.common.JobConfig;
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
		
		log.info("========Job end======");
	}

	@Override
	public void doBusiness(Pipeline pipeline) {
		log.info("========Job business begin......=========");
		PCollection<KafkaMsg> streams = readFromKafka(pipeline, KafkaMsg.class);
		logMsg(streams);
		// business
		PCollection<KV<String, KafkaMsg>> datas = businessLogic(streams);
		// format and write
		PCollection<KV<String, String>> result = formatResult(datas);
		// writeToKafka(result, KafkaUtils.borkers, KafkaUtils.topic_result);
		writeToEs(result, JobConfig.getEsCluster(), JobConfig.getEsIndexName(), "test");
	}

	/**
	 * 业务逻辑,testing
	 * 注：必须是static方法，否则会报：unable to serialize DoFnAndMainOutput{doFn=com.reed.log.jobs.KafkaToEsJob$1@51e4ccb3, mainOutputTag=Tag<output>}
	 * @param streams
	 */
	@SuppressWarnings("serial")
	public static PCollection<KV<String, KafkaMsg>> businessLogic(PCollection<KafkaMsg> streams) {
		return streams.apply(ParDo.of(new DoFn<KafkaMsg, KV<String, KafkaMsg>>() {
			@ProcessElement
			public void processElement(ProcessContext c) {
				KV<String, KafkaMsg> kv = KV.<String, KafkaMsg>of(c.element().getMsgKey(), c.element());
				c.output(kv);
			}
		}));
	}
}

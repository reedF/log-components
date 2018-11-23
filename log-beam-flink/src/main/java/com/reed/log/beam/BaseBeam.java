package com.reed.log.beam;

import java.io.IOException;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

import com.reed.log.common.JobConfig;
import com.reed.log.common.JobOptions;

import lombok.extern.slf4j.Slf4j;

/**
 * beam基础类，定义公共方法
 * @author reed
 *
 */
@Slf4j
public abstract class BaseBeam {

	/**
	 * 业务逻辑
	 * @param pipeline
	 */
	public abstract void doBusiness(Pipeline pipeline);

	public static void runningJob(Pipeline pipeline, BaseBeam beam) {
		pipeline.getOptions().setJobName(beam.getClass().getName());
		beam.doBusiness(pipeline);
	}

	public static Pipeline initPipeline(String[] args) {
		JobOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(JobOptions.class);
		options.setRunner(JobConfig.getRunner());
		return Pipeline.create(options);
	}

	public static void executePipeline(Pipeline pipeline) {
		// execute beam pipeline
		PipelineResult result = pipeline.run();
		try {
			result.waitUntilFinish();
		} catch (Exception exception) {
			try {
				result.cancel();
			} catch (IOException e) {
				log.error("======Job running error:{}======", e.getMessage());
			}
		} finally {
			log.info("======Job stop======");
		}
	}

}

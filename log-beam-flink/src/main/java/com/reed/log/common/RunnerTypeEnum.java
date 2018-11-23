package com.reed.log.common;

import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.runners.flink.FlinkRunner;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.PipelineRunner;

/**
 * beam runner
 * @author reed
 *
 */
public enum RunnerTypeEnum {

	Direct(JobConfig.RUNNER_TYPE_DIRECT, DirectRunner.class), Flink(JobConfig.RUNNER_TYPE_FLINK, FlinkRunner.class);

	private String name;

	private Class<PipelineRunner<PipelineResult>> runner;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private RunnerTypeEnum(String name, Class runner) {
		this.name = name;
		this.runner = runner;
	}

	public static Class<PipelineRunner<PipelineResult>> getRunner(String name) {
		for (RunnerTypeEnum c : RunnerTypeEnum.values()) {
			if (c.getName().equals(name)) {
				return c.runner;
			}
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public Class<PipelineRunner<PipelineResult>> getRunner() {
		return runner;
	}

}

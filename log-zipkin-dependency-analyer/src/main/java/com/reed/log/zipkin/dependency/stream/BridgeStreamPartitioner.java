package com.reed.log.zipkin.dependency.stream;

import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.processor.StreamPartitioner;

/**
 * 根据key指定topic 分区
 * @author reed
 *
 */
public class BridgeStreamPartitioner implements StreamPartitioner<String, String> {

	@Override
	public Integer partition(String key, String value, int numPartitions) {
		int r = new Random().nextInt(numPartitions);
		if (StringUtils.isNotBlank(key)) {
			r = Utils.toPositive(Utils.murmur2(key.getBytes())) % numPartitions;
		}
		return r;
	}

}

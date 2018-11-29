package com.reed.log.es;

import org.apache.beam.sdk.io.elasticsearch.ElasticsearchIO;
import org.apache.beam.sdk.io.elasticsearch.ElasticsearchIO.ConnectionConfiguration;

import com.reed.log.common.JobConfig;

/**
 * es setting
 * @author reed
 *
 */
public class EsConfig {

	public static final String M = "-";

	public static ConnectionConfiguration initEsConfig(String[] esCluster, String indexDate, String indexType) {
		String indexName = JobConfig.getEsIndexName() + M + indexDate;
		return ElasticsearchIO.ConnectionConfiguration.create(esCluster, indexName, indexType);
	}
}

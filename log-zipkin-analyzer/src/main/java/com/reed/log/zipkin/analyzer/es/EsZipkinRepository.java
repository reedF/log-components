package com.reed.log.zipkin.analyzer.es;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

@Component
public interface EsZipkinRepository extends ElasticsearchRepository<EsZipkin, Long> {

	List<EsZipkin> findByTraceIdAndTypeAndApp(String traceId, String type, String app);
}

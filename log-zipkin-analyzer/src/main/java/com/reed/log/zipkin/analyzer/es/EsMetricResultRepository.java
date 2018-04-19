package com.reed.log.zipkin.analyzer.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

@Component
public interface EsMetricResultRepository extends ElasticsearchRepository<EsMetricResult, Long> {

}

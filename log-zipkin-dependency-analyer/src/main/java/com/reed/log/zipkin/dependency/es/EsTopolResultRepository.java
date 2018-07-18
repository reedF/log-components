package com.reed.log.zipkin.dependency.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

@Component
public interface EsTopolResultRepository extends ElasticsearchRepository<EsTopolResult, String> {

}

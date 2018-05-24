package com.reed.log.zipkin.analyzer.alarm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

@Component
public interface AlarmItemRepository extends ElasticsearchRepository<AlarmItem, Long> {

	Page<AlarmItem> findByEnable(Boolean enable, Pageable pageable);

	Page<AlarmItem> findDistinctByTitle(String title, Pageable pageable);
}

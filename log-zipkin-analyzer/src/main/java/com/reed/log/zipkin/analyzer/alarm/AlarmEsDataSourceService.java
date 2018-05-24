package com.reed.log.zipkin.analyzer.alarm;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.reed.log.zipkin.analyzer.alarm.utils.SendEmailService;
import com.reed.log.zipkin.analyzer.alarm.utils.SendSMSService;
import com.reed.log.zipkin.analyzer.es.EsZipkin;
import com.reed.log.zipkin.analyzer.es.EsZipkinRepository;

/**
 * Es 数据源报警服务实现
 * @author reed
 *
 */
@Service
public class AlarmEsDataSourceService extends AlarmBaseService<EsZipkin> implements AlarmService<EsZipkin> {

	public static Logger logger = LoggerFactory.getLogger(AlarmEsDataSourceService.class);
	@Autowired
	private SendEmailService emailService;
	@Autowired
	private SendSMSService smsService;
	@Autowired
	private EsZipkinRepository esZipkinRepository;

	@Autowired
	private ElasticsearchTemplate esAlarmTemplate;

	/**
	 * 查询去重
	 */
	public Page<AlarmItem> findDistinctByTitle(String title, Pageable page) {
		String field = "title.keyword";
		String aggName = "title-agg";
		String top = "top";
		Page<AlarmItem> r = null;
		QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
		if (StringUtils.isNotBlank(title)) {
			queryBuilder = QueryBuilders.termQuery(field, title);
		}
		// CardinalityAggregationBuilder agg =
		// AggregationBuilders.cardinality("title-agg").field("title.keyword");
		SearchQuery sq = new NativeSearchQueryBuilder().withQuery(queryBuilder).withPageable(page)
				.addAggregation(AggregationBuilders.terms(aggName).field(field).subAggregation(AggregationBuilders
						.topHits(top).explain(true).from(page.getPageNumber() * page.getPageSize()).size(1)))
				.build();
		r = (AggregatedPage<AlarmItem>) esAlarmTemplate.queryForPage(sq, AlarmItem.class, new SearchResultMapper() {
			@SuppressWarnings({ "hiding", "unchecked" })
			@Override
			public <AlarmItem> AggregatedPage<AlarmItem> mapResults(SearchResponse response, Class<AlarmItem> clazz,
					Pageable pageable) {
				List<AlarmItem> values = new ArrayList<>();
				Terms agg = response.getAggregations().get(aggName);
				for (Terms.Bucket entry : agg.getBuckets()) {
					TopHits topHits = entry.getAggregations().get(top);
					for (SearchHit hit : topHits.getHits()) {
						values.add((AlarmItem) hit.getSource());
					}
				}
				return new AggregatedPageImpl<AlarmItem>((List<AlarmItem>) values);
			}
		});

		return r;
	}

	@Override
	public List<EsZipkin> getDatasByCondition(AlarmItem alarm, Pageable page) {
		List<EsZipkin> r = null;
		if (alarm != null && StringUtils.isNotBlank(alarm.getCondition())) {
			String start = Long2Date(System.currentTimeMillis() - alarm.getFrequency() * 1000);
			BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
			String[] dsl = alarm.getCondition().split(",");
			if (dsl != null && dsl.length > 0) {
				for (String s : dsl) {
					if (StringUtils.isNotBlank(s)) {
						queryBuilder.must(QueryBuilders.wrapperQuery(s));
					}
				}
			}
			queryBuilder.filter(QueryBuilders.rangeQuery("createTime").gte(start));
			Page<EsZipkin> pages = esZipkinRepository.search(queryBuilder, page);
			r = pages.getContent();
		}
		return r;
	}

	@Override
	public void doActions(List<EsZipkin> datas, AlarmItem alarm) {
		if (datas != null && alarm != null && alarm.getActions() != null) {
			for (AlarmAction action : alarm.getActions()) {
				switch (action.getType().toUpperCase()) {
				case "EMAIL":
					emailService.sendSimpleMail(action.getFrom(), action.getTo(), action.getSubject(), action.getMsg());
					break;
				case "SMS":
					smsService.sendSms(action.getTo(), action.getMsg());
					break;
				case "WECHAT":
					// TODO
					break;
				default:
					logger.info("<<<<<<<<<<DEFAULT ACTION:{},{}>>>>>>>>>>>>>", alarm.getTitle(), action.getType());
					break;
				}
			}
			logger.info("<<<<<<<<<<Alarm action done:{},{}>>>>>>>>>>>", alarm.getTitle(), alarm.getDataSourceType());
		}
	}

	public String Long2Date(Long timestamp) {
		String r = null;
		if (timestamp != null) {
			// 转换日期
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
			r = dateFormat.format(new Date(timestamp));
		}
		return r;
	}

}

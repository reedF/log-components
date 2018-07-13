package test.zipkin.log.es;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import com.reed.log.zipkin.analyzer.AnalyzerApplication;
import com.reed.log.zipkin.analyzer.es.EsMetricResult;
import com.reed.log.zipkin.analyzer.metric.MetricObj;

@RunWith(MySpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { AnalyzerApplication.class })
public class TestEsService {

	public static final String appFileld = "app";
	public static final String nameFileld = "name";
	public static final String spanNameFileld = "spans.name.keyword";
	public static final String pidFileld = "spans.pid";
	public static final String qpsFileld = "spans.qps";
	public static final String costFileld = "spans.cost";
	public static final String spanIdFileld = "spans.id";
	public static final String timeFileld = "createTime";
	public static final String agg = "agg-";

	public static final String testData = "shopcenter-crmapi";

	@Autowired
	private ElasticsearchTemplate template;

	@Test
	public void testNestedQuery() {
		long endTime = System.currentTimeMillis();
		long startTime = endTime - 60 * 60 * 1000;
		Assert.assertNotNull(nestedQuery(testData, startTime, endTime, PageRequest.of(0, 10)));
	}

	@Test
	public void testAgg() {
		long endTime = System.currentTimeMillis();
		long startTime = endTime - 60 * 60 * 1000;
		Assert.assertNotNull(aggSearch(testData, startTime, endTime, PageRequest.of(0, 10)));
	}

	@Test
	public void testNestedAgg() {
		long endTime = System.currentTimeMillis();
		long startTime = endTime - 60 * 60 * 1000;
		Assert.assertNotNull(nestedAggSearch(testData, startTime, endTime, PageRequest.of(0, 10)));
	}

	/**
	 * 嵌套查询（nestedQuery），可对nested类型的字段进行查询
	 * @param m
	 * @param app
	 * @param ids
	 * @return
	 */
	private Page<EsMetricResult> aggSearch(String app, Long startTime, Long endTime, Pageable page) {
		String start = long2Date(startTime);
		String end = long2Date(endTime);
		String dslName = "nested";
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
				//
				.must(QueryBuilders.termQuery(appFileld, app))
				// FieldType.Object时，可使用
				// .must(QueryBuilders.termQuery(pidFileld, -1))
				.must(QueryBuilders.rangeQuery(timeFileld).gte(start).lte(end))
				// FieldType.Nested时使用
				.must(QueryBuilders.nestedQuery("spans", QueryBuilders.termQuery(pidFileld, -1), ScoreMode.None));
		SearchQuery sq = new NativeSearchQueryBuilder().withQuery(queryBuilder).withPageable(page)
				.addAggregation(AggregationBuilders.terms(agg + nameFileld).field(nameFileld)
						.size((page.getPageNumber() + 1) * page.getPageSize())
						.subAggregation(AggregationBuilders.nested(dslName, "spans")
								.subAggregation(AggregationBuilders.avg(agg + costFileld).field(costFileld))))
				//
				.build();

		Page<EsMetricResult> r = template.queryForPage(sq, EsMetricResult.class, new SearchResultMapper() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<EsMetricResult> datas = new ArrayList<>();
				long total = 0;
				Aggregations aggs = response.getAggregations();
				if (aggs != null) {
					Terms groups = aggs.get(agg + nameFileld);
					total = groups.getBuckets().size();
					for (Terms.Bucket entry : groups.getBuckets()) {
						Nested nesteds = entry.getAggregations().get(dslName);
						EsMetricResult v = new EsMetricResult();
						Avg avg = nesteds.getAggregations().get(agg + costFileld);
						v.setName(entry.getKeyAsString());
						v.setApp(avg.getValueAsString());
						datas.add(v);
					}
				}
				return new AggregatedPageImpl<T>((List<T>) datas, pageable, total);
			}
		});
		return r;
	}

	/**
	 * 嵌套聚合（nested Aggregation），nested类型的字段且为集合类型时，在该字段的数据集合中进行过滤条件查询
	 * @param app
	 * @param startTime
	 * @param endTime
	 * @param page
	 * @return
	 */
	private Page<EsMetricResult> nestedAggSearch(String app, Long startTime, Long endTime, Pageable page) {
		String dslName = "nested";
		String start = long2Date(startTime);
		String end = long2Date(endTime);
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
				//
				.must(QueryBuilders.termQuery(appFileld, app))
				.must(QueryBuilders.rangeQuery(timeFileld).gte(start).lte(end))
				.must(QueryBuilders.nestedQuery("spans", QueryBuilders.termQuery(pidFileld, -1), ScoreMode.None));
		QueryBuilder nested = QueryBuilders.nestedQuery(dslName, queryBuilder, ScoreMode.Avg);
		SearchQuery sq = new NativeSearchQueryBuilder().withQuery(queryBuilder).withPageable(page)
				.addAggregation(AggregationBuilders.nested(dslName, "spans")
						.subAggregation(AggregationBuilders.terms(agg + spanNameFileld).field(spanNameFileld)
								.size((page.getPageNumber() + 1) * page.getPageSize())
								//
								.subAggregation(AggregationBuilders.avg(agg + costFileld).field(costFileld))))
				//
				.build();

		Page<EsMetricResult> r = template.queryForPage(sq, EsMetricResult.class, new SearchResultMapper() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
				List<EsMetricResult> datas = new ArrayList<>();
				long total = 0;
				Aggregations aggs = response.getAggregations();
				if (aggs != null) {
					Nested nesteds = aggs.get(dslName);
					Terms groups = nesteds.getAggregations().get(agg + spanNameFileld);
					total = groups.getBuckets().size();
					for (Terms.Bucket entry : groups.getBuckets()) {
						EsMetricResult v = new EsMetricResult();
						Avg avg = entry.getAggregations().get(agg + costFileld);
						v.setName(entry.getKeyAsString());
						v.setApp(avg.getValueAsString());
						datas.add(v);
					}
				}
				return new AggregatedPageImpl<T>((List<T>) datas, pageable, total);
			}
		});
		return r;
	}

	private Page<EsMetricResult> nestedQuery(String app, Long startTime, Long endTime, Pageable page) {
		String dslName = "nested";
		String start = long2Date(startTime);
		String end = long2Date(endTime);
		QueryBuilder nested = QueryBuilders.boolQuery()
				//
				.must(QueryBuilders.termQuery(pidFileld, -1))
				//
				.must(QueryBuilders.rangeQuery(costFileld).gt(3000000));
		QueryBuilder queryBuilder = QueryBuilders.boolQuery()
				//
				.must(QueryBuilders.termQuery(appFileld, app))
				.must(QueryBuilders.rangeQuery(timeFileld).gte(start).lte(end))
				.must(QueryBuilders.nestedQuery("spans", nested, ScoreMode.None));
		SearchQuery sq = new NativeSearchQueryBuilder().withQuery(queryBuilder).withPageable(page)
				//
				.build();
		Page<EsMetricResult> r = template.queryForPage(sq, EsMetricResult.class);
		return r;
	}

	public static String long2Date(Long timestamp) {
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

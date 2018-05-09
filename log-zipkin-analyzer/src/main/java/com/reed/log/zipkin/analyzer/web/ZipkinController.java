package com.reed.log.zipkin.analyzer.web;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.reed.log.zipkin.analyzer.es.EsMetricResult;
import com.reed.log.zipkin.analyzer.es.EsMetricResultService;
import com.reed.log.zipkin.analyzer.es.EsZipkin;
import com.reed.log.zipkin.analyzer.redis.MetricsCacheService;
import com.reed.log.zipkin.analyzer.tree.TreeObj;

/**
 * 查询统计结果
 * @author reed
 *
 */
@RestController
@RequestMapping("/")
public class ZipkinController {

	@Autowired
	private MetricsCacheService cacheService;

	@Autowired
	private EsMetricResultService esService;

	/**
	 * 获取全部应用
	 * @return
	 */
	@RequestMapping(value = "apps")
	public ResponseEntity<Set<TreeObj>> getApps() {
		Set<TreeObj> r = new HashSet<>();
		Set<String> sets = cacheService.getAppNames();
		if (sets != null && !sets.isEmpty()) {
			for (String s : sets) {
				TreeObj t = new TreeObj<>();
				t.setApp(s);
				t.setId(s);
				r.add(t);
			}
		}
		return new ResponseEntity<Set<TreeObj>>(r, HttpStatus.OK);
	}

	/**
	 * 获取特定应用下的全部统计span
	 * @param app
	 * @return
	 */
	@RequestMapping(value = "spans")
	public ResponseEntity<Set<TreeObj>> getSpans(String app) {
		ResponseEntity<Set<TreeObj>> r = new ResponseEntity<>(HttpStatus.OK);
		if (app != null) {
			Set<TreeObj> set = new HashSet<>();
			Set<String> sets = cacheService.getMetricNames(app);
			if (sets != null && !sets.isEmpty()) {
				for (String s : sets) {
					TreeObj t = new TreeObj<>();
					t.setName(s);
					t.setId(s);
					set.add(t);
				}
			}
			r = ResponseEntity.ok().body(set);
		} else {
			r = ResponseEntity.badRequest().build();
		}
		return r;
	}

	/**
	 * 根据应用名、span名称，获取实时统计结果,集合式统计结果
	 * @param app
	 * @param name
	 * @return
	 */
	@RequestMapping(value = "/current/result")
	public ResponseEntity<List<EsMetricResult>> getCurrentResult(String app, String name) {
		ResponseEntity<List<EsMetricResult>> r = new ResponseEntity<>(HttpStatus.OK);
		if (app != null && name != null) {
			r = ResponseEntity.ok().body(esService.findCurrentResult(app, name));
		} else {
			r = ResponseEntity.ok().body(esService.findAllCurrentResult());
		}
		return r;
	}

	/**
	 * 根据应用名、span名称，获取实时统计结果，完整跟踪树式结果
	 * @param app
	 * @param name
	 * @return
	 */
	@RequestMapping(value = "/current/result/tree")
	public ResponseEntity<List<TreeObj>> getTreeCurrentResult(String app, String name) {
		ResponseEntity<List<TreeObj>> r = new ResponseEntity<>(HttpStatus.OK);
		r = ResponseEntity.ok().body(esService.findTreeCurrentResult(app, name));
		return r;
	}

	/**
	 * 查询耗时、错误trace信息
	 * @param app
	 * @param type
	 * @param name
	 * @param start
	 * @param end
	 * @param pageable
	 * @return
	 */
	@RequestMapping(value = "/es/trace")
	public ResponseEntity<Page<EsZipkin>> getEsTrace(@RequestParam(required = true) String app,
			@RequestParam(required = true) String type, @RequestParam(required = true) String name, Long start,
			Long end, @PageableDefault(size = 20, sort = { "createTime",
					"children.duration" }, direction = Direction.DESC) Pageable pageable) {
		ResponseEntity<Page<EsZipkin>> r = new ResponseEntity<>(HttpStatus.OK);
		Page<EsZipkin> page = esService.findTrace(app, type, name, Long2Date(start), Long2Date(end), pageable);

		r = ResponseEntity.ok().body(page);
		return r;
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

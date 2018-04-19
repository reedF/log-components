package com.reed.log.zipkin.analyzer.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reed.log.zipkin.analyzer.es.EsMetricResult;
import com.reed.log.zipkin.analyzer.es.EsMetricResultService;
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
}

package com.reed.log.demo.controller;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reed.log.aop.annotations.LogAopPointCut;
import com.reed.log.demo.test.AnnotationTestService;
import com.reed.log.pojo.LogObj;


@RestController
@RequestMapping("/")
public class DemoController {
	
	@Autowired
	//@Qualifier("annotationOrderTestBean")
	private AnnotationTestService bean;

	@RequestMapping("/name")
	public String printMessage(@RequestParam(defaultValue = "Michael") String name) {
		return name;
	}

	@RequestMapping(value = "/obj", produces = "application/json;charset=UTF-8")
	public LogObj obj(LogObj obj, String name) {
		bean.test(null, name);
		return obj;
	}

	@LogAopPointCut(canLogResult = "true")
	@RequestMapping(value = "/json", produces = "application/json;charset=UTF-8")
	public LogObj json(@RequestBody LogObj obj, String name, HttpServletRequest request) {
		test(obj, name);		
		return obj;
	}

	// 本类内部调用本地方法时，切面不生效
	@LogAopPointCut(canLogResult = "true")
	public LogObj test(LogObj obj, String name) {
		try {
			Thread.sleep(new Random().nextInt(500));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}
}

package com.reed.log.demo.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.reed.log.pojo.LogObj;

@RestController
@RequestMapping("/")
public class DemoController {

	@RequestMapping("/name")
	public String printMessage(@RequestParam(defaultValue = "Michael") String name) {
		return name;
	}

	@RequestMapping(value = "/obj", produces = "application/json;charset=UTF-8")
	public LogObj obj(LogObj obj, String name) {
		return obj;
	}

	@RequestMapping(value = "/json", produces = "application/json;charset=UTF-8")
	public LogObj json(@RequestBody LogObj obj, String name, HttpServletRequest request) {
		return obj;
	}
}

package com.reed.log.demo.test;

import java.util.Random;

import org.springframework.stereotype.Service;

import com.reed.log.aop.annotations.LogAopPointCut;
import com.reed.log.pojo.LogObj;


@Service
@LogAopPointCut
public class AnnotationTestService {

	@LogAopPointCut(canLogResult = "true")
	public LogObj test(LogObj obj, String name) {
		try {
			Thread.sleep(new Random().nextInt(500));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return obj;
	}
}

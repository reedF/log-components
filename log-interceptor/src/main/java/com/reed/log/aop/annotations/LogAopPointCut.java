package com.reed.log.aop.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * service级日志拦截注解,应用在需要拦截记录日志的业务类或方法上，以便细粒度控制日志记录
 * @author reed
 *
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface LogAopPointCut {
	//是否记录返回数据
	String canLogResult() default "false";
}

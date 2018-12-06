package com.reed.log.interceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.reed.log.aop.annotations.LogAopPointCut;
import com.reed.log.autoconfig.LogAspectProperties;
import com.reed.log.pojo.LogObj;

/**
 * 
 * @ClassName: LogAspect
 * @Description: 日志记录AOP实现
 *
 */
@Aspect
public class LogAspect {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private LogAspectProperties logProperties;

	@Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
	public void pointcut() {
	}

	// @within与@annotation区别：https://blog.csdn.net/cxh5060/article/details/45151863
	// @within表示拦截含有com.reed.log.aop.annotations.LogAopPointCut这个注解的类中"所有方法",即拦截使用此注解的"类"
	// @annotation拦截含有这个注解的"方法"
	@Pointcut("@within(com.reed.log.aop.annotations.LogAopPointCut)")
	// @Pointcut("@annotation(com.reed.log.aop.annotations.LogAopPointCut)")
	public void pointcutByAnnotation() {
	}

	/**
	 * 
	 * @Title：doAround拦截HTTP切点
	 * @Description: 环绕触发
	 * @param pjp
	 * @return
	 * @throws Throwable
	 */
	// @Around("pointcut() || pointcutByAnnotation()")
	@Around("pointcut()")
	// @Around("pointcutByAnnotation()")
	public Object doAround(ProceedingJoinPoint pjp) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String pkg = pjp.getTarget().getClass().getPackage().getName();
		try {
			// need to log
			if (checkPackages(pkg)) {
				LogObj log = new LogObj();
				log.setStartTime(System.currentTimeMillis());

				RequestAttributes ra = RequestContextHolder.getRequestAttributes();
				ServletRequestAttributes sra = (ServletRequestAttributes) ra;
				HttpServletRequest request = sra.getRequest();
				// 获取请求地址
				log.setUri(request.getRequestURI());
				log.setMethod(request.getMethod());

				// 获取输入参数
				// log.getInputParamMap().putAll(request.getParameterMap());
				getParams(pjp, log);

				// 执行完方法的返回值：调用proceed()方法，就会触发切入点方法执行
				Object result = pjp.proceed();// result的值就是被拦截方法的返回值

				// log.setOutputParamMap(getMap(result));
				if (logProperties.isCanLogResult()) {
					getResult(result, log);
				}
				log.setHttpCode(sra.getResponse().getStatus());
				log.setEndTime(System.currentTimeMillis());
				printOptLog(log);
				return result;
			} else {
				return pjp.proceed();
			}
		} catch (Throwable e) {
			logger.error("=========LogAspect error:{}========", e);
			resultMap.put("code", 500);
			resultMap.put("msg", "Logging error:" + e.getMessage());
			resultMap.put("data", null);
			return JSON.toJSONString(resultMap);
		}
	}

	@Before("@annotation(ann)")
	public void before(JoinPoint joinPoint, LogAopPointCut ann) {
		logger.info("===================");
	}

	@Around("pointcutByAnnotation()")
	public Object doAroundByAnno(ProceedingJoinPoint pjp) throws Throwable {
		String pkg = pjp.getTarget().getClass().getPackage().getName();
		try {
			// need to log
			if (checkPackages(pkg)) {
				LogObj log = new LogObj();
				log.setStartTime(System.currentTimeMillis());

				// 获取请求地址
				log.setUri(pjp.getSignature().getDeclaringTypeName());
				log.setMethod(pjp.getStaticPart().getSignature().getName());
				// 获取输入参数
				getParams(pjp, log);

				// 执行完方法的返回值：调用proceed()方法，就会触发切入点方法执行
				Object result = pjp.proceed();// result的值就是被拦截方法的返回值

				if (getCanLogResult(pjp)) {
					getResult(result, log);
				}

				log.setEndTime(System.currentTimeMillis());
				printOptLog(log);
				return result;
			} else {
				return pjp.proceed();
			}
		} catch (Throwable e) {
			logger.error("=========LogAspect error:{}========", e);
			throw e;
		}
	}

	private boolean checkPackages(String pkg) {
		boolean r = false;
		if (logProperties.getPackages() != null) {
			r = logProperties.getPackages().contains(pkg);
		}
		return r;
	}

	private void printOptLog(LogObj log) {
		if (log != null) {
			logger.info(JSON.toJSONString(log));
		}
	}

	private Map<String, Object> getMap(Object o) {
		Map<String, Object> result = new HashMap<String, Object>();
		if (o != null) {
			Field[] declaredFields = o.getClass().getDeclaredFields();
			try {
				for (Field field : declaredFields) {
					field.setAccessible(true);
					if (field.getName() != null && !field.getName().equals("serialPersistentFields")
							&& !field.getName().equals("hash") && !field.getName().equals("serialVersionUID")
							&& !field.getName().equals("CASE_INSENSITIVE_ORDER")) {
						result.put(field.getName(), field.get(o));
					}
				}
			} catch (Exception e) {
				logger.error("=======LogAspect object to map error:{}=======", e);
			}
		}
		return result;
	}

	private void getParams(JoinPoint point, LogObj log) {
		String[] parameterNames = ((MethodSignature) point.getSignature()).getParameterNames();
		if (Objects.nonNull(parameterNames)) {
			for (int i = 0; i < parameterNames.length; i++) {
				Object o = point.getArgs()[i];
				if (!(o instanceof HttpServletRequest) && !(o instanceof MultipartFile)) {
					log.getInputParamMap().put(parameterNames[i], JSON.toJSONString(o));
				}
			}
		}
	}

	private void getResult(Object result, LogObj log) {
		if (result != null) {
			Map<String, Object> m = new HashMap<>();
			m.put(result.getClass().getSimpleName(), JSON.toJSON(result));
			log.setOutputParamMap(m);
		}
	}

	/**
	 * 判断是否记录返回值
	 * @param pjp
	 * @return
	 */
	private boolean getCanLogResult(ProceedingJoinPoint pjp) {
		boolean r = false;
		Signature target = pjp.getSignature();
		if (target != null) {
			if (target instanceof MethodSignature) {
				MethodSignature m = (MethodSignature) target;
				LogAopPointCut annotation = m.getMethod().getAnnotation(LogAopPointCut.class);
				// 注解在类上，未注解在方法上时，从类上获取注解
				if (annotation == null) {
					annotation = m.getMethod().getDeclaringClass().getAnnotation(LogAopPointCut.class);
				}
				if (getAnnotaionMethod(annotation)) {
					r = true;
				}
			}
		}
		return r;
	}

	/**
	 * 获取是否记录返回值
	 * @param annotation
	 * @return
	 */
	private boolean getAnnotaionMethod(LogAopPointCut annotation) {
		String r = null;
		if (annotation != null) {
			Method[] me = annotation.annotationType().getDeclaredMethods();
			for (Method meth : me) {
				if (meth.getName().equals("canLogResult")) {
					try {
						r = (String) meth.invoke(annotation, null);
						break;
					} catch (Exception e) {
						logger.error("=======LogAspect get annotation error:{}=======", e);
					}
				}
			}
		}
		return "true".equals(r);

	}
}

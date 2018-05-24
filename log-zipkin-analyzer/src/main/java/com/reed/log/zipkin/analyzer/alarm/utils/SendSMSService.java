package com.reed.log.zipkin.analyzer.alarm.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

@Service
public class SendSMSService {
	private static Logger logger = LoggerFactory.getLogger(SendSMSService.class);

	@Value("${alarm.sms.url}")
	private String SMS_URL;

	@Autowired
	private RestTemplateBuilder builder;

	@Autowired
	private RestTemplate restTemplate;

	// 使用RestTemplateBuilder来实例化RestTemplate对象，spring默认已经注入了RestTemplateBuilder实例
	@Bean
	public RestTemplate restTemplate() {
		return builder.build();
	}

	public void sendSms(String tos, String msg) {
		try {
			if (StringUtils.isNotBlank(tos) && StringUtils.isNotBlank(msg)) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				// 封装参数，千万不要替换为Map与HashMap，否则参数无法传递
				MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
				// 也支持中文
				params.add("tos", tos);
				params.add("content", msg);
				HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(
						params, headers);
				String res = restTemplate.postForEntity(SMS_URL, requestEntity, String.class).getBody();
				logger.info("=========Send SMS:tos:{},Msg:{},Response:{}=========", tos, msg, res);
			}
		} catch (Exception e) {
			logger.error("=========Send SMS error:{}==========", e.getCause());
		}
	}
}

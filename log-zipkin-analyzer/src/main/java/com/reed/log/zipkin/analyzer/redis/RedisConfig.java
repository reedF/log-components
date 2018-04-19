package com.reed.log.zipkin.analyzer.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Double> getLongRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		final RedisTemplate<String, Double> redisTemplate = new RedisTemplate<String, Double>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Double>(Double.class));
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericToStringSerializer<Double>(Double.class));
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}

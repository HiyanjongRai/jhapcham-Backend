package com.example.jhapcham.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    public static final String PRODUCT_DETAIL = "product-detail";
    public static final String PRODUCT_PAGE = "product-page";
    public static final String CATEGORY_LIST = "category-list";
    public static final String RECOMMENDATIONS = "recommendations";
    public static final String BANNERS = "banners";
    public static final String SELLER_PROFILE = "seller-profile";
    public static final String HOMEPAGE = "homepage";
    
    // Homepage endpoints caches
    public static final String BEST_SELLERS = "best-sellers";
    public static final String TOP_RATED = "top-rated";
    public static final String MOST_WISHLISTED = "most-wishlisted";
    public static final String TRENDING = "trending";
    public static final String POPULAR_SEARCHES = "popular-searches";
    public static final String DASHBOARD_STATISTICS = "dashboard-statistics";
    public static final String ANNOUNCEMENTS = "announcements";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> cacheTtls = new java.util.HashMap<>();
        cacheTtls.put(PRODUCT_DETAIL, defaults.entryTtl(Duration.ofMinutes(15)));
        cacheTtls.put(PRODUCT_PAGE, defaults.entryTtl(Duration.ofMinutes(3)));
        cacheTtls.put(CATEGORY_LIST, defaults.entryTtl(Duration.ofHours(1)));
        cacheTtls.put(RECOMMENDATIONS, defaults.entryTtl(Duration.ofMinutes(30)));
        cacheTtls.put(BANNERS, defaults.entryTtl(Duration.ofMinutes(5)));
        cacheTtls.put(SELLER_PROFILE, defaults.entryTtl(Duration.ofMinutes(15)));
        cacheTtls.put(HOMEPAGE, defaults.entryTtl(Duration.ofMinutes(5)));
        cacheTtls.put(BEST_SELLERS, defaults.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put(TOP_RATED, defaults.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put(MOST_WISHLISTED, defaults.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put(TRENDING, defaults.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put(POPULAR_SEARCHES, defaults.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put(DASHBOARD_STATISTICS, defaults.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put(ANNOUNCEMENTS, defaults.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheTtls)
                .transactionAware()
                .build();
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}

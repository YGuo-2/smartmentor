package com.tricia.smartmentor.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ======================== String ========================

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ======================== Hash ========================

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void hDelete(String key, String... fields) {
        redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    // ======================== ZSet (排行榜) ========================

    public void zAdd(String key, String member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    public Double zIncrBy(String key, String member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    public Double zScore(String key, String member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    public Long zRank(String key, String member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    /**
     * 获取排行榜 Top N（分数从高到低）
     */
    public Set<ZSetOperations.TypedTuple<Object>> zTopN(String key, int n) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, n - 1);
    }

    public Long zSize(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    // ======================== 限流 ========================

    /**
     * 滑动窗口限流
     * @param key     限流 key（如 rate_limit:chat:userId）
     * @param maxCount 窗口内最大请求数
     * @param windowSeconds 窗口大小（秒）
     * @return true=允许通过, false=被限流
     */
    public boolean isAllowed(String key, int maxCount, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        // Remove expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count current window
        Long count = redisTemplate.opsForZSet().zCard(key);
        if (count != null && count >= maxCount) {
            return false;
        }

        // Add current request
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, windowSeconds + 1, TimeUnit.SECONDS);
        return true;
    }
}

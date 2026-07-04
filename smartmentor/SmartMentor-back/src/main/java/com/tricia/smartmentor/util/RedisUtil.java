package com.tricia.smartmentor.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_RATE_LIMIT_SCRIPT =
            new DefaultRedisScript<>(
                    "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]); " +
                            "local count = redis.call('ZCARD', KEYS[1]); " +
                            "if count >= tonumber(ARGV[2]) then return 0; end; " +
                            "redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4]); " +
                            "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[5])); " +
                            "return 1;",
                    Long.class);

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
        String member = now + "-" + UUID.randomUUID();

        Long allowed = redisTemplate.execute(
                SLIDING_WINDOW_RATE_LIMIT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(windowStart),
                String.valueOf(maxCount),
                String.valueOf(now),
                member,
                String.valueOf(windowSeconds + 1));
        return Long.valueOf(1L).equals(allowed);
    }
}

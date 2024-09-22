package io.github.playlistmanager.provider;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RedisProvider {

    private StringRedisTemplate redisTemplate;

    public RedisProvider(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void saveMap(String key, Map<String, String> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    public Map<String, String> getMap(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, String> resultMap = new HashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            resultMap.put(entry.getKey().toString(), Base64.getEncoder().encodeToString(entry.getValue().toString().getBytes()));
        }

        return resultMap;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

}

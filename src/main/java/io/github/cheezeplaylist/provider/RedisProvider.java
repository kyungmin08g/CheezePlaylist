package io.github.cheezeplaylist.provider;

import io.github.cheezeplaylist.dto.MusicFileDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RedisProvider {

    private final StringRedisTemplate redisTemplate;

    public RedisProvider(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void saveMap(String key, MusicFileDto musicFileDTO) {
        Map<String, String> musicFileMap = new HashMap<>();
        musicFileMap.put("name", musicFileDTO.getTitle());
        musicFileMap.put("data", musicFileDTO.getMusicFileBytes()); // byte[]를 Base64로 인코딩

        redisTemplate.opsForHash().putAll(key, musicFileMap);
    }

    public void addMusicFileToList(String key, MusicFileDto musicFileDTO) {
        String musicFileString = musicFileDTO.getTitle() + ":" + musicFileDTO.getMusicFileBytes();
        redisTemplate.opsForList().rightPush(key, musicFileString);
    }

    public List<MusicFileDto> getMusicFilesFromList(String key) {
        List<MusicFileDto> musicFiles = new ArrayList<>();

        List<String> musicFileStrings = redisTemplate.opsForList().range(key, 0, -1);
        for (String musicFileString : musicFileStrings) {
            String[] parts = musicFileString.split(":");
            String name = parts[0];
            byte[] data = Base64.getDecoder().decode(parts[1]);

//            musicFiles.add(new MusicFileDTO(name, data));
        }

        return musicFiles;
    }


    public void delete(String key) {
        redisTemplate.delete(key);
    }

}

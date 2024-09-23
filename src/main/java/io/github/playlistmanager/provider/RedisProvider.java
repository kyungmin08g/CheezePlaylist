package io.github.playlistmanager.provider;

import io.github.playlistmanager.dto.MusicFileDTO;
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

    public void saveMap(String key, MusicFileDTO musicFileDTO) {
        Map<String, String> musicFileMap = new HashMap<>();
        musicFileMap.put("name", musicFileDTO.getTitle());
        musicFileMap.put("data", Base64.getEncoder().encodeToString(musicFileDTO.getMusicFileBytes())); // byte[]를 Base64로 인코딩

        redisTemplate.opsForHash().putAll(key, musicFileMap);
    }

    public void addMusicFileToList(String key, MusicFileDTO musicFileDTO) {
        String musicFileString = musicFileDTO.getTitle() + ":" + Base64.getEncoder().encodeToString(musicFileDTO.getMusicFileBytes());
        redisTemplate.opsForList().rightPush(key, musicFileString);
    }

    public List<MusicFileDTO> getMusicFilesFromList(String key) {
        List<MusicFileDTO> musicFiles = new ArrayList<>();

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

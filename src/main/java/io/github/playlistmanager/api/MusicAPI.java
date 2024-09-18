package io.github.playlistmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MusicAPI {

    @Value("${youtube.api-key}")
    private static String youtubeApiKey;

    // 쿼리를 받고 동영상의 URL를 만들어주는 메소드
    public static String videoUrl(String query) {
        String searchVideoResultJson = searchVideo(query);

        try {
            String videoIdValue = "";
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode json = objectMapper.readTree(searchVideoResultJson);
            JsonNode items = json.get("items");

            for (JsonNode item : items) {
                JsonNode id = item.get("id");
                JsonNode videoId = id.get("videoId");
                videoIdValue = videoId.asText();
            }

            return "https://www.youtube.com/watch?v=" + videoIdValue;
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        return "해당 동영상이 없음";
    }

    // 쿼리를 받으면 YouTube Data API를 통해 동영상을 검색하는 메소드
    public static String searchVideo(String query) {
        Mono<String> response = WebClient.builder().baseUrl("https://www.googleapis.com").build().get()
                .uri(uriBuilder -> uriBuilder.path("/youtube/v3/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query + " (lyrics)")
                        .queryParam("maxResults", "1")
                        .queryParam("type", "video")
                        .queryParam("key", youtubeApiKey)
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class);

        return response.block();
    }

    // URL를 mp3로 변환하는 메소드(yt-dlp라는 파이썬 스크립트 사용함)
    public static void mp3Conversion(String youtubeURL) {
        String downloadPath = System.getProperty("user.dir") + "/src/main/resources/static/audio";

        try {
            // 프로세스 시작
            Process process = new ProcessBuilder("/opt/homebrew/bin/yt-dlp", "-x", "--audio-format", "mp3", youtubeURL)
                    .directory(new File(downloadPath)).start();

            // 명령어 출력 및 오류 메시지 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // 오류 출력
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            // 프로세스가 완료될 때까지 대기
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("변환 완료!");
            } else {
                System.out.println("변환 실패! 오류 코드: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            e.fillInStackTrace();
        }
    }

}

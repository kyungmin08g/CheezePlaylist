package io.github.playlistmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class YouTubeAPI {

    @Value("${youtube.api-key}")
    public String youtubeApiKey;

    // 쿼리를 받고 동영상의 URL를 만들어주는 메소드
    public String videoUrl(String query) {
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
    public String searchVideo(String query) {
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

}

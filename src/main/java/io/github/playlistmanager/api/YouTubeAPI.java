package io.github.playlistmanager.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class YouTubeAPI {

    @Value("${youtube.api-key}")
    public String youtubeApiKey;

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

        System.out.println(response.block());

        return response.block();
    }

}

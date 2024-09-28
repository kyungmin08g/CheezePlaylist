package io.github.chzzkapi.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chzzkapi.listener.ChatListener;
import io.github.chzzkapi.message.ChatMessage;
import io.github.chzzkapi.message.DonationMessage;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class ChzzkAPI {

    private static String chatChannelId = null;
    private static String accessToken = null;
    private static int serverId = 0;
    private static WebSocketSession webSocketSession;

    private static final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void chat(String channelId, ChatListener listener) {
        String chatChannelId = getChatChannelId(channelId);
        String accessToken = getAccessToken(chatChannelId);

        // 서버 아이디 구하기
        for (char i : chatChannelId.toCharArray()) {
            serverId += Character.getNumericValue(i);
        }
        serverId = Math.abs(serverId) % 9 + 1;

        CompletableFuture.runAsync(() -> { // 비동기 처리
            // 웹소켓 클라이언트 연결
            webSocketClient.execute(URI.create("wss://kr-ss" + serverId + ".chat.naver.com/chat"), new WebSocketHandler() {
                @Override
                public Mono<Void> handle(@Nullable WebSocketSession session) {
                    webSocketSession = session;

                    Map<String, Object> bdyJson = new HashMap<>();
                    bdyJson.put("uid", null);
                    bdyJson.put("devType", 2001);
                    bdyJson.put("accTkn", accessToken);
                    bdyJson.put("auth", "READ");

                    Map<String, Object> initialJson = new HashMap<>();
                    initialJson.put("ver", "2");
                    initialJson.put("cmd", 100);
                    initialJson.put("svcid", "game");
                    initialJson.put("cid", chatChannelId);
                    initialJson.put("bdy", bdyJson);
                    initialJson.put("tid", 1);

                    try {
                        String json = objectMapper.writeValueAsString(initialJson);

                        return Objects.requireNonNull(session).send(Flux.interval(Duration.ofSeconds(1))
                                .map(tick ->
                                        session.textMessage(json)
                                )).and(session.receive().doOnNext(msg -> {
                                    /*
                                        여기서는 메소드 호출 안시키는게 가장 좋을 듯. 확실히 데이터가 빠르게 넘어오고 끊기지고 않고 좋네 :)
                                    */
                                    getConnect(msg, listener);
                                    getChat(msg, listener);
                                    getDonation(msg, listener);
                        })).then();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(5)).doBeforeRetry(
                    retrySignal -> System.out.println("연결 재시도 중..")
            )).block();
        });
    }

    public static void onDisconnect() {
        if (webSocketSession.isOpen()) webSocketSession.close().subscribe();
    }

    // 채팅 아이디 받기 메소드
    private static String getChatChannelId(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("polling/v3/channels/" + channelId + "/live-status")
                .retrieve().bodyToMono(String.class);

        try {
            ObjectMapper liveStatusObjectMapper = new ObjectMapper();
            JsonNode liveStatusJson = liveStatusObjectMapper.readTree(response.block());
            JsonNode liveStatusContentJson = liveStatusJson.get("content");
            chatChannelId = liveStatusContentJson.get("chatChannelId").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return chatChannelId;
    }

    // 액세스 토큰 받기 메소드
    private static String getAccessToken(String chatChannelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://comm-api.game.naver.com").build()
                .get()
                .uri("nng_main/v1/chats/access-token?channelId=" + chatChannelId + "&chatType=STREAMING")
                .retrieve().bodyToMono(String.class);

        try {
            ObjectMapper accessTokenObjectMapper = new ObjectMapper();
            JsonNode accessTokenJson = accessTokenObjectMapper.readTree(response.block());
            JsonNode accessTokenContentJson = accessTokenJson.get("content");
            accessToken = accessTokenContentJson.get("accessToken").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return accessToken;
    }

    private static void getConnect(WebSocketMessage msg, ChatListener listener) {
        try {
            JsonNode jsonNode = objectMapper.readTree(msg.getPayloadAsText());
            JsonNode bdy = jsonNode.get("bdy");
            JsonNode auth = bdy.get("auth"); // 처음에 연결할떄 생기는 JSON

            if (auth != null) {
                listener.onConnect();
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    private static void getChat(WebSocketMessage msg, ChatListener listener) {
        try {
            JsonNode jsonNode = objectMapper.readTree(msg.getPayloadAsText());
            JsonNode bdy = jsonNode.get("bdy");
            if (bdy == null) return;

            JsonNode auth = bdy.get("auth"); // 처음에 연결할떄 생기는 JSON
            if (auth != null) return;

            for (JsonNode bdyNode : bdy) {
                int msgTypeCode = bdyNode.get("msgTypeCode").asInt();

                if (msgTypeCode == 1) { // 후원 타입이면..
                    String profile = bdyNode.get("profile").asText();
                    JsonNode message = bdyNode.get("msg");

                    if (profile.equals("null")) { // 프로필이 없을 떄
                        listener.onChat(new ChatMessage("익명", message.asText()));
                        return;
                    }

                    JsonNode profileNode = objectMapper.readTree(profile);
                    JsonNode nickname = profileNode.get("nickname");
                    listener.onChat(new ChatMessage(nickname.asText(), message.asText()));
                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    private static void getDonation(WebSocketMessage msg, ChatListener listener) {
        try {
            JsonNode jsonNode = objectMapper.readTree(msg.getPayloadAsText());
            JsonNode bdy = jsonNode.get("bdy");
            if (bdy == null) return;

            JsonNode auth = bdy.get("auth"); // 처음에 연결할떄 생기는 JSON
            if (auth != null) return;

            for (JsonNode bdyNode : bdy) {
                int msgTypeCode = bdyNode.get("msgTypeCode").asInt();

                if (msgTypeCode == 10) { // 후원 타입이면..
                    String profile = bdyNode.get("profile").asText();
                    String payAmount = objectMapper.readTree(bdyNode.get("extras").asText()).get("payAmount").asText();
                    JsonNode message = bdyNode.get("msg");

                    if (profile.equals("null")) { // 프로필이 없을 떄
                        listener.onDonation(new DonationMessage("익명", message.asText(), payAmount));
                        return;
                    }

                    JsonNode profileNode = objectMapper.readTree(profile);
                    JsonNode nickname = profileNode.get("nickname");
                    listener.onDonation(new DonationMessage(nickname.asText(), message.asText(), payAmount));
                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

}

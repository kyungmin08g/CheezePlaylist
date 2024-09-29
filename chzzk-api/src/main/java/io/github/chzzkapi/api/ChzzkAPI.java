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
    private static final int oneYear = 12;

    private static final ReactorNettyWebSocketClient webSocketClient = new ReactorNettyWebSocketClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 비동기로 웹소켓 클라이언트에 연결하는 메소드
    public static void chat(String channelId, ChatListener listener) {
        String chatChannelId = getChatChannelId(channelId);
        String accessToken = getAccessToken(chatChannelId);

        // 서버 아이디 구하기
        for (char i : chatChannelId.toCharArray()) {
            serverId += Character.getNumericValue(i);
        }
        serverId = Math.abs(serverId) % 9 + 1;

        // 초기 세팅? -> 웹소켓 서버에 먼저 보내줘야하는 값
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

        CompletableFuture.runAsync(() -> { // 비동기 처리
            // 웹소켓 클라이언트 연결
            webSocketClient.execute(URI.create("wss://kr-ss" + serverId + ".chat.naver.com/chat"), new WebSocketHandler() {
                @Override
                public Mono<Void> handle(@Nullable WebSocketSession session) {
                    webSocketSession = session;
                    try {
                        String json = objectMapper.writeValueAsString(initialJson);

                        return Objects.requireNonNull(session).send(Flux.interval(Duration.ofSeconds(1))
                                .map(tick ->
                                        session.textMessage(json)
                                )).and(session.receive().doOnNext(msg -> {
//                                        System.out.println(msg.getPayloadAsText());
                                        getConnect(msg, listener);
                                        getChat(msg, listener);
                                        getDonation(msg, listener);
                                })).then();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).retryWhen(Retry.fixedDelay(30, Duration.ofSeconds(1)).doBeforeRetry(
                    retrySignal -> System.out.println("연결 재시도 중..")
            )).block();
        });
    }

    // 웹소켓과 연결 해제하는 메소드
    public static void onDisconnect() {
        if (webSocketSession.isOpen()) webSocketSession.close().subscribe();
    }

    // 채널 이름 구하는 메소드
    public static String getChannelName(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("channelName").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 설명 구하는 메소드
    public static String getChannelDescription(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("channelDescription").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 팔로워 상태 구하는 메소드
    public static String getFollowCount(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("followerCount").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 채팅 규칙 받는 메소드
    public static String getChannelChatRule(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId + "/chat-rules")
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("rule").asText();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return null;
    }

    // 채널 라이브 상태 구하는 메소드
    public static boolean isLive(String channelId) {
        Mono<String> response = WebClient.builder().baseUrl("https://api.chzzk.naver.com").build()
                .get()
                .uri("service/v1/channels/" + channelId)
                .retrieve()
                .bodyToMono(String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response.block());
            JsonNode content = jsonNode.get("content");

            return content.get("openLive").asBoolean();
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }

        return false;
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

    // 채팅 처리 메소드
    private static void getChat(WebSocketMessage msg, ChatListener listener) {
        try {
            JsonNode jsonNode = objectMapper.readTree(msg.getPayloadAsText());
            JsonNode bdy = jsonNode.get("bdy");
            if (bdy == null) return;

            JsonNode auth = bdy.get("auth"); // 처음에 연결할떄 생기는 JSON
            if (auth != null) return;

            JsonNode cmd = jsonNode.get("cmd");
            if (cmd.asInt() == 94008) return; // 클린봇 때문에..

            for (JsonNode bdyNode : bdy) {
                int msgTypeCode = bdyNode.get("msgTypeCode").asInt();

                if (msgTypeCode == 1) { // 채팅 타입이면..
                    String profile = bdyNode.get("profile").asText();
                    JsonNode message = bdyNode.get("msg");

                    if (profile.equals("null")) { // 프로필이 없을 떄
                        listener.onChat(new ChatMessage("익명", message.asText(), null));
                        return;
                    }

                    JsonNode profileNode = objectMapper.readTree(profile);
                    JsonNode nickname = profileNode.get("nickname");
                    JsonNode streamingProperty = profileNode.get("streamingProperty");
                    listener.onChat(new ChatMessage(nickname.asText(), message.asText(), monthsCalculation(streamingProperty)));
                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    // 도네이션 처리 메소드
    private static void getDonation(WebSocketMessage msg, ChatListener listener) {
        try {
            JsonNode jsonNode = objectMapper.readTree(msg.getPayloadAsText());
            JsonNode bdy = jsonNode.get("bdy");
            if (bdy == null) return;

            JsonNode auth = bdy.get("auth"); // 처음에 연결할떄 생기는 JSON
            if (auth != null) return;

            JsonNode cmd = jsonNode.get("cmd");
            if (cmd.asInt() == 94008) return; // 클린봇 때문에..

            for (JsonNode bdyNode : bdy) {
                int msgTypeCode = bdyNode.get("msgTypeCode").asInt();

                if (msgTypeCode == 10) { // 후원 타입이면..
                    String profile = bdyNode.get("profile").asText();
                    String payAmount = objectMapper.readTree(bdyNode.get("extras").asText()).get("payAmount").asText();
                    JsonNode message = bdyNode.get("msg");

                    if (profile.equals("null")) { // 프로필이 없을 떄
                        listener.onDonation(new DonationMessage("익명", message.asText(), payAmount, null));
                        return;
                    }

                    JsonNode profileNode = objectMapper.readTree(profile);
                    JsonNode nickname = profileNode.get("nickname");
                    JsonNode streamingProperty = profileNode.get("streamingProperty");
                    listener.onDonation(new DonationMessage(nickname.asText(), message.asText(), payAmount, monthsCalculation(streamingProperty)));
                }
            }
        } catch (JsonProcessingException e) {
            e.fillInStackTrace();
        }
    }

    // 구독 년 및 개월 구하는 메소드
    private static String monthsCalculation(JsonNode streamingPropertyNode) {
        JsonNode subscription = streamingPropertyNode.get("subscription");
        if (subscription == null) return null;

        JsonNode accumulativeMonth = subscription.get("accumulativeMonth");

        int year = 0;
        int month = 0;
        for (int i = oneYear; i <= accumulativeMonth.asInt(); i += oneYear) {
            year += 1;
            month = i;
        }

        if (year == 0) return accumulativeMonth.asInt() + "개월";
        if ((accumulativeMonth.asInt() - month) == 0) return year + "년";

        return year + "년 " + (accumulativeMonth.asInt() - month) + "개월";
    }

}

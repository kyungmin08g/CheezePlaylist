package io.github.chzzkapi.message;

import lombok.Getter;

public class ChatMessage {
    @Getter
    private final String content;
    private final String nickName;
    private final String subscriptionMonth;

    public ChatMessage(String nickName, String content, String subscriptionMonth) {
        this.nickName = nickName;
        this.content = content;
        this.subscriptionMonth = subscriptionMonth;
    }

    public String getNickName() {
        return this.nickName;
    }

    public String getSubscriptionMonth() {
        return this.subscriptionMonth;
    }
}

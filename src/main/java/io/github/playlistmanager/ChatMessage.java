package io.github.playlistmanager;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Builder
public class ChatMessage {
    @Getter
    private final String nickName;
    @Getter
    private final String content;
    private final String subscriptionMonth;

    public ChatMessage(String nickName, String content, String subscriptionMonth) {
        this.nickName = nickName;
        this.content = content;
        this.subscriptionMonth = subscriptionMonth;
    }

    public String getSubscriptionMonth() {
        return this.subscriptionMonth;
    }
}

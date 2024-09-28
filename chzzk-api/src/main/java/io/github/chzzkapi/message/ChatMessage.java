package io.github.chzzkapi.message;

import lombok.Getter;

public class ChatMessage {
    @Getter
    private final String content;
    private final String nickName;

    public ChatMessage(String nickName, String content) {
        this.nickName = nickName;
        this.content = content;
    }

    public String getNickName() {
        return this.nickName;
    }
}

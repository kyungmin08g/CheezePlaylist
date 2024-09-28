package io.github.chzzkapi.message;

import lombok.Getter;

public class DonationMessage {
    private final String nickName;
    @Getter
    private final String content;
    @Getter
    private final String payAmount;

    public DonationMessage(String nickName, String content, String payAmount) {
        this.nickName = nickName;
        this.content = content;
        this.payAmount = payAmount;
    }

    public String getNickName() {
        return nickName;
    }
}

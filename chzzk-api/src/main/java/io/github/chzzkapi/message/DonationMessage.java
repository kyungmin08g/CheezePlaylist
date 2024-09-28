package io.github.chzzkapi.message;

import lombok.Getter;

@Getter
public class DonationMessage {
    private final String nickName;
    private final String content;
    private final String payAmount;

    public DonationMessage(String nickName, String content, String payAmount) {
        this.nickName = nickName;
        this.content = content;
        this.payAmount = payAmount;
    }

}

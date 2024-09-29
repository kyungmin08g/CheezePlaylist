package io.github.chzzkapi.message;

import lombok.Getter;

public class DonationMessage {
    @Getter
    private final String nickName;
    @Getter
    private final String content;
    @Getter
    private final String payAmount;
    private final String subscriptionMonth;

    public DonationMessage(String nickName, String content, String payAmount, String subscriptionMonth) {
        this.nickName = nickName;
        this.content = content;
        this.payAmount = payAmount;
        this.subscriptionMonth = subscriptionMonth;
    }

    public String getSubscriptionMonth() {
        return this.subscriptionMonth;
    }

}

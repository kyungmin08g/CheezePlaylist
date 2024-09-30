package io.github.chzzkapi.listener;

import io.github.chzzkapi.message.ChatMessage;
import io.github.chzzkapi.message.DonationMessage;

public interface ChatListener {
    void onConnect();
    void onChat(ChatMessage message);
    void onDonation(DonationMessage message);
    void onDisconnect(boolean open);
}

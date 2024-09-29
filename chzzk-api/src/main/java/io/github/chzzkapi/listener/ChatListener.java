package io.github.chzzkapi.listener;

import io.github.chzzkapi.message.ChatMessage;
import io.github.chzzkapi.message.DonationMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

public interface ChatListener {
    void onConnect();
    void onChat(ChatMessage message);
    void onDonation(DonationMessage message);
    void onDisconnect(boolean open);
}

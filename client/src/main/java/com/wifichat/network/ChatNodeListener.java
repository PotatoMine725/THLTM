package com.wifichat.network;

import com.wifichat.model.ChatMessage;
import com.wifichat.model.PeerInfo;

import java.util.List;

public interface ChatNodeListener {
    void onPeerListUpdated(List<PeerInfo> peers);

    void onRoomDiscovered(String roomName);

    void onGroupMessage(ChatMessage message);

    void onPrivateMessage(ChatMessage message);

    void onSystemNotice(String message);
}


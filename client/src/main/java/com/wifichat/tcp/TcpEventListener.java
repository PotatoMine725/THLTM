package com.wifichat.tcp;

import com.wifichat.shared.dto.MessageRecord;

public interface TcpEventListener {
    void onMessageEvent(MessageRecord message);

    void onDisconnected(String reason);
}

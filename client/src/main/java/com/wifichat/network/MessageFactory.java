package com.wifichat.network;

import com.wifichat.model.ChatMessage;
import com.wifichat.model.ChatScope;
import com.wifichat.model.PeerInfo;
import com.wifichat.util.TextUtils;

import java.util.UUID;

public class MessageFactory {
    private final String localUserId;
    private final String localUserName;

    public MessageFactory(String localUserId, String localUserName) {
        this.localUserId = localUserId;
        this.localUserName = localUserName;
    }

    public ChatMessage group(String roomName, String content, ChatMessage replyTarget) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                ChatScope.GROUP,
                localUserId,
                localUserName,
                roomName,
                null,
                null,
                System.currentTimeMillis(),
                content,
                replyTarget == null ? null : replyTarget.messageId(),
                replyTarget == null ? null : TextUtils.shortPreview(replyTarget.senderName(), replyTarget.content())
        );
    }

    public ChatMessage privateMessage(PeerInfo peer, String content, ChatMessage replyTarget) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                ChatScope.PRIVATE,
                localUserId,
                localUserName,
                null,
                peer.userId(),
                peer.displayName(),
                System.currentTimeMillis(),
                content,
                replyTarget == null ? null : replyTarget.messageId(),
                replyTarget == null ? null : TextUtils.shortPreview(replyTarget.senderName(), replyTarget.content())
        );
    }
}


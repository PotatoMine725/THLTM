package com.wifichat.shared.dto;

import java.util.ArrayList;
import java.util.List;

public class HistoryBatchResponse implements java.io.Serializable {
    public String conversationKey;
    public List<MessageRecord> messages = new ArrayList<>();

    public HistoryBatchResponse() {
    }
}


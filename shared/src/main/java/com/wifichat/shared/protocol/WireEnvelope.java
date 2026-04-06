package com.wifichat.shared.protocol;

public class WireEnvelope implements java.io.Serializable {
    public String type;
    public String requestId;
    public Object payload;
    public String errorCode;
    public String errorMessage;

    public WireEnvelope() {
    }

    public static WireEnvelope of(String type, String requestId, Object payload) {
        WireEnvelope envelope = new WireEnvelope();
        envelope.type = type;
        envelope.requestId = requestId;
        envelope.payload = payload;
        return envelope;
    }

    public static WireEnvelope error(String requestId, String errorCode, String errorMessage) {
        WireEnvelope envelope = new WireEnvelope();
        envelope.type = PacketTypes.ERROR;
        envelope.requestId = requestId;
        envelope.errorCode = errorCode;
        envelope.errorMessage = errorMessage;
        return envelope;
    }
}


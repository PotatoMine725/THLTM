package com.wifichat.tcp;

public class TcpClientException extends Exception {
    public TcpClientException(String message) {
        super(message);
    }

    public TcpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

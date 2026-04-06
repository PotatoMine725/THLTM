package com.wifichat.shared.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class WireCodec {
    private static final int MAX_FRAME_SIZE = 4 * 1024 * 1024;

    public void write(OutputStream outputStream, WireEnvelope envelope) throws IOException {
        byte[] data = serialize(envelope);
        DataOutputStream out = new DataOutputStream(outputStream);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public WireEnvelope read(InputStream inputStream) throws IOException {
        DataInputStream in = new DataInputStream(inputStream);
        int length = in.readInt();
        if (length <= 0 || length > MAX_FRAME_SIZE) {
            throw new IOException("Invalid frame length: " + length);
        }

        byte[] data = new byte[length];
        in.readFully(data);
        Object decoded = deserialize(data);
        if (!(decoded instanceof WireEnvelope envelope)) {
            throw new IOException("Invalid envelope payload type");
        }
        return envelope;
    }

    private byte[] serialize(WireEnvelope envelope) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(envelope);
            out.flush();
        }
        return bos.toByteArray();
    }

    private Object deserialize(byte[] data) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown payload class", e);
        }
    }
}

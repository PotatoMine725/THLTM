package com.wifichat.network;

import com.wifichat.model.NetworkPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PacketCodec {
    public byte[] encode(NetworkPacket packet) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(packet);
            out.flush();
        }
        return bos.toByteArray();
    }

    public NetworkPacket decode(byte[] data, int length) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, length);
        try (ObjectInputStream in = new ObjectInputStream(bis)) {
            Object value = in.readObject();
            if (!(value instanceof NetworkPacket packet)) {
                throw new IOException("Unsupported packet payload");
            }
            return packet;
        }
    }
}


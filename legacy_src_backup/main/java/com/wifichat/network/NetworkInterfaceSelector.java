package com.wifichat.network;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkInterfaceSelector {
    private NetworkInterfaceSelector() {
    }

    public static NetworkInterface choose(String preferredName) throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        NetworkInterface fallback = null;

        while (interfaces.hasMoreElements()) {
            NetworkInterface current = interfaces.nextElement();
            if (!current.isUp() || current.isLoopback() || !current.supportsMulticast()) {
                continue;
            }
            if (!hasIpv4(current)) {
                continue;
            }

            if (preferredName != null && !preferredName.isBlank()) {
                String lower = preferredName.toLowerCase();
                String name = current.getName() == null ? "" : current.getName().toLowerCase();
                String display = current.getDisplayName() == null ? "" : current.getDisplayName().toLowerCase();
                if (name.contains(lower) || display.contains(lower)) {
                    return current;
                }
            }

            if (fallback == null) {
                fallback = current;
            }
        }

        return fallback;
    }

    private static boolean hasIpv4(NetworkInterface networkInterface) {
        return networkInterface.getInterfaceAddresses().stream()
                .anyMatch(address -> address.getAddress() instanceof Inet4Address);
    }
}


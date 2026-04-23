package network;

import java.net.InetSocketAddress;

public record RawUDPRequest(InetSocketAddress address, byte[] data) {}

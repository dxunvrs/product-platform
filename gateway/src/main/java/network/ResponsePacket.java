package network;

import java.net.InetSocketAddress;

public record ResponsePacket(InetSocketAddress address, byte[] data) { }

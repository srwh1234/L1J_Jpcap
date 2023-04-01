package main;

import java.net.InetAddress;

import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;

public class ServerPacketReceiver implements PacketReceiver {

	private final NetworkInterface networkInterface;

	private final ServerPacketReader serverPacketReader;

	public ServerPacketReceiver(final NetworkInterface networkInterface) {
		this.networkInterface = networkInterface;
		this.serverPacketReader = new ServerPacketReader();
	}

	@Override
	public void receivePacket(final Packet packet) {
		try {

			if (packet instanceof TCPPacket) {
				final InetAddress address = ((TCPPacket) packet).src_ip;

				// 不要客戶端封包
				if (isFromClient(address)) {
					return;
				}

				// 丟給reader
				serverPacketReader.put(packet.data);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	// 封包是否來自客戶端
	private boolean isFromClient(final InetAddress srcAddress) {
		for (final NetworkInterfaceAddress address : networkInterface.addresses) {
			if (address.address.equals(srcAddress)) {
				return true;
			}
		}
		return false;
	}
}

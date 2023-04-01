package main;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerPacketReader extends Thread {

	private final LinkedBlockingQueue<byte[]> bytesQueue = new LinkedBlockingQueue<>();

	private Cipher cipher;

	private byte[] buffer = new byte[0];

	public ServerPacketReader() {
		this.start();
	}

	public void put(final byte[] data) {
		bytesQueue.offer(data);
	}

	@Override
	public void run() {
		try {
			byte[] data;
			while (true) {

				// 超過時間仍無空間則返回null
				data = bytesQueue.poll(3, TimeUnit.MINUTES);

				if (data != null && data.length > 0) {
					read(data);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	// 黏包的拆包處理
	private void read(final byte[] data) {

		// 沒讀完的加上新的
		buffer = StaticUtils.concat(buffer, data);

		while (buffer.length >= 2) {
			final int len = (buffer[0] & 0xff) | ((buffer[1] << 8) & 0xff00);

			if (len < 0) {
				buffer = new byte[0];
				break;
			}

			// 資料不夠讀 等待下一筆
			if (len > buffer.length) {
				break;
			}

			final byte[] read = new byte[len];
			System.arraycopy(buffer, 0, read, 0, len);

			// 處理正確大小的封包
			handlePacket(read);

			// 剩餘的
			final int nextLen = buffer.length - len;

			if (nextLen == 0) {
				buffer = new byte[0];
				break;
			}
			// 沒讀完的當做下一筆的開頭
			buffer = Arrays.copyOfRange(buffer, len, buffer.length);
		}
	}

	private void handlePacket(final byte[] packet) {
		byte[] data = new byte[packet.length - 2];
		System.arraycopy(packet, 2, data, 0, data.length);

		final int subCode = data[0] & 0xFF;

		// 初始化加解密
		if (cipher == null) {
			initCipher(data);
			System.out.println("S_KEY (" + subCode + ")");

			final PacketLogger logger = new PacketLogger(data);
			logger.write();
			return;
		}

		// 解密
		data = cipher.decryptServer(data);

		final PacketLogger logger = new PacketLogger(data);
		logger.print();
		logger.write();
	}

	private void initCipher(final byte[] data) {
		int key = data[1] & 0xff;
		key |= data[2] << 8 & 0xff00;
		key |= data[3] << 16 & 0xff0000;
		key |= data[4] << 24 & 0xff000000;
		cipher = new Cipher(key);
	}

}

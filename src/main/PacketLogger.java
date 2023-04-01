package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class PacketLogger {

	private final byte[] data;

	private final int subCode;

	private String packetName = "UNDEFINED";

	public PacketLogger(final byte[] data) {
		this.data = data;
		this.subCode = data[0] & 0xFF;

		final String name = Config.SP_NAME_TABLE.get(subCode);
		if (!StaticUtils.isEmpty(name)) {
			this.packetName = name;
		}
	}

	public void print() {
		// 結尾 *號 不顯示封包描述
		if (packetName.endsWith("*")) {
			return;
		}

		final String describe = String.format("SubCode: %d (%d)", subCode, data.length);

		System.out.println(String.format("[%s] %s", packetName, describe));
		System.out.println(StaticUtils.showHexText(data));
	}

	public void write() {
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			final File folder = new File(".\\ServerPacket");

			if (!folder.exists()) {
				folder.mkdir();
			}

			final File file = new File(folder, String.format("Log%s.txt", Config.RUN_TIMESTAMP));

			fw = new FileWriter(file, true);
			bw = new BufferedWriter(fw);

			final String date = Config.SDF.format(System.currentTimeMillis());
			final String describe = String.format("SubCode: %d (%d)", subCode, data.length);

			bw.write(String.format("[%s] %s\t\t\t\t%s\n", packetName, describe, date));
			bw.write(StaticUtils.showHexText(data) + "\n");
			bw.flush();

		} catch (final Exception e) {
			e.printStackTrace();

		} finally {
			StaticUtils.close(bw, fw);
		}
	}
}

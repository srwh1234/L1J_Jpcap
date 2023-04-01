package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReaderThread extends Thread {
	private final Properties prop;

	public ConfigReaderThread() {
		this.prop = new Properties();
		this.start();
	}

	@Override
	public void run() {
		while (true) {
			try (FileInputStream fis = new FileInputStream("PacketName.ini")) {
				prop.load(fis);

				for (int i = 0; i <= 256; i++) {
					final String name = prop.getProperty("" + i, "").trim();

					if (StaticUtils.isEmpty(name)) {
						continue;
					}
					Config.SP_NAME_TABLE.put(i, name);
				}

			} catch (final IOException e) {
				// System.err.println("Failed to read config file: PacketName.ini");
			}

			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				// ignore
			}
		}
	}
}

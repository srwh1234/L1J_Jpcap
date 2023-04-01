package main;

import java.util.InputMismatchException;
import java.util.Scanner;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;

public class Api {

	public static void main(final String[] args) {
		final Scanner sc = new Scanner(System.in);

		try {
			final NetworkInterface[] devices = JpcapCaptor.getDeviceList();

			for (int i = 0; i < devices.length; i++) {
				System.out.println(i + ":" + devices[i].description);
			}

			if (devices.length <= 0) {
				System.out.println("沒有可用的接口");
				return;
			}

			// 持續讀取設定檔 PacketName.ini
			new ConfigReaderThread();

			System.out.println(String.format("\n請選擇要攔截的網路接口(%d~%d):", 0, devices.length - 1));
			final int index = sc.nextInt();

			System.out.println("開始監聽:\n");

			final JpcapCaptor captor = JpcapCaptor.openDevice(devices[index], 65535, false, 20);

			// 設定過濾條件
			captor.setFilter("tcp port " + 2000, true);

			// 開始監聽
			captor.loopPacket(-1, new ServerPacketReceiver(devices[index]));

		} catch (final InputMismatchException e) {
			System.out.println("請輸入數字");

		} catch (final Exception e) {
			e.printStackTrace();

		} finally {
			sc.close();
		}
	}

}

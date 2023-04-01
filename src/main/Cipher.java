/**
 * License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE").
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR
 * COPYRIGHT LAW IS PROHIBITED.
 *
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 *
 */
package main;

public class Cipher {
	/**
	 * 靜態私用變數 (32位元,靜態唯讀) 該數值只有在Cipher初始化時才會被調用
	 */
	private final static int _1 = 0x9c30d539;

	/**
	 * 初始的解碼數值
	 */
	private final static int _2 = 0x930fd7e2;

	/**
	 * 靜態混淆密碼 (32位元,靜態唯讀) 該數值只有在Cipher初始化時才會被調用
	 */
	private final static int _3 = 0x7c72e993;

	/**
	 * 靜態混淆密碼 (32位元,靜態唯讀) 該數值只有在編碼或解碼時才會被調用
	 */
	private final static int _4 = 0x287effc3;

	/**
	 * 動態加密鑰匙 (位元組陣列長度為8)
	 */
	private final byte[] eb = new byte[8];

	/**
	 * 動態解密鑰匙 (位元組陣列長度為8)
	 */
	private final byte[] db = new byte[8];

	/**
	 * 參考用的封包鑰匙 (位元組陣列長度為256)
	 */
	private final byte[] hb = new byte[256];

	/**
	 * 解密種子暫存
	 */
	private final byte[] ch = new byte[256];

	/**
	 * 初始化
	 *
	 * @param key
	 */
	public Cipher(final int key) {
		byte t = 0;
		int temp = 0;
		final int[] keys = { key ^ _1, _2 };
		keys[0] = Integer.rotateLeft(keys[0], 0x13);
		keys[1] ^= keys[0] ^ _3;

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 4; j++) {
				final byte data = ((byte) (keys[i] >> j * 8 & 0xFF));
				this.db[(i * 4 + j)] = data;
				this.eb[(i * 4 + j)] = data;
			}
		}

		for (int i = 0; i < 256; i++) {
			this.hb[i] = ((byte) i);
		}

		for (int j = 0; j < 256; j++) {
			temp = this.hb[j] + temp + this.eb[(j % 8)] & 0xFF;
			t = this.hb[temp];
			this.hb[temp] = this.hb[j];
			this.hb[j] = t;
		}

		System.arraycopy(this.hb, 0, this.ch, 0, 256);
	}

	/**
	 * 資料加密.
	 *
	 * @param data
	 *            原始數據
	 * @return data 加密後的數據
	 */
	public byte[] encryptHash(final byte[] data) {

		final int length = data.length + 1;
		int b = 0, c = 0;
		byte d = 0;

		for (int a = 1; a < length; ++a) {
			b += hb[a & 0xff];
			b &= 0xff;
			c = a & 0xff;
			d = hb[c];
			hb[c] = hb[b];
			hb[b] = d;
			data[a - 1] ^= hb[(hb[b] + hb[c]) & 0xff];
		}

		return data;
	}

	/**
	 * 資料解密-客戶端使用 (Blowfish)
	 *
	 * @param data
	 * @return
	 */
	public byte[] decryptClient(final byte[] data) {
		data[0] = ((byte) (data[0] ^ (this.db[5] ^ data[1])));

		data[1] = ((byte) (data[1] ^ (this.db[4] ^ data[2])));

		data[2] = ((byte) (data[2] ^ (this.db[3] ^ data[3])));

		data[3] = ((byte) (data[3] ^ this.db[2]));

		int length = data.length;

		for (int i = length - 1; i >= 1; i--) {
			data[i] = ((byte) (data[i] ^ (data[(i - 1)] ^ this.db[(i & 0x7)])));
		}

		data[0] = ((byte) (data[0] ^ this.db[0]));

		length -= 4;

		final byte[] temp = new byte[length];
		System.arraycopy(data, 4, temp, 0, length);

		update(this.db, temp);
		return temp;
	}

	/**
	 * 將客戶端封包進行加密
	 *
	 * @param data
	 * @return
	 */
	public byte[] encryptClient(final byte[] data) {
		final byte[] nd = new byte[data.length + 4];
		System.arraycopy(data, 0, nd, 4, data.length);
		nd[0] ^= eb[0];

		for (int i = 1; i < nd.length; i++) {
			nd[i] ^= nd[i - 1] ^ eb[i & 7];
		}

		nd[3] ^= eb[2];
		nd[2] ^= eb[3] ^ nd[3];
		nd[1] ^= eb[4] ^ nd[2];
		nd[0] ^= eb[5] ^ nd[1];
		update(eb, data);
		return nd;
	}

	/**
	 * 資料解密-伺服器端
	 *
	 * @param data
	 * @return
	 */
	public byte[] decryptServer(final byte[] data) {
		final int length = data.length + 1;
		int b = 0;
		int c = 0;
		int d = 0;
		for (int a = 1; a < length; a++) {
			b = a & 0xFF;
			c += this.ch[b];
			c &= 255;
			d = this.ch[b];
			this.ch[b] = this.ch[c];
			this.ch[c] = ((byte) (d & 0xFF));
			final int i = (a - 1);
			data[i] = ((byte) (data[i] ^ this.ch[(this.ch[b] + this.ch[c] & 0xFF)]));
		}
		return data;
	}

	/**
	 * 將指定的鑰匙進行混淆並與混淆鑰匙相加(_4)
	 *
	 * @param data
	 *            , 受保護的資料
	 * @return data, 原始的資料
	 */
	private void update(final byte[] data, final byte[] ref) {
		for (int i = 0; i < 4; i++) {
			data[i] ^= ref[i];
		}
		final int int32 = (((data[7] & 0xFF) << 24) | ((data[6] & 0xFF) << 16) | ((data[5] & 0xFF) << 8) | (data[4] & 0xFF)) + _4;

		for (int i = 0; i < 4; i++) {
			data[i + 4] = (byte) (int32 >> (i * 8) & 0xff);
		}
	}
}

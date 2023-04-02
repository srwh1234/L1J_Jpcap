package main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

public class StaticUtils {

	private static String fillHex(final int i, final int j) {
		String s = Integer.toHexString(i);
		for (int k = s.length(); k < j; k++) {
			s = (new StringBuilder()).append("0").append(s).toString();
		}
		return s;
	}

	/**
	 * 將byte陣列 做文字顯示的格式化
	 */
	public static String getHexText(final byte[] data) {
		final HashMap<Integer, String> list = findStrings(data);

		final StringBuffer sb = new StringBuffer();
		int byteCount = 0;
		for (int i = 0; i < data.length; i++) {

			// 新的一行開始
			if (byteCount % 16 == 0) {
				sb.append((new StringBuilder()).append(fillHex(i, 4)).append(": ").toString());
			}
			// 內容
			sb.append((new StringBuilder()).append(fillHex(data[i] & 0xff, 2)).append(" ").toString());

			// text
			if (++byteCount == 16) {
				sb.append("   ");

				final int offset = i - 15;
				for (int j = 0; j < 16; j++) {

					final int index = offset + j;
					if (list.containsKey(index)) {
						sb.append(list.get(index));
					}
				}
				sb.append("\r\n");
				byteCount = 0;
			}
		}
		// 最後一行對齊
		final int last = data.length % 16;
		if (last > 0) {
			// 內容推齊
			for (int i = 0; i < 17 - last; i++) {
				sb.append("   ");
			}
			// text
			final int offset = data.length - last;
			for (int i = 0; i < last; i++) {
				final int index = offset + i;
				if (list.containsKey(index)) {
					sb.append(list.get(index));
				}
			}
			sb.append("\r\n");
		}
		return sb.toString();
	}

	/**
	 * 0x00為字串的結尾 *
	 * 用此找出可能是文字的byte陣列,並轉成文字
	 */
	private static HashMap<Integer, String> findStrings(final byte[] data) {
		final HashMap<Integer, String> result = new HashMap<>();
		int index = 1;
		int start = 1;

		while (start < data.length) {

			// 找出下一個0x00
			int end = start;
			while (end < data.length && data[end] != 0x00) {
				end++;
			}

			// 複製陣列並判斷是否文字
			final byte[] bytes = Arrays.copyOfRange(data, start, end);
			final String str = toBig5Encoding(bytes);
			result.put(index, str);

			// 下個循環
			index += bytes.length + 1;
			start = end + 1;
		}
		return result;
	}

	// 中文字
	private static String toBig5Encoding(final byte[] bytes) {
		boolean isBig5 = false;
		boolean isANSI = false;
		String result = "";

		try {
			if (bytes.length <= 2) {
				return result;
			}

			int begin = 0;
			/*
			 常用的 BIG5 內碼區域
			
			0xA140-0xA3BF: 標點符號、希臘字母及特殊符號
			0xA440-0xC67E: 常用漢字，先按筆劃再按部首排序。
			0xC940-0xF9D5: 次常用漢字，亦是先按筆劃再按部首排序。
			 * */

			// 判斷前後2byte是不是Big5
			for (int i = 0; i < bytes.length - 1; i++) {

				// 標點符號
				if ((bytes[i] >= 0x40 && bytes[i] <= 0xBF)//
						&& (bytes[i + 1] >= 0xA1 && bytes[i + 1] <= 0xA3)) {
					begin = i % 2;
					break;
				}

				// 常用漢字
				if ((bytes[i] >= 0x40 && bytes[i] <= 0x7E)//
						&& (bytes[i + 1] >= 0xA4 && bytes[i + 1] <= 0xC6)) {
					begin = i % 2;
					break;
				}
			}

			// 翻譯
			for (int i = begin; i < bytes.length; i += 2) {
				int tmp = 0;
				if (i + 1 >= bytes.length) { // 奇數補0湊一組
					tmp = (bytes[i] << 8) | (0 & 0xff);
				} else {
					tmp = (bytes[i] << 8) | (bytes[i + 1] & 0xff);
				}

				tmp &= 0xFFFF;

				final byte[] bb = new byte[] { (byte) (tmp >>> 8), (byte) tmp };

				// 0xA140-0xA3BF || 0xA440-0xC67E
				if (tmp >= 0xA140 && tmp <= 0xA3BF			//
						|| tmp >= 0xA440 && tmp <= 0xC67E) {
					result += new String(bb, "BIG5");
					isBig5 = true;
				} else {
					result += (bb[0] >= 0x20 && bb[0] < 0x80 && !isBig5) ? (char) bb[0] : "";
					result += (bb[1] >= 0x20 && bb[1] < 0x80 && !isBig5) ? (char) bb[1] : "";
					isANSI = true;
				}
			}

			result = result.trim();

			if (result.isEmpty() || (isANSI && result.length() < 2)) {
				result = "";
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	// XXX 判斷
	/**
	 * 判斷字串是否為空值或空白或全空格
	 */
	public static boolean isEmpty(final String str) {
		if (str == null || str.trim().isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * 判斷一個字串是否為數字
	 */
	public static boolean isNumeric(final String str) {
		if (isEmpty(str)) {
			return false;
		}
		return str.matches("-?\\d+(\\.\\d+)?");
	}

	/**
	 * 判斷一個字串是否為整數數字
	 */
	public static boolean isInteger(final String str) {
		if (isEmpty(str)) {
			return false;
		}
		return str.matches("-?\\d+");
	}

	/**
	 * 判斷一個字串是否由英文字母和數字組成
	 */
	public static boolean isAlphanumeric(final String str) {
		return str.matches("^[a-zA-Z0-9]+$");
	}

	/**
	 * 判斷一個字串是否全由英文字母組成
	 */
	public static boolean isAlphabetic(final String str) {
		return str.matches("^[a-zA-Z]+$");
	}

	// XXX 轉型
	/**
	 * InputStream 轉成 byte[]
	 */
	public static byte[] toByteArray(final InputStream is) throws IOException {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final byte[] buffer = new byte[1024];
		int len;
		while ((len = is.read(buffer)) > -1) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}

	/**
	 * 字串轉int陣列 1,2,3 --> {1,2,3}
	 */
	public static int[] toIntArray(String str) {
		if (isEmpty(str)) {
			return new int[0];
		}
		if (str.endsWith(",")) {
			str = str.substring(0, str.length() - 1);
		}
		final String[] split = str.split(",");

		final int[] result = new int[split.length];

		for (int i = 0; i < split.length; i++) {
			if (split[i].trim().length() == 0) {
				continue;
			}
			result[i] = Integer.parseInt(split[i]);
		}
		return result;
	}

	/**
	 * 字串轉String陣列 a,b,c --> {a,b,c}
	 */
	public static String[] toStringArray(String str) {
		if (isEmpty(str)) {
			return new String[0];
		}

		if (str.endsWith(",")) {
			str = str.substring(0, str.length() - 1);
		}
		final String[] split = str.split(",");

		final String[] result = new String[split.length];

		for (int i = 0; i < split.length; i++) {
			if (split[i].trim().length() == 0) {
				continue;
			}
			result[i] = split[i].trim();
		}
		return result;
	}

	/**
	 * int陣列轉字串 {1,2,3} --> 1,2,3
	 */
	public static String toString(final int[] array) {
		if (array == null) {
			return "";
		}

		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			sb.append(array[i] + ",");
		}
		String result = sb.toString();
		if (result.endsWith(",")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	// XXX 取值

	@SuppressWarnings("unchecked")
	public static <T> T cast(final Object obj) {
		return (T) obj;
	}

	/**
	 * 星期一到星期天用1-7表示
	 */
	public static int getTrueWeek() {
		final Calendar now = Calendar.getInstance();
		// 一周的第一天是否為星期天
		final boolean isFirstSunday = (now.getFirstDayOfWeek() == Calendar.SUNDAY);

		int weekDay = now.get(Calendar.DAY_OF_WEEK);
		// 若一周的第一天是星期天,則-1
		if (isFirstSunday) {
			weekDay = weekDay - 1;
			if (weekDay == 0) {
				weekDay = 7;
			}
		}
		return weekDay;
	}

	/**
	 * 從檔案路徑中取得檔案的副檔名
	 */
	public static String getFileExtension(final String fileName) {
		if (isEmpty(fileName)) {
			return "";
		}
		final int dotIndex = fileName.lastIndexOf(".");
		return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
	}

	/**
	 * 從File中取得檔案的副檔名
	 */
	public static String getFileExtension(final File file) {
		final String fileName = file.getName();
		final int index = fileName.lastIndexOf('.');
		if (index != -1) {
			return fileName.substring(index + 1, fileName.length());
		}
		return "";
	}

	/**
	 * 從檔案路徑中取得檔案名稱+副檔名
	 */
	public static String getFileName(final String filePath) {
		if (isEmpty(filePath)) {
			return "";
		}
		if (filePath.indexOf('.') == -1) {
			return "";
		}
		final File file = new File(filePath);
		return file.getName();
	}

	/**
	 * 從File中取得不含副檔名的檔案名稱
	 */
	public static String getNameWithoutExtension(final File file) {
		final String fileName = file.getName();
		final int index = fileName.lastIndexOf('.');
		if (index != -1) {
			return fileName.substring(0, index);
		}
		return "";
	}

	// XXX 關閉

	/**
	 * 關閉實現 AutoCloseable 接口的物件
	 */
	public static void close(final AutoCloseable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (final Exception e) {
			// ignore
		}
	}

	/**
	 * 關閉實現 AutoCloseable 接口的物件們
	 */
	public static void close(final AutoCloseable... closeables) {
		if (closeables != null) {
			for (final AutoCloseable c : closeables) {
				close(c);
			}
		}
	}

	/** 陣列合併 **/
	public static byte[] concat(final byte[] array1, final byte[] array2) {
		if (array1 == null && array2 == null) {
			return new byte[0];
		}
		if (array1 == null) {
			return array2.clone();
		}
		if (array2 == null) {
			return array1.clone();
		}

		final byte[] newArray = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, newArray, 0, array1.length);
		System.arraycopy(array2, 0, newArray, array1.length, array2.length);
		return newArray;
	}
}

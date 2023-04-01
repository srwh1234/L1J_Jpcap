package main;

import java.text.SimpleDateFormat;
import java.util.HashMap;

public class Config {
	public static String RUN_TIMESTAMP;
	public static SimpleDateFormat SDF = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	public static HashMap<Integer, String> SP_NAME_TABLE = new HashMap<>();

	static {
		RUN_TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis());
	}
}

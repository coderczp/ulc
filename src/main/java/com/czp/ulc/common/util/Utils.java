/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function:工具类
 *
 * @date:2017年3月28日/上午10:39:03
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@SuppressWarnings("restriction")
public class Utils {

	public static final String KEY_ALGORITHM = "DES";
	public static final String key = "A1B2C3D4E5F60708";
	public static final String CIPHER_ALGORITHM = "DES/ECB/PKCS5Padding";
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static SecretKey keyGenerator(String keyStr) throws Exception {
		byte input[] = HexString2Bytes(keyStr);
		DESKeySpec desKey = new DESKeySpec(input);
		return SecretKeyFactory.getInstance("DES").generateSecret(desKey);
	}

	private static int parse(char c) {
		if (c >= 'a')
			return (c - 'a' + 10) & 0x0f;
		if (c >= 'A')
			return (c - 'A' + 10) & 0x0f;
		return (c - '0') & 0x0f;
	}

	// 从十六进制字符串到字节数组转换
	public static byte[] HexString2Bytes(String hexstr) {
		int j = 0;
		byte[] b = new byte[hexstr.length() / 2];
		for (int i = 0; i < b.length; i++) {
			char c0 = hexstr.charAt(j++);
			char c1 = hexstr.charAt(j++);
			b[i] = (byte) ((parse(c0) << 4) | parse(c1));
		}
		return b;
	}

	/**
	 * 加密数据
	 * 
	 * @param data
	 *            待加密数据
	 * @param key
	 *            密钥
	 * @return 加密后的数据
	 */
	public static String encrypt(String data) {
		try {
			Key deskey = keyGenerator(key);
			SecureRandom random = new SecureRandom();
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, deskey, random);
			byte[] results = cipher.doFinal(data.getBytes());
			return new sun.misc.BASE64Encoder().encode(results);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void count(Map<String, Integer> map, String key) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + 1);
		} else {
			map.put(key, 1);
		}
	}

	/**
	 * 解密数据
	 * 
	 * @param data
	 *            待解密数据
	 * @param key
	 *            密钥
	 * @return 解密后的数据
	 */
	public static String decrypt(String data) {
		try {
			Key deskey = keyGenerator(key);
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, deskey);
			return new String(cipher.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(data)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static boolean notEmpty(Object str) {
		return str != null && str.toString().length() > 0;
	}

	public static boolean isEmpty(Collection<?> list) {
		return list == null || list.isEmpty();
	}

	public static byte[] intToBytes(int x) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(x);
		return buffer.array();
	}

	public static int bytesToInt(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.put(bytes);
		buffer.flip();// need flip
		return buffer.getInt();
	}

	public static List<String> readLines(InputStream is) {
		LinkedList<String> lines = new LinkedList<String>();
		try {
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return lines;
	}

	public static void close(AutoCloseable item) {
		try {
			if (item != null) {
				item.close();
				LOG.debug("close:{}", item);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/***
	 * 将小时分钟秒设置为0
	 * 
	 * @param millis
	 * @return
	 */
	public static Date igroeHMSTime(long millis) {
		Calendar ins = Calendar.getInstance();
		ins.setTimeInMillis(millis);
		ins.set(Calendar.HOUR_OF_DAY, 0);
		ins.set(Calendar.MINUTE, 0);
		ins.set(Calendar.SECOND, 0);
		ins.set(Calendar.MILLISECOND, 0);
		return ins.getTime();
	}

	public static byte[] longToBytes(long num) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		return buffer.putLong(num).array();
	}

	public static Long bytesToLong(byte[] values) {
		return ByteBuffer.wrap(values).getLong();
	}

	public static FilenameFilter newFilter(String suffix) {
		return new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(suffix);
			}
		};
	}

	public static int getFileId(File file) {
		String name = file.getName();
		return Integer.parseInt(name.substring(0, name.indexOf(".")));
	}
}

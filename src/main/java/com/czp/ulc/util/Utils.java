/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * Function:工具类
 *
 * @date:2017年6月10日/下午4:03:43
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class Utils {

	public static String decrypt(String text, String key, String charset) throws Exception {
		byte[] keyBase64DecodeBytes = Base64.getDecoder().decode(key);
		DESKeySpec desKeySpec = new DESKeySpec(keyBase64DecodeBytes);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
		Cipher cipher = Cipher.getInstance("DES");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		byte[] textBytes = new sun.misc.BASE64Decoder().decodeBuffer(text);// Base64.getDecoder().decode(text);
		System.out.println(new String(textBytes));
		byte[] bytes = cipher.doFinal(textBytes);
		return new String(bytes, charset);
	}
}

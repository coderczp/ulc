/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.meta;

import java.io.ByteArrayOutputStream;

/**
 * Function:内存缓存
 *
 * @date:2017年5月22日/下午5:02:00
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class MemOutStream extends ByteArrayOutputStream {

	public MemOutStream(int maxLen) {
		super(maxLen);
	}

	public byte[] getBuf() {
		return buf;
	}

	/**
	 * 覆盖指定位置的数据
	 * 
	 * @param offet
	 * @param buf
	 */
	public void orrvide(int offet, byte[] data) {
		System.arraycopy(data, 0, buf, offet, data.length);
	}
}

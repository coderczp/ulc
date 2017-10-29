/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.core;

import org.springframework.validation.BindingResult;

/**
 * Function:参数异常
 *
 * @date:2017年3月28日/下午2:05:51
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class ArgException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private BindingResult error;

	private int code = 1001;

	public ArgException(BindingResult error) {
		super();
		this.error = error;
	}

	public BindingResult getError() {
		return error;
	}

	public int getCode() {
		return code;
	}

}

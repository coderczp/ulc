/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.core.bean;

import java.io.Serializable;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Function:关键词rule
 *
 * @date:2017年3月28日/下午4:56:45
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class KeywordRule implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	/** 要匹配的机器 */
	@NotEmpty(message = "host id is empty")
	private Integer hostId;

	/** 要匹配的文件 */
	@NotEmpty(message = "file is empty")
	private String file;

	/** 包含该关键字则匹配 */
	@NotEmpty(message = "keyword is empty")
	private String keyword;

	/** 包含该关键字则不不匹配,优先级比keyword高 */
	private String exclude = "";

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getExclude() {
		return exclude;
	}

	public void setExclude(String exclude) {
		this.exclude = exclude;
	}

	public Integer getHostId() {
		return hostId;
	}

	public void setHostId(Integer hostId) {
		this.hostId = hostId;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

}

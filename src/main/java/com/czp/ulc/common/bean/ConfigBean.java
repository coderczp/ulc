package com.czp.ulc.common.bean;

import java.io.Serializable;

/**
 * key value配置
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月18日 下午2:52:26</li>
 * 
 * @version 0.0.1
 */

public class ConfigBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String type;

	private String key;

	private String value;

	private Integer id;

	public ConfigBean(String type, String key, String value) {
		this.type = type;
		this.key = key;
		this.value = value;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(String value) {
		this.value = value;
	}

	
	
	public String getType() {
		return type;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public int getIntValue() {
		return Integer.valueOf(value);
	}

}

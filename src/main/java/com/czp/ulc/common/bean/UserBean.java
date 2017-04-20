package com.czp.ulc.common.bean;

import java.io.Serializable;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月18日 下午2:58:10</li>
 * 
 * @version 0.0.1
 */

public class UserBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	private String name;

	private String email;

	private String pwd;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

}

package com.czp.ulc.core.bean;

import java.io.Serializable;

/**
 * 菜单
 *
 * <li>创建人：coder_czp@126.com</li>
 * <li>创建时间：2017年9月18日</li>
 * 
 * @version 0.0.1
 */
public class Menu implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	private String name;

	private String href;

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

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
}

package com.czp.ulc.core.bean;

import java.io.Serializable;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月18日 下午2:58:10</li>
 * 
 * @version 0.0.1
 */
public class UserMenu implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	private String mail;

	/** {@link Menu.id} */
	private Integer menuId;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getMenuId() {
		return menuId;
	}

	public void setMenuId(Integer menuId) {
		this.menuId = menuId;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

}

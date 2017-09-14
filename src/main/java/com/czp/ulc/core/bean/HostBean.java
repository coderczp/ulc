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
 * Function:主机对象
 *
 * @date:2017年3月27日/下午3:54:25
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class HostBean implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_PORT = 22;

	@NotEmpty(message = "host isempty")
	private String host;

	@NotEmpty(message = "name isempty")
	private String name;

	@NotEmpty(message = "user isempty")
	private String user;

	@NotEmpty(message = "pwd isempty")
	private String pwd;

	private int port = DEFAULT_PORT;

	private int status = 0;

	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	@Override
	public String toString() {
		return "HostBean [id=" + id + ", host=" + host + ", name=" + name + ", port=" + port + "]";
	}

}

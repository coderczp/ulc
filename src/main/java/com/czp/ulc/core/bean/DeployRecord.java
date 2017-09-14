package com.czp.ulc.core.bean;

import java.io.Serializable;

import javax.persistence.Id;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年8月9日 下午4:25:33</li>
 * 
 * @version 0.0.1
 */

public class DeployRecord implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private Integer id;

	private String project;

	private String host;

	private String author;
	
	private String status;

	private String time;
	
	private String log;
	
	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

}

package com.czp.ulc.core.bean;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * function
 *
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月18日-下午2:49:13</li>
 * 
 * @version 0.0.1
 */
@Table(name="processor")
public class ProcessorBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	/** 主机ID */
	@Column(name="hostId")
	private Integer hostId;

	@NotEmpty(message = "name is empty")
	private String name;

	/** 进程路径 */
	@NotEmpty(message = "path is empty")
	private String path;

	/** 管理脚本里面需要保护start stop restart方法 */
	private String shell = "service.sh";

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getHostId() {
		return hostId;
	}

	public void setHostId(Integer hostId) {
		this.hostId = hostId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getShell() {
		return shell;
	}

	public void setShell(String shell) {
		this.shell = shell;
	}

}

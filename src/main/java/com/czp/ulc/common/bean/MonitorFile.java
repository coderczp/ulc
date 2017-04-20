package com.czp.ulc.common.bean;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * 要监控的文件
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年3月30日 下午4:05:17</li>
 * 
 * @version 0.0.1
 */

public class MonitorFile implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	/** 主机ID */
	@NotNull(message = "host id is empty")
	private Integer hostId;

	/** 监控的文件 */
	@NotEmpty(message = "file id is empty")
	private String file;

	/** 要排除的文件 */
	private String excludeFile;

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

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getExcludeFile() {
		return excludeFile;
	}

	public void setExcludeFile(String excludeFile) {
		this.excludeFile = excludeFile;
	}

}

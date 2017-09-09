package com.czp.ulc.common.param;

import java.util.Set;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年9月9日/下午6:41:56
 * @copyright coder_czp@126.com
 *
 */
public class QueryParam {

	public long to;
	public long from;
	public Set<String> servers;

	public QueryParam(long to, long from, Set<String> servers) {
		this.to = to;
		this.from = from;
		this.servers = servers;
	}
}

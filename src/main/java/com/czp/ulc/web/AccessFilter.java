/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.util.Utils;

/**
 * Function:访问校验
 *
 * @date:2017年6月10日/上午9:32:32
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class AccessFilter implements Filter {

	private String key;
	/** 需要跳转的登录地址 */
	private String loginUrl;
	private String[] skipUrls;
	private static final long timeout = 1000 * 60 * 60 * 24 * 30l;
	private static Logger LOG = LoggerFactory.getLogger(AccessFilter.class);

	public AccessFilter(String loginUrl, String skipUrls, String key) {
		this.skipUrls = skipUrls.split(",");
		this.loginUrl = loginUrl;
		this.key = key;
	}

	private boolean checkToken(String token, HttpSession session) {
		if (token == null || token.isEmpty())
			return false;
		try {
			String decrypt = Utils.decrypt(token, key, "utf-8");
			LOG.info("decrypt src:{} to:{}", token, decrypt);
			String time = decrypt.substring(decrypt.lastIndexOf(",") + 1, decrypt.length() - 1).trim();
			if (System.currentTimeMillis() - Long.valueOf(time) > timeout) {
				LOG.info("token expire");
				return false;
			}
			session.setAttribute("user", decrypt);
		} catch (Exception e) {
			LOG.error("decrypt error", e);
			return false;
		}
		return true;
	}

	private String getCookie(HttpServletRequest request, String key) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null)
			return null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(key))
				return cookie.getValue();
		}
		return null;
	}

	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain paramFilterChain)
			throws IOException, ServletException {
		HttpServletResponse rep = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;
		String url = req.getRequestURL().toString();
		HttpSession session = req.getSession();
		Object user = session.getAttribute("user");
		LOG.info("url:{},user:{}", url, user);

		if (isSkipUrl(url) || user != null || url.contains(IndexController.CALLBACK)) {
			paramFilterChain.doFilter(req, rep);
			return;
		}
		String token = getCookie(req, IndexController.TOKEN);
		if (!checkToken(token, session)) {
			String uri = req.getRequestURI();
			String ctx = req.getContextPath();
			String callback = url.substring(0, url.indexOf(uri) + ctx.length());
			callback = callback.concat(IndexController.CALLBACK);
			rep.sendRedirect(loginUrl.replace("#{url}", callback));
			return;
		}
		paramFilterChain.doFilter(req, rep);
	}

	private boolean isSkipUrl(String url) {
		for (String string : skipUrls) {
			if (url.contains(string))
				return true;
		}
		return false;
	}

	@Override
	public void init(FilterConfig cfg) throws ServletException {
	}

}

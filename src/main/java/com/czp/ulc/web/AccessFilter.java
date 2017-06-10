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
import java.net.URLDecoder;

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

	/** 需要跳转的登录地址 */
	private String loginUrl;
	public static final String TOKEN = "token";
	public static final String key = "O/KhRvHBBy8=";
	public static final long timeout = 1000 * 60 * 60 * 24 * 30l;
	private static Logger LOG = LoggerFactory.getLogger(AccessFilter.class);

	public AccessFilter(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	private boolean checkToken(String token) {
		if (token == null || token.isEmpty())
			return false;
		try {
			String decrypt = Utils.decrypt(token, key, "utf-8");
			LOG.info("user:{}", decrypt);
			String time = decrypt.substring(decrypt.lastIndexOf(",") + 1, decrypt.length() - 1).trim();
			long end = System.currentTimeMillis();
			if (end - Long.valueOf(time) > timeout) {
				LOG.info("token expire");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private String getToken(HttpServletRequest request) {
		String token = request.getQueryString();
		if (token != null && token.contains(TOKEN))
			return URLDecoder.decode(token.substring(token.lastIndexOf(TOKEN) + TOKEN.length() + 1)).trim();
		return getCookie(request, TOKEN);
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
		LOG.info("url:{}", url);

		if (url.contains("/stop") || session.getAttribute(TOKEN) != null) {
			paramFilterChain.doFilter(req, rep);
			return;
		}

		String token = getToken(req);
		LOG.info("token:{}", token);
		if (!checkToken(token)) {
			String callback = url;
			if (!url.endsWith("/"))
				callback = callback.concat("/");
			if (!callback.contains("/callback"))
				callback = callback.concat("callback");
			String replace = loginUrl.replace("#{url}", callback);
			rep.sendRedirect(replace);
			return;
		} else {
			session.setAttribute(TOKEN, token);
			rep.addCookie(new Cookie(TOKEN, token));
		}
		paramFilterChain.doFilter(req, rep);
	}

	@Override
	public void init(FilterConfig paramFilterConfig) throws ServletException {

	}

}

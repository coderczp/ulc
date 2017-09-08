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

import com.alibaba.fastjson.JSONObject;
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
			LOG.info("token:{}", token);
			if (token.contains("%"))
				token = URLDecoder.decode(token, "utf-8");

			String decrypt = Utils.decrypt(token, key, "utf-8");
			LOG.info("decrypt src:{} to:{}", token, decrypt);
			JSONObject json = JSONObject.parseObject(decrypt);
			long time = json.getLongValue("time");
			if (System.currentTimeMillis() - Long.valueOf(time) > timeout) {
				LOG.info("token expire");
				return false;
			}
			String account = json.getString("account");
			session.setAttribute("user", account);
		} catch (Exception e) {
			LOG.error("decrypt error", e);
			return false;
		}
		return true;
	}

	private String getCookie(HttpServletRequest request, String key) {
		String token = request.getParameter(key);
		if (token != null && !token.isEmpty())
			return token;
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
		LOG.debug("url:{},user:{}", url, user);

		if (isSkipUrl(url) || user != null) {
			paramFilterChain.doFilter(req, rep);
			return;
		}
		if (url.contains(IndexController.CALLBACK)) {
			String token = req.getParameter("token").trim();
			if (token != null && checkToken(token, session)) {
				String baseUrl = getBaseUrl(req, url);
				rep.sendRedirect(baseUrl);
				return;
			}
		}

		String token = getCookie(req, IndexController.TOKEN);
		if (!checkToken(token, session)) {
			String baseUrl = getBaseUrl(req, url);
			String realCallBack = baseUrl.concat(IndexController.CALLBACK);
			rep.sendRedirect(loginUrl.replace("#{url}", realCallBack));
			return;
		}
		paramFilterChain.doFilter(req, rep);
	}

	private String getBaseUrl(HttpServletRequest req, String url) {
		String ctx = req.getContextPath();
		String host = req.getHeader("host");
		String uri = req.getRequestURI();
		// String referer = req.getHeader("referer");
		// if (referer != null) {
		// return referer;
		// }
		if (host != null) {
			return String.format("%s://%s%s", req.getScheme(), host, ctx);
		}
		return url.substring(0, url.indexOf(uri) + ctx.length());
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

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
	private String loginUrl;
	private String[] skipUrls;
	public static final String SESSION_KEY ="user";
	public static final String CALLBACK = "/callback";
	private static final String TOKEN_NAME = "account";
	private static final int COOK_TIMEOUT = 60 * 60 * 24 * 10;
	private static final long AUTH_TIMEOUT = 1000 * 60 * 60 * 24 * 30L;
	private static Logger LOG = LoggerFactory.getLogger(AccessFilter.class);
	private static String[] deviceArray = new String[] { "android", "mac os", "windows phone" };

	public AccessFilter(String loginUrl, String skipUrls, String key) {
		this.skipUrls = skipUrls.split(",");
		this.loginUrl = loginUrl;
		this.key = key;
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
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse rep = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;
		String url = req.getRequestURL().toString();
		HttpSession session = req.getSession();
		Object user = session.getAttribute(SESSION_KEY);
		LOG.debug("url:{},user:{}", url, user);

		if (isSkipUrl(url) || user != null) {
			chain.doFilter(req, rep);
			return;
		}

		if (isAuthSucessCallback(rep, req, url, session)) {
			return;
		}

		if (hasLogin(req, session)) {
			chain.doFilter(request, response);
			return;
		}

		gotoLogin(rep, req, url);

	}

	private void gotoLogin(HttpServletResponse rep, HttpServletRequest req, String url) throws IOException {
		String baseUrl = getCallbackUrl(req, url);
		String realCallBack = baseUrl.concat(CALLBACK);
		String login = loginUrl.replace("#{url}", realCallBack);
		if (isMobileDevice(req.getHeader("User-Agent"))) {
			login = login.replace("redirect_url", "cb");
		}
		rep.sendRedirect(login);
	}

	private boolean hasLogin(HttpServletRequest req, HttpSession session) {
		String token = getCookie(req, TOKEN_NAME);
		if (token == null)
			return false;

		try {
			String account = Utils.decrypt(token);
			session.setAttribute(SESSION_KEY, account);
			return true;
		} catch (Exception e) {
			LOG.error("invalid token", e);
		}
		return false;
	}

	private boolean isAuthSucessCallback(HttpServletResponse rep, HttpServletRequest req, String url,
			HttpSession session) throws IOException {
		if (!url.contains(CALLBACK))
			return false;

		String token = req.getParameter("token");
		if (token == null) {
			return false;
		}
		if (token.contains("%"))
			token = URLDecoder.decode(token, "utf-8");

		String decrypt = Utils.decryptAuth(token, key, "utf-8");
		LOG.info("decrypt auth token to:{}", decrypt);
		JSONObject json = JSONObject.parseObject(decrypt);
		long time = json.getLongValue("time");
		if (System.currentTimeMillis() - Long.valueOf(time) > AUTH_TIMEOUT) {
			LOG.info("token expire {}", json);
			return false;
		}
		String account = json.getString("account");
		session.setAttribute("user", account);
		String baseUrl = getCallbackUrl(req, url);
		String encrypt = Utils.encrypt(account);
		Cookie cookie = new Cookie(TOKEN_NAME, encrypt);
		cookie.setMaxAge(COOK_TIMEOUT);
		rep.addCookie(cookie);
		rep.sendRedirect(baseUrl);
		return true;
	}

	/**
	 * android : 所有android设备 mac os : iphone ipad windows
	 * phone:Nokia等windows系统的手机
	 */
	private static boolean isMobileDevice(String requestHeader) {
		if (requestHeader == null)
			return false;
		requestHeader = requestHeader.toLowerCase();
		for (int i = 0; i < deviceArray.length; i++) {
			if (requestHeader.indexOf(deviceArray[i]) > 0) {
				return true;
			}
		}
		return false;
	}

	private String getCallbackUrl(HttpServletRequest req, String url) {
		String ctx = req.getContextPath();
		String host = req.getHeader("host");
		if (host != null) {
			return String.format("%s://%s%s", req.getScheme(), host, ctx);
		}
		String uri = req.getRequestURI();
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

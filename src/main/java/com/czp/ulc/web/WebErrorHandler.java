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
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ibatis.builder.BuilderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.czp.ulc.core.ArgInvalideException;

/**
 * Function:统一的错误处理器
 *
 * @date:2017年3月28日/下午2:08:13
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@Component
public class WebErrorHandler extends AbstractHandlerExceptionResolver {

	private final static String JSON_TYPE = "application/json;utf-8";
	private static final Logger LOG = LoggerFactory.getLogger(WebErrorHandler.class);

	@Override
	protected ModelAndView doResolveException(HttpServletRequest req, HttpServletResponse resp, Object handler,
			Exception err) {
		try {
			JSONObject res = null;
			Throwable real = err.getCause() == null ? err : err.getCause();
			if (real instanceof ArgInvalideException) {
				res = handleArgError((ArgInvalideException) real);
			} else if (real instanceof BuilderException) {
				res = handleBuilderError((BuilderException) real);
			} else {
				res = handleException(real);
			}
			writeToClient(req, resp, res.toJSONString());
		} catch (Exception e) {
			LOG.error("web error", e);
		}
		return null;
	}

	private JSONObject handleBuilderError(BuilderException real) {
		Throwable e = real.getCause();
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		return handleException(e == null ? real : e);
	}

	private JSONObject handleException(Throwable err) {
		String message = err.getMessage();
		JSONObject json = new JSONObject();
		json.put("error", true);
		json.put("code", 1002);
		json.put("info", message == null ? err.toString() : message);
		return json;
	}

	private JSONObject handleArgError(ArgInvalideException err) {
		StringBuilder info = new StringBuilder();
		List<ObjectError> allErrors = err.getError().getAllErrors();
		for (ObjectError item : allErrors) {
			info.append(item.getDefaultMessage()).append(",");
		}
		JSONObject json = new JSONObject();
		json.put("error", true);
		json.put("info", info);
		json.put("code", err.getCode());
		return json;

	}

	public static void writeToClient(HttpServletRequest req, HttpServletResponse resp, String res) throws IOException {
		String type = req.getContentType();
		resp.setContentType(type == null ? JSON_TYPE : type);
		resp.getWriter().print(res);
		resp.getWriter().flush();
		resp.getWriter().close();
	}
	
	public static void writeJSON(HttpServletResponse resp, JSON res) {
		try {
			resp.setContentType(JSON_TYPE);
			PrintWriter writer = resp.getWriter();
			writer.print(res);
			writer.close();
		} catch (Exception e) {
			LOG.error("web error", e);
		}
	}

}

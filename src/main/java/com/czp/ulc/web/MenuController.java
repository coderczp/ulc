package com.czp.ulc.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.czp.ulc.core.ArgException;
import com.czp.ulc.core.bean.Menu;
import com.czp.ulc.core.bean.UserMenu;
import com.czp.ulc.core.dao.MenuDao;
import com.czp.ulc.core.dao.UserMenuDao;

/**
 * function
 *
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月18日-下午3:34:12</li>
 * 
 * @version 0.0.1
 */
@RestController
@RequestMapping("/menu")
public class MenuController {

	@Autowired
	private MenuDao dao;

	@Autowired
	private UserMenuDao uMdao;

	@RequestMapping("/add")
	public UserMenu addConfigFile(@Valid UserMenu bean, BindingResult result) {
		if (result.hasErrors()) {
			throw new ArgException(result);
		}
		if (uMdao.insertUseGeneratedKeys(bean) < 1) {
			throw new RuntimeException("add bean fail");
		}
		return bean;
	}

	@RequestMapping("/del")
	public UserMenu del(UserMenu arg) {
		Assert.notNull(arg.getId(), "id is required");
		UserMenu inDb = uMdao.selectOne(arg);
		if (inDb == null || uMdao.delete(arg) < 1) {
			throw new RuntimeException("del bean fail");
		}
		return arg;
	}

	@RequestMapping("/userMenu")
	public List<Menu> getUserMenu(HttpServletRequest req) {
		String email = (String) req.getSession().getAttribute(AccessFilter.SESSION_KEY);
		return dao.queryUserMenu(email);
	}

	@RequestMapping("/list")
	public List<Menu> list(UserMenu arg) {
		return uMdao.query(arg);
	}
}

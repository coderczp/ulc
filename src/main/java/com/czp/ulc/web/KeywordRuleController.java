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

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.czp.ulc.core.ArgInvalideException;
import com.czp.ulc.core.bean.KeywordRule;
import com.czp.ulc.core.dao.KeywordRuleDao;

/**
 * Function:关键词接口
 *
 * @date:2017年3月28日/下午6:09:06
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
@RestController
@RequestMapping("/kw")
public class KeywordRuleController {

	@Autowired
	private KeywordRuleDao dao;

	@RequestMapping("/add")
	public KeywordRule add(@Valid KeywordRule rule, BindingResult result) throws Exception {
		if (result.hasErrors()) {
			throw new ArgInvalideException(result);
		}
		dao.insertUseGeneratedKeys(rule);
		return rule;
	}

	@RequestMapping("/list")
	public List<KeywordRule> list(KeywordRule rule) throws Exception {
		return dao.list(rule);
	}

	@RequestMapping("/del")
	public KeywordRule del(KeywordRule rule) throws Exception {
		Assert.notNull(rule.getId(), "id is empty");
		dao.deleteByPrimaryKey(rule.getId());
		return rule;
	}
}

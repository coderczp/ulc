/***
 * 激活指定的菜单
 * @param menuId
 * @returns
 */

var menus = {
	'日志搜索':	'./index.html',
	'进程管理':	'./proc.html',
	'主机管理':	'./proc.html',
	'Info':	'./info.html',
	'pv':	'./pv.html',
};

var procs= {
	'Processor':'all',
	'itrip_rp':'itrip_rp',
	'itrip_wap':'itrip_wap',
	'itrip_pay':'itrip_pay',
	'itrip_shop':'itrip_shop',
	'itrip_main':'itrip_main',
	'service_b2b':'service_b2b',
	'service_pms':'service_pms',
	'itrip_mobile':'itrip_mobile',
	'service_order':'service_order',
	'itrip_openapi':'itrip_openapi',
	'service_product':'service_product',
	'service_booking':'service_booking',
	'itrip_b2b_v2_b2b':'itrip_b2b_v2_b2b',
	'itrip_openapi_b2b':'itrip_openapi_b2b',
	'service_order_email':'service_order_email',
};

var files = {
	'File':'all',
	'run.log':'run.log',
	'db.log':'db.log',
	'dao.log':'dao.log',
	'error.log':'error.log',
	'request.log':'request.log',
	'booking.log':'booking.log',
	'dubborpc.log':'dubborpc.log',
	'dubboclient.log':'dubboclient.log',
	'sitebooking.log':'sitebooking.log',
};

/**将菜单绑定到指定的元素*/
function bindMenus(ulMenuId){
	var menuUl = $(ulMenuId);
	if(!menuUl)alert('ULC not found menu ul');
	var html = '';
	for(var name in menus) {
		var href = menus[name];
		html +='<li><a href="'+href+'">'+name+'</a></li>';
	}
	menuUl.html(html);
}

/**将进程绑定到指定的元素*/
function bindProcs(procSelectId){
	var proc = $(procSelectId);
	if(!proc)alert('ULC not found file select');
	var html = '';
	for(var name in procs) {
		var val = procs[name];
		html +='<option value="'+val+'">'+name+'</option>';
	}
	proc.html(html);
}

/**将文件绑定到指定的元素*/
function bindFiles(fileId){
	var file = $(fileId);
	if(!file)alert('ULC not found file select');
	var html = '';
	for(var name in files) {
		var val = procs[name];
		html +='<option value="'+val+'">'+name+'</option>';
	}
	file.html(html);
}
var files = {
	'all' : 'all',
	'run.log' : 'run.log',
	'db.log' : 'db.log',
	'dao.log' : 'dao.log',
	'error.log' : 'error.log',
	'request.log' : 'request.log',
	'booking.log' : 'booking.log',
	'dubborpc.log' : 'dubborpc.log',
	'dubboclient.log' : 'dubboclient.log',
	'sitebooking.log' : 'sitebooking.log',
};

/** 全局变量 */
var Glob = {
	'hosts' : {},
};

/** 注册Ajax全局错误函数 */
$.ajaxSetup({
	error : function(jqXHR, textStatus, errorMsg) {
		alert('发送错误,请刷新' + (errorMsg || textStatus));
	},
	success : function(data) {
		if (data.error) {
			alert(data.info);
			return false;
		}
	},
});

/** 将菜单绑定到指定的元素 */
function bindMenus(ulMenuId) {
	var menuUl = $(ulMenuId);
	if (!menuUl)
		alert('ULC not found menu ul');

	$.get('./menu/userMenu', function(menus) {
		var html = '';
		for ( var i in menus) {
			var item = menus[i];
			var name = item.name;
			var href = item.href;
			html += '<li><a href="' + href + '">' + name + '</a></li>';
		}
		menuUl.html(html);
	});
}

/** 将进程绑定到指定的元素 */
function bindProcs(procSelectId) {
	var proc = $(procSelectId);
	if (!proc)
		alert('ULC not found file select');
	$.get('./proc/list', function(procs) {
		var html = '<option value="all">all</option>';
		for ( var i in procs) {
			var item = procs[i];
			var name = item.name;
			html += '<option value="' + name + '">' + name + '</option>';
		}
		proc.html(html);
	});
}

/** 将文件绑定到指定的元素 */
function bindFiles(fileId) {
	var file = $(fileId);
	if (!file)
		alert('ULC not found file select');
	var html = '';
	for ( var name in files) {
		var val = files[name];
		html += '<option value="' + val + '">' + name + '</option>';
	}
	file.html(html);
}

/** 加载并绑定主机列表到select */
function asynLoadHost(hostSelectId, callback) {
	$.get('./host/list', function(d) {
		var hosts = Glob.hosts;
		var html = '<option value="">all</option>';
		for ( var i in d) {
			var host = d[i];
			var id = host.id;
			var name = host.name;
			var strId = "" + id;
			html += '<option value=' + host.id + '>' + name + '</option>';
			hosts[strId] = name;
		}
		$(hostSelectId).html(html);

		if (callback)
			callback();
	});
}

/** 将id映射为name */
function mapHostIdToName(id) {
	return Glob.hosts[id] || id;
}

/** 创建button */
function buildBtn(func, name, args) {
	var arg = '';
	for ( var i in args) {
		arg += "\"" + args[i] + "\","
	}
	return "<button onclick='" + func + "(" + arg + ")'>" + name + "</button>";
}
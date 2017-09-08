package com.czp.ulc.web;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年8月8日/下午11:30:09
 * @copyright coder_czp@126.com
 *
 */
@RestController
@RequestMapping("/file")
public class FileUploadConstroller {

	private static File base = new File("deploy");

	public FileUploadConstroller() {
		if (!base.exists())
			base.mkdirs();
	}

	@RequestMapping("/list")
	public Object list() {
		File[] listFiles = listFiles(null);
		JSONArray files = new JSONArray();
		for (File file : listFiles) {
			files.add(file.getName());
		}
		return files;
	}

	public static File[] listFiles(FilenameFilter filter) {
		return filter == null ? base.listFiles() : base.listFiles(filter);
	}

	@PostMapping("/upload")
	public Object handleFileUpload(@RequestParam("file") MultipartFile file) throws Exception {
		JSONObject res = new JSONObject();
		String fileName = file.getOriginalFilename();
		if (fileName == null || fileName.isEmpty()) {
			res.put("code", 500);
			res.put("error", "file is empty");
		} else {
			InputStream in = file.getInputStream();
			Path target = createPath(fileName.trim());
			Files.copy(in, target);
			res.put("code", 200);
			res.put("path", target.toFile().getName());
			in.close();
		}
		return res;
	}

	private Path createPath(String fileName) {
		SimpleDateFormat spf = new SimpleDateFormat("yyyyMMddHHmmss");
		String path = String.format("%s_%s", spf.format(new Date()), fileName);
		return new File(base, path).toPath();
	}

	public static File getPath(String name) {
		return new File(base, name);
	}
}

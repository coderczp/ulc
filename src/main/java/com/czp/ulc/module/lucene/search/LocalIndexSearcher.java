package com.czp.ulc.module.lucene.search;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;

import com.czp.ulc.core.bean.IndexMeta;
import com.czp.ulc.core.dao.IndexMetaDao;
import com.czp.ulc.module.lucene.FileParallelSearch;
import com.czp.ulc.module.lucene.MemIndexBuilder;
import com.czp.ulc.module.mapreduce.MapreduceModule;

/**
 * 整合本地内存和文件搜索
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:48:31</li>
 * 
 * @version 0.0.1
 */

public class LocalIndexSearcher {

	private IndexMetaDao metaDao;
	private MemIndexBuilder memSer;
	private MapreduceModule mrModule;
	private FileParallelSearch fileSearch;

	public void setMrModule(MapreduceModule mrModule) {
		this.mrModule = mrModule;
	}

	public void setMetaDao(IndexMetaDao metaDao) {
		this.metaDao = metaDao;
	}

	public void setMemSer(MemIndexBuilder memSer) {
		this.memSer = memSer;
	}

	public FileParallelSearch getFileSearch() {
		return fileSearch;
	}

	public void setFileSearch(FileParallelSearch fileSearch) {
		this.fileSearch = fileSearch;
	}

	/**
	 * 异步搜索
	 * 
	 * @param task
	 * @return
	 * @throws IOException
	 */
	public void localSearch(SearchTask task) throws IOException {
		int memReturn = memSer.searchInRam(task);
		if (memReturn < task.getQuery().getSize()) {
			fileSearch.search(task, memReturn);
		} else {
			task.getCallback().finish();
		}
	}

	public void searchAll(SearchTask task) throws IOException {
		mrModule.doRemoteSearch(task);
		localSearch(task);
	}

	public IndexMeta getMeta() {
		IndexMeta count = metaDao.count(null);
		if (count == null) {
			return new IndexMeta();
		}
		return count;
	}

	public void localCount(SearchTask search) throws IOException {
		memSer.count(search);
		fileSearch.count(search);
	}

	public Analyzer getAnalyzer() {
		return memSer.getAnalyzer();
	}

}

package com.czp.ulc.module.lucene.search;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import com.czp.ulc.core.bean.IndexMeta;
import com.czp.ulc.core.dao.IndexMetaDao;
import com.czp.ulc.module.lucene.FileParallelSearch;
import com.czp.ulc.module.lucene.MemIndexBuilder;
import com.czp.ulc.module.lucene.SearchCallback;

/**
 * 整合内存和文件搜索
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:48:31</li>
 * 
 * @version 0.0.1
 */

public class LocalIndexSearcher {

	private IndexMetaDao metaDao;
	private MemIndexBuilder memSer;
	private FileParallelSearch parallelFileSearch;

	public void setMetaDao(IndexMetaDao metaDao) {
		this.metaDao = metaDao;
	}

	public void setMemSer(MemIndexBuilder memSer) {
		this.memSer = memSer;
	}

	public void setParallelFileSearch(FileParallelSearch parallelFileSearch) {
		this.parallelFileSearch = parallelFileSearch;
	}

	public void search(SearchCallback search) throws Exception {
		long allDocs = getMeta().getDocs();
		int memMatch = memSer.searchInRam(search);
		if (memMatch >= search.getQuery().getSize()) {
			search.onFinish(allDocs, memMatch);
		} else {
			parallelFileSearch.search(search, allDocs, memMatch);
		}
	}

	public IndexMeta getMeta() {
		IndexMeta count = metaDao.count(null);
		if (count == null) {
			return new IndexMeta();
		}
		return count;
	}

	public Map<String, Long> count(SearchCallback search) throws IOException {
		Map<String, Long> count = memSer.count(search);
		parallelFileSearch.count(search, count);
		return count;
	}

	public Analyzer getAnalyzer() {
		return memSer.getAnalyzer();
	}

}

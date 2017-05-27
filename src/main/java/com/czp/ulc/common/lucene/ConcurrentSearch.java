/* 
 * 创建日期 2016-11-10
 *
 * 成都澳乐科技有限公司版权所有
 * 电话：028-85253121 
 * 传真：028-85253121
 * 邮编：610041 
 * 地址：成都市武侯区航空路6号丰德国际C3
 */
package com.czp.ulc.common.lucene;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.czp.ulc.collect.handler.SearchCallback;
import com.czp.ulc.common.shutdown.ShutdownCallback;
import com.czp.ulc.common.shutdown.ShutdownManager;

/**
 * Function:并行搜索
 *
 * @date:2017年5月27日/下午2:37:04
 * @Author:jeff.cao@aoliday.com
 * @version:1.0
 */
public class ConcurrentSearch implements ShutdownCallback {

	private ExecutorService worker;

	public ConcurrentSearch(int threadSize) {
		this.worker = Executors.newFixedThreadPool(threadSize);
		ShutdownManager.getInstance().addCallback(this);
	}

	/**
	 * 并行搜索
	 * 
	 * @param searchs
	 * @return
	 */
	public void search(List<SearchParam> searchs) {
		if (searchs.isEmpty())
			return;
		SearchParam param0 = searchs.get(0);
		AtomicLong total = new AtomicLong(param0.getDocs());
		for (int i = 0; i < searchs.size(); i++) {
			final SearchParam param = searchs.get(i);
			worker.execute(() -> searchInIndex(param, total));
		}
	}
	
	

	private void searchInIndex(SearchParam param, AtomicLong total) {
		try {
			SearchCallback callBack = param.getCallBack();
			IndexSearcher searcher = param.getSearcher();
			Set<String> feilds = callBack.getFeilds();
			Query query = callBack.getQuery();
			String host = param.getHost();
			TopDocs docs = searcher.search(query, callBack.getSize());
			for (ScoreDoc scoreDoc : docs.scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc, feilds);
				String file = doc.get(DocField.FILE);
				String line = doc.get(DocField.LINE);
				if (!callBack.handle(host, file, line, docs.totalHits, total.get()))
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSystemExit() {
		worker.shutdownNow();
	}
}

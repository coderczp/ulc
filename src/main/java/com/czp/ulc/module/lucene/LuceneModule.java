package com.czp.ulc.module.lucene;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.czp.ulc.core.dao.IndexMetaDao;
import com.czp.ulc.core.dao.LuceneFileDao;
import com.czp.ulc.core.message.MessageCenter;
import com.czp.ulc.module.IModule;
import com.czp.ulc.module.lucene.search.LocalIndexSearcher;
import com.czp.ulc.module.mapreduce.MapreduceModule;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年9月11日 下午4:39:36</li>
 * 
 * @version 0.0.1
 */

@Service
public class LuceneModule implements IModule {

	@Autowired
	private Environment env;

	@Autowired
	private IndexMetaDao metaDao;

	@Autowired
	private LuceneFileDao lFileDao;

	@Autowired
	private MessageCenter mqCenter;

	@Autowired
	private MapreduceModule mrModule;

	private LocalIndexSearcher searcher;

	@Override
	public boolean start(SingletonBeanRegistry ctx) {
		LuceneConfig.config(env);

		File srcDir = LuceneConfig.getDataDir();
		File indexDir = LuceneConfig.getIndexDir();
		Analyzer analyzer = LuceneConfig.ANALYZER;

		FileIndexBuilder fileBuilder = new FileIndexBuilder(srcDir, indexDir, analyzer, metaDao, lFileDao);
		MemIndexBuilder memSer = new MemIndexBuilder(fileBuilder, analyzer);
		FileParallelSearch pFileSearch = new FileParallelSearch(analyzer,lFileDao);

		searcher = new LocalIndexSearcher();
		searcher.setFileSearch(pFileSearch);
		searcher.setMemSer(memSer);
		searcher.setMetaDao(metaDao);
		searcher.setMrModule(mrModule);

		mqCenter.addConcumer(memSer);

		// export local bean to spring context
		ctx.registerSingleton("luceneSearcher", searcher);

		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String name() {
		return "Lucene Module";
	}

	@Override
	public int order() {
		return 0;
	}

}

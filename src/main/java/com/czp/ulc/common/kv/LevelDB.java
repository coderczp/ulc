package com.czp.ulc.common.kv;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.czp.ulc.common.shutdown.ShutdownCallback;

/**
 * Function:KVDB,需要记录每一步的耗时,方便排问题,考虑性能,不采用动态代理
 *
 * @date:2016年6月26日/下午8:44:52
 * @Author:coder_czp@126.com
 * @version:1.0
 */
public class LevelDB implements ShutdownCallback, KVDB {

	private DB db;
	private Options options = new Options();
	private Logger log = LoggerFactory.getLogger(LevelDB.class);
	private static ConcurrentHashMap<String, LevelDB> dbs = new ConcurrentHashMap<String, LevelDB>();

	private LevelDB(String path) {
		try {
			options.compressionType(CompressionType.SNAPPY);
			options.createIfMissing(true);
			db = factory.open(new File(path), options);
			log.info("kvdb is inited,db:{} path:{}", db, path);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static KVDB getGlobDB() {
		return open("./lv_db_glob");
	}

	public static KVDB open(String path) {
		if (!dbs.containsKey(path)) {
			synchronized (dbs) {
				if (!dbs.containsKey(path)) {
					dbs.put(path, new LevelDB(path));
				}
			}
		}
		return dbs.get(path);
	}

	@Override
	public void close() {
		onSystemExit();
	}

	@Override
	public void delGroup(String group) {

	}

	@Override
	public int hincr(String gruop, byte[] key) {
		return 0;
	}

	@Override
	public byte[] hdel(String gruop, byte[] key) {
		return null;
	}

	@Override
	public void hput(String gruop, byte[] key, byte[] value) {

	}

	@Override
	public void hget(String group, byte[] key, byte[] defaultVal) {

	}

	@Override
	public void onSystemExit() {
		Set<Entry<String, LevelDB>> entrySet = dbs.entrySet();
		for (Entry<String, LevelDB> entry : entrySet) {
			try {
				entry.getValue().db.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

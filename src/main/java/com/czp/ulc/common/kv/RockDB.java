//package com.czp.ulc.common.kv;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//
//import org.rocksdb.CompressionType;
//import org.rocksdb.Options;
//import org.rocksdb.RocksDB;
//import org.rocksdb.RocksDBException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.alibaba.fastjson.JSONObject;
//import com.czp.ulc.common.ShutdownCallback;
//import com.czp.ulc.common.util.Utils;
//
///**
// * 请添加描述
// * <li>创建人：Jeff.cao</li>
// * <li>创建时间：2017年4月28日 上午11:02:09</li>
// * 
// * @version 0.0.1
// */
//
//public class RockDB implements ShutdownCallback, KVDB {
//
//	static {
//		RocksDB.loadLibrary();
//	}
//
//	private RocksDB db;
//	private Options options = new Options();
//	private Logger log = LoggerFactory.getLogger(LevelDB.class);
//	private static final List<String> EMPTY = new ArrayList<String>();
//	private static ConcurrentHashMap<String, RockDB> dbs = new ConcurrentHashMap<>();
//
//	private RockDB(String path) {
//		try {
//			options.setCompressionType(CompressionType.BZLIB2_COMPRESSION);
//			options.setCreateIfMissing(true);
//			db = RocksDB.open(options, path);
//			log.info("kvdb is inited,db:{} path:{}", db, path);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	public static RockDB getGlobDB() {
//		return open("./lv_db_glob");
//	}
//
//	public static RockDB open(String path) {
//		RockDB db = dbs.get(path);
//		if (db != null)
//			return db;
//
//		synchronized (dbs) {
//			if (!dbs.containsKey(path)) {
//				dbs.put(path, new RockDB(path));
//			}
//		}
//		return dbs.get(path);
//	}
//
//	@Override
//	public void onSystemExit() {
//		if (db != null)
//			db.close();
//	}
//
//	@Override
//	public boolean append(String key, String values) {
//		doPut(key.getBytes(UTF8), values.getBytes(UTF8));
//		return false;
//	}
//
//	@Override
//	public boolean put(String key, String values) {
//		doPut(key.getBytes(UTF8), values.getBytes(UTF8));
//		return false;
//	}
//
//	@Override
//	public boolean put(String key, long values) {
//		doPut(key.getBytes(UTF8), Utils.longToBytes(values));
//		return false;
//	}
//
//	@Override
//	public String get(String key) {
//		byte[] bs = doGet(key);
//		return bs == null ? null : new String(bs, UTF8);
//	}
//
//	@Override
//	public String get(Integer key) {
//		return null;
//	}
//
//	@Override
//	public String hget(String key, String haskKey) {
//		return null;
//	}
//
//	@Override
//	public void hincr(String key, String haskKey) {
//
//	}
//
//	@Override
//	public JSONObject hgetAll(String key) {
//		return null;
//	}
//
//	@Override
//	public boolean hput(String key, String hashKey, Object obj) {
//		return false;
//	}
//
//	@Override
//	public List<String> getList(String key) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void put(byte[] key, byte[] value) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void put(byte[] key, String value) {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public String get(byte[] key) {
//		return null;
//	}
//
//	@Override
//	public void putInt(Integer key, String value) {
//
//	}
//
//	@Override
//	public long getLong(String key, long dftVal) {
//		return 0;
//	}
//
//	private void doPut(byte[] key, byte[] values) {
//		try {
//			db.put(key, values);
//		} catch (RocksDBException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	private byte[] doGet(String key) {
//		try {
//			return db.get(key.getBytes(UTF8));
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}
//}

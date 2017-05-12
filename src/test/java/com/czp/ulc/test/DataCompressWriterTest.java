//package com.czp.ulc.test;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.stream.Stream;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//import com.czp.ulc.common.meta.CompressDB;
//import com.czp.ulc.common.meta.DataCodec;
//import com.czp.ulc.common.meta.MetaReadWriter;
//import com.czp.ulc.common.meta.MetaReadWriter.Meta;
//
//import junit.framework.Assert;
//
///**
// * 请添加描述
// * <li>创建人：Jeff.cao</li>
// * <li>创建时间：2017年5月3日 上午9:56:01</li>
// * 
// * @version 0.0.1
// */
//
//public class DataCompressWriterTest {
//
//	private MetaReadWriter mw;
//	
//	private CompressDB db;
//
//	@Before
//	public void init() throws Exception {
//		mw = new MetaReadWriter("./log/data");
//		db = new CompressDB("./test");
//	}
//
//	@After
//	public void destory() throws Exception {
//		mw.close();
//		db.close();
//	}
//
//	@Test
//	@SuppressWarnings("deprecation")
//	public void testEncodeId() {
//		byte[] metaId = DataCodec.encodeMetaId(20170508, 11, 1175);
//		Meta meta = DataCodec.decodeMetaId(metaId);
//		Assert.assertEquals(meta.getFileId(), 11);
//		Assert.assertEquals(meta.getDirId(), 20170508l);
//		Assert.assertEquals(meta.getLineNo(), 1175l);
//	}
//
//	@Test
//	public void testReadLineFromUnCompress() throws Exception {
//		List<byte[]> lineRequest = new LinkedList<>();
//		byte[] bs = DataCodec.encodeMetaId(20170511, 0, 12);
//		byte[] bs2 = DataCodec.encodeMetaId(20170511, 0, 19);
//		byte[] bs3 = DataCodec.encodeMetaId(20170511, 0, 59);
//		lineRequest.add(bs);
//		lineRequest.add(bs2);
//		lineRequest.add(bs3);
//		Map<Long, Map<Long, String>> mergeRead = mw.mergeRead(lineRequest);
//		for (Entry<Long, Map<Long, String>> b : mergeRead.entrySet()) {
//			Map<Long, String> value = b.getValue();
//			for (Entry<Long, String> c : value.entrySet()) {
//				System.out.println(c + ":" + c.getValue());
//			}
//		}
//	}
//
//	@Test
//	public void testLoadLine() throws IOException {
//		byte[] uuid = CompressDB.encodeUUID(17511, (short) 0, 11);
//		System.out.println(db.get(uuid));
//	}
//
//	@Test
//	public void testWrite() throws Exception {
//		Stream<String> lines = Files.lines(new File("./log/data/20170511/0.log").toPath());
//		lines.forEach(line -> {
//			try {
//				db.put(line);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		});
//		lines.close();
//	}
//}

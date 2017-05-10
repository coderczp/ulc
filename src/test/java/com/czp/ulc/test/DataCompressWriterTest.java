package com.czp.ulc.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.czp.ulc.common.meta.MetaReadWriter;
import com.czp.ulc.common.meta.MetaReadWriter.Meta;
import com.czp.ulc.common.util.Utils;

import junit.framework.Assert;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年5月3日 上午9:56:01</li>
 * 
 * @version 0.0.1
 */

public class DataCompressWriterTest {

	private MetaReadWriter mw;

	@Before
	public void init() throws Exception {
		mw = new MetaReadWriter("./log/data");
	}

	@After
	public void destory() throws Exception {
		mw.close();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testEncodeId() {
		byte[] metaId = MetaReadWriter.encodeMetaId(20170508, 11, 1175);
		Meta meta = MetaReadWriter.decodeMetaId(metaId);
		Assert.assertEquals(meta.getFileId(), 11);
		Assert.assertEquals(meta.getDirId(), 20170508l);
		Assert.assertEquals(meta.getLineNo(), 1175l);
	}

	@Test
	public void testReadLine() throws Exception {
		List<byte[]> lineRequest = new LinkedList<>();
		byte[] bs = MetaReadWriter.encodeMetaId(20170510, 0, 3408);
		lineRequest.add(bs);
		Map<Long, Map<Long, String>> mergeRead = mw.mergeRead(lineRequest);
		for (Entry<Long, Map<Long, String>> b : mergeRead.entrySet()) {
			System.out.println(b.getValue());
		}
	}

	@Test
	public void testLoadLine() throws IOException {
		// File file = new File("./tmp");
		// if (!file.exists())
		// return;
		// System.out.println(mw.loadLineCount(file));
	}

	@Test
	public void testWrite() throws Exception {
		// Stream<String> lines = Files.lines(new File("./tmp/0.log").toPath());
		// lines.forEach(line -> {
		// try {
		// mw.write(line);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// });
		// lines.close();
		// testReadLine();
	}
}

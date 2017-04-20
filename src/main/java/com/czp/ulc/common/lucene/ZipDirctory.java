package com.czp.ulc.common.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;

/**
 * @dec Function
 * @author coder_czp@126.com
 * @date 2017年4月7日/下午5:19:33
 * @copyright coder_czp@126.com
 *
 */
public class ZipDirctory extends BaseDirectory {

	private ZipFile file;
	private ZipOutputStream zos;
	private ArrayList<ZipEntry> tempFile = new ArrayList<ZipEntry>();
	private ArrayList<String> deleteFiles = new ArrayList<String>();
	private Map<String, String> map = new HashMap<String, String>();

	protected ZipDirctory(LockFactory lockFactory, String dir) {
		super(lockFactory);
		try {
			File f = new File(dir);
			if (!f.exists()) {
				this.zos = new ZipOutputStream(new FileOutputStream(f));
				ZipEntry zi = new ZipEntry("tmp_file.text");
				zos.putNextEntry(zi);
				zos.closeEntry();
				zos.close();
			}

			this.file = new ZipFile(dir);
			//this.zos = new ZipOutputStream(new FileOutputStream(dir));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] listAll() throws IOException {
		Enumeration<? extends ZipEntry> entries = file.entries();
		LinkedList<String> list = new LinkedList<String>();
		while (entries.hasMoreElements()) {
			ZipEntry e = entries.nextElement();
			list.add(e.getName());
		}
		String[] strings = new String[list.size()];
		list.toArray(strings);
		return strings;
	}

	@Override
	public void deleteFile(String name) throws IOException {
		deleteFiles.add(name);
	}

	@Override
	public long fileLength(String name) throws IOException {
		if (deleteFiles.contains(name))
			throw new FileNotFoundException(name);
		return file.getEntry(name).getSize();
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		ZipEntry item = new ZipEntry(name);
		zos.putNextEntry(item);
		return new ZipIndexOutput(name, name, zos);
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		String name = "tmp_" + prefix + (System.nanoTime()) + suffix;
		ZipEntry item = new ZipEntry(name);
		zos.putNextEntry(item);
		tempFile.add(item);
		return new ZipIndexOutput(name, name, zos);
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		for (ZipEntry item : tempFile) {
			for (String string : names) {
				if (string.equals(item.getName())) {
					item.setExtra(new byte[0]);
				}
			}
			for (String name : deleteFiles) {
				if (name.equals(item.getName())) {
					item.setExtra(new byte[0]);
				}
			}
		}
		zos.flush();
	}

	@Override
	public void rename(String source, String dest) throws IOException {
		map.put(source, dest);
	}

	@Override
	public void syncMetaData() throws IOException {
		zos.flush();
	}

	@Override
	public IndexInput openInput(String name, IOContext context) throws IOException {
		if (deleteFiles.contains(name))
			throw new FileNotFoundException(name);

		return new ZipIndexInput(name, file, 0, -1);
	}

	@Override
	public void close() throws IOException {
		zos.close();
		file.close();
	}

//	public static void main(String[] args) throws IOException {
//		String path = "/Users/itrip/Downloads/ulc/src/main/java/com/czp/ulc/collect/handler/ErrorLogHandler.java";
//		Analyzer analyzer = new StandardAnalyzer();
//		IndexWriterConfig conf2 = new IndexWriterConfig(analyzer);
//		conf2.setOpenMode(OpenMode.CREATE_OR_APPEND);
//		conf2.setUseCompoundFile(true);
//		FSDirectory dir = FSDirectory.open(Paths.get("lucene_dex"));
//		IndexWriter fileWriter = new IndexWriter(dir, conf2);
//		List<String> readAllLines = Files.readAllLines(Paths.get(path));
//		for (String string : readAllLines) {
//			Document doc = new Document();
//			doc.add(new TextField("id", string, Store.YES));
//			fileWriter.addDocument(doc);
//		}
//		fileWriter.commit();
//		fileWriter.close();

//		ZipDirctory d = new ZipDirctory(new SingleInstanceLockFactory(), "/Users/itrip/Downloads/ulc/test.zip");
//		DirectoryReader reader = DirectoryReader.open(d);
//
//		FuzzyQuery fq = new FuzzyQuery(new Term("id", "AtomicInteger"));
//		IndexSearcher searcher = new IndexSearcher(reader);
//		TopDocs docs = searcher.search(fq, 10);
//		for (ScoreDoc scoreDoc : docs.scoreDocs) {
//			Document doc = reader.document(scoreDoc.doc);
//			System.out.println(doc.get("id"));
//		}
//		System.out.println(docs.totalHits);
//
//	}
}

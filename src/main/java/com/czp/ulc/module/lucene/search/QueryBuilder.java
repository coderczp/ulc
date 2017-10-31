package com.czp.ulc.module.lucene.search;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

import com.czp.ulc.module.lucene.DocField;
import com.czp.ulc.module.lucene.RangeQueryParser;
import com.czp.ulc.util.Utils;
import com.czp.ulc.web.QueryCondtion;

/**
 * 请添加描述
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年10月31日</li>
 * 
 * @version 0.0.1
 */

public class QueryBuilder {

	public static Query getMemQuery(Analyzer analyzer, QueryCondtion cdt) {
		return build(analyzer, cdt, true);
	}

	public static Query getFileQuery(Analyzer analyzer, QueryCondtion cdt) {
		return build(analyzer, cdt, false);
	}

	private static Query build(Analyzer analyzer, QueryCondtion cdt, boolean addHost) {
		try {
			RangeQueryParser parser = new RangeQueryParser(DocField.ALL_FEILD, analyzer);
			parser.addSpecFied(DocField.TIME, LongPoint.class);
			StringBuilder sb = new StringBuilder(
					String.format("%s:[%s TO %s]", DocField.TIME, cdt.getStart(), cdt.getEnd()));
			if (Utils.notEmpty(cdt.getProc())) {
				sb.append(String.format(" AND %s:%s", DocField.FILE, cdt.getProc()));
			}
			if (Utils.notEmpty(cdt.getFile())) {
				sb.append(String.format(" AND %s:%s", DocField.FILE, cdt.getFile()));
			}
			if (Utils.notEmpty(cdt.getQ())) {
				sb.append(String.format(" AND %s:%s", DocField.LINE, cdt.getQ()));
			}
			if (addHost) {
				Set<String> hosts = cdt.getHosts();
				if (!hosts.isEmpty()) {
					sb.append(String.format(" AND %s:(", DocField.HOST));
					int size = hosts.size() - 1, i = 0;
					for (String string : hosts) {
						sb.append(string);
						if (i++ < size) {
							sb.append(" OR ");
						}
					}
					sb.append(")");
				}
			}
			return parser.parse(sb.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

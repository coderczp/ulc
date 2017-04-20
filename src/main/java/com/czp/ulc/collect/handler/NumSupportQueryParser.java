package com.czp.ulc.collect.handler;

import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;

/**
 * 支持数值查询的QueryParser
 * <li>创建人：Jeff.cao</li>
 * <li>创建时间：2017年4月13日 上午9:58:51</li>
 * <p>
 * Fields Lucene支持多字段数据，当你在查询的时候你可以指定一个字段查询，也可以使用默认的字段。你可以使用 字段名 + “：” +
 * 查询词来指定字段名搜索。举个例子，让我们假定Lucene的索引中含有两个字段，Title字段和Text字段，其中Text字段是默认字段，当你想找
 * 到一篇文档其中标题包含“The Right Way”同时文本中包含“go”，你可以输入： title:"The Right Way" AND
 * text:go 或者： title:" The Right Way " AND go
 * 如果字段是默认字段的话，在查询语法中可以不需要显式指定。注意，使用默认字段有可能会造成如下的结果： title:Do it right
 * 以上查询将查找标题中含有“Do”，Text字段字段中含有“it”和“right”的文档，因为Text是默认字段，所以如果想要查找Title中完整包含的很用引号引起来。
 * 
 * 二、模糊查询 Term Modifiers Lucene支持在Term中使用通配符来支持模糊查询。
 * 
 * Wildcard Searches [类：org.apache.lucene.search.WildcardQuery]
 * Lucene支持单个或者多个字符的通配符查询，匹配单一字符使用符号“?”，匹配多个字符使用符号“*”。
 * “?”通配符将查找所有满足通过一个字符替换后符合条件的文档。比如：搜索“test”和“text”你可以使用： te?t
 * “*”通配符将查询0个或者多个字符替换后符合条件的。举例来说，查询test，tests或者tester，你可以使用一下字符串来搜索： test*
 * 当然，你也可以将“*”放在字符的中间 te*t 注意：你不能将“*”和“?”放在第一个字符来查询。（Lucene应该是出于性能考虑，所以不支持该功能）
 * 
 * Fuzzy Searches [org.apache.lucene.search.FuzzyQuery]
 * Lucene支持基于编辑距离算法的模糊搜索，你可以使用波浪符号“~”放在查询词的后面，比如搜索一个与“roam”拼写相近的词可以使用： roam~
 * 该查询将寻找类似“foam”和“roams”等的词语。也可以说是相似度查询。
 * 
 * Proximity Searches [org.apache.lucene.search.PrefixQuery]
 * Lucene支持指定距离查询，你可以使用波浪号“~”加数字在查询词后。举例来说搜索“apache”和“jakarta”距离10个字符以内，你可以使用如下语法：
 * "jakarta apache"~10
 * 通过这个语法支持，我们可以单字索引，分词查询，分词完后，满足每个词的单字必须间距为1。这样可以保证100%的召回率，但是在索引方面将造成索引臃肿，同时查询速度也将在某程度上降低，一般来说，在150W文章数据到200W数据的时候性能将会明显的降低。
 * 
 * Range Searches [org.apache.lucene.search.RangeQuery]
 * 范围查询允许你指定某个字段最大值和最小值，查询在二者之间的所有文档。范围查询可以包含或者不包含最大值和最小值，排序是按照字典顺序来排序的。
 * mod_date:[20020101 TO 20030101]
 * 这个将查找满足mode_date字段在大于等于20020101，小于等于20030101范围的所有文档，注意：范围查询并不是为日期字段专设的，你也可以对非日期字段进行范围查询。
 * title:{Aida TO Carmen}
 * 这个将查找所有标题在Aida和Carmen之间但不包含Aida和Carmen的文档。包含最大值和最小值的查询使用方括号，排除则使用花括号。
 * 
 * 
 * 
 * 三、优先级 Boosting a Term
 * Lucene支持给不同的查询词设置不同的权重。设置权重使用“^”符号，将“^”放于查询词的尾部，同时跟上权重值，权重因子越大，该词越重要。设置权重允许你通过给不同的查询词设置不同的权重来影响文档的相关性，假如你在搜索：
 * jakarta apache 如果你认为“jakarta”在查询时中更加重要，你可以使用如下语法： jakarta^4 apache
 * 这将使含有Jakarta的文档具有更高的相关性，同样你也可以给短语设置权重如下： "jakarta apache"^4 "jakarta lucene"
 * 在默认情况下，权重因子为1，当然权重因子也可以小于1。
 * 
 * 四、Term操作符 Boolean operators 布尔操作符可以将多个Term合并为一个复杂的逻辑查询。Lucene支持AND， +，OR，NOT，
 * -作为操作符号。注意，所有的符号必须为大写。
 * 
 * OR OR操作符默认的连接操作符。这意味着，当没有给多个Term显式指定操作符时，将使用OR，只要其中一个Term含有，则可以查询出文档，这跟逻辑符
 * 号||的意思相似。假设我们查询一个文档含有“jakarta apache”或者“jakarta”时，我们可以使用如下语法： "jakarta
 * apache" jakarta 或者 "jakarta apache" OR jakarta
 * 
 * AND AND操作符规定必须所有的Term都出现才能满足查询条件，这跟逻辑符号&&意思相似。如果我们要搜索一个文档中同时含有“jakarta
 * apache”和“jakarta lucene”，我们可以使用如下语法： "jakarta apache" AND "jakarta lucene"
 * 
 * +
 * +操作符规定在其后的Term必须出现在文档中，也就是查询词中的MUST属性。举个例子来说，当我们要查询一个文档必须包含“jakarta”，同时可以包含也可以不包含“lucene”时，我们可以使用如下语法：
 * +jakarta apache
 * 
 * NOT NOT操作符规定查询的文档必须不包含NOT之后的Term，这跟逻辑符号中的!相似。当我们要搜索一篇文档中必须含有“jakarta
 * apache”同时不能含有“Jakarta lucene”时，我们可以使用如下查询； "jakarta apache" NOT "jakarta
 * lucene" 注意：NOT操作符不能使用在单独Term中，举例来说，以下查询将返回无结果： NOT "jakarta apache"
 * 
 * - -操作符排除了包含其后Term的文档，跟NOT有点类似，假设我们要搜索“Jakarta apache”但不包含“Jakarta
 * lucene”时，我们使用如下语法： "jakarta apache" -"jakarta lucene"
 * 
 * Grouping
 * Lucene支持使用圆括号来将查询表达式分组，这将在控制布尔控制查询中非常有用。举例来说：当搜索必须含有“website”，另外必须含有“jakarta”和“apache”之一，我们可以用如下语法：
 * (jakarta OR apache) AND website 这种语法对消除歧义，确保查询表达式的正确性具有很大的意义。
 * 
 * Field Grouping Lucene支持对字段用圆括号来进行分组，当我们要查询标题中含有“return”和“pink
 * ranther”时，我们可以使用如下语法： title:(+return +"pink panther")
 * 
 * Escaping Special Characters Lucene支持转义查询中的特殊字符，以下是Lucene的特殊字符清单： + - && || !
 * ( ) { } [ ] ^ " ~ * ? : \ 转义特殊字符我们可以使用符号“\”放于字符之前。比如我们要搜索(1+1):2，我们可以使用如下语法：
 * \(1\+1\)\:2
 * 
 * Tips: QueryParser.escape(q) 可转换q中含有查询关键字的字符！如：* ,? 等
 * </p>
 * 
 * @version 0.0.1
 */

public class NumSupportQueryParser extends MultiFieldQueryParser {

	private HashMap<String, Class<?>> maps = new HashMap<>();

	public NumSupportQueryParser(String[] fields, Analyzer analyzer) {
		super(fields, analyzer);
	}

	/**
	 * 需要特殊处理的域
	 * 
	 * @param name
	 * @param type
	 */
	public void addSpecFied(String name, Class<?> type) {
		maps.put(name, type);
	}

	@Override
	protected org.apache.lucene.search.Query newRangeQuery(String field, String part1, String part2,
			boolean startInclusive, boolean endInclusive) {
		Class<?> class1 = maps.get(field);
		if (LongPoint.class.equals(class1)) {
			return LongPoint.newRangeQuery(field, Long.valueOf(part1), Long.valueOf(part2));
		} else if (IntPoint.class.equals(class1)) {
			return IntPoint.newRangeQuery(field, Integer.valueOf(part1), Integer.valueOf(part2));
		} else {
			return super.newRangeQuery(field, part1, part2, startInclusive, endInclusive);
		}
	}

}

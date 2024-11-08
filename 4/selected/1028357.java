package org.wportal.search;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.highlight.QueryHighlightExtractor;
import org.wportal.util.HtmlUtil;
import java.util.*;

public class LuceneSearcher extends LuceneBase {

    /**
     * ������ʾ��ʽ
     */
    private String hiliteStartTag = "<font color='red'>";

    private String hiliteEndTag = "</font>";

    private int showLength = 200;

    public void setHiliteStartTag(String hiliteStartTag) {
        this.hiliteStartTag = hiliteStartTag;
    }

    public void setHiliteEndTag(String hiliteEndTag) {
        this.hiliteEndTag = hiliteEndTag;
    }

    public int getShowLength() {
        return showLength;
    }

    public void setShowLength(int showLength) {
        this.showLength = showLength;
    }

    private static String[] QUERY_FIELDS = new String[] { "title", "content" };

    public List search(String queryString) {
        List resultList = Collections.EMPTY_LIST;
        try {
            Query query = MultiFieldQueryParser.parse(queryString, QUERY_FIELDS, analyzer);
            IndexReader reader = getIndexReader();
            long tick = System.currentTimeMillis();
            query = query.rewrite(reader);
            Searcher searcher = new IndexSearcher(reader);
            tick = System.currentTimeMillis();
            Hits hits = searcher.search(query);
            tick = System.currentTimeMillis();
            resultList = new ArrayList();
            QueryHighlightExtractor highlighter = new QueryHighlightExtractor(query, analyzer, hiliteStartTag, hiliteEndTag);
            int highlightFragmentSizeInBytes = 40;
            int maxNumFragmentsRequired = 4;
            String fragmentSeparator = "...";
            for (int i = 0; i < hits.length(); i++) {
                Document doc = hits.doc(i);
                Map fields = new LinkedHashMap();
                for (Enumeration e = doc.fields(); e.hasMoreElements(); ) {
                    Field field = (Field) e.nextElement();
                    String name = field.name();
                    String text = field.stringValue();
                    if (name.equals("title")) {
                        text = highlighter.getBestFragments(text, 999999, 1, fragmentSeparator);
                        if (StringUtils.isEmpty(text)) text = field.stringValue();
                    } else if (name.equals("content")) {
                        text = highlighter.getBestFragments(text, highlightFragmentSizeInBytes, maxNumFragmentsRequired, fragmentSeparator);
                        if (StringUtils.isEmpty(text)) text = HtmlUtil.truncate(field.stringValue(), showLength);
                    }
                    fields.put(name, text);
                }
                resultList.add(fields);
            }
        } catch (Exception e) {
            logger.error("LuceneSearcher.search", e);
        }
        return resultList;
    }
}

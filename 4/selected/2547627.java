package com.primeton.fbsearch;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import net.paoding.analysis.analyzer.PaodingAnalyzer;
import net.paoding.analysis.knife.PaodingMaker;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

public class SearchFiles {

    public static void PrefixQuery() throws IOException, ParseException {
        PrefixQuery query = new PrefixQuery(new Term("body", "FLEX"));
        String indexHome = IndexFiles.INDEX_HOME;
        IndexSearcher searcher = new IndexSearcher(indexHome);
        Hits hits = searcher.search(query);
        for (int i = 0; i < hits.length(); i++) {
            System.out.println("** 结果=" + i);
            System.out.println("score=" + hits.score(i));
            System.out.println("path=" + hits.doc(i).get("path"));
            System.out.println("modified=" + hits.doc(i).get("modified"));
        }
        System.out.println("hits length=" + hits.length() + " finish!");
    }

    public static void Query() throws IOException, ParseException {
        Hits hits = null;
        String queryString = "规范";
        Query query = null;
        String indexHome = IndexFiles.INDEX_HOME;
        IndexSearcher searcher = new IndexSearcher(indexHome);
        IndexFiles index = new IndexFiles();
        try {
            QueryParser qp = new QueryParser("name", index.panalyzer);
            query = qp.parse(queryString);
        } catch (ParseException e) {
        }
        if (searcher != null) {
            hits = searcher.search(query, new Sort("name_sort"));
            if (hits.length() > 0) {
                System.out.println("找到:" + hits.length() + " 个结果!");
                for (int i = 0; i < hits.length(); i++) {
                    Document doc = hits.doc(i);
                    System.out.println("** 结果=" + i);
                    System.out.println("score=" + hits.score(i));
                    System.out.println("path=" + doc.get("path"));
                    System.out.println("name=" + doc.get("name"));
                    System.out.println("modified=" + doc.get("modified"));
                    System.out.println(" ");
                }
            }
        }
        System.out.println("SearchFile is finish!");
    }

    public static void search() throws Exception {
        String indexHome = IndexFiles.INDEX_HOME;
        IndexSearcher searcher = new IndexSearcher(indexHome);
        Date date1 = new Date();
        PaodingAnalyzer panalyzer = new PaodingAnalyzer();
        panalyzer.setKnife(PaodingMaker.make());
        panalyzer.setMode(1);
        QueryParser paser = new QueryParser("body", panalyzer);
        Query query = paser.parse("普元");
        IndexReader reader = searcher.getIndexReader();
        query.rewrite(reader);
        HashSet terms = new HashSet();
        query.extractTerms(terms);
        Iterator it = terms.iterator();
        while (it.hasNext()) {
            Term term = (Term) it.next();
            TermDocs tDocs = reader.termDocs(term);
            while (tDocs.next()) {
                Document tmp = searcher.getIndexReader().document(tDocs.doc());
                System.out.println("\t出现次数：" + tDocs.freq() + "\t符合的文件" + tmp.get("path"));
            }
        }
        Date date2 = new Date();
        System.out.println("search()用时：" + (date2.getTime() - date1.getTime()) + "秒");
    }

    public static void QueryTerm() throws IOException, ParseException {
        Hits hits = null;
        try {
            IndexFiles index = new IndexFiles();
            String indexHome = IndexFiles.INDEX_HOME;
            IndexSearcher searcher = new IndexSearcher(indexHome);
            Term t = new Term("name", "ppt");
            Query query = new TermQuery(t);
            if (searcher != null) {
                hits = searcher.search(query);
                if (hits.length() > 0) {
                    System.out.println("找到:" + hits.length() + " 个结果!");
                    for (int i = 0; i < hits.length(); i++) {
                        Document doc = hits.doc(i);
                        System.out.println("** 结果=" + i);
                        System.out.println("score=" + hits.score(i));
                        System.out.println("path=" + doc.get("path"));
                        System.out.println("name=" + doc.get("name"));
                        System.out.println("modified=" + doc.get("modified"));
                        System.out.println(" ");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("SearchFile is finish!");
    }

    public static void main(String[] args) throws Exception {
        Query();
    }
}

package util.ir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.LockObtainFailedException;
import util.io.DirectoryInput;
import util.io.ZipInput;
import util.parser.DBPedia;
import util.parser.wikiXML.AnchorExtractor;
import util.wikiXML.objectModel.WikiLink;
import util.wikiXML.objectModel.WikiPage;

public class IndexerAnchorText implements Constants {

    /**
	 * @param args
	 */
    private String index = "/media/sata/data/indexes/wikiXML_anchorText_white/";

    private IndexWriter writer = null;

    private IndexReader reader = null;

    private IndexSearcher searcher = null;

    private QueryParser q = null;

    Hashtable<String, String> wiki_titles = new Hashtable<String, String>();

    private int limit = (int) (Integer.MAX_VALUE * 0.01);

    /**
	 * Index zip files in the directory specified in 'path'
	 * 
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 * @throws ParseException
	 * 
	 * 
	 */
    public void indexDirectory(String path) throws CorruptIndexException, LockObtainFailedException, IOException, ParseException {
        LinkedList<String> files = DirectoryInput.listDirectory(path, "zip");
        writer = new IndexWriter(index, new WhitespaceAnalyzer(), false, MaxFieldLength.UNLIMITED);
        reader = IndexReader.open(writer.getDirectory(), true);
        searcher = new IndexSearcher(reader);
        writer.setMergeScheduler(new org.apache.lucene.index.SerialMergeScheduler());
        writer.setRAMBufferSizeMB(320);
        writer.setMergeFactor(10);
        writer.setMaxFieldLength(Integer.MAX_VALUE);
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        q = new QueryParser(FIELD_ID, analyzer);
        for (int i = 0; i < files.size(); i++) {
            String zipPath = files.get(i);
            ZipInput zipFiles = new ZipInput(zipPath);
            LinkedList<InputStream> streams = zipFiles.getZipFilesInputStream();
            System.out.println("Indexing zip file:" + zipPath);
            for (int j = 0; j < streams.size(); j++) {
                WikiPage wiki = AnchorExtractor.parseWikiFile(streams.get(j));
                indexWikiXMLAnchorText(wiki);
            }
            writer.commit();
        }
        writer.optimize();
    }

    public void indexDirectory_hashing(String path) throws CorruptIndexException, LockObtainFailedException, IOException, ParseException {
        LinkedList<String> files = DirectoryInput.listDirectory(path, ".zip");
        for (int i = 0; i < files.size(); i++) {
            String suffix = files.get(i);
            suffix = suffix.substring(suffix.lastIndexOf("/"), suffix.length());
            writer = new IndexWriter(index + "/" + suffix, new WhitespaceAnalyzer(), true, MaxFieldLength.UNLIMITED);
            writer.setMergeScheduler(new org.apache.lucene.index.SerialMergeScheduler());
            writer.setRAMBufferSizeMB(320);
            writer.setMergeFactor(10);
            writer.setMaxFieldLength(Integer.MAX_VALUE);
            String zipPath = files.get(i);
            ZipInput zipFiles = new ZipInput(zipPath);
            LinkedList<InputStream> streams = zipFiles.getZipFilesInputStream();
            System.out.println("Indexing zip file:" + zipPath);
            Hashtable<String, StringBuilder> hash = new Hashtable<String, StringBuilder>();
            Hashtable<String, StringBuilder> hash_raw = new Hashtable<String, StringBuilder>();
            for (int j = 0; j < streams.size(); j++) {
                WikiPage wiki = AnchorExtractor.parseWikiFile(streams.get(j));
                updateHashWithAnchorText(wiki, hash_raw, hash);
            }
            indexAnchorHashes(writer, hash_raw, hash);
            hash.clear();
            hash_raw.clear();
        }
    }

    public void indexDirectory_hashing2(String path) throws CorruptIndexException, LockObtainFailedException, IOException, ParseException {
        LinkedList<String> files = DirectoryInput.listDirectory(path, ".zip");
        HashSet<String> current = new HashSet<String>();
        for (int i = 0; i < files.size(); i++) {
            String temp = "file:" + files.get(i);
            if (!current.contains(temp.toLowerCase())) {
                String suffix = files.get(i);
                suffix = suffix.substring(suffix.lastIndexOf("/"), suffix.length());
                writer = new IndexWriter(index + "/" + suffix, new WhitespaceAnalyzer(), true, MaxFieldLength.UNLIMITED);
                writer.setMergeScheduler(new org.apache.lucene.index.SerialMergeScheduler());
                writer.setRAMBufferSizeMB(320);
                writer.setMergeFactor(10);
                writer.setMaxFieldLength(Integer.MAX_VALUE);
                String zipPath = files.get(i);
                ZipInput zipFiles = new ZipInput(zipPath);
                LinkedList<InputStream> streams = zipFiles.getZipFilesInputStream();
                System.out.println("Indexing zip file:" + zipPath);
                Hashtable<String, StringBuilder> hash = new Hashtable<String, StringBuilder>();
                Hashtable<String, StringBuilder> hash_raw = new Hashtable<String, StringBuilder>();
                for (int j = 0; j < streams.size(); j++) {
                    WikiPage wiki = AnchorExtractor.parseWikiFile(streams.get(j));
                    updateHashWithAnchorText(wiki, hash_raw, hash);
                }
                indexAnchorHashes(writer, hash_raw, hash);
                hash.clear();
                hash_raw.clear();
                zipFiles.close();
            } else {
                System.out.println(files.get(i) + " ya existe");
            }
        }
    }

    private void indexAnchorHashes(IndexWriter writer2, Hashtable<String, StringBuilder> hashRaw, Hashtable<String, StringBuilder> hash) throws CorruptIndexException, IOException {
        Enumeration<String> keys = hash.keys();
        while (keys.hasMoreElements()) {
            Document doc = new Document();
            String key = keys.nextElement();
            String anchor = hash.get(key).toString();
            String anchor_raw = hashRaw.get(key).toString();
            doc.add(new Field(FIELD_ANCHOR, anchor, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field(FIELD_ANCHOR_RAW, anchor_raw, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(FIELD_ID, key, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(WIKIPEDIA_TITLE, wiki_titles.get(key), Field.Store.YES, Field.Index.NOT_ANALYZED));
            writer2.addDocument(doc);
        }
        writer2.commit();
        writer2.optimize();
        writer2.close();
    }

    private void updateHashWithAnchorText(WikiPage wiki, Hashtable<String, StringBuilder> hash_raw, Hashtable<String, StringBuilder> hash) {
        for (int i = 0; wiki != null && wiki.links != null && i < wiki.links.size(); i++) {
            WikiLink link = wiki.links.get(i);
            if (link != null && link.getPageId() != null && link.getText() != null && !link.getText().equals("null")) {
                if (!wiki_titles.containsKey(link.getPageId())) {
                    wiki_titles.put(link.getPageId(), link.getPageName());
                }
                if (hash.containsKey(link.getPageId())) {
                    StringBuilder text = hash.get(link.getPageId());
                    StringBuilder text1 = hash_raw.get(link.getPageId());
                    if (text1.length() < limit) {
                        text = text.append("\t" + link.getText());
                        hash.put(link.getPageId(), text);
                        text1 = text1.append("\t\"" + link.getText() + "\"");
                        hash_raw.put(link.getPageId(), text1);
                    } else {
                        System.out.println("Length limit: " + link.getHref());
                    }
                } else {
                    StringBuilder text = new StringBuilder(link.getText());
                    hash.put(link.getPageId(), text);
                    text = new StringBuilder("\t" + link.getText() + "\t");
                    hash_raw.put(link.getPageId(), text);
                }
            }
        }
    }

    private void indexWikiXMLAnchorText(WikiPage wiki) throws CorruptIndexException, IOException, ParseException {
        for (int i = 0; wiki != null && wiki.links != null && i < wiki.links.size(); i++) {
            WikiLink link = wiki.links.get(i);
            if (link != null && link.getPageId() != null) {
                writer.commit();
                Hits hits = searcher.search(q.parse(link.getPageId()));
                System.out.println("Link: " + link.getPageId());
                if (hits != null && hits.length() > 0) {
                    System.out.println("updating...");
                    Document doc = hits.doc(0);
                    String anchor_raw = doc.get(FIELD_ANCHOR_RAW);
                    if (anchor_raw.length() < limit) {
                        anchor_raw += "\"" + link.getText() + "\"";
                    } else {
                        System.out.println("Anchor text limit :" + link.getPageId());
                    }
                    String anchor = doc.get(FIELD_ANCHOR);
                    if (anchor.length() < limit) {
                        anchor += link.getText();
                    } else {
                        System.out.println("Anchor text limit :" + link.getPageId());
                    }
                    Document doc_updated = createDocumentLucene(link, anchor_raw, anchor);
                    writer.updateDocument(new Term(FIELD_ID, link.getId()), doc_updated);
                    writer.commit();
                } else {
                    System.out.println("creating...");
                    Document doc_updated = createDocumentLucene(link, "\"" + link.getText() + "\"", link.getText());
                    writer.addDocument(doc_updated);
                }
            }
        }
    }

    private Document createDocumentLucene(WikiLink link, String anchor_raw, String anchor) {
        Document doc = new Document();
        doc.add(new Field(FIELD_ANCHOR, anchor, Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field(FIELD_ANCHOR_RAW, anchor_raw, Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_ID, link.getPageId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(WIKIPEDIA_TITLE, link.getPageName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        return doc;
    }

    public static void main(String[] args) throws CorruptIndexException, LockObtainFailedException, IOException, ParseException {
        String path = "/media/sata/data/wikipedia/en/wikixml-20080724/";
        IndexerAnchorText indexer = new IndexerAnchorText();
        indexer.indexDirectory_hashing2(path);
    }
}

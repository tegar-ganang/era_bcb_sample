package util.ir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import util.io.FileInput;
import util.io.ZipInput;
import util.parser.ParseHTML;
import util.parser.html.objects.Anchor;
import util.parser.wikiXML.AnchorExtractor;
import util.wikiXML.objectModel.WikiPage;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;

public class IndexHopitalSites {

    private static final String FIELD_TEXT = "text";

    private static final String FIELD_ANCHOR = "anchor";

    private static final String FIELD_ANCHOR_RAW = "anchor_raw";

    private static final String FIELD_URL = "source";

    private List<File> list = new ArrayList<File>();

    Hashtable<String, String> target_anchor = new Hashtable<String, String>();

    Hashtable<String, Integer> anchor_freq = new Hashtable<String, Integer>();

    HashSet<String> anchor_stop = new HashSet<String>();

    public IndexHopitalSites(String path) {
        File folder = new File(path);
        getFiles(folder, list);
        System.out.println("list.size = " + list.size());
    }

    private String index = "/media/sata__/data/indexes/hospital_sites_anchor/";

    private IndexWriter writer = null;

    private IndexReader reader = null;

    private IndexSearcher searcher = null;

    private QueryParser q = null;

    public String anchorText(ParseHTML parser) {
        LinkedList<Anchor> lista = parser.getAnchor();
        String total = "";
        for (int i = 0; lista != null && i < lista.size(); i++) {
            Anchor a = lista.get(i);
            System.out.println(a.getSource() + "\t" + a.getTarget() + "\t" + a.getAnchor().trim());
            String an = a.getAnchor().replaceAll("\\s+", " ");
            total = total + an.trim() + "\n";
        }
        return total.trim();
    }

    public static void standardSearch(String question, String field, String visualizedfield, String index, Hashtable<String, Float> hash, int max) throws ParseException, CorruptIndexException, IOException {
        String querystr = question;
        DutchAnalyzer analyzer = new DutchAnalyzer();
        Query q = new QueryParser(field, analyzer).parse(querystr);
        IndexSearcher searcher = new IndexSearcher(index);
        System.out.println("Searching for: " + q.toString(field));
        Hits hits = searcher.search(q);
        System.out.println("> ");
        int end = Math.min(hits.length(), max);
        for (int i = 0; i < end; i++) {
            Document doc = hits.doc(i);
            String name = doc.get(visualizedfield);
            if (name != null) {
                System.out.println((i + 1) + ". " + name + " (score " + hits.score(i) + ", docid 000000");
                if (hash.containsKey(name)) {
                    Float f = hash.get(name);
                    f = f + hits.score(i);
                } else {
                    hash.put(name, hits.score(i));
                }
            } else {
                System.out.println((i + 1) + ". " + "No name for this document");
            }
        }
        System.out.println("\n" + hits.length() + " matching documents");
        searcher.close();
    }

    public void indexHTMLFiles() throws CorruptIndexException, IOException {
        writer = new IndexWriter(index, new DutchAnalyzer(), true, MaxFieldLength.UNLIMITED);
        reader = IndexReader.open(writer.getDirectory(), true);
        searcher = new IndexSearcher(reader);
        writer.setMergeScheduler(new org.apache.lucene.index.SerialMergeScheduler());
        writer.setRAMBufferSizeMB(320);
        writer.setMergeFactor(10);
        writer.setMaxFieldLength(Integer.MAX_VALUE);
        for (int i = 0; i < list.size(); i++) {
            String path = list.get(i).getPath();
            boolean pass = false;
            if (path.matches("(.)*\\.[a-z]{3,4}$")) {
                pass = true;
                if (path.endsWith("html") || path.endsWith("htm") || path.endsWith("php") || path.endsWith("jps") || path.endsWith("asp")) {
                    pass = false;
                }
            }
            if (pass) continue;
            ParseHTML parser = new ParseHTML(list.get(i));
            if (parser.source == null) continue;
            Document doc = new Document();
            String text = parser.getBodyText();
            doc.add(new Field(FIELD_TEXT, text, Field.Store.YES, Field.Index.ANALYZED));
            String anchor = anchorText(parser);
            doc.add(new Field(FIELD_ANCHOR, anchor, Field.Store.YES, Field.Index.ANALYZED));
            doc.add(new Field(FIELD_ANCHOR_RAW, anchor, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(FIELD_URL, parser.source, Field.Store.YES, Field.Index.NOT_ANALYZED));
            System.out.println("Indexing file:" + list.get(i).getPath());
            writer.addDocument(doc);
            writer.commit();
        }
        System.out.println("Optimizing index...");
        writer.optimize();
        writer.close();
    }

    public void readAnchorFromFile() {
        String path = "/home/sergio/Documents/hospital/health_websites/raw_anchor_text.txt";
        FileInput in = new FileInput(path);
        String line = in.readString();
        target_anchor = new Hashtable<String, String>();
        anchor_freq = new Hashtable<String, Integer>();
        while (line != null) {
            line = line.toLowerCase();
            String fields[] = line.split("\t");
            if (fields.length < 3) continue;
            String url = fields[1];
            String anchor = fields[2];
            target_anchor.put(url, anchor);
            if (anchor_freq.containsKey(anchor)) {
                anchor_freq.put(anchor, anchor_freq.get(anchor) + 1);
            } else {
                anchor_freq.put(anchor, 1);
            }
            line = in.readString();
        }
    }

    public void readStopAnchor() {
        String path = "/home/sergio/Documents/hospital/health_websites/anchor_stop_words.txt";
        FileInput in = new FileInput(path);
        String line = in.readString();
        anchor_stop = new HashSet<String>();
        while (line != null) {
            line = line.toLowerCase().trim();
            anchor_stop.add(line);
            line = in.readString();
        }
    }

    public boolean isStopAnchor(String anchor) {
        Iterator<String> iter = anchor_stop.iterator();
        while (iter.hasNext()) {
            String stop = iter.next();
            stop = stop.toLowerCase().trim();
            if (anchor.contains(stop)) return true;
        }
        return false;
    }

    public boolean isValidAnchor(String anchor) {
        if (anchor.contains("@") || anchor.contains(".nl") || anchor.contains("www.")) {
            return false;
        }
        if (anchor.matches("!|[|]|<|>|{|}")) {
            return false;
        }
        return true;
    }

    public void indexAnchorHospitalFiles() throws CorruptIndexException, IOException {
        writer = new IndexWriter(index, new DutchAnalyzer(), true, MaxFieldLength.UNLIMITED);
        readAnchorFromFile();
        readStopAnchor();
        reader = IndexReader.open(writer.getDirectory(), true);
        searcher = new IndexSearcher(reader);
        writer.setMergeScheduler(new org.apache.lucene.index.SerialMergeScheduler());
        writer.setRAMBufferSizeMB(320);
        writer.setMergeFactor(10);
        writer.setMaxFieldLength(Integer.MAX_VALUE);
        Enumeration<String> enu = target_anchor.keys();
        while (enu.hasMoreElements()) {
            String anchor = enu.nextElement();
            String url = target_anchor.get(anchor);
            int freq = anchor_freq.get(anchor);
            if (!isStopAnchor(anchor) && freq > 2 && !isValidAnchor(anchor)) {
                Document doc = new Document();
                doc.add(new Field(FIELD_TEXT, anchor, Field.Store.YES, Field.Index.ANALYZED));
                doc.add(new Field(FIELD_ANCHOR, anchor, Field.Store.YES, Field.Index.ANALYZED));
                doc.add(new Field(FIELD_ANCHOR_RAW, anchor, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(FIELD_URL, url, Field.Store.YES, Field.Index.NOT_ANALYZED));
                System.out.println("Indexing file:" + anchor);
                writer.addDocument(doc);
                writer.commit();
            }
        }
        System.out.println("Optimizing index...");
        writer.optimize();
        writer.close();
    }

    public void extractTextFromFiles() {
        for (int i = 0; i < list.size(); i++) {
            String path = list.get(i).getPath();
            boolean pass = false;
            if (path.matches("(.)*\\.[a-z]{3,4}$")) {
                pass = true;
                if (path.endsWith("html") || path.endsWith("htm") || path.endsWith("php") || path.endsWith("jps") || path.endsWith("asp")) {
                    pass = false;
                }
            }
            if (pass) continue;
            ParseHTML parser = new ParseHTML(list.get(i));
            if (parser.source == null) continue;
            Document doc = new Document();
            String text = parser.getBodyText();
            text = text.replaceAll("\n", ". ");
            text = text.replaceAll("\t", " ");
            System.out.println(list.get(i).getPath() + "\t" + text);
        }
    }

    public static void extractText() {
        File folder = new File("/home/sergio/data/crawler/hospital/http:/");
        String path = "/home/sergio/data/crawler/hospital/http:/";
        IndexHopitalSites indexer = new IndexHopitalSites(path);
        indexer.extractTextFromFiles();
    }

    public static void extractAnchor() {
        File folder = new File("/home/sergio/data/crawler/hospital/http:/");
        String path = "/home/sergio/data/crawler/hospital/http:/";
        IndexHopitalSites indexer = new IndexHopitalSites(path);
        indexer.extractAnchorFromFiles();
    }

    public void extractAnchorFromFiles() {
        for (int i = 0; i < list.size(); i++) {
            String path = list.get(i).getPath();
            boolean pass = false;
            if (path.matches("(.)*\\.[a-z]{3,4}$")) {
                pass = true;
                if (path.endsWith("html") || path.endsWith("htm") || path.endsWith("php") || path.endsWith("jps") || path.endsWith("asp")) {
                    pass = false;
                }
            }
            if (pass) continue;
            ParseHTML parser = new ParseHTML(list.get(i));
            if (parser.source == null) continue;
            LinkedList<Anchor> lista = parser.getAnchor();
            String total = "";
            for (int j = 0; lista != null && j < lista.size(); j++) {
                Anchor a = lista.get(j);
                System.out.println(a.getSource() + "\t" + a.getTarget() + "\t" + a.getAnchor().trim());
                String an = a.getAnchor().replaceAll("\\s+", " ");
                total = total + an.trim() + "\n";
            }
        }
    }

    public static void index() {
        File folder = new File("/home/sergio/data/crawler/hospital/http:/");
        String path = "/home/sergio/data/crawler/hospital/http:/";
        IndexHopitalSites indexer = new IndexHopitalSites(path);
        try {
            indexer.indexHTMLFiles();
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void indexAnchor() {
        String path = "/home/sergio/data/crawler/hospital/http:/";
        IndexHopitalSites indexer = new IndexHopitalSites(path);
        try {
            indexer.indexAnchorHospitalFiles();
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void search(String query) throws CorruptIndexException, ParseException, IOException {
        String index = "/media/sata__/data/indexes/hospital_sites";
        Hashtable<String, Float> hash = new Hashtable<String, Float>();
        standardSearch(query, FIELD_ANCHOR, FIELD_URL, index, hash, 10);
    }

    public static void main(String[] args) {
        indexAnchor();
    }

    private static void getFiles(File folder, List<File> list) {
        folder.setReadOnly();
        File[] files = folder.listFiles();
        for (int j = 0; j < files.length; j++) {
            if (!files[j].isDirectory()) list.add(files[j]);
            if (files[j].isDirectory()) getFiles(files[j], list);
        }
    }
}

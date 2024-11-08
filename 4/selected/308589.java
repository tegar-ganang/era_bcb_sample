package pt.utl.ist.lucene.treceval.geoclef.parser;

import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.DocumentFactory;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import pt.utl.ist.lucene.utils.Dom4jUtil;
import pt.utl.ist.lucene.treceval.geoclef.Globals;

/**
 * @author Jorge Machado
 * @date 10/Nov/2008
 * @see pt.utl.ist.lucene.treceval.geoclef
 */
public class GeoParserOutputFileMonitor {

    static int counter = 0;

    static int fileNumber = 0;

    static FileOutputStream fout = null;

    static String outputDir;

    static int numberOfRecordsInFile = 100;

    public static void initMonitor(String dir, int skip) {
        fileNumber = skip;
        counter = 0;
        outputDir = dir;
        new File(outputDir).mkdirs();
    }

    private static FileOutputStream getOut() throws IOException {
        if (fout == null || counter % numberOfRecordsInFile == 0) {
            flushClose();
            fout = new FileOutputStream(outputDir + "\\geoParse" + fileNumber + ".xml");
            fileNumber++;
            fout.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes());
            fout.write("<geoParsedDocs>".getBytes());
            counter++;
            return fout;
        }
        counter++;
        return fout;
    }

    public static void flushClose() throws IOException {
        if (fout != null) {
            fout.write("</geoParsedDocs>".getBytes());
            fout.flush();
            System.out.println("Channel Size:" + fout.getChannel().size());
            fout.close();
            fout = null;
        }
    }

    public static class ElementProb {

        public Element elem;

        public float prob;

        public ElementProb(Element elem, float prob) {
            this.elem = elem;
            this.prob = prob;
        }
    }

    static void cleanLowProbEntries(Element doc) {
        XPath entryCollectionXpath = doc.createXPath("//gp:EntryCollection");
        entryCollectionXpath.setNamespaceURIs(Globals.namespacesOutput);
        Element collection = (Element) entryCollectionXpath.selectSingleNode(doc);
        XPath entryXpath = collection.createXPath("./gp:GazetteerEntry");
        entryXpath.setNamespaceURIs(Globals.namespacesOutput);
        List<Element> nodes = entryXpath.selectNodes(collection);
        HashMap<String, ElementProb> maxProbs = new HashMap<String, ElementProb>();
        List<Element> toRemove = new ArrayList<Element>();
        for (Element entry : nodes) {
            String probStr = entry.attributeValue("probability");
            String entryIdStr = entry.attributeValue("entryID");
            float prob = Float.parseFloat(probStr);
            ElementProb maxProbForEntry = maxProbs.get(entryIdStr);
            if (maxProbForEntry == null) {
                maxProbForEntry = new ElementProb(entry, prob);
                maxProbs.put(entryIdStr, maxProbForEntry);
            } else if (prob > maxProbForEntry.prob) {
                toRemove.add(maxProbForEntry.elem);
                maxProbForEntry.prob = prob;
                maxProbForEntry.elem = entry;
            } else toRemove.add(entry);
        }
        for (Element toRemoveElem : toRemove) {
            collection.remove(toRemoveElem);
        }
    }

    public static synchronized void writeGeoParseElement(String docid, Element geoParse) throws IOException {
        cleanLowProbEntries(geoParse);
        Element docElem = DocumentFactory.getInstance().createElement("DOC", Globals.localNamespace);
        docElem.addAttribute("DOCNO", docid);
        docElem.add(geoParse);
        Dom4jUtil.writeDontCloseStream(docElem, getOut());
    }
}

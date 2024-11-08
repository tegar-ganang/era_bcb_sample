package com.sun.java.help.search;

import javax.help.search.SearchItem;
import java.io.*;
import java.util.Vector;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Locale;
import java.awt.datatransfer.DataFlavor;
import java.lang.reflect.Method;

/**
 * This class established a SearchEnvironment for JavaHelp classes
 *
 * @version	1.11	04/19/98
 * @author Jacek R. Ambroziak
 * @author Roger D. Brinkley
 */
class SearchEnvironment {

    private IntegerArray concepts;

    private IntegerArray offsets;

    private byte[] allLists;

    private IntegerArray concepts3;

    private IntegerArray offsets3;

    private byte[] allChildren;

    private IntegerArray documents;

    private IntegerArray offsets2;

    private IntegerArray titles;

    private RAFFile positionsFile = null;

    private byte[] positions;

    private ByteArrayDecompressor compr;

    private BtreeDict tmap;

    private URL base;

    private String indexDir;

    public SearchEnvironment(String indexDir, URL hsBase) throws Exception {
        this.base = hsBase;
        this.indexDir = indexDir;
        readFromDB();
        compr = new ByteArrayDecompressor(null, 0);
    }

    public int fetch(String conceptName) throws Exception {
        return tmap.fetch(conceptName);
    }

    public String fetch(int conceptID) throws Exception {
        return tmap.fetch(conceptID);
    }

    public SearchItem makeItem(QueryHit hit) throws Exception {
        Vector concepts = new Vector();
        int[] conceptIDs = hit.getArray();
        for (int i = 0; i < conceptIDs.length; i++) {
            if (conceptIDs[i] > 0) {
                concepts.addElement(fetch(conceptIDs[i]));
            }
        }
        int begin = hit.getBegin();
        int end = hit.getEnd();
        return new SearchItem(this.base, fetch(titles.at(hit.getDocument())), Locale.getDefault().toString(), fetch(documents.at(hit.getDocument())), hit.getScore(), begin, end, concepts);
    }

    public String hitToString(QueryHit hit) throws Exception {
        StringBuffer result = new StringBuffer();
        result.append(hit.getScore());
        result.append(" ");
        result.append(fetch(documents.at(hit.getDocument())));
        if (false) {
            result.append(" ");
            result.append(fetch(titles.at(hit.getDocument())));
        }
        result.append(" [");
        int begin = hit.getBegin();
        int end = hit.getEnd();
        result.append(begin);
        result.append(", ");
        result.append(end);
        result.append("], {");
        int[] concepts = hit.getArray();
        for (int i = 0; i < concepts.length; i++) {
            if (concepts[i] > 0) result.append(fetch(concepts[i])); else result.append("--");
            if (i < concepts.length - 1) result.append(", ");
        }
        result.append("}");
        return result.toString();
    }

    public byte[] getPositions(int docId) throws java.io.IOException {
        int offset = offsets2.at(docId);
        int upto;
        if (docId + 1 == offsets2.cardinality()) {
            upto = (int) positionsFile.length();
        } else {
            upto = offsets2.at(docId + 1);
        }
        positions = new byte[upto - offset];
        positionsFile.seek(offset);
        positionsFile.read(positions, 0, upto - offset);
        return positions;
    }

    public int getDocumentIndex(int docId) {
        return offsets2.at(docId);
    }

    public void close() throws java.io.IOException {
        positionsFile.close();
    }

    public boolean occursInText(int concept) {
        return concepts.indexOf(concept) >= 0;
    }

    public NonnegativeIntegerGenerator getDocumentIterator(int concept) {
        int index = concepts.indexOf(concept);
        if (index >= 0) return new ConceptList(allLists, offsets.at(index)); else return null;
    }

    public NonnegativeIntegerGenerator getChildIterator(int concept) {
        int index = concepts3.indexOf(concept);
        if (index >= 0) return new ConceptList(allChildren, offsets3.at(index)); else return null;
    }

    public void getChildren(int concept, IntegerArray array) throws Exception {
        int index = concepts3.indexOf(concept);
        if (index >= 0) {
            int where = offsets3.at(index);
            compr.initReading(allChildren, where + 1);
            compr.ascDecode(allChildren[where], array);
        }
    }

    public int getConceptLength(int concept) throws Exception {
        return tmap.fetch(concept).length();
    }

    private URL getURL(String s) throws MalformedURLException {
        URL hsBase = this.base;
        String dir = this.indexDir;
        URL back;
        URL baseURL = null;
        File file;
        if (hsBase == null) {
            file = new File(dir);
            if (file.exists()) {
                if (File.separatorChar != '/') {
                    dir = dir.replace(File.separatorChar, '/');
                }
                if (dir.lastIndexOf('/') != dir.length() - 1) {
                    dir = dir.concat("/");
                }
                debug("file:" + dir);
                baseURL = new URL("file", "", dir);
            } else {
                baseURL = new URL(dir);
            }
        }
        if (hsBase != null) {
            back = new URL(hsBase, dir + "/" + s);
        } else {
            back = new URL(baseURL, s);
        }
        return back;
    }

    private void readChildrenData() throws Exception {
        URL url;
        URLConnection connect;
        BufferedInputStream in;
        try {
            url = getURL("CHILDREN.TAB");
            connect = url.openConnection();
            InputStream ois = connect.getInputStream();
            if (ois == null) {
                concepts3 = new IntegerArray(1);
                return;
            }
            in = new BufferedInputStream(ois);
            int k1 = in.read();
            concepts3 = new IntegerArray(4096);
            StreamDecompressor sddocs = new StreamDecompressor(in);
            sddocs.ascDecode(k1, concepts3);
            int k2 = in.read();
            offsets3 = new IntegerArray(concepts3.cardinality() + 1);
            offsets3.add(0);
            StreamDecompressor sdoffsets = new StreamDecompressor(in);
            sdoffsets.ascDecode(k2, offsets3);
            in.close();
            url = getURL("CHILDREN");
            connect = url.openConnection();
            ois = connect.getInputStream();
            if (ois == null) {
                concepts3 = new IntegerArray(1);
                return;
            }
            in = new BufferedInputStream(ois);
            int length = connect.getContentLength();
            allChildren = new byte[length];
            in.read(allChildren);
            in.close();
        } catch (MalformedURLException e) {
            concepts3 = new IntegerArray(1);
        } catch (FileNotFoundException e2) {
            concepts3 = new IntegerArray(1);
        } catch (IOException e2) {
            concepts3 = new IntegerArray(1);
        }
    }

    private void readFromDB() throws Exception {
        URL url;
        URLConnection connect;
        BufferedInputStream in = null;
        Schema schema = new Schema(base, indexDir, false);
        BtreeDictParameters params = new BtreeDictParameters(schema, "TMAP");
        params.readState();
        tmap = new BtreeDict(params);
        readChildrenData();
        url = getURL("DOCS.TAB");
        connect = url.openConnection();
        in = new BufferedInputStream(connect.getInputStream());
        int k1 = in.read();
        concepts = new IntegerArray(4096);
        StreamDecompressor sddocs = new StreamDecompressor(in);
        sddocs.ascDecode(k1, concepts);
        int k2 = in.read();
        offsets = new IntegerArray(concepts.cardinality() + 1);
        offsets.add(0);
        StreamDecompressor sdoffsets = new StreamDecompressor(in);
        sdoffsets.ascDecode(k2, offsets);
        in.close();
        url = getURL("DOCS");
        connect = url.openConnection();
        in = new BufferedInputStream(connect.getInputStream());
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buff = new byte[512];
        int i = 0;
        while ((i = in.read(buff)) != -1) {
            data.write(buff, 0, i);
        }
        allLists = data.toByteArray();
        in.close();
        url = getURL("OFFSETS");
        connect = url.openConnection();
        in = new BufferedInputStream(connect.getInputStream());
        k1 = in.read();
        documents = new IntegerArray(4096);
        sddocs = new StreamDecompressor(in);
        sddocs.ascDecode(k1, documents);
        k2 = in.read();
        offsets2 = new IntegerArray(documents.cardinality() + 1);
        sdoffsets = new StreamDecompressor(in);
        sdoffsets.ascDecode(k2, offsets2);
        int k3 = in.read();
        titles = new IntegerArray(documents.cardinality());
        StreamDecompressor sdtitles = new StreamDecompressor(in);
        sdtitles.decode(k3, titles);
        in.close();
        RAFFileFactory factory = RAFFileFactory.create();
        url = getURL("POSITIONS");
        positionsFile = factory.get(url, false);
    }

    /**
   * For printf debugging.
   */
    private static boolean debugFlag = false;

    private static void debug(String str) {
        if (debugFlag) {
            System.out.println("SearchEnvironment: " + str);
        }
    }
}

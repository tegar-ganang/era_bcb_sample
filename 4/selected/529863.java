package org.jpedal.objects.outlines;

import java.awt.Point;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.PageLookup;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * encapsulate the Outline data
 */
public class OutlineData {

    private Document OutlineDataXML;

    /**locations of top target*/
    private float[] pagesTop;

    private Map pointLookupTable;

    /**locations of top and bottom target*/
    private float[] pagesBottom;

    /**lookup for converting page to ref*/
    private String[] refTop;

    /**lookup for converting page to ref*/
    private String[] refBottom;

    /**final table*/
    private String[] lookup;

    private Map fields = new Hashtable();

    private Map keysUsedTable = new Hashtable();

    private OutlineData() {
    }

    /**create list when object initialised*/
    public OutlineData(int pageCount) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            OutlineDataXML = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            System.err.println("Exception " + e + " generating XML document");
        }
        fields.put("Title", "x");
        fields.put("Dest", "x");
        String[] keysUsed = { "Title", "Next", "Last" };
        for (int i = 0; i < keysUsed.length; i++) keysUsedTable.put(keysUsed[i], "x");
        pageCount++;
        pagesTop = new float[pageCount];
        pagesBottom = new float[pageCount];
        refTop = new String[pageCount];
        refBottom = new String[pageCount];
        lookup = new String[pageCount];
        pointLookupTable = new HashMap();
    }

    /**return the list*/
    public Document getList() {
        return OutlineDataXML;
    }

    /**
	 * read the outline data
	 */
    public int readOutlineFileMetadata(Object outlineObject, PdfObjectReader currentPdfFile, PageLookup pageLookup) {
        LogWriter.writeMethod("{readOutlineFileMetadata " + outlineObject + "}", 0);
        int count = 0;
        String startObj, nextObj, endObj, rawDest, title;
        Map values;
        if (outlineObject instanceof String) values = currentPdfFile.readObject((String) outlineObject, false, null); else values = (Map) outlineObject;
        Object rawNumber = values.get("Count");
        if (rawNumber != null) count = Integer.parseInt(currentPdfFile.getValue((String) rawNumber));
        startObj = (String) values.get("First");
        if (startObj != null) {
            Element root = OutlineDataXML.createElement("root");
            OutlineDataXML.appendChild(root);
            int level = 0;
            readOutlineLevel(root, currentPdfFile, pageLookup, startObj, level);
        }
        return count;
    }

    /**
	 * returns default bookmark to select for each page
	 * - not part of API and not live
	 */
    public String[] getDefaultBookmarksForPage() {
        return lookup;
    }

    /**
	 * read a level
	 */
    private void readOutlineLevel(Element root, PdfObjectReader currentPdfFile, PageLookup pageLookup, String startObj, int level) {
        String nextObj;
        String endObj;
        String rawDest;
        String convertedTitle = "";
        Object anchor;
        float coord = 0;
        byte[] title;
        Map values;
        Element child = OutlineDataXML.createElement("title");
        while (true) {
            values = currentPdfFile.readObject(startObj, false, fields);
            String ID = startObj;
            coord = -1;
            nextObj = (String) values.get("Next");
            endObj = (String) values.get("Last");
            startObj = (String) values.get("First");
            Object destStream = values.get("Dest");
            if (destStream != null) {
                if (destStream instanceof byte[]) {
                    rawDest = currentPdfFile.getTextString((byte[]) destStream);
                } else {
                    rawDest = (String) destStream;
                }
            } else rawDest = null;
            if ((rawDest != null) && (rawDest.startsWith("("))) {
                rawDest = rawDest.substring(1, rawDest.length() - 1);
                rawDest = currentPdfFile.convertNameToRef(rawDest);
                if (rawDest.indexOf("[") == -1) {
                    Map parentObject = currentPdfFile.readObject(rawDest, false, fields);
                    rawDest = (String) parentObject.get("D");
                }
            }
            anchor = (values.get("A"));
            if (anchor != null) {
                Map anchorObj;
                if (anchor instanceof String) {
                    anchorObj = currentPdfFile.readObject((String) anchor, false, fields);
                } else {
                    anchorObj = (Map) anchor;
                }
                rawDest = (String) anchorObj.get("D");
                if (rawDest != null && rawDest.startsWith("(")) {
                    Map DField = new HashMap();
                    DField.put("D", "x");
                    currentPdfFile.flushObjectCache();
                    anchorObj = currentPdfFile.readObject((String) anchor, false, DField);
                    byte[] newD = currentPdfFile.getByteTextStringValue(anchorObj.get("D"), DField);
                    rawDest = currentPdfFile.getTextString(newD);
                }
            }
            title = currentPdfFile.getByteTextStringValue(values.get("Title"), fields);
            if (title != null) {
                convertedTitle = currentPdfFile.getTextString(title);
                child = OutlineDataXML.createElement("title");
                root.appendChild(child);
                child.setAttribute("title", convertedTitle);
                Iterator keyList = values.keySet().iterator();
                while (keyList.hasNext()) {
                    String currentKey = keyList.next().toString();
                    if (!keysUsedTable.containsKey(currentKey)) {
                        Object keyValue = values.get(currentKey);
                        if ((keyValue != null) && (keyValue instanceof String)) child.setAttribute(currentKey, (String) keyValue);
                    }
                }
            }
            if ((rawDest != null) && (rawDest.startsWith("("))) {
                rawDest = currentPdfFile.convertNameToRef(rawDest);
                if (rawDest != null) {
                    Map destObj = currentPdfFile.readObject(rawDest, false, null);
                    if (destObj != null) rawDest = (String) destObj.get("D");
                }
            } else if (rawDest != null) {
                String name = currentPdfFile.convertNameToRef(rawDest);
                if (name != null) {
                    rawDest = name;
                    if ((rawDest != null) && (rawDest.endsWith("R"))) {
                        Map destObj = currentPdfFile.readObject(rawDest, false, null);
                        if (destObj != null) rawDest = (String) destObj.get("D");
                    }
                }
            }
            if ((rawDest != null)) {
                String ref = "";
                int page = -1;
                if (rawDest.startsWith("[")) {
                    StringTokenizer destValues = new StringTokenizer(rawDest, "[]/ ");
                    if (destValues.countTokens() > 3) ref = destValues.nextToken() + " " + destValues.nextToken() + " " + destValues.nextToken();
                } else ref = rawDest;
                page = pageLookup.convertObjectToPageNumber(ref);
                if (page == -1) {
                    Map newValue = currentPdfFile.readObject(ref, false, null);
                    String rawValue = (String) newValue.get("rawValue");
                    if (rawValue != null) {
                        rawValue = Strip.removeArrayDeleminators(rawValue);
                        int p = rawValue.indexOf(" R");
                        if (p != -1) {
                            ref = rawValue.substring(0, p + 2);
                            page = pageLookup.convertObjectToPageNumber(ref);
                            p = rawValue.indexOf("/FitH");
                            if (p != -1) {
                                String value = rawValue.substring(p + 5).trim();
                                coord = Float.parseFloat(value);
                            }
                        }
                    }
                }
                if (page == -1) {
                } else {
                    child.setAttribute("page", "" + page);
                    child.setAttribute("level", "" + level);
                    child.setAttribute("objectRef", ID);
                    Integer pageInt = new Integer(page);
                    if ((rawDest != null) && (rawDest.indexOf("/XYZ") != -1)) {
                        rawDest = rawDest.substring(rawDest.indexOf("/XYZ") + 4);
                        StringTokenizer destValues = new StringTokenizer(rawDest, "[] ");
                        String x = destValues.nextToken();
                        if (x.equals("null")) x = "0";
                        String y = destValues.nextToken();
                        if (y.equals("null")) y = "0";
                        pointLookupTable.put(title, new Point((int) Float.parseFloat(x), (int) Float.parseFloat(y)));
                    }
                    if (refTop[page] == null) {
                        pagesTop[page] = coord;
                        refTop[page] = ID;
                        pagesBottom[page] = coord;
                        refBottom[page] = ID;
                    } else {
                        String lastRef = refTop[page];
                        float last = pagesTop[page];
                        if ((last > coord) && (last != -1)) {
                            pagesTop[page] = coord;
                            refTop[page] = ID;
                        }
                        lastRef = refBottom[page];
                        last = pagesBottom[page];
                        if ((last < coord) && (last != -1)) {
                            pagesBottom[page] = coord;
                            refBottom[page] = ID;
                        }
                    }
                }
            }
            if (startObj != null) readOutlineLevel(child, currentPdfFile, pageLookup, startObj, level + 1);
            if (nextObj == null) break;
            startObj = nextObj;
        }
    }
}

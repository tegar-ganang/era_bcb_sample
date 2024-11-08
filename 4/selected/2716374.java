package org.jpedal.objects;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.jpedal.exception.PdfException;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;

/**
 * holds data from annotations and provides methods to
 * decode, store and retrieve PdfAnnotations.
 */
public class PdfAnnots {

    /**current annot number*/
    private int annotNumber = 0;

    PdfObjectReader currentPdfFile = null;

    private Hashtable fields = new Hashtable();

    /** sets up annotation object to hold content - 
	 * (This method is used internally to generate the annotations 
	 * and should not be called)
	 */
    public PdfAnnots(PdfObjectReader currentPdfFile, PageLookup pageLookup) {
        this.currentPdfFile = currentPdfFile;
        fields.put("Contents", "x");
        fields.put("T", "x");
        fields.put("Subj", "x");
        fields.put("V", "x");
        fields.put("CA", "x");
        fields.put("DA", "x");
        fields.put("Dest", "x");
    }

    private PdfAnnots() {
    }

    /**hold raw annot data*/
    private List Annots = new ArrayList();

    /**hold raw annot data*/
    private Map annotArea = new Hashtable();

    private Map annotType = new Hashtable();

    private Map annotCustomIcon = new HashMap();

    /**
	 * read the annotation from the page
	 */
    public final void readAnnots(String annots) throws PdfException {
        LogWriter.writeMethod("{pdf-readAnnots: " + annots + "}", 1);
        annots = Strip.removeArrayDeleminators(annots);
        if (annots.length() > 0) {
            try {
                if (annots.startsWith("<<")) {
                } else {
                    StringTokenizer initialValues = new StringTokenizer(annots, "R");
                    while (initialValues.hasMoreTokens()) {
                        String value = initialValues.nextToken().trim() + " R";
                        readAnnot(value);
                    }
                }
            } catch (Exception e) {
                LogWriter.writeLog("Exception " + e + " processing annots >" + annots + "<");
                throw new PdfException("Exception");
            }
        }
    }

    /**
	 * get number of annotations
	 */
    public final int getAnnotCount() {
        return this.Annots.size();
    }

    /**
	 * get raw PDF data for annotation - returns null if not in range
	 * (first annot is 0, not 1)
	 */
    public final Map getAnnotRawData(int i) {
        return (Map) Annots.get(i);
    }

    /**
     * get all raw PDF data for annotation
     */
    public final List getAnnotRawDataList() {
        return Annots;
    }

    /**
     * show if has own custom icon
     */
    public final boolean hasOwnIcon(int i) {
        return annotCustomIcon.get(new Integer(i)) != null;
    }

    /**
	 * get  area as it is stored in the PDF
	 */
    public final String getAnnotObjectArea(int i) {
        return (String) annotArea.get(new Integer(i));
    }

    /**
	 * get  color for annotation
	 */
    public final Color getAnnotColor(int i) {
        Color annotColor = null;
        Object rawAnnot = Annots.get(i);
        Map annot = null;
        if (rawAnnot instanceof String) {
            annot = currentPdfFile.readObject((String) rawAnnot, false, null);
        } else {
            annot = (Map) rawAnnot;
        }
        String cols = (String) annot.get("C");
        if (cols != null) {
            String rawCol = Strip.removeArrayDeleminators(currentPdfFile.getValue((cols)));
            StringTokenizer colElements = new StringTokenizer(rawCol);
            if (colElements.countTokens() == 3) {
                float r = Float.parseFloat(colElements.nextToken());
                float g = Float.parseFloat(colElements.nextToken());
                float b = Float.parseFloat(colElements.nextToken());
                annotColor = new Color(r, g, b);
            }
        }
        return annotColor;
    }

    /**
	 * get  stroke to use for border - return null if no border
	 */
    public final Stroke getBorderStroke(int i) {
        int width = 0;
        Object rawAnnot = Annots.get(i);
        Map annot = null;
        if (rawAnnot instanceof String) {
            annot = currentPdfFile.readObject((String) rawAnnot, false, null);
        } else {
            annot = (Map) rawAnnot;
        }
        String cols = (String) annot.get("Border");
        if (cols != null) {
            String rawCol = Strip.removeArrayDeleminators(currentPdfFile.getValue((cols)));
            StringTokenizer colElements = new StringTokenizer(rawCol);
            if (colElements.countTokens() == 1) {
                width = Integer.parseInt(rawCol);
            } else if (colElements.countTokens() == 3) {
                for (int j = 0; j < 2; j++) colElements.nextToken();
                width = Integer.parseInt(colElements.nextToken());
            }
        }
        if (width == 0) return null; else return new BasicStroke(width);
    }

    /**
	 * get  area as it is stored in the PDF
	 */
    public final String getAnnotSubType(int i) {
        String subtype = null;
        Map currentAnnot = (Map) Annots.get(i);
        subtype = (String) currentAnnot.get("Subtype");
        if ((subtype != null) && (subtype.startsWith("/"))) subtype = subtype.substring(1);
        return subtype;
    }

    /**
	 * read an annotation from the page
	 */
    private final void readAnnot(String annot) {
        LogWriter.writeMethod("{pdf-readAnnot: " + annot + "}", 1);
        Map annotationObject = new Hashtable();
        if (annot.startsWith("<<")) annotationObject = currentPdfFile.directValuesToMap(annot); else {
            annotationObject = currentPdfFile.readObject(annot, false, fields);
        }
        String type = (String) annotationObject.get("Type");
        if (type == null) type = "/Annot";
        String subtype = (String) annotationObject.get("Subtype");
        if (subtype == null) subtype = "none";
        annotType.put(new Integer(annotNumber), subtype);
        if (annotationObject.get("AP") != null) annotCustomIcon.put(new Integer(annotNumber), "x");
        String rectString = (String) annotationObject.get("Rect");
        if (rectString != null) {
            String rect = Strip.removeArrayDeleminators(currentPdfFile.getValue(rectString));
            annotArea.put(new Integer(annotNumber), rect);
            String[] keysToSubstitute = { "A", "FS" };
            substituteKeyValues(currentPdfFile, annotationObject, keysToSubstitute);
            this.Annots.add(annotNumber, annotationObject);
            annotNumber++;
        }
    }

    /**
     * replace all values with direct items
     */
    private void substituteKeyValues(PdfObjectReader currentPdfFile, Map annotationObject, String[] keysToSubstitute) {
        int count = keysToSubstitute.length;
        for (int j = 0; j < count; j++) {
            try {
                Object annotObjectData = annotationObject.get(keysToSubstitute[j]);
                if (annotObjectData != null) {
                    Map actionData = null;
                    if (annotObjectData instanceof String) {
                        actionData = currentPdfFile.readObject((String) annotObjectData, false, null);
                        if (actionData.containsKey("startStreamOnDisk")) currentPdfFile.readStream((String) annotObjectData, false);
                        if (actionData.containsKey("Stream")) {
                            byte[] decompressedData = currentPdfFile.readStream(actionData, (String) annotObjectData, true, false, false);
                            actionData.put("DecodedStream", decompressedData);
                        }
                    } else actionData = (Map) annotObjectData;
                    if (actionData.containsKey("EF")) {
                        Map fileData = (Map) actionData.get("EF");
                        String[] fileSpec = { "F" };
                        substituteKeyValues(currentPdfFile, fileData, fileSpec);
                    }
                    annotationObject.put(keysToSubstitute[j], actionData);
                }
            } catch (Exception e) {
                LogWriter.writeLog("Exception " + e);
            }
        }
    }

    /**
     * a text field
     */
    public String getField(int i, String field) {
        String returnValue = null;
        Object rawAnnot = Annots.get(i);
        Map currentAnnot = (Map) currentPdfFile.resolveToMapOrString(field, rawAnnot);
        byte[] title = currentPdfFile.getByteTextStringValue(currentAnnot.get(field), fields);
        if (title != null) returnValue = currentPdfFile.getTextString(title);
        return returnValue;
    }

    /**
     * return color of border or black as default
     */
    public Color getBorderColor(int i) {
        Color annotColor = Color.black;
        Object rawAnnot = Annots.get(i);
        Map annot = null;
        if (rawAnnot instanceof String) {
            annot = currentPdfFile.readObject((String) rawAnnot, false, null);
        } else {
            annot = (Map) rawAnnot;
        }
        String cols = (String) annot.get("Border");
        if (cols != null) {
            String rawCol = Strip.removeArrayDeleminators(currentPdfFile.getValue((cols)));
            StringTokenizer colElements = new StringTokenizer(rawCol);
            if (colElements.countTokens() == 3) {
                float r = Float.parseFloat(colElements.nextToken());
                float g = Float.parseFloat(colElements.nextToken());
                float b = Float.parseFloat(colElements.nextToken());
                annotColor = new Color(r, g, b);
            }
        }
        return annotColor;
    }
}

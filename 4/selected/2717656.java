package org.jpedal.fonts;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.fonts.glyph.T3Glyph;
import org.jpedal.fonts.glyph.T3Glyphs;
import org.jpedal.fonts.glyph.T3Size;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.LogWriter;
import org.jpedal.parser.PdfStreamDecoder;
import org.jpedal.render.DynamicVectorRenderer;

/**
 * handles type1 specifics
 */
public class Type3 extends PdfFont {

    /**handle onto GS so we can read color*/
    protected GraphicsState currentGraphicsState;

    /** get handles onto Reader so we can access the file */
    public Type3(PdfObjectReader current_pdf_file, GraphicsState currentGraphicsState) {
        glyphs = new T3Glyphs();
        init(current_pdf_file);
        this.currentGraphicsState = currentGraphicsState;
    }

    /**read in a font and its details from the pdf file*/
    public final Map createFont(Map values, String fontID, boolean renderPage, Map descFontValues, ObjectStore objectStore) throws Exception {
        LogWriter.writeMethod("{readType3Font}" + values, 0);
        fontTypes = StandardFonts.TYPE3;
        Map fontDescriptor = super.createFont(values, fontID, renderPage, descFontValues, objectStore);
        readWidths(values);
        readEmbeddedFont(values, objectStore);
        if (renderPage) setFont(getBaseFontName(), 1);
        return fontDescriptor;
    }

    /**
	 * @param values
	 */
    private void readEmbeddedFont(Map values, ObjectStore objectStore) {
        boolean hires = false;
        Object procs = values.get("CharProcs");
        if (procs != null) {
            Map procValues;
            if (procs instanceof String) procValues = currentPdfFile.readObject((String) procs, false, null); else procValues = (Map) procs;
            Iterator procKeys = procValues.keySet().iterator();
            while (procKeys.hasNext()) {
                String pKey = (String) procKeys.next();
                String objectRef = (String) procValues.get(pKey);
                if (objectRef != null && renderPage) {
                    PdfStreamDecoder glyphDecoder = new PdfStreamDecoder(currentPdfFile, hires, true);
                    glyphDecoder.setStore(objectStore);
                    Object res = values.get("Resources");
                    if (res != null) {
                        Map resValue = null;
                        if (res instanceof Map) resValue = (Map) res; else resValue = this.currentPdfFile.getSubDictionary(res);
                        if (resValue != null) {
                            try {
                                glyphDecoder.readResources(false, resValue, false);
                            } catch (PdfException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                    DynamicVectorRenderer glyphDisplay = new DynamicVectorRenderer(0, false, 20, objectStore);
                    glyphDisplay.setHiResImageForDisplayMode(hires);
                    glyphDisplay.setType3Glyph(pKey);
                    try {
                        glyphDecoder.init(false, true, 7, 0, new PdfPageData(), 0, glyphDisplay, currentPdfFile, new Hashtable(), new Hashtable());
                        glyphDecoder.setDefaultColors(currentGraphicsState.getNonstrokeColor(), currentGraphicsState.getNonstrokeColor());
                    } catch (Exception e) {
                        LogWriter.writeLog("Font exception " + e);
                    }
                    int renderX = 0, renderY = 0;
                    try {
                        T3Size t3 = glyphDecoder.decodePageContent(objectRef, 0, 0, null);
                        renderX = t3.x;
                        renderY = t3.y;
                    } catch (PdfException e1) {
                        e1.printStackTrace();
                    }
                    boolean ignoreColors = glyphDecoder.ignoreColors;
                    glyphDecoder = null;
                    PdfGlyph glyph = new T3Glyph(glyphDisplay, renderX, renderY, ignoreColors, pKey);
                    glyphs.setT3Glyph(pKey, glyph);
                }
            }
            isFontEmbedded = true;
        }
    }
}

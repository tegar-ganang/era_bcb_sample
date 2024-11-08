package org.jpedal.fonts;

import java.util.Map;
import org.jpedal.fonts.tt.TTGlyphs;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.ObjectStore;
import org.jpedal.utils.LogWriter;

/**
 * handles truetype specifics
 *  */
public class CIDFontType2 extends TrueType {

    /**get handles onto Reader so we can access the file*/
    public CIDFontType2(PdfObjectReader currentPdfFile) {
        isCIDFont = true;
        TTstreamisCID = true;
        glyphs = new TTGlyphs();
        init(currentPdfFile);
    }

    /**get handles onto Reader so we can access the file*/
    public CIDFontType2(PdfObjectReader currentPdfFile, boolean ttflag) {
        isCIDFont = true;
        TTstreamisCID = ttflag;
        glyphs = new TTGlyphs();
        init(currentPdfFile);
    }

    /**read in a font and its details from the pdf file*/
    public Map createFont(Map values, String fontID, boolean renderPage, Map descFontValues, ObjectStore objectStore) throws Exception {
        LogWriter.writeMethod("{readFontType0 " + fontID + "}", 0);
        Map fontDescriptor = null;
        fontTypes = StandardFonts.CIDTYPE2;
        this.fontID = fontID;
        fontDescriptor = createCIDFont(values, descFontValues);
        if (fontDescriptor != null) {
            String fontFileRef = (String) fontDescriptor.get("FontFile2");
            if (fontFileRef != null) {
                if (renderPage) readEmbeddedFont(currentPdfFile.readStream(fontFileRef, true), hasEncoding, false);
            }
        }
        if ((renderPage) && (!isFontEmbedded) && (this.substituteFontFile != null)) {
            this.substituteFontUsed(substituteFontFile, substituteFontName);
            isFontSubstituted = true;
            this.isFontEmbedded = true;
        }
        if (renderPage) setFont(getBaseFontName(), 1);
        if (!isFontEmbedded) selectDefaultFont();
        return fontDescriptor;
    }
}

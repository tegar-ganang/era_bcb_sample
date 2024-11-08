package org.jpedal.fonts;

import java.util.Map;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.glyph.T1Glyphs;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.ObjectStore;
import org.jpedal.utils.LogWriter;

/**
 * handles truetype specifics
 *  */
public class CIDFontType0 extends Type1C {

    /**used to display non-embedded fonts*/
    private CIDFontType2 subFont = null;

    /**get handles onto Reader so we can access the file*/
    public CIDFontType0(PdfObjectReader currentPdfFile) {
        glyphs = new T1Glyphs(true);
        isCIDFont = true;
        TTstreamisCID = true;
        init(currentPdfFile);
        this.currentPdfFile = currentPdfFile;
    }

    /**read in a font and its details from the pdf file*/
    public Map createFont(Map values, String fontID, boolean renderPage, Map descFontValues, ObjectStore objectStore) throws Exception {
        LogWriter.writeMethod("{readCIDFONT0 " + fontID + "}", 0);
        fontTypes = StandardFonts.CIDTYPE0;
        this.fontID = fontID;
        Map fontDescriptor = createCIDFont(values, descFontValues);
        if (fontDescriptor != null) readEmbeddedFont(values, fontDescriptor);
        if ((renderPage) && (!isFontEmbedded) && (this.substituteFontFile != null)) {
            isFontSubstituted = true;
            subFont = new CIDFontType2(currentPdfFile, TTstreamisCID);
            subFont.substituteFontUsed(substituteFontFile, substituteFontName);
            this.isFontEmbedded = true;
        }
        if (!isFontEmbedded) selectDefaultFont();
        if (renderPage) setFont(getBaseFontName(), 1);
        return fontDescriptor;
    }

    /**
	 * used by  non type3 font
	 */
    public PdfJavaGlyphs getGlyphData() {
        if (subFont != null) return subFont.getGlyphData(); else return glyphs;
    }
}

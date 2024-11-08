package org.jpedal.fonts.tt;

import org.jpedal.utils.LogWriter;
import org.jpedal.fonts.Type1C;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.glyph.T1Glyphs;
import org.jpedal.fonts.glyph.GlyphFactory;

public class CFF extends Table {

    Type1C cffData;

    PdfJavaGlyphs glyphs = new T1Glyphs(false);

    boolean hasCFFdata = false;

    public CFF(FontFile2 currentFontFile) {
        LogWriter.writeMethod("{readCFFTable}", 0);
        int startPointer = currentFontFile.selectTable(FontFile2.CFF);
        if (startPointer != 0) {
            int length = currentFontFile.getTableSize(FontFile2.CFF);
            byte[] data = currentFontFile.readBytes(startPointer, length);
            try {
                cffData = new Type1C(data, glyphs);
                hasCFFdata = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasCFFData() {
        return hasCFFdata;
    }

    public PdfGlyph getCFFGlyph(GlyphFactory factory, String glyph, float[][] Trm, int rawInt, String displayValue, float currentWidth, String key) {
        return glyphs.getEmbeddedGlyph(factory, glyph, Trm, rawInt, displayValue, currentWidth, key);
    }
}

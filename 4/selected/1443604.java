package org.jpedal.fonts.tt;

import java.util.Hashtable;
import org.jpedal.utils.LogWriter;

public class Glyf extends Table {

    /**holds mappings for drawing the glpyhs*/
    private Hashtable charStrings = new Hashtable();

    public Glyf(FontFile2 currentFontFile, int glyphCount, int[] glyphIndexStart) {
        LogWriter.writeMethod("{readGlyfTable}", 0);
        int startPointer = currentFontFile.selectTable(FontFile2.LOCA);
        int glyf = currentFontFile.getTable(FontFile2.GLYF);
        if (startPointer != 0) {
            for (int i = 0; i < glyphCount; i++) {
                if ((glyphIndexStart[i] == glyphIndexStart[i + 1])) charStrings.put(new Integer(i), new Integer(-1)); else {
                    charStrings.put(new Integer(i), new Integer(glyf + glyphIndexStart[i]));
                }
            }
        }
    }

    public boolean isPresent(int glyph) {
        Object value = charStrings.get(new Integer(glyph));
        return value != null;
    }

    public int getCharString(int glyph) {
        Object value = charStrings.get(new Integer(glyph));
        if (value == null) return glyph; else return ((Integer) value).intValue();
    }
}

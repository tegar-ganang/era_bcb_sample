package org.jpedal.fonts.tt;

import java.util.HashMap;
import java.util.Map;
import org.jpedal.utils.LogWriter;

public class Post extends Table {

    /**map glyphs onto ID values in font*/
    Map translateToID = new HashMap();

    private final String[] macEncoding = { ".notdef", ".null", "nonmarkingreturn", "space", "exclam", "quotedbl", "numbersign", "dollar", "percent", "ampersand", "quotesingle", "parenleft", "parenright", "asterisk", "plus", "comma", "hyphen", "period", "slash", "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "colon", "semicolon", "less", "equal", "greater", "question", "at", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "bracketleft", "backslash", "bracketright", "asciicircum", "underscore", "grave", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "braceleft", "bar", "braceright", "asciitilde", "Adieresis", "Aring", "Ccedilla", "Eacute", "Ntilde", "Odieresis", "Udieresis", "aacute", "agrave", "acircumflex", "adieresis", "atilde", "aring", "ccedilla", "eacute", "egrave", "ecircumflex", "edieresis", "iacute", "igrave", "icircumflex", "idieresis", "ntilde", "oacute", "ograve", "ocircumflex", "odieresis", "otilde", "uacute", "ugrave", "ucircumflex", "udieresis", "dagger", "degree", "cent", "sterling", "section", "bullet", "paragraph", "germandbls", "registered", "copyright", "trademark", "acute", "dieresis", "notequal", "AE", "Oslash", "infinity", "plusminus", "lessequal", "greaterequal", "yen", "mu", "partialdiff", "summation", "product", "pi", "integral", "ordfeminine", "ordmasculine", "Omega", "ae", "oslash", "questiondown", "exclamdown", "logicalnot", "radical", "florin", "approxequal", "Delta", "guillemotleft", "guillemotright", "ellipsis", "nonbreakingspace", "Agrave", "Atilde", "Otilde", "OE", "oe", "endash", "emdash", "quotedblleft", "quotedblright", "quoteleft", "quoteright", "divide", "lozenge", "ydieresis", "Ydieresis", "fraction", "currency", "guilsinglleft", "guilsinglright", "fi", "fl", "daggerdbl", "periodcentered", "quotesinglbase", "quotedblbase", "perthousand", "Acircumflex", "Ecircumflex", "Aacute", "Edieresis", "Egrave", "Iacute", "Icircumflex", "Idieresis", "Igrave", "Oacute", "Ocircumflex", "apple", "Ograve", "Uacute", "Ucircumflex", "Ugrave", "dotlessi", "circumflex", "tilde", "macron", "breve", "dotaccent", "ring", "cedilla", "hungarumlaut", "ogonek", "caron", "Lslash", "lslash", "Scaron", "scaron", "Zcaron", "zcaron", "brokenbar", "Eth", "eth", "Yacute", "yacute", "Thorn", "thorn", "minus", "multiply", "onesuperior", "twosuperior", "threesuperior", "onehalf", "onequarter", "threequarters", "franc", "Gbreve", "gbreve", "Idotaccent", "Scedilla", "scedilla", "Cacute", "cacute", "Ccaron", "ccaron", "dcroat" };

    public Post(FontFile2 currentFontFile) {
        LogWriter.writeMethod("{readPostTable}", 0);
        int startPointer = currentFontFile.selectTable(FontFile2.POST);
        if (startPointer == 0) LogWriter.writeLog("No Post table found"); else {
            int id = (int) (10 * currentFontFile.getFixed());
            currentFontFile.getFixed();
            currentFontFile.getFWord();
            currentFontFile.getFWord();
            currentFontFile.getNextUint16();
            currentFontFile.getNextUint16();
            currentFontFile.getNextUint32();
            currentFontFile.getNextUint32();
            currentFontFile.getNextUint32();
            currentFontFile.getNextUint32();
            int numberOfGlyphs;
            if (id != 30) {
                for (int i = 0; i < 258; i++) this.translateToID.put(this.macEncoding[i], new Integer(i));
            }
            switch(id) {
                case 20:
                    numberOfGlyphs = currentFontFile.getNextUint16();
                    int[] glyphNameIndex = new int[numberOfGlyphs];
                    int numberOfNewGlyphs = 0;
                    for (int i = 0; i < numberOfGlyphs; i++) {
                        glyphNameIndex[i] = currentFontFile.getNextUint16();
                        if ((glyphNameIndex[i] > 257) & (glyphNameIndex[i] < 32768)) numberOfNewGlyphs++;
                    }
                    String[] names = new String[numberOfNewGlyphs];
                    for (int i = 0; i < numberOfNewGlyphs; i++) names[i] = currentFontFile.getString();
                    for (int i = 0; i < numberOfGlyphs; i++) {
                        if ((glyphNameIndex[i] > 257) & (glyphNameIndex[i] < 32768)) this.translateToID.put(names[glyphNameIndex[i] - 258], new Integer(i));
                    }
                    break;
                case 25:
                    numberOfGlyphs = currentFontFile.getNextUint16();
                    int[] glyphOffset = new int[numberOfGlyphs];
                    for (int i = 0; i < numberOfGlyphs; i++) {
                        glyphOffset[i] = currentFontFile.getNextint8();
                        translateToID.put(macEncoding[glyphOffset[i] + i], new Integer(glyphOffset[i]));
                    }
                    break;
            }
        }
    }

    /**
	 * lookup glyph in post table
	 */
    public int convertGlyphToCharacterCode(String glyph) {
        int idx = 0;
        Integer newID = (Integer) translateToID.get(glyph);
        if (newID == null) return idx; else return newID.intValue();
    }
}

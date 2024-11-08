package org.jpedal.fonts;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import org.jpedal.PdfDecoder;
import org.jpedal.parser.PdfStreamDecoder;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.ObjectStore;
import org.jpedal.utils.Strip;
import org.jpedal.exception.PdfFontException;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.ToInteger;

/**
 * contains all generic pdf font data for fonts.<P>
 *
  */
public class PdfFont implements Serializable {

    public Font javaFont = null;

    protected String embeddedFontName = null, embeddedFamilyName = null, copyright = null;

    private float missingWidth = -1f;

    public PdfJavaGlyphs glyphs = new PdfJavaGlyphs();

    /**cache for translate values*/
    private String[] cachedValue = new String[256];

    /**allow use to remove spaces and - from font names and ensure lowercase*/
    protected boolean cleanupFonts = false;

    static {
        setStandardFontMappings();
    }

    public PdfFont() {
    }

    /**read in a font and its details for generic usage*/
    public void createFont(String fontName) throws Exception {
    }

    /**get handles onto Reader so we can access the file*/
    public PdfFont(PdfObjectReader current_pdf_file) {
        init(current_pdf_file);
    }

    public static void setStandardFontMappings() {
        int count = StandardFonts.files_names.length;
        for (int i = 0; i < count; i++) {
            String key = StandardFonts.files_names_bis[i].toLowerCase();
            String value = StandardFonts.javaFonts[i].toLowerCase();
            if ((!key.equals(value)) && (!PdfDecoder.fontSubstitutionAliasTable.containsKey(key))) PdfDecoder.fontSubstitutionAliasTable.put(key, value);
        }
        for (int i = 0; i < count; i++) {
            String key = StandardFonts.files_names[i].toLowerCase();
            String value = StandardFonts.javaFonts[i].toLowerCase();
            if ((!key.equals(value)) && (!PdfDecoder.fontSubstitutionAliasTable.containsKey(key))) PdfDecoder.fontSubstitutionAliasTable.put(key, value);
            StandardFonts.javaFontList.put(StandardFonts.files_names[i], "x");
        }
        PdfDecoder.fontSubstitutionAliasTable.put("arialmt", "arial");
        PdfDecoder.fontSubstitutionAliasTable.put("arial-boldmt", "arialbd");
    }

    protected String substituteFont = null;

    protected boolean renderPage = false;

    private final float xscale = (float) 0.001;

    /**embedded encoding*/
    protected int embeddedEnc = StandardFonts.STD;

    /**holds lookup to map char differences in embedded font*/
    protected String[] diffs;

    /**flag to show if Font included embedded data*/
    public boolean isFontEmbedded = false;

    protected boolean TTstreamisCID = false;

    /**String used to reference font (ie F1)*/
    protected String fontID = "";

    /**number of glyphs - 65536 for CID fonts*/
    protected int maxCharCount = 256;

    /**show if encoding set*/
    protected boolean hasEncoding = true;

    /**flag to show if double-byte*/
    private boolean isDoubleByte = false;

    /**font type*/
    protected int fontTypes;

    protected String substituteFontFile = null, substituteFontName = null;

    /**lookup to track which char is space. -1 means none set*/
    private int spaceChar = -1;

    /**holds lookup to map char differences*/
    private String[] diffTable;

    /**holds flags for font*/
    private int fontFlag = 0;

    /**lookup for which of each char for embedded fonts which we can flush*/
    private float[] widthTable;

    /**size to use for space if not defined (-1 is no setting)*/
    private float possibleSpaceWidth = -1;

    /**handle onto file access*/
    protected PdfObjectReader currentPdfFile;

    /**loader to load data from jar*/
    protected ClassLoader loader = this.getClass().getClassLoader();

    /**FontBBox for font*/
    public double[] FontMatrix = { 0.001d, 0d, 0d, 0.001d, 0, 0 };

    /**font bounding box*/
    public float[] FontBBox = { 0f, 0f, 1f, 1f };

    /**
	 * flag to show
	 * Gxxx, Bxxx, Cxxx.
	 */
    protected boolean isHex = false;

    /**holds lookup to map char values*/
    private String[] unicodeMappings;

    /**encoding pattern used for font. -1 means not set*/
    protected int fontEnc = -1;

    /**flag to show type of font*/
    protected boolean isCIDFont = false;

    /**lookup CID index mappings*/
    protected String[] CMAP;

    /** CID font encoding*/
    protected String CIDfontEncoding;

    /**default width for font*/
    private float defaultWidth = 1f;

    protected boolean isFontSubstituted = false;

    /**flag to show if font had explicit encoding - we need to use embedded in some ghostscript files*/
    protected boolean hasFontEncoding;

    protected int italicAngle = 0;

    /**test if cid.jar present first time we need it*/
    private static boolean isCidJarPresent;

    /**
	 * used to show truetype used for type 0 CID
	 */
    public boolean isFontSubstituted() {
        return isFontSubstituted;
    }

    /**
	 * flag if double byte CID char
	 */
    public boolean isDoubleByte() {
        return isDoubleByte;
    }

    /**set the default width of a CID font*/
    protected final void setCIDFontDefaultWidth(String value) {
        defaultWidth = Float.parseFloat(value) / 1000f;
    }

    /**Method to add the widths of a CID font*/
    protected final void setCIDFontWidths(String values) {
        values = values.substring(1, values.length() - 1).trim();
        widthTable = new float[65536];
        for (int ii = 0; ii < 65536; ii++) widthTable[ii] = -1;
        StringTokenizer widthValues = new StringTokenizer(values, " []", true);
        String nextValue;
        while (widthValues.hasMoreTokens()) {
            if (!widthValues.hasMoreTokens()) break;
            while (true) {
                nextValue = widthValues.nextToken();
                if (!nextValue.equals(" ")) break;
            }
            int pointer = Integer.parseInt(nextValue);
            while (true) {
                nextValue = widthValues.nextToken();
                if (!nextValue.equals(" ")) break;
            }
            if (nextValue.equals("[")) {
                while (true) {
                    while (true) {
                        nextValue = widthValues.nextToken();
                        if (!nextValue.equals(" ")) break;
                    }
                    if (nextValue.equals("]")) break;
                    widthTable[pointer] = Float.parseFloat(nextValue) / 1000f;
                    pointer++;
                }
            } else {
                int endPointer = 1 + Integer.parseInt(nextValue);
                while (true) {
                    nextValue = widthValues.nextToken();
                    if (!nextValue.equals(" ")) break;
                }
                for (int ii = pointer; ii < endPointer; ii++) widthTable[ii] = Float.parseFloat(nextValue) / 1000f;
            }
        }
    }

    /**flag if CID font*/
    public final boolean isCIDFont() {
        return isCIDFont;
    }

    /**set number of glyphs to 256 or 65536*/
    protected final void init(PdfObjectReader current_pdf_file) {
        this.currentPdfFile = current_pdf_file;
        if (isCIDFont) maxCharCount = 65536;
        glyphs.init(maxCharCount, isCIDFont);
    }

    /**return unicode value for this index value */
    private final String getUnicodeMapping(int char_int) {
        if (unicodeMappings == null) return null; else return unicodeMappings[char_int];
    }

    /**store encoding and load required mappings*/
    protected final void putFontEncoding(int enc) {
        fontEnc = enc;
        StandardFonts.checkLoaded(enc);
    }

    /**return the mapped character*/
    public final String getUnicodeValue(String displayValue, int rawInt) {
        String textValue = getUnicodeMapping(rawInt);
        if (textValue == null) textValue = displayValue;
        return textValue;
    }

    /**
	 * convert value read from TJ operand into correct glyph<br>Also check to
	 * see if mapped onto unicode value
	 */
    public final String getGlyphValue(int rawInt) {
        if (cachedValue[rawInt] != null) return cachedValue[rawInt];
        String return_value = null;
        if (isCIDFont) {
            String unicodeMappings = getUnicodeMapping(rawInt);
            if (unicodeMappings != null) return_value = unicodeMappings;
            if (return_value == null) {
                String fontEncoding = CIDfontEncoding;
                if (diffTable != null) {
                    return_value = diffTable[rawInt];
                } else if (fontEncoding != null) {
                    if (fontEncoding.startsWith("Identity-")) {
                        return_value = "" + (char) rawInt;
                    } else if (CMAP != null) {
                        String newChar = CMAP[rawInt];
                        if (newChar != null) return_value = newChar;
                    }
                }
                if (return_value == null) return_value = "" + ((char) rawInt);
            }
        } else return_value = getStandardGlyphValue(rawInt);
        cachedValue[rawInt] = return_value;
        return return_value;
    }

    /**
	 * read translation table
	 * @throws PdfFontException
	 */
    private final void handleCIDEncoding(String encodingType) throws PdfFontException {
        String line = "";
        int begin, end, entry;
        boolean inDefinition = false;
        BufferedReader CIDstream = null;
        if (encodingType.startsWith("/")) encodingType = encodingType.substring(1);
        CIDfontEncoding = encodingType;
        if (encodingType.startsWith("Identity-")) {
            isDoubleByte = true;
        } else {
            if (!isCidJarPresent) {
                isCidJarPresent = true;
                InputStream in = PdfFont.class.getResourceAsStream("/org/jpedal/res/cid/00_ReadMe.pdf");
                if (in == null) throw new PdfFontException("cid.jar not on classpath");
            }
            CMAP = new String[65536];
            if (encodingType.equals("ETenms-B5-H")) encodingType = "ETen-B5-H"; else if (encodingType.equals("ETenms-B5-V")) encodingType = "ETen-B5-V";
            try {
                CIDstream = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("org/jpedal/res/cid/" + encodingType), "Cp1252"));
            } catch (Exception e) {
                LogWriter.writeLog("Problem reading encoding for CID font " + fontID + " " + encodingType + " Check CID.jar installed");
            }
            if (encodingType.equals("UniJIS-UCS2-H")) isDoubleByte = true;
            if (CIDstream != null) {
                while (true) {
                    try {
                        line = CIDstream.readLine();
                    } catch (Exception e) {
                    }
                    if (line == null) break;
                    if (line.indexOf("endcidrange") != -1) inDefinition = false;
                    if (inDefinition == true) {
                        StringTokenizer CIDentry = new StringTokenizer(line, " <>[]");
                        boolean multiple_values = false;
                        if (line.indexOf("[") != -1) multiple_values = true;
                        begin = Integer.parseInt(CIDentry.nextToken(), 16);
                        end = Integer.parseInt(CIDentry.nextToken(), 16);
                        entry = Integer.parseInt(CIDentry.nextToken(), 16);
                        for (int i = begin; i < end + 1; i++) {
                            if (multiple_values == true) {
                                entry = Integer.parseInt(CIDentry.nextToken(), 16);
                                CMAP[i] = "" + (char) entry;
                            } else {
                                CMAP[i] = "" + (char) entry;
                                entry++;
                            }
                        }
                    }
                    if (line.indexOf("begincidrange") != -1) inDefinition = true;
                }
            }
        }
        if (CIDstream != null) {
            try {
                CIDstream.close();
            } catch (Exception e) {
                LogWriter.writeLog("Problem reading encoding for CID font " + fontID + " " + encodingType + " Check CID.jar installed");
            }
        }
    }

    /**
	 * convert value read from TJ operand into correct glyph<br> Also check to
	 * see if mapped onto unicode value
	 */
    public final String getStandardGlyphValue(int char_int) {
        String unicode_char = getUnicodeMapping(char_int);
        if ((unicode_char != null)) return unicode_char;
        String return_value = "", mapped_char;
        int font_encoding = getFontEncoding(true);
        mapped_char = getMappedChar(char_int, true);
        if (mapped_char != null) {
            String char_mapping = StandardFonts.getUnicodeName(this.fontEnc + mapped_char);
            if (char_mapping != null) return_value = char_mapping; else {
                char_mapping = StandardFonts.getUnicodeName(mapped_char);
                if (char_mapping != null) return_value = char_mapping; else {
                    if (mapped_char.length() == 1) {
                        return_value = mapped_char;
                    } else if (mapped_char.length() > 1) {
                        char c = mapped_char.charAt(0);
                        char c2 = mapped_char.charAt(1);
                        if (c == 'B' | c == 'C' | c == 'c' | c == 'G') {
                            mapped_char = mapped_char.substring(1);
                            try {
                                int val = (isHex) ? Integer.valueOf(mapped_char, 16).intValue() : Integer.parseInt(mapped_char);
                                return_value = String.valueOf((char) val);
                            } catch (Exception e) {
                                return_value = "";
                            }
                        } else return_value = "";
                        boolean isHex = ((c >= 48 && c <= 57) || (c >= 97 && c <= 102) || (c >= 65 && c <= 70)) && ((c2 >= 48 && c2 <= 57) || (c2 >= 97 && c2 <= 102) || (c2 >= 65 && c2 <= 70));
                        if (return_value.length() == 0 && this.fontTypes == StandardFonts.TYPE3 && mapped_char.length() == 2 && isHex) {
                            try {
                                return_value = "" + (char) Integer.parseInt(mapped_char, 16);
                            } catch (Exception e) {
                            }
                        }
                    } else return_value = "";
                }
            }
        } else if (font_encoding > -1) return_value = StandardFonts.getEncodedChar(font_encoding, char_int);
        return return_value;
    }

    /**set the font being used or try to approximate*/
    public final Font getJavaFont(int size) {
        int style = Font.PLAIN;
        boolean isJavaFontInstalled = false;
        String weight = null, mappedName = null, font_family_name = glyphs.fontName;
        String testFont = font_family_name;
        if (font_family_name != null) testFont = font_family_name.toLowerCase();
        if (testFont.equals("arialmt")) {
            testFont = "arial";
            font_family_name = testFont;
        } else if (testFont.equals("arial-boldmt")) {
            testFont = "arial Bold";
            font_family_name = testFont;
        }
        if (mappedName != null) {
            font_family_name = mappedName;
            testFont = font_family_name.toLowerCase();
        }
        if (PdfJavaGlyphs.fontList != null) {
            int count = PdfJavaGlyphs.fontList.length;
            for (int i = 0; i < count; i++) {
                System.out.println(PdfJavaGlyphs.fontList[i] + "<>" + testFont);
                if ((PdfJavaGlyphs.fontList[i].indexOf(testFont) != -1)) {
                    isJavaFontInstalled = true;
                    font_family_name = PdfJavaGlyphs.fontList[i];
                    i = count;
                }
            }
        }
        if (isJavaFontInstalled == false) {
            if (weight == null) {
                String test = font_family_name.toLowerCase();
                if (test.indexOf("heavy") != -1) style = Font.BOLD; else if (test.indexOf("bold") != -1) style = Font.BOLD; else if (test.indexOf("roman") != -1) style = Font.ROMAN_BASELINE;
                if (test.indexOf("italic") != -1) style = style + Font.ITALIC; else if (test.indexOf("oblique") != -1) style = style + Font.ITALIC;
            }
        }
        if (isJavaFontInstalled) return new Font(font_family_name, style, size); else {
            System.out.println("No match with " + glyphs.getBaseFontName() + " " + " " + testFont + " " + weight + " " + style);
            System.exit(1);
            return null;
        }
    }

    /**set the font used for default from Java fonts on system
	 * - check it is a valid font (otherwise it will default to Lucida anyway)
	 */
    public final void setDefaultDisplayFont(String fontName) {
        glyphs.defaultFont = fontName;
    }

    /**
	 * Returns the java font, initializing it first if it hasn't been used before.
	 */
    public final Font getJavaFontX(int size) {
        return new Font(glyphs.font_family_name, glyphs.style, size);
    }

    /**read in generic font details from the pdf file*/
    protected final void readGenericFontMetadata(Map values) {
        LogWriter.writeMethod("{readGenericFontMetadata " + fontID + "}", 0);
        String fontMatrix = (String) values.get("FontMatrix");
        if (fontMatrix != null) {
            StringTokenizer tokens = new StringTokenizer(fontMatrix, "[] ");
            for (int i = 0; i < 6; i++) {
                FontMatrix[i] = (Float.parseFloat(tokens.nextToken()));
            }
        }
        String fontBounding = (String) values.get("FontBBox");
        if (fontBounding != null) {
            StringTokenizer tokens = new StringTokenizer(fontBounding, "[] ");
            for (int i = 0; i < 4; i++) FontBBox[i] = Float.parseFloat(tokens.nextToken());
        }
        String baseFontName = currentPdfFile.getValue((String) values.get("BaseFont"));
        if (baseFontName == null) baseFontName = currentPdfFile.getValue((String) values.get("FontName"));
        if (baseFontName == null) baseFontName = fontID; else baseFontName = baseFontName.substring(1);
        if (cleanupFonts || PdfStreamDecoder.runningStoryPad) baseFontName = cleanupFontName(baseFontName);
        glyphs.fontName = baseFontName;
        int index = baseFontName.indexOf("+");
        if (index == 6) glyphs.fontName = baseFontName.substring(index + 1);
        glyphs.setBaseFontName(baseFontName);
    }

    /**
	 * get font name as a string from ID (ie Tf /F1) and load if one of Adobe 14
	 */
    public final String getFontName() {
        StandardFonts.loadStandardFontWidth(glyphs.fontName);
        return glyphs.fontName;
    }

    /**
	 * get raw font name which may include +xxxxxx
	 */
    public final String getBaseFontName() {
        return glyphs.getBaseFontName();
    }

    /**
	 * set raw font name which may include +xxxxxx
	 */
    public final void setBaseFontName(String fontName) {
        glyphs.setBaseFontName(fontName);
    }

    /**
	 * set font name
	 */
    public final void setFontName(String fontName) {
        glyphs.fontName = fontName;
    }

    /**
	 * get width of a space
	 */
    public final float getCurrentFontSpaceWidth() {
        float width;
        int space_value = spaceChar;
        if (space_value != -1) width = getWidth(space_value); else width = possibleSpaceWidth;
        if (width == -1) width = 0.2f;
        return width;
    }

    protected final int getFontEncoding(boolean notNull) {
        int result = fontEnc;
        if (result == -1 && notNull) result = StandardFonts.STD;
        return result;
    }

    /** Returns width of the specified character<br>
	 *  Allows for no value set*/
    public final float getWidth(int charInt) {
        float width = -1;
        if (widthTable != null) width = widthTable[charInt];
        if (width == -1) {
            if (isCIDFont) {
                width = defaultWidth;
            } else {
                String charName = getMappedChar(charInt, false);
                if ((charName != null) && (charName.equals(".notdef"))) charName = StandardFonts.getUnicodeChar(getFontEncoding(true), charInt);
                Float value = StandardFonts.getStandardWidth(glyphs.fontName, charName);
                if (value != null) width = value.floatValue(); else {
                    if (missingWidth != -1) width = missingWidth * xscale; else width = 0;
                }
            }
        }
        return width;
    }

    /**generic CID code
	 * @throws PdfFontException */
    public Map createCIDFont(Map values, Map descFontValues) throws PdfFontException {
        Map fontDescriptor = null;
        cachedValue = new String[65536];
        String encoding = (String) values.get("Encoding");
        if (encoding != null) handleCIDEncoding(encoding);
        Object toUnicode = values.get("ToUnicode");
        if (toUnicode != null) {
            if (toUnicode instanceof String) this.readUnicode(currentPdfFile.readStream((String) toUnicode, true), fontID); else this.readUnicode((byte[]) ((Map) toUnicode).get("DecodedStream"), fontID);
        }
        String rawWidths = currentPdfFile.getValue((String) descFontValues.get("W"));
        if (rawWidths != null) setCIDFontWidths(rawWidths);
        String defaultWidth = currentPdfFile.getValue((String) descFontValues.get("DW"));
        if (defaultWidth != null) setCIDFontDefaultWidth(defaultWidth);
        Object CIDtoGIDvalue = descFontValues.get("CIDToGIDMap");
        if (CIDtoGIDvalue != null) {
            if (CIDtoGIDvalue instanceof String) {
                String CIDtoGIDMap = (String) CIDtoGIDvalue;
                if (CIDtoGIDMap.endsWith(" R")) {
                    byte[] CIDMaps = currentPdfFile.readStream(CIDtoGIDMap, true);
                    int j = 0;
                    int[] CIDToGIDMap = new int[CIDMaps.length / 2];
                    for (int i = 0; i < CIDMaps.length; i = i + 2) {
                        int val = (((CIDMaps[i] & 255) << 8) + (CIDMaps[i + 1] & 255));
                        CIDToGIDMap[j] = val;
                        j++;
                    }
                    glyphs.setGIDtoCID(CIDToGIDMap);
                } else if (CIDtoGIDMap.equals("/Identity")) {
                    handleCIDEncoding("Identity-");
                } else {
                    LogWriter.writeLog("not yet supported in demo.");
                    System.err.println("not yet supported in demo.");
                }
            } else {
                LogWriter.writeLog("not yet supported in demo.");
                System.err.println("not yet supported in demo.");
            }
        }
        Object Info = descFontValues.get("CIDSystemInfo");
        Map CIDSystemInfo = new Hashtable();
        if (Info instanceof Map) CIDSystemInfo = (Map) Info; else CIDSystemInfo = currentPdfFile.readObject((String) Info, false, null);
        String ordering = (String) CIDSystemInfo.get("Ordering");
        if (ordering != null) {
            if (ordering.indexOf("Japan") != -1) {
                substituteFontFile = "kochi-mincho.ttf";
                substituteFontName = "Kochi Mincho";
                this.TTstreamisCID = false;
            } else if (ordering.indexOf("Korean") != -1) {
                System.err.println("Unsupported font encoding " + ordering);
            } else if (ordering.indexOf("Chinese") != -1) {
                System.err.println("Chinese " + ordering);
            } else {
            }
            if (substituteFontName != null) LogWriter.writeLog("Using font " + substituteFontName + " for " + ordering);
        }
        Object fontDescriptorRef = descFontValues.get("FontDescriptor");
        if (fontDescriptorRef != null) {
            fontDescriptor = null;
            if (fontDescriptorRef instanceof String) {
                String ref = (String) fontDescriptorRef;
                if (ref.length() > 1) fontDescriptor = currentPdfFile.readObject(ref, false, null);
            } else {
                fontDescriptor = (Map) fontDescriptorRef;
            }
            if (fontDescriptor != null) readGenericFontMetadata(fontDescriptor);
        }
        return fontDescriptor;
    }

    /**
	 *
	 */
    protected final void selectDefaultFont() {
    }

    /**read inwidth values*/
    public void readWidths(Map values) throws Exception {
        LogWriter.writeMethod("{readWidths}" + values, 0);
        String firstChar = currentPdfFile.getValue((String) values.get("FirstChar"));
        int firstCharNumber = 1;
        if (firstChar != null) firstCharNumber = ToInteger.getInteger(firstChar);
        String lastChar = currentPdfFile.getValue((String) values.get("LastChar"));
        float shortestWidth = 0;
        String width_value = currentPdfFile.getValue((String) values.get("Widths"));
        if (width_value != null) {
            widthTable = new float[maxCharCount];
            for (int ii = 0; ii < maxCharCount; ii++) widthTable[ii] = -1;
            String rawWidths = width_value.substring(1, width_value.length() - 1).trim();
            StringTokenizer widthValues = new StringTokenizer(rawWidths);
            int lastCharNumber = ToInteger.getInteger(lastChar);
            float widthValue;
            float ratio = (float) (1f / FontMatrix[0]);
            if (ratio < 0) ratio = -ratio;
            for (int i = firstCharNumber; i < lastCharNumber + 1; i++) {
                if (!widthValues.hasMoreTokens()) {
                    widthValue = 0;
                } else {
                    if (fontTypes == StandardFonts.TYPE3) widthValue = Float.parseFloat(widthValues.nextToken()) / (ratio); else widthValue = Float.parseFloat(widthValues.nextToken()) * xscale;
                    widthTable[i] = widthValue;
                    if ((widthValue > 0)) {
                        shortestWidth = shortestWidth + widthValue;
                    }
                }
            }
        }
    }

    /**read in a font and its details from the pdf file*/
    public Map createFont(Map values, String fontID, boolean renderPage, Map descFontValues, ObjectStore objectStore) throws Exception {
        LogWriter.writeMethod("{readNonCIDFont}" + values + "{render=" + renderPage, 0);
        if ((PdfJavaGlyphs.fontList == null) & (renderPage)) {
            PdfJavaGlyphs.fontList = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            int count = PdfJavaGlyphs.fontList.length;
            for (int i = 0; i < count; i++) PdfJavaGlyphs.fontList[i] = PdfJavaGlyphs.fontList[i].toLowerCase();
        }
        this.fontID = fontID;
        this.renderPage = renderPage;
        readGenericFontMetadata(values);
        String fontType = (String) values.get("Subtype");
        if (fontType == null) fontType = "";
        Object toUnicode = values.get("ToUnicode");
        if (toUnicode != null) {
            if (toUnicode instanceof String) this.readUnicode(currentPdfFile.readStream((String) toUnicode, true), fontID); else this.readUnicode((byte[]) ((Map) toUnicode).get("DecodedStream"), fontID);
        }
        String firstChar = (String) values.get("FirstChar");
        int firstCharNumber = 1;
        if (firstChar != null) firstCharNumber = ToInteger.getInteger(firstChar);
        String lastChar = (String) values.get("LastChar");
        float shortestWidth = 0;
        int count = 0;
        String width_value = currentPdfFile.getValue((String) values.get("Widths"));
        if (width_value != null) {
            widthTable = new float[maxCharCount];
            for (int ii = 0; ii < maxCharCount; ii++) widthTable[ii] = -1;
            String rawWidths = width_value.substring(1, width_value.length() - 1).trim();
            StringTokenizer widthValues = new StringTokenizer(rawWidths);
            int lastCharNumber = ToInteger.getInteger(lastChar) + 1;
            float widthValue;
            float ratio = (float) (1f / FontMatrix[0]);
            ratio = -ratio;
            for (int i = firstCharNumber; i < lastCharNumber; i++) {
                if (widthValues.hasMoreTokens() == false) widthValue = 0; else if (fontTypes == StandardFonts.TYPE3) widthValue = Float.parseFloat(widthValues.nextToken()) / (ratio); else widthValue = Float.parseFloat(widthValues.nextToken()) * xscale;
                widthTable[i] = widthValue;
                if ((widthValue > 0)) {
                    shortestWidth = shortestWidth + widthValue;
                    count++;
                }
            }
        }
        Object encValue = values.get("Encoding");
        if (encValue != null) {
            if (encValue instanceof String) handleFontEncoding((String) encValue, null, fontType); else handleFontEncoding("", (Map) encValue, fontType);
        } else {
            handleNoEncoding(0);
        }
        if (count > 0) possibleSpaceWidth = shortestWidth / (2 * count);
        Map fontDescriptor = null;
        Object rawFont = values.get("FontDescriptor");
        if (rawFont instanceof String) {
            String fontDescriptorRef = (String) rawFont;
            if (fontDescriptorRef != null && fontDescriptorRef.length() > 1) fontDescriptor = currentPdfFile.readObject(fontDescriptorRef, false, null);
        } else fontDescriptor = (Map) rawFont;
        if (fontDescriptor != null) {
            int flags = 0;
            String value = (String) fontDescriptor.get("Flags");
            if (value != null) flags = Integer.parseInt(value);
            fontFlag = flags;
            glyphs.remapFont = false;
            int flag = fontFlag;
            if ((flag & 4) == 4) glyphs.remapFont = true;
            value = currentPdfFile.getValue((String) fontDescriptor.get("MissingWidth"));
            if (value != null) missingWidth = Float.parseFloat(value);
        }
        return fontDescriptor;
    }

    /**
     *
     */
    private int handleNoEncoding(int encValue) {
        String font = getBaseFontName();
        if (font.equals("ZapfDingbats") || (font.equals("ZaDb"))) {
            putFontEncoding(StandardFonts.ZAPF);
            glyphs.defaultFont = "Zapf Dingbats";
            StandardFonts.checkLoaded(StandardFonts.ZAPF);
            encValue = StandardFonts.ZAPF;
        } else if (font.equals("Symbol")) {
            putFontEncoding(StandardFonts.SYMBOL);
            encValue = StandardFonts.SYMBOL;
        } else putFontEncoding(StandardFonts.STD);
        hasEncoding = false;
        return encValue;
    }

    /**
	 * handle font encoding and store information
	 */
    private final void handleFontEncoding(String ref, Map values, String fontType) {
        hasFontEncoding = true;
        int encoding = getFontEncoding(false);
        String encName = "";
        int encValue = encoding;
        if (encValue == -1) {
            if (fontType.equals("/TrueType")) encValue = StandardFonts.MAC; else encValue = StandardFonts.STD;
        }
        if ((ref.indexOf(" ") != -1) | (values != null)) {
            if (values == null) values = currentPdfFile.readObject(ref, false, null);
            String baseEncoding = currentPdfFile.getValue((String) values.get("BaseEncoding"));
            String diffs = currentPdfFile.getValue((String) values.get("Differences"));
            if (baseEncoding != null) {
                if (baseEncoding.startsWith("/")) baseEncoding = baseEncoding.substring(1);
                encName = baseEncoding;
                hasEncoding = true;
            } else {
                encValue = handleNoEncoding(encValue);
            }
            if (diffs != null) {
                glyphs.setIsSubsetted(true);
                diffs = Strip.removeArrayDeleminators(diffs);
                int pointer = 0;
                String ignoreValues = " \r\n";
                StringTokenizer eachValue = new StringTokenizer(diffs, " /\r\n", true);
                while (eachValue.hasMoreTokens()) {
                    String value = eachValue.nextToken();
                    if (ignoreValues.indexOf(value) != -1) {
                    } else if (value.equals("/")) {
                        value = eachValue.nextToken();
                        while (ignoreValues.indexOf(value) != -1) value = eachValue.nextToken();
                        if (value.startsWith("space")) spaceChar = pointer;
                        putMappedChar(pointer, value);
                        pointer++;
                        char c = value.charAt(0);
                        if (c == 'B' | c == 'c' | c == 'C' | c == 'G') {
                            int i = 1, l = value.length();
                            while (!isHex && i < l) isHex = Character.isLetter(value.charAt(i++));
                        }
                    } else if (Character.isDigit(value.charAt(0))) {
                        pointer = Integer.parseInt(value);
                    }
                }
            }
        } else encName = ref;
        if (encName.indexOf("MacRomanEncoding") != -1) encValue = StandardFonts.MAC; else if (encName.indexOf("WinAnsiEncoding") != -1) encValue = StandardFonts.WIN; else if (encName.indexOf("MacExpertEncoding") != -1) encValue = StandardFonts.MACEXPERT; else if ((encName.indexOf("STD") == -1) & (encValue == -1)) LogWriter.writeLog("Encoding type " + encName + " not implemented");
        if (encValue > -1) putFontEncoding(encValue);
    }

    /** Insert a new mapped char in the name mapping table */
    protected final void putMappedChar(int charInt, String mappedChar) {
        if (diffTable == null) diffTable = new String[maxCharCount];
        if (diffTable[charInt] == null) diffTable[charInt] = mappedChar;
    }

    /**
	 *holds amount of y displacement for embedded type 3font
	 */
    public double getType3Ydisplacement(int rawInt) {
        return 0;
    }

    /** Returns the char glyph corresponding to the specified code for the specified font. */
    public final String getMappedChar(int charInt, boolean remap) {
        String result = null;
        if (diffTable != null) result = diffTable[charInt];
        if ((remap) && (result != null) && (result.equals(".notdef"))) result = " ";
        if (result == null) result = StandardFonts.getUnicodeChar(getFontEncoding(true), charInt);
        if (result == null && charInt > 40 && getFontEncoding(true) == StandardFonts.WIN) result = "bullet";
        if ((isFontEmbedded) && (result == null)) {
            if (diffs != null) result = diffs[charInt];
            if ((result == null)) result = StandardFonts.getUnicodeChar(this.embeddedEnc, charInt);
        }
        return result;
    }

    public final String getEmbeddedChar(int charInt) {
        String embeddedResult = null;
        if (isFontEmbedded) {
            if (diffs != null) embeddedResult = diffs[charInt];
            if ((embeddedResult == null) && charInt < 256) embeddedResult = StandardFonts.getUnicodeChar(this.embeddedEnc, charInt);
        }
        return embeddedResult;
    }

    /**
	 * read unicode translation table
	 */
    protected final void readUnicode(byte[] data, String font_ID) {
        String line;
        int begin, end, entry, inDefinition = 0;
        BufferedReader unicode_mapping_stream = null;
        ByteArrayInputStream bis = null;
        unicodeMappings = new String[maxCharCount];
        try {
            bis = new ByteArrayInputStream(data);
            unicode_mapping_stream = new BufferedReader(new InputStreamReader(bis));
            if (unicode_mapping_stream != null) {
                while (true) {
                    line = unicode_mapping_stream.readLine();
                    if ((line == null)) break; else if (line.indexOf("endbf") != -1) inDefinition = 0;
                    if (inDefinition == 1) {
                        StringTokenizer unicode_entry = new StringTokenizer(line, " <>[]");
                        begin = Integer.parseInt(unicode_entry.nextToken(), 16);
                        String rawEntry = unicode_entry.nextToken();
                        String value = "";
                        if (rawEntry.length() < 4) {
                            entry = Integer.parseInt(unicode_entry.nextToken(), 16);
                            value = String.valueOf((char) entry);
                        } else {
                            for (int i = 0; i < rawEntry.length(); i = i + 4) {
                                entry = Integer.parseInt(rawEntry.substring(i, i + 4), 16);
                                value = value + String.valueOf((char) entry);
                            }
                        }
                        unicodeMappings[begin] = value;
                    } else if (inDefinition == 2) {
                        StringTokenizer unicode_entry = new StringTokenizer(line, " <>[]");
                        begin = Integer.parseInt(unicode_entry.nextToken(), 16);
                        end = Integer.parseInt(unicode_entry.nextToken(), 16);
                        String rawEntry = "";
                        while (unicode_entry.hasMoreTokens()) rawEntry = rawEntry + unicode_entry.nextToken();
                        int offset = 0;
                        for (int i = begin; i < end + 1; i++) {
                            String value = "";
                            int count = rawEntry.length();
                            for (int ii = 0; ii < count; ii = ii + 4) {
                                entry = Integer.parseInt(rawEntry.substring(ii, ii + 4), 16);
                                if (i + 4 > count) entry = entry + offset;
                                value = value + String.valueOf((char) entry);
                            }
                            unicodeMappings[i] = value;
                            offset++;
                        }
                    }
                    if (line.indexOf("beginbfchar") != -1) inDefinition = 1; else if (line.indexOf("beginbfrange") != -1) inDefinition = 2;
                }
            }
        } catch (Exception e) {
            LogWriter.writeLog("Exception setting up text object " + e);
        }
        if (unicode_mapping_stream != null) {
            try {
                bis.close();
                unicode_mapping_stream.close();
            } catch (IOException e1) {
                LogWriter.writeLog("Exception setting up text object " + e1);
            }
        }
    }

    /**
	 * gets type of font (ie 3 ) so we can call type
	 * specific code.
	 * @return int of type
	 */
    public final int getFontType() {
        return fontTypes;
    }

    /**
	 * name of font used to display
	 */
    public String getSubstituteFont() {
        return this.substituteFontName;
    }

    /**
	 * test if there is a valid value
	 */
    public boolean isValidCodeRange(int rawInt) {
        if (CMAP == null) return false; else {
            return (CMAP[rawInt] != null);
        }
    }

    /**used in generic renderer*/
    public float getGlyphWidth(String charGlyph, int rawInt, String displayValue) {
        if (this.fontTypes == StandardFonts.TRUETYPE) {
            return glyphs.getTTWidth(charGlyph, rawInt, displayValue, false);
        } else {
            return 0;
        }
    }

    /**set subtype (only used by generic font*/
    public void setSubtype(int fontType) {
        this.fontTypes = fontType;
    }

    /**used by JPedal internally for font substitution*/
    public void setSubstituted(boolean value) {
        this.isFontSubstituted = value;
    }

    public PdfJavaGlyphs getGlyphData() {
        return glyphs;
    }

    public Font setFont(String font, int textSize) {
        return glyphs.setFont(font, textSize);
    }

    public boolean is1C() {
        return glyphs.is1C();
    }

    public boolean isFontSubsetted() {
        return glyphs.isSubsetted;
    }

    public void setValuesForGlyph(int rawInt, String charGlyph, String displayValue, String embeddedChar) {
        glyphs.setValuesForGlyph(rawInt, charGlyph, displayValue, embeddedChar);
    }

    /**
     * remove unwanted chars from string name
     */
    String cleanupFontName(String baseFontName) {
        int length = baseFontName.length();
        StringBuffer cleanedName = new StringBuffer(length);
        for (int aa = 0; aa < length; aa++) {
            char c = baseFontName.charAt(aa);
            if (c == ' ' || c == '-') {
            } else cleanedName.append(c);
        }
        return cleanedName.toString();
    }

    public int getItalicAngle() {
        return italicAngle;
    }
}

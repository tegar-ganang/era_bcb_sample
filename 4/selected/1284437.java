package org.jpedal.fonts;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import org.jpedal.exception.PdfFontException;
import org.jpedal.fonts.tt.TTGlyphs;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.io.ObjectStore;
import org.jpedal.utils.LogWriter;

/**
 * handles truetype specifics
 *  */
public class TrueType extends PdfFont {

    public TrueType() {
    }

    public void readFontData(byte[] fontData) {
        LogWriter.writeMethod("{readFontData}", 0);
        fontTypes = glyphs.readEmbeddedFont(TTstreamisCID, fontData);
    }

    /**allows us to substitute a font to use for display
	 * @throws PdfFontException */
    protected void substituteFontUsed(String substituteFontFile, String substituteFontName) throws PdfFontException {
        InputStream from = null;
        try {
            from = loader.getResourceAsStream("org/jpedal/res/fonts/" + substituteFontFile);
        } catch (Exception e) {
            System.err.println("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");
            LogWriter.writeLog("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");
        }
        if (from == null) throw new PdfFontException("Unable to load font " + substituteFontFile);
        try {
            ByteArrayOutputStream to = new ByteArrayOutputStream();
            byte[] buffer = new byte[65535];
            int bytes_read;
            while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
            to.close();
            from.close();
            readFontData(to.toByteArray());
            glyphs.setEncodingToUse(hasEncoding, this.getFontEncoding(false), true, isCIDFont);
            isFontEmbedded = true;
        } catch (Exception e) {
            System.err.println("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");
            LogWriter.writeLog("Exception " + e + " reading " + substituteFontFile + " Check cid  jar installed");
        }
    }

    /**entry point when using generic renderer*/
    public TrueType(String substituteFont) {
        glyphs = new TTGlyphs();
        init(null);
        this.substituteFont = substituteFont;
    }

    /**get handles onto Reader so we can access the file*/
    public TrueType(PdfObjectReader current_pdf_file, String substituteFont) {
        glyphs = new TTGlyphs();
        init(current_pdf_file);
        this.substituteFont = substituteFont;
    }

    /**read in a font and its details from the pdf file*/
    public Map createFont(Map values, String fontID, boolean renderPage, Map descFontValues, ObjectStore objectStore) throws Exception {
        LogWriter.writeMethod("{readTrueTypeFont}" + values, 0);
        fontTypes = StandardFonts.TRUETYPE;
        Map fontDescriptor = super.createFont(values, fontID, renderPage, descFontValues, objectStore);
        if (renderPage) {
            if ((fontDescriptor != null)) {
                Object fontFileRef = fontDescriptor.get("FontFile2");
                try {
                    if (fontFileRef != null) {
                        byte[] stream;
                        if (fontFileRef instanceof String) stream = currentPdfFile.readStream((String) fontFileRef, true); else stream = (byte[]) ((Map) fontFileRef).get("DecodedStream");
                        readEmbeddedFont(stream, hasEncoding, false);
                    }
                } catch (Exception e) {
                }
            }
            if ((!isFontEmbedded) && (substituteFont != null)) {
                if (glyphs.remapFont) glyphs.remapFont = false;
                BufferedInputStream from;
                InputStream jarFile = loader.getResourceAsStream(substituteFont);
                if (jarFile == null) from = new BufferedInputStream(new FileInputStream(substituteFont)); else from = new BufferedInputStream(jarFile);
                ByteArrayOutputStream to = new ByteArrayOutputStream();
                byte[] buffer = new byte[65535];
                int bytes_read;
                while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
                to.close();
                from.close();
                readEmbeddedFont(to.toByteArray(), false, true);
                isFontSubstituted = true;
            }
        }
        readWidths(values);
        if (renderPage) setFont(glyphs.fontName, 1);
        return fontDescriptor;
    }

    /**read in a font and its details for generic usage*/
    public void createFont(String fontName) throws Exception {
        fontTypes = StandardFonts.TRUETYPE;
        setBaseFontName(fontName);
        BufferedInputStream from;
        InputStream jarFile = loader.getResourceAsStream(substituteFont);
        if (jarFile == null) from = new BufferedInputStream(new FileInputStream(substituteFont)); else from = new BufferedInputStream(jarFile);
        ByteArrayOutputStream to = new ByteArrayOutputStream();
        byte[] buffer = new byte[65535];
        int bytes_read;
        while ((bytes_read = from.read(buffer)) != -1) to.write(buffer, 0, bytes_read);
        to.close();
        from.close();
        readEmbeddedFont(to.toByteArray(), false, true);
        isFontSubstituted = true;
    }

    /**
	 * read truetype font data and also install font onto System
	 * so we can use
	 */
    protected final void readEmbeddedFont(byte[] font_data, boolean hasEncoding, boolean isSubstituted) {
        LogWriter.writeMethod("{readEmbeddedFont}", 0);
        try {
            LogWriter.writeLog("Embedded TrueType font used");
            readFontData(font_data);
            isFontEmbedded = true;
            glyphs.setEncodingToUse(hasEncoding, this.getFontEncoding(false), isSubstituted, TTstreamisCID);
        } catch (Exception e) {
            LogWriter.writeLog("Exception " + e + " processing TrueType font");
            e.printStackTrace();
        }
    }
}

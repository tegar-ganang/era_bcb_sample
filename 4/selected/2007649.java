package gnu.java.awt.peer.x;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Map;
import gnu.java.awt.font.FontDelegate;
import gnu.java.awt.font.FontFactory;
import gnu.java.awt.peer.ClasspathFontPeer;

public class XFontPeer2 extends ClasspathFontPeer {

    private class XLineMetrics extends LineMetrics {

        private Font font;

        private FontRenderContext fontRenderContext;

        XLineMetrics(Font f, CharacterIterator ci, int b, int l, FontRenderContext rc) {
            font = f;
            fontRenderContext = rc;
        }

        public float getAscent() {
            return fontDelegate.getAscent(font.getSize(), fontRenderContext.getTransform(), fontRenderContext.isAntiAliased(), fontRenderContext.usesFractionalMetrics(), true);
        }

        public int getBaselineIndex() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        public float[] getBaselineOffsets() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        public float getDescent() {
            return (int) fontDelegate.getDescent(font.getSize(), new AffineTransform(), false, false, false);
        }

        public float getHeight() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        public float getLeading() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        public int getNumChars() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        public float getStrikethroughOffset() {
            return 0.F;
        }

        public float getStrikethroughThickness() {
            return 0.F;
        }

        public float getUnderlineOffset() {
            return 0.F;
        }

        public float getUnderlineThickness() {
            return 0.F;
        }
    }

    private class XFontMetrics extends FontMetrics {

        XFontMetrics(Font f) {
            super(f);
        }

        public int getAscent() {
            return (int) fontDelegate.getAscent(getFont().getSize(), new AffineTransform(), false, false, false);
        }

        public int getDescent() {
            return (int) fontDelegate.getDescent(getFont().getSize(), new AffineTransform(), false, false, false);
        }

        public int getHeight() {
            GlyphVector gv = fontDelegate.createGlyphVector(getFont(), new FontRenderContext(new AffineTransform(), false, false), new StringCharacterIterator("m"));
            Rectangle2D b = gv.getVisualBounds();
            return (int) b.getHeight();
        }

        public int charWidth(char c) {
            Point2D advance = new Point2D.Double();
            fontDelegate.getAdvance(c, getFont().getSize(), new AffineTransform(), false, false, true, advance);
            return (int) advance.getX();
        }

        public int charsWidth(char[] chars, int offs, int len) {
            return stringWidth(new String(chars, offs, len));
        }

        public int stringWidth(String s) {
            GlyphVector gv = fontDelegate.createGlyphVector(getFont(), new FontRenderContext(new AffineTransform(), false, false), new StringCharacterIterator(s));
            Rectangle2D b = gv.getVisualBounds();
            return (int) b.getWidth();
        }
    }

    private FontDelegate fontDelegate;

    XFontPeer2(String name, int style, int size) {
        super(name, style, size);
        try {
            File fontfile = new File("/usr/share/fonts/truetype/ttf-bitstream-vera/Vera.ttf");
            FileInputStream in = new FileInputStream(fontfile);
            FileChannel ch = in.getChannel();
            ByteBuffer buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, fontfile.length());
            fontDelegate = FontFactory.createFonts(buffer)[0];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    XFontPeer2(String name, Map atts) {
        super(name, atts);
        try {
            File fontfile = new File("/usr/share/fonts/truetype/freefont/FreeSans.ttf");
            FileInputStream in = new FileInputStream(fontfile);
            FileChannel ch = in.getChannel();
            ByteBuffer buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, fontfile.length());
            fontDelegate = FontFactory.createFonts(buffer)[0];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean canDisplay(Font font, char c) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int canDisplayUpTo(Font font, CharacterIterator i, int start, int limit) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String getSubFamilyName(Font font, Locale locale) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String getPostScriptName(Font font) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int getNumGlyphs(Font font) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int getMissingGlyphCode(Font font) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public byte getBaselineFor(Font font, char c) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String getGlyphName(Font font, int glyphIndex) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public GlyphVector createGlyphVector(Font font, FontRenderContext frc, CharacterIterator ci) {
        return fontDelegate.createGlyphVector(font, frc, ci);
    }

    public GlyphVector createGlyphVector(Font font, FontRenderContext ctx, int[] glyphCodes) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public GlyphVector layoutGlyphVector(Font font, FontRenderContext frc, char[] chars, int start, int limit, int flags) {
        StringCharacterIterator i = new StringCharacterIterator(new String(chars), start, limit, 0);
        return fontDelegate.createGlyphVector(font, frc, i);
    }

    public FontMetrics getFontMetrics(Font font) {
        return new XFontMetrics(font);
    }

    public boolean hasUniformLineMetrics(Font font) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public LineMetrics getLineMetrics(Font font, CharacterIterator ci, int begin, int limit, FontRenderContext rc) {
        return new XLineMetrics(font, ci, begin, limit, rc);
    }

    public Rectangle2D getMaxCharBounds(Font font, FontRenderContext rc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Rectangle2D getStringBounds(Font font, CharacterIterator ci, int begin, int limit, FontRenderContext frc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

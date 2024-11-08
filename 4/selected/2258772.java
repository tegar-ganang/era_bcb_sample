package sod.io.resource;

import gnu.java.awt.font.FontDelegate;
import gnu.java.awt.font.FontFactory;
import gnu.java.awt.font.GNUGlyphVector;
import java.awt.FontFormatException;
import java.awt.Image;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import sod.exception.GlyphException;
import sod.glyph.Glyph;
import sod.io.resource.util.BitmapReader;

public class ResourceLoader {

    private ResourceLoader() {
    }

    public static Map<String, Image> loadBitmaps(String path) throws IOException {
        Map<String, Image> bitmaps = new HashMap<String, Image>();
        String[] entries = ResourceLoader.getFiles(path, ".bmp");
        if (entries == null || entries.length == 0) {
            throw new IOException(String.format("No bitmaps were found in %s.%n", path));
        }
        for (int i = 0; i < entries.length; i++) {
            File f = new File(path + "/" + entries[i]);
            bitmaps.put(entries[i], BitmapReader.read(f));
        }
        return bitmaps;
    }

    public static Map<String, Image> loadIcons(String path) throws IOException {
        Map<String, Image> icons = new HashMap<String, Image>();
        String[] entries = ResourceLoader.getFiles(path, ".gif");
        if (entries == null || entries.length == 0) {
            throw new IOException(String.format("No bitmaps were found in %s.%n", path));
        }
        for (int i = 0; entries != null && i < entries.length; i++) {
            File f = new File(path + "/" + entries[i]);
            icons.put(entries[i], ImageIO.read(ImageIO.createImageInputStream(f)));
        }
        return icons;
    }

    public static Glyph getFontGlyph(String path, Character c, float pointSize) throws FontFormatException, IOException, GlyphException {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        FontDelegate fontDelegate = null;
        FileInputStream fis = null;
        FileChannel fc = null;
        try {
            fis = new FileInputStream(path);
            fc = fis.getChannel();
            ByteBuffer buf = fc.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fc.size());
            fontDelegate = FontFactory.createFonts(buf)[0];
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    if (fc != null) {
                        fc.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }
        StringCharacterIterator iter = new StringCharacterIterator(c.toString());
        GNUGlyphVector gv = fontDelegate.createGlyphVector(pointSize, new AffineTransform(), frc, iter);
        if (gv == null || gv.getGlyphContours(0).length == 0) {
            throw new GlyphException(String.format("Glyph <%c> is invalid.%n", c));
        }
        return new Glyph(gv.getGlyphContours(0), gv.getLogicalBounds());
    }

    private static String[] getFiles(String path, final String extension) {
        File dir = new File(path);
        return dir.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(extension)) && new File(dir.getPath() + "\\" + name).isFile();
            }
        });
    }
}

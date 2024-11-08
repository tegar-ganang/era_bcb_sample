package com.google.code.appengine.imageio;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.code.appengine.imageio.ImageIO;
import com.google.code.appengine.imageio.ImageReader;
import com.google.code.appengine.imageio.ImageWriter;
import junit.framework.TestCase;

public class ImageIOTest extends TestCase {

    public void testReadURL() throws Exception {
        for (URL url : listImages()) {
            assertNotNull("Failed to load image from URL " + url, ImageIO.read(url));
            assertFalse("The current thread has been interrupted! URL: " + url, Thread.currentThread().isInterrupted());
        }
    }

    protected List<URL> listImages() {
        final String imgPath = "/images/utest.";
        final Class<? extends ImageIOTest> c = getClass();
        final List<URL> img = new LinkedList<URL>();
        img.add(c.getResource(imgPath + "jpg"));
        img.add(c.getResource(imgPath + "png"));
        return img;
    }

    public void testCache() throws Exception {
        ImageIO.setUseCache(true);
        assertTrue("Failed to enable cache", ImageIO.getUseCache());
        ImageIO.setUseCache(false);
        assertFalse("Failed to disable cache", ImageIO.getUseCache());
        ImageIO.setCacheDirectory(null);
        assertNull("Failed to set cache directory", ImageIO.getCacheDirectory());
        try {
            ImageIO.setCacheDirectory(new File(""));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    private void testFormat(String format) {
        ImageReader reader = ImageIO.getImageReadersByFormatName(format).next();
        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        assertEquals("getImageReader() returns an incorrect reader for " + format, ImageIO.getImageReader(writer).getClass(), reader.getClass());
        assertEquals("getImageWriter() returns an incorrect writer for " + format, ImageIO.getImageWriter(reader).getClass(), writer.getClass());
    }

    public void testGetJpegReaderWriter() throws Exception {
        testFormat("jpeg");
    }

    public void testGetPngReaderWriter() throws Exception {
        testFormat("png");
    }

    public void testGetNullReaderWriter() throws Exception {
        try {
            ImageIO.getImageWriter(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ImageIO.getImageReader(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testGetReaderMIMETypes() {
        Set<String> expectedMIMETypes = new HashSet<String>(Arrays.asList(new String[] { "image/gif", "image/x-png", "image/png", "image/jpeg" }));
        Set<String> actualMIMETypes = new HashSet<String>(Arrays.asList(ImageIO.getReaderMIMETypes()));
        assertTrue(actualMIMETypes.containsAll(expectedMIMETypes));
    }

    public void testGetWriterMIMETypes() {
        Set<String> expectedMIMETypes = new HashSet<String>(Arrays.asList(new String[] { "image/x-png", "image/png", "image/jpeg" }));
        Set<String> actualMIMETypes = new HashSet<String>(Arrays.asList(ImageIO.getWriterMIMETypes()));
        assertTrue(actualMIMETypes.containsAll(expectedMIMETypes));
    }

    public void testGetReaderFormatNames() {
        Set<String> expectedFormatNames = new HashSet<String>(Arrays.asList(new String[] { "JPG", "jpg", "GIF", "gif", "JPEG", "jpeg", "PNG", "png" }));
        Set<String> actualFormatNames = new HashSet<String>(Arrays.asList(ImageIO.getReaderFormatNames()));
        assertTrue(actualFormatNames.containsAll(expectedFormatNames));
    }

    public void testGetWriterFormatNames() {
        Set<String> expectedFormatNames = new HashSet<String>(Arrays.asList(new String[] { "JPG", "jpg", "JPEG", "jpeg", "PNG", "png" }));
        Set<String> actualFormatNames = new HashSet<String>(Arrays.asList(ImageIO.getWriterFormatNames()));
        assertTrue(actualFormatNames.containsAll(expectedFormatNames));
    }
}

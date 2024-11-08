package net.gencsoy.tesjeract;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import org.junit.Test;

public class TesjeractTest {

    static {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) System.loadLibrary("tessdll");
        System.loadLibrary("tesjeract");
    }

    @Test
    public void testConstructor() {
        new Tesjeract("eng");
    }

    private File getTiffFile() {
        URL resource = getClass().getResource("eurotext.tif");
        String tiffFileName = resource.getFile().replaceAll("%20", " ");
        File tiffFile = new File(tiffFileName);
        assertTrue(tiffFileName, tiffFile.exists());
        return tiffFile;
    }

    @Test
    public void recognizeTiffImage() throws IOException {
        File tiff = getTiffFile();
        MappedByteBuffer buf = new FileInputStream(tiff).getChannel().map(MapMode.READ_ONLY, 0, tiff.length());
        Tesjeract tess = new Tesjeract("eng");
        EANYCodeChar[] words = tess.recognizeAllWords(buf);
        assertEquals("There should be 352 chars in sample tiff image", 352, words.length);
        int i = 0;
        for (char c : "The(quick)".toCharArray()) {
            assertEquals((int) c, words[i++].char_code);
        }
    }

    @Test
    public void getLanguages() {
        assertFalse(Tesjeract.getLanguages().isEmpty());
    }
}

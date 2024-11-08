package com.notuvy;

import com.notuvy.file.Directory;
import org.junit.BeforeClass;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;

/**
 * Abstract base for test cases.
 *
 * @author murali
 */
public class BaseTest {

    protected static final Directory TEST_DIR = Directory.HOME.subdir(".mkutils").subdir("test");

    protected static File SOURCE_FILE = TEST_DIR.entry("source.jpg");

    protected static File TEXT_FILE = TEST_DIR.entry("original.txt");

    @BeforeClass
    public static void ensureSourceFile() throws IOException {
        ensure(SOURCE_FILE);
        ensure(TEXT_FILE);
    }

    private static void ensure(File pFile) throws IOException {
        if (!pFile.exists()) {
            FileOutputStream fos = new FileOutputStream(pFile);
            String resourceName = "/" + pFile.getName();
            InputStream is = BaseTest.class.getResourceAsStream(resourceName);
            Assert.assertNotNull(String.format("Could not find resource [%s].", resourceName), is);
            IOUtils.copy(is, fos);
            fos.close();
        }
    }

    @Test
    public void avoidAnnoyingErrorMessageWhenRunningTestsInAnt() {
        Assert.assertTrue(true);
    }
}

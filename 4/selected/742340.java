package de.wilanthaou.songbookcreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import de.wilanthaou.commons.util.ResourceUtils;

public class AbstractSongLoadingTestBase {

    protected File testSbk;

    protected File test1Sbk;

    @BeforeClass
    public static void initLocale() {
        ResourceUtils.setDefaultLocale(Locale.ENGLISH);
    }

    @Before
    public void setUp() throws IOException {
        testSbk = File.createTempFile("songbook", "sbk");
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("test.sbk"), new FileOutputStream(testSbk));
        test1Sbk = File.createTempFile("songbook", "sbk");
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("test1.sbk"), new FileOutputStream(test1Sbk));
    }

    @After
    public void tearDown() {
        if ((testSbk != null) && testSbk.exists()) {
            testSbk.delete();
        }
        if ((test1Sbk != null) && test1Sbk.exists()) {
            test1Sbk.delete();
        }
    }
}

package net.sourceforge.antme.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import net.sourceforge.antme.Constants;
import net.sourceforge.antme.Language;
import net.sourceforge.antme.jad.JadFile;
import net.sourceforge.antme.signing.KeyStoreReaderException;

public class SignTaskTest extends BaseTestTask {

    private static final String INPUT_JAD_PATH = "test/signing/MyMidlet.jad";

    private static final String OUTPUT_JAD_FILE = "output.jad";

    private static final String CORRECT_JAD_PATH = "test/signing/CorrectOutput.jad";

    public SignTaskTest(String s) {
        super(s);
    }

    public void setUp() {
        configureProject("testCases.xml", "dir.test.temp");
        Language.setTestMode(true);
    }

    public void tearDown() {
        cleanTestDirectory();
        Language.setTestMode(false);
    }

    public void testGood() throws Exception {
        executeTarget("SignTaskTest.testGood");
        JadFile result = JadFile.loadJadFile(new File(getTestDirectory(), OUTPUT_JAD_FILE), "UTF-8");
        JadFile correct = JadFile.loadJadFile(new File(CORRECT_JAD_PATH), "UTF-8");
        Properties rAttributes = result.getAttributes();
        Properties cAttributes = correct.getAttributes();
        assertEquals(cAttributes.size(), rAttributes.size());
        Iterator attrIterator = rAttributes.entrySet().iterator();
        while (attrIterator.hasNext()) {
            Map.Entry item = (Map.Entry) attrIterator.next();
            assertEquals(item.getValue(), cAttributes.get(item.getKey()));
        }
        assertEquals(correct.getMidletCount(), result.getMidletCount());
        for (int i = 0; i < correct.getMidletCount(); i++) {
            assertTrue("testGood midlet[" + i + "]", correct.getMidlet(i).equals(result.getMidlet(i)));
        }
    }

    public void testDisabled() {
        executeTarget("SignTaskTest.testDisabled");
        File output = new File(getTestDirectory(), OUTPUT_JAD_FILE);
        assertFalse(output.exists());
    }

    private static final String[] PARAM_TESTS = { "SignTaskTest.testMissingInputJad", Constants.LANG_ATTRIBUTE_REQUIRED + ":inputjad:meSignSuite", "SignTaskTest.testMissingJar", Constants.LANG_ATTRIBUTE_REQUIRED + ":jarfile:meSignSuite", "SignTaskTest.testMissingOutputJad", Constants.LANG_ATTRIBUTE_REQUIRED + ":outputjad:meSignSuite", "SignTaskTest.testMissingKeyStore", Constants.LANG_ATTRIBUTE_REQUIRED + ":keystore:meSignSuite", "SignTaskTest.testMissingStorePass", Constants.LANG_ATTRIBUTE_REQUIRED + ":storepass:meSignSuite", "SignTaskTest.testMissingAlias", Constants.LANG_ATTRIBUTE_REQUIRED + ":alias:meSignSuite", "SignTaskTest.testMissingKeyPass", Constants.LANG_ATTRIBUTE_REQUIRED + ":keypass:meSignSuite" };

    public void testMissingParameters() {
        for (int i = 0; i < PARAM_TESTS.length; i += 2) {
            executeBuildExceptionTarget(PARAM_TESTS[i]);
            assertExceptionMessageIs(PARAM_TESTS[i + 1]);
        }
    }

    public void testNoSuch() {
        executeBuildExceptionTarget("SignTaskTest.testNoSuchJad");
        assertExceptionMessageIs(Constants.LANG_NO_SUCH_FILE + ":NoSuchFile.jad");
        executeBuildExceptionTarget("SignTaskTest.testNoSuchJar");
        assertExceptionMessageIs(Constants.LANG_NO_SUCH_FILE + ":NoSuchFile.jar");
        executeBuildExceptionTarget("SignTaskTest.testNoSuchKeystore");
        assertExceptionMessageIs(Constants.LANG_NO_SUCH_FILE + ":NoSuchFile.ks");
    }

    public void testBadProviderType() {
        executeBuildExceptionTarget("SignTaskTest.testBadProviderType");
        assertExceptionMessageIs(KeyStoreReaderException.LANG_PROVIDER_NOT_CONFIGURED);
    }

    public void testLoadError() throws Exception {
        RandomAccessFile ras = null;
        FileLock lock = null;
        try {
            File theFile = new File(INPUT_JAD_PATH);
            ras = new RandomAccessFile(theFile, "rw");
            lock = ras.getChannel().lock();
            executeBuildExceptionTarget("SignTaskTest.testIOError");
            assertExceptionMessageContains("IOException");
        } finally {
            lock.release();
            ras.close();
        }
    }

    public void testSaveError() throws Exception {
        FileOutputStream stream = null;
        FileLock lock = null;
        try {
            File theFile = new File(getTestDirectory(), OUTPUT_JAD_FILE);
            stream = new FileOutputStream(theFile);
            lock = stream.getChannel().lock();
            executeBuildExceptionTarget("SignTaskTest.testIOError");
            assertExceptionMessageContains("IOException");
        } finally {
            lock.release();
            stream.close();
        }
    }
}

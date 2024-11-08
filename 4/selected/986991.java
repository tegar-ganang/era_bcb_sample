package org.dctmutils.common.bof.tbo.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dctmutils.common.FileHelper;
import org.dctmutils.common.FormatHelper;
import org.dctmutils.common.test.DctmUtilsSessionTestCase;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.common.DfException;

/**
 * Base class for DctmUtils TBO unit tests.
 * 
 * @author <a href="mailto:luther@dctmutils.org">Luther E. Birdzell</a>
 */
public abstract class DctmUtilsTboTestCase extends DctmUtilsSessionTestCase {

    private static Log log = LogFactory.getLog(DctmUtilsTboTestCase.class);

    /**
     * The name of the test text file.
     */
    public static final String TEST_TEXT_FILE = "test.txt";

    /**
     * The name of the test ms word file.
     */
    public static final String TEST_MS_WORD_FILE = "test.doc";

    /**
     * The name of the test pdf file.
     */
    public static final String TEST_PDF_FILE = "test.pdf";

    /**
     * The key in unitTest.properties that specifies whether or not unit test
     * files are deleted.
     */
    public static final String IS_TBO_CLEANUP_ACTIVE = "isTboCleanupActive";

    /**
     * 
     */
    protected static FormatHelper formatHelper = null;

    /**
     * Creates a new <code>DctmUtilsTboTestCase</code> instance.
     */
    public DctmUtilsTboTestCase() {
        super();
    }

    /**
     * Creates a new <code>DctmUtilsTboTestCase</code> instance.
     * 
     * @param name
     *            a <code>String</code> value
     */
    public DctmUtilsTboTestCase(String name) {
        super(name);
    }

    /**
     * Run before each test.
     */
    protected void setUp() {
        try {
            super.setUp();
            if (formatHelper == null) {
                formatHelper = FormatHelper.getInstance(getDfSession());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Loads the default test text file as a String.
     * 
     * @return a <code>String</code> value
     * @exception IOException
     *                if an error occurs
     */
    protected String getTestTextFileAsString() throws IOException {
        return FileHelper.getFileAsStringFromClassPath(TEST_TEXT_FILE);
    }

    /**
     * Loads the specified test file as a String.
     * 
     * @param fileName
     *            a <code>String</code> value
     * @return a <code>String</code> value
     * @exception IOException
     *                if an error occurs
     */
    protected String getTestFileAsString(String fileName) throws IOException {
        return FileHelper.getFileAsStringFromClassPath(fileName);
    }

    /**
     * Sets the content of the document with the content of the test file. The
     * test file is loaded from the classpath - content type is auto detected.
     * NOTE: this method does not call document.save(), you must do that to
     * persist these changes.
     * 
     * @param document
     *            an <code>IDfDocument</code> value
     * @exception Exception
     *                if an error occurs
     */
    protected void setDefaultTextTestContent(IDfDocument document) throws Exception {
        setTestContent(document, TEST_TEXT_FILE);
    }

    /**
     * Sets the content of the document with the content of the test file. The
     * test file is loaded from the classpath - content type is auto detected.
     * NOTE: this method does not call document.save(), you must do that to
     * persist these changes.
     * 
     * @param document
     *            an <code>IDfDocument</code> value
     * @exception Exception
     *                if an error occurs
     */
    protected void setDefaultMSWordTestContent(IDfDocument document) throws Exception {
        setTestContent(document, TEST_MS_WORD_FILE);
    }

    /**
     * Sets the content of the document with the content of the test file. The
     * test file is loaded from the classpath - content type is auto detected.
     * NOTE: this method does not call document.save(), you must do that to
     * persist these changes.
     * 
     * @param document
     *            an <code>IDfDocument</code> value
     * @exception Exception
     *                if an error occurs
     */
    protected void setDefaultPdfTestContent(IDfDocument document) throws Exception {
        setTestContent(document, TEST_PDF_FILE);
    }

    /**
     * Sets the content of the document with the content of the test file. The
     * test file is loaded from the classpath - content type is auto detected.
     * NOTE: this method does not call document.save(), you must do that to
     * persist these changes.
     * 
     * @param document
     *            an <code>IDfDocument</code> value
     * @param testFileName
     *            a <code>String</code> value
     * @exception Exception
     *                if an error occurs
     */
    protected void setTestContent(IDfDocument document, String testFileName) throws Exception {
        InputStream testFileIs = new BufferedInputStream(FileHelper.getFileAsStreamFromClassPath(testFileName));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(testFileIs, baos);
        String contentType = formatHelper.getFormatForExtension(FileHelper.getFileExtension(testFileName));
        document.setContentType(contentType);
        document.setContent(baos);
    }

    /**
     * Delete all versions of the test document.
     * 
     * @param document
     *            an <code>IDfDocument</code> value
     */
    protected void cleanup(IDfDocument document) {
        try {
            if (document != null) {
                document.setSessionManager(getDfSessionManager());
                document.destroyAllVersions();
            }
        } catch (DfException ignore) {
        }
    }

    /**
     * The IS_TBO_CLEANUP_ACTIVE property in unitTest.properties determines
     * whether or not TBO test files are deleted after the unit tests.
     * 
     * @return a <code>boolean</code> value
     */
    protected boolean isTboCleanupActive() {
        boolean isTboCleanupActive = false;
        String isTboCleanupActiveStr = getPropertyValue(IS_TBO_CLEANUP_ACTIVE);
        if (StringUtils.isNotBlank(isTboCleanupActiveStr)) {
            isTboCleanupActive = Boolean.valueOf(isTboCleanupActiveStr).booleanValue();
        }
        return isTboCleanupActive;
    }
}

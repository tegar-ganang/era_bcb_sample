package org.apache.commons.net.ftp;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A functional test suite for checking that site listings work.
 * @author <a href="mailto:brekke@apache.org">Jeffrey D. Brekke</a>
 * @version $Id: ListingFunctionalTest.java 1022913 2010-10-15 13:33:35Z sebb $
 */
public class ListingFunctionalTest extends TestCase {

    static final int HOSTNAME = 0;

    static final int VALID_PARSERKEY = 1;

    static final int INVALID_PARSERKEY = 2;

    static final int INVALID_PATH = 3;

    static final int VALID_FILENAME = 4;

    static final int VALID_PATH = 5;

    public static final Test suite() {
        String[][] testData = { { "ftp.ibiblio.org", "unix", "vms", "HA!", "javaio.jar", "pub/languages/java/javafaq" }, { "ftp.wacom.com", "windows", "VMS", "HA!", "wacom97.zip", "pub\\drivers" }, { "ftp.decuslib.com", "vms", "windows", "[.HA!]", "FREEWARE_SUBMISSION_INSTRUCTIONS.TXT;1", "[.FREEWAREV80.FREEWARE]" } };
        Class<?> clasz = ListingFunctionalTest.class;
        Method[] methods = clasz.getDeclaredMethods();
        TestSuite allSuites = new TestSuite("FTP Listing Functional Test Suite");
        for (int i = 0; i < testData.length; i++) {
            TestSuite suite = new TestSuite(testData[i][VALID_PARSERKEY] + " @ " + testData[i][HOSTNAME]);
            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];
                if (method.getName().startsWith("test")) {
                    suite.addTest(new ListingFunctionalTest(method.getName(), testData[i]));
                }
            }
            allSuites.addTest(suite);
        }
        return allSuites;
    }

    private FTPClient client;

    private String hostName;

    private String invalidParserKey;

    private String invalidPath;

    private String validFilename;

    private String validParserKey;

    private String validPath;

    /**
     * Constructor for FTPClientTest.
     *
     * @param arg0
     */
    public ListingFunctionalTest(String arg0, String[] settings) {
        super(arg0);
        invalidParserKey = settings[INVALID_PARSERKEY];
        validParserKey = settings[VALID_PARSERKEY];
        invalidPath = settings[INVALID_PATH];
        validFilename = settings[VALID_FILENAME];
        validPath = settings[VALID_PATH];
        hostName = settings[HOSTNAME];
    }

    /**
     * @param fileList
     * @param string
     *
     * @return
     */
    private boolean findByName(List<?> fileList, String string) {
        boolean found = false;
        Iterator<?> iter = fileList.iterator();
        while (iter.hasNext() && !found) {
            Object element = iter.next();
            if (element instanceof FTPFile) {
                FTPFile file = (FTPFile) element;
                found = file.getName().equals(string);
            } else {
                String filename = (String) element;
                found = filename.endsWith(string);
            }
        }
        return found;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        client = new FTPClient();
        client.connect(hostName);
        client.login("anonymous", "anonymous");
        client.enterLocalPassiveMode();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            client.logout();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (client.isConnected()) {
            client.disconnect();
        }
        client = null;
        super.tearDown();
    }

    public void testInitiateListParsing() throws IOException {
        client.changeWorkingDirectory(validPath);
        FTPListParseEngine engine = client.initiateListParsing();
        List<FTPFile> files = Arrays.asList(engine.getNext(25));
        assertTrue(files.toString(), findByName(files, validFilename));
    }

    public void testInitiateListParsingWithPath() throws IOException {
        FTPListParseEngine engine = client.initiateListParsing(validParserKey, validPath);
        List<FTPFile> files = Arrays.asList(engine.getNext(25));
        assertTrue(files.toString(), findByName(files, validFilename));
    }

    public void testInitiateListParsingWithPathAndAutodetection() throws IOException {
        FTPListParseEngine engine = client.initiateListParsing(validPath);
        List<FTPFile> files = Arrays.asList(engine.getNext(25));
        assertTrue(files.toString(), findByName(files, validFilename));
    }

    public void testInitiateListParsingWithPathAndAutodetectionButEmpty() throws IOException {
        FTPListParseEngine engine = client.initiateListParsing(invalidPath);
        assertFalse(engine.hasNext());
    }

    public void testInitiateListParsingWithPathAndIncorrectParser() throws IOException {
        FTPListParseEngine engine = client.initiateListParsing(invalidParserKey, invalidPath);
        assertFalse(engine.hasNext());
    }

    public void testListFiles() throws IOException {
        FTPClientConfig config = new FTPClientConfig(validParserKey);
        client.configure(config);
        List<FTPFile> files = Arrays.asList(client.listFiles(validPath));
        assertTrue(files.toString(), findByName(files, validFilename));
    }

    public void testListFilesWithAutodection() throws IOException {
        client.changeWorkingDirectory(validPath);
        List<FTPFile> files = Arrays.asList(client.listFiles());
        assertTrue(files.toString(), findByName(files, validFilename));
    }

    public void testListFilesWithIncorrectParser() throws IOException {
        FTPClientConfig config = new FTPClientConfig(invalidParserKey);
        client.configure(config);
        FTPFile[] files = client.listFiles(validPath);
        assertEquals(0, files.length);
    }

    public void testListFilesWithPathAndAutodectionButEmpty() throws IOException {
        FTPFile[] files = client.listFiles(invalidPath);
        assertEquals(0, files.length);
    }

    public void testListFilesWithPathAndAutodetection() throws IOException {
        List<FTPFile> files = Arrays.asList(client.listFiles(validPath));
        assertTrue(files.toString(), findByName(files, validFilename));
    }

    public void testListNames() throws IOException {
        client.changeWorkingDirectory(validPath);
        String[] names = client.listNames();
        assertNotNull(names);
        List<String> lnames = Arrays.asList(names);
        assertTrue(lnames.toString(), lnames.contains(validFilename));
    }

    public void testListNamesWithPath() throws IOException {
        String[] listNames = client.listNames(validPath);
        assertNotNull("listNames not null", listNames);
        List<String> names = Arrays.asList(listNames);
        assertTrue(names.toString(), findByName(names, validFilename));
    }

    public void testListNamesWithPathButEmpty() throws IOException {
        String[] names = client.listNames(invalidPath);
        assertNull(names);
    }
}

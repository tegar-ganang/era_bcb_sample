package test.net.sf.karatasi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.DriverManager;
import java.util.Arrays;
import net.sf.karatasi.DatabaseLibrarian;
import net.sf.karatasi.SyncServer;
import net.sf.karatasi.User;
import net.sf.karatasi.UserAuthenticator;
import org.junit.Assert;
import org.junit.Test;

/** Test for {@link SyncServer} and its sessions.
 * @author Mathias Kussinger
 */
@SuppressWarnings({ "HardcodedFileSeparator", "HardcodedLineSeparator" })
public class SyncServerTest {

    /** The size for buffers. */
    private static final int BUF_SIZE = 1024;

    /** The default test port for the SyncServer. */
    private static final int DEFAULT_TEST_PORT = 12347;

    /** Tests http username extraction from authentication string. */
    @Test
    public void testHttpAuthUsernameExtraction() {
        Assert.assertEquals("User name has to match.", "Aladdin", SyncServer.getUserNameFromHttpAuthentication("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="));
    }

    /** Test server listening on port and port change
     *
     * @throws Exception on multiple errors.
     */
    @Test
    public void testServerPortListeningAndChange() throws Exception {
        int testPort = TestHelpers.findUnboundSocket(12457);
        final SyncServer serverRunnable = new SyncServer(testPort, 0, null, null);
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        serverRunnable.start();
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
        serverRunnable.setPort(testPort);
        Socket testSocket;
        testSocket = new Socket("localhost", testPort);
        try {
            Assert.assertTrue("We should be connected to the server on default port " + testPort + '.', testSocket.isConnected());
        } finally {
            testSocket.close();
        }
        testPort = TestHelpers.findUnboundSocket(testPort + 5556);
        serverRunnable.setPort(testPort);
        testSocket = new Socket("localhost", testPort);
        try {
            Assert.assertTrue("We should be connected to the server on new port " + testPort + '.', testSocket.isConnected());
        } finally {
            testSocket.close();
        }
        serverRunnable.stop();
        Assert.assertFalse("Server must not be running now.", serverRunnable.isRunning());
    }

    /** Tests the url and http header data discretion.
     * This test checks:
     * <ul>
     * <li>The mirror response
     * <li>The url and http header data processing
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testServerHttpDataProcessing() throws Exception {
        final int testPort = TestHelpers.findUnboundSocket(DEFAULT_TEST_PORT);
        final SyncServer serverRunnable = new SyncServer(testPort, 16, null, null);
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        serverRunnable.start();
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
        final String testResult;
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /mirror?version=5&direction=just+right HTTP/1.0\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "WWW-Authenticate: Basic realm=\"karatasi\"\r\n" + "testline1: 1234567890\n" + "\r\n" + "you: shouldn't see me X-(\r\n";
            testOut.write(testQuery.getBytes());
            testOut.flush();
            final StringBuilder testResultTmp = new StringBuilder();
            final byte[] buffer = new byte[BUF_SIZE];
            for (int count; (count = testIn.read(buffer)) != -1; ) {
                testResultTmp.append(new String(buffer, 0, count));
            }
            testResult = testResultTmp.toString();
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nurl direction::just right\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nurl version::5\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody authorization::Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody host::localhost\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody www-authenticate::Basic realm=\"karatasi\"\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody testline1::1234567890\r") >= 0));
        serverRunnable.stop();
    }

    /** Tests the authentication request.
     * This test checks:
     * <ul>
     * <li>A negative authentication
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testServerAuthenticationRequest() throws Exception {
        final int testPort = TestHelpers.findUnboundSocket(DEFAULT_TEST_PORT);
        final String testDirName = TestHelpers.prepareDataDirectory();
        TestHelpers.copyDatabaseToDataDirectory("l_test1.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test2.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test3.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test4.db");
        Class.forName("org.sqlite.JDBC");
        Assert.assertNotNull("Driver has to be loaded.", DriverManager.getDriver("jdbc:sqlite:"));
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDirName);
        databaseLibrary.addDatabasesFromDirectory(testDirName);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User(1, "Aladdin", "open sesame"));
        final SyncServer serverRunnable = new SyncServer(testPort, 0, databaseLibrary, deviceAuthenticator);
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        serverRunnable.start();
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
        final StringBuilder testResultTmp = new StringBuilder();
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /list?version=5 HTTP/1.0\r\n" + "Host: localhost\r\n" + "\r\n";
            testOut.write(testQuery.getBytes());
            final byte[] buffer = new byte[BUF_SIZE];
            int count;
            while ((count = testIn.read(buffer)) >= 0) {
                testResultTmp.append(new String(buffer, 0, count));
            }
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        final String testResult = testResultTmp.toString();
        final int pos = testResult.indexOf("\r\n\r\n");
        Assert.assertTrue("Response has to contain an empty line.", (pos >= 0));
        Assert.assertTrue("Response may not contain data.", (testResult.indexOf("<dblist>") < 0));
        final String checkString = "HTTP/1.0 401 Authorization Required\r\n" + "WWW-Authenticate: Basic realm=\"karatasi\"\r\n";
        Assert.assertTrue("Response has to contain server response.", (testResult.indexOf(checkString) == 0));
        serverRunnable.stop();
        TestHelpers.removeDataDirectory();
    }

    /** Tests the download of the database list.
     * This test checks:
     * <ul>
     * <li>The format of the file list
     * <li>A positive authentication
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testFileListResponse() throws Exception {
        final int testPort = TestHelpers.findUnboundSocket(DEFAULT_TEST_PORT);
        final String testDirName = TestHelpers.prepareDataDirectory();
        TestHelpers.copyDatabaseToDataDirectory("l_test1.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test2.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test3.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test4.db");
        Class.forName("org.sqlite.JDBC");
        Assert.assertNotNull("Driver has to be loaded.", DriverManager.getDriver("jdbc:sqlite:"));
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDirName);
        databaseLibrary.addDatabasesFromDirectory(testDirName);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User(1, "Aladdin", "open sesame"));
        final SyncServer serverRunnable = new SyncServer(testPort, 0, databaseLibrary, deviceAuthenticator);
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        serverRunnable.start();
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
        final StringBuilder testResultTmp = new StringBuilder();
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /list?version=5 HTTP/1.0\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "\r\n";
            testOut.write(testQuery.getBytes());
            final byte[] buffer = new byte[BUF_SIZE];
            int count;
            while ((count = testIn.read(buffer)) >= 0) {
                testResultTmp.append(new String(buffer, 0, count));
            }
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        final String testResult = testResultTmp.toString();
        final int pos = testResult.indexOf("\r\n\r\n");
        Assert.assertTrue("Response has to contain an empty line.", (pos >= 0));
        final String fileList = testResult.substring(pos + 4, testResult.length());
        final String refList = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" + "<dblist>\r\n" + "<file><fullname>test1</fullname><size>183296</size><version>1</version><time>1231605037</time></file>\r\n" + "<file><fullname>test3</fullname><size>2793472</size><version>1</version><time>1231605120</time></file>\r\n" + "<file><fullname>test4</fullname><size>18432</size><version>3</version><time>1238240999</time></file>\r\n" + "</dblist>\r\n";
        Assert.assertEquals("File list has to look like this.", refList, fileList);
        serverRunnable.stop();
        TestHelpers.removeDataDirectory();
    }

    /** Tests the down load of a database.
     * This test checks:
     * <ul>
     * <li>The file compared to the original one
     * <li>A positive authentication
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testFileDownloadResponse() throws Exception {
        final int testPort = TestHelpers.findUnboundSocket(DEFAULT_TEST_PORT);
        final String testDirName = TestHelpers.prepareDataDirectory();
        final String testFileName = TestHelpers.copyDatabaseToDataDirectory("l_test1.db");
        Class.forName("org.sqlite.JDBC");
        Assert.assertNotNull("Driver has to be loaded.", DriverManager.getDriver("jdbc:sqlite:"));
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDirName);
        databaseLibrary.addDatabasesFromDirectory(testDirName);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User(1, "Aladdin", "open sesame"));
        final SyncServer serverRunnable = new SyncServer(testPort, 0, databaseLibrary, deviceAuthenticator);
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        serverRunnable.start();
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /download?version=5&fullname=test1&device=Aladin HTTP/1.0\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "\r\n";
            testOut.write(testQuery.getBytes());
            testOut.flush();
            int downloadFileSize = 0;
            boolean readingHeader = true;
            do {
                final byte[] inByte = new byte[1];
                final StringBuilder headerLineBuffer = new StringBuilder();
                while (testIn.read(inByte) > 0 && inByte[0] != (byte) '\n') {
                    if (inByte[0] == (byte) '\r') {
                        continue;
                    }
                    headerLineBuffer.append(new String(inByte, 0, 1, "ASCII"));
                }
                final String headerLine = headerLineBuffer.toString();
                if (headerLine.startsWith("Content-Length: ")) {
                    downloadFileSize = Integer.parseInt(headerLine.substring("Content-Length: ".length()));
                }
                if (headerLine.length() == 0) {
                    readingHeader = false;
                }
            } while (readingHeader);
            Assert.assertEquals("Download size has to be as the file size.", downloadFileSize, (new File(testFileName)).length());
            final byte[] downloadBuffer = new byte[BUF_SIZE];
            final byte[] fileBuffer = new byte[BUF_SIZE];
            assert downloadBuffer.length == fileBuffer.length;
            final InputStream fileIn = new FileInputStream(testFileName);
            int downloadCount;
            int byteSum = 0;
            while ((downloadCount = testIn.read(downloadBuffer, 0, downloadBuffer.length)) >= 0) {
                byteSum += downloadCount;
                final int fileCount = fileIn.read(fileBuffer, 0, downloadCount);
                Assert.assertEquals("File read count has to match downloadad r count.", fileCount, downloadCount);
                for (int n = 0; n < downloadCount; n++) {
                    Assert.assertEquals("File data to match downloaded data.", downloadBuffer[n], fileBuffer[n]);
                }
            }
            Assert.assertEquals("Download count has to be download size.", byteSum, (new File(testFileName)).length());
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        serverRunnable.stop();
        TestHelpers.removeDataDirectory();
    }

    /** Tests the successful up-load of a database.
     * This test checks:
     * <ul>
     * <li>The file compared to the original one
     * <li>A positive authentication
     * <li>A correct response
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testFileUploadSuccess() throws Exception {
        final int testPort = TestHelpers.findUnboundSocket(DEFAULT_TEST_PORT);
        final String testDirName = TestHelpers.prepareDataDirectory();
        Class.forName("org.sqlite.JDBC");
        Assert.assertNotNull("Driver has to be loaded.", DriverManager.getDriver("jdbc:sqlite:"));
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDirName);
        databaseLibrary.addDatabasesFromDirectory(testDirName);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User(1, "Aladdin", "open sesame"));
        final SyncServer serverRunnable = new SyncServer(testPort, 0, databaseLibrary, deviceAuthenticator);
        Assert.assertNotNull("Server runnable has to be allocated.", serverRunnable);
        serverRunnable.start();
        Assert.assertTrue("Server should be running now.", serverRunnable.isRunning());
        final String testFileName = TestHelpers.getTestStorePath() + File.separatorChar + "l_test3.db";
        final File testFile = new File(testFileName);
        final long testFileByteCount = testFile.length();
        final InputStream testFileIn = new FileInputStream(testFileName);
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "PUT /upload?version=5&device=Aladin HTTP/1.0\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "Content-Type: application/octet-stream\r\n" + "Content-Length: " + testFileByteCount + "\r\n" + "\r\n";
            testOut.write(testQuery.getBytes());
            final byte[] fileBuffer = new byte[BUF_SIZE];
            int readCount = 0;
            long writeCount = 0;
            while ((readCount = testFileIn.read(fileBuffer, 0, fileBuffer.length)) >= 0) {
                try {
                    testOut.write(fileBuffer, 0, readCount);
                } catch (final Exception e) {
                    Assert.fail("Writing of a file to server may not fail:" + e);
                    writeCount -= readCount;
                }
                writeCount += readCount;
            }
            testOut.flush();
            testFileIn.close();
            final StringBuilder resultBuffer = new StringBuilder();
            while ((readCount = testIn.read(fileBuffer, 0, fileBuffer.length)) >= 0) {
                resultBuffer.append(new String(fileBuffer, 0, readCount));
            }
            final String testResult = resultBuffer.toString();
            Assert.assertEquals("Count of bytes written has to be the size of the file.", writeCount, testFileByteCount);
            Assert.assertEquals("Response to be 200 OK.", 0, testResult.indexOf("HTTP/1.0 200 OK\r\n"));
            final String newFilePath = testDirName + File.separatorChar + "l_test3.db";
            final byte[] refFileBuffer = new byte[1000];
            final byte[] newFileBuffer = new byte[1000];
            final InputStream refFileIn = new FileInputStream(testFileName);
            final InputStream newFileIn = new FileInputStream(newFilePath);
            int refFileReadCount;
            while ((refFileReadCount = refFileIn.read(refFileBuffer, 0, 1000)) >= 0) {
                final int newFileReadCount = newFileIn.read(newFileBuffer, 0, 1000);
                Assert.assertEquals("Reference and uploaded file have to have the same size.", newFileReadCount, refFileReadCount);
                for (int n = 0; n < refFileReadCount; n++) {
                    if (refFileBuffer[n] != newFileBuffer[n]) {
                        Assert.fail("Byte in reference and uploaded file at " + n + " have to have the same value.");
                    }
                }
            }
            refFileIn.close();
            newFileIn.close();
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        serverRunnable.stop();
        TestHelpers.removeDataDirectory();
    }
}

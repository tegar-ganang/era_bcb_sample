package test.net.sf.karatasi.server;

import static test.net.sf.karatasi.TestHelpers.findUnboundSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;
import net.sf.japi.net.rest.server.RestServer;
import net.sf.japi.net.rest.server.RestServerFactory;
import net.sf.karatasi.User;
import net.sf.karatasi.UserAuthenticator;
import net.sf.karatasi.database.Database;
import net.sf.karatasi.librarian.DatabaseLibrarian;
import net.sf.karatasi.server.KaratasiAuthenticator;
import net.sf.karatasi.server.SyncServerSessionFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.net.sf.karatasi.TestHelpers;

/** Test for {@link RestServer} and its sessions.
 * @author Mathias Kussinger
 */
@SuppressWarnings({ "HardcodedFileSeparator", "HardcodedLineSeparator" })
public class SyncServerTest {

    /** The size for buffers. */
    private static final int BUF_SIZE = 1024;

    /** The default test port for the SyncServer. */
    private static final int DEFAULT_TEST_PORT = 12347;

    /** Load database driver.
     * @throws ClassNotFoundException if the JDBC driver could not be loaded.
     * @throws SQLException if an SQL error happens.
     */
    @BeforeClass
    public static void loadJDBCDriver() throws ClassNotFoundException, SQLException {
        Database.loadJDBCDriver();
        Assert.assertNotNull("Driver has to be loaded.", DriverManager.getDriver("jdbc:sqlite:"));
    }

    /** Tests http username extraction from authentication string. */
    @Test
    public void testHttpAuthUsernameExtraction() {
        Assert.assertEquals("User name has to match.", "Aladdin", KaratasiAuthenticator.getUserNameFromHttpAuthentication("Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="));
    }

    /** Test server listening on port and port change.
     *
     * @throws Exception on multiple errors.
     */
    @Test
    public void testServerPortListeningAndChange() throws Exception {
        int testPort = findUnboundSocket(12457);
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(null, null));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        syncServer.setPort(testPort);
        Socket testSocket;
        testSocket = new Socket("localhost", testPort);
        try {
            Assert.assertTrue("We should be connected to the server on default port " + testPort + '.', testSocket.isConnected());
        } finally {
            testSocket.close();
        }
        testPort = findUnboundSocket(testPort + 5556);
        syncServer.setPort(testPort);
        testSocket = new Socket("localhost", testPort);
        try {
            Assert.assertTrue("We should be connected to the server on new port " + testPort + '.', testSocket.isConnected());
        } finally {
            testSocket.close();
        }
        syncServer.stop();
        Assert.assertFalse("Server must not be running now.", syncServer.isRunning());
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
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(null, null));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        final String testResult;
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /mirror?version=5&direction=just+right HTTP/1.1\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "WWW-Authenticate: Basic realm=\"karatasi\"\r\n" + "testline1: 1234567890\n" + "\r\n" + "you: shouldn't see me X-(\r\n";
            testOut.write(testQuery.getBytes());
            testOut.flush();
            final StringBuilder testResultTmp = new StringBuilder();
            final byte[] buffer = new byte[BUF_SIZE];
            try {
                for (int count; (count = testIn.read(buffer)) != -1; ) {
                    testResultTmp.append(new String(buffer, 0, count));
                }
            } catch (final SocketException e) {
            }
            testResult = testResultTmp.toString();
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nurl direction::just right\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nurl version::5\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody Authorization::Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody Host::localhost\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody WWW-Authenticate::Basic realm=\"karatasi\"\r") >= 0));
        Assert.assertTrue("Response has to contain this line.", (testResult.indexOf("\nbody testline1::1234567890\r") >= 0));
        syncServer.stop();
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
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final File testDir = TestHelpers.prepareDataDirectory();
        TestHelpers.copyDatabaseToDataDirectory("l_test1.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test2.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test3.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test4.db");
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDir);
        databaseLibrary.initDatabasesFromDirectory(testDir);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User("Aladdin", "open sesame"));
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(databaseLibrary, deviceAuthenticator));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        final StringBuilder testResultTmp = new StringBuilder();
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /list?version=5 HTTP/1.1\r\n" + "Host: localhost\r\n" + "\r\n";
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
        final String checkString1 = "HTTP/1.1 401 Unauthorized\r\n";
        final String checkString2 = "WWW-Authenticate: Basic realm=\"karatasi\"\r\n";
        Assert.assertTrue("Response has to contain server response.", testResult.startsWith(checkString1));
        Assert.assertTrue("Response has to contain server response.", testResult.contains(checkString2));
        syncServer.stop();
        databaseLibrary.releaseDatabases();
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
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final File testDir = TestHelpers.prepareDataDirectory();
        TestHelpers.copyDatabaseToDataDirectory("l_test1.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test2.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test3.db");
        TestHelpers.copyDatabaseToDataDirectory("l_test4.db");
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDir);
        databaseLibrary.initDatabasesFromDirectory(testDir);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User("Aladdin", "open sesame"));
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(databaseLibrary, deviceAuthenticator));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        final StringBuilder testResultTmp = new StringBuilder();
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /list?version=5 HTTP/1.1\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "\r\n";
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
        final String refList = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n" + "<!DOCTYPE dblist PUBLIC \"-//Karatasi//DTD Karatasi DB List 1.0//EN\" \"/DTD/karatasiDbList1.0.dtd\">\r\n" + "<dblist>\r\n" + "    <file fullname=\"test1\" size=\"183296\" version=\"1\" time=\"1231605037\" />\r\n" + "    <file fullname=\"test2\" size=\"0\" version=\"0\" time=\"0\" />\r\n" + "    <file fullname=\"test3\" size=\"2793472\" version=\"1\" time=\"1231605120\" />\r\n" + "    <file fullname=\"test4\" size=\"18432\" version=\"3\" time=\"1238240999\" />\r\n" + "</dblist>\r\n";
        Assert.assertEquals("File list has to look like this.", refList, fileList);
        syncServer.stop();
        TestHelpers.removeDataDirectory();
    }

    /** Tests the download of the DTD file.
     * This test checks:
     * <ul>
     * <li>The existence and format elements of the DTD file
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testKaratasiDtdResponse() throws Exception {
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final File testDir = TestHelpers.prepareDataDirectory();
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDir);
        databaseLibrary.initDatabasesFromDirectory(testDir);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User("Aladdin", "open sesame"));
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(databaseLibrary, deviceAuthenticator));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        final StringBuilder testResultTmp = new StringBuilder();
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /DTD/karatasiDbList1.0.dtd HTTP/1.1\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "\r\n";
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
        final String refList = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!ELEMENT dblist (file*)>\n" + "<!ELEMENT file EMPTY>\n" + "<!ATTLIST file\n" + "    fullname CDATA #REQUIRED\n" + "    size     CDATA #REQUIRED\n" + "    version  CDATA #REQUIRED\n" + "    time     CDATA #REQUIRED\n" + ">\n";
        Assert.assertEquals("File list has to look like this.", refList, fileList);
        syncServer.stop();
        TestHelpers.removeDataDirectory();
    }

    /** Tests the down load of a database.
     * This test checks:
     * <ul>
     * <li>The file compared to the original one
     * <li>A positive authentication
     * <li>Server respects the client version when processing the download request
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testFileDownloadResponse() throws Exception {
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final File testDir = TestHelpers.prepareDataDirectory();
        final File testFile1 = TestHelpers.copyDatabaseToDataDirectory("l_test4.db");
        final File testFile2 = TestHelpers.copyDatabaseToDataDirectory("l_test2.db");
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDir);
        databaseLibrary.initDatabasesFromDirectory(testDir);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User("Aladdin", "open sesame"));
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(databaseLibrary, deviceAuthenticator));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        performDownloadWithVerification(testPort, "test4", testFile1, 5, true);
        performDownloadWithVerification(testPort, "test4", testFile1, 5, true);
        performDownloadWithVerification(testPort, "test4", testFile1, 2, false);
        performDownloadWithVerification(testPort, "test2", testFile2, 3, false);
        performDownloadWithVerification(testPort, "test4", testFile1, 3, true);
        syncServer.stop();
        TestHelpers.removeDataDirectory();
    }

    /** Helper method for database download: Request and perform download request.
     * @param serverPort the server port
     * @param fullName the requested database full name
     * @param verificationFile the file to verify the correct download
     * @param clientVersion the max. database version which is supported by the client
     * @param goodCase: true = good-case test, false = bad-case test
     * @throws Exception if something unexpected happens
     */
    private void performDownloadWithVerification(final int serverPort, final String fullName, final File verificationFile, final int clientVersion, final boolean goodCase) throws Exception {
        final Socket testSocket = new Socket("localhost", serverPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /databases/" + fullName + "?version=" + clientVersion + "&device=Aladin HTTP/1.1\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "\r\n";
            testOut.write(testQuery.getBytes());
            testOut.flush();
            int downloadFileSize = 0;
            String statusCode = "";
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
                if (headerLine.startsWith("HTTP/1.1 ")) {
                    statusCode = headerLine.substring("HTTP/1.1 ".length());
                }
                if (headerLine.startsWith("Content-Length: ")) {
                    downloadFileSize = Integer.parseInt(headerLine.substring("Content-Length: ".length()));
                }
                if (headerLine.length() == 0) {
                    readingHeader = false;
                }
            } while (readingHeader);
            if (goodCase) {
                Assert.assertEquals("Status code 200 expected.", "200 OK", statusCode);
                Assert.assertEquals("Download size has to be as the file size.", downloadFileSize, verificationFile.length());
                final byte[] downloadBuffer = new byte[BUF_SIZE];
                final byte[] fileBuffer = new byte[BUF_SIZE];
                assert downloadBuffer.length == fileBuffer.length;
                int byteSum = 0;
                final InputStream fileIn = new FileInputStream(verificationFile);
                try {
                    int downloadCount;
                    while ((downloadCount = testIn.read(downloadBuffer, 0, downloadBuffer.length)) >= 0) {
                        byteSum += downloadCount;
                        final int fileCount = fileIn.read(fileBuffer, 0, downloadCount);
                        if (goodCase) {
                            Assert.assertEquals("File read count has to match downloaded r count.", fileCount, downloadCount);
                            for (int n = 0; n < downloadCount; n++) {
                                Assert.assertEquals("File data to match downloaded data.", downloadBuffer[n], fileBuffer[n]);
                            }
                        }
                    }
                } finally {
                    fileIn.close();
                }
                Assert.assertEquals("Download count has to be download size.", byteSum, verificationFile.length());
            } else {
                Assert.assertEquals("Status code 404 expected.", "404 Not Found", statusCode);
            }
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
    }

    /** Tests the up-load of a database.
     * This test checks:
     * <ul>
     * <li>The file compared to the original one
     * <li>A positive authentication
     * <li>A correct response for uploaded new database
     * <li>A correct response for uploaded existing database
     * <li>Correct response for conflict scenario (database cannot be borrowed)
     * <li>Correct response for uploaded new broken database
     * <li>Correct response for uploaded existing broken database.
     * <li>Correct response for uploaded new (broken) database with 0 bytes.
     * <li>Correct response for uploaded existing (broken) database with 0 bytes.
     * </ul>
     *
     * @throws Exception if file doesn't exist or copy fails.
     */
    @Test
    public void testFileUpload() throws Exception {
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final File testDir = TestHelpers.prepareDataDirectory();
        final File srcDir = TestHelpers.prepareAlternateDataDirectory();
        TestHelpers.copyDatabaseToAlternateDataDirectory("l_test3.db");
        TestHelpers.copyDatabaseToAlternateDataDirectory("l_test2.db");
        final File empty = new File(srcDir, "l_empty.db");
        if (!empty.createNewFile()) {
            Assert.fail();
        }
        Assert.assertEquals(0, empty.length());
        final DatabaseLibrarian databaseLibrary = new DatabaseLibrarian(testDir);
        databaseLibrary.initDatabasesFromDirectory(testDir);
        final UserAuthenticator deviceAuthenticator = new UserAuthenticator();
        deviceAuthenticator.addUser(new User("Aladdin", "open sesame"));
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(databaseLibrary, deviceAuthenticator));
        Assert.assertNotNull("Server runnable has to be allocated.", syncServer);
        syncServer.start();
        Assert.assertTrue("Server should be running now.", syncServer.isRunning());
        String srcName = "l_test3.db";
        String targetName = "l_test3.db";
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 201 Created\r\n", targetName, true);
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 200 OK\r\n", targetName, true);
        final Database database = databaseLibrary.borrowDatabase("test3");
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 409 Conflict\r\n", targetName, false);
        databaseLibrary.returnDatabase(database);
        srcName = targetName = "l_test2.db";
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 201 Created\r\n", targetName, true);
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 200 OK\r\n", targetName, true);
        final File srcFile = new File(srcDir, "l_test3.db");
        final Database db = new Database(srcFile);
        db.open();
        db.renameTo("Jürgen");
        db.close();
        srcName = "l_test3.db";
        targetName = "l_J%C3%BCrgen.db";
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 201 Created\r\n", targetName, true);
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 200 OK\r\n", targetName, true);
        srcName = targetName = "l_empty.db";
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 201 Created\r\n", targetName, true);
        performUploadWithVerification(testPort, testDir, srcDir, srcName, "HTTP/1.1 200 OK\r\n", targetName, true);
        syncServer.stop();
        TestHelpers.removeDataDirectory();
        TestHelpers.removeAlternateDataDirectory();
    }

    /** helper method to test the upload of a file.
     * @param serverPort the server port
     * @param workingDir the working directory of the server / librarian
     * @param srcDir the source directory for upload
     * @param filenameForUpload the file name to be uploaded
     * @param expectedResponse the first part of the expected server response
     * @param targetName filename after reception (w/o path)
     * @param success whether it is a goodcase or a badcase test
     * @throws UnknownHostException
     * @throws IOException
     * @throws Exception
     * @throws FileNotFoundException
     */
    private void performUploadWithVerification(final int serverPort, final File workingDir, final File srcDir, final String filenameForUpload, final String expectedResponse, final String targetName, final boolean success) throws UnknownHostException, IOException, Exception, FileNotFoundException {
        final Socket testSocket = new Socket("localhost", serverPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            try {
                final OutputStream testOut = testSocket.getOutputStream();
                try {
                    final File fileForUpload = new File(srcDir, filenameForUpload);
                    uploadFileToServer(testOut, fileForUpload, success);
                    final String testResult = receiveServerResponse(testIn);
                    Assert.assertEquals(0, testResult.indexOf(expectedResponse));
                    if (success) {
                        final File transmittedFile = new File(workingDir, targetName);
                        TestHelpers.compareFiles(fileForUpload, transmittedFile);
                    }
                } finally {
                    testOut.close();
                }
            } finally {
                testIn.close();
            }
        } finally {
            testSocket.close();
        }
    }

    /** Helper method for upload: upload the database file to the server (send request).
     * @param streamToServer the stream to the server
     * @param testFile the file to be uploaded
     * @param isGoodCase true = goodcase test / false = badcase test
     * @throws Exception when writing to the server fails unexpectedly
     */
    private void uploadFileToServer(final OutputStream streamToServer, final File testFile, final boolean isGoodCase) throws Exception {
        final long fileLength = testFile.length();
        final Database db = new Database(testFile);
        final String fullName = db.getFullName();
        long numOfBytesTransmitted = 0;
        final byte[] fileBuffer = new byte[BUF_SIZE];
        final InputStream testFileIn = new FileInputStream(testFile);
        try {
            final String testQuery = "PUT /databases/" + URLEncoder.encode(fullName, "UTF-8") + "?version=5&device=Aladin HTTP/1.1\r\n" + "Host: localhost\r\n" + "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" + "Content-Type: application/octet-stream\r\n" + "Content-Length: " + fileLength + "\r\n" + "\r\n";
            streamToServer.write(testQuery.getBytes());
            try {
                long readCount;
                while (fileLength - numOfBytesTransmitted > 0) {
                    readCount = fileLength - numOfBytesTransmitted;
                    if (readCount > fileBuffer.length) {
                        readCount = fileBuffer.length;
                    }
                    readCount = testFileIn.read(fileBuffer, 0, (int) readCount);
                    if (readCount >= 0) {
                        streamToServer.write(fileBuffer, 0, (int) readCount);
                        numOfBytesTransmitted += readCount;
                    }
                }
                streamToServer.flush();
            } catch (final IOException e) {
                System.err.println("uploadFileToServer: IOException, reason = " + e.getMessage());
                Assert.fail("Writing of a file to server may not fail");
            }
        } finally {
            testFileIn.close();
        }
        if (isGoodCase) {
            Assert.assertEquals("Number of bytes transmitted has to be the size of the file.", numOfBytesTransmitted, fileLength);
        }
    }

    /** Helper method for database upload: receive the response from the server.
     * @param streamFromServer an open stream where the server sends the response
     * @return the server response as String
     * @throws IOException
     */
    private String receiveServerResponse(final InputStream streamFromServer) throws IOException {
        final byte[] resultBuffer = new byte[BUF_SIZE];
        final StringBuilder resultString = new StringBuilder();
        try {
            for (int readCount; (readCount = streamFromServer.read(resultBuffer, 0, resultBuffer.length)) >= 0; ) {
                resultString.append(new String(resultBuffer, 0, readCount));
            }
        } catch (final IOException e) {
            System.err.println("receiveServerResponse: IOException, reason = " + e.getMessage());
            Assert.fail("Reading the server response  may not throw an exception");
        }
        return resultString.toString();
    }

    /** Tests that sending an invalid request is answered with 400 Bad Request.
     * @throws Exception (unexpected)
     */
    @Test
    public void testBadRequest() throws Exception {
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(null, null));
        syncServer.start();
        final String testResult;
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET /mirror?version=5&direction=just+right HTTP/1.1 foo\r\n" + "Host: localhost\r\n" + "\r\n";
            testOut.write(testQuery.getBytes());
            testOut.flush();
            final StringBuilder testResultTmp = new StringBuilder();
            final byte[] buffer = new byte[BUF_SIZE];
            try {
                for (int count; (count = testIn.read(buffer)) != -1; ) {
                    testResultTmp.append(new String(buffer, 0, count));
                }
            } catch (final SocketException e) {
            }
            testResult = testResultTmp.toString();
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        Assert.assertTrue(Pattern.compile("^HTTP/1\\.[01] 400 Bad Request.*", Pattern.DOTALL).matcher(testResult).matches());
    }

    /** Tests that sending a request with an unsupported Content-* header is answered with 501 Not Implemented.
     * @throws Exception (unexpected)
     */
    @Test
    public void testUploadContentNotImplemented() throws Exception {
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(null, null));
        syncServer.start();
        final String testResult;
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "PUT /databases/test1 HTTP/1.1\r\n" + "Host: localhost\r\n" + "Content-Foo: foobar\r\n" + "Content-Length: 3\r\n" + "\r\n" + "FOO";
            testOut.write(testQuery.getBytes());
            testOut.flush();
            final StringBuilder testResultTmp = new StringBuilder();
            final byte[] buffer = new byte[BUF_SIZE];
            try {
                for (int count; (count = testIn.read(buffer)) != -1; ) {
                    testResultTmp.append(new String(buffer, 0, count));
                }
            } catch (final SocketException e) {
            }
            testResult = testResultTmp.toString();
            testIn.close();
            testOut.close();
        } finally {
            testSocket.close();
        }
        Assert.assertTrue(Pattern.compile("^HTTP/1\\.[01] 501 Not Implemented.*", Pattern.DOTALL).matcher(testResult).matches());
    }

    /** Tests that an incomplete header is answered with 400 Bad Request.
     * @throws Exception (unexpected)
     */
    @Test
    public void testIncompleteRequest() throws Exception {
        final int testPort = findUnboundSocket(DEFAULT_TEST_PORT);
        final RestServer syncServer = RestServerFactory.getInstance().newRestServer(testPort, new SyncServerSessionFactory(null, null));
        syncServer.start();
        final Socket testSocket = new Socket("localhost", testPort);
        try {
            final InputStream testIn = testSocket.getInputStream();
            final OutputStream testOut = testSocket.getOutputStream();
            final String testQuery = "GET / HTTP/1.1\r\n";
            testOut.write(testQuery.getBytes("UTF-8"));
            testOut.flush();
            testSocket.shutdownOutput();
            final StringBuilder testResultTmp = new StringBuilder();
            final byte[] buffer = new byte[BUF_SIZE];
            for (int count; (count = testIn.read(buffer)) != -1; ) {
                testResultTmp.append(new String(buffer, 0, count));
            }
            final String testResult = testResultTmp.toString();
            Assert.assertTrue(Pattern.compile("^HTTP/1\\.[01] 400 Bad Request.*", Pattern.DOTALL).matcher(testResult).matches());
        } finally {
            testSocket.close();
        }
    }
}

package fedora.server.journal.readerwriter.multifile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import fedora.server.errors.ServerException;
import fedora.server.journal.JournalConstants;
import fedora.server.journal.JournalConsumer;
import fedora.server.journal.ServerInterface;

public class TestLockingFollowingJournalReader extends TestCase implements JournalConstants, MultiFileJournalConstants {

    private static final int WAIT_INTERVAL = 5;

    private static final String JOURNAL_FILENAME_PREFIX = "unit";

    private static final String DUMMY_HASH_VALUE = "Dummy Hash";

    private File journalDirectory;

    private File archiveDirectory;

    private File lockRequestFile;

    private File lockAcceptedFile;

    private Map parameters;

    private ServerInterface server;

    private String role = "DumbGrunt";

    private MockManagementDelegateForJournalTesting delegate;

    private int initialNumberOfThreads;

    public TestLockingFollowingJournalReader(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        journalDirectory = createTempDirectory("fedoraTestingJournalFiles");
        archiveDirectory = createTempDirectory("fedoraTestingArchiveFiles");
        lockRequestFile = new File(journalDirectory.getPath() + File.separator + "lockRequested");
        lockRequestFile.delete();
        lockAcceptedFile = new File(journalDirectory.getPath() + File.separator + "lockAccepted");
        lockAcceptedFile.delete();
        server = new MockServerForJournalTesting(DUMMY_HASH_VALUE);
        parameters = new HashMap();
        parameters.put(PARAMETER_JOURNAL_RECOVERY_LOG_CLASSNAME, "fedora.server.journal.readerwriter.multifile." + "MockJournalRecoveryLogForJournalTesting");
        parameters.put(PARAMETER_JOURNAL_READER_CLASSNAME, "fedora.server.journal.readerwriter.multifile." + "LockingFollowingJournalReader");
        parameters.put(PARAMETER_JOURNAL_DIRECTORY, journalDirectory.getPath());
        parameters.put(PARAMETER_ARCHIVE_DIRECTORY, archiveDirectory.getPath());
        parameters.put(PARAMETER_FOLLOW_POLLING_INTERVAL, "1");
        parameters.put(PARAMETER_JOURNAL_FILENAME_PREFIX, JOURNAL_FILENAME_PREFIX);
        parameters.put(PARAMETER_LOCK_REQUESTED_FILENAME, lockRequestFile.getPath());
        parameters.put(PARAMETER_LOCK_ACCEPTED_FILENAME, lockAcceptedFile.getPath());
        delegate = new MockManagementDelegateForJournalTesting();
        initialNumberOfThreads = getNumberOfCurrentThreads();
    }

    /**
     * Create 3 files and watch it process all of them
     */
    public void testSimpleNoLocking() {
        try {
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            JournalConsumer consumer = new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();
            assertEquals("Expected to see 3 ingests", 3, delegate.getIngestCalls());
            assertEquals("Journal files not all gone", 0, howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files", 3, howManyFilesInDirectory(archiveDirectory));
            System.out.println(MockJournalRecoveryLogForJournalTesting.getInstance().getLogSummary());
        } catch (Throwable e) {
            processException(e);
        }
    }

    /**
     * A lock request created before startup will prevent processing. When the
     * request is removed, processing will occur.
     */
    public void testLockBeforeStartingAndResume() {
        try {
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createLockRequest();
            JournalConsumer consumer = new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);
            waitForLockAccepted();
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("Journal files should not be processed", 0, delegate.getIngestCalls());
            assertEquals("Journal files should not be processed", 3, howManyFilesInDirectory(journalDirectory));
            assertEquals("Journal files should not be processed", 0, howManyFilesInDirectory(archiveDirectory));
            int lockMessageIndex = assertLockMessageInLog();
            removeLockRequest();
            waitForLockReleased();
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();
            assertEquals("Expected to see 3 ingests", 3, delegate.getIngestCalls());
            assertEquals("Journal files not all gone", 0, howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files", 3, howManyFilesInDirectory(archiveDirectory));
            assertUnlockMessageInLog(lockMessageIndex);
            System.out.println(MockJournalRecoveryLogForJournalTesting.getInstance().getLogSummary());
        } catch (Throwable e) {
            processException(e);
        }
    }

    /**
     * A lock request created while a file is in progress, which should prevent
     * further processing until it is removed.
     */
    public void testLockWhileProcessingAndResume() {
        try {
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            createJournalFileFromString(getSimpleIngestString());
            delegate.setIngestOperation(new LockAfterSecondIngest());
            JournalConsumer consumer = new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);
            waitForLockAccepted();
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("We should stop after the second ingest", 2, delegate.getIngestCalls());
            assertEquals("One Journal file should not be processed", 1, howManyFilesInDirectory(journalDirectory));
            assertEquals("Only two Journal files should be processed", 2, howManyFilesInDirectory(archiveDirectory));
            int lockMessageIndex = assertLockMessageInLog();
            removeLockRequest();
            waitForLockReleased();
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();
            assertEquals("Expected to see 3 ingests", 3, delegate.getIngestCalls());
            assertEquals("Journal files not all gone", 0, howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files", 3, howManyFilesInDirectory(archiveDirectory));
            assertUnlockMessageInLog(lockMessageIndex);
            System.out.println(MockJournalRecoveryLogForJournalTesting.getInstance().getLogSummary());
        } catch (Throwable e) {
            processException(e);
        }
    }

    /**
     * A lock request created while the system if polling, which should prevent
     * further processing until it is removed.
     * 
     * Create 1 files and watch it process all of them. Create a lock and wait
     * for the ack. Create a 2nd file, and it will not be processed. Remove the
     * lock; ack is removed and last file is processed.
     */
    public void testLockWhilePollingAndResume() {
        try {
            createJournalFileFromString(getSimpleIngestString());
            JournalConsumer consumer = new JournalConsumer(parameters, role, server);
            startConsumerThread(consumer);
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("The first file should have been processed.", 1, delegate.getIngestCalls());
            assertEquals("The first file should have been processed.", 0, howManyFilesInDirectory(journalDirectory));
            assertEquals("The first file should have been processed.", 1, howManyFilesInDirectory(archiveDirectory));
            createLockRequest();
            waitForLockAccepted();
            createJournalFileFromString(getSimpleIngestString());
            waitWhileThreadRuns(WAIT_INTERVAL);
            assertEquals("The second file should not have been processed.", 1, delegate.getIngestCalls());
            assertEquals("The second file should not have been processed.", 1, howManyFilesInDirectory(journalDirectory));
            assertEquals("The second file should not have been processed.", 1, howManyFilesInDirectory(archiveDirectory));
            int lockMessageIndex = assertLockMessageInLog();
            removeLockRequest();
            waitForLockReleased();
            waitWhileThreadRuns(WAIT_INTERVAL);
            consumer.shutdown();
            assertEquals("Expected to see 2 ingests", 2, delegate.getIngestCalls());
            assertEquals("Journal files not all gone", 0, howManyFilesInDirectory(journalDirectory));
            assertEquals("Wrong number of archive files", 2, howManyFilesInDirectory(archiveDirectory));
            assertUnlockMessageInLog(lockMessageIndex);
            System.out.println(MockJournalRecoveryLogForJournalTesting.getInstance().getLogSummary());
        } catch (Throwable e) {
            processException(e);
        }
    }

    private void createLockRequest() throws IOException {
        lockRequestFile.createNewFile();
    }

    private void removeLockRequest() {
        lockRequestFile.delete();
    }

    private int howManyFilesInDirectory(File directory) {
        return MultiFileJournalHelper.getSortedArrayOfJournalFiles(directory, JOURNAL_FILENAME_PREFIX).length;
    }

    /**
     * Wait until the JournalConsumerThread stops, or until the time limit
     * expires, whichever comes first.
     */
    private void waitWhileThreadRuns(int maxSecondsToWait) {
        for (int i = 0; i < maxSecondsToWait; i++) {
            if (getNumberOfCurrentThreads() == initialNumberOfThreads) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wait until the lock is accepted, or until the time runs out. If the
     * latter, complain.
     */
    private void waitForLockAccepted() {
        int maxWait = 3;
        for (int i = 0; i < maxWait; i++) {
            if (lockAcceptedFile.exists()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fail("Lock was not accepted after " + maxWait + " seconds.");
    }

    /**
     * Wait until the lock is released, or until the time runs out. If the
     * latter, complain.
     */
    private void waitForLockReleased() {
        int maxWait = 3;
        for (int i = 0; i < maxWait; i++) {
            if (!lockAcceptedFile.exists()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        fail("Lock was not released after " + maxWait + " seconds.");
    }

    /**
     * Set the ManagementDelegate into the JournalConsumer, which will create
     * the JournalConsumerThread.
     */
    private void startConsumerThread(JournalConsumer consumer) {
        consumer.setManagementDelegate(delegate);
    }

    private int getNumberOfCurrentThreads() {
        int i = Thread.currentThread().getThreadGroup().enumerate(new Thread[500]);
        System.out.println("There are " + i + " threads in the group");
        return i;
    }

    private void createJournalFileFromString(String text) throws IOException {
        File journal = File.createTempFile(JOURNAL_FILENAME_PREFIX, null, journalDirectory);
        journal.deleteOnExit();
        FileWriter writer = new FileWriter(journal);
        writer.write(text);
        writer.close();
    }

    /**
     * Confirm that the last message in the log is a lock message, and return
     * its position in the log.
     */
    private int assertLockMessageInLog() {
        List messages = MockJournalRecoveryLogForJournalTesting.getInstance().getLogMessages();
        int lastMessageIndex = messages.size() - 1;
        String lastMessage = (String) messages.get(lastMessageIndex);
        assertStringStartsWith(lastMessage, "Lock request detected:");
        return lastMessageIndex;
    }

    /**
     * Confirm that the log message following the lock message is in fact an
     * unlock message.
     */
    private void assertUnlockMessageInLog(int lockMessageIndex) {
        List messages = MockJournalRecoveryLogForJournalTesting.getInstance().getLogMessages();
        int unlockMessageIndex = lockMessageIndex + 1;
        assertTrue(messages.size() > unlockMessageIndex);
        String unlockMessage = (String) messages.get(unlockMessageIndex);
        assertStringStartsWith(unlockMessage, "Lock request removed");
    }

    private void assertStringStartsWith(String string, String prefix) {
        if (!string.startsWith(prefix)) {
            fail("String does not start as expected: string='" + string + "', prefix='" + prefix + "'");
        }
    }

    private void processException(Throwable e) {
        if (e instanceof ServerException) {
            System.err.println("ServerException: code='" + ((ServerException) e).getCode() + "', class='" + e.getClass().getName() + "'");
            StackTraceElement[] traces = e.getStackTrace();
            for (int i = 0; i < traces.length; i++) {
                System.err.println(traces[i]);
            }
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }
            fail("Threw a ServerException");
        } else {
            e.printStackTrace();
            fail("Threw an exception");
        }
    }

    private File createTempDirectory(String name) {
        File directory = new File(System.getProperty("java.io.tmpdir"), name);
        directory.mkdir();
        cleanOutDirectory(directory);
        directory.deleteOnExit();
        return directory;
    }

    private void cleanOutDirectory(File directory) {
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        }
    }

    private String getSimpleIngestString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<FedoraJournal repositoryHash=\"" + DUMMY_HASH_VALUE + "\" timestamp=\"2006-08-11T11:14:43.011-0400\">\n" + "  <JournalEntry method=\"ingestObject\" timestamp=\"2006-08-11T11:14:42.690-0400\" clientIpAddress=\"128.84.103.30\" loginId=\"fedoraAdmin\">\n" + "    <context>\n" + "      <password>junk</password>\n" + "      <noOp>false</noOp>\n" + "      <now>2006-08-11T11:14:42.690-0400</now>\n" + "      <multimap name=\"environment\">\n" + "        <multimapkey name=\"urn:fedora:names:fedora:2.1:environment:httpRequest:authType\">\n" + "          <multimapvalue>BASIC</multimapvalue>\n" + "        </multimapkey>\n" + "      </multimap>\n" + "      <multimap name=\"subject\"></multimap>\n" + "      <multimap name=\"action\"> </multimap>\n" + "      <multimap name=\"resource\"></multimap>\n" + "      <multimap name=\"recovery\"></multimap>\n" + "    </context>\n" + "    <argument name=\"serialization\" type=\"stream\">PD94</argument>\n" + "    <argument name=\"message\" type=\"string\">Minimal Ingest sample</argument>\n" + "    <argument name=\"format\" type=\"string\">foxml1.0</argument>\n" + "    <argument name=\"encoding\" type=\"string\">UTF-8</argument>\n" + "    <argument name=\"newPid\" type=\"boolean\">true</argument>\n" + "  </JournalEntry>\n" + "</FedoraJournal>\n";
    }

    /**
     * Set one of these as the ingest object on the ManagementDelegate. When the
     * second ingest operation begins, a Lock Request will be created.
     */
    private final class LockAfterSecondIngest implements Runnable {

        public void run() {
            if (delegate.getIngestCalls() == 2) {
                try {
                    createLockRequest();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

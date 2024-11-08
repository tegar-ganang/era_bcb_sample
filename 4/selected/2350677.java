package net.sourceforge.processdash.util.lock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileConcurrencyLock implements ConcurrencyLock {

    private File lockFile;

    private String lockToken;

    private String extraInfo;

    private ConcurrencyLockApprover approver = null;

    private FileChannel lockChannel = null;

    private FileLock lock = null;

    private LockWatcher messageHandler = null;

    private boolean listenForLostLock = true;

    private Thread shutdownHook = null;

    private static Logger logger = Logger.getLogger(FileConcurrencyLock.class.getName());

    public FileConcurrencyLock(File lockFile) {
        this(lockFile, null);
    }

    public FileConcurrencyLock(File lockFile, String lockToken) {
        if (lockFile == null) throw new NullPointerException("lockFile cannot be null");
        if (lockToken == null) lockToken = Long.toString(System.currentTimeMillis());
        this.lockFile = lockFile;
        this.lockToken = lockToken;
    }

    public File getLockFile() {
        return lockFile;
    }

    public String getLockToken() {
        return lockToken;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public boolean isListenForLostLock() {
        return listenForLostLock;
    }

    public void setListenForLostLock(boolean listenForLostLock) {
        this.listenForLostLock = listenForLostLock;
    }

    public ConcurrencyLockApprover getApprover() {
        return approver;
    }

    public void setApprover(ConcurrencyLockApprover approver) {
        this.approver = approver;
    }

    public String getLockHash() {
        return Integer.toString(Math.abs(lockFile.hashCode()), Character.MAX_RADIX);
    }

    public void acquireLock(String extraInfo) throws LockFailureException {
        acquireLock(null, null, extraInfo);
    }

    /** Obtain a concurrency lock for the data in the given directory.
    *
    * @param lockFile a file symbolizing data we want to lock.  Clients must
    *     use their own convention for associating lock files with associated
    *     protected data.  This class will need to create the named lock file
    *     in order to successfully obtain a lock.
    * @param message (optional) a message to send to the owner of the lock.
    *     The message cannot contain carriage return or newline characters.
    *     If this parameter is null, no attempt will be made to contact the
    *     other process.
    * @param listener (optional) a listener who will receive messages from
    *     other processes which want this lock.
    * @param extraInfo (optional) an arbitrary string of text (containing no
    *     carriage return or newline characters) to write into the lock file.
    *     This could be used by clients to identify the owner of the lock in
    *     a human-meaningful way.
    *
    * @throws SentLockMessageException if someone else already owns this lock, and
    *     we were able to send a message to them.  The exception will include
    *     the response from that owner.
    * @throws AlreadyLockedException if someone else already owns this lock,
    *     but we were unable to contact them.
    * @throws LockFailureException if the lock could not be obtained for any other
    *     reason.
    */
    public synchronized void acquireLock(String message, LockMessageHandler listener, String extraInfo) throws SentLockMessageException, AlreadyLockedException, LockFailureException {
        try {
            if (approver != null) approver.approveLock(this, extraInfo);
            lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = lockChannel.tryLock(0, 1, false);
            if (lock != null) {
                if (listener != null) messageHandler = new LockWatcher(listener, extraInfo); else writeLockMetaData("", 0, lockToken, extraInfo);
                this.extraInfo = extraInfo;
                registerShutdownHook();
            } else {
                tryToSendMessage(message);
            }
        } catch (LockFailureException fe) {
            throw fe;
        } catch (Exception e) {
            releaseLock();
            throw new CannotCreateLockException(e);
        }
        logger.log(Level.FINE, "Obtained lock for: {0}", lockFile);
    }

    public synchronized boolean isLocked() {
        return (lock != null);
    }

    public void assertLock() throws LockFailureException {
        assertLock(false);
    }

    private synchronized void assertLock(boolean forceNativeReassert) throws LockFailureException {
        if (!lockFile.getParentFile().isDirectory()) {
            logger.log(Level.FINE, "Lock directory does not exist for {0}", lockFile);
            throw new LockUncertainException();
        }
        if (!lockFile.exists()) {
            logger.log(Level.FINE, "Lock file no longer exists: {0}", lockFile);
            throw new NotLockedException();
        }
        try {
            FileChannel oldLockChannel = lockChannel;
            FileChannel newLockChannel = lockChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            LockMetaData metaData = readLockMetaData(false);
            if (metaData == null) {
                logger.log(Level.FINE, "Lock file could not be read: {0}", lockFile);
                closeChannel(oldLockChannel);
                throw new AlreadyLockedException(null);
            }
            if (!lockToken.equals(metaData.token)) {
                logger.log(Level.FINE, "Lock was lost: {0}", lockFile);
                closeChannel(oldLockChannel);
                throw new AlreadyLockedException(metaData.extraInfo);
            }
            this.extraInfo = metaData.extraInfo;
            if (lock == null && approver != null) approver.approveLock(this, extraInfo);
            if (forceNativeReassert) {
                try {
                    if (lock != null) lock.release();
                    closeChannel(oldLockChannel);
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.log(Level.FINE, "Exception when releasing lock for native reassert", e);
                }
                lock = null;
            }
            if (lock == null) {
                lock = lockChannel.tryLock(0, 1, false);
            } else {
                this.lockChannel = oldLockChannel;
                closeChannel(newLockChannel);
            }
            if (lock == null) {
                logger.log(Level.FINE, "Lock could not be reestablished: {0}", lockFile);
                throw new AlreadyLockedException(null);
            } else {
                logger.log(Level.FINEST, "Lock is valid: {0}", lockFile);
            }
        } catch (LockFailureException fe) {
            throw fe;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unexpected exception when asserting file lock", e);
            throw new LockFailureException(e);
        }
    }

    private void closeChannel(FileChannel c) {
        if (c != null) try {
            c.close();
        } catch (Exception e) {
            logger.log(Level.FINE, "Exception when closing channel", e);
        }
    }

    /** Release this concurrency lock.
     */
    public synchronized void releaseLock() {
        releaseLock(isLocked());
    }

    public synchronized void releaseLock(boolean deleteMetadata) {
        logger.log(Level.FINE, "Unlocking lock: {0}", lockFile);
        if (messageHandler != null) {
            try {
                messageHandler.terminate();
                messageHandler = null;
            } catch (Exception e) {
            }
        }
        if (lock != null) {
            try {
                lock.release();
                lock = null;
            } catch (Exception e) {
            }
        }
        if (lockChannel != null) {
            try {
                lockChannel.close();
                lockChannel = null;
            } catch (Exception e) {
            }
        }
        if (deleteMetadata) {
            try {
                lockFile.delete();
            } catch (Exception e) {
            }
        }
        this.extraInfo = null;
        if (shutdownHook != null) {
            try {
                if (Runtime.getRuntime().removeShutdownHook(shutdownHook)) shutdownHook = null;
            } catch (Exception e) {
            }
        }
    }

    private void tryToSendMessage(String message) throws LockFailureException {
        if (lockFile.exists() == false || lockFile.length() < 2) throw new CannotCreateLockException();
        String extraInfo = null;
        try {
            LockMetaData metaData = readLockMetaData(true);
            if (metaData == null) throw new AlreadyLockedException(null); else extraInfo = metaData.extraInfo;
            if (message == null || message.length() == 0) throw new AlreadyLockedException(extraInfo);
            if (!getCurrentHost().equals(metaData.hostName)) throw new AlreadyLockedException(extraInfo);
            Socket s = new Socket(LOOPBACK_ADDR, metaData.port);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), false);
            out.println(message);
            out.flush();
            s.setSoTimeout(4000);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            String liveToken = in.readLine();
            String response = in.readLine();
            out.close();
            in.close();
            s.close();
            if (metaData.token.equals(liveToken)) throw new SentLockMessageException(response); else throw new AlreadyLockedException(extraInfo);
        } catch (LockFailureException fe) {
            throw fe;
        } catch (Exception exc) {
            AlreadyLockedException e = new AlreadyLockedException(extraInfo);
            e.initCause(exc);
            throw e;
        }
    }

    private void writeLockMetaData(String hostName, int port, String token, String extraInfo) throws IOException {
        Writer out = Channels.newWriter(lockChannel, "UTF-8");
        out.write("\n");
        out.write(hostName + "\n");
        out.write(port + "\n");
        out.write(token + "\n");
        if (extraInfo != null) {
            extraInfo = extraInfo.replace('\r', ' ').replace('\n', ' ');
            out.write(extraInfo + "\n");
        }
        out.flush();
        lockChannel.force(true);
    }

    private static class LockMetaData {

        String hostName;

        int port;

        String token;

        String extraInfo;
    }

    private LockMetaData readLockMetaData(boolean close) throws IOException {
        LockMetaData result = new LockMetaData();
        BufferedReader infoIn = new BufferedReader(Channels.newReader(lockChannel.position(1), "UTF-8"));
        result.hostName = infoIn.readLine();
        if (result.hostName == null) return null;
        result.port = Integer.parseInt(infoIn.readLine());
        result.token = infoIn.readLine();
        result.extraInfo = infoIn.readLine();
        if (close) infoIn.close();
        return result;
    }

    /** Register a JVM shutdown hook to clean up the files created by this lock.
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread() {

            public void run() {
                shutdownHook = null;
                releaseLock();
            }
        });
    }

    /** Get the IP address of the current host.
     */
    private String getCurrentHost() {
        String currentHost = "127.0.0.1";
        try {
            currentHost = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ioe) {
        }
        return currentHost;
    }

    private static InetAddress LOOPBACK_ADDR;

    static {
        try {
            LOOPBACK_ADDR = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /** Listen for and handle messages from other would-be lock owners.
     */
    private class LockWatcher extends Thread {

        private ServerSocket serverSocket;

        private String token;

        private LockMessageHandler listener;

        private volatile boolean isRunning;

        public LockWatcher(LockMessageHandler listener, String extraInfo) throws IOException {
            super("Concurrency Lock Message Handler for " + lockFile);
            setDaemon(true);
            this.serverSocket = ServerSocketChannel.open().socket();
            this.serverSocket.bind(new InetSocketAddress(LOOPBACK_ADDR, 0));
            this.token = lockToken;
            this.listener = listener;
            writeLockMetaData(getCurrentHost(), serverSocket.getLocalPort(), token, extraInfo);
            this.isRunning = true;
            start();
        }

        public void run() {
            boolean nativeReassertNeeded = false;
            setCheckInterval(60);
            while (isRunning) {
                try {
                    Socket s = serverSocket.getChannel().socket().accept();
                    if (isRunning == true) handle(s);
                } catch (Exception e) {
                }
                if (listenForLostLock) {
                    try {
                        long start = System.currentTimeMillis();
                        synchronized (FileConcurrencyLock.this) {
                            if (isRunning == false) break;
                            assertLock(nativeReassertNeeded);
                        }
                        long end = System.currentTimeMillis();
                        logger.log(Level.FINEST, "assertValidity took {0} ms", new Long(end - start));
                        nativeReassertNeeded = false;
                        setCheckInterval(60);
                    } catch (LockUncertainException lue) {
                        nativeReassertNeeded = true;
                        setCheckInterval(20);
                    } catch (Exception e) {
                        logger.log(Level.FINER, "Exception when listening for lost lock", e);
                        try {
                            dispatchMessage(LockMessage.LOCK_LOST_MESSAGE);
                        } catch (Exception e1) {
                        }
                        setCheckInterval(0);
                    }
                }
            }
        }

        private void setCheckInterval(int seconds) {
            try {
                this.serverSocket.setSoTimeout(seconds * 1000);
            } catch (Exception e) {
            }
        }

        private void handle(Socket s) throws Exception {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
            String message = in.readLine();
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"));
            try {
                String response = dispatchMessage(message);
                out.println(token);
                out.println(response);
                out.flush();
            } catch (Exception e) {
            }
            out.close();
            in.close();
            s.close();
        }

        private String dispatchMessage(String message) throws Exception {
            LockMessage msg = new LockMessage(FileConcurrencyLock.this, message);
            String result = listener.handleMessage(msg);
            return result;
        }

        public void terminate() {
            isRunning = false;
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }
}

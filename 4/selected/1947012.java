package it.sauronsoftware.junique;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Point-of-entry of the JUnique library.
 * 
 * @author Carlo Pelliccia
 */
public class JUnique {

    /**
	 * The directory where lock files are stored.
	 */
    private static final File LOCK_FILES_DIR = new File(System.getProperty("user.home"), ".junique");

    /**
	 * A global lock file, to perform extra-JVM lock operations.
	 */
    private static final File GLOBAL_LOCK_FILE = new File(LOCK_FILES_DIR, "global.lock");

    /**
	 * The global file channel.
	 */
    private static FileChannel globalFileChannel = null;

    /**
	 * The global file lock.
	 */
    private static FileLock globalFileLock = null;

    /**
	 * Locks table. Normalized IDs are placed in the key side, while the value
	 * is a {@link Lock} object representing the lock details.
	 */
    private static Hashtable locks = new Hashtable();

    static {
        if (!LOCK_FILES_DIR.exists()) {
            LOCK_FILES_DIR.mkdirs();
        }
        Runtime rt = Runtime.getRuntime();
        rt.addShutdownHook(new Thread(new ShutdownHook()));
    }

    /**
	 * This method tries to acquire a lock in the user-space for a given ID.
	 * 
	 * @param id
	 *            The lock ID.
	 * @throws AlreadyLockedException
	 *             If the lock cannot be acquired, since it has been already
	 *             taken in the user-space.
	 */
    public static void acquireLock(String id) throws AlreadyLockedException {
        acquireLock(id, null);
    }

    /**
	 * This method tries to acquire a lock in the user-space for a given ID.
	 * 
	 * @param id
	 *            The lock ID.
	 * @param messageHandler
	 *            An optional message handler that will be used after the lock
	 *            has be acquired to handle incoming messages on the lock
	 *            channel.
	 * @throws AlreadyLockedException
	 *             If the lock cannot be acquired, since it has been already
	 *             taken in the user-space.
	 */
    public static void acquireLock(String id, MessageHandler messageHandler) throws AlreadyLockedException {
        File lockFile;
        File portFile;
        FileChannel fileChannel;
        FileLock fileLock;
        Server server;
        String nid = normalizeID(id);
        j_lock();
        try {
            lockFile = getLockFileForNID(nid);
            portFile = getPortFileForNID(nid);
            LOCK_FILES_DIR.mkdirs();
            try {
                RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
                fileChannel = raf.getChannel();
                fileLock = fileChannel.tryLock();
                if (fileLock == null) {
                    throw new AlreadyLockedException(id);
                }
            } catch (Throwable t) {
                throw new AlreadyLockedException(id);
            }
            server = new Server(id, messageHandler);
            Lock lock = new Lock(id, lockFile, portFile, fileChannel, fileLock, server);
            locks.put(nid, lock);
            server.start();
            Writer portWriter = null;
            try {
                portWriter = new FileWriter(portFile);
                portWriter.write(String.valueOf(server.getListenedPort()));
                portWriter.flush();
            } catch (Throwable t) {
                ;
            } finally {
                if (portWriter != null) {
                    try {
                        portWriter.close();
                    } catch (Throwable t) {
                        ;
                    }
                }
            }
        } finally {
            j_unlock();
        }
    }

    /**
	 * It releases a previously acquired lock on an ID. Please note that a lock
	 * can be realeased only by the same JVM that has previously acquired it. If
	 * the given ID doens't correspond to a lock that belongs to the current
	 * JVM, no action will be taken.
	 * 
	 * @param id
	 *            The lock ID.
	 */
    public static void releaseLock(String id) {
        String nid = normalizeID(id);
        j_lock();
        try {
            Lock lock = (Lock) locks.remove(nid);
            if (lock != null) {
                releaseLock(lock);
            }
        } finally {
            j_unlock();
        }
    }

    /**
	 * Internal lock release routine.
	 * 
	 * @param lock
	 *            The lock to release.
	 */
    private static void releaseLock(Lock lock) {
        lock.getServer().stop();
        try {
            lock.getLockFileLock().release();
        } catch (Throwable t) {
            ;
        }
        try {
            lock.getLockFileChannel().close();
        } catch (Throwable t) {
            ;
        }
        lock.getPortFile().delete();
        lock.getLockFile().delete();
    }

    /**
	 * It sends a message to the JVM process that has previously locked the
	 * given ID. The message will be delivered only if the lock for the given ID
	 * has been actually acquired, and only if who has acquired it is interested
	 * in message handling.
	 * 
	 * @param id
	 *            The lock ID.
	 * @param message
	 *            The message.
	 * @return A response for the message. It returns null if the message cannot
	 *         be delivered. It returns an empty string if the message has been
	 *         delivered but the recipient hasn't supplied a response for it.
	 */
    public static String sendMessage(String id, String message) {
        int port = -1;
        j_lock();
        try {
            String nid = normalizeID(id);
            File portFile = getPortFileForNID(nid);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(portFile));
                String line = reader.readLine();
                if (line != null) {
                    port = Integer.parseInt(line);
                }
            } catch (Throwable t) {
                ;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable t) {
                        ;
                    }
                }
            }
        } finally {
            j_unlock();
        }
        String response = null;
        if (port > 0) {
            Socket socket = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                socket = new Socket("localhost", port);
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                Message.write(message, outputStream);
                response = Message.read(inputStream);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable t) {
                        ;
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable t) {
                        ;
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable t) {
                        ;
                    }
                }
            }
        }
        return response;
    }

    /**
	 * It returns a "normalized" version of an ID.
	 * 
	 * @param id
	 *            The source ID.
	 * @return The normalized ID.
	 */
    private static String normalizeID(String id) {
        int hashcode = id.hashCode();
        boolean positive = hashcode >= 0;
        long longcode = positive ? (long) hashcode : -(long) hashcode;
        StringBuffer hexstring = new StringBuffer(Long.toHexString(longcode));
        while (hexstring.length() < 8) {
            hexstring.insert(0, '0');
        }
        if (positive) {
            hexstring.insert(0, '0');
        } else {
            hexstring.insert(0, '1');
        }
        return hexstring.toString();
    }

    /**
	 * It returns the lock file associated to a normalized ID.
	 * 
	 * @param nid
	 *            The normalized ID.
	 * @return The lock file for this normalized ID.
	 */
    private static File getLockFileForNID(String nid) {
        String filename = normalizeID(nid) + ".lock";
        return new File(LOCK_FILES_DIR, filename);
    }

    /**
	 * It returns the port file associated to a normalized ID.
	 * 
	 * @param nid
	 *            The corresponding normalized ID.
	 * @return The port file for this normalized ID.
	 */
    private static File getPortFileForNID(String nid) {
        String filename = normalizeID(nid) + ".port";
        return new File(LOCK_FILES_DIR, filename);
    }

    /**
	 * This one performs a cross-JVM lock on all JUnique instances. Calling this
	 * lock causes the acquisition of an exclusive extra-JVM access to JUnique
	 * file system resources.
	 */
    private static void j_lock() {
        do {
            LOCK_FILES_DIR.mkdirs();
            try {
                RandomAccessFile raf = new RandomAccessFile(GLOBAL_LOCK_FILE, "rw");
                FileChannel channel = raf.getChannel();
                FileLock lock = channel.lock();
                globalFileChannel = channel;
                globalFileLock = lock;
                break;
            } catch (Throwable t) {
                ;
            }
        } while (true);
    }

    /**
	 * Release a previously acquired extra-JVM JUnique lock.
	 */
    private static void j_unlock() {
        FileChannel channel = globalFileChannel;
        FileLock lock = globalFileLock;
        globalFileChannel = null;
        globalFileLock = null;
        try {
            lock.release();
        } catch (Throwable t) {
            ;
        }
        try {
            channel.close();
        } catch (Throwable t) {
            ;
        }
    }

    /**
	 * Some shutdown hook code, releasing any unreleased lock on JVM regular
	 * shutdown.
	 */
    private static class ShutdownHook implements Runnable {

        public void run() {
            j_lock();
            try {
                ArrayList nids = new ArrayList();
                for (Enumeration e = locks.keys(); e.hasMoreElements(); ) {
                    String nid = (String) e.nextElement();
                    nids.add(nid);
                }
                for (Iterator i = nids.iterator(); i.hasNext(); ) {
                    String nid = (String) i.next();
                    Lock lock = (Lock) locks.remove(nid);
                    releaseLock(lock);
                }
            } finally {
                j_unlock();
            }
        }
    }
}

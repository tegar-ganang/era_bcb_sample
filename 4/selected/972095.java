package net.ontopia.infoset.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import net.ontopia.utils.StreamUtils;

/**
 * INTERNAL: A content store implementation based on the file system.
 * It uses a two-level structure where inside the root directory is a
 * set of directories, each of which contains a fixed number of files
 * (N). A key is mapped to a file name (key modulo N) and a directory
 * name (key divided by N). The next free key is stored in a file in
 * the top directory, and keys are allocated in blocks.
 */
public class FileContentStore implements ContentStoreIF {

    public static final int FILES_PER_DIRECTORY = 1000;

    public static final int KEY_BLOCK_SIZE = 10;

    private int files_per_directory;

    private boolean open;

    private File store_root;

    private int last_key;

    private int end_of_key_block;

    private File key_file;

    public FileContentStore(File store_root) throws ContentStoreException {
        if (!store_root.canWrite()) throw new ContentStoreException("Content store root directory '" + store_root.getAbsoluteFile() + "' not writable.");
        this.store_root = store_root;
        this.files_per_directory = FILES_PER_DIRECTORY;
        this.open = true;
        this.key_file = new File(store_root, "keyfile.txt");
        allocateNewBlock();
    }

    public synchronized boolean containsKey(int key) throws ContentStoreException {
        checkOpen();
        return getFileForKey(key).exists();
    }

    public synchronized ContentInputStream get(int key) throws ContentStoreException {
        checkOpen();
        File file = getFileForKey(key);
        try {
            return new ContentInputStream(new FileInputStream(file), (int) file.length());
        } catch (FileNotFoundException e) {
            throw new ContentStoreException("No entry in content store for key " + key + "; file " + file.getAbsoluteFile() + " not " + "found.");
        }
    }

    public int add(ContentInputStream data) throws ContentStoreException {
        return add(data, data.getLength());
    }

    public synchronized int add(InputStream data, int length) throws ContentStoreException {
        checkOpen();
        int key = getNewKey();
        File file = getFileForKey(key);
        if (file.exists()) throw new ContentStoreException("Content store corrupted: file already " + "exists for key " + key + ".");
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdir();
            OutputStream out = new FileOutputStream(file);
            StreamUtils.transfer(data, out);
            out.close();
        } catch (IOException e) {
            throw new ContentStoreException("Error writing data to content store.", e);
        }
        if (file.length() != length) throw new ContentStoreException("Stored entry for key " + key + " of wrong " + "size. Given length was " + length + ", but " + "resulting entry was " + file.length());
        return key;
    }

    public synchronized boolean remove(int key) throws ContentStoreException {
        checkOpen();
        File file = getFileForKey(key);
        return file.delete();
    }

    public synchronized void close() throws ContentStoreException {
        checkOpen();
        open = false;
    }

    private void checkOpen() throws ContentStoreException {
        if (!open) throw new ContentStoreException("Content store on " + store_root + "not open");
    }

    private File getFileForKey(int key) {
        int dirpart = key / FILES_PER_DIRECTORY;
        int filepart = key % FILES_PER_DIRECTORY;
        return new File(store_root, "" + dirpart + File.separator + filepart);
    }

    private int getNewKey() throws ContentStoreException {
        if (last_key == end_of_key_block) allocateNewBlock();
        last_key++;
        return last_key;
    }

    static final int MAX_SPINS = 1000;

    static final int SPIN_TIMEOUT = 10;

    private void allocateNewBlock() throws ContentStoreException {
        RandomAccessFile out = null;
        boolean exception_thrown = false;
        try {
            out = new RandomAccessFile(key_file, "rws");
            for (int i = 0; i < MAX_SPINS; i++) {
                FileLock l = out.getChannel().tryLock();
                if (l == null) {
                    try {
                        Thread.sleep(SPIN_TIMEOUT);
                    } catch (InterruptedException e) {
                    }
                    continue;
                } else {
                    try {
                        int old_key;
                        int new_key;
                        String content = null;
                        if (out.length() == 0) {
                            old_key = 0;
                            new_key = old_key + KEY_BLOCK_SIZE;
                        } else {
                            try {
                                content = out.readUTF();
                                old_key = Integer.parseInt(content);
                                new_key = old_key + KEY_BLOCK_SIZE;
                            } catch (NumberFormatException e) {
                                if (content.length() > 100) content = content.substring(0, 100) + "...";
                                throw new ContentStoreException("Content store key file corrupted. Contained: '" + content + "'");
                            }
                        }
                        out.seek(0);
                        out.writeUTF(Integer.toString(new_key));
                        end_of_key_block = new_key;
                        last_key = old_key;
                        return;
                    } finally {
                        try {
                            l.release();
                        } catch (Throwable t) {
                            throw new ContentStoreException("Could not release key file lock.", t);
                        }
                    }
                }
            }
            throw new ContentStoreException("Block allocation timed out.");
        } catch (ContentStoreException e) {
            exception_thrown = true;
            throw e;
        } catch (Throwable t) {
            exception_thrown = true;
            throw new ContentStoreException(t);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    if (!exception_thrown) throw new ContentStoreException("Problems occurred when closing content store.", e);
                }
            }
        }
    }
}

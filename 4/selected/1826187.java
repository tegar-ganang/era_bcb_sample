package org.garret.ptl.images.storage;

import org.apache.log4j.Logger;
import org.garret.ptl.util.StringUtil;
import org.garret.ptl.util.SystemException;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A file storage that maps to a single folder with files
 *
 * @author Andrey Subbotin
 */
public class FileStorageFolder implements IFileStorage {

    private static final RemoveFileQueue removeQueue = new RemoveFileQueue();

    static {
        removeQueue.start();
    }

    private final File folder;

    public FileStorageFolder(File folder) {
        folder.mkdirs();
        this.folder = folder;
    }

    public FileStorageFolder(File folder, String subfolder) {
        if (StringUtil.isEmpty(subfolder)) this.folder = folder; else this.folder = new File(folder, subfolder);
        this.folder.mkdirs();
    }

    public boolean exists(String name) {
        assert name != null;
        return new File(folder, name).exists();
    }

    public long timestamp(String name) {
        assert name != null;
        return new File(folder, name).lastModified();
    }

    public byte[] get(String name) {
        assert name != null;
        File file = new File(folder, name);
        FileInputStream inputFile = null;
        try {
            inputFile = new FileInputStream(file);
            int bytesAvailable = (int) file.length();
            byte[] buffer = new byte[bytesAvailable];
            readInputStream(buffer, inputFile);
            return buffer;
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            if (inputFile != null) try {
                inputFile.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] get(File file) {
        FileInputStream inputFile = null;
        try {
            inputFile = new FileInputStream(file);
            int bytesAvailable = (int) file.length();
            byte[] buffer = new byte[bytesAvailable];
            readInputStream(buffer, inputFile);
            return buffer;
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            if (inputFile != null) try {
                inputFile.close();
            } catch (IOException e) {
            }
        }
    }

    public File getFile(String path) {
        return new File(folder, path);
    }

    public InputStream getInputStream(String path) {
        try {
            return new FileInputStream(new File(folder, path));
        } catch (FileNotFoundException e) {
            throw new SystemException(e);
        }
    }

    public void read(String name, OutputStream out) throws SystemException {
        File file = new File(folder, name);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] buf = new byte[2048];
            int t;
            while ((t = in.read(buf)) > 0) out.write(buf, 0, t);
            out.flush();
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    public void copy(File from, String to) throws SystemException {
        assert from != null;
        File dst = new File(folder, to);
        dst.getParentFile().mkdirs();
        FileChannel in = null;
        FileChannel out = null;
        try {
            if (!dst.exists()) dst.createNewFile();
            in = new FileInputStream(from).getChannel();
            out = new FileOutputStream(dst).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception e1) {
            }
            try {
                if (out != null) out.close();
            } catch (Exception e1) {
            }
        }
    }

    public void create(String name, InputStream in) throws SystemException {
        assert !StringUtil.isEmpty(name);
        FileOutputStream out = null;
        File file = new File(folder, name);
        file.getParentFile().mkdirs();
        try {
            if (!file.exists()) file.createNewFile();
            out = new FileOutputStream(file);
            byte[] buf = new byte[2048];
            int t = 0;
            while ((t = in.read(buf)) > 0) out.write(buf, 0, t);
            out.flush();
        } catch (IOException e) {
            throw new SystemException(e);
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception e1) {
            }
        }
    }

    public void save(String name, byte[] data) {
        assert name != null;
        File file = new File(folder, name);
        file.getParentFile().mkdirs();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
            }
        }
    }

    public void move(String name, File source) {
        boolean r = source.renameTo(new File(folder, name));
        if (!r) throw new RuntimeException("file not moved");
    }

    public static void save(File file, byte[] data) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
            }
        }
    }

    public Collection<String> enumerate() {
        List<String> result = new ArrayList<String>();
        for (File file : folder.listFiles()) if (!file.isDirectory()) result.add(file.getName());
        return result;
    }

    public void delete(String name) {
        File file = new File(folder, name);
        file.delete();
    }

    public File path() {
        return folder;
    }

    public void markForDeletion(String name) {
        File file = new File(folder, name);
        removeQueue.add(file);
    }

    private static void readInputStream(byte[] buffer, InputStream input) throws IOException {
        int readed = 0;
        while (readed < buffer.length) {
            readed += input.read(buffer, readed, buffer.length - readed);
        }
    }

    private static class RemoveFileQueue {

        private static final Logger LOGGER = Logger.getLogger(RemoveFileQueue.class);

        private static final int MIN_FILES_TO_REMOVE_QUEUE_LENGTH = 3;

        private final ArrayList<File> filesToRemoveQueue;

        private final Thread process;

        public RemoveFileQueue() {
            filesToRemoveQueue = new ArrayList<File>();
            process = new Thread("removingFilesThread") {

                public void run() {
                    synchronized (this) {
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                    processRemovingFiles();
                }
            };
        }

        public void start() {
            process.start();
        }

        public void add(File f) {
            synchronized (filesToRemoveQueue) {
                filesToRemoveQueue.add(f);
                if (filesToRemoveQueue.size() > MIN_FILES_TO_REMOVE_QUEUE_LENGTH) filesToRemoveQueue.notify();
            }
        }

        private void delete(File f) {
            if (f.exists()) {
                if (f.isDirectory()) {
                    for (File x : f.listFiles()) delete(x);
                }
                f.delete();
            }
        }

        public void processRemovingFiles() {
            while (true) {
                try {
                    ArrayList<File> files = null;
                    synchronized (filesToRemoveQueue) {
                        files = (ArrayList<File>) filesToRemoveQueue.clone();
                        filesToRemoveQueue.clear();
                    }
                    if (files != null) {
                        for (File f : files) delete(f);
                    }
                    synchronized (filesToRemoveQueue) {
                        filesToRemoveQueue.wait();
                    }
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        }
    }
}

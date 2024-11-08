package org.bitdrive.file.core.impl;

import org.bitdrive.core.logging.api.Level;
import org.bitdrive.core.logging.api.Logger;
import org.bitdrive.file.core.api.FileFactory;
import org.bitdrive.file.core.api.ReadableFile;
import org.bitdrive.file.core.api.ReadableInputStream;
import org.bitdrive.network.core.api.Connection;
import org.bitdrive.network.file.api.RemoteFileManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class FileFactoryImpl implements FileFactory {

    private final class ByteArrayWrapper {

        private final byte[] data;

        public ByteArrayWrapper(byte[] data) {
            if (data == null) {
                throw new NullPointerException();
            }
            this.data = data;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ByteArrayWrapper)) {
                return false;
            }
            return Arrays.equals(data, ((ByteArrayWrapper) other).data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }

    private class RemoteConnectionItem {

        public LinkedList<Connection> connections;

        public long size;

        public RemoteConnectionItem(long size) {
            this.size = size;
            this.connections = new LinkedList<Connection>();
        }
    }

    private Logger logger;

    private RemoteFileManager remoteFileManager = null;

    private HashMap<ByteArrayWrapper, LinkedList<File>> localFiles;

    private HashMap<ByteArrayWrapper, RemoteConnectionItem> remoteFiles;

    public FileFactoryImpl(Logger logger) {
        this.logger = logger;
        this.localFiles = new HashMap<ByteArrayWrapper, LinkedList<File>>();
        this.remoteFiles = new HashMap<ByteArrayWrapper, RemoteConnectionItem>();
    }

    public void setRemoteFileManager(RemoteFileManager remoteFileManager) {
        this.remoteFileManager = remoteFileManager;
    }

    public File getLocalFile(byte[] hash, File toFind) {
        LinkedList<File> files = localFiles.get(new ByteArrayWrapper(hash));
        if (files == null) return null;
        if (toFind == null) {
            int count = files.size();
            if (count > 1) {
                return files.get(((int) Math.random() * 100) % count);
            } else {
                return files.get(0);
            }
        } else {
            for (File file : files) {
                if (file.equals(toFind)) return file;
            }
            return null;
        }
    }

    public Connection getRemoteFile(byte[] hash, Connection toFind) {
        RemoteConnectionItem item = remoteFiles.get(new ByteArrayWrapper(hash));
        if (item == null) return null;
        if (toFind == null) {
            int count = item.connections.size();
            if (count > 1) {
                return item.connections.get(((int) Math.random() * 100) % count);
            } else {
                return item.connections.get(0);
            }
        } else {
            for (Connection con : item.connections) {
                if (con.equals(item.connections)) return con;
            }
            return null;
        }
    }

    public ReadableFile getFile(byte[] hash) {
        File file;
        RemoteConnectionItem item;
        if ((file = getLocalFile(hash, null)) != null) {
            try {
                return new LocalFile(hash, file);
            } catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, "FileFactory.getFile Failed to open localfile, filename: " + file.getName());
            }
        }
        if (remoteFileManager == null) return null;
        if ((item = remoteFiles.get(new ByteArrayWrapper(hash))) != null) {
            return remoteFileManager.createFile(hash, item.size, item.connections);
        }
        return null;
    }

    public void addFile(byte[] hash, File localFile) {
        LinkedList<File> files = localFiles.get(new ByteArrayWrapper(hash));
        if (files == null) {
            files = new LinkedList<File>();
            localFiles.put(new ByteArrayWrapper(hash), files);
        }
        for (File f : files) {
            if (f.getAbsolutePath().equals(localFile.getAbsolutePath())) return;
        }
        files.add(localFile);
    }

    public void addFile(byte[] hash, long size, Connection connection) {
        RemoteConnectionItem item = remoteFiles.get(new ByteArrayWrapper(hash));
        if (item == null) {
            item = new RemoteConnectionItem(size);
            remoteFiles.put(new ByteArrayWrapper(hash), item);
        }
        item.connections.add(connection);
    }

    public void removeFile(byte[] hash, File localFile) {
        LinkedList<File> files = localFiles.get(new ByteArrayWrapper(hash));
        if (files == null) return;
        files.remove(localFile);
        if (files.size() == 0) {
            localFiles.remove(files);
        }
    }

    public void removeFile(byte[] hash, Connection connection) {
        RemoteConnectionItem item = remoteFiles.get(new ByteArrayWrapper(hash));
        if (item == null) return;
        item.connections.remove(connection);
        if (item.connections.size() == 0) {
            remoteFiles.remove(item);
            if (!localFiles.containsKey(new ByteArrayWrapper(hash))) {
                remoteFileManager.closeFile(hash);
            }
        }
    }

    public void copyFile(byte[] hash, File destinationFile) throws IOException {
        int read;
        byte[] buffer = new byte[512 * 1024];
        FileOutputStream outputStream;
        ReadableFile file = getFile(hash);
        if (file == null) throw new FileNotFoundException("File not found");
        outputStream = new FileOutputStream(destinationFile);
        while ((read = file.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.close();
        file.close();
    }

    public ReadableFile getRemoteFile(byte[] hash) {
        RemoteConnectionItem item;
        if (remoteFileManager == null) return null;
        item = remoteFiles.get(new ByteArrayWrapper(hash));
        if (item != null) {
            return remoteFileManager.createFile(hash, item.size, item.connections);
        }
        return null;
    }
}

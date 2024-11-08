package com.ev.evgetme.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import com.ev.evgetme.FileSysExt;

/**
 * @author Ernestas Vaiciukevicius 
 *
 */
public class JSR75FileSys implements FileSysExt {

    protected FileConnection getFileConnection(String name, int mode) throws IOException {
        return (FileConnection) Connector.open("file://" + name, mode, true);
    }

    public void deleteFile(String name) throws IOException {
        FileConnection conn = getFileConnection(name, Connector.WRITE);
        try {
            conn.delete();
        } finally {
            conn.close();
        }
    }

    public Enumeration list(String name) throws IOException {
        if (name == null || "/".equals(name) || "\\".equals(name)) {
            return FileSystemRegistry.listRoots();
        } else {
            FileConnection conn = getFileConnection(name, Connector.READ);
            try {
                return conn.list();
            } finally {
                conn.close();
            }
        }
    }

    public int length(String name) throws IOException {
        FileConnection connection = getFileConnection(name, Connector.READ);
        try {
            return (int) connection.fileSize();
        } finally {
            connection.close();
        }
    }

    public void rename(String orig, String targ) throws IOException {
        int dirInd = orig.lastIndexOf('/');
        boolean sameDir = false;
        if (dirInd >= 0) {
            sameDir = targ.length() >= dirInd && orig.substring(0, dirInd).equals(targ.substring(0, dirInd));
        }
        if (sameDir) {
            FileConnection conn = getFileConnection(orig, Connector.READ_WRITE);
            try {
                conn.rename(targ.substring(dirInd + 1));
            } finally {
                conn.close();
            }
        } else {
            copy(orig, targ);
            deleteFile(orig);
        }
    }

    public boolean exists(String name) throws IOException {
        FileConnection conn = getFileConnection(name, Connector.READ);
        try {
            return conn.exists();
        } finally {
            conn.close();
        }
    }

    public boolean isDir(String name) throws IOException {
        FileConnection conn = getFileConnection(name, Connector.READ);
        try {
            return conn.isDirectory();
        } finally {
            conn.close();
        }
    }

    public void copy(String from, String to) throws IOException {
        FileConnection origConn = getFileConnection(from, Connector.READ);
        try {
            FileConnection destConn = getFileConnection(to, Connector.WRITE);
            try {
                if (destConn.exists()) {
                    throw new IOException("File exists");
                }
                InputStream is = origConn.openInputStream();
                try {
                    OutputStream os = destConn.openOutputStream();
                    byte[] buf = new byte[512];
                    while (is.available() > 0) {
                        int read = is.read(buf, 0, Math.min(is.available(), buf.length));
                        os.write(buf, 0, read);
                    }
                } finally {
                    is.close();
                }
            } finally {
                destConn.close();
            }
        } finally {
            origConn.close();
        }
    }

    public void truncate(String name, int size) throws IOException {
        FileConnection conn = getFileConnection(name, Connector.WRITE);
        try {
            conn.truncate(size);
        } finally {
            conn.close();
        }
    }

    public void create(String name) throws IOException {
        FileConnection conn = getFileConnection(name, Connector.WRITE);
        try {
            conn.create();
        } finally {
            conn.close();
        }
    }

    public void debugWrite(String name, String msg) {
        try {
            if (!exists(name)) {
                create(name);
            }
            FileConnection conn = getFileConnection(name, Connector.WRITE);
            try {
                OutputStream os = conn.openOutputStream();
                try {
                    os.write((msg + "\n").getBytes());
                } finally {
                    os.close();
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "JSR75FileSys";
    }
}

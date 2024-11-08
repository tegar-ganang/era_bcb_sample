package eu.cherrytree.paj.file;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class FileReader {

    DataInputStream in;

    InputStream stream;

    public FileReader(String path) throws ZipException, IOException {
        path = fixPath(path, File.separatorChar);
        String topath = "";
        int index = 0;
        boolean zip = false;
        path.trim();
        if (path.startsWith(File.separator)) index++;
        while (index < path.length()) {
            index = path.indexOf(File.separatorChar, index) + 1;
            if (index == 0) topath = path; else topath = path.substring(0, index - 1);
            if (new File(topath + ".pak").exists()) {
                zip = true;
                break;
            } else if (!(new File(topath).exists())) throw new FileNotFoundException("Couldn't find: " + path); else if (topath.equals(path)) break;
        }
        if (zip) {
            ZipFile zipfile = new ZipFile(new File(topath + ".pak"));
            ZipEntry entry = zipfile.getEntry(fixPath(path.substring(index), '/'));
            if (entry == null) throw new FileNotFoundException("Couldn't find \"" + path.substring(index) + "\" in " + topath + ".pak");
            stream = zipfile.getInputStream(entry);
        } else stream = new FileInputStream(path);
        in = new DataInputStream(new BufferedInputStream(stream));
    }

    public static void extractFile(String input, String output) throws ZipException, IOException {
        FileReader reader = new FileReader(input);
        InputStream in = reader.getInputStream();
        OutputStream out = new FileOutputStream(new File(output));
        byte[] buf = new byte[512];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        reader.close();
        out.close();
    }

    public static String[] getFileList(String path) throws ZipException, IOException {
        path = fixPath(path, File.separatorChar);
        String topath = "";
        int index = 0;
        boolean zip = false;
        path.trim();
        if (path.startsWith(File.separator)) index++;
        while (index < path.length()) {
            index = path.indexOf(File.separatorChar, index) + 1;
            if (index == 0) topath = path; else topath = path.substring(0, index - 1);
            if (new File(topath + ".pak").exists()) {
                zip = true;
                break;
            } else if (!(new File(topath).exists())) throw new FileNotFoundException("Couldn't find: " + path); else if (topath.equals(path)) break;
        }
        Vector<String> files = new Vector<String>();
        if (zip) {
            ZipFile zipfile = new ZipFile(new File(topath + ".pak"));
            String p = fixPath(path.substring(index), '/') + "/";
            @SuppressWarnings("rawtypes") Enumeration zipEntries = zipfile.entries();
            while (zipEntries.hasMoreElements()) {
                String str = ((ZipEntry) zipEntries.nextElement()).getName();
                if (!str.contains(".manifest") && str.startsWith(p) && !str.equals(p)) files.add(str.substring(str.indexOf(p) + p.length()));
            }
        } else {
            File dir = new File(path);
            String[] list = dir.list();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    if (!list[i].equals(".manifest")) ;
                    files.add(list[i]);
                }
            }
        }
        String[] ret = new String[files.size()];
        for (int i = 0; i < ret.length; i++) ret[i] = files.get(i);
        return ret;
    }

    private static String fixPath(String path, char sep) {
        return path.replace('/', sep).replace('\\', sep);
    }

    public byte readByte() throws IOException {
        return in.readByte();
    }

    public void readBytes(byte[] bytes) throws IOException {
        in.read(bytes);
    }

    public short readShort() throws IOException {
        byte[] r = new byte[2];
        in.read(r);
        return EndianConverter.toShort(r);
    }

    public int readInt() throws IOException {
        byte[] r = new byte[4];
        in.read(r);
        return EndianConverter.toInt(r);
    }

    public long readLong() throws IOException {
        byte[] r = new byte[8];
        in.read(r);
        return EndianConverter.toLong(r);
    }

    public float readFloat() throws IOException {
        byte[] r = new byte[4];
        in.read(r);
        return EndianConverter.toFloat(r);
    }

    public double readDouble() throws IOException {
        byte[] r = new byte[8];
        in.read(r);
        return EndianConverter.toDouble(r);
    }

    public void close() throws IOException {
        in.close();
    }

    public InputStream getInputStream() {
        return stream;
    }
}

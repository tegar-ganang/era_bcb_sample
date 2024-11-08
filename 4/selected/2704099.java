package it.allerj.common.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileUtil {

    public static File setTextToFile(File file, String text) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(file.length());
            raf.writeChars(text);
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(text);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static String getTextFromFile(File file) {
        StringBuffer sb = new StringBuffer("");
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            in.close();
        } catch (IOException e) {
            System.out.println("[" + FileUtil.class.getName() + "] getTextFromFile() : " + e.getMessage());
        }
        return sb.toString();
    }

    public static File getFileFromByteArray(File file, byte[] bytes, boolean append) {
        ByteBuffer bbuf = getByteBufferFromByteArray(bytes);
        try {
            FileChannel wChannel = new FileOutputStream(file, append).getChannel();
            wChannel.write(bbuf);
            wChannel.close();
        } catch (IOException e) {
        }
        return file;
    }

    public static byte[] getByteArrayFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    public static ByteBuffer getByteBufferFromByteArray(byte[] bytes) {
        ByteBuffer bbuf = ByteBuffer.wrap(bytes);
        return bbuf;
    }

    public static byte[] getByteArrayFromByteBuffer(int start, int end, ByteBuffer bbuf) {
        bbuf.clear();
        byte[] bytes = new byte[bbuf.capacity()];
        bbuf.get(bytes, start, end);
        return bytes;
    }

    public static void visitAllDirsAndFiles(File dir, ProcessingRoutine processingRoutine) {
        processingRoutine.process(dir);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllDirsAndFiles(new File(dir, children[i]), processingRoutine);
            }
        }
    }

    public static void visitAllDirs(File dir, ProcessingRoutine processingRoutine) {
        if (dir.isDirectory()) {
            processingRoutine.process(dir);
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllDirs(new File(dir, children[i]), processingRoutine);
            }
        }
    }

    public static void visitAllFiles(File dir, ProcessingRoutine processingRoutine) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllFiles(new File(dir, children[i]), processingRoutine);
            }
        } else {
            processingRoutine.process(dir);
        }
    }

    public static File getDirByName(String urlDir) {
        return new File(urlDir);
    }

    public interface ProcessingRoutine {

        public void process(File dir);

        public void processString(String line);

        public Object getReults();
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static void readTextFileAndProcess(File file, ProcessingRoutine processingRoutine) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                processingRoutine.processString(str);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Data una url relativa al package (es.: it/micra/properties/fileditesto.txt) mi restituisce un line number reader per leggere un file riga per riga
     * @param url
     * @return
     */
    public static LineNumberReader getLineNumberReader(String url) {
        return new LineNumberReader(new InputStreamReader(FileUtil.class.getClassLoader().getResourceAsStream(url)));
    }

    /**
     * dato un file mi restituisce un line number reader per leggere un file riga per riga
     * 
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public static LineNumberReader getLineNumberReader(File file) throws FileNotFoundException {
        return new LineNumberReader(new FileReader(file));
    }
}

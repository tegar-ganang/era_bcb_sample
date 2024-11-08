package javacream.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * FileUtil
 * 
 * @author Glenn Powell
 * 
 */
public class FileUtil {

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx >= 0) return filename.substring(idx + 1);
        return null;
    }

    public static File replaceExtension(File file, String ext) {
        String abs = file.getAbsolutePath();
        int idx = abs.lastIndexOf('.');
        if (idx < 0) return new File(abs + "." + ext);
        return new File(abs.substring(0, idx + 1) + ext);
    }

    public static String getRelativePath(File dir, File file) throws IOException {
        ArrayList<String> dirComponents = new ArrayList<String>();
        File fileAbsolute = dir.getCanonicalFile();
        while (fileAbsolute != null) {
            dirComponents.add(fileAbsolute.getName());
            fileAbsolute = fileAbsolute.getParentFile();
        }
        ArrayList<String> fileComponents = new ArrayList<String>();
        fileAbsolute = file.getCanonicalFile();
        while (fileAbsolute != null) {
            fileComponents.add(fileAbsolute.getName());
            fileAbsolute = fileAbsolute.getParentFile();
        }
        int i = dirComponents.size() - 1;
        int j = fileComponents.size() - 1;
        while ((i >= 0) && (j >= 0) && (dirComponents.get(i).equals(fileComponents.get(j)))) {
            --i;
            --j;
        }
        StringBuffer buffer = new StringBuffer();
        for (; i >= 0; --i) {
            buffer.append(".." + File.separator);
        }
        for (; j >= 1; j--) {
            buffer.append(fileComponents.get(j) + File.separator);
        }
        buffer.append(fileComponents.get(j));
        return buffer.toString();
    }

    public static void copyFile(File in, File out) throws IOException {
        copyFile(in, out, true);
    }

    public static void copyFile(File in, File out, boolean copyModified) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }
            if (copyModified) out.setLastModified(in.lastModified());
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public static boolean recursiveDelete(File dir) throws SecurityException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory()) {
                    if (!recursiveDelete(files[i])) return false;
                } else {
                    if (!files[i].delete()) return false;
                }
            }
            return dir.delete();
        }
        return false;
    }

    public static File[] recursiveListFiles(File dir) throws SecurityException {
        return recursiveListFiles(dir, null, null);
    }

    public static File[] recursiveListFiles(File dir, FilenameFilter filter) throws SecurityException {
        return recursiveListFiles(dir, filter, null);
    }

    public static File[] recursiveListFiles(File dir, FileFilter filter) throws SecurityException {
        return recursiveListFiles(dir, null, filter);
    }

    private static File[] recursiveListFiles(File dir, FilenameFilter filenameFilter, FileFilter fileFilter) throws SecurityException {
        ArrayList<File> fileV = new ArrayList<File>();
        recursiveListFiles(fileV, dir, filenameFilter, fileFilter);
        return fileV.toArray(new File[fileV.size()]);
    }

    private static void recursiveListFiles(ArrayList<File> fileV, File dir, FilenameFilter filenameFilter, FileFilter fileFilter) throws SecurityException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                recursiveListFiles(fileV, files[i], filenameFilter, fileFilter);
                if (filenameFilter != null && !filenameFilter.accept(dir, files[i].getName())) continue;
                if (fileFilter != null && !fileFilter.accept(files[i])) continue;
                fileV.add(files[i]);
            }
        }
    }
}

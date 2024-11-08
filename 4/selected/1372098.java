package be.vds.basics.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtil {

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public static boolean deleteFolder(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteFolder(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static File createFile(String file) throws IOException {
        String[] paths = getPaths(file);
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            File f = new File(path);
            if (!f.exists()) {
                if (i == paths.length - 1) {
                    f.createNewFile();
                } else {
                    f.mkdir();
                }
            }
        }
        return new File(file);
    }

    public static void main(String[] args) {
        String[] arrays = getPaths("C:/test/test2/hr.txt");
        System.out.println(Arrays.toString(arrays));
        arrays = getPaths("C:\\test\\test2\\hr.txt", "\\");
        System.out.println(Arrays.toString(arrays));
        try {
            createFile("/home/gautier/test2/hr.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String[] getPaths(String file) {
        return getPaths(file, String.valueOf(File.separatorChar));
    }

    private static String[] getPaths(String file, String fileSeparator) {
        if (!file.endsWith(fileSeparator)) file = file + fileSeparator;
        List<String> pathsList = new ArrayList<String>();
        int previousIndex = 0;
        int currentIndex = file.indexOf(fileSeparator);
        while (currentIndex != -1) {
            pathsList.add(file.substring(0, currentIndex));
            previousIndex = currentIndex + 1;
            currentIndex = file.indexOf(fileSeparator, previousIndex);
        }
        int i = 0;
        String[] a = new String[pathsList.size()];
        for (String string : pathsList) {
            a[i] = string;
            i++;
        }
        return a;
    }
}

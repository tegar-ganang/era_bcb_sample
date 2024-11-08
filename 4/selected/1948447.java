package ops.netbeansmodules.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author angr
 */
public class FileHelper {

    public static String getTextFileText(String string) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(new File(string));
        byte[] b = new byte[fis.available()];
        fis.read(b);
        return new String(b);
    }

    public static String unixSlashed(String runDirectory) {
        try {
            String ret = runDirectory.replace("\\", "/");
            return ret;
        } catch (StringIndexOutOfBoundsException exception) {
            return runDirectory;
        }
    }

    /** Creates a new instance of FileHelper */
    public FileHelper() {
    }

    public static String getRelativePath(String currentDirectory, String childPath) {
        return childPath.substring(currentDirectory.length() - 1);
    }

    public static String cropExtension(String fileName) {
        try {
            String ret = fileName.substring(0, fileName.lastIndexOf("."));
            if (ret != null) {
                return ret;
            } else {
                return fileName;
            }
        } catch (StringIndexOutOfBoundsException stringIndexOutOfBoundsException) {
            return fileName;
        }
    }

    public static void createAndWriteFile(String outFilePath, String outFileText) throws IOException {
        File outFile = new File(outFilePath);
        outFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(outFile);
        fos.write(outFileText.getBytes());
        fos.close();
    }

    /**
     * get relative path of File 'f' with respect to 'home' directory
     * example : home = /a/b/c
     *           f    = /a/d/e/x.txt
     *           s = getRelativePath(home,f) = ../../d/e/x.txt
     * @param home base path, should be a directory, not a file, or it doesn't
    make sense
     * @param f file to generate path for
     * @return path from home to f as a string
     */
    public static String getRelativePath(File home, File f) {
        File r;
        List homelist;
        List filelist;
        String s;
        homelist = getPathList(home);
        filelist = getPathList(f);
        s = matchPathLists(homelist, filelist);
        return s;
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static String getExtension(String file) {
        String ext = null;
        String s = file;
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1);
        }
        return ext;
    }

    private static List getPathList(File f) {
        List l = new ArrayList();
        File r;
        try {
            r = f.getCanonicalFile();
            while (r != null) {
                l.add(r.getName());
                r = r.getParentFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            l = null;
        }
        return l;
    }

    /**
     * figure out a string representing the relative path of
     * 'f' with respect to 'r'
     * @param r home path
     * @param f path of file
     */
    private static String matchPathLists(List r, List f) {
        int i;
        int j;
        String s;
        s = "";
        i = r.size() - 1;
        j = f.size() - 1;
        while ((i >= 0) && (j >= 0) && (r.get(i).equals(f.get(j)))) {
            i--;
            j--;
        }
        for (; i >= 0; i--) {
            s += ".." + File.separator;
        }
        for (; j >= 1; j--) {
            s += f.get(j) + File.separator;
        }
        s += f.get(j);
        return s;
    }

    public static void copyFile(File in, File out) throws IOException {
        if (in.getCanonicalPath().equals(out.getCanonicalPath())) {
            return;
        }
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }
}

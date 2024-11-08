package au.com.cahaya.asas.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.twmacinta.util.MD5;

/**
 * This class provides utility methods for working with files.
 *
 * @author Mathew Pole
 * @since  September 2001
 * @version $Revision$
 *
 */
public class FileUtil {

    /** The private logger for this class */
    private static Logger myLog = LoggerFactory.getLogger(FileUtil.class);

    /**  */
    public static final String cFileTypeDirectory = "directory";

    /**  */
    public static final String cFileTypeFile = "file";

    /**  */
    public static final String cFileTypeLink = "link";

    /**  */
    public static final String cFileTypeOther = "other";

    /**
   *
   */
    public static StringBuffer readerToString(BufferedReader in, boolean insertNewLine) throws IOException {
        StringBuffer result = new StringBuffer(in.readLine());
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (insertNewLine) {
                result.append("\n");
            }
            result.append(inputLine);
        }
        return result;
    }

    /**
   * Detect a symoblic link by comparing the canonical and absolute paths.
   *
   * Portions of this code are based on comments by J.P. Lewis at
   * http://www.idiom.com/~zilla/Xfiles/javasymlinks.html.
   *
   * @todo If a directory is symbolic link then children will also appear as
   *       symbolic links
   *
   * @return true if symbolic link (or file doesn't exist)
   *
   * @see http://www.idiom.com/~zilla/Xfiles/javasymlinks.html
   */
    public static boolean isLink(File file) throws IOException {
        if (!file.exists()) {
            return true;
        } else {
            String cnnpath = file.getCanonicalPath();
            String abspath = file.getAbsolutePath();
            return !abspath.equals(cnnpath);
        }
    }

    /**
   * @throws IOException
   *
   */
    public static String typeAsString(File file) throws IOException {
        if (isLink(file)) {
            return cFileTypeLink;
        } else if (file.isDirectory()) {
            return cFileTypeDirectory;
        } else if (file.isFile()) {
            return cFileTypeFile;
        } else {
            return cFileTypeOther;
        }
    }

    /**
   * Delete a non-empty directory.
   *
   * @param path
   * @return true on success
   */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return path.delete();
    }

    /**
   *
   * @param algorithm Must be an algorithm supported by
           {@link java.security.MessageDigest#getInstance}
   * @throws IOException
   * @see http://www.jguru.com/faq/view.jsp?EID=3822
   */
    public static String calcDigest(File file, String algorithm) throws IOException {
        return MD5.asHex(MD5.getHash(file));
    }

    /**
   * Copy a file.
   *
   * Based on http://www.rgagnon.com/javadetails/java-0064.html
   */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }
            out.setLastModified(in.lastModified());
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    /**
   *
   * @see <a href="http://www.javaworld.com/javaworld/jw-03-2001/jw-0302-java101.html">The ins and outs of standard input/output</a>
   */
    public static long readLong(InputStream in) throws IOException {
        long num = 0;
        int ch;
        boolean positive = true;
        while ((ch = in.read()) != '\n') {
            if (ch >= '0' && ch <= '9') {
                num *= 10;
                num += ch - '0';
            } else if (ch == '-') {
                positive = false;
            } else {
                break;
            }
        }
        if (positive) {
            return num;
        } else {
            return -1 * num;
        }
    }

    /**
  *
  */
    public static File createUniqueFileUsingNumber(File path, String fileName) {
        if (path == null) {
            myLog.debug("createUniqueFileUsingNumber - setting path to '.'");
            path = new File(".");
        }
        if (!path.exists() || !path.isDirectory()) {
            myLog.debug("createUniqueFileUsingNumber - error path {} invalid", path);
            return null;
        }
        File file = new File(path, fileName);
        if (file.exists()) {
            String name = fileName;
            String extension = null;
            int dotIndex = fileName.indexOf(".");
            if (dotIndex > 0) {
                name = fileName.substring(0, dotIndex - 1);
                if (fileName.length() > dotIndex + 1) {
                    extension = fileName.substring(dotIndex + 1);
                }
            }
            int number = 1;
            while (file.exists()) {
                myLog.debug("createUniqueFileUsingNumber - duplicate - fileName: {}, file: {}", fileName, file);
                file = new File(path, name + "-" + String.format("%4d", number++) + "." + extension);
            }
        }
        return file;
    }
}

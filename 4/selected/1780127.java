package fr.albin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

public class FileUtils {

    public static void copyFile(File source, File dest) throws Exception {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            throw new Exception("Cannot copy file " + source.getAbsolutePath() + " to " + dest.getAbsolutePath(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                throw new Exception("Cannot close streams.", e);
            }
        }
    }

    public static String getRelativePath(File baseDir, File file) throws Exception {
        String result = "";
        LOGGER.debug("Base dir : " + baseDir.getAbsolutePath());
        LOGGER.debug("File : " + file.getAbsolutePath());
        LOGGER.debug("File separator : " + File.separator);
        StringTokenizer tokenizer1 = new StringTokenizer(baseDir.getAbsolutePath(), File.separator);
        StringTokenizer tokenizer2 = new StringTokenizer(file.getAbsolutePath(), File.separator);
        while (tokenizer1.hasMoreTokens()) {
            if (!tokenizer2.hasMoreTokens()) {
                throw new Exception("The base dir path " + baseDir.getAbsolutePath() + " is not valid for file " + file.getAbsolutePath());
            }
            if (!tokenizer1.nextToken().equals(".")) {
                tokenizer2.nextToken();
            }
        }
        while (tokenizer2.hasMoreTokens()) {
            if (result.length() > 0) {
                result += File.separator;
            }
            result += tokenizer2.nextToken();
        }
        LOGGER.info("Relative path is : " + result);
        return result;
    }

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class);
}

package tools.files;

import engine.EngineDriver;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUtils {

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        long flag = 0;
        if (!destFile.exists()) destFile.createNewFile();
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            flag = destination.transferFrom(source, 0, source.size());
        } catch (Exception e) {
            Logger.getLogger(FileUtils.class.getPackage().getName()).log(Level.WARNING, "ERROR: Problem copying file", e);
        } finally {
            if (source != null) source.close();
            if (destination != null) destination.close();
        }
        if (flag == 0) return false; else return true;
    }

    public static boolean checkImgEncomDirectory(File source) {
        if (source.getParent().equals("images")) {
            File token = source.getParentFile();
            if (token.getParent().equals("ENCOM")) return true; else return false;
        } else return false;
    }
}

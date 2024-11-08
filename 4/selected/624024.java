package edu.clemson.cs.nestbed.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileUtils {

    private static final Log log = LogFactory.getLog(FileUtils.class);

    public static void copyFile(File in, File out) throws FileNotFoundException, IOException {
        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = new FileInputStream(in).getChannel();
            destinationChannel = new FileOutputStream(out).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        } finally {
            try {
                sourceChannel.close();
            } catch (Exception ex) {
            }
            try {
                destinationChannel.close();
            } catch (Exception ex) {
            }
        }
    }

    public static File makeProgramDir(File root, int testbedID, int projectID, int programID) {
        File dir = new File(root, Integer.toString(testbedID));
        dir.mkdir();
        dir = new File(dir, Integer.toString(projectID));
        dir.mkdir();
        dir = new File(dir, Integer.toString(programID));
        dir.mkdir();
        return dir;
    }

    public static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
            directory.delete();
        }
    }

    public static void cleanupParentDirectories(File root, File directory) {
        for (File dir = directory; dir != null && !dir.equals(root); dir = dir.getParentFile()) {
            if (dir.exists()) {
                dir.delete();
            }
        }
    }
}

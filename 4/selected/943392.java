package oldmcdata.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class CopyFilesFromTo {

    private CopyFilesFromTo() {
    }

    public static void copyFromTo(String srcFileName, String destFileName) {
        File srcFile = new File(srcFileName);
        File destFile = new File(destFileName);
        copyFromTo(srcFile, destFile);
    }

    public static void copyFromTo(File srcFile, File destFile) {
        FileChannel in = null, out = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(srcFile);
        } catch (FileNotFoundException fnfe) {
            System.out.println("File: " + srcFile.toString());
            System.out.println("file does not exist, " + "is a directory rather than a regular file, " + "or for some other reason cannot be opened for reading");
            System.exit(-1);
        }
        try {
            fos = new FileOutputStream(destFile);
        } catch (FileNotFoundException fnfe) {
            System.out.println("File: " + destFile.toString());
            System.out.println("file exists but is a directory rather than a regular file, " + "does not exist but cannot be created, " + "or cannot be opened for any other reason");
            System.exit(-1);
        }
        try {
            in = fis.getChannel();
            out = fos.getChannel();
            in.transferTo(0, in.size(), out);
            fos.flush();
            fos.close();
            out.close();
            fis.close();
            in.close();
            System.out.println("Completed copying " + srcFile.toString() + " to " + destFile.toString());
        } catch (IOException ioe) {
            System.out.println("IOException copying file: " + ioe.getMessage());
            System.exit(-1);
        }
        long srcModified = srcFile.lastModified();
        if (srcModified > 0L && destFile.exists()) {
            destFile.setLastModified(srcModified);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("arguments: sourcefile destfile");
            System.exit(-1);
        }
        System.out.println("copying sourcefile: " + args[0]);
        System.out.println("to destfile: " + args[1]);
        CopyFilesFromTo.copyFromTo(args[0], args[1]);
    }
}

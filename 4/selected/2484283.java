package uk.ac.roe.antigen.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 * A file that is able to be copied - you'd think this would be straightforward
 * wouldn't you?
 * 
 * @author jdt
 */
public class CopyableFile extends File {

    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(CopyableFile.class.getName());

    public static void main(String[] args) throws IOException {
        CopyableFile file = new CopyableFile("C:/cygwin");
        file.copyTo(new File("C:/cygwin2"));
        System.out.println("is abs " + file.isAbsolute());
        System.out.println("absolute file " + file.getAbsoluteFile());
        System.out.println("absolute path " + file.getAbsolutePath());
        System.out.println("canonical file " + file.getCanonicalFile());
        System.out.println("canonical path " + file.getCanonicalPath());
        System.out.println("name " + file.getName());
        System.out.println("parent " + file.getParent());
        System.out.println("parent file " + file.getParentFile());
        System.out.println("path " + file.getPath());
    }

    /**
     * Copy this file or directory to the given new location
	 * @param newLocation
	 * @throws IOException
	 */
    public void copyTo(File newLocation) throws IOException {
        logger.fine("Copying " + getAbsolutePath() + " to " + newLocation);
        if (isFile()) {
            logger.fine("(file)");
            copyFileTo(newLocation);
        } else {
            logger.fine("(folder)");
            File[] files = listFiles();
            logger.fine("Contains " + files.length + " files");
            newLocation.mkdirs();
            for (int i = 0; i < files.length; ++i) {
                logger.fine("Processing " + files[i]);
                CopyableFile file = new CopyableFile(files[i]);
                File newFile = new File(newLocation, file.getName());
                file.copyTo(newFile);
            }
        }
    }

    /**
     * You'd think this would be obvious....
     * Remove a directory, even if not empty
     */
    public boolean recursivelyDelete() {
        if (isFile()) {
            logger.fine("deleting " + getName());
            return delete();
        } else {
            File[] files = listFiles();
            for (int i = 0; i < files.length; ++i) {
                CopyableFile file = new CopyableFile(files[i]);
                file.recursivelyDelete();
            }
            return delete();
        }
    }

    private void copyFileTo(File destination) throws IOException {
        logger.fine("Copying from " + destination + "...");
        FileChannel srcChannel = new FileInputStream(getAbsolutePath()).getChannel();
        logger.fine("...got source channel " + srcChannel + "...");
        FileChannel destChannel = new FileOutputStream(new File(destination.getAbsolutePath())).getChannel();
        logger.fine("...got destination channel " + destChannel + "...");
        logger.fine("...Got channels...");
        destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        logger.fine("...transferred.");
        srcChannel.close();
        destChannel.close();
    }

    /**
	 * @param arg0
	 */
    public CopyableFile(String arg0) {
        super(arg0);
    }

    /**
	 * @param arg0
	 * @param arg1
	 */
    public CopyableFile(File arg0, String arg1) {
        super(arg0, arg1);
    }

    public CopyableFile(File arg0) {
        super(arg0.getAbsolutePath());
    }

    /**
	 * @param arg0
	 * @param arg1
	 */
    public CopyableFile(String arg0, String arg1) {
        super(arg0, arg1);
    }

    /**
	 * @param arg0
	 */
    public CopyableFile(URI arg0) {
        super(arg0);
    }
}

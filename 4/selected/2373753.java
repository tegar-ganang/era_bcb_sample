package org.matsim.contrib.matsim4opus.utils.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * @author thomas
 *
 */
public class FileCopy {

    private static final Logger log = Logger.getLogger(FileCopy.class);

    /**
	 * Copies all files from a source directory into a target directory
	 * @param sourceDir
	 * @param targetRoot
	 * @return true if successful 
	 */
    public static boolean copyTree(String sourceDir, String targetRoot) {
        boolean result;
        try {
            File source = new File(sourceDir);
            File root = new File(targetRoot);
            if (source.exists() == false || source.isDirectory() == false) {
                log.error("Source path dosn't exsist (\"" + source.getCanonicalPath() + "\"). Can't copy files.");
                return false;
            }
            if (root.exists() == false) {
                log.error("Destination path dosn't exsist (\"" + root.getCanonicalPath() + "\").");
                log.info("Creating destination directory.");
                if (!root.mkdirs()) {
                    log.equals("Creating destination directory faild!");
                    return false;
                }
            }
            String targetRootName = Paths.checkPathEnding(root.getCanonicalPath());
            ArrayList<File> fileNames = listAllFiles(source, true);
            result = true;
            File target;
            for (File f : fileNames) {
                String fullName = f.getCanonicalPath();
                int pos = fullName.indexOf(sourceDir);
                String subName = null;
                if (sourceDir.endsWith("/")) subName = fullName.substring(pos + sourceDir.length()); else subName = fullName.substring(pos + sourceDir.length() + 1);
                String targetName = targetRootName + subName;
                target = new File(targetName);
                if (f.isDirectory()) {
                    if (target.exists() == false) {
                        boolean st = target.mkdir();
                        if (st == false) result = false;
                    }
                    continue;
                }
                boolean st = fileCopy(f, target);
                if (st == false) result = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
	 * Gather all files within a directory
	 * @param rootDir directory
	 * @param includeDirNames true if subdirectories
	 * @return all files within a given directory
	 */
    public static ArrayList<File> listAllFiles(File rootDir, boolean includeDirNames) {
        ArrayList<File> result = new ArrayList<File>();
        try {
            File[] fileList = rootDir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory() == true) {
                    if (includeDirNames) result.add(fileList[i]);
                    result.addAll(listAllFiles(fileList[i], includeDirNames));
                } else result.add(fileList[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Copies a file (source file) to a given output file
	 * @param sourceFile
	 * @param outputFile
	 * @return 
	 * @throws IOException
	 * @throws Exception
	 */
    public static boolean fileCopy(File sourceFile, File outputFile) throws IOException, Exception {
        log.info("Copying file " + sourceFile.getCanonicalPath() + " to " + outputFile.getCanonicalPath());
        return writeBinaryFile(readBinaryFile(sourceFile), outputFile);
    }

    /** 
	* reads a binary input file into a byte array
	* 
	* @param fileName  binary input file
	* @return          byte[] or null
	*/
    public static byte[] readBinaryFile(File sourceFile) {
        byte[] result = null;
        try {
            BufferedInputStream input;
            input = new BufferedInputStream(new FileInputStream(sourceFile));
            int num = input.available();
            result = new byte[num];
            input.read(result, 0, num);
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    /** 
	* writes a byte array into a given output file 
    * 
    * @param data     output data
    * @param outputFile output file 
    * @return         true if successful
    */
    public static boolean writeBinaryFile(byte[] data, File outputFile) {
        boolean result = true;
        try {
            BufferedOutputStream output;
            output = new BufferedOutputStream(new FileOutputStream(outputFile));
            output.write(data, 0, data.length);
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
	 * moving a file to another directory
	 * @param source String
	 * @param target String
	 * @return true if successful
	 */
    public static boolean moveFileOrDirectory(String source, String target) {
        File sourceFile = new File(source);
        File destinationDir = null;
        if (sourceFile.exists()) {
            log.info("Moving " + source + " to " + target);
            FileName fm = new FileName(target, '/', '.');
            if (fm.filename().length() <= 0) {
                log.error("No file name for hot start pop file given!");
                return false;
            }
            destinationDir = new File(fm.path());
            if (!destinationDir.exists()) {
                log.info("Target directory doesn't exsit and will created ...");
                destinationDir.mkdirs();
                log.info("... done!");
            }
            boolean success = sourceFile.renameTo(new File(target));
            return success;
        }
        log.error("File not found: " + source);
        return false;
    }

    /**
	 * testing FileCopy
	 * @param args
	 */
    public static void main(String args[]) {
        String source = "/Users/thomas/Development/opus_home/matsim4opus/output/output_plans.xml.gz";
        String target = "/Users/thomas/Development/opus_home/matsim4opus/tmp/blalbla.xml.gz";
        FileCopy.moveFileOrDirectory(source, target);
    }
}

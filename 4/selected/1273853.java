package playground.tnicolai.urbansim.utils.io;

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
	 * @return true if succssesful 
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
            if (root.exists() == false || root.isDirectory() == false) {
                log.error("Destination path dosn't exsist (\"" + root.getCanonicalPath() + "\"). Can't copy files.");
                return false;
            }
            String targetRootName = root.getCanonicalPath() + File.separator;
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
        log.info("Copying generated outputPlans file " + sourceFile.getCanonicalPath() + " to " + outputFile.getCanonicalPath());
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
}

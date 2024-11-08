package fr.sonictools.jgrisbicatcleaner.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

    /**
	 * makes a copy of a file
	 * 
	 * @param inputFile
	 *            the file to be copied
	 * @param outputFile
	 *            the copy
	 * @throws IOException
	 */
    public static void copyFile(File inputFile, File outputFile) throws IOException {
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            inChannel = new FileInputStream(inputFile).getChannel();
            outChannel = new FileOutputStream(outputFile).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (inChannel != null) {
                    inChannel.close();
                }
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**
	 * display a file given as input, on the standard output
	 * 
	 * @param fileToDisplay
	 * @throws IOException
	 */
    public static void displayFile(File fileToDisplay) throws IOException {
        BufferedReader br = null;
        String curLine = null;
        br = new BufferedReader(new FileReader(fileToDisplay));
        try {
            while ((curLine = br.readLine()) != null) {
                System.out.println(curLine);
            }
        } finally {
            br.close();
        }
    }

    /**
	 * Ensure the concerned file contains the input String This function make
	 * the search line by line.
	 * 
	 * @param file
	 * @param string
	 * @return boolean true->the input string exists in the file / false->the
	 *         input string does not exist in the file
	 */
    public static boolean isStringExists(File file, String string) {
        BufferedReader br = null;
        String curLine = null;
        boolean isStringExists = false;
        try {
            br = new BufferedReader(new FileReader(file));
            while ((curLine = br.readLine()) != null) {
                if (curLine.contains(string)) {
                    isStringExists = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            isStringExists = false;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    isStringExists = false;
                }
            }
        }
        return isStringExists;
    }

    /**
	 * Compare 2 files line by line.
	 * 
	 * @param file1
	 * @param file2
	 * @return boolean true->equals / false->not equals
	 */
    public static boolean compareFiles(File file1, File file2) {
        BufferedReader br1 = null;
        String curLine1 = null;
        BufferedReader br2 = null;
        String curLine2 = null;
        boolean areEquals = true;
        if (file1.length() != file2.length()) {
            return false;
        }
        try {
            br1 = new BufferedReader(new FileReader(file1));
            br2 = new BufferedReader(new FileReader(file2));
            while ((curLine1 = br1.readLine()) != null) {
                curLine2 = br2.readLine();
                if (!curLine1.equals(curLine2)) {
                    areEquals = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            areEquals = false;
        } finally {
            try {
                br1.close();
                br2.close();
            } catch (IOException e) {
                e.printStackTrace();
                areEquals = false;
            }
        }
        return areEquals;
    }
}

package com.ctext.autshumatopatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Static utillities.
 * @author W. Fourie
 */
public class Utils {

    private static String OS;

    private static final String pathToFiles = "." + File.separator + "files" + File.separator;

    public static String APP_TO_PATCH = "";

    public static String APP_OLD_VERSION = "";

    public static String APP_NEW_VERSION = "";

    private static String jarFile = "";

    private static HashMap<String, String> osPaths = new HashMap<String, String>();

    /**
     * Detects the OS name from the system properties.
     */
    public static void detectOS() {
        OS = System.getProperty("os.name");
        System.out.println("detectOS: " + OS);
    }

    /**
     * Returns the OS string detected.
     */
    public static String getOS() {
        return OS;
    }

    /**
     * Reads the supplied 'ImportStrings.dat' file for the application specific strings & properties.
     */
    public static void readAppData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("./ImportStrings.dat"));
            String rdLine;
            while ((rdLine = br.readLine()) != null) {
                if (rdLine.startsWith("APP_TO_PATCH=")) APP_TO_PATCH = extractString(rdLine, "APP_TO_PATCH=");
                if (rdLine.startsWith("APP_OLD_VERSION=")) APP_OLD_VERSION = extractString(rdLine, "APP_OLD_VERSION=");
                if (rdLine.startsWith("APP_NEW_VERSION")) APP_NEW_VERSION = extractString(rdLine, "APP_NEW_VERSION=");
                if (rdLine.startsWith("jarFile=")) jarFile = extractString(rdLine, "jarFile=");
                if (rdLine.startsWith("pathXP")) osPaths.put("Windows XP", extractString(rdLine, "pathXP="));
                if (rdLine.startsWith("pathVISTA")) osPaths.put("Windows Vista", extractString(rdLine, "pathVISTA="));
                if (rdLine.startsWith("path7")) osPaths.put("Windows 7", extractString(rdLine, "path7="));
                if (rdLine.startsWith("pathLINUX")) osPaths.put("Linux", extractString(rdLine, "pathLINUX="));
                if (rdLine.startsWith("pathMAC")) osPaths.put("Mac", extractString(rdLine, "pathMAC="));
            }
            br.close();
        } catch (FileNotFoundException fnf) {
            fnf.printStackTrace();
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    private static String extractString(String line, String start) {
        return line.substring(line.indexOf(start) + start.length());
    }

    /**
     * Attempts to detect the path to the specified application.
     * @return
     * <li> If found the complete path to the application is returned.
     * <li> If the application could not be automatically detected an empty string is returned.
     */
    public static String detectPath() {
        String retString = "";
        for (String id : osPaths.keySet()) {
            if (id.equals(OS)) {
                if (validateLocation(osPaths.get(id))) retString = osPaths.get(id);
                break;
            }
        }
        return retString;
    }

    /** 
     * Check if the presented directory contains <code>jarFile</code>.
     * Will return true if the location is valid. False otherwise.
     */
    public static boolean validateLocation(String path) {
        if (!path.endsWith(File.separator)) path = path + File.separator;
        File file = new File(path + jarFile);
        if (file.exists()) return true; else return false;
    }

    /**
     * Collects a list of the files to be copied.
     */
    public static HashSet<String> collectFilesToCopy() {
        HashSet<String> fileSet = new HashSet<String>();
        File filesDir = new File(pathToFiles);
        if (filesDir.exists()) fileSet = collectFile(filesDir, fileSet); else System.out.println("collectFilesToCopy: Invalid path to the files directory.");
        return fileSet;
    }

    private static HashSet<String> collectFile(File file, HashSet<String> fileSet) {
        if (file.isDirectory()) for (File nowFile : file.listFiles()) {
            if (nowFile.isDirectory()) fileSet.addAll(collectFile(nowFile, fileSet)); else fileSet.add(nowFile.getPath());
        } else fileSet.add(file.getPath());
        return fileSet;
    }

    /**
     * Parses a filename by removing the <code>pathToFiles</code> sequence at the begging.
     * This is used to get the relative paths of the files to be copied.
     * @param in The filename to parse.
     * @return The modified filename without the <code>pathToFiles</code> at the start.
     */
    public static String parseFilePath(String in) {
        return in.substring(pathToFiles.length());
    }

    /**
     * Copies the inFile to the outFile.
     * @param outFile Destination of the new copy of <code>inFile</code>.
     * @param inFile The file to be copied.
     * @return The success of the operation.
     */
    public static boolean copyFile(File outFile, File inFile) {
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            if (outFile.createNewFile()) {
                inStream = new FileInputStream(inFile);
                outStream = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inStream.read(buffer)) > 0) outStream.write(buffer, 0, length);
                inStream.close();
                outStream.close();
            } else return false;
        } catch (IOException iox) {
            iox.printStackTrace();
            return false;
        }
        return true;
    }
}

package org.tigr.antware.shared.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import org.tigr.antware.shared.exceptions.DataFormatErrorException;
import org.tigr.antware.shared.exceptions.InvalidFileFormatException;

/**
 * The class <code>FileUtils</code> provides utility methods to test different
 * types of documents. Presently the following types of files can be compared
 * 1. Text files where comparison is made line by line for equality
 * 2. Properties files where comparison is made for the existence of same 
 * properties and property values
 *
 */
public class FileUtils {

    private static Logger logger = new Logger(FileUtils.class.getCanonicalName());

    /**
     * The <code>compareTextFiles</code> method compares two text files line by line
     * for the same text.
     *
     * @param firstFile a <code>String</code> value for the first file name
     * @param secondFile a <code>String</code> value for the second file name
     * @return a <code>boolean</code> value of true if both the files are same
     * @exception FileNotFoundException if one of the specified files does not exist
     * @exception IOException if an error occurs reading one of the files
     */
    public static boolean compareTextFiles(String firstFile, String secondFile) throws FileNotFoundException, IOException {
        LineNumberReader lnrFirst = null;
        LineNumberReader lnrSecond = null;
        boolean success = true;
        try {
            lnrFirst = new LineNumberReader(new FileReader(firstFile));
            lnrSecond = new LineNumberReader(new FileReader(secondFile));
            boolean loop = true;
            while (loop) {
                String first = lnrFirst.readLine();
                String second = lnrSecond.readLine();
                if ((first != null) && (second != null)) {
                    if (!first.equals(second)) {
                        loop = false;
                        success = false;
                    }
                } else if ((first != null) || (second != null)) {
                    loop = false;
                    success = false;
                } else {
                    loop = false;
                }
            }
        } finally {
            if (lnrFirst != null) {
                lnrFirst.close();
            }
            if (lnrSecond != null) {
                lnrSecond.close();
            }
        }
        return success;
    }

    /**
     * The <code>comparePropertiesFiles</code> method compares two properties files 
     * to ensure that the files contain all the properties and that the property
     * values are same. The comparison is done by creating two properties hashes
     * and iterating over the hashes to ensure that the properties are same.
     *
     * @param firstFile a <code>String</code> value for the first file name
     * @param secondFile a <code>String</code> value for the second file name
     * @return a <code>boolean</code> value of true if both the files are same
     * @exception FileNotFoundException if one of the specified files does not exist
     * @exception IOException if an error occurs reading one of the files
     */
    public static boolean comparePropertiesFiles(String firstFile, String secondFile) throws FileNotFoundException, IOException {
        if (logger.isFinerEnabled()) {
            logger.finer("Comparing properties in file " + firstFile + " with properties in file " + secondFile);
        }
        FileInputStream fis = new FileInputStream(firstFile);
        Properties first = new Properties();
        first.load(fis);
        fis.close();
        fis = new FileInputStream(secondFile);
        Properties second = new Properties();
        second.load(fis);
        fis.close();
        if (first.size() != second.size()) {
            if (logger.isFinestEnabled()) {
                logger.finest("The two properties are not same length");
            }
            return false;
        }
        for (Enumeration e = first.keys(); e.hasMoreElements(); ) {
            String prop = (String) e.nextElement();
            if (!second.containsKey(prop)) {
                if (logger.isFinestEnabled()) {
                    logger.finest("The second properties file does not contain property " + prop);
                }
                return false;
            }
            String firstValue = (String) first.get(prop);
            String secondValue = (String) second.get(prop);
            if (!firstValue.equals(secondValue)) {
                if (logger.isFinestEnabled()) {
                    logger.finest("The value " + secondValue + " for the property " + prop + " in second properties file does not match the first value " + firstValue);
                }
                return false;
            }
        }
        if (logger.isFinerEnabled()) {
            logger.finer("Compared properties");
        }
        return true;
    }

    /**
     * The <code>compareBCPFiles</code> method compares two BCP files 
     * for the same text. This method assumes that although the data is the
     * same the order may not be the same, so the method first loads the
     * contents of a file entirely in a hash and then reads the second
     * file to see if all the entries in the file exist in the hash and vice versa
     *
     * @param firstFile a <code>String</code> value for the first file name
     * @param secondFile a <code>String</code> value for the second file name
     * @return a <code>boolean</code> value of true if both the files are same
     * @exception FileNotFoundException if one of the specified files does not exist
     * @exception IOException if an error occurs reading one of the files
     */
    public static boolean compareBCPFiles(String firstFile, String secondFile) throws FileNotFoundException, IOException {
        if (logger.isFinerEnabled()) {
            logger.finer("Comparing BCP data in file " + firstFile + " with BCP data in file " + secondFile);
        }
        HashSet firstFileData = new HashSet();
        LineNumberReader lnrFirst = null;
        LineNumberReader lnrSecond = null;
        try {
            lnrFirst = new LineNumberReader(new FileReader(firstFile));
            String first = "";
            while ((first = lnrFirst.readLine()) != null) {
                firstFileData.add(first);
            }
        } finally {
            if (lnrFirst != null) {
                lnrFirst.close();
            }
        }
        try {
            lnrSecond = new LineNumberReader(new FileReader(secondFile));
            String second = "";
            while ((second = lnrSecond.readLine()) != null) {
                if (firstFileData.contains(second)) {
                    firstFileData.remove(second);
                } else {
                    if (logger.isFinestEnabled()) {
                        logger.finest("The data " + second + " does not exist in the first file " + firstFile);
                    }
                    return false;
                }
            }
            if (firstFileData.size() != 0) {
                if (logger.isFinestEnabled()) {
                    logger.finest("The first file " + firstFile + " contains extra data not contained in " + secondFile);
                }
                return false;
            }
        } finally {
            if (lnrSecond != null) {
                lnrSecond.close();
            }
        }
        if (logger.isFinestEnabled()) {
            logger.finest("The first file " + firstFile + " and second file " + secondFile + " contain identical data");
        }
        return true;
    }

    /**
     * The <code>preloadBuffer</code> method loads data from the specified reader
     * till the end of line terminator is detected or the end-of-file is reached.
     * This loaded data is appended to the buffer and the buffer returned
     *
     * @param reader a <code>BufferedReader</code> value for the buffered file reader
     * @param buffer a <code>StringBuffer</code> value containing the read data
     * @param terminator a <code>String</code> value for the record terminator string
     * @return a <code>StringBuffer</code> value containing accumulated data, null 
     * if the end of file has reached
     * @exception IOException if an error occurs
     */
    private static StringBuffer preloadBuffer(BufferedReader reader, StringBuffer buffer, String terminator) throws IOException {
        char[] readChars = new char[1024];
        boolean termFound = false;
        boolean eof = false;
        int termPos = buffer.toString().indexOf(terminator);
        if (termPos != -1) {
            return buffer;
        }
        while ((!termFound) && (!eof)) {
            int readCount = reader.read(readChars, 0, 1024);
            if (readCount == -1) {
                eof = true;
            } else {
                buffer.append(readChars, 0, readCount);
            }
            termPos = buffer.toString().indexOf(terminator);
            if (termPos != -1) {
                return buffer;
            }
        }
        return null;
    }

    /**
     * The <code>compareBCPFiles</code> method compares two BCP files 
     * for the same text with a record terminator that is different than a newline. 
     * This method assumes that although the data is the
     * same the order may not be the same, so the method first loads the
     * contents of a file entirely in a hash and then reads the second
     * file to see if all the entries in the file exist in the hash and vice versa
     *
     * @param firstFile a <code>String</code> value for the first file name
     * @param secondFile a <code>String</code> value for the second file name
     * @param terminator a <code>String</code> value for the record terminator
     * @return a <code>boolean</code> value of true if both the files are same
     * @exception FileNotFoundException if one of the specified files does not exist
     * @exception IOException if an error occurs reading one of the files
     */
    public static boolean compareBCPFiles(String firstFile, String secondFile, String terminator) throws FileNotFoundException, IOException {
        if (logger.isFinerEnabled()) {
            logger.finer("Comparing BCP data in file " + firstFile + " with BCP data in file " + secondFile + " with record terminator " + terminator);
        }
        HashSet firstFileData = new HashSet();
        BufferedReader brFirst = null;
        BufferedReader brSecond = null;
        StringBuffer firstBuffer = new StringBuffer();
        StringBuffer secondBuffer = new StringBuffer();
        try {
            brFirst = new BufferedReader(new FileReader(firstFile));
            String first = "";
            while ((firstBuffer = preloadBuffer(brFirst, firstBuffer, terminator)) != null) {
                int termPos = firstBuffer.toString().indexOf(terminator);
                termPos += terminator.length();
                first = firstBuffer.substring(0, termPos);
                firstBuffer = new StringBuffer(firstBuffer.substring(termPos));
                firstFileData.add(first);
            }
        } finally {
            if (brFirst != null) {
                brFirst.close();
            }
        }
        try {
            brSecond = new BufferedReader(new FileReader(secondFile));
            String second = "";
            while ((secondBuffer = preloadBuffer(brSecond, secondBuffer, terminator)) != null) {
                int termPos = secondBuffer.toString().indexOf(terminator);
                termPos += terminator.length();
                second = secondBuffer.substring(0, termPos);
                secondBuffer = new StringBuffer(secondBuffer.substring(termPos));
                if (firstFileData.contains(second)) {
                    firstFileData.remove(second);
                } else {
                    if (logger.isFinestEnabled()) {
                        logger.finest("The data " + second + " does not exist in the first file " + firstFile);
                    }
                    return false;
                }
            }
            if (firstFileData.size() != 0) {
                if (logger.isFinestEnabled()) {
                    logger.finest("The first file " + firstFile + " contains extra data not contained in " + secondFile);
                }
                return false;
            }
        } finally {
            if (brSecond != null) {
                brSecond.close();
            }
        }
        if (logger.isFinestEnabled()) {
            logger.finest("The first file " + firstFile + " and second file " + secondFile + " contain identical data");
        }
        return true;
    }

    /**
     * The <code>copyFile</code> method copies the specified source file to the specified
     * destination file.
     *
     * @param sourceFile a <code>String</code> value representing the source file name
     * @param destFile a <code>String</code> value representing the destination file name
     * @exception FileNotFoundException if an error occurs opening the source file
     * @exception IOException if an error occurs reading or writing files
     */
    public static void copyFile(String sourceFile, String destFile) throws FileNotFoundException, IOException {
        if (logger.isFinestEnabled()) {
            logger.finest("Copying file '" + sourceFile + "' to '" + destFile + "'");
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        int data = -1;
        try {
            fis = new FileInputStream(sourceFile);
            fos = new FileOutputStream(destFile);
            while ((data = fis.read()) != -1) {
                fos.write(data);
            }
        } finally {
            if (fis != null) fis.close();
            if (fos != null) fos.close();
        }
    }

    /**
     * The method <code>compareINIFiles</code> method compares the contents of the two INI style files
     * to make sure that the files are similar. The method ignores the order in which entries exist,
     * but ensures that all entries and their values exist in both files. This comparison is white 
     * space sensitive for the values of each of the keys.
     * 
     * @param firstFile
     * @param secondFile
     * 
     * @return a <code>boolean</code> of true if the two files are similar, false otherwise
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidFileFormatException
     * @throws DataFormatErrorException a <code></code>
     */
    public static boolean compareINIFiles(String firstFile, String secondFile) throws FileNotFoundException, IOException, InvalidFileFormatException, DataFormatErrorException {
        boolean same = false;
        if (logger.isFinestEnabled()) {
            logger.finest("Comparing INI file: " + firstFile + " to " + secondFile);
        }
        IniReader reader = new IniReader(firstFile);
        Map firstMap = reader.getIniMap();
        reader = new IniReader(secondFile);
        Map secondMapMap = reader.getIniMap();
        return SimilarityUtils.areMapsSimilar(firstMap, secondMapMap);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length < 3) {
            System.out.println("Usage: FileUtils <first file> <second file> <type (text|bcp|properties|copy)> <terminator>");
            System.exit(1);
        }
        boolean result = false;
        if (args[2].equalsIgnoreCase("text")) {
            result = FileUtils.compareTextFiles(args[0], args[1]);
            System.out.println("The two " + args[2] + " files " + args[0] + " and " + args[1] + " are identical: " + result);
        } else if (args[2].equalsIgnoreCase("properties")) {
            result = FileUtils.comparePropertiesFiles(args[0], args[1]);
            System.out.println("The two " + args[2] + " files " + args[0] + " and " + args[1] + " are identical: " + result);
        } else if (args[2].equalsIgnoreCase("bcp")) {
            if (args.length == 4) {
                result = FileUtils.compareBCPFiles(args[1], args[0], args[3]);
            } else {
                result = FileUtils.compareBCPFiles(args[1], args[0]);
            }
            System.out.println("The two " + args[2] + " files " + args[0] + " and " + args[1] + " are identical: " + result);
        } else if (args[2].equalsIgnoreCase("copy")) {
            FileUtils.copyFile(args[0], args[1]);
        }
        System.exit(0);
    }
}

package org.amlfilter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/**
 *
 * This class has a set of utilities for creating and handling data files utilities.
 * 	These utilities are originally used for the testing of the loader (making a parallel load and comparing the data).
 *
 * @author Marco
 *
 */
public class DataFileUtils {

    /**
	 * Writes a string to a file as a new line and returns the first char position.
	 * It uses FileChannel. It is recommended to open it in append mode.
	 * It takes the format for the new line from the general constants. IT ADDS the new line char.
	 * The string is written as it comes. No char conversion is made.
	 *
	 * @param pFc	The FileChannel to use
	 * @param pLine	The string to write.
	 * @return		The position where the line was written.
	 * @throws Exception
	 *
	 */
    public static long appendLine(FileChannel pFc, String pLine) throws Exception {
        long retVal = pFc.size();
        byte[] stringByteArray = pLine.getBytes(GeneralConstants.UTF8);
        byte[] newLineByteArray = GeneralConstants.NEW_LINE_TOKEN.getBytes(GeneralConstants.UTF8);
        ByteBuffer bb = ByteBuffer.allocate(stringByteArray.length + newLineByteArray.length);
        bb.put(stringByteArray);
        bb.put(newLineByteArray);
        bb.rewind();
        pFc.write(bb);
        return retVal;
    }

    public static long appendLine(File pF, String pLine) throws Exception {
        long retVal = pF.length();
        FileUtils.writeStringToFile(pF, pLine.concat(GeneralConstants.NEW_LINE_TOKEN), GeneralConstants.UTF8);
        return retVal;
    }

    /**
	 * Creates a FileChannel opened in append mode.
	 * Remember to manually close it after using it.
	 *
	 * @param pFilePath		The path to the file.
	 * @return				The FileChannel
	 * @throws Exception
	 */
    public static FileChannel openFileChannelForAppend(String pFilePath) throws Exception {
        File f = new File(pFilePath);
        FileOutputStream fos = new FileOutputStream(f, true);
        return fos.getChannel();
    }

    /**
	 * Creates a FileChannel opened in READ mode.
	 * Remember to manually close it after using it.
	 *
	 * @param pFilePath		The path to the file.
	 * @return				The FileChannel
	 * @throws Exception
	 */
    public static FileChannel openFileChannelForReading(String pFilePath) throws Exception {
        File f = new File(pFilePath);
        FileInputStream fis = new FileInputStream(f);
        return fis.getChannel();
    }

    /**
	 * Reads a line from a file channel from the specified offset.
	 * It checks the consistency in the provided offset: if it bigger than zero, it checks that there is a
	 * new line right before it.
	 * If the line is too big (>2000), it raises an exception.
	 *
	 * @param pFc			The file channel
	 * @param pLineOffset	The line offset to read from
	 * @return				The string with the line contents
	 * @throws Exception
	 */
    public static String readLineAt(FileChannel pFc, long pLineOffset) throws Exception {
        return readLineAt(pFc, pLineOffset, GeneralConstants.UTF8);
    }

    /**
	 * Reads a line from a file channel from the specified offset.
	 * It checks the consistency in the provided offset: if it bigger than zero, it checks that there is a
	 * new line right before it.
	 * If the line is too big (>2000), it raises an exception.
	 *
	 * @param pFc			The file channel
	 * @param pLineOffset	The line offset to read from
	 * @return				The string with the line contents
	 * @throws Exception
	 */
    public static String readLineAt(FileChannel pFc, long pLineOffset, String pEncoding) throws Exception {
        int numOfCharsToRead = 300;
        int scaleForFurtherReading = 2;
        String lineSeparator = GeneralConstants.NEW_LINE_TOKEN;
        if (pLineOffset > 0) {
            int lineSepLength = lineSeparator.length();
            ByteBuffer bbSep = ByteBuffer.allocate(lineSepLength);
            pFc.read(bbSep, pLineOffset - lineSepLength);
            String previousLineSeparator = new String(bbSep.array(), pEncoding);
            if (!previousLineSeparator.equals(lineSeparator)) {
                throw new IllegalStateException("the provided offset (" + pLineOffset + ") is not the begining of a line.");
            }
        }
        ByteBuffer bb = ByteBuffer.allocate(numOfCharsToRead);
        pFc.read(bb, pLineOffset);
        String rawBytes = new String(bb.array(), pEncoding);
        String[] lines = rawBytes.split(lineSeparator);
        if (lines.length < 2) {
            numOfCharsToRead = numOfCharsToRead * scaleForFurtherReading;
            bb = ByteBuffer.allocate(numOfCharsToRead);
            pFc.read(bb, pLineOffset);
            rawBytes = new String(bb.array(), pEncoding);
            lines = rawBytes.split(lineSeparator);
            if (lines.length < 2) {
                numOfCharsToRead = numOfCharsToRead * scaleForFurtherReading;
                bb = ByteBuffer.allocate(numOfCharsToRead);
                pFc.read(bb, pLineOffset);
                rawBytes = new String(bb.array(), pEncoding);
                lines = rawBytes.split(lineSeparator);
                if (lines.length < 2) {
                    numOfCharsToRead = numOfCharsToRead * scaleForFurtherReading;
                    bb = ByteBuffer.allocate(numOfCharsToRead);
                    pFc.read(bb, pLineOffset);
                    rawBytes = new String(bb.array(), pEncoding);
                    lines = rawBytes.split(lineSeparator);
                    if (lines.length < 2) {
                        numOfCharsToRead = numOfCharsToRead * scaleForFurtherReading;
                        bb = ByteBuffer.allocate(numOfCharsToRead);
                        pFc.read(bb, pLineOffset);
                        rawBytes = new String(bb.array(), pEncoding);
                        lines = rawBytes.split(lineSeparator);
                        if (lines.length < 2) {
                            throw new IllegalStateException("* Found a line bigger than " + numOfCharsToRead + " bytes . Offset: " + pLineOffset);
                        }
                    }
                }
            }
        }
        return lines[0];
    }

    /**
	 * Reads a line from a file channel from the specified offset.
	 * It checks the consistency in the provided offset: if it bigger than zero, it checks that there is a
	 * new line right before it.
	 * If the line is too big (>2000), it raises an exception.
	 *
	 * @param pFc			The file channel
	 * @param pOffset	The line offset to read from
	 * @return				The string with the line contents
	 * @throws Exception
	 */
    public static String readStringAt(FileChannel pFc, long pOffset, int pCharsToRead) throws Exception {
        return readStringAt(pFc, pOffset, pCharsToRead, GeneralConstants.UTF8);
    }

    /**
	 * Reads a line from a file channel from the specified offset.
	 * It checks the consistency in the provided offset: if it bigger than zero, it checks that there is a
	 * new line right before it.
	 * If the line is too big (>2000), it raises an exception.
	 *
	 * @param pFc			The file channel
	 * @param pOffset	The line offset to read from
	 * @return				The string with the line contents
	 * @throws Exception
	 */
    public static String readStringAt(FileChannel pFc, long pOffset, int pCharsToRead, String pEncoding) throws Exception {
        if (pFc.size() < pOffset + pCharsToRead) {
            pCharsToRead = (int) (pFc.size() - pOffset);
        }
        ByteBuffer bb = ByteBuffer.allocate(pCharsToRead);
        long posToStartReading = pOffset;
        if (posToStartReading < 0) {
            posToStartReading = 0;
        }
        pFc.read(bb, posToStartReading);
        String rawBytes = new String(bb.array(), pEncoding);
        return rawBytes;
    }

    /**
	 * Cleans the data file from orphans.
	 *
	 * @param pDataFile		The output file to compact
	 * @param pIndexFile	The index file
	 * @throws Exception
	 */
    public static void removeOrphansFromDataFile(String pDataFile, String pIndexFile) throws Exception {
        Map<String, Long> index = (Map<String, Long>) ObjectUtils.readObjectFromFile(pIndexFile);
        index = removeOrphansFromDataFile(pDataFile, index);
        ObjectUtils.persistObjectToFile(index, pIndexFile);
    }

    /**
	 * Cleans the data file from orphans.
	 *
	 * @param pDataFile	The output file to compact
	 * @param pIndex	The map that acts as an index for the data file
	 */
    public static Map<String, Long> removeOrphansFromDataFile(String pDataFile, Map<String, Long> pIndex) throws Exception {
        System.out.println("# removeOrphansFromDataFile() ...");
        FileChannel fcOldFile = null;
        FileChannel fcNewFile = null;
        try {
            String originalDataFileName = pDataFile;
            String oldDataFileName = pDataFile + ".old";
            String newDataFileName = pDataFile + ".new";
            File f1 = new File(originalDataFileName);
            File f2 = new File(oldDataFileName);
            f1.renameTo(f2);
            fcOldFile = openFileChannelForReading(oldDataFileName);
            fcNewFile = openFileChannelForAppend(newDataFileName);
            Map<String, Long> newIndex = new HashMap<String, Long>();
            System.out.println("\t ... ordering records");
            List<String> orderedIndex = new ArrayList<String>(pIndex.keySet());
            Collections.sort(orderedIndex);
            System.out.println("\t ... done ordering. Saving the new data file ...");
            String record = null;
            long originalFileOffset = -1L;
            long newFileOffset = -1L;
            String indexCode = null;
            for (int i = 0; i < orderedIndex.size(); i++) {
                indexCode = orderedIndex.get(i);
                originalFileOffset = pIndex.get(indexCode);
                record = readLineAt(fcOldFile, originalFileOffset);
                newFileOffset = appendLine(fcNewFile, record);
                newIndex.put(indexCode, newFileOffset);
            }
            System.out.println("\t ... Num elements in the file : " + orderedIndex.size());
            fcNewFile.close();
            fcOldFile.close();
            f1 = new File(newDataFileName);
            f2 = new File(originalDataFileName);
            f1.renameTo(f2);
            f1 = new File(oldDataFileName);
            f1.deleteOnExit();
            f1.delete();
            return newIndex;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                fcNewFile.close();
                fcOldFile.close();
            } catch (Exception ignoredException) {
            } finally {
                System.out.println("# removeOrphansFromDataFile() - DONE !!");
            }
        }
    }
}

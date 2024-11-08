package lib.io;

import java.io.*;
import java.util.*;

/**
 * Responsible for providing a log file that text can be written to.
 * The main feature of this log file is that the size is limited. 
 * 
 * @author Paul Austen
 *
 */
public class TextLogFile {

    File logFile;

    boolean rollAround;

    int maxLogFileSize = 1048536;

    String header;

    BufferedWriter bw;

    int reductionRatio = 3;

    private boolean addTimeStamps;

    private String timeStampPrefix;

    /**
   * Constructor
   * @param logFile The log file.
   * @param maxLogFileSize The max size of the log file in bytes.
   * @param header The header to write to the log file.
   * @param append If true the file will be appended to. If false it will be overwritten.
   * @param rollAround If true then when the max size is reached the older data will be lost.
   * @param addTimeStamps If true then each message added to the log file will be prefixed with a time stamp to denote when it was added to the log file.
   * @param timeStampPrefix If the above is true then this text will be added in front (to the left of) the time stamp.
   */
    public TextLogFile(File logFile, int maxLogFileSize, String header, boolean append, boolean rollAround, boolean addTimeStamps, String timeStampPrefix) {
        this.logFile = logFile;
        this.maxLogFileSize = maxLogFileSize;
        this.header = header;
        this.rollAround = rollAround;
        this.addTimeStamps = addTimeStamps;
        this.timeStampPrefix = timeStampPrefix;
        if (logFile.isFile() && !append) {
            logFile.delete();
        }
    }

    /**
   * Append text to the log file.
   * 
   * @param theText The text to be added.
   * @throws IOException
   */
    public synchronized void append(String theText) throws IOException {
        if (theText == null || theText.length() < 1) {
            return;
        }
        if (logFile.isFile()) {
            if (maxLogFileSize > 0 && ((logFile.length() + theText.length()) >= maxLogFileSize)) {
                if (rollAround) {
                    chopFileDisk();
                } else {
                    throw new IOException("Log file size is at maximum.");
                }
            }
        }
        bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("" + logFile, true));
            if (header != null && header.length() > 0) {
                bw.write(header);
                bw.newLine();
                bw.newLine();
                header = null;
            }
            if (addTimeStamps) {
                for (int i = 0; i < theText.length(); i++) {
                    bw.write(theText.charAt(i));
                    if (theText.charAt(i) == '\n') {
                        bw.write(timeStampPrefix + " " + new Date() + " : ");
                    }
                }
            } else {
                bw.write(theText);
            }
            bw.flush();
        } catch (IOException ex) {
            if (ex.getMessage().endsWith("not enough space on the disk") && rollAround) {
                if (rollAround) {
                    chopFile();
                    append(theText);
                } else {
                    throw new IOException("Log file size is at maximum.");
                }
            } else {
                throw ex;
            }
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
   * Chop down the size of the file
   * Uses ram as the interim storage for the file. Therefore it may use a very
   * large chunk of ram but will work when no disk space is available
   * 
   * @throws IOException
   */
    private void chopFile() throws IOException {
        long fileLength = logFile.length();
        RandomAccessFile raf = null;
        BufferedOutputStream bos = null;
        if (fileLength < 1) {
            return;
        }
        try {
            raf = new RandomAccessFile(logFile, "r");
            raf.seek(fileLength / reductionRatio);
            byte readBuffer[] = new byte[(int) (fileLength - (fileLength / reductionRatio))];
            raf.read(readBuffer);
            raf.close();
            bos = new BufferedOutputStream(new FileOutputStream(logFile));
            bos.write(readBuffer);
        } catch (OutOfMemoryError ex) {
            try {
                chopFileDisk();
            } catch (IOException EX) {
                throw new IOException("Unable to reduce the size of the " + logFile.getName() + " file. Disk full and not enough ram.");
            }
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
   * Chop down the size of the file
   * Uses the disk as the interim storage buffer
   * Therefore will work with very large files but only if enough disk space is
   * available
   * 
   * @throws IOException
   */
    private void chopFileDisk() throws IOException {
        File tempFile = new File("" + logFile + ".tmp");
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        long startCopyPos;
        byte readBuffer[] = new byte[2048];
        int readCount;
        long totalBytesRead = 0;
        if (reductionRatio > 0 && logFile.length() > 0) {
            startCopyPos = logFile.length() / reductionRatio;
        } else {
            startCopyPos = 0;
        }
        try {
            bis = new BufferedInputStream(new FileInputStream(logFile));
            bos = new BufferedOutputStream(new FileOutputStream(tempFile));
            do {
                readCount = bis.read(readBuffer, 0, readBuffer.length);
                if (readCount > 0) {
                    totalBytesRead += readCount;
                    if (totalBytesRead > startCopyPos) {
                        bos.write(readBuffer, 0, readCount);
                    }
                }
            } while (readCount > 0);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException ex) {
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ex) {
                }
            }
        }
        if (tempFile.isFile()) {
            if (!logFile.delete()) {
                throw new IOException("Error when attempting to delete the " + logFile + " file.");
            }
            if (!tempFile.renameTo(logFile)) {
                throw new IOException("Error when renaming the " + tempFile + " to " + logFile + ".");
            }
        }
    }

    /**
   * Return the name of the log file.
   */
    public String toString() {
        return logFile.toString();
    }

    /**
   * Set the reduction ratio. This defines the amount the log file will be reduced by
   * when it reaches the max size.
   * 
   * @param reductionRatio The reduction ratio.
   */
    public void setReductionRatio(int reductionRatio) {
        this.reductionRatio = reductionRatio;
    }
}

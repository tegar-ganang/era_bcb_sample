package org.dbe.composer.wfengine.bpel.server.logging;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import org.dbe.composer.wfengine.SdlException;
import org.dbe.composer.wfengine.bpel.server.engine.IProcessLogger;
import org.dbe.composer.wfengine.bpel.server.engine.SdlEngineFactory;
import org.dbe.composer.wfengine.util.SdlCloser;
import org.dbe.composer.wfengine.util.SdlUnsynchronizedCharArrayWriter;
import org.dbe.composer.wfengine.util.SdlUtil;

/**
 * File based logging. Writes the contents of the process log to a file once the
 * process completes.
 */
public class SdlFileLogger extends ServiceInMemoryProcessLogger {

    /** max size of a log permitted to be kept in memory before being written to disk. */
    private static final int MAX_LOG_SIZE = 1024 * 128;

    /**
     * Ctor takes a map of options of the config file.
     */
    public SdlFileLogger(Map aConfig) {
        super(aConfig);
        String value = (String) aConfig.get("DeleteFilesOnStartup");
        boolean deleteFileOnStartup = true;
        if (!SdlUtil.isNullOrEmpty(value)) deleteFileOnStartup = Boolean.valueOf(value).booleanValue();
        if (deleteFileOnStartup) {
            deleteLogFiles();
        }
    }

    /**
     * Deletes the existing log files on startup. This is only called during construction
     * of the logger if the "DeleteFilesOnStartup" entry is set to "true" in the config
     * or if not present at all.
     */
    protected void deleteLogFiles() {
        File dir = new File(SdlEngineFactory.getSdlEngineConfig().getLoggingBaseDir(), "process-logs/");
        File[] files = dir.listFiles(new FileFilter() {

            public boolean accept(File aFile) {
                return aFile.isFile() && aFile.getName().endsWith(".log");
            }
        });
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
    }

    /**
     * Reads the file into a String
     * @param raf
     * @throws IOException
     */
    protected String read(RandomAccessFile raf) throws IOException {
        StringBuffer log = new StringBuffer((int) raf.length());
        String line = null;
        while ((line = raf.readLine()) != null) {
            log.append(line);
            log.append('\n');
        }
        return log.toString();
    }

    /**
     * Writes the contents of the process's log to a file and removes it from the
     * buffer map.
     */
    protected void closeLog(long aPid) throws IOException {
        writeToFile(aPid);
        getBufferMap().remove(aPid);
    }

    /**
     * Gets the file for the process's log.
     * @param aPid
     */
    protected File getFile(long aPid) throws IOException {
        File file = new File(SdlEngineFactory.getSdlEngineConfig().getLoggingBaseDir(), "process-logs/" + aPid + ".log");
        file.getParentFile().mkdirs();
        return file;
    }

    public String getAbbreviatedLog(long aProcessId) throws Exception {
        writeToFile(aProcessId);
        File file = getFile(aProcessId);
        if (file.length() < (1024 * 512)) {
            return readLogFileIntoString(file);
        } else {
            return readAbbreviatedLog(file);
        }
    }

    /**
     * Reads the head and tail of the log file. The amount of lines for the head
     * and tail is determined by the engine config.
     * @param aFile
     */
    private String readAbbreviatedLog(File aFile) throws IOException {
        SdlUnsynchronizedCharArrayWriter writer = new SdlUnsynchronizedCharArrayWriter();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(aFile, "r");
            int headLimit = SdlEngineFactory.getSdlEngineConfig().getIntegerEntry("Logging.Head", IProcessLogger.DEFAULT_HEAD);
            int tailLimit = SdlEngineFactory.getSdlEngineConfig().getIntegerEntry("Logging.Tail", IProcessLogger.DEFAULT_TAIL);
            read(raf, writer, headLimit);
            boolean moved = seekToTail(raf, tailLimit);
            if (moved) {
                writer.write(SNIP);
            }
            read(raf, writer, Integer.MAX_VALUE);
        } finally {
            SdlCloser.close(raf);
        }
        return writer.toString();
    }

    /**
     * Moves the file pointer to where we need to be to read the tail portion
     * of the log.
     * @param aRandomAccessFile
     * @param aTailLimit
     * @throws IOException
     */
    private boolean seekToTail(RandomAccessFile aRandomAccessFile, int aTailLimit) throws IOException {
        boolean moved = false;
        long sweetspot = aRandomAccessFile.length() - (aTailLimit * 120);
        if (sweetspot > aRandomAccessFile.getFilePointer()) {
            aRandomAccessFile.seek(sweetspot);
            aRandomAccessFile.readLine();
            moved = true;
        }
        return moved;
    }

    /**
     * Reads at most aLimit number of chars from the file and writes them to the
     * writer
     * @param aRandomAccessFile
     * @param aWriter
     * @param aLimit
     * @throws IOException
     */
    private void read(RandomAccessFile aRandomAccessFile, SdlUnsynchronizedCharArrayWriter aWriter, int aLimit) throws IOException {
        String line;
        int count = 0;
        while (count++ < aLimit && (line = aRandomAccessFile.readLine()) != null) {
            aWriter.write(line);
            aWriter.write('\n');
        }
    }

    /**
     * Returns the whole log as a string
     */
    private String readLogFileIntoString(File aFile) {
        Reader reader = null;
        SdlUnsynchronizedCharArrayWriter writer = new SdlUnsynchronizedCharArrayWriter();
        try {
            reader = new FileReader(aFile);
            char[] buff = new char[1024 * 128];
            int read;
            while ((read = reader.read(buff)) != -1) writer.write(buff, 0, read);
        } catch (IOException e) {
            SdlCloser.close(reader);
        }
        return writer.toString();
    }

    public Reader getFullLog(long aProcessId) throws Exception {
        writeToFile(aProcessId);
        return new FileReader(getFile(aProcessId));
    }

    /**
     * Writes the contents of the log's memory buffer to disk. This is done when
     * the process closes, buffer gets too big, or if someone is requesting the abbreviated or
     * full log.
     * @param aProcessId
     * @throws IOException
     */
    protected void writeToFile(long aProcessId) throws IOException {
        StringBuffer sb = getBuffer(aProcessId, false);
        if (sb != null) {
            synchronized (sb) {
                Writer w = null;
                try {
                    w = new FileWriter(getFile(aProcessId), true);
                    w.write(sb.toString());
                    sb.setLength(0);
                } finally {
                    SdlCloser.close(w);
                }
            }
        }
    }

    protected void appendToLog(long aPid, String line) {
        if (!SdlUtil.isNullOrEmpty(line)) {
            StringBuffer sb = getBuffer(aPid, true);
            synchronized (sb) {
                sb.append(line);
                sb.append('\n');
                if (sb.length() > MAX_LOG_SIZE) {
                    try {
                        writeToFile(aPid);
                    } catch (IOException e) {
                        SdlException.logError(e, "error writing log to file");
                    }
                }
            }
        }
    }
}

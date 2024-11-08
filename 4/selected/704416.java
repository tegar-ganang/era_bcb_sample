package com.od.jtimeseries.server.serialization;

import com.od.jtimeseries.timeseries.impl.RoundRobinTimeSeries;
import com.od.jtimeseries.util.logging.LogMethods;
import com.od.jtimeseries.util.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: nick
 * Date: 17-May-2009
 * Time: 22:14:28
 *
 * Handles reading and writing time series data to/from the filesystem
 *
 * nb. The order of taking locks should be
 * - lock on FileHeader
 * - RoundRobinSerializer internal lock
 *
 * Locks on FileHeader are required since some values (e.g properties) are set outside the serializer, and we need to make
 * state changes/state reads across FileHeader field changes atomic
 *
 * In addition to FileHeader locking there is currently a RoundRobinSerializer internal lock for both read and write operations
 * In theory, per file locking might be sufficient, but the locking would need to be strengthened to guarantee a
 * max of one FileHeader instance per series on disk
 */
public class RoundRobinSerializer extends AbstractLockedSerializer {

    private static final LogMethods logMethods = LogUtils.getLogMethods(RoundRobinSerializer.class);

    private static boolean shutdownHandlingDisabled;

    private final File rootDirectory;

    private final String timeSeriesFileSuffix;

    private SerializerOperations serializerOperations = new SerializerOperations();

    public RoundRobinSerializer(File rootDirectory, String timeSeriesFileSuffix) {
        this.rootDirectory = rootDirectory;
        this.timeSeriesFileSuffix = timeSeriesFileSuffix;
        checkRootDirectory(rootDirectory);
        addShutdownHook();
    }

    private void checkRootDirectory(File rootDirectory) {
        if (!rootDirectory.canWrite()) {
            logMethods.error("Timeseries Directory at " + rootDirectory.getAbsolutePath() + " does not exist or is not writable, cannot start server");
            throw new RuntimeException("Timeseries Directory at " + rootDirectory.getAbsolutePath() + " does not exist or is not writable, cannot start server");
        }
    }

    protected void doWriteSeries(FileHeader fileHeader, RoundRobinTimeSeries t) throws SerializationException {
        fileRewriteCounter.incrementCount();
        byte[] properties = fileHeader.getPropertiesAsByteArray();
        int newHeaderLength = getNewHeaderLength(fileHeader, properties);
        int head = t.size() == 0 ? -1 : 0;
        int tail = t.size();
        fileHeader.updateHeaderFields(newHeaderLength, head, tail, t.getMaxSize(), t.getLatestTimestamp());
        File f = getFile(fileHeader);
        AuditedFileChannel b = null;
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(f, "rw");
            b = new AuditedFileChannel(r.getChannel(), fileBytesWritten, fileBytesRead);
            serializerOperations.writeHeader(fileHeader, properties, b);
            serializerOperations.writeBody(t, b);
        } catch (Throwable e) {
            logMethods.error("Failed to write time series file " + f);
            fileErrorCounter.incrementCount();
            throw new SerializationException("Failed to write time series file " + f, e);
        } finally {
            flushAndClose(f, b, r);
        }
    }

    protected RoundRobinTimeSeries doReadSeries(FileHeader fileHeader) throws SerializationException {
        fileReadCounter.incrementCount();
        synchronized (readWriteLock) {
            File f = getFile(fileHeader);
            RandomAccessFile r = null;
            AuditedFileChannel c = null;
            try {
                r = new RandomAccessFile(f, "r");
                c = new AuditedFileChannel(r.getChannel(), fileBytesWritten, fileBytesRead);
                serializerOperations.readHeader(fileHeader, c);
                return serializerOperations.readBody(fileHeader, c);
            } catch (Throwable e) {
                fileErrorCounter.incrementCount();
                throw new SerializationException("Failed to deserialize file " + fileHeader, e);
            } finally {
                flushAndClose(f, c, r);
            }
        }
    }

    protected FileHeader doReadHeader(File f) throws SerializationException {
        FileHeader h = new FileHeader();
        readHeaderImpl(h, f);
        return h;
    }

    protected boolean doFileExists(FileHeader fileHeader) {
        boolean result = false;
        File f = null;
        try {
            f = getFile(fileHeader);
            result = f.exists();
        } catch (SerializationException e) {
            logMethods.error("Error checking file exists " + f, e);
        }
        return result;
    }

    protected void doReadHeader(FileHeader fileHeader) throws SerializationException {
        File f = getFile(fileHeader);
        if (!f.exists()) {
            throw new SerializationException("File for header " + fileHeader + " does not exist");
        } else if (!f.canRead()) {
            throw new SerializationException("File for header " + fileHeader + " is not readable");
        }
        readHeaderImpl(fileHeader, f);
    }

    protected void doMigratePath(FileHeader header, String newPath) throws SerializationException {
        File f = getFile(header);
        checkFileWriteable(f);
        File dest = getFileName(newPath);
        if (dest.exists()) {
            throw new SerializationException("Cannot migrate file " + f + " to path " + dest + " which already exists");
        }
        f.renameTo(dest);
        header.setPath(newPath);
        writeHeaderProperties(header);
    }

    protected void doWriteHeaderProperties(FileHeader header) throws SerializationException {
        if (header.isPropertiesRewriteRequired()) {
            doAppendToSeries(header, new RoundRobinTimeSeries(0));
        }
    }

    protected void doAppendToSeries(FileHeader header, RoundRobinTimeSeries l) throws SerializationException {
        fileAppendCounter.incrementCount();
        boolean rewriteProperties = header.isPropertiesRewriteRequired();
        if (!shutdown && (l.size() > 0 || rewriteProperties)) {
            File file = getFile(header);
            checkFileWriteable(file);
            RandomAccessFile r = null;
            AuditedFileChannel c = null;
            try {
                r = new RandomAccessFile(file, "rw");
                c = new AuditedFileChannel(r.getChannel(), fileBytesWritten, fileBytesRead);
                if (rewriteProperties) {
                    byte[] properties = header.getPropertiesAsByteArray();
                    int newHeaderLength = getNewHeaderLength(header, properties);
                    if (newHeaderLength > header.getHeaderLength()) {
                        RoundRobinTimeSeries s = serializerOperations.readBody(header, c);
                        s.addAll(l);
                        writeSeries(header, s);
                    } else {
                        serializerOperations.doAppend(header, l, true, properties, c);
                    }
                } else {
                    serializerOperations.doAppend(header, l, false, null, c);
                }
            } catch (Throwable e) {
                fileErrorCounter.incrementCount();
                throw new SerializationException("Failed to append items to file " + header, e);
            } finally {
                flushAndClose(file, c, r);
            }
        }
    }

    protected File doGetFile(FileHeader f) throws SerializationException {
        String path = f.getPath();
        if (path == null) {
            throw new SerializationException("Cannot get File for FileHeader with null context path");
        }
        return getFileName(path);
    }

    private File getFileName(String pathToEncode) throws SerializationException {
        try {
            String fileName = URLEncoder.encode(pathToEncode, "UTF-8") + timeSeriesFileSuffix;
            return new File(rootDirectory, fileName);
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException("Failed to encode file name", e);
        }
    }

    protected File doCreateFile(FileHeader fileHeader) throws SerializationException {
        RoundRobinTimeSeries r = new RoundRobinTimeSeries(fileHeader.getSeriesMaxLength());
        writeSeries(fileHeader, r);
        return getFile(fileHeader);
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    private int getNewHeaderLength(FileHeader fileHeader, byte[] properties) {
        int headerBytesToWrite = properties.length + SerializerOperations.PROPERTIES_OFFSET;
        return fileHeader.calculateNewHeaderLength(headerBytesToWrite);
    }

    private void readHeaderImpl(FileHeader fileHeader, File f) throws SerializationException {
        fileHeaderReadCounter.incrementCount();
        RandomAccessFile r = null;
        AuditedFileChannel c = null;
        try {
            r = new RandomAccessFile(f, "r");
            c = new AuditedFileChannel(r.getChannel(), fileBytesWritten, fileBytesRead);
            serializerOperations.readHeader(fileHeader, c);
        } catch (Throwable e) {
            fileErrorCounter.incrementCount();
            throw new SerializationException("Failed to deserialize header " + fileHeader, e);
        } finally {
            flushAndClose(f, c, r);
        }
    }

    private void checkFileWriteable(File file) throws SerializationException {
        if (!file.canWrite()) {
            String error = "Cannot write to file " + file + ". This file no longer exists or is not writeable";
            logMethods.error(error);
            throw new SerializationException(error);
        }
    }

    private void addShutdownHook() {
        if (!shutdownHandlingDisabled) {
            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run() {
                    logMethods.info("Shutdown Starting");
                    shutdownNow();
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    logMethods.info("Shutdown complete");
                }
            });
        }
    }

    private void shutdownNow() {
        synchronized (readWriteLock) {
            shutdown = true;
        }
    }

    /**
     * Safely close a RandomAccessFile and associated channel, forcing channel changes to disk
     */
    private void flushAndClose(File f, AuditedFileChannel c, RandomAccessFile r) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                logMethods.error("Error closing file channel " + f, e);
            }
        }
        if (r != null) {
            try {
                r.close();
            } catch (IOException e) {
                logMethods.error("Error closing file " + f, e);
            }
        }
    }

    public static void setShutdownHandlingDisabled(boolean shutdownHandlingDisabled) {
        RoundRobinSerializer.shutdownHandlingDisabled = shutdownHandlingDisabled;
    }
}

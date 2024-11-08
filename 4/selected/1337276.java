package gnu.saw.server.filetransfer;

import gnu.saw.SAW;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.martiansoftware.jsap.CommandLineTokenizer;

public class SAWFileTransferServerTransfer implements Runnable {

    private static final int fileTransferBufferSize = 8 * 1024;

    private volatile boolean stopped;

    private volatile boolean finished;

    private volatile boolean compression;

    private volatile boolean resume;

    private volatile boolean check;

    private volatile boolean verified;

    private int bytesRead;

    private int remoteFileStatus;

    private int localFileStatus;

    private int remoteFileAccess;

    private int localFileAccess;

    private long remoteChecksum;

    private long localChecksum;

    private long remoteFileSize;

    private long localFileSize;

    private long maxOffsets;

    private long readOffsets;

    private final byte[] fileTransferBuffer = new byte[fileTransferBufferSize];

    private CRC32 crc32 = new CRC32();

    private String command;

    private String[] splitCommand;

    private File fileTransferFile;

    private File fileTransferTrueFile;

    private RandomAccessFile fileTransferRandomAccessFile;

    private CheckedInputStream checksumReader;

    private InputStream fileTransferInputStream;

    private OutputStream fileTransferOutputStream;

    private InputStream fileTransferFileInputStream;

    private OutputStream fileTransferFileOutputStream;

    private SAWFileTransferServerSession session;

    public SAWFileTransferServerTransfer(SAWFileTransferServerSession session) {
        this.session = session;
        this.finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getRemoteFileSize() {
        return remoteFileSize;
    }

    public long getLocalFileSize() {
        return localFileSize;
    }

    public long getReadOffsets() {
        return readOffsets;
    }

    private boolean getFileStatus() {
        return (writeLocalFileStatus() && readRemoteFileStatus());
    }

    private boolean getFileAccess() {
        return (writeLocalFileAccess() && readRemoteFileAccess());
    }

    private boolean getFileSizes() {
        return (writeLocalFileSize() && readRemoteFileSize());
    }

    private boolean getFileChecksums() {
        return (writeLocalFileChecksum() && readRemoteFileChecksum());
    }

    private boolean writeLocalFileStatus() {
        try {
            if (fileTransferFile.exists()) {
                if (fileTransferFile.isFile()) {
                    localFileStatus = SAW.SAW_FILE_TRANSFER_FILE_IS_FILE;
                } else if (fileTransferFile.isDirectory()) {
                    localFileStatus = SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY;
                } else {
                    localFileStatus = SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN;
                }
            } else {
                localFileStatus = SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND;
            }
        } catch (Exception e) {
            localFileStatus = SAW.SAW_FILE_TRANSFER_FILE_ERROR;
        }
        try {
            session.getServer().getConnection().getFileTransferControlDataOutputStream().write(localFileStatus);
            session.getServer().getConnection().getFileTransferControlDataOutputStream().flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean writeLocalFileAccess() {
        try {
            if (fileTransferFile.exists()) {
                if (fileTransferFile.canRead()) {
                    localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_ONLY;
                    if (fileTransferFile.canWrite()) {
                        localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE;
                    }
                } else if (fileTransferFile.canWrite()) {
                    localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_WRITE_ONLY;
                } else {
                    localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_DENIED;
                }
            } else {
                if (fileTransferFile.createNewFile()) {
                    if (fileTransferFile.canRead()) {
                        localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_ONLY;
                        if (fileTransferFile.canWrite()) {
                            localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE;
                        }
                    } else if (fileTransferFile.canWrite()) {
                        localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_WRITE_ONLY;
                    } else {
                        fileTransferFile.delete();
                        localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_DENIED;
                    }
                } else {
                    localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_DENIED;
                }
            }
        } catch (Exception e) {
            localFileAccess = SAW.SAW_FILE_TRANSFER_FILE_ACCESS_ERROR;
        }
        try {
            session.getServer().getConnection().getFileTransferControlDataOutputStream().write(localFileAccess);
            session.getServer().getConnection().getFileTransferControlDataOutputStream().flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean writeLocalFileSize() {
        localFileSize = fileTransferFile.length();
        try {
            session.getServer().getConnection().getFileTransferControlDataOutputStream().writeLong(localFileSize);
            session.getServer().getConnection().getFileTransferControlDataOutputStream().flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean writeLocalFileChecksum() {
        crc32.reset();
        readOffsets = 0;
        try {
            if (checksumReader == null) {
                checksumReader = new CheckedInputStream(Channels.newInputStream(fileTransferRandomAccessFile.getChannel()), crc32);
            }
            if (remoteFileSize < localFileSize) {
                maxOffsets = remoteFileSize;
            } else {
                maxOffsets = localFileSize;
            }
            while (!stopped && maxOffsets > readOffsets) {
                if (readOffsets + fileTransferBufferSize >= maxOffsets) {
                    bytesRead = checksumReader.read(fileTransferBuffer, 0, Long.valueOf(maxOffsets - readOffsets).intValue());
                } else {
                    bytesRead = checksumReader.read(fileTransferBuffer, 0, fileTransferBufferSize);
                }
                readOffsets += bytesRead;
            }
            localChecksum = checksumReader.getChecksum().getValue();
            session.getServer().getConnection().getFileTransferControlDataOutputStream().writeLong(localChecksum);
            session.getServer().getConnection().getFileTransferControlDataOutputStream().flush();
            fileTransferRandomAccessFile.seek(0);
            if (stopped) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileStatus() {
        try {
            remoteFileStatus = session.getServer().getConnection().getFileTransferControlDataInputStream().read();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileAccess() {
        try {
            remoteFileAccess = session.getServer().getConnection().getFileTransferControlDataInputStream().read();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileSize() {
        try {
            remoteFileSize = session.getServer().getConnection().getFileTransferControlDataInputStream().readLong();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileChecksum() {
        try {
            remoteChecksum = session.getServer().getConnection().getFileTransferControlDataInputStream().readLong();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryUpload() {
        try {
            fileTransferRandomAccessFile = new RandomAccessFile(fileTransferFile, "r");
            fileTransferRandomAccessFile.getChannel().lock();
        } catch (Exception e) {
        }
        if (verifyUpload()) {
            return (setUploadStreams() && uploadFileData());
        }
        return false;
    }

    private boolean verifyUpload() {
        verified = true;
        try {
            if (getFileStatus()) {
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    verified = false;
                }
                if (verified && getFileAccess()) {
                    if (!(remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_WRITE_ONLY)) {
                        verified = false;
                    }
                    if (!(localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_ONLY)) {
                        verified = false;
                    }
                    localFileSize = 0;
                    remoteFileSize = 0;
                    readOffsets = 0;
                    if (verified && getFileSizes()) {
                        if (resume) {
                            resume = false;
                            if (localFileSize >= remoteFileSize && remoteFileSize >= 0) {
                                if (getFileChecksums()) {
                                    if (remoteFileStatus != SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND && localChecksum == remoteChecksum) {
                                        resume = true;
                                        readOffsets = remoteFileSize;
                                    } else {
                                    }
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean setUploadStreams() {
        try {
            if (compression) {
                fileTransferOutputStream = new GZIPOutputStream(session.getServer().getConnection().getFileTransferDataOutputStream());
            } else {
                fileTransferOutputStream = session.getServer().getConnection().getFileTransferDataOutputStream();
            }
            if (resume) {
                fileTransferRandomAccessFile.seek(remoteFileSize);
            }
            fileTransferFileInputStream = Channels.newInputStream(fileTransferRandomAccessFile.getChannel());
            return true;
        } catch (Exception e) {
            try {
                fileTransferOutputStream.close();
            } catch (Exception e1) {
            }
            try {
                fileTransferFileInputStream.close();
            } catch (Exception e1) {
            }
            return false;
        }
    }

    private boolean uploadFileData() {
        try {
            while (!stopped && readOffsets < localFileSize) {
                if (localFileSize - readOffsets > fileTransferBufferSize) {
                    bytesRead = fileTransferFileInputStream.read(fileTransferBuffer, 0, fileTransferBufferSize);
                } else {
                    bytesRead = fileTransferFileInputStream.read(fileTransferBuffer, 0, Long.valueOf(localFileSize - readOffsets).intValue());
                }
                if (bytesRead == -1) {
                    break;
                } else {
                    fileTransferOutputStream.write(fileTransferBuffer, 0, bytesRead);
                    fileTransferOutputStream.flush();
                }
                readOffsets += bytesRead;
            }
            fileTransferOutputStream.close();
            if (!stopped) {
                if (check) {
                    fileTransferRandomAccessFile.seek(0);
                    remoteFileSize = localFileSize;
                    if (getFileChecksums()) {
                        if (localChecksum != remoteChecksum) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                fileTransferOutputStream.close();
            } catch (Exception e1) {
            }
            try {
                fileTransferFileInputStream.close();
            } catch (Exception e1) {
            }
        }
    }

    private boolean tryDownload() {
        try {
            fileTransferRandomAccessFile = new RandomAccessFile(fileTransferFile, "rw");
            fileTransferRandomAccessFile.getChannel().lock();
        } catch (Exception e) {
        }
        if (verifyDownload()) {
            return (setDownloadStreams() && downloadFileData());
        } else {
            return false;
        }
    }

    private boolean verifyDownload() {
        verified = true;
        try {
            if (getFileStatus()) {
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND) {
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    verified = false;
                }
                if (verified && getFileAccess()) {
                    if (!(remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_ONLY)) {
                        verified = false;
                    }
                    if (!(localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_WRITE_ONLY)) {
                        verified = false;
                    }
                    localFileSize = 0;
                    remoteFileSize = 0;
                    readOffsets = 0;
                    if (verified && getFileSizes()) {
                        if (resume) {
                            resume = false;
                            if (remoteFileSize >= localFileSize && localFileSize >= 0) {
                                if (getFileChecksums()) {
                                    if (localFileStatus != SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND && localChecksum == remoteChecksum) {
                                        resume = true;
                                        readOffsets = localFileSize;
                                    } else {
                                    }
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean setDownloadStreams() {
        try {
            if (compression) {
                fileTransferInputStream = new GZIPInputStream(session.getServer().getConnection().getFileTransferDataInputStream());
            } else {
                fileTransferInputStream = session.getServer().getConnection().getFileTransferDataInputStream();
            }
            if (resume) {
                fileTransferRandomAccessFile.seek(localFileSize);
            }
            fileTransferFileOutputStream = Channels.newOutputStream(fileTransferRandomAccessFile.getChannel());
            return true;
        } catch (Exception e) {
            try {
                fileTransferFileOutputStream.close();
            } catch (Exception e1) {
            }
            return false;
        }
    }

    private boolean downloadFileData() {
        try {
            while (readOffsets < remoteFileSize) {
                if (remoteFileSize - readOffsets > fileTransferBufferSize) {
                    bytesRead = fileTransferInputStream.read(fileTransferBuffer, 0, fileTransferBufferSize);
                } else {
                    bytesRead = fileTransferInputStream.read(fileTransferBuffer, 0, Long.valueOf(remoteFileSize - readOffsets).intValue());
                }
                if (bytesRead == -1) {
                    break;
                } else {
                    fileTransferFileOutputStream.write(fileTransferBuffer, 0, bytesRead);
                    fileTransferFileOutputStream.flush();
                }
                readOffsets += bytesRead;
            }
            if (fileTransferFile.length() >= 0) {
                if (!stopped) {
                    if (check) {
                        fileTransferRandomAccessFile.seek(0);
                        localFileSize = fileTransferFile.length();
                        if (getFileChecksums()) {
                            if (localChecksum == remoteChecksum) {
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                    fileTransferFileOutputStream.close();
                    fileTransferTrueFile = new File(splitCommand[3]);
                    if (!fileTransferTrueFile.isAbsolute()) {
                        fileTransferTrueFile = new File(session.getServer().getSession().getWorkingDirectory(), splitCommand[3]);
                    }
                    if (!fileTransferFile.renameTo(fileTransferTrueFile)) {
                        if (fileTransferTrueFile.delete()) {
                            return fileTransferFile.renameTo(fileTransferTrueFile);
                        } else {
                            return false;
                        }
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                fileTransferFileOutputStream.close();
            } catch (Exception e1) {
            }
        }
    }

    public void run() {
        try {
            splitCommand = CommandLineTokenizer.tokenize(command);
            if (splitCommand.length != 4) {
                finished = true;
                return;
            }
            if (splitCommand[0].equalsIgnoreCase("*SAWFILETRANSFER")) {
                if (splitCommand[1].toUpperCase().contains("P")) {
                    fileTransferFile = new File(splitCommand[3] + ".tmp");
                    if (!fileTransferFile.isAbsolute()) {
                        fileTransferFile = new File(session.getServer().getSession().getWorkingDirectory(), splitCommand[3] + ".tmp");
                    }
                    compression = false;
                    resume = false;
                    check = false;
                    if (splitCommand[1].toUpperCase().contains("C")) {
                        compression = true;
                    }
                    if (splitCommand[1].toUpperCase().contains("R")) {
                        resume = true;
                    }
                    if (splitCommand[1].toUpperCase().contains("V")) {
                        check = true;
                    }
                    if (tryDownload()) {
                    } else {
                    }
                } else if (splitCommand[1].toUpperCase().contains("G")) {
                    fileTransferFile = new File(splitCommand[2]);
                    if (!fileTransferFile.isAbsolute()) {
                        fileTransferFile = new File(session.getServer().getSession().getWorkingDirectory(), splitCommand[2]);
                    }
                    compression = false;
                    resume = false;
                    check = false;
                    if (splitCommand[1].toUpperCase().contains("C")) {
                        compression = true;
                    }
                    if (splitCommand[1].toUpperCase().contains("R")) {
                        resume = true;
                    }
                    if (splitCommand[1].toUpperCase().contains("V")) {
                        check = true;
                    }
                    if (tryUpload()) {
                    } else {
                    }
                } else {
                }
            } else {
            }
        } catch (Exception e) {
        }
        if (fileTransferRandomAccessFile != null) {
            try {
                fileTransferRandomAccessFile.close();
            } catch (Exception e) {
            }
        }
        fileTransferRandomAccessFile = null;
        if (checksumReader != null) {
            try {
                checksumReader.close();
            } catch (Exception e) {
            }
        }
        checksumReader = null;
        finished = true;
    }
}

package gnu.saw.client.filetransfer;

import gnu.saw.SAW;
import gnu.saw.terminal.SAWTerminal;
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

public class SAWFileTransferClientTransfer implements Runnable {

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

    private SAWFileTransferClientSession session;

    public SAWFileTransferClientTransfer(SAWFileTransferClientSession session) {
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
            session.getClient().getConnection().getFileTransferControlDataOutputStream().write(localFileStatus);
            session.getClient().getConnection().getFileTransferControlDataOutputStream().flush();
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
            session.getClient().getConnection().getFileTransferControlDataOutputStream().write(localFileAccess);
            session.getClient().getConnection().getFileTransferControlDataOutputStream().flush();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean writeLocalFileSize() {
        localFileSize = fileTransferFile.length();
        try {
            session.getClient().getConnection().getFileTransferControlDataOutputStream().writeLong(localFileSize);
            session.getClient().getConnection().getFileTransferControlDataOutputStream().flush();
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
            session.getClient().getConnection().getFileTransferControlDataOutputStream().writeLong(localChecksum);
            session.getClient().getConnection().getFileTransferControlDataOutputStream().flush();
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
            remoteFileStatus = session.getClient().getConnection().getFileTransferControlDataInputStream().read();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileAccess() {
        try {
            remoteFileAccess = session.getClient().getConnection().getFileTransferControlDataInputStream().read();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileSize() {
        try {
            remoteFileSize = session.getClient().getConnection().getFileTransferControlDataInputStream().readLong();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean readRemoteFileChecksum() {
        try {
            remoteChecksum = session.getClient().getConnection().getFileTransferControlDataInputStream().readLong();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryUpload() {
        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Trying to send file...\nSAW>");
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
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file not found!\nSAW>");
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file is a directory!\nSAW>");
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file is of unknown type!\nSAW>");
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Error found while analyzing local file!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file is a directory!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file is of unknown type!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Error found while analyzing remote file!\nSAW>");
                    verified = false;
                }
                if (verified && getFileAccess()) {
                    if (!(localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_ONLY)) {
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file is not readable!\nSAW>");
                        verified = false;
                    }
                    if (!(remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_WRITE_ONLY)) {
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file is not writable!\nSAW>");
                        verified = false;
                    }
                    localFileSize = 0;
                    remoteFileSize = 0;
                    readOffsets = 0;
                    if (verified && getFileSizes()) {
                        if (resume) {
                            resume = false;
                            if (localFileSize >= remoteFileSize && remoteFileSize >= 0) {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:Checking resume possibility...\nSAW>");
                                if (getFileChecksums()) {
                                    if (remoteFileStatus != SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND && localChecksum == remoteChecksum) {
                                        resume = true;
                                        readOffsets = remoteFileSize;
                                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Resume possible!\nSAW>");
                                    } else {
                                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Resume not possible!\nSAW>");
                                    }
                                }
                            }
                        }
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file size: '" + localFileSize + "' bytes\nSAW>");
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
                fileTransferOutputStream = new GZIPOutputStream(session.getClient().getConnection().getFileTransferDataOutputStream());
            } else {
                fileTransferOutputStream = session.getClient().getConnection().getFileTransferDataOutputStream();
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
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Verifying file integrity...\nSAW>");
                    fileTransferRandomAccessFile.seek(0);
                    remoteFileSize = localFileSize;
                    if (getFileChecksums()) {
                        if (localChecksum == remoteChecksum) {
                            SAWTerminal.print("\nSAW>SAWFILETRANSFER:File integrity verified!\nSAW>");
                        } else {
                            SAWTerminal.print("\nSAW>SAWFILETRANSFER:File integrity corrupted!\nSAW>");
                            return false;
                        }
                    } else {
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Cannot verify file integrity!\nSAW>");
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
        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Trying to receive file...\nSAW>");
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
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file is a directoy!\nSAW>");
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file is of unknown type!\nSAW>");
                    verified = false;
                }
                if (localFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Error found while analyzing local file!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file not found!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_DIRECTORY) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file is a directory!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_IS_UNKNOWN) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file is of unknown type!\nSAW>");
                    verified = false;
                }
                if (remoteFileStatus == SAW.SAW_FILE_TRANSFER_FILE_ERROR) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Error found while analyzing remote file!\nSAW>");
                    verified = false;
                }
                if (verified && getFileAccess()) {
                    if (!(localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || localFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_WRITE_ONLY)) {
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Local file is not writable!\nSAW>");
                        verified = false;
                    }
                    if (!(remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_AND_WRITE || remoteFileAccess == SAW.SAW_FILE_TRANSFER_FILE_ACCESS_READ_ONLY)) {
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file is not readable!\nSAW>");
                        verified = false;
                    }
                    localFileSize = 0;
                    remoteFileSize = 0;
                    readOffsets = 0;
                    if (verified && getFileSizes()) {
                        if (resume) {
                            resume = false;
                            if (remoteFileSize >= localFileSize && localFileSize >= 0) {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:Checking resume possibility...\nSAW>");
                                if (getFileChecksums()) {
                                    if (localFileStatus != SAW.SAW_FILE_TRANSFER_FILE_NOT_FOUND && localChecksum == remoteChecksum) {
                                        resume = true;
                                        readOffsets = localFileSize;
                                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Resume possible!\nSAW>");
                                    } else {
                                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Resume not possible!\nSAW>");
                                    }
                                }
                            }
                        }
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Remote file size: '" + remoteFileSize + "' bytes\nSAW>");
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
                fileTransferInputStream = new GZIPInputStream(session.getClient().getConnection().getFileTransferDataInputStream());
            } else {
                fileTransferInputStream = session.getClient().getConnection().getFileTransferDataInputStream();
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
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Verifying file integrity...\nSAW>");
                        fileTransferRandomAccessFile.seek(0);
                        localFileSize = fileTransferFile.length();
                        if (getFileChecksums()) {
                            if (localChecksum == remoteChecksum) {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:File integrity verified!\nSAW>");
                            } else {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:File integrity corrupted!\nSAW>");
                                return false;
                            }
                        } else {
                            SAWTerminal.print("\nSAW>SAWFILETRANSFER:Cannot verify file integrity!\nSAW>");
                            return false;
                        }
                    }
                    fileTransferFileOutputStream.close();
                    fileTransferTrueFile = new File(splitCommand[3]);
                    if (!fileTransferTrueFile.isAbsolute()) {
                        fileTransferTrueFile = new File(session.getClient().getSession().getWorkingDirectory(), splitCommand[3]);
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
                synchronized (this) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Syntax error detected!\nSAW>");
                    finished = true;
                    return;
                }
            }
            if (splitCommand[0].equalsIgnoreCase("*SAWFILETRANSFER")) {
                if (splitCommand[1].toUpperCase().contains("P")) {
                    fileTransferFile = new File(splitCommand[2]);
                    if (!fileTransferFile.isAbsolute()) {
                        fileTransferFile = new File(session.getClient().getSession().getWorkingDirectory(), splitCommand[2]);
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
                        synchronized (this) {
                            if (!stopped) {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:File transfer completed!\nSAW>");
                            } else {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:File transfer interrupted!\nSAW>");
                            }
                            finished = true;
                        }
                    } else {
                        synchronized (this) {
                            SAWTerminal.print("\nSAW>SAWFILETRANSFER:File transfer failed!\nSAW>");
                            finished = true;
                        }
                    }
                } else if (splitCommand[1].toUpperCase().contains("G")) {
                    fileTransferFile = new File(splitCommand[3] + ".tmp");
                    if (!fileTransferFile.isAbsolute()) {
                        fileTransferFile = new File(session.getClient().getSession().getWorkingDirectory(), splitCommand[3] + ".tmp");
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
                        synchronized (this) {
                            if (!stopped) {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:File transfer completed!\nSAW>");
                            } else {
                                SAWTerminal.print("\nSAW>SAWFILETRANSFER:File transfer interrupted!\nSAW>");
                            }
                            finished = true;
                        }
                    } else {
                        synchronized (this) {
                            SAWTerminal.print("\nSAW>SAWFILETRANSFER:File transfer failed!\nSAW>");
                            finished = true;
                        }
                    }
                } else {
                    synchronized (this) {
                        SAWTerminal.print("\nSAW>SAWFILETRANSFER:Syntax error detected!\nSAW>");
                        finished = true;
                    }
                }
            } else {
                synchronized (this) {
                    SAWTerminal.print("\nSAW>SAWFILETRANSFER:Syntax error detected!\nSAW>");
                    finished = true;
                }
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

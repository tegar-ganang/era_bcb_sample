package cu.ftpd.commands.transfer;

import cu.ftpd.Server;
import cu.ftpd.Connection;
import cu.ftpd.ServiceManager;
import cu.ftpd.events.Event;
import cu.shell.ProcessResult;
import cu.ftpd.logging.Logging;
import cu.ftpd.filesystem.FileSystem;
import cu.ftpd.filesystem.Section;
import cu.ftpd.filesystem.filters.ForbiddenFilesFilter;
import cu.ftpd.filesystem.permissions.SpeedPermission;
import cu.ftpd.filesystem.permissions.ActionPermission;
import cu.ftpd.user.User;
import cu.ftpd.user.UserPermission;
import java.nio.channels.FileLock;
import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.util.zip.CheckedOutputStream;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * @author Markus Jevring <markus@jevring.net>
 * @since 2008-jan-08 - 21:13:17
 * @version $Id: CommandSTOR.java 300 2010-03-09 20:48:19Z jevring $
 */
public class CommandSTOR implements TransferController {

    private TransferThread transfer;

    private FileLock lock;

    private boolean semaphoreTaken = false;

    private FileOutputStream fos;

    private Socket dataConnection;

    private RandomAccessFile raf;

    private final Connection connection;

    private File file;

    private final FileSystem fs;

    private final User user;

    private Section section;

    private final String filename;

    private final boolean append;

    private final boolean encryptedDataConnection;

    private final boolean sscn;

    private final CRC32 crc = new CRC32();

    private final boolean onTheFlyCrc;

    private InetAddress remoteHost;

    private final boolean fastAsciiTransfer;

    public CommandSTOR(Connection connection, FileSystem fs, User user, String filename, boolean append, boolean encryptedDataConnection, boolean sscn, boolean onTheFlyCrc, boolean fastAsciiTransfer) {
        this.connection = connection;
        this.fs = fs;
        this.user = user;
        this.filename = filename;
        this.append = append;
        this.encryptedDataConnection = encryptedDataConnection;
        this.sscn = sscn;
        this.onTheFlyCrc = onTheFlyCrc;
        this.fastAsciiTransfer = fastAsciiTransfer;
    }

    public void start() {
        connection.dataConnectionSemaphore.acquireUninterruptibly();
        try {
            dataConnection = connection.getDataConnection();
            remoteHost = dataConnection.getInetAddress();
            if ((semaphoreTaken = Server.getInstance().getUploadSemaphore().tryAcquire()) || user.hasPermission(UserPermission.PRIVILEGED)) {
                try {
                    if (filename != null) {
                        file = fs.resolveFile(filename);
                    } else {
                        file = fs.createUniqueFile(user.getUsername());
                    }
                    String ftpPathToFile = FileSystem.resolvePath(file);
                    if (ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.UPLOAD, ftpPathToFile, user)) {
                        if (!ForbiddenFilesFilter.getForbiddenFiles().contains(file.getName())) {
                            if (file.getParentFile().canWrite()) {
                                if (!file.exists() || hasModifyPermission(file)) {
                                    if (remoteHost != null) {
                                        if (remoteHost.equals(connection.getClientHost()) || ServiceManager.getServices().getPermissions().hasPermission(ActionPermission.FXPUPLOAD, ftpPathToFile, user)) {
                                            Event event = new Event(Event.UPLOAD, user, fs.getRealParentWorkingDirectoryPath(), fs.getFtpParentWorkingDirectory());
                                            event.setProperty("transfer.state", "PENDING");
                                            event.setProperty("transfer.bytesTransferred", "0");
                                            event.setProperty("transfer.timeTaken", "0");
                                            event.setProperty("file.path.real", file.getAbsolutePath());
                                            event.setProperty("file.path.ftp", ftpPathToFile);
                                            event.setProperty("filesystem.transfer.type", fs.getType());
                                            event.setProperty("connection.remote.host", remoteHost.getHostAddress());
                                            boolean proceed = ServiceManager.getServices().getEventHandler().handleBeforeEvent(event, connection);
                                            if (proceed) {
                                                section = fs.getSection(file);
                                                ServiceManager.getServices().getMetadataHandler().setOwnership(file, user.getUsername(), user.getPrimaryGroup());
                                                connection.setControlConnectionTimeout(0);
                                                createTransfer();
                                                if (filename == null) {
                                                    connection.respond("150 FILE: " + file.getName());
                                                } else {
                                                    connection.respond("150 Opening " + fs.getType() + " mode data connection for STOR command" + (encryptedDataConnection ? " with SSL/TLS encryption." : "."));
                                                }
                                                transfer.start();
                                            } else {
                                                close();
                                            }
                                        } else {
                                            close();
                                            connection.respond("531 You do not have permission to upload to another host than the one you are connecting from (FXPUPLOAD)");
                                        }
                                    } else {
                                        close();
                                        connection.respond("500 Remote host unknown, probably due to a closed data connection, transfer failed.");
                                    }
                                } else {
                                    close();
                                    connection.fileExists();
                                }
                            } else {
                                close();
                                connection.respond("553 Not allowed to upload in a write-protected directory.");
                            }
                        } else {
                            close();
                            connection.respond("531 Forbidden filename.");
                        }
                    } else {
                        close();
                        connection.respond("531 Permission denied.");
                    }
                } catch (FileNotFoundException e) {
                    close();
                    connection.respond("500 Error creating file: " + FileSystem.resolvePath(file));
                } catch (IOException e) {
                    close();
                    connection.respond("500 " + e.getMessage());
                }
            } else {
                close();
                connection.respond("553 Too many users uploading at the moment.");
            }
        } catch (IllegalStateException e) {
            close();
            connection.respond("500 " + e.getMessage());
        }
    }

    private void createTransfer() throws IOException {
        if (encryptedDataConnection) {
            ((javax.net.ssl.SSLSocket) dataConnection).setUseClientMode(sscn);
        }
        long limit = ServiceManager.getServices().getPermissions().getLimit(user, FileSystem.resolvePath(file.getParentFile()), SpeedPermission.UPLOAD);
        if (fs.getOffset() > 0) {
            raf = new RandomAccessFile(file, "rw");
            takeLock(raf);
            raf.seek(fs.getOffset());
            if (limit <= 0) {
                transfer = new RandomAccessFileTransferThread(this, dataConnection.getInputStream(), raf);
            } else {
                transfer = new SpeedLimitedRandomAccessFileTransferThread(this, dataConnection.getInputStream(), raf, limit);
            }
        } else {
            fos = new FileOutputStream(file, append);
            takeLock(fos);
            if ("ASCII".equals(fs.getType())) {
                if (fastAsciiTransfer) {
                    transfer = new CharacterTransferThread(this, new InputStreamReader(dataConnection.getInputStream(), "ISO-8859-1"), new BufferedWriter(new OutputStreamWriter(fos, "ISO-8859-1")));
                } else {
                    transfer = new TranslatingCharacterTransferThread(this, new BufferedReader(new InputStreamReader(dataConnection.getInputStream(), "ISO-8859-1")), new BufferedWriter(new OutputStreamWriter(fos, "ISO-8859-1")), true);
                }
            } else {
                OutputStream out = fos;
                if (!append && onTheFlyCrc) {
                    out = new CheckedOutputStream(fos, crc);
                }
                if (limit <= 0) {
                    transfer = new ByteTransferThread(this, dataConnection.getInputStream(), out);
                } else {
                    transfer = new SpeedLimitingByteTransferThread(this, dataConnection.getInputStream(), out, limit);
                }
            }
        }
    }

    public void error(Exception e, long bytesTransferred, long transferTime) {
        close();
        ProcessResult pcr = postProcess(bytesTransferred, transferTime);
        connection.reportTransferFailure(e.getMessage());
        Event event = createEvent(bytesTransferred, transferTime, pcr, "FAILED");
        ServiceManager.getServices().getEventHandler().handleAfterEvent(event);
    }

    private Event createEvent(long bytesTransferred, long transferTime, ProcessResult pcr, String state) {
        Event event = new Event(Event.UPLOAD, user, fs.getRealParentWorkingDirectoryPath(), fs.getFtpParentWorkingDirectory());
        event.setProperty("transfer.state", state);
        event.setProperty("transfer.bytesTransferred", String.valueOf(bytesTransferred));
        event.setProperty("transfer.timeTaken", String.valueOf(transferTime));
        event.setProperty("transfer.postprocessor.exitvalue", String.valueOf(pcr.exitvalue));
        event.setProperty("transfer.postprocessor.message", pcr.message);
        event.setProperty("file.path.real", file.getAbsolutePath());
        event.setProperty("file.path.ftp", FileSystem.resolvePath(file));
        event.setProperty("filesystem.transfer.type", fs.getType());
        event.setProperty("connection.remote.host", (remoteHost == null ? "n/a - connection failure" : remoteHost.getHostAddress()));
        return event;
    }

    public void complete(long bytesTransferred, long transferTime) {
        close();
        ProcessResult pcr = postProcess(bytesTransferred, transferTime);
        connection.reply(226, pcr.message, true);
        connection.respond("226- " + fs.getType() + " transfer of " + filename + " complete.");
        connection.statline(transfer.getSpeed());
        Event event = createEvent(bytesTransferred, transferTime, pcr, "COMPLETE");
        ServiceManager.getServices().getEventHandler().handleAfterEvent(event);
    }

    private ProcessResult postProcess(long bytesTransferred, long transferTime) {
        ProcessResult pcr = ServiceManager.getServices().getTransferPostProcessor().process(file, crc.getValue(), bytesTransferred, transferTime, user, section.getName(), transfer.getSpeed());
        if (pcr.exitvalue == ProcessResult.OK.exitvalue) {
            log(bytesTransferred, transferTime);
        } else {
            file.delete();
        }
        return pcr;
    }

    private void log(long bytesTransferred, long transferTime) {
        ServiceManager.getServices().getUserStatistics().upload(user.getUsername(), section.getName(), bytesTransferred, transferTime);
        if (!user.hasLeech()) {
            user.giveCredits(bytesTransferred * section.getRatio());
        }
    }

    private void close() {
        if (semaphoreTaken) {
            Server.getInstance().getUploadSemaphore().release();
        }
        fs.rest(0);
        if (lock != null && lock.isValid()) {
            try {
                lock.release();
            } catch (IOException e) {
                Logging.getErrorLog().reportException("Failed to release lock", e);
            }
        }
        if (dataConnection != null) {
            try {
                dataConnection.close();
            } catch (IOException e) {
                Logging.getErrorLog().reportException("Failed to close data connection", e);
            }
        }
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                Logging.getErrorLog().reportException("Failed to close output stream", e);
            }
        }
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                Logging.getErrorLog().reportException("Failed to close random access file", e);
            }
        }
        connection.resetControlConnectionTimeout();
        connection.dataConnectionSemaphore.release();
    }

    private void takeLock(FileOutputStream out) throws IOException {
        try {
            lock = out.getChannel().tryLock();
        } catch (java.nio.channels.OverlappingFileLockException e) {
        }
        if (lock == null) {
            throw new IOException("File is already being uploaded: Could not acquire exclusive lock on file");
        }
    }

    private void takeLock(RandomAccessFile raf) throws IOException {
        try {
            lock = raf.getChannel().tryLock();
        } catch (java.nio.channels.OverlappingFileLockException e) {
        }
        if (lock == null) {
            throw new IOException("File is already being uploaded: Could not acquire exclusive lock on file");
        }
    }

    /**
     * Determines if the current user is allowed to modify the file in question.
     * This modification is context-dependent, meaning that in this case, we examine if we are allowed to resume or delete the file as appropriate.
     * @param file the file for which we want to examine the permission
     * @return true if the user has the right to modify it, false otherwise.
     */
    private boolean hasModifyPermission(File file) {
        int permission;
        if (fs.isOwner(user, file)) {
            if (fs.getOffset() > 0 || append) {
                permission = ActionPermission.RESUMEOWN;
            } else {
                permission = ActionPermission.DELETEOWN;
            }
        } else {
            if (fs.getOffset() > 0 || append) {
                permission = ActionPermission.RESUME;
            } else {
                permission = ActionPermission.DELETE;
            }
        }
        return ServiceManager.getServices().getPermissions().hasPermission(permission, FileSystem.resolvePath(file), user);
    }

    public boolean isRunning() {
        return transfer != null && transfer.isAlive();
    }

    public long getSpeed() {
        if (transfer != null) {
            return transfer.getCurrentSpeed();
        } else {
            return 0;
        }
    }
}

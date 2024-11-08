package netblend.master.slaves;

import static netblend.NetBlendProtocol.BAD_PATH_ERROR;
import static netblend.NetBlendProtocol.CANCEL_TRANSFER_COMMAND;
import static netblend.NetBlendProtocol.DATA_CHUNK_SIZE;
import static netblend.NetBlendProtocol.DELETE_OUTPUT_FILE_COMMAND;
import static netblend.NetBlendProtocol.DELETE_SOURCE_FILE_COMMAND;
import static netblend.NetBlendProtocol.DOWNLOAD_OUTPUT_FILE_COMMAND;
import static netblend.NetBlendProtocol.END_OF_DATA_RESPONSE;
import static netblend.NetBlendProtocol.LIST_OUTPUT_FILES_COMMAND;
import static netblend.NetBlendProtocol.LIST_SOURCE_FILES_COMMAND;
import static netblend.NetBlendProtocol.OK_RESPONSE;
import static netblend.NetBlendProtocol.SHUTDOWN_SERVER_COMMAND;
import static netblend.NetBlendProtocol.TRANSFER_CANCELLED_RESPONSE;
import static netblend.NetBlendProtocol.UPLOAD_SOURCE_FILE_COMMAND;
import static netblend.NetBlendSystem.hashFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import netblend.master.MasterMain;
import netblend.master.files.FileInformation;

public class FilesConnection {

    /**
	 * Command results.
	 */
    public enum Result {

        /**
		 * Indicates that the command finished successfully.
		 */
        OK, /**
		 * Indicates that the command was cancelled successfully.
		 */
        CANCELLED, /**
		 * Indicates that the command failed.
		 */
        FAILED
    }

    private FilesActivity activityStatus = FilesActivity.IDLE;

    private boolean cancelled = false;

    private int progress = 0;

    private SlaveHandler parent;

    private DataOutputStream dataOut;

    private DataInputStream dataIn;

    /**
	 * Creates a new connection with the specified streams.
	 * 
	 * @param dataIn
	 *            input from the remote server.
	 * @param dataOut
	 *            output to the remote server.
	 */
    public FilesConnection(SlaveHandler parent, DataInputStream dataIn, DataOutputStream dataOut) {
        this.parent = parent;
        this.dataIn = dataIn;
        this.dataOut = dataOut;
    }

    /**
	 * Attempts to shutdown the server.
	 */
    public synchronized void issueShutdown() {
        try {
            dataOut.writeInt(SHUTDOWN_SERVER_COMMAND);
            dataIn.close();
            dataOut.close();
        } catch (IOException e) {
            parent.disconnectFromFiles();
        }
    }

    /**
	 * Attempts to break this connection to the slave server.
	 */
    public void disconnect() {
        try {
            dataIn.close();
        } catch (IOException e) {
        }
        try {
            dataOut.close();
        } catch (IOException e) {
        }
    }

    private void changeProgress(int i) {
        progress = i;
        MasterMain.getInstance().fireSlaveProgressChanged(parent, progress);
    }

    /**
	 * Returns the progress of the latest command.
	 * 
	 * @return the progress of the latest command.
	 */
    public int getProgress() {
        return progress;
    }

    /**
	 * Gets the current activity status of the connection
	 * 
	 * @return the status of the connection.
	 */
    public FilesActivity getActivityStatus() {
        return activityStatus;
    }

    /**
	 * Lists the files in the upload/source directory.
	 * 
	 * @return a list of files in the server's upload directory.
	 */
    public synchronized FileInformation[] getSourceFileList() {
        return getFileList(LIST_SOURCE_FILES_COMMAND);
    }

    /**
	 * Lists the files in the download/output directory.
	 * 
	 * @return a list of files in the server's output directory.
	 */
    public synchronized FileInformation[] getOuputFileList() {
        return getFileList(LIST_OUTPUT_FILES_COMMAND);
    }

    /**
	 * Obtains a file list.
	 * 
	 * @param command
	 *            which file listing command is to be used.
	 * @return the list of files.
	 */
    private synchronized FileInformation[] getFileList(int command) {
        activityStatus = FilesActivity.LISTING_FILES;
        cancelled = false;
        changeProgress(0);
        TreeSet<FileInformation> tree = new TreeSet<FileInformation>();
        try {
            int count;
            int dataLength;
            byte[] bytes;
            String filename;
            boolean isDirectory;
            long fileSize;
            long modified;
            dataOut.writeInt(command);
            count = dataIn.readInt();
            dataLength = dataIn.readInt();
            int done = 0;
            while (dataLength != END_OF_DATA_RESPONSE) {
                bytes = new byte[dataLength];
                dataIn.readFully(bytes);
                filename = new String(bytes);
                isDirectory = dataIn.readBoolean();
                fileSize = dataIn.readLong();
                modified = dataIn.readLong();
                tree.add(new FileInformation(isDirectory, filename, fileSize, modified));
                done++;
                changeProgress((100 * done) / count);
                if (cancelled) {
                    dataOut.writeInt(CANCEL_TRANSFER_COMMAND);
                    break;
                } else {
                    dataOut.writeInt(OK_RESPONSE);
                }
                dataLength = dataIn.readInt();
            }
            int result = dataIn.readInt();
            if (result == OK_RESPONSE) {
                FileInformation[] array = new FileInformation[tree.size()];
                Iterator<FileInformation> e = tree.iterator();
                for (int i = 0; e.hasNext(); i++) array[i] = e.next();
                changeProgress(100);
                activityStatus = FilesActivity.IDLE;
                return array;
            }
        } catch (IOException e) {
            changeProgress(0);
            parent.disconnectFromFiles();
            return null;
        }
        activityStatus = FilesActivity.IDLE;
        return null;
    }

    /**
	 * Uploads a file to this slave server.
	 * 
	 * @param file
	 *            the file to upload.
	 * @return the status returned by the command.
	 */
    public synchronized Result sendFile(File file) {
        activityStatus = FilesActivity.UPLOADING;
        cancelled = false;
        changeProgress(0);
        if (file.exists()) {
            String fileName = file.getName();
            long fileSize = file.length();
            try {
                dataOut.writeInt(UPLOAD_SOURCE_FILE_COMMAND);
                dataOut.writeInt(fileName.length());
                dataOut.writeBytes(fileName);
                if (dataIn.readInt() == BAD_PATH_ERROR) {
                    activityStatus = FilesActivity.IDLE;
                    return Result.FAILED;
                }
                dataOut.writeLong(file.lastModified());
                BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
                byte[] fileBytes;
                int chunkToWrite;
                int read;
                final int MAX_CHUNK = DATA_CHUNK_SIZE;
                for (long bytesLeft = fileSize; bytesLeft > 0; bytesLeft -= MAX_CHUNK) {
                    changeProgress(100 - (int) ((100 * bytesLeft) / fileSize));
                    chunkToWrite = (int) ((bytesLeft > MAX_CHUNK) ? MAX_CHUNK : bytesLeft);
                    dataOut.writeLong(chunkToWrite);
                    fileBytes = new byte[chunkToWrite];
                    read = fileIn.read(fileBytes);
                    while ((read != -1) && (chunkToWrite > 0)) {
                        dataOut.write(fileBytes, 0, read);
                        chunkToWrite -= read;
                        fileBytes = new byte[chunkToWrite];
                        read = fileIn.read(fileBytes);
                    }
                    if (cancelled) break;
                }
                fileIn.close();
                if (cancelled) {
                    dataOut.writeInt(CANCEL_TRANSFER_COMMAND);
                    changeProgress(0);
                } else {
                    dataOut.writeInt(END_OF_DATA_RESPONSE);
                    changeProgress(100);
                }
                int response = dataIn.readInt();
                activityStatus = FilesActivity.IDLE;
                if (response == OK_RESPONSE) return Result.OK; else if (response == TRANSFER_CANCELLED_RESPONSE) return Result.CANCELLED;
            } catch (IOException e) {
                changeProgress(0);
                parent.disconnectFromFiles();
            }
        }
        activityStatus = FilesActivity.IDLE;
        return Result.FAILED;
    }

    /**
	 * Downloads a file from this server.
	 * 
	 * @param remoteFileName
	 *            the name of the file to download.
	 * @param localFile
	 *            the file to download into.
	 * @return the status returned by the command.
	 */
    public synchronized Result receiveFile(String remoteFileName, File localFile) {
        activityStatus = FilesActivity.DOWNLOADING;
        cancelled = false;
        changeProgress(0);
        File tempFile;
        DataOutputStream fileOut;
        try {
            tempFile = File.createTempFile("nbdl", null);
            fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));
        } catch (IOException e) {
            e.printStackTrace();
            activityStatus = FilesActivity.IDLE;
            return Result.FAILED;
        }
        try {
            dataOut.writeInt(DOWNLOAD_OUTPUT_FILE_COMMAND);
            dataOut.writeInt(remoteFileName.length());
            dataOut.writeBytes(remoteFileName);
            if (dataIn.readInt() != OK_RESPONSE) {
                activityStatus = FilesActivity.IDLE;
                return Result.FAILED;
            }
            int dataLength;
            byte[] bytes;
            long fileSize = dataIn.readLong();
            long modified = dataIn.readLong();
            dataLength = dataIn.readInt();
            bytes = new byte[dataLength];
            dataIn.readFully(bytes);
            String remoteHash = new String(bytes);
            long fileWritten = 0;
            dataLength = dataIn.readInt();
            while (dataLength != END_OF_DATA_RESPONSE) {
                bytes = new byte[dataLength];
                dataIn.readFully(bytes);
                fileOut.write(bytes);
                fileWritten += dataLength;
                changeProgress((int) ((100 * fileWritten) / fileSize));
                if (cancelled) {
                    dataOut.writeInt(CANCEL_TRANSFER_COMMAND);
                    break;
                } else dataOut.writeInt(OK_RESPONSE);
                dataLength = dataIn.readInt();
            }
            fileOut.close();
            tempFile.setLastModified(modified);
            activityStatus = FilesActivity.IDLE;
            int result = dataIn.readInt();
            if (result == OK_RESPONSE) {
                String tempHash = hashFile(tempFile);
                if (tempHash.equals(remoteHash)) {
                    if (localFile.exists()) localFile.delete();
                    boolean done = tempFile.renameTo(localFile);
                    tempFile.delete();
                    if (done) return Result.OK; else return Result.FAILED;
                } else return Result.FAILED;
            } else if (result == TRANSFER_CANCELLED_RESPONSE) {
                return Result.CANCELLED;
            }
        } catch (IOException e) {
            changeProgress(0);
            parent.disconnectFromFiles();
        }
        activityStatus = FilesActivity.IDLE;
        return Result.FAILED;
    }

    /**
	 * Raises a flag to cancel the most recent action releasing any locks held
	 * by the running method.
	 */
    public void cancelAction() {
        cancelled = true;
    }

    /**
	 * Deletes the specified source file on the remote machine.
	 * 
	 * @param fileName
	 *            the file to delete.
	 * @return the status returned by the command.
	 */
    public synchronized Result deleteSourceFile(String fileName) {
        return deleteFile(DELETE_SOURCE_FILE_COMMAND, fileName);
    }

    /**
	 * Deletes the specified output file on the remote machine.
	 * 
	 * @param fileName
	 *            the file to delete.
	 * @return the status returned by the command.
	 */
    public synchronized Result deleteOutputFile(String fileName) {
        return deleteFile(DELETE_OUTPUT_FILE_COMMAND, fileName);
    }

    public synchronized Result deleteFile(int command, String fileName) {
        activityStatus = FilesActivity.DELETING;
        cancelled = false;
        changeProgress(0);
        try {
            dataOut.writeInt(command);
            dataOut.writeInt(fileName.length());
            dataOut.writeBytes(fileName);
            if (dataIn.readInt() != OK_RESPONSE) {
                activityStatus = FilesActivity.IDLE;
                return Result.FAILED;
            }
        } catch (IOException e) {
            activityStatus = FilesActivity.IDLE;
            changeProgress(0);
            parent.disconnectFromFiles();
            return Result.FAILED;
        }
        changeProgress(100);
        activityStatus = FilesActivity.IDLE;
        return Result.OK;
    }
}

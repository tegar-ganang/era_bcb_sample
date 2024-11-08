package netblend.slave.files;

import static netblend.NetBlendProtocol.BAD_PATH_ERROR;
import static netblend.NetBlendProtocol.CANCEL_TRANSFER_COMMAND;
import static netblend.NetBlendProtocol.DATA_CHUNK_SIZE;
import static netblend.NetBlendProtocol.DELETE_FAILED_ERROR;
import static netblend.NetBlendProtocol.DELETE_OUTPUT_FILE_COMMAND;
import static netblend.NetBlendProtocol.DELETE_SOURCE_FILE_COMMAND;
import static netblend.NetBlendProtocol.DISCONNECT_FROM_SERVER_COMMAND;
import static netblend.NetBlendProtocol.DOWNLOAD_OUTPUT_FILE_COMMAND;
import static netblend.NetBlendProtocol.END_OF_DATA_RESPONSE;
import static netblend.NetBlendProtocol.FILE_NOT_FOUND_ERROR;
import static netblend.NetBlendProtocol.FILE_READ_ERROR;
import static netblend.NetBlendProtocol.FILE_WRITE_ERROR;
import static netblend.NetBlendProtocol.LIST_OUTPUT_FILES_COMMAND;
import static netblend.NetBlendProtocol.LIST_SOURCE_FILES_COMMAND;
import static netblend.NetBlendProtocol.OK_RESPONSE;
import static netblend.NetBlendProtocol.SHUTDOWN_SERVER_COMMAND;
import static netblend.NetBlendProtocol.TRANSFER_CANCELLED_RESPONSE;
import static netblend.NetBlendProtocol.UPLOAD_SOURCE_FILE_COMMAND;
import static netblend.NetBlendSystem.OUTPUT_PATH;
import static netblend.NetBlendSystem.SOURCE_PATH;
import static netblend.NetBlendSystem.hashFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import netblend.slave.SlaveMain;

public class FileServerConnection extends Thread {

    protected Socket socket;

    protected DataInputStream dataIn;

    protected DataOutputStream dataOut;

    /**
	 * Creates a new connection handler.
	 * 
	 * @param socket
	 *            the socket on which this connection communicates.
	 * @param dataIn
	 *            the input stream for incoming data.
	 * @param dataOut
	 *            the output stream for outgoing data.
	 */
    public FileServerConnection(Socket socket, DataInputStream dataIn, DataOutputStream dataOut) {
        super("FileServerConnection");
        this.socket = socket;
        this.dataIn = dataIn;
        this.dataOut = dataOut;
    }

    @Override
    public void run() {
        int command;
        try {
            do {
                command = dataIn.readInt();
                switch(command) {
                    case LIST_SOURCE_FILES_COMMAND:
                        listFilesCommand(SOURCE_PATH + File.separator);
                        break;
                    case LIST_OUTPUT_FILES_COMMAND:
                        listFilesCommand(OUTPUT_PATH + File.separator);
                        break;
                    case UPLOAD_SOURCE_FILE_COMMAND:
                        uploadFileCommand();
                        break;
                    case DOWNLOAD_OUTPUT_FILE_COMMAND:
                        downloadFileCommand();
                        break;
                    case DELETE_SOURCE_FILE_COMMAND:
                        deleteFileCommand(SOURCE_PATH);
                        break;
                    case DELETE_OUTPUT_FILE_COMMAND:
                        deleteFileCommand(OUTPUT_PATH);
                        break;
                    case DISCONNECT_FROM_SERVER_COMMAND:
                        dataIn.close();
                        dataOut.close();
                        socket.close();
                        System.out.println("Diconnected.");
                        return;
                    case SHUTDOWN_SERVER_COMMAND:
                        SlaveMain.shutdown();
                        return;
                    default:
                        System.err.println("Error: Bad request: " + command);
                        socket.close();
                        return;
                }
            } while (command != DISCONNECT_FROM_SERVER_COMMAND);
        } catch (IOException e) {
            System.out.println("Connection lost.");
        }
    }

    /**
	 * Receives an uploading file.
	 * 
	 * @throws IOException
	 */
    private void uploadFileCommand() throws IOException {
        int dataLength = dataIn.readInt();
        byte[] bytes = new byte[dataLength];
        dataIn.readFully(bytes);
        String fileName = new String(bytes);
        if ((fileName.indexOf("..") != -1) || (fileName.indexOf(":") != -1) || fileName.startsWith("/") || fileName.startsWith("\\")) {
            dataOut.writeInt(BAD_PATH_ERROR);
            return;
        }
        File file = new File(SOURCE_PATH + File.separator + fileName);
        DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        dataOut.writeInt(OK_RESPONSE);
        boolean fileWriteError = false;
        boolean cancelled = false;
        long totalSize = 0;
        long modified = dataIn.readLong();
        dataLength = dataIn.readInt();
        while (dataLength != -1 && dataLength != END_OF_DATA_RESPONSE && dataLength != CANCEL_TRANSFER_COMMAND) {
            bytes = new byte[dataLength];
            dataIn.readFully(bytes);
            try {
                fileOut.write(bytes);
            } catch (IOException e) {
                fileWriteError = true;
            }
            totalSize += dataLength;
            dataLength = dataIn.readInt();
        }
        if (dataLength == CANCEL_TRANSFER_COMMAND) cancelled = true;
        if (fileWriteError) {
            System.out.println("ERROR: File upload failed: " + fileName);
            dataOut.writeInt(FILE_WRITE_ERROR);
        } else if (cancelled) {
            System.out.println("File upload cancelled: " + fileName);
            dataOut.writeInt(TRANSFER_CANCELLED_RESPONSE);
        } else {
            System.out.println("File uploaded: " + fileName + " (" + totalSize + " bytes)");
            dataOut.writeInt(OK_RESPONSE);
        }
        fileOut.close();
        if (cancelled) file.delete(); else file.setLastModified(modified);
    }

    /**
	 * Transmits a file for download.
	 * 
	 * @throws IOException
	 */
    private void downloadFileCommand() throws IOException {
        int dataLength = dataIn.readInt();
        byte[] bytes = new byte[dataLength];
        dataIn.readFully(bytes);
        String fileName = new String(bytes);
        if ((fileName.indexOf("..") != -1) || (fileName.indexOf(":") != -1) || fileName.startsWith("/") || fileName.startsWith("\\")) {
            dataOut.writeInt(BAD_PATH_ERROR);
            return;
        }
        File file = new File(OUTPUT_PATH + File.separator + fileName);
        if (!file.exists() || file.isDirectory()) {
            dataOut.writeInt(FILE_NOT_FOUND_ERROR);
            return;
        }
        BufferedInputStream fileIn;
        try {
            fileIn = new BufferedInputStream(new FileInputStream(file));
        } catch (Exception e) {
            dataOut.writeInt(FILE_READ_ERROR);
            return;
        }
        dataOut.writeInt(OK_RESPONSE);
        long fileSize = file.length();
        dataOut.writeLong(fileSize);
        dataOut.writeLong(file.lastModified());
        String hash = hashFile(file);
        dataOut.writeInt(hash.length());
        dataOut.writeBytes(hash);
        boolean cancelled = false;
        byte[] fileBytes;
        int chunkToWrite;
        int read;
        final int MAX_CHUNK = DATA_CHUNK_SIZE;
        for (long bytesLeft = fileSize; bytesLeft > 0; bytesLeft -= MAX_CHUNK) {
            chunkToWrite = (int) ((bytesLeft > MAX_CHUNK) ? MAX_CHUNK : bytesLeft);
            dataOut.writeInt(chunkToWrite);
            fileBytes = new byte[chunkToWrite];
            read = fileIn.read(fileBytes);
            while ((read != -1) && (chunkToWrite > 0)) {
                dataOut.write(fileBytes, 0, read);
                chunkToWrite -= read;
                fileBytes = new byte[chunkToWrite];
                read = fileIn.read(fileBytes);
            }
            if (dataIn.readInt() == CANCEL_TRANSFER_COMMAND) {
                cancelled = true;
                break;
            }
        }
        if (!cancelled) {
            dataOut.writeInt(END_OF_DATA_RESPONSE);
            dataOut.writeInt(OK_RESPONSE);
        } else {
            dataOut.writeInt(TRANSFER_CANCELLED_RESPONSE);
        }
        fileIn.close();
    }

    /**
	 * Deletes a file.
	 * 
	 * @throws IOException
	 */
    private void deleteFileCommand(String path) throws IOException {
        int dataLength = dataIn.readInt();
        byte[] bytes = new byte[dataLength];
        dataIn.readFully(bytes);
        String fileName = new String(bytes);
        if ((fileName.indexOf("..") != -1) || (fileName.indexOf(":") != -1) || fileName.startsWith("/") || fileName.startsWith("\\")) {
            dataOut.writeInt(BAD_PATH_ERROR);
            return;
        }
        File file = new File(path + File.separator + fileName);
        if (!file.exists() || file.isDirectory()) {
            dataOut.writeInt(FILE_NOT_FOUND_ERROR);
        } else {
            if (!file.delete()) dataOut.writeInt(DELETE_FAILED_ERROR); else {
                dataOut.writeInt(OK_RESPONSE);
                System.out.println("File deleted: " + fileName);
            }
        }
    }

    /**
	 * Lists the files in the specified path.
	 * 
	 * @throws IOException
	 */
    private void listFilesCommand(String path) throws IOException {
        File directory = new File(path);
        File[] fileList = directory.listFiles();
        boolean cancelled = false;
        if (fileList == null) {
            dataOut.writeInt(0);
        } else {
            dataOut.writeInt(fileList.length);
            for (int i = 0; i < fileList.length; i++) {
                String fileName = fileList[i].getName();
                long fileSize = fileList[i].length();
                boolean isDirectory = fileList[i].isDirectory();
                long modified = fileList[i].lastModified();
                dataOut.writeInt(fileName.length());
                dataOut.writeBytes(fileName);
                dataOut.writeBoolean(isDirectory);
                dataOut.writeLong(fileSize);
                dataOut.writeLong(modified);
                if (dataIn.readInt() == CANCEL_TRANSFER_COMMAND) {
                    cancelled = true;
                    break;
                }
            }
        }
        if (cancelled) {
            dataOut.writeInt(TRANSFER_CANCELLED_RESPONSE);
        } else {
            dataOut.writeInt(END_OF_DATA_RESPONSE);
            dataOut.writeInt(OK_RESPONSE);
        }
    }
}

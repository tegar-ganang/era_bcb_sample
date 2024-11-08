package net.sf.autoshare.data.remote.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.sf.autoshare.data.DataConstants;
import net.sf.autoshare.data.remote.Command;
import net.sf.autoshare.data.remote.CommandInterface;
import net.sf.autoshare.data.remote.DataInterface;
import net.sf.autoshare.data.remote.MaxLengthExceededException;
import net.sf.autoshare.network.Connection;
import net.sf.autoshare.network.EncryptionException;

public class SendFileCommand extends CommandImplementation {

    public static void send(Connection conn, File storagePath, String filename) throws EncryptionException, IOException {
        CommandInterface.sendCommand(Command.SEND_FILE, conn.getActiveOut());
        filename = filename.replace(File.pathSeparatorChar, '/');
        DataInterface.sendNextString(filename, conn.getActiveOut());
        File file = new File(storagePath, filename);
        InputStream fileIn = new FileInputStream(file);
        try {
            conn.getActiveOut().writeLong(file.length());
            byte[] buf = new byte[DataConstants.FILE_BUFFER_SIZE];
            int readLength;
            while ((readLength = fileIn.read(buf)) > 0) {
                conn.getActiveOut().write(buf, 0, readLength);
            }
        } finally {
            fileIn.close();
        }
        conn.flushOut();
    }

    /**
   * @return the filename
   * @throws IOException 
   * @throws MaxLengthExceededException 
   */
    public static void read(Connection conn, File storagePath) throws MaxLengthExceededException, IOException {
        String filename = DataInterface.receiveNextString(conn.getActiveIn(), DataConstants.MAX_FILE_NAME_LENGTH);
        File file = new File(storagePath, filename);
        if (file.exists()) {
            throw new IOException("File already exists");
        }
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        OutputStream fileOut = new FileOutputStream(file);
        try {
            long fileSize = conn.getActiveIn().readLong();
            byte[] buf = new byte[DataConstants.FILE_BUFFER_SIZE];
            int readLength;
            int totalReadLength = 0;
            while (true) {
                readLength = buf.length;
                if (readLength > fileSize - totalReadLength) {
                    readLength = (int) (fileSize - totalReadLength);
                }
                readLength = conn.getActiveIn().read(buf, 0, readLength);
                totalReadLength += readLength;
                fileOut.write(buf, 0, readLength);
            }
        } finally {
            fileOut.close();
        }
    }
}

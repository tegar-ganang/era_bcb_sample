package jerry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Logger class implemented as a thread called from a external listener.
 * 
 * @author (h0t@_G0|i, Alby87
 * 
 */
public class NGStatsLogger extends Thread {

    /**
	 * Local variable client connection
	 */
    private Socket clientConn;

    /**
	 * Local Client IP address variable.
	 */
    private String clientIpAddress;

    /**
	 * Local variable temporary Log directory path
	 */
    private final File logDir;

    /**
	 * Local variable UT Log directory path
	 */
    private final File utLogDir;

    /**
	 * Local variable FTP host name to upload htmls
	 */
    private final String ftpHostName;

    /**
	 * Local variable UT html directory path
	 */
    private final File htmlsDir;

    /**
	 * Local variable where is the Users Manager object
	 */
    private final UsersManager usersManager;

    /**
	 * Constructor
	 * 
	 * @param clientConn
	 * @param logDir
	 * @param utLogDir
	 * @param ftpHostName
	 * @param htmlsDir
	 */
    public NGStatsLogger(Socket clientConn, File logDir, File utLogDir, String ftpHostName, File htmlsDir, UsersManager usersManager) {
        super(clientConn.getRemoteSocketAddress().toString());
        this.clientConn = clientConn;
        this.logDir = logDir;
        this.utLogDir = utLogDir;
        this.htmlsDir = htmlsDir;
        this.ftpHostName = ftpHostName;
        this.usersManager = usersManager;
    }

    /**
	 * Thread run method
	 */
    @Override
    public void run() {
        usersManager.addThread();
        try {
            byte[] buffer = new byte[9000];
            try {
                readFiles(buffer);
            } catch (SocketException e) {
                System.out.println("client terminated");
            }
            File[] files = logDir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    if (name.contains(".log")) {
                        return true;
                    }
                    return false;
                }
            });
            for (File tempFile : files) {
                String name = ConvertUTWorldLogToSimpleText.convertFile(tempFile.getAbsolutePath(), utLogDir.getAbsolutePath());
                usersManager.reWriteFile(name);
                LogToHtmlConvertor.generateHtmls(name, htmlsDir, usersManager.getUserDBDir());
            }
            FtpUploader.uploadToFtpSite(htmlsDir, ftpHostName);
        } catch (Exception e) {
            throw new LoggerException(e);
        } finally {
            try {
                clientConn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        usersManager.deleteThread();
    }

    /**
	 * Reading data
	 * 
	 * @param clientConn
	 * @param buffer
	 *            object
	 * @throws InterruptedException
	 * @throws IOException
	 */
    private void readFiles(byte[] buffer) throws InterruptedException, IOException {
        int available = 0;
        boolean sentAck = false;
        FileOutputStream oStream = null;
        FileInputStream iStream = null;
        File tempFile = null;
        while (true) {
            Thread.sleep(100);
            available = clientConn.getInputStream().read(buffer);
            if (available == -1) {
                break;
            }
            if (available > 0) {
                if (oStream != null) {
                    byte[] writebuffer = null;
                    writebuffer = new byte[available];
                    for (int i = 0; i < writebuffer.length; i++) {
                        writebuffer[i] = buffer[i];
                    }
                    oStream.write(writebuffer);
                    oStream.flush();
                    if (available >= 9000) continue;
                }
            }
            if (buffer[0] == 'N' && buffer[1] == 'E' && buffer[2] == 'X' && buffer[3] == 'T' && buffer[4] == 'F' && buffer[5] == 'I' && buffer[6] == 'L' && buffer[7] == 'E' || readHeader(buffer, available)) {
                for (int i = 0; i < available; i++) {
                    System.out.print((char) buffer[i]);
                }
                System.out.println("");
                clientConn.getOutputStream().write(new byte[] { 'R', 'E', 'A', 'D', 'Y' }, 0, 5);
                clientConn.getOutputStream().flush();
                tempFile = File.createTempFile("temp", "");
                oStream = new FileOutputStream(tempFile);
                iStream = new FileInputStream(tempFile);
                sentAck = false;
            } else if (buffer[available - 1] == -1 && !sentAck) {
                oStream.getChannel().position(oStream.getChannel().position() - 5);
                clientConn.getOutputStream().write(new byte[] { 'A', 'C', 'K' }, 0, 3);
                clientConn.getOutputStream().flush();
                File file = File.createTempFile("Unreal.ngLog." + clientIpAddress + ".", ".log", logDir);
                FileOutputStream uncomStream = ZlibDecompressUtil.uncompressLogFile(iStream, file);
                oStream.close();
                iStream.close();
                uncomStream.close();
                tempFile.delete();
                oStream = null;
                iStream = null;
                sentAck = true;
            }
        }
    }

    /**
	 * Reading header
	 * 
	 * @param clientConn
	 * @param buffer
	 *            object
	 * @throws IOException
	 * @throws InterruptedException
	 */
    private boolean readHeader(byte[] buffer, int available) throws IOException, InterruptedException {
        String headerText = "";
        if (available > 0) {
            if (buffer[0] == 'H' && buffer[1] == 'E' && buffer[2] == 'L' && buffer[3] == 'L' && buffer[4] == 'O') {
                for (int i = 0; i < available; i++) {
                    headerText = headerText + ((char) buffer[i]);
                }
                clientIpAddress = headerText.split("\t")[4];
                return true;
            }
        }
        return false;
    }
}

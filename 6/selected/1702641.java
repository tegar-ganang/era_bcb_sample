package com.kni.etl.ketl.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.w3c.dom.Node;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.ketl.exceptions.KETLThreadException;
import com.kni.etl.ketl.smp.ETLThreadManager;
import com.kni.etl.util.ManagedFastInputChannel;
import com.kni.util.net.ftp.DefaultFTPFileListParser;
import com.kni.util.net.ftp.FTP;
import com.kni.util.net.ftp.FTPClient;
import com.kni.util.net.ftp.FTPConnectionClosedException;
import com.kni.util.net.ftp.FTPFile;
import com.kni.util.net.ftp.FTPReply;

/**
 * The Class FTPFileReader.
 */
public class FTPFileReader extends FileReader {

    @Override
    protected String getVersion() {
        return "$LastChangedRevision: 499 $";
    }

    /**
	 * Close FTP connections.
	 */
    private void closeFTPConnections() {
        if (this.ftpClients == null) {
            return;
        }
        for (Object element : this.ftpClients) {
            if (((FTPClient) element).isConnected()) {
                try {
                    ((FTPClient) element).disconnect();
                } catch (IOException f) {
                }
            }
        }
    }

    /** The USER. */
    static String USER = "USER";

    /** The PASSWORD. */
    static String PASSWORD = "PASSWORD";

    /** The TRANSFE r_ TYPE. */
    static String TRANSFER_TYPE = "TRANSFER_TYPE";

    /** The BINARY. */
    static String BINARY = "BINARY";

    /** The ASCII. */
    static String ASCII = "ASCII";

    /** The SERVER. */
    static String SERVER = "SERVER";

    /** The ftp clients. */
    private Object[] ftpClients = null;

    /** The FILENAM e_ POS. */
    static int FILENAME_POS = 0;

    /** The PARAMLIS t_ I d_ POS. */
    static int PARAMLIST_ID_POS = 1;

    /**
	 * Instantiates a new FTP file reader.
	 * 
	 * @param pXMLConfig
	 *            the XML config
	 * @param pPartitionID
	 *            the partition ID
	 * @param pPartition
	 *            the partition
	 * @param pThreadManager
	 *            the thread manager
	 * 
	 * @throws KETLThreadException
	 *             the KETL thread exception
	 */
    public FTPFileReader(Node pXMLConfig, int pPartitionID, int pPartition, ETLThreadManager pThreadManager) throws KETLThreadException {
        super(pXMLConfig, pPartitionID, pPartition, pThreadManager);
    }

    /**
	 * Gets the FTP filenames.
	 * 
	 * @param iParamList
	 *            The param list
	 * 
	 * @return the FTP filenames
	 */
    public Object[][] getFTPFilenames(int iParamList) {
        Object[][] result = null;
        boolean binaryTransfer = true;
        String searchString = this.getParameterValue(iParamList, NIOFileReader.SEARCHPATH);
        if (searchString == null) {
            return null;
        }
        String tmp = this.getParameterValue(iParamList, FTPFileReader.TRANSFER_TYPE);
        if ((tmp != null) && tmp.equalsIgnoreCase(FTPFileReader.ASCII)) {
            binaryTransfer = false;
        }
        FTPClient ftp = this.getFTPConnection(this.getParameterValue(iParamList, FTPFileReader.USER), this.getParameterValue(iParamList, FTPFileReader.PASSWORD), this.getParameterValue(iParamList, FTPFileReader.SERVER), binaryTransfer, "Directory listing connection.");
        if (ftp == null) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Could not connect to server.");
            return null;
        }
        FTPFile[] fList;
        try {
            fList = ftp.listFiles(new DefaultFTPFileListParser(), searchString);
            char pathSeperator = '\\';
            String pathName = null;
            int endOfPath = searchString.lastIndexOf(pathSeperator);
            if (endOfPath == -1) {
                pathSeperator = '/';
                endOfPath = searchString.lastIndexOf(pathSeperator);
            }
            if (endOfPath != -1) {
                pathName = searchString.substring(0, endOfPath);
            }
            if (fList != null) {
                ArrayList res = new ArrayList();
                for (FTPFile element : fList) {
                    Object[] o = new Object[2];
                    o[FTPFileReader.PARAMLIST_ID_POS] = new Integer(iParamList);
                    if (pathName != null) {
                        o[FTPFileReader.FILENAME_POS] = pathName + pathSeperator + element.getName();
                        res.add(o);
                    } else {
                        o[FTPFileReader.FILENAME_POS] = element.getName();
                        res.add(o);
                    }
                }
                if (res.size() > 0) {
                    result = new Object[res.size()][];
                    res.toArray(result);
                }
            }
        } catch (IOException e1) {
            ResourcePool.LogException(e1, this);
            ResourcePool.LogMessage(this, ResourcePool.WARNING_MESSAGE, searchString + " caused IO Exception, file will be ignored");
        }
        if (ftp.isConnected()) {
            try {
                ftp.disconnect();
            } catch (IOException f) {
            }
        }
        return result;
    }

    /** The tmp FTP clients. */
    ArrayList tmpFTPClients = new ArrayList();

    /** The connection cnt. */
    int connectionCnt = 1;

    @Override
    int getFileChannels(FileToRead[] astrPaths) throws Exception {
        int iNumPaths = 0;
        FTPClient ftp = null;
        boolean binaryTransfer = true;
        if (astrPaths == null) {
            return 0;
        }
        if (this.mAllowDuplicates == false) {
            this.maFiles = NIOFileReader.dedupFileList(this.maFiles);
        }
        for (FileToRead element : astrPaths) {
            InputStream tmpStream;
            try {
                if (ftp == null) {
                    String tmp = this.getParameterValue(element.paramListID, FTPFileReader.TRANSFER_TYPE);
                    if ((tmp != null) && tmp.equalsIgnoreCase(FTPFileReader.ASCII)) {
                        binaryTransfer = false;
                    }
                    ftp = this.getFTPConnection(this.getParameterValue(element.paramListID, FTPFileReader.USER), this.getParameterValue(element.paramListID, FTPFileReader.PASSWORD), this.getParameterValue(element.paramListID, FTPFileReader.SERVER), binaryTransfer, "Parallel connection " + ++this.connectionCnt);
                }
                if (ftp == null) {
                    ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP: Could not connect to server.");
                    if (this.tmpFTPClients.size() > 0) {
                        this.ftpClients = this.tmpFTPClients.toArray();
                        this.closeFTPConnections();
                    }
                    return -1;
                }
                tmpStream = ftp.retrieveFileStream(element.filePath);
                this.openChannels++;
                ManagedFastInputChannel rf = new ManagedFastInputChannel();
                rf.mfChannel = java.nio.channels.Channels.newChannel(tmpStream);
                rf.mPath = element.filePath;
                this.mvReadyFiles.add(rf);
                this.maFiles.add(element);
                iNumPaths++;
            } catch (Exception e) {
                while (this.mvReadyFiles.size() > 0) {
                    ManagedFastInputChannel fs = this.mvReadyFiles.remove(0);
                    this.close(fs, NIOFileReader.OK_RECORD);
                }
                throw new Exception("Failed to open file: " + e.toString());
            }
        }
        return iNumPaths;
    }

    /**
	 * Gets the FTP connection.
	 * 
	 * @param strUser
	 *            the str user
	 * @param strPassword
	 *            the str password
	 * @param strServer
	 *            the str server
	 * @param binaryTransfer
	 *            the binary transfer
	 * @param connectionNote
	 *            the connection note
	 * 
	 * @return the FTP connection
	 */
    private FTPClient getFTPConnection(String strUser, String strPassword, String strServer, boolean binaryTransfer, String connectionNote) {
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(strServer);
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Connected to " + strServer + ", " + connectionNote);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP server refused connection.");
                return null;
            }
        } catch (IOException e) {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException f) {
                    return null;
                }
            }
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP Could not connect to server.");
            ResourcePool.LogException(e, this);
            return null;
        }
        try {
            if (!ftp.login(strUser, strPassword)) {
                ftp.logout();
                ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "FTP login failed.");
                return null;
            }
            ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Remote system is " + ftp.getSystemName() + ", " + connectionNote);
            if (binaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            }
            ftp.enterLocalPassiveMode();
        } catch (FTPConnectionClosedException e) {
            ResourcePool.LogMessage(this, ResourcePool.ERROR_MESSAGE, "Server closed connection.");
            ResourcePool.LogException(e, this);
            return null;
        } catch (IOException e) {
            ResourcePool.LogException(e, this);
            return null;
        }
        return ftp;
    }
}

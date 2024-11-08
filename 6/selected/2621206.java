package com.mindtree.techworks.insight.download.ftpbrowse.test;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import com.mindtree.techworks.insight.download.ftpbrowse.FTPBrowseException;
import com.mindtree.techworks.insight.download.ftpbrowse.FTPFileFile;
import com.mindtree.techworks.insight.download.ftpbrowse.FTPRemoteFileSystemView;
import com.mindtree.techworks.insight.gui.widgets.InsightRemoteFileChooser;

/**
 * TODO
 * 
 * 
 * 
 * @author Bindul Bhowmik
 * @version $Revision: 27 $ $Date: 2007-12-16 06:58:03 -0500 (Sun, 16 Dec 2007) $
 *  
 */
public class FileChooserTestFrame extends JFrame {

    /**
	 * Used for object serialization
	 */
    private static final long serialVersionUID = 7240401258426606510L;

    /**
	 * Logger
	 */
    private static final Logger logger = Logger.getLogger(FileChooserTestFrame.class.getName());

    /**
	 * @throws java.awt.HeadlessException
	 * @throws MalformedURLException
	 */
    public FileChooserTestFrame() throws HeadlessException, MalformedURLException {
        super();
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent aEvent) {
                System.exit(0);
            }
        });
        Dimension dim = getToolkit().getScreenSize();
        Rectangle abounds = getBounds();
        setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
        setVisible(true);
        URL url = new URL("ftp://cendantstp/");
        char[] password = "spnr".toCharArray();
        PasswordAuthentication passwordAuthentication = new PasswordAuthentication("spnr", password);
        FTPRemoteFileSystemView remoteFileSystemView = new FTPRemoteFileSystemView(url, passwordAuthentication);
        JFileChooser fileChooser = new InsightRemoteFileChooser(remoteFileSystemView);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        File[] selectedFiles = null;
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFiles = fileChooser.getSelectedFiles();
            for (int i = 0; i < selectedFiles.length; i++) {
                if (selectedFiles[i] instanceof FTPFileFile) {
                    FTPFileFile ftpFile = (FTPFileFile) selectedFiles[i];
                    logger.fine(ftpFile.getName());
                    logger.fine(ftpFile.getPath());
                } else {
                    logger.fine(selectedFiles[i].toString());
                    logger.fine(selectedFiles[i].getAbsolutePath());
                }
            }
        }
        remoteFileSystemView.disconnect();
        try {
            if (null != selectedFiles) {
                FTPClient ftpClient = new FTPClient();
                InetAddress inetAddress = InetAddress.getByName(url.getHost());
                ftpClient.connect(inetAddress);
                if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    throw new FTPBrowseException(ftpClient.getReplyString());
                }
                if (null != passwordAuthentication) {
                    ftpClient.login(passwordAuthentication.getUserName(), new StringBuffer().append(passwordAuthentication.getPassword()).toString());
                }
                for (int i = 0; i < selectedFiles.length; i++) {
                    FTPFileFile file = (FTPFileFile) selectedFiles[i];
                    logger.fine(file.getPath());
                    FileOutputStream fos = new FileOutputStream(new File("d:/junk/ftp/test.txt"));
                    logger.fine("" + ftpClient.retrieveFile(file.getPath().replaceAll("\\\\", "/"), fos));
                    fos.close();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
	 * Summa method
	 * @param args
	 */
    public static void main(String[] args) {
        LogManager logManager = LogManager.getLogManager();
        try {
            logManager.readConfiguration(FileChooserTestFrame.class.getClassLoader().getResourceAsStream("com/mindtree/logging/download/ftpbrowse/test/logging.properties"));
        } catch (SecurityException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            FileChooserTestFrame fileChooserTestFrame = new FileChooserTestFrame();
            fileChooserTestFrame.getHeight();
        } catch (HeadlessException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}

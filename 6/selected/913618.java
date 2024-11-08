package at.filemonkey.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import at.filemonkey.data.FTPSite;
import at.filemonkey.data.MonkeyFTPFile;
import at.filemonkey.model.renderer.FtpCellRenderer;
import at.filemonkey.model.transferhandler.FtpTransferHandler;

/**
 * the main class for FTPConnection
 * for delete, rename, download, upload Files or create Folders
 * implements the JList
 * @author Philipp Maurer
 * 
 */
public class FtpDirectoryNavigator extends DirectoryNavigator {

    private FTPClient client = new FTPClient();

    private boolean connection;

    private int dirSize = 0;

    private MonkeyFTPFile[] fileList;

    public FtpDirectoryNavigator() {
        connection = false;
    }

    public FtpDirectoryNavigator(FTPSite site) throws SocketException, IOException {
        connect(site);
    }

    public boolean connect(FTPSite site) throws SocketException, IOException {
        dirSize = 0;
        client.connect(site.getHost(), site.getPort());
        connection = client.login(site.getUser(), site.getPassword());
        if (connection) {
            client.enterRemotePassiveMode();
            client.enterLocalPassiveMode();
            reList();
        }
        return connection;
    }

    public boolean testConnection(FTPSite site) throws IOException {
        FTPClient testclient = new FTPClient();
        testclient.connect(site.getHost(), site.getPort());
        boolean check = testclient.login(site.getUser(), site.getPassword());
        testclient.disconnect();
        return check;
    }

    public boolean isConnected() {
        return connection;
    }

    public void disconnect() throws IOException {
        connection = false;
        dirSize = 0;
        fileList = new MonkeyFTPFile[0];
        client.disconnect();
    }

    @Override
    public boolean cd(String dir) throws IOException {
        if (connection) {
            boolean check = client.changeWorkingDirectory(dir);
            reList();
            return check;
        } else return false;
    }

    @Override
    public boolean up() throws IOException {
        if (connection) {
            boolean check = client.changeToParentDirectory();
            reList();
            return check;
        } else return false;
    }

    @Override
    public String getCurrentPath() throws IOException {
        if (connection) {
            String workingDirectory = client.printWorkingDirectory();
            if (workingDirectory.substring(workingDirectory.length() - 1, workingDirectory.length()).equals("/")) return workingDirectory; else return workingDirectory + "/";
        }
        return "";
    }

    /**
	 * updates cached values fileList and dirSize should be called whenever
	 * there is a change in the file structure or change directory
	 * 
	 * @throws IOException
	 */
    public void reList() throws IOException {
        FTPFile[] files = client.listFiles();
        dirSize = files.length - 1;
        String dir = getCurrentPath();
        fileList = new MonkeyFTPFile[dirSize];
        for (int i = 0; i < dirSize; i++) {
            fileList[i] = new MonkeyFTPFile(dir, files[i + 1]);
        }
    }

    @Override
    public boolean delete(String file) throws IOException {
        if (connection) {
            boolean check = client.deleteFile(getCurrentPath() + file);
            if (check) reList(); else {
                check = client.removeDirectory(getCurrentPath() + file);
                if (check) reList();
            }
            return check;
        } else return false;
    }

    @Override
    public boolean createFolder(String name) throws IOException {
        if (connection) {
            boolean check = client.makeDirectory(name);
            reList();
            return check;
        } else return false;
    }

    @Override
    public boolean rename(String oldName, String newName) throws IOException {
        if (connection) {
            boolean check = client.rename(oldName, newName);
            reList();
            return check;
        } else return false;
    }

    public File download(MonkeyFTPFile monkeyfile) throws IOException {
        if (connection) {
            File tempfile = File.createTempFile("name", ".ext");
            FileOutputStream fos = new FileOutputStream(tempfile);
            boolean check = client.retrieveFile(monkeyfile.getPath() + monkeyfile.getFtpfile().getName(), fos);
            fos.close();
            if (!check) {
                return null;
            } else return tempfile;
        } else return null;
    }

    public boolean upload(File file) throws IOException, FileNotFoundException {
        if (connection) {
            InputStream inStream = new FileInputStream(file);
            boolean check = client.storeFile(file.getName(), inStream);
            inStream.close();
            reList();
            return check;
        } else return false;
    }

    @Override
    public String getFilename(Object file) {
        MonkeyFTPFile ftpfile = (MonkeyFTPFile) file;
        return ftpfile.getFtpfile().getName();
    }

    @Override
    public ListCellRenderer getListCellRenderer() {
        return new FtpCellRenderer();
    }

    @Override
    public TreeCellRenderer getTreeCellRenderer() {
        return null;
    }

    @Override
    public void addTreeModelListener(TreeModelListener arg0) {
    }

    @Override
    public Object getChild(Object obj, int index) {
        return 0;
    }

    @Override
    public int getChildCount(Object obj) {
        return 0;
    }

    @Override
    public int getIndexOfChild(Object obj1, Object obj2) {
        return 0;
    }

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public boolean isLeaf(Object obj) {
        return false;
    }

    @Override
    public void removeTreeModelListener(TreeModelListener arg0) {
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {
    }

    @Override
    public void addListDataListener(ListDataListener arg0) {
    }

    @Override
    public Object getElementAt(int index) {
        return fileList[index];
    }

    @Override
    public int getSize() {
        return dirSize;
    }

    @Override
    public void removeListDataListener(ListDataListener arg0) {
    }

    public FTPClient getClient() {
        return client;
    }

    public void setClient(FTPClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "FTP";
    }
}

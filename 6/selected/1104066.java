package utils;

import components.TextImageObj;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import sun.awt.shell.ShellFolder;

/**
 * Ftp util
 * @author pmchanh
 * ref: http://www.kodejava.org/examples/357.html and http://blog.codebeach.com/2008/02/get-file-type-icon-with-java.html
 */
public class FtpResource {

    private String _url;

    private String _password;

    private String _username;

    private FTPClient _ftpClient;

    private String _rootPath;

    public String getRootPath() {
        return _rootPath;
    }

    /**
     * Get working directory
     */
    public String getWorkingDir() throws Exception {
        try {
            return _ftpClient.printWorkingDirectory();
        } catch (IOException ex) {
            Logger.getLogger(FtpResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception(ex.getMessage());
        }
    }

    /**
     * Constructor
     */
    public FtpResource(String url, String username, String password) {
        _url = url;
        _password = password;
        _username = username;
    }

    /**
     *  Connect to server
     */
    public Boolean connect() throws Exception {
        try {
            _ftpClient = new FTPClient();
            _ftpClient.connect(_url);
            _ftpClient.login(_username, _password);
            _rootPath = _ftpClient.printWorkingDirectory();
            return true;
        } catch (Exception ex) {
            throw new Exception("Cannot connect to server.");
        }
    }

    /**
     * Disconnect from server
     */
    public void disConnect() throws Exception {
        try {
            _ftpClient.logout();
            _ftpClient.disconnect();
        } catch (Exception ex) {
            throw new Exception("Cannot disconnect from server.");
        }
        _ftpClient = null;
    }

    /**
     *  Change working directory
     */
    public Boolean changeDir(String remotePath) throws Exception {
        if (_ftpClient != null) {
            try {
                _ftpClient.cwd(remotePath);
            } catch (IOException ex) {
                throw new Exception(ex.getMessage());
            }
        }
        return true;
    }

    /**
     * go to parent of working directory
     */
    public boolean goUp() {
        try {
            _ftpClient.changeToParentDirectory();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    /**
     *  get all files from current dir in server
     */
    public Vector getAllFiles(String wd, Boolean isSmallIcon) throws Exception {
        Vector rs = new Vector();
        if (_ftpClient != null && _ftpClient.isConnected()) {
            try {
                changeDir(wd);
                ArrayList<Object[]> dirArr = new ArrayList<Object[]>();
                ArrayList<Object[]> firArr = new ArrayList<Object[]>();
                FTPFile[] ftpFiles = _ftpClient.listFiles();
                for (FTPFile file : ftpFiles) {
                    Object[] item = new Object[4];
                    Icon icon = getIcon(file, isSmallIcon);
                    String ext = getExtension(file);
                    item[0] = new TextImageObj(getName(file), icon, ext);
                    item[1] = ext;
                    item[2] = getSize(file);
                    item[3] = getDate(file.getTimestamp());
                    if (file.isDirectory()) dirArr.add(item); else firArr.add(item);
                }
                Collections.sort(dirArr, new Comparer());
                Collections.sort(firArr, new Comparer());
                Iterator iterDir = dirArr.iterator();
                Iterator iterFile = firArr.iterator();
                if (!_ftpClient.printWorkingDirectory().equals(_rootPath)) {
                    Object[] emptyRow = { TextImageObj.createEmptyObj(), "", "", "", "" };
                    rs.add(emptyRow);
                }
                while (iterDir.hasNext()) rs.add(iterDir.next());
                while (iterFile.hasNext()) rs.add(iterFile.next());
            } catch (IOException ex) {
                throw new Exception(ex.getMessage());
            }
        }
        return rs;
    }

    /**
     * Lay icon file
     * Thamkhao: http://blog.codebeach.com/2008/02/get-file-type-icon-with-java.html
     */
    private Icon getIcon(FTPFile file, Boolean smallIcon) {
        File temp = null;
        Icon ico = null;
        if (file.isFile()) {
            try {
                temp = File.createTempFile("icon", "." + getExtension(file));
            } catch (IOException ex) {
                Logger.getLogger(FtpResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (file.isDirectory()) {
            temp = new File("C:\\tinitemp");
            temp.mkdir();
        }
        ShellFolder sf;
        if (smallIcon) {
            FileSystemView view = FileSystemView.getFileSystemView();
            ico = view.getSystemIcon(temp);
        } else {
            try {
                sf = ShellFolder.getShellFolder(temp);
                ico = new ImageIcon(sf.getIcon(true));
                temp.delete();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FtpResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ico;
    }

    private static String getExtension(FTPFile f) {
        String extension = "";
        String name = f.getName();
        if (!f.isDirectory()) {
            int e = name.lastIndexOf(".");
            if (e > 0) extension = name.substring(e + 1, name.length());
        }
        return extension;
    }

    private static String getSize(FTPFile f) {
        String rs = "<DIR>";
        if (!f.isDirectory()) rs = "" + f.getSize();
        return rs;
    }

    private static String getName(FTPFile f) {
        String name = f.getName();
        if (!f.isDirectory()) {
            int e = name.lastIndexOf(".");
            if (e > 0) name = name.substring(0, e);
        }
        return name;
    }

    private static String getDate(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int date = cal.get(Calendar.DAY_OF_MONTH);
        int time = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        String kq = "";
        kq += date < 10 ? "0" + date : "" + date;
        kq += "/";
        kq += month < 10 ? "0" + month : "" + month;
        kq += "/" + year + " ";
        kq += time < 10 ? "0" + time + ":" : "" + time + ":";
        kq += minute < 10 ? "0" + minute + ":" : "" + minute + ":";
        kq += second < 10 ? "0" + second : "" + second;
        return kq;
    }
}

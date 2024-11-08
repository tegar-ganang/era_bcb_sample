package de.lotk.webftp.business;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.oro.text.perl.Perl5Util;
import de.lotk.webftp.PrintCommandListener;
import de.lotk.webftp.bean.LoginData;
import de.lotk.webftp.exceptions.AccessDeniedException;
import de.lotk.webftp.exceptions.ConnectionClosedException;
import de.lotk.webftp.exceptions.ConnectionEstablishException;

/**
 * Default-Implementierung einer FtpClientConnection
 * 
 * @author Stephan Sann
 * @version 1.0
 */
public class DefaultFtpClientConnection implements FtpClientConnection {

    /** Perl5Util fuer Preg-Match-Aufgaben */
    private static Perl5Util perl5Util = new Perl5Util();

    /** Unser FTPClient-Objekt */
    private FTPClient ftpClient = null;

    /** Uebertragungstyp */
    private int uebertragungsTyp = 0;

    /**
   * Initialisiert ein neues DefaultBtWebPermissionManager-Objekt
   * 
   * @throws   InstantiationException
   */
    public DefaultFtpClientConnection() throws InstantiationException {
        this.ftpClient = new FTPClient();
        this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
    }

    /**
   * Erstellt rekursiv Verzeichnisse auf dem Server
   * 
   * @param   pathDone       Bereits abgearbeiteter Pfad
   * @param   pathRemainder  Noch zu bearbeitender Pfad
   * @return                 <code>true</code>, wenn erfolgreich;
   *                         <code>false</code>, wenn nicht erfolgreich.
   */
    private boolean recursiveDirCreate(String pathDone, String pathRemainder) throws IOException {
        boolean erfolg = true;
        ArrayList remainderParts = new ArrayList();
        perl5Util.split(remainderParts, "#/#", pathRemainder);
        String pathToDo = (String) remainderParts.get(0);
        String pathNewDone = null;
        if (pathDone.length() >= 1) {
            pathNewDone = (new StringBuffer(pathDone)).append('/').append(pathToDo).toString();
        } else {
            pathNewDone = pathToDo;
        }
        if (!this.listNames(pathDone).contains(pathToDo)) {
            erfolg = this.makeDirectory(pathNewDone);
        }
        if ((remainderParts.size() > 1) && (remainderParts.get(1) != null) && (((String) remainderParts.get(1)).length() >= 1)) {
            String pathNewRemainder = pathRemainder.substring(pathRemainder.indexOf('/') + 1);
            erfolg = this.recursiveDirCreate(pathNewDone, pathNewRemainder);
        }
        return (erfolg);
    }

    /**
   * Loescht rekursiv Verzeichnisse auf dem Server
   * 
   * @param   path           Zu loeschendes Verzeichnis
   * @return                 <code>true</code>, wenn erfolgreich;
   *                         <code>false</code>, wenn nicht erfolgreich.
   */
    private boolean recursiveDirDeletion(String path) throws IOException {
        boolean erfolg = true;
        FTPFile[] ftpFiles = this.filterDotDirs(this.ftpClient.listFiles(path));
        for (int xx = 0; xx < ftpFiles.length; xx++) {
            StringBuffer actEntry = new StringBuffer(path).append('/');
            actEntry.append(ftpFiles[xx].getName());
            if (!ftpFiles[xx].isDirectory()) {
                erfolg = this.deleteFile(actEntry.toString());
            } else {
                erfolg = this.recursiveDirDeletion(actEntry.toString());
            }
            if (!erfolg) {
                break;
            }
        }
        erfolg = this.removeDirectory(path);
        return (erfolg);
    }

    /**
   * Liefert die Objekte in dem uebergebenen Array als List-Objekt zurueck. Wenn
   * das uebergebene Array "null" oder leer ist, wird eine leere Liste
   * zurueckgegeben.
   * 
   * @param   objArray  Das umzuwandelnde Array
   * @return            <code>List</code> mit Elementen aus uebergebenem Array
   */
    private List asList(Object[] objArray) {
        if ((objArray == null) || (objArray.length < 1)) {
            return (new ArrayList());
        } else {
            return (Arrays.asList(objArray));
        }
    }

    /**
   * Da "listNames" bei manchen FTP-Servern nicht nur die File-Names, sondern
   * auch den gesammten Pfad zurueckliefern, mussen wir letzteren rausfiltern.
   * 
   * @param   namesList  Namens-Liste, die gefiltert werden soll. 
   * @return             <code>String[]</code> mit gefilterten File-Namen
   *                     <code>null</code>, wenn null uebergeben wurde.
   */
    private String[] filterNamesList(String[] namesList) {
        if (namesList != null) {
            int slashPos = 0;
            for (int yy = 0; yy < namesList.length; yy++) {
                if (((slashPos = namesList[yy].lastIndexOf('/')) > (-1)) && (slashPos != (namesList[yy].length() - 1))) {
                    namesList[yy] = namesList[yy].substring(slashPos + 1);
                }
            }
            return (namesList);
        } else {
            return (null);
        }
    }

    /**
   * Filtert die "Dot-Dirs" ("." und "..") aus einem FTP-File-Array.
   * 
   * @param       ftpFiles  FTPFile-Array, das gefiltert werden soll.
   * @return                <code>FTPFile[]</code> ohne Dot-Dirs.
   */
    private FTPFile[] filterDotDirs(FTPFile[] ftpFiles) {
        ArrayList filteredList = new ArrayList();
        for (int xx = 0; xx < ftpFiles.length; xx++) {
            if ((!ftpFiles[xx].getName().equals(".")) && (!ftpFiles[xx].getName().equals(".."))) {
                filteredList.add(ftpFiles[xx]);
            }
        }
        return ((FTPFile[]) filteredList.toArray(new FTPFile[filteredList.size()]));
    }

    public void login(LoginData loginData) throws ConnectionEstablishException, AccessDeniedException {
        try {
            int reply;
            this.ftpClient.connect(loginData.getFtpServer());
            reply = this.ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                this.ftpClient.disconnect();
                throw (new ConnectionEstablishException("FTP server refused connection."));
            }
        } catch (IOException e) {
            if (this.ftpClient.isConnected()) {
                try {
                    this.ftpClient.disconnect();
                } catch (IOException f) {
                }
            }
            e.printStackTrace();
            throw (new ConnectionEstablishException("Could not connect to server.", e));
        }
        try {
            if (!this.ftpClient.login(loginData.getFtpBenutzer(), loginData.getFtpPasswort())) {
                this.logout();
                throw (new AccessDeniedException("Could not login into server."));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw (new AccessDeniedException("Could not login into server.", ioe));
        }
    }

    public boolean logout() {
        try {
            this.ftpClient.logout();
        } catch (IOException ioe) {
            return (false);
        } finally {
            if (this.ftpClient.isConnected()) {
                try {
                    this.ftpClient.disconnect();
                } catch (IOException f) {
                    return (false);
                }
            }
        }
        return (true);
    }

    public void verifyConnection(LoginData loginData) throws IOException, AccessDeniedException, ConnectionEstablishException {
        try {
            boolean erfolg = this.sendNoOp();
            if (!erfolg) {
                throw (new IOException("Noop konnte nicht gesandt werden."));
            }
        } catch (ConnectionClosedException cce) {
            cce.printStackTrace();
            this.login(loginData);
        }
    }

    public List listFiles() throws IOException {
        return (this.asList(this.filterDotDirs(this.ftpClient.listFiles())));
    }

    public List listFiles(String pathname) throws IOException {
        return (this.asList(this.filterDotDirs(this.ftpClient.listFiles(pathname))));
    }

    public List listNames() throws IOException {
        return (this.asList(this.filterNamesList(this.ftpClient.listNames())));
    }

    public List listNames(String pathname) throws IOException {
        return (this.asList(this.filterNamesList(this.ftpClient.listNames(pathname))));
    }

    public boolean retrieveFile(String remote, OutputStream local) throws IOException {
        return (this.ftpClient.retrieveFile(remote, local));
    }

    public boolean storeFile(String remote, InputStream local) throws IOException {
        return (this.ftpClient.storeFile(remote, local));
    }

    public boolean sendSiteCommand(String command) throws IOException {
        return (this.ftpClient.sendSiteCommand(command));
    }

    public InputStream retrieveFileStream(String remote) throws IOException {
        return (this.ftpClient.retrieveFileStream(remote));
    }

    public boolean completePendingCommand() throws IOException {
        return (this.ftpClient.completePendingCommand());
    }

    public boolean rename(String from, String to) throws IOException {
        return (this.ftpClient.rename(from, to));
    }

    public boolean deleteFile(String pathname) throws IOException {
        return (this.ftpClient.deleteFile(pathname));
    }

    public boolean deleteMultipleFiles(String[] pathnames) throws IOException {
        boolean erfolg = true;
        for (int ww = 0; ww < pathnames.length; ww++) {
            erfolg = this.deleteFile(pathnames[ww]);
            if (!erfolg) {
                return (false);
            }
        }
        return (erfolg);
    }

    public int getFileType() {
        return (this.uebertragungsTyp);
    }

    public boolean setFileType(int fileType) throws IOException {
        if (this.ftpClient.setFileType(fileType)) {
            this.uebertragungsTyp = fileType;
            return (true);
        } else {
            return (false);
        }
    }

    public boolean makeDirectory(String pathname) throws IOException {
        return (this.ftpClient.makeDirectory(pathname));
    }

    public boolean mkDirs(String pathname) throws IOException {
        return (this.recursiveDirCreate("", pathname));
    }

    public boolean sendNoOp() throws IOException {
        try {
            return (this.ftpClient.sendNoOp());
        } catch (FTPConnectionClosedException cce) {
            throw (new ConnectionClosedException(cce.getMessage()));
        }
    }

    public boolean removeDirectory(String pathname) throws IOException {
        return (this.ftpClient.removeDirectory(pathname));
    }

    public boolean removeDirRecursively(String pathname) throws IOException {
        return (this.recursiveDirDeletion(pathname));
    }

    public void destroy() {
        this.logout();
    }
}

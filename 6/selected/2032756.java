package sourced;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stuart
 */
public class FTPProject implements Project {

    String hostname;

    String username;

    String password;

    int port;

    boolean requiresPassword;

    FTPActiveFolder activeFolder;

    FTPClient client;

    public FTPProject() {
    }

    public FTPProject(String hostname, String username, String password, String port) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        if (this.password.equals("")) {
            this.requiresPassword = false;
        } else {
            this.requiresPassword = true;
        }
        if (port.equals("")) {
            this.port = 21;
        } else {
            this.port = Integer.parseInt(port);
        }
    }

    public ActiveFolder getBaseFolder() {
        return this.activeFolder;
    }

    public String getName() {
        return this.hostname;
    }

    public boolean establish() {
        return createFTPConnection();
    }

    private boolean createFTPConnection() {
        client = new FTPClient();
        System.out.println("Client created");
        try {
            client.connect(this.hostname, this.port);
            System.out.println("Connected: " + this.hostname + ", " + this.port);
            client.login(username, password);
            System.out.println("Logged in: " + this.username + ", " + this.password);
            this.setupActiveFolder();
            return true;
        } catch (IllegalStateException ex) {
            Logger.getLogger(FTPProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(FTPProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FTPIllegalReplyException ex) {
            Logger.getLogger(FTPProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FTPException ex) {
            Logger.getLogger(FTPProject.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void setupActiveFolder() throws IOException {
        this.activeFolder = new FTPActiveFolder("", client);
    }

    public int projectType() {
        return Project.REMOTE;
    }

    public String saveToXML() {
        String xml = "<project type='" + this.getClass().getName() + "'>";
        xml += "<hostname>" + this.hostname + "</hostname>";
        xml += "<username>" + this.username + "</username>";
        xml += "<port>" + this.port + "</port>";
        xml += "<requirespassword>" + this.requiresPassword + "</requirespassword>";
        xml += "</project>";
        return xml;
    }

    public void buildFromMap(HashMap map) {
        this.username = (String) map.get("username");
        this.hostname = (String) map.get("hostname");
        this.port = Integer.parseInt((String) map.get("port"));
        this.requiresPassword = Boolean.parseBoolean((String) map.get("requirespassword"));
    }

    public boolean requiresPassword() {
        return requiresPassword;
    }

    public void setPassword(String pwd) {
        this.password = pwd;
    }

    public boolean equals(Project proj) {
        if (proj instanceof FTPProject) {
            FTPProject test = (FTPProject) proj;
            if (test.getHostname().equals(this.hostname) && test.getUsername().equals(this.username) && test.getPassword().equals(this.password)) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    public String getHostname() {
        return this.hostname;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public HashMap<String, String> getProjectProperties() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setProjectProperties(HashMap<String, String> props) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void moveUpLevel() {
        this.activeFolder.moveUpLevel();
    }

    public boolean setLocation(String location) {
        return this.activeFolder.setLocation(location);
    }

    public String getBasePath() {
        return this.activeFolder.getFullPath();
    }

    public String toString() {
        return this.username + "@" + this.hostname;
    }
}

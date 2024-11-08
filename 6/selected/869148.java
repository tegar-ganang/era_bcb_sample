package synology;

import java.io.IOException;
import java.net.UnknownHostException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;

public class TryMe {

    public static void main(String[] args) {
        try {
            FTPClient p = new FTPClient();
            p.connect("url");
            p.login("login", "pass");
            int sendCommand = p.sendCommand("SYST");
            System.out.println("TryMe.main() - " + sendCommand + " (sendCommand)");
            sendCommand = p.sendCommand("PWD");
            System.out.println("TryMe.main() - " + sendCommand + " (sendCommand)");
            sendCommand = p.sendCommand("NOOP");
            System.out.println("TryMe.main() - " + sendCommand + " (sendCommand)");
            sendCommand = p.sendCommand("PASV");
            System.out.println("TryMe.main() - " + sendCommand + " (sendCommand)");
            p.changeWorkingDirectory("/");
            try {
                printDir(p, "/");
            } catch (Exception e) {
                e.printStackTrace();
            }
            p.logout();
            p.disconnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printDir(FTPClient c, String s) throws Exception {
        FTPFile[] listFiles = c.listFiles(s);
        for (int i = 0; i < listFiles.length; ++i) {
            FTPFile file = listFiles[i];
            String name = file.getName();
            System.out.println("TryMe.main() - " + s + "/" + name + " (name)");
            if (file.isDirectory()) {
                printDir(c, s + name + "/");
            }
        }
    }
}

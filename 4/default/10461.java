import java.io.*;
import javax.swing.ListSelectionModel;
import javax.swing.*;
import javax.swing.table.*;

public class XylFTPGUI extends javax.swing.JFrame implements XylFTPInterface {

    class FileEntry {

        public boolean IsDirectory;

        public long Size;

        public String Name;
    }

    private String UserName;

    private String PassWord;

    private String StrRemotePWD;

    private FileEntry RemoteFileList[];

    private XylFTPConnection FTPConnection;

    private final int TABLE_MIN_ROWS = 20;

    public String[] GetCommands() throws Exception {
        return null;
    }

    public boolean IsValidEcho(String Echo) {
        if (Echo.length() < 4) return false;
        if (Echo.charAt(0) < '0' || Echo.charAt(0) > '9') return false;
        if (Echo.charAt(1) < '0' || Echo.charAt(1) > '9') return false;
        if (Echo.charAt(2) < '0' || Echo.charAt(2) > '9') return false;
        return true;
    }

    private static String ParseIP(String From) throws XylFTPException {
        int i, j;
        String t;
        String[] nums;
        i = From.indexOf("(");
        if (i == -1) throw new XylFTPException("PASV", 1, "Invalid echo!");
        j = From.indexOf(")");
        if (j == -1) throw new XylFTPException("PASV", 1, "Invalid echo!");
        t = From.substring(i + 1, j);
        nums = t.split(",");
        if (nums.length < 4) throw new XylFTPException("PASV", 1, "Invalid echo!");
        return (nums[0] + "." + nums[1] + "." + nums[2] + "." + nums[3]);
    }

    private static int ParsePort(String From) throws XylFTPException {
        int i, j;
        int port1, port2;
        String t;
        String[] nums;
        i = From.indexOf("(");
        if (i == -1) throw new XylFTPException("", 1, "Invalid echo!");
        j = From.indexOf(")");
        if (j == -1) throw new XylFTPException("", 1, "Invalid echo!");
        t = From.substring(i + 1, j);
        nums = t.split(",");
        port1 = Integer.parseInt(nums[4]);
        port2 = Integer.parseInt(nums[5]);
        return port1 * 256 + port2;
    }

    protected void ShowEcho(String Echo) {
        jTextCommandAndEcho.setText(jTextCommandAndEcho.getText() + "Server: " + Echo + "\r\n");
    }

    protected void ShowCommand(String cmd) {
        jTextCommandAndEcho.setText(jTextCommandAndEcho.getText() + "Send command: " + cmd);
    }

    public int ProcessEcho(String OEcho) {
        String Echo = OEcho.trim();
        ShowEcho(Echo);
        if (Echo.charAt(3) == '-') return 6;
        if (Echo.substring(0, 1).equals("1")) return 1;
        if (Echo.substring(0, 1).equals("2")) {
            if (Echo.substring(0, 3).equals("227")) return 3; else if (Echo.substring(0, 3).equals("200")) return 3; else return 2;
        }
        if (Echo.substring(0, 1).equals("3")) return 3;
        if (Echo.substring(0, 1).equals("4")) {
            if (Echo.substring(0, 3).equals("421")) {
                FTPConnection.SetStatus(0);
                return 2;
            }
            return 4;
        }
        if (Echo.substring(0, 1).equals("5")) return 5;
        if (Echo.substring(1, 2).equals("0")) return 0; else return -1;
    }

    protected String GetRemotePWD() throws XylFTPException, IOException {
        String echo;
        int i, j;
        int echoMeaning;
        FTPConnection.SendCommand("PWD\r\n");
        ShowCommand("PWD\r\n");
        echo = FTPConnection.GetEcho();
        echoMeaning = ProcessEcho(echo);
        while (echoMeaning == 6) {
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
        }
        if (echoMeaning == 5) {
            throw new XylFTPException("pwd", "Server refuses");
        }
        if (FTPConnection.GetStatus() == 0) {
            throw new XylFTPException("pwd", " Connection  not exsits");
        }
        i = echo.indexOf("\"");
        j = echo.lastIndexOf("\"");
        return echo.substring(i + 1, j);
    }

    protected void ChangeRemotePWD(String DirToChangeTo) {
        String echo;
        int echoMeaning;
        try {
            FTPConnection.SendCommand("CWD " + DirToChangeTo + "\r\n");
            ShowCommand("CWD " + DirToChangeTo + "\r\n");
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
            while (echoMeaning == 6) {
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
            }
            StrRemotePWD = GetRemotePWD();
            jTextRemotePWD.setText(StrRemotePWD);
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please disconnect and connect again!", "XylFTP cwd error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error" + " , the error message is: " + ex.getMessage() + " please disconnect and connect again!", "XylFTP cwd error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        }
    }

    protected void ParseRemoteFileList(String StrRemoteFileList) {
        if (StrRemoteFileList == null || StrRemoteFileList.equals("")) {
            RemoteFileList = new FileEntry[0];
            return;
        }
        String listEntries[] = StrRemoteFileList.split("\r\n");
        FileEntry TmpRemoteFileList[] = new FileEntry[listEntries.length];
        boolean IsValidTmpRemoteFileList[] = new boolean[listEntries.length];
        String strSize;
        int k, m, curLen, flag, delimCount;
        int validCount = 0;
        for (int i = 0; i < TmpRemoteFileList.length; i++) {
            k = 0;
            TmpRemoteFileList[i] = new FileEntry();
            if (listEntries[i].charAt(k) == 'd' || listEntries[i].charAt(k) == 'l') {
                TmpRemoteFileList[i].IsDirectory = true;
            } else {
                TmpRemoteFileList[i].IsDirectory = false;
            }
            curLen = listEntries[i].length();
            flag = 0;
            delimCount = 0;
            strSize = "";
            while (k < curLen) {
                if (listEntries[i].charAt(k) == ' ') {
                    flag = 1;
                } else {
                    if (flag == 1) {
                        delimCount++;
                        flag = 0;
                    }
                    if (delimCount == 4) {
                        strSize += listEntries[i].charAt(k);
                    }
                    if (delimCount == 8) {
                        break;
                    }
                }
                k++;
            }
            m = listEntries[i].lastIndexOf(" ->");
            if (m == -1) {
                TmpRemoteFileList[i].Name = listEntries[i].substring(k);
            } else {
                TmpRemoteFileList[i].Name = listEntries[i].substring(k, m);
            }
            try {
                TmpRemoteFileList[i].Size = Integer.parseInt(strSize);
                IsValidTmpRemoteFileList[i] = true;
                validCount++;
            } catch (NumberFormatException ex) {
                IsValidTmpRemoteFileList[i] = false;
            }
        }
        RemoteFileList = new FileEntry[validCount];
        int validInd = -1;
        for (int i = 0; i < TmpRemoteFileList.length; i++) {
            if (IsValidTmpRemoteFileList[i] == true) {
                validInd++;
                RemoteFileList[validInd] = new FileEntry();
                RemoteFileList[validInd].IsDirectory = TmpRemoteFileList[i].IsDirectory;
                RemoteFileList[validInd].Name = TmpRemoteFileList[i].Name;
                RemoteFileList[validInd].Size = TmpRemoteFileList[i].Size;
            }
        }
    }

    protected void ListRemoteFiles() throws Exception {
        String echo;
        String strRemoteFileList;
        String HostIP;
        int HostPort;
        int echoMeaning;
        try {
            if (FTPConnection.IsPassive()) {
                FTPConnection.SendCommand("PASV\r\n");
                ShowCommand("PASV\r\n");
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 5) {
                    throw new XylFTPException("ls", "Server refuses");
                }
                if (FTPConnection.GetStatus() == 0) {
                    throw new XylFTPException("ls", "Server has closed the control connection");
                }
                HostIP = ParseIP(echo);
                HostPort = ParsePort(echo);
                FTPConnection.OpenDataConnection(HostIP, HostPort);
                FTPConnection.SendCommand("LIST " + StrRemotePWD + "\r\n");
                ShowCommand("LIST " + StrRemotePWD + "\r\n");
                echo = FTPConnection.GetEcho();
                if (!IsValidEcho(echo)) throw new XylFTPException("ls", FTPConnection.GetStatus(), "Invaild echo.");
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 1) {
                    strRemoteFileList = FTPConnection.GetListPassive();
                    ParseRemoteFileList(strRemoteFileList);
                    echo = FTPConnection.GetEcho();
                    echoMeaning = ProcessEcho(echo);
                }
            } else {
                String selfIp, selfPort;
                selfIp = FTPConnection.GetSelfIP();
                selfPort = FTPConnection.GetSelfPort();
                FTPConnection.ListenForDataConnection();
                FTPConnection.SendCommand("PORT " + selfIp + selfPort + "\r\n");
                ShowCommand("PORT " + selfIp + selfPort + "\r\n");
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 5) {
                    throw new XylFTPException("ls", "Server refuses");
                }
                if (FTPConnection.GetStatus() == 0) {
                    throw new XylFTPException("ls", "Server has closed the control connection");
                }
                FTPConnection.SendCommand("LIST " + StrRemotePWD + "\r\n");
                ShowCommand("LIST " + StrRemotePWD + "\r\n");
                FTPConnection.OpenDataConnection();
                echo = FTPConnection.GetEcho();
                if (!IsValidEcho(echo)) throw new XylFTPException("ls", FTPConnection.GetStatus(), "Invaild echo.");
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 1) {
                    strRemoteFileList = FTPConnection.GetListActive();
                    ParseRemoteFileList(strRemoteFileList);
                    echo = FTPConnection.GetEcho();
                    echoMeaning = ProcessEcho(echo);
                }
            }
            TableModel dataModel = new AbstractTableModel() {

                public int getColumnCount() {
                    return 3;
                }

                public int getRowCount() {
                    return Math.max(RemoteFileList.length, TABLE_MIN_ROWS);
                }

                public boolean isCellEditable(int row, int col) {
                    return false;
                }

                public Object getValueAt(int row, int col) {
                    if (row < RemoteFileList.length) {
                        switch(col) {
                            case 0:
                                return RemoteFileList[row].Name;
                            case 1:
                                return RemoteFileList[row].IsDirectory ? "Folder" : "File";
                            case 2:
                                return RemoteFileList[row].Size;
                        }
                    }
                    return "";
                }
            };
            jTableRemoteFiles.setModel(dataModel);
            jTableRemoteFiles.getColumnModel().getColumn(0).setHeaderValue("Name");
            jTableRemoteFiles.getColumnModel().getColumn(1).setHeaderValue("Type");
            jTableRemoteFiles.getColumnModel().getColumn(2).setHeaderValue("Size");
        } finally {
            FTPConnection.CloseDataConnection();
        }
    }

    protected void UploadFile() {
        JFileChooser chooserSave = new JFileChooser();
        int returnVal = chooserSave.showOpenDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File localFile = chooserSave.getSelectedFile();
        if (!(localFile.exists() && localFile.canRead())) {
            JOptionPane.showMessageDialog(this, "The file does not exist or cannot read!", "XylFTP upload error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String echo;
        int echoMeaning;
        String HostIP;
        int HostPort;
        try {
            if (FTPConnection.IsPassive()) {
                FTPConnection.SendCommand("PASV\r\n");
                ShowCommand("PASV\r\n");
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 5) {
                    throw new XylFTPException("put", "Server refuses");
                }
                if (FTPConnection.GetStatus() == 0) {
                    throw new XylFTPException("put", "Server has closed the control connection");
                }
                HostIP = ParseIP(echo);
                HostPort = ParsePort(echo);
                FTPConnection.OpenDataConnection(HostIP, HostPort);
                FTPConnection.SetStatus(4);
                FTPConnection.SendCommand("STOR " + localFile.getName() + "\r\n");
                ShowCommand("STOR " + localFile.getName() + "\r\n");
            } else {
                String selfIp, selfPort;
                selfIp = FTPConnection.GetSelfIP();
                selfPort = FTPConnection.GetSelfPort();
                FTPConnection.ListenForDataConnection();
                FTPConnection.SendCommand("PORT " + selfIp + selfPort + "\r\n");
                ShowCommand("PORT " + selfIp + selfPort + "\r\n");
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 5) {
                    throw new XylFTPException("put", "Server refuses");
                }
                if (FTPConnection.GetStatus() == 0) {
                    throw new XylFTPException("put", "Server has closed the control connection");
                }
                FTPConnection.SetStatus(4);
                FTPConnection.SendCommand("STOR " + localFile.getName() + "\r\n");
                ShowCommand("STOR " + localFile.getName() + "\r\n");
                FTPConnection.OpenDataConnection();
            }
            echo = FTPConnection.GetEcho();
            if (!IsValidEcho(echo)) throw new XylFTPException("put", FTPConnection.GetStatus(), "Invaild echo.");
            echoMeaning = ProcessEcho(echo);
            if (echoMeaning == 1) {
                FTPConnection.SetLocalFile(localFile.getCanonicalPath());
                if (FTPConnection.IsPassive()) {
                    FTPConnection.SendFilePassive();
                } else {
                    FTPConnection.SendFileActive();
                }
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
            } else {
                throw new XylFTPException("put", "server refuses data connection");
            }
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please disconnect and connect again!", "XylFTP upload error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error" + " , the error message is: " + ex.getMessage() + " please disconnect and connect again!", "XylFTP upload error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        }
        if (FTPConnection.GetStatus() == 4) {
            FTPConnection.SetStatus(2);
        }
        try {
            FTPConnection.CloseDataConnection();
        } catch (Exception ex) {
        }
    }

    protected void DownloadFile() {
        int selectedRow = jTableRemoteFiles.getSelectedRow();
        String strFileToDown, strRemotePathAndName;
        if (FTPConnection.GetStatus() != 2 || RemoteFileList == null) {
            JOptionPane.showMessageDialog(this, "You have not connected to server!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedRow == -1 || selectedRow >= RemoteFileList.length) {
            JOptionPane.showMessageDialog(this, "You should select a file first!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (RemoteFileList[selectedRow].IsDirectory) {
            JOptionPane.showMessageDialog(this, "You can't download a folder!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        strFileToDown = RemoteFileList[selectedRow].Name;
        JFileChooser chooserSave = new JFileChooser();
        chooserSave.setSelectedFile(new File(strFileToDown));
        int returnVal = chooserSave.showSaveDialog(this);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File localFile = chooserSave.getSelectedFile();
        if (localFile.exists()) {
            int iChoice = JOptionPane.showConfirmDialog(this, "The file already exists, do you want to overwrite it?", "XylFTP download Message", JOptionPane.YES_NO_OPTION);
            if (iChoice == 1) {
                return;
            }
        }
        if (localFile.exists() && !localFile.canWrite()) {
            JOptionPane.showMessageDialog(this, "This file cannot write!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String echo;
        int echoMeaning;
        String HostIP;
        int HostPort;
        try {
            if (FTPConnection.IsPassive()) {
                FTPConnection.SendCommand("PASV\r\n");
                ShowCommand("PASV\r\n");
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 5) {
                    throw new XylFTPException("get", "Server refuses");
                }
                if (FTPConnection.GetStatus() == 0) {
                    throw new XylFTPException("get", "Server has closed the control connection");
                }
                HostIP = ParseIP(echo);
                HostPort = ParsePort(echo);
                FTPConnection.OpenDataConnection(HostIP, HostPort);
                FTPConnection.SetStatus(3);
                FTPConnection.SendCommand("RETR " + strFileToDown + "\r\n");
                ShowCommand("RETR " + strFileToDown + "\r\n");
            } else {
                String selfIp, selfPort;
                selfIp = FTPConnection.GetSelfIP();
                selfPort = FTPConnection.GetSelfPort();
                FTPConnection.ListenForDataConnection();
                FTPConnection.SendCommand("PORT " + selfIp + selfPort + "\r\n");
                ShowCommand("PORT " + selfIp + selfPort + "\r\n");
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
                if (echoMeaning == 5) {
                    throw new XylFTPException("get", "Server refuses");
                }
                if (FTPConnection.GetStatus() == 0) {
                    throw new XylFTPException("get", "Server has closed the control connection");
                }
                FTPConnection.SetStatus(3);
                FTPConnection.SendCommand("RETR " + strFileToDown + "\r\n");
                ShowCommand("RETR " + strFileToDown + "\r\n");
                FTPConnection.OpenDataConnection();
            }
            echo = FTPConnection.GetEcho();
            if (!IsValidEcho(echo)) throw new XylFTPException("ls", FTPConnection.GetStatus(), "Invaild echo.");
            echoMeaning = ProcessEcho(echo);
            if (echoMeaning == 1) {
                FTPConnection.SetLocalFile(localFile.getCanonicalPath());
                if (FTPConnection.IsPassive()) {
                    FTPConnection.GetFilePassive();
                } else {
                    FTPConnection.GetFileActive();
                }
                echo = FTPConnection.GetEcho();
                echoMeaning = ProcessEcho(echo);
            } else {
                throw new XylFTPException("put", "server refuses data connection");
            }
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please disconnect and connect again!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error" + " , the error message is: " + ex.getMessage() + " please disconnect and connect again!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        }
        if (FTPConnection.GetStatus() == 3) {
            FTPConnection.SetStatus(2);
        }
        try {
            FTPConnection.CloseDataConnection();
        } catch (Exception ex) {
        }
    }

    /** Creates new form xylftp */
    public XylFTPGUI() {
        initComponents();
        setTitle("XylFTP");
        FTPConnection = new XylFTPConnection();
        UserName = jTextFieldUser.getText();
        PassWord = new String(jPassword.getPassword());
        jTableRemoteFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ClearTable();
    }

    protected void Connect() {
        int port;
        boolean toGetEchoAgain;
        int echoMeaning;
        UserName = jTextFieldUser.getText();
        PassWord = new String(jPassword.getPassword());
        if (FTPConnection.GetStatus() != 0) {
            try {
                FTPConnection.CloseConnection();
            } catch (Exception e) {
            }
            FTPConnection.SetStatus(0);
        }
        try {
            port = Integer.parseInt(jTextPort.getText());
        } catch (Exception e) {
            port = 21;
        }
        FTPConnection.SetPort(port);
        FTPConnection.SetHost(jTextServer.getText());
        jTextCommandAndEcho.setText("");
        try {
            FTPConnection.OpenConnection();
            FTPConnection.SetStatus(1);
            echoMeaning = ProcessEcho(FTPConnection.GetEcho());
            while (echoMeaning == 6) {
                echoMeaning = ProcessEcho(FTPConnection.GetEcho());
            }
            if (FTPConnection.GetStatus() == 0) {
                throw new Exception("timeout");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error when establishing control connection with the server, the error message is: " + ex.getMessage(), "XylFTP connection error", JOptionPane.ERROR_MESSAGE);
            try {
                FTPConnection.CloseConnection();
            } catch (Exception e) {
            }
            FTPConnection.SetStatus(0);
            return;
        }
        try {
            FTPConnection.SendCommand("USER " + UserName + "\r\n");
            ShowCommand("USER " + UserName + "\r\n");
            echoMeaning = ProcessEcho(FTPConnection.GetEcho());
            while (echoMeaning == 6) {
                echoMeaning = ProcessEcho(FTPConnection.GetEcho());
            }
            if (FTPConnection.GetStatus() == 0 || echoMeaning == 5) {
                throw new Exception();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login failed!", "XylFTP connection error", JOptionPane.ERROR_MESSAGE);
            try {
                FTPConnection.CloseConnection();
            } catch (Exception e) {
            }
            FTPConnection.SetStatus(0);
            return;
        }
        try {
            if (echoMeaning == 3) {
                FTPConnection.SendCommand("PASS " + PassWord + "\r\n");
                ShowCommand("PASS " + "********" + "\r\n");
                echoMeaning = ProcessEcho(FTPConnection.GetEcho());
                while (echoMeaning == 6) {
                    echoMeaning = ProcessEcho(FTPConnection.GetEcho());
                }
                if (FTPConnection.GetStatus() == 0 || echoMeaning == 5) {
                    throw new Exception();
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login failed!", "XylFTP connection error", JOptionPane.ERROR_MESSAGE);
            try {
                FTPConnection.CloseConnection();
            } catch (Exception e) {
            }
            FTPConnection.SetStatus(0);
            return;
        }
        FTPConnection.SetStatus(2);
        try {
            StrRemotePWD = GetRemotePWD();
            jTextRemotePWD.setText(StrRemotePWD);
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error listing remote files, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please connect again!", "XylFTP connection error", JOptionPane.ERROR_MESSAGE);
            try {
                FTPConnection.CloseConnection();
            } catch (Exception e) {
            }
            FTPConnection.SetStatus(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error listing remote files" + " , the error message is: " + ex.getMessage() + " please connect again!", "XylFTP connection error", JOptionPane.ERROR_MESSAGE);
            try {
                FTPConnection.CloseConnection();
            } catch (Exception e) {
            }
            FTPConnection.SetStatus(0);
        }
    }

    protected void ClearTable() {
        TableModel emptyModel = new AbstractTableModel() {

            public int getColumnCount() {
                return 3;
            }

            public int getRowCount() {
                return TABLE_MIN_ROWS;
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public Object getValueAt(int row, int col) {
                return "";
            }
        };
        jTableRemoteFiles.setModel(emptyModel);
        jTableRemoteFiles.getColumnModel().getColumn(0).setHeaderValue("Name");
        jTableRemoteFiles.getColumnModel().getColumn(1).setHeaderValue("Type");
        jTableRemoteFiles.getColumnModel().getColumn(2).setHeaderValue("Size");
    }

    protected void Disconnect() {
        jTextRemotePWD.setText("");
        jTextCommandAndEcho.setText("");
        ClearTable();
        try {
            FTPConnection.CloseConnection();
        } catch (Exception ex) {
        }
        FTPConnection.SetStatus(0);
    }

    protected void Delete() {
        int echoMeaning;
        String echo;
        String strSel;
        if (FTPConnection.GetStatus() != 2 || RemoteFileList == null) {
            JOptionPane.showMessageDialog(this, "You have not connected to server!", "XylFTP delete error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = jTableRemoteFiles.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= RemoteFileList.length) {
            JOptionPane.showMessageDialog(this, "You should select a file first!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        strSel = RemoteFileList[selectedRow].Name;
        int iChoice = JOptionPane.showConfirmDialog(this, "Do you want to delete the " + (RemoteFileList[selectedRow].IsDirectory ? "Folder " : "File ") + strSel + "? (Remember that you can only delete empty folders)", "XylFTP download Message", JOptionPane.YES_NO_OPTION);
        if (iChoice == 1) {
            return;
        }
        try {
            String cmd, cmdUI;
            if (RemoteFileList[selectedRow].IsDirectory) {
                cmd = "RMD ";
                cmdUI = "rmdir";
            } else {
                cmd = "DELE ";
                cmdUI = "delete";
            }
            FTPConnection.SendCommand(cmd + strSel + "\r\n");
            ShowCommand(cmd + strSel + "\r\n");
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
            if (echoMeaning == 5) {
                throw new XylFTPException(cmdUI, "Server refuses");
            }
            if (FTPConnection.GetStatus() == 0) {
                throw new XylFTPException(cmdUI, " Connection does not exsit");
            }
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please disconnect and connect again!", "XylFTP delete error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error" + " , the error message is: " + ex.getMessage() + " please disconnect and connect again!", "XylFTP delete error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        }
    }

    protected void MakeDir() {
        if (FTPConnection.GetStatus() != 2 || RemoteFileList == null) {
            JOptionPane.showMessageDialog(this, "You have not connected to server!", "XylFTP makedir error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String strRenameTo = JOptionPane.showInputDialog(this, "Please input the name to rename to:");
        if (strRenameTo == null || strRenameTo.equals("")) {
            return;
        }
        String echo;
        int echoMeaning;
        try {
            FTPConnection.SendCommand("MKD " + strRenameTo + "\r\n");
            ShowCommand("MKD " + strRenameTo + "\r\n");
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
            if (echoMeaning == 5) {
                throw new XylFTPException("makedir", "Server refuses");
            }
            if (FTPConnection.GetStatus() == 0) {
                throw new XylFTPException("makedir", " Connection  not exsits");
            }
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please disconnect and connect again!", "XylFTP makedir error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error" + " , the error message is: " + ex.getMessage() + " please disconnect and connect again!", "XylFTP makedir error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        }
    }

    protected void Rename() {
        int echoMeaning;
        String echo;
        String strSel;
        if (FTPConnection.GetStatus() != 2 || RemoteFileList == null) {
            JOptionPane.showMessageDialog(this, "You have not connected to server!", "XylFTP rename error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = jTableRemoteFiles.getSelectedRow();
        if (selectedRow == -1 || selectedRow >= RemoteFileList.length) {
            JOptionPane.showMessageDialog(this, "You should select a file first!", "XylFTP rename error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        strSel = RemoteFileList[selectedRow].Name;
        String strRenameTo = JOptionPane.showInputDialog(this, "Please input the name to rename to:");
        if (strRenameTo == null || strRenameTo.equals("")) {
            return;
        }
        try {
            FTPConnection.SendCommand("RNFR " + strSel + "\r\n");
            ShowCommand("RNFR " + strSel + "\r\n");
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
            if (echoMeaning != 3) {
                throw new XylFTPException("rename", "Server refuses");
            }
            if (FTPConnection.GetStatus() == 0) {
                throw new XylFTPException("rename", " Connection  not exsits");
            }
            FTPConnection.SendCommand("RNTO " + strRenameTo + "\r\n");
            ShowCommand("RNTO " + strRenameTo + "\r\n");
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
            if (echoMeaning == 5) {
                throw new XylFTPException("rename", "Server refuses");
            }
            if (FTPConnection.GetStatus() == 0) {
                throw new XylFTPException("rename", " Connection  not exsits");
            }
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            JOptionPane.showMessageDialog(this, "There is an error, the command is: " + ex.GetCommand() + " , the error message is: " + ex.GetMessage() + " please disconnect and connect again!", "XylFTP rename error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "There is an error" + " , the error message is: " + ex.getMessage() + " please disconnect and connect again!", "XylFTP rename error", JOptionPane.ERROR_MESSAGE);
            if (FTPConnection.GetStatus() == 0) {
                try {
                    FTPConnection.CloseConnection();
                } catch (Exception e) {
                }
                ClearTable();
            }
        }
    }

    protected void Cdup() {
        int echoMeaning;
        String echo;
        if (FTPConnection.GetStatus() != 2 || RemoteFileList == null) {
            JOptionPane.showMessageDialog(this, "You have not connected to server!", "XylFTP rename error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            FTPConnection.SendCommand("CDUP " + "\r\n");
            ShowCommand("CDUP " + "\r\n");
            echo = FTPConnection.GetEcho();
            echoMeaning = ProcessEcho(echo);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            StrRemotePWD = GetRemotePWD();
            jTextRemotePWD.setText(StrRemotePWD);
            ListRemoteFiles();
        } catch (XylFTPException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initComponents() {
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextCommandAndEcho = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jLabelServer = new javax.swing.JLabel();
        jTextServer = new javax.swing.JTextField();
        jLabelUser = new javax.swing.JLabel();
        jTextFieldUser = new javax.swing.JTextField();
        jLabelPass = new javax.swing.JLabel();
        jPassword = new javax.swing.JPasswordField();
        jLabelport = new javax.swing.JLabel();
        jTextPort = new javax.swing.JTextField();
        jCkbMode = new javax.swing.JCheckBox();
        jToolBarViews = new javax.swing.JToolBar();
        jBtnConnect = new javax.swing.JButton();
        jBtnDisconnect = new javax.swing.JButton();
        jBtnUpload = new javax.swing.JButton();
        jBtnDownload = new javax.swing.JButton();
        jTextRemotePWD = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableRemoteFiles = new javax.swing.JTable();
        jButtonCdUp = new javax.swing.JButton();
        jMenuBarMain = new javax.swing.JMenuBar();
        jMenuFiles = new javax.swing.JMenu();
        jMenuItemConnect = new javax.swing.JMenuItem();
        jMenuItemDiscon = new javax.swing.JMenuItem();
        jMenuItemQuit = new javax.swing.JMenuItem();
        jMenuCommands = new javax.swing.JMenu();
        jMenuItemDownload = new javax.swing.JMenuItem();
        jMenuItemUpload = new javax.swing.JMenuItem();
        jMenuItemDelete = new javax.swing.JMenuItem();
        jMenuItemRename = new javax.swing.JMenuItem();
        jMenuItemMakedir = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemAbout = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        jTextCommandAndEcho.setColumns(20);
        jTextCommandAndEcho.setRows(5);
        jScrollPane4.setViewportView(jTextCommandAndEcho);
        jLabelServer.setText("Server");
        jTextServer.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextServerActionPerformed(evt);
            }
        });
        jLabelUser.setText("User");
        jTextFieldUser.setText("anonymous");
        jLabelPass.setText("Password");
        jPassword.setText("xylftpuser@xylftp");
        jLabelport.setText("Port");
        jTextPort.setText("21");
        jCkbMode.setSelected(true);
        jCkbMode.setText("Passive Mode");
        jCkbMode.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCkbMode.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCkbMode.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCkbModeActionPerformed(evt);
            }
        });
        jToolBarViews.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jBtnConnect.setFont(new java.awt.Font("����", 1, 12));
        jBtnConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/connect.jpg")));
        jBtnConnect.setMaximumSize(new java.awt.Dimension(21, 21));
        jBtnConnect.setMinimumSize(new java.awt.Dimension(21, 21));
        jBtnConnect.setPreferredSize(new java.awt.Dimension(187, 135));
        jBtnConnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnConnectActionPerformed(evt);
            }
        });
        jToolBarViews.add(jBtnConnect);
        jBtnDisconnect.setFont(new java.awt.Font("����", 1, 12));
        jBtnDisconnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/disconnect.JPG")));
        jBtnDisconnect.setMaximumSize(new java.awt.Dimension(21, 21));
        jBtnDisconnect.setMinimumSize(new java.awt.Dimension(21, 21));
        jBtnDisconnect.setPreferredSize(new java.awt.Dimension(187, 135));
        jBtnDisconnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDisconnectActionPerformed(evt);
            }
        });
        jToolBarViews.add(jBtnDisconnect);
        jBtnUpload.setFont(new java.awt.Font("����", 1, 12));
        jBtnUpload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/IU.JPG")));
        jBtnUpload.setMaximumSize(new java.awt.Dimension(21, 21));
        jBtnUpload.setMinimumSize(new java.awt.Dimension(21, 21));
        jBtnUpload.setPreferredSize(new java.awt.Dimension(187, 135));
        jBtnUpload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnUploadActionPerformed(evt);
            }
        });
        jToolBarViews.add(jBtnUpload);
        jBtnDownload.setFont(new java.awt.Font("����", 1, 12));
        jBtnDownload.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/ID.JPG")));
        jBtnDownload.setMaximumSize(new java.awt.Dimension(21, 21));
        jBtnDownload.setMinimumSize(new java.awt.Dimension(21, 21));
        jBtnDownload.setPreferredSize(new java.awt.Dimension(187, 135));
        jBtnDownload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDownloadActionPerformed(evt);
            }
        });
        jToolBarViews.add(jBtnDownload);
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addComponent(jLabelServer).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTextServer, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabelUser)).addComponent(jToolBarViews, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTextFieldUser, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabelPass).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabelport).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTextPort, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jCkbMode, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { jPassword, jTextFieldUser });
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabelServer).addComponent(jTextServer, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabelUser).addComponent(jTextFieldUser, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabelPass).addComponent(jPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jLabelport).addComponent(jTextPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jCkbMode)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(jToolBarViews, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] { jPassword, jTextFieldUser, jTextPort, jTextServer });
        jTextRemotePWD.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextRemotePWDActionPerformed(evt);
            }
        });
        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setBorder(null);
        jTableRemoteFiles.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jTableRemoteFiles.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        jTableRemoteFiles.setAutoscrolls(false);
        jTableRemoteFiles.setIntercellSpacing(new java.awt.Dimension(0, 0));
        jTableRemoteFiles.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        jTableRemoteFiles.setRequestFocusEnabled(false);
        jTableRemoteFiles.setShowHorizontalLines(false);
        jTableRemoteFiles.setShowVerticalLines(false);
        jTableRemoteFiles.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableRemoteFilesMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTableRemoteFiles);
        jButtonCdUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/cdup.JPG")));
        jButtonCdUp.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/cdup.JPG")));
        jButtonCdUp.setDisabledSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/Icon/cdup.JPG")));
        jButtonCdUp.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCdUpActionPerformed(evt);
            }
        });
        jMenuFiles.setText("File");
        jMenuFiles.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFilesActionPerformed(evt);
            }
        });
        jMenuItemConnect.setText("Connect");
        jMenuItemConnect.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jMenuItemConnectMouseClicked(evt);
            }
        });
        jMenuItemConnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectActionPerformed(evt);
            }
        });
        jMenuFiles.add(jMenuItemConnect);
        jMenuItemDiscon.setText("Disconnect");
        jMenuItemDiscon.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDisconActionPerformed(evt);
            }
        });
        jMenuFiles.add(jMenuItemDiscon);
        jMenuItemQuit.setText("Quit");
        jMenuItemQuit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemQuitActionPerformed(evt);
            }
        });
        jMenuFiles.add(jMenuItemQuit);
        jMenuBarMain.add(jMenuFiles);
        jMenuCommands.setText("Command");
        jMenuCommands.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCommandsActionPerformed(evt);
            }
        });
        jMenuItemDownload.setText("Download");
        jMenuItemDownload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDownloadActionPerformed(evt);
            }
        });
        jMenuCommands.add(jMenuItemDownload);
        jMenuItemUpload.setText("Upload");
        jMenuItemUpload.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemUploadActionPerformed(evt);
            }
        });
        jMenuCommands.add(jMenuItemUpload);
        jMenuItemDelete.setText("Delete");
        jMenuItemDelete.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeleteActionPerformed(evt);
            }
        });
        jMenuCommands.add(jMenuItemDelete);
        jMenuItemRename.setText("Rename");
        jMenuItemRename.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRenameActionPerformed(evt);
            }
        });
        jMenuCommands.add(jMenuItemRename);
        jMenuItemMakedir.setText("Make directory");
        jMenuItemMakedir.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMakedirActionPerformed(evt);
            }
        });
        jMenuCommands.add(jMenuItemMakedir);
        jMenuBarMain.add(jMenuCommands);
        jMenuHelp.setText("Help");
        jMenuItemAbout.setText("About");
        jMenuItemAbout.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemAbout);
        jMenuBarMain.add(jMenuHelp);
        setJMenuBar(jMenuBarMain);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE).addContainerGap()).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE).addContainerGap()).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jTextRemotePWD, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButtonCdUp, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jTextRemotePWD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(jButtonCdUp, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE).addContainerGap()));
        pack();
    }

    private void jMenuItemAboutActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showMessageDialog(this, "Copyright (C) XiYou Linux Group.", "About", JOptionPane.PLAIN_MESSAGE);
    }

    private void jButtonCdUpActionPerformed(java.awt.event.ActionEvent evt) {
        Cdup();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        Disconnect();
    }

    private void jMenuItemQuitActionPerformed(java.awt.event.ActionEvent evt) {
        Disconnect();
        System.exit(0);
    }

    private void jMenuItemUploadActionPerformed(java.awt.event.ActionEvent evt) {
        if (FTPConnection.GetStatus() == 2) UploadFile(); else {
            JOptionPane.showMessageDialog(this, "You can't upload file!", "XylFTP upload error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void jMenuCommandsActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jBtnUploadActionPerformed(java.awt.event.ActionEvent evt) {
        if (FTPConnection.GetStatus() == 2) UploadFile(); else {
            JOptionPane.showMessageDialog(this, "You can't upload file!", "XylFTP upload error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void jMenuItemDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        Delete();
    }

    private void jMenuItemDownloadActionPerformed(java.awt.event.ActionEvent evt) {
        DownloadFile();
    }

    private void jTableRemoteFilesMouseClicked(java.awt.event.MouseEvent evt) {
        int clickCount = 0;
        clickCount = evt.getClickCount();
        if (clickCount == 2) {
            if (FTPConnection.GetStatus() != 2 || RemoteFileList == null) {
                JOptionPane.showMessageDialog(this, "You have not connected to server!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int selectedRow = jTableRemoteFiles.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "You should select a file first!", "XylFTP download error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (selectedRow >= RemoteFileList.length) {
                return;
            }
            if (RemoteFileList[selectedRow].IsDirectory) {
                String strDirChoosen = RemoteFileList[selectedRow].Name;
                String strDirToEnter;
                if (StrRemotePWD.charAt(StrRemotePWD.length() - 1) == '/') {
                    strDirToEnter = StrRemotePWD + strDirChoosen;
                } else {
                    strDirToEnter = StrRemotePWD + "/" + strDirChoosen;
                }
                ChangeRemotePWD(strDirToEnter);
            } else {
                DownloadFile();
            }
        }
    }

    private void jTextRemotePWDActionPerformed(java.awt.event.ActionEvent evt) {
        ChangeRemotePWD(jTextRemotePWD.getText());
    }

    private void jBtnDisconnectActionPerformed(java.awt.event.ActionEvent evt) {
        Disconnect();
    }

    private void jTextServerActionPerformed(java.awt.event.ActionEvent evt) {
        Connect();
    }

    private void jCkbModeActionPerformed(java.awt.event.ActionEvent evt) {
        if (jCkbMode.isSelected()) {
            FTPConnection.SetPassive();
        } else {
            FTPConnection.SetActive();
        }
    }

    private void jBtnDownloadActionPerformed(java.awt.event.ActionEvent evt) {
        DownloadFile();
    }

    private void jBtnConnectActionPerformed(java.awt.event.ActionEvent evt) {
        Connect();
    }

    private void jMenuItemConnectMouseClicked(java.awt.event.MouseEvent evt) {
    }

    private void jMenuItemMakedirActionPerformed(java.awt.event.ActionEvent evt) {
        MakeDir();
    }

    private void jMenuItemRenameActionPerformed(java.awt.event.ActionEvent evt) {
        Rename();
    }

    private void jMenuItemDisconActionPerformed(java.awt.event.ActionEvent evt) {
        Disconnect();
    }

    private void jMenuItemConnectActionPerformed(java.awt.event.ActionEvent evt) {
        Connect();
    }

    private void jMenuFilesActionPerformed(java.awt.event.ActionEvent evt) {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new XylFTPGUI().setVisible(true);
            }
        });
    }

    private javax.swing.JButton jBtnConnect;

    private javax.swing.JButton jBtnDisconnect;

    private javax.swing.JButton jBtnDownload;

    private javax.swing.JButton jBtnUpload;

    private javax.swing.JButton jButtonCdUp;

    private javax.swing.JCheckBox jCkbMode;

    private javax.swing.JLabel jLabelPass;

    private javax.swing.JLabel jLabelServer;

    private javax.swing.JLabel jLabelUser;

    private javax.swing.JLabel jLabelport;

    private javax.swing.JMenuBar jMenuBarMain;

    private javax.swing.JMenu jMenuCommands;

    private javax.swing.JMenu jMenuFiles;

    private javax.swing.JMenu jMenuHelp;

    private javax.swing.JMenuItem jMenuItemAbout;

    private javax.swing.JMenuItem jMenuItemConnect;

    private javax.swing.JMenuItem jMenuItemDelete;

    private javax.swing.JMenuItem jMenuItemDiscon;

    private javax.swing.JMenuItem jMenuItemDownload;

    private javax.swing.JMenuItem jMenuItemMakedir;

    private javax.swing.JMenuItem jMenuItemQuit;

    private javax.swing.JMenuItem jMenuItemRename;

    private javax.swing.JMenuItem jMenuItemUpload;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPasswordField jPassword;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JTable jTableRemoteFiles;

    private javax.swing.JTextArea jTextCommandAndEcho;

    private javax.swing.JTextField jTextFieldUser;

    private javax.swing.JTextField jTextPort;

    private javax.swing.JTextField jTextRemotePWD;

    private javax.swing.JTextField jTextServer;

    private javax.swing.JToolBar jToolBarViews;
}

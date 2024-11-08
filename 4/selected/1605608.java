package com.onyourmind.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.StringTokenizer;
import com.onyourmind.awt.OymFrame;

public class ConnectionHandler extends Thread {

    public Socket socket;

    public DataInputStream dis;

    public DataOutputStream dos;

    public Statement statement;

    public Connection connection;

    public String username = "";

    public String password = "";

    public String databaseAlias = "";

    public String path = "";

    public PrintWriter log = null;

    public boolean isOracle = true;

    public boolean isSQLServer = false;

    public String dbClass = "oracle.jdbc.driver.OracleDriver";

    public String dbPrefix = "jdbc:oracle:oci7:";

    public boolean isMicrosoft = false;

    public static final String slash = "&&SLASH&&";

    public static final int SAVE_METHOD_FILE = OymFrame.SAVE_METHOD_FILE;

    public static final int SAVE_METHOD_DATABASE = OymFrame.SAVE_METHOD_DATABASE;

    private int saveMethod = SAVE_METHOD_FILE;

    boolean isSpecial = false;

    private int contentLength = 0;

    public String modelType = null;

    public ConnectionHandler(Socket pSocket, String strUsername, String strPassword, String strPath, String strDatabaseAlias, PrintWriter psLog, String strDatabaseType) {
        super();
        socket = pSocket;
        username = strUsername;
        password = strPassword;
        databaseAlias = strDatabaseAlias;
        path = strPath;
        log = psLog;
        isOracle = (strDatabaseType == null || strDatabaseType.equals("Oracle"));
        if (!isOracle) {
            isMicrosoft = (System.getProperty("java.vendor").indexOf("Microsoft") != -1);
            if (isMicrosoft) dbClass = "com.ms.jdbc.odbc.JdbcOdbcDriver"; else dbClass = "sun.jdbc.odbc.JdbcOdbcDriver";
            dbPrefix = "jdbc:odbc:";
        }
        if (strDatabaseType != null && strDatabaseType.equals("SQLServer")) {
            isSQLServer = true;
            dbClass = "weblogic.jdbc.dblib.Driver";
            dbPrefix = "jdbc:weblogic:mssqlserver";
        }
        if (strDatabaseType != null && strDatabaseType.equals("hSQL")) {
            dbClass = "org.hsql.jdbcDriver";
            dbPrefix = "jdbc:HypersonicSQL:";
        }
    }

    public void connectToDatabase() throws Exception {
        Class.forName(dbClass);
        if (databaseAlias == null) databaseAlias = "";
        if (isOracle && databaseAlias.length() > 0) databaseAlias = "@" + databaseAlias;
        if (isOracle) connection = DriverManager.getConnection(dbPrefix + username + "/" + password + databaseAlias); else if (isSQLServer) {
            Properties props = new Properties();
            props.put("user", username);
            props.put("password", password);
            props.put("server", "johndell300");
            props.put("db", databaseAlias);
            connection = DriverManager.getConnection(dbPrefix, props);
        } else connection = DriverManager.getConnection(dbPrefix + databaseAlias, username, password);
        statement = connection.createStatement();
    }

    public void run() {
        try {
            InputStream in = new BufferedInputStream(socket.getInputStream());
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            dis = new DataInputStream(in);
            dos = new DataOutputStream(out);
            processClientRequest();
        } catch (Exception e) {
            errorHandling("Socket close for connection failed", e);
        }
    }

    public void processClientRequest() {
        try {
            byte[] byteArray = new byte[4];
            int nReadResult = dis.read(byteArray);
            if (nReadResult != byteArray.length) {
                errorHandling("Not enough bytes sent.");
                return;
            }
            String strBytes = new String(byteArray);
            if (strBytes.equals("GET ") || strBytes.equals("POST")) {
                handleHTTPRequest();
                return;
            }
            int nRequest = getIntFromBytes(byteArray);
            if (nRequest > 6 || getSaveMethod() == SAVE_METHOD_DATABASE) connectToDatabase();
            if (isSpecial) errorHandling("Socket request type (nRequest): " + nRequest + " of length " + contentLength);
            handleRequest(nRequest);
            dos.close();
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (Exception ex) {
            errorHandling("Client request processing failed", ex);
        }
    }

    public void handleRequest(int nRequest) throws Exception {
        switch(nRequest) {
            case 1:
                upOneLevel();
                break;
            case 2:
                createNewFolder();
                break;
            case 3:
                openFile();
                break;
            case 4:
                saveFile();
                break;
            case 5:
                getPathSeparator();
                break;
            case 6:
                delete();
                break;
            case 8:
                login();
                break;
            case 13:
                changePassword();
                break;
            case 14:
                addUser();
                break;
            case 15:
                removeUser();
                break;
            case 16:
                userList();
                break;
            case 27:
                customQuery();
                break;
            case 28:
                customUpdate();
                break;
        }
    }

    public int getIntFromBytes(byte[] byteArray) {
        return (byteArray[0] << 24) | (byteArray[1] << 16) + (byteArray[2] << 8) + byteArray[3];
    }

    @SuppressWarnings("deprecation")
    public void handleHTTPRequest() {
        String ct;
        String version = "";
        File theFile;
        File docroot = new File(path);
        String indexfile = "index.html";
        String strHeaderLine = null;
        try {
            PrintStream os = new PrintStream(socket.getOutputStream());
            String strFirstLine = dis.readLine();
            isSpecial = (strFirstLine.indexOf("special.htm") != -1);
            if (strFirstLine.indexOf("support.htm") != -1) {
                os.print("<HTML><HEAD><TITLE>Support</TITLE></HEAD>\r\n");
                os.print("<BODY><P><H1>Status</H1></P>The server has now been restarted.  We apologize for any inconvenience.</BODY></HTML>\r\n");
                os.close();
                dos.close();
                System.exit(0);
                return;
            }
            StringTokenizer st = new StringTokenizer(strFirstLine);
            if (true) {
                String file = st.nextToken();
                if (file.endsWith("/")) file += indexfile;
                ct = guessContentTypeFromName(file);
                if (st.hasMoreTokens()) version = st.nextToken();
                while ((strHeaderLine = dis.readLine()) != null) {
                    if (isSpecial) {
                        setContentLength(strHeaderLine);
                        setModelType(strHeaderLine);
                    }
                    if (strHeaderLine.trim().equals("")) break;
                }
                if (isSpecial) {
                    processClientRequest();
                    return;
                }
                try {
                    theFile = new File(docroot, file.substring(1, file.length()));
                    FileInputStream fis = new FileInputStream(theFile);
                    byte[] theData = new byte[(int) theFile.length()];
                    fis.read(theData);
                    fis.close();
                    if (version.startsWith("HTTP/")) {
                        os.print("HTTP/1.0 200 OK\r\n");
                        Date now = new Date();
                        os.print("Date: " + now + "\r\n");
                        os.print("Server: jhttp 1.0\r\n");
                        os.print("Content-length: " + theData.length + "\r\n");
                        os.print("Content-type: " + ct + "\r\n\r\n");
                    }
                    os.write(theData);
                    os.close();
                    dos.close();
                } catch (IOException e) {
                    if (version.startsWith("HTTP/")) {
                        os.print("HTTP/1.0 404 File Not Found\r\n");
                        Date now = new Date();
                        os.print("Date: " + now + "\r\n");
                        os.print("Server: jhttp 1.0\r\n");
                        os.print("Content-type: text/html" + "\r\n\r\n");
                    }
                    os.print("<HTML><HEAD><TITLE>File Not Found</TITLE></HEAD>\r\n");
                    os.print("<BODY><H1>HTTP Error 404: File Not Found</H1></BODY></HTML>\r\n");
                    os.close();
                    dos.close();
                }
            } else {
                if (version.startsWith("HTTP/")) {
                    os.print("HTTP/1.0 501 Not Implemented\r\n");
                    Date now = new Date();
                    os.print("Date: " + now + "\r\n");
                    os.print("Server: jhttp 1.0\r\n");
                    os.print("Content-type: text/html" + "\r\n\r\n");
                }
                os.println("<HTML><HEAD><TITLE>Not Implemented</TITLE></HEAD>");
                os.println("<BODY><H1>HTTP Error 501: Not Implemented</H1></BODY></HTML>");
                os.close();
            }
        } catch (IOException e) {
        }
        try {
            socket.close();
        } catch (Exception e) {
            errorHandling("Socket close for HTTP connection failed", e);
        }
    }

    public String guessContentTypeFromName(String name) {
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html"; else if (name.endsWith(".txt") || name.endsWith(".java")) return "text/plain"; else if (name.endsWith(".gif")) return "image/gif"; else if (name.endsWith(".class")) return "application/octet-stream"; else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg"; else return "text/plain";
    }

    private void setModelType(String strLine) {
        if (!strLine.startsWith("Model-Type")) return;
        StringTokenizer tok = new StringTokenizer(strLine);
        tok.nextToken();
        modelType = tok.nextToken();
    }

    private void setContentLength(String strLine) {
        int nLength = contentLength(strLine);
        if (nLength > 0) contentLength = nLength;
    }

    private int contentLength(String strLine) {
        strLine = strLine.toUpperCase();
        if (strLine.startsWith("CONTENT-LENGTH")) return (getLength(strLine));
        return 0;
    }

    private int getLength(String strLine) {
        StringTokenizer tok = new StringTokenizer(strLine);
        tok.nextToken();
        return (Integer.parseInt(tok.nextToken()));
    }

    public void errorHandling(String strPrimaryMessage) {
        errorHandling(strPrimaryMessage, (String) null);
    }

    public void errorHandling(String strPrimaryMessage, String strExtendedMessage) {
        strPrimaryMessage = strPrimaryMessage + " : ";
        if (strExtendedMessage == null) {
            System.out.println(strPrimaryMessage);
            if (log != null) log.println(strPrimaryMessage);
            return;
        }
        System.out.print(strPrimaryMessage);
        if (log != null) log.print(strPrimaryMessage);
        System.out.println(strExtendedMessage);
        if (log != null) log.println(strExtendedMessage);
    }

    public void errorHandling(String strPrimaryMessage, Exception ex) {
        errorHandling(strPrimaryMessage, ex.getClass().getName() + " - " + ex.getMessage());
    }

    public void errorHandling(String strPrimaryMessage, boolean bTime) {
        strPrimaryMessage += (" at " + (new java.util.Date(System.currentTimeMillis())).toString());
        errorHandling(strPrimaryMessage);
    }

    public String getFileNameFromPath(String strPath) {
        int nLastSeparator = strPath.lastIndexOf(File.separator);
        if (nLastSeparator != -1) return strPath.substring(nLastSeparator + 1);
        return strPath;
    }

    public String getFolderFromPath(String strPath) {
        int nLastSeparator = strPath.lastIndexOf(File.separator);
        if (nLastSeparator != -1) return strPath.substring(0, nLastSeparator);
        return strPath;
    }

    public String prependPathAndReplaceSlashes(String strFile) {
        if (getSaveMethod() == SAVE_METHOD_FILE) strFile = path + strFile;
        return replaceSlashes(strFile);
    }

    public boolean isDirectory(String strFolderName) {
        try {
            if (getSaveMethod() == SAVE_METHOD_FILE) {
                File fileDir = new File(strFolderName);
                return fileDir.isDirectory();
            }
            String strSQL = "SELECT * FROM SAVED_FILE WHERE FILE_FOLDER='" + replaceSingleQuote(getFolderFromPath(strFolderName)) + "' AND FILE_NAME='" + replaceSingleQuote(getFileNameFromPath(strFolderName)) + "' AND FILE_TYPE=0";
            ResultSet rset = queryDatabase(strSQL);
            if (rset == null || !rset.next()) return false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean mkdir(String strFolderName) {
        try {
            if (getSaveMethod() == SAVE_METHOD_FILE) {
                File fileDir = new File(strFolderName);
                return fileDir.mkdir();
            }
            String strSQL = "INSERT INTO SAVED_FILE (FILE_TYPE, FILE_FOLDER, FILE_NAME) VALUES (0, '" + replaceSingleQuote(getFolderFromPath(strFolderName)) + "', '" + replaceSingleQuote(getFileNameFromPath(strFolderName)) + "')";
            statement.execute(strSQL);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isFile(String strFileName) {
        try {
            if (getSaveMethod() == SAVE_METHOD_FILE) {
                File file = new File(strFileName);
                return file.isFile();
            }
            String strSQL = "SELECT * FROM SAVED_FILE WHERE FILE_FOLDER='" + replaceSingleQuote(getFolderFromPath(strFileName)) + "' AND FILE_NAME='" + replaceSingleQuote(getFileNameFromPath(strFileName)) + "' AND FILE_TYPE=1";
            ResultSet rset = queryDatabase(strSQL);
            if (rset != null && rset.next()) return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean canRead(String strFileName) {
        if (getSaveMethod() == SAVE_METHOD_FILE) {
            File file = new File(strFileName);
            return file.canRead();
        }
        return isFile(strFileName);
    }

    public void upOneLevel() {
        try {
            String strNewLevel = upOneLevel(dis.readUTF());
            dos.writeUTF(strNewLevel);
            writeFileListToSocket(prependPathAndReplaceSlashes(strNewLevel));
        } catch (Exception e) {
            errorHandling("UpOneLevel failed");
        }
    }

    public String upOneLevel(String strCurrentLevel) {
        try {
            int nLastSeparator = strCurrentLevel.lastIndexOf(File.separator);
            String strNewLevel = strCurrentLevel;
            if (nLastSeparator != -1) strNewLevel = strCurrentLevel.substring(0, nLastSeparator);
            String strNewLevelNoSlashes = prependPathAndReplaceSlashes(strNewLevel);
            if (getSaveMethod() == SAVE_METHOD_FILE && !isDirectory(strNewLevelNoSlashes)) mkdir(strNewLevelNoSlashes);
            return strNewLevel;
        } catch (Exception e) {
            errorHandling("UpOneLevel(String) failed");
        }
        return strCurrentLevel;
    }

    public String replaceSlashes(String strName) {
        int nPos = strName.indexOf('/');
        while (nPos != -1) {
            String strFirst = strName.substring(0, nPos);
            String strLast = strName.substring(nPos + 1);
            strName = strFirst + slash + strLast;
            nPos = strName.indexOf('/');
        }
        return strName.replace(' ', '_');
    }

    public String insertSlashes(String strName) {
        int nPos = strName.indexOf(slash);
        while (nPos != -1) {
            String strFirst = strName.substring(0, nPos);
            String strLast = strName.substring(nPos + slash.length());
            strName = strFirst + "/" + strLast;
            nPos = strName.indexOf(slash);
        }
        return strName.replace('_', ' ');
    }

    public void getPathSeparator() {
        try {
            dos.writeUTF(File.separator);
        } catch (Exception e) {
            errorHandling("GetPathSeparator failed");
        }
    }

    public java.util.List<String> getFileList(String strFolderName) {
        try {
            if (getSaveMethod() == SAVE_METHOD_FILE) {
                File fileDir = new File(strFolderName);
                return Arrays.asList(fileDir.list());
            }
            java.util.List<String> list = new ArrayList<String>();
            String strSQL = "SELECT FILE_NAME FROM SAVED_FILE WHERE FILE_FOLDER='" + replaceSingleQuote(strFolderName) + "'";
            ResultSet rset = queryDatabase(strSQL);
            while (rset.next()) {
                String strName = rset.getString(1);
                if (strName != null && strName.length() > 0) list.add(strName);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeFileListToSocket(String strFolderName) {
        try {
            java.util.List<String> fileList = getFileList(strFolderName);
            dos.writeInt(fileList.size());
            for (String file : fileList) {
                if (isDirectory(strFolderName + File.separator + file)) file = "[Folder] " + file;
                dos.writeUTF(insertSlashes(file));
            }
        } catch (Exception e) {
            errorHandling("WriteFileListToSocket failed");
        }
    }

    public void createNewFolder() {
        try {
            String strFolderName = prependPathAndReplaceSlashes(dis.readUTF());
            if (!mkdir(strFolderName)) {
                dos.writeInt(0);
                return;
            }
            dos.writeInt(1);
        } catch (Exception e) {
            errorHandling("CreateNewFolder failed");
        }
    }

    public void openFile() {
        try {
            String strFile = prependPathAndReplaceSlashes(dis.readUTF());
            if (isDirectory(strFile)) {
                dos.writeInt(1);
                writeFileListToSocket(strFile);
                return;
            }
            if (isFile(strFile) && canRead(strFile)) {
                dos.writeInt(2);
                load(strFile);
                return;
            }
            dos.writeInt(0);
        } catch (Exception e) {
            errorHandling("OpenFile failed");
        }
    }

    public InputStream getBinaryStream(String strFileName) {
        try {
            String strSQL = "SELECT FILE_DATA FROM SAVED_FILE WHERE FILE_FOLDER='" + replaceSingleQuote(getFolderFromPath(strFileName)) + "' AND FILE_NAME='" + replaceSingleQuote(getFileNameFromPath(strFileName)) + "' AND FILE_TYPE=1";
            ResultSet rset = queryDatabase(strSQL);
            if (rset != null && rset.next()) return rset.getBinaryStream(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void load(String strFileName) {
        InputStream isCurrent = null;
        DataInputStream disCurrent = null;
        try {
            if (getSaveMethod() == SAVE_METHOD_FILE) isCurrent = new FileInputStream(strFileName); else isCurrent = getBinaryStream(strFileName);
            disCurrent = new DataInputStream(isCurrent);
            while (true) dos.writeByte(disCurrent.readByte());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                dos.close();
                if (disCurrent != null) disCurrent.close();
                if (isCurrent != null) isCurrent.close();
            } catch (IOException ioe) {
            }
        }
    }

    public void saveFile() {
        try {
            setSaveMethod(dis.readInt());
            save(dis.readUTF());
        } catch (Exception e) {
            errorHandling("SaveFile failed");
        }
    }

    public void save(String strFileName) {
        int nExcelModelID = -1;
        try {
            nExcelModelID = dis.readInt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (getSaveMethod() == SAVE_METHOD_DATABASE) {
            saveToDatabase(strFileName, nExcelModelID);
            return;
        }
        FileOutputStream fosCurrent = null;
        DataOutputStream dosCurrent = null;
        try {
            upOneLevel(strFileName);
            fosCurrent = new FileOutputStream(prependPathAndReplaceSlashes(strFileName));
            dosCurrent = new DataOutputStream(fosCurrent);
            if (isSpecial) {
                int nByteCount = 4 + 4 + 4 + strFileName.length();
                while (nByteCount++ < contentLength) dosCurrent.writeByte(dis.readByte());
                if (dosCurrent != null) dosCurrent.close();
                if (fosCurrent != null) fosCurrent.close();
            } else while (true) dosCurrent.writeByte(dis.readByte());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (dosCurrent != null) dosCurrent.close();
                if (fosCurrent != null) fosCurrent.close();
                dis.close();
                dos.close();
            } catch (IOException ioe) {
            }
        }
    }

    public void saveToDatabase(String strFileName, int nExcelModelID) {
        String strPathName = "", strFileNameOnly = "", strFolderName = "";
        try {
            int nByteCount = contentLength - (4 + 4 + 4 + strFileName.length());
            strPathName = prependPathAndReplaceSlashes(strFileName);
            strFileNameOnly = getFileNameFromPath(strPathName);
            strFolderName = getFolderFromPath(strPathName);
            if (!isFile(strPathName)) statement.execute("INSERT INTO SAVED_FILE (FILE_TYPE, FILE_FOLDER, FILE_NAME, EXCELMODELID) VALUES (1, '" + replaceSingleQuote(strFolderName) + "', '" + replaceSingleQuote(strFileNameOnly) + "', " + nExcelModelID + " )");
            String strSQL = "UPDATE SAVED_FILE SET FILE_DATA = ?, FILE_SIZE = " + nByteCount + " WHERE FILE_FOLDER = '" + replaceSingleQuote(strFolderName) + "' AND FILE_NAME = '" + replaceSingleQuote(strFileNameOnly) + "' AND FILE_TYPE=1";
            PreparedStatement psNew = connection.prepareStatement(strSQL);
            psNew.setBinaryStream(1, dis, nByteCount);
            psNew.execute();
            statement.executeUpdate("UPDATE EXCELMODEL SET TEMPLATECREATED = -1 WHERE MODELID = " + nExcelModelID);
            dis.close();
        } catch (Exception e) {
            try {
                statement.execute("DELETE FROM SAVED_FILE WHERE FILE_TYPE=1 AND FILE_FOLDER='" + replaceSingleQuote(strFolderName) + "' AND FILE_NAME='" + replaceSingleQuote(strFileNameOnly) + "' AND EXCELMODELID=" + nExcelModelID);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public void delete() {
        try {
            deleteFile(prependPathAndReplaceSlashes(dis.readUTF()));
            writeFileListToSocket(prependPathAndReplaceSlashes(dis.readUTF()));
        } catch (Exception e) {
            errorHandling("Delete failed");
        }
    }

    public void deleteFile(String strFileName) {
        try {
            if (getSaveMethod() == SAVE_METHOD_FILE) new File(strFileName).delete(); else {
                String strSQL = "DELETE FROM SAVED_FILE WHERE FILE_FOLDER='" + replaceSingleQuote(getFolderFromPath(strFileName)) + "' AND FILE_NAME='" + replaceSingleQuote(getFileNameFromPath(strFileName)) + "'";
                statement.execute(strSQL);
            }
        } catch (Exception e) {
            errorHandling("Delete failed");
        }
    }

    public void removeUser() {
    }

    public void userList() {
    }

    public String getDateString() {
        GregorianCalendar gcNow = new GregorianCalendar();
        if (isOracle) return "TO_DATE('" + (gcNow.get(Calendar.YEAR)) + "-" + (gcNow.get(Calendar.MONTH) + 1) + "-" + gcNow.get(Calendar.DATE) + "', 'YYYY-MM-DD')";
        return "'" + gcNow.get(Calendar.DATE) + "/" + (gcNow.get(Calendar.MONTH) + 1) + "/" + (gcNow.get(Calendar.YEAR)) + "'";
    }

    public static String replaceSingleQuote(String strAffiliate) {
        int nPos = strAffiliate.indexOf("'");
        if (nPos == -1) return strAffiliate;
        return replaceSingleQuote(strAffiliate.substring(0, nPos)) + "''" + replaceSingleQuote(strAffiliate.substring(nPos + 1, strAffiliate.length()));
    }

    public void addUser() {
    }

    public long getAutoNumber(String strField, String strTable) {
        try {
            ResultSet rset = queryDatabase("SELECT MAX(" + strField + ") FROM " + strTable);
            if (rset.next()) return (rset.getLong(1) + 1);
        } catch (Exception e) {
            errorHandling("getAutoNumber failed", e);
        }
        return 1;
    }

    public void changePassword() {
    }

    public void numberedFetch(String strQuery) throws Exception {
        if (strQuery == null || strQuery.length() == 0) return;
        ResultSet rset = queryDatabase(strQuery);
        while (rset.next()) {
            String strNext = rset.getString(1);
            if (strNext == null) strNext = "";
            dos.writeUTF(strNext);
        }
    }

    public void login() {
    }

    public ResultSet userQuery(String strUserName, String strPassword) throws Exception {
        return queryDatabase("SELECT * FROM USER WHERE (USER_CD='" + strUserName + "' AND PSWRD_CD='" + strPassword + "' AND ENBLD_CD=1)");
    }

    public int authenticate(String strUserName, String strPassword) {
        return 0;
    }

    public ResultSet queryDatabase(String strQuery) throws Exception {
        return statement.executeQuery(strQuery);
    }

    public void customQuery() {
        try {
            ResultSet rset = queryDatabase(dis.readUTF());
            java.util.List<Object[]> list = new ArrayList<Object[]>();
            int nColumnCount = rset.getMetaData().getColumnCount();
            while (rset.next()) {
                Object[] objArray = new Object[nColumnCount];
                for (int nCol = 0; nCol < nColumnCount; nCol++) objArray[nCol] = rset.getObject(nCol + 1);
                list.add(objArray);
            }
            ObjectOutputStream oos = new ObjectOutputStream(dos);
            oos.writeObject(list);
            dos.flush();
        } catch (Exception e) {
            errorHandling("CustomQuery failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void customUpdate() {
        try {
            ObjectInputStream ois = new ObjectInputStream(dis);
            java.util.List<String> updateSql = (java.util.List<String>) ois.readObject();
            for (String sql : updateSql) {
                statement.executeUpdate(sql);
            }
            dos.writeBoolean(true);
        } catch (Exception e) {
            try {
                dos.writeBoolean(false);
                dos.writeUTF(e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            errorHandling("CustomUpdate failed", e);
        }
    }

    public int getSaveMethod() {
        return saveMethod;
    }

    public void setSaveMethod(int nSaveMethod) {
        saveMethod = nSaveMethod;
    }
}

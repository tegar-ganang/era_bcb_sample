package org.cofax.cms;

import org.cofax.*;
import javax.servlet.http.*;
import java.util.*;
import org.apache.commons.net.ftp.*;
import java.io.*;

/**
 *  CofaxToolsFTP: Handles FTP transactions and menus for Cofax Tools to allow
 *  access to templates and images on remote file systems.
 *
 *@author     Charles Harvey
 *@created    May 2, 2002
 */
public class CofaxToolsFTP {

    /**
     *  Handles media upload associated with articles from the user desktop to
     *  the tools server, then on to the live servers.
     *
     *@param  articleImageName  Description of the Parameter
     *@param  mediaName         Description of the Parameter
     *@param  year              Description of the Parameter
     *@param  month             Description of the Parameter
     *@param  day               Description of the Parameter
     *@param  db                Description of the Parameter
     *@param  session           Description of the Parameter
     *@return                   Description of the Return Value
     */
    public static String uploadArticleMedia(String localPath, String articleImageName, String year, String month, String day, DataStore db, HttpSession session) {
        CofaxToolsUser user = (CofaxToolsUser) session.getAttribute("user");
        if (!localPath.endsWith(File.separator)) {
            localPath += File.separator;
        }
        FTPClient ftp = new FTPClient();
        String liveFTPLogin = (String) user.workingPubConfigElementsHash.get("LIVEFTPLOGIN");
        String liveFTPPassword = (String) user.workingPubConfigElementsHash.get("LIVEFTPPASSWORD");
        String liveImagesServer = (String) user.workingPubConfigElementsHash.get("LIVEFTPSERVER");
        String liveImagesFolder = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESFOLDER");
        if (!liveImagesFolder.endsWith("/")) {
            liveImagesFolder = liveImagesFolder + "/";
        }
        String liveImagesYearFolder = "";
        String liveImagesMonthFolder = "";
        String fileLocation = "";
        fileLocation += "/" + year + "/" + month + "/" + day;
        liveImagesYearFolder = liveImagesFolder + year;
        liveImagesMonthFolder = (liveImagesYearFolder + "/" + month);
        liveImagesFolder = (liveImagesMonthFolder + "/" + day);
        CofaxToolsUtil.log("CofaxToolsFTP: liveImagesServer: " + liveImagesServer);
        CofaxToolsUtil.log("CofaxToolsFTP: liveImagesFolder: " + liveImagesFolder);
        boolean stored = false;
        ArrayList servers = splitServers(liveImagesServer);
        for (int count = 0; count < servers.size(); count++) {
            String server = (String) servers.get(count);
            try {
                int reply;
                ftp.connect(server);
                CofaxToolsUtil.log("CofaxToolsFTP: uploadArticleMedia connecting to server : " + server);
                CofaxToolsUtil.log("CofaxToolsFTP: uploadArticleMedia results: " + ftp.getReplyString());
                CofaxToolsUtil.log(ftp.getReplyString());
                reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftp.disconnect();
                    return ("CofaxToolsFTP uploadArticleMedia ERROR: FTP server refused connection.");
                } else {
                    ftp.login(liveFTPLogin, liveFTPPassword);
                }
                try {
                    ftp.setFileType(FTP.IMAGE_FILE_TYPE);
                    InputStream input;
                    CofaxToolsUtil.log("CofaxToolsFTP: opening file stream: " + localPath + articleImageName);
                    input = new FileInputStream(localPath + articleImageName);
                    CofaxToolsUtil.log("CofaxToolsFTP: attempting to change working directory to: " + liveImagesFolder);
                    boolean changed = ftp.changeWorkingDirectory(liveImagesFolder);
                    CofaxToolsUtil.log("CofaxToolsFTP: uploadArticleMedia results: " + changed);
                    if (changed == false) {
                        CofaxToolsUtil.log("CofaxToolsFTP: uploadArticleMedia attempting to create directory :" + liveImagesFolder);
                        boolean newDirYear = ftp.makeDirectory(liveImagesYearFolder);
                        boolean newDirMonth = ftp.makeDirectory(liveImagesMonthFolder);
                        boolean newDir = ftp.makeDirectory(liveImagesFolder);
                        CofaxToolsUtil.log("CofaxToolsFTP: uploadArticleMedia results: YearDir: " + newDirYear + " MonthDir: " + newDirMonth + " finalDir: " + newDir);
                        changed = ftp.changeWorkingDirectory(liveImagesFolder);
                    }
                    if (changed) {
                        CofaxToolsUtil.log("CofaxToolsFTP: storing " + articleImageName + " to " + liveImagesFolder);
                        stored = ftp.storeFile(articleImageName, input);
                    } else {
                        CofaxToolsUtil.log("CofaxToolsFTP: failed changing: " + liveImagesFolder);
                    }
                    if (stored) {
                        CofaxToolsUtil.log("CofaxToolsFTP: Successfully ftped file.");
                    } else {
                        CofaxToolsUtil.log("CofaxToolsFTP: Failed ftping file.");
                    }
                    input.close();
                    ftp.logout();
                    ftp.disconnect();
                } catch (org.apache.commons.net.io.CopyStreamException e) {
                    CofaxToolsUtil.log("CofaxToolsFTP: Failed ftping file." + e.toString());
                    CofaxToolsUtil.log("CofaxToolsFTP: " + e.getIOException().toString());
                    return ("Cannot upload file " + liveImagesFolder + "/" + articleImageName);
                } catch (IOException e) {
                    CofaxToolsUtil.log("CofaxToolsFTP: Failed ftping file." + e.toString());
                    return ("Cannot upload file " + liveImagesFolder + "/" + articleImageName);
                } catch (Exception e) {
                    CofaxToolsUtil.log("CofaxToolsFTP: Failed ftping file." + e.toString());
                    return ("Cannot upload file " + liveImagesFolder + "/" + articleImageName);
                }
            } catch (IOException e) {
                return ("Could not connect to server: " + e);
            }
        }
        return ("");
    }

    /**
     *  Returns String filename after stripping filepath from original String.
     *
     *@param  mediaName  Description of the Parameter
     *@return            the filename
     */
    public static String stripPath(String mediaName) {
        int forwardSlash = mediaName.lastIndexOf("/");
        int backSlash = mediaName.lastIndexOf("\\");
        if (forwardSlash > -1) {
            mediaName = mediaName.substring(forwardSlash + 1);
        }
        if (backSlash > -1) {
            mediaName = mediaName.substring(backSlash + 1);
        }
        return (mediaName);
    }

    /**
     *  Returns String folderpath after stripping filename from original String.
     *
     *@param  mediaName  Description of the Parameter
     *@return            Description of the Return Value
     */
    public static String stripName(String mediaName) {
        int forwardSlash = mediaName.lastIndexOf("/");
        int backSlash = mediaName.lastIndexOf("\\");
        if (forwardSlash > -1) {
            mediaName = mediaName.substring(0, forwardSlash + 1);
        }
        if (backSlash > -1) {
            mediaName = mediaName.substring(0, backSlash + 1);
        }
        return (mediaName);
    }

    /**
     *  Splits a String of comma separated servers (or any String) and returns
     *  an arrayList. Returns populated with one server or original String if no
     *  commas exist in String.
     *
     *@param  servers  Description of the Parameter
     *@return          Description of the Return Value
     */
    public static ArrayList splitServers(String servers) {
        ArrayList al = new ArrayList();
        if ((servers != null) && (!servers.equals(""))) {
            if (servers.indexOf(",") > -1) {
                StringTokenizer st = new StringTokenizer(servers, ",");
                while (st.hasMoreTokens()) {
                    al.add(st.nextToken(","));
                }
            } else {
                al.add(servers);
            }
            return (al);
        } else {
            al.add("NoServerListedInConfig");
            return (al);
        }
    }

    /**
     *  Returns a String[] of any files that exist in the current publications'
     *  import folder.
     *
     *@param  session  Description of the Parameter
     *@return          Description of the Return Value
     */
    public static String[] viewFilesToImport(HttpSession session) {
        FTPClient ftp = new FTPClient();
        CofaxToolsUser user = (CofaxToolsUser) session.getAttribute("user");
        String importFTPServer = (String) user.workingPubConfigElementsHash.get("IMPORTFTPSERVER") + "";
        String importFTPLogin = (String) user.workingPubConfigElementsHash.get("IMPORTFTPLOGIN") + "";
        String importFTPPassword = (String) user.workingPubConfigElementsHash.get("IMPORTFTPPASSWORD") + "";
        String importFTPFilePath = (String) user.workingPubConfigElementsHash.get("IMPORTFTPFILEPATH");
        String[] dirList = null;
        if (importFTPServer.equals("") || importFTPLogin.equals("") || importFTPPassword.equals("")) {
            return dirList;
        }
        boolean loggedIn = false;
        try {
            int reply;
            ftp.connect(importFTPServer);
            CofaxToolsUtil.log("CofaxToolsFTP viewFilesToImport connecting: " + ftp.getReplyString());
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.logout();
                ftp.disconnect();
                CofaxToolsUtil.log("CofaxToolsFTP viewFilesToImport ERROR: FTP server refused connection.");
            } else {
                loggedIn = ftp.login(importFTPLogin, importFTPPassword);
                CofaxToolsUtil.log("CofaxToolsFTP viewFilesToImport Logging in: " + importFTPLogin + " " + importFTPPassword);
            }
            if (loggedIn) {
                try {
                    ftp.changeWorkingDirectory(importFTPFilePath);
                    CofaxToolsUtil.log("CofaxToolsFTP viewFilesToImport changing dir: " + importFTPFilePath);
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        CofaxToolsUtil.log("ERROR: cannot change directory");
                    }
                    FTPFile[] remoteFileList = ftp.listFiles();
                    ArrayList tmpArray = new ArrayList();
                    for (int i = 0; i < remoteFileList.length; i++) {
                        FTPFile testFile = remoteFileList[i];
                        if (testFile.getType() == FTP.ASCII_FILE_TYPE) {
                            tmpArray.add(testFile.getName());
                        }
                    }
                    dirList = (String[]) tmpArray.toArray(new String[0]);
                    ftp.logout();
                    ftp.disconnect();
                } catch (java.io.IOException e) {
                    CofaxToolsUtil.log("CofaxToolsFTP viewFilesToImport cannot read directory: " + importFTPFilePath);
                }
            }
        } catch (IOException e) {
            CofaxToolsUtil.log("CofaxToolsFTP viewFilesToImport could not connect to server: " + e);
        }
        return (dirList);
    }

    /**
     *  Retrieves a file via FTP from the import server, writes the file to the
     *  local transfer folder, reads in file contents, and returns the body as a
     *  string so it may be placed into the creat article tool.
     *
     *@param  fileName  Description of the Parameter
     *@param  session   Description of the Parameter
     *@return           The importFileBody value
     */
    public static String getImportFileBody(String fileName, HttpSession session) {
        FTPClient ftp = new FTPClient();
        CofaxToolsUser user = (CofaxToolsUser) session.getAttribute("user");
        String fileTransferFolder = CofaxToolsServlet.fileTransferFolder;
        String importFTPServer = (String) user.workingPubConfigElementsHash.get("IMPORTFTPSERVER");
        String importFTPLogin = (String) user.workingPubConfigElementsHash.get("IMPORTFTPLOGIN");
        String importFTPPassword = (String) user.workingPubConfigElementsHash.get("IMPORTFTPPASSWORD");
        String importFTPFilePath = (String) user.workingPubConfigElementsHash.get("IMPORTFTPFILEPATH");
        String body = ("");
        try {
            int reply;
            ftp.connect(importFTPServer);
            CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody connecting to server " + importFTPServer);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return ("CofaxToolsFTP getImportFileBody ERROR: FTP server refused connection.");
            } else {
                ftp.login(importFTPLogin, importFTPPassword);
            }
            try {
                boolean change = ftp.changeWorkingDirectory(importFTPFilePath);
                CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody changing to directory: " + importFTPFilePath);
                CofaxToolsUtil.log("Results: " + change);
                OutputStream output;
                output = new FileOutputStream(fileTransferFolder + fileName);
                boolean retrieve = ftp.retrieveFile(fileName, output);
                CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody retrieving file: " + fileName);
                CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody results: " + change);
                output.close();
                body = CofaxToolsUtil.readFile(fileTransferFolder + fileName, true);
                CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody deleting remote file: " + importFTPFilePath + fileName);
                boolean delete = ftp.deleteFile(importFTPFilePath + fileName);
                CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody results: " + delete);
                CofaxToolsUtil.log("CofaxToolsFTP getImportFileBody disconnecting from server:" + importFTPServer);
                ftp.logout();
                ftp.disconnect();
            } catch (java.io.IOException e) {
                return ("CofaxToolsFTP getImportFileBody ERROR: cannot write file: " + fileName);
            }
        } catch (IOException e) {
            return ("CofaxToolsFTP getImportFileBody  ERROR: could not connect to server: " + e);
        }
        return (body);
    }

    /**
     *  Returns an HTML Combo Box of media or templates to be uploaded from the
     *  beta server or deleted from the live server.
     *
     *@param  fromMode    Description of the Parameter
     *@param  fromFolder  Description of the Parameter
     *@param  action      Description of the Parameter
     *@param  object      Description of the Parameter
     *@param  session     Description of the Parameter
     *@return             The uploadDeleteComboBox value
     */
    public static String getUploadDeleteComboBox(String fromMode, String fromFolder, String action, String object, HttpSession session) {
        FTPClient ftp = new FTPClient();
        CofaxToolsUser user = (CofaxToolsUser) session.getAttribute("user");
        StringBuffer links = new StringBuffer();
        StringBuffer folders = new StringBuffer();
        String folder = "";
        String server = "";
        String login = "";
        String password = "";
        int i = 0;
        String liveFTPServer = (String) user.workingPubConfigElementsHash.get("LIVEFTPSERVER") + "";
        liveFTPServer = liveFTPServer.trim();
        if ((liveFTPServer == null) || (liveFTPServer.equals("null")) || (liveFTPServer.equals(""))) {
            return ("This tool is not " + "configured and will not operate. " + "If you wish it to do so, please edit " + "this publication's properties and add correct values to " + " the Remote Server Upstreaming section.");
        }
        if (action.equals("Upload")) {
            server = (String) user.workingPubConfigElementsHash.get("TESTFTPSERVER");
            login = (String) user.workingPubConfigElementsHash.get("TESTFTPLOGIN");
            password = (String) user.workingPubConfigElementsHash.get("TESTFTPPASSWORD");
            CofaxToolsUtil.log("server= " + server + " , login= " + login + " , password=" + password);
            if (object.equals("Media")) {
                folder = (String) user.workingPubConfigElementsHash.get("TESTIMAGESFOLDER");
            }
            if (object.equals("Templates")) {
                folder = (String) user.workingPubConfigElementsHash.get("TESTTEMPLATEFOLDER");
                CofaxToolsUtil.log("testTemplateFolder= " + folder);
            }
        }
        if (action.equals("Delete")) {
            login = (String) user.workingPubConfigElementsHash.get("LIVEFTPLOGIN");
            password = (String) user.workingPubConfigElementsHash.get("LIVEFTPPASSWORD");
            if (object.equals("Media")) {
                server = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESSERVER");
                folder = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESFOLDER");
            }
            if (object.equals("Templates")) {
                server = (String) user.workingPubConfigElementsHash.get("LIVEFTPSERVER");
                folder = (String) user.workingPubConfigElementsHash.get("LIVETEMPLATEFOLDER");
            }
        }
        ArrayList servers = splitServers(server);
        try {
            int reply;
            ftp.connect((String) servers.get(0));
            CofaxToolsUtil.log("CofaxToolsFTP getUploadDeleteComboBox connecting to server: " + (String) servers.get(0));
            CofaxToolsUtil.log("CofaxToolsFTP getUploadDeleteComboBox results: " + ftp.getReplyString());
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return ("CofaxToolsFTP getUploadDeleteComboBox ERROR: FTP server refused connection: " + server);
            } else {
                ftp.login(login, password);
                CofaxToolsUtil.log("CofaxToolsFTP getUploadDeleteComboBox login / pass " + login + " " + password);
            }
            try {
                String tempParentFolderName = folder;
                CofaxToolsUtil.log("fromfolder is " + fromFolder);
                if ((fromFolder != null) && (fromFolder.length() > folder.length())) {
                    folder = fromFolder;
                    tempParentFolderName = folder;
                    int subSt = folder.lastIndexOf("/");
                    tempParentFolderName = tempParentFolderName.substring(0, subSt);
                    String publicName = "";
                    int subStri = folder.lastIndexOf((String) user.workingPubName);
                    if (subStri > -1) {
                        publicName = folder.substring(subStri);
                    }
                    folders.append("<B><A HREF=\'/tools/?mode=" + fromMode + "&hl=templates_view_templates_images&fromFolder=" + tempParentFolderName + "\'>" + tempParentFolderName + "</A></B><BR>\n");
                } else if ((fromFolder != null) && (fromFolder.length() == folder.length())) {
                    folder = fromFolder;
                    tempParentFolderName = folder;
                    int subSt = folder.lastIndexOf("/");
                    tempParentFolderName = tempParentFolderName.substring(0, subSt);
                }
                boolean changed = ftp.changeWorkingDirectory(folder);
                CofaxToolsUtil.log("CofaxToolsFTP getUploadDeleteComboBox changing working directory to " + folder);
                CofaxToolsUtil.log("CofaxToolsFTP getUploadDeleteComboBox results " + changed);
                FTPFile[] files = null;
                if ((action.equals("Delete")) && (object.equals("Templates"))) {
                    files = ftp.listFiles(new CofaxToolsNTFileListParser());
                } else {
                    files = ftp.listFiles(new CofaxToolsNTFileListParser());
                }
                if (files == null) {
                    CofaxToolsUtil.log("null");
                }
                for (int ii = 0; ii < files.length; ii++) {
                    FTPFile thisFile = (FTPFile) files[ii];
                    String name = thisFile.getName();
                    if (!thisFile.isDirectory()) {
                        links.append("<INPUT TYPE=CHECKBOX NAME=" + i + " VALUE=" + folder + "/" + name + ">" + name + "<BR>\n");
                        i++;
                    }
                    if ((thisFile.isDirectory()) && (!name.startsWith(".")) && (!name.endsWith("."))) {
                        int subStr = folder.lastIndexOf((String) user.workingPubName);
                        String tempFolderName = "";
                        if (subStr > -1) {
                            tempFolderName = folder.substring(subStr);
                        } else {
                            tempFolderName = folder;
                        }
                        folders.append("<LI><A HREF=\'/tools/?mode=" + fromMode + "&hl=templates_view_templates_images&fromFolder=" + folder + "/" + name + "\'>" + tempFolderName + "/" + name + "</A><BR>");
                    }
                }
                ftp.logout();
                ftp.disconnect();
            } catch (java.io.IOException e) {
                return ("CofaxToolsFTP getUploadDeleteComboBox cannot read directory: " + folder);
            }
        } catch (IOException e) {
            return ("Could not connect to server: " + e);
        }
        links.append("<INPUT TYPE=HIDDEN NAME=numElements VALUE=" + i + " >\n");
        links.append("<INPUT TYPE=SUBMIT VALUE=\"" + action + " " + object + "\">\n");
        return (folders.toString() + links.toString());
    }

    /**
     *  Completes file transfer actions to upload or delete templates or images
     *  via FTP.
     *
     *@param  action       Description of the Parameter
     *@param  object       Description of the Parameter
     *@param  numElements  Description of the Parameter
     *@param  req          Description of the Parameter
     *@param  session      Description of the Parameter
     *@return              Description of the Return Value
     */
    public static String uploadOrDeleteMediaOrTemplates(String action, String object, String numElements, HttpServletRequest req, HttpSession session) {
        FTPClient ftp = new FTPClient();
        CofaxToolsUser user = (CofaxToolsUser) session.getAttribute("user");
        StringBuffer links = new StringBuffer();
        StringBuffer folders = new StringBuffer();
        String folder = "";
        String server = "";
        String login = "";
        String password = "";
        String fileTransferFolder = CofaxToolsServlet.fileTransferFolder;
        String liveFolder = "";
        ArrayList servers = new ArrayList();
        StringBuffer message = new StringBuffer();
        message.append("Status:<BR>");
        if (action.equals("Upload")) {
            server = (String) user.workingPubConfigElementsHash.get("TESTFTPSERVER");
            login = (String) user.workingPubConfigElementsHash.get("TESTFTPLOGIN");
            password = (String) user.workingPubConfigElementsHash.get("TESTFTPPASSWORD");
            if (object.equals("Media")) {
                folder = (String) user.workingPubConfigElementsHash.get("TESTIMAGESFOLDER");
            }
            if (object.equals("Templates")) {
                folder = (String) user.workingPubConfigElementsHash.get("TESTTEMPLATEFOLDER");
            }
        }
        if (action.equals("Delete")) {
            login = (String) user.workingPubConfigElementsHash.get("LIVEFTPLOGIN");
            password = (String) user.workingPubConfigElementsHash.get("LIVEFTPPASSWORD");
            if (object.equals("Media")) {
                server = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESSERVER");
                folder = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESFOLDER");
            }
            if (object.equals("Templates")) {
                server = (String) user.workingPubConfigElementsHash.get("LIVEFTPSERVER");
                folder = (String) user.workingPubConfigElementsHash.get("LIVETEMPLATEFOLDER");
            }
        }
        ArrayList al = new ArrayList();
        int numElement = Integer.parseInt(numElements);
        for (int i = 0; i < numElement; i++) {
            String key = String.valueOf(i);
            String file = req.getParameter(key);
            if (file != null) {
                al.add(file);
            }
        }
        if (action.equals("Upload")) {
            try {
                int reply;
                ftp.connect(server);
                CofaxToolsUtil.log(ftp.getReplyString());
                reply = ftp.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftp.disconnect();
                    return ("FTP server refused connection.");
                } else {
                    ftp.login(login, password);
                }
                for (int ii = 0; ii < al.size(); ii++) {
                    String fileName = (String) al.get(ii);
                    String folderName = stripName(fileName);
                    fileName = stripPath(fileName);
                    try {
                        ftp.changeWorkingDirectory(folderName);
                        OutputStream output;
                        output = new FileOutputStream(fileTransferFolder + fileName);
                        ftp.retrieveFile(fileName, output);
                        CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates retrieving file: " + ftp.getReplyString());
                        message.append("Retrieving file " + fileName + " to local disk.<BR>");
                        output.close();
                    } catch (java.io.IOException e) {
                        return ("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: cannot write file" + e);
                    }
                }
                ftp.logout();
                ftp.disconnect();
            } catch (IOException e) {
                CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: Could not connect to server: " + e);
                return ("Could not connect to server: " + e);
            }
            login = (String) user.workingPubConfigElementsHash.get("LIVEFTPLOGIN");
            password = (String) user.workingPubConfigElementsHash.get("LIVEFTPPASSWORD");
            if (object.equals("Media")) {
                server = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESSERVER");
                liveFolder = (String) user.workingPubConfigElementsHash.get("LIVEIMAGESFOLDER");
            }
            if (object.equals("Templates")) {
                server = (String) user.workingPubConfigElementsHash.get("LIVEFTPSERVER");
                liveFolder = (String) user.workingPubConfigElementsHash.get("LIVETEMPLATEFOLDER");
            }
            servers = splitServers(server);
            for (int iii = 0; iii < servers.size(); iii++) {
                try {
                    int reply;
                    String connectServer = (String) servers.get(iii);
                    ftp.connect(connectServer);
                    CofaxToolsUtil.log(ftp.getReplyString());
                    reply = ftp.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        ftp.disconnect();
                        CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: server refused connection: " + connectServer);
                        return ("CofaxToolsFTP uploadOrDeleteMediaOrTemplates FTP server refused connection.");
                    } else {
                        ftp.login(login, password);
                    }
                    for (int ii = 0; ii < al.size(); ii++) {
                        String fileName = (String) al.get(ii);
                        CofaxToolsUtil.log("Original String " + fileName);
                        CofaxToolsUtil.log("Search for " + folder);
                        CofaxToolsUtil.log("Replace " + liveFolder);
                        String folderName = CofaxToolsUtil.replace(fileName, folder, liveFolder);
                        CofaxToolsUtil.log("Results: " + folderName);
                        folderName = stripName(folderName);
                        fileName = stripPath(fileName);
                        try {
                            InputStream io;
                            io = new FileInputStream(fileTransferFolder + fileName);
                            CofaxToolsUtil.log("Reading file : " + fileTransferFolder + fileName);
                            boolean directoryExists = ftp.changeWorkingDirectory(folderName);
                            if (directoryExists == false) {
                                CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates directory: " + folderName + " does not exist. Attempting to create.");
                                message.append("Directory: " + folderName + " does not exist. Attempting to create.<BR>");
                                boolean canCreatDir = ftp.makeDirectory(folderName);
                                CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates results: " + canCreatDir);
                                message.append("Results: " + canCreatDir + "<BR>");
                            }
                            boolean isStored = ftp.storeFile(fileName, io);
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates storing file: " + fileName + " in directory: " + folderName);
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates on server : " + connectServer);
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates results: " + isStored + " : " + ftp.getReplyString());
                            message.append("Storing file " + fileName + "<BR> to location " + folderName + "<BR> on server " + connectServer + ".<BR>");
                        } catch (java.io.IOException e) {
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates cannot upload file" + fileName + "<BR>To path: " + folderName + "<BR>On server " + connectServer);
                            return ("Cannot upload file" + fileName + "<BR>To path: " + folderName + "<BR>On server " + connectServer);
                        }
                    }
                    ftp.logout();
                    ftp.disconnect();
                    message.append("Success<BR><BR>");
                } catch (IOException e) {
                    CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates could not connect to server: " + e);
                    return ("Could not connect to server: " + e);
                }
            }
            if (object.equals("Templates")) {
                String cSServers = (String) user.workingPubConfigElementsHash.get("CACHESERVERS");
                System.out.println("getting cache servers: " + cSServers);
                ArrayList cServers = splitServers(cSServers);
                for (int iiii = 0; iiii < cServers.size(); iiii++) {
                    String thisClearCacheServer = (String) cServers.get(iiii);
                    try {
                        String connectServer = (String) cServers.get(iiii);
                        for (int iiiii = 0; iiiii < al.size(); iiiii++) {
                            String thisFilePath = (String) al.get(iiiii);
                            String folderNameFileName = CofaxToolsUtil.replace(thisFilePath, folder, liveFolder);
                            String URLToClear = CofaxToolsServlet.removeTemplateCache + folderNameFileName;
                            CofaxToolsClearCache clear = new CofaxToolsClearCache("HTTP://" + thisClearCacheServer + URLToClear);
                            clear.start();
                            message.append("Clearing Cache for " + folderNameFileName + "<BR>");
                            message.append("on server " + thisClearCacheServer + "<BR>Success<BR><BR>");
                        }
                    } catch (Exception e) {
                        CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: could not connect to server clearing cache " + e);
                    }
                }
            }
            for (int i = 0; i < al.size(); i++) {
                String fileName = (String) al.get(i);
                String folderName = stripName(fileName);
                fileName = stripPath(fileName);
                File file = new File(fileTransferFolder + fileName);
                boolean delete = file.delete();
                CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates deleting file from local drive: " + fileTransferFolder + fileName);
                CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates results: " + delete);
            }
        }
        servers = splitServers(server);
        if (action.equals("Delete")) {
            for (int iii = 0; iii < servers.size(); iii++) {
                try {
                    int reply;
                    String connectServer = (String) servers.get(iii);
                    ftp.connect(connectServer);
                    CofaxToolsUtil.log(ftp.getReplyString());
                    reply = ftp.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        ftp.disconnect();
                        CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: FTP server refused connection: " + connectServer);
                        return ("FTP server refused connection.");
                    } else {
                        ftp.login(login, password);
                    }
                    for (int ii = 0; ii < al.size(); ii++) {
                        String fileName = (String) al.get(ii);
                        String folderName = stripName(fileName);
                        fileName = stripPath(fileName);
                        try {
                            ftp.changeWorkingDirectory(folderName);
                            ftp.deleteFile(fileName);
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates deleting file: " + fileName + " from directory: " + folderName);
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates on server : " + connectServer);
                            CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates results: " + ftp.getReplyString());
                            message.append("Deleting file " + fileName + "<BR>");
                            message.append("from folder " + folderName + "<BR>");
                            message.append("on server " + connectServer + "<BR>");
                        } catch (java.io.IOException e) {
                            return ("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: cannot delete file" + fileName);
                        }
                    }
                    message.append("Success<BR><BR>");
                    ftp.logout();
                    ftp.disconnect();
                } catch (IOException e) {
                    CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR: Could not connect to server: " + e);
                    return ("Could not connect to server: " + e);
                }
            }
            if (object.equals("Templates")) {
                String cISServers = (String) user.workingPubConfigElementsHash.get("CACHESERVERS");
                ArrayList cIServers = splitServers(cISServers);
                for (int iiiiii = 0; iiiiii < cIServers.size(); iiiiii++) {
                    String thisClearCacheIServer = (String) cIServers.get(iiiiii);
                    try {
                        String connectServer = (String) cIServers.get(iiiiii);
                        for (int iiiiiii = 0; iiiiiii < al.size(); iiiiiii++) {
                            String thisFilePathI = (String) al.get(iiiiiii);
                            String URLToClearI = CofaxToolsServlet.removeTemplateCache + thisFilePathI;
                            CofaxToolsClearCache clearI = new CofaxToolsClearCache("HTTP://" + thisClearCacheIServer + URLToClearI);
                            clearI.start();
                            message.append("Clearing Cache for " + thisFilePathI + "<BR>");
                            message.append("on server " + thisClearCacheIServer + "<BR>Success<BR><BR>");
                        }
                    } catch (Exception e) {
                        CofaxToolsUtil.log("CofaxToolsFTP uploadOrDeleteMediaOrTemplates ERROR clearing cache " + e);
                    }
                }
            }
        }
        return (message.toString());
    }

    /**
     *  Determine if a String contains four consecutive numbers; return true if
     *  so, false if not. Used to determine if we are currently browsing through
     *  the /year/month/day sections under the objects/ images folders.
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */
    public static boolean containsFourDigits(String name) {
        char[] charChars = name.toCharArray();
        int test = 0;
        for (int i = 0; i < charChars.length; i++) {
            if (Character.isDigit(charChars[i])) {
                test++;
            } else if (test > 0) {
                test--;
            }
            if (test >= 3) {
                return (true);
            }
        }
        return (false);
    }
}

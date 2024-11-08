package org.gmod.ant.task.net.ftp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.gmod.ant.task.utils.InputValidation;

/**
 * This task connects to a remote ftp server and generates a listing file
 * based on the requested files.  It is used over the standard Ant FTP task
 * because of performance reasons.  The native Ant FTP task has trouble when
 * dealing with several thousand files because of its file and directory
 * scanning.  This task also supports symbolic links which Ant's FTP task
 * does not at the time of this writing.
 * 
 * 
 * @author Josh Goodman
 * @version CVS $Revision: 1.10 $
 * 
 */
public class FtpListing extends Task {

    private static final Pattern DATE_SIZE_WITH_TIME = Pattern.compile("(\\d+\\s[A-Za-z]+\\s+[0-9]+\\s+[0-9]+:[0-9]+)\\s+([0-9A-Za-z\\.\\/\\\\\\_\\-]+)");

    private static final Pattern DATE_SIZE_WITH_YEAR = Pattern.compile("(\\d+\\s[A-Za-z]+\\s+[0-9]+\\s+[0-9]+)\\s+([0-9A-Za-z\\.\\/\\\\\\_\\-]+)");

    private String ftpServer;

    private int ftpPort;

    private String username;

    private String password;

    private String remoteDir;

    private File listingFile;

    private BufferedWriter bw;

    private String remoteFiles;

    private String[] remoteFileStrings;

    private Pattern[] remoteFilePatterns;

    private int numDir;

    private FTPClient client;

    /**
     * The function that is executed by Ant when the ftp listing task is used.  This function
     * connects to the remote ftp server, lists the files wanted, and writes the listing to a file.
     * 
     * @throws org.apache.tools.ant.BuildException If an Ant build exception is encountered.
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() {
        checkInput();
        try {
            client = new FTPClient();
            log("Connecting to " + ftpServer, Project.MSG_INFO);
            client.connect(ftpServer, ftpPort);
            checkFtpCode(client, "FTP server refused connection:");
            log("Connected", Project.MSG_INFO);
            log("Logging in", Project.MSG_INFO);
            if (!client.login(username, password)) {
                log("Login failed: " + client.getReplyString(), Project.MSG_ERR);
            }
            log("Login successful", Project.MSG_INFO);
            client.enterLocalPassiveMode();
            checkFtpCode(client, "Couldn't change connection type to passive: ");
            log("Changed to passive mode.", Project.MSG_VERBOSE);
            client.changeWorkingDirectory(remoteDir);
            checkFtpCode(client, "Can't change to directory: " + remoteDir);
            log("Listing FTP files", Project.MSG_INFO);
            for (int i = 0; i < remoteFileStrings.length; i++) {
                remoteFilePatterns = makePattern(remoteFileStrings[i]);
                numDir = remoteFilePatterns.length - 1;
                log("Setting number of directories to: " + numDir, Project.MSG_VERBOSE);
                FTPFile[] files = client.listFiles(remoteDir);
                files = followSymLink(client, files);
                log("# of files in " + remoteDir + " is " + files.length, Project.MSG_VERBOSE);
                scanDir(0, numDir, files, null);
            }
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException iof) {
                }
            }
            log("Could not connect to " + ftpServer + " " + ioe.getMessage(), Project.MSG_ERR);
        }
    }

    /**
     * Sets the FTP port number.
     * 
     * @param ftpPort The ftpPort to set.
     */
    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    /**
     * Sets the FTP server string.
     * 
     * @param ftpServer The ftpServer to set.
     */
    public void setFtpServer(String ftpServer) {
        this.ftpServer = ftpServer;
    }

    /**
     * Sets the file name to store the listing information in.
     * 
     * @param listingFile The listingFile to set.
     */
    public void setListingFile(File listingFile) {
        this.listingFile = listingFile;
        try {
            bw = new BufferedWriter(new FileWriter(listingFile));
        } catch (IOException ioe) {
            log("IO Exception: " + ioe.getMessage(), Project.MSG_ERR);
        }
    }

    /**
     * Sets the ftp password.
     * 
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the remote directory to search.
     * 
     * @param remoteDir The remoteDir to set.
     */
    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    /**
     * Sets the patterns used to select remote files.
     * 
     * @param remoteFiles The remoteFiles to set.
     */
    public void setRemoteFiles(String remoteFiles) {
        this.remoteFiles = remoteFiles;
        remoteFileStrings = remoteFiles.split("\\s+");
    }

    /**
     * Sets the ftp username.
     * 
     * @param username The username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    private FTPFile[] followSymLink(FTPClient client, FTPFile[] files) {
        try {
            if (files.length == 1 && files[0].isSymbolicLink()) {
                String linkTarget = files[0].getLink();
                if (linkTarget.indexOf('/') == -1) {
                    linkTarget = remoteDir.replaceAll("[\\w\\d\\.-]+$", linkTarget);
                }
                log("Remote directory is a sym link.  Listing files in " + linkTarget, Project.MSG_VERBOSE);
                files = client.listFiles(linkTarget);
            }
        } catch (IOException ioe) {
            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException iof) {
                }
            }
            log("Could not connect to " + ftpServer + " " + ioe.getMessage(), Project.MSG_ERR);
        }
        return files;
    }

    /**
     * Returns an array of patterns for a directory tree.  Each node
     * on the tree is represented by a pattern in the array.
     * 
     * @param pattern A string with the patterns as strings using '/' as a directory delimiter.
     * @return Returns an array of pattern objects.
     */
    private Pattern[] makePattern(String pattern) {
        String[] patterns = pattern.split("[/]");
        Pattern[] p = new Pattern[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            log("Making pattern: " + patterns[i], Project.MSG_VERBOSE);
            p[i] = Pattern.compile(patterns[i]);
        }
        return p;
    }

    /**
     * Scans the remote directory for the request files.  This is a recursive
     * function to navigate ftp directory trees like the pdb ftp server.
     * 
     * @param loopCount The number of times it has gone through scanDir.  Should be 0 when the code first calls scanDir.
     * @param dirCount The number of directories in the current listing.
     * @param files The FTP file array to loop over.
     * @param path The sub directory path to loop in.
     */
    private void scanDir(int loopCount, int dirCount, FTPFile[] files, String path) {
        log("Entering scanDir.", Project.MSG_VERBOSE);
        log("loopCount = " + loopCount + " dirCount = " + dirCount + " path = " + path, Project.MSG_VERBOSE);
        try {
            if (dirCount == 0) {
                for (int i = 0; i < files.length; i++) {
                    FTPFile file = files[i];
                    String name = file.getName();
                    Matcher fileMatcher = remoteFilePatterns[loopCount].matcher(name);
                    if (file.isFile() && !name.equals(".") && !name.equals("..") && fileMatcher.find()) {
                        if (path != null) {
                            String ftpListingTxt = file.toString();
                            ftpListingTxt = ftpListingTxt.replaceAll(name, path + "/" + name);
                            bw.write(ftpListingTxt);
                            bw.newLine();
                            log(ftpListingTxt, Project.MSG_VERBOSE);
                        } else {
                            bw.write(file.toString());
                            bw.newLine();
                            log(file.toString(), Project.MSG_VERBOSE);
                        }
                    } else if (file.isSymbolicLink() && !name.equals(".") && !name.equals("..") && fileMatcher.find()) {
                        log(file.getName() + " is a symbolic link.", Project.MSG_VERBOSE);
                        String linkFileName = file.getLink();
                        log("The link target is " + linkFileName, Project.MSG_VERBOSE);
                        try {
                            String linkFileListingTxt = client.listFiles(linkFileName)[0].toString();
                            String newFtpListingTxt = fixLinkFileDate(file.toString(), linkFileListingTxt, null);
                            log("newFtpListingTxt = " + newFtpListingTxt, Project.MSG_VERBOSE);
                            bw.write(newFtpListingTxt);
                            bw.newLine();
                            log(newFtpListingTxt, Project.MSG_VERBOSE);
                        } catch (ArrayIndexOutOfBoundsException aiobe) {
                            log("Warning - Can't find file: " + linkFileName, Project.MSG_WARN);
                        }
                    } else {
                        log("Ignoring file: " + name, Project.MSG_VERBOSE);
                        log("Listing: " + file.toString(), Project.MSG_VERBOSE);
                    }
                }
            } else {
                for (int i = 0; i < files.length; i++) {
                    FTPFile file = files[i];
                    String name = file.getName();
                    Matcher dirMatcher = remoteFilePatterns[loopCount].matcher(name);
                    if (isDir(file) && !name.equals(".") && !name.equals("..") && dirMatcher.find()) {
                        String newPath;
                        if (path == null) {
                            newPath = name;
                        } else {
                            newPath = path + "/" + name;
                        }
                        scanDir(loopCount + 1, dirCount - 1, client.listFiles(newPath), newPath);
                    }
                }
            }
        } catch (IOException ioe) {
            log("IO Exception encountered: " + ioe.getMessage(), Project.MSG_ERR);
        }
    }

    /**
     * A recursive function to replace the actual file size and date in the listing
     * file for symbolicly linked files.
     * 
     * @param linkFileTxt The ftp listing information for the link file.
     * @param linkTargetTxt The ftp listing information for the target file.
     * @param replaceTxt The file size and date to replace in the link file.
     * @return The corrected ftp listing text.
     */
    private String fixLinkFileDate(String linkFileTxt, String linkTargetTxt, String replaceTxt) {
        String linkListingTxt = null;
        log("linkFileTxt = " + linkFileTxt, Project.MSG_VERBOSE);
        log("linkTargetTxt = " + linkTargetTxt, Project.MSG_VERBOSE);
        log("replaceTxt = " + replaceTxt, Project.MSG_VERBOSE);
        if (replaceTxt == null) {
            replaceTxt = matchFTPDate(linkTargetTxt);
            if (replaceTxt == null) {
                log("Couldn't match any date/file size in text: " + linkTargetTxt, Project.MSG_ERR);
            }
            linkListingTxt = fixLinkFileDate(linkFileTxt, linkTargetTxt, replaceTxt);
        } else {
            String txt2replace = matchFTPDate(linkFileTxt);
            linkListingTxt = linkFileTxt.replaceAll(txt2replace, replaceTxt);
        }
        return linkListingTxt;
    }

    /**
     * Returns the file size and date information for a ftp listing.
     * 
     * @param ftpListing
     * @return The file size and date for the ftp file.
     */
    private String matchFTPDate(String ftpListing) {
        Matcher yearMatcher = DATE_SIZE_WITH_YEAR.matcher(ftpListing);
        Matcher timeMatcher = DATE_SIZE_WITH_TIME.matcher(ftpListing);
        String matchedDate = null;
        if (yearMatcher.find()) {
            matchedDate = yearMatcher.group(1);
        } else if (timeMatcher.find()) {
            matchedDate = timeMatcher.group(1);
        }
        return matchedDate;
    }

    /**
     * A simple check for a directory is not enough because it will ignore
     * directories which are symbolic links to ther directories.
     * This function should catch both.
     * 
     * @param file The FTPFile object to check.
     * @return boolean indicating if the FTPFile is a directory or not.
     * 
     */
    private boolean isDir(FTPFile dir) {
        boolean isDirectory = false;
        if (dir.isDirectory()) {
            isDirectory = true;
        } else if (dir.isSymbolicLink()) {
            try {
                isDirectory = isDir(client.listFiles(dir.getLink())[0]);
            } catch (IOException ioe) {
                log("IO Exception: " + ioe.getMessage(), Project.MSG_ERR);
            }
        }
        return isDirectory;
    }

    /**
     * Checks the attributes passed to ftplisting task.
     *
     * @throws org.apache.tools.ant.BuildException If required attributes are not set.
     */
    private void checkInput() {
        InputValidation.checkString(ftpServer, "FTP server name");
        InputValidation.checkString(Integer.toString(ftpPort), "FTP port number");
        InputValidation.checkString(username, "username");
        InputValidation.checkString(password, "password");
        InputValidation.checkString(remoteDir, "remote FTP directory");
        InputValidation.checkString(listingFile, "the ftp listing file");
        InputValidation.checkString(remoteFiles, "the remote file regular expression");
    }

    private void checkFtpCode(FTPClient ftp, String mesg) {
        if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            log("FTP error " + mesg + " " + ftp.getReplyString(), Project.MSG_ERR);
        }
    }
}

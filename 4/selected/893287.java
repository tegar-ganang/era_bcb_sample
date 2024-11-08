package org.rr.jsendfile.app;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import org.rr.jsendfile.gui.Status;
import org.rr.jsendfile.util.ProgInfo;
import org.rr.jsendfile.util.Sysinfo;
import org.rr.jsendfile.util.TxtUtil;
import org.rr.jsendfile.util.net.FileInfoWriter;
import org.rr.jsendfile.util.typ.TransFile;
import org.rr.jsendfile.util.typ.User;

/**
 * holt Datei ab die f�r den User an den SAFT-Server gesendet wurde. Folgende Parameter sind m�glich:
 *
 *  usage: receive -a [destpath]
 *
 *  options:   -a specify all files to receive
 *  -F <filename> specify filename for receiving
 *  -d delete file(s) and dont receive
 *  -l list all files
 *  -k keep files in spool after receiving
 *  -P receive file to stdout
 *  -b <host> bounce (forward) a file
 *  -m receive messages
 *  -u <local username>
 *  -o <spoolpath>
 *  -h help
 *  -V version
 *
 * @author  R�diger Rauschenbach
 */
public class Receive {

    private boolean help = false;

    private boolean version = false;

    private boolean overwrite = false;

    private boolean delete = false;

    private boolean all = false;

    private boolean keep = false;

    private boolean msg = false;

    private boolean lst = false;

    private boolean stdOut = false;

    private String bounce = null;

    private static DefaultTableModel tableModel = null;

    private static StringBuffer messageBuffer = null;

    private Status transferStatus;

    private File spoolPath;

    private File copyPath;

    private File copyFile = null;

    private User user;

    /** Creates a new instance of Receive and init the defaults */
    public Receive() {
        try {
            user = new User(Sysinfo.getUserName());
        } catch (Exception e) {
        }
        if ((copyPath = new File(Sysinfo.getUserHome() + "/Eigene Dateien/")).exists()) {
        } else {
            copyPath = new File(Sysinfo.getUserHome());
        }
        spoolPath = this.getSpoolDirFromServerConfig();
    }

    /**
     * this is only the start-in
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Receive().parseParameters(args, new Status());
    }

    /**
     * this is only the start-in
     * @param args the command line arguments
     */
    public static void main(String[] args, Status transferStatus) {
        new Receive().parseParameters(args, transferStatus);
    }

    /**
     * start-in but with a TableModel reference for fillin if the -l (--list)
     * parameter is called. The given TableModel is emptied before use.
     */
    public static void main(String[] args, DefaultTableModel inTableModel) {
        (tableModel = inTableModel).setRowCount(0);
        new Receive().parseParameters(args, new Status());
    }

    /**
     * start-in but with a StringBuffer reference for fillin if the -m (--message)
     * parameter is called. The given StringBuffer is emptied before use.
     */
    public static void main(String[] args, StringBuffer inBuff) {
        (messageBuffer = inBuff).setLength(0);
        new Receive().parseParameters(args, new Status());
    }

    /** initial to become non static. this is the core, it checks all parameters
     * using getopt and init all what the parameters say.
     * @param args just the command line arguments from the main method.
     */
    public void parseParameters(String[] args, Status inTransferStatus) {
        if (args.length == 0) {
            args = new String[] { "--help" };
        }
        this.transferStatus = inTransferStatus;
        LongOpt[] longopts = new LongOpt[13];
        int c;
        longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        longopts[1] = new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V');
        longopts[2] = new LongOpt("spoolpath", LongOpt.REQUIRED_ARGUMENT, null, 'o');
        longopts[3] = new LongOpt("localuser", LongOpt.REQUIRED_ARGUMENT, null, 'n');
        longopts[4] = new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'F');
        longopts[5] = new LongOpt("bounce", LongOpt.NO_ARGUMENT, null, 'b');
        longopts[6] = new LongOpt("list", LongOpt.NO_ARGUMENT, null, 'l');
        longopts[7] = new LongOpt("keep", LongOpt.NO_ARGUMENT, null, 'k');
        longopts[8] = new LongOpt("msg", LongOpt.NO_ARGUMENT, null, 'm');
        longopts[9] = new LongOpt("all", LongOpt.NO_ARGUMENT, null, 'a');
        longopts[10] = new LongOpt("delete", LongOpt.NO_ARGUMENT, null, 'd');
        longopts[11] = new LongOpt("stdout", LongOpt.NO_ARGUMENT, null, 'P');
        longopts[12] = new LongOpt("overwrite", LongOpt.NO_ARGUMENT, null, 'O');
        Getopt g = new Getopt("Sendfiled", args, "-:o:n:F:b:adlkPmhVO;", longopts);
        while ((c = g.getopt()) != -1) {
            switch(c) {
                case 'h':
                    this.help = true;
                    break;
                case 'V':
                    this.version = true;
                    break;
                case 'O':
                    this.overwrite = true;
                    break;
                case 'o':
                    try {
                        File spoolPathArgument = new File(new TxtUtil().cutFirstChar(g.getOptarg(), '='));
                        if (spoolPathArgument.isDirectory()) {
                            spoolPath = new File(spoolPathArgument + "/" + user.getUserName());
                        } else {
                            System.out.println("Spoolfilepath \"" + spoolPathArgument + "\" not found");
                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing spoolfilepath");
                        return;
                    }
                    break;
                case 'n':
                    try {
                        user.setUserName(new TxtUtil().cutFirstChar(g.getOptarg(), '='));
                        spoolPath = new File(spoolPath.getParent() + "/" + user.getUserName());
                    } catch (Exception e) {
                        System.out.println("Error processing username");
                        return;
                    }
                    break;
                case 'l':
                    this.lst = true;
                    break;
                case 'F':
                    copyFile = new File(g.getOptarg());
                    break;
                case 'k':
                    this.keep = true;
                    break;
                case 'm':
                    this.msg = true;
                    break;
                case 'a':
                    this.all = true;
                    break;
                case 'd':
                    this.delete = true;
                    break;
                case 'P':
                    this.stdOut = true;
                    break;
                case 'b':
                    this.bounce = new TxtUtil().cutFirstChar(g.getOptarg(), '=');
                    break;
                default:
                    try {
                        String inCopyPath = g.getOptarg();
                        if (!(inCopyPath == null) && !(copyPath = new File(inCopyPath)).exists()) {
                            System.out.println("Error processing destination");
                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing destination");
                        return;
                    }
                    break;
            }
        }
        if (this.version) {
            System.out.println("Sendmsg v" + ProgInfo.VERSION + " From " + ProgInfo.AUTHOR);
            return;
        }
        if (this.lst) {
            this.listFiles();
            return;
        }
        if (this.msg) {
            this.printMessages();
            return;
        }
        if (this.delete) {
            try {
                if (this.copyFile != null) {
                    this.deleteFile(this.copyFile);
                } else {
                    this.deleteAllFiles();
                }
            } catch (FileCopyException e) {
                System.out.println(e.getMessage());
            }
            return;
        }
        if (this.bounce != null) {
            if (this.all) {
                this.bounceAll();
                return;
            } else if (this.copyFile != null) {
                this.bounceFile(this.copyFile);
                return;
            }
        }
        if (this.all) {
            this.copyAllFiles();
            return;
        } else if (this.copyFile != null) {
            try {
                this.fileCopy(this.copyFile);
                return;
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("nothing to do");
        }
        System.out.println("usage: receive -a [destpath]");
        System.out.println("");
        System.out.println("options:   -a specify all files to receive. If not specified, look at the user home.");
        System.out.println("           -F <filename> specify filename for receiving");
        System.out.println("           -d delete specifyed file(s) and dont receive");
        System.out.println("           -l list all files");
        System.out.println("           -k keep files in spool after receiving");
        System.out.println("           -P receive file to stdout");
        System.out.println("           -b <user@host[:port]> bounce (forward) a file");
        System.out.println("           -m receive messages");
        System.out.println("           -n <local username>");
        System.out.println("           -o <spoolpath>");
        System.out.println("           -h help");
        System.out.println("           -V version");
        this.quit();
    }

    /**
     * Bounce the File to another user using Sendfile
     */
    private void bounceFile(File filename) {
        File source = new File(this.spoolPath + "/" + filename);
        TransFile tf = this.getFileInfo(new File(filename.getName()));
        StringBuffer sBuff = new StringBuffer();
        sBuff.append("\"" + source.getPath() + "\" ");
        if (tf.getExpectedCompressedFileSize() == tf.getExpectedFileSize()) {
            sBuff.append("-u ");
        }
        if (tf.getCharset() != null && tf.getCharset() != "") {
            sBuff.append("-t ");
        }
        if (tf.getFileComment() != null && tf.getFileComment() != "") {
            sBuff.append("-c \"" + tf.getFileComment() + "\" ");
        }
        sBuff.append("-n " + user.getUserName() + " ");
        sBuff.append(this.bounce);
        Sendfile.main(new TxtUtil().createArguments(sBuff.toString()), transferStatus);
    }

    /**
     * Bounce all Files to another user using Sendfile
     */
    private void bounceAll() {
        File[] spoolFiles = spoolPath.listFiles();
        try {
            for (int i = 0; i < spoolFiles.length; i++) {
                try {
                    if (spoolFiles[i].isFile()) {
                        this.bounceFile(new File(spoolFiles[i].getName()));
                    }
                } catch (NullPointerException e) {
                    System.out.println("could not send file " + spoolFiles[i].getName());
                }
            }
        } catch (NullPointerException e) {
            System.out.println("no files found");
        }
    }

    /**
     * List all Files from actual Userdirectory
     */
    private void listFiles() {
        int realFiles = 0;
        File[] spoolFiles = spoolPath.listFiles();
        try {
            for (int i = 0; i < spoolFiles.length; i++) {
                TransFile tf = this.getFileInfo(new File(spoolFiles[i].getName()));
                if (spoolFiles[i].isFile() && tf != null) {
                    realFiles++;
                    System.out.println("From " + tf.getUserFrom().getUserName() + "@" + tf.getUserFrom().getUserAdress() + " (" + tf.getUserFrom().getUserRealName() + ")");
                    System.out.println("  " + tf.getExpectedFileDate() + "  " + (tf.getExpectedFileSize() / 1000.) + " Kb  " + spoolFiles[i].getName());
                    System.out.println("  " + tf.getFileComment());
                    System.out.println();
                    if (Receive.tableModel != null) {
                        Vector v = new Vector();
                        v.addElement(spoolFiles[i].getName());
                        v.addElement(String.valueOf((tf.getExpectedFileSize() / 1000.)) + " Kb");
                        v.addElement(tf.getUserFrom().getUserName());
                        v.addElement(tf.getExpectedFileDate().getPureDate());
                        v.addElement(tf.getExpectedFileDate().getPureTime());
                        v.addElement(tf.getFileComment());
                        tableModel.addRow(v);
                    }
                }
            }
            if (realFiles == 0) {
                throw new NullPointerException();
            }
        } catch (NullPointerException e) {
            System.out.println("no files found");
        }
    }

    /**
     * get the fileinfo which is saved with the file using the FileInfoWriter
     */
    private TransFile getFileInfo(File filename) {
        try {
            return new FileInfoWriter(new TransFile(new File(this.spoolPath.getParent()), filename, user, user)).readConfigFile();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Copy ALL files from SpoolPath (with Username)
     */
    private void copyAllFiles() {
        File[] spoolFiles = spoolPath.listFiles();
        try {
            for (int i = 0; i < spoolFiles.length; i++) {
                try {
                    if (spoolFiles[i].isFile()) {
                        this.fileCopy(new File(spoolFiles[i].getName()));
                    }
                } catch (IOException e) {
                    System.out.println("could not copy file " + spoolFiles[i]);
                }
            }
        } catch (NullPointerException e) {
            System.out.println("no files found");
        }
    }

    /** Delete specified file on spool */
    private void deleteFile(File filename) throws FileCopyException {
        File source = new File(this.spoolPath + "/" + filename);
        File sourceInfo = new File(this.spoolPath + "/info/" + copyFile + ".desc");
        if (source.exists()) {
            if (source.delete()) {
                sourceInfo.delete();
            } else {
                throw new FileCopyException("file could not be deleted: " + source);
            }
        } else {
            throw new FileCopyException("file does not exists: " + source);
        }
        System.out.println("file successfully deleted " + source);
    }

    /**
     * Delete all files on spool
     */
    private void deleteAllFiles() {
        File[] spoolFiles = spoolPath.listFiles();
        try {
            for (int i = 0; i < spoolFiles.length; i++) {
                if (spoolFiles[i].isFile()) {
                    this.deleteFile(new File(spoolFiles[i].getName()));
                }
            }
        } catch (FileCopyException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * print all Message to current Screen (System.out)
     */
    private void printMessages() {
        byte buffer[];
        BufferedReader messageFile = null;
        File messageSpoolPath = new File(spoolPath + "/messages/");
        String[] spoolFiles = messageSpoolPath.list();
        try {
            for (int i = 0; i < spoolFiles.length; i++) {
                if (new File(messageSpoolPath + "/" + spoolFiles[i]).isFile()) {
                    messageFile = new BufferedReader(new FileReader(messageSpoolPath + "/" + spoolFiles[i]));
                    buffer = new byte[1024];
                    String s;
                    while ((s = messageFile.readLine()) != null) {
                        System.out.println(s);
                        if (messageBuffer != null) {
                            messageBuffer.append(s + "\n");
                        }
                    }
                    messageFile.close();
                    if (this.keep == false) {
                        new File(spoolPath + "/messages/" + spoolFiles[i]).delete();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("no messages found");
        } finally {
            if (messageFile != null) try {
                messageFile.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * exit (only a System.exit)
     */
    public void quit() {
    }

    /**
     * get the spoolfile path from the server config file or 
     * get the program default value.
     * @return the spoolfile path
     */
    private File getSpoolDirFromServerConfig() {
        try {
            Properties props = new Properties();
            InputStream in = new FileInputStream(new File(Sysinfo.getCurPath() + "/config/jsendfile-server.conf"));
            props.load(in);
            String propSpoolfiledirectory = props.getProperty("spoolfiledirectory");
            if (propSpoolfiledirectory != null && propSpoolfiledirectory.length() > 0) {
                return new File(propSpoolfiledirectory);
            }
        } catch (Exception e) {
        }
        return new File(Sysinfo.getCurPath() + "/spool/" + user.getUserName());
    }

    /**
     * this runs like the filecopy, but copy the file to stdout (System.out)
     */
    private void fileDump(File filename) throws FileCopyException {
        File source_file = new File(spoolPath + "/" + filename);
        BufferedReader source = null;
        String stringBuffer;
        try {
            if (!source_file.exists() || !source_file.isFile()) throw new FileCopyException("no such source file: " + source_file);
            if (!source_file.canRead()) throw new FileCopyException("source file is unreadable: " + source_file);
            try {
                source = new BufferedReader(new FileReader(source_file));
                while ((stringBuffer = source.readLine()) != null) {
                    System.out.println(stringBuffer);
                }
            } catch (IOException e) {
                throw new FileCopyException("" + e.getMessage() + ": " + source_file);
            }
        } finally {
            if (source != null) try {
                source.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * copy method using a 1024byte buffer. If
     * @param filename  filename of of the file to copy
     * @throws IOException if anything runs false with the copy process
     */
    private void fileCopy(File filename) throws IOException {
        if (this.stdOut) {
            this.fileDump(filename);
            return;
        }
        File source_file = new File(spoolPath + "/" + filename);
        File destination_file = new File(copyPath + "/" + filename);
        FileInputStream source = null;
        FileOutputStream destination = null;
        byte[] buffer;
        int bytes_read;
        try {
            if (!source_file.exists() || !source_file.isFile()) throw new FileCopyException("no such source file: " + source_file);
            if (!source_file.canRead()) throw new FileCopyException("source file is unreadable: " + source_file);
            if (destination_file.exists()) {
                if (destination_file.isFile()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                    if (!destination_file.canWrite()) throw new FileCopyException("destination file is unwriteable: " + destination_file);
                    if (!this.overwrite) {
                        System.out.print("File " + destination_file + " already exists. Overwrite? (Y/N): ");
                        System.out.flush();
                        if (!in.readLine().toUpperCase().equals("Y")) throw new FileCopyException("copy cancelled.");
                    }
                } else throw new FileCopyException("destination is not a file: " + destination_file);
            } else {
                File parentdir = parent(destination_file);
                if (!parentdir.exists()) throw new FileCopyException("destination directory doesn't exist: " + destination_file);
                if (!parentdir.canWrite()) throw new FileCopyException("destination directory is unwriteable: " + destination_file);
            }
            source = new FileInputStream(source_file);
            destination = new FileOutputStream(destination_file);
            buffer = new byte[1024];
            while ((bytes_read = source.read(buffer)) != -1) {
                destination.write(buffer, 0, bytes_read);
            }
            System.out.println("File " + filename + " successfull copied to " + destination_file);
            if (this.keep == false && source_file.isFile()) {
                try {
                    source.close();
                } catch (Exception e) {
                }
                if (source_file.delete()) {
                    new File(this.spoolPath + "/info/" + filename + ".desc").delete();
                }
            }
        } finally {
            if (source != null) try {
                source.close();
            } catch (IOException e) {
            }
            if (destination != null) try {
                destination.flush();
            } catch (IOException e) {
            }
            if (destination != null) try {
                destination.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * File.getParent() can return null when the file is specified without
     * a directory or is in the root directory.
     */
    private File parent(File f) throws FileCopyException {
        String dirname = f.getParent();
        if (dirname == null) {
            throw new FileCopyException("error getting parent directory");
        }
        return new File(dirname);
    }

    class FileCopyException extends IOException {

        public FileCopyException(String msg) {
            super(msg);
        }
    }
}

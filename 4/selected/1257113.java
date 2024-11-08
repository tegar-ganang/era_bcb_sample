package vaspgui;

import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * This class controls all input and output between the program and the
 * file system, whether it be local or remote.
 * @author Tim Hecht
 *
 */
public class IO {

    /**
     * Boolean variable that gives true if the system is connected to a remote
     * system and false if it is using the local filesystem.
     */
    private static boolean isConnected;

    /**
     * Gives true if the local machine is running windows and false otherwise.
     */
    private static boolean isWindows;

    /**
     * Instance of the SCP connection class, which is used to give commands to
     * the remote system.
     */
    private static SCP connection;

    /**
     * A string containing the current directory.
     */
    private static String curdir;

    /**
     * A string containing the folder separation character, which depends on the
     * operating system.
     */
    private static String sep;

    /**
     * Constructor for IO class. Sets up the curdir variable and the sep
     * variable.
     */
    protected IO() {
        if (System.getProperty("os.name").startsWith("W")) {
            isWindows = true;
            curdir = "C:\\";
            sep = "\\";
        } else {
            isWindows = false;
            curdir = "/";
            sep = "/";
        }
    }

    /**
     *A method which obtains the text from a text file.
     * @param fileName File name of file to be read
     * @return An array of each line of the file
     */
    public static ArrayList<String> getFile(final String path, final String fileName) {
        if (!checkForFile(path, fileName)) {
            return null;
        }
        if (isConnected) {
            try {
                String newpath = "." + sep + "tmp" + sep;
                SCP.getClient().get(path + fileName, newpath);
                return getFileLocal(newpath, fileName);
            } catch (IOException e) {
                return null;
            }
        } else {
            return getFileLocal(path, fileName);
        }
    }

    private static ArrayList<String> getFileLocal(final String path, final String fileName) {
        try {
            Scanner scan = new Scanner(new File(path + sep + fileName));
            ArrayList<String> returnarray = new ArrayList<String>();
            while (scan.hasNext()) {
                returnarray.add(scan.nextLine());
            }
            return returnarray;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String getJobFile(int nodes, int processors, int memory, String walltime, String jobname, String email, String path, String fileName) {
        try {
            Scanner scan = new Scanner(new File("." + sep + "job.file"));
            String returnString = "";
            while (scan.hasNext()) {
                returnString += scan.nextLine() + "\n";
            }
            returnString = returnString.replaceAll("@@nodes@@", Integer.toString(nodes));
            returnString = returnString.replaceAll("@@processorspernode@@", Integer.toString(processors));
            returnString = returnString.replaceAll("@@memory@@", Integer.toString(memory));
            returnString = returnString.replaceAll("@@walltime@@", walltime);
            returnString = returnString.replaceAll("@@jobname@@", jobname);
            returnString = returnString.replaceAll("@@email@@", email);
            returnString = returnString.replaceAll("@@totalnodes@@", Integer.toString(nodes * processors));
            returnString = returnString.replaceAll("@@path@@", path);
            returnString = returnString.replaceAll("@@file@@", fileName);
            return returnString;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * Checks whether the given file exists on the local machine or the server.
     * @param fileName Name of the file to check
     * @return Returns true if files exist, false otherwise
     */
    public static boolean checkForFile(final String path, final String fileName) {
        if (isConnected) {
            return getFiles(path).contains(fileName);
        } else {
            return (new File(path + sep + fileName)).exists();
        }
    }

    public static ArrayList<String> getFiles(String path) {
        if (isConnected) {
            ArrayList<String> toSort = connection.executeCommand("ls -l " + path);
            ArrayList<String> returnarray = new ArrayList<String>();
            for (int i = 0; i < toSort.size(); i++) {
                if (toSort.get(i).startsWith("-")) {
                    returnarray.add(toSort.get(i).split(" ")[toSort.get(i).split(" ").length - 1]);
                }
            }
            return returnarray;
        } else {
            File[] filearray = (new File(path)).listFiles();
            ArrayList<String> returnarray = new ArrayList<String>();
            for (int i = 0; i < filearray.length; i++) {
                if (filearray[i].isFile() && !filearray[i].isHidden()) {
                    returnarray.add(filearray[i].getPath().replace(curdir, ""));
                }
            }
            return returnarray;
        }
    }

    public static ArrayList<String> getFolders(String path) {
        if (isConnected) {
            ArrayList<String> toSort = connection.executeCommand("ls -l " + path);
            ArrayList<String> returnarray = new ArrayList<String>();
            for (int i = 0; i < toSort.size(); i++) {
                if (toSort.get(i).startsWith("d")) returnarray.add(toSort.get(i).split(" ")[toSort.get(i).split(" ").length - 1]); else if (toSort.get(i).startsWith("l")) {
                    returnarray.add(toSort.get(i).split(" ")[toSort.get(i).split(" ").length - 3]);
                }
            }
            return returnarray;
        } else {
            File[] folderarray = (new File(path)).listFiles();
            ArrayList<String> returnarray = new ArrayList<String>();
            for (int i = 0; i < folderarray.length; i++) if (folderarray[i].isDirectory() && !folderarray[i].isHidden()) returnarray.add(folderarray[i].getPath().replace(path, ""));
            return returnarray;
        }
    }

    public static void makeDir(String folderName) {
        if (isConnected) {
            connection.executeCommand("mkdir " + curdir + folderName);
        } else {
            (new File(curdir + folderName)).mkdir();
        }
    }

    public static void delDir(String folderName) {
        if (isConnected) {
            connection.executeCommand("rm -r " + curdir + folderName);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     *
     * @return A string containing the current directory
     */
    public static String getCurDir() {
        return curdir;
    }

    /**
     * This method changes directory into the folder given by the parameter
     * folderPath.
     * @param folderPath String containing path of directory to change into
     */
    public static void setCurDir(final String folderPath) {
        curdir = folderPath;
        vaspgui.MainPanel.updateDirBar(curdir);
    }

    /**
     * Returns the path by going into the specified folder.
     * @param path Parent folder where sub folder is located.
     * @param folderName Folder to change directories into.
     * @return String containing the path.
     */
    public static String intoDir(String path, String folderName) {
        return path + folderName + sep;
    }

    public static String findParent(String path) {
        if (isConnected) {
            String dirname = connection.executeCommand("dirname " + path).get(0);
            if (dirname.equals("/")) return "/";
            return dirname + sep;
        } else {
            File file = new File(path);
            if (!hasParent(path)) return path; else return file.getParent() + sep;
        }
    }

    public static boolean hasParent(String path) {
        if (isConnected) {
            return !path.equals("/");
        } else {
            return (new File(path)).getParent() == null;
        }
    }

    /**Establishes the SCP connection.
     *
     * @param host String containing the name of the host to connect to
     * @param user String containing the username to connect with
     * @param password String containing the password to connect with
     */
    public static void connect(final String host, final String user, final String password) {
        connection = new SCP(host, user, password);
        if (!connection.isConnected()) {
            isConnected = false;
            connection = null;
        } else {
            isConnected = true;
            sep = "/";
            setCurDir(connection.executeCommand("pwd").get(0) + sep);
        }
    }

    /**
     * Closes the SCP connection.
     */
    public static void closeConnection() {
        if (isConnected) {
            connection.closeConnection();
        }
    }

    /**
     * A method to see if an SCP connection is open.
     * @return True if connected and false if not
     */
    public static boolean isConnected() {
        return isConnected;
    }

    /**
     * This method saves a file with the given filename and given
     * text.
     * @param fileName File name of file to be saved
     * @param text Text to be saved in file
     * @return True if file save is successful and false otherwise
     */
    public static boolean saveFile(final String path, final String fileName, final String text) {
        if (!checkOverwrite(path, fileName)) {
            return false;
        }
        if (isConnected) {
            try {
                if (saveFileLocal("." + sep + "tmp" + sep, fileName, text)) {
                    SCP.getClient().put("." + sep + "tmp" + sep + fileName, path);
                    MainPanel.updateStatusBar(fileName + " was saved successfully.");
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        } else {
            return saveFileLocal(path, fileName, text);
        }
    }

    public static boolean saveFileLocal(final String path, final String fileName, final String text) {
        try {
            PrintWriter out = new PrintWriter(path + sep + fileName);
            out.println(text);
            out.close();
            MainPanel.updateStatusBar(fileName + " was saved successfully.");
            return true;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks whether a file with the given file name exists already, and pulls
     * up an option pane asking the user whether or not they wish to overwrite
     * the file.
     * @param fileName File name to check whether it will be overwritten.
     * @return -false if user chooses not to overwrite an existing file,
     * true otherwise.
     */
    private static boolean checkOverwrite(final String path, final String fileName) {
        if (checkForFile(path, fileName)) {
            int answer = JOptionPane.showConfirmDialog(new JFrame(), fileName + " file already exists.  Overwrite?");
            if (answer == JOptionPane.YES_OPTION) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * This method destroys any SCP connection, and sets the IO file to
     * read from the local system.
     */
    public static void makeLocal() {
        if (isConnected) {
            closeConnection();
            connection = null;
        }
        isConnected = false;
        if (isWindows) {
            curdir = "C:\\";
            sep = "\\";
        } else {
            curdir = "/";
            sep = "/";
        }
    }

    public static ArrayList<String> executeCommand(String command) {
        return connection.executeCommand(command);
    }
}

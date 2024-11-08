package net.sf.mailsomething.util;

import java.util.Vector;
import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;

/**
 *  This has evolved into being first class to be initiated. Could actually be called MailSomething and the
 * methods that dont fit, like copyFile could be placed in..... GeneralSupport :)
 *
 * This should be merged with lib.util.CommonUtils
 * 
 *@author     Stig Tanggaard
 *@created    February 23, 2002
 */
public class GeneralSupport extends Object {

    private static GeneralSupport support = null;

    private static File appDirectory = null;

    private static File userDirectory = null;

    private Vector vector;

    private File settingsFile;

    private AppUser user;

    /**
	 *  Constructor for the GeneralSupport object
	 */
    private GeneralSupport() {
        vector = new Vector();
    }

    public static void copy(FileInputStream in, FileOutputStream out) throws IOException {
        FileChannel fcIn = in.getChannel();
        FileChannel fcOut = out.getChannel();
        fcIn.transferTo(0, fcIn.size(), fcOut);
    }

    /**
	 * This method will copy the folder and its contents of the sourceDir
	 * to the destinationdir. It will create a new folder in the destdir
	 * with the name of the source. 
	 * Doesnt copy folders. 
	 * 
	 * @param sourceDir
	 * @param destDir
	 */
    public static void copyDir(File sourceDir, File destDir) {
        if (!sourceDir.exists()) return;
        if (!destDir.exists()) return;
        File file = new File(destDir, sourceDir.getName());
        if (!file.exists()) file.mkdir();
        File[] list = sourceDir.listFiles();
        for (int i = 0; i < list.length; i++) {
            if (list[i].isDirectory()) continue;
            File temp = new File(file, list[i].getName());
            try {
                temp.createNewFile();
                GeneralSupport.copyFile(list[i], temp);
            } catch (IOException f) {
            }
        }
    }

    /**
	 *  Not sure this is being used.
	 *
	 *@param  old   Description of the Parameter
	 *@param  file  Description of the Parameter
	 */
    public static void copyFile(File old, File file) {
        if (!old.exists()) {
            return;
        }
        try {
            file.createNewFile();
            FileInputStream fileInputStream = new FileInputStream(old);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            copy(fileInputStream, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            fileInputStream.close();
        } catch (IOException f) {
        }
    }

    /** Appdirectory is where the program is installed, ie the basedirectory. This isnt
	* necessarely the place where usersettings and messages etc. (but it is possible).
	* The appdirectory should have at least the class files of the program, and 1 settings
	* file which is global and not user specific, ie it should keep paths to settingsfile of specific
	* users for the case theres several users using one instance of the program. And some kinda
	* file with paths of modules/jars (which could be a executing file where the paths are listed in
	* classpath/path). For classes (other than this) there shouldnt be any interest in this directory
	* but instead in userdirectory.
	* 
	*/
    private void locateAppDirectory() {
        String className = this.getClass().getName().replace('.', '/') + ".class";
        URL url = ClassLoader.getSystemResource(className);
        appDirectory = new File(url.getPath().substring(0, url.getPath().length() - className.length()));
    }

    private GeneralSupport getInstance() {
        if (support == null) support = new GeneralSupport();
        return support;
    }

    public static Object get(String className) {
        return null;
    }

    public static void setSomething(Object object) {
        if (support == null) support = new GeneralSupport();
        support.vector.add(object);
    }

    public static Object getSomething() {
        if (support == null) support = new GeneralSupport();
        return support.vector.elementAt(0);
    }

    public static void main(String[] args) {
    }

    public static String getDateStyle() {
        return "EEE, d MMM yyyy hh,mm,ss aa zzzzz";
    }
}

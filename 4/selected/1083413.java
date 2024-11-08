package com.chazaqdev.etei;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.PointerType;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JFileChooser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 * @author mike
 */
public class AppDataDir {

    private static String theAppsDataDir = "";

    public static String getApplicationDataDir() {
        String s = "";
        if (com.sun.jna.Platform.isWindows()) {
            Shell32 shell32 = (Shell32) Native.loadLibrary("shell32", Shell32.class, OPTIONS);
            HWND hwndOwner = null;
            int nFolder = Shell32.CSIDL_LOCAL_APPDATA;
            HANDLE hToken = null;
            int dwFlags = Shell32.SHGFP_TYPE_CURRENT;
            char[] pszPath = new char[Shell32.MAX_PATH];
            int hResult = shell32.SHGetFolderPath(hwndOwner, nFolder, hToken, dwFlags, pszPath);
            if (Shell32.S_OK == hResult) {
                String path = new String(pszPath);
                int len = path.indexOf('\0');
                path = path.substring(0, len);
                s = "" + path;
                File f = new File(s);
                if (f.exists()) {
                    s = f.getAbsolutePath();
                } else {
                    s = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
                }
            } else {
                s = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
            }
        }
        if (com.sun.jna.Platform.isLinux() || com.sun.jna.Platform.isMac() || com.sun.jna.Platform.isFreeBSD() || com.sun.jna.Platform.isOpenBSD() || com.sun.jna.Platform.isSolaris()) {
            File f = new File("~/");
            if (f.exists()) {
                s = f.getAbsolutePath();
            } else {
                s = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
            }
        }
        return s;
    }

    private static void moveUnzipAndExtract(File f) {
        File outputDir = new File(theAppsDataDir + "/" + f.getName().substring(0, f.getName().length() - 4));
        outputDir.mkdir();
        System.out.println("" + outputDir.getAbsolutePath() + ":" + theAppsDataDir);
        ZipUtils.unzipArchive(f, outputDir);
    }

    public static void copyOverWarFile() {
        System.out.println("Copy Over War File:");
        File dir = new File(theAppsDataDir);
        FileFilter ff = new WildcardFileFilter("*.war");
        if (dir.listFiles(ff).length == 0) {
            dir = new File(System.getProperty("user.dir") + "/war");
            if (dir.exists()) {
                File[] files = dir.listFiles(ff);
                for (File f : files) {
                    try {
                        File newFile = new File("" + theAppsDataDir + "/" + f.getName());
                        System.out.println("Creating new file \"" + f.getAbsolutePath() + "\"");
                        newFile.createNewFile();
                        InputStream fi = new FileInputStream(f);
                        OutputStream fo = new FileOutputStream(newFile);
                        IOUtils.copy(fi, fo);
                        moveUnzipAndExtract(newFile);
                    } catch (Exception ex) {
                        Logger.getLogger(AppDataDir.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } else {
            System.out.println("Found a war in the apps data dir, ignoring a fresh copy");
        }
        new JFileChooser().setCurrentDirectory(new File(theAppsDataDir));
        System.setProperty("user.dir", theAppsDataDir);
        System.out.println("User.dir : " + System.getProperty("user.dir"));
    }

    private static String APP_NAME = "";

    public static String getApplicationName() {
        if (APP_NAME.equals("")) {
            APP_NAME = "ETEI";
            File f = new File("./name.ini");
            if (f.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(f);
                    Scanner scanner = new Scanner(fis);
                    if (scanner.hasNextLine()) {
                        APP_NAME = scanner.nextLine();
                        System.out.println("App name = " + APP_NAME);
                    }
                    scanner.close();
                    fis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return APP_NAME;
    }

    /**
     * Creates the application's application data directory
     * @param name  The folder. Don't forget that / is already prepended
     */
    public static void createAppDataDir(String name) {
        String appDataDir = getApplicationDataDir() + "/" + name;
        File f = new File(appDataDir);
        if (f.exists()) {
            System.out.println("Not creating already created app dir");
        } else {
            f.mkdirs();
        }
        theAppsDataDir = f.getAbsolutePath();
        copyOverWarFile();
    }

    public static String getFirstWarYouCanFind() {
        String s = "";
        boolean foundDirectory = false;
        File dir = new File(theAppsDataDir);
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.exists() && f.isDirectory() && !f.getName().equals("webapps")) {
                    System.out.println("Found directory \"" + f.getAbsolutePath() + "\"");
                    return f.getAbsolutePath();
                }
            }
        }
        if (!foundDirectory) {
            FileFilter ff = new WildcardFileFilter("*.war");
            if (dir.listFiles(ff) == null) {
                System.out.println("Great scott!");
            }
            if (dir.listFiles(ff) == null || dir.listFiles(ff).length == 0) {
                System.out.println("THIS IS WRONG, GOING TO RUN BOGUS!");
                System.exit(1);
            } else {
                s = "" + dir.listFiles(ff)[0].getAbsolutePath();
                System.out.println("GetFirstWarYouCanFind:>>" + s);
            }
        }
        return s;
    }

    public static String getTheApplicationsDataDir() {
        return theAppsDataDir;
    }

    private static Map<String, Object> OPTIONS = new HashMap<String, Object>();

    static {
        OPTIONS.put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
        OPTIONS.put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
    }

    private static class HANDLE extends PointerType implements NativeMapped {
    }

    private static class HWND extends HANDLE {
    }

    private static interface Shell32 extends Library {

        public static final int MAX_PATH = 260;

        public static final int CSIDL_LOCAL_APPDATA = 0x001c;

        public static final int SHGFP_TYPE_CURRENT = 0;

        public static final int SHGFP_TYPE_DEFAULT = 1;

        public static final int S_OK = 0;

        /**
         * see http://msdn.microsoft.com/en-us/library/bb762181(VS.85).aspx
         *
         * HRESULT SHGetFolderPath( HWND hwndOwner, int nFolder, HANDLE hToken,
         * DWORD dwFlags, LPTSTR pszPath);
         */
        public int SHGetFolderPath(HWND hwndOwner, int nFolder, HANDLE hToken, int dwFlags, char[] pszPath);
    }
}

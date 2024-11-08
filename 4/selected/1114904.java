package be.ac.fundp.infonet.econf.util;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Various utilities for the infonet softwares
 * @author St�phane NICOLL - Infonet FUNDP
 * @version 0.2
 */
public class Utilities {

    /**
     * Logging object.
     */
    private static org.apache.log4j.Category m_logCat = org.apache.log4j.Category.getInstance(Utilities.class.getName());

    /**
     * Converts a file path name appropriate to the platform.
     * @param filename the file pathname
     * @return a converted path name
     */
    public static String convertPathname(String filename) {
        if (filename == null) return null;
        String sep = File.separator;
        if (sep.equals("/")) return filename.replace('\\', '/');
        return filename.replace('/', '\\');
    }

    /**
     * Gets a file reference. If the file already exist, its reference is returned.
     * @param content The content of the file
     * @param path The path of the file
     * @throws IOException If an IO error occurs.
     */
    public static File getFile(byte[] content, String path) throws IOException {
        File f = new File(path);
        if (f.exists()) return f; else {
            f.createNewFile();
            FileOutputStream out = new FileOutputStream(f);
            out.write(content);
            out.flush();
            out.close();
            return f;
        }
    }

    /**
     * Checks whether this specified directory is valid.
     * @return a valid directory representation (with a trailing / or \)
     */
    public static String checkDir(String dir) {
        dir = convertPathname(dir);
        if (!dir.endsWith(File.separator)) dir = dir + File.separator;
        return dir;
    }

    /**
     * Creates the directory denoted by this path if it does not exist.
     * @param path The full path to create
     * @return true if everything is ok (the directory exists or has been created), false otherwise.
     */
    public static boolean createDirectory(String path) {
        if (path == null) return false;
        try {
            path = checkDir(path);
            File f = new File(path);
            if (f.isDirectory()) return true; else {
                f.mkdirs();
                m_logCat.info("eConf created " + f);
            }
            return true;
        } catch (Exception e) {
            m_logCat.error("Error while creating directory for: " + path, e);
            return false;
        }
    }

    /**
     * Retrieves the website from an URL e.g. www.google.com from http://www.google.com/search?=infonet
     * @param url The URL to work on
     * @return the web site without the http:// or null if this is not a correct url
     */
    public static String getWebSite(URL url) {
        String anURL = url.toString();
        if (anURL.startsWith("http://")) anURL = anURL.substring(7, anURL.length());
        int i = anURL.indexOf("/");
        return (anURL.substring(0, i));
    }

    /**
     * Constructs the full URL.
     * @param relativePath The relative path to work on (e.g.: index.html or /images/img.gif)
     * @param originURL The context URL
     */
    public static URL rebuildURL(String relativePath, URL originURL) {
        URL u = null;
        if ((relativePath == null) || (originURL == null)) return null;
        try {
            u = new URL(relativePath);
            return u;
        } catch (MalformedURLException ue) {
        }
        if (relativePath.startsWith("/")) {
            try {
                u = new URL("http", originURL.getAuthority(), relativePath);
                return u;
            } catch (MalformedURLException ue2) {
                m_logCat.warn("Trying to construct : " + relativePath + " from: " + originURL);
            }
        } else {
            String tmp = originURL.toString();
            int i = tmp.lastIndexOf("/");
            if (i != -1) {
                tmp = tmp.substring(0, i + 1);
                tmp = tmp + relativePath;
                try {
                    u = new URL(tmp);
                    return u;
                } catch (MalformedURLException ue3) {
                    m_logCat.warn("Trying to construct : " + relativePath + " from: " + originURL);
                }
            }
        }
        return null;
    }

    /**
     * Checks whether the specified URI represents a full URL or not.
     * @param uri The URI to check
     * @return true if this URI represents a full URL, false otherwise
     */
    public static boolean isFullURL(String uri) {
        try {
            URL u = new URL(uri);
            return true;
        } catch (MalformedURLException e) {
            if (uri.indexOf("www.") != -1) return true;
        }
        return false;
    }

    /**
     * Returns the file name of this URL.
     * @param u The URL
     * @return the file name of u (e.g. index.html)
     */
    public static String getFileName(URL u) {
        try {
            String anURL = u.toString();
            int i = anURL.lastIndexOf("/");
            return (anURL.substring(i + 1));
        } catch (Exception e) {
            return ("");
        }
    }

    /**
     * Replace all occurrences of oldToken in s with newToken, or only the first occurrence if fAll is false. <br>
     * replace("aaaa", "aa", "bbb", false) returns "bbbaa"<br> replace("aaaa", "aa", "bbb", true) returns "bbbbbb"
     */
    public static String replace(String s, String oldToken, String newToken, boolean fAll) {
        if ((s == null) || (oldToken == null) || (newToken == null)) {
            throw new IllegalArgumentException("Null argument(s) seen");
        }
        int oldTokenLen = oldToken.length();
        StringBuffer sb = null;
        int oldPos = 0;
        int pos = s.indexOf(oldToken, oldPos);
        if (oldPos > -1) {
            sb = new StringBuffer(s.length());
        }
        for (; pos > -1; pos = s.indexOf(oldToken, oldPos)) {
            sb.append(s.substring(oldPos, pos));
            sb.append(newToken);
            oldPos = pos + oldTokenLen;
            if (!fAll) {
                break;
            }
        }
        return ((oldPos > 0) ? sb.append(s.substring(oldPos)).toString() : s);
    }

    /**
     * Copy the content of a file to another file.
     * @param src
     * The source file
     * @param des
     * The destination file
     * return true if the copy was successful, false otherwise.
     */
    public static boolean copyFile(File src, File des) {
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(des));
            int b;
            while ((b = in.read()) != -1) out.write(b);
            out.flush();
            out.close();
            in.close();
            return true;
        } catch (IOException ie) {
            m_logCat.error("Copy file + " + src + " to " + des + " failed!", ie);
            return false;
        }
    }

    /**
     * Inits a Class with the specified class name.
     * @param className
     * The full class name
     * @return A class instance or null if the class has not been found
     */
    public static Class initClass(String className) {
        if (className == null) return null;
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException nfe) {
            m_logCat.error("The specified class: " + className + " has not been found", nfe);
        }
        return null;
    }

    /**
     * Creates a new instance of the specified class.
     * @return the instance or null if a problem occurs.
     */
    public static Object instantiateClass(Class c) {
        if (c == null) return null;
        try {
            return c.newInstance();
        } catch (InstantiationException ie) {
            m_logCat.error("Cannot instanciate class: " + c, ie);
        } catch (IllegalAccessException iae) {
            m_logCat.error("Cannot access class: " + c, iae);
        }
        return null;
    }

    /**
     * Append the specified name to the specified file name. <BR>
     * <P>If f = "autoexec.bat" and app = "-bak" this fonction will return
     * "autoexec-bak.bat"</P>
     * @param s
     * The original file name
     * @param app
     * The String to append to the end of the file name
     * @return a new File or null if an error occurs
     */
    public static String appendName(String s, String app) {
        if (s == null) return null;
        if (app == null) app = "";
        int i = s.lastIndexOf(".");
        if (i != -1) {
            String first = s.substring(0, i);
            String second = s.substring(i, s.length());
            return (first + app + second);
        } else return (s + app);
    }

    /**
     * Returns the file from the specified URL.
     */
    public static String getFileFromURL(String anURL) {
        try {
            int i = anURL.lastIndexOf("/");
            return (anURL.substring(i + 1));
        } catch (Exception e) {
            return ("");
        }
    }

    /**
     * Converts the specified directory for being writtent to a property file.
     * @param dir
     * A directory
     * @return the directory
     */
    public static String convertPropDir(String dir) {
        if (File.separator.equals("/")) return dir;
        String res = "";
        int i = dir.indexOf("\\");
        while ((dir != "") || (dir == null)) {
            if (i == -1) return res; else {
                dir = dir.substring(0, i);
                res += dir + "\\\\";
            }
        }
        return res;
    }
}

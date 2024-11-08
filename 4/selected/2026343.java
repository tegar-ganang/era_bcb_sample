package org.dbe.servent.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Stack;

/**
 * 
 * @author bob
 */
public class FSTool {

    /**
     * Deletes a directory and all its contents
     * 
     * @param theDir
     *                  The directory to remove
     */
    public static void rmdir(File dir) {
        Stack dirStack = new Stack();
        File actualItem = dir;
        dirStack.push(actualItem);
        while (!dirStack.isEmpty()) {
            actualItem = (File) dirStack.peek();
            ArrayList children = getDirs(actualItem);
            if (children.size() == 0) {
                delFiles(actualItem);
                actualItem.delete();
                dirStack.pop();
            } else {
                for (int i = 0; i < children.size(); i++) {
                    dirStack.push(children.get(i));
                }
            }
        }
    }

    /**
     * Gets all directories in a directory
     * 
     * @param dir
     *                  The directory to inspect
     * @return The contents of the directory <tt>dir</tt>
     */
    public static ArrayList getDirs(File dir) {
        ArrayList theDirs = new ArrayList();
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].isDirectory()) {
                    theDirs.add(contents[i]);
                }
            }
        }
        return theDirs;
    }

    /**
     * Gets all readable files in a directory
     * 
     * @param dir
     *                  The directory to inspect
     * @return The contents of the directory <tt>dir</tt>
     */
    public static ArrayList getFiles(File dir) {
        ArrayList theFiles = new ArrayList();
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (int i = 0; i < contents.length; i++) {
                if ((contents[i].isFile()) && (contents[i].canRead())) {
                    theFiles.add(contents[i]);
                }
            }
        }
        return theFiles;
    }

    /**
     * Deletes all files in a directory.
     * 
     * @param dir
     *                  The directory
     */
    private static void delFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    files[i].delete();
                }
            }
        }
    }

    /**
     * Return the file byte array
     * 
     * @param path
     *                  path to the deployment dir
     * @param codebaseFile
     *                  name of the codebase JAR file
     * @return codebase byte array to send to the clients
     */
    public static byte[] read(File file) {
        byte[] result = null;
        if (file.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(file);
                result = new byte[(int) file.length()];
                fis.read(result);
            } catch (IOException e) {
            }
        }
        return result;
    }

    /**
     * @param servicePath
     * @param newPath
     */
    public static boolean copy(File source, File target) {
        boolean done = false;
        if (source.isDirectory() && source.canRead()) {
            target.mkdirs();
            done = true;
            File[] children = source.listFiles();
            for (int i = 0; i < children.length; i++) {
                File child = children[i];
                if (child.isDirectory()) {
                    done = done && copy(child, new File(target, child.getName()));
                } else {
                    done = done && dump(child, new File(target, child.getName()));
                }
            }
        }
        return done;
    }

    /**
     * 
     * @param source
     * @param target
     * @return
     */
    public static boolean dump(File source, File target) {
        boolean done = false;
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(source));
            OutputStream os = new BufferedOutputStream(new FileOutputStream(target));
            while (is.available() > 0) {
                os.write(is.read());
            }
            os.flush();
            os.close();
            is.close();
            return true;
        } catch (IOException e) {
        }
        return done;
    }
}

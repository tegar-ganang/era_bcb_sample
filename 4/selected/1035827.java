package com.csft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class Util {

    /**
	 * This method dowloads a text file from an internet web site.
	 * 
	 * @param netAddress
	 *            the web address
	 * @param arguments
	 *            the posted parameters for the web address
	 * 
	 * @return the string array of the whole file.
	 * @throws IOException
	 * @exception IOException
	 *                will be thrown if the IO error happens. Normally a missing
	 *                file is the case.
	 */
    public static String[] downloadLineByLine(String netAddress, String arguments) throws IOException {
        List<String> ret = new ArrayList<String>();
        URL u = new URL(netAddress);
        URLConnection uc = u.openConnection();
        uc.setDoOutput(true);
        PrintWriter pw = new PrintWriter(uc.getOutputStream());
        pw.println(arguments);
        pw.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String res = in.readLine();
        while (res != null) {
            ret.add(res);
            res = in.readLine();
        }
        in.close();
        return (String[]) ret.toArray(new String[0]);
    }

    /**
	 * This method read a text file, and store in a string array.
	 * 
	 * @param fileName
	 *            the name of the file
	 * 
	 * @return the string array of the whole file.
	 */
    public static String[] readFileLineByLine(String fileName) {
        List<String> sl = new ArrayList<String>();
        try {
            if (!new File(fileName).exists()) {
                new File(fileName).createNewFile();
            }
            FileInputStream fis = new FileInputStream(new File(fileName));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);
            String currline = "";
            while ((currline != null)) {
                currline = reader.readLine();
                if (currline != null) {
                    sl.add(currline);
                }
            }
            reader.close();
            isr.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] strs = sl.toArray(new String[0]);
        return strs;
    }

    /**
	 * This method write the contents in a string array into a text file.
	 * 
	 * @param fileName
	 * @param writeList
	 */
    public static void writeLineByLine(final String fileName, final String[] writeList) {
        try {
            final PrintWriter pw = new PrintWriter(new FileWriter(fileName));
            for (final String element : writeList) {
                pw.println(element);
            }
            pw.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File sourceFile, String toDir) {
        try {
            copyFile(sourceFile, toDir, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File sourceFile, String toDir, boolean create) {
        try {
            copyFile(sourceFile, toDir, create, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(File sourceFile, String toDir, boolean create, boolean overwrite) throws FileNotFoundException, IOException {
        FileInputStream source = null;
        FileOutputStream destination = null;
        byte[] buffer;
        int bytes_read;
        File toFile = new File(toDir);
        if (create && !toFile.exists()) toFile.mkdirs();
        if (toFile.exists()) {
            File destFile = new File(toDir + "/" + sourceFile.getName());
            try {
                if (!destFile.exists() || overwrite) {
                    source = new FileInputStream(sourceFile);
                    destination = new FileOutputStream(destFile);
                    buffer = new byte[1024];
                    while (true) {
                        bytes_read = source.read(buffer);
                        if (bytes_read == -1) break;
                        destination.write(buffer, 0, bytes_read);
                    }
                }
            } catch (Exception exx) {
                exx.printStackTrace();
            } finally {
                if (source != null) try {
                    source.close();
                } catch (IOException e) {
                }
                if (destination != null) try {
                    destination.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void deleteRecursive(File _file) {
        try {
            if (_file.exists()) {
                if (_file.isDirectory()) {
                    File[] files = _file.listFiles();
                    for (int i = 0, l = files.length; i < l; ++i) deleteRecursive(files[i]);
                }
                _file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

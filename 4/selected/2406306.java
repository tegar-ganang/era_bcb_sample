package net.sourceforge.javalogging.installer;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.zip.*;
import javax.swing.*;

public class Installer {

    private static ProgressDialog progress = new ProgressDialog();

    public static void main(String[] args) {
        progress.setVisible(true);
        String step = null;
        try {
            if (checkJavaVersion()) {
                if (lumberjackAlreadyInstalled()) {
                    int answer = JOptionPane.showConfirmDialog(progress, "Lumberjack is already installed. Overwrite?", "Overwrite current installation?", JOptionPane.OK_CANCEL_OPTION);
                    if (answer != JOptionPane.OK_OPTION) {
                        JOptionPane.showMessageDialog(progress, "Installation cancelled...");
                        System.exit(1);
                    }
                    progress.addMessage("Overwriting existing installation...");
                    progress.mark(progress.CHECK_EXISTING);
                    try {
                        removeExistingLumberjack();
                    } catch (Exception e) {
                        step = "Problems removing existing Lumberjack";
                        throw e;
                    }
                } else {
                    progress.mark(progress.CHECK_EXISTING);
                }
                try {
                    installClassFiles();
                    progress.mark(progress.INSTALL_CLASSES);
                } catch (Exception e) {
                    step = "Problems installing class files.";
                    throw e;
                }
                boolean installProperties = true;
                if (loggingPropertiesAlreadyInstalled()) {
                    int answer = JOptionPane.showConfirmDialog(progress, "'logging.properties' exists. Overwrite?", "Overwrite current logging.properties?", JOptionPane.OK_CANCEL_OPTION);
                    if (answer != JOptionPane.OK_OPTION) {
                        progress.addMessage("Not installing 'logging.properties'...");
                        installProperties = false;
                    } else {
                        progress.addMessage("Overwriting existing 'logging.properties'...");
                    }
                }
                if (installProperties) {
                    try {
                        installPropertiesFile();
                        progress.mark(progress.INSTALL_PROPERTIES);
                    } catch (Exception e) {
                        step = "Problems installing logging.properties.";
                        throw e;
                    }
                }
                progress.enableClose("All done!", 0);
            }
        } catch (Exception e) {
            progress.addMessage(step);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            progress.enableClose(sw.getBuffer().toString(), 1);
        }
    }

    private static boolean checkJavaVersion() {
        String version = System.getProperty("java.version");
        if (!version.startsWith("1.2") && !version.startsWith("1.3")) {
            if (version.charAt(0) == '1' && version.charAt(1) == '.' && version.charAt(2) >= '4') {
                progress.enableClose("The Java Logging API is in JDK 1.4 and beyond...", 1);
                return false;
            } else if (version.startsWith("1.1")) {
                progress.enableClose("Lumberjack does not work with JDK 1.1.x...", 1);
                return false;
            } else {
                progress.enableClose("Unable to parse version info. Sorry!", 1);
                return false;
            }
        }
        return true;
    }

    private static boolean lumberjackAlreadyInstalled() {
        String javaHome = System.getProperty("java.home");
        File classes = new File(javaHome + File.separator + "classes");
        if (classes.exists()) {
            File logging = new File(classes.getAbsolutePath() + File.separator + "java" + File.separator + "util" + File.separator + "logging");
            if (logging.exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean loggingPropertiesAlreadyInstalled() {
        String javaHome = System.getProperty("java.home");
        File loggingDotProperties = new File(javaHome + File.separator + "lib" + File.separator + "logging.properties");
        if (loggingDotProperties.exists()) {
            return true;
        }
        return false;
    }

    private static void removeExistingLumberjack() throws Exception {
        String javaHome = System.getProperty("java.home");
        File logging = new File(javaHome + File.separator + "classes" + File.separator + "java" + File.separator + "util" + File.separator + "logging");
        File[] allFiles = logging.listFiles();
        for (int index = 0; index < allFiles.length; index++) {
            File current = allFiles[index];
            if (!current.delete()) {
                throw new IllegalArgumentException("Unable to delete '" + current.getAbsolutePath() + "'!");
            }
        }
    }

    private static void installClassFiles() throws Exception {
        String javaHome = System.getProperty("java.home");
        StringBuffer loggingDirName = new StringBuffer(javaHome + File.separator + "classes" + File.separator + "java" + File.separator + "util" + File.separator + "logging");
        File loggingDir = new File(loggingDirName.toString());
        if (!loggingDir.exists() && !loggingDir.mkdirs()) {
            String msg = "Unable to assure output directory '" + loggingDir.getAbsolutePath() + "'!'";
            progress.addMessage(msg);
            throw new FileNotFoundException(msg);
        }
        ZipFile z = new ZipFile(System.getProperty("java.class.path"));
        Enumeration e = z.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            String name = entry.getName();
            if (name.startsWith("java/util/logging") && name.length() > "java/util/logging/".length()) {
                int entrySize = (int) entry.getSize();
                byte[] contents = new byte[entrySize];
                InputStream in = z.getInputStream(entry);
                int bytesRead = 0;
                while (bytesRead < entrySize) {
                    bytesRead += in.read(contents, bytesRead, entrySize - bytesRead);
                }
                in.close();
                String fileName = name.substring(name.lastIndexOf('/') + 1);
                File outputFile = new File(loggingDirName.toString() + File.separator + fileName);
                OutputStream out = new FileOutputStream(outputFile);
                out.write(contents, 0, bytesRead);
                out.close();
                progress.addMessage("Installed '" + name + "' (" + entry.getSize() + " bytes)...");
            }
        }
    }

    private static void installPropertiesFile() throws Exception {
        ZipFile z = new ZipFile(System.getProperty("java.class.path"));
        ZipEntry entry = (ZipEntry) z.getEntry("net/sourceforge/javalogging/logging.properties");
        int entrySize = (int) entry.getSize();
        byte[] contents = new byte[entrySize];
        InputStream in = z.getInputStream(entry);
        int bytesRead = 0;
        while (bytesRead < entrySize) {
            bytesRead += in.read(contents, bytesRead, entrySize - bytesRead);
        }
        in.close();
        String javaHome = System.getProperty("java.home");
        StringBuffer outputFileName = new StringBuffer(javaHome + File.separator + "lib" + File.separator + "logging.properties");
        File outputFile = new File(outputFileName.toString());
        OutputStream out = new FileOutputStream(outputFile);
        out.write(contents);
        out.close();
        progress.addMessage("Installed '" + outputFile.getAbsolutePath() + "' (" + entry.getSize() + " bytes)...");
    }
}

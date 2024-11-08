package com.arsenal.util;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.awt.*;
import javax.swing.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Util {

    public static boolean runCommand(String command) {
        try {
            Runtime.getRuntime().exec(command);
            return true;
        } catch (IOException ioe) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean unJar(File file) {
        if ((file != null) && file.isFile()) {
            return runCommand("jar xvf " + file.getPath());
        }
        return false;
    }

    public static boolean unJarAllFilesInDirectory(File directory) {
        if ((directory != null) && directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith(".jar") || files[i].getName().endsWith(".zip")) {
                    unJar(files[i]);
                }
            }
            return true;
        } else return false;
    }

    public static Properties loadPropertiesFromFilename(String filename) {
        try {
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream(filename);
            props.load(fis);
            return props;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean storePropertiesToFilename(Properties props, String filename) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            props.store(fos, "Do Not Edit!");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyfile(String file0, String file1) {
        try {
            File f0 = new File(file0);
            File f1 = new File(file1);
            FileInputStream in = new FileInputStream(f0);
            FileOutputStream out = new FileOutputStream(f1);
            int c;
            while ((c = in.read()) != -1) out.write(c);
            in.close();
            out.close();
            in = null;
            out = null;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean copyfileIfNeccessary(String file0, String file1) {
        File f0 = new File(file0);
        File f1 = new File(file1);
        if (!f0.exists() || !f0.isFile()) return false;
        if (f1.exists() && f1.isFile()) {
            if (f1.length() == f0.length()) return true; else return copyfile(file0, file1);
        } else {
            return copyfile(file0, file1);
        }
    }

    public static boolean removeDirectory(String dir) {
        File fdir = new File(dir);
        if (!fdir.exists() || !fdir.isDirectory()) return false;
        try {
            String[] list = fdir.list();
            for (int i = 0; i < list.length; i++) {
                File f = new File(dir + "/" + list[i]);
                f.delete();
            }
            return fdir.delete();
        } catch (Exception e) {
            return false;
        }
    }

    public static void moveAllImagesToTargetDir(String sourcedir, String targetdir) {
        File fdir = new File(sourcedir);
        if (!fdir.exists() || !fdir.isDirectory()) return;
        String[] list = fdir.list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].endsWith("jpg") || list[i].endsWith("gif") || list[i].endsWith("JPG") || list[i].endsWith("GIF") || list[i].endsWith("bmp") || list[i].endsWith("BMP")) {
                Util.copyfileIfNeccessary(sourcedir + File.separator + list[i], targetdir + File.separator + list[i]);
            }
        }
    }

    public static void removeAllImagesFromDir(String sourcedir) {
        File fdir = new File(sourcedir);
        if (!fdir.exists() || !fdir.isDirectory()) return;
        String[] list = fdir.list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].endsWith("jpg") || list[i].endsWith("gif") || list[i].endsWith("JPG") || list[i].endsWith("GIF") || list[i].endsWith("bmp") || list[i].endsWith("BMP")) {
                File f = new File(sourcedir + File.separator + list[i]);
                f.delete();
            }
        }
    }

    public static void removeAllFilesInDirectoryBeginningWith(String sourcedir, String keyword) {
        File fdir = new File(sourcedir);
        if (!fdir.exists() || !fdir.isDirectory()) return;
        String[] list = fdir.list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].startsWith(keyword)) {
                File f = new File(sourcedir + File.separator + list[i]);
                f.delete();
            }
        }
    }

    public static void changeJMenuItemComponentColorsForJMenu(JMenu menu, Color backgroundcolor, Color foregroundcolor) {
        menu.setBackground(foregroundcolor);
        menu.setForeground(backgroundcolor);
        Component[] items = menu.getMenuComponents();
        for (int i = 0; i < items.length; i++) {
            menu.getMenuComponent(i).setBackground(foregroundcolor);
            menu.getMenuComponent(i).setForeground(backgroundcolor);
        }
    }

    public void changeIconsIfNeccessary(String iconPath, Component component) {
    }

    public static void main(String[] args) {
        System.out.println("Running unjar util");
        if (args[0] == null) {
            System.out.println("no argument given, please provide a directory to unjar files.");
            System.exit(1);
        }
        File f = new File(args[0]);
        System.out.println("directory to unjar files in: " + f.getPath());
        unJarAllFilesInDirectory(f);
        System.out.println("finished unjar'ing file in: " + f.getPath());
    }

    /******************************************************************
    *
    * media play methods
    *
    ******************************************************************/
    private static final int EXTERNAL_BUFFER_SIZE = 128000;

    public static void playAudioFile(File audioFile) {
        if (!audioFile.exists()) return;
        AudioInputStream audioInputStream = null;
        AudioFormat audioFormat = null;
        SourceDataLine line = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            audioFormat = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            int bytesRead = 0;
            byte[] data = new byte[EXTERNAL_BUFFER_SIZE];
            while (bytesRead != -1) {
                try {
                    bytesRead = audioInputStream.read(data, 0, data.length);
                } catch (IOException e) {
                }
                if (bytesRead >= 0) {
                    int bytesWritten = line.write(data, 0, bytesRead);
                }
            }
            line.drain();
            line.close();
        } catch (Exception e) {
        }
        line = null;
        audioInputStream = null;
        audioFormat = null;
    }

    public static void playAudioFileInThread(final String filename) {
        try {
            Runnable runner = new Runnable() {

                public void run() {
                    try {
                        playAudioFile(new File(filename));
                    } catch (Exception e) {
                    }
                }
            };
            new ReThread(runner, "playAudioFileInThread.run").start();
        } catch (Exception e) {
        }
    }

    public static void setDefaultLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ex) {
        }
    }
}

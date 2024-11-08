package org.hfbk.util;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Label;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.hfbk.ui.UIUtils;

public class Installer {

    public static void unzip(String zipfile, String targetdir) throws IOException {
        ZipFile jar = new ZipFile(zipfile);
        Enumeration<? extends ZipEntry> fs = jar.entries();
        while (fs.hasMoreElements()) {
            ZipEntry file = fs.nextElement();
            if (file.isDirectory()) continue;
            File f = new File(targetdir + File.separator + file.getName());
            if (f.exists() && f.lastModified() > file.getTime()) continue;
            System.out.println("Extracting " + f.getPath());
            String path = f.getParent();
            new File(path).mkdirs();
            InputStream is = jar.getInputStream(file);
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) > 0) fos.write(buffer, 0, read);
            fos.close();
            is.close();
        }
    }

    public static void unzip(URL zipfile, String targetdir) throws IOException {
        ZipInputStream jar = new ZipInputStream(zipfile.openStream());
        ZipEntry file;
        while ((file = jar.getNextEntry()) != null) {
            if (file.isDirectory()) continue;
            File f = new File(targetdir + File.separator + file.getName());
            if (f.exists() && f.lastModified() > file.getTime()) continue;
            System.out.println("Extracting " + f.getPath());
            String path = f.getParent();
            new File(path).mkdirs();
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = jar.read(buffer)) > 0) fos.write(buffer, 0, read);
            fos.close();
        }
        jar.close();
    }

    static void install(File dir) {
        try {
            URL source = Installer.class.getProtectionDomain().getCodeSource().getLocation();
            System.out.println(source);
            unzip(source, dir.getAbsolutePath() + File.separator + "VisClient");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Installer() {
        Frame f = new Frame("Vis/Installer");
        f.setUndecorated(true);
        f.add(new Label("Select directory where the VisClient directory should be created:", Label.CENTER), BorderLayout.NORTH);
        JFileChooser fc = new JFileChooser("") {

            public void approveSelection() {
                setVisible(false);
                install(getSelectedFile());
                JOptionPane.showMessageDialog(this, "Yo.");
                System.exit(0);
            }

            public void cancelSelection() {
                System.exit(1);
            }
        };
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        f.add(fc);
        UIUtils.blackify(f);
        f.setSize(640, 480);
        f.setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        new Installer();
    }
}

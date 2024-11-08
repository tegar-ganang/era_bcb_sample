package br.cefetpe.tsi.ww.zip;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.JFileChooser;

public class Unzip {

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public void extract(String args) throws IOException {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(args);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    System.err.println("Extracting directory: " + entry.getName());
                    (new File(entry.getName())).mkdir();
                    continue;
                }
                System.err.println("Extracting file: " + entry.getName());
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream("\\data\\" + entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    public static void main(String[] args) {
        JFileChooser jfc = new JFileChooser();
        jfc.showOpenDialog(null);
        Unzip unzipper = new Unzip();
        try {
            unzipper.extract(jfc.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

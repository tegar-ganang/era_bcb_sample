package org.phoneid.keepassinstaller;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.EventQueue;
import javax.swing.JFileChooser;
import java.io.*;
import java.util.jar.*;
import java.nio.channels.FileChannel;

public final class KeePassInstaller implements Runnable {

    public static void main(String[] args) {
        EventQueue.invokeLater(new KeePassInstaller());
    }

    public void run() {
        try {
            JFileChooser fc = new JFileChooser();
            fc.addChoosableFileFilter(new KDBFilter());
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File dir = fc.getCurrentDirectory();
                File file = fc.getSelectedFile();
                System.out.println(file.getAbsolutePath());
                copyFile(file.getAbsolutePath(), "Database.kdb");
            } else {
                System.err.println("Cannot open file");
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void copyFile(String from, String to) throws IOException {
        FileChannel srcChannel = new FileInputStream(from).getChannel();
        FileChannel dstChannel = new FileOutputStream(to).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    public void copyStream(InputStream in, OutputStream out) throws IOException {
        byte buffer[] = new byte[1024];
        while (true) {
            int nRead = in.read(buffer, 0, buffer.length);
            if (nRead <= 0) break;
            out.write(buffer, 0, nRead);
        }
    }

    public void createJar(String pathKDB, String nameKDB) throws IOException {
        JarEntry jarAdd;
        copyFile(pathKDB, nameKDB);
        FileInputStream inStreamManifest = new FileInputStream("META-INF/MANIFEST.MF");
        FileOutputStream outStream = new FileOutputStream("KeePassJ2ME-kdb.jar");
        JarOutputStream out = new JarOutputStream(outStream, new Manifest(inStreamManifest));
        jarAdd = new JarEntry(nameKDB);
        out.putNextEntry(jarAdd);
        FileInputStream in = new FileInputStream(nameKDB);
        copyStream(in, out);
        in.close();
        out.close();
    }
}

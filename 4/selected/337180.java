package net.sourceforge.ubcdcreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JTextArea;

public class UnzipThread extends Thread {

    private String filename = null;

    private String dstdir = null;

    private String complete = "Complete.";

    private TextUpdater textUpdater = null;

    public UnzipThread(String filename, String dstdir, boolean stdout, JTextArea textBox, String complete) {
        this.filename = filename;
        this.dstdir = dstdir;
        this.complete = complete;
        textUpdater = new TextUpdater(textBox, stdout);
    }

    public void run() {
        try {
            textUpdater.start();
            int cnt;
            byte[] buf = new byte[4096];
            File file = null;
            ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(filename)));
            ZipEntry ze = zis.getNextEntry();
            FileOutputStream fos;
            while (ze != null) {
                if (ze.isDirectory()) {
                    file = new File(ze.getName());
                    if (!file.exists()) {
                        textUpdater.appendText("Creating directory: " + ze.getName() + "\n");
                        file.mkdirs();
                    }
                } else {
                    textUpdater.appendText("Extracting file: " + ze.getName() + "\n");
                    fos = new FileOutputStream(dstdir + File.separator + ze.getName());
                    while ((cnt = zis.read(buf, 0, buf.length)) != -1) fos.write(buf, 0, cnt);
                    fos.close();
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.close();
            if (complete != null) textUpdater.appendText(complete + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        textUpdater.setFinished(true);
    }
}

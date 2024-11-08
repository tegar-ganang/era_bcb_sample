package net.sf.gham.plugins.commands.tools.filemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JTextArea;
import net.sf.gham.core.util.constants.Directory;
import net.sf.jtwa.Messages;

/**
 * @author fabio
 *
 */
class DbBackupper {

    private final IProcessFinishListener processFinishListener;

    public DbBackupper(IProcessFinishListener listener) {
        processFinishListener = listener;
    }

    public void createBackup(JTextArea out) {
        File dir = new File(Directory.BACKUP);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        new ZipThread(out).start();
    }

    private class ZipThread extends Thread {

        private final JTextArea out;

        public ZipThread(JTextArea out) {
            this.out = out;
        }

        @Override
        public void run() {
            String fileName = "backup" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".zip";
            out.append(Messages.getString("Creating_file") + " " + fileName + "...\n");
            ZipOutputStream zos = null;
            try {
                zos = new ZipOutputStream(new FileOutputStream(new File(Directory.BACKUP + fileName)));
                zipDir(out, Directory.ROOT, zos, Directory.ROOT.substring(0, Directory.ROOT.length() - 5));
            } catch (IOException e) {
                out.append(e.getMessage());
            } finally {
                try {
                    if (zos != null) {
                        zos.close();
                    }
                } catch (IOException e) {
                }
                processFinishListener.processFinished();
            }
        }
    }

    private void zipDir(JTextArea out, String dir2zip, ZipOutputStream zos, String pathToDir) throws IOException {
        File zipDir = new File(dir2zip);
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                String filePath = f.getPath();
                zipDir(out, filePath, zos, pathToDir);
                continue;
            }
            out.append(Messages.getString("Zipping_file") + " " + f.getName() + "...\n");
            FileInputStream fis = new FileInputStream(f);
            ZipEntry anEntry = new ZipEntry(f.getPath().substring(pathToDir.length()));
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        }
    }
}

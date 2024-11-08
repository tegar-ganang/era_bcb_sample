package ingenias.editor.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JWindow;
import ingenias.editor.IDE;
import ingenias.editor.IDEAbs;

public class AutomaticBackup implements Runnable {

    Thread backupthread = null;

    boolean finish = false;

    int backupFrequency = 0;

    IDEAbs ide;

    int counter = 0;

    public static final int MAXIMUM_BACKUPS = 10;

    public AutomaticBackup(IDEAbs ide2, int backupFrequency) {
        this.backupFrequency = backupFrequency;
        this.ide = ide2;
    }

    public void startBackup() {
        this.backupthread = new Thread(this);
        this.backupthread.start();
    }

    public boolean isStarted() {
        return backupthread != null;
    }

    private void moveFile(File orig, File target) throws IOException {
        byte buffer[] = new byte[1000];
        int bread = 0;
        FileInputStream fis = new FileInputStream(orig);
        FileOutputStream fos = new FileOutputStream(target);
        while (bread != -1) {
            bread = fis.read(buffer);
            if (bread != -1) fos.write(buffer, 0, bread);
        }
        fis.close();
        fos.close();
        orig.delete();
    }

    public int copyBackupFiles() {
        return 0;
    }

    public void run() {
        try {
            while (!finish) {
                Thread.currentThread().sleep(60 * backupFrequency * 1000);
                while (ide.ids.isBusy()) {
                    Thread.currentThread().sleep(1000);
                }
                if (IDE.changes) {
                    ide.setEnabled(false);
                    final JWindow jw = ide.showMessageWindow("Security Backup in progress...");
                    new Thread() {

                        public void run() {
                            while (!jw.isVisible()) {
                                Thread.currentThread().yield();
                            }
                            PersistenceManager p = new PersistenceManager();
                            JFileChooser jfc = new JFileChooser();
                            File homedir = jfc.getCurrentDirectory();
                            String filename = homedir.getPath() + "/.idk/idkbackup" + counter + ".xml";
                            File backup = new File(filename);
                            try {
                                p.save(backup, ide.ids);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            counter = (counter + 1) % MAXIMUM_BACKUPS;
                            jw.setVisible(false);
                            ide.setEnabled(true);
                        }
                    }.start();
                }
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        ide.setEnabled(true);
    }

    public void finish() {
        finish = true;
    }
}

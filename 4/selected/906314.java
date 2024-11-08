package dk.kapetanovic.jaft.action.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class FileHandler implements Serializable {

    private static int unique = 0;

    private File backupDir;

    public FileHandler(File backupDir) {
        this.backupDir = backupDir;
    }

    public FileHandler() {
    }

    public File generateBackup(File file) {
        File result = null;
        int no = 0;
        do {
            synchronized (FileHandler.class) {
                no = ++unique;
            }
            result = new File(backupDir, file.getName() + "-" + no + ".dat");
        } while (result.exists());
        return result;
    }

    public void restoreBackup(File original, File backup) throws IOException {
        System.out.print("restoring: ");
        if (backup.exists()) {
            if (original.exists()) delete(original);
            rename(backup, original);
        }
    }

    public void delete(File file) throws IOException {
        System.out.println("delete " + file);
        if (!file.delete()) throw new IOException("Error deleting file " + file.getAbsolutePath());
    }

    public void rename(File source, File dest) throws IOException {
        System.out.println("rename " + source + " -> " + dest);
        if (!source.renameTo(dest)) throw new IOException("Error renaming file " + source.getAbsolutePath() + " to " + dest.getAbsolutePath());
    }

    /** Copy source to dest, overwriting dest. */
    public void copy(File source, File dest) throws IOException {
        System.out.println("copy " + source + " -> " + dest);
        FileInputStream in = new FileInputStream(source);
        try {
            FileOutputStream out = new FileOutputStream(dest);
            try {
                byte[] buf = new byte[1024];
                int len = 0;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public void setBackupDir(File dir) {
        this.backupDir = dir;
    }
}

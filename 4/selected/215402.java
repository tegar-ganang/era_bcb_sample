package gnu.fishingcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import gnu.fishingcat.sql.SQLCommand;

public class BackupAction extends AbstractAction {

    Component _parent;

    public BackupAction(Component parent) {
        super("Backup Data", Kit.getIcon("backup"));
        _parent = parent;
    }

    public void actionPerformed(ActionEvent e) {
        File file = Kit.getSaveFile(_parent, Kit.FCAT_FILE_FILTER);
        if (file == null) {
            return;
        }
        if (!file.getName().endsWith(".fcat")) {
            String path = file.getAbsolutePath();
            path += ".fcat";
            file = new File(path);
        }
        try {
            go(file);
            JOptionPane.showMessageDialog(_parent, "Backup Successful", "Success!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(_parent, "Backup Failed", "Groan", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static File snapshot() throws Exception {
        Date now = new Date();
        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(now);
        File backup = new File(Kit.getDataDir() + "save-" + time + ".fcat");
        go(backup);
        return backup;
    }

    public static void go(File file) throws Exception {
        try {
            Preferences prefs = Preferences.userNodeForPackage(BackupAction.class);
            FileOutputStream fos = new FileOutputStream(Kit.getDataDir() + Kit.PREFS_FILE);
            prefs.exportSubtree(fos);
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String datadir = Kit.getDataDir();
        FileOutputStream fos = new FileOutputStream(file);
        SQLCommand.execute("SHUTDOWN");
        zipDir(datadir, datadir, new ZipOutputStream(fos));
        fos.close();
    }

    public static void zipDir(String relativeTo, String dir2zip, ZipOutputStream zos) throws Exception {
        File zipDir = new File(dir2zip);
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                if (f.getName().equals("CVS")) {
                    continue;
                }
                String filePath = f.getPath();
                zipDir(relativeTo, filePath, zos);
                continue;
            }
            if (f.getName().startsWith("save-")) {
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            ZipEntry anEntry = new ZipEntry(f.getPath().substring(relativeTo.length()));
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            zos.closeEntry();
            fis.close();
        }
    }
}

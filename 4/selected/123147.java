package omschaub.azcvsupdater.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import org.eclipse.swt.widgets.TableItem;

public class BackupUtils {

    public static void makeBackup(File dir, String sourcedir, String destinationdir, String destinationDirEnding) {
        String[] files;
        files = dir.list();
        File checkdir = new File(destinationdir + System.getProperty("file.separator") + destinationDirEnding);
        if (!checkdir.isDirectory()) {
            checkdir.mkdir();
        }
        ;
        Date date = new Date();
        long msec = date.getTime();
        checkdir.setLastModified(msec);
        File checkFile = new File(checkdir + System.getProperty("file.separator") + "azureus.config");
        if (checkFile.exists()) {
            checkFile.setLastModified(msec);
        }
        try {
            for (int i = 0; i < files.length; i++) {
                File f = new File(dir, files[i]);
                File g = new File(files[i]);
                if (f.isDirectory()) {
                } else {
                    String destinationFile = checkdir + System.getProperty("file.separator") + g;
                    String sourceFile = sourcedir + System.getProperty("file.separator") + g;
                    FileInputStream infile = new FileInputStream(sourceFile);
                    FileOutputStream outfile = new FileOutputStream(destinationFile);
                    int c;
                    while ((c = infile.read()) != -1) outfile.write(c);
                    infile.close();
                    outfile.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[][] backupSort(final TableItem[] items_to_sort, final int type, final int columns, boolean ascending) {
        TableItem item_temp1, item_temp2;
        String[][] results = new String[items_to_sort.length][columns];
        for (int i = 0; i < items_to_sort.length; i++) {
            for (int j = 0; j < items_to_sort.length - 1; j++) {
                if (ascending) {
                    if (items_to_sort[j].getText(type).compareToIgnoreCase(items_to_sort[j + 1].getText(type)) > 0 || items_to_sort[j].getText(type).compareToIgnoreCase(items_to_sort[j + 1].getText(type)) == 0) {
                        item_temp1 = items_to_sort[j];
                        item_temp2 = items_to_sort[j + 1];
                        items_to_sort[j] = item_temp2;
                        items_to_sort[j + 1] = item_temp1;
                    }
                } else {
                    if (items_to_sort[j].getText(type).compareToIgnoreCase(items_to_sort[j + 1].getText(type)) < 0 || items_to_sort[j].getText(type).compareToIgnoreCase(items_to_sort[j + 1].getText(type)) == 0) {
                        item_temp1 = items_to_sort[j];
                        item_temp2 = items_to_sort[j + 1];
                        items_to_sort[j] = item_temp2;
                        items_to_sort[j + 1] = item_temp1;
                    }
                }
            }
        }
        for (int j = 0; j < items_to_sort.length; j++) {
            for (int h = 0; h < columns; h++) {
                results[j][h] = items_to_sort[j].getText(h);
            }
        }
        return results;
    }
}

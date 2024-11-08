package game.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: lagon
 * Date: 21-Dec-2009
 * Time: 14:34:50
 * To change this template use File | Settings | File Templates.
 */
public class ZipFolder {

    private static String dirSep;

    public static void doZipFolder(String folderPath, String zipName, boolean deleteSource) throws IOException {
        dirSep = System.getProperty("file.separator");
        File f = new File(zipName);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f));
        zipDir(folderPath, ".", zos);
        zos.close();
        if (deleteSource) {
            deleteContentRecursively(folderPath);
        }
        File f2 = new File(folderPath + dirSep + zipName);
        FileHelper.move(f, f2, true);
    }

    private static void deleteContentRecursively(String folderPath) {
        File dir = new File(folderPath);
        System.out.printf("Processing %s\n", folderPath);
        File[] dirList = dir.listFiles();
        for (int i = 0; i < dirList.length; i++) {
            File f = dirList[i];
            if (f.isDirectory()) {
                deleteContentRecursively(folderPath + dirSep + f.getName());
            }
            f.delete();
        }
    }

    private static void zipDir(String basedir, String dir2zip, ZipOutputStream zos) throws IOException {
        File zipDir = new File(basedir + dirSep + dir2zip);
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                String filePath = dir2zip + dirSep + f.getName();
                zipDir(basedir, filePath, zos);
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            ZipEntry anEntry = new ZipEntry(dir2zip + dirSep + f.getName());
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        }
    }
}

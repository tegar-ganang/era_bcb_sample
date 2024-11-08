package de.blitzcoder.collide;

import de.blitzcoder.collide.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author blitzcoder
 * This class is used to Copy the config files from /de/blitzcoder/collide/standardfiles/ next to the jar
 */
public class StandardFiles {

    public static void check() {
        String[] files = new String[] { "collide.props", "interface.xml", "textarea.props", "filetypes.xml", "blitzmax.xml", "menu.xml", "templates.obj" };
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            if (files[i].toLowerCase().endsWith(".zip")) {
                extract(files[i]);
            } else if (!file.exists()) {
                System.out.println("Missing file noticed: " + file.getName() + ". Creating.");
                copyFile(files[i]);
            }
        }
    }

    private static void copyFile(String fileName) {
        File file = new File(fileName);
        try {
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            InputStream in = StandardFiles.class.getResourceAsStream("/de/blitzcoder/collide/standardfiles/" + fileName);
            while (in.available() != 0) {
                out.write(in.read());
            }
            in.close();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void extract(String fileName) {
        File file = new File("./");
        ZipInputStream zip = new ZipInputStream(StandardFiles.class.getResourceAsStream("/de/blitzcoder/collide/standardfiles/" + fileName));
        try {
            byte[] buf = new byte[1024];
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                File newFile = new File(file, entry.getName());
                if (newFile.exists()) {
                    entry = zip.getNextEntry();
                    continue;
                }
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                    entry = zip.getNextEntry();
                    continue;
                }
                System.out.println("Missing file noticed: " + newFile.getPath().substring(2) + ". Decompressing.");
                newFile.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(newFile);
                int n;
                while ((n = zip.read(buf, 0, 1024)) > -1) {
                    outputStream.write(buf, 0, n);
                }
                outputStream.close();
                zip.closeEntry();
                entry = zip.getNextEntry();
            }
        } catch (Exception ex) {
            Log.log("Failed to decompress " + fileName);
            ex.printStackTrace();
        }
    }
}

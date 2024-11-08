package net.sf.kosmagene.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;

public class FileManager {

    public static void copy(File source, File target) throws IOException {
        if (source.getName().startsWith(".")) return;
        if (source.isDirectory()) {
            target.mkdirs();
            String[] files = source.list();
            for (int i = 0; i < files.length; i++) {
                File srcfile = new File(source.getAbsoluteFile(), files[i]);
                File dstfile = new File(target.getAbsoluteFile(), files[i]);
                copy(srcfile, dstfile);
            }
        } else {
            FileInputStream sourceStream = new FileInputStream(source);
            target.createNewFile();
            FileOutputStream targetStream = new FileOutputStream(target);
            copy(sourceStream, targetStream);
        }
    }

    public static void copy(FileInputStream source, FileOutputStream target) throws IOException {
        FileChannel sourceChannel = source.getChannel();
        FileChannel targetChannel = target.getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        sourceChannel.close();
        targetChannel.close();
    }

    public static void copy(URL sourceUrl, File target) throws IOException {
        try {
            URI sourceUri = new URI(sourceUrl.toString());
            File sourceFile = new File(sourceUri);
            copy(sourceFile, target);
        } catch (URISyntaxException e) {
            throw new IOException("Failed to copy file, bad formed URI. " + e.getMessage());
        } catch (IOException e) {
            throw e;
        }
    }
}

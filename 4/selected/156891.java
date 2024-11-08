package l1j.server.server.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

/**
 * 解壓縮程序
 * using Apache ant.jar
 */
public class UnZipUtil {

    /**
	 * Zip檔解壓縮
	 * @param zipFile 要解壓縮的檔案
	 * @param ToPath 目的路徑 
	 */
    public static void unZip(String zipFile, String ToPath) {
        try {
            ZipFile zipfile = new ZipFile(zipFile);
            Enumeration<?> zipenum = zipfile.getEntries();
            while (zipenum.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) zipenum.nextElement();
                File newFile = new File(ToPath, ze.getName());
                ReadableByteChannel rc = Channels.newChannel(zipfile.getInputStream(ze));
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    FileOutputStream fos = new FileOutputStream(newFile);
                    FileChannel fc = fos.getChannel();
                    fc.transferFrom(rc, 0, ze.getSize());
                    fos.close();
                }
            }
            zipfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package base.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;

/**
 * This class provides some basic methods to compress/uncompress files and
 * folders in zip compression format.
 * 
 * @author Guido Angelo Ingenito
 */
public class ZipUtil {

    private static final transient Logger logger = Logger.getLogger(ZipUtil.class.getName());

    /**
	 * This method is a wrapper for the method the zipDirectory(File,
	 * ZipOutputStream). It provides a more easy interface to be used when must
	 * be zipped a file or a folder.
	 * 
	 * @param inputFile
	 *            The input file/folder to be zipped.
	 * @param outputFile
	 *            The zipped outputted file.
	 * @throws IOException
	 *             If something wrong happpens.
	 */
    public static void zipDirectory(File inputFile, File outputFile) throws IOException {
        logger.debug("zipDirectory(in:" + inputFile.toString() + ", out:" + outputFile.toString() + ")");
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
        zipDirectory(inputFile, zipOutputStream);
        zipOutputStream.close();
    }

    /**
	 * This method provides a simple zip compression for a file or an entire
	 * folder.
	 * 
	 * @param inputFile
	 *            The input file/folder whose content must be zipped.
	 * @param zipOutputStream
	 *            The target output stream that points to the location where the
	 *            zip file must be placed.
	 * @throws IOException
	 *             If something wrong happens.
	 */
    public static void zipDirectory(File inputFile, ZipOutputStream zipOutputStream) throws IOException {
        String[] dirList = inputFile.list();
        byte[] readBuffer = new byte[1024];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File file = new File(inputFile, dirList[i]);
            if (file.isDirectory()) {
                zipDirectory(file, zipOutputStream);
                continue;
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(file.getPath());
            zipOutputStream.putNextEntry(zipEntry);
            while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                zipOutputStream.write(readBuffer, 0, bytesIn);
            }
            fileInputStream.close();
        }
    }

    public static void zipAllFile(File[] inputFile, File outputFile) throws IOException {
        logger.debug("zipAllFile(in:" + inputFile.length + ", out:" + outputFile.toString() + ")");
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
        zipAllFile(inputFile, zipOutputStream);
        zipOutputStream.close();
    }

    public static void zipAllFile(File[] inputFile, ZipOutputStream zipOutputStream) throws IOException {
        byte[] readBuffer = new byte[1024];
        int bytesIn = 0;
        for (int i = 0; i < inputFile.length; i++) {
            if (inputFile[i].isDirectory()) {
                zipDirectory(inputFile[i], zipOutputStream);
                continue;
            }
            FileInputStream fileInputStream = new FileInputStream(inputFile[i]);
            ZipEntry zipEntry = new ZipEntry(inputFile[i].getPath());
            zipOutputStream.putNextEntry(zipEntry);
            while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                zipOutputStream.write(readBuffer, 0, bytesIn);
            }
            fileInputStream.close();
        }
    }

    /**
	 * This method is a wrapper for the method the zipFile(File,
	 * ZipOutputStream). It provides a more easy interface to be used when must
	 * be zipped a file (not a folder).
	 * 
	 * @param inputFile
	 *            The input file (not a folder) to be zipped.
	 * @param outputFile
	 *            The zipped outputted file.
	 * @throws IOException
	 *             If something wrong happpens.
	 */
    public static void zipFile(File inputFile, File outputFile) throws IOException {
        logger.debug("zipFile(in:" + inputFile.toString() + ", out:" + outputFile.toString() + ")");
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
        zipFile(inputFile, zipOutputStream);
        zipOutputStream.close();
    }

    /**
	 * This method provides a simple zip compression for a file (not a folder).
	 * 
	 * @param inputFile
	 *            The file that must be zippede.
	 * @param zipOutputStream
	 *            The target output stream that points to the location where the
	 *            zip file must be placed.
	 * @throws IOException
	 *             If something wrong happens.
	 */
    public static void zipFile(File inputFile, ZipOutputStream zipOutputStream) throws IOException {
        byte[] buf = new byte[1024];
        zipOutputStream.putNextEntry(new ZipEntry(inputFile.getName()));
        FileInputStream fileInputStream = new FileInputStream(inputFile);
        int length;
        while ((length = fileInputStream.read(buf)) > 0) {
            zipOutputStream.write(buf, 0, length);
        }
        fileInputStream.close();
        zipOutputStream.closeEntry();
    }

    /**
	 * This method provides a simple unzip facility for a composite zip file (a
	 * zip file that contains files and folders).
	 * 
	 * @param inputFile
	 *            The zip file that must be extracted.
	 * @param outputDirectory
	 *            The destination folder where the content of the input zipped
	 *            file must be placed.
	 * @throws IOException
	 *             If something wrong happens.
	 */
    public static void unZipFile(File inputFile, File outputDirectory) throws IOException {
        logger.debug("unZipFile(in:" + inputFile.toString() + ", out:" + outputDirectory.toString() + ")");
        ZipFile zipFile = new ZipFile(inputFile);
        Enumeration zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
            logger.debug("Unpacking: " + zipEntry.getName());
            File file = new File(outputDirectory, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                file.mkdirs();
            } else {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                File dir = new File(file.getParent());
                dir.mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int readByte;
                while ((readByte = bis.read()) != -1) {
                    bos.write((byte) readByte);
                }
                bos.close();
                fos.close();
            }
            logger.debug(zipEntry.getName() + " : Unpacked.");
        }
    }
}

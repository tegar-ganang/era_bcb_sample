package org.project.trunks.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.project.trunks.utilities.DateUtil;
import org.project.trunks.utilities.FileUtilities;
import org.apache.commons.logging.*;
import java.io.IOException;

public class ZipUtility {

    /**
   * Logger
   */
    private static org.apache.commons.logging.Log log = LogFactory.getLog(FileUtilities.class);

    protected String _startDir2zip;

    /**
   * zipDirectory
   * @param dir2zip Directory to zip
   * @param zipFileName Result file name ( with complete path )
   * @throws java.lang.Exception
   */
    public void zipDirectory(String dir2zip, String zipFileName) throws Exception {
        zipDirectory(dir2zip, zipFileName, false);
    }

    public void zipDirectory(String dir2zip, String zipFileName, boolean removeSrcDir) throws Exception {
        log.info("<<<< ZipUtility.zipDirectory('" + dir2zip + "') -> '" + zipFileName + "' - Begin");
        _startDir2zip = dir2zip;
        File zipDir = new File(dir2zip);
        if (!zipDir.isDirectory()) throw new Exception("File [" + dir2zip + "] is not a directory.");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
        zipDirectory(dir2zip, zos);
        zos.close();
        if (removeSrcDir) {
            FileUtilities.deleteDirectory(new File(dir2zip + File.separator));
            new File(dir2zip).delete();
        }
        log.info("<<<< ZipUtility.zipDirectory('" + dir2zip + "') -> '" + zipFileName + "' - End");
    }

    public void zipDirectory(String dir2zip, ZipOutputStream zos) throws Exception {
        File zipDir = new File(dir2zip);
        if (!zipDir.isDirectory()) throw new Exception("File [" + dir2zip + "] is not a directory.");
        String[] dirList = zipDir.list();
        if (dirList.length == 0) {
            zos.putNextEntry(new ZipEntry(getPath(dir2zip, _startDir2zip) + File.separator + "."));
        } else {
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDirectory(filePath, zos);
                    continue;
                }
                zipFile(f, zos);
            }
        }
    }

    /**
   * zipFile
   * @param file2zip File to zip
   * @param zipFileName Result file name ( with complete path )
   * @throws java.lang.Exception
   */
    public void zipFile(String file2zip, String zipFileName) throws Exception {
        zipFile(file2zip, zipFileName, false);
    }

    public void zipFile(String file2zip, String zipFileName, boolean removeSrcFile) throws Exception {
        log.info("<<<< ZipUtility.zipFile('" + file2zip + "') -> '" + zipFileName + "' - Begin");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
        zipFile(new File(file2zip), zos);
        zos.close();
        if (removeSrcFile) {
            FileUtilities.deleteDirectory(new File(file2zip + File.separator));
            new File(file2zip).delete();
        }
        log.info("<<<< ZipUtility.zipFile('" + file2zip + "') -> '" + zipFileName + "' - End");
    }

    private void zipFile(File f, ZipOutputStream zos) throws Exception {
        byte[] readBuffer = new byte[2048];
        int bytesIn = 0;
        FileInputStream fis = new FileInputStream(f);
        ZipEntry anEntry = new ZipEntry(getPath(f.getPath(), _startDir2zip));
        zos.putNextEntry(anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1) {
            zos.write(readBuffer, 0, bytesIn);
        }
        fis.close();
    }

    public void zipDirectory(String dir2zip, String dirTemp, ByteArrayOutputStream baos, boolean removeSrcDir) throws Exception {
        String fileName = dirTemp + File.separator + "tmp" + new DateUtil().getUniqueTime() + ".zip";
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
        zipDirectory(dir2zip, zos);
        zos.close();
        FileInputStream fileStream = new FileInputStream(fileName);
        FileUtilities.autoPipeStream(fileStream, baos);
        fileStream.close();
        File f = new File(fileName);
        if (f.exists()) f.delete();
        if (removeSrcDir) {
            FileUtilities.deleteDirectory(new File(dir2zip + File.separator));
            new File(dir2zip).delete();
        }
    }

    /**
   * unzipFile
   * @param zipFile Zip file
   * @param destDir
   * @param removeSrcFile
   * @throws java.lang.Exception
   */
    public void unzipDirectory(String zipFile, String destDir) throws Exception {
        unzipDirectory(zipFile, destDir, false);
    }

    public void unzipDirectory(String zipFile, String destDir, boolean removeSrcFile) throws Exception {
        log.info("<<<< ZipUtility.unzipDirectory('" + zipFile + "') -> '" + destDir + "' - Begin");
        File fdesdir = new File(destDir);
        if (!fdesdir.exists()) {
            fdesdir.mkdirs();
        }
        unzipFile(new File(zipFile), fdesdir, removeSrcFile);
        log.info("<<<< ZipUtility.unzipDirectory('" + zipFile + "') -> '" + destDir + "' - End");
    }

    /**
   * unzipFile
   * @param zipFile Zip file
   * @param destFile Result file name
   * @param removeSrcFile
   * @throws java.lang.Exception
   */
    public void unzipFile(String zipFile, String destFile) throws Exception {
        unzipFile(zipFile, destFile, false);
    }

    public void unzipFile(String zipFile, String destFile, boolean removeSrcFile) throws Exception {
        log.info("<<<< ZipUtility.unzipFile('" + zipFile + "') -> '" + destFile + "' - Begin");
        unzipFile(new File(zipFile), new File(destFile), removeSrcFile);
        log.info("<<<< ZipUtility.unzipFile('" + zipFile + "') -> '" + destFile + "' - End");
    }

    public static void unzipFile(File zipFile, File destFile, boolean removeSrcFile) throws Exception {
        ZipInputStream zipinputstream = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipentry = zipinputstream.getNextEntry();
        int BUFFER_SIZE = 4096;
        while (zipentry != null) {
            String entryName = zipentry.getName();
            log.info("<<<<<< ZipUtility.unzipFile - Extracting: " + zipentry.getName());
            File newFile = null;
            if (destFile.isDirectory()) newFile = new File(destFile, entryName); else newFile = destFile;
            if (zipentry.isDirectory() || entryName.endsWith(File.separator + ".")) {
                newFile.mkdirs();
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                byte[] bufferArray = buffer.array();
                FileUtilities.createDirectory(newFile.getParentFile());
                FileChannel destinationChannel = new FileOutputStream(newFile).getChannel();
                while (true) {
                    buffer.clear();
                    int lim = zipinputstream.read(bufferArray);
                    if (lim == -1) break;
                    buffer.flip();
                    buffer.limit(lim);
                    destinationChannel.write(buffer);
                }
                destinationChannel.close();
                zipinputstream.closeEntry();
            }
            zipentry = zipinputstream.getNextEntry();
        }
        zipinputstream.close();
        if (removeSrcFile) {
            if (zipFile.exists()) zipFile.delete();
        }
    }

    /**
   * getPath
   * @param inputPath String
   * @param refDir2zip
   * @return relative file name
   * @throws IOException
   */
    private static String getPath(String inputPath, String refDir2zip) throws IOException {
        if (refDir2zip == null) refDir2zip = new File(inputPath).getCanonicalFile().getParent();
        int pos = refDir2zip.length();
        if (!refDir2zip.endsWith(File.separator)) pos++;
        return inputPath.substring(pos);
    }
}

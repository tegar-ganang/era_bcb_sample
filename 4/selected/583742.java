package ti.mcore.u.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import ti.mcore.u.StringUtil;

/**
 * Zip/Unzip - related utilities.
 *
 * @author alex.k@ti.com
 */
public class ZipUtil {

    public static final String ZIP_EXT = ".zip";

    private ZipUtil() {
    }

    /**
   * Return InputStream for <code>zipEntryName</code> assuming that zip file
   * name is <code>zipEntryName+".zip"</code>.<br>
   * If <code>zipEntryName == null</code>, return <code>null</code>.<br>
   * <br>
   * <br>E.g.<br>
   * 
   * Don't forget to close this input stream when you are done!
   * 
   * @param zipEntryName
   * @return entry InputStream
   * 
   * @throws IOException
   * @throws NullPointerException if there is no <code>zipEntryName == null</code>
   * or no <code>zipEntryName</code> entry in .zip file.
   * 
   * @author alex.k@ti.com
   */
    public static InputStream getEntry(String zipFilePath, String zipEntryName) throws IOException, NullPointerException {
        if (zipFilePath == null) {
            throw new NullPointerException("zipFilePath == null");
        }
        if (zipEntryName == null) {
            throw new NullPointerException("zipEntryName == null");
        }
        ZipFile zipFile = new ZipFile(zipEntryName + ZIP_EXT);
        ZipEntry entry = zipFile.getEntry(zipEntryName);
        return zipFile.getInputStream(entry);
    }

    /**
   * Return InputStream for <code>zipEntryPath</code>  assuming that zip file
   * name is <code>zipEntryName+".zip"</code>.<br>
   * If <code>zipEntryName == null</code>, return <code>null</code>.<br>
   * <br>
   * <br>E.g.<br>
   * 
   * Don't forget to close this input stream when you are done!
   * 
   * @param zipEntryName
   * @return entry InputStream
   * 
   * @throws IOException
   * @throws NullPointerException if there is no <code>zipEntryName == null</code>
   * or no <code>zipEntryName</code> entry in .zip file.
   * 
   * @author alex.k@ti.com
   */
    public static InputStream getEntry(String zipFilePath) throws NullPointerException, IOException {
        if (zipFilePath == null) {
            throw new NullPointerException("zipFilePath == null");
        }
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            throw new NullPointerException("\"" + zipFilePath + "\" does not exist");
        }
        String zipEntryName = zipFile.getName();
        zipEntryName = StringUtil.replaceSuffix(zipEntryName, ZIP_EXT, null);
        return getEntry(zipFilePath, zipEntryName);
    }

    private static final int BUFFER_SIZE = 2048;

    /** Store full path in zip when archiving */
    public static final int STORE_FULL_PATH_IN_ZIP = 0;

    /** Store only file names in zip when archiving */
    public static final int STORE_NAME_ONLY_IN_ZIP = 1;

    /** Store relative path in zip when archiving */
    public static final int STORE_RELATIVE_PATH_IN_ZIP = 2;

    /** Store path relative to root directory in zip when archiving */
    public static final int STORE_PATH_FROM_ZIP_ROOT = 3;

    /**
   * Create a zip file from directory
   *
   * @param zipName name of the file to create
   * @param rootDir root directory to archive
   * @param storePolicy the store policy to use (STORE_FULL_PATH_IN_ZIP,
   * STORE_NAME_ONLY_IN_ZIP, STORE_RELATIVE_PATH_IN_ZIP or
   * STORE_PATH_FROM_ZIP_ROOT)
   * @throws Exception if fails
   */
    public static void zip(String zipName, String rootDir, int storePolicy) throws Exception {
        if (zipName == null || rootDir == null) throw new Exception("Invalid arguments to create zip file");
        try {
            FileOutputStream fos = new FileOutputStream(zipName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            directoryWalker(rootDir, rootDir, zos, storePolicy);
            zos.flush();
            zos.finish();
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * Expand the content of the zip file
   *
   * @param zipName of the file to expand
   * @param targetDir where to place unzipped files
   */
    public static boolean unzip(String zipName, String targetDir) {
        File ftargetDir = new File(targetDir);
        if (!ftargetDir.exists()) ftargetDir.mkdirs();
        if (!ftargetDir.exists()) return false;
        File fzipname = new File(zipName);
        if (!fzipname.exists()) return false;
        boolean isCorruptedFile = false;
        try {
            FileInputStream fis = new FileInputStream(fzipname);
            ZipInputStream zis = new ZipInputStream(fis);
            try {
                ZipEntry entry;
                byte[] data = new byte[BUFFER_SIZE];
                while ((entry = zis.getNextEntry()) != null) {
                    int count;
                    String target = targetDir + File.separator + entry.getName().replace('\\', '/').replace('/', File.separatorChar);
                    File fget = new File(target);
                    if (entry.isDirectory()) {
                        fget.mkdirs();
                    } else {
                        fget.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(target);
                        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                        while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
                            dest.write(data, 0, count);
                        }
                        dest.flush();
                        dest.close();
                        fos.close();
                    }
                }
            } catch (EOFException ex) {
                isCorruptedFile = true;
            }
            zis.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return !isCorruptedFile;
    }

    /**
   * Walk through currentDir and recursively in its subdirectories. Each file
   * found is zipped.
   *
   * @param currentDir directory to walk through
   * @param rootDir root directory for path references
   * @param zos ZipOutputSteam to write to
   * @param storePolicy file path storing policy
   * @throws IOException if an error occurs
   */
    private static void directoryWalker(String currentDir, String rootDir, ZipOutputStream zos, int storePolicy) throws IOException {
        File dirObj = new File(currentDir);
        if (dirObj.exists() == true) {
            if (dirObj.isDirectory() == true) {
                File[] fileList = dirObj.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].isDirectory()) {
                        directoryWalker(fileList[i].getPath(), rootDir, zos, storePolicy);
                    } else if (fileList[i].isFile()) {
                        zipFile(fileList[i].getPath(), zos, storePolicy, rootDir);
                    }
                }
            } else {
            }
        } else {
        }
    }

    /**
   * TODO: zipFunc definition.
   *
   * @param filePath file to compress
   * @param zos ZipOutputSteam to write to
   * @param storePolicy file path storing policy
   * @param rootDir root directory for path references
   * @throws IOException if an error occurs
   */
    private static void zipFile(String filePath, ZipOutputStream zos, int storePolicy, String rootDir) throws IOException {
        File ffilePath = new File(filePath);
        String path = "";
        switch(storePolicy) {
            case STORE_FULL_PATH_IN_ZIP:
                path = ffilePath.getAbsolutePath();
                break;
            case STORE_NAME_ONLY_IN_ZIP:
                ffilePath.getName();
                break;
            case STORE_RELATIVE_PATH_IN_ZIP:
                File f = new File("");
                String pathToHere = f.getAbsolutePath();
                path = ffilePath.getAbsolutePath();
                path = path.substring(path.indexOf(pathToHere + File.separator) + pathToHere.length());
                break;
            case STORE_PATH_FROM_ZIP_ROOT:
                path = ffilePath.getAbsolutePath();
                String tmpDir = rootDir + File.separator;
                path = path.substring(path.indexOf(tmpDir) + tmpDir.length());
                break;
            default:
                break;
        }
        FileInputStream fileStream = new FileInputStream(filePath);
        BufferedInputStream bis = new BufferedInputStream(fileStream);
        ZipEntry fileEntry = new ZipEntry(path);
        zos.putNextEntry(fileEntry);
        byte[] data = new byte[BUFFER_SIZE];
        int byteCount;
        while ((byteCount = bis.read(data, 0, BUFFER_SIZE)) > -1) zos.write(data, 0, byteCount);
    }
}

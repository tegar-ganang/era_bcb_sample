package gnu.saw.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SAWZipUtils {

    public static boolean createZipFile(String zipFilePath, int level, final byte[] readBuffer, String... sourcePaths) throws IOException {
        if (sourcePaths == null || sourcePaths.length == 0) {
            return false;
        }
        File zipArchive = new File(zipFilePath + ".tmp");
        if (zipArchive.exists()) {
            if (!zipArchive.isFile()) {
                return false;
            }
        } else if (!zipArchive.createNewFile()) {
            if (!zipArchive.delete() || !zipArchive.createNewFile()) {
                return false;
            }
        }
        OutputStream fileStream = null;
        ZipOutputStream zipWriter = null;
        try {
            fileStream = Channels.newOutputStream(new FileOutputStream(zipArchive).getChannel());
            zipWriter = new ZipOutputStream(fileStream);
            zipWriter.setLevel(level);
            for (String contentPath : sourcePaths) {
                File nextFile = new File(contentPath).getAbsoluteFile();
                if (nextFile.exists()) {
                    if (nextFile.isFile()) {
                        if (!addFileToZip(zipWriter, zipArchive, nextFile, readBuffer, "")) {
                            return false;
                        }
                    } else if (nextFile.isDirectory()) {
                        if (!addDirectoryToZip(zipWriter, zipArchive, nextFile, readBuffer, nextFile.getName())) {
                            return false;
                        }
                    }
                }
            }
        } finally {
            boolean error = false;
            try {
                if (zipWriter != null) {
                    zipWriter.flush();
                    zipWriter.finish();
                    zipWriter.close();
                } else {
                    error = true;
                }
            } catch (IOException e) {
                error = true;
            }
            if (fileStream != null) {
                fileStream.close();
            }
            if (error) {
                return false;
            }
        }
        File trueZipArchive = new File(zipFilePath);
        if (!zipArchive.renameTo(trueZipArchive)) {
            if (trueZipArchive.delete()) {
                if (!zipArchive.renameTo(trueZipArchive)) {
                    zipArchive.delete();
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean addDirectoryToZip(ZipOutputStream zipWriter, File zipArchive, File directory, final byte[] readBuffer, String currentPath) throws IOException {
        if (!directory.canRead()) {
            return false;
        }
        ZipEntry zipDirectory = new ZipEntry(currentPath + '/');
        zipWriter.putNextEntry(zipDirectory);
        zipWriter.closeEntry();
        zipWriter.flush();
        for (File child : directory.listFiles()) {
            if (child.isFile()) {
                if (!addFileToZip(zipWriter, zipArchive, child, readBuffer, currentPath + '/')) {
                    return false;
                }
            } else if (child.isDirectory()) {
                if (!addDirectoryToZip(zipWriter, zipArchive, child, readBuffer, currentPath + '/' + child.getName())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean addFileToZip(ZipOutputStream zipWriter, File zipArchive, File file, final byte[] readBuffer, String currentPath) throws IOException {
        if (zipArchive.getAbsoluteFile().equals(file.getAbsoluteFile())) {
            return true;
        }
        InputStream fileInputStream = null;
        try {
            if (!file.canRead()) {
                return false;
            }
            fileInputStream = Channels.newInputStream(new FileInputStream(file).getChannel());
            ZipEntry fileEntry = new ZipEntry(currentPath + file.getName());
            zipWriter.putNextEntry(fileEntry);
            int readBytes;
            while ((readBytes = fileInputStream.read(readBuffer)) > 0) {
                zipWriter.write(readBuffer, 0, readBytes);
                zipWriter.flush();
            }
            zipWriter.closeEntry();
            zipWriter.flush();
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
        return true;
    }

    public static boolean extractZipFile(String zipFilePath, final byte[] readBuffer, String destinationPath) throws IOException {
        File file = new File(zipFilePath);
        if (file.exists() && !file.isFile()) {
            return false;
        }
        if (destinationPath.equalsIgnoreCase("")) {
            destinationPath = ".";
        }
        File directory = new File(destinationPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                return false;
            }
        } else if (directory.exists()) {
            if (!directory.isDirectory()) {
                return false;
            }
        }
        InputStream fileStream = null;
        ZipInputStream zipReader = null;
        try {
            fileStream = Channels.newInputStream(new FileInputStream(file).getChannel());
            zipReader = new ZipInputStream(fileStream);
            ZipEntry zipEntry;
            while ((zipEntry = zipReader.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    if (!extractDirectoryFromZip(zipEntry, destinationPath)) {
                        return false;
                    }
                } else {
                    if (!extractFileFromZip(zipReader, zipEntry, readBuffer, destinationPath)) {
                        return false;
                    }
                }
                zipReader.closeEntry();
            }
        } finally {
            if (zipReader != null) {
                zipReader.close();
            }
        }
        return true;
    }

    private static boolean extractDirectoryFromZip(ZipEntry zipEntry, String destinationPath) {
        File directory = new File(destinationPath + File.separatorChar + zipEntry.getName());
        if (directory.exists() && directory.isDirectory()) {
            return true;
        }
        return directory.mkdirs();
    }

    private static boolean extractFileFromZip(ZipInputStream zipReader, ZipEntry zipEntry, final byte[] readBuffer, String destinationPath) throws IOException {
        File file = new File(destinationPath + File.separatorChar + zipEntry.getName());
        if (file.exists()) {
            if (!file.isFile()) {
                return false;
            }
        } else if (!file.createNewFile()) {
            if (!file.delete() || !file.createNewFile()) {
                return false;
            }
        }
        OutputStream fileOutputStream = null;
        try {
            fileOutputStream = Channels.newOutputStream(new FileOutputStream(file).getChannel());
            int readBytes;
            while ((readBytes = zipReader.read(readBuffer)) > 0) {
                fileOutputStream.write(readBuffer, 0, readBytes);
                fileOutputStream.flush();
            }
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
        return true;
    }
}

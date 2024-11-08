package org.jefb.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.jefb.entity.FileEntity;
import org.jefb.service.ICommonPersistenceService;
import org.jefb.service.IFileSystemService;
import org.jefb.service.exception.FileSystemException;
import org.jefb.util.JefbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileSystemService implements IFileSystemService {

    private static Logger log = LoggerFactory.getLogger(FileSystemService.class);

    @Autowired
    private ICommonPersistenceService commonPersistenceService;

    public List<File> getFileList(File directoryToEnter, final boolean showHidden) {
        if (!directoryToEnter.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "Directory doesn't exists.");
        }
        if (!directoryToEnter.canRead()) {
            throw new FileSystemException("filemanager.msg.dir_access_denied", "Unable to enter a directory.");
        }
        if (directoryToEnter.listFiles() != null) {
            return Arrays.asList(directoryToEnter.listFiles(new FileFilter() {

                public boolean accept(File file) {
                    if ((!showHidden && file.isHidden()) || (!showHidden && file.getName().startsWith("."))) {
                        return false;
                    }
                    return true;
                }
            }));
        } else {
            throw new FileSystemException("Undable to list files.");
        }
    }

    public Integer getChildCount(String subPath, String homeDir) {
        File file = new File(homeDir + JefbUtils.FILE_SEPARATOR + subPath);
        if (file.isDirectory()) {
            return file.list().length;
        } else {
            return 0;
        }
    }

    public void deleteFile(File file, String workspace, String homeDir) {
        if (!file.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "Directory doesn't exists.");
        }
        if (!file.canRead()) {
            throw new FileSystemException("filemanager.msg.unable_to_delete", "Unable to delete a directory.");
        }
        FileEntity example = new FileEntity();
        example.setName(file.getName());
        example.setPath(JefbUtils.extractPath(file.getAbsolutePath(), file.getName(), workspace, homeDir));
        example.setWorkspace(workspace);
        List<FileEntity> foundEntities = commonPersistenceService.findAllByExample(example);
        if (foundEntities.isEmpty()) {
            deleteFile(file);
        } else {
            boolean deleted = deleteFile(file);
            if (deleted) {
                FileEntity fileEntity = foundEntities.get(0);
                commonPersistenceService.deleteEntity(fileEntity);
            } else {
                throw new FileSystemException("filemanager.msg.unable_to_delete", "Unable to delete a file or directory.");
            }
        }
    }

    private boolean deleteFile(File file) {
        if (file.isDirectory()) {
            try {
                FileUtils.cleanDirectory(file);
                return file.delete();
            } catch (IOException e) {
                throw new FileSystemException("filemanager.msg.unable_to_delete", "Unable to delete a file or directory.");
            }
        } else {
            return file.delete();
        }
    }

    public void renameFile(File origFile, String newFileName, String workspace, String homeDir) {
        if (!origFile.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "File doesn't exists.");
        }
        if (!origFile.canRead()) {
            throw new FileSystemException("filemanager.msg.unable_to_rename", "Unable to rename file.");
        }
        File fileToCheck = new File(origFile.getParent(), newFileName);
        if (fileToCheck.exists()) {
            throw new FileSystemException("filemanager.msg.file_already_exists2", "File " + fileToCheck.getAbsolutePath() + " already exists.");
        }
        String fileName = origFile.getName();
        String absolutePath = origFile.getAbsolutePath();
        String newAbsoluteFileName = absolutePath.substring(0, absolutePath.lastIndexOf(origFile.getName()));
        newAbsoluteFileName = newAbsoluteFileName + newFileName;
        File renamedFile = new File(newAbsoluteFileName);
        boolean isRenamed = origFile.renameTo(renamedFile);
        if (isRenamed && origFile.isFile()) {
            FileEntity example = new FileEntity(fileName, JefbUtils.extractPath(absolutePath, fileName, workspace, homeDir), workspace);
            List<FileEntity> foundEntities = commonPersistenceService.findAllByExample(example);
            if (!foundEntities.isEmpty()) {
                FileEntity foundEntity = foundEntities.get(0);
                foundEntity.setName(newFileName);
                commonPersistenceService.updateEntity(foundEntity);
            }
        }
    }

    public void writeFile(InputStream inputStream, String fileName) {
        try {
            File f = createNonExistentDestinationFile(new File(fileName));
            OutputStream out = new FileOutputStream(f);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (Exception e) {
            throw new FileSystemException("filemanager.msg.unable_to_write", "Unable to write file.");
        }
    }

    public void createDir(String fullDirName) {
        File newDir = new File(fullDirName);
        while (newDir.exists()) {
            String iteratedDirName = fullDirName + "_";
            newDir = new File(iteratedDirName);
        }
        boolean mkdir = newDir.mkdir();
        if (!mkdir) {
            throw new FileSystemException("filemanager.msg.unable_to_create", "Unable to create new directory:" + newDir.getName());
        }
    }

    public void setCommonPersistenceService(ICommonPersistenceService commonPersistenceService) {
        this.commonPersistenceService = commonPersistenceService;
    }

    public ICommonPersistenceService getCommonPersistenceService() {
        return commonPersistenceService;
    }

    public void copyToDirectory(File srcFile, File destDir) {
        if (!srcFile.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "Directory doesn't exists.");
        }
        if (!srcFile.canRead()) {
            throw new FileSystemException("filemanager.msg.unable_to_copy", "Unable to copy file.");
        }
        try {
            File destFile = new File(destDir.getAbsolutePath(), srcFile.getName());
            String absolutePath = JefbUtils.extractPath(srcFile);
            if (absolutePath.equals(destDir.getAbsolutePath())) {
                FileUtils.copyFile(srcFile, createNonExistentDestinationFile(destFile));
            } else {
                if (destFile.exists()) {
                    destFile.delete();
                }
                if (srcFile.isFile()) {
                    FileUtils.copyFileToDirectory(srcFile, destDir);
                } else {
                    FileUtils.copyDirectoryToDirectory(srcFile, destDir);
                }
            }
        } catch (IOException e) {
            throw new FileSystemException("filemanager.msg.unable_to_copy", "Unable to copy file. " + e.getMessage());
        }
    }

    private File createNonExistentDestinationFile(File destFile) {
        if (!destFile.exists()) {
            return destFile;
        } else {
            String extension = JefbUtils.extractExtension(destFile);
            String fileNameWithoutExtension = JefbUtils.extractFileNameWithoutExtension(destFile);
            String path = JefbUtils.extractPath(destFile);
            File newDestFile;
            do {
                newDestFile = new File(path, fileNameWithoutExtension + "_" + extension);
            } while (newDestFile.exists());
            return newDestFile;
        }
    }

    public void moveToDirectory(File srcFile, File destDirectory) {
        try {
            File destFile = new File(destDirectory.getAbsolutePath(), srcFile.getName());
            if (destFile.exists()) {
                destFile.delete();
            }
            FileUtils.moveToDirectory(srcFile, destDirectory, false);
        } catch (IOException e) {
            throw new FileSystemException("filemanager.msg.unable_to_move", "Unable to move file. " + e.getMessage());
        }
    }

    public boolean containsFile(File dir, File file) {
        if (!dir.exists()) {
            return false;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        File fileToCheck = new File(dir.getAbsoluteFile(), file.getName());
        if (fileToCheck.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDirectoryEmpty(File directory) {
        if (!directory.isDirectory()) {
            throw new FileSystemException("filemanager.msg_isnt_dir", directory.getName() + " isn't a directory.");
        }
        if (!directory.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "Directory doesn't exists.");
        }
        if (!directory.canRead()) {
            throw new FileSystemException("filemanager.msg.dir_access_denied", "Unable to enter a directory.");
        }
        return directory.list().length <= 0;
    }

    public void zipFile(File fileToZip, String zipName) {
        if (!fileToZip.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "Directory doesn't exists.");
        }
        if (!fileToZip.canRead()) {
            throw new FileSystemException("filemanager.msg.unable_to_zip", "Unable to zip file.");
        }
        String absoluteZipFileName = fileToZip.getParent() + JefbUtils.FILE_SEPARATOR + zipName;
        if (fileToZip.isDirectory()) {
            try {
                zipDir(fileToZip, absoluteZipFileName);
            } catch (IOException e) {
                throw new FileSystemException("filemanager.msg.unable_to_zip", "Unable to zip file. " + e.getMessage());
            }
        } else {
            try {
                zipFileInternal(fileToZip, absoluteZipFileName);
            } catch (IOException e) {
                throw new FileSystemException("filemanager.msg.unable_to_zip", "Unable to zip file." + e.getMessage());
            }
        }
    }

    private void zipFileInternal(File fileToZip, String zipName) throws IOException {
        byte[] buf = new byte[2048];
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipName));
        FileInputStream in = new FileInputStream(fileToZip);
        out.putNextEntry(new ZipEntry(fileToZip.getName()));
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.closeEntry();
        in.close();
        out.close();
    }

    /**
	 * Zip up a directory
	 * 
	 * @param directory
	 * @param zipName
	 * @throws IOException
	 */
    public void zipDir(File directory, String zipName) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipName));
        String path = "";
        zipDirInternal(directory, zos, path);
        zos.close();
    }

    /**
	 * Zip up a directory path
	 * 
	 * @param directory
	 * @param zos
	 * @param path
	 * @throws IOException
	 */
    private void zipDirInternal(File zipDir, ZipOutputStream zos, String path) throws IOException {
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                File filePath = f.getAbsoluteFile();
                zipDirInternal(filePath, zos, path + f.getName() + "/");
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            try {
                ZipEntry anEntry = new ZipEntry(path + f.getName());
                zos.putNextEntry(anEntry);
                bytesIn = fis.read(readBuffer);
                while (bytesIn != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                    bytesIn = fis.read(readBuffer);
                }
            } finally {
                fis.close();
            }
        }
    }

    public Long calculateSizeOfDirectory(File directory) {
        return sizeOfDirectory(directory);
    }

    public long sizeOfDirectory(File directory) {
        if (!directory.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "Directory doesn't exists.");
        }
        long size = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return 0L;
        }
        for (File file : files) {
            size += sizeOf(file);
        }
        return size;
    }

    public long sizeOf(File file) {
        if (!file.exists()) {
            return 0L;
        }
        if (file.isDirectory()) {
            return sizeOfDirectory(file);
        } else {
            return file.length();
        }
    }

    public String getFileContent(File file) {
        if (!file.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "File doesn't exists.");
        }
        if (!file.canRead()) {
            throw new FileSystemException("filemanager.msg.unable_to_read", "Unable to read file.");
        }
        StringBuilder contents = new StringBuilder();
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            throw new FileSystemException("filemanager.msg.unable_to_read2", "Unable to read file.");
        }
        return contents.toString();
    }

    public void saveFileContent(File file, String content) {
        if (file == null) {
            throw new FileSystemException("File should not be null.");
        }
        if (!file.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "File doesn't exists.");
        }
        if (!file.canRead()) {
            throw new FileSystemException("filemanager.msg.unable_to_write", "Unable to save file.");
        }
        if (!file.isFile()) {
            throw new FileSystemException("Unable to save file.", "Should not be a directory: " + file);
        }
        if (!file.canWrite()) {
            throw new FileSystemException("filemanager.msg.unable_to_write", "Unable to save file.");
        }
        try {
            Writer output = new BufferedWriter(new FileWriter(file));
            try {
                output.write(content);
            } finally {
                output.close();
            }
        } catch (Exception e) {
            throw new FileSystemException("filemanager.msg.unable_to_write", "Unable to save file.");
        }
    }

    public void createFile(String fileName) {
        File newFile = new File(fileName);
        File nonExistentFile = createNonExistentDestinationFile(newFile);
        try {
            FileUtils.touch(nonExistentFile);
        } catch (IOException e) {
            throw new FileSystemException("filemanager.msg.unable_to_create", "Unable to create a file.");
        }
    }

    private void checkFile(File file) {
        if (file == null) {
            throw new FileSystemException("File should not be null.");
        }
        if (!file.exists()) {
            throw new FileSystemException("filemanager.msg.dir_doesn_exist", "File doesn't exists.");
        }
    }

    public void unzipFile(File zippedFile) {
        checkFile(zippedFile);
        File parentDir = zippedFile.getParentFile();
        String fileNamewithoutExtenson = JefbUtils.extractFileNameWithoutExtension(zippedFile);
        File destFile = new File(parentDir.getAbsolutePath(), fileNamewithoutExtenson);
        destFile = createNonExistentDestinationFile(destFile);
        try {
            ZipFile zipFile = new ZipFile(zippedFile);
            Enumeration<? extends ZipEntry> zipEntryEnum = zipFile.entries();
            while (zipEntryEnum.hasMoreElements()) {
                ZipEntry zipEntry = zipEntryEnum.nextElement();
                extractEntry(zipFile, zipEntry, destFile.getAbsolutePath());
            }
        } catch (Exception e) {
            String errorMessage = "Unable to unzip file [" + zippedFile.getName() + "].";
            log.error(errorMessage, e);
            throw new FileSystemException("filemanager.msg.unable_to_unzip", errorMessage);
        }
    }

    private void extractEntry(ZipFile zf, ZipEntry entry, String destDir) throws IOException {
        final byte[] buffer = new byte[0xFFFF];
        File file = new File(destDir, entry.getName());
        if (entry.isDirectory()) file.mkdirs(); else {
            new File(file.getParent()).mkdirs();
            InputStream is = null;
            OutputStream os = null;
            try {
                is = zf.getInputStream(entry);
                os = new FileOutputStream(file);
                for (int len; (len = is.read(buffer)) != -1; ) os.write(buffer, 0, len);
            } finally {
                if (os != null) os.close();
                if (is != null) is.close();
            }
        }
    }
}

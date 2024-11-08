package au.edu.monash.merc.capture.file.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.List;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import au.edu.monash.merc.capture.file.FileSystemSerivce;
import au.edu.monash.merc.capture.util.io.DCFileUtils;
import au.edu.monash.merc.capture.util.stage.ScanFileFilter;

@Scope("prototype")
@Service
public class FileSystemServiceImpl implements FileSystemSerivce {

    @Override
    public boolean checkWritePermission(String pathName) {
        return DCFileUtils.checkWritePermission(pathName);
    }

    @Override
    public void createDirectory(String dirName) {
        DCFileUtils.createDirectory(dirName);
    }

    @Override
    public void deleteDirectory(String dirName) {
        DCFileUtils.deleteDirectory(dirName);
    }

    @Override
    public void changeDirectory(String olderDirName, String newDirName) {
        DCFileUtils.moveDirectory(olderDirName, newDirName);
    }

    @Override
    public void copyFile(String srcFile, String destFile) {
        DCFileUtils.copyFile(srcFile, destFile, true);
    }

    @Override
    public void moveFile(File srcFile, String destFileName, boolean override) {
        DCFileUtils.moveFile(srcFile, destFileName, override);
    }

    @Override
    public void moveFile(String srcFileName, String destFileName, boolean override) {
        DCFileUtils.moveFile(srcFileName, destFileName, override);
    }

    @Override
    public void deleteFile(String fileName) {
        DCFileUtils.deleteFile(fileName);
    }

    @Override
    public void renameFile(String olderFileName, String newFileName) {
        DCFileUtils.moveFile(olderFileName, newFileName, true);
    }

    @Override
    public byte[] readFileToByteArray(String fileName) {
        return DCFileUtils.readFileToByteArray(fileName);
    }

    @Override
    public InputStream downloadFile(String fileName) {
        return DCFileUtils.readFileToInputStream(fileName);
    }

    @Override
    public List<String> discoverFiles(String stagePath, FilenameFilter filter) {
        return DCFileUtils.discoverFileNames(stagePath, filter);
    }

    public static void main(String[] args) throws Exception {
        FileSystemServiceImpl fileService = new FileSystemServiceImpl();
        String root = "/opt/datastore";
        System.out.println("data store path permission: write? " + fileService.checkWritePermission(root));
        String stageDir = "/opt/datastore/stage";
        FileSystemServiceImpl fileDiscover = new FileSystemServiceImpl();
        ScanFileFilter filter = new ScanFileFilter();
        filter.setFileExt(".nc");
        List<String> files = fileDiscover.discoverFiles(stageDir, filter);
        for (String f : files) {
            System.out.println("========> found file: " + f);
        }
    }
}

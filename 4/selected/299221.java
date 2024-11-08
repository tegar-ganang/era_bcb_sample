package com.mobfee.business.dao.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import org.springframework.web.servlet.ModelAndView;
import com.mobfee.business.dao.IFileManager;

public class FileManager implements IFileManager {

    private String fileRootPath;

    private String imageRootPath;

    @Override
    public void save(InputStream stream, String fileName, String path, String type) throws IOException {
        String rootPath = "";
        if (type.equals("file")) rootPath = fileRootPath;
        if (type.equals("image")) rootPath = imageRootPath;
        File dir = new File(rootPath + path);
        dir.mkdirs();
        FileOutputStream fs = new FileOutputStream(rootPath + path + fileName);
        byte[] buffer = new byte[1024 * 1024];
        int bytesum = 0;
        int byteread = 0;
        while ((byteread = stream.read(buffer)) != -1) {
            bytesum += byteread;
            fs.write(buffer, 0, byteread);
            fs.flush();
        }
        fs.close();
        stream.close();
    }

    @Override
    public void deleteFile(String location) {
        File file = new File(fileRootPath + location);
        if (file.isFile() && file.exists()) {
            file.delete();
        }
    }

    @Override
    public File getFile(String filePath) {
        File file = new File(fileRootPath + filePath);
        if (file.exists() && file.canRead()) {
            return file;
        }
        return null;
    }

    public void setFileRootPath(String fileRootPath) {
        this.fileRootPath = fileRootPath;
    }

    public void setImageRootPath(String imageRootPath) {
        this.imageRootPath = imageRootPath;
    }
}

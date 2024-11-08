package com.wonebiz.crm.server.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.wonebiz.crm.server.dao.PhotoDao;

@Controller
@Scope("prototype")
public class FileUploadCtrl {

    private final Logger log = Logger.getLogger(ProtraitUploadCtrl.class);

    @Autowired
    private PhotoDao photoDao;

    @RequestMapping(value = "/data/flexOutput/fileUpload.do", method = RequestMethod.POST)
    public void fileUpload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("新 fileUpload 运行...");
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFile("file");
        if (file == null) {
            throw new Exception("上传失败：文件为空");
        }
        String fileName = file.getOriginalFilename();
        String realPath = request.getSession().getServletContext().getRealPath("/");
        String fullPath = realPath + "uploadFiles/news";
        File uploadFile = new File(fullPath);
        if (!uploadFile.exists()) {
            uploadFile.mkdirs();
        }
        String path = String.valueOf(new Date().getTime());
        String postfix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        String pathFileName = path + postfix;
        SaveFileFromInputStream(file.getInputStream(), fullPath, pathFileName);
        photoDao.saveFileName(fileName, pathFileName);
        log.info("新 fileUpload 运行结束...");
    }

    @RequestMapping(value = "/data/flexOutput/fileUpload2.do", method = RequestMethod.POST)
    public void fileUpload2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("新 fileUpload 运行...");
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFile("file");
        if (file == null) {
            throw new Exception("上传失败：文件为空");
        }
        String realPath = request.getSession().getServletContext().getRealPath("/");
        String fullPath = realPath + "images";
        File uploadFile = new File(fullPath);
        if (!uploadFile.exists()) {
            uploadFile.mkdirs();
        }
        String pathFileName = "900.gif";
        SaveFileFromInputStream(file.getInputStream(), fullPath, pathFileName);
        log.info("新 fileUpload 运行结束...");
    }

    public void SaveFileFromInputStream(InputStream stream, String path, String filename) throws IOException {
        FileOutputStream fs = new FileOutputStream(path + "/" + filename);
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
}

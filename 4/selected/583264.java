package com.ncs.mail.controller;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller("mailController")
@Scope("prototype")
public class FileUpload {

    private final Logger log = Logger.getLogger(FileUpload.class);

    @RequestMapping(value = "/flex/uploadCustomer.do", method = RequestMethod.POST)
    public void fileUpload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("FileUpload : flex/uploadCustomer.do run...");
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFile("file");
        if (file == null) {
            throw new Exception("upload failed ：there's no file(s)!");
        }
        String fileName = file.getOriginalFilename();
        String realPath = this.getClass().getClassLoader().getResource("").getPath();
        String fullPath = realPath + "uploadFiles";
        log.info("upload path : fullPath:" + fullPath);
        File uploadFile = new File(fullPath);
        if (!uploadFile.exists()) {
            uploadFile.mkdirs();
        }
        String postfix = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        String saveFileName = "uploadCustomer" + postfix;
        SaveFileFromInputStream(file.getInputStream(), fullPath, saveFileName);
        log.info("FileUpload : flex/fileUpload.do end...");
    }

    @RequestMapping(value = "/flex/downloadCustomerTemplate.do", method = RequestMethod.GET)
    public void downloadCustomerTemplate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = "customerTemplate.xls";
        String filePath = this.getClass().getClassLoader().getResource("").getPath() + "template" + File.separator + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            response.sendError(404, "File not found!");
            return;
        }
        BufferedInputStream br = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[1024];
        int len = 0;
        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "filename=" + fileName);
        response.setHeader("Cache-Control", "no-cache");
        OutputStream out = response.getOutputStream();
        while ((len = br.read(buf)) > 0) out.write(buf, 0, len);
        br.close();
        out.close();
    }

    public void SaveFileFromInputStream(InputStream stream, String path, String filename) throws IOException {
        log.info("SaveFileFromInputStream start...");
        FileOutputStream fs = new FileOutputStream(path + "/" + filename);
        byte[] buffer = new byte[1024 * 1024];
        int byteSum = 0;
        int byteRead = 0;
        while ((byteRead = stream.read(buffer)) != -1) {
            byteSum += byteRead;
            fs.write(buffer, 0, byteRead);
            fs.flush();
        }
        fs.close();
        stream.close();
        log.info("SaveFileFromInputStream end...");
    }
}

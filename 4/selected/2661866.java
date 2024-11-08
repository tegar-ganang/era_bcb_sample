package com.chenxin.authority.web.controller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.UUID;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.chenxin.authority.common.jackjson.JackJson;
import com.chenxin.authority.common.utils.FileDigest;
import com.chenxin.authority.pojo.ExtReturn;

@Controller
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @RequestMapping("/fileupload")
    public void processUpload2(@RequestParam MultipartFile file, HttpServletRequest request, HttpServletResponse response, PrintWriter writer) {
        try {
            logger.info("start");
            String fileMD5 = FileDigest.getFileMD5(file.getInputStream());
            logger.info(fileMD5);
            String savePath = request.getSession().getServletContext().getRealPath("/upload");
            String uploadFileName = file.getOriginalFilename();
            String fileType = StringUtils.substringAfterLast(uploadFileName, ".");
            logger.debug("文件的MD5：{},上传的文件名：{},文件后缀名：{},文件大小：{}", new Object[] { fileMD5, StringUtils.substringBeforeLast(uploadFileName, "."), fileType, file.getSize() });
            String dataPath = DateFormatUtils.format(new Date(), "yyyy-MM" + File.separator + "dd");
            String saveName = UUID.randomUUID().toString();
            String finalPath = File.separator + dataPath + File.separator + saveName + ("".equals(fileType) ? "" : "." + fileType);
            logger.debug("savePath:{},finalPath:{}", new Object[] { savePath, finalPath });
            File saveFile = new File(savePath + finalPath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            FileUtils.writeByteArrayToFile(saveFile, file.getBytes());
            String returnMsg = JackJson.fromObjectToJson(new ExtReturn(true, "磁盘空间已经满了！"));
            logger.debug("{}", returnMsg);
            writer.print(returnMsg);
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            writer.flush();
            writer.close();
        }
    }

    @RequestMapping("/download")
    public void download(HttpServletRequest request, HttpServletResponse response) {
        InputStream input = null;
        ServletOutputStream output = null;
        try {
            String savePath = request.getSession().getServletContext().getRealPath("/upload");
            String fileType = ".log";
            String dbFileName = "83tomcat日志测试哦";
            String downloadFileName = dbFileName + fileType;
            String finalPath = "\\2011-12\\01\\8364b45f-244d-41b6-bbf48df32064a935";
            downloadFileName = new String(downloadFileName.getBytes("GBK"), "ISO-8859-1");
            File downloadFile = new File(savePath + finalPath);
            if (!downloadFile.getParentFile().exists()) {
                downloadFile.getParentFile().mkdirs();
            }
            if (!downloadFile.isFile()) {
                FileUtils.touch(downloadFile);
            }
            response.setContentType("aapplication/vnd.ms-excel ;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-disposition", "attachment; filename=" + downloadFileName);
            input = new FileInputStream(downloadFile);
            output = response.getOutputStream();
            IOUtils.copy(input, output);
            output.flush();
        } catch (Exception e) {
            logger.error("Exception: ", e);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }
}

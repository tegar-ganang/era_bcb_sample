package com.bol.controllers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import com.bol.util.FileUploadBean;

public class FileUploadController extends SimpleFormController {

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
        FileUploadBean bean = (FileUploadBean) command;
        byte[] file = bean.getFile();
        String imagelocation = null;
        Map env = System.getProperties();
        String path = (String) env.get("jetty.home") + "/webapps";
        String host = request.getRemoteHost();
        String context = request.getContextPath();
        path = path + context;
        if (file == null) {
            if (ServletFileUpload.isMultipartContent(request)) {
                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                List items = upload.parseRequest(request);
                if (items != null) {
                    Iterator iter = items.iterator();
                    if (iter != null) {
                        while (iter.hasNext()) {
                            FileItem item = (FileItem) iter.next();
                            if (item.isFormField()) {
                            } else {
                                String fieldName = item.getFieldName();
                                String fileName = item.getName();
                                String contentType = item.getContentType();
                                long sizeInBytes = item.getSize();
                                file = item.get();
                            }
                        }
                    }
                    imagelocation = writeRawBytes(file, host, path);
                }
            }
        } else {
            imagelocation = writeRawBytes(file, host, path);
        }
        bean.setFilename(imagelocation);
        return super.onSubmit(request, response, command, errors);
    }

    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }

    private String writeRawBytes(byte[] file, String host, String path) throws IOException {
        Date now = new Date();
        long time = now.getTime();
        String filename = host.trim() + "_" + time + ".jpg";
        String imgname = path + "/images/complaintimg/" + filename;
        final FileOutputStream fos = new FileOutputStream(imgname);
        FileChannel fc = fos.getChannel();
        ByteBuffer buffer = ByteBuffer.wrap(file);
        fc.write(buffer);
        fc.close();
        return filename;
    }
}

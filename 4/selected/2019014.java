package cn.collin.commons.web.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.ServletRequestUtils;

/**
 * 附件下载servlet
 * 
 * @author collin.code@gmail.com
 * 
 */
public class DownloadServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String filePath = ServletRequestUtils.getRequiredStringParameter(request, "filepath");
        String realPath = this.getServletContext().getRealPath(filePath);
        File file = new File(realPath);
        if (file != null) {
            FileDataSource fileDataSource = new FileDataSource(file);
            response.setContentType(fileDataSource.getContentType());
            FileInputStream inputStream = (FileInputStream) fileDataSource.getInputStream();
            FileChannel channel = inputStream.getChannel();
            String mimetype = this.getServletContext().getMimeType(filePath);
            response.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
            response.setContentLength((int) file.length());
            response.addHeader("Content-Disposition", "attachment;filename=" + file.getName());
            OutputStream outputStream = response.getOutputStream();
            long bytesTransfered = channel.transferTo(0, channel.size(), Channels.newChannel(outputStream));
            response.flushBuffer();
        }
    }
}

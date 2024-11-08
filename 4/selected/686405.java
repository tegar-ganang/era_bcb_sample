package com.infineon.dns.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.infineon.dns.model.Document;
import com.infineon.dns.service.DocumentService;
import com.infineon.dns.util.DNSUtil;
import com.infineon.dns.util.Locator;

public class DownloadReviewReport extends HttpServlet {

    private static final long serialVersionUID = -420796763724267467L;

    public DownloadReviewReport() {
        super();
    }

    public void destroy() {
        super.destroy();
    }

    public void init() throws ServletException {
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            String documentId = request.getParameter("documentId");
            String directory = DNSUtil.getProperty("dns.upload.review.report.path");
            Document document = Locator.lookupService(DocumentService.class).getDocumentById(Integer.parseInt(documentId));
            String fileName = document.getReviewReportName();
            File f = new File(directory + fileName);
            BufferedInputStream br = new BufferedInputStream(new FileInputStream(f));
            byte[] buf = new byte[1024];
            int len = 0;
            response.reset();
            response.setContentType("application/x-msdownload");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            OutputStream out = response.getOutputStream();
            while ((len = br.read(buf)) > 0) out.write(buf, 0, len);
            br.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }
}

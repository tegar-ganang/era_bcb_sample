package net.sourceforge.cruisecontrol.dashboard.web.view;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class FileView extends BaseFileView {

    public static final int DEFAULT_DOWNLOAD_THRESHHOLD = 4 * 1024 * 1024;

    public String getContentType() {
        return "application/octet-stream";
    }

    private void handleFile(File file, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String filename = file.getName();
        long filesize = file.length();
        String mimeType = getMimeType(filename);
        response.setContentType(mimeType);
        if (filesize > getDownloadThreshhold()) {
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        }
        response.setContentLength((int) filesize);
        ServletOutputStream out = response.getOutputStream();
        IOUtils.copy(new FileInputStream(file), out);
        out.flush();
    }

    private int getDownloadThreshhold() {
        String threshhold = getServletContext().getInitParameter("download.threshhold");
        if (threshhold == null) {
            return DEFAULT_DOWNLOAD_THRESHHOLD;
        }
        return Integer.parseInt(threshhold);
    }

    private String getMimeType(String filename) {
        if (filename.endsWith(".svg")) {
            return "image/svg+xml";
        }
        String mimeType = this.getServletContext().getMimeType(filename);
        if (StringUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public void render(Map map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        File file = (File) map.get("targetFile");
        handleFile(file, httpServletRequest, httpServletResponse);
    }
}

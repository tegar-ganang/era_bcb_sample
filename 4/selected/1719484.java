package net.sourceforge.cruisecontrol.dashboard.web.view;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.View;

public class DownloadView implements View {

    private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;

    private String contentType = "text/xml";

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void render(Map map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
        File file = (File) map.get("targetFile");
        IOUtils.copy(new FileInputStream(file), baos);
        httpServletResponse.setContentType(getContentType());
        httpServletResponse.setContentLength(baos.size());
        httpServletResponse.addHeader("Content-disposition", "attachment; filename=" + file.getName());
        ServletOutputStream out = httpServletResponse.getOutputStream();
        baos.writeTo(out);
        out.flush();
    }
}

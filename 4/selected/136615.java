package com.nhncorp.usf.core.result.direct;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import com.nhncorp.lucy.web.helper.ServletHelper;
import com.nhncorp.usf.core.config.runtime.ResultPageInfo;

/**
 * @author Web Platform Development Team
 */
public class DownloadResult extends AbstractDirectResult {

    private static final long serialVersionUID = 1252921644258727555L;

    /**
     * 전달된 {@link DataMap} 정보에 정의된 파일을 download함.
     * 
     * @param resultPageInfo the page information
     * @param dataMap the usf result data
     * @throws Exception the Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public void execute(ResultPageInfo resultPageInfo, Map<String, Object> dataMap) throws Exception {
        HttpServletResponse response = ServletHelper.getResponse();
        Properties prop = resultPageInfo.getProperties();
        Map<String, Object> downloadInfo = (Map<String, Object>) dataMap.get(prop.get("src"));
        if (downloadInfo != null) {
            String serverFileName = (String) downloadInfo.get(prop.get("serverFileName"));
            String downloadFileName = (String) downloadInfo.get(prop.get("downloadFileName"));
            File file = new File(serverFileName);
            ServletContext servletContext = ServletHelper.getServletContext();
            String mimetype = null;
            if (servletContext != null) {
                mimetype = servletContext.getMimeType(serverFileName);
            }
            response.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadFileName + "\"");
            int length = 0;
            byte[] bbuf = new byte[1024];
            FileInputStream fin = null;
            DataInputStream in = null;
            ServletOutputStream op = null;
            try {
                fin = new FileInputStream(file);
                in = new DataInputStream(fin);
                op = response.getOutputStream();
                while ((in != null) && ((length = in.read(bbuf)) != -1)) {
                    op.write(bbuf, 0, length);
                }
                op.flush();
            } catch (FileNotFoundException e) {
                log.debug("fail to find the soruce file : " + serverFileName);
            } catch (IOException e) {
                log.debug("fail to read/write IO stream : " + e.getMessage());
            } finally {
                try {
                    if (fin != null) {
                        fin.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (op != null) {
                        op.close();
                    }
                } catch (IOException e) {
                    log.debug("fail to close Input/Output Stream : " + serverFileName);
                }
            }
        }
    }
}

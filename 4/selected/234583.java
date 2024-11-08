package br.com.tablechart.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ResourceLoaderAction implements IAction {

    public void execute(HttpServletRequest pReq, HttpServletResponse pResp) throws IOException {
        String resourceName = RESOURCES_PKG + pReq.getParameter(PARAM_NAME);
        log.debug("Carregando recurso: " + resourceName);
        setContentType(pReq, pResp);
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        pResp.getOutputStream().write(readBytes(resource));
    }

    private byte[] readBytes(InputStream pStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = pStream.read(buffer)) >= 0) {
            bos.write(buffer, 0, len);
        }
        pStream.close();
        bos.close();
        return bos.toByteArray();
    }

    private void setContentType(HttpServletRequest pReq, HttpServletResponse pResp) {
        String resourceType = pReq.getParameter(PARAM_TYPE);
        String contentType = "text/html";
        if (resourceType.equals(IMAGE)) {
            contentType = "image/jpg";
        }
        if (resourceType.equals(CSS)) {
            contentType = "text/css";
        }
        pResp.setContentType(contentType);
    }

    private final Log log = LogFactory.getLog(RequestUtil.class);

    private static final String IMAGE = "image";

    private static final String CSS = "css";

    private static final String PARAM_NAME = "name";

    private static final String PARAM_TYPE = "type";

    private static final String RESOURCES_PKG = "br/com/tablechart/web/resources/";
}

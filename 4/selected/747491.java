package org.cmsuite2.web.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.cmsuite2.enumeration.MediaRequestType;
import org.cmsuite2.enumeration.MediaType;
import org.cmsuite2.util.media.IMediaReader;
import org.cmsuite2.util.media.MediaBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class MediaStreamer extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static Logger logger = Logger.getLogger(MediaStreamer.class);

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        IMediaReader categoryReader = (IMediaReader) springContext.getBean("categoryReader");
        IMediaReader productReader = (IMediaReader) springContext.getBean("productReader");
        String idStr = request.getParameter("id");
        if (StringUtils.isEmpty(idStr) || !StringUtils.isNumeric(idStr)) {
            sendCode(response, 401);
            return;
        }
        long id = Long.parseLong(idStr);
        String type = request.getParameter("type");
        if (StringUtils.isEmpty(type)) {
            sendCode(response, 401);
            return;
        }
        MediaRequestType mediaRequestType = MediaRequestType.valueOf(type);
        String forceStr = request.getParameter("force");
        boolean force = false;
        if (StringUtils.isNotEmpty(forceStr)) force = Boolean.parseBoolean(forceStr);
        Locale currentLocale = request.getLocale();
        String ifModifiedSince = request.getHeader("If-Modified-Since");
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", currentLocale);
        MediaBean mediaBean = null;
        if (MediaRequestType.PREVIEW_Category.equals(mediaRequestType) || MediaRequestType.BANNER_Category.equals(mediaRequestType)) mediaBean = categoryReader.getImage(mediaRequestType, id, force, sdf, currentLocale, ifModifiedSince);
        if (MediaRequestType.PREVIEW_Product.equals(mediaRequestType) || MediaRequestType.BANNER_Product.equals(mediaRequestType)) mediaBean = productReader.getImage(mediaRequestType, id, force, sdf, currentLocale, ifModifiedSince);
        if (mediaBean == null) {
            sendCode(response, 404);
            return;
        }
        if (mediaBean.hasCodes()) {
            sendCode(response, mediaBean.getCodes().get(0).getCode());
            return;
        }
        if (MediaType.video.equals(mediaBean.getMediaType())) sendRedirect(response, mediaBean); else sendData(response, mediaBean);
    }

    private void sendCode(HttpServletResponse response, int code) {
        if (logger.isInfoEnabled()) logger.info("sendCode[" + code + "]");
        response.setStatus(code);
        response.setContentLength(0);
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
    }

    private void sendData(HttpServletResponse response, MediaBean mediaBean) throws IOException {
        if (logger.isInfoEnabled()) logger.info("sendData[" + mediaBean + "]");
        response.setContentLength(mediaBean.getContentLength());
        response.setContentType(mediaBean.getContentType());
        response.addHeader("Last-Modified", mediaBean.getLastMod());
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("content-disposition", "attachment, filename=" + (new Date()).getTime() + "_" + mediaBean.getFileName());
        byte[] content = mediaBean.getContent();
        InputStream is = null;
        OutputStream os = null;
        try {
            os = response.getOutputStream();
            is = new ByteArrayInputStream(content);
            IOUtils.copy(is, os);
        } catch (IOException e) {
            logger.error(e, e);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
            }
            if (os != null) try {
                os.close();
            } catch (IOException e) {
            }
        }
    }

    private void sendRedirect(HttpServletResponse response, MediaBean mb) throws IOException {
        if (logger.isInfoEnabled()) logger.info("sendData[]");
        response.sendRedirect(mb.getFilePrefix() + mb.getFileName());
    }
}

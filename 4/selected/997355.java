package com.jujunie.service.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Element;
import com.jujunie.service.xml.InvalidXMLElementException;

public class OutputStreamDisplayHandlerXML extends WebElementXML implements DisplayHandlerXML, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 7523347691877891608L;

    public static final String INPUTSTREAM_KEY = "com.jujunie.service.web.WriterDisplayHandlerXML#INPUTSTREAM_KEY";

    /** Code */
    private String code = "";

    public void init(Element displayHandlerNode) throws WebConfigException, InvalidXMLElementException {
        this.code = super.getAttribute(displayHandlerNode, ATT_CODE, true);
    }

    public void display(WebPage page, HttpServletRequest req, HttpServletResponse resp) throws DisplayException {
        page.getDisplayInitialiser().initDisplay(new HttpRequestDisplayContext(req), req);
        StreamProvider is = (StreamProvider) req.getAttribute(INPUTSTREAM_KEY);
        if (is == null) {
            throw new IllegalStateException("No OutputStreamDisplayHandlerXML.InputStream found in request attribute" + " OutputStreamDisplayHandlerXML.INPUTSTREAM_KEY");
        }
        resp.setContentType(is.getMimeType());
        resp.setHeader("Content-Disposition", "attachment;filename=" + is.getName());
        try {
            InputStream in = is.getInputStream();
            OutputStream out = resp.getOutputStream();
            if (in != null) {
                IOUtils.copy(in, out);
            }
            is.write(resp.getOutputStream());
            resp.flushBuffer();
        } catch (IOException e) {
            throw new DisplayException("Error writing input stream to response", e);
        }
    }

    public String getCode() {
        return this.code;
    }

    public static interface StreamProvider {

        InputStream getInputStream();

        void write(OutputStream out) throws IOException;

        String getMimeType();

        String getName();
    }

    public static class DefaultStreamProvider implements StreamProvider {

        private InputStream inputStream = null;

        private String mimeType = "application/octet-stream";

        private String name = "noname";

        public InputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(java.io.InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void write(OutputStream out) {
        }
    }
}

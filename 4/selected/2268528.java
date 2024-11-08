package com.genia.toolbox.projects.sample_ws_trampo.war.ws.interceptor;

import java.io.IOException;
import java.io.InputStream;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.log4j.Logger;

public class ReadInInterceptor {

    private static final Logger logger = Logger.getLogger(ReadInInterceptor.class);

    private final Message message;

    private final int limit;

    public ReadInInterceptor(Message message, int limit) {
        this.limit = limit;
        this.message = message;
    }

    public void logging() throws Fault {
        final InterceptorWrapper wrap = new InterceptorWrapper(message);
        final LoggingMessage buffer = new LoggingMessage("Inbound Message\n----------------------------");
        String encoding = (String) wrap.getEncoding();
        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        Object headers = wrap.getProtocolHeaders();
        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        InputStream is = (InputStream) wrap.getContent(InputStream.class);
        if (is != null) {
            CachedOutputStream bos = new CachedOutputStream();
            try {
                IOUtils.copy(is, bos);
                bos.flush();
                is.close();
                this.message.setContent(InputStream.class, bos.getInputStream());
                if (bos.getTempFile() != null) {
                    logger.error("\nMessage (saved to tmp file):\n");
                    logger.error("Filename: " + bos.getTempFile().getAbsolutePath() + "\n");
                }
                if (bos.size() > limit) {
                    logger.error("(message truncated to " + limit + " bytes)\n");
                }
                bos.writeCacheTo(buffer.getPayload(), limit);
                bos.close();
            } catch (IOException e) {
                throw new Fault(e);
            }
        }
        logger.debug("Message received :\n" + buffer.getPayload().toString());
    }
}

package de.objectcode.soa.common.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.soa.esb.actions.ActionProcessingException;

public class LimitedStreamReader implements Runnable {

    private static final Log LOG = LogFactory.getLog(LimitedStreamReader.class);

    final InputStream inputStream;

    final long limit;

    byte[] result;

    boolean readed = false;

    public LimitedStreamReader(InputStream inputStream, long limit) {
        this.inputStream = inputStream;
        this.limit = limit;
    }

    public byte[] getResult() {
        return result;
    }

    public void run() {
        try {
            perform();
        } catch (Exception e) {
            LOG.error("Exception", e);
        }
    }

    public void perform() throws ActionProcessingException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int readed;
            long count = 0;
            while ((readed = inputStream.read(buffer)) > 0) {
                bos.write(buffer, 0, readed);
                count += readed;
                if (limit > 0 && count > limit) {
                    throw new ActionProcessingException("Exceeded read limit: " + limit);
                }
            }
            inputStream.close();
            bos.close();
            result = bos.toByteArray();
        } catch (IOException e) {
            throw new ActionProcessingException(e);
        }
    }
}

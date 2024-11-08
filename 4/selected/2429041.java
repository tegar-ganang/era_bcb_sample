package org.apache.james.mime4j.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.util.TempPath;
import org.apache.james.mime4j.util.TempStorage;

/**
 * Binary body backed by a {@link org.apache.james.mime4j.util.TempFile}.
 *
 * 
 * @version $Id: TempFileBinaryBody.java,v 1.2 2004/10/02 12:41:11 ntherning Exp $
 */
class MemoryBinaryBody extends AbstractBody implements BinaryBody {

    private static Log log = LogFactory.getLog(MemoryBinaryBody.class);

    private Entity parent = null;

    private byte[] tempFile = null;

    /**
     * Use the given InputStream to build the TemporyFileBinaryBody
     * 
     * @param is the InputStream to use as source
     * @throws IOException
     */
    public MemoryBinaryBody(InputStream is) throws IOException {
        TempPath tempPath = TempStorage.getInstance().getRootTempPath();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(is, out);
        out.close();
        tempFile = out.toByteArray();
    }

    /**
     * @see org.apache.james.mime4j.message.AbstractBody#getParent()
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * @see org.apache.james.mime4j.message.AbstractBody#setParent(org.apache.james.mime4j.message.Entity)
     */
    public void setParent(Entity parent) {
        this.parent = parent;
    }

    /**
     * @see org.apache.james.mime4j.message.BinaryBody#getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(tempFile);
    }

    /**
     * @see org.apache.james.mime4j.message.Body#writeTo(java.io.OutputStream)
     */
    public void writeTo(OutputStream out) throws IOException {
        IOUtils.copy(getInputStream(), out);
    }
}

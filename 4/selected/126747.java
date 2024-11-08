package org.wings;

import org.wings.externalizer.ExternalizeManager;
import org.wings.io.Device;
import org.wings.session.PropertyService;
import org.wings.session.SessionManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * TODO: documentation
 *
 * @author <a href="mailto:haaf@mercatis.de">Armin Haaf</a>
 * @author <a href="mailto:H.Zeller@acm.org">Henner Zeller</a>
 * @version $Revision: 1759 $
 */
public abstract class StaticResource extends Resource {

    private static final Log logger = LogFactory.getLog("org.wings");

    /**
     * Flags that influence the behaviour of the externalize manager
     */
    protected int externalizerFlags = ExternalizeManager.FINAL;

    /**
     * A buffer for temporal storage of the resource
     */
    protected transient LimitedBuffer buffer;

    /**
     * The size of this resource. Initially, this will be '-1', but
     * the value is updated, once the Resource is delivered.
     */
    protected int size = -1;

    /**
     * An ByteArrayOutputStream that buffers up to the limit
     * MAX_SIZE_TO_BUFFER. Is able to write to an Device.
     */
    protected static final class LimitedBuffer extends ByteArrayOutputStream {

        public static final int MAX_SIZE_TO_BUFFER = 8 * 1024;

        private boolean withinLimit;

        private int maxSizeToBuffer = MAX_SIZE_TO_BUFFER;

        /**
         * creates a new buffer
         */
        LimitedBuffer() {
            super(64);
            withinLimit = true;
            initMaxSizeToBuffer();
        }

        private void initMaxSizeToBuffer() {
            if (SessionManager.getSession() == null) return;
            Object prop = SessionManager.getSession().getProperty("Resource.MaxSizeToBuffer");
            if (prop != null && prop instanceof Number) {
                maxSizeToBuffer = ((Number) prop).intValue();
            }
        }

        /**
         * write to the stream. If the output size exceeds the limit,
         * then set the stream to error state.
         */
        public void write(byte[] b, int off, int len) {
            if (!withinLimit) return;
            withinLimit = (count + len < maxSizeToBuffer);
            if (withinLimit) super.write(b, off, len); else reset();
        }

        /**
         * returns, whether the filled buffer is within the limits,
         * and thus, its content is valid and can be used.
         */
        public boolean isValid() {
            return withinLimit;
        }

        /**
         * sets, whether this resource is valid.
         */
        public void setValid(boolean valid) {
            withinLimit = valid;
        }

        /**
         * returns the _raw_ buffer; i.e. the buffer may be larger than
         * the current size().
         */
        public byte[] getBytes() {
            return buf;
        }

        /**
         * write to some output device.
         */
        public void writeTo(Device out) throws IOException {
            out.write(buf, 0, size());
        }
    }

    /**
     * A static resource that is obtained from the specified class loader
     */
    protected StaticResource(String extension, String mimeType) {
        super(extension, mimeType);
    }

    /**
     * Get the id that identifies this resource as an externalized object.
     * If the object has not been externalized yet, it will be externalized.
     *
     * @return the externalization id
     */
    public String getId() {
        if (id == null) {
            ExternalizeManager ext = SessionManager.getSession().getExternalizeManager();
            id = ext.getId(ext.externalize(this, externalizerFlags));
            logger.debug("new " + getClass().getName() + " with id " + id);
        }
        return id;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Reads the resource into an LimitedBuffer and returns it. If the
     * size of the resource is larger than
     * {@link LimitedBuffer#MAX_SIZE_TO_BUFFER}, then the returned Buffer
     * is empty and does not contain the Resource's content (and the
     * isValid() flag is false).
     *
     * @return buffered resource as LimitedBuffer, that may be invalid,
     *         if the size of the resource is beyond MAX_SIZE_TO_BUFFER. It is
     *         null, if the Resource returned an invalid stream.
     */
    protected LimitedBuffer bufferResource() throws IOException {
        if (buffer == null) {
            buffer = new LimitedBuffer();
            InputStream resource = getResourceStream();
            if (resource != null) {
                byte[] copyBuffer = new byte[1024];
                int read;
                while (buffer.isValid() && (read = resource.read(copyBuffer)) > 0) {
                    buffer.write(copyBuffer, 0, read);
                }
                resource.close();
                if (buffer.isValid()) {
                    size = buffer.size();
                }
            } else {
                logger.fatal("Resource returned empty stream: " + this);
                buffer.setValid(false);
            }
        }
        return buffer;
    }

    /**
     * writes the Resource to the given Stream. If the resource
     * is not larger than {@link LimitedBuffer#MAX_SIZE_TO_BUFFER}, then
     * an internal buffer caches the content the first time, so that it
     * is delivered as fast as possible at any subsequent calls.
     *
     * @param out the sink, the content of the resource should
     *            be written to.
     */
    public final void write(Device out) throws IOException {
        if (buffer == null) {
            bufferResource();
            if (buffer == null) return;
        }
        if (buffer.isValid()) {
            buffer.writeTo(out);
        } else {
            InputStream resource = getResourceStream();
            if (resource != null) {
                int deliverSize = 0;
                byte[] copyBuffer = new byte[1024];
                int read;
                while ((read = resource.read(copyBuffer)) > 0) {
                    out.write(copyBuffer, 0, read);
                    deliverSize += read;
                }
                resource.close();
                size = deliverSize;
            }
        }
        out.flush();
    }

    /**
     * Return the size in bytes of the resource, if known
     *
     * @return
     */
    public final int getLength() {
        return size;
    }

    public SimpleURL getURL() {
        String name = getId();
        if ((externalizerFlags & ExternalizeManager.GLOBAL) > 0) {
            return new SimpleURL(name);
        } else {
            RequestURL requestURL = (RequestURL) getPropertyService().getProperty("request.url");
            requestURL = (RequestURL) requestURL.clone();
            requestURL.setResource(name);
            return requestURL;
        }
    }

    private PropertyService propertyService;

    protected PropertyService getPropertyService() {
        if (propertyService == null) propertyService = (PropertyService) SessionManager.getSession();
        return propertyService;
    }

    /**
     * TODO: documentation
     *
     * @return
     */
    public String toString() {
        return getId();
    }

    /**
     * set the externalizer flags as defined in
     * {@link org.wings.externalizer.AbstractExternalizeManager}.
     */
    public void setExternalizerFlags(int flags) {
        externalizerFlags = flags;
    }

    public int getExternalizerFlags() {
        return externalizerFlags;
    }

    protected static String resolveName(Class baseClass, String fileName) {
        if (fileName == null) {
            return fileName;
        }
        if (!fileName.startsWith("/")) {
            while (baseClass.isArray()) {
                baseClass = baseClass.getComponentType();
            }
            String baseName = baseClass.getName();
            int index = baseName.lastIndexOf('.');
            if (index != -1) {
                fileName = baseName.substring(0, index).replace('.', '/') + "/" + fileName;
            }
        } else {
            fileName = fileName.substring(1);
        }
        return fileName;
    }

    protected abstract InputStream getResourceStream() throws IOException;
}

package org.fxplayer.rest.providers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fxplayer.rest.representations.TrackRepresentation;

/**
 * The Class TrackProvider.
 */
@Provider
public class TrackProvider implements MessageBodyWriter<TrackRepresentation> {

    /** The Constant LOG. */
    public static final Log LOG = LogFactory.getLog(TrackProvider.class);

    @Override
    public long getSize(final TrackRepresentation t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        if (mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE)) if (!t.isTranscode(mediaType)) return new File(t.getPath()).length(); else return t.getLength() * t.getUser().getUserConfiguration().getMaxBitRate() / 8;
        return -1;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE) && type == TrackRepresentation.class;
    }

    @Override
    public void writeTo(final TrackRepresentation t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws WebApplicationException {
        if (mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE)) {
            InputStream is = null;
            try {
                httpHeaders.add("Content-Type", "audio/mp3");
                IOUtils.copy(is = t.getInputStream(mediaType), entityStream);
            } catch (final IOException e) {
                LOG.warn("IOException : maybe remote client has disconnected");
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }
}

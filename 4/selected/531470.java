package org.yass.rest.providers;

import java.io.File;
import java.io.FileInputStream;
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
import org.yass.domain.Track;

/**
 * @author Sven Duzont
 * 
 */
@Provider
public class TrackProvider implements MessageBodyWriter<Track> {

    @Override
    public long getSize(final Track t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        if (mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE)) return new File(t.getPath()).length();
        return -1;
    }

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType) {
        return type == Track.class;
    }

    @Override
    public void writeTo(final Track t, final Class<?> type, final Type genericType, final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
        if (mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE)) {
            final InputStream in = new FileInputStream(new File(t.getPath()));
            int c;
            while ((c = in.read()) != -1) entityStream.write(c);
        }
    }
}

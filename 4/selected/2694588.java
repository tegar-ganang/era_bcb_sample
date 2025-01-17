package org.restlet.ext.jaxrs.internal.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.engine.io.BioUtils;
import org.restlet.ext.jaxrs.internal.util.Util;
import org.restlet.representation.Representation;

/**
 * This ProviderWrapper is used to read directly from a {@link Reader}.
 * 
 * @author Stephan Koops
 * @see BufferedReaderProvider
 */
@Provider
public class ReaderProvider extends AbstractProvider<Reader> {

    /**
     * Returns a Reader wrapping the given entity stream, with respect to the
     * {@link CharacterSet} of the entity of the current {@link Request}, or
     * UTF-8 if no character set was given or if it is not available
     */
    static Reader getReader(InputStream entityStream) {
        final Representation entity = Request.getCurrent().getEntity();
        CharacterSet cs;
        if (entity != null) {
            cs = entity.getCharacterSet();
            if (cs == null) {
                cs = Util.JAX_RS_DEFAULT_CHARACTER_SET;
            }
        } else {
            cs = Util.JAX_RS_DEFAULT_CHARACTER_SET;
        }
        try {
            return BioUtils.getReader(entityStream, cs);
        } catch (UnsupportedEncodingException e) {
            try {
                return BioUtils.getReader(entityStream, null);
            } catch (UnsupportedEncodingException e2) {
                throw new WebApplicationException(500);
            }
        }
    }

    /**
     * @see javax.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object)
     */
    @Override
    public long getSize(Reader t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    /**
     * @see MessageBodyReader#readFrom(Class, Type, MediaType, Annotation[],
     *      MultivaluedMap, InputStream)
     */
    @Override
    public Reader readFrom(Class<Reader> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        return getReader(entityStream);
    }

    @Override
    protected Class<?> supportedClass() {
        return Reader.class;
    }

    /**
     * @see MessageBodyWriter#writeTo(Object, Type, Annotation[], MediaType,
     *      MultivaluedMap, OutputStream)
     */
    @Override
    public void writeTo(Reader reader, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        final CharacterSet cs = Response.getCurrent().getEntity().getCharacterSet();
        Util.copyStream(BioUtils.getStream(reader, cs), entityStream);
    }
}

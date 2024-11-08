package net.fortytwo.linkeddata.dereferencers;

import net.fortytwo.linkeddata.Dereferencer;
import net.fortytwo.ripple.RippleException;
import net.fortytwo.ripple.util.RDFUtils;
import org.openrdf.rio.RDFFormat;
import org.restlet.resource.Representation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class JarURIDereferencer implements Dereferencer {

    public Representation dereference(final String uri) throws RippleException {
        return new JarRepresentation(uri);
    }

    private class JarRepresentation extends Representation {

        private InputStream inputStream;

        public JarRepresentation(final String uri) throws RippleException {
            RDFFormat format = RDFUtils.guessRdfFormat(uri, null);
            setMediaType(RDFUtils.findMediaType(format));
            JarURLConnection jc;
            try {
                jc = (JarURLConnection) (new URL(uri).openConnection());
                inputStream = jc.getInputStream();
            } catch (IOException e) {
                throw new RippleException(e);
            }
        }

        public ReadableByteChannel getChannel() throws IOException {
            return null;
        }

        public InputStream getStream() throws IOException {
            return inputStream;
        }

        public void write(OutputStream outputStream) throws IOException {
        }

        public void write(WritableByteChannel writableByteChannel) throws IOException {
        }
    }
}

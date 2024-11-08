package uk.ac.ebi.intact.psicquic.ws.util;

import org.apache.commons.io.IOUtils;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class CompressedStreamingOutput implements StreamingOutput {

    private InputStream is;

    public CompressedStreamingOutput(InputStream is) {
        this.is = is;
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOs = new GZIPOutputStream(baos);
        IOUtils.copy(is, gzipOs);
        baos.close();
        gzipOs.close();
        output.write(baos.toByteArray());
    }
}

package javacream.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ByteArrayResourceHandler
 * 
 * @author Glenn Powell
 *
 */
public class ByteArrayResourceHandler implements ResourceHandler<byte[]> {

    public byte[] read(InputStream input) throws ResourceException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            while (input.available() > 0) bytes.write(input.read());
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    public void write(byte[] resource, OutputStream output) throws ResourceException {
        try {
            output.write(resource);
        } catch (ClassCastException e) {
            throw new ResourceException(e);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }
}

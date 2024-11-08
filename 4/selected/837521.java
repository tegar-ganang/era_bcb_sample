package blueprint4j.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamableInNone implements StreamableIn {

    private InputStream instream = null;

    private StreamableIn streamin = null;

    /**
	 * Filter to look for a string as the end of stream marker
	 */
    public StreamableInNone() {
    }

    public StreamableInNone(InputStream _instream) throws IOException {
        setInStream(_instream);
    }

    public StreamableInNone(StreamableIn _streamin) throws IOException {
        setInStream(_streamin);
    }

    public void setInStream(StreamableIn _streamin) throws IOException {
        streamin = _streamin;
    }

    public void setInStream(InputStream _instream) throws IOException {
        if (streamin == null) instream = _instream; else streamin.setInStream(_instream);
    }

    public byte[] readBuffer() throws IOException {
        if (instream != null) {
            if (instream.available() == 0) return null;
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            for (; instream.available() > 0; stream.write(instream.read())) ;
            return stream.toByteArray();
        } else return streamin.readBuffer();
    }

    public void refresh() {
    }

    public void purge() {
    }

    public StreamableIn getNewInstance() {
        return new StreamableInNone();
    }
}

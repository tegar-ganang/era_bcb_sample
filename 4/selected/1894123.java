package jlibs.xml.sax.async;

import org.xml.sax.InputSource;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Santhosh Kumar T
 */
public class ChannelInputSource extends InputSource {

    public ChannelInputSource() {
    }

    public ChannelInputSource(String systemId) {
        super(systemId);
    }

    public ChannelInputSource(InputStream byteStream) {
        super(byteStream);
    }

    public ChannelInputSource(Reader characterStream) {
        super(characterStream);
    }

    public ChannelInputSource(ReadableByteChannel channel) {
        setChannel(channel);
    }

    private ReadableByteChannel channel;

    public ReadableByteChannel getChannel() {
        return channel;
    }

    public void setChannel(ReadableByteChannel channel) {
        this.channel = channel;
    }
}

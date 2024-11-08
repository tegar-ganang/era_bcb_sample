package org.apache.http.nio.entity;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;

/**
 * An entity whose content is retrieved from a byte array. In addition to the 
 * standard {@link HttpEntity} interface this class also implements NIO specific 
 * {@link HttpNIOEntity}.
 *
 * @deprecated Use {@link NByteArrayEntity}
 * 
 * @version $Revision: 744570 $
 * 
 * @since 4.0
 */
@Deprecated
public class ByteArrayNIOEntity extends ByteArrayEntity implements HttpNIOEntity {

    public ByteArrayNIOEntity(final byte[] b) {
        super(b);
    }

    public ReadableByteChannel getChannel() throws IOException {
        return Channels.newChannel(getContent());
    }
}

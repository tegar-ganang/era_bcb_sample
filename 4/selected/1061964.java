package org.apache.http.nio.entity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

/**
 * An entity whose content is retrieved from a string. In addition to the 
 * standard {@link HttpEntity} interface this class also implements NIO specific 
 * {@link HttpNIOEntity}.
 *
 * @deprecated Use {@link NStringEntity}
 *
 * @version $Revision: 744570 $
 * 
 * @since 4.0
 */
@Deprecated
public class StringNIOEntity extends StringEntity implements HttpNIOEntity {

    public StringNIOEntity(final String s, String charset) throws UnsupportedEncodingException {
        super(s, charset);
    }

    public ReadableByteChannel getChannel() throws IOException {
        return Channels.newChannel(getContent());
    }
}

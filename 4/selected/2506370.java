package net.sf.viwow.nio.net.tcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import net.sf.viwow.seda.Event;
import net.sf.viwow.seda.EventHandler;

public class ReadEventHandler extends EventHandler {

    public void handleEvent(Event event) {
        if (event instanceof SocketEvent) {
            handleSelectionKeyEvent((SocketEvent) event);
        } else {
            throw new IllegalArgumentException("The event should be the type of " + SocketEvent.class.getName());
        }
    }

    protected void handleSelectionKeyEvent(SocketEvent event) {
        SocketChannel channel = (SocketChannel) event.getSocket().getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            channel.read(buffer);
            buffer.flip();
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buffer);
            System.out.print(charBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

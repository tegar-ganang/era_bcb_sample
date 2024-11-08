package galao.net.io;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.bushe.swing.event.EventBus;
import galao.core.logging.Logging;
import galao.core.properties.Registry;
import galao.event.MessageTransferedEvent;
import galao.event.io.ByteTransferredEvent;
import galao.net.io.api.Input;
import galao.net.io.api.MessageOutput;
import galao.net.io.api.Output;
import galao.net.message.Message;

public class SocketInputChannel extends Input {

    private Socket in;

    public SocketInputChannel(Socket in) {
        super(null);
        this.in = in;
    }

    public SocketInputChannel(Input i) {
        super(i);
    }

    @Override
    public byte[] read() throws IOException {
        SocketChannel sc = this.in.getChannel();
        byte[] incoming = {};
        ByteBuffer tmp = ByteBuffer.wrap(new byte[1024]);
        tmp.rewind();
        int l = 0;
        while ((l = sc.read(tmp)) >= 0) {
            tmp.flip();
            incoming = this.concatArray(incoming, tmp.array(), l);
            tmp.clear();
        }
        sc.close();
        this.in.close();
        new ByteTransferredEvent(incoming.length, true).publish();
        return incoming;
    }

    byte[] concatArray(byte[] a, byte[] b, int l) {
        byte[] c = new byte[a.length + l];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, l);
        return c;
    }
}

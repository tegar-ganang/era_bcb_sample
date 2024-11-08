package sippoint.framework.module.transport.sandbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.LinkedList;

/**
 * @author Martin Hynar
 * 
 */
public class NIOConnection implements Connection {

    private SelectionKey sk;

    private ConnectionUser cu;

    private int state;

    private LinkedList sendQ = new LinkedList();

    private CharsetEncoder encoder;

    private CharsetDecoder decoder;

    private ByteBuffer recvBuffer = null;

    private ByteBuffer sendBuffer = null;

    private StringBuffer recvString = new StringBuffer();

    private String crlf = "\r\n";

    private boolean writeReady = false;

    private String name = "";

    /**
	 * construct a NIOConnection from a selection key
	 */
    NIOConnection(SelectionKey sk) {
        state = Connection.OPENED;
        this.sk = sk;
        sk.attach(this);
        Charset charset = Charset.forName("ISO-8859-1");
        decoder = charset.newDecoder();
        encoder = charset.newEncoder();
        recvBuffer = ByteBuffer.allocate(8196);
    }

    /**
	 * attach a connection user to this connection
	 */
    public void attach(ConnectionUser cu) {
        this.cu = cu;
    }

    /**
	 * process a read ready selection
	 */
    public void doRead() {
        SocketChannel sc = (SocketChannel) sk.channel();
        if (sc.isOpen()) {
            int len;
            recvBuffer.clear();
            try {
                len = sc.read(recvBuffer);
            } catch (IOException e) {
                e.printStackTrace();
                len = -1;
            }
            Functions.dout(1, "read len=" + len);
            if (len > 0) {
                recvBuffer.flip();
                CharBuffer buf = null;
                try {
                    buf = decoder.decode(recvBuffer);
                } catch (Exception ce) {
                    ce.printStackTrace();
                    len = -1;
                }
                toUser(buf);
            }
            if (len < 0) close();
        } else System.out.println("read closed");
    }

    private void toUser(CharBuffer buf) {
        if (buf != null) {
            int i = 0;
            int j = 0;
            recvString.append(buf);
            int z = recvString.length();
            while (i < z) {
                char c = recvString.charAt(i);
                if (c == '\r' || c == '\n') {
                    i++;
                    if (c == '\r' && i < z && '\n' == recvString.charAt(i)) i++;
                    cu.receive(recvString.substring(j, i));
                    j = i + 1;
                } else i++;
            }
            if (j < z) recvString.delete(0, j); else recvString = new StringBuffer();
        }
    }

    /**
	 * process a write ready selection
	 */
    public void doWrite() {
        Functions.dout(12, "write ready");
        sk.interestOps(SelectionKey.OP_READ);
        writeReady = true;
        if (sendBuffer != null) write(sendBuffer);
        writeQueued();
    }

    /**
	 * queue up a text string to send and try to send it out
	 */
    public void send(String text) {
        sendQ.add(text);
        writeQueued();
    }

    private void writeQueued() {
        while (writeReady && sendQ.size() > 0) {
            String msg = (String) sendQ.remove(0);
            write(msg);
        }
    }

    private void write(String text) {
        try {
            ByteBuffer buf = encoder.encode(CharBuffer.wrap(text));
            write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write(ByteBuffer data) {
        SocketChannel sc = (SocketChannel) sk.channel();
        if (sc.isOpen()) {
            int len = 0;
            if (data.hasRemaining()) {
                try {
                    len = sc.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    close();
                }
            }
            if (data.hasRemaining()) {
                Functions.dout(12, "write blocked");
                writeReady = false;
                sk.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                sendBuffer = data;
            } else sendBuffer = null;
        } else Functions.dout(12, "write closed");
    }

    public void close() {
        if (state != Connection.CLOSED) {
            SocketChannel sc = (SocketChannel) sk.channel();
            if (sc.isOpen()) {
                Functions.dout(2, "closing channel");
                try {
                    sc.close();
                    sk.selector().wakeup();
                    sk.attach(null);
                } catch (IOException ce) {
                    Functions.fail(ce, "close failed");
                }
            } else Functions.dout(12, "already closed");
            state = Connection.CLOSED;
            cu.stateChange(state);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String nm) {
        name = nm;
    }

    public int getState() {
        return state;
    }
}

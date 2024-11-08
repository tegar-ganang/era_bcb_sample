package biz.xsoftware.examples.socket;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * The real implementation behind TCPSocket.
 * 
 * @author Dean Hiller
 */
public class TCPSocketImpl implements TCPSocket {

    private Socket socket;

    /**
	 * @showcode
	 */
    public TCPSocketImpl(Socket s) {
        socket = s;
    }

    /**
	 * @see biz.xsoftware.examples.socket.TCPSocket#read(java.nio.ByteBuffer)
	 * @showcode
	 */
    public int read(ByteBuffer b) throws IOException {
        return socket.getChannel().read(b);
    }

    /**
	 * @see biz.xsoftware.examples.socket.TCPSocket#write(java.nio.ByteBuffer)
	 * @showcode
	 */
    public int write(ByteBuffer b) throws IOException {
        return socket.getChannel().write(b);
    }
}

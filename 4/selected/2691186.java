package org.xsocket.stream;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.junit.Test;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.IServer;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.Server;

/**
*
* @author grro@xsocket.org
*/
public final class SimpleNonBlockingClientConnectionTest {

    private static String DELIMITER = "\r\n";

    @Test
    public void testByUsingHandler() throws Exception {
        IServer server = new Server(new EchoHandler());
        StreamUtils.start(server);
        IDataHandler clientSideHandler = new IDataHandler() {

            public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
                connection.setAutoflush(false);
                String response = connection.readStringByDelimiter(DELIMITER, Integer.MAX_VALUE);
                System.out.println("response: " + response);
                return true;
            }
        };
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort(), clientSideHandler);
        con.setAutoflush(false);
        con.setDefaultEncoding("ISO-8859-1");
        System.out.println("sending the request");
        con.write("NICK maneh" + DELIMITER);
        con.write("USER asdasd" + DELIMITER);
        con.flush();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {
        }
        con.close();
        server.close();
    }

    @Test
    public void testByUsingPooling() throws Exception {
        IServer server = new Server(new EchoHandler());
        StreamUtils.start(server);
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort());
        con.setAutoflush(false);
        con.setDefaultEncoding("ISO-8859-1");
        System.out.println("sending the request");
        con.write("NICK maneh" + DELIMITER);
        con.write("USER asdasd" + DELIMITER);
        con.flush();
        boolean received = false;
        do {
            try {
                String response = con.readStringByDelimiter(DELIMITER, Integer.MAX_VALUE);
                System.out.println("response: " + response);
                received = true;
            } catch (BufferUnderflowException bue) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                }
            }
        } while (!received);
        con.close();
        server.close();
    }

    private static class EchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            connection.setAutoflush(false);
            connection.write(connection.readByteBufferByDelimiter(DELIMITER, Integer.MAX_VALUE));
            connection.write(DELIMITER);
            connection.flush();
            return true;
        }
    }
}

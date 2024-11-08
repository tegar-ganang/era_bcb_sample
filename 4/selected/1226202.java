package org.hopto.pentaj.jexin.node;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import javax.net.SocketFactory;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;
import org.hopto.pentaj.jexin.stacktrace.StackFrame;
import org.hopto.pentaj.test.StackFrameArgumentMatcher;

public class TraceClientImplTest {

    private NodeAddress node = new NodeAddress("testHost", TraceClient.DEFAULT_PORT);

    private IMocksControl control = createStrictControl();

    private SocketFactory socketFactory = control.createMock(SocketFactory.class);

    private Socket socket = control.createMock(Socket.class);

    private TraceClientObserver traceClientObserver = control.createMock(TraceClientObserver.class);

    private PipedInputStream socketInputStream = new PipedInputStream();

    private PipedOutputStream socketOutputStream = new PipedOutputStream();

    private TraceClientImpl client = new TraceClientImpl(socketFactory);

    private ObjectInputStream in;

    private ObjectOutputStream out;

    private int action;

    @Test(expected = IllegalArgumentException.class)
    public void testNullNode() throws Exception {
        client.connect(null, traceClientObserver);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullClientConfigObserver() throws Exception {
        client.connect(node, null);
    }

    @Test(expected = UnknownHostException.class)
    public void testInvalidHost() throws Exception {
        expect(socketFactory.createSocket(node.getHost(), node.getPort())).andThrow(new UnknownHostException());
        control.replay();
        try {
            client.connect(node, traceClientObserver);
        } finally {
            control.verify();
        }
    }

    @Test(expected = IOException.class)
    public void testInvalidPort() throws Exception {
        expect(socketFactory.createSocket(node.getHost(), node.getPort())).andThrow(new IOException());
        control.replay();
        try {
            client.connect(node, traceClientObserver);
        } finally {
            control.verify();
        }
    }

    @Test
    public void testObservers() throws Exception {
        Thread serverThread = new Thread(new Runnable() {

            public void run() {
                try {
                    in = new ObjectInputStream(new PipedInputStream(socketOutputStream));
                    out.writeObject("testNode");
                    out.writeObject(new HashMap<Integer, String>());
                    out.writeInt(TraceClientImpl.STACK_FRAME_START_ACTION);
                    out.writeLong(Thread.currentThread().getId());
                    out.writeObject(Thread.currentThread().getName());
                    out.writeObject("method signature");
                    out.writeObject(new int[0]);
                    out.writeInt(TraceClientImpl.STACK_FRAME_END_ACTION);
                    out.writeLong(Thread.currentThread().getId());
                    out.writeInt(TraceClientImpl.STACK_FRAME_EXCEPTION_ACTION);
                    out.writeLong(Thread.currentThread().getId());
                    out.writeObject("an exception");
                    out.flush();
                    action = in.readInt();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        expect(socketFactory.createSocket(node.getHost(), node.getPort())).andReturn(socket);
        socket.setSoTimeout(TraceClientImpl.DEFAULT_READ_TIMEOUT);
        expect(socket.getOutputStream()).andReturn(socketOutputStream);
        expect(socket.getInputStream()).andReturn(socketInputStream);
        traceClientObserver.connected("testNode", new HashMap<Integer, String>());
        socket.setSoTimeout(0);
        StackFrame startFrame = new StackFrame("method signature", new int[0]);
        expect(traceClientObserver.stackFrameStart(eq(serverThread.getId()), eq(serverThread.getName()), StackFrameArgumentMatcher.stackFrame(startFrame))).andReturn(null);
        traceClientObserver.stackFrameReturn(serverThread.getId());
        traceClientObserver.stackFrameException(serverThread.getId(), "an exception");
        traceClientObserver.disconnected();
        socket.close();
        control.replay();
        out = new ObjectOutputStream(new PipedOutputStream(socketInputStream));
        serverThread.start();
        Thread.sleep(250);
        client.connect(node, traceClientObserver);
        Thread.sleep(250);
        client.disconnect();
        out.close();
        in.close();
        serverThread.join(4000);
        assertEquals(TraceClientImpl.PROCEED_ACTION, action);
        client.waitForServerThread();
        control.verify();
    }

    @Test
    public void testConnectThenCloseStreamToClient() throws Exception {
        expect(socketFactory.createSocket(node.getHost(), node.getPort())).andReturn(socket);
        socket.setSoTimeout(TraceClientImpl.DEFAULT_READ_TIMEOUT);
        expect(socket.getOutputStream()).andReturn(socketOutputStream);
        traceClientObserver.disconnected();
        socket.close();
        control.replay();
        new ObjectOutputStream(new PipedOutputStream(socketInputStream)).close();
        try {
            client.connect(node, traceClientObserver);
            fail("closing stream without sending server config did not cause an exception");
        } catch (IOException e) {
        }
        control.verify();
    }
}

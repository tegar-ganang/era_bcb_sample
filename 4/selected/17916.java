package org.mortbay.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.nio.ChannelEndPoint;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.jetty.nio.AbstractNIOConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.log.Log;
import org.mortbay.util.ajax.Continuation;

/**
 * @author gregw
 *
 */
public class RandomConnector extends AbstractNIOConnector {

    static Random random = new Random(System.currentTimeMillis());

    static int rate = 1;

    /**
     * Constructor.
     * 
     */
    public RandomConnector() {
    }

    public Object getConnection() {
        return null;
    }

    protected void doStart() throws Exception {
        super.doStart();
    }

    protected void doStop() throws Exception {
        super.doStop();
    }

    public void open() throws IOException {
    }

    public void close() throws IOException {
    }

    public void accept(int acceptorID) throws IOException {
        try {
            File file = new File("fakeRequests.txt");
            if (!file.exists()) file = new File("modules/jetty/src/test/resources/fakeRequests.txt");
            if (!file.exists()) file = new File("src/test/resources/fakeRequests.txt");
            if (!file.exists()) file = new File("/tmp/fakeRequests.txt");
            if (!file.exists()) {
                System.err.println("No such file " + file);
                System.exit(1);
            }
            Thread.sleep(random.nextInt(50 * rate));
            ByteChannel channel = new FileInputStream(file).getChannel();
            RandomEndPoint gep = new RandomEndPoint(this, channel);
            try {
                while (gep.isOpen()) {
                    Thread.sleep(random.nextInt(10 * rate));
                    synchronized (gep) {
                        if (!gep.dispatched) {
                            gep.dispatched = true;
                            getThreadPool().dispatch(gep);
                        }
                    }
                }
            } finally {
                connectionClosed(gep._connection);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void customize(EndPoint endpoint, Request request) throws IOException {
        super.customize(endpoint, request);
    }

    public int getLocalPort() {
        return 0;
    }

    public static class RandomEndPoint extends ChannelEndPoint implements EndPoint, Runnable {

        HttpConnection _connection;

        boolean dispatched = false;

        public RandomEndPoint(RandomConnector connector, ByteChannel channel) {
            super(channel);
            _connection = new HttpConnection(connector, this, connector.getServer());
            connector.connectionOpened(_connection);
        }

        public void run() {
            System.err.print("|");
            try {
                _connection.handle();
            } catch (ClosedChannelException e) {
                Log.ignore(e);
            } catch (EofException e) {
                Log.debug("EOF", e);
                try {
                    close();
                } catch (IOException e2) {
                    Log.ignore(e2);
                }
            } catch (HttpException e) {
                Log.debug("BAD", e);
                try {
                    close();
                } catch (IOException e2) {
                    Log.ignore(e2);
                }
            } catch (Throwable e) {
                Log.warn("handle failed", e);
                try {
                    close();
                } catch (IOException e2) {
                    Log.ignore(e2);
                }
            } finally {
                Continuation continuation = _connection.getRequest().getContinuation();
                if (continuation != null && continuation.isPending()) {
                } else {
                    synchronized (RandomEndPoint.this) {
                        dispatched = false;
                    }
                }
            }
        }

        public boolean blockReadable(long millisecs) {
            try {
                Thread.sleep(random.nextInt(10 * rate));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        public boolean blockWritable(long millisecs) {
            try {
                Thread.sleep(random.nextInt(10 * rate));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        public int fill(Buffer buffer) throws IOException {
            if (random.nextInt() % 10 < 2) {
                return 0;
            }
            int len = random.nextInt(20 * rate);
            if (len > buffer.space()) len = buffer.space();
            NIOBuffer temp = new NIOBuffer(len, false);
            int len2 = super.fill(temp);
            if (len2 < 0) {
                return -1;
            }
            if (len != len2) throw new IllegalStateException();
            temp.mark();
            buffer.put(temp);
            temp.reset();
            System.err.print(temp);
            buffer.skip(len);
            return len;
        }

        public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException {
            int len = 0;
            if (header != null && header.hasContent()) len += flush(header);
            if (header == null || !header.hasContent()) {
                if (buffer != null && buffer.hasContent()) len += flush(buffer);
            }
            if (buffer == null || !buffer.hasContent()) {
                if (trailer != null && trailer.hasContent()) len += flush(trailer);
            }
            return len;
        }

        public int flush(Buffer buffer) throws IOException {
            if (random.nextInt(10) < 2) {
                return 0;
            }
            int len = random.nextInt(20 * rate);
            if (len > buffer.length()) len = buffer.length();
            Buffer temp = buffer.get(len);
            System.err.print(temp);
            return len;
        }

        public boolean isBlocking() {
            return false;
        }
    }

    public static void main(String[] arg) throws Exception {
        Server server = new Server();
        server.addConnector(new RandomConnector());
        Context context = new Context(server, "/", Context.SESSIONS);
        context.addServlet(new ServletHolder(new HelloServlet()), "/*");
        server.start();
        server.join();
    }

    public static class HelloServlet extends HttpServlet {

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.println("<h1>Hello SimpleServlet: " + request.getRequestURI() + "</h1><pre>");
            int lines = random.nextInt(100);
            for (int i = 0; i < lines; i++) out.println(i + " Blah blah blah. Now is the time for all good FSMs to work. Hoooo nooo broooon cooooo");
            out.println("</pre>");
        }
    }
}

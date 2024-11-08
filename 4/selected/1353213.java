package org.asyncj.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asyncj.Async;
import org.asyncj.handlers.HTTPHandler;
import org.asyncj.handlers.Handler;
import org.asyncj.handlers.ServerHandler;

public class SimpleHTTPServer extends ServerHandler {

    public SimpleHTTPServer(int port) throws IOException {
        super(port);
    }

    public static void main(String[] args) throws IOException {
        Async router = Async.startup();
        try {
            log.info("Usage:\n java " + SimpleHTTPServer.class + "port\n" + "Where:\n\tport - port number to listen for HTTP requests\n.");
            int port = (args.length > 0) ? Integer.parseInt(args[0]) : 8086;
            log.info("Starting the very SimpleHTTPServer server on port:" + port);
            router.register(new SimpleHTTPServer(port));
            router.run();
            System.out.println("fin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Handler getServiceHandler(Async router, SelectableChannel servingChannel) throws IOException {
        return new SimpleHTTPHandler((SocketChannel) servingChannel);
    }

    public static class SimpleHTTPHandler extends HTTPHandler {

        public SimpleHTTPHandler(SocketChannel aChannel) throws IOException {
            super(aChannel);
        }

        public void processGET(Request request) {
            String path = "c:\\" + request.requestLine[1].substring(1);
            File filehd = new File(path);
            int len;
            long llen = filehd.length();
            if (llen > Long.MAX_VALUE) len = Integer.MAX_VALUE; else len = (int) llen;
            Response response = new Response();
            response.put("content-type", "text/html");
            response.put("content-length", "" + len);
            write(response.toString().getBytes());
            System.out.println("WRITING:\n" + response);
            try {
                FileChannel sourceChannel = new FileInputStream(filehd).getChannel();
                sourceChannel.transferTo(0, sourceChannel.size(), (SocketChannel) channel);
                setWriteReady(false);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final Log log = LogFactory.getLog(SimpleHTTPHandler.class);
}

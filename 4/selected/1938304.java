package net.rptools.communitytool.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.rptools.communitytool.service.testconnect.TestConnectService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class RPServiceManager {

    private static final Logger log = Logger.getLogger(TestConnectService.class);

    private int port;

    private boolean stop;

    private ServerSocket server;

    private Map<String, RPService> handlerMap = new HashMap<String, RPService>();

    private ExecutorService threadPool = Executors.newFixedThreadPool(3);

    static {
        configureLogging();
    }

    public RPServiceManager(int port) {
        this.port = port;
        addService(new TestConnectService());
    }

    public void addService(RPService service) {
        handlerMap.put(service.getServiceId(), service);
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        server = new ServerSocket(port);
        new ReceiveThread().start();
    }

    public synchronized void stop() {
        if (server == null) {
            return;
        }
        try {
            stop = true;
            server.close();
            server = null;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private class ReceiveThread extends Thread {

        @Override
        public void run() {
            try {
                while (!stop) {
                    Socket socket = server.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    String request = reader.readLine();
                    if (!request.startsWith("GET ")) {
                        socket.close();
                        continue;
                    }
                    String handlerName = request.substring("GET ".length());
                    int index = handlerName.indexOf(" HTTP");
                    if (index < 0) {
                        socket.close();
                        continue;
                    }
                    handlerName = handlerName.substring(0, index).trim();
                    if (handlerName.length() == 0) {
                        socket.close();
                        continue;
                    }
                    if (handlerName.charAt(0) == '/' && handlerName.length() > 1) {
                        handlerName = handlerName.substring(1);
                    }
                    index = handlerName.indexOf("/");
                    String query = null;
                    if (index > 0) {
                        query = handlerName.substring(index + 1);
                        handlerName = handlerName.substring(0, index);
                    }
                    RPService handler = handlerMap.get(handlerName);
                    if (handler == null) {
                        log.error("Could not find handler for service: " + handlerName);
                        socket.close();
                        continue;
                    }
                    threadPool.submit(new HandleConnection(handler, query, socket, reader, writer));
                }
            } catch (IOException e) {
            } finally {
                server = null;
            }
        }
    }

    private static void configureLogging() {
        new DOMConfigurator().doConfigure(RPServiceManager.class.getClassLoader().getResourceAsStream("net/rptools/communitytool/service/logging.xml"), LogManager.getLoggerRepository());
    }

    private static class HandleConnection implements Runnable {

        private String request;

        private Socket socket;

        private RPService handler;

        private BufferedReader reader;

        private BufferedWriter writer;

        public HandleConnection(RPService handler, String request, Socket socket, BufferedReader reader, BufferedWriter writer) {
            this.request = request;
            this.socket = socket;
            this.handler = handler;
            this.reader = reader;
            this.writer = writer;
        }

        public void run() {
            try {
                RPServiceContext.initialize(socket);
                handler.handleRequest(request, reader, writer);
            } catch (IOException ioe) {
                log.error("IOError: " + ioe, ioe);
            } catch (Throwable t) {
                log.error("Error", t);
            } finally {
                RPServiceContext.dispose();
                try {
                    writer.flush();
                    socket.close();
                } catch (IOException ioe) {
                    log.error("Error closing socket", ioe);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        RPServiceManager service = new RPServiceManager(51235);
        service.start();
    }
}

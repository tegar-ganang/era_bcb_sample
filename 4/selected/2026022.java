package org.lindenb.tinytools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Date;
import org.lindenb.io.IOUtils;
import org.lindenb.io.YWriter;
import org.lindenb.util.Compilation;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class EchoProxy implements HttpHandler {

    private static final String CONTEXT = "/listen";

    private static final int PORT = 9999;

    @Override
    public void handle(HttpExchange http) throws IOException {
        Headers reqHeaders = http.getRequestHeaders();
        Headers respHeader = http.getResponseHeaders();
        respHeader.add("Content-Type", "text/plain");
        http.sendResponseHeaders(200, 0);
        PrintWriter console = new PrintWriter(System.err);
        PrintWriter web = new PrintWriter(http.getResponseBody());
        PrintWriter out = new PrintWriter(new YWriter(web, console));
        out.println("### " + new Date() + " ###");
        out.println("Method: " + http.getRequestMethod());
        out.println("Protocol: " + http.getProtocol());
        out.println("RemoteAddress.HostName: " + http.getRemoteAddress().getHostName());
        for (String key : reqHeaders.keySet()) {
            out.println("* \"" + key + "\"");
            for (String v : reqHeaders.get(key)) {
                out.println("\t" + v);
            }
        }
        InputStream in = http.getRequestBody();
        if (in != null) {
            out.println();
            IOUtils.copyTo(new InputStreamReader(in), out);
            in.close();
        }
        out.flush();
        out.close();
    }

    public static void main(String[] args) {
        try {
            String context = CONTEXT;
            int port = PORT;
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println("Pierre Lindenbaum PhD. " + Compilation.getLabel());
                    System.err.println("-h this screen");
                    System.err.println("-c <context> default :" + CONTEXT);
                    System.err.println("-p <port> default :" + PORT);
                    return;
                } else if (args[optind].equals("-c")) {
                    context = args[++optind];
                } else if (args[optind].equals("-p")) {
                    port = Integer.parseInt(args[++optind]);
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(context, new EchoProxy());
            System.out.println("Listening to http://localhost:" + port + "" + context);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

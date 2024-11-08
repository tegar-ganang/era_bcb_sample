package org.simpleframework;

import org.simpleframework.location.ApplicationMonitor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * @author Yo
 * 
 * @version $Revision: $, $Date: $, $Name: $
 */
public class ServerTest implements Container {

    public void handle(Request request, Response response) {
        response.setCode(200);
        response.setDate("Date", System.currentTimeMillis());
        response.setContentLength(request.getContentLength());
        byte[] buffer = new byte[4096];
        int written = 0;
        int length = request.getContentLength();
        try {
            InputStream in = request.getInputStream();
            OutputStream out = response.getOutputStream(4096);
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
                written += read;
            }
            in.close();
            out.close();
        } catch (IOException ioex) {
            ioex.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (written != length) {
                System.out.println(written + " " + length);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new ApplicationMonitor().start();
        ServerTest server = new ServerTest();
        Server processor = new ContainerServer(server, 20);
        SocketConnection connection = new SocketConnection(processor);
        int port = 9001;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]).intValue();
        }
        connection.connect(new InetSocketAddress(port));
    }
}

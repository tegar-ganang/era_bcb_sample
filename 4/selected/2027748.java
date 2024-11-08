package com.abb.util;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;

/** a tiny socket server listening on localhost:80 and printing an
    incoming data stream to stdout. The input is also transmitted back
    through the socket output stream.

    @author Axel Uhl
    @version $Id: HTTPDebugServer.java,v 1.3 2001/01/06 18:51:17 aul Exp $
*/
public class HTTPDebugServer {

    public static void main(String[] args) {
        try {
            int port = 80;
            if (args.length > 0) port = new Integer(args[0]).intValue();
            ServerSocket ss = new ServerSocket(port);
            Socket s = ss.accept();
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();
            int read = 0;
            while (read != -1) {
                read = is.read();
                if (read != -1) {
                    System.out.write(read);
                    System.out.flush();
                    os.write(read);
                    os.flush();
                }
            }
            System.out.println("\n\nclosed.");
            is.close();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

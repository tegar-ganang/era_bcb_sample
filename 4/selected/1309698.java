package com.objectwave.simpleSockets;

import com.objectwave.simpleSockets.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 *  A server that can sit between a client and a server to look like that
 *  server. This allows * us to capture byte related information about the
 *  communication between a client and a server. * SystemProperties are used to
 *  configure server. * ow.gatewayHost='The Host We are to emulate' *
 *  ow.gatewayPort='The port we are to connect on to the host' * and a command
 *  line argument of the port this server is suppose to listen for client
 *  requests
 *
 * @author  dhoag
 * @version  $Id: Gateway.java,v 2.0 2001/06/11 15:46:53 dave_hoag Exp $
 */
public class Gateway extends SimpleServer {

    String host = System.getProperty("ow.gatewayHost");

    String portStr = System.getProperty("ow.gatewayPort");

    private Vector clients = new Vector();

    /**
	 */
    public Gateway() {
        if (host == null) {
            host = "localhost";
        }
        if (portStr == null) {
            portStr = "3001";
        }
    }

    /**
	 * @param  port
	 */
    public Gateway(int port) {
        this();
        if (port > 0) {
            port(port);
            return;
        }
        String str = (String) System.getProperty("ow.gatewaySrvrPort");
        if (str != null) {
            port(new Integer(str).intValue());
        }
    }

    /**
	 *  This method should be overriden to create an instance * of your
	 *  SimpleServer. It should look the same xcept it should * Specify new
	 *  <YourServer>.startServer();
	 *
	 * @param  args
	 */
    public static void main(String args[]) {
        System.out.println("Gateway server");
        System.out.println("");
        if (args.length > 0) {
            new Gateway(new Integer(args[0]).intValue()).startServer();
        } else {
            new Gateway().startServer();
        }
    }

    /**
	 * @return  The TargetSocket value
	 * @exception  UnknownHostException
	 * @exception  IOException
	 */
    public Socket getTargetSocket() throws UnknownHostException, IOException {
        int port = 1000;
        try {
            port = Integer.parseInt(portStr);
        } catch (Throwable t) {
            System.out.println(t);
        }
        System.out.println("Directing gateway client to " + host + ":" + port);
        return new Socket(host, port);
    }

    /**
	 *  Override this method if you don't want to do anything special with the *
	 *  handling of clients. Just return your ServerClient object.
	 *
	 * @param  svr
	 * @param  count
	 * @param  thread
	 * @return  The ClientServer value
	 */
    public ServeClient getClientServer(SimpleServer svr, int count, Thread thread) {
        try {
            return new MyServeClient(svr, count, thread, getTargetSocket());
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}

/**
 * @author  dhoag
 * @version  $Id: Gateway.java,v 2.0 2001/06/11 15:46:53 dave_hoag Exp $
 */
class MyServeClient extends ServeClient {

    /**
	 */
    Socket targetSocket;

    /**
	 * @param  svr
	 * @param  count
	 * @param  thread
	 * @param  target
	 */
    MyServeClient(SimpleServer svr, int count, Thread thread, Socket target) {
        super(svr, count, thread);
        targetSocket = target;
    }

    /**
	 *  This method will print a hex dump of the given byte array to the given *
	 *  output stream. Each line of the output will be 2-digit hex numbers, *
	 *  separated by single spaces, followed by the characters corresponding to *
	 *  those hex numbers, or a '.' if the given character is unprintable. Each of
	 *  * these numbers will correspond to a byte of the byte array. * *
	 *
	 * @param  bytes the byte array to write *
	 * @param  writer the destination for the output. *
	 * @param  read
	 * @exception  java.io.IOException thrown if there's an error writing strings
	 *      to the writer.
	 * @author  Steve Sinclair *
	 */
    public static void hexDump(final byte[] bytes, int read, final java.io.Writer writer) throws java.io.IOException {
        final int width = 16;
        for (int i = 0; i < read; i += width) {
            int limit = (i + width > read) ? read - i : width;
            int j;
            StringBuffer literals = new StringBuffer(width);
            StringBuffer hex = new StringBuffer(width * 3);
            for (j = 0; j < limit; ++j) {
                int aByte = bytes[i + j];
                if (aByte < 0) {
                    aByte = 0xff + aByte + 1;
                }
                if (aByte < 0x10) {
                    hex.append('0');
                }
                hex.append(Integer.toHexString(aByte));
                hex.append(' ');
                if (aByte >= 32 && aByte < 128) {
                    literals.append((char) aByte);
                } else {
                    literals.append('.');
                }
            }
            for (; j < width; ++j) {
                literals.append(" ");
                hex.append("-- ");
            }
            hex.append(' ');
            hex.append(literals);
            hex.append('\n');
            writer.write(hex.toString());
        }
    }

    /**
	 * @return  The Gateway value
	 */
    public Gateway getGateway() {
        return (Gateway) server;
    }

    /**
	 * @param  target
	 * @param  clientStream
	 * @return  The TargetReader value
	 */
    protected Runnable getTargetReader(final Socket target, final BufferedOutputStream clientStream) {
        return new Runnable() {

            BufferedInputStream bin;

            /**
				 *  Main processing method for the MyServeClient object
				 */
            public void run() {
                byte[] bytes = new byte[1024];
                try {
                    bin = new BufferedInputStream(target.getInputStream());
                    while (true) {
                        int read = bin.read(bytes);
                        if (read == -1) {
                            System.out.println("BytesRead " + read);
                            break;
                        }
                        printBytes("FROM: Gateway Target", bytes, read);
                        clientStream.write(bytes, 0, read);
                        clientStream.flush();
                    }
                } catch (Throwable t) {
                    System.out.println("Exception in gateway read " + t);
                }
            }
        };
    }

    /**
	 *  This departs from the traditional simple server implementations.
	 *
	 * @exception  IOException
	 * @exception  EOFException
	 */
    protected void loop() throws IOException, EOFException {
        BufferedInputStream bin = socket.getBufferedInputStream();
        Runnable r = getTargetReader(targetSocket, socket.getBufferedOutputStream());
        getGateway().startThread(r);
        long tstart = 0;
        long tend = 0;
        String request = null;
        String reply = null;
        int sent = 0;
        byte[] bytes = new byte[1024];
        try {
            while (alive) {
                int read = bin.read(bytes);
                printBytes("Gateway Client ", bytes, read);
                targetSocket.getOutputStream().write(bytes, 0, read);
                tend = System.currentTimeMillis();
            }
        } catch (java.net.SocketException ex) {
            if (debug) {
                System.out.println("loop(): " + ex);
            }
            handleSocketException(ex);
        } catch (EOFException ex) {
            if (debug) {
                System.out.println("loop(): " + ex);
            }
            throw ex;
        } catch (IOException ex) {
            if (debug) {
                System.out.println("loop(): " + ex);
                ex.printStackTrace();
            }
            System.out.println("loop(): " + ex);
            throw ex;
        } finally {
            if (debug) {
                System.out.println("Exiting loop(): alive = " + alive);
            }
        }
    }

    /**
	 * @param  from
	 * @param  bytes
	 * @param  read
	 */
    protected synchronized void printBytes(String from, byte[] bytes, int read) {
        String hex = "";
        hex += Integer.toHexString(read);
        System.out.println("FROM: " + from + " Length " + hex);
        try {
            Writer w = new PrintWriter(System.out);
            hexDump(bytes, read, w);
            w.flush();
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
}

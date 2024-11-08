package com.objectwave.simpleSockets;

import com.objectwave.simpleSockets.*;
import com.objectwave.utility.StringManipulator;
import java.net.*;
import java.util.*;
import java.io.*;

/**
 * Serve up data to one particular client.
 * Two threads are used from a thread pool ro process requests.
 * One thread reads data from the client, the other publishes data.
 *
 * @version 2.0
 * @author Dave Hoag
 */
public class ServeHTTPClient extends ServeClient {

    /**  The place to write responses */
    Socket targetSocket;

    static ReplyHandler defaultReplyHandler;

    ReplyHandler replyHandler;

    MyWriter myWriter;

    /**
     * Set the default ReplyHandler if none is specified for the particular instance.
     * @param han ReplyHandler How to repsond to http requests
     */
    public static void setDefaultReplyHandler(ReplyHandler han) {
        defaultReplyHandler = han;
    }

    /**
     * Through use of a reply handler, the SimpleHTTP server can be used as is without
     * requiring a subclass of special implementation. The ReplyHander will return a String
     * as a result of requests, preferably an HTTP document.
     * @param han com.objectwave.simpleSockets.ReplyHandler A custom implementation
	 */
    public void setReplyHandler(ReplyHandler han) {
        replyHandler = han;
    }

    /**
     * @return ReplyHandle The reply handler assigned to this instance, or the default one
     */
    public ReplyHandler getReplyHandler() {
        if (replyHandler == null) {
            return defaultReplyHandler;
        }
        return replyHandler;
    }

    /**
	 * The Writer represents the client. Anything that is to be sent to client should
	 * be sent here.
	 */
    public MyWriter getMyWriter() {
        return myWriter;
    }

    /**
     * Simply associate the socket that represents this client connection with
     * this instance. A required step by the server.
	 * @param s The socket this client should now handle.
	 */
    public synchronized void bind(Socket s) throws IOException {
        System.out.println("Binding new connection");
        targetSocket = s;
        targetSocket.setSoTimeout(2000);
        super.bind(s);
    }

    /**
     * Simply pass the constructor through.
     * @param thread is the thread upon which this instance will be running.
     * @param count - Just some server assigned id
     * @param svr The SimpleServer instance using this instance.
	 */
    ServeHTTPClient(SimpleServer svr, int count, Thread thread) {
        super(svr, count, thread);
    }

    /**
	 */
    public SimpleHTTP getGateway() {
        return (SimpleHTTP) server;
    }

    /**
     * Skip some of the HTTP request header information.
     * @param str The string sent in over the socket
     * @return The remainder of the string after the beginning of the header information
	 */
    protected String extractRequest(String str) {
        if (debug) {
            System.out.println("Processing Request: " + str);
        }
        StringReader rdr = new StringReader(str);
        LineNumberReader numberReader = new LineNumberReader(rdr);
        try {
            String line = numberReader.readLine();
            line = line.substring(line.indexOf('/')).trim();
            int idx = line.indexOf(' ');
            if (idx > 0) {
                line = line.substring(0, idx);
            }
            return line;
        } catch (ThreadDeath td) {
            td.printStackTrace();
            throw td;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return str;
    }

    /**
	 * This departs from the traditional simple server implementations.
	 */
    protected void loop() throws IOException, EOFException {
        BufferedInputStream bin = socket.getBufferedInputStream();
        long tstart = 0;
        String request = null;
        HeaderAndData data = null;
        int sent = 0;
        byte[] bytes = new byte[1024];
        myWriter = new MyWriter(targetSocket);
        getGateway().startThread(myWriter);
        try {
            while (alive) {
                int read = bin.read(bytes);
                if (read > 0) {
                    String req = extractRequest(new String(bytes, 0, read));
                    data = processRequestData(req);
                    if (data != null) {
                        myWriter.sendValue(data);
                    }
                } else {
                    break;
                }
            }
        } catch (java.net.SocketException ex) {
            if (debug) System.out.println("loop(): " + ex);
            handleSocketException(ex);
        } catch (EOFException ex) {
            if (debug) System.out.println("loop(): " + ex);
            throw ex;
        } catch (IOException ex) {
            if (debug) {
                System.out.println("loop(): " + ex);
                ex.printStackTrace();
            }
            System.out.println("loop(): " + ex);
            throw ex;
        } finally {
            if (debug) System.out.println("Exiting loop(): alive = " + alive);
            myWriter.kill();
        }
    }

    /**
	 * Override this method to process your request.  Returning a string
	 * from this method will imply to this to call emitReply(<YourString>);
     * @param requestString The information on the HTTP request after the host and port
     * @return String The reply to send to the browser. This is the actual formatted HTML reply that goes over the socket.
	 */
    protected HeaderAndData processRequestData(String requestString) throws IOException {
        if (getReplyHandler() != null) {
            String req = extractRequest(requestString);
            return formatHTMLHeader(getReplyHandler().processRequest(req));
        }
        int idx = requestString.indexOf(' ');
        String actual = requestString;
        if (idx > 0) {
            requestString.substring(0, idx);
        }
        return formatHTMLHeader(new StringBufferInputStream(getTestString(actual)));
    }

    /**
	 * Override this method to process your request.  Returning a string
	 * from this method will imply to this to call emitReply(<YourString>);
     * @param requestString The information on the HTTP request after the host and port
     * @return String The reply to send to the browser. This is the actual formatted HTML reply that goes over the socket.
	protected String processRequest(String requestString)
	{
		if(getReplyHandler() != null)
		{
			String req = extractRequest(requestString);
//System.out.println("Extracted Requst " + req);
			return formatHTMLHeader(getReplyHandler().processRequest(req));
		}
		int idx = requestString.indexOf(' ');
		String actual = requestString;
		if(idx > 0)
		{
			requestString.substring(0, idx);
		}
		return formatHTMLHeader(getTestString(actual));
	}
	/**
	 * Put a standard HTML header on the actual html.
	 * @param actualHtml String Expected to be valid HTML.
	 * @return String The header prepended upon the actualHtml.
	 */
    public static HeaderAndData formatHTMLHeader(InputStream actualHtml) throws IOException {
        return new HeaderAndData("HTTP/1.1 200 OK\nDate: Thu, 05 Aug 1999 GMT\nServer: Dave's Server\nExpires: Tue, 03 Aug 1999 GMT\nConnection: Keep-Alive\nTransfer-Enoding: chunked\nContent-Type: text/html\nContent-Length:" + actualHtml.available() + "\n\n", actualHtml);
    }

    /**
	 * A simple test HTML string that will print HERE WE ARE and change the title of the page to
	 * the parameter.
	 * @param title The new title.
	 */
    String getTestString(String title) {
        String actualHtml = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">" + "<HTML>\n<HEAD>\n<META NAME='GENERATOR' Content='Visual Page 1.0 for Windows'>" + "<META HTTP-EQUIV='Content-Type' CONTENT='text/html;CHARSET=iso-8859-1'>" + "<TITLE>" + title + "</TITLE>\n</HEAD>\n<BODY>HERE WE ARE</BODY></HMTL>";
        return actualHtml;
    }

    /**
	 * This class is used to write data back to the connecting client.
	 * Writing back to the client is done on its own thread.
	 */
    class MyWriter implements Runnable {

        Socket target;

        OutputStream clientStream;

        boolean alive = true;

        public synchronized void kill() {
            alive = false;
            notifyAll();
        }

        public MyWriter(Socket ta) throws IOException {
            target = ta;
            clientStream = new BufferedOutputStream(target.getOutputStream());
        }

        Vector writeValues = new Vector();

        public synchronized void sendValue(HeaderAndData value) {
            writeValues.addElement(value);
            notifyAll();
        }

        BufferedInputStream bin;

        public void run() {
            try {
                Thread.currentThread().setName("Writer for client " + ServeHTTPClient.this.thread.getName());
                while (alive) {
                    synchronized (this) {
                        if (writeValues.size() == 0) {
                            wait();
                        }
                        if (!alive) return;
                        for (int i = 0; i < writeValues.size(); ++i) {
                            HeaderAndData clientData = (HeaderAndData) writeValues.elementAt(i);
                            byte[] data = new byte[1024];
                            InputStream in = new StringBufferInputStream(clientData.header);
                            for (int read = in.read(data); read != -1; read = in.read(data)) {
                                clientStream.write(data, 0, read);
                            }
                            in = clientData.data;
                            for (int read = in.read(data); read != -1; read = in.read(data)) {
                                clientStream.write(data, 0, read);
                            }
                        }
                        clientStream.flush();
                        writeValues.removeAllElements();
                    }
                }
            } catch (ThreadDeath td) {
                System.out.println("ThreadDeath in gateway read " + td);
                throw td;
            } catch (Throwable t) {
                System.out.println("Exception in gateway read " + t);
            } finally {
                if (debug) System.out.println("ClientWriter is exiting.");
                Thread.currentThread().setName("Pooled Thread");
            }
        }
    }

    static class HeaderAndData {

        HeaderAndData(final String hdr, final InputStream strm) {
            header = hdr;
            data = strm;
        }

        String header;

        InputStream data;
    }
}

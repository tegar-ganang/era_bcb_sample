package edu.lcmi.grouppac.http;

import java.io.*;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import edu.lcmi.grouppac.GroupPacComponent;
import edu.lcmi.grouppac.util.Debug;

/**
 * An application that listens for connections and serves files. This http server is the simplest
 * implementation possible. It's only purpose is to server the NS_Ref file, which contains the
 * strigfied reference to the Name Service.  The allowed command-line parameters are: - htport
 * &lt;int&gt;: the port the http server will listen on (default value: 80) - htbacklog
 * &lt;int&gt;: the number of requests to hold before refusing connections (default value: 5) -
 * htdocs &lt;path&gt;: the path to the root of the http server filesystem (default value: . - the
 * current folder) - h: shows a help screen  Creation date: (01/08/2001 16:19:17)
 * 
 * @version $Revision: 1.12 $
 * @author <a href="mailto:padilha@das.ufsc.br">Ricardo Sangoi Padilha</a>, <a
 *         href="http://www.das.ufsc.br/">UFSC, Florian�polis, SC, Brazil</a>
 */
public class WebServer implements GroupPacComponent {

    private int backlog = 5;

    private String htdocs = ".";

    private int port = 80;

    private volatile boolean running = true;

    class RequestThread implements Runnable {

        private Socket clientSocket;

        /**
		 * Creates a new RequestThread object.
		 * 
		 * @param s
		 */
        public RequestThread(Socket s) {
            clientSocket = s;
        }

        /**
		 * Description
		 */
        public void run() {
            try {
                HTTPrequest req = getRequest(clientSocket);
                implementMethod(req);
            } catch (IOException ioe) {
                Debug.output(3, ioe);
            }
        }
    }

    /**
	 * Constructor. Accept an array of parameters, to set the port, backlog and document root.
	 * Creation date: (03/08/2001 12:24:50)
	 * 
	 * @param args java.lang.String[]
	 */
    public WebServer(String[] args) {
        parseArgs(args);
    }

    /**
	 * Read an HTTP request into a continuous String.
	 * 
	 * @param client a connected client stream socket
	 * @return a populated HTTPrequest instance
	 * @exception ProtocolException If not a valid HTTP header
	 * @exception IOException
	 */
    public HTTPrequest getRequest(Socket client) throws IOException, ProtocolException {
        BufferedReader inbound = null;
        HTTPrequest request = null;
        try {
            inbound = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String reqhdr = readHeader(inbound);
            request = parseReqHdr(reqhdr);
            request.clientSocket = client;
            request.inbound = inbound;
        } catch (ProtocolException pe) {
            if (inbound != null) inbound.close();
            throw pe;
        } catch (IOException ioe) {
            if (inbound != null) inbound.close();
            throw ioe;
        }
        return request;
    }

    /**
	 * Runs the web server.
	 * 
	 * @see GroupPacComponent#run(Object)
	 */
    public void run(Object notificator) {
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(port, backlog);
            Debug.output(1, "HTTP Server up");
            synchronized (notificator) {
                notificator.notifyAll();
            }
            while (running) {
                try {
                    clientSocket = serverSocket.accept();
                    RequestThread rt = new RequestThread(clientSocket);
                    Thread t = new Thread(rt, "HTTP Request Processor");
                    t.start();
                } catch (Exception ioe) {
                    Debug.output(1, ioe);
                }
            }
            serverSocket.close();
        } catch (IOException ioe) {
            Debug.output(1, ioe);
            Debug.output(1, "HTTP: IOException: Server port could not be opened.");
        } finally {
            synchronized (notificator) {
                notificator.notifyAll();
            }
        }
    }

    /**
	 * Respond to an HTTP request
	 * 
	 * @param request the HTTP request to respond to
	 * @exception ProtocolException If unimplemented request method
	 */
    protected void implementMethod(HTTPrequest request) throws ProtocolException {
        try {
            edu.lcmi.grouppac.util.Debug.output(4, "HTTP: Servicing: " + request);
            if ((request.method.equals("GET")) || (request.method.equals("HEAD"))) serviceGetRequest(request); else throw new ProtocolException("Unimplemented method: " + request.method);
        } catch (ProtocolException pe) {
            sendNegativeResponse(request, pe);
        }
    }

    /**
	 * Parsed the passed request String and populate an HTTPrequest.
	 * 
	 * @param reqhdr the HTTP request as a continous String
	 * @return a populated HTTPrequest instance
	 * @exception IOException
	 * @exception ProtocolException If name,value pairs have no ':'
	 */
    protected HTTPrequest parseReqHdr(String reqhdr) throws IOException, ProtocolException {
        HTTPrequest req = new HTTPrequest();
        StringTokenizer lines = new StringTokenizer(reqhdr, "\r\n");
        String currentLine = lines.nextToken();
        StringTokenizer members = new StringTokenizer(currentLine, " \t");
        req.method = members.nextToken();
        req.file = members.nextToken();
        if (req.file.equals("/")) req.file = "/index.html";
        req.version = members.nextToken();
        while (lines.hasMoreTokens()) {
            String line = lines.nextToken();
            int slice = line.indexOf(':');
            if (slice == -1) throw new ProtocolException("Invalid HTTP header: " + line); else {
                String name = line.substring(0, slice).trim();
                String value = line.substring(slice + 1).trim();
                req.addNameValue(name, value);
            }
        }
        return req;
    }

    /**
	 * Assemble an HTTP request header String from the passed BufferedReader.
	 * 
	 * @param is the reader to use
	 * @return a continuous String representing the header
	 * @exception IOException
	 * @exception ProtocolException If a pre HTTP/1.0 request
	 */
    protected String readHeader(BufferedReader is) throws IOException, ProtocolException {
        String command;
        String line;
        if ((command = is.readLine()) == null) command = "";
        command += "\n";
        if (command.indexOf("HTTP/") != -1) {
            while (((line = is.readLine()) != null) && !line.equals("")) command += (line + "\n");
        } else throw new ProtocolException("Pre HTTP/1.0 request");
        return command;
    }

    /**
	 * Send a negative (404 NOT FOUND) response
	 * 
	 * @param request the HTTP request to respond to
	 */
    protected void sendNegativeResponse(HTTPrequest request) {
        sendNegativeResponse(request, null);
    }

    /**
	 * Send a negative (404 NOT FOUND) response
	 * 
	 * @param request the HTTP request to respond to
	 * @param pe
	 */
    protected void sendNegativeResponse(HTTPrequest request, Exception pe) {
        PrintStream outbound = null;
        try {
            outbound = new PrintStream(request.clientSocket.getOutputStream(), true);
            outbound.print("HTTP/1.0 ");
            outbound.print("404 NOT_FOUND\r\n");
            outbound.print("\r\n");
            outbound.print("404 - File not found.");
            if (pe != null) {
                outbound.print("<br>");
                pe.printStackTrace(outbound);
            }
            outbound.close();
            request.inbound.close();
        } catch (IOException ioe) {
            edu.lcmi.grouppac.util.Debug.output(3, ioe);
        }
    }

    /**
	 * Send a response header for the file and the file itself. Handles GET and HEAD request
	 * methods.
	 * 
	 * @param request the HTTP request to respond to
	 * @throws ProtocolException
	 */
    protected void serviceGetRequest(HTTPrequest request) throws ProtocolException {
        try {
            if (request.file.indexOf("..") != -1) throw new ProtocolException("Relative paths not supported");
            String fileToGet = htdocs + request.file;
            FileInputStream inFile = new FileInputStream(fileToGet);
            Debug.output(4, "HTTP: Sending file " + fileToGet + " " + inFile.available() + " Bytes");
            sendFile(request, inFile);
            inFile.close();
        } catch (FileNotFoundException fnf) {
            sendNegativeResponse(request, fnf);
        } catch (ProtocolException pe) {
            sendNegativeResponse(request, pe);
        } catch (IOException ioe) {
            sendNegativeResponse(request, ioe);
        }
    }

    /**
	 * Show a help screen.
	 */
    private void help() {
        String a = "WebServer is a basic http server\n\n";
        a += " -htport    <int>     Set the server port.\n";
        a += " -htbacklog <int>     Set the number of connections to hold before refusing new ones.\n";
        a += " -htdocs    <folder>  Document root.\n";
        System.out.println(a);
    }

    /**
	 * If there are any command-line arguments, they're processed here.
	 * 
	 * @param args command-line parameters
	 */
    private void parseArgs(String[] args) {
        if (args == null) return;
        for (int i = 0; i < args.length; i++) {
            if (args[i].toLowerCase().equals("-htport")) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                }
            } else if (args[i].toLowerCase().equals("-htbacklog")) {
                try {
                    backlog = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                }
            } else if (args[i].toLowerCase().equals("-htdocs")) htdocs = args[i + 1]; else if (args[i].toLowerCase().equals("-h")) help();
        }
    }

    /**
	 * Send the file from the InputStream
	 * 
	 * @param request the HTTP request instance
	 * @param inFile the opened input file stream to send\
	 */
    private void sendFile(HTTPrequest request, InputStream inFile) {
        PrintStream outbound = null;
        try {
            outbound = new PrintStream(request.clientSocket.getOutputStream(), true);
            outbound.print("HTTP/1.0 200 OK\r\n");
            outbound.print("Content-type: text/html\r\n");
            outbound.print("Content-Length: " + inFile.available() + "\r\n");
            outbound.print("\r\n");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
            if (!request.method.equals("HEAD")) {
                byte[] dataBody = new byte[1024];
                while (inFile.read(dataBody) != -1) outbound.write(dataBody);
            }
            outbound.close();
            request.inbound.close();
        } catch (IOException ioe) {
            Debug.output(3, ioe);
        }
    }
}

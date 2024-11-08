package iwork.util.webgate;

import java.io.*;
import java.net.*;

/** */
public class WebEndServer {

    /** */
    public static String mystring;

    /** */
    public WebEndServer thisServer;

    /**Socket to the Server. */
    private static ServerSocket s;

    /**Socket to get requests/reply. */
    public static Socket presentSocket;

    /**PrintWriter to which to write replies. */
    public static PrintWriter presentWriter;

    /**WebEndHandler handling requests. */
    public static WebEndHandler portReader;

    public static String queryStr = new String();

    /**Indicates whether server reply data is available. */
    public static boolean isAvailable = false;

    /**Create and start portReader to handle requests/replies to/from the web server. */
    public void Initialize() {
        try {
            portReader = new WebEndHandler(this);
            portReader.start();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**Check whether there is available data from the web server.  
   * @return The value of the isAvailable variable.*/
    public boolean isDataAvailable() {
        return isAvailable;
    }

    /**Get the query string. */
    public String GetQuery() {
        return queryStr;
    }

    /**Assign pout as the current writer & mark data as available. */
    public void DataAvailable(PrintWriter pout) {
        try {
            isAvailable = true;
            presentWriter = pout;
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**Write str to presentWriter.  */
    public void ReturnResult(String str) {
        try {
            presentWriter.print(str);
            presentWriter.flush();
        } catch (Exception e) {
            System.out.println("Send String " + e);
        }
    }

    /**Close presentWriter and presentSocket, and mark data as unavailable. */
    public void End() {
        try {
            presentWriter.close();
            presentSocket.close();
            isAvailable = false;
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /** */
    public void Redirect(String str) {
        ReturnResult("#R#" + str);
    }

    /** Call the exitProgram method of the portReader. */
    public void exitProgram() {
        try {
            portReader.control = false;
            portReader.exitProgram();
        } catch (Exception e) {
            System.out.println("Exit Program " + e);
        }
    }

    /**Open the web page at the given URL.
   @return 1 on success, 0 otherwise.*/
    public int OpenWebPage(String url) {
        InputStreamReader inFile;
        try {
            URL source = new URL(url);
            source.getContent();
            return 1;
        } catch (Exception e) {
            System.out.println("Opening Web Page Exception " + e);
        }
        return 0;
    }

    /**Copy a URL file to a local file. */
    public int CopyHTTPtoLocal(String url, String localpath) {
        InputStreamReader inFile;
        FileOutputStream outFile;
        int c;
        try {
            URL source = new URL(url);
            inFile = new InputStreamReader(source.openStream());
            outFile = new FileOutputStream(localpath);
            while ((c = inFile.read()) != -1) outFile.write(c);
            inFile.close();
            outFile.close();
        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 1;
    }

    /** Web Server Listener.*/
    public static void main(String[] args) {
        System.out.println("Web Server Listener Started");
        WebEndServer myServer = new WebEndServer();
        myServer.Initialize();
        String myStr = new String();
        String myInput = new String();
        boolean done = false;
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (!done) {
                myInput = stdin.readLine();
                if (myInput.equalsIgnoreCase("file")) {
                    String src = stdin.readLine();
                    String dest = stdin.readLine();
                    myServer.CopyHTTPtoLocal(src, dest);
                    done = true;
                }
                if (myInput.equalsIgnoreCase("exit")) done = true;
                if (myInput.equalsIgnoreCase("Web")) myServer.OpenWebPage("http://localhost/proxysvr?tet=test");
                if ((isAvailable == true) && (done == false)) {
                    myStr = myServer.GetQuery();
                    System.out.println(myStr);
                    myServer.ReturnResult(myInput);
                    myServer.ReturnResult(myInput);
                    myServer.End();
                } else System.out.println("No Web Input");
            }
            myServer.exitProgram();
        } catch (Exception e) {
            System.out.println("Main Program" + e);
        }
    }
}

/** */
class WebEndHandler extends Thread {

    /**The WebEndServer to handle. */
    private WebEndServer myServer;

    /**The incoming socket. */
    private Socket incoming;

    /**Control variable. The WebEndHandler runs while control is true. */
    public boolean control = true;

    /**A ServerSocket. */
    private ServerSocket s;

    /**Constructor. Assigns a Server to handle, and creates a ServerSocket on port 8189. */
    public WebEndHandler(WebEndServer Server) {
        myServer = Server;
        try {
            System.out.println("Before socket is created\n");
            s = new ServerSocket(8189);
            if (s == null) System.out.println("Server Socket not initialized");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**Run communication between the server and the requests on the server socket s. */
    public void run() {
        try {
            while (control) {
                if (!myServer.isAvailable) {
                    incoming = s.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
                    String str = in.readLine();
                    myServer.queryStr = str;
                    myServer.presentSocket = incoming;
                    PrintWriter pout = new PrintWriter(incoming.getOutputStream(), true);
                    myServer.DataAvailable(pout);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception Generated" + e);
        }
    }

    /**Stop running.*/
    public void exitProgram() {
        try {
            control = false;
            s.close();
        } catch (Exception e) {
            System.out.println("Thread Exit" + e);
        }
    }
}

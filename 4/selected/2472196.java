package uk.org.jembatan.lombok.core;

import java.io.*;
import java.net.Socket;
import java.util.*;
import javax.servlet.http.HttpServlet;
import uk.org.jembatan.lombok.servlet.*;

class LombokServerConnection extends Thread {

    public LombokServerConnection(Socket socket1, Context context1, File file) {
        root = null;
        context = null;
        socket = socket1;
        setPriority(4);
        context = context1;
        root = file;
    }

    public void setContext(Context context1) {
        context = context1;
    }

    public void setRoot(File file) {
        root = file;
    }

    private static void spoolFile(InputStream inputstream, OutputStream outputstream, int i) throws IOException {
        for (int j = 0; j < i; j++) outputstream.write(inputstream.read());
        outputstream.flush();
    }

    public void run() {
        PrintWriter printwriter = null;
        try {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "8859_1"));
            InputStream inputstream = socket.getInputStream();
            BufferedOutputStream bufferedoutputstream = new BufferedOutputStream(socket.getOutputStream());
            printwriter = new PrintWriter(new OutputStreamWriter(bufferedoutputstream, "8859_1"));
            String s = bufferedreader.readLine();
            HashMap hashmap = new HashMap(6);
            String s1;
            while ((s1 = bufferedreader.readLine()).length() != 0) {
                System.out.println("Header : " + s1);
                if (s1.startsWith("Host:")) {
                    String s2 = s1.substring(0, 4);
                    String s3 = s1.substring(5);
                    hashmap.put(s2, s3);
                } else {
                    int i = s1.lastIndexOf(":");
                    String s4 = s1.substring(0, i);
                    String s5 = s1.substring(i + 1);
                    hashmap.put(s4, s5);
                }
            }
            handleRequest(bufferedreader, inputstream, bufferedoutputstream, printwriter, s, hashmap);
            bufferedoutputstream.close();
            printwriter.close();
            socket.close();
        } catch (UnsupportedEncodingException unsupportedencodingexception) {
            System.out.println("Unsupported encoding: " + unsupportedencodingexception);
            outputError(printwriter, unsupportedencodingexception);
        } catch (IOException ioexception) {
            System.out.println("Could not get appropriate streams: " + ioexception);
            outputError(printwriter, ioexception);
        } catch (Exception exception) {
            System.out.println("unexpected exception: " + exception);
            outputError(printwriter, exception);
        }
    }

    public void outputError(PrintWriter printwriter, Exception exception) {
        try {
            printwriter.println("<html><head><title>Lombok Error Page</title></head>");
            printwriter.println("<body><h2>A problem occurred with your request</h2>");
            printwriter.println("<p>" + exception);
            printwriter.println("</body></html>");
            printwriter.close();
        } catch (Exception exception1) {
            System.out.println("unresoved problem: " + exception);
        }
    }

    public Hashtable parseParams(String s) {
        System.out.println(s);
        Hashtable hashtable = new Hashtable(8);
        StringTokenizer stringtokenizer = new StringTokenizer(s, "&");
        try {
            while (stringtokenizer.hasMoreElements()) {
                StringTokenizer stringtokenizer1 = new StringTokenizer(stringtokenizer.nextToken(), "=");
                String s1 = stringtokenizer1.nextToken();
                String s2 = stringtokenizer1.nextToken();
                hashtable.put(s1, s2);
            }
        } catch (NoSuchElementException nosuchelementexception) {
        }
        return hashtable;
    }

    public void handleRequest(BufferedReader bufferedreader, InputStream inputstream, BufferedOutputStream bufferedoutputstream, PrintWriter printwriter, String s, HashMap hashmap) {
        Hashtable hashtable = new Hashtable();
        String s1 = s;
        System.out.println(s);
        StringTokenizer stringtokenizer = new StringTokenizer(s);
        stringtokenizer.nextToken();
        s = stringtokenizer.nextToken();
        if (s.indexOf("?") >= 0) {
            int i = s.indexOf("?");
            String s2 = s.substring(i + 1);
            s = s.substring(0, i);
            System.out.println(s);
            hashtable = parseParams(s2);
        }
        if (s.endsWith("/AxisServlet") || s.endsWith(".jws") || s.endsWith("?wsdl")) {
            LombokServletContext lombokservletcontext = context.getAxisContext();
            HttpServlet httpservlet = (HttpServlet) lombokservletcontext.getServlet(0);
            LombokServletRequest lombokservletrequest = new LombokServletRequest(s, s1, hashmap, hashtable, bufferedreader);
            LombokServletResponse lombokservletresponse = new LombokServletResponse(bufferedoutputstream, printwriter);
            try {
                httpservlet.service(lombokservletrequest, lombokservletresponse);
                printwriter.flush();
                bufferedoutputstream.flush();
            } catch (Exception exception) {
                System.out.println("cannot access servlet: " + exception);
                exception.printStackTrace();
                exception.printStackTrace(printwriter);
                outputError(printwriter, exception);
            }
        } else if (s.endsWith("/admin/") || s.endsWith("/admin")) {
            LombokServletContext adminContext = context.getAdminContext();
            HttpServlet adminServlet = (HttpServlet) adminContext.getServlet(0);
            LombokServletRequest lombokservletrequest1 = new LombokServletRequest(s, s1, hashmap, hashtable, bufferedreader);
            LombokServletResponse lombokservletresponse1 = new LombokServletResponse(bufferedoutputstream, printwriter);
            try {
                adminServlet.service(lombokservletrequest1, lombokservletresponse1);
                printwriter.flush();
                bufferedoutputstream.flush();
            } catch (Exception exception1) {
                System.out.println("cannot access servlet: " + exception1);
                exception1.printStackTrace();
                exception1.printStackTrace(printwriter);
                outputError(printwriter, exception1);
            }
        } else if (s.length() >= 10 && s.substring(0, 9).equals("/services")) {
            LombokServletContext lombokservletcontext2 = context.getAxisContext();
            HttpServlet httpservlet2 = (HttpServlet) lombokservletcontext2.getServlet(0);
            LombokServletRequest lombokservletrequest2 = new LombokServletRequest(s, s1, hashmap, hashtable, bufferedreader);
            LombokServletResponse lombokservletresponse2 = new LombokServletResponse(bufferedoutputstream, printwriter);
            try {
                httpservlet2.service(lombokservletrequest2, lombokservletresponse2);
                printwriter.flush();
                bufferedoutputstream.flush();
            } catch (Exception exception2) {
                System.out.println("cannot access servlet: " + exception2);
                exception2.printStackTrace();
                exception2.printStackTrace(printwriter);
                outputError(printwriter, exception2);
            }
        } else {
            try {
                File file = new File(root.getAbsolutePath() + "/webapps/axis/index.html");
                FileInputStream fileinputstream = new FileInputStream(file);
                int j = fileinputstream.available();
                for (int k = 0; k < j; k++) bufferedoutputstream.write(fileinputstream.read());
                fileinputstream.close();
                bufferedoutputstream.flush();
                printwriter.close();
            } catch (IOException ioexception) {
                System.out.println("Problems outputting the file :" + ioexception);
                outputError(printwriter, ioexception);
            }
        }
    }

    Socket socket;

    File root;

    Context context;
}

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.Vector;

public class FTP {

    protected String host;

    protected int port;

    protected boolean passive = true;

    protected Socket controlSocket;

    protected Socket dataSocket;

    protected OutputStream controlOut;

    protected InputStream controlIn;

    protected InputStream dataIn;

    protected BufferedReader fromControl;

    protected BufferedReader fromData;

    protected PrintWriter toControl;

    protected Vector returnLines;

    protected int last_file_size;

    /** Creates new FTP */
    public FTP(String host, int port) {
        this.host = host;
        this.port = port;
        returnLines = new Vector();
        try {
            try {
                controlSocket = new Socket(host, port);
            } catch (UnknownHostException e) {
                javax.swing.JOptionPane.showMessageDialog(null, "ERROR: Cant connect to " + host + ":" + port, "ERROR!", JOptionPane.ERROR_MESSAGE);
            }
            controlIn = controlSocket.getInputStream();
            controlOut = controlSocket.getOutputStream();
            fromControl = new BufferedReader(new InputStreamReader(controlIn));
            toControl = new PrintWriter(new OutputStreamWriter(controlOut));
        } catch (IOException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "ERROR: IO-ERROR!", "ERROR!", JOptionPane.ERROR_MESSAGE);
        }
        getResult();
    }

    public int login(String user, String pass) {
        int r = -1;
        toControl.print("USER " + user + "\n");
        toControl.flush();
        r = getResult();
        if ((r != 331) && (r != 230)) return r;
        if (r == 230) return 0;
        System.out.println("USER ok");
        toControl.print("PASS " + pass + "\n");
        toControl.flush();
        if ((r = getResult()) != 230) return r;
        System.out.println("PASS ok");
        return 0;
    }

    public int cwd(String dir) {
        int r;
        toControl.print("CWD " + dir + "\n");
        toControl.flush();
        if ((r = getResult()) != 250) return r;
        return 0;
    }

    public String pwd() {
        toControl.print("PWD\n");
        toControl.flush();
        return getSubStringBetween(getResultString(), '"', '"');
    }

    public int quote(String q) {
        toControl.print("QUOTE " + q + "\n");
        toControl.flush();
        return getResult();
    }

    public String getSubStringBetween(String s, int c1, int c2) {
        System.out.println("SUBSTRING: " + s);
        return s.substring(s.indexOf(c1) + 1, s.indexOf(c2, s.indexOf(c1) + 1));
    }

    private int[] parseParentheses(String s) {
        String b;
        int[] ba;
        int i2 = 0;
        int i1 = 0;
        System.out.println("parseParentheses(): " + s);
        ba = new int[6];
        s = s.substring(s.indexOf('(') + 1, s.indexOf(')')).concat(",");
        for (i2 = 0; i2 < 6; i2++) {
            System.out.println(s.substring(0, s.indexOf(',')));
            ba[i2] = Integer.parseInt(s.substring(0, s.indexOf(',')));
            s = s.substring(s.indexOf(',') + 1);
        }
        return ba;
    }

    public Vector longls() {
        Vector returnStrings = new Vector();
        if (passive) {
            toControl.print("PASV\n");
            toControl.flush();
            int[] i = parseParentheses(getResultString());
            String datahost = i[0] + "." + i[1] + "." + i[2] + "." + i[3];
            int dataport = (i[4] * 256) + i[5];
            try {
                try {
                    dataSocket = new Socket(datahost, dataport);
                } catch (UnknownHostException e) {
                }
                dataIn = dataSocket.getInputStream();
                fromData = new BufferedReader(new InputStreamReader(dataIn));
            } catch (IOException e) {
            }
            toControl.print("LIST\n");
            toControl.flush();
            if (getResult() == 550) return null;
            int i1 = 0;
            try {
                for (String l = null; (l = fromData.readLine()) != null; returnStrings.add(l)) ;
                dataSocket.close();
                fromData.close();
            } catch (IOException e) {
            }
            getResult();
            return returnStrings;
        }
        return null;
    }

    public Vector ls() {
        Vector returnStrings = new Vector();
        if (passive) {
            toControl.print("PASV\n");
            toControl.flush();
            int[] i = parseParentheses(getResultString());
            String datahost = i[0] + "." + i[1] + "." + i[2] + "." + i[3];
            int dataport = (i[4] * 256) + i[5];
            try {
                try {
                    dataSocket = new Socket(datahost, dataport);
                } catch (UnknownHostException e) {
                }
                dataIn = dataSocket.getInputStream();
                fromData = new BufferedReader(new InputStreamReader(dataIn));
            } catch (IOException e) {
            }
            toControl.print("NLST\n");
            toControl.flush();
            if (getResult() == 550) return null;
            int i1 = 0;
            try {
                for (String l = null; (l = fromData.readLine()) != null; returnStrings.add(l)) ;
                dataSocket.close();
                fromData.close();
            } catch (IOException e) {
            }
            getResult();
            return returnStrings;
        }
        return null;
    }

    public void close() {
        toControl.print("QUIT\n");
        toControl.flush();
        getResult();
    }

    public int getResult() {
        boolean endOfThisBlock = false;
        int returnValue = -1;
        try {
            while ((!fromControl.ready())) ;
            for (String l = null; ((!endOfThisBlock) && ((l = fromControl.readLine()) != null)); ) {
                returnLines.add(new String("<-- " + l + "\n"));
                System.out.println(l);
                if ((l.length() > 3) && (l.charAt(3) == ' ') && (l.charAt(0) >= '1') && (l.charAt(0) <= '9')) {
                    endOfThisBlock = true;
                    returnValue = Integer.parseInt(l.substring(0, 3));
                }
            }
        } catch (IOException e) {
        }
        return returnValue;
    }

    public String getResultString() {
        boolean endOfThisBlock = false;
        String returnValue = null;
        try {
            while ((!fromControl.ready())) ;
            for (String l = null; ((!endOfThisBlock) && ((l = fromControl.readLine()) != null)); ) {
                returnLines.add(new String("<-- " + l + "\n"));
                System.out.println(l);
                if (l.charAt(3) != '-') {
                    endOfThisBlock = true;
                    returnValue = l.substring(4, l.length());
                }
            }
        } catch (IOException e) {
        }
        return returnValue;
    }

    public Vector getReturnLines() {
        Vector v = returnLines;
        returnLines = new Vector();
        return v;
    }

    private int parseFileSize(String s) {
        return Integer.parseInt(s.substring(s.indexOf('(') + 1, s.indexOf("bytes)", s.indexOf('(')) - 1));
    }

    public int getLastFileSize() {
        return last_file_size;
    }

    public int put(BufferedReader data, String remotePath) {
        if (passive) {
            toControl.print("TYPE I\n");
            toControl.flush();
            if (getResult() != 200) System.out.println("put(): Cant set Type to I");
            toControl.print("PASV\n");
            toControl.flush();
            int[] i = parseParentheses(getResultString());
            String datahost = i[0] + "." + i[1] + "." + i[2] + "." + i[3];
            int dataport = (i[4] * 256) + i[5];
            BufferedWriter out = null;
            int lastread = 0;
            char[] buffer = new char[2048];
            try {
                try {
                    dataSocket = new Socket(datahost, dataport);
                } catch (UnknownHostException e) {
                    System.out.println("UnknownHost");
                    return -2;
                }
                out = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
            } catch (IOException e) {
                System.out.println("IOExc!");
                return -1;
            }
            System.out.println("CWD " + remotePath.substring(0, remotePath.lastIndexOf('/')));
            toControl.print("CWD " + remotePath.substring(0, remotePath.lastIndexOf('/')) + "\n");
            toControl.flush();
            getResult();
            System.out.println("STOR " + remotePath.substring(remotePath.lastIndexOf('/') + 1));
            toControl.print("STOR " + remotePath.substring(remotePath.lastIndexOf('/') + 1) + "\n");
            toControl.flush();
            try {
                for (int c = 0; (lastread = data.read(buffer, 0, 2048)) != -1; ) {
                    out.write(buffer, 0, lastread);
                    c += lastread;
                }
                out.close();
            } catch (IOException e) {
                return -2;
            }
            getResult();
            getResult();
        }
        return 0;
    }

    public BufferedInputStream retr(String localFilePath) {
        toControl.print("TYPE I\n");
        toControl.flush();
        if (getResult() != 200) System.out.println("put(): Cant set Type to I");
        if (passive) {
            toControl.print("PASV\n");
            toControl.flush();
            int[] i = parseParentheses(getResultString());
            String datahost = i[0] + "." + i[1] + "." + i[2] + "." + i[3];
            int dataport = (i[4] * 256) + i[5];
            BufferedInputStream in = null;
            try {
                try {
                    dataSocket = new Socket(datahost, dataport);
                } catch (UnknownHostException e) {
                    System.out.println("UnknownHost");
                }
                in = new BufferedInputStream(dataSocket.getInputStream());
            } catch (IOException e) {
                System.out.println("IOExc!");
            }
            System.out.println("Sending RETR");
            toControl.print("RETR " + localFilePath + "\n");
            toControl.flush();
            System.out.println("sent!");
            last_file_size = parseFileSize(getResultString());
            System.out.println("result got");
            return in;
        }
        return null;
    }
}

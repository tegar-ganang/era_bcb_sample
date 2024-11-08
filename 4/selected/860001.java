package de.kout.wlFxp;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.regex.*;

/**
 *  here are all the ftp things
 *
 *@author     Alexander Kout
 *@created    30. M�rz 2002
 */
public class Ftp extends Thread {

    MainPanel panel;

    String host, user, pass;

    int port;

    String pwd;

    String getReplyMsg;

    SocketChannel sc = null;

    BufferedReader reader = null;

    getReplyThread gThread;

    PrintStream writer = null;

    InetAddress localAddress;

    Vector cmd;

    boolean connected;

    String transferMode = "";

    boolean ready = true;

    boolean quitting;

    long size;

    volatile long rest;

    private Charset charset = Charset.forName("ISO-8859-15");

    private CharsetDecoder decoder = charset.newDecoder();

    private CharsetEncoder encoder = charset.newEncoder();

    private ByteBuffer dbuf = ByteBuffer.allocateDirect(1024000);

    private CharBuffer cbuf = CharBuffer.allocate(1024);

    /**
	 *  Constructor for the Ftp object
	 *
	 *@param  panel  the parent panel
	 *@param  host   the host to connect to
	 *@param  port   the port to connect to
	 *@param  user   the username to use
	 *@param  pass   the password to use
	 */
    public Ftp(MainPanel panel, String host, int port, String user, String pass) {
        super();
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.panel = panel;
        cmd = new Vector();
    }

    /**
	 *  Gets the connected attribute of the Ftp object
	 *
	 *@return    The connected value
	 */
    public boolean isConnected() {
        return sc.isConnected();
    }

    /**
	 *  gets some line of reply from the control port
	 *
	 *@return                  The reply value
	 *@exception  IOException  Description of Exception
	 */
    public int getReply() throws IOException {
        while (gThread.ret == 0) {
            try {
                sleep(50);
            } catch (InterruptedException e) {
            }
        }
        getReplyMsg = gThread.line;
        int ret = gThread.ret;
        Utilities.print("ret = " + ret + "\n");
        gThread.ret = 0;
        return ret;
    }

    /**
	 *  gets the current directory
	 *
	 *@exception  IOException  Description of Exception
	 */
    public void getPwd() throws IOException {
        try {
            doCmd("PWD");
            String output = getReplyMsg;
            while (output.length() < 6 || output.substring(5, output.length()).indexOf("\"") == -1) {
                getReply();
                output = getReplyMsg;
            }
            String tmppwd = output.substring(5, output.length());
            pwd = tmppwd.substring(0, tmppwd.indexOf("\""));
            panel.ftpDir = pwd;
        } catch (CharacterCodingException e) {
            panel.frame.statusArea.append(e + "\n", panel.color);
        }
    }

    /**
	 *  Gets the transferRate attribute of the Ftp object
	 *
	 *@param  time  Description of Parameter
	 *@param  size  Description of Parameter
	 */
    public void getTransferRate(long time, long size) {
        double secs = time / 1000.0;
        double rate = size / secs;
        panel.frame.statusArea.append(secs + "s " + "size " + Utilities.humanReadable(size) + " rate: " + Utilities.humanReadable(rate) + "/s\n", panel.color);
    }

    /**
	 *  Main processing method for the Ftp object
	 */
    public void run() {
        try {
            connect();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            panel.frame.statusArea.append(e + "\n", panel.color);
            try {
                if (sc != null) {
                    sc.close();
                }
            } catch (IOException ex) {
                System.err.println("connect failed!");
            }
        }
        if (!isConnected()) {
            return;
        }
        try {
            doList();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            try {
                sc.close();
                quit();
            } catch (IOException ex) {
                panel.frame.statusArea.append(e + "\n", panel.color);
            }
        }
        String command;
        while (connected) {
            synchronized (cmd) {
                if (cmd.isEmpty()) {
                    try {
                        cmd.wait();
                    } catch (InterruptedException e) {
                        panel.frame.statusArea.append(e + "\n", panel.color);
                    }
                }
                ready = false;
                command = (String) cmd.firstElement();
                cmd.removeElementAt(0);
            }
            try {
                if (command.equals("quit")) {
                    quit();
                } else if (command.equals("connect")) {
                    connect();
                } else if (command.startsWith("changeDir")) {
                    changeDir(command.substring(10, command.length()));
                } else if (command.equals("cdup")) {
                    cdup();
                } else if (command.startsWith("cmd")) {
                    doCmd(command.substring(4, command.length()));
                } else if (command.equals("list")) {
                    doList();
                } else if (command.equals("download")) {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    panel.frame.queueList.updateView();
                    FtpFile from = transfer.getSource();
                    FtpFile to = transfer.getDest();
                    if (from.isDirectory()) {
                        new File(to.getAbsolutePath()).mkdir();
                        panel.frame.queueList.removeFirst();
                        changeDir(from.getAbsolutePath());
                        doList();
                        FtpFile[] files = panel.files;
                        for (int i = files.length - 1; i >= 0; i--) {
                            FtpFile tmp = new FtpFile(files[i].getName());
                            tmp.setAbsolutePath(to.getAbsolutePath() + File.separator + files[i].getName());
                            panel.frame.queueList.addAtBegin(new Transfer(files[i], tmp, panel.mode, panel.otherPanel.mode, panel.position, panel.ftpSession.currentServer, null));
                        }
                    } else {
                        if (!panel.ftpDir.equals(from.getAbsolutePath().substring(0, from.getAbsolutePath().lastIndexOf("/")))) {
                            changeDir(from.getAbsolutePath().substring(0, from.getAbsolutePath().lastIndexOf("/")));
                            doList();
                        }
                        if (!panel.otherPanel.dir.equals(to.getAbsolutePath().substring(0, to.getAbsolutePath().lastIndexOf(File.separator)))) {
                            panel.otherPanel.setDir(to.getAbsolutePath().substring(0, to.getAbsolutePath().lastIndexOf(File.separator)));
                        }
                        if (!transferMode.equals("BINARY")) {
                            doCmd("TYPE I");
                            transferMode = "BINARY";
                        }
                        this.size = from.getSize();
                        if (new File(to.getAbsolutePath()).exists()) {
                            to.setSize(new File(to.getAbsolutePath()).length());
                            transfer = new Transfer(from, to, panel.mode, panel.otherPanel.mode, panel.position, panel.ftpSession.currentServer, null);
                        }
                        rest = 0;
                        if (to.getSize() > 0 && to.getSize() == from.getSize()) {
                            rest = -1;
                        } else if (to.getSize() > 0) {
                            new ResumeDialog(panel, transfer);
                        }
                        if (rest != -1) {
                            download("RETR " + from.getName());
                            if (!panel.frame.tm.abort) {
                                panel.frame.queueList.removeFirst();
                                panel.frame.queueList.updateView();
                            }
                        } else {
                            panel.frame.queueList.removeFirst();
                            panel.frame.queueList.updateView();
                        }
                    }
                    panel.otherPanel.updateView();
                    synchronized (panel.frame.tm.done) {
                        panel.frame.tm.done.notify();
                    }
                } else if (command.equals("upload")) {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    FtpFile from = transfer.getSource();
                    FtpFile to = transfer.getDest();
                    if (from.isDirectory()) {
                        doCmd("MKD " + to.getAbsolutePath());
                        panel.frame.queueList.removeFirst();
                        panel.otherPanel.setDir(from.getAbsolutePath());
                        FtpFile[] files = from.list();
                        for (int i = files.length - 1; i >= 0; i--) {
                            FtpFile tmp = new FtpFile(files[i].getName());
                            tmp.setAbsolutePath(to.getAbsolutePath() + "/" + files[i].getName());
                            panel.frame.queueList.addAtBegin(new Transfer(files[i], tmp, panel.otherPanel.mode, panel.mode, panel.otherPanel.position, null, panel.ftpSession.currentServer));
                        }
                    } else {
                        if (!panel.ftpDir.equals(to.getAbsolutePath().substring(0, to.getAbsolutePath().lastIndexOf("/")))) {
                            changeDir(to.getAbsolutePath().substring(0, to.getAbsolutePath().lastIndexOf("/")));
                            doList();
                        }
                        if (!panel.otherPanel.dir.equals(from.getAbsolutePath().substring(0, from.getAbsolutePath().lastIndexOf(File.separator)))) {
                            panel.otherPanel.setDir(from.getAbsolutePath().substring(0, from.getAbsolutePath().lastIndexOf(File.separator)));
                        }
                        if (!transferMode.equals("BINARY")) {
                            doCmd("TYPE I");
                            transferMode = "BINARY";
                        }
                        this.size = from.getSize();
                        rest = 0;
                        upload("STOR " + from.getName());
                        if (!panel.frame.tm.abort) {
                            panel.frame.queueList.removeFirst();
                            panel.frame.queueList.updateView();
                        }
                    }
                    synchronized (panel.frame.tm.done) {
                        panel.frame.tm.done.notify();
                    }
                } else if (command.startsWith("MKD")) {
                    doCmd(command);
                    doList();
                } else if (command.equals("fxp")) {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    panel.frame.queueList.updateView();
                    FtpFile from = transfer.getSource();
                    FtpFile to = transfer.getDest();
                    fxp(from.getName());
                    panel.frame.queueList.removeFirst();
                    panel.frame.queueList.updateView();
                    synchronized (panel.frame.tm.done) {
                        panel.frame.tm.done.notify();
                    }
                } else if (command.equals("delete")) {
                    Vector v = (Vector) cmd.firstElement();
                    cmd.removeElementAt(0);
                    while (!v.isEmpty()) {
                        FtpFile f = (FtpFile) v.firstElement();
                        v.removeElementAt(0);
                        if (!f.isDirectory()) {
                            if (!panel.ftpDir.equals(f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf("/")))) {
                                changeDir(f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf("/")));
                            }
                            doCmd("DELE " + f.getName());
                        } else if (f.isDirectory()) {
                            recursiveDelete(f);
                        }
                    }
                    panel.updateView();
                } else {
                    doCmd(command);
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
                try {
                    sc.close();
                    quit();
                } catch (IOException ex) {
                    panel.frame.statusArea.append(e + "\n", panel.color);
                }
            }
            try {
                sleep(50);
            } catch (InterruptedException e) {
                panel.frame.statusArea.append(e + "\n", panel.color);
            } finally {
                ready = true;
            }
        }
    }

    /**
	 *  Adds a feature to the Command attribute of the Ftp object
	 *
	 *@param  command  The feature to be added to the Command attribute
	 */
    public void addCommand(String command) {
        synchronized (cmd) {
            cmd.addElement(command);
            cmd.notify();
        }
    }

    /**
	 *  Adds a feature to the Transfer attribute of the Ftp object
	 *
	 *@param  command  The feature to be added to the Transfer attribute
	 *@param  t        The feature to be added to the Transfer attribute
	 */
    public void addTransfer(String command, Transfer t) {
        synchronized (cmd) {
            cmd.addElement(command);
            cmd.addElement(t);
            cmd.notify();
        }
    }

    /**
	 *  Description of the Method
	 *
	 *@param  dir              Description of the Parameter
	 *@exception  IOException  Description of the Exception
	 */
    protected void recursiveDelete(FtpFile dir) throws IOException {
        changeDir(dir.getAbsolutePath());
        doList();
        FtpFile[] files = panel.files;
        for (int i = files.length - 1; i >= 0; i--) {
            if (!files[i].isDirectory()) {
                doCmd("DELE " + files[i].getName());
            } else {
                recursiveDelete(files[i]);
            }
        }
        changeDir(dir.getAbsolutePath());
        cdup();
        doCmd("RMD " + dir.getName());
    }

    /**
	 *  Description of the Method
	 *
	 *@exception  IOException  Description of Exception
	 */
    public void doList() throws IOException {
        if (!transferMode.equals("ASCII")) {
            doCmd("TYPE A");
            transferMode = "ASCII";
        }
        panel.updateFtpView(list("LIST"));
    }

    /**
	 *  connects to host and makes login, pwd
	 *
	 *@exception  IOException  Description of Exception
	 */
    public void connect() throws IOException {
        panel.frame.statusArea.append("Connecting to: " + host + "\n", panel.color);
        connectToServer();
        if (getReply() > 3) {
            quit();
            return;
        }
        if (login() > 3) {
            quit();
            return;
        }
        getPwd();
    }

    /**
	 *  is called by connect to make a new SocketChannel it also gets the
	 *  localAddress for later
	 *
	 *@exception  IOException  Description of Exception
	 */
    public void connectToServer() throws IOException {
        Socket tmp = new Socket(host, port);
        localAddress = tmp.getLocalAddress();
        tmp.close();
        InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(host), port);
        sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(isa);
        while (!sc.finishConnect()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
        gThread = new getReplyThread(panel, sc);
        connected = true;
    }

    /**
	 *  Description of the Method
	 *
	 *@return                  Description of the Returned Value
	 *@exception  IOException  Description of Exception
	 */
    public int login() throws IOException {
        String msg = "USER " + user;
        doCmd(msg);
        msg = "PASS " + pass;
        return doCmd(msg);
    }

    /**
	 *  sends a command and calls getReply
	 *
	 *@param  cmd              the command without <CR>
	 *@return                  Description of the Returned Value
	 *@exception  IOException  Description of Exception
	 */
    public int doCmd(String cmd) throws IOException {
        gThread.ret = 0;
        if (cmd.startsWith("PASS")) {
            panel.frame.statusArea.append("PASS\n", panel.color);
        } else {
            panel.frame.statusArea.append(cmd + "\n", panel.color);
        }
        cbuf.clear();
        cbuf.put(cmd + "\r\n");
        cbuf.flip();
        sc.write(encoder.encode(cbuf));
        cbuf.clear();
        Utilities.print(cmd + "\r\n");
        return getReply();
    }

    /**
	 *  Description of the Method
	 *
	 *@param  dir              Description of Parameter
	 *@exception  IOException  Description of Exception
	 */
    public void changeDir(String dir) throws IOException {
        doCmd("CWD " + dir);
        getPwd();
        doList();
    }

    /**
	 *  Description of the Method
	 *
	 *@exception  IOException  Description of Exception
	 */
    public void cdup() throws IOException {
        doCmd("CDUP");
        getPwd();
        doList();
    }

    /**
	 *  does command "quit" and closes the SocketChannel
	 *
	 *@exception  IOException  Description of Exception
	 */
    public void quit() throws IOException {
        quitting = true;
        Utilities.print("quitting...\n");
        panel.ftpDir = " ";
        connected = false;
        panel.updateFtpView("");
        if (sc != null) {
            if (sc.isConnected()) {
                doCmd("QUIT");
                gThread.quit = true;
                Utilities.print("socket closed: " + host + "\n");
            }
        }
    }

    /**
	 *  Description of the Method
	 *
	 *@param  v  Description of the Parameter
	 */
    public void delete(Vector v) {
        synchronized (cmd) {
            cmd.addElement("delete");
            cmd.addElement(v);
            cmd.notify();
        }
    }

    /**
	 *  lists the content of the current directory
	 *
	 *@param  command          Description of Parameter
	 *@return                  Description of the Returned Value
	 *@exception  IOException  Description of Exception
	 */
    public String list(String command) throws IOException {
        SocketChannel sc = null;
        if (panel.ftpSession.passive) {
            sc = doPASV(command);
        } else {
            sc = acceptConnection(command);
        }
        if (sc == null) {
            return "";
        }
        String output = "";
        int amount;
        dbuf.clear();
        while ((amount = sc.read(dbuf)) != -1) {
            dbuf.flip();
            CharBuffer cb = decoder.decode(dbuf);
            output += cb;
            dbuf.clear();
        }
        sc.close();
        gThread.ret = 0;
        return output;
    }

    /**
	 *  Description of the Method
	 *
	 *@param  command          Description of Parameter
	 *@exception  IOException  Description of Exception
	 */
    public void download(String command) throws IOException {
        SocketChannel sc = null;
        boolean append = false;
        if (panel.ftpSession.passive) {
            sc = doPASV(command);
        } else {
            sc = acceptConnection(command);
        }
        Date d1 = new Date();
        if (sc == null) {
            return;
        }
        if (rest > 0) {
            append = true;
        }
        FileOutputStream fos = new FileOutputStream(new File(panel.otherPanel.dir + File.separator + command.substring(5, command.length())), append);
        FileChannel fc = fos.getChannel();
        int amount;
        dbuf.clear();
        long size = rest;
        Date d3 = new Date();
        Date d4;
        long diffSize = 0;
        long diffTime = 0;
        try {
            while ((amount = sc.read(dbuf)) != -1 && !panel.frame.tm.abort) {
                d4 = new Date();
                size += amount;
                diffSize += amount;
                if ((diffTime = d4.getTime() - d3.getTime()) > 1000) {
                    panel.frame.pBar.setValue((int) (size * 100 / this.size));
                    String perf = Utilities.humanReadable(size) + "@" + Utilities.humanReadable(diffSize / (diffTime / 1000.0)) + "/s";
                    panel.frame.statusLabel.setText(perf);
                    panel.frame.setTitle(perf);
                    diffSize = 0;
                    diffTime = 0;
                    d3 = new Date();
                }
                dbuf.flip();
                fc.write(dbuf);
                dbuf.clear();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        panel.frame.pBar.setValue(0);
        panel.frame.statusLabel.setText("");
        panel.frame.setTitle("wlFxp");
        fc.force(true);
        fos.close();
        gThread.ret = 0;
        sc.close();
        Date d2 = new Date();
        getTransferRate(d2.getTime() - d1.getTime(), size);
    }

    /**
	 *  Description of the Method
	 *
	 *@param  command          Description of Parameter
	 *@exception  IOException  Description of Exception
	 */
    public void upload(String command) throws IOException {
        SocketChannel sc = null;
        if (panel.ftpSession.passive) {
            sc = doPASV(command);
        } else {
            sc = acceptConnection(command);
        }
        Date d1 = new Date();
        if (sc == null) {
            return;
        }
        FileInputStream fis = new FileInputStream(new File(panel.otherPanel.dir + File.separator + command.substring(5, command.length())));
        FileChannel fc = fis.getChannel();
        int amount;
        dbuf.clear();
        long size = 0;
        Date d3 = new Date();
        Date d4;
        long diffSize = 0;
        long diffTime = 0;
        try {
            while ((amount = fc.read(dbuf)) != -1 && !panel.frame.tm.abort) {
                d4 = new Date();
                size += amount;
                diffSize += amount;
                if ((diffTime = d4.getTime() - d3.getTime()) > 1000) {
                    panel.frame.pBar.setValue((int) (size * 100 / this.size));
                    String perf = Utilities.humanReadable(size) + "@" + Utilities.humanReadable(diffSize / (diffTime / 1000.0)) + "/s";
                    panel.frame.statusLabel.setText(perf);
                    panel.frame.setTitle(perf);
                    diffSize = 0;
                    diffTime = 0;
                    d3 = new Date();
                }
                dbuf.flip();
                sc.write(dbuf);
                dbuf.clear();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        panel.frame.pBar.setValue(0);
        panel.frame.statusLabel.setText("");
        panel.frame.setTitle("wlFxp");
        fis.close();
        sc.close();
        gThread.ret = 0;
        Date d2 = new Date();
        getTransferRate(d2.getTime() - d1.getTime(), size);
    }

    /**
	 *  Description of the Method
	 *
	 *@param  filename         Description of Parameter
	 *@exception  IOException  Description of Exception
	 */
    public void fxp(String filename) throws IOException {
        if (!transferMode.equals("BINARY")) {
            doCmd("TYPE I");
            transferMode = "BINARY";
        }
        if (doCmd("PASV") > 3) {
            return;
        }
        String output = getReplyMsg;
        output = output.substring(output.indexOf("(") + 1, output.indexOf(")"));
        panel.otherPanel.ftpSession.ftp.receivefxp(output, filename);
    }

    /**
	 *  Description of the Method
	 *
	 *@param  output           Description of Parameter
	 *@param  filename         Description of Parameter
	 *@exception  IOException  Description of Exception
	 */
    public void receivefxp(String output, String filename) throws IOException {
        if (!transferMode.equals("BINARY")) {
            doCmd("TYPE I");
            transferMode = "BINARY";
        }
        if (doCmd("PORT " + output) > 3) {
            return;
        }
        if (doCmd("STOR " + filename) < 4) {
            if (panel.otherPanel.ftpSession.ftp.doCmd("RETR " + filename) < 4) {
            }
            gThread.ret = 0;
        }
    }

    /**
	 *  gets a SocketChannel for a datatransfer after some command is send
	 *
	 *@param  cmd              the command that needs a dataport
	 *@return                  Description of the Returned Value
	 *@exception  IOException  Description of Exception
	 */
    public SocketChannel acceptConnection(String cmd) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress isa = new InetSocketAddress(localAddress, 0);
        ssc.socket().bind(isa);
        port(ssc.socket());
        if (rest > 0) {
            doCmd("REST " + rest);
        }
        if (doCmd(cmd) > 3) {
            return null;
        }
        return ssc.accept();
    }

    /**
	 *  sends a port command
	 *
	 *@param  serverSocket     the socket that should be used to accept a
	 *      connection
	 *@exception  IOException  Description of Exception
	 */
    public void port(ServerSocket serverSocket) throws IOException {
        int localport = serverSocket.getLocalPort();
        InetAddress inetaddress = serverSocket.getInetAddress();
        byte[] addrbytes = inetaddress.getAddress();
        short addrshorts[] = new short[4];
        for (int i = 0; i <= 3; i++) {
            addrshorts[i] = addrbytes[i];
            if (addrshorts[i] < 0) {
                addrshorts[i] += 256;
            }
        }
        doCmd("port " + addrshorts[0] + "," + addrshorts[1] + "," + addrshorts[2] + "," + addrshorts[3] + "," + ((localport & 0xff00) >> 8) + "," + (localport & 0x00ff));
    }

    /**
	 *  sends a PASV command and returns a InetSocketAddress that you can connect
	 *  to
	 *
	 *@param  command          Description of Parameter
	 *@return                  the InetSocketAddress which you can build a
	 *      SocketChannel from.
	 *@exception  IOException  Description of Exception
	 */
    public SocketChannel doPASV(String command) throws IOException {
        InetSocketAddress isa = null;
        dbuf.clear();
        try {
            doCmd("PASV");
            String output = getReplyMsg;
            while (output.indexOf(")") == -1) {
                getReply();
                output = getReplyMsg;
            }
            output = output.substring(output.indexOf("(") + 1, output.indexOf(")"));
            String[] parts = output.split(",");
            String ip = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
            int port = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]);
            isa = new InetSocketAddress(InetAddress.getByName(ip), port);
            if (rest > 0) {
                doCmd("REST " + rest);
            }
            panel.frame.statusArea.append(command + "\n", panel.color);
            Utilities.print(command + "\r\n");
            gThread.ret = 0;
            cbuf.put(command + "\r\n");
            cbuf.flip();
            sc.write(encoder.encode(cbuf));
            cbuf.clear();
            SocketChannel sc = SocketChannel.open();
            sc.connect(isa);
            if (getReply() > 3) {
                sc = null;
            }
            return sc;
        } catch (CharacterCodingException e) {
            panel.frame.statusArea.append(e + "\n", panel.color);
        }
        return sc;
    }
}

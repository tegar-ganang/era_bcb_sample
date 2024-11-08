package de.kout.wlFxp.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import de.kout.wlFxp.Configuration;
import de.kout.wlFxp.MyFile;
import de.kout.wlFxp.Transfer;
import de.kout.wlFxp.Utilities;
import de.kout.wlFxp.wlFxp;
import de.kout.wlFxp.crypto.jotp;
import de.kout.wlFxp.interfaces.wlPanel;

/**
 * here are all the ftp things
 * 
 * @author Alexander Kout
 * 
 * 30. März 2002
 */
public class Ftp extends Thread {

    private boolean isPRETCapable = true;

    wlPanel panel;

    String host, user, pass;

    int port;

    String pwd;

    String getReplyMsg, getReplyMultiLine;

    SSLSocketChannel sc = null;

    getReplyThread gThread;

    InetAddress localAddress;

    Vector<Object> cmd;

    boolean connected;

    String transferMode = "";

    boolean quitting;

    boolean proxy;

    boolean abortConnect;

    long size;

    /**
	 * Description of the Field
	 */
    public volatile long rest;

    int retrycount, retrydelay;

    long timeout;

    Hashtable<String, String> cache;

    boolean aborted;

    private Charset charset;

    private CharsetDecoder decoder;

    private CharsetEncoder encoder;

    private ByteBuffer dbuf = ByteBuffer.allocateDirect(65536);

    private ByteBuffer ubuf = ByteBuffer.allocateDirect(16384);

    int SSL;

    boolean PROTdata, PROTlist;

    private CharBuffer cbuf = CharBuffer.allocate(4096);

    private CRC32 crc;

    private byte[] crcbuffer = new byte[65536];

    /**
	 * 
	 */
    public boolean computeCRC;

    private Hashtable<String, String> crcCache;

    private StringBuffer output = new StringBuffer(32000);

    private FtpServer server;

    private Configuration cfg;

    /**
	 * Constructor for the Ftp object
	 * 
	 * @param panel
	 *            the parent panel
	 * @param server
	 *            Description of the Parameter
	 */
    public Ftp(wlPanel panel, FtpServer server) {
        super();
        cfg = wlFxp.getConfig();
        crc = new CRC32();
        crcCache = new Hashtable<String, String>();
        charset = Charset.forName(cfg.locale());
        decoder = charset.newDecoder();
        encoder = charset.newEncoder();
        this.host = server.getFirstHost();
        this.server = server;
        if (server.getPort() == 0) {
            port = 21;
        } else {
            port = server.getPort();
        }
        this.user = server.getUser();
        this.pass = server.getPasswd();
        this.panel = panel;
        if (server.getRetryCount() != -1) {
            retrycount = server.getRetryCount();
        } else {
            retrycount = cfg.getRetrycount();
        }
        if (server.getRetryDelay() != -1) {
            retrydelay = server.getRetryDelay();
        } else {
            retrydelay = cfg.getRetrydelay();
        }
        SSL = server.getSSL();
        PROTdata = server.getPROTdata();
        PROTlist = server.getPROTlist();
        timeout = cfg.getTimeout() * 1000;
        cmd = new Vector<Object>();
        cache = new Hashtable<String, String>();
    }

    /**
	 * Gets the connected attribute of the Ftp object
	 * 
	 * @return The connected value
	 */
    public boolean isConnected() {
        if (!connected) {
            return false;
        }
        return sc.isConnected();
    }

    /**
	 * checks if the reply thread has some new lines
	 * 
	 * @return The reply value
	 * @exception IOException
	 *                Description of Exception
	 */
    public int getReply() throws IOException {
        gThread.start = System.currentTimeMillis();
        while (gThread.ret == 0) {
            try {
                sleep(50);
            } catch (InterruptedException e) {
            }
        }
        getReplyMsg = gThread.line;
        getReplyMultiLine = gThread.lines;
        int ret = gThread.ret;
        Utilities.print("ret = " + ret + "\n");
        gThread.ret = 0;
        gThread.start = 0;
        return ret;
    }

    /**
	 * gets the current directory
	 * 
	 * @exception IOException
	 *                Description of Exception
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
            panel.setFtpDir(pwd);
        } catch (CharacterCodingException e) {
            panel.getFrame().getStatusArea().append(e + "\n", panel.getColor());
        }
    }

    /**
	 * Gets the transferRate attribute of the Ftp object
	 * 
	 * @param time
	 *            Description of Parameter
	 * @param size
	 *            Description of Parameter
	 */
    public void getTransferRate(long time, long size) {
        double secs = time / 1000.0;
        double rate = size / secs;
        StringBuffer t = new StringBuffer(100);
        t.append(Utilities.humanReadableTime(secs)).append(" size: ").append(Utilities.humanReadable(size)).append(" rate: ").append(Utilities.humanReadable(rate)).append("/s\n");
        panel.getFrame().getStatusArea().append(t.toString(), "darkred");
    }

    /**
	 * Main processing method for the Ftp object
	 */
    public void run() {
        proxy = false;
        cfg.setAbort(false);
        for (int i = 0; !abortConnect && i < retrycount && !isConnected() && !(i == 1 && !cfg.getAutoreconnect()) && !quitting && panel.getFtpSession().getFtp() != null; i++) {
            try {
                connect();
            } catch (Exception e) {
                if (Utilities.debug) {
                    e.printStackTrace(System.err);
                }
                Utilities.saveStackTrace(e);
                panel.getFrame().getStatusArea().append(e + "\n", "red");
                try {
                    if (sc != null) {
                        sc.close();
                    }
                } catch (IOException ex) {
                    System.err.println("sc close failed!");
                }
                if (cfg.getAutoreconnect() && !panel.getFtpSession().connected() && !quitting) {
                    panel.getFrame().getStatusArea().append(host + " connection lost #" + (i + 1) + ".\nreconnecting in " + retrydelay + " secs.\n", "red");
                    try {
                        Thread.sleep(retrydelay * 1000);
                    } catch (InterruptedException ie) {
                    }
                }
                host = server.getNextHost();
            }
        }
        if (!isConnected()) {
            if (wlFxp.getTm() != null) {
                cfg.setAbort(true);
                synchronized (wlFxp.getTm().done) {
                    wlFxp.getTm().done.notify();
                }
                synchronized (wlFxp.getTm().connect) {
                    wlFxp.getTm().connect.notify();
                }
            }
            return;
        }
        try {
            if (!abortConnect) {
                doList(false);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            Utilities.saveStackTrace(e);
            panel.getFrame().getStatusArea().append(e + "\n", "red");
            try {
                sc.close();
                quit(false);
            } catch (IOException ex) {
            }
        }
        if (wlFxp.getTm() != null) {
            synchronized (wlFxp.getTm().done) {
                wlFxp.getTm().done.notify();
            }
            synchronized (wlFxp.getTm().connect) {
                wlFxp.getTm().connect.notify();
            }
        }
        String command;
        long noopTime;
        long frameNoopTime;
        while (connected) {
            if (cfg.isNOOP()) {
                noopTime = System.currentTimeMillis();
                frameNoopTime = cfg.getNoopTime() * 1000;
                while (cmd.isEmpty()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    if ((System.currentTimeMillis() - noopTime) > frameNoopTime) {
                        if (cfg.getIdleAction().equals("NOOP")) {
                            cmd.addElement("NOOP");
                        } else if (cfg.getIdleAction().equals("PWD")) {
                            cmd.addElement("PWD");
                        } else if (cfg.getIdleAction().equals("LIST")) {
                            cmd.addElement("list");
                        } else if (cfg.getIdleAction().equals("CWD .")) {
                            cmd.addElement("CWD .");
                        }
                    }
                }
                command = (String) cmd.firstElement();
                cmd.removeElementAt(0);
            } else {
                synchronized (cmd) {
                    if (cmd.isEmpty()) {
                        try {
                            cmd.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    command = (String) cmd.firstElement();
                    cmd.removeElementAt(0);
                }
            }
            aborted = false;
            cfg.setAbort(false);
            try {
                if (command.equals("quit")) {
                    quit(true);
                } else if (command.equals("connect")) {
                    proxy = false;
                    connect();
                } else if (command.startsWith("changeDir")) {
                    changeDir(command.substring(10, command.length()), true);
                } else if (command.equals("cdup")) {
                    cdup();
                } else if (command.startsWith("cmd")) {
                    doCmd(command.substring(4, command.length()));
                } else if (command.equals("list")) {
                    doList(false);
                } else if (command.equals("download")) {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    MyFile from = transfer.getSource();
                    MyFile to = transfer.getDest();
                    if (from.isDirectory()) {
                        panel.getFrame().getQueueList().removeFirst();
                        panel.getFrame().getQueueList().updateView();
                        if (changeDir(from.getAbsolutePath(), true)) {
                            Vector files = panel.getFiles();
                            if (files.size() > 0 || !cfg.getUseSkiplist() || !cfg.getSkipEmptyDir()) new File(to.getAbsolutePath()).mkdir(); else panel.getFrame().getStatusArea().append("Skipped " + from.getName() + "\n", "red");
                            for (int i = files.size() - 1; i >= 0; i--) {
                                MyFile tmp = new MyFile(((MyFile) files.elementAt(i)).getName());
                                tmp.setFtpMode(false);
                                tmp.setAbsolutePath(to.getAbsolutePath() + File.separator + ((MyFile) files.elementAt(i)).getName());
                                panel.getFrame().getQueueList().addAtBegin(new Transfer(((MyFile) files.elementAt(i)), tmp, panel.getMode(), panel.getOtherPanel().getMode(), panel.getPosition(), panel.getFtpSession().currentServer(), null));
                            }
                        }
                        panel.getFrame().getQueueList().updateView();
                    } else {
                        if (!panel.getDir().equals(from.getParent())) {
                            changeDir(from.getParent(), true);
                        }
                        if (!panel.getOtherPanel().getDir().equals(to.getParent())) {
                            panel.getOtherPanel().setDir(to.getParent());
                        }
                        this.size = from.getSize();
                        if (new File(to.getAbsolutePath()).exists()) {
                            to.setSize(new File(to.getAbsolutePath()).length());
                            transfer = new Transfer(from, to, panel.getMode(), panel.getOtherPanel().getMode(), panel.getPosition(), panel.getFtpSession().currentServer(), null);
                        }
                        rest = 0;
                        String[] fileExist = cfg.getFileExist();
                        if (to.getSize() > 0 && to.getSize() == from.getSize()) {
                            if (fileExist[3].equals("auto skip")) rest = -1; else if (fileExist[3].equals("auto overwrite")) rest = 0; else if (fileExist[3].equals("ask")) panel.newResumeDialog(transfer);
                        } else if (to.getSize() > 0 && to.getSize() > from.getSize()) {
                            if (fileExist[6].equals("auto skip")) rest = -1; else if (fileExist[6].equals("auto overwrite")) rest = 0; else if (fileExist[6].equals("ask")) panel.newResumeDialog(transfer);
                        } else if (to.getSize() > 0) {
                            if (fileExist[0].equals("auto skip")) rest = -1; else if (fileExist[0].equals("auto resume")) rest = to.getSize(); else if (fileExist[0].equals("auto overwrite")) rest = 0; else if (fileExist[0].equals("ask")) panel.newResumeDialog(transfer);
                        }
                        if (rest != -1) {
                            if (!transferMode.equals("BINARY")) {
                                doCmd("TYPE I");
                                transferMode = "BINARY";
                            }
                            if (download("RETR ", from, to) && !cfg.getAbort()) {
                                panel.getFrame().getQueueList().removeFirst();
                                panel.getFrame().getQueueList().updateView();
                                testSFV(to);
                            }
                        } else {
                            panel.getFrame().getStatusArea().append("Skipped " + from.getName() + "\n", "red");
                            panel.getFrame().getQueueList().removeFirst();
                            panel.getFrame().getQueueList().updateView();
                        }
                    }
                    rest = 0;
                    while (!wlFxp.getTm().waiting) {
                        Thread.sleep(10);
                    }
                    synchronized (wlFxp.getTm().done) {
                        wlFxp.getTm().done.notify();
                    }
                    Utilities.print("tm notified\n");
                } else if (command.equals("upload")) {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    MyFile from = transfer.getSource();
                    MyFile to = transfer.getDest();
                    if (from.isDirectory()) {
                        panel.getFrame().getQueueList().removeFirst();
                        panel.getFrame().getQueueList().updateView();
                        cache.remove(panel.getDir());
                        panel.getOtherPanel().setDir(from.getAbsolutePath());
                        MyFile[] files = from.list();
                        if (files.length > 0 || !cfg.getUseSkiplist() || !cfg.getSkipEmptyDir()) doCmd("MKD " + to.getAbsolutePath()); else panel.getFrame().getStatusArea().append("Skipped " + from.getName() + "\n", "red");
                        for (int i = files.length - 1; i >= 0; i--) {
                            MyFile tmp = new MyFile(files[i].getName());
                            tmp.setAbsolutePath(to.getAbsolutePath() + "/" + files[i].getName());
                            panel.getFrame().getQueueList().addAtBegin(new Transfer(files[i], tmp, panel.getOtherPanel().getMode(), panel.getMode(), panel.getOtherPanel().getPosition(), null, panel.getFtpSession().currentServer()));
                        }
                        panel.getFrame().getQueueList().updateView();
                    } else {
                        if (!panel.getDir().equals(to.getParent())) {
                            changeDir(to.getAbsolutePath().substring(0, to.getAbsolutePath().lastIndexOf("/")), false);
                        }
                        if (!panel.getOtherPanel().getDir().equals(from.getParent())) {
                            panel.getOtherPanel().setDir(from.getParent());
                        }
                        this.size = from.getSize();
                        Vector files = panel.getFiles();
                        for (int i = 0; i < files.size(); i++) {
                            if (to.getName().equals(((MyFile) files.elementAt(i)).getName())) {
                                to.setSize(((MyFile) files.elementAt(i)).getSize());
                                transfer = new Transfer(from, to, panel.getOtherPanel().getMode(), panel.getMode(), panel.getOtherPanel().getPosition(), null, panel.getFtpSession().currentServer());
                                break;
                            }
                        }
                        rest = 0;
                        String[] fileExist = cfg.getFileExist();
                        if (to.getSize() > 0 && to.getSize() == from.getSize()) {
                            if (fileExist[4].equals("auto skip")) rest = -1; else if (fileExist[4].equals("auto overwrite")) rest = 0; else if (fileExist[4].equals("ask")) panel.newResumeDialog(transfer);
                        } else if (to.getSize() > 0 && to.getSize() > from.getSize()) {
                            if (fileExist[7].equals("auto skip")) rest = -1; else if (fileExist[7].equals("auto overwrite")) rest = 0; else if (fileExist[7].equals("ask")) panel.newResumeDialog(transfer);
                        } else if (to.getSize() > 0) {
                            if (fileExist[1].equals("auto skip")) rest = -1; else if (fileExist[1].equals("auto resume")) rest = to.getSize(); else if (fileExist[1].equals("auto overwrite")) rest = 0; else if (fileExist[1].equals("ask")) panel.newResumeDialog(transfer);
                        }
                        if (rest != -1) {
                            if (!transferMode.equals("BINARY")) {
                                doCmd("TYPE I");
                                transferMode = "BINARY";
                            }
                            if (upload("STOR ", from, to) && !cfg.getAbort()) {
                                panel.getFrame().getQueueList().removeFirst();
                                panel.getFrame().getQueueList().updateView();
                                cache.remove(to.getParent());
                            }
                        } else {
                            panel.getFrame().getStatusArea().append("Skipped " + from.getName() + "\n", "red");
                            panel.getFrame().getQueueList().removeFirst();
                            panel.getFrame().getQueueList().updateView();
                        }
                    }
                    rest = 0;
                    while (!wlFxp.getTm().waiting) {
                        Thread.sleep(10);
                    }
                    synchronized (wlFxp.getTm().done) {
                        wlFxp.getTm().done.notify();
                    }
                } else if (command.startsWith("MKD")) {
                    doCmd(command);
                    doList(false);
                } else if (command.equals("fxp")) {
                    Transfer transfer = (Transfer) cmd.firstElement();
                    cmd.removeElementAt(0);
                    MyFile from = transfer.getSource();
                    MyFile to = transfer.getDest();
                    if (from.isDirectory()) {
                        panel.getFrame().getQueueList().removeFirst();
                        panel.getFrame().getQueueList().updateView();
                        panel.getOtherPanel().getFtpSession().getFtp().cache.remove(to.getParent());
                        changeDir(from.getAbsolutePath(), true);
                        Vector files = panel.getFiles();
                        if (files.size() > 0 || !cfg.getUseSkiplist() || !cfg.getSkipEmptyDir()) panel.getOtherPanel().getFtpSession().getFtp().doCmd("MKD " + to.getAbsolutePath()); else panel.getFrame().getStatusArea().append("Skipped " + from.getName() + "\n", "red");
                        for (int i = files.size() - 1; i >= 0; i--) {
                            MyFile tmp = new MyFile(((MyFile) files.elementAt(i)).getName());
                            tmp.setAbsolutePath(to.getAbsolutePath() + "/" + ((MyFile) files.elementAt(i)).getName());
                            panel.getFrame().getQueueList().addAtBegin(new Transfer(((MyFile) files.elementAt(i)), tmp, panel.getMode(), panel.getOtherPanel().getMode(), panel.getPosition(), panel.getFtpSession().currentServer(), panel.getOtherPanel().getFtpSession().currentServer()));
                        }
                        panel.getFrame().getQueueList().updateView();
                    } else {
                        if (!panel.getDir().equals(from.getParent())) {
                            changeDir(from.getParent(), true);
                        }
                        if (!panel.getOtherPanel().getDir().equals(to.getParent())) {
                            panel.getOtherPanel().getFtpSession().getFtp().changeDir(to.getParent(), false);
                        }
                        panel.getOtherPanel().getFtpSession().getFtp().size = from.getSize();
                        Vector files = panel.getOtherPanel().getFiles();
                        for (int i = 0; i < files.size(); i++) {
                            if (to.getName().equals(((MyFile) files.elementAt(i)).getName())) {
                                to.setSize(((MyFile) files.elementAt(i)).getSize());
                                transfer = new Transfer(from, to, panel.getMode(), panel.getOtherPanel().getMode(), panel.getPosition(), panel.getFtpSession().currentServer(), panel.getOtherPanel().getFtpSession().currentServer());
                                break;
                            }
                        }
                        rest = 0;
                        String[] fileExist = cfg.getFileExist();
                        if (to.getSize() > 0 && to.getSize() == from.getSize()) {
                            if (fileExist[5].equals("auto skip")) rest = -1; else if (fileExist[5].equals("auto overwrite")) rest = 0; else if (fileExist[5].equals("ask")) panel.newResumeDialog(transfer);
                        } else if (to.getSize() > 0 && to.getSize() > from.getSize()) {
                            if (fileExist[8].equals("auto skip")) rest = -1; else if (fileExist[8].equals("auto overwrite")) rest = 0; else if (fileExist[8].equals("ask")) panel.newResumeDialog(transfer);
                        } else if (to.getSize() > 0) {
                            if (fileExist[2].equals("auto skip")) rest = -1; else if (fileExist[2].equals("auto resume")) rest = to.getSize(); else if (fileExist[2].equals("auto overwrite")) rest = 0; else if (fileExist[2].equals("ask")) panel.newResumeDialog(transfer);
                        }
                        if (rest != -1) {
                            if (fxp(from, to, false) > 3) {
                                if (panel.getOtherPanel().getFtpSession().getFtp().fxp(from, to, true) > 3) {
                                    panel.getFrame().getStatusArea().append("transfer failed\n", "red");
                                }
                            }
                            panel.getFrame().getQueueList().removeFirst();
                            panel.getFrame().getQueueList().updateView();
                            panel.getOtherPanel().getFtpSession().getFtp().cache.remove(to.getParent());
                        } else {
                            panel.getFrame().getStatusArea().append("Skipped " + from.getName() + "\n", "red");
                            panel.getFrame().getQueueList().removeFirst();
                            panel.getFrame().getQueueList().updateView();
                        }
                    }
                    while (!wlFxp.getTm().waiting) {
                        Thread.sleep(10);
                    }
                    synchronized (wlFxp.getTm().done) {
                        wlFxp.getTm().done.notify();
                    }
                } else if (command.equals("delete")) {
                    Vector v = (Vector) cmd.firstElement();
                    cmd.removeElementAt(0);
                    while (!v.isEmpty() && !cfg.getAbort()) {
                        MyFile f = (MyFile) v.firstElement();
                        v.removeElementAt(0);
                        if (!f.isDirectory()) {
                            if (!panel.getDir().equals(f.getParent())) {
                                changeDir(f.getParent(), false);
                            }
                            if (doCmd("DELE " + f.getName()) < 4) panel.removeElement(f);
                        } else if (f.isDirectory()) {
                            if (recursiveDelete(f)) {
                                panel.removeElement(f);
                            }
                        }
                        cache.remove(f.getParent());
                    }
                    panel.lightUpdateView();
                } else {
                    doCmd(command);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                Utilities.saveStackTrace(e);
                synchronized (wlFxp.getTm().done) {
                    wlFxp.getTm().done.notify();
                }
                panel.getFrame().getStatusArea().append(e + "\n", "red");
                try {
                    sc.close();
                    quit(false);
                } catch (IOException ex) {
                }
                if (!quitting) {
                    panel.getFtpSession().setAbortReconnect(false);
                    if (cfg.getAutoreconnect() && !panel.getFtpSession().connected()) {
                        panel.getFrame().getStatusArea().append("connection lost.\nreconnecting in " + retrydelay + " secs.\n", "red");
                        try {
                            Thread.sleep(retrydelay * 1000);
                        } catch (InterruptedException ie) {
                        }
                        if (!panel.getFtpSession().connected() && !panel.getFtpSession().abortReconnect()) {
                            panel.getFtpSession().connect(panel.getFtpSession().currentServer());
                        }
                    }
                }
            }
            try {
                sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
	 * Adds a feature to the Command attribute of the Ftp object
	 * 
	 * @param command
	 *            The feature to be added to the Command attribute
	 */
    public void addCommand(String command) {
        synchronized (cmd) {
            cmd.addElement(command);
            cmd.notify();
        }
    }

    /**
	 * Adds a feature to the Transfer attribute of the Ftp object
	 * 
	 * @param command
	 *            The feature to be added to the Transfer attribute
	 * @param t
	 *            The feature to be added to the Transfer attribute
	 */
    public void addTransfer(String command, Transfer t) {
        synchronized (cmd) {
            cmd.addElement(command);
            cmd.addElement(t);
            cmd.notify();
        }
    }

    /**
	 * recursive delete a dir
	 * 
	 * @param dir
	 *            Description of the Parameter
	 * @return des
	 * @exception IOException
	 *                Description of the Exception
	 */
    protected boolean recursiveDelete(MyFile dir) throws IOException {
        if (cfg.getAbort()) {
            return false;
        }
        changeDir(dir.getAbsolutePath(), true);
        Vector files = panel.getFiles();
        MyFile ftpfile;
        while (!files.isEmpty() && !cfg.getAbort()) {
            ftpfile = (MyFile) files.firstElement();
            files.removeElementAt(0);
            if (!ftpfile.isDirectory()) {
                doCmd("DELE " + ftpfile.getName());
            } else {
                recursiveDelete(ftpfile);
            }
        }
        if (cfg.getAbort()) {
            return false;
        }
        changeDir(dir.getParent(), true);
        cache.remove(dir.getAbsolutePath());
        if (doCmd("RMD " + dir.getName()) < 4) return true;
        return false;
    }

    /**
	 * Description of the Method
	 * 
	 * @param useCache
	 *            Description of the Parameter
	 * @exception IOException
	 *                Description of Exception
	 */
    public void doList(boolean useCache) throws IOException {
        if (cfg.getCache() && useCache && cache.containsKey(panel.getDir())) {
            panel.updateFtpView(cache.get(panel.getDir()));
        } else {
            if (!transferMode.equals("ASCII")) {
                doCmd("TYPE A");
                transferMode = "ASCII";
            }
            panel.updateFtpView(list("LIST -aL"));
        }
    }

    /**
	 * connects to host and makes login, pwd
	 * 
	 * @exception Exception
	 *                Description of the Exception
	 */
    public void connect() throws Exception {
        panel.getFrame().getStatusArea().append("Connecting to: " + host + "\n", panel.getColor());
        connectToServer();
        if (!abortConnect && getReply() > 3) {
            quit(false);
            throw new Exception("Server REPLY");
        }
        if (!abortConnect && login() > 3) {
            quit(false);
            throw new Exception("LOGIN");
        }
        if (!abortConnect && SSL == 4) {
            doCmd("PBSZ 0");
            if (doCmd("PROT C") > 4) {
                PROTdata = true;
                PROTlist = true;
            }
        }
        if (!abortConnect) {
            getPwd();
        }
        if (!abortConnect) {
            if (!panel.getFtpSession().currentServer().getPath().equals("") && !panel.getDir().equals(panel.getFtpSession().currentServer().getPath())) {
                doCmd("CWD " + panel.getFtpSession().currentServer().getPath());
                getPwd();
            }
        }
    }

    /**
	 * is called by connect to make a new SocketChannel it also gets the
	 * localAddress for later
	 * 
	 * @exception IOException
	 *                Description of Exception
	 */
    public void connectToServer() throws IOException {
        if (cfg.isProxy()) {
            sc = (SSLSocketChannel) wlFxp.proxyConnect(host, port, true);
            proxy = true;
        } else {
            InetSocketAddress isa = new InetSocketAddress(InetAddress.getByName(host), port);
            sc = new SSLSocketChannel();
            sc.configureBlocking(false);
            sc.connect(isa);
            long start = System.currentTimeMillis();
            while (!sc.finishConnect() && !abortConnect) {
                if ((System.currentTimeMillis() - start) > timeout) throw new IOException("connect timed out");
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }
        }
        if (!abortConnect) {
            localAddress = sc.socket().getLocalAddress();
            wlFxp.getIdentd().setUserPort(user, sc.socket().getLocalPort() + "");
            gThread = new getReplyThread(panel, sc);
            connected = true;
        } else if (sc != null) {
            quit(false);
        }
    }

    /**
	 * sends the USER PASS sequence
	 * 
	 * @return Description of the Returned Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public int login() throws IOException {
        if (SSL > 0) {
            if (SSL == 3) {
                if (doCmd("AUTH TLS") > 3) SSL = 0;
            } else if (SSL == 1) {
                if (doCmd("AUTH SSL") > 3) SSL = 0;
            }
            gThread.wait = true;
            SSL = sc.tryTLS(SSL);
            gThread.wait = false;
        }
        String msg = "USER " + user;
        int ret;
        if ((ret = doCmd(msg)) > 3) {
            return ret;
        }
        if (ret == 2) {
            return 2;
        }
        String[] challenge = new String[2];
        int type;
        if ((type = testOTP(getReplyMultiLine, challenge)) > 0) {
            Utilities.print("Challenge is: " + challenge[0] + " " + challenge[1] + "\n");
            msg = "PASS " + jotp.computeOTP(challenge[0], challenge[1], pass, type);
        } else msg = "PASS " + pass;
        return doCmd(msg);
    }

    private int testOTP(String msg, String[] challenge) {
        String[] lines = Utilities.split(msg, "\r\n");
        for (int i = 0; i < lines.length; i++) {
            String[] t = Utilities.split(lines[i], " ");
            for (int k = 1; k < t.length; k++) {
                try {
                    if (Pattern.matches("[0-9]+", t[k])) {
                        challenge[0] = t[k];
                        challenge[1] = t[k + 1];
                        if (lines[i].indexOf("md4") != -1) return 4; else return 5;
                    }
                } catch (Exception e) {
                }
            }
        }
        return -1;
    }

    /**
	 * sends a command and calls getReply
	 * 
	 * @param cmd
	 *            the command without <CR>
	 * @return Description of the Returned Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public int doCmd(String cmd) throws IOException {
        gThread.ret = 0;
        if (cmd.startsWith("PASS")) {
            panel.getFrame().getStatusArea().append("PASS\n", "black");
        } else {
            panel.getFrame().getStatusArea().append(cmd + "\n", "black");
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
	 * sends the import IP Sync senquence
	 * 
	 * @exception IOException
	 *                Description of the Exception
	 */
    private void ipSync() throws IOException {
        if (SSL == 0) {
            byte[] b = new byte[2];
            b[0] = -1;
            b[1] = -12;
            cbuf.clear();
            cbuf.put((char) 255);
            cbuf.put((char) 244);
            cbuf.flip();
            sc.write(encoder.encode(cbuf));
            cbuf.clear();
            sc.sendUrgentData(sc.sc, 255);
            sc.sendUrgentData(sc.sc, 242);
        }
    }

    /**
	 * sends IpSync and ABOR
	 * 
	 * @exception IOException
	 *                Description of the Exception
	 */
    public void abort() throws IOException {
        cfg.setAbort(true);
        ipSync();
        send("ABOR");
        aborted = true;
    }

    /**
	 * Description of the Method
	 * 
	 * @param dir
	 *            Description of Parameter
	 * @param useCache 
	 * @return desc
	 * @exception IOException
	 *                Description of Exception
	 */
    public boolean changeDir(String dir, boolean useCache) throws IOException {
        int ret = doCmd("CWD " + dir);
        getPwd();
        doList(useCache);
        return ret == 2;
    }

    /**
	 * Description of the Method
	 * 
	 * @exception IOException
	 *                Description of Exception
	 */
    public void cdup() throws IOException {
        doCmd("CDUP");
        getPwd();
        doList(true);
    }

    /**
	 * does command "quit" and closes the SocketChannel if necessary it does a
	 * IpSync
	 * 
	 * @param block 
	 * 
	 * @exception IOException
	 *                Description of Exception
	 */
    public void quit(boolean block) throws IOException {
        quitting = true;
        Utilities.print("quitting...\n");
        connected = false;
        if (panel.getFtpSession().getFtp() == this) {
            panel.setFtpDir(" ");
            panel.updateFtpView("");
        }
        if (sc != null) {
            if (sc.isConnected()) {
                if (wlFxp.getTm() != null && wlFxp.getTm().waiting) {
                    ipSync();
                }
                try {
                    if (block) {
                        doCmd("QUIT");
                    } else {
                        send("QUIT");
                    }
                } catch (IOException e) {
                }
                if (gThread != null) {
                    gThread.quit = true;
                }
                sc.close();
                Utilities.print("socket closed: " + host + "\n");
            }
        }
    }

    /**
	 * Description of the Method
	 * 
	 * @param v
	 *            Description of the Parameter
	 */
    public void delete(Vector v) {
        synchronized (cmd) {
            cmd.addElement("delete");
            cmd.addElement(v);
            cmd.notify();
        }
    }

    private void configurePROT(boolean PROTx) throws IOException {
        if (SSL > 0 && PROTx) {
            if (doCmd("PROT P") > 3) {
                PROTdata = false;
                PROTlist = false;
            }
        } else if (SSL > 0 && !PROTx) {
            if (doCmd("PROT C") > 3) {
                PROTdata = true;
                PROTlist = true;
            }
        } else {
            PROTdata = false;
            PROTlist = false;
        }
    }

    /**
	 * lists the content of the current directory
	 * 
	 * @param command
	 *            Description of Parameter
	 * @return Description of the Returned Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public String list(String command) throws IOException {
        configurePROT(PROTlist);
        SSLSocketChannel sc = null;
        if (cfg.getPassive()) {
            sc = doPASV(command);
        } else {
            sc = acceptConnection(command);
        }
        if (sc == null) {
            return "";
        }
        if (PROTlist) sc.tryTLS(3);
        output.delete(0, output.length());
        int amount;
        dbuf.clear();
        long start = System.currentTimeMillis();
        CharBuffer cb;
        while ((amount = sc.read(dbuf)) != -1 && !aborted) {
            if (amount == 0) {
                if ((System.currentTimeMillis() - start) > timeout) {
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
            } else {
                start = System.currentTimeMillis();
            }
            dbuf.flip();
            cb = decoder.decode(dbuf);
            output.append(cb);
            dbuf.clear();
        }
        if (aborted) {
            output.delete(0, output.length());
            aborted = false;
            for (int i = 0; i < 10 && gThread.ret == 0; i++) {
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
        sc.close();
        gThread.ret = 0;
        cache.put(panel.getDir(), output.toString());
        if (Utilities.debug) Utilities.print(output.toString());
        return output.toString();
    }

    /**
	 * @return desc
	 */
    public StringBuffer getOutput() {
        if (cfg.getCache() && cache.containsKey(panel.getDir())) {
            return new StringBuffer(cache.get(panel.getDir()));
        }
        return output;
    }

    /**
	 * Description of the Method
	 * 
	 * @param command
	 *            Description of Parameter
	 * @param from
	 *            Description of the Parameter
	 * @param to
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public boolean download(String command, MyFile from, MyFile to) throws IOException {
        configurePROT(PROTdata);
        crc.reset();
        if (crcCache.containsKey(from.getName())) computeCRC = true; else computeCRC = false;
        SSLSocketChannel sc = null;
        boolean append = false;
        if (cfg.getPassive()) {
            sc = doPASV(command + from.getName());
        } else {
            sc = acceptConnection(command + from.getName());
        }
        Date d1 = new Date();
        if (sc == null) {
            return true;
        }
        if (PROTdata) sc.tryTLS(3);
        if (rest > 0) {
            append = true;
        }
        FileOutputStream fos = new FileOutputStream(panel.getOtherPanel().getDir() + File.separator + to.getName(), append);
        FileChannel fc = fos.getChannel();
        int amount;
        dbuf.clear();
        long size = rest;
        Date d3 = new Date();
        long diffSize = 0;
        long diffTime = 0;
        boolean ret = true;
        long start = System.currentTimeMillis();
        long elapsedStart = start;
        try {
            while ((amount = sc.read(dbuf)) != -1 && !aborted) {
                if (amount == 0) {
                    if ((System.currentTimeMillis() - start) > timeout) {
                        break;
                    }
                    try {
                        Thread.sleep(4);
                    } catch (InterruptedException e) {
                    }
                } else {
                    start = System.currentTimeMillis();
                }
                size += amount;
                diffSize += amount;
                if ((diffTime = System.currentTimeMillis() - d3.getTime()) > 1000) {
                    panel.getFrame().setStatusBar(from.getName(), from.getSize(), diffSize / (diffTime / 1000.0), size, (System.currentTimeMillis() - elapsedStart) / 1000);
                    String perf = Utilities.humanReadable(size) + "@" + Utilities.humanReadable(diffSize / (diffTime / 1000.0)) + "/s";
                    panel.getFrame().setTitle(perf);
                    diffSize = 0;
                    diffTime = 0;
                    d3 = new Date();
                }
                if (dbuf.remaining() > 0) {
                    continue;
                }
                dbuf.flip();
                fc.write(dbuf);
                crcUpdate();
                dbuf.clear();
            }
            dbuf.flip();
            fc.write(dbuf);
            crcUpdate();
            dbuf.clear();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            Utilities.saveStackTrace(e);
            ret = false;
        }
        if (aborted) {
            ret = false;
            aborted = false;
            for (int i = 0; i < 10 && gThread.ret == 0; i++) {
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
        if (size < from.getSize()) {
            ret = false;
        }
        Date d2 = new Date();
        panel.getFrame().setStatusBar("", 0, 0.0, 0, 0);
        panel.getFrame().setTitle("wlFxp");
        fc.force(true);
        fos.close();
        sc.close();
        if (computeCRC) {
            panel.getFrame().getStatusArea().append(to.getName() + " crc32: " + hexformat(crc.getValue(), 8), panel.getColor());
            if (crcCache.get(from.getName()).equals(hexformat(crc.getValue(), 8))) panel.getFrame().getStatusArea().append("checksum correct", panel.getColor()); else panel.getFrame().getStatusArea().append("checksum incorrect", "red");
        }
        gThread.ret = 0;
        getTransferRate(d2.getTime() - d1.getTime(), size - rest);
        return ret;
    }

    /**
	 * Description of the Method
	 * 
	 * @param command
	 *            Description of Parameter
	 * @param from
	 *            Description of the Parameter
	 * @param to
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public boolean upload(String command, MyFile from, MyFile to) throws IOException {
        SSLSocketChannel sc = null;
        configurePROT(PROTdata);
        if (cfg.getPassive()) {
            sc = doPASV(command + to.getName());
        } else {
            sc = acceptConnection(command + to.getName());
        }
        Date d1 = new Date();
        if (sc == null) {
            return true;
        }
        if (PROTdata) sc.tryTLS(3);
        FileInputStream fis = new FileInputStream(new File(panel.getOtherPanel().getDir() + File.separator + from.getName()));
        FileChannel fc = fis.getChannel();
        if (rest > 0) {
            fc.position(rest);
        }
        int amount;
        ubuf.clear();
        long size = rest;
        Date d3 = new Date();
        long diffSize = 0;
        long diffTime = 0;
        boolean ret = true;
        int s_amount = 0;
        long start = System.currentTimeMillis();
        long elapsedStart = start;
        int i;
        try {
            bigwhile: while ((amount = fc.read(ubuf)) != -1 && !aborted) {
                ubuf.flip();
                start = System.currentTimeMillis();
                i = 0;
                while ((i = sc.write(ubuf)) != -1 && !aborted) {
                    s_amount += i;
                    size += i;
                    diffSize += i;
                    if ((diffTime = System.currentTimeMillis() - d3.getTime()) > 1000) {
                        panel.getFrame().setStatusBar(from.getName(), from.getSize(), diffSize / (diffTime / 1000.0), size, (System.currentTimeMillis() - elapsedStart) / 1000);
                        String perf = Utilities.humanReadable(size) + "@" + Utilities.humanReadable(diffSize / (diffTime / 1000.0)) + "/s";
                        panel.getFrame().setTitle(perf);
                        diffSize = 0;
                        diffTime = 0;
                        d3 = new Date();
                    }
                    if (amount <= s_amount) {
                        break;
                    }
                    if (i == 0) {
                        if ((System.currentTimeMillis() - start) > timeout) {
                            break bigwhile;
                        } else {
                            try {
                                Thread.sleep(4);
                            } catch (InterruptedException e) {
                            }
                        }
                    } else {
                        start = System.currentTimeMillis();
                    }
                }
                if (i == -1) {
                    break;
                }
                s_amount = 0;
                ubuf.clear();
            }
        } catch (IOException e) {
            Utilities.saveStackTrace(e);
            ret = false;
        }
        if (aborted) {
            ret = false;
            for (int j = 0; j < 10 && gThread.ret == 0; j++) {
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
        if (size < from.getSize()) {
            ret = false;
        }
        panel.getFrame().setStatusBar("", 0, 0.0, 0, 0);
        panel.getFrame().setTitle("wlFxp");
        fis.close();
        sc.close();
        while (!aborted && gThread.ret == 0) {
            try {
                sleep(50);
            } catch (InterruptedException e) {
            }
        }
        gThread.ret = 0;
        aborted = false;
        Date d2 = new Date();
        getTransferRate(d2.getTime() - d1.getTime(), size - rest);
        return ret;
    }

    /**
	 * Description of the Method
	 * 
	 * @param s
	 *            Description of the Parameter
	 * @exception IOException
	 *                Description of the Exception
	 */
    public void send(String s) throws IOException {
        try {
            gThread.ret = 0;
            panel.getFrame().getStatusArea().append(s + "\n", "black");
            cbuf.clear();
            cbuf.put(s + "\r\n");
            cbuf.flip();
            sc.write(encoder.encode(cbuf));
            cbuf.clear();
            Utilities.print(s + "\r\n");
        } catch (NullPointerException e) {
        }
    }

    /**
	 * Description of the Method
	 * 
	 * @param from
	 *            Description of the Parameter
	 * @param to
	 *            Description of the Parameter
	 * @param alternative
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public int fxp(MyFile from, MyFile to, boolean alternative) throws IOException {
        if (!transferMode.equals("BINARY")) {
            doCmd("TYPE I");
            transferMode = "BINARY";
        }
        if (sendPRET("RETR " + from.getName()) > 3) return 5;
        if (doCmd("PASV") > 3) {
            return 5;
        }
        String output = getReplyMsg;
        if (output.indexOf("(") == -1 || output.indexOf(")") == -1) return 5;
        output = output.substring(output.indexOf("(") + 1, output.indexOf(")"));
        return panel.getOtherPanel().getFtpSession().getFtp().receivefxp(output, from, to, alternative);
    }

    /**
	 * Description of the Method
	 * 
	 * @param output
	 *            Description of Parameter
	 * @param from
	 *            Description of the Parameter
	 * @param to
	 *            Description of the Parameter
	 * @param alternative
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public int receivefxp(String output, MyFile from, MyFile to, boolean alternative) throws IOException {
        if (!transferMode.equals("BINARY")) {
            doCmd("TYPE I");
            transferMode = "BINARY";
        }
        if (doCmd("PORT " + output) > 3) {
            return 5;
        }
        if (panel.getOtherPanel().getFtpSession().getFtp().rest > 0) {
            panel.getOtherPanel().getFtpSession().getFtp().doCmd("REST " + panel.getOtherPanel().getFtpSession().getFtp().rest);
            doCmd("REST " + panel.getOtherPanel().getFtpSession().getFtp().rest);
        }
        if (alternative) {
            if (doCmd("RETR " + from.getName()) < 4) {
                if (panel.getOtherPanel().getFtpSession().getFtp().doCmd("STOR " + to.getName()) < 4) {
                    while (gThread.ret == 0 && !cfg.getAbort()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (cfg.getAbort() && !aborted) {
                        abort();
                    }
                }
                gThread.ret = 0;
            }
        } else {
            Date d1 = new Date();
            if (doCmd("STOR " + to.getName()) < 4) {
                if (panel.getOtherPanel().getFtpSession().getFtp().doCmd("RETR " + from.getName()) < 4) {
                    while (gThread.ret == 0 && !cfg.getAbort()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (cfg.getAbort() && !aborted) {
                        abort();
                    }
                    Date d2 = new Date();
                    getTransferRate(d2.getTime() - d1.getTime(), size - rest);
                } else {
                    return 5;
                }
                gThread.ret = 0;
            } else {
                return 5;
            }
        }
        return 0;
    }

    /**
	 * gets a SocketChannel for a datatransfer after some command is send
	 * 
	 * @param cmd
	 *            the command that needs a dataport
	 * @return Description of the Returned Value
	 * @exception IOException
	 *                Description of Exception
	 */
    public SSLSocketChannel acceptConnection(String cmd) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress isa = new InetSocketAddress(localAddress, 0);
        InetAddress i;
        int port;
        SSLSocketChannel sc = null;
        if (proxy) {
            sc = wlFxp.proxyBind(isa);
            i = wlFxp.getProxyInetAddress();
            port = wlFxp.getProxyBindPort();
        } else {
            ssc.socket().bind(isa);
            i = ssc.socket().getInetAddress();
            port = ssc.socket().getLocalPort();
        }
        port(i, port);
        if (rest > 0) {
            if (doCmd("REST " + rest) > 3) {
                rest = 0;
            }
        }
        if (doCmd(cmd) > 3) {
            return null;
        }
        if (!proxy) {
            sc = new SSLSocketChannel(ssc.accept());
            while (!sc.finishConnect() && !abortConnect) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }
        }
        sc.socket().setReceiveBufferSize(65536);
        sc.socket().setSendBufferSize(65536);
        return sc;
    }

    /**
	 * sends a port command
	 * 
	 * @param inetaddress
	 *            Description of the Parameter
	 * @param localport
	 *            Description of the Parameter
	 * @exception IOException
	 *                Description of Exception
	 */
    public void port(InetAddress inetaddress, int localport) throws IOException {
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
	 * sends a PASV command and returns a InetSocketAddress that you can connect
	 * to
	 * 
	 * @param command
	 *            Description of Parameter
	 * @return the InetSocketAddress which you can build a SocketChannel from.
	 * @exception IOException
	 *                Description of Exception
	 */
    public SSLSocketChannel doPASV(String command) throws IOException {
        InetSocketAddress isa = null;
        SSLSocketChannel sc = null;
        dbuf.clear();
        try {
            sendPRET(command);
            doCmd("PASV");
            String output = getReplyMsg;
            while (output.indexOf(")") == -1) {
                getReply();
                output = getReplyMsg;
            }
            if (output.indexOf(",") == -1) return null;
            output = output.substring(output.indexOf("(") + 1, output.indexOf(")"));
            String[] parts = Utilities.split(output, ",");
            String ip = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
            int port = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]);
            if (rest > 0) {
                if (doCmd("REST " + rest) > 3) {
                    rest = 0;
                }
            }
            send(command);
            if (proxy) {
                sc = (SSLSocketChannel) wlFxp.proxyConnect(host, port, true);
            } else {
                isa = new InetSocketAddress(InetAddress.getByName(ip), port);
                sc = new SSLSocketChannel();
                sc.socket().setReceiveBufferSize(65536);
                sc.socket().setSendBufferSize(65536);
                sc.configureBlocking(false);
                sc.connect(isa);
                while (!sc.finishConnect() && !abortConnect) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (getReply() > 3) {
                sc = null;
            }
            return sc;
        } catch (CharacterCodingException e) {
            panel.getFrame().getStatusArea().append(e + "\n", panel.getColor());
        }
        return sc;
    }

    private int sendPRET(String command) throws IOException {
        if (this.isPRETCapable) {
            int reply = doCmd("PRET " + command);
            if (command.startsWith("LIST") && reply > 3) {
                this.isPRETCapable = false;
            }
            return reply;
        }
        return 3;
    }

    private void updateChecksum(byte[] buffer, int off, int len) {
        crc.update(buffer, off, len);
    }

    /**
	 * @param value
	 * @param nibbles
	 * @return desc
	 */
    public String hexformat(long value, int nibbles) {
        StringBuffer sb = new StringBuffer(Long.toHexString(value));
        while (sb.length() < nibbles) sb.insert(0, '0');
        return sb.toString();
    }

    private void crcUpdate() {
        if (computeCRC) {
            int crcLength = dbuf.position();
            dbuf.flip();
            dbuf.get(crcbuffer, 0, crcLength);
            updateChecksum(crcbuffer, 0, crcLength);
        }
    }

    private void testSFV(MyFile f) {
        if (Pattern.matches(".*.sfv", f.getName().toLowerCase())) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(";")) {
                        String[] t = Utilities.split(line, " ");
                        if (t.length > 1) {
                            crcCache.put(t[0], t[1].toLowerCase());
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }
}

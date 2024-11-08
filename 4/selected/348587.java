package obix.ui;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import obix.*;
import obix.io.*;
import obix.net.*;

/**
 * UiSession subclasses ObixSession to manage each session to
 * to a authority uri which has been opened by the application.
 * It provides UI specific functionality such as loading icons.
 *
 * @author    Brian Frank
 * @creation  14 Sept 05
 * @version   $Revision$ $Date$
 */
public class UiSession extends ObixSession {

    /**
   * List active sessions.
   */
    public static UiSession[] list() {
        return (UiSession[]) sessions.values().toArray(new UiSession[sessions.size()]);
    }

    /**
   * Get or create a UiSession for the specified uri.  Each
   * session manages the uri space of one authority such
   * as "http://foo/".  If we don't have an existing session
   * for the uri's authority, then we prompt the user to
   * create a new one (lobby, username, password).  Return
   * null if no session available (user cancels out).
   */
    public static UiSession make(Shell shell, Uri uri) throws Exception {
        uri.checkAbsolute();
        String auth = uri.getAuthority();
        UiSession session = (UiSession) sessions.get(auth);
        if (session != null) return session;
        Ask ask = new Ask();
        ask.lobby = uri.getAuthority() + "obix/";
        ask = (Ask) Form.prompt(shell.getRootPane(), "Open New Session", ask);
        if (ask == null) return null;
        session = new UiSession(new Uri(ask.lobby), ask.userName, ask.password);
        session.open();
        sessions.put(auth, session);
        shell.sessionCreated(session);
        return session;
    }

    static HashMap sessions = new HashMap();

    static class Ask {

        public String lobby = "";

        public String userName = "admin";

        public String password = "";
    }

    private UiSession(Uri lobby, String user, String pass) {
        super(lobby, user, pass);
    }

    /**
   * Open a session.
   */
    public void open() throws Exception {
        super.open();
        iconLoader = new IconLoader();
        iconLoader.start();
    }

    /**
   * Close a session.
   */
    public void close() {
        super.close();
        iconLoader.kill();
        iconLoader = null;
    }

    public Response request(Uri uri) throws Exception {
        Response resp = new Response();
        resp.session = this;
        InputStream in = open(uri);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) >= 0) bout.write(c);
        in.close();
        byte[] buf = bout.toByteArray();
        StringBuffer source = new StringBuffer(buf.length);
        for (int i = 0; i < buf.length; ++i) {
            c = buf[i];
            if (c == '\n' || (' ' <= c && c <= 0x7F)) source.append((char) c); else source.append('.');
        }
        resp.source = source.toString();
        try {
            resp.obj = new ObixDecoder(new ByteArrayInputStream(buf)).decodeDocument();
        } catch (Throwable e) {
            resp.objError = e;
        }
        return resp;
    }

    public Obj invoke(Shell shell, Op op) throws Exception {
        Uri href = op.getNormalizedHref();
        if (href == null) throw new Exception("Missing href for op");
        Obj in = getInvokeIn(op);
        if (in != null) {
            in.setWritable(true, true);
            in = ObjSheet.prompt(shell, "Invoke " + op.getName(), this, in);
            if (in == null) return null;
        } else {
            in = new Obj("in");
        }
        return invoke(href, in);
    }

    /**
   * Get the input object for the specific operation. 
   * Return null if the empty obj should be used.
   */
    public Obj getInvokeIn(Op op) throws Exception {
        Contract in = op.getIn();
        if (in.containsOnlyObj()) return null;
        System.out.println("getInvokeIn: " + in);
        Uri inUri = in.primary().normalize(op.getNormalizedHref());
        return read(inUri);
    }

    /**
   * Load an icon for the specified obj.  If it explicitly
   * provides an icon then use it.  Otherwise, take a look
   * at the contract list to see if we can pick a sensible
   * predefined one.
   */
    public Icon loadIcon(Obj obj) {
        ImageIcon def = getDefaultIcon(obj);
        Uri href = obj.getIcon();
        if (href == null) return def;
        return loadIcon(href, def);
    }

    /**
   * Map the specified obj to a default icon.
   */
    public ImageIcon getDefaultIcon(Obj obj) {
        if (obj instanceof Op) return iconOp;
        if (obj.getIs() == null) return iconDefault;
        Uri[] contracts = obj.getIs().list();
        for (int i = 0; i < contracts.length; ++i) {
            String c = contracts[i].toString();
            if (c.equals("obix:About")) return iconInfo;
            if (c.equals("obix:WatchService")) return iconGlasses;
            if (c.equals("obix:Alarm")) return iconAlarm;
            if (c.equals("obix:History")) return iconHistory;
        }
        return iconDefault;
    }

    /**
   * Load the specified icon.  If it has already been loaded
   * then return it.  Otherwise return immediately and do
   * the actual network loading asynchronously.
   */
    public Icon loadIcon(Uri href, ImageIcon def) {
        if (href == null) return def;
        synchronized (iconLock) {
            String key = href.toString();
            RemoteIcon icon = (RemoteIcon) iconCache.get(key);
            if (icon != null) return icon;
            icon = new RemoteIcon(href, def);
            iconCache.put(key, icon);
            iconQueue.add(icon);
            iconLock.notifyAll();
            return icon;
        }
    }

    /**
   * RemoteIcon is used to synchronously return an Icon
   * which may be used by the UI, but handles asynchronously
   * loading the image from the session.
   */
    static class RemoteIcon implements Icon {

        RemoteIcon(Uri href, ImageIcon def) {
            this.href = href;
            this.image = def.getImage();
        }

        public int getIconHeight() {
            return h;
        }

        public int getIconWidth() {
            return w;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (image != null) g.drawImage(image, x, y, c);
        }

        int w = 16, h = 16;

        Image image;

        Uri href;
    }

    /**
   * This is the thread which processes the icon queue
   * and loads images into RemoteIcons.
   */
    class IconLoader extends Thread {

        IconLoader() {
            super("IconLoader");
        }

        public void kill() {
            alive = false;
            interrupt();
        }

        public void run() {
            while (alive) {
                try {
                    RemoteIcon icon = null;
                    synchronized (iconLock) {
                        while (iconQueue.size() == 0) iconLock.wait();
                        icon = (RemoteIcon) iconQueue.remove(0);
                    }
                    InputStream in = open(icon.href);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    while (true) {
                        int n = in.read(buf);
                        if (n < 0) break;
                        out.write(buf, 0, n);
                    }
                    byte[] data = out.toByteArray();
                    Image image = Toolkit.getDefaultToolkit().createImage(data);
                    if (image == null) continue;
                    icon.image = Utils.sync(image);
                    icon.w = image.getWidth(null);
                    icon.h = image.getHeight(null);
                    Shell.theShell.repaint();
                } catch (Exception e) {
                    if (!alive) break;
                    e.printStackTrace();
                }
            }
        }

        boolean alive = true;
    }

    /**
   * Response packages up both the raw text returned from
   * a read operation, plus the decoded obj (if valid).  It
   * let's us display the various tabs in ViewObj.
   */
    public static class Response {

        public UiSession session;

        public String source;

        public Obj obj;

        public Throwable objError;
    }

    static final ImageIcon iconDefault = Utils.icon("x16/object.png");

    static final ImageIcon iconOp = Utils.icon("x16/exclaim.png");

    static final ImageIcon iconInfo = Utils.icon("x16/info.png");

    static final ImageIcon iconGlasses = Utils.icon("x16/glasses.png");

    static final ImageIcon iconHistory = Utils.icon("x16/history.png");

    static final ImageIcon iconAlarm = Utils.icon("x16/alarm.png");

    Object iconLock = new Object();

    IconLoader iconLoader = new IconLoader();

    HashMap iconCache = new HashMap();

    ArrayList iconQueue = new ArrayList();
}

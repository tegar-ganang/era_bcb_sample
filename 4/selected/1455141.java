package org.armedbear.j.mail;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import org.armedbear.j.Debug;
import org.armedbear.j.Editor;
import org.armedbear.j.FastStringBuffer;
import org.armedbear.j.Log;
import org.armedbear.j.Netrc;
import org.armedbear.j.SocketConnection;
import org.armedbear.j.Utilities;

public final class ImapSession {

    private static final int DISCONNECTED = 0;

    private static final int NONAUTHENTICATED = 1;

    private static final int AUTHENTICATED = 2;

    private static final int SELECTED = 3;

    public static final int OK = 0;

    public static final int NO = 1;

    public static final int BAD = 2;

    public static final int PREAUTH = 3;

    public static final int BYE = 4;

    public static final int UNKNOWN = -1;

    private final ImapURL url;

    private final String user;

    private final String password;

    private String tunnelHost;

    private int tunnelPort = -1;

    private ImapMailbox mailbox;

    private int state;

    private boolean echo;

    private Socket socket;

    private MailReader reader;

    private OutputStreamWriter writer;

    private String folderName;

    private boolean readOnly;

    private int messageCount;

    private int recent;

    private int uidValidity;

    private int uidNext;

    private String errorText;

    private long lastErrorMillis;

    private ImapSession(ImapURL url, String user, String password) {
        this.url = url;
        this.folderName = url.getFolderName();
        this.echo = url.isDebug();
        this.user = user;
        this.password = password;
    }

    public final void setMailbox(ImapMailbox mb) {
        if (mailbox != null) Debug.bug();
        mailbox = mb;
    }

    public final boolean isReadOnly() {
        return readOnly;
    }

    public final String getHost() {
        return url.getHost();
    }

    public final int getPort() {
        return url.getPort();
    }

    public final String getUser() {
        return user;
    }

    public final String getFolderName() {
        return folderName;
    }

    public final int getMessageCount() {
        return messageCount;
    }

    public final int getRecent() {
        return recent;
    }

    public final int getUidNext() {
        return uidNext;
    }

    public final int getUidValidity() {
        return uidValidity;
    }

    public final String getErrorText() {
        return errorText;
    }

    public void setTunnel(String tunnel) {
        if (tunnel != null) {
            tunnel = tunnel.trim();
            int colon = tunnel.indexOf(':');
            if (colon > 0) {
                tunnelHost = tunnel.substring(0, colon);
                try {
                    tunnelPort = Integer.parseInt(tunnel.substring(colon + 1).trim());
                } catch (NumberFormatException e) {
                    Log.error(e);
                    tunnelHost = null;
                    tunnelPort = -1;
                }
            }
        }
        Log.debug("setTunnel host = |" + tunnelHost + "| port = " + tunnelPort);
    }

    public final synchronized long getLastErrorMillis() {
        return lastErrorMillis;
    }

    private final synchronized void setLastErrorMillis(long millis) {
        Log.debug("setLastErrorMillis");
        lastErrorMillis = millis;
    }

    public static ImapSession getSession(ImapURL url) {
        if (url.getHost() == null || url.getFolderName() == null) return null;
        String user = url.getUser();
        if (user == null || user.length() == 0) user = System.getProperty("user.name");
        return getSession(url, user);
    }

    public static ImapSession getSession(ImapURL url, String user) {
        String password = Netrc.getPassword(url.getHost(), user);
        if (password == null) return null;
        return new ImapSession(url, user, password);
    }

    public static ImapSession getSession(ImapURL url, String user, String password) {
        return new ImapSession(url, user, password);
    }

    public boolean verifyConnected() {
        if (state != DISCONNECTED) {
            if (writeTagged("noop")) {
                if (getResponse() == OK) return true;
            }
        }
        return connect();
    }

    public boolean verifySelected(String folderName) {
        if (state == SELECTED && this.folderName.equals(folderName)) return true;
        return reselect(folderName);
    }

    private boolean connect() {
        socket = null;
        errorText = null;
        final String h;
        final int p;
        final boolean ssl;
        if (tunnelHost != null && tunnelPort > 0) {
            h = tunnelHost;
            p = tunnelPort;
            ssl = p == ImapURL.DEFAULT_SSL_PORT;
            Log.debug("connect using tunnel h = " + h + " p = " + p);
        } else {
            h = getHost();
            p = getPort();
            ssl = url.isSSL();
        }
        SocketConnection sc = new SocketConnection(h, p, ssl, 30000, 200, null);
        Log.debug("connecting to " + h + " on port " + p);
        socket = sc.connect();
        if (socket == null) {
            errorText = sc.getErrorText();
            Log.error(errorText);
            return false;
        }
        Log.debug("connected to " + h);
        boolean succeeded = false;
        boolean oldEcho = echo;
        if (Editor.isDebugEnabled()) echo = true;
        try {
            reader = new MailReader(socket.getInputStream());
            writer = new OutputStreamWriter(socket.getOutputStream(), "iso-8859-1");
            if (readLine() != null) {
                writeTagged("login " + user + " " + password);
                if (getResponse() == OK) {
                    state = AUTHENTICATED;
                    succeeded = true;
                }
            }
        } catch (IOException e) {
            Log.error(e);
        } finally {
            echo = oldEcho;
        }
        return succeeded;
    }

    private static final String UIDVALIDITY = "* OK [UIDVALIDITY ";

    private static final String UIDNEXT = "* OK [UIDNEXT ";

    public boolean reselect(String folderName) {
        long start = System.currentTimeMillis();
        boolean oldEcho = echo;
        if (Editor.isDebugEnabled()) echo = true;
        try {
            if (state < AUTHENTICATED || !writeTagged("select \"" + folderName + "\"")) {
                connect();
                if (state < AUTHENTICATED) return false;
                if (!writeTagged("select \"" + folderName + "\"")) return false;
            }
            while (true) {
                String s = readLine();
                if (s == null) {
                    Log.error("ImapSession.reselect readLine returned null");
                    this.folderName = null;
                    messageCount = 0;
                    recent = 0;
                    return false;
                }
                final String upper = s.toUpperCase();
                if (upper.startsWith("* NO ")) {
                    mailbox.setStatusText(s.substring(5).trim());
                    continue;
                }
                if (upper.startsWith("* ")) {
                    if (upper.endsWith(" EXISTS")) {
                        processUntaggedResponse(s);
                        continue;
                    }
                    if (upper.endsWith(" RECENT")) {
                        processUntaggedResponse(s);
                        continue;
                    }
                }
                if (upper.startsWith(UIDVALIDITY)) {
                    uidValidity = Utilities.parseInt(s.substring(UIDVALIDITY.length()));
                    continue;
                }
                if (upper.startsWith(UIDNEXT)) {
                    uidNext = Utilities.parseInt(s.substring(UIDNEXT.length()));
                    continue;
                }
                if (upper.startsWith(lastTag + " ")) {
                    if (upper.startsWith(lastTag + " OK ")) {
                        state = SELECTED;
                        this.folderName = folderName;
                        readOnly = upper.indexOf("[READ-ONLY]") >= 0;
                        if (readOnly) {
                            Log.warn("reselect mailbox " + folderName + " is read-only!");
                            setLastErrorMillis(System.currentTimeMillis());
                        } else {
                            Log.debug("reselect mailbox " + folderName + " is read-write");
                        }
                        return true;
                    } else {
                        Log.error("SELECT " + folderName + " failed");
                        state = AUTHENTICATED;
                        this.folderName = null;
                        messageCount = 0;
                        recent = 0;
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.error(e);
            disconnect();
            this.folderName = null;
            messageCount = 0;
            recent = 0;
            return false;
        } finally {
            echo = oldEcho;
            long elapsed = System.currentTimeMillis() - start;
            Log.debug("ImapSession.reselect " + folderName + " " + elapsed + " ms");
        }
    }

    public boolean close() {
        if (state != SELECTED) {
            Log.debug("already closed");
            return true;
        }
        if (writeTagged("close") && getResponse() == OK) state = AUTHENTICATED;
        folderName = null;
        messageCount = 0;
        recent = 0;
        return true;
    }

    public void logout() {
        Log.debug("ImapSession.logout " + getHost());
        if (state > DISCONNECTED) {
            if (writeTagged("logout")) getResponse();
            if (state > DISCONNECTED) disconnect();
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.error(e);
            }
            socket = null;
            reader = null;
            writer = null;
        }
        state = DISCONNECTED;
    }

    public String readLine() {
        if (reader == null) return null;
        try {
            String s = reader.readLine();
            if (s != null) {
                if (echo) Log.debug("<== " + s);
                if (s.startsWith(lastTag + " ")) errorText = getTaggedResponseText(s);
            }
            return s;
        } catch (InterruptedIOException e) {
            Log.error(e);
            setLastErrorMillis(System.currentTimeMillis());
            disconnect();
            return null;
        } catch (SocketException e) {
            disconnect();
            return null;
        } catch (IOException e) {
            Log.error(e);
            setLastErrorMillis(System.currentTimeMillis());
            disconnect();
            return null;
        }
    }

    public void uidStore(int uid, String arg) {
        FastStringBuffer sb = new FastStringBuffer("uid store ");
        sb.append(uid);
        sb.append(' ');
        sb.append(arg);
        writeTagged(sb.toString());
    }

    public void uidStore(String messageSet, String arg) {
        FastStringBuffer sb = new FastStringBuffer("uid store ");
        sb.append(messageSet);
        sb.append(' ');
        sb.append(arg);
        writeTagged(sb.toString());
    }

    public boolean writeTagged(String s) {
        if (writer == null) return false;
        int index = s.indexOf(' ');
        final String lastCommand = index >= 0 ? s.substring(0, index) : s;
        s = nextTag() + " " + s;
        if (echo) {
            if (lastCommand.equalsIgnoreCase("login")) {
                index = s.lastIndexOf(' ');
                if (index >= 0) Log.debug("==> " + s.substring(0, index)); else Log.debug("==> " + s);
            } else Log.debug("==> " + s);
        }
        try {
            writer.write(s.concat("\r\n"));
            writer.flush();
            return true;
        } catch (IOException e) {
            Log.error(e);
            disconnect();
            return false;
        }
    }

    public int getResponse() {
        while (true) {
            String s = readLine();
            if (s == null) return BYE;
            String upper = s.toUpperCase();
            int index = upper.indexOf("[ALERT]");
            if (index >= 0) mailbox.setAlertText(s.substring(index + 7).trim());
            if (upper.startsWith("* BYE ")) {
                Log.debug("getResponse |" + s + "|");
                disconnect();
                return BYE;
            }
            if (upper.startsWith(lastTag + " ")) {
                upper = upper.substring(lastTag.length() + 1);
                if (upper.startsWith("OK ")) return OK;
                if (upper.startsWith("NO ")) {
                    mailbox.setStatusText(s.substring(3).trim());
                    return NO;
                }
                if (upper.startsWith("BAD ")) return BAD;
                if (upper.startsWith("PREAUTH")) return PREAUTH;
                if (upper.startsWith("BYE")) {
                    disconnect();
                    return BYE;
                }
                return UNKNOWN;
            }
            processUntaggedResponse(s);
        }
    }

    private void processUntaggedResponse(String s) {
        Log.debug("processUntaggedResponse |" + s + "|");
        if (s.startsWith("* ")) {
            final String upper = s.toUpperCase();
            if (upper.endsWith(" EXISTS")) {
                try {
                    messageCount = Integer.parseInt(upper.substring(2, upper.length() - 7));
                    Log.debug("messageCount = " + messageCount);
                } catch (NumberFormatException e) {
                    Log.error(e);
                }
            } else if (upper.endsWith(" RECENT")) {
                try {
                    recent = Integer.parseInt(upper.substring(2, upper.length() - 7));
                    Log.debug("recent = " + recent);
                } catch (NumberFormatException e) {
                    Log.error(e);
                }
            } else if (upper.endsWith(" EXPUNGE")) {
                try {
                    int messageNumber = Integer.parseInt(upper.substring(2, upper.length() - 8));
                    if (messageCount > 0) {
                        --messageCount;
                        Log.debug("EXPUNGE messageCount = " + messageCount);
                    } else Log.error("received untagged EXPUNGE response with messageCount = " + messageCount);
                    mailbox.messageExpunged(messageNumber);
                } catch (NumberFormatException e) {
                    Log.error(e);
                }
            }
        }
    }

    private static String getTaggedResponseText(String taggedResponse) {
        int index = taggedResponse.indexOf(' ');
        if (index < 0) return null;
        index = taggedResponse.indexOf(' ', index + 1);
        if (index < 0) return null;
        return taggedResponse.substring(index + 1);
    }

    private int tagNumber;

    private String lastTag;

    public final String lastTag() {
        return lastTag;
    }

    private final String nextTag() {
        return lastTag = "A".concat(String.valueOf(++tagNumber));
    }

    public final void setEcho(boolean b) {
        echo = b;
    }

    protected void finalize() throws Throwable {
        Log.debug("ImapSession.finalize " + getHost());
        super.finalize();
    }
}

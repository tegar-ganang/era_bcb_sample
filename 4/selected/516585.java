package org.jmule.core.protocol.cli;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;
import org.jmule.core.*;
import org.jmule.core.internalCommunications.DirectComClient;
import org.jmule.util.*;
import org.jmule.ui.sacli.CommandLineInterface;

/** FIXME: class have to get a javadoc or die
 * @author casper
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:13 $
 */
public class CliConnection implements Connection {

    static Logger log = Logger.getLogger(CliConnection.class.getName());

    CommandLineInterface cli;

    public CliConnection(SocketChannel sc) {
        cli = new CommandLineInterface(new DirectComClient());
        outLines = new LinkedList();
        inLines = new LinkedList();
        this.channel = sc;
        log.fine("New remote command line client.");
        outLines.add("jMule (c) by the jMule Group (www.jmule.org)\n");
        outLines.add(prompt);
    }

    /** @see org.jmule.core.Connection#hasInput() */
    public boolean hasInput() {
        return (inLines.size() > 0);
    }

    /** @see org.jmule.core.Connection#hasOutput() */
    public boolean hasOutput() {
        return ((outLines.size() > 0) | hasOutBuffer);
    }

    /** @see org.jmule.core.Connection#check(int) */
    public boolean check(int count) {
        if (hasInput()) {
            if (cli.processCommand((String) inLines.removeFirst(), outLines)) {
                outLines.add(prompt);
            } else close();
        }
        if (hasOutput()) {
            if (!writeSelected) {
                log.finest("cli: DEBUG: Registering for WRITE.");
                selectionKey.interestOps(SelectionKey.OP_WRITE);
                writeSelected = true;
            }
        } else {
            if (writeSelected) {
                log.finest("cli: DEBUG: Registering for READ.");
                selectionKey.interestOps(SelectionKey.OP_READ);
                writeSelected = false;
            }
        }
        if (getChannel().socket().isClosed()) {
            System.out.println("Socket closed.");
            return (false);
        }
        return (!(doClose && (!hasOutput())));
    }

    /** @see org.jmule.core.Connection#isConnected() */
    public boolean isConnected() {
        return true;
    }

    /** @see org.jmule.core.Connection#setConnected(boolean) */
    public void setConnected(boolean connected) {
    }

    /** @see org.jmule.core.Connection#doClose() */
    public boolean doClose() {
        return doClose;
    }

    /** @see org.jmule.core.Connection#close() */
    public void close() {
        try {
            getChannel().close();
        } catch (IOException e) {
        }
        doClose = true;
    }

    /** @see org.jmule.core.Connection#getConnectTimeoutAt() */
    public int getConnectTimeoutAt() {
        return 0;
    }

    /** @see org.jmule.core.Connection#getLastActivity() */
    public long getLastActivity() {
        return 0;
    }

    /** @see org.jmule.core.Connection#setLastActivity(long) */
    public void setLastActivity(long lastActivity) {
    }

    /** @see org.jmule.core.Connection#getSelectionKey() */
    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    /** @see org.jmule.core.Connection#setSelectionKey(SelectionKey) */
    public void setSelectionKey(SelectionKey key) {
        this.selectionKey = key;
        if (hasOutput()) {
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private ByteBuffer inBytes = ByteBuffer.allocate(240);

    public boolean processInput() throws IOException {
        inBytes.clear();
        if (channel.read(inBytes) == -1) {
            return false;
        }
        addReceivedBytesNum(inBytes.position());
        inBytes.flip();
        while (inBytes.hasRemaining()) {
            if (actInLine == null) {
                actInLine = new StringBuffer();
            }
            byte inByte = inBytes.get();
            if (inByte == 10 || inByte == 13) {
                if (actInLine.length() > 0) {
                    inLines.add(actInLine.toString());
                    actInLine = null;
                }
            } else {
                actInLine.append((char) inByte);
            }
        }
        return true;
    }

    /** @see org.jmule.core.Connection#addReceivedBytesNum(int) */
    public void addReceivedBytesNum(int nbytes) {
    }

    /** @see org.jmule.core.Connection#addSentBytesNum(int) */
    public void addSentBytesNum(int nbytes) {
    }

    /** @see org.jmule.core.Connection#getChannel() */
    public SocketChannel getChannel() {
        return channel;
    }

    /** @see org.jmule.core.Connection#setChannel(SocketChannel) */
    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    /** @see org.jmule.core.Connection#getPeerAddress() */
    public InetSocketAddress getPeerAddress() {
        return null;
    }

    /** @see org.jmule.core.Connection#setPeerAddress(InetSocketAddress) */
    public void setPeerAddress(InetSocketAddress address) {
        this.address = address;
    }

    /** @see org.jmule.core.Connection#getConnectionNumber() */
    public int getConnectionNumber() {
        return 0;
    }

    /** processes output lines */
    public synchronized void processOutput() {
        LogUtil.entering(log);
        while ((hasOutput()) && getChannel().isOpen()) {
            if (!hasOutBuffer()) {
                String nextLine = (String) outLines.removeFirst();
                byte[] outBytes = nextLine.getBytes();
                outBuffer = ByteBuffer.wrap(outBytes);
                setHasOutBuffer(true);
            }
            ByteBuffer buf = outBuffer;
            try {
                int nbytes = getChannel().write(buf);
                addSentBytesNum(nbytes);
                if (buf.remaining() == 0) {
                    setHasOutBuffer(false);
                    outBuffer = null;
                } else {
                    break;
                }
            } catch (IOException e) {
                System.out.println("Error sending...: " + e.getMessage());
                try {
                    getChannel().close();
                } catch (IOException e2) {
                }
                doClose = true;
            }
        }
        if (hasOutput()) {
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        } else {
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }

    protected InetSocketAddress address;

    protected LinkedList inLines;

    protected LinkedList outLines;

    protected StringBuffer actInLine;

    protected ByteBuffer outBuffer;

    protected boolean hasOutBuffer = false;

    protected boolean doClose = false;

    protected SelectionKey selectionKey;

    protected SocketChannel channel;

    protected boolean writeSelected = false;

    static final String prompt = "jMule>";

    /**
	 * Returns hasOutBuffer.
	 * @return boolean
	 */
    public boolean hasOutBuffer() {
        return hasOutBuffer;
    }

    /**
	 * Sets hasOutBuffer.
	 * @param hasOutBuffer The hasOutBuffer to set
	 */
    public void setHasOutBuffer(boolean hasOutBuffer) {
        this.hasOutBuffer = hasOutBuffer;
    }
}

package org.antlr.runtime.debug;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.tree.TreeAdaptor;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/** A proxy debug event listener that forwards events over a socket to
 *  a debugger (or any other listener) using a simple text-based protocol;
 *  one event per line.  ANTLRWorks listens on server socket with a
 *  RemoteDebugEventSocketListener instance.  These two objects must therefore
 *  be kept in sync.  New events must be handled on both sides of socket.
 */
public class DebugEventSocketProxy extends BlankDebugEventListener {

    public static final int DEFAULT_DEBUGGER_PORT = 0xC001;

    protected int port = DEFAULT_DEBUGGER_PORT;

    protected ServerSocket serverSocket;

    protected Socket socket;

    protected String grammarFileName;

    protected PrintWriter out;

    protected BufferedReader in;

    /** Who am i debugging? */
    protected BaseRecognizer recognizer;

    /** Almost certainly the recognizer will have adaptor set, but
	 *  we don't know how to cast it (Parser or TreeParser) to get
	 *  the adaptor field.  Must be set with a constructor. :(
	 */
    protected TreeAdaptor adaptor;

    public DebugEventSocketProxy(BaseRecognizer recognizer, TreeAdaptor adaptor) {
        this(recognizer, DEFAULT_DEBUGGER_PORT, adaptor);
    }

    public DebugEventSocketProxy(BaseRecognizer recognizer, int port, TreeAdaptor adaptor) {
        this.grammarFileName = recognizer.getGrammarFileName();
        this.adaptor = adaptor;
        this.port = port;
    }

    public void handshake() throws IOException {
        if (serverSocket == null) {
            serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
            socket.setTcpNoDelay(true);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF8");
            out = new PrintWriter(new BufferedWriter(osw));
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, "UTF8");
            in = new BufferedReader(isr);
            out.println("ANTLR " + DebugEventListener.PROTOCOL_VERSION);
            out.println("grammar \"" + grammarFileName);
            out.flush();
            ack();
        }
    }

    public void commence() {
    }

    public void terminate() {
        transmit("terminate");
        out.close();
        try {
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }

    protected void ack() {
        try {
            in.readLine();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        }
    }

    protected void transmit(String event) {
        out.println(event);
        out.flush();
        ack();
    }

    public void enterRule(String grammarFileName, String ruleName) {
        transmit("enterRule " + grammarFileName + " " + ruleName);
    }

    public void enterAlt(int alt) {
        transmit("enterAlt " + alt);
    }

    public void exitRule(String grammarFileName, String ruleName) {
        transmit("exitRule " + grammarFileName + " " + ruleName);
    }

    public void enterSubRule(int decisionNumber) {
        transmit("enterSubRule " + decisionNumber);
    }

    public void exitSubRule(int decisionNumber) {
        transmit("exitSubRule " + decisionNumber);
    }

    public void enterDecision(int decisionNumber) {
        transmit("enterDecision " + decisionNumber);
    }

    public void exitDecision(int decisionNumber) {
        transmit("exitDecision " + decisionNumber);
    }

    public void consumeToken(Token t) {
        String buf = serializeToken(t);
        transmit("consumeToken " + buf);
    }

    public void consumeHiddenToken(Token t) {
        String buf = serializeToken(t);
        transmit("consumeHiddenToken " + buf);
    }

    public void LT(int i, Token t) {
        if (t != null) transmit("LT " + i + " " + serializeToken(t));
    }

    public void mark(int i) {
        transmit("mark " + i);
    }

    public void rewind(int i) {
        transmit("rewind " + i);
    }

    public void rewind() {
        transmit("rewind");
    }

    public void beginBacktrack(int level) {
        transmit("beginBacktrack " + level);
    }

    public void endBacktrack(int level, boolean successful) {
        transmit("endBacktrack " + level + " " + (successful ? TRUE : FALSE));
    }

    public void location(int line, int pos) {
        transmit("location " + line + " " + pos);
    }

    public void recognitionException(RecognitionException e) {
        StringBuffer buf = new StringBuffer(50);
        buf.append("exception ");
        buf.append(e.getClass().getName());
        buf.append(" ");
        buf.append(e.index);
        buf.append(" ");
        buf.append(e.line);
        buf.append(" ");
        buf.append(e.charPositionInLine);
        transmit(buf.toString());
    }

    public void beginResync() {
        transmit("beginResync");
    }

    public void endResync() {
        transmit("endResync");
    }

    public void semanticPredicate(boolean result, String predicate) {
        StringBuffer buf = new StringBuffer(50);
        buf.append("semanticPredicate ");
        buf.append(result);
        serializeText(buf, predicate);
        transmit(buf.toString());
    }

    public void consumeNode(Object t) {
        StringBuffer buf = new StringBuffer(50);
        buf.append("consumeNode");
        serializeNode(buf, t);
        transmit(buf.toString());
    }

    public void LT(int i, Object t) {
        int ID = adaptor.getUniqueID(t);
        String text = adaptor.getText(t);
        int type = adaptor.getType(t);
        StringBuffer buf = new StringBuffer(50);
        buf.append("LN ");
        buf.append(i);
        serializeNode(buf, t);
        transmit(buf.toString());
    }

    protected void serializeNode(StringBuffer buf, Object t) {
        int ID = adaptor.getUniqueID(t);
        String text = adaptor.getText(t);
        int type = adaptor.getType(t);
        buf.append(" ");
        buf.append(ID);
        buf.append(" ");
        buf.append(type);
        Token token = adaptor.getToken(t);
        int line = -1;
        int pos = -1;
        if (token != null) {
            line = token.getLine();
            pos = token.getCharPositionInLine();
        }
        buf.append(" ");
        buf.append(line);
        buf.append(" ");
        buf.append(pos);
        int tokenIndex = adaptor.getTokenStartIndex(t);
        buf.append(" ");
        buf.append(tokenIndex);
        serializeText(buf, text);
    }

    public void nilNode(Object t) {
        int ID = adaptor.getUniqueID(t);
        transmit("nilNode " + ID);
    }

    public void errorNode(Object t) {
        int ID = adaptor.getUniqueID(t);
        String text = t.toString();
        StringBuffer buf = new StringBuffer(50);
        buf.append("errorNode ");
        buf.append(ID);
        buf.append(" ");
        buf.append(Token.INVALID_TOKEN_TYPE);
        serializeText(buf, text);
        transmit(buf.toString());
    }

    public void createNode(Object t) {
        int ID = adaptor.getUniqueID(t);
        String text = adaptor.getText(t);
        int type = adaptor.getType(t);
        StringBuffer buf = new StringBuffer(50);
        buf.append("createNodeFromTokenElements ");
        buf.append(ID);
        buf.append(" ");
        buf.append(type);
        serializeText(buf, text);
        transmit(buf.toString());
    }

    public void createNode(Object node, Token token) {
        int ID = adaptor.getUniqueID(node);
        int tokenIndex = token.getTokenIndex();
        transmit("createNode " + ID + " " + tokenIndex);
    }

    public void becomeRoot(Object newRoot, Object oldRoot) {
        int newRootID = adaptor.getUniqueID(newRoot);
        int oldRootID = adaptor.getUniqueID(oldRoot);
        transmit("becomeRoot " + newRootID + " " + oldRootID);
    }

    public void addChild(Object root, Object child) {
        int rootID = adaptor.getUniqueID(root);
        int childID = adaptor.getUniqueID(child);
        transmit("addChild " + rootID + " " + childID);
    }

    public void setTokenBoundaries(Object t, int tokenStartIndex, int tokenStopIndex) {
        int ID = adaptor.getUniqueID(t);
        transmit("setTokenBoundaries " + ID + " " + tokenStartIndex + " " + tokenStopIndex);
    }

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }

    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    protected String serializeToken(Token t) {
        StringBuffer buf = new StringBuffer(50);
        buf.append(t.getTokenIndex());
        buf.append(' ');
        buf.append(t.getType());
        buf.append(' ');
        buf.append(t.getChannel());
        buf.append(' ');
        buf.append(t.getLine());
        buf.append(' ');
        buf.append(t.getCharPositionInLine());
        serializeText(buf, t.getText());
        return buf.toString();
    }

    protected void serializeText(StringBuffer buf, String text) {
        buf.append(" \"");
        if (text == null) {
            text = "";
        }
        text = escapeNewlines(text);
        buf.append(text);
    }

    protected String escapeNewlines(String txt) {
        txt = txt.replaceAll("%", "%25");
        txt = txt.replaceAll("\n", "%0A");
        txt = txt.replaceAll("\r", "%0D");
        return txt;
    }
}

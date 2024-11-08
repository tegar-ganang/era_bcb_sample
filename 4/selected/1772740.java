package sk.naive.talker.tcpadapter;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.channels.*;
import java.rmi.*;
import java.util.logging.Level;
import java.util.*;
import sk.naive.talker.*;
import sk.naive.talker.message.*;
import sk.naive.talker.util.*;
import sk.naive.talker.persistence.PersistenceException;
import sk.naive.talker.callback.*;
import sk.naive.talker.adapter.*;
import sk.naive.talker.props.*;

/**
 * TCP adapter user (client representation).
 *
 * @author <a href="mailto:virgo@naive.deepblue.sk">Richard "Virgo" Richter</a>
 * @version $Revision: 1.117 $ $Date: 2005/03/21 20:28:49 $
 */
public class TCPUser extends AbstractUser {

    public static final String UPROP_7BIT_APPROXIMATION = "tcp7bitApproximation";

    public static final String UPROP_CHARSET = "tcpCharset";

    public static final String UPROP_7BIT_TEMPORARY = "tcp7bitTemporary";

    public static final String UPROP_STYLE_SHEET = "tcpStyleSheet";

    public static final String UPROP_TERM_LINES = "tcpTermLines";

    public static final String UPROP_TERM_COLS = "tcpTermCols";

    private static final Replacer[] sendReplacers;

    private static final Replacer[] rcvReplacers;

    static MessageFactory messageFactory;

    private Socket socket;

    private int loginTryCount;

    private StringBuilder buffer;

    /**
	 * Callbacks implementing line processing for the one user state.
	 * <p>
	 * Eg. user is going through many stages during login or creating new user or
	 * editing (line editor). Line processing hook can be presented for the user.
	 * If there is no one then user's line is sent to talker as a command. If there
	 * is line callback presented then process method is called. Method can do
	 * anything appropriate for this state and returns callback for the next one.
	 */
    private Callback lineCallback;

    public TCPUser(Socket socket, TCPAdapter a, RemoteTalker t) throws RemoteException, InvalidPropertyValueException {
        super(a, t);
        this.socket = socket;
        buffer = new StringBuilder();
        getProperties().put(UPROP_TERM_COLS, 80);
        getProperties().put(UPROP_TERM_LINES, 24);
        loginTryCount = 3;
    }

    public void send(String s) throws java.rmi.RemoteException {
        try {
            try {
                s = messageProcessor.process(s);
            } catch (TagProcessorException e) {
                throw new RemoteException("Message/Tag processor exception.", e);
            }
            if (s == null || s.length() == 0) {
                return;
            }
            s = sendReplace(s);
            boolean prop7bitApproximation = BooleanProperty.booleanValue(getString(UPROP_7BIT_APPROXIMATION));
            boolean prop7bitTemporary = BooleanProperty.booleanValue(getString(UPROP_7BIT_TEMPORARY));
            if (prop7bitApproximation || prop7bitTemporary) {
                s = Utils.sevenBitApproximation(s);
            }
            send(Charset.forName(getString(UPROP_CHARSET)).encode(s));
        } catch (IOException e) {
            logger.fine("Disconnected on IOException: " + e.getMessage());
            disconnect(e.getMessage());
        }
    }

    public String sendReplace(String s) {
        return replace(s, sendReplacers);
    }

    public void sendDirectly(String s) throws IOException {
        send(s.getBytes());
    }

    public void send(byte[] ba) throws IOException {
        send(ByteBuffer.wrap(ba));
    }

    public void send(ByteBuffer bb) throws IOException {
        logger.finest("Sending to " + socket + " (user " + getId() + "):\n" + Utils.hexaString(bb.array(), true));
        SocketChannel sc = socket.getChannel();
        while (bb.remaining() > 0) {
            sc.write(bb);
        }
    }

    void sendMessage(String key) throws RemoteException {
        send(messageFactory.getString(key, getProperties()));
    }

    private void sendLineBreak() throws RemoteException {
        send(Utils.tag(TagConsts.BR));
    }

    /**
	 * Line processor called when user entered whole line.
	 * <p>
	 * If there is any callback installed its process method is
	 * invoked with this line. In other case talker kernel side
	 * processes the message.
	 *
	 * @param s line content
	 * @throws sk.naive.talker.callback.CallbackException
	 * @throws RemoteException
	 */
    public void processLine(String s) throws CallbackException, RemoteException {
        s = replace(s, rcvReplacers);
        if (lineCallback != null) {
            lineCallback = lineCallback.process(s);
        } else {
            talker.processUserMessage(this, s);
        }
    }

    String resolveResponse(String resourceKey, String response) {
        if (response == null || response.length() == 0) {
            return "";
        }
        String[] sa = Utils.split(messageFactory.getString(resourceKey, getProperties()), ";", -1);
        Map<String, String> resolver = new LinkedHashMap<String, String>();
        for (int i = 0; i < sa.length; i++) {
            String[] rsa = Utils.split(sa[i], "=", 2);
            resolver.put(rsa[0], rsa[1]);
        }
        response = Utils.findFirstInCollection(Utils.normalize(response), resolver.keySet(), true);
        response = (String) resolver.get(response);
        if (response == null) {
            return "";
        }
        return response;
    }

    public void disconnectCleanup() throws RemoteException {
        try {
            socket.close();
        } catch (Exception e) {
            throw new RemoteException("Error closing socket in disconnect()", e);
        }
    }

    /**
	 * Character level processor.
	 * @param bb
	 * @throws IOException
	 * @throws sk.naive.talker.callback.CallbackException
	 */
    public void processBuffer(ByteBuffer bb) throws IOException, CallbackException {
        CharBuffer cb = Charset.forName(getString(UPROP_CHARSET)).decode(bb);
        while (cb.hasRemaining()) {
            char c = cb.get();
            if (c == '\b' || c == 0x7f) {
                if (buffer.length() > 0) {
                    buffer.deleteCharAt(buffer.length() - 1);
                }
                sendDirectly("\b \b");
            } else if (c == '\r' || c == '\n') {
                String s = buffer.toString();
                buffer.setLength(0);
                processLine(s);
                break;
            } else if (c == 0x15) {
                buffer.setLength(0);
            } else if (c < ' ') {
            } else {
                buffer.append(c);
            }
        }
    }

    public void processCommand(ByteBuffer bb) throws IOException, CallbackException {
        try {
            while (true) {
                while (bb.hasRemaining() && bb.get() != IAC) ;
                if (!bb.hasRemaining()) {
                    break;
                }
                bb.mark();
                byte commandByte = bb.get();
                if (commandByte == SE) {
                } else if (commandByte == SB) {
                    byte optionByte = bb.get();
                    if (optionByte == NAWS) {
                        getProperties().put(UPROP_TERM_COLS, ((int) bb.getShort()));
                        getProperties().put(UPROP_TERM_LINES, ((int) bb.getShort()));
                    }
                } else if (commandByte == EL) {
                } else if (commandByte == EC) {
                } else if (commandByte == XEOF) {
                } else {
                    bb.reset();
                    logger.finest("Unrecognized: IAC " + Utils.hexaString(bb.get()) + " " + Utils.hexaString(bb.get()));
                }
                bb.reset();
            }
        } catch (Exception e) {
            logger.severe("(uid = " + getId() + ") TMP debug in processCommand bb = " + Utils.hexaString(bb.array(), bb.limit(), true));
            Utils.unexpectedExceptionWarning(e);
        }
        bb.clear();
    }

    public void handshake() throws IOException {
        send(DO_BINARY);
        send(DO_LINEMODE);
        send(LM_SB_EDIT);
        send(WONT_ECHO);
        sendMessage("login.issue");
        lineCallback = new LoginCallback();
        send(DO_NAWS);
    }

    public Socket getSocket() {
        return socket;
    }

    public static final byte IAC = (byte) 255;

    public static final byte DONT = (byte) 254;

    public static final byte DO = (byte) 253;

    public static final byte WONT = (byte) 252;

    public static final byte WILL = (byte) 251;

    public static final byte SB = (byte) 250;

    public static final byte EL = (byte) 248;

    public static final byte EC = (byte) 247;

    public static final byte SE = (byte) 240;

    public static final byte XEOF = (byte) 236;

    public static final byte BINARY = (byte) 0;

    public static final byte ECHO = (byte) 1;

    public static final byte NAWS = (byte) 31;

    public static final byte LINEMODE = (byte) 34;

    public static final byte LM_MODE = 1;

    public static final byte LM_FORWARDMASK = 2;

    public static final byte LM_SLC = 3;

    public static final byte MODE_EDIT = 0x01;

    public static final byte MODE_TRAPSIG = 0x02;

    public static final byte MODE_ACK = 0x04;

    public static final byte MODE_SOFT_TAB = 0x08;

    public static final byte MODE_LIT_ECHO = 0x10;

    public static final byte[] DO_BINARY = { IAC, DO, BINARY };

    public static final byte[] DO_LINEMODE = { IAC, DO, LINEMODE };

    public static final byte[] DO_NAWS = { IAC, DO, NAWS };

    public static final byte[] LM_SB_EDIT = { IAC, SB, LINEMODE, LM_MODE, MODE_EDIT, IAC, SE };

    public static final byte[] WONT_ECHO = { IAC, WONT, ECHO };

    public static final byte[] WILL_ECHO = { IAC, WILL, ECHO };

    public static final String TCP_BR = "\r\n";

    private class LoginCallback implements Callback {

        public LoginCallback() throws RemoteException {
            sendMessage("login.prompt");
        }

        public Callback process(String s) throws CallbackException {
            try {
                if (s == null || s.length() == 0) {
                    return new LoginCallback();
                }
                if (s.startsWith(".")) {
                    String[] sa = Utils.splitWords(s.substring(1), 2);
                    String comm = sa[0];
                    if ("who".startsWith(comm)) {
                        String whoResponse = talker.processSystemMessage("who");
                        String[] logins;
                        if (whoResponse.length() > 0) {
                            logins = whoResponse.split("\\n");
                        } else {
                            logins = new String[0];
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append(messageFactory.getString("who.head", getProperties()));
                        Map ctx = new HashMap();
                        for (String name : logins) {
                            ctx.put(DefaultMessageFactory.CTXKEY_VAL, name);
                            sb.append(messageFactory.getString("who.line", ctx, (String) get(User.UPROP_LANG)));
                        }
                        ctx.put(DefaultMessageFactory.CTXKEY_VAL, String.valueOf(logins.length));
                        sb.append(messageFactory.getString("who.tail", ctx, (String) get(User.UPROP_LANG)));
                        send(sb.toString());
                    }
                    if ("quit".startsWith(comm)) {
                        sendMessage("login.quitRequested");
                        disconnect(null);
                        return null;
                    }
                    if ("help".startsWith(comm)) {
                        sendMessage("login.promptHelp");
                        return new LoginCallback();
                    }
                    if ("new".startsWith(comm)) {
                        if (sa.length > 1) {
                            return processNewUserName(sa[1]);
                        }
                        return new NewUserName();
                    }
                    if ("7bit".startsWith(comm)) {
                        Property prop = getProperty(UPROP_7BIT_TEMPORARY);
                        if (sa.length > 1) {
                            try {
                                set(UPROP_7BIT_TEMPORARY, PropUtils.completeValue(prop, sa[1]));
                                sendMessage("login.issue");
                            } catch (InvalidPropertyValueException e) {
                                send(messageFactory.getString("login.invalidBoolean", getProperties()) + prop.validValues() + "<" + TagConsts.BR + ">");
                            }
                        }
                        sendMessage("login.7bitStatus");
                    }
                    if ("lang".startsWith(comm)) {
                        Property prop = getProperty(User.UPROP_LANG);
                        if (sa.length > 1) {
                            try {
                                set(User.UPROP_LANG, PropUtils.completeValue(prop, sa[1]));
                                send(messageFactory.getString("login.issue", getProperties()));
                            } catch (InvalidPropertyValueException e) {
                                send(messageFactory.getString("login.invalidLanguage", getProperties()) + prop.validValues() + "<" + TagConsts.BR + ">");
                            }
                        }
                        sendMessage("login.langStatus");
                    }
                } else {
                    try {
                        s = Utils.normalize(s);
                        set(User.UPROP_LOGIN, s);
                    } catch (InvalidPropertyValueException e) {
                        sendMessage("login.invalidLogin");
                        return new LoginCallback();
                    }
                    return new PasswordCallback();
                }
                return new LoginCallback();
            } catch (IOException e) {
                throw new CallbackException(e);
            }
        }
    }

    /**
	 * Called for processing user name and determining his login.
	 * <p>
	 * Can be called from LoginCallback if .new command has specified user name or
	 * from NewUserName callback. In any case it return NewUserName callback for
	 * not correctly specified name or NewUserFirstPassword callback. If empty
	 * string is provided, LoginCallback is returned.
	 *
	 * @param s new user name with diacritics and casing
	 * @return next callback
	 */
    private Callback processNewUserName(String s) throws CallbackException {
        try {
            if (s.length() == 0) {
                sendMessage("login.new.cancelled");
                return new LoginCallback();
            }
            if (s.length() < 2 || s.length() > 16) {
                sendMessage("login.new.invalitNameLength");
                return new LoginCallback();
            }
            String login = Utils.normalize(s);
            try {
                set(User.UPROP_LOGIN, login);
                set(User.UPROP_NAME, Utils.capitalizeFirst(s));
            } catch (InvalidPropertyValueException e) {
                sendMessage("login.new.invalidName");
                return new NewUserName();
            }
            sendMessage("login.new.newLoginInfo");
            return new NewUserFirstPassword();
        } catch (IOException e) {
            throw new CallbackException(e);
        }
    }

    public void processUserCallback(CallbackResult callbackResult) throws RemoteException {
        talker.processUserCallback(this, callbackResult);
    }

    public int getCols() {
        return (Integer) getProperties().get(TCPUser.UPROP_TERM_COLS);
    }

    public int getLines() {
        return (Integer) getProperties().get(TCPUser.UPROP_TERM_LINES);
    }

    /** New user name callback class. */
    private class NewUserName implements Callback {

        public NewUserName() throws RemoteException {
            sendMessage("login.new.name");
        }

        public Callback process(String s) throws CallbackException {
            return processNewUserName(s);
        }
    }

    /** New user password callback class. */
    private class NewUserFirstPassword implements Callback {

        public NewUserFirstPassword() throws IOException {
            send(WILL_ECHO);
            sendMessage("login.new.password1");
        }

        public Callback process(String s) throws CallbackException {
            try {
                send(WONT_ECHO);
                sendLineBreak();
                if (s.length() == 0) {
                    sendMessage("login.new.cancelled");
                    return new LoginCallback();
                }
                try {
                    set(User.UPROP_PASSWORD, s);
                } catch (InvalidPropertyValueException e) {
                    throw new CallbackException(e);
                }
                return new NewUserSecondPassword();
            } catch (IOException e) {
                throw new CallbackException(e);
            }
        }
    }

    /** New user password confirmation callback class. */
    private class NewUserSecondPassword implements Callback {

        public NewUserSecondPassword() throws IOException {
            send(WILL_ECHO);
            sendMessage("login.new.password2");
        }

        public Callback process(String s) throws CallbackException {
            try {
                send(WONT_ECHO);
                sendLineBreak();
                if (!(s.equals(get(User.UPROP_PASSWORD)))) {
                    sendMessage("login.new.passwordNoMatch");
                    return new NewUserFirstPassword();
                }
                try {
                    set(User.UPROP_AUTH_METHOD, User.AUTH_METHOD_NEW);
                } catch (InvalidPropertyValueException e) {
                    Utils.unexpectedExceptionWarning(e);
                }
                return new NewUserSex();
            } catch (IOException e) {
                throw new CallbackException(e);
            }
        }
    }

    /** New user sex chalenge callback class. */
    private class NewUserSex implements Callback {

        public NewUserSex() throws IOException {
            sendMessage("login.new.sex");
        }

        public Callback process(String s) throws CallbackException {
            try {
                if (s.length() == 0) {
                    sendMessage("login.new.cancelled");
                    return new LoginCallback();
                }
                s = resolveResponse("login.new.sexResolver", s);
                if (s.length() == 0) {
                    return new NewUserSex();
                }
                try {
                    set(SexProperty.UPROP_SEX, s);
                } catch (InvalidPropertyValueException e) {
                    Utils.unexpectedExceptionWarning(e);
                }
                try {
                    talker.userIn(TCPUser.this);
                } catch (AuthenticationException e) {
                    sendMessage("login.new.error");
                    logger.fine("New user not created (" + e.getMessage() + ").");
                    return new LoginCallback();
                } catch (PersistenceException e) {
                    sendMessage("login.new.error");
                    logger.log(Level.WARNING, "Persistence problem", e);
                    return new LoginCallback();
                }
                performLogin();
                return null;
            } catch (IOException e) {
                throw new CallbackException(e);
            }
        }
    }

    /** Password callback class. */
    private class PasswordCallback implements Callback {

        public PasswordCallback() throws IOException {
            send(WILL_ECHO);
            sendMessage("login.password");
        }

        public Callback process(String s) throws CallbackException {
            try {
                send(WONT_ECHO);
                sendLineBreak();
                try {
                    set(User.UPROP_PASSWORD, s);
                    set(User.UPROP_AUTH_METHOD, null);
                } catch (InvalidPropertyValueException e) {
                    Utils.unexpectedExceptionWarning(e);
                }
                try {
                    talker.userIn(TCPUser.this);
                } catch (AuthenticationException e) {
                    sendMessage("login.incorrectPassword");
                    if (--loginTryCount > 0) {
                        return new LoginCallback();
                    }
                    disconnect(null);
                    logger.fine("Unauthorized user.");
                    return null;
                } catch (PersistenceException e) {
                    sendMessage("login.error");
                    logger.log(Level.WARNING, "Persistence problem", e);
                    return new LoginCallback();
                }
                performLogin();
                return null;
            } catch (IOException e) {
                throw new CallbackException(e);
            }
        }
    }

    private void performLogin() throws IOException {
        sendDirectly(AnsiUtils.SET_WRAP_LINES);
        registerLoggedIn();
    }

    private class ClassReplacer implements TagProcessor {

        private String predefinedStyle;

        public ClassReplacer() {
        }

        public ClassReplacer(String predefinedStyle) {
            this.predefinedStyle = predefinedStyle;
        }

        public String process(MessageProcessingContext ctx, String params) throws TagProcessorException {
            String styleClass = params;
            if (predefinedStyle != null) {
                styleClass = predefinedStyle;
            }
            return StyleSheet.getStyleSheet(getString(UPROP_STYLE_SHEET)).getAnsiSequence(styleClass);
        }
    }

    private class ResetReplacer implements TagProcessor {

        private String postString;

        public ResetReplacer() {
            postString = "";
        }

        public ResetReplacer(String postString) {
            this.postString = postString;
        }

        public String process(MessageProcessingContext ctx, String params) throws TagProcessorException {
            return ansiReset() + postString;
        }
    }

    String ansiReset() {
        return StyleSheet.getStyleSheet(getString(UPROP_STYLE_SHEET)).getAnsiSequence("reset");
    }

    private boolean installUniqueCallback(Callback callback) throws RemoteException {
        if (lineCallback == null) {
            lineCallback = callback;
            return true;
        }
        if (callback instanceof IdentifiedCallback) {
            processUserCallback(new CallbackCancelResult(((IdentifiedCallback) callback).getId()));
        }
        return false;
    }

    {
        register(TagConsts.EDITOR, new TagProcessor() {

            public String process(MessageProcessingContext ctx, String params) throws TagProcessorException {
                final LineEditor le;
                le = new LineEditor(TCPUser.this, params);
                try {
                    if (!installUniqueCallback(le)) {
                        return null;
                    }
                } catch (RemoteException e) {
                    Utils.unexpectedExceptionWarning(e);
                }
                ctx.switchConsumer(new MessageProcessingContext.BufferMessageConsumer() {

                    public String result() {
                        try {
                            le.start(buffer.toString());
                        } catch (RemoteException e) {
                            Utils.unexpectedExceptionWarning(e);
                        }
                        return null;
                    }
                }, true);
                ctx.interruptProcessing();
                return null;
            }
        });
        register(TagConsts.PAGING, new TagProcessor() {

            public String process(MessageProcessingContext ctx, String params) throws TagProcessorException {
                ctx.switchConsumer(new MessageProcessingContext.BufferMessageConsumer() {

                    private boolean processed;

                    public void append(String s) throws TagProcessorException {
                        if (processed || lineCallback != null) {
                            return;
                        }
                        Pager pager = new Pager(TCPUser.this, s);
                        try {
                            pager.showPage();
                        } catch (RemoteException e) {
                            throw new TagProcessorException(e);
                        }
                        if (!pager.isFinished()) {
                            lineCallback = pager;
                        }
                        processed = true;
                    }

                    public String result() {
                        return null;
                    }
                }, true);
                return null;
            }
        });
        register(TagConsts.RESOURCE, new TagProcessor() {

            public String process(MessageProcessingContext ctx, String params) throws TagProcessorException {
                return messageProcessor.process(messageFactory.getString(params, getProperties()));
            }
        });
        register(TagConsts.RESET, new ResetReplacer());
        register(TagConsts.CLEAR, new ResetReplacer(AnsiUtils.ERASE_DISPLAY + AnsiUtils.HOME(0, 0)));
        register(TagConsts.BR, new ResetReplacer(TCP_BR));
        register(TagConsts.CLASS, new ClassReplacer());
        register(TagConsts.HEADER, new ClassReplacer("header"));
        register(TagConsts.FOOTER, new ClassReplacer("footer"));
        register(TagConsts.LIST, new ListTagProcessor());
        register(TagConsts.TABLE, new TableTagProcessor());
        register(TagConsts.COLS, new ColsTagProcessor());
        register(TagConsts.ITEM, new ItemTagProcessor());
        register("bp1", new TagReplacer("\007"));
        register("bp2", new TagReplacer("\007\007\007"));
        register("", new TagReplacer(""));
    }

    static {
        messageFactory = new DefaultMessageFactory("sk/naive/talker/tcpadapter/messages");
        List l = new ArrayList();
        l.add(Replacer.getReplacer("<([<>])>", "$1"));
        sendReplacers = new AbstractUser.Replacer[l.size()];
        l.toArray(sendReplacers);
        l = new ArrayList();
        l.add(Replacer.getReplacer("([<>])", "<$1>"));
        rcvReplacers = new AbstractUser.Replacer[l.size()];
        l.toArray(rcvReplacers);
    }
}

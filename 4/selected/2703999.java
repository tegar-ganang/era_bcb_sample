package net.sf.atmodem4j.core.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.sf.atmodem4j.core.Modem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author aploese
 */
public class Parser {

    private ParserState parserState = ParserState.COLLECT_ALL;

    public String getEcho() {
        return parsedEcho;
    }

    private char[] buildToken(String s, char... c) {
        StringBuilder sb1 = new StringBuilder(s);
        sb1.append(c);
        return sb1.toString().toCharArray();
    }

    private char[] buildToken(char... c) {
        return c;
    }

    private void matchedGarbage(int end) {
        modem.garbageCollected(this, sb.substring(0, end));
        sb.delete(0, end);
    }

    private int findLastToken(char[] token) {
        if (sb.length() - token.length <= 0) {
            return -1;
        }
        for (int i = sb.length() - token.length; i >= 0; i--) {
            for (int j = token.length - 1; j >= 0; j--) {
                if (sb.charAt(i + j) == token[j]) {
                    if (j == 0) {
                        return i;
                    }
                } else {
                    break;
                }
            }
        }
        return -1;
    }

    private boolean lastIs(char[] chars) {
        if (sb.length() < chars.length) {
            return false;
        }
        boolean result = true;
        int sbOffset = sb.length() - 1;
        int charsOffset = chars.length - 1;
        for (int i = 0; i < chars.length; i++) {
            if (sb.charAt(sbOffset - i) != chars[charsOffset - i]) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void resetParser() {
        clearBuffer();
        parserState = ParserState.COLLECT_ALL;
        parsedData = new String[0];
        parsedEcho = null;
    }

    public enum ParserState {

        COLLECT_ALL, COLLECT_CMD_ECHO, COLLECT_DATA_OR_RESPONSE
    }

    private static final Log log = LogFactory.getLog(Parser.class);

    public static final String ONLINE_DATA_OMNLINE_COMMAND_INDICATOR = "+++";

    private InputStream os;

    private InputStream is;

    private ParserThread parserThread;

    private char cr = '\r';

    private char lf = '\n';

    private char[] tokenCrLf = new char[] { cr, lf };

    private char[] tokenCrCrLf = new char[] { cr, cr, lf };

    private char[] tokenPrompt = new char[] { '>' };

    private char[] tokenAT = new char[] { 'A', 'T' };

    private char[] tokenOk;

    private char[] tokenConnect;

    private char[] tokenRing;

    private char[] tokenNoCarrier;

    private char[] tokenError;

    private char[] tokenNoDialtone;

    private char[] tokenBusy;

    private char[] tokenNoAnswer;

    private char[] tokenConnectWithText;

    private ModemState state;

    private final StringBuilder sb = new StringBuilder();

    private int onlineEscapeIndex;

    private String parsedEcho;

    private String[] parsedData;

    public char getCR() {
        return cr;
    }

    public char getLF() {
        return lf;
    }

    public boolean isOnlineDataMode() {
        return ModemState.ONLINE_DATA_STATE.equals(state);
    }

    public void prepareOnlineHangup() {
        state = ModemState.ONLINE_COMMAND_STATE;
    }

    class ConnectionInputStream extends InputStream {

        private final int[] buffer = new int[2048];

        private int readPos = 0;

        private int writePos = 0;

        private void clearBuffer() {
            log.info("Clean Buffer start");
            synchronized (buffer) {
                readPos = 0;
                writePos = 0;
            }
            log.info("Clean Buffer done");
        }

        private void putByte(int b) {
            synchronized (buffer) {
                if (writePos == buffer.length - 1 && readPos == 0) {
                    throw new RuntimeException("Buffer full no read");
                } else {
                    if (writePos == readPos - 1) {
                        throw new RuntimeException("Buffer Overrun at: " + writePos);
                    }
                }
                buffer[writePos++] = b;
                if (writePos == buffer.length) {
                    writePos = 0;
                    log.info("Buffer write wrap");
                }
                buffer.notifyAll();
            }
        }

        @Override
        public int read() throws IOException {
            int result;
            synchronized (buffer) {
                if (writePos == readPos) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException ex) {
                        return -1;
                    }
                }
                result = buffer[readPos++];
                if (readPos == buffer.length) {
                    readPos = 0;
                    log.info("Buffer read wrap");
                }
            }
            return result;
        }

        @Override
        public void close() {
            log.info("Buffer Close");
            putByte(-1);
            clearBuffer();
        }
    }

    Modem modem;

    private boolean rxtxTimeoutEnabled = false;

    private ConnectionInputStream connectionInputStream = new ConnectionInputStream();

    public Parser(boolean rxtxTimeoutEnabled) {
        this.rxtxTimeoutEnabled = rxtxTimeoutEnabled;
        state = ModemState.COMMAND_STATE;
        setLineChars('\r', '\n');
    }

    public Parser(InputStream is, char cr, char lf, boolean rxtxTimeoutEnabled) {
        this(rxtxTimeoutEnabled);
        setInputStream(is);
        setLineChars(cr, lf);
        start();
    }

    public void setModem(Modem modem) {
        this.modem = modem;
    }

    public Modem getModem() {
        return modem;
    }

    public synchronized void clearBuffer() {
        sb.delete(0, sb.length());
    }

    public void setRxtxTimeoutEnabled(boolean rxtxTimeoutEnabled) {
        this.rxtxTimeoutEnabled = rxtxTimeoutEnabled;
    }

    /**
     * @return the is
     */
    public InputStream getInputStream() {
        return is;
    }

    /**
     * @param is the is to set
     */
    public void setInputStream(InputStream is) {
        final InputStream oldIs = this.is;
        this.is = is;
        if (oldIs == null && is != null) {
            start();
        }
    }

    public void setOutputStream(InputStream os) {
        this.os = os;
    }

    /**
     * @param lineEnd the lineEnd to set
     */
    public void setLineChars(char cr, char lf) {
        this.cr = cr;
        this.lf = lf;
        this.tokenCrLf = buildToken(cr, lf);
        this.tokenCrCrLf = buildToken(cr, cr, lf);
        this.tokenOk = buildToken(ResultCodeToken.OK, cr, lf);
        this.tokenConnect = buildToken(ResultCodeToken.CONNECT, cr, lf);
        this.tokenRing = buildToken(ResultCodeToken.RING, cr, lf);
        this.tokenNoCarrier = buildToken(ResultCodeToken.NO_CARRIER, cr, lf);
        this.tokenError = buildToken(ResultCodeToken.ERROR, cr, lf);
        this.tokenNoDialtone = buildToken(ResultCodeToken.NO_DIALTONE, cr, lf);
        this.tokenBusy = buildToken(ResultCodeToken.BUSY, cr, lf);
        this.tokenNoAnswer = buildToken(ResultCodeToken.NO_ANSWER, cr, lf);
        this.tokenConnectWithText = buildToken(ResultCodeToken.CONNECT);
    }

    public void start() {
        if (parserThread != null) {
            return;
        }
        parserThread = new ParserThread();
        parserThread.setDaemon(true);
        parserThread.start();
    }

    private void matchedBusy() {
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.BUSY, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedConnect() {
        state = ModemState.ONLINE_DATA_STATE;
        connectionInputStream.clearBuffer();
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.CONNECT, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedConnectWithText() {
        state = ModemState.ONLINE_DATA_STATE;
        connectionInputStream.clearBuffer();
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.CONNECT, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedError() {
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.ERROR, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedNoAnswer() {
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.NO_ANSWER, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedNoCarrier() {
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.NO_CARRIER, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedNoDialtone() {
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.NO_DIALTONE, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedOk() {
        if (modem != null) {
            modem.parsedResultCode(this, new ResultCodeToken(ResultCodeToken.OK, parsedData, parsedEcho));
        }
        resetParser();
    }

    private void matchedRing() {
        if (modem != null) {
            modem.parsedRing(this);
        }
        sb.delete(sb.length() - tokenRing.length, sb.length() - 1);
    }

    private void matchedPrompt() {
        sb.delete(0, 1);
        if (modem != null) {
            modem.parsedPrompt(this);
        }
    }

    /**
     * @return the state
     */
    public ModemState getState() {
        return state;
    }

    class ParserThread extends Thread {

        @Override
        public void run() {
            log.debug("Execute ParserThread");
            try {
                while (true) {
                    int i = is.read();
                    if (i == -1) {
                        if (!rxtxTimeoutEnabled) {
                            parseByte(i);
                            break;
                        } else {
                            try {
                                sleep(100);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }
                    } else {
                        if (log.isTraceEnabled()) {
                            log.trace(String.format("Char received 0x%x\t%s", i, (char) i));
                        }
                        parseByte((byte) i);
                    }
                }
            } catch (IOException ex) {
            } finally {
                parserThread = null;
                log.debug("ParserThread finished");
            }
        }
    }

    private boolean matchResult(String token) {
        return sb.lastIndexOf(token) != -1;
    }

    private synchronized void parseChar(char b) {
        sb.append(b);
        if (lastIs(tokenRing)) {
            matchedRing();
        }
        switch(parserState) {
            case COLLECT_ALL:
                if (lastIs(tokenOk)) {
                    matchedOk();
                } else if (lastIs(tokenBusy)) {
                    matchedBusy();
                } else if (lastIs(tokenConnect)) {
                    matchedConnect();
                } else if (lastIs(tokenError)) {
                    matchedError();
                } else if (lastIs(tokenNoAnswer)) {
                    matchedNoAnswer();
                } else if (lastIs(tokenNoCarrier)) {
                    matchedNoCarrier();
                } else if (lastIs(tokenNoDialtone)) {
                    matchedNoDialtone();
                } else if (lastIs(tokenCrLf)) {
                    if (findLastToken(tokenConnectWithText) >= 0) {
                        matchedConnectWithText();
                    }
                } else if (lastIs(tokenAT)) {
                    parserState = ParserState.COLLECT_CMD_ECHO;
                    if (sb.length() > tokenAT.length) {
                        matchedGarbage(sb.length() - tokenAT.length);
                    }
                    break;
                }
                break;
            case COLLECT_CMD_ECHO:
                if (lastIs(tokenCrCrLf)) {
                    parsedEcho = sb.substring(0, sb.length() - tokenCrCrLf.length);
                    parsedData = new String[0];
                    parserState = ParserState.COLLECT_DATA_OR_RESPONSE;
                    clearBuffer();
                }
                break;
            case COLLECT_DATA_OR_RESPONSE:
                if (parsedData.length == 0 && lastIs(tokenPrompt)) {
                    matchedPrompt();
                } else if (lastIs(tokenOk)) {
                    matchedOk();
                } else if (lastIs(tokenBusy)) {
                    matchedBusy();
                } else if (lastIs(tokenConnect)) {
                    matchedConnect();
                } else if (lastIs(tokenError)) {
                    matchedError();
                } else if (lastIs(tokenNoAnswer)) {
                    matchedNoAnswer();
                } else if (lastIs(tokenNoCarrier)) {
                    matchedNoCarrier();
                } else if (lastIs(tokenNoDialtone)) {
                    matchedNoDialtone();
                } else if (lastIs(tokenCrLf)) {
                    if (findLastToken(tokenConnectWithText) >= 0) {
                        matchedConnectWithText();
                    } else {
                        parsedData = Arrays.copyOf(parsedData, parsedData.length + 1);
                        parsedData[parsedData.length - 1] = sb.substring(0, sb.length() - tokenCrLf.length);
                        clearBuffer();
                    }
                }
                break;
            default:
                throw new RuntimeException("UNKNOWN PARSER STATE");
        }
    }

    private void parseByte(int b) {
        switch(state) {
            case COMMAND_STATE:
                if (b != -1) {
                    parseChar((char) b);
                }
                break;
            case ONLINE_COMMAND_STATE:
                if (b != -1) {
                    parseChar((char) b);
                }
                break;
            case ONLINE_DATA_STATE:
                if ((char) b == ONLINE_DATA_OMNLINE_COMMAND_INDICATOR.charAt(onlineEscapeIndex)) {
                    onlineEscapeIndex++;
                    if (onlineEscapeIndex == ONLINE_DATA_OMNLINE_COMMAND_INDICATOR.length()) {
                        state = ModemState.ONLINE_COMMAND_STATE;
                        onlineEscapeIndex = 0;
                        log.info("received \"+++\", goto online command mode");
                    }
                } else {
                    onlineEscapeIndex = 0;
                }
                connectionInputStream.putByte(b);
                break;
        }
    }

    public InputStream getConnectionInputStream() {
        return connectionInputStream;
    }

    public String getBuffer() {
        return sb.toString();
    }
}

package org.szegedi.nioserver.protocols.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.szegedi.nbpipe.ByteBufferPool;
import org.szegedi.nbpipe.NonblockingPipe;
import org.szegedi.nioserver.protocols.AbstractPipedProtocolHandler;

class HttpProtocolHandler extends AbstractPipedProtocolHandler implements NioHttpRequest {

    private static final int MIN_LINE_BUFFER_SIZE = 1024;

    private static final int MAX_LINE_BUFFER_SIZE = 32768;

    private static final String HTTP09_PROTOCOL = "HTTP/0.9";

    private static final String HTTP11_PROTOCOL = "HTTP/1.1";

    private static final byte CR = (byte) '\r';

    private static final byte LF = (byte) '\n';

    private static final byte SP = (byte) ' ';

    private static final byte HT = (byte) '\t';

    private static final byte COLON = (byte) ':';

    private static final byte LC_OFFSET = (byte) ('a' - 'A');

    private static final String TRANSFER_ENCODING = "transfer-encoding";

    private static final String CONTENT_LENGTH = "content-length";

    private static final String CONNECTION = "connection";

    private static final String EXPECT = "expect";

    private static final String COOKIE = "cookie";

    private static final String CHUNKED = "chunked";

    private static final String CONNECTION_KEEP_ALIVE = "keep-alive";

    private static final String CONNECTION_CLOSE = "close";

    private static final String EXPECT_100_VALUE = "100-continue";

    private static final int STATE_SKIP_CRLF = 0;

    private static final int STATE_READ_REQUEST_LINE = 1;

    private static final int STATE_READ_HEADER = 2;

    private static final int STATE_READ_BODY = 3;

    private static final int STATE_READ_CHUNKED_BODY = 4;

    private static final int STATE_SKIP_TRAILING_HEADERS = 5;

    private static final int STATE_GENERATE_RESPONSE = 6;

    private static final int STATE_ERROR = 7;

    private static final int END_OF_INPUT = 0;

    private static final int NO_INPUT = 1;

    private static final int MORE_INPUT = 2;

    private final ByteBufferPool bufferPool;

    private final Adapter adapter;

    private boolean closedConnection = false;

    private int state;

    private boolean bufferedRead;

    private int chunkRemaining;

    private int contentLength;

    private boolean lastByteWasCR = false;

    private byte[] lineBuffer = new byte[MIN_LINE_BUFFER_SIZE];

    private int lineLen;

    private char[] headerBuffer = new char[MIN_LINE_BUFFER_SIZE];

    private int headerLen;

    private String method;

    private String uri;

    private String protocol;

    private final Map headers = new HashMap();

    private final NonblockingPipe requestBodyPipe;

    private boolean keepAlive;

    private boolean sendAck;

    private boolean http11;

    private boolean chunked;

    HttpProtocolHandler(SocketChannel channel, ByteBufferPool bufferPool, Adapter adapter) {
        super(channel, bufferPool);
        this.bufferPool = bufferPool;
        this.adapter = adapter;
        requestBodyPipe = new NonblockingPipe(bufferPool);
        reset();
    }

    public boolean doRead() throws IOException {
        if (state == STATE_GENERATE_RESPONSE) {
            return false;
        }
        ByteBuffer buf = null;
        boolean endRead = false;
        try {
            while (!endRead) readLoop: {
                if (bufferedRead) {
                    if (buf == null) buf = bufferPool.getMemoryBuffer();
                    if (!buf.hasRemaining()) {
                        buf.clear();
                        int read = socketChannel.read(buf);
                        if (read < 1) {
                            if (read == -1) endRead = true;
                            break readLoop;
                        }
                        buf.flip();
                    }
                }
                switch(state) {
                    case STATE_SKIP_CRLF:
                        {
                            skipCrLf(buf);
                            break;
                        }
                    case STATE_READ_REQUEST_LINE:
                        {
                            readRequestLine(buf);
                            break;
                        }
                    case STATE_READ_HEADER:
                        {
                            readHeader(buf);
                            break;
                        }
                    case STATE_READ_BODY:
                        {
                            switch(readBody(buf)) {
                                case END_OF_INPUT:
                                    endRead = true;
                                case NO_INPUT:
                                    break readLoop;
                            }
                            break;
                        }
                    case STATE_READ_CHUNKED_BODY:
                        {
                            switch(readChunkedBody(buf)) {
                                case END_OF_INPUT:
                                    endRead = true;
                                case NO_INPUT:
                                    break readLoop;
                            }
                            break;
                        }
                    case STATE_SKIP_TRAILING_HEADERS:
                        {
                            skipTrailingHeaders(buf);
                            break;
                        }
                    case STATE_ERROR:
                        {
                            endRead = true;
                            break;
                        }
                }
            }
        } finally {
            if (buf != null) bufferPool.putBuffer(buf);
        }
        return endRead;
    }

    /**
     * Processing logic of the first state - skip initial CR and LF characters
     * in the request.
     */
    private void skipCrLf(ByteBuffer buf) {
        for (int i = 0, l = buf.remaining(); i++ < l; ) {
            byte b = buf.get();
            if (b != CR && b != LF) {
                buf.position(buf.position() - 1);
                state = STATE_READ_REQUEST_LINE;
                break;
            }
        }
    }

    /**
     * Processing logic of the second state - read request line containing
     * the protocol version, method, and the URL. Input is buffered until an
     * EOL, and then parsed into method, URI and protocol tokens.
     */
    private void readRequestLine(ByteBuffer buf) {
        if (copyBufferToLine(buf)) {
            int i = 0;
            while (i < lineLen) {
                byte b = lineBuffer[i++];
                if (b == SP) {
                    break;
                }
            }
            method = new String(lineBuffer, 0, i);
            while (i < lineLen && lineBuffer[i++] == SP) ;
            int uristart = i;
            while (i < lineLen) {
                byte b = lineBuffer[i++];
                if (b == SP) {
                    break;
                }
            }
            uri = new String(lineBuffer, uristart, i - uristart);
            while (i < lineLen && lineBuffer[i++] == SP) ;
            protocol = new String(lineBuffer, i, lineLen - i).trim();
            if (protocol.length() == 0) protocol = HTTP09_PROTOCOL;
            sendAck = false;
            http11 = keepAlive = protocol.equals(HTTP11_PROTOCOL);
            state = STATE_READ_HEADER;
            lineLen = 0;
        }
    }

    /**
     * Processing logic of the third state - read headers.Input is
     * buffered until an EOL, and then parsed into a name-value pair. If
     * a line begins with a space or horizontal tab, it is appended to the
     * previous header's value. Special headers that affect request processing
     * are interpreted on the fly (Content-Length, Transfer-Encoding, Expect, 
     * Cookie, Connection).
     */
    private void readHeader(ByteBuffer buf) {
        if (copyBufferToLine(buf)) {
            if (lineLen == 0) {
                processHeader();
                interpretHeaders();
                if (chunked) {
                    state = STATE_READ_CHUNKED_BODY;
                    contentLength = 0;
                } else {
                    if (contentLength == -1) {
                    } else {
                        state = STATE_READ_BODY;
                        chunkRemaining = contentLength;
                    }
                }
                if (sendAck) sendAcknowledgement();
                return;
            }
            byte b = lineBuffer[0];
            if (b == SP || b == HT) {
                lineBuffer[0] = SP;
            } else {
                processHeader();
            }
            copyLineToHeader();
            lineLen = 0;
        }
    }

    /**
     * Copies the input line buffer to the header buffer. This is required as
     * a single buffer can span multiple lines.
     */
    private void copyLineToHeader() {
        if (lineLen + headerLen > headerBuffer.length) {
            if (headerBuffer.length < MAX_LINE_BUFFER_SIZE) {
                char[] newBuf = new char[2 * headerBuffer.length];
                System.arraycopy(headerBuffer, 0, newBuf, 0, headerLen);
                headerBuffer = newBuf;
            } else {
            }
        }
        System.arraycopy(lineBuffer, 0, headerBuffer, headerLen, lineLen);
        headerLen += lineLen;
    }

    /**
     * Processes a header after it is fully read. Interprets headers that
     * affect request processing.
     */
    private void processHeader() {
        int i = 0;
        for (; i < headerLen; ++i) {
            char b = headerBuffer[i];
            if (b == COLON) break;
            if (b >= 'A' && b <= 'Z') headerBuffer[i] = (char) (b + LC_OFFSET);
        }
        int nameLen = i;
        for (; i < headerLen; ++i) {
            char b = headerBuffer[i];
            if (b != SP && b != HT) break;
        }
        String name = new String(headerBuffer, 0, nameLen);
        int valueStart = i;
        int valueLen = headerLen - valueStart;
        String oldValue = (String) headers.get(name);
        String value = null;
        if (oldValue == null) {
            value = new String(headerBuffer, valueStart, valueLen);
        } else {
            value = new StringBuffer(oldValue.length() + 1 + valueLen).append(oldValue).append(',').append(headerBuffer, valueStart, valueLen).toString();
        }
        headers.put(name, value);
    }

    /**
     * Looks up and stores those headers that affect protocol handling.
     */
    private void interpretHeaders() {
        String value = (String) headers.get(TRANSFER_ENCODING);
        if (value != null && value.indexOf(CHUNKED) != -1) chunked = true;
        if (!chunked) {
            value = (String) headers.get(CONTENT_LENGTH);
            if (value != null) {
                try {
                    contentLength = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                }
            }
        }
        value = (String) headers.get(CONNECTION);
        if (value != null) {
            if (value.equalsIgnoreCase(CONNECTION_CLOSE)) keepAlive = false; else if (value.equalsIgnoreCase(CONNECTION_KEEP_ALIVE)) keepAlive = true;
        }
        sendAck = EXPECT_100_VALUE.equalsIgnoreCase((String) headers.get(EXPECT));
    }

    private char[] toLowerCase(char[] buf) {
        for (int i = 0, l = buf.length; i < l; i++) {
            char c = buf[i];
            if (c >= 'A' && c <= 'Z') buf[i] = (char) (c + LC_OFFSET);
        }
        return buf;
    }

    /**
     * Logic for the fourth state - read non-chunked body. When all of the
     * body is read, asynchronous request processing is triggered.
     * @throws IOException which gets propagated to the doRead(), which will
     * close the channel and terminate the connection in case of an I/O
     * error (which usually signals an unrecoverable network problem)
     */
    private int readBody(ByteBuffer buf) throws IOException {
        int readStatus = readAvailableBodyChunk(buf);
        if (chunkRemaining == 0) {
            bufferedRead = true;
            processRequest();
        }
        return readStatus;
    }

    /**
     * Logic for the fifth state - read chunked body. When all of the
     * body is read, trailing header skipping state is entered.
     * @throws IOException which gets propagated to the doRead(), which will
     * close the channel and terminate the connection in case of an I/O
     * error (which usually signals an unrecoverable network problem)
     */
    private int readChunkedBody(ByteBuffer buf) throws IOException {
        if (chunkRemaining == 0) {
            bufferedRead = true;
            if (copyBufferToLine(buf)) {
                if (lineLen > 0) {
                    try {
                        chunkRemaining = Integer.parseInt(new String(lineBuffer, 0, lineLen), 16);
                    } catch (NumberFormatException e) {
                    }
                    lineLen = 0;
                    if (chunkRemaining == 0) {
                        state = STATE_SKIP_TRAILING_HEADERS;
                    } else {
                        contentLength += chunkRemaining;
                    }
                }
            }
            return MORE_INPUT;
        } else {
            return readAvailableBodyChunk(buf);
        }
    }

    private int readAvailableBodyChunk(ByteBuffer buf) throws IOException {
        if (bufferedRead) {
            chunkRemaining -= requestBodyPipe.transferFrom(buf, chunkRemaining);
            if (chunkRemaining > 0) {
                bufferedRead = false;
            }
            return MORE_INPUT;
        } else {
            int writeCount = requestBodyPipe.transferFrom(socketChannel, chunkRemaining);
            if (writeCount == 0) return NO_INPUT;
            if (writeCount == -1) return END_OF_INPUT;
            chunkRemaining -= writeCount;
            return MORE_INPUT;
        }
    }

    /**
     * Logic for the sixth state - skip trailing headers of the chunked body.
     * When all trailing headers are skipped, asynchronous request processing
     * is triggered.
     */
    private void skipTrailingHeaders(ByteBuffer buf) {
        if (copyBufferToLine(buf)) {
            if (lineLen == 0) {
                processRequest();
            }
        }
    }

    /**
     * Called after the request body is fully buffered. It starts the
     * container-specific request processing on a separate thread.
     */
    private void processRequest() {
        adapter.serviceRequest(this);
        state = STATE_GENERATE_RESPONSE;
    }

    /**
     * Copies the byte buffer to the line buffer up to the
     * first CR, LF, or CRLF. The encountered line terminator
     * itself is consumed but not copied.
     */
    private boolean copyBufferToLine(ByteBuffer buf) {
        boolean lineCompleted = false;
        if (lastByteWasCR && buf.hasRemaining()) {
            byte b = buf.get();
            if (b != LF) {
                buf.position(buf.position() - 1);
            }
        }
        int lineCapacity = lineBuffer.length;
        for (int i = 0, l = buf.remaining(); i++ < l; ) {
            byte b = buf.get();
            if (b == CR) {
                if (i < l) {
                    b = buf.get();
                    if (b != LF) {
                        buf.position(buf.position() - 1);
                    }
                    lastByteWasCR = false;
                } else {
                    lastByteWasCR = true;
                }
                lineCompleted = true;
                break;
            } else if (b == LF) {
                lineCompleted = true;
                break;
            } else {
                if (lineLen >= lineCapacity) {
                    if (lineCapacity < MAX_LINE_BUFFER_SIZE) {
                        byte[] newBuf = new byte[2 * lineCapacity];
                        System.arraycopy(lineBuffer, 0, newBuf, 0, lineLen);
                        lineBuffer = newBuf;
                    } else {
                    }
                }
                lineBuffer[lineLen++] = b;
            }
        }
        return lineCompleted;
    }

    public void doEndRead() throws IOException {
        closeConnection();
    }

    public void doEndWrite() throws IOException {
        closeConnection();
    }

    private synchronized void closeConnection() {
        if (!closedConnection) {
            closedConnection = true;
            try {
                socketChannel.close();
            } catch (IOException e) {
            }
            outputPipe.clear();
            outputPipe.closeForWriting();
            requestBodyPipe.clear();
            requestBodyPipe.closeForWriting();
        }
    }

    private void reset() {
        state = STATE_SKIP_CRLF;
        keepAlive = sendAck = http11 = chunked = lastByteWasCR = false;
        bufferedRead = true;
        contentLength = -1;
        lineLen = headerLen = 0;
        headers.clear();
    }

    private void sendAcknowledgement() {
    }

    final void doneResponse() {
        reset();
        try {
            doRead();
        } catch (IOException e) {
            closeConnection();
        }
    }

    public int getContentLength() {
        return contentLength;
    }

    public Iterator getHeaderNames() {
        return headers.keySet().iterator();
    }

    public String getHeader(String name) {
        return (String) headers.get(name);
    }

    public String getMethod() {
        return method;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getURI() {
        return uri;
    }

    public InputStream getRequestBodyInputStream() {
        return requestBodyPipe.getInputStream();
    }

    public OutputStream getResponseOutputStream() {
        return outputPipe.getOutputStream();
    }
}

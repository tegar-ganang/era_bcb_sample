package uips.communication.http.impl.baseServer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.logging.Level;
import uips.support.Consts;
import uips.support.localization.IMessagesInstance;
import uips.support.logging.ILogInstance;
import uips.support.storage.IUipFilesAccessInstance;
import uips.support.storage.interfaces.IUipFile;

/**
 * Class processing client HTTP requests and sending responses.
 * <br><br>
 * Based on Miroslav Macik's C# version of UIProtocolServer
 *
 * @author Miroslav Macik (macikm1@fel.cvut.cz, CTU Prague, FEE)
 * @author Jindrich Basek (basekjin@fit.cvut.cz, CTU Prague, FIT)
 */
public class BaseHttpClient implements Runnable {

    /**
     * HTTP server listening for new client connections
     */
    private final BaseHttpServerListener server;

    /**
     * Client communication socket
     */
    private final SocketChannel socket;

    /**
     * Adress of connected client
     */
    private final String remoteAddress;

    /**
     * This thread
     */
    private Thread clientThread;

    /**
     * Is client thread started?
     */
    private boolean started = true;

    /**
     * Charset decoded
     */
    private final CharsetDecoder decoder;

    /**
     * Buffer for sending data
     */
    private final ByteBuffer sendBuffer;

    private final IUipFilesAccessInstance filesAccess;

    private final ILogInstance log;

    private final IMessagesInstance messages;

    private final String mediaRoot;

    /**
     * Disposes HTTP client thread and closes connection
     */
    public void dispose() {
        this.started = false;
        try {
            this.socket.socket().close();
            this.socket.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Constructor.
     *
     * @param server Reference to http server
     * @param socket Client's socket
     * @param instanceServer is the server client running on HTTP server common
     * for all instance or only private for one instance
     */
    public BaseHttpClient(BaseHttpServerListener server, SocketChannel socket, IUipFilesAccessInstance filesAccess, ILogInstance log, IMessagesInstance messages, String mediaRoot) {
        this.messages = messages;
        this.mediaRoot = mediaRoot;
        this.log = log;
        this.filesAccess = filesAccess;
        this.server = server;
        this.socket = socket;
        this.sendBuffer = ByteBuffer.allocateDirect(Consts.HttpClientBufferSize);
        this.decoder = Charset.forName("UTF-8").newDecoder();
        this.remoteAddress = socket.socket().getRemoteSocketAddress().toString();
        this.clientThread = new Thread(this);
    }

    /**
     * Starts thread communicating with client
     */
    public void start() {
        this.clientThread.start();
    }

    /**
     * Processes http client request and sends response.
     */
    @Override
    public void run() {
        try {
            StringBuilder receivedText = new StringBuilder(Consts.HttpClientBufferSize * 5);
            ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(Consts.HttpClientBufferSize);
            while (this.started) {
                receiveBuffer.clear();
                int receivedBytesCount = this.socket.read(receiveBuffer);
                if (receivedBytesCount == -1 || receivedBytesCount == 0) {
                    break;
                }
                receiveBuffer.flip();
                receivedText.append(this.decoder.decode(receiveBuffer).toString());
                this.log.write(Level.INFO, String.format(this.messages.getString("HttpClientRequest"), receivedBytesCount, this.remoteAddress));
                int indexReq = receivedText.indexOf(Consts.HeaderCRLF + Consts.HeaderCRLF);
                while (indexReq != -1) {
                    indexReq += (Consts.HeaderCRLF + Consts.HeaderCRLF).length();
                    String textToDecode = receivedText.substring(0, indexReq);
                    receivedText = new StringBuilder(receivedText.substring(indexReq, receivedText.length()));
                    if (!textToDecode.equals("")) {
                        this.log.write(Level.FINE, String.format(this.messages.getString("HttpReceived"), textToDecode));
                        parseHttpRequest(textToDecode);
                    }
                    indexReq = receivedText.indexOf(Consts.HeaderCRLF + Consts.HeaderCRLF);
                }
            }
        } catch (AsynchronousCloseException ex) {
            this.log.write(Level.WARNING, this.messages.getString("HttpClientViolentlyDisconected"));
        } catch (IOException ex) {
            this.log.write(Level.SEVERE, String.format(this.messages.getString("HttpClientSocketException"), ex.toString()));
        } finally {
            synchronized (this.server.getClients()) {
                this.server.getClients().remove(this);
            }
            try {
                this.socket.socket().close();
                this.socket.close();
                this.log.write(Level.INFO, String.format(this.messages.getString("HttpClientClosedSocket"), this.remoteAddress));
                this.log.write(Level.INFO, this.messages.getString("ThreadStopped"));
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Parses http request
     *
     * @param text Receieved request
     */
    private void parseHttpRequest(String text) {
        try {
            String parsedText = text.substring(0, text.indexOf(Consts.HeaderCRLF));
            int currentIndex = 0;
            int lastIndex = 0;
            int extensionIndex = parsedText.length() - 1;
            String appDir = null;
            String method = null, id = "", extension = "";
            while (currentIndex < parsedText.length()) {
                if (parsedText.charAt(currentIndex) == ' ') {
                    break;
                }
                currentIndex++;
            }
            method = parsedText.substring(lastIndex, currentIndex);
            currentIndex += 2;
            lastIndex = currentIndex;
            int currentIndex2 = currentIndex;
            while (currentIndex2 < parsedText.length()) {
                if (parsedText.charAt(currentIndex2) == '/') {
                    appDir = parsedText.substring(lastIndex, currentIndex2);
                    currentIndex2++;
                    currentIndex = currentIndex2;
                    lastIndex = currentIndex2;
                    break;
                } else if ((parsedText.charAt(currentIndex2) == ' ') || parsedText.charAt(currentIndex2) == '?') {
                    appDir = parsedText.substring(lastIndex, currentIndex2);
                    sendErrorResponse(appDir, extension);
                    return;
                }
                currentIndex2++;
            }
            while (currentIndex < parsedText.length()) {
                if (parsedText.charAt(currentIndex) == '.') {
                    extensionIndex = currentIndex + 1;
                } else if ((parsedText.charAt(currentIndex) == ' ') || parsedText.charAt(currentIndex) == '?') {
                    break;
                }
                currentIndex++;
            }
            id = parsedText.substring(lastIndex, extensionIndex - 1);
            id = URLDecoder.decode(id, "UTF-8");
            extension = parsedText.substring(extensionIndex, currentIndex);
            sendResponse(appDir, id, extension, method);
        } catch (Exception ex) {
            this.log.write(Level.SEVERE, String.format(this.messages.getString("HttpErrorParsing"), ex.toString()));
        }
    }

    /**
     * Method for sending appropriate http response
     *
     * @param id file name without file extension
     * @param extension file extension
     * @param method HTTP method
     * @param appDir id of application this response bellongs to
     * @throws InterruptedException operation interrupted
     */
    private void sendResponse(String appDir, String id, String extension, String method) throws InterruptedException {
        if (method != null && method.equals("GET")) {
            if (appDir == null) {
                sendErrorResponse(id, extension);
            } else {
                IUipFile getFile;
                try {
                    getFile = this.filesAccess.getFile(appDir + "/" + this.mediaRoot + "/" + id + "." + extension);
                    sendBytesResponse(extension, getFile);
                } catch (IOException ex) {
                    sendErrorResponse(id, extension);
                }
            }
        } else {
            sendErrorResponse(id, extension);
        }
    }

    /**
     * Sends the response into socket
     *
     * @param extension file extension
     * @param getFile requested file
     * @throws InterruptedException operation interrupted
     */
    private void sendBytesResponse(String extension, IUipFile getFile) throws InterruptedException {
        StringBuilder responseHeader = new StringBuilder(Consts.HeaderMaxLength);
        synchronized (this.sendBuffer) {
            try {
                int fileLength = (int) getFile.length();
                responseHeader.append(Consts.HeaderOk);
                responseHeader.append(Consts.HeaderServer);
                responseHeader.append(Consts.HeaderAcceptRanges);
                responseHeader.append(Consts.HeaderContentLength);
                responseHeader.append(fileLength);
                responseHeader.append(Consts.HeaderCRLF);
                responseHeader.append(Consts.HeaderKeepAlive);
                responseHeader.append(Consts.HeaderConnection);
                if (extension.equals(Consts.UIExtension)) {
                    responseHeader.append(Consts.HeaderContentTypeFlash);
                } else {
                    responseHeader.append(Consts.HeaderContentTypeOctetStream);
                }
                responseHeader.append(Consts.HeaderCRLF);
                String responseHeaderString = responseHeader.toString();
                byte[] bytes = responseHeaderString.getBytes(Charset.forName("UTF-8"));
                if (bytes.length > Consts.HeaderMaxLength) {
                    this.log.write(Level.SEVERE, this.messages.getString("HttpResponseMaxHeaderSizeReached"));
                    return;
                }
                int repHeaderSend = bytes.length / Consts.HttpClientBufferSize;
                int lenHeaderRest = bytes.length % Consts.HttpClientBufferSize;
                int sentBytes = 0;
                int sendBytesCount = bytes.length + fileLength;
                byte fileBytes[] = null;
                for (int i = 0; i < repHeaderSend; i++) {
                    this.sendBuffer.clear();
                    this.sendBuffer.put(bytes, sentBytes, Consts.HttpClientBufferSize);
                    this.sendBuffer.flip();
                    sentBytes += Consts.HttpClientBufferSize;
                    int n = 0;
                    while (n < Consts.HttpClientBufferSize) {
                        n += this.socket.write(this.sendBuffer);
                    }
                }
                if (lenHeaderRest > 0) {
                    this.sendBuffer.clear();
                    this.sendBuffer.put(bytes, sentBytes, lenHeaderRest);
                    fileBytes = getFile.getBytes(Consts.HttpClientBufferSize - lenHeaderRest);
                    this.sendBuffer.put(fileBytes);
                    this.sendBuffer.flip();
                    sentBytes += lenHeaderRest + fileBytes.length;
                    int n = 0;
                    while (n < lenHeaderRest + fileBytes.length) {
                        n += this.socket.write(this.sendBuffer);
                    }
                }
                while ((fileBytes = getFile.getBytes(Consts.HttpClientBufferSize)) != null) {
                    this.sendBuffer.clear();
                    this.sendBuffer.put(fileBytes);
                    this.sendBuffer.flip();
                    sentBytes += fileBytes.length;
                    int n = 0;
                    while (n < fileBytes.length) {
                        n += this.socket.write(this.sendBuffer);
                    }
                }
                this.log.write(Level.INFO, String.format(this.messages.getString("HttpClientResponse"), sendBytesCount, this.remoteAddress));
                this.log.write(Level.FINE, String.format(this.messages.getString("HttpSent"), responseHeaderString));
            } catch (IOException ex) {
                getFile.dispose();
                this.log.write(Level.SEVERE, String.format(this.messages.getString("HttpErrorCanNotSend"), ex.toString()));
            }
        }
    }

    /**
     * Sends the error response 404 - file not found into socket
     *
     * @param extension file extension
     * @param id name of file
     */
    private void sendErrorResponse(String id, String extension) {
        StringBuilder responseHeader = new StringBuilder(Consts.HeaderMaxLength);
        synchronized (this.sendBuffer) {
            try {
                String htmlResponse = Consts.HtmlDocType + String.format(Consts.HtmlFileNotFoud, id, extension);
                byte[] bytesResponse = htmlResponse.getBytes(Charset.forName("UTF-8"));
                responseHeader.append(Consts.HeaderFileNotFound);
                responseHeader.append(Consts.HeaderServer);
                responseHeader.append(Consts.HeaderAcceptRanges);
                responseHeader.append(Consts.HeaderContentLength);
                responseHeader.append(bytesResponse.length);
                responseHeader.append(Consts.HeaderCRLF);
                responseHeader.append(Consts.HeaderKeepAlive);
                responseHeader.append(Consts.HeaderConnection);
                responseHeader.append(Consts.HeaderContentTypeHtml);
                responseHeader.append(Consts.HeaderCRLF);
                String responseHeaderString = responseHeader.toString();
                byte[] bytes = responseHeaderString.getBytes(Charset.forName("UTF-8"));
                if (bytes.length > Consts.HeaderMaxLength) {
                    this.log.write(Level.SEVERE, this.messages.getString("HttpResponseMaxHeaderSizeReached"));
                    return;
                }
                byte[] sendBytes = (responseHeaderString + htmlResponse).getBytes(Charset.forName("UTF-8"));
                int sentBytes = 0;
                int sendBytesCount = sendBytes.length;
                while (sentBytes < sendBytesCount) {
                    int len = Math.min(Consts.HttpClientBufferSize, sendBytes.length - sentBytes);
                    this.sendBuffer.clear();
                    this.sendBuffer.put(sendBytes, sentBytes, len);
                    this.sendBuffer.flip();
                    sentBytes += len;
                    int n = 0;
                    while (n < len) {
                        n += this.socket.write(this.sendBuffer);
                    }
                }
                this.log.write(Level.INFO, String.format(this.messages.getString("HttpClientResponse"), sendBytesCount, this.remoteAddress));
                this.log.write(Level.FINE, String.format(this.messages.getString("HttpSent"), (responseHeaderString + htmlResponse)));
            } catch (IOException ex) {
                this.log.write(Level.SEVERE, String.format(this.messages.getString("HttpErrorCanNotSend"), ex.toString()));
            }
        }
    }
}

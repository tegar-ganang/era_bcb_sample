package uips.uipserver;

import uips.support.Messages;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.logging.Level;
import uips.support.Consts;
import uips.support.Log;
import uips.support.Settings;
import uips.support.uipfiles.IUipFile;
import uips.support.uipfiles.UipFilesAccess;

/**
 * Class processing client HTTP requests and sending responses.
 * <br><br>
 * Based on Miroslav Macik's C# version of UIProtocolServer
 *
 * @author Miroslav Macik (macikm1@fel.cvut.cz, CTU Prague,  FEE)
 * @author Jindrich Basek (basekjin@fel.cvut.cz, CTU Prague,  FEE)
 */
public class HttpClient implements Runnable {

    /**
     * HTTP server listening for new client connections
     */
    private final HttpServerListener server;

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
     * this client belongs to HTTP server of instance (true), client belongs
     * to HTTP server common for all instances (false)
     */
    private boolean instanceServer;

    /**
     * Buffer for sending data
     */
    private final ByteBuffer sendBuffer;

    /**
     * Disposes HTTP client thread and closes connection
     */
    public void dispose() {
        started = false;
        try {
            socket.socket().close();
            socket.close();
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
    public HttpClient(HttpServerListener server, SocketChannel socket, boolean instanceServer) {
        this.instanceServer = instanceServer;
        this.server = server;
        this.socket = socket;
        sendBuffer = ByteBuffer.allocateDirect(Consts.HttpClientBufferSize);
        decoder = Charset.forName("UTF-8").newDecoder();
        remoteAddress = socket.socket().getRemoteSocketAddress().toString();
        clientThread = new Thread(this);
    }

    /**
     * Starts thread communicating with client
     */
    public void start() {
        clientThread.start();
    }

    /**
     * Processes http client request and sends response.
     */
    @Override
    public void run() {
        try {
            StringBuilder receivedText = new StringBuilder(Consts.HttpClientBufferSize * 5);
            ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(Consts.HttpClientBufferSize);
            while (started) {
                receiveBuffer.clear();
                int receivedBytesCount = socket.read(receiveBuffer);
                if (receivedBytesCount == -1 || receivedBytesCount == 0) {
                    break;
                }
                receiveBuffer.flip();
                receivedText.append(decoder.decode(receiveBuffer).toString());
                Log.write(Level.INFO, "HttpClient", "Receive", String.format(Messages.getString("HttpClientRequest"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), receivedBytesCount, remoteAddress));
                int indexReq = receivedText.indexOf(Consts.HeaderCRLF + Consts.HeaderCRLF);
                while (indexReq != -1) {
                    indexReq += (Consts.HeaderCRLF + Consts.HeaderCRLF).length();
                    String textToDecode = receivedText.substring(0, indexReq);
                    receivedText = new StringBuilder(receivedText.substring(indexReq, receivedText.length()));
                    if (!textToDecode.equals("")) {
                        Log.write(Level.FINE, String.format(Messages.getString("HttpReceived"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), textToDecode));
                        parseHttpRequest(textToDecode);
                    }
                    indexReq = receivedText.indexOf(Consts.HeaderCRLF + Consts.HeaderCRLF);
                }
            }
        } catch (AsynchronousCloseException ex) {
            Log.write(Level.WARNING, "HttpClient", "Receive", Messages.getString("HttpClientViolentlyDisconected"));
        } catch (IOException ex) {
            Log.write(Level.SEVERE, "HttpClient", "Receive", String.format(Messages.getString("HttpClientSocketException"), ex.toString(), ((server.getInstanceId() == null) ? "" : server.getInstanceId())));
        } finally {
            synchronized (server.getClients()) {
                server.getClients().remove(this);
            }
            try {
                socket.socket().close();
                socket.close();
                Log.write(Level.INFO, String.format(Messages.getString("HttpClientClosedSocket"), remoteAddress, ((server.getInstanceId() == null) ? "" : server.getInstanceId())));
                Log.write(Level.INFO, "HttpClient", ((server.getInstanceId() == null) ? "" : server.getInstanceId()), Messages.getString("ThreadStopped"));
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
            text = text.substring(0, text.indexOf(Consts.HeaderCRLF));
            int currentIndex = 0;
            int lastIndex = 0;
            int extensionIndex = text.length() - 1;
            String appDir = null;
            String method = null, id = "", extension = "";
            while (currentIndex < text.length()) {
                if (text.charAt(currentIndex) == ' ') {
                    break;
                }
                currentIndex++;
            }
            method = text.substring(lastIndex, currentIndex);
            currentIndex += 2;
            lastIndex = currentIndex;
            if (instanceServer) {
                int currentIndex2 = currentIndex;
                appDir = server.getAppDir();
                while (currentIndex2 < text.length()) {
                    if (text.charAt(currentIndex2) == '/') {
                        if (appDir.equals(text.substring(lastIndex, currentIndex2))) {
                            currentIndex2++;
                            currentIndex = currentIndex2;
                            lastIndex = currentIndex2;
                            break;
                        } else {
                            break;
                        }
                    } else if ((text.charAt(currentIndex2) == ' ') || text.charAt(currentIndex2) == '?') {
                        break;
                    }
                    currentIndex2++;
                }
            } else {
                int currentIndex2 = currentIndex;
                while (currentIndex2 < text.length()) {
                    if (text.charAt(currentIndex2) == '/') {
                        appDir = text.substring(lastIndex, currentIndex2);
                        currentIndex2++;
                        currentIndex = currentIndex2;
                        lastIndex = currentIndex2;
                        break;
                    } else if ((text.charAt(currentIndex2) == ' ') || text.charAt(currentIndex2) == '?') {
                        break;
                    }
                    currentIndex2++;
                }
            }
            while (currentIndex < text.length()) {
                if (text.charAt(currentIndex) == '.') {
                    extensionIndex = currentIndex + 1;
                } else if ((text.charAt(currentIndex) == ' ') || text.charAt(currentIndex) == '?') {
                    break;
                }
                currentIndex++;
            }
            id = text.substring(lastIndex, extensionIndex - 1);
            id = URLDecoder.decode(id, "UTF-8");
            extension = text.substring(extensionIndex, currentIndex);
            sendResponse(appDir, id, extension, method);
        } catch (Exception ex) {
            Log.write(Level.SEVERE, "HttpClient", "ParseHttpRequest", String.format(Messages.getString("HttpErrorParsing"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), ex.toString()));
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
                    getFile = UipFilesAccess.getFile(appDir + "/" + Settings.getMediaRoot() + "/" + id + "." + extension);
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
        synchronized (sendBuffer) {
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
                    Log.write(Level.SEVERE, "HttpClient", "SendResponseBytes", Messages.getString("HttpResponseMaxHeaderSizeReached"));
                    return;
                }
                int repHeaderSend = bytes.length / Consts.HttpClientBufferSize;
                int lenHeaderRest = bytes.length % Consts.HttpClientBufferSize;
                int sentBytes = 0;
                int sendBytesCount = bytes.length + fileLength;
                byte fileBytes[] = null;
                for (int i = 0; i < repHeaderSend; i++) {
                    sendBuffer.clear();
                    sendBuffer.put(bytes, sentBytes, Consts.HttpClientBufferSize);
                    sendBuffer.flip();
                    sentBytes += Consts.HttpClientBufferSize;
                    int n = 0;
                    while (n < Consts.HttpClientBufferSize) {
                        n += socket.write(sendBuffer);
                    }
                }
                if (lenHeaderRest > 0) {
                    sendBuffer.clear();
                    sendBuffer.put(bytes, sentBytes, lenHeaderRest);
                    fileBytes = getFile.getBytes(Consts.HttpClientBufferSize - lenHeaderRest);
                    sendBuffer.put(fileBytes);
                    sendBuffer.flip();
                    sentBytes += lenHeaderRest + fileBytes.length;
                    int n = 0;
                    while (n < lenHeaderRest + fileBytes.length) {
                        n += socket.write(sendBuffer);
                    }
                }
                while ((fileBytes = getFile.getBytes(Consts.HttpClientBufferSize)) != null) {
                    sendBuffer.clear();
                    sendBuffer.put(fileBytes);
                    sendBuffer.flip();
                    sentBytes += fileBytes.length;
                    int n = 0;
                    while (n < fileBytes.length) {
                        n += socket.write(sendBuffer);
                    }
                }
                Log.write(Level.INFO, "HttpClient", "SendResponseBytes", String.format(Messages.getString("HttpClientResponse"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), sendBytesCount, remoteAddress));
                Log.write(Level.FINE, String.format(Messages.getString("HttpSent"), responseHeaderString));
            } catch (IOException ex) {
                getFile.dispose();
                Log.write(Level.SEVERE, "HttpClient", "SendResponseBytes", String.format(Messages.getString("HttpErrorCanNotSend"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), ex.toString()));
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
        synchronized (sendBuffer) {
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
                    Log.write(Level.SEVERE, "HttpClient", "SendResponseBytes", Messages.getString("HttpResponseMaxHeaderSizeReached"));
                    return;
                }
                byte[] sendBytes = (responseHeaderString + htmlResponse).getBytes(Charset.forName("UTF-8"));
                int sentBytes = 0;
                int sendBytesCount = sendBytes.length;
                while (sentBytes < sendBytesCount) {
                    int len = Math.min(Consts.HttpClientBufferSize, sendBytes.length - sentBytes);
                    sendBuffer.clear();
                    sendBuffer.put(sendBytes, sentBytes, len);
                    sendBuffer.flip();
                    sentBytes += len;
                    int n = 0;
                    while (n < len) {
                        n += socket.write(sendBuffer);
                    }
                }
                Log.write(Level.INFO, "HttpClient", "SendResponseBytes", String.format(Messages.getString("HttpClientResponse"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), sendBytesCount, remoteAddress));
                Log.write(Level.FINE, String.format(Messages.getString("HttpSent"), (responseHeaderString + htmlResponse)));
            } catch (IOException ex) {
                Log.write(Level.SEVERE, "HttpClient", "SendResponseBytes", String.format(Messages.getString("HttpErrorCanNotSend"), ((server.getInstanceId() == null) ? "" : server.getInstanceId()), ex.toString()));
            }
        }
    }
}

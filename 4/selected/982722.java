package uips.ipcportal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import javax.net.ssl.SSLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import uips.support.Consts;
import uips.support.Log;
import uips.support.Messages;

/**
 * Class processing client signal requests and sending responses.
 * <br><br>
 * Based on Miroslav Macik's C# version of UIProtocolServer
 *
 * @author Jindrich Basek (basekjin@fel.cvut.cz, CTU Prague,  FEE)
 */
public class SignalClient implements Runnable {

    /**
     * Signal server listening for new client connections
     */
    private final SignalServerListener server;

    /**
     * Client communication socket
     */
    private final Socket socket;

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
    private boolean started;

    /**
     * Output socket stream
     */
    private BufferedOutputStream output;

    /**
     * Input socket stream
     */
    private BufferedInputStream input;

    /**
     * Class constructor, creates new thread communicating with client.
     *
     * @param server Signal server listening for new client connections
     * @param socket Client communication socket
     */
    public SignalClient(SignalServerListener server, Socket socket) {
        this.server = server;
        this.socket = socket;
        remoteAddress = socket.getRemoteSocketAddress().toString();
        started = true;
        clientThread = new Thread(this);
        try {
            output = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            output = null;
        }
        try {
            input = new BufferedInputStream(socket.getInputStream());
        } catch (IOException ex) {
            input = null;
        }
    }

    /**
     * Starts thread communicating with client
     */
    public void start() {
        clientThread.start();
    }

    /**
     * Disposes communication chanel and thread
     */
    public void dispose() {
        started = false;
        try {
            socket.close();
        } catch (Exception ex) {
        }
    }

    /**
     * Method receiving data from aclient and starting parse of received signal
     */
    @Override
    public void run() {
        try {
            StringBuilder receivedText = new StringBuilder(Consts.SignalClientBufferSize * 5);
            byte receiveBuffer[] = new byte[Consts.SignalClientBufferSize];
            while (started && input != null) {
                int receivedBytesCount = input.read(receiveBuffer);
                if (receivedBytesCount == -1) {
                    Log.write(Level.WARNING, "SignalClient", "Receive", String.format(Messages.getString("SignalClientClosedSocket"), remoteAddress));
                    break;
                }
                Log.write(Level.INFO, "SignalClient", "Receive", String.format(Messages.getString("SignalClientReceivedBytes"), receivedBytesCount, remoteAddress));
                receivedText.append(new String(receiveBuffer, 0, receivedBytesCount, "UTF-8"));
                int indexReq = receivedText.indexOf(String.valueOf(Consts.NullByteChar));
                while (indexReq != -1) {
                    indexReq++;
                    String textToDecode = receivedText.substring(0, indexReq - 1);
                    receivedText = new StringBuilder(receivedText.substring(indexReq, receivedText.length()));
                    if (!textToDecode.equals("")) {
                        Log.write(Level.FINE, "SignalClient", "Receive", String.format(Messages.getString("SignalReceived"), textToDecode));
                        try {
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document document = dBuilder.parse(new ByteArrayInputStream(textToDecode.getBytes("UTF-8")));
                            parseSignal(document);
                        } catch (ParserConfigurationException ex) {
                            Log.write(Level.SEVERE, "SignalClient", "Receive", ex.toString());
                        } catch (SAXParseException ex) {
                            Log.write(Level.SEVERE, "SignalClient", "Receive", ex.toString());
                        } catch (SAXException ex) {
                            Log.write(Level.SEVERE, "SignalClient", "Receive", String.format("%s,%s", Consts.ErrorXmlSchemaValidation, ex.toString()));
                        }
                    }
                    indexReq = receivedText.indexOf(String.valueOf(Consts.NullByteChar));
                }
            }
        } catch (SSLException ex) {
        } catch (IOException ex) {
            Log.write(Level.SEVERE, "SignalClient", "Receive", String.format(Messages.getString("SignalClientSocketException"), ex.toString()));
        } finally {
            server.getClients().remove(this);
            try {
                socket.close();
                Log.write(Level.INFO, "SignalClient", "Receive", Messages.getString("ThreadStopped"));
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Parses received signal and starts appropriate signal handler if exists
     *
     * @param document received signal in DOM tree (signals are XML text)
     */
    private void parseSignal(Document document) {
        if (started) {
            document.getDocumentElement().normalize();
            Signal signal = new Signal(document.getDocumentElement());
            try {
                Class<?> sigHandlerClass = Class.forName("uips.ipcportal." + signal.getName() + "SigHandler");
                ISignalHandler sigHandler = (ISignalHandler) sigHandlerClass.newInstance();
                sigHandler.handle(signal, this);
            } catch (Exception ex) {
                Log.write(Level.WARNING, "SignalClient", "parseSignal", String.format(Messages.getString("SignalNotRecognized"), signal.getName()));
            }
        }
    }

    /**
     * Sends text (char sequence to client)
     *
     * @param text text to send
     * @return send bytes count
     */
    public int sendTextResponse(String text) {
        int sendBytesCount = 0;
        if (output != null) {
            synchronized (output) {
                try {
                    int sentBytes = 0;
                    byte sendBuffer[] = text.trim().getBytes("UTF-8");
                    sendBytesCount = sendBuffer.length;
                    while (sentBytes < sendBytesCount) {
                        int len = Math.min(Consts.SignalClientBufferSize - 1, sendBuffer.length - sentBytes);
                        output.write(sendBuffer, sentBytes, len);
                        sentBytes += len;
                        if (sentBytes >= sendBytesCount) {
                            output.write(Consts.NullByte);
                            len++;
                        }
                        output.flush();
                    }
                    Log.write(Level.INFO, String.format(Messages.getString("SignalClientSentBytes"), sendBytesCount, remoteAddress));
                    Log.write(Level.FINE, "SignalClient", "SendTextResponse", String.format("\"%s\"", text.trim()));
                } catch (Exception ex) {
                    Log.write(Level.SEVERE, "SignalClient", "SendTextResponse", ex.toString());
                }
            }
        }
        return sendBytesCount;
    }
}

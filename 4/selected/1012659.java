package uips.uipserver;

import uips.support.Messages;
import uips.instances.Instance;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;
import uips.models.Model;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import uips.support.Consts;
import uips.support.Log;
import uips.support.Settings;
import uips.support.database.DatabaseAccess;

/**
 * Class processing xml client requests and sending response.
 * <br><br>
 * Based on Miroslav Macik's C# version of UIProtocolServer
 *
 * @author Miroslav Macik (macikm1@fel.cvut.cz, CTU Prague,  FEE)
 * @author Jindrich Basek (basekjin@fel.cvut.cz, CTU Prague,  FEE)
 */
public class XmlClient implements Runnable {

    /**
     * server listening for new client connections
     */
    private final XmlServerListener server;

    /**
     * Client communication socket
     */
    private final SocketChannel socket;

    /**
     * Buffer for sending data
     */
    private final ByteBuffer sendBuffer;

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
     * Instance of ClientInfo containg informations about connected client
     */
    private ClientInfo clientInfo;

    /**
     * Charset decoded
     */
    private final CharsetDecoder decoder;

    /**
     * Instance to that client belongs, null if instance not recognized yet
     * or not found
     */
    private Instance mainInstance;

    /**
     * Map of private models of this client (key - model ID,
     * value - Model instance)
     */
    private final Map<String, Model> privateModels;

    /**
     * Map of private models of this client (key - model ID,
     * value - Model instance)
     *
     * @return Map of private models of this client (key - model ID,
     * value - Model instance)
     */
    public Map<String, Model> getPrivateModels() {
        return privateModels;
    }

    /**
     * Sets instance to that client belongs
     *
     * @param mainInstance Instance to that client belongs
     */
    public void setMainInstance(Instance mainInstance) {
        this.mainInstance = mainInstance;
    }

    /**
     * Instance to that client belongs, null if instance not recognized yet
     * or not found
     *
     * @return Instance to that client belongs, null if instance not
     * recognized yet or not found
     */
    public Instance getMainInstance() {
        return mainInstance;
    }

    /**
     * Disposes communication chanel and thread
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
     * Instance of ClientInfo containg informations about connected client
     *
     * @return Instance of ClientInfo containg informations about
     * connected client
     */
    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    /**
     * Sets instance of ClientInfo containg informations about connected client
     * @param clientInfo Instance of ClientInfo containg informations about
     * connected client
     */
    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    /**
     * Constructor of XmlClient
     *
     * @param server Reference to xml server
     * @param socket Client's socket
     */
    public XmlClient(XmlServerListener server, SocketChannel socket) {
        this.server = server;
        this.socket = socket;
        privateModels = Collections.synchronizedMap(new HashMap<String, Model>());
        decoder = Charset.forName("UTF-8").newDecoder();
        sendBuffer = ByteBuffer.allocateDirect(Consts.XmlClientBufferSize);
        remoteAddress = socket.socket().getRemoteSocketAddress().toString();
        clientInfo = null;
        clientThread = new Thread(this);
    }

    /**
     * Starts thread communicating with client
     */
    public void start() {
        clientThread.start();
    }

    /**
     * Parses XML sent by client. If client sent UIP event, event is handlet
     * by InstanceManager if client does not belongs to some instance
     * or by Instance.
     *
     * @param document DOM representation of received XML
     */
    private void parseXmlRequest(Document document) {
        if (started) {
            document.getDocumentElement().normalize();
            Command command = new Command(document.getDocumentElement());
            switch(command.getType()) {
                case Event:
                    while (UipServer.getUipsInstance().getInstancesManager() == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                        }
                    }
                    if (mainInstance == null) {
                        try {
                            UipServer.getUipsInstance().getInstancesManager().handleEvent(command, this);
                        } catch (Exception ex) {
                            Log.write(Level.SEVERE, "XmlClient", "SendResponse", ex.toString(), this);
                            sendErrorResponse(Consts.ErrorUserNotConnected, Consts.PropertyValueError, ex.getMessage());
                            started = false;
                        }
                    }
                    if (started) {
                        mainInstance.handleEvent(command, this);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Sends response to client
     *
     * @param command response to send
     */
    public void sendResponse(Command command) {
        try {
            sendTextResponse(command.getXmlText());
            Log.write(Level.FINE, "XmlClient", "SendResponse", Messages.getString("Finished"), this);
        } catch (Exception ex) {
            Log.write(Level.SEVERE, "XmlClient", "SendResponse", ex.toString(), this);
        }
    }

    /**
     * Sends error response to client
     *
     * @param eventx type of error
     * @param severity error severity
     * @param description error message
     */
    public void sendErrorResponse(String eventx, String severity, String description) {
        try {
            Command command = new Command(eventx, severity, description);
            sendTextResponse(command.getXmlText());
            Log.write(Level.FINE, "XmlClient", "SendErrorResponse", Messages.getString("Finished"), this);
        } catch (Exception ex) {
            Log.write(Level.SEVERE, "XmlClient", "SendErrorResponse", ex.toString(), this);
        }
    }

    /**
     * Receive method
     */
    @Override
    public void run() {
        try {
            if (UipServer.getUipsInstance().getInstancesManager() != null) {
                if (UipServer.getUipsInstance().getUipsStatus() != UipsStatus.Started) {
                    throw new UipsException(String.format(Messages.getString("ErrorUipsNotInitializedBadStatus"), UipServer.getUipsInstance().getUipsStatus()));
                }
            } else {
                throw new UipsException(Messages.getString("ErrorUipsNotInitializedNoManager"));
            }
            StringBuilder receivedText = new StringBuilder(Consts.XmlClientBufferSize * 5);
            ByteBuffer receiveBuffer = ByteBuffer.allocateDirect(Consts.XmlClientBufferSize);
            Validator validator = Settings.getXmlReaderSettings().newValidator();
            while (started) {
                receiveBuffer.clear();
                int receivedBytesCount = socket.read(receiveBuffer);
                if (receivedBytesCount == -1) {
                    Log.write(Level.WARNING, "XmlClient", "Receive", String.format(Messages.getString("XmlClientClosedSocket"), remoteAddress), this);
                    break;
                }
                receiveBuffer.flip();
                receivedText.append(decoder.decode(receiveBuffer).toString());
                Log.write(Level.INFO, "XmlClient", "Receive", String.format(Messages.getString("XmlClientReceivedBytes"), receivedBytesCount, remoteAddress), this);
                String xmlEnd = "</" + Consts.ElementNameUIProtocol + ">";
                int indexEnd = receivedText.indexOf(xmlEnd + Consts.NullByteChar);
                int indexStart = 0;
                if (indexEnd != -1) {
                    indexEnd += xmlEnd.length();
                    indexStart = indexEnd + 1;
                }
                if (indexEnd == -1) {
                    indexEnd = receivedText.indexOf(String.valueOf(Consts.NullByteChar));
                    if (indexEnd != -1) {
                        indexStart = indexEnd + 1;
                    }
                    if (indexEnd == -1) {
                        indexEnd = receivedText.indexOf(xmlEnd);
                        if (indexEnd != -1) {
                            indexEnd += xmlEnd.length();
                            indexStart = indexEnd;
                        }
                    }
                }
                while (indexEnd != -1) {
                    String textToDecode = receivedText.substring(0, indexEnd);
                    receivedText = new StringBuilder(receivedText.substring(indexStart, receivedText.length()));
                    if (!textToDecode.equals("")) {
                        Log.write(Level.FINE, "XmlClient", "Receive", String.format(Messages.getString("XmlReceived"), textToDecode), this);
                        try {
                            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                            Document document = dBuilder.parse(new ByteArrayInputStream(textToDecode.getBytes("UTF-8")));
                            document.getDocumentElement().normalize();
                            if (document.getDocumentElement().getNodeType() == Node.ELEMENT_NODE && document.getDocumentElement().getNodeName().equals(Consts.ElementNameUIProtocol)) {
                                if (document.getDocumentElement().hasAttribute("xmlns")) {
                                    document.getDocumentElement().removeAttribute("xmlns");
                                }
                                if (document.getDocumentElement().hasAttribute("xmlns:xsi")) {
                                    document.getDocumentElement().removeAttribute("xmlns:xsi");
                                }
                                if (document.getDocumentElement().hasAttribute("xsi:noNamespaceSchemaLocation")) {
                                    document.getDocumentElement().removeAttribute("xsi:noNamespaceSchemaLocation");
                                }
                            }
                            DOMSource domSource = new DOMSource(document);
                            validator.validate(domSource);
                            parseXmlRequest(document);
                        } catch (ParserConfigurationException ex) {
                            Log.write(Level.SEVERE, "XmlClient", "Receive", ex.toString(), this);
                        } catch (SAXParseException ex) {
                            Log.write(Level.SEVERE, "XmlClient", "Receive", ex.toString(), this);
                            sendErrorResponse("XmlException", Consts.PropertyValueError, String.format(Messages.getString("ErrorParseXml"), ex.toString(), textToDecode));
                        } catch (SAXException ex) {
                            Log.write(Level.SEVERE, "XmlClient", "Receive", String.format("%s,%s", Consts.ErrorXmlSchemaValidation, ex.toString()), this);
                            sendErrorResponse(Consts.ErrorXmlSchemaValidation, Consts.PropertyValueError, ex.toString());
                        } catch (Exception ex) {
                            Log.write(Level.SEVERE, "XmlClient", "Receive", String.format("%s,%s", Consts.ErrorXmlSchemaValidation, ex.toString()), this);
                            sendErrorResponse(Consts.ErrorXmlSchemaValidation, Consts.PropertyValueError, ex.toString());
                        }
                    }
                    indexEnd = receivedText.indexOf(xmlEnd + Consts.NullByteChar);
                    indexStart = 0;
                    if (indexEnd != -1) {
                        indexEnd += xmlEnd.length();
                        indexStart = indexEnd + 1;
                    }
                    if (indexEnd == -1) {
                        indexEnd = receivedText.indexOf(String.valueOf(Consts.NullByteChar));
                        if (indexEnd != -1) {
                            indexStart = indexEnd + 1;
                        }
                        if (indexEnd == -1) {
                            indexEnd = receivedText.indexOf(xmlEnd);
                            if (indexEnd != -1) {
                                indexEnd += xmlEnd.length();
                                indexStart = indexEnd;
                            }
                        }
                    }
                }
            }
        } catch (UipsException ex) {
            sendErrorResponse(Consts.ErrorServerNotReady, Consts.PropertyValueError, ex.toString());
            Log.write(Level.SEVERE, "XmlClient", "Receive", ex.toString(), this);
        } catch (AsynchronousCloseException ex) {
            Log.write(Level.WARNING, "XmlClient", "Receive", Messages.getString("XmlClientViolentlyDisconected"), this);
        } catch (IOException ex) {
            Log.write(Level.SEVERE, "XmlClient", "Receive", String.format(Messages.getString("XmlClientSocketException"), ex.toString()), this);
            if (mainInstance != null) {
                mainInstance.clientDisconnected(this);
            }
        } finally {
            if (mainInstance != null) {
                mainInstance.removeClient(this);
            }
            try {
                socket.socket().close();
                socket.close();
                Log.write(Level.INFO, "XmlClient", ((mainInstance == null) ? "" : mainInstance.getInstanceId()), Messages.getString("ThreadStopped"), this);
            } catch (Exception ex) {
            }
            if (UipServer.getUipsInstance().getUipsStatus() != UipsStatus.Stopped && UipServer.getUipsInstance().getUipsStatus() != UipsStatus.Unknown) {
                if (mainInstance != null) {
                    DatabaseAccess.notifyInstanceStatus(mainInstance.getInstanceId());
                }
                DatabaseAccess.notifyServerStatus();
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
        synchronized (sendBuffer) {
            int sentBytes = 0;
            byte[] bytes = text.trim().getBytes(Charset.forName("UTF-8"));
            int sendBytesCount = bytes.length;
            try {
                while (sentBytes < sendBytesCount) {
                    int len = Math.min(Consts.XmlClientBufferSize - 1, bytes.length - sentBytes);
                    sendBuffer.clear();
                    sendBuffer.put(bytes, sentBytes, len);
                    sentBytes += len;
                    if (sentBytes >= sendBytesCount) {
                        sendBuffer.put(Consts.NullByte);
                        len++;
                    }
                    sendBuffer.flip();
                    int n = 0;
                    while (n < len) {
                        n += socket.write(sendBuffer);
                    }
                }
                Log.write(Level.INFO, String.format(Messages.getString("XmlClientSentBytes"), sendBytesCount, remoteAddress), this);
                Log.write(Level.FINE, "XmlClient", "SendTextResponse", text.trim(), this);
            } catch (Exception ex) {
                Log.write(Level.SEVERE, "XmlClient", "SendTextResponse", ex.toString(), this);
            }
            return sendBytesCount;
        }
    }
}

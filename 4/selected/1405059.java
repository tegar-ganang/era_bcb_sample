package uips.communication.uip.impl.xmlSsl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;
import java.util.logging.Level;
import javax.net.ssl.SSLException;
import javax.xml.bind.JAXBException;
import uips.clients.IGenericClient;
import uips.communication.uip.interfaces.IUipClient;
import uips.support.Consts;
import uips.support.localization.IMessagesInstance;
import uips.support.logging.ILogInstance;
import uips.tree.convertors.IInnerToOuterConv;
import uips.tree.inner.interfaces.IActionInn;
import uips.tree.inner.interfaces.IEventInn;
import uips.tree.inner.interfaces.IInterfaceInn;
import uips.tree.inner.interfaces.IModelInn;
import uips.tree.inner.interfaces.IUIProtocolInn;
import uips.tree.outer.factories.impl.xml.UipXmlTreeFactory;
import uips.tree.outer.impl.xml.ActionOut;
import uips.tree.outer.impl.xml.EventOut;
import uips.tree.outer.impl.xml.InterfaceOut;
import uips.tree.outer.impl.xml.ModelOut;
import uips.tree.outer.impl.xml.UIProtocolOut;

/**
 * Class processing client signal requests and sending responses.
 * <br><br>
 * Based on Miroslav Macik's C# version of UIProtocolServer
 *
 * @author Jindrich Basek (basekjin@fit.cvut.cz, CTU Prague, FIT)
 */
public class XmlSslClient implements IUipClient {

    /**
     * Client communication socket
     */
    private Socket socket;

    /**
     * Adress of connected client
     */
    private String remoteAddress;

    /**
     * This thread
     */
    private Thread clientThread;

    /**
     * Is client thread started?
     */
    private boolean started = true;

    /**
     * Output socket stream
     */
    private BufferedOutputStream output;

    /**
     * Input socket stream
     */
    private BufferedInputStream input;

    private IGenericClient genericClient;

    private IInnerToOuterConv innerToOuterConv;

    private final UipXmlTreeFactory uipXmlTreeFactory;

    private final ILogInstance log;

    private final IMessagesInstance messages;

    /**
     * Class constructor, creates new thread communicating with client.
     *
     * @param server Signal server listening for new client connections
     * @param socket Client communication socket
     */
    public XmlSslClient(Socket socket, UipXmlTreeFactory uipXmlTreeFactory, ILogInstance logInstance, IMessagesInstance messages) {
        this.messages = messages;
        this.log = logInstance;
        this.socket = socket;
        this.remoteAddress = socket.getRemoteSocketAddress().toString();
        this.clientThread = new Thread(this);
        this.uipXmlTreeFactory = uipXmlTreeFactory;
        try {
            this.output = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException ex) {
            this.output = null;
        }
        try {
            this.input = new BufferedInputStream(socket.getInputStream());
        } catch (IOException ex) {
            this.input = null;
        }
    }

    /**
     * Starts thread communicating with client
     */
    @Override
    public void initialize(IGenericClient genericClientIn, IInnerToOuterConv innerToOuterConvIn) {
        this.innerToOuterConv = innerToOuterConvIn;
        this.innerToOuterConv.initializeConvertor(this.uipXmlTreeFactory);
        this.genericClient = genericClientIn;
    }

    /**
     * Starts thread communicating with client
     */
    @Override
    public void start() {
        this.clientThread.start();
    }

    /**
     * Disposes communication chanel and thread
     */
    @Override
    public void dispose() {
        this.started = false;
        try {
            this.socket.close();
        } catch (Exception ex) {
        }
    }

    @Override
    public void sendUiProtocol(IUIProtocolInn uipInn) {
        try {
            UIProtocolOut uipOut = (UIProtocolOut) this.innerToOuterConv.convertUiprotocol(uipInn);
            String response = this.uipXmlTreeFactory.generateXml(uipOut);
            sendTextResponse(response);
            this.log.write(Level.FINE, this.messages.getString("Finished"), this.genericClient);
        } catch (Exception ex) {
            this.log.write(Level.SEVERE, ex.toString(), this.genericClient);
        }
    }

    @Override
    public void sendAction(IActionInn actionInn) {
        try {
            ActionOut actionOut = (ActionOut) this.innerToOuterConv.convertAction(actionInn);
            String response = this.uipXmlTreeFactory.generateXml(actionOut);
            sendTextResponse(response);
            this.log.write(Level.FINE, this.messages.getString("Finished"), this.genericClient);
        } catch (Exception ex) {
            this.log.write(Level.SEVERE, ex.toString(), this.genericClient);
        }
    }

    @Override
    public void sendModel(IModelInn modelInn) {
        try {
            ModelOut modelOut = (ModelOut) this.innerToOuterConv.convertModel(modelInn);
            String response = this.uipXmlTreeFactory.generateXml(modelOut);
            sendTextResponse(response);
            this.log.write(Level.FINE, this.messages.getString("Finished"), this.genericClient);
        } catch (Exception ex) {
            this.log.write(Level.SEVERE, ex.toString(), this.genericClient);
        }
    }

    @Override
    public void sendInterface(IInterfaceInn interfaceInn) {
        try {
            InterfaceOut interfaceOut = (InterfaceOut) this.innerToOuterConv.convertInterface(interfaceInn);
            String response = this.uipXmlTreeFactory.generateXml(interfaceOut);
            sendTextResponse(response);
            this.log.write(Level.FINE, this.messages.getString("Finished"), this.genericClient);
        } catch (Exception ex) {
            this.log.write(Level.SEVERE, ex.toString(), this.genericClient);
        }
    }

    @Override
    public void sendEvent(IEventInn eventInn) {
        try {
            EventOut eventOut = (EventOut) this.innerToOuterConv.convertEvent(eventInn);
            String response = this.uipXmlTreeFactory.generateXml(eventOut);
            sendTextResponse(response);
            this.log.write(Level.FINE, this.messages.getString("Finished"), this.genericClient);
        } catch (Exception ex) {
            this.log.write(Level.SEVERE, ex.toString(), this.genericClient);
        }
    }

    @Override
    public void run() {
        try {
            StringBuilder receivedText = new StringBuilder(Consts.XmlClientBufferSize * 5);
            byte receiveBuffer[] = new byte[Consts.XmlClientBufferSize];
            while (this.started && this.input != null) {
                int receivedBytesCount = this.input.read(receiveBuffer);
                if (receivedBytesCount == -1) {
                    this.log.write(Level.WARNING, String.format(this.messages.getString("XmlClientClosedSocket"), this.remoteAddress), this.genericClient);
                    break;
                }
                receivedText.append(new String(receiveBuffer, 0, receivedBytesCount, "UTF-8"));
                this.log.write(Level.INFO, String.format(this.messages.getString("XmlClientReceivedBytes"), receivedBytesCount, this.remoteAddress), this.genericClient);
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
                        this.log.write(Level.FINE, String.format(this.messages.getString("XmlReceived"), textToDecode), this.genericClient);
                        try {
                            UIProtocolOut uipOut = this.uipXmlTreeFactory.parseXml(textToDecode);
                            if (this.started) {
                                this.genericClient.handleMessage(uipOut);
                            }
                        } catch (JAXBException ex) {
                            this.log.write(Level.SEVERE, String.format("%s,%s", Consts.ErrorXmlSchemaValidation, ex.toString()), this.genericClient);
                            this.genericClient.sendErrorResponse(Consts.ErrorXmlSchemaValidation, Consts.PropertyValueError, ex.toString());
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
        } catch (SocketException ex) {
        } catch (SSLException ex) {
        } catch (AsynchronousCloseException ex) {
            this.log.write(Level.WARNING, this.messages.getString("XmlClientViolentlyDisconected"), this.genericClient);
        } catch (IOException ex) {
            this.log.write(Level.SEVERE, String.format(this.messages.getString("XmlClientSocketException"), ex.toString()), this.genericClient);
            this.genericClient.clientDisconnected();
        } finally {
            this.started = false;
            this.genericClient.removeClient();
            try {
                this.socket.close();
                this.log.write(Level.INFO, this.messages.getString("ThreadStopped"), this.genericClient);
            } catch (Exception ex) {
            }
            this.genericClient.communicationClosed();
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
        if (this.output != null) {
            synchronized (this.output) {
                try {
                    int sentBytes = 0;
                    byte sendBuffer[] = text.trim().getBytes("UTF-8");
                    sendBytesCount = sendBuffer.length;
                    while (sentBytes < sendBytesCount) {
                        int len = Math.min(Consts.XmlClientBufferSize - 1, sendBuffer.length - sentBytes);
                        this.output.write(sendBuffer, sentBytes, len);
                        sentBytes += len;
                        if (sentBytes >= sendBytesCount) {
                            this.output.write(Consts.NullByte);
                            len++;
                        }
                        this.output.flush();
                    }
                    this.log.write(Level.INFO, String.format(this.messages.getString("XmlClientSentBytes"), sendBytesCount, this.remoteAddress));
                    this.log.write(Level.FINE, text.trim(), this.genericClient);
                } catch (Exception ex) {
                    this.log.write(Level.SEVERE, ex.toString());
                }
            }
        }
        return sendBytesCount;
    }

    @Override
    public void stopCommunication() {
        this.started = false;
    }
}

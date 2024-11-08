package com.abiquo.framework.comm.transaction.xml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.log4j.Logger;
import org.codehaus.stax2.XMLStreamReader2;
import com.abiquo.framework.comm.CommunicationTime;
import com.abiquo.framework.comm.transaction.ITransaction;
import com.abiquo.framework.config.FrameworkConfiguration;
import com.abiquo.framework.xml.AbsXMLMessage;
import com.abiquo.framework.xml.EventConstants;
import com.abiquo.framework.xml.MessageEventAllocator;
import com.abiquo.util.OutputStreamDecoderWriter;

/**
 * This thread is used to read the incoming XML message form socket, when a event message is read it returns, but if
 * attached streaming is also available it is write into specified output Stream. Used on ActiveTransaction
 * PassiveTransaction. For ActiveTransaction it close communication at its end.
 */
public class TransactionReader extends Thread {

    /** The logger object. */
    private static final Logger logger = Logger.getLogger(TransactionReader.class);

    /** The transaction where set some of its messages */
    private ITransaction transaction;

    /** Used on XML Stax implementation to get events (AbsXMLMessage) from XML stream reader */
    protected final MessageEventAllocator messageAllocator;

    /** Reads from StreamTransaction. */
    private XMLStreamReader2 reader;

    /** The socket form RemoteTransaction, will shutdown input at end. */
    private Socket sock;

    /** As a thread can throw exception, it stores for getException *. */
    private Exception except;

    /** Used configuration */
    private FrameworkConfiguration configuration;

    /** Additional thread to handle output streaming data */
    private ReadXMLToOutputStream readXMLStream;

    /** Records the communication time */
    private CommunicationTime time;

    /**
	 * The class constructor .
	 * 
	 * @param tx
	 *            the transaction to read some of its messages (if active the response, if passive the request)
	 * 
	 * @param r
	 *            the Reader object used to obtain XML from socket input stream
	 * @param sock
	 *            the socket object
	 * @param configurationTx
	 *            to tune communication
	 * @param t
	 *            the time communication created on Active or Passive Communication
	 */
    public TransactionReader(ITransaction tx, XMLStreamReader2 r, Socket s, FrameworkConfiguration config, CommunicationTime t) {
        transaction = tx;
        time = t;
        reader = r;
        configuration = config;
        messageAllocator = new MessageEventAllocator(configuration);
        sock = s;
    }

    /**
	 * Perform the read operation. Return after some event message is read. For DataTransfers starts another thread
	 * ReadXMLToOutputStream to pull data to transaction output stream.
	 */
    public void run() {
        AbsXMLMessage message;
        time.setTimeStartRead();
        try {
            message = (AbsXMLMessage) messageAllocator.allocate((XMLStreamReader) reader);
            logger.debug("Read response message " + message.getClass().getName());
            if (transaction.isActive()) {
                transaction.setResponse(message.getMessage());
            } else {
                transaction.setRequest(message.getMessage());
            }
            boolean isRequiredReadStream = false;
            if (isRequiredReadStream) {
                readXMLStream = new ReadXMLToOutputStream();
                readXMLStream.start();
            } else {
                finishRead();
            }
        } catch (XMLStreamException e) {
            logger.error(e);
            e.printStackTrace();
            except = e;
        } catch (IOException e) {
            logger.error("closing socket ", e);
            except = e;
        }
    }

    /**
	 * Ends the XML communication transaction read step. For ActiveCommunication also close the socket.
	 */
    private void finishRead() throws XMLStreamException, IOException {
        time.setTimeEndRead();
        logger.debug("Closing reader");
        reader.close();
        logger.debug("and shutting down socket input");
        sock.shutdownInput();
        if (transaction.isActive()) {
            sock.close();
            time.logTimes();
        }
    }

    /**
	 * Throws an exception if some is set.
	 * 
	 * @throws Exception
	 *             the captured exception at run method.
	 */
    public void throwException() throws Exception {
        if (except != null) throw except;
    }

    /**
	 * Waits until available input stream.
	 * 
	 * @todo then call throwException?? must be there (2different exception)
	 */
    public void joinStreamReader() throws InterruptedException {
        if (readXMLStream == null) {
            logger.warn("no REadXMLToOutputStream thrad aviable");
        } else {
            readXMLStream.join();
        }
    }

    /**
	 * Thread to handle transaction output data stream. It obtain output content as base64 decoded characters from a
	 * Data.CData event.
	 */
    private class ReadXMLToOutputStream extends Thread {

        public void run() {
            logger.debug("start ReadXMLToOutputStream thread");
            Writer writeStream = null;
            try {
                writeStream = new OutputStreamDecoderWriter(transaction.getDataOutputStream());
            } catch (UnsupportedEncodingException e1) {
                logger.error("OutputStreamDecoderWriter require jvm platform to support US_ASCII charset ", e1);
                except = e1;
                return;
            }
            try {
                boolean bEndAttachData;
                int tokenType;
                logger.debug("Reading XML stream and printing to output stream");
                bEndAttachData = false;
                while (reader.hasNext() && !bEndAttachData) {
                    tokenType = reader.next();
                    if (tokenType == EventConstants.START_ELEMENT) {
                        logger.warn("Readed START_ELEMENT :" + "element text:" + reader.getElementText() + " localName:" + reader.getLocalName() + " Name:" + reader.getName());
                    } else if (tokenType == EventConstants.END_DOCUMENT) {
                        logger.debug("End document");
                        bEndAttachData = true;
                    } else if (tokenType == EventConstants.END_ELEMENT) {
                        logger.warn("Readed END_ELEMENT :" + " localName:" + reader.getLocalName() + " Name:" + reader.getName());
                        bEndAttachData = true;
                    } else if (tokenType == EventConstants.CHARACTERS || tokenType == EventConstants.CDATA) {
                        logger.debug("CHARACTERS events were found " + tokenType);
                        try {
                            int reads;
                            reads = reader.getText(writeStream, false);
                            writeStream.flush();
                            logger.debug("getText readed bytes " + reads);
                        } catch (IOException e) {
                            logger.error("Exception during Character getText ", e);
                            except = e;
                        }
                    } else {
                        logger.warn("another EventType " + tokenType);
                    }
                }
                logger.debug("No more characters events, end output");
                try {
                    logger.debug("Closing output writer");
                    writeStream.close();
                } catch (IOException e) {
                    logger.error("Error string stream writer ", e);
                    except = e;
                }
            } catch (XMLStreamException e) {
                logger.error("An XMLStreamException has ocurred", e);
                except = e;
            }
            logger.debug("ReadXMLToOutputStream done");
            try {
                finishRead();
            } catch (XMLStreamException e) {
                logger.error(e);
                e.printStackTrace();
                except = e;
            } catch (IOException e) {
                logger.error("closing socket ", e);
                except = e;
            }
        }
    }
}

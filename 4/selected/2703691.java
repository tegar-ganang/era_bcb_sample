package com.abiquo.framework.xml.transaction.stream;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import javax.xml.stream.XMLStreamException;
import org.apache.log4j.Logger;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.evt.XMLEvent2;
import com.abiquo.framework.xml.EventConstants;
import com.abiquo.framework.xml.Stax2Factories;

/**
 * Thread to handle transaction input data stream. It send the input content as base64 encoded characters inside a
 * Data.CData event.
 */
public class WriteXMLFromInputStream implements Callable<Boolean> {

    private static final Logger logger = Logger.getLogger(WriteXMLFromInputStream.class);

    /**
	 * cause write logger inputStream content to DATA_INPUT.txt
	 * 
	 * @deprecated TODO from config
	 */
    private final boolean bLogIS = false;

    /** Data source, bytes are read from there. */
    private InputStream input;

    /**
	 * Data XML output stream, bytes are write to there. 
	 * This class usually start with a DataType startElement already wrote on it.
	 * When it ends @see isClossingTransfeResponse the endElement (Data and other) MUST be writed. 
	 * */
    private XMLStreamWriter2 writer;

    /** If true, its a streaming communication, so Thread from XMLMessageReader delegate on this class the closing of the message and communication. */
    private boolean isClossingTransferResponse;

    /** How many bytes wait before flush the writer. */
    private final int bytes4Chunck = 256;

    /** How many bytes (before encoding) this thread read form InputStream source. */
    private long readedBytes = 0;

    public WriteXMLFromInputStream(XMLStreamWriter2 w, InputStream is, boolean IsClossingTransferResponse) {
        writer = w;
        input = new BufferedInputStream(is);
        isClossingTransferResponse = IsClossingTransferResponse;
    }

    /**
	 * Write the XML CData while transaction inputs is available.
	 */
    public Boolean call() throws XMLStreamException {
        byte[] byteRead;
        int nRead;
        String strChunk;
        logger.debug("Attaching input data from input stream");
        if (bLogIS) {
            initLogger();
        }
        byteRead = new byte[bytes4Chunck];
        nRead = 0;
        while (nRead >= 0) {
            try {
                nRead = input.read(byteRead);
                readedBytes += nRead;
                if (nRead != bytes4Chunck) {
                    byte[] byteFit = new byte[nRead];
                    System.arraycopy(byteRead, 0, byteFit, 0, nRead);
                    byteRead = byteFit;
                }
                if (nRead > 0) {
                    writer.writeBinary(byteRead, 0, nRead);
                    writer.flush();
                    if (bLogIS) {
                        logEncodedChunk(strChunk);
                    }
                }
            } catch (IOException e) {
                String msg = "Error reading input stream";
                logger.error(msg, e);
                throw new XMLStreamException(msg, e);
            }
            logger.debug(String.format("Read->write: %d (raw bytes) ", readedBytes));
        }
        if (isClossingTransferResponse) {
            finishWrite();
        }
        logger.debug("All the content was written");
        if (bLogIS) {
            try {
                writerOUTLOG.close();
            } catch (XMLStreamException e) {
            }
        }
        return Boolean.TRUE;
    }

    /**
	 * TODO only to close TransferResponse write (alwais be passive)
	 *  
	 * Ends the XML communication transaction write step.
	 * For PassiveCommunication also close the socket.
	 */
    private void finishWrite() throws XMLStreamException {
        writer.writeEndElement();
        writer.writeEndElement();
        writeEndEnvelope(writer);
        logger.debug("Closing writer");
        writer.close();
    }

    /**
	 * Adds the GridMessage XML end element to finish the transaction.
	 * 
	 * @param writer
	 *            the XML writer where put the end header
	 * 
	 * @throws XMLStreamException
	 *             Some problem occured during event creation/write
	 */
    protected void writeEndEnvelope(XMLStreamWriter2 writer) throws XMLStreamException {
        XMLEvent2 eE;
        eE = (XMLEvent2) EventConstants.evntFact.createEndElement(EventConstants.QN_GRID_MESSAGE, null);
        eE.writeUsing(writer);
        writer.flush();
        XMLEvent2 documentEnd = (XMLEvent2) EventConstants.evntFact.createEndDocument();
        documentEnd.writeUsing(writer);
        writer.flush();
    }

    /** Object to log the input stream data content */
    XMLStreamWriter2 writerOUTLOG;

    /**
	 * Instantiate writerOUTLOG to record the attached transaction input stream.
	 */
    private void initLogger() {
        try {
            XMLOutputFactory2 outFact = Stax2Factories.getStreamWriterFactory();
            FileOutputStream fOS = new FileOutputStream("logs/Streams_write.txt", true);
            writerOUTLOG = (XMLStreamWriter2) outFact.createXMLStreamWriter(fOS);
        } catch (FileNotFoundException e) {
            logger.error("Failed to open file for logger input stream", e);
        } catch (XMLStreamException e) {
            final String msg = "cant create logger writer stream to record encoded input chunks";
            logger.error(msg, e);
        }
    }

    /**
	 * Log a chunk of the transaction input stream.
	 */
    private void logEncodedChunk(String strChunk) {
        try {
            writerOUTLOG.writeCData(strChunk);
            writerOUTLOG.flush();
        } catch (XMLStreamException e) {
        }
    }
}

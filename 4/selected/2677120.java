package de.ddb.conversion.converters.marc21xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.ddb.charset.CharsetUtil;
import de.ddb.conversion.BinaryConverter;
import de.ddb.conversion.ConversionParameters;
import de.ddb.conversion.Converter;
import de.ddb.conversion.ConverterException;
import de.ddb.conversion.converters.MarcToMarcxmlConverter;
import de.ddb.conversion.format.Format;
import de.ddb.pica.record.PicaRecord;

/**
 * @author heck
 *
 */
public class ConversionThread implements Runnable {

    private static Log logger = LogFactory.getLog(ConversionThread.class);

    private Element collection;

    private BinaryConverter binaryConverter;

    private List<PicaRecord> records;

    private String srcEnc;

    private String trgEnc;

    private Format marcXmlFormat;

    private DocumentBuilder docBuilder;

    private String intEnc;

    private BlockingQueue<Session> sessionPool;

    /**
	 * @param sessionPool 
	 * @throws ParserConfigurationException
	 */
    public ConversionThread(BlockingQueue<Session> sessionPool) throws ParserConfigurationException {
        this.sessionPool = sessionPool;
        this.docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    @Override
    public void run() {
        Session session = null;
        try {
            session = this.sessionPool.take();
            Charset srcChar = CharsetUtil.forName(this.srcEnc);
            if (logger.isDebugEnabled()) {
                logger.debug("Converting pica record[" + srcChar.displayName() + "] to marc21-xml[" + this.trgEnc + "]...");
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Print record:\n" + records.toString());
            }
            byte[] marc21Buffer = exec(binaryConverter.getCommand(), toByteArray(records, srcChar), session, binaryConverter.getEnvironmentProperties());
            Converter marcXmlConv = new MarcToMarcxmlConverter();
            ConversionParameters params = new ConversionParameters();
            params.setSourceCharset(this.intEnc);
            params.setTargetCharset(this.trgEnc);
            ByteArrayInputStream inBuf = null;
            byte[] marcXmlBuffer = marcXmlConv.convert(marc21Buffer, params);
            if (logger.isTraceEnabled()) {
                logger.trace("Print marc [" + new String(marc21Buffer) + "].");
            }
            List<byte[]> convertedRecords = new ArrayList<byte[]>();
            inBuf = new ByteArrayInputStream(marcXmlBuffer);
            if (logger.isTraceEnabled()) {
                logger.trace("Print marcxml:\n" + new String(marcXmlBuffer));
            }
            Document holdDoc = docBuilder.parse(inBuf);
            NodeList recNodeList = holdDoc.getDocumentElement().getChildNodes();
            for (int i = 0; i < recNodeList.getLength(); i++) {
                Node recNode = recNodeList.item(i);
                Node newNode = this.collection.getOwnerDocument().importNode(recNode, true);
                this.collection.appendChild(newNode);
            }
        } catch (ConverterException e) {
            Node rec = this.collection.getOwnerDocument().createElement("record");
            Node cdata = this.collection.getOwnerDocument().createCDATASection(e.getMessage() + "\nIllegal record:\n" + records.toString());
            rec.appendChild(cdata);
            this.collection.appendChild(rec);
            logger.debug(e + "\nIllegal record:\n" + records.toString());
        } catch (IOException e) {
            Node rec = this.collection.getOwnerDocument().createElement("record");
            Node cdata = this.collection.getOwnerDocument().createCDATASection(e.getMessage() + "\nIllegal record:\n" + records.toString());
            rec.appendChild(cdata);
            this.collection.appendChild(rec);
            logger.debug(e + "\nIllegal record:\n" + records.toString());
        } catch (SAXException e) {
            Node rec = this.collection.getOwnerDocument().createElement("record");
            Node cdata = this.collection.getOwnerDocument().createCDATASection(e.getMessage() + "\nIllegal record:\n" + records.toString());
            rec.appendChild(cdata);
            this.collection.appendChild(rec);
            logger.debug(e + "\nIllegal record:\n" + records.toString());
        } catch (InterruptedException e) {
            logger.error("Could not take session from session pool.", e);
        } finally {
            this.collection = null;
            this.binaryConverter = null;
            this.records = null;
            this.srcEnc = null;
            this.trgEnc = null;
            this.intEnc = null;
            this.docBuilder = null;
            try {
                if (session != null) {
                    this.sessionPool.put(session);
                }
            } catch (InterruptedException e) {
                logger.error("Could not return session to session pool.");
            }
        }
    }

    protected byte[] exec(String command, byte[] convertable, Session session, Map<String, Object> properties) throws ConverterException {
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream errOut = new ByteArrayOutputStream();
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            if (properties != null) {
                for (String key : properties.keySet()) {
                    logger.debug("setting environment property: " + key + "=" + properties.get(key));
                    channel.setEnv(key, (String) properties.get(key));
                }
            }
            channel.connect();
            InputStream in = channel.getInputStream();
            ReadStreamThread readInput = new ReadStreamThread(in, out);
            threadPool.execute(readInput);
            InputStream errIn = channel.getErrStream();
            ReadStreamThread readError = new ReadStreamThread(errIn, errOut);
            threadPool.execute(readError);
            OutputStream channelOut = channel.getOutputStream();
            ReadStreamThread writeRequest = new ReadStreamThread(new ByteArrayInputStream(convertable), channelOut);
            threadPool.execute(writeRequest);
            threadPool.shutdown();
            while (true) {
                if (threadPool.isTerminated()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Closing conversion thread. Finished.");
                    }
                    break;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Waiting for ssh response to return...");
                }
                Thread.sleep(500);
            }
            String error = errOut.toString("UTF-8");
            if (error != null && error.length() > 0) {
                throw new ConverterException(error);
            }
        } catch (JSchException e) {
            throw new ConverterException(e);
        } catch (IOException e) {
            throw new ConverterException(e);
        } catch (InterruptedException e) {
            throw new ConverterException(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return out.toByteArray();
    }

    private byte[] toByteArray(List<PicaRecord> records, Charset ch) throws IOException {
        if (ch == null) {
            ch = Charset.defaultCharset();
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (PicaRecord r : records) {
            buffer.write(r.toPicaPlusString().getBytes(ch));
            buffer.write("\n".getBytes(ch));
        }
        return buffer.toByteArray();
    }

    /**
	 * @return
	 */
    public BinaryConverter getBinaryConverter() {
        return binaryConverter;
    }

    /**
	 * @param binaryConverter
	 */
    public void setBinaryConverter(BinaryConverter binaryConverter) {
        this.binaryConverter = binaryConverter;
    }

    /**
	 * @return
	 */
    public Element getCollection() {
        return collection;
    }

    /**
	 * @param collection
	 */
    public void setCollection(Element collection) {
        this.collection = collection;
    }

    /**
	 * @return
	 */
    public List<PicaRecord> getRecord() {
        return records;
    }

    /**
	 * @param records
	 */
    public void setRecord(List<PicaRecord> records) {
        this.records = records;
    }

    /**
	 * @return
	 */
    public String getSrcEnc() {
        return srcEnc;
    }

    /**
	 * @param srcEnc
	 */
    public void setSrcEnc(String srcEnc) {
        this.srcEnc = srcEnc;
    }

    /**
	 * @return
	 */
    public String getTrgEnc() {
        return trgEnc;
    }

    /**
	 * @param trgEnc
	 */
    public void setTrgEnc(String trgEnc) {
        this.trgEnc = trgEnc;
    }

    /**
	 * @return
	 */
    public String getIntEnc() {
        return intEnc;
    }

    /**
	 * @param intEnc
	 */
    public void setIntEnc(String intEnc) {
        this.intEnc = intEnc;
    }

    /**
	 * @return
	 */
    public Format getMarcXmlFormat() {
        return marcXmlFormat;
    }

    /**
	 * @param marcXmlFormat
	 */
    public void setMarcXmlFormat(Format marcXmlFormat) {
        this.marcXmlFormat = marcXmlFormat;
    }
}

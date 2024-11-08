package de.ddb.conversion.converters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.apache.xalan.processor.TransformerFactoryImpl;
import org.marc4j.ErrorHandler;
import org.marc4j.MarcReaderException;
import org.marc4j.marc.Leader;
import org.marc4j.marcxml.MarcXmlReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import de.ddb.charset.CharsetUtil;
import de.ddb.charset.EightBitCharset;
import de.ddb.conversion.ConversionParameters;
import de.ddb.conversion.ConverterException;
import de.ddb.conversion.GenericConverter;

/**
 * @author kett
 *
 */
public class MarcToMarcxmlConverter extends GenericConverter {

    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(MarcToMarcxmlConverter.class);

    public void convert(Reader in, Writer out, ConversionParameters params) throws ConverterException, IOException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Init MarcXmlReader");
            }
            MyMarcXmlReader producer = new MyMarcXmlReader();
            if (params.getSourceCharset() != null) {
                producer.setProperty("http://marc4j.org/properties/character-conversion", new EightBitCharsetCharacterConverter(CharsetUtil.forName(params.getSourceCharset())));
            }
            producer.setParams(params);
            producer.setProperty("http://marc4j.org/properties/error-handler", new MyErrorHandler());
            TransformerFactory factory = new TransformerFactoryImpl();
            Transformer transformer;
            if (logger.isDebugEnabled()) {
                logger.debug("Init Transformer");
            }
            transformer = factory.newTransformer();
            if (params.isAddDoctype()) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            } else {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Set errorListener");
            }
            transformer.setErrorListener(new MyErrorListener());
            if (logger.isDebugEnabled()) {
                logger.debug("Do transformation");
            }
            StreamResult result = new StreamResult(out);
            InputSource inputSource = new InputSource(in);
            Source source = new SAXSource(producer, inputSource);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            logger.error("Could not transform source.", e);
            throw new ConverterException(e);
        } catch (SAXException e) {
            logger.error("Could not create SAXSource.", e);
            throw new ConverterException(e);
        }
    }

    public void convert(InputStream in, OutputStream out, ConversionParameters params) throws ConverterException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("convert(InputStream, OutputStream, params.sourceCharset=" + params.getSourceCharset() + ", params.targetCharset=" + params.getTargetCharset() + ") - start");
        }
        BufferedReader reader = null;
        BufferedWriter writer = null;
        reader = new BufferedReader(new InputStreamReader(in, new EightBitCharset()));
        writer = new BufferedWriter(new OutputStreamWriter(out, CharsetUtil.forName(params.getTargetCharset())));
        convert(reader, writer, params);
        try {
            writer.flush();
        } catch (IOException e) {
        }
    }
}

class MyErrorHandler implements ErrorHandler {

    public static Logger logger = Logger.getLogger(MyErrorHandler.class);

    public void warning(MarcReaderException e) {
        throw new RuntimeException(e);
    }

    public void error(MarcReaderException e) {
        logger.info("cause: " + e.getCause() + ", controlNumber: " + e.getControlNumber() + ", position: " + e.getPosition(), e);
        throw new RuntimeException(e);
    }

    public void fatalError(MarcReaderException e) {
        throw new RuntimeException(e);
    }
}

class MyErrorListener implements ErrorListener {

    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(MyErrorListener.class);

    public void error(TransformerException exception) throws TransformerException {
        if (logger.isDebugEnabled()) {
            logger.debug("error(TransformerException) - start");
        }
        throw exception;
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        if (logger.isDebugEnabled()) {
            logger.debug("fatalError(TransformerException) - start");
        }
        throw exception;
    }

    public void warning(TransformerException exception) throws TransformerException {
        if (logger.isDebugEnabled()) {
            logger.debug("warning(TransformerException) - start");
        }
        throw exception;
    }
}

class MyMarcXmlReader extends MarcXmlReader {

    private ConversionParameters params;

    @Override
    public void endCollection() throws SAXException {
        if (params.isAddCollectionFooter()) {
            super.endCollection();
        }
    }

    @Override
    public void startCollection() throws SAXException {
        if (params.isAddCollectionHeader()) {
            super.startCollection();
        }
    }

    public ConversionParameters getParams() {
        return this.params;
    }

    public void setParams(ConversionParameters params) {
        this.params = params;
    }

    @Override
    public void startRecord(Leader leader) throws SAXException {
        leader.setCharCodingScheme('a');
        super.startRecord(leader);
    }
}

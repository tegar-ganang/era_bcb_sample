package org.xaware.server.engine.instruction.bizcomps.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.core.io.Resource;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.shared.util.EncodingHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Class to deal with the response from a single get or post operation
 * 
 * @author jtarnowski
 */
public class HttpResultHelper {

    /** Our name for logging */
    private static final String className = "HttpResultHelper";

    /** Our logger */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(className);

    /**
     * Constructor is private
     */
    private HttpResultHelper() {
    }

    /**
     * public method to handle results
     * 
     * @param stream -
     *            InputStream from the get or post
     * @param config -
     *            HttpConfigInfo object
     * @return Element to be added as a child of xa:response
     * @throws IOException
     * @throws JDOMException
     * @throws XAwareException
     */
    public static Element handleResponse(InputStream stream, HttpConfigInfo config) throws IOException, JDOMException, XAwareException {
        Element returnElem = null;
        try {
            String outputType = config.getOutputType();
            if (XAwareConstants.XAWARE_DATATYPE_BINARY.equals(outputType)) {
                returnElem = handleBinaryResponse(stream, config);
            } else {
                String fileName = config.getOutputFile();
                if (fileName != null) {
                    writeToFile(stream, fileName);
                } else if (XAwareConstants.XAWARE_OUTPUT_XML.equals(outputType)) {
                    returnElem = handleXmlResponse(stream);
                } else if (XAwareConstants.XAWARE_OUTPUT_TEXT.equals(outputType) || XAwareConstants.XAWARE_OUTPUT_CDATA.equals(outputType)) {
                    returnElem = handleTextResponse(stream, config);
                }
            }
        } finally {
            stream.close();
        }
        return returnElem;
    }

    /**
     * Read the InputStream and add it as the Text child of an Element
     * 
     * @param stream -
     *            InputStream
     * @param config -
     *            HttpConfigInfo object
     * @return Element to be added as a child of xa:response
     * @throws IOException
     */
    protected static Element handleTextResponse(InputStream stream, HttpConfigInfo config) throws IOException {
        String resultName = config.getResultName();
        if (resultName == null) {
            resultName = "RESULT";
        }
        Element returnElem = new Element(resultName);
        final BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String inputLine;
        final StringBuffer buf = new StringBuffer(1000);
        while ((inputLine = in.readLine()) != null) {
            buf.append(inputLine);
        }
        String result = buf.toString();
        returnElem.addContent(result);
        return returnElem;
    }

    /**
     * Read the InputStream and parse as XML and return it as an Element
     * 
     * @param stream -
     *            InputStream
     * @param config -
     *            HttpConfigInfo object
     * @return Element to be added as a child of xa:response
     * @throws IOException
     */
    protected static Element handleXmlResponse(InputStream stream) throws XAwareException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String inputLine;
        final StringBuffer buf = new StringBuffer(1000);
        String result;
        try {
            while ((inputLine = in.readLine()) != null) {
                buf.append(inputLine);
            }
            result = buf.toString();
        } catch (IOException ex) {
            final String message = "IOException reading XML response:" + ex.getLocalizedMessage();
            logger.severe(message, className, "handleXmlResponse");
            throw new XAwareException(message);
        }
        final SAXBuilder builder = new SAXBuilder();
        final StringReader reader = new StringReader(result);
        Document doc = null;
        try {
            doc = builder.build(reader);
        } catch (IOException ex) {
            final String message = "IOException handling XML response:" + ex.getLocalizedMessage();
            logger.severe(message, className, "handleXmlResponse");
            throw new XAwareException(message);
        } catch (JDOMException ex) {
            final String message = "JDOMException handling XML response:" + ex.getLocalizedMessage() + " Tried to parse:\"" + result + '\"';
            logger.severe(message, className, "handleXmlResponse");
            throw new XAwareException(message);
        }
        final Element root = doc.getRootElement();
        return ((Element) root.detach());
    }

    /**
     * Read the InputStream and base 64 encode it and return an Element that has the result as a Text child
     * 
     * @param stream -
     *            InputStream
     * @param config -
     *            HttpConfigInfo object
     * @return Element to be added as a child of xa:response
     * @throws IOException
     */
    protected static Element handleBinaryResponse(InputStream stream, HttpConfigInfo config) throws IOException {
        String resultName = config.getResultName();
        if (resultName == null) {
            resultName = "RESULT";
        }
        Element returnElem = new Element(resultName);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        EncodingHelper.encodeBase64(stream, out);
        returnElem.addContent(new String(out.toByteArray()));
        return returnElem;
    }

    /**
     * Output to a local file
     * 
     * @param stream -
     *            InputStream
     * @param fileName -
     *            String
     */
    protected static void writeToFile(InputStream instream, String fileName) throws IOException, XAwareException {
        try {
            Resource resource = ResourceHelper.getResource(fileName);
            if (resource != null) {
                File file = resource.getFile();
                java.io.OutputStream output1 = new java.io.FileOutputStream(file);
                if (output1 == null) {
                    String msg = "Unable to create output stream for :" + fileName;
                    logger.severe(msg, className, "writeToFile");
                    throw new XAwareException(msg);
                } else {
                    java.io.BufferedOutputStream outBuff = new java.io.BufferedOutputStream(output1);
                    int dataByte;
                    BufferedReader in = new BufferedReader(new InputStreamReader(instream));
                    while ((dataByte = in.read()) != -1) outBuff.write(dataByte);
                    outBuff.flush();
                    output1.close();
                }
            }
        } catch (IOException e) {
            logger.severe("HTTP IO exception" + e.getMessage(), className, "writeToFile");
            throw e;
        }
    }
}

package org.databene.xslt;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

/**
 * Performs XSL transformations on XML strings and streams.<br/>
 * <br/>
 * Created: 26.01.2007 08:31:09
 */
public class XSLTTransformer {

    private static final Map<String, Transformer> transformers = new HashMap<String, Transformer>();

    private static Transformer getTransformer(String xsltString) throws TransformerConfigurationException {
        Transformer transformer = transformers.get(xsltString);
        if (transformer == null) {
            Source xsltSource = new StreamSource(new StringReader(xsltString));
            transformer = TransformerFactory.newInstance().newTransformer(xsltSource);
            transformers.put(xsltString, transformer);
        }
        return transformer;
    }

    public static String transform(String xmlString, String xsltString) throws TransformerException {
        Reader source = new StringReader(xmlString);
        StringWriter writer = new StringWriter();
        transform(source, xsltString, writer);
        return writer.getBuffer().toString();
    }

    public static void transform(Reader reader, String xsltString, Writer writer) throws TransformerException {
        Transformer transformer = getTransformer(xsltString);
        transformer.transform(new StreamSource(reader), new StreamResult(writer));
    }
}

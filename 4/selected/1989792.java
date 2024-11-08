package org.tolven.xml;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class Transformer {

    javax.xml.transform.Transformer transformer;

    public Transformer(String xslt) {
        try {
            InputStream stream = getClass().getResourceAsStream(xslt);
            if (stream == null) {
                throw new RuntimeException("Resource not found: " + xslt);
            }
            Source source = new StreamSource(stream);
            transformer = TransformerFactory.newInstance().newTransformer(source);
        } catch (Exception e) {
            throw new RuntimeException("Error creating transformer for " + xslt, e);
        }
    }

    public Transformer(InputStream inputStream) throws Exception {
        Source source = new StreamSource(inputStream);
        transformer = TransformerFactory.newInstance().newTransformer(source);
    }

    public String transform(String fileName) {
        try {
            File file = new File(fileName);
            Source xmlSource = new StreamSource(file);
            Writer writer = new StringWriter();
            Result outputTarget = new StreamResult(writer);
            transformer.transform(xmlSource, outputTarget);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error Tranforming " + fileName, e);
        }
    }

    public void transform(Reader reader, Writer writer) throws Exception {
        Source xmlSource = new StreamSource(reader);
        Result outputTarget = new StreamResult(writer);
        transformer.transform(xmlSource, outputTarget);
    }
}

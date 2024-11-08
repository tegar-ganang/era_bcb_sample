package com.ohua.engine.utils.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import com.ohua.engine.exceptions.XMLParserException;
import org.exolab.castor.mapping.Mapping;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class OperatorDescriptorDeserializer {

    private Logger _logger = Logger.getLogger(getClass().getName());

    private OperatorDescriptorParser _opDescParser = new OperatorDescriptorParser();

    public OperatorDescription deserialize(String operatorName) throws SAXException, ParserConfigurationException, IOException, XMLParserException {
        OperatorDescription description = null;
        Enumeration<URL> opDescDirs = getClass().getClassLoader().getResources("META-INF/operators");
        while (opDescDirs.hasMoreElements()) {
            URL opDescDir = opDescDirs.nextElement();
            File file = new File(opDescDir.getFile() + "/" + operatorName + ".xml");
            if (file.exists()) {
                description = deserialize(file);
                break;
            }
        }
        if (description == null) {
            throw new IllegalArgumentException("Descriptor for operator " + operatorName + " not found.");
        } else {
            return description;
        }
    }

    public OperatorDescription deserialize(File file) throws SAXException, ParserConfigurationException, IOException, XMLParserException {
        Charset charset = Charset.forName("ISO-8859-15");
        CharsetDecoder decoder = charset.newDecoder();
        Pattern linePattern = Pattern.compile(".*\r?\n");
        Pattern propertiesOpenTag = Pattern.compile("<ohua:operator-properties>");
        Pattern propertiesEndTag = Pattern.compile("</ohua:operator-properties>");
        FileInputStream opDescriptorFile = new FileInputStream(file);
        FileChannel opDescriptorChannel = opDescriptorFile.getChannel();
        MappedByteBuffer mappedByteBuffer = opDescriptorChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) opDescriptorChannel.size());
        CharBuffer cb = decoder.decode(mappedByteBuffer);
        Matcher lm = linePattern.matcher(cb);
        Matcher pm = null;
        int startIndexCastorContent = 0;
        int endIndexCastorContent = 0;
        int dataBytesSeen = 0;
        int lines = 0;
        boolean castorContent = false;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();
            if (pm == null) {
                pm = propertiesOpenTag.matcher(cs);
            } else {
                pm.reset(cs);
            }
            if (pm.find()) {
                if (!castorContent) {
                    castorContent = true;
                    dataBytesSeen += pm.end();
                    startIndexCastorContent = dataBytesSeen;
                    dataBytesSeen = 0;
                    pm = propertiesEndTag.matcher(cs);
                } else {
                    dataBytesSeen += pm.start() - 1;
                    endIndexCastorContent = startIndexCastorContent + dataBytesSeen;
                    break;
                }
            } else {
                dataBytesSeen += cs.length();
            }
            if (lm.end() == cb.limit()) {
                _logger.fine("No properties found in operator descriptor " + file.getName());
                break;
            }
        }
        _logger.fine("part 1: " + cb.subSequence(0, startIndexCastorContent));
        _logger.fine("part 2: " + cb.subSequence(startIndexCastorContent, endIndexCastorContent));
        _logger.fine("part 3: " + cb.subSequence(endIndexCastorContent, cb.length()));
        ByteBuffer xmlReaderBuffer = ByteBuffer.allocate(cb.length() - (endIndexCastorContent - startIndexCastorContent));
        ByteBuffer castorBuffer = ByteBuffer.allocate(endIndexCastorContent - startIndexCastorContent);
        mappedByteBuffer.position(0);
        mappedByteBuffer.get(xmlReaderBuffer.array(), 0, startIndexCastorContent);
        mappedByteBuffer.position(startIndexCastorContent);
        mappedByteBuffer.get(castorBuffer.array(), 0, endIndexCastorContent - startIndexCastorContent);
        mappedByteBuffer.position(endIndexCastorContent);
        mappedByteBuffer.get(xmlReaderBuffer.array(), startIndexCastorContent, cb.length() - endIndexCastorContent);
        opDescriptorChannel.close();
        ByteArrayInputStream xmlReaderInputStream = new ByteArrayInputStream(xmlReaderBuffer.array());
        ByteArrayInputStream castorInputStream = new ByteArrayInputStream(castorBuffer.array());
        XMLReader xmlReader = OhuaXMLParserFactory.getInstance().createXMLReader();
        xmlReader.setContentHandler(_opDescParser);
        xmlReader.parse(new InputSource(xmlReaderInputStream));
        OperatorDescription description = _opDescParser.getParsedOperatorDescription();
        if (castorInputStream.available() > 0) {
            Mapping propertiesMapping = new Mapping();
            description.setPropertiesMapping(propertiesMapping);
            propertiesMapping.loadMapping(new InputSource(castorInputStream));
            castorInputStream.close();
        }
        xmlReaderInputStream.close();
        return description;
    }
}

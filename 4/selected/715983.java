package org.achup.elgenerador.xml.process;

import org.achup.elgenerador.process.GenerationProcessInfo;
import org.achup.elgenerador.process.ProcessInfo;
import org.achup.elgenerador.process.ReadMetadataProcessInfo;
import org.achup.elgenerador.xml.ElementParser;
import org.achup.elgenerador.xml.ElementWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Marco Bassaletti
 */
public class ProcessInfoElement implements ElementParser<ProcessInfo>, ElementWriter<ProcessInfo> {

    @Override
    public ProcessInfo parseXML(Element element) {
        if (element.getNodeName().compareTo("process") != 0) {
            throw new IllegalArgumentException("Element name differs from process.");
        }
        ProcessInfo processInfo = null;
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                if (childElement.getNodeName().compareTo("read-metadata") == 0) {
                    ElementParser<ReadMetadataProcessInfo> parser = new ReadMetadataProcessInfoElement();
                    processInfo = parser.parseXML(childElement);
                } else if (childElement.getNodeName().compareTo("generate-data") == 0) {
                    ElementParser<GenerationProcessInfo> parser = new GenerationProcessInfoElement();
                    processInfo = parser.parseXML(childElement);
                }
            }
        }
        return processInfo;
    }

    @Override
    public Element createXML(Document document, ProcessInfo processInfo) {
        Element element = document.createElement("process");
        if (processInfo instanceof ReadMetadataProcessInfo) {
            ReadMetadataProcessInfo readMetadataProcessInfo = (ReadMetadataProcessInfo) processInfo;
            ElementWriter<ReadMetadataProcessInfo> writer = new ReadMetadataProcessInfoElement();
            Element childElement = writer.createXML(document, readMetadataProcessInfo);
            element.appendChild(childElement);
        } else if (processInfo instanceof GenerationProcessInfo) {
            GenerationProcessInfo generationProcessInfo = (GenerationProcessInfo) processInfo;
            ElementWriter<GenerationProcessInfo> writer = new GenerationProcessInfoElement();
            Element childElement = writer.createXML(document, generationProcessInfo);
            element.appendChild(childElement);
        }
        return element;
    }
}

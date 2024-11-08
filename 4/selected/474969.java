package org.springframework.richclient.settings.xml;

import java.io.IOException;
import org.springframework.richclient.settings.SettingsException;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Peter De Bruycker
 */
public class RootXmlSettings extends XmlSettings {

    private XmlSettingsReaderWriter readerWriter;

    private Document doc;

    public RootXmlSettings(Document doc, XmlSettingsReaderWriter readerWriter) {
        super(getSettingsElement(doc));
        this.doc = doc;
        Assert.notNull(readerWriter, "XmlSettingsReaderWriter cannot be null");
        this.readerWriter = readerWriter;
    }

    private static Element getSettingsElement(Document doc) {
        Assert.notNull(doc, "Document cannot be null");
        return doc.getDocumentElement();
    }

    public void save() throws IOException {
        try {
            readerWriter.write(this);
        } catch (SettingsException e) {
            e.printStackTrace();
        }
    }

    public Document getDocument() {
        return doc;
    }
}

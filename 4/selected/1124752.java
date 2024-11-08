package valentino.rejaxb.common.rss1;

import java.util.List;
import valentino.rejaxb.annotations.ClassXmlNodeName;
import valentino.rejaxb.annotations.XmlAttributeName;
import valentino.rejaxb.annotations.XmlNodeName;

@ClassXmlNodeName("rdf:RDF")
public class Rdf {

    @XmlAttributeName("xmlns:rdf")
    private String xmlnsRdf;

    @XmlAttributeName("xmlns")
    private String xmlns;

    @XmlNodeName("channel")
    private List<RdfChannel> channels;

    @XmlNodeName("image")
    private RdfImage image;

    @XmlNodeName("item")
    private List<RdfItem> items;

    @XmlNodeName("textinput")
    private RdfTextInput textInput;

    public String getXmlnsRdf() {
        return xmlnsRdf;
    }

    public void setXmlnsRdf(String xmlnsRdf) {
        this.xmlnsRdf = xmlnsRdf;
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public List<RdfChannel> getChannels() {
        return channels;
    }

    public void setChannels(List<RdfChannel> channels) {
        this.channels = channels;
    }

    public RdfImage getImage() {
        return image;
    }

    public void setImage(RdfImage image) {
        this.image = image;
    }

    public List<RdfItem> getItems() {
        return items;
    }

    public void setItems(List<RdfItem> items) {
        this.items = items;
    }

    public RdfTextInput getTextInput() {
        return textInput;
    }

    public void setTextInput(RdfTextInput textInput) {
        this.textInput = textInput;
    }
}

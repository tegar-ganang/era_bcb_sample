package valentino.rejaxb.common.rss2;

import java.util.List;
import valentino.rejaxb.annotations.ClassXmlNodeName;
import valentino.rejaxb.annotations.XmlAttributeName;

@ClassXmlNodeName("rss")
public class Rss2 {

    private List<Channel> channels;

    @XmlAttributeName("version")
    private String version;

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

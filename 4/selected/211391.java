package componente.rss.tag;

import componente.rss.tag.rss.Channel;

public class Rss {

    private String version;

    private Channel channel;

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("");
        str.append("RSS version: " + (version == null ? "" : version));
        str.append((channel == null ? "" : "\n\t" + channel));
        return str.toString();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}

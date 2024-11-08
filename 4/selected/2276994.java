package core.rss.elem;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that represent the <code>&lt;rss&gt;</code> root element.
 * 
 * @author Henrique A. Viecili
 */
public class RSS {

    /**
     * Constant to specify version 2.0 for the generated feed.
     */
    public static final String VERSION_2_0 = "2.0";

    /**
     * Constant to specify the default version used for the generated feed.
     */
    public static final String DEFAULT_VERSION = VERSION_2_0;

    private String version;

    private List<Channel> channels;

    /**
     * The RSS constructor with default version
     */
    public RSS() {
        this(DEFAULT_VERSION);
    }

    /** The RSS constructor specifying version
     * @exception InvalidRequiredParamException if some parameter passed is invalid
     * @param version The RSS Specification version
     */
    public RSS(String version) {
        super();
        if (!VERSION_2_0.equals(version)) throw new InvalidRequiredParamException("version invalid: " + version);
        this.version = version;
        this.channels = new ArrayList<Channel>();
    }

    /** Gets this RSS Feed version
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }

    /** Sets The RSS Specification version
     * @param version The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /** Gets the channel elements of this RSS Feed
     * @return Returns the channels.
     */
    public List getChannels() {
        return channels;
    }

    /** Add a channel to this RSS Feed
     * @param channel The channel to add.
     */
    public void addChannel(Channel channel) {
        this.channels.add(channel);
    }
}

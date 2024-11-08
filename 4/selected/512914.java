package dl.pushlog;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;

/**
 * @version $Id: ChannelConfig.java,v 1.4 2005/06/14 22:31:29 dirk Exp $
 * @author $Author: dirk $
 */
public class ChannelConfig extends Properties {

    /** matches every line */
    public static final String DEFAULT_REGEX = ".*";

    /** report whole match */
    public static final String DEFAULT_FORMAT = "${0}";

    /** immutable (perferable unique) name */
    public final String id;

    private ChannelOpener channelOpener = null;

    public ChannelConfig(String id) {
        this.id = id;
        super.setProperty("regex", DEFAULT_REGEX);
        super.setProperty("format", DEFAULT_FORMAT);
    }

    public ReadableByteChannel openChannel() throws IOException {
        return channelOpener.openChannel(this);
    }

    /**
     * the channel configuration is bound to a factory class which does the
     * actuall channel opening. Java NIO channels where choosen instead of
     * InputStreams due to the need for interruptible I/O operations.
     * @return channel openener instance
     */
    public ChannelOpener getChannelOpener() {
        return channelOpener;
    }

    public void setChannelOpener(ChannelOpener channelOpener) {
        this.channelOpener = channelOpener;
    }

    /**
     * unique name for human reference in config files and logs
     * @return unique id
     */
    public String getId() {
        return id;
    }

    /**
     * reported matches are composed according to this formatting rule. matched
     * groups from the regex match can be references with '${n}' where n is the
     * group number. Group 0 ( '${0}') is the default values and represents the
     * whole line which was matched.
     * @see java.util.regex.Pattern
     * @return formatting string
     */
    public String getFormat() {
        return super.getProperty("format");
    }

    public void setFormat(String format) {
        super.setProperty("format", format);
    }

    /**
     * streams are matched line by line against this regex
     * @return regular expression
     */
    public String getRegex() {
        return this.getProperty("regex");
    }

    public void setRegex(String regex) {
        super.setProperty("regex", regex);
    }

    public void setFile(File file) {
        super.put("file", file);
        this.setChannelOpener(new FileChannelOpener());
    }

    public File getFile() {
        return (File) super.get("file");
    }
}

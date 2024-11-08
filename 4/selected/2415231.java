package net.dromard.common.rss.feed;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RSS {

    public static final String RSS = "rss";

    private Channel channel;

    private String version;

    private static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss.");

    /**
	 * @param date The string to be parsed
	 * @return The date corresponding to the string.
	 * @throws ParseException If the string is not well formatted
	 */
    public static Date parseDate(final String date) throws ParseException {
        return (date != null && date.length() > 0) ? DATE_FORMATTER.parse(date.replace('T', '.').replace('Z', '.')) : null;
    }

    /**
	 * @param date The date to be formatted into string
	 * @return The string corresponding to the date.
	 */
    public static String formatDate(final Date date) {
        return (date != null) ? DATE_FORMATTER.format(date).replaceFirst("\\.([^\\.]*)\\.", "T$1Z") : null;
    }

    /**
	 * @return the version
	 */
    public String getVersion() {
        return version;
    }

    /**
	 * @param version the version to set
	 */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
	 * @return the channel
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * @param channel the channel to set
	 */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(channel.toString());
        return buf.toString();
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(RSS).append(" version=\"" + version + "\">\n");
        buf.append(channel.toXML("\t"));
        buf.append("</").append(RSS).append(">").append('\n');
        return buf.toString();
    }
}

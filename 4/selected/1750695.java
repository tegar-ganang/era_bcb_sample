package net.itsite.rss;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description：
 * @Email: eliyanfei@126.com
 * @Author foo.li
 * @Time 2011-1-14 上午11:34:49
 */
public class RSSChannel {

    public String title = "";

    public String link = "";

    public String description = "";

    public String language = "";

    public int ttl = 5;

    public List<ChannelItem> channelItem;

    public RSSChannel() {
        this.channelItem = new ArrayList<ChannelItem>(20);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    /**
	 * 创建新闻
	 * @return
	 */
    public ChannelItem getChannelItem() {
        final ChannelItem item = new ChannelItem();
        this.channelItem.add(item);
        return item;
    }

    public void buildRSSChannel(final StringBuffer buf) {
        buf.append("<channel>").append("\r\n");
        buf.append("<title><![CDATA[" + title + "]]></title>").append("\r\n");
        buf.append("<link>" + link + "</link>").append("\r\n");
        buf.append("<description><![CDATA[" + description + "-技术引导IT,IT创新未来]]></description>").append("\r\n");
        buf.append("<language>" + language + "</language>").append("\r\n");
        buf.append("<image>").append("\r\n");
        buf.append("<link>http://www.itniwo.net</link>").append("\r\n");
        buf.append("<url>http://www.itniwo.net/simple/template/images/logo.png</url>").append("\r\n");
        buf.append("<title>OsChina.NET</title>").append("\r\n");
        buf.append("</image>").append("\r\n");
        for (final ChannelItem item : channelItem) {
            item.buildChannelItem(buf);
        }
        buf.append("</channel>").append("\r\n");
    }
}

package net.simpleframework.web.page.component.ui.portal.module;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import net.simpleframework.core.AbstractXmlDocument;
import net.simpleframework.util.DateUtils;
import net.simpleframework.util.HTMLUtils;
import net.simpleframework.util.StringUtils;
import net.simpleframework.web.page.PageRequestResponse;
import net.simpleframework.web.page.component.HandleException;
import net.simpleframework.web.page.component.ui.portal.PageletBean;
import net.simpleframework.web.page.component.ui.portal.PortalUtils;
import org.dom4j.Element;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class RssUtils {

    public static class RssChannelItem {

        private String title;

        private String link;

        private String description;

        private Date pubDate;

        private String comments;

        public String getComments() {
            return comments;
        }

        public String getDescription() {
            return description;
        }

        public String getLink() {
            return link;
        }

        public Date getPubDate() {
            return pubDate;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class RssChannel {

        private String title;

        private String link;

        private Collection<RssChannelItem> channelItems;

        public Collection<RssChannelItem> getChannelItems() {
            if (channelItems == null) {
                channelItems = new ArrayList<RssChannelItem>();
            }
            return channelItems;
        }

        public String getLink() {
            return link;
        }

        public String getTitle() {
            return title;
        }
    }

    static final String[] RFC822_MASKS = { "EEE, dd MMM yyyy HH:mm:ss z", "EEE, dd MMM yyyy hh:mm a z", "yyyy-MM-dd HH:mm:ss" };

    public static RssChannel getRssChannel(final String rssUrl) {
        return getRssChannel(rssUrl, true);
    }

    public static RssChannel getRssChannel(final String rssUrl, final boolean parseItems) {
        if (!StringUtils.hasText(rssUrl)) {
            return null;
        }
        try {
            final URL url = new URL(rssUrl);
            final RssChannel channel = new RssChannel();
            new AbstractXmlDocument(url) {

                @Override
                protected void init() {
                    final Element ele = getRoot().element("channel");
                    if (ele == null) {
                        return;
                    }
                    channel.title = ele.elementTextTrim("title");
                    channel.link = ele.elementTextTrim("link");
                    if (StringUtils.hasText(channel.link)) {
                        final String link = channel.link.toLowerCase();
                        if ((link.charAt(0) != '/') && !link.startsWith("http://")) {
                            channel.link = "http://" + link;
                        }
                    }
                    if (parseItems) {
                        final Iterator<?> it = ele.elementIterator("item");
                        while (it.hasNext()) {
                            final Element iele = (Element) it.next();
                            final RssChannelItem item = new RssChannelItem();
                            channel.getChannelItems().add(item);
                            item.title = iele.elementTextTrim("title");
                            item.link = iele.elementTextTrim("link");
                            item.description = iele.elementTextTrim("description");
                            final String dateString = iele.elementTextTrim("pubDate");
                            for (final String mask : RFC822_MASKS) {
                                final SimpleDateFormat sdf = new SimpleDateFormat(mask, Locale.ENGLISH);
                                try {
                                    item.pubDate = sdf.parse(dateString);
                                    break;
                                } catch (final ParseException e) {
                                }
                            }
                            item.comments = iele.elementTextTrim("comments");
                        }
                    }
                }
            };
            return channel;
        } catch (final IOException ex) {
            throw HandleException.wrapException(ex);
        }
    }

    public static String rssRender(final PageRequestResponse requestResponse) {
        final StringBuilder sb = new StringBuilder();
        try {
            final PageletBean pagelet = PortalUtils.getPageletBean(requestResponse);
            final RssModuleHandle rssModule = (RssModuleHandle) PortalModuleRegistryFactory.getInstance().getModuleHandle(pagelet);
            final RssChannel channel = rssModule.getRssChannel();
            if (channel == null) {
            } else {
                final Collection<RssChannelItem> items = channel.getChannelItems();
                final int l = Math.min(rssModule.getRows(), items.size());
                final Iterator<RssChannelItem> it = items.iterator();
                int j = 0;
                sb.append("<ul class=\"rss\">");
                while (j++ < l) {
                    final RssChannelItem item = it.next();
                    sb.append("<li>");
                    sb.append("<a target=\"_blank\" href=\"").append(item.getLink()).append("\">");
                    sb.append(item.getTitle()).append("</a>");
                    final String desc = item.getDescription();
                    if (rssModule.isShowTip() && StringUtils.hasText(desc)) {
                        sb.append("<div style=\"display: none;\">").append(HTMLUtils.convertHtmlLines(desc)).append("</div>");
                    }
                    final String pubDate = DateUtils.getDifferenceDate(item.getPubDate());
                    if (StringUtils.hasText(pubDate)) {
                        sb.append("<span> - ").append(pubDate).append("</span>");
                    }
                    sb.append("</li>");
                }
                sb.append("</ul><div class=\"rss_more\">");
                sb.append("<a onclick=\"$Actions['rssContentWindow'](_lo_getPagelet(this).params);\">#(RssUtils.0)&raquo;</a>");
                sb.append("</div>");
            }
        } catch (final Exception e) {
            return HTMLUtils.convertHtmlLines(e.toString());
        }
        return sb.toString();
    }
}

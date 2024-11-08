package net.itsite.rss.impl;

import java.util.Date;
import java.util.Map;
import net.a.ItSiteUtil;
import net.itsite.i.IItSiteApplicationModule;
import net.itsite.rss.ChannelItem;
import net.itsite.rss.RSSChannel;
import net.simpleframework.ado.db.IQueryEntitySet;
import net.simpleframework.organization.IUser;
import net.simpleframework.util.HTMLUtils;

/**
 * @author 李岩飞
 *
 */
public abstract class ARss implements IRss {

    protected abstract IItSiteApplicationModule getApplicationModule();

    protected IQueryEntitySet<Map<String, Object>> queryData() {
        return null;
    }

    protected abstract String getSQL();

    protected Object[] getValues() {
        return new Object[] {};
    }

    protected abstract String getCatalog(final Object catalogId);

    protected String getLink(final Object id) {
        return getApplicationModule().getViewUrl(id);
    }

    @Override
    public void buildRss(final RSSChannel rssChannel, StringBuffer buf) {
        final IItSiteApplicationModule applicationModule = getApplicationModule();
        IQueryEntitySet<Map<String, Object>> qsData = null;
        if (applicationModule != null) qsData = applicationModule.queryBean(getSQL(), getValues()); else qsData = queryData();
        if (qsData != null) {
            Map<String, Object> data;
            while ((data = qsData.next()) != null) {
                if (qsData.position() > 100) {
                    return;
                }
                final ChannelItem channelItem = rssChannel.getChannelItem();
                channelItem.title = (String) data.get("title");
                channelItem.link = ItSiteUtil.url + getLink(data.get("id"));
                final Object content = data.get("content");
                if (content == null) {
                    channelItem.description = channelItem.title;
                } else {
                    channelItem.description = HTMLUtils.truncateHtml(HTMLUtils.createHtmlDocument((String) content, true), 100, false, false, false);
                }
                channelItem.category = getCatalog(data.get("catalogId"));
                final IUser user = ItSiteUtil.getUserById(data.get("userId"));
                channelItem.author = user == null ? "" : user.getText();
                channelItem.pubDate = ((Date) data.get("createDate")).toGMTString();
            }
        }
    }
}

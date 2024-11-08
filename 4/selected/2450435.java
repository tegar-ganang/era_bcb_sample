package org.vosao.command;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.vosao.business.impl.mq.AbstractSubscriber;
import org.vosao.business.mq.Message;
import org.vosao.entity.ContentEntity;
import org.vosao.entity.PageEntity;
import org.vosao.utils.DateUtil;
import org.vosao.utils.StrUtil;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

public class PageSearchCommand extends AbstractSubscriber {

    private static int CONTENT_SIZE = 100;

    private String clientId;

    private String query;

    private boolean published;

    private boolean unpublished;

    private Date from;

    private Date to;

    @Override
    public void onMessage(Message message) {
        parseMessage(message);
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        for (PageEntity page : getDao().getPageDao().select()) {
            if (filter(page)) {
                channelService.sendMessage(new ChannelMessage(clientId, createHitJSON(page)));
            }
        }
        channelService.sendMessage(new ChannelMessage(clientId, "({end:true})"));
    }

    private String createHitJSON(PageEntity page) {
        List<ContentEntity> contents = getDao().getPageDao().getContents(page.getId());
        String content = "No content";
        if (contents.size() > 0) {
            content = StrUtil.extractSearchTextFromHTML(contents.get(0).getContent());
            content = content.length() > CONTENT_SIZE ? content.substring(0, CONTENT_SIZE) : content;
        }
        return String.format("({end:false,id:%d,title:\"%s\",content:\"%s\"" + ",version:%d})", page.getId(), page.getTitle(), content, page.getVersion());
    }

    private boolean filter(PageEntity page) {
        if (!unpublished && published && !page.isApproved()) {
            return false;
        }
        if (!published && unpublished && page.isApproved()) {
            return false;
        }
        if (from != null && page.getPublishDate().before(from)) {
            return false;
        }
        if (to != null && page.getPublishDate().after(to)) {
            return false;
        }
        if (StringUtils.isEmpty(query)) {
            return true;
        }
        if (page.getTitle().toLowerCase().indexOf(query) != -1) {
            return true;
        }
        for (ContentEntity content : getDao().getPageDao().getContents(page.getId())) {
            if (content.getContent().toLowerCase().indexOf(query) != -1) {
                return true;
            }
        }
        return false;
    }

    private void parseMessage(Message message) {
        CommandMessage msg = (CommandMessage) message;
        clientId = msg.getClientId();
        query = msg.getParams().get("query");
        query = query != null ? query.toLowerCase() : query;
        published = Boolean.valueOf(msg.getParams().get("published"));
        unpublished = Boolean.valueOf(msg.getParams().get("unpublished"));
        if (msg.getParams().get("from") != null) {
            try {
                from = DateUtil.toDate(msg.getParams().get("from"));
            } catch (ParseException e) {
                logger.error(e.getMessage());
            }
        }
        if (msg.getParams().get("to") != null) {
            try {
                to = DateUtil.toDate(msg.getParams().get("to"));
            } catch (ParseException e) {
                logger.error(e.getMessage());
            }
        }
    }
}

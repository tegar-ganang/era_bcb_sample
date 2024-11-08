package org.encuestame.comet.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.collections.ListUtils;
import org.apache.log4j.Logger;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.java.annotation.Listener;
import org.cometd.java.annotation.Service;
import org.encuestame.core.util.JSONUtils;
import org.encuestame.utils.web.CommentBean;

/**
 * Comments service.
 * @author Picado, Juan juanATencuestame.org
 * @since 12/08/2011
 */
@Named
@Singleton
@Service("commentCometService")
public class CommentsStreamService extends AbstractCometService {

    private Logger log = Logger.getLogger(this.getClass());

    /**
     *
     */
    @Listener("/service/comment/get")
    public void processStream(final ServerSession remote, final ServerMessage.Mutable message) {
        final Map<String, Object> output = new HashMap<String, Object>();
        try {
            log.debug("CommentsStreamService............");
            List<CommentBean> comments = getCommentService().getCommentsbyUser(20, 0);
            log.debug("CommentsStreamService.comments size .." + comments.size());
            output.put("comments", JSONUtils.convertObjectToJsonString(comments));
        } catch (Exception e) {
            output.put("comments", ListUtils.EMPTY_LIST);
            log.fatal("cometd: username invalid " + e);
        }
        remote.deliver(getServerSession(), message.getChannel(), output, null);
    }
}

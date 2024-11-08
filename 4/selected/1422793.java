package com.hs.mail.imap.message.responder;

import org.apache.commons.collections.CollectionUtils;
import com.hs.mail.imap.message.response.StoreResponse;
import com.hs.mail.imap.message.response.UnsolicitedResponse;

/**
 * 
 * @author Won Chul Doh
 * @since Aug 1, 2010
 * 
 */
public class UnsolicitedResponder extends StoreResponder {

    public UnsolicitedResponder(Responder responder) {
        super(responder.getChannel(), responder.getRequest());
    }

    public void respond(UnsolicitedResponse response) {
        if (response.isSizeChanged()) {
            untagged(response.getMessageCount() + " EXISTS\r\n");
            untagged(response.getRecentMessageCount() + " RECENT\r\n");
        } else {
            if (CollectionUtils.isNotEmpty(response.getExpungedMsns())) {
                for (Integer msn : response.getExpungedMsns()) {
                    untagged(msn + " " + "EXPUNGE\r\n");
                }
            }
            if (CollectionUtils.isNotEmpty(response.getFlagsResponses())) {
                for (StoreResponse flagResponse : response.getFlagsResponses()) {
                    respond(flagResponse);
                }
            }
        }
    }
}

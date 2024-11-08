package com.ericsson.xsmp.service.event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import javax.activation.DataHandler;
import org.apache.commons.io.IOUtils;
import org.spark.util.GuidUtil;
import com.ericsson.xsmp.common.mq.MessageQueue;
import com.ericsson.xsmp.common.mq.SimpleMessageQueue;
import com.ericsson.xsmp.core.SessionContextUtil;

public class DefaultEventMgmtImpl extends AbstractEventMgmt {

    MessageQueue queue;

    String attachmentStorge = System.getProperty("java.io.tmpdir");

    MessageQueue getQueue() {
        if (queue != null) return queue;
        synchronized (this) {
            if (queue != null) return queue;
            queue = new SimpleMessageQueue(5, 2);
            ((SimpleMessageQueue) queue).setConsumerFactory(new EventConsumerFactory(this));
        }
        return queue;
    }

    public void setQueue(MessageQueue queue) {
        this.queue = queue;
    }

    public String getAttachmentStorge() {
        return attachmentStorge;
    }

    public void setAttachmentStorge(String savePath) {
        this.attachmentStorge = savePath;
    }

    public String postEvent(EventDocument eventDoc, Map attachments) {
        if (eventDoc == null || eventDoc.getEvent() == null) return null;
        if (queue == null) {
            sendEvent(eventDoc, attachments);
            return eventDoc.getEvent().getEventId();
        }
        if (attachments != null) {
            Iterator iter = attachments.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                if (entry.getValue() instanceof DataHandler) {
                    File file = new File(attachmentStorge + "/" + GuidUtil.generate() + entry.getKey());
                    try {
                        IOUtils.copy(((DataHandler) entry.getValue()).getInputStream(), new FileOutputStream(file));
                        entry.setValue(file);
                    } catch (IOException err) {
                        err.printStackTrace();
                    }
                }
            }
        }
        InternalEventObject eventObj = new InternalEventObject();
        eventObj.setEventDocument(eventDoc);
        eventObj.setAttachments(attachments);
        eventObj.setSessionContext(SessionContextUtil.getCurrentContext());
        eventDoc.getEvent().setEventId(GuidUtil.generate());
        getQueue().post(eventObj);
        return eventDoc.getEvent().getEventId();
    }
}

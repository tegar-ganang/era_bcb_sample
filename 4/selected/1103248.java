package com.ericsson.xsmp.service.event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import org.apache.commons.io.IOUtils;
import org.spark.util.GuidUtil;
import org.springframework.jms.core.JmsTemplate;
import com.ericsson.xsmp.core.SessionContextUtil;

public class JMSEventMgmtImpl extends AbstractEventMgmt {

    JmsTemplate jmsTemplate;

    String destinationName;

    String attachmentStorge = System.getProperty("java.io.tmpdir");

    public String getAttachmentStorge() {
        return attachmentStorge;
    }

    public void setAttachmentStorge(String savePath) {
        this.attachmentStorge = savePath;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public String postEvent(EventDocument eventDoc, Map attachments) {
        if (eventDoc == null || eventDoc.getEvent() == null) return null;
        if (jmsTemplate == null) {
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
        if (destinationName != null) jmsTemplate.convertAndSend(destinationName, eventObj); else jmsTemplate.convertAndSend(eventObj);
        return eventDoc.getEvent().getEventId();
    }

    public void handleMessage(Serializable message) {
        if (message instanceof InternalEventObject) {
            InternalEventObject event = (InternalEventObject) message;
            Map originAttachmentMap = event.getAttachments();
            Map attachmentMap = null;
            if (originAttachmentMap != null) {
                attachmentMap = new HashMap();
                Iterator iter = originAttachmentMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry.getValue() instanceof File) attachmentMap.put(entry.getKey(), new DataHandler(new FileDataSource((File) entry.getValue()))); else attachmentMap.put(entry.getKey(), entry.getValue());
                }
                event.setAttachments(attachmentMap);
            }
            try {
                processEvent(event);
            } finally {
                if (originAttachmentMap != null) {
                    Iterator iter = originAttachmentMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        if (entry.getValue() instanceof File) {
                            try {
                                ((File) entry.getValue()).delete();
                            } catch (Throwable err) {
                            }
                        }
                    }
                }
            }
        } else System.out.println("Unknown jms message!");
    }
}

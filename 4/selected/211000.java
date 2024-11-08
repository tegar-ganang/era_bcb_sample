package com.atosorigin.nl.jspring2008.buzzword.dummy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * @author Jeroen Benckhuijsen (jeroen.benckhuijsen@gmail.com)
 * 
 */
public class ContentDispatcherImpl extends JmsTemplate implements ContentDispatcher {

    private static final Logger LOG = Logger.getLogger(ContentDispatcherImpl.class);

    /**
	 * 
	 */
    public ContentDispatcherImpl() {
        super();
    }

    @Override
    public void dispatchContent(InputStream is) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending content message over JMS");
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(is, bos);
        this.send(new MessageCreator() {

            @Override
            public Message createMessage(Session session) throws JMSException {
                BytesMessage message = session.createBytesMessage();
                message.writeBytes(bos.toByteArray());
                return message;
            }
        });
    }
}

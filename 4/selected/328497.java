package de.objectcode.openk.soa.test;

import static org.testng.Assert.assertNotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import de.objectcode.openk.soa.model.v1.DocumentType;
import de.objectcode.openk.soa.model.v1.EventDocumentMessage;
import de.objectcode.openk.soa.model.v1.EventType;
import de.objectcode.soa.common.mfm.api.normalize.NormalizedHelper;

@Test(groups = "event-document")
public class DocumentEventTest {

    TopicConnection connection;

    TopicSession session;

    Topic topic;

    @BeforeClass
    public void initialize() throws Exception {
        InitialContext iniCtx = new InitialContext();
        Object tmp = iniCtx.lookup("ConnectionFactory");
        TopicConnectionFactory tcf = (TopicConnectionFactory) tmp;
        connection = tcf.createTopicConnection();
        topic = (Topic) iniCtx.lookup("topic/event-document");
        assertNotNull(topic);
        session = connection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    @Test(enabled = false)
    public void createDocument() throws Exception {
        MessageProducer producer = session.createProducer(topic);
        TextMessage message = session.createTextMessage();
        EventType event = new EventType();
        event.setUid("0");
        event.setAction(EventType.EventActionEnum.CREATE);
        event.setApplication("integration-test");
        event.setApplicationuid(new Date().toString());
        DocumentType document = new DocumentType();
        document.setName("test-document");
        document.setContentType("application/pdf");
        document.setContent(readResource("/de/objectcode/openk/soa/test/test-document.pdf"));
        document.setCharacterEncoding("UTF-8");
        document.setRepositoryLocation("test-folder");
        EventDocumentMessage eventDocument = new EventDocumentMessage();
        eventDocument.setEvent(event);
        eventDocument.setDocument(document);
        message.setText(NormalizedHelper.toXML(eventDocument));
        producer.send(message);
        producer.close();
    }

    @AfterClass
    public void shutdown() throws Exception {
        session.close();
        connection.stop();
        connection.close();
    }

    public byte[] readResource(String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int readed;
        while ((readed = is.read(buffer)) > 0) {
            bos.write(buffer, 0, readed);
        }
        is.close();
        bos.close();
        return bos.toByteArray();
    }
}

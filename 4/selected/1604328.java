package gawky.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

public class MailParter {

    String bodytext;

    ArrayList<Attachment> attachments;

    Message message;

    public MailParter(Message message) throws MessagingException, IOException {
        this.message = message;
        attachments = new ArrayList<Attachment>();
        if (message.getContentType().startsWith("text/plain")) bodytext = (String) message.getContent(); else handlePart(message);
    }

    private void handlePart(final Part part) throws MessagingException, IOException {
        if (part.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                handlePart(multipart.getBodyPart(i));
            }
            return;
        }
        if (part.getContentType().startsWith("text/html") || part.getContentType().startsWith("text/plain")) {
            if (bodytext == null) bodytext = (String) part.getContent(); else bodytext = bodytext + (String) part.getContent();
        } else if (!part.getContentType().startsWith("text/plain")) {
            Attachment attachment = new Attachment();
            attachment.setContenttype(part.getContentType());
            attachment.setFilename(part.getFileName());
            InputStream in = part.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count = 0;
            while ((count = in.read(buffer)) >= 0) bos.write(buffer, 0, count);
            in.close();
            attachment.setContent(bos.toByteArray());
            attachments.add(attachment);
        }
    }

    public int getAttachmentCount() {
        return attachments.size();
    }

    public Attachment getAttachment(int i) {
        return attachments.get(i);
    }

    public String getBodytext() {
        return bodytext;
    }

    public int getMessageNumber() throws Exception {
        return message.getMessageNumber();
    }

    public String getSubject() throws Exception {
        return message.getSubject();
    }

    public String getFrom() throws Exception {
        return ((InternetAddress) message.getFrom()[0]).getAddress();
    }

    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}

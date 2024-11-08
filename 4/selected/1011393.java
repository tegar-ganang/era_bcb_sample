package portal.presentation.messenger;

import hambo.app.base.SimplePage;
import hambo.util.HamboException;
import hambo.pim.PartId;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import hambo.messaging.Messaging;

public class AttachmentViewer extends SimplePage {

    public AttachmentViewer() {
        super(true);
    }

    protected void processPage() throws HamboException {
        try {
            long msgnr = getLongParameter("msg", 0);
            String partid = getParameter("part");
            Part part = Messaging.getMailStorage(user_id).getMessage(msgnr);
            if (partid != null && !partid.trim().equals("")) {
                part = locatePart((Multipart) part.getContent(), PartId.valueOf(partid));
                response.setContentType(part.getContentType());
                OutputStream out = response.getOutputStream();
                InputStream source = part.getInputStream();
                int b;
                while ((b = source.read()) != -1) out.write(b);
            } else {
                response.setContentType("message/rfc822");
                part.writeTo(response.getOutputStream());
            }
        } catch (IOException err) {
            throw new HamboException("Failed to view attachment", err);
        } catch (MessagingException err) {
            throw new HamboException("Failed to view attachment", err);
        }
    }

    protected Part locatePart(Multipart container, PartId partid) throws MessagingException, IOException, HamboException {
        if (partid.getParent() != null) {
            Part parent = locatePart(container, partid.getParent());
            if (parent instanceof Multipart) {
                container = (Multipart) parent;
            } else {
                Object p = parent.getContent();
                if (p instanceof MimeMessage) {
                    p = ((MimeMessage) p).getContent();
                }
                container = (Multipart) p;
            }
        }
        return container.getBodyPart(partid.getIndex());
    }
}

package net.sf.istcontract.wsimport.encoding;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import net.sf.istcontract.wsimport.api.message.Attachment;
import net.sf.istcontract.wsimport.developer.StreamingAttachmentFeature;
import net.sf.istcontract.wsimport.developer.StreamingDataHandler;
import net.sf.istcontract.wsimport.util.ByteArrayBuffer;
import net.sf.istcontract.wsimport.util.ByteArrayDataSource;
import org.jvnet.mimepull.MIMEMessage;
import org.jvnet.mimepull.MIMEPart;
import javax.activation.DataHandler;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Mime multipart message into primary part and attachment parts. It
 * parses the stream lazily as and when required.
 *
 * @author Vivek Pandey
 * @author Jitendra Kotamraju
 */
public final class MimeMultipartParser {

    private final String start;

    private final MIMEMessage message;

    private Attachment root;

    private final Map<String, Attachment> attachments = new HashMap<String, Attachment>();

    private boolean gotAll;

    public MimeMultipartParser(InputStream in, String contentType, StreamingAttachmentFeature feature) {
        ContentType ct = new ContentType(contentType);
        String boundary = ct.getParameter("boundary");
        if (boundary == null || boundary.equals("")) {
            throw new WebServiceException("MIME boundary parameter not found" + contentType);
        }
        message = (feature != null) ? new MIMEMessage(in, boundary, feature.getConfig()) : new MIMEMessage(in, boundary);
        String st = ct.getParameter("start");
        if (st != null && st.length() > 2 && st.charAt(0) == '<' && st.charAt(st.length() - 1) == '>') {
            st = st.substring(1, st.length() - 1);
        }
        start = st;
    }

    /**
     * Parses the stream and returns the root part. If start parameter is
     * present in Content-Type, it is used to determine the root part, otherwise
     * root part is the first part.
     *
     * @return StreamAttachment for root part
     *         null if root part cannot be found
     *
     */
    @Nullable
    public Attachment getRootPart() {
        if (root == null) {
            root = new PartAttachment((start != null) ? message.getPart(start) : message.getPart(0));
        }
        return root;
    }

    /**
     * Parses the entire stream and returns all MIME parts except root MIME part.
     *
     * @return Map<String, StreamAttachment> for all attachment parts
     */
    @NotNull
    public Map<String, Attachment> getAttachmentParts() {
        if (!gotAll) {
            MIMEPart rootPart = (start != null) ? message.getPart(start) : message.getPart(0);
            List<MIMEPart> parts = message.getAttachments();
            for (MIMEPart part : parts) {
                if (part != rootPart) {
                    PartAttachment attach = new PartAttachment(part);
                    attachments.put(attach.getContentId(), attach);
                }
            }
            gotAll = true;
        }
        return attachments;
    }

    /**
     * This method can be called to get a matching MIME attachment part for the
     * given contentId. It parses the stream until it finds a matching part.
     *
     * @return StreamAttachment attachment for contentId
     *         null if there is no attachment for contentId
     */
    @Nullable
    public Attachment getAttachmentPart(String contentId) throws IOException {
        Attachment attach = attachments.get(contentId);
        if (attach == null) {
            MIMEPart part = message.getPart(contentId);
            attach = new PartAttachment(part);
            attachments.put(contentId, attach);
        }
        return attach;
    }

    static class PartAttachment implements Attachment {

        final MIMEPart part;

        byte[] buf;

        PartAttachment(MIMEPart part) {
            this.part = part;
        }

        @NotNull
        public String getContentId() {
            return part.getContentId();
        }

        @NotNull
        public String getContentType() {
            return part.getContentType();
        }

        public byte[] asByteArray() {
            if (buf == null) {
                ByteArrayBuffer baf = new ByteArrayBuffer();
                try {
                    baf.write(part.readOnce());
                } catch (IOException ioe) {
                    throw new WebServiceException(ioe);
                }
                buf = baf.toByteArray();
            }
            return buf;
        }

        public DataHandler asDataHandler() {
            return (buf != null) ? new DataHandler(new ByteArrayDataSource(buf, getContentType())) : new StreamingDataHandler(part);
        }

        public Source asSource() {
            return (buf != null) ? new StreamSource(new ByteArrayInputStream(buf)) : new StreamSource(part.readOnce());
        }

        public InputStream asInputStream() {
            return (buf != null) ? new ByteArrayInputStream(buf) : part.readOnce();
        }

        public void writeTo(OutputStream os) throws IOException {
            if (buf != null) {
                os.write(buf);
            } else {
                InputStream in = part.readOnce();
                byte[] temp = new byte[8192];
                int len;
                while ((len = in.read(temp)) != -1) {
                    os.write(temp, 0, len);
                }
            }
        }

        public void writeTo(SOAPMessage saaj) throws SOAPException {
            saaj.createAttachmentPart().setDataHandler(asDataHandler());
        }
    }
}

package org.jaffa.components.attachment.services;

import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.log4j.Logger;
import org.jaffa.components.attachment.domain.Attachment;
import org.jaffa.exceptions.ApplicationExceptions;
import org.jaffa.exceptions.FrameworkException;
import org.jaffa.persistence.IPersistent;
import org.jaffa.persistence.UOW;
import org.jaffa.persistence.util.PersistentHelper;
import org.jaffa.util.ExceptionHelper;
import org.jaffa.util.JAXBHelper;

/** This class provides various means of generating attachments.
 */
public class AttachmentService {

    private static final Logger log = Logger.getLogger(AttachmentService.class);

    /** A content-type to indicate that the attachment is an array of bytes.
     * Force the contentType to a nonstandard value such as 'application/x-download', so that the suggested filename will be used when saving it from the browser.
     * If we do not pass any contentType, then both IE and FireFox will treat the file as HTMl and append a '.html' extension to the filename.
     * If we pass contentType as 'application/octet-stream', IE appends a '.txt' extension to the filename.
     * If we pass contentType as 'application/base64', FireFox appends a random extension to the filename.
     * Check http://www.onjava.com/pub/a/onjava/excerpt/jebp_3/index3.html for more information.
     */
    public static final String CONTENT_TYPE_BYTE_ARRAY = "application/x-download";

    /** A content-type to indicate that the attachment was generated from a String, and has plain text. */
    public static final String CONTENT_TYPE_STRING = "text/plain";

    /** A content-type to indicate that the attachment was generated using JAXB, and has XML data. */
    public static final String CONTENT_TYPE_JAXB = "text/xml";

    /** A content-type to indicate that the attachment was generated using XmlEncoder, and has XML data. */
    public static final String CONTENT_TYPE_XML_ENCODER = "text/xml";

    /** Creates an Attachment domain object based on the input.
     * The domain object is persisted.
     * @param attachTo the domain object to which the attachment is related. It is used for determining the serializedKey of the Attachment domain object being created.
     * @param attachment the attachment. It is used for determining the data and originalFileName of the Attachment domain object being created.
     * @throws FrameworkException If any system error occurs.
     * @throws ApplicationExceptions If any application error occurs.
     */
    public static void attach(IPersistent attachTo, Object attachment) throws FrameworkException, ApplicationExceptions {
        UOW uow = null;
        try {
            uow = new UOW();
            attach(uow, attachTo, attachment);
            uow.commit();
        } catch (Exception e) {
            throw ExceptionHelper.throwAFR(e);
        } finally {
            if (uow != null) uow.rollback();
        }
    }

    /** Creates an Attachment domain object based on the input.
     * The domain object is added to the input UOW instance.
     * @param uow The UOW.
     * @param attachTo the domain object to which the attachment is related. It is used for determining the serializedKey of the Attachment domain object being created.
     * @param attachment the attachment. It is used for determining the data and originalFileName of the Attachment domain object being created.
     * @throws FrameworkException If any system error occurs.
     * @throws ApplicationExceptions If any application error occurs.
     */
    public static void attach(UOW uow, IPersistent attachTo, Object attachment) throws FrameworkException, ApplicationExceptions {
        if (log.isDebugEnabled()) log.debug("Attaching " + attachment + " to " + attachTo);
        uow.add(createAttachment(attachTo, attachment));
    }

    /** Creates an Attachment domain object based on the input.
     * @param attachTo the domain object to which the attachment is related. It is used for determining the serializedKey of the Attachment domain object being created.
     * @param attachment the attachment. It is used for determining the data and originalFileName of the Attachment domain object being created.
     * @throws FrameworkException If any system error occurs.
     * @throws ApplicationExceptions If any application error occurs.
     * @return the Attachment domain object.
     */
    public static Attachment createAttachment(IPersistent attachTo, Object attachment) throws FrameworkException, ApplicationExceptions {
        try {
            Attachment a = new Attachment();
            a.setSerializedKey(PersistentHelper.generateSerializedKey(attachTo));
            a.setAttachmentType("E");
            a.setData(createAttachmentData(attachment));
            a.setOriginalFileName(createAttachmentName(attachment));
            a.setContentType(createContentType(attachment));
            if (log.isDebugEnabled()) log.debug("Created attachment object " + a);
            return a;
        } catch (Exception e) {
            throw ExceptionHelper.throwAFR(e);
        }
    }

    /** Converts the input attachment into an array of bytes.
     * The following algorithm is used for conversion
     *   - If the input is an array of bytes, then it is returned as is.
     *   - If the input is a String, then its getBytes() method is used to generate the output.
     *   - If the input is a File, then its contents are loaded to generate the output.
     *   - If the input carries the JAXB annotation, then it is marshalled into XML using JAXB.
     *   - If none of the above are satisfied, the attachment is marshalled into XML using the XMLEncoder.
     * @param attachment the attachment to be converted
     * @throws FrameworkException If any system error occurs.
     * @throws ApplicationExceptions If any application error occurs.
     * @return the attachment as an array of bytes.
     */
    public static byte[] createAttachmentData(Object attachment) throws FrameworkException, ApplicationExceptions {
        try {
            byte[] data = null;
            if (attachment.getClass().isArray() && attachment.getClass().getComponentType() == Byte.TYPE) {
                if (log.isDebugEnabled()) log.debug("A byte[] has been passed in. Will be used as is");
                data = (byte[]) attachment;
            } else if (attachment instanceof String) {
                if (log.isDebugEnabled()) log.debug("A String has been passed in. The getBytes() method will be used to create attachment data");
                data = ((String) attachment).getBytes();
            } else if (attachment instanceof File) {
                if (log.isDebugEnabled()) log.debug("A File has been passed in. The File contents will be used to create attachment data");
                InputStream is = null;
                ByteArrayOutputStream bos = null;
                try {
                    is = new BufferedInputStream(new FileInputStream((File) attachment));
                    bos = new ByteArrayOutputStream();
                    int b;
                    while ((b = is.read()) != -1) bos.write(b);
                    bos.flush();
                    data = bos.toByteArray();
                } finally {
                    try {
                        if (bos != null) bos.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (is != null) is.close();
                    } catch (IOException e) {
                    }
                }
            } else if (attachment.getClass().isAnnotationPresent(XmlRootElement.class)) {
                if (log.isDebugEnabled()) log.debug(attachment.getClass().getName() + " has the 'XmlRootElement' JAXB annotation, and hence will be marshalled using JAXB");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                JAXBContext jc = JAXBHelper.obtainJAXBContext(attachment.getClass());
                Marshaller marshaller = jc.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(attachment, new BufferedOutputStream(os));
                data = os.toByteArray();
            } else {
                if (log.isDebugEnabled()) log.debug(attachment.getClass().getName() + " does not have the 'XmlRootElement' JAXB annotation, and hence will be marshalled using XMLEncoder");
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                XMLEncoder e = new XMLEncoder(new BufferedOutputStream(os));
                e.writeObject(attachment);
                e.close();
                data = os.toByteArray();
            }
            return data;
        } catch (Exception e) {
            throw ExceptionHelper.throwAFR(e);
        }
    }

    /** Returns a name for the attachment.
     * For now, it simply returns the class name of the input, unless it is a File, in which case the file name is returned.
     * @param attachment the attachment.
     * @throws FrameworkException If any system error occurs.
     * @throws ApplicationExceptions If any application error occurs.
     * @return a name for the attachment.
     */
    public static String createAttachmentName(Object attachment) throws FrameworkException, ApplicationExceptions {
        String attachmentName = attachment instanceof File ? ((File) attachment).getName() : attachment.getClass().getName();
        if (log.isDebugEnabled()) log.debug("Attachment name is " + attachmentName);
        return attachmentName;
    }

    /** Returns the contentType for the attachment.
     * The following algorithm is used
     *   - If the input is an array of bytes, then "application/x-download" is returned.
     *   - If the input is a String, then "text/plain" is returned.
     *   - If the input is a File, then a null is returned. The file name should be used to determine the contentType.
     *   - If the input carries the JAXB annotation, then "text/xml" is returned.
     *   - If none of the above are satisfied, then "text/xml" is returned.
     * @param attachment the attachment.
     * @throws FrameworkException If any system error occurs.
     * @throws ApplicationExceptions If any application error occurs.
     * @return the contentType for the attachment.
     */
    public static String createContentType(Object attachment) throws FrameworkException, ApplicationExceptions {
        String contentType = null;
        if (attachment.getClass().isArray() && attachment.getClass().getComponentType() == Byte.TYPE) {
            contentType = CONTENT_TYPE_BYTE_ARRAY;
        } else if (attachment instanceof String) {
            contentType = CONTENT_TYPE_STRING;
        } else if (attachment instanceof File) {
        } else if (attachment.getClass().isAnnotationPresent(XmlRootElement.class)) {
            contentType = CONTENT_TYPE_JAXB;
        } else {
            contentType = CONTENT_TYPE_XML_ENCODER;
        }
        return contentType;
    }
}

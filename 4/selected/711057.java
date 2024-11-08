package com.windsor.node.ws2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataHandler;
import javax.mail.internet.MimeUtility;
import javax.xml.namespace.QName;
import net.exchangenetwork.www.schema.node._2.AttachmentType;
import net.exchangenetwork.www.schema.node._2.DocumentFormatType;
import net.exchangenetwork.www.schema.node._2.GenericXmlType;
import net.exchangenetwork.www.schema.node._2.NodeDocumentType;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axiom.om.util.Base64;
import org.apache.axis2.databinding.types.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3.www._2005._05.xmlmime.ContentType_type0;
import sun.misc.BASE64Decoder;
import com.windsor.node.common.domain.CommonContentType;
import com.windsor.node.common.domain.Document;
import com.windsor.node.common.util.CommonContentAndFormatConverter;
import com.windsor.node.ws2.Endpoint2FaultMessage;

public class NodeUtil {

    public static Document getDocumentFromNodeDocumentType(NodeDocumentType wsdlDoc) {
        try {
            Document wnosDoc = new Document();
            wnosDoc.setDocumentId(null);
            wnosDoc.setDocumentName(wsdlDoc.getDocumentName());
            wnosDoc.setType(CommonContentAndFormatConverter.convert(wsdlDoc.getDocumentFormat().getValue()));
            wnosDoc.setContent(inputStreamToBytes(wsdlDoc.getDocumentContent().getBase64Binary().getInputStream()));
            return wnosDoc;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] encode(byte[] b) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream b64os = MimeUtility.encode(baos, "Base64");
            b64os.write(b);
            b64os.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error while encoding bytes", ex);
        }
    }

    public static byte[] decode(byte[] b) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(b);
            InputStream b64is = MimeUtility.decode(bais, "Base64");
            byte[] tmp = new byte[b.length];
            int n = b64is.read(tmp);
            byte[] res = new byte[n];
            System.arraycopy(tmp, 0, res, 0, n);
            return res;
        } catch (Exception ex) {
            throw new RuntimeException("Error while decoding bytes", ex);
        }
    }

    public static byte[] inputStreamToBytes(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Error while converting stream to bytes", ex);
        }
    }

    /**
     * getGenericXmlType
     * 
     * @param type
     * @param content
     * @return
     * @throws Endpoint2FaultMessage
     */
    public static GenericXmlType getGenericXmlType(CommonContentType type, byte[] content) {
        Logger LOGGER = LoggerFactory.getLogger(NodeUtil.class);
        LOGGER.error("[getGenericXmlType]: type: " + type);
        try {
            GenericXmlType gxt = new GenericXmlType();
            if (type == CommonContentType.XML) {
                gxt.setFormat(DocumentFormatType.XML);
                gxt.setExtraElement(AXIOMUtil.stringToOM(new String(content, "UTF-8")));
            } else if (type == CommonContentType.ZIP) {
                OMElement zip = OMAbstractFactory.getOMFactory().createOMElement(new QName("base64Zip"));
                zip.setText(Base64.encode(content));
                gxt.setFormat(DocumentFormatType.ZIP);
                gxt.setExtraElement(zip);
            } else {
                gxt.setFormat(DocumentFormatType.OTHER);
                gxt.setExtraElement(AXIOMUtil.stringToOM(new String(content)));
            }
            return gxt;
        } catch (Exception ex) {
            LOGGER.error("[getGenericXmlType]: ERROR: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public static Document getBytesFromGenericXmlType(GenericXmlType content) {
        Logger LOGGER = LoggerFactory.getLogger(NodeUtil.class);
        LOGGER.debug("[getBytesFromGenericXmlType]:");
        try {
            Document resultDoc = new Document();
            if (content.getFormat() == DocumentFormatType.ZIP) {
                resultDoc.setType(CommonContentType.ZIP);
                resultDoc.setContent(new BASE64Decoder().decodeBuffer(content.getExtraElement().getText()));
            } else {
                resultDoc.setType(CommonContentType.XML);
                resultDoc.setContent(content.getExtraElement().toString().getBytes("UTF-8"));
            }
            return resultDoc;
        } catch (Exception ex) {
            LOGGER.error("[getBytesFromGenericXmlType]: ERROR: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    public static NodeDocumentType getNodeDocumentFromWnosDoc(Document wnosDoc) {
        Logger LOGGER = LoggerFactory.getLogger(NodeUtil.class);
        LOGGER.debug("Creating attachment from WNOS Doc: " + wnosDoc);
        try {
            NodeDocumentType newDoc = new NodeDocumentType();
            AttachmentType attachment = new AttachmentType();
            LOGGER.debug("setting documentFormat to " + wnosDoc.getType().getName());
            newDoc.setDocumentFormat(DocumentFormatType.Factory.fromValue(wnosDoc.getType().getName()));
            LOGGER.debug("setting documentId to " + wnosDoc.getDocumentId());
            newDoc.setDocumentId(new Id(wnosDoc.getDocumentId()));
            LOGGER.debug("setting documentName to " + wnosDoc.getDocumentName());
            newDoc.setDocumentName(wnosDoc.getDocumentName());
            ContentType_type0 contentType = new ContentType_type0();
            if (wnosDoc.getType().equals(CommonContentType.XML_STR)) {
                contentType.setContentType_type0("application/xml");
            } else {
                contentType.setContentType_type0("application/octet-stream");
            }
            LOGGER.error("Set contentType to " + contentType.getContentType_type0());
            LOGGER.error("Creating datasource for the attachment...");
            ByteArrayDataSource bads = new ByteArrayDataSource(wnosDoc.getContent());
            LOGGER.error("Creating data handler...");
            attachment.setBase64Binary(new DataHandler(bads));
            attachment.setContentType(contentType);
            newDoc.setDocumentContent(attachment);
            LOGGER.debug("Attachment ready to go,");
            return newDoc;
        } catch (Exception ex) {
            LOGGER.error("[getNodeDocumentFromWnosDoc]: ERROR: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}

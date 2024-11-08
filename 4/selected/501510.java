package org.marre.wap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import org.marre.mime.MimeHeader;
import org.marre.util.StringUtil;
import org.marre.wap.util.WspUtil;

/**
 *
 * @author Markus Eriksson
 * @version $Id: MmsHeaderEncoder.java 280 2003-11-08 11:26:42Z c95men $
 */
public class MmsHeaderEncoder {

    private MmsHeaderEncoder() {
    }

    /**
     * Writes a wsp encoded content-location header as specified in
     * WAP-230-WSP-20010705-a.pdf.
     *
     * @param theOs
     * @param theContentLocation
     * @throws IOException
     */
    public static void writeHeaderContentLocation(OutputStream theOs, String theContentLocation) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_CONTENT_LOCATION);
        WspUtil.writeTextString(theOs, theContentLocation);
    }

    public static void writeHeaderContentType(OutputStream theOs, String theContentType) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_CONTENT_TYPE);
        WspUtil.writeContentType(theOs, theContentType);
    }

    public static void writeHeaderContentType(OutputStream theOs, MimeHeader theContentType) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_CONTENT_TYPE);
        WspUtil.writeContentType(theOs, theContentType);
    }

    public static void writeEncodedStringValue(OutputStream baos, String theStringValue) throws IOException {
        WspUtil.writeTextString(baos, theStringValue);
    }

    public static void writeHeaderXMmsMessageType(OutputStream theOs, int theMessageTypeId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_MESSAGE_TYPE);
        WspUtil.writeShortInteger(theOs, theMessageTypeId);
    }

    public static void writeHeaderXMmsMessageType(OutputStream theOs, String theMessageType) throws IOException {
        int messageTypeId = StringUtil.findString(MmsConstants.X_MMS_MESSAGE_TYPE_NAMES, theMessageType.toLowerCase());
        if (messageTypeId != -1) {
            writeHeaderXMmsMessageType(theOs, messageTypeId);
        }
    }

    public static void writeHeaderXMmsTransactionId(OutputStream theOs, String theTransactionId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_TRANSACTION_ID);
        WspUtil.writeTextString(theOs, theTransactionId);
    }

    public static void writeHeaderXMmsMmsVersion(OutputStream theOs, int theVersionId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_MMS_VERSION);
        switch(theVersionId) {
            case MmsConstants.X_MMS_MMS_VERSION_ID_1_0:
            default:
                WspUtil.writeShortInteger(theOs, 0x10);
                break;
        }
    }

    public static void writeHeaderXMmsMmsVersion(OutputStream theOs, String theVersion) throws IOException {
        int versionId = StringUtil.findString(MmsConstants.X_MMS_MMS_VERSION_NAMES, theVersion.toLowerCase());
        if (versionId != -1) {
            writeHeaderXMmsMessageType(theOs, versionId);
        }
    }

    public static void writeHeaderDate(OutputStream theOs, Date date) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_DATE);
        long time = date.getTime();
        WspUtil.writeLongInteger(theOs, time);
    }

    public static void writeHeaderFrom(OutputStream theOs, String theFrom) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_FROM);
        if (theFrom == null) {
            WspUtil.writeValueLength(theOs, 1);
            WspUtil.writeShortInteger(theOs, MmsConstants.FROM_INSERT_ADDRESS);
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WspUtil.writeShortInteger(baos, MmsConstants.FROM_ADDRESS_PRESENT);
            MmsHeaderEncoder.writeEncodedStringValue(baos, theFrom);
            baos.close();
            WspUtil.writeValueLength(theOs, baos.size());
            theOs.write(baos.toByteArray());
        }
    }

    public static void writeHeaderSubject(OutputStream theOs, String theSubject) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_SUBJECT);
        MmsHeaderEncoder.writeEncodedStringValue(theOs, theSubject);
    }

    public static void writeHeaderTo(OutputStream theOs, String theRecipient) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_TO);
        MmsHeaderEncoder.writeEncodedStringValue(theOs, theRecipient);
    }

    public static void writeHeaderCc(OutputStream theOs, String theRecipient) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_CC);
        MmsHeaderEncoder.writeEncodedStringValue(theOs, theRecipient);
    }

    public static void writeHeaderBcc(OutputStream theOs, String theRecipient) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_BCC);
        MmsHeaderEncoder.writeEncodedStringValue(theOs, theRecipient);
    }

    public static void writeHeaderXMmsReadReply(OutputStream theOs, int theReadReplyId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_READ_REPLY);
        WspUtil.writeShortInteger(theOs, theReadReplyId);
    }

    public static void writeHeaderXMmsReadReply(OutputStream theOs, String theReadReply) throws IOException {
        int readReplyId = StringUtil.findString(MmsConstants.X_MMS_READ_REPLY_NAMES, theReadReply.toLowerCase());
        if (readReplyId != -1) {
            writeHeaderXMmsReadReply(theOs, readReplyId);
        }
    }

    public static void writeHeaderXMmsPriority(OutputStream theOs, int thePriorityId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_PRIORITY);
        WspUtil.writeShortInteger(theOs, thePriorityId);
    }

    public static void writeHeaderXMmsPriority(OutputStream theOs, String thePriority) throws IOException {
        int priorityId = StringUtil.findString(MmsConstants.X_MMS_PRIORITY_NAMES, thePriority.toLowerCase());
        if (priorityId != -1) {
            writeHeaderXMmsPriority(theOs, priorityId);
        }
    }

    public static void writeHeaderXMmsStatus(OutputStream theOs, int theStatusId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_STATUS);
        WspUtil.writeShortInteger(theOs, theStatusId);
    }

    public static void writeHeaderXMmsStatus(OutputStream theOs, String theStatus) throws IOException {
        int statusId = StringUtil.findString(MmsConstants.X_MMS_STATUS_NAMES, theStatus.toLowerCase());
        if (statusId != -1) {
            writeHeaderXMmsStatus(theOs, statusId);
        }
    }

    public static void writeHeaderXMmsMessageClass(OutputStream theOs, int theMessageClassId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_MESSAGE_CLASS);
        WspUtil.writeShortInteger(theOs, theMessageClassId);
    }

    public static void writeHeaderXMmsMessageClass(OutputStream theOs, String theMessageClass) throws IOException {
        int messageClassId = StringUtil.findString(MmsConstants.X_MMS_MESSAGE_CLASS_NAMES, theMessageClass.toLowerCase());
        if (messageClassId != -1) {
            writeHeaderXMmsMessageClass(theOs, messageClassId);
        }
    }

    public static void writeHeaderXMmsMessageSize(OutputStream theOs, long messageSize) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_MESSAGE_SIZE);
        WspUtil.writeLongInteger(theOs, messageSize);
    }

    public static void writeHeaderXMmsExpiryAbsolute(OutputStream theOs, long theExpiry) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WspUtil.writeShortInteger(baos, MmsConstants.ABSOLUTE_TOKEN);
        WspUtil.writeLongInteger(baos, theExpiry);
        baos.close();
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_EXPIRY);
        WspUtil.writeValueLength(theOs, baos.size());
        theOs.write(baos.toByteArray());
    }

    public static void writeHeaderXMmsExpiryAbsolute(OutputStream theOs, Date theExpiry) throws IOException {
        writeHeaderXMmsExpiryAbsolute(theOs, theExpiry.getTime());
    }

    public static void writeHeaderXMmsExpiryRelative(OutputStream theOs, long theExpiry) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WspUtil.writeShortInteger(baos, MmsConstants.RELATIVE_TOKEN);
        WspUtil.writeLongInteger(baos, theExpiry);
        baos.close();
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_EXPIRY);
        WspUtil.writeValueLength(theOs, baos.size());
        theOs.write(baos.toByteArray());
    }

    public static void writeHeaderXMmsSenderVisibility(OutputStream theOs, int theVisibilityId) throws IOException {
        WspUtil.writeShortInteger(theOs, MmsConstants.HEADER_ID_X_MMS_SENDER_VISIBILITY);
        WspUtil.writeShortInteger(theOs, theVisibilityId);
    }

    public static void writeHeaderXMmsSenderVisibility(OutputStream theOs, String theVisibility) throws IOException {
        int visibilityId = StringUtil.findString(MmsConstants.X_MMS_SENDER_VISIBILITY_NAMES, theVisibility.toLowerCase());
        if (visibilityId != -1) {
            writeHeaderXMmsSenderVisibility(theOs, visibilityId);
        }
    }

    public static void writeApplicationHeader(OutputStream theOs, String theName, String theValue) throws IOException {
        WspUtil.writeTokenText(theOs, theName);
        WspUtil.writeTextString(theOs, theValue);
    }
}

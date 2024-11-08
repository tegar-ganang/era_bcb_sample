package com.android.email;

import com.android.email.mail.Address;
import com.android.email.mail.Flag;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Part;
import com.android.email.mail.Message.RecipientType;
import com.android.email.mail.internet.MimeBodyPart;
import com.android.email.mail.internet.MimeHeader;
import com.android.email.mail.internet.MimeMessage;
import com.android.email.mail.internet.MimeMultipart;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.mail.internet.TextBody;
import com.android.email.provider.AttachmentProvider;
import com.android.email.provider.EmailContent;
import com.android.email.provider.EmailContent.Attachment;
import com.android.email.provider.EmailContent.AttachmentColumns;
import org.apache.commons.io.IOUtils;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

public class LegacyConversions {

    /** DO NOT CHECK IN "TRUE" */
    private static final boolean DEBUG_ATTACHMENTS = false;

    static final String BODY_QUOTED_PART_REPLY = "quoted-reply";

    static final String BODY_QUOTED_PART_FORWARD = "quoted-forward";

    static final String BODY_QUOTED_PART_INTRO = "quoted-intro";

    /**
     * Copy field-by-field from a "store" message to a "provider" message
     * @param message The message we've just downloaded (must be a MimeMessage)
     * @param localMessage The message we'd like to write into the DB
     * @result true if dirty (changes were made)
     */
    public static boolean updateMessageFields(EmailContent.Message localMessage, Message message, long accountId, long mailboxId) throws MessagingException {
        Address[] from = message.getFrom();
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        Address[] cc = message.getRecipients(Message.RecipientType.CC);
        Address[] bcc = message.getRecipients(Message.RecipientType.BCC);
        Address[] replyTo = message.getReplyTo();
        String subject = message.getSubject();
        Date sentDate = message.getSentDate();
        Date internalDate = message.getInternalDate();
        if (from != null && from.length > 0) {
            localMessage.mDisplayName = from[0].toFriendly();
        }
        if (sentDate != null) {
            localMessage.mTimeStamp = sentDate.getTime();
        }
        if (subject != null) {
            localMessage.mSubject = subject;
        }
        localMessage.mFlagRead = message.isSet(Flag.SEEN);
        if (localMessage.mFlagLoaded != EmailContent.Message.FLAG_LOADED_COMPLETE) {
            if (localMessage.mDisplayName == null || "".equals(localMessage.mDisplayName)) {
                localMessage.mFlagLoaded = EmailContent.Message.FLAG_LOADED_UNLOADED;
            } else {
                localMessage.mFlagLoaded = EmailContent.Message.FLAG_LOADED_PARTIAL;
            }
        }
        localMessage.mFlagFavorite = message.isSet(Flag.FLAGGED);
        localMessage.mServerId = message.getUid();
        if (internalDate != null) {
            localMessage.mServerTimeStamp = internalDate.getTime();
        }
        try {
            localMessage.mMessageId = ((MimeMessage) message).getMessageId();
        } catch (MessagingException me) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, "Missing message-id for UID=" + localMessage.mServerId);
            }
        }
        localMessage.mMailboxKey = mailboxId;
        localMessage.mAccountKey = accountId;
        if (from != null && from.length > 0) {
            localMessage.mFrom = Address.pack(from);
        }
        localMessage.mTo = Address.pack(to);
        localMessage.mCc = Address.pack(cc);
        localMessage.mBcc = Address.pack(bcc);
        localMessage.mReplyTo = Address.pack(replyTo);
        return true;
    }

    /**
     * Copy body text (plain and/or HTML) from MimeMessage to provider Message
     */
    public static boolean updateBodyFields(EmailContent.Body body, EmailContent.Message localMessage, ArrayList<Part> viewables) throws MessagingException {
        body.mMessageKey = localMessage.mId;
        StringBuffer sbHtml = null;
        StringBuffer sbText = null;
        StringBuffer sbHtmlReply = null;
        StringBuffer sbTextReply = null;
        StringBuffer sbIntroText = null;
        for (Part viewable : viewables) {
            String text = MimeUtility.getTextFromPart(viewable);
            String[] replyTags = viewable.getHeader(MimeHeader.HEADER_ANDROID_BODY_QUOTED_PART);
            String replyTag = null;
            if (replyTags != null && replyTags.length > 0) {
                replyTag = replyTags[0];
            }
            boolean isHtml = "text/html".equalsIgnoreCase(viewable.getMimeType());
            if (replyTag != null) {
                boolean isQuotedReply = BODY_QUOTED_PART_REPLY.equalsIgnoreCase(replyTag);
                boolean isQuotedForward = BODY_QUOTED_PART_FORWARD.equalsIgnoreCase(replyTag);
                boolean isQuotedIntro = BODY_QUOTED_PART_INTRO.equalsIgnoreCase(replyTag);
                if (isQuotedReply || isQuotedForward) {
                    if (isHtml) {
                        sbHtmlReply = appendTextPart(sbHtmlReply, text);
                    } else {
                        sbTextReply = appendTextPart(sbTextReply, text);
                    }
                    localMessage.mFlags &= ~EmailContent.Message.FLAG_TYPE_MASK;
                    localMessage.mFlags |= isQuotedReply ? EmailContent.Message.FLAG_TYPE_REPLY : EmailContent.Message.FLAG_TYPE_FORWARD;
                    continue;
                }
                if (isQuotedIntro) {
                    sbIntroText = appendTextPart(sbIntroText, text);
                    continue;
                }
            }
            if (isHtml) {
                sbHtml = appendTextPart(sbHtml, text);
            } else {
                sbText = appendTextPart(sbText, text);
            }
        }
        if (sbText != null && sbText.length() != 0) {
            body.mTextContent = sbText.toString();
        }
        if (sbHtml != null && sbHtml.length() != 0) {
            body.mHtmlContent = sbHtml.toString();
        }
        if (sbHtmlReply != null && sbHtmlReply.length() != 0) {
            body.mHtmlReply = sbHtmlReply.toString();
        }
        if (sbTextReply != null && sbTextReply.length() != 0) {
            body.mTextReply = sbTextReply.toString();
        }
        if (sbIntroText != null && sbIntroText.length() != 0) {
            body.mIntroText = sbIntroText.toString();
        }
        return true;
    }

    /**
     * Helper function to append text to a StringBuffer, creating it if necessary.
     * Optimization:  The majority of the time we are *not* appending - we should have a path
     * that deals with single strings.
     */
    private static StringBuffer appendTextPart(StringBuffer sb, String newText) {
        if (newText == null) {
            return sb;
        } else if (sb == null) {
            sb = new StringBuffer(newText);
        } else {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(newText);
        }
        return sb;
    }

    /**
     * Copy attachments from MimeMessage to provider Message.
     *
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param attachments the attachments to add
     * @throws IOException
     */
    public static void updateAttachments(Context context, EmailContent.Message localMessage, ArrayList<Part> attachments) throws MessagingException, IOException {
        localMessage.mAttachments = null;
        for (Part attachmentPart : attachments) {
            addOneAttachment(context, localMessage, attachmentPart);
        }
    }

    /**
     * Add a single attachment part to the message
     *
     * This will skip adding attachments if they are already found in the attachments table.
     * The heuristic for this will fail (false-positive) if two identical attachments are
     * included in a single POP3 message.
     * TODO: Fix that, by (elsewhere) simulating an mLocation value based on the attachments
     * position within the list of multipart/mixed elements.  This would make every POP3 attachment
     * unique, and might also simplify the code (since we could just look at the positions, and
     * ignore the filename, etc.)
     *
     * TODO: Take a closer look at encoding and deal with it if necessary.
     *
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param part a single attachment part from POP or IMAP
     * @throws IOException
     */
    private static void addOneAttachment(Context context, EmailContent.Message localMessage, Part part) throws MessagingException, IOException {
        Attachment localAttachment = new Attachment();
        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name == null) {
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getContentType());
            name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }
        long size = 0;
        String disposition = part.getDisposition();
        if (disposition != null) {
            String s = MimeUtility.getHeaderParameter(disposition, "size");
            if (s != null) {
                size = Long.parseLong(s);
            }
        }
        String[] partIds = part.getHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA);
        String partId = partIds != null ? partIds[0] : null;
        localAttachment.mFileName = MimeUtility.getHeaderParameter(contentType, "name");
        localAttachment.mMimeType = part.getMimeType();
        localAttachment.mSize = size;
        localAttachment.mContentId = part.getContentId();
        localAttachment.mContentUri = null;
        localAttachment.mMessageKey = localMessage.mId;
        localAttachment.mLocation = partId;
        localAttachment.mEncoding = "B";
        if (DEBUG_ATTACHMENTS) {
            Log.d(Email.LOG_TAG, "Add attachment " + localAttachment);
        }
        Uri uri = ContentUris.withAppendedId(Attachment.MESSAGE_ID_URI, localMessage.mId);
        Cursor cursor = context.getContentResolver().query(uri, Attachment.CONTENT_PROJECTION, null, null, null);
        boolean attachmentFoundInDb = false;
        try {
            while (cursor.moveToNext()) {
                Attachment dbAttachment = new Attachment().restore(cursor);
                if (stringNotEqual(dbAttachment.mFileName, localAttachment.mFileName)) continue;
                if (stringNotEqual(dbAttachment.mMimeType, localAttachment.mMimeType)) continue;
                if (stringNotEqual(dbAttachment.mContentId, localAttachment.mContentId)) continue;
                if (stringNotEqual(dbAttachment.mLocation, localAttachment.mLocation)) continue;
                attachmentFoundInDb = true;
                localAttachment.mId = dbAttachment.mId;
                if (DEBUG_ATTACHMENTS) {
                    Log.d(Email.LOG_TAG, "Skipped, found db attachment " + dbAttachment);
                }
                break;
            }
        } finally {
            cursor.close();
        }
        if (!attachmentFoundInDb) {
            localAttachment.save(context);
        }
        saveAttachmentBody(context, part, localAttachment, localMessage.mAccountKey);
        if (localMessage.mAttachments == null) {
            localMessage.mAttachments = new ArrayList<Attachment>();
        }
        localMessage.mAttachments.add(localAttachment);
        localMessage.mFlagAttachment = true;
    }

    static boolean stringNotEqual(String a, String b) {
        if (a == null && b == null) return false;
        if (a == null) a = "";
        if (b == null) b = "";
        return !a.equals(b);
    }

    /**
     * Save the body part of a single attachment, to a file in the attachments directory.
     */
    public static void saveAttachmentBody(Context context, Part part, Attachment localAttachment, long accountId) throws MessagingException, IOException {
        if (part.getBody() != null) {
            long attachmentId = localAttachment.mId;
            InputStream in = part.getBody().getInputStream();
            File saveIn = AttachmentProvider.getAttachmentDirectory(context, accountId);
            if (!saveIn.exists()) {
                saveIn.mkdirs();
            }
            File saveAs = AttachmentProvider.getAttachmentFilename(context, accountId, attachmentId);
            saveAs.createNewFile();
            FileOutputStream out = new FileOutputStream(saveAs);
            long copySize = IOUtils.copy(in, out);
            in.close();
            out.close();
            String contentUriString = AttachmentProvider.getAttachmentUri(accountId, attachmentId).toString();
            localAttachment.mSize = copySize;
            localAttachment.mContentUri = contentUriString;
            ContentValues cv = new ContentValues();
            cv.put(AttachmentColumns.SIZE, copySize);
            cv.put(AttachmentColumns.CONTENT_URI, contentUriString);
            Uri uri = ContentUris.withAppendedId(Attachment.CONTENT_URI, attachmentId);
            context.getContentResolver().update(uri, cv, null, null);
        }
    }

    /**
     * Read a complete Provider message into a legacy message (for IMAP upload).  This
     * is basically the equivalent of LocalFolder.getMessages() + LocalFolder.fetch().
     */
    public static Message makeMessage(Context context, EmailContent.Message localMessage) throws MessagingException {
        MimeMessage message = new MimeMessage();
        message.setSubject(localMessage.mSubject == null ? "" : localMessage.mSubject);
        Address[] from = Address.unpack(localMessage.mFrom);
        if (from.length > 0) {
            message.setFrom(from[0]);
        }
        message.setSentDate(new Date(localMessage.mTimeStamp));
        message.setUid(localMessage.mServerId);
        message.setFlag(Flag.DELETED, localMessage.mFlagLoaded == EmailContent.Message.FLAG_LOADED_DELETED);
        message.setFlag(Flag.SEEN, localMessage.mFlagRead);
        message.setFlag(Flag.FLAGGED, localMessage.mFlagFavorite);
        message.setRecipients(RecipientType.TO, Address.unpack(localMessage.mTo));
        message.setRecipients(RecipientType.CC, Address.unpack(localMessage.mCc));
        message.setRecipients(RecipientType.BCC, Address.unpack(localMessage.mBcc));
        message.setReplyTo(Address.unpack(localMessage.mReplyTo));
        message.setInternalDate(new Date(localMessage.mServerTimeStamp));
        message.setMessageId(localMessage.mMessageId);
        message.setHeader(MimeHeader.HEADER_CONTENT_TYPE, "multipart/mixed");
        MimeMultipart mp = new MimeMultipart();
        mp.setSubType("mixed");
        message.setBody(mp);
        try {
            addTextBodyPart(mp, "text/html", null, EmailContent.Body.restoreBodyHtmlWithMessageId(context, localMessage.mId));
        } catch (RuntimeException rte) {
            Log.d(Email.LOG_TAG, "Exception while reading html body " + rte.toString());
        }
        try {
            addTextBodyPart(mp, "text/plain", null, EmailContent.Body.restoreBodyTextWithMessageId(context, localMessage.mId));
        } catch (RuntimeException rte) {
            Log.d(Email.LOG_TAG, "Exception while reading text body " + rte.toString());
        }
        boolean isReply = (localMessage.mFlags & EmailContent.Message.FLAG_TYPE_REPLY) != 0;
        boolean isForward = (localMessage.mFlags & EmailContent.Message.FLAG_TYPE_FORWARD) != 0;
        if (isReply || isForward) {
            try {
                addTextBodyPart(mp, "text/plain", BODY_QUOTED_PART_INTRO, EmailContent.Body.restoreIntroTextWithMessageId(context, localMessage.mId));
            } catch (RuntimeException rte) {
                Log.d(Email.LOG_TAG, "Exception while reading text reply " + rte.toString());
            }
            String replyTag = isReply ? BODY_QUOTED_PART_REPLY : BODY_QUOTED_PART_FORWARD;
            try {
                addTextBodyPart(mp, "text/html", replyTag, EmailContent.Body.restoreReplyHtmlWithMessageId(context, localMessage.mId));
            } catch (RuntimeException rte) {
                Log.d(Email.LOG_TAG, "Exception while reading html reply " + rte.toString());
            }
            try {
                addTextBodyPart(mp, "text/plain", replyTag, EmailContent.Body.restoreReplyTextWithMessageId(context, localMessage.mId));
            } catch (RuntimeException rte) {
                Log.d(Email.LOG_TAG, "Exception while reading text reply " + rte.toString());
            }
        }
        return message;
    }

    /**
     * Helper method to add a body part for a given type of text, if found
     *
     * @param mp The text body part will be added to this multipart
     * @param contentType The content-type of the text being added
     * @param quotedPartTag If non-null, HEADER_ANDROID_BODY_QUOTED_PART will be set to this value
     * @param partText The text to add.  If null, nothing happens
     */
    private static void addTextBodyPart(MimeMultipart mp, String contentType, String quotedPartTag, String partText) throws MessagingException {
        if (partText == null) {
            return;
        }
        TextBody body = new TextBody(partText);
        MimeBodyPart bp = new MimeBodyPart(body, contentType);
        if (quotedPartTag != null) {
            bp.addHeader(MimeHeader.HEADER_ANDROID_BODY_QUOTED_PART, quotedPartTag);
        }
        mp.addBodyPart(bp);
    }

    static Account makeLegacyAccount(Context context, EmailContent.Account fromAccount) {
        Account result = new Account(context);
        result.setDescription(fromAccount.getDisplayName());
        result.setEmail(fromAccount.getEmailAddress());
        result.setSyncWindow(fromAccount.getSyncLookback());
        result.setAutomaticCheckIntervalMinutes(fromAccount.getSyncInterval());
        result.setNotifyNewMail(0 != (fromAccount.getFlags() & EmailContent.Account.FLAGS_NOTIFY_NEW_MAIL));
        result.setVibrate(0 != (fromAccount.getFlags() & EmailContent.Account.FLAGS_VIBRATE));
        result.setDeletePolicy(fromAccount.getDeletePolicy());
        result.mUuid = fromAccount.getUuid();
        result.setName(fromAccount.mSenderName);
        result.setRingtone(fromAccount.mRingtoneUri);
        result.mProtocolVersion = fromAccount.mProtocolVersion;
        result.setStoreUri(fromAccount.getStoreUri(context));
        result.setSenderUri(fromAccount.getSenderUri(context));
        return result;
    }

    static EmailContent.Account makeAccount(Context context, Account fromAccount) {
        EmailContent.Account result = new EmailContent.Account();
        result.setDisplayName(fromAccount.getDescription());
        result.setEmailAddress(fromAccount.getEmail());
        result.mSyncKey = null;
        result.setSyncLookback(fromAccount.getSyncWindow());
        result.setSyncInterval(fromAccount.getAutomaticCheckIntervalMinutes());
        int flags = 0;
        if (fromAccount.isNotifyNewMail()) flags |= EmailContent.Account.FLAGS_NOTIFY_NEW_MAIL;
        if (fromAccount.isVibrate()) flags |= EmailContent.Account.FLAGS_VIBRATE;
        result.setFlags(flags);
        result.setDeletePolicy(fromAccount.getDeletePolicy());
        result.mCompatibilityUuid = fromAccount.getUuid();
        result.setSenderName(fromAccount.getName());
        result.setRingtone(fromAccount.getRingtone());
        result.mProtocolVersion = fromAccount.mProtocolVersion;
        result.mNewMessageCount = 0;
        result.setStoreUri(context, fromAccount.getStoreUri());
        result.setSenderUri(context, fromAccount.getSenderUri());
        return result;
    }
}

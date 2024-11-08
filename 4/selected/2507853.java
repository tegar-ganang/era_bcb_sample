package com.android.email.mail.transport;

import com.android.email.codec.binary.Base64;
import com.android.email.codec.binary.Base64OutputStream;
import com.android.email.mail.Address;
import com.android.email.mail.MessagingException;
import com.android.email.mail.internet.MimeUtility;
import com.android.email.provider.EmailContent.Attachment;
import com.android.email.provider.EmailContent.Body;
import com.android.email.provider.EmailContent.Message;
import org.apache.commons.io.IOUtils;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to output RFC 822 messages from provider email messages
 */
public class Rfc822Output {

    private static final Pattern PATTERN_START_OF_LINE = Pattern.compile("(?m)^");

    private static final Pattern PATTERN_ENDLINE_CRLF = Pattern.compile("\r\n");

    static final SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);

    static String buildBodyText(Context context, Message message, boolean appendQuotedText) {
        Body body = Body.restoreBodyWithMessageId(context, message.mId);
        if (body == null) {
            return null;
        }
        String text = body.mTextContent;
        int flags = message.mFlags;
        boolean isReply = (flags & Message.FLAG_TYPE_REPLY) != 0;
        boolean isForward = (flags & Message.FLAG_TYPE_FORWARD) != 0;
        String intro = body.mIntroText == null ? "" : body.mIntroText;
        if (!appendQuotedText) {
            if (isReply) {
                text += intro;
            } else if (isForward) {
                text += "\r\n";
            }
            return text;
        }
        String quotedText = body.mTextReply;
        if (quotedText != null) {
            Matcher matcher = PATTERN_ENDLINE_CRLF.matcher(quotedText);
            quotedText = matcher.replaceAll("\n");
        }
        if (isReply) {
            text += intro;
            if (quotedText != null) {
                Matcher matcher = PATTERN_START_OF_LINE.matcher(quotedText);
                text += matcher.replaceAll(">");
            }
        } else if (isForward) {
            text += intro;
            if (quotedText != null) {
                text += quotedText;
            }
        }
        return text;
    }

    /**
     * Write the entire message to an output stream.  This method provides buffering, so it is
     * not necessary to pass in a buffered output stream here.
     *
     * @param context system context for accessing the provider
     * @param messageId the message to write out
     * @param out the output stream to write the message to
     * @param appendQuotedText whether or not to append quoted text if this is a reply/forward
     *
     * TODO alternative parts (e.g. text+html) are not supported here.
     */
    public static void writeTo(Context context, long messageId, OutputStream out, boolean appendQuotedText, boolean sendBcc) throws IOException, MessagingException {
        Message message = Message.restoreMessageWithId(context, messageId);
        if (message == null) {
            return;
        }
        OutputStream stream = new BufferedOutputStream(out, 1024);
        Writer writer = new OutputStreamWriter(stream);
        String date = mDateFormat.format(new Date(message.mTimeStamp));
        writeHeader(writer, "Date", date);
        writeEncodedHeader(writer, "Subject", message.mSubject);
        writeHeader(writer, "Message-ID", message.mMessageId);
        writeAddressHeader(writer, "From", message.mFrom);
        writeAddressHeader(writer, "To", message.mTo);
        writeAddressHeader(writer, "Cc", message.mCc);
        if (sendBcc) {
            writeAddressHeader(writer, "Bcc", message.mBcc);
        }
        writeAddressHeader(writer, "Reply-To", message.mReplyTo);
        String text = buildBodyText(context, message, appendQuotedText);
        Uri uri = ContentUris.withAppendedId(Attachment.MESSAGE_ID_URI, messageId);
        Cursor attachmentsCursor = context.getContentResolver().query(uri, Attachment.CONTENT_PROJECTION, null, null, null);
        try {
            boolean mixedParts = attachmentsCursor.getCount() > 0;
            String mixedBoundary = null;
            if (!mixedParts) {
                if (text != null) {
                    writeTextWithHeaders(writer, stream, text);
                } else {
                    writer.write("\r\n");
                }
            } else {
                writeHeader(writer, "MIME-Version", "1.0");
                mixedBoundary = "--_com.android.email_" + System.nanoTime();
                writeHeader(writer, "Content-Type", "multipart/mixed; boundary=\"" + mixedBoundary + "\"");
                writer.write("\r\n");
                if (text != null) {
                    writeBoundary(writer, mixedBoundary, false);
                    writeTextWithHeaders(writer, stream, text);
                }
                while (attachmentsCursor.moveToNext()) {
                    writeBoundary(writer, mixedBoundary, false);
                    Attachment attachment = Attachment.getContent(attachmentsCursor, Attachment.class);
                    writeOneAttachment(context, writer, stream, attachment);
                    writer.write("\r\n");
                }
                writeBoundary(writer, mixedBoundary, true);
            }
        } finally {
            attachmentsCursor.close();
        }
        writer.flush();
        out.flush();
    }

    /**
     * Write a single attachment and its payload
     */
    private static void writeOneAttachment(Context context, Writer writer, OutputStream out, Attachment attachment) throws IOException, MessagingException {
        writeHeader(writer, "Content-Type", attachment.mMimeType + ";\n name=\"" + attachment.mFileName + "\"");
        writeHeader(writer, "Content-Transfer-Encoding", "base64");
        writeHeader(writer, "Content-Disposition", "attachment;" + "\n filename=\"" + attachment.mFileName + "\";" + "\n size=" + Long.toString(attachment.mSize));
        writeHeader(writer, "Content-ID", attachment.mContentId);
        writer.append("\r\n");
        InputStream inStream = null;
        try {
            Uri fileUri = Uri.parse(attachment.mContentUri);
            inStream = context.getContentResolver().openInputStream(fileUri);
            writer.flush();
            Base64OutputStream base64Out = new Base64OutputStream(out);
            IOUtils.copy(inStream, base64Out);
            base64Out.close();
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioe) {
            throw new MessagingException("Invalid attachment.", ioe);
        }
    }

    /**
     * Write a single header with no wrapping or encoding
     *
     * @param writer the output writer
     * @param name the header name
     * @param value the header value
     */
    private static void writeHeader(Writer writer, String name, String value) throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(value);
            writer.append("\r\n");
        }
    }

    /**
     * Write a single header using appropriate folding & encoding
     *
     * @param writer the output writer
     * @param name the header name
     * @param value the header value
     */
    private static void writeEncodedHeader(Writer writer, String name, String value) throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(MimeUtility.foldAndEncode2(value, name.length() + 2));
            writer.append("\r\n");
        }
    }

    /**
     * Unpack, encode, and fold address(es) into a header
     *
     * @param writer the output writer
     * @param name the header name
     * @param value the header value (a packed list of addresses)
     */
    private static void writeAddressHeader(Writer writer, String name, String value) throws IOException {
        if (value != null && value.length() > 0) {
            writer.append(name);
            writer.append(": ");
            writer.append(MimeUtility.fold(Address.packedToHeader(value), name.length() + 2));
            writer.append("\r\n");
        }
    }

    /**
     * Write a multipart boundary
     *
     * @param writer the output writer
     * @param boundary the boundary string
     * @param end false if inner boundary, true if final boundary
     */
    private static void writeBoundary(Writer writer, String boundary, boolean end) throws IOException {
        writer.append("--");
        writer.append(boundary);
        if (end) {
            writer.append("--");
        }
        writer.append("\r\n");
    }

    /**
     * Write text (either as main body or inside a multipart), preceded by appropriate headers.
     *
     * Note this always uses base64, even when not required.  Slightly less efficient for
     * US-ASCII text, but handles all formats even when non-ascii chars are involved.  A small
     * optimization might be to prescan the string for safety and send raw if possible.
     *
     * @param writer the output writer
     * @param out the output stream inside the writer (used for byte[] access)
     * @param text The original text of the message
     */
    private static void writeTextWithHeaders(Writer writer, OutputStream out, String text) throws IOException {
        writeHeader(writer, "Content-Type", "text/plain; charset=utf-8");
        writeHeader(writer, "Content-Transfer-Encoding", "base64");
        writer.write("\r\n");
        byte[] bytes = text.getBytes("UTF-8");
        writer.flush();
        out.write(Base64.encodeBase64Chunked(bytes));
    }
}

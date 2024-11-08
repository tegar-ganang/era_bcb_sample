package org.apache.james.core;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Date;
import java.util.Enumeration;
import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.NewsAddress;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.RFC822DateFormat;

/**
 * This object wraps a MimeMessage, only loading the underlying MimeMessage
 * object when needed.  Also tracks if changes were made to reduce
 * unnecessary saves.
 */
public class MimeMessageWrapper extends MimeMessage {

    /**
     * Can provide an input stream to the data
     */
    MimeMessageSource source = null;

    /**
     * The Internet headers in memory
     */
    MailHeaders headers = null;

    /**
     * The mime message in memory
     */
    MimeMessage message = null;

    /**
     * Record whether a change was made to this message
     */
    boolean modified = false;

    /**
     * How to format a mail date
     */
    RFC822DateFormat mailDateFormat = new RFC822DateFormat();

    /**
     * A constructor that instantiates a MimeMessageWrapper based on 
     * a MimeMessageSource
     *
     * @param source the MimeMessageSource
     */
    public MimeMessageWrapper(MimeMessageSource source) {
        super(Session.getDefaultInstance(System.getProperties(), null));
        this.source = source;
    }

    /**
     * Returns the source ID of the MimeMessageSource that is supplying this
     * with data.
     * @see MimeMessageSource
     */
    public String getSourceId() {
        return source.getSourceId();
    }

    /**
     * Load the message headers from the internal source.
     *
     * @throws MessagingException if an error is encountered while 
     *                            loading the headers
     */
    private synchronized void loadHeaders() throws MessagingException {
        if (headers != null) {
            return;
        }
        try {
            InputStream in = source.getInputStream();
            try {
                headers = new MailHeaders(in);
            } finally {
                in.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new MessagingException("Unable to parse headers from stream: " + ioe.getMessage(), ioe);
        }
    }

    /**
     * Load the complete MimeMessage from the internal source.
     *
     * @throws MessagingException if an error is encountered while 
     *                            loading the message
     */
    private synchronized void loadMessage() throws MessagingException {
        if (message != null) {
            return;
        }
        InputStream in = null;
        try {
            in = source.getInputStream();
            headers = new MailHeaders(in);
            ByteArrayInputStream headersIn = new ByteArrayInputStream(headers.toByteArray());
            in = new SequenceInputStream(headersIn, in);
            message = new MimeMessage(session, in);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new MessagingException("Unable to parse stream: " + ioe.getMessage(), ioe);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Internal implementation to get InternetAddress headers
     */
    private Address[] getAddressHeader(String name) throws MessagingException {
        String addr = getHeader(name, ",");
        if (addr == null) {
            return null;
        } else {
            return InternetAddress.parse(addr);
        }
    }

    /**
     * Internal implementation to find headers
     */
    private String getHeaderName(Message.RecipientType recipienttype) throws MessagingException {
        String s;
        if (recipienttype == Message.RecipientType.TO) {
            s = RFC2822Headers.TO;
        } else if (recipienttype == Message.RecipientType.CC) {
            s = RFC2822Headers.CC;
        } else if (recipienttype == Message.RecipientType.BCC) {
            s = RFC2822Headers.BCC;
        } else if (recipienttype == RecipientType.NEWSGROUPS) {
            s = "Newsgroups";
        } else {
            throw new MessagingException("Invalid Recipient Type");
        }
        return s;
    }

    /**
     * Get whether the message has been modified.
     *
     * @return whether the message has been modified
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        if (message == null || !isModified()) {
            InputStream in = source.getInputStream();
            try {
                copyStream(in, os);
            } finally {
                in.close();
            }
        } else {
            writeTo(os, os);
        }
    }

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        writeTo(os, os, ignoreList);
    }

    /**
     * Write
     */
    public void writeTo(OutputStream headerOs, OutputStream bodyOs) throws IOException, MessagingException {
        writeTo(headerOs, bodyOs, new String[0]);
    }

    public void writeTo(OutputStream headerOs, OutputStream bodyOs, String[] ignoreList) throws IOException, MessagingException {
        if (message == null || !isModified()) {
            InputStream in = source.getInputStream();
            try {
                InternetHeaders headers = new InternetHeaders(in);
                PrintWriter pos = new InternetPrintWriter(new BufferedWriter(new OutputStreamWriter(headerOs), 512), true);
                for (Enumeration e = headers.getNonMatchingHeaderLines(ignoreList); e.hasMoreElements(); ) {
                    String header = (String) e.nextElement();
                    pos.println(header);
                }
                pos.println();
                pos.flush();
                copyStream(in, bodyOs);
            } finally {
                in.close();
            }
        } else {
            writeTo(message, headerOs, bodyOs, ignoreList);
        }
    }

    /**
     * Convenience method to take any MimeMessage and write the headers and body to two
     * different output streams
     */
    public static void writeTo(MimeMessage message, OutputStream headerOs, OutputStream bodyOs) throws IOException, MessagingException {
        writeTo(message, headerOs, bodyOs, null);
    }

    /**
     * Convenience method to take any MimeMessage and write the headers and body to two
     * different output streams, with an ignore list
     */
    public static void writeTo(MimeMessage message, OutputStream headerOs, OutputStream bodyOs, String[] ignoreList) throws IOException, MessagingException {
        if (message instanceof MimeMessageWrapper) {
            MimeMessageWrapper wrapper = (MimeMessageWrapper) message;
            wrapper.writeTo(headerOs, bodyOs, ignoreList);
        } else {
            if (message.getMessageID() == null) {
                message.saveChanges();
            }
            Enumeration headers = message.getNonMatchingHeaderLines(ignoreList);
            PrintWriter hos = new InternetPrintWriter(new BufferedWriter(new OutputStreamWriter(headerOs), 512), true);
            while (headers.hasMoreElements()) {
                hos.println((String) headers.nextElement());
            }
            hos.println();
            hos.flush();
            InputStream bis = null;
            OutputStream bos = null;
            try {
                bos = MimeUtility.encode(bodyOs, message.getEncoding());
                bis = message.getInputStream();
            } catch (javax.activation.UnsupportedDataTypeException udte) {
                try {
                    bis = message.getRawInputStream();
                    bos = bodyOs;
                } catch (javax.mail.MessagingException _) {
                    throw udte;
                }
            } catch (javax.mail.MessagingException me) {
                try {
                    bis = message.getRawInputStream();
                    bos = bodyOs;
                } catch (javax.mail.MessagingException _) {
                    throw me;
                }
            }
            try {
                copyStream(bis, bos);
            } finally {
                bis.close();
            }
        }
    }

    /**
     * Various reader methods
     */
    public Address[] getFrom() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        Address from[] = getAddressHeader(RFC2822Headers.FROM);
        if (from == null) {
            from = getAddressHeader(RFC2822Headers.SENDER);
        }
        return from;
    }

    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        if (type == RecipientType.NEWSGROUPS) {
            String s = headers.getHeader("Newsgroups", ",");
            if (s == null) {
                return null;
            } else {
                return NewsAddress.parse(s);
            }
        } else {
            return getAddressHeader(getHeaderName(type));
        }
    }

    public Address[] getAllRecipients() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        Address toAddresses[] = getRecipients(RecipientType.TO);
        Address ccAddresses[] = getRecipients(RecipientType.CC);
        Address bccAddresses[] = getRecipients(RecipientType.BCC);
        Address newsAddresses[] = getRecipients(RecipientType.NEWSGROUPS);
        if (ccAddresses == null && bccAddresses == null && newsAddresses == null) {
            return toAddresses;
        }
        int i = (toAddresses == null ? 0 : toAddresses.length) + (ccAddresses == null ? 0 : ccAddresses.length) + (bccAddresses == null ? 0 : bccAddresses.length) + (newsAddresses == null ? 0 : newsAddresses.length);
        Address allAddresses[] = new Address[i];
        int j = 0;
        if (toAddresses != null) {
            System.arraycopy(toAddresses, 0, allAddresses, j, toAddresses.length);
            j += toAddresses.length;
        }
        if (ccAddresses != null) {
            System.arraycopy(ccAddresses, 0, allAddresses, j, ccAddresses.length);
            j += ccAddresses.length;
        }
        if (bccAddresses != null) {
            System.arraycopy(bccAddresses, 0, allAddresses, j, bccAddresses.length);
            j += bccAddresses.length;
        }
        if (newsAddresses != null) {
            System.arraycopy(newsAddresses, 0, allAddresses, j, newsAddresses.length);
            j += newsAddresses.length;
        }
        return allAddresses;
    }

    public Address[] getReplyTo() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        Address replyTo[] = getAddressHeader(RFC2822Headers.REPLY_TO);
        if (replyTo == null) {
            replyTo = getFrom();
        }
        return replyTo;
    }

    public String getSubject() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        String subject = getHeader(RFC2822Headers.SUBJECT, null);
        if (subject == null) {
            return null;
        }
        try {
            return MimeUtility.decodeText(subject);
        } catch (UnsupportedEncodingException _ex) {
            return subject;
        }
    }

    public Date getSentDate() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        String header = getHeader(RFC2822Headers.DATE, null);
        if (header != null) {
            try {
                return mailDateFormat.parse(header);
            } catch (ParseException _ex) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * We do not attempt to define the received date, although in theory this is the last
     * most date in the Received: headers.  For now we return null, which means we are
     * not implementing it.
     */
    public Date getReceivedDate() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return null;
    }

    /**
     * This is the MimeMessage implementation - this should return ONLY the
     * body, not the entire message (should not count headers).  Will have
     * to parse the message.
     */
    public int getSize() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getSize();
    }

    /**
     * Corrects JavaMail 1.1 version which always returns -1.
     * Only corrected for content less than 5000 bytes,
     * to avoid memory hogging.
     */
    public int getLineCount() throws MessagingException {
        InputStream in = null;
        try {
            in = getContentStream();
        } catch (Exception e) {
            return -1;
        }
        if (in == null) {
            return -1;
        }
        try {
            LineNumberReader counter = new LineNumberReader(new InputStreamReader(in, getEncoding()));
            char[] block = new char[4096];
            while (counter.read(block) > -1) {
            }
            return counter.getLineNumber();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return -1;
        } finally {
            try {
                in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Returns size of message, ie headers and content. Current implementation
     * actually returns number of characters in headers plus number of bytes
     * in the internal content byte array.
     */
    public long getMessageSize() throws MessagingException {
        try {
            return source.getMessageSize();
        } catch (IOException ioe) {
            throw new MessagingException("Error retrieving message size", ioe);
        }
    }

    public String getContentType() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        String value = getHeader(RFC2822Headers.CONTENT_TYPE, null);
        if (value == null) {
            return "text/plain";
        } else {
            return value;
        }
    }

    public boolean isMimeType(String mimeType) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.isMimeType(mimeType);
    }

    public String getDisposition() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getDisposition();
    }

    public String getEncoding() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getEncoding();
    }

    public String getContentID() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return getHeader("Content-Id", null);
    }

    public String getContentMD5() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return getHeader("Content-MD5", null);
    }

    public String getDescription() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getDescription();
    }

    public String[] getContentLanguage() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getContentLanguage();
    }

    public String getMessageID() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return getHeader(RFC2822Headers.MESSAGE_ID, null);
    }

    public String getFileName() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getFileName();
    }

    public InputStream getInputStream() throws IOException, MessagingException {
        if (message == null) {
            loadMessage();
            return message.getInputStream();
        } else {
            return message.getInputStream();
        }
    }

    public DataHandler getDataHandler() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getDataHandler();
    }

    public Object getContent() throws IOException, MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getContent();
    }

    public String[] getHeader(String name) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getHeader(name);
    }

    public String getHeader(String name, String delimiter) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getHeader(name, delimiter);
    }

    public Enumeration getAllHeaders() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getAllHeaders();
    }

    public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getMatchingHeaders(names);
    }

    public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getNonMatchingHeaders(names);
    }

    public Enumeration getAllHeaderLines() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getAllHeaderLines();
    }

    public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getMatchingHeaderLines(names);
    }

    public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getNonMatchingHeaderLines(names);
    }

    public Flags getFlags() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getFlags();
    }

    public boolean isSet(Flags.Flag flag) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.isSet(flag);
    }

    /**
     * Writes content only, ie not headers, to the specified OutputStream.
     *
     * @param outs the OutputStream to which the content is written
     */
    public void writeContentTo(OutputStream outs) throws java.io.IOException, MessagingException {
        if (message == null) {
            loadMessage();
        }
        InputStream in = getContentStream();
        try {
            copyStream(in, outs);
        } finally {
            in.close();
        }
    }

    /**
     * Convenience method to copy streams
     */
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] block = new byte[1024];
        int read = 0;
        while ((read = in.read(block)) > -1) {
            out.write(block, 0, read);
        }
        out.flush();
    }

    public void setFrom(Address address) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setFrom(address);
    }

    public void setFrom() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setFrom();
    }

    public void addFrom(Address[] addresses) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.addFrom(addresses);
    }

    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setRecipients(type, addresses);
    }

    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.addRecipients(type, addresses);
    }

    public void setReplyTo(Address[] addresses) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setReplyTo(addresses);
    }

    public void setSubject(String subject) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        headers.setHeader(RFC2822Headers.SUBJECT, subject);
        message.setSubject(subject);
    }

    public void setSubject(String subject, String charset) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        try {
            headers.setHeader(RFC2822Headers.SUBJECT, new String(subject.getBytes(charset)));
        } catch (java.io.UnsupportedEncodingException _) {
        }
        message.setSubject(subject, charset);
    }

    public void setSentDate(Date d) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        headers.setHeader(RFC2822Headers.DATE, mailDateFormat.format(d));
        message.setSentDate(d);
    }

    public void setDisposition(String disposition) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setDisposition(disposition);
    }

    public void setContentID(String cid) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setContentID(cid);
    }

    public void setContentMD5(String md5) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setContentMD5(md5);
    }

    public void setDescription(String description) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setDescription(description);
    }

    public void setDescription(String description, String charset) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setDescription(description, charset);
    }

    public void setContentLanguage(String[] languages) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setContentLanguage(languages);
    }

    public void setFileName(String filename) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setFileName(filename);
    }

    public void setDataHandler(DataHandler dh) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setDataHandler(dh);
    }

    public void setContent(Object o, String type) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setContent(o, type);
    }

    public void setText(String text) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setText(text);
    }

    public void setText(String text, String charset) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setText(text, charset);
    }

    public void setContent(Multipart mp) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setContent(mp);
    }

    public Message reply(boolean replyToAll) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        return message.reply(replyToAll);
    }

    public void setHeader(String name, String value) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        headers.setHeader(name, value);
        message.setHeader(name, value);
    }

    public void addHeader(String name, String value) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        headers.addHeader(name, value);
        message.addHeader(name, value);
    }

    public void removeHeader(String name) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        headers.removeHeader(name);
        message.removeHeader(name);
    }

    public void addHeaderLine(String line) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        headers.addHeaderLine(line);
        message.addHeaderLine(line);
    }

    public void setFlags(Flags flag, boolean set) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setFlags(flag, set);
    }

    public void saveChanges() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.saveChanges();
    }

    public InputStream getRawInputStream() throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        return message.getRawInputStream();
    }

    public void addRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.addRecipients(type, addresses);
    }

    public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {
        if (message == null) {
            loadMessage();
        }
        modified = true;
        message.setRecipients(type, addresses);
    }
}

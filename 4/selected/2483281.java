package com.sun.mail.imap;

import java.util.Date;
import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Locale;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import com.sun.mail.util.*;
import com.sun.mail.iap.*;
import com.sun.mail.imap.protocol.*;

public class IMAPMessage extends MimeMessage {

    protected BODYSTRUCTURE bs;

    protected ENVELOPE envelope;

    private Date receivedDate;

    private int size = -1;

    private boolean peek;

    private int seqnum;

    private long uid = -1;

    protected String sectionId;

    private String type;

    private String subject;

    private String description;

    private boolean headersLoaded = false;

    private Hashtable loadedHeaders;

    private static String EnvelopeCmd = "ENVELOPE INTERNALDATE RFC822.SIZE";

    /**
     * Constructor.
     */
    protected IMAPMessage(IMAPFolder folder, int msgnum, int seqnum) {
        super(folder, msgnum);
        this.seqnum = seqnum;
        flags = null;
    }

    /**
     * Constructor, for use by IMAPNestedMessage.
     */
    protected IMAPMessage(Session session) {
        super(session);
    }

    /**
     * Get this message's folder's protocol connection.
     * Throws FolderClosedException, if the protocol connection
     * is not available.
     *
     * ASSERT: Must hold the messageCacheLock.
     */
    protected IMAPProtocol getProtocol() throws ProtocolException, FolderClosedException {
        ((IMAPFolder) folder).waitIfIdle();
        IMAPProtocol p = ((IMAPFolder) folder).protocol;
        if (p == null) throw new FolderClosedException(folder); else return p;
    }

    protected boolean isREV1() throws FolderClosedException {
        IMAPProtocol p = ((IMAPFolder) folder).protocol;
        if (p == null) throw new FolderClosedException(folder); else return p.isREV1();
    }

    /**
     * Get the messageCacheLock, associated with this Message's
     * Folder.
     */
    protected Object getMessageCacheLock() {
        return ((IMAPFolder) folder).messageCacheLock;
    }

    /**
     * Get this message's IMAP sequence number.
     *
     * ASSERT: This method must be called only when holding the
     * 	messageCacheLock.
     */
    protected int getSequenceNumber() {
        return seqnum;
    }

    /**
     * Set this message's IMAP sequence number.
     *
     * ASSERT: This method must be called only when holding the
     * 	messageCacheLock.
     */
    protected void setSequenceNumber(int seqnum) {
        this.seqnum = seqnum;
    }

    /**
     * Wrapper around the protected method Message.setMessageNumber() to 
     * make that method accessible to IMAPFolder.
     */
    protected void setMessageNumber(int msgnum) {
        super.setMessageNumber(msgnum);
    }

    protected long getUID() {
        return uid;
    }

    protected void setUID(long uid) {
        this.uid = uid;
    }

    protected void setExpunged(boolean set) {
        super.setExpunged(set);
        seqnum = -1;
    }

    protected void checkExpunged() throws MessageRemovedException {
        if (expunged) throw new MessageRemovedException();
    }

    /**
     * Do a NOOP to force any untagged EXPUNGE responses
     * and then check if this message is expunged.
     */
    protected void forceCheckExpunged() throws MessageRemovedException, FolderClosedException {
        synchronized (getMessageCacheLock()) {
            try {
                getProtocol().noop();
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
            }
        }
        if (expunged) throw new MessageRemovedException();
    }

    protected int getFetchBlockSize() {
        return ((IMAPStore) folder.getStore()).getFetchBlockSize();
    }

    /**
     * Get the "From" attribute.
     */
    public Address[] getFrom() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        return aaclone(envelope.from);
    }

    public void setFrom(Address address) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void addFrom(Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the "Sender" attribute.
     */
    public Address getSender() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        if (envelope.sender != null) return (envelope.sender)[0]; else return null;
    }

    public void setSender(Address address) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the desired Recipient type.
     */
    public Address[] getRecipients(Message.RecipientType type) throws MessagingException {
        checkExpunged();
        loadEnvelope();
        if (type == Message.RecipientType.TO) return aaclone(envelope.to); else if (type == Message.RecipientType.CC) return aaclone(envelope.cc); else if (type == Message.RecipientType.BCC) return aaclone(envelope.bcc); else return super.getRecipients(type);
    }

    public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void addRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the ReplyTo addresses.
     */
    public Address[] getReplyTo() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        return aaclone(envelope.replyTo);
    }

    public void setReplyTo(Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the decoded subject.
     */
    public String getSubject() throws MessagingException {
        checkExpunged();
        if (subject != null) return subject;
        loadEnvelope();
        if (envelope.subject == null) return null;
        try {
            subject = MimeUtility.decodeText(envelope.subject);
        } catch (UnsupportedEncodingException ex) {
            subject = envelope.subject;
        }
        return subject;
    }

    public void setSubject(String subject, String charset) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the SentDate.
     */
    public Date getSentDate() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        if (envelope.date == null) return null; else return new Date(envelope.date.getTime());
    }

    public void setSentDate(Date d) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the recieved date (INTERNALDATE)
     */
    public Date getReceivedDate() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        if (receivedDate == null) return null; else return new Date(receivedDate.getTime());
    }

    /**
     * Get the message size. <p>
     *
     * Note that this returns RFC822.SIZE.  That is, it's the
     * size of the whole message, header and body included.
     */
    public int getSize() throws MessagingException {
        checkExpunged();
        if (size == -1) loadEnvelope();
        return size;
    }

    /**
     * Get the total number of lines. <p>
     *
     * Returns the "body_fld_lines" field from the
     * BODYSTRUCTURE. Note that this field is available
     * only for text/plain and message/rfc822 types
     */
    public int getLineCount() throws MessagingException {
        checkExpunged();
        loadBODYSTRUCTURE();
        return bs.lines;
    }

    /** 
     * Get the content language.
     */
    public String[] getContentLanguage() throws MessagingException {
        checkExpunged();
        loadBODYSTRUCTURE();
        if (bs.language != null) return (String[]) (bs.language).clone(); else return null;
    }

    public void setContentLanguage(String[] languages) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the In-Reply-To header.
     *
     * @since	JavaMail 1.3.3
     */
    public String getInReplyTo() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        return envelope.inReplyTo;
    }

    /**
     * Get the Content-Type.
     *
     * Generate this header from the BODYSTRUCTURE. Append parameters
     * as well.
     */
    public String getContentType() throws MessagingException {
        checkExpunged();
        if (type == null) {
            loadBODYSTRUCTURE();
            ContentType ct = new ContentType(bs.type, bs.subtype, bs.cParams);
            type = ct.toString();
        }
        return type;
    }

    /**
     * Get the Content-Disposition.
     */
    public String getDisposition() throws MessagingException {
        checkExpunged();
        loadBODYSTRUCTURE();
        return bs.disposition;
    }

    public void setDisposition(String disposition) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the Content-Transfer-Encoding.
     */
    public String getEncoding() throws MessagingException {
        checkExpunged();
        loadBODYSTRUCTURE();
        return bs.encoding;
    }

    /**
     * Get the Content-ID.
     */
    public String getContentID() throws MessagingException {
        checkExpunged();
        loadBODYSTRUCTURE();
        return bs.id;
    }

    public void setContentID(String cid) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the Content-MD5.
     */
    public String getContentMD5() throws MessagingException {
        checkExpunged();
        loadBODYSTRUCTURE();
        return bs.md5;
    }

    public void setContentMD5(String md5) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the decoded Content-Description.
     */
    public String getDescription() throws MessagingException {
        checkExpunged();
        if (description != null) return description;
        loadBODYSTRUCTURE();
        if (bs.description == null) return null;
        try {
            description = MimeUtility.decodeText(bs.description);
        } catch (UnsupportedEncodingException ex) {
            description = bs.description;
        }
        return description;
    }

    public void setDescription(String description, String charset) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get the Message-ID.
     */
    public String getMessageID() throws MessagingException {
        checkExpunged();
        loadEnvelope();
        return envelope.messageId;
    }

    /**
     * Get the "filename" Disposition parameter. (Only available in
     * IMAP4rev1). If thats not available, get the "name" ContentType
     * parameter.
     */
    public String getFileName() throws MessagingException {
        checkExpunged();
        String filename = null;
        loadBODYSTRUCTURE();
        if (bs.dParams != null) filename = bs.dParams.get("filename");
        if (filename == null && bs.cParams != null) filename = bs.cParams.get("name");
        return filename;
    }

    public void setFileName(String filename) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get all the bytes for this message. Overrides getContentStream()
     * in MimeMessage. This method is ultimately used by the DataHandler
     * to obtain the input stream for this message.
     *
     * @see javax.mail.internet.MimeMessage#getContentStream
     */
    protected InputStream getContentStream() throws MessagingException {
        InputStream is = null;
        boolean pk = getPeek();
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                if (p.isREV1() && (getFetchBlockSize() != -1)) return new IMAPInputStream(this, toSection("TEXT"), bs != null ? bs.size : -1, pk);
                if (p.isREV1()) {
                    BODY b;
                    if (pk) b = p.peekBody(getSequenceNumber(), toSection("TEXT")); else b = p.fetchBody(getSequenceNumber(), toSection("TEXT"));
                    if (b != null) is = b.getByteArrayInputStream();
                } else {
                    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), "TEXT");
                    if (rd != null) is = rd.getByteArrayInputStream();
                }
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
        if (is == null) throw new MessagingException("No content"); else return is;
    }

    /**
     * Get the DataHandler object for this message.
     */
    public synchronized DataHandler getDataHandler() throws MessagingException {
        checkExpunged();
        if (dh == null) {
            loadBODYSTRUCTURE();
            if (type == null) {
                ContentType ct = new ContentType(bs.type, bs.subtype, bs.cParams);
                type = ct.toString();
            }
            if (bs.isMulti()) dh = new DataHandler(new IMAPMultipartDataSource(this, bs.bodies, sectionId, this)); else if (bs.isNested() && isREV1()) dh = new DataHandler(new IMAPNestedMessage(this, bs.bodies[0], bs.envelope, sectionId == null ? "1" : sectionId + ".1"), type);
        }
        return super.getDataHandler();
    }

    public void setDataHandler(DataHandler content) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Write out the bytes into the given outputstream.
     */
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        InputStream is = null;
        boolean pk = getPeek();
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                if (p.isREV1()) {
                    BODY b;
                    if (pk) b = p.peekBody(getSequenceNumber(), sectionId); else b = p.fetchBody(getSequenceNumber(), sectionId);
                    if (b != null) is = b.getByteArrayInputStream();
                } else {
                    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), null);
                    if (rd != null) is = rd.getByteArrayInputStream();
                }
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
        if (is == null) throw new MessagingException("No content");
        byte[] bytes = new byte[1024];
        int count;
        while ((count = is.read(bytes)) != -1) os.write(bytes, 0, count);
    }

    /**
     * Get the named header.
     */
    public String[] getHeader(String name) throws MessagingException {
        checkExpunged();
        if (isHeaderLoaded(name)) return headers.getHeader(name);
        InputStream is = null;
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                if (p.isREV1()) {
                    BODY b = p.peekBody(getSequenceNumber(), toSection("HEADER.FIELDS (" + name + ")"));
                    if (b != null) is = b.getByteArrayInputStream();
                } else {
                    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), "HEADER.LINES (" + name + ")");
                    if (rd != null) is = rd.getByteArrayInputStream();
                }
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
        if (is == null) return null;
        if (headers == null) headers = new InternetHeaders();
        headers.load(is);
        setHeaderLoaded(name);
        return headers.getHeader(name);
    }

    /**
     * Get the named header.
     */
    public String getHeader(String name, String delimiter) throws MessagingException {
        checkExpunged();
        if (getHeader(name) == null) return null;
        return headers.getHeader(name, delimiter);
    }

    public void setHeader(String name, String value) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void addHeader(String name, String value) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    public void removeHeader(String name) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get all headers.
     */
    public Enumeration getAllHeaders() throws MessagingException {
        checkExpunged();
        loadHeaders();
        return super.getAllHeaders();
    }

    /**
     * Get matching headers.
     */
    public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
        checkExpunged();
        loadHeaders();
        return super.getMatchingHeaders(names);
    }

    /**
     * Get non-matching headers.
     */
    public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
        checkExpunged();
        loadHeaders();
        return super.getNonMatchingHeaders(names);
    }

    public void addHeaderLine(String line) throws MessagingException {
        throw new IllegalWriteException("IMAPMessage is read-only");
    }

    /**
     * Get all header-lines.
     */
    public Enumeration getAllHeaderLines() throws MessagingException {
        checkExpunged();
        loadHeaders();
        return super.getAllHeaderLines();
    }

    /**
     * Get all matching header-lines.
     */
    public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
        checkExpunged();
        loadHeaders();
        return super.getMatchingHeaderLines(names);
    }

    /**
     * Get all non-matching headerlines.
     */
    public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
        checkExpunged();
        loadHeaders();
        return super.getNonMatchingHeaderLines(names);
    }

    /**
     * Get the Flags for this message.
     */
    public synchronized Flags getFlags() throws MessagingException {
        checkExpunged();
        loadFlags();
        return super.getFlags();
    }

    /**
     * Test if the given Flags are set in this message.
     */
    public synchronized boolean isSet(Flags.Flag flag) throws MessagingException {
        checkExpunged();
        loadFlags();
        return super.isSet(flag);
    }

    /**
     * Set/Unset the given flags in this message.
     */
    public synchronized void setFlags(Flags flag, boolean set) throws MessagingException {
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                p.storeFlags(getSequenceNumber(), flag, set);
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
    }

    /**
     * Set whether or not to use the PEEK variant of FETCH when
     * fetching message content.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized void setPeek(boolean peek) {
        this.peek = peek;
    }

    /**
     * Get whether or not to use the PEEK variant of FETCH when
     * fetching message content.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized boolean getPeek() {
        return peek;
    }

    /**
     * Invalidate cached header and envelope information for this
     * message.  Subsequent accesses of this information will
     * cause it to be fetched from the server.
     *
     * @since	JavaMail 1.3.3
     */
    public synchronized void invalidateHeaders() {
        headersLoaded = false;
        loadedHeaders = null;
        envelope = null;
        bs = null;
        receivedDate = null;
        size = -1;
        type = null;
        subject = null;
        description = null;
    }

    /**
     * The prefetch method. Called from IMAPFolder.fetch()
     */
    static void fetch(IMAPFolder folder, Message[] msgs, FetchProfile fp) throws MessagingException {
        class FetchProfileCondition implements Utility.Condition {

            private boolean needEnvelope = false;

            private boolean needFlags = false;

            private boolean needBodyStructure = false;

            private boolean needUID = false;

            private boolean needHeaders = false;

            private boolean needSize = false;

            private String[] hdrs = null;

            public FetchProfileCondition(FetchProfile fp) {
                if (fp.contains(FetchProfile.Item.ENVELOPE)) needEnvelope = true;
                if (fp.contains(FetchProfile.Item.FLAGS)) needFlags = true;
                if (fp.contains(FetchProfile.Item.CONTENT_INFO)) needBodyStructure = true;
                if (fp.contains(UIDFolder.FetchProfileItem.UID)) needUID = true;
                if (fp.contains(IMAPFolder.FetchProfileItem.HEADERS)) needHeaders = true;
                if (fp.contains(IMAPFolder.FetchProfileItem.SIZE)) needSize = true;
                hdrs = fp.getHeaderNames();
            }

            public boolean test(IMAPMessage m) {
                if (needEnvelope && m._getEnvelope() == null) return true;
                if (needFlags && m._getFlags() == null) return true;
                if (needBodyStructure && m._getBodyStructure() == null) return true;
                if (needUID && m.getUID() == -1) return true;
                if (needHeaders && !m.areHeadersLoaded()) return true;
                if (needSize && m.size == -1) return true;
                for (int i = 0; i < hdrs.length; i++) {
                    if (!m.isHeaderLoaded(hdrs[i])) return true;
                }
                return false;
            }
        }
        StringBuffer command = new StringBuffer();
        boolean first = true;
        boolean allHeaders = false;
        if (fp.contains(FetchProfile.Item.ENVELOPE)) {
            command.append(EnvelopeCmd);
            first = false;
        }
        if (fp.contains(FetchProfile.Item.FLAGS)) {
            command.append(first ? "FLAGS" : " FLAGS");
            first = false;
        }
        if (fp.contains(FetchProfile.Item.CONTENT_INFO)) {
            command.append(first ? "BODYSTRUCTURE" : " BODYSTRUCTURE");
            first = false;
        }
        if (fp.contains(UIDFolder.FetchProfileItem.UID)) {
            command.append(first ? "UID" : " UID");
            first = false;
        }
        if (fp.contains(IMAPFolder.FetchProfileItem.HEADERS)) {
            allHeaders = true;
            if (folder.protocol.isREV1()) command.append(first ? "BODY.PEEK[HEADER]" : " BODY.PEEK[HEADER]"); else command.append(first ? "RFC822.HEADER" : " RFC822.HEADER");
            first = false;
        }
        if (fp.contains(IMAPFolder.FetchProfileItem.SIZE)) {
            command.append(first ? "RFC822.SIZE" : " RFC822.SIZE");
            first = false;
        }
        String[] hdrs = null;
        if (!allHeaders) {
            hdrs = fp.getHeaderNames();
            if (hdrs.length > 0) {
                if (!first) command.append(" ");
                command.append(craftHeaderCmd(folder.protocol, hdrs));
            }
        }
        Utility.Condition condition = new FetchProfileCondition(fp);
        synchronized (folder.messageCacheLock) {
            MessageSet[] msgsets = Utility.toMessageSet(msgs, condition);
            if (msgsets == null) return;
            Response[] r = null;
            Vector v = new Vector();
            try {
                r = folder.protocol.fetch(msgsets, command.toString());
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (CommandFailedException cfx) {
            } catch (ProtocolException pex) {
                throw new MessagingException(pex.getMessage(), pex);
            }
            if (r == null) return;
            for (int i = 0; i < r.length; i++) {
                if (r[i] == null) continue;
                if (!(r[i] instanceof FetchResponse)) {
                    v.addElement(r[i]);
                    continue;
                }
                FetchResponse f = (FetchResponse) r[i];
                IMAPMessage msg = folder.getMessageBySeqNumber(f.getNumber());
                int count = f.getItemCount();
                boolean unsolicitedFlags = false;
                for (int j = 0; j < count; j++) {
                    Item item = f.getItem(j);
                    if (item instanceof Flags) {
                        if (!fp.contains(FetchProfile.Item.FLAGS) || msg == null) unsolicitedFlags = true; else msg.flags = (Flags) item;
                    } else if (item instanceof ENVELOPE) msg.envelope = (ENVELOPE) item; else if (item instanceof INTERNALDATE) msg.receivedDate = ((INTERNALDATE) item).getDate(); else if (item instanceof RFC822SIZE) msg.size = ((RFC822SIZE) item).size; else if (item instanceof BODYSTRUCTURE) msg.bs = (BODYSTRUCTURE) item; else if (item instanceof UID) {
                        UID u = (UID) item;
                        msg.uid = u.uid;
                        if (folder.uidTable == null) folder.uidTable = new Hashtable();
                        folder.uidTable.put(new Long(u.uid), msg);
                    } else if (item instanceof RFC822DATA || item instanceof BODY) {
                        InputStream headerStream;
                        if (item instanceof RFC822DATA) headerStream = ((RFC822DATA) item).getByteArrayInputStream(); else headerStream = ((BODY) item).getByteArrayInputStream();
                        InternetHeaders h = new InternetHeaders();
                        h.load(headerStream);
                        if (msg.headers == null || allHeaders) msg.headers = h; else {
                            Enumeration e = h.getAllHeaders();
                            while (e.hasMoreElements()) {
                                Header he = (Header) e.nextElement();
                                if (!msg.isHeaderLoaded(he.getName())) msg.headers.addHeader(he.getName(), he.getValue());
                            }
                        }
                        if (allHeaders) msg.setHeadersLoaded(true); else {
                            for (int k = 0; k < hdrs.length; k++) msg.setHeaderLoaded(hdrs[k]);
                        }
                    }
                }
                if (unsolicitedFlags) v.addElement(f);
            }
            int size = v.size();
            if (size != 0) {
                Response[] responses = new Response[size];
                v.copyInto(responses);
                folder.handleResponses(responses);
            }
        }
    }

    private synchronized void loadEnvelope() throws MessagingException {
        if (envelope != null) return;
        Response[] r = null;
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                int seqnum = getSequenceNumber();
                r = p.fetch(seqnum, EnvelopeCmd);
                for (int i = 0; i < r.length; i++) {
                    if (r[i] == null || !(r[i] instanceof FetchResponse) || ((FetchResponse) r[i]).getNumber() != seqnum) continue;
                    FetchResponse f = (FetchResponse) r[i];
                    int count = f.getItemCount();
                    for (int j = 0; j < count; j++) {
                        Item item = f.getItem(j);
                        if (item instanceof ENVELOPE) envelope = (ENVELOPE) item; else if (item instanceof INTERNALDATE) receivedDate = ((INTERNALDATE) item).getDate(); else if (item instanceof RFC822SIZE) size = ((RFC822SIZE) item).size;
                    }
                }
                p.notifyResponseHandlers(r);
                p.handleResult(r[r.length - 1]);
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
        if (envelope == null) throw new MessagingException("Failed to load IMAP envelope");
    }

    private static String craftHeaderCmd(IMAPProtocol p, String[] hdrs) {
        StringBuffer sb;
        if (p.isREV1()) sb = new StringBuffer("BODY.PEEK[HEADER.FIELDS ("); else sb = new StringBuffer("RFC822.HEADER.LINES (");
        for (int i = 0; i < hdrs.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(hdrs[i]);
        }
        if (p.isREV1()) sb.append(")]"); else sb.append(")");
        return sb.toString();
    }

    private synchronized void loadBODYSTRUCTURE() throws MessagingException {
        if (bs != null) return;
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                bs = p.fetchBodyStructure(getSequenceNumber());
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
            if (bs == null) {
                forceCheckExpunged();
                throw new MessagingException("Unable to load BODYSTRUCTURE");
            }
        }
    }

    private synchronized void loadHeaders() throws MessagingException {
        if (headersLoaded) return;
        InputStream is = null;
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                if (p.isREV1()) {
                    BODY b = p.peekBody(getSequenceNumber(), toSection("HEADER"));
                    if (b != null) is = b.getByteArrayInputStream();
                } else {
                    RFC822DATA rd = p.fetchRFC822(getSequenceNumber(), "HEADER");
                    if (rd != null) is = rd.getByteArrayInputStream();
                }
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
        if (is == null) throw new MessagingException("Cannot load header");
        headers = new InternetHeaders(is);
        headersLoaded = true;
    }

    private synchronized void loadFlags() throws MessagingException {
        if (flags != null) return;
        synchronized (getMessageCacheLock()) {
            try {
                IMAPProtocol p = getProtocol();
                checkExpunged();
                flags = p.fetchFlags(getSequenceNumber());
            } catch (ConnectionException cex) {
                throw new FolderClosedException(folder, cex.getMessage());
            } catch (ProtocolException pex) {
                forceCheckExpunged();
                throw new MessagingException(pex.getMessage(), pex);
            }
        }
    }

    private synchronized boolean areHeadersLoaded() {
        return headersLoaded;
    }

    private synchronized void setHeadersLoaded(boolean loaded) {
        headersLoaded = loaded;
    }

    private synchronized boolean isHeaderLoaded(String name) {
        if (headersLoaded) return true;
        return (loadedHeaders != null) ? loadedHeaders.containsKey(name.toUpperCase(Locale.ENGLISH)) : false;
    }

    private synchronized void setHeaderLoaded(String name) {
        if (loadedHeaders == null) loadedHeaders = new Hashtable(1);
        loadedHeaders.put(name.toUpperCase(Locale.ENGLISH), name);
    }

    private String toSection(String what) {
        if (sectionId == null) return what; else return sectionId + "." + what;
    }

    private InternetAddress[] aaclone(InternetAddress[] aa) {
        if (aa == null) return null; else return (InternetAddress[]) aa.clone();
    }

    private Flags _getFlags() {
        return flags;
    }

    private ENVELOPE _getEnvelope() {
        return envelope;
    }

    private BODYSTRUCTURE _getBodyStructure() {
        return bs;
    }

    void _setFlags(Flags flags) {
        this.flags = flags;
    }

    Session _getSession() {
        return session;
    }
}

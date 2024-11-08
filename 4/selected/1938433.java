package owagate.pop;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import owagate.mail.MailBox;
import owagate.mail.MailMessage;
import owagate.util.Ex;

/**
 This class implements the POP3 commands in the PopTransactions interface. It
 does so given a MailBox and a PopTextStream.
 */
public class PopTransactionsImpl implements PopTransactions {

    /** Constructs a new instance of PopTransactionsImpl. */
    public PopTransactionsImpl(PopTextStream stream, MailBox mailbox) {
        mMail = mailbox.getMail();
        mMailBox = mailbox;
        mStream = stream;
        mDeletedMsgs = (mMail.length == 0) ? null : new boolean[mMail.length];
        for (int i = 0; i < mMail.length; ++i) mDeletedMsgs[i] = false;
    }

    public void handleDele(String body) {
        if (!parseParams(body, kRegExRequiredMsgId)) return;
        mStream.writeOk("message " + mMsgId + " deleted");
        mDeletedMsgs[mMsgIndex] = true;
    }

    public void handleList(String body) {
        if (!parseParams(body, kRegExOptionalMsgId)) return;
        if (mMsgId > 0) mStream.writeOk(mMsgId + " " + mMail[mMsgIndex].getSize()); else {
            mStream.writeOk();
            mStream.beginMultiLine();
            for (int i = 0; i < mMail.length; ++i) if (!mDeletedMsgs[i]) mStream.writeLine((i + 1) + " " + mMail[i].getSize());
            mStream.endMultiLine();
        }
    }

    public void handleNoop(String body) {
    }

    public void handleQuit(String body) {
        mStream.writeOk("Goodbye!");
        for (int i = 0; i < mMail.length; ++i) if (mDeletedMsgs[i]) mMail[i].remove();
    }

    public void handleRetr(String body) {
        if (!parseParams(body, kRegExRequiredMsgId)) return;
        MailMessage mail = mMail[mMsgIndex];
        mStream.writeOk(mail.getSize() + " octets");
        InputStream stream = mail.openMessage();
        try {
            mStream.beginMultiLine();
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            for (String line; (line = in.readLine()) != null; ) mStream.writeLine(line);
            mStream.endMultiLine();
        } catch (Exception e) {
            Ex.failed("Failed to read message", e);
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    public void handleRset(String body) {
        for (int i = 0; i < mDeletedMsgs.length; ++i) mDeletedMsgs[i] = false;
        mStream.writeOk();
    }

    public void handleStat(String body) {
        int count = 0;
        long size = 0;
        for (int i = 0; i < mMail.length; ++i) if (!mDeletedMsgs[i]) {
            ++count;
            size += mMail[i].getSize();
        }
        mStream.writeOk(count + " " + size);
    }

    public void handleTop(String body) {
        if (!parseParams(body, kRegExMsgIdAndOther)) return;
        mStream.writeErr("invalid command");
    }

    public void handleUidl(String body) {
        if (!parseParams(body, kRegExOptionalMsgId)) return;
        if (mMsgId > 0) mStream.writeOk(mMsgId + " " + mMail[mMsgIndex].getUniqueId()); else {
            mStream.writeOk();
            mStream.beginMultiLine();
            for (int i = 0; i < mMail.length; ++i) if (!mDeletedMsgs[i]) mStream.writeLine((i + 1) + " " + mMail[i].getUniqueId());
            mStream.endMultiLine();
        }
    }

    public void unknown(String line) {
        Ex.failed("invalid command");
    }

    /**
     Parses the given parameter string for a match to the given regex. The regex
     can specify an option or required msgid, or a required msgid and 2nd param.
     If the params don't match the regex, this method writes an -ERR and returns
     false. If a msgid is available, it is checked for validity. If valid, true
     is returned and mMsgId contains the value. Otherwise, -ERR is writtena and
     false is returned.
     */
    private boolean parseParams(String params, Pattern regex) {
        Matcher m = regex.matcher(params);
        if (m == null || !m.matches()) {
            mStream.writeErr("invalid parameters");
            return false;
        }
        String s;
        switch(m.groupCount()) {
            default:
                assert false;
                return false;
            case 2:
                s = m.group(2);
                mOtherParam = Integer.parseInt(s);
                if (mOtherParam < 0) {
                    mStream.writeErr("invalid 2nd parameter");
                    return false;
                }
            case 1:
                s = m.group(1);
                if (s == null) mMsgId = -1; else {
                    mMsgId = Integer.parseInt(s);
                    if (mMsgId < 1) {
                        mStream.writeErr("invalid msg number");
                        return false;
                    }
                    if (mMsgId > mMail.length) {
                        mStream.writeErr("no such message, only " + mMail.length + " messages in maildrop");
                        return false;
                    }
                    mMsgIndex = mMsgId - 1;
                    if (mDeletedMsgs[mMsgIndex]) {
                        mStream.writeErr("message " + mMsgId + " already deleted");
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    private static final Pattern kRegExOptionalMsgId = Pattern.compile("^\\s*(\\d+)?\\s*.*$");

    private static final Pattern kRegExRequiredMsgId = Pattern.compile("^\\s*(\\d+)\\s*.*$");

    private static final Pattern kRegExMsgIdAndOther = Pattern.compile("^\\s*(\\d+)\\s+(\\d+)\\s*.*$");

    private boolean[] mDeletedMsgs;

    private MailMessage[] mMail;

    private MailBox mMailBox;

    private PopTextStream mStream;

    private int mMsgId;

    private int mMsgIndex;

    private int mOtherParam;
}

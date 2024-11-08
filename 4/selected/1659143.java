package org.smapcore.smap.transport.beep;

import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.core.SessionCredential;
import org.smapcore.smap.core.SMAPCredential;
import org.smapcore.smap.core.SMAPException;
import org.smapcore.smap.core.SMAPHeader;
import org.smapcore.smap.core.SMAPMessage;
import org.smapcore.smap.core.SMAPParsedEnvelope;
import org.smapcore.smap.core.SMAPProc;
import org.smapcore.smap.core.SMAPStatus;

public class SMAPParser extends Object {

    public SMAPParser() {
        super();
    }

    public SMAPParsedEnvelope parse(Object message) throws SMAPException {
        Message beepMessage;
        try {
            beepMessage = (Message) message;
        } catch (ClassCastException cce) {
            throw new SMAPException("received message not a beep message");
        }
        if (beepMessage == null) {
            throw new SMAPException("null message");
        }
        SMAPParsedEnvelope envelope = new SMAPParsedEnvelope();
        InputStream is = beepMessage.getDataStream().getInputStream();
        envelope.setCredentials(parseCredential(is, beepMessage.getChannel().getSession().getPeerCredential()));
        envelope.setHeader(parseHeader(is));
        parseMessages(is, envelope);
        return (envelope);
    }

    private SMAPCredential parseCredential(InputStream input, final SessionCredential beepCredential) throws SMAPException {
        if (beepCredential == null) {
            return (null);
        }
        SMAPCredential cred = new SMAPCredential(beepCredential.getAuthenticator(), beepCredential.getAuthenticatorType(), beepCredential.getAlgorithm());
        return (cred);
    }

    private SMAPHeader parseHeader(InputStream input) throws SMAPException {
        String version = null;
        String url = null;
        String method = null;
        String errmsg = null;
        HashMap args = null;
        int errcode = 0;
        int msgnum = -1;
        boolean moreHeader = true;
        while (moreHeader == true) {
            byte[] line = getHeaderLine(input);
            if (line == null) {
                throw new SMAPException("cannot parse header");
            }
            String tag = getHeaderTag(line);
            String value = getHeaderValue(line);
            if (tag == null) {
                throw new SMAPException("unable to parse header");
            }
            if (value == null) {
                if (tag.startsWith("EOH")) {
                    moreHeader = false;
                }
            } else if (tag.equalsIgnoreCase("POST")) {
                int pos = value.indexOf(' ');
                url = value.substring(0, pos).trim();
                version = value.substring(pos + 1).trim();
            } else if (tag.equalsIgnoreCase("message-count")) {
                try {
                    msgnum = Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    throw new SMAPException("unable to parse message-number");
                }
            } else if (tag.equalsIgnoreCase("request-response")) {
                int pos = value.indexOf(' ');
                try {
                    errcode = Integer.parseInt(value.substring(0, pos));
                } catch (NumberFormatException nfe) {
                    throw new SMAPException("unable to parse rquest response");
                }
                errmsg = value.substring(pos + 1).trim();
            } else if (tag.equalsIgnoreCase("request")) {
                int pos = value.indexOf(' ');
                if (pos > 0) {
                    method = value.substring(0, pos).trim();
                    args = parseHeaderRequestArgs(value.substring(pos).trim().getBytes());
                } else {
                    method = value;
                }
            }
        }
        SMAPHeader header = new SMAPHeader(version, url);
        if (msgnum >= 0) {
            header.setMessageCount(msgnum);
        }
        if (errcode > 0) {
            header.setResponseStatus(new SMAPStatus(errcode, errmsg));
        }
        if (method != null) {
            header.setRequestProc(new SMAPProc(method, args));
        }
        return (header);
    }

    private void parseMessages(InputStream input, SMAPParsedEnvelope envelope) {
        MessageParser mp = new MessageParser(input, envelope);
        mp.start();
        return;
    }

    private byte[] getHeaderLine(InputStream input) {
        StringBuffer linebuf = new StringBuffer(100);
        boolean eol = false;
        char c;
        while (eol != true) {
            try {
                c = (char) input.read();
            } catch (IOException ie) {
                System.out.println("null char");
                return (null);
            }
            if (c == -1) {
                return (null);
            }
            if (c == '\r') {
                continue;
            }
            if (c != '\n') {
                linebuf.append(c);
            } else {
                eol = true;
            }
        }
        return (linebuf.toString().getBytes());
    }

    private String getHeaderTag(final byte[] line) {
        StringBuffer tagbuf = new StringBuffer(16);
        boolean begin = true;
        char c;
        for (int i = 0; i < line.length; i++) {
            c = (char) line[i];
            if ((begin == true) && ((c == ' ') || (c == '\t') || (c == '<'))) {
                continue;
            }
            if ((c == ':') || (c == ' ')) {
                return (tagbuf.toString().trim());
            }
            begin = false;
            tagbuf.append(c);
        }
        return (tagbuf.toString().trim());
    }

    private String getHeaderValue(final byte[] line) {
        StringBuffer tagbuf = new StringBuffer(512);
        boolean begin = true;
        int pos;
        for (pos = 0; pos < line.length; pos++) {
            if ((line[pos] == ':') || (line[pos] == ' ')) {
                break;
            }
        }
        if (pos == line.length) {
            return (null);
        }
        for (int i = pos + 1; i < line.length; i++) {
            char c = (char) line[i];
            if ((begin == true) && ((c == ' ') || (c == '\t'))) {
                continue;
            }
            begin = false;
            tagbuf.append(c);
        }
        for (int i = tagbuf.length() - 1; i > 0; i--) {
            char c = tagbuf.charAt(i);
            if (c == '>') {
                tagbuf.setCharAt(i, ' ');
            } else if ((c != ' ') && (c != '\t')) {
                break;
            }
        }
        return (tagbuf.toString().trim());
    }

    private HashMap parseHeaderRequestArgs(final byte[] line) {
        StringBuffer nameBuf = new StringBuffer(64);
        StringBuffer valueBuf = new StringBuffer(64);
        HashMap args = new HashMap();
        int pos = 0;
        while ((pos < line.length) && (line[pos] != '=')) {
            nameBuf.append(line[pos++]);
        }
        ++pos;
        while ((pos < line.length) && (line[pos] == ' ')) {
            ++pos;
        }
        boolean quote = false;
        while (pos < line.length) {
            if (line[pos] == '\\') {
                if (++pos >= line.length) {
                    break;
                }
                valueBuf.append(line[pos++]);
                continue;
            } else {
                if (line[pos] == '"') {
                    if (quote == true) {
                        quote = false;
                    } else {
                        quote = true;
                    }
                    ++pos;
                    continue;
                }
            }
            if (quote == false) {
                if ((line[pos] == ' ') || (line[pos] == '\t') || (line[pos] == ',') || (line[pos] == '&')) {
                    args.put(nameBuf.toString(), valueBuf.toString());
                    nameBuf.setLength(0);
                    valueBuf.setLength(0);
                    ++pos;
                    continue;
                }
            }
            valueBuf.append(line[pos++]);
        }
        if (nameBuf.length() > 0) {
            args.put(nameBuf.toString(), valueBuf.toString());
        }
        return (args);
    }

    class MessageParser extends Thread {

        private SMAPParsedEnvelope envelope;

        private InputStream input;

        MessageParser(InputStream input, SMAPParsedEnvelope envelope) {
            this.input = input;
            this.envelope = envelope;
        }

        public void run() {
            SMAPMessage message;
            while ((message = parseNextMessage()) != null) {
                this.envelope.addMessage(message);
                ((SMAPInputStream) message.getInputStream()).waitForEOM();
            }
            return;
        }

        private SMAPMessage parseNextMessage() {
            String content = null;
            Date date = new Date();
            int size = -1;
            while (true) {
                byte[] line = getHeaderLine(this.input);
                if (line == null) {
                    return (null);
                }
                String tag = getHeaderTag(line);
                String value = getHeaderValue(line);
                if (tag == null) {
                    return (null);
                }
                if (tag.equalsIgnoreCase("DATA")) {
                    break;
                } else if (value == null) {
                    break;
                }
                if (tag.equalsIgnoreCase("message-content")) {
                    content = value;
                } else if (tag.equalsIgnoreCase("message-length")) {
                    try {
                        size = Integer.parseInt(value);
                    } catch (NumberFormatException nfe) {
                    }
                } else if (tag.equalsIgnoreCase("message-date")) {
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy");
                    try {
                        date = sdf.parse(value);
                    } catch (Exception e) {
                    }
                }
            }
            SMAPInputStream sis;
            if (size == -1) {
                sis = new SMAPInputStream(this.input);
            } else {
                sis = new SMAPInputStream(this.input, size);
            }
            return (new SMAPMessage(content, sis, size, date));
        }
    }
}

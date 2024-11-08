package com.googlecode.yoohoo.xmppcore.protocol.stanza;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.yoohoo.xmppcore.protocol.XmlLangText;
import com.googlecode.yoohoo.xmppcore.protocol.parsing.ProtocolKey;
import com.googlecode.yoohoo.xmppcore.protocol.translation.IXmlWriter;

public class Message extends Stanza {

    public static final ProtocolKey PROTOCOL_KEY = new ProtocolKey("message", null);

    private List<XmlLangText> subjects;

    private List<XmlLangText> bodies;

    private String thread;

    private static String[] MESSAGE_TYPES;

    static {
        MessageType[] messageTypes = MessageType.values();
        MESSAGE_TYPES = new String[messageTypes.length];
        for (int i = 0; i < messageTypes.length; i++) {
            MESSAGE_TYPES[i] = messageTypes[i].toString();
        }
    }

    public Message() {
    }

    public List<XmlLangText> getSubjects() {
        if (subjects == null) {
            subjects = new ArrayList<XmlLangText>();
        }
        return subjects;
    }

    public void setSubjects(List<XmlLangText> subjects) {
        this.subjects = subjects;
    }

    public List<XmlLangText> getBodies() {
        if (bodies == null) {
            bodies = new ArrayList<XmlLangText>(1);
        }
        return bodies;
    }

    public void setBodies(List<XmlLangText> bodies) {
        this.bodies = bodies;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    @Override
    protected String[] getValidTypes() {
        return MESSAGE_TYPES;
    }

    @Override
    public StanzaKind getStanzaKind() {
        return StanzaKind.MESSAGE;
    }

    @Override
    protected void doToXml(IXmlWriter writer) {
        if (subjects != null) {
            for (XmlLangText subject : subjects) {
                writer.writeStartElement("subject");
                if (subject.getXmlLang() != null) {
                    writer.writeAttribute("xml:lang", subject.getXmlLang());
                }
                if (subject.getText() != null) {
                    writer.writeCharacters(subject.getText());
                }
                writer.writeEndElement();
            }
        }
        if (bodies != null) {
            for (XmlLangText body : bodies) {
                writer.writeStartElement("body");
                if (body.getXmlLang() != null) {
                    writer.writeAttribute("xml:lang", body.getXmlLang());
                }
                if (body.getText() != null) {
                    writer.writeCharacters(body.getText());
                }
                writer.writeEndElement();
            }
        }
        if (thread != null) {
            writer.writeStartElement("thread");
            writer.writeCharacters(thread);
            writer.writeEndElement();
        }
    }
}

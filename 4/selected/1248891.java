package vavi.net.xmpp;

import java.io.IOException;

/**
 * Message.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 040117 nsano initial version <br>
 */
public class Message extends Xmpp {

    /** */
    protected String getXmlName() {
        return "message";
    }

    /** */
    private String type;

    /** */
    public void setType(String type) {
        this.type = type;
    }

    /** */
    public String getType() {
        return type;
    }

    /** */
    private String from;

    /** */
    public void setFrom(String from) {
        this.from = from;
    }

    /** */
    public String getFrom() {
        return from;
    }

    /** */
    private String to;

    /** */
    public void setTo(String to) {
        this.to = to;
    }

    /** */
    public String getTo() {
        return to;
    }

    private String thread;

    /** */
    public void setThread(String thread) {
        this.thread = thread;
    }

    /** */
    public String getThread() {
        return thread;
    }

    private String subject;

    /** */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /** */
    public String getSubject() {
        return subject;
    }

    /** */
    private String body;

    /** */
    public void setBody(String body) {
        this.body = body;
    }

    /** */
    public String getBody() {
        return body;
    }

    /** */
    private String x;

    /** */
    public void setX(String x) {
        this.x = x;
    }

    /** */
    public String getX() {
        return x;
    }

    /** */
    private String error;

    /** */
    public void setError(String error) {
        this.error = error;
    }

    /** */
    public String getError() {
        return error;
    }

    /** */
    public static String getGroupChatSubjectString(String channelName, String text) {
        Message message = new Message();
        message.setType("groupchat");
        message.setTo(channelName);
        message.setSubject(text);
        return message.toString();
    }

    /** */
    public static String getGroupChatString(String channelName, String text) {
        Message message = new Message();
        message.setType("groupchat");
        message.setTo(channelName);
        message.setBody(text);
        return message.toString();
    }

    /** */
    public static String getPrivateChatString(String to, String text, String thread) {
        Message message = new Message();
        message.setType("chat");
        message.setTo(to);
        message.setThread(thread);
        message.setBody(text);
        return message.toString();
    }

    /** */
    public static String getPrivateMessageString(String to, String subject, String text, String thread) {
        Message message = new Message();
        message.setType("normal");
        message.setTo(to);
        message.setSubject(subject);
        message.setBody(text);
        return message.toString();
    }

    /** */
    public void handle(JabberConnection jc) throws IOException {
        if (type.equals("groupchat")) {
            System.err.println("##### gc mess recieved.");
            String f = jc.exCN(getFrom());
            if (getBody() != null && getBody().length() != 0) {
                System.err.println("##### handing message to groupchat");
                jc.getChat(f).processIncoming(jc.exN(getFrom()), getBody());
            } else {
                System.err.println("#### message is empty");
            }
            if (getSubject() != null && getSubject().length() != 0) {
                jc.getChat(f).processIncoming(jc.exN(getFrom()), "", getSubject());
            }
        } else if (type.equals("chat")) {
            System.err.println("##### private chat mess recieved.");
            String f = jc.exCN(getFrom());
            String thread = getThread();
            String body = getBody();
            if (body != null && body.length() != 0) {
                System.err.println("##### handing message to private chat");
                String t = "";
                if (thread != null) {
                    t = thread;
                }
                String from1 = getFrom();
                if (jc.isChat(from1) | jc.isPrivateChat(from1)) {
                    from1 = from1.substring(0, from1.indexOf("@"));
                } else {
                    from1 = from1.substring(from1.indexOf("/") + 1);
                }
                jc.fireEvent(this, "privateChat", new Object[] { getFrom(), t, from1, body });
            } else {
                System.err.println("#### message is empty");
            }
            jc.messageList.add(this);
        } else if (type.equals("normal") || type.equals("")) {
            System.err.println("##### private mess recieved.");
            String f = jc.exCN(getFrom());
            String body = getBody();
            String subject = getSubject();
            if (body != null && body.length() != 0) {
                System.err.println("##### displaying mesasage");
                jc.fireEvent(this, "displayMessage", this);
            } else {
                System.err.println("#### message is empty");
            }
            jc.messageList.add(this);
        } else if (type.equals("headline")) {
            jc.messageList.add(this);
            jc.fireEvent(this, "displayHeadline", this);
        } else if (type.equals("error")) {
            Chat gpc = jc.getChannelFor(getFrom());
            String error = getError();
            String t = "";
            if (getThread() != null) t = getThread();
            jc.getPrivateChat(getFrom(), true, t);
        }
        jc.fireEvent(this, "handleIQ", this);
        System.gc();
    }
}

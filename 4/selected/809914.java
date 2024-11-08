package vavi.net.xmpp;

import java.io.IOException;

/**
 * Presence.
 *
 * @author	<a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version	0.00	040125	nsano	initial version <br>
 */
public class Presence extends Xmpp {

    /** */
    protected String getXmlName() {
        return "presence";
    }

    /** */
    private String to;

    /** */
    public void setTo(String to) {
        this.to = to;
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
    private String status;

    /** */
    public void setStatus(String status) {
        this.status = status;
    }

    /** */
    public String getStatus() {
        return status;
    }

    /** */
    private String show;

    /** */
    public void setShow(String show) {
        this.show = show;
    }

    /** */
    public String getShow() {
        return show;
    }

    /** */
    public void handle(JabberConnection jc) throws IOException {
        String show = getShow();
        Chat gpc = jc.getChannelFor(getFrom());
        if (gpc.getType().equals("chat")) {
            String t = getType();
            if (t.equals("unavailable")) {
                System.err.println("*** " + getFrom() + ": unavailable.");
            } else {
                if (show != null) {
                    System.err.println("*** " + getFrom() + ": " + show);
                } else {
                    System.err.println("*** " + getFrom() + ": available.");
                }
            }
        }
        System.out.println("*#* Presence from " + getFrom());
        if (show == null) {
            Contact c = new Contact(this);
            jc.fireEvent(this, "updateContact", c);
            System.out.println("*#* contact: " + c.getJid());
            String t = getType();
            if (t != null) {
                if (t.equals("unavailable")) {
                    jc.fireEvent(this, "displayInGroupChats", new Object[] { getFrom(), "*** " + getFrom() + " is unavailable." });
                    jc.fireEvent(this, "displayInPrivateChats", new Object[] { getFrom(), "*** " + getFrom() + " is unavailable." });
                    if (jc.getChannelFor(getFrom()) != null) {
                        jc.fireEvent(this, "displayStatus", new Object[] { getFrom(), "unavailable ..." });
                    }
                    jc.fireEvent(this, "removeContact", c);
                } else if (t.equals("subscribe")) {
                    jc.fireEvent(this, "displaySubReq", this);
                }
            } else {
                c.setShow("online");
                jc.fireEvent(this, "updateContact", c);
            }
        } else {
            Contact c = new Contact(this);
            jc.fireEvent(this, "updateContact", c);
            System.out.println("*#* contact: " + c.getJid());
            c.setShow(show);
            System.out.println("show for " + c.getJid() + " set to " + c.getShow());
            c.setStatus("[no status]");
            jc.fireEvent(this, "updateContact", c);
            if (getStatus() != null) {
                c.setStatus(getStatus());
            }
            jc.fireEvent(this, "displayInPrivateChats", new Object[] { getFrom(), "*** " + getFrom() + " is " + c.getShow() + "/" + c.getStatus() + "." });
            if (jc.getChannelFor(getFrom()) != null) {
                jc.fireEvent(this, "displayStatus", getFrom());
            }
        }
        jc.fireEvent(this, "rebuildRoster");
    }
}

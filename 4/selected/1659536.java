package org.psepr.jClient;

import org.xmlpull.v1.XmlPullParser;

/**
 * Manage a lease on a channel.
 * This deals with the "internal" part of the lease -- the parsing, reception
 * and management of the lease.  One of these is created for each of the user's
 * leases and this holds the objects that are parsing, receiving and managing
 * the lease.
 * * @author Robert.Adams@intel.com
 *
 */
public class PsEPRLease {

    private String namespace;

    private String channel;

    private PayloadParser pParser;

    private EventReceiver pReceiver;

    private LeaseManager lManager;

    private DebugLogger log;

    /**
	 * 
	 */
    public PsEPRLease() {
        super();
        this.init();
    }

    /**
	 * Create a new lease.
	 * @param chan
	 * @param xpp
	 * @param xpr
	 * @param lm
	 */
    public PsEPRLease(String chan, PayloadParser xpp, EventReceiver xpr, LeaseManager lm) {
        super();
        this.init();
        this.setChannel(chan);
        this.setNamespace(xpp.getNamespace());
        this.setLeaseManager(lm);
        this.setPayloadParser(xpp);
        this.setEventReceiver(xpr);
        return;
    }

    private void init() {
        namespace = "";
        channel = "";
        pParser = null;
        pReceiver = null;
        log = new DebugLogger(PsEPRService.AppName, "PsEPRLease");
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String ns) {
        namespace = ns;
        return;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String ch) {
        if (ch == null || (ch.length() < 1) || !ch.startsWith("/") || !ch.endsWith("/")) {
            throw new PsEPRException("Setting lease on a channel without beginning and ending slashes");
        }
        channel = ch;
        return;
    }

    public PayloadParser getPayloadParser() {
        return pParser;
    }

    public void setPayloadParser(PayloadParser xpp) {
        pParser = xpp;
        return;
    }

    public EventReceiver getEventReceiver() {
        return pReceiver;
    }

    public void setEventReceiver(EventReceiver xpp) {
        pReceiver = xpp;
        return;
    }

    public LeaseManager getLeaseManager() {
        return lManager;
    }

    public void setLeaseManager(LeaseManager xlm) {
        lManager = xlm;
        return;
    }

    /**
	 * Called by the connection for every message that comes in.
	 * We see if it's for us and return the parsed payload if so.
	 * @param ns the namespace found in the payload element
	 * @param parser pointed into the payload element. Returned after.
	 * @return a Payload object or 'null' if we didn't parse it
	 */
    public Payload parsePayload(String ns, XmlPullParser parser) {
        Payload tPL = null;
        log.log(log.INVOCATION, "Enter parsePayload. ns=" + ns);
        if (lManager != null) {
            log.log(log.LEASEDETAIL, "Lease message for known channel. Passing to lease manager.");
            tPL = lManager.parsePayload(ns, parser);
        }
        if ((tPL == null) && (pParser != null)) {
            log.log(log.LEASEDETAIL, "Message on known channel. Parsing by user routine.");
            tPL = pParser.parsePayload(ns, parser);
        }
        return tPL;
    }

    public boolean receiveEvent(PsEPREvent ev) {
        boolean processed = false;
        log.log(log.INVOCATION, "Enter receive event. chan=" + channel + ", tochan=" + ev.getToChannel());
        if (PsEPRService.isSubChannel(channel, ev.getToChannel())) {
            try {
                if (lManager != null && ev != null) {
                    processed = lManager.receiveEvent(ev);
                }
                if (!processed) {
                    if (ev.getPayload() != null) {
                        if (this.getNamespace() == null || this.getNamespace().length() == 0 || ev.getPayload().getNamespace().equalsIgnoreCase(this.getNamespace())) {
                            processed = this.getEventReceiver().receiveEvent(ev);
                        } else {
                            log.log(log.LEASEDETAIL, "Received payload of unexpected type. Found=" + ev.getPayload().getNamespace() + ", wanted=" + this.getNamespace());
                        }
                    } else {
                        log.log(log.LEASEDETAIL, "Received null payload");
                    }
                }
            } catch (Exception e) {
                log.log(log.BADERROR, "PsEPRLease.receiveEvent exception:" + e.toString());
                processed = false;
            }
        } else {
            log.log(log.DETAIL, "Not my channel. (have=" + ev.getToChannel() + ", want=" + channel);
        }
        return processed;
    }
}

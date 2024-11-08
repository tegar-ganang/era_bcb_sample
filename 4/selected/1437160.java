package org.psepr.jClient;

import java.util.Iterator;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;

/**
 * <p>
 * This handles the collection of leases for the connection.  This abstracts
 * the information from a data structure in the connection class into
 * something that can be used by everyone who handles the incoming
 * stream of messages - the parser and the queuer.
 * </p>
 * <p>
 * This is not used by any user of the library.
 * </p>
 * @author Robert.Adams@intel.com
 */
public class LeaseCollection {

    private ArrayList leases = new ArrayList();

    private DebugLogger log = null;

    /**
	 * 
	 */
    public LeaseCollection() {
        super();
        this.init();
    }

    private void init() {
        log = new DebugLogger(PsEPRService.AppName, "LeaseCollection");
        return;
    }

    public synchronized void addLease(PsEPRLease ls) {
        log.log(log.INVOCATION, "Enter addLease: chan=" + ls.getChannel() + ", ns=" + ls.getNamespace());
        if (!leases.contains(ls)) {
            leases.add(ls);
            ls.getLeaseManager().start();
        }
        return;
    }

    public synchronized void releaseLease(PsEPRLease ls) {
        log.log(log.INVOCATION, "Enter removeLease: chan=" + ls.getChannel() + ", ns=" + ls.getNamespace());
        int ii = leases.indexOf(ls);
        if (ii >= 0) {
            log.log(log.INVOCATION, "RemoveLease: removing lease");
            leases.remove(ii);
            ls.getLeaseManager().release();
        }
        return;
    }

    public void releaseAllLeases() {
        log.log(log.INVOCATION, "Enter removeAllLeases");
        while (!leases.isEmpty()) {
            try {
                this.releaseLease((PsEPRLease) leases.get(0));
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
	 * Called to force all of the leases to renegotiate.  This usually
	 * happens when the underlying XMPP connection has dropped and
	 * is re-establised.
	 */
    public synchronized void forceRenegotiation() {
        log.log(log.INVOCATION, "Enter forceRenegotiation");
        for (Iterator ii = leases.iterator(); ii.hasNext(); ) {
            try {
                ((PsEPRLease) ii.next()).getLeaseManager().forceRenegotiation();
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
	 * Called when every payload is received. The caller expects this
	 * routine to find a parser for the payload and, if found, return
	 * the parsed payload.  If no payload parser is found, return 'null'.
	 * @param ns the contents of xmlns of the payload
	 * @param parser
	 * @return the parsed payload or 'null' if no parser found
	 */
    public Payload parsePayload(String ns, XmlPullParser parser) {
        Payload myPL = null;
        log.log(log.INVOCATION, "Enter parsePayload. ns=" + ns);
        for (Iterator ii = leases.iterator(); ii.hasNext(); ) {
            try {
                PsEPRLease aLease = (PsEPRLease) ii.next();
                myPL = aLease.parsePayload(ns, parser);
                if (myPL != null) {
                    break;
                }
            } catch (Exception e) {
                log.log(log.BADERROR, "parsePayload exploded.");
                throw new PsEPRException("LeaseCollection.parsePayload exception: " + e.toString());
            }
        }
        return myPL;
    }

    /**
	 * Called when every event is received.  This routine is to give
	 * the event to every receiver who can process it.
	 * This is not synchronized with the presumtion that lower level
	 * routines (the lease classes themselves) will handle locking.
	 * @param ev the PsEPREvent structure of the received event
	 */
    public void receiveEvent(PsEPREvent ev) {
        log.log(log.INVOCATION, "Enter receiveEvent.");
        for (Iterator ii = leases.iterator(); ii.hasNext(); ) {
            try {
                PsEPRLease aLease = (PsEPRLease) ii.next();
                aLease.receiveEvent(ev);
            } catch (Exception e) {
            }
        }
        return;
    }
}

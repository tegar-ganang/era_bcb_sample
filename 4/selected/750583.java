package org.psepr.jClient;

/**
 * Create a single lease and keep an eye on it and call back if it fails.
 * <p>
 * When one calls for a lease, it may take a while to aquire same and, later,
 * the connectin could drop or the server could not be replying to the lease
 * so the application must take some action.
 * </p>
 * <p>
 * Using this routine to create a lease also creates a watcher thread that
 * keeps checking to make sure the lease is still alive.  If the lease goes
 * inactive, the PsEPR connection is forced to reconnect and the lease will
 * get renegotiated.
 * </p>
 * <p>
 * If the lease cannot be restored, an optional routine that implements the
 * <code>LeaseWatcher</code> interface is called with an error string.
 * </p>
 * <p>
 * Code using this routine looks like:
 * </p>
 * <pre>
 * // pConn == an aquired PsEPRConnection (see <code>PsEPRConnection</code>
 * LeaseHandler inLease = null;
 * try {
 * 	// get some parser for the payloads
 * 	PayloadParser xParser = new PayloadGeneric();
 * 	// ask for the lease (notice no <code>LeaseManager</code> specified
 * 	inLease = new LeaseHandler(pConn, "/", xParser, eQueue, null);
 * // wait for the lease to be active
 * inLease.waitForLease();
 * }
 * catch (PsEPRException e) {
 * 	System.out.println("GetLease failed:"+e.toString());
 * 	pConn.close();
 * 	return;
 * }
 * catch (Exception e) {
 * 	// who knows why this would ever happen
 * 	System.out.println("GetLease exception:"+e.toString());
 * 	pConn.close();
 * 	return;
 * }
 * </pre>
 * 
 * @author Robert.Adams@intel.com
 */
public class LeaseHandler {

    private PsEPRConnection pConn;

    private String channel;

    private PayloadParser payloadParser;

    private EventReceiver eventReceiver;

    private LeaseWatcher leaseWatcher;

    private PsEPRLease pLease;

    private boolean keepChecking;

    /**
	 * 
	 */
    public LeaseHandler() {
        super();
        this.init();
    }

    /**
	 * Create a lease on a channel. The payloads are 'generic' and are returned as
	 * strings of XML (PayloadGeneric).
	 * @param con an open PsEPRConnection
	 * @param chan the channel to get a lease on
	 * @param eq where to send the received events
	 * @throws PsEPRException
	 */
    public LeaseHandler(PsEPRConnection con, String chan, EventReceiver eq) throws PsEPRException {
        super();
        this.init();
        this.setConnection(con);
        this.setChannel(chan);
        this.setPayloadParser(new PayloadGeneric());
        this.setEventReceiver(eq);
        connectLease();
    }

    /**
	 * Create a lease on a channel. The payloads are 'generic' and are returned as
	 * strings of XML (PayloadGeneric).
	 * @param con an open PsEPRConnection
	 * @param chan the channel to get a lease on
	 * @param eq where to send the received events
	 * @param lw routine to call of a lease cannot be renegotiated (may be null)
	 * @throws PsEPRException
	 */
    public LeaseHandler(PsEPRConnection con, String chan, EventReceiver eq, LeaseWatcher lw) throws PsEPRException {
        super();
        this.init();
        this.setConnection(con);
        this.setChannel(chan);
        this.setPayloadParser(new PayloadGeneric());
        this.setEventReceiver(eq);
        this.setLeaseWatcher(lw);
        connectLease();
    }

    /**
	 * Create a managed lease on a channel.
	 * @param con an open PsEPRConnection
	 * @param chan the channel to get a lease on
	 * @param pp the parser for the payload
	 * @param eq where to send the received events
	 * @param lw routine to call of a lease cannot be renegotiated (may be null)
	 * @throws PsEPRException
	 */
    public LeaseHandler(PsEPRConnection con, String chan, PayloadParser pp, EventReceiver eq, LeaseWatcher lw) throws PsEPRException {
        super();
        this.init();
        this.setConnection(con);
        this.setChannel(chan);
        this.setPayloadParser(pp);
        this.setEventReceiver(eq);
        this.setLeaseWatcher(lw);
        connectLease();
    }

    private void init() {
        this.pConn = null;
        this.channel = null;
        this.payloadParser = null;
        this.eventReceiver = null;
        this.leaseWatcher = null;
        this.pLease = null;
        this.keepChecking = false;
    }

    public PsEPRConnection getConnection() {
        return this.pConn;
    }

    public void setConnection(PsEPRConnection xx) {
        this.pConn = xx;
        return;
    }

    public String getChannel() {
        return this.channel;
    }

    public void setChannel(String xx) {
        this.channel = xx;
        return;
    }

    public PayloadParser getPayloadParser() {
        return this.payloadParser;
    }

    public void setPayloadParser(PayloadParser xx) {
        this.payloadParser = xx;
        return;
    }

    public EventReceiver getEventReceiver() {
        return this.eventReceiver;
    }

    public void setEventReceiver(EventReceiver xx) {
        this.eventReceiver = xx;
        return;
    }

    public LeaseWatcher getLeaseWatcher() {
        return this.leaseWatcher;
    }

    public void setLeaseWatcher(LeaseWatcher xx) {
        this.leaseWatcher = xx;
        return;
    }

    public PsEPRLease getLease() {
        return this.pLease;
    }

    /**
	 * Do a manual connect after setting all the correct parameters
	 */
    public void connect() throws PsEPRException {
        connectLease();
    }

    /**
	 * Disconnect the lease from the connection
	 *
	 */
    public void disconnect() {
        if (this.pConn != null && this.pLease != null) {
            this.pConn.releaseLease(this.pLease);
        }
        this.keepChecking = false;
        this.pConn = null;
        this.pLease = null;
    }

    /**
	 * Wait until the lease is active.
	 */
    public void waitForLease() {
        if (this.pConn == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null connection handle");
        }
        if (this.channel == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null channel");
        }
        if (this.payloadParser == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null event parser");
        }
        if (this.eventReceiver == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null event receiver");
        }
        int retryCount = 10;
        while (retryCount-- > 0) {
            if (this.pLease.getLeaseManager().getLeaseActive()) {
                break;
            }
            try {
                Thread.sleep(5);
            } catch (Exception e) {
                throw new PsEPRException("LeaseHandler.waitForLease: After 10 trys, could not get lease to wait for");
            }
        }
    }

    /**
	 * Internal routine that creates the lease and the timer that watches it
	 */
    private void connectLease() {
        if (this.pConn == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null connection handle");
        }
        if (this.channel == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null channel");
        }
        if (this.payloadParser == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null event parser");
        }
        if (this.eventReceiver == null) {
            throw new PsEPRException("LeaseHandler.waitForLease: cannot wait because passed null event receiver");
        }
        try {
            this.pLease = this.pConn.getLease(this.channel, this.payloadParser, this.eventReceiver);
        } catch (PsEPRException e) {
            this.pConn.close();
            throw e;
        }
        Thread checker = new Thread(new CheckConnection());
        checker.setName("LeaseHandler:" + this.pLease.getChannel());
        keepChecking = true;
        checker.start();
    }

    /**
	 * TimerTask to wait keep checking if the connection is good.
	 * If the connection goes down, it's time for us to stop.
	 */
    private class CheckConnection implements Runnable {

        public void run() {
            doWait(60000);
            while (keepChecking) {
                if (LeaseHandler.this.pConn != null) {
                    if (LeaseHandler.this.pLease != null) {
                        if (LeaseHandler.this.pConn.isConnected) {
                            if (!LeaseHandler.this.pLease.getLeaseManager().getLeaseActive()) {
                                LeaseHandler.this.pConn.forceReconnection();
                            }
                        } else {
                            callWatcher("pConn.isConnected returned false");
                        }
                    } else {
                        callWatcher("pLease is null");
                    }
                } else {
                    callWatcher("pConn is null");
                }
                doWait(10000);
            }
        }

        private void doWait(long ww) {
            try {
                Thread.sleep(ww);
            } catch (Exception e) {
            }
        }

        private void callWatcher(String reason) {
            try {
                if (LeaseHandler.this.leaseWatcher != null) {
                    LeaseHandler.this.leaseWatcher.LeaseStateChange(LeaseHandler.this.pLease, reason);
                }
            } catch (Exception e) {
            }
        }
    }
}

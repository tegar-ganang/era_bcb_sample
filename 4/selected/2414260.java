package org.psepr.WatchChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import org.psepr.jClient.PayloadGeneric;
import org.psepr.jClient.PayloadParser;
import org.psepr.jClient.PsEPRConnection;
import org.psepr.jClient.PsEPRLease;
import org.psepr.jClient.PsEPRService;
import org.psepr.jClient.PsEPRServiceIdentity;

/**
 * Attribute send provides a simple interface to send PsEPR attributes 
 * 
 * @author Paul Brett <paul.brett@intel.com>
 */
public class ChannelWatcher {

    /** CvsId */
    public static final String CvsId = "$Id: ChannelWatcher.java 33 2007-03-08 18:31:28Z Misterblue $";

    private String channel = null;

    private boolean verbose = false;

    private String fromSvc = null;

    private String password = null;

    private ArrayList<String> userConfigs = new ArrayList<String>();

    private PsEPRConnection pConn = null;

    private PsEPRLease pLease;

    private String type = ANY_TYPE;

    private static final String ANY_TYPE = "ANY";

    private EventPrinter printer;

    /**
     * TODO
     * 
     * @param printer
     */
    public ChannelWatcher(EventPrinter printer) {
        this.printer = printer;
    }

    /**
     * Extract any command line parameters and return the unused parameters
     * 
     * @param args
     * @return unread parameters
     */
    public String[] readArgs(String[] args) {
        Vector<String> v = new Vector<String>();
        for (int jj = 0; jj < args.length; jj++) {
            if (args[jj].equalsIgnoreCase("--type")) {
                this.type = args[++jj];
            } else if (args[jj].equalsIgnoreCase("--channel")) {
                this.channel = args[++jj];
            } else if (args[jj].equalsIgnoreCase("--service")) {
                this.fromSvc = args[++jj];
            } else if (args[jj].equalsIgnoreCase("--password")) {
                this.password = args[++jj];
            } else if (args[jj].equalsIgnoreCase("--config")) {
                this.userConfigs.add(args[++jj]);
            } else if (args[jj].equalsIgnoreCase("--verbose")) {
                this.verbose = true;
            } else {
                v.add(args[jj]);
            }
        }
        return v.toArray(new String[0]);
    }

    /**
     * Connect to the PsEPR network
     * @throws IOException if PsEPR connection cannot be established
     * 
     */
    public void connect() throws IOException {
        if (this.userConfigs.size() > 0) {
            String[] uc = new String[this.userConfigs.size()];
            PsEPRService.setConfigFiles(this.userConfigs.toArray(uc));
        }
        PsEPRServiceIdentity pServiceID = new PsEPRServiceIdentity();
        if ((this.fromSvc == null) || (this.password == null)) {
            if (this.verbose) {
                System.out.println("Using configuration default service/password");
            }
            pServiceID.useDefaultIdentity();
        } else {
            pServiceID.setSimpleIdentity(this.fromSvc, this.password);
        }
        this.pConn = new PsEPRConnection(pServiceID);
        if (this.pConn == null) {
            throw new IOException("Cannot connect to PsEPR");
        }
        PayloadParser xParser;
        if (this.type == ANY_TYPE) {
            xParser = new PayloadGeneric();
        } else {
            xParser = new PayloadGeneric(this.type);
        }
        this.pLease = this.pConn.getLease(this.channel, xParser, this.printer);
    }

    /**
     * Disconnect the PsEPR connection (if currently connected)
     */
    public void disconnect() {
        if (this.pConn != null) {
            this.pConn.close();
            this.pConn = null;
        }
    }

    /** @return Returns the fromService. */
    public String getFromService() {
        return this.fromSvc;
    }

    /** @param service The fromService to set. */
    public void setFromService(String service) {
        this.fromSvc = service;
    }

    /** @param password The fromServicePassword to set. */
    public void setFromServicePassword(String password) {
        this.password = password;
    }

    /** @return Returns the channel. */
    public String getChannel() {
        return this.channel;
    }

    /** @param channel The channel to set. */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /** @return Returns the verbose. */
    public boolean isVerbose() {
        return this.verbose;
    }

    /** @param verbose The verbose to set. */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /** @return Returns the type. */
    public String getType() {
        return this.type;
    }

    /** @param type The type to set. */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * TODO 
     * @return TODO
     */
    public boolean isConnected() {
        if (this.pConn == null) return false;
        if (!this.pConn.isConnected) return false;
        if (this.pLease == null) return false;
        return (this.pLease.getLeaseManager().getLeaseActive());
    }
}

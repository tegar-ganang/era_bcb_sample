package com.vinny.xacml.event;

import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vinny.xacml.Resource;

public class TransactionResourceControlEvent extends EventObject {

    public static final String ALL_REGISTERED = "ALL_REGISTERED";

    public static final String SERIALIZED = "SERIALIZED";

    public static final String UNDO = "UNDO";

    public static final String REDO = "REDO";

    public static final String RERUN_TRANSACTION_PERMIT = "RERUN_TRANSACTION_PERMIT";

    public static final String RERUN_TRANSACTION_DENY = "RERUN_TRANSACTION_DENY";

    public static final String RERUN_TRANSACTION_RUN_PERMIT = "RERUN_TRANSACTION_RUN_PERMIT";

    public static final String RERUN_TRANSACTION_RUN_DENY = "RERUN_TRANSACTION_RUN_DENY";

    String pdpId, transactionId, type;

    List<String> readlist, writelistPermit, writelistDeny;

    Map<String, Resource> resources;

    Set<String> writeResourceIdsForPdp;

    public Set<String> getWriteResourceIdsForPdp() {
        return writeResourceIdsForPdp;
    }

    public void setWriteResourceIdsForPdp(Set<String> writeResourceIdsForPdp) {
        this.writeResourceIdsForPdp = writeResourceIdsForPdp;
    }

    public TransactionResourceControlEvent(Object source, String pdpId, String transactionId, List<String> readlist, List<String> writelistPermit, List<String> writelistDeny, String type) {
        super(source);
        this.pdpId = pdpId;
        this.transactionId = transactionId;
        this.type = type;
        this.readlist = readlist;
        this.writelistPermit = writelistPermit;
        this.writelistDeny = writelistDeny;
    }

    public String getPdpId() {
        return pdpId;
    }

    public List<String> getReadlist() {
        return readlist;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public List<String> getWritelistPermit() {
        return writelistPermit;
    }

    public List<String> getWritelistDeny() {
        return writelistDeny;
    }

    @Override
    public String toString() {
        String rep = "";
        rep = rep + "{ pdpId=" + pdpId + ", tId=" + transactionId + ", type=" + type;
        if (readlist != null) {
            rep += ", reads=" + readlist.toString();
        }
        if (writelistPermit != null) {
            rep += ", writesPermit=" + writelistPermit.toString();
        }
        if (writelistPermit != null) {
            rep += ", writesDeny=" + writelistDeny.toString();
        }
        return rep + "}";
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

    public void setResources(Map<String, Resource> resources) {
        this.resources = resources;
    }
}

package com.vinny.xacml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.vinny.xacml.event.RegisterResourceEvent;
import com.vinny.xacml.event.TransactionResourceControlEvent;

public class Transaction {

    private String transactionId;

    private String pdpId;

    private Hashtable<String, Resource> globalDenyLog;

    private Hashtable<String, Resource> globalPermitLog;

    private Map<String, Resource> localCachePermit;

    private Map<String, Resource> localCacheDeny;

    private Hashtable<String, String> subTransactionStatuses;

    private Hashtable<String, ArrayList<Boolean>> subTransactionStatusesForPermitAndDeny;

    private List<String> reads;

    private List<String> localWritesOnPermit;

    private List<String> localWritesOnDeny;

    private List<String> globalWritesOnPermit;

    private List<String> globalWritesOnDeny;

    public boolean[] transactionPermitDenyNature;

    private String combiningAlgorithm;

    public static final String COMBINING_ALGORITHM_DENY_OVERRIDES = "DENY_OVERRIDES";

    public static final String COMBINING_ALGORITHM_PERMIT_OVERRIDES = "PERMIT_OVERRIDES";

    private int status = 0;

    public static final String TRM_STATUS_INIT = "TRM_STATUS_INIT";

    public static final String TRM_STATUS_SERIALIZED = "TRM_STATUS_SERIALIZED";

    private String trmStatus;

    public boolean myFinalResult;

    public Transaction(String transactionId, List readList, List localWriteOnPermitList, List localWriteOnDenyList, List globalWriteOnPermitList, List globalWriteOnDenyList, String pdpId, String combiningAlgorithm, boolean[] transactionPermitDenyNature) {
        super();
        this.transactionId = transactionId;
        this.pdpId = pdpId;
        this.localCachePermit = new HashMap<String, Resource>();
        this.localCacheDeny = new HashMap<String, Resource>();
        this.globalDenyLog = new Hashtable<String, Resource>();
        this.globalPermitLog = new Hashtable<String, Resource>();
        this.subTransactionStatuses = new Hashtable<String, String>();
        this.subTransactionStatusesForPermitAndDeny = new Hashtable<String, ArrayList<Boolean>>();
        this.reads = readList;
        this.localWritesOnPermit = localWriteOnPermitList;
        this.localWritesOnDeny = localWriteOnDenyList;
        this.globalWritesOnPermit = globalWriteOnPermitList;
        this.globalWritesOnDeny = globalWriteOnDenyList;
        this.combiningAlgorithm = combiningAlgorithm;
        this.transactionPermitDenyNature = transactionPermitDenyNature;
        trmStatus = TRM_STATUS_INIT;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getStatus() {
        return status;
    }

    public Hashtable<String, String> getSubTransactionStatuses() {
        return subTransactionStatuses;
    }

    public void setSubTransactionStatuses(Hashtable<String, String> subTransactionStatuses) {
        this.subTransactionStatuses = subTransactionStatuses;
    }

    private void readResource(String resourceId) {
        Resource resource = null;
        if (!localCachePermit.containsKey(resourceId)) {
            resource = ResourceManager.getResource(resourceId);
            if (resource == null) {
                System.out.println("Resource: " + resourceId + " not found");
            } else {
                localCachePermit.put(resourceId, resource);
                localCacheDeny.put(resourceId, resource);
            }
        } else {
            resource = localCachePermit.get(resourceId);
        }
    }

    private Resource writeResource(String resourceId, boolean permitOrDeny) {
        Map<String, Resource> localCache = null;
        if (permitOrDeny) {
            localCache = localCachePermit;
        } else {
            localCache = localCacheDeny;
        }
        Resource resource = null;
        if (!localCache.containsKey(resourceId)) {
            resource = ResourceManager.getResource(resourceId);
            if (resource == null) {
                System.out.println("Resource: " + resourceId + " not found");
                return null;
            } else {
                localCache.put(resourceId, resource);
            }
        } else {
            resource = localCache.get(resourceId).clone();
        }
        resource.setData(resource.getData() + ":WRITE-" + pdpId + "-" + transactionId);
        if (permitOrDeny) {
            globalPermitLog.put(resource.getId(), resource);
        } else {
            globalDenyLog.put(resource.getId(), resource);
        }
        return resource;
    }

    public void begin() {
        List<String> readlist = new ArrayList<String>();
        List<String> writelistPermit = new ArrayList<String>();
        List<String> writelistDeny = new ArrayList<String>();
        for (String readResourceId : reads) {
            readResource(readResourceId);
            readlist.add(readResourceId);
        }
        if (transactionPermitDenyNature[0]) {
            for (String readResourceId : localWritesOnPermit) {
                Resource w = writeResource(readResourceId, true);
                if (w != null) {
                    writelistPermit.add(w.getId());
                }
            }
        } else {
            for (String readResourceId : localWritesOnDeny) {
                Resource w = writeResource(readResourceId, true);
                if (w != null) {
                    writelistPermit.add(w.getId());
                }
            }
        }
        for (String readResourceId : globalWritesOnPermit) {
            Resource w = writeResource(readResourceId, true);
            if (w != null) {
                writelistPermit.add(w.getId());
            }
        }
        if (transactionPermitDenyNature[1]) {
            for (String readResourceId : localWritesOnPermit) {
                Resource w = writeResource(readResourceId, false);
                if (w != null) {
                    writelistDeny.add(w.getId());
                }
            }
        } else {
            for (String readResourceId : localWritesOnDeny) {
                Resource w = writeResource(readResourceId, false);
                if (w != null) {
                    writelistDeny.add(w.getId());
                }
            }
        }
        for (String readResourceId : globalWritesOnDeny) {
            Resource w = writeResource(readResourceId, false);
            if (w != null) {
                writelistDeny.add(w.getId());
            }
        }
        TransactionResourceControlEvent event = new TransactionResourceControlEvent(this, pdpId, transactionId, readlist, writelistPermit, writelistDeny, TransactionResourceControlEvent.ALL_REGISTERED);
        TransactionResourceManager.getInstance().enqueEvent(event);
    }

    public void rerun(Map<String, Resource> newValues, Set<String> writeResourceIdsForPdp, boolean permitOrDeny) {
        List<String> globalWrites = null;
        Hashtable<String, Resource> log;
        boolean transactionPermitDeny = true;
        Map<String, Resource> localCache = null;
        String resourceControlEvent = "";
        if (permitOrDeny) {
            localCache = localCachePermit;
            transactionPermitDeny = transactionPermitDenyNature[0];
            globalWrites = globalWritesOnPermit;
            log = globalPermitLog;
            resourceControlEvent = TransactionResourceControlEvent.RERUN_TRANSACTION_RUN_PERMIT;
        } else {
            localCache = localCacheDeny;
            transactionPermitDeny = transactionPermitDenyNature[1];
            globalWrites = globalWritesOnDeny;
            log = globalDenyLog;
            resourceControlEvent = TransactionResourceControlEvent.RERUN_TRANSACTION_RUN_DENY;
        }
        if (newValues != null) {
            localCache.putAll(newValues);
        }
        Map<String, Resource> writemap = new HashMap<String, Resource>();
        for (String readResourceId : reads) {
            readResource(readResourceId);
        }
        if (transactionPermitDeny) {
            for (String readResourceId : localWritesOnPermit) {
                Resource w = writeResource(readResourceId, permitOrDeny);
                if (w != null) {
                    writemap.put(w.getId(), w);
                }
            }
        } else {
            for (String readResourceId : localWritesOnDeny) {
                Resource w = writeResource(readResourceId, permitOrDeny);
                if (w != null) {
                    writemap.put(w.getId(), w);
                }
            }
        }
        for (String readResourceId : globalWrites) {
            Resource w = writeResource(readResourceId, true);
            if (w != null) {
                writemap.put(w.getId(), w);
            }
        }
        Set<String> removeRedoResourceList = new HashSet<String>();
        for (String resourceID : log.keySet()) {
            if (!writeResourceIdsForPdp.contains(resourceID)) {
                removeRedoResourceList.add(resourceID);
            }
        }
        for (String removeResource : removeRedoResourceList) {
            log.remove(removeResource);
        }
        TransactionResourceControlEvent event = new TransactionResourceControlEvent(this, pdpId, transactionId, null, null, null, resourceControlEvent);
        event.setResources(writemap);
        TransactionResourceManager.getInstance().enqueEvent(event);
    }

    public void onGlobalPermit() {
        System.out.println(pdpId + " PERMIT COMMITTING");
        for (String writeId : globalPermitLog.keySet()) {
            ResourceManager.putResource(globalPermitLog.get(writeId));
        }
    }

    public void onGlobalDeny() {
        System.out.println(pdpId + " DENY COMMITTING");
        for (String writeId : globalDenyLog.keySet()) {
            ResourceManager.putResource(globalDenyLog.get(writeId));
        }
    }

    public String getTrmStatus() {
        return trmStatus;
    }

    public void setTrmStatus(String trmStatus) {
        this.trmStatus = trmStatus;
    }

    public Hashtable<String, ArrayList<Boolean>> getSubTransactionStatusesForPermitAndDeny() {
        return subTransactionStatusesForPermitAndDeny;
    }

    public void setSubTransactionStatusesForPermitAndDeny(Hashtable<String, ArrayList<Boolean>> subTransactionStatusesForPermitAndDeny) {
        this.subTransactionStatusesForPermitAndDeny = subTransactionStatusesForPermitAndDeny;
    }

    public boolean evaluatePermitOrDeny() {
        if (combiningAlgorithm.equals(COMBINING_ALGORITHM_PERMIT_OVERRIDES)) {
            if (transactionPermitDenyNature[0] == true) {
                return true;
            } else {
                for (String sub : subTransactionStatusesForPermitAndDeny.keySet()) {
                    ArrayList<Boolean> subEval = subTransactionStatusesForPermitAndDeny.get(sub);
                    if (subEval.get(0).booleanValue()) {
                        return true;
                    }
                }
            }
            return false;
        } else if (combiningAlgorithm.equals(COMBINING_ALGORITHM_DENY_OVERRIDES)) {
            if (transactionPermitDenyNature[0] == false) {
                return false;
            } else {
                for (String sub : subTransactionStatusesForPermitAndDeny.keySet()) {
                    ArrayList<Boolean> subEval = subTransactionStatusesForPermitAndDeny.get(sub);
                    if (!subEval.get(0).booleanValue()) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}

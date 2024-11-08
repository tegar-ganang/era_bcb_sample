package org.tripcom.api.execution;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Collection;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.tripcom.integration.exception.*;
import org.tripcom.api.controller.BusConnectionManager;
import org.tripcom.api.controller.Controller;
import org.tripcom.api.controller.SystemBusArea;
import org.tripcom.integration.entry.ClientInfo;
import org.tripcom.integration.entry.Component;
import org.tripcom.integration.entry.DataResultExternal;
import org.tripcom.integration.entry.KernelAddress;
import org.tripcom.integration.entry.LoadQueryTSAdapterEntry;
import org.tripcom.integration.entry.LoadResultEntry;
import org.tripcom.integration.entry.ManagementDataResultExternal;
import org.tripcom.integration.entry.ManagementOperation;
import org.tripcom.integration.entry.ManagementOperationExternal;
import org.tripcom.integration.entry.MetaDataKind;
import org.tripcom.integration.entry.MgmtDMEntry;
import org.tripcom.integration.entry.OutOperationExternal;
import org.tripcom.integration.entry.RdOperationExternal;
import org.tripcom.integration.entry.ErrorResultExternal;
import org.tripcom.integration.entry.RdTSAdapterEntry;
import org.tripcom.integration.entry.ReadType;
import org.tripcom.integration.entry.Result;
import org.tripcom.integration.entry.SecurityInfo;
import org.tripcom.integration.entry.SecurityCookieInfo;
import org.tripcom.integration.entry.SecurityAssertionsInfo;
import org.tripcom.integration.entry.SpaceURI;
import org.tripcom.integration.entry.StructuralMetadataCatalogueQueryEntry;
import org.tripcom.integration.entry.StructuralMetadataCatalogueResultEntry;
import org.tripcom.integration.entry.Template;
import org.tripcom.integration.entry.Timeout;
import org.tripcom.integration.entry.TripleEntry;
import org.tripcom.integration.entry.UnsubscribeRequest;
import org.tripcom.integration.entry.transaction.CreateTransactionExternal;
import org.tripcom.integration.entry.transaction.EndTransactionExternal;
import org.tripcom.integration.entry.transaction.EndTransactionResultExternal;
import org.tripcom.integration.entry.transaction.EndTransactionType;
import org.tripcom.integration.entry.transaction.GetTransactionExternal;
import org.tripcom.integration.entry.transaction.TransactionResultExternal;
import org.tripcom.integration.entry.transaction.TransactionType;
import org.tripcom.api.auxiliary.Pair;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;

/**
 * 
 * The "bottom" of the class hierarchy of the TS API
 * (see org.tripcom.api.ws.server.Tripcom for more info)
 * 
 * Eventually, all API operations call one of the methods of
 * this class in order to write an entry to the system bus
 * for another component
 * 
 */
public class Utils {

    private static Logger log = Logger.getLogger(Utils.class);

    private static final int MANAGEMENT_TIMEOUT = Integer.MAX_VALUE;

    public static boolean actAsDummy = false;

    /**
	 * This method is a for a general out. 
	 * 
	 * @param t
	 *            if t has size >1, it is an "extended" out.
	 * @param space
	 * @param timeout
	 *            how long do we wait for an error/data response?
	 *  @return cookie, if the SM send one
	 */
    public static String out(Set<Statement> t, java.net.URI space, long timeout, String certificate, String cookie, String[] assertions, URI transactionID, boolean synchrone, String signature) throws TSAPIException {
        long OpId = Controller.opIDadd();
        OutOperationExternal outOperationExternal = new OutOperationExternal();
        outOperationExternal.operationID = OpId;
        outOperationExternal.data = statementSetToData(t);
        outOperationExternal.synchronous = synchrone;
        try {
            if (t.size() == 0) {
                log.error("out: no statement");
                throw new java.lang.RuntimeException("out: no statement:");
            } else {
                outOperationExternal.data = statementSetToData(t);
            }
        } catch (Exception e) {
            log.error("out: " + e.toString());
            throw new java.lang.RuntimeException("out: could not parse Statements:" + e.toString());
        }
        outOperationExternal.securityInfo = createSecurityInfo(certificate, cookie, assertions, signature);
        outOperationExternal.space = new SpaceURI(space);
        if (log.isInfoEnabled()) log.info("Space for OUT is: " + space);
        outOperationExternal.timestamp = new Date().getTime();
        outOperationExternal.transactionID = transactionID;
        writeToBus(outOperationExternal);
        if ((certificate != null && cookie == null) || synchrone) {
            if (actAsDummy) return null;
            long confirmationTimeout = 2000;
            if (synchrone) confirmationTimeout = Long.MAX_VALUE;
            Result rdResultEntry = Controller.takeEntry(OpId, confirmationTimeout);
            if (rdResultEntry != null) {
                if (synchrone && rdResultEntry instanceof ErrorResultExternal) {
                    throw new TSAPIException("WS Server Erro (probably an authentication problem): ErrorResultExternal Description:" + ((ErrorResultExternal) rdResultEntry).getDescription());
                } else return getCookieByOPID(OpId);
            }
        }
        return getCookieByOPID(OpId);
    }

    /**
	 * write a rd operation to the bus in order to propagate
	 * it to the other components
	 * 
	 * @param query SPARQL query
	 * @param space space in the triple space (if any)
	 * @param timeout how long to wait for a response
	 * @param readType RD,RDMultiple,IN,INMultiple,...
	 * @param opId OperationID from Controller
	 * @param certificate X509 Certificate
	 * @param cookie Cookie from previous authentication
	 * @param assertions security assertions
	 * @param knownkernels
	 * @param transactionID
	 * @param signature
	 * @return
	 * @throws TSAPIException
	 */
    public static Pair<String, Set<Set<Statement>>> read(String query, java.net.URI space, int timeout, ReadType readType, long opId, String certificate, String cookie, String[] assertions, Set<URI> knownkernels, URI transactionID, String signature) throws TSAPIException {
        if (log.isInfoEnabled()) {
            log.info(readType + " from " + space + ", opId: " + opId + ", timeout: " + timeout);
        }
        int timeout_reduction = Math.max((timeout / 10), 2000);
        timeout = timeout - timeout_reduction;
        if (timeout <= 0) {
            log.warn("Timeout is too low. Operation will not be processed.");
            timeout = 0;
        }
        long startTime = System.currentTimeMillis();
        RdOperationExternal rdOperationExternal = new RdOperationExternal();
        Template template = generateTemplate(query);
        rdOperationExternal.operationID = opId;
        rdOperationExternal.kind = readType;
        log.info("RD TYPE: " + readType.toString());
        rdOperationExternal.query = template;
        if (space != null) rdOperationExternal.space = new SpaceURI(space); else rdOperationExternal.space = null;
        rdOperationExternal.timeout = new Timeout(timeout, new Date().getTime());
        rdOperationExternal.securityInfo = createSecurityInfo(certificate, cookie, assertions, signature);
        rdOperationExternal.visitedKernels = knownkernels;
        rdOperationExternal.transactionID = transactionID;
        writeToBus(rdOperationExternal);
        Set<Set<Statement>> resultSetSet = new HashSet<Set<Statement>>();
        Result rdResultEntry;
        if (!actAsDummy) rdResultEntry = Controller.takeEntry(opId, timeout); else {
            rdResultEntry = new DataResultExternal();
            Set<Set<TripleEntry>> fakeResultSet = new HashSet<Set<TripleEntry>>();
            Set<TripleEntry> fakeResult = new HashSet<TripleEntry>();
            fakeResult.add(createDummyTripleEntry(1));
            if ((readType == ReadType.INMULTIPLE) || readType == ReadType.READMULTIPLE) fakeResult.add(createDummyTripleEntry(2));
            fakeResultSet.add(fakeResult);
            ((DataResultExternal) rdResultEntry).result = fakeResultSet;
        }
        if (rdResultEntry instanceof ErrorResultExternal) {
            HashSet<Set<Statement>> result = new HashSet<Set<Statement>>();
            result.add(new HashSet<Statement>());
            String newcookie = getCookieByOPID(opId);
            throw new TSAPIException("WS Server Erro (probably an authentication problem): ErrorResultExternal Description: " + ((ErrorResultExternal) rdResultEntry).getDescription());
        }
        if (!(rdResultEntry instanceof DataResultExternal)) {
            HashSet<Set<Statement>> result = new HashSet<Set<Statement>>();
            result.add(new HashSet<Statement>());
            return new Pair<String, Set<Set<Statement>>>(getCookieByOPID(opId), result);
        }
        DataResultExternal dataResultExternal = (DataResultExternal) rdResultEntry;
        try {
            log.info("read got " + dataResultExternal.result.size() + "result sets");
            resultSetSet = dataToStatementSet(dataResultExternal.result);
        } catch (Exception e) {
            log.error("rd/in: something was wrong with the resultSet: " + e.toString());
            String newcookie = getCookieByOPID(opId);
            return new Pair<String, Set<Set<Statement>>>(newcookie, resultSetSet);
        }
        if (log.isInfoEnabled()) log.info("rd/in operation id: " + opId + " finished after " + (System.currentTimeMillis() - startTime) + " ms");
        if (log.isInfoEnabled()) if (resultSetSet.size() != 0) log.info("result size is " + resultSetSet.iterator().next().size()); else log.info("result size is 0");
        {
            ValueFactory myFactory = new ValueFactoryImpl();
            org.openrdf.model.URI mySubject = myFactory.createURI("tsc://tripcom.com:8080/rd");
            org.openrdf.model.URI myPredicate = myFactory.createURI("tsc://tripcom.com:8080/rd/recursive_read/completeness");
            org.openrdf.model.impl.LiteralImpl myObject;
            if (log.isInfoEnabled()) log.info("Field queryCompleted is set to " + dataResultExternal.queryCompleted);
            if (dataResultExternal.queryCompleted == null) myObject = (org.openrdf.model.impl.LiteralImpl) myFactory.createLiteral("null"); else if (dataResultExternal.queryCompleted) myObject = (org.openrdf.model.impl.LiteralImpl) myFactory.createLiteral("true"); else myObject = (org.openrdf.model.impl.LiteralImpl) myFactory.createLiteral("false");
            Statement st = myFactory.createStatement(mySubject, myPredicate, myObject);
            if (resultSetSet.size() != 0) resultSetSet.iterator().next().add(st);
        }
        return new Pair<String, Set<Set<Statement>>>(getCookieByOPID(opId), resultSetSet);
    }

    /**
	 * helper method, used to convert TripleEntries into
	 * openrdf Statements
	 * 
	 * @param result
	 * @return
	 */
    private static Set<Set<Statement>> dataToStatementSet(Set<Set<TripleEntry>> result) {
        Set<Set<Statement>> resultSetSet = new HashSet<Set<Statement>>();
        Iterator<Set<TripleEntry>> outerIt = result.iterator();
        while (outerIt.hasNext()) {
            Set<TripleEntry> tSet = outerIt.next();
            if (tSet == null) {
                continue;
            }
            Set<Statement> resultSet = new HashSet<Statement>();
            Iterator<TripleEntry> innerIt = tSet.iterator();
            while (innerIt.hasNext()) {
                TripleEntry tEntry = innerIt.next();
                if (tEntry == null) continue;
                Resource s = tEntry.getSubject();
                org.openrdf.model.URI p = tEntry.getPredicate();
                Value o = tEntry.getObject();
                Statement resultStatement = new StatementImpl(s, p, o);
                resultSet.add(resultStatement);
            }
            resultSetSet.add(resultSet);
        }
        return resultSetSet;
    }

    /**
	 * helper method, used to convert 
	 * openrdf Statements into TripleEntries 
	 * 
	 * @param result
	 * @return
	 */
    private static Set<TripleEntry> statementSetToData(Set<Statement> tSet) {
        Set<TripleEntry> tripleSet = new HashSet<TripleEntry>();
        Iterator<Statement> statementIt = tSet.iterator();
        while (statementIt.hasNext()) {
            Statement t = statementIt.next();
            TripleEntry tEntry = new TripleEntry();
            tEntry.setSubject(t.getSubject());
            tEntry.setPredicate(t.getPredicate());
            tEntry.setObject(t.getObject());
            tripleSet.add(tEntry);
        }
        return tripleSet;
    }

    /**
	 * Writes entries to the system bus
	 * 
	 */
    private static void writeToBus(Entry entry) {
        if (actAsDummy) return;
        try {
            JavaSpace systemBus = (JavaSpace) BusConnectionManager.getSharedInstance().getBus(SystemBusArea.INCOMING);
            systemBus.write(entry, null, Lease.FOREVER);
        } catch (Exception e) {
            log.error("LindaReadWithSpaceThread: Could not write to Systen BUS " + e.toString(), e);
        }
    }

    /**
	 * generate a template from a query
	 * 
	 * @param query query
	 * @return template
	 */
    private static Template generateTemplate(String query) {
        Template template = new Template();
        template.setQuery(query);
        return template;
    }

    /**
	 * writes an appropriate entry for a  management API operation
	 * to the system bus
	 * 
	 * @param parameters operation dependant list of parameters
	 * @param op operation type
	 * @param certificate client's X509 certificate
	 * @param cookie SM cookie from previous authentication
	 * @param assertions security assertions
	 * @param signature client's XML signature for operation
	 * @return
	 * @throws TSAPIException
	 */
    public static Pair<String, ManagementDataResultExternal> readManagement(List<Object> parameters, ManagementOperation op, String certificate, String cookie, String[] assertions, String signature) throws TSAPIException {
        if (log.isInfoEnabled()) {
            log.info("Utils: readManagement: Operation is " + op);
            log.info("parameter list size is " + parameters.size());
        }
        long opid = Controller.opIDadd();
        ManagementOperationExternal operation = new ManagementOperationExternal(opid, op, parameters, createSecurityInfo(certificate, cookie, assertions, signature));
        writeToBus(operation);
        Result result;
        if (!actAsDummy) result = (Result) Controller.takeEntry(operation.operationID, MANAGEMENT_TIMEOUT); else {
            result = new ManagementDataResultExternal();
            if (op == ManagementOperation.Create || op == ManagementOperation.CreateRemote) {
                List<Object> objl = new LinkedList<Object>();
                try {
                    objl.add(new java.net.URI("tsc://tripcom.com:8080/fakeuri"));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                ((ManagementDataResultExternal) result).result = objl;
            } else if (op == ManagementOperation.GET_POLICY) {
                Set<TripleEntry> objl = new HashSet<TripleEntry>();
                objl.add(createDummyTripleEntry(1));
                ((ManagementDataResultExternal) result).result = new LinkedList<Object>();
                ((ManagementDataResultExternal) result).result.add(objl);
            }
        }
        if (log.isInfoEnabled()) log.info("result with operationID : " + result.operationID + " taken from ManagementAPI.");
        if (result instanceof ManagementDataResultExternal) {
            log.info("Management Result taken");
            return new Pair<String, ManagementDataResultExternal>(getCookieByOPID(opid), (ManagementDataResultExternal) result);
        } else {
            log.info("Answer has wrong result type: " + result.getClass());
            if (result instanceof ErrorResultExternal) {
                ErrorResultExternal er = (ErrorResultExternal) result;
                log.info("error : " + er.getError() + ",description:" + er.getDescription());
                throw new TSAPIException(er.getDescription());
            }
            result = new ManagementDataResultExternal();
            ((ManagementDataResultExternal) result).result = null;
            return new Pair<String, ManagementDataResultExternal>(getCookieByOPID(opid), new ManagementDataResultExternal());
        }
    }

    /**
	 * Performs a subscription to the triple space by
	 * informing the TSAdapter via an appropriate entry
	 * on the system bus 
	 * 
	 * note that the registering of the subscription with the
	 * API's subscription Thread happens in the extended API
	 * 
	 * @param query query for subscription
	 * @param space space for subscription
	 * @param opID Controller's operation ID
	 * @param certificate client's X509 certificate
	 * @param cookie SM's cookie from previous authentication
	 * @param assertions security assertions
	 * @param signature client's XML signature for operation
	 * @return
	 */
    public static URI subscribe(String query, URI space, long opID, String certificate, String cookie, String[] assertions, String signature) {
        RdTSAdapterEntry entry = new RdTSAdapterEntry();
        entry.operationID = opID;
        entry.space = new SpaceURI(space);
        entry.query = query;
        entry.kind = ReadType.NOTIFY;
        if (log.isInfoEnabled()) log.info("subscription request recieved");
        writeToBus(entry);
        try {
            return new URI(space.toString() + "/" + opID);
        } catch (Exception e) {
            log.error("invalid uri for subscription:" + e.getMessage());
            return null;
        }
    }

    /**
	 * inform TSAdapter about the removal of a subscription
	 * 
	 * @param subscription ID of the subscription
	 * @param signature client's XML signature
	 */
    public static void unsubscribe(URI subscription, String signature) {
        if (log.isInfoEnabled()) log.info("unsubscription with uri : " + subscription);
        Long opId = Long.parseLong(subscription.toString().substring(subscription.toString().lastIndexOf('/') + 1));
        writeToBus(new UnsubscribeRequest(opId));
        SubscriptionThread.unsubscribe(opId);
    }

    /**
	 * retrieve new statements for a subscription
	 * blocks, if none are found
	 * 
	 * @param opId operation id identifying subscription
	 * @return
	 */
    public static Set<Statement> callSubscriptions(long opId) {
        log.info("Call subscriptions");
        DataResultExternal data = (DataResultExternal) Controller.takeEntry(opId, Long.MAX_VALUE);
        Set<Statement> result = new HashSet<Statement>();
        for (Iterator<Set<Statement>> i = dataToStatementSet(data.result).iterator(); i.hasNext(); ) {
            for (Iterator<Statement> j = i.next().iterator(); j.hasNext(); ) result.add(j.next());
        }
        log.info("[Utils/Subscription]: got " + result.size() + " statements for opID " + opId);
        return result;
    }

    /**
	 * send the MetadataManager an entry requesting the space catalogue for
	 * a given space
	 * 
	 * @param space space 
	 * @param signature client's XML signature
	 * @return catalogue for space
	 */
    public static Map<URI, Map<MetaDataKind, Set<URI>>> getCatalogue(URI space, String signature) throws TSAPIException {
        long opId = Controller.opIDadd();
        log.info("Operation ID for getCatalogue is " + opId);
        SpaceURI uri = null;
        if (space != null) uri = new SpaceURI(space);
        StructuralMetadataCatalogueQueryEntry request = new StructuralMetadataCatalogueQueryEntry(opId, uri);
        writeToBus(request);
        log.info("waiting for result for structural metadata request");
        Result result;
        if (!actAsDummy) {
            result = Controller.takeEntry(opId, Long.MAX_VALUE);
        } else {
            result = new StructuralMetadataCatalogueResultEntry();
            Set<URI> uriset = new HashSet<URI>();
            try {
                uriset.add(new URI("tsc://tripcom.com:8080/fakeuri"));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            Map<MetaDataKind, Set<URI>> map = new HashMap<MetaDataKind, Set<URI>>();
            map.put(MetaDataKind.SEEALSO, uriset);
            Map<URI, Map<MetaDataKind, Set<URI>>> catalogue = new HashMap<URI, Map<MetaDataKind, Set<URI>>>();
            try {
                catalogue.put(new URI("tsc://tripcom.com:8080/fakeuri"), map);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            ((StructuralMetadataCatalogueResultEntry) result).catalogue = catalogue;
        }
        if (result instanceof StructuralMetadataCatalogueResultEntry) {
            log.info("got valid result");
            return ((StructuralMetadataCatalogueResultEntry) result).catalogue;
        } else if (result instanceof ErrorResultExternal) {
            log.info("got errorresultexternal - throwing exception:");
            log.info("|" + ((ErrorResultExternal) result).getDescription() + "|");
            throw new TSAPIException(((ErrorResultExternal) result).getDescription());
        }
        {
            log.info("expected answer was of class " + result.getClass() + ", not tructuralMetadataCatalogueResultEntry");
            return null;
        }
    }

    /**
	 * 
	 * helper method, assembles a SecurityInfo Object
	 * from the various parameters
	 * 
	 * @param certificate
	 * @param cookie
	 * @param assertions
	 * @param signature
	 * @return
	 * @throws TSAPIException
	 */
    private static SecurityInfo createSecurityInfo(String certificate, String cookie, String[] assertions, String signature) throws TSAPIException {
        SecurityInfo ret;
        if (certificate == null) {
            ret = null;
        } else if (cookie != null) {
            ret = new SecurityCookieInfo(certificate, cookie);
        } else if (assertions != null) {
            ret = new SecurityAssertionsInfo(certificate, new HashSet<String>(Arrays.asList(assertions)));
        } else {
            ret = new SecurityInfo(certificate);
        }
        if (ret != null && signature != null) {
            if (signature.substring(0, 7).equalsIgnoreCase("DMFLAG=1")) ret.setDMFromKernelFlag(true); else if (signature.substring(0, 7).equalsIgnoreCase("DMFLAG=0")) ret.setDMFromKernelFlag(false); else throw new TSAPIException("Encoding of DM Flag in signature Invalid. Expected: DMFLAG=1 or DMFLAG=0, got: " + signature.substring(0, 7));
            ret.setXMLSignature(signature.substring(8));
        }
        return ret;
    }

    /**
	 * 
	 * Write an entry for the Transaction Mananger in order
	 * to create a transaction ID
	 * 
	 * @param type
	 * @param signature
	 * @return
	 * @throws TSAPIException
	 */
    public static URI createTransaction(String type, String signature) throws TSAPIException {
        long opId = Controller.opIDadd();
        CreateTransactionExternal entry = new CreateTransactionExternal();
        entry.operationID = opId;
        entry.type = TransactionType.valueOf(type);
        entry.timeout = new Long(1000);
        writeToBus(entry);
        log.info("written begin transaction entry, waiting for result.");
        Result result;
        if (!actAsDummy) result = Controller.takeEntry(opId, Long.MAX_VALUE); else {
            result = new TransactionResultExternal();
            try {
                ((TransactionResultExternal) result).transactionID = new URI("tsc://tripcom.com:8080/somefaketransactionid");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (!(result instanceof TransactionResultExternal)) throw new TSAPIException("Got wrong answer for CreateTransactionExternal entry: " + result.getClass());
        URI uri = ((TransactionResultExternal) result).transactionID;
        return uri;
    }

    /**
	 * 
	 * Write an entry for the Transaction Mananger in order
	 * to get the current transaction ID
	 * 
	 * 
	 * @param transactionID
	 * @param signature
	 * @return
	 * @throws TSAPIException
	 */
    public static URI getTransaction(URI transactionID, String signature) throws TSAPIException {
        long opId = Controller.opIDadd();
        GetTransactionExternal entry = new GetTransactionExternal();
        entry.operationID = opId;
        entry.transactionID = transactionID;
        writeToBus(entry);
        log.info("written get transaction entry, waiting for result.");
        Result result;
        if (!actAsDummy) result = Controller.takeEntry(opId, Long.MAX_VALUE); else {
            result = new TransactionResultExternal();
            try {
                ((TransactionResultExternal) result).transactionID = new URI("tsc://tripcom.com:8080/somefaketransactionid");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (!(result instanceof TransactionResultExternal)) throw new TSAPIException("Got wrong answer for CreateTransactionExternal entry: " + result.getClass());
        URI uri = ((TransactionResultExternal) result).transactionID;
        return uri;
    }

    /**
	 * 
	 * Write an entry for the Transaction Mananger in order
	 * to get the commit the current transaction 
	 * 
	 * 
	 * @param transactionID
	 * @param signature
	 * @return
	 * @throws TSAPIException
	 */
    public static URI commitTransaction(URI transactionID, String signature) throws TSAPIException {
        long opId = Controller.opIDadd();
        EndTransactionExternal entry = new EndTransactionExternal();
        entry.operationID = opId;
        entry.transactionID = transactionID;
        entry.type = EndTransactionType.COMMIT;
        writeToBus(entry);
        log.info("written end/COMMIT transaction entry, waiting for result.");
        Result result;
        if (!actAsDummy) result = Controller.takeEntry(opId, Long.MAX_VALUE); else {
            result = new TransactionResultExternal();
            try {
                ((TransactionResultExternal) result).transactionID = new URI("tsc://tripcom.com:8080/somefaketransactionid");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (!(result instanceof EndTransactionResultExternal)) throw new TSAPIException("Got wrong answer for EndTransactionExternal entry: " + result.getClass());
        return transactionID;
    }

    /**
	 * 
	 * Write an entry for the Transaction Mananger in order
	 * to get the rollback the current transaction 
	 * 
	 * 
	 * @param transactionID
	 * @param signature
	 * @return
	 * @throws TSAPIException
	 */
    public static URI rollbackTransaction(URI transactionID, String signature) throws TSAPIException {
        long opId = Controller.opIDadd();
        EndTransactionExternal entry = new EndTransactionExternal();
        entry.operationID = opId;
        entry.transactionID = transactionID;
        entry.type = EndTransactionType.COMMIT;
        writeToBus(entry);
        log.info("written end/COMMIT transaction entry, waiting for result.");
        Result result;
        if (!actAsDummy) result = Controller.takeEntry(opId, Long.MAX_VALUE); else {
            result = new TransactionResultExternal();
            try {
                ((TransactionResultExternal) result).transactionID = new URI("tsc://tripcom.com:8080/somefaketransactionid");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        if (!(result instanceof EndTransactionResultExternal)) throw new TSAPIException("Got wrong answer for EndTransactionExternal entry: " + result.getClass());
        return transactionID;
    }

    /**
	 * 
	 * return a cookie the SM send for a operation, if the
	 * Controller received one
	 * 
	 * @param opid
	 * @return
	 */
    public static String getCookieByOPID(long opid) {
        return Controller.cookieByOpID(opid);
    }

    /**
	 * 
	 * write an entry to the MM for a lookup operation
	 * 
	 * @param searchedSpace
	 * @param deepestAncestorSpaceFoundSoFar
	 * @param signature
	 * @return
	 */
    public static Map<SpaceURI, KernelAddress> lookup(SpaceURI searchedSpace, SpaceURI deepestAncestorSpaceFoundSoFar, String signature) {
        long OpID = Controller.opIDadd();
        List<Object> parameters = new LinkedList<Object>();
        parameters.add(searchedSpace);
        parameters.add(deepestAncestorSpaceFoundSoFar);
        MgmtDMEntry request = new MgmtDMEntry(ManagementOperation.LookUp, searchedSpace, null, OpID, null, parameters);
        if (!actAsDummy) writeToBus(request); else {
            Map<SpaceURI, KernelAddress> fakemap = new HashMap<SpaceURI, KernelAddress>();
            SpaceURI fakeuri = new SpaceURI("tsc://tripcom.com:8080/fakeuri");
            fakemap.put(fakeuri, fakeuri.getKernelAddressOfRootSpace());
            return fakemap;
        }
        log.info("waiting for lookup result");
        Result result = Controller.takeEntry(OpID, Long.MAX_VALUE);
        ;
        log.info("got lookup result");
        if (!(result instanceof ManagementDataResultExternal)) {
            log.info("lookup Error - result has wrong entry type: " + result.getClass());
            return null;
        }
        ManagementDataResultExternal mresult = (ManagementDataResultExternal) result;
        List<Object> r = mresult.result;
        Map<SpaceURI, KernelAddress> map = (Map<SpaceURI, KernelAddress>) r.get(0);
        return map;
    }

    /**
	 * helper method for dummy component testing, creates
	 * some triples
	 * 
	 * @param i
	 * @return
	 */
    private static TripleEntry createDummyTripleEntry(int i) {
        ValueFactory myFactory = new ValueFactoryImpl();
        org.openrdf.model.URI mySubject = myFactory.createURI("tsc://tripcom.com:8080/dummytriplesubject" + i);
        org.openrdf.model.URI myPredicate = myFactory.createURI("tsc://tripcom.com:8080/dummytriplepredicate" + i);
        Value myObject = myFactory.createLiteral("somevalue");
        TripleEntry t = new TripleEntry(mySubject, myPredicate, myObject);
        return t;
    }

    /**
	 * write an entry to the ts adapter to get the current
	 * kernel load
	 * 
	 * @return kernel load
	 * @throws TSAPIException
	 */
    public static int getKernelLoad() throws TSAPIException {
        long OpID = Controller.opIDadd();
        LoadQueryTSAdapterEntry entry = new LoadQueryTSAdapterEntry(Component.API, OpID);
        writeToBus(entry);
        log.info("waiting for load query result");
        Result result = Controller.takeEntry(OpID, Long.MAX_VALUE);
        ;
        log.info("got load query result");
        if (!(result instanceof LoadResultEntry)) {
            log.error("load query result has wrong result type:" + result.getClass());
            throw new TSAPIException("load query result has wrong result type:" + result.getClass());
        }
        return ((LoadResultEntry) result).load;
    }
}

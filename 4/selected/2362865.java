package com.fujitsu.arcon.njs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.unicore.AJOIdentifier;
import org.unicore.PortfolioIdentifier;
import org.unicore.Ulogin;
import org.unicore.UserAttributes;
import org.unicore.Vsite;
import org.unicore.ajo.AbstractAction;
import org.unicore.ajo.AbstractJob;
import org.unicore.ajo.BookResources;
import org.unicore.ajo.CancelResourceBooking;
import org.unicore.ajo.QueryResourceBooking;
import org.unicore.outcome.AbstractActionStatus;
import org.unicore.outcome.AbstractJob_Outcome;
import org.unicore.outcome.BookResources_Outcome;
import org.unicore.outcome.CancelResourceBooking_Outcome;
import org.unicore.outcome.Outcome;
import org.unicore.outcome.QueryResourceBooking_Outcome;
import org.unicore.resources.AlternativeUspace;
import org.unicore.resources.Home;
import org.unicore.resources.ResourceBooking;
import org.unicore.resources.Root;
import org.unicore.resources.StorageServer;
import org.unicore.resources.Temp;
import org.unicore.sets.ResourceSet;
import org.unicore.upl.ConsignJob;
import org.unicore.upl.ListVsites;
import org.unicore.upl.ListVsitesReply;
import org.unicore.utility.AbstractActionIterator;
import org.unicore.utility.ConsignForm;
import com.fujitsu.arcon.njs.actions.KnownAction;
import com.fujitsu.arcon.njs.actions.MappedStorage;
import com.fujitsu.arcon.njs.actions.XKnownAction;
import com.fujitsu.arcon.njs.interfaces.AJORewriter;
import com.fujitsu.arcon.njs.interfaces.AJORewriterException;
import com.fujitsu.arcon.njs.interfaces.AltFT;
import com.fujitsu.arcon.njs.interfaces.ConnectionFailedException;
import com.fujitsu.arcon.njs.interfaces.GAS;
import com.fujitsu.arcon.njs.interfaces.IncarnatedUlogin;
import com.fujitsu.arcon.njs.interfaces.IncarnatedUser;
import com.fujitsu.arcon.njs.interfaces.Mapping;
import com.fujitsu.arcon.njs.interfaces.MappingFailedException;
import com.fujitsu.arcon.njs.interfaces.NJSException;
import com.fujitsu.arcon.njs.interfaces.ResChecker;
import com.fujitsu.arcon.njs.interfaces.ResourceCheckerException;
import com.fujitsu.arcon.njs.interfaces.ResourceReservationExecution;
import com.fujitsu.arcon.njs.interfaces.ResourceReservationService;
import com.fujitsu.arcon.njs.interfaces.Role;
import com.fujitsu.arcon.njs.interfaces.TSIConnection;
import com.fujitsu.arcon.njs.logger.LoggerManager;
import com.fujitsu.arcon.njs.priest.GeneralData;
import com.fujitsu.arcon.njs.priest.TargetSystem;
import com.fujitsu.arcon.njs.priest.Task;
import com.fujitsu.arcon.njs.priest.UspaceManager;
import com.fujitsu.arcon.njs.reception.GatewayConnectionFactory;
import com.fujitsu.arcon.njs.reception.RemoteNJS;
import com.fujitsu.arcon.njs.reception.SSLCredentials;
import com.fujitsu.arcon.njs.reception.UPLConnection;
import com.fujitsu.arcon.upl.ListPorts;
import com.fujitsu.arcon.upl.ListPortsReply;

/**
 * 
 * Implementations of the services that the NJS provides to plugins/extensions.
 * 
 * @see com.ujitsu.arcon.njs.interfaces.NJSServices
 * 
 * @author Sven van den Berghe, fujitsu 
 *
 * @version $Revision: 1.4 $ $Date: 2006/03/10 21:35:51 $
 *
 **/
public class NJSServicesImpl implements com.fujitsu.arcon.njs.interfaces.NJSServices {

    protected com.fujitsu.arcon.njs.logger.Logger logger;

    public NJSServicesImpl() {
        logger = LoggerManager.get("NJS");
    }

    public synchronized TSIConnection getTSIConnection(IncarnatedUser user) throws com.fujitsu.arcon.njs.interfaces.TSIUnavailableException {
        if (logger.CHAT) logger.chat("Request from NJSService for a TSI connection " + (user != null ? " for <" + user.getXlogin() + "/" + user.getProject() + ">" : ""));
        try {
            return TargetSystem.getTargetSystem().getTSIConnectionFactory().getTSIConnection(user);
        } catch (NJSException nex) {
            logger.warning("Refused request from NJSService for a TSI conection because: " + nex.getMessage());
            return null;
        }
    }

    public java.net.Socket getConnection(String machine, int port) throws ConnectionFailedException {
        if (logger.CHAT) logger.chat("NJSService request for a connection to <" + machine + ":" + port + ">");
        try {
            UPLConnection.Factory gcf = RemoteNJS.getConnectionFactory(new Vsite("ssl://" + machine + ":" + port, "NJSService"));
            return gcf.getRawSocket();
        } catch (Exception ex) {
            logger.warning("Failed NJSService request for a connection to <" + machine + ":" + port + ">", ex);
            throw new ConnectionFailedException("Failed NJSService request for a connection to <" + machine + ":" + port + "> " + ex.getMessage(), ex);
        }
    }

    public org.unicore.sets.ResourceSet getCurrentResources() {
        return com.fujitsu.arcon.njs.actions.DoGetResources.getCurrentResources();
    }

    protected AbstractJob_Outcome _consignJob(AbstractJob ajo) throws NJSException {
        if (ajo.getStreamed() != null && ajo.getStreamed().elements().hasMoreElements()) {
            throw new NJSException("Cannot stream Portfolios with this AJO");
        }
        SSLCredentials signer;
        UPLConnection connection = null;
        try {
            GatewayConnectionFactory gcf = (GatewayConnectionFactory) RemoteNJS.getConnectionFactory(ajo.getVsite());
            signer = gcf.getCreds();
            connection = (UPLConnection) gcf.connect();
        } catch (Exception ex) {
            throw new NJSException("Could not contact remote Vsite: " + ex.getMessage(), ex);
        }
        AbstractJob_Outcome ajoo;
        try {
            Ulogin endorser = new Ulogin(signer.getUlogin().getCertificateChain());
            AbstractActionIterator aai = new AbstractActionIterator(ajo);
            while (aai.hasNext()) {
                AbstractAction aa = aai.next();
                if (aa instanceof AbstractJob) {
                    AbstractJob candidate = (AbstractJob) aa;
                    if (candidate.getConsignForm() == null || candidate.getSignature() == null) {
                        candidate.setAJOEndorser(endorser);
                    }
                }
            }
            ConsignJob cj = new ConsignJob();
            AbstractJob ajo_c = null;
            synchronized (signer) {
                ajo_c = org.unicore.utility.ConsignForm.convertTo(ajo, signer);
            }
            cj.setAJO(ajo_c.getConsignForm());
            cj.setAJOEndorser(endorser);
            cj.setPolling(false);
            cj.setSignature(ajo_c.getSignature());
            cj.setStreamed(false);
            cj.setVsite(ajo.getVsite());
            cj.setTarget(ajo.getAJOId());
            ajoo = (AbstractJob_Outcome) (new ObjectInputStream(new ByteArrayInputStream(connection.consignSynchronousJob(cj).getOutcome()))).readObject();
            ajoo = ConsignForm.convertFrom(ajoo);
        } catch (IOException ex) {
            connection.closeError();
            throw new NJSException("IO Error with AJO execution: " + ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            connection.closeOK();
            throw new NJSException("CC Error in AJO execution: " + ex.getMessage(), ex);
        } catch (NJSException ex) {
            connection.closeOK();
            throw new NJSException("Error in AJO execution: " + ex.getMessage(), ex);
        } catch (SignatureException ex) {
            connection.closeOK();
            throw new NJSException("Error in AJO execution preparation: " + ex.getMessage(), ex);
        } catch (CertificateException ex) {
            connection.closeOK();
            throw new NJSException("Error in AJO execution preparation: " + ex.getMessage(), ex);
        }
        connection.closeOK();
        return ajoo;
    }

    protected Outcome _consignAction(AbstractAction action, UserAttributes user, Vsite vsite) throws NJSException {
        AbstractJob ajo = new AbstractJob("Created by NJSServices at <" + NJSGlobals.getVsite() + ">");
        ajo.setUserAttributes(user);
        ajo.setVsite(vsite);
        ajo.add(action);
        AbstractJob_Outcome ajoo = _consignJob(ajo);
        Outcome o = ajoo.getOutcome(action.getId());
        if (o.getStatus().isEquivalent(AbstractActionStatus.NEVER_RUN)) {
            throw new NJSException("Wrapper AJO failed <" + ajoo.getStatus() + "<" + ajoo.getReason() + ">");
        }
        return o;
    }

    public static class AFTImpl extends NJSServicesImpl implements AltFT.NJS {

        public static class Factory implements AltFT.AFTFactory {

            private AltFT.AFTFactory[] factories;

            public void init(AltFT.NJS njs) {
                com.fujitsu.arcon.njs.logger.Logger logger = LoggerManager.get("NJS");
                String factory_classes = System.getProperty("aft.factories");
                Set instances = new HashSet();
                if (factory_classes != null) {
                    StringTokenizer st = new StringTokenizer(factory_classes, ":");
                    while (st.hasMoreTokens()) {
                        String class_name = st.nextToken().trim();
                        try {
                            AltFT.AFTFactory nf = (AltFT.AFTFactory) Class.forName(class_name).newInstance();
                            nf.init(njs);
                            instances.add(nf);
                            logger.config("External class used for Alternative File Transfer Factory <" + class_name + ">");
                        } catch (ClassCastException ccex) {
                        } catch (ClassNotFoundException ex) {
                            logger.severe("Alternative file transfer factory class <" + class_name + "> could not be found. \n" + "CLASSPATH OK? The class name fully qualified?");
                            NJSGlobals.goToLimbo();
                        } catch (Exception ex) {
                            logger.severe("Problems creating an instance of <" + class_name + ">", ex);
                            NJSGlobals.goToLimbo();
                        }
                    }
                }
                if (instances.size() > 0) factories = (AltFT.AFTFactory[]) instances.toArray(new AltFT.AFTFactory[0]);
            }

            public AltFT.AlternativeFileTransfer get(Vsite vsite) {
                if (factories == null) return null;
                for (int i = 0; i < factories.length; i++) {
                    AltFT.AlternativeFileTransfer temp = factories[i].get(vsite);
                    if (temp != null) return temp;
                }
                return null;
            }
        }

        public Mapping notifyTransfer(AJOIdentifier uspace_id, PortfolioIdentifier portfolio, UserAttributes user_attributes) throws MappingFailedException {
            if (logger.CHAT) logger.chat("AFT notifyTransfer <" + user_attributes.getUser().getCertificate().getSubjectDN().getName() + ">");
            IncarnatedUser user;
            try {
                user = Authoriser.authoriseConsignJob(user_attributes, user_attributes.getUser(), user_attributes.getUser());
            } catch (Exception ex) {
                throw new MappingFailedException(ex.getMessage());
            }
            UspaceManager.Uspace uspace;
            try {
                uspace = NJSGlobals.getUspaceManager().makePSUspace(uspace_id, user_attributes, user_attributes.getUser(), user);
            } catch (Exception ex) {
                throw new MappingFailedException(ex.getMessage());
            }
            try {
                String hdn = com.fujitsu.arcon.njs.actions.IncarnatedPortfolio.createHidden(portfolio, uspace).getHiddenDirName();
                String reply = TargetSystem.getTargetSystem().doCommand(Task.Priest.MKDIR + hdn + "\n" + GeneralData.Priest.afterCommand("Temp dir make failed") + "echo \"SSTTAARRTT_PPWWDD\" \n" + "echo " + hdn + " \n" + "echo \"EENNDD_PPWWDD\" \n", user, uspace);
                int start = reply.indexOf("SSTTAARRTT_PPWWDD");
                int end = reply.indexOf("EENNDD_PPWWDD");
                String expanded_hdn = reply.substring(start + "SSTTAARRTT_PPWWDD\n".length(), end - 1);
                return new MappingImpl(user, expanded_hdn);
            } catch (Exception ex) {
                throw new MappingFailedException(ex.getMessage());
            }
        }

        public Mapping notifyTransfer(org.unicore.resources.PathedStorage storage, UserAttributes user_attributes) throws MappingFailedException {
            if (logger.CHAT) logger.chat("AFT map <" + user_attributes.getUser().getCertificate().getSubjectDN().getName() + ">");
            IncarnatedUser user;
            try {
                user = Authoriser.authoriseConsignJob(user_attributes, user_attributes.getUser(), user_attributes.getUser());
            } catch (Exception ex) {
                throw new MappingFailedException(ex.getMessage());
            }
            MappedStorage mstorage;
            if (storage instanceof AlternativeUspace) {
                try {
                    NJSGlobals.getUspaceManager().makePSUspace(((AlternativeUspace) storage).getUspace(), user_attributes, user_attributes.getUser(), user);
                } catch (Exception e) {
                    throw (new MappingFailedException("Creating AlternativeUspace: " + e.getMessage()));
                }
                mstorage = new MappedStorage.AlternativeUspace((AlternativeUspace) storage, null);
            } else if (storage instanceof Home) {
                mstorage = new MappedStorage.Storage(storage);
            } else if (storage instanceof Root) {
                mstorage = new MappedStorage.Storage(storage);
            } else if (storage instanceof StorageServer) {
                mstorage = new MappedStorage.Storage(storage);
            } else if (storage instanceof Temp) {
                mstorage = new MappedStorage.Storage(storage);
            } else {
                throw new MappingFailedException("Storage instance is not suitable for single file transfer (Uspace or null) <" + storage + ">");
            }
            try {
                TargetSystem.getTargetSystem().incarnateMappedStorage(mstorage);
                return new MappingImpl(user, mstorage.getLocation());
            } catch (NJSException nex) {
                throw new MappingFailedException(nex.getMessage());
            }
        }

        private class MappingImpl implements Mapping {

            private String directory;

            private IncarnatedUser iu;

            public MappingImpl(IncarnatedUser iu, String directory) {
                this.iu = iu;
                this.directory = directory;
            }

            public String getDirectory() {
                return directory;
            }

            public String getXlogin() {
                return iu.getXlogin();
            }

            public String getProject() {
                return iu.getProject();
            }

            public String getName() {
                return iu.getName();
            }

            public Role getRole() {
                return iu.getRole();
            }

            public boolean sameUlogin(IncarnatedUlogin ulogin) {
                return iu.sameUlogin(ulogin);
            }

            public boolean sameUser(IncarnatedUser user) {
                return iu.sameUser(user);
            }
        }
    }

    public static class ResourceCheckerImpl extends NJSServicesImpl implements ResChecker.NJS {

        private static KnownActionDB kadb;

        public static void init(KnownActionDB in_kadb) {
            kadb = in_kadb;
        }

        public ResourceCheckerImpl() {
        }

        public AbstractJob_Outcome consignJob(AbstractJob ajo) throws ResourceCheckerException {
            try {
                return _consignJob(ajo);
            } catch (NJSException nex) {
                throw new ResourceCheckerException(nex.getMessage(), nex);
            }
        }

        public void consignJob(AbstractJob ajo, ResChecker.JobCompletedListener listener) throws ResourceCheckerException {
            if (ajo.getStreamed() != null && ajo.getStreamed().elements().hasMoreElements()) {
                throw new ResourceCheckerException("Cannot stream Portfolios with this AJO");
            }
            GatewayConnectionFactory gcf;
            try {
                gcf = (GatewayConnectionFactory) RemoteNJS.getConnectionFactory(ajo.getVsite());
            } catch (Exception ex) {
                throw new ResourceCheckerException("Could not contact remote Vsite: " + ex.getMessage(), ex);
            }
            UserAttributes njs_as_user = new UserAttributes();
            AbstractJob ajo_c;
            try {
                njs_as_user.setUser(new Ulogin(gcf.getCreds().getUlogin().getCertificateChain()));
                synchronized (gcf.getCreds()) {
                    ajo_c = org.unicore.utility.ConsignForm.convertTo(ajo, gcf.getCreds());
                }
                ajo_c.setAJOEndorser(njs_as_user.getUser());
            } catch (Exception ex) {
                throw new ResourceCheckerException("Error in AJO execution, preparation: " + ex.getMessage(), ex);
            }
            Ulogin endorser = njs_as_user.getUser();
            AbstractActionIterator aai = new AbstractActionIterator(ajo);
            while (aai.hasNext()) {
                AbstractAction aa = aai.next();
                if (aa instanceof AbstractJob) {
                    AbstractJob candidate = (AbstractJob) aa;
                    if (candidate.getConsignForm() == null || candidate.getSignature() == null) {
                        candidate.setAJOEndorser(endorser);
                    }
                }
            }
            com.fujitsu.arcon.njs.interfaces.IncarnatedUser inced_user;
            try {
                inced_user = Authoriser.authoriseConsignJob(njs_as_user, njs_as_user.getUser(), njs_as_user.getUser());
            } catch (Exception ex) {
                throw new ResourceCheckerException("Could not create a user for this NJS: " + ex);
            }
            UspaceManager.Uspace dummy_uspace = new UspaceManager.Uspace(null, njs_as_user, njs_as_user.getUser(), inced_user);
            com.fujitsu.arcon.njs.priest.DummyRootAJO dummy_root = new com.fujitsu.arcon.njs.priest.DummyRootAJO((org.unicore.AJOIdentifier) null, inced_user, dummy_uspace, (com.fujitsu.arcon.njs.priest.OutcomeStore) null);
            com.fujitsu.arcon.njs.actions.ParentAction parent = new _ParentAction(listener, kadb);
            com.fujitsu.arcon.njs.actions.XKnownAction xaction = new _XKnownAction(ajo_c, null, parent, dummy_root, kadb);
            try {
                kadb.add(xaction);
                xaction.setStatus(AbstractActionStatus.READY);
            } catch (Exception ex) {
                throw new ResourceCheckerException("Problems starting broker AJO execution: " + ex);
            }
        }

        class _ParentAction implements com.fujitsu.arcon.njs.actions.ParentAction {

            private ResChecker.JobCompletedListener listener;

            private com.fujitsu.arcon.njs.KnownActionDB kadb;

            public _ParentAction(ResChecker.JobCompletedListener listener, com.fujitsu.arcon.njs.KnownActionDB kadb) {
                this.listener = listener;
                this.kadb = kadb;
            }

            public void childDone(com.fujitsu.arcon.njs.actions.KnownAction child) {
                ((com.fujitsu.arcon.njs.actions.XKnownAction) child).doROA();
                kadb.remove(child.getId());
                listener.jobDone((AbstractJob_Outcome) child.getOutcome());
            }

            public String getName() {
                return "Dummy parent for Broker AJOs";
            }

            public com.fujitsu.arcon.njs.priest.OutcomeStore getOutcomeStore() {
                return null;
            }

            public String getIterationString() {
                return "You should not see this";
            }
        }

        class _XKnownAction extends com.fujitsu.arcon.njs.actions.XKnownAction {

            public _XKnownAction(AbstractAction a, Outcome o, com.fujitsu.arcon.njs.actions.ParentAction p, com.fujitsu.arcon.njs.actions.RootAJO r, KnownActionDB k) {
                super(a, o, p, r, k);
                setAAType("BKRAJO");
            }

            public boolean requiresUspace() {
                return false;
            }

            public boolean isInteresting() {
                return false;
            }
        }

        public String getStorageLocation(org.unicore.resources.Storage storage) {
            if (storage instanceof org.unicore.resources.Spool) {
                return com.fujitsu.arcon.njs.priest.GeneralData.Priest.getSpoolRoot();
            } else if (storage instanceof org.unicore.resources.USpace || storage instanceof org.unicore.resources.AlternativeUspace) {
                return com.fujitsu.arcon.njs.priest.GeneralData.Priest.getUspaceRoot();
            } else {
                return com.fujitsu.arcon.njs.priest.TargetSystem.getTargetSystem().mapStorage(storage);
            }
        }

        public Vsite[] listVsites(String machine, int port) throws ResourceCheckerException {
            if (logger.CHAT) logger.chat("Request for a ListVsites at <" + machine + ":" + port + ">");
            UPLConnection c = null;
            try {
                UPLConnection.Factory gcf = RemoteNJS.getConnectionFactory(new Vsite("ssl://" + machine + ":" + port, "GATEWAY"));
                c = (UPLConnection) gcf.connect();
                ListVsitesReply lvr = (ListVsitesReply) c.sendRequestRepeating(new ListVsites());
                c.closeOK();
                return lvr.getList();
            } catch (IOException ex) {
                logger.warning("Failed request for a ListVsites at <" + machine + ":" + port + ">", ex);
                c.closeError();
                throw new ResourceCheckerException("Failed request for a ListVsites at <" + machine + ":" + port + "> " + ex.getMessage(), ex);
            } catch (NJSException e) {
                logger.warning("Failed request for a ListVsites at <" + machine + ":" + port + ">", e);
                c.closeOK();
                throw new ResourceCheckerException("Failed request for a ListVsites at <" + machine + ":" + port + "> " + e.getMessage(), e);
            }
        }

        public Vsite[] listPorts(String machine, int port) throws ResourceCheckerException {
            if (logger.CHAT) logger.chat("Request for a ListPorts at <" + machine + ":" + port + ">");
            UPLConnection c = null;
            try {
                UPLConnection.Factory gcf = RemoteNJS.getConnectionFactory(new Vsite("ssl://" + machine + ":" + port, "GATEWAY"));
                c = (UPLConnection) gcf.connect();
                ListPortsReply lpr = (ListPortsReply) c.sendRequestRepeating(new ListPorts());
                c.closeOK();
                return lpr.getList();
            } catch (IOException ex) {
                logger.warning("Failed request for a ListPorts at <" + machine + ":" + port + ">", ex);
                c.closeError();
                throw new ResourceCheckerException("Failed request for a ListPorts at <" + machine + ":" + port + "> " + ex.getMessage(), ex);
            } catch (NJSException e) {
                logger.warning("Failed request for a ListPorts at <" + machine + ":" + port + ">", e);
                c.closeOK();
                throw new ResourceCheckerException("Failed request for a ListPorts at <" + machine + ":" + port + "> " + e.getMessage(), e);
            }
        }
    }

    public static class GASImpl extends NJSServicesImpl implements GAS.NJS {
    }

    public static class ResourceReservationServiceImpl extends NJSServicesImpl implements ResourceReservationService.NJS {
    }

    public static class ResourceReservationExecutionImpl extends NJSServicesImpl implements ResourceReservationExecution.NJS {

        public ResourceBooking book(Vsite vsite, UserAttributes user, ResourceSet requested, Date start_time) throws NJSException {
            BookResources br = new BookResources("NJSServices BookResources from <" + NJSGlobals.getVsite() + ">");
            br.setResourcesToBook(requested);
            br.setStartTime(start_time);
            BookResources_Outcome bro = (BookResources_Outcome) _consignAction(br, user, vsite);
            if (bro.getStatus().isEquivalent(AbstractActionStatus.NOT_SUCCESSFUL)) {
                throw new NJSException(bro.getReason() + "(" + bro.getStatus() + ")");
            } else {
                return bro.getBooking();
            }
        }

        public void cancel(Vsite vsite, UserAttributes user, String booking_reference) throws NJSException {
            CancelResourceBooking crb = new CancelResourceBooking("NJSServices Cancel Resource Booking from <" + NJSGlobals.getVsite() + ">");
            crb.setReference(booking_reference);
            CancelResourceBooking_Outcome crbo = (CancelResourceBooking_Outcome) _consignAction(crb, user, vsite);
            if (crbo.getStatus().isEquivalent(AbstractActionStatus.NOT_SUCCESSFUL)) {
                throw new NJSException(crbo.getReason() + "(" + crbo.getStatus() + ")");
            }
        }

        public ResourceBooking query(Vsite vsite, UserAttributes user, String booking_reference) throws NJSException {
            QueryResourceBooking qrb = new QueryResourceBooking("NJSServices Query Resource Booking from <" + NJSGlobals.getVsite() + ">");
            qrb.setReference(booking_reference);
            QueryResourceBooking_Outcome qrbo = (QueryResourceBooking_Outcome) _consignAction(qrb, user, vsite);
            if (qrbo.getStatus().isEquivalent(AbstractActionStatus.NOT_SUCCESSFUL)) {
                throw new NJSException(qrbo.getReason() + "(" + qrbo.getStatus() + ")");
            } else {
                return qrbo.getBooking();
            }
        }
    }

    public static class AJORewriterServiceImpl extends NJSServicesImpl implements AJORewriter.NJS {

        private static KnownActionDB kadb;

        public static void init(KnownActionDB in_kadb) {
            kadb = in_kadb;
        }

        public void rewritten(AbstractJob rewrittenAJO) throws NJSException {
            XKnownAction xka = getXKnownAction(rewrittenAJO.getAJOId());
            xka.setFinishedRewriting();
            if (xka.getStatus().isEquivalent(AbstractActionStatus.READY)) {
                xka.replaceJob(rewrittenAJO);
                kadb.ready(xka);
            } else if (xka.getStatus().isEquivalent(AbstractActionStatus.HELD)) {
                xka.replaceJob(rewrittenAJO);
                xka.logEvent("AJO rewriter returned (rewritten) while HELD, will wait for RESUME");
            } else if (xka.getStatus().isEquivalent(AbstractActionStatus.DONE)) {
                xka.logEvent("AJO rewriter returned (rewritten), but already DONE. No action on XKA.");
                throw new NJSException("The target AJO is DONE (cancelled or aborted?). Rewritten AJO not executed.");
            } else {
                xka.logEvent("AJO rewriter returned (rewritten) while in some unexpected state. Ignore it.");
                throw new NJSException("The target AJO in in an unexpected state <" + xka.getStatus() + ">. Rewritten AJO not executed.");
            }
        }

        public void notRewritten(AJOIdentifier jobId) {
            try {
                XKnownAction xka = getXKnownAction(jobId);
                xka.setFinishedRewriting();
                if (xka.getStatus().isEquivalent(AbstractActionStatus.READY)) {
                    kadb.ready(xka);
                } else if (xka.getStatus().isEquivalent(AbstractActionStatus.HELD)) {
                    xka.logEvent("AJO rewriter returned (not rewritten) while HELD, will wait for RESUME");
                } else if (xka.getStatus().isEquivalent(AbstractActionStatus.DONE)) {
                    xka.logEvent("AJO rewriter returned (not rewritten), but already DONE. Ignore it.");
                } else {
                    xka.logEvent("AJO rewriter returned (not rewritten) while in some unexpected state,. Ignore it.");
                }
            } catch (NJSException e) {
                logger.info("Error executing notRewritten for AJO re writer on <" + jobId.getName() + "/" + Integer.toHexString(jobId.getValue()) + ">. Message: " + e.getMessage());
            }
        }

        public void rewriteFailed(AJOIdentifier jobId, AJORewriterException cause) {
            try {
                XKnownAction xka = getXKnownAction(jobId);
                xka.setFinishedRewriting();
                if (xka.getStatus().isEquivalent(AbstractActionStatus.READY)) {
                    xka.setStatus(AbstractActionStatus.FAILED_IN_CONSIGN, "AJORewriter failed with the following reason: " + cause.getMessage());
                } else if (xka.getStatus().isEquivalent(AbstractActionStatus.HELD)) {
                    xka.setStatus(AbstractActionStatus.FAILED_IN_CONSIGN, "AJORewriter failed with the following reason: " + cause.getMessage());
                } else if (xka.getStatus().isEquivalent(AbstractActionStatus.DONE)) {
                    xka.logEvent("AJO rewriter returned (rewrite failed), but already DONE. Ignore it.");
                } else {
                    xka.setStatus(AbstractActionStatus.FAILED_IN_CONSIGN, "AJORewriter failed with the following reason: " + cause.getMessage());
                }
            } catch (NJSException e) {
                logger.info("Error executing rewriteFailed for AJO re writer on <" + jobId.getName() + "/" + Integer.toHexString(jobId.getValue()) + ">. Message: " + e.getMessage());
            }
        }

        /**
		 * Get the XKnownAction from the KADB
		 * @param id
		 * @return
		 */
        private XKnownAction getXKnownAction(AJOIdentifier id) throws NJSException {
            KnownAction ka = kadb.get(id);
            if (ka == null) {
                throw new NJSException("The action is not known to the NJS (may have been aborted or cancelled).");
            }
            if (ka instanceof XKnownAction) {
                return (XKnownAction) ka;
            } else {
                throw new NJSException("The action is no longer a sub-AJO (help?)");
            }
        }
    }
}

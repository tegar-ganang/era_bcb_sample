package com.fujitsu.arcon.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.unicore.AJOIdentifier;
import org.unicore.User;
import org.unicore.Vsite;
import org.unicore.ajo.AbstractAction;
import org.unicore.ajo.AbstractJob;
import org.unicore.ajo.GetActionStatus;
import org.unicore.outcome.AbstractActionStatus;
import org.unicore.outcome.AbstractJob_Outcome;
import org.unicore.outcome.GetActionStatus_Outcome;
import org.unicore.outcome.Outcome;
import org.unicore.upl.ConsignJob;
import org.unicore.upl.ConsignJobReply;
import org.unicore.upl.ListVsites;
import org.unicore.upl.ListVsitesReply;
import org.unicore.upl.Reply;
import org.unicore.upl.RetrieveOutcome;
import org.unicore.upl.RetrieveOutcomeAck;
import org.unicore.upl.RetrieveOutcomeReply;
import org.unicore.upl.UnicoreResponse;
import org.unicore.utility.AbstractActionIterator;
import org.unicore.utility.ConsignForm;
import org.unicore.utility.UserAttributesConverter;
import com.fujitsu.arcon.common.ChunkManager;
import com.fujitsu.arcon.common.ConnectionFactory;
import com.fujitsu.arcon.common.FileChunkManager;
import com.fujitsu.arcon.common.FileReaderFactory;
import com.fujitsu.arcon.common.FileTransferEngine;
import com.fujitsu.arcon.common.FileWriterFactory;
import com.fujitsu.arcon.common.Logger;
import com.fujitsu.arcon.common.RSDReaderFactory;
import com.fujitsu.arcon.common.UPLCJWriterFactory;
import com.fujitsu.arcon.common.UPLReadingChunkManager;

/**
 * Run jobs, monitor them and fetch back results.
 *
 * @author Sven van den Berghe, fujitsu
 *
 * @version $Revision: 1.7 $ $Date: 2006/03/10 21:23:47 $
 *
 **/
public class JobManager {

    public static class Exception extends java.lang.Exception {

        public Exception(String message) {
            super(message);
        }

        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
	 * Thrown when the server fails in a way that is worth retrying
	 * (no TSI, busy etc).
	 * 
	 * @author Sven van den Berghe, Fujitsu Laboratories of Europe
	 * 
	 * Copyright Fujitsu Laboratories of Europe 2003
	 *  
	 * Created Sep 5, 2003
	 */
    public static class SoftException extends JobManager.Exception {

        public SoftException(String message) {
            super(message);
        }

        public SoftException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static File outcome_dir;

    /**
	 * Set the directory that the Client Libraries will use to
	 * write result files fetched from the NJS.
	 *
	 **/
    public static void setOutcomeRootDirectory(File f) {
        outcome_dir = f;
    }

    public static File getOutcomeRootDirectory() {
        return outcome_dir;
    }

    /**
     * the delete policy on outcome files:
     */
    private static boolean deleteOnExit = true;

    /**
     * Whether or not to delete any retrieved files when this
     * client application exits.
     *
     * @param  what true means output files are deleted when the VM finishes
     */
    public static void setDeleteOnExit(boolean what) {
        deleteOnExit = what;
    }

    public static boolean getDeleteOnExit() {
        return deleteOnExit;
    }

    private static int BUFFER_SIZE = 16384;

    /**
     * Set the size of the buffers used in file transfers (mainly concurrent ones)
     * 
     * @param size
     */
    public static void setBufferSize(int size) {
        BUFFER_SIZE = size;
    }

    private static boolean always_poll;

    static boolean alwaysPoll() {
        return always_poll;
    }

    /**
	 * Set the mode for waiting for the end of a job. Generally best to
	 * leave at the deafult value.
	 *
	 **/
    static void setAlwaysPoll(boolean b) {
        always_poll = b;
    }

    /**
     * Get a list of the Vsites (and Ports) provided by a Gateway.
     *
     * Updates the Gateway instance with current Vsites.
     *
     **/
    public static void listVsites(Gateway gateway) throws JobManager.Exception, Connection.Exception {
        Connection c = Connection.getConnection(gateway.getReference());
        try {
            Reply from_lv = c.send(new ListVsites());
            checkUPLReply(from_lv);
            gateway.clearVsites();
            ListVsitesReply lvr = (ListVsitesReply) from_lv;
            Vsite[] temp = lvr.getList();
            for (int i = 0; i < temp.length; i++) {
                CLogger.status("Vsite: " + temp[i] + " " + temp[i].getAJOSpecificationTitle() + " " + temp[i].getAJOSpecificationVersion());
                gateway.addVsite(new VsiteTh(gateway.getReference(), temp[i]));
            }
            Reply from_lp = c.send(new com.fujitsu.arcon.upl.ListPorts());
            checkUPLReply(from_lp);
            gateway.clearPorts();
            com.fujitsu.arcon.upl.ListPortsReply lpr = (com.fujitsu.arcon.upl.ListPortsReply) from_lp;
            temp = lpr.getList();
            for (int i = 0; i < temp.length; i++) {
                CLogger.status("Port:  " + temp[i] + " " + temp[i].getAJOSpecificationTitle() + " " + temp[i].getAJOSpecificationVersion());
                gateway.addPort(new VsiteTh(gateway.getReference(), temp[i]));
            }
        } finally {
            c.done();
        }
    }

    /**
	 * Execute a job (AJO) on a Vsite asynchronously (not waiting for the result). Poll for job completion using {@link #getOutcome}.
	 *
	 * @param ajo The AbstractJob to execute.
	 * @param vsite Where to execute the AbstractJob.
	 *
	 **/
    public static void consignAsynchronous(AbstractJob ajo, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        consignAsynchronous(ajo, null, vsite);
    }

    /**
	 * Execute a job (AJO) on a Vsite asynchronously (not waiting for the result). Poll for job completion using {@link #getOutcome}.
	 *
	 * @param ajo The AbstractJob to execute.
	 * @param portfolios The files to send with the execute job request
	 * @param vsite Where to execute the AbstractJob.
	 *
	 **/
    public static void consignAsynchronous(AbstractJob ajo, PortfolioTh[] portfolios, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        addPortfolios(ajo, portfolios);
        ConsignJob to_consign = makeConsignJob(ajo, false, vsite, true);
        Connection c = Connection.getConnection(vsite);
        try {
            Reply from_consign = c.send(to_consign, portfolios);
            checkUPLReply(from_consign);
        } finally {
            c.done();
        }
    }

    /**
	 * Execute a job (AJO) on a Vsite synchronously (waiting for the result).
	 *
	 * @param ajo The AbstractJob to execute.
	 * @param vsite Where to execute the AbstractJob.
	 *
	 * @return A handle to the results of executing the job.
	 *
	 **/
    public static OutcomeTh consignSynchronous(AbstractJob ajo, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        return consignSynchronous(ajo, null, vsite);
    }

    /**
	 * Execute a job (AJO) on a Vsite synchronously (waiting for the result).
	 *
	 * @param ajo The AbstractJob to execute.
	 * @param portfolios The files to send with the execute job request
	 * @param vsite Where to execute the AbstractJob.
	 *
	 * @return A handle to the results of executing the job.
	 *
	 **/
    public static OutcomeTh consignSynchronous(AbstractJob ajo, PortfolioTh[] portfolios, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        addPortfolios(ajo, portfolios);
        ConsignJob to_consign = makeConsignJob(ajo, true, vsite, true);
        Connection c = Connection.getConnection(vsite);
        try {
            Reply reply = c.send(to_consign, portfolios);
            checkUPLReply(reply);
            OutcomeTh oth = handleConignSynchronousReply(reply, ajo, c, vsite);
            return oth;
        } finally {
            c.done();
        }
    }

    /**
     * Wrap the AbstractAction in an AJO and consign it to
     * the Vsite - synchronously.
     *
     **/
    public static OutcomeTh executeAction(AbstractAction action, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        AbstractJob ajo = new AbstractJob("AJO wrapper for " + action.getName());
        ajo.add(action);
        OutcomeTh tho = consignSynchronous(ajo, vsite);
        return tho;
    }

    /**
	 * Get the current status and any results of a Job executing on a Vsite. This method
	 * always returns the Outcome object for the Job.
	 * The Outcome object contains the status of the Job and the status of all the AbstractActions
	 * in the Job. The Outcome may also include the results of an AbstractAction's execution, 
	 * where these are stored in in the Outcome. If the Job has finished execution, this method
	 * will also fetch any Outcome files that the Job produced. Outcome files are the files that
	 * the Job specifically saved for return to the Client, usually stdout, stderr and the result
	 * of the CopyPortfolioToOutcome AbstractAction.
	 * <p>
	 * This method also cleans up finished jobs, removing the Job and all Outcome files from the Vsite.
	 * <p>
	 * This method uses a single UPL stream to transfer the data.
	 * <p>
	 * More control over the fetching of result files and removing Jobs can be obtained using
	 * {@link #checkOutcome}, {@link #retrieveData} and {@link #removeJob}.
	 *
	 * @param ajo_id The Identifier of the job to query for results.
	 * @param vsite Where the target job is executing.
	 *
	 * @return The current results.
	 * 
	 *
	 **/
    public static OutcomeTh getOutcome(AJOIdentifier ajo_id, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        OutcomeTh reply = getActualOutcome(ajo_id, vsite, true);
        AbstractJob_Outcome ajoo;
        try {
            ajoo = ConsignForm.convertFrom((AbstractJob_Outcome) reply.getOutcome());
        } catch (ClassCastException ccex) {
            throw new JobManager.Exception("Did not get AbstractJob_Outcome from NJS, versioning?", ccex);
        } catch (java.lang.Exception ex) {
            throw new JobManager.Exception("Problems deserialising Outcomes in AJO Outcome", ex);
        }
        reply.setOutcome(ajoo);
        if (ajoo.getStatus().isEquivalent(AbstractActionStatus.DONE)) sendROA(ajo_id, vsite);
        return reply;
    }

    /**
	 * Retrieve the Outcome and Outcome files produced by the execution of a Job. The Job must have completed
	 * 
	 * @param ajo_id The job whose Outcome and Outcome files will be fetched.
	 * @param vsite  Where the Job executed.

	 * 
	 * @throws JobManager.Exception
	 * @throws Connection.Exception
	 */
    public static OutcomeTh retrieveData(AJOIdentifier ajo_id, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        OutcomeTh reply = getActualOutcome(ajo_id, vsite, true);
        AbstractJob_Outcome ajoo;
        try {
            ajoo = ConsignForm.convertFrom((AbstractJob_Outcome) reply.getOutcome());
        } catch (ClassCastException ccex) {
            throw new JobManager.Exception("Did not get AbstractJob_Outcome from NJS, versioning?", ccex);
        } catch (java.lang.Exception ex) {
            throw new JobManager.Exception("Problems deserialising Outcomes in AJO Outcome", ex);
        }
        reply.setOutcome(ajoo);
        return reply;
    }

    /**
	 * Get the current status and any results of a Job executing on a Vsite. This method
	 * returns the Outcome object for the Job. It does not return Outcome files. This method
	 * does not clean up finished Jobs.
	 * When a Job has finished execution it should be cleaned up using {@link #removeJob}.
	 * Outcome files can be fetched using {@link #retrieveData}. {@link #getOutcome} combines
	 * all these calls.
	 *
	 * @param ajo_id The Identifier of the job to query for results.
	 * @param vsite Where the target job is executing.
	 *
	 * @return The current results.
	 * 
	 *
	 **/
    public static OutcomeTh checkOutcome(AJOIdentifier ajo_id, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        OutcomeTh reply = getActualOutcome(ajo_id, vsite, false);
        AbstractJob_Outcome ajoo;
        try {
            ajoo = ConsignForm.convertFrom((AbstractJob_Outcome) reply.getOutcome());
        } catch (ClassCastException ccex) {
            throw new JobManager.Exception("Did not get AbstractJob_Outcome from NJS, versioning?", ccex);
        } catch (java.lang.Exception ex) {
            throw new JobManager.Exception("Problems deserialising Outcomes in AJO Outcome", ex);
        }
        reply.setOutcome(ajoo);
        return reply;
    }

    /**
	 * Remove a finished Job from the Vsite. This methods removes the Job and
	 * any Outcome files from the Vsite. If the Job is not complete, then the 
	 * method fails.
	 * 
	 * @param ajo_id The Identifier of the Job to be removed.
	 * @param vsite  Where the Job is executing.
	 * 
	 * @throws JobManager.Exception An error occurred in processing (e.g. the Job was not complete).
	 * @throws Connection.Exception An error with the connection to teh Vsite.
	 */
    public static void removeJob(AJOIdentifier ajo_id, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        sendROA(ajo_id, vsite);
    }

    /**
	 * Tests if a Job has finished execution.
	 * 
	 * @param ajo_id The Identifier of the Job to be tested.
	 * @param vsite  Where the Job is executing.
	 * 
	 * @return True if the Job is complete, false otherwise.
	 * 
	 * @throws JobManager.Exception An error in execution (e.g. Job not found).
	 * @throws Connection.Exception
	 */
    public static boolean testJobComplete(AJOIdentifier ajo_id, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        GetActionStatus gas = new GetActionStatus("Arcon servlet status query", ajo_id);
        OutcomeTh outcome = JobManager.executeAction(gas, vsite);
        if (outcome.getOutcome().getStatus().isEquivalent(AbstractActionStatus.SUCCESSFUL)) {
            return ((GetActionStatus_Outcome) outcome.getActionOutcome(gas.getId())).getActionStatus().isEquivalent(AbstractActionStatus.DONE);
        } else {
            throw new JobManager.Exception("Job status test failed (Job not on Vsite?)");
        }
    }

    static class LocalConnectionFactory implements ConnectionFactory {

        private VsiteTh vsite;

        public LocalConnectionFactory(VsiteTh vsite) {
            this.vsite = vsite;
        }

        public com.fujitsu.arcon.common.Connection connect() {
            try {
                return Connection.getConnection(vsite);
            } catch (Connection.Exception e) {
                CLogger.status("Problems getting Connection: " + e.getMessage());
                return null;
            }
        }

        public Socket getRawSocket() throws IOException {
            return null;
        }
    }

    static class LocalFTERequestor implements FileTransferEngine.Requestor {

        public void transferDone() {
            synchronized (this) {
                this.notifyAll();
                done = true;
            }
        }

        private boolean done = false;

        public synchronized boolean done() {
            return done;
        }
    }

    static class LocalLogger implements Logger {

        public void logComment(String comment) {
            CLogger.status(comment);
        }

        public void logError(String error) {
            CLogger.status("FTE Error: " + error);
        }

        public void logError(String error, java.lang.Exception ex) {
            CLogger.status("FTE Error: " + error + " " + ex.getMessage());
            ex.printStackTrace();
        }

        public boolean writeComments() {
            return true;
        }
    }

    public static void consignJob(AbstractJob ajo, PortfolioTh[] portfolios, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        if (portfolios != null && portfolios.length > 0) {
            addPortfolios(ajo, portfolios);
            FileChunkManager cm = new FileChunkManager(new LocalLogger(), BUFFER_SIZE);
            for (int i = 0; i < portfolios.length; i++) {
                cm.addPortfolio(portfolios[i].getPortfolio(), portfolios[i].getFiles());
            }
            _consignJob(ajo, vsite, 1, cm, true);
        } else {
            consignAsynchronous(ajo, vsite);
        }
    }

    private static void _consignJob(AbstractJob ajo, VsiteTh vsite, int streams, FileChunkManager cm, boolean reset_ids) throws JobManager.Exception {
        ConsignJob to_consign = makeConsignJob(ajo, false, vsite, reset_ids);
        FileReaderFactory rf = new FileReaderFactory(streams, new LocalLogger());
        UPLCJWriterFactory wf = new UPLCJWriterFactory(to_consign, streams, new LocalLogger(), new LocalConnectionFactory(vsite));
        LocalFTERequestor monitor = new LocalFTERequestor();
        FileTransferEngine fte = FileTransferEngine.create(rf, wf, cm, monitor, new LocalLogger(), BUFFER_SIZE);
        fte.doTransfer();
        synchronized (monitor) {
            try {
                if (!monitor.done()) monitor.wait();
            } catch (InterruptedException e) {
            }
        }
        if (!fte.transferOK()) {
            throw new JobManager.Exception("File transfer failed: " + fte.errorMessage());
        }
        checkUPLReply(wf.getConsignJobReply());
    }

    private static OutcomeTh handleConignSynchronousReply(Reply from_consign, AbstractJob ajo, Connection c, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        OutcomeTh reply;
        if (from_consign instanceof ConsignJobReply) {
            CLogger.status("NJS does not want to execute synchronously, polling");
            do {
                try {
                    Thread.sleep(5000);
                } catch (java.lang.Exception ex) {
                }
                ;
                CLogger.status("Waiting for NJS to finish execution");
                reply = getActualOutcome((AJOIdentifier) ajo.getId(), vsite, true);
                if (reply == null) {
                    return null;
                }
            } while (!(reply.getOutcome().getStatus().isEquivalent(AbstractActionStatus.DONE)));
            sendROA((AJOIdentifier) ajo.getId(), vsite);
        } else {
            reply = processROR((RetrieveOutcomeReply) from_consign, c);
        }
        AbstractJob_Outcome ajoo;
        try {
            ajoo = ConsignForm.convertFrom(reply.getOutcome());
        } catch (java.lang.Exception ex) {
            throw new JobManager.Exception("Problems deserialising Outcomes in AJO Outcome", ex);
        }
        reply.setOutcome(ajoo);
        return reply;
    }

    private static void addPortfolios(AbstractJob ajo, PortfolioTh[] portfolios) throws JobManager.Exception {
        if (portfolios != null) {
            for (int i = 0; i < portfolios.length; i++) {
                File[] files = portfolios[i].getFiles();
                for (int j = 0; j < files.length; j++) {
                    if (!files[j].canRead()) {
                        if (!files[j].getName().equals("NO_OVERWRITE")) throw new JobManager.Exception("Cannot read file: " + files[j]);
                    }
                }
                ajo.addStreamed(portfolios[i].getPortfolio());
            }
        }
    }

    private static OutcomeTh getActualOutcome(AJOIdentifier ajo_id, VsiteTh vsite, boolean fetch_files) throws JobManager.Exception, Connection.Exception {
        if (vsite == null) {
            throw new JobManager.Exception("Vsite is null.");
        }
        RetrieveOutcome ro = new RetrieveOutcome(ajo_id, new Vsite(new Vsite.Reference(vsite.getName(), vsite.getReference().getAddress())));
        ro.setSendStreamedData(fetch_files);
        Connection c = Connection.getConnection(vsite);
        try {
            Reply from_ro = c.send(ro);
            checkUPLReply(from_ro);
            OutcomeTh result = processROR((RetrieveOutcomeReply) from_ro, c);
            return result;
        } finally {
            c.done();
        }
    }

    /**
	 * Send a message to clean up a job from the Vsite. This is not usually
	 * necessary as the getOutcome calls will clean up when a job
	 * has completed execution.
	 *
	 * @param ajo_id The job to clean up.
	 * @param vsite Wher the job is running.
	 *
	 **/
    public static void sendROA(AJOIdentifier ajo_id, VsiteTh vsite) throws JobManager.Exception, Connection.Exception {
        RetrieveOutcomeAck roa = new RetrieveOutcomeAck(ajo_id, new Vsite(new Vsite.Reference(vsite.getName(), vsite.getReference().getAddress())));
        Connection c = Connection.getConnection(vsite);
        try {
            Reply from_roa = c.send(roa);
            checkUPLReply(from_roa);
        } finally {
            c.done();
        }
    }

    private static OutcomeTh processROR(RetrieveOutcomeReply ror, Connection c) throws JobManager.Exception {
        byte[] outcome_bytes = ror.getOutcome();
        Outcome njs_reply;
        try {
            njs_reply = (Outcome) (new ObjectInputStream(new ByteArrayInputStream(outcome_bytes))).readObject();
        } catch (java.lang.Exception ex) {
            throw new JobManager.Exception("Problems deserialising Outcome (classes compatible?)", ex);
        }
        OutcomeTh reply = new OutcomeTh();
        reply.setOutcome((AbstractJob_Outcome) njs_reply);
        if (ror.hasStreamed()) {
            CLogger.status("Reading streamed data from a RetrieveOutcomeReply");
            ZipInputStream streamed = null;
            try {
                streamed = c.getStreamedInputStream();
            } catch (IOException ioex) {
                throw new JobManager.Exception("Problems getting streamed data: " + ioex.getMessage(), ioex);
            }
            try {
                String message = null;
                ZipEntry entry = streamed.getNextEntry();
                while (entry != null) {
                    String mode = "no mode";
                    if (entry.getExtra() != null) {
                        mode = "mode " + Byte.toString(entry.getExtra()[0]);
                    }
                    if (entry.isDirectory()) {
                        CLogger.status("Directory <" + entry.getName() + "> skipped.");
                    } else {
                        File entry_file = new File(outcome_dir, entry.getName().replace('/', '_'));
                        if (deleteOnExit) entry_file.deleteOnExit();
                        CLogger.status("Extracting file <" + entry.getName() + "> to <" + entry_file + ">, size <" + entry.getSize() + ">, mode <" + mode + ">");
                        byte[] buffer = new byte[65536];
                        FileOutputStream fos = new FileOutputStream(entry_file);
                        int read;
                        do {
                            read = streamed.read(buffer, 0, buffer.length);
                            CLogger.status("Read <" + read + "> from streamed data");
                            if (read > 0) {
                                fos.write(buffer, 0, read);
                            }
                        } while (read >= 0);
                        fos.close();
                        String path = entry.getName();
                        String the_action_id = null;
                        StringTokenizer st = new StringTokenizer(path, "/");
                        while (st.hasMoreTokens()) {
                            String this_token = st.nextToken();
                            if (!this_token.startsWith("AA")) break;
                            the_action_id = this_token;
                        }
                        if (the_action_id == null) {
                            message = "Streamed file names badly structured (expect AA): " + path;
                            break;
                        }
                        Integer id_value;
                        try {
                            long temp = Long.parseLong(the_action_id.substring(2), 16);
                            if (temp > Integer.MAX_VALUE) temp = temp + 2 * Integer.MIN_VALUE;
                            id_value = new Integer((int) temp);
                        } catch (java.lang.Exception ex) {
                            message = "Problems parsing directory as AA identifier: " + the_action_id;
                            break;
                        }
                        Collection already_there = (Collection) reply.getFilesMapping().get(id_value);
                        if (already_there == null) {
                            already_there = new HashSet();
                            reply.getFilesMapping().put(id_value, already_there);
                        }
                        already_there.add(entry_file);
                    }
                    entry = streamed.getNextEntry();
                }
                if (message != null) {
                    throw new JobManager.Exception(message);
                }
            } catch (ZipException zex) {
                throw new JobManager.Exception("Errors (zip) reading data streamed from NJS.", zex);
            } catch (IOException ioex) {
                throw new JobManager.Exception("Errors reading data streamed from NJS.", ioex);
            }
            try {
                streamed.close();
            } catch (java.lang.Exception ex) {
            }
            c.doneWithStreamedInputStream();
        }
        return reply;
    }

    private static ConsignJob makeConsignJob(AbstractJob ajo, boolean synchronous, VsiteTh vsite, boolean reset_ids) throws JobManager.Exception {
        if (ajo.getVsite() == null) ajo.setVsite(new org.unicore.Vsite(vsite.getReference().getAddress(), vsite.getName()));
        if (reset_ids) org.unicore.utility.ResetIdentifiers.doit(ajo);
        boolean polling_mode = !synchronous;
        Identity identity = ((Reference.SSL) vsite.getReference()).getIdentity();
        ajo.setUserAttributes(identity.getAttributes());
        AbstractActionIterator aai = new AbstractActionIterator(ajo);
        while (aai.hasNext()) {
            AbstractAction aa = aai.next();
            if (aa instanceof AbstractJob) {
                AbstractJob aj = (AbstractJob) aa;
                if (aj.getAJOEndorser() == null) aj.setAJOEndorser(identity.getEndorser());
            }
        }
        AbstractJob converted_ajo;
        try {
            converted_ajo = ConsignForm.convertTo(ajo, identity.getSignature());
        } catch (NotSerializableException nsex) {
            throw new JobManager.Exception("Problems serialising AJO. Class Paths OK?", nsex);
        } catch (IOException ioex) {
            throw new JobManager.Exception("Internal IO problems serialising AJO.", ioex);
        } catch (SignatureException sex) {
            throw new JobManager.Exception("Problems serialising AJO, cryptography not initialised?", sex);
        } catch (NullPointerException nex) {
            throw new JobManager.Exception("Null pointer serialising AJO.\n", nex);
        }
        if (alwaysPoll()) polling_mode = true;
        ConsignJob reply = new ConsignJob((AJOIdentifier) ajo.getId(), vsite.getVsite(), identity.getEndorser(), converted_ajo.getConsignForm(), converted_ajo.getSignature(), (byte[]) null, ajo.getStreamed() != null, polling_mode);
        try {
            User old_style_endorser = new User(identity.getEndorser().getCertificateChain(), "");
            old_style_endorser.setXlogin(UserAttributesConverter.getXlogin(ajo.getUserAttributes()));
            old_style_endorser.setProject(UserAttributesConverter.getProject(ajo.getUserAttributes()));
            old_style_endorser.setEmailAddress(UserAttributesConverter.getEmailAddress(ajo.getUserAttributes()));
            reply.setEndorser(old_style_endorser);
        } catch (CertificateException e) {
        }
        return reply;
    }

    protected static void checkUPLReply(Reply reply) throws JobManager.Exception {
        UnicoreResponse[] ur = reply.getTrace();
        if (ur[ur.length - 1].getReturnCode() < -1) {
            String message = "Server suggests try again: " + ur[ur.length - 1].getReturnCode() + ":" + ur[ur.length - 1].getComment();
            throw new JobManager.SoftException(message);
        } else if (ur[ur.length - 1].getReturnCode() != 0) {
            String message = "UPL Reply reports an error: " + ur[ur.length - 1].getReturnCode() + ":" + ur[ur.length - 1].getComment();
            throw new JobManager.Exception(message);
        }
    }
}

package hu.sztaki.lpds.APP_SPEC.services;

import hu.sztaki.lpds.APP_SPEC.exceptions.APP_SPECException;
import hu.sztaki.lpds.pgportal.portlets.compiler.Pair;
import hu.sztaki.lpds.pgportal.portlets.credential.SZGCredentialBean;
import hu.sztaki.lpds.pgportal.services.credential.SZGCredential;
import hu.sztaki.lpds.pgportal.services.credential.SZGCredentialManager;
import hu.sztaki.lpds.pgportal.services.credential.SZGStoreKey;
import hu.sztaki.lpds.pgportal.services.pgrade.GridConfigs;
import hu.sztaki.lpds.pgportal.services.pgrade.PersistenceManager;
import hu.sztaki.lpds.pgportal.services.pgrade.SZGJob;
import hu.sztaki.lpds.pgportal.services.pgrade.SZGWorkflow;
import hu.sztaki.lpds.pgportal.services.pgrade.SZGWorkflowList;
import hu.sztaki.lpds.pgportal.services.quota.quotaStaticService;
import hu.sztaki.lpds.pgportal.services.utils.MiscUtils;
import hu.sztaki.lpds.pgportal.services.utils.PropertyLoader;
import hu.sztaki.lpds.pgportal.services.utils.WorkflowUtils;
import hu.sztaki.lpds.pgportal.wfeditor.client.pstudy.ParameterKeyList;
import hu.sztaki.lpds.pgportal.wfeditor.client.pstudy.autogen.AutoGeneratorJob;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to store and manage Instances that generated from a specified published application
 * @author  akos balasko MTA SZTAKI
 */
public class APP_SPEC_simple_Instance extends APP_SPECInstance {

    public APP_SPEC_simple_Instance() {
    }

    /**
     * Constructor of the Instance class. 
     * @param user - login name of the user
     * @param name - name of the Instance 
     * @param desc - description of Instance
     * @param baseworkflow - Name of the Published Workflow
     */
    public APP_SPEC_simple_Instance(String userId, String role, String rootID, String name, String desc, String baseworkflow) {
        this.name = name;
        this.group = role;
        this.userId = userId;
        this.description = desc;
        PersistenceManager.initPM(rootID);
        finishedjobs = new ArrayList<String>();
        this.BaseWorkflowName = baseworkflow;
        logger.debug("BASEWORKFLOW IS : " + baseworkflow + " ; rootID is : " + rootID);
        System.out.println("WORKFLOWS OF ROOTID : ");
        for (SZGWorkflow w : SZGWorkflowList.getInstance(rootID).getWorkflowList()) {
            System.out.println("ID : " + w.getId().toString());
        }
        wf = SZGWorkflowList.getInstance(rootID).getWorkflow(BaseWorkflowName);
        System.out.println("BaseWorkflow Saved : " + baseworkflow + " Workflow is :" + wf.getId());
        inputuploadstatus = new ArrayList<Boolean>();
        this.status = STATUS_INCOMPLETE;
        this.log = "";
        this.workflowStatuses = new HashMap<String, Integer>();
        this.worklowStats = new HashMap<String, List<Integer>>();
        this.JobStatuses = new ArrayList<Pair<String, Pair<Integer, Integer>>>();
        cm = SZGCredentialManager.getInstance();
    }

    /**
     * Generates a new Workflow from BaseWorkflow. It Copies the necessarry files, and modify them to be able the use it correctly.
     *  @param userID - the ID of the User. Workflow will be generated for this user's current instance  
     *  @throws APP_SPECException - if generalization fails, exception will be thrown
     * @return void
     */
    public void generateWorkflow(String userID) throws APP_SPECException {
        if (wf == null) {
            wf = SZGWorkflowList.getInstance(userID).getWorkflow(this.BaseWorkflowName);
        }
        ParameterKeyList keyList = new ParameterKeyList();
        System.out.println("INITIALLY BASEWF IS IN isPS: " + wf.isPS() + " state ");
        for (SZGJob job : wf.getJobList()) {
            System.out.println("Job: " + job.getName() + "Type is : " + job.getType() + " Status : " + job.getStatus());
        }
        Date date = GregorianCalendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
        try {
            String wfName = getName().replaceAll("\\W", "_");
            File wfDir = getWorkflowDir(userID, wfName);
            if (wfDir.exists()) {
                MiscUtils.deleteFileRecursively(wfDir);
            }
            MiscUtils.copyFileRecursively(getWorkflowDir(wf), wfDir, true);
            File wrkFile = new File(getWorkflowDir(userID, wfName), wf.getId() + "_remote.wrk");
            wrkFile.renameTo(new File(getWorkflowDir(userID, wfName), wfName + "_remote.wrk"));
            if (wf.getJobsByType(SZGJob.AUTO_GEN_TYPE).length != 0) {
                for (SZGJob genJob : wf.getJobsByType(SZGJob.AUTO_GEN_TYPE)) {
                    AutoGeneratorJob job = this.getAutoGeneratorJob(wf, genJob.getName());
                    System.out.println("INPUT TEXT : " + job.getInputText());
                    writeAutogeneratorJob(wfDir, job);
                }
            }
            modifyWorkflowName(userID, wfName);
            modifySEPath(userID, wfName);
            WorkflowUtils.loadInWorkflow(userID, wfName, true);
            quotaStaticService.getInstance().reallocateUserWorkflowSpace(userID, wfName);
            genwf = SZGWorkflowList.getInstance(userID).getWorkflow(wfName);
            this.APP_SPECWorkflowName = genwf.getId().toString();
            this.addLogMessage("Workflow Generated");
            this.setStatus(genwf.getStatus());
            worklowStats.put(genwf.getId().toString(), this.parsePSWorkflowStatistics(genwf));
            String path = PropertyLoader.getPrefixDir() + "users/" + userID + "/" + wfName + "_files";
            File deletepublished = new File(path + "/" + published_property);
            System.out.println("PATH IS : " + path + "/" + published_property);
            deletepublished.delete();
        } catch (Exception e) {
            System.out.println("[LOGGER ERROR] " + "Cannot generate workflow  " + e.getMessage());
            throw new APP_SPECException("Cannot generate workflow!", e);
        }
    }

    protected void modifyWorkflowPath(String userID, String wfName) {
        File wfDir = getWorkflowDir(userID, wfName);
        File[] files = new File[2];
        files[0] = new File(wfDir, wfName + "_remote.wrk");
        files[1] = new File(wfDir, "outputFilesRemotePath.dat");
        for (File file : files) {
            System.out.println("LOGGER[DEBUG" + file.getAbsolutePath());
            String fileText = MiscUtils.readFileToStr(file.getAbsolutePath());
            fileText = fileText.replaceAll("\\Q" + getWorkflowDir(wf).getAbsolutePath() + "\\E", getWorkflowDir(wf.getUserId().toString(), wfName).getAbsolutePath());
            MiscUtils.writeStrToFile(file.getAbsolutePath(), fileText);
        }
    }

    /**
     * Modifies the name of the Workflow on the .wrk files and each files with .desc extension.
     */
    protected void modifyWorkflowName(String userID, String wfName) {
        File wfDir = getWorkflowDir(userID, wfName);
        for (File file : wfDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.endsWith(".wrk") || name.endsWith(".desc") || name.equals("outputFilesRemotePath.dat");
            }
        })) {
            System.out.println("LOGGER[DEBUG" + file.getAbsolutePath());
            String fileText = MiscUtils.readFileToStr(file.getAbsolutePath());
            fileText = fileText.replaceAll("\\Q" + wf.getId().toString() + "\\E", wfName);
            MiscUtils.writeStrToFile(file.getAbsolutePath(), fileText);
        }
    }

    /**
     * Modifies the path of the remote files. 
     */
    protected void modifySEPath(String userID, String wfName) {
        File wfDir = getWorkflowDir(userID, wfName);
        String PSDir = "/" + userID + "/" + getName().replaceAll("\\W", "_");
        String PSOutputDir = PSDir + "/outputs/" + "input_ohm";
        String PSInputDir = PSDir + "/inputs/" + "output_ohm";
        if (wf.isPS()) {
            File propDesc = new File(wfDir, ".PS=PROPS1.desc");
            MiscUtils.writeStrToFile(propDesc.getAbsolutePath(), MiscUtils.readFileToStr(propDesc.getAbsolutePath()).replaceAll("/grid/([^/]*)/[^ ]*", "/grid/$1" + PSDir));
        }
        for (SZGJob agenJob : wf.getJobList()) {
            if (agenJob.getType() == SZGJob.GENERATOR_TYPE) {
                File jdl = new File(wfDir, agenJob.getName() + "/" + agenJob.getName() + ".jdl");
                MiscUtils.writeStrToFile(jdl.getAbsolutePath(), (MiscUtils.readFileToStr(jdl.getAbsolutePath()).replaceAll("\"/grid/([^/]*)/([^ ])*\"", "\"/grid/$1" + PSInputDir + "\"")).replaceAll("/users/([^/])*/([^/])*_files", "/users/" + userID + "/" + wfName));
            }
        }
        File wrk = new File(wfDir, wfName + "_remote.wrk");
        String wrkText = MiscUtils.readFileToStr(wrk.getAbsolutePath());
        wrkText = wrkText.replaceAll("\"/grid/([^/]*)/([^ ])*\"", "\"/grid/$1" + PSInputDir + "\"");
        wrkText = wrkText.replaceAll("\"lfn:/grid/([^/]*)/[^/]*/([^ ]*)\" ([^ ]*) PERMANENT OUTPUT", "\"lfn:/grid/$1" + PSOutputDir + "/$2\" /$3 PERMANENT OUTPUT");
        wrkText = wrkText.replaceAll("/users/([^/])*/([^/])*_files", "/users/" + userID + "/" + wfName + "_files");
        MiscUtils.writeStrToFile(wrk.getAbsolutePath(), wrkText);
    }

    /**
     * Write an object file for the Autogenerator Job. It will contains the actual parameters.
     * 
     * @param wfDir
     * @param job
     * @throws IOException
     */
    protected void writeAutogeneratorJob(File wfDir, AutoGeneratorJob job) throws IOException {
        File genFile = new File(wfDir, job.getJobName() + "/" + "generator.object");
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(genFile));
        out.writeObject(job);
        out.close();
    }

    protected AutoGeneratorJob getAutoGeneratorJob(SZGWorkflow workflow, String name) throws APP_SPECException {
        AutoGeneratorJob ret = null;
        SZGJob genJob = workflow.getJobByName(workflow.getUserId(), name);
        System.out.println("LOGGER[DEBUG" + "APP_SPEC workflow's (" + workflow.getId() + ") AutogenJob:" + genJob.getName());
        File genFile = new File(new File(APP_SPECInstance.getWorkflowDir(workflow), genJob.getName()), "generator.object");
        System.out.println("LOGGER[DEBUG" + genFile.getAbsolutePath() + ";" + genFile.exists());
        try {
            ObjectInputStream objectStream = new ObjectInputStream(new FileInputStream(genFile));
            ret = (AutoGeneratorJob) objectStream.readObject();
        } catch (Exception e) {
            System.out.println("[LOGGER ERROR] " + "Error while getAutoGeneratorJob for workflow(" + workflow.getId() + "):" + e.getMessage());
            throw new APP_SPECException("Autogenerator Job can not found!", e);
        }
        return ret;
    }

    protected boolean userCertCheckMG(String username, String gridName) {
        if (gridName.equals("local") || gridName.equals("PBS") || gridName.equals("LSF") || gridName.equals("3GBRIDGE")) return true;
        SZGCredentialManager cm = SZGCredentialManager.getInstance();
        SZGCredential c = cm.getCredentialForGrid(username, gridName);
        if (c == null) return false;
        try {
            if (c.getTimeLeftInSeconds() <= 0) return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Starts the generated workflow 
     * @param portalUserName login name of the user
     * @return void
     * @throws APP_SPECException
     * 
     */
    public void startWorkflow(String portalUserName, String basewfUserID) throws APP_SPECException {
        String errorMessage = null;
        this.log = new String();
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        System.out.println("I'm here!!!");
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        if (genwf.isCertNeeded()) {
            SZGJob[] jobs = genwf.getJobList();
            String gridName;
            for (int i = 0; i < jobs.length; i++) {
                gridName = jobs[i].getGrid();
                if (!jobs[i].getGrid().endsWith(GridConfigs.CLUSTERGRID_NAME) && !this.userCertCheckMG(portalUserName, gridName)) {
                    System.out.println("No valid certificate for " + gridName);
                    APP_SPECException e = new APP_SPECException("No valid certificate for " + gridName);
                    throw e;
                }
            }
        }
        try {
            if (!genwf.isSubmitted() || genwf.isStopped()) {
                genwf.submit();
                System.out.println(this.APP_SPECWorkflowName + " submitted successfully! ");
                this.timer = new Timer(this.getName() + "Timer");
                timer.schedule(new WorkflowStatusTask(this), 0);
            }
        } catch (Exception e) {
            errorMessage = errorMessage == null ? e.getMessage() + "\n" : errorMessage + e.getMessage() + "\n";
        }
        this.setStatus(this.STATUS_RUNNING);
        if (errorMessage != null) {
            throw new APP_SPECException(errorMessage);
        }
    }

    /**
     * Tries to rescue the generated workflow  
     * @param portalUserName login name of the user
     * @return void
     * @throws Exception 
     */
    public void RescueWorkflow(String portalUserName) throws Exception {
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        String userID = (String) genwf.getUserId();
        if (quotaStaticService.getInstance().getUserBeans().get(userID) != null) if (!quotaStaticService.getInstance().getUserBean(userID).isStorageHasFreeSpace()) {
            System.out.println("Must free disc space before submission!");
            return;
        }
        if (genwf.isCertNeeded()) {
            SZGJob[] jobs = genwf.getJobList();
            String gridName;
            for (int i = 0; i < jobs.length; i++) {
                if (!jobs[i].isFinished()) {
                    gridName = jobs[i].getGrid();
                    if (!jobs[i].getGrid().endsWith(GridConfigs.CLUSTERGRID_NAME) && !this.userCertCheckMG(userID, gridName)) {
                        System.out.println("No valid certificate for " + gridName + "!");
                        throw new APP_SPECException("No valid certificate for " + gridName + "!", this);
                    }
                }
            }
        }
        genwf.rescue();
        System.out.println("Workflow successfully rescued.");
        this.setStatus(this.STATUS_RUNNING);
    }

    /**
     * Cancels the generated workflow  
     * @param portalUserName login name of the user
     * @return void
     * @throws Exception 
     */
    public void CancelWorkflow(String portalUserName) throws Exception {
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        SZGWorkflowList wfl = SZGWorkflowList.getInstance(WorkflowUtils.getPSWFUserHash((String) genwf.getUserId(), genwf, false));
        wfl.stopWorkflow(genwf.getId());
        this.setStatus(this.STATUS_STOPPED);
        timer.cancel();
    }

    /**
     * Suspends the generated workflow  
     * @param portalUserName login name of the user
     * @return void
     * @throws Exception 
     */
    public void SuspendWorkflow(String portalUserName) throws Exception {
        boolean go = true;
        if (genwf.isPS()) {
            for (SZGJob job : genwf.getJobsByType(2)) {
                if (job.getStatus() == 3) {
                    go = false;
                }
            }
            for (SZGJob job : genwf.getJobsByType(3)) {
                if (job.getStatus() == 3) {
                    go = false;
                }
            }
            for (SZGJob job : genwf.getJobsByType(5)) {
                if (job.getStatus() == 3) {
                    go = false;
                }
            }
            if (genwf.getJobsByType(2).length == 0 && genwf.getJobsByType(3).length == 0 && genwf.getJobsByType(5).length == 0) {
                go = false;
            }
        }
        if (go) {
            genwf.freeUpPSWorkflow();
            this.cm = SZGCredentialManager.getInstance();
            this.cb = new SZGCredentialBean(portalUserName);
            if (this.cm != null) {
                this.cb.setCredentials(this.cm.getCredentials(portalUserName));
                try {
                    this.cb.setUseThis(this.cm.getUseThis(portalUserName));
                } catch (Exception ex) {
                }
            }
            try {
                loadUsrCert(portalUserName);
            } catch (Exception e) {
            }
            String userID = (String) genwf.getUserId();
            System.out.println("WorkflowID : " + genwf.getId() + "UserID : " + genwf.getUserId() + "Is ShowPS Details :  " + genwf.isShowPSDetails());
            genwf.setShowPSDetails(true);
            SZGWorkflowList wfl = SZGWorkflowList.getInstance(WorkflowUtils.getPSWFUserHash(userID, genwf, genwf == null ? true : !genwf.isShowPSDetails()));
            System.out.println("is showPSDetails : " + wfl.getWorkflow(genwf.getId()).isShowPSDetails());
            wfl.suspendWorkflow(genwf.getId());
            this.setStatus(this.STATUS_PAUSED);
        } else {
            System.out.println("Workflow isEWF is true do nothing !!! ");
            APP_SPECException e = new APP_SPECException("Trying Suspend while eWFs submitted!");
            throw e;
        }
    }

    public void SuspendeWorkflow(String portalUserName, String eWorkflowName) {
        boolean go = true;
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        String userID = (String) genwf.getUserId();
        System.out.println("WorkflowID : " + genwf.getId() + "UserID : " + genwf.getUserId() + "Is ShowPS Details :  " + genwf.isShowPSDetails());
        genwf.setShowPSDetails(true);
        SZGWorkflow[] wfl = WorkflowUtils.loadIneWorkflows(portalUserName, genwf);
        for (int i = 0; i < wfl.length; ++i) {
            if (wfl[i].getId().toString().equals(new String(eWorkflowName))) {
                wfl[i].suspend();
            }
        }
        this.setStatus(this.STATUS_PAUSED);
    }

    public void CanceleWorkflow(String portalUserName, String eWorkflowName) {
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        SZGWorkflow[] wfl = WorkflowUtils.loadIneWorkflows(portalUserName, genwf);
        for (int i = 0; i < wfl.length; ++i) {
            if (wfl[i].getId().toString().equals(new String(eWorkflowName))) {
                wfl[i].stop(false);
            }
        }
    }

    public void RescueeWorkflow(String portalUserName, String eWorkflowName) throws Exception {
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        String userID = (String) genwf.getUserId();
        if (quotaStaticService.getInstance().getUserBeans().get(userID) != null) if (!quotaStaticService.getInstance().getUserBean(userID).isStorageHasFreeSpace()) {
            System.out.println("Must free disc space before submission!");
            return;
        }
        if (genwf.isCertNeeded()) {
            SZGJob[] jobs = genwf.getJobList();
            String gridName;
            for (int i = 0; i < jobs.length; i++) {
                if (!jobs[i].isFinished()) {
                    gridName = jobs[i].getGrid();
                    if (!jobs[i].getGrid().endsWith(GridConfigs.CLUSTERGRID_NAME) && !this.userCertCheckMG(userID, gridName)) {
                        System.out.println("No valid certificate for " + gridName + "!");
                        throw new APP_SPECException("No valid certificate for " + gridName + "!", this);
                    }
                }
            }
        }
        SZGWorkflow[] wfl = WorkflowUtils.loadIneWorkflows(portalUserName, genwf);
        for (int i = 0; i < wfl.length; ++i) {
            if (wfl[i].getId().toString().equals(new String(eWorkflowName))) {
                wfl[i].rescue();
            }
        }
        System.out.println("eWorkflow successfully rescued.");
    }

    public void starteWorkflow(String portalUserName, String basewfUserID, String eWorkflowName) throws APP_SPECException {
        String errorMessage = null;
        this.log = new String();
        this.cm = SZGCredentialManager.getInstance();
        this.cb = new SZGCredentialBean(portalUserName);
        System.out.println("I'm here!!!");
        if (this.cm != null) {
            this.cb.setCredentials(this.cm.getCredentials(portalUserName));
            try {
                this.cb.setUseThis(this.cm.getUseThis(portalUserName));
            } catch (Exception ex) {
            }
        }
        try {
            loadUsrCert(portalUserName);
        } catch (Exception e) {
        }
        if (genwf.isCertNeeded()) {
            SZGJob[] jobs = genwf.getJobList();
            String gridName;
            for (int i = 0; i < jobs.length; i++) {
                gridName = jobs[i].getGrid();
                if (!jobs[i].getGrid().endsWith(GridConfigs.CLUSTERGRID_NAME) && !this.userCertCheckMG(portalUserName, gridName)) {
                    System.out.println("No valid certificate for " + gridName);
                    APP_SPECException e = new APP_SPECException("No valid certificate for " + gridName);
                    throw e;
                }
            }
        }
        try {
            SZGWorkflow eWorkflow2Submit = null;
            SZGWorkflow[] wfl = WorkflowUtils.loadIneWorkflows(portalUserName, genwf);
            for (int i = 0; i < wfl.length; ++i) {
                if (wfl[i].getId().toString().equals(new String(eWorkflowName))) {
                    eWorkflow2Submit = wfl[i];
                }
            }
            if (!genwf.isSubmitted() || genwf.isStopped() || eWorkflow2Submit != null) {
                eWorkflow2Submit.submit();
                System.out.println(eWorkflow2Submit.getId().toString() + " submitted successfully! ");
            }
        } catch (Exception e) {
            errorMessage = errorMessage == null ? e.getMessage() + "\n" : errorMessage + e.getMessage() + "\n";
        }
        this.setStatus(this.STATUS_RUNNING);
        if (errorMessage != null) {
            throw new APP_SPECException(errorMessage);
        }
    }

    public boolean CheckCert(String userName) {
        return genwf.isCertNeeded();
    }

    protected String loadUsersDirPath() throws Exception {
        return PropertyLoader.getPrefixDir() + "/users/";
    }

    protected String loadUsrCert(String usr) throws Exception {
        String usrDir = this.loadUsersDirPath() + usr + "/";
        File uDir = new File(usrDir);
        if (!uDir.exists()) {
            if (!uDir.mkdirs()) {
                return null;
            }
        }
        FileReader fin = new FileReader(usrDir + usr);
        BufferedReader in = new BufferedReader(fin);
        SZGCredential[] creds = this.cb.getCredentials();
        if (creds == null) {
            try {
                String sor = new String(" ");
                while ((sor = in.readLine()) != null) {
                    int indx = 0;
                    int indv = sor.indexOf(";", indx);
                    String Id = new String(sor.substring(indx, indv));
                    indx = indv + 1;
                    indv = sor.indexOf(";", indx);
                    String DownloadedFrom = new String(sor.substring(indx, indv));
                    indx = indv + 1;
                    indv = sor.indexOf(";", indx);
                    String TimeLeft = new String(sor.substring(indx, indv));
                    indx = indv + 1;
                    indv = sor.indexOf(";#", indx);
                    String Description = new String(sor.substring(indx, indv));
                    indx = indv + 2;
                    indv = sor.indexOf(";", indx);
                    String gsVal = new String(sor.substring(indx, indv));
                    System.out.println("loadUsrCert:Id:" + Id + "_DownloadedFrom:" + DownloadedFrom + "_TimeLeft:" + TimeLeft + "_Description:" + Description + "_gsVal:" + gsVal + "_");
                    SZGStoreKey key = new SZGStoreKey(usr, Id);
                    String cfp;
                    if (0 == gsVal.compareTo(" ")) {
                        System.out.println("loadUsrCert: nincs grighez r");
                        cfp = new String(usrDir + "x509up");
                        InputStream crinstr = new FileInputStream(cfp);
                        this.cm.loadFromFile(crinstr, DownloadedFrom, Integer.parseInt(TimeLeft), key, Description);
                    } else {
                        System.out.println("loadUsrCert: grighez VAN r");
                        cfp = new String(usrDir + "x509up." + gsVal.trim());
                        InputStream crinstr = new FileInputStream(cfp);
                        this.cm.loadFromFile(crinstr, DownloadedFrom, Integer.parseInt(TimeLeft), key, Description);
                        while (indv != -1) {
                            this.cm.setCredentialForGrid(usr, Id, gsVal.trim());
                            indx = indv + 1;
                            indv = sor.indexOf(";", indx);
                            if (indv != -1) {
                                gsVal = new String(sor.substring(indx, indv));
                            }
                        }
                    }
                }
                in.close();
            } catch (Exception e) {
                System.out.println("loadUsrCert ERROR:" + e);
            }
        } else {
            System.out.println("loadUsrCert: - mar betoltve");
        }
        return " ";
    }

    /**
     * Sets the name of the baseworkflow. new workflow for the instance will be generated from it. 
     * @param baseWorkflowName
     * @return void
     */
    public void setBaseWorkflowName(String baseWorkflowName) {
        this.BaseWorkflowName = baseWorkflowName;
    }

    public String getBaseWorkflowName() {
        return this.BaseWorkflowName;
    }

    public Set<String> getNeededCreds(String Username) {
        Set<String> ret = new HashSet<String>();
        SZGCredentialManager cm = null;
        cm = SZGCredentialManager.getInstance();
        for (SZGJob job : genwf.getJobList()) {
            if (cm.getCredentialForGrid(Username, job.getGrid()) == null) {
                ret.add(job.getGrid());
            }
        }
        return ret;
    }

    public void setResource(String userId, String jobId, String grid, String hostname) throws APP_SPECException {
        System.out.println("JobId is : " + jobId);
        System.out.println("Resource is : " + hostname);
        if (!isGlite(userId, jobId)) {
            this.genwf.getJobByName(userId, jobId).setGrid(grid);
            this.genwf.getJobByName(userId, jobId).setHostname(hostname);
            SaveResource(userId, jobId, grid, hostname);
        } else {
            APP_SPECException e = new APP_SPECException("this is a gLite specific job. Resource modification is not supported yet!");
            throw e;
        }
    }

    private boolean isGlite(String userId, String jobId) {
        File f = new File(this.getWorkflowPath(userId) + jobId + "/" + jobId + ".jdl");
        return f.exists();
    }

    private void SaveResource(String userId, String jobId, String grid, String hostname) {
        String gridfile = this.getWorkflowPath(userId) + jobId + ".grid";
        String descfile = this.getWorkflowPath(userId) + "." + jobId + ".desc";
        System.out.println("gridfilepath is : " + gridfile);
        System.out.println("descfilepath is : " + descfile);
        MiscUtils.writeStrToFile(gridfile, grid);
        String[] descContent = MiscUtils.readFileToStr(descfile).split(" ");
        descContent[3] = grid;
        descContent[4] = hostname;
        String newdescContent = "";
        for (String s : descContent) {
            newdescContent += s + " ";
        }
        MiscUtils.writeStrToFile(descfile, newdescContent);
    }

    protected class WorkflowStatusTask extends TimerTask {

        protected APP_SPECInstance project;

        protected int eWfCount;

        protected static final int E_WF_COUNT = 3;

        protected boolean finished = false;

        protected int allJobNumber = 0;

        protected int finishedJobNumber = 0;

        public WorkflowStatusTask(APP_SPECInstance project) {
            this.project = project;
            this.eWfCount = 0;
        }

        public int getallJobNumber() {
            return this.allJobNumber;
        }

        public int getfinishedJobNumber() {
            return this.finishedJobNumber;
        }

        public boolean isfinished() {
            return (allJobNumber == finishedJobNumber);
        }

        @Override
        public void run() {
            while (!finished) {
                boolean wfStatusChanged = false;
                System.out.println("Project (" + project.getName() + ") timer executed!");
                int currentStatus = project.getGeneratedAPP_SPECWorkflow().getStatus();
                System.out.println("After --  status is : " + currentStatus);
                int oldStatus = project.getStatus();
                if (currentStatus != oldStatus) {
                    project.addLogMessage(project.getGeneratedAPP_SPECWorkflow().getId().toString(), "Workflow (" + project.getGeneratedAPP_SPECWorkflow().getId().toString() + ") status changed" + " from " + WF_NAMES[oldStatus].toUpperCase() + " to " + WF_NAMES[currentStatus].toUpperCase() + ".");
                    project.setStatus(currentStatus);
                    if (currentStatus == 3) wfStatusChanged = true;
                }
                String wfName = project.getGeneratedAPP_SPECWorkflow().getId().toString();
                String logMsg = "Workflow (" + project.getGeneratedAPP_SPECWorkflow().getId().toString() + ") statistics changed: ";
                int index = 0;
                for (SZGJob job : project.getGeneratedAPP_SPECWorkflow().getJobList()) {
                    if (((Pair<String, Pair<Integer, Integer>>) (project.JobStatuses.get(index))).getSecond().getSecond() != job.getStatus()) {
                        if (JobStatuses.get(index).getSecond().getSecond() > 0) {
                            project.addLogMessage(wfName, job.getName() + " job's status is changed From " + JOB_STAT_NAMES[JobStatuses.get(index).getSecond().getSecond()].toUpperCase() + " To : " + JOB_STAT_NAMES[job.getStatus()].toUpperCase());
                        } else {
                            project.addLogMessage(wfName, job.getName() + " job's status is changed From Undefined To :  " + job.getStatus());
                        }
                        if (job.getStatus() == 3 && !project.isJobFinished(job.getWorkflowId(), job.getName())) {
                            project.getFinishedjobs().add(job.getWorkflowId() + "@" + job.getName());
                            project.setfinishedjobnumber(1);
                        }
                        project.JobStatuses.set(index, new Pair(job.getName(), new Pair(job.getType(), job.getStatus())));
                    }
                    ++index;
                }
                List<Integer> currentStat = project.parsePSWorkflowStatistics(project.getGeneratedAPP_SPECWorkflow());
                List<Integer> oldStat = ((List<Integer>) (project.worklowStats.get(project.getGeneratedAPP_SPECWorkflow().getId().toString())));
                if (currentStat != null && !currentStat.equals(oldStat)) {
                    if (oldStat == null) {
                        Integer[] empty = { 0, 0, 0, 0, 0, 0 };
                        oldStat = Arrays.asList(empty);
                    }
                    System.out.println(logMsg);
                    System.out.println("Check jobs in eWorkflows...");
                    if (project.getGeneratedAPP_SPECWorkflow().getPSParent() != null) {
                        String workflowID = project.getGeneratedAPP_SPECWorkflow().getPSParent().getId();
                        System.out.println("parent is " + workflowID);
                    }
                    String userID = project.getGeneratedAPP_SPECWorkflow().getUserId();
                    String path = userID + "/" + project.getGeneratedAPP_SPECWorkflow().getId() + "_files";
                    System.out.println("path is : " + path);
                    if (SZGWorkflowList.getInstance(userID + "/" + project.getGeneratedAPP_SPECWorkflow().getId() + "_files").getWorkflowList().length == 0) WorkflowUtils.loadInWorkflows(path, false);
                    SZGWorkflow[] eWFList = SZGWorkflowList.getInstance(userID + "/" + project.getGeneratedAPP_SPECWorkflow().getId() + "_files").getWorkflowList();
                    System.out.println("eWF list length is :" + eWFList.length);
                    project.setEwfslistlenght(eWFList.length);
                    for (int i = 0; i < eWFList.length; ++i) {
                        for (SZGJob job : eWFList[i].getJobList()) {
                            if (job.getStatus() == 3 && !project.isJobFinished(job.getWorkflowId(), job.getName())) {
                                project.getFinishedjobs().add(job.getWorkflowId() + "@" + job.getName());
                                project.setfinishedjobnumber(1);
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(1000 * 30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getNumberofjobs() {
        return this.genwf.getJobList().length;
    }

    public int getNumberoffinishedjobs() {
        return numberofFinishedJobs;
    }

    public void setNumberoffinishedjobs(int numberofFinishedJobs) {
        this.numberofFinishedJobs = numberofFinishedJobs;
    }

    public ArrayList<String> getFinishedjobs() {
        return finishedjobs;
    }

    public void setFinishedjobs(ArrayList<String> finishedjobs) {
        this.finishedjobs = finishedjobs;
    }

    @Override
    protected SZGWorkflow getActualBaseWorkflow() {
        return this.wf;
    }

    @Override
    protected SZGWorkflow getActualComponent() {
        return this.genwf;
    }

    public int compareTo(APP_SPEC_simple_Instance arg0) {
        return 0;
    }

    @Override
    protected void setGeneratedAPP_SPECWorkflow(ArrayList<SZGWorkflow> wf) {
        this.genwf = wf.get(0);
    }

    @Override
    public int getPercent() {
        return 0;
    }

    @Override
    public int getNumberOfJobs(String userId) {
        return this.genwf.getJobList().length;
    }

    public void UpdateStatus() {
        boolean wfStatusChanged = false;
        System.out.println("Project (" + this.getName() + ") timer executed!");
        int currentStatus = this.getGeneratedAPP_SPECWorkflow().getStatus();
        System.out.println("After --  status is : " + currentStatus);
        int oldStatus = this.getStatus();
        if (currentStatus != oldStatus) {
            this.addLogMessage(this.getGeneratedAPP_SPECWorkflow().getId().toString(), "Workflow (" + this.getGeneratedAPP_SPECWorkflow().getId().toString() + ") status changed" + " from " + WF_NAMES[oldStatus].toUpperCase() + " to " + WF_NAMES[currentStatus].toUpperCase() + ".");
            this.setStatus(currentStatus);
            if (currentStatus == 3) wfStatusChanged = true;
        }
        String wfName = this.getGeneratedAPP_SPECWorkflow().getId().toString();
        String logMsg = "Workflow (" + this.getGeneratedAPP_SPECWorkflow().getId().toString() + ") statistics changed: ";
        int index = 0;
        for (SZGJob job : this.getGeneratedAPP_SPECWorkflow().getJobList()) {
            if (((Pair<String, Pair<Integer, Integer>>) (this.JobStatuses.get(index))).getSecond().getSecond() != job.getStatus()) {
                if (JobStatuses.get(index).getSecond().getSecond() > 0) {
                    this.addLogMessage(wfName, job.getName() + " job's status is changed From " + JOB_STAT_NAMES[JobStatuses.get(index).getSecond().getSecond()].toUpperCase() + " To : " + JOB_STAT_NAMES[job.getStatus()].toUpperCase());
                } else this.addLogMessage(wfName, job.getName() + " job's status is changed From Undefined To :  " + job.getStatus());
                if (job.getStatus() == 3 && !this.isJobFinished(job.getWorkflowId(), job.getName())) {
                    this.getFinishedjobs().add(job.getWorkflowId() + "@" + job.getName());
                    this.setfinishedjobnumber(1);
                }
                this.JobStatuses.set(index, new Pair(job.getName(), new Pair(job.getType(), job.getStatus())));
            }
            ++index;
        }
        List<Integer> currentStat = this.parsePSWorkflowStatistics(this.getGeneratedAPP_SPECWorkflow());
        List<Integer> oldStat = ((List<Integer>) (this.worklowStats.get(this.getGeneratedAPP_SPECWorkflow().getId().toString())));
        if (currentStat != null && !currentStat.equals(oldStat)) {
            if (oldStat == null) {
                Integer[] empty = { 0, 0, 0, 0, 0, 0 };
                oldStat = Arrays.asList(empty);
            }
            System.out.println(logMsg);
            System.out.println("Check jobs in eWorkflows...");
            if (this.getGeneratedAPP_SPECWorkflow().getPSParent() != null) {
                String workflowID = this.getGeneratedAPP_SPECWorkflow().getPSParent().getId();
                System.out.println("parent is " + workflowID);
            }
            String userID = this.getGeneratedAPP_SPECWorkflow().getUserId();
            String path = userID + "/" + this.getGeneratedAPP_SPECWorkflow().getId() + "_files";
            System.out.println("path is : " + path);
            if (SZGWorkflowList.getInstance(userID + "/" + this.getGeneratedAPP_SPECWorkflow().getId() + "_files").getWorkflowList().length == 0) WorkflowUtils.loadInWorkflows(path, false);
            SZGWorkflow[] eWFList = SZGWorkflowList.getInstance(userID + "/" + this.getGeneratedAPP_SPECWorkflow().getId() + "_files").getWorkflowList();
            System.out.println("eWF list length is :" + eWFList.length);
            int countNumberoferroreWFs = 0;
            for (int i = 0; i < eWFList.length; ++i) {
                System.out.println("UpdateStatus(): " + "eWorkflowName is : " + eWFList[i].getId());
                for (SZGJob job : eWFList[i].getJobList()) {
                    if (job.getStatus() == 3 && !this.isJobFinished(job.getWorkflowId(), job.getName())) {
                        this.getFinishedjobs().add(job.getWorkflowId() + "@" + job.getName());
                        this.setfinishedjobnumber(1);
                    } else System.out.println("UpdateStatus(): " + "isJobFinished false");
                }
                int eWFstatus = eWFList[i].getStatus();
                if (eWFstatus == SZGWorkflow.STATUS_RESCUE || eWFstatus == SZGWorkflow.STATUS_ERROR) countNumberoferroreWFs++;
            }
            this.setNumberoferrorewfs(countNumberoferroreWFs);
            this.setEwfslistlenght(eWFList.length);
            System.out.println("UpdateStatus(): " + "eWF in rescue/error status :" + countNumberoferroreWFs);
            this.addLogMessage(wfName, "UpdateStatus(): " + "There are " + countNumberoferroreWFs + " eWf(s) in rescue/error status.");
        }
    }
}

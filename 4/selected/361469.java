package hu.sztaki.lpds.APP_SPEC.services;

import hu.sztaki.lpds.APP_SPEC.exceptions.APP_SPECException;
import hu.sztaki.lpds.SuperWorkflow.beans.ConnectionBean;
import hu.sztaki.lpds.SuperWorkflow.beans.SuperWorkflowBean;
import hu.sztaki.lpds.SuperWorkflow.services.SuperWorkflowManager;
import hu.sztaki.lpds.SuperWorkflow.utils.StatusConstants;
import hu.sztaki.lpds.pgportal.portlets.compiler.Pair;
import hu.sztaki.lpds.pgportal.portlets.credential.SZGCredentialBean;
import hu.sztaki.lpds.pgportal.services.utils.*;
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
import hu.sztaki.lpds.pgportal.wfeditor.client.pstudy.*;
import hu.sztaki.lpds.pgportal.wfeditor.client.pstudy.autogen.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.log4j.Logger;
import org.gridsphere.services.core.user.*;
import org.gridsphere.provider.portletui.beans.FileInputBean;

/**
 * Class to store and manage Instances that generated from a specified published application
 * @author  akos balasko MTA SZTAKI
 */
public class APP_SPEC_concat_Instance extends APP_SPECInstance {

    public static final int SW_STATUS_INIT = 0;

    public static final int SW_STATUS_RUNNING = 1;

    public static final int SW_STATUS_ABORTED = 3;

    public static final int SW_STATUS_RESCUE = 3;

    public static final int SW_STATUS_FINISHED = 3;

    public static final int COMPONENT_STATUS_UPLOADED = 0;

    public static final int COMPONENT_STATUS_SUBMITTED = 1;

    public static final int COMPONENT_STATUS_ABORTED = 2;

    public static final int COMPONENT_STATUS_FINISHED = 3;

    public static final int COMPONENT_STATUS_ERROR = 4;

    public static final int COMPONENT_STATUS_INCOMPLETE = 5;

    public static final int COMPONENT_STATUS_RUNNING = 6;

    public static final int COMPONENT_STATUS_RESCUE = 7;

    public APP_SPEC_concat_Instance() {
    }

    private SuperWorkflowBean generated_superworkflow;

    public SuperWorkflowBean getGenerated_superworkflow() {
        return generated_superworkflow;
    }

    public void setGenerated_superworkflow(SuperWorkflowBean generatedSuperworkflow) {
        generated_superworkflow = generatedSuperworkflow;
    }

    public SuperWorkflowBean getSource_superworkflow() {
        return source_superworkflow;
    }

    public void setSource_superworkflow(SuperWorkflowBean sourceSuperworkflow) {
        source_superworkflow = sourceSuperworkflow;
    }

    private SuperWorkflowBean source_superworkflow;

    /**
     * Constructor of the Instance class. 
     * @param user - login name of the user
     * @param name - name of the Instance 
     * @param desc - description of Instance
     * @param baseworkflow - Name of the Published Workflow
     */
    public APP_SPEC_concat_Instance(String usr_id, String role, String rootID, String name, String desc, String baseworkflow) {
        this.userId = usr_id;
        this.rootID = rootID;
        this.name = name;
        this.group = role;
        this.description = desc;
        PersistenceManager.initPM(rootID);
        finishedjobs = new ArrayList<String>();
        logger.debug("BASEWORKFLOW IS : " + baseworkflow + " ; rootID is : " + rootID);
        try {
            this.source_superworkflow = SuperWorkflowManager.getInstance().getSuperWorkflowBean(rootID, baseworkflow);
            for (int i = 0; i < source_superworkflow.getSuperworkflow().size(); ++i) {
                System.out.println("Base component workflow is : " + source_superworkflow.getSuperworkflow().get(i).getFirst().getFirst());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("BaseWorkflow Saved : " + baseworkflow);
        inputuploadstatus = new ArrayList<Boolean>();
        this.status = STATUS_INCOMPLETE;
        this.log = "";
        this.workflowStatuses = new HashMap<String, Integer>();
        this.worklowStats = new HashMap<String, List<Integer>>();
        this.JobStatuses = new ArrayList<Pair<String, Pair<Integer, Integer>>>();
        cm = SZGCredentialManager.getInstance();
    }

    public void copySWdescriptors() throws Exception {
        File dir = new File(PropertyLoader.getPrefixDir() + "/users/" + this.userId + "/.superworkflows");
        if (!dir.exists()) dir.mkdir();
        String source_path = PropertyLoader.getPrefixDir() + "/users/" + this.rootID + "/.superworkflows/" + this.source_superworkflow.getName();
        String dest_path = PropertyLoader.getPrefixDir() + "/users/" + this.userId + "/.superworkflows/" + this.generated_superworkflow.getName();
        String desc = MiscUtils.readFileToStr(source_path);
        MiscUtils.writeStrToFile(dest_path, desc);
        String source_conn_path = source_path + "_connections";
        String dest_conn_path = dest_path + "_connections";
        desc = MiscUtils.readFileToStr(source_conn_path);
        MiscUtils.writeStrToFile(dest_conn_path, desc);
    }

    /**
     * Generates a new Workflow from BaseWorkflow. It Copies the necessarry files, and modify them to be able the use it correctly.
     *  @param userID - the ID of the User. Workflow will be generated for this user's current instance  
     *  @throws APP_SPECException - if generalization fails, exception will be thrown
     * @return void
     */
    public void generateWorkflow(String userID) throws APP_SPECException {
        SuperWorkflowBean generated_workflow = new SuperWorkflowBean(this.getName());
        this.setGenerated_superworkflow(generated_workflow);
        for (int i = 0; i < this.source_superworkflow.getSuperworkflow().size(); ++i) {
            String workflowname = this.source_superworkflow.getSuperworkflow().get(i).getFirst().getFirst();
            wf = SZGWorkflowList.getInstance(rootID).getWorkflow(workflowname);
            String new_component_name = this.getName() + "_" + workflowname;
            String next_component_name = this.source_superworkflow.getSuperworkflow().get(i).getFirst().getSecond();
            String new_next_component_name = this.getName() + "_" + next_component_name;
            int howmany = this.source_superworkflow.getSuperworkflow().get(i).getSecond().getFirst();
            System.out.println("-");
            ParameterKeyList keyList = new ParameterKeyList();
            System.out.println("INITIALLY BASEWF IS IN isPS: " + wf.isPS() + " state ");
            for (SZGJob job : wf.getJobList()) {
                System.out.println("Job: " + job.getName() + "Type is : " + job.getType() + " Status : " + job.getStatus());
            }
            Date date = GregorianCalendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
            try {
                System.out.println("- Component name : " + new_component_name);
                File wfDir = getWorkflowDir(userID, new_component_name);
                if (wfDir.exists()) {
                    MiscUtils.deleteFileRecursively(wfDir);
                }
                MiscUtils.copyFileRecursively(getWorkflowDir(wf), wfDir, true);
                File wrkFile = new File(getWorkflowDir(userID, new_component_name), wf.getId() + "_remote.wrk");
                wrkFile.renameTo(new File(getWorkflowDir(userID, new_component_name), new_component_name + "_remote.wrk"));
                if (wf.getJobsByType(SZGJob.AUTO_GEN_TYPE).length != 0) {
                    for (SZGJob genJob : wf.getJobsByType(SZGJob.AUTO_GEN_TYPE)) {
                        AutoGeneratorJob job = this.getAutoGeneratorJob(wf, genJob.getName());
                        System.out.println("INPUT TEXT : " + job.getInputText());
                        writeAutogeneratorJob(wfDir, job);
                    }
                }
                System.out.println("- after creating autogenerator");
                modifyWorkflowName(userID, new_component_name);
                System.out.println("- after creating autogenerator - after modifying workflow name");
                modifySEPath(userID, new_component_name);
                System.out.println("- after creating autogenerator - after modifying SE path");
                WorkflowUtils.loadInWorkflow(userID, new_component_name, true);
                quotaStaticService.getInstance().reallocateUserWorkflowSpace(userID, new_component_name);
                System.out.println("- after creating autogenerator - after quoting");
                genwf = SZGWorkflowList.getInstance(userID).getWorkflow(new_component_name);
                System.out.println("- after creating autogenerator - after modifying SE path genwf");
                this.APP_SPECWorkflowName = genwf.getId().toString();
                System.out.println("- after appspecworkflowname : " + this.APP_SPECWorkflowName);
                this.setStatus(genwf.getStatus());
                System.out.println("- after appspecworkflowname -after status");
                String path = PropertyLoader.getPrefixDir() + "users/" + userID + "/" + new_component_name + "_files";
                File deletepublished = new File(path + "/" + published_property);
                System.out.println("PATH IS : " + path + "/" + published_property);
                deletepublished.delete();
                System.out.println("after all");
            } catch (Exception e) {
                System.out.println("[LOGGER ERROR] " + "Cannot generate workflow  " + e.getMessage());
                throw new APP_SPECException("Cannot generate workflow!", e);
            }
        }
        for (int i = 0; i < this.source_superworkflow.getSuperworkflow().size(); ++i) {
            String workflowname = this.source_superworkflow.getSuperworkflow().get(i).getFirst().getFirst();
            wf = SZGWorkflowList.getInstance(rootID).getWorkflow(workflowname);
            String new_component_name = this.getName() + "_" + workflowname;
            String next_component_name = this.source_superworkflow.getSuperworkflow().get(i).getFirst().getSecond();
            String new_next_component_name = "";
            if (next_component_name == null) new_next_component_name = null; else new_next_component_name = this.getName() + "_" + next_component_name;
            System.out.println("new component name : " + new_component_name);
            System.out.println("new next component name : " + new_next_component_name);
            int howmany = this.source_superworkflow.getSuperworkflow().get(i).getSecond().getFirst();
            this.generated_superworkflow.AddWorkflow(userID, i, new_component_name, howmany, new_next_component_name);
        }
        for (int i = 0; i < this.source_superworkflow.getSuperworkflow().size(); ++i) {
            String workflowname = this.source_superworkflow.getSuperworkflow().get(i).getFirst().getFirst();
            wf = SZGWorkflowList.getInstance(rootID).getWorkflow(workflowname);
            String new_component_name = this.getName() + "_" + workflowname;
            String next_component_name = this.source_superworkflow.getSuperworkflow().get(i).getFirst().getSecond();
            int howmany = this.source_superworkflow.getSuperworkflow().get(i).getSecond().getFirst();
            String new_next_component_name = "";
            if (next_component_name == null) {
                if (howmany > 1) {
                    new_next_component_name = new_component_name;
                } else {
                    new_next_component_name = null;
                }
            } else {
                if (howmany > 1) {
                    new_next_component_name = new_component_name;
                } else {
                    new_next_component_name = this.getName() + "_" + next_component_name;
                }
            }
            System.out.println("new component name : " + new_component_name);
            System.out.println("new next component name : " + new_next_component_name);
            Enumeration<String> connectionkeys = this.source_superworkflow.getSuperworkflow().get(i).getSecond().getSecond().keys();
            while (connectionkeys.hasMoreElements()) {
                String key = connectionkeys.nextElement();
                ConnectionBean conn_bean = this.source_superworkflow.getSuperworkflow().get(i).getSecond().getSecond().get(key);
                conn_bean.setSourcewf(new_component_name);
                conn_bean.setDestwf(new_next_component_name);
                this.generated_superworkflow.AddConnection(conn_bean);
            }
        }
        try {
            SuperWorkflowManager.getInstance().AddSuperWorkflow(userId, this.generated_superworkflow);
        } catch (Exception e) {
            e.printStackTrace();
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

    public void setNumberofjobs(int numberofJobs) {
        this.numberofJobs = numberofJobs;
    }

    public int getNumberoffinishedjobs() {
        System.out.println("Numberoffinishedjobs:userid:(" + userId + ") sw name :" + this.getName() + " ... maybe : " + this.getAPP_SPECWorkflowName());
        int number = 0;
        try {
            ArrayList<ArrayList<Pair<Integer, Integer>>> stats = SuperWorkflowManager.getInstance().getSuperWorkflowStatuses_details(userId, this.getName());
            for (int i = 0; i < stats.size(); ++i) {
                for (int j = 0; j < stats.get(i).size(); ++j) {
                    if (stats.get(i).get(j).getFirst() == 3) {
                        number += stats.get(i).get(j).getSecond();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return number;
    }

    public void setNumberoffinishedjobs(int numberofFinishedJobs) {
        this.numberofFinishedJobs = numberofFinishedJobs;
    }

    public ArrayList<String> getFinishedjobs() {
        System.out.println("These are the finished jobs : ");
        for (String str : finishedjobs) {
            System.out.println(str);
        }
        System.out.println(".......");
        return finishedjobs;
    }

    public void setFinishedjobs(ArrayList<String> finishedjobs) {
        this.finishedjobs = finishedjobs;
    }

    @Override
    protected SZGWorkflow getActualBaseWorkflow() {
        return null;
    }

    @Override
    protected SZGWorkflow getActualComponent() {
        SuperWorkflowBean bean = this.getGenerated_superworkflow();
        System.out.println("get actual component concat");
        System.out.println("is null?");
        if (bean.getSuperworkflow() == null) {
            System.out.println("yes");
        } else System.out.println("no");
        for (int i = 0; i < bean.getSuperworkflow().size(); ++i) {
            String act_wf_name = bean.getSuperworkflow().get(i).getFirst().getFirst();
            System.out.println("get actual component concat act wf_name is : " + act_wf_name);
            System.out.println("real component wf name is : " + act_wf_name + " user is : " + userId);
            System.out.println("status is : " + SZGWorkflowList.getInstance(userId).getWorkflow(act_wf_name).getStatus());
            if (SZGWorkflowList.getInstance(userId).getWorkflow(act_wf_name).getStatus() == COMPONENT_STATUS_UPLOADED) {
                System.out.println("actual component has status init ...returning " + act_wf_name);
                return SZGWorkflowList.getInstance(userId).getWorkflow(act_wf_name);
            }
        }
        System.out.println("return null");
        return null;
    }

    public int compareTo(APP_SPEC_simple_Instance arg0) {
        return 0;
    }

    @Override
    protected void setGeneratedAPP_SPECWorkflow(ArrayList<SZGWorkflow> wf) {
        this.wf = wf.get(0);
    }

    protected void setJobStatuses(String userId) {
    }

    public int getNumberofjobs() {
        int number_of_jobs = 0;
        for (int i = 0; i < this.getGenerated_superworkflow().getSuperworkflow().size(); ++i) {
            System.out.println("component workflow is : " + this.getGenerated_superworkflow().getSuperworkflow().get(i).getFirst().getFirst());
            int instance_jobs = SZGWorkflowList.getInstance(this.userId).getWorkflow(this.getGenerated_superworkflow().getSuperworkflow().get(i).getFirst().getFirst()).getJobList().length * this.getGenerated_superworkflow().getSuperworkflow().get(i).getSecond().getFirst();
            number_of_jobs += instance_jobs;
        }
        System.out.println("number of jobs in superworkflow (" + this.getName() + ") :" + number_of_jobs);
        return number_of_jobs;
    }

    @Override
    public int getPercent() {
        return 0;
    }

    @Override
    public int getNumberOfJobs(String userId) {
        return 0;
    }
}

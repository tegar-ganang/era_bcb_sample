package hu.sztaki.lpds.USGIME;

import hu.sztaki.lpds.USGIME.services.*;
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
import hu.sztaki.lpds.pgportal.wfeditor.client.pstudy.ParameterKeyList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
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
import org.gridlab.gridsphere.portlet.User;

/**
 *
 * @author balasko
 */
public class USGIMEInstance implements Comparable<USGIMEInstance> {

    public static final int STATUS_INCOMPLETE = 0;

    public static final int STATUS_INIT = 1;

    public static final int STATUS_STOPPED = 2;

    public static final int STATUS_RUNNING = 3;

    public static final int STATUS_FINISHED = 4;

    public static final int STATUS_ERROR = 5;

    public static final int STATUS_PAUSED = 6;

    public static final int NORMAL_TYPE = 0;

    public static final int COLLECTOR_TYPE = 1;

    public static final int NORMAL_GEN_TYPE = 2;

    public static final int AUTO_GEN_TYPE = 3;

    public static final int GENERATOR_TYPE = 5;

    private String published_property = ".Published";

    public static final String[] WF_NAMES = { "init", "submitted", "aborted", "finished", "error", "incomplete", "running", "rescue" };

    static final String[] WF_COLORS = { "white", "orange", "red", "lime", "blue", "white", "red", "brown" };

    public static final String[] JOB_STAT_NAMES = { "init", "submitted", "running", "finished", "error", "system-on-hold", "migrated", "waiting", "sheduled" };

    private SZGCredentialManager cm = null;

    private SZGCredentialBean cb = null;

    private HashMap<String, Integer> workflowStatuses;

    private HashMap<String, List<Integer>> worklowStats;

    private USGIMEBean usgimebean;

    private String USGIMEWorkflowName;

    private String BaseWorkflowName;

    private SZGWorkflow wf;

    private SZGWorkflow genwf;

    private String name;

    private String log;

    private String description;

    private String oid;

    private Integer status;

    private String statusstr;

    private String statuscolor;

    private String Creatingtime;

    private String useroid = null;

    private int numberofinputs;

    private ArrayList<Boolean> inputuploadstatus;

    private ArrayList<Pair<String, Pair<Integer, Integer>>> JobStatuses;

    private int numberofJobs;

    private int numberofFinishedJobs = 0;

    private Timer timer;

    public USGIMEInstance() {
    }

    /**
	 * Constructor of the Class. 
	 * @param user - Object of User
	 * @param name - Name of the Project 
	 * @param desc - Description of Project
	 * @param baseworkflow - Name of the Base Workflow
	 */
    public USGIMEInstance(String rootID, User user, String name, String desc, String baseworkflow) throws USGIMEException {
        SZGWorkflow[] wflist = SZGWorkflowList.getInstance(user.getUserID()).getWorkflowList();
        for (int i = 0; i < wflist.length; ++i) {
            if (wflist[i].getId().equals(new String(name))) {
                throw new USGIMEException("Normal Workflow Exists with this name!");
            }
        }
        this.name = name;
        this.description = desc;
        Date d = new Date();
        DateFormat df = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
        Creatingtime = df.format(d);
        PersistenceManager.initPM(rootID);
        this.BaseWorkflowName = baseworkflow;
        System.out.println("BASEWORKFLOW IS : " + baseworkflow + " ; rootID is : " + rootID);
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

    public USGIMEBean getUsgimeBean() {
        return this.usgimebean;
    }

    /**
	 * Sets List of jobStatuses to the current stage.
	 */
    public void setJobStatuses() {
        System.out.println("genwf jobs are");
        for (SZGJob job : this.genwf.getJobList()) {
            System.out.println("Name is : " + job.getName());
            System.out.println("Type is : " + job.getType());
            System.out.println("Status is : " + job.getStatus());
            if (this.JobStatuses == null) {
                this.JobStatuses = new ArrayList<Pair<String, Pair<Integer, Integer>>>();
            }
            this.JobStatuses.add(new Pair(job.getName(), new Pair(job.getType(), job.getStatus())));
        }
    }

    public List<Integer> getWorkflowStats(String wfname) {
        return (this.worklowStats.get(wfname));
    }

    public ArrayList<Pair<String, Pair<Integer, Integer>>> getJobStatuses() {
        return this.JobStatuses;
    }

    /**
	 * Returns the path of the Workflows for a User specified in argument
	 * @param user - Object of User
	 * @return Path of workflows
	 */
    public String getWorkflowPath(User user) {
        String ret = PropertyLoader.getPrefixDir() + "/users/" + user.getUserID() + "/" + genwf.getId().toString() + "/";
        System.out.println(ret);
        return ret;
    }

    /**
	 * Creates new object to store Stats. It is Useful, when user creates new Project and logout
	 */
    public void initLogs() {
        if (this.worklowStats == null) this.worklowStats = new HashMap<String, List<Integer>>();
    }

    public int compareTo(USGIMEInstance o) {
        return this.name.compareTo(o.getName());
    }

    /**
	 * Reads Stored logs into log.
	 */
    public void ReadStoredLog() {
        try {
            File wfDir = getWorkflowDir(wf.getUserId().toString(), name);
            this.setLog(MiscUtils.readFileToStr(wfDir + "/.AppWF_log.txt"));
        } catch (Exception e) {
            System.out.println("Warning in ReadStoredLog - Maybe new workflow, Logfile does not exists! ");
        }
    }

    public int getNumberOfJobs() {
        if (genwf.isPS() && genwf != null && genwf.hasPSStatisticfile()) {
            Object oPS = null;
            oPS = LogFileUtil.getPSStatistics(genwf.getUserId().toString(), genwf.getId().toString()).get(0);
            int eWorkflows = Integer.parseInt(String.valueOf(oPS));
            int PSJobs = 0;
            for (SZGJob job : genwf.getJobsByType(this.NORMAL_TYPE)) {
                if (!job.getName().equals(new String("PS=PROPS1"))) ++PSJobs;
            }
            return PSJobs * eWorkflows + genwf.getJobsByType(this.AUTO_GEN_TYPE).length + genwf.getJobsByType(this.NORMAL_GEN_TYPE).length + genwf.getJobsByType(this.COLLECTOR_TYPE).length;
        } else {
            return genwf.getJobList().length;
        }
    }

    public void setfinishedjobnumber(int abs) {
        this.numberofFinishedJobs += abs;
    }

    public int getfinishedjobnumber() {
        return numberofFinishedJobs;
    }

    /**
	 *  Sets status of ExtendedWorkflow Object depending on the status of the generated workflow.
	 * 
	 */
    public void updateInstanceStatus() {
        if (this.genwf != null && this.genwf.isSubmitted()) {
            if (this.genwf.getStatus() == this.genwf.STATUS_INCOMPLETE || this.genwf.getStatus() == this.genwf.STATUS_UPLOADED) {
                this.status = this.STATUS_INIT;
            }
            if (this.genwf.getStatus() == this.genwf.STATUS_ABORTED || this.genwf.getStatus() == this.genwf.STATUS_ERROR || this.genwf.getStatus() == this.genwf.STATUS_RESCUE) {
                this.status = this.STATUS_ERROR;
            }
            if (this.genwf.getStatus() == this.genwf.STATUS_SUBMITTED || this.genwf.getStatus() == this.genwf.STATUS_RUNNING) {
                this.status = this.STATUS_RUNNING;
            }
            if (this.genwf.getStatus() == this.genwf.STATUS_FINISHED) {
                this.timer.cancel();
                this.setStatus(this.genwf.STATUS_FINISHED);
            }
        } else this.status = this.STATUS_INIT;
    }

    /**
	 *  Parses the PS Workflow Statistic file
	 * @param wf - Object of the Workflow
	 * @return List of the parsed statuses
	 */
    private List<Integer> parsePSWorkflowStatistics(SZGWorkflow wf) {
        List<Integer> ret = null;
        if (wf.hasPSStatisticfile()) {
            ret = new ArrayList<Integer>();
            List<String> stats = LogFileUtil.getPSStatistics(wf.getUserId().toString(), wf.getId().toString());
            for (int i = 0; i <= 5; i++) {
                ret.add(Integer.valueOf(stats.get(i)));
            }
        }
        return ret;
    }

    public void addLogMessage(String msg) {
        addLogMessage(null, msg);
    }

    public void clearLog() {
        this.log = "";
    }

    public void setNumberofInputs(int noi) {
        this.numberofinputs = noi;
    }

    public int getNumberofInputs() {
        return this.numberofinputs;
    }

    public void setInputUploadStatus() {
        for (int i = 0; i < numberofinputs; ++i) {
            inputuploadstatus.add(false);
        }
    }

    public String getUSGIMEWorkflowName() {
        return USGIMEWorkflowName;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return this.status;
    }

    public void setUSGIMEWorkflowName(String USGIMEWorkflowName) {
        this.USGIMEWorkflowName = USGIMEWorkflowName;
    }

    public USGIMEInstance(SZGWorkflow wf) {
        this.wf = wf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getUseroid() {
        return useroid;
    }

    public void setUseroid(String useroid) {
        this.useroid = useroid;
    }

    public SZGWorkflow getUSGIMEWorkflow() {
        return wf;
    }

    public void setUSGIMEWorkflow(SZGWorkflow wf) {
        this.wf = wf;
    }

    public SZGWorkflow getGeneratedUSGIMEWorkflow() {
        return genwf;
    }

    public void setGeneratedUSGIMEWorkflow(SZGWorkflow wf) {
        this.genwf = wf;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    /**
	 * Adds a log message, and writes out to .AppWF_log.txt in the Directory of the Workflow
	 * @param wfName
	 * @param msg
	 */
    public void addLogMessage(String wfName, String msg) {
        Date date = GregorianCalendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("[yy.MM.dd HH:mm:ss]");
        String stamp = formatter.format(date);
        String logMsg = stamp + " " + msg + "\n";
        this.log += logMsg;
        File wfDir = getWorkflowDir(genwf.getUserId().toString(), this.getGeneratedUSGIMEWorkflow().getId().toString());
        File Logfile = new File(wfDir, ".AppWF_log.txt");
        String logfromfile = "";
        if (Logfile.exists()) {
            logfromfile = MiscUtils.readFileToStr(wfDir + "/.AppWF_log.txt");
        } else {
            try {
                Logfile.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException in addLogMessage");
            }
        }
        logfromfile = logfromfile + logMsg;
        MiscUtils.writeStrToFile(Logfile.getAbsolutePath(), logfromfile);
    }

    public String getInputtext(String jobname, String portname, String filename) {
        try {
            File wfdir = this.getWorkflowDir(genwf);
            File file = new File(wfdir.getAbsolutePath() + "/" + jobname + "/" + "portname" + "/" + filename);
            return MiscUtils.readFileToStr(file.getAbsolutePath());
        } catch (Exception e) {
            throw new USGIMEException("getting input text Failed!");
        }
    }

    public void setInputtext(String jobname, String portname, String filename, String text) {
        try {
            File wfdir = this.getWorkflowDir(genwf);
            File file = new File(wfdir.getAbsolutePath() + "/" + jobname + "/" + "portname" + "/" + filename);
            MiscUtils.writeStrToFile(file.getAbsolutePath(), text);
        } catch (Exception e) {
            throw new USGIMEException("setting input text Failed!");
        }
    }

    /**
	 * Generates a new Workflow from BaseWorkflow. It Copies the necessarry files, and modify them to be able the use it correctly.
	 *  @param userID - the ID of the User. Workflow will be generated for this user's current instance  
	 *  @throws USGIMEException - if generalization fails, exception will be thrown
	 * @return void
	 */
    public void generateWorkflow(String userID, ArrayList<String> ceList, ArrayList<String> seList) throws USGIMEException {
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
            modifyWorkflowName(userID, wfName);
            String path = PropertyLoader.getPrefixDir() + "users/" + userID + "/" + wfName + "_files";
            File deletepublished = new File(path + "/" + published_property);
            System.out.println("PATH IS : " + path + "/" + published_property);
            deletepublished.delete();
            this.usgimebean = new USGIMEBean(userID, this.USGIMEWorkflowName, ceList, seList, new Date().getTime());
            String newline = System.getProperty("line.separator");
            String celistString = "";
            for (int i = 0; i < ceList.size(); ++i) {
                celistString = celistString + ceList.get(i) + newline;
            }
            String selistString = "";
            for (int i = 0; i < seList.size(); ++i) {
                selistString = selistString + seList.get(i) + newline;
            }
            MiscUtils.writeStrToFile(path + "/.ceList", celistString);
            MiscUtils.writeStrToFile(path + "/.seList", selistString);
            generateJobs(userID, wfName, ceList, seList);
            WorkflowUtils.loadInWorkflow(userID, wfName);
            if (SZGWorkflowList.getInstance(userID).getWorkflow(wfName).getJobByName(userID, "Job_TEMPLATE") != null) {
                Object jobId = SZGWorkflowList.getInstance(userID).getWorkflow(wfName).getJobByName(userID, "Job_TEMPLATE").getId();
                try {
                    SZGWorkflowList.getInstance(userID).getWorkflow(wfName).deleteJob(jobId);
                } catch (Exception e) {
                }
            }
            for (SZGJob job : SZGWorkflowList.getInstance(userID).getWorkflow(wfName).getJobList()) {
                System.out.println("job is : " + job.getId() + " name is : " + job.getName());
                job.setIsToBeMonitored(true);
            }
            quotaStaticService.getInstance().reallocateUserWorkflowSpace(userID, wfName);
            genwf = SZGWorkflowList.getInstance(userID).getWorkflow(wfName);
            this.addLogMessage("Workflow Generated");
            this.setStatus(genwf.getStatus());
        } catch (Exception e) {
            System.out.println("[LOGGER ERROR] " + "Cannot generate workflow  " + e.getMessage());
            e.printStackTrace();
            throw new USGIMEException("Cannot generate workflow!", e);
        }
    }

    public static final File getJobDir(String userId, String wfname, String jobName) {
        File ret = null;
        if (wfname != null) {
            ret = new File(PropertyLoader.getPrefixDir() + "/users/" + userId + "/" + wfname + "_files/" + jobName + "/");
        }
        return ret;
    }

    public void GenerateDescFiles(SZGWorkflow templatewf, String templatejobname, String userId, String workflowname, String jobname) {
        String path = PropertyLoader.getPrefixDir() + "/users/" + templatewf.getUserId().toString() + "/" + templatewf.getId().toString() + "_files/";
        String destpath = PropertyLoader.getPrefixDir() + "/users/" + userId + "/" + workflowname + "_files/";
        MiscUtils.writeStrToFile(destpath + jobname + ".grid", MiscUtils.readFileToStr(path + templatejobname + ".grid"));
        MiscUtils.writeStrToFile(destpath + jobname + ".owner", userId);
        String templatejobdesc = WorkflowUtils.loadJobDesc(templatewf.getUserId().toString(), templatewf.getId().toString(), templatejobname);
        String grid = templatejobdesc.split(" ")[3];
        String host = templatejobdesc.split(" ")[4];
        String monitor = templatejobdesc.split(" ")[5];
        String type = templatejobdesc.split(" ")[6];
        String desc = userId + " " + workflowname + " " + jobname + " " + grid + " " + host + " " + monitor + " " + type;
        MiscUtils.writeStrToFile(destpath + "." + jobname + ".desc", desc);
    }

    public void generateJobs(String userId, String wfname, ArrayList<String> ceList, ArrayList<String> seList) throws USGIMEException {
        PersistenceManager.initPM(userId);
        String templatejobName = "Job_TEMPLATE";
        String developerId = this.getUSGIMEWorkflow().getUserId().toString();
        String developerwfname = this.getBaseWorkflowName();
        SZGJob templateJob = this.getUSGIMEWorkflow().getJobByName(developerId, templatejobName);
        String gridName = templateJob.getGrid();
        int type = templateJob.getType();
        int space = 92;
        String newline = System.getProperty("line.separator");
        String wrkcontent = "workflow \"" + wfname + "\"" + newline + "{" + newline + "}" + newline + "{";
        ArrayList<String> jobcoordinates = new ArrayList();
        jobcoordinates.add("\"Job_TEMPLATE\" 10 19");
        for (int i = 0; i < ceList.size(); ++i) {
            String hostname = templateJob.getHostname();
            String jobName = ceList.get(i).toString();
            GenerateDescFiles(this.getUSGIMEWorkflow(), templatejobName, userId, wfname, jobName);
            MiscUtils.copyFileRecursively(getJobDir(developerId, developerwfname, templatejobName), getJobDir(userId, wfname, jobName), true);
            File jdlFile = new File(getJobDir(userId, wfname, jobName) + "/Job_TEMPLATE.jdl");
            String jdlText = MiscUtils.readFileToStr(jdlFile.getAbsolutePath());
            MiscUtils.writeStrToFile(getJobDir(userId, wfname, jobName) + "/" + jobName + ".jdl", jdlText);
            File jdl = new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname.toString() + "_files/" + jobName + "/" + jobName + ".jdl");
            String jdltext = MiscUtils.readFileToStr(jdl.getAbsolutePath());
            jdltext = jdltext.replaceAll("<TEMPLATECE>", ceList.get(i).toString());
            jdltext = jdltext.replaceAll("Arguments = \"<TEMPLATEARG>\"", "Arguments = \"" + userId + " " + ceList.get(i) + " " + gridName.split("_")[0] + "\"");
            jdltext = jdltext.replaceAll("Job_TEMPLATE.sh", jobName + ".sh");
            MiscUtils.writeStrToFile(jdl.getAbsolutePath(), jdltext);
            File jobdir = getJobDir(userId, wfname, jobName);
            wrkcontent = wrkcontent + "\"" + jobName + "\"" + " SEQ_PORTAL (is_instrumented=false;monitor=off) \"" + jobdir.getAbsolutePath() + "/wrapper_scr.sh\"" + newline + "{" + newline + "\"" + userId + " " + ceList.get(i).toString() + " " + gridName.split("_")[0] + "\"" + newline + "}" + "\"LINUX\"" + newline + "{" + newline + "\"default:/jobmanager\"" + newline + "}" + newline + " 0 \"" + jobdir + "/0/inputses.txt\" (file_type=local;original_path=\"E:\\USGIME\\input\\inputses.txt\";is_copy=true) PERMANENT INPUT" + newline + "1 \"" + jobdir + "/1/results\" (file_type=local;original_path=\"results\";is_copy=true) PERMANENT OUTPUT" + newline + newline;
            int previousX = Integer.parseInt(jobcoordinates.get(jobcoordinates.size() - 1).split(" ")[1]);
            int previousY = Integer.parseInt(jobcoordinates.get(jobcoordinates.size() - 1).split(" ")[2]);
            int firstX = Integer.parseInt(jobcoordinates.get(0).split(" ")[1]);
            int firstY = Integer.parseInt(jobcoordinates.get(0).split(" ")[2]);
            System.out.println("PREVIOUSX : " + previousX);
            System.out.println("PREVIOUSY : " + previousY);
            System.out.println("firstX : " + firstX);
            System.out.println("firstY : " + firstY);
            if ((previousX - firstX) / space < 6) {
                Integer newX = previousX + space;
                System.out.println("newX : " + newX);
                jobcoordinates.add("\"" + jobName + "\" " + newX.toString() + " " + previousY);
            } else {
                Integer newY = previousY + space;
                System.out.println("newY : " + newY);
                jobcoordinates.add("\"" + jobName + "\" " + firstX + " " + newY.toString());
            }
            copySElist(seList, userId, wfname, jobName, "0");
        }
        jobcoordinates.remove(0);
        wrkcontent = wrkcontent + newline + "}" + newline + "{" + newline;
        for (int i = 0; i < jobcoordinates.size(); ++i) {
            wrkcontent = wrkcontent + jobcoordinates.get(i) + newline;
        }
        wrkcontent = wrkcontent + "}";
        MiscUtils.writeStrToFile(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/" + wfname + "_remote.wrk", wrkcontent);
        System.out.println("PAth is : " + PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/" + templatejobName);
        MiscUtils.deleteFileRecursively(new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/" + templatejobName));
        File dgrid = new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/" + templatejobName + ".grid");
        if (dgrid.delete()) {
            System.out.println("gridfile deleted");
        }
        File downer = new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/" + templatejobName + ".owner");
        if (downer.delete()) {
            System.out.println("ownerfile deleted");
        }
        File dstatus = new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/" + templatejobName + ".status");
        if (dstatus.delete()) {
            System.out.println("statusfile deleted");
        }
        File ddesc = new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname + "_files/." + templatejobName + ".desc");
        if (ddesc.delete()) {
            System.out.println("descfile deleted");
        }
    }

    private void copySElist(ArrayList<String> seList, String userId, String wfname, String jobName, String port) {
        String newline = System.getProperty("line.separator");
        String seListstr = "";
        for (int j = 0; j < seList.size(); ++j) {
            seListstr = seListstr + seList.get(j) + newline;
        }
        File inputF;
        inputF = new File(PropertyLoader.getPrefixDir() + "users/" + userId + "/" + wfname.toString() + "_files/" + jobName + "/" + port + "/inputses.txt");
        MiscUtils.writeStrToFile(inputF.getAbsolutePath(), seListstr);
    }

    private void modifyWorkflowPath(String userID, String wfName) {
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
    private void modifyWorkflowName(String userID, String wfName) {
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
    private void modifySEPath(String userID, String wfName) {
        File wfDir = getWorkflowDir(userID, wfName);
        String PSDir = "/" + userID + "/" + getName().replaceAll("\\W", "_");
        String PSOutputDir = PSDir + "/outputs/" + "input_ohm";
        String PSInputDir = PSDir + "/inputs/" + "output_ohm";
        if (wf.isPS()) {
            File propDesc = new File(wfDir, ".PS=PROPS1.desc");
            MiscUtils.writeStrToFile(propDesc.getAbsolutePath(), MiscUtils.readFileToStr(propDesc.getAbsolutePath()).replaceAll("/grid/([^/]*)/[^ ]*", "/grid/$1" + PSDir));
        }
        for (SZGJob agenJob : wf.getJobList()) {
            if (agenJob.getType() == SZGJob.AUTO_GEN_TYPE) {
                File jdl = new File(wfDir, agenJob.getName() + "/" + agenJob.getName() + ".jdl");
                MiscUtils.writeStrToFile(jdl.getAbsolutePath(), (MiscUtils.readFileToStr(jdl.getAbsolutePath()).replaceAll("\"/grid/([^/]*)/([^ ])*\"", "\"/grid/$1" + PSInputDir + "\"")).replaceAll("/users/([^/])*/([^/])*_files", "/users/" + userID + "/" + wfName));
            }
        }
        File wrk = new File(wfDir, wfName + "_remote.wrk");
        String wrkText = MiscUtils.readFileToStr(wrk.getAbsolutePath());
        wrkText = wrkText.replaceAll("\"/grid/([^/]*)/([^ ])*\"", "\"/grid/$1/" + PSInputDir + "\"");
        wrkText = wrkText.replaceAll("\"lfn:/grid/([^/]*)/[^/]*/([^ ]*)\" ([^ ]*) PERMANENT OUTPUT", "\"lfn:/grid/$1/" + PSOutputDir + "/$2\" /$3 PERMANENT OUTPUT");
        wrkText = wrkText.replaceAll("/users/([^/])*/([^/])*_files", "/users/" + userID + "/" + wfName + "_files");
        MiscUtils.writeStrToFile(wrk.getAbsolutePath(), wrkText);
    }

    /**
	 * Returns the directory of a workflow
	 * 
	 * @param wf 
	 * @return File - Directory of the Workflow specified in argument 
	 */
    public static final File getWorkflowDir(SZGWorkflow wf) {
        File ret = null;
        if (wf != null) {
            ret = new File(PropertyLoader.getPrefixDir() + "/users/" + wf.getUserId() + "/" + wf.getId() + "_files/");
        }
        return ret;
    }

    public static final File getWorkflowDir(String userName, String wfName) {
        File ret = null;
        if (userName != null && wfName != null) {
            ret = new File(PropertyLoader.getPrefixDir() + "/users/" + userName + "/" + wfName + "_files/");
        }
        return ret;
    }

    private boolean userCertCheckMG(String username, String gridName) {
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
	 * @param portalUserName
	 * @return void
	 * @throws USGIMEException
	 * 
	 */
    public void startWorkflow(String portalUserName, String basewfUserID, String gridName) throws USGIMEException {
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
            String tmp_gridName;
            for (int i = 0; i < jobs.length; i++) {
                tmp_gridName = jobs[i].getGrid();
                if (!jobs[i].getGrid().endsWith(GridConfigs.CLUSTERGRID_NAME) && !this.userCertCheckMG(portalUserName, tmp_gridName)) {
                    System.out.println("No valid certificate for " + tmp_gridName);
                    USGIMEException e = new USGIMEException("No valid certificate for " + tmp_gridName);
                    throw e;
                }
            }
        }
        try {
            if (!genwf.isSubmitted() || genwf.isStopped()) {
                genwf.submit();
                System.out.println(this.USGIMEWorkflowName + " submitted successfully! ");
                this.timer = new Timer(this.getName() + "Timer");
                timer.schedule(new WorkflowStatusTask(this, gridName), 0);
                this.setStatus(this.STATUS_RUNNING);
            }
        } catch (Exception e) {
            errorMessage = errorMessage == null ? e.getMessage() + "\n" : errorMessage + e.getMessage() + "\n";
        }
        if (errorMessage != null) {
            throw new USGIMEException(errorMessage);
        }
    }

    /**
	 * Tries to rescue the generated workflow  
	 * @param portalUserName
	 * @return void
	 */
    public void RescueWorkflow(String portalUserName) {
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
                        throw new USGIMEException("No valid certificate for " + gridName + "!", this);
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
	 * @param portalUserName
	 * @return void
	 */
    public void CancelWorkflow(String portalUserName) {
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
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
	 * Suspends the generated workflow  
	 * @param portalUserName
	 * @return void
	 */
    public void SuspendWorkflow(String portalUserName) {
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
            USGIMEException e = new USGIMEException("Trying Suspend while eWFs submitted!");
            throw e;
        }
    }

    public boolean CheckCert(String userName) {
        return genwf.isCertNeeded();
    }

    private String loadUsersDirPath() throws Exception {
        return PropertyLoader.getPrefixDir() + "/users/";
    }

    private String loadUsrCert(String usr) throws Exception {
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

    private class WorkflowStatusTask extends TimerTask {

        private USGIMEInstance project;

        private int eWfCount;

        private static final int E_WF_COUNT = 3;

        private boolean finished = false;

        private int allJobNumber = 0;

        private int finishedJobNumber = 0;

        public WorkflowStatusTask(USGIMEInstance project, String gridName) {
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
                int currentStatus = project.getGeneratedUSGIMEWorkflow().getStatus();
                System.out.println("After --  status is : " + currentStatus);
                int oldStatus = project.getStatus();
                if (currentStatus != oldStatus) {
                    project.addLogMessage(project.getGeneratedUSGIMEWorkflow().getId().toString(), "Workflow (" + project.getGeneratedUSGIMEWorkflow().getId().toString() + ") status changed" + " from " + WF_NAMES[oldStatus].toUpperCase() + " to " + WF_NAMES[currentStatus].toUpperCase() + ".");
                    project.setStatus(currentStatus);
                    if (currentStatus == 3) project.setfinishedjobnumber(1);
                    wfStatusChanged = true;
                }
                String wfName = project.getGeneratedUSGIMEWorkflow().getId().toString();
                String logMsg = "Workflow (" + project.getGeneratedUSGIMEWorkflow().getId().toString() + ") statistics changed: ";
                int index = 0;
                for (SZGJob job : project.getGeneratedUSGIMEWorkflow().getJobList()) {
                    if (project.JobStatuses.get(index).getSecond().getSecond() != job.getStatus()) {
                        if (JobStatuses.get(index).getSecond().getSecond() > 0) {
                            project.addLogMessage(wfName, job.getName() + " job's status is changed From " + JOB_STAT_NAMES[JobStatuses.get(index).getSecond().getSecond()].toUpperCase() + " To : " + JOB_STAT_NAMES[job.getStatus()].toUpperCase());
                            if (JobStatuses.get(index).getSecond().getSecond() == 3) {
                                project.addLogMessage(wfName, job.getName() + "Sending Parse_ThreadRequest...");
                                try {
                                    USGIMEOutputManager outputManager = USGIMEOutputManager.getInstance();
                                    outputManager.Parse_ThreadRequest(project.getGeneratedUSGIMEWorkflow().getUserId().toString(), wfName, job.getName());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            project.addLogMessage(wfName, job.getName() + " job's status is changed From Undefined To :  " + job.getStatus());
                        }
                        project.JobStatuses.set(index, new Pair(job.getName(), new Pair(job.getType(), job.getStatus())));
                    }
                    ++index;
                }
                List<Integer> currentStat = project.parsePSWorkflowStatistics(project.getGeneratedUSGIMEWorkflow());
                List<Integer> oldStat = project.worklowStats.get(project.getGeneratedUSGIMEWorkflow().getId().toString());
                if (currentStat != null && !currentStat.equals(oldStat)) {
                    if (oldStat == null) {
                        Integer[] empty = { 0, 0, 0, 0, 0, 0 };
                        oldStat = Arrays.asList(empty);
                    }
                    System.out.println(logMsg);
                    if (currentStat.get(0).intValue() != oldStat.get(0).intValue()) {
                        project.addLogMessage(wfName, logMsg + currentStat.get(0) + " eWf initialized.");
                    }
                    if (currentStat.get(2) > oldStat.get(2)) {
                        project.addLogMessage(wfName, logMsg + Math.abs(currentStat.get(2) - oldStat.get(2)) + " eWf(s) submitted.");
                    }
                    if (currentStat.get(3) > oldStat.get(3)) {
                        project.addLogMessage(wfName, logMsg + Math.abs(currentStat.get(3) - oldStat.get(3)) + " eWf(s) in rescue state.");
                    }
                    if (currentStat.get(4) > oldStat.get(4)) {
                        project.addLogMessage(wfName, logMsg + Math.abs(currentStat.get(4) - oldStat.get(4)) + " eWf(s) in error state.");
                    }
                    if (currentStat.get(5) > oldStat.get(5)) {
                        project.addLogMessage(wfName, logMsg + Math.abs(currentStat.get(5) - oldStat.get(5)) + " eWf(s) finished.");
                        finishedJobNumber = finishedJobNumber + Math.abs(currentStat.get(5) - oldStat.get(5));
                        project.setfinishedjobnumber(Math.abs(currentStat.get(5) - oldStat.get(5)));
                        if (currentStat.get(5).intValue() == currentStat.get(0).intValue()) {
                            this.cancel();
                            project.addLogMessage(wfName, logMsg + " all eWf(s) finished.");
                            project.setStatus(project.STATUS_FINISHED);
                            finished = true;
                        }
                    }
                    project.worklowStats.clear();
                    project.worklowStats.put(project.getGeneratedUSGIMEWorkflow().getId().toString(), currentStat);
                }
                try {
                    Thread.sleep(1000 * 30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getCreatingtime() {
        return Creatingtime;
    }

    public void setCreatingtime(String creatingTime) {
        Creatingtime = creatingTime;
    }

    public String getStatusstr() {
        if (this.getGeneratedUSGIMEWorkflow() != null) return WF_NAMES[this.getGeneratedUSGIMEWorkflow().getStatus()]; else return "Deleted";
    }

    public void setStatusstr(String statusstr) {
        this.statusstr = statusstr;
    }

    public String getStatuscolor() {
        if (this.getGeneratedUSGIMEWorkflow() != null) return WF_COLORS[this.getGeneratedUSGIMEWorkflow().getStatus()]; else return WF_COLORS[0];
    }

    public void setStatuscolor(String statuscolor) {
        this.statuscolor = statuscolor;
    }
}

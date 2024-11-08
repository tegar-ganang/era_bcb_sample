package sc.fgrid.engine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import sc.fgrid.common.Constants;
import sc.fgrid.common.Status;
import sc.fgrid.script.AccessEnum;
import sc.fgrid.script.VarTypeEnum;
import sc.fgrid.types.InstanceCredType;
import sc.fgrid.types.VariableValue;

/**
 * TODO ARBEIT 12345 The agent creation needs to be synchronized! ßEs koennten
 * auch mehrere Instance-Objecte zu // einer Instance existieren, oder? deshalb
 * reicht // es nicht nur die Methode zu synchronizieren.
 * 
 * 
 * One instance of a type of a service. This is a container for all Variables of
 * a job and other data. Don't confuse this with a session (corresponding to an
 * object of the ClientService class, it is orthogonal to it.
 */
@Entity
@Table(name = "INSTANCES")
public class Instance {

    @Transient
    private static Logger log = Logger.getLogger(Instance.class);

    @Version
    private long version;

    @Column(name = "INSTANCE_ID")
    @Id
    @GeneratedValue
    private long instanceID = -1;

    @Basic
    @Column(name = "KEY")
    private String key;

    @Basic
    @Column(name = "SERVICE_ID")
    private String serviceID;

    @Basic
    @Column(name = "CREATOR")
    private String creator;

    @Basic
    @Column(name = "NAME")
    private String name;

    @Transient
    private Service service;

    @Transient
    private Map<String, VarAttributes> attributesMap;

    @Transient
    private Engine engine;

    @OneToMany(cascade = javax.persistence.CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<VarValue> varset = new HashSet<VarValue>();

    /** Don't use this, should only used by JPA. Can this be private? */
    public Instance() {
    }

    /**
     * Purposely restricted access, should only be called by Service.
     * 
     * @param creator
     *            the user who initiated the instance creation.
     */
    Instance(String serviceID, String creator, String instanceName, Engine engine) {
        this.serviceID = serviceID;
        this.creator = creator;
        this.name = instanceName;
        key = sc.fgrid.common.Util.createRandomKey(Math.pow(2, 40));
        setEngine(engine);
        Set<String> varNames = (Set<String>) getAttributesMap().keySet();
        for (String name : varNames) {
            VarAttributes a = getAttributesMap().get(name);
            try {
                if (a.getName().equals(Constants.VAR_CREATION_TIME)) {
                    String now = Calendar.getInstance().getTime().toLocaleString();
                    VarValue value = new VarValue(a.getName(), now);
                    varset.add(value);
                } else if (a.getName().equals(Constants.VAR_CREATOR)) {
                    if (false) {
                        log.warn("added value to fg_creator because IN/INOUT not implemented");
                        VarValue value = new VarValue(a.getName(), "I you see this, it is a bug! 1dsa43gf0d");
                        varset.add(value);
                    }
                } else {
                    VarValue value = a.createVarValue();
                    varset.add(value);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Illegal values for setting a variable, probably wrong defaults\n" + "Should have been detected already when reading in the instance!");
            }
        }
    }

    /**
     * Get the temporary working directory for this instance, below workdir. Uses
     * instanceID as work directory, but could also be any other unique identifier.
     * No need to be secret.
     */
    public File getInstanceDir() {
        if (instanceID < 0) {
            throw new RuntimeException("instanceID not set, is this already persisted?");
        }
        File workdir = engine.getWorkdir();
        String name = "r-" + instanceID;
        return new File(workdir, name);
    }

    public void setupInstanceDir() throws IOException {
        if (instanceID < 0) {
            throw new RuntimeException("Call setupInstanceDir only on persisted instance!");
        }
        File instanceDir = getInstanceDir();
        boolean created = instanceDir.mkdir();
        if (!created) {
            log.warn("Job directory " + instanceDir.getAbsolutePath() + " already exists!");
        }
        PrintStream ss = null;
        File serviceScriptFile = new File(instanceDir, "service.py");
        ss = new PrintStream(serviceScriptFile);
        service.createServiceScript(ss);
        ss.close();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("instanceID=" + instanceID + "\n");
        sb.append("serviceID=" + serviceID + "\n");
        sb.append("creator=" + creator + "\n");
        sb.append("N = " + getAttributesMap().size());
        if (getAttributesMap().size() != varset.size()) {
            throw new RuntimeException("Unexpected inconsistency in Variable Data");
        }
        for (VarValue varval : varset) {
            sb.append(" " + varval);
        }
        return sb.toString();
    }

    void setEngine(Engine engine) {
        try {
            this.engine = engine;
        } catch (Exception ex) {
            throw new RuntimeException("Could not get the Engine, transient data not updated!");
        }
        service = engine.getService(serviceID);
        if (service == null) {
            log.warn("Tried to load an Instance (instanceID=" + instanceID + ") of a instance " + "which has a non-existing type (" + serviceID + ")!");
        } else {
            attributesMap = service.getAttributesMap();
        }
    }

    @SuppressWarnings("unused")
    @PrePersist
    private void prePersist() {
    }

    public long getInstanceID() {
        return instanceID;
    }

    public String getKey() {
        return key;
    }

    public InstanceCredType getInstanceCred() {
        InstanceCredType instanceCred = new InstanceCredType();
        instanceCred.setId(instanceID);
        instanceCred.setKey(key);
        return instanceCred;
    }

    public Service getService() {
        if (engine == null) throw new RuntimeException("First set engine!");
        return service;
    }

    public String getCreator() {
        return creator;
    }

    /** Dangerous, no access control! */
    VarValue getVarValue(String name) throws ClientException, ConfigException {
        return getVarValue(name, Caller.INTERNAL);
    }

    public VarValue getVarValue(String name, Caller caller) throws ClientException, ConfigException {
        VarAttributes va = attributesMap.get(name);
        if (va == null) {
            throw new ClientException(instanceID, "variable " + name + " not known!");
        }
        if (caller != Caller.INTERNAL) {
            AccessEnum rights = caller == Caller.AGENT ? va.getAgentAccess() : va.getAccess();
            if (rights == AccessEnum.NONE) {
                throw new ClientException(instanceID, "variable " + name + " not accessible!");
            }
        }
        for (VarValue var : varset) {
            if (name.equals(var.getName())) {
                return var;
            }
        }
        if (name.equals(Constants.VAR_CREATOR)) {
            VarValue value = getAttributesMap().get(Constants.VAR_CREATOR).createVarValue();
            value.setStringValue(creator);
            return value;
        }
        throw new ConfigException("Variable " + name + " not found!\n" + "This can happen if the administrator changed the interface of \n" + "a instance and forgot to change the instanceID.");
    }

    /**
     * 
     * @param varnames
     * @param caller
     *            rights of the caller
     * @throws EngineException
     */
    Set<VarValue> getVarValues(Collection<String> varnames, Caller caller) throws EngineException {
        Set<VarValue> values = new HashSet<VarValue>();
        for (String name : varnames) {
            VarValue value = getVarValue(name, caller);
            values.add(value);
        }
        return values;
    }

    /**
     * Updates the variable value, does not set it to a new copy, so stays the
     * same persisted instance. The type must be the same.
     */
    public void updateVarValue(VarValue vararg, Caller caller) throws EngineException {
        updateVariableValue(vararg.asVariableValue(), caller);
    }

    /**
     * Updates the variable value, does not set it to a new copy, so stays the
     * same persisted instance. The type must be the same.
     */
    public void updateVariableValue(VariableValue vararg, Caller caller) throws EngineException {
        String varName = vararg.getName();
        VarAttributes va = attributesMap.get(varName);
        if (va == null) {
            throw new EngineException("variable " + varName + " not known!");
        }
        if (caller != Caller.INTERNAL) {
            AccessEnum rights = caller == caller.AGENT ? va.getAgentAccess() : va.getAccess();
            boolean cond1 = (va.getType() != VarTypeEnum.FILES && rights != AccessEnum.INOUT);
            boolean cond2 = (va.getType() == VarTypeEnum.FILES && rights == AccessEnum.NONE);
            if (cond1 || cond2) {
                throw new EngineException("variable " + varName + " not writable!");
            }
        }
        for (VarValue var : varset) {
            if (vararg.getName().equals(var.getName())) {
                var.update(vararg);
                return;
            }
        }
        throw new RuntimeException("Variable " + vararg.getName() + " not found");
    }

    /**
     * @return the serviceID
     */
    public String getServiceID() {
        return serviceID;
    }

    /**
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(long version) {
        this.version = version;
    }

    public String getStatus() {
        return "not-implemented";
    }

    /**
     * @return the attributesMap
     */
    public Map<String, VarAttributes> getAttributesMap() {
        if (engine == null) throw new RuntimeException("First set engine!");
        return attributesMap;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Try to clean up resources of this instance before the instance is
     * deleted. E.g. delete files or other external resources. Prerequisite is a
     * corresponding status of this instance, e.g. a status where no agent is
     * running.
     */
    void cleanUp() {
        log.info("cleaning up instance with ID=" + instanceID);
        File instanceDir = getInstanceDir();
        sc.fgrid.common.Util.deleteFile(instanceDir);
    }

    /**
     * Create the program (a shell script with embedded scripts and finally calls
     * python) which needs to be transported and executed on the remote host,
     * and which is actually the agent!
     * 
     * @param ps
     *            The stream where to write this (binary data!)
     * @param agentName
     * @param agentID
     * @param agentKey
     */
    void createAgentProgram(PrintStream ps, String agentName, String agentID) throws IOException, ConfigException {
        Agent agent = service.getAgent(agentName);
        File instanceDir = getInstanceDir();
        double agent_timeout_seconds = engine.getTimeout();
        String ii = "\"";
        String agentScript = "agent-" + agentName + ".py";
        String serviceScript = "service.py";
        Set<String> taskNames = agent.getTasks().keySet();
        File scriptDir = getService().getScriptDir();
        final boolean embedScripts = true;
        {
            ps.println("#!/bin/sh");
            ps.println("umask 0077");
        }
        for (String taskName : taskNames) {
            Task task = getService().getTask(taskName);
            String script = task.getScriptContent();
            if (embedScripts) {
                ps.println();
                ps.println("cat > " + task.getFile() + " << " + ii + "EOF_" + task.getFile() + ii);
                ps.println(script);
                ps.println("EOF_" + task.getFile());
            } else {
                File taskScriptFile = new File(instanceDir, task.getFile());
                PrintStream xs = new PrintStream(taskScriptFile);
                xs.print(script);
                xs.close();
            }
        }
        {
            PrintStream ss = null;
            if (embedScripts) {
                ps.println();
                ps.println("cat > " + serviceScript + " << " + ii + "EOF_" + serviceScript + ii);
                ss = ps;
            } else {
                File serviceScriptFile = new File(instanceDir, serviceScript);
                ss = new PrintStream(serviceScriptFile);
            }
            service.createServiceScript(ss);
            if (embedScripts) ps.println("EOF_" + serviceScript); else ss.close();
        }
        {
            URL url = new URL(engine.getAgentContextURL() + "/" + engine.getAgentURLPath());
            log.debug("url for agent = " + url.toExternalForm());
            PrintStream as = null;
            if (embedScripts) {
                ps.println();
                ps.println("cat > " + agentScript + " << " + ii + "EOF_" + agentScript + ii);
                as = ps;
            } else {
                File agentScriptFile = new File(instanceDir, agentScript);
                as = new PrintStream(agentScriptFile);
            }
            String tab[] = { "", "    ", "        ", "            ", "                ", "                    " };
            int i = 0;
            as.println(tab[i] + "from wrapper import ZSIWrapper");
            as.println(tab[i] + "from service import Service");
            as.println(tab[i] + "import threading");
            as.println(tab[i] + "from sys import exit");
            as.println();
            as.println(tab[i] + "URL=" + ii + url.toString() + ii);
            as.println(tab[i] + "agentID=" + ii + agentID + ii);
            as.println(tab[i] + "instanceID=" + instanceID);
            as.println(tab[i] + "instanceKey=" + ii + key + ii);
            as.println(tab[i] + "timeout=" + ii + agent_timeout_seconds + ii);
            as.println(tab[i] + "stop_flag=0");
            as.println(tab[i] + "kill_flag=0");
            as.println();
            as.println(tab[i] + "# Need extra port for each thread, " + "ZSIWrapper (actually ZSI) not thread save");
            as.println(tab[i] + "port0=ZSIWrapper(URL, agentID, instanceID, instanceKey, timeout)");
            as.println(tab[i] + "port0.setAgentStatus(" + Status.RUNNING.ordinal() + ")");
            as.println();
            as.println(tab[i] + "def create_namespace(service) :");
            as.println(tab[i] + "    " + ii + "create a namespace for injection " + "into a task script and returns it." + ii);
            as.println(tab[i] + "# first get most (not all) variables");
            as.print(tab[i] + "    arglist = [ ");
            int counter = 0;
            for (VarAttributes va : attributesMap.values()) {
                counter++;
                String varname = va.getName();
                if (Constants.VAR_STDOUT.equals(varname)) continue;
                VarTypeEnum vartype = va.getType();
                switch(vartype) {
                    case DOUBLE:
                    case STRING:
                    case LONG:
                    case BOOLEAN:
                    case CHOICE:
                        as.print(tab[i] + "    '" + varname + "'");
                        if (counter != attributesMap.size()) as.println(",\\");
                        break;
                    case FILES:
                        break;
                    default:
                        throw new RuntimeException("unkown data type in switch");
                }
            }
            as.println("]");
            as.println(tab[i] + "    vars=port0.getManyVars(arglist)");
            as.println(tab[i] + "    namesp = vars ");
            as.println(tab[i] + "    namesp[ 'service' ] = service ");
            as.println(tab[i] + "    return namesp");
            as.println();
            for (String taskName : taskNames) {
                Task task = getService().getTask(taskName);
                boolean background = task.isBackground();
                as.println(tab[i] + "def task_" + taskName + "():");
                {
                    i++;
                    as.println(tab[i] + "try :");
                    if (background) {
                        as.println(tab[i] + "    # as ZSIWrapper is not task save," + " create a new one");
                        as.println(tab[i] + "    port_task=ZSIWrapper(URL, agentID, " + "instanceID, instanceKey, timeout)");
                        as.println(tab[i] + "    service = Service(port_task)");
                    } else {
                        as.println(tab[i] + "    # this is foreground, " + "use port0 from current task");
                        as.println(tab[i] + "    service = Service(port0)");
                    }
                    as.println(tab[i] + "    globals = create_namespace(service)");
                    as.println(tab[i] + "    locals = { }");
                    as.println(tab[i] + "    execfile('" + task.getFile() + "', globals, locals )");
                    as.println(tab[i] + "except :");
                    as.println(tab[i] + "    port0.setTaskStatus('" + taskName + "', " + Status.FAILED.ordinal() + " )");
                    as.println(tab[i] + "else :");
                    as.println(tab[i] + "    if kill_flag :");
                    as.println(tab[i] + "        task_exit_status = " + Status.FAILED.ordinal());
                    as.println(tab[i] + "    elif stop_flag :");
                    as.println(tab[i] + "        task_exit_status = " + Status.TERMINATED.ordinal());
                    as.println(tab[i] + "    else :");
                    as.println(tab[i] + "        task_exit_status = " + Status.FINISHED.ordinal());
                    as.println(tab[i] + "    port0.setTaskStatus('" + taskName + "', task_exit_status )");
                    as.println();
                    i--;
                }
            }
            as.println(tab[i] + "try:");
            i++;
            as.println(tab[i] + "tasklist = []");
            as.println(tab[i] + "action=None");
            as.println(tab[i] + "while action != '" + Agent.STOP + "' and action != '" + Agent.KILL + "' and action != '" + Agent.FINISH + "' :");
            {
                i++;
                as.println(tab[i] + "# print 'ACTION=', action");
                as.println(tab[i] + "action = port0.getAction()");
                as.println(tab[i] + "");
                as.println(tab[i] + "# cleanup tasklist (only to save memory)");
                as.println(tab[i] + "# In two cycles, don't change list during iter.");
                as.println(tab[i] + "to_be_removed = []");
                as.println(tab[i] + "for tr2 in tasklist:");
                as.println(tab[i] + "    if (not tr2.isAlive()):");
                as.println(tab[i] + "        to_be_removed.append(tr2)");
                as.println(tab[i] + "for tr3 in to_be_removed:");
                as.println(tab[i] + "    # print 'old task removed: ',tr3.getName()");
                as.println(tab[i] + "    tasklist.remove(tr3)");
                as.println(tab[i] + "");
                as.println(tab[i] + "if action == None :");
                as.println(tab[i] + "    # print 'processing None'");
                as.println(tab[i] + "    None");
                as.println(tab[i] + "if action == '" + Agent.FINISH + "' :");
                as.println(tab[i] + "    # print 'processing FINISH'");
                as.println(tab[i] + "    None");
                as.println(tab[i] + "elif action == '" + Agent.STOP + "' :");
                as.println(tab[i] + "    stop_flag=1");
                as.println(tab[i] + "    # print 'processing STOP'");
                String onStop = agent.onStop;
                if (onStop != null) {
                    String function = "task_" + onStop;
                    as.println(tab[i] + "    " + function + "()");
                }
                as.println(tab[i] + "");
                as.println(tab[i] + "elif action == '" + Agent.KILL + "' :");
                as.println(tab[i] + "    kill_flag=1");
                as.println(tab[i] + "    # print 'processing KILL'");
                String onKill = agent.onKill;
                if (onKill != null) {
                    String function = "task_" + onKill;
                    as.println(tab[i] + "    " + function + "()");
                }
                as.println(tab[i] + "    # This would be the place " + "to kill external programs");
                as.println(tab[i] + "    port0.setAgentStatus(" + Status.FAILED.ordinal() + ")");
                as.println(tab[i] + "    # exit without waiting for bg threads");
                as.println(tab[i] + "    exit(13)");
                as.println(tab[i] + "");
                for (String taskName : taskNames) {
                    Task task = getService().getTask(taskName);
                    boolean background = task.isBackground();
                    as.println(tab[i] + "elif action == '" + Agent.TASK + taskName + "':");
                    String function = "task_" + taskName;
                    if (background) {
                        as.println(tab[i] + "    # create, register and start task");
                        as.println(tab[i] + "    t = threading.Thread(target=" + function + ", name='" + function + "')");
                        as.println(tab[i] + "    tasklist.append(t)");
                        as.println(tab[i] + "    t.start()");
                    } else {
                        as.println(tab[i] + "    " + function + "()");
                    }
                }
                as.println(tab[i] + "else :");
                as.println(tab[i] + "    print 'undefined case in agent script iov0j23fds, " + "action=', action");
                i--;
            }
            as.println();
            as.println(tab[i] + "# wait for running background tasks");
            as.println(tab[i] + "# print 'remaining threads (inclides demonic ones!):'" + ", threading.activeCount()");
            as.println(tab[i] + "# for tr in threading.enumerate():");
            as.println(tab[i] + "#    print 'remaining thread: '," + " tr.getName()");
            as.println(tab[i] + "for tr2 in tasklist:");
            as.println(tab[i] + "    # print 'joining with ',tr2.getName()");
            as.println(tab[i] + "    tr2.join()");
            i--;
            as.println(tab[i] + "# Set agent status");
            as.println(tab[i] + "except :");
            as.println(tab[i] + "    port0.setAgentStatus(" + Status.FAILED.ordinal() + ")");
            as.println(tab[i] + "else :");
            as.println(tab[i] + "    if kill_flag :");
            as.println(tab[i] + "        agent_exit_status = " + Status.FAILED.ordinal());
            as.println(tab[i] + "    elif stop_flag :");
            as.println(tab[i] + "        agent_exit_status = " + Status.TERMINATED.ordinal());
            as.println(tab[i] + "    else :");
            as.println(tab[i] + "        agent_exit_status = " + Status.FINISHED.ordinal());
            as.println(tab[i] + "    port0.setAgentStatus( agent_exit_status )");
            if (embedScripts) ps.println("EOF_" + agentScript); else as.close();
        }
        for (String agentFileName : agent.getFilesSet()) {
            ps.println();
            File agentRelFile = new File(agentFileName);
            File directory = agentRelFile.getParentFile();
            if (directory != null) {
                ps.println("mkdir -p " + directory.getPath());
            }
            ps.println("cat > " + agentFileName + " << " + ii + "EOF_" + agentFileName + ii);
            File file = new File(scriptDir, agentFileName);
            byte bytes[] = FileUtils.readFileToByteArray(file);
            ps.write(bytes);
            ps.println();
            ps.println("EOF_" + agentFileName);
        }
        if (embedScripts) {
            ps.println("SKIP=`awk '/^__ARCHIVE_FOLLOWS__/ { print NR + 1; exit 0; }' $0`");
            ps.println("tail -n +$SKIP $0 | gzip -dc | tar x ");
            ps.println("python -u " + agentScript);
            ps.println("exit 0");
            ps.println("__ARCHIVE_FOLLOWS__");
            java.io.InputStream istream = this.getClass().getClassLoader().getResourceAsStream("sc/fgrid/agent.tar.gz");
            IOUtils.copy(istream, ps);
        } else {
            String pypath = engine.getAgentPythonPath();
            ps.println("export PYTHONPATH=" + ii + pypath + ii);
            ps.println("python -u " + agentScript);
            ps.println("exit 0");
        }
    }
}

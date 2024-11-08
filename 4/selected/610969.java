package hu.usz.inf.netspotter.agent;

import hu.usz.inf.netspotter.AgentProperties;
import hu.usz.inf.netspotter.agent.jobs.CopyFileJob;
import hu.usz.inf.netspotter.agent.jobs.DataExchangeJob;
import hu.usz.inf.netspotter.agent.jobs.LogicJob;
import hu.usz.inf.netspotter.agent.jobs.RestartJob;
import hu.usz.inf.netspotter.agentlogic.AgentLogic;
import hu.usz.inf.netspotter.server.RemoteCenter;
import hu.usz.inf.netspotter.server.entities.AgentRep;
import hu.usz.inf.netspotter.server.entities.Interface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import net.MACaddress;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;

public class Agent implements AgentControl {

    public static Scheduler _scheduler;

    protected static final String AGENT_LOGIC_CLASS_PATH = "hu" + File.separator + "usz" + File.separator + "inf" + File.separator + "netspotter" + File.separator + "agentlogic";

    private static final String SCHEDULE_SKIPPED = "Skipped scheduling {0} at {1}: time is in the past.";

    public static String fromByteToString(byte[] from) {
        String to = "";
        for (int i = 0; i < from.length; i++) {
            to = to + Character.toString((char) from[i]);
        }
        return to;
    }

    public static void main(String[] args) {
    }

    protected String name;

    protected Vector<RunResults> results = new Vector<RunResults>();

    private States state;

    private Vector<Inet6Address> ip6Addresses;

    private Vector<Inet4Address> ip4Addresses;

    private Vector<Interface> interfaces;

    private Vector<String> errorMessages;

    private RemoteCenter _center;

    private SchedulerFactory sf;

    private HashMap<String, JobDetail> jobs = new HashMap<String, JobDetail>();

    private AgentRep _agentRep;

    protected final AgentProperties _properties;

    public Agent() {
        initAgent();
        _properties = AgentProperties.getProperties();
        initSchedudler();
    }

    public Agent(String name, RemoteCenter c) {
        this._center = c;
        this.name = name;
        initAgent();
        _properties = AgentProperties.getProperties();
        initSchedudler();
    }

    @SuppressWarnings("deprecation")
    public void agentLogicExecution(Date date) {
        if (getState() != States.Idle) return;
        setState(States.Executing);
        System.out.println("Agent logic execution");
        try {
            ClassLoader parentLoader = Agent.class.getClassLoader();
            File outDir = new File(_properties.getProperty("agentFileLocation"));
            File out = new File(outDir.getAbsolutePath() + File.separator + AGENT_LOGIC_CLASS_PATH + File.separator + name + ".class");
            System.out.println(out.toURL());
            URLClassLoader loader1 = new URLClassLoader(new URL[] { outDir.toURL() }, parentLoader);
            Class cls1 = loader1.loadClass("hu.usz.inf.netspotter.agentlogic." + name);
            AgentLogic ag = (AgentLogic) cls1.newInstance();
            System.out.println("Agent " + name);
            ag.prepare();
            ag.run();
            RunResults run = new RunResults();
            run.testcaseid = ag.getTestCaseId();
            run.time = date;
            run.feedbacks = ag.getFeedback();
            run.agent = name;
            results.add(run);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            setState(States.Idle);
        }
    }

    public void copyLogic() {
        if (getState() == States.Idle) {
            setState(States.Synchronizing);
            try {
                FileChannel sourceChannel = new FileInputStream(new File(_properties.getProperty("binPath") + name + ".class")).getChannel();
                FileChannel destinationChannel = new FileOutputStream(new File(_properties.getProperty("agentFileLocation") + name + ".class")).getChannel();
                sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
                sourceChannel.close();
                destinationChannel.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setState(States.Idle);
        }
    }

    public synchronized void dataExcange() {
        if (getState() != States.Idle) return;
        setState(States.DataExcahenge);
        try {
            processSchedule(_center.dataExchange(_agentRep, results));
            results.removeAllElements();
            System.out.println("Data Exchange successful.");
        } catch (Exception x) {
            x.printStackTrace(System.out);
        } finally {
            setState(States.Idle);
        }
    }

    public synchronized RemoteCenter getC() {
        return _center;
    }

    public synchronized Vector<Interface> getInterfaces() {
        return interfaces;
    }

    public synchronized Vector<Inet4Address> getIp4Addresses() {
        return ip4Addresses;
    }

    public synchronized States getState() {
        return state;
    }

    public void restart() {
        Logger.getLogger(getClass().getName()).info(MessageFormat.format("Restarting agent {0}...", name));
        setState(States.Idle);
        try {
            _scheduler.deleteJob("DataExch", "A");
            _scheduler.deleteJob("LogicJob", "A");
            _scheduler.deleteJob("CopyFileJob", "A");
            _scheduler.deleteJob("RestartJob", "A");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        initAgent();
        initSchedudler();
        Logger.getLogger(getClass().getName()).info(MessageFormat.format("Restarted agent {0}...", name));
    }

    public void scheduleJob(Schedule sched) {
        try {
            if (sched.type == ScheduleType.Cron) {
                CronTrigger trigger = new CronTrigger();
                trigger.setCronExpression(sched.schedudle);
                trigger.setStartTime(TriggerUtils.getEvenMinuteDate(new Date()));
                trigger.setName(this.name + name);
                if (Agent._scheduler == null) {
                    _scheduler = sf.getScheduler();
                    _scheduler.start();
                }
                JobDetail jd = jobs.get(sched.task);
                _scheduler.scheduleJob(jd, trigger);
            } else {
                if (sched.date.before(new Date())) {
                    Logger.getLogger(getClass().getName()).warning(MessageFormat.format(SCHEDULE_SKIPPED, sched.task, sched.date));
                } else {
                    Date d = sched.date;
                    StringBuilder triggerName = new StringBuilder(name);
                    triggerName.append("-");
                    triggerName.append(sched.task);
                    triggerName.append("-");
                    triggerName.append(sched.schedudle);
                    Logger.getLogger(getClass().getName()).info(MessageFormat.format("Scheduling trigger {0} for {1}.", triggerName.toString(), d));
                    SimpleTrigger trigger = new SimpleTrigger(triggerName.toString(), null, d, null, 0, 0L);
                    if (Agent._scheduler == null) {
                        _scheduler = sf.getScheduler();
                        _scheduler.start();
                    }
                    JobDetail jd = jobs.get(sched.task);
                    jd.getJobDataMap().put("Date", d);
                    _scheduler.scheduleJob(jd, trigger);
                }
            }
        } catch (Exception e) {
            errorMessages.add(e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void setInterfaces(Vector<Interface> interfaces) {
        this.interfaces = interfaces;
    }

    public synchronized void setIpAddresses(Vector<Inet4Address> ipAddresses) {
        this.ip4Addresses = ipAddresses;
    }

    public synchronized void setState(States state) {
        this.state = state;
    }

    private void initAgent() {
        results = new Vector<RunResults>(5);
        state = States.Idle;
        _agentRep = new AgentRep();
        ip6Addresses = new Vector<Inet6Address>();
        ip4Addresses = new Vector<Inet4Address>();
        interfaces = new Vector<Interface>();
        errorMessages = new Vector<String>();
        try {
            NetworkInterface[] ni = JpcapCaptor.getDeviceList();
            for (int i = 0; i < ni.length; i++) {
                MACaddress mac = new MACaddress(ni[i].mac_address);
                if (!mac.toString().equals("00:00:00:00:00:00")) {
                    Interface inte = new Interface();
                    inte.macAddress = mac;
                    inte.name = new Integer(i).toString();
                    if (ni[i].description == null) inte.description = ni[i].name; else inte.description = ni[i].description;
                    for (int a = 0; a < ni[i].addresses.length; a++) {
                        InetAddress ia = ni[i].addresses[a].address;
                        if (ia instanceof Inet6Address) {
                            inte.ip6addresses.add((Inet6Address) ia);
                            ip6Addresses.add((Inet6Address) ia);
                        } else {
                            inte.ip4addresses.add((Inet4Address) ia);
                            ip4Addresses.add((Inet4Address) ia);
                        }
                    }
                    interfaces.add(inte);
                } else {
                    Logger.getLogger(getClass().getName()).info(MessageFormat.format("Interface {0} ({1}) excluded because of zero MAC address.", ni[i].name, ni[i].description));
                }
            }
            _agentRep.setInterfaces(interfaces);
            _agentRep.setIp6Addresses(ip6Addresses);
            _agentRep.setIp4Addresses(ip4Addresses);
            _agentRep.setName(name);
            _agentRep.ID = name;
        } catch (Exception e) {
            errorMessages.add(e.getMessage());
        }
    }

    private void addJob(String jobName, Class cls) {
        Logger.getLogger(getClass().getName()).finer(MessageFormat.format("Adding job {0} to agent {1}...", jobName));
        jobs.put(jobName, newJob(jobName, cls, new Date()));
        Logger.getLogger(getClass().getName()).finer(MessageFormat.format("Added job {0} to agent {1}...", jobName, name));
    }

    private void initSchedudler() {
        Logger.getLogger(getClass().getName()).info(MessageFormat.format("Initializing agent scheduler in agent {0}...", name));
        sf = new StdSchedulerFactory();
        Logger.getLogger(getClass().getName()).fine(MessageFormat.format("Adding jobs to agent {0}...", name));
        addJob("DataExch", DataExchangeJob.class);
        addJob("LogicJob", LogicJob.class);
        addJob("CopyFileJob", CopyFileJob.class);
        addJob("RestartJob", RestartJob.class);
        Logger.getLogger(getClass().getName()).fine(MessageFormat.format("Added jobs to agent {0}...", name));
        Schedule sch = new Schedule();
        sch.task = "DataExch";
        sch.action = Action.Add;
        sch.type = ScheduleType.Cron;
        sch.schedudle = "*/5 * * * * ?";
        scheduleJob(sch);
    }

    private JobDetail newJob(String name, Class c, Date d) {
        JobDetail jd = new JobDetail(name, this.name, c);
        JobDataMap jdm = jd.getJobDataMap();
        jdm.put("Agent", this);
        jdm.put("Date", d);
        return jd;
    }

    private void processSchedule(Vector<Schedule> sched) {
        if (sched != null) for (Schedule schedudle : sched) {
            Logger.getLogger(getClass().getName()).info(MessageFormat.format("Got schedule {0} at {1} ({2}).", schedudle.task, schedudle.schedudle, schedudle.date));
            scheduleJob(schedudle);
        }
    }

    public void listSchedules() {
        for (JobDetail jd : jobs.values()) {
            System.out.println(jd.getName());
        }
    }
}

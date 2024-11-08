package cz.karry.jtw;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.log4j.Logger;

/**
 *
 * @author karry
 */
public class ThreadWatchDog extends AbstractPoolingThread {

    private static final Logger logger = Logger.getLogger(ThreadWatchDog.class);

    private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private static final String delimiter = "\n===================================================================\n";

    private final VirtualMachineDescriptor machine;

    private final String statDir;

    private OutputStreamWriter output;

    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    private ThreadMXBean threadBean;

    private final long warningTime;

    private final Map<Long, BlockedThreadInfo> blockedThreadsMap = new HashMap<Long, BlockedThreadInfo>();

    ThreadWatchDog(VirtualMachineDescriptor m, String statDir, long warningTime) {
        this.machine = m;
        this.warningTime = warningTime;
        this.statDir = statDir;
        this.name = "threadPool-" + machine.id();
    }

    @Override
    public boolean start(long poolTime) {
        try {
            JMXServiceURL jmxUrl = getURLForPid(machine.id());
            JMXConnector jmxc = JMXConnectorFactory.connect(jmxUrl);
            MBeanServerConnection server = jmxc.getMBeanServerConnection();
            threadBean = ManagementFactory.newPlatformMXBeanProxy(server, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
            if (!threadBean.isThreadCpuTimeSupported()) {
                throw new Exception("This VM does not support thread CPU time monitoring");
            } else {
                threadBean.setThreadCpuTimeEnabled(true);
            }
            if (!threadBean.isThreadContentionMonitoringSupported()) {
                throw new Exception("This VM does not support thread contention monitoring");
            } else {
                threadBean.setThreadContentionMonitoringEnabled(true);
            }
            if (!threadBean.isObjectMonitorUsageSupported()) {
                throw new Exception("This VM does not supports monitoring of object monitor usage");
            }
            if (!threadBean.isSynchronizerUsageSupported()) {
                throw new Exception("This VM does not supports monitoring of ownable synchronizer usage");
            }
            File f = new File(statDir, machine.id());
            int i = 0;
            while (f.exists()) {
                f = new File(statDir, machine.id() + "." + i);
                i++;
            }
            output = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(f)));
            output.write("watchdog for process \"" + (machine.id() + "/" + machine.displayName()) + "\" started at " + dateFormat.format(new Date()) + "\n\n");
            super.start(poolTime);
            logger.info("watchdog started for \"" + (machine.id() + "/" + machine.displayName()) + "\"; stat file " + f.getPath());
        } catch (Exception ex) {
            logger.error("Exception while starting process watchdog for " + machine.id() + "/" + machine.displayName(), ex);
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (!super.stop()) return false;
        logger.info("watchdog stopped for " + (machine.id() + "/" + machine.displayName()));
        try {
            if (output != null) {
                output.flush();
                output.close();
            }
        } catch (IOException ex) {
            logger.error("exception while closing output", ex);
        }
        return true;
    }

    @Override
    void doJob() {
        long[] tids = null;
        ThreadInfo[] tinfos = null;
        try {
            tids = threadBean.getAllThreadIds();
            tinfos = threadBean.getThreadInfo(tids, true, true);
        } catch (java.lang.reflect.UndeclaredThrowableException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof java.rmi.ConnectException) {
                logger.warn("remote VM was probably stopped");
                stop();
            } else {
                logger.error("undeclared exception", ex);
            }
            return;
        }
        Set<Long> blockedTids = new HashSet<Long>();
        long tid;
        BlockedThreadInfo blockInfo;
        boolean flushOutput = false;
        for (ThreadInfo info : tinfos) {
            if (info != null && info.getThreadState() == Thread.State.BLOCKED) {
                tid = info.getThreadId();
                blockedTids.add(tid);
                blockInfo = blockedThreadsMap.get(tid);
                if (blockInfo == null) {
                    blockInfo = new BlockedThreadInfo(info);
                    blockedThreadsMap.put(tid, blockInfo);
                } else {
                    if (blockInfo.getBlockedCount() != info.getBlockedCount()) {
                        blockInfo.setBlockedCount(info.getBlockedCount());
                        blockInfo.setBlockedTime(info.getBlockedTime());
                        blockInfo.setWarned(false);
                    } else {
                        if (info.getBlockedTime() - blockInfo.getBlockedTime() >= warningTime && (!blockInfo.warnedAlready())) {
                            try {
                                if (!flushOutput) output.write(delimiter);
                                writeThreadWarning(info, tinfos, blockInfo.getBlockedTime());
                            } catch (IOException ex) {
                                logger.error("exception while store blocked thread stat", ex);
                            } finally {
                                flushOutput = true;
                                blockInfo.setWarned(true);
                            }
                        }
                    }
                }
            }
        }
        Set<Long> copy = new HashSet<Long>(blockedThreadsMap.keySet());
        for (Long id : copy) {
            if (!blockedTids.contains(id)) blockedThreadsMap.remove(id);
        }
        if (flushOutput) {
            try {
                output.flush();
            } catch (IOException ex) {
                logger.error("exception while flushing output", ex);
            }
        }
    }

    private JMXServiceURL getURLForPid(String pid) throws Exception {
        final VirtualMachine vm = VirtualMachine.attach(pid);
        String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        if (connectorAddress == null) {
            String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
            vm.loadAgent(agent);
            connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            assert connectorAddress != null;
        }
        return new JMXServiceURL(connectorAddress);
    }

    private void writeThreadWarning(ThreadInfo info, ThreadInfo[] tinfos, long lastBlockStart) throws IOException {
        String msg = dateFormat.format(new Date()) + " - thread " + info.getThreadId() + " \"" + info.getThreadName() + "\" is blocked more than " + (info.getBlockedTime() - lastBlockStart) + "ms!";
        logger.debug(msg);
        output.write("\n" + msg + "\n");
        output.write(info.toString() + "");
        output.write("blocked by:\n");
        long blockedBy = info.getLockOwnerId();
        for (ThreadInfo i : tinfos) {
            if (i != null && i.getThreadId() == blockedBy) {
                output.write(i.toString());
                break;
            }
        }
    }
}

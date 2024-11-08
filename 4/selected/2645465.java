package goldengate.ftp.exec.control;

import goldengate.common.cpu.CpuManagement;
import goldengate.common.cpu.CpuManagementInterface;
import goldengate.common.cpu.CpuManagementNoInfo;
import goldengate.common.cpu.CpuManagementSysmon;
import goldengate.common.database.DbAdmin;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import goldengate.ftp.core.config.FtpInternalConfiguration;
import goldengate.ftp.exec.config.FileBasedConfiguration;
import java.util.Random;

/**
 * Constraint Limit (CPU and connection - network and local -) handler.
 * @author Frederic Bregier
 *
 */
public class ConstraintLimitHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(ConstraintLimitHandler.class);

    private static Random random = new Random();

    private static CpuManagementInterface cpuManagement;

    private static double cpuLimit = 0.8;

    private static int channelLimit = 1000;

    private double lastLA = 0.0;

    private long lastTime;

    public ConstraintLimitHandler(boolean useCpuLimit, boolean useJdKCpuLimit, double cpulimit, int channellimit) {
        if (useCpuLimit) {
            if (useJdKCpuLimit) {
                try {
                    cpuManagement = new CpuManagement();
                } catch (IllegalArgumentException e) {
                    cpuManagement = new CpuManagementNoInfo();
                }
            } else {
                cpuManagement = new CpuManagementSysmon();
            }
        } else {
            cpuManagement = new CpuManagementNoInfo();
        }
        cpuLimit = cpulimit;
        channelLimit = channellimit;
        lastTime = System.currentTimeMillis();
    }

    /**
     * 
     * @return True if one of the limit is exceeded.
     */
    public boolean checkConstraints() {
        if (cpuLimit < 1 && cpuLimit > 0) {
            long newTime = System.currentTimeMillis();
            if ((newTime - lastTime) < (FtpInternalConfiguration.WAITFORNETOP / 2)) {
                if (lastLA <= cpuLimit) {
                    return false;
                }
            }
            lastTime = newTime;
            lastLA = cpuManagement.getLoadAverage();
            if (lastLA > cpuLimit) {
                logger.debug("LA: " + lastLA + " > " + cpuLimit);
                return true;
            }
        }
        if (channelLimit > 0) {
            int nb = DbAdmin.getNbConnection() - DbAdmin.nbHttpSession;
            if (channelLimit <= nb) {
                logger.debug("NW:" + nb + " > " + channelLimit);
                return true;
            }
            nb = FileBasedConfiguration.fileBasedConfiguration.getFtpInternalConfiguration().getNumberSessions();
            if (channelLimit <= nb) {
                logger.debug("NL:" + nb + " > " + channelLimit);
                return true;
            }
        }
        return false;
    }

    /**
     * Same as checkConstraints except that the thread will sleep some time proportionally to
     * the current Load (if CPU related)
     * @param step the current step in retry
     * @return True if one of the limit is exceeded.
     */
    public boolean checkConstraintsSleep(int step) {
        long delay = FtpInternalConfiguration.WAITFORNETOP / 2;
        if (cpuLimit < 1 && cpuLimit > 0) {
            long newTime = System.currentTimeMillis();
            if ((newTime - lastTime) < delay) {
                if (lastLA > cpuLimit) {
                    double sleep = lastLA * delay * (step + 1) * random.nextFloat();
                    long shorttime = (long) sleep;
                    try {
                        Thread.sleep(shorttime);
                    } catch (InterruptedException e) {
                    }
                } else {
                    return false;
                }
            }
        }
        if (checkConstraints()) {
            delay = getSleepTime() * (step + 1);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 
     * @return a time below TIMEOUTCON with a random
     */
    public static long getSleepTime() {
        return (long) (FileBasedConfiguration.fileBasedConfiguration.TIMEOUTCON * random.nextFloat()) + 100;
    }

    /**
     * @return the cpuLimit
     */
    public static double getCpuLimit() {
        return cpuLimit;
    }

    /**
     * @param cpuLimit the cpuLimit to set
     */
    public static void setCpuLimit(double cpuLimit) {
        ConstraintLimitHandler.cpuLimit = cpuLimit;
    }

    /**
     * @return the channelLimit
     */
    public static int getChannelLimit() {
        return channelLimit;
    }

    /**
     * @param channelLimit the channelLimit to set
     */
    public static void setChannelLimit(int channelLimit) {
        ConstraintLimitHandler.channelLimit = channelLimit;
    }
}

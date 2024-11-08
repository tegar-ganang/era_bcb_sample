package com.jujunie.integration.database;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jujunie.integration.ConfigurationException;
import com.jujunie.integration.DatabaseConfiguration;
import com.jujunie.integration.EMailProcessReportHandler;
import com.jujunie.integration.IntegrationException;
import com.jujunie.integration.ProcessReportHandler;
import com.jujunie.integration.database.DumpInfo.STATUS;
import com.jujunie.integration.internal.InsufficientRightsException;
import com.jujunie.integration.internal.UserVO;
import com.jujunie.service.Container;

/**
 * Controls Dump execution and existance
 * @author Julien B�ti
 * @since 0.06.01
 */
public abstract class DumpController {

    /** Logger */
    private static final Log log = LogFactory.getLog(DumpController.class);

    /** Schema information */
    private SchemaInfo schemaInfo = null;

    /** My container */
    private DumpControllerContainer myContainer = null;

    /** Please don't use. Use {@link #getInstance(SchemaInfo, Container)} instead */
    public DumpController() {
    }

    /**
     * Get DumpController instance corresponding to the given schema information
     * @param s schema information
     * @param c container to store the dump controller
     * @return dump controller
     * @throws DBException on Dump controll instanciation error
     */
    public static synchronized DumpController getInstance(SchemaInfo s, Container c) throws DBException {
        String key = s.getIdentifierKey();
        log.debug("Getting DumpController for Schema: " + key);
        DumpController res = DumpControllerContainer.getInstance(c).getDumpController(s);
        if (res == null || res.getSchemaInfo() != s) {
            log.debug("No controller yet :p, or not with the rigth schema information");
            try {
                res = s.getDatabase().getType().getDumpController().newInstance();
            } catch (IllegalAccessException e) {
                throw new DBException("Cannot instanciate dump controller: " + e.getMessage(), e);
            } catch (InstantiationException e) {
                throw new DBException("Cannot instanciate dump controller: " + e.getMessage(), e);
            }
            res.schemaInfo = s;
            res.myContainer = DumpControllerContainer.getInstance(c);
            res.myContainer.setDumpController(res);
        } else {
            log.debug("DumpController found in container");
        }
        return res;
    }

    /**
     * @return schema information
     */
    SchemaInfo getSchemaInfo() {
        return this.schemaInfo;
    }

    /**
     * Check if DumpController instance corresponding to the given schema information exist
     * @param s schema information
     * @param c container to store the dump controller
     * @return true if exist
     */
    public static boolean exist(SchemaInfo s, Container c) throws DBException {
        DumpController res = (DumpController) c.getAttribute(s.getIdentifierKey());
        return res != null;
    }

    /**
     * @return dump export information
     */
    public DumpInfo getDumpInfo(UserVO user) throws DBException {
        DumpInfo info = this.getSpecificDumpInfo();
        Date now = new Date();
        int retention = 0;
        try {
            retention = DatabaseConfiguration.getConfiguration().getDumpRetentionHours();
        } catch (ConfigurationException e) {
            throw new DBException("Configuration exception while getting retention time: " + e.getMessage(), e);
        }
        if (!user.canLaunchExport()) {
            info.setBlocked(true);
            info.setBlockedReason("You do not have enought rights to launch dumps exports...");
        } else if (info.status() == STATUS.READY) {
            info.setBlocked(false);
            info.setBlockedReason("");
        }
        if (info.status() == STATUS.RUNNING) {
            info.setBlocked(true);
            info.setBlockedReason("Cannot launch an export as it is already launched. Please be patient!");
        } else if (info.status() == STATUS.FAILED) {
            info.setBlocked(false);
            info.setBlockedReason("");
        } else if (info.status() == STATUS.SUCCESS) {
            if (info != null && info.lastCallDate() != null && (info.lastCallDate().getTime() + 3600000 * retention) > now.getTime()) {
                info.setBlocked(true);
                info.setBlockedReason("Export already launched less than 12 hours ago. Please use the current one");
            } else {
                info.setBlocked(false);
                info.setStatus(STATUS.READY);
            }
        }
        if (!info.isBlocked()) {
            if (!this.checkMaxThreadOK()) {
                info.setBlocked(true);
                info.setBlockedReason("Too many dumps are running, either globaly or on the same server." + " Please try again later");
            }
        }
        return info;
    }

    /**
     * @return dump export information
     */
    public abstract DumpInfo getSpecificDumpInfo();

    public synchronized void launchExport(UserVO user) throws DBException, IntegrationException {
        if (!user.canLaunchExport()) {
            throw new InsufficientRightsException("Launch dump export");
        }
        DumpInfo info = this.getDumpInfo(user);
        Date now = new Date();
        if (!info.isBlocked()) {
            if (this.schemaInfo.getSize() < 0l) {
                throw new DBException("Cannot launch export as schema size not yet initialized");
            }
            Collection<ProcessReportHandler> notifiers = new LinkedList<ProcessReportHandler>();
            String permanentEmailNotifier = DatabaseConfiguration.getConfiguration().getPermanentMailNotification();
            if (!StringUtils.isEmpty(permanentEmailNotifier)) {
                EMailProcessReportHandler notifier = new EMailProcessReportHandler();
                notifier.addTo(permanentEmailNotifier);
                notifier.addTo(user.getEmail());
                notifier.setSubject("[Integration] Dump export notification for: " + this.getSchemaInfo().getIdentifierKey() + " by " + user.getName());
                notifiers.add(notifier);
            }
            try {
                String dumpRepositoryPath = DatabaseConfiguration.getConfiguration().getDumpRepository();
                this.checkSpaceLeftAndCleanup(dumpRepositoryPath, user);
                this.cleanup(dumpRepositoryPath, user);
                info.setLastCallDate(now);
                this.launchSpecificExport(dumpRepositoryPath, DatabaseConfiguration.getConfiguration().getDumpEnvironmentParameters(this.schemaInfo.getDatabase().getType()), notifiers);
            } catch (ConfigurationException e) {
                throw new DBException("Configuration exception while getting dump parameters: " + e.getMessage(), e);
            }
        } else {
            throw new DBException("Cannot launch export: " + info.getBlockedReason());
        }
    }

    /**
     * Launch export
     * @param repositoryPath path to the storage repo
     * @param envp environment parameters
     * @throws DBException if an export is already running
     */
    abstract void launchSpecificExport(String repositoryPath, Map<String, String> envp, Collection<ProcessReportHandler> notifiers) throws DBException, IntegrationException;

    private boolean checkMaxThreadOK() throws DBException {
        int max = 0, maxServer = 0;
        try {
            max = DatabaseConfiguration.getConfiguration().getMaxRunning();
            maxServer = DatabaseConfiguration.getConfiguration().getMaxServerRunning();
        } catch (ConfigurationException e) {
            throw new DBException("Configuration exception while getting max running dumps: " + e.getMessage(), e);
        }
        int running = 0, runningServer = 0;
        for (DumpController dc : this.myContainer.getDumpControllers()) {
            if (dc.getSpecificDumpInfo().status() == STATUS.RUNNING) {
                running++;
                if (dc.getSchemaInfo().getDatabase().getHost().equals(this.schemaInfo.getDatabase().getHost())) {
                    runningServer++;
                }
            }
        }
        return running < max && runningServer < maxServer;
    }

    private void checkSpaceLeftAndCleanup(String path, UserVO user) throws DBException {
        long freeSpace = this.getFreeSpace(path);
        if (freeSpace < this.schemaInfo.getSize() / 1024) {
            for (DumpController dc : this.myContainer.getDumpControllers()) {
                dc.cleanup(path, user);
            }
        } else {
            return;
        }
        try {
            freeSpace = FileSystemUtils.freeSpaceKb(path);
        } catch (IOException e) {
            throw new DBException("IOException while getting system free space: " + e.getMessage(), e);
        }
        if (freeSpace < this.schemaInfo.getSize() / 1024) {
            throw new DBException("Cannot export as free disk space: " + (freeSpace / 1024) + "Mb is not enought for schema size: " + (this.schemaInfo.getSize() / 1024 / 1024) + "Mb");
        }
    }

    /**
     * Getting avaliable free space by getting system freespace, and removing any running export schema size.
     * @return available free space
     */
    private long getFreeSpace(String path) throws DBException {
        long res = 0l;
        try {
            res = FileSystemUtils.freeSpaceKb(path);
        } catch (IOException e) {
            throw new DBException("IOException while getting system free space: " + e.getMessage(), e);
        }
        for (DumpController dc : this.myContainer.getDumpControllers()) {
            res -= dc.getSchemaInfo().getSize() / 1024;
        }
        return res;
    }

    private void cleanup(String path, UserVO user) throws DBException {
        DumpInfo info = this.getDumpInfo(user);
        int retention = 0;
        Date now = new Date();
        try {
            retention = DatabaseConfiguration.getConfiguration().getDumpRetentionHours();
        } catch (ConfigurationException e) {
            throw new DBException("Configuration exception while getting retention time: " + e.getMessage(), e);
        }
        if (info != null && info.lastCallDate() != null && (info.lastCallDate().getTime() + 3600000 * retention) < now.getTime()) {
            new File(path + File.separatorChar + info.getLogFileName()).delete();
            new File(path + File.separatorChar + info.getDumpFileName()).delete();
            new File(path + File.separatorChar + info.getMD5SumFileName()).delete();
            info.setLogFileName(null);
            info.setDumpFileName(null);
        }
    }

    /**
     * Dump thread
     * @author Julien Béti
     */
    static class DumpThread extends Thread {

        /** Logger */
        private static final Log log = LogFactory.getLog(DumpThread.class);

        private Process process = null;

        private ProcessBuilder pb = null;

        private Collection<ProcessReportHandler> notifiers = null;

        private DumpInfo info = null;

        /**
         * Launches the thread 
         */
        DumpThread(List<String> cmd, String basePath, Collection<ProcessReportHandler> notifiers, DumpInfo info) throws IntegrationException {
            log.debug("Creating new DumpThread");
            this.pb = new ProcessBuilder(cmd);
            this.pb.directory(new File(basePath));
            this.notifiers = notifiers;
            this.info = info;
            log.debug("Current env. variables:");
            this.startNotifiers();
            this.infoNotifiers("Dump thread initialized with following parameters:");
            this.parameterNotifiers("Base path", basePath);
            this.parameterNotifiers("Command  ", StringUtils.join(cmd.iterator(), ' '));
            this.infoNotifiers("Environment parameters before modifications:");
            this.logEnvironment();
        }

        @Override
        public void run() {
            log.debug("Now running....");
            log.debug("Current env. variables:");
            try {
                this.infoNotifiers("Environment parameters after modifications:");
                this.logEnvironment();
                this.infoNotifiers("Dump thread will now run...");
                this.endNotifiers();
                this.process = this.pb.start();
                this.process.waitFor();
                if (this.process.exitValue() != 0) {
                    this.startNotifiers();
                    this.infoNotifiers("Dump Failed. Return status: " + this.process.exitValue());
                    this.endNotifiers();
                    return;
                }
                List<String> cmd = new LinkedList<String>();
                cmd.add("gzip");
                cmd.add(info.getDumpFileName());
                File basePath = this.pb.directory();
                this.pb = new ProcessBuilder(cmd);
                this.pb.directory(basePath);
                log.debug("Executing: " + StringUtils.join(cmd.iterator(), ' '));
                this.process = this.pb.start();
                this.process.waitFor();
                if (this.process.exitValue() != 0) {
                    this.startNotifiers();
                    this.infoNotifiers("Dump GZip Failed. Return status: " + this.process.exitValue());
                    this.endNotifiers();
                    return;
                }
                info.setDumpFileName(info.getDumpFileName() + ".gz");
                info.setMD5SumFileName(info.getDumpFileName() + ".md5sum");
                cmd = new LinkedList<String>();
                cmd.add("md5sum");
                cmd.add("-b");
                cmd.add(info.getDumpFileName());
                log.debug("Executing: " + StringUtils.join(cmd.iterator(), ' '));
                this.pb = new ProcessBuilder(cmd);
                this.pb.directory(basePath);
                this.process = this.pb.start();
                BufferedOutputStream md5sumFileOut = new BufferedOutputStream(new FileOutputStream(basePath.getAbsolutePath() + File.separatorChar + info.getMD5SumFileName()));
                IOUtils.copy(this.process.getInputStream(), md5sumFileOut);
                this.process.waitFor();
                md5sumFileOut.flush();
                md5sumFileOut.close();
                if (this.process.exitValue() != 0) {
                    this.startNotifiers();
                    this.infoNotifiers("Dump GZip MD5Sum Failed. Return status: " + this.process.exitValue());
                    this.endNotifiers();
                    return;
                } else {
                    this.startNotifiers();
                    this.infoNotifiers("Dump, gzip and md5sum sucessfuly completed.");
                    this.endNotifiers();
                }
            } catch (IOException e) {
                String message = "IOException launching command: " + e.getMessage();
                log.error(message, e);
                throw new IllegalStateException(message, e);
            } catch (InterruptedException e) {
                String message = "InterruptedException launching command: " + e.getMessage();
                log.error(message, e);
                throw new IllegalStateException(message, e);
            } catch (IntegrationException e) {
                String message = "IntegrationException launching command: " + e.getMessage();
                log.error(message, e);
                throw new IllegalStateException(message, e);
            }
        }

        /**
         * Add environment variable
         * @param name name
         * @param value value
         */
        void addEnvParam(String name, String value) {
            log.debug("Adding env. param: " + name + '=' + value);
            this.pb.environment().put(name, value);
        }

        void logEnvironment() throws IntegrationException {
            for (Map.Entry<String, String> entry : this.pb.environment().entrySet()) {
                log.debug(entry.getKey() + '=' + entry.getValue());
                this.parameterNotifiers(entry.getKey(), entry.getValue());
            }
        }

        void infoNotifiers(String infoMsg) throws IntegrationException {
            for (ProcessReportHandler notifier : this.notifiers) {
                notifier.info(infoMsg);
            }
        }

        void parameterNotifiers(String name, String value) throws IntegrationException {
            for (ProcessReportHandler notifier : this.notifiers) {
                notifier.parameter(name, value);
            }
        }

        void startNotifiers() throws IntegrationException {
            for (ProcessReportHandler notifier : this.notifiers) {
                notifier.start();
            }
        }

        void endNotifiers() throws IntegrationException {
            for (ProcessReportHandler notifier : this.notifiers) {
                notifier.end();
            }
        }

        void updateStatus() {
            if (this.getState() == Thread.State.NEW) {
                this.info.setStatus(STATUS.READY);
            } else if (this.getState() == Thread.State.BLOCKED || this.getState() == Thread.State.RUNNABLE || this.getState() == Thread.State.TIMED_WAITING || this.getState() == Thread.State.WAITING) {
                this.info.setStatus(STATUS.RUNNING);
            } else if (this.getState() == Thread.State.TERMINATED) {
                if (this.process.exitValue() != 0) {
                    this.info.setStatus(STATUS.FAILED);
                } else {
                    this.info.setStatus(STATUS.SUCCESS);
                }
            }
        }
    }
}

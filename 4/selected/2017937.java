package net.sf.jabs.task.cmd;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import net.sf.jabs.data.Fields;
import net.sf.jabs.data.project.ProjectDAO;
import net.sf.jabs.data.report.ReportDAO;
import net.sf.jabs.jobs.JobMonitorMessage;
import net.sf.jabs.process.SystemExec;
import net.sf.jabs.task.BaseTaskExec;
import net.sf.jabs.task.TaskStartupException;
import net.sf.jabs.util.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ofbiz.core.entity.GenericValue;

public class ExecTaskRunner extends BaseTaskExec {

    private static Log _log = LogFactory.getLog(ExecTaskRunner.class);

    protected void startup() throws TaskStartupException {
        if (_log.isDebugEnabled()) _log.info("Starting up command task");
    }

    protected void run() {
        _log.info("Running task: " + _task.get(Fields.ProjectTask.NAME));
        if (_log.isDebugEnabled()) {
            _log.info("Command: " + _loader.getProperty(_task.getString(Fields.ProjectTask.COMMAND), true));
            _log.info("Order: " + _task.get(Fields.ProjectTask.ORDER));
        }
        int taskError = 0;
        SystemExec e = new SystemExec();
        e.setEchoAll(false);
        e.setEnvironment(_loader.getEnvironment());
        JobMonitorMessage jmm = observableMessage("Running " + _task.getString(Fields.ProjectTask.NAME));
        e.setDirectory(getDirectory());
        setLogTitle();
        configureTaskEval(_task, e);
        Long timeout = getLong("timeout");
        if (timeout != null && timeout > 0) {
            e.setTimeout(timeout);
        }
        e.setJobMonitor(jmm);
        e.run(_loader.getProperty(_task.getString(Fields.ProjectTask.COMMAND), true));
        taskError = e.error() ? e.getReturnCode() : e.getParseErrorCount();
        if (taskError > 0) {
            _errors++;
            observableMessage(_task.getString(Fields.ProjectTask.NAME) + " returned an error status.");
            if (e.isInterrupted()) {
                setHaltCondition();
            } else {
                setStopCondition();
            }
        }
        if (e.getParseWarningCount() > 0) {
            _warnings++;
        }
        setLogResults(e.getStdOut(), e.getStdErr());
        setErrors(taskError, e.getParseWarningCount());
        setComplete();
        storeAttachments(_task, _reportLog);
    }

    protected void shutdown() {
        if (_log.isDebugEnabled()) _log.info("Shutting down exec task");
    }

    private void storeAttachments(GenericValue task, GenericValue report) {
        _log.info("Processing attachments");
        ProjectDAO projectDAO = new ProjectDAO();
        List<GenericValue> attachments = projectDAO.getProjectTaskAttach(task);
        for (GenericValue attachment : attachments) {
            _log.info("Saving attachment " + attachment.getString("file"));
            String aSource = _loader.getProperty(attachment.getString("file"), true);
            String aDest = _loader.getProperty(Constants.SYSTEM_CONFIG_ATTACH_PATH, false);
            _log.info("Source " + aSource);
            _log.info("Destination " + aDest);
            File fs = new File(aSource);
            if (fs.getParent() == null) {
                fs = new File(getDirectory() + Constants.FILE_SEP_FW + aSource);
            }
            if (fs.exists()) {
                GenericValue e = ReportDAO.getNewReportAttachment();
                String destReportDir = task.getString("project") + Constants.FILE_SEP_FW + report.getString("projectReport") + Constants.FILE_SEP_FW + e.getString("id");
                aDest += Constants.FILE_SEP_FW + destReportDir;
                File fd = new File(aDest);
                if (!fd.exists()) fd.mkdirs();
                try {
                    if (fs.isFile()) {
                        FileUtils.copyFileToDirectory(fs, fd);
                    } else {
                        FileUtils.copyDirectory(fs, fd);
                    }
                } catch (IOException ex) {
                    _log.error("Unable to save attachment", ex);
                }
                e.set("log", report.get("id"));
                e.set("file", destReportDir);
                e.set("url", destReportDir);
                e.set("description", attachment.get("description"));
                _attachments.add(e);
            } else {
                _log.warn("File attachment [" + fs.getAbsolutePath() + "] does not exist");
            }
        }
    }

    private void configureTaskEval(GenericValue task, SystemExec e) {
        ProjectDAO projectDAO = new ProjectDAO();
        List<GenericValue> taskEval = projectDAO.getProjectTaskEval(task);
        Iterator<GenericValue> i = taskEval.iterator();
        while (i.hasNext()) {
            GenericValue g = (GenericValue) i.next();
            e.setTestCondition(g.getString("stream"), g.getString("type"), g.getString("expression"));
        }
    }
}

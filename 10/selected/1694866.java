package com.entelience.probe;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.entelience.objects.Incident;
import com.entelience.objects.asset.AssetId;
import com.entelience.objects.probes.ProbeParameter;
import com.entelience.objects.probes.ProbeStatus;
import com.entelience.provider.DbIncident;
import com.entelience.sql.Db;
import com.entelience.sql.DbHelper;
import com.entelience.sql.UsesDbObject;
import com.entelience.unix.GetPid;
import com.entelience.util.DateHelper;
import com.entelience.util.StringHelper;
import com.entelience.util.Version;
import com.entelience.util.Config;

/**
 * Store a more traditional view of probes running.  This will provide
 * information to the original probe history tables, so that the cli
 * reporting tools are independant of the remote / local file state
 * information.
 */
public final class ProbeHistoryDb {

    private ProbeHistoryDb() {
    }

    private static org.apache.log4j.Logger _logger = com.entelience.util.Logs.getProbeLogger();

    private static final Object lock = new Object();

    private static final GetPid pidGenerator = new GetPid();

    private static final Set<ProbeBase> unstartedProbes = new HashSet<ProbeBase>();

    /**
     * Can I get a lock for this probe before running it.
     * If not I actually need to exit all the probes...
     *
     * When in the unit tests it actually just removes all the locks and returns
     * true.  There shouldn't be two unit test cases running at the same time even
     * if things go really wierd.
     * 
     * @return true if you should be able to get a lock.
     */
    public static synchronized boolean canGetLock(Db statusDb, ProbeBase probeBase) throws ControlProblem, Exception {
        if (statusDb == null) {
            _logger.warn("StatusDb is null");
            return false;
        }
        if (probeBase == null) throw new IllegalArgumentException("The ProbeBasse cannot be null");
        boolean serialMode = Config.getProperty(statusDb, "com.entelience.esis.probe.serialMode", true);
        int maxParallel = Config.getProperty(statusDb, "com.entelience.esis.probe.maximumParallelRun", 2);
        boolean threadMode = Config.getProperty(statusDb, "com.entelience.esis.probe.threadMode", false);
        int maxThreads = Config.getProperty(statusDb, "com.entelience.esis.probe.maximumGlobalThreads", 4);
        if (maxParallel > maxThreads) throw new IllegalStateException("The maximum number of parallel run (" + maxParallel + ") cannot be greater than the maximum global threads (" + maxThreads + ") , at least one thread per run");
        if (maxThreads < 1 || maxParallel < 1) throw new IllegalStateException("The maximum number of parallel run (" + maxParallel + ") and maximum global threads (" + maxThreads + ") must be at least 1");
        _logger.info("Running probe in [serialMode: " + serialMode + ", maxParallel: " + maxParallel + ", threadMode: " + threadMode + "]");
        if (com.entelience.util.StaticConfig.inUnitTests) {
            _logger.warn("In Unit Test, truncating e_probe_pid");
            statusDb.begin();
            try {
                statusDb.executeSql("TRUNCATE e_probe_pid;");
            } catch (Exception e) {
                statusDb.rollback();
                throw e;
            } finally {
                statusDb.commitUnless();
            }
            return true;
        } else {
            _logger.debug("Entering in synchronized section");
            try {
                statusDb.enter();
                int lockCount = DbHelper.getIntKey(statusDb.prepareStatement("SELECT count(probe_name) FROM e_probe_pid"));
                if (lockCount == 0) return true;
                int threadsCount = DbHelper.getIntKey(statusDb.prepareStatement("SELECT SUM(thread) FROM e_probe_pid"));
                int currentPid = pidGenerator.getPid();
                _logger.info("The current pid is [" + currentPid + "] with [" + lockCount + "] probe marked as running and [" + threadsCount + "] threads");
                StringBuffer bf = new StringBuffer();
                ResultSet rs = statusDb.executeQuery(statusDb.prepareStatement("SELECT lower(probe_name), run_date, pid FROM e_probe_pid"));
                if (rs.next()) {
                    String probeName = rs.getString(1);
                    Date when = DateHelper.toDateOrNull(rs.getTimestamp(2));
                    int pid = (rs.getObject(3) == null ? -1 : rs.getInt(3));
                    if ((lockCount == maxParallel) || (threadsCount == maxThreads)) {
                        do {
                            probeName = rs.getString(1);
                            when = DateHelper.toDateOrNull(rs.getTimestamp(2));
                            pid = (rs.getObject(3) == null ? -1 : rs.getInt(3));
                            bf.append("Probe (" + probeName + ") has been running since (" + DateHelper.HTMLDateOrNull(when) + ") with pid (" + pid + ")");
                            _logger.warn("Probe (" + probeName + ") has been running since (" + DateHelper.HTMLDateOrNull(when) + ") with pid (" + pid + ")");
                        } while (rs.next());
                        if (lockCount == maxParallel) throw new ControlProblem("Maximum simultaneous probe run (" + maxParallel + ") has been reached " + bf.toString());
                        if (maxThreads == threadsCount) throw new ControlProblem("Maximum simultaneous probe threads (" + maxThreads + ") has been reached " + bf.toString());
                    } else {
                        if ((serialMode && !threadMode) || (!serialMode && !threadMode && probeBase.getClass().getName().equalsIgnoreCase(probeName)) || (serialMode && threadMode && !probeBase.getClass().getName().equalsIgnoreCase(probeName))) {
                            throw new ControlProblem("Cannot run (" + probeBase.getClass().getName() + ") as (" + probeName + ") has been running since (" + DateHelper.HTMLDateOrNull(when) + ") with pid (" + pid + ") with (serial: " + serialMode + ", thread: " + threadMode + ")");
                        } else if (threadMode && probeBase.getClass().getName().equalsIgnoreCase(probeName)) {
                            if (currentPid != pid) {
                                throw new ControlProblem("The current process (" + currentPid + ")is not the process that launched probe (" + probeName + ") which has been running since (" + DateHelper.HTMLDateOrNull(when) + ") with pid (" + pid + ")");
                            }
                            if (probeBase instanceof FileProbe) {
                                _logger.info("Allowing probe (" + probeBase.getClass().getName() + ") to execute in threaded mode");
                                return true;
                            } else {
                                _logger.warn("Only FileProbe cannot be run in threaded mode");
                                throw new ControlProblem("The probe (" + probeName + ") has been running since (" + DateHelper.HTMLDateOrNull(when) + ") with pid (" + pid + ") and doesn't support concurrency");
                            }
                        } else {
                            _logger.warn("Running probes in non serial mode, multiple different probes can run at the same time");
                            _logger.warn("Probe (" + probeBase.getClass().getName() + ") will run in parallel of others");
                            return true;
                        }
                    }
                }
                return true;
            } finally {
                statusDb.exit();
            }
        }
    }

    /**
     */
    protected static void checkProbesWithoutImports(Db statusDb) throws Exception {
        StringBuffer msg = new StringBuffer("Probes that did not import anything : ");
        StringBuffer summ = new StringBuffer("");
        StringBuffer probeName = new StringBuffer();
        int nbProbes = 0;
        for (ProbeBase p : unstartedProbes) {
            if (p.isSendIncidentWhenNoData()) {
                _logger.info("Probe " + p.getClass().getName() + " did not import anything, sending incident");
                ++nbProbes;
                msg.append('\n').append(p.getClass().getName());
                if (nbProbes > 1) probeName.append('\n');
                probeName.append(p.getClass().getName());
            } else {
                _logger.debug("Probe " + p.getClass().getName() + " did not import anything. No incident will be generated due to probe configuration");
            }
        }
        if (nbProbes == 0) {
            return;
        }
        Incident i = new Incident();
        i.setProbeMessage(msg.toString());
        i.setProbeName(probeName.toString());
        i.setEsisVersion(Version.getVersionString());
        i.setSummary(summ.toString());
        try {
            statusDb.begin();
            DbIncident.saveIncident(statusDb, i, new ArrayList<Exception>());
        } catch (Exception e) {
            statusDb.rollback();
            throw e;
        } finally {
            statusDb.commitUnless();
        }
    }

    /**
     * Mark a probe being started.
     *
     * Actually get a lock. Check you can get one before !
     *
     *
     * @return probe_history_id for subsequent database calls
     */
    public static int markStarted(Db statusDb, ProbeBase probeBase, boolean trialRun) throws Exception, ControlProblem {
        _logger.debug("Marking probe (" + probeBase.getClass().getName() + ") as started with trial (" + trialRun + ")");
        if (trialRun) return -1;
        _logger.debug("Entering in synchronized section");
        synchronized (lock) {
            if (!canGetLock(statusDb, probeBase)) throw new Exception("Unable to get a lock for probe (" + probeBase.getClass().getName() + ")");
            try {
                statusDb.begin();
                createLock(statusDb, probeBase);
                setProbeRunning(statusDb, probeBase);
                unstartedProbes.remove(probeBase);
                _logger.debug("The probe (" + probeBase.getClass().getName() + ") has been marked as started with probe history id (" + probeBase.getProbeHistoryId() + ")");
                return probeBase.getProbeHistoryId();
            } catch (Exception e) {
                statusDb.rollback();
                throw e;
            } finally {
                statusDb.commitUnless();
                _logger.debug("Leaving synchronized section");
            }
        }
    }

    /**
     * get a probe history id for a probe
     */
    public static int markProbeStarted(Db statusDb, ProbeBase probeBase, boolean trialRun) throws ControlProblem, Exception {
        _logger.debug("Marking probe (" + probeBase.getClass().getName() + ") as about to start with trial (" + trialRun + ")");
        if (trialRun) return -1;
        _logger.debug("Entering in synchronized section");
        synchronized (lock) {
            try {
                statusDb.begin();
                Integer thread = DbHelper.getKey(statusDb.prepareStatement("SELECT thread FROM e_probe_pid WHERE lower(probe_name) = lower('" + probeBase.getClass().getName() + "')"));
                if (thread != null) {
                    System.out.println("The probe (" + probeBase.getClass().getName() + ") is marked as already started. Use stop to kill id and or reset its state");
                    throw new IllegalStateException("The probe (" + probeBase.getClass().getName() + ") is already started");
                }
                PreparedStatement ps_start = statusDb.prepareStatement("INSERT INTO e_probe_history (probe_name, run_date, no_data, e_probe_run_status_id) VALUES (?, current_timestamp, true, ?) RETURNING e_probe_history_id");
                ps_start.setString(1, probeBase.getClass().getName());
                ps_start.setInt(2, ProbeStatus.NO_DATA);
                int id = DbHelper.getKey(ps_start);
                probeBase.setProbeHistoryId(id);
                unstartedProbes.add(probeBase);
                return id;
            } catch (Exception e) {
                statusDb.rollback();
                throw e;
            } finally {
                statusDb.commitUnless();
                _logger.debug("Leaving synchronized section");
            }
        }
    }

    /**
     */
    private static void removeLock(Db statusDb, ProbeBase probeBase) throws Exception {
        _logger.debug("Removing PID lock for (" + probeBase.getClass().getName() + ")");
        PreparedStatement ps_remove_lock = statusDb.prepareStatement("DELETE FROM e_probe_pid WHERE lower(probe_name)=lower(?)");
        ps_remove_lock.setString(1, probeBase.getClass().getName());
        statusDb.executeUpdate(ps_remove_lock);
    }

    /**
     */
    private static void createLock(Db statusDb, ProbeBase pb) throws Exception {
        _logger.trace("Creating PID lock for probe (" + pb.getClass().getName() + ")");
        Integer thread = DbHelper.getKey(statusDb.prepareStatement("SELECT thread FROM e_probe_pid WHERE lower(probe_name) = lower('" + pb.getClass().getName() + "')"));
        if (thread != null) throw new IllegalStateException("The probe (" + pb.getClass().getName() + ") is already started");
        int pid = pidGenerator.getPid();
        if (pid == -1) throw new IllegalStateException("Unable to get the PID for the running process !");
        PreparedStatement ps_create_lock = statusDb.prepareStatement("INSERT INTO e_probe_pid (probe_name, pid) VALUES (?, ?)");
        ps_create_lock.setString(1, pb.getClass().getName());
        ps_create_lock.setInt(2, pid);
        statusDb.executeUpdate(ps_create_lock);
    }

    /**
     */
    private static void setProbeRunning(Db statusDb, ProbeBase probeBase) throws Exception {
        PreparedStatement ps_start_elt = statusDb.prepareStatement("UPDATE e_probe_history SET run_date=current_timestamp, no_data = false, e_probe_run_status_id = ? WHERE e_probe_history_id=?");
        ps_start_elt.setInt(1, ProbeStatus.STILL_RUNNING);
        ps_start_elt.setInt(2, probeBase.getProbeHistoryId());
        statusDb.executeUpdate(ps_start_elt);
    }

    /**
     * Returns the probe PID or null
     */
    public static Integer getProbePID(Db statusDb, String probeName) throws Exception {
        PreparedStatement pst = statusDb.prepareStatement("SELECT pid FROM e_probe_pid WHERE lower(probe_name) = lower(?)");
        pst.setString(1, probeName);
        Integer pid = DbHelper.getKey(pst);
        _logger.debug("Probe (" + probeName + ")" + (pid == null ? " is not " : " is ") + " running");
        return pid;
    }

    /**
     * Specialisation for file probe, store the local filename in the old file status tables.  These
     * are still used by various webservices, and its the only place where this information is stored.
     */
    public static int markStarted(Db statusDb, FileProbe fileProbe, LocalFileState lfs, boolean trialRun) throws ControlProblem, Exception {
        if (lfs == null) throw new IllegalArgumentException("The LocalFileState cannot be null");
        _logger.debug("Marking FileProbe (" + fileProbe.getClass().getName() + ") has running with " + lfs + " in trial (" + trialRun + ")");
        if (trialRun) return -1;
        _logger.debug("Entering in synchronized section");
        synchronized (lock) {
            try {
                statusDb.begin();
                if (getProbePID(statusDb, fileProbe.getClass().getName()) == null) {
                    createLock(statusDb, fileProbe);
                    setProbeRunning(statusDb, fileProbe);
                    unstartedProbes.remove(fileProbe);
                } else {
                    PreparedStatement add_thread = statusDb.prepareStatement("UPDATE e_probe_pid SET thread=1 WHERE lower(probe_name) = lower(?)");
                    add_thread.setString(1, fileProbe.getClass().getName());
                    statusDb.executeUpdate(add_thread);
                }
                Integer phId = DbHelper.getKey(statusDb.prepareStatement("SELECT probe_history_id FROM e_local_file_state WHERE local_file_state_id=" + lfs.local_file_state_id));
                if (phId != null && phId.intValue() != fileProbe.getProbeHistoryId()) _logger.warn("The local file state id (" + lfs.local_file_state_id + ") is already linked to probe history id (" + fileProbe.getProbeHistoryId() + ")");
                PreparedStatement ps_start_file1 = statusDb.prepareStatement("UPDATE e_local_file_state SET probe_history_id=? WHERE local_file_state_id=?");
                ps_start_file1.setInt(1, fileProbe.getProbeHistoryId());
                ps_start_file1.setInt(2, lfs.local_file_state_id);
                int res = statusDb.executeUpdate(ps_start_file1);
                if (res != 1) throw new IllegalStateException("Unable to update the local file state record (" + lfs.local_file_state_id + ") for probe history record (" + fileProbe.getProbeHistoryId() + ")");
                PreparedStatement ps_start_file2 = statusDb.prepareStatement("INSERT INTO e_probe_file_history (e_probe_history_id, file_name, file_size, e_local_file_state_id) VALUES (?, ?, ?, ?)");
                ps_start_file2.setInt(1, fileProbe.getProbeHistoryId());
                ps_start_file2.setString(2, lfs.file.getAbsolutePath());
                ps_start_file2.setLong(3, lfs.file.length());
                ps_start_file2.setLong(4, lfs.local_file_state_id);
                res = statusDb.executeUpdate(ps_start_file2);
                if (res != 1) throw new IllegalStateException("Unable to insert probe file history record for file (" + lfs.file.getAbsolutePath() + ") and probe history record (" + fileProbe.getProbeHistoryId() + ")");
                if (fileProbe.isAssetProbe()) {
                    fillAssetProbeDetails(statusDb, fileProbe);
                }
                return fileProbe.getProbeHistoryId();
            } catch (Exception e) {
                statusDb.rollback();
                throw e;
            } finally {
                statusDb.commitUnless();
                _logger.debug("Leaving synchronized section");
            }
        }
    }

    /**
     * get last imported timestamp for history and fill the probe with it
     *
     */
    private static void fillAssetProbeDetails(Db db, FileProbe ap) throws Exception {
        try {
            db.enter();
            AssetId assId = ap.getAssetId();
            if (assId == null) {
                _logger.warn("AssetProbe not related to any asset !");
                return;
            }
            PreparedStatement ps_get_latest_imported_ts_for_asset = db.prepareStatement("SELECT MAX(last_imported_ts) FROM e_probe_asset_history pah " + " INNER JOIN e_probe_history ph ON ph.e_probe_history_id = pah.e_probe_history_id " + " WHERE ph.probe_name = ? AND e_asset_id = ?");
            ps_get_latest_imported_ts_for_asset.setString(1, ap.getClass().getName());
            ps_get_latest_imported_ts_for_asset.setInt(2, assId.getId());
            ResultSet rs = db.executeQuery(ps_get_latest_imported_ts_for_asset);
            Date last = null;
            if (rs.next()) {
                last = DateHelper.toDateOrNull(rs.getTimestamp(1));
            } else {
                _logger.info("Probe (" + ap.getClass().getName() + ") has never run for asset (" + assId + ")");
            }
            _logger.info("Last imported timestamp is (" + last + ") for probe (" + ap.getClass().getName() + ")");
            ap.setLastImportedTimestamp(last);
        } finally {
            db.exit();
        }
    }

    /**
     * Update the probe history saying that this probe has finished.
     *
     * @param probeBase probe that has run
     * @param success if true then the probe ran successfully, false then it either failed or got an error
     * @param error if true then the probe had an error, implies that success is false.
     * @param ex exception if any (can be null)
     * @param message any message to store in the database
     */
    public static void markFinished(Db statusDb, ProbeBase probeBase, boolean success, boolean error, boolean trialRun) throws Exception {
        _logger.debug("Marking probe (" + probeBase.getClass().getName() + ") has finished with status (success: " + success + ", error: " + error + ") and trial (" + trialRun + ")");
        if (trialRun) return;
        if (success && error) throw new IllegalArgumentException("Cannot have both success and error set!");
        _logger.debug("Entering in synchronized section");
        synchronized (lock) {
            try {
                statusDb.begin();
                Integer thread = DbHelper.getKey(statusDb.prepareStatement("SELECT thread FROM e_probe_pid WHERE lower(probe_name) = lower('" + probeBase.getClass().getName() + "')"));
                if (thread == null) throw new IllegalStateException("Trying to mark as finished a non running probe (" + probeBase.getClass().getName() + ")");
                if (thread.intValue() > 1) {
                    _logger.trace("Decreasing thread count (" + (thread.intValue() + 1) + ") by one for probe (" + probeBase.getClass().getName() + ")");
                    PreparedStatement remove_thread = statusDb.prepareStatement("UPDATE e_probe_pid SET thread -= 1 WHERE lower(probe_name) = lower(?)");
                    remove_thread.setString(1, probeBase.getClass().getName());
                    statusDb.executeUpdate(remove_thread);
                }
            } catch (Exception e) {
                _logger.error("Exception while removing probe PID or decreasing thread count " + probeBase.getClass().getName(), e);
                statusDb.rollback();
                throw e;
            } finally {
                _logger.debug("Leaving synchronized section");
                statusDb.commitUnless();
            }
        }
    }

    /**
     */
    public static void markProbeFinished(Db statusDb, ProbeBase probeBase, boolean success, boolean error, Exception ex, String message, boolean trialRun) throws Exception {
        _logger.debug("Finalizing probe (" + probeBase.getClass().getName() + ") with (success: " + success + ", error: " + error + ", exception: " + (ex == null ? "null" : ex.getMessage()) + ", msg :" + message + ", trial: " + trialRun + ")");
        String msg = (ex != null && message == null ? StringHelper.getMessage(ex) : message);
        String stackTrace = (ex == null ? "" : StringHelper.getStackTraceAsString(ex));
        try {
            statusDb.begin();
            Integer state = DbHelper.getKey(statusDb.prepareStatement("SELECT e_probe_run_status_id FROM e_probe_history WHERE e_probe_history_id=" + probeBase.getProbeHistoryId()));
            if (state == null) throw new IllegalStateException("Trying to mark as finished an invalid probe history record (" + probeBase.getProbeHistoryId() + ")");
            if (state != ProbeStatus.STILL_RUNNING && state != ProbeStatus.NO_DATA) throw new IllegalStateException("Trying to mark as finished an already closed (" + state + ") probe history record (" + probeBase.getProbeHistoryId() + ")");
            Integer thread = DbHelper.getKey(statusDb.prepareStatement("SELECT thread FROM e_probe_pid WHERE lower(probe_name) = lower('" + probeBase.getClass().getName() + "')"));
            if (thread == null) throw new IllegalStateException("Finalizing a probe (" + probeBase.getClass().getName() + ") which is not running");
            if (thread.intValue() != 1) throw new IllegalStateException("Finalizing a probe (" + probeBase.getClass().getName() + ") with abnormal thread count (" + thread + ")");
            synchronized (lock) {
                removeLock(statusDb, probeBase);
            }
            PreparedStatement ps_finish_elt = statusDb.prepareStatement("UPDATE e_probe_history SET end_date=current_timestamp, success=?, error=?, error_message=?, exception_stacktrace=?, e_probe_run_status_id = ? WHERE e_probe_history_id=?");
            ps_finish_elt.setBoolean(1, success);
            ps_finish_elt.setBoolean(2, error);
            DbHelper.setSafeString(ps_finish_elt, 3, msg);
            DbHelper.setSafeString(ps_finish_elt, 4, stackTrace);
            int status = ProbeStatus.FAILURE;
            if (success) status = ProbeStatus.SUCCESS;
            if (error) status = ProbeStatus.ERROR;
            ps_finish_elt.setInt(5, status);
            ps_finish_elt.setInt(6, probeBase.getProbeHistoryId());
            statusDb.executeUpdate(ps_finish_elt);
            saveProbeParameters(statusDb, probeBase);
            if (probeBase instanceof FileProbe) {
                FileProbe fp = (FileProbe) probeBase;
                updateFileProbeHistory(statusDb, fp);
                if (fp.isAssetProbe()) {
                    fillAssetProbeHistory(statusDb, fp, fp.getProbeHistoryId());
                }
            }
            Incident i = new Incident();
            i.setProbeName(probeBase.getClass().getName());
            i.setEsisVersion(Version.getVersionString());
            i.setLastException(stackTrace);
            List<Exception> exs = new ArrayList<Exception>();
            if (ex != null) exs.add(ex);
            if (status == ProbeStatus.FAILURE && probeBase.isSendIncidentWhenFailure()) {
                _logger.warn("Probe " + probeBase.getClass().getName() + " ended with failure : generating incident due to probe configuration");
                i.setSummary("Probe " + probeBase.getClass().getName() + " failed to succeed");
                i.setProbeMessage("Probe " + probeBase.getClass().getName() + " failed to succeed");
                DbIncident.saveIncident(statusDb, i, null);
            } else if (status == ProbeStatus.ERROR && probeBase.isSendIncidentWhenError()) {
                _logger.warn("Probe " + probeBase.getClass().getName() + " ended with error : generating incident due to probe configuration");
                i.setSummary("Probe " + probeBase.getClass().getName() + " error : " + msg);
                i.setProbeMessage("Probe " + probeBase.getClass().getName() + " error : " + msg);
                DbIncident.saveIncident(statusDb, i, null);
            }
        } catch (Exception e) {
            _logger.error("Exception while setting probe to finished with message [" + msg + "] " + probeBase.getClass().getName(), e);
            statusDb.rollback();
            throw e;
        } finally {
            statusDb.commitUnless();
        }
    }

    /**
     */
    private static void updateFileProbeHistory(Db db, FileProbe fp) throws Exception {
        try {
            db.enter();
            PreparedStatement pstUpdFileHistory = db.prepareStatement("UPDATE e_probe_file_history SET count_elts = ?, count_elts_valid = ? WHERE e_probe_history_id = ?");
            pstUpdFileHistory.setLong(1, fp.getNbElements());
            pstUpdFileHistory.setLong(2, fp.getNbValidElements());
            pstUpdFileHistory.setInt(3, fp.getProbeHistoryId());
            db.executeUpdate(pstUpdFileHistory);
        } finally {
            db.exit();
        }
    }

    /**
     * Saves the parameters used for this run
     */
    private static void saveProbeParameters(Db db, ProbeBase pb) throws Exception {
        _logger.trace("Saving probe (" + pb.getClass().getName() + ") run parameters in history");
        if (pb.getUsedParameters() == null || pb.getUsedParameters().isEmpty()) return;
        try {
            db.enter();
            PreparedStatement pstGetParameterId = db.prepareStatement("SELECT e_probe_parameter_id FROM e_probe_parameter WHERE parameter_name = ?");
            PreparedStatement pstAddParameterId = db.prepareStatement("INSERT INTO e_probe_parameter (parameter_name) VALUES (?) RETURNING e_probe_parameter_id");
            PreparedStatement pstAddParameterHistory = db.prepareStatement("INSERT INTO e_probe_parameter_history (e_probe_history_id, e_probe_parameter_id, e_probe_parameter_origin_id, parameter_value) VALUES (?, ?, ?, ?)");
            for (ProbeParameter pp : pb.getUsedParameters()) {
                Integer parameterId;
                pstGetParameterId.setString(1, pp.getParameterName());
                parameterId = DbHelper.getKey(pstGetParameterId);
                if (parameterId == null) {
                    _logger.info("Adding probe parameter (" + pp.getParameterName() + ") to the referential");
                    pstAddParameterId.setString(1, pp.getParameterName());
                    parameterId = DbHelper.getIntKey(pstAddParameterId);
                }
                pstAddParameterHistory.setInt(1, pb.getProbeHistoryId());
                pstAddParameterHistory.setInt(2, parameterId);
                pstAddParameterHistory.setInt(3, pp.getOriginId());
                pstAddParameterHistory.setString(4, pp.getEffectiveValue());
                db.executeUpdate(pstAddParameterHistory);
            }
        } finally {
            db.exit();
        }
    }

    /**
     */
    private static void fillAssetProbeHistory(Db db, FileProbe ap, int historyId) throws Exception {
        try {
            db.enter();
            AssetId assId = ap.getAssetId();
            if (assId == null) throw new IllegalStateException("FileProbe not related to any asset !");
            Date lastImportedDate = ap.getLastTimestampCurrentRun();
            if (lastImportedDate == null) {
                _logger.error("AssetProbe has no lastTimestamp for this run");
                return;
            }
            PreparedStatement ps_add_asset_probe_history = db.prepareStatement("INSERT INTO e_probe_asset_history (e_asset_id, e_probe_history_id, last_imported_ts) VALUES (?, ?, ?)");
            ps_add_asset_probe_history.setInt(1, assId.getId());
            ps_add_asset_probe_history.setInt(2, historyId);
            ps_add_asset_probe_history.setTimestamp(3, DateHelper.sql(lastImportedDate));
            db.executeUpdate(ps_add_asset_probe_history);
        } finally {
            db.exit();
        }
    }
}

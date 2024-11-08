package com.entelience.probe;

import com.entelience.sql.Db;
import com.entelience.sql.DbHelper;
import com.entelience.sql.UsesDbObject;
import com.entelience.util.DateHelper;
import com.entelience.util.Config;
import java.util.Date;
import java.util.Iterator;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

/**
 * Manage the e_local_file_state table.
 * 
 * Uses statusDb.
 */
public class LocalFileStateDb extends UsesDbObject {

    private static org.apache.log4j.Logger _logger = com.entelience.util.Logs.getProbeLogger();

    private boolean trialRun = true;

    private PreparedStatement ps_find;

    private PreparedStatement ps_find2;

    private PreparedStatement ps_add;

    private PreparedStatement ps_get_md;

    private PreparedStatement ps_add_md;

    private PreparedStatement ps_update_md;

    private PreparedStatement ps_update;

    private PreparedStatement ps_update_state;

    private PreparedStatement ps_find_date;

    private PreparedStatement ps_find_date2;

    public static final Object lock = new Object();

    /**
     * Allow the API to perform without changing anything in the database.
     */
    public void setTrialRun(boolean trialRun) {
        this.trialRun = trialRun;
    }

    private int retryCount = 3;

    /**
     * Set-up prepared statements for this object.
     */
    protected void prepare() throws Exception {
        Db db = getDb();
        ps_find2 = db.prepareStatement("SELECT local_file_state_id, local_state, retry_count, message FROM e_local_file_state WHERE filename=? AND root_dir=? AND sub_dir=?");
        ps_find = db.prepareStatement("SELECT local_file_state_id FROM e_local_file_state WHERE filename=? AND root_dir=? AND sub_dir=?");
        ps_add = db.prepareStatement("INSERT INTO e_local_file_state (filename, orig_filename, root_dir, sub_dir) VALUES (?, ?, ?, ?) RETURNING local_file_state_id");
        ps_update = db.prepareStatement("UPDATE e_local_file_state SET local_state=?, retry_count=?, message=?, probe=? WHERE local_file_state_id=?");
        ps_update_state = db.prepareStatement("UPDATE e_local_file_state SET local_state=? WHERE local_file_state_id=?");
        ps_get_md = db.prepareStatement("SELECT name, text_value, int_value, double_value, date_value FROM e_metadata WHERE remote_file_state_id = ?");
        ps_update_md = db.prepareStatement("UPDATE e_metadata SET text_value = ?, int_value = ?, double_value = ?, date_value = ? WHERE local_file_state_id = ? AND name = ?");
        ps_add_md = db.prepareStatement("INSERT INTO e_metadata (text_value, int_value, double_value, date_value, local_file_state_id, name) VALUES (?, ?, ?, ?, ?, ?)");
        ps_find_date = db.prepareStatement("SELECT max(date_value) FROM e_local_file_state fs INNER JOIN e_metadata md ON md.local_file_state_id = fs.local_file_state_id " + "WHERE md.date_value IS NOT NULL AND md.name = 'date' AND root_dir=? AND orig_filename=? AND (probe=? OR probe IS NULL) AND fs.local_file_state_id <> ?");
        ps_find_date2 = db.prepareStatement("SELECT max(date_value) FROM e_local_file_state fs INNER JOIN e_metadata md ON md.local_file_state_id = fs.local_file_state_id " + " WHERE md.date_value IS NOT NULL AND md.name = 'date' AND root_dir=? AND orig_filename=? AND (probe=? OR probe IS NULL)");
        retryCount = Config.getProperty(db, "com.entelience.esis.probe.retryAttemptsBeforeFailure", 5);
    }

    /**
     * Check if a lfs object exists...  eg prior to fetching a file from
     * a mirror we see if we've not already 'done' it.  This permits us
     * to only download HTTP files when the last modified changes...
     *
     * Check constraints before relying on them.  Its saner.
     *
     * @return true if we already know about this filename, root_dir in e_local_file_state_db
     */
    public boolean checkExists(String filename, String dir, String subDir) throws Exception {
        synchronized (lock) {
            Db db = getDb();
            try {
                db.enter();
                _logger.info("Looking up " + filename + ", " + dir);
                ps_find.setString(1, filename);
                ps_find.setString(2, dir);
                ps_find.setString(3, subDir == null ? "." : subDir);
                ResultSet rs_find = db.executeQuery(ps_find);
                return rs_find.next();
            } finally {
                db.exit();
            }
        }
    }

    /**
     * Look-up a LocalFileState object in the database given file + directory names. 
     * Should none exist create a record in an initial state.
     *
     * This method, because it is only called once for each file in the FileProbeContainer per run, handles any
     * state transitions that must happen between run n and run n+1.
     *
     * @param filename On disk filename
     * @param originalFilename Filename before we added meta-data tags
     * @param dir Where the file is (or was)
     * @param inherit Optional (may be null) filestate from which to inherit meta-data etc -- used when
     *        we make a localfilestate from a remotefilestate or when we expand an archive to get at its files.
     *
     * @return LocalFileState object, with its state updated for run n+1 if necessary, for this file.
     */
    public LocalFileState findOrAdd(String filename, String originalFilename, String dir, String subDir, FileState inherit) throws Exception {
        synchronized (lock) {
            Db db = getDb();
            try {
                db.begin();
                _logger.debug("Looking up LocalFileState for (" + filename + ") (" + originalFilename + ") (" + dir + ")");
                ps_find2.setString(1, filename);
                ps_find2.setString(2, dir);
                ps_find2.setString(3, subDir == null ? "." : subDir);
                ResultSet rs_find = db.executeQuery(ps_find2);
                if (rs_find.next()) {
                    LocalFileState lfs = new LocalFileState(filename, originalFilename, dir, subDir);
                    lfs.local_file_state_id = rs_find.getInt(1);
                    lfs.state = FileStates.get(rs_find.getInt(2));
                    lfs.retry_count = rs_find.getInt(3);
                    lfs.message = rs_find.getString(4);
                    ps_get_md.setInt(1, lfs.local_file_state_id);
                    ResultSet rsMd = db.executeQuery(ps_get_md);
                    if (rsMd.next()) {
                        do {
                            String name = rsMd.getString(1);
                            String valueT = rsMd.getString(2);
                            Long valueL = rsMd.getLong(3);
                            Double valueDbl = rsMd.getDouble(4);
                            Date valueD = DateHelper.toDateOrNull(rsMd.getTimestamp(5));
                            if (valueT != null) {
                                lfs.addMetadata(MetadataFactory.getMetadata(db, name, valueT));
                            } else if (valueL != null) {
                                lfs.addMetadata(MetadataFactory.getMetadata(name, valueL));
                            } else if (valueDbl != null) {
                                lfs.addMetadata(MetadataFactory.getMetadata(name, valueDbl));
                            } else if (valueD != null) {
                                lfs.addMetadata(MetadataFactory.getMetadata(name, valueD));
                            }
                        } while (rsMd.next());
                    }
                    lfs.transferMetadata(inherit);
                    _logger.debug("File (" + filename + ") has a local state (" + lfs.state + ")");
                    switch(lfs.state) {
                        case SUCCESS:
                        case SUPERCEDED:
                        case DUPLICATE:
                            lfs.to_archive = true;
                            lfs.setSkip(true, lfs.state.toString());
                            break;
                        case FAILURE:
                            if (retryCount > 0 && lfs.retry_count < retryCount) {
                                lfs.state = FileStates.READY;
                                lfs.setSkip(false, lfs.state.toString());
                            } else {
                                lfs.to_errors_archive = true;
                                lfs.setSkip(true, lfs.state.toString());
                            }
                            break;
                        case ERROR_RETRY:
                            if (retryCount > 0 && lfs.retry_count > retryCount) {
                                lfs.state = FileStates.FAILURE;
                                lfs.to_errors_archive = true;
                                lfs.setSkip(true, lfs.state.toString());
                            } else {
                                lfs.state = FileStates.READY;
                                lfs.setSkip(false, lfs.state.toString());
                            }
                            break;
                        case READY:
                        case RUNNING:
                        case DOWNLOADING:
                        case ABORTED:
                        case ABORTED_INCONSISTENT:
                        case IGNORED:
                            lfs.state = FileStates.READY;
                            lfs.setSkip(false, lfs.state.toString());
                            break;
                        case SUCCESS_ARCHIVED:
                        case SUPERCEDED_ARCHIVED:
                        case DUPLICATE_ARCHIVED:
                        case FAILURE_ARCHIVED:
                        case IGNORED_ARCHIVED:
                        default:
                            lfs.setSkip(true, lfs.state.toString());
                            break;
                    }
                    if (!trialRun) {
                        updateMetadataInTx(lfs);
                    }
                    return lfs;
                } else {
                    if (trialRun) {
                        LocalFileState lfs = new LocalFileState(filename, originalFilename, dir, subDir);
                        lfs.state = FileStates.READY;
                        lfs.transferMetadata(inherit);
                        return lfs;
                    }
                    ps_add.setString(1, filename);
                    ps_add.setString(2, originalFilename);
                    ps_add.setString(3, dir);
                    ps_add.setString(4, subDir == null ? "." : subDir);
                    int id = DbHelper.getIntKey(ps_add);
                    LocalFileState lfs = new LocalFileState(filename, originalFilename, dir, subDir);
                    lfs.local_file_state_id = id;
                    lfs.state = FileStates.READY;
                    lfs.transferMetadata(inherit);
                    _logger.debug("Returning a new LocalFileState " + lfs);
                    return lfs;
                }
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    /**
     * Mark the file as running, we're just about to call process() on the probe.
     *
     * @param in file we're about to run a FileProbe on
     * @return what was passed in with its state updated.
     */
    public LocalFileState storeBeforeProcess(LocalFileState in) throws Exception {
        if (trialRun) return in;
        synchronized (lock) {
            Db db = getDb();
            try {
                db.begin();
                if (in == null) return null;
                if (in.local_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.filename);
                in.state = FileStates.RUNNING;
                updateMetadataInTx(in);
                return in;
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    /**
     * Store the outcome of the probe run.
     * Interpret success by marking the file as SUCCESS or ( ERROR | FAILURE )
     *
     * @param in file we ran the probe on
     * @param skipped true if we skipped this file for various reasons
     * @param ran true if we actually ran a probe on this file, false otherwise (maybe we ran out of time or something)
     * @param successOrFailure true if we succeeded, false if the probe failed or had an error
     * @param error true if probe had an error (implies that successOrFailure was false)
     * @param ex Exception we may have caught whilst running the probe, may be null
     * @param _message An error message which didn't come from an exception, may be null
     * @return input file state with its state updated.
     */
    public LocalFileState storeOutcome(LocalFileState in, boolean skipped, boolean ran, boolean successOrFailure, boolean error, Exception ex, String _message) throws Exception {
        if (trialRun) return in;
        String message = _message;
        synchronized (lock) {
            Db db = getDb();
            try {
                db.begin();
                if (in == null) return null;
                if (in.local_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.filename);
                if (skipped) {
                    if (!ran) {
                        in.state = FileStates.IGNORED;
                        in.retry_count = 0;
                    }
                }
                if (ran) {
                    if (successOrFailure) {
                        in.state = FileStates.SUCCESS;
                    } else {
                        if (error) {
                            in.state = FileStates.ERROR_RETRY;
                            in.retry_count++;
                        } else {
                            in.state = FileStates.FAILURE;
                        }
                    }
                }
                if (message == null && ex != null) message = ex.getMessage();
                int n = 1;
                ps_update.setInt(n++, in.state.getId());
                ps_update.setInt(n++, in.retry_count);
                ps_update.setString(n++, message);
                ps_update.setObject(n, null);
                if (ran && in.pf != null) {
                    ps_update.setString(n, in.pf.getClassName());
                }
                ++n;
                ps_update.setInt(n++, in.local_file_state_id);
                db.executeUpdate(ps_update);
                updateMetadataInTx(in);
                return in;
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    /**
     * Now flag the file as being archived.  It has an archive state
     * which indicates what had happened to the file before it was 
     * archived -- SUCCESS, ...  See FileStates for more details.
     *
     * @param in file state
     * @return input file state updated to indicate archive reason
     */
    public LocalFileState storeArchived(LocalFileState in) throws Exception {
        if (trialRun) return in;
        if (in == null) return null;
        synchronized (lock) {
            switch(in.state) {
                case RUNNING:
                case READY:
                case SUCCESS:
                    in.state = FileStates.SUCCESS_ARCHIVED;
                    break;
                case IGNORED:
                    in.state = FileStates.IGNORED_ARCHIVED;
                    break;
                case SUPERCEDED:
                    in.state = FileStates.SUPERCEDED_ARCHIVED;
                    break;
                case DUPLICATE:
                    in.state = FileStates.DUPLICATE_ARCHIVED;
                    break;
                case FAILURE:
                    in.state = FileStates.FAILURE_ARCHIVED;
                    break;
                default:
                    return in;
            }
            in.to_archive = false;
            in.to_errors_archive = false;
            Db db = getDb();
            try {
                db.begin();
                if (in.local_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.filename);
                ps_update_state.setInt(1, in.state.getId());
                ps_update_state.setInt(2, in.local_file_state_id);
                int upd = db.executeUpdate(ps_update_state);
                if (upd != 1) {
                    _logger.warn("Error updating file state for " + in.filename);
                }
                updateMetadataInTx(in);
                return in;
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    /**
     * As a compliment to RemoteFileStateDb#findUpToDate we can also
     * check the local file state db for the same file.
     * if it would exist post downloading then we report on that.
     *
     * Relates to HttpFetchMirror and HttpDateCrawlMirror.
     *
     * @param fromRemoteDb most recent date from e_remote_file_state for this file
     * @param in remote object we want to check for on local state database
     * @param dir current working directory
     * @return most recent date for the given file (which may have many different versions in the database)
     */
    public Date findUpToDate(Date fromRemoteDb, RemoteFileState in, String dir) throws Exception {
        Date fromLocalDb = null;
        if (in.pf == null) return fromRemoteDb;
        Db db = getDb();
        try {
            db.enter();
            ps_find_date2.setString(1, dir);
            ps_find_date2.setString(2, in.orig_filename);
            ps_find_date2.setString(3, in.pf.getClassName());
            ResultSet rs = db.executeQuery(ps_find_date2);
            if (rs.next()) fromLocalDb = DateHelper.toDateOrNull(rs.getTimestamp(1));
        } finally {
            db.exit();
        }
        if (fromLocalDb == null && fromRemoteDb != null) return fromRemoteDb;
        if (fromLocalDb != null && fromRemoteDb == null) return fromLocalDb;
        if (fromLocalDb == null && fromRemoteDb == null) return null;
        if (fromLocalDb.before(fromRemoteDb)) return fromRemoteDb; else return fromLocalDb;
    }

    /**
     * Get the date of the most up-to-date file given the same
     * dir and original_filename [ we no longer know the url ]
     *
     * Relates to HttpFetchMirror and HttpDateCrawlMirror.
     *
     * @param in remote object we want to check for on local state database
     * @return most recent date for the given file (which may have many different versions in the database)
     */
    public Date findUpToDate(LocalFileState in) throws Exception {
        if (in.pf == null) return null;
        Db db = getDb();
        try {
            db.enter();
            ps_find_date.setString(1, in.dir);
            ps_find_date.setString(2, in.orig_filename);
            ps_find_date.setString(3, in.pf.getClassName());
            ps_find_date.setInt(4, in.local_file_state_id);
            ResultSet rs = db.executeQuery(ps_find_date);
            if (rs.next()) {
                return DateHelper.toDateOrNull(rs.getTimestamp(1));
            } else return null;
        } finally {
            db.exit();
        }
    }

    /**
     * Mark a file as ignored.
     *
     * @param in file state
     * @return input file state updated
     */
    public LocalFileState storeIgnored(LocalFileState in) throws Exception {
        if (trialRun) return in;
        if (in.local_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.filename);
        if (in.state.getId() > 0 && in.state != FileStates.IGNORED) {
            throw new IllegalStateException("Cannot mark this file as ignored " + in.state);
        }
        in.state = FileStates.IGNORED;
        synchronized (lock) {
            Db db = getDb();
            try {
                db.begin();
                ps_update_state.setInt(1, in.state.getId());
                ps_update_state.setInt(2, in.local_file_state_id);
                int upd = db.executeUpdate(ps_update_state);
                if (upd != 1) {
                    _logger.warn("Error updating file state for " + in.filename);
                }
                updateMetadataInTx(in);
                return in;
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    /**
     * Mark a file as superceded.
     *
     * @param in file state
     * @return input file state updated
     */
    public LocalFileState storeSuperceded(LocalFileState in) throws Exception {
        if (trialRun) return in;
        if (in.local_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.filename);
        synchronized (lock) {
            switch(in.state) {
                case SUCCESS_ARCHIVED:
                case IGNORED_ARCHIVED:
                case SUPERCEDED_ARCHIVED:
                case DUPLICATE_ARCHIVED:
                case FAILURE_ARCHIVED:
                    throw new IllegalStateException("Cannot mark this file as superceded (" + in.state.toString() + ")");
                default:
                    in.state = FileStates.SUPERCEDED;
                    break;
            }
            Db db = getDb();
            try {
                db.begin();
                ps_update_state.setInt(1, in.state.getId());
                int upd = db.executeUpdate(ps_update_state);
                if (upd != 1) {
                    _logger.warn("Error updating file state for " + in.filename);
                }
                updateMetadataInTx(in);
                return in;
            } catch (Exception e) {
                db.rollback();
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    private LocalFileState updateMetadataInTx(LocalFileState in) throws Exception {
        synchronized (lock) {
            Db db = getDb();
            try {
                db.enter();
                for (Iterator<Metadata> it = in.getMetadata().iterator(); it.hasNext(); ) {
                    Metadata md = it.next();
                    String name = DbHelper.nullify(md.getName());
                    String valueS = null;
                    Long valueL = null;
                    Double valueDbl = null;
                    Date valueD = null;
                    if (md instanceof TextMetadata) {
                        valueS = ((TextMetadata) md).getValue();
                    } else if (md instanceof LongMetadata) {
                        valueL = ((LongMetadata) md).getValue();
                    } else if (md instanceof DoubleMetadata) {
                        valueDbl = ((DoubleMetadata) md).getValue();
                    } else if (md instanceof DateMetadata) {
                        valueD = ((DateMetadata) md).getValue();
                    } else {
                        _logger.error("Unknown metadata type (" + name + ";" + md.getClass().getName() + ")");
                        continue;
                    }
                    if (name == null) _logger.error("Null metadata name for (" + md.getClass().getName() + ")");
                    if (valueS == null && valueL == null && valueDbl == null && valueD == null) throw new IllegalStateException("There is no value for metadata (" + name + ")");
                    ps_update_md.setString(1, valueS);
                    ps_update_md.setObject(2, valueL);
                    ps_update_md.setObject(3, valueDbl);
                    ps_update_md.setTimestamp(4, DateHelper.sqlOrNull(valueD));
                    ps_update_md.setInt(5, in.local_file_state_id);
                    ps_update_md.setString(6, name);
                    int res = db.executeUpdate(ps_update_md);
                    if (res == 0) {
                        ps_add_md.setString(1, valueS);
                        ps_add_md.setObject(2, valueL);
                        ps_add_md.setObject(3, valueDbl);
                        ps_add_md.setTimestamp(4, DateHelper.sqlOrNull(valueD));
                        ps_add_md.setInt(5, in.local_file_state_id);
                        ps_add_md.setString(6, name);
                        db.executeUpdate(ps_add_md);
                    }
                    _logger.debug("metadata updated for (" + in.toString() + ")");
                }
                return in;
            } finally {
                db.exit();
            }
        }
    }

    /**
     * Update the meta-data stored for this file state...
     *
     * Note: This is called too often.
     *
     * @param in Input file state.
     * @return what was input, unchanged
     */
    public LocalFileState updateMetadata(LocalFileState in) throws Exception {
        if (trialRun) return in;
        if (in.local_file_state_id == 0) throw new IllegalArgumentException("Should have local file state id for (" + in.filename + ")");
        synchronized (lock) {
            Db db = getDb();
            try {
                db.begin();
                return updateMetadataInTx(in);
            } catch (Exception e) {
                db.rollback();
                _logger.error("Exception encountered whilst updating metadata for local file state (" + in.filename + ") (" + e + ")");
                throw e;
            } finally {
                db.commitUnless();
            }
        }
    }

    /**
     * Return probe run-state for a given day and given a source lfs object.  Typically
     * we look at day n-1 or n+1 given input n.
     *
     * The sql we use keeps all other meta-data dimensions the same.
     *
     * This is used for strict dateorder, reversedateorder checks in the FileProbeContainer.
     *
     * @param day Check this day.  (Corresponds to date n-1 or date n+1)
     * @param in Using this local file.
     * @param dir In this working directory.
     *
     * @return LocalFileState object corresponding to the precondition for date n.
     */
    public LocalFileState getStateForDay(Date day, LocalFileState in, String dir) throws Exception {
        if (in.pf == null) throw new IllegalArgumentException(in.filename + " must have a probe to run.");
        Db db = getDb();
        try {
            db.enter();
            PreparedStatement ps = db.prepareStatement("SELECT fs.local_file_state_id, local_state, retry_count, message, filename, orig_filename, sub_dir FROM e_local_file_state fs " + " INNER JOIN e_metadata md ON md.local_file_state_id = fs.local_file_state_id WHERE probe=? AND md.date_value = ? AND root_dir = ?");
            int n = 0;
            ps.setString(++n, in.pf.getClassName());
            ps.setTimestamp(++n, DateHelper.sql(day));
            ps.setString(++n, dir);
            ResultSet rs = db.executeQuery(ps);
            if (rs.next()) {
                LocalFileState lfs = new LocalFileState(rs.getString(5), rs.getString(6), dir, rs.getString(7));
                lfs.local_file_state_id = rs.getInt(1);
                lfs.state = FileStates.get(rs.getInt(2));
                lfs.retry_count = rs.getInt(3);
                lfs.message = rs.getString(4);
                ps_get_md.setInt(1, lfs.local_file_state_id);
                ResultSet rsMd = db.executeQuery(ps_get_md);
                if (rsMd.next()) {
                    do {
                        String name = rsMd.getString(1);
                        String valueT = rsMd.getString(2);
                        Long valueL = rsMd.getLong(3);
                        Double valueDbl = rsMd.getDouble(4);
                        Date valueD = DateHelper.toDateOrNull(rsMd.getTimestamp(5));
                        if (valueT != null) {
                            lfs.addMetadata(MetadataFactory.getMetadata(db, name, valueT));
                        } else if (valueL != null) {
                            lfs.addMetadata(MetadataFactory.getMetadata(name, valueL));
                        } else if (valueDbl != null) {
                            lfs.addMetadata(MetadataFactory.getMetadata(name, valueDbl));
                        } else if (valueD != null) {
                            lfs.addMetadata(MetadataFactory.getMetadata(name, valueD));
                        }
                    } while (rsMd.next());
                }
                return lfs;
            } else return null;
        } finally {
            db.exit();
        }
    }

    /**
     * check to see whether a probe has run "before" a given date; keeping all other meta-data dimensions the same.
     *
     * This is used for strict dateorder, reversedateorder checks in the FileProbeContainer.
     *
     * @param day Check this day.  (Corresponds to date n-1 or date n+1)
     * @param in Using this local file.
     * @param dir In this working directory.
     *
     * @return true if something is found on or before given day
     */
    public boolean existsBefore(Date day, LocalFileState in, String dir) throws Exception {
        if (in.pf == null) throw new IllegalArgumentException(in.filename + " must have a probe to run.");
        Db db = getDb();
        try {
            db.enter();
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT fs.local_file_state_id FROM e_local_file_state fs INNER JOIN e_metadata md ON md.local_file_state_id = fs.local_file_state_id " + " WHERE probe=? AND local_state NOT IN (0, 4) AND md.date_value <= ? AND root_dir = ?");
            PreparedStatement ps = db.prepareStatement(sql.toString());
            int n = 0;
            ps.setString(++n, in.pf.getClassName());
            ps.setTimestamp(++n, DateHelper.sql(day));
            ps.setString(++n, dir);
            ResultSet rs = db.executeQuery(ps);
            return rs.next();
        } finally {
            db.exit();
        }
    }

    /**
     * check to see whether a probe has run "after" a given date; keeping all other meta-data dimensions the same.
     *
     * This is used for strict dateorder, reversedateorder checks in the FileProbeContainer.
     *
     * @param day Check this day.  (Corresponds to date n-1 or date n+1)
     * @param in Using this local file.
     * @param dir In this working directory.
     *
     * @return true if something is found on or before given day
     */
    public boolean existsAfter(Date day, LocalFileState in, String dir) throws Exception {
        if (in.pf == null) throw new IllegalArgumentException(in.filename + " must have a probe to run.");
        Db db = getDb();
        try {
            db.enter();
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT fs.local_file_state_id FROM e_local_file_state fs INNER JOIN e_metadata md ON md.local_file_state_id = fs.local_file_state_id " + " WHERE probe=? AND local_state NOT IN (0, 4) AND md.date_value >= ? AND root_dir = ?");
            PreparedStatement ps = db.prepareStatement(sql.toString());
            int n = 0;
            ps.setString(++n, in.pf.getClassName());
            ps.setTimestamp(++n, DateHelper.sql(day));
            ps.setString(++n, dir);
            ResultSet rs = db.executeQuery(ps);
            return rs.next();
        } finally {
            db.exit();
        }
    }
}

package com.entelience.probe;

import java.util.Date;
import java.util.Iterator;
import com.entelience.sql.Db;
import com.entelience.sql.UsesDbObject;
import com.entelience.sql.DbHelper;
import com.entelience.util.DateHelper;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

/**
 * Manage the e_remote_file_state table.
 *
 * Most methods here are similar to those in LocalFileStateDb
 */
public class RemoteFileStateDb extends UsesDbObject {

    private static org.apache.log4j.Logger _logger = com.entelience.util.Logs.getProbeLogger();

    private boolean trialRun = true;

    private PreparedStatement ps_find;

    private PreparedStatement ps_find2;

    private PreparedStatement ps_add;

    private PreparedStatement ps_update;

    private PreparedStatement ps_update_state;

    private PreparedStatement ps_update_md;

    private PreparedStatement ps_add_md;

    private PreparedStatement ps_find_date;

    private PreparedStatement ps_get_md;

    /**
     * Allow the API to perform without changing anything in the database.
     */
    public void setTrialRun(boolean trialRun) {
        this.trialRun = trialRun;
    }

    /**
     * Set-up prepared statements for this object.
     */
    protected void prepare() throws Exception {
        Db db = getDb();
        ps_find = db.prepareStatement("SELECT remote_file_state_id FROM e_remote_file_state WHERE filename=? AND url=?");
        ps_find2 = db.prepareStatement("SELECT remote_file_state_id, remote_state, retry_count, last_modified, message FROM e_remote_file_state WHERE filename=? AND url=?");
        ps_get_md = db.prepareStatement("SELECT name, text_value, int_value, double_value, date_value FROM e_metadata WHERE remote_file_state_id = ?");
        ps_update = db.prepareStatement("UPDATE e_remote_file_state SET remote_state=?, retry_count=?, last_modified=?, message=?, local_id = ? WHERE remote_file_state_id=?");
        ps_update_state = db.prepareStatement("UPDATE e_remote_file_state SET remote_state=? WHERE remote_file_state_id=?");
        ps_update_md = db.prepareStatement("UPDATE e_metadata SET text_value = ?, int_value = ?, double_value = ?, date_value = ? WHERE remote_file_state_id = ? AND name = ?");
        ps_add_md = db.prepareStatement("INSERT INTO e_metadata (text_value, int_value, double_value, date_value, remote_file_state_id, name) VALUES (?, ?, ?, ?, ?, ?)");
        ps_find_date = db.prepareStatement("SELECT max(date_value) FROM e_remote_file_state fs INNER JOIN e_metadata md ON md.local_file_state_id = fs.local_id WHERE md.date_value IS NOT NULL AND md.name = 'date' AND url=? AND fs.remote_file_state_id <> ?");
    }

    /**
     * Check if a rfs object exists...  eg prior to fetching a file from
     * a mirror we see if we've not already 'done' it.
     *
     * Check constraints before relying on them.  Its saner.
     *
     * @return true if we already know about this filename, dir in e_local_file_state_db
     */
    public boolean checkExists(String filename, String url) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            _logger.info("Looking up " + filename + ", " + url);
            ps_find.setString(1, filename);
            ps_find.setString(2, url);
            ResultSet rs_find = db.executeQuery(ps_find);
            return rs_find.next();
        } finally {
            db.exit();
        }
    }

    /**
     * Look-up a RemoteFileState object in the database given file + url names. 
     * Should none exist create a record in an initial state.
     *
     * This method, because it is only called once for each file on each Mirror in the FileProbeContainer per run, handles any
     * state transitions that must happen between run n and run n+1.
     *
     * @param filename On disk filename
     * @param originalFilename Filename before we added meta-data tags
     * @param url remote location
     *
     * @return RemoteFileState object, with its state updated for run n+1 if necessary, for this file.
     */
    public RemoteFileState findOrAdd(String filename, String originalFilename, String url, String subDir) throws Exception {
        Db db = getDb();
        try {
            db.begin();
            ps_find2.setString(1, filename);
            ps_find2.setString(2, url);
            ResultSet rs_find = db.executeQuery(ps_find2);
            if (rs_find.next()) {
                RemoteFileState rfs = new RemoteFileState();
                rfs.filename = filename;
                rfs.orig_filename = originalFilename;
                rfs.url = url;
                rfs.subDir = subDir;
                rfs.remote_file_state_id = rs_find.getInt(1);
                rfs.state = FileStates.get(rs_find.getInt(2));
                rfs.retry_count = rs_find.getInt(3);
                rfs.last_modified = rs_find.getLong(4);
                rfs.message = rs_find.getString(5);
                _logger.debug("Found remote file (" + rfs.toString() + ")");
                ps_get_md.setInt(1, rfs.remote_file_state_id);
                ResultSet rsMd = db.executeQuery(ps_get_md);
                if (rsMd.next()) {
                    do {
                        String name = rsMd.getString(1);
                        String valueT = rsMd.getString(2);
                        Long valueL = rsMd.getLong(3);
                        Double valueDbl = rsMd.getDouble(4);
                        Date valueD = DateHelper.toDateOrNull(rsMd.getTimestamp(5));
                        if (valueT != null) {
                            rfs.addMetadata(MetadataFactory.getMetadata(db, name, valueT));
                        } else if (valueL != null) {
                            rfs.addMetadata(MetadataFactory.getMetadata(name, valueL));
                        } else if (valueDbl != null) {
                            rfs.addMetadata(MetadataFactory.getMetadata(name, valueDbl));
                        } else if (valueD != null) {
                            rfs.addMetadata(MetadataFactory.getMetadata(name, valueD));
                        }
                    } while (rsMd.next());
                }
                switch(rfs.state) {
                    case FAILURE:
                        rfs.setSkip(true, rfs.state.toString());
                        break;
                    case ERROR_RETRY:
                        rfs.setSkip(true, rfs.state.toString());
                        break;
                    case READY:
                    case RUNNING:
                    case DOWNLOADING:
                    case ABORTED:
                    case ABORTED_INCONSISTENT:
                    case IGNORED:
                        rfs.state = FileStates.READY;
                        rfs.setSkip(false, rfs.state.toString());
                        break;
                    case SUCCESS:
                    case SUPERCEDED:
                    case DUPLICATE:
                    case SUCCESS_ARCHIVED:
                    case SUPERCEDED_ARCHIVED:
                    case DUPLICATE_ARCHIVED:
                    case FAILURE_ARCHIVED:
                    case IGNORED_ARCHIVED:
                    default:
                        rfs.setSkip(true, rfs.state.toString());
                        break;
                }
                if (!trialRun) updateMetadataInTx(rfs);
                return rfs;
            } else {
                Db wDb = (Db) getDb().clone();
                try {
                    wDb.begin();
                    if (trialRun) {
                        RemoteFileState rfs = new RemoteFileState();
                        rfs.filename = filename;
                        rfs.orig_filename = originalFilename;
                        rfs.url = url;
                        rfs.state = FileStates.READY;
                        rfs.subDir = subDir;
                        return rfs;
                    }
                    PreparedStatement ps_add = db.prepareStatement("INSERT INTO e_remote_file_state (filename, orig_filename, url) VALUES (?, ?, ?) RETURNING remote_file_state_id");
                    ps_add.setString(1, filename);
                    ps_add.setString(2, originalFilename);
                    ps_add.setString(3, url);
                    int id = DbHelper.getIntKey(ps_add);
                    RemoteFileState rfs = new RemoteFileState();
                    rfs.filename = filename;
                    rfs.orig_filename = originalFilename;
                    rfs.url = url;
                    rfs.subDir = subDir;
                    rfs.remote_file_state_id = id;
                    rfs.state = FileStates.READY;
                    return rfs;
                } catch (Exception ex) {
                    wDb.rollback();
                    throw ex;
                } finally {
                    wDb.commitUnless();
                    wDb.safeClose();
                }
            }
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.commitUnless();
        }
    }

    /**
     * Mark the file as ignored (for fetch) in the database.  Mirror#list has
     * already been successful.
     *
     * @param in remote file information
     * @return what was passed in with its state updated.
     */
    public synchronized RemoteFileState storeIgnored(RemoteFileState in) throws Exception {
        _logger.trace("Storing as ignored (" + in.filename + ")");
        if (trialRun) return in;
        Db db = getDb();
        try {
            db.begin();
            in.state = FileStates.IGNORED;
            ps_update_state.setInt(1, in.state.getId());
            ps_update_state.setInt(2, in.remote_file_state_id);
            db.executeUpdate(ps_update_state);
            updateMetadataInTx(in);
            return in;
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.commitUnless();
        }
    }

    /**
     * Mark the file as ready (for fetch) in the database.  Mirror#list has
     * already been successful.
     *
     * @param in remote file information
     * @return what was passed in with its state updated.
     */
    public synchronized RemoteFileState storeReady(RemoteFileState in) throws Exception {
        _logger.trace("Storing as ready (" + in.filename + ")");
        if (trialRun) return in;
        Db db = getDb();
        try {
            db.begin();
            in.state = FileStates.DOWNLOADING;
            ps_update_state.setInt(1, in.state.getId());
            ps_update_state.setInt(2, in.remote_file_state_id);
            db.executeUpdate(ps_update_state);
            updateMetadataInTx(in);
            return in;
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.commitUnless();
        }
    }

    /**
     * Mark the remote object as fetched, or indicate what kind of failure
     * we got.
     *
     * @param in remote file information
     * @param fetched . is null when error
     * @param exOrNull exception we got, null indicates failure otherwise error
     * @return what was passed in with its state updated.
     */
    public synchronized RemoteFileState storeFetched(RemoteFileState in, MirrorReturn fetched, Exception exOrNull) throws Exception {
        _logger.trace("Storing as fetched (" + in.filename + ")");
        if (trialRun) return in;
        Db db = getDb();
        try {
            db.begin();
            boolean success = (fetched != null);
            Integer localFileStateId = null;
            if (fetched != null) {
                localFileStateId = fetched.local.local_file_state_id;
            }
            if (success) {
                in.state = FileStates.SUCCESS;
            } else {
                if (exOrNull == null) {
                    in.state = FileStates.FAILURE;
                } else {
                    in.state = FileStates.ERROR_RETRY;
                    in.retry_count++;
                }
            }
            ps_update.setInt(1, in.state.getId());
            ps_update.setInt(2, in.retry_count);
            ps_update.setLong(3, in.last_modified);
            String m = null;
            if (exOrNull != null) m = exOrNull.getMessage();
            ps_update.setString(4, m);
            ps_update.setObject(5, localFileStateId);
            ps_update.setInt(6, in.remote_file_state_id);
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

    /**
     * Get the date of the most up-to-date file given the same
     * url and original_filename.
     *
     * Relates to HttpFetchMirror and HttpDateCrawlMirror.
     *
     * @param in remote object we want to check
     * @return most recent date for the given file (which may have many different versions in the database)
     */
    public Date findUpToDate(RemoteFileState in) throws Exception {
        Db db = getDb();
        try {
            db.enter();
            ps_find_date.setString(1, in.url);
            ps_find_date.setInt(2, in.remote_file_state_id);
            ResultSet rs = db.executeQuery(ps_find_date);
            if (rs.next()) {
                return DateHelper.toDateOrNull(rs.getTimestamp(1));
            } else return null;
        } finally {
            db.exit();
        }
    }

    /**
     * Mark a file as superceded.
     *
     * @param in file state
     * @return input file state updated
     */
    public RemoteFileState storeSuperceded(RemoteFileState in) throws Exception {
        if (trialRun) return in;
        if (in.remote_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.url);
        switch(in.state) {
            case SUCCESS_ARCHIVED:
            case IGNORED_ARCHIVED:
            case SUPERCEDED_ARCHIVED:
            case DUPLICATE_ARCHIVED:
            case FAILURE_ARCHIVED:
                throw new IllegalStateException("Cannot mark this file as superceded (" + in.state.toString() + ").");
            default:
                in.state = FileStates.SUPERCEDED;
                break;
        }
        Db db = getDb();
        try {
            db.begin();
            ps_update_state.setInt(1, in.state.getId());
            ps_update_state.setInt(2, in.remote_file_state_id);
            db.executeUpdate(ps_update_state);
            updateMetadataInTx(in);
            return in;
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.commitUnless();
        }
    }

    /**
     * internal method to update metadata. 
     * unlike other methods in this class, it must be called inside a transaction
     *
     */
    private RemoteFileState updateMetadataInTx(RemoteFileState in) throws Exception {
        _logger.trace("Updating metadata in tx for (" + in.filename + ")");
        Db db = getDb();
        try {
            db.enter();
            for (Iterator<Metadata> it = in.getMetadata().iterator(); it.hasNext(); ) {
                Metadata md = it.next();
                String name = md.getName();
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
                    continue;
                }
                ps_update_md.setString(1, valueS);
                ps_update_md.setObject(2, valueL);
                ps_update_md.setObject(3, valueDbl);
                ps_update_md.setTimestamp(4, DateHelper.sqlOrNull(valueD));
                ps_update_md.setInt(5, in.remote_file_state_id);
                ps_update_md.setString(6, name);
                int res = db.executeUpdate(ps_update_md);
                if (res == 0) {
                    ps_add_md.setString(1, valueS);
                    ps_add_md.setObject(2, valueL);
                    ps_add_md.setObject(3, valueDbl);
                    ps_add_md.setTimestamp(4, DateHelper.sqlOrNull(valueD));
                    ps_add_md.setInt(5, in.remote_file_state_id);
                    ps_add_md.setString(6, name);
                    db.executeUpdate(ps_add_md);
                }
            }
            return in;
        } finally {
            db.exit();
        }
    }

    /**
     * Update the meta-data stored for this file state...
     *
     * This is called too often.
     *
     * @param in Input file state.
     * @return what was input, unchanged
     */
    public synchronized RemoteFileState updateMetadata(RemoteFileState in) throws Exception {
        if (trialRun) return in;
        if (in.remote_file_state_id == 0) throw new IllegalArgumentException("Should have database id for " + in.url);
        Db db = getDb();
        try {
            db.begin();
            return updateMetadataInTx(in);
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.commitUnless();
        }
    }
}

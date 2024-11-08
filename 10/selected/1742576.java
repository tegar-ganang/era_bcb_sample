package com.agentfactory.agentspotter.file;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Dinh Doan Van Bien
 */
public class SessionRecord extends Observable {

    private long id;

    private long startTimestamp;

    private long stopTimestamp;

    public SessionRecord() {
        this.startTimestamp = System.currentTimeMillis();
    }

    private SessionRecord(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.startTimestamp = rs.getLong("startTimestamp");
        this.stopTimestamp = rs.getLong("stopTimestamp");
    }

    public void insertInto(DbHelper db) throws SQLException {
        db.executeUpdate("INSERT INTO session (startTimestamp) VALUES (?);", this.getStartTimestamp());
        this.id = db.getLastId();
    }

    public void update(DbHelper db) throws SQLException {
        db.executeUpdate("UPDATE session\n" + "   SET startTimestamp = ?, stopTimestamp = ?\n" + "WHERE id = ?", this.getStartTimestamp(), this.getStopTimestamp(), this.getId());
    }

    public void delete(DbHelper db) throws SQLException {
        try {
            db.executeUpdate("DELETE FROM event WHERE session = ?", this.getId());
            db.executeUpdate("DELETE FROM message WHERE session = ?", this.getId());
            db.executeUpdate("DELETE FROM agent WHERE session = ?", this.getId());
            db.executeUpdate("DELETE FROM session WHERE id = ?", this.getId());
            db.commit();
            setChanged();
            notifyObservers("delete");
        } catch (SQLException ex) {
            db.rollback();
            Logger.getLogger(SessionRecord.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    public static List<SessionRecord> loadAll(DbHelper db) throws SQLException {
        List<SessionRecord> lst = new ArrayList<SessionRecord>();
        ResultSet rs = db.loadData("SELECT id, startTimestamp, stopTimestamp FROM session ORDER BY date");
        try {
            while (rs.next()) {
                lst.add(new SessionRecord(rs));
            }
        } finally {
            rs.close();
        }
        return lst;
    }

    @Override
    public String toString() {
        return getLongDescription();
    }

    public String getShortDescription() {
        if (this.getStartTimestamp() == 0) return "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        return dateFormat.format(new Date(this.getStartTimestamp()));
    }

    public String getLongDescription() {
        if (this.getStartTimestamp() == 0) return "";
        return getShortDescription() + " - " + new Duration(getDuration()).getNiceText();
    }

    public long getId() {
        return id;
    }

    public long getStopTimestamp() {
        return stopTimestamp;
    }

    public void setStopTimestamp(long stopTimestamp) {
        this.stopTimestamp = stopTimestamp;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getDuration() {
        if (getStopTimestamp() == 0) return 0;
        return getStopTimestamp() - getStartTimestamp();
    }
}

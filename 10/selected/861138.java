package hambo.pim;

import java.sql.*;
import java.math.BigDecimal;
import java.util.Vector;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import hambo.svc.*;
import hambo.svc.database.*;

/**
 * Business object for events
 */
public class EventBO extends BaseBO {

    public static final int SUBJECT = 0;

    public static final int TEXT = 1;

    public static final int STARTDAY = 2;

    public static final int STARTMONTH = 3;

    public static final int STARTYEAR = 4;

    public static final int STARTHOUR = 5;

    public static final int STARTMINUTE = 6;

    public static final int ENDHOUR = 7;

    public static final int ENDMINUTE = 8;

    public static final int PLACE = 9;

    public static final int CONTACTPERSON = 10;

    public static final int ALLDAY = 11;

    public static final int RESET = 0;

    public static final int INSERT = 1;

    public static final int UPDATE = 2;

    public static final int DELETE = 3;

    public static final int EVENT_LIMIT = 5000;

    private EventDO DO = null;

    public EventBO() {
        super();
        DO = new EventDO();
    }

    public EventBO(BigDecimal oid, BigDecimal owner) throws SQLException {
        this(oid, owner, true);
    }

    public EventBO(BigDecimal oid, BigDecimal owner, boolean html) throws SQLException {
        super();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select cal_Event.owner,cal_Event.subject,cal_Event.text,cal_Event.place," + "cal_Event.contactperson,cal_Event.startdate,cal_Event.enddate,cal_Event.starttime," + "cal_Event.endtime,cal_Event.allday,cal_Event.syncstatus,cal_Event.dirtybits," + "cal_Event.OId,cal_Event_Remind.rtime from cal_Event,cal_Event_Remind  " + "where cal_Event.OId=? and cal_Event.owner=? and " + DBUtil.getQueryJoin(con, "cal_Event.OId", "cal_Event_Remind.event"));
            ps.setBigDecimal(1, oid);
            ps.setBigDecimal(2, owner);
            rs = con.executeQuery(ps, null);
            if (rs.next() == false) {
                throw new DataObjectNotFoundException("EventBO: [oid=" + oid + "]");
            }
            DO = new EventDO(rs, html);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public EventDO getDO() {
        return DO;
    }

    public void setDO(EventDO eDO) {
        DO = eDO;
    }

    public void insertEvent(EventDO eDO) throws SQLException {
        DO = eDO;
        insertEvent();
    }

    public void insertEvent() throws SQLException, LimitExceededException {
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select count(*) from cal_Event where owner=?");
            ps.setBigDecimal(1, DO.getOwner());
            rs = con.executeQuery(ps, null);
            rs.next();
            if (rs.getInt(1) >= EVENT_LIMIT) {
                fireUserEvent("EventBO insertEvent: Event limit exceeded");
                throw new LimitExceededException("fail");
            }
            con.reset();
            ps = con.prepareStatement("insert into cal_Event (owner,subject,text,place," + "contactperson,startdate,enddate,starttime,endtime," + "allday,syncstatus,dirtybits) values(?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setBigDecimal(1, DO.getOwner());
            ps.setString(2, DO.getSubject());
            ps.setString(3, DO.getText());
            ps.setString(4, DO.getPlace());
            ps.setString(5, DO.getContactperson());
            ps.setDate(6, DO.getStartdate());
            if (DO.getEnddate() != null) ps.setDate(7, DO.getEnddate()); else ps.setNull(7, Types.DATE);
            ps.setTime(8, DO.getStarttime());
            if (DO.getEndtime() != null) ps.setTime(9, DO.getEndtime()); else ps.setNull(9, Types.TIME);
            ps.setBoolean(10, DO.getAllday());
            ps.setInt(11, INSERT);
            ps.setInt(12, DO.getDirtybits());
            con.executeUpdate(ps, null);
            ps = con.prepareStatement(DBUtil.getQueryCurrentOID(con, "cal_Event", "newoid"));
            rs = con.executeQuery(ps, null);
            if (rs.next()) DO.setOId(rs.getBigDecimal("newoid"));
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public void updateEvent(EventDO eDO) throws SQLException {
        DO = eDO;
        updateEvent();
    }

    public void updateEvent() throws SQLException {
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("update cal_Event set owner=?,subject=?,text=?,place=?,contactperson=?," + "startdate=?,enddate=?,starttime=?,endtime=?,allday=?,syncstatus=?,dirtybits=? where OId=?");
            ps.setBigDecimal(1, DO.getOwner());
            ps.setString(2, DO.getSubject());
            ps.setString(3, DO.getText());
            ps.setString(4, DO.getPlace());
            ps.setString(5, DO.getContactperson());
            ps.setDate(6, DO.getStartdate());
            if (DO.getEnddate() != null) ps.setDate(7, DO.getEnddate()); else ps.setNull(7, Types.DATE);
            ps.setTime(8, DO.getStarttime());
            if (DO.getEndtime() != null) ps.setTime(9, DO.getEndtime()); else ps.setNull(9, Types.TIME);
            ps.setBoolean(10, DO.getAllday());
            if (DO.getSyncstatus() == RESET) ps.setInt(11, UPDATE); else ps.setInt(11, DO.getSyncstatus());
            ps.setInt(12, DO.getDirtybits());
            ps.setBigDecimal(13, DO.getOId());
            con.executeUpdate(ps, null);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public Vector getEventsBetween(BigDecimal owner, String orderby, Date begin, Date end) throws SQLException {
        return getEventsBetween(owner, orderby, begin, end, true);
    }

    public Vector getEventsBetween(BigDecimal owner, String orderby, Date begin, Date end, boolean html) throws SQLException {
        Vector events = new Vector();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select cal_Event.owner,cal_Event.subject,cal_Event.text,cal_Event.place," + "cal_Event.contactperson,cal_Event.startdate,cal_Event.enddate,cal_Event.starttime," + "cal_Event.endtime,cal_Event.allday,cal_Event.syncstatus,cal_Event.dirtybits," + "cal_Event.OId,cal_Event_Remind.rtime from cal_Event,cal_Event_Remind " + "where cal_Event.owner=? and " + DBUtil.getQueryJoin(con, "cal_Event.OId", "cal_Event_Remind.event ") + "and cal_Event.startdate>=? and cal_Event.startdate<? and cal_Event.syncstatus<? " + "order by " + orderby);
            ps.setBigDecimal(1, owner);
            ps.setDate(2, begin);
            ps.setDate(3, end);
            ps.setInt(4, DELETE);
            rs = con.executeQuery(ps, null);
            while (rs.next()) {
                events.addElement(DO = new EventDO(rs, html));
                DO.setRemindtime(rs.getBigDecimal("rtime"));
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return events;
    }

    public Vector getEventDatesBetween(BigDecimal owner, String orderby, Date begin, Date end) throws SQLException {
        Vector events = new Vector();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select startdate from cal_Event where owner=? and startdate>=? " + "and startdate<? and syncstatus<? group by startdate order by " + orderby);
            ps.setBigDecimal(1, owner);
            ps.setDate(2, begin);
            ps.setDate(3, end);
            ps.setInt(4, DELETE);
            rs = con.executeQuery(ps, null);
            while (rs.next()) {
                events.addElement(rs.getDate("startdate"));
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return events;
    }

    public Vector getMonthlyEventDates(BigDecimal owner, String orderby, Calendar cal) throws SQLException {
        cal.set(Calendar.DATE, 1);
        Date begin = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        Date end = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        return getEventDatesBetween(owner, orderby, begin, end);
    }

    public Vector getSevenDaysEvents(BigDecimal owner, String orderby, Calendar cal) throws SQLException {
        Date begin = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.DATE, +7);
        Date end = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        return getEventsBetween(owner, orderby, begin, end);
    }

    public Vector getWeeklyEvents(BigDecimal owner, String orderby, Calendar cal) throws SQLException {
        int back = -cal.get(Calendar.DAY_OF_WEEK) + cal.getFirstDayOfWeek();
        if (back == 1) back = -6;
        cal.add(Calendar.DATE, back);
        Date begin = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.DATE, +7);
        Date end = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        return getEventsBetween(owner, orderby, begin, end);
    }

    public Vector getDailyEvents(BigDecimal owner, String orderby, Calendar cal) throws SQLException {
        return getDailyEvents(owner, orderby, cal, true);
    }

    public Vector getDailyEvents(BigDecimal owner, String orderby, Calendar cal, boolean html) throws SQLException {
        Date begin = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH));
        Date end = Date.valueOf(cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + (cal.get(Calendar.DAY_OF_MONTH) + 1));
        return getEventsBetween(owner, orderby, begin, end, html);
    }

    public Vector upSync(BigDecimal owner, boolean fullSync) throws SQLException {
        Vector evts = new Vector();
        try {
            boolean foundone = false;
            con = allocateConnection(tableName);
            if (fullSync) {
                ps = con.prepareStatement("select * from  cal_Event where owner=? order by syncstatus");
                ps.setBigDecimal(1, owner);
            } else {
                ps = con.prepareStatement("select * from  cal_Event where owner=? and syncstatus>? order by syncstatus");
                ps.setBigDecimal(1, owner);
                ps.setInt(2, RESET);
            }
            rs = con.executeQuery(ps, null);
            while (rs.next()) {
                evts.addElement(new EventDO(rs, true, true));
                foundone = true;
            }
            if (foundone) {
                PreparedStatement delrem = con.prepareStatement("delete from cal_Event_Remind where " + "cal_Event_Remind.event in (select cal_Event.OId from cal_Event " + "where cal_Event.owner=? and cal_Event.syncstatus=?)");
                PreparedStatement delevt = con.prepareStatement("delete from cal_Event where owner=? and syncstatus=?");
                PreparedStatement update = con.prepareStatement("update cal_Event set syncstatus=?,dirtybits=? " + "where owner=?");
                delrem.setBigDecimal(1, owner);
                delrem.setInt(2, DELETE);
                delevt.setBigDecimal(1, owner);
                delevt.setInt(2, DELETE);
                update.setInt(1, RESET);
                update.setInt(2, RESET);
                update.setBigDecimal(3, owner);
                con.reset();
                con.executeUpdate(delrem, null);
                con.reset();
                con.executeUpdate(delevt, null);
                con.reset();
                con.executeUpdate(update, null);
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return evts;
    }

    public void downSync(Vector v) throws SQLException {
        try {
            con = allocateConnection(tableName);
            PreparedStatement update = con.prepareStatement("update cal_Event set owner=?,subject=?,text=?,place=?," + "contactperson=?,startdate=?,enddate=?,starttime=?,endtime=?,allday=?," + "syncstatus=?,dirtybits=? where OId=? and syncstatus=?");
            PreparedStatement insert = con.prepareStatement("insert into cal_Event (owner,subject,text,place," + "contactperson,startdate,enddate,starttime,endtime,allday,syncstatus," + "dirtybits) values(?,?,?,?,?,?,?,?,?,?,?,?)");
            PreparedStatement insert1 = con.prepareStatement(DBUtil.getQueryCurrentOID(con, "cal_Event", "newoid"));
            PreparedStatement delete1 = con.prepareStatement("delete from  cal_Event_Remind where event=?");
            PreparedStatement delete2 = con.prepareStatement("delete from  cal_Event where OId=? " + "and (syncstatus=? or syncstatus=?)");
            for (int i = 0; i < v.size(); i++) {
                try {
                    DO = (EventDO) v.elementAt(i);
                    if (DO.getSyncstatus() == INSERT) {
                        insert.setBigDecimal(1, DO.getOwner());
                        insert.setString(2, DO.getSubject());
                        insert.setString(3, DO.getText());
                        insert.setString(4, DO.getPlace());
                        insert.setString(5, DO.getContactperson());
                        insert.setDate(6, DO.getStartdate());
                        insert.setDate(7, DO.getEnddate());
                        insert.setTime(8, DO.getStarttime());
                        insert.setTime(9, DO.getEndtime());
                        insert.setBoolean(10, DO.getAllday());
                        insert.setInt(11, RESET);
                        insert.setInt(12, RESET);
                        con.executeUpdate(insert, null);
                        con.reset();
                        rs = con.executeQuery(insert1, null);
                        if (rs.next()) DO.setOId(rs.getBigDecimal("newoid"));
                        con.reset();
                    } else if (DO.getSyncstatus() == UPDATE) {
                        update.setBigDecimal(1, DO.getOwner());
                        update.setString(2, DO.getSubject());
                        update.setString(3, DO.getText());
                        update.setString(4, DO.getPlace());
                        update.setString(5, DO.getContactperson());
                        update.setDate(6, DO.getStartdate());
                        update.setDate(7, DO.getEnddate());
                        update.setTime(8, DO.getStarttime());
                        update.setTime(9, DO.getEndtime());
                        update.setBoolean(10, DO.getAllday());
                        update.setInt(11, RESET);
                        update.setInt(12, RESET);
                        update.setBigDecimal(13, DO.getOId());
                        update.setInt(14, RESET);
                        con.executeUpdate(update, null);
                        con.reset();
                    } else if (DO.getSyncstatus() == DELETE) {
                        try {
                            con.setAutoCommit(false);
                            delete1.setBigDecimal(1, DO.getOId());
                            con.executeUpdate(delete1, null);
                            delete2.setBigDecimal(1, DO.getOId());
                            delete2.setInt(2, RESET);
                            delete2.setInt(3, DELETE);
                            if (con.executeUpdate(delete2, null) < 1) {
                                con.rollback();
                            } else {
                                con.commit();
                            }
                        } catch (Exception e) {
                            con.rollback();
                            throw e;
                        } finally {
                            con.reset();
                        }
                    }
                } catch (Exception e) {
                    if (DO != null) logError("Sync-EventDO.owner = " + DO.getOwner().toString() + " oid = " + (DO.getOId() != null ? DO.getOId().toString() : "NULL"), e);
                }
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public void deleteEvent() throws SQLException {
        deleteEvent(DO.getOId(), DO.getOwner());
    }

    public void deleteEvent(BigDecimal oid, BigDecimal owner) throws SQLException {
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("delete from  cal_Event_Remind where event=?");
            ps.setBigDecimal(1, oid);
            con.executeUpdate(ps, null);
            con.reset();
            ps = con.prepareStatement("delete from cal_Event where syncstatus=? and OId=? and owner=?");
            ps.setInt(1, INSERT);
            ps.setBigDecimal(2, oid);
            ps.setBigDecimal(3, owner);
            if (con.executeUpdate(ps, null) < 1) {
                con.reset();
                ps = con.prepareStatement("update cal_Event set syncstatus=? where OId=? and owner=?");
                ps.setInt(1, DELETE);
                ps.setBigDecimal(2, oid);
                ps.setBigDecimal(3, owner);
                con.executeUpdate(ps, null);
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }
}

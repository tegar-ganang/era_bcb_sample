package lebah.planner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import lebah.db.UniqueID;
import lebah.planner.PlannerModule.TimeComparator;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class PlannerData {

    private static PlannerData instance;

    private PlannerData() {
    }

    public static PlannerData getInstance() {
        if (instance == null) instance = new PlannerData();
        return instance;
    }

    static String fmt(String s) {
        s = s.trim();
        if (s.length() == 1) return "0".concat(s); else return s;
    }

    static String strDate(int y, int m, int d) {
        String str = "" + y;
        if (m < 10) str += "-0" + m; else str += "-" + m;
        if (d < 10) str += "-0" + d; else str += "-" + d;
        return str;
    }

    static String fmtTime(int h, int m) {
        String mn = m < 10 ? "0" + Integer.toString(m) : Integer.toString(m);
        String ap = "";
        if (h < 12) {
            ap = "AM";
            if (h == 0) h = 12;
        } else {
            if (h > 12) h = h - 12;
            ap = "PM";
        }
        return Integer.toString(h).concat(":").concat(mn).concat("").concat(ap);
    }

    static String putLineBreak(String str) {
        StringBuffer txt = new StringBuffer(str);
        char c = '\r';
        while (txt.toString().indexOf(c) > -1) {
            int pos = txt.toString().indexOf(c);
            txt.replace(pos, pos + 1, "<br>");
        }
        return txt.toString();
    }

    public void doInsertTask(String user, String subjectid, Hashtable data) throws Exception {
        if (subjectid == null) subjectid = "";
        Db db = null;
        Connection conn = null;
        String sql = "";
        String task_description = (String) data.get("description");
        if ("".equals(task_description.trim())) return;
        String year1 = (String) data.get("year1");
        String month1 = (String) data.get("month1");
        String day1 = (String) data.get("day1");
        String hour1 = (String) data.get("hour1");
        String minute1 = (String) data.get("minute1");
        String hour2 = (String) data.get("hour2");
        String minute2 = (String) data.get("minute2");
        String invitelist = (String) data.get("invitelist");
        int ispublic = 1;
        String task_date = year1 + "-" + fmt(month1) + "-" + fmt(day1);
        Vector inviteVector = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(invitelist);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token != null) {
                inviteVector.addElement(token.trim());
            }
        }
        String id = user.concat(Long.toString(UniqueID.get()));
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            {
                r.add("task_id", id);
                r.add("user_login", user);
                r.add("task_description", task_description);
                r.add("task_date", task_date);
                r.add("hour_start", Integer.parseInt(hour1));
                r.add("hour_end", Integer.parseInt(hour2));
                r.add("minute_start", Integer.parseInt(minute1));
                r.add("minute_end", Integer.parseInt(minute2));
                r.add("task_public", ispublic);
                r.add("subject_id", subjectid);
                sql = r.getSQLInsert("planner_task");
                stmt.executeUpdate(sql);
            }
            {
                sql = "DELETE FROM planner_task_invite WHERE task_id = '" + id + "' ";
                stmt.executeUpdate(sql);
            }
            for (int i = 0; i < inviteVector.size(); i++) {
                r = new SQLRenderer();
                r.add("task_id", id);
                r.add("user_id", (String) inviteVector.elementAt(i));
                r.add("inviter_id", user);
                r.add("allow_edit", 0);
                sql = r.getSQLInsert("planner_task_invite");
                stmt.executeUpdate(sql);
            }
            conn.commit();
        } catch (DbException dbex) {
            System.out.println(dbex.getMessage());
            throw dbex;
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rex) {
                }
            }
            System.out.println(ex.getMessage() + sql);
            throw ex;
        } finally {
            if (db != null) db.close();
        }
    }

    public Vector getTaskVector(String user, String subjectid, int year1, int month1, int day1) throws Exception {
        Db db = null;
        String sql = "";
        Vector v = new Vector();
        String strdate = strDate(year1, month1, day1);
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            if ("".equals(subjectid)) {
                r.add("task_id");
                r.add("task_description");
                r.add("hour_start");
                r.add("hour_end");
                r.add("minute_start");
                r.add("minute_end");
                r.add("task_public");
                r.add("user_login", user);
                r.add("subject_id", "");
                r.add("task_date", strdate);
                sql = r.getSQLSelect("planner_task");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable task = new Hashtable();
                    String task_id = rs.getString("task_id");
                    String task_description = rs.getString("task_description");
                    String task_public = rs.getString("task_public");
                    int hour_start = rs.getInt("hour_start");
                    int minute_start = rs.getInt("minute_start");
                    int hour_end = rs.getInt("hour_end");
                    int minute_end = rs.getInt("minute_end");
                    String time_start = fmtTime(hour_start, minute_start);
                    String time_end = fmtTime(hour_end, minute_end);
                    task.put("task_id", task_id);
                    task.put("task_description", putLineBreak(task_description));
                    task.put("task_public", task_public);
                    task.put("hour_start", new Integer(hour_start));
                    task.put("hour_end", new Integer(hour_end));
                    task.put("minute_start", new Integer(minute_start));
                    task.put("minute_end", new Integer(minute_end));
                    task.put("time_start", time_start);
                    task.put("time_end", time_end);
                    task.put("inviter", "");
                    task.put("canEdit", new Boolean(true));
                    task.put("subject_code", "");
                    task.put("subject_title", "");
                    v.addElement(task);
                }
            }
            if (!"".equals(subjectid)) {
                r.clear();
                r.add("p.task_id");
                r.add("p.task_description");
                r.add("hour_start");
                r.add("hour_end");
                r.add("minute_start");
                r.add("minute_end");
                r.add("p.task_public");
                r.add("p.user_login");
                r.add("u.user_name");
                r.add("p.user_login", r.unquote("u.user_login"));
                r.add("p.subject_id", subjectid);
                r.add("p.subject_id", r.unquote("s.subject_id"));
                r.add("s.member_id", user);
                r.add("s.status", "active");
                r.add("task_date", strdate);
                sql = r.getSQLSelect("planner_task p, member_subject s, users u");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable task = new Hashtable();
                    String task_id = rs.getString("task_id");
                    String task_description = rs.getString("task_description");
                    String task_public = rs.getString("task_public");
                    String user_task = rs.getString("user_login");
                    String user_name = rs.getString("user_name");
                    int hour_start = rs.getInt("hour_start");
                    int minute_start = rs.getInt("minute_start");
                    int hour_end = rs.getInt("hour_end");
                    int minute_end = rs.getInt("minute_end");
                    String time_start = fmtTime(hour_start, minute_start);
                    String time_end = fmtTime(hour_end, minute_end);
                    task.put("task_id", task_id);
                    task.put("task_description", putLineBreak(task_description));
                    task.put("task_public", task_public);
                    task.put("hour_start", new Integer(hour_start));
                    task.put("hour_end", new Integer(hour_end));
                    task.put("minute_start", new Integer(minute_start));
                    task.put("minute_end", new Integer(minute_end));
                    task.put("time_start", time_start);
                    task.put("time_end", time_end);
                    task.put("inviter", user_name);
                    if (user.equals(user_task)) task.put("canEdit", new Boolean(true)); else task.put("canEdit", new Boolean(false));
                    task.put("subject_code", "");
                    task.put("subject_title", "");
                    v.addElement(task);
                }
            } else {
                r.clear();
                r.add("task_id");
                r.add("task_description");
                r.add("hour_start");
                r.add("hour_end");
                r.add("minute_start");
                r.add("minute_end");
                r.add("task_public");
                r.add("p.user_login");
                r.add("u.user_name");
                r.add("s.subject_code");
                r.add("s.subject_title");
                r.add("p.subject_id", r.unquote("ms.subject_id"));
                r.add("ms.member_id", user);
                r.add("ms.status", "active");
                r.add("p.subject_id", r.unquote("ms.subject_id"));
                r.add("ms.subject_id", r.unquote("s.subject_id"));
                r.add("p.user_login", r.unquote("u.user_login"));
                r.add("task_date", strdate);
                sql = r.getSQLSelect("planner_task p, users u, member_subject ms, subject s");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable task = new Hashtable();
                    String task_id = rs.getString("task_id");
                    String task_description = rs.getString("task_description");
                    String task_public = rs.getString("task_public");
                    String user_task = rs.getString("user_login");
                    String user_name = rs.getString("user_name");
                    int hour_start = rs.getInt("hour_start");
                    int minute_start = rs.getInt("minute_start");
                    int hour_end = rs.getInt("hour_end");
                    int minute_end = rs.getInt("minute_end");
                    String subject_code = rs.getString("subject_code");
                    String subject_title = rs.getString("subject_title");
                    String time_start = fmtTime(hour_start, minute_start);
                    String time_end = fmtTime(hour_end, minute_end);
                    task.put("task_id", task_id);
                    task.put("task_description", putLineBreak(task_description));
                    task.put("task_public", task_public);
                    task.put("hour_start", new Integer(hour_start));
                    task.put("hour_end", new Integer(hour_end));
                    task.put("minute_start", new Integer(minute_start));
                    task.put("minute_end", new Integer(minute_end));
                    task.put("time_start", time_start);
                    task.put("time_end", time_end);
                    task.put("inviter", "");
                    if (user.equals(user_task)) task.put("canEdit", new Boolean(true)); else task.put("canEdit", new Boolean(false));
                    task.put("subject_code", subject_code);
                    task.put("subject_title", subject_title);
                    v.addElement(task);
                }
            }
            if ("".equals(subjectid)) {
                r = new SQLRenderer();
                r.add("t.task_id AS id");
                r.add("t.task_description");
                r.add("t.task_public");
                r.add("u.user_name AS inviter_name");
                r.add("hour_start");
                r.add("hour_end");
                r.add("minute_start");
                r.add("minute_end");
                r.add("t.task_id", r.unquote("i.task_id"));
                r.add("i.inviter_id", r.unquote("u.user_login"));
                r.add("i.user_id", user);
                r.add("task_date", strdate);
                sql = r.getSQLSelect("planner_task t, planner_task_invite i, users u");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable task = new Hashtable();
                    String task_id = rs.getString("id");
                    String task_description = rs.getString("task_description");
                    String task_public = rs.getString("task_public");
                    String inviter_name = rs.getString("inviter_name");
                    int hour_start = rs.getInt("hour_start");
                    int minute_start = rs.getInt("minute_start");
                    int hour_end = rs.getInt("hour_end");
                    int minute_end = rs.getInt("minute_end");
                    String time_start = fmtTime(hour_start, minute_start);
                    String time_end = fmtTime(hour_end, minute_end);
                    task.put("task_id", task_id);
                    task.put("task_description", putLineBreak(task_description));
                    task.put("task_public", task_public);
                    task.put("hour_start", new Integer(hour_start));
                    task.put("hour_end", new Integer(hour_end));
                    task.put("minute_start", new Integer(minute_start));
                    task.put("minute_end", new Integer(minute_end));
                    task.put("time_start", time_start);
                    task.put("time_end", time_end);
                    task.put("inviter", inviter_name);
                    task.put("canEdit", new Boolean(false));
                    task.put("subject_code", "");
                    task.put("subject_title", "");
                    v.addElement(task);
                }
                Collections.sort(v, new TimeComparator());
            }
            return v;
        } catch (DbException dbex) {
            System.out.println(dbex.getMessage());
            throw dbex;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage() + sql);
            throw ex;
        } finally {
            if (db != null) db.close();
        }
    }

    public class TimeComparator implements java.util.Comparator {

        public int compare(Object o1, Object o2) {
            Hashtable htbl1 = (Hashtable) o1;
            Hashtable htbl2 = (Hashtable) o2;
            int start_hour1 = ((Integer) htbl1.get("hour_start")).intValue();
            int start_minute1 = ((Integer) htbl1.get("minute_start")).intValue();
            int start_hour2 = ((Integer) htbl2.get("hour_start")).intValue();
            int start_minute2 = ((Integer) htbl2.get("minute_start")).intValue();
            int end_hour1 = ((Integer) htbl1.get("hour_end")).intValue();
            int end_minute1 = ((Integer) htbl1.get("minute_end")).intValue();
            int end_hour2 = ((Integer) htbl2.get("hour_end")).intValue();
            int end_minute2 = ((Integer) htbl2.get("minute_end")).intValue();
            int result = 0;
            if (start_hour1 > start_hour2) result = 1; else if (start_hour1 < start_hour2) result = -1; else if (start_hour1 == start_hour2) {
                if (start_minute1 > start_minute2) result = 1; else if (start_minute1 < start_minute2) result = -1; else if (start_minute1 == start_minute2) {
                    if (end_hour1 > end_hour2) result = 1; else if (end_hour1 < end_hour2) result = -1; else if (end_hour1 == end_hour2) {
                        if (end_minute1 > end_minute2) result = 1; else if (end_minute1 < end_minute2) result = -1; else if (end_minute1 == end_minute2) {
                            result = 0;
                        }
                    } else result = 0;
                }
            } else result = 0;
            return result;
        }
    }
}

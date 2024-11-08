package lebah.planner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collections;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import lebah.db.UniqueID;
import org.apache.velocity.Template;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class PlannerModule extends lebah.portal.velocity.VTemplate {

    private String timeZoneId = "";

    boolean isLastPage = false;

    static int LIST_ROWS = 20;

    boolean standAlone = true;

    private static String[] month_name = { "January", "February", "March", "April", "May", "Jun", "July", "August", "September", "October", "November", "December" };

    private static String[] day_name = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

    private static String[] hour_name = { "12 AM", "1 AM", "2 AM", "3 AM", "4 AM", "5 AM", "6 AM", "7 AM", "8 AM", "9 AM", "10 AM", "11 AM", "12 PM", "1 PM", "2 PM", "3 PM", "4 PM", "5 PM", "6 PM", "7 PM", "8 PM", "9 PM", "10 PM", "11 PM" };

    public void setStandAlone(boolean b) {
        standAlone = b;
    }

    public Template doTemplate() throws Exception {
        Template template = engine.getTemplate("vtl/schedule/list_task.vm");
        HttpSession session = request.getSession();
        String inCollabModule = session.getAttribute("inCollabModule") != null ? (String) session.getAttribute("inCollabModule") : "false";
        String subjectId = "";
        if ("true".equals(inCollabModule)) subjectId = getId();
        context.put("month_name", month_name);
        String action = !"".equals(getParam("planner_action")) ? getParam("planner_action") : "listmytask";
        if ("addtask".equals(action)) {
            template = engine.getTemplate("vtl/schedule/add_task.vm");
        } else if ("insert".equals(action)) {
            doInsertTask(session, subjectId);
            doListTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("listmytask".equals(action)) {
            doListTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("deletetask".equals(action)) {
            doDeleteTask(session);
            doListTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("selectdate".equals(action)) {
            doListTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("edittask".equals(action)) {
            doEditTask(session);
            template = engine.getTemplate("vtl/schedule/edit_task.vm");
        } else if ("updatetask".equals(action)) {
            doUpdateTask(session);
            doListTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("goPreviousWeek".equals(action)) {
            doListTask(session, subjectId, "prevweek");
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("goNextWeek".equals(action)) {
            doListTask(session, subjectId, "nextweek");
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("goToday".equals(action)) {
            doListTask(session, subjectId, "today");
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("listUsers".equals(action)) {
            prepareList(session);
            getRows(session, 1);
            template = engine.getTemplate("vtl/schedule/list_users.vm");
        } else if ("goPage".equals(action)) {
            int page = Integer.parseInt(getParam("pagenum"));
            getRows(session, page);
            session.setAttribute("page_number", Integer.toString(page));
        } else if ("back".equals(action)) {
            doListCurrentTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        } else if ("selectNames".equals(action)) {
            String[] userids = request.getParameterValues("userids");
            context.put("inviteList", userids);
            doListCurrentTask(session, subjectId);
            template = engine.getTemplate("vtl/schedule/list_task.vm");
        }
        return template;
    }

    private void doInsertTask(HttpSession session, String subjectid) throws Exception {
        if (subjectid == null) subjectid = "";
        Db db = null;
        Connection conn = null;
        String sql = "";
        String user = (String) session.getAttribute("_portal_login");
        String task_description = getParam("description");
        if ("".equals(task_description.trim())) return;
        String year1 = getParam("year1");
        String month1 = getParam("month1");
        String day1 = getParam("day1");
        String hour1 = getParam("hour1");
        String minute1 = getParam("minute1");
        String hour2 = getParam("hour2");
        String minute2 = getParam("minute2");
        int ispublic = !"".equals(getParam("public")) ? Integer.parseInt(getParam("public")) : 1;
        String start_date = year1 + "-" + fmt(month1) + "-" + fmt(day1) + " " + fmt(hour1) + ":" + fmt(minute1) + ":00";
        String end_date = year1 + "-" + fmt(month1) + "-" + fmt(day1) + " " + fmt(hour2) + ":" + fmt(minute2) + ":00";
        String task_date = year1 + "-" + fmt(month1) + "-" + fmt(day1);
        String[] invitelist = request.getParameterValues("invitelist");
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
                r.add("task_start_date", start_date);
                r.add("task_end_date", end_date);
                r.add("task_public", ispublic);
                r.add("subject_id", subjectid);
                sql = r.getSQLInsert("planner_task");
                stmt.executeUpdate(sql);
            }
            {
                sql = "DELETE FROM planner_task_invite WHERE task_id = '" + id + "' ";
                stmt.executeUpdate(sql);
            }
            if (invitelist != null) {
                for (int i = 0; i < invitelist.length; i++) {
                    r = new SQLRenderer();
                    r.add("task_id", id);
                    r.add("user_id", invitelist[i]);
                    r.add("inviter_id", user);
                    r.add("allow_edit", 0);
                    sql = r.getSQLInsert("planner_task_invite");
                    stmt.executeUpdate(sql);
                }
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

    private String fmt(String s) {
        s = s.trim();
        if (s.length() == 1) return "0".concat(s); else return s;
    }

    private String strDate(int y, int m, int d) {
        String str = "" + y;
        if (m < 10) str += "-0" + m; else str += "-" + m;
        if (d < 10) str += "-0" + d; else str += "-" + d;
        return str;
    }

    private void doListTask(HttpSession session, String subjectid) throws Exception {
        doListTask(session, subjectid, "");
    }

    private void doListTask(HttpSession session, String subjectid, String go) throws Exception {
        String user = (String) session.getAttribute("_portal_login");
        int year1 = 0, month1 = 0, day1 = 0;
        java.util.Calendar calendar = "".equals(timeZoneId) ? new java.util.GregorianCalendar() : new java.util.GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
        calendar.setTime(new java.util.Date());
        if (!"today".equals(go)) {
            year1 = !"".equals(getParam("year1")) ? Integer.parseInt(getParam("year1")) : calendar.get(Calendar.YEAR);
            month1 = !"".equals(getParam("month1")) ? Integer.parseInt(getParam("month1")) : calendar.get(Calendar.MONTH) + 1;
            day1 = !"".equals(getParam("day1")) ? Integer.parseInt(getParam("day1")) : calendar.get(Calendar.DAY_OF_MONTH);
            if (!"".equals(getParam("year1")) && !"".equals(getParam("month1")) && !"".equals(getParam("month1"))) {
                calendar = new java.util.GregorianCalendar(year1, month1 - 1, day1);
            }
        }
        if ("nextweek".equals(go)) {
            calendar.add(Calendar.DATE, 7);
        } else if ("prevweek".equals(go)) {
            calendar.add(Calendar.DATE, -7);
        }
        session.setAttribute("calendar", calendar);
        displayWeekly(session, calendar, subjectid);
        if (!"".equals(subjectid) && userIsTeacher(user, subjectid)) context.put("allowAddTask", new Boolean(true)); else context.put("allowAddTask", new Boolean(false));
    }

    private void doListCurrentTask(HttpSession session, String subjectid) throws Exception {
        String user = (String) session.getAttribute("_portal_login");
        java.util.Calendar calendar = (java.util.Calendar) session.getAttribute("calendar");
        displayWeekly(session, calendar, subjectid);
        if (!"".equals(subjectid) && userIsTeacher(user, subjectid)) context.put("allowAddTask", new Boolean(true)); else context.put("allowAddTask", new Boolean(false));
    }

    private void displayWeekly(HttpSession session, java.util.Calendar calendar, String subjectid) throws Exception {
        String user = (String) session.getAttribute("_portal_login");
        int year1 = calendar.get(Calendar.YEAR);
        int month1 = calendar.get(Calendar.MONTH) + 1;
        int day1 = calendar.get(Calendar.DAY_OF_MONTH);
        int day_week = calendar.get(Calendar.DAY_OF_WEEK);
        int max_day = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        Hashtable schedules = new Hashtable();
        Hashtable scheduleDates = new Hashtable();
        Hashtable scheduleDateValues = new Hashtable();
        int dy = day1, mn = month1, yr = year1, dw = day_week;
        for (int i = 0; i < day_week - 1; i++) {
            int num = --dw;
            if (dy == 1) {
                mn--;
                if (mn == 0) {
                    mn = 12;
                    yr--;
                }
                Calendar dummy = new java.util.GregorianCalendar(yr, mn - 1, 1);
                int dummy_max = dummy.getActualMaximum(Calendar.DAY_OF_MONTH);
                dy = dummy_max;
                String taskdate = lebah.util.DateTool.getDateFormatted((new java.util.GregorianCalendar(yr, mn - 1, dy)).getTime());
                Vector tasklist = PlannerData.getInstance().getTaskVector(user, subjectid, yr, mn, dy);
                schedules.put("d" + Integer.toString(num), tasklist);
                scheduleDates.put("d" + Integer.toString(num), taskdate);
                Hashtable h = new Hashtable();
                h.put("day", new Integer(dy));
                h.put("month", new Integer(mn));
                h.put("year", new Integer(yr));
                scheduleDateValues.put("d" + Integer.toString(num), h);
            } else {
                dy--;
                Vector tasklist = PlannerData.getInstance().getTaskVector(user, subjectid, yr, mn, dy);
                schedules.put("d" + Integer.toString(num), tasklist);
                String taskdate = lebah.util.DateTool.getDateFormatted((new java.util.GregorianCalendar(yr, mn - 1, dy)).getTime());
                scheduleDates.put("d" + Integer.toString(num), taskdate);
                Hashtable h = new Hashtable();
                h.put("day", new Integer(dy));
                h.put("month", new Integer(mn));
                h.put("year", new Integer(yr));
                scheduleDateValues.put("d" + Integer.toString(num), h);
            }
        }
        dy = day1;
        mn = month1;
        yr = year1;
        for (int i = 0; i < (8 - day_week); i++) {
            int num = i + day_week;
            Vector tasklist = PlannerData.getInstance().getTaskVector(user, subjectid, yr, mn, dy);
            schedules.put("d" + Integer.toString(num), tasklist);
            String taskdate = lebah.util.DateTool.getDateFormatted((new java.util.GregorianCalendar(yr, mn - 1, dy)).getTime());
            scheduleDates.put("d" + Integer.toString(num), taskdate);
            Hashtable h = new Hashtable();
            h.put("day", new Integer(dy));
            h.put("month", new Integer(mn));
            h.put("year", new Integer(yr));
            scheduleDateValues.put("d" + Integer.toString(num), h);
            if (dy == max_day) {
                dy = 0;
                mn++;
                if (mn == 13) {
                    mn = 1;
                    yr++;
                }
            }
            dy++;
        }
        context.put("schedules", schedules);
        context.put("scheduleDates", scheduleDates);
        context.put("scheduleDateValues", scheduleDateValues);
        String date_display = lebah.util.DateTool.getDateFormatted(calendar.getTime());
        context.put("year", new Integer(year1));
        context.put("month", new Integer(month1));
        context.put("day", new Integer(day1));
        context.put("date_display", date_display);
    }

    private boolean userIsTeacher(String userid, String subjectid) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("member_id", userid);
            r.add("subject_id", subjectid);
            r.add("role");
            sql = r.getSQLSelect("member_subject");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                if ("tutor".equals(rs.getString("role"))) return true; else return false;
            } else return false;
        } finally {
            if (db != null) db.close();
        }
    }

    private void doDeleteTask(HttpSession session) {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            String task_id = getParam("task_id");
            sql = "DELETE FROM planner_task WHERE task_id = '" + task_id + "'";
            stmt.executeUpdate(sql);
        } catch (DbException dbex) {
            System.out.println(dbex.getMessage());
        } catch (SQLException ex) {
            System.out.println(ex.getMessage() + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    private String fmtTime(int h, int m) {
        String mn = m < 10 ? "0" + Integer.toString(m) : Integer.toString(m);
        String ap = "";
        if (h < 12) {
            ap = "AM";
            if (h == 0) h = 12;
        } else {
            if (h > 12) h = h - 12;
            ap = "PM";
        }
        return Integer.toString(h).concat(":").concat(mn).concat(" ").concat(ap);
    }

    String putLineBreak(String str) {
        StringBuffer txt = new StringBuffer(str);
        char c = '\r';
        while (txt.toString().indexOf(c) > -1) {
            int pos = txt.toString().indexOf(c);
            txt.replace(pos, pos + 1, "<br>");
        }
        return txt.toString();
    }

    private void doEditTask(HttpSession session) {
        Hashtable task = new Hashtable();
        Db db = null;
        Connection conn = null;
        String sql = "";
        String user = (String) session.getAttribute("_portal_login");
        String id = getParam("task_id");
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.add("task_description");
                r.add("task_public");
                r.add("hour_start");
                r.add("hour_end");
                r.add("minute_start");
                r.add("minute_end");
                r.add("task_date");
                r.add("task_id", id);
                sql = r.getSQLSelect("planner_task");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String task_id = id;
                    String task_description = rs.getString("task_description");
                    java.sql.Date task_date = rs.getDate("task_date");
                    String date_display = lebah.util.DateTool.getDateFormatted(task_date);
                    String task_public = rs.getString("task_public");
                    Calendar c = new java.util.GregorianCalendar();
                    c.setTime(task_date);
                    int year1 = c.get(Calendar.YEAR);
                    int month1 = c.get(Calendar.MONTH) + 1;
                    int day1 = c.get(Calendar.DAY_OF_MONTH);
                    int hour_start = rs.getInt("hour_start");
                    int minute_start = rs.getInt("minute_start");
                    int hour_end = rs.getInt("hour_end");
                    int minute_end = rs.getInt("minute_end");
                    String time_start = fmtTime(hour_start, minute_start);
                    String time_end = fmtTime(hour_end, minute_end);
                    task.put("task_id", task_id);
                    task.put("task_description", task_description);
                    task.put("hour_start", new Integer(hour_start));
                    task.put("minute_start", new Integer(minute_start));
                    task.put("hour_end", new Integer(hour_end));
                    task.put("minute_end", new Integer(minute_end));
                    task.put("task_public", task_public);
                    task.put("time_start", time_start);
                    task.put("time_end", time_end);
                    task.put("inviter", "");
                    context.put("task", task);
                    context.put("year", new Integer(year1));
                    context.put("month", new Integer(month1));
                    context.put("day", new Integer(day1));
                    context.put("date_display", date_display);
                }
            }
            String invite_list = "";
            Vector inviteList = new Vector();
            {
                r = new SQLRenderer();
                r.add("user_id");
                r.add("task_id", id);
                sql = r.getSQLSelect("planner_task_invite", "user_id");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String userid = rs.getString("user_id");
                    inviteList.addElement(userid);
                    invite_list += userid + " ";
                }
            }
            context.put("inviteList", inviteList);
            context.put("task_invite_list", invite_list);
        } catch (DbException dbex) {
            System.out.println(dbex.getMessage());
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rex) {
                }
            }
            System.out.println(ex.getMessage() + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    private void doUpdateTaskX(HttpSession session) {
        Db db = null;
        Connection conn = null;
        String sql = "";
        String user = (String) session.getAttribute("_portal_login");
        String id = getParam("task_id");
        String task_description = getParam("description");
        String year1 = getParam("year1");
        String month1 = getParam("month1");
        String day1 = getParam("day1");
        String hour1 = getParam("hour1");
        String minute1 = getParam("minute1");
        String hour2 = getParam("hour2");
        String minute2 = getParam("minute2");
        int ispublic = !"".equals(getParam("public")) ? Integer.parseInt(getParam("public")) : 1;
        String task_date = year1 + "-" + fmt(month1) + "-" + fmt(day1);
        String[] invitelist = request.getParameterValues("invitelist");
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            {
                sql = "UPDATE planner_task SET task_description = '" + task_description + "', " + "task_date = '" + task_date + " WHERE task_id = '" + id + "'";
                stmt.executeUpdate(sql);
            }
            {
                sql = "DELETE FROM planner_task_invite WHERE task_id = '" + id + "' ";
                stmt.executeUpdate(sql);
            }
            if (invitelist != null) {
                for (int i = 0; i < invitelist.length; i++) {
                    r = new SQLRenderer();
                    r.add("task_id", id);
                    r.add("user_id", invitelist[i]);
                    r.add("inviter_id", user);
                    r.add("allow_edit", 0);
                    sql = r.getSQLInsert("planner_task_invite");
                    stmt.executeUpdate(sql);
                }
            }
            conn.commit();
        } catch (DbException dbex) {
            System.out.println(dbex.getMessage());
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rex) {
                }
            }
            System.out.println(ex.getMessage() + sql);
        } finally {
            if (db != null) db.close();
        }
    }

    private void doUpdateTask(HttpSession session) throws Exception {
        String id = getParam("task_id");
        Db db = null;
        Connection conn = null;
        String sql = "";
        String user = (String) session.getAttribute("_portal_login");
        String task_description = getParam("description");
        if ("".equals(task_description.trim())) return;
        String year1 = getParam("year1");
        String month1 = getParam("month1");
        String day1 = getParam("day1");
        String hour1 = getParam("hour1");
        String minute1 = getParam("minute1");
        String hour2 = getParam("hour2");
        String minute2 = getParam("minute2");
        int ispublic = !"".equals(getParam("public")) ? Integer.parseInt(getParam("public")) : 1;
        String task_date = year1 + "-" + fmt(month1) + "-" + fmt(day1);
        String[] invitelist = request.getParameterValues("invitelist");
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            {
                r.update("task_id", id);
                r.add("task_description", task_description);
                r.add("task_date", task_date);
                r.add("hour_start", Integer.parseInt(hour1));
                r.add("hour_end", Integer.parseInt(hour2));
                r.add("minute_start", Integer.parseInt(minute1));
                r.add("minute_end", Integer.parseInt(minute2));
                r.add("task_public", ispublic);
                sql = r.getSQLUpdate("planner_task");
                stmt.executeUpdate(sql);
            }
            {
                sql = "DELETE FROM planner_task_invite WHERE task_id = '" + id + "' ";
                stmt.executeUpdate(sql);
            }
            if (invitelist != null) {
                for (int i = 0; i < invitelist.length; i++) {
                    r = new SQLRenderer();
                    r.add("task_id", id);
                    r.add("user_id", invitelist[i]);
                    r.add("inviter_id", user);
                    r.add("allow_edit", 0);
                    sql = r.getSQLInsert("planner_task_invite");
                    stmt.executeUpdate(sql);
                }
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

    public static String[] getTimeZoneIDs() {
        return TimeZone.getAvailableIDs();
    }

    void prepareList(HttpSession session) throws Exception {
        Vector list = UsersData.getList();
        int pages = list.size() / LIST_ROWS;
        double leftover = ((double) list.size() % (double) LIST_ROWS);
        if (leftover > 0.0) ++pages;
        context.put("pages", new Integer(pages));
        session.setAttribute("pages", new Integer(pages));
        session.setAttribute("userList", list);
    }

    void getRows(HttpSession session, int page) throws Exception {
        Vector list = (Vector) session.getAttribute("userList");
        Vector items = getPage(page, LIST_ROWS, list);
        context.put("users", items);
        context.put("page_number", new Integer(page));
        context.put("pages", (Integer) session.getAttribute("pages"));
    }

    Vector getPage(int page, int size, Vector list) throws Exception {
        int elementstart = (page - 1) * size;
        int elementlast = 0;
        if (page * size < list.size()) {
            elementlast = (page * size) - 1;
            isLastPage = false;
            context.put("eol", new Boolean(false));
        } else {
            elementlast = list.size() - 1;
            isLastPage = true;
            context.put("eol", new Boolean(true));
        }
        if (page == 1) context.put("bol", new Boolean(true)); else context.put("bol", new Boolean(false));
        Vector v = new Vector();
        for (int i = elementstart; i < elementlast + 1; i++) {
            v.addElement(list.elementAt(i));
        }
        return v;
    }
}

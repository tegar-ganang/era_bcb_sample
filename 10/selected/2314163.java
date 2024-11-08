package mecca.sis.registration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;
import mecca.db.Db;
import mecca.db.DbException;
import mecca.db.SQLRenderer;
import mecca.sis.struct.Subject;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class StudentData {

    public static void add(Hashtable studentInfo) throws Exception {
        add(studentInfo, "add");
    }

    public static void update(Hashtable studentInfo) throws Exception {
        add(studentInfo, "update");
    }

    public static void add(Hashtable studentInfo, String mode) throws Exception {
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                r.add("id");
                r.add("id", (String) studentInfo.get("student_id"));
                sql = r.getSQLSelect("student");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true; else found = false;
            }
            if (found && !"update".equals(mode)) throw new Exception("student Id was invalid!");
            {
                r.clear();
                r.add("applicant_id", getInfoData(studentInfo, "applicant_id"));
                r.add("password", getInfoData(studentInfo, "student_id"));
                r.add("name", getInfoData(studentInfo, "name"));
                r.add("icno", getInfoData(studentInfo, "icno"));
                r.add("address", getInfoData(studentInfo, "address"));
                r.add("address1", getInfoData(studentInfo, "address1"));
                r.add("address2", getInfoData(studentInfo, "address2"));
                r.add("address3", getInfoData(studentInfo, "address3"));
                r.add("city", getInfoData(studentInfo, "city"));
                r.add("state", getInfoData(studentInfo, "state"));
                r.add("poscode", getInfoData(studentInfo, "poscode"));
                r.add("country_code", getInfoData(studentInfo, "country_code"));
                r.add("phone", getInfoData(studentInfo, "phone"));
                r.add("birth_date", getInfoData(studentInfo, "birth_date"));
                r.add("gender", getInfoData(studentInfo, "gender"));
            }
            if (!found) {
                r.add("id", (String) studentInfo.get("student_id"));
                sql = r.getSQLInsert("student");
                stmt.executeUpdate(sql);
            } else {
                r.update("id", (String) studentInfo.get("student_id"));
                sql = r.getSQLUpdate("student");
                stmt.executeUpdate(sql);
            }
            {
                String centre_id = getInfoData(studentInfo, "centre_id");
                sql = "update student set centre_id = '" + centre_id + "' where id = '" + (String) studentInfo.get("student_id") + "'";
                stmt.executeUpdate(sql);
            }
            conn.commit();
        } catch (DbException dbex) {
            throw dbex;
        } catch (SQLException sqlex) {
            try {
                conn.rollback();
            } catch (SQLException rollex) {
            }
            throw sqlex;
        } finally {
            if (db != null) db.close();
        }
        createPortalLogin((String) studentInfo.get("student_id"), (String) studentInfo.get("student_id"), (String) studentInfo.get("name"));
    }

    private static String getInfoData(Hashtable h, String field) {
        if (h.get(field) != null) return (String) h.get(field); else return "";
    }

    private static void createPortalLogin(String login, String password, String name) throws Exception {
        String sql = "";
        Connection conn = null;
        Db db = null;
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                sql = "select user_login from users where user_login = '" + login + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                {
                    r.add("user_login", login);
                    r.add("user_password", mecca.util.PasswordService.encrypt(password));
                    r.add("user_name", name);
                    r.add("user_role", "student");
                    sql = r.getSQLInsert("users");
                    stmt.executeUpdate(sql);
                }
                {
                    sql = "select css_name from user_css where user_login = 'student'";
                    ResultSet rs = stmt.executeQuery(sql);
                    String css_name = "default.css";
                    if (rs.next()) css_name = rs.getString("css_name");
                    sql = "insert into user_css (user_login, css_name) values ('" + login + "', '" + css_name + "')";
                    stmt.executeUpdate(sql);
                }
                {
                    Vector vector = new Vector();
                    {
                        sql = "select tab_id, tab_title, sequence, display_type from tab_template where user_login = 'student'";
                        ResultSet rs = stmt.executeQuery(sql);
                        while (rs.next()) {
                            Hashtable h = new Hashtable();
                            h.put("tab_id", rs.getString("tab_id"));
                            h.put("tab_title", rs.getString("tab_title"));
                            h.put("sequence", rs.getString("sequence"));
                            h.put("display_type", rs.getString("display_type"));
                            vector.addElement(h);
                        }
                    }
                    {
                        for (int i = 0; i < vector.size(); i++) {
                            Hashtable h = (Hashtable) vector.elementAt(i);
                            r.clear();
                            r.add("tab_id", (String) h.get("tab_id"));
                            r.add("tab_title", (String) h.get("tab_title"));
                            r.add("sequence", Integer.parseInt((String) h.get("sequence")));
                            r.add("display_type", (String) h.get("display_type"));
                            r.add("user_login", login);
                            sql = r.getSQLInsert("tab");
                            stmt.executeUpdate(sql);
                        }
                    }
                }
                {
                    Vector vector = new Vector();
                    {
                        sql = "select tab_id, module_id, sequence, module_custom_title, column_number " + "from user_module_template where user_login = 'student'";
                        ResultSet rs = stmt.executeQuery(sql);
                        while (rs.next()) {
                            Hashtable h = new Hashtable();
                            h.put("tab_id", rs.getString("tab_id"));
                            h.put("module_id", rs.getString("module_id"));
                            h.put("sequence", rs.getString("sequence"));
                            h.put("module_custom_title", mecca.db.Db.getString(rs, "module_custom_title"));
                            String coln = mecca.db.Db.getString(rs, "column_number");
                            h.put("column_number", coln.equals("") ? "0" : coln);
                            vector.addElement(h);
                        }
                    }
                    if (vector.size() > 0) {
                        for (int i = 0; i < vector.size(); i++) {
                            Hashtable h = (Hashtable) vector.elementAt(i);
                            r.clear();
                            r.add("tab_id", (String) h.get("tab_id"));
                            r.add("module_id", (String) h.get("module_id"));
                            r.add("sequence", Integer.parseInt((String) h.get("sequence")));
                            r.add("module_custom_title", (String) h.get("module_custom_title"));
                            r.add("column_number", Integer.parseInt((String) h.get("column_number")));
                            r.add("user_login", login);
                            sql = r.getSQLInsert("user_module");
                            stmt.executeUpdate(sql);
                        }
                    }
                }
            } else {
                r.add("user_name", name);
                r.update("user_login", login);
                sql = r.getSQLUpdate("users");
                stmt.executeUpdate(sql);
            }
            conn.commit();
        } catch (Exception ex) {
            try {
                conn.rollback();
            } catch (SQLException rollex) {
            }
            throw ex;
        } finally {
            if (db != null) db.close();
        }
    }

    private static String getPeriodRootId(String program_code) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("period_root_id");
            r.add("program_code", program_code);
            sql = r.getSQLSelect("program");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) return rs.getString("period_root_id"); else return "";
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getDataMap() throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Hashtable dataMap = new Hashtable();
            {
                r.add("data_id");
                r.add("data_name");
                sql = r.getSQLSelect("custom_data");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    dataMap.put(rs.getString("data_id"), rs.getString("data_name"));
                }
            }
            return dataMap;
        } finally {
            if (db != null) db.close();
        }
    }

    private static void addToList(Hashtable dataMap, Hashtable dataFilter, Hashtable displayData, Hashtable h, Vector v, Vector dataField) {
        boolean add = true;
        for (Enumeration e = dataMap.keys(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String s1 = dataFilter.get(key) != null ? (String) dataFilter.get(key) : "";
            String s2 = h.get(key) != null ? (String) h.get(key) : "";
            if (!"".equals(s1) && !(s1).equals(s2)) {
                add = false;
                break;
            }
        }
        if (add) {
            v.addElement(displayData);
        }
    }

    public static Hashtable getEnrollmentInfo(String student_id) throws Exception {
        String session_id = SessionData.getCurrentSessionId();
        boolean isExist = false;
        Db db = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            String sql = "select batch_id from student_status where session_id = '" + session_id + "' " + "and student_id = '" + student_id + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) isExist = true;
        } finally {
            if (db != null) db.close();
        }
        if (isExist) return getEnrollmentInfo1(student_id, session_id); else return getEnrollmentInfo2(student_id);
    }

    public static Hashtable getEnrollmentInfo(Statement stmt, String student_id) throws Exception {
        String session_id = SessionData.getCurrentSessionId();
        boolean isExist = false;
        String sql = "select batch_id from student_status where session_id = '" + session_id + "' " + "and student_id = '" + student_id + "'";
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) isExist = true;
        if (isExist) return getEnrollmentInfo1(student_id, session_id); else return getEnrollmentInfo2(student_id);
    }

    public static Hashtable getEnrollmentInfo1(String student_id, String session_id) throws Exception {
        Db db = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            return getEnrollmentInfo1(stmt, student_id, session_id);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getEnrollmentInfo1(Statement stmt, String student_id, String session_id) throws Exception {
        Hashtable info = new Hashtable();
        info.put("isEnrolled", new Boolean(false));
        String sql = "";
        SQLRenderer r = new SQLRenderer();
        boolean enrolled = true;
        String period_id = "";
        {
            r.add("stu.name as student_name");
            r.add("stu.icno");
            r.add("c.course_id");
            r.add("c.course_code");
            r.add("c.course_name");
            r.add("sc.program_code");
            r.add("p.program_name");
            r.add("sc.period_root_id");
            r.add("sc.track_id");
            r.add("sc.intake_month");
            r.add("sc.intake_year");
            r.add("sc.intake_code");
            r.add("sc.intake_session");
            r.add("s2.session_name as intake_session_name");
            r.add("sta.batch_id as batch_session");
            r.add("s.session_name as batch_session_name");
            r.add("f.faculty_code");
            r.add("f.faculty_name");
            r.add("i.institution_id");
            r.add("i.institution_name");
            r.add("i.institution_abbr");
            r.add("b.period_id as session_period_id");
            r.add("sc.student_id", r.unquote("stu.id"));
            r.add("c.course_id", r.unquote("sc.course_id"));
            r.add("sc.program_code", r.unquote("p.program_code"));
            r.add("c.faculty_id", r.unquote("f.faculty_id"));
            r.add("f.institution_id", r.unquote("i.institution_id"));
            r.add("sta.student_id", r.unquote("sc.student_id"));
            r.add("sta.batch_id", r.unquote("s.session_id"));
            r.add("sc.intake_session", r.unquote("s2.session_id"));
            r.add("b.session_id", r.unquote("sta.session_id"));
            r.add("b.intake_session", r.unquote("sta.batch_id"));
            r.add("b.period_root_id", r.unquote("sc.period_root_id"));
            r.add("sc.student_id", student_id);
            r.add("sta.session_id", session_id);
            sql = r.getSQLSelect("student stu, student_course sc, student_status sta, study_course c, intake_batch b, session s, session s2, program p, faculty f, institution i");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                info.put("id", student_id);
                info.put("student_id", student_id);
                info.put("student_name", rs.getString("student_name"));
                info.put("icno", Db.getString(rs, "icno"));
                info.put("course_id", mecca.db.Db.getString(rs, "course_id"));
                info.put("course_code", mecca.db.Db.getString(rs, "course_code"));
                info.put("program_code", mecca.db.Db.getString(rs, "program_code"));
                info.put("program_name", mecca.db.Db.getString(rs, "program_name"));
                info.put("course_name", mecca.db.Db.getString(rs, "course_name"));
                info.put("period_scheme", mecca.db.Db.getString(rs, "period_root_id"));
                info.put("track_id", mecca.db.Db.getString(rs, "track_id"));
                info.put("intake_month", new Integer(rs.getInt("intake_month")));
                info.put("intake_year", new Integer(rs.getInt("intake_year")));
                info.put("intake_code", mecca.db.Db.getString(rs, "intake_code"));
                info.put("intake_session", mecca.db.Db.getString(rs, "intake_session"));
                info.put("intake_session_name", mecca.db.Db.getString(rs, "intake_session_name"));
                info.put("batch_session", mecca.db.Db.getString(rs, "batch_session"));
                info.put("batch_session_name", mecca.db.Db.getString(rs, "batch_session_name"));
                info.put("faculty_code", mecca.db.Db.getString(rs, "faculty_code"));
                info.put("faculty_name", mecca.db.Db.getString(rs, "faculty_name"));
                info.put("institution_id", mecca.db.Db.getString(rs, "institution_id"));
                info.put("institution_name", mecca.db.Db.getString(rs, "institution_name"));
                info.put("institution_abbr", mecca.db.Db.getString(rs, "institution_abbr"));
                period_id = mecca.db.Db.getString(rs, "session_period_id");
                info.put("session_period_id", period_id);
            } else {
                enrolled = false;
            }
        }
        if (!enrolled) return info;
        String track_id = (String) info.get("track_id");
        if ("".equals(track_id)) track_id = "0";
        if (!"0".equals(track_id)) {
            r.clear();
            r.add("t.program_code", (String) info.get("program_code"));
            r.add("t.track_id", (String) info.get("track_id"));
            r.add("t.track_name");
            r.add("t.period_root_id");
            r.add("p.program_name");
            r.add("t.program_code", r.unquote("p.program_code"));
            sql = r.getSQLSelect("program_track t, program p");
            ResultSet rs = stmt.executeQuery(sql);
            Hashtable h = new Hashtable();
            while (rs.next()) {
                info.put("track_name", rs.getString("track_name"));
            }
        } else {
            info.put("track_name", "Default");
        }
        {
            info.put("isEnrolled", new Boolean(true));
            int period_level = 0;
            String parent_id = "";
            info.put("period_name", "none");
            String period_name = (String) info.get("period_name");
            while (period_level != 0) {
                r.clear();
                r.add("period_name");
                r.add("parent_id");
                r.add("period_level");
                r.add("period_id", parent_id);
                sql = r.getSQLSelect("period");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    period_name = rs.getString("period_name") + ", " + period_name;
                    parent_id = rs.getString("parent_id");
                    period_level = rs.getInt("period_level");
                }
            }
            info.put("period_name", period_name);
        }
        info.put("period_id", period_id);
        if (period_id != null && !"".equals(period_id)) {
            if ("FINISHED".equals(period_id)) {
                info.put("period_name", "FINISHED STUDY");
            } else {
                createPeriodName(info);
            }
        } else {
            info.put("period_name", "UNDEFINED");
        }
        {
            sql = "select c.centre_id, centre_code, centre_name from student s, learning_centre c " + "where s.centre_id = c.centre_id and id = '" + student_id + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                info.put("centre_id", rs.getString("centre_id"));
                info.put("centre_code", rs.getString("centre_code"));
                info.put("centre_name", rs.getString("centre_name"));
            } else {
                info.put("centre_id", "");
                info.put("centre_code", "");
                info.put("centre_name", "");
            }
        }
        return info;
    }

    public static Hashtable getEnrollmentInfo2(String student_id) throws Exception {
        Db db = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            return getEnrollmentInfo2(stmt, student_id);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getEnrollmentInfo2(Statement stmt, String student_id) throws Exception {
        Hashtable info = new Hashtable();
        info.put("isEnrolled", new Boolean(false));
        String sql = "";
        SQLRenderer r = new SQLRenderer();
        boolean enrolled = true;
        {
            r.add("stu.name as student_name");
            r.add("stu.icno");
            r.add("c.course_id");
            r.add("c.course_code");
            r.add("c.course_name");
            r.add("sc.program_code");
            r.add("p.program_name");
            r.add("sc.period_root_id");
            r.add("sc.track_id");
            r.add("sc.intake_month");
            r.add("sc.intake_year");
            r.add("sc.intake_code");
            r.add("sc.intake_session");
            r.add("s.session_name");
            r.add("f.faculty_code");
            r.add("f.faculty_name");
            r.add("i.institution_id");
            r.add("i.institution_name");
            r.add("i.institution_abbr");
            r.add("sc.student_id", student_id);
            r.add("sc.student_id", r.unquote("stu.id"));
            r.add("c.course_id", r.unquote("sc.course_id"));
            r.add("sc.program_code", r.unquote("p.program_code"));
            r.add("c.faculty_id", r.unquote("f.faculty_id"));
            r.add("f.institution_id", r.unquote("i.institution_id"));
            r.add("sc.intake_session", r.unquote("s.session_id"));
            sql = r.getSQLSelect("student stu, student_course sc, study_course c, session s, program p, faculty f, institution i");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                info.put("id", student_id);
                info.put("student_id", student_id);
                info.put("student_name", rs.getString("student_name"));
                info.put("icno", Db.getString(rs, "icno"));
                info.put("course_id", mecca.db.Db.getString(rs, "course_id"));
                info.put("course_code", mecca.db.Db.getString(rs, "course_code"));
                info.put("program_code", mecca.db.Db.getString(rs, "program_code"));
                info.put("program_name", mecca.db.Db.getString(rs, "program_name"));
                info.put("course_name", mecca.db.Db.getString(rs, "course_name"));
                info.put("period_scheme", mecca.db.Db.getString(rs, "period_root_id"));
                info.put("track_id", mecca.db.Db.getString(rs, "track_id"));
                info.put("intake_month", new Integer(rs.getInt("intake_month")));
                info.put("intake_year", new Integer(rs.getInt("intake_year")));
                info.put("intake_code", mecca.db.Db.getString(rs, "intake_code"));
                info.put("intake_session", mecca.db.Db.getString(rs, "intake_session"));
                info.put("intake_session_name", mecca.db.Db.getString(rs, "session_name"));
                info.put("faculty_code", mecca.db.Db.getString(rs, "faculty_code"));
                info.put("faculty_name", mecca.db.Db.getString(rs, "faculty_name"));
                info.put("institution_id", mecca.db.Db.getString(rs, "institution_id"));
                info.put("institution_name", mecca.db.Db.getString(rs, "institution_name"));
                info.put("institution_abbr", mecca.db.Db.getString(rs, "institution_abbr"));
                info.put("batch_session", "");
                info.put("batch_session_name", "");
            } else {
                enrolled = false;
            }
        }
        if (!enrolled) return info;
        String track_id = (String) info.get("track_id");
        if ("".equals(track_id)) track_id = "0";
        if (!"0".equals(track_id)) {
            r.clear();
            r.add("t.program_code", (String) info.get("program_code"));
            r.add("t.track_id", (String) info.get("track_id"));
            r.add("t.track_name");
            r.add("t.period_root_id");
            r.add("p.program_name");
            r.add("t.program_code", r.unquote("p.program_code"));
            sql = r.getSQLSelect("program_track t, program p");
            ResultSet rs = stmt.executeQuery(sql);
            Hashtable h = new Hashtable();
            while (rs.next()) {
                info.put("track_name", rs.getString("track_name"));
            }
        } else {
            info.put("track_name", "Default");
        }
        {
            info.put("isEnrolled", new Boolean(true));
            int period_level = 0;
            String parent_id = "";
            info.put("period_name", "none");
            String period_name = (String) info.get("period_name");
            while (period_level != 0) {
                r.clear();
                r.add("period_name");
                r.add("parent_id");
                r.add("period_level");
                r.add("period_id", parent_id);
                sql = r.getSQLSelect("period");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    period_name = rs.getString("period_name") + ", " + period_name;
                    parent_id = rs.getString("parent_id");
                    period_level = rs.getInt("period_level");
                }
            }
            info.put("period_name", period_name);
        }
        String period_id = SessionData.getCurrentPeriodId((String) info.get("period_scheme"), (String) info.get("intake_session"));
        info.put("period_id", period_id);
        if (period_id != null && !"".equals(period_id)) {
            if ("FINISHED".equals(period_id)) {
                info.put("period_name", "FINISHED STUDY");
            } else {
                createPeriodName(info);
            }
        } else {
            info.put("period_name", "UNDEFINED");
        }
        {
            String session_id = (String) info.get("intake_session");
            sql = "select c.centre_id, centre_code, centre_name from student s, learning_centre c " + "where s.centre_id = c.centre_id and id = '" + student_id + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                info.put("centre_id", rs.getString("centre_id"));
                info.put("centre_code", rs.getString("centre_code"));
                info.put("centre_name", rs.getString("centre_name"));
            } else {
                info.put("centre_id", "");
                info.put("centre_code", "");
                info.put("centre_name", "");
            }
        }
        return info;
    }

    private static String fmt(String s) {
        s = s.trim();
        if (s.length() == 1) return "0".concat(s); else return s;
    }

    private static void createPeriodName(Hashtable info) throws Exception {
        Db db = null;
        String sql = "";
        info.put("period_name", "");
        if (info.get("period_id") == null || "".equals((String) info.get("period_id"))) return;
        if (info.get("period_scheme") == null || "".equals((String) info.get("period_scheme"))) return;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.clear();
                r.add("period_name");
                r.add("period_level");
                r.add("parent_id");
                r.add("period_id", (String) info.get("period_id"));
                r.add("period_root_id", (String) info.get("period_scheme"));
                sql = r.getSQLSelect("period");
                int period_level = 0;
                String parent_id = "";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    info.put("period_name", rs.getString("period_name"));
                    period_level = rs.getInt("period_level");
                    parent_id = rs.getString("parent_id");
                }
                String period_name = (String) info.get("period_name");
                while (period_level != 0) {
                    r.clear();
                    r.add("period_name");
                    r.add("parent_id");
                    r.add("period_level");
                    r.add("period_id", parent_id);
                    sql = r.getSQLSelect("period");
                    rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        period_name = rs.getString("period_name") + ", " + period_name;
                        parent_id = rs.getString("parent_id");
                        period_level = rs.getInt("period_level");
                    }
                }
                info.put("period_name", period_name);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getEnrollmentInfo(String student_id, String session_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Hashtable info = new Hashtable();
            boolean enrolled = false;
            {
                r.add("c.course_id");
                r.add("c.course_code");
                r.add("c.course_name");
                r.add("sc.program_code");
                r.add("p.program_name");
                r.add("sc.period_root_id");
                r.add("sc.intake_month");
                r.add("sc.intake_year");
                r.add("sc.intake_code");
                r.add("sta.batch_id as intake_session");
                r.add("s.session_name");
                r.add("f.faculty_code");
                r.add("f.faculty_name");
                r.add("i.institution_id");
                r.add("i.institution_name");
                r.add("i.institution_abbr");
                r.add("sc.student_id", student_id);
                r.add("sta.session_id", session_id);
                r.add("c.course_id", r.unquote("sc.course_id"));
                r.add("sc.program_code", r.unquote("p.program_code"));
                r.add("c.faculty_id", r.unquote("f.faculty_id"));
                r.add("f.institution_id", r.unquote("i.institution_id"));
                r.add("sta.student_id", r.unquote("sc.student_id"));
                r.add("sta.batch_id", r.unquote("s.session_id"));
                sql = r.getSQLSelect("student_course sc, student_status sta, study_course c, session s, program p, faculty f, institution i");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    enrolled = true;
                    info.put("course_id", mecca.db.Db.getString(rs, "course_id"));
                    info.put("course_code", mecca.db.Db.getString(rs, "course_code"));
                    info.put("program_code", mecca.db.Db.getString(rs, "program_code"));
                    info.put("program_name", mecca.db.Db.getString(rs, "program_name"));
                    info.put("course_name", mecca.db.Db.getString(rs, "course_name"));
                    info.put("period_scheme", mecca.db.Db.getString(rs, "period_root_id"));
                    info.put("intake_month", new Integer(rs.getInt("intake_month")));
                    info.put("intake_year", new Integer(rs.getInt("intake_year")));
                    info.put("intake_code", mecca.db.Db.getString(rs, "intake_code"));
                    info.put("intake_session", mecca.db.Db.getString(rs, "intake_session"));
                    info.put("intake_session_name", mecca.db.Db.getString(rs, "session_name"));
                    info.put("current_period", mecca.db.Db.getString(rs, "period_id"));
                    info.put("faculty_code", mecca.db.Db.getString(rs, "faculty_code"));
                    info.put("faculty_name", mecca.db.Db.getString(rs, "faculty_name"));
                    info.put("institution_id", mecca.db.Db.getString(rs, "institution_id"));
                    info.put("institution_name", mecca.db.Db.getString(rs, "institution_name"));
                    info.put("institution_abbr", mecca.db.Db.getString(rs, "institution_abbr"));
                }
            }
            if (enrolled) {
                info.put("period_id", (String) info.get("current_period"));
                createPeriodName(info);
            }
            {
                sql = "select c.centre_id, centre_code, centre_name from student s, learning_centre c " + "where s.centre_id = c.centre_id and id = '" + student_id + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    info.put("centre_id", rs.getString("centre_id"));
                    info.put("centre_code", rs.getString("centre_code"));
                    info.put("centre_name", rs.getString("centre_name"));
                } else {
                    info.put("centre_id", "");
                    info.put("centre_code", "");
                    info.put("centre_name", "");
                }
            }
            return info;
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getStudent(String student) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("id", student);
            r.add("name");
            sql = r.getSQLSelect("student");
            ResultSet rs = stmt.executeQuery(sql);
            Hashtable h = new Hashtable();
            if (rs.next()) {
                h.put("id", student);
                h.put("name", rs.getString("name"));
            }
            return h;
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector getStudentList(String session_id, String program_code) throws Exception {
        Db db = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            return getStudentList(stmt, session_id, program_code);
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector getStudentList(Statement stmt, String session_id, String program_code) throws Exception {
        String sql = "";
        sql = "SELECT " + "st.id, st.name, st.address, " + "st.city, st.state, st.poscode, st.country_code, st.phone, " + "st.birth_date, st.email, p.period_id, prog.program_code, " + "prog.program_name, p.period_root_id, ses.session_name, ses2.session_name as intake_name " + "FROM student st, student_course sc, student_status sta, intake_batch b, " + "period p, program prog, session ses, session ses2 " + "WHERE " + "st.id = sc.student_id " + "AND st.id = sta.student_id " + "AND b.session_id = sta.session_id " + "AND b.intake_session = sta.batch_id  " + "AND b.period_id = p.period_id  " + "AND b.period_root_id = p.period_root_id " + "AND b.period_root_id = sc.period_root_id " + "AND p.period_root_id = sc.period_root_id " + "AND prog.program_code = sc.program_code  " + "AND sta.batch_id = ses.session_id  " + "AND sc.intake_session = ses2.session_id ";
        if (!"".equals(program_code)) sql += "AND prog.program_code = '" + program_code + "' ";
        if (!"".equals(session_id)) sql += "AND sta.session_id = '" + session_id + "' ";
        sql += "ORDER BY st.name";
        ResultSet rs = stmt.executeQuery(sql);
        Vector v = new Vector();
        while (rs.next()) {
            Hashtable h = new Hashtable();
            h.put("id", rs.getString("id"));
            h.put("name", rs.getString("name"));
            h.put("period_id", rs.getString("period_id"));
            v.addElement(h);
        }
        return v;
    }

    public static Vector getStudentList(Statement stmt, String session_id, String program_code, String period_scheme) throws Exception {
        return getStudentList(stmt, session_id, program_code, period_scheme, "");
    }

    public static Vector getStudentList(Statement stmt, String session_id, String program_code, String period_scheme, String intake_session) throws Exception {
        String sql = "";
        sql = "SELECT " + "st.id, st.name, st.address, " + "st.city, st.state, st.poscode, st.country_code, st.phone, " + "st.birth_date, st.email, p.period_id, prog.program_code, " + "prog.program_name, p.period_root_id, ses.session_name, ses2.session_name as intake_name " + "FROM student st, student_course sc, student_status sta, intake_batch b, " + "period p, program prog, session ses, session ses2 " + "WHERE " + "st.id = sc.student_id " + "AND st.id = sta.student_id " + "AND b.session_id = sta.session_id " + "AND b.intake_session = sta.batch_id  " + "AND b.period_id = p.period_id  " + "AND b.period_root_id = p.period_root_id " + "AND b.period_root_id = sc.period_root_id " + "AND p.period_root_id = sc.period_root_id " + "AND prog.program_code = sc.program_code  " + "AND sta.batch_id = ses.session_id  " + "AND sc.intake_session = ses2.session_id ";
        if (!"".equals(intake_session)) sql += "AND sc.intake_session = '" + intake_session + "' ";
        if (!"".equals(program_code)) sql += "AND prog.program_code = '" + program_code + "' ";
        if (!"".equals(session_id)) sql += "AND sta.session_id = '" + session_id + "' ";
        sql += "AND sc.period_root_id = '" + period_scheme + "' " + "ORDER BY st.name";
        ResultSet rs = stmt.executeQuery(sql);
        Vector v = new Vector();
        while (rs.next()) {
            Hashtable h = new Hashtable();
            h.put("id", rs.getString("id"));
            h.put("name", rs.getString("name"));
            h.put("period_id", rs.getString("period_id"));
            v.addElement(h);
        }
        return v;
    }

    public static Vector getStudentListAlternateTrack(Statement stmt, String period_scheme, String session_id, String program_code) throws Exception {
        String sql = "";
        sql = "SELECT st.id, st.name, p.period_id " + "FROM student st, student_course sc, student_status sta,  " + "intake_batch b, period p, program prog, program_track track,  " + "session ses, session ses2  " + "WHERE st.id = sc.student_id  " + "AND st.id = sta.student_id  " + "AND b.session_id = sta.session_id  " + "AND b.intake_session = sta.batch_id   " + "AND b.period_id = p.period_id   " + "AND b.period_root_id = p.period_root_id  " + "AND b.period_root_id = sc.period_root_id  " + "AND sc.period_root_id = track.period_root_id " + "AND prog.program_code = sc.program_code   " + "AND track.program_code = prog.program_code " + "AND sta.batch_id = ses.session_id   " + "AND sc.intake_session = ses2.session_id  ";
        if (!"".equals(program_code)) sql += "AND prog.program_code = '" + program_code + "' ";
        if (!"".equals(session_id)) sql += "AND sta.session_id = '" + session_id + "' ";
        sql += "AND track.period_root_id = '" + period_scheme + "' " + "ORDER BY st.name";
        System.out.println(sql);
        ResultSet rs = stmt.executeQuery(sql);
        Vector v = new Vector();
        while (rs.next()) {
            Hashtable h = new Hashtable();
            h.put("id", rs.getString("id"));
            h.put("name", rs.getString("name"));
            h.put("period_id", rs.getString("period_id"));
            v.addElement(h);
        }
        return v;
    }

    public static Vector getSubjectList(String studentId, String periodId, String programCode) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("ss.subject_id");
            r.add("fs.subject_code");
            r.add("fs.subject_name");
            r.add("fs.credit_hrs");
            r.add("ss.register_id", "active");
            r.add("ss.student_id", studentId);
            r.add("ss.period_id", periodId);
            r.add("ss.program_code", programCode);
            r.add("ss.subject_id", r.unquote("fs.subject_id"));
            sql = r.getSQLSelect("student_subject ss, faculty_subject fs");
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                Subject s = new Subject();
                s.setId(rs.getString("subject_id"));
                s.setCode(rs.getString("subject_code"));
                s.setName(rs.getString("subject_name"));
                s.setCreditHours(rs.getInt("credit_hrs"));
                v.addElement(s);
            }
            return v;
        } finally {
            if (db != null) db.close();
        }
    }

    public static void deleteSubjectLMS(String student_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "delete from member_subject where member_id = '" + student_id + "'";
            stmt.executeUpdate(sql);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void addSubjectLMS(Subject subject, String student_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            found = false;
            {
                r.clear();
                r.add("subject_code");
                r.add("subject_id", subject.getId());
                sql = r.getSQLSelect("subject");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                {
                    r.clear();
                    r.add("subject_id", subject.getId());
                    r.add("subject_code", subject.getCode());
                    r.add("subject_title", subject.getName());
                    r.add("module_id", "");
                    r.add("subject_comment", "");
                    r.add("subject_text", "");
                    sql = r.getSQLInsert("subject");
                    stmt.executeUpdate(sql);
                }
            }
            {
                r.clear();
                r.add("subject_id");
                r.add("member_id", student_id);
                r.add("subject_id", subject.getId());
                sql = r.getSQLSelect("member_subject");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true; else found = false;
            }
            if (!found) {
                r.clear();
                r.add("member_id", student_id);
                r.add("subject_id", subject.getId());
                r.add("role", "learner");
                r.add("status", "active");
                r.add("module_id", "");
                sql = r.getSQLInsert("member_subject");
                stmt.executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getSubjectList(String student_id, String period_id, Hashtable info) throws Exception {
        String course_id = (String) info.get("course_id");
        String period_scheme_id = (String) info.get("period_scheme");
        String program_code = (String) info.get("program_code");
        String intake_session = (String) info.get("intake_session");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Vector coreSubjects = new Vector();
            {
                r.add("subject_id");
                r.add("program_code", program_code);
                r.add("course_id", course_id);
                r.add("period_root_id", period_scheme_id);
                r.add("period_id", period_id);
                r.add("subject_option", "c");
                sql = r.getSQLSelect("course_structure");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    coreSubjects.addElement(rs.getString("subject_id"));
                }
            }
            Vector activeSubjects = new Vector();
            Vector exemptedSubjects = new Vector();
            Vector withdrawSubjects = new Vector();
            {
                r.clear();
                r.add("fs.subject_id");
                r.add("fs.subject_code");
                r.add("fs.subject_name");
                r.add("fs.credit_hrs");
                r.add("ss.register_id");
                r.add("sr.status_name");
                r.add("ss.student_id", student_id);
                r.add("ss.program_code", program_code);
                r.add("ss.course_id", course_id);
                r.add("ss.period_root_id", period_scheme_id);
                r.add("ss.period_id", period_id);
                r.add("ss.period_id", r.unquote("p.period_id"));
                r.add("fs.subject_id", r.unquote("ss.subject_id"));
                r.add("ss.register_id", r.unquote("sr.status_id"));
                sql = r.getSQLSelect("student_subject ss, subject_reg_status sr, faculty_subject fs, period p");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    String subject_id = rs.getString("subject_id");
                    String subject_option = "";
                    h.put("subject_id", subject_id);
                    if (coreSubjects.contains(subject_id)) subject_option = "c";
                    h.put("subject_code", rs.getString("subject_code"));
                    h.put("subject_name", rs.getString("subject_name"));
                    h.put("credit_hrs", rs.getString("credit_hrs"));
                    h.put("subject_option", subject_option);
                    String registerId = rs.getString("register_id");
                    h.put("status_name", rs.getString("status_name"));
                    h.put("status_id", registerId != null ? registerId : "active");
                    if (SubjectStatus.getCategory(registerId) == 0) {
                        activeSubjects.addElement(h);
                    } else if (SubjectStatus.getCategory(registerId) == 1) {
                        exemptedSubjects.addElement(h);
                    } else if (SubjectStatus.getCategory(registerId) == 2) {
                        withdrawSubjects.addElement(h);
                    }
                }
                Hashtable subjectGroup = new Hashtable();
                subjectGroup.put("active", activeSubjects);
                subjectGroup.put("exempted", exemptedSubjects);
                subjectGroup.put("withdraw", withdrawSubjects);
                return subjectGroup;
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector getIntakeStatus(String student_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Hashtable statusList = new Hashtable();
            {
                sql = "select status_id, status_name from study_status";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    statusList.put(rs.getString(1), rs.getString(2));
                }
            }
            Vector v = new Vector();
            {
                r.clear();
                r.add("sta.student_id", student_id);
                r.add("sta.session_id", r.unquote("ses.session_id"));
                r.add("sta.batch_id", r.unquote("ses2.session_id"));
                r.add("sc.student_id", r.unquote("sta.student_id"));
                r.add("sta.session_id");
                r.add("sta.batch_id");
                r.add("sta.repeat_no");
                r.add("sta.status");
                r.add("ses.session_name");
                r.add("ses2.session_name as batch_session");
                r.add("sc.period_root_id as period_scheme");
                sql = r.getSQLSelect("student_course sc, student_status sta, session ses, session ses2", "ses.start_date");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    h.put("session_id", rs.getString("session_id"));
                    h.put("batch_id", rs.getString("batch_id"));
                    h.put("repeat_no", new Integer(rs.getInt("repeat_no")));
                    String s = rs.getString("status");
                    h.put("status_id", s != null ? s : "active");
                    s = statusList.get(s) != null ? (String) statusList.get(s) : "ACTIVE";
                    h.put("status_name", s);
                    h.put("session_name", rs.getString("session_name"));
                    h.put("batch_session", rs.getString("batch_session"));
                    h.put("period_scheme", rs.getString("period_scheme"));
                    v.addElement(h);
                }
            }
            for (int i = 0; i < v.size(); i++) {
                Hashtable h = (Hashtable) v.elementAt(i);
                String period_scheme = (String) h.get("period_scheme");
                String batch_id = (String) h.get("batch_id");
                String session_id = (String) h.get("session_id");
                sql = "select i.period_id " + "from intake_batch i " + "where i.period_root_id = '" + period_scheme + "' " + "and i.intake_session = '" + batch_id + "' " + "and i.session_id = '" + session_id + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String period_id = rs.getString(1);
                    h.put("period_id", period_id);
                    createPeriodName(h);
                }
            }
            boolean canSetRepeat = true;
            for (int i = v.size() - 1; i > -1; i--) {
                Hashtable h = (Hashtable) v.elementAt(i);
                int repeat_no = ((Integer) h.get("repeat_no")).intValue();
                h.put("canSetRepeat", new Boolean(canSetRepeat));
                if (repeat_no > 0) {
                    canSetRepeat = false;
                }
            }
            return v;
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getSessionPeriodMap(String student_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Hashtable statusList = new Hashtable();
            {
                sql = "select status_id, status_name from study_status";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    statusList.put(rs.getString(1), rs.getString(2));
                }
            }
            Vector v = new Vector();
            {
                r.clear();
                r.add("sta.student_id", student_id);
                r.add("sta.session_id", r.unquote("ses.session_id"));
                r.add("sta.batch_id", r.unquote("ses2.session_id"));
                r.add("sc.student_id", r.unquote("sta.student_id"));
                r.add("sta.session_id");
                r.add("sta.batch_id");
                r.add("sta.repeat_no");
                r.add("sta.status");
                r.add("ses.session_name");
                r.add("ses2.session_name as batch_session");
                r.add("sc.period_root_id as period_scheme");
                sql = r.getSQLSelect("student_course sc, student_status sta, session ses, session ses2", "ses.start_date");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    h.put("session_id", rs.getString("session_id"));
                    h.put("batch_id", rs.getString("batch_id"));
                    h.put("repeat_no", new Integer(rs.getInt("repeat_no")));
                    String s = rs.getString("status");
                    h.put("status_id", s != null ? s : "active");
                    s = statusList.get(s) != null ? (String) statusList.get(s) : "ACTIVE";
                    h.put("status_name", s);
                    h.put("session_name", rs.getString("session_name"));
                    h.put("batch_session", rs.getString("batch_session"));
                    h.put("period_scheme", rs.getString("period_scheme"));
                    v.addElement(h);
                }
            }
            Hashtable map = new Hashtable();
            for (int i = 0; i < v.size(); i++) {
                Hashtable h = (Hashtable) v.elementAt(i);
                String period_scheme = (String) h.get("period_scheme");
                String batch_id = (String) h.get("batch_id");
                String session_id = (String) h.get("session_id");
                sql = "select i.period_id " + "from intake_batch i " + "where i.period_root_id = '" + period_scheme + "' " + "and i.intake_session = '" + batch_id + "' " + "and i.session_id = '" + session_id + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String period_id = rs.getString(1);
                    map.put(session_id, period_id);
                }
            }
            return map;
        } finally {
            if (db != null) db.close();
        }
    }

    public static Vector getAvailableSessions(String student_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Hashtable statusList = new Hashtable();
            {
                sql = "select status_id, status_name from study_status";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    statusList.put(rs.getString(1), rs.getString(2));
                }
            }
            Vector v = new Vector();
            {
                r.clear();
                r.add("sta.student_id", student_id);
                r.add("sta.session_id", r.unquote("ses.session_id"));
                r.add("sta.batch_id", r.unquote("ses2.session_id"));
                r.add("sc.student_id", r.unquote("sta.student_id"));
                r.add("sta.session_id");
                r.add("sta.batch_id");
                r.add("sta.repeat_no");
                r.add("sta.status");
                r.add("ses.session_name");
                r.add("ses2.session_name as batch_session");
                r.add("sc.period_root_id as period_scheme");
                sql = r.getSQLSelect("student_course sc, student_status sta, session ses, session ses2", "ses.start_date");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String period_scheme = rs.getString("period_scheme");
                    String session_id = rs.getString("session_id");
                    String session_name = rs.getString("session_name");
                    String batch_id = rs.getString("batch_id");
                    Hashtable h = new Hashtable();
                    h.put("period_scheme", period_scheme);
                    h.put("session_id", session_id);
                    h.put("session_name", session_name);
                    h.put("batch_id", batch_id);
                    h.put("repeat_no", new Integer(rs.getInt("repeat_no")));
                    v.addElement(h);
                }
            }
            for (int i = 0; i < v.size(); i++) {
                Hashtable h = (Hashtable) v.elementAt(i);
                String period_scheme = (String) h.get("period_scheme");
                String batch_id = (String) h.get("batch_id");
                String session_id = (String) h.get("session_id");
                sql = "select i.period_id " + "from intake_batch i " + "where i.period_root_id = '" + period_scheme + "' " + "and i.intake_session = '" + batch_id + "' " + "and i.session_id = '" + session_id + "' ";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    String period_id = rs.getString(1);
                    h.put("period_id", period_id);
                    createPeriodName(h);
                }
            }
            boolean canSetRepeat = true;
            for (int i = v.size() - 1; i > -1; i--) {
                Hashtable h = (Hashtable) v.elementAt(i);
                int repeat_no = ((Integer) h.get("repeat_no")).intValue();
                h.put("canSetRepeat", new Boolean(canSetRepeat));
                if (repeat_no > 0) {
                    canSetRepeat = false;
                }
            }
            return v;
        } finally {
            if (db != null) db.close();
        }
    }

    public static void registerProgram(Hashtable info) throws Exception {
        String student_id = (String) info.get("student_id");
        String track_id = (String) info.get("track_id");
        if (track_id == null || "".equals(track_id)) track_id = "0";
        String program_code = (String) info.get("program_code");
        Object obj_intake_month = info.get("intake_month");
        Object obj_intake_year = info.get("intake_year");
        int intake_month = 0;
        int intake_year = 0;
        if (obj_intake_month instanceof Integer) intake_month = ((Integer) obj_intake_month).intValue(); else if (obj_intake_month instanceof String) intake_month = Integer.parseInt((String) obj_intake_month);
        if (obj_intake_year instanceof Integer) intake_year = ((Integer) obj_intake_year).intValue(); else if (obj_intake_year instanceof String) intake_year = Integer.parseInt((String) obj_intake_year);
        String intake_session = IntakeStatus.getSessionId(intake_year, intake_month);
        String course_id = "";
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.clear();
                r.add("program_code", program_code);
                r.add("course_id");
                sql = r.getSQLSelect("program");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) course_id = rs.getString("course_id");
                info.put("course_id", course_id);
            }
            String periodId = "";
            {
                if (!"0".equals(track_id)) {
                    r.clear();
                    r.add("period_root_id");
                    r.add("program_code", program_code);
                    r.add("track_id", track_id);
                    sql = r.getSQLSelect("program_track");
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        periodId = rs.getString("period_root_id");
                    }
                }
            }
            if ("".equals(periodId)) {
                r.clear();
                r.add("period_root_id");
                r.add("program_code", program_code);
                sql = r.getSQLSelect("program");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    periodId = rs.getString("period_root_id");
                }
            }
            boolean found = false;
            {
                r.clear();
                r.add("student_id");
                r.add("student_id", student_id);
                sql = r.getSQLSelect("student_course");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            String code_month = "", code_year = "";
            code_month = intake_month < 10 ? "0" + intake_month : "" + intake_month;
            code_year = Integer.toString(intake_year).substring(2);
            String intake_code = code_year + code_month;
            if (!found) {
                r.clear();
                r.add("intake_session", intake_session);
                r.add("period_root_id", periodId);
                r.add("track_id", track_id);
                r.add("student_id", student_id);
                r.add("program_code", program_code);
                r.add("course_id", course_id);
                r.add("intake_month", intake_month);
                r.add("intake_year", intake_year);
                r.add("intake_code", intake_code);
                sql = r.getSQLInsert("student_course");
                stmt.executeUpdate(sql);
            } else {
                r.clear();
                r.add("intake_session", intake_session);
                r.add("period_root_id", periodId);
                r.add("track_id", track_id);
                r.add("program_code", program_code);
                r.add("course_id", course_id);
                r.update("student_id", student_id);
                r.add("intake_month", intake_month);
                r.add("intake_year", intake_year);
                r.add("intake_code", intake_code);
                sql = r.getSQLUpdate("student_course");
                stmt.executeUpdate(sql);
            }
            info.put("intake_session", intake_session);
            IntakeStatus.doUpdate(student_id, intake_session, periodId);
            tabulateAllSubjectForStudent(info);
        } finally {
            if (db != null) db.close();
        }
    }

    public static void registerProgram(StudentInfo student) throws Exception {
        String student_id = student.getId();
        String track_id = student.getTrackId();
        if (track_id == null || "".equals(track_id)) track_id = "0";
        String program_code = student.getProgramCode();
        int intake_month = student.getIntakeMonth();
        int intake_year = student.getIntakeYear();
        String intake_code = student.getIntakeCode();
        String intake_session = student.getIntakeSessionId();
        String course_id = "";
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.clear();
                r.add("program_code", program_code);
                r.add("course_id");
                sql = r.getSQLSelect("program");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) course_id = rs.getString("course_id");
                student.setCourseId(course_id);
            }
            String periodId = "";
            {
                if (!"0".equals(track_id)) {
                    r.clear();
                    r.add("period_root_id");
                    r.add("program_code", program_code);
                    r.add("track_id", track_id);
                    sql = r.getSQLSelect("program_track");
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        periodId = rs.getString("period_root_id");
                    }
                }
            }
            if ("".equals(periodId)) {
                r.clear();
                r.add("period_root_id");
                r.add("program_code", program_code);
                sql = r.getSQLSelect("program");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    periodId = rs.getString("period_root_id");
                }
            }
            boolean found = false;
            {
                r.clear();
                r.add("student_id");
                r.add("student_id", student_id);
                sql = r.getSQLSelect("student_course");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                r.clear();
                r.add("intake_session", intake_session);
                r.add("period_root_id", periodId);
                r.add("track_id", track_id);
                r.add("student_id", student_id);
                r.add("program_code", program_code);
                r.add("course_id", course_id);
                r.add("intake_month", intake_month);
                r.add("intake_year", intake_year);
                r.add("intake_code", intake_code);
                sql = r.getSQLInsert("student_course");
                stmt.executeUpdate(sql);
            } else {
                r.clear();
                r.add("intake_session", intake_session);
                r.add("period_root_id", periodId);
                r.add("track_id", track_id);
                r.add("program_code", program_code);
                r.add("course_id", course_id);
                r.update("student_id", student_id);
                r.add("intake_month", intake_month);
                r.add("intake_year", intake_year);
                r.add("intake_code", intake_code);
                sql = r.getSQLUpdate("student_course");
                stmt.executeUpdate(sql);
            }
            IntakeStatus.doUpdate(student_id, intake_session, periodId);
            tabulateAllSubjectForStudent(student);
        } finally {
            if (db != null) db.close();
        }
    }

    private static void tabulateAllSubjectForStudent(Hashtable info) throws Exception {
        String student_id = (String) info.get("student_id");
        String course_id = (String) info.get("course_id");
        String period_root = (String) info.get("period_scheme");
        String program_code = (String) info.get("program_code");
        StudentInfo student = new StudentInfo();
        student.setId(student_id);
        student.setCourseId(course_id);
        student.setPeriodId(period_root);
        student.setProgramCode(program_code);
        tabulateAllSubjectForStudent(student);
    }

    private static void tabulateAllSubjectForStudent(StudentInfo info) throws Exception {
        String student_id = info.getId();
        String course_id = info.getCourseId();
        String period_root = info.getPeriodId();
        String program_code = info.getProgramCode();
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.clear();
                r.add("period_root_id");
                r.add("program_code", program_code);
                sql = r.getSQLSelect("program");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) period_root = rs.getString("period_root_id");
            }
            Vector periodIds = new Vector();
            {
                r.clear();
                r.add("period_root_id", period_root);
                r.add("period_id");
                sql = r.getSQLSelect("period", "period_sequence");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    periodIds.addElement(rs.getString("period_id"));
                }
            }
            Hashtable subjectIds = new Hashtable();
            for (int i = 0; i < periodIds.size(); i++) {
                String periodId = (String) periodIds.elementAt(i);
                r.clear();
                r.add("course_id", course_id);
                r.add("program_code", program_code);
                r.add("period_root_id", period_root);
                r.add("period_id", periodId);
                r.add("subject_id");
                sql = r.getSQLSelect("course_structure");
                ResultSet rs = stmt.executeQuery(sql);
                Vector subjects = new Vector();
                while (rs.next()) {
                    String s = rs.getString("subject_id");
                    subjects.addElement(s);
                }
                subjectIds.put(periodId, subjects);
            }
            {
                r.clear();
                r.add("student_id", student_id);
                r.add("period_root_id", period_root);
                r.add("course_id", course_id);
                r.add("program_code", program_code);
                sql = r.getSQLDelete("student_subject");
                stmt.executeUpdate(sql);
            }
            for (int i = 0; i < periodIds.size(); i++) {
                String periodId = (String) periodIds.elementAt(i);
                Vector subjects = (Vector) subjectIds.get(periodId);
                for (int k = 0; k < subjects.size(); k++) {
                    String subjectId = (String) subjects.elementAt(k);
                    r.clear();
                    r.add("student_id", student_id);
                    r.add("period_root_id", period_root);
                    r.add("period_id", periodId);
                    r.add("course_id", course_id);
                    r.add("program_code", program_code);
                    r.add("subject_id", subjectId);
                    r.add("register_id", "active");
                    sql = r.getSQLInsert("student_subject");
                    stmt.executeUpdate(sql);
                }
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static Hashtable getStudentBiodata(String id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("id");
            r.add("applicant_id");
            r.add("password");
            r.add("name");
            r.add("icno");
            r.add("address");
            r.add("address1");
            r.add("address2");
            r.add("address3");
            r.add("city");
            r.add("state");
            r.add("poscode");
            r.add("country_code");
            r.add("email");
            r.add("phone");
            r.add("gender");
            r.add("birth_date");
            r.add("intake_session");
            r.add("centre_id");
            r.add("id", id);
            sql = r.getSQLSelect("student");
            ResultSet rs = stmt.executeQuery(sql);
            Hashtable h = null;
            if (rs.next()) {
                h = getStudentData(rs);
            }
            if (h != null) {
            } else {
                h = new Hashtable();
            }
            return h;
        } catch (DbException dbex) {
            throw dbex;
        } catch (SQLException sqlex) {
            throw sqlex;
        } finally {
            if (db != null) db.close();
        }
    }

    private static Hashtable getStudentData(ResultSet rs) throws Exception {
        Hashtable studentInfo = new Hashtable();
        studentInfo.put("student_id", Db.getString(rs, "id"));
        studentInfo.put("applicant_id", Db.getString(rs, "applicant_id"));
        studentInfo.put("password", Db.getString(rs, "password"));
        studentInfo.put("name", Db.getString(rs, "name"));
        studentInfo.put("icno", Db.getString(rs, "icno"));
        studentInfo.put("address", Db.getString(rs, "address"));
        studentInfo.put("address1", Db.getString(rs, "address1"));
        studentInfo.put("address2", Db.getString(rs, "address2"));
        studentInfo.put("address3", Db.getString(rs, "address3"));
        studentInfo.put("city", Db.getString(rs, "city"));
        studentInfo.put("state", Db.getString(rs, "state"));
        studentInfo.put("poscode", Db.getString(rs, "poscode"));
        studentInfo.put("country_code", Db.getString(rs, "country_code"));
        studentInfo.put("email", Db.getString(rs, "email"));
        studentInfo.put("gender", Db.getString(rs, "gender"));
        studentInfo.put("phone", Db.getString(rs, "phone"));
        studentInfo.put("birth_date", Db.getString(rs, "birth_date"));
        java.util.Date birthDate = rs.getDate("birth_date");
        int year = 0, month = 0, day = 0;
        if (birthDate != null) {
            Calendar c = new GregorianCalendar();
            c.setTime(birthDate);
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH) + 1;
            day = c.get(Calendar.DAY_OF_MONTH);
        }
        studentInfo.put("birth_year", new Integer(year));
        studentInfo.put("birth_month", new Integer(month));
        studentInfo.put("birth_day", new Integer(day));
        studentInfo.put("centre_id", Db.getString(rs, "centre_id"));
        studentInfo.put("intake_session", Db.getString(rs, "intake_session"));
        return studentInfo;
    }
}

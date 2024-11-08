package educate.sis.registration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;
import educate.sis.struct.PeriodData;
import educate.sis.struct.ProgramData;
import educate.sis.struct.Subject;
import lebah.db.DataHelper;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import lebah.general.CodeData;
import lebah.util.DateTool;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class StudentData {

    public static void savePhoto(final String studentId, final String photoFileName) throws Exception {
        new DataHelper() {

            public String doSQL() {
                SQLRenderer r = new SQLRenderer();
                return r.update("id", studentId).add("photoFileName", photoFileName).getSQLUpdate("student");
            }
        }.execute();
    }

    public static Biodata getBiodata(String student_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("b.phone_no_permanent");
            r.add("b.phone_no");
            r.add("b.phone_mobile");
            r.add("disability");
            r.add("g.code");
            r.add("g.gender_name");
            r.add("m.code");
            r.add("m.status_name");
            r.add("re.code");
            r.add("re.religion_name");
            r.add("r.code");
            r.add("r.race_name");
            r.add("n.code");
            r.add("n.nationality_name");
            r.add("b.birth_date");
            r.add("b.icno");
            r.add("b.guardian_name");
            r.add("b.guardian_address1");
            r.add("b.guardian_address2");
            r.add("s.name");
            r.add("b.student_id", student_id);
            r.add("g.code", r.unquote("b.gender"));
            r.add("m.code", r.unquote("b.marriage_status"));
            r.add("re.code", r.unquote("b.religion"));
            r.add("r.code", r.unquote("b.race"));
            r.add("n.code", r.unquote("b.nationality"));
            r.add("b.student_id", r.unquote("s.id"));
            sql = r.getSQLSelect("student s,student_biodata b, race_code r, nationality_code n, religion_code re, marital_code m, gender_code g");
            ResultSet rs = stmt.executeQuery(sql);
            Biodata biodata = new Biodata();
            if (rs.next()) {
                biodata.setPhoneNumberPermanent(rs.getString(1));
                biodata.setPhoneNumber(rs.getString(2));
                biodata.setPhoneMobile(rs.getString(3));
                String disability = rs.getString(4);
                biodata.setDisability("1".equals(disability) ? false : true);
                Code gender = new GenderCode();
                gender.setCode(rs.getString(5));
                gender.setName(rs.getString(6));
                biodata.setGender(gender);
                Code marital = new MaritalCode();
                marital.setCode(rs.getString(7));
                marital.setName(rs.getString(8));
                biodata.setMaritalStatus(marital);
                Code religion = new ReligionCode();
                religion.setCode(rs.getString(9));
                religion.setName(rs.getString(10));
                biodata.setReligion(religion);
                Code race = new RaceCode();
                race.setCode(rs.getString(11));
                race.setName(rs.getString(12));
                biodata.setRace(race);
                Code nationality = new NationalityCode();
                nationality.setCode(rs.getString(13));
                nationality.setName(rs.getString(14));
                biodata.setNationality(nationality);
                biodata.setGuardianName(rs.getString("guardian_name"));
                biodata.setGuardianAddress1(rs.getString("guardian_address1"));
                biodata.setGuardianAddress2(rs.getString("guardian_address2"));
                biodata.setBirthDate(Db.getDate(rs, "birth_date"));
                biodata.setIcno(Db.getString(rs, "icno"));
                biodata.setName(Db.getString(rs, "name"));
            } else {
            }
            return biodata;
        } finally {
            if (db != null) db.close();
        }
    }

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
                r.add("city", getInfoData(studentInfo, "city"));
                r.add("state", getInfoData(studentInfo, "state"));
                r.add("poscode", getInfoData(studentInfo, "poscode"));
                r.add("country_code", getInfoData(studentInfo, "country_code"));
                r.add("phone", getInfoData(studentInfo, "phone"));
                r.add("birth_date", getInfoData(studentInfo, "birth_date"));
                r.add("email", getInfoData(studentInfo, "email"));
                r.add("nationality", getInfoData(studentInfo, "nationality"));
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
            saveBiodata(studentInfo, stmt);
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

    public static void saveBiodata(Hashtable studentInfo, Statement stmt) throws SQLException {
        String sql;
        boolean found;
        found = false;
        {
            sql = "select student_id from student_biodata where student_id = '" + (String) studentInfo.get("student_id") + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) found = true;
        }
        SQLRenderer r = new SQLRenderer();
        {
            r.clear();
            r.add("gender", getInfoData(studentInfo, "gender"));
            r.add("marriage_status", getInfoData(studentInfo, "marital"));
            r.add("religion", getInfoData(studentInfo, "religion"));
            r.add("race", getInfoData(studentInfo, "race"));
            r.add("nationality", getInfoData(studentInfo, "nationality"));
            r.add("icno", getInfoData(studentInfo, "icno"));
            String birth_date = getInfoData(studentInfo, "birth_date");
            System.out.println("birthdate=" + birth_date);
            if (birth_date != null && !"".equals(birth_date)) r.add("birth_date", birth_date);
            r.add("phone_no_permanent", getInfoData(studentInfo, "phone_no_permanent"));
            r.add("phone_no", getInfoData(studentInfo, "phone_no"));
            r.add("phone_mobile", getInfoData(studentInfo, "phone_mobile"));
            r.add("guardian_name", getInfoData(studentInfo, "guardian_name"));
            r.add("guardian_address1", getInfoData(studentInfo, "guardian_address1"));
            r.add("guardian_address2", getInfoData(studentInfo, "guardian_address2"));
            r.add("disability", getInfoData(studentInfo, "disability"));
            if (!found) {
                r.add("student_id", (String) studentInfo.get("student_id"));
                sql = r.getSQLInsert("student_biodata");
                stmt.executeUpdate(sql);
            } else {
                r.update("student_id", (String) studentInfo.get("student_id"));
                sql = r.getSQLUpdate("student_biodata");
                stmt.executeUpdate(sql);
            }
        }
    }

    private static String getInfoData(Hashtable h, String field) {
        if (h.get(field) != null) return (String) h.get(field); else return "";
    }

    public static void createPortalLogin(String login, String password, String name) throws Exception {
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
                    r.add("user_password", lebah.util.PasswordService.encrypt(password));
                    r.add("user_name", name);
                    r.add("user_role", "student");
                    sql = r.getSQLInsert("users");
                    stmt.executeUpdate(sql);
                }
                {
                    String css_name = "default.css";
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
                            sql = r.getSQLInsert("tabs");
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
                            h.put("module_custom_title", lebah.db.Db.getString(rs, "module_custom_title"));
                            String coln = lebah.db.Db.getString(rs, "column_number");
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
        String session_id = "";
        boolean isExist = false;
        Db db = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            String sql = "";
            String program_code = "";
            {
                r.clear();
                r.add("student_id", student_id);
                r.add("program_code");
                sql = r.getSQLSelect("student_course");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) program_code = rs.getString(1);
            }
            {
                session_id = SessionData.getCurrentSessionId(stmt, program_code);
            }
            {
                sql = "select batch_id from student_status where session_id = '" + session_id + "' " + "and student_id = '" + student_id + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) isExist = true;
            }
        } finally {
            if (db != null) db.close();
        }
        if (isExist) return getEnrollmentInfo1(student_id, session_id); else return getEnrollmentInfo2(student_id);
    }

    public static Hashtable getEnrollmentInfo(Statement stmt, String student_id) throws Exception {
        String session_id = "";
        String sql = "";
        String program_code = "";
        SQLRenderer r = new SQLRenderer();
        {
            r.clear();
            r.add("student_id", student_id);
            r.add("program_code");
            sql = r.getSQLSelect("student_course");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) program_code = rs.getString(1);
        }
        {
            session_id = SessionData.getCurrentSessionId(stmt, program_code);
        }
        sql = "select batch_id from student_status where session_id = '" + session_id + "' " + "and student_id = '" + student_id + "'";
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return getEnrollmentInfo1(stmt, student_id, session_id);
        } else {
            return getEnrollmentInfo2(stmt, student_id);
        }
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
        Hashtable<String, Object> info = new Hashtable<String, Object>();
        info.put("isEnrolled", new Boolean(false));
        String sql = "";
        SQLRenderer r = new SQLRenderer();
        boolean enrolled = true;
        info.put("openSession", false);
        String period_id = "";
        {
            r.add("stu.name as student_name");
            r.add("stu.icno");
            r.add("stu.applicant_id");
            r.add("stu.currency_type");
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
            r.add("bio.nationality");
            r.add("bio.phone_no_permanent");
            r.add("bio.phone_no");
            r.add("bio.phone_mobile");
            r.add("stu.photoFileName");
            r.add("sc.student_id", r.unquote("stu.id"));
            r.add("c.course_id", r.unquote("sc.course_id"));
            r.add("sc.program_code", r.unquote("p.program_code"));
            r.add("c.faculty_id", r.unquote("f.faculty_id"));
            r.add("f.institution_id", r.unquote("i.institution_id"));
            r.add("sta.student_id", r.unquote("bio.student_id"));
            r.add("sta.student_id", r.unquote("sc.student_id"));
            r.add("sta.batch_id", r.unquote("s.session_id"));
            r.add("sc.intake_session", r.unquote("s2.session_id"));
            r.add("b.session_id", r.unquote("sta.session_id"));
            r.add("b.intake_session", r.unquote("sta.batch_id"));
            r.add("b.period_root_id", r.unquote("sc.period_root_id"));
            r.add("sc.student_id", student_id);
            r.add("sta.session_id", session_id);
            sql = r.getSQLSelect("student stu, " + "student_course sc, " + "student_status sta, " + "student_biodata bio, " + "study_course c, " + "intake_batch b, " + "sessions s, " + "sessions s2, " + "program p, " + "faculty f, " + "institution i");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                info.put("id", student_id);
                info.put("student_id", student_id);
                info.put("applicant_id", lebah.db.Db.getString(rs, "applicant_id"));
                info.put("currency_type", rs.getString("currency_type") != null ? rs.getString("currency_type") : "local");
                info.put("student_name", rs.getString("student_name"));
                info.put("icno", Db.getString(rs, "icno"));
                info.put("course_id", lebah.db.Db.getString(rs, "course_id"));
                info.put("course_code", lebah.db.Db.getString(rs, "course_code"));
                info.put("program_code", lebah.db.Db.getString(rs, "program_code"));
                System.out.println("program=" + Db.getString(rs, "program_code"));
                info.put("program_name", lebah.db.Db.getString(rs, "program_name"));
                info.put("course_name", lebah.db.Db.getString(rs, "course_name"));
                info.put("period_scheme", lebah.db.Db.getString(rs, "period_root_id"));
                info.put("track_id", lebah.db.Db.getString(rs, "track_id"));
                info.put("intake_month", new Integer(rs.getInt("intake_month")));
                info.put("intake_year", new Integer(rs.getInt("intake_year")));
                info.put("intake_code", lebah.db.Db.getString(rs, "intake_code"));
                info.put("intake_session", lebah.db.Db.getString(rs, "intake_session"));
                info.put("intake_session_name", lebah.db.Db.getString(rs, "intake_session_name"));
                info.put("batch_session", lebah.db.Db.getString(rs, "batch_session"));
                info.put("batch_session_name", lebah.db.Db.getString(rs, "batch_session_name"));
                info.put("faculty_code", lebah.db.Db.getString(rs, "faculty_code"));
                info.put("faculty_name", lebah.db.Db.getString(rs, "faculty_name"));
                info.put("institution_id", lebah.db.Db.getString(rs, "institution_id"));
                info.put("institution_name", lebah.db.Db.getString(rs, "institution_name"));
                info.put("institution_abbr", lebah.db.Db.getString(rs, "institution_abbr"));
                info.put("photoFileName", lebah.db.Db.getString(rs, "photoFileName"));
                info.put("nationality", Db.getString(rs, "nationality"));
                period_id = lebah.db.Db.getString(rs, "session_period_id");
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
                createPeriodName(stmt, info);
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
        boolean open = false;
        {
            r.reset();
            r.relate("sc.track_id", "pt.track_id");
            r.relate("sc.program_code", "pt.program_code");
            r.relate("pt.period_root_id", "p.period_root_id");
            r.add("p.path_no");
            sql = r.getSQLSelect("student_course sc, program_track pt, period_root p");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                open = rs.getInt(1) == -1 ? true : false;
            }
        }
        info.put("openSession", open);
        {
            r.reset();
            r.add("stu.name as student_name");
            r.add("stu.icno");
            r.add("stu.applicant_id");
            r.add("stu.currency_type");
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
            if (!open) r.add("s.session_name");
            r.add("f.faculty_code");
            r.add("f.faculty_name");
            r.add("i.institution_id");
            r.add("i.institution_name");
            r.add("i.institution_abbr");
            r.add("stu.photoFileName");
            r.add("sc.date_register");
            r.add("sc.student_id", student_id);
            r.add("sc.student_id", r.unquote("stu.id"));
            r.add("c.course_id", r.unquote("sc.course_id"));
            r.add("sc.program_code", r.unquote("p.program_code"));
            r.add("c.faculty_id", r.unquote("f.faculty_id"));
            r.add("f.institution_id", r.unquote("i.institution_id"));
            if (!open) r.add("sc.intake_session", r.unquote("s.session_id"));
            if (!open) sql = r.getSQLSelect("student stu, student_course sc, study_course c, sessions s, program p, faculty f, institution i"); else sql = r.getSQLSelect("student stu, student_course sc, study_course c, program p, faculty f, institution i");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                info.put("id", student_id);
                info.put("applicant_id", Db.getString(rs, "applicant_id"));
                info.put("currency_type", rs.getString("currency_type") != null ? rs.getString("currency_type") : "local");
                info.put("student_id", student_id);
                info.put("student_name", rs.getString("student_name"));
                info.put("icno", Db.getString(rs, "icno"));
                info.put("course_id", lebah.db.Db.getString(rs, "course_id"));
                info.put("course_code", lebah.db.Db.getString(rs, "course_code"));
                info.put("program_code", lebah.db.Db.getString(rs, "program_code"));
                info.put("program_name", lebah.db.Db.getString(rs, "program_name"));
                info.put("course_name", lebah.db.Db.getString(rs, "course_name"));
                info.put("period_scheme", lebah.db.Db.getString(rs, "period_root_id"));
                info.put("track_id", lebah.db.Db.getString(rs, "track_id"));
                info.put("intake_month", new Integer(rs.getInt("intake_month")));
                info.put("intake_year", new Integer(rs.getInt("intake_year")));
                info.put("intake_code", lebah.db.Db.getString(rs, "intake_code"));
                info.put("intake_session", lebah.db.Db.getString(rs, "intake_session"));
                if (!open) info.put("intake_session_name", lebah.db.Db.getString(rs, "session_name"));
                info.put("faculty_code", lebah.db.Db.getString(rs, "faculty_code"));
                info.put("faculty_name", lebah.db.Db.getString(rs, "faculty_name"));
                info.put("institution_id", lebah.db.Db.getString(rs, "institution_id"));
                info.put("institution_name", lebah.db.Db.getString(rs, "institution_name"));
                info.put("institution_abbr", lebah.db.Db.getString(rs, "institution_abbr"));
                info.put("photoFileName", lebah.db.Db.getString(rs, "photoFileName"));
                info.put("date_register", lebah.db.Db.getDate(rs, "date_register"));
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
        if (!open) {
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
                    createPeriodName(stmt, info);
                }
            } else {
                info.put("period_name", "UNDEFINED");
            }
        } else {
            String period_id = "";
            r.reset();
            r.add("period_id");
            r.add("student_id", student_id);
            r.add("program_code", (String) info.get("program_code"));
            sql = r.getSQLSelect("student_course");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                period_id = rs.getString(1);
            }
            info.put("period_id", period_id);
            if (period_id != null && !"".equals(period_id)) {
                if ("FINISHED".equals(period_id)) {
                    info.put("period_name", "FINISHED STUDY");
                } else {
                    createPeriodName(stmt, info);
                }
            } else {
                info.put("period_name", "UNDEFINED");
            }
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

    private static void createPeriodName(Statement stmt, Hashtable info) throws Exception {
        String sql = "";
        info.put("period_name", "");
        if (info.get("period_id") == null || "".equals((String) info.get("period_id"))) return;
        if (info.get("period_scheme") == null || "".equals((String) info.get("period_scheme"))) return;
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
                sql = r.getSQLSelect("student_course sc, student_status sta, study_course c, sessions s, program p, faculty f, institution i");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    enrolled = true;
                    info.put("course_id", lebah.db.Db.getString(rs, "course_id"));
                    info.put("course_code", lebah.db.Db.getString(rs, "course_code"));
                    info.put("program_code", lebah.db.Db.getString(rs, "program_code"));
                    info.put("program_name", lebah.db.Db.getString(rs, "program_name"));
                    info.put("course_name", lebah.db.Db.getString(rs, "course_name"));
                    info.put("period_scheme", lebah.db.Db.getString(rs, "period_root_id"));
                    info.put("intake_month", new Integer(rs.getInt("intake_month")));
                    info.put("intake_year", new Integer(rs.getInt("intake_year")));
                    info.put("intake_code", lebah.db.Db.getString(rs, "intake_code"));
                    info.put("intake_session", lebah.db.Db.getString(rs, "intake_session"));
                    info.put("intake_session_name", lebah.db.Db.getString(rs, "session_name"));
                    info.put("current_period", lebah.db.Db.getString(rs, "period_id"));
                    info.put("faculty_code", lebah.db.Db.getString(rs, "faculty_code"));
                    info.put("faculty_name", lebah.db.Db.getString(rs, "faculty_name"));
                    info.put("institution_id", lebah.db.Db.getString(rs, "institution_id"));
                    info.put("institution_name", lebah.db.Db.getString(rs, "institution_name"));
                    info.put("institution_abbr", lebah.db.Db.getString(rs, "institution_abbr"));
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
            r.add("icno");
            r.add("currency_type");
            sql = r.getSQLSelect("student");
            ResultSet rs = stmt.executeQuery(sql);
            Hashtable h = new Hashtable();
            if (rs.next()) {
                h.put("id", student);
                h.put("name", rs.getString("name"));
                h.put("icno", rs.getString("icno"));
                h.put("currency_type", rs.getString("currency_type") != null ? rs.getString("currency_type") : "local");
            }
            return h;
        } finally {
            if (db != null) db.close();
        }
    }

    public static StudentInfo getStudentInfo(String studentId) throws Exception {
        StudentInfo info = new StudentInfo();
        Hashtable h = getEnrollmentInfo(studentId);
        info.setId((String) h.get("id"));
        info.setName((String) h.get("student_name"));
        info.setIcno((String) h.get("icno"));
        info.setCourseId((String) h.get("course_id"));
        info.setCourseCode((String) h.get("course_code"));
        info.setProgramCode((String) h.get("program_code"));
        info.setProgramName((String) h.get("program_name"));
        info.setIntakeMonth(((Integer) h.get("intake_month")).intValue());
        info.setIntakeYear(((Integer) h.get("intake_year")).intValue());
        info.setIntakeCode((String) h.get("intake_code"));
        info.setIntakeSessionId((String) h.get("intake_session"));
        info.setIntakeSessionName((String) h.get("intake_session_name"));
        info.setBatchSession((String) h.get("batch_session"));
        info.setBatchSessionName((String) h.get("batch_session_name"));
        info.setFacultyCode((String) h.get("faculty_code"));
        info.setFacultyName((String) h.get("faculty_name"));
        info.setInstitutionId((String) h.get("institution_id"));
        info.setInstitutionName((String) h.get("institution_name"));
        info.setInstitutionAbbr((String) h.get("institution_abbr"));
        info.setPeriodId((String) h.get("period_id"));
        info.setPeriodName((String) h.get("period_name"));
        info.setPeriodScheme((String) h.get("period_scheme"));
        info.setTrackId((String) h.get("track_id"));
        info.setTrackName((String) h.get("track_name"));
        info.setCentreId((String) h.get("centre_id"));
        info.setCentreCode((String) h.get("centre_code"));
        info.setCentreName((String) h.get("centre_name"));
        return info;
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
        sql = "SELECT " + "st.id, st.name, st.address, " + "st.city, st.state, st.poscode, st.country_code, st.phone, " + "st.birth_date, st.email, p.period_id, prog.program_code, " + "prog.program_name, p.period_root_id, ses.session_name, ses2.session_name as intake_name " + "FROM student st, student_course sc, student_status sta, intake_batch b, " + "period p, program prog, sessions ses, sessions ses2 " + "WHERE " + "st.id = sc.student_id " + "AND st.id = sta.student_id " + "AND b.session_id = sta.session_id " + "AND b.intake_session = sta.batch_id  " + "AND b.period_id = p.period_id  " + "AND b.period_root_id = p.period_root_id " + "AND b.period_root_id = sc.period_root_id " + "AND p.period_root_id = sc.period_root_id " + "AND prog.program_code = sc.program_code  " + "AND sta.batch_id = ses.session_id  " + "AND sc.intake_session = ses2.session_id ";
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
        sql = "SELECT " + "b.intake_session, prog.program_code, sc.intake_code, st.id, st.name, st.address, " + "st.city, st.state, st.poscode, st.country_code, st.phone, " + "st.birth_date, st.email, p.period_id, prog.program_code, " + "prog.program_name, p.period_root_id, ses.session_name, ses2.session_name as intake_name " + "FROM student st, student_course sc, student_status sta, intake_batch b, " + "period p, program prog, sessions ses, sessions ses2 " + "WHERE " + "st.id = sc.student_id " + "AND st.id = sta.student_id " + "AND b.session_id = sta.session_id " + "AND b.intake_session = sta.batch_id  " + "AND b.period_id = p.period_id  " + "AND b.period_root_id = p.period_root_id " + "AND b.period_root_id = sc.period_root_id " + "AND p.period_root_id = sc.period_root_id " + "AND prog.program_code = sc.program_code  " + "AND sta.batch_id = ses.session_id  " + "AND sc.intake_session = ses2.session_id ";
        if (!"".equals(intake_session)) sql += "AND sc.intake_session = '" + intake_session + "' ";
        if (!"".equals(program_code)) sql += "AND prog.program_code = '" + program_code + "' ";
        if (!"".equals(session_id)) sql += "AND sta.session_id = '" + session_id + "' ";
        if (!"".equals(period_scheme)) sql += "AND sc.period_root_id = '" + period_scheme + "' ";
        sql += "ORDER BY sc.intake_code, prog.program_code, st.name";
        ResultSet rs = stmt.executeQuery(sql);
        Vector v = new Vector();
        while (rs.next()) {
            Hashtable h = new Hashtable();
            h.put("id", rs.getString("id"));
            h.put("name", rs.getString("name"));
            h.put("period_id", rs.getString("period_id"));
            h.put("intake_session", rs.getString("intake_session"));
            h.put("program_code", rs.getString("program_code"));
            h.put("intake_code", rs.getString("intake_code"));
            v.addElement(h);
        }
        return v;
    }

    public static Vector getStudentListAlternateTrack(Statement stmt, String period_scheme, String session_id, String program_code) throws Exception {
        String sql = "";
        sql = "SELECT st.id, st.name, p.period_id " + "FROM student st, student_course sc, student_status sta,  " + "intake_batch b, period p, program prog, program_track track,  " + "sessions ses, sessions ses2  " + "WHERE st.id = sc.student_id  " + "AND st.id = sta.student_id  " + "AND b.session_id = sta.session_id  " + "AND b.intake_session = sta.batch_id   " + "AND b.period_id = p.period_id   " + "AND b.period_root_id = p.period_root_id  " + "AND b.period_root_id = sc.period_root_id  " + "AND sc.period_root_id = track.period_root_id " + "AND prog.program_code = sc.program_code   " + "AND track.program_code = prog.program_code " + "AND sta.batch_id = ses.session_id   " + "AND sc.intake_session = ses2.session_id  ";
        if (!"".equals(program_code)) sql += "AND prog.program_code = '" + program_code + "' ";
        if (!"".equals(session_id)) sql += "AND sta.session_id = '" + session_id + "' ";
        sql += "AND track.period_root_id = '" + period_scheme + "' " + "ORDER BY st.name";
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
            System.out.println("program=" + programCode);
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
                    r.add("classroom_id", subject.getId());
                    r.add("subject_code", subject.getCode());
                    r.add("subject_title", subject.getName());
                    r.add("module_id", "");
                    r.add("subject_comment", "");
                    r.add("subject_text", "");
                    sql = r.getSQLInsert("subject");
                    stmt.executeUpdate(sql);
                }
            } else {
                String subject_code = "";
                {
                    r.clear();
                    r.add("subject_id", subject.getId());
                    r.add("subject_code");
                    sql = r.getSQLSelect("subject");
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        subject_code = rs.getString(1);
                    }
                }
                if (!subject.getCode().equals(subject_code)) {
                    r.clear();
                    r.update("subject_id", subject.getId());
                    r.add("subject_code", subject.getCode());
                    r.add("subject_title", subject.getName());
                    sql = r.getSQLUpdate("subject");
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
                sql = r.getSQLSelect("student_course sc, student_status sta, sessions ses, sessions ses2", "ses.start_date");
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
                sql = r.getSQLSelect("student_course sc, student_status sta, sessions ses, sessions ses2", "ses.start_date");
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
                sql = r.getSQLSelect("student_course sc, student_status sta, sessions ses, sessions ses2", "ses.start_date");
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
        String intake_session = (String) info.get("intake_session");
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
            registerSubjectsToStudent(student);
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
        registerSubjectsToStudent(student);
    }

    private static void registerSubjectsToStudent(StudentInfo info) throws Exception {
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
            Hashtable h = null;
            {
                r.add("id", id);
                r.add("id");
                r.add("applicant_id");
                r.add("password");
                r.add("name");
                r.add("icno");
                r.add("address");
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
                r.add("currency_type");
                r.add("nationality");
                sql = r.getSQLSelect("student s");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    h = getStudentData(rs);
                    String state_code = (String) h.get("state_code");
                    String state = getStateName(stmt, state_code);
                    if ("".equals(state)) h.put("other_state", state_code); else h.put("other_state", "");
                    h.put("state", state);
                    String country_code = (String) h.get("country_code");
                    String countryname = lebah.general.CountryData.getCountryName(country_code);
                    h.put("country", countryname);
                }
                if (h != null) {
                    Biodata biodata = getBiodata(id);
                    h.put("biodata", biodata);
                } else {
                    h = new Hashtable();
                }
            }
            {
                String applicantId = (String) h.get("applicant_id");
                r.clear();
                r.add("uid", applicantId);
                r.add("app_ref");
                sql = r.getSQLSelect("inceif_applicant");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    h.put("app_ref", rs.getString(1));
                }
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

    private static String getStateName(Statement stmt, String code) throws Exception {
        String sql = "select state_name from state_code where code = '" + code + "'";
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) return rs.getString(1); else return "";
    }

    private static Hashtable getStudentData(ResultSet rs) throws Exception {
        Hashtable studentInfo = new Hashtable();
        studentInfo.put("student_id", Db.getString(rs, "id"));
        studentInfo.put("id", Db.getString(rs, "id"));
        studentInfo.put("applicant_id", Db.getString(rs, "applicant_id"));
        studentInfo.put("password", Db.getString(rs, "password"));
        studentInfo.put("name", Db.getString(rs, "name"));
        studentInfo.put("icno", Db.getString(rs, "icno"));
        studentInfo.put("address", Db.getString(rs, "address"));
        studentInfo.put("city", Db.getString(rs, "city"));
        String state_code = Db.getString(rs, "state");
        studentInfo.put("state_code", state_code);
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
        studentInfo.put("currency_type", rs.getString("currency_type") != null ? rs.getString("currency_type") : "local");
        studentInfo.put("intake_session", Db.getString(rs, "intake_session"));
        studentInfo.put("nationality", Db.getString(rs, "nationality"));
        return studentInfo;
    }

    public static void registerSubjectsToStudent(String student_id) throws Exception {
        Hashtable studentInfo = StudentData.getEnrollmentInfo(student_id);
        String course_id = (String) studentInfo.get("course_id");
        String program_code = (String) studentInfo.get("program_code");
        String period_scheme = (String) studentInfo.get("period_scheme");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            Vector periodIds = new Vector();
            {
                r.add("period_root_id", period_scheme);
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
                r.add("period_root_id", period_scheme);
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
                r.add("period_root_id", period_scheme);
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
                    r.add("period_root_id", period_scheme);
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
}

package mecca.sis.admission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import mecca.db.Db;
import mecca.db.DbException;
import mecca.db.SQLRenderer;
import org.apache.velocity.Template;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class SetupSubjectModule extends mecca.portal.velocity.VTemplate {

    public Template doTemplate() throws Exception {
        HttpSession session = request.getSession();
        String template_name = "vtl/sis/setup_subject.vm";
        String exam_id = getParam("exam_id");
        String grade_id = getParam("grade_id");
        context.put("exam_id", exam_id);
        context.put("grade_id", grade_id);
        String submit = getParam("command");
        Vector examList = new Vector(), gradeList = new Vector();
        prepareList(examList, gradeList);
        context.put("examList", examList);
        context.put("gradeList", gradeList);
        if ("add".equals(submit)) {
            add();
        } else if ("delete".equals(submit)) {
            delete();
        }
        Vector subjectList = new Vector();
        Hashtable examDetail = new Hashtable();
        subjectList = getItem(examDetail);
        context.put("subjectList", subjectList);
        context.put("examDetail", examDetail);
        Template template = engine.getTemplate(template_name);
        return template;
    }

    private Vector getItem(Hashtable examDetail) throws Exception {
        String exam_id = getParam("exam_id");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.add("adm_exam_name");
                r.add("adm_exam_id", exam_id);
                sql = r.getSQLSelect("adm_exam");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    examDetail.put("id", exam_id);
                    examDetail.put("name", rs.getString("adm_exam_name"));
                }
            }
            Vector v = new Vector();
            {
                r.clear();
                r.add("adm_subject_id");
                r.add("adm_subject_name");
                r.add("adm_grade_display_id");
                r.add("adm_exam_id", exam_id);
                sql = r.getSQLSelect("adm_exam_subject", "adm_exam_id");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    h.put("exam_id", exam_id);
                    h.put("subject_id", rs.getString("adm_subject_id"));
                    h.put("subject_name", rs.getString("adm_subject_name"));
                    h.put("grade_display_id", rs.getString("adm_grade_display_id"));
                    v.addElement(h);
                }
            }
            return v;
        } finally {
            if (db != null) db.close();
        }
    }

    private void prepareList(Vector examList, Vector gradeList) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.add("adm_exam_id");
                r.add("adm_exam_name");
                sql = r.getSQLSelect("adm_exam");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    h.put("id", rs.getString("adm_exam_id"));
                    h.put("name", rs.getString("adm_exam_name"));
                    examList.addElement(h);
                }
            }
            {
                r.clear();
                r.add("adm_grade_display_id");
                r.add("adm_grade_display_name");
                sql = r.getSQLSelect("adm_display_grade_main");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Hashtable h = new Hashtable();
                    h.put("id", rs.getString("adm_grade_display_id"));
                    h.put("name", rs.getString("adm_grade_display_name"));
                    gradeList.addElement(h);
                }
            }
        } finally {
            if (db != null) db.close();
        }
    }

    private void add() throws Exception {
        String exam_id = getParam("exam_id");
        String subject_id = getParam("subject_id");
        String subject_name = getParam("subject_name");
        String grade_id = getParam("grade_id");
        if ("".equals(exam_id) || "".equals(subject_id) || "".equals(subject_name) || "".equals(grade_id)) throw new Exception("Can not have empty fields!");
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
                r.add("adm_exam_id");
                r.add("adm_exam_id", exam_id);
                sql = r.getSQLSelect("adm_exam");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) throw new Exception("Exam Id was invalid!");
            {
                r.clear();
                r.add("adm_subject_id");
                r.add("adm_subject_id", subject_id);
                sql = r.getSQLSelect("adm_exam_subject");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true; else found = false;
            }
            if (found) {
                r.clear();
                r.clear();
                r.add("adm_subject_name", subject_name);
                r.add("adm_grade_display_id", grade_id);
                r.update("adm_exam_id", exam_id);
                r.update("adm_subject_id", subject_id);
                sql = r.getSQLUpdate("adm_exam_subject");
                stmt.executeUpdate(sql);
            } else {
                r.clear();
                r.add("adm_exam_id", exam_id);
                r.add("adm_subject_id", subject_id);
                r.add("adm_subject_name", subject_name);
                r.add("adm_grade_display_id", grade_id);
                sql = r.getSQLInsert("adm_exam_subject");
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
        } finally {
            if (db != null) db.close();
        }
    }

    private Vector list() throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("adm_exam_id");
            r.add("adm_subject_id");
            r.add("adm_subject_name");
            sql = r.getSQLSelect("adm_exam_subject", "adm_exam_id");
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                Hashtable h = new Hashtable();
                h.put("exam_id", rs.getString("adm_exam_id"));
                h.put("subject_id", rs.getString("adm_subject_id"));
                h.put("subject_name", rs.getString("adm_subject_name"));
                v.addElement(h);
            }
            return v;
        } finally {
            if (db != null) db.close();
        }
    }

    private void delete() throws Exception {
        String subject_id = getParam("subject_id");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                r.add("applicant_id");
                r.add("adm_subject_id", subject_id);
                sql = r.getSQLSelect("adm_applicant_exam");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                sql = "DELETE FROM adm_exam_subject WHERE adm_subject_id = '" + subject_id + "'";
                stmt.executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }
}

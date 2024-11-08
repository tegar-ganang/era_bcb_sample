package educate.sis.admission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class ExamResultData {

    public static void save(Vector examInfo, Hashtable info) throws Exception {
        String applicant_id = (String) info.get("applicant_id");
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            sql = "DELETE FROM adm_applicant_exam WHERE applicant_id = '" + applicant_id + "'";
            stmt.executeUpdate(sql);
            for (int i = 0; i < examInfo.size(); i++) {
                Hashtable exam = (Hashtable) examInfo.elementAt(i);
                String exam_id = (String) exam.get("id");
                Vector subjects = (Vector) exam.get("subjects");
                for (int k = 0; k < subjects.size(); k++) {
                    Hashtable subject = (Hashtable) subjects.elementAt(k);
                    String subject_id = (String) subject.get("id");
                    String grade = (String) info.get(subject_id);
                    if (!"".equals(grade) && !"0".equals(grade)) {
                        r.clear();
                        r.add("applicant_id", applicant_id);
                        r.add("adm_exam_id", exam_id);
                        r.add("adm_subject_id", subject_id);
                        r.add("adm_subject_grade", Integer.parseInt(grade));
                        sql = r.getSQLInsert("adm_applicant_exam");
                        stmt.executeUpdate(sql);
                    }
                }
            }
            conn.commit();
        } catch (SQLException sqlex) {
            try {
                conn.rollback();
            } catch (SQLException rollex) {
            }
        } finally {
            if (db != null) db.close();
        }
    }

    public static void list(String applicant_id, Hashtable applicantDetail, Hashtable examDetail) throws Exception {
        String applicant_name = "";
        if ("".equals(applicant_id)) throw new Exception("Empty value to search!");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            {
                r.add("applicant_name");
                r.add("applicant_id", applicant_id);
                sql = r.getSQLSelect("adm_applicant");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    applicantDetail.put("id", applicant_id);
                    applicantDetail.put("name", rs.getString("applicant_name"));
                }
            }
            {
                r.clear();
                r.add("app.adm_subject_id");
                r.add("app.adm_subject_grade");
                r.add("app.applicant_id", applicant_id);
                r.add("app.adm_exam_id", r.unquote("ex.adm_exam_id"));
                r.add("ex.adm_exam_id", r.unquote("subj.adm_exam_id"));
                r.add("app.adm_subject_id", r.unquote("subj.adm_subject_id"));
                sql = r.getSQLSelect("adm_applicant_exam app, adm_exam ex, adm_exam_subject subj");
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String subject_id = rs.getString("adm_subject_id");
                    int grade = rs.getInt("adm_subject_grade");
                    examDetail.put(subject_id, new Integer(grade));
                }
            }
        } catch (DbException dbex) {
            throw dbex;
        } catch (SQLException sqlex) {
            throw sqlex;
        } finally {
            if (db != null) db.close();
        }
    }

    public static void add(Hashtable data) throws Exception {
        String applicant_id = (String) data.get("applicant_id");
        String subject_id = (String) data.get("subject_id");
        String subject_grade = (String) data.get("subject_grade");
        String exam_id = (String) data.get("exam_id");
        if ("".equals(subject_grade)) throw new Exception("Can not have empty fields!");
        if ("".equals(subject_id)) throw new Exception("Can not have empty fields!");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                r.add("applicant_id");
                r.add("applicant_id", applicant_id);
                r.add("adm_exam_id", exam_id);
                r.add("adm_subject_id", subject_id);
                sql = r.getSQLSelect("adm_applicant_exam");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true; else found = false;
            }
            if (!found) {
                r.clear();
                r.add("applicant_id", applicant_id);
                r.add("adm_exam_id", exam_id);
                r.add("adm_subject_id", subject_id);
                r.add("adm_subject_grade", subject_grade);
                sql = r.getSQLInsert("adm_applicant_exam");
                stmt.executeUpdate(sql);
            } else {
                r.clear();
                r.add("adm_subject_grade", subject_grade);
                r.update("applicant_id", applicant_id);
                r.update("adm_exam_id", exam_id);
                r.update("adm_subject_id", subject_id);
                sql = r.getSQLUpdate("adm_applicant_exam");
                stmt.executeUpdate(sql);
            }
        } catch (DbException dbex) {
            throw dbex;
        } catch (SQLException sqlex) {
            throw sqlex;
        } finally {
            if (db != null) db.close();
        }
    }

    public static void delete(Hashtable data) throws Exception {
        String applicant_id = (String) data.get("applicant_id");
        String exam_id = (String) data.get("exam_id");
        String subject_id = (String) data.get("subject_id");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            sql = "DELETE FROM adm_applicant_exam WHERE applicant_id = '" + applicant_id + "' " + " AND adm_exam_id = '" + exam_id + "' AND adm_subject_id = '" + subject_id + "'";
            stmt.executeUpdate(sql);
        } finally {
            if (db != null) db.close();
        }
    }
}

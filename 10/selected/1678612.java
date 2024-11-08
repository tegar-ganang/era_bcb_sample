package educate.sis.admission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import lebah.db.Db;
import lebah.db.DbException;
import lebah.db.SQLRenderer;
import org.apache.velocity.Template;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class SetupExamModule extends lebah.portal.velocity.VTemplate {

    public Template doTemplate() throws Exception {
        HttpSession session = request.getSession();
        String template_name = "vtl/sis/setup_exam.vm";
        String submit = getParam("command");
        context.put("examDetail", new Hashtable());
        if ("add".equals(submit) || "update".equals(submit)) {
            Hashtable exam = add();
            context.put("examDetail", exam);
        } else if ("getdata".equals(submit)) {
            Hashtable exam = getdata();
            context.put("examDetail", exam);
        } else if ("delete".equals(submit)) {
            delete();
        } else if ("updatedisplay".equals(submit)) {
            String[] ids = request.getParameterValues("exam_ids");
            if (ids != null) updateDisplay(ids);
        }
        Vector v = list();
        context.put("examVector", v);
        Template template = engine.getTemplate(template_name);
        return template;
    }

    private Hashtable getdata() throws Exception {
        String exam_id = getParam("exam_id");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("adm_exam_name");
            r.add("adm_exam_id", exam_id);
            sql = r.getSQLSelect("adm_exam");
            ResultSet rs = stmt.executeQuery(sql);
            Hashtable h = new Hashtable();
            if (rs.next()) {
                h.put("id", exam_id);
                h.put("name", rs.getString("adm_exam_name"));
            }
            return h;
        } finally {
            if (db != null) db.close();
        }
    }

    private Hashtable add() throws Exception {
        String exam_id = getParam("exam_id");
        String exam_name = getParam("exam_name");
        Hashtable data = new Hashtable();
        if ("".equals(exam_id.trim())) throw new Exception("Exam Id empty");
        if ("".equals(exam_name.trim())) throw new Exception("Exam Name empty");
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                r.add("adm_exam_name");
                r.add("adm_exam_id", exam_id);
                sql = r.getSQLSelect("adm_exam");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (found) {
                r.clear();
                r.add("adm_exam_name", exam_name);
                r.update("adm_exam_id", exam_id);
                sql = r.getSQLUpdate("adm_exam");
                stmt.executeUpdate(sql);
            } else {
                r.clear();
                r.add("adm_exam_id", exam_id);
                r.add("adm_exam_name", exam_name);
                sql = r.getSQLInsert("adm_exam");
                stmt.executeUpdate(sql);
            }
            conn.commit();
            data.put("id", exam_id);
            data.put("name", exam_name);
            return data;
        } catch (SQLException sqlex) {
            try {
                conn.rollback();
            } catch (SQLException rollex) {
            }
            throw sqlex;
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
            r.add("adm_exam_name");
            r.add("display");
            sql = r.getSQLSelect("adm_exam", "adm_exam_id");
            ResultSet rs = stmt.executeQuery(sql);
            Vector v = new Vector();
            while (rs.next()) {
                Hashtable h = new Hashtable();
                h.put("exam_id", rs.getString("adm_exam_id"));
                h.put("exam_name", rs.getString("adm_exam_name"));
                h.put("display", lebah.db.Db.getString(rs, "display"));
                v.addElement(h);
            }
            return v;
        } catch (DbException dbex) {
            throw dbex;
        } catch (SQLException sqlex) {
            throw sqlex;
        } finally {
            if (db != null) db.close();
        }
    }

    private void delete() throws Exception {
        String exam_id = getParam("exam_id");
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                r.add("adm_subject_id");
                r.add("adm_exam_id", exam_id);
                sql = r.getSQLSelect("adm_exam_subject");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                sql = "DELETE FROM adm_exam WHERE adm_exam_id = '" + exam_id + "'";
                stmt.executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    void updateDisplay(String[] ids) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            {
                sql = "update adm_exam set display = 'no'";
                stmt.executeUpdate(sql);
            }
            for (int i = 0; i < ids.length; i++) {
                sql = "update adm_exam set display = 'yes' where adm_exam_id = '" + ids[i] + "'";
                stmt.executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }
}

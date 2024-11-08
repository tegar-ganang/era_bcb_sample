package educate.sis.struct;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;
import javax.servlet.http.HttpSession;
import lebah.db.Db;
import lebah.db.SQLRenderer;
import org.apache.velocity.Template;
import educate.sis.tools.Token;
import educate.sis.tools.UniqueStringId;

/**
 * @author Shamsul Bahrin Abd Mutalib
 * @version 1.01
 */
public class SetupStudyPeriodModule extends lebah.portal.velocity.VTemplate {

    public Template doTemplate() throws Exception {
        HttpSession session = request.getSession();
        String template_name = "vtl/sis/struct/setup_study_period.vm";
        String submit = getParam("command");
        Vector periodList = session.getAttribute("periodList") != null ? (Vector) session.getAttribute("periodList") : new Vector();
        session.setAttribute("periodList", periodList);
        String schema_id = "";
        boolean isSelected = false;
        boolean isEdited = false;
        if ("add".equals(submit)) {
            schema_id = getParam("schema_list");
            addPeriod(periodList, session);
            isSelected = true;
            isEdited = true;
        } else if ("delete".equals(submit)) {
            schema_id = getParam("schema_code");
            deletePeriod(periodList, session);
            isSelected = true;
            isEdited = true;
        } else if ("addStructure".equals(submit)) {
            schema_id = getParam("new_code");
            int path_no = Integer.parseInt(getParam("new_path_no"));
            addNewStructure(schema_id, path_no);
            periodList = PeriodData.getPeriodData(schema_id);
            session.setAttribute("periodList", periodList);
            isSelected = true;
            isEdited = false;
        } else if ("save".equals(submit)) {
            savePeriodData(periodList);
            schema_id = getParam("schema_code");
            periodList = PeriodData.getPeriodData(schema_id);
            session.setAttribute("periodList", periodList);
            isSelected = true;
            isEdited = false;
        } else if ("getdata".equals(submit)) {
            schema_id = getParam("schema_list");
            periodList = PeriodData.getPeriodData(schema_id);
            session.setAttribute("periodList", periodList);
            isSelected = true;
            isEdited = false;
        } else if ("remove".equals(submit)) {
            schema_id = getParam("schema_list");
            remove(schema_id);
            isSelected = false;
            isEdited = false;
        } else {
            periodList = new Vector();
            session.setAttribute("periodList", periodList);
        }
        context.put("isSelected", new Boolean(isSelected));
        context.put("isEdited", new Boolean(isEdited));
        if (isSelected) {
            Hashtable periodData = new Hashtable();
            getdata(session, periodData, schema_id);
            context.put("periodData", periodData);
            Vector allPeriod = new Vector();
            getAllPeriod(periodList, allPeriod);
            context.put("allPeriod", allPeriod);
            context.put("periodList", periodList);
            String period_select = getParam("period_list");
            context.put("periodSelect", period_select);
        } else {
            context.put("periodData", new Hashtable());
        }
        Vector periodschemaList = PeriodData.getSchemeList();
        context.put("periodschemaList", periodschemaList);
        session.setAttribute("token", Token.get());
        Template template = engine.getTemplate(template_name);
        return template;
    }

    private void getdata(HttpSession session, Hashtable periodData, String schema_id) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            r.add("period_schema_code");
            r.add("path_no");
            r.add("period_root_id", schema_id);
            sql = r.getSQLSelect("period_root");
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                periodData.put("code", rs.getString("period_schema_code"));
                periodData.put("path_no", new Integer(rs.getInt("path_no")));
                periodData.put("id", schema_id);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    private void deletePeriod(Vector periodList, HttpSession session) throws Exception {
        String token = getParam("token");
        String token_session = session.getAttribute("token") != null ? (String) session.getAttribute("token") : "";
        if (!token.equals(token_session)) return;
        String period_list = getParam("period_list");
        for (int i = 0; i < periodList.size(); i++) {
            Period p = (Period) periodList.elementAt(i);
            if (period_list.equals(p.getId())) {
                periodList.remove(p);
            } else {
                p.remove(period_list);
            }
        }
    }

    private void addPeriod(Vector periodList, HttpSession session) throws Exception {
        String token = getParam("token");
        String token_session = session.getAttribute("token") != null ? (String) session.getAttribute("token") : "";
        if (!token.equals(token_session)) return;
        String scheme_code = getParam("schema_code");
        String period_name = getParam("period_name");
        String period_list = getParam("period_list");
        if ("".equals(period_name)) throw new Exception("Empty string...");
        Period period = new Period();
        period.setId(UniqueStringId.get());
        period.setName(period_name);
        if (!"".equals(period_list)) {
            for (int i = 0; i < periodList.size(); i++) {
                Period p = (Period) periodList.elementAt(i);
                if (period_list.equals(p.getId())) {
                    period.setId(p.getId() + "-" + period.getName().toUpperCase());
                    p.addChild(period);
                    break;
                } else {
                    Period child = p.findChildById(period_list);
                    if (child != null) {
                        period.setId(child.getId() + "-" + period.getName().toUpperCase());
                        child.addChild(period);
                        break;
                    } else {
                    }
                }
            }
        } else {
            period.setId(scheme_code + "-" + period.getName().toUpperCase());
            period.setSequence(periodList.size() + 1);
            periodList.addElement(period);
        }
    }

    private void getAllPeriod(Vector periods, Vector v) {
        for (int i = 0; i < periods.size(); i++) {
            Period period = (Period) periods.elementAt(i);
            v.addElement(period);
            if (period.hasChild()) {
                getAllPeriod(period.getChild(), v);
            }
        }
    }

    private void savePeriod(Vector periods) {
        for (int i = 0; i < periods.size(); i++) {
            Period period = (Period) periods.elementAt(i);
            Period parent = period.getParent();
            String parent_id = "0";
            String parent_name = "";
            if (parent != null) {
                parent_id = parent.getId();
                parent_name = parent.getName();
            }
            if (period.hasChild()) {
                savePeriod(period.getChild());
            }
        }
    }

    private void savePeriod(String schema_code, Vector periods, Statement stmt, SQLRenderer r, String sql) throws SQLException, Exception {
        for (int i = 0; i < periods.size(); i++) {
            Period period = (Period) periods.elementAt(i);
            Period parent = period.getParent();
            String parent_id = "0";
            String parent_name = "";
            if (parent != null) {
                parent_id = parent.getId();
                parent_name = parent.getName();
            } else {
                period.setSequence(i + 1);
            }
            r.clear();
            r.add("period_root_id", schema_code);
            r.add("period_id", period.getId());
            r.add("parent_id", parent_id);
            r.add("period_level", period.getLevel());
            r.add("period_sequence", period.getSequence());
            r.add("period_name", period.getName());
            sql = r.getSQLInsert("period");
            stmt.executeUpdate(sql);
            if (period.hasChild()) {
                savePeriod(schema_code, period.getChild(), stmt, r, sql);
            }
        }
    }

    private void addNewStructure(String code, int path_no) throws Exception {
        Db db = null;
        String sql = "";
        try {
            db = new Db();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            boolean found = false;
            {
                r.add("period_schema_code");
                r.add("period_schema_code", code);
                sql = r.getSQLSelect("period_root");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                r.clear();
                r.add("period_schema_code", code);
                r.add("period_root_id", code);
                r.add("path_no", path_no);
                sql = r.getSQLInsert("period_root");
                stmt.executeUpdate(sql);
            }
        } finally {
            if (db != null) db.close();
        }
    }

    private void savePeriodData(Vector periods) throws Exception {
        String period_list = getParam("period_list");
        String schema_code = getParam("schema_code");
        if ("".equals(schema_code)) throw new Exception("Empty string...");
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            conn = db.getConnection();
            Statement stmt = db.getStatement();
            SQLRenderer r = new SQLRenderer();
            conn.setAutoCommit(false);
            boolean found = false;
            {
                r.add("period_schema_code");
                r.add("period_schema_code", schema_code);
                sql = r.getSQLSelect("period_root");
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) found = true;
            }
            if (!found) {
                r.clear();
                r.add("period_schema_code", schema_code);
                r.add("period_root_id", schema_code);
                r.add("path_no", 0);
                sql = r.getSQLInsert("period_root");
                stmt.executeUpdate(sql);
            }
            {
                sql = "DELETE FROM period WHERE period_root_id = '" + schema_code + "'";
                stmt.executeUpdate(sql);
            }
            savePeriod(schema_code, periods, stmt, r, sql);
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

    private void remove(String schema_id) throws Exception {
        Db db = null;
        String sql = "";
        Connection conn = null;
        try {
            db = new Db();
            conn = db.getConnection();
            conn.setAutoCommit(false);
            Statement stmt = db.getStatement();
            sql = "DELETE FROM period where period_root_id = '" + schema_id + "' ";
            stmt.executeUpdate(sql);
            sql = "DELETE FROM period_root WHERE period_root_id = '" + schema_id + "' ";
            stmt.executeUpdate(sql);
            conn.commit();
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
}

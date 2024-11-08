package org.mc.app;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.simple.JSONObject;
import org.eg.Utils;
import org.mc.ajax.JSON;
import org.mc.content.ContentCreator;
import org.mc.content.Retailer;
import org.mc.db.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetailerCreator extends ContentCreator {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(RetailerCreator.class);
    }

    private Retailer create() throws SQLException, IOException {
        Connection conn = null;
        Statement st = null;
        String query = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            st = conn.createStatement();
            query = "insert into " + DB.Tbl.ret + "(" + col.title + "," + col.addDate + "," + col.authorId + ") " + "values('" + title + "',now()," + user.getId() + ")";
            st.executeUpdate(query, new String[] { col.id });
            rs = st.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("Не удается получить generated key 'id' в таблице retailers.");
            }
            int genId = rs.getInt(1);
            rs.close();
            saveDescr(genId);
            conn.commit();
            Retailer ret = new Retailer();
            ret.setId(genId);
            ret.setTitle(title);
            ret.setDescr(descr);
            RetailerViewer.getInstance().somethingUpdated();
            return ret;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (Exception e1) {
            }
            throw e;
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
            }
            try {
                st.close();
            } catch (Exception e) {
            }
            try {
                conn.close();
            } catch (Exception e) {
            }
        }
    }

    public String getJsonResponce() {
        try {
            log.debug("Создание нового вендора: " + title);
            JSONObject json = new JSONObject();
            if (sameTitleExists()) {
                json.put(JSON.KEY_RESULT, JSON.VAL_SAME_TITLE_EXISTS);
                return json.toString();
            }
            Retailer r = create();
            json.put(JSON.KEY_RESULT, JSON.VAL_SUCCESS);
            json.put(JSON.KEY_ID, r.getId());
            return json.toString();
        } catch (Exception e) {
            log.warn("Ошибка при создании нового вендора.", e);
            return Utils.wrapExceptionIntoJson(e);
        }
    }

    @Override
    protected File getDescrFileInner(int id) {
        return Retailer.getDescrFile(id);
    }

    @Override
    protected String sameTitleExistsQuery() {
        return "SELECT " + col.id + " FROM " + DB.Tbl.ret + " where " + col.title + "='" + title + "'";
    }
}

package org.mc.user;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.simple.JSONObject;
import org.eg.Utils;
import org.mc.ajax.JSON;
import org.mc.app.Inittable;
import org.mc.db.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO DeInject, validate input
 * @author evgeniivs
 */
public class UserCreator extends Inittable {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(UserCreator.class);
    }

    private String name;

    private String login;

    private String pass;

    private String email;

    private String role;

    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public static boolean sameLoginExists(String login) throws SQLException {
        return DB.Action.sameExists("select " + col.id + " from " + DB.Tbl.users + " where " + col.login + " = '" + login + "'");
    }

    private boolean sameLoginExists() throws SQLException {
        return sameLoginExists(login);
    }

    private int create() throws SQLException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        String query = null;
        try {
            conn = dataSource.getConnection();
            st = conn.createStatement();
            query = "insert into " + DB.Tbl.users + "(" + col.name + "," + col.login + "," + col.pass + "," + col.passHash + "," + col.email + "," + col.role + "," + col.addDate + ") values('" + name + "','" + login + "','" + pass + "','" + pass.hashCode() + "','" + email + "'," + role + ",now())";
            st.executeUpdate(query, new String[] { col.id });
            rs = st.getGeneratedKeys();
            while (rs.next()) {
                int genId = rs.getInt(1);
                conn.commit();
                return genId;
            }
            throw new SQLException("Не удается получить generatedKey при создании пользователя.");
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

    public String getJsonResponse() {
        try {
            log.debug("Создание нового пользователя с именем '{}' и логином '{}'.", name, login);
            JSONObject json = new JSONObject();
            if (sameLoginExists()) {
                json.put(JSON.KEY_RESULT, JSON.VAL_SAME_LOGIN_EXISTS);
                return json.toString();
            }
            int newUserId = create();
            json.put(JSON.KEY_RESULT, JSON.VAL_SUCCESS);
            json.put(JSON.KEY_ID, newUserId);
            return json.toString();
        } catch (Exception e) {
            log.warn("Ошибка при создании нового пользователя.", e);
            return Utils.wrapExceptionIntoJson(e);
        }
    }
}

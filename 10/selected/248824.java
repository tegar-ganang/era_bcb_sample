package org.mc.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mc.app.Inittable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DB extends Inittable {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(DB.class);
    }

    public static class Pool {

        public static String validationQuery = "select now()";
    }

    public static class Tbl {

        public static String cat = "categories";

        public static String prod = "products";

        public static String catProdRel = "cat_prod_rel";

        public static String prodAttrs = "prod_attrs";

        public static String ret = "retailers";

        public static String vend = "vendors";

        public static String retProdRel = "ret_prod_rel";

        public static String users = "users";

        public static String prodAttrChanges = "prod_attr_changes";

        public static String prodChanges = "prod_changes";

        public static String tree = "tree";

        public static String treeCat = "tree_cat";

        public static String treeProd = "tree_prod";

        public static String img = "img";
    }

    public static class Col {

        public static String id = "id";

        public static String catId = "cat_id";

        public static String prodId = "prod_id";

        public static String title = "title";

        public static String parentId = "parent_id";

        public static String val = "val";

        public static String descr = "descr";

        public static String addDate = "add_date";

        public static String temp = "temp";

        public static String visible = "visible";

        public static String name = "name";

        public static String regDate = "reg_date";

        public static String locked = "locked";

        public static String role = "role";

        public static String attrId = "attr_id";

        public static String deleted = "deleted";

        public static String newTitle = "new_title";

        public static String newDescr = "new_descr";

        public static String pass = "pass";

        public static String login = "login";

        public static String email = "email";

        public static String retId = "ret_id";

        public static String refId = "ref_id";

        public static String vendorId = "vend_id";

        public static String authorId = "author_id";

        public static String lKey = "lk";

        public static String rKey = "rk";

        public static String level = "lev";

        public String treeId = "tree_id";

        public String passHash = "pass_hash";

        public String entityId = "entity_id";

        public String entity = "entity";

        public String order = "order_";

        public String thumbName = "thumb_name";
    }

    public static class Action {

        public static int simpleUpdate(String query) throws SQLException {
            Connection conn = null;
            Statement st = null;
            try {
                conn = dataSource.getConnection();
                st = conn.createStatement();
                int res = st.executeUpdate(query);
                conn.commit();
                return res;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (Exception e1) {
                }
                throw e;
            } finally {
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

        public static boolean sameExists(String query) throws SQLException {
            Connection conn = null;
            Statement st = null;
            ResultSet rs = null;
            try {
                conn = dataSource.getConnection();
                st = conn.createStatement();
                rs = st.executeQuery(query);
                while (rs.next()) {
                    return true;
                }
                return false;
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
    }

    public static class Stored {

        public static String createCategory = "";
    }

    public static class RetCodes {

        public static int sameTitleExists = -1;
    }
}

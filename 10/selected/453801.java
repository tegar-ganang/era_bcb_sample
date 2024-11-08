package fido.servlets.testmods;

import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import fido.db.ProperNounTable;
import fido.db.ProperNoun;
import fido.servlets.ClearData;
import fido.util.FidoDataSource;

public class TestProperNounTable extends GenericTestModule {

    protected static void clearTables() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = FidoDataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            ClearData.clearTables(stmt);
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (2, '')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (3, '')");
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public static void test_simple(PrintWriter out) {
        try {
            clearTables();
            ProperNounTable table = new ProperNounTable();
            table.add("jack", 1);
            table.add("john", 1);
            table.add("jack", 1);
            table.add("samantha", 1);
            table.modify("jack", 2, 2);
            table.add("jack", 3);
            Collection list = table.list();
            if (list.size() != 5) throw new Exception("List contained " + list.size() + " items, expected 5");
            Iterator it = list.iterator();
            ProperNoun pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 1)) throw new Exception("First proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 2)) throw new Exception("second proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 3)) throw new Exception("third proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("john") == false) || (pn.getObjectId() != 1)) throw new Exception("fourth proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("samantha") == false) || (pn.getObjectId() != 1)) throw new Exception("fifth proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            list = table.get("jack");
            it = list.iterator();
            if (list.size() != 3) throw new Exception("Get returned " + list.size() + " items, expected 3");
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 1)) throw new Exception("First proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 2)) throw new Exception("second proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 3)) throw new Exception("third proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            table.delete("jack", 2);
            list = table.list();
            if (list.size() != 4) throw new Exception("List contained " + list.size() + " items, expected 4");
            it = list.iterator();
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 1)) throw new Exception("First proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("jack") == false) || (pn.getObjectId() != 3)) throw new Exception("second proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("john") == false) || (pn.getObjectId() != 1)) throw new Exception("third proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            pn = (ProperNoun) it.next();
            if ((pn.getNoun().equals("samantha") == false) || (pn.getObjectId() != 1)) throw new Exception("fourth proper noun was '" + pn.getNoun() + "' with object " + pn.getObjectId());
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }
}

package fido.servlets.testmods;

import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import fido.db.GrammarLinkTable;
import fido.db.DictionaryTable;
import fido.db.WordNotFoundException;
import fido.db.WordSense;
import fido.servlets.ClearData;
import fido.util.FidoDataSource;

public class TestDictionaryTable extends GenericTestModule {

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
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (4, '')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (5, '')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (6, '')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (7, '')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (8, '')");
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public static void test_save_and_reopen(PrintWriter out) {
        try {
            clearTables();
            DictionaryTable dictionary = new DictionaryTable();
            GrammarLinkTable grammarLink = new GrammarLinkTable();
            grammarLink.add("A", GrammarLinkTable.SUBJECT);
            grammarLink.add("B", GrammarLinkTable.SUBJECT);
            grammarLink.add("C", GrammarLinkTable.SUBJECT);
            grammarLink.add("D", GrammarLinkTable.SUBJECT);
            grammarLink.add("E", GrammarLinkTable.SUBJECT);
            grammarLink.add("F", GrammarLinkTable.SUBJECT);
            dictionary.add("hello", "A+", 1);
            dictionary.add("hello", "B+", 2);
            Collection senses = dictionary.lookupWord("hello");
            boolean ws1 = false;
            boolean ws2 = false;
            for (Iterator it = senses.iterator(); it.hasNext(); ) {
                WordSense sense = (WordSense) it.next();
                if (sense.getRepresentedObject() == 1) ws1 = true;
                if (sense.getRepresentedObject() == 2) ws2 = true;
            }
            if (ws1 == false) throw new Exception("ws1 not found");
            if (ws2 == false) throw new Exception("ws2 not found");
            int id = dictionary.add("bye", "C-", 3);
            dictionary.add("bye", "C+", 4);
            dictionary.delete("bye", id);
            senses = dictionary.lookupWord("bye");
            for (Iterator it = senses.iterator(); it.hasNext(); ) {
                WordSense sense = (WordSense) it.next();
                if (sense.getRepresentedObject() != 4) throw new Exception("only word sense != 4, it was " + sense.getRepresentedObject());
            }
            dictionary.add("final", "D+", 5);
            dictionary.add("final", "E+", 6);
            if (dictionary.contains("final") == false) throw new Exception("Word 'final' was not found in dictionary");
            id = dictionary.add("modify", "F-", 7);
            dictionary.modify("modify", id, "F+", 8);
            senses = dictionary.lookupWord("modify");
            for (Iterator it = senses.iterator(); it.hasNext(); ) {
                WordSense sense = (WordSense) it.next();
                if (sense.getRepresentedObject() != 8) throw new Exception("only word sense not = 8, it was " + sense.getRepresentedObject());
            }
            senses = dictionary.lookupWord("modify");
            for (Iterator it = senses.iterator(); it.hasNext(); ) {
                WordSense sense = (WordSense) it.next();
                if (sense.getRepresentedObject() != 8) throw new Exception("only word sense not = 8, it was " + sense.getRepresentedObject());
            }
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }
}

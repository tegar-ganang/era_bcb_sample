package fido.servlets.testmods;

import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import fido.db.GrammarLinkTable;
import fido.db.WordClassificationTable;
import fido.db.WordClassification;
import fido.db.WordSense;
import fido.servlets.ClearData;
import fido.util.FidoDataSource;

public class TestWordClassificationTable extends GenericTestModule {

    protected static void clearTables() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = FidoDataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            ClearData.clearTables(stmt);
            stmt.executeUpdate("delete from Objects");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (1, 'Money value')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (2, 'Date')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (3, 'Unix path')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (4, 'Dos path')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (5, 'Time')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (6, 'IP address')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (7, 'Internet hostname')");
            stmt.executeUpdate("insert into Objects (ObjectId, Description) values (8, 'Number')");
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public static void test_working(PrintWriter out) {
        try {
            clearTables();
            WordClassificationTable classify = new WordClassificationTable();
            WordSense sense;
            classify.add("$N[.N]", "", 1, "");
            classify.add("N/N/N", "", 2, "");
            classify.add("[/](C/)C", "", 3, "");
            classify.add("[A:](C\\\\)C", "", 4, "");
            classify.add("N:N", "", 5, "");
            classify.add("N.N.N.N", "", 6, "");
            classify.add("E(.E)", "", 7, "");
            classify.add("{+-}N[.N]", "", 8, "");
            sense = classify.classify("$45");
            if (sense.getRepresentedObject() != 1) throw new Exception("'$45' did not equal '1': " + sense.getRepresentedObject());
            sense = classify.classify("$905.33");
            if (sense.getRepresentedObject() != 1) throw new Exception("'$905.33' did not equal '1': " + sense.getRepresentedObject());
            sense = classify.classify("10/20/2000");
            if (sense.getRepresentedObject() != 2) throw new Exception("'10/20/2000' did not equal '2': " + sense.getRepresentedObject());
            sense = classify.classify("/home/cdionis/.bashrc");
            if (sense.getRepresentedObject() != 3) throw new Exception("'/home/cdionis/.bashrc' did not equal '3': " + sense.getRepresentedObject());
            sense = classify.classify("javaProjects/RegularExpression");
            if (sense.getRepresentedObject() != 3) throw new Exception("'javaProjects/RegularExpression' did not equal '3': " + sense.getRepresentedObject());
            sense = classify.classify("/This/Is/A/Really/Long/path");
            if (sense.getRepresentedObject() != 3) throw new Exception("'/This/Is/A/Really/Long/path' did not equal '3': " + sense.getRepresentedObject());
            sense = classify.classify("a:\\chad.floppy");
            if (sense.getRepresentedObject() != 4) throw new Exception("'a:\\chad.floppy' did not equal '4': " + sense.getRepresentedObject());
            sense = classify.classify("\\chad.exe");
            if (sense.getRepresentedObject() != 4) throw new Exception("'\\chad.exe' did not equal '4': " + sense.getRepresentedObject());
            sense = classify.classify("\\path\\working.bat");
            if (sense.getRepresentedObject() != 4) throw new Exception("'\\path\\working.bat' did not equal '4': " + sense.getRepresentedObject());
            sense = classify.classify("12:30");
            if (sense.getRepresentedObject() != 5) throw new Exception("'12:30' did not equal '5': " + sense.getRepresentedObject());
            sense = classify.classify("199.82.9.187");
            if (sense.getRepresentedObject() != 6) throw new Exception("'199.82.9.187' did not equal '6': " + sense.getRepresentedObject());
            sense = classify.classify("www.hotmail.com");
            if (sense.getRepresentedObject() != 7) throw new Exception("'www.hotmail.com' did not equal '7': " + sense.getRepresentedObject());
            sense = classify.classify("123");
            if (sense.getRepresentedObject() != 8) throw new Exception("'123' did not equal '8': " + sense.getRepresentedObject());
            sense = classify.classify("+123");
            if (sense.getRepresentedObject() != 8) throw new Exception("'+123' did not equal '8': " + sense.getRepresentedObject());
            sense = classify.classify("-123");
            if (sense.getRepresentedObject() != 8) throw new Exception("'-123' did not equal '8': " + sense.getRepresentedObject());
            sense = classify.classify("123.45");
            if (sense.getRepresentedObject() != 8) throw new Exception("'123.45' did not equal '8': " + sense.getRepresentedObject());
            sense = classify.classify("+123.45");
            if (sense.getRepresentedObject() != 8) throw new Exception("'+123.45' did not equal '8': " + sense.getRepresentedObject());
            sense = classify.classify("hi");
            if (sense != null) throw new Exception("'hi' was not null: " + sense.getRepresentedObject());
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_get_all(PrintWriter out) {
        try {
            clearTables();
            WordClassificationTable classify = new WordClassificationTable();
            GrammarLinkTable grammarLinks = new GrammarLinkTable();
            grammarLinks.add("N", GrammarLinkTable.ARTICLE);
            grammarLinks.add("A", GrammarLinkTable.ARTICLE);
            grammarLinks.add("B", GrammarLinkTable.ARTICLE);
            classify.add("$N[.N]", "N+", 1, "");
            classify.add("{+-}N[.N]", "A+ | B-", 8, "");
            Collection classifications = classify.list();
            if (classifications.size() != 2) throw new Exception("Expected two rows, but got " + classifications.size());
            Iterator it = classifications.iterator();
            WordClassification wc = (WordClassification) it.next();
            if (wc.getRank() != 1) throw new Exception("classifications[0] rank does not equal '1': " + wc.getRank());
            if (wc.getPattern().equals("$N[.N]") == false) throw new Exception("classifications[0] pattern does not equal '$N[.N]': " + wc.getPattern());
            if (wc.getGrammarString().equals("N+") == false) throw new Exception("classifications[0] grammar string does not equal 'N+': " + wc.getGrammarString());
            if (wc.getObjectId().equals("1") == false) throw new Exception("classifications[0] object id does not equal '1': " + wc.getObjectId());
            wc = (WordClassification) it.next();
            if (wc.getRank() != 2) throw new Exception("classifications[1] rank does not equal '2': " + wc.getRank());
            if (wc.getPattern().equals("{+-}N[.N]") == false) throw new Exception("classifications[1] pattern does not equal '{+-}N[.N]': " + wc.getPattern());
            if (wc.getGrammarString().equals("A+ | B-") == false) throw new Exception("classifications[1] grammar string does not equal 'A+ | B-': " + wc.getGrammarString());
            if (wc.getObjectId().equals("8") == false) throw new Exception("classifications[1] object id does not equal '8': " + wc.getObjectId());
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_ordering(PrintWriter out) {
        try {
            clearTables();
            GrammarLinkTable grammarLinks = new GrammarLinkTable();
            grammarLinks.add("A", GrammarLinkTable.ARTICLE);
            WordClassificationTable classify = new WordClassificationTable();
            WordSense sense;
            classify.add("$N[.N]", "A+", 1, "");
            classify.add("{+-}N(.N)", "A+", 8, "");
            classify.add("N.N.N.N", "A+", 6, "");
            sense = classify.classify("45.55.22.99");
            if (sense.getRepresentedObject() != 8) throw new Exception("'45.55.22.99' did not equal '8': " + sense.getRepresentedObject() + " initially");
            classify.modify(2, "{+-}N[.N]", "A+", 8, "");
            sense = classify.classify("45.55.22.99");
            if (sense.getRepresentedObject() != 6) throw new Exception("'45.55.22.99' did not equal '6': " + sense.getRepresentedObject() + " after modifying the class pattern");
            classify.modify(2, "{+-}N(.N)", "A+", 8, "");
            classify.moveRowUp(3);
            sense = classify.classify("45.55.22.99");
            if (sense.getRepresentedObject() != 6) throw new Exception("'45.55.22.99' did not equal '6': " + sense.getRepresentedObject() + " after moving the IP address up.");
            classify.moveRowDown(2);
            sense = classify.classify("45.55.22.99");
            if (sense.getRepresentedObject() != 8) throw new Exception("'45.55.22.99' did not equal '8': " + sense.getRepresentedObject() + " after moving the IP address row down");
            classify.delete(2);
            sense = classify.classify("45.55.22.99");
            if (sense.getRepresentedObject() != 6) throw new Exception("'45.55.22.99' did not equal '6': " + sense.getRepresentedObject() + " after deleting the Number pattern.");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }
}

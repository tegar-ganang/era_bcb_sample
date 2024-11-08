package fido.servlets.testmods;

import java.sql.*;
import java.util.*;
import java.io.PrintWriter;
import fido.db.LanguageMorphologyTable;
import fido.db.MorphologyRecognizeMatch;
import fido.db.MorphologyRule;
import fido.db.MorphologyTagNotFoundException;
import fido.servlets.ClearData;
import fido.util.FidoDataSource;

public class TestLanguageMorphologyTable extends GenericTestModule {

    protected static void clearTables() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = FidoDataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            ClearData.clearTables(stmt);
            stmt.executeUpdate("delete from MorphologyTags");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('not')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('plural')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('third singular')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('again')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('past tense')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('against')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('deprive')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('cause to happen')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('nounify')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('someone who believes')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('belief system of')");
            stmt.executeUpdate("insert into MorphologyTags (TagName) values ('capable of')");
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    public static void test_regular_form(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "*", "*s");
            Collection recog = languageTable.recognize("English", "dogs");
            if (recog.size() != 1) throw new Exception("Recognizing 'dogs' produced " + recog.size() + ", not 1 result");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("dog") == false) throw new Exception("Recognizing 'dogs' produced '" + match.getRootString() + "', not 'dog'");
            Vector expTags = new Vector();
            expTags.add("plural");
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            String surface = languageTable.generate("English", "plural", "dog");
            if (surface.equals("dogs") == false) throw new Exception("Generating plural of 'dog' produced '" + surface + "', not 'dogs'");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_irregular_form(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "child", "children");
            languageTable.add("English", "plural", "*ch", "*ches");
            Collection recog = languageTable.recognize("English", "children");
            if (recog.size() != 1) throw new Exception("Recognizing 'children' produced " + recog.size() + ", not 1 result");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("child") == false) throw new Exception("Recognizing 'children' produced '" + match.getRootString() + "', not 'child'");
            Vector expTags = new Vector();
            expTags.add("plural");
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            String surface = languageTable.generate("English", "plural", "child");
            if (surface.equals("children") == false) throw new Exception("Generating plural of 'child' produced '" + surface + "', not 'children'");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_no_rule(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "child", "children");
            languageTable.add("English", "plural", "*ch", "*ches");
            Collection recog = languageTable.recognize("English", "boxes");
            if (recog.size() != 1) throw new Exception("Recognizing 'boxes' produced " + recog.size() + ", not 1 result");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("boxes") == false) throw new Exception("Recognizing 'boxes' produced '" + match.getRootString() + "', not 'boxes'");
            Vector expTags = new Vector();
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            String surface = languageTable.generate("English", "plural", "box");
            if (surface.equals("box") == false) throw new Exception("Generating plural of 'box' produced '" + surface + "', not 'box'");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_adding_rule_priorities(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "deer", "deer");
            languageTable.add("English", "plural", "*x", "*xes");
            languageTable.add("English", "plural", "child", "children");
            languageTable.add("English", "plural", "*ch", "*ches");
            languageTable.add("English", "plural", "*", "*s");
            Collection rules = languageTable.list();
            if (rules.size() != 5) throw new Exception("Found " + rules.size() + " rules, exptected 5");
            Iterator it = rules.iterator();
            MorphologyRule rule = (MorphologyRule) it.next();
            if (rule.getRoot().equals("child") == false) throw new Exception("Rule 0 was " + rule.getRoot() + " not child");
            rule = (MorphologyRule) it.next();
            if (rule.getRoot().equals("deer") == false) throw new Exception("Rule 1 was " + rule.getRoot() + " not deer");
            rule = (MorphologyRule) it.next();
            if (rule.getRoot().equals("*ch") == false) throw new Exception("Rule 2 was " + rule.getRoot() + " not *ch");
            rule = (MorphologyRule) it.next();
            if (rule.getRoot().equals("*x") == false) throw new Exception("Rule 3 was " + rule.getRoot() + " not *x");
            rule = (MorphologyRule) it.next();
            if (rule.getRoot().equals("*") == false) throw new Exception("Rule 4 was " + rule.getRoot() + " not *");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_adding_duplicate_rule(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "*", "*s");
            languageTable.add("English", "plural", "*", "*s");
            Collection rules = languageTable.list();
            if (rules.size() != 1) throw new Exception("Found " + rules.size() + " rules, exptected 1");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_change_priorities(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "*s", "*ses");
            languageTable.add("English", "plural", "*x", "*xes");
            languageTable.add("English", "plural", "*ch", "*ches");
            languageTable.add("English", "plural", "*", "*s");
            String surface = languageTable.generate("English", "plural", "glass");
            if (surface.equals("glasses") == false) throw new Exception("1: Generating plural of 'glass' produced '" + surface + "', not 'glasses'");
            languageTable.moveRuleUp("English", "plural", 4);
            surface = languageTable.generate("English", "plural", "glass");
            if (surface.equals("glasss") == false) throw new Exception("2: Generating plural of 'glass' produced '" + surface + "', not 'glasss'");
            languageTable.moveRuleDown("English", "plural", 3);
            surface = languageTable.generate("English", "plural", "glass");
            if (surface.equals("glasses") == false) throw new Exception("3: Generating plural of 'glass' produced '" + surface + "', not 'glasses'");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_remove_rule(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "*s", "*ses");
            languageTable.add("English", "plural", "*", "*s");
            String surface = languageTable.generate("English", "plural", "glass");
            if (surface.equals("glasses") == false) throw new Exception("1: Generating plural of 'glass' produced '" + surface + "', not 'glasses'");
            languageTable.delete("English", "plural", 1);
            surface = languageTable.generate("English", "plural", "glass");
            if (surface.equals("glasss") == false) throw new Exception("2: Generating plural of 'glass' produced '" + surface + "', not 'glasss'");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_multi_match(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "*", "*s");
            languageTable.add("English", "third singular", "*", "*s");
            Collection recog = languageTable.recognize("English", "dogs");
            if (recog.size() != 2) throw new Exception("Recognizing 'dogs' produced " + recog.size() + ", not 2 result");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("dog") == false) throw new Exception("Recognizing 'dogs' produced '" + match.getRootString() + "', not 'dog'");
            Vector thirdSingularTag = new Vector();
            thirdSingularTag.add("third singular");
            Vector pluralTag = new Vector();
            pluralTag.add("plural");
            if ((thirdSingularTag.containsAll(match.getMorphologyTags()) == false) && (pluralTag.containsAll(match.getMorphologyTags()) == false)) {
                throw new Exception("Tags vectors did not match");
            }
            match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("dog") == false) throw new Exception("Recognizing 'dogs' produced '" + match.getRootString() + "', not 'dog'");
            if ((thirdSingularTag.containsAll(match.getMorphologyTags()) == false) && (pluralTag.containsAll(match.getMorphologyTags()) == false)) {
                throw new Exception("Tags vectors did not match");
            }
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_determine_use_flag(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "plural", "deer", "deer");
            Collection recog = languageTable.recognize("English", "deer");
            if (recog.size() != 1) throw new Exception("Recognizing 'deer' produced " + recog.size() + ", not 1 result");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("deer") == false) throw new Exception("Recognizing 'deer' produced '" + match.getRootString() + "', not 'deer'");
            Vector expTags = new Vector();
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_morph_type_does_not_exist(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            try {
                languageTable.add("English", "does not exist", "*", "*s");
                throw new Exception("Attempt to add rule with unknown morph tag worked??");
            } catch (MorphologyTagNotFoundException e) {
            }
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_reentrant(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "not", "*", "un*");
            languageTable.add("English", "again", "*", "re*");
            languageTable.add("English", "past tense", "*e", "*ed");
            languageTable.add("English", "past tense", "*", "*ed");
            Collection recog = languageTable.recognize("English", "unresolved");
            if (recog.size() != 2) throw new Exception("Recognizing 'unresolved' produced " + recog.size() + ", not 2 results");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if ((match.getRootString().equals("solv") == false) && (match.getRootString().equals("solve") == false)) throw new Exception("Recognizing 'unresolved' produced '" + match.getRootString() + "', not 'solv' or 'solve'");
            Vector expTags = new Vector();
            expTags.add("not");
            expTags.add("again");
            expTags.add("past tense");
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            match = (MorphologyRecognizeMatch) it.next();
            if ((match.getRootString().equals("solv") == false) && (match.getRootString().equals("solve") == false)) throw new Exception("Recognizing 'unresolved' produced '" + match.getRootString() + "', not 'solv' or 'solve'");
            expTags = new Vector();
            expTags.add("not");
            expTags.add("again");
            expTags.add("past tense");
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    private static void checkMatch(String root, Collection tags) throws Exception {
        if ((root.equals("solv") == false) && (root.equals("solve") == false)) throw new Exception("Recognizing 'unresolved' produced '" + root + "', not 'solv' or 'solve'");
        Vector expTags = new Vector();
        expTags.add("not");
        expTags.add("again");
        expTags.add("past tense");
        if (expTags.containsAll(tags) == false) {
            expTags.clear();
            expTags.add("not");
            expTags.add("again");
            expTags.add("third singular");
            if (expTags.containsAll(tags) == false) throw new Exception("Tags vectors did not match");
        }
    }

    public static void test_reentrant_multiple(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "not", "*", "un*");
            languageTable.add("English", "again", "*", "re*");
            languageTable.add("English", "past tense", "*e", "*ed");
            languageTable.add("English", "past tense", "*", "*ed");
            languageTable.add("English", "third singular", "*e", "*ed");
            languageTable.add("English", "third singular", "*", "*ed");
            Collection recog = languageTable.recognize("English", "unresolved");
            if (recog.size() != 4) throw new Exception("Recognizing 'unresolved' produced " + recog.size() + ", not 4 results");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            checkMatch(match.getRootString(), match.getMorphologyTags());
            match = (MorphologyRecognizeMatch) it.next();
            checkMatch(match.getRootString(), match.getMorphologyTags());
            match = (MorphologyRecognizeMatch) it.next();
            checkMatch(match.getRootString(), match.getMorphologyTags());
            match = (MorphologyRecognizeMatch) it.next();
            checkMatch(match.getRootString(), match.getMorphologyTags());
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }

    public static void test_long(PrintWriter out) {
        try {
            clearTables();
            LanguageMorphologyTable languageTable = new LanguageMorphologyTable();
            languageTable.add("English", "against", "*", "anti*");
            languageTable.add("English", "deprive", "*", "dis*");
            languageTable.add("English", "cause to happen", "*", "e*");
            languageTable.add("English", "capable of", "*", "*blish");
            languageTable.add("English", "nounify", "*", "*ment");
            languageTable.add("English", "someone who believes", "*", "*arian");
            languageTable.add("English", "belief system of", "*", "*ism");
            Collection recog = languageTable.recognize("English", "antidisestablishmentarianism");
            if (recog.size() != 1) throw new Exception("Recognizing 'unresolved' produced " + recog.size() + ", not 4 results");
            Iterator it = recog.iterator();
            MorphologyRecognizeMatch match = (MorphologyRecognizeMatch) it.next();
            if (match.getRootString().equals("sta") == false) throw new Exception("Recognizing 'antidisestablishmentarianism' produced '" + match.getRootString() + "', not 'sta'");
            Vector expTags = new Vector();
            expTags.add("against");
            expTags.add("deprive");
            expTags.add("cause to happen");
            expTags.add("capable of");
            expTags.add("nounify");
            expTags.add("someone who believes");
            expTags.add("belief system of");
            if (expTags.containsAll(match.getMorphologyTags()) == false) throw new Exception("Tags vectors did not match");
            testPassed(out);
        } catch (Exception e) {
            testFailed(e, out);
        }
    }
}

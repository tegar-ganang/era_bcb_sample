package fido.db;

import java.util.*;
import java.sql.*;
import fido.util.BooleanTree;
import fido.util.FidoDataSource;
import fido.grammar.GrammarParseException;

/**
 * The dictionary is a simple map that takes a String input, representing a word
 * in the sentence, and returns a collection of zero to many WordSenses.
 * Each WordSense is a distinct definition of a word.  Each word sense also
 * contains a grammar string that is used to connect words in the user
 * input.<P>
 * By default, the dictionary contains two special words.  These words cannot
 * be matched by words of the same name in a sentence because these words are
 * stored as all uppercase and the Word Separation
 * module converts all words from the sentence to lower case.
 * <OL>
 * <LI><I>LEFT-WALL</I> - This word is the left most word of a sentence.  It
 * provides grammar links to find the verb in a sentence.
 * <LI><I>UNKNOWN-WORD</I> - This word is the default if no other word matches
 * the String from the input sentence.  This allows the grammar parser to include
 * this word in the sentence and let the semantic processing determine the real
 * meaning.
 * </OL>
 *
 * @see fido.db.ObjectTable
 * @see fido.db.WordSense
 * @see fido.linguistic.WordSeparation
 */
public class DictionaryTable {

    /**
	 * Creates a new instance of DictionaryTable
	 */
    public DictionaryTable() {
    }

    /**
	 * Returns an array of Strings containing all of the words in
	 * the dictionary.  Each word may or may not contain word senses.
	 * The list of words are in alphabetical order.
	 * 
	 * @return array of all words
	 */
    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Word, SenseNumber, GrammarString, ObjectId " + "from Dictionary order by Word";
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                while (rs.next() == true) {
                    DictionaryEntry entry = new DictionaryEntry(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4));
                    list.add(entry);
                }
                return list;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Tests for the existance of a word in the dictionary.
	 * 
	 * @return true if the string is in the dictionary, even if it
	 *         has no senses.
	 */
    public boolean contains(String word) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select count(1) from Dictionary where Word = '" + word + "'";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new SQLException("No rows returned for count(1) query"); else {
                    int num = rs.getInt(1);
                    if (num == 0) return false; else return true;
                }
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private int max(Statement stmt, String name) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select max(SenseNumber) from Dictionary where Word = '" + name + "'";
            int row;
            rs = stmt.executeQuery(sql);
            if (rs.next() == true) return rs.getInt(1) + 1; else return 1;
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
	 * Adds a new word sense to a word.  If the word does not exists
	 * in the Dictionary, a new word is created.  After adding the
	 * word sense, the Dictionary is stored in the database.
	 * If the representedObject entry of the WordSense is not null, then a
	 * link between the object in the ObjectTable and this word
	 * is created with the system link type <i>Word</i>.
	 * 
	 * @param word word to add the word sense to
	 * @param sense word sense to add
	 * 
	 * @exception SQLException thrown if there is a Input / Output error
	 *            saving the Dictionary.
	 * @exception LinkTypeNotFoundException thrown if the LinkTypeList does not
	 *            contain the system link <i>Word</i>
	 * 
	 * @return row number of the new word sense within the word
	 */
    public int add(String word, String grammarString, int objectId) throws FidoDatabaseException, GrammarLinkNotFoundException, GrammarParseException, ObjectNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                BooleanTree tmp = GrammarExpressions.parseExpression(grammarString);
                ObjectTable ot = new ObjectTable();
                if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int row = max(stmt, word);
                String sql = "insert into Dictionary (Word, SenseNumber, GrammarString, ObjectId) " + "values ('" + word + "', " + row + ", '" + grammarString + "', " + objectId + ")";
                stmt.executeUpdate(sql);
                return row;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Replaces a word sense of a word specified by the row number.
	 * 
	 * @param word word to replace the word sense in
	 * @param row used to specify which word sense to replace.
	 * @param sense word sense to add
	 * 
	 * @exception FidoDatabaseException thrown if there is a Input / Output error
	 *            saving the Dictionary.
	 * @exception WordNotFoundException word parameter does not exists
	 *            in the Dictionary
	 */
    public void modify(String word, int row, String grammarString, int objectId) throws FidoDatabaseException, GrammarLinkNotFoundException, GrammarParseException, ObjectNotFoundException, WordNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                BooleanTree tmp = GrammarExpressions.parseExpression(grammarString);
                ObjectTable ot = new ObjectTable();
                if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                String sql = "update Dictionary set GrammarString = '" + grammarString + "', ObjectId = '" + objectId + "' where word = '" + word + "' and SenseNumber = " + row;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int rows = stmt.executeUpdate(sql);
                if (rows == 0) throw new WordNotFoundException(word);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Removes a word sense of a word specified by the row number.
	 * 
	 * @param word word to remove the word sense from
	 * @param row used to specify which word sense to remove
	 * 
	 * @exception FidoDatabaseException thrown if there is a Input / Output error
	 *            saving the Dictionary.
	 */
    public void delete(String word, int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = max(stmt, word);
                String sql = "delete from Dictionary where Word = '" + word + "' and SenseNumber = " + row;
                stmt.executeUpdate(sql);
                for (int i = row; i < max; ++i) {
                    stmt.executeUpdate("update Dictionary set SenseNumber = " + i + " where SenseNumber = " + (i + 1) + " and Word = '" + word + "'");
                }
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Retrieves a specified entry from the dictionary specified by
	 * word.
	 * 
	 * @return a Vector containing all of the WordSenses for the word.  If
	 *         the word is not found, an empty Vector is returned.
	 */
    public Collection lookupWord(String word) throws FidoDatabaseException, GrammarParseException, GrammarLinkNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select GrammarString, ObjectId from Dictionary where word = '" + word + "'";
                Vector copy = new Vector();
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                while (rs.next() == true) {
                    String gs = rs.getString(1);
                    BooleanTree bt = GrammarExpressions.parseExpression(gs);
                    WordSense ws = new WordSense(bt, gs, rs.getInt(2));
                    copy.add(ws);
                }
                return copy;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Scans all of the dictionary words for a reference to the class and removes
	 * the reference.
	 * 
	 * @param objectName name of the class to search for and remove
	 * 
	 * @exception FidoDatabaseException thrown if there is a Input / Output error
	 *            saving the Dictionary.
	 */
    public void deleteReferenceToObject(Statement stmt, String objectId) throws SQLException {
        String sql = "update Dictionary set ObjectId = -1 where ObjectId = '" + objectId + "'";
        stmt.executeUpdate(sql);
    }

    /**
	 * Creates a hash value for a given row.  This is a unique integer
	 * value that represents all of the columns for that row.  This is used
	 * by data entry servlets to ensure the row was not updated between the
	 * time the row data was downloaded, and the time the user sent the update.
	 * 
	 * @exception FidoDatabaseException Thrown if the database had an error.
	 * @exception WordNotFoundException Thrown if the parameters did not specify
	 *                                  a row in the table.
	 * 
	 * @return A unique integer representing the data for the row.
	 */
    public int hashCode(String word, String row) throws FidoDatabaseException, WordNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select GrammarString, ObjectId " + "from Dictionary " + "where Word = '" + word + "' and SenseNumber = " + row;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                if (rs.next() == false) throw new WordNotFoundException(word);
                Vector list = new Vector();
                list.add(word);
                list.add(row);
                list.add(rs.getString(1));
                list.add(rs.getString(2));
                return list.hashCode();
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }
}

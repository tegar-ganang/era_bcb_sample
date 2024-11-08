package fido.db;

import java.sql.*;
import java.util.*;
import fido.util.BooleanTree;
import fido.util.FidoDataSource;
import fido.grammar.GrammarParseException;

/**
 * This class maintains a list of word classifications, which are patterns
 * used to classify words based on the characters that compise it.  The
 * list of classifications are stored in an ordered table, so that the
 * order of the patterns are maintained.  Also, <CODE>moveRowUp()</CODE>
 * and <CODE>moveRowDown()</CODE> can change the priority of a classification.
 * The order is importantant, as more generic patterns near the end match a
 * larger group of words.<P>
 * Patterns:<P>
 * <TABLE BORDER=1>
 * <TH>Character</TH><TH>Description</TH>
 * <TR>
 * 	<TD>N</TD>
 * 	<TD>a number 0 to 9 repeating one to many times</TD>
 * </TR>
 * <TR>
 * 	<TD>A</TD>
 * 	<TD>alpha character a to z and A to Z repeating one to many times</TD>
 * </TR>
 * <TR>
 * 	<TD>E</TD>
 * 	<TD>Either number or alpha repeating one to many times.  In practice,
 * 	    these are usually hostnames, variables, or some computer identifier.
 * 	    Therefore, this class will require the first character to be an
 * 	    alpha.</TD>
 * </TR>
 * <TR>
 * 	<TD>C</TD>
 * 	<TD>Any character, exception forward slash and back slash,
 * 	    repeating one to many times.  This type is used in
 * 	    Unix and Dos path names.</TD>
 * </TR>
 * <TR>
 * 	<TD>[]</TD>
 * 	<TD>All chars listed optionally appearing once</TD>
 * </TR>
 * <TR>
 * 	<TD>{}</TD>
 * 	<TD>Any char listed optionally appearing once</TD>
 * </TR>
 * <TR>
 * 	<TD>()</TD>
 * 	<TD>All chars listend repeating at least once, possibly many times</TD>
 * </TR>
 * </TABLE><P>
 * The following table contains sample patterns of word classes:<BR>
 * (Note: Order in the patterns is significant.  Therefore, more general
 * patterns should appear at the bottom of the list)<P>
 * <TABLE BORDER=1>
 * <TH>Pattern</TH><TH>Class Type</TH><TH>Notes</TH>
 * <TR>
 * 	<TD>$N[.N]</TD>
 * 	<TD>Money value</TD>
 * 	<TD>The '$' (dollar sign) is not a special character, therefore
 * 	    must be matched explicitly.  The pattern reads:  A word
 * 	    starting with a dollar sign, zero to many numbers, and
 * 	    optionally, a decimal point and more numbers.</TD>
 * </TR>
 * <TR>
 * 	<TD>N/N/N</TD>
 * 	<TD>Date</TD>
 * 	<TD>The slashes in this pattern must match explicitly.</TD>
 * </TR>
 * <TR>
 * 	<TD>(C/)C</TD>
 * 	<TD>Unix path</TD>
 * 	<TD>This pattern looks for any character besides a slash, as
 * 	    specified by the 'C'.  Then matches a slash.  This pattern can
 * 	    be repeated as many times as necessary.  The final 'C'
 * 	    accepts any characters following the final slash.  Since
 * 	    the letter classes, such as 'C', can match zero characters,
 * 	    the word can start or end in a slash.</TD>
 * </TR>
 * <TR>
 * 	<TD>[A:](C\)C</TD>
 * 	<TD>Dos path</TD>
 * 	<TD>Similar to the Unix path pattern, this pattern accepts
 * 	    an alpha character followed by a colon for Dos drive letters.</TD>
 * </TR>
 * <TR>
 * 	<TD>N:N</TD>
 * 	<TD>Time</TD>
 * 	<TD>Two numbers separated by a colon.  Note the pattern does
 * 	    not require the first number to be in the range of 0 to 
 * 	    23.  Nor does it limit the first number to be only two
 * 	    digits.</TD>
 * </TR>
 * <TR>
 * 	<TD>N.N.N.N</TD>
 * 	<TD>IP Address</TD>
 * 	<TD>As in the Time pattern, the individual components do not
 * 	    have to be valid ip numbers.  This is one example where
 * 	    order in the table is important, because this pattern will
 * 	    catch four numbers separated by periods.  The next pattern
 * 	    will catch four groups of characters if this pattern
 * 	    does not match.</TD>
 * </TR>
 * <TR>
 * 	<TD>E(.E)</TD>
 * 	<TD>Internet hostname</TD>
 * 	<TD>Matches a string of alpha and numeric characters, followed
 * 	    by at least one period, then more alpha and numeric chars.
 * 	    Matches internet hostnames such as www.yahoo.com</TD>
 * </TR>
 * <TR>
 * 	<TD>{+-}N[.N]</TD>
 * 	<TD>Number</TD>
 * 	<TD>The first part of this pattern allows either a plus or
 * 	    minus to precede the number, but does not require one.</TD>
 * </TR>
 * <TR>
 * 	<TD>E</TD>
 * 	<TD>Default</TD>
 * 	<TD>This is a catch all pattern if any of the above patterns
 * 	    fail to match.</TD>
 * </TR>
 * </TABLE>
 */
public class WordClassificationTable {

    /**
	 * Creates a new WordClassificationTable instance.
	 */
    public WordClassificationTable() {
    }

    /**
	 * Returns a array of Strings containing all Word Classifications.  A
	 * row will contain the unique identifier, the pattern to match, and
	 * the word sense values: a grammar string and the link to the object
	 * in the ObjectList.
	 *
	 * @return list of all classifications fit to be displayed in a table
	 */
    public Collection list() throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Rank, Pattern, GrammarString, ObjectId, Description " + "from WordClassifications order by Rank";
                conn = FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector classes = new Vector();
                while (rs.next() == true) {
                    WordClassification cls = new WordClassification(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
                    classes.add(cls);
                }
                return classes;
            } finally {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    private int findMaxRank(Statement stmt) throws SQLException {
        ResultSet rs = null;
        try {
            String sql = "select max(Rank) from WordClassifications";
            rs = stmt.executeQuery(sql);
            if (rs.next() == false) throw new SQLException("No rows returned for select max() query"); else {
                int num = rs.getInt(1);
                return num;
            }
        } finally {
            if (rs != null) rs.close();
        }
    }

    /**
	 * Moves the row specified by <i>row</i> up in priority.  The String <i>row</i>
	 * must be a number.
	 * 
	 * @exception SQLException Input / Output error saving WordClassifications object
	 *            to database
	 */
    public void moveRowUp(int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = findMaxRank(stmt);
                if ((row < 2) || (row > max)) throw new IllegalArgumentException("Row number not between 2 and " + max);
                stmt.executeUpdate("update WordClassifications set Rank = -1 where Rank = " + row);
                stmt.executeUpdate("update WordClassifications set Rank = " + row + " where Rank = " + (row - 1));
                stmt.executeUpdate("update WordClassifications set Rank = " + (row - 1) + " where Rank = -1");
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
	 * Moves the row specified by <i>row</i> down in priority.  The String <i>row</i>
	 * must be a number.
	 * 
	 * @exception FidoIOException Input / Output error saving WordClassifications object
	 *            to database
	 */
    public void moveRowDown(int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = findMaxRank(stmt);
                if ((row < 1) || (row > (max - 1))) throw new IllegalArgumentException("Row number not between 1 and " + (max - 1));
                stmt.executeUpdate("update WordClassifications set Rank = -1 where Rank = " + row);
                stmt.executeUpdate("update WordClassifications set Rank = " + row + " where Rank = " + (row + 1));
                stmt.executeUpdate("update WordClassifications set Rank = " + (row + 1) + " where Rank = -1");
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
	 * Adds a new classification to the end of the list.  Since the pattern is
	 * at the end, this will be the last pattern the word will be compared to.
	 * if any pattern above this one matches, this pattern will not be used.
	 * 
	 * @param pattern Regular expression type string for matching word characters
	 * @param sense WordSense used to represent this word
	 * 
	 * @exception FidoIOException Input / Output error saving WordClassifications object
	 *            to database
	 */
    public void add(String pattern, String grammarString, int objectId, String description) throws FidoDatabaseException, ObjectNotFoundException, GrammarParseException, GrammarLinkNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                BooleanTree tmp = GrammarExpressions.parseExpression(grammarString);
                ObjectTable ot = new ObjectTable();
                if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int max = findMaxRank(stmt);
                String sql = "insert into WordClassifications (Rank, Pattern, GrammarString, ObjectId, Description) " + "values (" + (max + 1) + ", '" + pattern + "', '" + grammarString + "', '" + objectId + "', '" + description + "')";
                stmt.executeUpdate(sql);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Replaces the new classification at the row specified in the <i>row</i> paramter
	 * with a row containing the parameter information.
	 * 
	 * @param row integer row value to replace
	 * @param pattern Regular expression type string for matching word characters
	 * @param sense WordSense used to represent this word
	 * 
	 * @exception FidoIOException Input / Output error saving WordClassifications object
	 *            to database
	 */
    public void modify(int row, String pattern, String grammarString, int objectId, String description) throws FidoDatabaseException, ObjectNotFoundException, GrammarParseException, GrammarLinkNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                BooleanTree tmp = GrammarExpressions.parseExpression(grammarString);
                ObjectTable ot = new ObjectTable();
                if (ot.contains(objectId) == false) throw new ObjectNotFoundException(objectId);
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                int max = findMaxRank(stmt);
                if ((row < 1) || (row > max)) throw new IllegalArgumentException("Row number not between 1 and " + max);
                String sql = "update WordClassifications set Pattern = '" + pattern + "', GrammarString = '" + grammarString + "', ObjectId = '" + objectId + "', Description = '" + description + "' where Rank = " + row;
                stmt.executeUpdate(sql);
            } finally {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
        } catch (SQLException e) {
            throw new FidoDatabaseException(e);
        }
    }

    /**
	 * Removes the new classification at the row specified in the <i>row</i> paramter.
	 * 
	 * @param row integer row value to replace
	 * 
	 * @exception FidoIOException Input / Output error saving WordClassifications object
	 *            to database
	 */
    public void delete(int row) throws FidoDatabaseException {
        try {
            Connection conn = null;
            Statement stmt = null;
            try {
                conn = fido.util.FidoDataSource.getConnection();
                conn.setAutoCommit(false);
                stmt = conn.createStatement();
                int max = findMaxRank(stmt);
                if ((row < 1) || (row > max)) throw new IllegalArgumentException("Row number not between 1 and " + max);
                stmt.executeUpdate("delete from WordClassifications where Rank = " + row);
                for (int i = row; i < max; ++i) stmt.executeUpdate("update WordClassifications set Rank = " + i + " where Rank = " + (i + 1));
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

    private int parseGroup(char endChar, String subPattern) {
        int i;
        for (i = 0; i < subPattern.length(); ++i) if (subPattern.charAt(i) == endChar) break;
        return i + 1;
    }

    private int eatAllNumbers(String str, int startAt) {
        int i;
        for (i = startAt; i < str.length(); ++i) if (Character.isDigit(str.charAt(i)) == false) break;
        return i;
    }

    private int eatAllAlpha(String str, int startAt) {
        int i;
        for (i = startAt; i < str.length(); ++i) if (Character.isLetter(str.charAt(i)) == false) break;
        return i;
    }

    private int eatAllEither(String str, int startAt) {
        int i;
        boolean first = true;
        for (i = startAt; i < str.length(); ++i) {
            if ((first == true) && (Character.isLetter(str.charAt(i)) == false)) break; else if (Character.isLetterOrDigit(str.charAt(i)) == false) break;
            first = false;
        }
        return i;
    }

    private int eatAnyCharacter(String str, int startAt) {
        int i;
        for (i = startAt; i < str.length(); ++i) {
            char c = str.charAt(i);
            if ((c == '/') || (c == '\\')) break;
        }
        return i;
    }

    private boolean eatLiteralCharacter(String str, char c, int startAt) {
        if (startAt >= str.length()) return false; else if (str.charAt(startAt) != c) return false;
        return true;
    }

    private int eatAllInGroup(String str, int stringIndex, String pattern) {
        int startAt = stringIndex;
        for (int i = 0; i < pattern.length(); ++i) {
            char c = pattern.charAt(i);
            if (c == 'N') stringIndex = eatAllNumbers(str, stringIndex); else if (c == 'A') stringIndex = eatAllAlpha(str, stringIndex); else if (c == 'E') stringIndex = eatAllEither(str, stringIndex); else if (c == 'C') stringIndex = eatAnyCharacter(str, stringIndex); else {
                if (eatLiteralCharacter(str, c, stringIndex) == false) return startAt;
                ++stringIndex;
            }
        }
        return stringIndex;
    }

    private int eatAnyInGroup(String str, int stringIndex, String pattern) {
        for (int i = 0; i < pattern.length(); ++i) {
            char c = pattern.charAt(i);
            if (c == 'N') {
                int move = eatAllNumbers(str, stringIndex);
                if (move != stringIndex) return move;
            } else if (c == 'A') {
                int move = eatAllAlpha(str, stringIndex);
                if (move != stringIndex) return move;
            } else if (c == 'E') {
                int move = eatAllEither(str, stringIndex);
                if (move != stringIndex) return move;
            } else if (c == 'C') {
                int move = eatAnyCharacter(str, stringIndex);
                if (move != stringIndex) return move;
            } else {
                if (eatLiteralCharacter(str, c, stringIndex) == true) return stringIndex + 1;
            }
        }
        return stringIndex;
    }

    private boolean isValid(String str, String pattern) {
        int stringIndex = 0;
        int patternIndex;
        for (patternIndex = 0; patternIndex < pattern.length(); ++patternIndex) {
            char c = pattern.charAt(patternIndex);
            if (c == 'N') stringIndex = eatAllNumbers(str, stringIndex); else if (c == 'A') stringIndex = eatAllAlpha(str, stringIndex); else if (c == 'E') stringIndex = eatAllEither(str, stringIndex); else if (c == 'C') stringIndex = eatAnyCharacter(str, stringIndex); else if (c == '(') {
                int j = parseGroup(')', pattern.substring(patternIndex + 1));
                String group = pattern.substring(patternIndex + 1, j + patternIndex);
                patternIndex += j;
                boolean atLeastOne = false;
                while (true) {
                    int move = eatAllInGroup(str, stringIndex, group);
                    if (move == stringIndex) break;
                    atLeastOne = true;
                    stringIndex = move;
                }
                if (atLeastOne == false) return false;
            } else if (c == '{') {
                int j = parseGroup('}', pattern.substring(patternIndex + 1));
                String group = pattern.substring(patternIndex + 1, j + patternIndex);
                patternIndex += j;
                stringIndex = eatAnyInGroup(str, stringIndex, group);
            } else if (c == '[') {
                int j = parseGroup(']', pattern.substring(patternIndex + 1));
                String group = pattern.substring(patternIndex + 1, j + patternIndex);
                patternIndex += j;
                stringIndex = eatAllInGroup(str, stringIndex, group);
            } else {
                if (eatLiteralCharacter(str, c, stringIndex) == false) return false;
                ++stringIndex;
            }
        }
        if ((stringIndex == str.length()) && (patternIndex == pattern.length())) return true;
        return false;
    }

    /**
	 * Attempts to match the list of patterns in the WordClassifications to the
	 * user string parameter.  The first match that is found, a WordSense object
	 * is returned that contains a GrammarString to be used to insert the word
	 * into the sentence grammar, and a pointer to the object in the ObjectHierarchy
	 * that is used to represent the word in processing.
	 * 
	 * @return A WordSense for this word pattern, null if no pattern matches
	 */
    public WordSense classify(String str) throws FidoDatabaseException, GrammarParseException, GrammarLinkNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Pattern, GrammarString, ObjectId from WordClassifications " + "order by Rank";
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                while (rs.next() == true) {
                    if (isValid(str, rs.getString(1)) == true) {
                        String gs = rs.getString(2);
                        BooleanTree tree = GrammarExpressions.parseExpression(gs);
                        WordSense ws = new WordSense(tree, gs, rs.getInt(3));
                        return ws;
                    }
                }
                return null;
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
	 * Creates a hash value for a given row.  This is a unique integer
	 * value that represents all of the columns for that row.  This is used
	 * by data entry servlets to ensure the row was not updated between the
	 * time the row data was downloaded, and the time the user sent the update.
	 * 
	 * @exception FidoDatabaseException Thrown if the database had an error.
	 * @exception ClassificationNotFoundException Thrown if the parameters did not specify
	 *                                            a row in the table.
	 * 
	 * @return A unique integer representing the data for the row.
	 */
    public int hashCode(String rank) throws FidoDatabaseException, ClassificationNotFoundException {
        try {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;
            try {
                String sql = "select Pattern, GrammarString, ObjectId, Description " + "from WordClassifications where Rank = " + rank;
                conn = fido.util.FidoDataSource.getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
                Vector list = new Vector();
                if (rs.next() == false) throw new ClassificationNotFoundException(rank);
                list.add(rs.getString(1));
                list.add(rs.getString(2));
                list.add(rs.getString(3));
                list.add(rs.getString(4));
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

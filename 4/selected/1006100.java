package de.joergjahnke.jdesktopsearch.abstractionlayer;

import de.joergjahnke.common.util.StringUtils;
import de.joergjahnke.jdesktopsearch.QueryUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.RangeQuery;

/**
 * Implementation of the AbstractIndexManager for SQL Servers
 * 
 * @author J�rg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class SQLServerIndexManager extends AbstractIndexManager {

    private static final long CLEANUP_TIMER = 5 * 60 * 1000;

    private static final boolean DEBUG = false;

    private final String serverType;

    private final String serverVersion;

    private final String serverName;

    private final int serverPort;

    private final String user;

    private final String password;

    private String[] searchFields = null;

    private volatile Connection con = null;

    private volatile CleanupThread cleanupThread = null;

    private volatile Date lastCleanupInit = null;

    /**
     * Creates a new instance of SQLServerIndexManager
     */
    public SQLServerIndexManager(final Properties properties) {
        super(properties);
        this.serverType = properties.getProperty("indexer_type");
        this.serverVersion = properties.getProperty("sqlserver_version");
        this.serverName = properties.getProperty("sqlserver_name");
        this.serverPort = null == properties.getProperty("sqlserver_port") ? -1 : Integer.parseInt(properties.getProperty("sqlserver_port"));
        this.user = properties.getProperty("sqlserver_user");
        this.password = properties.getProperty("sqlserver_password");
    }

    /**
     * Get database connection
     *
     * @return  database connection
     */
    protected Connection getConnection() throws SQLException, ClassNotFoundException {
        String dbName = null;
        if (this.indexName.lastIndexOf(File.separator) >= 0) {
            dbName = this.indexName.substring(this.indexName.lastIndexOf(File.separator) + 1, this.indexName.indexOf(".", this.indexName.lastIndexOf(File.separator)));
        } else {
            dbName = this.indexName;
        }
        final String connectURI = "jdbc:jtds:" + serverType + "://" + serverName + (serverPort >= 0 ? ":" + serverPort : "") + "/" + dbName + ";user=" + user + ";password=" + password + ";TDS=" + serverVersion + ";appname=JDesktopSearch";
        DriverManager.setLoginTimeout(30);
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        if (DEBUG) System.err.println(this.getClass().getName() + ": ConnectURI: " + connectURI);
        return DriverManager.getConnection(connectURI);
    }

    /**
     * Get database connection.
     * Tries to reuse an existing connection which does not need to be closed.
     * This class tries to automatically free connections after a preset time.
     */
    private synchronized Connection getAutoClosingConnection() throws Exception {
        boolean createNewCleanupThread = false;
        if (null == this.con || this.con.isClosed()) {
            this.con = getConnection();
            createNewCleanupThread = true;
        } else if (null != this.cleanupThread && this.cleanupThread.isAlive() && (this.lastCleanupInit.getTime() + CLEANUP_TIMER / 2 > new Date().getTime())) {
            this.cleanupThread.interrupt();
            createNewCleanupThread = true;
        }
        if (createNewCleanupThread) {
            this.cleanupThread = new CleanupThread(this.con);
            this.cleanupThread.start();
            this.lastCleanupInit = new Date();
        }
        return this.con;
    }

    public Set<String> getRootDirectories(String newParam) {
        final Set<String> result = new HashSet<String>();
        try {
            Connection con = getAutoClosingConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Name FROM RootDir_");
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    public void addRootDirectory(final String filename) throws IOException {
        if (!this.getRootDirectories("").contains(filename)) {
            try {
                Connection con = getAutoClosingConnection();
                PreparedStatement stmt = con.prepareStatement("INSERT INTO RootDir_ ( Name ) VALUES ( ? )");
                stmt.setString(1, filename);
                stmt.executeUpdate();
                stmt.close();
                stmt = null;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }
        }
    }

    protected int getNumberOfIndexedFiles() throws IOException {
        try {
            Connection con = getAutoClosingConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM File_");
            int size = 0;
            if (null != rs && rs.next()) {
                size = rs.getInt(1);
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            return size;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    protected Map<String, Integer> getNumberOfIndexedFilesPerType() throws IOException {
        final Map<String, Integer> result = new HashMap<String, Integer>();
        try {
            Connection con = getAutoClosingConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT REVERSE( SUBSTRING( REVERSE( Name ), 1, CHARINDEX( '.', REVERSE( Name ) ) ) ) AS \"Extension\", COUNT(*) AS \"Count\"" + " FROM File_" + " GROUP BY REVERSE( SUBSTRING( REVERSE( Name ), 1, CHARINDEX( '.', REVERSE( Name ) ) ) )" + " ORDER BY \"Extension\"");
            while (rs.next()) {
                result.put(rs.getString("Extension"), rs.getInt("Count"));
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
        return result;
    }

    public Long getDocumentFileDate(final String filename) {
        Long result = null;
        try {
            Connection con = getAutoClosingConnection();
            PreparedStatement stmt = con.prepareStatement("SELECT Word_" + " FROM Token_ T" + " INNER JOIN Field_ FL ON T.FieldID = FL.ID" + " INNER JOIN File_ F ON FL.FileID = F.ID" + " WHERE FL.Name = 'modified'" + "   AND F.Name = ?");
            stmt.setString(1, filename);
            ResultSet rs = stmt.executeQuery();
            if (null != rs && rs.next()) {
                result = DateTools.stringToTime(rs.getString(1));
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    public Set<String> listFiles(String directory) {
        final Set<String> result = new HashSet<String>();
        try {
            Connection con = getAutoClosingConnection();
            PreparedStatement stmt = con.prepareStatement("SELECT Name FROM File_ WHERE Name LIKE ? AND Name NOT LIKE ?");
            if (!directory.endsWith(File.separator)) directory += File.separator;
            stmt.setString(1, directory + "%");
            stmt.setString(2, directory + "%" + File.separator + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getString(1));
            }
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    /**
     * Create sql query from a Lucene query
     *
     * @param   query   contains query parameters
     * @return  Map containing a part of the where clause to be create and the
     *              corresponding value that needs to be inserted into this
     *              part in a PreparedStatement
     */
    private PreparedStatement getSQLStatement(Connection con, Query query, String searchPath) throws SQLException {
        final WhereClauseParameters whereClauseSearchInTokens = getSQLWhereClause(query, "AND", "FL.Name = ? AND EXISTS ( SELECT * FROM Token_ T WHERE FL.ID = T.FieldID AND T.Word_ $OPERATOR ? )");
        final WhereClauseParameters whereClauseSearchInFields = getSQLWhereClause(query, "AND", "FL.Name = ? AND FL.IsIndexable = 1 AND FL.IsCompressed = 0 AND FL.Value LIKE '%' + ? + '%'");
        final WhereClauseParameters whereClauseSearchTerms = getSearchTermClause(query);
        if (DEBUG) System.err.println(this.getClass().getName() + ": SearchTerms: " + whereClauseSearchTerms.getSQL() + ", " + whereClauseSearchTerms.getParameters());
        String sql = "SELECT FileID AS \"FileID\", SUM( Rating ) AS \"Rating\"" + " FROM (" + "  SELECT F.ID AS \"FileID\", 1.0 / CASE WHEN FL.FullValue IS NULL THEN LEN( FL.Value ) ELSE CASE WHEN IsCompressed = 1 THEN 2 ELSE 1 END * DATALENGTH( FullValue ) END AS \"Rating\"" + "   FROM File_ F" + "   INNER JOIN Field_ FL ON F.ID = FL.FileID" + "   WHERE 1=1" + (null != searchPath && !"".equals(searchPath) ? " AND F.Name LIKE ?" : "") + whereClauseSearchInTokens.getSQL() + "     AND FL.ID IN ( SELECT FieldID FROM Token_ WHERE 1=2" + whereClauseSearchTerms.getSQL() + "     )" + "    UNION " + "  SELECT F.ID AS \"FileID\", 1.0 / CASE WHEN FL.FullValue IS NULL THEN LEN( FL.Value ) ELSE CASE WHEN IsCompressed = 1 THEN 2 ELSE 1 END * DATALENGTH( FullValue ) END AS \"Rating\"" + "   FROM File_ F" + "   INNER JOIN Field_ FL ON F.ID = FL.FileID" + "   WHERE 1=1" + (null != searchPath && !"".equals(searchPath) ? " AND F.Name LIKE ?" : "") + whereClauseSearchInFields.getSQL() + ") A" + " GROUP BY FileID" + " ORDER BY Rating DESC";
        if (DEBUG) System.err.println(this.getClass().getName() + ": SQL= " + sql);
        sql = sql.replaceAll("1=1 AND ", "");
        sql = sql.replaceAll("1=2 OR ", "");
        if (DEBUG) System.err.println(this.getClass().getName() + ": SQL= " + sql);
        final PreparedStatement stmt = con.prepareStatement(sql);
        int p = 1;
        if (null != searchPath && !"".equals(searchPath)) {
            stmt.setString(p, searchPath + "%");
            ++p;
        }
        for (String param : whereClauseSearchInTokens.getParameters()) {
            stmt.setString(p, param);
            ++p;
        }
        for (String param : whereClauseSearchTerms.getParameters()) {
            stmt.setString(p, param);
            ++p;
        }
        if (null != searchPath && !"".equals(searchPath)) {
            stmt.setString(p, searchPath + "%");
            ++p;
        }
        for (String param : whereClauseSearchInFields.getParameters()) {
            stmt.setString(p, param);
            ++p;
        }
        return stmt;
    }

    private void processTermQuery(final StringBuffer sql, final Collection<String> parameters, final Query query, final String logicalOperator, final String termQuery) {
        Term term = null;
        String operator = null;
        if (query instanceof TermQuery) {
            term = ((TermQuery) query).getTerm();
            operator = "=";
        } else if (query instanceof WildcardQuery) {
            term = ((WildcardQuery) query).getTerm();
            operator = "LIKE";
        } else if (query instanceof PrefixQuery) {
            Term prefixTerm = ((PrefixQuery) query).getPrefix();
            term = new Term(prefixTerm.field(), prefixTerm.text() + "*");
            operator = "LIKE";
        }
        sql.append(" " + logicalOperator + " ( " + termQuery.replaceAll("\\$OPERATOR", operator) + " )");
        parameters.add(term.field());
        String text = term.text();
        if (DEBUG) System.err.println(this.getClass().getName() + ": T= " + text);
        text = text.replaceAll("\\*", "\\%");
        text = text.replaceAll("\\_", "\\[\\_\\]");
        text = text.replaceAll("\\?", "\\_");
        if (DEBUG) System.err.println(this.getClass().getName() + ": T= " + text);
        parameters.add(text);
    }

    private void processPhraseQuery(final StringBuffer sql, final Collection<String> parameters, final Query query, final String logicalOperator, final String termQuery) {
        final StringBuffer phrase = new StringBuffer();
        final PhraseQuery phraseQuery = (PhraseQuery) query;
        for (Term phraseTerm : phraseQuery.getTerms()) {
            phrase.append(phraseTerm.text());
        }
        sql.append(" " + logicalOperator + " ( " + termQuery.replaceAll("\\$OPERATOR", "=") + " )");
        String text = phrase.toString();
        text = text.replaceAll("\\_", "\\[\\_\\]");
        if (DEBUG) System.err.println(this.getClass().getName() + ": T= " + text);
        parameters.add(phraseQuery.getTerms()[0].field());
        parameters.add(text);
    }

    private void processRangeQuery(final StringBuffer sql, final Collection<String> parameters, final Query query, final String logicalOperator, final String termQuery) {
        final String operator1 = ((RangeQuery) query).isInclusive() ? ">=" : ">";
        final String operator2 = ((RangeQuery) query).isInclusive() ? "<=" : "<";
        sql.append(" " + logicalOperator + " ( " + termQuery.replaceAll("\\$OPERATOR", operator1) + " OR " + termQuery.replaceAll("\\$OPERATOR", operator2) + " )");
        final Term lowerTerm = ((RangeQuery) query).getLowerTerm();
        final Term upperTerm = ((RangeQuery) query).getUpperTerm();
        String lowerText = lowerTerm.text();
        String upperText = upperTerm.text();
        lowerText = lowerText.replaceAll("\\_", "\\[\\_\\]");
        upperText = upperText.replaceAll("\\_", "\\[\\_\\]");
        parameters.add(lowerTerm.field());
        parameters.add(lowerText);
        parameters.add(upperTerm.field());
        parameters.add(upperText);
    }

    /**
     * Optimize a set a sub-queries.
     * If the given queries have similarities and can be merged to one query
     * then this method will clear the query list and the parameter list and
     * fill them with one element each which will be the optimized query with
     * the optimized parameter list.
     *
     * @param   sqlList list of queries to optimize
     * @param   paramsList  list of parameters for the queries
     */
    private void optimizeQuery(final List<String> sqlList, final List<Collection<String>> paramsList) {
        if (sqlList.size() > 1) {
            boolean areAllSQLEqual = new HashSet<String>(sqlList).size() == 1;
            if (areAllSQLEqual) {
                boolean isOneFieldMultipleValuesAtSamePosition = true;
                final Set<String> allFields = new HashSet<String>();
                List<String> lastValues = null;
                for (Collection<String> paramPart : paramsList) {
                    final Set<String> fields = new HashSet<String>();
                    final List<String> values = new LinkedList<String>();
                    boolean isField = true;
                    for (String param : paramPart) {
                        if (isField) {
                            fields.add(param);
                            if (fields.size() > 1) {
                                isOneFieldMultipleValuesAtSamePosition = false;
                                break;
                            }
                        } else {
                            values.add(param);
                        }
                        isField = !isField;
                    }
                    if (!isOneFieldMultipleValuesAtSamePosition || (null != lastValues && !lastValues.equals(values))) {
                        isOneFieldMultipleValuesAtSamePosition = false;
                        break;
                    } else {
                        allFields.addAll(fields);
                        lastValues = values;
                    }
                }
                if (isOneFieldMultipleValuesAtSamePosition) {
                    String newsql = sqlList.get(0).replaceAll("FL.Name = \\?", "FL.Name IN ( \\?" + StringUtils.repeat(",\\?", allFields.size() - 1) + " )");
                    final Collection<String> newparams = new LinkedList<String>();
                    for (String value : lastValues) {
                        newparams.addAll(allFields);
                        newparams.add(value);
                    }
                    sqlList.clear();
                    sqlList.add(newsql);
                    paramsList.clear();
                    paramsList.add(newparams);
                    if (DEBUG) System.err.println(this.getClass().getName() + ": Optimization: New SQL= " + sqlList);
                    if (DEBUG) System.err.println(this.getClass().getName() + ": Optimization: New Parameters= " + paramsList);
                }
            }
        }
    }

    /**
     * Append new parts to a where clause
     *
     * @param   sqlList list of sql snippets to append, will be cleared after appending
     * @param   paramsList  list of parameter-lists to append, will be cleared after appending
     * @param   sql sql command to be appended to
     * @param   params  parameter list to be extended
     */
    private void appendToWhereClause(final Collection<String> sqlList, final Collection<Collection<String>> paramsList, final StringBuffer sql, final Collection<String> params) {
        for (String sqlPart : sqlList) {
            sql.append(sqlPart);
        }
        for (Collection<String> paramsPart : paramsList) {
            params.addAll(paramsPart);
        }
        sqlList.clear();
        paramsList.clear();
    }

    /**
     * Get SQL queries' WHERE-clause
     *
     * @param   query   Lucene query to translate into a SQL WHERE-clause
     * @param   logicalOperator logical operator to use when connecting the results of the query to an existing WHERE-clause
     * @param   termQuery   SQL query fragment to use
     * @return  list of strings where the first entry is an SQL query that can be used in a PreparedStatement. The following
     *          strings are to be set as parameters for this PreparedStatement.
     */
    private WhereClauseParameters getSQLWhereClause(final Query query, String logicalOperator, final String termQuery) {
        if (DEBUG) System.err.println(this.getClass().getName() + ": Query-Class: " + query.getClass());
        if (DEBUG) System.err.println(this.getClass().getName() + ": Query: " + query);
        final List<String> params = new LinkedList<String>();
        final StringBuffer sql = new StringBuffer();
        if (query instanceof BooleanQuery) {
            boolean brackedOpened = false;
            final List<String> sqlList = new LinkedList<String>();
            final List<Collection<String>> paramsList = new LinkedList<Collection<String>>();
            for (BooleanClause clause : ((BooleanQuery) query).getClauses()) {
                if (DEBUG) System.err.println(this.getClass().getName() + ": BooleanClause: p: " + clause.isProhibited() + ", r:  " + clause.isRequired());
                if (clause.isProhibited()) {
                    if (!"AND NOT".equals(logicalOperator)) {
                        sql.append(" " + logicalOperator + " NOT ( 1=1");
                        brackedOpened = true;
                        appendToWhereClause(sqlList, paramsList, sql, params);
                    }
                    final WhereClauseParameters subQuery = getSQLWhereClause(clause.getQuery(), "AND", termQuery);
                    sqlList.add(subQuery.getSQL());
                    paramsList.add(subQuery.getParameters());
                    logicalOperator = "AND NOT";
                } else if (clause.isRequired()) {
                    if (!"AND".equals(logicalOperator)) {
                        sql.append(" " + logicalOperator + " ( 1=1");
                        brackedOpened = true;
                        appendToWhereClause(sqlList, paramsList, sql, params);
                    }
                    final WhereClauseParameters subQuery = getSQLWhereClause(clause.getQuery(), "AND", termQuery);
                    sqlList.add(subQuery.getSQL());
                    paramsList.add(subQuery.getParameters());
                    logicalOperator = "AND";
                } else {
                    if (!"OR".equals(logicalOperator)) {
                        sql.append(" " + logicalOperator + " ( 1=2");
                        brackedOpened = true;
                        appendToWhereClause(sqlList, paramsList, sql, params);
                    }
                    final WhereClauseParameters subQuery = getSQLWhereClause(clause.getQuery(), "OR", termQuery);
                    sqlList.add(subQuery.getSQL());
                    paramsList.add(subQuery.getParameters());
                    logicalOperator = "OR";
                }
            }
            if (!sqlList.isEmpty()) {
                if ("OR".equals(logicalOperator)) {
                    optimizeQuery(sqlList, paramsList);
                }
                if (DEBUG) System.err.println(this.getClass().getName() + ": AfterOptimization: New SQL= " + sql);
                if (DEBUG) System.err.println(this.getClass().getName() + ": AfterOptimization: New Parameters= " + params);
                appendToWhereClause(sqlList, paramsList, sql, params);
                if (DEBUG) System.err.println(this.getClass().getName() + ": AfterOptimization: New SQL= " + sql);
                if (DEBUG) System.err.println(this.getClass().getName() + ": AfterOptimization: New Parameters= " + params);
            }
            if (brackedOpened) {
                sql.append(" )");
            }
        } else if (query instanceof TermQuery || query instanceof WildcardQuery || query instanceof PrefixQuery) {
            processTermQuery(sql, params, query, logicalOperator, termQuery);
        } else if (query instanceof PhraseQuery) {
            processPhraseQuery(sql, params, query, logicalOperator, termQuery);
        } else if (query instanceof RangeQuery) {
            processRangeQuery(sql, params, query, logicalOperator, termQuery);
        } else {
            throw new RuntimeException("Query type '" + query.getClass() + "' not supported!");
        }
        if (DEBUG) System.err.println(this.getClass().getName() + ": SQL= " + sql);
        if (DEBUG) System.err.println(this.getClass().getName() + ": Params= " + params);
        return new WhereClauseParameters(sql.toString(), params);
    }

    /**
     * Create SQL WHERE-clause fragment that includes a check for any of the terms used in the query
     *
     * @param   query   Lucene query
     * @return  list of strings where the first entry is an SQL query that can be used in a PreparedStatement. The following
     *          strings are to be set as parameters for this PreparedStatement.
     */
    private WhereClauseParameters getSearchTermClause(final Query query) {
        final List<String> params = new LinkedList<String>();
        final StringBuffer sql = new StringBuffer();
        final String termQuery = "Word_ $OPERATOR ?";
        if (query instanceof BooleanQuery) {
            for (BooleanClause clause : ((BooleanQuery) query).getClauses()) {
                final WhereClauseParameters subQuery = getSearchTermClause(clause.getQuery());
                if (!params.containsAll(subQuery.getParameters())) {
                    sql.append(subQuery.getSQL());
                    params.addAll(subQuery.getParameters());
                }
            }
        } else if (query instanceof TermQuery || query instanceof WildcardQuery || query instanceof PrefixQuery) {
            processTermQuery(sql, params, query, "OR", termQuery);
            params.remove(0);
        } else if (query instanceof PhraseQuery) {
            processPhraseQuery(sql, params, query, "OR", termQuery);
            params.remove(0);
        } else if (query instanceof RangeQuery) {
            processRangeQuery(sql, params, query, "OR", termQuery);
            params.remove(2);
            params.remove(0);
        }
        return new WhereClauseParameters(sql.toString(), params);
    }

    /**
     * Create documents for a set of file IDs
     *
     * @param   fileIDs document IDs in the database
     * @return  collection of documents created from the IDs
     */
    private Collection<Document> getDocuments(final Connection con, final Set<Integer> fileIDs) throws SQLException, DataFormatException, IOException, ClassNotFoundException {
        final Map<Integer, Document> result = new HashMap<Integer, Document>(fileIDs.size());
        final Set<Integer> fileIDsCopy = new HashSet<Integer>(fileIDs);
        if (DEBUG) System.err.println(this.getClass().getName() + ": Starting document retrieval at " + new java.util.Date());
        while (fileIDsCopy.size() > 0) {
            Set<Integer> max1000FileIDs = new HashSet<Integer>(new ArrayList<Integer>(fileIDsCopy).subList(0, Math.min(fileIDsCopy.size(), 1000)));
            fileIDsCopy.removeAll(max1000FileIDs);
            final String in = "?" + StringUtils.repeat(",?", max1000FileIDs.size() - 1);
            if (DEBUG) System.err.println(this.getClass().getName() + ": IDs: " + max1000FileIDs.size());
            PreparedStatement getFilesStmt = con.prepareStatement("SELECT ID AS \"FileID\", Name AS \"Name\" FROM File_ WHERE ID IN ( " + in + " )");
            PreparedStatement getFieldsStmt = con.prepareStatement("SELECT FileID AS \"FileID\", ID AS \"FieldID\", Name AS \"Name\", Value AS \"Value\", FullValue AS \"FullValue\", IsCompressed AS \"IsCompressed\" FROM Field_ WHERE FileID IN ( " + in + " )");
            PreparedStatement getTokensStmt = con.prepareStatement("SELECT FileID AS \"FileID\", Word_ AS \"Word\", Occurrences AS \"Occurrences\" FROM Token_ T INNER JOIN Field_ F ON T.FieldID = F.ID WHERE F.FileID IN ( " + in + " )");
            int i = 1;
            for (int fileID : max1000FileIDs) {
                getFilesStmt.setInt(i, fileID);
                getFieldsStmt.setInt(i, fileID);
                getTokensStmt.setInt(i, fileID);
                ++i;
            }
            if (DEBUG) System.err.println(this.getClass().getName() + ": Getting file data at " + new java.util.Date());
            ResultSet rs = getFilesStmt.executeQuery();
            while (rs.next()) {
                int fileID = rs.getInt("FileID");
                Document doc = new Document();
                result.put(fileID, doc);
            }
            rs.close();
            if (DEBUG) System.err.println(this.getClass().getName() + ": Getting field data at " + new java.util.Date());
            rs = getFieldsStmt.executeQuery();
            while (rs.next()) {
                final int fileID = rs.getInt("FileID");
                final int fieldID = rs.getInt("FieldID");
                final String name = rs.getString("Name");
                String value = rs.getString("Value");
                final byte[] fullValue = rs.getBytes("FullValue");
                final boolean isCompressed = rs.getBoolean("IsCompressed");
                if (isCompressed) {
                    final ByteArrayOutputStream newFullValue = new ByteArrayOutputStream();
                    final Inflater decompresser = new Inflater();
                    decompresser.setInput(fullValue);
                    final byte[] buf = new byte[4000];
                    while (!decompresser.finished()) {
                        final int read = decompresser.inflate(buf);
                        newFullValue.write(buf, 0, read);
                    }
                    decompresser.end();
                    value = newFullValue.toString();
                }
                final Document doc = result.get(fileID);
                if (null != value) {
                    doc.add(new Field(name, value, Field.Store.NO, Field.Index.NO));
                }
            }
            rs.close();
            if (DEBUG) System.err.println(this.getClass().getName() + ": Getting token data at " + new java.util.Date());
            rs = getTokensStmt.executeQuery();
            while (rs.next()) {
                final int fileID = rs.getInt("FileID");
                final String word = rs.getString("Word");
                final int occurrences = rs.getInt("Occurrences");
                final Document doc = result.get(fileID);
                final Field field = new Field("token", word, Field.Store.NO, Field.Index.NO);
                field.setBoost(occurrences);
                doc.add(field);
            }
            rs.close();
            rs = null;
            getTokensStmt.close();
            getTokensStmt = null;
            getFieldsStmt.close();
            getFieldsStmt = null;
            getFilesStmt.close();
            getFilesStmt = null;
            if (DEBUG) System.err.println(this.getClass().getName() + ": " + result.size() + " Documents ready at " + new java.util.Date());
        }
        return result.values();
    }

    protected Query getSearchQuery(final String[] fields, final String searchText, final String searchPath) throws ParseException {
        this.searchFields = fields;
        try {
            String[] s = new String[fields.length];
            s[0] = searchText;
            return MultiFieldQueryParser.parse(s, fields, new StandardAnalyzer());
        } catch (ParseException e) {
            if (e.getMessage().indexOf("Encountered: \"?\" (63), after :") >= 0 || e.getMessage().indexOf("Encountered: \"*\" (42), after :") >= 0) {
                throw new ParseException("Query terms starting with a wildcard character are not allowed! Please modify your search!");
            } else {
                throw e;
            }
        }
    }

    protected Collection<Document> getSearchResults(final Query query, final String searchPath, final int maxRows) throws IOException {
        Connection con = null;
        Exception ex = null;
        List<Document> searchResults = null;
        try {
            con = getConnection();
            PreparedStatement stmt = getSQLStatement(con, query, searchPath);
            stmt.setMaxRows(maxRows * 2);
            if (DEBUG) System.err.println(this.getClass().getName() + ": Query ready at " + new java.util.Date());
            ResultSet rs = stmt.executeQuery();
            Set<Integer> fileIDs = new HashSet<Integer>();
            while (rs.next()) {
                fileIDs.add(rs.getInt("FileID"));
            }
            rs.close();
            rs = null;
            stmt.close();
            searchResults = new ArrayList<Document>(getDocuments(con, fileIDs));
            final Set<String> searchTerms = QueryUtils.getSearchTerms(query, "contents");
            searchTerms.addAll(QueryUtils.getSearchTerms(query, "title"));
            for (Document doc : searchResults) {
                double rating = 0;
                final Field[] tokenFields = doc.getFields("token");
                final String title = null == doc.getValues("title") ? "" : StringUtils.join(doc.getValues("title"), " ").toLowerCase();
                for (String searchTerm : searchTerms) {
                    searchTerm = searchTerm.toLowerCase();
                    for (Field field : tokenFields) {
                        final String token = field.stringValue().toLowerCase();
                        if (token.indexOf(searchTerm) >= 0) {
                            rating += field.getBoost() / tokenFields.length;
                        }
                    }
                    if (title.indexOf(searchTerm) > 0) {
                        rating += 2.0 * searchTerm.length() / title.length();
                    }
                }
                doc.add(new Field("rating", new Double(rating).toString(), Field.Store.NO, Field.Index.NO));
            }
            if (DEBUG) System.err.println(this.getClass().getName() + ": Sorting started at " + new java.util.Date());
            Collections.sort(searchResults, new Comparator<Document>() {

                public int compare(Document doc1, Document doc2) {
                    return (int) (-100000 * Double.parseDouble(doc1.getField("rating").stringValue()) + 100000 * Double.parseDouble(doc2.getField("rating").stringValue()));
                }
            });
            if (DEBUG) System.err.println(this.getClass().getName() + ": Sorting done at " + new java.util.Date());
            stmt.close();
            stmt = null;
        } catch (Exception e) {
            e.printStackTrace();
            ex = e;
        } finally {
            try {
                con.close();
                con = null;
            } catch (SQLException e) {
            }
        }
        if (null != ex) throw new IOException(ex.getMessage());
        if (DEBUG) System.err.println(this.getClass().getName() + ": " + searchResults.size() + " SearchResults ready at " + new java.util.Date());
        if (maxRows > 0 && searchResults.size() > maxRows) {
            return searchResults.subList(0, maxRows);
        } else {
            return searchResults;
        }
    }

    /**
     * Get an AbstractIndexWriter writing into the SQL Server tables
     */
    protected AbstractIndexWriter getIndexWriter(final boolean create) throws IOException {
        return new SQLServerIndexWriter(this, create);
    }

    class WhereClauseParameters {

        private final String sql;

        private final Collection<String> params;

        public WhereClauseParameters(String sql, Collection<String> params) {
            this.sql = sql;
            this.params = params;
        }

        public final String getSQL() {
            return this.sql;
        }

        public final Collection<String> getParameters() {
            return this.params;
        }
    }

    class CleanupThread extends Thread {

        private final Connection con;

        public CleanupThread(Connection con) {
            this.con = con;
        }

        public void run() {
            try {
                Thread.sleep(CLEANUP_TIMER);
                if (null != this.con) {
                    try {
                        this.con.close();
                    } catch (Exception e) {
                    }
                    ;
                }
            } catch (InterruptedException e) {
            }
        }
    }

    ;
}

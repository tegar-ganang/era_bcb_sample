package seismosurfer.database;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.driver.OraclePreparedStatement;
import oracle.jdbc.driver.OracleResultSet;
import oracle.ord.im.OrdDoc;
import oracle.ord.im.OrdHttpUploadFile;
import oracle.ord.im.OrdMediaUtil;
import seismosurfer.data.DocumentMetaData;
import seismosurfer.data.constants.QueryNames;
import seismosurfer.util.SeismoException;
import com.bbn.openmap.util.Debug;

/**
 * A DAO class that encapsulates all access to the
 * DOCUMENT table. This class uses an Oracle specific
 * connection and the Oracle interMedia API.
 *
 */
public class DocumentDAO implements QueryNames {

    protected static final String INSERT_DOC = " INSERT INTO document (docid, quakeid, description, doc, info) VALUES " + " (DOC_SEQ.NEXTVAL, ?, ?, ORDSYS.ORDDOC.INIT(), ?) ";

    protected static final String CURRVAL = " SELECT DOC_SEQ.CURRVAL FROM dual ";

    protected static final String FETCH_DOC_FOR_UPDATE = " SELECT doc FROM document WHERE docid = ? FOR UPDATE ";

    protected static final String UPDATE_DOC = " UPDATE document SET doc = ? WHERE docid = ? ";

    protected static final String SELECT_DOC = " SELECT doc " + " FROM document " + " WHERE docid = ? ";

    protected static final String DOCS_METADATA = " SELECT docid, description, " + " d.doc.getMimeType(), d.doc.getSource(), d.doc.getSourceType()," + " d.doc.getSourceName(), d.doc.getContentLength(), info " + " FROM document d " + " WHERE quakeid = ? ";

    protected static final String GET_DOCUMENT = " SELECT d.doc FROM document d WHERE docid = ? ";

    private static String JDBC_URL;

    private static Stack connStack = new Stack();

    private static boolean driverLoaded = false;

    /**
     * Creates a DocumentDAO object and saves the 
     * database url string that is used by the
     * Oracle JDBC driver.
     * 
     * @param url
     */
    public DocumentDAO(String url) {
        JDBC_URL = url;
    }

    /**
     * Loads a row from the database into a DocumentMetaData object.
     * 
     * @param rs The resultset from which the row will be loaded.
     * @return A DocumentMetaData object which contains the data of a row.
     * @throws SQLException
     */
    public static DocumentMetaData load(ResultSet rs) throws SQLException {
        DocumentMetaData md = new DocumentMetaData();
        md.setDocumentID(rs.getLong(1));
        md.setDescription(rs.getString(2));
        md.setMimeType(rs.getString(3));
        md.setSource(rs.getString(4));
        md.setSourceType(rs.getString(5));
        md.setSourceName(rs.getString(6));
        md.setContentLength(rs.getString(7));
        md.setInfo(rs.getString(8));
        return md;
    }

    /**
     * Retrives a list with the documents` metadata of a quake.
     * 
     * @param quakeID the id of a quake
     * @return a list with the documents` metadata of a quake
     */
    public List getDocumentsMetadata(long quakeID) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List result = new ArrayList();
        try {
            stmt = DB.prepare(DOCS_METADATA);
            stmt.setLong(1, quakeID);
            rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(load(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new SeismoException(e);
        } finally {
            DB.cleanUp(stmt, rs);
        }
    }

    /**
     * Retrives a document given its id.
     * 
     * @param docid the document id
     * @return an OrdDoc object that represents a document
     */
    public OrdDoc getDocument(long docid) {
        Connection orclConn = null;
        OrdDoc doc = null;
        try {
            orclConn = getOracleConnection();
            PreparedStatement stmt = orclConn.prepareStatement(GET_DOCUMENT);
            stmt.setLong(1, docid);
            OracleResultSet rset = (OracleResultSet) stmt.executeQuery();
            if (rset.next()) {
                doc = (OrdDoc) rset.getCustomDatum(1, OrdDoc.getFactory());
            } else {
                throw new SeismoException("Document not found in database.");
            }
            rset.close();
            stmt.close();
            freeOracleConnection(orclConn);
            return doc;
        } catch (SQLException e) {
            throw new SeismoException(e);
        } finally {
            freeOracleConnection(orclConn);
        }
    }

    /**
     * Inserts a submitted url and its associated data for an earthquake. 
     * 
     * @param quakeid the id of the earthquake for which the url will be
     *        inserted
     * @param url the URL to be inserted in the db
     * @param description the description of the url
     * @param info additional information for the url
     */
    public void insertURL(long quakeid, URL url, String description, String info) {
        insertDocOrURL(quakeid, null, url, URL, description, info);
    }

    /**
     * Inserts a submitted document and its associated data for an earthquake. 
     * 
     * @param quakeid the id of the earthquake for which the document will be
     *        inserted
     * @param doc the OrdHttpUploadFile that contains the document
     *        to be inserted in the db
     * @param description the description of the document
     * @param info additional information for the document
     */
    public void insertDocument(long quakeid, OrdHttpUploadFile doc, String description, String info) {
        insertDocOrURL(quakeid, doc, null, DOC, description, info);
    }

    /**
     * Inserts a submitted document or url and its associated data 
     * for an earthquake. The type of the document to be inserted 
     * can be set using the QueryNames.DOC or QueryNames.URL constants.
     * 
     * @param quakeid  the id of the earthquake for which the document will be
     *        inserted
     * @param doc  the OrdHttpUploadFile that contains the document
     *        to be inserted in the db
     * @param url  the URL to be inserted in the db
     * @param type the type of submission. Either a document or url
     *        is submitted/inserted.
     * @param description  the description of the document or url
     * @param info  additional information for the document or url
     */
    protected void insertDocOrURL(long quakeid, OrdHttpUploadFile doc, URL url, int type, String description, String info) {
        Connection orclConn = null;
        try {
            orclConn = getOracleConnection();
            orclConn.setAutoCommit(false);
            OraclePreparedStatement stmt = (OraclePreparedStatement) orclConn.prepareStatement(INSERT_DOC);
            stmt.setLong(1, quakeid);
            stmt.setString(2, description);
            stmt.setString(3, info);
            stmt.executeUpdate();
            Debug.output("DocumentDAO:" + quakeid + "  " + description + "  " + info);
            stmt = (OraclePreparedStatement) orclConn.prepareStatement(CURRVAL);
            OracleResultSet rset = (OracleResultSet) stmt.executeQuery();
            if (!rset.next()) {
                throw new SeismoException("CURRVAL not found !!!");
            }
            long docid = rset.getLong(1);
            System.out.println("docid : " + docid);
            stmt = (OraclePreparedStatement) orclConn.prepareStatement(FETCH_DOC_FOR_UPDATE);
            stmt.setLong(1, docid);
            Debug.output("DocumentDAO:" + docid);
            rset = (OracleResultSet) stmt.executeQuery();
            if (!rset.next()) {
                throw new SeismoException("New row not found in table");
            }
            OrdDoc document = (OrdDoc) rset.getCustomDatum(1, OrdDoc.getFactory());
            if (type == DOC && doc != null) {
                loadDoc(document, doc);
            } else if (type == URL && url != null) {
                loadURL(document, url);
            } else {
                throw new SeismoException("Wrong document type. Expected DOC or URL.");
            }
            stmt = (OraclePreparedStatement) orclConn.prepareStatement(UPDATE_DOC);
            stmt.setCustomDatum(1, document);
            stmt.setLong(2, docid);
            stmt.execute();
            stmt.close();
            Debug.output("DOC format: " + document.getFormat());
            Debug.output("DOC MIME: " + document.getMimeType());
            Debug.output("DOC source: " + document.getSource());
            orclConn.commit();
        } catch (SQLException e) {
            try {
                orclConn.rollback();
            } catch (SQLException ex) {
                throw new SeismoException(ex);
            }
        } catch (IOException e) {
            throw new SeismoException(e);
        } finally {
            freeOracleConnection(orclConn);
        }
    }

    private void loadURL(OrdDoc document, URL url) throws SQLException {
        String srcType = url.getProtocol();
        System.out.println(srcType);
        Debug.output(url.getPath());
        int sep = url.getPath().lastIndexOf("/");
        Debug.output("Sep:" + sep);
        String srcLocation = url.getHost() + url.getFile().substring(0, sep);
        System.out.println(srcLocation);
        String srcName = url.getFile().substring(sep + 1);
        System.out.println(srcName);
        document.setSource(srcType, srcLocation, srcName);
    }

    private void loadDoc(OrdDoc document, OrdHttpUploadFile doc) throws SQLException, IOException, SQLException {
        document.setSource("", "", doc.getSimpleFileName());
        doc.loadDoc(document);
    }

    /**
     * The getConnection method implements a simple JDBC connection pool using a
     * Java Stack to hold the set of available connections. If the stack is
     * empty, then getConnection simply creates a new connection.
     * 
     * @return an Oracle Connection object
     */
    private Connection getOracleConnection() throws SQLException {
        OracleConnection conn = null;
        synchronized (connStack) {
            if (!driverLoaded) {
                DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
                driverLoaded = true;
            }
            if (connStack.empty()) {
                conn = (OracleConnection) DriverManager.getConnection(JDBC_URL);
                try {
                    OrdMediaUtil.imCompatibilityInit(conn);
                } catch (Exception e) {
                    throw new SQLException(e.toString());
                }
            } else {
                conn = (OracleConnection) connStack.pop();
            }
        }
        conn.setAutoCommit(true);
        return conn;
    }

    /**
     * The freeConnection method simply returns a JDBC connection to the pool.
     */
    private void freeOracleConnection(Connection conn) {
        if (conn != null) {
            synchronized (connStack) {
                connStack.push(conn);
            }
        }
    }
}

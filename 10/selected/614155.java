package ro.gateway.aida.obj.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import ro.gateway.aida.db.DBPersistenceManager;
import ro.gateway.aida.db.PersistenceToken;
import ro.gateway.aida.obj.AIDAActivityObject;
import ro.gateway.aida.obj.AIDADocument;
import ro.gateway.aida.srv.IIDGenerator;

/**
 * <p>Title: Romanian AIDA</p>
 * <p>Description: :D application</p>
 * <p>Copyright: Copyright (comparator) 2003</p>
 * <p>Company: Romania Development Gateway </p>
 * @author Mihai Popoaei, mihai_popoaei@yahoo.com, smike@intellisource.ro
 * @version 1.0-* @version $Id: AIDADocumentDB.java,v 1.1 2004/10/24 23:37:11 mihaipostelnicu Exp $
 */
public class AIDADocumentDB extends DBPersistenceManager {

    private AIDADocumentDB(PersistenceToken token) {
        super(token);
    }

    public static AIDADocumentDB getManager(PersistenceToken token) {
        return new AIDADocumentDB(token);
    }

    /**
	 * todo: tranzaction
	 * @param idGenerators
	 * @param item
	 * @throws SQLException
	 */
    public void insert(IIDGenerator idGenerators, AIDADocument item) throws SQLException {
        AIDAActivityObjectDB.getManager(token).insert(idGenerators, item);
        Connection con = getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(DOC_INSERT);
            ps.setLong(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getRelativeLink());
            ps.executeUpdate();
            ps.close();
            insertDescriptions(con, item);
        } catch (SQLException sqlEx) {
            con.rollback();
            throw sqlEx;
        } finally {
            con.close();
        }
        return;
    }

    private void insertDescriptions(Connection con, AIDADocument item) throws SQLException {
        String[] langs = item.getAvailableLanguages();
        if ((langs == null) || (langs.length < 1)) {
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement(DESC_INSERT);
            for (int i = 0; i < langs.length; i++) {
                ps.clearParameters();
                ps.setLong(1, item.getId());
                ps.setString(2, langs[i]);
                ps.setString(3, item.getDesc(langs[i]));
                ps.setString(4, item.getTitle(langs[i]));
                ps.executeUpdate();
            }
            ps.close();
        } catch (SQLException sqlEx) {
            throw sqlEx;
        }
        return;
    }

    public AIDADocument[] getForActivity(long activity_id) throws SQLException {
        Connection con = getConnection();
        AIDADocument[] result = null;
        ArrayList items = new ArrayList();
        try {
            PreparedStatement ps = con.prepareStatement(GET_DOC_FOR_ACTIVITY);
            ps.setLong(1, activity_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AIDADocument item = new AIDADocument();
                item.setId(rs.getLong(1));
                item.setName(rs.getString(2));
                item.setRelativeLink(rs.getString(3));
                items.add(item);
            }
            rs.close();
            ps.close();
            if (items.size() > 0) {
                result = new AIDADocument[items.size()];
                items.toArray(result);
            } else {
                return null;
            }
            ps = con.prepareStatement(GET_DOCS_FOR_DOC);
            for (int i = 0; i < result.length; i++) {
                ps.setLong(1, result[i].getId());
                rs = ps.executeQuery();
                while (rs.next()) {
                    String lang = rs.getString(1);
                    result[i].setDesc(lang, rs.getString(2));
                    if (rs.wasNull()) {
                        result[i].setDesc(lang, null);
                    }
                    result[i].setTitle(lang, rs.getString(3));
                    if (rs.wasNull()) {
                        result[i].setTitle(lang, null);
                    }
                }
                rs.close();
            }
            ps.close();
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } finally {
            con.close();
        }
        return result;
    }

    private void delete_descs(long id) throws SQLException {
        Connection con = getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(DELETE_DESCS);
            ps.setLong(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } finally {
            con.close();
        }
    }

    private void delete_docs(long id) throws SQLException {
        Connection con = getConnection();
        try {
            PreparedStatement ps = con.prepareStatement(DELETE_DOC);
            ps.setLong(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } finally {
            con.close();
        }
    }

    private void delete_descs_for_activity(long id) throws SQLException {
        Connection con = getConnection();
        String database_pname = con.getMetaData().getDatabaseProductName();
        if (0 == database_pname.compareToIgnoreCase("MYSQL")) {
            System.out.println("MYSQL - deleteForActivity (documents) -- DEBUG");
            con.close();
            deleteDescsForActivityMYSQL(id);
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement(DELETE_DESCS_FOR_ACTIVITY);
            ps.setLong(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } finally {
            con.close();
        }
    }

    private void deleteDescsForActivityMYSQL(long id) throws SQLException {
        AIDADocument[] docs = getForActivity(id);
        if (docs != null) {
            Connection con = getConnection();
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM doc_descs WHERE id = ?");
                for (int i = 0; i < docs.length; i++) {
                    ps.clearParameters();
                    ps.setLong(1, docs[i].getId());
                    ps.executeUpdate();
                }
                ps.close();
            } catch (SQLException sqlEx) {
                throw sqlEx;
            } finally {
                con.close();
            }
        }
    }

    private void delete_docs_for_activity(long id) throws SQLException {
        Connection con = getConnection();
        String database_pname = con.getMetaData().getDatabaseProductName();
        if (0 == database_pname.compareToIgnoreCase("MYSQL")) {
            System.out.println("MYSQL - deleteForActivity (documents) -- DEBUG");
            con.close();
            deleteDocsForActivityMYSQL(id);
            return;
        }
        try {
            PreparedStatement ps = con.prepareStatement(DELETE_DOC_FOR_ACTIVITY);
            ps.setLong(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } finally {
            con.close();
        }
    }

    private void deleteDocsForActivityMYSQL(long id) throws SQLException {
        AIDADocument[] docs = getForActivity(id);
        if (docs != null) {
            Connection con = getConnection();
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM doc_docs WHERE id = ?");
                for (int i = 0; i < docs.length; i++) {
                    ps.clearParameters();
                    ps.setLong(1, docs[i].getId());
                    ps.executeUpdate();
                }
                ps.close();
            } catch (SQLException sqlEx) {
                throw sqlEx;
            } finally {
                con.close();
            }
        }
    }

    public void delete(long id) throws SQLException {
        delete_descs(id);
        delete_docs(id);
    }

    public final void deleteForActivity(long id) throws SQLException {
        delete_descs_for_activity(id);
        delete_docs_for_activity(id);
        AIDAActivityObjectDB.getManager(token).deleteForActivityByType(id, AIDAActivityObject.TYPE_DOCUMENT);
    }

    private static final String DOC_INSERT = "INSERT INTO doc_docs (id, fileName, relLink) " + "VALUES (?,?,?)";

    private static final String DESC_INSERT = "INSERT INTO doc_descs (id, lang, description, title) " + "VALUES (?, ?, ?, ?)";

    private static final String GET_DOC_FOR_ACTIVITY = "SELECT d.id, d.fileName, d.relLink " + "FROM doc_docs d, objects o " + "WHERE o.activity_id=? AND o.id=d.id";

    private static final String GET_DOCS_FOR_DOC = "SELECT lang, description, title " + "FROM doc_descs WHERE id=?";

    private static final String DELETE_DESCS = "DELETE FROM doc_descs " + "WHERE id = ?";

    private static final String DELETE_DOC = "DELETE FROM doc_docs " + "WHERE id = ?";

    private static final String DELETE_DESCS_FOR_ACTIVITY = "DELETE FROM doc_descs " + "WHERE id IN (SELECT id FROM objects WHERE activity_id=?)";

    private static final String DELETE_DOC_FOR_ACTIVITY = "DELETE FROM doc_docs " + "WHERE id IN (SELECT id FROM objects WHERE activity_id=?)";
}

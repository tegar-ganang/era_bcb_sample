package de.jformular.persistens;

import de.jformular.interfaces.FormularContainer;
import de.jformular.util.config.ConfigManager;
import de.jformular.util.config.Configuration;
import de.jformular.util.log.Log;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import javax.naming.InitialContext;

/**
 * Class declaration
 * @author Frank Dolibois, fdolibois@itzone.de, http://www.itzone.de
 * @version $Id: FormularContextPersistensImpl.java,v 1.9 2002/10/14 14:01:49 fdolibois Exp $
 */
public class FormularContextPersistensImpl implements FormularContextPersistens, java.io.Serializable {

    private static long seed = 0;

    private static Random rand = new Random();

    private static String FORMULAR_INDEX = "FORMULAR_INDEX";

    private static String CONTEXT_INDEX = "CONTEXT_INDEX";

    /**
     * speichert den aktuellen FormularContainer und gibt diesen
     * mit gesetzten ID wieder zur�ck, sonst null
     */
    public FormularContainer store(FormularContainer formularContainer) {
        Connection con = null;
        Statement stm = null;
        StringBuffer sql = new StringBuffer();
        return null;
    }

    /**
     *
     *
     *
     * @param id
     * @param connection
     * @return
     */
    private boolean deleteFormularContext(String id, Connection connection) {
        ResultSet rs = null;
        Statement stm1 = null;
        Statement stm2 = null;
        StringBuffer sql = new StringBuffer();
        try {
            stm1 = connection.createStatement();
            stm2 = connection.createStatement();
            sql.append("SELECT * FROM ANTR_EC WHERE ANTR_REFERENZ_BEZ='");
            sql.append(id);
            sql.append("'");
            rs = stm1.executeQuery(sql.toString());
            if (rs != null) {
                while (rs.next()) {
                    sql = new StringBuffer();
                    sql.append("DELETE FROM ANDT_EC WHERE ANDT_ANTR# = ");
                    sql.append(rs.getLong("ANTR_ANTR#"));
                    stm2.executeUpdate(sql.toString());
                }
                rs.close();
                rs = null;
            }
            sql = new StringBuffer();
            sql.append("DELETE FROM ANTR_EC WHERE ANTR_REFERENZ_BEZ='");
            sql.append(id);
            sql.append("'");
            stm1.execute(sql.toString());
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception ignore) {
                }
            }
            if (stm1 != null) {
                try {
                    stm1.close();
                } catch (Exception ignore) {
                }
            }
            if (stm2 != null) {
                try {
                    stm2.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * l�d einen gespeicherten FormularContainer mit ID
     */
    public FormularContainer load(String formularContextID) {
        return null;
    }

    /**
     *
     *
     *
     * @param indexname
     * @return
     */
    private long newIndex(String indexname) {
        Connection con = null;
        ResultSet rs = null;
        Statement stm = null;
        StringBuffer sql = new StringBuffer();
        indexname = indexname.trim().toUpperCase();
        try {
            long index = -1;
            synchronized (FormularContextPersistensImpl.class) {
                con = getConnection();
                stm = con.createStatement();
                if ((con != null) && (stm != null)) {
                    con.setAutoCommit(false);
                    sql = new StringBuffer();
                    sql.append("SELECT * FROM INDX_EC WHERE INDX_NAME='");
                    sql.append(indexname);
                    sql.append("' FOR UPDATE");
                    rs = stm.executeQuery(sql.toString());
                    if ((rs != null) && rs.next()) {
                        sql = new StringBuffer();
                        index = rs.getLong("INDX_WERT") + 1;
                        sql.append("UPDATE INDX_EC SET INDX_WERT = ");
                        sql.append(index);
                        sql.append(" WHERE INDX_NAME='");
                        sql.append(indexname);
                        sql.append("'");
                        rs.close();
                        rs = null;
                        if (stm.executeUpdate(sql.toString()) == 1) {
                            con.commit();
                        } else {
                            con.rollback();
                            index = -1;
                        }
                    } else {
                        sql = new StringBuffer();
                        sql.append("INSERT INTO INDX_EC (INDX_NAME, INDX_WERT) VALUES('");
                        sql.append(indexname);
                        sql.append("', ");
                        sql.append(1);
                        sql.append(")");
                        if (stm.executeUpdate(sql.toString()) == 1) {
                            con.commit();
                            index = 1;
                        } else {
                            con.rollback();
                        }
                    }
                }
            }
            return index;
        } catch (Exception e) {
            Log.getLogger().error("Error during execute SQL-Statement: " + sql.toString(), e);
            return -1;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception ignore) {
                }
            }
            if (stm != null) {
                try {
                    stm.close();
                } catch (Exception ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * liefert eine jdbc Connection aus dem Initial Context des AppServers
     */
    private Connection getConnection() {
        InitialContext ctx = null;
        try {
            ctx = new javax.naming.InitialContext();
            javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup(ConfigManager.getConfiguration(Configuration.JFORMULAR_CONFIGURATION).getString("jformular.persistens.dsname"));
            return ds.getConnection(ConfigManager.getConfiguration(Configuration.JFORMULAR_CONFIGURATION).getString("jformular.persistens.database.name"), ConfigManager.getConfiguration(Configuration.JFORMULAR_CONFIGURATION).getString("jformular.persistens.database.pwd"));
        } catch (Exception e) {
            Log.getLogger().fatal(e.getMessage(), e);
            return null;
        }
    }

    /**
     *
     *
     *
     * @param FormularContainer
     * @return
     */
    private String newContextID(FormularContainer FormularContainer) {
        long id = newIndex(CONTEXT_INDEX);
        if (id != -1) {
            return createRandomSequence(13) + id;
        } else {
            return null;
        }
    }

    /**
     * l�scht einen gespeicherten FormularContainer mit ID
     */
    public boolean delete(String formularContextID) {
        Connection con = null;
        try {
            con = getConnection();
            return deleteFormularContext(formularContextID, con);
        } catch (Exception e) {
            return false;
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Create a random Sequence of characters for use as an ID
     * @param length of the generated String
     * @return a String with <i>length</i> characters
     */
    private String createRandomSequence(int length) {
        final char[][] allowedCharSet = { { '0', '9' }, { 'a', 'z' }, { 'A', 'Z' } };
        char[] charSet = new char[255];
        int idx = 0;
        for (int n = 0; n < allowedCharSet.length; n++) {
            for (char k = allowedCharSet[n][0]; k <= allowedCharSet[n][1]; k++) {
                charSet[idx++] = k;
                if (idx >= 255) {
                    break;
                }
            }
            if (idx >= 255) {
                break;
            }
        }
        char[] seq = new char[length];
        Random r = new Random(seed);
        for (int i = 0; i < length; i++) {
            seq[i] = charSet[r.nextInt(idx)];
        }
        seed = rand.nextLong() + System.currentTimeMillis();
        return new String(seq);
    }
}

package org.sulweb.infumon.common;

import java.sql.*;
import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
public class AutoIncrementID {

    public static final StringBuffer tableName = new StringBuffer("AUTOIDS");

    private PreparedStatement select, insert;

    private Connection conn;

    private long bitmask;

    private static final short N_BITS_LONG_TYPE = (short) 64;

    private short nshift;

    private long maskedBitsValue;

    private Set alreadyInitializedTables;

    public AutoIncrementID() {
        this(N_BITS_LONG_TYPE, 0);
    }

    /**
   * This constructor allows for key partitioning. The upper part of the returned IDs will be filled with
   * the specified value, after having shifted it of the proper number of bits. "The upper part"
   * means the part of the ID that corresponds to the number of bits not used for the ID itself, 
   * that is, the masked ones. For example, let's assume you called this constructor with
   * <pre>
   *    new AutoIncrementID((short)40, 5L);
   * </pre>
   * then you call 
   * <pre>
   *    getId(myconn, "MyTable", "ID");
   * </pre>
   * Then the returned value would be (nextID & 0x000000FFFFFFFFFFL) | (5L << 40), 
   * where nextID is the first available ID and 0x000000FFFFFFFFFFL is the bitmask of 40 bits;
   * assuming the next ID were 0xC5A07L, the returned value would be 0x00000500000C5A07L.
   * This can be used to specify the "partition" of the key as the upper part of the ID,
   * thus allowing to mix database records between different databases, given
   * a different partition number for each database.
   * @param nbits The number of bits reserved for the IDs
   * @param maskedBitsValue The number assigned to the local database (the partition number)
   * @throws IllegalArgumentException If the partition number cannot fit into the remaining bits
   */
    public AutoIncrementID(short nbits, long maskedBitsValue) throws IllegalArgumentException {
        alreadyInitializedTables = new HashSet();
        bitmask = 0L;
        for (short i = 0; i < nbits; i++) {
            bitmask = bitmask << 1;
            bitmask = bitmask | 1L;
        }
        this.maskedBitsValue = maskedBitsValue;
        short nFreeBits = (short) (N_BITS_LONG_TYPE - nbits);
        short nRequiredBitsForPartitionNumber = computeRequiredBits(maskedBitsValue);
        if (nRequiredBitsForPartitionNumber > nFreeBits) throw new IllegalArgumentException("Not enough free (masked) bits for the specified value");
        nshift = nbits;
    }

    protected void init(Connection conn) throws SQLException {
        this.conn = conn;
        createTableIfNotExisting(conn);
        select = conn.prepareStatement("select tableName,lastId from " + tableName + " where tableName=?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        insert = conn.prepareStatement("insert into " + tableName + " (lastId, tableName) values (?, ?)");
    }

    public void createTableIfNotExisting(Connection conn) throws SQLException {
        String sql = "select * from " + tableName;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.executeQuery();
        } catch (SQLException sqle) {
            ps.close();
            sql = "create table " + tableName + " ( tableName varchar(255) not null primary key, " + "   lastId numeric(18) not null)";
            ps = conn.prepareStatement(sql);
            ps.executeUpdate();
        } finally {
            ps.close();
            try {
                if (!conn.getAutoCommit()) conn.commit();
            } catch (Exception e) {
                conn.rollback();
            }
        }
    }

    protected void drop(Connection conn) throws SQLException {
        String sql = "drop table " + tableName;
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.executeUpdate();
    }

    public final short getNBitsForTheIDs() {
        return nshift;
    }

    public final long getPartitionNumber() {
        return maskedBitsValue;
    }

    private static short computeRequiredBits(long maskedBitsValue) {
        short result = 0;
        for (short i = 0; i < N_BITS_LONG_TYPE; i++) {
            result = i;
            if (maskedBitsValue == 0) break;
            maskedBitsValue = maskedBitsValue >> 1;
        }
        return result;
    }

    public long getId(Connection conn, String p_table, String fieldName) throws SQLException {
        long result = -1;
        if (conn != this.conn) init(conn);
        conn.setAutoCommit(false);
        synchronized (this) {
            try {
                select.setString(1, p_table);
                ResultSet rs = select.executeQuery();
                long tempRes = -1;
                if (rs.next()) {
                    tempRes = rs.getLong(2);
                    long newTempRes = (tempRes + 1L) & bitmask;
                    rs.updateLong(2, newTempRes);
                    rs.updateRow();
                    rs.close();
                    conn.commit();
                } else {
                    rs.close();
                    try {
                        createRow(p_table, fieldName);
                    } catch (SQLException sqle) {
                    }
                    tempRes = getId(conn, p_table, fieldName);
                }
                result = tempRes | (maskedBitsValue << nshift);
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw sqle;
            } finally {
                if (result == -1) try {
                    conn.rollback();
                } catch (SQLException e) {
                }
            }
        }
        return result;
    }

    private long getCurrentMax(String p_table, String fieldName) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("select max(" + fieldName + ") from " + p_table);
        ResultSet rs = ps.executeQuery();
        long currentMax = 0;
        if (rs.next()) currentMax = rs.getLong(1) + 1;
        return currentMax;
    }

    private void createRow(String p_table, String fieldName) throws SQLException {
        long newId = getCurrentMax(p_table, fieldName);
        insert.setLong(1, newId);
        insert.setString(2, p_table);
        insert.executeUpdate();
        conn.commit();
    }
}

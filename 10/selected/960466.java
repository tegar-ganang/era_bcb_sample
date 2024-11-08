package org.epoline.jdms.program.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.epoline.jsf.jdms.TSProgManager;
import org.epoline.jsf.jdms.TSProgram;
import org.epoline.jsf.jdms.dl.JDmsRequest;

public class PHC404 implements TSProgram {

    /**
	 * These strings contain the SQL code for next-id.
	*/
    private static String NID1 = "UPDATE TPH059_XSYSSEQ " + "SET XSTMOD = " + TSProgManager.TIMESTAMP + ", XSTBCHSEQNUM = XSTBCHSEQNUM + 1 " + "WHERE ( XSTKEY = ? ) AND ( XSTBCHDATE = " + TSProgManager.DATE + " ) ";

    private static String TABNID1 = "404NID1";

    private static String NID2 = "UPDATE TPH059_XSYSSEQ " + "SET XSTMOD = " + TSProgManager.TIMESTAMP + ", XSTBCHSEQNUM = 1, XSTBCHDATE =  " + TSProgManager.DATE + " " + "WHERE ( XSTKEY = ? ) ";

    private static String TABNID2 = "404NID2";

    private static String NID3 = "SELECT XSTBCHDATE, XSTBCHSEQNUM, XSTMOD FROM TPH059_XSYSSEQ WHERE ( XSTKEY = ? ) ";

    private static StringBuffer BUF = new StringBuffer("                                         ERROR...");

    private static String TABNID3 = "404NID3";

    public JDmsRequest exec(Connection con, JDmsRequest req) {
        return nid(con, req);
    }

    /**
	 * nid : returns a new number.
	*/
    private JDmsRequest nid(Connection con, JDmsRequest req) {
        JDmsRequest rc = req;
        String sin = req.sCommarea;
        if (sin == null) {
            sin = new String(req.bCommarea);
        }
        if (sin.length() < 15) {
            rc.TSCode = -99;
            rc.TSMessage = "Illegal length of commarea.";
            return rc;
        }
        String key1 = sin.substring(0, 15);
        PreparedStatement stm = null;
        StringBuffer buffer = new StringBuffer(key1);
        int count = 0;
        ResultSet rs = null;
        boolean doCommit = false;
        try {
            if (con.getAutoCommit()) {
                con.setAutoCommit(false);
                doCommit = true;
            }
            stm = TSProgManager.getStatement(con, TABNID1, NID1);
            stm.setString(1, key1);
            count = stm.executeUpdate();
            stm.close();
            stm = null;
            if (count == 0) {
                stm = TSProgManager.getStatement(con, TABNID2, NID2);
                stm.setString(1, key1);
                count = stm.executeUpdate();
                stm.close();
                stm = null;
            }
            if (count == 0) {
                buffer.append(BUF);
                buffer.append("01+00100");
                rc.SQLCode = +100;
                rc.SQLMessage = "XSYSSEQ-REC with key " + key1 + " not found.";
            } else {
                stm = TSProgManager.getStatement(con, TABNID3, NID3);
                stm.setString(1, key1);
                rs = stm.executeQuery();
                if (rs.next()) {
                    String XstBchDate = rs.getString(1);
                    int tempint = 0;
                    tempint = rs.getInt(2);
                    String XstBchSeqNum = ("" + (10000 + tempint)).substring(1);
                    String XSTMOD = rs.getString(3);
                    buffer.append(XstBchDate.substring(0, 10));
                    buffer.append(XstBchSeqNum);
                    buffer.append((XSTMOD + "000000").substring(0, 26));
                    buffer.append("DUMMY   00+00000");
                }
                rs.close();
                rs = null;
                rc.SQLCode = 0;
                if (doCommit) {
                    con.commit();
                    doCommit = false;
                }
            }
        } catch (SQLException e) {
            buffer.append(BUF);
            buffer.append("02");
            buffer.append(e.getErrorCode());
            rc.SQLCode = e.getErrorCode();
            rc.SQLMessage = e.getSQLState();
        } catch (Exception e) {
            buffer.append(BUF);
            buffer.append("02");
            buffer.append(JDmsRequest.RC_ERR);
            rc.SQLCode = JDmsRequest.RC_ERR;
            rc.SQLMessage = e.getMessage();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                }
            }
            if (doCommit) {
                try {
                    con.rollback();
                } catch (SQLException e) {
                }
            }
        }
        rc.bCommarea = buffer.toString().getBytes();
        rc.sCommarea = null;
        return rc;
    }
}

package com.knowgate.forums;

import java.util.Date;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import com.knowgate.debug.DebugFile;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.misc.Gadgets;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataobjs.DBBind;
import com.knowgate.dataobjs.DBPersist;
import com.knowgate.hipergate.Product;

/**
 * <p>NewsMessage</p>
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class NewsMessage extends DBPersist {

    /**
   * Create empty NewsMessage
   */
    public NewsMessage() {
        super(DB.k_newsmsgs, "NewsMessage");
    }

    /**
   * Post a Plain Text Message
   * @param oConn Database Conenction
   * @param sNewsGroupId GUID of NewsGroup for posting
   * @param sThreadId GUID of message thread (may be <b>null</b>)
   * @param dtStart Start publishing date (may be <b>null</b>)
   * @param dtEnd Expiration date (may be <b>null</b>)
   * @param iStatus STATUS_VALIDATED or STATUS_PENDING
   * @param sText Message Text
   * @return GUID of new message
   * @throws SQLException
   */
    public String post(JDCConnection oConn, String sNewsGroupId, String sThreadId, Date dtStart, Date dtEnd, short iStatus, String sText) throws SQLException {
        if (DebugFile.trace) {
            DebugFile.writeln("Begin NewsMessage.post([Connection], " + sNewsGroupId + "," + sThreadId + ")");
            DebugFile.incIdent();
        }
        String sRetVal;
        remove(DB.gu_newsgrp);
        if (sNewsGroupId != null) put(DB.gu_newsgrp, sNewsGroupId);
        remove(DB.gu_thread_msg);
        if (sThreadId != null) put(DB.gu_thread_msg, sThreadId);
        remove(DB.dt_start);
        if (dtStart != null) put(DB.dt_start, dtStart);
        remove(DB.dt_end);
        if (dtEnd != null) put(DB.dt_end, dtEnd);
        remove(DB.id_status);
        put(DB.id_status, iStatus);
        remove(DB.tx_msg);
        if (sText != null) put(DB.tx_msg, sText);
        if (store(oConn)) sRetVal = getString(DB.gu_msg); else sRetVal = null;
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End NewsMessage.post() : " + sRetVal);
        }
        return sRetVal;
    }

    /**
   * <p>Store NewsMessage</p>
   * Message is posted into a NewsGroup by setting gu_newsgrp property of
   * NewsMessage to the GUID of newsMessage that will contain it.<br>
   * If gu_msg is <b>null</b> then a new GUID will be assigned.<br>
   * If gu_thread_msg is <b>null</b> and gu_parent_msg is <b>null</b> then gu_thread_msg will be assigned the same value as gu_msg.<br>
   * If gu_thread_msg is <b>null</b> and gu_parent_msg is not <b>null</b> then gu_thread_msg will be assigned the same the parent message thread.<br>
   * If id_status is <b>null</b> then it will be assigned to NewsMessage.STATUS_PENDING.<br>
   * If id_msg_type is <b>null</b> then it will be assigned to "TXT" by default.<br>
   * If dt_start is <b>null</b> then message visibility will start inmediately.<br>
   * If dt_end is <b>null</b> then message will never expire.<br>
   * dt_published will always be set to the currents system date no matter with values it is passed as parameter.<br>
   * nu_thread_msgs will be updated in all messages from this thread by calling k_sp_count_thread_msgs stored procedure.<br>
   * Column k_newsgroups.dt_last_update is automatically set to the current date each time a new message is stored.
   * @param oConn Database Connection
   * @throws SQLException
   */
    public boolean store(JDCConnection oConn) throws SQLException {
        boolean bNewMsg;
        boolean bRetVal;
        String sMsgId;
        int nThreadMsgs;
        ResultSet oRSet;
        Statement oStmt;
        CallableStatement oCall;
        String sSQL;
        Timestamp dtNow = new Timestamp(DBBind.getTime());
        if (DebugFile.trace) {
            DebugFile.writeln("Begin NewsMessage.store([Connection])");
            DebugFile.incIdent();
        }
        if (!AllVals.containsKey(DB.gu_msg)) {
            bNewMsg = true;
            sMsgId = Gadgets.generateUUID();
            put(DB.gu_msg, sMsgId);
        } else {
            bNewMsg = false;
            sMsgId = getString(DB.gu_msg);
        }
        if (!AllVals.containsKey(DB.id_status)) put(DB.id_status, STATUS_PENDING);
        if (!AllVals.containsKey(DB.gu_thread_msg)) if (AllVals.containsKey(DB.gu_parent_msg)) {
            oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(SELECT " + DB.gu_thread_msg + " FROM " + DB.k_newsmsgs + " WHERE " + DB.gu_msg + "='" + getStringNull(DB.gu_parent_msg, "null") + "'");
            oRSet = oStmt.executeQuery("SELECT " + DB.gu_thread_msg + " FROM " + DB.k_newsmsgs + " WHERE " + DB.gu_msg + "='" + getString(DB.gu_parent_msg) + "'");
            if (oRSet.next()) put(DB.gu_thread_msg, oRSet.getString(1)); else put(DB.gu_thread_msg, sMsgId);
            oRSet.close();
            oStmt.close();
        } else put(DB.gu_thread_msg, sMsgId);
        if (oConn.getDataBaseProduct() == JDCConnection.DBMS_POSTGRESQL) {
            sSQL = "SELECT k_sp_count_thread_msgs ('" + getString(DB.gu_thread_msg) + "')";
            oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSQL + ")");
            oRSet = oStmt.executeQuery(sSQL);
            oRSet.next();
            nThreadMsgs = oRSet.getInt(1);
            oRSet.close();
            oStmt.close();
        } else {
            sSQL = "{ call k_sp_count_thread_msgs('" + getString(DB.gu_thread_msg) + "',?)}";
            if (DebugFile.trace) DebugFile.writeln("CallableStatement.prepareCall(" + sSQL + ")");
            oCall = oConn.prepareCall(sSQL);
            oCall.registerOutParameter(1, Types.INTEGER);
            oCall.execute();
            nThreadMsgs = oCall.getInt(1);
            oCall.close();
        }
        replace(DB.nu_thread_msgs, ++nThreadMsgs);
        if (!AllVals.containsKey(DB.dt_start)) put(DB.dt_start, dtNow);
        if (!AllVals.containsKey(DB.id_msg_type)) put(DB.id_msg_type, "TXT");
        replace(DB.dt_published, dtNow);
        bRetVal = super.store(oConn);
        oStmt = oConn.createStatement();
        sSQL = "UPDATE " + DB.k_newsmsgs + " SET " + DB.nu_thread_msgs + "=" + String.valueOf(nThreadMsgs) + " WHERE " + DB.gu_thread_msg + "='" + getString(DB.gu_thread_msg) + "'";
        if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate(" + sSQL + ")");
        oStmt.executeUpdate(sSQL);
        oStmt.close();
        if (!AllVals.containsKey(DB.gu_newsgrp) && !sMsgId.equals(get(DB.gu_thread_msg))) {
            sSQL = "SELECT " + DB.gu_category + " FROM " + DB.k_x_cat_objs + " WHERE " + DB.gu_object + "='" + getStringNull(DB.gu_thread_msg, "null") + "'";
            oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSQL + ")");
            oRSet = oStmt.executeQuery(sSQL);
            if (oRSet.next()) put(DB.gu_newsgrp, oRSet.getString(1));
            oRSet.close();
            oStmt.close();
        }
        if (bRetVal && AllVals.containsKey(DB.gu_newsgrp)) {
            if (DebugFile.trace) DebugFile.writeln("Category.store() && containsKey(DB.gu_newsgrp)");
            if (bNewMsg) {
                if (DebugFile.trace) DebugFile.writeln("new message");
                boolean bHasLastUpdate;
                try {
                    bHasLastUpdate = (((DBBind) (oConn.getPool().getDatabaseBinding())).getDBTable(DB.k_newsgroups).getColumnByName(DB.dt_last_update) != null);
                } catch (NullPointerException npe) {
                    bHasLastUpdate = true;
                }
                if (bHasLastUpdate) {
                    PreparedStatement oUpdt = null;
                    sSQL = "UPDATE " + DB.k_newsgroups + " SET " + DB.dt_last_update + "=? WHERE " + DB.gu_newsgrp + "=?";
                    if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement (" + sSQL + ")");
                    oUpdt = oConn.prepareStatement(sSQL);
                    oUpdt.setTimestamp(1, new Timestamp(new Date().getTime()));
                    oUpdt.setObject(2, AllVals.get(DB.gu_newsgrp), java.sql.Types.VARCHAR);
                    oUpdt.executeUpdate();
                    oUpdt.close();
                    oUpdt = null;
                }
            } else {
                sSQL = "DELETE FROM " + DB.k_x_cat_objs + " WHERE " + DB.gu_category + "='" + getString(DB.gu_newsgrp) + "' AND " + DB.gu_object + "='" + sMsgId + "'";
                oStmt = oConn.createStatement();
                if (DebugFile.trace) DebugFile.writeln("Statement.execute(" + sSQL + ")");
                oStmt.execute(sSQL);
                oStmt.close();
            }
            sSQL = "INSERT INTO " + DB.k_x_cat_objs + "(" + DB.gu_category + "," + DB.gu_object + "," + DB.id_class + "," + DB.bi_attribs + "," + DB.od_position + ") VALUES ('" + getString(DB.gu_newsgrp) + "','" + sMsgId + "'," + String.valueOf(NewsMessage.ClassId) + ",0,NULL)";
            oStmt = oConn.createStatement();
            if (DebugFile.trace) DebugFile.writeln("Statement.execute(" + sSQL + ")");
            oStmt.execute(sSQL);
            oStmt.close();
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End NewsMessage.store() : " + String.valueOf(bRetVal));
        }
        return bRetVal;
    }

    /**
   * <p>Delete NewsMessage.</p>
   * Files attached to NewsMessage (stored as Products) are delete prior to
   * the NewsMessage itself. Then k_sp_del_newsmsg stored procedure is called.
   * @param oConn Database Connection
   * @throws SQLException
   */
    public boolean delete(JDCConnection oConn) throws SQLException {
        Product oProd;
        Statement oStmt;
        CallableStatement oCall;
        String sSQL;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin NewsMessage.delete([Connection])");
            DebugFile.incIdent();
            DebugFile.writeln("gu_msg=" + getStringNull(DB.gu_msg, "null"));
        }
        if (!isNull(DB.gu_product)) {
            oProd = new Product(oConn, getString(DB.gu_product));
            oStmt = oConn.createStatement();
            sSQL = "UPDATE " + DB.k_newsmsgs + " SET " + DB.gu_product + "=NULL WHERE " + DB.gu_msg + "='" + getString(DB.gu_msg) + "'";
            if (DebugFile.trace) DebugFile.writeln("Statement.executeUpdate(" + sSQL + ")");
            oStmt.executeUpdate(sSQL);
            oStmt.close();
            oProd.delete(oConn);
            remove(DB.gu_product);
        }
        sSQL = "{ call k_sp_del_newsmsg ('" + getString(DB.gu_msg) + "') }";
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareCall(" + sSQL + ")");
        oCall = oConn.prepareCall(sSQL);
        oCall.execute();
        oCall.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End NewsMessage.delete() : true");
        }
        return true;
    }

    /**
   * <p>Delete NewsMessage.</p>
   * Files attached to NewsMessage (stored as Products) are delete prior to
   * the NewsMessage itself. Then k_sp_del_newsmsg stored procedure is called.
   * @param oConn Database Connection
   * @param sNewsMsgGUID GUID of NewsMessage to be deleted
   * @throws SQLException
   */
    public static boolean delete(JDCConnection oConn, String sNewsMsgGUID) throws SQLException {
        NewsMessage oMsg = new NewsMessage();
        if (oMsg.load(oConn, new Object[] { sNewsMsgGUID })) return oMsg.delete(oConn); else return false;
    }

    public static final short STATUS_VALIDATED = 0;

    public static final short STATUS_PENDING = 1;

    public static final short STATUS_DISCARDED = 2;

    public static final short STATUS_EXPIRED = 3;

    public static final short ClassId = 31;
}

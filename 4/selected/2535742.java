package com.knowgate.scheduler;

import java.util.Date;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import com.knowgate.debug.DebugFile;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataobjs.DBBind;
import com.knowgate.dataobjs.DBSubset;
import com.knowgate.misc.Gadgets;
import com.knowgate.hipergate.QueryByForm;
import com.knowgate.crm.DistributionList;
import java.util.Properties;

/**
 * <p>Feeds atoms to RAM based AtomQueue</p>
 * @author Sergio Montoro Ten
 * @version 2.0
 */
public class AtomFeeder {

    private int iMaxBatchSize;

    public AtomFeeder() {
        iMaxBatchSize = 10000;
    }

    public void setMaxBatchSize(int iMaxBatch) {
        iMaxBatchSize = iMaxBatch;
    }

    public int getMaxBatchSize() {
        return iMaxBatchSize;
    }

    /**
   * <p>Load a dynamic list of members from k_member_address to k_job_atoms</p>
   * <p>Registers will be filtered according to the query stored at k_queries table
   * witch corresponds to the list at k_lists used by Job being loaded.</p>
   * @param oConn Database Connection
   * @param sJobGUID Job to be loaded
   * @param dtExec Scheduled Execution DateTime
   * @param sListGUID Base List GUID
   * @param sQueryGUID GUID of Query to be used for member filtering upon retrieval
   * @param sWorkAreaGUID GUID of WorArea
   * @throws SQLException
   */
    private int loadDynamicList(JDCConnection oConn, String sJobGUID, Date dtExec, String sListGUID, String sQueryGUID, String sWorkAreaGUID) throws SQLException {
        Statement oStmt;
        QueryByForm oQBF;
        String sSQL;
        int iInserted;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin AtomFeeder.loadDynamicList([Connection] , " + sJobGUID + "," + dtExec.toString() + "," + sQueryGUID + "," + sWorkAreaGUID + " )");
            DebugFile.incIdent();
        }
        String sColumns = "gu_company,gu_contact,tx_email,tx_name,tx_surname,tx_salutation,nm_commercial,tp_street,nm_street,nu_street,tx_addr1,tx_addr2,nm_country,nm_state,mn_city,zipcode,work_phone,direct_phone,home_phone,mov_phone,fax_phone,other_phone,po_box";
        oQBF = new QueryByForm(oConn, DB.k_member_address, "ma", sQueryGUID);
        oStmt = oConn.createStatement();
        sSQL = "INSERT INTO " + DB.k_job_atoms + " (gu_job,id_status," + sColumns + ") " + " (SELECT '" + sJobGUID + "'," + String.valueOf(Atom.STATUS_PENDING) + "," + sColumns + " FROM " + DB.k_member_address + " ma WHERE ma.gu_workarea='" + sWorkAreaGUID + "' AND (" + oQBF.composeSQL() + ") AND NOT EXISTS (SELECT " + DB.tx_email + " FROM " + DB.k_x_list_members + " b WHERE b." + DB.gu_query + "='" + sListGUID + "' AND b." + DB.tp_list + "=" + String.valueOf(DistributionList.TYPE_BLACK) + " AND b." + DB.tx_email + "=ma." + DB.tx_email + "))";
        if (DebugFile.trace) DebugFile.writeln("Connection.executeUpdate(" + sSQL + ")");
        iInserted = oStmt.executeUpdate(sSQL);
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End AtomFeeder.loadDynamicList() : " + String.valueOf(iInserted));
        }
        return iInserted;
    }

    /**
   * <p>Load a static member list from k_x_list_members to k_job_atoms</p>
   * @param oConn Database Connection
   * @param sJobGUID GUID of Job to be loaded
   * @param dtExec Execution date to be assigned to Atoms (inherited from job)
   * @param sListGUID GUID of list to be loaded
   * @throws SQLException
   */
    private int loadStaticList(JDCConnection oConn, String sJobGUID, Date dtExec, String sListGUID) throws SQLException {
        Statement oStmt;
        String sSQL;
        int iInserted;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin AtomFeeder.loadStaticList([Connection] , " + sJobGUID + "," + dtExec.toString() + "," + sListGUID + ")");
            DebugFile.incIdent();
        }
        String sColumns = "id_format,gu_company,gu_contact,tx_email,tx_name,tx_surname,tx_salutation";
        oStmt = oConn.createStatement();
        sSQL = "INSERT INTO " + DB.k_job_atoms + " (gu_job,id_status," + sColumns + ") " + " (SELECT '" + sJobGUID + "'," + String.valueOf(Atom.STATUS_PENDING) + "," + sColumns + " FROM " + DB.k_x_list_members + " m WHERE " + DB.gu_list + "='" + sListGUID + "' AND m." + DB.bo_active + "<>0 AND NOT EXISTS (SELECT " + DB.tx_email + " FROM " + DB.k_x_list_members + " b WHERE b." + DB.gu_query + "='" + sListGUID + "' AND b." + DB.tp_list + "=" + String.valueOf(DistributionList.TYPE_BLACK) + " AND b." + DB.tx_email + "=m." + DB.tx_email + "))";
        if (DebugFile.trace) DebugFile.writeln("Connection.executeUpdate(" + sSQL + ")");
        iInserted = oStmt.executeUpdate(sSQL);
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End AtomFeeder.loadStaticList() : " + String.valueOf(iInserted));
        }
        return iInserted;
    }

    /**
   * <p>Load direct list into k_job_atoms table</p>
   * @param oConn Database Connection
   * @param sJobGUID GUID of Job to be loaded
   * @param dtExec Execution date to be assigned to Atoms (inherited from job)
   * @param sListGUID GUID of list to be loaded
   * @throws SQLException
   */
    private int loadDirectList(JDCConnection oConn, String sJobGUID, Date dtExec, String sListGUID) throws SQLException {
        return loadStaticList(oConn, sJobGUID, dtExec, sListGUID);
    }

    private Properties parseParameters(String sTxParams) {
        String aVariable[];
        String aParams[];
        Properties oParams = new Properties();
        if (DebugFile.trace) {
            DebugFile.writeln("Begin AtomFeeder.parseParameters(" + sTxParams + ")");
            DebugFile.incIdent();
        }
        if (sTxParams != null) {
            if (sTxParams.length() > 0) {
                aParams = Gadgets.split(sTxParams, ",");
                for (int p = 0; p < aParams.length; p++) {
                    aVariable = Gadgets.split(aParams[p], ":");
                    oParams.put(aVariable[0], aVariable[1]);
                }
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End AtomFeeder.parseParameters() : " + String.valueOf(oParams.size()));
        }
        return oParams;
    }

    /**
   * <p>Load an Atom batch into k_job_atoms table</p>
   * <p>Atoms will be taken by looking up pending Jobs by its execution date and extracting Atoms
   * for nearest Jobs in time.<br>
   * On each loadAtoms() no more than iWorkerThreads Jobs will be loaded at a time.
   * @param oConn Database Connection
   * @param iWorkerThreads Number of worker thread. This parameter will limit the number of loaded Jobs as the program will try to use a one to one ratio between Jobs and WorkerThreads.
   * @return DBSubset with loaded Jobs
   * @throws SQLException
   */
    public DBSubset loadAtoms(JDCConnection oConn, int iWorkerThreads) throws SQLException {
        PreparedStatement oCmdsStmt;
        PreparedStatement oJobStmt;
        ResultSet oCmdsSet;
        DBSubset oJobsSet;
        int iJobCount;
        String aParams[];
        String aVariable[];
        Properties oParams;
        DistributionList oDistribList;
        Date dtNow = new Date();
        Date dtExec;
        String sSQL;
        int iLoaded = 0;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin AtomFeeder.loadAtoms([Connection], " + String.valueOf(iWorkerThreads) + ")");
            DebugFile.incIdent();
        }
        oJobsSet = new DBSubset(DB.k_jobs, "gu_job,gu_job_group,gu_workarea,id_command,tx_parameters,id_status,dt_execution,dt_finished,dt_created,dt_modified", DB.id_status + "=" + String.valueOf(Job.STATUS_PENDING) + " ORDER BY " + DB.dt_execution + " DESC", iWorkerThreads);
        oJobsSet.setMaxRows(iWorkerThreads);
        iJobCount = oJobsSet.load(oConn);
        sSQL = "UPDATE " + DB.k_jobs + " SET " + DB.id_status + "=" + String.valueOf(Job.STATUS_RUNNING) + "," + DB.dt_execution + "=" + DBBind.Functions.GETDATE + " WHERE " + DB.gu_job + "=?";
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
        oJobStmt = oConn.prepareStatement(sSQL);
        for (int j = 0; j < iJobCount; j++) {
            oParams = parseParameters(oJobsSet.getString(4, j));
            if (oParams.getProperty("gu_list") != null) {
                oDistribList = new DistributionList(oConn, oParams.getProperty("gu_list"));
                if (oDistribList.isNull(DB.dt_execution)) dtExec = dtNow; else dtExec = oDistribList.getDate(DB.dt_execution);
                switch(oDistribList.getShort(DB.tp_list)) {
                    case DistributionList.TYPE_DYNAMIC:
                        iLoaded += loadDynamicList(oConn, oJobsSet.getString(0, j), dtExec, oParams.getProperty("gu_list"), oDistribList.getString(DB.gu_query), oDistribList.getString(DB.gu_workarea));
                        break;
                    case DistributionList.TYPE_STATIC:
                        iLoaded += loadStaticList(oConn, oJobsSet.getString(0, j), dtExec, oParams.getProperty("gu_list"));
                        break;
                    case DistributionList.TYPE_DIRECT:
                        iLoaded += loadDirectList(oConn, oJobsSet.getString(0, j), dtExec, oParams.getProperty("gu_list"));
                        break;
                }
            } else iLoaded = 0;
            if (DebugFile.trace) DebugFile.writeln("PrepareStatement.setString(1, '" + oJobsSet.getStringNull(0, j, "") + "')");
            oJobStmt.setString(1, oJobsSet.getString(0, j));
            if (DebugFile.trace) DebugFile.writeln("PrepareStatement.executeUpdate()");
            oJobStmt.executeUpdate();
        }
        if (DebugFile.trace) DebugFile.writeln("PrepareStatement.close()");
        oJobStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End AtomFeeder.loadAtoms() : " + String.valueOf(oJobsSet.getRowCount()));
        }
        return oJobsSet;
    }

    /**
   * <p>Load Atoms for a given Job into k_job_atoms table</p>
   * On each loadAtoms() no more than iWorkerThreads Jobs will be loaded at a time.
   * @param oConn Database Connection
   * @param sJodId GUID of Job for witch atoms are to be loaded.
   * @return DBSubset with loaded Job
   * @throws SQLException
   */
    public DBSubset loadAtoms(JDCConnection oConn, String sJobId) throws SQLException {
        PreparedStatement oCmdsStmt;
        PreparedStatement oJobStmt;
        ResultSet oCmdsSet;
        DBSubset oJobsSet;
        int iJobCount;
        String aParams[];
        String aVariable[];
        Properties oParams;
        DistributionList oDistribList;
        Date dtNow = new Date();
        Date dtExec;
        String sSQL;
        int iLoaded = 0;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin AtomFeeder.loadAtoms([Connection], " + sJobId + ")");
            DebugFile.incIdent();
        }
        oJobsSet = new DBSubset(DB.k_jobs, "gu_job,gu_job_group,gu_workarea,id_command,tx_parameters,id_status,dt_execution,dt_finished,dt_created,dt_modified", DB.gu_job + "='" + sJobId + "'", 1);
        iJobCount = oJobsSet.load(oConn);
        sSQL = "UPDATE " + DB.k_jobs + " SET " + DB.id_status + "=" + String.valueOf(Job.STATUS_RUNNING) + "," + DB.dt_execution + "=" + DBBind.Functions.GETDATE + " WHERE " + DB.gu_job + "=?";
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
        oJobStmt = oConn.prepareStatement(sSQL);
        if (1 == iJobCount) {
            oParams = parseParameters(oJobsSet.getString(4, 0));
            if (oParams.getProperty("gu_list") != null) {
                oDistribList = new DistributionList(oConn, oParams.getProperty("gu_list"));
                if (oDistribList.isNull(DB.dt_execution)) dtExec = dtNow; else dtExec = oDistribList.getDate(DB.dt_execution);
                switch(oDistribList.getShort(DB.tp_list)) {
                    case DistributionList.TYPE_DYNAMIC:
                        iLoaded += loadDynamicList(oConn, oJobsSet.getString(0, 0), dtExec, oParams.getProperty("gu_list"), oDistribList.getString(DB.gu_query), oDistribList.getString(DB.gu_workarea));
                        break;
                    case DistributionList.TYPE_STATIC:
                        iLoaded += loadStaticList(oConn, oJobsSet.getString(0, 0), dtExec, oParams.getProperty("gu_list"));
                        break;
                    case DistributionList.TYPE_DIRECT:
                        iLoaded += loadDirectList(oConn, oJobsSet.getString(0, 0), dtExec, oParams.getProperty("gu_list"));
                        break;
                }
            } else iLoaded = 0;
            if (DebugFile.trace) DebugFile.writeln("PrepareStatement.setString(1, '" + oJobsSet.getStringNull(0, 0, "") + "')");
            oJobStmt.setString(1, oJobsSet.getString(0, 0));
            if (DebugFile.trace) DebugFile.writeln("PrepareStatement.executeUpdate()");
            oJobStmt.executeUpdate();
        }
        if (DebugFile.trace) DebugFile.writeln("PrepareStatement.close()");
        oJobStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End AtomFeeder.loadAtoms() : " + sJobId);
        }
        return oJobsSet;
    }

    /**
   * <p>Feed RAM queue with pending Atoms from k_job_atoms table</p>
   * @param oConn Database Connection
   * @param oQueue AtomQueue
   * @throws SQLException
   */
    public void feedQueue(JDCConnection oConn, AtomQueue oQueue) throws SQLException {
        Statement oStmt;
        PreparedStatement oUpdt;
        PreparedStatement oPgSt;
        ResultSet oRSet;
        ResultSetMetaData oMDat;
        String sJobId;
        int iAtomId;
        int iJobCol;
        int iAtmCol;
        int iProcessed;
        String sSQL;
        Atom oAtm;
        boolean bNext;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin AtomFeeder.feedQueue([Connection], [AtomQueue])");
            DebugFile.incIdent();
        }
        oStmt = oConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        sSQL = "SELECT a.*, j." + DB.tx_parameters + " FROM " + DB.k_job_atoms + " a, " + DB.k_jobs + " j WHERE a." + DB.id_status + "=" + String.valueOf(Atom.STATUS_PENDING) + " AND j." + DB.gu_job + "=a." + DB.gu_job + " ORDER BY j." + DB.dt_execution;
        if (DebugFile.trace) DebugFile.writeln("Statement.executeQuery(" + sSQL + ")");
        oRSet = oStmt.executeQuery(sSQL);
        try {
            oRSet.setFetchSize(getMaxBatchSize());
        } catch (SQLException e) {
        }
        oMDat = oRSet.getMetaData();
        iJobCol = oRSet.findColumn(DB.gu_job);
        iAtmCol = oRSet.findColumn(DB.pg_atom);
        sSQL = "UPDATE " + DB.k_job_atoms + " SET " + DB.id_status + "=" + Atom.STATUS_RUNNING + " WHERE " + DB.gu_job + "=? AND " + DB.pg_atom + "=?";
        if (DebugFile.trace) DebugFile.writeln("Connection.prepareStatement(" + sSQL + ")");
        oUpdt = oConn.prepareStatement(sSQL);
        iProcessed = 0;
        bNext = oRSet.next();
        while (bNext && iProcessed < iMaxBatchSize) {
            oAtm = new Atom(oRSet, oMDat);
            oQueue.push(oAtm);
            sJobId = oRSet.getString(iJobCol);
            iAtomId = oRSet.getInt(iAtmCol);
            bNext = oRSet.next();
            oUpdt.setString(1, sJobId);
            oUpdt.setInt(2, iAtomId);
            if (DebugFile.trace) DebugFile.writeln("PreparedStatement.executeUpdate(UPDATE " + DB.k_job_atoms + " SET " + DB.id_status + "=" + Atom.STATUS_RUNNING + " WHERE " + DB.gu_job + "='" + sJobId + "' AND " + DB.pg_atom + "=" + String.valueOf(iAtomId) + ")");
            oUpdt.executeUpdate();
            iProcessed++;
        }
        oUpdt.close();
        oRSet.close();
        oStmt.close();
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End AtomFeeder.feedQueue() : " + String.valueOf(iProcessed));
        }
    }

    /**
   * Formatea una fecha en formato escape ODBC
   * @param dt Fecha a formatear
   * @param sFormat tipo de formato {d=yyyy-mm-dd, ts=yyyy-mm-dd hh:nn:ss}
   * @return Fecha formateada como una cadena
   */
    private static String escape(java.util.Date dt) {
        String str;
        String sMonth, sDay, sHour, sMin, sSec;
        str = "{ ts '";
        sMonth = (dt.getMonth() + 1 < 10 ? "0" + String.valueOf((dt.getMonth() + 1)) : String.valueOf(dt.getMonth() + 1));
        sDay = (dt.getDate() < 10 ? "0" + String.valueOf(dt.getDate()) : String.valueOf(dt.getDate()));
        str += String.valueOf(dt.getYear() + 1900) + "-" + sMonth + "-" + sDay + " ";
        sHour = (dt.getHours() < 10 ? "0" + String.valueOf(dt.getHours()) : String.valueOf(dt.getHours()));
        sMin = (dt.getMinutes() < 10 ? "0" + String.valueOf(dt.getMinutes()) : String.valueOf(dt.getMinutes()));
        sSec = (dt.getSeconds() < 10 ? "0" + String.valueOf(dt.getSeconds()) : String.valueOf(dt.getSeconds()));
        str += " " + sHour + ":" + sMin + ":" + sSec;
        str = str.trim() + "'}";
        return str;
    }
}

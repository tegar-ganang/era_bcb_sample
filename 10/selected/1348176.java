package org.openXpertya.replication;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.openXpertya.model.MAsyncReplication;
import org.openXpertya.model.PO;
import org.openXpertya.model.X_AD_ReplicationError;
import org.openXpertya.process.SvrProcess;
import org.openXpertya.util.DB;
import org.openXpertya.util.Env;
import org.openXpertya.util.Trx;

/**
 * Procesa la tabla de replicación asincronica a fin de 
 * impactar las entradas existentes en las tablas de producción
 */
public class AsyncReplicationProcess extends SvrProcess {

    /** ID de esta organización */
    int thisOrgID = -1;

    @Override
    protected void prepare() {
        thisOrgID = DB.getSQLValue(get_TrxName(), "SELECT AD_Org_ID FROM AD_ReplicationHost WHERE thisHost = 'Y' AND AD_Client_ID = " + getAD_Client_ID());
    }

    @Override
    protected String doIt() throws Exception {
        if (thisOrgID == -1) throw new Exception(" Sin marca de host.  Debe realizar la configuración correspondiente en la ventana Hosts de Replicación ");
        int[] orgs = PO.getAllIDs("AD_Org", " isActive = 'Y' AND AD_Client_ID = " + getAD_Client_ID() + " AND AD_Org_ID != " + thisOrgID, get_TrxName());
        int initialChangelog_ID = -1, finalChangelog_ID = -1, cur_initialChangelog_ID = -1, new_initialChangelog_ID = -1, AD_AsyncReplication_ID = -1;
        for (int i = 0; i < orgs.length; i++) {
            try {
                PreparedStatement pstmt = DB.prepareStatement(" SELECT async_content, initialchangelog_id, finalchangelog_id, AD_AsyncReplication_ID, async_action " + " FROM AD_AsyncReplication " + " WHERE Org_Source_ID = " + orgs[i] + " AND isActive = 'Y' " + " AND (async_status IS NULL OR async_status = '" + MAsyncReplication.ASYNC_STATUS_ErrorInReplication + "')" + " ORDER BY AD_AsyncReplication_ID ASC ", get_TrxName());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    initialChangelog_ID = rs.getInt("initialchangelog_id");
                    finalChangelog_ID = rs.getInt("finalchangelog_id");
                    AD_AsyncReplication_ID = rs.getInt("AD_AsyncReplication_ID");
                    new_initialChangelog_ID = rs.getInt("initialchangelog_id");
                    if (cur_initialChangelog_ID == -1) cur_initialChangelog_ID = new_initialChangelog_ID;
                    if (cur_initialChangelog_ID != new_initialChangelog_ID) {
                        DB.executeUpdate(" UPDATE AD_AsyncReplication SET Async_Status = '" + MAsyncReplication.ASYNC_STATUS_Replicated + "'" + " WHERE AD_AsyncReplication_ID = " + AD_AsyncReplication_ID, get_TrxName());
                        Trx.getTrx(get_TrxName()).commit();
                        cur_initialChangelog_ID = new_initialChangelog_ID;
                    }
                    boolean delayedInsert = MAsyncReplication.ASYNC_ACTION_DelayedReplicate.equals(rs.getString("async_action"));
                    ReplicationXMLUpdater.processChangelog(rs.getString("async_content"), get_TrxName(), orgs[i], initialChangelog_ID, finalChangelog_ID);
                }
            } catch (Exception e) {
                Trx.getTrx(get_TrxName()).rollback();
                e.printStackTrace();
                X_AD_ReplicationError aLog = new X_AD_ReplicationError(getCtx(), 0, get_TrxName());
                aLog.setORG_Target_ID(orgs[i]);
                aLog.setInitialChangelog_ID(cur_initialChangelog_ID);
                aLog.setFinalChangelog_ID(finalChangelog_ID);
                aLog.setReplication_Type(X_AD_ReplicationError.REPLICATION_TYPE_Asynchronous);
                aLog.setReplication_Error(Env.getDateTime("yyyy/MM/dd-HH:mm:ss.SSS") + " - Error local en replicación asincrónica. AD_Async_Replication_ID:" + AD_AsyncReplication_ID + ". Error:" + e.getMessage());
                aLog.setClientOrg(getAD_Client_ID(), thisOrgID);
                aLog.save();
                Trx.getTrx(get_TrxName()).commit();
            }
        }
        return "FINALIZADO";
    }
}

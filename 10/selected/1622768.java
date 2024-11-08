package org.openXpertya.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import org.openXpertya.model.X_I_ReportLineSet;
import org.openXpertya.model.X_PA_ReportSource;
import org.openXpertya.report.CReportLine;
import org.openXpertya.report.MReportLineSet;
import org.openXpertya.util.CLogger;
import org.openXpertya.util.DB;
import org.openXpertya.util.Trx;

/**
 *	Import ReportLines from I_ReportLineSet_Lines
 *
 * 	@author Comunidad de Desarrollo OpenXpertya 
 *         *Basado en Codigo Original Modificado, Revisado y Optimizado de:
 *         *Copyright � 	Jorg Janke
 * 	@version 	$Id: ImportReportLine.java,v 0.9 $
 */
public class ImportReportLineSetLines extends SvrProcess {

    private CLogger log = CLogger.getCLogger(ImportReportLineSetLines.class);

    /**
	 * 	Import ReportLine Constructor
	 */
    public ImportReportLineSetLines() {
        super();
        log.log(Level.FINER, "ImportReportLineSetLines");
    }

    /**	Cliente al que se importar�n los datos		*/
    private int m_AD_Client_ID = 0;

    /** Default Report Line Set			*/
    private int m_PA_ReportLineSet_ID = 0;

    /**	Delete old Imported				*/
    private boolean m_deleteOldImported = false;

    /** C_Element: elemento del arbol contable */
    private int m_C_Element_ID = 0;

    private org.openXpertya.model.MElement m_MElement = null;

    private int m_AD_Tree_ID = 0;

    /** Organization a la que se importara	*/
    private int m_AD_Org_ID = 0;

    /** Effective						*/
    private Timestamp m_DateValue = null;

    /** Transaccion 						*/
    private String trxName = null;

    /** Comprobaciones del cliente									*/
    private String clientCheck = " AND AD_Client_ID=" + m_AD_Client_ID;

    /**
	 *  Prepare - e.g., get Parameters.
	 */
    protected void prepare() {
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (para[i].getParameter() == null) ; else if (name.equals("AD_Client_ID")) m_AD_Client_ID = ((BigDecimal) para[i].getParameter()).intValue(); else if (name.equals("PA_ReportLineSet_ID")) m_PA_ReportLineSet_ID = ((BigDecimal) para[i].getParameter()).intValue(); else if (name.equals("DeleteOldImported")) m_deleteOldImported = "Y".equals(para[i].getParameter()); else if (name.equals("C_Element_ID")) {
                m_C_Element_ID = ((BigDecimal) para[i].getParameter()).intValue();
                m_MElement = new org.openXpertya.model.MElement(getCtx(), m_C_Element_ID, get_TrxName());
            } else log.log(Level.SEVERE, "ImportReportLine.prepare - Unknown Parameter: " + name);
        }
        if (m_DateValue == null) m_DateValue = new Timestamp(System.currentTimeMillis());
    }

    /**
	 *  Perrform process.
	 *  @return Message
	 *  @throws Exception
	 */
    protected String doIt() throws java.lang.Exception {
        trxName = Trx.createTrxName();
        Trx trx = Trx.get(trxName, true);
        trx.start();
        if (m_AD_Client_ID == 0 || m_C_Element_ID == 0 || m_MElement == null || m_MElement.getC_Element_ID() == 0) {
            return "Falta alguno de los datos para la importacion (Compañia o Elemento)";
        }
        m_AD_Tree_ID = m_MElement.getAD_Tree_ID();
        StringBuffer sql = null;
        int no = 0;
        if (m_deleteOldImported) {
            sql = new StringBuffer("DELETE I_ReportLineSet " + "WHERE I_IsImported='Y'").append(clientCheck);
            no = DB.executeUpdate(sql.toString(), trxName);
            log.log(Level.FINER, "ImportReportLineSetLines.doIt", "Delete Old Imported =" + no);
        }
        sql = new StringBuffer("UPDATE I_ReportLineSet " + "SET AD_Client_ID = COALESCE (AD_Client_ID, ").append(m_AD_Client_ID).append(")," + " AD_Org_ID = COALESCE (AD_Org_ID, 0)," + " IsActive = COALESCE (IsActive, 'Y')," + " Created = COALESCE (Created, SysDate)," + " CreatedBy = COALESCE (CreatedBy, 0)," + " Updated = COALESCE (Updated, SysDate)," + " UpdatedBy = COALESCE (UpdatedBy, 0)," + " I_ErrorMsg = NULL," + " I_IsImported = 'N' " + "WHERE I_IsImported<>'Y' OR I_IsImported IS NULL");
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLineSet.doIt", "Reset=" + no);
        sql = new StringBuffer("UPDATE I_ReportLineSet " + "SET SeqNo=I_ReportLineSet_ID " + "WHERE SeqNo IS NULL" + " AND I_IsImported='N'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Set SeqNo Default=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine " + "SET IsSummary='N' " + "WHERE IsSummary IS NULL OR IsSummary NOT IN ('Y','N')" + " AND I_IsImported<>'Y'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Set IsSummary Default=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine " + "SET IsPrinted='Y' " + "WHERE IsPrinted IS NULL OR IsPrinted NOT IN ('Y','N')" + " AND I_IsImported<>'Y'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Set IsPrinted Default=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine " + "SET LineType='D' " + "WHERE LineType IS NULL OR LineType NOT IN ('S','C','D')" + " AND I_IsImported<>'Y'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Set LineType Default=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid CalculationType, ' " + "WHERE AmountType IS NOT NULL AND UPPER(AmountType) NOT IN ('BP','CP','DP','QP', 'BY','CY','DY','QY', 'BT','CT','DT','QT')" + " AND I_IsImported<>'Y'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Invalid AmountType=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine " + "SET I_IsImported='E', I_ErrorMsg=I_ErrorMsg||'ERR=Invalid CalculationType, ' " + "WHERE PostingType IS NOT NULL AND PostingType NOT IN ('A','B','E','S')" + " AND I_IsImported<>'Y'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Invalid PostingType=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine i " + "SET PA_ReportLineSet_ID=(SELECT PA_ReportLineSet_ID FROM PA_ReportLineSet r" + " WHERE i.Name=r.Name  AND ROWNUM=1) " + "WHERE PA_ReportLineSet_ID IS NULL " + " AND I_IsImported='N'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Set PA_ReportLine_ID=" + no);
        sql = new StringBuffer("UPDATE I_ReportLine i " + "SET PA_ReportLine_ID=(SELECT PA_ReportLine_ID FROM PA_ReportLine r" + " WHERE i.Name=r.Name  AND ROWNUM=1) " + "WHERE PA_ReportLine_ID IS NULL AND PA_ReportLineSet_ID IS NOT NULL" + " AND I_IsImported='N'").append(clientCheck);
        no = DB.executeUpdate(sql.toString(), trxName);
        log.log(Level.FINER, "ImportReportLine.doIt", "Set PA_ReportLine_ID=" + no);
        if (importReportLineSet() == false) {
            trx.rollback();
            trx.close();
            return "No se ha podido crear el conjunto de lineas de informe";
        }
        if (importReportLines() == false) {
            trx.rollback();
            trx.close();
            return "No se han podido crear las lineas del informe";
        }
        if (importReportLineSource() == false) {
            trx.rollback();
            trx.close();
            return "No se han podido crear las lineas fuente del informe";
        }
        trx.commit();
        trx.close();
        return "";
    }

    /** 
	 * Importamos el conjunto de lineas de informe
	 * @return
	 */
    private boolean importReportLineSet() {
        String sql = "select rls.rls_name, rls.rls_description from i_reportlineset rls where " + "rls.pa_reportlineset_id is null group by rls.rls_name, rls.rls_description";
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql, trxName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                MReportLineSet rls = new MReportLineSet(getCtx(), 0, trxName);
                rls.setName(rs.getString("rls_name"));
                rls.setDescription(rs.getString("rls_description"));
                rls.save(trxName);
                String upd_sql = "update i_reportlineset set pa_reportlineset_id=? where rls_name=? and rls_description=?";
                PreparedStatement upd_pstmt = DB.prepareStatement(upd_sql, trxName);
                upd_pstmt.setInt(1, rls.getPA_ReportLineSet_ID());
                upd_pstmt.setString(2, rls.getName());
                upd_pstmt.setString(3, rls.getDescription());
                upd_pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Fallo al crear el conjunto de lineas de informe: " + e);
            return false;
        }
        return true;
    }

    private boolean importReportLines() {
        String sql = "select * from i_reportlineset rls where " + "rls.pa_reportlineset_id is not null and rls.pa_reportline_id is null" + " and rls.isReportSource='N' ";
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql, trxName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                CReportLine rl = new CReportLine(getCtx(), 0, trxName);
                X_I_ReportLineSet ir = new X_I_ReportLineSet(getCtx(), rs, get_TrxName());
                rl.setName(ir.getName());
                rl.setDescription(ir.getDescription());
                rl.setPA_ReportLineSet_ID(ir.getPA_ReportLineSet_ID());
                rl.setSeqNo(ir.getSeqNo());
                rl.setPrintLineNo(ir.getPrintLineNo());
                rl.setFunc(ir.getFunc());
                rl.setLineType(ir.getLineType());
                rl.setPostingType(ir.getPostingType());
                rl.setAmountType(ir.getAmountType());
                rl.setIsPrinted(ir.isPrinted());
                rl.setChangeSign(ir.isChangeSign());
                rl.setNegativeAsZero(ir.isNegativeAsZero());
                rl.setIsEverPrinted(ir.isEverPrinted());
                rl.setIsPageBreak(ir.isPageBreak());
                rl.setIsBold(ir.isBold());
                rl.setIndentLevel(ir.getIndentLevel());
                if (rl.save() == false) {
                    log.log(Level.SEVERE, "Error grabando la linea de informe.");
                    return false;
                }
                ir.setPA_ReportLine_ID(rl.getPA_ReportLine_ID());
                ir.setI_IsImported(true);
                if (ir.save(trxName) == false) {
                    log.log(Level.SEVERE, "Error guardando el PA_ReportLine_ID en la tabla de importacion.");
                    return false;
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Fallo al crear el conjunto de lineas de informe: " + e);
            return false;
        }
        return true;
    }

    private boolean importReportLineSource() {
        String sql = "update i_reportlineset set pa_reportline_id= ( select rl.pa_reportline_id from pa_reportline rl, pa_reportlineset rls 	where rl.pa_reportlineset_id=rls.pa_reportlineset_id 	and rl.name=trim(i_reportlineset.name) and rls.name=trim(i_reportlineset.rls_name) and rl.ad_client_id=i_reportlineset.ad_client_id and rl.ad_org_id=i_reportlineset.ad_org_id and rls.ad_client_id=i_reportlineset.ad_client_id and rls.ad_org_id=i_reportlineset.ad_org_id) where isReportSource='Y'";
        if (DB.executeUpdate(sql, trxName) == -1) {
            log.log(Level.SEVERE, "Error actualizado el pa_reportline_id de las fuentes");
            return false;
        }
        sql = "update i_reportlineset as ir set c_elementvalue_id= ( select ev.c_elementvalue_id from c_elementvalue ev where ev.issummary='Y' and ev.value=trim(ir.ev_value) and ev.ad_org_id=ir.ad_org_id) where ir.isReportSource='Y' and ir.ev_value is not null and ir.c_elementvalue_id is null and ir.rs_elementType='AC'";
        if (DB.executeUpdate(sql, trxName) == -1) {
            log.log(Level.SEVERE, "Error actualizado el c_elementvalue_id de las fuentes");
            return false;
        }
        sql = "update i_reportlineset as ir set i_isimported='E', i_errorMsg='fuente invalida' where ir.isReportSource='Y' and (ir.c_elementvalue_id is null or ir.c_elementvalue_id=0) and ir.rs_elementtype='AC' and (ir.pa_reportline_id is null or ir.pa_reportline_id=0)";
        if (DB.executeUpdate(sql, trxName) == -1) {
            log.log(Level.SEVERE, "Error marcando fuentes erroneas.");
            return false;
        }
        sql = "select * from i_reportlineset as ir where ir.isReportSource='Y' and ir.I_isImported='N' and ir.rs_elementtype='AC'";
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql, trxName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                X_PA_ReportSource r = new X_PA_ReportSource(getCtx(), 0, trxName);
                X_I_ReportLineSet ir = new X_I_ReportLineSet(getCtx(), rs, trxName);
                r.set_TrxName(trxName);
                r.setElementType(r.ELEMENTTYPE_Account);
                r.setC_ElementValue_ID(ir.getC_ElementValue_ID());
                if (ir.getEv_Value().trim().equals("395")) {
                    log.log(Level.SEVERE, "PA_ReportLine_ID: " + ir.getPA_ReportLine_ID());
                }
                r.setPA_ReportLine_ID(ir.getPA_ReportLine_ID());
                if (r.save(trxName) == false) {
                    log.log(Level.SEVERE, "Error creando las fuentes del informe:" + ir.getEv_Value() + ": " + r.getPA_ReportLine_ID());
                    return false;
                }
                ir.setPA_ReportSource_ID(r.getPA_ReportSource_ID());
                ir.setI_IsImported(true);
                if (ir.save(trxName) == false) {
                    log.log(Level.SEVERE, "Error guardando el PA_ReportSource_ID en la tabla de importacion.");
                    return false;
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error recorriendo las lineas de fuente.");
            return false;
        }
        return true;
    }
}

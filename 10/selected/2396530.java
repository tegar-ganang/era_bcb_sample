package medisis.control;

import com.medication.medisis.IllnessInfo;
import com.medication.medisis.Medication;
import com.medication.medisis.MedicationDocument;
import com.medication.medisis.MedicationListDocument;
import com.medication.medisis.MedicationReport;
import com.medication.medisis.MedicationReportDocument;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.Stateless;
import javax.ejb.LocalBean;
import medisis.db.DBHelper;
import medisis.helper.MedisisKeyGenerator;
import medisis.sql.SQL;

/**
 *
 * @author kideo
 */
@Stateless
@LocalBean
public class MedicationControllerBean implements MedicationControllerBeanLocal {

    private DBHelper helper = null;

    public MedicationListDocument getMedicationList() {
        MedicationListDocument doc = null;
        ResultSet rs = null;
        try {
            helper = new DBHelper();
            doc = MedicationListDocument.Factory.newInstance();
            doc.addNewMedicationList();
            rs = helper.getResultSet(SQL.getMedicationList());
            while (rs.next()) {
                Medication entry = doc.getMedicationList().addNewMedication();
                entry.setMedno(rs.getString("MEDNO"));
                entry.setMedication(rs.getString("MEDICATION"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (SQLException ee) {
                ee.printStackTrace();
            }
        }
        return doc;
    }

    public MedicationListDocument searchForMedication(String sql) {
        ResultSet rsSearch = null;
        MedicationListDocument doc = null;
        try {
            helper = new DBHelper();
            PreparedStatement psSearch = helper.prepareStatement(sql);
            rsSearch = psSearch.executeQuery();
            doc = MedicationListDocument.Factory.newInstance();
            doc.addNewMedicationList();
            while (rsSearch.next()) {
                Medication m = doc.getMedicationList().addNewMedication();
                m.setMedno(rsSearch.getString("MEDNO"));
                m.setMedication(rsSearch.getString("MEDICATION"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsSearch != null) {
                    rsSearch.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    public MedicationReportDocument getMedicationReport() {
        MedicationReportDocument doc = null;
        ResultSet rsMeds = null;
        try {
            helper = new DBHelper();
            doc = MedicationReportDocument.Factory.newInstance();
            PreparedStatement psMeds = helper.prepareStatement(SQL.getMedicationReport());
            rsMeds = psMeds.executeQuery();
            String tmp = "";
            String mn = "";
            doc.addNewMedicationReport();
            if (rsMeds.next()) {
                while (true) {
                    boolean finished = false;
                    mn = rsMeds.getString("MEDNO");
                    if (tmp.equals("")) {
                        tmp = mn;
                    }
                    String name = rsMeds.getString("MEDICATION");
                    MedicationReport mm = doc.getMedicationReport().addNewMedication();
                    mm.setMedno(mn);
                    mm.setMedication(name);
                    while (tmp.equals(mn)) {
                        IllnessInfo ii = mm.addNewStats();
                        ii.setIllness(rsMeds.getString("ILLNESS"));
                        ii.setCount(new BigInteger(String.valueOf(rsMeds.getInt("COUNT"))));
                        if (!rsMeds.next()) {
                            finished = true;
                            break;
                        }
                        mn = rsMeds.getString("MEDNO");
                    }
                    if (finished) {
                        break;
                    }
                    tmp = mn;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsMeds != null) {
                    rsMeds.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    public boolean insertMedicationList(String medicationListDocument) {
        boolean ret = false;
        PreparedStatement psRollback = null;
        try {
            MedicationListDocument doc = MedicationListDocument.Factory.parse(medicationListDocument);
            helper = new DBHelper();
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psInsert = helper.prepareStatement(SQL.insertMedication());
            psRollback = helper.prepareStatement(SQL.rollback());
            for (int i = 0; i < doc.getMedicationList().getMedicationArray().length; i++) {
                doc.getMedicationList().getMedicationArray(i).setMedno(MedisisKeyGenerator.generate());
                psInsert.setString(1, doc.getMedicationList().getMedicationArray(i).getMedno());
                psInsert.setString(2, doc.getMedicationList().getMedicationArray(i).getMedication());
                psInsert.addBatch();
            }
            psBegin.executeUpdate();
            psInsert.executeBatch();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
            try {
                if (psRollback != null) {
                    psRollback.executeUpdate();
                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public boolean deleteMedication(String medno) {
        boolean ret = false;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            psRollback = helper.prepareStatement(SQL.rollback());
            PreparedStatement psDelete = helper.prepareStatement(SQL.deleteMedication());
            psDelete.setString(1, medno);
            psBegin.executeUpdate();
            psDelete.executeUpdate();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
            try {
                if (psRollback != null) {
                    psRollback.executeUpdate();
                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        } finally {
            try {
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}

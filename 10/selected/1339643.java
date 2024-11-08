package medisis.control;

import com.illness.medisis.Illness;
import com.illness.medisis.IllnessDocument;
import com.illness.medisis.IllnessListDocument;
import com.illness.medisis.IllnessStatsListDocument;
import com.illness.medisis.MedicalIllness;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
public class IllnessControllerBean implements IllnessControllerBeanLocal {

    private DBHelper helper = null;

    public IllnessListDocument getIllnessList() {
        IllnessListDocument doc = null;
        ResultSet rsList = null;
        try {
            helper = new DBHelper();
            doc = IllnessListDocument.Factory.newInstance();
            PreparedStatement psList = helper.prepareStatement(SQL.getIllnessList());
            rsList = psList.executeQuery();
            doc.addNewIllnessList();
            while (rsList.next()) {
                Illness i = doc.getIllnessList().addNewIllness();
                i.setIllnessno(rsList.getString("ILLNESSNO"));
                i.setIllness(rsList.getString("ILLNESS"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsList != null) {
                    rsList.close();
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

    public boolean insertIllness(String illnessDocument) {
        boolean ret = false;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            IllnessDocument doc = IllnessDocument.Factory.parse(illnessDocument);
            PreparedStatement psAdd = helper.prepareStatement(SQL.insertIllness());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            psRollback = helper.prepareStatement(SQL.rollback());
            psAdd.setString(1, MedisisKeyGenerator.generate());
            psAdd.setString(2, doc.getIllness().getIllness().getIllness());
            psBegin.executeUpdate();
            psAdd.executeUpdate();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
            try {
                psRollback.executeUpdate();
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

    public IllnessListDocument searchForIllness(String sql) {
        ResultSet rsResults = null;
        IllnessListDocument doc = null;
        try {
            helper = new DBHelper();
            PreparedStatement psResults = helper.prepareStatement(sql);
            rsResults = psResults.executeQuery();
            doc = IllnessListDocument.Factory.newInstance();
            doc.addNewIllnessList();
            while (rsResults.next()) {
                Illness i = doc.getIllnessList().addNewIllness();
                i.setIllness(rsResults.getString("ILLNESS"));
                i.setIllnessno(rsResults.getString("ILLNESSNO"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsResults != null) {
                    rsResults.close();
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

    public boolean deleteIllness(String illnessDocument) {
        boolean ret = false;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            IllnessDocument doc = IllnessDocument.Factory.parse(illnessDocument);
            psRollback = helper.prepareStatement(SQL.rollback());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psDelete = helper.prepareStatement(SQL.deleteIllness());
            psDelete.setString(1, doc.getIllness().getIllness().getIllnessno());
            psBegin.executeUpdate();
            psDelete.executeUpdate();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
            try {
                psRollback.executeUpdate();
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

    public IllnessListDocument getDiagnosisPatientStatsReport() {
        ResultSet rsReport = null;
        IllnessListDocument doc = null;
        try {
            helper = new DBHelper();
            PreparedStatement psReport = helper.prepareStatement(SQL.getDiagnosisPatientStatsReport());
            rsReport = psReport.executeQuery();
            doc = IllnessListDocument.Factory.newInstance();
            doc.addNewIllnessList();
            while (rsReport.next()) {
                Illness i = doc.getIllnessList().addNewIllness();
                i.setCount(new BigInteger(String.valueOf(rsReport.getInt("COUNT"))));
                i.setIllness(rsReport.getString("ILLNESS"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsReport != null) {
                    rsReport.close();
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

    public IllnessStatsListDocument getDiagnosisMedicalStatsReport() {
        IllnessStatsListDocument doc = null;
        ResultSet rsReport = null;
        try {
            helper = new DBHelper();
            PreparedStatement psReport = helper.prepareStatement(SQL.getDiagnosisMedicalStatsReport());
            rsReport = psReport.executeQuery();
            doc = IllnessStatsListDocument.Factory.newInstance();
            doc.addNewIllnessStatsList();
            while (rsReport.next()) {
                MedicalIllness m = doc.getIllnessStatsList().addNewStats();
                m.setName(rsReport.getString("NAME"));
                m.setTitle(rsReport.getString("TITLE"));
                m.setSurname(rsReport.getString("SURNAME"));
                m.setCount(new BigInteger(String.valueOf(rsReport.getInt("COUNT"))));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsReport != null) {
                    rsReport.close();
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
}

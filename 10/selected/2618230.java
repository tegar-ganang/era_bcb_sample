package medisis.control;

import com.patient.medisis.PatientDocument;
import com.patient.medisis.SymptomRatingType;
import com.symptom.medisis.Symptom;
import com.symptom.medisis.SymptomListDocument;
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
public class SymptomControllerBean implements SymptomControllerBeanLocal {

    private DBHelper helper = null;

    public SymptomListDocument getSymptomList() {
        SymptomListDocument doc = null;
        ResultSet rs = null;
        try {
            helper = new DBHelper();
            rs = helper.getResultSet(SQL.getSymptomList());
            doc = SymptomListDocument.Factory.newInstance();
            doc.addNewSymptomList();
            while (rs.next()) {
                Symptom s = doc.getSymptomList().addNewSymptom();
                s.setDsm(rs.getString("GROUPDESCRIPTION"));
                s.setSymptom(rs.getString("SYMPTOM"));
                s.setSymptomno(rs.getString("SYMPTOMNO"));
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

    public SymptomListDocument getDSMGroupList() {
        SymptomListDocument doc = null;
        ResultSet rsGroup = null;
        PreparedStatement psGroup = null;
        try {
            helper = new DBHelper();
            doc = SymptomListDocument.Factory.newInstance();
            psGroup = helper.prepareStatement(SQL.getDSMGroupList());
            rsGroup = psGroup.executeQuery();
            doc.addNewSymptomList();
            while (rsGroup.next()) {
                doc.getSymptomList().addNewSymptom().setDsm(rsGroup.getString("GROUPDESCRIPTION"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsGroup != null) {
                    rsGroup.close();
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

    public boolean insertSymptoms(String symptomListDocument) {
        boolean ret = true;
        PreparedStatement psSymptom = null;
        PreparedStatement psStart = null;
        PreparedStatement psCommit = null;
        PreparedStatement psRollback = null;
        SymptomListDocument doc = null;
        try {
            helper = new DBHelper();
            doc = SymptomListDocument.Factory.parse(symptomListDocument);
            psStart = helper.prepareStatement(SQL.begin());
            psCommit = helper.prepareStatement(SQL.commit());
            psRollback = helper.prepareStatement(SQL.rollback());
            psSymptom = helper.prepareStatement(SQL.insertSymptom());
            psStart.executeUpdate();
            for (int i = 0; i < doc.getSymptomList().getSymptomArray().length; i++) {
                Symptom symptom = doc.getSymptomList().getSymptomArray(i);
                symptom.setSymptomno(MedisisKeyGenerator.generate());
                psSymptom.setString(1, symptom.getSymptomno());
                psSymptom.setString(2, symptom.getSymptom());
                psSymptom.setString(3, symptom.getDsm());
                psSymptom.addBatch();
            }
            psSymptom.executeBatch();
            psCommit.executeUpdate();
        } catch (Exception e) {
            ret = false;
            e.printStackTrace();
            try {
                psRollback.executeUpdate();
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

    public boolean deleteSymptom(String symptomNo) {
        boolean ret = true;
        PreparedStatement psSymptom = null;
        PreparedStatement psCommit = null;
        PreparedStatement psBegin = null;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            psBegin = helper.prepareStatement(SQL.begin());
            psSymptom = helper.prepareStatement(SQL.deleteSymptom());
            psCommit = helper.prepareStatement(SQL.commit());
            psSymptom.setString(1, symptomNo);
            psBegin.executeUpdate();
            psSymptom.executeUpdate();
            psCommit.executeUpdate();
        } catch (Exception e) {
            ret = false;
            try {
                psRollback.executeUpdate();
            } catch (Exception ee) {
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

    public SymptomListDocument searchForSymptoms(String sql) {
        SymptomListDocument doc = null;
        ResultSet rsSearch = null;
        try {
            helper = new DBHelper();
            doc = SymptomListDocument.Factory.newInstance();
            PreparedStatement psSearch = helper.prepareStatement(sql);
            rsSearch = psSearch.executeQuery();
            doc.addNewSymptomList();
            while (rsSearch.next()) {
                Symptom s = doc.getSymptomList().addNewSymptom();
                s.setDsm(rsSearch.getString("GROUPDESCRIPTION"));
                s.setSymptom(rsSearch.getString("SYMPTOM"));
                s.setSymptomno(rsSearch.getString("SYMPTOMNO"));
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
            }
        }
        return doc;
    }

    public PatientDocument getSymptomForPatient(String patientDocument) {
        PatientDocument in = null;
        PatientDocument out = null;
        ResultSet rsSymptom = null;
        try {
            helper = new DBHelper();
            in = PatientDocument.Factory.parse(patientDocument);
            long dateFrom = in.getPatient().getAdmitanceRecord().getAdmitanceArray(0).getDateAdmited();
            long dateTo = in.getPatient().getAdmitanceRecord().getAdmitanceArray(0).getDateDischarged();
            String patientNo = in.getPatient().getPatientno();
            PreparedStatement psSymptom = helper.prepareStatement(SQL.getSymptomsForPatient());
            psSymptom.setLong(1, dateFrom);
            psSymptom.setLong(2, (dateTo == 0 ? System.currentTimeMillis() : dateTo));
            psSymptom.setString(3, patientNo);
            rsSymptom = psSymptom.executeQuery();
            out = PatientDocument.Factory.newInstance();
            out.addNewPatient().addNewVectors().addNewVector().addNewSymptomRatingList();
            while (rsSymptom.next()) {
                SymptomRatingType s = out.getPatient().getVectors().getVectorArray(0).getSymptomRatingList().addNewRating();
                s.setSymptom(rsSymptom.getString("SYMPTOM"));
                s.setSymptomNo(rsSymptom.getString("SYMPTOMNO"));
                s.setDsm(rsSymptom.getString("GROUPDESCRIPTION"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsSymptom != null) {
                    rsSymptom.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return out;
    }
}

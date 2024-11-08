package medisis.control;

import com.patient.medisis.AdmitanceType;
import com.patient.medisis.MedicationType;
import com.patient.medisis.NextOfKinType;
import com.patient.medisis.PatientDetailExtensionType;
import com.patient.medisis.PatientDocument;
import com.patient.medisis.PatientInWardType;
import com.patient.medisis.PatientListDetailedDocument;
import com.patient.medisis.PatientListDocument;
import com.patient.medisis.PatientPhysicalAssessmentListDocument;
import com.patient.medisis.Physicalassessment;
import com.patient.medisis.SymptomRatingType;
import com.patient.medisis.VectorType;
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
public class PatientControllerBean implements PatientControllerBeanLocal {

    private DBHelper helper = null;

    public PatientListDocument getPatientList() {
        PatientListDocument doc = PatientListDocument.Factory.newInstance();
        doc.addNewPatientList();
        ResultSet rs = null;
        try {
            helper = new DBHelper();
            rs = helper.getResultSet(SQL.getPatientList());
            while (rs.next()) {
                com.patient.medisis.PatientListDocument.PatientList.Patient pp = doc.getPatientList().addNewPatient();
                pp.setPatientno(rs.getString("PATIENTNO"));
                pp.setName(rs.getString("NAME"));
                pp.setSurname(rs.getString("SURNAME"));
                pp.setTitle(rs.getString("TITLE"));
                pp.setId(rs.getString("ID"));
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    public PatientDocument getPatient(String patientNo) {
        PatientDocument patient = null;
        ResultSet rsPatient = null;
        ResultSet rsNok = null;
        ResultSet rsAdmitance = null;
        ResultSet rsWard = null;
        ResultSet rsVector = null;
        ResultSet rsMeds = null;
        ResultSet rsPA = null;
        try {
            helper = new DBHelper();
            patient = PatientDocument.Factory.newInstance();
            PreparedStatement psPatient = helper.prepareStatement(SQL.getPatient());
            PreparedStatement psAdmitance = helper.prepareStatement(SQL.getAdmitanceRecord());
            PreparedStatement psWard = helper.prepareStatement(SQL.getPatientInWardInformation());
            PreparedStatement psPA = helper.prepareStatement(SQL.getPatientPhysicalAssessmentRecord());
            psPatient.setString(1, patientNo);
            psWard.setString(1, patientNo);
            psAdmitance.setString(1, patientNo);
            psPA.setString(1, patientNo);
            rsPatient = psPatient.executeQuery();
            if (rsPatient.next()) {
                String ptitle = rsPatient.getString("TITLE");
                String pname = rsPatient.getString("NAME");
                String surname = rsPatient.getString("SURNAME");
                String id = rsPatient.getString("ID");
                String illnessno = rsPatient.getString("ILLNESSNO");
                patient.addNewPatient();
                patient.getPatient().setId(id);
                patient.getPatient().setIllnessno(illnessno);
                patient.getPatient().setName(pname);
                patient.getPatient().setSurname(surname);
                patient.getPatient().setStreet(rsPatient.getString("STREET"));
                patient.getPatient().setSuburb(rsPatient.getString("SUBURB"));
                patient.getPatient().setPostcode(rsPatient.getString("POSTCODE"));
                patient.getPatient().setTitle(ptitle);
                PreparedStatement psNok = helper.prepareStatement(SQL.getNextofkin());
                psNok.setString(1, patientNo);
                rsNok = psNok.executeQuery();
                patient.getPatient().addNewNextOfKinList();
                while (rsNok.next()) {
                    NextOfKinType nok = patient.getPatient().getNextOfKinList().addNewNextOfKin();
                    nok.setTitle(rsNok.getString("TITLE"));
                    nok.setName(rsNok.getString("NAME"));
                    nok.setSurname(rsNok.getString("SURNAME"));
                    nok.setTelephone(rsNok.getString("TELEPHONE"));
                    nok.setCell(rsNok.getString("CELLPHONE"));
                }
                rsAdmitance = psAdmitance.executeQuery();
                patient.getPatient().addNewAdmitanceRecord();
                while (rsAdmitance.next()) {
                    AdmitanceType a = patient.getPatient().getAdmitanceRecord().addNewAdmitance();
                    a.setAdmitanceNo(rsAdmitance.getString("ADMITENCENO"));
                    a.setDateAdmited(rsAdmitance.getLong("DATEADMITED"));
                    a.setDateDischarged(rsAdmitance.getLong("DATEDISCHARGED"));
                }
                rsWard = psWard.executeQuery();
                patient.getPatient().addNewWardHistory();
                while (rsWard.next()) {
                    PatientInWardType p = patient.getPatient().getWardHistory().addNewPatientInWard();
                    p.setWardno(rsWard.getString("WARDNO"));
                    p.setWard(rsWard.getString("WARD"));
                    p.setWardDescription(rsWard.getString("WARDDESCRIPTION"));
                    p.setPwno(rsWard.getString("PWNO"));
                    p.setDateAdmitted(rsWard.getLong("DATEADMITED"));
                    p.setDateLeft(rsWard.getLong("DATELEFT"));
                }
                PreparedStatement psVector = helper.prepareStatement(SQL.getVectors());
                PreparedStatement psMeds = helper.prepareStatement(SQL.getPatientMedication());
                psVector.setString(1, patientNo);
                rsVector = psVector.executeQuery();
                int counter = 0;
                String vn = "", tmpvn = "";
                patient.getPatient().addNewVectors();
                VectorType vector = null;
                while (rsVector.next()) {
                    if (counter == 0) {
                        vector = patient.getPatient().getVectors().addNewVector();
                        vector.addNewSymptomRatingList();
                        vector.addNewMedicationList();
                        vn = rsVector.getString("VECTORNO");
                        tmpvn = vn;
                        counter++;
                    }
                    String symptom = rsVector.getString("SYMPTOM");
                    String dsm = rsVector.getString("GROUPDESCRIPTION");
                    String symptomno = rsVector.getString("SYMPTOMNO");
                    Integer rating = new Integer(rsVector.getInt("RATING"));
                    Long dateofrating = new Long(rsVector.getLong("DATEOFRATING"));
                    String notes = rsVector.getString("NOTES");
                    String gaf = rsVector.getString("GAF");
                    String userno = rsVector.getString("USERNO");
                    String title = rsVector.getString("TITLE");
                    String name = rsVector.getString("NAME");
                    String dsurname = rsVector.getString("SURNAME");
                    String telephone = rsVector.getString("TELEPHONE");
                    String cellphone = rsVector.getString("CELLPHONE");
                    vn = rsVector.getString("VECTORNO");
                    if (!vn.equals(tmpvn)) {
                        psMeds.setString(1, tmpvn);
                        rsMeds = psMeds.executeQuery();
                        while (rsMeds.next()) {
                            MedicationType m = vector.getMedicationList().addNewMedication();
                            m.setDose(rsMeds.getString("DOSE"));
                            m.setMedication(rsMeds.getString("MEDICATION"));
                            m.setMedno(rsMeds.getString("MEDNO"));
                        }
                        rsMeds.close();
                        vector = patient.getPatient().getVectors().addNewVector();
                        vector.addNewSymptomRatingList();
                        vector.addNewMedicationList();
                        tmpvn = vn;
                    }
                    vector.setDateOfRating(dateofrating.longValue());
                    vector.setUserTitle(title);
                    vector.setGaf(gaf);
                    vector.setNotes(notes);
                    vector.setUserCell(cellphone);
                    vector.setUserRealName(name);
                    vector.setUserSurname(dsurname);
                    vector.setUserTelephone(telephone);
                    vector.setUserno(userno);
                    vector.setVectorNo(vn);
                    if (symptom == null) {
                        symptom = symptomno;
                    }
                    SymptomRatingType s = vector.getSymptomRatingList().addNewRating();
                    s.setSymptom(symptom);
                    s.setSymptomNo(symptomno);
                    s.setValue(new BigInteger(rating.toString()));
                    s.setDsm(dsm);
                }
                if (vector != null) {
                    psMeds.setString(1, tmpvn);
                    rsMeds = psMeds.executeQuery();
                    if (vector.getMedicationList() == null) {
                        vector.addNewMedicationList();
                    }
                    while (rsMeds.next()) {
                        MedicationType m = vector.getMedicationList().addNewMedication();
                        m.setDose(rsMeds.getString("DOSE"));
                        m.setMedication(rsMeds.getString("MEDICATION"));
                        m.setMedno(rsMeds.getString("MEDNO"));
                    }
                    rsMeds.close();
                }
                rsPA = psPA.executeQuery();
                patient.getPatient().addNewPhysicalAssessments();
                while (rsPA.next()) {
                    Physicalassessment s = patient.getPatient().getPhysicalAssessments().addNewPhysicalAssessment();
                    s.setAssessmentdate(rsPA.getLong("ASSESSMENTDATE"));
                    s.setAssessmentno(rsPA.getString("ASSESSMENTNO"));
                    s.setBp(rsPA.getString("BP"));
                    s.setLmp(rsPA.getLong("LMP"));
                    s.setNails(rsPA.getString("NAILS"));
                    s.setNailsrating(rsPA.getInt("NAILSRATING"));
                    s.setPulse(rsPA.getString("PULSE"));
                    s.setRemarks(rsPA.getString("REMARKS"));
                    s.setResp(rsPA.getString("RESP"));
                    s.setSkin(rsPA.getString("SKIN"));
                    s.setSkinrating(rsPA.getInt("SKINRATING"));
                    s.setTemp(rsPA.getString("TEMP"));
                    s.setUrine(rsPA.getString("URINE"));
                    s.setUrinerating(rsPA.getInt("URINERATING"));
                    s.setCellphone(rsPA.getString("CELLPHONE"));
                    s.setName(rsPA.getString("NAME"));
                    s.setSurname(rsPA.getString("SURNAME"));
                    s.setTelephone(rsPA.getString("TELEPHONE"));
                    s.setTitle(rsPA.getString("TITLE"));
                    s.setWard(rsPA.getString("WARD"));
                    s.setWeight(rsPA.getString("WEIGHT"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsVector != null) {
                    rsVector.close();
                }
                if (rsPA != null) {
                    rsPA.close();
                }
                if (rsMeds != null) {
                    rsMeds.close();
                }
                if (rsWard != null) {
                    rsWard.close();
                }
                if (rsAdmitance != null) {
                    rsAdmitance.close();
                }
                if (rsNok != null) {
                    rsNok.close();
                }
                if (rsPatient != null) {
                    rsPatient.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (SQLException ee) {
                ee.printStackTrace();
            }
        }
        return patient;
    }

    public PatientDocument addPatient(String patient) {
        String key = null;
        PreparedStatement psPatient = null;
        PreparedStatement psNextofkin = null;
        PreparedStatement psCommit = null;
        PreparedStatement psRollback = null;
        PreparedStatement psAdmitance = null;
        PreparedStatement psWard = null;
        PreparedStatement psHospital = null;
        ResultSet rsHospital = null;
        PatientDocument doc = null;
        try {
            doc = PatientDocument.Factory.parse(patient);
            helper = new DBHelper();
            psPatient = helper.prepareStatement(SQL.insertPatient());
            psNextofkin = helper.prepareStatement(SQL.insertNextofkin());
            psCommit = helper.prepareStatement(SQL.commit());
            psRollback = helper.prepareStatement(SQL.rollback());
            psAdmitance = helper.prepareStatement(SQL.insertAdmitance());
            psWard = helper.prepareStatement(SQL.insertPatientWard());
            psHospital = helper.prepareStatement(SQL.getHospitalFromWard());
            key = MedisisKeyGenerator.generate();
            doc.getPatient().setPatientno(key);
            psPatient.setString(1, key);
            psPatient.setString(2, doc.getPatient().getId());
            psPatient.setString(3, doc.getPatient().getIllnessno());
            psPatient.setString(4, doc.getPatient().getTitle());
            psPatient.setString(5, doc.getPatient().getName());
            psPatient.setString(6, doc.getPatient().getSurname());
            psPatient.setString(7, doc.getPatient().getStreet());
            psPatient.setString(8, doc.getPatient().getSuburb());
            psPatient.setString(9, doc.getPatient().getPostcode());
            psPatient.addBatch();
            String wardno = doc.getPatient().getWardHistory().getPatientInWardArray(0).getWardno();
            psHospital.setString(1, wardno);
            rsHospital = psHospital.executeQuery();
            rsHospital.next();
            String hospitalno = rsHospital.getString("HOSPITALNO");
            psAdmitance.setString(1, MedisisKeyGenerator.generate());
            psAdmitance.setString(2, hospitalno);
            psAdmitance.setString(3, doc.getPatient().getPatientno());
            psAdmitance.setLong(4, System.currentTimeMillis());
            psAdmitance.addBatch();
            psWard.setString(1, doc.getPatient().getWardHistory().getPatientInWardArray(0).getPwno());
            psWard.setString(2, doc.getPatient().getPatientno());
            psWard.setString(3, doc.getPatient().getWardHistory().getPatientInWardArray(0).getWardno());
            psWard.setLong(4, System.currentTimeMillis());
            psWard.setLong(5, 0);
            psWard.addBatch();
            NextOfKinType[] nok = doc.getPatient().getNextOfKinList().getNextOfKinArray();
            for (int i = 0; i < nok.length; i++) {
                NextOfKinType n = nok[i];
                n.setNextOfKinNo(MedisisKeyGenerator.generate());
                psNextofkin.setString(1, n.getNextOfKinNo());
                psNextofkin.setString(2, key);
                psNextofkin.setString(3, n.getTitle());
                psNextofkin.setString(4, n.getName());
                psNextofkin.setString(5, n.getSurname());
                psNextofkin.setString(6, n.getTelephone());
                psNextofkin.setString(7, n.getCell());
                psNextofkin.addBatch();
            }
            psPatient.executeBatch();
            psAdmitance.executeBatch();
            psWard.executeBatch();
            psNextofkin.executeBatch();
            psCommit.executeUpdate();
        } catch (Exception e) {
            patient = null;
            try {
                psRollback.executeUpdate();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        } finally {
            try {
                if (rsHospital != null) {
                    rsHospital.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    public PatientDocument updatePatient(String patientDocument) {
        ResultSet rsnok = null;
        PatientDocument doc = null;
        PreparedStatement psRollback = null;
        ResultSet rsWard = null;
        boolean ward = false;
        try {
            helper = new DBHelper();
            doc = PatientDocument.Factory.parse(patientDocument);
            PreparedStatement psPatient = helper.prepareStatement(SQL.updatePatient());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psDeleteNOK = helper.prepareStatement(SQL.deleteNextofkin());
            PreparedStatement psAddNOK = helper.prepareStatement(SQL.insertNextofkin());
            PreparedStatement psWardUpdate = helper.prepareStatement(SQL.updatePatientWardDateLeftInformation());
            PreparedStatement psWardInsert = helper.prepareStatement(SQL.insertPatientWard());
            PreparedStatement psWardInfo = helper.prepareStatement(SQL.getPatientInWardInformation());
            psRollback = helper.prepareStatement(SQL.rollback());
            psPatient.setString(1, doc.getPatient().getName());
            psPatient.setString(2, doc.getPatient().getSurname());
            psPatient.setString(3, doc.getPatient().getStreet());
            psPatient.setString(4, doc.getPatient().getSuburb());
            psPatient.setString(5, doc.getPatient().getPostcode());
            psPatient.setString(6, doc.getPatient().getId());
            psPatient.setString(7, doc.getPatient().getIllnessno());
            psPatient.setString(8, doc.getPatient().getPatientno());
            psWardInfo.setString(1, doc.getPatient().getPatientno());
            rsWard = psWardInfo.executeQuery();
            while (rsWard.next()) {
                if (rsWard.getLong("DATELEFT") == 0) {
                    String wardno = rsWard.getString("WARDNO");
                    PatientInWardType[] piw = doc.getPatient().getWardHistory().getPatientInWardArray();
                    String xmlWardNo = "";
                    String xmlOldPwno = "";
                    String xmlPwno = "";
                    long dateAdmitted = 0;
                    for (int i = 0; i < piw.length; i++) {
                        if (piw[i].getDateLeft() == 0) {
                            PatientInWardType first = piw[i];
                            if (i + 1 < piw.length) {
                                for (int j = i + 1; j < piw.length; j++) {
                                    if (piw[i].getDateLeft() == 0) {
                                        PatientInWardType sec = piw[j];
                                        if (first.getDateAdmitted() < sec.getDateAdmitted()) {
                                            xmlWardNo = sec.getWardno();
                                            xmlOldPwno = first.getPwno();
                                            xmlPwno = sec.getPwno();
                                            dateAdmitted = sec.getDateAdmitted();
                                            ward = true;
                                            break;
                                        } else {
                                            xmlWardNo = first.getWardno();
                                            xmlOldPwno = sec.getPwno();
                                            xmlPwno = first.getPwno();
                                            dateAdmitted = first.getDateAdmitted();
                                            ward = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!wardno.equals(xmlWardNo)) {
                        psWardUpdate.setLong(1, System.currentTimeMillis());
                        psWardUpdate.setString(2, xmlOldPwno);
                        psWardInsert.setString(1, xmlPwno);
                        psWardInsert.setString(2, doc.getPatient().getPatientno());
                        psWardInsert.setString(3, xmlWardNo);
                        psWardInsert.setLong(4, dateAdmitted);
                        psWardInsert.setLong(5, 0);
                    } else {
                        ward = false;
                    }
                }
            }
            psDeleteNOK.setString(1, doc.getPatient().getPatientno());
            for (int i = 0; i < doc.getPatient().getNextOfKinList().getNextOfKinArray().length; i++) {
                NextOfKinType n = doc.getPatient().getNextOfKinList().getNextOfKinArray(i);
                n.setNextOfKinNo(MedisisKeyGenerator.generate());
                psAddNOK.setString(1, n.getNextOfKinNo());
                psAddNOK.setString(2, doc.getPatient().getPatientno());
                psAddNOK.setString(3, n.getTitle());
                psAddNOK.setString(4, n.getName());
                psAddNOK.setString(5, n.getSurname());
                psAddNOK.setString(6, n.getTelephone());
                psAddNOK.setString(7, n.getCell());
                psAddNOK.addBatch();
            }
            psBegin.executeUpdate();
            psPatient.executeUpdate();
            psDeleteNOK.executeUpdate();
            psAddNOK.executeBatch();
            if (ward) {
                psWardUpdate.executeUpdate();
                psWardInsert.executeUpdate();
            }
            psCommit.executeUpdate();
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
                if (rsnok != null) {
                    rsnok.close();
                }
                if (rsWard != null) {
                    rsWard.close();
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

    public boolean dischargePatient(String patientDocument) {
        PreparedStatement psRollback = null;
        boolean ret = false;
        try {
            helper = new DBHelper();
            PatientDocument doc = PatientDocument.Factory.parse(patientDocument);
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psAdmitance = helper.prepareStatement(SQL.updateAdmitanceRecord());
            PreparedStatement psWard = helper.prepareStatement(SQL.updatePatientWardDateLeftInformation());
            psRollback = helper.prepareStatement(SQL.rollback());
            PatientInWardType pp = doc.getPatient().getWardHistory().getPatientInWardArray(0);
            AdmitanceType aa = doc.getPatient().getAdmitanceRecord().getAdmitanceArray(0);
            psWard.setLong(1, System.currentTimeMillis());
            psWard.setString(2, pp.getPwno());
            psAdmitance.setLong(1, System.currentTimeMillis());
            psAdmitance.setString(2, aa.getAdmitanceNo());
            psBegin.executeUpdate();
            psAdmitance.executeUpdate();
            psWard.executeUpdate();
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

    public PatientListDocument searchForPatient(String sql) {
        ResultSet rsResults = null;
        PatientListDocument doc = null;
        try {
            helper = new DBHelper();
            PreparedStatement psResults = helper.prepareStatement(sql);
            rsResults = psResults.executeQuery();
            doc = PatientListDocument.Factory.newInstance();
            doc.addNewPatientList();
            while (rsResults.next()) {
                String patientNo = rsResults.getString("PATIENTNO");
                String title = rsResults.getString("TITLE");
                String name = rsResults.getString("NAME");
                String id = rsResults.getString("ID");
                String surname = rsResults.getString("SURNAME");
                String street = rsResults.getString("STREET");
                String suburb = rsResults.getString("SUBURB");
                String postcode = rsResults.getString("POSTCODE");
                String illnessno = rsResults.getString("ILLNESSNO");
                com.patient.medisis.PatientListDocument.PatientList.Patient p = doc.getPatientList().addNewPatient();
                p.setTitle(title);
                p.setStreet(street);
                p.setSuburb(suburb);
                p.setPostcode(postcode);
                p.setPatientno(patientNo);
                p.setName(name);
                p.setSurname(surname);
                p.setIllnessno(illnessno);
                p.setId(id);
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

    public com.patient.medisis.PatientListDetailedDocument printPatientsInHospitalList(String hospitalno) {
        PatientListDetailedDocument doc = null;
        ResultSet rsPatient = null;
        try {
            helper = new DBHelper();
            doc = PatientListDetailedDocument.Factory.newInstance();
            PreparedStatement psPatient = helper.prepareStatement(SQL.printPatientsInHospital());
            psPatient.setString(1, hospitalno);
            rsPatient = psPatient.executeQuery();
            String tmp = "";
            doc.addNewPatientListDetailed();
            if (rsPatient.next()) {
                while (true) {
                    boolean finished = false;
                    String id = rsPatient.getString(1);
                    if (tmp.equals("")) {
                        tmp = id;
                    }
                    String illnessno = rsPatient.getString(2);
                    String ptitle = rsPatient.getString(3);
                    String pname = rsPatient.getString(4);
                    String psurname = rsPatient.getString(5);
                    String street = rsPatient.getString(6);
                    String suburb = rsPatient.getString(7);
                    String postcode = rsPatient.getString(8);
                    long dateadmited = rsPatient.getLong(9);
                    String ward = rsPatient.getString(15);
                    long wdateadmited = rsPatient.getLong(16);
                    String pwno = rsPatient.getString(17);
                    String wardDescription = rsPatient.getString(18);
                    String wardno = rsPatient.getString(19);
                    PatientDetailExtensionType p = doc.getPatientListDetailed().addNewPatient();
                    p.setName(pname);
                    p.setSurname(psurname);
                    p.setId(id);
                    p.setIllnessno(illnessno);
                    p.setTitle(ptitle);
                    p.setStreet(street);
                    p.setSuburb(suburb);
                    p.setPostcode(postcode);
                    AdmitanceType a = p.addNewAdmitanceRecord().addNewAdmitance();
                    a.setDateAdmited(dateadmited);
                    a.setDateDischarged(0);
                    PatientInWardType w = p.addNewWardHistory().addNewPatientInWard();
                    w.setDateAdmitted(wdateadmited);
                    w.setDateLeft(0);
                    w.setPwno(pwno);
                    w.setWard(ward);
                    w.setWardDescription(wardDescription);
                    w.setWardno(wardno);
                    p.addNewNextOfKinList();
                    while (id.equals(tmp)) {
                        NextOfKinType n = p.getNextOfKinList().addNewNextOfKin();
                        String ntitle = rsPatient.getString(10);
                        String nname = rsPatient.getString(11);
                        String nsurname = rsPatient.getString(12);
                        String telephone = rsPatient.getString(13);
                        String cellphone = rsPatient.getString(14);
                        n.setCell(cellphone);
                        n.setName(nname);
                        n.setSurname(nsurname);
                        n.setTelephone(telephone);
                        n.setTitle(ntitle);
                        if (!rsPatient.next()) {
                            finished = true;
                            break;
                        }
                        id = rsPatient.getString(1);
                    }
                    tmp = id;
                    if (finished) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsPatient != null) {
                    rsPatient.close();
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

    public PatientListDetailedDocument allPatientsOnRecordList(String hospitalno) {
        PatientListDetailedDocument doc = null;
        ResultSet rsPatient = null;
        ResultSet rsAdmitance = null;
        try {
            helper = new DBHelper();
            doc = PatientListDetailedDocument.Factory.newInstance();
            PreparedStatement psPatient = helper.prepareStatement(SQL.printAllPatientsOnRecord());
            PreparedStatement psAdmitance = helper.prepareStatement(SQL.getAdmitanceRecord());
            psPatient.setString(1, hospitalno);
            rsPatient = psPatient.executeQuery();
            doc.addNewPatientListDetailed();
            while (rsPatient.next()) {
                String patientno = rsPatient.getString("PATIENTNO");
                String id = rsPatient.getString("ID");
                String ptitle = rsPatient.getString("TITLE");
                String pname = rsPatient.getString("NAME");
                String psurname = rsPatient.getString("SURNAME");
                String street = rsPatient.getString("STREET");
                String suburb = rsPatient.getString("SUBURB");
                String postcode = rsPatient.getString("POSTCODE");
                PatientDetailExtensionType p = doc.getPatientListDetailed().addNewPatient();
                p.setName(pname);
                p.setSurname(psurname);
                p.setId(id);
                p.setTitle(ptitle);
                p.setStreet(street);
                p.setSuburb(suburb);
                p.setPostcode(postcode);
                psAdmitance.setString(1, patientno);
                rsAdmitance = psAdmitance.executeQuery();
                p.addNewAdmitanceRecord();
                while (rsAdmitance.next()) {
                    AdmitanceType a = p.getAdmitanceRecord().addNewAdmitance();
                    a.setAdmitanceNo(rsAdmitance.getString("ADMITENCENO"));
                    a.setDateAdmited(rsAdmitance.getLong("DATEADMITED"));
                    a.setDateDischarged(rsAdmitance.getLong("DATEDISCHARGED"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsPatient != null) {
                    rsPatient.close();
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

    public boolean insertPhysicalAssessment(String physicalAssessmentListDocument) {
        boolean ret = false;
        try {
            helper = new DBHelper();
            PreparedStatement psInsert = helper.prepareStatement(SQL.insertPhysicalAssessment());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PatientPhysicalAssessmentListDocument doc = PatientPhysicalAssessmentListDocument.Factory.parse(physicalAssessmentListDocument);
            psInsert.setString(1, MedisisKeyGenerator.generate());
            psInsert.setString(2, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getPatientno());
            psInsert.setString(3, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getUserno());
            psInsert.setLong(4, System.currentTimeMillis());
            psInsert.setString(5, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getWardno());
            psInsert.setString(6, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getSkin());
            psInsert.setInt(7, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getSkinrating());
            psInsert.setString(8, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getNails());
            psInsert.setInt(9, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getNailsrating());
            psInsert.setString(10, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getTemp());
            psInsert.setString(11, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getPulse());
            psInsert.setString(12, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getResp());
            psInsert.setString(13, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getBp());
            psInsert.setString(14, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getWeight());
            psInsert.setString(15, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getUrine());
            psInsert.setInt(16, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getUrinerating());
            psInsert.setLong(17, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getLmp());
            psInsert.setString(18, doc.getPatientPhysicalAssessmentList().getAssessmentArray(0).getRemarks());
            psInsert.executeUpdate();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
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
}

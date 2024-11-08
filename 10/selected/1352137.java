package medisis.control;

import com.ward.medisis.Ward;
import com.ward.medisis.WardDocument;
import com.ward.medisis.WardListDocument;
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
public class WardControllerBean {

    private DBHelper helper = null;

    public WardListDocument getWardList(String hospitalNo) {
        ResultSet rsList = null;
        WardListDocument doc = null;
        try {
            helper = new DBHelper();
            doc = WardListDocument.Factory.newInstance();
            doc.addNewWardList();
            PreparedStatement psList = helper.prepareStatement(SQL.getWardList());
            psList.setString(1, hospitalNo);
            rsList = psList.executeQuery();
            while (rsList.next()) {
                Ward i = doc.getWardList().addNewWard();
                i.setWard(rsList.getString("WARD"));
                i.setWardno(rsList.getString("WARDNO"));
                i.setWardDescription(rsList.getString("WARDDESCRIPTION"));
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

    public boolean deleteWard(String wardDocument) {
        boolean ret = false;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            WardDocument doc = WardDocument.Factory.parse(wardDocument);
            psRollback = helper.prepareStatement(SQL.rollback());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psDelete = helper.prepareStatement(SQL.deleteWard());
            psDelete.setString(1, doc.getWard().getWard().getWardno());
            psBegin.executeUpdate();
            psDelete.executeUpdate();
            psCommit.executeUpdate();
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

    public boolean insertWard(String wardDocument) {
        boolean ret = false;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            WardDocument doc = WardDocument.Factory.parse(wardDocument);
            PreparedStatement psAdd = helper.prepareStatement(SQL.insertWard());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            psRollback = helper.prepareStatement(SQL.rollback());
            psAdd.setString(1, doc.getWard().getWard().getHospitalno());
            psAdd.setString(2, MedisisKeyGenerator.generate());
            psAdd.setString(3, doc.getWard().getWard().getWard());
            psAdd.setString(4, doc.getWard().getWard().getWardDescription());
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
}

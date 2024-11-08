package ces.coffice.addrslist.facade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import ces.coffice.addrslist.dao.iplm.AddrslistMainDao;
import ces.coral.dbo.DBOperation;
import ces.coral.dbo.DBOperationFactory;
import ces.coral.dbo.ERDBOperationFactory;
import ces.coral.file.CesGlobals;
import ces.platform.system.common.ConfigReader;
import ces.platform.system.common.Constant;

/**
 * @author sam
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AddrsInterface {

    private static final String INSERT_USER = "INSERT INTO coffice_addrslist_entry(ID, FOLDER_ID, SURNAME, NAME, UNIT, DEPT, JOB, EMAIL, MOBILE, OFFICE_POSITION, COMPANY_ADDRES, COMPANY_TEL, COMPANY_ZIP, COMPANY_FIX, HOME_ADDRES, HOME_TEL, HOME_ZIP, HOMEPAGE, USER_ID, ORG_ID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    static {
        new CesGlobals().setConfigFile(Constant.DB_CONFIGE_FILE);
    }

    protected DBOperationFactory factory = new ERDBOperationFactory();

    protected static String POOL_NAME = ConfigReader.getInstance().getAttribute(Constant.SYSTEM_POOLNAME_PATH);

    public AddrsInterface() {
    }

    public void addUser(String name, String unit, String organizeName, int userId, int orgId, String email) {
        Connection connection = null;
        PreparedStatement ps = null;
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            connection = dbo.getConnection();
            ps = connection.prepareStatement(INSERT_USER);
            ps.setInt(1, AddrslistMainDao.getNewID());
            ps.setInt(2, -100);
            ps.setString(3, name.substring(0, 1));
            ps.setString(4, name.substring(1));
            ps.setString(5, unit);
            ps.setString(6, organizeName);
            ps.setString(7, "");
            ps.setString(8, email);
            ps.setString(9, "");
            ps.setString(10, "");
            ps.setString(11, "");
            ps.setString(12, "");
            ps.setString(13, "");
            ps.setString(14, "");
            ps.setString(15, "");
            ps.setString(16, "");
            ps.setString(17, "");
            ps.setString(18, "");
            ps.setInt(19, userId);
            ps.setInt(20, orgId);
            ps.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } finally {
            try {
                ps.close();
                connection.close();
                dbo.close();
            } catch (Exception e) {
            }
        }
    }

    public void modify(String strName, String strNewPass) {
        String str = "update coffice_addrslist_entry set dept =? where user_id =?";
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
            }
        } finally {
            try {
            } catch (Exception e) {
            }
        }
    }

    public void delUser(String userName) {
        if (userName == null) return;
        String surname = userName.substring(0, 1);
        String name = userName.substring(1);
        Connection connection = null;
        PreparedStatement ps = null;
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        ResultSet rs = null;
        try {
            String srtSql = "select user_id from coffice_addrslist_entry where surname = ? and name = ?";
            connection = dbo.getConnection();
            ps = connection.prepareStatement(srtSql);
            ps.setString(1, surname);
            ps.setString(2, name);
            rs = ps.executeQuery();
            if (!rs.next()) {
            }
            int userId = rs.getInt("user_id");
            String strDelUser = "delete from coffice_addrslist_entry where user_id = ?";
            ps = connection.prepareStatement(strDelUser);
            ps.setInt(1, userId);
            ps.execute();
            connection.commit();
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        } finally {
            try {
                ps.close();
                connection.close();
                dbo.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void delUser(int userId) {
        Connection connection = null;
        PreparedStatement ps = null;
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        ResultSet rs = null;
        try {
            String strDelUser = "delete from coffice_addrslist_entry where user_id = ?";
            connection = dbo.getConnection();
            ps = connection.prepareStatement(strDelUser);
            ps.setInt(1, userId);
            ps.execute();
            connection.commit();
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                ps.close();
                connection.close();
                dbo.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        AddrsInterface Addrs = new AddrsInterface();
        try {
            Addrs.modify("��Сͦ", "670B14728AD9902AECBA32E22FA4F6BD");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

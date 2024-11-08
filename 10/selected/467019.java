package medisis.control;

import com.user.medisis.GetUserDocument;
import com.user.medisis.User;
import com.user.medisis.UserAttr;
import com.user.medisis.UserDocument;
import com.user.medisis.UserListDocument;
import com.user.medisis.UserWorkloadReport;
import com.user.medisis.UserWorkloadReportDocument;
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
public class UserControllerBean implements UserControllerBeanLocal {

    private DBHelper helper = null;

    public UserDocument login(String userDocument) {
        UserDocument in = null;
        UserDocument out = null;
        ResultSet rsLogin = null;
        try {
            helper = new DBHelper();
            in = UserDocument.Factory.parse(userDocument);
            out = UserDocument.Factory.newInstance();
            String username = in.getUser().getUsername();
            String password = in.getUser().getPassword();
            PreparedStatement psLogin = helper.prepareStatement(SQL.getLoginDetail());
            psLogin.setString(1, username);
            rsLogin = psLogin.executeQuery();
            if (rsLogin.next()) {
                String pswd = rsLogin.getString("PASSWORD");
                String role = rsLogin.getString("ROLE");
                String userno = rsLogin.getString("USERNO");
                String title = rsLogin.getString("TITLE");
                String name = rsLogin.getString("NAME");
                String surname = rsLogin.getString("SURNAME");
                String telephone = rsLogin.getString("TELEPHONE");
                String cellphone = rsLogin.getString("CELLPHONE");
                out.addNewUser();
                if (pswd.equals(password)) {
                    out.getUser().setCellphone(cellphone);
                    out.getUser().setLoggedIn(true);
                    out.getUser().setName(name);
                    out.getUser().setRole(role);
                    out.getUser().setSurname(surname);
                    out.getUser().setTelephone(telephone);
                    out.getUser().setTitle(title);
                    out.getUser().setUserno(userno);
                } else {
                    out.getUser().setLoggedIn(false);
                }
            } else {
                out.addNewUser();
                out.getUser().setLoggedIn(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsLogin != null) {
                    rsLogin.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (SQLException e) {
            }
        }
        return out;
    }

    public UserDocument getUser(String userParams) {
        ResultSet rsLogin = null;
        GetUserDocument in = null;
        UserDocument out = null;
        try {
            helper = new DBHelper();
            in = GetUserDocument.Factory.parse(userParams);
            String sql = null;
            if (in.getGetUser().getUserParams().getUsingUsername()) {
                sql = SQL.getLoginDetail();
            } else {
                sql = SQL.getUserDetail();
            }
            out = UserDocument.Factory.newInstance();
            out.addNewUser();
            PreparedStatement psLogin = helper.prepareStatement(sql);
            psLogin.setString(1, in.getGetUser().getUserParams().getParam());
            rsLogin = psLogin.executeQuery();
            if (rsLogin.next()) {
                String pswd = rsLogin.getString("PASSWORD");
                String role = rsLogin.getString("ROLE");
                String userno = rsLogin.getString("USERNO");
                String title = rsLogin.getString("TITLE");
                String name = rsLogin.getString("NAME");
                String surname = rsLogin.getString("SURNAME");
                String telephone = rsLogin.getString("TELEPHONE");
                String cellphone = rsLogin.getString("CELLPHONE");
                out.getUser().setCellphone(cellphone);
                out.getUser().setLoggedIn(true);
                out.getUser().setRole(role);
                out.getUser().setUserno(userno);
                out.getUser().setTitle(title);
                out.getUser().setName(name);
                out.getUser().setSurname(surname);
                out.getUser().setTelephone(telephone);
                out.getUser().setUsername(in.getGetUser().getUserParams().getParam());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsLogin != null) {
                    rsLogin.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (SQLException e) {
            }
        }
        return out;
    }

    public boolean updateUser(String userDocument) {
        boolean ret = false;
        DBHelper helper = null;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            UserDocument doc = UserDocument.Factory.parse(userDocument);
            psRollback = helper.prepareStatement(SQL.rollback());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psUpdate = helper.prepareStatement(SQL.updateUser());
            psUpdate.setString(1, doc.getUser().getRoleno());
            psUpdate.setString(2, doc.getUser().getPassword());
            psUpdate.setString(3, doc.getUser().getTitle());
            psUpdate.setString(4, doc.getUser().getName());
            psUpdate.setString(5, doc.getUser().getSurname());
            psUpdate.setString(6, doc.getUser().getTelephone());
            psUpdate.setString(7, doc.getUser().getCellphone());
            psUpdate.setString(8, doc.getUser().getHospitalno());
            psUpdate.setString(9, doc.getUser().getUserno());
            psBegin.executeUpdate();
            psUpdate.executeUpdate();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
            ret = false;
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

    public boolean changePassword(String userDocument) {
        boolean ret = false;
        DBHelper helper = null;
        PreparedStatement psRollback = null;
        try {
            UserDocument doc = UserDocument.Factory.parse(userDocument);
            helper = new DBHelper();
            PreparedStatement psPassword = helper.prepareStatement(SQL.getUpdatePassword());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            psRollback = helper.prepareStatement(SQL.rollback());
            psPassword.setString(1, doc.getUser().getPassword());
            psPassword.setString(2, doc.getUser().getUserno());
            psPassword.executeUpdate();
            psCommit.executeUpdate();
            ret = true;
        } catch (Exception e) {
            try {
                if (psRollback != null) {
                    psRollback.executeUpdate();
                }
            } catch (SQLException ee) {
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

    public boolean addUser(String userDocument) {
        boolean ret = true;
        DBHelper helper = null;
        PreparedStatement psRollback = null;
        try {
            helper = new DBHelper();
            UserDocument doc = UserDocument.Factory.parse(userDocument);
            psRollback = helper.prepareStatement(SQL.rollback());
            PreparedStatement psBegin = helper.prepareStatement(SQL.begin());
            PreparedStatement psCommit = helper.prepareStatement(SQL.commit());
            PreparedStatement psAdd = helper.prepareStatement(SQL.insertUser());
            psAdd.setString(1, doc.getUser().getHospitalno());
            psAdd.setString(2, MedisisKeyGenerator.generate());
            psAdd.setString(3, doc.getUser().getRoleno());
            psAdd.setString(4, doc.getUser().getUsername());
            psAdd.setString(5, doc.getUser().getPassword());
            psAdd.setString(6, doc.getUser().getTitle());
            psAdd.setString(7, doc.getUser().getName());
            psAdd.setString(8, doc.getUser().getSurname());
            psAdd.setString(9, doc.getUser().getTelephone());
            psAdd.setString(10, doc.getUser().getCellphone());
            psBegin.executeUpdate();
            psAdd.executeUpdate();
            psCommit.executeUpdate();
        } catch (Exception e) {
            ret = false;
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

    public boolean checkUserExistance(String userDocument) {
        DBHelper helper = null;
        boolean ret = false;
        ResultSet rsCheck = null;
        UserDocument doc = null;
        try {
            helper = new DBHelper();
            doc = UserDocument.Factory.parse(userDocument);
            PreparedStatement psCheck = helper.prepareStatement(SQL.checkUserExistance());
            psCheck.setString(1, doc.getUser().getUsername());
            rsCheck = psCheck.executeQuery();
            rsCheck.next();
            if (rsCheck.getInt(1) != 0) {
                ret = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rsCheck != null) {
                    rsCheck.close();
                }
                if (helper != null) {
                    helper.cleanup();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public UserListDocument getUserList(String hospitalno) {
        DBHelper helper = null;
        ResultSet rsList = null;
        UserListDocument doc = null;
        try {
            helper = new DBHelper();
            doc = UserListDocument.Factory.newInstance();
            PreparedStatement psList = helper.prepareStatement(SQL.getUserList());
            psList.setString(1, hospitalno);
            rsList = psList.executeQuery();
            doc.addNewUserList();
            while (rsList.next()) {
                UserAttr ua = doc.getUserList().addNewUser();
                ua.setUsername(rsList.getString("USERNAME"));
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

    public UserListDocument searchForUser(String sql) {
        DBHelper helper = null;
        ResultSet rsSearch = null;
        UserListDocument doc = null;
        try {
            helper = new DBHelper();
            doc = UserListDocument.Factory.newInstance();
            doc.addNewUserList();
            PreparedStatement psSearch = helper.prepareStatement(sql);
            rsSearch = psSearch.executeQuery();
            while (rsSearch.next()) {
                String role = rsSearch.getString("ROLE");
                String userno = rsSearch.getString("USERNO");
                String username = rsSearch.getString("USERNAME");
                String title = rsSearch.getString("TITLE");
                String name = rsSearch.getString("NAME");
                String surname = rsSearch.getString("SURNAME");
                String telephone = rsSearch.getString("TELEPHONE");
                String cellphone = rsSearch.getString("CELLPHONE");
                String hospitalno = rsSearch.getString("HOSPITALNO");
                User user = doc.getUserList().addNewUser();
                user.setCellphone(cellphone);
                user.setName(name);
                user.setRole(role);
                user.setSurname(surname);
                user.setTelephone(telephone);
                user.setTitle(title);
                user.setUserno(userno);
                user.setUsername(username);
                user.setHospitalno(hospitalno);
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    public UserWorkloadReportDocument getUserWorkloadReport(String userWorkloadReportDocument) {
        DBHelper helper = null;
        ResultSet rsReport = null;
        UserWorkloadReportDocument in = null, out = null;
        try {
            helper = new DBHelper();
            in = UserWorkloadReportDocument.Factory.parse(userWorkloadReportDocument);
            PreparedStatement psReport = helper.prepareStatement(SQL.getUserWorkloadReport());
            psReport.setLong(1, in.getUserWorkloadReport().getUserWorkloadReportArray(0).getDateFrom());
            psReport.setLong(2, in.getUserWorkloadReport().getUserWorkloadReportArray(0).getDateTo());
            psReport.setString(3, in.getUserWorkloadReport().getUserWorkloadReportArray(0).getHospitalno());
            psReport.setLong(4, in.getUserWorkloadReport().getUserWorkloadReportArray(0).getDateFrom());
            psReport.setLong(5, in.getUserWorkloadReport().getUserWorkloadReportArray(0).getDateTo());
            psReport.setString(6, in.getUserWorkloadReport().getUserWorkloadReportArray(0).getHospitalno());
            rsReport = psReport.executeQuery();
            out = UserWorkloadReportDocument.Factory.newInstance();
            out.addNewUserWorkloadReport();
            while (rsReport.next()) {
                UserWorkloadReport uu = out.getUserWorkloadReport().addNewUserWorkloadReport();
                uu.setType(rsReport.getString("TYPE"));
                uu.setTitle(rsReport.getString("TITLE"));
                uu.setName(rsReport.getString("NAME"));
                uu.setSurname(rsReport.getString("SURNAME"));
                uu.setCount(new BigInteger(String.valueOf(rsReport.getInt("COUNT"))));
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
        return out;
    }
}

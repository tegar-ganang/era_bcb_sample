package ces.coffice.joblog.dao.imp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import ces.coffice.common.base.BaseDao;
import ces.coffice.common.base.BaseVo;
import ces.coffice.common.base.DbBase;
import ces.coral.dbo.DBOperation;
import ces.coral.log.Logger;

/**
*
* <p>Title: ������Ϣ(OA)3.0</p>
* <p>Description: </p>
* <p>Copyright: Copyright (c) 2004 </p>
* <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
* @author ��ͱ�
* @version 3.0
*/
public class AuthLstOfRoleDAO extends DbBase implements BaseDao {

    static Logger log = new Logger(AuthLstOfRoleDAO.class);

    public static final int AUTH_FLAG = 0;

    public static final int CHECK_FLAG = 1;

    public void insertJobLog(String userId, String[] checkId, String checkType, String objType) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preStm = null;
        String sql = "insert into COFFICE_JOBLOG_CHECKAUTH (USER_ID,CHECK_ID,CHECK_TYPE,OBJ_TYPE) values (?,?,?,?)";
        String cleanSql = "delete from COFFICE_JOBLOG_CHECKAUTH where " + "user_id = '" + userId + "' and check_type = '" + checkType + "' and obj_type = '" + objType + "'";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            preStm = connection.prepareStatement(cleanSql);
            int dCount = preStm.executeUpdate();
            String sHaveIns = ",";
            preStm = connection.prepareStatement(sql);
            for (int j = 0; j < checkId.length; j++) {
                if (sHaveIns.indexOf("," + checkId[j] + ",") < 0) {
                    preStm.setInt(1, Integer.parseInt(userId));
                    preStm.setInt(2, Integer.parseInt(checkId[j]));
                    preStm.setInt(3, Integer.parseInt(checkType));
                    preStm.setInt(4, Integer.parseInt(objType));
                    preStm.executeUpdate();
                    sHaveIns += checkId[j] + ",";
                }
            }
            connection.commit();
        } catch (Exception ex) {
            log.debug((new Date().toString()) + " ������Ȩ��ʧ��! ");
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw e;
            }
            throw ex;
        } finally {
            close(null, null, preStm, connection, dbo);
        }
    }

    public String selJobLog(String userObj, String checkType, String objType) throws Exception {
        StringBuffer html = new StringBuffer("");
        String strSql = "select OA.check_id userObj,T.user_name from COFFICE_JOBLOG_CHECKAUTH OA,T_SYS_USER T where" + " OA.user_id = ? and OA.check_type = ? and OA.obj_type = ? and oa.check_id = T.user_id";
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preStm = null;
        ResultSet ret = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preStm = connection.prepareStatement(strSql);
            preStm.setInt(1, Integer.parseInt(userObj));
            preStm.setInt(2, Integer.parseInt(checkType));
            preStm.setInt(3, Integer.parseInt(objType));
            ret = preStm.executeQuery();
            while (ret.next()) {
                html.append("<option value=" + ret.getString(1) + ">").append(ret.getString(2)).append("</option>");
            }
        } catch (Exception ex) {
            log.debug((new Date().toString()) + " ������Ȩ��ʧ��! ");
            throw ex;
        } finally {
            close(ret, null, preStm, connection, dbo);
        }
        return html.toString();
    }

    public Vector selJobLogCheck(String userObj, String checkType, String objType) throws Exception {
        String strSql = "select OA.check_id userObj,T.user_name from COFFICE_JOBLOG_CHECKAUTH OA,T_SYS_USER T where" + " OA.user_id = ? and OA.check_type = ? and OA.obj_type = ? and oa.check_id = T.user_id";
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preStm = null;
        ResultSet ret = null;
        Vector ss = new Vector();
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preStm = connection.prepareStatement(strSql);
            preStm.setInt(1, Integer.parseInt(userObj));
            preStm.setInt(2, Integer.parseInt(checkType));
            preStm.setInt(3, Integer.parseInt(objType));
            ret = preStm.executeQuery();
            while (ret.next()) {
                String[] aa = new String[2];
                aa[0] = String.valueOf(ret.getInt(1));
                aa[1] = ret.getString(2);
                ss.add(aa);
            }
        } catch (Exception ex) {
            log.debug((new Date().toString()) + " ������Ȩ��ʧ��! ");
            throw ex;
        } finally {
            close(ret, null, preStm, connection, dbo);
        }
        return ss;
    }

    public void doAddBatch(Collection entitys) throws Exception {
    }

    public void doDelBatch(Collection entitys) throws Exception {
    }

    public boolean doDelete(BaseVo vo) throws Exception {
        return false;
    }

    public void doNew(BaseVo vo) throws Exception {
    }

    public boolean doUpdate(BaseVo bo) throws Exception {
        return false;
    }

    public void doUpdateBatch(Collection entitys) throws Exception {
    }

    public BaseVo getEntity(String condition) throws Exception {
        return null;
    }
}

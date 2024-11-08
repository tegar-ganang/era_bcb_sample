package ces.coffice.addrslist.dao.iplm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Vector;
import ces.coffice.addrslist.AddrException;
import ces.coffice.addrslist.vo.AddrslistAuth;
import ces.coffice.common.base.BaseDao;
import ces.coffice.common.base.BaseVo;
import ces.coffice.common.base.DbBase;
import ces.coral.dbo.DBOperation;
import ces.coral.log.Logger;
import ces.platform.system.common.ValueAsc;

public class AddrslistAuthDao extends DbBase implements BaseDao {

    static Logger logger = new Logger(AddrslistAuthDao.class);

    public static final String SHARE_TABLE = "coffice_addrslist_share";

    private int folderId;

    private int userId;

    private String auth;

    private String userName;

    public AddrslistAuthDao(int userId) {
        this.userId = userId;
    }

    public AddrslistAuthDao() {
        this(0);
    }

    public int getFolderId() {
        return this.folderId;
    }

    public int getUserId() {
        return this.userId;
    }

    public String getAuth() {
        return this.auth;
    }

    /**
	 * @return
	 */
    public String getUserName() {
        return this.userName;
    }

    /**
	 * @param string
	 */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public void doAddBatch(Collection entitys) throws AddrException {
    }

    public void doDelBatch() throws AddrException {
    }

    public void doUpdateBatch() throws AddrException {
    }

    public void doNew() throws AddrException {
    }

    public void doNew(BaseVo vo) throws AddrException {
    }

    public boolean doUpdate(BaseVo vo) throws AddrException {
        return false;
    }

    public Vector getEntities(String condition) throws AddrException {
        return null;
    }

    /**
     * ɾ�����ļ���
     * @param boolean
     * @throws AddrException
     */
    public boolean doDelete(BaseVo vo) throws AddrException {
        boolean success = false;
        AddrslistAuth addrsAuth = (AddrslistAuth) vo;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        String sql = "delete from " + SHARE_TABLE + " where user_id=?";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            ps = connection.prepareStatement(sql);
            ps.setInt(1, addrsAuth.getUserId());
            int result = ps.executeUpdate();
            success = true;
        } catch (Exception ex) {
            logger.error("ɾ���ļ��й���ʧ��,�ļ���ID " + addrsAuth.getFolderId() + " " + ex.getMessage());
            throw new AddrException("ɾ���ļ��й���ʧ��!");
        } finally {
            close(resultSet, null, ps, connection, dbo);
        }
        return success;
    }

    /**
	 * �õ������ļ����б���Ϣ
	 * @param vo
	 * @throws AddrException
	 */
    public BaseVo getEntity(String condition) throws AddrException {
        AddrslistAuth addrsAuth = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String sql = "select a.user_id,a.auth,a.folder_id,b.user_name from " + SHARE_TABLE + " a, t_sys_user b where a.user_id = b.user_id and a.user_id=" + condition + "";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            ps = connection.prepareStatement(sql);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                addrsAuth = AddrslistAuthDao.generateAuth(result, va);
            }
        } catch (Exception ex) {
            logger.error("�õ������ļ����б���Ϣʧ��,�ļ���ID " + addrsAuth.getFolderId() + " " + ex.getMessage());
            throw new AddrException("�õ������ļ����б���Ϣʧ��!");
        } finally {
            close(result, null, ps, connection, dbo);
        }
        return addrsAuth;
    }

    /**
	 * ���������ļ�����Ϣ
	 * @param vo
	 * @throws AddrException
	 */
    public void doNew(Vector userId, String shareFlag, String folderId) throws AddrException {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rset = null;
        String sql = "insert into " + SHARE_TABLE + " values(?,?,?)";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            for (int i = 0; i < userId.size(); i++) {
                String user = (String) userId.elementAt(i);
                ps = connection.prepareStatement(sql);
                ps.setInt(1, Integer.parseInt(folderId));
                ps.setInt(2, Integer.parseInt(user));
                ps.setString(3, shareFlag);
                int resultCount = ps.executeUpdate();
                if (resultCount != 1) {
                    throw new Exception("error");
                }
            }
            connection.commit();
        } catch (Exception ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            logger.error("���������ļ�����Ϣʧ��, " + ex.getMessage());
            throw new AddrException("���������ļ�����Ϣʧ��,һ�������ļ���ֻ�ܹ����ͬһ�û�һ��!");
        } finally {
            close(rset, null, ps, connection, dbo);
        }
    }

    /**
	 * �õ������ļ�����Ϣ
	 * @param Vector
	 * @throws AddrException
	 */
    public Vector getAddrsAuth(String folderId) throws AddrException {
        Vector vec = new Vector();
        AddrslistAuth addrsAuth = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String sql = "select a.user_id,a.auth,a.folder_id,b.user_name from " + SHARE_TABLE + " a, t_sys_user b where a.user_id = b.user_id and a.folder_id = " + folderId + "";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            ps = connection.prepareStatement(sql);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                addrsAuth = AddrslistAuthDao.generateAuth(result, va);
                vec.addElement(addrsAuth);
            }
        } catch (Exception ex) {
            logger.error("�õ������ļ�����Ϣʧ��, " + ex.getMessage());
            throw new AddrException("�õ������ļ�����Ϣʧ��!");
        } finally {
            close(result, null, ps, connection, dbo);
        }
        return vec;
    }

    public static AddrslistAuth generateAuth(ResultSet result, ValueAsc v) {
        AddrslistAuth addrsAuth = null;
        try {
            addrsAuth = new AddrslistAuth(result.getInt(v.next()));
            addrsAuth.setAuth(result.getString(v.next()));
            addrsAuth.setFolderId(result.getInt(v.next()));
            addrsAuth.setUserName(result.getString(v.next()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return addrsAuth;
    }

    public void doDelBatch(Collection entitys) throws Exception {
    }

    public void doUpdateBatch(Collection entitys) throws Exception {
    }
}

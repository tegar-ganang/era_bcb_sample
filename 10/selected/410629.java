package jp.web.sync.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import jp.web.sync.relax.response.Data;
import jp.web.sync.relax.response.LocationInfo;
import jp.web.sync.relax.response.ResponseXML;
import jp.web.sync.util.Constant;

/**
 * @author sync
 *
 */
public class LocationInfoDao extends BaseDao {

    protected static Logger log = Logger.getLogger(LocationInfoDao.class);

    /**
	 *
	 * @param id
	 * @param lattitude
	 * @param longitude
	 * @return
	 */
    public int addLocationInfo(int id, double lattitude, double longitude) {
        int ret = 0;
        Connection conn = null;
        PreparedStatement psmt = null;
        try {
            String sql = "insert into kddb.location_info (user_id, latitude, longitude) values(?, ?, ?)";
            conn = getConnection();
            psmt = conn.prepareStatement(sql);
            psmt.setInt(1, id);
            psmt.setDouble(2, lattitude);
            psmt.setDouble(3, longitude);
            ret = psmt.executeUpdate();
            if (ret == 1) {
                conn.commit();
            } else {
                conn.rollback();
            }
        } catch (SQLException ex) {
            log.error("[addLocationInfo]", ex);
        } finally {
            endProsess(conn, psmt, null, null);
        }
        return ret;
    }

    /**
	 *
	 * @param groupId
	 * @param userId
	 * @return
	 */
    public ResponseXML getLocationInfo(int userId, int groupId, double lattitude, double longitude) {
        Connection conn = null;
        CallableStatement csmt = null;
        ResultSet rst = null;
        ResponseXML resXML = new ResponseXML();
        Data data = new Data();
        List<LocationInfo> list = new ArrayList<LocationInfo>();
        try {
            String sql = "call location_get(?, ?, ?, ?);";
            conn = getConnection();
            csmt = conn.prepareCall(sql);
            csmt.setInt(1, userId);
            csmt.setInt(2, groupId);
            csmt.setDouble(3, lattitude);
            csmt.setDouble(4, longitude);
            rst = csmt.executeQuery();
            while (rst.next()) {
                resXML.setCode(rst.getString("code"));
                if (rst.getString("code").equals(Constant.CODE_LOCATION_LIST_SUCCESS)) {
                    LocationInfo locationInfo = new LocationInfo();
                    locationInfo.setCreateDateAndTime(rst.getTimestamp("createdateandtime").getTime());
                    locationInfo.setUserName(rst.getString("user_name"));
                    locationInfo.setLattitude(rst.getDouble("latitude"));
                    locationInfo.setLongitude(rst.getDouble("longitude"));
                    list.add(locationInfo);
                }
            }
            data.setLocationInfo(list.toArray(new LocationInfo[0]));
            resXML.setData(data);
        } catch (SQLException ex) {
            log.error("[getLocationInfo]", ex);
        } finally {
            endProsess(conn, null, csmt, rst);
        }
        return resXML;
    }
}

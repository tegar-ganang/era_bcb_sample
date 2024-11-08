package bill.model.dao;

import bill.model.vo.Order;
import bill.model.vo.OrderDetail;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.AppException;
import util.DBUtils;
import util.DateUtil;

/**
 *
 * @author Administrator
 */
public class OrderDAO {

    public static long getMaxID(Connection con, PreparedStatement pstmt, ResultSet rs) throws SQLException {
        long num = 0;
        String sql = "select max(ID) from SO_SOMain";
        pstmt = con.prepareStatement(sql);
        rs = pstmt.executeQuery();
        if (rs.next()) {
            num = rs.getLong(1);
        }
        return num;
    }

    public static long getiSOsID(Connection con, PreparedStatement pstmt, ResultSet rs) throws SQLException {
        long num = 0;
        String sql = "select max(iSOsID) from SO_SODetails";
        pstmt = con.prepareStatement(sql);
        rs = pstmt.executeQuery();
        if (rs.next()) {
            num = rs.getLong(1);
        }
        return num;
    }

    public static long getMaxCSOCode(String pre8Code, Connection con, PreparedStatement pstmt, ResultSet rs) throws SQLException {
        long code = 0L;
        String sql = "select top 1 cSOCode from SO_SOMain where cSOCode like ? " + "order by cSOCode desc";
        pstmt = con.prepareStatement(sql);
        pstmt.setString(1, "%" + pre8Code + "%");
        rs = pstmt.executeQuery();
        if (rs.next()) {
            String codeStr = rs.getString(1);
            codeStr = codeStr.substring(9);
            code = Long.parseLong(codeStr);
        }
        return code;
    }

    public static String getCSOCode(String code, long cSOCode) {
        StringBuffer codeStrBuf = new StringBuffer();
        codeStrBuf.append(code).append(9);
        String csoCode = String.valueOf(cSOCode + 1);
        int zeroLen = 7 - csoCode.length();
        for (int i = 1; i <= zeroLen; i++) {
            codeStrBuf.append("0");
        }
        codeStrBuf.append(csoCode);
        return codeStrBuf.toString();
    }

    public static void batchInsertOrder(Order o) throws AppException {
        Connection conn = DBUtils.getDataConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sqlOrder = null;
        String sqlDetail = null;
        try {
            conn.setAutoCommit(false);
            sqlOrder = "insert into SO_SOMain (ID,cSOCode,dDate," + "cBusType,cCusCode,cCusName," + "cDepCode,cSTCode," + "iVTid,cMaker,cMemo) values (?,?,?,?,?,?,?,?,?,?,?)";
            sqlDetail = "insert into SO_SODetails (ID,cSOCode,cInvCode,cInvName," + "iNum,iQuantity,iTaxUnitPrice,iSum,dPreDate,iSOsID,cFree1,cUnitID) values(?,?,?,?,?,?,?,?,?,?,?,?)";
            pstmt = conn.prepareStatement(sqlOrder);
            long maxId = getMaxID(conn, pstmt, rs);
            maxId++;
            pstmt.setLong(1, maxId);
            Long l = maxId;
            o.setId(l.intValue());
            String code = o.getCode();
            long maxCSOCode = getMaxCSOCode(code, conn, pstmt, rs);
            String csOCode = getCSOCode(code, maxCSOCode);
            o.setCode(csOCode);
            pstmt.setString(2, csOCode);
            pstmt.setDate(3, DateUtil.getSqlDateFormUtilDate(o.getOrderDate()));
            pstmt.setString(4, o.getBusinessType());
            pstmt.setString(5, o.getC().getId());
            pstmt.setString(6, o.getC().getName());
            pstmt.setString(7, o.getP().getDept().getId());
            pstmt.setString(8, o.getSaleType().getId());
            pstmt.setInt(9, o.getiVtid());
            pstmt.setString(10, o.getP().getName());
            pstmt.setString(11, o.getRemark());
            pstmt.executeUpdate();
            pstmt.clearParameters();
            pstmt = conn.prepareStatement(sqlDetail);
            ArrayList<OrderDetail> ods = o.getOds();
            long iSOsID = getiSOsID(conn, pstmt, rs);
            for (OrderDetail od : ods) {
                pstmt.setLong(1, maxId);
                pstmt.setString(2, csOCode);
                pstmt.setString(3, od.getInventory().getId());
                pstmt.setString(4, od.getInventory().getName());
                pstmt.setInt(5, od.getPiece());
                pstmt.setBigDecimal(6, od.getCount());
                pstmt.setBigDecimal(7, od.getPrice());
                pstmt.setBigDecimal(8, od.getSum());
                pstmt.setDate(9, DateUtil.getSqlDateFormUtilDate(od.getSendDate()));
                pstmt.setLong(10, ++iSOsID);
                pstmt.setString(11, od.getPacking().getcValue());
                pstmt.setString(12, od.getInventory().getSAComUnitCode());
                pstmt.executeUpdate();
                pstmt.clearParameters();
            }
            conn.commit();
        } catch (SQLException sqle) {
            try {
                conn.rollback();
                throw new AppException(sqle.getMessage());
            } catch (SQLException ex) {
                Logger.getLogger(OrderDAO.class.getName()).log(Level.SEVERE, null, ex);
                throw new AppException(ex.getMessage());
            }
        } finally {
            DBUtils.closeAll(rs, pstmt, conn);
        }
    }
}

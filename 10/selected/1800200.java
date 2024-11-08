package com.util;

import com.service.Service;
import com.util.KeyInfo.KeyGenerator;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import ototype.*;
import javax.sql.DataSource;
import java.io.*;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PublicUtil {

    static XmlBeanFactory factory;

    static KeyGenerator keygen;

    static ItemUitl itul;

    static {
        factory = new XmlBeanFactory(new ClassPathResource("applicationContext.xml"));
        keygen = (KeyGenerator) factory.getBean("KeyGen");
        itul = (ItemUitl) factory.getBean("itul");
    }

    public DataSource getDateSource() {
        return getJdbcTemplate().getDataSource();
    }

    public void initContext() {
    }

    public JdbcTemplate getJdbcTemplate() {
        initContext();
        return itul.getJt();
    }

    private Set getBlackMobileSet() {
        JdbcTemplate jt = this.getJdbcTemplate();
        Iterator it = jt.queryForList("select distinct mobile as Mobile from t_black_mobile").iterator();
        Set set = new HashSet();
        while (it.hasNext()) {
            Map mp = (Map) it.next();
            String mobile = (String) mp.get("Mobile");
            if (mobile != null) {
                set.add(mobile);
            }
        }
        return set;
    }

    public List ValidMobileNumber(String[] ml) {
        ArrayList amls = new ArrayList();
        if (ml == null || ml.length == 0) {
            return amls;
        }
        Set blackMobileSet = getBlackMobileSet();
        for (int i = 0; i < ml.length; i++) {
            String mobile = ml[i];
            System.out.println("mobile = " + mobile);
            if (mobile != null && (blackMobileSet.size() == 0 || !blackMobileSet.contains(mobile))) {
                amls.add(mobile);
            }
        }
        System.out.println("amls.size() = " + amls.size());
        return amls;
    }

    private Set getSeriousWordSet() {
        JdbcTemplate jt = this.getJdbcTemplate();
        List list = jt.queryForList("select distinct word as Word from t_serious_word ");
        Iterator iter = list.iterator();
        Set set = new HashSet();
        while (iter.hasNext()) {
            Map mp = (Map) iter.next();
            String word = (String) mp.get("Word");
            if (word != null && !"".equals(word)) {
                set.add(word);
            }
        }
        return set;
    }

    public String[] arrayRpcStrings(ArrayList aryl) {
        String[] strs = new String[aryl.size()];
        for (int i = 0; i < aryl.size(); i++) {
            strs[i] = aryl.get(i).toString();
        }
        return strs;
    }

    public int getSegquence(String tname) {
        return (int) keygen.getNextKey(tname);
    }

    public String validSeriouseWord(String content) {
        String seriouseWord = null;
        if (content == null || content.length() == 0) {
            return seriouseWord;
        }
        Set ls = getSeriousWordSet();
        Iterator it = ls.iterator();
        while (it.hasNext()) {
            String wds = (String) it.next();
            int i = content.indexOf(wds);
            if (i != -1) {
                seriouseWord = wds;
                break;
            }
        }
        return seriouseWord;
    }

    /**
     * ��鴫����ַ��Ƿ�Ϊ���дʻ�
     *
     * @param content_str
     * @param hRet        9 ��ݿ��쳣
     * @param _return     1 �����дʻ� 2 ���дʻ�
     * @throws SQLException
     */
    public void checkContent(String content_str, javax.xml.rpc.holders.IntHolder hRet, javax.xml.rpc.holders.StringHolder _return) throws SQLException {
        _return.value = "1";
        String sql = "select * from t_serious_word where Word=?";
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, content_str);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                _return.value = "2";
            }
            pstmt.close();
        } catch (SQLException e) {
            hRet.value = 9;
            throw e;
        } finally {
            conn.close();
        }
    }

    public int sendMsg(MsgSendReq msgSendReq, List mobileList) throws SQLException, ParseException {
        int sequence = getSegquence("t_send_request");
        Connection conn = null;
        try {
            conn = getJdbcTemplate().getDataSource().getConnection();
            conn.setAutoCommit(false);
            String sqlRequest = "insert into t_send_request (RequestKey,UserName,AuthPass,WordLength," + "MSGType,smstype,Content,SendUrl,MobileList,Priority,CreateDate,ScheduleDate,State," + "remark,ext,AccountId,UserId,PID,ModuleId,CDRType)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sqlRequest);
            int i = 0;
            pstmt.setInt(++i, sequence);
            pstmt.setString(++i, msgSendReq.getAuthInfo().getUserName());
            pstmt.setString(++i, msgSendReq.getAuthInfo().getAuthPass());
            pstmt.setInt(++i, msgSendReq.getWordLength());
            pstmt.setString(++i, msgSendReq.getMsgType());
            pstmt.setInt(++i, msgSendReq.getSmsType());
            pstmt.setString(++i, msgSendReq.getContent());
            pstmt.setString(++i, msgSendReq.getSendURL());
            pstmt.setString(++i, (msgSendReq.getML()).length + "");
            pstmt.setInt(++i, msgSendReq.getPrioprity());
            pstmt.setString(++i, Const.getDateString(msgSendReq.getCreateDate()));
            pstmt.setString(++i, Const.getDateString(msgSendReq.getScheduleDate()));
            pstmt.setInt(++i, 0);
            pstmt.setString(++i, "");
            pstmt.setString(++i, "");
            pstmt.setString(++i, msgSendReq.getAuthInfo().getAccountId());
            pstmt.setString(++i, msgSendReq.getAuthInfo().getUserId());
            pstmt.setString(++i, msgSendReq.getAuthInfo().getPID());
            pstmt.setInt(++i, msgSendReq.getAuthInfo().getModuleId());
            pstmt.setInt(++i, msgSendReq.getAuthInfo().getCDRType());
            pstmt.execute();
            System.out.println("mobileList.size()= " + mobileList.size());
            for (int j = 0; j < mobileList.size(); j++) {
                String sql_dyhile = " insert into dyhikemessages(from_mobile,to_mobile,pay_mobile_tel,msg_content,cost,create_date,RequestKey,userName,PASSWORD)" + " values(?,?,?,?,?,?,?,?,?)";
                i = 0;
                pstmt = conn.prepareStatement(sql_dyhile);
                pstmt.setString(++i, msgSendReq.getAuthInfo().getUserName());
                pstmt.setString(++i, (String) mobileList.get(j));
                pstmt.setString(++i, msgSendReq.getAuthInfo().getUserName());
                pstmt.setString(++i, msgSendReq.getContent());
                pstmt.setInt(++i, 0);
                pstmt.setString(++i, Const.getDateString(msgSendReq.getCreateDate()));
                pstmt.setInt(++i, sequence);
                pstmt.setString(++i, itul.getUserName());
                pstmt.setString(++i, itul.getPassword());
                pstmt.execute();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw e;
            }
        }
        return sequence;
    }

    public boolean chkDataConnect() {
        boolean chkConn = true;
        try {
            Connection conn = this.getJdbcTemplate().getDataSource().getConnection();
            conn.close();
        } catch (SQLException e) {
            Logger logger = Logger.getLogger(this.getClass());
            logger.error("Error checkDBConnection:", e);
            chkConn = false;
        }
        return chkConn;
    }

    public boolean checkContent(ototype.MsgSendReq msgSendReq) {
        boolean rtchk = true;
        if ((msgSendReq.getML() == null || msgSendReq.getML().equals("")) || (msgSendReq.getContent() == null || msgSendReq.getContent().equals("")) || msgSendReq.getAuthInfo() == null) rtchk = false;
        return rtchk;
    }

    public MsgRecvInfo[] recvMsg(MsgRecvReq msgRecvReq) throws SQLException {
        String updateSQL = " update dyhikemomessages set receive_id = ?, receive_Time = ?  where mo_to =? and receive_id =0  limit 20";
        String selectSQL = " select MOMSG_ID,mo_from,mo_to,create_time,mo_content from dyhikemomessages where receive_id =?  ";
        String insertSQL = " insert into t_receive_history select * from dyhikemomessages  where receive_id =?  ";
        String deleteSQL = " delete from dyhikemomessages where receive_id =? ";
        Logger logger = Logger.getLogger(this.getClass());
        ArrayList msgInfoList = new ArrayList();
        String mo_to = msgRecvReq.getAuthInfo().getUserName();
        MsgRecvInfo[] msgInfoArray = new ototype.MsgRecvInfo[0];
        String receiveTime = Const.DF.format(new Date());
        logger.debug("recvMsgNew1");
        Connection conn = null;
        try {
            int receiveID = this.getSegquence("receiveID");
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            conn.setAutoCommit(false);
            PreparedStatement pstmt = conn.prepareStatement(updateSQL);
            pstmt.setInt(1, receiveID);
            pstmt.setString(2, receiveTime);
            pstmt.setString(3, mo_to);
            int recordCount = pstmt.executeUpdate();
            logger.info(recordCount + " record(s) got");
            if (recordCount > 0) {
                pstmt = conn.prepareStatement(selectSQL);
                pstmt.setInt(1, receiveID);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    MsgRecvInfo msg = new MsgRecvInfo();
                    msg.setDestMobile(rs.getString("mo_to"));
                    msg.setRecvAddi(rs.getString("mo_to"));
                    msg.setSendAddi(rs.getString("MO_FROM"));
                    msg.setContent(rs.getString("mo_content"));
                    msg.setRecvDate(rs.getString("create_time"));
                    msgInfoList.add(msg);
                }
                msgInfoArray = (MsgRecvInfo[]) msgInfoList.toArray(new MsgRecvInfo[msgInfoList.size()]);
                pstmt = conn.prepareStatement(insertSQL);
                pstmt.setInt(1, receiveID);
                pstmt.execute();
                pstmt = conn.prepareStatement(deleteSQL);
                pstmt.setInt(1, receiveID);
                pstmt.execute();
                conn.commit();
            }
            logger.debug("recvMsgNew2");
            return msgInfoArray;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * ���ֻ��ַ��װ��MsgSendReq����
     *
     * @param username
     * @param subId
     * @param password
     * @param msg
     * @param destnumbers
     * @param scheduleDate
     * @param msg_type
     * @return
     */
    public MsgSendReq getMsgSendReq(String username, String subId, String password, String timeStamp, String msg, String destnumbers, String scheduleDate, int msg_type) {
        MsgSendReq msq = new MsgSendReq();
        AuthInfo auinfo = new AuthInfo();
        auinfo.setAuthPass(password);
        auinfo.setUserName(username);
        auinfo.setTimestamp(timeStamp);
        msq.setScheduleDate(scheduleDate);
        msq.setAuthInfo(auinfo);
        msq.setContent(msg);
        msq.setML(destnumbers.split(Const.MOBILE_NUMBER_SEPERATOR));
        msq.setSendURL("  ");
        msq.setMsgType(msg_type + "");
        msq.setCreateDate(Const.DF.format(new Date()));
        return msq;
    }

    /**
     * ���SQL�õ����͹����Ϣ
     *
     * @param sql
     * @return
     * @throws Exception
     */
    public MsgRecvInfo[] getMsgSearch(String sql) throws SQLException {
        Logger logger = Logger.getLogger(this.getClass());
        MsgRecvInfo[] msgInfoArray = new ototype.MsgRecvInfo[0];
        ArrayList array = new ArrayList();
        Connection conn = this.getJdbcTemplate().getDataSource().getConnection();
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                MsgRecvInfo msgrcv = new MsgRecvInfo();
                msgrcv.setContent(rs.getString("mo_content"));
                msgrcv.setRecvDate(rs.getString("receive_time"));
                msgrcv.setDestMobile(rs.getString("mo_to"));
                msgrcv.setSendAddi(rs.getString("mo_to"));
                array.add(msgrcv);
            }
            msgInfoArray = (MsgRecvInfo[]) array.toArray(new MsgRecvInfo[array.size()]);
        } catch (SQLException e) {
            logger.error("error getMsgSearch", e);
            throw e;
        } finally {
            conn.close();
        }
        return msgInfoArray;
    }

    public void getExecuteRs(String sql, javax.xml.rpc.holders.IntHolder hRet) throws SQLException {
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.execute();
            pstmt.close();
        } catch (SQLException e) {
            hRet.value = 9;
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    public void getSMSstat(int batchid, ototype.holders.ArrayOfMsgInfoHolder msgSendStatInfo, javax.xml.rpc.holders.IntHolder hRet) throws SQLException {
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            String sql = "select * from dyhikemessages where requestKey=? union select * from t_send_history where requestKey=? ";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, batchid);
            pstmt.setInt(2, batchid);
            ResultSet rs = pstmt.executeQuery();
            ArrayList array = new ArrayList();
            while (rs.next()) {
                MsgInfo msgInfo = new MsgInfo();
                msgInfo.setContent(rs.getString("MSG_CONTENT"));
                msgInfo.setCreateDate(rs.getString("CREATE_DATE"));
                msgInfo.setMobile(rs.getString("TO_MOBILE"));
                msgInfo.setSendDate(rs.getString("SEND_TIME") == null ? "0" : rs.getString("SEND_TIME"));
                msgInfo.setState(rs.getInt("SEND_OUT_FLAG"));
                array.add(msgInfo);
            }
            msgSendStatInfo.value = (MsgInfo[]) array.toArray(new MsgInfo[array.size()]);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    public void msgSearchCount(String username, javax.xml.rpc.holders.StringHolder count, javax.xml.rpc.holders.IntHolder hRet) throws SQLException {
        String sql = "select count(*) from dyhikemomessages where mo_to=? union  " + "  select count(*) from dyhikemessages where FROM_MOBILE =?";
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();
            int i = 0;
            while (rs.next()) {
                i += rs.getInt(1);
            }
            count.value = i + "";
            pstmt.close();
        } catch (SQLException e) {
            throw e;
        } finally {
            conn.close();
        }
    }

    public boolean checkAuthCode(String authCode) {
        if (authCode == null || authCode.equals("")) {
            return false;
        }
        return authCode.equals(itul.getAuthcode());
    }

    public int addUser(ototype.AuthInfo auth_Info, java.lang.String authCode) throws SQLException {
        Logger logger = Logger.getLogger(Service.class);
        boolean ca = this.checkAuthCode(authCode);
        int i = 0;
        if (!ca) {
            i = 2;
            return i;
        }
        String sql = " insert into auth(username,password) values (?,?)";
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, auth_Info.getUserName());
            pstmt.setString(2, auth_Info.getAuthPass());
            pstmt.execute();
        } catch (SQLException e) {
            logger.error("error addUser", e);
            i = 9;
            throw e;
        } finally {
            conn.close();
        }
        return i;
    }

    /**
     * ɾ���û�
     *
     * @param auth_Info
     * @param authCode
     * @return
     * @throws SQLException
     */
    public int deleteUser(ototype.AuthInfo auth_Info, java.lang.String authCode) throws SQLException {
        boolean ca = this.checkAuthCode(authCode);
        int i = 0;
        if (!ca) {
            i = 2;
            return i;
        }
        String sql = " delete from auth where username=? ";
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, auth_Info.getUserName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            i = 9;
            throw e;
        } finally {
            conn.close();
        }
        return i;
    }

    /**
     * ����û�����
     *
     * @param auth_Info
     * @param newPassWord
     * @return
     * @throws SQLException
     */
    public int updateUser(ototype.AuthInfo auth_Info, java.lang.String newPassWord) throws SQLException {
        String sql = "update auth set password=? where username=?";
        Connection conn = null;
        int recordCount = 0;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPassWord);
            pstmt.setString(2, auth_Info.getUserName());
            recordCount = pstmt.executeUpdate();
        } finally {
            conn.close();
        }
        return recordCount;
    }

    /**
     * ��֤�û�
     *
     * @param auth_Info
     * @return
     * @throws SQLException
     */
    public boolean validUser(ototype.AuthInfo auth_Info) {
        Logger logger = Logger.getLogger(PublicUtil.class);
        String aps = auth_Info.getAuthPass();
        String username = auth_Info.getUserName();
        String password = new String();
        Connection conn = null;
        String sql = "select password from auth where username=?";
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                password = rs.getString("password");
            } else {
                conn.close();
                return false;
            }
            MD5 md5 = new MD5();
            password = md5.getMD5ofStr(password + auth_Info.getTimestamp()).toLowerCase();
            if (password.equals(aps)) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error validUser", e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Error validUser on close conn", e);
                }
            }
        }
    }

    public static void main(String[] args) throws SQLException, IOException {
        ReceiveFaxWrapper rfw = new ReceiveFaxWrapper();
        rfw.setStartTime("2007-07-05");
        rfw.setEndTime("2010-09-08");
        rfw.setReceiverNumber("13420982815");
        PublicUtil pu = new PublicUtil();
    }
}

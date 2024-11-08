package com.util;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.apache.log4j.Logger;
import com.util.KeyInfo.KeyGenerator;
import com.service.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.io.FileInputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import ototype.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: Jul 20, 2008
 * Time: 10:39:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class FaxUtils {

    static XmlBeanFactory factory;

    static KeyGenerator keygen;

    static ItemUitl itul;

    static {
        factory = new XmlBeanFactory(new ClassPathResource("applicationContext.xml"));
        keygen = (KeyGenerator) factory.getBean("KeyGen");
        itul = (ItemUitl) factory.getBean("itul");
    }

    Logger logger = Logger.getLogger(FaxUtils.class);

    public DataSource getDateSource() {
        return getJdbcTemplate().getDataSource();
    }

    public void initContext() {
    }

    public JdbcTemplate getJdbcTemplate() {
        initContext();
        return itul.getJt();
    }

    public int getSegquence(String tname) {
        return (int) keygen.getNextKey(tname);
    }

    public String getFaxID(String str) {
        String dateSequence = new SimpleDateFormat(Const.DATE_FORMAT_SEQUENCE).format(new Date());
        String sequence = getSegquence("t_fax_send" + str + dateSequence) + "";
        for (int i = 0; i < (6 - sequence.length()); i++) {
            sequence = "0" + sequence;
        }
        return str + dateSequence + sequence;
    }

    /**
     * ��֤�����û�
     *
     * @param
     * @return
     * @throws java.sql.SQLException
     */
    public boolean validFaxUser(String userName, String userPassword) {
        Connection conn = null;
        String sql = "select password from t_fax_user where userID=?";
        String password = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                password = rs.getString("password");
            } else {
                conn.close();
                return false;
            }
            MD5 md5 = new MD5();
            if (userPassword != null && userPassword.equalsIgnoreCase(md5.getMD5ofStr(password))) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            logger.error("Error validFaxUser", e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Error validFaxUser on close conn", e);
                }
            }
        }
    }

    /**
     * @param parameters
     * @return efr.ResultCodeΪ����״̬
     *         //    100�������ɹ�
     *         //    200������ʧ�ܡ�����û����ȷ�Ĵ���ԭ���򷵻ش˴����룩
     *         //    201�������ֶβ�����
     *         //    202���ֶ����ʹ���
     *         //    203���ֶγ��������
     *         //    204���û�id�ѱ�ʹ��
     *         //    205�����������Ѿ���ʹ��
     *         //    206������������Դ�޷��ɹ�����
     *         //    207���ֻ���ѱ�ʹ��
     *         //    208���ֻ����Դ�޷��ɹ�����
     *         //    209���û���ע��
     *         //    210���û��ʷѲ���-------����
     *         //    211���û���֤δͨ��
     * @throws SQLException
     */
    public EFaxResult sendFax(ototype.SendFaxWrapper parameters) {
        EFaxResult efr = new EFaxResult();
        if (!validFaxUser(parameters.getUserID(), parameters.getPassWord())) {
            efr.setResultCode(211);
            return efr;
        }
        Connection conn = null;
        String faxKey = getSegquence("t_fax_send") + "";
        String sql = "insert into t_fax_send(faxKey,userID,appcode,sendername," + "sendernumber,sendercompany,sendtime,accountId, userId2, PID, moduleId, CDRType) values(?,?,?,?,?,?,?,?,?,?,?,?)";
        Fax fax = parameters.getFax();
        FaxContactor sender = fax.getSender();
        FaxContactor[] receiver = fax.getReceiver();
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            conn.setAutoCommit(false);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, faxKey);
            pstmt.setString(2, parameters.getUserID());
            pstmt.setString(3, parameters.getAppCode());
            pstmt.setString(4, sender.getContactor());
            pstmt.setString(5, sender.getFaxNumber());
            pstmt.setString(6, sender.getCompany());
            pstmt.setString(7, fax.getSendTime());
            pstmt.setString(8, parameters.getAccountId());
            pstmt.setString(9, parameters.getUserId());
            pstmt.setString(10, parameters.getPID());
            pstmt.setInt(11, parameters.getModuleId());
            pstmt.setInt(12, parameters.getCDRType());
            pstmt.executeUpdate();
            sql = "insert into t_fax_contactor(faxKey,contactorID,contactor,faxnumber,company) values(?,?,?,?,?)";
            pstmt = conn.prepareStatement(sql);
            for (int k = 0; k < receiver.length; k++) {
                pstmt.setString(1, faxKey);
                pstmt.setString(2, receiver[k].getContactorID());
                pstmt.setString(3, receiver[k].getContactor());
                pstmt.setString(4, receiver[k].getFaxNumber());
                pstmt.setString(5, receiver[k].getCompany());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            sql = "insert into t_fax_file(faxKey,fileID,filename,filetype,fileurl,faxpages) values(?,?,?,?,?,?)";
            pstmt = conn.prepareStatement(sql);
            FaxFile[] files = fax.getFiles();
            for (int h = 0; h < files.length; h++) {
                String fileID = getSegquence("t_Fax_file") + "";
                pstmt.setString(1, faxKey);
                pstmt.setString(2, fileID);
                pstmt.setString(3, files[h].getFileName());
                pstmt.setString(4, files[h].getFileType());
                pstmt.setString(5, files[h].getFileURL());
                pstmt.setInt(6, files[h].getFaxPages());
                Service.writeByteFile(files[h].getFile(), fileID);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            efr.setResultCode(100);
            efr.setResultInfo(faxKey);
        } catch (SQLException e) {
            efr.setResultCode(200);
            try {
                conn.rollback();
            } catch (Exception e1) {
                logger.error("Error validFaxUser", e1);
            }
            logger.error("Error validFaxUser", e);
        } catch (IOException e) {
            efr.setResultCode(200);
            logger.error("Error write file on sendfax", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Error sendFax on close conn", e);
                }
            }
        }
        return efr;
    }

    /**
     * ���ܴ��棬���ڽ��ܴ���ĵ绰�����ʱ�䷶Χ��
     * receive ������Ϊnull,�������Ϊnull.
     * startTime Ϊnull,��ʾ�ӵ�����ʱ��ʼ
     * endTime Ϊnull,��ʾ������12ʱ����
     *
     * @param parameters
     * @return
     * @throws SQLException
     */
    public ototype.ReceiveFaxResultWrapper receiveFax(ototype.ReceiveFaxWrapper parameters) {
        EFaxResult efr = new EFaxResult();
        ReceiveFaxResultWrapper rfrw = new ReceiveFaxResultWrapper();
        rfrw.setReceiveFaxResult(efr);
        if (!validFaxUser(parameters.getUserID(), parameters.getPassWord())) {
            efr.setResultCode(211);
            return rfrw;
        }
        Connection conn = null;
        String sql1 = "select * from t_fax_received where  receivernumber =? and  receiveTime >=? and receiveTime <= ?";
        String sql2 = "select * from t_fax_received where  receivernumber =? and  receiveTime >=? and receiveTime <= ? and sendernumber=? ";
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = null;
            if (!StringUtils.hasText(parameters.getSenderNumber())) {
                pstmt = conn.prepareStatement(sql1);
            } else {
                pstmt = conn.prepareStatement(sql2);
            }
            pstmt.setString(1, parameters.getReceiverNumber());
            String startTime = null;
            if (StringUtils.hasText(parameters.getStartTime())) {
                startTime = parameters.getStartTime();
            } else {
                startTime = new SimpleDateFormat(Const.DATE_FORMAT_SHORT).format(new Date()) + " 00:00:00";
            }
            pstmt.setString(2, startTime);
            String endTime = null;
            if (StringUtils.hasText(parameters.getEndTime())) {
                endTime = parameters.getEndTime();
            } else {
                endTime = new SimpleDateFormat(Const.DATE_FORMAT_SHORT).format(new Date()) + " 11:59:59";
            }
            pstmt.setString(3, endTime);
            if (StringUtils.hasText(parameters.getSenderNumber())) {
                pstmt.setString(4, parameters.getSenderNumber());
            }
            ResultSet rs = pstmt.executeQuery();
            StringBuffer sb = new StringBuffer();
            List faxIDList = new ArrayList();
            while (rs.next()) {
                String faxKey = rs.getString("faxID");
                faxIDList.add(faxKey);
                sb.append("<receiveFax>");
                sb.append("<faxId>").append(faxKey).append("</faxId>");
                String SQL = "select filename,fileurl,filetype,faxpages from t_fax_File where faxKey =?";
                PreparedStatement pstmtFile = conn.prepareStatement(SQL);
                pstmtFile.setString(1, faxKey);
                ResultSet rsFile = pstmtFile.executeQuery();
                while (rsFile.next()) {
                    sb.append("<file><fileName> ").append(rsFile.getString("filename")).append("</fileName><fileURL>");
                    sb.append(rsFile.getString("fileUrl")).append("</fileURL><fileType>");
                    sb.append(rsFile.getString("fileType")).append("</fileType><faxPages>");
                    sb.append(rsFile.getString("faxPages")).append("</faxPages></file>");
                }
                rsFile.close();
                sb.append("<receiveNumber>").append(rs.getString("receiverNumber")).append("</receiveNumber>");
                sb.append("<senderNumber>").append(rs.getString("senderNumber")).append("</senderNumber>");
                String time = "";
                if (rs.getTimestamp("receiveTime") != null) {
                    time = new SimpleDateFormat(Const.DATE_FORMAT).format(rs.getTimestamp("receiveTime"));
                }
                sb.append("<receiveTime>").append(time).append("</receiveTime>");
                sb.append("</receiveFax>");
            }
            rs.close();
            removeFile(faxIDList);
            efr.setResultCode(100);
            efr.setResultInfo(sb.toString());
            rfrw.setReceiveFaxResult(efr);
        } catch (SQLException e) {
            efr.setResultCode(200);
            logger.error("Error receiveFax", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Error receiveFax on close conn", e);
                }
            }
        }
        return rfrw;
    }

    /**
     * remove fax from table fax_received to table fax_received_history
     *
     * @param faxIDList
     * @return
     * @throws SQLException
     */
    private void removeFile(List faxIDList) throws SQLException {
        String insertSQL = "insert into t_fax_received_history\n" + "(faxID,agentUserid,userID,appcode,receivernumber,\n" + "sendernumber,receivetime,readtime)\n" + "select faxID,agentUserid,userID,appcode,receivernumber,\n" + "sendernumber,receivetime,now() from\n" + "t_fax_received where faxid =? ";
        String deleteSQL = "delete from t_fax_received where faxid =?";
        Connection conn = null;
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            conn.setAutoCommit(false);
            PreparedStatement pstmtInsert = conn.prepareStatement(insertSQL);
            PreparedStatement pstmtDelete = conn.prepareStatement(deleteSQL);
            for (int i = 0; i < faxIDList.size(); i++) {
                String faxID = (String) faxIDList.get(i);
                pstmtDelete.setString(1, faxID);
                pstmtDelete.addBatch();
                pstmtInsert.setString(1, faxID);
                pstmtInsert.addBatch();
            }
            pstmtInsert.executeBatch();
            pstmtDelete.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
    }

    public EFaxResult sendFaxState(SendFaxStateWrapper parameters) {
        EFaxResult result = new EFaxResult();
        if (!validFaxUser(parameters.getUserID(), parameters.getPassWord())) {
            result.setResultCode(211);
            return result;
        }
        String sql = "select * from t_fax_fee, t_fax_contactor, \n" + "t_fax_send where t_fax_fee.faxkey=?\n" + "and t_fax_contactor.faxkey=t_fax_fee.faxkey\n" + "and t_fax_fee.contactorid=t_fax_contactor.contactorid\n" + "and t_fax_fee.faxkey=t_fax_send.faxkey";
        Connection conn = null;
        String faxKey = parameters.getFaxID();
        try {
            conn = this.getJdbcTemplate().getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, faxKey);
            ResultSet rs = pstmt.executeQuery();
            StringBuffer sb = new StringBuffer();
            while (rs.next()) {
                sb.append("<FaxInfo xmlns=\"http://soap.model.service.fax99.com\">");
                String faxID = rs.getString("faxID");
                if (faxID != null && faxID.startsWith("M")) {
                    sb.append("<faxId>").append(rs.getString("contactorid")).append("</faxId>");
                    sb.append("<batchNO>").append(faxID).append("</batchNO>");
                } else {
                    sb.append("<faxId>").append(faxID).append("</faxId>");
                }
                sb.append("<orderID>").append(rs.getString("orderID")).append("</orderID>");
                sb.append("<faxNumber>").append(rs.getString("faxNumber")).append("</faxNumber>");
                String SQL = "select filename,fileurl,filetype,faxpages,fileID from t_fax_File where faxKey =?";
                PreparedStatement pstmtFile = conn.prepareStatement(SQL);
                pstmtFile.setString(1, parameters.getFaxID());
                ResultSet rsFile = pstmtFile.executeQuery();
                while (rsFile.next()) {
                    sb.append("<file><fileName> ").append(rsFile.getString("filename")).append("</fileName><fileID>");
                    sb.append(rsFile.getString("fileID")).append("</fileID><fileURL>");
                    sb.append(rsFile.getString("fileUrl")).append("</fileURL><fileType>");
                    sb.append(rsFile.getString("fileType")).append("</fileType><faxPages>");
                    sb.append(rsFile.getString("faxPages")).append("</faxPages></file>");
                }
                sb.append("<seconds>").append(rs.getString("seconds")).append("</seconds>");
                sb.append("<fee>").append(rs.getString("fee")).append("</fee>");
                sb.append("<status>").append(rs.getString("status")).append("</status>");
                sb.append("<failedReason>").append(rs.getString("failedReason")).append("</failedReason>");
                sb.append("<isOversea>").append("1".equals(rs.getString("isOversea"))).append("</isOversea>");
                sb.append("</FaxInfo>");
                rsFile.close();
            }
            result.setResultCode(100);
            result.setResultInfo(sb.toString());
        } catch (SQLException e) {
            result.setResultCode(200);
            logger.error("Error sendFaxState", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    logger.error("Error receiveFax on close conn", e);
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        FaxUtils faxUtils = new FaxUtils();
        SendFaxStateWrapper sendstate = new SendFaxStateWrapper();
        sendstate.setUserID("test");
        MD5 md5 = new MD5();
        sendstate.setPassWord(md5.getMD5ofStr("test"));
        sendstate.setFaxID("M200807210011");
        EFaxResult stateResult = faxUtils.sendFaxState(sendstate);
        System.out.println("stateResult.getResultCode() = " + stateResult.getResultCode());
        System.out.println("stateResult.getResultInfo() = " + stateResult.getResultInfo());
        ReceiveFaxWrapper rfw = new ReceiveFaxWrapper();
        rfw.setUserID("test");
        md5 = new MD5();
        System.out.println("md5.getMD5ofStr(\"test\") = " + md5.getMD5ofStr("test"));
        rfw.setPassWord(md5.getMD5ofStr("test"));
        rfw.setStartTime("2008-07-05");
        rfw.setEndTime("2010-09-08");
        rfw.setReceiverNumber("1111");
        ReceiveFaxResultWrapper rf = faxUtils.receiveFax(rfw);
        System.out.println("rf.getReceiveFaxResult() .getResultCode() = " + rf.getReceiveFaxResult().getResultCode());
        System.out.println("rf.getReceiveFaxResult() .getResultInfo() = " + rf.getReceiveFaxResult().getResultInfo());
        ototype.SendFaxWrapper sfw = new ototype.SendFaxWrapper();
        sfw.setUserID("test");
        sfw.setAppCode("15836");
        md5 = new MD5();
        sfw.setPassWord(md5.getMD5ofStr("test"));
        Fax fx = new Fax();
        FaxContactor fc = new FaxContactor();
        fc.setCompany("ibm");
        fc.setContactor("bill");
        fc.setContactorID("11");
        fc.setFaxNumber("85682869");
        FaxContactor fc1 = new FaxContactor();
        fc1.setCompany("ibm");
        fc1.setContactor("bill");
        fc1.setContactorID("22");
        fc1.setFaxNumber("85682869");
        fx.setSender(fc);
        FaxContactor[] fcs = new FaxContactor[] { fc, fc1 };
        fx.setReceiver(fcs);
        File file = new File("d:\\hello.class");
        FileInputStream fi = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = 0;
        while ((i = fi.read()) != -1) {
            baos.write(i);
        }
        byte[] bys = baos.toByteArray();
        fx.setSendTime("2008-08-05 08:11:25");
        fx.setResendTimes(5);
        fx.setResendDelay(2);
        FaxFile ffile = new FaxFile();
        ffile.setFaxPages(5);
        ffile.setFile(bys);
        ffile.setFileName("abc.doc");
        ffile.setFileType("doc");
        ffile.setFileURL("js");
        FaxFile[] ffiles = new FaxFile[] { ffile, ffile };
        fx.setFiles(ffiles);
        sfw.setFax(fx);
        EFaxResult sendResult = faxUtils.sendFax(sfw);
        System.out.println("sendResult.getResultCode() = " + sendResult.getResultCode());
        System.out.println("sendResult.getResultInfo() = " + sendResult.getResultInfo());
    }
}

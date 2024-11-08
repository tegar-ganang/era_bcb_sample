package com.techstar.exchange.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.activation.DataHandler;
import javax.xml.messaging.JAXMException;
import javax.xml.messaging.ProviderConnection;
import javax.xml.soap.AttachmentPart;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.FileCopyUtils;
import com.techstar.exchange.convert.DozerConvert;
import com.techstar.exchange.convert.XStreamConvert;
import com.techstar.exchange.dto.control.AttachmentDto;
import com.techstar.exchange.dto.control.ControlDto;
import com.techstar.exchange.dto.control.PersonDto;
import com.techstar.exchange.dto.control.QueryDto;
import com.techstar.exchange.dto.control.RecvDto;
import com.techstar.exchange.service.IexchangeService;
import com.techstar.exchange.service.utils.Constant;
import com.techstar.exchange.transfers.message.EXTSSPMessageFactory;
import com.techstar.exchange.transfers.utils.DBUtil;
import com.techstar.exchange.transfers.utils.MessageConfig;
import com.techstar.exchange.transfers.utils.UUIDHexGenerator;
import com.techstar.framework.service.messaging.client.ConnectionFactoryImpl;
import com.techstar.framework.service.messaging.client.Receiver;
import com.techstar.framework.service.messaging.message.EXTSSPHeader;
import com.techstar.framework.service.messaging.message.EXTSSPMessage;
import com.techstar.framework.service.messaging.message.TSSPBody;
import com.techstar.framework.service.messaging.message.Util;

/**
 * 
 * @author caojian Apr 4, 2007
 * modify by xiongcf 2007-4-19
 */
public class ExchangeServiceImpl implements IexchangeService {

    public void sendMessage(ControlDto control, Object bussinessDto) throws Exception {
        String sign = control.getBussinessSign();
        control.setModule(getModuleByBussSign(sign));
        Object transObject = DozerConvert.bussToTrans(bussinessDto, sign);
        String xml = XStreamConvert.beanToXml(transObject);
        List recvDtos = control.getRecvDtos();
        if (recvDtos == null) throw new Exception("接收方信息不能为空！");
        for (int i = 0; i < recvDtos.size(); i++) {
            RecvDto recv = (RecvDto) recvDtos.get(i);
            control.setTo(recv.getTo());
            sendSingleMessage(control, xml);
        }
    }

    /**
	 * 发送单条esb消息
	 * @param control 控件信息Dto
	 * @param context 发送内容
	 * @param type 0-发送业务消息 1-发送通知或公告消息
	 * @throws Exception
	 */
    private void sendSingleMessage(ControlDto control, String context) throws Exception {
        EXTSSPMessageFactory mfactory = new EXTSSPMessageFactory();
        EXTSSPMessage message = mfactory.genMessage(control, context);
        ConnectionFactoryImpl factory = new ConnectionFactoryImpl();
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "";
        ProviderConnection providerconnection = null;
        try {
            String sendQ = low(control.getModule() + "_" + control.getFrom() + "_" + control.getTo() + "_send");
            String recvQ = low(control.getModule() + "_" + control.getFrom() + "_recv");
            System.out.println("======sendQ: " + sendQ);
            System.out.println("======recvQ: " + recvQ);
            providerconnection = factory.createConnection(sendQ, recvQ);
            providerconnection.send(message);
            conn.setAutoCommit(false);
            EXTSSPHeader header = message.getEXHeader();
            UUIDHexGenerator u = new UUIDHexGenerator();
            String id = u.generate().toString();
            pstmt = conn.prepareStatement(drawOutSendSql(header, id));
            pstmt.executeUpdate();
            saveSendClobMessage(pstmt, conn, rs, context, id);
            List attachments = control.getAttachments();
            if (attachments != null && attachments.size() > 0) {
                for (int i = 0; i < attachments.size(); i++) {
                    AttachmentDto adto = (AttachmentDto) attachments.get(i);
                    String contentId = adto.getId();
                    String attid = u.generate().toString();
                    sql = "insert into message_send_attachment(ATTACHMENTID," + "VERSION,MSENDID,BUSS_ID,ATTACHMENT) values('" + attid + "',0,'" + id + "','" + contentId + "',empty_blob())";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.executeUpdate();
                    sql = "select attachment from message_send_attachment" + " where attachmentid = '" + attid + "' for update";
                    pstmt = conn.prepareStatement(sql);
                    rs = pstmt.executeQuery();
                    rs.next();
                    Blob blob = rs.getBlob(1);
                    OutputStream blobOutputStream = ((oracle.sql.BLOB) blob).getBinaryOutputStream();
                    FileCopyUtils.copy(adto.getContent(), blobOutputStream);
                    blobOutputStream.close();
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs, pstmt, conn);
            if (providerconnection != null) {
                try {
                    providerconnection.close();
                } catch (JAXMException jaxmexception) {
                    jaxmexception.printStackTrace();
                }
            }
        }
    }

    /**
	 * 将字符串所有字符转换成小写
	 * @param text
	 * @return
	 */
    private String low(String text) {
        char[] chs = text.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < chs.length; i++) {
            sb.append((chs[i] + "").toLowerCase());
        }
        return sb.toString();
    }

    /**
	 * 得到业务对象对应的模块名称
	 * @param bussinessSign
	 * @return
	 * @throws SQLException 
	 */
    private String getModuleByBussSign(String bussinessSign) throws SQLException {
        String sql = "select module from message_qname t where buss_sign='" + bussinessSign + "'";
        String module = "";
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                module = rs.getString(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs, pstmt, conn);
        }
        return module;
    }

    /**
	 * 保存发送消息主体内容
	 * @param pstmt
	 * @param conn
	 * @param rs
	 * @param context
	 * @param id
	 */
    private void saveSendClobMessage(PreparedStatement pstmt, Connection conn, ResultSet rs, String context, String id) throws Exception {
        String sql = "select message from message_send" + " where msendid = '" + id + "' for update";
        pstmt = conn.prepareStatement(sql);
        rs = pstmt.executeQuery();
        rs.next();
        Clob clob = rs.getClob(1);
        Writer clobWriter = ((oracle.sql.CLOB) clob).getCharacterOutputStream();
        clobWriter.write(context);
        clobWriter.close();
    }

    public void recvMessage() throws Exception {
        String[] modules = null;
        String[] recvs = null;
        String local_node = "";
        try {
            local_node = MessageConfig.getValue("LOCAL_NODE");
            String recv_queue = MessageConfig.getValue("RECV_QUEUE");
            String module = MessageConfig.getValue("MODULE");
            modules = StringUtils.split(module, ",");
            recvs = StringUtils.split(recv_queue, ",");
        } catch (Exception e) {
            throw new Exception("recvMessage()失败！请检查message.properties中是否正确配置了LOCAL_NODE、RECV_QUEUE、MODULE参数");
        }
        List sendQs = new LinkedList();
        List recvQs = new LinkedList();
        for (int i = 0; i < modules.length; i++) {
            String m = modules[i];
            for (int j = 0; j < recvs.length; j++) {
                String sendQ = m + "_" + local_node + "_" + recvs[j] + "_send";
                String recvQ = m + "_" + local_node + "_recv";
                sendQs.add(sendQ);
                recvQs.add(recvQ);
            }
        }
        for (int i = 0; i < sendQs.size(); i++) {
            String sendQ = (String) sendQs.get(i);
            String recvQ = (String) recvQs.get(i);
            System.out.println("开始接收：sendQ=" + sendQ + "  recvQ=" + recvQ);
            try {
                recvMessage(sendQ, recvQ);
            } catch (Exception e) {
                System.out.println("没有从 " + recvQ + " 队列中得到新的消息！");
                e.printStackTrace();
            }
        }
    }

    private void recvMessage(String from, String to) throws Exception {
        ConnectionFactoryImpl factory = new ConnectionFactoryImpl();
        Receiver receiver = null;
        ProviderConnection connection = factory.createConnection(from, to);
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "";
        try {
            receiver = Receiver.createReceiver(connection);
            receiver.open();
            EXTSSPMessage message = (EXTSSPMessage) receiver.receiveEX();
            if (message == null) {
                System.out.println("no message");
            } else {
                conn.setAutoCommit(false);
                EXTSSPHeader header = message.getEXHeader();
                UUIDHexGenerator u = new UUIDHexGenerator();
                String id = u.generate().toString();
                pstmt = conn.prepareStatement(drawOutRecvSql(header, id));
                pstmt.executeUpdate();
                String xml = "";
                TSSPBody body = message.getBody();
                xml = body.getDomAsString();
                xml = xml.replaceAll("ns1:", "");
                saveClobMessage(pstmt, conn, rs, xml, id);
                String notify_id = "";
                Iterator iter = message.getAttachments();
                while (iter.hasNext()) {
                    AttachmentPart a = (AttachmentPart) iter.next();
                    String contentId = a.getContentId();
                    if (contentId.startsWith(Constant.PREFIX_PERSON)) {
                        DataHandler dh = a.getDataHandler();
                        InputStream is = dh.getInputStream();
                        byte[] temp = FileCopyUtils.copyToByteArray(is);
                        String content = new String(temp);
                        RecvDto recv = (RecvDto) XStreamConvert.xmlToBean(content);
                        if (recv == null) throw new Exception("接收方信息对象转换错误！请检查存入的信息对象xml字符串是否正确:" + content);
                        if (notify_id.equals("")) {
                            notify_id = u.generate().toString();
                            header.setType(Constant.MESSAGETYPE_NOTIFY);
                            pstmt = conn.prepareStatement(drawOutRecvSql(header, notify_id));
                            pstmt.executeUpdate();
                            String notify_content = header.getNotifyContent();
                            if (notify_content == null) notify_content = "接收到新的esb消息，但未定义通知消息内容!";
                            saveClobMessage(pstmt, conn, rs, notify_content, notify_id);
                        }
                        savePersonInfo(pstmt, conn, recv, notify_id);
                    } else {
                        DataHandler dh = a.getDataHandler();
                        InputStream is = dh.getInputStream();
                        String attid = u.generate().toString();
                        sql = "insert into message_recv_attachment(ATTACHMENTID," + "VERSION,MRECVID,BUSS_ID,ATTACHMENT) values('" + attid + "',0,'" + id + "','" + contentId + "',empty_blob())";
                        pstmt = conn.prepareStatement(sql);
                        pstmt.executeUpdate();
                        sql = "select attachment from message_recv_attachment" + " where attachmentid = '" + attid + "' for update";
                        pstmt = conn.prepareStatement(sql);
                        rs = pstmt.executeQuery();
                        rs.next();
                        Blob blob = rs.getBlob(1);
                        OutputStream blobOutputStream = ((oracle.sql.BLOB) blob).getBinaryOutputStream();
                        FileCopyUtils.copy(is, blobOutputStream);
                        is.close();
                        blobOutputStream.close();
                    }
                }
                conn.commit();
                conn.setAutoCommit(true);
            }
            receiver.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                System.out.println("received message, rollback");
                if (receiver != null) {
                    receiver.rollback();
                }
            } catch (JAXMException e1) {
                e1.printStackTrace();
            }
        } finally {
            DBUtil.close(rs, pstmt, conn);
            if (receiver != null) {
                try {
                    receiver.close();
                } catch (JAXMException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JAXMException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * 保存message_person表信息
	 * @param pstmt
	 * @param conn
	 * @param rs
	 * @param content
	 * @param id
	 * @throws Exception 
	 */
    private void savePersonInfo(PreparedStatement pstmt, Connection conn, RecvDto recv, String id) throws Exception {
        if (recv.getPersonDtos() != null && recv.getPersonDtos().size() > 0) {
            UUIDHexGenerator u = new UUIDHexGenerator();
            for (int i = 0; i < recv.getPersonDtos().size(); i++) {
                PersonDto pd = (PersonDto) recv.getPersonDtos().get(i);
                String sql = "insert into message_person(id," + "mrecv_id,personid,isread,mobile,version) values('" + u.generate().toString() + "','" + id + "','" + pd.getPersonId() + "','0','" + pd.getMobile() + "','0')";
                pstmt = conn.prepareStatement(sql);
                pstmt.executeUpdate();
            }
        }
    }

    /**
	 * 保存接收到信息主体内容
	 * @param conn 
	 * @param pstmt 
	 * @param rs
	 * @param xml
	 * @param id 
	 * @throws SQLException
	 * @throws IOException
	 */
    private void saveClobMessage(PreparedStatement pstmt, Connection conn, ResultSet rs, String content, String id) throws Exception {
        String sql = "select message from message_recv" + " where mrecvid = '" + id + "' for update";
        pstmt = conn.prepareStatement(sql);
        rs = pstmt.executeQuery();
        rs.next();
        Clob clob = rs.getClob(1);
        Writer clobWriter = ((oracle.sql.CLOB) clob).getCharacterOutputStream();
        clobWriter.write(content);
        clobWriter.close();
    }

    /**
	 * 根据header生成表MESSAGE_RECV的insertsql
	 * 
	 * @param header
	 * @param id
	 * @return
	 */
    private String drawOutSendSql(EXTSSPHeader header, String id) {
        String sql = "insert into MESSAGE_SEND(" + "MSENDID,ADDR_FROM,ADDR_TO,BUSS_SIGN,SENDTIME," + "CENTERTASKID,AREATASKID,TYPE,CONTENTVERSION,ISTRIGGER," + "OBJECTNAME,METHODNAME,TRACENUMBER,MESSAGE,VERSION)" + " values('" + id + "','" + header.getFrom() + "','" + header.getTo() + "','" + header.getBussinessSign() + "',to_date('" + header.getTimestamp() + "','yyyy-mm-dd hh24:mi:ss'),'" + header.getCenterTaskId() + "','" + header.getAreaTaskId() + "','" + header.getType() + "','" + header.getContentVersion() + "','" + header.getIsTrigger() + "','" + header.getObjectName() + "','" + header.getMethodName() + "','" + header.getTraceNumber() + "',empty_clob(),0)";
        return sql;
    }

    /**
	 * 根据header生成表MESSAGE_RECV的insertsql
	 * 
	 * @param header
	 * @param id
	 * @return
	 */
    private String drawOutRecvSql(EXTSSPHeader header, String id) {
        String sql = "insert into MESSAGE_RECV(" + "MRECVID,ADDR_FROM,ADDR_TO,BUSS_SIGN,SENDTIME,RECVTIME,ISUSED," + "CENTERTASKID,AREATASKID,TYPE,CONTENTVERSION,ISTRIGGER," + "OBJECTNAME,METHODNAME,TRACENUMBER,MESSAGE,VERSION)" + " values('" + id + "','" + header.getFrom() + "','" + header.getTo() + "','" + header.getBussinessSign() + "',to_date('" + header.getTimestamp() + "','yyyy-mm-dd hh24:mi:ss'),to_date('" + Util.generateTimeStamp() + "','yyyy-mm-dd hh24:mi:ss'),'" + "0" + "','" + header.getCenterTaskId() + "','" + header.getAreaTaskId() + "','" + header.getType() + "','" + header.getContentVersion() + "','" + header.getIsTrigger() + "','" + header.getObjectName() + "','" + header.getMethodName() + "','" + header.getTraceNumber() + "',empty_clob(),0)";
        return sql;
    }

    /**
	 * 根据ControlDto生成表MESSAGE_RECV的insertsql
	 * 
	 * @param header
	 * @param id
	 * @return
	 */
    private String drawOutRecvSql(ControlDto header, String id) {
        String sql = "insert into MESSAGE_RECV(" + "MRECVID,ADDR_FROM,ADDR_TO,BUSS_SIGN,SENDTIME,RECVTIME,ISUSED," + "CENTERTASKID,AREATASKID,TYPE,CONTENTVERSION,ISTRIGGER," + "OBJECTNAME,METHODNAME,TRACENUMBER,MESSAGE,VERSION)" + " values('" + id + "','" + dealNull(header.getFrom()) + "','" + dealNull(header.getTo()) + "','" + dealNull(header.getBussinessSign()) + "',to_date('" + Util.generateTimeStamp() + "','yyyy-mm-dd hh24:mi:ss'),to_date('" + Util.generateTimeStamp() + "','yyyy-mm-dd hh24:mi:ss'),'" + "0" + "','" + dealNull(header.getCenterTaskId()) + "','" + dealNull(header.getAreaTaskId()) + "','" + dealNull(header.getType()) + "','" + dealNull(header.getContentVersion()) + "','" + dealNull(header.getIsTrigger()) + "','" + dealNull(header.getObjectName()) + "','" + dealNull(header.getMethodName()) + "','" + dealNull(header.getTraceNumber()) + "',empty_clob(),0)";
        return sql;
    }

    /**
	 * 处理空值
	 * @param from
	 * @return
	 */
    private String dealNull(String str) {
        if (str == null || str.equalsIgnoreCase("null")) return ""; else return str;
    }

    public List recvMessage(QueryDto querydto, String type) throws Exception {
        List result = new ArrayList();
        StringBuffer sb = new StringBuffer("select mrecvid, message from message_recv where 1=1 and type='" + type + "' ");
        if (isQuery(querydto.getFrom())) {
            sb.append(" and addr_from = '" + querydto.getFrom() + "' ");
        }
        if (isQuery(querydto.getTo())) {
            sb.append(" and addr_to = '" + querydto.getTo() + "' ");
        }
        if (isQuery(querydto.getPersonId())) {
            sb.append(" and mrecvid in(select p.mrecv_id from message_person p where p.isRead='0' and p.personid='" + querydto.getPersonId() + "') ");
        }
        sb.append(" order by recvtime desc");
        System.out.println(sb.toString());
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sb.toString());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String mrecvid = rs.getString(1);
                Clob clob = rs.getClob(2);
                String message = clob.getSubString(1, (int) clob.length());
                result.add(message);
                String sql = "update message_recv t " + "set isused = '1' " + "where mrecvid = '" + mrecvid + "'";
                pstmt = conn.prepareStatement(sql);
                pstmt.executeUpdate();
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs, pstmt, conn);
        }
        return result;
    }

    /**
	 * 是否是查询条件
	 * @param from
	 * @return
	 */
    private boolean isQuery(String str) {
        if (str == null || "".equals(str) || "null".equals(str)) return false; else return true;
    }

    public List recvMessage(QueryDto querydto) throws Exception {
        List result = new ArrayList();
        String sql = "select * " + "from message_recv t " + "where type='3' and isused = '0' ";
        if (querydto != null) {
            if (querydto != null && querydto.getSign() != null) sql = sql + " and buss_sign = '" + querydto.getSign() + "'";
        }
        System.out.println(sql);
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                List list = new ArrayList();
                String mrecvid = rs.getString(1);
                ControlDto dto = new ControlDto();
                dto.setFrom(rs.getString(2));
                dto.setTo(rs.getString(3));
                dto.setBussinessSign(rs.getString(4));
                dto.setSendTime(rs.getString(5));
                dto.setRecvTime(rs.getString(6));
                dto.setCenterTaskId(rs.getString(8));
                dto.setAreaTaskId(rs.getString(9));
                dto.setType(rs.getString(10));
                dto.setContentVersion(rs.getString(11));
                dto.setIsTrigger(rs.getString(12));
                dto.setObjectName(rs.getString(13));
                dto.setMethodName(rs.getString(14));
                dto.setTraceNumber(rs.getString(15));
                List attachments = new ArrayList();
                sql = "select * from message_recv_attachment t " + "where mrecvid = '" + mrecvid + "'";
                pstmt = conn.prepareStatement(sql);
                ResultSet brs = pstmt.executeQuery();
                while (brs.next()) {
                    byte[] buf = new byte[128];
                    Blob blob = brs.getBlob(5);
                    InputStream is = blob.getBinaryStream();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int len = 0;
                    while ((len = is.read(buf)) != -1) {
                        os.write(buf, 0, len);
                    }
                    byte[] b = os.toByteArray();
                    os.flush();
                    os.close();
                    AttachmentDto adto = new AttachmentDto();
                    adto.setId(brs.getString(4));
                    adto.setContent(b);
                    attachments.add(adto);
                }
                dto.setAttachments(attachments);
                list.add(dto);
                Clob clob = rs.getClob(16);
                String bussXml = clob.getSubString(1, (int) clob.length());
                Object trans = XStreamConvert.xmlToBean(bussXml);
                Object buss = DozerConvert.transToBuss(trans, rs.getString(4));
                list.add(buss);
                sql = "update message_recv t " + "set isused = '1' " + "where mrecvid = '" + mrecvid + "'";
                pstmt = conn.prepareStatement(sql);
                pstmt.executeUpdate();
                result.add(list);
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs, pstmt, conn);
        }
        return result;
    }

    public void sendMessage(ControlDto control, String message) throws Exception {
        List recvDtos = control.getRecvDtos();
        if (recvDtos == null) throw new Exception("接收方信息不能为空！");
        for (int i = 0; i < recvDtos.size(); i++) {
            RecvDto recv = (RecvDto) recvDtos.get(i);
            control.setTo(recv.getTo());
            Connection conn = DBUtil.getConnection();
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn.setAutoCommit(false);
                UUIDHexGenerator u = new UUIDHexGenerator();
                String id = u.generate().toString();
                pstmt = conn.prepareStatement(drawOutRecvSql(control, id));
                pstmt.executeUpdate();
                saveClobMessage(pstmt, conn, rs, message, id);
                savePersonInfo(pstmt, conn, recv, id);
                conn.commit();
                conn.setAutoCommit(true);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                DBUtil.close(rs, pstmt, conn);
            }
        }
    }
}

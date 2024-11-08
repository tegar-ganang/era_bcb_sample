package parse;

import org.apache.log4j.Logger;
import com.creawor.hz_market.t_channel.t_channel;
import com.creawor.hz_market.t_channel_sale.t_channel_sale;
import com.creawor.hz_market.t_group.t_group;
import com.creawor.km.util.DateUtil;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import jxl.*;
import net.sf.hibernate.*;
import net.sf.hibernate.cfg.Configuration;

public class ChannelHandle {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(ChannelHandle.class);

    String date = null;

    private String filePath = "";

    public String uploadMessage = "";

    public void setString(String date) {
        if (logger.isDebugEnabled()) {
            logger.debug("setString(String) - start");
        }
        this.date = date;
        if (logger.isDebugEnabled()) {
            logger.debug("setString(String) - end");
        }
    }

    public ChannelHandle() {
        m_channelAL = null;
        m_channelSaleAL = null;
    }

    private Date StringtoDate(String str) {
        if (logger.isDebugEnabled()) {
            logger.debug("StringtoDate(String) - start");
        }
        if (str == null || "".equals(str)) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = format.parse(str);
        } catch (Exception ex) {
            logger.error("StringtoDate(String)", ex);
            ex.printStackTrace();
            Date returnDate = StringtoDate("1970-01-01");
            if (logger.isDebugEnabled()) {
                logger.debug("StringtoDate(String) - end");
            }
            return returnDate;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("StringtoDate(String) - end");
        }
        return date;
    }

    public void readExcel(String filePath) throws UploadException {
        if (logger.isDebugEnabled()) {
            logger.debug("readExcel(String) - start");
        }
        if (filePath == null) return;
        m_channelAL = new ArrayList();
        m_channelSaleAL = new ArrayList();
        t_channel channel = null;
        t_channel_sale channelsale = null;
        try {
            InputStream is = new FileInputStream(filePath);
            readExcel(filePath, is);
        } catch (Exception e) {
            logger.error("readExcel(String)", e);
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("readExcel(String) - end");
        }
    }

    public ArrayList readExcel(String filePath, InputStream is) throws UploadException {
        if (logger.isDebugEnabled()) {
            logger.debug("readExcel(InputStream) - start");
        }
        ArrayList al = new ArrayList();
        CheckDate cd = new CheckDate();
        m_channelAL = new ArrayList();
        m_channelSaleAL = new ArrayList();
        t_channel channel = null;
        t_channel_sale channelsale = null;
        try {
            this.filePath = filePath;
            WorkbookSettings workbookSettings = new WorkbookSettings();
            workbookSettings.setEncoding("UTF-8");
            Workbook rwb = Workbook.getWorkbook(is, workbookSettings);
            Sheet st = rwb.getSheet(0);
            int length = st.getRow(1).length;
            logger.info(length);
            if (length != 18) {
                uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�2�б���������ģ��Ҫ��(��18��),��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                return null;
            }
            int rownum = st.getRows();
            for (int i = 2; i < rownum; i++) {
                Cell cell[] = st.getRow(i);
                if (null == cell[0].getContents() || "".equals(cell[0].getContents().trim())) continue;
                channel = new t_channel();
                channelsale = new t_channel_sale();
                channel.setService_hall_code(cell[0].getContents().trim());
                channelsale.setChannel_code(cell[0].getContents().trim());
                channel.setService_hall_name(cell[1].getContents());
                channelsale.setChannel_name(cell[1].getContents());
                if (null != cell[2].getContents() && !"".equals(cell[2].getContents())) {
                    channel.setCompany(cell[2].getContents());
                    channelsale.setCompany(cell[2].getContents());
                }
                if (cell[3] != null && cell[3].getContents().length() != 0) {
                    String dateStr = cell[3].getContents();
                    if (null == dateStr || "".equals(dateStr)) if (!(cd.checkDate(dateStr) || cd.checkDate2(dateStr))) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>4</font>�е�<font color='red'>����</font>�ֶ���ݲ��Ϸ�(yyyy-mm),��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                    channel.setInsert_day(DateUtil.parse(dateStr, null));
                    channelsale.setInsert_day(DateUtil.parse(dateStr, null));
                } else {
                    try {
                        channel.setInsert_day(java.sql.Date.valueOf((cell[3].getContents())));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>4</font>�е�<font color='red'>����</font>Ϊ��,�����ݲ��Ϸ�(yyyy-mm),��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[4].getContents() && !"".equals(cell[4].getContents())) {
                    channel.setChannel_type(cell[4].getContents());
                }
                if (null != cell[5].getContents() && !"".equals(cell[5].getContents())) {
                    channel.setStar_level(cell[5].getContents());
                } else {
                    channel.setStar_level("0");
                }
                if (null != cell[6].getContents() && !"".equals(cell[6].getContents())) {
                    channel.setContact_man(cell[6].getContents());
                }
                if (null != cell[7].getContents() && !"".equals(cell[7].getContents())) {
                    channel.setContact_mobile(cell[7].getContents());
                    channel.setContact_tel(cell[7].getContents());
                }
                if (null != cell[8].getContents() && !"".equals(cell[8].getContents())) {
                    try {
                        channel.setRent(Double.parseDouble(cell[8].getContents()));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>9</font>�е�<font color='red'>�������</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channel.setRent(0);
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>9</font>�е�<font color='red'>�������</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[9].getContents() && !"".equals(cell[9].getContents())) {
                    try {
                        channelsale.setCharge_avg(Float.parseFloat(cell[9].getContents()));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>10</font>�е�<font color='red'>���³�ֵ��</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channelsale.setCharge_avg(0);
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>10</font>�е�<font color='red'>���³�ֵ��</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[10].getContents() && !"".equals(cell[10].getContents())) {
                    try {
                        channelsale.setCard_sale_avg(Float.parseFloat(cell[10].getContents()));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>11</font>�е�<font color='red'>�����׿�������</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channelsale.setCard_sale_avg(0);
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>11</font>�е�<font color='red'>�����׿�������</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[11].getContents() && !"".equals(cell[11].getContents())) {
                    try {
                        channelsale.setCard_apply_avg(Float.parseFloat(cell[11].getContents()));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>12</font>�е�<font color='red'>�����׿���ȡ��</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channelsale.setCard_apply_avg(0);
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>12</font>�е�<font color='red'>�����׿���ȡ��</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[12].getContents() && !"".equals(cell[12].getContents())) {
                    try {
                        channelsale.setRecompense(Float.parseFloat(cell[12].getContents()));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>13</font>�е�<font color='red'>���³��</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channelsale.setRecompense(0);
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>13</font>�е�<font color='red'>���³��</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[13].getContents() && !"".equals(cell[13].getContents())) {
                    try {
                        channel.setAddress(cell[13].getContents());
                    } catch (Exception e) {
                        logger.error("readExcel(InputStream)", e);
                    }
                }
                if (null != cell[14].getContents() && !"".equals(cell[14].getContents())) {
                    try {
                        channel.setTown(cell[14].getContents());
                    } catch (Exception e) {
                        logger.error("readExcel(InputStream)", e);
                    }
                }
                if (null != cell[15].getContents() && !"".equals(cell[15].getContents())) {
                    try {
                        channel.setCounty(cell[15].getContents().trim());
                        if ("�ع�".equalsIgnoreCase(cell[15].getContents().trim())) {
                            channel.setParent("����ֹ�˾");
                        } else {
                            channel.setParent(cell[15].getContents().trim() + "�ֹ�˾");
                        }
                    } catch (Exception e) {
                        logger.error("readExcel(InputStream)", e);
                    }
                }
                if (null != cell[16].getContents() && !"".equals(cell[16].getContents())) {
                    try {
                        channel.setX(Double.parseDouble((cell[16].getContents())));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>17</font>�е�<font color='red'>����</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channel.setX(Double.parseDouble((cell[16].getContents())));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>17</font>�е�<font color='red'>����</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                if (null != cell[17].getContents() && !"".equals(cell[17].getContents())) {
                    try {
                        channel.setY(Double.parseDouble((cell[17].getContents())));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>17</font>�е�<font color='red'>γ��</font>�ֶβ�Ϊ����,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                } else {
                    try {
                        channel.setY(Double.parseDouble((cell[17].getContents())));
                    } catch (Exception e) {
                        uploadMessage = "&nbsp;&nbsp;&nbsp;&nbsp;�����ļ�:<font color='blue'>" + filePath + "</font> <p>�ĵ�<font color='red'>" + (i + 1) + "</font>��,��<font color='red'>17</font>�е�<font color='red'>γ��</font>Ϊ��,��ݵ���<font color='red'><b>ʧ��</b></font>,�����µ���!";
                        return null;
                    }
                }
                try {
                    channelsale.setUpdated_day(new Date());
                    channelsale.setOpentype("����");
                } catch (Exception e) {
                    logger.error("readExcel(InputStream)", e);
                }
                m_channelSaleAL.add(channelsale);
                m_channelAL.add(channel);
                al.add(channel);
            }
            System.out.println("\r\n");
            rwb.close();
        } catch (Exception e) {
            logger.error("readExcel(InputStream)", e);
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("readExcel(InputStream) - end");
        }
        return al;
    }

    public String insertInfo() throws UploadException {
        if (logger.isDebugEnabled()) {
            logger.debug("insertInfo() - start");
        }
        String rsStr = "";
        rsStr = insertData(m_channelAL, "channel");
        System.out.println("ssssssssss");
        rsStr += insertData(m_channelSaleAL, "channelsale");
        if ("".equals(rsStr)) {
            rsStr = "�����ļ�:<font color='blue'>" + filePath + "</font>����ɹ�";
        }
        return rsStr;
    }

    private String insertData(ArrayList al, String type) throws UploadException {
        if (logger.isDebugEnabled()) {
            logger.debug("insertData(ArrayList, String) - start");
        }
        String rsStr = "";
        Session sels = null;
        try {
            SessionFactory sf = (new Configuration()).configure().buildSessionFactory();
            SessionFactory upsf = (new Configuration()).configure().buildSessionFactory();
            Session session = null;
            sels = sf.openSession();
            java.sql.Connection con = sels.connection();
            if (type == "channel") {
                t_channel cvo = null;
                for (int i = 0; i < al.size(); i++) {
                    session = upsf.openSession();
                    Transaction tx = session.beginTransaction();
                    try {
                        cvo = (t_channel) al.get(i);
                        String code = cvo.getService_hall_code();
                        Date day = cvo.getInsert_day();
                        String dateStr = DateUtil.getStr(day, null);
                        String company = cvo.getCompany();
                        dateStr = dateStr.substring(0, 7);
                        String sqlStr = "select * from t_channel where service_hall_code= '" + code + "' and company='" + company + "'  and left(convert(varchar(10),insert_day,120),7)='" + dateStr + "'";
                        ResultSet rs = con.createStatement().executeQuery(sqlStr);
                        if (rs.next()) {
                            cvo.setId(rs.getInt("id"));
                            session.update(cvo);
                        } else {
                            session.save(cvo);
                        }
                        rs.close();
                        tx.commit();
                    } catch (Exception e) {
                        logger.error("insertData(ArrayList, String)", e);
                        rsStr += "��������Ϊ" + cvo.getService_hall_code() + "�������Ϊ" + cvo.getService_hall_name() + "����ʧ�ܣ�<br>";
                        continue;
                    }
                }
            }
            if (type == "channelsale") {
                t_channel_sale csvo = null;
                for (int i = 0; i < al.size(); i++) {
                    session = upsf.openSession();
                    Transaction tx = session.beginTransaction();
                    try {
                        csvo = (t_channel_sale) al.get(i);
                        String code = csvo.getChannel_code();
                        java.util.Date date = csvo.getInsert_day();
                        String dateStr = DateUtil.getStr(date, null);
                        if (null != dateStr) {
                            dateStr = dateStr.substring(0, 7);
                        }
                        String company = csvo.getCompany();
                        String sqlStr = "select * from t_channel_sale where channel_code= '" + code + "' and left(convert(varchar(10),insert_day,120),7)='" + dateStr + "' and company='" + company + "'";
                        ResultSet rs = con.createStatement().executeQuery(sqlStr);
                        if (rs.next()) {
                            csvo.setId(rs.getInt("id"));
                            session.update(csvo);
                        } else {
                            session.save(csvo);
                        }
                        rs.close();
                    } catch (Exception e) {
                        logger.error("insertData(ArrayList, String)", e);
                        e.printStackTrace();
                    }
                    tx.commit();
                }
            }
            session.flush();
            session.close();
            sels.close();
        } catch (HibernateException e) {
            logger.error("insertData(ArrayList, String)", e);
            rsStr = e.getMessage();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("insertData(ArrayList, String) - end");
        }
        return rsStr;
    }

    public static void main(String args[]) {
        if (logger.isDebugEnabled()) {
            logger.debug("main(String[]) - start");
        }
        try {
            ChannelHandle ex = new ChannelHandle();
            String file = "f:/shehuiqudao(cmp).xls";
            ex.setString("2008-08-08");
            ex.readExcel(file);
            ex.insertInfo();
            for (int i = 0; i < ex.m_channelAL.size(); i++) {
                t_channel vo = (t_channel) ex.m_channelAL.get(i);
                System.out.println("VO:::" + vo.getAddress());
                System.out.println("PublishdateVO:::" + vo.getAddress());
            }
            for (int i = 0; i < ex.m_channelSaleAL.size(); i++) {
                t_channel_sale vo = (t_channel_sale) ex.m_channelSaleAL.get(i);
                System.out.println("VO:::" + vo.getChannel_name());
            }
        } catch (Exception e) {
            logger.error("main(String[])", e);
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("main(String[]) - end");
        }
    }

    public ArrayList m_channelAL;

    public ArrayList m_channelSaleAL;
}

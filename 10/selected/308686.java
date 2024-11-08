package ces.platform.system.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import ces.coral.dbo.DBOperation;
import ces.coral.dbo.ERDBOperationFactory;
import ces.coral.file.CesGlobals;
import ces.coral.log.Logger;

/**
 * <p>����:
 * <font class=titlefont>
 * �����к���ɡ���
 * </font>
 * <p>����:
 * <font class=descriptionfont>
 * <br>������к�
 * </font>
 * <p>�汾��:
 * <font class=versionfont>
 * Copyright (c) 2.50.2003.0925
 * </font>
 * <p>��˾:
 * <font class=companyfont>
 * �Ϻ�������Ϣ��չ���޹�˾
 * </font>
 * @author ����
 * @version 2.50.2003.0925
 */
public class IdGenerator extends DbBase {

    static Logger logger = new Logger(ces.platform.system.common.IdGenerator.class);

    public static final String GEN_ID_SYS_USER = "s_user_id";

    public static final String GEN_ID_SYS_RESOURCE = "s_resource_id";

    public static final String GEN_ID_SYS_AUTHORITY = "s_authority_id";

    public static final String GEN_ID_SYS_DATAPRIV = "s_datapriv_id";

    public static final String GEN_ID_SYS_ASSIGNLOG = "s_assignlog_id";

    public static final String GEN_ID_SYS_LOGINTIME = "s_logintime_id";

    public static final String GEN_ID_SYS_CTGR = "s_ctgr_id";

    public static final String GEN_ID_SYS_DATA = "s_data_id";

    public static final String GEN_ID_SYS_ORGANIZE = "s_organize_id";

    public static final String GEN_ID_SYS_VERSION = "s_version_no";

    public static final String GEN_ID_SYS_RECORD_NO = "s_record_no";

    public static final String GEN_ID_SYS_USER_GROUP = "s_user_group_id";

    public static final String GEN_ID_WM_WEBMAIL = "s_webmail_id";

    public static final String GEN_ID_WM_ACCOUNT = "s_account_id";

    public static final String GEN_ID_WM_AFFIX = "s_affix_id";

    public static final String GEN_ID_WM_ADDRESS = "s_address_id";

    public static final String GEN_ID_WM_MAILBOX = "s_mailbox_id";

    public static final String GEN_ID_WM_FOLDER = "mail_folder";

    public static final String GEN_ID_WM_FILE = "mail_file";

    public static final String GEN_ID_WM_FOLDER_SHARE = "mail_folder_share";

    public static final String GEN_ID_IP_DOC = "T_IP_DOC.ID";

    public static final String GEN_ID_IP_DOC_RES = "T_IP_DOC_RES.ID";

    public static final String GEN_ID_IP_ORDER_NO = "T_IP_ORDER_NO";

    public static final String GEN_ID_IP_SITE = "T_IP_SITE.ID";

    public static final String GEN_ID_IP_CHANNEL = "T_IP_CHANNEL.ID";

    public static final String GEN_ID_IP_TEMPLATE = "T_IP_TEMPLATE.ID";

    public static final String GEN_ID_IP_TREE = "s_ip_tree";

    public static final String GEN_ID_IP_DOC_TYPE = "T_IP_DOC_TYPE.ID";

    public static final String GEN_ID_COFFICE_DOC_REV = "coffice_doc_rcv.ID";

    public static final String GEN_ID_COFFICE_DOC_SEND = "coffice_doc_send.ID";

    public static final String GEN_ID_COFFICE_DOC_CONSULT = "coffice_doc_consult.ID";

    public static final String GEN_ID_COFFICE_ADDRS_FOLDER = "coffice_addrslist_folder.ID";

    public static final String GEN_ID_COFFICE_ADDRS_ENTRY = "coffice_addrslist_entry.ID";

    public static final String GEN_ID_COFFICE_CAL_PERSONPLAN = "coffice_cal_personplan.ID";

    public static final String GEN_ID_COFFICE_CAL_PERSET = "coffice_cal_perset.ID";

    public static final String GEN_ID_COFFICE_MESSAGE = "COFFICE_MESSAGE.ID";

    public static final String GEN_ID_COFFICE_DUTY = "coffice_duty.ID";

    public static final String GEN_ID_COFFICE_DUTY_PLAN = "coffice_duty_plan.ID";

    public static final String GEN_ID_COFFICE_DUTY_PLANLIST = "coffice_duty_planlist.ID";

    public static final String GEN_ID_COFFICE_MEET_APPLY = "COFFICE_MEET_APPLY.ID";

    public static final String GEN_ID_COFFICE_MEET_SUMMARY = "coffice_meet_summary.ID";

    public static final String GEN_ID_COFFICE_RES = "coffice_res.ID";

    private static IdGenerator instance = null;

    private Hashtable list = new Hashtable();

    private static String strGetHighValue = "SELECT high FROM t_bcm_identifier WHERE id = ?";

    private static String strUpdateHighValue = "UPDATE t_bcm_identifier SET high = ? WHERE id = ?";

    public static synchronized IdGenerator getInstance() {
        if (instance == null) {
            instance = new IdGenerator();
        }
        return instance;
    }

    /**
     * �����Ƶõ����µ�ֵ
     * @param strName �������
     * @return ���µ�ֵ
     * @throws Exception �����׳��쳣
     */
    public synchronized long getId(String strName) throws Exception {
        Sequence seq = (Sequence) list.get(strName);
        if (seq == null) {
            seq = new Sequence(strName);
            list.put(strName, seq);
        }
        return seq.getId();
    }

    class Sequence {

        private String name;

        private long high;

        private int low;

        private int lowStep;

        private long lowMax;

        Sequence(String strName) throws Exception {
            ResultSet rs = null;
            this.name = strName;
            DBOperation dbo = factory.createDBOperation(POOL_NAME);
            try {
                rs = dbo.select("high,low,low_max", "t_bcm_identifier", "id='" + this.name + "'");
                if (rs.next()) {
                    high = rs.getLong("high");
                    low = rs.getInt("low");
                    lowStep = low;
                    lowMax = rs.getLong("low_max");
                } else {
                    logger.debug("Sequence(" + name + ")������!");
                }
                if (rs != null) rs.close();
                if (lowMax != 1) {
                    high = getNextHighValue();
                }
            } catch (Exception e) {
                logger.error("��ѯ���кų��?���ܳ�ʼ��sequence�ࣺ " + e.toString());
                throw e;
            } finally {
                closeConnection(dbo);
            }
        }

        public void setLow(int low) {
            this.low = low;
        }

        public int getLow() {
            return low;
        }

        public void setLowMax(long lowMax) {
            this.lowMax = lowMax;
        }

        public long getLowMax() {
            return lowMax;
        }

        public void setHigh(long high) {
            this.high = high;
        }

        public long getHigh() {
            return high;
        }

        public long getId() throws Exception {
            long id = -1L;
            lowStep++;
            if (lowStep >= this.lowMax) {
                lowStep = low;
                high = getNextHighValue();
            }
            id = this.lowMax * high + (long) lowStep;
            return id;
        }

        private long getNextHighValue() throws Exception {
            Connection con = null;
            PreparedStatement psGetHighValue = null;
            PreparedStatement psUpdateHighValue = null;
            ResultSet rs = null;
            long high = -1L;
            int isolation = -1;
            DBOperation dbo = factory.createDBOperation(POOL_NAME);
            try {
                con = dbo.getConnection();
                psGetHighValue = con.prepareStatement(strGetHighValue);
                psGetHighValue.setString(1, this.name);
                for (rs = psGetHighValue.executeQuery(); rs.next(); ) {
                    high = rs.getLong("high");
                }
                psUpdateHighValue = con.prepareStatement(strUpdateHighValue);
                psUpdateHighValue.setLong(1, high + 1L);
                psUpdateHighValue.setString(2, this.name);
                psUpdateHighValue.executeUpdate();
            } catch (SQLException e) {
                if (con != null) {
                    con.rollback();
                }
                throw e;
            } finally {
                if (psUpdateHighValue != null) {
                    psUpdateHighValue.close();
                }
                close(dbo, psGetHighValue, rs);
            }
            return high;
        }
    }

    public static void main(String args[]) {
        IdGenerator idGen = IdGenerator.getInstance();
        try {
            System.out.println("[in IdGenerator.main()]" + idGen.getId("T_IP_DOC.ID"));
            System.out.println("[in IdGenerator.main()]" + idGen.getId("T_IP_DOC.ID"));
            System.out.println("[in IdGenerator.main()]" + idGen.getId("T_IP_DOC.ID"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

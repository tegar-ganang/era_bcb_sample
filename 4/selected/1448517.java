package ces.platform.system.ui.config.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ces.coral.lang.CESProperties;
import ces.coral.lang.StringUtil;
import ces.coral.xml.XMLProperties;
import ces.platform.system.common.Constant;
import ces.platform.system.common.Translate;
import ces.platform.system.common.XmlConstant;
import ces.platform.system.common.XmlInfo;
import ces.platform.system.ui.config.form.CommonForm;

/**
* <p>Title: ������Ϣ(OA)3.0</p>
* <p>Description: </p>
* <p>Copyright: Copyright (c) 2004 </p>
* <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
* @author ����
* @version 3.0
*/
public class CommonAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String strForward = "";
        CommonForm common = (CommonForm) form;
        try {
            Map map = new HashMap();
            String dataDir = common.getDataDir();
            String dataBasePath = dataDir + "cofficeData" + File.separator;
            String filePath = dataDir;
            filePath = StringUtil.replaceAll(filePath, "\\", "/");
            String cesHomePath = filePath;
            filePath = filePath + "coffice/webmail/temp";
            dataDir += "platformData";
            String baseUrl = common.getBaseUrl();
            String infoDataDir = baseUrl + "platformData/infoplat/";
            String imBusinessHome = baseUrl + "coffice/taizhang.jsp";
            String sUrl = common.getSysUrl();
            map.clear();
            map.put(XmlConstant.sysDataBaseType, common.getSysDataBaseType());
            map.put(XmlConstant.sysUserName, common.getSysUserName());
            map.put(XmlConstant.sysPassWord, common.getSysPassWord());
            map.put(XmlConstant.sysDriver, common.getSysJdbc());
            map.put(XmlConstant.sysServerURL, sUrl);
            map.put(XmlConstant.infoDataBaseType, common.getSysDataBaseType());
            map.put(XmlConstant.infoUserName, common.getSysUserName());
            map.put(XmlConstant.infoPassWord, common.getSysPassWord());
            map.put(XmlConstant.infoDriver, common.getSysJdbc());
            map.put(XmlConstant.infoServerURL, common.getSysUrl());
            map.put(XmlConstant.wfDataBaseType, common.getSysDataBaseType());
            map.put(XmlConstant.wfUserName, common.getSysUserName());
            map.put(XmlConstant.wfPassWord, common.getSysPassWord());
            map.put(XmlConstant.wfDriver, common.getSysJdbc());
            map.put(XmlConstant.wfServerURL, common.getSysUrl());
            saveFile(cesHomePath + "WEB-INF/config/ces_config.xml", map);
            String mailFlag = common.getMailSupply();
            String oaFlag = common.getOaSupply();
            map.clear();
            map.put(XmlConstant.dataDir, dataDir);
            map.put(XmlConstant.appServer, common.getAppServer());
            map.put(XmlConstant.loadMessageCom, common.getLoadMessageCom());
            map.put(Constant.IM_SERVER_IP, common.getImServerIp());
            map.put(Constant.IM_SERVER_PORT, common.getImServerPort());
            map.put(Constant.IM_RECEIVE_PORT, common.getImReceivePort());
            map.put(Constant.IM_BUSINESS_HOME, imBusinessHome);
            map.put(XmlConstant.mailSupply, mailFlag);
            map.put(XmlConstant.bbsSupply, common.getBbsSupply());
            map.put(XmlConstant.ipSupply, common.getIpSupply());
            map.put(XmlConstant.oaSupply, oaFlag);
            map.put(XmlConstant.addressSupply, common.getAddressSupply());
            map.put(XmlConstant.infoDataDir, infoDataDir);
            map.put(XmlConstant.authority, common.getAuthority());
            map.put(XmlConstant.usercontrol, common.getUsercontrol());
            map.put(XmlConstant.OA_BASE_URL, baseUrl);
            map.put(XmlConstant.SYS_DATABASE_TYPE, common.getSysDataBaseType());
            map.put(XmlConstant.SYS_TRANSCHARSET_URL, common.getTraUrl());
            map.put(XmlConstant.SYS_TRANSCHARSET_FORM, common.getTraForm());
            map.put(XmlConstant.SYS_TRANSCHARSET_INCL, common.getTraIncl());
            map.put(XmlConstant.SYS_TRANSCHARSET_COOK, common.getTraCook());
            map.put(XmlConstant.SYS_TRANSCHARSET_SESS, common.getTraSess());
            map.put(XmlConstant.SYS_TRANSCHARSET_BEAN, common.getTraBean());
            saveFile(cesHomePath + "WEB-INF/config/platform.xml", map);
            if ("true".equals(mailFlag)) {
                map.clear();
                map.put(XmlConstant.affixSize, common.getAffixSize());
                map.put(XmlConstant.server, common.getServer());
                map.put(XmlConstant.defaultDomain, common.getServer());
                map.put(XmlConstant.track, common.getTrack());
                map.put(XmlConstant.retunReceipt, common.getRetunReceipt());
                map.put(XmlConstant.serverType, common.getServerType());
                map.put(XmlConstant.filePath, filePath);
                map.put(XmlConstant.WM_MC_TS_SUPPORT, common.getTimingsend());
                map.put(XmlConstant.WM_MC_TS_TIME, common.getTstime());
                saveFile(cesHomePath + "WEB-INF/config/webmail.xml", map);
            }
            if ("true".equals(oaFlag)) {
                map.clear();
                map.put(XmlConstant.OA_CALPLAN_IM_NOTICE, common.getImNotice());
                map.put(XmlConstant.OA_DUTYMANGE_CHANNELPATH, common.getChannelPath());
                map.put(XmlConstant.OA_DUTYMANGE_DOCTYPEPATH, common.getDoctypePath());
                map.put(XmlConstant.OA_CALPLAN_MESSAGE, common.getLoad());
                map.put(XmlConstant.OA_CALPLAN_LEADER_CHANNEL_PATH, common.getLeadChannelPath());
                map.put(XmlConstant.OA_ADDRESS_SEARCH_LEFT, common.getLeft());
                map.put(XmlConstant.OA_ADDRESS_SEARCH_RIGHT, common.getRight());
                map.put(XmlConstant.OA_ADDRESS_SEARCH_BUTTOM, common.getButtom());
                map.put(XmlConstant.OA_RECEIVE_IM_NOTICE, common.getReImNotice());
                map.put(XmlConstant.OA_SEND_IM_NOTICE, common.getSend());
                map.put(XmlConstant.OA_MEETMANAGE_IM_NOTICE, common.getMeetImNotice());
                map.put(XmlConstant.OA_DATA_BASE_PATH, dataBasePath);
                String channelName = Translate.translate(common.getChannelName(), Constant.PARAM_FORMBEAN);
                String doctypeName = Translate.translate(common.getDoctypeName(), Constant.PARAM_FORMBEAN);
                String leadChannelName = Translate.translate(common.getLeadChannelName(), Constant.PARAM_FORMBEAN);
                map.put(XmlConstant.OA_DUTYMANGE_CHANNELPATH_NAME, channelName);
                map.put(XmlConstant.OA_DUTYMANGE_DOCTYPEPATH_NAME, doctypeName);
                map.put(XmlConstant.OA_CALPLAN_LEADER_CHANNEL_NAME, leadChannelName);
                map.put(XmlConstant.OA_CALPLAN_MESSAGE, common.getCalPlanLoad());
                map.put(XmlConstant.OA_CALPLAN_MESSAGE_TIME, common.getCalPlanTime());
                saveFile(cesHomePath + "WEB-INF/config/oa.xml", map);
            }
            XmlInfo.getInstance().getInfo();
            map.clear();
            map.put("cesHome", cesHomePath + "WEB-INF/config");
            savePropertiesFile(cesHomePath + "WEB-INF/classes/ces_init.properties", map);
            map.clear();
            map.put("log4j.appender.cescomutil.File", cesHomePath + "WEB-INF/logs/cescomutil.log");
            map.put("log4j.appender.cescomtag.File", cesHomePath + "WEB-INF/logs/cescomtag.log");
            map.put("log4j.appender.infoplat.File", cesHomePath + "WEB-INF/logs/infoplat.log");
            map.put("log4j.appender.coffice.File", cesHomePath + "WEB-INF/logs/coffice.log");
            savePropertiesFile(cesHomePath + "WEB-INF/config/system.properties", map);
            if ("true".equals(mailFlag)) {
                map.clear();
                map.put("hibernate.connection.driver_class", common.getSysJdbc());
                map.put("hibernate.connection.username", common.getSysUserName());
                map.put("hibernate.connection.url", sUrl);
                map.put("hibernate.connection.password", common.getSysPassWord());
                savePropertiesFile(cesHomePath + "WEB-INF/classes/hibernate.properties", map);
            }
            if ("true".equals(common.getBbsSupply())) {
                map.clear();
                map.put("database.driver", common.getSysJdbc());
                map.put("database.username", common.getSysUserName());
                map.put("database.url", sUrl);
                map.put("database.password", common.getSysPassWord());
                map.put("cnjbb2.home", cesHomePath + "bbs/cnjbb2_data/upload");
                map.put("database.type", common.getSysDataBaseType());
                savePropertiesFile(cesHomePath + "WEB-INF/classes/cnjbb2.properties", map);
            }
            strForward = "success";
        } catch (Exception ex) {
            ex.printStackTrace();
            strForward = "error";
        }
        return mapping.findForward(strForward);
    }

    private boolean saveFile(String fileName, Map map) {
        boolean bReturn = false;
        XMLProperties XmlPro = null;
        try {
            XmlPro = new XMLProperties(fileName, false);
            Iterator iter = map.keySet().iterator();
            String sKey = "";
            while (iter.hasNext()) {
                sKey = (String) iter.next();
                XmlPro.setProperty(sKey, map.get(sKey));
            }
            XmlPro.loadData();
            bReturn = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bReturn;
    }

    private boolean savePropertiesFile(String fileName, Map map) {
        boolean bReturn = false;
        FileOutputStream objFos = null;
        File file = null;
        try {
            file = new File(fileName);
            if (file.exists() && file.canWrite()) {
                CESProperties props = new CESProperties();
                props.clear();
                props.load(file);
                Iterator iter = map.keySet().iterator();
                String sKey = "";
                while (iter.hasNext()) {
                    sKey = (String) iter.next();
                    props.setProperty(sKey, map.get(sKey).toString());
                }
                objFos = new FileOutputStream(file);
                props.store(objFos, "");
                props = null;
            }
            bReturn = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (objFos != null) objFos.close();
                file = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bReturn;
    }
}

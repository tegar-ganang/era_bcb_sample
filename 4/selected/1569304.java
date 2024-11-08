package ces.platform.infoplat.core.base;

import java.io.File;
import java.util.Hashtable;
import ces.coral.file.CesGlobals;
import ces.platform.infoplat.core.DS;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.common.XmlConstant;

/**
 * <p>Title: ��Ϣƽ̨2.5��������Ϣ������</p>
 * <p>Description: ����Ϊ�����࣬���ڼ���ϵͳ��������Ϣ���ڴ棬�ӿ�����ٶ�</p>
 * <p>Copyright: Copyright (c) 2004 </p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class ConfigInfo {

    private static String strConfigFile;

    private static long lastModifyTime;

    private String osType;

    private String appServer;

    private boolean urlParam;

    private boolean formParam;

    private boolean includeParam;

    private boolean cookieParam;

    private boolean sessionParam;

    private boolean formbeanParam;

    private String infoplatVersion;

    private String poolName;

    private String platformDataDir;

    private String channelAuthority;

    private boolean docTypeCollectAuthority;

    private boolean doclistUsercontrol;

    private String infoplatDataDir;

    private String infoplatDataDirContext;

    private String[] serverAccessList;

    private boolean wfIncrementIndex = true;

    private Hashtable ftDocParser = new Hashtable();

    private String wfIndexDir;

    private String dateFormat;

    private String dateTimeFormat;

    private String[] imageFileSuffix;

    private int imageMinWidth;

    private int imageMinHeight;

    private boolean watermarkPrint;

    private String watermarkTitle;

    private String watermarkFontfamily;

    private String watermarkFontsize;

    private String watermarkFontcolor;

    private String watermarkAlign;

    private String[][] docTemplate;

    private String[][] chanTemplate;

    private String[][] docDsTemplate;

    private String[][] chanDsTemplate;

    private String[][] scheduleTemplate;

    private Hashtable templateTypeAndName = new Hashtable();

    private Hashtable dses = null;

    private Hashtable channleShdual = new Hashtable();

    private boolean docPublishParent = false;

    private static ConfigInfo mInstance;

    /**
	 * ��ȡ����ʵ���Ψһ����
	 * @return
	 */
    public static ConfigInfo getInstance() {
        if (mInstance == null) {
            synchronized ("ss") {
                mInstance = new ConfigInfo();
            }
        } else {
            File f = new File(strConfigFile);
            if (f.lastModified() != lastModifyTime) {
                lastModifyTime = f.lastModified();
                mInstance = null;
                mInstance = new ConfigInfo();
            }
        }
        return mInstance;
    }

    /**
	 * ˽�еĹ��캯��
	 * һ����װ��������Ϣ
	 */
    private ConfigInfo() {
        String strTemp;
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        strConfigFile = CesGlobals.getCesHome() + "/platform.xml";
        osType = cesGlobals.getCesXmlProperty("platform.install.os");
        appServer = cesGlobals.getCesXmlProperty("platform.install.app-server");
        strTemp = cesGlobals.getCesXmlProperty("platform.infoplat.doc-publish-parent");
        if (strTemp != null && "true".equalsIgnoreCase(strTemp)) {
            docPublishParent = true;
        }
        strTemp = cesGlobals.getCesXmlProperty(XmlConstant.SYS_TRANSCHARSET_URL);
        urlParam = (strTemp != null && strTemp.equals("1")) ? true : false;
        strTemp = cesGlobals.getCesXmlProperty(XmlConstant.SYS_TRANSCHARSET_URL);
        formParam = (strTemp != null && strTemp.equals("1")) ? true : false;
        strTemp = cesGlobals.getCesXmlProperty(XmlConstant.SYS_TRANSCHARSET_INCL);
        includeParam = (strTemp != null && strTemp.equals("1")) ? true : false;
        strTemp = cesGlobals.getCesXmlProperty(XmlConstant.SYS_TRANSCHARSET_COOK);
        cookieParam = (strTemp != null && strTemp.equals("1")) ? true : false;
        strTemp = cesGlobals.getCesXmlProperty(XmlConstant.SYS_TRANSCHARSET_SESS);
        sessionParam = (strTemp != null && strTemp.equals("1")) ? true : false;
        strTemp = cesGlobals.getCesXmlProperty(XmlConstant.SYS_TRANSCHARSET_BEAN);
        formbeanParam = (strTemp != null && strTemp.equals("1")) ? true : false;
        infoplatVersion = cesGlobals.getCesXmlProperty("platform.infoplat.version");
        platformDataDir = cesGlobals.getCesXmlProperty("platform.datadir");
        infoplatDataDir = platformDataDir + "/infoplat/";
        infoplatDataDirContext = cesGlobals.getCesXmlProperty("platform.infoplat.datadir-context");
        if (!infoplatDataDirContext.endsWith("/")) {
            infoplatDataDirContext += "/";
        }
        serverAccessList = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.access-list.access"), "#$");
        for (int i = 0; serverAccessList != null && i < serverAccessList.length; i++) {
            serverAccessList[i] = serverAccessList[i].trim().toLowerCase();
        }
        poolName = cesGlobals.getCesXmlProperty("platform.infoplat.poolname");
        channelAuthority = cesGlobals.getCesXmlProperty("platform.infoplat.channel-authority");
        channelAuthority = channelAuthority == null ? "" : channelAuthority;
        String sDocTypeCollectAuthority = cesGlobals.getCesXmlProperty("platform.infoplat.doctype-collect-authority");
        docTypeCollectAuthority = false;
        if (null != sDocTypeCollectAuthority && "true".equalsIgnoreCase(sDocTypeCollectAuthority)) {
            docTypeCollectAuthority = true;
        }
        String sDoclistUsercontrol = cesGlobals.getCesXmlProperty("platform.infoplat.doclist_usercontrol");
        doclistUsercontrol = false;
        if (null != sDoclistUsercontrol && "true".equalsIgnoreCase(sDoclistUsercontrol)) {
            doclistUsercontrol = true;
        }
        wfIndexDir = infoplatDataDir + "indexdb";
        String parserName = cesGlobals.getCesXmlProperty("platform.infoplat.fulltext.file-parser&class-name");
        String extName = cesGlobals.getCesXmlProperty("platform.infoplat.fulltext.file-parser");
        String[] parserNames = Function.stringToArray(parserName, "#$");
        String[] extNames = Function.stringToArray(extName, "#$");
        for (int i = 0; i < extNames.length; i++) {
            String tmpExt = extNames[i];
            String[] tmpExts = Function.stringToArray(tmpExt, ";");
            for (int j = 0; j < tmpExts.length; j++) {
                this.ftDocParser.put(tmpExts[j].trim().toLowerCase(), parserNames[i].trim());
            }
        }
        wfIncrementIndex = Boolean.valueOf(cesGlobals.getCesXmlProperty("platform.infoplat.fulltext.wf-index.increment")).booleanValue();
        dateFormat = cesGlobals.getCesXmlProperty("platform.infoplat.date-format");
        dateTimeFormat = cesGlobals.getCesXmlProperty("platform.infoplat.datetime-format");
        imageFileSuffix = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.image.file-suffix"), ";");
        String tmpImageMinWidth = cesGlobals.getCesXmlProperty("platform.infoplat.image.min-width");
        if (tmpImageMinWidth == null || tmpImageMinWidth.trim().equals("")) {
            tmpImageMinWidth = "0";
        }
        imageMinWidth = Integer.parseInt(tmpImageMinWidth.trim());
        String tmpImageMinHeight = cesGlobals.getCesXmlProperty("platform.infoplat.image.min-height");
        if (tmpImageMinHeight == null || tmpImageMinHeight.trim().equals("")) {
            tmpImageMinHeight = "0";
        }
        imageMinHeight = Integer.parseInt(tmpImageMinHeight.trim());
        watermarkPrint = Boolean.valueOf(cesGlobals.getCesXmlProperty("platform.infoplat.image.watermark&print")).booleanValue();
        watermarkTitle = cesGlobals.getCesXmlProperty("platform.infoplat.image.watermark.title");
        watermarkFontfamily = cesGlobals.getCesXmlProperty("platform.infoplat.image.watermark.font-family");
        watermarkFontsize = cesGlobals.getCesXmlProperty("platform.infoplat.image.watermark.font-size");
        watermarkFontcolor = cesGlobals.getCesXmlProperty("platform.infoplat.image.watermark.font-color");
        watermarkAlign = cesGlobals.getCesXmlProperty("platform.infoplat.image.watermark.align");
        String[] docTemplateNames = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.doc-template.template&name"), "#$");
        String[] docTemplateTypes = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.doc-template.template&type"), "#$");
        docTemplate = new String[docTemplateTypes.length][2];
        for (int i = 0; i < docTemplateTypes.length; i++) {
            docTemplate[i][0] = docTemplateTypes[i].trim();
            docTemplate[i][1] = docTemplateNames[i].trim();
            templateTypeAndName.put(docTemplate[i][0], docTemplate[i][1]);
        }
        String[] chanTemplateNames = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.chan-template.template&name"), "#$");
        String[] chanTemplateTypes = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.chan-template.template&type"), "#$");
        chanTemplate = new String[chanTemplateTypes.length][2];
        for (int i = 0; i < chanTemplateTypes.length; i++) {
            chanTemplate[i][0] = chanTemplateTypes[i].trim();
            chanTemplate[i][1] = chanTemplateNames[i].trim();
            templateTypeAndName.put(chanTemplate[i][0], chanTemplate[i][1]);
        }
        String[] docDsTemplateNames = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.docds-template.template&name"), "#$");
        String[] docDsTemplateTypes = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.docds-template.template&type"), "#$");
        docDsTemplate = new String[docDsTemplateTypes.length][2];
        for (int i = 0; i < docDsTemplateTypes.length; i++) {
            docDsTemplate[i][0] = docDsTemplateTypes[i].trim();
            docDsTemplate[i][1] = docDsTemplateNames[i].trim();
            templateTypeAndName.put(docDsTemplate[i][0], docDsTemplate[i][1]);
        }
        String[] chanDsTemplateNames = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.chands-template.template&name"), "#$");
        String[] chanDsTemplateTypes = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.templates.chands-template.template&type"), "#$");
        chanDsTemplate = new String[chanDsTemplateTypes.length][2];
        for (int i = 0; i < chanDsTemplateTypes.length; i++) {
            chanDsTemplate[i][0] = chanDsTemplateTypes[i].trim();
            chanDsTemplate[i][1] = chanDsTemplateNames[i].trim();
            templateTypeAndName.put(chanDsTemplate[i][0], chanDsTemplate[i][1]);
        }
        String[] dsChannel = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.channel-schedule.schedule&channel"), "#$");
        String[] dsTime = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.channel-schedule.schedule&time"), "#$");
        scheduleTemplate = new String[dsChannel.length][3];
        for (int i = 0; i < dsChannel.length; i++) {
            scheduleTemplate[i][0] = dsChannel[i].trim();
            scheduleTemplate[i][1] = dsTime[i].trim();
            channleShdual.put(scheduleTemplate[i][0], scheduleTemplate[i][1]);
        }
        String[] dsName = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.ds-registered.ds&name"), "#$");
        String[] dsClassName = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.ds-registered.ds&class-name"), "#$");
        String[] dsPropertyJsp = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.ds-registered.ds&property-jsp"), "#$");
        String[] dsDescription = Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.ds-registered.ds&description"), "#$");
        for (int i = 0; dsName != null && i < dsName.length; i++) {
            if (dses == null) {
                dses = new Hashtable();
            }
            DS ds = new DS();
            ds.setName(dsName[i].trim());
            ds.setClassName(dsClassName[i].trim());
            ds.setPropertyJsp(dsPropertyJsp[i].trim());
            ds.setDescription(dsDescription[i].trim());
            dses.put(dsName[i].trim(), ds);
        }
    }

    /**
	 * �õ�Ftp Server IP
	 * @param	siteId վ��ID
	 * @return	String Ftp Server IP
	 */
    public String getSynFtpServer(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".ftp.server");
    }

    /**
	 * �õ�Ftp Server �˿�
	 * @param	siteId վ��ID
	 * @return	String Ftp Server �˿�
	 */
    public int getSynFtpPort(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        String result = cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".ftp.port");
        if (result == null || result.trim().equals("")) {
            result = "21";
        }
        return Integer.parseInt(result);
    }

    /**
	 * �õ�Ftp Server �û�
	 * @param	siteId վ��ID
	 * @return	String Ftp Server �û�
	 */
    public String getSynFtpLoginUser(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".ftp.username");
    }

    /**
	 * �õ�Ftp Server �û�����
	 * @param	siteId վ��ID
	 * @return	String Ftp Server �û�����
	 */
    public String getSynFtpPassword(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".ftp.password");
    }

    /**
	 * �õ�Ftp Server Ŀ��·��
	 * @param	siteId վ��ID
	 * @return	String Ftp Server Ŀ��·��
	 */
    public String getSynFtpRootPath(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".ftp.path");
    }

    /**
	 * �õ�ͬ������
	 * @param	siteId վ��ID
	 * @return	String ͬ������
	 */
    public String getSynInfoplatExportClassName(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".infoplat-export.class-name");
    }

    /**
	 * �õ�����ҵ�����ݵ�����
	 * @param	siteId վ��ID
	 * @return	String ����ҵ�����ݵ�����
	 */
    public String getSynOtherExportClassName(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".other-export.class-name");
    }

    /**
	 * �õ�ͬ����ݵı���
	 * @param	siteId վ��ID
	 * @return	String ��ݵı���
	 */
    public String[] getSynInfoplatExportTables(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".infoplat-export.export-tables"), ";");
    }

    /**
	 * �õ������������
	 * @param	siteId վ��ID
	 * @return	String �����������
	 */
    public String[] getSynInfoplatExportCondition(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".infoplat-export.export-condition"), ";");
    }

    /**
	 * �õ�Ƶ������ʱ���
	 * @return	Hashtable Ƶ�������б�
	 */
    public Hashtable getChannelSchedule() {
        return channleShdual;
    }

    /**
	 * �õ�����ҵ�����ݵ�������
	 * @param	siteId վ��ID
	 * @return	String ��ݵı���
	 */
    public String[] getSynOtherExportTables(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".other-export.export-tables"), ";");
    }

    /**
	 * �õ�����ҵ�����ݵ�������
	 * @param	siteId վ��ID
	 * @return	String ��ݵ�������
	 */
    public String[] getSynOtherExportCondition(int siteId) {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile(Const.CONFIG_FILE);
        return Function.stringToArray(cesGlobals.getCesXmlProperty("platform.infoplat.site-syn.site-" + siteId + ".other-export.export-condition"), ";");
    }

    /**
	 * �õ����ڸ�ʽ
	 * @return	String ���ڸ�ʽ
	 */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
	 * �õ�����ʱ���ʽ
	 * @return	String ����ʱ���ʽ
	 */
    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
	 * һ���Get����
	 */
    public Hashtable getFtDocParser() {
        return ftDocParser;
    }

    /**
	 * һ���Get����
	 */
    public String getInfoplatDataDir() {
        return infoplatDataDir;
    }

    /**
	 * һ���Get����
	 */
    public String getInfoplatVersion() {
        return infoplatVersion;
    }

    /**
	 * һ���Get����
	 */
    public String getPlatformDataDir() {
        return platformDataDir;
    }

    /**
	 * һ���Get����
	 */
    public String getPoolName() {
        return poolName;
    }

    /**
	 * һ���Get����
	 */
    public boolean isWfIncrementIndex() {
        return wfIncrementIndex;
    }

    /**
	 * һ���Get����
	 */
    public String getWfIndexDir() {
        return wfIndexDir;
    }

    /**
	 * һ���Get����
	 */
    public String getAppServer() {
        return appServer;
    }

    /**
	 * һ���Get����
	 */
    public boolean isCookieParam() {
        return cookieParam;
    }

    /**
	 * һ���Get����
	 */
    public boolean isFormParam() {
        return formParam;
    }

    /**
	 * һ���Get����
	 */
    public boolean isIncludeParam() {
        return includeParam;
    }

    /**
	 * һ���Get����
	 */
    public String getChannelAuthority() {
        return channelAuthority;
    }

    /**
	 * һ���Get����
	 */
    public boolean isDocTypeCollectAuthority() {
        return docTypeCollectAuthority;
    }

    public boolean isDoclistUsercontrol() {
        return doclistUsercontrol;
    }

    /**
	 * һ���Get����
	 */
    public String getOsType() {
        return osType;
    }

    /**
	 * һ���Get����
	 */
    public boolean isSessionParam() {
        return sessionParam;
    }

    /**
	 * һ���Get����
	 */
    public boolean isUrlParam() {
        return urlParam;
    }

    /**
	 * һ���Get����
	 */
    public boolean isDocPublishParent() {
        return docPublishParent;
    }

    /**
	 * һ���Get����
	 */
    public String getWatermarkAlign() {
        return watermarkAlign;
    }

    /**
	 * һ���Get����
	 */
    public String getWatermarkFontcolor() {
        return watermarkFontcolor;
    }

    /**
	 * һ���Get����
	 */
    public String getWatermarkFontfamily() {
        return watermarkFontfamily;
    }

    /**
	 * һ���Get����
	 */
    public String getWatermarkFontsize() {
        return watermarkFontsize;
    }

    /**
	 * һ���Get����
	 */
    public boolean isWatermarkPrint() {
        return watermarkPrint;
    }

    /**
	 * һ���Get����
	 */
    public String getWatermarkTitle() {
        return watermarkTitle;
    }

    /**
	 * һ���Get����
	 */
    public String[] getImageFileSuffix() {
        return imageFileSuffix;
    }

    /**
	 * һ���Get����
	 */
    public int getImageMinHeight() {
        return imageMinHeight;
    }

    /**
	 * һ���Get����
	 */
    public int getImageMinWidth() {
        return imageMinWidth;
    }

    /**
	 * һ���Get����
	 */
    public String getInfoplatDataDirContext() {
        return infoplatDataDirContext;
    }

    /**
	 * һ���Get����
	 */
    public String[][] getChanTemplate() {
        return chanTemplate;
    }

    /**
	 * һ���Get����
	 */
    public String[][] getDocTemplate() {
        return docTemplate;
    }

    /**
	 * һ���Get����
	 */
    public String[][] getChanDsTemplate() {
        return chanDsTemplate;
    }

    /**
	 * һ���Get����
	 */
    public String[][] getDocDsTemplate() {
        return docDsTemplate;
    }

    /**
	 * һ���Get����
	 */
    public Hashtable getDses() {
        return dses;
    }

    /**
	 * һ���Get����
	 */
    public void setDses(Hashtable dses) {
        this.dses = dses;
    }

    /**
	 * һ���Get����
	 */
    public Hashtable getTemplateTypeAndName() {
        return templateTypeAndName;
    }

    /**
	 * һ���Get����
	 */
    public boolean isFormbeanParam() {
        return formbeanParam;
    }

    /**
	 * һ���Get����
	 */
    public String[] getServerAccessList() {
        return serverAccessList;
    }

    public static void main(String[] args) {
        System.out.println(ConfigInfo.getInstance().infoplatDataDirContext);
    }
}

package ces.platform.infoplat.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import ces.coral.dbo.DBOperation;
import ces.coral.file.FileOperation;
import ces.coral.lang.StringUtil;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.dao.ChannelDAO;
import ces.platform.infoplat.core.dao.DocumentPublishDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.service.syn.SiteSyn;
import ces.platform.infoplat.ui.website.defaultvalue.ChannelPath;
import ces.platform.infoplat.utils.Function;
import ces.platform.infoplat.utils.http.GenHtml;
import ces.platform.system.common.IdGenerator;

/**
 *  <p>
 *  Title: ��Ϣƽ̨2.5��Ƶ����</p> <p>
 *  Description: </p> <p>
 *  Copyright: Copyright (c) 2004 </p> <p>
 *  Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 *@Title      ��Ϣƽ̨
 *@Company    �Ϻ�������Ϣ���޹�˾
 *@version    2.5
 *@author     ���� ֣��ǿ
 *@created    2004��2��12��
 */
public class Channel extends TreeNode {

    public static final String DATA_RELATIVE = "channelpathrelating";

    public static final String PAGE_RELATIVE = "relative_page";

    public static final String REFRESH_NEVER = "9";

    public static final String REFRESH_RIGHT_NOW = "8";

    public static final String REFRESH_FOREVER = "2";

    public static final String REFRESH_YES = "1";

    public static final String REFRESH_NO = "0";

    private int channelID;

    private String name;

    private String desc;

    private String asciiName;

    private int siteId;

    private String siteAsciiName;

    private String channelType;

    private String dataUrl;

    private String useStatus;

    private int orderNo;

    private String style;

    private int creator;

    private Date createDate;

    private String templateId;

    private String templateName;

    private String refresh;

    private String completePath;

    private String defineFilePath;

    private String defineFileName;

    private String defineFileContent;

    private String resBaseURL;

    private int pageNum;

    private List selfDefineList;

    private String extendParent;

    /**
     * @return Returns the selfDefineList.
     */
    public List getSelfDefineList() {
        return selfDefineList;
    }

    /**
     * @param selfDefineList The selfDefineList to set.
     */
    public void setSelfDefineList(List selfDefineList) {
        this.selfDefineList = selfDefineList;
    }

    /**
     * @return Returns the extendParent.
     */
    public String getExtendParent() {
        return extendParent;
    }

    /**
     * @param extendParent The extendParent to set.
     */
    public void setExtendParent(String extendParent) {
        this.extendParent = extendParent;
    }

    public Channel() {
    }

    public Channel(int id) {
        this.channelID = id;
    }

    /**
	 * �ѵ�ǰƵ�����뵽��ݿ���
	 * @throws java.lang.Exception
	 */
    public void add(boolean createFolder) throws Exception {
        if (this.getType() == null || this.getType().trim().equals("")) {
            this.setType("chan");
        }
        super.add();
        new ChannelDAO().add(this, createFolder);
    }

    /**
	 * �ѵ�ǰƵ�����µ���ݿ���
	 * @throws java.lang.Exception
	 */
    public void update() throws Exception {
        super.update();
        new ChannelDAO().update(this);
    }

    /**
	 * �ѵ�ǰƵ��ɾ��
	 * @throws java.lang.Exception
	 */
    public void delete(boolean delFolder, boolean delDoc) throws Exception {
        ChannelDAO channelDao = new ChannelDAO();
        DocumentPublish[] docPubs = null;
        DocumentCBF[] docCbfs = null;
        Channel channel = null;
        int i;
        if (!delDoc) {
            docPubs = Documents.getPublishedList(this.getPath(), 0);
            if (null != docPubs && docPubs.length > 0) {
                String sExc = "Ƶ�� " + this.getName() + " �����з����ĵ���";
                for (i = 0; i < docPubs.length; i++) {
                    sExc += docPubs[i].getTitle() + " ";
                }
                throw new Exception(sExc);
            }
            docCbfs = Documents.getDocsByChanPath(this.getPath(), false);
            if (null != docCbfs && docCbfs.length > 0) {
                String sExc = "Ƶ�� " + this.getName() + " �����ĵ���";
                for (i = 0; i < docCbfs.length; i++) {
                    sExc += docCbfs[i].getTitle() + " ";
                }
                String[] chanPath = Documents.getPublishedChannelPaths(docCbfs);
                if (null != chanPath && chanPath.length > 0) {
                    sExc += " ��������Ƶ�� ";
                    for (i = 0; i < chanPath.length; i++) {
                        channel = channelDao.getInstanceByPath(chanPath[i]);
                        if (null != channel) {
                            sExc += channel.getName() + " ";
                        }
                    }
                }
                throw new Exception(sExc);
            }
        }
        TreeNode[] tns = this.getList();
        if (tns != null) {
            for (i = 0; i < tns.length; i++) {
                channel = (Channel) tns[i];
                channel.delete(delFolder, delDoc);
            }
        }
        SiteChannelDocTypeRelation.deleteDocTypes(this.getPath());
        channelDao.delete(this, delFolder);
        super.delete();
    }

    /**
	 * Ƶ��ͬ��������
	 * @throws java.lang.Exception
	 */
    public void synOutSite() throws Exception {
        ConfigInfo ci = ConfigInfo.getInstance();
        SiteSyn siteSyn = new SiteSyn();
        siteSyn.setFtpLoginUser(ci.getSynFtpLoginUser(siteId));
        siteSyn.setFtpPassword(ci.getSynFtpPassword(siteId));
        siteSyn.setFtpPort(ci.getSynFtpPort(siteId));
        siteSyn.setFtpServerIp(ci.getSynFtpServer(siteId));
        siteSyn.setFtpRootPath(ci.getSynFtpRootPath(siteId));
        siteSyn.setSynInfoplatExportClassName(ci.getSynInfoplatExportClassName(siteId));
        siteSyn.setSynOtherExportClassName(ci.getSynOtherExportClassName(siteId));
        boolean isIncSyn = true;
        siteSyn.toOutSite(getPath(), isIncSyn);
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getAsciiName() {
        return asciiName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getChannelID() {
        return channelID;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getCreateDate() {
        return createDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getCreator() {
        return creator;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDataUrl() {
        return dataUrl;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDesc() {
        return desc;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getName() {
        return name;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getOrderNo() {
        return orderNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getSiteId() {
        return siteId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getStyle() {
        return style;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getTemplateId() {
        return templateId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getPageNum() {
        return pageNum;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setAsciiName(String asciiName) {
        this.asciiName = asciiName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setCreator(int creator) {
        this.creator = creator;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDataUrl(String dataUrl) {
        this.dataUrl = dataUrl;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getChannelType() {
        return channelType;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getUseStatus() {
        return useStatus;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setUseStatus(String useStatus) {
        this.useStatus = useStatus;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public boolean isRefresh() {
        if (this.refresh == null) {
            this.refresh = "0";
        }
        return refresh.equalsIgnoreCase("1") || refresh.equalsIgnoreCase("2");
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getRefresh() {
        if (this.refresh == null) {
            this.refresh = "0";
        }
        return refresh;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setRefresh(boolean b) {
        refresh = b ? "1" : "0";
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setRefresh(String b) {
        refresh = b;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getTemplateName() {
        return templateName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getCompletePath() throws Exception {
        if (this.completePath == null) {
            this.completePath = cfg.getInfoplatDataDir() + "pub/" + this.getSiteAsciiName() + "/" + this.getAsciiName();
        }
        return completePath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setCompletePath(String completePath) {
        this.completePath = completePath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDefineFileContent() {
        (new ChannelDAO()).readDefineFile(this);
        return defineFileContent;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDefineFileContent(String defineFileContent) {
        this.defineFileContent = defineFileContent;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDefineFileName(String defineFileName) {
        this.defineFileName = defineFileName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDefineFileName() {
        if (this.defineFileName == null) {
            this.defineFileName = "index_d.html";
        }
        return defineFileName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDefineFilePath() throws Exception {
        if (this.defineFilePath == null) {
            this.defineFilePath = cfg.getInfoplatDataDir() + "pub/" + this.getSiteAsciiName() + "/" + this.getAsciiName() + "/";
        }
        return defineFilePath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDefineFilePath(String defineFilePath) {
        this.defineFilePath = defineFilePath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setResBaseURL(String resBaseURL) {
        this.resBaseURL = resBaseURL;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getResBaseURL() throws Exception {
        if (this.resBaseURL == null) {
            this.resBaseURL = cfg.getInfoplatDataDirContext() + "pub/" + this.getSiteAsciiName() + "/" + this.getAsciiName() + "/";
        }
        return resBaseURL;
    }

    /**
	 * ���¶����ļ���������Ϣ��������
	 * @param htmContent
	 * @return
	 * @throws java.lang.Exception
	 */
    public boolean updateDefineFile() throws Exception {
        (new ChannelDAO()).updateDefineFile(this, this.defineFileContent);
        return true;
    }

    /**
	 * �õ�CHDC �ļ�
	 * @return
	 * @throws java.lang.Exception
	 * @info bill added on 5.28
	 */
    public boolean getDCFile() throws Exception {
        boolean blStatic = true;
        String indexDCFilePathName = new StringBuffer().append(cfg.getInfoplatDataDir()).append("pub/").append(getSiteAsciiName()).append("/").append(asciiName).toString();
        String dcs = indexDCFilePathName + "/index_dcs.jsp";
        File dc = new File(dcs);
        if (!dc.isFile()) {
            String dcd = indexDCFilePathName + "/index_dcd.jsp";
            dc = new File(indexDCFilePathName);
            blStatic = false;
        }
        if (!dc.isFile()) {
            throw new Exception("��Ŀ¼" + indexDCFilePathName + "��û���ҵ�index_dcs.jsp �� index_dcd.jsp");
        }
        return blStatic;
    }

    /**
	 * ���CHP�ļ�
	 * @return
	 * @throws java.lang.Exception
	 * @info bill added on 5.28
	 */
    public boolean generatePrototypeFile() throws Exception {
        boolean blStatic = getDCFile();
        String siteAsciiName = getSiteAsciiName();
        String indexDcFileUrl = new StringBuffer().append(cfg.getInfoplatDataDirContext()).append("pub/").append(siteAsciiName).append("/").append(asciiName).append(blStatic ? "/index_dcs.jsp" : "/index_dcd.jsp").toString();
        indexDcFileUrl += "?channelPath=" + this.getPath();
        String indexPFilePathName = new StringBuffer().append(cfg.getInfoplatDataDir()).append("pub/").append(siteAsciiName).append("/").append(asciiName).toString();
        String p = indexPFilePathName + (blStatic ? "/index_ps.jsp" : "/index_pd.jsp");
        return new GenHtml().generateHtmlPage(indexDcFileUrl, p, true, true);
    }

    /**
	 * ���CHDC CHP�ļ�
	 * @return
	 * @throws java.lang.Exception
	 * @info bill modifid on 5.28
	 */
    public boolean genDCandPFile() throws Exception {
        boolean blStatic = (new ChannelDAO()).genDCFile(this);
        String siteAsciiName = getSiteAsciiName();
        String indexDFileUrl = cfg.getInfoplatDataDirContext() + "pub/" + siteAsciiName + "/" + asciiName + "/index_dc.jsp";
        indexDFileUrl += "?channelPath=" + this.getPath();
        String indexDCFilePathName = cfg.getInfoplatDataDir() + "pub/" + siteAsciiName + "/" + asciiName + "/" + (blStatic ? "index_ps.jsp" : "index_pd.jsp");
        try {
            new GenHtml().generateHtmlPage(indexDFileUrl, indexDCFilePathName, true);
        } catch (Exception ex) {
            log.error("ˢ��Ƶ��ҳ�棨HTML/JSP������");
            log.error(ex.toString());
        }
        return true;
    }

    /**
	 * ����CHDC�ļ�����CHDC�ļ���TAG�����õķ���
	 * �滻CHD�ļ��е����Դ���ñ�ǩ��PARAMS����ֵ����Ҫ������pathֵ
	 * @return
	 * @throws java.lang.Exception
	 */
    public boolean updateIndexDFile(HashMap hmParams) throws Exception {
        (new ChannelDAO()).updateIndexDFile(this, hmParams);
        return true;
    }

    /**
	 * վ��Ƶ����������
	 * ע�⣺��ȫ����ʱƵ�����в�ʵ�ַ����ĵ�����Ҫ�����ĵ����Ե���Site.publishDocuments()������
	 * @param blFull �Ƿ���ȫ����������ȫ�������ж��Ƿ���Ҫ������
	 */
    public void publish(boolean blFull) throws Exception {
        (new ChannelDAO()).publish(this, blFull);
    }

    /**
	 * վ��Ƶ����������
	 * ע�⣺��ȫ����ʱƵ�����в�ʵ�ַ����ĵ�����Ҫ�����ĵ����Ե���Site.publishDocuments()������
	 * ����ͬ��
	 * @param blFull �Ƿ���ȫ����������ȫ�������ж��Ƿ���Ҫ������
	 * @param isSyn  �Ƿ�ͬ��
	 */
    public void publish(boolean blFull, boolean isSyn) throws Exception {
        (new ChannelDAO()).publish(this, blFull, isSyn);
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getSiteAsciiName() throws Exception {
        if (siteAsciiName == null) {
            siteAsciiName = new ChannelDAO().getSiteAsciiName(this);
        }
        return siteAsciiName;
    }

    /**
	 * һ��get/set����
	 * @return
	 */
    public void setSiteAsciiName(String siteAsciiName) {
        this.siteAsciiName = siteAsciiName;
    }

    /**
	 * �õ�Ƶ���������url
	 * @return
	 */
    public String getPublishedURL() {
        String url = new StringBuffer("../").append(this.getAsciiName()).append("/").toString();
        return url;
    }

    /**
	 * �õ�Ƶ���������url
	 * ���ĵ�ҳ����ʹ��
	 * @return
	 */
    public String getPublishedURLForDoc() {
        String url = new StringBuffer("../../").append(this.getAsciiName()).append("/").toString();
        return url;
    }

    /**
	 * �õ�Ƶ��Ԥ��url
	 * @return
	 */
    public String getPreviewPublishedURL() {
        String url = null;
        try {
            url = cfg.getInfoplatDataDirContext() + new StringBuffer("pub/").append(this.getSiteAsciiName()).append("/").append(this.getAsciiName()).append("/").toString();
        } catch (Exception ex) {
        }
        return url;
    }

    /**
	 * �õ�Ƶ���ļ����ϼ�Ƶ��
	 * ��һ��Ƶ���������,indexΪ0���Ƕ���Ƶ��
	 * @return
	 */
    public Channel[] getSelfAndAncestors() {
        Channel[] channels = null;
        String channelPath = this.getPath();
        if (channelPath == null) {
            return null;
        }
        try {
            String sitePath = channelPath.substring(0, 10);
            channelPath = channelPath.substring(10);
            int amount = channelPath.length() / 5;
            channels = new Channel[amount];
            for (int i = 0; i < amount; i++) {
                Channel channel = (Channel) Channel.getInstance(sitePath + channelPath.substring(0, (i + 1) * 5));
                if (channel == null) {
                    return null;
                }
                channels[i] = channel;
            }
        } catch (Exception e) {
            log.error(" �õ�Ƶ���ļ����ϼ�Ƶ������", e);
        }
        return channels;
    }

    /**
	 * �õ�Ƶ���ļ����ϼ�Ƶ������ư�����ķָ��
	 * @return
	 */
    public String getSelfAndAncestors(String channelPath, String sep) {
        String allPath = "";
        try {
            if (null == channelPath || "".equals(channelPath)) return "";
            allPath = new ChannelDAO().getSelfAndAncestors(channelPath, sep);
        } catch (Exception e) {
            log.error(" �õ�Ƶ���ļ����ϼ�Ƶ������", e);
        }
        return allPath;
    }

    /**
	 * ����Ƶ��ҳ��ˢ�·�����־
	 * @param blRefreshFlag
	 * @info 20041007 bill changed the mechanism of the method to escape recursive update
	 * cos the rule wether updating should be determined by higher method
	 */
    public void updateRefreshFlag(boolean blRefreshFlag) {
        try {
            new ChannelDAO().updateRefreshFlag(this, blRefreshFlag);
        } catch (Exception ex) {
            log.error("����Ƶ��ҳ��ˢ�±�־���?", ex);
        }
    }

    /**
	 * �õ�Ƶ���ܵķ�����
	 */
    public int getPublishAmount(String timeOffset) throws Exception {
        return new DocumentPublishDAO().getPublishedAmount(this.getPath(), timeOffset);
    }

    /**
	 * ���վ��Ƶ�����ƺ���
	 * @param orgChanPath	����ԭƵ��Path
	 * @param aimChanPath	����Ŀ��Ƶ��Path
	 */
    public void copy(String orgChanPath, String aimChanPath) throws Exception {
        ChannelDAO channelDao = new ChannelDAO();
        Channel orgChannel = channelDao.getInstanceByPath(orgChanPath);
        Channel aimChannel = copySingle(orgChannel, aimChanPath);
        List list = channelDao.getChannelsList(orgChanPath);
        if (null != list && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                copy(((Channel) list.get(i)).getPath(), aimChannel.getPath());
            }
        }
    }

    /**
	 * ɾ��Ƶ���µ��ĵ�
	 * @param blRefreshFlag
	 */
    public void delChannelDoc() throws Exception {
        new ChannelDAO().delChannelDoc(this);
    }

    /**
	 * ����վ��Ƶ�����ƺ���
	 * @param orgChannel	����ԭƵ������
	 * @param aimChanPath	����Ŀ��Ƶ��Path
	 */
    private Channel copySingle(Channel orgChannel, String aimChanPath) throws Exception {
        Channel channel = new Channel();
        ChannelPath channelPath = new ChannelPath();
        channelPath.setCPath(aimChanPath);
        String treeId = channelPath.getDefaultValue();
        channel.setPath(aimChanPath + treeId);
        int level = aimChanPath.length() / 5;
        int channelId = (int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_CHANNEL);
        String parentId = aimChanPath.substring((level - 1) * 5, aimChanPath.length());
        String sitePath = aimChanPath.substring(0, 10);
        int siteID = ((Site) TreeNode.getInstance(sitePath)).getSiteID();
        channel.setSiteId(siteID);
        channel.setId(treeId);
        channel.setLevel(level);
        channel.setChannelID(channelId);
        channel.setParentId(parentId);
        channel.setChannelType(orgChannel.getChannelType());
        channel.setName(orgChannel.getName() + channel.getChannelID());
        channel.setTitle(orgChannel.getName() + channel.getChannelID());
        channel.setDescription(orgChannel.getDesc());
        channel.setTemplateId(orgChannel.getTemplateId());
        channel.setOrderNo(orgChannel.getOrderNo());
        channel.setUseStatus(orgChannel.getUseStatus());
        channel.setRefPath(REFRESH_RIGHT_NOW);
        channel.setRefresh(REFRESH_RIGHT_NOW);
        channel.setPageNum(orgChannel.getPageNum());
        String orgName = orgChannel.getAsciiName();
        if (orgName.indexOf("_") > -1) orgName = orgName.substring(0, orgName.indexOf("_"));
        channel.setAsciiName(orgName + "_" + channel.getChannelID());
        channel.setSelfDefineList(new ChannelDAO().getSelfDefine(orgChannel.getPath()));
        channel.add(true);
        channel = (Channel) TreeNode.getInstance(channel.getPath());
        HashMap result = SiteChannelDocTypeRelation.getDocTypePathsAndShowTemplateIds(orgChannel.getPath(), false);
        if (null != result) {
            Iterator iterator = result.keySet().iterator();
            String docType, tempId;
            ArrayList docTypes = new ArrayList();
            ArrayList tempIds = new ArrayList();
            while (iterator.hasNext()) {
                docType = (String) iterator.next();
                tempId = (String) result.get(docType);
                if ("null".equals(tempId)) tempId = "";
                docTypes.add(docType);
                tempIds.add(tempId);
            }
            SiteChannelDocTypeRelation.addBySiteChannelPath(channel.getPath(), (String[]) docTypes.toArray(new String[] {}), (String[]) tempIds.toArray(new String[] {}));
        }
        ConfigInfo cfg = ConfigInfo.getInstance();
        String orgFileUrl = cfg.getInfoplatDataDir() + "pub" + File.separator + orgChannel.getSiteAsciiName() + File.separator + orgChannel.getAsciiName() + File.separator;
        String aimFileUrl = cfg.getInfoplatDataDir() + "pub" + File.separator + channel.getSiteAsciiName() + File.separator + channel.getAsciiName() + File.separator;
        FileOperation.copyDir(orgFileUrl, aimFileUrl);
        BufferedReader reader;
        String line, documents;
        StringBuffer document;
        File psFile = new File(aimFileUrl + "index_ps.jsp");
        if (psFile.exists()) {
            reader = new BufferedReader(new FileReader(psFile));
            line = null;
            document = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                document.append(line + "\n");
            }
            documents = StringUtil.replaceAll(document.toString(), orgChannel.getPath(), channel.getPath());
            Function.writeTextFile(documents, aimFileUrl + "index_ps.jsp", "GBK", true);
            reader.close();
        }
        File pdFile = new File(aimFileUrl + "index_pd.jsp");
        if (pdFile.exists()) {
            reader = new BufferedReader(new FileReader(pdFile));
            line = null;
            document = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                document.append(line + "\n");
            }
            documents = StringUtil.replaceAll(document.toString(), orgChannel.getPath(), channel.getPath());
            Function.writeTextFile(documents, aimFileUrl + "index_pd.jsp", "GBK", true);
            reader.close();
        }
        return channel;
    }

    public static void main(String[] args) {
        try {
            Channel channel = new Channel();
            channel.copy("000000070201702", "0000000702018020090200802");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

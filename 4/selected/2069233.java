package ces.platform.infoplat.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ces.coral.lang.StringUtil;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.dao.DocumentCBFDAO;
import ces.platform.infoplat.core.dao.DocumentPublishDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.service.indexserver.parser.html.HtmlParser;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.facade.OrgUser;

/**
 * <p>
 * Title: ������Ϣƽ̨
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: �Ϻ�������Ϣ��չ���޹�˾
 * </p>
 *
 * @author ���� ֣��ǿ
 * @version 2.5
 */
public class DocumentPublish extends Document {

    private int publisher;

    private String publisherName;

    private Date publishDate;

    private int orderNo;

    private Date validStartDate;

    private Date validEndDate;

    private boolean synStatus = false;

    private String siteAsciiName;

    private String channelAsciiName;

    private String docTypeName;

    private String published_channel_count;

    private String published_channel;

    private String full_path;

    public DocumentPublish() {
    }

    /**
	 * ���һ��DocumentCBF����������һ��DocumentPublish����
	 *
	 * @param doc
	 */
    public DocumentPublish(DocumentCBF doc, String publishPath, int publisher, Date publishDate, Date validStartDate, Date validEndDate, int orderNo) {
        this.id = doc.id;
        this.abstractWords = doc.getAbstractWords();
        this.actyInstId = doc.getActyInstId();
        this.attachStatus = doc.getAttachStatus();
        this.author = doc.getAuthor();
        this.channelPath = publishPath;
        this.contentFile = doc.getContentFile();
        this.createDate = doc.getCreateDate();
        this.creater = doc.getCreater();
        this.doctypePath = doc.getDoctypePath();
        this.editorRemark = doc.getEditorRemark();
        this.emitDate = doc.getEmitDate();
        this.emitUnit = doc.getEmitUnit();
        this.hyperlink = doc.getHyperlink();
        this.keywords = doc.getKeywords();
        this.notes = doc.getNotes();
        this.orderNo = orderNo;
        this.periodicalNo = doc.getPeriodicalNo();
        this.pertinentWords = doc.getPertinentWords();
        this.publishDate = publishDate;
        this.publisher = publisher;
        this.remarkProp = doc.getRemarkProp();
        this.remarkPropName = doc.getRemarkPropName();
        this.reservation1 = doc.getReservation1();
        this.reservation2 = doc.getReservation2();
        this.reservation3 = doc.getReservation3();
        this.reservation4 = doc.getReservation4();
        this.reservation5 = doc.getReservation5();
        this.reservation6 = doc.getReservation6();
        this.securityLevelId = doc.getSecurityLevelId();
        this.securityLevelName = doc.getSecurityLevelName();
        this.sourceId = doc.getSourceId();
        this.subTitle = doc.getSubTitle();
        this.title = doc.getTitle();
        this.titleColor = doc.getTitleColor();
        this.validEndDate = validEndDate;
        this.validStartDate = validStartDate;
        this.wordNo = doc.getWordNo();
        this.yearNo = doc.getYearNo();
        this.createrName = doc.getCreaterName();
    }

    /**
	 * �õ�һ���Ѿ��������ĵ�ʵ��
	 *
	 * @param docID
	 * @param channelPath
	 * @return @throws
	 *         java.lang.Exception
	 */
    public static DocumentPublish getInstance(String channelPath, int docId) throws Exception {
        return new DocumentPublishDAO().getInstance(docId, channelPath);
    }

    /**
	 * �����ĵ�������ǰ�Ķ����е���ݱ��浽��ݿ���(�½�����,����һ�������ĵ�)
	 */
    public void add() throws Exception {
        new DocumentPublishDAO().add(this);
    }

    /**
	 * ����������ݵ�����
	 * @return
	 * @throws java.lang.Exception
	 */
    public String getContentLength() {
        String docContent = "";
        String strFileName = "";
        File file = null;
        if (strFileName.equals("")) {
            strFileName = new DocumentPublishDAO().getContentName(this);
        }
        try {
            file = new File(strFileName);
            if (file.exists()) {
                HtmlParser parser = new HtmlParser();
                docContent = parser.getFileContent(strFileName);
                if (docContent == null) {
                    docContent = "";
                }
            }
            docContent = StringUtil.replaceAll(docContent, " ", "");
            return "" + docContent.length();
        } catch (Exception ex3) {
            log.error("�����ļ���ֵ", ex3);
            return null;
        }
    }

    /**
	 * ɾ��һ���Ѿ��������ĵ�
	 */
    public void delete() throws Exception {
        new DocumentPublishDAO().delete(this);
    }

    /**
	 * �õ����ĵ��ĸ����б�
	 */
    public DocAffix[] getAffixList() throws Exception {
        return new DocumentCBFDAO().getAffixList(this.id);
    }

    /**
	 * �õ����ĵ���ĳһ������Ϣ
	 *
	 * @param affixID
	 */
    public DocAffix getAffix(int affixId) throws Exception {
        return (DocAffix) DocResource.getInstance(affixId);
    }

    /**
	 * �õ����ĵ�������ͼƬ��Դ
	 */
    public DocPicture[] getPictureList() throws Exception {
        return new DocumentCBFDAO().getPictureList(this.id);
    }

    /**
	 * Ϊ���ĵ���ĳ��ͼƬ��Դ
	 *
	 * @param pictureID
	 */
    public DocPicture getPicture(int pictureId) throws Exception {
        return (DocPicture) DocResource.getInstance(pictureId);
    }

    /**
	 * �õ����ĵ�������������Դ
	 */
    public DocContentResource[] getDocContentResourceList() throws Exception {
        return new DocumentCBFDAO().getDocContentResourceList(this.id);
    }

    /**
	 * �õ����ĵ�������ĵ��б�
	 */
    public Document[] getCorrelationDocList() throws Exception {
        return new DocumentPublishDAO().getCorrelationDocList(this.id, this.siteAsciiName);
    }

    /**
	 * �õ������ĵ�ѡ������ĵ����б�
	  ��ΪҪ���Ѿ����ĵ��ӹ�ѡ���б��й��˵�,���԰��Ѿ�ѡ����ĵ�����
	 */
    public Document[] getCorrelationList(int[] selectionDocIds) throws Exception {
        return Documents.getMainList(this.getPertinentWords(), selectionDocIds);
    }

    /**
	 * �õ��Ѿ��������ڿ��ĵ����ĵ��б�
	 *
	 * @return @throws
	 *         java.lang.Exception
	 */
    public DocumentPublish[] getMagazineList() throws Exception {
        DocumentCBF[] docCBF = new DocumentCBFDAO().getMagazineList(id, null);
        if (docCBF == null || docCBF.length == 0) {
            return null;
        }
        DocumentPublish[] result = new DocumentPublish[docCBF.length];
        for (int i = 0; i < docCBF.length; i++) {
            result[i] = new DocumentPublish(docCBF[i], this.getChannelPath(), this.getPublisher(), this.publishDate, this.validStartDate, this.validEndDate, this.orderNo);
        }
        return result;
    }

    /**
	 * �õ��ڿ���,Ҫ�õ�һ���ڿ��ĵ��������е����ĵ�,�õ��ݹ� @throws
	  java.lang.Exception
	 */
    private void getMagazineTree(DocumentPublish doc, List treeList) throws Exception {
        DocumentCBF[] magazine = DocumentCBF.getInstance(doc.id).getMagazineList(null);
        for (int i = 0; magazine != null && magazine.length > 0 && i < magazine.length; i++) {
            DocumentPublish tmp = new DocumentPublish(magazine[i], doc.getChannelPath(), doc.getPublisher(), doc.getPublishDate(), doc.getValidStartDate(), doc.getValidEndDate(), doc.getOrderNo());
            treeList.add(tmp);
        }
        treeList.add(this);
    }

    /**
	 * �õ��ڿ���
	 *
	 * @throws java.lang.Exception
	 */
    public List getMagazineTree() throws Exception {
        List result = new ArrayList();
        getMagazineTree(this, result);
        return result;
    }

    /**
	 * �ĵ���������
	 *
	 * @param isBackProcess
	 *            �Ƿ���˹�����
	 * @throws java.lang.Exception
	 */
    public void unPublish(String userId, boolean isBackProcess) throws Exception {
        new DocumentPublishDAO().unPublish(this, userId, isBackProcess);
    }

    /**
	 * ���·���
	 * @param userId String
	 * @param isBackProcess boolean
	 * @throws Exception
	 */
    public void rePublish() throws Exception {
        if (this.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD)) {
            DocumentPublish[] adp = getMagazineList();
            if (adp != null && adp.length > 0) {
                for (int i = 0; i < adp.length; i++) adp[i].rePublish();
            }
        }
        new DocumentPublishDAO().rePublish(this);
    }

    /**
	 * �õ����ĵ�����������ͼ��Դ
	 */
    public DocPicture[] getBreviaryImageList() throws Exception {
        return new DocumentCBFDAO().getBreviaryImageList(this.id);
    }

    public String getPublisherName() {
        if (publisherName == null) {
            try {
                publisherName = new OrgUser().getUser(publisher).getUserName();
            } catch (Exception e) {
                log.error("�õ�publisherName���?");
            }
        }
        return this.publisherName;
    }

    /**
	 * �ĵ�ת��
	 */
    public void transferPublish() {
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getPublishDate() {
        return publishDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getPublisher() {
        return publisher;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setPublisher(int publisher) {
        this.publisher = publisher;
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
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getValidEndDate() {
        return validEndDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getValidStartDate() {
        return validStartDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setValidEndDate(Date validEndDate) {
        this.validEndDate = validEndDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setValidStartDate(Date validStartDate) {
        this.validStartDate = validStartDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public boolean isSynStatus() {
        return synStatus;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setSynStatus(boolean synStatus) {
        this.synStatus = synStatus;
    }

    /**
	 * �����������
	 *
	 * @return @throws
	 *         java.lang.Exception
	 */
    public String getContent() {
        return new DocumentPublishDAO().getContent(this);
    }

    /**
	 * ��Ƶ���л���������� �����ڵ�ƪ�ĵ������
	 *
	 * @return @throws
	 *         java.lang.Exception
	 */
    public String getContentAtChannel() {
        String str = new DocumentPublishDAO().getContent(this);
        String imageFile = "d_" + this.id + ".files";
        String year = String.valueOf(this.getCreateDate().getYear() + 1900);
        String month = String.valueOf(this.getCreateDate().getMonth() + 1);
        if (month.length() == 1) {
            month = "0" + month;
        }
        String yyyyMM = year + month;
        str = StringUtil.replaceAll(str, "href=\"" + imageFile + "/", "href=\"../docs/" + yyyyMM + "/" + imageFile + "/");
        str = StringUtil.replaceAll(str, "href=" + imageFile + "/", "href=../docs/" + yyyyMM + "/" + imageFile + "/");
        str = StringUtil.replaceAll(str, "src=\"" + imageFile + "/", "src=\"../docs/" + yyyyMM + "/" + imageFile + "/");
        str = StringUtil.replaceAll(str, "src=" + imageFile + "/", "src=../docs/" + yyyyMM + "/" + imageFile + "/");
        str = StringUtil.replaceAll(str, "src=\"res/", "src=\"../docs/" + yyyyMM + "/res/");
        str = StringUtil.replaceAll(str, "src=res/", "src=../docs/" + yyyyMM + "/res/");
        return str;
    }

    /**
	 * ���÷���������
	 *
	 * @param publisherName
	 */
    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    /**
	 * �õ��ĵ��������url,��Ƶ��ģ��ר�õ�
	 *
	 * @return
	 */
    public String getPublishedURL() {
        String url = new StringBuffer("../docs/").append(Function.getNYofDate(this.getCreateDate())).append("/d_").append(this.id).append(".html").toString();
        return url;
    }

    /**
	 * �õ��ĵ��������url,���ĵ���ʾģ��ר�õ�
	 *
	 * @return
	 */
    public String getPublishedURLForDocTemplate() {
        String url = new StringBuffer("../").append(Function.getNYofDate(this.getCreateDate())).append("/d_").append(this.id).append(".html").toString();
        return url;
    }

    /**
	 * ȡ���ĵ������·��url
	 *
	 * @return
	 */
    public String getPreviewPublishedURL() {
        String url = new StringBuffer("../../../../platformData/infoplat/pub/").append(this.siteAsciiName).append("/docs/").append(Function.getNYofDate(this.getCreateDate())).append("/d_").append(this.id).append(".html").toString();
        return url;
    }

    /**
	 * �õ���Դ��url,����Ƶ��ģ��
	 *
	 * @return
	 */
    public String getPublishedResURL() {
        String url = new StringBuffer("../").append("docs/").append(Function.getNYofDate(this.getCreateDate())).append("/res/").toString();
        return url;
    }

    /**
	 * �õ���Դ��url,�����ĵ���ʾģ��
	 *
	 * @return
	 */
    public String getPublishedResURLForDocTemplate() {
        String url = new StringBuffer("./").append("res/").toString();
        return url;
    }

    /**
	 * �õ�Ƶ��Ӣ����
	 *
	 * @return
	 */
    public String getChannelAsciiName() {
        return channelAsciiName;
    }

    /**
	 * �õ�վ��Ӣ����
	 *
	 * @return
	 */
    public String getSiteAsciiName() {
        return siteAsciiName;
    }

    /**
	 * ����Ƶ��Ӣ����
	 *
	 * @param string
	 */
    public void setChannelAsciiName(String string) {
        channelAsciiName = string;
    }

    /**
	 * ����վ��Ӣ����
	 *
	 * @param string
	 */
    public void setSiteAsciiName(String string) {
        siteAsciiName = string;
    }

    /**
	 * �õ�������λ�� ���λ������һ��Ƶ���������,indexΪ0���Ƕ���Ƶ��
	  ��Ȼ�ĵ����Ա�����n��Ƶ��,����λ��ֻ��һ��,�����ֻ������
	 *
	 * @return
	 */
    public Channel[] getPublishedPosition() {
        Channel[] channels = null;
        String channelPath = this.getChannelPath();
        if (channelPath == null) {
            return null;
        }
        try {
            Channel channel = (Channel) Channel.getInstance(channelPath);
            channels = channel.getSelfAndAncestors();
        } catch (Exception e) {
            log.error(" �õ��ĵ�������λ�ó���", e);
        }
        return channels;
    }

    /**
	 * ɾ���ڿ�,Ҫ����ɾ��,����Ҫɾ�����������е����ĵ�,�õ��ݹ� @param userId
	 * ��ǰ��¼�û� @param date ��ǰ����ʱ��
	 * @throws java.lang.Exception
	 */
    private void unPublishMagazine(String userId, boolean isBackProcess, DocumentPublish doc) throws Exception {
        DocumentCBF[] magazine = DocumentCBF.getInstance(doc.id).getMagazineList(null);
        for (int i = 0; magazine != null && magazine.length > 0 && i < magazine.length; i++) {
            DocumentPublish tmp = new DocumentPublish(magazine[i], doc.getChannelPath(), doc.getPublisher(), doc.getPublishDate(), doc.getValidStartDate(), doc.getValidEndDate(), doc.getOrderNo());
            tmp.unPublishMagazine(userId, isBackProcess, tmp);
        }
        boolean bool = isBackProcess && doc.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD) && doc.getDoctypePath().length() == 15;
        unPublish(userId, bool);
    }

    /**
	 * �����ڿ�,�Ѹ��ڿ�������������ĵ�ȫ������
	 *
	 * @param userId
	 * @param date
	 * @throws java.lang.Exception
	 */
    public void unPublishMagazine(String userId, boolean isBackProcess) throws Exception {
        if (new DocumentPublishDAO().isMoreChannel(this)) {
            boolean bool = isBackProcess && getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD) && getDoctypePath().length() == 15;
            unPublish(userId, bool);
        } else {
            unPublishMagazine(userId, isBackProcess, this);
        }
    }

    /**
	 * �õ����ѷ����ĵ���ӦƵ����
	 * @return
	 */
    public String getCBFChannelName() {
        String result = "";
        try {
            DocumentCBF cbf = DocumentCBF.getInstance(this.id);
            if (cbf != null) {
                String channelPath = cbf.getChannelPath();
                Channel channel = (Channel) TreeNode.getInstance(channelPath);
                if (channel != null) {
                    result = channel.getName();
                }
            }
        } catch (Exception ex) {
            log.error("�õ��÷����ĵ��ĳ�ʼƵ�������ʧ�ܣ�");
        }
        return result;
    }

    /**
	 * �����ַ���
	 * @param str
	 * @param length
	 * @return
	 */
    public String limitString(String str, int length) {
        return Function.limitString(str, length);
    }

    /**
	 * ����ַ��Ȳ��Ƿ���ĳ�ַ����
	 * @param str
	 * @param length
	 * @param endStr
	 * @return
	 */
    public String limitString(String str, int length, String endStr) {
        return Function.limitString(str, length, endStr);
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDocTypeName() {
        return docTypeName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDocTypeName(String docTypeName) {
        this.docTypeName = docTypeName;
    }

    /**
     * @return Returns the full_path.
     */
    public String getFull_path() {
        return full_path;
    }

    /**
     * @param full_path The full_path to set.
     */
    public void setFull_path(String full_path) {
        this.full_path = full_path;
    }

    public void setPublished_channel(String published_channel) {
        this.published_channel = published_channel;
    }

    public void setPublished_channel_count(String published_channel_count) {
        this.published_channel_count = published_channel_count;
    }

    public String getPublished_channel() {
        if (null == published_channel || "".equals(published_channel) || "null".equalsIgnoreCase(published_channel)) new DocumentPublishDAO().setPublish_channel(this);
        return published_channel;
    }

    public String getPublished_channel_count() {
        if (null == published_channel_count || "".equals(published_channel_count) || "null".equalsIgnoreCase(published_channel_count)) new DocumentPublishDAO().setPublish_channel(this);
        return published_channel_count;
    }

    public static void main(String[] args) {
        try {
            DocumentPublish object = DocumentPublish.getInstance("000000070201802", 29002);
            object.rePublish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package ces.platform.infoplat.core;

import java.util.Date;
import java.io.File;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ces.coral.lang.StringUtil;
import ces.platform.infoplat.service.indexserver.parser.html.HtmlParser;
import ces.platform.infoplat.core.dao.DocResourceDAO;
import ces.platform.infoplat.core.dao.DocumentCBFDAO;
import ces.platform.infoplat.core.dao.DocumentsDAO;
import ces.platform.infoplat.utils.Function;
import ces.platform.infoplat.core.base.*;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class DocumentCBF extends Document {

    private Template template = null;

    public DocumentCBF() {
    }

    /**
	 * ��һ��DocumentBak�������һ��DocumentCBF�ɱ෢����
	 * @param doc
	 */
    public DocumentCBF(DocumentBak doc) {
        this.id = doc.id;
        this.abstractWords = doc.getAbstractWords();
        this.actyInstId = doc.getActyInstId();
        this.attachStatus = doc.getAttachStatus();
        this.author = doc.getAuthor();
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
        this.periodicalNo = doc.getPeriodicalNo();
        this.pertinentWords = doc.getPertinentWords();
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
        this.wordNo = doc.getWordNo();
        this.yearNo = doc.getYearNo();
    }

    /**
	 * �õ�һ���Ѿ����ڵ��ĵ�(���Ǵ���ݿ���ȡ)
	 * @param docID �ĵ�id
	 * @return
	 * @throws java.lang.Exception
	 */
    public static DocumentCBF getInstance(int docID) throws Exception {
        return new DocumentCBFDAO().getInstance(docID);
    }

    /**
	 * @param docId
	 * @return
	 */
    public static Document getInstance(String docId) throws Exception {
        int i = Integer.parseInt(docId);
        return new DocumentCBFDAO().getInstance(i);
    }

    /**
	 * �õ����ĵ��ĸ����б�
	 */
    public DocAffix[] getAffixList() throws Exception {
        return new DocumentCBFDAO().getAffixList(this.id);
    }

    /**
	 * �õ����ĵ���ĳһ������Ϣ
	 * @param affixID
	 */
    public DocAffix getAffix(int affixID) throws Exception {
        return (DocAffix) DocResource.getInstance(affixID);
    }

    /**
	 * �жϸ��ĵ��Ƿ���ͼƬ��Դ
	 * @return true ���� false ������
	 */
    public boolean isExistPicture() throws Exception {
        boolean bReturn = false;
        if (new DocumentCBFDAO().getPictureListNum(this.id) > 0) {
            bReturn = true;
        }
        return bReturn;
    }

    /**
	 * �õ����ĵ�������ͼƬ��Դ��
	 * @return int
	 */
    public int getPictureListNum() throws Exception {
        return new DocumentCBFDAO().getPictureListNum(this.id);
    }

    /**
	 * �õ����ĵ�������ͼƬ��Դ
	 */
    public DocPicture[] getPictureList() throws Exception {
        return new DocumentCBFDAO().getPictureList(this.id);
    }

    /**
	 * �õ����ĵ�����������ͼ��Դ
	 */
    public DocPicture[] getBreviaryImageList() throws Exception {
        return new DocumentCBFDAO().getBreviaryImageList(this.id);
    }

    /**
	 * Ϊ���ĵ���ĳ��ͼƬ��Դ
	 * @param pictureID
	 */
    public DocPicture getPicture(int pictureID) throws Exception {
        return (DocPicture) DocResource.getInstance(pictureID);
    }

    /**
	 * �õ����ĵ�������������Դ
	 */
    public DocContentResource[] getDocContentResourceList() throws Exception {
        return new DocumentCBFDAO().getDocContentResourceList(this.id);
    }

    /**
	 * �õ����ĵ�������ĵ��б�,�Ѿ�ѡ�������ĵ�
	 */
    public Document[] getCorrelationDocList() throws Exception {
        return new DocumentCBFDAO().getCorrelationDocList(this.id);
    }

    /**
	 * �õ������ĵ�ѡ������ĵ����б�
	 * ��ΪҪ���Ѿ����ĵ��ӹ�ѡ���б��й��˵�,���԰��Ѿ�ѡ����ĵ�����
	 */
    public Document[] getCorrelationList(int[] selectionDocIDs) throws Exception {
        return (DocumentCBF[]) new DocumentsDAO().getMainList(this.getPertinentWords(), selectionDocIDs);
    }

    /**
	 * �õ����ĵ�������ĵ��б�,�Ѿ�ѡ�������ĵ�
	 */
    public DocumentCBF[] getMagazineList(String condition) throws Exception {
        if (condition == null) {
            condition = "";
        }
        return new DocumentCBFDAO().getMagazineList(id, condition);
    }

    /**
	 * �����ĵ�������ǰ�Ķ����е���ݱ��浽��ݿ���(�½�����)
	 */
    public void add() throws Exception {
        this.lastestModifyDate = Function.getSysTime();
        new DocumentCBFDAO().add(this);
    }

    /**
	 * �����ĵ�������ǰ�Ķ����е���ݱ��浽��ݿ��У��޸ģ�
	 */
    public void update() throws Exception {
        this.lastestModifyDate = Function.getSysTime();
        new DocumentCBFDAO().update(this);
    }

    /**
	 * ɾ���ĵ�������վ<br>
	 * ˼·<br>
	   �Ȳ�ѯ���ĵ������е���Դ(����,ͼƬ,����ĵ�,��������)��id,<br>
	   ������DocResource��getInstance�෽����ȡ��Щ��Դ��ʵ��,Ȼ�������Դ��ʵ��deleteɾ����Դ<br>
	   ���ɾ���ĵ��������Ϣ<br>
	 * @param userId ɾ����
	 * @param date ɾ������
	 * @throws java.lang.Exception
	 */
    public void delete(int userId, Date date) throws Exception {
        delete(userId, date, false);
    }

    /**
	 * ɾ���ĵ�������վ<br>
	 * ˼·<br>
	   �Ȳ�ѯ���ĵ������е���Դ(����,ͼƬ,����ĵ�,��������)��id,<br>
	   ������DocResource��getInstance�෽����ȡ��Щ��Դ��ʵ��,Ȼ�������Դ��ʵ��deleteɾ����Դ<br>
	   ���ɾ���ĵ��������Ϣ<br>
	 * @param userId ɾ����
	 * @param date ɾ������
	 * @param hasWorkFlow �Ƿ���Ҫ��ͣ������,Ҳ���Ǵ��ĵ����޹�����
	 * @throws java.lang.Exception
	 */
    public void delete(int userId, Date date, boolean hasWorkFlow) throws Exception {
        new DocumentRecycle(this, userId, date).add();
        new DocumentCBFDAO().delete(userId, this, hasWorkFlow);
    }

    private void deleteMagazine(int userId, Date date, DocumentCBF doc) throws Exception {
        DocumentCBF[] magazine = doc.getMagazineList(null);
        for (int i = 0; magazine != null && magazine.length > 0 && i < magazine.length; i++) {
            magazine[i].deleteMagazine(userId, date, magazine[i]);
        }
        boolean isStopWorkflow = doc.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD);
        doc.delete(userId, date, isStopWorkflow && doc.getDoctypePath().length() == 15);
    }

    /**
	 * ɾ���ڿ�,��Ѹ��ڿ��������е����ĵ�ȫ��ɾ��
	 * @param userId
	 * @param date
	 * @throws java.lang.Exception
	 */
    public void deleteMagazine(int userId, Date date) throws Exception {
        deleteMagazine(userId, date, this);
        if (this.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD) && this.getDoctypePath().length() > 15) {
            String sCondition = " (URI = '" + this.getId() + "' " + " or URI like '" + this.getId() + ",%' " + " or URI like '%," + this.getId() + "' " + " or URI like '%," + this.getId() + ",%') " + " and TYPE = '" + DocResource.DOC_MAGAZINE_TYPE + "' ";
            DocResource[] docRes = new DocResourceDAO().getResourcesByCondition(sCondition);
            if (null != docRes && docRes.length > 0) {
                String sId = String.valueOf(this.getId());
                String sUri = "";
                for (int i = 0; i < docRes.length; i++) {
                    sUri = docRes[i].getUri();
                    if (sUri.equals(sId)) {
                        docRes[i].delete();
                        return;
                    } else if (sUri.startsWith(sId + ",")) {
                        sUri = sUri.substring(sId.length() + 1);
                    } else if (sUri.endsWith("," + sId)) {
                        sUri = sUri.substring(0, sUri.length() - sId.length() - 1);
                    } else {
                        sUri = sUri.substring(0, sUri.indexOf(sId) - 1) + sUri.substring(sUri.indexOf(sId) + sId.length());
                    }
                    docRes[i].setUri(sUri);
                    docRes[i].update();
                }
            }
        }
    }

    private void publishMagazine(String[] publishPaths, int publisher, Date publishDate, Date validStartDate, Date validEndDate, boolean isNextStep, DocumentCBF doc) throws Exception {
        DocumentCBF[] magazine = doc.getMagazineList(null);
        for (int i = 0; magazine != null && magazine.length > 0 && i < magazine.length; i++) {
            magazine[i].publishMagazine(publishPaths, publisher, publishDate, validStartDate, validEndDate, isNextStep, magazine[i]);
        }
        boolean bool = isNextStep && doc.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD);
        publish(publishPaths, publisher, publishDate, validStartDate, validEndDate, bool && doc.getDoctypePath().length() == 15);
    }

    /**
	 * �����ڿ�,�Ѹ��ڿ�������������ĵ�ȫ������
	 * @param userId
	 * @param date
	 * @throws java.lang.Exception
	 */
    public void publishMagazine(String[] publishPaths, int publisher, Date publishDate, Date validStartDate, Date validEndDate, boolean isNextStep) throws Exception {
        publishMagazine(publishPaths, publisher, publishDate, validStartDate, validEndDate, isNextStep, this);
    }

    /**
	 * �ĵ�����,������˼·����<br>
	 * 1.���ĵ���path���뵽��������<br>
	 * 2.���ж�̬ģ��,���html�ļ�
	 * 3.����Ƶ������״̬
	 * 4.ִ�й���������һ��(��ѡ)
	 * @param doc
	 * @param publishPaths
	 * @param isNextStep �Ƿ�ִ�й���������һ��,��Ϊ,���·�������Ҫִ�й���������һ��,������Ҫ�Ӹ�����
	 * @throws java.lang.Exception
	 */
    void publish(String[] publishPaths, int publisher, Date publishDate, Date validStartDate, Date validEndDate, boolean isNextStep) throws Exception {
        new DocumentCBFDAO().publish(this, publishPaths, publisher, publishDate, validStartDate, validEndDate, isNextStep);
    }

    /**
	 * Ϊ���ĵ��ϴ�����
	 */
    public void uploadAffix(HttpServletRequest request, HttpServletResponse response) throws Exception {
        new DocumentCBFDAO().uploadAffix(this, request, response);
        new DocumentCBFDAO().updateLastModifyDate(this.id);
    }

    public String getUploadPath() throws Exception {
        return new DocumentCBFDAO().getUploadPath(this);
    }

    /**
	 * Ϊ���ĵ��ϴ�����FTP��ʽ
	 */
    public void uploadAffixWithFTP(int id, String[] fileProperties, String uri) throws Exception {
        new DocumentCBFDAO().uploadBigFile(id, fileProperties, uri);
        new DocumentCBFDAO().updateLastModifyDate(this.id);
    }

    /**
	 * Ϊ���ĵ��ϴ�ͼƬ��Դ
	 */
    public void uploadPicture(HttpServletRequest request, HttpServletResponse response) throws Exception {
        new DocumentCBFDAO().uploadPicture(this, request, response);
        new DocumentCBFDAO().updateLastModifyDate(this.id);
    }

    /**
	 * Ϊ���ĵ���������ĵ�
	 * @param docIDs
	 */
    public void addCorrelationDoc(int[] correlationDocIDs) throws Exception {
        new DocumentCBFDAO().addCorrelationDoc(this.id, correlationDocIDs);
        new DocumentCBFDAO().updateLastModifyDate(this.id);
    }

    /**
	 * Ϊ���ĵ������ڿ��ĵ�(���ĵ�)
	 * @param magazineIds
	 */
    public void addMagazine(int[] magazineIds) throws Exception {
        new DocumentCBFDAO().addMagazine(id, magazineIds);
    }

    /**
	 * ɾ����ĵ��Ĳ�������ĵ�
	 * @param docIDs
	 */
    public void delCorrelationDoc(int[] docIDs) throws Exception {
        new DocumentCBFDAO().delCorrelationDoc(this.id, docIDs);
        new DocumentCBFDAO().updateLastModifyDate(this.id);
    }

    /**
	 * �õ����ĵ��������б�
	 */
    public DocRemark[] getRemarkList() throws Exception {
        return null;
    }

    /**
	 * Ϊ���ĵ�������
	 */
    public void remark() throws Exception {
    }

    /**
	 * ��ȡ���ĵ�����ʷ������Ϣ�б�
	 * @param docID
	 * @return ����Ҳ���,����new DocApprove[0]
	 * @throws java.lang.Exception
	 */
    public DocApprove[] getHistoryApprove() throws Exception {
        return new DocumentCBFDAO().getHistoryApprove(this.id);
    }

    /**
	 * �õ���ƪ�ĵ������е���Դ��uri,��������ĵ�����<br>
	 * �˷��������ĵ�����ʱ,copy�ĵ���Դ������Ŀ¼��
	 * @return
	 */
    public String[] getAllResourceUri() throws Exception {
        return new DocumentCBFDAO().getAllResourceUri(this.id);
    }

    /**
	 * �õ���ƪ�ĵ������е���Դ����
	 * @return
	 */
    public DocResource[] getAllResources() throws Exception {
        return new DocumentCBFDAO().getAllResources(this.id);
    }

    /**
	 * �õ���ƪ�ĵ�����������ЩƵ����path��
	 * @return
	 * @throws java.lang.Exception
	 */
    public String[] getPublishedChannelPath() throws Exception {
        return new DocumentCBFDAO().getPublishedChannelPath(this.id);
    }

    /**
	 * ��һƪ�ĵ�������<br>
	 * 1.�����ĵ����������
	 * 2.������Դ
	 * @throws java.lang.Exception
	 */
    public void backup(int bakor, Date bakDate) throws Exception {
        new DocumentBak(this, bakor, bakDate).add();
    }

    /**
	 * �����ĵ���������
	 * @param docContent
	 * @throws java.lang.Exception
	 */
    public void saveContent(String docContent) throws Exception {
        new DocumentCBFDAO().saveContent(this, docContent);
    }

    /**
	 * �����ĵ���������(�Ƿ�Ϊͼ�Ļ�������)
	 * @param docContent
	 * @throws java.lang.Exception
	 */
    public void saveContent(String docContent, boolean blRte) throws Exception {
        new DocumentCBFDAO().saveContent(this, docContent, blRte);
    }

    /**
	 * �����������
	 * @return
	 * @throws java.lang.Exception
	 */
    public String getContent() {
        if (this.content == null) {
            this.content = new DocumentCBFDAO().getContent(this);
        }
        return this.content;
    }

    /**
	 * ����������ݵ�����
	 * @return
	 * @throws java.lang.Exception
	 */
    public String getContentLength() {
        String doccontent = "";
        File file = null;
        if (this.content == null) {
            this.content = new DocumentCBFDAO().getContentName(this);
        }
        try {
            file = new File(this.content);
            if (file.exists()) {
                HtmlParser parser = new HtmlParser();
                doccontent = parser.getFileContent(this.content);
                if (doccontent == null) {
                    doccontent = "";
                }
            }
            return "" + StringUtil.replaceAll(doccontent, " ", "").length();
        } catch (Exception ex3) {
            log.error("�����ļ���ֵ", ex3);
            return null;
        }
    }

    /**
	 * �ѱ��ĵ�����,���ƺ���ĵ�,title�ֶ�ǰ�����'(����)'������,�������<br>
	 * @param channelPath Ҫ���Ƶ��ĸ�Ƶ����(ֻ��ָ��һ��,����Ϊnull)
	 * @param docTypePath ���ƺ���ĵ�����path(ֻ��ָ��һ��)
	 * @param loginUserId ��ǰ��¼�û�
	 * @throws java.lang.Exception
	 */
    public void copyNew(String channelPath, String docTypePath, int loginUserId) throws Exception {
        DocumentCBFDAO dCBFDAO = new DocumentCBFDAO();
        DocumentCBF[] dCBF = null;
        if (this.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD)) dCBF = dCBFDAO.getMagazineList(this.getId(), null);
        dCBFDAO.copyNew(this, channelPath, docTypePath, loginUserId);
        if (dCBF != null && dCBF.length > 0) {
            int[] magazineIds = null;
            if (dCBF != null && dCBF.length > 0) {
                magazineIds = new int[dCBF.length];
                for (int i = 0; i < dCBF.length; i++) {
                    dCBFDAO.copyNewMagazine(dCBF[i], "", dCBF[i].getDoctypePath(), loginUserId);
                    magazineIds[i] = dCBF[i].getId();
                }
                dCBFDAO.setMagazine(this.getId(), magazineIds);
            }
        }
    }

    /**
	 * �����ĵ�������޸�ʱ��
	 */
    public void updateLastModifyDate() {
        new DocumentCBFDAO().updateLastModifyDate(this.id);
    }

    public Template getTemplate(String publishPath) throws Exception {
        if (template == null) {
            template = new DocumentCBFDAO().getTemplate(this, publishPath);
            if (template == null) {
                throw new Exception("û���ҵ�idΪ" + this.getId() + "��Ƶ��·��Ϊ" + publishPath + "��ģ��!");
            }
        }
        return template;
    }

    /**
	 * @param template
	 */
    public void setTemplate(Template template) {
        this.template = template;
    }

    /**
	 * �õ�Ԥ����λ�� ���λ������һ��Ƶ���������,indexΪ0���Ƕ���Ƶ��
	  ��Ȼ�ĵ����Ա�����n��Ƶ��,����λ��ֻ��һ��,�����ֻ������
	 *
	 * @return
	 */
    public Channel[] getPosition() {
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

    public static void main(String[] a) {
        try {
            DocumentCBF doc = getInstance(43002);
            String sContent = doc.getContent();
            int iInd = sContent.indexOf("src=\"res/");
            if (iInd > -1) {
                ConfigInfo cfg = ConfigInfo.getInstance();
                String sRep = cfg.getInfoplatDataDirContext() + "workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/";
                for (; iInd > -1; ) {
                    sContent = sContent.substring(0, iInd + 5) + sRep + sContent.substring(iInd + 5);
                    iInd = sContent.indexOf("src=\"res/");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

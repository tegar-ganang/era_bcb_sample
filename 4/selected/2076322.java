package ces.platform.infoplat.core;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ces.common.workflow.WAPI;
import ces.common.workflow.WFFactory;
import ces.platform.infoplat.core.base.BaseClass;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.dao.ChannelDAO;
import ces.platform.infoplat.core.dao.DocumentsDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.utils.Function;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class Documents extends BaseClass {

    public Documents() {
    }

    /**
     * �õ��ɱ෢�ĵ��б�
     * @param userId  ��ǰ��½�û�id
     * @param processId  ����ID
     * @param activityId   ���̽ڵ�ID���ĵ���״̬,����:�ɼ�,������,��ȣ�
     * @param treePath   ��path(������siteChannelPath����docTypePath)
     * @param isFilterByChannel
        �ǲ��ǰ���siteChannelPath�������б����,�����,�Ͱ���docTypePath������
     * @return �ĵ���������
     */
    public static DocumentCBF[] getMainList(String userId, int processId, int activityId, String treePath, boolean isFilterByChannel) throws Exception {
        return (DocumentCBF[]) new DocumentsDAO().getMainList(userId, processId, activityId, treePath, isFilterByChannel);
    }

    /**
     * �õ��ɱ෢�ĵ��б�
     * @param userId  ��ǰ��½�û�id
     * @param processId  ����ID
     * @param activityId   ���̽ڵ�ID���ĵ���״̬,����:�ɼ�,������,��ȣ�
     * @param treePath   ��path(������siteChannelPath����docTypePath)
     * @param isFilterByChannel
        �ǲ��ǰ���siteChannelPath�������б����,�����,�Ͱ���docTypePath������
     * @param resultNum  ��Ҫ���صļ�¼����
     * @return �ĵ���������
     */
    public static DocumentCBF[] getMainList(String userId, int processId, int activityId, String treePath, boolean isFilterByChannel, int resultNum) throws Exception {
        return (DocumentCBF[]) new DocumentsDAO().getMainList(userId, processId, activityId, treePath, isFilterByChannel);
    }

    public static DocumentCBF[] getMainList(int[] docIds, String condition) throws Exception {
        return (DocumentCBF[]) new DocumentsDAO().getMainList(docIds, condition);
    }

    /**
	 * ���Ƶ��·���õ��ɱ෢�ĵ��б�
	 * @param chanPath	Ƶ��·��
	 * @param descendant �Ƿ񷵻���Ƶ���µ��ĵ�
	 * @return �ĵ���������
	 */
    public static DocumentCBF[] getDocsByChanPath(String chanPath, boolean descendant) throws Exception {
        return (DocumentCBF[]) new DocumentsDAO().getDocsByChannelPath(chanPath, descendant);
    }

    /**
     * �õ������ĵ�ѡ������ĵ����б�
     * @param correlationWords ��ش�
     * @return
     * @throws java.lang.Exception
     */
    public static Document[] getMainList(String correlationWords, int[] selectionDocIDs) throws Exception {
        return (DocumentCBF[]) new DocumentsDAO().getMainList(correlationWords, selectionDocIDs);
    }

    public DocumentPublish[] getNoWithdrawList(String path, String condition) throws Exception {
        return new DocumentsDAO().getPublishedList(path, -1, null, true, null);
    }

    /**
     * �õ�ĳ��Ƶ����������ĵ��б�,��������
     * @param siteChannelPath
     */
    public static DocumentCBF[] getExamineList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath) throws Exception {
        return null;
    }

    /**
     * �õ�ĳ��Ƶ����������ĵ��б�
     * @param siteChannelPath
     * @param resultNum Ҫ���صļ�¼������,<=0ʱ��ʾ��������
     */
    public static DocumentCBF[] getExamineList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath, int resultNum) throws Exception {
        return null;
    }

    /**
     * �õ�ĳ��Ƶ����������ע���ĵ��б�,��������
     * @param siteChannelPath
     */
    public static DocumentCBF[] getShenYuePiZhuList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath) throws Exception {
        return null;
    }

    /**
     * �õ�ĳ��Ƶ����������ע���ĵ��б�
     * @param siteChannelPath
     * @param resultNum Ҫ���صļ�¼������,<=0ʱ��ʾ��������
     */
    public static DocumentCBF[] getShenYuePiZhuList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath, int resultNum) throws Exception {
        return null;
    }

    /**
     * �õ�ĳ��Ƶ������ĵ��б�,��������
     * @param siteChannelPath
     */
    public static DocumentCBF[] getPublishList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath) throws Exception {
        return null;
    }

    /**
     * �õ�ĳ��Ƶ������ĵ��б�
     * @param siteChannelPath
     * @param resultNum Ҫ���صļ�¼������,<=0ʱ��ʾ��������
     */
    public static DocumentCBF[] getPublishList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath, int resultNum) throws Exception {
        return null;
    }

    /**
     * �õ������ĵ��б�
     * @param workFlowID  ����ID
     * @param nodeID   ���̽ڵ�ID���ĵ���״̬��
     * @param docTypePath   �ĵ�������path
     * @param siteChannelPath  վ��Ƶ����path
     * @return �ĵ���������
     */
    public static Document[] getBackupList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath) throws Exception {
        return new DocumentsDAO().getBackupList();
    }

    /**
     * �õ�����վ�ĵ��б�
     * @param workFlowID  ����ID
     * @param nodeID   ���̽ڵ�ID���ĵ���״̬��
     * @param docTypePath   �ĵ�������path
     * @param siteChannelPath  վ��Ƶ����path
     * @return �ĵ���������
     */
    public static Document[] getRecycleList(int workFlowID, int nodeID, String docTypePath, String siteChannelPath) throws Exception {
        return new DocumentsDAO().getRecycleList();
    }

    public static void main(String args[]) {
        int[] d = new int[1];
        d[0] = 7502;
        try {
            delete(1, d);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ɾ��һ���ĵ�������վ
     * @param docIds
     */
    public static void delete(int userId, int[] docIds) throws Exception {
        if (docIds == null) {
            throw new Exception("û�еõ�Ҫɾ����ĵ�id!");
        }
        WAPI wapi = WFFactory.createWAPI("" + userId);
        for (int i = 0; i < docIds.length; i++) {
            DocumentCBF doc = DocumentCBF.getInstance(docIds[i]);
            if (doc == null) {
                staticLog.error("���ĵ�û���ҵ�,doc_id=" + docIds[i]);
                continue;
            }
            String[] pubChannelPath = doc.getPublishedChannelPath();
            if (null != pubChannelPath && pubChannelPath.length > 0) {
                ChannelDAO channelDao = new ChannelDAO();
                Channel channel;
                String sTemp = "";
                for (i = 0; i < pubChannelPath.length; i++) {
                    channel = (Channel) channelDao.getInstanceByPath(pubChannelPath[i]);
                    if (null != channel) sTemp += "," + channel.getName();
                }
                if (sTemp.length() > 0) sTemp = sTemp.substring(1);
                throw new Exception("Ҫɾ����ĵ� " + doc.getTitle() + " ������Ƶ�� " + sTemp + " ��!");
            }
            if (null != doc.getActyInstId() && !"".equals(doc.getActyInstId())) wapi.removeAllInfoOfProsInst(Long.parseLong(doc.getActyInstId()));
            if (doc.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD)) {
                doc.deleteMagazine(userId, Function.getSysTime());
            } else {
                doc.delete(userId, Function.getSysTime());
            }
        }
    }

    /**
     * �õ�ĳ��Ƶ���ѷ����ĵ��б�,��������
     * @param siteChannelPath
     * @info do not use static keyword anymore
     */
    public static DocumentPublish[] getPublishedList(String siteChannelPath) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(siteChannelPath, 0, false, false);
    }

    /**
	 * �����������õ�ĳ��Ƶ���ѷ����ĵ��б�,��������
	 * @param siteChannelPath
	 * @param condition ��ϲ�ѯ����
	 * @info do not use static keyword anymore
	 */
    public DocumentPublish[] getPublishedList(String siteChannelPath, String condition) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(siteChannelPath, 0, false, "", false, condition);
    }

    /**
     * �õ�ĳʱ���ĳ��Ƶ���ѷ����ĵ��б�,��������
     * @param siteChannelPath
     * @info do not use static keyword anymore
     */
    public DocumentPublish[] getPublishedList(String siteChannelPath, String strStartTime, String strEndTime) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(siteChannelPath, strStartTime, strEndTime, 0, false, false);
    }

    /**
        /**
      * �õ�ĳ��Ƶ���ѷ����ĵ��б�
      * @param siteChannelPath
      * @param resultNum Ҫ���صļ�¼������,<=0ʱ��ʾ��������
      */
    public static DocumentPublish[] getPublishedList(String siteChannelPath, int resultNum) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(siteChannelPath, resultNum, false, false);
    }

    /**
     * �õ�ĳ��Ƶ�� ������Ƶ�� �ѷ����ĵ��б�
     * @param siteChannelPath
     * @param resultNum Ҫ���صļ�¼������,<=0ʱ��ʾ��������
     */
    public static DocumentPublish[] getPublishedList(String siteChannelPath, int resultNum, boolean descendant) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(siteChannelPath, resultNum, descendant, false);
    }

    /**
     * �õ�����������Ƶ�����ĵ�
     * @param siteChannelPath Ƶ��path
     * @param resultNum Ҫ���ؼ�¼������,���˲���<=0ʱ,��ʾ��������
     * @param descendant �Ƿ񷵻���Ƶ���µ��ĵ�
     * @param orderByColumn �����ֶ�,Ҫ�ӱ����
     * @param asc �����ǽ������� true:˳�� false:����
     * @return ���û���ҵ�,return new DocumentPublish[0]����null
     * @throws java.lang.Exception
     */
    public static DocumentPublish[] getPublishedList(String siteChannelPath, int resultNum, boolean descendant, String orderByColumn, boolean asc) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(siteChannelPath, resultNum, descendant, orderByColumn, asc);
    }

    /**
	* �õ�ĳƵ�������Ⱥ���󷢲����ĵ��Ĳɼ�����
	* @param siteChannelPath Ƶ��path
	* @param descendant �Ƿ񷵻���Ƶ���µ��ĵ�
	* @throws java.lang.Exception
	*/
    public static Date[] getCreateDateList(String siteChannelPath, boolean descendant) throws Exception {
        return new DocumentsDAO().getCreateDateList(siteChannelPath, descendant);
    }

    /**
	 * �õ����±���������Ƶ�����ĵ�
	 * @param siteChannelPath Ƶ��path
	 * @param resultNum Ҫ���ؼ�¼������,���˲���<=0ʱ,��ʾ��������
	 * @param descendant �Ƿ񷵻���Ƶ���µ��ĵ�
	 * @param orderByColumn �����ֶ�,Ҫ�ӱ����
	 * @param asc �����ǽ������� true:˳�� false:����
	 * @return ���û���ҵ�,return new DocumentPublish[0]����null
	 * @throws java.lang.Exception
	 */
    public static DocumentPublish[] getPublishedDocList(String siteChannelPath, String CreateDateFrom, String CreateDateTo, int resultNum, boolean descendant, String orderByColumn, boolean asc, boolean isValid) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedDocList(siteChannelPath, CreateDateFrom, CreateDateTo, resultNum, descendant, orderByColumn, asc, isValid);
    }

    /**
     * �õ���ƪ�ĵ�,�Ѿ��������Ƶ����path
     * @param docs
     * @return
     * @throws java.lang.Exception
     */
    public static String[] getPublishedChannelPaths(DocumentCBF[] docs) throws Exception {
        List list = new ArrayList();
        for (int i = 0; i < docs.length; i++) {
            String[] tmp = docs[i].getPublishedChannelPath();
            for (int j = 0; j < tmp.length; j++) {
                if (list.indexOf(tmp[j]) < 0) {
                    list.add(tmp[j]);
                }
            }
        }
        return Function.list2StringArray(list);
    }

    /**
     * ����
     * @param siteChannelPath
     * @param docIDs
     * @param offset
     */
    public void compositor(String siteChannelPath, String[] docIDs, int offset) {
    }

    /**
     * ����һ���ĵ�
     * @param docIDs
     */
    public void publish(int[] docIds, String[] publishPaths, int publisher, Date publishDate, Date validStartDate, Date validEndDate, boolean isNextStep) throws Exception {
        if (docIds == null) {
            throw new Exception("Ҫ�������ĵ�id����Ϊnull!");
        }
        ArrayList createDates = new ArrayList();
        for (int i = 0; i < docIds.length; i++) {
            DocumentCBF doc = DocumentCBF.getInstance(docIds[i]);
            createDates.add(doc.getCreateDate());
            if (doc == null) {
                staticLog.error("û���ҵ�docId=" + docIds[i] + "���ĵ�ʵ��");
                continue;
            }
            if (doc.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD)) {
                doc.publishMagazine(publishPaths, publisher, publishDate, validStartDate, validEndDate, isNextStep);
            } else {
                doc.publish(publishPaths, publisher, publishDate, validStartDate, validEndDate, isNextStep);
            }
        }
        refreshChannelPages(publishPaths, createDates);
    }

    /**
     * �����ĵ����������
     * @param userId
     * @param processId
     * @param workitemId
     */
    public void endPublishProcess(String userId, int processId, String workitemId) {
        new DocumentsDAO().endPublishProcess(userId, processId, workitemId);
    }

    /**
     * ����һ���ĵ�
     * @param docIDs
     */
    public void unPublish(int[] docIds, String[] channelPaths, String userId, boolean isBackProcess) throws Exception {
        if (docIds == null || channelPaths == null) {
            throw new Exception("Ҫ�������ĵ�id����ͷ���Ƶ��path����������һ��Ϊnull!ע�⣺����������Ϊnull��");
        }
        if (channelPaths.length != docIds.length) {
            throw new Exception("�ĵ�id����ͷ���Ƶ��path�������һ�£�");
        }
        ArrayList createDates = new ArrayList();
        for (int i = 0; i < docIds.length; i++) {
            if (channelPaths[i] == null) {
                staticLog.error("�������?��" + i + "��Ƶ��pathΪnull��");
                continue;
            }
            DocumentPublish doc = DocumentPublish.getInstance(channelPaths[i], docIds[i]);
            createDates.add(doc.getCreateDate());
            if (doc == null) {
                staticLog.error("û�еõ�Ƶ��path=" + channelPaths[i] + ",docId=" + docIds[i] + "�ķ����ĵ�ʵ��");
                continue;
            }
            if (doc.getDoctypePath().startsWith(Const.DOCTYPE_PATH_MAGAZINE_HEAD)) {
                doc.unPublishMagazine(userId, isBackProcess);
            } else {
                doc.unPublish(userId, isBackProcess);
            }
        }
        refreshChannelPages(channelPaths, createDates);
    }

    /**
     * ����һ���ĵ�
     * @param docIDs
     */
    public static void backup(int[] docIds, int bakor, Date bakDate) throws Exception {
        if (docIds == null) {
            throw new Exception("û�еõ�Ҫ���ݵ��ĵ���");
        }
        for (int i = 0; i < docIds.length; i++) {
            DocumentCBF doc = DocumentCBF.getInstance(docIds[i]);
            if (doc == null) {
                staticLog.error("û�еõ�docId=" + docIds[i] + "���ĵ�ʵ��");
                continue;
            }
            doc.backup(bakor, bakDate);
        }
    }

    /**
     * ɾ��һ����ĵ�,����ɾ���
     * @param docIds
     */
    public static void deleteBackup(int[] docIds, Date[] bakDate) throws Exception {
        if (docIds == null || bakDate == null) {
            throw new Exception("��ɱ���ʱ,�ĵ�id�ͱ���ʱ�䶼����Ϊnull!");
        }
        for (int i = 0; i < docIds.length; i++) {
            DocumentBak doc = DocumentBak.getInstance(docIds[i], (Timestamp) bakDate[i]);
            staticLog.error("û�еõ�id=" + docIds[i] + ",��������=" + bakDate[i] + "�ı����ĵ�,�޷�ɾ��˱����ĵ�!");
            doc.delete();
        }
    }

    /**
     * �汾�ع���ֻ�ع���ݣ����ع�������Ϣ
     * @param docIDs
     */
    public void rollback(int[] docIDs) throws Exception {
    }

    /**
     * �ӻ���վ����ɾ��
     * @param docIds
     */
    public static void deleteRecycle(int userId, int[] docIds, Date[] delDate) throws Exception {
        for (int i = 0; i < docIds.length; i++) {
            DocumentRecycle.getInstance(docIds[i], (Timestamp) delDate[i]).delete(userId);
        }
    }

    /**
     * �ӻ���վ�ָ�,��ԭ
     * @param docIDs
     */
    public void unRecycle(int[] docIDs, Timestamp[] delDate) throws Exception {
        if (docIDs == null || delDate == null) {
            throw new Exception("Ҫɾ����ĵ���ź�ɾ�����ڲ���Ϊ��!");
        }
        for (int i = 0; i < docIDs.length; i++) {
            DocumentRecycle documentRecycle = DocumentRecycle.getInstance(docIDs[i], delDate[i]);
            if (documentRecycle != null) {
                documentRecycle.unRecycle();
            }
        }
    }

    /**
     * ��ջ���վ
     * @param docIDs
     */
    public void emptyRecycle() throws Exception {
    }

    /**
     * �õ�һƪ�ĵ������еķ���Ƶ��·��
     * @param docId
     * @return
     */
    public static String[] getPublishPaths(int docId) throws Exception {
        return new DocumentsDAO().getPublishPaths(docId);
    }

    /**
     * �õ�һƪ�ĵ���������Щ��վ��
     * @param docId
     * @return
     * @throws java.lang.Exception
     */
    public static Site[] getPublishSites(int docId) throws Exception {
        Site[] result = null;
        String[] publishPaths = getPublishPaths(docId);
        if (publishPaths != null) {
            List publishSitePathList = new ArrayList();
            for (int i = 0; i < publishPaths.length; i++) {
                if (publishPaths[i] != null && publishPaths[i].length() >= 10) {
                    publishSitePathList.add(publishPaths[i].substring(0, 10));
                }
            }
            String[] sitePaths = Function.distinct(Function.list2StringArray(publishSitePathList));
            if (sitePaths != null) {
                result = new Site[sitePaths.length];
                for (int i = 0; i < sitePaths.length; i++) {
                    if (sitePaths[i] != null) {
                        result[i] = (Site) TreeNode.getInstance(sitePaths[i]);
                    }
                }
            }
        }
        return result;
    }

    /**
     * �õ�һƪ�ĵ���������Щ��վ,��������Щվ���asciiName
     * @param docId
     * @return
     * @throws java.lang.Exception
     */
    public static String[] getPublishSiteAsciiNames(int docId) throws Exception {
        String[] result = null;
        Site[] sites = getPublishSites(docId);
        if (sites != null) {
            List list = new ArrayList();
            for (int i = 0; i < sites.length; i++) {
                if (sites[i] != null) {
                    list.add(sites[i].getAsciiName());
                }
            }
            result = Function.list2StringArray(list);
        }
        return result;
    }

    /**
     * ����һ���ĵ�
     * @param docIds һƪ�ĵ����ĵ�id,������Ϊ��
     * @param channelPath ָ����Щ���ĵ���Ĭ�������channelPath��,����Ϊ��
     * @param docTypePath ָ����Щ���ĵ���docTypePath,������Ϊ��
     * @param loginUserId ��ǰ��¼�û�
     * @throws java.lang.Exception
     */
    public static void copyNew(int[] docIds, String channelPath, String docTypePath, int loginUserId) throws Exception {
        if (docTypePath == null || docTypePath.trim().equals("")) {
            throw new Exception("�����ĵ���ʱ��,���ĵ�������һ��docTypePath!");
        }
        for (int i = 0; docIds != null && i < docIds.length; i++) {
            DocumentCBF doc = DocumentCBF.getInstance(docIds[i]);
            if (doc == null) {
                staticLog.error("û�еõ�docId=" + docIds[i] + "���ĵ�ʵ���ƪ�ĵ����Ʋ��ɹ���");
                continue;
            }
            doc.copyNew(channelPath, docTypePath, loginUserId);
        }
    }

    /**
     * �õ�һ��Ƶ���������е��ڿ��б�(�ѷ���)
     * @param channelPath
     * @throws java.lang.Exception
     */
    public static DocumentPublish[] getMagazineList(String channelPath, String year) throws Exception {
        if (channelPath == null || channelPath.trim().equals("")) {
            throw new Exception("�õ�һ��Ƶ�������ѷ������е��ڿ��б�ʧ��!ԭ��:û�еõ���Ƶ����path!");
        }
        return new DocumentsDAO().getMagazineList(channelPath, year);
    }

    /**
	 * �õ�һ���ڿ�ĳƵ���������е��ڿ��б�(�ѷ���)
	 * @param channelPath
	 * @param docId	���ڿ��ĵ�ID
	 * @throws java.lang.Exception
	 */
    public static DocumentPublish[] getNextMagazineList(String channelPath, int docId) throws Exception {
        if (channelPath == null || channelPath.trim().equals("")) {
            throw new Exception("�õ�һ���ڿ�ĳƵ���������е��ڿ��б�ʧ��!ԭ��:û�еõ���Ƶ����path!");
        }
        return new DocumentsDAO().getMagazineList(channelPath, docId);
    }

    /**
     * �õ�һ��Ƶ���������е��ڿ������(�ѷ���)
     * @param channelPath
     * @throws java.lang.Exception
     */
    public static String[] getMagazineYearList(String channelPath) throws Exception {
        if (channelPath == null || channelPath.trim().equals("")) {
            throw new Exception("�õ�һ��Ƶ�������ѷ������е��ڿ��б�ʧ��!ԭ��:û�еõ���Ƶ����path!");
        }
        return new DocumentsDAO().getMagazineYearList(channelPath);
    }

    /**
     * �õ�һ��Ƶ���������һ���ڿ����ڿ�ͷʵ��
     * @param channelPath
     * @throws java.lang.Exception
     */
    public static DocumentPublish getLastMagazine(String channelPath, String year) throws Exception {
        if (channelPath == null || channelPath.trim().equals("")) {
            throw new Exception("�õ�һ��Ƶ�������ѷ������е��ڿ��б�ʧ��!ԭ��:û�еõ���Ƶ����path!");
        }
        return new DocumentsDAO().getLastMagazine(channelPath, year);
    }

    /**
     * ˢ��Ƶ��ҳ��
     * @param publishPaths
     */
    public void refreshChannelPages(String[] publishPaths, List createDates) throws Exception {
        if (publishPaths != null && publishPaths.length > 0 && createDates != null && createDates.size() > 0) {
            for (int i = 0; i < publishPaths.length; i++) {
                for (int j = 0; j < createDates.size(); j++) {
                    refreshChannelPage(publishPaths[i], (Date) createDates.get(j));
                }
            }
        }
    }

    void refreshChannelPage(String publishPath, Date createDate) throws Exception {
        String path = publishPath;
        TreeNode tmp = TreeNode.getInstance(publishPath);
        Channel chan = null;
        if (!(tmp instanceof Channel)) {
            throw new Exception("����ת������!pathΪ" + path + "�õ��Ĳ���Ƶ������!");
        } else {
            chan = (Channel) tmp;
        }
        if (chan == null) {
            throw new Exception("û���ҵ�pathΪ" + path + "��Ƶ��!");
        }
        ChannelDAO dao = new ChannelDAO();
        int flag = chan.getPageNum();
        if (flag == 0) {
            if (chan.getRefresh().equals(Channel.REFRESH_NO)) {
                chan.updateRefreshFlag(true);
            } else if (chan.getRefresh().equals(Channel.REFRESH_RIGHT_NOW)) {
                dao.generateChannelPage(chan, false);
            }
        } else {
            dao.generateChannelTPage(chan, false, createDate, flag);
        }
        String[] channels = dao.getRelatingChannel(path, Channel.PAGE_RELATIVE);
        if (channels != null) {
            for (int j = 0; j < channels.length; j++) {
                Channel channel = (Channel) Channel.getInstance(channels[j]);
                if (channel != null) {
                    flag = channel.getPageNum();
                    if (flag == 0) {
                        String refresh = channel.getRefresh();
                        if (refresh.equals(Channel.REFRESH_NO)) {
                            channel.updateRefreshFlag(true);
                        } else if (refresh.equals(Channel.REFRESH_RIGHT_NOW)) {
                            dao.generateChannelPage(channel, false);
                        }
                    } else {
                        dao.generateChannelTPage(channel, false, createDate, flag);
                    }
                }
            }
        }
    }

    /**
	 * ˢ��Ƶ��ҳ��
	 * @param publishPaths
	 */
    public void refreshChannelPages(String[] publishPaths) throws Exception {
        for (int i = 0; i < publishPaths.length; i++) {
            refreshChannelPage(publishPaths[i]);
        }
    }

    void refreshChannelPage(String publishPath) throws Exception {
        String path = publishPath;
        TreeNode tmp = TreeNode.getInstance(publishPath);
        Channel chan = null;
        if (!(tmp instanceof Channel)) {
            throw new Exception("����ת������!pathΪ" + path + "�õ��Ĳ���Ƶ������!");
        } else {
            chan = (Channel) tmp;
        }
        if (chan == null) {
            throw new Exception("û���ҵ�pathΪ" + path + "��Ƶ��!");
        }
        ChannelDAO dao = new ChannelDAO();
        if (chan.getRefresh().equals(Channel.REFRESH_NO)) {
            chan.updateRefreshFlag(true);
        } else if (chan.getRefresh().equals(Channel.REFRESH_RIGHT_NOW)) {
            dao.generateChannelPage(chan, false);
        }
        String[] channels = dao.getRelatingChannel(path, Channel.PAGE_RELATIVE);
        if (channels != null) {
            for (int j = 0; j < channels.length; j++) {
                Channel channel = (Channel) Channel.getInstance(channels[j]);
                if (channel != null) {
                    String refresh = channel.getRefresh();
                    if (refresh.equals(Channel.REFRESH_NO)) {
                        channel.updateRefreshFlag(true);
                    } else if (refresh.equals(Channel.REFRESH_RIGHT_NOW)) {
                        dao.generateChannelPage(channel, false);
                    }
                }
            }
        }
    }

    /**����ת��
	 * @param docIds
	 * @param targetChannelPaths
	 * @comment 050107 bill add this method intending to copy a published doc directly
	 */
    public void copyPublish(int[] docIds, String selfChannelPath, String[] targetChannelPaths) throws Exception {
        for (int i = 0; i < targetChannelPaths.length; i++) {
            new DocumentsDAO().copyPublish(docIds, selfChannelPath, targetChannelPaths[i]);
            refreshChannelPage(targetChannelPaths[i]);
        }
    }

    /**
     * �õ�����������Ƶ�����ĵ�
     * @param resultNum Ҫ���ؼ�¼������,���˲���<=0ʱ,��ʾ��������
     * @param orderByColumn �����ֶ�,Ҫ�ӱ����
     * @param asc �����ǽ������� true:˳�� false:����
     * @return ���û���ҵ�,return new DocumentPublish[0]����null
     * @throws java.lang.Exception
     */
    public static DocumentPublish[] getPublishedList(int resultNum, String orderByColumn, boolean asc, String condition) throws Exception {
        return (DocumentPublish[]) new DocumentsDAO().getPublishedList(resultNum, orderByColumn, asc, condition);
    }
}

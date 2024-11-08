package ces.platform.infoplat.utils.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ces.common.workflow.WAPI;
import ces.common.workflow.WFFactory;
import ces.common.workflow.entity.Task;
import ces.coral.dbo.DBOperation;
import ces.coral.dbo.DBOperationFactory;
import ces.coral.dbo.ERDBOperationFactory;
import ces.coral.encrypt.MD5;
import ces.coral.file.CesGlobals;
import ces.coral.file.FileOperation;
import ces.coral.image.ImgHandle;
import ces.platform.infoplat.core.DocAffix;
import ces.platform.infoplat.core.DocContentResource;
import ces.platform.infoplat.core.DocPicture;
import ces.platform.infoplat.core.DocResource;
import ces.platform.infoplat.core.DocType;
import ces.platform.infoplat.core.DocumentCBF;
import ces.platform.infoplat.core.Documents;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.dao.DocumentCBFDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.core.wf.IpWfControl;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.common.Constant;
import ces.platform.system.common.IdGenerator;

/**
 * @author mysheros
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DocumentTools {

    /**
     * ʵ���ĵ���������
     * @param doc �ĵ�����
     * @param retFile �������ݻ����������ļ�·�����ļ���blRtypeΪ-1ʱΪ·��������Ϊ����
     * @param retAffixs ͼ�Ļ�������Դ·�����ļ���
     * @param pictures �ϴ�ͼƬ����Դ·�����ļ���
     * @param affixs	�ϴ���������Դ·�����ļ���
     * @param isPublish �Ƿ񷢲�
     * @param blRtype ͼ�Ļ���һ���ĵ���������ļ������ļ����ݣ�Ϊ-1ʱΪ·��
     * @param blRte �Ƿ���ͼ�Ļ��Ż����������� trueΪͼ�Ļ��� falseΪһ���ĵ�
     * @return doc_id �ĵ�ID
     * @throws Exception
     */
    public int importData(DocumentCBF doc, String retFile, String[] retAffixs, String[] pictures, String[] affixs, boolean isPublish, String blRtype, boolean blRte) throws Exception {
        DocumentCBFDAO docCBFDAO = new DocumentCBFDAO();
        int docId = (int) (IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC));
        doc.setId(docId);
        doc.setRemarkProp(1);
        if (doc.getCreateDate() == null) doc.setCreateDate(Function.getSysTime());
        String actyInsyId = create(doc);
        doc.setActyInstId(actyInsyId);
        docCBFDAO.add(doc);
        saveContent(doc, retFile, retAffixs, blRtype, blRte);
        if (pictures != null && pictures.length > 0) upLoad(doc, pictures, DocResource.PICTURE_TYPE);
        if (affixs != null && affixs.length > 0) upLoad(doc, affixs, DocResource.AFFIX_TYPE);
        if (isPublish) {
            Documents docs = new Documents();
            docs.publish(new int[] { doc.getId() }, new String[] { doc.getChannelPath() }, doc.getCreater(), Function.getSysTime(), Function.getSysTime(), null, true);
            String workItemId = getWorkItemId(actyInsyId);
            docs.endPublishProcess(String.valueOf(doc.getCreater()), Integer.parseInt(doc.getProcessId()), workItemId);
        }
        return docId;
    }

    /**
	 * ʵ���ĵ���������
     * @param doc �ĵ�����
     * @param retFile ���������ļ�
     * @param retAffixs ͼ�Ļ�������Դ·�����ļ���
     * @param pictures �ϴ�ͼƬ����Դ·�����ļ���
     * @param affixs	�ϴ���������Դ·�����ļ���
     * @param isPublish �Ƿ񷢲�
     * @param blRte �Ƿ���ͼ�Ļ��Ż�����������
     * @return doc_id �ĵ�ID
	 * @throws Exception
	 */
    public int importData(DocumentCBF doc, String retFile, String[] retAffixs, String[] pictures, String[] affixs, boolean isPublish, boolean blRte) throws Exception {
        return importData(doc, retFile, retAffixs, pictures, affixs, isPublish, "-1", blRte);
    }

    /**
     * ɾ���ĵ�
     * @param docId �ĵ�ID����
     * @throws Exception
     */
    public void delDocument(int[] docIds) throws Exception {
        if (docIds == null || docIds.length == 0) return;
        int i, j;
        String[] publishPaths = null;
        int[] tmpDocIds = null;
        for (i = 0; i < docIds.length; i++) {
            publishPaths = Documents.getPublishPaths(docIds[i]);
            if (publishPaths == null || publishPaths.length == 0) continue;
            tmpDocIds = new int[publishPaths.length];
            for (j = 0; j < tmpDocIds.length; j++) tmpDocIds[j] = docIds[i];
            new Documents().unPublish(tmpDocIds, publishPaths, "1", true);
        }
        Documents.delete(1, docIds);
    }

    /**
	 * ɾ���ĵ�
	 * @param refIds ������������
	 * @throws Exception
	 */
    public void delDocumentsByRef(int[] refIds) throws Exception {
        try {
            if (refIds != null && refIds.length > 0) delDocument(getDocIds(refIds));
        } catch (Exception e) {
            throw new Exception("�����������ɾ���ĵ�ʧ�ܣ� " + e.getMessage());
        }
    }

    /**
	 * �޸��ĵ�����ɾ����������ɾ��ʱ�����������ɾ��
     * @param doc 		�ĵ����󣬰���������
     * @param retFile 	���������ļ�
     * @param retAffixs ͼ�Ļ�������Դ·�����ļ���
     * @param pictures 	�ϴ�ͼƬ����Դ·�����ļ���
     * @param affixs	�ϴ���������Դ·�����ļ���
     * @param isPublish �Ƿ񷢲�
     * @param blRte		�Ƿ���ͼ�Ļ��Ż�����������
     * @return doc_id	�ĵ�ID
	 * @throws Exception
	 */
    public int updateData(DocumentCBF doc, String retFile, String[] retAffixs, String[] pictures, String[] affixs, boolean isPublish, boolean blRte) throws Exception {
        int docId = 0;
        try {
            int[] refIds = { Integer.parseInt(doc.getReservation6()) };
            delDocumentsByRef(refIds);
            docId = importData(doc, retFile, retAffixs, pictures, affixs, isPublish, blRte);
        } catch (Exception e) {
            throw new Exception("�޸��ĵ�ʧ��!" + e.getMessage());
        }
        return docId;
    }

    /**
     * ������������
     * @param doc
     * @param docContent
     * @param retAffixs ͼ�Ļ�������Դ·�����ļ���
     * @param blRte
     */
    private void saveContent(DocumentCBF docBF, String docContent, String[] retAffixs, String blRtype, boolean blRte) throws Exception {
        try {
            CesGlobals cesGlobals = new CesGlobals();
            String uploadPath;
            cesGlobals.setConfigFile("platform.xml");
            uploadPath = cesGlobals.getCesXmlProperty("platform.datadir");
            uploadPath = new File(uploadPath).getPath();
            if (uploadPath.endsWith("\\")) {
                uploadPath = uploadPath.substring(0, uploadPath.length() - 1);
            }
            uploadPath = uploadPath + "/infoplat/workflow/docs/" + Function.getNYofDate(docBF.getCreateDate()) + "/";
            if (!new File(uploadPath).isDirectory()) {
                new File(uploadPath).mkdirs();
            }
            if (blRtype.equals("-1")) {
                if (docContent != null && docContent.length() > 0) {
                    String[] reFile = { docContent };
                    if (blRte && retAffixs != null && retAffixs.length > 0) {
                        BufferedInputStream in = new BufferedInputStream(new FileInputStream(docContent));
                        StringBuffer sCon = new StringBuffer();
                        byte[] b = new byte[1024];
                        int iLen = 0;
                        while (iLen != -1) {
                            iLen = in.read(b);
                            sCon.append(new String(b));
                        }
                        in.close();
                        String content = sCon.toString();
                        if (!content.trim().toLowerCase().startsWith("<body>")) content = "<body>" + content + "</body>";
                        DocumentCBFDAO docCBFDAO = new DocumentCBFDAO();
                        DocContentResource[] docResource = docCBFDAO.getDocContentResourceList(docBF.getId());
                        List tmp = new ArrayList();
                        for (int j = 0; j < docResource.length; j++) {
                            tmp.add(docResource[j].getUri());
                        }
                        String fileName = "";
                        File ff;
                        uploadPath += "res/";
                        if (!new File(uploadPath).isDirectory()) {
                            new File(uploadPath).mkdirs();
                        }
                        for (int i = 0; i < retAffixs.length && i < tmp.size(); i++) {
                            ff = new File(retAffixs[i]);
                            if (ff.isFile()) {
                                FileOperation.copy(retAffixs[i], uploadPath + tmp.get(i));
                            }
                        }
                    }
                } else {
                    String newFileName = "d_" + docBF.getId() + ".data";
                    FileOperation.copy(docContent, uploadPath + newFileName);
                }
            } else {
                if (docContent != null && !docContent.equals("")) {
                    if (!docContent.trim().toLowerCase().startsWith("<body>")) docContent = "<body>" + docContent + "</body>";
                    if (blRte) {
                    } else {
                    }
                    if (blRte && retAffixs != null && retAffixs.length > 0) {
                        DocumentCBFDAO docCBFDAO = new DocumentCBFDAO();
                        DocContentResource[] docResource = docCBFDAO.getDocContentResourceList(docBF.getId());
                        List tmp = new ArrayList();
                        for (int j = 0; j < docResource.length; j++) {
                            tmp.add(docResource[j].getUri());
                        }
                        String fileName = "";
                        File ff;
                        uploadPath += "res/";
                        if (!new File(uploadPath).isDirectory()) {
                            new File(uploadPath).mkdirs();
                        }
                        for (int i = 0; i < retAffixs.length && i < tmp.size(); i++) {
                            ff = new File(retAffixs[i]);
                            if (ff.isFile()) {
                                FileOperation.copy(retAffixs[i], uploadPath + tmp.get(i));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("������������ʧ��!" + e.getMessage());
        }
    }

    /**
     * �������̽��
     * @param doc
     * @return
     * @throws Exception
     */
    private String create(DocumentCBF doc) throws Exception {
        try {
            DocType docType = (DocType) TreeNode.getInstance(doc.getDoctypePath());
            if (docType == null) {
                throw new Exception("û���ҵ�path=" + doc.getDoctypePath() + "���ĵ�����!");
            }
            String sCreater = "1";
            if (doc.getCreater() != 0) {
                sCreater = "" + doc.getCreater();
            }
            WAPI wapi = WFFactory.createWAPI(sCreater);
            int activityId = new IpWfControl().getStartActivity(docType.getProcessId());
            Task task = wapi.startProcessInstance(docType.getProcessId(), String.valueOf(activityId), "�������");
            String processInstId = String.valueOf(task.getProsInstId());
            return processInstId;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("û���ҵ�path=" + doc.getDoctypePath() + "���ĵ�����!");
        }
    }

    /**
     * �ϴ��ļ������浽��Դ����
     * @param doc
     * @param items
     * @param uploadType
     * @throws Exception
     */
    private void upLoad(DocumentCBF doc, String[] items, String uploadType) throws Exception {
        CesGlobals cesGlobals = new CesGlobals();
        String uploadPath;
        cesGlobals.setConfigFile("platform.xml");
        uploadPath = cesGlobals.getCesXmlProperty("platform.datadir");
        uploadPath = new File(uploadPath).getPath();
        if (uploadPath.endsWith("\\")) {
            uploadPath = uploadPath.substring(0, uploadPath.length() - 1);
        }
        uploadPath = uploadPath + "/infoplat/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/res/";
        if (!new File(uploadPath).isDirectory()) {
            new File(uploadPath).mkdirs();
        }
        try {
            String orgFileName = "", newFileName = "";
            long fileSize;
            File ff, f;
            MD5 strMD5 = new MD5();
            for (int i = 0; i < items.length; i++) {
                ff = new File(items[i]);
                if (ff.isFile()) {
                    orgFileName = ff.getName();
                    fileSize = ff.length();
                    if (fileSize != 0 && orgFileName != null) {
                        newFileName = "t" + strMD5.getMD5ofStr(orgFileName + fileSize + (new Random()).nextInt()) + "." + orgFileName.substring(orgFileName.lastIndexOf(".") + 1);
                        FileOperation.copy(items[i], uploadPath + newFileName);
                        f = new File(uploadPath + newFileName);
                        String breviaryImageFileName = "min" + f.getName();
                        if (DocResource.PICTURE_TYPE.equals(uploadType)) {
                            ConfigInfo ci = ConfigInfo.getInstance();
                            ImgHandle imgHandle = new ImgHandle();
                            try {
                                imgHandle.zoom(f.getPath(), f.getParent() + File.separator + breviaryImageFileName, ci.getImageMinHeight(), ci.getImageMinWidth());
                            } catch (Exception ex1) {
                                breviaryImageFileName = "";
                            }
                        }
                        DocResource dr = null;
                        if (DocResource.AFFIX_TYPE.equals(uploadType)) {
                            dr = new DocAffix();
                        } else if (DocResource.PICTURE_TYPE.equals(uploadType)) {
                            dr = new DocPicture();
                        }
                        dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                        dr.setAutoPlay(false);
                        dr.setCreateDate(Function.getSysTime());
                        dr.setCreater(1);
                        dr.setDocId(doc.getId());
                        dr.setFileExt(newFileName.substring(newFileName.lastIndexOf(".") + 1));
                        dr.setFileSize(Function.byteToKB(fileSize));
                        dr.setOrderNo(Integer.parseInt(new ces.platform.infoplat.ui.common.defaultvalue.OrderNo().getDefaultValue()));
                        dr.setOriginalFile(orgFileName);
                        dr.setType(uploadType);
                        dr.setUri(newFileName);
                        dr.add();
                        if (DocResource.PICTURE_TYPE.equals(uploadType) && !breviaryImageFileName.equals("")) {
                            dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                            dr.setCreateDate(Function.getSysTime());
                            dr.setFileSize(Function.byteToKB(fileSize));
                            dr.setOrderNo(Integer.parseInt(new ces.platform.infoplat.ui.common.defaultvalue.OrderNo().getDefaultValue()));
                            dr.setOriginalFile("(����ͼ)" + orgFileName);
                            dr.setUri(breviaryImageFileName);
                            dr.add();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private int[] getDocIds(int refIds[]) throws Exception {
        int[] result = null;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        new CesGlobals().setConfigFile(Constant.DB_CONFIGE_FILE);
        DBOperationFactory factory = new ERDBOperationFactory();
        DBOperation dbo = factory.createDBOperation(ConfigInfo.getInstance().getPoolName());
        String sql = "select id from t_ip_doc where reservation6 = ? ";
        try {
            List tmp = new ArrayList();
            con = dbo.getConnection();
            ps = con.prepareStatement(sql);
            for (int i = 0; i < refIds.length; i++) {
                ps.setString(1, String.valueOf(refIds[i]));
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    tmp.add(String.valueOf(resultSet.getInt(1)));
                }
            }
            result = new int[tmp.size()];
            for (int i = 0; i < tmp.size(); i++) result[i] = Integer.parseInt((String) tmp.get(i));
        } catch (Exception ex) {
            throw ex;
        } finally {
            resultSet.close();
            ps.close();
            con.close();
            dbo.close();
        }
        return result;
    }

    private String getWorkItemId(String processId) throws Exception {
        String workItemId = null;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        new CesGlobals().setConfigFile(Constant.DB_CONFIGE_FILE);
        DBOperationFactory factory = new ERDBOperationFactory();
        DBOperation dbo = factory.createDBOperation(ConfigInfo.getInstance().getPoolName());
        String sql = "select id from t_wf_task where pros_inst_id = ? order by id desc";
        try {
            List tmp = new ArrayList();
            con = dbo.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, processId);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                tmp.add(resultSet.getString(1));
            }
            workItemId = (String) tmp.get(0);
        } catch (Exception ex) {
            throw ex;
        } finally {
            resultSet.close();
            ps.close();
            con.close();
            dbo.close();
        }
        return workItemId;
    }

    public static void main(String args[]) {
        DocumentTools dt = new DocumentTools();
        int docId = 0;
        String retFile = "D:\\d_1147502.data";
        retFile = "��ʱ�����ĵ���������룬֮�����з������ĵ��������룬�������ˢ�µ�Ƶ��ҳ��Ҳ�����룬����<IMG src=\"res/tC1697FAEE9C0A4AA9356186B710E6CE6.gif\" align=left>һ��";
        String[] retAffixs = new String[] {};
        String[] pictures = new String[] {};
        String[] affixs = new String[] {};
        DocumentCBF doc = new DocumentCBF();
        doc.setChannelPath("000000080201402");
        doc.setDoctypePath("000010000101102");
        doc.setProcessId("1");
        doc.setSecurityLevelId(1);
        doc.setRemarkProp(1);
        doc.setCreater(1);
        doc.setContent("333");
        doc.setAbstractWords("44444");
        doc.setContentFile("555");
        Timestamp date = Function.getSysTime();
        try {
            int[] ids = { 0 };
            dt.delDocumentsByRef(ids);
            for (int i = 0; i < 1; i++) {
                doc.setTitle("����� " + i);
                doc.setReservation6("" + i);
                date.setDate(date.getDate() + 1);
                doc.setCreateDate(date);
                docId = dt.importData(doc, retFile, retAffixs, pictures, affixs, true, "", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}

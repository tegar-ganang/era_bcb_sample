package ces.platform.infoplat.ui.workflow.action;

import java.util.ArrayList;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ces.coral.log.Logger;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.DocumentCBF;
import ces.platform.infoplat.core.DocumentPublish;
import ces.platform.infoplat.core.Documents;
import ces.platform.infoplat.core.SiteChannelDocTypeRelation;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.dao.ChannelDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.ui.common.DefaultTreeNodeTitleDecorate;
import ces.platform.infoplat.ui.common.TreeJsCode;
import ces.platform.infoplat.ui.common.defaultvalue.LoginUser;
import ces.platform.infoplat.ui.common.defaultvalue.OrderNo;
import ces.platform.infoplat.ui.workflow.publish.PublishTreeAuthority;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.dbaccess.User;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description:
 * ����������:<br>
 * 1.�����ĵ�����������,�����?���ĵ����߼�,��󷵻ؽ��,������������<br>
 * 2.��ʾ����ҳ��(���ڴ���һЩĬ��ֵ��)</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class DocPublishAction extends Action {

    Logger log = new Logger(getClass());

    private static final String OPERATE_SHOW_PUBLISH_JSP = "showJsp";

    private static final String OPERATE_PUBLISH = "publishLogical";

    private static final String OPERATE_COPY_TRANSFER_PUBLISH = "copyTransferPublishLogical";

    private static final String OPERATE_MOVE_TRANSFER_PUBLISH = "moveTransferPublishLogical";

    private static final String OPERATE_SHOW_TRANSFER_JSP = "transfer";

    private static final String OPERATE_REPUBLISH = "rePublish";

    private static final String OPERATE_UNPUBLISH_BYALLCHANNEL = "unPublishByAllChannel";

    private static final String OPERATE_UNPUBLISH_BYCURRCHANNEL = "unPublishByCurrChannel";

    private static final int PUBLISH_TREE = 1;

    private static final int TRANSFER_TREE = 2;

    private boolean repaixu = true;

    private boolean redate = true;

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String docId = request.getParameter("docID");
        try {
            LoginUser lu = new LoginUser();
            lu.setRequest(request);
            int userId = Integer.parseInt(lu.getDefaultValue());
            User user = (User) request.getSession().getAttribute("user");
            String flagSA = (user != null ? user.getFlagSA() : "");
            if (docId == null) {
                throw new Exception("û�еõ�Ҫ�������ĵ�id");
            }
            if ("false".equals(request.getParameter("repaixu"))) {
                repaixu = false;
            } else {
                repaixu = true;
            }
            if ("false".equals(request.getParameter("redate"))) {
                redate = false;
            } else {
                redate = true;
            }
            String operate = request.getParameter("operate");
            operate = operate == null ? OPERATE_SHOW_PUBLISH_JSP : operate;
            String workItemId = request.getParameter("workItemId");
            String processId = request.getParameter("processId");
            int[] docIds = Function.strArray2intArray(Function.stringToArray(docId));
            String[] targetChannelPaths = Function.stringToArray(request.getParameter("channelPath"));
            String selfChannelPath = request.getParameter("selfChannelPath");
            if (operate.trim().equalsIgnoreCase(OPERATE_SHOW_PUBLISH_JSP)) {
                showPublishJsp(docIds, request, user);
                return mapping.findForward("showPublishJsp");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_PUBLISH)) {
                if (targetChannelPaths == null || targetChannelPaths.length == 0) {
                    throw new Exception("û�еõ�Ҫ������վ��Ƶ��path!");
                }
                publish(docIds, targetChannelPaths, request, true);
                new Documents().endPublishProcess(String.valueOf(user.getUserID()), Integer.parseInt(processId), workItemId);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�����ɹ���");
                request.setAttribute(Const.SUCCESS_FRESH_FLAG, "opener");
                return mapping.findForward("operateSuccess");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_COPY_TRANSFER_PUBLISH)) {
                if (targetChannelPaths == null || targetChannelPaths.length == 0) {
                    throw new Exception("û�еõ�Ҫ������վ��Ƶ��path!");
                }
                int iInd = selfChannelPath.indexOf(",");
                if (iInd > -1) selfChannelPath = selfChannelPath.substring(0, iInd);
                new Documents().copyPublish(docIds, selfChannelPath, targetChannelPaths);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "����ת���ɹ���");
                request.setAttribute(Const.SUCCESS_FRESH_FLAG, "opener");
                return mapping.findForward("operateSuccess");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_MOVE_TRANSFER_PUBLISH)) {
                if (targetChannelPaths == null || targetChannelPaths.length == 0) {
                    throw new Exception("û�еõ�Ҫ������վ��Ƶ��path!");
                }
                String[] selfChannelPaths = Function.stringToArray(selfChannelPath);
                if (selfChannelPaths == null || selfChannelPaths.length != docIds.length) {
                    throw new Exception("û�еõ��ĵ����?����path,���ߺ��ĵ�id��һһ��Ӧ!!");
                }
                new Documents().copyPublish(docIds, selfChannelPaths[0], targetChannelPaths);
                unPublish(docIds, selfChannelPaths, String.valueOf(user.getUserID()), false);
                for (int i = 0; i < targetChannelPaths.length; i++) {
                    for (int j = 0; j < docIds.length; j++) {
                        DocumentCBF cbf = DocumentCBF.getInstance(docIds[j]);
                        cbf.setChannelPath(targetChannelPaths[i]);
                        cbf.update();
                    }
                }
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�ƶ�ת���ɹ���");
                request.setAttribute(Const.SUCCESS_FRESH_FLAG, "opener");
                return mapping.findForward("operateSuccess");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_SHOW_TRANSFER_JSP)) {
                showTransferJsp(docIds, request, user);
                return mapping.findForward("showPublishJsp");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_REPUBLISH)) {
                String republishChannel = request.getParameter("republishChannel");
                if (republishChannel == null || republishChannel.trim().equalsIgnoreCase("selfChannel")) {
                    if (targetChannelPaths == null || targetChannelPaths.length == 0) {
                        throw new Exception("û�еõ�Ҫ������վ��Ƶ��path!");
                    }
                    rePublish(docIds, targetChannelPaths, repaixu, redate, request);
                } else {
                    for (int i = 0; i < docIds.length; i++) {
                        String[] publishPaths = Documents.getPublishPaths(docIds[i]);
                        if (publishPaths == null || publishPaths.length == 0) {
                            log.error("û�еõ�docId=" + docIds[i] + "�ķ���Ƶ��path!");
                            continue;
                        }
                        for (int j = 0; j < publishPaths.length; j++) {
                            int[] tmpDocIds = new int[1];
                            tmpDocIds[0] = docIds[i];
                            String[] tmpPublishPaths = new String[1];
                            tmpPublishPaths[0] = publishPaths[j];
                            if (tmpPublishPaths[0] == null) {
                                log.error("�õ��ĵ�������pathʱ,������һ��path=null,���Ը����·�����Ƶ��ʱ����!docId=" + tmpDocIds[0]);
                                continue;
                            }
                            rePublish(tmpDocIds, tmpPublishPaths, repaixu, redate, request);
                        }
                    }
                }
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "���·����ɹ���");
                request.setAttribute(Const.SUCCESS_FRESH_FLAG, "opener");
                return mapping.findForward("operateSuccess");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_UNPUBLISH_BYCURRCHANNEL)) {
                if (targetChannelPaths == null || targetChannelPaths.length == 0 || targetChannelPaths.length != docIds.length) {
                    throw new Exception("û�еõ�Ҫ������վ��Ƶ��path,���ߺ��ĵ�id��һһ��Ӧ!");
                }
                unPublish(docIds, targetChannelPaths, String.valueOf(user.getUserID()), true);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�����ڵ�ǰƵ���ϵ��ĵ��ɹ���");
                request.setAttribute(Const.SUCCESS_FRESH_FLAG, "opener");
                return mapping.findForward("operateSuccess");
            } else if (operate.trim().equalsIgnoreCase(OPERATE_UNPUBLISH_BYALLCHANNEL)) {
                for (int i = 0; i < docIds.length; i++) {
                    String[] publishPaths = Documents.getPublishPaths(docIds[i]);
                    if (publishPaths == null || publishPaths.length == 0) {
                        log.error("û�еõ�docId=" + docIds[i] + "�����з������Ƶ��,���ĵ�����ʧ��!");
                        continue;
                    }
                    int[] tmpDocIds = new int[publishPaths.length];
                    for (int j = 0; j < tmpDocIds.length; j++) {
                        tmpDocIds[j] = docIds[i];
                    }
                    unPublish(tmpDocIds, publishPaths, String.valueOf(user.getUserID()), true);
                }
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "����������Ƶ���ϵ��ĵ��ɹ���");
                request.setAttribute(Const.SUCCESS_FRESH_FLAG, "opener");
                return mapping.findForward("operateSuccess");
            } else {
                throw new Exception("δ֪�Ĳ�������:" + operate + "!��������url��");
            }
        } catch (Exception ex) {
            log.error("��������", ex);
            ex.printStackTrace();
            request.setAttribute(Const.ERROR_MESSAGE_NAME, "����!" + ex.getMessage());
            return mapping.findForward("error");
        }
    }

    private void unPublish(int[] docIds, String[] channelPaths, String userId, boolean isBackProcess) throws Exception {
        new Documents().unPublish(docIds, channelPaths, userId, isBackProcess);
    }

    private void showPublishJsp(int[] docIds, HttpServletRequest request, User user) throws Exception {
        request.setAttribute("operate", DocPublishAction.OPERATE_PUBLISH);
        request.setAttribute("docID", Function.arrayToStr(docIds));
        DocumentCBF[] docs = new DocumentCBF[docIds.length];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = DocumentCBF.getInstance(docIds[i]);
            if (docs[i] == null) {
                throw new Exception("���ܵõ��ĵ����Ϊ" + docIds[i] + "��ʵ��!��������Ƿ���ȷ!");
            }
        }
        request.setAttribute("docs", docs);
        DocumentCBF doc = docs[0];
        String[] defaultSelections = null;
        if (doc.getChannelPath() != null && !doc.getChannelPath().trim().equals("")) {
            defaultSelections = new String[1];
            defaultSelections[0] = doc.getChannelPath();
            String[] a = new ChannelDAO().getRelatingChannel(doc.getChannelPath(), Channel.DATA_RELATIVE);
            if (a != null) {
                String[] b = new String[a.length + 1];
                for (int i = 0; i < a.length; i++) {
                    b[i] = a[i];
                }
                b[a.length] = doc.getChannelPath();
                defaultSelections = b;
            }
        } else {
            defaultSelections = SiteChannelDocTypeRelation.getSiteChannelPaths(doc.getDoctypePath(), true);
        }
        TreeNode[] tns = null;
        if (ConfigInfo.getInstance().getChannelAuthority().equals("1")) {
            if (user.getUserID() == 1 || user.getFlagSA().trim().equals("1")) {
                tns = TreeNode.getSiteChannelTree();
            } else {
                tns = TreeNode.getSiteChannelTree(user.getUserID(), "8");
            }
        } else {
            tns = TreeNode.getSiteChannelTree();
        }
        if (tns == null) {
            tns = new TreeNode[0];
        }
        String[] links = Function.newStringArray(tns.length, "");
        String[] targets = Function.newStringArray(tns.length, "");
        TreeNode root = TreeNode.getSiteChannelTreeRoot();
        String rootLink = "";
        String rootTarget = "";
        String tempPaths = "";
        int i = 0;
        String tempPath;
        String selfSitePath = doc.getChannelPath().substring(0, 10);
        for (; i < tns.length; i++) {
            tempPath = tns[i].getPath();
            if (tempPath != null && tempPath.length() == 10) {
                tempPaths = tempPaths + tempPath + ",";
            } else if (ConfigInfo.getInstance().getChannelAuthority().equals("2") && tempPath != null && !tempPath.startsWith(selfSitePath)) {
                tempPaths = tempPaths + tempPath + ",";
            }
        }
        String[] disabledPaths = Function.stringToArray(tempPaths);
        String siteChannelTreeCode = getTreeJsCode(TreeNode.SITE_CHANNEL_TREE, root, rootLink, rootTarget, tns, links, targets, defaultSelections, disabledPaths, user);
        request.setAttribute("siteChannelTreeCode", siteChannelTreeCode);
    }

    private void showTransferJsp(int[] docIds, HttpServletRequest request, User user) throws Exception {
        String transferType = request.getParameter("transferType");
        if (transferType == null || transferType.trim().equalsIgnoreCase("copy")) {
            transferType = OPERATE_COPY_TRANSFER_PUBLISH;
        } else {
            transferType = OPERATE_MOVE_TRANSFER_PUBLISH;
        }
        request.setAttribute("operate", transferType);
        String channelPath = String.valueOf(request.getParameter("selfChannelPath"));
        request.setAttribute("selfChannelPath", request.getParameter("selfChannelPath"));
        request.setAttribute("docID", Function.arrayToStr(docIds));
        DocumentCBF[] docs = new DocumentCBF[docIds.length];
        for (int i = 0; i < docs.length; i++) {
            docs[i] = DocumentCBF.getInstance(docIds[i]);
            if (docs[i] == null) {
                throw new Exception("���ܵõ��ĵ����Ϊ" + docIds[i] + "��ʵ��!��������Ƿ���ȷ!");
            }
        }
        request.setAttribute("docs", docs);
        String[] published = Documents.getPublishedChannelPaths(docs);
        TreeNode[] tns = null;
        if (ConfigInfo.getInstance().getChannelAuthority().equals("1")) {
            if (user.getUserID() == 1 || user.getFlagSA().trim().equals("1")) {
                tns = TreeNode.getSiteChannelTree();
            } else {
                tns = TreeNode.getSiteChannelTree(user.getUserID(), "8");
            }
        } else {
            tns = TreeNode.getSiteChannelTree();
        }
        if (tns == null) {
            tns = new TreeNode[0];
        }
        String tempPaths = "";
        String tempPath = "";
        String selfSitePath = channelPath.substring(0, 10);
        for (int i = 0; i < tns.length; i++) {
            tempPath = tns[i].getPath();
            if (tempPath != null && tempPath.length() == 10) {
                tempPaths = tempPaths + tempPath + ",";
            } else if (ConfigInfo.getInstance().getChannelAuthority().equals("2") && tempPath != null && !tempPath.startsWith(selfSitePath)) {
                tempPaths = tempPaths + tempPath + ",";
            }
        }
        String[] sites = Function.stringToArray(tempPaths);
        int newLongth = published.length + sites.length;
        String[] disabledPaths = new String[newLongth];
        for (int j = 0; j < disabledPaths.length; j++) {
            if (j < published.length) {
                disabledPaths[j] = published[j];
            } else {
                disabledPaths[j] = sites[j - published.length];
            }
        }
        String[] links = Function.newStringArray(tns.length, "");
        String[] targets = Function.newStringArray(tns.length, "");
        TreeNode root = TreeNode.getSiteChannelTreeRoot();
        String rootLink = "";
        String rootTarget = "";
        String siteChannelTreeCode = getTreeJsCode(TreeNode.SITE_CHANNEL_TREE, root, rootLink, rootTarget, tns, links, targets, new String[0], disabledPaths, user);
        request.setAttribute("siteChannelTreeCode", siteChannelTreeCode);
    }

    private void rePublish(int[] docIds, String[] channelPaths, boolean repaixu, boolean redate, HttpServletRequest request) {
        if (docIds == null) {
            return;
        }
        ArrayList createDates = new ArrayList();
        for (int i = 0; i < docIds.length; i++) {
            try {
                DocumentPublish doc = DocumentPublish.getInstance(channelPaths[i], docIds[i]);
                createDates.add(doc.getCreateDate());
                Date validStartDate = null;
                if (request.getParameter("validStartDate") == null || request.getParameter("validStartDate").trim().equals("")) {
                    validStartDate = Function.getSysTime();
                } else {
                    validStartDate = java.sql.Timestamp.valueOf(request.getParameter("validStartDate"));
                }
                Date validEndDate = null;
                if (request.getParameter("validEndDate") == null || request.getParameter("validEndDate").trim().equals("")) {
                    validEndDate = null;
                } else {
                    validEndDate = java.sql.Timestamp.valueOf(request.getParameter("validEndDate"));
                }
                Date publishDate = null;
                if (request.getParameter("publishDate") == null || request.getParameter("publishDate").trim().equals("")) {
                    publishDate = Function.getSysTime();
                } else {
                    publishDate = java.sql.Timestamp.valueOf(request.getParameter("publishDate") + ":00.0");
                }
                LoginUser lu = new LoginUser();
                lu.setRequest(request);
                int publisher = Integer.parseInt(lu.getDefaultValue());
                doc.setPublisher(publisher);
                if (repaixu) {
                    doc.setOrderNo(Integer.parseInt(new OrderNo().getDefaultValue()));
                }
                if (redate) {
                    doc.setPublishDate(publishDate);
                }
                doc.rePublish();
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("���·����ĵ�����!docId=" + docIds[i]);
            }
        }
        try {
            new Documents().refreshChannelPages(channelPaths, createDates);
        } catch (Exception e) {
            log.error("���·����ĵ���,ˢ��Ƶ��ҳ�����!");
        }
    }

    private void rePublish(int[] docIds, String[] channelPaths, String userId, HttpServletRequest request) throws Exception {
        unPublish(docIds, channelPaths, userId, false);
        channelPaths = Function.distinct(channelPaths);
        publish(docIds, channelPaths, request, false);
    }

    private void publish(int[] docIds, String[] publishPaths, HttpServletRequest request, boolean isNextStep) throws Exception {
        Date validStartDate = null;
        if (request.getParameter("validStartDate") == null || request.getParameter("validStartDate").trim().equals("")) {
            validStartDate = Function.getSysTime();
        } else {
            validStartDate = java.sql.Timestamp.valueOf(request.getParameter("validStartDate") + ":00.000000000");
        }
        Date validEndDate = null;
        if (request.getParameter("validEndDate") == null || request.getParameter("validEndDate").trim().equals("")) {
            validEndDate = null;
        } else {
            validEndDate = java.sql.Timestamp.valueOf(request.getParameter("validEndDate") + ":00.000000000");
        }
        LoginUser lu = new LoginUser();
        lu.setRequest(request);
        int publisher = Integer.parseInt(lu.getDefaultValue());
        Date publishDate = null;
        if (request.getParameter("publishDate") == null || request.getParameter("publishDate").trim().equals("")) {
            publishDate = Function.getSysTime();
        } else {
            publishDate = java.sql.Timestamp.valueOf(request.getParameter("publishDate") + ":00.0");
        }
        new Documents().publish(docIds, publishPaths, publisher, publishDate, validStartDate, validEndDate, isNextStep);
    }

    /**
	 * ������Ľڵ����,���js����
	 * @param treeType ��������:վ��Ƶ�������ĵ�������
	 * @param root ���ĸ�ڵ����,������Ϊ��
	 * @param rootLink ���ĸ�ڵ�����ϵĳ�����
	 * @param rootTarget ���ĸ�ڵ�����ϳ����ӵ�Ŀ�괰��
	 * @param allTreeNodes �������нڵ���Ϣ,������Ϊ��
	 * @param linkList ���Ľڵ㳬������Ϣ,������Ϊ��
	 * @param targetList ���Ľڵ㳬���ӵ�Ŀ�괰�����,������Ϊ��
	 * @param defaultSelectionPaths Ĭ��ѡ�еĽڵ�path����
	 * @param disabledPaths ��Ҳ���ѡ�е�path����
	 * @param userId ��ǰ��¼�û�
	 * @return ����js����
	 * @throws java.lang.Exception
	 */
    private String getTreeJsCode(int treeType, TreeNode root, String rootLink, String rootTarget, TreeNode[] allTreeNodes, String[] linkList, String[] targetList, String[] defaultSelectionPaths, String[] disabledPaths, User user) throws Exception {
        if (root == null || allTreeNodes == null) {
            return "";
        }
        TreeJsCode tree = new TreeJsCode();
        tree.setItemType(TreeJsCode.ITEM_TYPE_CHECKBOX);
        tree.setTreeBehavior(TreeJsCode.TREE_BEHAVIOR_CLASSIC);
        tree.setDisplayCheckedNodes(true);
        if (ConfigInfo.getInstance().getChannelAuthority().equals("2")) {
            tree.setDisplayCheckedNodes(false);
        }
        tree.setRecursionChecked(false);
        tree.setTreeType(treeType);
        if (ConfigInfo.getInstance().getChannelAuthority().equals("1")) {
            if (user.getUserID() == 1 || user.getFlagSA().equals("1")) {
                tree.setTreeNodeAuthority(null);
            } else {
                tree.setTreeNodeAuthority(PublishTreeAuthority.getInstance());
            }
        } else {
            tree.setTreeNodeAuthority(null);
        }
        String[] operates = new String[1];
        operates[0] = Const.OPERATE_ID_RELEASE;
        tree.setOperates(operates);
        tree.setUserId(user.getUserID());
        tree.setTreeNodeTitleDecorate(DefaultTreeNodeTitleDecorate.getInstance());
        tree.setRoot(root);
        tree.setRootHyperlink(rootLink);
        tree.setRootTarget(rootTarget);
        tree.setTreeNodeList(allTreeNodes);
        tree.setHyperlinkList(linkList);
        tree.setTargetList(targetList);
        tree.setDefualtSelectedTreeNodePathList(defaultSelectionPaths);
        tree.setDisabledTreeNodePathList(disabledPaths);
        tree.setParentChild(false);
        return tree.getCode();
    }

    public static void main(String[] a) {
        try {
            new Documents().publish(new int[] { 56014 }, new String[] { "00000007020180200902" }, 1, Function.getSysTime(), null, null, true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}

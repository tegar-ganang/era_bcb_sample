package ces.platform.infoplat.ui.system.other.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.dao.BakAndRecoverDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.ui.common.DefaultTreeNodeTitleDecorate;
import ces.platform.infoplat.ui.common.TreeJsCode;
import ces.platform.infoplat.ui.workflow.publish.PublishTreeAuthority;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.dbaccess.User;

/**
 * @author mysheros
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PigenoholeAction extends Action {

    /**
     * ��ݹ鵵�ָ� 
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String action_status = request.getParameter("ACTION_FORM");
        if (action_status == null || action_status.equals("initial")) {
            getTreeNode(request);
            return mapping.findForward("showpigeonhole");
        } else if (action_status.trim().equals("bak")) {
            String bakChannelPath = request.getParameter("bakChannelPath").trim();
            String startDate = request.getParameter("startDate").trim();
            String endDate = request.getParameter("endDate").trim();
            try {
                new BakAndRecoverDAO().generateBakSql(bakChannelPath, startDate, endDate);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�鵵�ɹ���");
            } catch (Exception e) {
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�鵵ʧ�ܣ�");
            }
            getTreeNode(request);
            return mapping.findForward("showpigeonhole");
        } else if (action_status.trim().equals("recover")) {
            String recoverFolder = request.getParameter("recoverfolder");
            try {
                new BakAndRecoverDAO().doRecovery(recoverFolder);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�ָ��ɹ���");
            } catch (Exception e) {
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "�ָ�ʧ�ܣ�");
            }
            return mapping.findForward("recoverSuccess");
        } else {
            request.setAttribute(Const.ERROR_MESSAGE_NAME, "�޴˱��ݲ�������!");
            return mapping.findForward("error");
        }
    }

    /**
     * ��ȡƵ���б���
     * @param request
     */
    private void getTreeNode(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute("user");
        TreeNode[] tns = null;
        try {
            tns = TreeNode.getSiteChannelTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (tns == null) {
            tns = new TreeNode[0];
        }
        String[] links = Function.newStringArray(tns.length, "");
        String[] targets = Function.newStringArray(tns.length, "");
        TreeNode root = null;
        try {
            root = TreeNode.getSiteChannelTreeRoot();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        String rootLink = "";
        String rootTarget = "";
        String tempPaths = "";
        int i = 0;
        for (; i < tns.length; i++) {
            if (tns[i].getPath() != null && tns[i].getPath().length() == 10) {
                tempPaths = tempPaths + tns[i].getPath() + ",";
            }
        }
        String[] defaultSelections = new String[0];
        String[] disabledPaths = Function.stringToArray(tempPaths);
        String siteChannelTreeCode = null;
        try {
            siteChannelTreeCode = getTreeJsCode(TreeNode.SITE_CHANNEL_TREE, root, rootLink, rootTarget, tns, links, targets, defaultSelections, disabledPaths, user);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        request.setAttribute("siteChannelTreeCode", siteChannelTreeCode);
    }

    /**
     *     ���Ƶ���� 
     */
    private String getTreeJsCode(int treeType, TreeNode root, String rootLink, String rootTarget, TreeNode[] allTreeNodes, String[] linkList, String[] targetList, String[] defaultSelectionPaths, String[] disabledPaths, User user) throws Exception {
        if (root == null || allTreeNodes == null) {
            return "";
        }
        TreeJsCode tree = new TreeJsCode();
        tree.setItemType(TreeJsCode.ITEM_TYPE_CHECKBOX);
        tree.setTreeBehavior(TreeJsCode.TREE_BEHAVIOR_CLASSIC);
        tree.setDisplayCheckedNodes(true);
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
}

package ces.platform.infoplat.ui.website.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ces.coral.log.Logger;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.dao.ChannelDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.ui.common.DefaultTreeNodeTitleDecorate;
import ces.platform.infoplat.ui.common.TreeJsCode;
import ces.platform.infoplat.ui.common.defaultvalue.LoginUser;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.dbaccess.User;

public class ChannelCopyAction extends Action {

    Logger log = new Logger(this.getClass());

    private static final String SHOW_COPY_JSP = "showCopyJsp";

    private static final String COPY = "copy";

    private static final int SITE_CHANNEL_TREE = TreeNode.SITE_CHANNEL_TREE;

    public ChannelCopyAction() {
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String operate = (String) request.getParameter("operate");
        if (operate == null || operate.trim().equals("")) {
            operate = SHOW_COPY_JSP;
        }
        try {
            LoginUser lu = new LoginUser();
            lu.setRequest(request);
            int userId = Integer.parseInt(lu.getDefaultValue());
            User user = (User) request.getSession().getAttribute("user");
            String flagSA = (user != null ? user.getFlagSA() : "");
            if (SHOW_COPY_JSP.equalsIgnoreCase(operate)) {
                String orgChanPath = (String) request.getParameter("orgChanPath");
                String temp = "";
                TreeNode[] tns = TreeNode.getSiteChannelTree();
                for (int i = 0; i < tns.length; i++) {
                    if (tns[i].getPath().indexOf(orgChanPath) > -1) {
                        temp += tns[i].getPath() + ",";
                    }
                }
                String[] disabledPaths = Function.stringToArray(temp);
                String siteChannelTreeCode = getTreeJsCode(SITE_CHANNEL_TREE, TreeNode.getSiteChannelTreeRoot(), tns, new String[0], disabledPaths, user);
                request.setAttribute("siteChannelTreeCode", siteChannelTreeCode);
                return mapping.findForward("showCopyJsp");
            } else if (COPY.equalsIgnoreCase(operate)) {
                String orgChanPath = (String) request.getParameter("orgChanPath");
                String aimChanPath = (String) request.getParameter("aimChanPath");
                if (null == orgChanPath || "".equals(orgChanPath) || null == aimChanPath || "".equals(aimChanPath)) {
                    throw new Exception("����ԭƵ������Ŀ��Ƶ��Ϊ�գ�");
                }
                if (aimChanPath.indexOf(orgChanPath) > -1) {
                    throw new Exception("����Ŀ��Ƶ��Ϊ����Ƶ������Ƶ����");
                }
                int maxLevel = new ChannelDAO().getChannelMaxLevel(orgChanPath);
                if (maxLevel - (orgChanPath.length() - aimChanPath.length()) / 5 > Const.CHANNEL_PATH_MAX_LEVEL) {
                    throw new Exception("����Ŀ��Ƶ������̫�Ŀǰϵͳֻ֧�� " + Const.CHANNEL_PATH_MAX_LEVEL + " �㣡");
                }
                new Channel().copy(orgChanPath, aimChanPath);
                request.setAttribute(Const.SUCCESS_MESSAGE_NAME, "Ƶ�����Ƴɹ�");
                return mapping.findForward("operateSuccess");
            } else {
                throw new Exception("δ֪�Ĳ�������:" + operate + ",����Ƶ��������صĴ���!");
            }
        } catch (Exception ex) {
            log.error("Ƶ������ʧ��!", ex);
            request.setAttribute(Const.ERROR_MESSAGE_NAME, "Ƶ������ʧ��!<br>" + ex.getMessage());
            return mapping.findForward("error");
        }
    }

    /**
	 * ������Ľڵ����,���js����
	 * @param treeType ��������:վ��Ƶ�������ĵ�������
	 * @param root ���ĸ�ڵ����,������Ϊ��
	 * @param allTreeNodes �������нڵ���Ϣ,������Ϊ��
	 * @param defaultSelectionPaths Ĭ��ѡ�еĽڵ�path����
	 * @param disabeldPaths ��Ҳ���ѡ�е�path����
	 * @param userId ��ǰ��¼�û�
	 * @return ����js����
	 * @throws java.lang.Exception
	 */
    private String getTreeJsCode(int treeType, TreeNode root, TreeNode[] allTreeNodes, String[] defaultSelectionPaths, String[] disabeldPaths, User user) throws Exception {
        if (root == null || allTreeNodes == null) {
            return "";
        }
        TreeJsCode tree = new TreeJsCode();
        tree.setItemType(TreeJsCode.ITEM_TYPE_RADIO);
        tree.setTreeBehavior(TreeJsCode.TREE_BEHAVIOR_CLASSIC);
        tree.setTreeType(treeType);
        if (treeType == SITE_CHANNEL_TREE) {
            if (user.getUserID() == 1 || user.getFlagSA().equals("1")) {
                tree.setTreeNodeAuthority(null);
            } else {
                tree.setTreeNodeAuthority(null);
            }
        } else {
            tree.setTreeNodeAuthority(null);
        }
        String[] operates = new String[1];
        operates[0] = Const.OPERATE_ID_COLLECT;
        tree.setOperates(operates);
        tree.setUserId(user.getUserID());
        tree.setTreeNodeTitleDecorate(DefaultTreeNodeTitleDecorate.getInstance());
        tree.setRoot(root);
        tree.setRootHyperlink("");
        tree.setRootTarget("");
        tree.setTreeNodeList(allTreeNodes);
        String[] links = Function.newStringArray(allTreeNodes.length, "");
        tree.setHyperlinkList(links);
        String[] targetList = Function.newStringArray(allTreeNodes.length, "");
        tree.setTargetList(targetList);
        tree.setDefualtSelectedTreeNodePathList(defaultSelectionPaths);
        tree.setDisabledTreeNodePathList(disabeldPaths);
        return tree.getCode();
    }
}

package ces.platform.infoplat.ui.website.action;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ces.coral.log.Logger;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.Site;
import ces.platform.infoplat.core.SiteChannelDocTypeRelation;
import ces.platform.infoplat.core.Template;
import ces.platform.infoplat.core.Templates;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.dao.ChannelDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.ui.common.DefaultTreeNodeTitleDecorate;
import ces.platform.infoplat.ui.common.TreeJsCode;
import ces.platform.infoplat.ui.common.defaultvalue.OrderNo;
import ces.platform.infoplat.ui.website.defaultvalue.ChannelPath;
import ces.platform.infoplat.ui.website.form.ChannelpropForm;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.common.Constant;
import ces.platform.system.common.IdGenerator;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: ���channelProp��Ĭ��ֵ��</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 * @mender ��ʢ
 */
public class ChannelPropAction extends Action {

    /** �Ƿ񴴽��ļ��� */
    private boolean createFolder = false;

    private String splitTag = "#";

    Logger log = new Logger(getClass());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        String status = request.getParameter("status");
        String path = request.getParameter("treepath");
        createFolder = false;
        if ("ON".equals(request.getParameter("createfolder")) || "on".equals(request.getParameter("createfolder"))) {
            log.debug("create folder");
            createFolder = true;
        }
        String resultMapping = null;
        if (status.trim().equalsIgnoreCase("getData")) {
            try {
                Channel channel = (Channel) TreeNode.getInstance(path);
                String cPath = channel.getPath();
                int cLevel = cPath.length() / 5;
                TreeNode[] tns = TreeNode.getDocTypeTree();
                String[] docTypes = SiteChannelDocTypeRelation.getDocTypePaths(path, true);
                HashMap map = SiteChannelDocTypeRelation.getDocTypePathsAndShowTemplateIds(path, true);
                TreeNode[] docTypeNodeList = TreeNode.getDocTypeTree();
                if (docTypeNodeList == null) {
                    docTypeNodeList = new TreeNode[0];
                }
                String[] links = Function.newStringArray(docTypeNodeList.length, "javascript:CoralCheckBoxTreeItem.oncheck(); void(0);");
                String[] targetList = Function.newStringArray(docTypeNodeList.length, "");
                TreeNode root = TreeNode.getDocTypeTreeRoot();
                String rootLink = "";
                String rootTarget = "";
                String docTypeTree = this.getTreeJsCode(TreeJsCode.DOC_TYPE_TREE, root, rootLink, rootTarget, docTypeNodeList, links, targetList, docTypes, new String[0], map);
                request.setAttribute("previewURL", ((new ChannelDAO().getPreviewUrl(channel.getPath(), channel.getTemplateId()))));
                Map loadSelfDefine = new ChannelDAO().initAddChannelSelfDefine(cPath);
                request.setAttribute("SelfDefineInitial", loadSelfDefine);
                request.setAttribute("templateList", getModelTempalteList());
                request.setAttribute("templateListValue", channel.getTemplateId());
                request.setAttribute("pageNumValue", String.valueOf(channel.getPageNum()));
                request.setAttribute("docTypeTree", docTypeTree);
                request.setAttribute("result", channel);
                request.setAttribute("saveStatus", "modify");
                request.setAttribute("parentPath", cPath.substring(0, (cLevel - 1) * 5));
            } catch (Exception ex) {
                log.error("��ȡƵ�����Գ���!", ex);
                request.setAttribute(Const.ERROR_MESSAGE_NAME, ex.toString());
                mapping.findForward("error");
            }
        } else if (status.trim().equalsIgnoreCase("saveData")) {
            try {
                String saveStatus = request.getParameter("saveStatus");
                String parentPath = request.getParameter("parentPath");
                ChannelpropForm cForm = (ChannelpropForm) form;
                Channel channel = this.saveData(cForm, saveStatus, path, parentPath);
                String checkedValues = request.getParameter("checkedValues");
                String[] doctypes = Function.stringToArray(checkedValues, ",");
                String[] templateIds = Function.stringToArray(request.getParameter("templateIds"), ",");
                SiteChannelDocTypeRelation.addBySiteChannelPath(channel.getPath(), doctypes, templateIds);
                TreeNode[] tns = TreeNode.getDocTypeTree();
                if (tns == null) {
                    tns = new TreeNode[0];
                }
                HashMap map = SiteChannelDocTypeRelation.getDocTypePathsAndShowTemplateIds(channel.getPath(), true);
                String[] links = Function.newStringArray(tns.length, "javascript:CoralCheckBoxTreeItem.oncheck(); void(0);");
                String[] targetList = Function.newStringArray(tns.length, "");
                TreeNode root = TreeNode.getDocTypeTreeRoot();
                String rootLink = "";
                String rootTarget = "";
                String docTypeTree = this.getTreeJsCode(TreeJsCode.DOC_TYPE_TREE, root, rootLink, rootTarget, tns, links, targetList, doctypes, new String[0], map);
                Map selfDefineInitial = selfDefineLoad(cForm.getOrder_no(), cForm.getField(), cForm.getField_name());
                request.setAttribute("templateList", getModelTempalteList());
                request.setAttribute("templateListValue", cForm.getTemplate_id());
                request.setAttribute("SelfDefineInitial", selfDefineInitial);
                request.setAttribute("previewURL", ((new ChannelDAO().getPreviewUrl(channel.getPath(), channel.getTemplateId()))));
                request.setAttribute("docTypeTree", docTypeTree);
                request.setAttribute("result", channel);
                request.setAttribute("parentPath", parentPath);
                request.setAttribute("saveStatus", "modify");
            } catch (Exception ex) {
                log.error("����Ƶ�����Գ���!", ex);
                request.setAttribute(Const.ERROR_MESSAGE_NAME, ex.toString());
                mapping.findForward("error");
            }
        } else {
            try {
                Channel channel = new Channel();
                channel.setName("");
                channel.setAsciiName("");
                channel.setPath("");
                channel.setStyle("");
                channel.setDataUrl("");
                channel.setUseStatus("1");
                channel.setDescription("");
                channel.setCompletePath("");
                channel.setPageNum(10);
                channel.setOrderNo(Integer.parseInt(new OrderNo().getDefaultValue()));
                TreeNode[] tns = TreeNode.getDocTypeTree();
                if (tns == null) {
                    tns = new TreeNode[0];
                }
                String[] links = Function.newStringArray(tns.length, "javascript:CoralCheckBoxTreeItem.oncheck(); void(0);");
                String[] targetList = Function.newStringArray(tns.length, "");
                TreeNode root = TreeNode.getDocTypeTreeRoot();
                String rootLink = "";
                String rootTarget = "";
                String docTypeTree = this.getTreeJsCode(TreeJsCode.DOC_TYPE_TREE, root, rootLink, rootTarget, tns, links, targetList, new String[0], new String[0], null);
                request.setAttribute("docTypeTree", docTypeTree);
                request.setAttribute("result", channel);
                request.setAttribute("saveStatus", "add");
                String parentPath = request.getParameter("parentPath");
                request.setAttribute("templateList", getModelTempalteList());
                request.setAttribute("templateListValue", "");
                Map map = new ChannelDAO().initAddChannelSelfDefine(parentPath);
                request.setAttribute("parentPath", parentPath);
                request.setAttribute("SelfDefineInitial", map);
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("����Ƶ������!", ex);
                request.setAttribute(Const.ERROR_MESSAGE_NAME, ex.toString());
                mapping.findForward("error");
            }
        }
        return mapping.findForward("channelprop");
    }

    /**
     * �����������������ݵķ�������״̬ΪsaveData��ʱ�����
     * @param cForm  ��jsp�ϴ����actionForm������ż����������channel������ֵ
     * @param saveStatus  ˵�������״̬ add,update����
     * @param cPath  �Ѿ����ڵ�channel·��
     */
    public Channel saveData(ChannelpropForm cForm, String saveStatus, String cPath, String parentPath) throws UnsupportedEncodingException, Exception {
        Channel channel = new Channel();
        if (saveStatus.trim().equalsIgnoreCase("add")) {
            ChannelPath channelPath = new ChannelPath();
            channelPath.setCPath(parentPath);
            String treeId = channelPath.getDefaultValue();
            channel.setPath(parentPath + treeId);
            int level = parentPath.length() / 5;
            int channelId = (int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_CHANNEL);
            String parentId = parentPath.substring((level - 1) * 5, parentPath.length());
            String sitePath = parentPath.substring(0, 10);
            int siteID = ((Site) TreeNode.getInstance(sitePath)).getSiteID();
            channel.setSiteId(siteID);
            channel.setId(treeId);
            channel.setLevel(level);
            channel.setChannelID(channelId);
            channel.setParentId(parentId);
            channel.setChannelType("0");
            channel.setRefPath(cForm.getRefreshFlag());
        } else {
            channel = (Channel) TreeNode.getInstance(cPath);
        }
        channel.setName(Function.convertCharset(cForm.getChannelname(), Constant.PARAM_FORMBEAN));
        channel.setTitle(Function.convertCharset(cForm.getChannelname(), Constant.PARAM_FORMBEAN));
        channel.setDescription(Function.convertCharset(cForm.getDescription(), Constant.PARAM_FORMBEAN));
        channel.setOrderNo(Integer.parseInt(cForm.getNum()));
        channel.setUseStatus(cForm.getUsestatus());
        channel.setRefresh(cForm.getRefreshFlag());
        channel.setPageNum(Integer.parseInt(cForm.getPageNum().equals("") ? "0" : cForm.getPageNum()));
        channel.setTemplateId(cForm.getTemplate_id());
        channel.setSelfDefineList(titleSelfDefine(cForm.getOrder_no(), cForm.getField(), cForm.getField_name()));
        if (saveStatus.trim().equalsIgnoreCase("add")) {
            channel.setAsciiName(cForm.getChannelpath() + "_" + channel.getChannelID());
            channel.add(this.createFolder);
        } else {
            channel.setAsciiName(cForm.getChannelpath());
            channel.setExtendParent(cForm.getExtendparent());
            channel.update();
        }
        channel = (Channel) TreeNode.getInstance(channel.getPath());
        return channel;
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
     * @return ����js����
     * @throws java.lang.Exception
     */
    private String getTreeJsCode(int treeType, TreeNode root, String rootLink, String rootTarget, TreeNode[] allTreeNodes, String[] linkList, String[] targetList, String[] defaultSelectionPaths, String[] disabledPaths, HashMap defineData) throws Exception {
        if (root == null || allTreeNodes == null) {
            return "";
        }
        TreeJsCode tree = new TreeJsCode();
        tree.setItemType(TreeJsCode.ITEM_TYPE_CHECKBOX);
        tree.setTreeBehavior(TreeJsCode.TREE_BEHAVIOR_CLASSIC);
        tree.setDisplayCheckedNodes(true);
        tree.setTreeType(treeType);
        tree.setTreeNodeAuthority(null);
        tree.setTreeNodeTitleDecorate(DefaultTreeNodeTitleDecorate.getInstance());
        tree.setRoot(root);
        tree.setRootHyperlink(rootLink);
        tree.setRootTarget(rootTarget);
        tree.setTreeNodeList(allTreeNodes);
        tree.setHyperlinkList(linkList);
        tree.setTargetList(targetList);
        tree.setDefualtSelectedTreeNodePathList(defaultSelectionPaths);
        tree.setDisabledTreeNodePathList(disabledPaths);
        tree.setRecursionChecked(false);
        tree.setDefineData(defineData);
        tree.setParentChild(false);
        return tree.getCode();
    }

    private List titleSelfDefine(String[] order_no, String[] field, String[] field_name) {
        List list = new ArrayList();
        for (int i = 0; i < order_no.length; i++) {
            Map map = new HashMap();
            map.put("order_no", order_no[i]);
            map.put("field", field[i]);
            try {
                map.put("field_name", Function.convertCharset(field_name[i], Constant.PARAM_FORMBEAN));
            } catch (UnsupportedEncodingException e) {
                map.put("field_name", "ת������쳣");
            }
            list.add(map);
        }
        return list;
    }

    private Map selfDefineLoad(String[] order_no, String[] field, String[] field_name) {
        Map map = new HashMap();
        for (int i = 0; i < order_no.length; i++) {
            try {
                map.put(field[i].toUpperCase(), order_no[i] + splitTag + Function.convertCharset(field_name[i], Constant.PARAM_FORMBEAN));
            } catch (UnsupportedEncodingException e) {
                map.put(field[i].toUpperCase(), order_no[i] + splitTag + "�ַ�ת�������쳣");
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * ��ȡƵ��ģ���Զ���
     * @return
     */
    private String getModelTempalteList() {
        String showTemplateList = "";
        Template[] templates = new Templates().getTemplateList(2204);
        for (int i = 0; templates != null && i < templates.length; i++) {
            if (i == 0) {
                showTemplateList = templates[i].getName() + "$=" + templates[i].getId();
            } else {
                showTemplateList += ";" + templates[i].getName() + "$=" + templates[i].getId();
            }
        }
        templates = new Templates().getTemplateList(2205);
        for (int i = 0; templates != null && i < templates.length; i++) {
            if (showTemplateList.equals("")) {
                showTemplateList = templates[i].getName() + "$=" + templates[i].getId();
            } else {
                showTemplateList += ";" + templates[i].getName() + "$=" + templates[i].getId();
            }
        }
        return showTemplateList;
    }
}

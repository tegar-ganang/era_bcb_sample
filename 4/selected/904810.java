package ces.platform.infoplat.ui.system.update;

import java.sql.*;
import java.util.*;
import ces.coral.file.*;
import ces.coral.log.*;
import ces.platform.infoplat.core.tree.*;
import ces.platform.infoplat.ui.common.*;
import ces.platform.infoplat.utils.*;

/**
 *  Description of the Class
 *
 *@author     Administrator
 *@created    2003��12��24��
 */
public class Update2x {

    private static String cesHome = CesGlobals.getCesHome();

    private String dbDriver;

    private String dbConnectString;

    private String dbUser;

    private String dbPassword;

    private String installPath;

    Connection conn = null;

    private String strDocTypeRelation;

    private String strChannelRelation;

    private static Logger logger = new Logger(Update2x.class);

    /**
     *  the default constuctor of the class
     */
    public Update2x() {
        dbConnectString = null;
        dbUser = null;
        dbPassword = null;
        installPath = null;
    }

    /**
     *  Description of the Method
     */
    public void printMe() {
        logger.debug("**************");
        logger.debug("dbConnectString=" + dbConnectString);
        logger.debug("dbUser=" + dbUser);
        logger.debug("dbPassword=" + dbPassword);
        logger.debug("installPath=" + installPath);
        logger.debug("**************");
    }

    public void prepareData() {
    }

    /**
     * ����ϰ汾��Ϣ�������
     * @return
     */
    public Vector GetInfoplatType() throws SQLException {
        try {
            Class.forName(dbDriver);
            conn = DriverManager.getConnection(dbConnectString, dbUser, dbPassword);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        Vector vcInfoplatType = new Vector();
        if (conn != null) {
            String strSql = "select t.infotype_id,t.parenttype_name,t.infotype_name,count(b.infotype_id) " + "from t_infoplat_doctype t,t_infoplat_btc b " + "where t.infotype_id=b.infotype_id " + "group by t.infotype_id,t.parenttype_name,t.infotype_name " + "order by t.parenttype_name";
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(strSql);
            for (int i = 0; rs.next(); i++) {
                String[] temp = new String[2];
                temp[0] = rs.getString(1);
                temp[1] = rs.getString(2);
                temp[1] = temp[1] + "." + rs.getString(3);
                temp[1] = temp[1] + "(" + rs.getString(4) + ")";
                vcInfoplatType.add(temp);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex1) {
            }
        }
        return vcInfoplatType;
    }

    /**
     * ����°汾�ĵ�������
     * @return
     */
    public String getDocTypeTreeJsCode() {
        TreeNode[] docTypeNodeList = null;
        String[] links = null;
        String[] targetList = null;
        TreeNode root = null;
        String rootLink = null;
        String rootTarget = null;
        try {
            docTypeNodeList = TreeNode.getDocTypeTree();
            if (docTypeNodeList == null) {
                docTypeNodeList = new TreeNode[0];
            }
            links = Function.newStringArray(docTypeNodeList.length, "javascript:CheckBoxTreeItem.oncheck(); void(0);");
            targetList = Function.newStringArray(docTypeNodeList.length, "");
            root = TreeNode.getDocTypeTreeRoot();
            rootLink = "";
            rootTarget = "";
        } catch (Exception ex) {
        }
        try {
            return getDocTypeTreeJsCode(TreeJsCode.DOC_TYPE_TREE, root, rootLink, rootTarget, docTypeNodeList, links, targetList);
        } catch (Exception ex1) {
            return null;
        }
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
    private String getDocTypeTreeJsCode(int treeType, TreeNode root, String rootLink, String rootTarget, TreeNode[] allTreeNodes, String[] linkList, String[] targetList) throws Exception {
        if (root == null || allTreeNodes == null) {
            return "";
        }
        TreeJsCode tree = new TreeJsCode();
        tree.setItemType(TreeJsCode.ITEM_TYPE_RADIO);
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
        return tree.getCode();
    }

    /**
     *  ����°汾Ƶ����
     * @param <any>
     * @return
     */
    public String getChannelTreeJsCode() {
        String siteChannelTree = null;
        try {
            TreeNode[] siteChannelNodeList = TreeNode.getSiteChannelTree();
            if (siteChannelNodeList == null) {
                siteChannelNodeList = new TreeNode[0];
            }
            TreeNode root = TreeNode.getSiteChannelTreeRoot();
            String rootLink = "";
            String rootTarget = "";
            siteChannelTree = this.getChannelTreeJsCode(TreeJsCode.SITE_CHANNEL_TREE, root, siteChannelNodeList);
        } catch (Exception ex) {
        }
        return siteChannelTree;
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
    private String getChannelTreeJsCode(int treeType, TreeNode root, TreeNode[] allTreeNodes) throws Exception {
        if (root == null || allTreeNodes == null) {
            return "";
        }
        TreeJsCode tree = new TreeJsCode();
        tree.setItemType(TreeJsCode.ITEM_TYPE_RADIO);
        tree.setTreeBehavior(TreeJsCode.TREE_BEHAVIOR_CLASSIC);
        tree.setTreeType(treeType);
        tree.setTreeNodeTitleDecorate(DefaultTreeNodeTitleDecorate.getInstance());
        tree.setRoot(root);
        tree.setTreeNodeList(allTreeNodes);
        return tree.getCode();
    }

    /**
    * GET/SET����
    * @return
    */
    public String getDbConnectString() {
        return dbConnectString;
    }

    /**
	 * GET/SET����
	 * @return
     */
    public void setDbConnectString(String dbConnectString) {
        this.dbConnectString = dbConnectString;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public String getDbUser() {
        return dbUser;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    /**
	 *  Sets the installPath attribute of the Update2x object
	 *
	 *@param  installPath  The new installPath value
	 */
    public void setInstallPath(String installPath) {
        if (installPath != null) {
            String strTemp = installPath.substring(installPath.length());
            if (strTemp != null && !strTemp.equals("/") && !strTemp.equals("\\")) {
                installPath += "/";
            }
        }
        this.installPath = installPath;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public String getInstallPath() {
        return installPath;
    }

    /**
	 * @return
	 */
    public Logger getLogger() {
        return logger;
    }

    /**
	 *  Sets the logger attribute of the Update2x object
	 *
	 *@param  logger  The new logger value
	 */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public String getDbDriver() {
        return dbDriver;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public String getStrChannelRelation() {
        return strChannelRelation;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public void setStrChannelRelation(String strChannelRelation) {
        this.strChannelRelation = strChannelRelation;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public void setStrDocTypeRelation(String strDocTypeRelation) {
        this.strDocTypeRelation = strDocTypeRelation;
    }

    /**
	 * GET/SET����
	 * @return
	 */
    public String getStrDocTypeRelation() {
        return strDocTypeRelation;
    }

    /**
     * ����Ǩ����ݵĺ���
     */
    public void updateData() {
        UpdateData ud = new UpdateData(this);
        ud.importData();
    }
}

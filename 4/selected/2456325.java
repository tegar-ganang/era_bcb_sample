package com.javaeye.lonlysky.lforum.service.admin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import com.javaeye.lonlysky.lforum.comm.utils.Utils;
import com.javaeye.lonlysky.lforum.comm.utils.XmlElementUtil;
import com.javaeye.lonlysky.lforum.config.impl.ConfigLoader;

/**
 * 后台主菜单操作
 * 
 * @author 黄磊
 *
 */
public class MenuManager {

    public static final String CONFIG_PATH = ConfigLoader.getConfig().getWebpath() + "admin/xml/navmenu.xml";

    /**
	 * 增加主菜单
	 * @param title 主菜单标题
	 * @param defaulturl 主菜单默认展开的页面
	 * @return 新主菜单项ID
	 */
    public static int newMainMenu(String title, String defaulturl) {
        Document document = XmlElementUtil.readXML(CONFIG_PATH);
        int newMenuId = XmlElementUtil.findElements("toptabmenu", document.getRootElement()).size() + 1;
        Element newMainMenuItem = document.getRootElement().addElement("toptabmenu");
        Element node = newMainMenuItem.addElement("id");
        node.setText(newMenuId + "");
        node = newMainMenuItem.addElement("title");
        node.setText(title);
        node = newMainMenuItem.addElement("mainmenulist");
        node.setText("");
        node = newMainMenuItem.addElement("mainmenuidlist");
        node.setText("");
        node = newMainMenuItem.addElement("defaulturl");
        node.setText(defaulturl);
        node = newMainMenuItem.addElement("system");
        node.setText("0");
        XmlElementUtil.saveXML(CONFIG_PATH, document);
        return newMenuId;
    }

    /**
	 * 编辑一级主菜单
	 * @param menuid 一级主菜单的ID
	 * @param title 一级主菜单标题
	 * @param defaulturl 一级主菜单默认展开的页面
	 * @return 操作成功否
	 */
    public static boolean editMainMenu(int menuid, String title, String defaulturl) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        String xpath = "/dataset/toptabmenu[id=\"" + menuid + "\"]";
        if (doc.selectSingleNode(xpath) != null) {
            doc.selectSingleNode(xpath + "/title").setText(title);
            doc.selectSingleNode(xpath + "/defaulturl").setText(defaulturl);
            XmlElementUtil.saveXML(CONFIG_PATH, doc);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * 编辑主菜单
	 * @param oldMainMenuTitle 旧主菜单标题
	 * @param newMainMenuTitl 新主菜单标题
	 * @param defaulturl 默认展开的页面
	 * @return
	 */
    public static boolean editMainMenu(String oldMainMenuTitle, String newMainMenuTitle, String defaulturl) {
        int mainid = findMenuID(oldMainMenuTitle);
        if (mainid == -1) return false;
        return editMainMenu(mainid, newMainMenuTitle, defaulturl);
    }

    /**
	 * 删除一级菜单，其下子菜单必须为空
	 * @param menuid  要删除的一级菜单ID
	 * @return 操作成功否
	 */
    public static boolean deleteMainMenu(int menuid) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        List<Element> mainmenus = XmlElementUtil.findElements("toptabmenu", doc.getRootElement());
        boolean result = false;
        Element delMenu = null;
        int newid = menuid;
        for (Element menuitem : mainmenus) {
            if (menuitem.elementTextTrim("id").equals(Utils.null2String(menuid))) {
                if (!menuitem.elementTextTrim("mainmenulist").equals("")) {
                    return false;
                }
                delMenu = menuitem;
                doc.getRootElement().remove(menuitem);
                result = true;
                break;
            } else {
                if (delMenu != null) {
                    doc.selectSingleNode("/dataset/toptabmenu[id=\"" + menuitem.elementTextTrim("id") + "\"]/id").setText(newid + "");
                    newid++;
                }
            }
        }
        if (result) {
            XmlElementUtil.saveXML(CONFIG_PATH, doc);
        }
        return result;
    }

    /**
	 * 删除主菜单
	 * @param menuTitle 要删除的主菜单标题
	 * @return 操作成功否
	 */
    public static boolean deleteMainMenu(String menuTitle) {
        int mainId = findMenuID(menuTitle);
        if (mainId == -1) return false;
        return deleteMainMenu(mainId);
    }

    /**
	 * 编辑子菜单
	 * @param submenuid 子菜单ID
	 * @param menutitle 子菜单标题
	 * @return 操作成功否
	 */
    public static boolean editSubMenu(int submenuid, String menutitle) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        String xpath = "/dataset/mainmenu[id=\"" + submenuid + "\"]";
        if (doc.selectSingleNode(xpath) != null) {
            doc.selectSingleNode(xpath + "/title").setText(menutitle);
            XmlElementUtil.saveXML(CONFIG_PATH, doc);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * 编辑子菜单
	 * @param mainMenuTitle 主菜单标题
	 * @param oldSubMenuTitle 旧子菜单标题
	 * @param newSubMenuTitle 新子菜单标题
	 * @return 操作成功否
	 */
    public static boolean editSubMenu(String mainMenuTitle, String oldSubMenuTitle, String newSubMenuTitle) {
        int subid = findMenuID(mainMenuTitle, oldSubMenuTitle);
        if (subid == -1) return false;
        return editSubMenu(subid, newSubMenuTitle);
    }

    /**
	 * 增加子菜单
	 * @param mainmenuid 主菜单ID
	 * @param menutitle 子菜单标题
	 * @return 新建子菜单ID
	 */
    public static int newSubMenu(int mainmenuid, String menutitle) {
        int newid = 1;
        int newmenuid = 100;
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        List<Element> submains = XmlElementUtil.findElements("mainmenu", doc.getRootElement());
        newid += Utils.null2Int(submains.get(submains.size() - 1).elementTextTrim("id"), 0);
        newmenuid += Utils.null2Int(submains.get(submains.size() - 1).elementTextTrim("menuid"), 0);
        Element mainmenu = doc.getRootElement().addElement("mainmenu");
        Element node = mainmenu.addElement("id");
        node.setText(newid + "");
        node = mainmenu.addElement("menuid");
        node.setText(newmenuid + "");
        node = mainmenu.addElement("menutitle");
        node.setText(menutitle);
        String xpath = "/dataset/toptabmenu[id=\"" + mainmenuid + "\"]";
        if (doc.selectSingleNode(xpath) != null) {
            Node maimenulist = doc.selectSingleNode(xpath + "/mainmenulist");
            Node mainmenuidlist = doc.selectSingleNode(xpath + "/mainmenuidlist");
            doc.selectSingleNode(xpath + "/mainmenulist").setText(maimenulist.getText() + "," + newid);
            doc.selectSingleNode(xpath + "/mainmenuidlist").setText(mainmenuidlist.getText() + "," + newmenuid);
            doc.selectSingleNode(xpath + "/mainmenulist").setText(StringUtils.strip(doc.selectSingleNode(xpath + "/mainmenulist").getText(), ","));
            doc.selectSingleNode(xpath + "/mainmenuidlist").setText(StringUtils.strip(doc.selectSingleNode(xpath + "/mainmenuidlist").getText(), ","));
        }
        XmlElementUtil.saveXML(CONFIG_PATH, doc);
        return newmenuid;
    }

    /**
	 * 增加子菜单
	 * @param mainMenuTitle 主菜单标题
	 * @param newSubMenuTitle 新增子菜单标题
	 * @return 操作成功否
	 */
    public static int newSubMenu(String mainMenuTitle, String newSubMenuTitle) {
        int mainid = findMenuID(mainMenuTitle);
        if (mainid == -1) return -1;
        return newSubMenu(mainid, newSubMenuTitle);
    }

    /**
	 * 删除子菜单
	 * @param submenuid 子菜单ID
	 * @param mainmenuid 主菜单ID
	 * @return 操作成功否
	 */
    public static boolean deleteSubMenu(int submenuid, int mainmenuid) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        String xpath = "/dataset/mainmenu[id=\"" + submenuid + "\"]";
        String menuid = "";
        boolean result = false;
        if (doc.selectSingleNode(xpath) != null) {
            menuid = doc.selectSingleNode(xpath + "/menuid").getText();
            if (doc.selectSingleNode("/dataset/submain[menuparentid=\"" + menuid + "\"]") != null) {
                return false;
            }
            doc.getRootElement().remove(doc.selectSingleNode(xpath));
            result = true;
        }
        xpath = "/dataset/toptabmenu[id=\"" + mainmenuid + "\"]";
        if (doc.selectSingleNode(xpath) != null) {
            Node maimenulist = doc.selectSingleNode(xpath + "/mainmenulist");
            Node mainmenuidlist = doc.selectSingleNode(xpath + "/mainmenuidlist");
            doc.selectSingleNode(xpath + "/mainmenulist").setText(("," + maimenulist.getText() + ",").replace("," + submenuid + ",", ","));
            doc.selectSingleNode(xpath + "/mainmenuidlist").setText(("," + mainmenuidlist.getText() + ",").replace("," + menuid + ",", ","));
            doc.selectSingleNode(xpath + "/mainmenulist").setText(StringUtils.strip(doc.selectSingleNode(xpath + "/mainmenulist").getText(), ","));
            doc.selectSingleNode(xpath + "/mainmenuidlist").setText(StringUtils.strip(doc.selectSingleNode(xpath + "/mainmenuidlist").getText(), ","));
        }
        if (result) {
            XmlElementUtil.saveXML(CONFIG_PATH, doc);
        }
        return result;
    }

    /**
	 * 删除子菜单
	 * @param mainMenuTitle 主菜单标题
	 * @param subMenuTitle 要删除的子菜单标题
	 * @return
	 */
    public static boolean deleteSubMenu(String mainMenuTitle, String subMenuTitle) {
        int mainId = findMenuID(mainMenuTitle);
        int subId = findMenuID(mainMenuTitle, subMenuTitle);
        if (mainId == -1 || subId == -1) return false;
        return deleteSubMenu(subId, mainId);
    }

    /**
	 * 新建菜单项
	 * @param menuparentid 父菜单ID
	 * @param title 菜单标题
	 * @param link 菜单链接
	 * @return 操作成功否
	 */
    public static boolean newMenuItem(int menuparentid, String title, String link) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        if (doc.selectSingleNode("/dataset/submain[link=\"" + link + "\"]") != null) {
            return false;
        }
        Element submain = doc.getRootElement().addElement("submain");
        Element node = submain.addElement("menuparentid");
        node.setText(menuparentid + "");
        node = submain.addElement("menutitle");
        node.setText(title);
        node = submain.addElement("link");
        node.setText(link);
        node = submain.addElement("frameid");
        node.setText("main");
        XmlElementUtil.saveXML(CONFIG_PATH, doc);
        return true;
    }

    /**
	 * 新建菜单项
	 * @param mainMenuTitle 主菜单标题
	 * @param subMenutitle 子菜单标题
	 * @param newMenuItemTitle 新增菜单项标题
	 * @param link 菜单项的链接页面
	 * @return 操作成功否
	 */
    public static boolean newMenuItem(String mainMenuTitle, String subMenutitle, String newMenuItemTitle, String link) {
        int subid = findMenuMenuid(mainMenuTitle, subMenutitle);
        if (subid == -1) return false;
        return newMenuItem(subid, newMenuItemTitle, link);
    }

    /**
	 * 编辑菜单项
	 * @param id 菜单项的序号
	 * @param title 菜单项的标题
	 * @param link 菜单项的链接
	 * @return 操作成功否
	 */
    public static boolean editMenuItem(int id, String title, String link) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        List<Element> submains = XmlElementUtil.findElements("submain", doc.getRootElement());
        int rowcount = 0;
        for (Element sub : submains) {
            if (rowcount != id && sub.elementTextTrim("link").equals(link)) {
                return false;
            }
            rowcount++;
        }
        String tmpLink = submains.get(id).elementText("link");
        submains.get(id).element("menutitle").setText(title);
        submains.get(id).element("link").setText(link);
        List<Element> shortcuts = XmlElementUtil.findElements("shortcut", doc.getRootElement());
        for (Element shortmenuitem : shortcuts) {
            if (shortmenuitem.elementText("link").equals(tmpLink)) {
                shortmenuitem.element("link").setText(link);
                shortmenuitem.element("menutitle").setText(title);
                break;
            }
        }
        XmlElementUtil.saveXML(CONFIG_PATH, doc);
        return true;
    }

    /**
	 * 编辑菜单项
	 * @param mainMenuTitle 主菜单标题
	 * @param subMenuTitle 子菜单标题
	 * @param oldItemTitle 旧菜单项标题
	 * @param newItemTitle 新菜单项标题
	 * @param link 菜单项的链接
	 * @return 操作成功否
	 */
    public static boolean editMenuItem(String mainMenuTitle, String subMenuTitle, String oldItemTitle, String newItemTitle, String link) {
        int itemid = findMenuID(mainMenuTitle, subMenuTitle, oldItemTitle);
        if (itemid == -1) return false;
        return editMenuItem(itemid, newItemTitle, link);
    }

    /**
	 * 删除菜单项
	 * @param id 菜单项的序号
	 */
    public static void deleteMenuItem(int id) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        List<Element> submains = XmlElementUtil.findElements("submain", doc.getRootElement());
        String link = submains.get(id).elementText("link");
        doc.getRootElement().remove(submains.get(id));
        List<Element> shortcuts = XmlElementUtil.findElements("shortcut", doc.getRootElement());
        for (Element shortmenuitem : shortcuts) {
            if (shortmenuitem.elementText("link").equals(link)) {
                doc.getRootElement().remove(shortmenuitem);
                break;
            }
        }
        XmlElementUtil.saveXML(CONFIG_PATH, doc);
    }

    /**
	 * 删除菜单项
	 * @param mainMenuTitle 主菜单标题
	 * @param subMenuTitle 子菜单标题
	 * @param menuItemTitle 要删除的菜单项标题
	 * @return 操作成功否
	 */
    public static boolean deleteMenuItem(String mainMenuTitle, String subMenuTitle, String menuItemTitle) {
        int itemId = findMenuID(mainMenuTitle, subMenuTitle, menuItemTitle);
        if (itemId == -1) return false;
        deleteMenuItem(itemId);
        return true;
    }

    private static int findMenuID(String mainMenuTitle, String subMenuTitle, String menuItemTitle) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        String mainmenulist = "";
        if (doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]") == null) {
            return -1;
        } else {
            mainmenulist = doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]/mainmenulist").getText();
        }
        boolean find = false;
        String menuid = "";
        List<Element> submains = XmlElementUtil.findElements("mainmenu", doc.getRootElement());
        for (Element menuItem : submains) {
            if (("," + mainmenulist + ",").indexOf("," + menuItem.elementText("id") + ",") != -1 && menuItem.elementText("menutitle").equals(subMenuTitle)) {
                menuid = menuItem.elementText("menuid");
                find = true;
            }
        }
        if (!find) {
            return -1;
        }
        List<Element> submains1 = XmlElementUtil.findElements("submain", doc.getRootElement());
        int rowcount = 0;
        for (Element sub : submains1) {
            if (sub.elementTextTrim("menuparentid").equals(menuid) && sub.elementTextTrim("menutitle").equals(menuItemTitle)) {
                return rowcount;
            }
            rowcount++;
        }
        return -1;
    }

    private static int findMenuMenuid(String mainMenuTitle, String subMenuTitle) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        String mainmenulist = "";
        if (doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]") == null) {
            return -1;
        } else {
            mainmenulist = doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]/mainmenulist").getText();
        }
        List<Element> submains = XmlElementUtil.findElements("mainmenu", doc.getRootElement());
        for (Element menuItem : submains) {
            if (("," + mainmenulist + ",").indexOf("," + menuItem.elementText("id") + ",") != -1 && menuItem.elementText("menutitle").equals(subMenuTitle)) {
                return Utils.null2Int(menuItem.elementText("menuid"));
            }
        }
        return -1;
    }

    private static int findMenuID(String mainMenuTitle, String subMenuTitle) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        String mainmenulist = "";
        if (doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]") == null) {
            return -1;
        } else {
            mainmenulist = doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]/mainmenulist").getText();
        }
        List<Element> submains = XmlElementUtil.findElements("mainmenu", doc.getRootElement());
        for (Element menuItem : submains) {
            if (("," + mainmenulist + ",").indexOf("," + menuItem.elementText("id") + ",") != -1 && menuItem.elementText("menutitle").equals(subMenuTitle)) {
                return Utils.null2Int(menuItem.elementText("id"));
            }
        }
        return -1;
    }

    private static int findMenuID(String mainMenuTitle) {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        return Utils.null2Int(doc.selectSingleNode("/dataset/toptabmenu[title=\"" + mainMenuTitle + "\"]/id").getText());
    }

    /**
	 * 查找菜单项
	 * @param mainMenuTitle 主菜单标题
	 * @return 是否存在
	 */
    public static boolean findMenu(String mainMenuTitle) {
        return findMenuID(mainMenuTitle) != -1;
    }

    /**
	 * 查找菜单项
	 * @param mainMenuTitle 主菜单标题
	 * @param subMenuTitle 子菜单标题
	 * @return 是否存在
	 */
    public static boolean findMenu(String mainMenuTitle, String subMenuTitle) {
        return findMenuID(mainMenuTitle, subMenuTitle) != -1;
    }

    /**
	 * 查找菜单项
	 * @param mainMenuTitle 主菜单标题
	 * @param subMenuTitle 子菜单标题
	 * @param menuItemTitle 菜单项标题
	 * @return 是否存在
	 */
    public static boolean findMenu(String mainMenuTitle, String subMenuTitle, String menuItemTitle) {
        return findMenuID(mainMenuTitle, subMenuTitle, menuItemTitle) != -1;
    }

    /**
	 * 查找扩展菜单
	 * @return 扩展菜单项ID
	 */
    public static int findPluginMainMenu() {
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        if (doc.selectSingleNode("/dataset/toptabmenu[system=\"2\"]") == null) {
            return -1;
        } else {
            return Utils.null2Int(doc.selectSingleNode("/dataset/toptabmenu[system=\"2\"]/id").getText());
        }
    }

    /**
	 * 生成菜单Json文件
	 * @throws IOException
	 */
    public static void createMenuJson() throws IOException {
        String jsPath = ConfigLoader.getConfig().getWebpath() + "admin/xml/navmenu.js";
        Document doc = XmlElementUtil.readXML(CONFIG_PATH);
        int i = 0;
        List<Element> toptabmenuList = XmlElementUtil.findElements("toptabmenu", doc.getRootElement());
        StringBuilder menustr = new StringBuilder();
        menustr.append("var toptabmenu = [\r\n");
        for (Element element : toptabmenuList) {
            menustr.append("{'id':'");
            menustr.append(element.elementTextTrim("id"));
            menustr.append("','title':'");
            menustr.append(element.elementTextTrim("title"));
            menustr.append("','mainmenulist':'");
            menustr.append(element.elementTextTrim("mainmenulist"));
            menustr.append("','mainmenuidlist':'");
            menustr.append(element.elementTextTrim("mainmenuidlist"));
            menustr.append("','defaulturl':'");
            menustr.append(element.elementTextTrim("defaulturl"));
            menustr.append("','system':'");
            menustr.append(element.elementTextTrim("system"));
            i++;
            if (i == toptabmenuList.size()) {
                menustr.append("'}");
            } else {
                menustr.append("'},");
            }
        }
        menustr.append("\r\n];");
        i = 0;
        menustr.append("\r\nvar mainmenu = [\r\n");
        List<Element> mainmenuList = XmlElementUtil.findElements("mainmenu", doc.getRootElement());
        for (Element element : mainmenuList) {
            menustr.append("{'id':'");
            menustr.append(element.elementTextTrim("id"));
            menustr.append("','menuid':'");
            menustr.append(element.elementTextTrim("menuid"));
            menustr.append("','menutitle':'");
            menustr.append(element.elementTextTrim("menutitle"));
            i++;
            if (i == mainmenuList.size()) {
                menustr.append("'}");
            } else {
                menustr.append("'},");
            }
        }
        menustr.append("\r\n];");
        i = 0;
        menustr.append("\r\nvar submenu = [\r\n");
        List<Element> submainList = XmlElementUtil.findElements("submain", doc.getRootElement());
        for (Element element : submainList) {
            menustr.append("{'menuparentid':'");
            menustr.append(element.elementTextTrim("menuparentid"));
            menustr.append("','menutitle':'");
            menustr.append(element.elementTextTrim("menutitle"));
            menustr.append("','link':'");
            menustr.append(element.elementTextTrim("link"));
            menustr.append("','frameid':'");
            menustr.append(element.elementTextTrim("frameid"));
            i++;
            if (i == submainList.size()) {
                menustr.append("'}");
            } else {
                menustr.append("'},");
            }
        }
        menustr.append("\r\n];");
        menustr.append("\r\nvar shortcut = \r\n");
        menustr.append("[]");
        System.out.println(menustr.toString());
        FileUtils.writeStringToFile(new File(jsPath), menustr.toString(), "UTF-8");
    }

    /**
	 * 导入插件菜单
	 * @param menuConfigFile
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    public static void importPluginMenu(String menuConfigFile) throws IOException {
        backupMenuFile();
        Document doc = XmlElementUtil.readXML(menuConfigFile);
        List<Element> menuitems = XmlElementUtil.findElements("menuitem", doc.getRootElement());
        int pluginMainId = findPluginMainMenu();
        for (Element menuitem : menuitems) {
            int newsubmenuid = newSubMenu(pluginMainId, menuitem.attributeValue("title"));
            List<Element> items = menuitem.elements();
            for (Element item : items) {
                newMenuItem(newsubmenuid, item.elementTextTrim("title"), item.elementTextTrim("link"));
            }
        }
        createMenuJson();
    }

    /**
	 * 备份菜单方法
	 * @throws IOException
	 */
    public static void backupMenuFile() throws IOException {
        FileUtils.copyFile(new File(CONFIG_PATH), new File(ConfigLoader.getConfig().getWebpath() + "admin/xml/backup/" + Utils.dateFormat("yyyy-MM-dd HH_mm_ss") + ".xml"));
    }
}

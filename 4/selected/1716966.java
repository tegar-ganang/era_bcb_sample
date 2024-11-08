package GShape.Viewer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import GShape.Core.*;
import GShape.Viewer.GViewer.*;
import GShape.Viewer.GViewerConfig.*;
import processing.app.*;
import processing.core.*;

public class GViewerMenu {

    GViewer parent;

    GCore core;

    Frame frame;

    PApplet disp;

    String Title = "G Shape";

    String[] mainMenu = { "File", "Render", "View" };

    String[][] subMenus = { { "Save Jpg", "#SEPERATOR#", "Save Obj", "Save AutoLISP", "Save PovRAY", "Save Rhino", "Save SketchUP", "Save Maya", "Save ArchiCAD", "#SEPERATOR#", "Import Shapeobject", "#SEPERATOR#", "Open folder", "#SEPERATOR#", "Quit" }, { "Wire Black", "Wire White", "Shader", "#SEPERATOR#", "Refresh" }, { "Perspective View", "Top View", "Bottom View", "Front View", "Back View", "Left View", "Right View", "#SEPERATOR#", "Stored Views", "Edit Views", "#SEPERATOR#", "Store View" } };

    String[][] subMenusId = { { "menSaveJPG", "", "menSaveObj", "menSaveAutoLISP", "menSavePovRAY", "menSaveRhino", "menSaveSketchUP", "menSaveMaya", "menSaveArchiCAD", "", "menImportShapeobject", "", "menOpenFolder", "", "menQuit" }, { "men00WireBlack", "men00WireWhite", "men00Shader", "", "menRefresh" }, { "menPerspectiveView", "menTopView", "menBottomView", "menFrontView", "menBackView", "menLeftView", "menRightView", "", "menStoredViews", "menEditStoredViews", "", "menStoreView" } };

    enum MenuType {

        Menu, CheckMenu, SubMenu
    }

    MenuType[][] subMenusTyp = { { MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu }, { MenuType.CheckMenu, MenuType.CheckMenu, MenuType.CheckMenu, MenuType.Menu, MenuType.Menu }, { MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.Menu, MenuType.SubMenu, MenuType.Menu, MenuType.Menu, MenuType.Menu } };

    char[][] subMenusShortcut = { { KeyEvent.VK_S, ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', KeyEvent.VK_O, ' ', KeyEvent.VK_Q }, { KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, ' ', KeyEvent.VK_R }, { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', KeyEvent.VK_V } };

    Map<String, Menu> menDynamic;

    myMenuListener menListener;

    void setParent(GViewer parent) {
        this.parent = parent;
        this.core = parent.core;
    }

    protected void setup_Menu(PApplet disp) {
        this.disp = disp;
        if (disp.frame != null) frame = disp.frame; else frame = (Frame) disp.getParent().getParent();
        menListener = new myMenuListener(disp);
        MenuBar menMain = new MenuBar();
        menDynamic = Collections.synchronizedMap(new HashMap<String, Menu>());
        for (int i = 0; i < mainMenu.length; i++) {
            Menu topButton = new Menu(mainMenu[i]);
            menMain.add(topButton);
            for (int ii = 0; ii < subMenus[i].length; ii++) {
                if (subMenus[i][ii].equals("#SEPERATOR#")) {
                    topButton.addSeparator();
                } else {
                    MenuItem menItem;
                    if (subMenusTyp[i][ii] == MenuType.CheckMenu) {
                        menItem = new CheckboxMenuItem(subMenus[i][ii]);
                        ((CheckboxMenuItem) menItem).addItemListener(menListener);
                    } else if (subMenusTyp[i][ii] == MenuType.SubMenu) {
                        menItem = new Menu(subMenus[i][ii]);
                        menDynamic.put(subMenusId[i][ii], (Menu) menItem);
                    } else {
                        menItem = topButton.add(new MenuItem(subMenus[i][ii]));
                        menItem.addActionListener(menListener);
                    }
                    menItem.setName(subMenusId[i][ii]);
                    if (subMenusShortcut[i][ii] != ' ') menItem.setShortcut(new MenuShortcut(subMenusShortcut[i][ii], false));
                    topButton.add(menItem);
                }
            }
        }
        createDynamicMenus();
        frame.setMenuBar(menMain);
        frame.setTitle(Title);
    }

    public void createDynamicMenus() {
        Menu menStoredViews = menDynamic.get("menStoredViews");
        menStoredViews.removeAll();
        int i = 0;
        if (menStoredViews != null) {
            for (StoredView item : parent.conf.getViews()) {
                MenuItem menItem = menStoredViews.add(new MenuItem(item.name));
                menItem.addActionListener(menListener);
                menItem.setName("menDynStoredViews" + i);
                i += 1;
            }
        }
    }

    class myMenuListener implements ActionListener, ItemListener {

        PApplet disp;

        myMenuListener(PApplet disp) {
            this.disp = disp;
        }

        public void actionPerformed(ActionEvent e) {
            String id = ((MenuItem) (e.getSource())).getName();
            if (id.equals("menSaveJPG")) {
                parent.SaveAs = "ObjJPG";
                parent.SaveAsExt = "jpg";
            }
            if (id.equals("menSaveObj")) {
                parent.SaveAs = "ObjFile";
                parent.SaveAsExt = "obj";
            }
            if (id.equals("menSaveAutoLISP")) {
                parent.SaveAs = "AutoLISP";
                parent.SaveAsExt = "lsp";
            }
            if (id.equals("menSavePovRAY")) {
                parent.SaveAs = "PovRAY";
                parent.SaveAsExt = "pov";
            }
            if (id.equals("menSaveRhino")) {
                parent.SaveAs = "Rhino";
                parent.SaveAsExt = "rvb";
            }
            if (id.equals("menSaveSketchUP")) {
                parent.SaveAs = "SketchUP";
                parent.SaveAsExt = "rb";
            }
            if (id.equals("menSaveMaya")) {
                parent.SaveAs = "Maya";
                parent.SaveAsExt = "mel";
            }
            if (id.equals("menSaveArchiCAD")) {
                parent.SaveAs = "ArchiCAD";
                parent.SaveAsExt = "gdl";
            }
            if (id.equals("menImportShapeobject")) {
                class MyFilter implements FilenameFilter {

                    public boolean accept(File dir, String name) {
                        if (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".class")) return true;
                        return false;
                    }
                }
                FileDialog fileDialog = new FileDialog(frame, "Import Shape Object", FileDialog.LOAD);
                fileDialog.setFilenameFilter(new MyFilter());
                fileDialog.setFile("*.jar; *.class");
                fileDialog.setVisible(true);
                String directory = fileDialog.getDirectory();
                String filename = fileDialog.getFile();
                if (directory != null && filename != null) CopyFile(directory, filename);
            }
            if (id.equals("menOpenFolder")) {
                String folder = disp.sketchPath("data");
                File fl2 = new File(folder);
                if (!fl2.exists()) fl2.mkdir();
                try {
                    Runtime.getRuntime().exec("explorer \"" + folder + "\"");
                } catch (Exception e1) {
                }
                try {
                    processing.core.PApplet.open(folder);
                } catch (Exception e1) {
                }
                try {
                    String lunch = Preferences.get("launcher");
                    String[] params = new String[] { lunch, folder };
                    Runtime.getRuntime().exec(params);
                } catch (Exception e2) {
                }
            }
            if (id.equals("menQuit")) {
                System.exit(0);
            }
            if (id.equals("menRefresh")) disp.setup();
            if (id.equals("menPerspectiveView")) parent.setView(View.PERSPECTIVE);
            if (id.equals("menTopView")) parent.setView(View.TOP);
            if (id.equals("menBottomView")) parent.setView(View.BOTTOM);
            if (id.equals("menFrontView")) parent.setView(View.FRONT);
            if (id.equals("menBackView")) parent.setView(View.BACK);
            if (id.equals("menLeftView")) parent.setView(View.LEFT);
            if (id.equals("menRightView")) parent.setView(View.RIGHT);
            if (id.equals("menStoreView")) {
                String name = "";
                while (name.equals("")) {
                    name = JOptionPane.showInputDialog(null, "Enter View Name", "View");
                    if (name == null) return;
                }
                StoredView view = parent.conf.new StoredView(name, parent.rotX, parent.rotY);
                parent.conf.addView(view);
                parent.conf.SaveXML();
                createDynamicMenus();
            }
            if (id.length() > 17 && id.substring(0, 17).equals("menDynStoredViews")) {
                Integer index = new Integer(id.substring(17));
                StoredView xmlview = parent.conf.getViews().get(index);
                if (xmlview != null) parent.setView(xmlview.rotX, xmlview.rotY);
            }
        }

        public void itemStateChanged(ItemEvent e) {
            MenuItem men = (MenuItem) e.getSource();
            String id = ((MenuItem) (e.getSource())).getName();
            Menu x = (Menu) men.getParent();
            for (int i = 0; i < x.getItemCount(); i++) {
                String name = x.getItem(i).getName();
                if (name.length() > 5 && name.substring(0, 5).equalsIgnoreCase("men00")) {
                    if (!x.getItem(i).equals(men)) ((CheckboxMenuItem) x.getItem(i)).setState(false); else ((CheckboxMenuItem) x.getItem(i)).setState(true);
                }
            }
            if (id.equals("men00WireBlack")) parent.mode = parent.mode.WireBlack;
            if (id.equals("men00WireWhite")) parent.mode = parent.mode.WireWhite;
            if (id.equals("men00Shader")) parent.mode = parent.mode.Shader;
        }
    }

    public boolean CopyFile(String directory, String filename) {
        if (directory == null || filename == null) return false;
        if (directory == "" || filename == "") return false;
        File folder = new File(disp.sketchPath(core.Folder_sub));
        if (!folder.exists()) folder.mkdir();
        File in = new File(directory, filename);
        File out = new File(disp.sketchPath(core.Folder_sub), filename);
        FileInputStream is;
        try {
            is = new FileInputStream(in);
            FileOutputStream os = new FileOutputStream(out);
            os.getChannel().transferFrom(is.getChannel(), 0, in.length());
            is.close();
            os.close();
        } catch (Exception e1) {
            return false;
        }
        return true;
    }
}

package de.sharpner.jcmd.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import de.sharpner.jcmd.controller.ViewController;
import de.sharpner.jcmd.util.AdvancedConfigLoader;

public class CommanderView {

    private ViewController controller = null;

    private AdvancedConfigLoader language = null;

    private JFrame main_window = new JFrame();

    private JTabbedPane left_window = new JTabbedPane();

    private JTabbedPane right_window = new JTabbedPane();

    private JToolBar toolbar_top = new JToolBar("toolbar_top");

    private JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left_window, right_window);

    private JButton low_button_01 = null;

    private JButton low_button_02 = null;

    private JButton low_button_03 = null;

    private JButton low_button_04 = null;

    private JButton low_button_05 = null;

    private JButton low_button_06 = null;

    private JButton low_button_07 = null;

    private JButton low_button_08 = null;

    private JMenuBar menuBar = new JMenuBar();

    private JMenu menu_file = null;

    private JMenu menu_edit = null;

    private JMenuItem file_open = null;

    private JMenuItem file_save = null;

    private JMenuItem file_saveas = null;

    private JMenuItem file_exit = null;

    private JMenuItem edit_cut = null;

    private JMenuItem edit_copy = null;

    private JMenuItem edit_paste = null;

    private JMenu menu_settings = null;

    private JMenuItem settings_common = null;

    private JMenuItem settings_system = null;

    private JPopupMenu tab_menu = new JPopupMenu();

    private JMenuItem tab_menu_close = null;

    private JMenuItem tab_menu_rename = null;

    private JMenuItem tab_menu_newtab = null;

    private JMenuItem tab_menu_refresh = null;

    private JMenuItem tab_menu_clone = null;

    private JButton buttonbar_add = null;

    private JButton buttonbar_rem = null;

    private WindowTray windowTray = null;

    private LinkButtonBar linkButtonBar = null;

    /**
	 * 
	 * @param controller
	 */
    public void setController(ViewController controller) {
        this.controller = controller;
    }

    /**
	 * 
	 * @param identifier
	 * @return
	 */
    public JComponent getComponentByIdentifier(String identifier) {
        if (identifier.equals("left_window")) {
            return left_window;
        } else if (identifier.equals("right_window")) {
            return right_window;
        } else if (identifier.equals("toolbar_top")) {
            return toolbar_top;
        } else if (identifier.equals("splitPane")) {
            return splitPane;
        } else if (identifier.equals("low_button_01")) {
            return low_button_01;
        } else if (identifier.equals("low_button_02")) {
            return low_button_02;
        } else if (identifier.equals("low_button_03")) {
            return low_button_03;
        } else if (identifier.equals("low_button_04")) {
            return low_button_04;
        }
        if (identifier.equals("low_button_05")) {
            return low_button_05;
        } else if (identifier.equals("low_button_06")) {
            return low_button_06;
        } else if (identifier.equals("low_button_07")) {
            return low_button_07;
        } else if (identifier.equals("low_button_08")) {
            return low_button_08;
        } else if (identifier.equals("menuBar")) {
            return menuBar;
        } else if (identifier.equals("menu_file")) {
            return menu_file;
        } else if (identifier.equals("menu_edit")) {
            return menu_edit;
        } else if (identifier.equals("file_open")) {
            return file_open;
        } else if (identifier.equals("file_save")) {
            return file_save;
        } else if (identifier.equals("file_saveas")) {
            return file_saveas;
        } else if (identifier.equals("file_exit")) {
            return file_exit;
        } else if (identifier.equals("edit_cut")) {
            return edit_cut;
        } else if (identifier.equals("edit_copy")) {
            return edit_copy;
        } else if (identifier.equals("edit_paste")) {
            return edit_paste;
        } else if (identifier.equals("menu_settings")) {
            return menu_settings;
        } else if (identifier.equals("settings_common")) {
            return settings_common;
        } else if (identifier.equals("settings_system")) {
            return settings_system;
        } else if (identifier.equals("windowTray")) {
            return windowTray;
        } else if (identifier.equals("buttonbar_add")) {
            return buttonbar_add;
        } else if (identifier.equals("buttonbar_rem")) {
            return buttonbar_rem;
        } else if (identifier.equals("tab_menu_close")) {
            return tab_menu_close;
        } else if (identifier.equals("tab_menu_newtab")) {
            return tab_menu_newtab;
        } else if (identifier.equals("tab_menu_rename")) {
            return tab_menu_rename;
        } else if (identifier.equals("tab_menu_refresh")) {
            return tab_menu_refresh;
        } else if (identifier.equals("tab_menu_clone")) {
            return tab_menu_clone;
        }
        return null;
    }

    /**
	 * 
	 * @return
	 */
    public Frame getContainer() {
        return main_window;
    }

    /**
	 * 
	 * @param language
	 */
    public void setLanguage(AdvancedConfigLoader language) {
        this.language = language;
    }

    /**
	 * 
	 */
    public void createLayout() {
        if (language == null) {
            setDefaultLanguage();
        }
        main_window.setTitle("Java Commander 2 - Build 100");
        main_window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JPanel bottom = new JPanel();
        JPanel center = new JPanel();
        JPanel top = new JPanel();
        JPanel button_pane = new JPanel();
        JPanel search_pane = new JPanel();
        JPanel application_pane = new JPanel();
        ImageIcon jcicon16 = new ImageIcon(getClass().getResource("icons/jc16x16.png"));
        controller.updateView();
        main_window.add(top, BorderLayout.NORTH);
        main_window.add(center, BorderLayout.CENTER);
        main_window.add(bottom, BorderLayout.SOUTH);
        center.setLayout(new BorderLayout());
        BoxLayout top_box = new BoxLayout(top, BoxLayout.Y_AXIS);
        top.setLayout(top_box);
        BoxLayout bottom_box = new BoxLayout(bottom, BoxLayout.Y_AXIS);
        bottom.setLayout(bottom_box);
        toolbar_top.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbar_top.setFloatable(false);
        top.add(toolbar_top);
        center.add(splitPane);
        splitPane.setDividerLocation(main_window.getWidth() / 2);
        splitPane.setOneTouchExpandable(true);
        low_button_01 = new JButton(language.getValue("main_01_butt"));
        low_button_02 = new JButton(language.getValue("main_02_butt"));
        low_button_03 = new JButton(language.getValue("main_03_butt"));
        low_button_04 = new JButton(language.getValue("main_04_butt"));
        low_button_05 = new JButton(language.getValue("main_05_butt"));
        low_button_06 = new JButton(language.getValue("main_06_butt"));
        low_button_07 = new JButton(language.getValue("main_07_butt"));
        low_button_08 = new JButton(language.getValue("main_08_butt"));
        menu_file = new JMenu(language.getValue("file"));
        menu_edit = new JMenu(language.getValue("edit"));
        menu_settings = new JMenu(language.getValue("settings"));
        settings_common = new JMenuItem(language.getValue("common"));
        settings_system = new JMenuItem(language.getValue("system"));
        file_open = new JMenuItem(language.getValue("open"));
        file_save = new JMenuItem(language.getValue("save"));
        file_saveas = new JMenuItem(language.getValue("saveas"));
        file_exit = new JMenuItem(language.getValue("exit"));
        edit_cut = new JMenuItem(language.getValue("cut"));
        edit_copy = new JMenuItem(language.getValue("copy"));
        edit_paste = new JMenuItem(language.getValue("paste"));
        tab_menu_close = new JMenuItem(language.getValue("tab_close"));
        tab_menu_rename = new JMenuItem(language.getValue("tab_rename"));
        tab_menu_newtab = new JMenuItem(language.getValue("tab_newtab"));
        tab_menu_refresh = new JMenuItem(language.getValue("tab_refresh"));
        tab_menu_clone = new JMenuItem(language.getValue("tab_menu_clone"));
        buttonbar_add = new JButton(new ImageIcon(getClass().getResource("icons/add.png")));
        buttonbar_rem = new JButton(new ImageIcon(getClass().getResource("icons/remove.png")));
        menuBar.add(menu_file);
        menuBar.add(menu_edit);
        menuBar.add(menu_settings);
        menu_file.add(file_open);
        menu_file.add(file_save);
        menu_file.add(file_saveas);
        menu_file.addSeparator();
        menu_file.add(file_exit);
        menu_edit.add(edit_cut);
        menu_edit.add(edit_copy);
        menu_edit.add(edit_paste);
        menu_settings.add(settings_common);
        menu_settings.add(settings_system);
        main_window.setJMenuBar(menuBar);
        tab_menu.add(tab_menu_newtab);
        tab_menu.add(tab_menu_clone);
        tab_menu.add(tab_menu_rename);
        tab_menu.add(tab_menu_refresh);
        tab_menu.add(tab_menu_close);
        button_pane.add(low_button_01);
        button_pane.add(low_button_02);
        button_pane.add(low_button_03);
        button_pane.add(low_button_04);
        button_pane.add(low_button_05);
        button_pane.add(low_button_06);
        button_pane.add(low_button_07);
        button_pane.add(low_button_08);
        bottom.add(application_pane);
        bottom.add(button_pane);
        bottom.add(search_pane);
        toolbar_top.add(buttonbar_add);
        toolbar_top.add(buttonbar_rem);
        controller.sendEvent(null, "left_window", 0);
        controller.sendEvent(null, "right_window", 0);
        if (jcicon16 != null) {
            main_window.setIconImage(jcicon16.getImage());
        }
        windowTray = new WindowTray(jcicon16.getImage(), language);
        registerListener();
    }

    /**
	 * 
	 */
    public void showCommander() {
        main_window.setVisible(true);
    }

    /**
	 * 
	 */
    private void setDefaultLanguage() {
        language = new AdvancedConfigLoader("english.cfg");
        language.setValue("file", "File");
        language.setValue("edit", "Edit");
        language.setValue("open", "Open");
        language.setValue("save", "Save");
        language.setValue("saveas", "Save as...");
        language.setValue("exit", "Exit");
        language.setValue("cut", "Cut");
        language.setValue("copy", "Copy");
        language.setValue("paste", "Paste");
        language.setValue("settings", "Settings");
        language.setValue("common", "Main");
        language.setValue("system", "System");
        language.setValue("d01add", "add button");
        language.setValue("d01remove", "remove button");
        language.setValue("main_01_butt", "F1 Help");
        language.setValue("main_02_butt", "F2 No Idea");
        language.setValue("main_03_butt", "F3 View");
        language.setValue("main_04_butt", "F4 Edit");
        language.setValue("main_05_butt", "F5 Copy");
        language.setValue("main_06_butt", "F6 Move");
        language.setValue("main_07_butt", "F7 New Directory");
        language.setValue("main_08_butt", "F8 Delete");
        language.setValue("tab_close", "Close Tab");
        language.setValue("tab_rename", "Rename Tab");
        language.setValue("tab_newtab", "New Tab");
        language.setValue("tab_refresh", "Refresh Tab");
        language.setValue("tab_menu_clone", "Clone Tab");
        language.setValue("default_tab_name", "New Tab");
        language.setValue("mr01open", "Open");
        language.setValue("mr01.5open", "Open with...");
        language.setValue("mr01.6custom", "Enter Custom Command");
        language.setValue("mr02ren", "Rename");
        language.setValue("mr03del", "Delete");
        language.setValue("mr04mov", "Move");
        language.setValue("mr05cut", "Cut");
        language.setValue("mr06copy", "Copy");
        language.setValue("mr07paste", "Paste");
        language.setValue("mr08attr", "Properties");
        language.setValue("label_text", "Path: ");
        language.setValue("tray_menu_01", "Show/Hide");
        language.setValue("tray_menu_02", "Close");
        language.setValue("fcopyexist", "File to copy does not exist.");
        language.setValue("fcopyread", "Cannot read file. Not allowed.");
        language.setValue("fcopywrite", "Cannot write destination file.");
        language.setValue("fcopyover", "File already exists. Overwrite existing file?");
        language.setValue("fcopyexistd", "Destination Directory does not exist.");
        language.setValue("fcopyreadd", "Cannot read destination directory. Not allowed.");
    }

    /**
	 * 
	 */
    private void registerListener() {
        main_window.addComponentListener(new ComponentListener() {

            public void componentResized(ComponentEvent e) {
                controller.sendEvent(e, "main_window", 0);
            }

            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentShown(ComponentEvent e) {
            }
        });
        main_window.addWindowListener(new WindowListener() {

            public void windowClosing(WindowEvent e) {
                controller.sendEvent(e, "main_window", 0);
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }
        });
        low_button_01.addActionListener(new IdentifyListener("low_button_01", 0));
        low_button_02.addActionListener(new IdentifyListener("low_button_02", 0));
        low_button_03.addActionListener(new IdentifyListener("low_button_03", 0));
        low_button_04.addActionListener(new IdentifyListener("low_button_04", 0));
        low_button_05.addActionListener(new IdentifyListener("low_button_05", 0));
        low_button_06.addActionListener(new IdentifyListener("low_button_06", 0));
        low_button_07.addActionListener(new IdentifyListener("low_button_07", 0));
        low_button_08.addActionListener(new IdentifyListener("low_button_08", 0));
        buttonbar_add.addActionListener(new IdentifyListener("buttonbar_add", 0));
        buttonbar_rem.addActionListener(new IdentifyListener("buttonbar_rem", 0));
        tab_menu_rename.addActionListener(new IdentifyListener("tab_menu_rename", 0));
        tab_menu_close.addActionListener(new IdentifyListener("tab_menu_close", 0));
        tab_menu_newtab.addActionListener(new IdentifyListener("tab_menu_newtab", 0));
        tab_menu_refresh.addActionListener(new IdentifyListener("tab_menu_refresh", 0));
        tab_menu_clone.addActionListener(new IdentifyListener("tab_menu_clone", 0));
        left_window.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                controller.sendEvent(e, "left_window", 0);
            }
        });
        right_window.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                controller.sendEvent(e, "right_window", 0);
            }
        });
        windowTray.addTrayActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == windowTray.getItemByIdentifier("visible")) {
                    controller.sendEvent(e, "tray_visible", 0);
                } else if (e.getSource() == windowTray.getItemByIdentifier("close")) {
                    controller.sendEvent(e, "tray_close", 0);
                }
                if (e.getSource() == windowTray.getTrayIcon()) {
                    controller.sendEvent(e, "tray_visible", 0);
                }
            }
        });
        left_window.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                controller.sendEvent(e, "left_window", 0);
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
        right_window.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                controller.sendEvent(e, "right_window", 0);
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }
        });
    }

    public void printNotifyMessage(String cap, String str, int type) {
        windowTray.notifyTray(cap, str, type);
    }

    public void setLookAndFeel(String layout) throws Exception {
        UIManager.setLookAndFeel(layout);
    }

    private class IdentifyListener implements ActionListener {

        private String identifier = null;

        private int type = 0;

        public IdentifyListener(String identifier, int type) {
            this.identifier = identifier;
            this.type = type;
        }

        public void actionPerformed(ActionEvent e) {
            controller.sendEvent(e, identifier, type);
        }
    }

    public void showTabPopup(JComponent com, int x, int y) {
        tab_menu.show(com, x, y);
    }
}

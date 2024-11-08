package de.rizzek.rizztools;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 * This class is used to start RizzTools
 *
 * @author rizzek
 */
public class Main {

    private static RizzTool[] loadedTools;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RizzToolConstants.DEFAULT_LOGGER.log("Starting RizzTools...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            RizzToolConstants.DEFAULT_LOGGER.debug("Exception while setting LookAndFeel, can be ignored.");
        }
        if (!RizzToolConstants.RIZZTOOL_FOLDER.exists()) {
            RizzToolConstants.RIZZTOOL_FOLDER.mkdirs();
            RizzToolConstants.RIZZTOOL_FOLDER.mkdir();
        }
        if (!RizzToolConstants.TOOL_POOL_FOLDER.exists()) {
            RizzToolConstants.TOOL_POOL_FOLDER.mkdirs();
            RizzToolConstants.TOOL_POOL_FOLDER.mkdir();
        }
        File[] tools = RizzToolConstants.TOOL_POOL_FOLDER.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.toLowerCase().endsWith(".jar")) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        if (tools.length <= 0) {
            int choice = JOptionPane.showConfirmDialog(null, "This seems to be the first time you started RizzTools.\r\nDo you want to import any RizzTools?", "First start", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        if (f.getAbsolutePath().toLowerCase().endsWith(".jar") || f.isDirectory()) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "JAR-Files";
                    }
                });
                chooser.setDialogTitle("Choose RizzTool(s)");
                chooser.setMultiSelectionEnabled(true);
                chooser.showOpenDialog(null);
                File[] selected = chooser.getSelectedFiles();
                if (selected != null && selected.length > 0) {
                    for (int i = 0; i < selected.length; i++) {
                        File copiedFile = copyJarToPool(selected[i]);
                        if (copiedFile != null && copiedFile.exists()) {
                            tools = Arrays.copyOf(tools, tools.length + 1);
                            tools[tools.length - 1] = copiedFile;
                        }
                    }
                }
            }
        }
        RizzTool[] toolArray = ToolLoader.loadTools(tools);
        loadedTools = toolArray;
        initializeSystemTray(toolArray);
    }

    private static PopupMenu createPopupMenu(RizzTool[] tools) {
        PopupMenu popup = new PopupMenu();
        MenuItem mainItem = new MenuItem("RizzTools Main Window");
        mainItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new MainWindow(loadedTools).setVisible(true);
            }
        });
        popup.add(mainItem);
        popup.addSeparator();
        for (int i = 0; i < tools.length; i++) {
            MenuItem item = new MenuItem(tools[i].getDisplayName());
            item.addActionListener(new MenuListener(tools[i]));
            popup.add(item);
        }
        popup.addSeparator();
        MenuItem endItem = new MenuItem("Quit");
        endItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        popup.add(endItem);
        return popup;
    }

    public static void initializeSystemTray(RizzTool[] tools) {
        PopupMenu popup = createPopupMenu(tools);
        TrayIcon trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(RizzTool.class.getResource("rizztools.png")), "RizzTools", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                if (arg0.getButton() == MouseEvent.BUTTON1) {
                }
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }
        });
        try {
            SystemTray.getSystemTray().add(trayIcon);
            trayIcon.displayMessage("Info", "RizzTools menu has been started.", TrayIcon.MessageType.INFO);
        } catch (AWTException ex) {
            RizzToolConstants.DEFAULT_LOGGER.error("Could not add icon to tray: " + ex);
        }
    }

    private static File copyJarToPool(File file) {
        File outFile = new File(RizzToolConstants.TOOL_POOL_FOLDER.getAbsolutePath() + File.separator + file.getName());
        if (file != null && file.exists() && file.canRead()) {
            try {
                FileChannel inChan = new FileInputStream(file).getChannel();
                FileChannel outChan = new FileOutputStream(outFile).getChannel();
                inChan.transferTo(0, inChan.size(), outChan);
                return outFile;
            } catch (Exception ex) {
                RizzToolConstants.DEFAULT_LOGGER.error("Exception while copying jar file to tool pool [inFile=" + file.getAbsolutePath() + "] [outFile=" + outFile.getAbsolutePath() + ": " + ex);
            }
        } else {
            RizzToolConstants.DEFAULT_LOGGER.error("Could not copy jar file. File does not exist or can't read file. [inFile=" + file.getAbsolutePath() + "]");
        }
        return null;
    }
}

class MenuListener implements ActionListener {

    private final RizzTool tool;

    public MenuListener(RizzTool tool) {
        this.tool = tool;
    }

    public void actionPerformed(ActionEvent e) {
        tool.start();
    }
}

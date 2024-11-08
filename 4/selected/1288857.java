package teder;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class TederFrame extends JFrame implements ActionListener, WindowListener {

    public static final int DEFAULT_WIDTH = 800;

    public static final int DEFAULT_HEIGHT = 600;

    public TederData ttd;

    public TederPanel ttp;

    public JMenuBar mbar;

    public JMenu fileMenu;

    public JMenuItem openMenuItem;

    public JMenuItem saveMenuItem;

    public JMenuItem saveAsMenuItem;

    public JMenuItem exitMenuItem;

    public JMenu mapMenu;

    public JMenuItem resizeMenuItem;

    public JMenu tileMenu;

    public JCheckBoxMenuItem tileItems[];

    public JScrollPane spMain;

    public String lastPath;

    public TederFrame() {
        lastPath = System.getProperty("user.dir");
        ttd = new TederData(30, 30, 64, 64, "images/tiles.gif", this);
        ttp = new TederPanel(ttd);
        createMenuBar();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        spMain = new JScrollPane(ttp);
        spMain.setPreferredSize(new Dimension(200, 200));
        add(spMain);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
        System.exit(0);
    }

    public void windowClosing(WindowEvent e) {
        if (okayToClose()) {
            System.exit(0);
        }
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public boolean okayToClose() {
        if (ttd.getHasChanged()) {
            Object[] options = { "Yes", "No", "Cancel" };
            int n = JOptionPane.showOptionDialog(this, "The map has changed would you like to save the file before proceeding?", "File Has Changed", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
            if (n == 0) {
                return doSaveFile();
            } else if (n == 2) {
                return false;
            }
        }
        return true;
    }

    public void createMenuBar() {
        JMenuItem itmTmp;
        mbar = new JMenuBar();
        fileMenu = new JMenu("File");
        itmTmp = new JMenuItem("New");
        itmTmp.addActionListener(this);
        fileMenu.add(itmTmp);
        openMenuItem = new JMenuItem("Open...");
        openMenuItem.addActionListener(this);
        fileMenu.add(openMenuItem);
        saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(this);
        fileMenu.add(saveMenuItem);
        saveAsMenuItem = new JMenuItem("Save As...");
        saveAsMenuItem.addActionListener(this);
        fileMenu.add(saveAsMenuItem);
        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(this);
        fileMenu.add(exitMenuItem);
        mbar.add(fileMenu);
        mapMenu = new JMenu("Map");
        resizeMenuItem = new JMenuItem("Resize");
        resizeMenuItem.addActionListener(this);
        mapMenu.add(resizeMenuItem);
        mbar.add(mapMenu);
        tileMenu = new JMenu("Tile");
        tileItems = new JCheckBoxMenuItem[TederData.NUM_TILES];
        int i;
        for (i = 0; i < TederData.NUM_TILES; i++) {
            tileItems[i] = new JCheckBoxMenuItem("Tile " + i, new ImageIcon("images/tile_icons/tile" + i + ".gif"));
            tileItems[i].addActionListener(this);
            tileMenu.add(tileItems[i]);
        }
        mbar.add(tileMenu);
        adjustMenus();
        this.setJMenuBar(mbar);
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals("New")) {
            System.out.println("New command selected");
        } else if (evt.getSource() == exitMenuItem) {
            System.out.println("Exit selected");
            if (okayToClose()) System.exit(0);
        } else if (evt.getSource() == saveMenuItem) {
            this.doSaveFile();
            ttd.doSave();
        } else if (evt.getSource() == saveAsMenuItem) {
            this.doSaveAsFile();
        } else if (evt.getSource() == openMenuItem) {
            doLoadFile();
            ttp.resetPreferredSize();
            this.repaint();
        } else if (evt.getSource() == resizeMenuItem) {
            doMapSizeDialog();
            this.repaint();
        } else {
            int i;
            for (i = 0; i < TederData.NUM_TILES; i++) {
                if (evt.getSource() == tileItems[i]) {
                    ttp.curTile = i;
                }
            }
            adjustMenus();
        }
    }

    public void adjustMenus() {
        int i;
        for (i = 0; i < TederData.NUM_TILES; i++) {
            if (i == ttp.curTile) tileItems[i].setState(true); else tileItems[i].setState(false);
        }
    }

    public void doLoadFile() {
        JFileChooser chooser;
        if (!okayToClose()) return;
        if (lastPath == null) {
            chooser = new JFileChooser();
        } else {
            File fiTmp = new File(lastPath);
            chooser = new JFileChooser(fiTmp);
        }
        MapFileFilter filter = new MapFileFilter();
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fiSelected = chooser.getSelectedFile();
            lastPath = fiSelected.getPath();
            ttd.setFileName(fiSelected.getAbsolutePath());
            ttd.doLoad();
            ttd.setHasChanged(false);
        }
    }

    public boolean doSaveAsFile() {
        JFileChooser chooser;
        if (lastPath == null) {
            chooser = new JFileChooser();
        } else {
            File fiTmp = new File(lastPath);
            chooser = new JFileChooser(fiTmp);
        }
        MapFileFilter filter = new MapFileFilter();
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fiSelected = chooser.getSelectedFile();
            if (fiSelected.exists()) {
                Object[] options = { "Yes", "No" };
                int n = JOptionPane.showOptionDialog(this, "The file already exists, would you like to overwrite?", "File Exists", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (n == 1) {
                    return false;
                }
            }
            lastPath = fiSelected.getPath();
            ttd.setFileName(fiSelected.getAbsolutePath());
            ttd.doSave();
            ttd.setHasChanged(false);
            return true;
        }
        return false;
    }

    public boolean doSaveFile() {
        if (ttd.isValidFile()) {
            ttd.doSave();
        } else {
            return doSaveAsFile();
        }
        return true;
    }

    public void setMapSize(int w, int h) {
        ttd.resize(w, h);
        ttp.resetPreferredSize();
    }

    public void doMapSizeDialog() {
        MapSizeDialog msp = new MapSizeDialog(this, ttd.getWidth(), ttd.getHeight());
        msp.setVisible(true);
    }

    class MapFileFilter extends javax.swing.filechooser.FileFilter {

        public MapFileFilter() {
            super();
        }

        public boolean accept(File f) {
            if ((f.getName().endsWith(".dat")) || (f.isDirectory())) {
                return true;
            }
            return false;
        }

        public String getDescription() {
            return "Map .dat files";
        }
    }

    class MapSizeDialog extends JDialog {

        TederFrame parent;

        JButton okButton;

        JButton cancelButton;

        JTextField jtfWidth;

        JTextField jtfHeight;

        public MapSizeDialog(TederFrame parentInit, int wid, int hei) {
            super(parentInit, true);
            parent = parentInit;
            JPanel inputPane = new JPanel();
            inputPane.setLayout(new GridLayout(0, 2));
            inputPane.add(new JLabel("Width:"));
            jtfWidth = new JTextField("" + wid);
            inputPane.add(jtfWidth);
            inputPane.add(new JLabel("Height"));
            jtfHeight = new JTextField("" + hei);
            inputPane.add(jtfHeight);
            inputPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
            buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            buttonPane.add(Box.createHorizontalGlue());
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == cancelButton) {
                        setVisible(false);
                        dispose();
                    }
                }
            });
            buttonPane.add(cancelButton);
            buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
            okButton = new JButton("Ok");
            okButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == okButton) {
                        try {
                            int wid = Integer.parseInt(jtfWidth.getText());
                            int hei = Integer.parseInt(jtfHeight.getText());
                            parent.setMapSize(wid, hei);
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                        setVisible(false);
                        dispose();
                    }
                }
            });
            buttonPane.add(okButton);
            Container contentPane = getContentPane();
            contentPane.add(inputPane, BorderLayout.CENTER);
            contentPane.add(buttonPane, BorderLayout.PAGE_END);
            this.pack();
        }
    }
}

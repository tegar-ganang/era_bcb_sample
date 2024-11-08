package civquest.swing.mapedit;

import civquest.*;
import civquest.swing.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.io.*;
import javax.swing.filechooser.*;

public class Sidebar extends JPanel {

    MapEdit app;

    JToggleButton fill, erase;

    JPopupMenu listPopup;

    public Sidebar(MapEdit app) {
        this.app = app;
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        JLabel heading = new JLabel("MapEditor");
        heading.setFont(new Font("Dialog", Font.BOLD, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.weightx = 100;
        c.weighty = 20;
        c.gridwidth = 2;
        add(heading, c);
        JMenuItem m = new JMenuItem("Reload tiles");
        m.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
            }
        });
        listPopup = new JPopupMenu();
        listPopup.add(m);
        final JList tileList = new JList(new DefaultListModel());
        Enumeration keys = app.getTileLoader().getTiles().keys();
        for (int i = 0; keys.hasMoreElements(); i++) {
            String s = (String) keys.nextElement();
            s = new String(Character.toTitleCase(s.charAt(0)) + s.substring(1));
            ((DefaultListModel) tileList.getModel()).add(i, s);
        }
        tileList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                String tileName = ((String) ((DefaultListModel) tileList.getModel()).get(tileList.getSelectedIndex())).toLowerCase();
                Sidebar.this.app.setCurrentTile(tileName);
                Sidebar.this.erase.setSelected(false);
            }
        });
        tileList.addMouseListener(new MouseAdapter() {

            public void MousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    listPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 100;
        c.weighty = 100;
        c.gridwidth = 2;
        c.ipady = 30;
        c.ipadx = 30;
        add(new JScrollPane(tileList), c);
        final JCheckBox vgrid = new JCheckBox("Visible grid", true);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 100;
        c.weighty = 10;
        c.ipady = 30;
        c.ipadx = 30;
        c.gridwidth = 2;
        vgrid.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Sidebar.this.app.getMap().setVisibleGrid(vgrid.isSelected());
            }
        });
        add(vgrid, c);
        fill = new JToggleButton("Fill", new ImageIcon(app.getResourceURL("images/icons/fill.png")));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 50;
        c.weighty = 0;
        c.gridwidth = 1;
        fill.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Sidebar.this.app.setFillMode(fill.isSelected());
            }
        });
        fill.setMnemonic('F');
        add(fill, c);
        erase = new JToggleButton("Erase", new ImageIcon(app.getResourceURL("images/icons/erase.png")));
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 50;
        c.weighty = 0;
        c.gridwidth = 1;
        erase.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Sidebar.this.app.setEraseMode(erase.isSelected());
            }
        });
        erase.setMnemonic('E');
        add(erase, c);
        final JLabel widthLabel = new JLabel("Width:");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weightx = 100;
        c.ipady = 10;
        add(widthLabel, c);
        final JSlider widthSlider = new JSlider(1, 70, 10);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.weightx = 100;
        widthSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                Sidebar.this.app.getMap().changeWidth((int) widthSlider.getValue());
            }
        });
        add(widthSlider, c);
        final JLabel heightLabel = new JLabel("Height:");
        heightLabel.setSize(600, heightLabel.getWidth());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        c.weightx = 100;
        c.ipady = 10;
        add(heightLabel, c);
        final JSlider heightSlider = new JSlider(1, 70, 10);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.weightx = 100;
        heightSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                Sidebar.this.app.getMap().changeHeight((int) heightSlider.getValue());
            }
        });
        add(heightSlider, c);
        final JButton save = new JButton("Save");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.gridx = 0;
        c.gridy = 8;
        c.weightx = 50;
        c.ipady = 10;
        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveMap();
            }
        });
        save.setMnemonic('S');
        add(save, c);
        final JButton load = new JButton("Load");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.gridx = 1;
        c.gridy = 8;
        c.weightx = 50;
        c.ipady = 10;
        load.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadMap();
            }
        });
        load.setMnemonic('L');
        add(load, c);
    }

    private void saveMap() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select filename for the map");
        chooser.setFileFilter(new DialogFilter());
        int r = JOptionPane.NO_OPTION;
        while (r == JOptionPane.NO_OPTION) {
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                String filnavn = f.getAbsolutePath();
                if (!filnavn.endsWith(".map")) {
                    f = new File(filnavn += ".map");
                }
                if (f.exists()) {
                    r = JOptionPane.showConfirmDialog(this, "The file you chose already exists. Overwrite?", "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (r == JOptionPane.YES_OPTION) {
                        serialize(f);
                    }
                } else {
                    serialize(f);
                    r = JOptionPane.YES_OPTION;
                }
            } else r = JOptionPane.YES_OPTION;
        }
    }

    private void serialize(File f) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            ObjectOutputStream s = new ObjectOutputStream(out);
            MapSaveData save = new MapSaveData();
            Image[][] tiles = app.getMap().getTiles();
            String[][] names = new String[tiles.length][tiles[0].length];
            for (int y = 0; y < tiles.length; y++) for (int x = 0; x < tiles[0].length; x++) names[y][x] = app.getTileLoader().getTileName(tiles[y][x]);
            save.map = names;
            s.writeObject(save);
            s.flush();
            out.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "There was an error saving the map.\n\nMessage:\n" + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadMap() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select filename for the map");
        chooser.setFileFilter(new DialogFilter());
        int r = JOptionPane.NO_OPTION;
        int returnVal = chooser.showOpenDialog(this);
        File f = chooser.getSelectedFile();
        if (f != null) {
            if (f.exists()) {
                deserialize(f);
            } else {
                JOptionPane.showMessageDialog(this, "There is no file " + f.getName() + "!", "Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deserialize(File f) {
        try {
            app.getMap().initialize(new MapLoader(f));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "There was an error loading the map.\n\nMessage:\n" + e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void depressFill() {
        fill.setSelected(false);
    }
}

/**
 * A file dialog filter for JFileDialog that allows viewing of
 * directories and files ending in .map.
 */
class DialogFilter extends javax.swing.filechooser.FileFilter {

    public boolean accept(File f) {
        String name = f.getName();
        String ext;
        if (name.indexOf('.') > 0) ext = name.substring(name.lastIndexOf('.') + 1); else ext = "";
        if (f.isDirectory() || ext.equals("map")) return true;
        return false;
    }

    public String getDescription() {
        return "Map files for CivQuest map editor";
    }
}

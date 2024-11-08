package tools.leveleditor;

import tools.leveleditor.EditorFrame;
import java.awt.event.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import main.Main;
import main.Map;

@SuppressWarnings("serial")
class SaveMap extends JFrame {

    static Map map;

    static File mapfile;

    JTextArea foldername;

    JTextArea mapival, mapjval;

    final int EDGE = 15;

    public SaveMap() {
        this.getContentPane().setLayout(null);
        JLabel label1 = new JLabel("Map Folder: ");
        foldername = new JTextArea(main.Main.MAPDIR);
        JLabel label2 = new JLabel("i,j: ");
        mapival = new JTextArea();
        mapjval = new JTextArea();
        JButton savebutt = new JButton("Save");
        savebutt.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savebuttonActionPerformed(evt);
            }
        });
        JButton cancelbutt = new JButton("Cancel");
        cancelbutt.addActionListener(new ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveMap.this.setVisible(false);
                SaveMap.this.dispose();
            }
        });
        label1.setLocation(EDGE, EDGE);
        label1.setSize(label1.getPreferredSize());
        foldername.setLocation(2 * EDGE + label1.getWidth(), EDGE);
        foldername.setSize(foldername.getPreferredSize());
        label2.setLocation(EDGE, 2 * EDGE + foldername.getHeight());
        label2.setSize(label2.getPreferredSize());
        mapival.setLocation(2 * EDGE + label2.getWidth(), 2 * EDGE + foldername.getHeight());
        mapival.setColumns(3);
        mapival.setSize(mapival.getPreferredSize());
        mapjval.setLocation(3 * EDGE + label2.getWidth() + mapival.getWidth(), 2 * EDGE + foldername.getHeight());
        mapjval.setColumns(3);
        mapjval.setSize(mapjval.getPreferredSize());
        int nextrow = EDGE + mapjval.getLocation().y + mapjval.getHeight();
        savebutt.setLocation(EDGE, nextrow);
        savebutt.setSize(savebutt.getPreferredSize());
        cancelbutt.setLocation(2 * EDGE + savebutt.getWidth(), nextrow);
        cancelbutt.setSize(cancelbutt.getPreferredSize());
        nextrow += savebutt.getHeight() + EDGE;
        this.getContentPane().add(label1);
        this.getContentPane().add(foldername);
        this.getContentPane().add(label2);
        this.getContentPane().add(mapival);
        this.getContentPane().add(mapjval);
        this.getContentPane().add(savebutt);
        this.getContentPane().add(cancelbutt);
        this.setSize(300, EDGE + nextrow);
    }

    private void savebuttonActionPerformed(java.awt.event.ActionEvent evt) {
        String istr = mapival.getText();
        String jstr = mapjval.getText();
        int i, j;
        try {
            i = Integer.parseInt(istr);
            j = Integer.parseInt(jstr);
            if (i < 0 || j < 0) {
                JOptionPane.showMessageDialog(this, "i and j must be postive integers!", "Couldn't Save", JOptionPane.ERROR_MESSAGE);
                this.setVisible(false);
                this.dispose();
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "i and j must be postive integers!", "Couldn't Save", JOptionPane.ERROR_MESSAGE);
            this.setVisible(false);
            this.dispose();
            return;
        }
        File dir = new File(foldername.getText());
        if (!dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Folder must be a valid directory!", "Couldn't Save", JOptionPane.ERROR_MESSAGE);
            this.setVisible(false);
            this.dispose();
            return;
        }
        File file = new File(foldername.getText() + Main.getMapString(i, j));
        if (file.isFile()) {
            int o = JOptionPane.showConfirmDialog(this, "Warning! There is already a map with that name, Continue?", "Overwrite?", JOptionPane.NO_OPTION, JOptionPane.WARNING_MESSAGE);
            EditorFrame.print("Confirm dialog returned: " + o);
            if (o == 2 || o == 1) {
                this.setVisible(false);
                this.dispose();
                return;
            }
        }
        try {
            if (Map.saveMap(map, foldername.getText() + Main.getMapString(i, j))) JOptionPane.showMessageDialog(this, "Save of " + Main.getMapString(i, j) + " was sucessful!", jstr, JOptionPane.INFORMATION_MESSAGE); else throw new IOException();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Encountered error when trying to write map file!", "Couldn't Save", JOptionPane.ERROR_MESSAGE);
        }
        this.setVisible(false);
        this.dispose();
    }

    static void start(Map map, File mapfile) {
        SaveMap.map = map;
        SaveMap.mapfile = mapfile;
        try {
            if (Map.saveMap(map, mapfile.getPath())) JOptionPane.showMessageDialog(null, "Save of " + mapfile.getPath() + " was sucessful!", "fds", JOptionPane.INFORMATION_MESSAGE); else throw new IOException();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Encountered error when trying to write map file!", "Couldn't Save", JOptionPane.ERROR_MESSAGE);
        }
    }

    static void start(Map map) {
        SaveMap.map = map;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new SaveMap().setVisible(true);
            }
        });
    }
}

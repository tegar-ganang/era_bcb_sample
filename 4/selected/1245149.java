package gui;

import utilities.DrawingBean;
import simulation.GUIOptionManager;
import maps.MapsXMLManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * The frame used to define the terrain details of mobility
 * @author Arvanitis Ioannis
 */
public class TerrainPaintFrame extends javax.swing.JFrame implements Runnable {

    private int terrain_width;

    private int terrain_height;

    private Vector<DrawingBean> v;

    private final int MULT = 1;

    private final PropertyChangeSupport pcs;

    private Thread runner;

    /**
     * Sets the vector with the mobility details
     * @param v Vector with the mobility details
     */
    public void setV(Vector<DrawingBean> v) {
        this.pcs.firePropertyChange("v", this.v, v);
        this.v = v;
    }

    /**
     * Gets vector with the mobility details
     * @return the vector with the mobility details
     */
    public Vector<DrawingBean> getV() {
        return v;
    }

    /**
     * Creates new form TerrainPaintFrame
     * @param w Width (in cells) of canvas
     * @param h Height (in cells) of canvas
     */
    public TerrainPaintFrame(int w, int h) {
        this.terrain_width = w;
        this.terrain_height = h;
        pcs = new PropertyChangeSupport(this);
        runner = new Thread(this);
        runner.start();
    }

    public void run() {
        setTitle("Terrain paint");
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screensize = toolkit.getScreenSize();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        TerrainCanvasPanel canvas = new TerrainCanvasPanel(terrain_width, terrain_height);
        JPanel actions = new JPanel();
        final JRadioButton obstacle = new JRadioButton("Obstacle", true);
        final JRadioButton clearArea = new JRadioButton("Clear area", false);
        ButtonGroup color = new ButtonGroup();
        color.add(obstacle);
        color.add(clearArea);
        TerrainCanvasPanel.setColor(Color.BLACK);
        JButton ok = new JButton("Ok");
        JButton cancel = new JButton("Cancel");
        JButton clearAll = new JButton("Clear all");
        JButton save = new JButton("Save As...");
        JButton load = new JButton("Load");
        obstacle.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (obstacle.isSelected()) {
                    TerrainCanvasPanel.setColor(Color.BLACK);
                }
            }
        });
        clearArea.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (clearArea.isSelected()) {
                    TerrainCanvasPanel.setColor(Color.WHITE);
                }
            }
        });
        ok.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setV(TerrainCanvasPanel.getV());
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                TerrainCanvasPanel.getV().removeAllElements();
                dispose();
            }
        });
        clearAll.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                TerrainCanvasPanel.getV().removeAllElements();
                repaint();
            }
        });
        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                String cdpath = null;
                try {
                    cdpath = new java.io.File(".").getCanonicalPath() + "\\src\\maps";
                } catch (IOException ex) {
                    Logger.getLogger(TerrainPaintFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                chooser.setCurrentDirectory(new File(cdpath));
                int option = chooser.showSaveDialog(chooser);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        saveTerrain(file.getName(), TerrainCanvasPanel.getV());
                    } catch (IOException ex) {
                        Logger.getLogger(TerrainPaintFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        load.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                String cdpath = null;
                try {
                    cdpath = new java.io.File(".").getCanonicalPath() + "\\src\\maps";
                } catch (IOException ex) {
                    Logger.getLogger(TerrainPaintFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                chooser.setCurrentDirectory(new File(cdpath));
                int option = chooser.showOpenDialog(chooser);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try {
                        loadTerrain(file.getName());
                    } catch (IOException ex) {
                        Logger.getLogger(TerrainPaintFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        actions.setLayout(new GridLayout(1, 7));
        actions.add(obstacle);
        actions.add(clearArea);
        actions.add(ok);
        actions.add(cancel);
        actions.add(clearAll);
        actions.add(save);
        actions.add(load);
        getContentPane().add(canvas, BorderLayout.CENTER);
        getContentPane().add(actions, BorderLayout.SOUTH);
        setSize(canvas.getSize());
        int x = (int) (screensize.getWidth() - getWidth()) / 2;
        int y = (int) (screensize.getHeight() - getHeight()) / 2;
        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Saves the mobility details in xml file and picture
     * @param mapname Name of the map to save
     * @param v Vector with the mobility details
     * @throws java.io.IOException
     */
    public void saveTerrain(String mapname, Vector<DrawingBean> v) throws IOException {
        String[] formats = { "jpg", "png" };
        String format;
        boolean hasFormat = false;
        if (mapname.length() > 4) {
            format = mapname.substring(mapname.length() - 3).toLowerCase();
            for (int i = 0; i < formats.length; i++) {
                if (format.equalsIgnoreCase(formats[i])) {
                    hasFormat = true;
                    break;
                }
            }
            if (!hasFormat) {
                format = formats[0];
                mapname = mapname + "." + format;
            }
        } else {
            format = formats[0];
            mapname = mapname + "." + format;
        }
        String dstpath = new java.io.File(".").getCanonicalPath() + "\\src\\maps\\" + mapname;
        File dstfile = new File(dstpath);
        BufferedImage bi = new BufferedImage(terrain_width * MULT, terrain_height * MULT, BufferedImage.TYPE_INT_RGB);
        bi.createGraphics();
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, terrain_width * MULT, terrain_height * MULT);
        for (int i = 0; i < v.size(); i++) {
            g.setColor(v.get(i).getColor());
            g.fillRect(v.get(i).getPoint().x * MULT, v.get(i).getPoint().y * MULT, MULT, MULT);
        }
        if (dstfile.exists()) {
            int option = JOptionPane.showOptionDialog(this, "This file already exists. Overwrite it?", "NOTIFICATION", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (option == JOptionPane.YES_OPTION) {
                ImageIO.write(bi, format, dstfile);
                MapsXMLManager manager = new MapsXMLManager(mapname);
                manager.saveMap(GUIOptionManager.getMapWidthMeters(), GUIOptionManager.getMapHeightMeters(), terrain_width, terrain_height, v);
            }
        } else {
            ImageIO.write(bi, format, dstfile);
            MapsXMLManager manager = new MapsXMLManager(mapname);
            manager.saveMap(GUIOptionManager.getMapWidthMeters(), GUIOptionManager.getMapHeightMeters(), terrain_width, terrain_height, v);
        }
    }

    /**
     * Loads a map from a list and updates the canvas
     * @param mapname Name of the map to save
     * @throws java.io.IOException
     */
    private void loadTerrain(String mapname) throws IOException {
        MapsXMLManager manager = new MapsXMLManager(mapname);
        Vector<DrawingBean> tempv = manager.loadMap();
        if (tempv == null) {
            JOptionPane.showMessageDialog(null, "Sorry. This map does not exist.", "ERROR", JOptionPane.ERROR_MESSAGE);
        } else {
            TerrainCanvasPanel.setV(tempv);
            TerrainCanvasPanel.setX_cells(manager.getFragmentation().x);
            TerrainCanvasPanel.setY_cells(manager.getFragmentation().y);
            GUIOptionManager.setMapWidthMeters(manager.getCapacity().x);
            GUIOptionManager.setMapHeightMeters(manager.getCapacity().y);
            GUIOptionManager.setMapWidthObstacles(manager.getFragmentation().x);
            GUIOptionManager.setMapHeightObstacles(manager.getFragmentation().y);
            repaint();
        }
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(propertyName, listener);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 300, Short.MAX_VALUE));
        pack();
    }
}

package UADgraph;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import ForceGraph.Graph;
import org.xml.sax.SAXException;

/**
 *
 * @author Qwerty
 * TBD: make into Windows installer. http://launch4j.sourceforge.net/ or http://jsmooth.sourceforge.net/download.php
 *
 */
public class MainFrame extends javax.swing.JFrame implements KeyListener {

    public Graph graph = null;

    private LayoutManager layoutManager;

    private Thread t;

    private int refreshRate = 20;

    private final DisplayManager3D displayManager3D;

    private boolean draw = false;

    private int maxVisibleSet = 0;

    private final String titleString = "UAD Graph Viewer";

    /** Creates new form MainFrame */
    public MainFrame() {
        initComponents();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            classLoader.loadClass("javax.media.j3d.BoundingSphere");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Package required: Java3D is not installed.\nPlease download and install from:\n http://java.sun.com/javase/technologies/desktop/java3d/downloads/", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        displayManager3D = new DisplayManager3D(jpCanvas);
        layoutManager = new LayoutManager();
        displayManager3D.getCanvas().addKeyListener(this);
        t = new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        if (draw) {
                            if (graph != null) {
                                layoutManager.improveGraph();
                            }
                            displayManager3D.draw();
                        }
                        sleep(1000 / refreshRate);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NullPointerException ex) {
                        Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        t.setName("Graph update thread");
        t.start();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jpCanvas = new java.awt.Panel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jmFile = new javax.swing.JMenu();
        jmiLoadGraph = new javax.swing.JMenuItem();
        jmiSaveImage = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jmiExit = new javax.swing.JMenuItem();
        jmView = new javax.swing.JMenu();
        jmiShuffle = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jmiConstrain2D = new javax.swing.JCheckBoxMenuItem();
        jmiConstrainSphere = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jmiHome = new javax.swing.JMenuItem();
        jmHelp = new javax.swing.JMenu();
        jmiAbout = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("UAD Graph Viewer");
        javax.swing.GroupLayout jpCanvasLayout = new javax.swing.GroupLayout(jpCanvas);
        jpCanvas.setLayout(jpCanvasLayout);
        jpCanvasLayout.setHorizontalGroup(jpCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 685, Short.MAX_VALUE));
        jpCanvasLayout.setVerticalGroup(jpCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 361, Short.MAX_VALUE));
        jmFile.setText("File");
        jmiLoadGraph.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jmiLoadGraph.setText("Load Graph");
        jmiLoadGraph.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiLoadGraphActionPerformed(evt);
            }
        });
        jmFile.add(jmiLoadGraph);
        jmiSaveImage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jmiSaveImage.setText("Save As Image");
        jmiSaveImage.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiSaveImageActionPerformed(evt);
            }
        });
        jmFile.add(jmiSaveImage);
        jmFile.add(jSeparator3);
        jmiExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        jmiExit.setText("Exit");
        jmiExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiExitActionPerformed(evt);
            }
        });
        jmFile.add(jmiExit);
        jMenuBar1.add(jmFile);
        jmView.setText("View");
        jmiShuffle.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, 0));
        jmiShuffle.setText("Shuffle graph");
        jmiShuffle.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiShuffleActionPerformed(evt);
            }
        });
        jmView.add(jmiShuffle);
        jmView.add(jSeparator1);
        jmiConstrain2D.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_2, 0));
        jmiConstrain2D.setText("Constrain to 2D");
        jmiConstrain2D.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiConstrain2DActionPerformed(evt);
            }
        });
        jmView.add(jmiConstrain2D);
        jmiConstrainSphere.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, 0));
        jmiConstrainSphere.setText("Constrain to Sphere");
        jmiConstrainSphere.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiConstrainSphereActionPerformed(evt);
            }
        });
        jmView.add(jmiConstrainSphere);
        jmView.add(jSeparator2);
        jmiHome.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, 0));
        jmiHome.setText("Recentre View");
        jmiHome.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiHomeActionPerformed(evt);
            }
        });
        jmView.add(jmiHome);
        jMenuBar1.add(jmView);
        jmHelp.setText("Help");
        jmiAbout.setText("About");
        jmiAbout.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiAboutActionPerformed(evt);
            }
        });
        jmHelp.add(jmiAbout);
        jMenuBar1.add(jmHelp);
        setJMenuBar(jMenuBar1);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jpCanvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addComponent(jpCanvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        pack();
    }

    private void jmiLoadGraphActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser("./samples/");
        File file;
        MultiGraphMLReader reader = new MultiGraphMLReader();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            try {
                reader.read(file);
                setGraph(reader.getGraph());
                this.setTitle(titleString + " - " + file.getName());
            } catch (SAXException e) {
                JOptionPane.showMessageDialog(this, "Error: " + file + " is not a well-formed XML document", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading data from " + file, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void jmiExitActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void jmiShuffleActionPerformed(java.awt.event.ActionEvent evt) {
        layoutManager.scrambleGraph();
    }

    private void jmiConstrain2DActionPerformed(java.awt.event.ActionEvent evt) {
        layoutManager.setConstrainTo2D(!layoutManager.isConstrainTo2D());
    }

    private void jmiConstrainSphereActionPerformed(java.awt.event.ActionEvent evt) {
        layoutManager.setConstrainToSphere(!layoutManager.isConstrainToSphere());
    }

    private void jmiSaveImageActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fc = new JFileChooser("./samples/");
        FileFilter ff = new FileFilter() {

            public String getDescription() {
                return "JPEG Images";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String extension = (f.getName().substring(f.getName().lastIndexOf('.'))).toLowerCase();
                if (extension != null) {
                    extension = extension.substring(1);
                    if (extension.equals("jpeg") || extension.equals("jpg")) {
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        };
        fc.addChoosableFileFilter(ff);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists()) {
                Object[] options = { "OK", "Cancel" };
                if (JOptionPane.showOptionDialog(this, "File already exists:\n" + file.getName() + "\nDo you want to overwrite it?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]) == 1) {
                    return;
                }
            }
            try {
                displayManager3D.capture(file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Internal error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void jmiAboutActionPerformed(java.awt.event.ActionEvent evt) {
        JOptionPane.showConfirmDialog(this, "UAD Graph Viewer v1.0\nAuthor: Mark Shovman\nNov 2009", "About", JOptionPane.PLAIN_MESSAGE);
    }

    private void jmiHomeActionPerformed(java.awt.event.ActionEvent evt) {
        displayManager3D.resetView();
    }

    public void setGraph(Graph g) {
        draw = false;
        layoutManager.setGraph(g);
        layoutManager.scrambleGraph();
        displayManager3D.setGraph(g);
        graph = g;
        draw = true;
    }

    Graph getGraph() {
        return graph;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JSeparator jSeparator3;

    private javax.swing.JMenu jmFile;

    private javax.swing.JMenu jmHelp;

    private javax.swing.JMenu jmView;

    private javax.swing.JMenuItem jmiAbout;

    private javax.swing.JCheckBoxMenuItem jmiConstrain2D;

    private javax.swing.JCheckBoxMenuItem jmiConstrainSphere;

    private javax.swing.JMenuItem jmiExit;

    private javax.swing.JMenuItem jmiHome;

    private javax.swing.JMenuItem jmiLoadGraph;

    private javax.swing.JMenuItem jmiSaveImage;

    private javax.swing.JMenuItem jmiShuffle;

    private java.awt.Panel jpCanvas;

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    private void updateVisibleSets() {
        if (maxVisibleSet > graph.getNumberOfVertexSets()) {
            maxVisibleSet = graph.getNumberOfVertexSets();
        }
        if (maxVisibleSet < 0) {
            maxVisibleSet = 0;
        }
        for (int i = 0; i < graph.getNumberOfVertexSets(); i++) {
            graph.getVertexSet(i).setVisible(i < maxVisibleSet);
        }
    }
}
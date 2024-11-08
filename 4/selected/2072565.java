package UADgraphEditor;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import org.xml.sax.SAXException;
import javax.swing.*;
import java.awt.Color;
import java.io.*;

/**
 *
 * @author Mastrel
 */
public class App {

    private LinkChart linkChart = null;

    private Thread t;

    private int refreshRate = 20;

    private DisplayManager3D displayManager3D;

    private boolean draw = true;

    private final String titleString = "UAD Graph Editor";

    private static UIManager UI = new UIManager();

    private static App app = App.getInstance();

    public LayoutManager layoutManager = new LayoutManager();

    private LinkChartFileWriter writer = new LinkChartFileWriter();

    private StringEditor stred = new StringEditor();

    public LinkChartEditor editor = new LinkChartEditor(UI);

    public App() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            classLoader.loadClass("javax.media.j3d.BoundingSphere");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(UI, "Package required: Java3D is not installed.\nPlease download and install from:\n http://java.sun.com/javase/technologies/desktop/java3d/downloads/", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        displayManager3D = new DisplayManager3D(UI.jpViewCanvas);
        layoutManager = new LayoutManager();
        displayManager3D.getCanvas().addKeyListener(UI);
        createNewLinkChart();
        t = new Thread() {

            @Override
            public void run() {
                while (true) {
                    UI.jcbNodeGroupEditor.setSelected(UI.ngEdit.getFrame().isVisible());
                    UI.jcbLinkGroupEditor.setSelected(UI.lgEdit.getFrame().isVisible());
                    try {
                        if (draw) {
                            if (linkChart != null) {
                                if (!linkChart.isLocked()) {
                                    layoutManager.improveGraph();
                                }
                                draw();
                            }
                        }
                        sleep(1000 / refreshRate);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(UIManager.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NullPointerException ex) {
                        Logger.getLogger(UIManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        t.setName("Graph update thread");
        t.start();
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                UI.setVisible(true);
            }
        });
    }

    public LinkChart getChart() {
        return linkChart;
    }

    public static App getInstance() {
        if (app == null) {
            app = new App();
        }
        return app;
    }

    public void setChart(LinkChart lc) {
        linkChart = lc;
    }

    public void loadGraph() {
        JFileChooser fc = new JFileChooser("./samples/");
        File file;
        String filename = null;
        LinkChartFileReader reader = new LinkChartFileReader();
        if (fc.showOpenDialog(UI) == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            try {
                displayManager3D.clearScreen();
                reader.read(file);
                linkChart = reader.getChart();
                layoutManager.setGraph(linkChart.GetGraph());
                layoutManager.scrambleGraph();
                this.resetView();
                this.recentreChart();
                editor.setChart(linkChart);
                filename = file.getName();
                UI.setTitle(titleString + " - " + stred.removeNumCharsAtEnd(filename, 4));
                UI.ngEdit.update();
                UI.lgEdit.update();
            } catch (SAXException e) {
                JOptionPane.showMessageDialog(UI, "Error: " + file + " is not a well-formed XML document", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(UI, "Error reading data from " + file, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        linkChart.setLocked(false);
        linkChart.setDataChanged(false);
    }

    public void scrambleGraph() {
        layoutManager.scrambleGraph();
        recentreChart();
    }

    public void ConstrainTo2D() {
        layoutManager.setConstrainTo2D(!layoutManager.isConstrainTo2D());
    }

    public void ConstrainToSphere() {
        layoutManager.setConstrainToSphere(!layoutManager.isConstrainToSphere());
    }

    public void exitApplication() {
        System.exit(0);
    }

    public void resetView() {
        displayManager3D.resetView();
    }

    public void recentreChart() {
        layoutManager.recentre();
    }

    public void createNewLinkChart() {
        displayManager3D.clearScreen();
        linkChart = new LinkChart(displayManager3D);
        linkChart.AddLinkGroup("Default", Color.gray);
        linkChart.AddNodeGroup("Default", Color.red);
        UI.setTitle(titleString + " - " + "Untitled");
        layoutManager.setGraph(linkChart.GetGraph());
        layoutManager.scrambleGraph();
        this.resetView();
        this.recentreChart();
        linkChart.setLocked(true);
        editor.setChart(linkChart);
        UI.ngEdit.panel.removeAll();
        UI.lgEdit.panel.removeAll();
        javax.swing.SwingUtilities.updateComponentTreeUI(UI.ngEdit.panel);
        javax.swing.SwingUtilities.updateComponentTreeUI(UI.lgEdit.panel);
        linkChart.setDataChanged(false);
    }

    public void saveImage() {
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
        if (fc.showSaveDialog(UI) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists()) {
                Object[] options = { "OK", "Cancel" };
                if (JOptionPane.showOptionDialog(UI, "File already exists: " + file.getName() + "\nDo you want to overwrite it?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]) == 1) {
                    return;
                }
            }
            try {
                displayManager3D.capture(file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(UI, "Internal error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void saveChart() {
        JFileChooser fc = new JFileChooser("./samples/");
        int rc = fc.showDialog(null, "Save");
        if (rc == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f.exists()) {
                Object[] options = { "OK", "Cancel" };
                if (JOptionPane.showOptionDialog(UI, "File already exists: " + f.getName() + "\nDo you want to overwrite it?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]) == 1) {
                    return;
                }
            }
            String filepath = f.getAbsoluteFile().toString();
            String extention = ".xml";
            if (filepath.toLowerCase().endsWith(extention)) {
                writer.SaveXML(filepath);
            } else {
                writer.SaveXML(filepath + extention);
            }
        } else {
            System.out.println("Error: file was not saved!");
        }
        linkChart.setDataChanged(false);
        return;
    }

    public DisplayManager3D getDisplayManager() {
        return displayManager3D;
    }

    public UIManager getUIManager() {
        return UI;
    }

    private void draw() {
        linkChart.draw();
        if (linkChart.isDataChanged() == true && !UI.getTitle().endsWith("*")) {
            UI.setTitle(app.getUIManager().getTitle() + "*");
        } else if (linkChart.isDataChanged() == false && UI.getTitle().endsWith("*")) {
            UI.setTitle(stred.removeNumCharsAtEnd(UI.getTitle(), 1));
        }
    }
}

package vademecum.ui.visualizer.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JPanel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vademecum.Core;
import vademecum.core.experiment.ExperimentNode;
import vademecum.ui.project.Expertice;
import vademecum.ui.visualizer.GraphicsViewer;
import vademecum.ui.visualizer.utils.UniqueIDSet;
import vademecum.ui.visualizer.vgraphics.AbstractInteraction;
import vademecum.ui.visualizer.vgraphics.IPlotable;
import vademecum.ui.visualizer.vgraphics.VGraphics;

/**
 * XplorePanel
 * containing typically one or two Charts and
 * with optional Annotations
 * 
 * All containing and visible elements are extensions 
 * of the VGraphics-Class which itself is a common JComponent with additions for the Visualizer-Framework
 */
public class XplorePanel extends JPanel implements MouseListener {

    /**
	 * Logger instance
	 */
    private static Log log = LogFactory.getLog(XplorePanel.class);

    /**
	 * Reference to the belonging Expertice
	 */
    public Expertice expertice;

    /**
	 * Reference to the GraphicalEditor (e.g. 'VisualizerFrame')
	 */
    GraphicsViewer gViewer;

    /**
	 * Backgroundcolor of the XPlorePanel
	 */
    private Color colorBackground = Color.GRAY;

    /**
	 * Boolean Flag for the Visibility of SnapGrid
	 */
    boolean showGrid = false;

    /**
	 * Holding current switched Interaction-Mode
	 */
    int currentMode = 0;

    /**
	 * Boolean Flag indicating that any animations are running
	 */
    boolean frozen = false;

    int frozenMode = 0;

    /**
	 * Spacing of the Snapgrid
	 */
    int gridSpacing = 20;

    /**
	 * Link to the belonging ENode
	 */
    ExperimentNode sourceNode;

    /**
	 * ID set for the VGraphics Objects
	 */
    UniqueIDSet idSet;

    public XplorePanel() {
        super(false);
        setOpaque(true);
        setLayout(null);
        addMouseListener(this);
        this.setBackground(colorBackground);
        this.setVisible(true);
        idSet = new UniqueIDSet();
        try {
            this.expertice = (Expertice) Core.projectPanel.getSelectedComponent();
        } catch (Exception e) {
            System.out.println("no expertice found");
            e.printStackTrace();
        }
    }

    public XplorePanel(ExperimentNode eNode) {
        super(false);
        setOpaque(true);
        setLayout(null);
        addMouseListener(this);
        this.setBackground(colorBackground);
        this.setVisible(true);
        idSet = new UniqueIDSet();
        try {
            this.expertice = (Expertice) Core.projectPanel.getSelectedComponent();
        } catch (Exception e) {
            System.out.println("no expertice found");
            e.printStackTrace();
        }
        this.sourceNode = eNode;
    }

    /**
	 * @deprecated use XplorePanel() - Constructor instead
	 * @param buffering
	 */
    public XplorePanel(boolean buffering) {
        super(false);
        setOpaque(true);
        setLayout(null);
        addMouseListener(this);
        this.setBackground(colorBackground);
        this.setVisible(true);
        try {
            this.expertice = (Expertice) Core.projectPanel.getSelectedComponent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	* Add a VGraphics-Object to the Panel
	* 
	*/
    public void addViewable(VGraphics jc) {
        jc.setXplorePanel(this);
        jc.setID(idSet.getNextID());
        add(jc);
        addPropertyChangeListener("modechange", jc);
        jc.addMouseMotionListener(gViewer);
        if (jc.hasLinkedVGraphics() == true) {
            ArrayList<VGraphics> linkedVG = jc.getLinkedVGraphics();
            for (VGraphics v : linkedVG) {
                Component[] ca = this.getComponents();
                for (int i = 0; i < ca.length; i++) {
                    if (!v.equals(ca[i])) {
                        v.setID(idSet.getNextID());
                        this.addViewable(v);
                    }
                }
            }
        }
        adjustZOrder();
    }

    /**
	 * Getting all components which implements the IPlotable interface
	 * e.g. 2DScatterplot 
	 * @return
	 */
    public ArrayList<IPlotable> getPlotables() {
        ArrayList<IPlotable> list = new ArrayList<IPlotable>();
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (comp instanceof IPlotable) {
                list.add((IPlotable) comp);
            }
        }
        return list;
    }

    /**
	 * Getting all components which inherits from VGraphics
	 * (most of visible components)
	 * needed for restoring State
	 * @return
	 */
    public ArrayList<VGraphics> getViewables() {
        ArrayList<VGraphics> list = new ArrayList<VGraphics>();
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (comp instanceof VGraphics) {
                list.add((VGraphics) comp);
            }
        }
        return list;
    }

    /**
	 * Init the ZOrder of the VGraphics-Objects
	 */
    private void adjustZOrder() {
        Component[] ca = this.getComponents();
        Arrays.sort(ca);
        log.debug("Adjust ZOrder");
        log.debug(Arrays.toString(ca));
        for (int i = 0; i < ca.length; i++) {
            this.setComponentZOrder(ca[i], i);
        }
    }

    public void addViewableID(int id) {
        idSet.addID(id);
    }

    public int nextViewableID() {
        return idSet.getNextID();
    }

    public void releaseViewableID(int id) {
        idSet.releaseID(id);
    }

    public void clearViewableIDs() {
        idSet.clearIDs();
    }

    public VGraphics getLinkedVGraphicsByID(int id) {
        ArrayList<VGraphics> list = this.getViewables();
        for (VGraphics vg : list) {
            if (vg.getID() == id) return vg;
        }
        return null;
    }

    /**
	  * Setting up the GraphicalViewer (Frame)
	  * @param frame
	  */
    public void setGraphicalViewer(GraphicsViewer frame) {
        this.gViewer = frame;
        this.gViewer.updateViewer();
    }

    /**
	  * Getting the GraphicalViewer for that panel
	  * @return 
	  */
    public GraphicsViewer getGraphicalViewer() {
        return this.gViewer;
    }

    /**
	  * Setting up Interaction-Mode
	  * Panel will in return notify all containing VGraphics (e.g. Plots, Titlebar)
	  * 
	  * @param modeFlag
	  */
    public void setMode(int modeFlag) {
        int oldMode = this.currentMode;
        firePropertyChange("modechange", oldMode, modeFlag);
        this.currentMode = modeFlag;
    }

    /**
	  * Query current Interaction Mode
	  *
	  */
    public int getMode() {
        return this.currentMode;
    }

    /**
	  * Setting up Gridspacing
	  * @param spacing
	  */
    public void setGridSpacing(int spacing) {
        this.gridSpacing = spacing;
    }

    /**
	  * Returning Gridspacing of the explorePanel/Resultpanel
	  * @return
	  */
    public int getGridSpacing() {
        return this.gridSpacing;
    }

    /**
	  * Set on the visibility for the snapgrid
	  * @param b
	  */
    public void showGrid(boolean b) {
        this.showGrid = b;
    }

    /**
	  * Returns wheter snapgrid is visible or not
	  * @return boolean
	  */
    public boolean getGridState() {
        return this.showGrid;
    }

    /**
	  * Stopping all animations.
	  * The state of this panel and all containing VGraphics is now stable.
	  * Method is automatically invoked, if GraphicalEditor(VisualizerFrame) will be closed.
	  */
    public void freeze() {
        if (this.frozen == false) {
            Component[] ca = this.getComponents();
            for (int i = 0; i < ca.length; i++) {
                if (ca[i] instanceof VGraphics) {
                    VGraphics vg = (VGraphics) ca[i];
                    vg.freeze();
                }
            }
            this.frozen = true;
        }
        frozenMode = this.getMode();
        this.setMode(0);
    }

    /**
	  * Returns wheter Panel is frozen
	  * (no animation is running in this state)
	  * @return
	  */
    public boolean isFrozen() {
        return this.frozen;
    }

    /**
	  * 
	  * Is invoked, if an former instance of the XplorePanel has
	  * to be restored.
	  */
    public void melt() {
        if (this.frozen == true) {
            Component[] ca = this.getComponents();
            for (int i = 0; i < ca.length; i++) {
                if (ca[i] instanceof VGraphics) {
                    VGraphics vg = (VGraphics) ca[i];
                    vg.melt();
                }
            }
            this.frozen = false;
        }
        this.setMode(frozenMode);
    }

    /**
	  * Utility Function,
	  * which iterates through all VGraphics and add it to the set 
	  * of all Features
	  * @return
	  */
    public Vector<AbstractInteraction> getUniqueFeatures() {
        Vector<AbstractInteraction> v = new Vector<AbstractInteraction>();
        Component[] ca = this.getComponents();
        for (int i = 0; i < ca.length; i++) {
            System.out.println(ca[i]);
            if (ca[i] instanceof VGraphics) {
                log.debug("Getting FeatureSet");
                VGraphics vg = (VGraphics) ca[i];
                ArrayList<AbstractInteraction> featList = vg.getFeatureList();
                log.debug("NUMFeatures = " + featList.size());
                for (AbstractInteraction feat : featList) {
                    if (!v.contains(feat)) {
                        v.add(feat);
                    }
                }
            }
        }
        java.util.Collections.sort(v);
        return v;
    }

    /**
	  * Set Expertice for this XplorePanel
	  * @param expertice
	  */
    public void setExpertice(Expertice expertice) {
        this.expertice = expertice;
    }

    /**
	  * Get Expertice of this XplorePanel
	  * @return
	  */
    public Expertice getExpertice() {
        return this.expertice;
    }

    /**
	  * Check if an expertice exist for this XplorePanel
	  * @return
	  */
    public boolean hasExpertice() {
        if (expertice != null) return true;
        return false;
    }

    /**
	  * Setting up the ExperimentNode to which the
	  * panel/chart should belong to
	  * 
	  * perDefault this has to be done at the constructor
	  * of the panel-class
	  */
    public void setSourceNode(ExperimentNode n) {
        this.sourceNode = n;
    }

    /**
	   * Getting the ExperimentNode to which the 
	   * chart belongs to
	   * @return
	   */
    public ExperimentNode getSourceNode() {
        return this.sourceNode;
    }

    public BufferedImage getThumbnail() {
        int width = getWidth();
        int height = getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2D = image.createGraphics();
        paint(g2D);
        return image;
    }

    /**
	  * LabelText for the ResultItem(RI)
	  * @return
	  */
    public String getRILabelString() {
        String plotLabel = "Plot";
        ArrayList<IPlotable> plots = this.getPlotables();
        if (plots.size() > 0) {
            IPlotable plot = plots.get(0);
        }
        for (int i = 0; i < plots.size(); i++) {
            final IPlotable plot = plots.get(i);
            plotLabel = plot.getPlotMenuLabel();
        }
        return plotLabel;
    }

    /**
	  * Tooltip-text for the ResultItem(RI)
	  * @return
	  */
    public String getRITooltipString() {
        String s = "Figure";
        ArrayList<IPlotable> plots = this.getPlotables();
        if (plots.size() > 0) {
            IPlotable plot = plots.get(0);
        }
        for (int i = 0; i < plots.size(); i++) {
            final IPlotable plot = plots.get(i);
            s = plot.getPlotMenuLabel();
        }
        return s;
    }

    /**
	  * Fetching the Properties of the panel 
	  * and underlaying structures
	  * @return CompoundProperties
	  */
    public Properties getProperties() {
        Properties p = new Properties();
        String prefix = "Panel_";
        p.setProperty(prefix + "name", "XplorePanel");
        p.setProperty(prefix + "gridspacing", Integer.toString(this.gridSpacing));
        p.setProperty(prefix + "backgroundRGB", Integer.toString(this.getBackground().getRGB()));
        p.setProperty(prefix + "NUMVGraphics", Integer.toString(this.getViewables().size()));
        ArrayList<VGraphics> list = this.getViewables();
        int vgno = 0;
        for (VGraphics vg : list) {
            ++vgno;
            p.setProperty(prefix + "VG" + vgno, Integer.toString(vg.getID()));
            Properties p2 = vg.getProperties();
            Enumeration keyEn = p2.keys();
            while (keyEn.hasMoreElements()) {
                String key = (String) keyEn.nextElement();
                p.setProperty(key, p2.getProperty(key));
            }
        }
        return p;
    }

    /**
	  * Setting Properties of the Panel
	  * @param p Properties
	  */
    public void setProperties(Properties p) {
        String prefix = "Panel_";
        this.gridSpacing = Integer.parseInt(p.getProperty(prefix + "gridspacing"));
        this.setBackground(new Color(Integer.parseInt(p.getProperty(prefix + "backgroundRGB"))));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.showGrid == true) {
            drawGrid(g);
        }
    }

    /**
	  * Drawing the snapgrid
	  * @param g
	  */
    private void drawGrid(Graphics g) {
        Color previous = g.getColor();
        g.setColor(Color.black);
        int width = getSize().width, height = getSize().height;
        for (int i = 0; i < width; i += this.gridSpacing) {
            Point dest = new Point(i, 0);
            g.fillRect(dest.x, dest.y, 1, height);
        }
        for (int i = 0; i < height; i += this.gridSpacing) {
            g.fillRect(0, i, width, 1);
        }
        g.setColor(previous);
    }

    public void mousePressed(MouseEvent e) {
        System.out.println("Xplorepanel : Mouse Pressed!");
        Point cp = e.getPoint();
        System.out.println(cp);
        Component c = getComponentAt(e.getPoint().x, e.getPoint().y);
        System.out.println(c.toString());
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }
}

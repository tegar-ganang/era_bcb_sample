package vademecum.ui.visualizer.panel;

import java.awt.BorderLayout;
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
 * Figure(-panel) containing one plot with 
 * optional annotations
 * 
 * All containing and visible elements are extensions 
 * of the VGraphics-Class which itself is a common JComponent with 
 * additions for managing features
 */
public class FigurePanel extends JPanel implements MouseListener {

    /**
	 * Logger instance
	 */
    private static Log log = LogFactory.getLog(FigurePanel.class);

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

    /**	Unique Identifier 	*/
    int id = 0;

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

    public static final int CENTER = 0;

    public static final int NORTH = 1;

    public static final int EAST = 2;

    public static final int SOUTH = 3;

    public static final int WEST = 4;

    public static final int NORTHWEST = 5;

    public static final int NORTHEAST = 6;

    public static final int SOUTHWEST = 7;

    public static final int SOUTHEAST = 8;

    ArrayList<VGraphics> vgCollection = new ArrayList<VGraphics>();

    /**
	 * Link to the belonging ENode
	 */
    ExperimentNode sourceNode;

    /**
	 * ID set for the VGraphics Objects
	 */
    UniqueIDSet idSet;

    public FigurePanel() {
        super(true);
        setOpaque(true);
        setLayout(new BorderLayout());
        addMouseListener(this);
        ;
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

    public FigurePanel(ExperimentNode eNode) {
        super(true);
        setOpaque(true);
        setLayout(new BorderLayout());
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
	 * Unique Figure ID within the Viewer
	 * @param id
	 */
    public void setID(int id) {
        this.id = id;
    }

    /**
	 * Unique Figure ID within the Viewer
	 * @return Figure ID
	 */
    public int getID() {
        return this.id;
    }

    /**
	* Add a VGraphics-Object to the Panel
	* 
	*/
    public void addPlot(VGraphics plot) {
        plot.setType(VGraphics.TYPE_PLOT);
        vgCollection.add(plot);
        plot.setFigurePanel(this);
        plot.setID(idSet.getNextID());
        add(plot, BorderLayout.CENTER);
        addPropertyChangeListener("modechange", plot);
        plot.addMouseMotionListener(gViewer);
        if (plot.hasLinkedVGraphics() == true) {
            ArrayList<VGraphics> linkedVG = plot.getLinkedVGraphics();
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
	 * Translate Orientation Flag to BorderLayout Layout String
	 * @param flag
	 * @return
	 */
    public Vector<String> translateOrientation(int flag) {
        Vector<String> v = new Vector<String>();
        switch(flag) {
            case 0:
                v.add(BorderLayout.CENTER);
                break;
            case 1:
                v.add(BorderLayout.NORTH);
                break;
            case 2:
                v.add(BorderLayout.EAST);
                break;
            case 3:
                v.add(BorderLayout.SOUTH);
                break;
            case 4:
                v.add(BorderLayout.WEST);
                break;
            case 5:
                v.add(BorderLayout.WEST);
                v.add(BorderLayout.NORTH);
                break;
            case 6:
                v.add(BorderLayout.EAST);
                v.add(BorderLayout.NORTH);
                break;
            case 7:
                v.add(BorderLayout.WEST);
                v.add(BorderLayout.SOUTH);
                break;
            case 8:
                v.add(BorderLayout.EAST);
                v.add(BorderLayout.SOUTH);
                break;
            default:
                v.add(BorderLayout.CENTER);
                break;
        }
        return v;
    }

    public void addWidget(VGraphics widget) {
        widget.setType(VGraphics.TYPE_WIDGET);
        int orient = widget.getOrientation();
        Vector<String> posvec = translateOrientation(orient);
        if (posvec.size() == 1) {
            addWidget(widget, posvec.get(0));
        } else {
            addWidget(widget, posvec.get(0), posvec.get(1));
        }
    }

    /**
	 * Adds a Widget to position NORTH, EAST, SOUTH or WEST
	 * +---------+
	 * |    n    |
	 * + w |c| e |
	 * |    s    |
	 * +----+----+
	 * @param widget
	 * @param position
	 */
    public void addWidget(VGraphics widget, String position) {
        widget.setType(VGraphics.TYPE_WIDGET);
        vgCollection.add(widget);
        widget.setFigurePanel(this);
        widget.setID(idSet.getNextID());
        JPanel holder = new JPanel();
        holder.setBackground(Color.white);
        System.out.println("jc size : " + widget.getSize());
        System.out.println("jc preferred : " + widget.getPreferredSize());
        System.out.println("holder bounds : " + holder.getBounds());
        holder.add(widget);
        add(holder, position);
        addPropertyChangeListener("modechange", widget);
        widget.addMouseMotionListener(gViewer);
        if (widget.hasLinkedVGraphics() == true) {
            ArrayList<VGraphics> linkedVG = widget.getLinkedVGraphics();
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
    }

    /**
	 * Adds a Widget to positions eastnorth, eastwest or 
	 * westnorth, westsouth
	 * pos1 : west or east
	 * pos2 : north or south
	 * @param widget
	 * @param position
	 */
    public void addWidget(VGraphics widget, String pos1, String pos2) {
        widget.setType(VGraphics.TYPE_WIDGET);
        vgCollection.add(widget);
        widget.setFigurePanel(this);
        widget.setID(idSet.getNextID());
        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        holder.setBackground(Color.white);
        System.out.println("jc size : " + widget.getSize());
        System.out.println("jc preferred : " + widget.getPreferredSize());
        System.out.println("holder bounds : " + holder.getBounds());
        holder.add(widget, pos2);
        add(holder, pos1);
        addPropertyChangeListener("modechange", widget);
        widget.addMouseMotionListener(gViewer);
        if (widget.hasLinkedVGraphics() == true) {
            ArrayList<VGraphics> linkedVG = widget.getLinkedVGraphics();
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
    }

    public void addWidget(Component widget, int figorient) {
        Vector<String> posvec = translateOrientation(figorient);
        String position = posvec.get(0);
        JPanel holder = new JPanel();
        holder.setBackground(Color.white);
        System.out.println("jc size : " + widget.getSize());
        System.out.println("jc preferred : " + widget.getPreferredSize());
        System.out.println("holder bounds : " + holder.getBounds());
        holder.add(widget);
        add(holder, position);
        widget.addMouseMotionListener(gViewer);
    }

    public void removeWidget(VGraphics widget) {
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (comp instanceof VGraphics) {
                if (comp.equals(widget)) {
                    this.remove(comp);
                    vgCollection.remove(widget);
                    releaseViewableID(widget.getID());
                }
            } else if (comp instanceof JPanel) {
                removeWidgetRek((JPanel) comp, widget);
            }
        }
    }

    private void removeWidgetRek(JPanel panel, VGraphics widget) {
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (comp instanceof VGraphics) {
                panel.remove(comp);
                vgCollection.remove(widget);
                releaseViewableID(widget.getID());
            } else if (comp instanceof JPanel) {
                removeWidgetRek((JPanel) comp, widget);
            }
        }
    }

    private ArrayList<VGraphics> getVGraphicsOfPanel(JPanel panel) {
        ArrayList<VGraphics> list = new ArrayList<VGraphics>();
        for (int i = 0; i < panel.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (comp instanceof VGraphics) {
                log.debug("VGraphics found inside a panel : " + (VGraphics) comp);
                list.add((VGraphics) comp);
            } else if (comp instanceof JPanel) {
                list.addAll(getVGraphicsOfPanel((JPanel) comp));
            }
        }
        return list;
    }

    public ArrayList<Component> getAllComponents() {
        ArrayList<Component> list = new ArrayList<Component>();
        log.debug("num components : " + this.getComponentCount());
        for (int i = 0; i < this.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (comp instanceof JPanel) {
                log.debug("JPanel inside!");
                list.addAll(getAllComponentsOfPanel((JPanel) comp));
            } else if (comp instanceof VGraphics) {
                list.add((VGraphics) comp);
            } else if (comp instanceof Component) {
                list.add(comp);
            }
        }
        log.debug("final list : ");
        for (Component vdeb : list) {
            log.debug(vdeb);
        }
        return list;
    }

    private ArrayList<Component> getAllComponentsOfPanel(JPanel panel) {
        ArrayList<Component> list = new ArrayList<Component>();
        for (int i = 0; i < panel.getComponentCount(); i++) {
            Component comp = this.getComponent(i);
            if (!comp.equals(panel)) {
                if (comp instanceof JPanel) {
                    list.addAll(getAllComponentsOfPanel((JPanel) comp));
                } else if (comp instanceof VGraphics) {
                    log.debug("VGraphics found inside a panel : " + (VGraphics) comp);
                    list.add((VGraphics) comp);
                } else if (comp instanceof Component) {
                    list.add(comp);
                }
            }
        }
        return list;
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
	 * needed for restoring State
	 * @return
	 */
    public ArrayList<VGraphics> getViewables() {
        return this.vgCollection;
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
        for (Component ca : getAllComponents()) {
            if (ca instanceof VGraphics) {
                log.debug("Getting FeatureSet");
                VGraphics vg = (VGraphics) ca;
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
        prefix = prefix + String.valueOf(getID());
        p.setProperty(prefix + "name", "FigurePanel");
        p.setProperty(prefix + "gridspacing", Integer.toString(this.gridSpacing));
        p.setProperty(prefix + "backgroundRGB", Integer.toString(this.getBackground().getRGB()));
        p.setProperty(prefix + "NUMVGraphics", Integer.toString(this.getViewables().size()));
        ArrayList<VGraphics> list = this.getViewables();
        int vgno = 0;
        for (VGraphics vg : list) {
            ++vgno;
            p.setProperty(prefix + "VG" + vgno, Integer.toString(vg.getID()));
            log.debug(prefix + "VG" + vgno);
            Properties p2 = vg.getProperties();
            Enumeration keyEn = p2.keys();
            while (keyEn.hasMoreElements()) {
                String key = (String) keyEn.nextElement();
                String pankey = prefix + key;
                p.setProperty(pankey, p2.getProperty(key));
            }
        }
        return p;
    }

    /**
	  * Setting Properties of the Panel
	  * @param p Properties
	  */
    public void setProperties(Properties p) {
        String prefix = "Panel_" + String.valueOf(getID());
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

    /**
	 * @deprecated use addPlot or addWidget instead
	 */
    public void addViewable(VGraphics vg) {
        addPlot(vg);
    }
}

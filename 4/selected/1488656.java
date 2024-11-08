package mapdesigner;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import DnD.DragWindow;
import DnD.DragEvent;
import DnD.DragListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import svg.SVGConstructor;

/**
 *
 * @author Richard
 * @todo implement context menu to change ID (Tree and pnlRenderer)
 * @todo implement continent colouring (see {@link myriadempires.UI.lobby.MapPreview})
 * @todo implement menu to allow loading / saving / access to other features
 */
public class MapDesigner extends javax.swing.JFrame {

    public enum ERR {

        INVALID_ID(true), DUPLICATE_ID(true), INVALID_NAME, DUPLICATE_NAME, ORPHAN_TERRITORY, PATHING_ERROR;

        private final boolean abort;

        private ERR() {
            this(false);
        }

        private ERR(boolean abort) {
            this.abort = abort;
        }

        public boolean mustAbort() {
            return abort;
        }
    }

    public enum FileType {

        SVG("Scalable Vector Graphics file", "svg"), MAP("Myriad Empires map file", "zmap", "xmap"), XMAP("Myriad Empires uncompressed map file", "xmap"), ZMAP("Myriad Empires compressed map file", "zmap");

        public final class FileFilter extends javax.swing.filechooser.FileFilter {

            @Override
            public boolean accept(File f) {
                for (String e : ext) {
                    if (f.getName().endsWith(e)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String getDescription() {
                return fullDesc;
            }

            public FileType getType() {
                return FileType.this;
            }
        }

        public final String name;

        public final String[] ext;

        public final String fullDesc;

        private FileType(String name, String... ext) {
            this.name = name;
            this.ext = ext;
            String tFullDesc = name.trim() + " (";
            for (String e : ext) {
                tFullDesc += "*." + e.trim() + ",";
            }
            fullDesc = tFullDesc.substring(0, tFullDesc.length() - 1).trim() + ")";
        }

        public boolean accept(File filename) {
            String e = filename.getName().substring(filename.getName().lastIndexOf("."));
            return true;
        }

        public javax.swing.filechooser.FileFilter ff() {
            return new FileFilter();
        }
    }

    public static class Status {

        public boolean lockSelection = false;

        public boolean CxC = false;

        public HashMap<MapComponent, HashSet<ERR>> errata = new HashMap<MapComponent, HashSet<ERR>>();
    }

    DragListener dl = new DragListener() {

        public void dragEnter(DragEvent evt) {
            dragOver(evt);
        }

        public void dragOver(DragEvent evt) {
            for (int i = 0; i < pnlTree.getComponents().length; i++) {
                Component e = pnlTree.getComponents()[i];
                Point p = evt.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(p, e);
                if (e.contains(p)) {
                    pnlTree.revalidate();
                    pnlTree.repaint();
                    break;
                }
            }
        }

        public void dragExit(DragEvent evt) {
        }

        public void dragTerminate(DragEvent evt) {
            Enumeration<TreePath> e = pnlTree.getExpandedDescendants(new TreePath(root));
            refreshTreeData();
            for (TreePath tp : Collections.list(e)) {
                pnlTree.expandPath(tp);
            }
        }
    };

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();

    private final Status status = new Status();

    private File mapFile = null;

    private TreeSet<String> errors = new TreeSet<String>();

    private boolean mustAbort = false;

    private JFileChooser fc = new JFileChooser();

    /** Creates new form MapDesigner */
    public MapDesigner() {
        initComponents();
        DragWindow.registerDropTarget(pnlTree);
        DragWindow.addDragListener(dl, pnlTree);
    }

    public void loadSVG(File f) throws FileNotFoundException, IOException, Exception {
        FileInputStream fis = new FileInputStream(f);
        pnlRenderer.renderStream(fis, FileType.SVG);
        fis.close();
        refreshTreeData();
        sldZoom.setValue((int) (1000 * Math.log10(pnlRenderer.getZoom())));
    }

    public void loadXMap(File f) throws FileNotFoundException, IOException, Exception {
        FileInputStream fis = new FileInputStream(f);
        pnlRenderer.renderStream(fis, FileType.XMAP);
        fis.close();
        refreshTreeData();
        sldZoom.setValue((int) (1000 * Math.log10(pnlRenderer.getZoom())));
    }

    public void loadZMap(File f) throws FileNotFoundException, IOException, Exception {
        FileInputStream fis = new FileInputStream(f);
        GZIPInputStream zis = new GZIPInputStream(fis);
        pnlRenderer.renderStream(zis, FileType.ZMAP);
        zis.close();
        fis.close();
        refreshTreeData();
        sldZoom.setValue((int) (1000 * Math.log10(pnlRenderer.getZoom())));
    }

    private void refreshTreeData() {
        LinkedList<ContData> l = pnlRenderer.getList();
        root.removeAllChildren();
        for (ContData cd : l) {
            root.add(cd);
        }
        pnlTree.setModel(new DefaultTreeModel(root));
    }

    private void repaintTree() {
        Enumeration<TreePath> e = pnlTree.getExpandedDescendants(new TreePath(root));
        ((DefaultTreeModel) pnlTree.getModel()).reload();
        if (e != null) {
            for (TreePath tp : Collections.list(e)) {
                pnlTree.expandPath(tp);
            }
        }
        pnlTree.revalidate();
    }

    private void createContinent() {
        String name = JOptionPane.showInputDialog("Enter a name for this new continent:");
        if (name == null || name.length() < 1) {
            return;
        }
        pnlRenderer.newContinent(name);
        refreshTreeData();
    }

    private void renameTerritory() {
        MapComponent mc = (MapComponent) pnlTree.getSelectionPath().getLastPathComponent();
        mc.setName(JOptionPane.showInputDialog("Rename Territory:", mc.getName()));
    }

    private void toggleLock() {
        status.lockSelection = !status.lockSelection;
        mnuTerrLockSel.setSelected(status.lockSelection);
        mnuEditLockSel.setSelected(status.lockSelection);
        cmdLockSel.setSelected(status.lockSelection);
        pnlRenderer.repaint();
    }

    /**
	 * Toggle Colour-by-Continent
	 */
    private void toggleCxC() {
        status.CxC = !status.CxC;
        mnuContCxC.setSelected(status.CxC);
        mnuEditCxC.setSelected(status.CxC);
        cmdCxC.setSelected(status.CxC);
        pnlRenderer.repaint();
        pnlTree.treeDidChange();
        pnlTree.validate();
    }

    private String genBorderString(TerrData td) {
        String borders = " ";
        for (TerrData b : td.borders) {
            borders += b.getID() + ",";
        }
        return borders.substring(0, borders.length() - 1).trim();
    }

    private void outputXMLData(OutputStream os) throws ParserConfigurationException, TransformerException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document dom;
        db = dbf.newDocumentBuilder();
        dom = db.newDocument();
        Element map = dom.createElement("map");
        for (ContData cd : pnlRenderer.getList()) {
            Element contEl = dom.createElement("continent");
            contEl.setAttribute("id", cd.getID());
            contEl.setAttribute("name", cd.getName());
            contEl.setAttribute("bonus", new Integer(cd.getBonus()).toString());
            for (TerrData td : cd.ters) {
                Element terrEl = dom.createElement("territory");
                terrEl.setAttribute("id", td.getID());
                terrEl.setAttribute("name", td.getName());
                terrEl.setAttribute("borders", genBorderString(td));
                terrEl.setAttribute("pathdata", SVGConstructor.fromShape(td));
                contEl.appendChild(terrEl);
            }
            if (contEl.hasChildNodes()) {
                map.appendChild(contEl);
            }
        }
        dom.appendChild(map);
        TransformerFactory txf = TransformerFactory.newInstance();
        Transformer tx = txf.newTransformer();
        tx.setOutputProperty(OutputKeys.STANDALONE, "yes");
        tx.setOutputProperty(OutputKeys.METHOD, "xml");
        tx.setOutputProperty(OutputKeys.INDENT, "yes");
        StreamResult result = new StreamResult(os);
        DOMSource source = new DOMSource(dom);
        tx.transform(source, result);
    }

    private void checkValidity() {
        HashMap<String, MapComponent> idSet = new HashMap<String, MapComponent>();
        HashMap<String, MapComponent> CNameSet = new HashMap<String, MapComponent>();
        HashMap<String, MapComponent> TNameSet = new HashMap<String, MapComponent>();
        for (ContData cd : pnlRenderer.getList()) {
            if ((cd.getID() == null || cd.getID().length() < 1) && cd != ContData.NullCont) {
                flagErr(ERR.INVALID_ID, cd);
            } else if (idSet.containsKey(cd.getID())) {
                flagErr(ERR.DUPLICATE_ID, idSet.get(cd.getID()), cd);
            }
            if (cd.getName() == null || cd.getName().length() < 1) {
                flagErr(ERR.INVALID_NAME, cd);
            } else if (CNameSet.containsKey(cd.getName())) {
                flagErr(ERR.DUPLICATE_NAME, CNameSet.get(cd.getName()), cd);
            }
            for (TerrData td : cd.ters) {
                if (td.getID() == null || td.getID().length() < 1) {
                    flagErr(ERR.INVALID_ID, td);
                } else if (idSet.containsKey(td.getID())) {
                    flagErr(ERR.DUPLICATE_ID, idSet.get(td.getID()), td);
                }
                if (td.getName() == null || td.getName().length() < 1) {
                    flagErr(ERR.INVALID_NAME, td);
                } else if (TNameSet.containsKey(td.getName())) {
                    flagErr(ERR.DUPLICATE_NAME, TNameSet.get(td.getName()), td);
                }
                if (td.borders.size() < 1) {
                    flagErr(ERR.ORPHAN_TERRITORY, td);
                }
            }
        }
    }

    private void checkPathing() {
        LinkedList<TerrData> map = new LinkedList<TerrData>();
        for (ContData cd : pnlRenderer.getList()) {
            for (TerrData td : cd.ters) {
                if (td != null) {
                    map.add(td);
                }
            }
        }
        final HashMap<TerrData, Double> weights = new HashMap<TerrData, Double>(map.size(), 1);
        final Comparator comp = new Comparator<TerrData>() {

            public int compare(TerrData o1, TerrData o2) {
                return (int) (weights.get(o1) - weights.get(o2));
            }
        };
        ArrayList<TerrData> Q = new ArrayList<TerrData>(map.size());
        for (TerrData tr : map) {
            weights.put(tr, Double.POSITIVE_INFINITY);
        }
        Q.addAll(map);
        TerrData initial = Q.get(0);
        weights.put(initial, 0d);
        while (!Q.isEmpty()) {
            Collections.sort(Q, comp);
            TerrData c = Q.remove(0);
            if (weights.get(c).isInfinite()) {
                flagErr(ERR.PATHING_ERROR, initial, c);
                return;
            }
            List<TerrData> borders = (LinkedList) c.borders.clone();
            borders.retainAll(Q);
            for (TerrData b : borders) {
                if (weights.get(c) + 1 < weights.get(b)) {
                    weights.put(b, weights.get(c) + 1);
                }
            }
        }
    }

    private void flagErr(ERR type, MapComponent... e) {
        switch(type) {
            case INVALID_ID:
                errors.add("ID is invalid: " + e[0].getID());
                break;
            case DUPLICATE_ID:
                errors.add("ID is used more than once: " + e[0].getID());
                break;
            case INVALID_NAME:
                errors.add("Name is invalid: " + e[0].getName());
                break;
            case DUPLICATE_NAME:
                errors.add("Name is used more than once: " + e[0].getName());
                break;
            case ORPHAN_TERRITORY:
                errors.add("Territory has no borders: " + e[0].getID());
                break;
            case PATHING_ERROR:
                errors.add("Map incomplete.");
                break;
        }
        mustAbort = mustAbort || type.mustAbort();
        for (MapComponent mc : e) {
            if (!status.errata.containsKey(mc)) {
                status.errata.put(mc, new HashSet<ERR>());
            }
            status.errata.get(mc).add(type);
        }
    }

    private boolean saveFile(File f, FileType ft) {
        checkValidity();
        checkPathing();
        if (!errors.isEmpty()) {
            DefaultListModel lm = new DefaultListModel();
            for (String s : errors) {
                lm.addElement(s);
            }
            JList lst = new JList(lm);
            lst.setVisibleRowCount(5);
            JScrollPane scr = new JScrollPane(lst);
            int ret;
            if (mustAbort) {
                JOptionPane.showMessageDialog(this, new Object[] { "The following errors are present in this map.", "Errors in component IDs prevent the map being saved.", scr }, "Invalid map!", JOptionPane.WARNING_MESSAGE);
                ret = JOptionPane.CANCEL_OPTION;
            } else {
                ret = JOptionPane.showOptionDialog(this, new Object[] { "The following errors are present in this map:", scr }, "Invalid map!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] { "Save Anyway", "Cancel" }, null);
            }
            if (ret != JOptionPane.OK_OPTION) {
                return false;
            }
        }
        try {
            if (!f.getName().endsWith(".xmap") && !f.getName().endsWith(".zmap")) {
                f = new File(f.getParentFile(), f.getName() + ft.ext[0]);
            }
            if (f.exists()) {
                if (JOptionPane.showConfirmDialog(this, "This file already exists. Overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            if (ft == FileType.ZMAP) {
                GZIPOutputStream zos = new GZIPOutputStream(fos);
                outputXMLData(zos);
                zos.close();
            } else {
                outputXMLData(fos);
            }
            fos.close();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(MapDesigner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(MapDesigner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(MapDesigner.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        mnuTerr = new javax.swing.JPopupMenu();
        mnuTerrRename = new javax.swing.JMenuItem();
        mnuTerrSetCont = new javax.swing.JMenuItem();
        mnuTerrLockSel = new javax.swing.JCheckBoxMenuItem();
        mnuCont = new javax.swing.JPopupMenu();
        mnuContNewCont = new javax.swing.JMenuItem();
        mnuContSetBonus = new javax.swing.JMenuItem();
        mnuContCxC = new javax.swing.JCheckBoxMenuItem();
        mnuContRemoveCont = new javax.swing.JMenuItem();
        sptMap = new javax.swing.JSplitPane();
        scrRenderer = new javax.swing.JScrollPane();
        pnlRenderer = new mapdesigner.MapRenderer();
        scrTree = new javax.swing.JScrollPane();
        pnlTree = new mapdesigner.TreePane();
        pnlZoom = new javax.swing.JPanel();
        cmdFit = new javax.swing.JButton();
        sldZoom = new javax.swing.JSlider();
        tbrToolbar = new javax.swing.JToolBar();
        cmdLockSel = new javax.swing.JToggleButton();
        cmdCxC = new javax.swing.JToggleButton();
        cmdNewCont = new javax.swing.JButton();
        mnuBar = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mnuFileNew = new javax.swing.JMenuItem();
        mnuFileOpen = new javax.swing.JMenuItem();
        mnuFileSave = new javax.swing.JMenuItem();
        mnuFileSaveAs = new javax.swing.JMenuItem();
        mnuFile_1 = new javax.swing.JSeparator();
        mnuFileExit = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mnuEditSetID = new javax.swing.JMenuItem();
        mnuEdit_1 = new javax.swing.JSeparator();
        mnuEditNewCont = new javax.swing.JMenuItem();
        mnuEditSetBonus = new javax.swing.JMenuItem();
        mnuEditCxC = new javax.swing.JCheckBoxMenuItem();
        mnuEditRemoveCont = new javax.swing.JMenuItem();
        mnuEdit_2 = new javax.swing.JSeparator();
        mnuEditRename = new javax.swing.JMenuItem();
        mnuEditSetCont = new javax.swing.JMenuItem();
        mnuEditLockSel = new javax.swing.JCheckBoxMenuItem();
        mnuTerrRename.setText("Rename");
        mnuTerrRename.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TerrRenameActionPerformed(evt);
            }
        });
        mnuTerr.add(mnuTerrRename);
        mnuTerrSetCont.setText("Set Continent");
        mnuTerrSetCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TerrSetContActionPerformed(evt);
            }
        });
        mnuTerr.add(mnuTerrSetCont);
        mnuTerrLockSel.setText("Lock Selection");
        mnuTerrLockSel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockSelActionPerformed(evt);
            }
        });
        mnuTerr.add(mnuTerrLockSel);
        mnuContNewCont.setText("New Continent");
        mnuContNewCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewContActionPerformed(evt);
            }
        });
        mnuCont.add(mnuContNewCont);
        mnuContSetBonus.setText("Set Bonus");
        mnuContSetBonus.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetBonus(evt);
            }
        });
        mnuCont.add(mnuContSetBonus);
        mnuContCxC.setSelected(true);
        mnuContCxC.setText("Colour by Continent");
        mnuContCxC.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CxCActionPerformed(evt);
            }
        });
        mnuCont.add(mnuContCxC);
        mnuContRemoveCont.setText("Remove Continent");
        mnuContRemoveCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveContActionPerformed(evt);
            }
        });
        mnuCont.add(mnuContRemoveCont);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());
        sptMap.setResizeWeight(1.0);
        scrRenderer.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrRenderer.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrRenderer.setPreferredSize(new java.awt.Dimension(500, 200));
        pnlRenderer.setLock(status);
        pnlRenderer.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                DisplayContextMenu(evt);
                MapSelectTerritory(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                DisplayContextMenu(evt);
            }
        });
        javax.swing.GroupLayout pnlRendererLayout = new javax.swing.GroupLayout(pnlRenderer);
        pnlRenderer.setLayout(pnlRendererLayout);
        pnlRendererLayout.setHorizontalGroup(pnlRendererLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 565, Short.MAX_VALUE));
        pnlRendererLayout.setVerticalGroup(pnlRendererLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 396, Short.MAX_VALUE));
        scrRenderer.setViewportView(pnlRenderer);
        sptMap.setLeftComponent(scrRenderer);
        scrTree.setPreferredSize(new java.awt.Dimension(150, 322));
        scrTree.addComponentListener(new java.awt.event.ComponentAdapter() {

            public void componentResized(java.awt.event.ComponentEvent evt) {
                scrTreeComponentResized(evt);
            }
        });
        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        pnlTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        pnlTree.setEditable(true);
        pnlTree.setRootVisible(false);
        pnlTree.setMapRenderer(pnlRenderer);
        pnlTree.setStatus(status);
        pnlTree.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                DisplayContextMenu(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                DisplayContextMenu(evt);
            }
        });
        pnlTree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {

            public void treeCollapsed(javax.swing.event.TreeExpansionEvent evt) {
                ResizeTreeComponents(evt);
            }

            public void treeExpanded(javax.swing.event.TreeExpansionEvent evt) {
                ResizeTreeComponents(evt);
            }
        });
        pnlTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {

            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                pnlTreeValueChanged(evt);
            }
        });
        pnlTree.setLayout(new javax.swing.BoxLayout(pnlTree, javax.swing.BoxLayout.PAGE_AXIS));
        scrTree.setViewportView(pnlTree);
        sptMap.setRightComponent(scrTree);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(sptMap, gridBagConstraints);
        pnlZoom.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        cmdFit.setText("Fit");
        cmdFit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdFitActionPerformed(evt);
            }
        });
        pnlZoom.add(cmdFit);
        sldZoom.setMajorTickSpacing(100);
        sldZoom.setMaximum(1000);
        sldZoom.setMinimum(-1000);
        sldZoom.setValue(0);
        sldZoom.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sldZoomStateChanged(evt);
            }
        });
        pnlZoom.add(sldZoom);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(pnlZoom, gridBagConstraints);
        tbrToolbar.setRollover(true);
        cmdLockSel.setText("Lock Selection");
        cmdLockSel.setFocusable(false);
        cmdLockSel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdLockSel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cmdLockSel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockSelActionPerformed(evt);
            }
        });
        tbrToolbar.add(cmdLockSel);
        cmdCxC.setText("Color by Continent");
        cmdCxC.setFocusable(false);
        cmdCxC.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdCxC.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cmdCxC.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CxCActionPerformed(evt);
            }
        });
        tbrToolbar.add(cmdCxC);
        cmdNewCont.setText("New Continent");
        cmdNewCont.setFocusable(false);
        cmdNewCont.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cmdNewCont.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cmdNewCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewContActionPerformed(evt);
            }
        });
        tbrToolbar.add(cmdNewCont);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(tbrToolbar, gridBagConstraints);
        mnuFile.setMnemonic('f');
        mnuFile.setText("File");
        mnuFileNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileNew.setMnemonic('N');
        mnuFileNew.setText("New");
        mnuFileNew.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileNewActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileNew);
        mnuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileOpen.setMnemonic('O');
        mnuFileOpen.setText("Open");
        mnuFileOpen.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileOpenActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileOpen);
        mnuFileSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSave.setMnemonic('S');
        mnuFileSave.setText("Save");
        mnuFileSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSave);
        mnuFileSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        mnuFileSaveAs.setMnemonic('A');
        mnuFileSaveAs.setText("Save As...");
        mnuFileSaveAs.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileSaveAsActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileSaveAs);
        mnuFile.add(mnuFile_1);
        mnuFileExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        mnuFileExit.setMnemonic('x');
        mnuFileExit.setText("Exit");
        mnuFileExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFileExitActionPerformed(evt);
            }
        });
        mnuFile.add(mnuFileExit);
        mnuBar.add(mnuFile);
        mnuEdit.setMnemonic('e');
        mnuEdit.setText("Edit");
        mnuEditSetID.setText("Set ID...");
        mnuEditSetID.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditSetIDActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditSetID);
        mnuEdit.add(mnuEdit_1);
        mnuEditNewCont.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditNewCont.setMnemonic('n');
        mnuEditNewCont.setText("New Continent...");
        mnuEditNewCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewContActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditNewCont);
        mnuEditSetBonus.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditSetBonus.setText("Set Bonus...");
        mnuEditSetBonus.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SetBonus(evt);
            }
        });
        mnuEdit.add(mnuEditSetBonus);
        mnuEditCxC.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, 0));
        mnuEditCxC.setText("Colour by Continent");
        mnuEditCxC.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CxCActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditCxC);
        mnuEditRemoveCont.setText("Remove Continent");
        mnuEditRemoveCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveContActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditRemoveCont);
        mnuEdit.add(mnuEdit_2);
        mnuEditRename.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mnuEditRename.setMnemonic('r');
        mnuEditRename.setText("Rename...");
        mnuEditRename.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TerrRenameActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditRename);
        mnuEditSetCont.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_MASK));
        mnuEditSetCont.setMnemonic('S');
        mnuEditSetCont.setText("Set Continent...");
        mnuEditSetCont.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TerrSetContActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditSetCont);
        mnuEditLockSel.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, 0));
        mnuEditLockSel.setMnemonic('L');
        mnuEditLockSel.setText("Lock Selection");
        mnuEditLockSel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LockSelActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditLockSel);
        mnuBar.add(mnuEdit);
        setJMenuBar(mnuBar);
        pack();
    }

    private void sldZoomStateChanged(javax.swing.event.ChangeEvent evt) {
        pnlRenderer.setZoom(Math.pow(10, sldZoom.getValue() / 1000d));
        pnlRenderer.repaint();
    }

    private void cmdFitActionPerformed(java.awt.event.ActionEvent evt) {
        pnlRenderer.ScaleAndCentre(scrRenderer.getVisibleRect());
        sldZoom.setValue((int) (1000 * Math.log10(pnlRenderer.getZoom())));
    }

    private void pnlTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {
        TreePath sel = evt.getPath();
        if (sel.getLastPathComponent() instanceof TerrData) {
            pnlRenderer.setSelected((TerrData) sel.getLastPathComponent());
        } else {
            pnlRenderer.setSelected(null);
        }
        pnlRenderer.repaint();
    }

    private void DisplayContextMenu(java.awt.event.MouseEvent evt) {
        if (!evt.isPopupTrigger()) {
            return;
        }
        if (pnlTree.getSelectionPath() == null) {
            mnuTerrSetCont.setEnabled(false);
            mnuTerrRename.setEnabled(false);
            mnuTerrLockSel.setEnabled(false);
        } else {
            mnuTerrSetCont.setEnabled(true);
            mnuTerrRename.setEnabled(true);
            mnuTerrLockSel.setEnabled(true);
        }
        if (pnlTree.getSelectionPath().getLastPathComponent() instanceof TerrData) {
            mnuTerr.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
        if (pnlTree.getSelectionPath().getLastPathComponent() instanceof ContData) {
            mnuCont.show((Component) evt.getSource(), evt.getX(), evt.getY());
        }
    }

    private void MapSelectTerritory(java.awt.event.MouseEvent evt) {
        if (status.lockSelection) {
            return;
        }
        LinkedList<ContData> lst = pnlRenderer.getList();
        Point p = evt.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(p, pnlRenderer);
        pnlRenderer.convertPoint(p);
        for (ContData c : lst) {
            for (TerrData td : c.getChildren()) {
                if (td.contains(p)) {
                    pnlTree.setSelectionPath(new TreePath(new Object[] { root, c, td }));
                    pnlTree.treeDidChange();
                    pnlTree.repaint();
                }
            }
        }
    }

    private void TerrRenameActionPerformed(java.awt.event.ActionEvent evt) {
        renameTerritory();
    }

    private void scrTreeComponentResized(java.awt.event.ComponentEvent evt) {
        pnlTree.setSize(scrTree.getViewport().getSize().width, pnlTree.getSize().height);
        repaintTree();
    }

    private void TerrSetContActionPerformed(java.awt.event.ActionEvent evt) {
        ContData cCont = null;
        TerrData t = (TerrData) pnlTree.getSelectionPath().getLastPathComponent();
        for (ContData contData : pnlRenderer.getList()) {
            if (contData.getIndex(t) != -1) {
                cCont = contData;
                break;
            }
        }
        ContData newCont = (ContData) JOptionPane.showInputDialog(this, "Select continent to move to:", "Select Continent", JOptionPane.PLAIN_MESSAGE, null, pnlRenderer.getList().toArray(new ContData[0]), cCont);
        if (newCont == null) {
            return;
        }
        cCont.remove(t);
        newCont.add(t);
        Enumeration<TreePath> e = pnlTree.getExpandedDescendants(new TreePath(root));
        refreshTreeData();
        for (TreePath tp : Collections.list(e)) {
            pnlTree.expandPath(tp);
        }
        pnlTree.setSelectionPath(new TreePath(new Object[] { root, newCont, t }));
    }

    private void LockSelActionPerformed(java.awt.event.ActionEvent evt) {
        toggleLock();
    }

    private void NewContActionPerformed(java.awt.event.ActionEvent evt) {
        createContinent();
    }

    private void SetBonus(java.awt.event.ActionEvent evt) {
        ContData sel = (ContData) pnlTree.getSelectionPath().getLastPathComponent();
        if (sel.getName() == null) {
            JOptionPane.showMessageDialog(this, "You cannot set the bonus for the default continent.", "Unnamed Continent", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SpinnerModel spnMdl = new SpinnerNumberModel(sel.getBonus(), 0, null, 1);
        JPanel ctr = new JPanel(new FlowLayout());
        JSpinner spnBonus = new JSpinner(spnMdl);
        ctr.add(spnBonus);
        JOptionPane.showMessageDialog(null, new Object[] { "Please enter the bonus for: " + sel.getName(), ctr }, "Enter Bonus.", JOptionPane.PLAIN_MESSAGE);
        sel.setBonus((Integer) spnBonus.getValue());
        repaintTree();
    }

    private void CxCActionPerformed(java.awt.event.ActionEvent evt) {
        toggleCxC();
    }

    private void ResizeTreeComponents(javax.swing.event.TreeExpansionEvent evt) {
        pnlTree.validate();
    }

    private void SaveActionPerformed(java.awt.event.ActionEvent evt) {
        if (mapFile == null) {
            mnuFileSaveAsActionPerformed(evt);
        } else {
            String ext = mapFile.getName().substring(mapFile.getName().lastIndexOf(".") + 1);
            if (ext.equals("svg")) {
                saveFile(mapFile, FileType.SVG);
            } else if (ext.equals("xmap")) {
                saveFile(mapFile, FileType.XMAP);
            } else if (ext.equals("zmap")) {
                saveFile(mapFile, FileType.ZMAP);
            }
        }
    }

    private void mnuFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(FileType.XMAP.ff());
        fc.setFileFilter(FileType.ZMAP.ff());
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (saveFile(fc.getSelectedFile(), ((FileType.FileFilter) fc.getFileFilter()).getType())) {
                mapFile = fc.getSelectedFile();
            }
        }
    }

    private void mnuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(FileType.SVG.ff());
        fc.addChoosableFileFilter(FileType.XMAP.ff());
        fc.addChoosableFileFilter(FileType.ZMAP.ff());
        fc.setFileFilter(FileType.MAP.ff());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String name = fc.getSelectedFile().getName();
            String ext = name.substring(name.lastIndexOf(".") + 1);
            try {
                if (ext.equals("svg")) {
                    loadSVG(fc.getSelectedFile());
                } else if (ext.equals("xmap")) {
                    loadXMap(fc.getSelectedFile());
                } else if (ext.equals("zmap")) {
                    loadZMap(fc.getSelectedFile());
                } else {
                    ButtonGroup bg = new ButtonGroup();
                    JRadioButton[] rad = { new JRadioButton("Open as SVG"), new JRadioButton("Open as map"), new JRadioButton("Open as compressed map") };
                    bg.add(rad[0]);
                    bg.add(rad[1]);
                    bg.add(rad[2]);
                    if (JOptionPane.showOptionDialog(this, rad, "Unknown file type", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] { "Open", "Cancel" }, mnuFile) == JOptionPane.OK_OPTION) {
                        if (rad[0].isSelected()) {
                            loadSVG(fc.getSelectedFile());
                        } else if (rad[0].isSelected()) {
                            loadXMap(fc.getSelectedFile());
                        } else {
                            loadZMap(fc.getSelectedFile());
                        }
                    } else {
                        return;
                    }
                }
                mapFile = fc.getSelectedFile();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void mnuFileNewActionPerformed(java.awt.event.ActionEvent evt) {
        fc.resetChoosableFileFilters();
        fc.setAcceptAllFileFilterUsed(true);
        fc.addChoosableFileFilter(FileType.SVG.ff());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                loadSVG(fc.getSelectedFile());
                String nName = fc.getSelectedFile().getName();
                if (nName.lastIndexOf(".") != -1) {
                    nName = nName.subSequence(0, nName.lastIndexOf(".")) + ".xmap";
                }
                mapFile = new File(fc.getSelectedFile().getParentFile(), nName);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void mnuFileExitActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {
        int ret = JOptionPane.showConfirmDialog(this, "Closing will cause all unsaved data to be lost. Do you wish to save before exiting?", "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        switch(ret) {
            case JOptionPane.YES_OPTION:
                SaveActionPerformed(null);
                break;
            case JOptionPane.NO_OPTION:
                break;
            case JOptionPane.CANCEL_OPTION:
                return;
        }
        this.dispose();
    }

    private void mnuEditSetIDActionPerformed(java.awt.event.ActionEvent evt) {
        MapComponent sel = (MapComponent) pnlTree.getSelectionPath().getLastPathComponent();
        if (sel == null) {
            return;
        }
        String newID = JOptionPane.showInputDialog(this, "Enter new ID:", sel.getID());
        if (sel instanceof TerrData) {
            TerrData t = new TerrData(newID, ((TerrData) sel).GDATA);
            t.setName(sel.getName());
            ((ContData) pnlTree.getSelectionPath().getParentPath().getLastPathComponent()).remove((TerrData) sel);
            ((ContData) pnlTree.getSelectionPath().getParentPath().getLastPathComponent()).add(t);
        } else if (sel instanceof ContData) {
            ContData c = new ContData(newID, sel.getName());
            for (TerrData td : ((ContData) sel).ters) {
                c.add(td);
            }
            pnlRenderer.getList().remove((ContData) sel);
            pnlRenderer.getList().add(c);
        }
        refreshTreeData();
    }

    private void RemoveContActionPerformed(java.awt.event.ActionEvent evt) {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you wish to remove this continent?", "Are you sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (pnlTree.getSelectionPath().getLastPathComponent() instanceof ContData) {
                ContData r = (ContData) pnlTree.getSelectionPath().getLastPathComponent();
                for (TerrData t : r.ters) {
                    ContData.NullCont.add(t);
                }
                pnlRenderer.getList().remove(r);
            }
            refreshTreeData();
        }
    }

    /**
	 * @param args the command line arguments
	 */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MapDesigner().setVisible(true);
            }
        });
    }

    private javax.swing.JToggleButton cmdCxC;

    private javax.swing.JButton cmdFit;

    private javax.swing.JToggleButton cmdLockSel;

    private javax.swing.JButton cmdNewCont;

    private javax.swing.JMenuBar mnuBar;

    private javax.swing.JPopupMenu mnuCont;

    private javax.swing.JCheckBoxMenuItem mnuContCxC;

    private javax.swing.JMenuItem mnuContNewCont;

    private javax.swing.JMenuItem mnuContRemoveCont;

    private javax.swing.JMenuItem mnuContSetBonus;

    private javax.swing.JMenu mnuEdit;

    private javax.swing.JCheckBoxMenuItem mnuEditCxC;

    private javax.swing.JCheckBoxMenuItem mnuEditLockSel;

    private javax.swing.JMenuItem mnuEditNewCont;

    private javax.swing.JMenuItem mnuEditRemoveCont;

    private javax.swing.JMenuItem mnuEditRename;

    private javax.swing.JMenuItem mnuEditSetBonus;

    private javax.swing.JMenuItem mnuEditSetCont;

    private javax.swing.JMenuItem mnuEditSetID;

    private javax.swing.JSeparator mnuEdit_1;

    private javax.swing.JSeparator mnuEdit_2;

    private javax.swing.JMenu mnuFile;

    private javax.swing.JMenuItem mnuFileExit;

    private javax.swing.JMenuItem mnuFileNew;

    private javax.swing.JMenuItem mnuFileOpen;

    private javax.swing.JMenuItem mnuFileSave;

    private javax.swing.JMenuItem mnuFileSaveAs;

    private javax.swing.JSeparator mnuFile_1;

    private javax.swing.JPopupMenu mnuTerr;

    private javax.swing.JCheckBoxMenuItem mnuTerrLockSel;

    private javax.swing.JMenuItem mnuTerrRename;

    private javax.swing.JMenuItem mnuTerrSetCont;

    private mapdesigner.MapRenderer pnlRenderer;

    private mapdesigner.TreePane pnlTree;

    private javax.swing.JPanel pnlZoom;

    private javax.swing.JScrollPane scrRenderer;

    private javax.swing.JScrollPane scrTree;

    private javax.swing.JSlider sldZoom;

    private javax.swing.JSplitPane sptMap;

    private javax.swing.JToolBar tbrToolbar;
}

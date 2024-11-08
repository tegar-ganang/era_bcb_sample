package gov.sns.apps.mpx;

import gov.sns.tools.xml.XmlDataAdaptor;
import gov.sns.tools.apputils.files.*;
import gov.sns.tools.xml.viewer.ui.DefaultImages;
import gov.sns.xal.model.ModelException;
import gov.sns.xal.model.mpx.ModelProxy;
import gov.sns.xal.model.mpx.ModelProxyListenerAdaptor;
import gov.sns.xal.slg.LatticeError;
import gov.sns.xal.smf.application.AcceleratorWindow;
import gov.sns.xal.smf.impl.Magnet;
import gov.sns.xal.smf.impl.RfCavity;
import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.w3c.dom.Document;

/**
 * The MainWindow object of the MPXMain Application.
 * 
 * @author wdklotz
 * @version $Id: MPXMainWindow.java,v 1.28 2005/03/17 19:37:10 paul Exp
 *          $MPXMainWindow.java
 */
public class MPXMainWindow extends AcceleratorWindow {

    static final String dataLabel = "MPXMainWindow";

    private MPXBanner banner;

    private MPXDOMTree accelTree;

    private MPXDOMTree sequenceTree;

    private MPXDOMTree latticeTree;

    private MPXDOMTree probeTree;

    private MPXDOMTree trajectoryTree;

    private ImageIcon openFolder;

    private ImageIcon closedFolder;

    private ImageIcon leafImage;

    private JTabbedPane tabbedPane;

    private Map tabMap;

    private JTable phaseVectorTable;

    private JTable twissFunctionTable;

    private Document emptyTreeDocument;

    MPXDocument mpxDocument;

    private MPXStatePerElementView mpxSPEView;

    protected MPXTwissPlot plotPane;

    private MPXAttrPerNodeView nodeSPEView;

    MPXProxy mProxy;

    private RecentFileTracker _probeFileTracker;

    /** Creates a new instance of MainWindow */
    MPXMainWindow(MPXDocument document) {
        super(document);
        mpxDocument = document;
        setSize(1000, 600);
        _probeFileTracker = new RecentFileTracker(1, this.getClass(), "recent_probes");
        banner = new MPXBanner(mpxDocument);
        openFolder = new ImageIcon(DefaultImages.createOpenFolderImage());
        closedFolder = new ImageIcon(DefaultImages.createClosedFolderImage());
        leafImage = new ImageIcon(DefaultImages.createLeafImage());
        tabMap = new HashMap();
        emptyTreeDocument = XmlDataAdaptor.newEmptyDocumentAdaptor("NULL", null).document();
        makeContent();
        mProxy = mpxDocument.getModelProxy();
        mProxy.addModelProxyListener(new ModelProxyListenerAdaptor() {

            @Override
            public void accelMasterChanged(ModelProxy mp) {
                clearTrajectoryTreee();
                clearPhaseVectorTable();
                clearTwissFunctionTable();
                clearStatePerElementTable();
                clearPlotCheckboxes();
                clearTwissPlot();
                selectTab("Accel");
            }

            @Override
            public void accelSequenceChanged(ModelProxy mp) {
                clearTrajectoryTreee();
                clearPhaseVectorTable();
                clearTwissFunctionTable();
                clearStatePerElementTable();
                clearPlotCheckboxes();
                clearTwissPlot();
                selectTab("Lattice");
                plotPane.setAcceleratorSequence(mp.getAcceleratorSequence());
            }

            @Override
            public void probeMasterChanged(ModelProxy mp) {
                clearTrajectoryTreee();
                clearPhaseVectorTable();
                clearTwissFunctionTable();
                clearStatePerElementTable();
                clearPlotCheckboxes();
                plotPane.setPlotCheckbox();
                selectTab("Probe");
            }

            @Override
            public void modelResultsChanged(ModelProxy mp) {
                sequenceTree.getSelectionModel().clearSelection();
                clearStatePerElementTable();
                int type = mp.getProbeType();
                if (type == ModelProxy.ENVELOPE_PROBE) {
                    selectTab("Sequence");
                } else if (type == ModelProxy.PARTICLE_PROBE) {
                    selectTab("Sequence");
                } else {
                    selectTab("Trajectory");
                }
            }
        });
        sequenceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        sequenceTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                int pathCount = sequenceTree.getSelectionModel().getSelectionCount();
                if (pathCount == 0) {
                    return;
                }
                TreePath[] paths = sequenceTree.getSelectionModel().getSelectionPaths();
                handleSelectOnSequenceTree(paths);
            }
        });
    }

    /**
	 * Create the GUI
	 */
    protected void makeContent() {
        accelTree = new MPXDOMTree();
        sequenceTree = new MPXDOMTree();
        latticeTree = new MPXDOMTree();
        probeTree = new MPXDOMTree();
        trajectoryTree = new MPXDOMTree();
        clearAcceleratorTree();
        clearSequenceTree();
        clearLatticeTree();
        clearProbeTree();
        clearTrajectoryTreee();
        phaseVectorTable = new JTable();
        twissFunctionTable = new JTable();
        clearPhaseVectorTable();
        clearTwissFunctionTable();
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(leafImage);
        renderer.setClosedIcon(closedFolder);
        renderer.setOpenIcon(openFolder);
        accelTree.setCellRenderer(renderer);
        sequenceTree.setCellRenderer(renderer);
        latticeTree.setCellRenderer(renderer);
        probeTree.setCellRenderer(renderer);
        trajectoryTree.setCellRenderer(renderer);
        plotPane = new MPXTwissPlot(this);
        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(accelTree);
        JScrollPane jScrollPane2 = new JScrollPane();
        jScrollPane2.setViewportView(latticeTree);
        JScrollPane jScrollPane3 = new JScrollPane();
        jScrollPane3.setViewportView(probeTree);
        JScrollPane jScrollPane4 = new JScrollPane();
        jScrollPane4.setViewportView(trajectoryTree);
        JScrollPane jScrollPane5 = new JScrollPane();
        jScrollPane5.setViewportView(sequenceTree);
        jScrollPane5.setPreferredSize(new Dimension(400, 100));
        JScrollPane tablePane1 = new JScrollPane();
        tablePane1.setViewportView(phaseVectorTable);
        JScrollPane tablePane2 = new JScrollPane();
        tablePane2.setViewportView(twissFunctionTable);
        mpxSPEView = new MPXStatePerElementView();
        nodeSPEView = new MPXAttrPerNodeView();
        JSplitPane subSeqSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        subSeqSplitPane.setTopComponent(nodeSPEView);
        subSeqSplitPane.setBottomComponent(mpxSPEView);
        subSeqSplitPane.setDividerLocation(200);
        JSplitPane sequenceSplitPane = new JSplitPane();
        sequenceSplitPane.setLeftComponent(jScrollPane5);
        sequenceSplitPane.setRightComponent(subSeqSplitPane);
        sequenceSplitPane.setDividerLocation(450);
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.addTab("Accel", jScrollPane1);
        tabbedPane.addTab("Sequence", sequenceSplitPane);
        tabbedPane.addTab("Lattice", jScrollPane2);
        tabbedPane.addTab("Probe", jScrollPane3);
        tabbedPane.addTab("Trajectory", jScrollPane4);
        tabbedPane.addTab("Result Table 1", tablePane1);
        tabbedPane.addTab("Result Table 2", tablePane2);
        tabbedPane.addTab("Plots", plotPane);
        tabbedPane.setEnabledAt(2, false);
        tabMap.put("Accel", new Integer(tabbedPane.indexOfComponent(jScrollPane1)));
        tabMap.put("Lattice", new Integer(tabbedPane.indexOfComponent(jScrollPane2)));
        tabMap.put("Probe", new Integer(tabbedPane.indexOfComponent(jScrollPane3)));
        tabMap.put("Trajectory", new Integer(tabbedPane.indexOfComponent(jScrollPane4)));
        tabMap.put("Sequence", new Integer(tabbedPane.indexOfComponent(sequenceSplitPane)));
        tabMap.put("PhaseSpace", new Integer(tabbedPane.indexOfComponent(tablePane1)));
        tabMap.put("Twiss", new Integer(tabbedPane.indexOfComponent(tablePane2)));
        tabMap.put("Twiss Plot", new Integer(tabbedPane.indexOfComponent(plotPane)));
        getContentPane().add(tabbedPane);
        getContentPane().add(banner, "South");
    }

    protected MPXBanner getTextField() {
        return banner;
    }

    private void selectTab(String tab) {
        tabbedPane.setSelectedIndex(((Integer) tabMap.get(tab)).intValue());
    }

    protected JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
	 * Handle the "select-sequence" and "select-probe" action by opening a new
	 * file chooser.
	 */
    File openMasterFileChooser(File preset, String currentDirectory) {
        String[] fileTypes = { "probe" };
        if (currentDirectory == null) {
            currentDirectory = _probeFileTracker.getRecentFolderPath();
        }
        JFileChooser masterChooser = MPXMasterFileChooserFactory.getFileChooser(fileTypes, currentDirectory);
        File selection = null;
        masterChooser.setSelectedFile(preset);
        int status = masterChooser.showOpenDialog(this);
        switch(status) {
            case JFileChooser.CANCEL_OPTION:
                break;
            case JFileChooser.APPROVE_OPTION:
                selection = masterChooser.getSelectedFile();
                _probeFileTracker.cacheURL(selection);
                break;
            case JFileChooser.ERROR_OPTION:
                break;
        }
        return selection;
    }

    File openMasterFileChooser(File preset) {
        return openMasterFileChooser(preset, null);
    }

    private void handleSelectOnSequenceTree(TreePath[] paths) {
        if (paths.length == 1) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            String nodeInfo = node.getUserObject().toString();
            if (elementIdToken(nodeInfo) == null) {
                clearStatePerElementTable();
                return;
            }
        }
        Vector elementIds = new Vector();
        for (int i = 0; i < paths.length; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
            String nodeInfo = node.getUserObject().toString();
            String elementId = elementIdToken(nodeInfo);
            if (elementId != null) {
                elementIds.add(elementId);
            }
        }
        if (elementIds.size() != 0) {
            String[] ids = new String[elementIds.size()];
            elementIds.toArray(ids);
            clearStatePerElementTable();
            makeStatePerElementTableFor(ids);
        } else {
            clearStatePerElementTable();
        }
    }

    private String elementIdToken(String nodeInfo) {
        String elementId = null;
        StringTokenizer strtok = new StringTokenizer(nodeInfo, "\"");
        String tagWord = strtok.nextToken();
        String prefix = "<node";
        if (tagWord.startsWith(prefix) || tagWord.startsWith("<sequence")) {
            elementId = strtok.nextToken();
        }
        return elementId;
    }

    private void makeStatePerElementTableFor(String[] elementIds) {
        for (int i = 0; i < elementIds.length; i++) {
            System.out.println(elementIds[i]);
            try {
                if (!mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("RfCavity") && !mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("Bnch") && !mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("DTLTank") && !mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("CCL") && !mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("SCLCavity")) addStatePerElementTableFor(elementIds[i]);
                if (mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("QH") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("QV") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("DCH") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("DCV") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("QTH") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("QTV") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("QSC") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("DH")) {
                    Magnet magnetNode = (Magnet) (mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]));
                    addAttrPerNodeTableFor(magnetNode);
                } else if (mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("RfCavity") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("Bnch") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("DTLTank") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("CCL") || mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]).getType().equals("SCLCavity")) {
                    RfCavity rfCav = (RfCavity) (mpxDocument.getSelectedSequence().getNodeWithId(elementIds[i]));
                    addAttrPerNodeTableFor(rfCav);
                }
            } catch (NullPointerException e) {
                System.out.println("Nothing can be set for " + elementIds[i]);
            }
        }
    }

    private void addStatePerElementTableFor(String id) {
        MPXStatePerElementTable model;
        try {
            model = mpxDocument.getModelProxy().getStatePerElementTable(id);
            mpxSPEView.add(model);
        } catch (ModelException e) {
        } catch (LatticeError e) {
            System.out.println(e.getMessage());
        }
    }

    private void addAttrPerNodeTableFor(Magnet node) {
        MPXAttrPerNodeTable nodeAttrTable;
        nodeAttrTable = new MPXAttrPerNodeTable(mProxy.getScenario(), node, mpxDocument.getModelProxy().getChannelSource());
        nodeSPEView.add(nodeAttrTable);
    }

    private void addAttrPerNodeTableFor(RfCavity node) {
        MPXAttrPerNodeTable nodeAttrTable;
        nodeAttrTable = new MPXAttrPerNodeTable(mProxy.getScenario(), node, mpxDocument.getModelProxy().getChannelSource());
        nodeSPEView.add(nodeAttrTable);
    }

    void setAcceleratorTree(Document doc) {
        accelTree.setDocument(doc);
    }

    void clearAcceleratorTree() {
        setAcceleratorTree(emptyTreeDocument);
    }

    void setSequenceTree(Document doc) {
        sequenceTree.setDocument(doc);
    }

    void clearSequenceTree() {
        setSequenceTree(emptyTreeDocument);
    }

    void setLatticeTree(Document doc) {
        latticeTree.setDocument(doc);
    }

    void clearLatticeTree() {
        setLatticeTree(emptyTreeDocument);
    }

    void setProbeTree(Document doc) {
        probeTree.setDocument(doc);
    }

    void clearProbeTree() {
        setProbeTree(emptyTreeDocument);
    }

    void setTrajectoryTree(Document doc) {
        trajectoryTree.setDocument(doc);
    }

    void clearTrajectoryTreee() {
        setTrajectoryTree(emptyTreeDocument);
    }

    void setPhaseVectorTable(TableModel model) {
        phaseVectorTable.setModel(model);
    }

    void clearPhaseVectorTable() {
        setPhaseVectorTable(new DefaultTableModel());
    }

    void setTwissFunctionTable(TableModel model) {
        twissFunctionTable.setModel(model);
    }

    void clearTwissFunctionTable() {
        setTwissFunctionTable(new DefaultTableModel());
    }

    void setTwissPlot(MPXProxy mp) {
        plotPane.updatePlotData(mp);
    }

    void clearTwissPlot() {
        plotPane.reset();
    }

    void resetPlotData() {
        plotPane.resetData();
    }

    void clearPlotCheckboxes() {
        plotPane.clearCheckboxes();
    }

    void clearStatePerElementTable() {
        mpxSPEView.clear();
        nodeSPEView.clear();
    }
}

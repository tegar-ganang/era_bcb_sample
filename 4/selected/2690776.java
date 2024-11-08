package fca.gui.lattice;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import fca.core.context.binary.BinaryContext;
import fca.core.lattice.ConceptLattice;
import fca.core.lattice.NestedLattice;
import fca.core.rule.InformativeBasisAlgorithm;
import fca.core.rule.Rule;
import fca.core.rule.RuleAlgorithm;
import fca.exception.AlreadyExistsException;
import fca.exception.InvalidTypeException;
import fca.exception.LatticeMinerException;
import fca.exception.WriterException;
import fca.gui.Viewer;
import fca.gui.lattice.element.GraphicalConcept;
import fca.gui.lattice.element.GraphicalLattice;
import fca.gui.lattice.element.LatticeStructure;
import fca.gui.lattice.tool.Projection;
import fca.gui.lattice.tool.Search;
import fca.gui.lattice.tool.SearchNested;
import fca.gui.lattice.tool.SearchSimple;
import fca.gui.lattice.tool.Tree;
import fca.gui.rule.RuleViewer;
import fca.gui.util.ColorSet;
import fca.gui.util.DialogBox;
import fca.gui.util.ExampleFileFilter;
import fca.gui.util.ScreenImage;
import fca.gui.util.constant.LMHistory;
import fca.gui.util.constant.LMIcons;
import fca.gui.util.constant.LMOptions;
import fca.gui.util.constant.LMPreferences;
import fca.io.lattice.writer.xml.GaliciaXMLLatticeWriter;
import fca.messages.GUIMessages;

/**
 * Fen�tre affichant un treillis (imbriqu� ou non) avec sa structure arborescente et son panneau de
 * contr�le
 * @author Genevi�ve Roberge
 * @version 1.0
 */
public class LatticeViewer extends Viewer {

    /**
	 * 
	 */
    private static final long serialVersionUID = 6042867291900333577L;

    LatticeStructureFrame structureFrame;

    LatticePanel viewer;

    JButton nextBtn;

    JButton previousBtn;

    JButton zoomInBtn;

    JButton zoomOutBtn;

    JButton zoomAreaBtn;

    JButton noZoomBtn;

    JButton modifyBtn;

    JButton duplicateBtn;

    JButton ruleBtn;

    JButton captureBtn;

    Tree treePanel;

    Projection projectionPanel;

    Search searchPanel;

    JTabbedPane toolPane;

    GraphicalLattice glCurrent;

    JLabel textSizeLabel;

    JSlider textSize;

    JLabel textSizeValue;

    private FrameMenu frameMenu = null;

    /**
	 * Constructeur
	 * @param gl Le NestedGraphLattice qui doit �tre affich�
	 */
    public LatticeViewer(GraphicalLattice gl) {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        glCurrent = gl;
        structureFrame = null;
        setTitle(GUIMessages.getString("GUI.lattice") + " : " + gl.getName());
        frameMenu = new FrameMenu();
        setJMenuBar(frameMenu.getMenuBar());
        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().add(buildToolBar(), BorderLayout.NORTH);
        getContentPane().add(buildPanel(gl), BorderLayout.CENTER);
        viewer.lockHistory();
        frameMenu.setPresetOptions(LMOptions.USER);
        viewer.unlockHistory();
        addWindowListener(this);
        addComponentListener(new ComponentListener() {

            public void componentHidden(ComponentEvent e) {
            }

            public void componentMoved(ComponentEvent e) {
            }

            public void componentResized(ComponentEvent e) {
                viewer.lockHistory();
                showEntireLattice();
                viewer.unlockHistory();
            }

            public void componentShown(ComponentEvent e) {
            }
        });
    }

    private JToolBar buildToolBar() {
        JToolBar toolBar = new JToolBar(GUIMessages.getString("GUI.quickTools"));
        ToolBarListener listener = new ToolBarListener();
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        previousBtn = new JButton(LMIcons.getUndo());
        previousBtn.addActionListener(listener);
        previousBtn.setToolTipText(GUIMessages.getString("GUI.back"));
        previousBtn.setEnabled(false);
        previousBtn.setMnemonic(KeyEvent.VK_Z);
        toolBar.add(previousBtn);
        nextBtn = new JButton(LMIcons.getRedo());
        nextBtn.addActionListener(listener);
        nextBtn.setToolTipText(GUIMessages.getString("GUI.forward"));
        nextBtn.setEnabled(false);
        nextBtn.setMnemonic(KeyEvent.VK_Y);
        toolBar.add(nextBtn);
        toolBar.addSeparator();
        zoomInBtn = new JButton(LMIcons.getZoomIn());
        zoomInBtn.addActionListener(listener);
        zoomInBtn.setToolTipText(GUIMessages.getString("GUI.zoomIn"));
        zoomInBtn.setMnemonic(KeyEvent.VK_ADD);
        toolBar.add(zoomInBtn);
        zoomOutBtn = new JButton(LMIcons.getZoomOut());
        zoomOutBtn.addActionListener(listener);
        zoomOutBtn.setToolTipText(GUIMessages.getString("GUI.zoomOut"));
        zoomOutBtn.setMnemonic(KeyEvent.VK_SUBTRACT);
        toolBar.add(zoomOutBtn);
        zoomAreaBtn = new JButton(LMIcons.getFitZoomToSelection());
        zoomAreaBtn.addActionListener(listener);
        zoomAreaBtn.setToolTipText(GUIMessages.getString("GUI.zoomInSelectedArea"));
        zoomAreaBtn.setMnemonic(KeyEvent.VK_S);
        toolBar.add(zoomAreaBtn);
        noZoomBtn = new JButton(LMIcons.getNoZoom());
        noZoomBtn.addActionListener(listener);
        noZoomBtn.setToolTipText(GUIMessages.getString("GUI.showAllEntireLattice"));
        noZoomBtn.setMnemonic(KeyEvent.VK_A);
        toolBar.add(noZoomBtn);
        toolBar.addSeparator();
        modifyBtn = new JButton(LMIcons.getModifyTools());
        modifyBtn.addActionListener(listener);
        modifyBtn.setToolTipText(GUIMessages.getString("GUI.modifyLatticeStructure"));
        modifyBtn.setMnemonic(KeyEvent.VK_M);
        toolBar.add(modifyBtn);
        ruleBtn = new JButton(LMIcons.getShowRulesBig());
        ruleBtn.addActionListener(listener);
        ruleBtn.setToolTipText(GUIMessages.getString("GUI.showRules"));
        ruleBtn.setMnemonic(KeyEvent.VK_R);
        toolBar.add(ruleBtn);
        toolBar.addSeparator();
        captureBtn = new JButton(LMIcons.getPrintScreen());
        captureBtn.addActionListener(listener);
        captureBtn.setToolTipText(GUIMessages.getString("GUI.exportLatticeAsImage"));
        captureBtn.setMnemonic(KeyEvent.VK_E);
        toolBar.add(captureBtn);
        toolBar.addSeparator();
        textSizeLabel = new JLabel();
        textSizeLabel.setText("Label Size");
        toolBar.add(textSizeLabel);
        textSize = new JSlider(6, 36, 12);
        textSize.addChangeListener(new SliderListener());
        toolBar.add(textSize);
        textSizeValue = new JLabel();
        textSizeValue.setText("" + textSize.getValue());
        toolBar.add(textSizeValue);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.addSeparator();
        duplicateBtn = new JButton(LMIcons.getDuplicate());
        duplicateBtn.addActionListener(listener);
        duplicateBtn.setToolTipText(GUIMessages.getString("GUI.duplicate"));
        duplicateBtn.setMnemonic(KeyEvent.VK_D);
        toolBar.add(duplicateBtn);
        return toolBar;
    }

    private JPanel buildPanel(GraphicalLattice gl) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        JLabel currentLatticeName = new JLabel(GUIMessages.getString("GUI.lattice") + " : " + gl.getName());
        viewer = new LatticePanel(gl, this);
        ScrollPane viewerScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
        viewerScrollPane.add(viewer);
        viewerScrollPane.setMinimumSize(new Dimension(0, 50));
        viewerScrollPane.setPreferredSize(new Dimension(575, 575));
        treePanel = new Tree(gl, viewer);
        ScrollPane treeScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        treeScrollPane.setBackground(Color.WHITE);
        treeScrollPane.add(treePanel);
        treeScrollPane.setMinimumSize(new Dimension(150, 50));
        treePanel.getTree().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("leadSelectionPath")) {
                    GraphicalConcept selectedNode = treePanel.getSelectedNode();
                    if (!treePanel.isGlobalLatticeSelected()) {
                        viewer.selectAndShakeNode(selectedNode);
                        viewer.repaint();
                    }
                } else if (e.getPropertyName().equals("model")) {
                    viewer.lockHistory();
                    viewer.zoomInArea(viewer.getBoundsForLattice(viewer.getRootLattice()));
                    if (viewer.getRootLattice().isEditable()) viewer.getRootLattice().clearLattice();
                    viewer.repaint();
                    viewer.unlockHistory();
                }
            }
        });
        projectionPanel = new Projection(viewer.getRootLattice(), viewer);
        ScrollPane projectionScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        projectionScrollPane.setBackground(Color.WHITE);
        projectionScrollPane.add(projectionPanel);
        projectionScrollPane.setMinimumSize(new Dimension(0, 50));
        toolPane = new JTabbedPane();
        toolPane.addTab(GUIMessages.getString("GUI.treeViewPanel"), treeScrollPane);
        toolPane.addTab(GUIMessages.getString("GUI.projectSelectPanel"), projectionScrollPane);
        if (!glCurrent.isNested()) searchPanel = new SearchSimple(viewer.getRootLattice(), viewer); else searchPanel = new SearchNested(viewer.getRootLattice(), viewer);
        ScrollPane searchScrollPane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        searchScrollPane.setBackground(Color.WHITE);
        searchScrollPane.add(searchPanel);
        searchScrollPane.setMinimumSize(new Dimension(0, 50));
        toolPane.addTab(GUIMessages.getString("GUI.searchApproximatePanel"), searchScrollPane);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, toolPane, viewerScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(210);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(currentLatticeName, constraints);
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(splitPane, constraints);
        return panel;
    }

    /**
	 * Construit une fenêtre qui permettra de modifier la structure des treillis
	 */
    private void openStructureViewer() throws AlreadyExistsException, InvalidTypeException {
        Vector<ConceptLattice> conceptLattices = new Vector<ConceptLattice>();
        conceptLattices.add(viewer.getRootLattice().getNestedLattice().getConceptLattice());
        conceptLattices.addAll(viewer.getRootLattice().getNestedLattice().getInternalLattices());
        Vector<LatticeStructure> structures = new Vector<LatticeStructure>();
        structures.add(viewer.getRootLattice().getLatticeStructure());
        structures.addAll(viewer.getRootLattice().getInternalLatticeStructures());
        int conceptSizeType;
        if (viewer.getRootLattice().getConceptSize() == GraphicalConcept.LARGE_NODE_SIZE) conceptSizeType = LMOptions.LARGE; else conceptSizeType = LMOptions.SMALL;
        Vector<GraphicalLattice> latticesList = new Vector<GraphicalLattice>();
        for (int i = 0; i < conceptLattices.size(); i++) {
            ConceptLattice currentConceptLattice = conceptLattices.elementAt(i);
            LatticeStructure currentStructure = structures.elementAt(i);
            Vector<LatticeStructure> currentStructuresList = new Vector<LatticeStructure>();
            currentStructuresList.add(new LatticeStructure(currentStructure.getConceptPositions()));
            Vector<ConceptLattice> internalLattices = new Vector<ConceptLattice>();
            internalLattices.add(currentConceptLattice);
            NestedLattice currentNestedLattice = new NestedLattice(null, internalLattices, null, GUIMessages.getString("GUI.structure"));
            GraphicalLattice currentGraphLattice = new GraphicalLattice(currentNestedLattice, null, currentStructuresList);
            currentGraphLattice.setLatticeColor(ColorSet.getColorAt(i), ColorSet.getColorStringAt(i));
            currentGraphLattice.setConceptSizeType(conceptSizeType);
            latticesList.add(currentGraphLattice);
        }
        structureFrame = new LatticeStructureFrame(viewer, latticesList);
    }

    public LatticePanel getLatticePanel() {
        return viewer;
    }

    private void backHistory() {
        viewer.backHistory();
    }

    private void forwardHistory() {
        viewer.forwardHistory();
    }

    private void zoomIn() {
        viewer.zoomIn();
        repaint();
    }

    private void zoomOut() {
        viewer.zoomOut();
        repaint();
    }

    private void zoomInSelectedArea() {
        if (viewer.getSelectedArea() != null) viewer.zoomInSelectedArea(); else DialogBox.showMessageInformation(viewer, GUIMessages.getString("GUI.noSelectedArea"), GUIMessages.getString("GUI.noZoomInSelectedArea"));
    }

    private void showEntireLattice() {
        viewer.addHistoryItem(LMHistory.SCALE_AND_MOVE);
        viewer.lockHistory();
        viewer.zoomInArea(viewer.getBoundsForLattice(viewer.getRootLattice()));
        viewer.unlockHistory();
    }

    private void showAllLabels() {
        viewer.showAllLabels();
    }

    private void hideAllLabels() {
        viewer.hideAllLabels();
    }

    private void showAreaLabels() {
        if (viewer.getSelectedArea() != null) viewer.showAreaLabels(viewer.getSelectedArea()); else DialogBox.showMessageInformation(viewer, GUIMessages.getString("GUI.noSelectedArea"), GUIMessages.getString("GUI.noLabelInSelectedArea"));
    }

    private void hideAreaLabels() {
        if (viewer.getSelectedArea() != null) viewer.hideAreaLabels(viewer.getSelectedArea()); else DialogBox.showMessageInformation(viewer, GUIMessages.getString("GUI.noSelectedArea"), GUIMessages.getString("GUI.noLabelInSelectedArea"));
    }

    private void duplicateViewer() {
        LatticeViewer duplicateViewer = new LatticeViewer((GraphicalLattice) viewer.getRootLattice().clone());
        LatticePanel duplicatePanel = duplicateViewer.getLatticePanel();
        duplicatePanel.getRootLattice().setScale(viewer.getRootLattice().getScale());
        duplicatePanel.getRootLattice().setRootPosition(viewer.getRootLattice().getRootPosition().getX(), viewer.getRootLattice().getRootPosition().getY());
        WindowListener[] listeners = getWindowListeners();
        for (int i = 0; i < listeners.length; i++) duplicateViewer.addWindowListener(listeners[i]);
        duplicateViewer.pack();
        duplicateViewer.setLocation((int) getLocation().getX() + 25, (int) getLocation().getY() + 25);
        duplicateViewer.setVisible(true);
    }

    /**
	 * Duplique la fenetre pour {@link GraphicalLattice} donn�
	 * @param gl le GraphicalLattice a afficher dans la nouvelle fen�tre
	 */
    public void duplicateViewer(GraphicalLattice gl) {
        LatticeViewer duplicateViewer = new LatticeViewer(gl);
        WindowListener[] listeners = getWindowListeners();
        for (int i = 0; i < listeners.length; i++) duplicateViewer.addWindowListener(listeners[i]);
        duplicateViewer.pack();
        duplicateViewer.setLocation((int) getLocation().getX() + 25, (int) getLocation().getY() + 25);
        duplicateViewer.setVisible(true);
    }

    private void showRulePanel() {
        ConceptLattice lattice;
        if (viewer.getRootLattice().getNestedLattice().getInternalLattices().size() == 1) lattice = viewer.getRootLattice().getNestedLattice().getConceptLattice(); else {
            BinaryContext context = viewer.getRootLattice().getNestedLattice().getGlobalContext();
            lattice = new ConceptLattice(context);
        }
        String suppStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumSupport") + " (%)", GUIMessages.getString("GUI.minimumSupportForRules"), null, "50");
        if (suppStr == null) return;
        double minSupp = -1;
        while (minSupp < 0 || minSupp > 100) {
            try {
                minSupp = Double.parseDouble(suppStr);
            } catch (NumberFormatException ex) {
                DialogBox.showMessageWarning(this, GUIMessages.getString("GUI.valueMustBeBetween0And100"), GUIMessages.getString("GUI.wrongSupportValue"));
                minSupp = -1;
                suppStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumSupport") + " (%)", GUIMessages.getString("GUI.minimumSupportForRules"), null, "50");
                if (suppStr == null) return;
            }
        }
        String confStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumConfidence") + " (%)", GUIMessages.getString("GUI.minimumConfidenceForRules"), null, "50");
        if (confStr == null) return;
        double minConf = -1;
        while (minConf < 0 || minConf > 100) {
            try {
                minConf = Double.parseDouble(confStr);
            } catch (NumberFormatException ex) {
                DialogBox.showMessageWarning(this, GUIMessages.getString("GUI.valueMustBeBetween0And100"), GUIMessages.getString("GUI.wrongConfidenceValue"));
                minConf = -1;
                confStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumConfidence") + " (%)", GUIMessages.getString("GUI.minimumConfidenceForRules"), null, "50");
                if (confStr == null) return;
            }
        }
        RuleAlgorithm algo = new InformativeBasisAlgorithm(lattice, minSupp / 100.0, minConf / 100.0);
        Vector<Rule> rules = algo.getRules();
        viewer.getRootLattice().getNestedLattice().setRules(algo.getRules());
        viewer.repaint();
        new RuleViewer(rules, viewer.getRootLattice().getNestedLattice().getName(), algo.getMinimumSupport(), algo.getMinimumConfidence(), viewer);
    }

    /**
	 * Ouvre le panneau de demande de nom pour le fichier image a enregistrer puis sauvegarde de
	 * l'image sous le nom de fichier specifie
	 */
    private void showCapturePanel() {
        JFileChooser fileChooser = new JFileChooser(LMPreferences.getLastDirectory());
        fileChooser.setApproveButtonText(GUIMessages.getString("GUI.save"));
        fileChooser.setDialogTitle(GUIMessages.getString("GUI.saveCurrentLatticeAsImage"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setSelectedFile(new File(glCurrent.getName() + GUIMessages.getString("GUI.latticeDefaultImageName")));
        ExampleFileFilter filterBMP = new ExampleFileFilter("bmp", GUIMessages.getString("GUI.bmpFormat"));
        fileChooser.addChoosableFileFilter(filterBMP);
        ExampleFileFilter filterJPG = new ExampleFileFilter("jpg", GUIMessages.getString("GUI.jpgFormat"));
        fileChooser.addChoosableFileFilter(filterJPG);
        ExampleFileFilter filterPNG = new ExampleFileFilter("png", GUIMessages.getString("GUI.pngFormat"));
        fileChooser.addChoosableFileFilter(filterPNG);
        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fileName = fileChooser.getSelectedFile();
            ExampleFileFilter currentFilter = (ExampleFileFilter) fileChooser.getFileFilter();
            ArrayList<String> extensions = currentFilter.getExtensionsList();
            String oldFileType = ExampleFileFilter.getExtension(fileName);
            if (extensions != null && !extensions.contains(oldFileType)) {
                String newFileType = extensions.get(0);
                String oldFileName = fileName.getAbsolutePath();
                int posOldExt = oldFileName.lastIndexOf(".");
                String newFileName = oldFileName + "." + newFileType;
                if (posOldExt != -1) newFileName = newFileName.substring(0, posOldExt) + "." + newFileType;
                fileName = new File(newFileName);
            }
            if (fileName.exists()) {
                int overwrite = DialogBox.showDialogWarning(this, GUIMessages.getString("GUI.doYouWantToOverwriteFile"), GUIMessages.getString("GUI.selectedFileAlreadyExist"));
                if (overwrite == DialogBox.NO) {
                    DialogBox.showMessageInformation(this, GUIMessages.getString("GUI.imageNotSaved"), GUIMessages.getString("GUI.notSaved"));
                    return;
                }
            }
            try {
                ScreenImage.createImage(viewer, fileName.getAbsolutePath());
                DialogBox.showMessageInformation(this, GUIMessages.getString("GUI.imageSuccessfullySaved"), GUIMessages.getString("GUI.saveSuccess"));
            } catch (IOException ioe) {
                DialogBox.showMessageError(this, GUIMessages.getString("GUI.imageCouldnotBeSaved"), GUIMessages.getString("GUI.errorWithFile"));
            }
            LMPreferences.setLastDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
        }
    }

    /**
	 * Ouvre une boite de dialogue pour sauvegarder le treillis
	 */
    private void saveCurrentLatticeAs() {
        if (!glCurrent.isNested()) {
            ConceptLattice lattice = glCurrent.getNestedLattice().getConceptLattice();
            JFileChooser fileChooser = new JFileChooser(LMPreferences.getLastDirectory());
            fileChooser.setApproveButtonText(GUIMessages.getString("GUI.saveAs"));
            fileChooser.setDialogTitle(GUIMessages.getString("GUI.saveCurrentLattice"));
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            ExampleFileFilter filterGaliciaXml = new ExampleFileFilter("lat.xml", GUIMessages.getString("GUI.galiciaXMLLatticeFormat"));
            fileChooser.addChoosableFileFilter(filterGaliciaXml);
            int returnVal = fileChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fileName = fileChooser.getSelectedFile();
                ExampleFileFilter currentFilter = (ExampleFileFilter) fileChooser.getFileFilter();
                ArrayList<String> extensions = currentFilter.getExtensionsList();
                String oldFileType = ExampleFileFilter.getExtension(fileName);
                String newFileType = oldFileType;
                if (extensions != null && !extensions.contains(oldFileType)) {
                    newFileType = extensions.get(0);
                    String oldFileName = fileName.getAbsolutePath();
                    int posOldExt = oldFileName.lastIndexOf(".");
                    String newFileName = oldFileName + "." + newFileType;
                    if (posOldExt != -1) newFileName = newFileName.substring(0, posOldExt) + "." + newFileType;
                    fileName = new File(newFileName);
                }
                if (fileName.exists()) {
                    int overwrite = DialogBox.showDialogWarning(this, GUIMessages.getString("GUI.doYouWantToOverwriteFile"), GUIMessages.getString("GUI.selectedFileAlreadyExist"));
                    if (overwrite == DialogBox.NO) {
                        DialogBox.showMessageInformation(this, GUIMessages.getString("GUI.latticeNotSaved"), GUIMessages.getString("GUI.notSaved"));
                        return;
                    }
                }
                try {
                    String fileType = ExampleFileFilter.getExtension(fileName);
                    if (fileType.equals("lat.xml")) {
                        new GaliciaXMLLatticeWriter(fileName, lattice);
                    } else {
                        DialogBox.showMessageError(this, GUIMessages.getString("GUI.latticeExtensionNotKnown"), GUIMessages.getString("GUI.wrongLatticeFormat"));
                        return;
                    }
                    DialogBox.showMessageInformation(this, GUIMessages.getString("GUI.latticeSuccessfullySaved"), GUIMessages.getString("GUI.saveSuccess"));
                } catch (IOException ioe) {
                    DialogBox.showMessageError(this, GUIMessages.getString("GUI.latticeCouldnotBeSaved"), GUIMessages.getString("GUI.errorWithFile"));
                } catch (WriterException e) {
                    DialogBox.showMessageError(this, e);
                }
                LMPreferences.setLastDirectory(fileChooser.getCurrentDirectory().getAbsolutePath());
            }
        }
    }

    private boolean hasSetRulesInLabels() {
        viewer.addHistoryItem(LMHistory.RULES);
        RuleAlgorithm algo = calcRules();
        if (algo != null) {
            viewer.getRootLattice().getNestedLattice().setRules(algo.getRules());
            return true;
        }
        return false;
    }

    private RuleAlgorithm calcRules() {
        ConceptLattice lattice;
        if (viewer.getRootLattice().getNestedLattice().getInternalLattices().size() == 1) lattice = viewer.getRootLattice().getNestedLattice().getConceptLattice(); else {
            BinaryContext context = viewer.getRootLattice().getNestedLattice().getGlobalContext();
            lattice = new ConceptLattice(context);
        }
        String suppStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumSupport") + " (%)", GUIMessages.getString("GUI.minimumSupportForRules"), null, "50");
        if (suppStr == null) return null;
        double minSupp = -1;
        while (minSupp < 0 || minSupp > 100) {
            try {
                minSupp = Double.parseDouble(suppStr);
            } catch (NumberFormatException ex) {
                DialogBox.showMessageWarning(this, GUIMessages.getString("GUI.valueMustBeBetween0And100"), GUIMessages.getString("GUI.wrongSupportValue"));
                minSupp = -1;
                suppStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumSupport") + " (%)", GUIMessages.getString("GUI.minimumSupportForRules"), null, "50");
                if (suppStr == null) return null;
            }
        }
        String confStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumConfidence") + " (%)", GUIMessages.getString("GUI.minimumConfidenceForRules"), null, "50");
        if (confStr == null) return null;
        double minConf = -1;
        while (minConf < 0 || minConf > 100) {
            try {
                minConf = Double.parseDouble(confStr);
            } catch (NumberFormatException ex) {
                DialogBox.showMessageWarning(this, GUIMessages.getString("GUI.valueMustBeBetween0And100"), GUIMessages.getString("GUI.wrongConfidenceValue"));
                minConf = -1;
                confStr = (String) DialogBox.showInputQuestion(this, GUIMessages.getString("GUI.enterMinimumConfidence") + " (%)", GUIMessages.getString("GUI.minimumConfidenceForRules"), null, "50");
                if (confStr == null) return null;
            }
        }
        RuleAlgorithm algo = null;
        algo = new InformativeBasisAlgorithm(lattice, minSupp / 100.0, minConf / 100.0);
        return algo;
    }

    public void setBackMessage(int action) {
        if (action == LMOptions.NONE) {
            previousBtn.setEnabled(false);
            frameMenu.setUndoOperation(false, null);
        } else {
            String msg = getHistoryMessage(action);
            previousBtn.setEnabled(true);
            previousBtn.setToolTipText(GUIMessages.getString("GUI.undo") + " (" + msg + ")");
            frameMenu.setUndoOperation(true, msg);
        }
    }

    public void setForwardMessage(int action) {
        if (action == LMOptions.NONE) {
            nextBtn.setEnabled(false);
            frameMenu.setRedoOperation(false, null);
        } else {
            String msg = getHistoryMessage(action);
            nextBtn.setEnabled(true);
            nextBtn.setToolTipText(GUIMessages.getString("GUI.redo") + " (" + msg + ")");
            frameMenu.setRedoOperation(true, msg);
        }
    }

    private String getHistoryMessage(int type) {
        switch(type) {
            case LMHistory.ALL:
                return GUIMessages.getString("GUI.historyAll");
            case LMHistory.LATTICE_PROJECTION:
                return GUIMessages.getString("GUI.historyLatticeprojection");
            case LMHistory.MOVE_LATTICE:
                return GUIMessages.getString("GUI.historyMoveLattice");
            case LMHistory.SCALE:
                return GUIMessages.getString("GUI.historyChangeScale");
            case LMHistory.SCALE_AND_MOVE:
                return GUIMessages.getString("GUI.historyChangeScaleAndMove");
            case LMHistory.SELECTION:
                return GUIMessages.getString("GUI.historySelectConcepts");
            case LMHistory.RULES:
                return GUIMessages.getString("GUI.historyChangeRules");
            case LMHistory.ATTRIBUTE_LABELS:
                return GUIMessages.getString("GUI.historyAttributeLabels");
            case LMHistory.OBJECT_LABELS:
                return GUIMessages.getString("GUI.historyObjectLabels");
            case LMHistory.RULE_LABELS:
                return GUIMessages.getString("GUI.historyRuleLabels");
            case LMHistory.ATTENTION:
                return GUIMessages.getString("GUI.historyShakeOrBlink");
            case LMHistory.CONTRAST:
                return GUIMessages.getString("GUI.historyBlurOrReduce");
            case LMHistory.SINGLE_SELECTION:
                return GUIMessages.getString("GUI.historySingleSelection");
            case LMHistory.MULTIPLE_SELECTION:
                return GUIMessages.getString("GUI.historyMultipleSelection");
            case LMHistory.GLOBAL_BOTTOM:
                return GUIMessages.getString("GUI.historyShowBottom");
            case LMHistory.ANIMATION:
                return GUIMessages.getString("GUI.historyShowAnimation");
            case LMHistory.STRUCTURE:
                return GUIMessages.getString("GUI.historyLatticeStructure");
            case LMHistory.COLOR:
                return GUIMessages.getString("GUI.historyColorIntensity");
            case LMHistory.SIZE:
                return GUIMessages.getString("GUI.historyConceptSize");
            case LMHistory.SELECT_NODE:
                return GUIMessages.getString("GUI.historySelectConcept");
            case LMHistory.DESELECT_NODE:
                return GUIMessages.getString("GUI.historyDeselectConcept");
            case LMHistory.CHANGE_OPTIONS:
                return GUIMessages.getString("GUI.historyPresetOptions");
            case LMHistory.APPROXIMATION:
                return GUIMessages.getString("GUI.historyLatticeApproximation");
            default:
                return GUIMessages.getString("GUI.historyUnknow");
        }
    }

    /**
	 * Sauvegarde les options courantes comme celles de l'utilisateur
	 */
    private void saveCurrentOptions() {
        Preferences preferences = LMPreferences.getPreferences();
        preferences.putInt(LMPreferences.SINGLE_SEL_TYPE, viewer.getSingleSelType());
        preferences.putInt(LMPreferences.MULT_SEL_TYPE, viewer.getMultSelType());
        preferences.putInt(LMPreferences.SEL_CONTRAST_TYPE, viewer.getSelectionContrastType());
        preferences.putBoolean(LMPreferences.HIDE_OUT_OF_FOCUS, viewer.isHideLabelForOutOfFocusConcept());
        preferences.putInt(LMPreferences.ATT_LABEL_TYPE, viewer.getAttLabelType());
        preferences.putInt(LMPreferences.OBJ_LABEL_TYPE, viewer.getObjLabelType());
        preferences.putInt(LMPreferences.RULES_LABEL_TYPE, viewer.getRulesLabelType());
        preferences.putBoolean(LMPreferences.ANIMATE_ZOOM, viewer.isAnimateZoom());
        preferences.putInt(LMPreferences.FEATURE_TYPE, viewer.getAttentionFeatureType());
        preferences.putBoolean(LMPreferences.CHANGE_COLOR_INTENSITY, viewer.isChangeColorIntensity());
        preferences.putInt(LMPreferences.CONCEPT_SIZE_TYPE, viewer.getConceptSizeType());
        preferences.putBoolean(LMPreferences.SHOW_ALL_CONCEPTS, !viewer.isBottomHidden());
        preferences.putBoolean(LMPreferences.SHOW_LATTICE_MAP, viewer.isShowLatticeMap());
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            DialogBox.showMessageError(this, GUIMessages.getString("GUI.problemDuringBackupOptions"), GUIMessages.getString("GUI.savingProblem"));
        }
    }

    public void changeMenuItem(int item, int value) {
        frameMenu.selectMenuItem(item, value);
    }

    /**
	 * Classe g�rant le menu du {@link LatticeViewer}
	 * @author Ludovic Thomas
	 * @version 1.0
	 */
    private class FrameMenu {

        boolean emptyBottomsHidden;

        JMenuItem saveLattice;

        JMenuItem exportLattice;

        JMenuItem quitFrame;

        ButtonGroup optionsGroup;

        JMenuItem lightOptions;

        JMenuItem heavyOptions;

        JMenuItem userOptions;

        JMenuItem userSaveOptions;

        JRadioButtonMenuItem noSingleSel;

        JRadioButtonMenuItem singleSelFilter;

        JRadioButtonMenuItem singleSelIdeal;

        JRadioButtonMenuItem singleSelFilterIdeal;

        JRadioButtonMenuItem singleSelParents;

        JRadioButtonMenuItem singleSelChildren;

        JRadioButtonMenuItem singleSelParentsChildren;

        JRadioButtonMenuItem singleSelChildParents;

        JRadioButtonMenuItem noMultipleSel;

        JRadioButtonMenuItem multipleSelFilter;

        JRadioButtonMenuItem multipleSelIdeal;

        JRadioButtonMenuItem multipleSelFilterIdeal;

        JRadioButtonMenuItem multipleSelSubLattice;

        JRadioButtonMenuItem blurItem;

        JRadioButtonMenuItem fisheyeItem;

        JCheckBoxMenuItem hideOutFocusLabelItem;

        JRadioButtonMenuItem noAttributesItem;

        JRadioButtonMenuItem attReducedItem;

        JRadioButtonMenuItem attListItem;

        JRadioButtonMenuItem noObjectsItem;

        JRadioButtonMenuItem objReducedItem;

        JRadioButtonMenuItem objListItem;

        JRadioButtonMenuItem objCountItem;

        JRadioButtonMenuItem objPercAllItem;

        JRadioButtonMenuItem objPercNodeItem;

        JRadioButtonMenuItem noRulesItem;

        JRadioButtonMenuItem showRulesItem;

        JMenuItem setRulesItem;

        JCheckBoxMenuItem zoomAnimItem;

        JRadioButtonMenuItem shakeAnimationItem;

        JRadioButtonMenuItem blinkAnimationItem;

        JRadioButtonMenuItem noSearchAnimationItem;

        JRadioButtonMenuItem differentIntensityItem;

        JRadioButtonMenuItem sameIntensityItem;

        JRadioButtonMenuItem largeSizeItem;

        JRadioButtonMenuItem smallSizeItem;

        JRadioButtonMenuItem varySizeItem;

        JMenuItem undoOperation;

        JMenuItem redoOperation;

        JMenuItem zoomInItem;

        JMenuItem zoomOutItem;

        JMenuItem zoomSelectItem;

        JMenuItem zoomLatticeItem;

        JMenuItem showAllLabelsItem;

        JMenuItem hideAreaLabelsItem;

        JMenuItem showAreaLabelsItem;

        JMenuItem hideAllLabelsItem;

        JMenuItem duplicateItem;

        JMenuItem showBottomItem;

        JMenuItem showLatticeMapItem;

        JMenuItem latticeStructureItem;

        JMenuItem rulesItem;

        JMenuBar menuBar;

        JMenu fileMenu;

        JMenu editMenu;

        JMenu optionsMenu;

        JMenu toolsMenu;

        JRadioButtonMenuItem noGeneItem;

        JRadioButtonMenuItem showGeneItem;

        /**
		 * Construit un {@link FrameMenu} avec les options utilisateurs par d�faut
		 */
        public FrameMenu() {
            buildMenuBar();
            if (glCurrent.getNestedLattice().getInternalLattices().size() > 0) varySizeItem.setEnabled(false);
        }

        /**
		 * @return la menuBar
		 */
        public JMenuBar getMenuBar() {
            return menuBar;
        }

        /**
		 * Construit la menuBar
		 */
        private void buildMenuBar() {
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            FrameMenuListener listener = new FrameMenuListener();
            menuBar = new JMenuBar();
            fileMenu = new JMenu(GUIMessages.getString("GUI.file"));
            fileMenu.setMnemonic(KeyEvent.VK_F);
            editMenu = new JMenu(GUIMessages.getString("GUI.edit"));
            editMenu.setMnemonic(KeyEvent.VK_E);
            optionsMenu = new JMenu(GUIMessages.getString("GUI.options"));
            optionsMenu.setMnemonic(KeyEvent.VK_P);
            toolsMenu = new JMenu(GUIMessages.getString("GUI.tools"));
            toolsMenu.setMnemonic(KeyEvent.VK_T);
            duplicateItem = new JMenuItem(GUIMessages.getString("GUI.duplicateWindow"));
            duplicateItem.addActionListener(listener);
            duplicateItem.setMnemonic(KeyEvent.VK_D);
            duplicateItem.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
            fileMenu.add(duplicateItem);
            fileMenu.addSeparator();
            if (!glCurrent.isNested()) {
                saveLattice = new JMenuItem(GUIMessages.getString("GUI.saveAs"));
                saveLattice.addActionListener(listener);
                saveLattice.setMnemonic(KeyEvent.VK_S);
                saveLattice.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
                fileMenu.add(saveLattice);
            }
            exportLattice = new JMenuItem(GUIMessages.getString("GUI.exportAsImage"));
            exportLattice.addActionListener(listener);
            exportLattice.setMnemonic(KeyEvent.VK_E);
            exportLattice.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
            fileMenu.add(exportLattice);
            fileMenu.addSeparator();
            quitFrame = new JMenuItem(GUIMessages.getString("GUI.quit"));
            quitFrame.addActionListener(listener);
            quitFrame.setMnemonic(KeyEvent.VK_Q);
            quitFrame.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
            fileMenu.add(quitFrame);
            undoOperation = new JMenuItem(GUIMessages.getString("GUI.undo"));
            undoOperation.addActionListener(listener);
            undoOperation.setMnemonic(KeyEvent.VK_U);
            undoOperation.setAccelerator(KeyStroke.getKeyStroke("ctrl Z"));
            undoOperation.setEnabled(false);
            editMenu.add(undoOperation);
            redoOperation = new JMenuItem(GUIMessages.getString("GUI.redo"));
            redoOperation.addActionListener(listener);
            redoOperation.setMnemonic(KeyEvent.VK_R);
            redoOperation.setAccelerator(KeyStroke.getKeyStroke("ctrl Y"));
            redoOperation.setEnabled(false);
            editMenu.add(redoOperation);
            editMenu.addSeparator();
            JMenu zoomMenu = new JMenu(GUIMessages.getString("GUI.zoom"));
            zoomMenu.setMnemonic(KeyEvent.VK_Z);
            zoomInItem = new JMenuItem(GUIMessages.getString("GUI.zoomIn"));
            zoomInItem.addActionListener(listener);
            zoomInItem.setMnemonic(KeyEvent.VK_I);
            zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_MASK));
            zoomMenu.add(zoomInItem);
            zoomOutItem = new JMenuItem(GUIMessages.getString("GUI.zoomOut"));
            zoomOutItem.addActionListener(listener);
            zoomOutItem.setMnemonic(KeyEvent.VK_O);
            zoomOutItem.setDisplayedMnemonicIndex(5);
            zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_MASK));
            zoomMenu.add(zoomOutItem);
            zoomMenu.addSeparator();
            zoomSelectItem = new JMenuItem(GUIMessages.getString("GUI.zoomInSelectedArea"));
            zoomSelectItem.addActionListener(listener);
            zoomSelectItem.setMnemonic(KeyEvent.VK_S);
            zoomSelectItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift S"));
            zoomMenu.add(zoomSelectItem);
            zoomLatticeItem = new JMenuItem(GUIMessages.getString("GUI.showAllEntireLattice"));
            zoomLatticeItem.addActionListener(listener);
            zoomLatticeItem.setMnemonic(KeyEvent.VK_A);
            zoomLatticeItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift A"));
            zoomMenu.add(zoomLatticeItem);
            editMenu.add(zoomMenu);
            JMenu showLabelsMenu = new JMenu(GUIMessages.getString("GUI.labels"));
            showLabelsMenu.setMnemonic(KeyEvent.VK_L);
            showAllLabelsItem = new JMenuItem(GUIMessages.getString("GUI.showAllLabels"));
            showAllLabelsItem.addActionListener(listener);
            showLabelsMenu.add(showAllLabelsItem);
            hideAllLabelsItem = new JMenuItem(GUIMessages.getString("GUI.hideAllLabels"));
            hideAllLabelsItem.addActionListener(listener);
            showLabelsMenu.add(hideAllLabelsItem);
            showLabelsMenu.addSeparator();
            showAreaLabelsItem = new JMenuItem(GUIMessages.getString("GUI.showLabelsInSelectedArea"));
            showAreaLabelsItem.addActionListener(listener);
            showLabelsMenu.add(showAreaLabelsItem);
            hideAreaLabelsItem = new JMenuItem(GUIMessages.getString("GUI.hideLabelsInSelectedArea"));
            hideAreaLabelsItem.addActionListener(listener);
            showLabelsMenu.add(hideAreaLabelsItem);
            editMenu.add(showLabelsMenu);
            JMenu presetsMenu = new JMenu(GUIMessages.getString("GUI.presetOptions"));
            presetsMenu.setMnemonic(KeyEvent.VK_P);
            optionsGroup = new ButtonGroup();
            lightOptions = new JRadioButtonMenuItem(GUIMessages.getString("GUI.lightOptions"));
            lightOptions.addActionListener(listener);
            lightOptions.setMnemonic(KeyEvent.VK_1);
            lightOptions.setAccelerator(KeyStroke.getKeyStroke("ctrl NUMPAD1"));
            optionsGroup.add(lightOptions);
            presetsMenu.add(lightOptions);
            heavyOptions = new JRadioButtonMenuItem(GUIMessages.getString("GUI.heavyOptions"));
            heavyOptions.addActionListener(listener);
            heavyOptions.setMnemonic(KeyEvent.VK_2);
            heavyOptions.setAccelerator(KeyStroke.getKeyStroke("ctrl NUMPAD2"));
            optionsGroup.add(heavyOptions);
            presetsMenu.add(heavyOptions);
            userOptions = new JRadioButtonMenuItem(GUIMessages.getString("GUI.userOptions"));
            userOptions.addActionListener(listener);
            userOptions.setMnemonic(KeyEvent.VK_3);
            userOptions.setAccelerator(KeyStroke.getKeyStroke("ctrl NUMPAD3"));
            optionsGroup.add(userOptions);
            presetsMenu.add(userOptions);
            presetsMenu.addSeparator();
            userSaveOptions = new JMenuItem(GUIMessages.getString("GUI.saveCurrentOptionsAsUsers"));
            userSaveOptions.addActionListener(listener);
            presetsMenu.add(userSaveOptions);
            optionsMenu.add(presetsMenu);
            optionsMenu.addSeparator();
            JMenu selectionsMenu = new JMenu(GUIMessages.getString("GUI.selections"));
            selectionsMenu.setMnemonic(KeyEvent.VK_S);
            JMenu singleSelMenu = new JMenu(GUIMessages.getString("GUI.singleConcept"));
            ButtonGroup singleSelGroup = new ButtonGroup();
            noSingleSel = new JRadioButtonMenuItem(GUIMessages.getString("GUI.conceptOnly"));
            noSingleSel.addActionListener(listener);
            singleSelGroup.add(noSingleSel);
            singleSelMenu.add(noSingleSel);
            singleSelFilter = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showFilter"));
            singleSelFilter.addActionListener(listener);
            singleSelGroup.add(singleSelFilter);
            singleSelMenu.add(singleSelFilter);
            singleSelIdeal = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showIdeal"));
            singleSelIdeal.addActionListener(listener);
            singleSelGroup.add(singleSelIdeal);
            singleSelMenu.add(singleSelIdeal);
            singleSelFilterIdeal = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showFilterAndIdeal"));
            singleSelFilterIdeal.addActionListener(listener);
            singleSelGroup.add(singleSelFilterIdeal);
            singleSelMenu.add(singleSelFilterIdeal);
            singleSelParents = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showParents"));
            singleSelParents.addActionListener(listener);
            singleSelGroup.add(singleSelParents);
            singleSelMenu.add(singleSelParents);
            singleSelChildren = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showChildren"));
            singleSelChildren.addActionListener(listener);
            singleSelGroup.add(singleSelChildren);
            singleSelMenu.add(singleSelChildren);
            singleSelParentsChildren = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showParentsAndChildren"));
            singleSelParentsChildren.addActionListener(listener);
            singleSelGroup.add(singleSelParentsChildren);
            singleSelMenu.add(singleSelParentsChildren);
            selectionsMenu.add(singleSelMenu);
            JMenu multipleSelMenu = new JMenu(GUIMessages.getString("GUI.multipleConcepts"));
            ButtonGroup multipleSelGroup = new ButtonGroup();
            noMultipleSel = new JRadioButtonMenuItem(GUIMessages.getString("GUI.conceptsOnly"));
            noMultipleSel.addActionListener(listener);
            multipleSelGroup.add(noMultipleSel);
            multipleSelMenu.add(noMultipleSel);
            multipleSelFilter = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showCommonFilter"));
            multipleSelFilter.addActionListener(listener);
            multipleSelGroup.add(multipleSelFilter);
            multipleSelMenu.add(multipleSelFilter);
            multipleSelIdeal = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showCommonIdeal"));
            multipleSelIdeal.addActionListener(listener);
            multipleSelGroup.add(multipleSelIdeal);
            multipleSelMenu.add(multipleSelIdeal);
            multipleSelFilterIdeal = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showCommonFilterAndIdeal"));
            multipleSelFilterIdeal.addActionListener(listener);
            multipleSelGroup.add(multipleSelFilterIdeal);
            multipleSelMenu.add(multipleSelFilterIdeal);
            multipleSelSubLattice = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showSubLattice"));
            multipleSelSubLattice.addActionListener(listener);
            multipleSelGroup.add(multipleSelSubLattice);
            multipleSelMenu.add(multipleSelSubLattice);
            selectionsMenu.add(multipleSelMenu);
            selectionsMenu.addSeparator();
            ButtonGroup selectionGroup = new ButtonGroup();
            blurItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.blurUnrelatedConcepts"));
            blurItem.addActionListener(listener);
            selectionGroup.add(blurItem);
            selectionsMenu.add(blurItem);
            fisheyeItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.reduceUnrelatedConcepts"));
            fisheyeItem.addActionListener(listener);
            selectionGroup.add(fisheyeItem);
            selectionsMenu.add(fisheyeItem);
            optionsMenu.add(selectionsMenu);
            JMenu labelsMenu = new JMenu(GUIMessages.getString("GUI.labelInformation"));
            labelsMenu.setMnemonic(KeyEvent.VK_L);
            hideOutFocusLabelItem = new JCheckBoxMenuItem(GUIMessages.getString("GUI.hideLabelForBluredConcept"));
            hideOutFocusLabelItem.addActionListener(listener);
            hideOutFocusLabelItem.setMnemonic(KeyEvent.VK_H);
            hideOutFocusLabelItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift H"));
            labelsMenu.add(hideOutFocusLabelItem);
            labelsMenu.addSeparator();
            JMenu attributesMenu = new JMenu(GUIMessages.getString("GUI.attributes"));
            ButtonGroup attLabelsGroup = new ButtonGroup();
            noAttributesItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.dontShowAttributes"));
            noAttributesItem.addActionListener(listener);
            attLabelsGroup.add(noAttributesItem);
            attributesMenu.add(noAttributesItem);
            attReducedItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.reducedLabelling"));
            attReducedItem.addActionListener(listener);
            attLabelsGroup.add(attReducedItem);
            attributesMenu.add(attReducedItem);
            attListItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.completeList"));
            attListItem.addActionListener(listener);
            attLabelsGroup.add(attListItem);
            attributesMenu.add(attListItem);
            labelsMenu.add(attributesMenu);
            JMenu objectsMenu = new JMenu(GUIMessages.getString("GUI.objects"));
            ButtonGroup objLabelsGroup = new ButtonGroup();
            noObjectsItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.dontShowObjects"));
            noObjectsItem.addActionListener(listener);
            objLabelsGroup.add(noObjectsItem);
            objectsMenu.add(noObjectsItem);
            objReducedItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.reducedLabelling"));
            objReducedItem.addActionListener(listener);
            objLabelsGroup.add(objReducedItem);
            objectsMenu.add(objReducedItem);
            objListItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.completeList"));
            objListItem.addActionListener(listener);
            objLabelsGroup.add(objListItem);
            objectsMenu.add(objListItem);
            objCountItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.count"));
            objCountItem.addActionListener(listener);
            objLabelsGroup.add(objCountItem);
            objectsMenu.add(objCountItem);
            objPercAllItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.purcentOfAllObjects"));
            objPercAllItem.addActionListener(listener);
            objLabelsGroup.add(objPercAllItem);
            objectsMenu.add(objPercAllItem);
            objPercNodeItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.purcentOfExternalObjects"));
            objPercNodeItem.addActionListener(listener);
            objLabelsGroup.add(objPercNodeItem);
            objectsMenu.add(objPercNodeItem);
            labelsMenu.add(objectsMenu);
            JMenu geneMenu = new JMenu(GUIMessages.getString("GUI.generators"));
            ButtonGroup geneLabelsGroup = new ButtonGroup();
            noGeneItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.dontShowGene"));
            noGeneItem.setSelected(true);
            noGeneItem.addActionListener(listener);
            geneLabelsGroup.add(noGeneItem);
            geneMenu.add(noGeneItem);
            showGeneItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showGene"));
            showGeneItem.addActionListener(listener);
            geneLabelsGroup.add(showGeneItem);
            geneMenu.add(showGeneItem);
            labelsMenu.add(geneMenu);
            JMenu rulesMenu = new JMenu(GUIMessages.getString("GUI.rules"));
            setRulesItem = new JMenuItem(GUIMessages.getString("GUI.setRules"));
            setRulesItem.addActionListener(listener);
            rulesMenu.add(setRulesItem);
            rulesMenu.addSeparator();
            ButtonGroup rulesLabelsGroup = new ButtonGroup();
            noRulesItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.dontShowRules"));
            noRulesItem.addActionListener(listener);
            noRulesItem.setEnabled(false);
            rulesLabelsGroup.add(noRulesItem);
            rulesMenu.add(noRulesItem);
            showRulesItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.showRules"));
            showRulesItem.addActionListener(listener);
            showRulesItem.setEnabled(false);
            rulesLabelsGroup.add(showRulesItem);
            rulesMenu.add(showRulesItem);
            labelsMenu.add(rulesMenu);
            optionsMenu.add(labelsMenu);
            JMenu animationMenu = new JMenu(GUIMessages.getString("GUI.animations"));
            animationMenu.setMnemonic(KeyEvent.VK_A);
            zoomAnimItem = new JCheckBoxMenuItem(GUIMessages.getString("GUI.enableZoomAnimation"));
            zoomAnimItem.addActionListener(listener);
            zoomAnimItem.setMnemonic(KeyEvent.VK_A);
            zoomAnimItem.setDisplayedMnemonicIndex(12);
            animationMenu.add(zoomAnimItem);
            animationMenu.addSeparator();
            ButtonGroup searchGroup = new ButtonGroup();
            shakeAnimationItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.shakeFoundConcept"));
            shakeAnimationItem.addActionListener(listener);
            searchGroup.add(shakeAnimationItem);
            animationMenu.add(shakeAnimationItem);
            blinkAnimationItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.blinkFoundConcept"));
            blinkAnimationItem.addActionListener(listener);
            searchGroup.add(blinkAnimationItem);
            animationMenu.add(blinkAnimationItem);
            noSearchAnimationItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.noAnimationForFoundConcept"));
            noSearchAnimationItem.addActionListener(listener);
            searchGroup.add(noSearchAnimationItem);
            animationMenu.add(noSearchAnimationItem);
            optionsMenu.add(animationMenu);
            optionsMenu.addSeparator();
            JMenu conceptIntensityMenu = new JMenu(GUIMessages.getString("GUI.conceptColor"));
            conceptIntensityMenu.setMnemonic(KeyEvent.VK_C);
            ButtonGroup intensityGroup = new ButtonGroup();
            differentIntensityItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.intensityDependsOnObjectCount"));
            differentIntensityItem.addActionListener(listener);
            intensityGroup.add(differentIntensityItem);
            conceptIntensityMenu.add(differentIntensityItem);
            sameIntensityItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.sameIntensityForAllConcepts"));
            sameIntensityItem.addActionListener(listener);
            intensityGroup.add(sameIntensityItem);
            conceptIntensityMenu.add(sameIntensityItem);
            optionsMenu.add(conceptIntensityMenu);
            JMenu conceptSizeMenu = new JMenu(GUIMessages.getString("GUI.conceptSize"));
            conceptSizeMenu.setMnemonic(KeyEvent.VK_C);
            ButtonGroup sizeGroup = new ButtonGroup();
            largeSizeItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.sameForAllConceptsLarge"));
            largeSizeItem.addActionListener(listener);
            sizeGroup.add(largeSizeItem);
            conceptSizeMenu.add(largeSizeItem);
            smallSizeItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.sameForAllConceptsSmall"));
            smallSizeItem.addActionListener(listener);
            sizeGroup.add(smallSizeItem);
            conceptSizeMenu.add(smallSizeItem);
            varySizeItem = new JRadioButtonMenuItem(GUIMessages.getString("GUI.dependsOnOwnedObjects"));
            varySizeItem.addActionListener(listener);
            sizeGroup.add(varySizeItem);
            conceptSizeMenu.add(varySizeItem);
            optionsMenu.add(conceptSizeMenu);
            optionsMenu.addSeparator();
            showBottomItem = new JMenuItem(GUIMessages.getString("GUI.hideEmptyInfimum"));
            showBottomItem.addActionListener(listener);
            showBottomItem.setAccelerator(KeyStroke.getKeyStroke("ctrl I"));
            optionsMenu.add(showBottomItem);
            showLatticeMapItem = new JMenuItem(GUIMessages.getString("GUI.hideLatticeMap"));
            showLatticeMapItem.addActionListener(listener);
            showLatticeMapItem.setAccelerator(KeyStroke.getKeyStroke("ctrl H"));
            optionsMenu.add(showLatticeMapItem);
            latticeStructureItem = new JMenuItem(GUIMessages.getString("GUI.modifyLatticeStructure"));
            latticeStructureItem.addActionListener(listener);
            latticeStructureItem.setMnemonic(KeyEvent.VK_M);
            latticeStructureItem.setAccelerator(KeyStroke.getKeyStroke("ctrl M"));
            toolsMenu.add(latticeStructureItem);
            rulesItem = new JMenuItem(GUIMessages.getString("GUI.showRules"));
            rulesItem.addActionListener(listener);
            rulesItem.setMnemonic(KeyEvent.VK_R);
            rulesItem.setAccelerator(KeyStroke.getKeyStroke("ctrl R"));
            toolsMenu.add(rulesItem);
            menuBar.add(fileMenu);
            menuBar.add(editMenu);
            menuBar.add(optionsMenu);
            menuBar.add(toolsMenu);
        }

        /**
		 * Selectionne les bons menus selon certaines actions
		 * @param item le type d'item a selectionner via LMHistory
		 * @param value la valeur de l'item
		 */
        protected void selectMenuItem(int item, int value) {
            switch(item) {
                case LMHistory.ATTRIBUTE_LABELS:
                    switch(value) {
                        case LMOptions.NO_LABEL:
                            noAttributesItem.setSelected(true);
                            break;
                        case LMOptions.ATTRIBUTES_ALL:
                            attListItem.setSelected(true);
                            break;
                        case LMOptions.ATTRIBUTES_REDUCED:
                        default:
                            attReducedItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.OBJECT_LABELS:
                    switch(value) {
                        case LMOptions.NO_LABEL:
                            noObjectsItem.setSelected(true);
                            break;
                        case LMOptions.OBJECTS_REDUCED:
                            objReducedItem.setSelected(true);
                            break;
                        case LMOptions.OBJECTS_ALL:
                            objListItem.setSelected(true);
                            break;
                        case LMOptions.OBJECTS_NUMBER:
                            objCountItem.setSelected(true);
                            break;
                        case LMOptions.OBJECTS_PERC_CTX:
                            objPercAllItem.setSelected(true);
                            break;
                        case LMOptions.OBJECTS_PERC_NODE:
                        default:
                            objPercNodeItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.RULE_LABELS:
                    switch(value) {
                        case LMOptions.NO_LABEL:
                            noRulesItem.setSelected(true);
                            break;
                        case LMOptions.RULES_SHOW:
                        default:
                            showRulesItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.GENE_LABELS:
                    switch(value) {
                        case LMOptions.NO_LABEL:
                            noGeneItem.setSelected(true);
                            break;
                        case LMOptions.GENE_SHOW:
                        default:
                            showGeneItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.ATTENTION:
                    switch(value) {
                        case LMOptions.NONE:
                            noSearchAnimationItem.setSelected(true);
                            break;
                        case LMOptions.SHAKE:
                            shakeAnimationItem.setSelected(true);
                            break;
                        case LMOptions.BLINK:
                        default:
                            blinkAnimationItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.CONTRAST:
                    switch(value) {
                        case LMOptions.BLUR:
                            blurItem.setSelected(true);
                            break;
                        case LMOptions.FISHEYE:
                        default:
                            fisheyeItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.SINGLE_SELECTION:
                    switch(value) {
                        case LMOptions.NONE:
                            noSingleSel.setSelected(true);
                            break;
                        case LMOptions.FILTER:
                            singleSelFilter.setSelected(true);
                            break;
                        case LMOptions.IDEAL:
                            singleSelIdeal.setSelected(true);
                            break;
                        case LMOptions.FILTER_IDEAL:
                            singleSelFilterIdeal.setSelected(true);
                            break;
                        case LMOptions.PARENTS:
                            singleSelParents.setSelected(true);
                            break;
                        case LMOptions.CHILDREN:
                            singleSelChildren.setSelected(true);
                            break;
                        case LMOptions.PARENTS_CHILDREN:
                            singleSelParentsChildren.setSelected(true);
                            break;
                        case LMOptions.CHILDREN_PARENTS:
                        default:
                            singleSelChildParents.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.MULTIPLE_SELECTION:
                    switch(value) {
                        case LMOptions.NONE:
                            noMultipleSel.setSelected(true);
                            break;
                        case LMOptions.COMMON_FILTER:
                            multipleSelFilter.setSelected(true);
                            break;
                        case LMOptions.COMMON_IDEAL:
                            multipleSelIdeal.setSelected(true);
                            break;
                        case LMOptions.COMMON_FILTER_IDEAL:
                            multipleSelFilterIdeal.setSelected(true);
                            break;
                        case LMOptions.SUB_LATTICE:
                        default:
                            multipleSelSubLattice.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.GLOBAL_BOTTOM:
                    switch(value) {
                        case LMOptions.NONE:
                            showBottomItem.setSelected(false);
                            break;
                        case LMOptions.SHOW:
                        default:
                            showBottomItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.ANIMATION:
                    switch(value) {
                        case LMOptions.NONE:
                            zoomAnimItem.setSelected(false);
                            break;
                        case LMOptions.ANIMATION_OK:
                        default:
                            zoomAnimItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.COLOR:
                    switch(value) {
                        case LMOptions.CHANGE:
                            differentIntensityItem.setSelected(true);
                            break;
                        case LMOptions.SAME:
                        default:
                            sameIntensityItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.SIZE:
                    switch(value) {
                        case LMOptions.LARGE:
                            largeSizeItem.setSelected(true);
                            break;
                        case LMOptions.SMALL:
                            smallSizeItem.setSelected(true);
                            break;
                        case LMOptions.VARY:
                        default:
                            varySizeItem.setSelected(true);
                            break;
                    }
                    break;
                case LMHistory.CHANGE_OPTIONS:
                    switch(value) {
                        case LMOptions.LIGHT:
                            singleSelFilterIdeal.setSelected(true);
                            multipleSelFilterIdeal.setSelected(true);
                            blurItem.setSelected(true);
                            hideOutFocusLabelItem.setSelected(true);
                            attReducedItem.setSelected(true);
                            objPercNodeItem.setSelected(true);
                            setRulesHidden(true);
                            zoomAnimItem.setSelected(false);
                            blinkAnimationItem.setSelected(true);
                            sameIntensityItem.setSelected(true);
                            largeSizeItem.setSelected(true);
                            setEmptyBottomHidden(false);
                            setLatticeMapHidden(true);
                            break;
                        case LMOptions.HEAVY:
                            singleSelFilterIdeal.setSelected(true);
                            multipleSelFilterIdeal.setSelected(true);
                            fisheyeItem.setSelected(true);
                            hideOutFocusLabelItem.setSelected(false);
                            attListItem.setSelected(true);
                            objListItem.setSelected(true);
                            setRulesHidden(false);
                            showGeneItem.setSelected(true);
                            zoomAnimItem.setSelected(true);
                            shakeAnimationItem.setSelected(true);
                            differentIntensityItem.setSelected(true);
                            largeSizeItem.setSelected(true);
                            setEmptyBottomHidden(false);
                            setLatticeMapHidden(false);
                            break;
                        case LMOptions.USER:
                        default:
                            Preferences preferences = LMPreferences.getPreferences();
                            changeMenuItem(LMHistory.SINGLE_SELECTION, preferences.getInt(LMPreferences.SINGLE_SEL_TYPE, LMOptions.FILTER_IDEAL));
                            changeMenuItem(LMHistory.MULTIPLE_SELECTION, preferences.getInt(LMPreferences.MULT_SEL_TYPE, LMOptions.COMMON_FILTER_IDEAL));
                            changeMenuItem(LMHistory.CONTRAST, preferences.getInt(LMPreferences.SEL_CONTRAST_TYPE, LMOptions.FISHEYE));
                            hideOutFocusLabelItem.setSelected(preferences.getBoolean(LMPreferences.HIDE_OUT_OF_FOCUS, false));
                            changeMenuItem(LMHistory.ATTRIBUTE_LABELS, preferences.getInt(LMPreferences.ATT_LABEL_TYPE, LMOptions.ATTRIBUTES_ALL));
                            changeMenuItem(LMHistory.OBJECT_LABELS, preferences.getInt(LMPreferences.OBJ_LABEL_TYPE, LMOptions.OBJECTS_ALL));
                            changeMenuItem(LMHistory.RULE_LABELS, preferences.getInt(LMPreferences.RULES_LABEL_TYPE, LMOptions.RULES_SHOW));
                            boolean hideRules = preferences.getInt(LMPreferences.RULES_LABEL_TYPE, LMOptions.RULES_SHOW) == LMOptions.NO_LABEL;
                            setRulesHidden(hideRules);
                            zoomAnimItem.setSelected(preferences.getBoolean(LMPreferences.ANIMATE_ZOOM, true));
                            changeMenuItem(LMHistory.ATTENTION, preferences.getInt(LMPreferences.FEATURE_TYPE, LMOptions.SHAKE));
                            if (preferences.getBoolean(LMPreferences.CHANGE_COLOR_INTENSITY, true)) {
                                changeMenuItem(LMHistory.COLOR, LMOptions.CHANGE);
                            } else {
                                changeMenuItem(LMHistory.COLOR, LMOptions.SAME);
                            }
                            changeMenuItem(LMHistory.SIZE, preferences.getInt(LMPreferences.CONCEPT_SIZE_TYPE, LMOptions.LARGE));
                            setEmptyBottomHidden(false);
                            setLatticeMapHidden(!preferences.getBoolean(LMPreferences.SHOW_LATTICE_MAP, true));
                            break;
                    }
                    break;
                default:
                    break;
            }
        }

        /**
		 * Change le message pour le menu "Undo"
		 * @param enabled vrai s'il faut activer le menu "Undo", faux sinon
		 * @param message le message a mettre si le "Undo" est possible
		 */
        protected void setUndoOperation(boolean enabled, String message) {
            undoOperation.setEnabled(enabled);
            if (enabled && message != null && !message.equals("")) undoOperation.setText(GUIMessages.getString("GUI.undo") + " (" + message + ")");
        }

        /**
		 * Change le message pour le menu "Redo"
		 * @param enabled vrai s'il faut activer le menu "Redo", faux sinon
		 * @param message le message a mettre si le "Redo" est possible
		 */
        protected void setRedoOperation(boolean enabled, String message) {
            redoOperation.setEnabled(enabled);
            if (enabled && message != null && !message.equals("")) redoOperation.setText(GUIMessages.getString("GUI.redo") + " (" + message + ")");
        }

        /**
		 * Change le choix d'affichage de l'infimum s'il est vide et previens le treillis
		 * @param hide vrai s'il faut masquer l'infimum s'il est vide, faux sinon
		 */
        protected void setEmptyBottomHidden(boolean hide) {
            if (hide) {
                if (viewer.hasHideEmptyBottomConcepts()) {
                    emptyBottomsHidden = true;
                    showBottomItem.setText(GUIMessages.getString("GUI.showEmptyInfimum"));
                }
            } else {
                emptyBottomsHidden = false;
                showBottomItem.setText(GUIMessages.getString("GUI.hideEmptyInfimum"));
                viewer.showAllConcepts();
            }
        }

        /**
		 * Change le choix d'affichage de la petite carte du treillis et previens le treillis
		 * @param hide vrai s'il faut masquer la petite carte, faux sinon
		 */
        protected void setLatticeMapHidden(boolean hide) {
            if (hide) {
                showLatticeMapItem.setSelected(true);
                showLatticeMapItem.setText(GUIMessages.getString("GUI.showLatticeMap"));
                viewer.showLatticeMap(false);
            } else {
                showLatticeMapItem.setSelected(false);
                showLatticeMapItem.setText(GUIMessages.getString("GUI.hideLatticeMap"));
                viewer.showLatticeMap(true);
            }
        }

        /**
		 * Change le choix d'affichage des r�gles et previens le treillis
		 * @param hide vrai s'il faut masquer les r�gles, faux sinon
		 */
        protected void setRulesHidden(boolean hide) {
            if (hide) {
                viewer.lockHistory();
                noRulesItem.setSelected(true);
                viewer.setRulesLabelType(LMOptions.NO_LABEL);
                viewer.unlockHistory();
            } else {
                viewer.lockHistory();
                showRulesItem.setSelected(true);
                viewer.setRulesLabelType(LMOptions.RULES_SHOW);
                viewer.unlockHistory();
            }
        }

        /**
		 * Change le preset option et previens le treillis
		 * @param optionsType le type de preset
		 */
        protected void setPresetOptions(int optionsType) {
            if (optionsType != LMOptions.LIGHT && optionsType != LMOptions.HEAVY && optionsType != LMOptions.USER) return;
            switch(optionsType) {
                case LMOptions.LIGHT:
                    lightOptions.setSelected(true);
                    break;
                case LMOptions.HEAVY:
                    heavyOptions.setSelected(true);
                    break;
                case LMOptions.USER:
                default:
                    userOptions.setSelected(true);
                    break;
            }
            selectMenuItem(LMHistory.CHANGE_OPTIONS, optionsType);
            viewer.changeOptions(optionsType);
        }

        /**
		 * Listener du menu
		 * @author Genevi�ve Roberge
		 * @version 1.0
		 */
        private class FrameMenuListener implements ActionListener {

            public void actionPerformed(ActionEvent ae) {
                try {
                    if (ae.getSource() == lightOptions) {
                        setPresetOptions(LMOptions.LIGHT);
                    } else if (ae.getSource() == heavyOptions) {
                        setPresetOptions(LMOptions.HEAVY);
                    } else if (ae.getSource() == userOptions) {
                        setPresetOptions(LMOptions.USER);
                    } else if (ae.getSource() == userSaveOptions) {
                        saveCurrentOptions();
                    } else if (ae.getSource() == noSingleSel) {
                        viewer.setSingleSelType(LMOptions.NONE);
                    } else if (ae.getSource() == singleSelFilter) {
                        viewer.setSingleSelType(LMOptions.FILTER);
                    } else if (ae.getSource() == singleSelIdeal) {
                        viewer.setSingleSelType(LMOptions.IDEAL);
                    } else if (ae.getSource() == singleSelFilterIdeal) {
                        viewer.setSingleSelType(LMOptions.FILTER_IDEAL);
                    } else if (ae.getSource() == singleSelParents) {
                        viewer.setSingleSelType(LMOptions.PARENTS);
                    } else if (ae.getSource() == singleSelChildren) {
                        viewer.setSingleSelType(LMOptions.CHILDREN);
                    } else if (ae.getSource() == singleSelParentsChildren) {
                        viewer.setSingleSelType(LMOptions.PARENTS_CHILDREN);
                    } else if (ae.getSource() == singleSelChildParents) {
                        viewer.setSingleSelType(LMOptions.CHILDREN_PARENTS);
                    } else if (ae.getSource() == noMultipleSel) {
                        viewer.setMultSelType(LMOptions.NONE);
                    } else if (ae.getSource() == multipleSelFilter) {
                        viewer.setMultSelType(LMOptions.COMMON_FILTER);
                    } else if (ae.getSource() == multipleSelIdeal) {
                        viewer.setMultSelType(LMOptions.COMMON_IDEAL);
                    } else if (ae.getSource() == multipleSelFilterIdeal) {
                        viewer.setMultSelType(LMOptions.COMMON_FILTER_IDEAL);
                    } else if (ae.getSource() == multipleSelSubLattice) {
                        viewer.setMultSelType(LMOptions.SUB_LATTICE);
                    } else if (ae.getSource() == blurItem) {
                        viewer.setSelectionContrastType(LMOptions.BLUR);
                    } else if (ae.getSource() == fisheyeItem) {
                        viewer.setSelectionContrastType(LMOptions.FISHEYE);
                    } else if (ae.getSource() == hideOutFocusLabelItem) {
                        viewer.hideLabelForOutOfFocusConcept(hideOutFocusLabelItem.isSelected());
                    } else if (ae.getSource() == noAttributesItem) {
                        viewer.setAttLabelType(LMOptions.NO_LABEL);
                    } else if (ae.getSource() == attReducedItem) {
                        viewer.setAttLabelType(LMOptions.ATTRIBUTES_REDUCED);
                    } else if (ae.getSource() == attListItem) {
                        viewer.setAttLabelType(LMOptions.ATTRIBUTES_ALL);
                    } else if (ae.getSource() == noObjectsItem) {
                        viewer.setObjLabelType(LMOptions.NO_LABEL);
                    } else if (ae.getSource() == objReducedItem) {
                        viewer.setObjLabelType(LMOptions.OBJECTS_REDUCED);
                    } else if (ae.getSource() == objListItem) {
                        viewer.setObjLabelType(LMOptions.OBJECTS_ALL);
                    } else if (ae.getSource() == objCountItem) {
                        viewer.setObjLabelType(LMOptions.OBJECTS_NUMBER);
                    } else if (ae.getSource() == objPercAllItem) {
                        viewer.setObjLabelType(LMOptions.OBJECTS_PERC_CTX);
                    } else if (ae.getSource() == objPercNodeItem) {
                        viewer.setObjLabelType(LMOptions.OBJECTS_PERC_NODE);
                    } else if (ae.getSource() == setRulesItem) {
                        hasSetRulesInLabels();
                        noRulesItem.setEnabled(true);
                        showRulesItem.setEnabled(true);
                        setRulesHidden(setRulesItem.isSelected());
                    } else if (ae.getSource() == noRulesItem) {
                        viewer.setRulesLabelType(LMOptions.NO_LABEL);
                    } else if (ae.getSource() == showRulesItem) {
                        viewer.setRulesLabelType(LMOptions.RULES_SHOW);
                    } else if (ae.getSource() == noGeneItem) {
                        viewer.setGeneLabelType(LMOptions.NO_LABEL);
                    } else if (ae.getSource() == showGeneItem) {
                        viewer.setGeneLabelType(LMOptions.GENE_SHOW);
                    } else if (ae.getSource() == zoomAnimItem) {
                        viewer.setAnimateZoom(zoomAnimItem.isSelected());
                    }
                    if (ae.getSource() == shakeAnimationItem) {
                        viewer.setAttentionFeatureType(LMOptions.SHAKE);
                    }
                    if (ae.getSource() == blinkAnimationItem) {
                        viewer.setAttentionFeatureType(LMOptions.BLINK);
                    }
                    if (ae.getSource() == noSearchAnimationItem) {
                        viewer.setAttentionFeatureType(LMOptions.NONE);
                    } else if (ae.getSource() == differentIntensityItem) {
                        viewer.setChangeColorIntensity(true);
                    } else if (ae.getSource() == sameIntensityItem) {
                        viewer.setChangeColorIntensity(false);
                    } else if (ae.getSource() == largeSizeItem) {
                        viewer.setConceptSizeType(LMOptions.LARGE);
                    } else if (ae.getSource() == smallSizeItem) {
                        viewer.setConceptSizeType(LMOptions.SMALL);
                    } else if (ae.getSource() == varySizeItem) {
                        viewer.setConceptSizeType(LMOptions.VARY);
                    } else if (ae.getSource() == undoOperation) {
                        backHistory();
                    } else if (ae.getSource() == redoOperation) {
                        forwardHistory();
                    } else if (ae.getSource() == zoomInItem) {
                        zoomIn();
                    } else if (ae.getSource() == zoomOutItem) {
                        zoomOut();
                    } else if (ae.getSource() == zoomSelectItem) {
                        zoomInSelectedArea();
                    } else if (ae.getSource() == zoomLatticeItem) {
                        showEntireLattice();
                    } else if (ae.getSource() == showAllLabelsItem) {
                        showAllLabels();
                    } else if (ae.getSource() == hideAllLabelsItem) {
                        hideAllLabels();
                    } else if (ae.getSource() == showAreaLabelsItem) {
                        showAreaLabels();
                    } else if (ae.getSource() == hideAreaLabelsItem) {
                        hideAreaLabels();
                    } else if (ae.getSource() == duplicateItem) {
                        duplicateViewer();
                    } else if (ae.getSource() == showBottomItem) {
                        setEmptyBottomHidden(!emptyBottomsHidden);
                    } else if (ae.getSource() == showLatticeMapItem) {
                        setLatticeMapHidden(!showLatticeMapItem.isSelected());
                    } else if (ae.getSource() == latticeStructureItem) {
                        openStructureViewer();
                    } else if (ae.getSource() == rulesItem) {
                        showRulePanel();
                    } else if (ae.getSource() == exportLattice) {
                        showCapturePanel();
                    } else if (ae.getSource() == quitFrame) {
                        setVisible(false);
                        dispose();
                    } else if (ae.getSource() == saveLattice) {
                        saveCurrentLatticeAs();
                    }
                } catch (LatticeMinerException error) {
                    DialogBox.showMessageError(viewer, error);
                }
            }
        }
    }

    private class ToolBarListener implements ActionListener {

        public void actionPerformed(ActionEvent ae) {
            try {
                if (ae.getSource() == previousBtn) {
                    backHistory();
                }
                if (ae.getSource() == nextBtn) {
                    forwardHistory();
                }
                if (ae.getSource() == zoomInBtn) {
                    zoomIn();
                } else if (ae.getSource() == zoomOutBtn) {
                    zoomOut();
                } else if (ae.getSource() == zoomAreaBtn) {
                    zoomInSelectedArea();
                } else if (ae.getSource() == noZoomBtn) {
                    showEntireLattice();
                } else if (ae.getSource() == modifyBtn) {
                    openStructureViewer();
                } else if (ae.getSource() == ruleBtn) {
                    showRulePanel();
                } else if (ae.getSource() == captureBtn) {
                    showCapturePanel();
                } else if (ae.getSource() == duplicateBtn) {
                    duplicateViewer();
                }
            } catch (LatticeMinerException error) {
                DialogBox.showMessageError(viewer, error);
            }
        }
    }

    public class SliderListener implements ChangeListener {

        public void stateChanged(ChangeEvent ce) {
            int value = textSize.getValue();
            viewer.setLabelSize(value);
            textSizeValue.setText(Integer.toString(value));
            viewer.repaint();
        }
    }

    /**
	 * @return the treePanel
	 */
    public Tree getTreePanel() {
        return treePanel;
    }

    /**
	 * @return the projectionPanel
	 */
    public Projection getProjectionPanel() {
        return projectionPanel;
    }

    /**
	 * @return the searchPanel
	 */
    public Search getSearchPanel() {
        return searchPanel;
    }
}

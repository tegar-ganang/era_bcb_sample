package net.sourceforge.ondex.util.metadata;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Stack;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.DataSource;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.ONDEXGraphMetaData;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.core.Unit;
import net.sourceforge.ondex.core.memory.MemoryONDEXGraphMetaData;
import net.sourceforge.ondex.tools.threading.monitoring.FailableThread;
import net.sourceforge.ondex.util.metadata.actions.OpenAction;
import net.sourceforge.ondex.util.metadata.elements.MEList;
import net.sourceforge.ondex.util.metadata.elements.MEPopupMenu;
import net.sourceforge.ondex.util.metadata.elements.MEProgressMonitor;
import net.sourceforge.ondex.util.metadata.elements.METree;
import net.sourceforge.ondex.util.metadata.model.MetaDataType;
import net.sourceforge.ondex.util.metadata.ops.Operation;
import net.sourceforge.ondex.util.metadata.ops.UpdateOperation;

/**
 * an internal frame containing the actual editing tools.
 * 
 * @author jweile
 * 
 */
public class MetaDataWindow extends JInternalFrame {

    private static final long serialVersionUID = 460714459427214930L;

    private ONDEXGraphMetaData md;

    private METree ccTree, rtTree, anTree;

    private MEList<DataSource> dataSourceList;

    private MEList<EvidenceType> evList;

    private MEList<Unit> uList;

    private EditorPanel<ConceptClass> ccEditorPanel;

    private EditorPanel<RelationType> rtEditorPanel;

    private EditorPanel<DataSource> dataSourceEditorPanel;

    private EditorPanel<EvidenceType> evEditorPanel;

    private EditorPanel<Unit> uEditorPanel;

    private EditorPanel<AttributeName> anEditorPanel;

    private static int nextId = 0;

    private final int id = nextId++;

    private boolean modified;

    private File file;

    private Stack<Operation<?>> actionStack = new Stack<Operation<?>>(), reversionStack = new Stack<Operation<?>>();

    /**
	 * constructor
	 * 
	 * @param metadata
	 *            the metadata object to associate with (can be null)
	 * @param file
	 *            the corresponding metadata xml file (can be null)
	 */
    public MetaDataWindow(ONDEXGraphMetaData metadata, File file) {
        super("MetaData" + (nextId + 1), true, true, true, true);
        if (file != null) {
            setTitle(file.getName().substring(0, file.getName().length() - 4));
            this.file = file;
        }
        if (metadata == null) {
            md = new MemoryONDEXGraphMetaData();
            md.createConceptClass("Thing", "Thing", "Root concept class", null);
            md.createRelationType("r", "is related to", "Root relation type", "is related to", false, false, true, false, null);
        } else {
            md = metadata;
        }
        formatData();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                close();
            }
        });
        setupContentPane();
        pack();
        setVisible(true);
    }

    /**
	 * formats and corrects errors in the data: Adds missing inheritance
	 * statements, creates missing base classes and trims text fields.
	 */
    private void formatData() {
        if (md != null) {
            ConceptClass ccThing = md.getConceptClass("Thing");
            if (ccThing == null) {
                ccThing = md.createConceptClass("Thing", "Thing", "All concepts", null);
            }
            Iterator<ConceptClass> ccs = md.getConceptClasses().iterator();
            while (ccs.hasNext()) {
                ConceptClass cc = ccs.next();
                if (cc.getSpecialisationOf() == null && !cc.equals(ccThing)) {
                    cc.setSpecialisationOf(ccThing);
                }
                cc.setFullname(trimLB(cc.getFullname()));
                cc.setDescription(trimLB(cc.getDescription()));
            }
            RelationType rtRelated = md.getRelationType("r");
            if (rtRelated == null) {
                rtRelated = md.createRelationType("r", "is related to", "Root relation type", "is related to", false, false, true, false, null);
            }
            Iterator<RelationType> rts = md.getRelationTypes().iterator();
            while (rts.hasNext()) {
                RelationType rt = rts.next();
                if (rt.getSpecialisationOf() == null && !rt.equals(rtRelated)) {
                    rt.setSpecialisationOf(rtRelated);
                }
                rt.setFullname(trimLB(rt.getFullname()));
                rt.setDescription(trimLB(rt.getDescription()));
                rt.setInverseName(trimLB(rt.getInverseName()));
            }
            AttributeName anGDS = md.getAttributeName("Attribute");
            if (anGDS == null) {
                anGDS = md.createAttributeName("Attribute", "Attribute", "General data store", null, Object.class, null);
            }
            Iterator<AttributeName> ans = md.getAttributeNames().iterator();
            while (ans.hasNext()) {
                AttributeName an = ans.next();
                if (an.getSpecialisationOf() == null && !an.equals(anGDS)) {
                    an.setSpecialisationOf(anGDS);
                }
                an.setFullname(trimLB(an.getFullname()));
                an.setDescription(trimLB(an.getDescription()));
            }
            DataSource dataSourceUnknown = md.getDataSource("unknown");
            if (dataSourceUnknown == null) {
                dataSourceUnknown = md.createDataSource("unknown", "unknown", "unknown data source");
            }
            Iterator<DataSource> cvs = md.getDataSources().iterator();
            while (cvs.hasNext()) {
                DataSource dataSource = cvs.next();
                dataSource.setFullname(trimLB(dataSource.getFullname()));
                dataSource.setDescription(trimLB(dataSource.getDescription()));
            }
            EvidenceType etIMPD = md.getEvidenceType("IMPD");
            if (etIMPD == null) {
                etIMPD = md.createEvidenceType("IMPD", "Imported", "Imported");
            }
            Iterator<EvidenceType> evs = md.getEvidenceTypes().iterator();
            while (evs.hasNext()) {
                EvidenceType ev = evs.next();
                ev.setFullname(trimLB(ev.getFullname()));
                ev.setDescription(trimLB(ev.getDescription()));
            }
            Unit usec = md.getUnit("second");
            if (usec == null) {
                usec = md.createUnit("second", "second", "SI BASE Unit of time");
            }
            Iterator<Unit> us = md.getUnits().iterator();
            while (us.hasNext()) {
                Unit u = us.next();
                u.setFullname(trimLB(u.getFullname()));
                u.setDescription(trimLB(u.getDescription()));
            }
        }
    }

    /**
	 * an improved String trimming methods that also removes leading and
	 * trailing newlines as well as randomly inserted tabs.
	 * 
	 * @param s
	 * @return
	 */
    private String trimLB(String s) {
        if (s == null || s.equals("")) {
            return s;
        }
        char[] chars = s.toCharArray();
        int start = 0, end = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                continue;
            } else {
                start = i;
                break;
            }
        }
        for (int i = chars.length - 1; i >= 0; i--) {
            char c = chars[i];
            if (c == ' ' || c == '\r' || c == '\n' || c == '\t') {
                continue;
            } else {
                end = i;
                break;
            }
        }
        int newLength = (end - start) + 1;
        char[] charsNew = new char[newLength];
        System.arraycopy(chars, start, charsNew, 0, newLength);
        String s2 = new String(charsNew);
        return s2.replaceAll("\t", "");
    }

    /**
	 * sets up the main content
	 */
    private void setupContentPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane);
        tabbedPane.addTab("Concept classes", setupConceptClassPanel());
        tabbedPane.addTab("Relation types", setupRelationTypePanel());
        tabbedPane.addTab("Attribute names", setupAttributeNamePanel());
        tabbedPane.addTab("Data sources", setupCVPanel());
        tabbedPane.addTab("Evidence types", setupETPanel());
        tabbedPane.addTab("Units", setupUnitPanel());
        tabbedPane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                updateAll();
            }
        });
    }

    /**
	 * sets up the tab containing the relation type editor.
	 * 
	 * @return
	 */
    private Component setupRelationTypePanel() {
        RelationType rtRelated = md.getRelationType("r");
        rtTree = METree.forMetaData(md, MetaDataType.RELATION_TYPE);
        rtEditorPanel = new EditorPanel<RelationType>(rtRelated, md);
        rtTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                RelationType rt = (RelationType) node.getUserObject();
                rtEditorPanel.setContents(rt);
            }
        });
        rtTree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    TreePath path = rtTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        rtTree.setSelectionPath(path);
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();
                        boolean root = ((RelationType) n.getUserObject()).getId().equals("r");
                        MEPopupMenu<RelationType> popup = new MEPopupMenu<RelationType>(MetaDataType.RELATION_TYPE, rtTree, n, rtEditorPanel, root);
                        popup.show(rtTree, e.getX(), e.getY());
                    }
                }
            }
        });
        rtTree.addKeyListener(new KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                System.out.println("key typed");
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                }
            }
        });
        rtEditorPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateOperation<RelationType> a = rtEditorPanel.extractChangeAction();
                if (a != null) {
                    performOperation(a);
                    rtTree.getModel().valueForPathChanged(rtTree.getSelectionPath(), rtEditorPanel.getContents());
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(rtTree));
        splitPane.setRightComponent(rtEditorPanel);
        return splitPane;
    }

    /**
	 * sets up the tab containing the concept class editor.
	 * 
	 * @return
	 */
    private Component setupConceptClassPanel() {
        ConceptClass ccThing = md.getConceptClass("Thing");
        ccTree = METree.forMetaData(md, MetaDataType.CONCEPT_CLASS);
        ccEditorPanel = new EditorPanel<ConceptClass>(ccThing, md);
        ccTree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                ConceptClass cc = (ConceptClass) node.getUserObject();
                ccEditorPanel.setContents(cc);
            }
        });
        ccTree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    TreePath path = ccTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        ccTree.setSelectionPath(path);
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();
                        boolean root = ((ConceptClass) n.getUserObject()).getId().equals("Thing");
                        MEPopupMenu<ConceptClass> popup = new MEPopupMenu<ConceptClass>(MetaDataType.CONCEPT_CLASS, ccTree, n, ccEditorPanel, root);
                        popup.show(ccTree, e.getX(), e.getY());
                    }
                }
            }
        });
        ccTree.addKeyListener(new KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                }
            }
        });
        ccEditorPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateOperation<ConceptClass> a = ccEditorPanel.extractChangeAction();
                if (a != null) {
                    performOperation(a);
                    ccTree.getModel().valueForPathChanged(ccTree.getSelectionPath(), ccEditorPanel.getContents());
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(ccTree));
        splitPane.setRightComponent(ccEditorPanel);
        return splitPane;
    }

    private Component setupCVPanel() {
        DataSource dataSourceUnknown = md.getDataSource("unknown");
        dataSourceList = new MEList<DataSource>(MetaDataType.CV, md, "Data sources");
        dataSourceEditorPanel = new EditorPanel<DataSource>(dataSourceUnknown, md);
        dataSourceList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!(e.getValueIsAdjusting() || dataSourceList.getSelectionModel().getMinSelectionIndex() < 0)) {
                    int index = dataSourceList.convertRowIndexToModel(dataSourceList.getSelectionModel().getMinSelectionIndex());
                    DataSource dataSource = dataSourceList.getMDListModel().getMetaDataAt(index);
                    dataSourceEditorPanel.setContents(dataSource);
                }
            }
        });
        dataSourceList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int index = dataSourceList.rowAtPoint(e.getPoint());
                    dataSourceList.getSelectionModel().setSelectionInterval(index, index);
                    MEPopupMenu<DataSource> popup = new MEPopupMenu<DataSource>(MetaDataType.CV, dataSourceList, index, dataSourceEditorPanel);
                    popup.show(dataSourceList, e.getX(), e.getY());
                }
            }
        });
        dataSourceList.addKeyListener(new KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                }
            }
        });
        dataSourceEditorPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateOperation<DataSource> a = dataSourceEditorPanel.extractChangeAction();
                if (a != null) {
                    performOperation(a);
                    int index = dataSourceList.convertRowIndexToModel(dataSourceList.getSelectionModel().getMinSelectionIndex());
                    dataSourceList.getMDListModel().update(index);
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(dataSourceList));
        splitPane.setRightComponent(dataSourceEditorPanel);
        return splitPane;
    }

    /**
	 * sets up the evidence type editor
	 * 
	 * @return
	 */
    private Component setupETPanel() {
        EvidenceType etIMPD = md.getEvidenceType("IMPD");
        evList = new MEList<EvidenceType>(MetaDataType.EVIDENCE_TYPE, md, "Evidence types");
        evEditorPanel = new EditorPanel<EvidenceType>(etIMPD, md);
        evList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!(e.getValueIsAdjusting() || evList.getSelectionModel().getMinSelectionIndex() < 0)) {
                    int index = evList.convertRowIndexToModel(evList.getSelectionModel().getMinSelectionIndex());
                    EvidenceType ev = evList.getMDListModel().getMetaDataAt(index);
                    evEditorPanel.setContents(ev);
                }
            }
        });
        evList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int index = evList.rowAtPoint(e.getPoint());
                    evList.getSelectionModel().setSelectionInterval(index, index);
                    MEPopupMenu<EvidenceType> popup = new MEPopupMenu<EvidenceType>(MetaDataType.EVIDENCE_TYPE, evList, index, evEditorPanel);
                    popup.show(evList, e.getX(), e.getY());
                }
            }
        });
        evList.addKeyListener(new KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                }
            }
        });
        evEditorPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateOperation<EvidenceType> a = evEditorPanel.extractChangeAction();
                if (a != null) {
                    performOperation(a);
                    int index = evList.convertRowIndexToModel(evList.getSelectionModel().getMinSelectionIndex());
                    evList.getMDListModel().update(index);
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(evList));
        splitPane.setRightComponent(evEditorPanel);
        return splitPane;
    }

    /**
	 * sets up the unit editor.
	 * 
	 * @return
	 */
    private Component setupUnitPanel() {
        Unit u = md.getUnit("second");
        uList = new MEList<Unit>(MetaDataType.UNIT, md, "Units");
        uEditorPanel = new EditorPanel<Unit>(u, md);
        uList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!(e.getValueIsAdjusting() || uList.getSelectionModel().getMinSelectionIndex() < 0)) {
                    int index = uList.convertRowIndexToModel(uList.getSelectionModel().getMinSelectionIndex());
                    Unit u1 = uList.getMDListModel().getMetaDataAt(index);
                    uEditorPanel.setContents(u1);
                }
            }
        });
        uList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int index = uList.rowAtPoint(e.getPoint());
                    uList.getSelectionModel().setSelectionInterval(index, index);
                    MEPopupMenu<Unit> popup = new MEPopupMenu<Unit>(MetaDataType.UNIT, uList, index, uEditorPanel);
                    popup.show(uList, e.getX(), e.getY());
                }
            }
        });
        uList.addKeyListener(new KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                }
            }
        });
        uEditorPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateOperation<Unit> a = uEditorPanel.extractChangeAction();
                if (a != null) {
                    performOperation(a);
                    int index = evList.convertRowIndexToModel(uList.getSelectionModel().getMinSelectionIndex());
                    uList.getMDListModel().update(index);
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(uList));
        splitPane.setRightComponent(uEditorPanel);
        return splitPane;
    }

    /**
	 * sets up the attribute name editor.
	 * 
	 * @return
	 */
    private Component setupAttributeNamePanel() {
        AttributeName anRoot = md.getAttributeName("Attribute");
        anTree = METree.forMetaData(md, MetaDataType.ATTRIBUTE_NAME);
        anEditorPanel = new EditorPanel<AttributeName>(anRoot, md);
        anTree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                AttributeName an = (AttributeName) node.getUserObject();
                anEditorPanel.setContents(an);
            }
        });
        anTree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    TreePath path = anTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        anTree.setSelectionPath(path);
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();
                        boolean root = ((AttributeName) n.getUserObject()).getId().equals("Attribute");
                        MEPopupMenu<AttributeName> popup = new MEPopupMenu<AttributeName>(MetaDataType.ATTRIBUTE_NAME, anTree, n, anEditorPanel, root);
                        popup.show(anTree, e.getX(), e.getY());
                    }
                }
            }
        });
        anTree.addKeyListener(new KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                }
            }
        });
        anEditorPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateOperation<AttributeName> a = anEditorPanel.extractChangeAction();
                if (a != null) {
                    performOperation(a);
                    anTree.getModel().valueForPathChanged(anTree.getSelectionPath(), anEditorPanel.getContents());
                }
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(anTree));
        splitPane.setRightComponent(anEditorPanel);
        return splitPane;
    }

    /**
	 * returns the metadata object
	 * 
	 * @return
	 */
    public ONDEXGraphMetaData getMetaData() {
        return md;
    }

    /**
	 * performs a given operation and registers it with the action stack.
	 * 
	 * @param a
	 */
    public void performOperation(Operation<?> a) {
        a.perform();
        actionStack.push(a);
        setModified(true);
        MetaDataEditor.getInstance().setActionEnabled(new String[] { "Edit", "Undo" }, true);
    }

    /**
	 * undos a last performed action from the action stack.
	 */
    public void undo() {
        if (actionStack.size() > 0) {
            Operation<?> a = actionStack.pop();
            a.revert();
            reversionStack.push(a);
            updateAll();
            MetaDataEditor.getInstance().setActionEnabled(new String[] { "Edit", "Redo" }, true);
            if (actionStack.size() == 0) {
                MetaDataEditor.getInstance().setActionEnabled(new String[] { "Edit", "Undo" }, false);
            }
        }
    }

    /**
	 * redos the last undone action from the reversion stack.
	 */
    public void redo() {
        if (reversionStack.size() > 0) {
            Operation<?> a = reversionStack.pop();
            a.perform();
            actionStack.push(a);
            updateAll();
            MetaDataEditor.getInstance().setActionEnabled(new String[] { "Edit", "Undo" }, true);
            if (reversionStack.size() == 0) {
                MetaDataEditor.getInstance().setActionEnabled(new String[] { "Edit", "Redo" }, false);
            }
        }
    }

    /**
	 * updates all editor elements.
	 */
    public void updateAll() {
        ccEditorPanel.update();
        ccTree.updateAll();
        rtEditorPanel.update();
        rtTree.updateAll();
        dataSourceEditorPanel.update();
        dataSourceList.getMDListModel().update();
        evEditorPanel.update();
        evList.getMDListModel().update();
        uEditorPanel.update();
        uList.getMDListModel().update();
        anEditorPanel.update();
        anTree.updateAll();
    }

    /**
	 * returns this frame's id.
	 * 
	 * @return
	 */
    public long id() {
        return id;
    }

    /**
	 * closes this frame
	 * 
	 * @return whether the frame has really been closed.
	 */
    public boolean close() {
        if (modified) {
            Object[] options = { "Yes", "No", "Cancel" };
            int retVal = JOptionPane.showOptionDialog(this, "Do you want to save before closing?", "Save before closing?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (retVal == JOptionPane.YES_OPTION) {
                save();
                dispose();
                return true;
            } else if (retVal == JOptionPane.NO_OPTION) {
                dispose();
                return true;
            }
        } else {
            dispose();
            return true;
        }
        return false;
    }

    /**
	 * overrides the dispose method to terminate associated threads before
	 * disposing.
	 * 
	 * @see javax.swing.JInternalFrame#dispose()
	 */
    public void dispose() {
        ccEditorPanel.close();
        rtEditorPanel.close();
        dataSourceEditorPanel.close();
        evEditorPanel.close();
        uEditorPanel.close();
        anEditorPanel.close();
        super.dispose();
    }

    /**
	 * adds missing file type tags to a file.
	 */
    private void correctFileName() {
        if (file != null) {
            if (!file.getName().endsWith(".xml")) {
                file = new File(file.getAbsolutePath() + ".xml");
            }
        }
    }

    /**
	 * saves the frame's metadata object. queries the user for a file first.
	 */
    public void saveAs() {
        int retval = OpenAction.fc.showSaveDialog(this);
        if (retval == JFileChooser.APPROVE_OPTION) {
            file = OpenAction.fc.getSelectedFile();
            correctFileName();
            save();
            setTitle(file.getName().substring(0, file.getName().length() - 4));
        }
    }

    /**
	 * saves the frames's metadata object.
	 */
    public void save() {
        if (file == null) {
            saveAs();
        } else {
            final MetaDataWriter w = new MetaDataWriter(file, md);
            FailableThread t = new FailableThread("File writer thread") {

                @Override
                public void failableRun() throws Throwable {
                    try {
                        w.write();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    setModified(false);
                }
            };
            t.start();
            MEProgressMonitor.start("Saving to file", w);
        }
    }

    /**
	 * sets the modified flag and visualises it.
	 * 
	 * @param m
	 */
    private void setModified(boolean m) {
        if (!modified && m) {
            modified = true;
            setTitle("*" + getTitle());
        } else if (modified && !m) {
            modified = false;
            setTitle(getTitle().substring(1, getTitle().length()));
        }
    }
}

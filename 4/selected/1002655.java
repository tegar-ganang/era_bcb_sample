package ingenias.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Hashtable;
import ingenias.editor.extension.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import javax.swing.event.UndoableEditEvent;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgraph.event.*;
import java.util.Vector;
import org.jgraph.JGraph;
import org.jgraph.graph.*;
import org.jgraph.event.*;
import org.jgraph.plaf.basic.*;
import ingenias.editor.entities.*;
import java.io.*;
import ingenias.editor.persistence.*;
import java.awt.image.*;
import java.awt.datatransfer.*;
import java.awt.geom.Rectangle2D;
import ingenias.editor.cell.*;
import ingenias.exception.CannotLoad;
import ingenias.exception.NotInitialised;
import ingenias.exception.NullEntity;
import ingenias.exception.UnknowFormat;
import ingenias.generator.browser.BrowserImp;
import ingenias.generator.browser.Graph;
import ingenias.generator.browser.GraphEntity;

public abstract class IDEAbs extends ingenias.editor.IDEGUI implements java.io.Serializable, ClipboardOwner {

    public IDEState ids;

    private File currentFile;

    /**
	 *  Description of the Field
	 */
    public static File currentImageFolder;

    /**
	 *  Description of the Field
	 */
    private static File currentFileFolder;

    public static IDEAbs ide = null;

    public static boolean changes = false;

    protected Vector lastFiles = new Vector();

    protected Vector<TreePath> foundpaths = new Vector<TreePath>();

    protected int lastFoundIndex = 0;

    protected String lastSearch = "";

    protected boolean relationshipTyping = false;

    private AutomaticBackup abackup = null;

    public Preferences prefs = new Preferences();

    private static long initTime = System.currentTimeMillis();

    public static void setRelationshipTyping(boolean value) {
        ide.ids.editor.repaint();
        ide.relationshipTyping = value;
    }

    public static boolean getRelationshipTyping() {
        return ide.relationshipTyping;
    }

    public Vector getLastFiles() {
        return lastFiles;
    }

    /**
	 *  Sets the changed attribute of the IDE class
	 */
    public static void setChanged() {
        if (ide != null) {
            changes = true;
            ide.save.setEnabled(true);
            ide.saveas.setEnabled(true);
        }
    }

    /**
	 *  Sets the unChanged attribute of the IDE class
	 */
    public static void setUnChanged() {
        if (ide != null) {
            changes = false;
            ide.save.setEnabled(false);
            ide.saveas.setEnabled(true);
        }
    }

    /**
	 *  Constructor for the IDE object
	 */
    public IDEAbs() {
        super();
        JFileChooser jfc = new JFileChooser();
        File homedir = jfc.getCurrentDirectory();
        new File(homedir.getPath() + "/.idk").mkdir();
        ide = this;
        ids = new IDEState(null, this.rootObjetos, this.arbolObjetos, this.rootProject, this.arbolProyectos);
        ids.editor = new Editor(ids.om);
        pprin.add(ids.editor, java.awt.BorderLayout.CENTER);
        this.arbolProyectos.setCellRenderer(new ProjectTreeRenderer());
        this.arbolObjetos.setCellRenderer(new ProjectTreeRenderer());
        ids.editor.setEnabled(false);
        ids.prop = new Properties();
        ids.prop.put("IDK:extfolder", new ProjectProperty("IDK", "extfolder", "Extension Module Folder", "ext", "Folder where the IDE will find its new modules"));
        Log.initInstance(new PrintWriter(new TextAreaOutputStream(this.moduleOutput)), new PrintWriter(new TextAreaOutputStream(this.logs)));
        try {
            ingenias.generator.browser.BrowserImp.initialise();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ModuleLoader.cleanExtensionFolder();
        UpdateToolsAndCG update = new UpdateToolsAndCG(this);
        update.readLibs("ext");
        update.start();
        updateProjectsMenu(project);
        setUnChanged();
        this.validate();
        this.pack();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setVisible(true);
        JOptionPane.setRootFrame(this);
        this.currentFile = null;
        HyperlinkListener diagramLocator = new HyperlinkListener() {

            private String lastScrolledEntity = "";

            private int lastScrolledIndex = 0;

            public void hyperlinkUpdate(HyperlinkEvent e) {
                try {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        JEditorPane pane = (JEditorPane) e.getSource();
                        URL url = e.getURL();
                        if (url != null) {
                            if (url.getHost().equals("app")) {
                                String completePath = url.getPath().substring(1);
                                String diagramPath = null;
                                String entityPath = null;
                                if (completePath.indexOf("/") > -1) {
                                    diagramPath = completePath.substring(0, completePath.indexOf("/"));
                                    entityPath = completePath.substring(completePath.indexOf("/") + 1, completePath.length());
                                } else diagramPath = completePath;
                                Graph g = null;
                                g = BrowserImp.getInstance().getGraph(diagramPath);
                                ids.editor.changeGraph(g.getGraph());
                                if (entityPath != null) {
                                    DefaultGraphCell dgc = null;
                                    g.getGraph().clearSelection();
                                    Vector<DefaultGraphCell> dgcs = new Vector<DefaultGraphCell>();
                                    for (int j = 0; j < g.getGraph().getModel().getRootCount(); j++) {
                                        dgc = (DefaultGraphCell) g.getGraph().getModel().getRootAt(j);
                                        if (dgc.getUserObject() instanceof Entity) {
                                            Entity ent = (Entity) dgc.getUserObject();
                                            if (ent.getId().equals(entityPath)) {
                                                g.getGraph().addSelectionCell(dgc);
                                                dgcs.add(dgc);
                                            }
                                        }
                                        ;
                                    }
                                    if (dgc != null) if (lastScrolledEntity.equals(entityPath) && lastScrolledIndex < dgcs.size()) {
                                        g.getGraph().scrollCellToVisible(dgcs.elementAt(lastScrolledIndex));
                                        lastScrolledIndex = (lastScrolledIndex + 1) % dgcs.size();
                                    } else {
                                        lastScrolledEntity = entityPath;
                                        lastScrolledIndex = 0;
                                        g.getGraph().scrollCellToVisible(dgcs.elementAt(lastScrolledIndex));
                                    }
                                }
                            } else if (url.getHost().equals("ent")) {
                                String entity = url.getFile().substring(1);
                                Vector userobject = ids.om.findUserObject(entity);
                                if (userobject.size() == 0) {
                                    userobject = new Vector();
                                    Graph[] graphs = BrowserImp.getInstance().getGraphs();
                                    for (int k = 0; k < graphs.length; k++) {
                                        for (int j = 0; j < graphs[k].getGraph().getModel().getRootCount(); j++) {
                                            DefaultGraphCell dgc = (DefaultGraphCell) graphs[k].getGraph().getModel().getRootAt(j);
                                            if (dgc.getUserObject() instanceof Entity) {
                                                Entity ent = (Entity) dgc.getUserObject();
                                                if (ent.getId().equals(entity)) {
                                                    userobject.add(ent);
                                                    graphs[k].getGraph().setSelectionCell(dgc);
                                                    graphs[k].getGraph().scrollCellToVisible(dgc);
                                                }
                                            }
                                            ;
                                        }
                                    }
                                } else {
                                    locateAndScrollToObject(entity);
                                }
                            }
                        }
                    }
                } catch (NotInitialised e1) {
                    e1.printStackTrace();
                }
            }
        };
        searchDiagramPanel.addHyperlinkListener(diagramLocator);
        logs.addHyperlinkListener(diagramLocator);
        this.abackup = new AutomaticBackup(ide, 5);
        try {
            restorePreferences();
        } catch (UnknowFormat e1) {
            e1.printStackTrace();
        } catch (CannotLoad e1) {
            e1.printStackTrace();
        }
    }

    /**
	 * @throws UnknowFormat
	 * @throws CannotLoad
	 */
    private void restorePreferences() throws UnknowFormat, CannotLoad {
        new PersistenceManager().restorePreferences(this);
        if (currentFile != null) {
            currentFileFolder = currentFile.getParentFile();
        }
        if (prefs.getEditPropertiesMode().equals(Preferences.EditPropertiesMode.PANEL)) {
            editOnMessages.setSelected(true);
        }
        if (prefs.getEditPropertiesMode().equals(Preferences.EditPropertiesMode.POPUP)) {
            editPopUpProperties.setSelected(true);
        }
        if (prefs.getRelationshiplayout().equals(Preferences.RelationshipLayout.MANUAL)) {
            ids.editor.getJC().setSelectedIndex(1);
        }
        if (prefs.getRelationshiplayout().equals(Preferences.RelationshipLayout.AUTOMATIC)) {
            ids.editor.getJC().setSelectedIndex(0);
        }
        if (prefs.getModelingLanguage().equals(Preferences.ModelingLanguage.INGENIAS)) {
            enableINGENIASView_actionPerformed(null);
            enableINGENIASView.setSelected(true);
        }
        if (prefs.getModelingLanguage().equals(Preferences.ModelingLanguage.UML)) {
            enableUMLView_actionPerformed(null);
            enableUMLView.setSelected(true);
        }
        if (prefs.getRelationshipsLookAndFeel().equals(Preferences.RelationshipsLookAndFeel.FULL)) {
        }
    }

    private void updateProjectsMenu(JMenu project) {
    }

    public void updateHistory(final File f) {
        if (!this.lastFiles.contains(f)) {
            if (this.lastFiles.size() > 5) {
                this.lastFiles.remove(0);
            }
        } else {
            this.lastFiles.remove(f);
        }
        this.lastFiles.add(f);
        Component[] me = this.file.getMenuComponents();
        for (int k = 0; k < me.length; k++) {
            if (me[k] instanceof VisitedFileMenuItem) {
                this.file.remove(me[k]);
            }
        }
        final IDEAbs _ide = this;
        for (int k = 0; k < this.lastFiles.size(); k++) {
            final File current = (File) this.lastFiles.elementAt(k);
            if (current != null) {
                VisitedFileMenuItem vfmi = new VisitedFileMenuItem(current.getPath(), current);
                vfmi.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        int result = JOptionPane.OK_OPTION;
                        if (changes) {
                            result = JOptionPane.showConfirmDialog(_ide, "You will loose current data. Do you want to continue (y/n)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        }
                        if (result == JOptionPane.OK_OPTION) {
                            final File currentf = current;
                            new Thread() {

                                public void run() {
                                    loadFile(currentf);
                                }
                            }.start();
                        }
                    }
                });
                this.file.add(vfmi);
            }
        }
    }

    private void updateProperties(Properties extprop) {
        Enumeration enumeration = extprop.keys();
        while (enumeration.hasMoreElements()) {
            Object key = enumeration.nextElement();
            if (!this.ids.prop.containsKey(key)) {
                this.ids.prop.put(key, extprop.get(key));
            }
        }
    }

    protected void addToolEntry(ingenias.editor.extension.BasicTool bt) {
        Log.getInstance().logSYS("Added new module with name \"" + bt.getName() + "\"");
        JMenuItem nentry = new JMenuItem(bt.getName());
        nentry.setToolTipText(bt.getDescription());
        tools.add(nentry);
        this.updateProperties(bt.getProperties());
        final BasicTool bt1 = bt;
        final JFrame jf = this;
        nentry.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jf.setEnabled(false);
                final JWindow jw = showMessageWindow("Running " + bt1.getName());
                Thread t = new Thread() {

                    public void run() {
                        while (!jw.isVisible()) {
                            Thread.currentThread().yield();
                        }
                        bt1.setProperties(ids.prop);
                        int hclogs = ide.logs.getText().hashCode();
                        int hcout = ide.moduleOutput.getText().hashCode();
                        try {
                            bt1.run();
                            AudioPlayer.play("arpa.wav");
                            if (ide.moduleOutput.getText().hashCode() != hcout) {
                                ide.messagespane.setSelectedIndex(1);
                            }
                            if (ide.logs.getText().hashCode() != hclogs) {
                                ide.messagespane.setSelectedIndex(0);
                            }
                            jf.setEnabled(true);
                            jw.setVisible(false);
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            jf.setEnabled(true);
                            jw.hide();
                            Log.getInstance().logERROR(ex);
                            StackTraceElement[] ste = ex.getStackTrace();
                            for (int k = 0; k < ste.length; k++) {
                                Log.getInstance().logERROR(ste[k].toString());
                            }
                            AudioPlayer.play("watershot.wav");
                            if (ide.moduleOutput.getText().hashCode() != hcout) {
                                ide.messagespane.setSelectedIndex(1);
                            }
                            if (ide.logs.getText().hashCode() != hclogs) {
                                ide.messagespane.setSelectedIndex(0);
                            }
                        }
                        System.gc();
                    }
                };
                t.start();
            }
        });
    }

    protected void removeEntry(ingenias.editor.extension.BasicTool bt) {
        int k = 0;
        boolean found = false;
        JMenu entry = null;
        while (!found && k < codeGenerator.getItemCount()) {
            entry = (JMenu) codeGenerator.getItem(k);
            found = entry.getText().equals(bt.getName());
            if (!found) {
                k = k + 1;
            }
        }
        ;
        if (found) {
            codeGenerator.remove(k);
        }
        found = false;
        JMenuItem tentry = null;
        k = 0;
        while (!found && k < tools.getItemCount()) {
            tentry = (JMenuItem) tools.getItem(k);
            found = tentry.getText().equals(bt.getName());
            if (!found) {
                k = k + 1;
            }
        }
        ;
        if (found) {
            tools.remove(k);
        }
    }

    protected void addCGEntry(ingenias.editor.extension.BasicCodeGenerator bcg) {
        Log.getInstance().logSYS("Added new module with name \"" + bcg.getName() + "\"");
        JMenu nentry = new JMenu(bcg.getName());
        JMenuItem generate = new JMenuItem("generate");
        JMenuItem verify = new JMenuItem("verify");
        nentry.add(generate);
        nentry.add(verify);
        nentry.setToolTipText(bcg.getDescription());
        codeGenerator.add(nentry);
        this.updateProperties(bcg.getProperties());
        final BasicCodeGenerator bcg1 = bcg;
        final JFrame jf = this;
        generate.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jf.setEnabled(false);
                bcg1.setProperties(ids.prop);
                final JWindow jw = showMessageWindow("Running " + bcg1.getName());
                Thread t = new Thread() {

                    public void run() {
                        while (!jw.isVisible()) {
                            Thread.currentThread().yield();
                        }
                        int hclogs = ide.logs.getText().hashCode();
                        int hcout = ide.moduleOutput.getText().hashCode();
                        try {
                            boolean result = bcg1.verify();
                            if (!result) {
                                AudioPlayer.play("watershot.wav");
                            } else {
                                bcg1.run();
                                AudioPlayer.play("arpa.wav");
                            }
                            jw.setVisible(false);
                            jf.setEnabled(true);
                            if (ide.moduleOutput.getText().hashCode() != hcout) {
                                ide.messagespane.setSelectedIndex(1);
                            }
                            if (ide.logs.getText().hashCode() != hclogs) {
                                ide.messagespane.setSelectedIndex(0);
                            }
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                            jf.setEnabled(true);
                            jw.setVisible(false);
                            Log.getInstance().logERROR(ex);
                            StackTraceElement[] ste = ex.getStackTrace();
                            for (int k = 0; k < ste.length; k++) {
                                Log.getInstance().logERROR(ste[k].toString());
                            }
                            AudioPlayer.play("watershot.wav");
                            if (ide.moduleOutput.getText().hashCode() != hcout) {
                                ide.messagespane.setSelectedIndex(1);
                            }
                            if (ide.logs.getText().hashCode() != hclogs) {
                                ide.messagespane.setSelectedIndex(0);
                            }
                        }
                        System.gc();
                    }
                };
                t.start();
            }
        });
        verify.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                bcg1.setProperties(ids.prop);
                jf.setEnabled(false);
                final JWindow jw = showMessageWindow("Verifying with " + bcg1.getName());
                new Thread() {

                    public void run() {
                        while (!jw.isVisible()) {
                            Thread.currentThread().yield();
                        }
                        boolean result = bcg1.verify();
                        jw.setVisible(false);
                        if (!result) {
                            AudioPlayer.play("watershot.wav");
                        } else {
                            AudioPlayer.play("arpa.wav");
                            Log.getInstance().logSYS("Specification is correct");
                        }
                        jf.setEnabled(true);
                    }
                }.start();
            }
        });
    }

    Point getCenter(Dimension size) {
        Dimension d = this.getSize();
        Point result = new Point((d.width / 2 - size.width / 2) + this.getLocation().x, (d.height / 2 - size.height / 2) + this.getLocation().y);
        return result;
    }

    /**
	 *  Description of the Method
	 *
	 *@param  e  Description of Parameter
	 */
    void arbolProyectos_mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu jp = this.menuProjectTree(e);
            TreePath tp = ids.gm.arbolProyecto.getSelectionPath();
            if (tp != null) {
                e.translatePoint(0, 0);
                jp.show(ids.gm.arbolProyecto, e.getPoint().x, e.getPoint().y);
                this.repaint();
            }
        } else if (e.getClickCount() > 1) {
            TreePath tp = ids.gm.arbolProyecto.getSelectionPath();
            if (tp != null) {
                DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();
                Object uo = dmtn.getUserObject();
                if (tp != null && tp.getPathCount() > 1 && uo instanceof ModelJGraph) {
                    ModelJGraph m = (ModelJGraph) uo;
                    if (m != null) {
                        ids.gm.setCurrent(m);
                        ids.editor.changeGraph(m);
                        ids.editor.validate();
                        ids.editor.repaint();
                    }
                }
            }
        }
    }

    protected abstract JPopupMenu menuProjectTree(MouseEvent e);

    void arbolObjetos_mouseClicked(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu jp = this.menuObjectTree(e);
            TreePath tp = ids.om.arbolObjetos.getSelectionPath();
            if (tp != null) {
                jp.show(arbolObjetos, e.getPoint().x, e.getPoint().y);
                this.repaint();
            }
        } else if (e.getClickCount() > 1) {
            TreePath tp = ids.om.arbolObjetos.getSelectionPath();
            if (tp != null && tp.getPathCount() > 1) {
                JGraph jg = ids.gm.getCurrent();
                javax.swing.tree.DefaultMutableTreeNode dmtn = (javax.swing.tree.DefaultMutableTreeNode) tp.getLastPathComponent();
                this.getContentPane().validate();
            }
        }
    }

    public JPopupMenu menuObjectTree(MouseEvent me1) {
        JPopupMenu menu = new JPopupMenu();
        final MouseEvent me = me1;
        final TreePath tp = ids.om.arbolObjetos.getSelectionPath();
        final TreePath[] tps = ids.om.arbolObjetos.getSelectionPaths();
        if (tp != null && ids.gm.getModel(tp.getPath()) == null) {
            JGraph jg = ids.gm.getCurrent();
            final javax.swing.tree.DefaultMutableTreeNode dmtn = (javax.swing.tree.DefaultMutableTreeNode) tp.getLastPathComponent();
            final IDEAbs ide = this;
            if (dmtn != null && dmtn.getUserObject() instanceof Entity) {
                menu.add(new AbstractAction("Add to current graph") {

                    public void actionPerformed(ActionEvent e) {
                        Entity sel = (Entity) dmtn.getUserObject();
                        ids.editor.insertDuplicated(new Point(0, 0), sel);
                        setChanged();
                    }
                });
                menu.add(new AbstractAction("Edit") {

                    public void actionPerformed(ActionEvent e) {
                        Entity sel = (Entity) dmtn.getUserObject();
                        ingenias.editor.editiondialog.GeneralEditionFrame jf = new ingenias.editor.editiondialog.GeneralEditionFrame(ids.editor, ids.om, ide, "Edit " + sel.getId(), sel);
                        jf.setLocation(getCenter(jf.getSize()));
                        jf.pack();
                        jf.show();
                        repaint();
                        setChanged();
                    }
                });
                menu.add(new AbstractAction("Remove") {

                    public void actionPerformed(ActionEvent e) {
                        for (int k = 0; k < tps.length; k++) {
                            javax.swing.tree.DefaultMutableTreeNode dmtn = (javax.swing.tree.DefaultMutableTreeNode) tps[k].getLastPathComponent();
                            if (dmtn != null && dmtn.getUserObject() instanceof Entity) {
                                int result = JOptionPane.showConfirmDialog(ide, "This will remove permanently " + tps[k].getLastPathComponent() + ". Are you sure?", "removing object", JOptionPane.YES_NO_OPTION);
                                if (result == JOptionPane.OK_OPTION) {
                                    Entity sel = (Entity) dmtn.getUserObject();
                                    ids.om.removeEntity(sel);
                                    ids.gm.removeEntityFromAllGraphs(sel);
                                    repaint();
                                    setChanged();
                                }
                            }
                        }
                    }
                });
                menu.add(new AbstractAction("Search occurrences") {

                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (dmtn.getUserObject() instanceof Entity) {
                                Entity ent = (Entity) dmtn.getUserObject();
                                StringBuffer result = new StringBuffer();
                                result.append("Diagrams found:<ul>");
                                Graph[] graphs = BrowserImp.getInstance().getGraphs();
                                for (int k = 0; k < graphs.length; k++) {
                                    GraphEntity[] ges;
                                    try {
                                        ges = graphs[k].getEntities();
                                        boolean found = false;
                                        for (int j = 0; j < ges.length && !found; j++) {
                                            found = ges[j].getID().equals(ent.getId());
                                        }
                                        if (found) {
                                            result.append("<li><a href=\"http://app/" + graphs[k].getName() + "/" + ent.getId() + "\">" + graphs[k].getName() + "</a>");
                                        }
                                    } catch (NullEntity e1) {
                                        e1.printStackTrace();
                                    }
                                }
                                result.append("</ul>");
                                searchDiagramPanel.setText(result.toString());
                                focusSearchPane();
                            }
                        } catch (NotInitialised e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        }
        return menu;
    }

    void properties_actionPerformed(ActionEvent e) {
        PropertiesWindow pw = new PropertiesWindow(ids.prop);
        pw.setSize(350, 300);
        pw.setLocation(getCenter(pw.getSize()));
        pw.setVisible(true);
    }

    void save_actionPerformed(ActionEvent e) {
        if (currentFile != null) {
            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to overwrite (y/n)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    System.err.println(currentFile.getName());
                    ingenias.editor.persistence.PersistenceManager pm = new ingenias.editor.persistence.PersistenceManager();
                    pm.save(currentFile, ids);
                    this.updateHistory(currentFile);
                    Log.getInstance().log("Project saved successfully!!");
                    setUnChanged();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } else {
            this.saveas_actionPerformed(e);
            this.updateHistory(currentFile);
        }
    }

    private void eliminateChildren(javax.swing.tree.DefaultMutableTreeNode dmtn) {
        dmtn.removeAllChildren();
    }

    public void clearProjects() {
        this.eliminateChildren(this.ids.om.root);
        this.eliminateChildren(this.ids.gm.root);
        this.pprin.remove(ids.editor);
        ids = new IDEState(null, this.ids.om.root, this.ids.om.arbolObjetos, this.ids.gm.root, this.ids.gm.arbolProyecto);
        ids.editor = new Editor(ids.om);
        pprin.add(ids.editor, java.awt.BorderLayout.CENTER);
        this.validate();
        this.repaint();
    }

    void load_actionPerformed(ActionEvent e) {
        int result = JOptionPane.OK_OPTION;
        if (currentFile != null && changes) {
            result = JOptionPane.showConfirmDialog(this, "You will loose current data. Do you want to continue (y/n)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }
        if (result == JOptionPane.OK_OPTION) {
            try {
                JFileChooser jfc = null;
                if (currentFileFolder == null) {
                    jfc = new JFileChooser();
                } else {
                    jfc = new JFileChooser(currentFileFolder.getPath());
                }
                jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                    public boolean accept(File f) {
                        boolean acceptedFormat = f.getName().toLowerCase().endsWith(".xml");
                        return acceptedFormat || f.isDirectory();
                    }

                    public String getDescription() {
                        return "xml";
                    }
                });
                jfc.setLocation(getCenter(jfc.getSize()));
                jfc.showOpenDialog(this);
                final File input = jfc.getSelectedFile();
                if (input != null && !input.isDirectory()) {
                    new Thread() {

                        public void run() {
                            loadFile(input);
                            setUnChanged();
                        }
                    }.start();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            setEnabled(true);
        }
    }

    public JWindow showMessageWindow(String mess) {
        final JWindow jw = new BusyMessageWindow(ids, this);
        final String message = mess;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JLabel jl = new JLabel(message);
                jl.setFont(new java.awt.Font("Dialog", 1, 36));
                jw.getContentPane().add(jl);
                jw.pack();
                jw.setLocation(getCenter(jw.getSize()));
                jw.setVisible(true);
            }
        });
        return jw;
    }

    void capture_actionPerformed(ActionEvent e) {
        try {
            if (this.ids.gm.getCurrent() == null) {
                JOptionPane.showMessageDialog(this, "Please, open a diagram first");
            } else {
                JFileChooser jfc = null;
                if (currentImageFolder == null && currentFileFolder == null) {
                    jfc = new JFileChooser();
                } else {
                    if (currentImageFolder != null) {
                        jfc = new JFileChooser(this.currentImageFolder.getPath());
                    } else {
                        jfc = new JFileChooser(this.currentFileFolder.getPath());
                    }
                }
                String[] validformats = javax.imageio.ImageIO.getWriterFormatNames();
                final HashSet hs = new HashSet();
                for (int k = 0; k < validformats.length; k++) {
                    hs.add(validformats[k].toLowerCase());
                }
                hs.add("svg");
                hs.add("eps");
                jfc.setAcceptAllFileFilterUsed(false);
                Iterator it = hs.iterator();
                while (it.hasNext()) {
                    final String nextFormat = it.next().toString();
                    jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                        public boolean accept(File f) {
                            boolean acceptedFormat = f.getName().toLowerCase().endsWith(nextFormat);
                            return acceptedFormat || f.isDirectory();
                        }

                        public String getDescription() {
                            return nextFormat;
                        }
                    });
                }
                jfc.setLocation(getCenter(jfc.getSize()));
                jfc.showDialog(this, "Save");
                File sel = jfc.getSelectedFile();
                it = hs.iterator();
                String selectedFormat = jfc.getFileFilter().getDescription();
                if (sel != null && !sel.isDirectory()) {
                    if (sel != null && !(sel.getName().toLowerCase().endsWith(selectedFormat))) {
                        sel = new File(sel.getPath() + "." + selectedFormat);
                    }
                    JPanel temp = new JPanel(new BorderLayout());
                    Container parent = this.ids.gm.getCurrent().getParent();
                    temp.add(this.ids.gm.getCurrent(), BorderLayout.CENTER);
                    ingenias.editor.export.Diagram2SVG.diagram2SVG(temp, sel, selectedFormat);
                    parent.add(this.ids.gm.getCurrent());
                    currentImageFolder = sel;
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    void saveas_actionPerformed(ActionEvent e) {
        try {
            JFileChooser jfc = null;
            if (currentFileFolder == null) {
                jfc = new JFileChooser();
            } else {
                jfc = new JFileChooser(this.currentFileFolder);
                jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                    public boolean accept(File f) {
                        boolean acceptedFormat = f.getName().toLowerCase().endsWith(".xml");
                        return acceptedFormat || f.isDirectory();
                    }

                    public String getDescription() {
                        return "xml";
                    }
                });
            }
            boolean invalidFolder = true;
            File sel = null;
            while (invalidFolder) {
                jfc.setLocation(getCenter(jfc.getSize()));
                jfc.showSaveDialog(this);
                sel = jfc.getSelectedFile();
                invalidFolder = sel != null && !sel.getParentFile().exists();
                if (invalidFolder) {
                    JOptionPane.showMessageDialog(this, "You cannot save your file to " + sel.getParentFile().getPath() + ". That folder does not exist. Please, try again", "Error", JOptionPane.WARNING_MESSAGE);
                }
            }
            if (sel != null && !sel.isDirectory()) {
                if (sel.exists()) {
                    int result = JOptionPane.showConfirmDialog(this, "The file already exists. Do you want to overwrite (y/n)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.OK_OPTION) {
                        PersistenceManager p = new PersistenceManager();
                        p.save(sel, ids);
                        this.currentFile = sel;
                        this.currentFileFolder = sel.getParentFile();
                        this.updateHistory(currentFile);
                        setTitle("Project:" + sel.getAbsolutePath());
                        setUnChanged();
                    }
                } else {
                    PersistenceManager p = new PersistenceManager();
                    if (!sel.getPath().toLowerCase().endsWith(".xml")) {
                        sel = new File(sel.getPath() + ".xml");
                    }
                    p.save(sel, ids);
                    this.currentFile = sel;
                    this.currentFileFolder = sel.getParentFile();
                    setTitle("Project:" + sel.getAbsolutePath());
                    this.updateHistory(currentFile);
                    setUnChanged();
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    void this_windowClosed(WindowEvent e) {
        this.exit_actionPerformed(null);
    }

    void this_windowClosing(WindowEvent e) {
        this.exit_actionPerformed(null);
    }

    void exit_actionPerformed(ActionEvent e) {
        int result = JOptionPane.OK_OPTION;
        if (changes) {
            result = JOptionPane.showConfirmDialog(this, "If you exit, you will loose all changes. Are you sure?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }
        long ctime = System.currentTimeMillis();
        if (result == JOptionPane.OK_OPTION) {
            new PersistenceManager().savePreferences(this);
            this.hide();
            System.exit(0);
        }
    }

    private void replaceTree(DefaultMutableTreeNode replaced, DefaultMutableTreeNode replacee) {
        replaced.removeAllChildren();
        while (replacee.getChildCount() > 0) {
            DefaultMutableTreeNode first = (DefaultMutableTreeNode) replacee.getChildAt(0);
            replacee.remove(first);
            first.removeFromParent();
            replaced.add(first);
            first.setParent(replaced);
        }
    }

    public void loadFile(File file) {
        final File input = file;
        final JWindow jw = showMessageWindow("LOADING...");
        final IDEAbs self = this;
        new Thread(new Runnable() {

            public void run() {
                while (!jw.isVisible()) {
                    Thread.currentThread().yield();
                }
                if (!abackup.isStarted()) abackup.startBackup();
                try {
                    setEnabled(false);
                    Properties oldprops = (Properties) ids.prop.clone();
                    PersistenceManager pm = new PersistenceManager();
                    pm.savePreferences(self);
                    IDEState nids = pm.load(input.getAbsolutePath());
                    pprin.removeAll();
                    ids.editor = nids.editor;
                    buttonModelPanel.removeAll();
                    pprin.add(ids.editor, BorderLayout.CENTER);
                    self.replaceTree(rootObjetos, (DefaultMutableTreeNode) nids.om.arbolObjetos.getModel().getRoot());
                    self.replaceTree(rootProject, (DefaultMutableTreeNode) nids.gm.arbolProyecto.getModel().getRoot());
                    nids.gm.arbolProyecto = arbolProyectos;
                    nids.gm.root = rootProject;
                    nids.om.arbolObjetos = arbolObjetos;
                    nids.om.root = rootObjetos;
                    ((DefaultTreeModel) arbolProyectos.getModel()).reload();
                    ((DefaultTreeModel) arbolObjetos.getModel()).reload();
                    GraphManager.updateCopy(nids.gm);
                    ids.gm = GraphManager.getInstance();
                    ObjectManager.updateCopy(nids.om);
                    ids.om = ObjectManager.getInstance();
                    for (Object key : nids.prop.keySet()) {
                        ids.prop.put(key, nids.prop.get(key));
                    }
                    Log.getInstance().logSYS("Project loaded successfully");
                    validate();
                    setUnChanged();
                    currentFile = input;
                    currentFileFolder = currentFile.getParentFile();
                    setTitle("Project:" + input.getAbsolutePath());
                    updateProperties(oldprops);
                    updateHistory(currentFile);
                    for (TreePath tp : ids.gm.toExpad) {
                        Vector<Object> npath = new Vector<Object>();
                        for (Object path : tp.getPath()) {
                            npath.add(path);
                        }
                        npath.remove(0);
                        npath.insertElementAt(rootProject, 0);
                        arbolProyectos.expandPath(new TreePath(npath.toArray()));
                    }
                    restorePreferences();
                    ids.editor.changeGraph((ModelJGraph) ids.editor.getGraph());
                } catch (ingenias.exception.UnknowFormat e1) {
                    Log.getInstance().logSYS(e1.getMessage());
                    JOptionPane.showMessageDialog(ide, "Failure loading: format unknown. See MESSAGES pane");
                } catch (ingenias.exception.DamagedFormat df) {
                    Log.getInstance().logSYS(df.getMessage());
                    JOptionPane.showMessageDialog(ide, "Failure loading: some diagrams could not be loaded. See MESSAGES pane");
                } catch (ingenias.exception.CannotLoad cl) {
                    Log.getInstance().logSYS(cl.getMessage());
                    JOptionPane.showMessageDialog(ide, "Failure loading: could not load anything. See MESSAGES pane");
                }
                jw.hide();
                setEnabled(true);
            }
        }).start();
    }

    void undo_actionPerformed(ActionEvent e) {
        ActionEvent e1 = new ActionEvent(this.ids.editor.graph, e.getID(), e.getActionCommand(), e.getModifiers());
        if (ids.editor.getGraph() != null) {
            this.ids.editor.getCommonButtons().getUndo().actionPerformed(e1);
            setChanged();
        }
    }

    void redo_actionPerformed(ActionEvent e) {
        ActionEvent e1 = new ActionEvent(this.ids.editor.graph, e.getID(), e.getActionCommand(), e.getModifiers());
        if (ids.editor.getGraph() != null) {
            this.ids.editor.getCommonButtons().getRedo().actionPerformed(e1);
            setChanged();
        }
    }

    void delete_actionPerformed(ActionEvent e) {
        ActionEvent e1 = new ActionEvent(this.ids.editor.graph, e.getID(), e.getActionCommand(), e.getModifiers());
        if (ids.editor.getGraph() != null) {
            this.ids.editor.getCommonButtons().getRemove().actionPerformed(e1);
            setChanged();
        }
    }

    void selectall_actionPerformed(ActionEvent e) {
        if (ids.editor.getGraph() != null) ids.editor.getGraph().setSelectionCells(ids.editor.getGraph().getRoots());
    }

    void copy_actionPerformed(ActionEvent e) {
        ActionEvent e1 = new ActionEvent(this.ids.editor.graph, e.getID(), e.getActionCommand(), e.getModifiers());
        if (ids.editor.getGraph() != null) {
            (this.ids.editor.graph.getTransferHandler().getCopyAction()).actionPerformed(e1);
            setChanged();
        }
    }

    void paste_actionPerformed(ActionEvent e) {
        ActionEvent e1 = new ActionEvent(this.ids.editor.graph, e.getID(), e.getActionCommand(), e.getModifiers());
        if (ids.editor.getGraph() != null) {
            (this.ids.editor.graph.getTransferHandler().getPasteAction()).actionPerformed(e1);
            setChanged();
        }
    }

    void about_actionPerformed(ActionEvent e) {
        About a = new About();
        a.pack();
        a.setLocation(getCenter(a.getSize()));
        a.show();
    }

    void manual_actionPerformed(ActionEvent e) {
        Help h = new Help();
        h.loadHelp("doc/index.htm");
        h.helpPane.setCaretPosition(0);
        h.pack();
        h.setExtendedState(JFrame.MAXIMIZED_BOTH);
        h.show();
    }

    void clearMessages_actionPerformed(ActionEvent e, JTextPane pane) {
        pane.setText("");
    }

    void logs_mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 || e.getButton() == e.BUTTON3) {
            this.messagesMenu.show(this.logs, e.getX(), e.getY());
        }
    }

    void moduleOutput_mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 || e.getButton() == e.BUTTON3) {
            this.messagesMenu.show(this.moduleOutput, e.getX(), e.getY());
        }
    }

    void newProject_actionPerformed(ActionEvent e) {
        int result = JOptionPane.OK_OPTION;
        if (changes) {
            result = JOptionPane.showConfirmDialog(this, "If you create a new project, you will loose all changes. Are you sure?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }
        if (result == JOptionPane.OK_OPTION) {
            this.buttonModelPanel.removeAll();
            IDEState nids = IDEState.emptyIDEState();
            this.pprin.removeAll();
            ids.editor = nids.editor;
            this.pprin.add(ids.editor, BorderLayout.CENTER);
            this.replaceTree(this.rootObjetos, (DefaultMutableTreeNode) nids.om.arbolObjetos.getModel().getRoot());
            this.replaceTree(this.rootProject, (DefaultMutableTreeNode) nids.gm.arbolProyecto.getModel().getRoot());
            nids.gm.arbolProyecto = this.arbolProyectos;
            nids.gm.root = this.rootProject;
            nids.om.arbolObjetos = this.arbolObjetos;
            nids.om.root = this.rootObjetos;
            ((DefaultTreeModel) arbolProyectos.getModel()).reload();
            ((DefaultTreeModel) arbolObjetos.getModel()).reload();
            GraphManager.updateCopy(nids.gm);
            this.ids.gm = GraphManager.getInstance();
            ObjectManager.updateCopy(nids.om);
            this.ids.om = ObjectManager.getInstance();
            this.ids.prop = nids.prop;
            validate();
            setTitle("Empty project");
            setUnChanged();
            this.currentFile = null;
        }
    }

    /**
	 *  Description of the Method
	 *
	 * <param  e  Description of the Parameter
	 */
    void cpClipboard_actionPerformed(ActionEvent e) {
        try {
            JGraph graph = this.ids.editor.getGraph();
            if (graph != null) {
                BufferedImage im = new BufferedImage(graph.getPreferredSize().width, graph.getPreferredSize().height, BufferedImage.TYPE_INT_ARGB);
                Graphics g = im.getGraphics();
                graph.setDoubleBuffered(false);
                graph.setVisible(true);
                graph.setSize(graph.getPreferredSize());
                graph.paint(g);
                graph.setDoubleBuffered(true);
                g.dispose();
                ClipImage ci = new ClipImage(im);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ci, this);
            }
        } catch (Exception ae) {
            ae.printStackTrace();
        }
    }

    /**
	 *  Description of the Method
	 *
	 * >param  e  Description of the Parameter
	 */
    void forcegc_actionPerformed(ActionEvent e) {
        long before = Runtime.getRuntime().freeMemory();
        System.gc();
        JOptionPane.showMessageDialog(ide, "Free memory before:" + before + " and now:" + Runtime.getRuntime().freeMemory(), "Free memory", JOptionPane.INFORMATION_MESSAGE);
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    void enableUMLView_actionPerformed(ActionEvent e) {
        prefs.setModelingLanguage(Preferences.ModelingLanguage.UML);
        enableUMLView.setSelected(true);
    }

    void enableINGENIASView_actionPerformed(ActionEvent e) {
        prefs.setModelingLanguage(Preferences.ModelingLanguage.INGENIAS);
        enableINGENIASView.setSelected(true);
    }

    void resizeAllDiagrams_actionPerformed(ActionEvent e) {
        Vector<ModelJGraph> diagrams = this.ids.gm.getUOModels();
        for (int j = 0; j < diagrams.size(); j++) {
            GraphModel gm = diagrams.elementAt(j).getModel();
            for (int k = 0; k < gm.getRootCount(); k++) {
                DefaultGraphCell dgc = (DefaultGraphCell) gm.getRootAt(k);
                Object userobject = dgc.getUserObject();
                CellView currentview = diagrams.elementAt(j).getGraphLayoutCache().getMapping(dgc, false);
                Entity ent = (Entity) dgc.getUserObject();
                if (ent != null && RenderComponentManager.retrievePanel(ent.getType(), ent.getPrefs().getView()) != null) {
                    Dimension dim = RenderComponentManager.getSize(ent, ent.getType(), ent.getPrefs().getView());
                    if (dim != null) {
                        Map attributes = dgc.getAttributes();
                        Rectangle2D loc = GraphConstants.getBounds(attributes);
                        loc.setRect(loc.getX(), loc.getY(), dim.getWidth(), dim.getHeight());
                        GraphConstants.setBounds(attributes, loc);
                        Map nmap = new Hashtable();
                        nmap.put(dgc, attributes);
                        diagrams.elementAt(j).getModel().edit(nmap, null, null, null);
                        diagrams.elementAt(j).repaint();
                    }
                }
            }
        }
    }

    void resizeAll_actionPerformed(ActionEvent e) {
        GraphModel gm = this.ids.editor.getGraph().getModel();
        for (int k = 0; k < gm.getRootCount(); k++) {
            DefaultGraphCell dgc = (DefaultGraphCell) gm.getRootAt(k);
            Object userobject = dgc.getUserObject();
            CellView currentview = this.ids.editor.getGraph().getGraphLayoutCache().getMapping(dgc, false);
            Entity ent = (Entity) dgc.getUserObject();
            if (ent != null && RenderComponentManager.retrievePanel(ent.getType(), ent.getPrefs().getView()) != null) {
                Dimension dim = RenderComponentManager.getSize(ent, ent.getType(), ent.getPrefs().getView());
                if (dim != null) {
                    Map attributes = dgc.getAttributes();
                    Rectangle2D loc = GraphConstants.getBounds(attributes);
                    loc.setRect(loc.getX(), loc.getY(), dim.getWidth(), dim.getHeight());
                    GraphConstants.setBounds(attributes, loc);
                    Map nmap = new Hashtable();
                    nmap.put(dgc, attributes);
                    this.ids.editor.getGraph().getModel().edit(nmap, null, null, null);
                    this.ids.editor.getGraph().repaint();
                }
            }
        }
    }

    void importFileActionPerformed(ActionEvent evt) {
        int result = JOptionPane.OK_OPTION;
        if (currentFile != null && changes) {
            result = JOptionPane.showConfirmDialog(this, "You will merge the imported file with current one. Do you want to continue (y/n)?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }
        if (result == JOptionPane.OK_OPTION) {
            try {
                JFileChooser jfc = null;
                if (currentFileFolder == null) {
                    jfc = new JFileChooser();
                } else {
                    jfc = new JFileChooser(currentFileFolder.getPath());
                }
                jfc.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

                    public boolean accept(File f) {
                        boolean acceptedFormat = f.getName().toLowerCase().endsWith(".xml");
                        return acceptedFormat || f.isDirectory();
                    }

                    public String getDescription() {
                        return "xml";
                    }
                });
                jfc.showOpenDialog(ide);
                final File input = jfc.getSelectedFile();
                if (input != null && !input.isDirectory()) {
                    new Thread() {

                        public void run() {
                            importFile(input);
                            setUnChanged();
                        }
                    }.start();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            setEnabled(true);
        }
    }

    public void importFile(File file) {
        final File input = file;
        final JWindow jw = showMessageWindow("IMPORTING...");
        final IDEAbs self = this;
        new Thread() {

            public void run() {
                while (!jw.isVisible()) {
                    Thread.currentThread().yield();
                }
                try {
                    setEnabled(false);
                    Properties oldprops = (Properties) ids.prop;
                    PersistenceManager pm = new PersistenceManager();
                    pm.mergeFile(input.getAbsolutePath(), ids);
                    ((DefaultTreeModel) arbolProyectos.getModel()).reload();
                    ((DefaultTreeModel) arbolObjetos.getModel()).reload();
                    Log.getInstance().logSYS("Project imported successfully");
                    validate();
                    self.setUnChanged();
                    currentFile = input;
                    currentFileFolder = currentFile.getParentFile();
                    setTitle("Project:" + input.getAbsolutePath());
                    self.updateProperties(oldprops);
                    self.updateHistory(currentFile);
                    jw.hide();
                } catch (ingenias.exception.UnknowFormat e1) {
                    Log.getInstance().logSYS(e1.getMessage());
                    JOptionPane.showMessageDialog(ide, "Failure loading: format unknown. See MESSAGES pane");
                    jw.hide();
                } catch (ingenias.exception.DamagedFormat df) {
                    Log.getInstance().logSYS(df.getMessage());
                    JOptionPane.showMessageDialog(ide, "Failure loading: some diagrams could not be loaded. See MESSAGES pane");
                    jw.hide();
                } catch (ingenias.exception.CannotLoad cl) {
                    Log.getInstance().logSYS(cl.getMessage());
                    JOptionPane.showMessageDialog(ide, "Failure loading: could not load anything. See MESSAGES pane");
                    jw.hide();
                }
                setEnabled(true);
            }
        }.start();
    }

    private void springLayout() {
        GraphModel gm = this.ids.editor.getGraph().getModel();
        Hashtable vectors = new Hashtable();
        for (int k = 0; k < gm.getRootCount(); k++) {
            DefaultGraphCell dgc = (DefaultGraphCell) gm.getRootAt(k);
            Object userobject = dgc.getUserObject();
            CellView currentview = this.ids.editor.getGraph().getGraphLayoutCache().getMapping(dgc, false);
            Map attributes = dgc.getAttributes();
            Rectangle2D loc = GraphConstants.getBounds(attributes);
            double dirx = 0;
            double diry = 0;
            if (loc != null) {
                loc = (Rectangle2D) loc.clone();
                loc.setRect(loc.getX(), loc.getY(), loc.getWidth() + 10, loc.getHeight() + 10);
                Vector alreadyconsidered = new Vector();
                for (int i = 0; i < gm.getRootCount(); i++) {
                    DefaultGraphCell otherdgc = (DefaultGraphCell) gm.getRootAt(i);
                    if (otherdgc != dgc) {
                        Map otheratts = otherdgc.getAttributes();
                        Rectangle2D otherloc = GraphConstants.getBounds(otheratts);
                        if (otherloc != null) {
                            otherloc = (Rectangle2D) otherloc.clone();
                            otherloc.setRect(otherloc.getX(), otherloc.getY(), otherloc.getWidth() + 10, otherloc.getHeight() + 10);
                            if (otherloc.intersects(loc)) {
                                dirx = dirx + (otherloc.getCenterX() - loc.getCenterX());
                                diry = diry + (otherloc.getCenterY() - loc.getCenterY());
                            }
                            for (int j = 0; j < otherdgc.getChildCount(); j++) {
                                if (gm.isPort(otherdgc.getChildAt(j))) {
                                    Iterator edges = gm.edges(otherdgc.getChildAt(j));
                                    while (edges.hasNext()) {
                                        DefaultEdge de = (DefaultEdge) edges.next();
                                        if (!alreadyconsidered.contains(de)) {
                                            Map atts = de.getAttributes();
                                            DefaultGraphCell source = (DefaultGraphCell) ((DefaultPort) de.getSource()).getParent();
                                            DefaultGraphCell target = (DefaultGraphCell) ((DefaultPort) de.getTarget()).getParent();
                                            if (source != dgc && target != dgc) {
                                                Map sourceat = source.getAttributes();
                                                Map targetat = target.getAttributes();
                                                Rectangle2D locporto = GraphConstants.getBounds(sourceat);
                                                Rectangle2D locportt = GraphConstants.getBounds(targetat);
                                                if (locporto != null && locportt != null && loc.intersectsLine(locporto.getCenterX(), locporto.getCenterY(), locportt.getCenterX(), locportt.getCenterY())) {
                                                    double midx = (locportt.getCenterX() + locporto.getCenterX()) / 2;
                                                    double midy = (locportt.getCenterY() + locporto.getCenterY()) / 2;
                                                    dirx = dirx + (midx - loc.getCenterX());
                                                    diry = diry + (midy - loc.getCenterY());
                                                }
                                            }
                                            alreadyconsidered.add(de);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            vectors.put(dgc, new Point((int) dirx, (int) diry));
        }
        Enumeration enumeration = vectors.keys();
        Hashtable newCoords = new Hashtable();
        while (enumeration.hasMoreElements()) {
            DefaultGraphCell dgc = (DefaultGraphCell) enumeration.nextElement();
            Point p = (Point) vectors.get(dgc);
            double module = Math.sqrt(p.x * p.x + p.y * p.y);
            if (module != 0) {
                Map m = dgc.getAttributes();
                Rectangle2D loc = GraphConstants.getBounds(m);
                if (loc != null) {
                    double x = loc.getX() - (p.x / module) * 5;
                    double y = loc.getY() - (p.y / module) * 5;
                    double w = loc.getWidth();
                    double h = loc.getHeight();
                    if (x < 0) x = 1;
                    if (y < 0) y = 1;
                    Rectangle nrect = new Rectangle((int) x, (int) y, (int) w, (int) h);
                    GraphConstants.setBounds(m, nrect);
                    newCoords.put(dgc, m);
                }
            }
        }
        this.ids.editor.getGraph().getModel().edit(newCoords, null, null, null);
    }

    void elimOverlapActionPerformed(ActionEvent evt) {
        this.springLayout();
    }

    public void SearchActionPerformed(ActionEvent evt) {
        String id = this.searchField.getText();
        locateAndScrollToObject(id);
    }

    private void locateAndScrollToObject(String id) {
        if (id.equals(lastSearch) && lastFoundIndex < foundpaths.size()) {
            TreePath tp = (TreePath) this.foundpaths.elementAt(lastFoundIndex);
            ids.om.arbolObjetos.expandPath(tp);
            ids.om.arbolObjetos.scrollPathToVisible(tp);
            ids.om.arbolObjetos.setSelectionPath(tp);
            lastFoundIndex++;
        } else {
            foundpaths = this.ids.om.findUserObjectPathRegexp(id + ".*");
            if (foundpaths.size() > 0) {
                lastFoundIndex = 0;
                lastSearch = id;
                TreePath tp = (TreePath) this.foundpaths.elementAt(lastFoundIndex);
                ids.om.arbolObjetos.expandPath(tp);
                ids.om.arbolObjetos.scrollPathToVisible(tp);
                ids.om.arbolObjetos.setSelectionPath(tp);
                lastFoundIndex++;
            }
        }
    }

    public void searchFieldKeyTyped(KeyEvent evt) {
        if (evt.getKeyChar() == evt.VK_ENTER) {
            this.SearchActionPerformed(null);
        }
    }

    public void editPopUpProperties_selected() {
        prefs.setEditPropertiesMode(Preferences.EditPropertiesMode.POPUP);
    }

    ;

    public void editOnMessages_selected() {
        prefs.setEditPropertiesMode(Preferences.EditPropertiesMode.PANEL);
    }

    ;

    public void switchUMLView_actionPerformed(ActionEvent e) {
        this.enableUMLView_actionPerformed(e);
        Vector<Entity> entities = this.ids.om.getAllObjects();
        for (int k = 0; k < entities.size(); k++) {
            entities.elementAt(k).getPrefs().setView(ViewPreferences.ViewType.UML);
        }
    }

    public void switchINGENIASView_actionPerformed(ActionEvent e) {
        this.enableINGENIASView_actionPerformed(e);
        Vector<Entity> entities = this.ids.om.getAllObjects();
        for (int k = 0; k < entities.size(); k++) {
            entities.elementAt(k).getPrefs().setView(ViewPreferences.ViewType.INGENIAS);
        }
    }
}

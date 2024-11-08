package net.etherstorm.jopenrpg.swing.gametree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.Hashtable;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import net.etherstorm.jopenrpg.ReferenceManager;
import net.etherstorm.jopenrpg.net.TreeMessage;
import net.etherstorm.jopenrpg.swing.JFileHistoryMenu;
import net.etherstorm.jopenrpg.swing.actions.DefaultAction;
import net.etherstorm.jopenrpg.swing.actions.GametreeAction;
import net.etherstorm.jopenrpg.swing.actions.SendNodeToPlayerAction;
import net.etherstorm.jopenrpg.swing.event.FileHistoryEvent;
import net.etherstorm.jopenrpg.swing.event.FileHistoryListener;
import net.etherstorm.jopenrpg.swing.event.GameTreeEvent;
import net.etherstorm.jopenrpg.swing.event.GameTreeListener;
import net.etherstorm.jopenrpg.swing.nodehandlers.BaseNodehandler;
import net.etherstorm.jopenrpg.swing.nodehandlers.RootNodehandler;
import net.etherstorm.jopenrpg.util.ExceptionHandler;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * 
 * @author Ted Berg
 * @author $Author: tedberg $
 * @version $Revision: 1.49 $
 * $Date: 2006/11/16 20:06:28 $
 */
public class JGameTreePanel extends JPanel implements GameTreeListener, FileHistoryListener {

    protected JGameTree gametree;

    protected GameTreeModel treemodel;

    protected Hashtable images;

    protected File gametreeFile;

    protected JPopupMenu popupMenu;

    protected boolean treeIsDirty = false;

    protected SAXBuilder sax;

    protected ReferenceManager referenceManager = ReferenceManager.getInstance();

    protected String lastUsedDir = "";

    protected static Logger logger = Logger.getLogger(JGameTreePanel.class);

    public final OpenTreeAction openTreeAction;

    public final SaveTreeAction saveTreeAction;

    public final SaveTreeAsAction saveTreeAsAction;

    public final ImportXMLFileAction importXMLFileAction;

    public final ExportXMLFileAction exportXMLFileAction;

    public final SendNodeToPlayerAction sendNodeToPlayerAction;

    public final JFileHistoryMenu gametreeHistory;

    public final JFileHistoryMenu nodeHistory;

    /**
	 * 
	 */
    public JGameTreePanel() {
        super(new BorderLayout());
        sax = new SAXBuilder();
        logger.info("creating open tree action");
        openTreeAction = new OpenTreeAction();
        logger.info("creating save tree action");
        saveTreeAction = new SaveTreeAction();
        logger.info("creating saveas tree action");
        saveTreeAsAction = new SaveTreeAsAction();
        logger.info("creating importxmlfile tree action");
        importXMLFileAction = new ImportXMLFileAction();
        logger.info("creating exportxmlfile tree action");
        exportXMLFileAction = new ExportXMLFileAction();
        logger.info("creating sendnode tree action");
        sendNodeToPlayerAction = new SendNodeToPlayerAction(null);
        logger.info("creating reopen tree action");
        gametreeHistory = new JFileHistoryMenu("Reopen Gametree");
        logger.info("creating reimport node tree action");
        nodeHistory = new JFileHistoryMenu("Reimport Node");
        try {
            logger.info("finding default tree");
            java.net.URL u = JGameTreePanel.class.getResource("/tree.xml");
            logger.info("parsing tree " + u);
            Document doc = sax.build(u);
            logger.info("building tree model");
            treemodel = new GameTreeModel(doc);
            treeIsDirty = false;
        } catch (Exception ex) {
            logger.error(ex);
            ExceptionHandler.handleException(ex);
        }
        logger.info("creating tree");
        gametree = new JGameTree(treemodel);
        logger.info("creating renderer");
        GameTreeCellRenderer gtcr = new GameTreeCellRenderer();
        gametree.setCellRenderer(gtcr);
        logger.info("creating editor");
        gametree.setCellEditor(new MyGameTreeCellEditor(gametree, gtcr));
        add(new JScrollPane(gametree));
        logger.info("registering with tooltip manager");
        ToolTipManager.sharedInstance().registerComponent(gametree);
        images = new Hashtable();
        gametree.setEditable(true);
        logger.info("creating popup menu");
        popupMenu = new JPopupMenu("Gametree");
        logger.info("creating toolbar");
        JToolBar tb = new JToolBar();
        tb.add(new MoveNodeUpAction(null));
        tb.add(new MoveNodeDownAction(null));
        tb.add(new MoveNodeLeftAction(null));
        tb.add(new MoveNodeRightAction(null));
        tb.addSeparator();
        tb.add(new CloneNodeAction(null));
        tb.addSeparator();
        tb.add(new DeleteNodeAction(null));
        add(tb, BorderLayout.NORTH);
        logger.info("adding mouse listener");
        gametree.addMouseListener(new MouseAdapter() {

            /**
			* Method declaration
			* 
			* 
			* @param evt
			* 
			* 
			*/
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    TreePath path = gametree.getClosestPathForLocation(evt.getX(), evt.getY());
                    BaseNodehandler bnh = (BaseNodehandler) path.getPath()[path.getPathCount() - 1];
                    bnh.openHandler();
                } else {
                    tryPopup(evt);
                }
            }

            /**
			* Method declaration
			* 
			* 
			* @param evt
			* 
			* 
			*/
            public void mousePressed(MouseEvent evt) {
                tryPopup(evt);
            }

            /**
			* Method declaration
			* 
			* 
			* @param evt
			* 
			* 
			*/
            public void mouseReleased(MouseEvent evt) {
                tryPopup(evt);
            }
        });
        logger.info("adding self to core's gametreelistener list");
        referenceManager.getCore().addGameTreeListener(this);
        logger.info("adding self to file history listener lists");
        gametreeHistory.addFileHistoryListener(this);
        nodeHistory.addFileHistoryListener(this);
    }

    public void addNodehandler(BaseNodehandler bnh) {
        treemodel.addNode(bnh);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * 
	 */
    public void doLoadInitialTree() {
        try {
            java.net.URL u = JGameTreePanel.class.getResource("/tree.xml");
            Document doc = sax.build(u);
            treemodel = new GameTreeModel(doc);
            gametree.setModel(treemodel);
            treeIsDirty = false;
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param evt
	 * 
	 * 
	 */
    void tryPopup(MouseEvent evt) {
        if (popupMenu.isPopupTrigger(evt)) {
            TreePath p = gametree.getClosestPathForLocation(evt.getX(), evt.getY());
            popupMenu.removeAll();
            ((BaseNodehandler) p.getLastPathComponent()).populatePopupMenu(popupMenu);
            popupMenu.show(gametree, evt.getX(), evt.getY());
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 * 
	 */
    public void addTreeSelectionListener(TreeSelectionListener l) {
        gametree.addTreeSelectionListener(l);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param l
	 * 
	 * 
	 */
    public void removeTreeSelectionListener(TreeSelectionListener l) {
        gametree.removeTreeSelectionListener(l);
    }

    /**
	 *
	 * @param fhe
	 */
    public void reopenFile(FileHistoryEvent fhe) {
        if (fhe.getSource() == gametreeHistory) {
            loadGameTree(new File(fhe.getFilename()));
        } else if (fhe.getSource() == nodeHistory) {
            importXMLFile(new File(fhe.getFilename()));
        }
    }

    /**
	 * 
	 * 
	 * @param f
	 */
    public void loadGameTree(final File f) {
        try {
            new Thread(new Runnable() {

                public void run() {
                    try {
                        try {
                            Document doc = null;
                            doc = sax.build(f);
                            treemodel = new GameTreeModel(doc);
                        } catch (Exception spe) {
                            ExceptionHandler.handleException(spe);
                            return;
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                try {
                                    gametree.setModel(treemodel);
                                    gametreeFile = f;
                                    treeIsDirty = false;
                                    gametreeHistory.addFile(f);
                                    referenceManager.getMainFrame().setTitle(f.getCanonicalPath());
                                } catch (Exception ex) {
                                    ExceptionHandler.handleExceptionSafely(ex);
                                }
                            }
                        });
                    } catch (Exception ex) {
                        ExceptionHandler.handleExceptionSafely(ex);
                    }
                }
            }).start();
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param e
	 * 
	 * 
	 */
    public void doImportXML(Element e) {
        try {
            org.jdom.output.XMLOutputter xout = new org.jdom.output.XMLOutputter();
            RootNodehandler root = (RootNodehandler) treemodel.getRoot();
            root.addChildFromXML((Element) e.getChildren().get(0));
            TreeModelEvent tme = new TreeModelEvent(treemodel, treemodel.getTreePath(root), new int[] { treemodel.getChildCount(root) - 1 }, null);
            treemodel.fireTreeNodesInsertedEvent(tme);
            treeIsDirty = true;
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    public void addNode(BaseNodehandler bnh) {
        treemodel.addNode(bnh);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * 
	 */
    public void doImportXML() {
        try {
            JFileChooser fc = referenceManager.getDefaultFileChooser();
            fc.setSelectedFile(new File(lastUsedDir));
            int result = fc.showOpenDialog(referenceManager.getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                importXMLFile(fc.getSelectedFile());
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 *
	 * @param file
	 */
    public void importXMLFile(final File file) {
        try {
            lastUsedDir = file.getCanonicalPath();
            new Thread(new Runnable() {

                public void run() {
                    try {
                        final Document d = sax.build(file);
                        doImportXML(new Element("foo").addContent((Element) d.getRootElement().clone()));
                    } catch (org.jdom.JDOMException jdex) {
                        ExceptionHandler.handleExceptionSafely(jdex);
                    } catch (java.io.IOException ioe) {
                        ExceptionHandler.handleExceptionSafely(ioe);
                    }
                }
            }).start();
            nodeHistory.addFile(file);
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * 
	 */
    public void doExportXML() {
        try {
            BaseNodehandler bnh = (BaseNodehandler) gametree.getLastSelectedPathComponent();
            File sf = new File(bnh.getNodeName() + ".xml");
            JFileChooser fc = referenceManager.getDefaultFileChooser();
            fc.setSelectedFile(sf);
            int result = fc.showSaveDialog(referenceManager.getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if ((f.exists())) {
                    String str = MessageFormat.format("The file {0} exists.	Do you wish to overwrite it?", new Object[] { f });
                    result = JOptionPane.showConfirmDialog(referenceManager.getMainFrame(), str);
                    if (result != JOptionPane.YES_OPTION) return;
                }
                Document d = new Document(bnh.toXML());
                referenceManager.getCore().writeDomToFile(d, f);
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param e
	 * @param id
	 * 
	 * 
	 */
    public void doSendNodeToPlayer(Element e, String id) {
        referenceManager.getCore().sendTreeMessage(id, e);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param target
	 * 
	 * 
	 */
    public void doSendNodeToPlayers(BaseNodehandler target) {
        try {
            BaseNodehandler bnh = (target == null) ? (BaseNodehandler) gametree.getLastSelectedPathComponent() : target;
            if (bnh == null) {
                return;
            }
            TreeMessage tm = new TreeMessage();
            tm.setNodehandler(bnh);
            tm.setIncoming(false);
            tm.send();
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * 
	 */
    public void doOpenTree() {
        try {
            JFileChooser fc = referenceManager.getDefaultFileChooser();
            int result = fc.showOpenDialog(referenceManager.getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                loadGameTree(f);
                treemodel.fireTreeStructureChangedEvent();
                referenceManager.getMainFrame().setActiveDocument(f.toString());
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * 
	 */
    public void doSaveTree() {
        try {
            referenceManager.getMainFrame().setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            if (gametreeFile == null) {
                doSaveTreeAs();
                return;
            }
            System.out.println(treemodel.getRoot());
            System.out.println(((BaseNodehandler) treemodel.getRoot()).toXML());
            Document d = new Document(((BaseNodehandler) treemodel.getRoot()).toXML());
            referenceManager.getCore().writeDomToFile(d, gametreeFile);
            treeIsDirty = false;
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        } finally {
            referenceManager.getMainFrame().setCursor(java.awt.Cursor.getDefaultCursor());
        }
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * 
	 */
    public void doSaveTreeAs() {
        try {
            JFileChooser fc = referenceManager.getDefaultFileChooser();
            fc.setSelectedFile(gametreeFile);
            int result = fc.showSaveDialog(referenceManager.getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (f.exists()) {
                    String msg = MessageFormat.format("File {0} already exists.\nDo you wish to overwrite it?", new Object[] { f.getCanonicalPath() });
                    if (JOptionPane.YES_OPTION != JOptionPane.showInternalConfirmDialog(referenceManager.getMainFrame().getDesktop(), msg, "Overwrite File?", JOptionPane.YES_NO_OPTION)) return;
                }
                gametreeFile = f;
                doSaveTree();
            }
        } catch (Exception ex) {
            ExceptionHandler.handleException(ex);
        }
    }

    /**
	 * 
	 * 
	 * @param value
	 */
    Icon getNodeIcon(BaseNodehandler value) {
        return value.getNodeIcon();
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param evt
	 * 
	 * 
	 */
    public void receivedTreeNode(GameTreeEvent evt) {
        Element e = new Element("foo");
        e.addContent((Element) evt.getMsg().getNodehandler().clone());
        doImportXML(e);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param target
	 * 
	 * 
	 */
    public void doMoveNodeUp(BaseNodehandler target) {
        treemodel.doMoveNodeUp(target);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param target
	 * 
	 * 
	 */
    public void doMoveNodeDown(BaseNodehandler target) {
        treemodel.doMoveNodeDown(target);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param target
	 * 
	 * 
	 */
    public void doMoveNodeLeft(BaseNodehandler target) {
        treemodel.doMoveNodeLeft(target);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param target
	 * 
	 * 
	 */
    public void doMoveNodeRight(BaseNodehandler target) {
        treemodel.doMoveNodeRight(target);
    }

    public void doCloneNode(BaseNodehandler target) {
        treemodel.doCloneNode(target);
    }

    /**
	 * Method declaration
	 * 
	 * 
	 * @param target
	 * 
	 * 
	 */
    public void doDeleteNode(BaseNodehandler target) {
        treemodel.doDeleteNode(target);
    }

    /**
	 * 
	 * 
	 */
    class GameTreeCellRenderer extends DefaultTreeCellRenderer {

        protected Logger logger = Logger.getLogger(GameTreeCellRenderer.class);

        /**
		 * 
		 */
        public GameTreeCellRenderer() {
            super();
        }

        /**
		 * 
		 * 
		 * @param tree
		 * @param value
		 * @param selected
		 * @param expanded
		 * @param leaf
		 * @param row
		 * @param hasFocus
		 */
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            try {
                setToolTipText("Node class :" + ((BaseNodehandler) value).getNodeType() + "[" + value.getClass() + "]");
                Icon i = getNodeIcon((BaseNodehandler) value);
                if (i != null) {
                    setIcon(i);
                }
            } catch (Exception ex) {
            }
            setBackgroundNonSelectionColor(getBackground());
            return this;
        }
    }

    /**
	 * 
	 * 
	 */
    public class OpenTreeAction extends DefaultAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 */
        public OpenTreeAction() {
            super();
            initProperties("OpenTreeAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            doOpenTree();
        }
    }

    /**
	 * 
	 * 
	 */
    public class SaveTreeAction extends DefaultAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 */
        public SaveTreeAction() {
            super();
            initProperties("SaveTreeAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doSaveTree();
        }
    }

    /**
	 * 
	 * 
	 */
    public class SaveTreeAsAction extends DefaultAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 */
        public SaveTreeAsAction() {
            super();
            initProperties("SaveTreeAsAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doSaveTreeAs();
        }
    }

    /**
	 * 
	 * 
	 */
    public class ImportXMLFileAction extends DefaultAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 */
        public ImportXMLFileAction() {
            super();
            initProperties("ImportXMLFileAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doImportXML();
        }
    }

    /**
	 * 
	 * 
	 */
    public class ExportXMLFileAction extends DefaultAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 */
        public ExportXMLFileAction() {
            super();
            initProperties("ExportXMLFileAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doExportXML();
        }
    }

    /**
	 * 
	 * 
	 */
    public class MoveNodeUpAction extends GametreeAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 * @param bnh
		 * 
		 */
        public MoveNodeUpAction(BaseNodehandler bnh) {
            super(bnh);
            initProperties("MoveNodeUpAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doMoveNodeUp((BaseNodehandler) gametree.getLastSelectedPathComponent());
        }
    }

    /**
	 * 
	 * 
	 */
    public class MoveNodeDownAction extends GametreeAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 * @param bnh
		 * 
		 */
        public MoveNodeDownAction(BaseNodehandler bnh) {
            super(bnh);
            initProperties("MoveNodeDownAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doMoveNodeDown((BaseNodehandler) gametree.getLastSelectedPathComponent());
        }
    }

    /**
	 * 
	 * 
	 */
    public class MoveNodeLeftAction extends GametreeAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 * @param bnh
		 * 
		 */
        public MoveNodeLeftAction(BaseNodehandler bnh) {
            super(bnh);
            initProperties("MoveNodeLeftAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doMoveNodeLeft((BaseNodehandler) gametree.getLastSelectedPathComponent());
        }
    }

    /**
	 * 
	 * 
	 */
    public class MoveNodeRightAction extends GametreeAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 * @param bnh
		 * 
		 */
        public MoveNodeRightAction(BaseNodehandler bnh) {
            super(bnh);
            initProperties("MoveNodeRightAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doMoveNodeRight((BaseNodehandler) gametree.getLastSelectedPathComponent());
        }
    }

    /**
	 *
	 * 
	 */
    public class CloneNodeAction extends GametreeAction {

        /**
		 * @param bnh
		 */
        public CloneNodeAction(BaseNodehandler bnh) {
            super(bnh);
            initProperties("CloneNodeAction");
        }

        /**
		 * @param evt
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doCloneNode((BaseNodehandler) gametree.getLastSelectedPathComponent());
        }
    }

    /**
	 * 
	 * 
	 */
    public class DeleteNodeAction extends GametreeAction {

        /**
		 * Constructor declaration
		 * 
		 * 
		 * @param bnh
		 * 
		 */
        public DeleteNodeAction(BaseNodehandler bnh) {
            super(bnh);
            initProperties("DeleteNodeAction");
        }

        /**
		 * Method declaration
		 * 
		 * 
		 * @param evt
		 * 
		 * 
		 */
        public void actionPerformed(ActionEvent evt) {
            super.actionPerformed(evt);
            doDeleteNode((BaseNodehandler) gametree.getLastSelectedPathComponent());
        }
    }
}

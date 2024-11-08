package de.cinek.rssview;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import de.cinek.rssview.directory.FeedAssistent;
import de.cinek.rssview.event.AmphetaDeskEvent;
import de.cinek.rssview.event.AmphetaDeskListener;
import de.cinek.rssview.event.ArticleSelectionListener;
import de.cinek.rssview.event.ChannelSelectionListener;
import de.cinek.rssview.images.IconContainer;
import de.cinek.rssview.images.IconSet;
import de.cinek.rssview.io.DataStoreException;
import de.cinek.rssview.ui.JToolBarButton;
import de.cinek.rssview.ui.RssFolderDialog;

/**
 * Main RssView Window.
 * @version $Id: RssView.java,v 1.58 2004/11/13 21:32:29 saintedlama Exp $
 */
public class RssView extends JFrame {

    private static final String SELECTION_PATH = "selectionpath";

    public static final int VIEWSIZE_NORMAL = 0;

    public static final int VIEWSIZE_SMALL = 1;

    public static final int VIEWSIZE_ARTICLE_LIST = 2;

    public static final int VIEWSIZE_EXCLUSIVE = 3;

    public static final int MODE_INTERACTIVE = 0;

    public static final int MODE_EXCLUSIVE = 1;

    public static final String APP_NAME = "Rss Viewer";

    public static final String APP_VER = "2.0.0-BETA";

    public static final String VIEWSIZE_PROPERTY = "VIEWSIZE";

    public static final String OVERVIEW = "OVERVIEW";

    public static final String CONTENT = "CONTENT";

    public static final String CHANNEL_OVERVIEW = "CHANNEL_OVERVIEW";

    public static final String TOOLBAR = "TOOLBAR";

    public static final String MENUBAR = "MENUBAR";

    public static final String STATUSBAR = "STATUSBAR";

    public static final String CHANNEL_TOOLBAR = "CHANNEL_TOOLBAR";

    public static final String VIEW_MENUBAR = "VIEW_MENUBAR";

    public static final String ARTICLE_CONTENT = "ARTICLE_CONTENT";

    private RssChannelList channelList;

    private RssChannelView channelView;

    private RssArticleOverview channelTitle;

    private RssStatusBar statusBar;

    private JToolBar toolBar;

    private int viewSize = -1;

    private int mode = MODE_INTERACTIVE;

    private Action exitAction;

    private Action newChannelAction;

    private Action deleteChannelAction;

    private Action editChannelAction;

    private Action pasteChannelAction;

    private Action copyChannelAction;

    private Action infoAction;

    private Action optionsAction;

    private Action compactViewAction;

    private Action standardViewAction;

    private Action updateArticlesAction;

    private Action articleListViewAction;

    private Action channelDownAction;

    private Action channelUpAction;

    private Action markArticlesReadAction;

    private Action newFolderAction;

    private Action editFolderAction;

    private Action deleteFolderAction;

    private Action feedAssistantAction;

    private ChannelModel channelModel;

    private ChannelFetcher fetcher;

    /**
	 * Component map holds all components that can be placed/used on views
	 */
    private ComponentRepository componentRepository;

    /**
	 * View property sets up the outfit and layout of RssView and saves Settings
	 * to RssSettings bean.
	 */
    private View view;

    /**
	 * Represents the current selection Path in RssView
	 */
    private RssChannelSelectionModel selectionModel;

    private RssArticleSelectionModel articleSelectionModel;

    /**
	 * ResourceBundle for I18N
	 */
    private static final ResourceBundle rb = ResourceBundle.getBundle("rssview");

    ;

    private boolean shutdown = false;

    /**
	 * Constructs RssView Frame. If channelUrl is null RssView is started in
	 * interactive mode. If channelUrl is unequal null RssView is started in
	 * EXCLUSIVE Mode.
	 * 
	 * @param channelUrl Specifies Channel to load.
	 */
    public RssView() {
        super(APP_NAME + " - " + APP_VER);
        ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_APP);
        setIconImage(icon.getImage());
    }

    /**
	 * Initializes RssView. Sets up event listeners and starts monitoring
	 * Threads. Not that ChannelModel has to be set before calling initialize.
	 *
	 */
    public void initialize() {
        this.componentRepository = new DefaultComponentRepository();
        this.addPropertyChangeListener(VIEWSIZE_PROPERTY, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                viewSizeChanged(evt);
            }
        });
        AmphetaDeskAdapter.getInstance().addAmphetaDeskListener(new AmphetaDeskListener() {

            public void amphetaLinkChannelAdded(AmphetaDeskEvent event) {
                amphetaDeskChannelAdded(event);
            }
        });
        RssNotification notifier = new RssNotification();
        notifier.setChannelModel(channelModel);
        this.fetcher = new ChannelFetcher(this.channelModel);
        this.selectionModel = new RssChannelSelectionModel();
        this.articleSelectionModel = new RssArticleSelectionModel();
        initComponents();
        initActions();
        fetcher.start();
        RssShutdownHook shutdownHook = new RssShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void initComponents() {
        toolBar = createDefaultToolBar();
        componentRepository.register(TOOLBAR, toolBar);
        RssToolBar channelToolBar = new RssToolBar(this);
        componentRepository.register(CHANNEL_TOOLBAR, channelToolBar);
        JMenuBar menuBar = createDefaultMenuBar();
        componentRepository.register(MENUBAR, menuBar);
        JMenuBar viewMenuBar = createViewMenuBar();
        componentRepository.register(VIEW_MENUBAR, viewMenuBar);
        channelTitle = new RssArticleOverview(this);
        componentRepository.register(OVERVIEW, channelTitle);
        statusBar = new RssStatusBar();
        componentRepository.register(STATUSBAR, statusBar);
        channelView = new RssChannelView(this, statusBar, channelModel);
        componentRepository.register(CONTENT, channelView);
        RssArticleView articleView = new RssArticleView(this, statusBar, channelModel);
        componentRepository.register(ARTICLE_CONTENT, articleView);
        if (mode != MODE_EXCLUSIVE) {
            channelList = new RssChannelList(this, channelModel);
        } else {
            channelList = new RssChannelList(this, channelModel);
        }
        componentRepository.register(CHANNEL_OVERVIEW, channelList);
        String nodeNames[] = (String[]) RssSettings.getInstance().getSetting(SELECTION_PATH);
        Node nodes[] = channelModel.getTreePath(nodeNames);
        selectionModel.setSelectionPath(nodes);
        if (mode != MODE_EXCLUSIVE) {
            int newViewSize = RssSettings.getInstance().getDefaultView();
            if (newViewSize < 0) {
                newViewSize = RssSettings.getInstance().getViewSize();
            }
            setViewSize(newViewSize);
        } else {
            setViewSize(VIEWSIZE_EXCLUSIVE);
        }
    }

    private synchronized void persistSubscriptionModel() {
        if (mode != MODE_EXCLUSIVE) {
            try {
                RssCore.getInstance().getDataStore().persist(channelModel);
            } catch (DataStoreException dex) {
                JOptionPane.showMessageDialog(null, rb.getString("SQL_error"), rb.getString("Fatal_SQL_error"), JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        }
    }

    private JToolBar createDefaultToolBar() {
        JToolBar defaultToolBar = new JToolBar();
        defaultToolBar.setRollover(true);
        defaultToolBar.add(new JToolBarButton(getNewChannelAction()));
        defaultToolBar.add(new JToolBarButton(getEditChannelAction()));
        defaultToolBar.add(new JToolBarButton(getDeleteChannelAction()));
        defaultToolBar.addSeparator();
        defaultToolBar.add(new JToolBarButton(getPasteChannelAction()));
        defaultToolBar.add(new JToolBarButton(getCopyChannelAction()));
        defaultToolBar.addSeparator();
        defaultToolBar.add(new JToolBarButton(getNewFolderAction()));
        defaultToolBar.add(new JToolBarButton(getEditFolderAction()));
        defaultToolBar.add(new JToolBarButton(getDeleteFolderAction()));
        defaultToolBar.addSeparator();
        defaultToolBar.add(new JToolBarButton(getStandardViewAction()));
        defaultToolBar.add(new JToolBarButton(getCompactViewAction()));
        defaultToolBar.add(new JToolBarButton(getArticleListViewAction()));
        defaultToolBar.addSeparator();
        defaultToolBar.add(new JToolBarButton(getMarkArticlesReadAction()));
        defaultToolBar.addSeparator();
        defaultToolBar.add(new JToolBarButton(getFeedAssistentAction()));
        return defaultToolBar;
    }

    private JMenuBar createDefaultMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(rb.getString("Channel"));
        fileMenu.setMnemonic('C');
        fileMenu.add(new JMenuItem(getNewChannelAction()));
        fileMenu.add(new JMenuItem(getEditChannelAction()));
        fileMenu.add(new JMenuItem(getDeleteChannelAction()));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(getNewFolderAction()));
        fileMenu.add(new JMenuItem(getEditFolderAction()));
        fileMenu.add(new JMenuItem(getDeleteFolderAction()));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(getExitAction()));
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu(rb.getString("Edit"));
        editMenu.setMnemonic('E');
        editMenu.add(new JMenuItem(getPasteChannelAction()));
        editMenu.add(new JMenuItem(getCopyChannelAction()));
        menuBar.add(editMenu);
        JMenu viewMenu = new JMenu(rb.getString("View"));
        viewMenu.setMnemonic('V');
        viewMenu.add(new JMenuItem(getCompactViewAction()));
        viewMenu.add(new JMenuItem(getStandardViewAction()));
        viewMenu.add(new JMenuItem(getArticleListViewAction()));
        menuBar.add(viewMenu);
        JMenu optionsMenu = new JMenu(rb.getString("Settings"));
        optionsMenu.setMnemonic('S');
        optionsMenu.add(new JMenuItem(getOptionsAction()));
        menuBar.add(optionsMenu);
        JMenu helpMenu = new JMenu(rb.getString("Help"));
        helpMenu.setMnemonic('H');
        helpMenu.add(new JMenuItem(getInfoAction()));
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenuBar createViewMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(rb.getString("File"));
        fileMenu.setMnemonic('F');
        fileMenu.add(new JMenuItem(getExitAction()));
        menuBar.add(fileMenu);
        JMenu helpMenu = new JMenu(rb.getString("Help"));
        helpMenu.setMnemonic('H');
        helpMenu.add(new JMenuItem(getInfoAction()));
        menuBar.add(helpMenu);
        return menuBar;
    }

    public Action getChannelDownAction() {
        if (this.channelDownAction == null) {
            this.channelDownAction = new ChannelDownAction();
        }
        return channelDownAction;
    }

    public Action getChannelUpAction() {
        if (this.channelUpAction == null) {
            this.channelUpAction = new ChannelUpAction();
        }
        return channelUpAction;
    }

    public Action getCompactViewAction() {
        if (this.compactViewAction == null) {
            this.compactViewAction = new CompactViewAction();
        }
        return compactViewAction;
    }

    public Action getArticleListViewAction() {
        if (this.articleListViewAction == null) {
            this.articleListViewAction = new RssView.ArticleListViewAction();
        }
        return articleListViewAction;
    }

    public Action getStandardViewAction() {
        if (this.standardViewAction == null) {
            this.standardViewAction = new RssView.StandardViewAction();
        }
        return standardViewAction;
    }

    public Action getInfoAction() {
        if (this.infoAction == null) {
            this.infoAction = new InfoAction(this);
        }
        return infoAction;
    }

    public Action getOptionsAction() {
        if (this.optionsAction == null) {
            this.optionsAction = new OptionsAction();
        }
        return optionsAction;
    }

    public Action getExitAction() {
        if (exitAction == null) {
            this.exitAction = new ExitAction();
        }
        return exitAction;
    }

    public Action getNewChannelAction() {
        if (this.newChannelAction == null) {
            this.newChannelAction = new NewChannelAction(this);
        }
        return newChannelAction;
    }

    public Action getEditChannelAction() {
        if (this.editChannelAction == null) {
            this.editChannelAction = new EditChannelAction(this);
        }
        return editChannelAction;
    }

    public Action getDeleteChannelAction() {
        if (this.deleteChannelAction == null) {
            this.deleteChannelAction = new DeleteChannelAction();
        }
        return deleteChannelAction;
    }

    public Action getCopyChannelAction() {
        if (this.copyChannelAction == null) {
            this.copyChannelAction = new CopyChannelAction();
        }
        return copyChannelAction;
    }

    public Action getPasteChannelAction() {
        if (this.pasteChannelAction == null) {
            this.pasteChannelAction = new PasteChannelAction(this);
        }
        return pasteChannelAction;
    }

    public Action getMarkArticlesReadAction() {
        if (this.markArticlesReadAction == null) {
            this.markArticlesReadAction = new MarkArticlesReadAction();
        }
        return markArticlesReadAction;
    }

    public Action getUpdateArticlesAction() {
        if (this.updateArticlesAction == null) {
            this.updateArticlesAction = new UpdateArticlesAction();
        }
        return updateArticlesAction;
    }

    public Action getNewFolderAction() {
        if (this.newFolderAction == null) {
            this.newFolderAction = new AddFolderAction();
        }
        return newFolderAction;
    }

    public Action getEditFolderAction() {
        if (this.editFolderAction == null) {
            this.editFolderAction = new EditFolderAction();
        }
        return editFolderAction;
    }

    public Action getDeleteFolderAction() {
        if (this.deleteFolderAction == null) {
            this.deleteFolderAction = new RemoveFolderAction();
        }
        return deleteFolderAction;
    }

    public Action getFeedAssistentAction() {
        if (this.feedAssistantAction == null) {
            this.feedAssistantAction = new FeedAssistentAction();
        }
        return feedAssistantAction;
    }

    private void initActions() {
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                getExitAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, rb.getString("windowClosing")));
            }
        });
    }

    private void updateSettings() {
        if (view != null) {
            Node nodes[] = this.selectionModel.getSelectionPath();
            String[] nodeNames = new String[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                nodeNames[i] = nodes[i].getName();
            }
            RssSettings.getInstance().setSetting(SELECTION_PATH, nodeNames);
        }
        if (mode != MODE_EXCLUSIVE) {
            RssSettings.getInstance().setViewSize(viewSize);
        }
    }

    protected void updateArticles() {
        fetcher.updateAtOnce();
    }

    public boolean isInteractiveMode() {
        return (mode == MODE_INTERACTIVE);
    }

    public int getViewSize() {
        return viewSize;
    }

    /**
	 * Sets the viewSize property.
	 * 
	 * @param viewSize new value of viewSize
	 */
    public void setViewSize(int viewSize) {
        if (viewSize < VIEWSIZE_NORMAL || viewSize > VIEWSIZE_EXCLUSIVE) {
            viewSize = VIEWSIZE_NORMAL;
        }
        int oldViewSize = this.viewSize;
        this.viewSize = viewSize;
        if (this.viewSize != oldViewSize) {
            firePropertyChange(VIEWSIZE_PROPERTY, oldViewSize, viewSize);
        }
    }

    protected void viewSizeChanged(PropertyChangeEvent e) {
        int newViewSize = ((Integer) e.getNewValue()).intValue();
        if (this.view != null) {
            view.deactivate();
        }
        switch(newViewSize) {
            case VIEWSIZE_EXCLUSIVE:
                getCompactViewAction().setEnabled(false);
                getStandardViewAction().setEnabled(false);
                this.view = new RssExclusiveView();
                break;
            case VIEWSIZE_SMALL:
                getCompactViewAction().setEnabled(false);
                getStandardViewAction().setEnabled(true);
                getArticleListViewAction().setEnabled(true);
                this.view = new RssCompactView();
                break;
            case VIEWSIZE_ARTICLE_LIST:
                getCompactViewAction().setEnabled(true);
                getStandardViewAction().setEnabled(true);
                getArticleListViewAction().setEnabled(false);
                this.view = new RssArticleBasedView();
                break;
            default:
                getCompactViewAction().setEnabled(true);
                getStandardViewAction().setEnabled(false);
                getArticleListViewAction().setEnabled(true);
                this.view = new RssStandardView();
                break;
        }
        this.setJMenuBar(null);
        getContentPane().removeAll();
        setVisible(false);
        view.setupFrame(this, componentRepository);
        view.activate();
        setVisible(true);
    }

    protected void showOptions() {
        RssOptionsDialog optionsDlg = new RssOptionsDialog(this);
        optionsDlg.pack();
        optionsDlg.setLocationRelativeTo(this);
        optionsDlg.show();
    }

    public RssChannelList getChannelList() {
        return channelList;
    }

    public RssArticleOverview getChannelTitle() {
        return channelTitle;
    }

    public void addChannelSelectionListener(ChannelSelectionListener listener) {
        this.selectionModel.addChannelSelectionListener(listener);
    }

    public void removeChannelSelectionListener(ChannelSelectionListener listener) {
        this.selectionModel.removeChannelSelectionListener(listener);
    }

    public void addArticleSelectionListener(ArticleSelectionListener listener) {
        this.articleSelectionModel.addArticleSelectionListener(listener);
    }

    public void removeArticleSelectionListener(ArticleSelectionListener listener) {
        this.articleSelectionModel.removeArticleSelectionListener(listener);
    }

    public Dimension getPreferredSize() {
        switch(viewSize) {
            case VIEWSIZE_SMALL:
                return RssSettings.getInstance().getSmallViewSize();
            case VIEWSIZE_EXCLUSIVE:
                return RssSettings.getInstance().getExclusiveViewSize();
            default:
                return RssSettings.getInstance().getNormalViewSize();
        }
    }

    protected void amphetaDeskChannelAdded(AmphetaDeskEvent evt) {
        if (mode == MODE_INTERACTIVE) {
            RssChannelDialog dialog = RssChannelDialog.showDialog(this, evt.getUrl());
            if (dialog.getDialogResult() == RssChannelDialog.YES_OPTION) {
                NodePath path = new NodePath(selectionModel.getSelectionPath());
                if (path.getLastElement() instanceof CategoryNode) {
                    channelModel.add((CategoryNode) path.getLastElement(), dialog.getChannel());
                } else {
                    channelModel.add(path.getLastCategory(), dialog.getChannel());
                }
            }
        } else {
            channelModel.remove(channelModel.getRootNode(), 0);
            RssChannel channel = new RssChannel();
            channel.setUrl(evt.getUrl());
            channel.setName(rb.getString("Linked_by_AmphetaDesk_Adapter"));
            Object path[] = selectionModel.getSelectionPath();
            channelModel.add((RssGroupNode) path[path.length - 1], channel);
        }
    }

    /**
	 * Getter for property channelModel.
	 * 
	 * @return Value of property channelModel.
	 */
    public ChannelModel getChannelModel() {
        return channelModel;
    }

    /**
	 * Setter for property channelModel.
	 * 
	 * @param channelModel New value of property channelModel.
	 */
    public void setChannelModel(ChannelModel channelModel) {
        this.channelModel = channelModel;
    }

    public RssChannelSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public RssArticleSelectionModel getArticleSelectionModel() {
        return articleSelectionModel;
    }

    private class ExitAction extends AbstractAction {

        public ExitAction() {
            super(rb.getString("Exit"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_EXIT_APP);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('E'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Exit_RSSView"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Exit_RSSView"));
        }

        /**
		 * Saves settings and exits RssView.
		 */
        public void actionPerformed(ActionEvent e) {
            shutdown();
            dispose();
            System.exit(0);
        }
    }

    private void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        view.deactivate();
        updateSettings();
        if (mode != MODE_EXCLUSIVE) {
            persistSubscriptionModel();
        }
        RssSettings.getInstance().saveSettings();
    }

    public static ResourceBundle getResourceBundle() {
        return rb;
    }

    private class NewChannelAction extends AbstractAction {

        private RssView parent;

        public NewChannelAction(RssView parent) {
            super(rb.getString("New"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_NEW_CHANNEL);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('N'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("New_Channel"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("New_Channel"));
            this.parent = parent;
        }

        public void actionPerformed(ActionEvent e) {
            RssChannelDialog dialog = RssChannelDialog.showDialog(parent);
            if (dialog.getDialogResult() == RssChannelDialog.YES_OPTION) {
                System.out.println("Adding Channel to channel model!!!");
                NodePath path = new NodePath(selectionModel.getSelectionPath());
                if (path.getLastElement() instanceof CategoryNode) {
                    System.out.println("LastPathElement");
                    channelModel.add((CategoryNode) path.getLastElement(), dialog.getChannel());
                } else {
                    System.out.println("LastCategory");
                    channelModel.add(path.getLastCategory(), dialog.getChannel());
                }
            }
        }
    }

    private class EditChannelAction extends AbstractAction {

        private RssView parent;

        public EditChannelAction(RssView parent) {
            super(rb.getString("Edit"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_EDIT_CHANNEL);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('E'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Edit_Channel"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Edit_Channel"));
            this.parent = parent;
        }

        public void actionPerformed(ActionEvent e) {
            Object path[] = selectionModel.getSelectionPath();
            Object obj = path[path.length - 1];
            if ((obj != null) && obj instanceof RssChannel) {
                RssChannel channel = (RssChannel) obj;
                RssChannelDialog dialog = RssChannelDialog.showDialog(parent, channel);
                if (RssChannelDialog.YES_OPTION == dialog.getDialogResult()) {
                    channelModel.update(channel);
                }
            } else {
                JOptionPane.showMessageDialog(null, rb.getString("select_channel"), rb.getString("Error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    private class DeleteChannelAction extends AbstractAction {

        public DeleteChannelAction() {
            super(rb.getString("Delete"));
            KeyStroke del = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
            super.putValue(Action.ACCELERATOR_KEY, del);
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_DELETE_CHANNEL);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('D'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Delete_Channel"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Delete_Channel"));
        }

        public void actionPerformed(ActionEvent e) {
            NodePath path = new NodePath(selectionModel.getSelectionPath());
            Node node = path.getLastElement();
            if ((node != null) && node instanceof Channel) {
                Channel channel = (Channel) node;
                int option = JOptionPane.showConfirmDialog(null, rb.getString("really_delete") + " " + channel.getName() + "?", rb.getString("Confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (option == JOptionPane.YES_OPTION) {
                    channelModel.remove(path);
                }
            }
        }
    }

    private class PasteChannelAction extends AbstractAction {

        private RssView parent;

        public PasteChannelAction(RssView parent) {
            super(rb.getString("Paste"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_PASTE_URL);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('P'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Paste_Channel_URL"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Paste_Channel_URL"));
            this.parent = parent;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String s = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                    RssChannelDialog dialog = RssChannelDialog.showDialog(parent, s);
                    if (dialog.getDialogResult() == RssChannelDialog.YES_OPTION) {
                        NodePath path = new NodePath(selectionModel.getSelectionPath());
                        if (path.getLastElement() instanceof CategoryNode) {
                            System.out.println("LastPathElement");
                            channelModel.add((CategoryNode) path.getLastElement(), dialog.getChannel());
                        } else {
                            System.out.println("LastCategory");
                            channelModel.add(path.getLastCategory(), dialog.getChannel());
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    private class CopyChannelAction extends AbstractAction {

        public CopyChannelAction() {
            super(rb.getString("Copy"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_COPY_URL);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('C'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Copy_Channel_URL"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Copy_Channel_URL"));
        }

        public void actionPerformed(ActionEvent e) {
            Object path[] = selectionModel.getSelectionPath();
            Object node = path[path.length - 1];
            if ((node != null) && node instanceof Channel) {
                Channel channel = (Channel) node;
                if (channel == null) {
                    JOptionPane.showMessageDialog(null, rb.getString("select_channel"), rb.getString("Error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(new StringSelection(channel.getUrl()), null);
            }
        }
    }

    private class InfoAction extends AbstractAction {

        private RssView parent;

        public InfoAction(RssView parent) {
            super(rb.getString("About"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_ABOUT);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('A'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("About_RssView"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("About_RssView"));
            this.parent = parent;
        }

        public void actionPerformed(ActionEvent e) {
            RssInfoDialog.showInfo(parent);
        }
    }

    private class OptionsAction extends AbstractAction {

        public OptionsAction() {
            super(rb.getString("Options"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_OPTIONS);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('O'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Options"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Options"));
        }

        public void actionPerformed(ActionEvent e) {
            showOptions();
        }
    }

    private class CompactViewAction extends AbstractAction {

        public CompactViewAction() {
            super(rb.getString("Compact"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_COMPACT_VIEW);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('c'));
            super.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(rb.getString("alt_2")));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Compact_View"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Compact_View"));
        }

        public void actionPerformed(ActionEvent e) {
            setViewSize(VIEWSIZE_SMALL);
        }
    }

    private class ArticleListViewAction extends AbstractAction {

        public ArticleListViewAction() {
            super(rb.getString("Article_List"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_ARTICLELIST_VIEW);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('L'));
            super.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(rb.getString("alt_3")));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Article_List_View"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Article_List_View"));
        }

        public void actionPerformed(ActionEvent e) {
            setViewSize(VIEWSIZE_ARTICLE_LIST);
        }
    }

    private class StandardViewAction extends AbstractAction {

        public StandardViewAction() {
            super(rb.getString("Standard"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_STANDARD_VIEW);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('S'));
            super.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(rb.getString("alt_1")));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Standard_View"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Standard_View"));
        }

        public void actionPerformed(ActionEvent e) {
            setViewSize(VIEWSIZE_NORMAL);
        }
    }

    private class ChannelUpAction extends AbstractAction {

        public ChannelUpAction() {
            super(rb.getString("Up"));
            ImageIcon icon = IconContainer.getIconSet().getIcon(IconSet.ICON_UP);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('U'));
            KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK);
            super.putValue(Action.ACCELERATOR_KEY, up);
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Move_Channel_Up"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Move_Channel_Up"));
        }

        public void actionPerformed(ActionEvent e) {
            channelList.moveUp();
        }
    }

    private class ChannelDownAction extends AbstractAction {

        public ChannelDownAction() {
            super(rb.getString("Down"));
            KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
            super.putValue(Action.ACCELERATOR_KEY, up);
            IconSet iconSet = IconContainer.getIconSet();
            ImageIcon icon = iconSet.getIcon(IconSet.ICON_DOWN);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('D'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Move_Channel_Down"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Move_Channel_Down"));
        }

        public void actionPerformed(ActionEvent e) {
            channelList.moveDown();
        }
    }

    private class EditFolderAction extends AbstractAction {

        public EditFolderAction() {
            super(rb.getString("Edit_Folder"));
            IconSet iconSet = IconContainer.getIconSet();
            ImageIcon icon = iconSet.getIcon(IconSet.ICON_FOLDER_EDIT);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('E'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Change_Folder_name"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Change_Folder_name"));
        }

        public void actionPerformed(ActionEvent e) {
            NodePath path = new NodePath(selectionModel.getSelectionPath());
            if (path.getLastElement() instanceof RssGroupNode) {
                RssGroupNode node = (RssGroupNode) path.getLastElement();
                RssFolderDialog dialog = RssFolderDialog.showEditFolderDialog(RssView.this, node.getName());
                if (RssFolderDialog.YES_OPTION == dialog.getDialogResult()) {
                    node.setName(dialog.getFolderName());
                    channelModel.update(node);
                }
            }
        }
    }

    private class AddFolderAction extends AbstractAction {

        public AddFolderAction() {
            super(rb.getString("New_Folder"));
            IconSet iconSet = IconContainer.getIconSet();
            ImageIcon icon = iconSet.getIcon(IconSet.ICON_FOLDER_NEW);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('N'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Create_a_new_Folder"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Create_a_new_Folder"));
        }

        public void actionPerformed(ActionEvent e) {
            NodePath path = new NodePath(selectionModel.getSelectionPath());
            RssGroupNode parent = null;
            if (path.getLastElement() instanceof RssGroupNode) {
                parent = (RssGroupNode) path.getLastElement();
            } else {
                parent = path.getLastCategory();
            }
            RssFolderDialog dialog = RssFolderDialog.showNewFolderDialog(RssView.this, rb.getString("folder"));
            if (RssFolderDialog.YES_OPTION == dialog.getDialogResult()) {
                String name = dialog.getFolderName();
                channelModel.add(parent, new RssGroupNode(name));
            }
        }
    }

    private class RemoveFolderAction extends AbstractAction {

        public RemoveFolderAction() {
            super(rb.getString("Remove_Folder"));
            IconSet iconSet = IconContainer.getIconSet();
            ImageIcon icon = iconSet.getIcon(IconSet.ICON_FOLDER_DELETE);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('R'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Remove_Folder_from_Tree"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Remove_Folder_from_Tree"));
        }

        public void actionPerformed(ActionEvent e) {
            NodePath path = new NodePath(selectionModel.getSelectionPath());
            if (path.getLastElement() instanceof RssGroupNode) {
                RssGroupNode category = (RssGroupNode) path.getLastElement();
                if (category.containsChannels()) {
                    JOptionPane.showMessageDialog(null, rb.getString("error_folder_contains_channels"), rb.getString("Error"), JOptionPane.ERROR_MESSAGE);
                } else {
                    channelModel.remove(path);
                }
            }
        }
    }

    private class MarkArticlesReadAction extends AbstractAction {

        public MarkArticlesReadAction() {
            super(rb.getString("Mark_all_read"));
            IconSet iconSet = IconContainer.getIconSet();
            ImageIcon icon = iconSet.getIcon(IconSet.ICON_MARK_READ);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('M'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Mark_all_read"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Mark_all_read"));
        }

        public void actionPerformed(ActionEvent e) {
            channelList.markArticlesRead();
        }
    }

    private class UpdateArticlesAction extends AbstractAction {

        public UpdateArticlesAction() {
            super(rb.getString("Update"));
            IconSet iconSet = IconContainer.getIconSet();
            ImageIcon icon = iconSet.getIcon(IconSet.ICON_UPDATE);
            super.putValue(Action.SMALL_ICON, icon);
            super.putValue(Action.MNEMONIC_KEY, new Integer('U'));
            super.putValue(Action.LONG_DESCRIPTION, rb.getString("Update_Articles_immediatley"));
            super.putValue(Action.SHORT_DESCRIPTION, rb.getString("Update_Articles_immediatley"));
        }

        public void actionPerformed(ActionEvent e) {
            updateArticles();
        }
    }

    public class FeedAssistentAction extends AbstractAction {

        public FeedAssistentAction() {
            super("", IconContainer.getIconSet().getIcon(IconSet.ICON_ASSISTENT));
        }

        /**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
        public void actionPerformed(ActionEvent e) {
            FeedAssistent assistent = new FeedAssistent(RssView.this);
            assistent.initialize("http://rssview.sf.net/feeds/directory.xml");
            assistent.pack();
            assistent.setLocationRelativeTo(RssView.this);
            assistent.show();
        }
    }

    /**
	 * Persists settings and article database at shutdown
	 */
    private class RssShutdownHook extends Thread {

        public void run() {
            shutdown();
        }
    }

    /**
	 * @param title
	 * @param url
	 */
    public void addFeed(String title, String url) {
        RssChannel channel = new RssChannel();
        channel.setName(title);
        channel.setUrl(url);
        channel.setActive(RssSettings.DEFAULT_ACTIVE);
        channel.setPollInterval(RssSettings.DEFAULT_POLLINTERVAL);
        channel.setArticlesInView(RssSettings.DEFAULT_ARTICLESCOUNT);
        channel.setRememberArticlesEnabled(RssSettings.DEFAULT_REMEMBER);
        channel.setBeepEnabled(RssSettings.DEFAULT_BEEP);
        getChannelModel().add(getChannelModel().getRootNode(), channel);
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}

package net.sourceforge.rssowl.controller;

import net.sourceforge.rssowl.controller.dialog.AboutDialog;
import net.sourceforge.rssowl.controller.dialog.BlogrollDialog;
import net.sourceforge.rssowl.controller.dialog.CategoryDialog;
import net.sourceforge.rssowl.controller.dialog.ConfirmDeleteDialog;
import net.sourceforge.rssowl.controller.dialog.FavoriteDialog;
import net.sourceforge.rssowl.controller.dialog.FeedDiscoveryDialog;
import net.sourceforge.rssowl.controller.dialog.FeedSearchDialog;
import net.sourceforge.rssowl.controller.dialog.ImportOPMLDialog;
import net.sourceforge.rssowl.controller.dialog.OpenFeedDialog;
import net.sourceforge.rssowl.controller.dialog.PreferencesDialog;
import net.sourceforge.rssowl.controller.dialog.SearchDialog;
import net.sourceforge.rssowl.controller.dialog.ToolBarDialog;
import net.sourceforge.rssowl.controller.dialog.ValidateFeedDialog;
import net.sourceforge.rssowl.controller.forms.Hyperlink;
import net.sourceforge.rssowl.controller.thread.AggregationLoader;
import net.sourceforge.rssowl.controller.thread.SettingsManager;
import net.sourceforge.rssowl.controller.thread.UpdateManager;
import net.sourceforge.rssowl.dao.Exporter;
import net.sourceforge.rssowl.dao.Importer;
import net.sourceforge.rssowl.dao.SettingsLoader;
import net.sourceforge.rssowl.dao.SettingsSaver;
import net.sourceforge.rssowl.model.Category;
import net.sourceforge.rssowl.model.Channel;
import net.sourceforge.rssowl.model.Favorite;
import net.sourceforge.rssowl.model.NewsItem;
import net.sourceforge.rssowl.model.TabItemData;
import net.sourceforge.rssowl.model.TableItemData;
import net.sourceforge.rssowl.model.TreeItemData;
import net.sourceforge.rssowl.util.CryptoManager;
import net.sourceforge.rssowl.util.GlobalSettings;
import net.sourceforge.rssowl.util.search.SearchDefinition;
import net.sourceforge.rssowl.util.shop.BlogShop;
import net.sourceforge.rssowl.util.shop.BrowserShop;
import net.sourceforge.rssowl.util.shop.FileShop;
import net.sourceforge.rssowl.util.shop.FontShop;
import net.sourceforge.rssowl.util.shop.PaintShop;
import net.sourceforge.rssowl.util.shop.PrintShop;
import net.sourceforge.rssowl.util.shop.ProxyShop;
import net.sourceforge.rssowl.util.shop.RegExShop;
import net.sourceforge.rssowl.util.shop.SimpleFileShop;
import net.sourceforge.rssowl.util.shop.StringShop;
import net.sourceforge.rssowl.util.shop.URLShop;
import net.sourceforge.rssowl.util.shop.WidgetShop;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jdom.JDOMException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeSet;

/**
 * The Event Manager is responsible to react on user-action. User action may for
 * example occur when any item from the main menu is selected.
 * 
 * @author <a href="mailto:bpasero@rssowl.org">Benjamin Pasero </a>
 * @version 1.2.3
 */
public class EventManager {

    /** Edit Menu Action: Copy */
    static final int EDIT_ACTION_COPY = 1;

    /** Edit Menu Action: Cut */
    static final int EDIT_ACTION_CUT = 0;

    /** Edit Menu Action: Delete */
    static final int EDIT_ACTION_DELETE = 3;

    /** Edit Menu Action: Paste */
    static final int EDIT_ACTION_PASTE = 2;

    /** Edit Menu Action: Properties */
    static final int EDIT_ACTION_PROPERTIES = 5;

    /** Edit Menu Action: Select All */
    static final int EDIT_ACTION_SELECTALL = 4;

    private Clipboard cb;

    private Display display;

    private FavoritesTree rssOwlFavoritesTree;

    private GUI rssOwlGui;

    private NewsTabFolder rssOwlNewsTabFolder;

    private NewsText rssOwlNewsText;

    private Quickview rssOwlQuickView;

    private Shell shell;

    ToolBarDialog toolBarDialog;

    /**
   * Instantiate a new EventManager
   * 
   * @param display The display
   * @param shell The shell
   * @param rssOwlGui The Maincontroller
   */
    public EventManager(Display display, Shell shell, GUI rssOwlGui) {
        this.display = display;
        this.shell = shell;
        this.rssOwlGui = rssOwlGui;
        cb = new Clipboard(display);
        syncControls();
    }

    /**
   * Add displayed newsfeed to favorites
   */
    public void actionAddToFavorites() {
        String favUrl = "";
        String favTitle = "";
        TabItemData data = (TabItemData) rssOwlNewsTabFolder.getNewsHeaderTabFolder().getSelection().getData();
        if (data.getUrl() != null) favUrl = data.getUrl(); else if (Category.getLinkForTitle(data.getTitle()) != null) favUrl = Category.getLinkForTitle(data.getTitle());
        if (data.getTitle() != null) favTitle = data.getTitle();
        if (data.isAggregatedCat()) {
            favUrl = "";
            favTitle = "";
        }
        actionAddToFavorites(favTitle, favUrl);
    }

    /**
   * Add displayed newsfeed to favorites
   * 
   * @param title Title of the favorite
   * @param url URL of the favorite
   */
    public void actionAddToFavorites(String title, String url) {
        actionNewFavorite(url, title, rssOwlFavoritesTree.getSelectedCat());
    }

    /**
   * Open all favorits
   */
    public void actionAggregateAllCategories() {
        actionAggregateCategory(Category.getRootCategory(), SearchDefinition.NO_SEARCH, false);
    }

    /**
   * Open all favorits of the selected category and subcategorys
   * 
   * @param reload TRUE if the category is only reloaded and not displayed
   */
    public void actionAggregateCategory(boolean reload) {
        actionAggregateCategory(rssOwlFavoritesTree.getSelectedCat(), SearchDefinition.NO_SEARCH, reload);
    }

    /**
   * Export selected category into OPML format
   */
    public void actionCategoryToOPML() {
        actionCategoryToOPML(rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true)));
    }

    /**
   * Export selected category into OPML format
   * 
   * @param selectedCat The category to export into OPML.
   */
    public void actionCategoryToOPML(Category selectedCat) {
        actionCategoryToOPML(selectedCat, selectedCat.getName() + ".opml");
    }

    /**
   * Export selected category into OPML format
   * 
   * @param selectedCat The category to export into OPML.
   * @param fileName A file name suggestion.
   */
    public void actionCategoryToOPML(Category selectedCat, String fileName) {
        try {
            new Exporter().exportCategoryToOPML(selectedCat);
            FileShop.exportCategory(shell, fileName, new String[] { "*.opml", "*.xml", "*.*" });
        } catch (IOException e) {
            MessageBoxFactory.showError(shell, e);
        }
    }

    /**
   * Close displayed tab
   */
    public void actionCloseCurrent() {
        rssOwlNewsTabFolder.closeCurrent();
    }

    /**
   * Copy the location of the blogroll XML to clipboard
   */
    public void actionCopyBlogrollXmlLocation() {
        Category rssOwlCategory = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
        while (rssOwlCategory != null && rssOwlCategory.getParent().isBlogroll()) rssOwlCategory = rssOwlCategory.getParent();
        if (rssOwlCategory != null && StringShop.isset(rssOwlCategory.getPathToBlogroll())) cb.setContents(new Object[] { rssOwlCategory.getPathToBlogroll() }, new Transfer[] { TextTransfer.getInstance() });
    }

    /**
   * Copy the XML location of the selected favorite
   */
    public void actionCopyFavoritesXmlLocation() {
        TreeItemData data = (TreeItemData) rssOwlFavoritesTree.getFavoritesTree().getSelection()[0].getData();
        String selection = data.getName();
        String xmlLocation = Category.getLinkForTitle(selection);
        if (xmlLocation == null) xmlLocation = ((Favorite) rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true)).getFavorites().get(selection)).getUrl();
        cb.setContents(new Object[] { xmlLocation }, new Transfer[] { TextTransfer.getInstance() });
    }

    /**
   * Bring up the ToolBarDialog to customize the ToolBar.
   */
    public void actionCustomizeToolBar() {
        if (toolBarDialog != null && WidgetShop.isset(toolBarDialog.getShell())) {
            toolBarDialog.getShell().forceActive();
            return;
        }
        if (!GlobalSettings.isToolBarShown) rssOwlQuickView.setShowToolBar(true, false);
        toolBarDialog = new ToolBarDialog(rssOwlQuickView, shell, GUI.i18n.getTranslation("POP_CUSTOMIZE_TOOLBAR"));
        if (toolBarDialog.open() == Window.OK && GUI.isAlive()) {
            boolean requiresUpdate = false;
            String newToolBar = toolBarDialog.getToolBarItems();
            int newMode = toolBarDialog.getToolBarMode();
            boolean newUseSmallIcons = toolBarDialog.isUseSmallIcons();
            if (!newToolBar.equals(GlobalSettings.toolBarItems) || newMode != GlobalSettings.toolBarMode || newUseSmallIcons != GlobalSettings.useSmallIcons) requiresUpdate = true;
            GlobalSettings.isToolBarShown = true;
            GlobalSettings.toolBarItems = newToolBar;
            GlobalSettings.toolBarMode = newMode;
            GlobalSettings.useSmallIcons = newUseSmallIcons;
            if (requiresUpdate) {
                rssOwlQuickView.createToolBar();
                SettingsManager.getInstance().requestSave();
            }
        } else if (GUI.isAlive()) {
            if (!GlobalSettings.isToolBarShown) rssOwlQuickView.setShowToolBar(false, false);
        }
        toolBarDialog = null;
    }

    /**
   * Remove the selected blogroll
   */
    public void actionDeleteBlogroll() {
        int confirmDel = 0;
        if (GlobalSettings.confirmBlogrollDeletion) confirmDel = new ConfirmDeleteDialog(shell, GUI.i18n.getTranslation("MESSAGEBOX_TITLE_CONFIRM_DELETE"), GUI.i18n.getTranslation("LABEL_DELETE_SUBSCRIPTION"), GUI.i18n.getTranslation("QUESTION_DEL_SUB"), ConfirmDeleteDialog.TYPE_BLOGROLL).open();
        if (confirmDel == 0) {
            Category rssOwlCategory = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
            while (rssOwlCategory != null && rssOwlCategory.getParent().isBlogroll()) {
                rssOwlCategory.getParent().removeCategory(rssOwlCategory.getName(), rssOwlCategory.getParent());
                rssOwlCategory = rssOwlCategory.getParent();
            }
            if (rssOwlCategory != null) {
                Category.getBlogrolls().remove(rssOwlCategory.getPathToBlogroll());
                rssOwlCategory.getParent().removeCategory(rssOwlCategory.getName(), rssOwlCategory.getParent());
            }
            TreeItem rootBlogrollItem = rssOwlFavoritesTree.getFavoritesTree().getSelection()[0];
            while (rootBlogrollItem.getParentItem() != null && ((TreeItemData) rootBlogrollItem.getParentItem().getData()).isBlogroll()) rootBlogrollItem = rootBlogrollItem.getParentItem();
            rootBlogrollItem.dispose();
            SettingsManager.getInstance().requestSave();
        }
    }

    /**
   * Delete the selected category
   */
    public void actionDeleteCategory() {
        TreeItemData selectedTreeItemData = rssOwlFavoritesTree.getSelectedTreeItemData();
        int confirmDel = 0;
        if (selectedTreeItemData == null) return;
        if (GlobalSettings.confirmCategoryDeletion) {
            String dialogMessage = StringShop.printf(GUI.i18n.getTranslation("QUESTION_DEL_CAT"), new String[] { "%NAME%" }, new String[] { selectedTreeItemData.getName() });
            confirmDel = new ConfirmDeleteDialog(shell, GUI.i18n.getTranslation("MESSAGEBOX_TITLE_CONFIRM_DELETE"), GUI.i18n.getTranslation("LABEL_DELETE_CATEGORY"), dialogMessage, ConfirmDeleteDialog.TYPE_CATEGORY).open();
        }
        if (confirmDel == 0) {
            Category selectedCat = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
            Category parentCat = selectedCat.getParent();
            parentCat.removeCategory(selectedCat.getName(), parentCat);
            if (WidgetShop.isset(selectedCat.getTreeItem())) selectedCat.getTreeItem().dispose();
            if (GlobalSettings.useSystemTray() && GlobalSettings.showSystrayIcon) {
                boolean treeHasUnreadFavs = rssOwlFavoritesTree.getTreeHasUnreadFavs();
                rssOwlGui.getRSSOwlSystray().setTrayItemState(treeHasUnreadFavs);
            }
            SettingsManager.getInstance().requestSave();
        }
    }

    /**
   * Delete the selected feed
   */
    public void actionDeleteFavorite() {
        TreeItemData selectedTreeItemData = rssOwlFavoritesTree.getSelectedTreeItemData();
        int confirmDel = 0;
        if (selectedTreeItemData == null) return;
        if (GlobalSettings.confirmFavoriteDeletion) {
            String dialogMessage = StringShop.printf(GUI.i18n.getTranslation("QUESTION_DEL_FAV"), new String[] { "%NAME%" }, new String[] { selectedTreeItemData.getName() });
            confirmDel = new ConfirmDeleteDialog(shell, GUI.i18n.getTranslation("MESSAGEBOX_TITLE_CONFIRM_DELETE"), GUI.i18n.getTranslation("LABEL_DELETE_FAVORITE"), dialogMessage, ConfirmDeleteDialog.TYPE_FAVORITE).open();
        }
        if (confirmDel == 0) {
            Category selectedCat = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
            selectedCat.removeFavorite(selectedTreeItemData.getName(), true, true);
            if (selectedTreeItemData.getFavorite() != null) CryptoManager.getInstance().removeItem(selectedTreeItemData.getFavorite().getUrl());
            if (GlobalSettings.useSystemTray() && GlobalSettings.showSystrayIcon) {
                boolean treeHasUnreadFavs = rssOwlFavoritesTree.getTreeHasUnreadFavs();
                rssOwlGui.getRSSOwlSystray().setTrayItemState(treeHasUnreadFavs);
            }
            SettingsManager.getInstance().requestSave();
        }
    }

    /**
   * Open the dialog to discover newsfeeds on a website with the given URL.
   * 
   * @param url The URL to discover newsfeeds on
   */
    public void actionDiscoverFeeds(String url) {
        new FeedDiscoveryDialog(url, GUI.i18n.getTranslation("MENU_FEED_DISCOVERY"), GUI.i18n.getTranslation("DIALOG_MESSAGE_ENTER_URL"), rssOwlGui).open();
    }

    /**
   * Edit the selected Blogroll
   */
    public void actionEditBlogroll() {
        Category blogrollRootCategory = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
        while (blogrollRootCategory != null && blogrollRootCategory.getParent().isBlogroll()) blogrollRootCategory = blogrollRootCategory.getParent();
        new BlogrollDialog(rssOwlGui, shell, GUI.i18n.getTranslation("DIALOG_EDIT_BLOGROLL_TITLE"), GUI.i18n.getTranslation("DIALOG_ADD_SUBSCRIPTION_MESSAGE"), blogrollRootCategory).open();
    }

    /**
   * Edit a category
   */
    public void actionEditCategory() {
        Category selectedCat = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
        CategoryDialog editCategoryDialog = new CategoryDialog(shell, GUI.i18n.getTranslation("DIALOG_EDIT_CATEGORY_TITLE"), GUI.i18n.getTranslation("DIALOG_ADD_CATEGORY_MESSAGE"), selectedCat.getName(), selectedCat.getParent());
        if (editCategoryDialog.open() == Window.OK) {
            String newTitle = editCategoryDialog.getCatName();
            if (selectedCat.getParent().editCategory(selectedCat.getName(), newTitle)) {
                GlobalSettings.selectedTreeItem = selectedCat.toCatPath();
                rssOwlFavoritesTree.buildFavoritesTree();
                SettingsManager.getInstance().requestSave();
            } else MessageBoxFactory.showMessage(shell, SWT.ICON_WARNING | SWT.OK, GUI.i18n.getTranslation("MESSAGEBOX_TITLE_ATTENTION"), GUI.i18n.getTranslation("MESSAGEBOX_CAT_EXISTS") + "!");
        }
    }

    /**
   * Edit a favorite
   */
    public void actionEditFavorite() {
        Favorite rssOwlFavorite = rssOwlFavoritesTree.getSelectedFav();
        FavoriteDialog.isEditFavorite = true;
        FavoriteDialog rssOwlEditFavoriteDialog = new FavoriteDialog(rssOwlFavorite, shell, GUI.i18n.getTranslation("DIALOG_EDIT_FAVORITE_TITLE") + ": " + rssOwlFavorite.getTitle());
        if (rssOwlEditFavoriteDialog.open() == Window.OK) {
            Favorite updatedFavorite = rssOwlEditFavoriteDialog.getUpdatedFavorite();
            String oldTitle = rssOwlFavorite.getTitle();
            String oldUrl = rssOwlFavorite.getUrl();
            String newTitle = updatedFavorite.getTitle();
            String newUrl = updatedFavorite.getUrl();
            if (!rssOwlFavorite.getRSSOwlCategory().toCatPath().equals(updatedFavorite.getCatPath())) {
                rssOwlFavorite.getRSSOwlCategory().removeFavorite(oldTitle, false, true);
                rssOwlFavoritesTree.addFavorite(updatedFavorite.getCatPath(), newUrl, newTitle, updatedFavorite);
                rssOwlFavoritesTree.buildFavoritesTree();
            } else if (!oldUrl.equals(newUrl) || !oldTitle.equals(newTitle)) {
                rssOwlFavorite.getRSSOwlCategory().editFavorite(rssOwlFavorite, updatedFavorite);
                GlobalSettings.selectedTreeItem = rssOwlFavorite.getRSSOwlCategory().toCatPath() + StringShop.CAT_TOKENIZER + rssOwlFavorite.getTitle();
                rssOwlFavoritesTree.buildFavoritesTree();
            } else {
                updatedFavorite.clone(rssOwlFavoritesTree.getSelectedFav());
            }
            if (!oldUrl.equals(newUrl)) CryptoManager.getInstance().removeItem(oldUrl);
            if (ProxyShop.isUseProxy()) actionSetProxyOnTreeItem(updatedFavorite.isUseProxy());
            SettingsManager.getInstance().requestSave();
        }
    }

    /**
   * Export settings to local file
   * 
   * @param parent The parent Shell of the Dialog
   */
    public void actionExportSettings(Shell parent) {
        try {
            new SettingsSaver(rssOwlGui).saveUserSettings(false);
            FileShop.exportUserSettings(parent);
        } catch (IOException e) {
            MessageBoxFactory.showError(shell, e);
        }
    }

    /**
   * Generate OPML from selected favorite
   */
    public void actionFavoriteToOPML() {
        Favorite selectedFav = rssOwlFavoritesTree.getSelectedFav();
        Category selectedCat = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
        try {
            new Exporter().exportFavoriteToOPML(selectedFav, selectedCat);
            FileShop.exportCategory(shell, selectedFav.getTitle() + ".opml", new String[] { "*.opml", "*.xml", "*.*" });
        } catch (IOException e) {
            MessageBoxFactory.showError(shell, e);
        }
    }

    /**
   * The user has selected to open a TreeItem. In dependance of what kind of
   * data the TreeItem is representating, an action is performed.
   */
    public void actionHandleTreeItemSelect() {
        rssOwlFavoritesTree.handleTreeItemSelect(true);
    }

    /**
   * Import a synchronized Blogroll
   */
    public void actionImportBlogroll() {
        new BlogrollDialog(rssOwlGui, shell, GUI.i18n.getTranslation("DIALOG_ADD_SUBSCRIPTION_TITLE"), GUI.i18n.getTranslation("DIALOG_ADD_SUBSCRIPTION_MESSAGE")).open();
    }

    /**
   * Import favorites using a OPML file
   */
    public void actionImportOPML() {
        new ImportOPMLDialog(rssOwlGui, shell, GUI.i18n.getTranslation("MENU_IMPORT_OPML"), GUI.i18n.getTranslation("MESSAGEBOX_FILL_URL")).open();
    }

    /**
   * Import settings from local file
   * 
   * @param parent The parent Shell of the Dialog
   * @return boolean TRUE if the import of settings was successfull
   */
    public boolean actionImportSettings(Shell parent) {
        int status = SimpleFileShop.OP_ABORTED;
        try {
            status = FileShop.importUserSettings(parent);
        } catch (IOException e) {
            MessageBoxFactory.showError(shell, e);
        }
        if (status == SimpleFileShop.OP_FAILED) MessageBoxFactory.showMessage(shell, SWT.ICON_WARNING, GUI.i18n.getTranslation("MENU_IMPORT"), GUI.i18n.getTranslation("MESSAGEBOX_WRONG_IMPORT")); else if (status == SimpleFileShop.OP_SUCCESSFULL) {
            SettingsLoader settingsLoader = new SettingsLoader(rssOwlGui);
            rssOwlGui.updateCoreSettings(settingsLoader, true);
            rssOwlGui.updateUserSettings(settingsLoader, true);
            rssOwlGui.updateI18N();
            rssOwlGui.changeWindowLayout();
            FontShop.setFontForAll(rssOwlGui);
            MessageBoxFactory.showMessage(shell, SWT.ICON_INFORMATION, GUI.i18n.getTranslation("MESSAGEBOX_TITLE_ATTENTION"), GUI.i18n.getTranslation("MESSAGEBOX_IMPORT_SUCCESS"));
        }
        return (status == SimpleFileShop.OP_SUCCESSFULL);
    }

    /**
   * Mark the favorites on all categories read
   */
    public void actionMarkAllCategoriesRead() {
        Category rootCategory = Category.getRootCategory();
        CTabItem tabItem = rssOwlNewsTabFolder.getTabItem(rootCategory);
        if (tabItem != null) {
            Table newsTable = ((TabItemData) tabItem.getData()).getNewsHeaderTable();
            if (WidgetShop.isset(newsTable)) NewsTable.markAllRead(newsTable);
            Channel aggregationChannel = ((TabItemData) tabItem.getData()).getChannel();
            Enumeration newsItems = aggregationChannel.getItems().elements();
            while (newsItems.hasMoreElements()) {
                NewsItem newsItem = (NewsItem) newsItems.nextElement();
                newsItem.setRead(true);
            }
            rssOwlNewsTabFolder.updateTabItemStatus(tabItem);
        }
        Hashtable subCategories = rootCategory.getSubCategories();
        Enumeration elements = subCategories.elements();
        while (elements.hasMoreElements()) actionMarkCategoryRead((Category) elements.nextElement(), false);
        if (GlobalSettings.useSystemTray() && GlobalSettings.showSystrayIcon) rssOwlGui.getRSSOwlSystray().setTrayItemState(false);
    }

    /**
   * Mark news of selected feed read
   */
    public void actionMarkAllNewsRead() {
        actionMarkAllNewsRead(rssOwlNewsTabFolder.getSelectedChannel());
    }

    /**
   * Mark the favorites on an entire category read
   */
    public void actionMarkCategoryRead() {
        actionMarkCategoryRead(rssOwlFavoritesTree.getSelectedCat(), true);
    }

    /**
   * Mark all news of the selected favorite read
   */
    public void actionMarkFavoriteRead() {
        actionMarkFavoriteRead(rssOwlFavoritesTree.getSelectedFav());
    }

    /**
   * Add a new category. In dependance of the Selection in the Favorites Tree,
   * this can mean to create a new Sub-Category or a normal Category.
   */
    public void actionNewCategory() {
        TreeItemData data = null;
        if (rssOwlFavoritesTree.getFavoritesTree().getSelection().length > 0) data = (TreeItemData) rssOwlFavoritesTree.getFavoritesTree().getSelection()[0].getData();
        if (data != null && (data.isBlogroll() || data.isCategory())) actionNewCategory(false); else actionNewCategory(true);
    }

    /**
   * Add a new category
   * 
   * @param asSubCategory If TRUE the new category will be created as
   * subcategory of the selected one.
   */
    public void actionNewCategory(boolean asSubCategory) {
        Category parent = Category.getRootCategory();
        TreeItemData data = null;
        if (rssOwlFavoritesTree.getFavoritesTree().getSelection().length > 0) data = (TreeItemData) rssOwlFavoritesTree.getFavoritesTree().getSelection()[0].getData();
        if (data != null && !data.isBlogroll() && !data.isBlogrollFavorite()) parent = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
        if (!asSubCategory && !parent.isRoot()) parent = parent.getParent(); else parent.setExpanded(true);
        actionNewCategory(shell, parent);
    }

    /**
   * Add a new category
   * 
   * @param parent The parent Shell of the Dialog
   * @param parentCategory The parent category of the new category
   * @return String The Name of the new Category
   */
    public String actionNewCategory(Shell parent, Category parentCategory) {
        CategoryDialog rssOwlAddCategoryDialog = new CategoryDialog(parent, GUI.i18n.getTranslation("DIALOG_ADD_CATEGORY_TITLE"), GUI.i18n.getTranslation("DIALOG_ADD_CATEGORY_MESSAGE"), parentCategory);
        if (rssOwlAddCategoryDialog.open() == Window.OK) {
            String catTitle = rssOwlAddCategoryDialog.getCatName();
            rssOwlFavoritesTree.addCategory(catTitle, parentCategory);
            rssOwlFavoritesTree.buildFavoritesTree();
            return catTitle;
        }
        return null;
    }

    /**
   * Add a new newsfeed favorite
   */
    public void actionNewFavorite() {
        boolean catSelected = false;
        if (rssOwlFavoritesTree.getFavoritesTree().getSelectionCount() > 0 && !rssOwlFavoritesTree.getSelectedCat().isBlogroll()) catSelected = true;
        actionNewFavorite("", "", (catSelected == true) ? rssOwlFavoritesTree.getSelectedCat() : null);
    }

    /**
   * Display the "New favorite" dialog and create the new favorite if possible
   * 
   * @param url Preset URL field of the dialog
   * @param title Preset title field of the dialog
   * @param selectedCategory The selected category, or NULL
   */
    public void actionNewFavorite(String url, String title, Category selectedCategory) {
        Favorite newFavorite = new Favorite(url, title, null);
        newFavorite.setCatPath((selectedCategory != null) ? selectedCategory.toCatPath() : "");
        newFavorite.setUseProxy(ProxyShop.isUseProxy());
        FavoriteDialog.isEditFavorite = false;
        FavoriteDialog rssOwlEditFavoriteDialog = new FavoriteDialog(newFavorite, shell, GUI.i18n.getTranslation("DIALOG_ADD_FAVORITE_TITLE"));
        if (rssOwlEditFavoriteDialog.open() != Window.OK) return;
        newFavorite = rssOwlEditFavoriteDialog.getUpdatedFavorite();
        newFavorite.setCreationDate(new Date().getTime());
        rssOwlFavoritesTree.addFavorite(newFavorite.getCatPath(), newFavorite.getUrl(), newFavorite.getTitle(), newFavorite);
        rssOwlFavoritesTree.buildFavoritesTree();
        if (!GlobalSettings.workOffline) rssOwlGui.reloadNewsFeed(newFavorite.getUrl());
    }

    /**
   * Open the FAQ on the Entry on how to configure the system to use the
   * Internal Browser on Linux.
   */
    public void actionOpenFAQOnBrowser() {
        SearchDefinition search = new SearchDefinition("Internal Browser on Linux", SearchDefinition.SCOPE_TITLE);
        if (new File("doc/faq.xml").exists()) rssOwlGui.loadNewsFeed("doc/faq.xml", search, true, true, NewsTabFolder.DISPLAY_MODE_SELECT_NEWS); else rssOwlGui.loadNewsFeed(URLShop.RSSOWL_FAQ, search, true, true, NewsTabFolder.DISPLAY_MODE_SELECT_NEWS);
    }

    /**
   * Open the FAQ. Either open locally if exist, otherwise load the FAQ from the
   * Homepage.
   */
    public void actionOpenFAQ() {
        if (new File("doc/faq.xml").exists()) rssOwlGui.loadNewsFeed("doc/faq.xml", SearchDefinition.NO_SEARCH, true, true, NewsTabFolder.DISPLAY_MODE_FOCUS); else rssOwlGui.loadNewsFeed(URLShop.RSSOWL_FAQ, SearchDefinition.NO_SEARCH, true, true, NewsTabFolder.DISPLAY_MODE_FOCUS);
    }

    /**
   * Open a newsfeed into the tabfolder
   * 
   * @param url The url of the newsfeed
   */
    public void actionOpenFeed(String url) {
        rssOwlGui.loadNewsFeed(url, SearchDefinition.NO_SEARCH, true, true, NewsTabFolder.DISPLAY_MODE_FOCUS);
    }

    /**
   * Open selected categorie's direct Favorites
   */
    public void actionOpenSelectedCategory() {
        Tree favoritesTree = rssOwlFavoritesTree.getFavoritesTree();
        if (favoritesTree.getSelectionCount() > 0) {
            TreeItem selectedCat = favoritesTree.getSelection()[0];
            if (selectedCat.getData() != null && ((TreeItemData) selectedCat.getData()).isBlogroll()) actionDeepSynchronizeBlogroll(rssOwlFavoritesTree.getSelectedCat());
            TreeItem childs[] = favoritesTree.getSelection()[0].getItems();
            for (int i = 0; i < childs.length; i++) {
                TreeItem item = childs[i];
                if (item.getData() != null) {
                    TreeItemData data = (TreeItemData) item.getData();
                    if (data.getFavorite() != null) {
                        String url = data.getFavorite().getUrl();
                        rssOwlGui.loadNewsFeed(url, SearchDefinition.NO_SEARCH, true, true, NewsTabFolder.DISPLAY_MODE_FOCUS_FIRST);
                        data.getFavorite().setLastVisitDate(System.currentTimeMillis());
                    }
                }
            }
        }
    }

    /**
   * Handle the supplied Link. Either open it as Feed or show the Dialog to add
   * a new Favorite.
   * 
   * @param link The Link to handle.
   */
    public void actionHandleSuppliedLink(String link) {
        if (Category.linkExists(link)) rssOwlGui.loadNewsFeed(link, SearchDefinition.NO_SEARCH, true, true, NewsTabFolder.DISPLAY_MODE_FOCUS); else rssOwlGui.eventManager.actionAddToFavorites("", link);
    }

    /**
   * Show RSSOwl's tutorial in the browser
   */
    public void actionOpenTutorial() {
        new TutorialBrowser(display, shell, rssOwlGui).show();
    }

    /**
   * Open the URL in the browser
   * 
   * @param url The URL to open
   */
    public void actionOpenURL(String url) {
        BrowserShop.openLinkInTab(url);
    }

    /**
   * Open the URL in the external browser
   * 
   * @param url The URL to open
   */
    public void actionOpenURLExternal(String url) {
        BrowserShop.openLink(url);
    }

    /**
   * Reload displayed newsfeed
   */
    public void actionReload() {
        rssOwlNewsTabFolder.reloadFeed();
    }

    /**
   * Reload all favorits
   */
    public void actionReloadAllCategories() {
        actionAggregateCategory(Category.getRootCategory(), SearchDefinition.NO_SEARCH, true);
    }

    /**
   * Load selected feed in the tree from source
   */
    public void actionReloadFeed() {
        rssOwlGui.reloadNewsFeed(rssOwlFavoritesTree.getSelectedFav().getUrl());
    }

    /**
   * Rename the selected TreeItem (in place)
   */
    public void actionRenameSelectedItem() {
        rssOwlFavoritesTree.renameSelectedItem();
    }

    /**
   * Search in all categories
   */
    public void actionSearchInAllCategories() {
        actionSearchInCategory(Category.getRootCategory());
    }

    /**
   * Search in an entire category
   */
    public void actionSearchInCategory() {
        actionSearchInCategory(rssOwlFavoritesTree.getSelectedCat());
    }

    /**
   * Perform fulltext search in a feed
   */
    public void actionSearchInFeed() {
        SearchDialog rssOwlSearchDialog = new SearchDialog(shell, GUI.i18n.getTranslation("SEARCH_DIALOG_TITLE"), GUI.i18n.getTranslation("SEARCH_DIALOG_MESSAGE"));
        if (rssOwlSearchDialog.open() == Window.OK) {
            SearchDefinition searchDefinition = rssOwlSearchDialog.getValue();
            if (!StringShop.isset(searchDefinition.getPattern())) return;
            TreeItemData data = (TreeItemData) rssOwlFavoritesTree.getFavoritesTree().getSelection()[0].getData();
            String url = Category.getLinkForTitle(data.getName());
            rssOwlGui.loadNewsFeed(url, searchDefinition, true, true, NewsTabFolder.DISPLAY_MODE_FOCUS);
        }
    }

    /**
   * Set / Unset the internal browser view on the newstext to have HTML
   * formatted.
   * 
   * @param enabled TRUE if the internal browser should be used to display the
   * newstext.
   */
    public void actionSetBrowserView(boolean enabled) {
        GlobalSettings.useBrowserForNewsText = enabled;
        rssOwlGui.getRSSOwlMenu().setUseBrowserForNewsText(enabled);
        rssOwlNewsText.updateNewsTextComposite();
        SettingsManager.getInstance().requestSave();
    }

    /**
   * Enable / Disable use of proxy for this category / favorite
   * 
   * @param enabled State of use proxy
   */
    public void actionSetProxyOnTreeItem(boolean enabled) {
        Category parent = null;
        TreeItemData data = (TreeItemData) rssOwlFavoritesTree.getFavoritesTree().getSelection()[0].getData();
        if (data.isFavorite()) {
            data.getFavorite().setUseProxy(enabled);
            parent = rssOwlFavoritesTree.getSelectedCat();
        } else if (data.isBlogroll()) {
            Category rssOwlCategory = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
            while (rssOwlCategory != null && rssOwlCategory.getParent().isBlogroll()) rssOwlCategory = rssOwlCategory.getParent();
            if (rssOwlCategory != null) {
                rssOwlCategory.setUseProxy(enabled);
                parent = rssOwlCategory.getParent();
            }
        } else {
            rssOwlFavoritesTree.getSelectedCat().setUseProxy(enabled);
            parent = rssOwlFavoritesTree.getSelectedCat().getParent();
        }
        if (parent != null) parent.checkUseProxy(parent);
    }

    /**
   * Synchronize the selected Blogroll
   */
    public void actionSynchronizeBlogroll() {
        Category blogrollRootCategory = rssOwlFavoritesTree.getSelectedCat(rssOwlFavoritesTree.getTreePath(true));
        while (blogrollRootCategory != null && blogrollRootCategory.getParent().isBlogroll()) blogrollRootCategory = blogrollRootCategory.getParent();
        actionSynchronizeBlogroll(blogrollRootCategory);
    }

    /**
   * Synchronize the selected Blogroll
   * 
   * @param blogrollRootCategory The blogroll to synchronize
   */
    public void actionSynchronizeBlogroll(Category blogrollRootCategory) {
        Importer importer = new Importer(blogrollRootCategory.getPathToBlogroll(), blogrollRootCategory.getName(), blogrollRootCategory);
        shell.setCursor(GUI.display.getSystemCursor(SWT.CURSOR_WAIT));
        try {
            importer.importNewsfeeds(true);
            blogrollRootCategory.setUnSynchronized(false);
        } catch (IOException e) {
            MessageBoxFactory.showError(shell, GUI.i18n.getTranslation("ERROR_FILE_NOT_FOUND") + ":\n" + blogrollRootCategory.getPathToBlogroll());
            blogrollRootCategory.setExpanded(false);
        } catch (JDOMException e) {
            MessageBoxFactory.showError(shell, e.getLocalizedMessage() + ":\n" + blogrollRootCategory.getPathToBlogroll());
            blogrollRootCategory.setExpanded(false);
        } catch (IllegalArgumentException e) {
            MessageBoxFactory.showError(shell, e.getLocalizedMessage() + ":\n" + blogrollRootCategory.getPathToBlogroll());
            blogrollRootCategory.setExpanded(false);
        }
        rssOwlGui.getRSSOwlFavoritesTree().buildFavoritesTree(true);
        shell.setCursor(null);
    }

    /**
   * Open the dialog to validate newsfeeds
   * 
   * @param autoValidate If TRUE, validate automatically after dialog was opened
   */
    public void actionValidateFeeds(boolean autoValidate) {
        CTabItem tabItem = rssOwlNewsTabFolder.getNewsHeaderTabFolder().getSelection();
        String link = null;
        if (tabItem != null) {
            TabItemData data = ((TabItemData) tabItem.getData());
            if (!data.isMessage()) link = ((TabItemData) tabItem.getData()).getUrl();
        }
        if (!autoValidate) {
            if (!StringShop.isset(link)) {
                Clipboard cb = GUI.rssOwlGui.getEventManager().getClipBoard();
                TextTransfer transfer = TextTransfer.getInstance();
                String data = (String) cb.getContents(transfer);
                data = (data != null) ? data.trim() : null;
                if (StringShop.isset(data) && RegExShop.isValidURL(data)) link = data;
            }
            ValidateFeedDialog validateFeedDialog = new ValidateFeedDialog(shell, GUI.i18n.getTranslation("MENU_VALIDATE"), GUI.i18n.getTranslation("MESSAGEBOX_FILL_URL"), link);
            if (validateFeedDialog.open() == Window.OK) link = validateFeedDialog.getFeedUrl(); else return;
        }
        if (StringShop.isset(link)) actionOpenURL(URLShop.FEED_VALIDATOR_URL + URLShop.urlEncode(link));
    }

    /**
   * Get the shared Clipboard for Copy and Paste.
   * 
   * @return ClipBoard The shared clipboard for Copy and Paste.
   */
    public Clipboard getClipBoard() {
        return cb;
    }

    /**
   * Get the main controls from the maincontroller
   */
    public void syncControls() {
        rssOwlFavoritesTree = rssOwlGui.getRSSOwlFavoritesTree();
        rssOwlNewsTabFolder = rssOwlGui.getRSSOwlNewsTabFolder();
        rssOwlQuickView = rssOwlGui.getRSSOwlQuickview();
        rssOwlNewsText = rssOwlGui.getRSSOwlNewsText();
    }

    /**
   * Open all favorits of the selected category and subcategorys
   * 
   * @param rssOwlCategory The category to aggregate
   * @param searchDefinition Pattern and Scope of the Search.
   * @param reload TRUE if the category is only reloaded and not displayed
   */
    void actionAggregateCategory(Category rssOwlCategory, SearchDefinition searchDefinition, boolean reload) {
        if (rssOwlCategory == null) return;
        if (reload) rssOwlGui.getFeedCacheManager().unCacheNewsfeed(rssOwlCategory.toCatPath(true), false);
        actionDeepSynchronizeBlogroll(rssOwlCategory);
        TreeSet allFavorites = new TreeSet(new Comparator() {

            public int compare(Object obj1, Object obj2) {
                return -1;
            }
        });
        rssOwlCategory.getAllFavoriteTitles(allFavorites, rssOwlCategory);
        if (allFavorites.size() > 0) {
            AggregationLoader rssMultiLoader = new AggregationLoader(allFavorites, rssOwlCategory, rssOwlGui, rssOwlCategory.toCatPath(true), searchDefinition);
            rssMultiLoader.setReload(reload);
            rssMultiLoader.loadFavorites(!reload);
        } else MessageBoxFactory.showMessage(shell, SWT.ICON_INFORMATION, GUI.i18n.getTranslation("MESSAGEBOX_TITLE_INFORMATION"), GUI.i18n.getTranslation("DIALOG_MESSAGE_CAT_EMPTY"));
    }

    /**
   * Blog displayed newsfeed
   */
    void actionBlogNews() {
        NewsItem selectedNewsItem = rssOwlNewsTabFolder.getSelectedNewsItem();
        if (selectedNewsItem != null) BlogShop.blogNews(selectedNewsItem);
    }

    /**
   * Check for an updated RSSOwl version
   */
    void actionCheckUpdate() {
        UpdateManager updateManager = new UpdateManager(rssOwlGui);
        updateManager.setDisplayNoNewVersionInfo(true);
        updateManager.start();
    }

    /**
   * Close all opened tabs
   */
    void actionCloseAll() {
        rssOwlNewsTabFolder.closeAll();
    }

    /**
   * Close all opened tabs expect the current one
   */
    void actionCloseOthers() {
        rssOwlNewsTabFolder.closeAll(true, false);
    }

    /**
   * Copy URL of selected newsitem
   */
    void actionCopyNewsUrl() {
        NewsItem rssNewsItem = rssOwlNewsTabFolder.getSelectedNewsItem();
        if (rssNewsItem != null && StringShop.isset(rssNewsItem.getLink())) cb.setContents(new Object[] { rssNewsItem.getLink() }, new Transfer[] { TextTransfer.getInstance() });
    }

    /**
   * Copy text into clipboard
   * 
   * @param text Any control capable of holding text
   */
    void actionCopyText(Control text) {
        if (text instanceof StyledText) {
            if (((StyledText) text).getText() != null) {
                if (((StyledText) text).getSelectionCount() > 0) cb.setContents(new Object[] { ((StyledText) text).getSelectionText() }, new Transfer[] { TextTransfer.getInstance() }); else cb.setContents(new Object[] { ((StyledText) text).getText() }, new Transfer[] { TextTransfer.getInstance() });
            }
        } else if (text instanceof Hyperlink) {
            if (((Hyperlink) text).getText() != null) {
                cb.setContents(new Object[] { ((Hyperlink) text).getText() }, new Transfer[] { TextTransfer.getInstance() });
            }
        }
    }

    /**
   * Recursivly check the category and all childs for unsynchronized Blogrolls.
   * Synchronize any of those.
   * 
   * @param category The category to look for unsynchronized Blogrolls.
   */
    void actionDeepSynchronizeBlogroll(Category category) {
        if (category.isBlogroll() && category.isUnSynchronized()) actionSynchronizeBlogroll(category); else if (!category.isBlogroll()) {
            Enumeration subCategories = category.getSubCategories().elements();
            while (subCategories.hasMoreElements()) actionDeepSynchronizeBlogroll((Category) subCategories.nextElement());
        }
    }

    /**
   * Open the dialog to discover newsfeeds on a website
   */
    void actionDiscoverFeeds() {
        new FeedDiscoveryDialog(GUI.i18n.getTranslation("MENU_FEED_DISCOVERY"), GUI.i18n.getTranslation("DIALOG_MESSAGE_ENTER_URL"), rssOwlGui).open();
    }

    /**
   * Exit application
   */
    void actionExit() {
        rssOwlGui.onClose(new Event(), true);
        if (GUI.isClosing) display.dispose();
    }

    /**
   * Export displayed newsfeed to pdf, rtf or html
   * 
   * @param format PDF, RTF or HTML
   */
    void actionExportFeed(int format) {
        actionExportFeed(format, true);
    }

    /**
   * Export displayed newsfeed to pdf, rtf or html
   * 
   * @param format PDF, RTF or HTML
   * @param entireFeed If TRUE export the entire feed to the document. If FALSE,
   * get the newstitle and only generate the document from the selected
   * newsheader.
   */
    void actionExportFeed(int format, boolean entireFeed) {
        if (entireFeed) rssOwlNewsTabFolder.exportToDocument(format, null); else {
            NewsItem rssNewsItem = rssOwlNewsTabFolder.getSelectedNewsItem();
            if (rssNewsItem != null) rssOwlNewsTabFolder.exportToDocument(format, rssNewsItem.getTitle());
        }
    }

    /**
   * Goto next news (either read or unread)
   */
    void actionGotoNextNews() {
        NewsTable.actionDisplayNextNews(false);
    }

    /**
   * Goto next tab
   */
    void actionGotoNextTab() {
        rssOwlNewsTabFolder.gotoNextTab();
    }

    /**
   * Goto next unread news
   */
    void actionGotoNextUnreadNews() {
        NewsTable.actionDisplayNextNews(true);
    }

    /**
   * Goto previous tab
   */
    void actionGotoPreviousTab() {
        rssOwlNewsTabFolder.gotoPreviousTab();
    }

    /**
   * Create a newstip mail
   */
    void actionMailNewsTip() {
        NewsItem selectedNewsItem = rssOwlNewsTabFolder.getSelectedNewsItem();
        if (selectedNewsItem != null) BrowserShop.openLink(selectedNewsItem.toNewsTip());
    }

    /**
   * Mark news of given feed read and also update the table in the tabitem that
   * displays the feed.
   * 
   * @param channel The channel to mark read
   */
    void actionMarkAllNewsRead(Channel channel) {
        actionMarkAllNewsRead(channel, true);
    }

    /**
   * Mark news of given feed read
   * 
   * @param channel The channel to mark read
   * @param updateNewsTable If TRUE also update the table that is showing
   * newsitems in the tabfolder
   */
    void actionMarkAllNewsRead(Channel channel, boolean updateNewsTable) {
        if (channel == null) return;
        ArrayList feedXMLUrls = new ArrayList();
        Enumeration elements = channel.getItems().elements();
        while (elements.hasMoreElements()) {
            NewsItem rssNewsItem = (NewsItem) elements.nextElement();
            rssNewsItem.setRead(true);
            if (rssNewsItem.getNewsfeedXmlUrl() != null && !feedXMLUrls.contains(rssNewsItem.getNewsfeedXmlUrl())) feedXMLUrls.add(rssNewsItem.getNewsfeedXmlUrl());
            rssOwlGui.getArchiveManager().getArchive().addEntry(rssNewsItem);
        }
        if (channel.getLink() != null && Category.getFavPool().containsKey(channel.getLink())) {
            Favorite rssOwlFavorite = (Favorite) Category.getFavPool().get(channel.getLink());
            rssOwlFavorite.updateReadStatus(0);
        }
        if (channel.isAggregatedCat()) {
            for (int a = 0; a < feedXMLUrls.size(); a++) {
                String feedUrl = (String) feedXMLUrls.get(a);
                if (Category.getFavPool().containsKey(feedUrl)) {
                    Favorite rssOwlFavorite = (Favorite) Category.getFavPool().get(feedUrl);
                    rssOwlFavorite.updateReadStatus(0);
                }
            }
        }
        if (!updateNewsTable) return;
        CTabItem tabItem = rssOwlNewsTabFolder.getNewsHeaderTabFolder().getSelection();
        TabItemData data = (TabItemData) tabItem.getData();
        Table newsTable = (data != null) ? data.getNewsHeaderTable() : null;
        if (WidgetShop.isset(newsTable)) NewsTable.markAllRead(newsTable);
        rssOwlNewsTabFolder.updateTabItemStatus(tabItem);
    }

    /**
   * Mark the favorites on an entire category read
   * 
   * @param selectedCategory The category to mark read
   * @param updateSysTrayStatus Wether to update the Systemtray Status
   */
    void actionMarkCategoryRead(Category selectedCategory, boolean updateSysTrayStatus) {
        if (selectedCategory == null) return;
        Enumeration subCategories = selectedCategory.getSubCategories().elements();
        while (subCategories.hasMoreElements()) actionMarkCategoryRead((Category) subCategories.nextElement(), false);
        Enumeration allFavoritesIt = selectedCategory.getFavorites().elements();
        while (allFavoritesIt.hasMoreElements()) {
            Favorite rssOwlFavorite = (Favorite) allFavoritesIt.nextElement();
            actionMarkFavoriteRead(rssOwlFavorite, false);
        }
        CTabItem tabItem = rssOwlNewsTabFolder.getTabItem(selectedCategory);
        if (tabItem != null) {
            Table newsTable = ((TabItemData) tabItem.getData()).getNewsHeaderTable();
            if (WidgetShop.isset(newsTable)) NewsTable.markAllRead(newsTable);
            Channel aggregationChannel = ((TabItemData) tabItem.getData()).getChannel();
            Enumeration newsItems = aggregationChannel.getItems().elements();
            while (newsItems.hasMoreElements()) {
                NewsItem newsItem = (NewsItem) newsItems.nextElement();
                newsItem.setRead(true);
            }
            rssOwlNewsTabFolder.updateTabItemStatus(tabItem);
        }
        if (GlobalSettings.useSystemTray() && GlobalSettings.showSystrayIcon && updateSysTrayStatus) {
            boolean treeHasUnreadFavs = rssOwlFavoritesTree.getTreeHasUnreadFavs();
            rssOwlGui.getRSSOwlSystray().setTrayItemState(treeHasUnreadFavs);
        }
    }

    /**
   * Mark all news of the given favorite read
   * 
   * @param selectedFavorite The selected favorite containing news to mark read
   */
    void actionMarkFavoriteRead(Favorite selectedFavorite) {
        actionMarkFavoriteRead(selectedFavorite, true);
    }

    /**
   * Mark all news of the given favorite read
   * 
   * @param selectedFavorite The selected favorite containing news to mark read
   * @param updateSysTray Wether to update the Systemtray status
   */
    void actionMarkFavoriteRead(Favorite selectedFavorite, boolean updateSysTray) {
        if (selectedFavorite == null) return;
        String link = selectedFavorite.getUrl();
        if (rssOwlGui.getFeedCacheManager().isNewsfeedCached(link, selectedFavorite.unreadNewsAvailable())) {
            Channel channel = rssOwlGui.getFeedCacheManager().getCachedNewsfeed(link);
            if (channel != null) {
                Enumeration elements = channel.getItems().elements();
                while (elements.hasMoreElements()) {
                    NewsItem rssNewsItem = (NewsItem) elements.nextElement();
                    rssNewsItem.setRead(true);
                    rssOwlGui.getArchiveManager().getArchive().addEntry(rssNewsItem);
                }
            }
        }
        CTabItem tabItem = rssOwlNewsTabFolder.getFeedTabItem(link);
        if (tabItem == null) tabItem = rssOwlNewsTabFolder.getFeedTabItem(selectedFavorite.getTitle());
        if (tabItem != null) {
            Table newsTable = ((TabItemData) tabItem.getData()).getNewsHeaderTable();
            if (WidgetShop.isset(newsTable)) NewsTable.markAllRead(newsTable);
            rssOwlNewsTabFolder.updateTabItemStatus(tabItem);
        }
        selectedFavorite.updateReadStatus(0, updateSysTray);
    }

    /**
   * Mark selected news unread
   */
    void actionMarkNewsUnread() {
        actionMarkNewsUnread(rssOwlNewsTabFolder.getSelectedNewsItem());
    }

    /**
   * Mark the given News unread.
   * 
   * @param newsItem The NewsItem to mark unread.
   */
    void actionMarkNewsUnread(NewsItem newsItem) {
        rssOwlGui.getArchiveManager().getArchive().removeEntry(newsItem);
        CTabItem tabItem = rssOwlNewsTabFolder.getNewsHeaderTabFolder().getSelection();
        TabItemData data = (TabItemData) tabItem.getData();
        Table newsHeaderTable = data.getNewsHeaderTable();
        if (WidgetShop.isset(newsHeaderTable)) {
            if (newsHeaderTable.getSelectionCount() <= 0) return;
            newsHeaderTable.getItem(newsHeaderTable.getSelectionIndex()).setData(TableItemData.createNewsheaderData(false));
            NewsTable.updateTableItemStyle(newsHeaderTable.getItem(newsHeaderTable.getSelectionIndex()));
        }
        newsItem.setRead(false);
        Channel channel = rssOwlNewsTabFolder.getSelectedChannel();
        String feedUrl = null;
        if (channel != null && channel.getLink() != null && Category.getFavPool().containsKey(channel.getLink())) feedUrl = channel.getLink(); else if (newsItem.getNewsfeedXmlUrl() != null && Category.getFavPool().containsKey(newsItem.getNewsfeedXmlUrl())) feedUrl = newsItem.getNewsfeedXmlUrl();
        if (feedUrl != null) {
            Favorite rssOwlFavorite = (Favorite) Category.getFavPool().get(feedUrl);
            if (!rssOwlFavorite.isErrorLoading() && channel != null) {
                if (channel.isAggregatedCat()) rssOwlFavorite.updateReadStatus(channel.getUnreadNewsCount(newsItem.getNewsfeedXmlUrl())); else rssOwlFavorite.updateReadStatus(channel.getUnreadNewsCount());
            }
        }
        rssOwlNewsTabFolder.updateTabItemStatus(tabItem);
    }

    /**
   * Minimize application window
   */
    void actionMinimizeWindow() {
        shell.setMinimized(true);
    }

    /**
   * Show the about dialog
   */
    void actionOpenAbout() {
        new AboutDialog(shell, GUI.i18n.getTranslation("MENU_ABOUT")).open();
    }

    /**
   * Open newsfeed from the given path
   */
    void actionOpenFeed() {
        OpenFeedDialog openFeedDialog = new OpenFeedDialog(shell, GUI.i18n.getTranslation("BUTTON_OPEN"), GUI.i18n.getTranslation("MESSAGEBOX_FILL_URL"));
        if (openFeedDialog.open() == Window.OK) {
            String feedPath = openFeedDialog.getFeedPath();
            if (StringShop.isset(feedPath)) actionOpenFeed(feedPath);
        }
    }

    /**
   * Open the URL of the given news in the browser
   * 
   * @param newsTitle Title of the news.
   * @param external If TURE, force URL to open externally.
   */
    void actionOpenNewsURL(String newsTitle, boolean external) {
        Channel channel = rssOwlNewsTabFolder.getSelectedChannel();
        if (channel == null) return;
        NewsItem rssNewsItem = (NewsItem) channel.getItems().get(newsTitle);
        if (rssNewsItem == null) return;
        String url = rssNewsItem.getLink();
        if (!StringShop.isset(url) && StringShop.isset(rssNewsItem.getGuid())) url = rssNewsItem.getGuid();
        if (!StringShop.isset(url)) return;
        if (external) actionOpenURLExternal(url); else actionOpenURL(url);
    }

    /**
   * Show RSSOwl's preferences in a dialog
   */
    void actionOpenPreferences() {
        new PreferencesDialog(shell, GUI.i18n.getTranslation("MENU_PREFERENCES"), rssOwlGui).open();
    }

    /**
   * Open the URL in the browser
   * 
   * @param url The URL to open
   * @param handleOrigURL If TRUE the selected URL is compared to the
   * <origurl>tag that comes in AmphetaRate recommended articles. If the URL is
   * the origurl, the "New Favorite" Dialog is opened.
   */
    void actionOpenURL(String url, boolean handleOrigURL) {
        if (handleOrigURL) actionNewFavorite(url, "", null); else actionOpenURL(url);
    }

    /**
   * Print newstext of displayed news
   */
    void actionPrintNews() {
        boolean success;
        if (GlobalSettings.useBrowserForNewsText) success = PrintShop.printNewsFromBrowser(); else success = PrintShop.printNewsFromText();
        if (!success) MessageBoxFactory.showMessage(shell, SWT.ICON_WARNING, GUI.i18n.getTranslation("TOOLTIP_PRINT"), GUI.i18n.getTranslation("MESSAGEBOX_PRINT_EMPTYTEXT"));
    }

    /**
   * Rate a news (AmphetaRate)
   * 
   * @param rssNewsItem The newsitem to rate
   * @param rating One of the supported rating levels (Fantastic, Good,
   * Moderate, Bad, Very Bad).
   */
    void actionRateNews(NewsItem rssNewsItem, int rating) {
        rssOwlNewsText.rateNews(rssNewsItem, rating);
    }

    /**
   * Save displayed feed to local file
   */
    void actionSaveFeed() {
        if (!FileShop.saveSelectedNewsFeed()) MessageBoxFactory.showMessage(shell, SWT.ICON_WARNING, GUI.i18n.getTranslation("MENU_SAVE"), GUI.i18n.getTranslation("MESSAGEBOX_ERROR_SAVE_RSS"));
    }

    /**
   * Search in displayed newsfeed
   */
    void actionSearch() {
        rssOwlNewsTabFolder.searchInSelectedFeed();
    }

    /**
   * Open the dialog to search for newsfeeds
   */
    void actionSearchFeeds() {
        new FeedSearchDialog(GUI.i18n.getTranslation("MENU_FEEDSEARCH"), GUI.i18n.getTranslation("LABEL_SEARCH_TOPIC"), rssOwlGui).open();
    }

    /**
   * Search in an entire category
   * 
   * @param rssOwlCategory The category to search in
   */
    void actionSearchInCategory(Category rssOwlCategory) {
        String dialogTitle = (rssOwlCategory.isRoot()) ? GUI.i18n.getTranslation("BUTTON_SEARCH_ALL") : GUI.i18n.getTranslation("SEARCH_DIALOG_TITLE");
        SearchDialog rssOwlSearchDialog = new SearchDialog(shell, rssOwlCategory, dialogTitle, GUI.i18n.getTranslation("SEARCH_DIALOG_MESSAGE"));
        if (rssOwlSearchDialog.open() == Window.OK) {
            SearchDefinition searchDefinition = rssOwlSearchDialog.getValue();
            if (!StringShop.isset(searchDefinition.getPattern())) return;
            Category selectedCategory = rssOwlCategory;
            if (rssOwlSearchDialog.getCategory() != null && !rssOwlSearchDialog.getCategory().isRoot()) selectedCategory = rssOwlSearchDialog.getCategory();
            actionAggregateCategory(selectedCategory, searchDefinition, false);
        }
    }

    /**
   * Show the EPL license in a new tab
   */
    void actionShowLicense() {
        rssOwlNewsTabFolder.showLicenseTab(getClass().getResourceAsStream("/usr/EPL.txt"), PaintShop.iconInfo);
    }

    /**
   * Show / Hide the quickview control
   * 
   * @param show TRUE if visible
   */
    void actionShowQuickview(boolean show) {
        rssOwlQuickView.setShowQuickview(show, true);
    }

    /**
   * Show / Hide the toolbar control
   * 
   * @param show TRUE if visible
   */
    void actionShowToolBar(boolean show) {
        rssOwlQuickView.setShowToolBar(show, true);
    }

    /**
   * Show the welcome tab
   */
    void actionShowWelcome() {
        rssOwlNewsTabFolder.showWelcomeTab();
    }

    /**
   * Create a mail to advertise RSSOwl
   */
    void actionTellFriends() {
        BrowserShop.openLink(URLShop.createTellMyFriends());
    }

    /**
   * View displayed newsfeed in pdf, rtf or html
   * 
   * @param format PDF, RTF or HTML
   */
    void actionViewFeedInDocument(int format) {
        rssOwlNewsTabFolder.viewNewsInDocument(format);
    }

    /**
   * This method is called when a MenuItem from the Edit Menu is selected. It
   * will retrieve the current selected control and perform an action based on
   * the given action value and the type of Control that is selected.
   * 
   * @param action One of the supported actions of the edit menu
   */
    void handleEditAction(int action) {
        Control control = display.getFocusControl();
        if (!WidgetShop.isset(control)) return;
        switch(action) {
            case EDIT_ACTION_CUT:
                if (control instanceof Text) ((Text) control).cut(); else if (control instanceof StyledText) ((StyledText) control).cut(); else if (control instanceof Combo) ((Combo) control).cut();
                break;
            case EDIT_ACTION_COPY:
                if (control instanceof Text) ((Text) control).copy(); else if (control instanceof StyledText) ((StyledText) control).copy(); else if (control instanceof Combo) ((Combo) control).copy();
                break;
            case EDIT_ACTION_PASTE:
                if (control instanceof Text) ((Text) control).paste(); else if (control instanceof StyledText) ((StyledText) control).paste(); else if (control instanceof Combo) ((Combo) control).paste();
                break;
            case EDIT_ACTION_DELETE:
                if (control instanceof Tree) {
                    Tree tree = (Tree) control;
                    if (tree.getSelectionCount() > 0 && tree.getSelection()[0].getData() != null) {
                        Object data = tree.getSelection()[0].getData();
                        if (data instanceof TreeItemData) rssOwlGui.getRSSOwlFavoritesTree().performDeletion();
                    }
                }
                break;
            case EDIT_ACTION_SELECTALL:
                if (control instanceof Text) ((Text) control).selectAll(); else if (control instanceof StyledText) ((StyledText) control).selectAll(); else if (control instanceof Combo) ((Combo) control).setSelection(new Point(0, ((Combo) control).getText().length()));
                break;
            case EDIT_ACTION_PROPERTIES:
                if (control instanceof Tree) {
                    Tree tree = (Tree) control;
                    if (tree.getSelectionCount() > 0 && tree.getSelection()[0].getData() != null) {
                        Object data = tree.getSelection()[0].getData();
                        if (data instanceof TreeItemData && ((TreeItemData) data).isFavorite()) actionEditFavorite(); else if (data instanceof TreeItemData && ((TreeItemData) data).isCategory()) actionEditCategory(); else if (data instanceof TreeItemData && ((TreeItemData) data).isBlogroll()) actionEditBlogroll();
                    }
                }
                break;
        }
    }
}

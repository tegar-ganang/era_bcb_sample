package com.salas.bb.views;

import java.awt.*;
import javax.swing.*;
import com.jgoodies.plaf.*;
import com.jgoodies.plaf.plastic.PlasticLookAndFeel;
import com.jgoodies.plaf.windows.ExtWindowsLookAndFeel;
import com.jgoodies.swing.ExtToolBar;
import com.jgoodies.swing.application.*;
import com.jgoodies.swing.util.*;
import com.salas.bb.channelguide.ChannelGuideEntry;
import com.salas.bb.core.*;

/**
 * MainFrame - Containing the main content of the application
 * 
 */
public class MainFrame extends AbstractMainFrame {

    private static final Dimension PREF_SIZE = LookUtils.isLowRes ? new Dimension(620, 510) : new Dimension(760, 570);

    private JComponent articleListPanel;

    private JComponent channelListPanel;

    private JComponent channelGuidePanel;

    private JComponent splitPane;

    private JPanel mainPane;

    private JToolBar toolBar;

    private JLabel statusField;

    private PopupAdapter cGBtnPopupAdapter;

    private PopupAdapter chanListPopupAdapter;

    private PopupAdapter articleListPopupAdapter;

    /**
	 * Build the actual main window with this call. This leads to the calls to
	 * the other methods in this class. 
	 */
    public MainFrame() {
        super(Workbench.getGlobals().getWindowTitle());
    }

    /**
	 * buildContentPane - 
	 * 
	 * @return - 
	 */
    protected JComponent buildContentPane() {
        articleListPanel = new ArticleListPanel();
        channelListPanel = new ChannelListPanel();
        channelGuidePanel = new ChannelGuidePanel();
        statusField = UIFactory.createPlainLabel(Workbench.getGlobals().getCopyright());
        UIFactory.createPlainLabel(Workbench.getGlobals().getCopyright());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(buildToolBar(), BorderLayout.NORTH);
        topPanel.add(buildMainPane(), BorderLayout.CENTER);
        topPanel.add(buildStatusBar(), BorderLayout.SOUTH);
        topPanel.setPreferredSize(PREF_SIZE);
        return topPanel;
    }

    /**
	 * buildMainPane - Consisting of a ChannelGuidePanel on the left and the SplitPane
	 * in the center.
	 * 
	 * @return - 
	 */
    private Component buildMainPane() {
        mainPane = new JPanel();
        mainPane.setLayout(new BorderLayout());
        mainPane.add(channelGuidePanel, BorderLayout.WEST);
        mainPane.add(buildSplitPane(), BorderLayout.CENTER);
        return mainPane;
    }

    /**
	 * buildSplitPane - SplitPane consist of the ChannelListPanel on the left and the ItemListPanel
	 * on the right, with a movable split bar.
	 * 
	 * @return - 
	 */
    private Component buildSplitPane() {
        splitPane = UIFactory.createStrippedSplitPane(JSplitPane.HORIZONTAL_SPLIT, channelListPanel, articleListPanel, 0.0);
        splitPane.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));
        return splitPane;
    }

    /**
	  * Creates, configures, and composes the tool bar.
	  */
    private JToolBar buildToolBar() {
        ExtToolBar toolBar = new ExtToolBar("ToolBar");
        toolBar.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        toolBar.putClientProperty(ExtWindowsLookAndFeel.BORDER_STYLE_KEY, BorderStyle.SEPARATOR);
        toolBar.putClientProperty(PlasticLookAndFeel.BORDER_STYLE_KEY, BorderStyle.SEPARATOR);
        return toolBar;
    }

    /**
	  * Create and configure the StatusBar
	  */
    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(statusField, BorderLayout.WEST);
        statusField.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 3));
        return panel;
    }

    protected void restoreState() {
        GlobalController.SINGLETON.selectCGE(GlobalModel.SINGLETON.getSelectedCGE());
    }

    /**
	 * getChannelGuidePanel - 
	 * 
	 * @return - 
	 */
    public JComponent getChannelGuidePanel() {
        return channelGuidePanel;
    }

    /**
	 * getChannelListPanel - 
	 * 
	 * @return - 
	 */
    public JComponent getChannelListPanel() {
        return channelListPanel;
    }

    /**
	 * getItemListPanel - 
	 * 
	 * @return - 
	 */
    public ArticleListPanel getItemListPanel() {
        return (ArticleListPanel) articleListPanel;
    }

    /**
	 * getToolBarPanel - 
	 * 
	 * @return - 
	 */
    public JToolBar getToolBarPanel() {
        return toolBar;
    }

    /**
	 * showSelectedCGE - Tell the ChannelList to highlight (show selection)
	 * of the indicated ChannelGuideEntry.
	 * 
	 */
    public void showSelectedCGE() {
        ChannelGuideEntry cge = GlobalModel.SINGLETON.getSelectedCGE();
        ((ChannelListPanel) channelListPanel).showSelectedCGE(cge);
    }

    /**
	 * showArticleSelected - Make the ArticleListPanel display the indicated article
	 * in a selected state. (Note this is a UI selection and it is a substep
	 * to recording a selected Article. 
	 * 
	 * @param i - index or -1 if nothing is really selected
	 */
    public void showArticleSelected(int i) {
        if (i == -1 || articleListPanel == null) return;
        ((ArticleListPanel) articleListPanel).showSelectedItem(i);
    }

    /**
	 * getCGBtnPopupAdapter - returns a PopupAdapter for the Channel Guide Buttons
	 * Right Click Menu
	 */
    public PopupAdapter getCGBtnPopupAdapter() {
        if (cGBtnPopupAdapter == null) {
            cGBtnPopupAdapter = new PopupAdapter() {

                protected JPopupMenu createPopupMenu() {
                    JPopupMenu cgbtnPopUp = new JPopupMenu("Menu");
                    cgbtnPopUp.add(ActionManager.get(GlobalController.MARK_ALLREAD));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.SORT_CHANNELS));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.APPEND_CHANNEL));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.CHANNEL_PROPERTIES));
                    return cgbtnPopUp;
                }
            };
        }
        ;
        return cGBtnPopupAdapter;
    }

    /**
	 * getChanListPopupAdapter - Returns a PopupAdapter for the Channel List right click
	 * menu.
	 */
    public PopupAdapter getChanListPopupAdapter() {
        if (chanListPopupAdapter == null) {
            chanListPopupAdapter = new PopupAdapter() {

                protected JPopupMenu createPopupMenu() {
                    JPopupMenu cgbtnPopUp = new JPopupMenu("Menu");
                    cgbtnPopUp.add(ActionManager.get(GlobalController.MARK_ALLREAD));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.SORT_CHANNELS));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.MARK_FAVORITE));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.ADD_CHANNEL));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.DEL_CHANNEL));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.CHANNEL_PROPERTIES));
                    return cgbtnPopUp;
                }
            };
        }
        ;
        return chanListPopupAdapter;
    }

    /**
	 * getArticleListPopupAdapter - Returns a PopupAdapter for the Article List right
	 * click menu. 
	 */
    public PopupAdapter getArticleListPopupAdapter() {
        if (articleListPopupAdapter == null) {
            articleListPopupAdapter = new PopupAdapter() {

                protected JPopupMenu createPopupMenu() {
                    JPopupMenu cgbtnPopUp = new JPopupMenu("Menu");
                    cgbtnPopUp.add(ActionManager.get(GlobalController.MARK_ALLREAD));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.SORT_ARTICLES));
                    cgbtnPopUp.add(ActionManager.get(GlobalController.ARTICLE_PROPERTIES));
                    return cgbtnPopUp;
                }
            };
        }
        ;
        return articleListPopupAdapter;
    }
}

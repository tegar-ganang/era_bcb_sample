package games.strategy.triplea.ui;

import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.HistorySynchronizer;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.sound.ClipPlayer;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.AirThatCantLandUtil;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.sound.SoundPath;
import games.strategy.triplea.ui.history.HistoryDetailsPanel;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 
 * @author Sean Bridges
 * 
 *         Main frame for the triple a game
 */
public class TripleAFrame extends MainGameFrame {

    private GameData m_data;

    private IGame m_game;

    private MapPanel m_mapPanel;

    private MapPanelSmallView m_smallView;

    private JLabel m_message = new JLabel("No selection");

    private JLabel m_status = new JLabel("");

    private JLabel m_step = new JLabel("xxxxxx");

    private JLabel m_round = new JLabel("xxxxxx");

    private JLabel m_player = new JLabel("xxxxxx");

    private ActionButtons m_actionButtons;

    private Set<IGamePlayer> m_localPlayers;

    private JPanel m_gameMainPanel = new JPanel();

    private JPanel m_rightHandSidePanel = new JPanel();

    private JTabbedPane m_tabsPanel = new JTabbedPane();

    private StatPanel m_statsPanel;

    private StatPanel m_economyPanel;

    private TerritoryDetailPanel m_details;

    private JPanel m_historyPanel = new JPanel();

    private JPanel m_gameSouthPanel;

    private HistoryPanel m_historyTree;

    private boolean m_inHistory = false;

    private boolean m_inGame = true;

    private HistorySynchronizer m_historySyncher;

    private UIContext m_uiContext;

    private JPanel m_mapAndChatPanel;

    private ChatPanel m_chatPanel;

    private CommentPanel m_commentPanel;

    private JSplitPane m_chatSplit;

    private JSplitPane m_commentSplit;

    private EditPanel m_editPanel;

    private final ButtonModel m_editModeButtonModel;

    private final ButtonModel m_showCommentLogButtonModel;

    private IEditDelegate m_editDelegate;

    private JSplitPane m_gameCenterPanel;

    /** Creates new TripleAFrame */
    public TripleAFrame(final IGame game, final Set<IGamePlayer> players) throws IOException {
        super("TripleA - " + game.getData().getGameName());
        setIconImage(GameRunner.getGameIcon(this));
        m_game = game;
        m_data = game.getData();
        m_localPlayers = players;
        addZoomKeyboardShortcuts();
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(WINDOW_LISTENER);
        m_uiContext = new UIContext();
        m_uiContext.setDefaultMapDir(game.getData());
        m_uiContext.getMapData().verify(m_data);
        m_uiContext.setPlayerList(players);
        m_editModeButtonModel = new JToggleButton.ToggleButtonModel();
        m_editModeButtonModel.setEnabled(false);
        m_showCommentLogButtonModel = new JToggleButton.ToggleButtonModel();
        m_showCommentLogButtonModel.addActionListener(m_showCommentLogAction);
        m_showCommentLogButtonModel.setSelected(false);
        createMenuBar();
        final ImageScrollModel model = new ImageScrollModel();
        model.setScrollX(m_uiContext.getMapData().scrollWrapX());
        model.setMaxBounds(m_uiContext.getMapData().getMapDimensions().width, m_uiContext.getMapData().getMapDimensions().height);
        final Image small = m_uiContext.getMapImage().getSmallMapImage();
        m_smallView = new MapPanelSmallView(small, model);
        m_mapPanel = new MapPanel(m_data, m_smallView, m_uiContext, model);
        m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);
        this.addKeyListener(m_arrowKeyActionListener);
        m_mapPanel.addKeyListener(m_arrowKeyActionListener);
        m_mapPanel.initSmallMap();
        m_mapAndChatPanel = new JPanel();
        m_mapAndChatPanel.setLayout(new BorderLayout());
        m_commentPanel = new CommentPanel(this, m_data);
        m_chatSplit = new JSplitPane();
        m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        m_chatSplit.setOneTouchExpandable(true);
        m_chatSplit.setDividerSize(8);
        m_chatSplit.setResizeWeight(0.95);
        if (MainFrame.getInstance().getChat() != null) {
            m_commentSplit = new JSplitPane();
            m_commentSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
            m_commentSplit.setOneTouchExpandable(true);
            m_commentSplit.setDividerSize(8);
            m_commentSplit.setResizeWeight(0.5);
            m_commentSplit.setTopComponent(m_commentPanel);
            m_commentSplit.setBottomComponent(null);
            m_chatPanel = new ChatPanel(MainFrame.getInstance().getChat());
            m_chatPanel.setPlayerRenderer(new PlayerChatRenderer(m_game, m_uiContext));
            final Dimension chatPrefSize = new Dimension((int) m_chatPanel.getPreferredSize().getWidth(), 95);
            m_chatPanel.setPreferredSize(chatPrefSize);
            m_chatSplit.setTopComponent(m_mapPanel);
            m_chatSplit.setBottomComponent(m_chatPanel);
            m_mapAndChatPanel.add(m_chatSplit, BorderLayout.CENTER);
        } else {
            m_mapAndChatPanel.add(m_mapPanel, BorderLayout.CENTER);
        }
        m_gameMainPanel.setLayout(new BorderLayout());
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
        m_gameSouthPanel = new JPanel();
        m_gameSouthPanel.setLayout(new BorderLayout());
        m_gameSouthPanel.add(m_message, BorderLayout.WEST);
        m_message.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_message.setText("some text to set a reasonable preferred size");
        m_message.setPreferredSize(m_message.getPreferredSize());
        m_message.setText("");
        m_gameSouthPanel.add(m_status, BorderLayout.CENTER);
        m_status.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        final JPanel stepPanel = new JPanel();
        stepPanel.setLayout(new GridBagLayout());
        stepPanel.add(m_step, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        stepPanel.add(m_player, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        stepPanel.add(m_round, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        m_step.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_round.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_player.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        m_step.setHorizontalTextPosition(SwingConstants.LEADING);
        m_gameSouthPanel.add(stepPanel, BorderLayout.EAST);
        m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
        m_rightHandSidePanel.setLayout(new BorderLayout());
        m_rightHandSidePanel.add(m_smallView, BorderLayout.NORTH);
        m_tabsPanel.setBorder(null);
        m_rightHandSidePanel.add(m_tabsPanel, BorderLayout.CENTER);
        m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
        m_tabsPanel.addTab("Actions", m_actionButtons);
        m_actionButtons.setBorder(null);
        m_statsPanel = new StatPanel(m_data);
        m_tabsPanel.addTab("Stats", m_statsPanel);
        m_economyPanel = new EconomyPanel(m_data);
        m_tabsPanel.addTab("Economy", m_economyPanel);
        m_details = new TerritoryDetailPanel(m_mapPanel, m_data, m_uiContext, this);
        m_tabsPanel.addTab("Territory", m_details);
        m_editPanel = new EditPanel(m_data, m_mapPanel, this);
        m_tabsPanel.addChangeListener(new ChangeListener() {

            public void stateChanged(final ChangeEvent evt) {
                final JTabbedPane pane = (JTabbedPane) evt.getSource();
                final int sel = pane.getSelectedIndex();
                if (sel == -1) return;
                if (pane.getComponentAt(sel).equals(m_editPanel)) {
                    PlayerID player = null;
                    m_data.acquireReadLock();
                    try {
                        player = m_data.getSequence().getStep().getPlayerID();
                    } finally {
                        m_data.releaseReadLock();
                    }
                    m_actionButtons.getCurrent().setActive(false);
                    m_editPanel.display(player);
                } else {
                    m_actionButtons.getCurrent().setActive(true);
                    m_editPanel.setActive(false);
                }
            }
        });
        m_rightHandSidePanel.setPreferredSize(new Dimension((int) m_smallView.getPreferredSize().getWidth(), (int) m_mapPanel.getPreferredSize().getHeight()));
        m_gameCenterPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_mapAndChatPanel, m_rightHandSidePanel);
        m_gameCenterPanel.setOneTouchExpandable(true);
        m_gameCenterPanel.setDividerSize(8);
        m_gameCenterPanel.setResizeWeight(1.0);
        m_gameMainPanel.add(m_gameCenterPanel, BorderLayout.CENTER);
        m_gameCenterPanel.resetToPreferredSizes();
        this.setGlassPane(new JComponent() {

            @Override
            protected void paintComponent(final Graphics g) {
                g.setFont(new Font("Ariel", Font.BOLD, 50));
                g.setColor(new Color(255, 255, 255, 175));
                final Dimension size = m_mapPanel.getSize();
                g.drawString("Edit Mode", (int) ((size.getWidth() - 200) / 2), (int) ((size.getHeight() - 100) / 2));
            }
        });
        m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
        m_data.addDataChangeListener(m_dataChangeListener);
        game.addGameStepListener(m_stepListener);
        updateStep();
        m_uiContext.addShutdownWindow(this);
    }

    private void addZoomKeyboardShortcuts() {
        final String zoom_map_in = "zoom_map_in";
        ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.META_MASK), zoom_map_in);
        ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in);
        ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.META_MASK), zoom_map_in);
        ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in);
        ((JComponent) getContentPane()).getActionMap().put(zoom_map_in, new AbstractAction(zoom_map_in) {

            public void actionPerformed(final ActionEvent e) {
                if (getScale() < 100) setScale(getScale() + 10);
            }
        });
        final String zoom_map_out = "zoom_map_out";
        ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.META_MASK), zoom_map_out);
        ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.CTRL_MASK), zoom_map_out);
        ((JComponent) getContentPane()).getActionMap().put(zoom_map_out, new AbstractAction(zoom_map_out) {

            public void actionPerformed(final ActionEvent e) {
                if (getScale() > 16) setScale(getScale() - 10);
            }
        });
    }

    /**
	 * 
	 * @param value
	 *            - a number between 15 and 100
	 */
    void setScale(final double value) {
        getMapPanel().setScale(value / 100);
    }

    /**
	 * 
	 * @return a scale between 15 and 100
	 */
    private double getScale() {
        return getMapPanel().getScale() * 100;
    }

    public void stopGame() {
        if (m_uiContext == null) return;
        if (GameRunner.isMac()) {
            MacWrapper.unregisterShutdownHandler();
        }
        m_uiContext.shutDown();
        if (m_chatPanel != null) {
            m_chatPanel.setPlayerRenderer(null);
            m_chatPanel.setChat(null);
        }
        if (m_historySyncher != null) {
            m_historySyncher.deactivate();
            m_historySyncher = null;
        }
        m_game.removeGameStepListener(m_stepListener);
        m_game = null;
        m_uiContext = null;
        if (m_data != null) m_data.clearAllListeners();
        m_data = null;
        m_tabsPanel.removeAll();
        m_commentPanel.cleanUp();
        MAP_SELECTION_LISTENER = null;
        m_actionButtons = null;
        m_chatPanel = null;
        m_chatSplit = null;
        m_commentSplit = null;
        m_commentPanel = null;
        m_details = null;
        m_gameMainPanel = null;
        m_stepListener = null;
        m_gameSouthPanel = null;
        m_historyTree = null;
        m_historyPanel = null;
        m_mapPanel = null;
        m_mapAndChatPanel = null;
        m_message = null;
        m_status = null;
        m_rightHandSidePanel = null;
        m_smallView = null;
        m_statsPanel = null;
        m_economyPanel = null;
        m_step = null;
        m_round = null;
        m_player = null;
        m_tabsPanel = null;
        m_showGameAction = null;
        m_showHistoryAction = null;
        m_showMapOnlyAction = null;
        m_showCommentLogAction = null;
        m_localPlayers = null;
        m_editPanel = null;
        removeWindowListener(WINDOW_LISTENER);
        WINDOW_LISTENER = null;
    }

    @Override
    public void shutdown() {
        final int rVal = EventThreadJOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION);
        if (rVal != JOptionPane.OK_OPTION) return;
        System.exit(0);
    }

    @Override
    public void leaveGame() {
        final int rVal = EventThreadJOptionPane.showConfirmDialog(this, "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION);
        if (rVal != JOptionPane.OK_OPTION) return;
        if (m_game instanceof ServerGame) {
            ((ServerGame) m_game).stopGame();
        } else {
            m_game.getMessenger().shutDown();
            ((ClientGame) m_game).shutDown();
            MainFrame.getInstance().clientLeftGame();
        }
    }

    private void createMenuBar() {
        final TripleaMenu menu = new TripleaMenu(this);
        this.setJMenuBar(menu);
    }

    private WindowListener WINDOW_LISTENER = new WindowAdapter() {

        @Override
        public void windowClosing(final WindowEvent e) {
            leaveGame();
        }
    };

    public MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener() {

        Territory in;

        @Override
        public void mouseEntered(final Territory territory) {
            in = territory;
            refresh();
        }

        void refresh() {
            final StringBuilder buf = new StringBuilder(" ");
            buf.append(in == null ? "none" : in.getName());
            if (in != null) {
                final TerritoryAttachment ta = TerritoryAttachment.get(in);
                if (ta != null) {
                    final Iterator<TerritoryEffect> iter = ta.getTerritoryEffect().iterator();
                    if (iter.hasNext()) {
                        buf.append(" (");
                    }
                    while (iter.hasNext()) {
                        buf.append(iter.next().getName());
                        if (iter.hasNext()) buf.append(", "); else buf.append(")");
                    }
                    final int production = ta.getProduction();
                    final int unitProduction = ta.getUnitProduction();
                    final ResourceCollection resource = ta.getResources();
                    if (unitProduction > 0 && unitProduction != production) buf.append(", UnitProd: " + unitProduction);
                    if (production > 0 || (resource != null && resource.toString().length() > 0)) {
                        buf.append(", Prod: ");
                        if (production > 0) {
                            buf.append(production + " PUs");
                            if (resource != null && resource.toString().length() > 0) buf.append(", ");
                        }
                        if (resource != null) buf.append(resource.toString());
                    }
                }
            }
            m_message.setText(buf.toString());
        }
    };

    public void clearStatusMessage() {
        m_status.setText("");
        m_status.setIcon(null);
    }

    public void setStatusErrorMessage(final String msg) {
        m_status.setText(msg);
        if (!msg.equals("")) m_status.setIcon(new ImageIcon(m_mapPanel.getErrorImage())); else m_status.setIcon(null);
    }

    public void setStatusWarningMessage(final String msg) {
        m_status.setText(msg);
        if (!msg.equals("")) m_status.setIcon(new ImageIcon(m_mapPanel.getWarningImage())); else m_status.setIcon(null);
    }

    public void setStatusInfoMessage(final String msg) {
        m_status.setText(msg);
        if (!msg.equals("")) m_status.setIcon(new ImageIcon(m_mapPanel.getInfoImage())); else m_status.setIcon(null);
    }

    public IntegerMap<ProductionRule> getProduction(final PlayerID player, final boolean bid) {
        m_actionButtons.changeToProduce(player);
        return m_actionButtons.waitForPurchase(bid);
    }

    public HashMap<Unit, IntegerMap<RepairRule>> getRepair(final PlayerID player, final boolean bid) {
        m_actionButtons.changeToRepair(player);
        return m_actionButtons.waitForRepair(bid);
    }

    public MoveDescription getMove(final PlayerID player, final IPlayerBridge bridge, final boolean nonCombat) {
        m_actionButtons.changeToMove(player, nonCombat);
        if (!getBattlePanel().getBattleFrame().isVisible()) {
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            requestFocusInWindow();
                            transferFocus();
                        }
                    });
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else {
                requestFocusInWindow();
                transferFocus();
            }
        }
        return m_actionButtons.waitForMove(bridge);
    }

    public PlaceData waitForPlace(final PlayerID player, final boolean bid, final IPlayerBridge bridge) {
        m_actionButtons.changeToPlace(player);
        return m_actionButtons.waitForPlace(bid, bridge);
    }

    public void waitForEndTurn(final PlayerID player, final IPlayerBridge bridge) {
        m_actionButtons.changeToEndTurn(player);
        m_actionButtons.waitForEndTurn(this, bridge);
    }

    public FightBattleDetails getBattle(final PlayerID player, final Collection<Territory> battles, final Collection<Territory> bombingRaids) {
        m_actionButtons.changeToBattle(player, battles, bombingRaids);
        return m_actionButtons.waitForBattleSelection();
    }

    @Override
    public void notifyError(final String message) {
        EventThreadJOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void notifyMessage(final String message, final String title) {
        EventThreadJOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void notification(final String message) {
        EventThreadJOptionPane.showMessageDialog(this, message, "Notification", JOptionPane.INFORMATION_MESSAGE, true);
    }

    public boolean getOKToLetAirDie(final PlayerID m_id, String message, final boolean movePhase) {
        final boolean lhtrProd = AirThatCantLandUtil.isLHTRCarrierProduction(m_data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(m_data);
        final int carrierCount = m_id.getUnits().getMatches(Matches.UnitIsCarrier).size();
        final boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0;
        if (canProduceCarriersUnderFighter && carrierCount > 0) {
            message = message + "\nYou have " + carrierCount + " " + MyFormatter.pluralize("carrier", carrierCount) + " on which planes can land";
        }
        final String ok = movePhase ? "End Move Phase" : "Kill Planes";
        final String cancel = movePhase ? "Keep Moving" : "Change Placement";
        final String[] options = { cancel, ok };
        final int choice = EventThreadJOptionPane.showOptionDialog(this, message, "Air cannot land", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel);
        return choice == 1;
    }

    public boolean getOKToLetUnitsDie(final PlayerID m_id, final String message, final boolean movePhase) {
        final String ok = movePhase ? "Done Moving" : "Kill Units";
        final String cancel = movePhase ? "Keep Moving" : "Change Placement";
        final String[] options = { cancel, ok };
        final int choice = EventThreadJOptionPane.showOptionDialog(this, message, "Units cannot fight", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel);
        return choice == 1;
    }

    public boolean acceptPoliticalAction(final String acceptanceQuestion) {
        final int choice = EventThreadJOptionPane.showConfirmDialog(this, acceptanceQuestion, "Accept Political Proposal?", JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    public boolean getOK(final String message) {
        final int choice = EventThreadJOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION);
        return choice == JOptionPane.OK_OPTION;
    }

    public void notifyTechResults(final TechResults msg) {
        final AtomicReference<TechResultsDisplay> displayRef = new AtomicReference<TechResultsDisplay>();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    final TechResultsDisplay display = new TechResultsDisplay(msg, m_uiContext, m_data);
                    displayRef.set(display);
                }
            });
        } catch (final InterruptedException e) {
            throw new IllegalStateException();
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException();
        }
        EventThreadJOptionPane.showOptionDialog(this, displayRef.get(), "Tech roll", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "OK" }, "OK");
    }

    public boolean getStrategicBombingRaid(final Territory location) {
        final String message = (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(m_data) ? "Bomb/Escort" : "Bomb") + " in " + location.getName();
        final String bomb = (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(m_data) ? "Bomb/Escort" : "Bomb");
        final String normal = "Attack";
        final String[] choices = { bomb, normal };
        final int choice = EventThreadJOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, choices, bomb);
        return choice == 0;
    }

    public Unit getStrategicBombingRaidTarget(final Territory territory, final Collection<Unit> units) {
        if (units == null || units.size() == 0) return null;
        if (units.size() == 1) return units.iterator().next();
        final AtomicReference<Unit> selected = new AtomicReference<Unit>();
        final String message = "Select bombing target in " + territory.getName();
        final Tuple<JPanel, JList> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList>>() {

            public Tuple<JPanel, JList> run() {
                final JList list = new JList(new Vector<Unit>(units));
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.setSelectedIndex(0);
                final JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                final JScrollPane scroll = new JScrollPane(list);
                panel.add(scroll, BorderLayout.CENTER);
                return new Tuple<JPanel, JList>(panel, list);
            }
        });
        final JPanel panel = comps.getFirst();
        final JList list = comps.getSecond();
        final String[] options = { "OK", "Cancel" };
        final int selection = EventThreadJOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
        if (selection == 0) selected.set((Unit) list.getSelectedValue());
        return selected.get();
    }

    public int[] selectFixedDice(final int numDice, final int hitAt, final boolean hitOnlyIfEquals, final String title, final int diceSides) {
        final DiceChooser chooser = Util.runInSwingEventThread(new Util.Task<DiceChooser>() {

            public DiceChooser run() {
                return new DiceChooser(getUIContext(), numDice, hitAt, hitOnlyIfEquals, m_data);
            }
        });
        do {
            EventThreadJOptionPane.showMessageDialog(null, chooser, title, JOptionPane.PLAIN_MESSAGE);
        } while (chooser.getDice() == null);
        return chooser.getDice();
    }

    public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage) {
        final Tuple<JPanel, JList> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList>>() {

            public Tuple<JPanel, JList> run() {
                m_mapPanel.centerOn(currentTerritory);
                final JList list = new JList(new Vector<Territory>(candidates));
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.setSelectedIndex(0);
                final JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                final JScrollPane scroll = new JScrollPane(list);
                final JTextArea text = new JTextArea(unitMessage, 8, 30);
                text.setLineWrap(true);
                text.setEditable(false);
                text.setWrapStyleWord(true);
                panel.add(text, BorderLayout.NORTH);
                panel.add(scroll, BorderLayout.CENTER);
                return new Tuple<JPanel, JList>(panel, list);
            }
        });
        final JPanel panel = comps.getFirst();
        final JList list = comps.getSecond();
        final String[] options = { "OK" };
        final String title = "Select territory for air units to land, current territory is " + currentTerritory.getName();
        EventThreadJOptionPane.showOptionDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
        final Territory selected = (Territory) list.getSelectedValue();
        return selected;
    }

    public HashMap<Territory, IntegerMap<Unit>> selectKamikazeSuicideAttacks(final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack, final Resource attackResourceToken, final int maxNumberOfAttacksAllowed) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Should not be called from dispatch thread");
        }
        final CountDownLatch continueLatch = new CountDownLatch(1);
        final HashMap<Territory, IntegerMap<Unit>> selection = new HashMap<Territory, IntegerMap<Unit>>();
        if (possibleUnitsToAttack == null || possibleUnitsToAttack.isEmpty() || attackResourceToken == null || maxNumberOfAttacksAllowed <= 0) return selection;
        final Collection<IndividualUnitPanelGrouped> unitPanels = new ArrayList<IndividualUnitPanelGrouped>();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                final HashMap<String, Collection<Unit>> possibleUnitsToAttackStringForm = new HashMap<String, Collection<Unit>>();
                for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet()) {
                    final List<Unit> units = new ArrayList<Unit>(entry.getValue());
                    Collections.sort(units, new UnitBattleComparator(false, BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(m_data), m_data, false));
                    Collections.reverse(units);
                    possibleUnitsToAttackStringForm.put(entry.getKey().getName(), units);
                }
                m_mapPanel.centerOn(m_data.getMap().getTerritory(possibleUnitsToAttackStringForm.keySet().iterator().next()));
                final IndividualUnitPanelGrouped unitPanel = new IndividualUnitPanelGrouped(possibleUnitsToAttackStringForm, m_data, m_uiContext, "Select Units to Suicide Attack using " + attackResourceToken.getName(), maxNumberOfAttacksAllowed, true, false);
                unitPanels.add(unitPanel);
                final Object[] options = { "Attack", "None", "Wait" };
                final int option = JOptionPane.showOptionDialog(getParent(), unitPanel, "Select units to Suicide Attack using " + attackResourceToken.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
                if (option == JOptionPane.NO_OPTION) {
                    unitPanels.clear();
                    selection.clear();
                    continueLatch.countDown();
                    return;
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    try {
                        Thread.sleep(6000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    unitPanels.clear();
                    selection.clear();
                    run();
                } else {
                    if (unitPanels.size() != 1) throw new IllegalStateException("unitPanels should only contain 1 entry");
                    for (final IndividualUnitPanelGrouped terrChooser : unitPanels) {
                        for (final Entry<String, IntegerMap<Unit>> entry : terrChooser.getSelected().entrySet()) {
                            selection.put(m_data.getMap().getTerritory(entry.getKey()), entry.getValue());
                        }
                    }
                    continueLatch.countDown();
                }
            }
        });
        try {
            continueLatch.await();
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        return selection;
    }

    public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Integer, Collection<Unit>>> possibleScramblers) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Should not be called from dispatch thread");
        }
        final CountDownLatch continueLatch = new CountDownLatch(1);
        final HashMap<Territory, Collection<Unit>> selection = new HashMap<Territory, Collection<Unit>>();
        final Collection<Tuple<Territory, UnitChooser>> choosers = new ArrayList<Tuple<Territory, UnitChooser>>();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                m_mapPanel.centerOn(scrambleTo);
                final JPanel panel = new JPanel();
                panel.setLayout(new FlowLayout());
                for (final Territory from : possibleScramblers.keySet()) {
                    JScrollPane chooserScrollPane;
                    final JPanel panelChooser = new JPanel();
                    panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
                    panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
                    final JLabel whereFrom = new JLabel("From: " + from.getName());
                    whereFrom.setHorizontalAlignment(JLabel.LEFT);
                    whereFrom.setFont(new Font("Arial", Font.BOLD, 12));
                    panelChooser.add(whereFrom);
                    panelChooser.add(new JLabel(" "));
                    final Collection<Unit> possible = possibleScramblers.get(from).getSecond();
                    final int maxAllowed = Math.min(possibleScramblers.get(from).getFirst(), possible.size());
                    final UnitChooser chooser = new UnitChooser(possible, Collections.<Unit, Collection<Unit>>emptyMap(), m_data, false, m_uiContext);
                    chooser.setMaxAndShowMaxButton(maxAllowed);
                    choosers.add(new Tuple<Territory, UnitChooser>(from, chooser));
                    panelChooser.add(chooser);
                    chooserScrollPane = new JScrollPane(panelChooser);
                    panel.add(chooserScrollPane);
                }
                final Object[] options = { "Scramble", "None", "Wait" };
                final int option = JOptionPane.showOptionDialog(getParent(), panel, "Select units to scramble to " + scrambleTo.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
                if (option == JOptionPane.NO_OPTION) {
                    choosers.clear();
                    selection.clear();
                    continueLatch.countDown();
                    return;
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    try {
                        Thread.sleep(6000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    choosers.clear();
                    selection.clear();
                    run();
                } else {
                    for (final Tuple<Territory, UnitChooser> terrChooser : choosers) {
                        selection.put(terrChooser.getFirst(), terrChooser.getSecond().getSelected());
                    }
                    continueLatch.countDown();
                }
            }
        });
        try {
            continueLatch.await();
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        return selection;
    }

    public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Should not be called from dispatch thread");
        }
        final CountDownLatch continueLatch = new CountDownLatch(1);
        final Collection<Unit> selection = new ArrayList<Unit>();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                m_mapPanel.centerOn(current);
                final JPanel panel = new JPanel();
                panel.setLayout(new FlowLayout());
                JScrollPane chooserScrollPane;
                final JPanel panelChooser = new JPanel();
                panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
                panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
                final JLabel whereFrom = new JLabel("From: " + current.getName());
                whereFrom.setHorizontalAlignment(JLabel.LEFT);
                whereFrom.setFont(new Font("Arial", Font.BOLD, 12));
                panelChooser.add(whereFrom);
                panelChooser.add(new JLabel(" "));
                final int maxAllowed = possible.size();
                final UnitChooser chooser = new UnitChooser(possible, Collections.<Unit, Collection<Unit>>emptyMap(), m_data, false, m_uiContext);
                chooser.setMaxAndShowMaxButton(maxAllowed);
                panelChooser.add(chooser);
                chooserScrollPane = new JScrollPane(panelChooser);
                panel.add(chooserScrollPane);
                final Object[] options = { "Select", "None", "Wait" };
                final int option = JOptionPane.showOptionDialog(getParent(), panel, "Select units" + message, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
                if (option == JOptionPane.NO_OPTION) {
                    selection.clear();
                    continueLatch.countDown();
                    return;
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    try {
                        Thread.sleep(6000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    selection.clear();
                    run();
                } else {
                    selection.addAll(chooser.getSelected());
                    continueLatch.countDown();
                }
            }
        });
        try {
            continueLatch.await();
        } catch (final InterruptedException ex) {
            ex.printStackTrace();
        }
        return selection;
    }

    public void notifyPoliticalMessage(final String message) {
        EventThreadJOptionPane.showMessageDialog(this, message, "Political Alert", JOptionPane.INFORMATION_MESSAGE);
    }

    public PoliticalActionAttachment getPoliticalActionChoice(final PlayerID player, final boolean firstRun, final IPoliticsDelegate iPoliticsDelegate) {
        m_actionButtons.changeToPolitics(player);
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        requestFocusInWindow();
                        transferFocus();
                    }
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            requestFocusInWindow();
            transferFocus();
        }
        return m_actionButtons.waitForPoliticalAction(firstRun, iPoliticsDelegate);
    }

    public TechRoll getTechRolls(final PlayerID id) {
        m_actionButtons.changeToTech(id);
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        requestFocusInWindow();
                        transferFocus();
                    }
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            requestFocusInWindow();
            transferFocus();
        }
        return m_actionButtons.waitForTech();
    }

    public Territory getRocketAttack(final Collection<Territory> candidates, final Territory from) {
        final AtomicReference<Territory> selected = new AtomicReference<Territory>();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    final JList list = new JList(new Vector<Territory>(candidates));
                    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    list.setSelectedIndex(0);
                    final JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    final JScrollPane scroll = new JScrollPane(list);
                    panel.add(scroll, BorderLayout.CENTER);
                    if (from != null) {
                        panel.add(BorderLayout.NORTH, new JLabel("Targets for rocket in " + from.getName()));
                    }
                    final String[] options = { "OK", "Dont attack" };
                    final String message = "Select Rocket Target";
                    final int selection = JOptionPane.showOptionDialog(TripleAFrame.this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
                    if (selection == 0) selected.set((Territory) list.getSelectedValue());
                }
            });
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        return selected.get();
    }

    public Set<IGamePlayer> GetLocalPlayers() {
        return m_localPlayers;
    }

    public boolean playing(final PlayerID id) {
        if (id == null) return false;
        for (final IGamePlayer gamePlayer : m_localPlayers) {
            if (gamePlayer.getID().equals(id) && gamePlayer instanceof TripleAPlayer) {
                return true;
            }
        }
        return false;
    }

    public static int save(final String filename, final GameData m_data) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(filename);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(m_data);
            return 0;
        } catch (final Throwable t) {
            System.err.println(t.getMessage());
            return -1;
        } finally {
            try {
                if (fos != null) fos.flush();
            } catch (final Exception ignore) {
            }
            try {
                if (oos != null) oos.close();
            } catch (final Exception ignore) {
            }
        }
    }

    GameStepListener m_stepListener = new GameStepListener() {

        public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String stepDisplayName) {
            updateStep();
        }
    };

    private void updateStep() {
        final UIContext context = m_uiContext;
        if (context == null || context.isShutDown()) return;
        m_data.acquireReadLock();
        try {
            if (m_data.getSequence().getStep() == null) return;
        } finally {
            m_data.releaseReadLock();
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        updateStep();
                    }
                });
            } catch (final InterruptedException e) {
                e.printStackTrace();
            } catch (final InvocationTargetException e) {
                e.getCause().printStackTrace();
                throw new IllegalStateException(e.getCause().getMessage());
            }
            return;
        }
        int round;
        String stepDisplayName;
        PlayerID player;
        m_data.acquireReadLock();
        try {
            round = m_data.getSequence().getRound();
            stepDisplayName = m_data.getSequence().getStep().getDisplayName();
            player = m_data.getSequence().getStep().getPlayerID();
        } finally {
            m_data.releaseReadLock();
        }
        m_round.setText("Round:" + round + " ");
        m_step.setText(stepDisplayName);
        if (player != null) m_player.setText((playing(player) ? "" : "REMOTE: ") + player.getName());
        if (player != null && !player.isNull()) m_round.setIcon(new ImageIcon(m_uiContext.getFlagImageFactory().getFlag(player)));
        if (player != null && !player.isNull() && !playing(player) && !m_inHistory && !m_uiContext.getShowMapOnly()) {
            if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("We should be in dispatch thread");
            showHistory();
        } else if (player != null && !player.isNull() && playing(player) && m_inHistory) {
            showGame();
            ClipPlayer.getInstance().playClip(SoundPath.START_TURN, SoundPath.class);
        }
    }

    GameDataChangeListener m_dataChangeListener = new GameDataChangeListener() {

        public void gameDataChanged(final Change change) {
            try {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        if (m_uiContext == null) return;
                        if (getEditMode()) {
                            if (m_tabsPanel.indexOfComponent(m_editPanel) == -1) {
                                showEditMode();
                            }
                        } else {
                            if (m_tabsPanel.indexOfComponent(m_editPanel) != -1) {
                                hideEditMode();
                            }
                        }
                        if (m_uiContext.getShowMapOnly()) {
                            hideRightHandSidePanel();
                            final HistoryNode node = m_data.getHistory().getLastNode();
                            if (node instanceof Renderable) {
                                final Object details = ((Renderable) node).getRenderingData();
                                if (details instanceof MoveDescription) {
                                    final MoveDescription moveMessage = (MoveDescription) details;
                                    final Route route = moveMessage.getRoute();
                                    m_mapPanel.setRoute(null);
                                    m_mapPanel.setRoute(route);
                                    final Territory terr = route.getEnd();
                                    if (!m_mapPanel.isShowing(terr)) m_mapPanel.centerOn(terr);
                                }
                            }
                        } else {
                            showRightHandSidePanel();
                        }
                    }
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    };

    final KeyListener m_arrowKeyActionListener = new KeyListener() {

        final int diffPixel = 50;

        @Override
        public void keyPressed(final KeyEvent e) {
            final int x = m_mapPanel.getXOffset();
            final int y = m_mapPanel.getYOffset();
            final int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_RIGHT) getMapPanel().setTopLeft(x + diffPixel, y); else if (keyCode == KeyEvent.VK_LEFT) getMapPanel().setTopLeft(x - diffPixel, y); else if (keyCode == KeyEvent.VK_DOWN) getMapPanel().setTopLeft(x, y + diffPixel); else if (keyCode == KeyEvent.VK_UP) getMapPanel().setTopLeft(x, y - diffPixel);
        }

        @Override
        public void keyTyped(final KeyEvent e) {
        }

        @Override
        public void keyReleased(final KeyEvent e) {
        }
    };

    private void showEditMode() {
        m_tabsPanel.addTab("Edit", m_editPanel);
        if (m_editDelegate != null) m_tabsPanel.setSelectedComponent(m_editPanel);
        m_editModeButtonModel.setSelected(true);
        getGlassPane().setVisible(true);
    }

    private void hideEditMode() {
        if (m_tabsPanel.getSelectedComponent() == m_editPanel) m_tabsPanel.setSelectedIndex(0);
        m_tabsPanel.remove(m_editPanel);
        m_editModeButtonModel.setSelected(false);
        getGlassPane().setVisible(false);
    }

    public void showRightHandSidePanel() {
        m_rightHandSidePanel.setVisible(true);
    }

    public void hideRightHandSidePanel() {
        m_rightHandSidePanel.setVisible(false);
    }

    public HistoryPanel getHistoryPanel() {
        return m_historyTree;
    }

    private void showHistory() {
        m_inHistory = true;
        m_inGame = false;
        setWidgetActivation();
        final GameData clonedGameData;
        m_data.acquireReadLock();
        try {
            clonedGameData = GameDataUtils.cloneGameData(m_data);
            if (clonedGameData == null) return;
            clonedGameData.testLocksOnRead();
            if (m_historySyncher != null) throw new IllegalStateException("Two history synchers?");
            m_historySyncher = new HistorySynchronizer(clonedGameData, m_game);
        } finally {
            m_data.releaseReadLock();
        }
        m_statsPanel.setGameData(clonedGameData);
        m_economyPanel.setGameData(clonedGameData);
        m_details.setGameData(clonedGameData);
        m_mapPanel.setGameData(clonedGameData);
        m_data.removeDataChangeListener(m_dataChangeListener);
        clonedGameData.addDataChangeListener(m_dataChangeListener);
        final HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(clonedGameData, m_mapPanel);
        m_tabsPanel.removeAll();
        m_tabsPanel.add("History", historyDetailPanel);
        m_tabsPanel.add("Stats", m_statsPanel);
        m_tabsPanel.add("Economy", m_economyPanel);
        m_tabsPanel.add("Territory", m_details);
        if (getEditMode()) m_tabsPanel.add("Edit", m_editPanel);
        if (m_actionButtons.getCurrent() != null) m_actionButtons.getCurrent().setActive(false);
        m_historyPanel.removeAll();
        m_historyPanel.setLayout(new BorderLayout());
        final JPopupMenu popup = new JPopupMenu();
        popup.add(new AbstractAction("Show Summary Log") {

            public void actionPerformed(final ActionEvent ae) {
                final HistoryLog historyLog = new HistoryLog();
                historyLog.printRemainingTurn(m_historyTree.getCurrentPopupNode(), false, m_data.getDiceSides());
                historyLog.printTerritorySummary(clonedGameData);
                historyLog.printProductionSummary(clonedGameData);
                m_historyTree.clearCurrentPopupNode();
                historyLog.setVisible(true);
            }
        });
        popup.add(new AbstractAction("Show Detailed Log") {

            public void actionPerformed(final ActionEvent ae) {
                final HistoryLog historyLog = new HistoryLog();
                historyLog.printRemainingTurn(m_historyTree.getCurrentPopupNode(), true, m_data.getDiceSides());
                historyLog.printTerritorySummary(clonedGameData);
                historyLog.printProductionSummary(clonedGameData);
                m_historyTree.clearCurrentPopupNode();
                historyLog.setVisible(true);
            }
        });
        popup.add(new AbstractAction("Save Screenshot") {

            public void actionPerformed(final ActionEvent ae) {
                saveScreenshot(m_historyTree.getCurrentPopupNode());
                m_historyTree.clearCurrentPopupNode();
            }
        });
        final JSplitPane split = new JSplitPane();
        split.setOneTouchExpandable(true);
        split.setDividerSize(8);
        m_historyTree = new HistoryPanel(clonedGameData, historyDetailPanel, popup, m_uiContext);
        split.setLeftComponent(m_historyTree);
        split.setRightComponent(m_gameCenterPanel);
        split.setDividerLocation(150);
        m_historyPanel.add(split, BorderLayout.CENTER);
        m_historyPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
        getContentPane().removeAll();
        getContentPane().add(m_historyPanel, BorderLayout.CENTER);
        validate();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void show() {
        super.show();
    }

    public void showGame() {
        m_inGame = true;
        m_uiContext.setShowMapOnly(false);
        if (m_inHistory) {
            m_inHistory = false;
            if (m_historySyncher != null) {
                m_historySyncher.deactivate();
                m_historySyncher = null;
            }
            m_historyTree.goToEnd();
            m_historyTree = null;
            m_mapPanel.getData().removeDataChangeListener(m_dataChangeListener);
            m_statsPanel.setGameData(m_data);
            m_economyPanel.setGameData(m_data);
            m_details.setGameData(m_data);
            m_mapPanel.setGameData(m_data);
            m_data.addDataChangeListener(m_dataChangeListener);
            m_tabsPanel.removeAll();
        }
        setWidgetActivation();
        m_tabsPanel.add("Action", m_actionButtons);
        m_tabsPanel.add("Stats", m_statsPanel);
        m_tabsPanel.add("Economy", m_economyPanel);
        m_tabsPanel.add("Territory", m_details);
        if (getEditMode()) m_tabsPanel.add("Edit", m_editPanel);
        if (m_actionButtons.getCurrent() != null) m_actionButtons.getCurrent().setActive(true);
        m_gameMainPanel.removeAll();
        m_gameMainPanel.setLayout(new BorderLayout());
        m_gameMainPanel.add(m_gameCenterPanel, BorderLayout.CENTER);
        m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
        getContentPane().removeAll();
        getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
        m_mapPanel.setRoute(null);
        validate();
    }

    public void showMapOnly() {
        if (m_inHistory) {
            m_inHistory = false;
            if (m_historySyncher != null) {
                m_historySyncher.deactivate();
                m_historySyncher = null;
            }
            m_historyTree.goToEnd();
            m_historyTree = null;
            m_mapPanel.getData().removeDataChangeListener(m_dataChangeListener);
            m_mapPanel.setGameData(m_data);
            m_data.addDataChangeListener(m_dataChangeListener);
            m_gameMainPanel.removeAll();
            m_gameMainPanel.setLayout(new BorderLayout());
            m_gameMainPanel.add(m_mapAndChatPanel, BorderLayout.CENTER);
            m_gameMainPanel.add(m_rightHandSidePanel, BorderLayout.EAST);
            m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
            getContentPane().removeAll();
            getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
            m_mapPanel.setRoute(null);
        } else {
            m_inGame = false;
        }
        m_uiContext.setShowMapOnly(true);
        setWidgetActivation();
        validate();
    }

    public boolean saveScreenshot(final HistoryNode node, final File file) {
        final MapPanel mapPanel = getMapPanel();
        boolean retval = true;
        int round = 0;
        String step = null;
        PlayerID player = null;
        final Object[] pathFromRoot = node.getPath();
        for (final Object pathNode : pathFromRoot) {
            final HistoryNode curNode = (HistoryNode) pathNode;
            if (curNode instanceof Round) round = ((Round) curNode).getRoundNo();
            if (curNode instanceof Step) {
                player = ((Step) curNode).getPlayerID();
                step = curNode.getTitle();
            }
        }
        final double scale = m_uiContext.getScale();
        final BufferedImage mapImage = Util.createImage((int) (scale * mapPanel.getImageWidth()), (int) (scale * mapPanel.getImageHeight()), false);
        final Graphics2D mapGraphics = mapImage.createGraphics();
        try {
            final GameData data = mapPanel.getData();
            data.acquireReadLock();
            try {
                final int xOffset = mapPanel.getXOffset();
                final int yOffset = mapPanel.getYOffset();
                mapPanel.setTopLeft(0, 0);
                mapPanel.print(mapGraphics);
                mapPanel.setTopLeft(xOffset, yOffset);
            } finally {
                data.releaseReadLock();
            }
            Color title_color = m_uiContext.getMapData().getColorProperty("screenshot.title.color");
            if (title_color == null) title_color = Color.BLACK;
            final String s_title_x = m_uiContext.getMapData().getProperty("screenshot.title.x");
            final String s_title_y = m_uiContext.getMapData().getProperty("screenshot.title.y");
            final String s_title_size = m_uiContext.getMapData().getProperty("screenshot.title.font.size");
            int title_x;
            int title_y;
            int title_size;
            try {
                title_x = (int) (Integer.parseInt(s_title_x) * scale);
                title_y = (int) (Integer.parseInt(s_title_y) * scale);
                title_size = Integer.parseInt(s_title_size);
            } catch (final NumberFormatException nfe) {
                title_x = (int) (15 * scale);
                title_y = (int) (15 * scale);
                title_size = 15;
            }
            final AffineTransform transform = new AffineTransform();
            transform.scale(scale, scale);
            mapGraphics.setTransform(transform);
            mapGraphics.setFont(new Font("Ariel", Font.BOLD, title_size));
            mapGraphics.setColor(title_color);
            mapGraphics.drawString("Round " + round + ": " + (player != null ? player.getName() : "") + " - " + step, title_x, title_y);
            final boolean stats_enabled = m_uiContext.getMapData().getBooleanProperty("screenshot.stats.enabled");
            if (stats_enabled) {
                Color stats_text_color = m_uiContext.getMapData().getColorProperty("screenshot.text.color");
                if (stats_text_color == null) stats_text_color = Color.BLACK;
                Color stats_border_color = m_uiContext.getMapData().getColorProperty("screenshot.border.color");
                if (stats_border_color == null) stats_border_color = Color.WHITE;
                final String s_stats_x = m_uiContext.getMapData().getProperty("screenshot.stats.x");
                final String s_stats_y = m_uiContext.getMapData().getProperty("screenshot.stats.y");
                int stats_x;
                int stats_y;
                try {
                    stats_x = (int) (Integer.parseInt(s_stats_x) * scale);
                    stats_y = (int) (Integer.parseInt(s_stats_y) * scale);
                } catch (final NumberFormatException nfe) {
                    stats_x = (int) (120 * scale);
                    stats_y = (int) (70 * scale);
                }
                final JTable table = m_statsPanel.getStatsTable();
                final javax.swing.table.TableCellRenderer oldRenderer = table.getDefaultRenderer(Object.class);
                final Font oldTableFont = table.getFont();
                final Font oldTableHeaderFont = table.getTableHeader().getFont();
                final Dimension oldTableSize = table.getSize();
                final Color oldTableFgColor = table.getForeground();
                final Color oldTableSelFgColor = table.getSelectionForeground();
                final int oldCol0Width = table.getColumnModel().getColumn(0).getPreferredWidth();
                final int oldCol2Width = table.getColumnModel().getColumn(2).getPreferredWidth();
                table.setOpaque(false);
                table.setFont(new Font("Ariel", Font.BOLD, 15));
                table.setForeground(stats_text_color);
                table.setSelectionForeground(table.getForeground());
                table.setGridColor(stats_border_color);
                table.getTableHeader().setFont(new Font("Ariel", Font.BOLD, 15));
                table.getColumnModel().getColumn(0).setPreferredWidth(80);
                table.getColumnModel().getColumn(2).setPreferredWidth(90);
                table.setSize(table.getPreferredSize());
                table.doLayout();
                final int tableWidth = table.getSize().width;
                final int tableHeight = table.getSize().height;
                final int hdrWidth = tableWidth;
                final int hdrHeight = table.getTableHeader().getSize().height;
                final BufferedImage tblHdrImage = Util.createImage(hdrWidth, hdrHeight, false);
                final Graphics2D tblHdrGraphics = tblHdrImage.createGraphics();
                final BufferedImage tblImage = Util.createImage(tableWidth, tableHeight, true);
                final Graphics2D tblGraphics = tblImage.createGraphics();
                final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {

                    {
                        setOpaque(false);
                    }
                };
                table.setDefaultRenderer(Object.class, renderer);
                table.getTableHeader().print(tblHdrGraphics);
                table.print(tblGraphics);
                mapGraphics.drawImage(tblHdrImage, stats_x, stats_y, null);
                mapGraphics.drawImage(tblImage, stats_x, stats_y + (int) (hdrHeight * scale), null);
                tblHdrGraphics.dispose();
                tblGraphics.dispose();
                m_statsPanel.setStatsBgImage(null);
                tblHdrImage.flush();
                tblImage.flush();
                table.setDefaultRenderer(Object.class, oldRenderer);
                table.setOpaque(true);
                table.setForeground(oldTableFgColor);
                table.setSelectionForeground(oldTableSelFgColor);
                table.setFont(oldTableFont);
                table.getTableHeader().setFont(oldTableHeaderFont);
                table.setSize(oldTableSize);
                table.getColumnModel().getColumn(0).setPreferredWidth(oldCol0Width);
                table.getColumnModel().getColumn(2).setPreferredWidth(oldCol2Width);
                table.doLayout();
            }
            try {
                ImageIO.write(mapImage, "png", file);
            } catch (final Exception e2) {
                e2.printStackTrace();
                JOptionPane.showMessageDialog(TripleAFrame.this, e2.getMessage(), "Error saving Screenshot", JOptionPane.OK_OPTION);
                retval = false;
            }
        } finally {
            mapImage.flush();
            mapGraphics.dispose();
        }
        return retval;
    }

    public void saveScreenshot(final HistoryNode node) {
        final FileFilter pngFilter = new FileFilter() {

            @Override
            public boolean accept(final File f) {
                if (f.isDirectory()) return true; else return f.getName().endsWith(".png");
            }

            @Override
            public String getDescription() {
                return "Saved Screenshots, *.png";
            }
        };
        final JFileChooser fileChooser = new SaveGameFileChooser();
        fileChooser.setFileFilter(pngFilter);
        final int rVal = fileChooser.showSaveDialog(this);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getParent(), f.getName() + ".png");
            if (f.exists()) {
                final int choice = JOptionPane.showConfirmDialog(this, "A file by that name already exists. Do you wish to over write it?", "Over-write?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.OK_OPTION) return;
            }
            final File file = f;
            final Runnable t = new Runnable() {

                public void run() {
                    if (saveScreenshot(node, file)) JOptionPane.showMessageDialog(TripleAFrame.this, "Screenshot Saved", "Screenshot Saved", JOptionPane.INFORMATION_MESSAGE);
                }
            };
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(t);
                } catch (final Exception e2) {
                    e2.printStackTrace();
                }
            } else {
                t.run();
            }
        }
    }

    private void setWidgetActivation() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    setWidgetActivation();
                }
            });
            return;
        }
        if (m_showHistoryAction != null) {
            m_showHistoryAction.setEnabled(!(m_inHistory || m_uiContext.getShowMapOnly()));
        }
        if (m_showGameAction != null) {
            m_showGameAction.setEnabled(!m_inGame);
        }
        if (m_showMapOnlyAction != null) {
            boolean foundHuman = false;
            for (final IGamePlayer gamePlayer : m_localPlayers) {
                if (gamePlayer instanceof TripleAPlayer) {
                    foundHuman = true;
                }
            }
            if (!foundHuman) {
                m_showMapOnlyAction.setEnabled(m_inGame || m_inHistory);
            } else {
                m_showMapOnlyAction.setEnabled(false);
            }
        }
        if (m_editModeButtonModel != null) {
            if (m_editDelegate == null || m_uiContext.getShowMapOnly()) {
                m_editModeButtonModel.setEnabled(false);
            } else {
                m_editModeButtonModel.setEnabled(true);
            }
        }
    }

    public void setEditDelegate(final IEditDelegate editDelegate) {
        m_editDelegate = editDelegate;
        m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
        setWidgetActivation();
    }

    public IEditDelegate getEditDelegate() {
        return m_editDelegate;
    }

    public ButtonModel getEditModeButtonModel() {
        return m_editModeButtonModel;
    }

    public ButtonModel getShowCommentLogButtonModel() {
        return m_showCommentLogButtonModel;
    }

    public boolean getEditMode() {
        boolean isEditMode = false;
        m_mapPanel.getData().acquireReadLock();
        try {
            isEditMode = EditDelegate.getEditMode(m_mapPanel.getData());
        } finally {
            m_mapPanel.getData().releaseReadLock();
        }
        return isEditMode;
    }

    private AbstractAction m_showCommentLogAction = new AbstractAction() {

        public void actionPerformed(final ActionEvent ae) {
            if (((ButtonModel) ae.getSource()).isSelected()) {
                showCommentLog();
            } else {
                hideCommentLog();
            }
        }

        private void hideCommentLog() {
            if (m_chatPanel != null) {
                m_commentSplit.setBottomComponent(null);
                m_chatSplit.setBottomComponent(m_chatPanel);
                m_chatSplit.validate();
            } else {
                m_mapAndChatPanel.removeAll();
                m_chatSplit.setTopComponent(null);
                m_chatSplit.setBottomComponent(null);
                m_mapAndChatPanel.add(m_mapPanel, BorderLayout.CENTER);
                m_mapAndChatPanel.validate();
            }
        }

        private void showCommentLog() {
            if (m_chatPanel != null) {
                m_commentSplit.setBottomComponent(m_chatPanel);
                m_chatSplit.setBottomComponent(m_commentSplit);
                m_chatSplit.validate();
            } else {
                m_mapAndChatPanel.removeAll();
                m_chatSplit.setTopComponent(m_mapPanel);
                m_chatSplit.setBottomComponent(m_commentPanel);
                m_mapAndChatPanel.add(m_chatSplit, BorderLayout.CENTER);
                m_mapAndChatPanel.validate();
            }
        }
    };

    private AbstractAction m_showHistoryAction = new AbstractAction("Show history") {

        public void actionPerformed(final ActionEvent e) {
            showHistory();
            m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
        }
    };

    private AbstractAction m_showGameAction = new AbstractAction("Show current game") {

        {
            setEnabled(false);
        }

        public void actionPerformed(final ActionEvent e) {
            showGame();
            m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
        }
    };

    private AbstractAction m_showMapOnlyAction = new AbstractAction("Show map only") {

        public void actionPerformed(final ActionEvent e) {
            showMapOnly();
            m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
        }
    };

    private final AbstractAction m_saveScreenshotAction = new AbstractAction("Export Screenshot...") {

        public void actionPerformed(final ActionEvent e) {
            HistoryNode curNode = null;
            if (m_historyTree == null) curNode = m_data.getHistory().getLastNode(); else curNode = m_historyTree.getCurrentNode();
            saveScreenshot(curNode);
        }
    };

    public Collection<Unit> moveFightersToCarrier(final Collection<Unit> fighters, final Territory where) {
        final AtomicReference<ScrollableTextField> textRef = new AtomicReference<ScrollableTextField>();
        final AtomicReference<JPanel> panelRef = new AtomicReference<JPanel>();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    final JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    final ScrollableTextField text = new ScrollableTextField(0, fighters.size());
                    text.setBorder(new EmptyBorder(8, 8, 8, 8));
                    panel.add(text, BorderLayout.CENTER);
                    panel.add(new JLabel("How many fighters do you want to move from " + where.getName() + " to new carrier?"), BorderLayout.NORTH);
                    panelRef.set(panel);
                    textRef.set(text);
                    panelRef.set(panel);
                }
            });
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        final int choice = EventThreadJOptionPane.showOptionDialog(this, panelRef.get(), "Place fighters on new carrier?", JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[] { "OK", "Cancel" }, "OK");
        if (choice == 0) {
            return new ArrayList<Unit>(new ArrayList<Unit>(fighters).subList(0, textRef.get().getValue()));
        } else return new ArrayList<Unit>(0);
    }

    public BattlePanel getBattlePanel() {
        return m_actionButtons.getBattlePanel();
    }

    public AbstractMovePanel getMovePanel() {
        return m_actionButtons.getMovePanel();
    }

    public TechPanel getTechPanel() {
        return m_actionButtons.getTechPanel();
    }

    public PlacePanel getPlacePanel() {
        return m_actionButtons.getPlacePanel();
    }

    public PurchasePanel getPurchasePanel() {
        return m_actionButtons.getPurchasePanel();
    }

    Action getShowGameAction() {
        return m_showGameAction;
    }

    Action getShowHistoryAction() {
        return m_showHistoryAction;
    }

    Action getShowMapOnlyAction() {
        return m_showMapOnlyAction;
    }

    Action getSaveScreenshotAction() {
        return m_saveScreenshotAction;
    }

    public UIContext getUIContext() {
        return m_uiContext;
    }

    MapPanel getMapPanel() {
        return m_mapPanel;
    }

    void updateMap(final String mapdir) throws IOException {
        m_uiContext.setMapDir(m_data, mapdir);
        if (m_uiContext.getMapData().getHasRelief()) {
            TileImageFactory.setShowReliefImages(true);
        }
        m_mapPanel.setGameData(m_data);
        m_mapPanel.changeImage(m_uiContext.getMapData().getMapDimensions());
        final Image small = m_uiContext.getMapImage().getSmallMapImage();
        m_smallView.changeImage(small);
        m_mapPanel.resetMap();
    }

    @Override
    public IGame getGame() {
        return m_game;
    }

    public StatPanel getStatPanel() {
        return m_statsPanel;
    }

    void setShowChatTime(final boolean showTime) {
        m_chatPanel.setShowChatTime(showTime);
    }
}

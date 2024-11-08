package br.furb.inf.tcc.tankcoders.menu;

import java.net.InetAddress;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.fenggui.ComboBox;
import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.FengGUI;
import org.fenggui.GameMenuButton;
import org.fenggui.Label;
import org.fenggui.ScrollContainer;
import org.fenggui.composites.MessageWindow;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.ISelectionChangedListener;
import org.fenggui.event.IWindowClosedListener;
import org.fenggui.event.SelectionChangedEvent;
import org.fenggui.event.WindowClosedEvent;
import org.fenggui.layout.BorderLayoutData;
import org.fenggui.layout.RowLayout;
import org.fenggui.layout.StaticLayout;
import org.fenggui.render.Font;
import org.fenggui.table.ITableModel;
import org.fenggui.table.Table;
import org.fenggui.util.Alphabet;
import org.fenggui.util.Color;
import org.fenggui.util.Point;
import org.fenggui.util.Spacing;
import org.fenggui.util.fonttoolkit.FontFactory;
import br.furb.inf.tcc.server.OnlinePlayer;
import br.furb.inf.tcc.tankcoders.TankCoders;
import br.furb.inf.tcc.tankcoders.client.GameClient;
import br.furb.inf.tcc.tankcoders.game.GameRulesConstants;
import br.furb.inf.tcc.tankcoders.game.Player;
import br.furb.inf.tcc.tankcoders.game.PlayerTank;
import br.furb.inf.tcc.tankcoders.game.PlayerTeam;
import br.furb.inf.tcc.tankcoders.game.StartGameArguments;
import br.furb.inf.tcc.tankcoders.game.TankModel;
import br.furb.inf.tcc.tankcoders.game.TankTeam;
import br.furb.inf.tcc.tankcoders.message.AnotherPlayerChangeModelResponse;
import br.furb.inf.tcc.tankcoders.message.PlayerChangeTeamResponse;
import br.furb.inf.tcc.tankcoders.message.AnotherUserLogonRespose;
import br.furb.inf.tcc.tankcoders.message.ChangeModel;
import br.furb.inf.tcc.tankcoders.message.ChangeTeam;
import br.furb.inf.tcc.tankcoders.message.InvalidChangeTeamResponse;
import br.furb.inf.tcc.tankcoders.message.StartBattle;
import br.furb.inf.tcc.tankcoders.message.UserLogonFailedResponse;
import br.furb.inf.tcc.tankcoders.message.UserLogonRespose;
import br.furb.inf.tcc.tankcoders.message.UserReady;
import br.furb.inf.tcc.tankcoders.message.UserUnready;
import br.furb.inf.tcc.util.lang.GameLanguage;
import br.furb.inf.tcc.util.ui.FengGuiUtils;
import br.furb.inf.tcc.util.ui.ToogleGameMenuButton;
import com.captiveimagination.jgn.clientserver.JGNConnection;
import com.captiveimagination.jgn.clientserver.JGNConnectionListener;
import com.captiveimagination.jgn.event.MessageListener;
import com.captiveimagination.jgn.message.Message;
import com.jme.util.GameTaskQueueManager;

/**
 * Prepare battle page.
 * @author Germano Fronza
 */
public class PrepareBattleMenuGUI {

    private Display display;

    private Container cGeneral;

    private Label labelBattleName;

    private Label labelPlayerStatus;

    private Label labelTeam1Name;

    private Label labelTeam2Name;

    private GameMenuButton back;

    private ToogleGameMenuButton ready;

    private Table tablePlayerTanks = new Table();

    private ComboBox<String>[] cbModelsArray;

    private ComboBox<String>[] cbTeamsArray;

    private Map<String, Object> serverInfos;

    private TankTeam team1;

    private TankTeam team2;

    GameClient gameClient;

    /** Player of this game instance */
    private Player localPlayer;

    /** List of all players. Sync by server */
    private Map<Short, Player> players;

    /** Map used to bind the player id with the table model matrix */
    private Map<Short, Integer> mapOfPlayersIdAndMatrixIndex;

    private Map<Integer, Short> mapOfMatrixIndexAndPlayersId;

    private static final String UNREADY_MSG = GameLanguage.getString("player.status.ready");

    private static final String READY_MSG = GameLanguage.getString("player.status.unready");

    public PrepareBattleMenuGUI(Map<String, Object> serverInfos, Player player) {
        this.serverInfos = serverInfos;
        team1 = new TankTeam(PlayerTeam.TEAM_1);
        team1.setName((String) serverInfos.get(JoinGameMenuGUI.TEAM1NAME_ENTRY));
        team2 = new TankTeam(PlayerTeam.TEAM_2);
        team2.setName((String) serverInfos.get(JoinGameMenuGUI.TEAM2NAME_ENTRY));
        localPlayer = player;
        players = new HashMap<Short, Player>();
        mapOfPlayersIdAndMatrixIndex = new HashMap<Short, Integer>();
        mapOfMatrixIndexAndPlayersId = new HashMap<Integer, Short>();
    }

    public void buildGUI(Display display) {
        this.display = display;
        cGeneral = new Container();
        cGeneral.setMinSize(754, 473);
        cGeneral.setSizeToMinSize();
        cGeneral.getAppearance().add(MenuPixmapBackgrounds.getPrepareBattlePixmapBackground());
        cGeneral.setLayoutManager(new StaticLayout());
        StaticLayout.center(cGeneral, display);
        display.addWidget(cGeneral);
        final Container cLabelBattleName = new Container();
        cLabelBattleName.getAppearance().setPadding(new Spacing(0, 10));
        cLabelBattleName.setLayoutManager(new RowLayout(false));
        cGeneral.addWidget(cLabelBattleName);
        final Container cPlayersTable = new Container();
        cPlayersTable.getAppearance().setPadding(new Spacing(0, 10));
        cPlayersTable.setLayoutManager(new RowLayout(false));
        cGeneral.addWidget(cPlayersTable);
        final Container cButtonBack = new Container();
        cButtonBack.getAppearance().setPadding(new Spacing(0, 10));
        cButtonBack.setLayoutManager(new RowLayout(true));
        cGeneral.addWidget(cButtonBack);
        final Container cReadyBack = new Container();
        cReadyBack.getAppearance().setPadding(new Spacing(0, 10));
        cReadyBack.setLayoutManager(new RowLayout(true));
        cGeneral.addWidget(cReadyBack);
        initComponents(cGeneral, display);
        buildComponents(cLabelBattleName, cPlayersTable, cButtonBack, cReadyBack, cGeneral);
        connectToServerAndRegisterPlayer();
    }

    private Font createAntiAliasedFont() {
        return FontFactory.renderStandardFont(new java.awt.Font("Sans", java.awt.Font.BOLD, 14), true, Alphabet.getDefaultAlphabet());
    }

    private void initComponents(final Container parentContainer, final Display display) {
        String battleName = (String) serverInfos.get(JoinGameMenuGUI.SERVERNAME_ENTRY);
        String team1Name = team1.getName();
        String team2Name = team2.getName();
        labelBattleName = FengGUI.createLabel(battleName);
        labelBattleName.getAppearance().setTextColor(Color.BLACK);
        labelBattleName.getAppearance().setFont(FontFactory.renderStandardFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 18)));
        labelPlayerStatus = FengGUI.createLabel(UNREADY_MSG);
        labelPlayerStatus.getAppearance().setTextColor(Color.BLACK);
        labelPlayerStatus.getAppearance().setFont(FontFactory.renderStandardFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 14)));
        labelTeam1Name = FengGUI.createLabel("Team 1: " + team1Name);
        labelTeam1Name.getAppearance().setTextColor(Color.BLUE);
        labelTeam1Name.getAppearance().setFont(FontFactory.renderStandardFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 14)));
        labelTeam2Name = FengGUI.createLabel("Team 2: " + team2Name);
        labelTeam2Name.getAppearance().setTextColor(Color.RED);
        labelTeam2Name.getAppearance().setFont(FontFactory.renderStandardFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 14)));
        tablePlayerTanks = new Table();
        tablePlayerTanks.getAppearance().setGridColor(Color.BLACK);
        tablePlayerTanks.getAppearance().setHeaderBackgroundColor(Color.BLACK);
        tablePlayerTanks.getAppearance().setHeadTextColor(Color.WHITE);
        tablePlayerTanks.getAppearance().setSelectionColor(Color.DARK_YELLOW);
        tablePlayerTanks.getAppearance().setFont(createAntiAliasedFont());
        tablePlayerTanks.setModel(new PlayerTanksTableModel());
        back = new GameMenuButton(FengGuiUtils.getRsrc("data/images/buttons/btn_back_0.png"), FengGuiUtils.getRsrc("data/images/buttons/btn_back_1.png"));
        back.addButtonPressedListener(new IButtonPressedListener() {

            public void buttonPressed(ButtonPressedEvent e) {
                disconnectAndReturnPreviouslyMenu(true, null);
            }
        });
        ready = new ToogleGameMenuButton(FengGuiUtils.getRsrc("data/images/buttons/btn_ready_0.png"), FengGuiUtils.getRsrc("data/images/buttons/btn_ready_1.png"), (FengGuiUtils.getRsrc("data/images/buttons/btn_ready_2.png")));
        ready.addButtonPressedListener(new IButtonPressedListener() {

            public void buttonPressed(ButtonPressedEvent e) {
                if (ready.isPressed()) {
                    labelPlayerStatus.setText(READY_MSG);
                    changeVisibleComboBoxComponents(false);
                    UserReady ur = new UserReady();
                    ur.setPlayerId(localPlayer.getId());
                    gameClient.sendToServer(ur);
                } else {
                    labelPlayerStatus.setText(UNREADY_MSG);
                    changeVisibleComboBoxComponents(true);
                    UserUnready uur = new UserUnready();
                    uur.setPlayerId(localPlayer.getId());
                    gameClient.sendToServer(uur);
                }
            }
        });
    }

    private void buildComponents(Container cBattleName, Container cPlayersTable, Container cButtonBack, Container cReadyBack, final Container parentContainer) {
        cBattleName.removeAllWidgets();
        cBattleName.addWidget(labelBattleName);
        cBattleName.addWidget(labelPlayerStatus);
        cBattleName.pack();
        cBattleName.setPosition(new Point(0, 430));
        final Container cTeamNames = new Container();
        cTeamNames.getAppearance().setPadding(new Spacing(0, 10));
        cTeamNames.setLayoutManager(new RowLayout(true));
        cTeamNames.addWidget(labelTeam1Name);
        cTeamNames.addWidget(FengGUI.createLabel("  |  "));
        cTeamNames.addWidget(labelTeam2Name);
        cTeamNames.pack();
        cTeamNames.setPosition(new Point(0, 312));
        cGeneral.addWidget(cTeamNames);
        final ScrollContainer sc = new ScrollContainer();
        sc.setLayoutData(BorderLayoutData.CENTER);
        cPlayersTable.addWidget(sc);
        sc.setInnerWidget(tablePlayerTanks);
        cPlayersTable.setPosition(new Point(0, -190));
        cPlayersTable.setSize(750, 500);
        tablePlayerTanks.getColumn(0).setWidth(200);
        tablePlayerTanks.getColumn(1).setWidth(270);
        tablePlayerTanks.getColumn(2).setWidth(130);
        tablePlayerTanks.getColumn(3).setWidth(130);
        cButtonBack.removeAllWidgets();
        cButtonBack.addWidget(back);
        cButtonBack.pack();
        cButtonBack.setPosition(new Point(0, 25));
        cReadyBack.removeAllWidgets();
        cReadyBack.addWidget(ready);
        cReadyBack.pack();
        cReadyBack.setPosition(new Point(630, 25));
    }

    private void changeVisibleComboBoxComponents(boolean visible) {
        for (ComboBox<String> combo : cbModelsArray) {
            if (combo != null) {
                combo.setVisible(visible);
            }
        }
        for (ComboBox<String> combo : cbTeamsArray) {
            if (combo != null) {
                combo.setVisible(visible);
            }
        }
    }

    class PlayerTanksTableModel implements ITableModel {

        static final int PLAYERNAME_INDEX = 0;

        static final int TANKNAME_INDEX = 1;

        static final int TANKMODEL_INDEX = 2;

        static final int TANKTEAM_INDEX = 3;

        int rowCount;

        int nextIndex = 0;

        String[] header = { "menu.prepareBattle.tableHeader.playerName", "menu.prepareBattle.tableHeader.tankName", "menu.prepareBattle.tableHeader.tankModel", "menu.prepareBattle.tableHeader.tankTeam" };

        String[][] matrix;

        public PlayerTanksTableModel() {
            createModel();
        }

        public void update() {
            createModel();
        }

        @SuppressWarnings("unchecked")
        private void createModel() {
            rowCount = GameRulesConstants.MAX_TANKS;
            matrix = new String[rowCount][header.length];
            cbModelsArray = new ComboBox[rowCount];
            cbTeamsArray = new ComboBox[rowCount];
        }

        @SuppressWarnings("unchecked")
        public void addPlayers(final Player[] players) {
            try {
                GameTaskQueueManager.getManager().render(new Callable<Object>() {

                    public Object call() throws Exception {
                        for (Player player : players) {
                            String pName = player.getName();
                            for (PlayerTank tank : player.getTanks()) {
                                PrepareBattleMenuGUI.this.players.put(player.getId(), player);
                                mapOfPlayersIdAndMatrixIndex.put(player.getId(), nextIndex);
                                mapOfMatrixIndexAndPlayersId.put(nextIndex, player.getId());
                                matrix[nextIndex][PLAYERNAME_INDEX] = pName;
                                matrix[nextIndex][TANKNAME_INDEX] = tank.getTankName();
                                matrix[nextIndex][TANKMODEL_INDEX] = "      " + GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.TankModel." + tank.getModel());
                                matrix[nextIndex][TANKTEAM_INDEX] = "      " + GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.PlayerTeam." + tank.getTeam().getTeamEnum());
                                if (player == localPlayer) {
                                    int y = 276 - (nextIndex * 19);
                                    ComboBox<String> cbModels = FengGUI.createComboBox(cGeneral);
                                    cbModels.addItem(GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.TankModel.M1_ABRAMS"));
                                    cbModels.addItem(GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.TankModel.JADGE_PANTHER"));
                                    cbModels.setXY(485, y);
                                    cbModels.addSelectionChangedListener(new ComboBoxModelsSelectionListener(cbModels));
                                    cbModelsArray[nextIndex] = cbModels;
                                    ComboBox<String> cbTeams = FengGUI.createComboBox(cGeneral);
                                    cbTeams.addItem(GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.PlayerTeam.TEAM_1"));
                                    cbTeams.addItem(GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.PlayerTeam.TEAM_2"));
                                    cbTeams.setXY(615, y);
                                    cbTeams.addSelectionChangedListener(new ComboBoxTeamsSelectionListener(cbTeams));
                                    cbTeamsArray[nextIndex] = cbTeams;
                                }
                                nextIndex++;
                            }
                        }
                        return null;
                    }
                }).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void changePlayerTankTeam(final short playerId, final String tankName, final PlayerTeam pt, final int newSlotLocation) {
            try {
                GameTaskQueueManager.getManager().render(new Callable<Object>() {

                    public Object call() throws Exception {
                        if (playerId == localPlayer.getId() && newSlotLocation != -1) {
                            PlayerTank tank = localPlayer.getTankByName(tankName);
                            tank.setInitialSlotLocation(newSlotLocation);
                        } else {
                            if (mapOfPlayersIdAndMatrixIndex.containsKey(playerId)) {
                                TankTeam tt = getTeamObjByEnum(pt);
                                PlayerTank tank = players.get(playerId).getTankByName(tankName);
                                if (newSlotLocation != -1) {
                                    tank.setInitialSlotLocation(newSlotLocation);
                                }
                                tank.setTeam(tt);
                                int matrixIndex = mapOfPlayersIdAndMatrixIndex.get(playerId);
                                matrix[matrixIndex][TANKTEAM_INDEX] = "      " + GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.PlayerTeam." + pt);
                            }
                        }
                        return null;
                    }
                }).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void changePlayerTankModel(final short playerId, final String tankName, final TankModel tm) {
            try {
                GameTaskQueueManager.getManager().render(new Callable<Object>() {

                    public Object call() throws Exception {
                        if (mapOfPlayersIdAndMatrixIndex.containsKey(playerId)) {
                            PlayerTank tank = players.get(playerId).getTankByName(tankName);
                            tank.setModel(tm);
                            int matrixIndex = mapOfPlayersIdAndMatrixIndex.get(playerId);
                            matrix[matrixIndex][TANKMODEL_INDEX] = "      " + GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.TankModel." + tm);
                        }
                        return null;
                    }
                }).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void removePlayer(final short playerId) {
            try {
                GameTaskQueueManager.getManager().render(new Callable<Object>() {

                    public Object call() throws Exception {
                        if (mapOfPlayersIdAndMatrixIndex.containsKey(playerId)) {
                            int index = mapOfPlayersIdAndMatrixIndex.get(playerId);
                            matrix[index][PLAYERNAME_INDEX] = null;
                            matrix[index][TANKNAME_INDEX] = null;
                            matrix[index][TANKMODEL_INDEX] = null;
                            matrix[index][TANKTEAM_INDEX] = null;
                            if (index < (nextIndex - 1)) {
                                for (int i = index; i < nextIndex - 1; i++) {
                                    matrix[i][PLAYERNAME_INDEX] = matrix[i + 1][PLAYERNAME_INDEX];
                                    matrix[i][TANKNAME_INDEX] = matrix[i + 1][TANKNAME_INDEX];
                                    matrix[i][TANKMODEL_INDEX] = matrix[i + 1][TANKMODEL_INDEX];
                                    matrix[i][TANKTEAM_INDEX] = matrix[i + 1][TANKTEAM_INDEX];
                                    if (cbModelsArray[i + 1] != null) {
                                        cbModelsArray[i] = cbModelsArray[i + 1];
                                        cbTeamsArray[i] = cbTeamsArray[i + 1];
                                        int y = 276 - (i * 19);
                                        cbModelsArray[i].setY(y);
                                        cbTeamsArray[i].setY(y);
                                        cbModelsArray[i + 1] = null;
                                        cbTeamsArray[i + 1] = null;
                                    }
                                    matrix[i + 1][PLAYERNAME_INDEX] = null;
                                    matrix[i + 1][TANKNAME_INDEX] = null;
                                    matrix[i + 1][TANKMODEL_INDEX] = null;
                                    matrix[i + 1][TANKTEAM_INDEX] = null;
                                    short actualPlayerId = mapOfMatrixIndexAndPlayersId.get(i + 1);
                                    mapOfPlayersIdAndMatrixIndex.put(actualPlayerId, i);
                                    mapOfMatrixIndexAndPlayersId.remove(i + 1);
                                }
                            }
                            nextIndex--;
                            players.remove(playerId);
                            mapOfPlayersIdAndMatrixIndex.remove(playerId);
                            mapOfMatrixIndexAndPlayersId.remove(index);
                        }
                        return null;
                    }
                }).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String getColumnName(int columnIndex) {
            return GameLanguage.getString(header[columnIndex]);
        }

        public int getColumnCount() {
            return matrix[0].length;
        }

        public Object getValue(int row, int column) {
            return matrix[row][column];
        }

        public int getRowCount() {
            return matrix.length;
        }

        public void clear() {
        }

        class ComboBoxModelsSelectionListener implements ISelectionChangedListener {

            private ComboBox<String> owner;

            public ComboBoxModelsSelectionListener(ComboBox<String> owner) {
                this.owner = owner;
            }

            @Override
            public void selectionChanged(SelectionChangedEvent arg0) {
                for (int i = 0; i < cbModelsArray.length; i++) {
                    if (cbModelsArray[i] == owner) {
                        matrix[i][TANKMODEL_INDEX] = "      " + owner.getSelectedValue();
                        TankModel tm;
                        String modelAbramsStr = GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.TankModel.M1_ABRAMS");
                        if (owner.getSelectedValue().equals(modelAbramsStr)) {
                            tm = TankModel.M1_ABRAMS;
                        } else {
                            tm = TankModel.JADGE_PANTHER;
                        }
                        PlayerTank tank = localPlayer.getTankByName(matrix[i][TANKNAME_INDEX]);
                        if (tank.getModel() != tm) {
                            tank.setModel(tm);
                            ChangeModel cm = new ChangeModel();
                            cm.setPlayerId(localPlayer.getId());
                            cm.setTankName(matrix[i][TANKNAME_INDEX]);
                            cm.setModel(tm);
                            gameClient.sendToServer(cm);
                        }
                        break;
                    }
                }
            }
        }

        class ComboBoxTeamsSelectionListener implements ISelectionChangedListener {

            private ComboBox<String> owner;

            public ComboBoxTeamsSelectionListener(ComboBox<String> owner) {
                this.owner = owner;
            }

            public void selectionChanged(SelectionChangedEvent e) {
                if (e.isSelected()) {
                    for (int i = 0; i < cbTeamsArray.length; i++) {
                        if (cbTeamsArray[i] == owner) {
                            matrix[i][TANKTEAM_INDEX] = "      " + owner.getSelectedValue();
                            PlayerTeam pt;
                            String team1Str = GameLanguage.getString("br.furb.inf.tcc.tankcoders.game.PlayerTeam.TEAM_1");
                            if (owner.getSelectedValue().equals(team1Str)) {
                                pt = PlayerTeam.TEAM_1;
                            } else {
                                pt = PlayerTeam.TEAM_2;
                            }
                            PlayerTank tank = localPlayer.getTankByName(matrix[i][TANKNAME_INDEX]);
                            if (tank.getTeam().getTeamEnum() != pt) {
                                tank.setTeam(getTeamObjByEnum(pt));
                                ChangeTeam ct = new ChangeTeam();
                                ct.setPlayerId(localPlayer.getId());
                                ct.setTankName(matrix[i][TANKNAME_INDEX]);
                                ct.setTeam(pt);
                                gameClient.sendToServer(ct);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void connectToServerAndRegisterPlayer() {
        try {
            gameClient = GameClient.getInstance();
            gameClient.setPlayer(localPlayer);
            InetAddress serverAddress = InetAddress.getByName((String) serverInfos.get(JoinGameMenuGUI.ADDRESS_ENTRY));
            gameClient.setupCommunication(serverAddress);
            gameClient.loginPlayer();
            addServerConnectionListener();
            addClientConnectionListener();
            addServerMessageListener();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addServerConnectionListener() {
        gameClient.addServerConnectionListener(new JGNConnectionListener() {

            public void connected(JGNConnection con) {
            }

            public void disconnected(JGNConnection con) {
                MessageWindow mw = new MessageWindow(GameLanguage.getString("menu.prepareBattle.messages.serverDisconnected") + "!");
                mw.setTitle(GameLanguage.getString("windowMessage.warning"));
                mw.pack();
                display.addWidget(mw);
                StaticLayout.center(mw, display);
                mw.getOKButton().addButtonPressedListener(new IButtonPressedListener() {

                    public void buttonPressed(ButtonPressedEvent arg0) {
                        disconnectAndReturnPreviouslyMenu(false, null);
                    }
                });
                mw.addWindowClosedListener(new IWindowClosedListener() {

                    public void windowClosed(WindowClosedEvent arg0) {
                        disconnectAndReturnPreviouslyMenu(false, null);
                    }
                });
            }
        });
    }

    private void addClientConnectionListener() {
        gameClient.addClientConnectionListener(new JGNConnectionListener() {

            public void connected(JGNConnection con) {
            }

            public void disconnected(JGNConnection con) {
                ((PlayerTanksTableModel) tablePlayerTanks.getModel()).removePlayer(con.getPlayerId());
            }
        });
    }

    private void addServerMessageListener() {
        gameClient.addMessageListener(new MessageListener() {

            public void messageCertified(Message message) {
            }

            public void messageFailed(Message message) {
            }

            public void messageSent(Message message) {
            }

            public void messageReceived(Message msg) {
                try {
                    Message message = msg.clone();
                    if (message instanceof UserLogonRespose) {
                        processUserLogonResponse(message);
                    } else if (message instanceof UserLogonFailedResponse) {
                        processUserLogonFailedResponse(message);
                    } else if (message instanceof AnotherUserLogonRespose) {
                        processAnotherUserLogonResponse(message);
                    } else if (message instanceof PlayerChangeTeamResponse) {
                        processPlayerChangeTeamResponse(message);
                    } else if (message instanceof InvalidChangeTeamResponse) {
                        processInvalidChangeTeamResponse(message);
                    } else if (message instanceof AnotherPlayerChangeModelResponse) {
                        processAnotherPlayerChangeModelResponse(message);
                    } else if (message instanceof StartBattle) {
                        processStartBattleMessage(message);
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }

            private void processUserLogonFailedResponse(Message message) {
                final String cause = ((UserLogonFailedResponse) message).getCause();
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            GameTaskQueueManager.getManager().render(new Callable<Object>() {

                                public Object call() throws Exception {
                                    disconnectAndReturnPreviouslyMenu(true, cause);
                                    return null;
                                }
                            }).get();
                        } catch (Exception e) {
                        }
                    }
                }).start();
            }

            private void processAnotherUserLogonResponse(Message message) {
                AnotherUserLogonRespose aulr = (AnotherUserLogonRespose) message;
                final Player newPlayer = new Player(aulr.getPlayerType());
                newPlayer.setId(aulr.getPlayerId());
                newPlayer.setName(aulr.getPlayerName());
                PlayerTank[] tanks = aulr.getTanks();
                for (int i = 0; i < tanks.length; i++) {
                    TankTeam tt = getTeamObjByEnum(tanks[i].getTeam().getTeamEnum());
                    tanks[i].setTeam(tt);
                    newPlayer.addTank(tanks[i]);
                }
                new Thread(new Runnable() {

                    public void run() {
                        Player[] playerToAdd = new Player[1];
                        playerToAdd[0] = newPlayer;
                        ((PlayerTanksTableModel) tablePlayerTanks.getModel()).addPlayers(playerToAdd);
                    }
                }).start();
            }

            private void processUserLogonResponse(Message message) {
                PlayerTank[] tanks = ((UserLogonRespose) message).getTanks();
                for (int i = 0; i < tanks.length; i++) {
                    for (PlayerTank playerTank : localPlayer.getTanks()) {
                        if (tanks[i].getTankName().equals(playerTank.getTankName())) {
                            playerTank.setModel(tanks[i].getModel());
                            TankTeam tt = getTeamObjByEnum(tanks[i].getTeam().getTeamEnum());
                            playerTank.setTeam(tt);
                            playerTank.setInitialSlotLocation(tanks[i].getInitialSlotLocation());
                            break;
                        }
                    }
                }
                final OnlinePlayer[] otherOnlinePlayers = ((UserLogonRespose) message).getOnlinePlayers();
                new Thread(new Runnable() {

                    public void run() {
                        PlayerTanksTableModel model = ((PlayerTanksTableModel) tablePlayerTanks.getModel());
                        Player[] playerToAdd = new Player[otherOnlinePlayers.length + 1];
                        for (int i = 0; i < otherOnlinePlayers.length; i++) {
                            Player p = new Player(otherOnlinePlayers[i].getPlayerType());
                            p.setId(otherOnlinePlayers[i].getPlayerId());
                            p.setName(otherOnlinePlayers[i].getPlayerName());
                            PlayerTank[] pTanks = otherOnlinePlayers[i].getTanks();
                            for (int j = 0; j < pTanks.length; j++) {
                                p.addTank(pTanks[j]);
                            }
                            playerToAdd[i] = p;
                        }
                        playerToAdd[otherOnlinePlayers.length] = localPlayer;
                        model.addPlayers(playerToAdd);
                    }
                }).start();
            }

            private void processPlayerChangeTeamResponse(Message message) {
                final PlayerChangeTeamResponse ctr = (PlayerChangeTeamResponse) message;
                new Thread(new Runnable() {

                    public void run() {
                        PlayerTanksTableModel model = ((PlayerTanksTableModel) tablePlayerTanks.getModel());
                        model.changePlayerTankTeam(ctr.getPlayerId(), ctr.getTankName(), ctr.getTeam(), ctr.getNewInitialSlotLocation());
                    }
                }).start();
            }

            private void processAnotherPlayerChangeModelResponse(Message message) {
                final AnotherPlayerChangeModelResponse ctr = (AnotherPlayerChangeModelResponse) message;
                new Thread(new Runnable() {

                    public void run() {
                        PlayerTanksTableModel model = ((PlayerTanksTableModel) tablePlayerTanks.getModel());
                        model.changePlayerTankModel(ctr.getPlayerId(), ctr.getTankName(), ctr.getModel());
                    }
                }).start();
            }

            private void processInvalidChangeTeamResponse(Message message) {
                final InvalidChangeTeamResponse ictr = (InvalidChangeTeamResponse) message;
                final PlayerTeam tt = (ictr.getTeam() == PlayerTeam.TEAM_1) ? PlayerTeam.TEAM_2 : PlayerTeam.TEAM_1;
                new Thread(new Runnable() {

                    public void run() {
                        PlayerTanksTableModel model = ((PlayerTanksTableModel) tablePlayerTanks.getModel());
                        model.changePlayerTankTeam(localPlayer.getId(), ictr.getTankName(), tt, -1);
                    }
                }).start();
                String teamName = getTeamObjByEnum(ictr.getTeam()).getName();
                MessageWindow mw = new MessageWindow(new Formatter().format(GameLanguage.getString("team.doesNotSuportMoreTank"), teamName).toString());
                mw.setTitle(GameLanguage.getString("team.warningWindowTitle"));
                mw.pack();
                display.addWidget(mw);
                StaticLayout.center(mw, display);
            }

            private void processStartBattleMessage(final Message message) {
                MessageWindow mw = new MessageWindow("       " + GameLanguage.getString("battle.starting") + "       ");
                mw.setTitle(GameLanguage.getString("windowMessage.information"));
                mw.getCloseButton().setVisible(false);
                mw.pack();
                display.addWidget(mw);
                StaticLayout.center(mw, display);
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            GameTaskQueueManager.getManager().render(new Callable<Object>() {

                                public Object call() throws Exception {
                                    StartGameArguments gameArgs = new StartGameArguments();
                                    String imgHeightMap = ((StartBattle) message).getTerrainHeightmapImage();
                                    if (imgHeightMap != null) {
                                        gameArgs.setTerrainHeightMapImage(imgHeightMap);
                                    }
                                    gameArgs.setLocalPlayer(localPlayer);
                                    for (Player player : players.values()) {
                                        gameArgs.addPlayer(player);
                                    }
                                    TankCoders.getGameInstance().changeToInGameState(gameArgs);
                                    return null;
                                }
                            }).get();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    public TankTeam getTeamObjByEnum(PlayerTeam pt) {
        if (pt == PlayerTeam.TEAM_1) {
            return team1;
        } else {
            return team2;
        }
    }

    private void disconnectAndReturnPreviouslyMenu(boolean sendDisconnect, String disconnectionCause) {
        if (gameClient.isConnected()) {
            if (sendDisconnect) {
                gameClient.disconnectFromServer();
            }
            gameClient.closeClientSokects();
        }
        new JoinGameMenuGUI().buildGUI(display);
        if (disconnectionCause != null) {
            MessageWindow mw = new MessageWindow(disconnectionCause);
            mw.setTitle(GameLanguage.getString("server.disconnectionCauseWindowTitle"));
            mw.pack();
            display.addWidget(mw);
            StaticLayout.center(mw, display);
        }
    }
}

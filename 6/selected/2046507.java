package games.midhedava.client.gui;

import games.midhedava.client.midhedava;
import games.midhedava.client.GameObjects;
import games.midhedava.client.GameScreen;
import games.midhedava.client.StaticGameLayers;
import games.midhedava.client.MidhedavaClient;
import games.midhedava.client.MidhedavaUI;
import games.midhedava.client.entity.Entity;
import games.midhedava.client.entity.EntityView;
import games.midhedava.client.entity.Inspector;
import games.midhedava.client.entity.User;
import games.midhedava.client.gui.styled.Style;
import games.midhedava.client.gui.styled.WoodStyle;
import games.midhedava.client.gui.styled.swing.StyledJButton;
import games.midhedava.client.gui.wt.GroundContainer;
import games.midhedava.client.gui.wt.InternalManagedDialog;
import games.midhedava.client.gui.wt.SettingsPanel;
import games.midhedava.client.gui.wt.core.WtBaseframe;
import games.midhedava.client.gui.wt.core.WtPanel;
import games.midhedava.client.gui.wt.core.WtWindowManager;
import games.midhedava.client.sound.SoundSystem;
import games.midhedava.client.soundreview.SoundMaster;
import games.midhedava.client.sprite.SpriteStore;
import games.midhedava.client.update.ClientGameConfiguration;
import games.midhedava.common.CollisionDetection;
import games.midhedava.common.Direction;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.net.URL;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import marauroa.common.Log4J;
import marauroa.common.game.RPObject;
import org.apache.log4j.Logger;

/** The main class that create the screen and starts the arianne client. */
public class j2DClient extends MidhedavaUI {

    private static final long serialVersionUID = 3356310866399084117L;

    /** width of the game screen (without the chat line) */
    public static int SCREEN_WIDTH;

    /** height of the game screen (without the chat line) */
    public static int SCREEN_HEIGHT;

    static {
        String[] dim = games.midhedava.client.midhedava.SCREEN_SIZE.split("x");
        SCREEN_WIDTH = Integer.parseInt(dim[0]);
        SCREEN_HEIGHT = Integer.parseInt(dim[1]);
    }

    /** the logger instance. */
    private static final Logger logger = Log4J.getLogger(j2DClient.class);

    /**
	 * The man window frame.
	 */
    private JFrame frame;

    private GameScreen screen;

    private Canvas canvas;

    private JLayeredPane pane;

    private KTextEdit gameLog;

    private boolean gameRunning;

    /** NOTE: It sounds bad to see here a GUI component. Try other way. */
    private JTextField playerChatText;

    private boolean ctrlDown;

    private boolean shiftDown;

    private boolean altDown;

    /** the main frame */
    private WtBaseframe baseframe;

    /** this is the ground */
    private GroundContainer ground;

    /** settings panel */
    private SettingsPanel settings;

    private Component quitDialog;

    /**
	 * Delayed direction release holder.
	 */
    protected DelayedDirectionRelease directionRelease;

    public j2DClient(MidhedavaClient client) {
        super(client);
        setDefault(this);
        frame = new JFrame();
        frame.setTitle(ClientGameConfiguration.get("GAME_NAME") + " " + midhedava.VERSION + " - The Age of Legends");
        URL url = SpriteStore.get().getResourceURL(ClientGameConfiguration.get("GAME_ICON"));
        frame.setIconImage(new ImageIcon(url).getImage());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        Container content = frame.getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        pane = new JLayeredPane();
        pane.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        content.add(pane);
        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        pane.add(panel, JLayeredPane.DEFAULT_LAYER);
        canvas = new Canvas();
        canvas.setBounds(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        canvas.setIgnoreRepaint(true);
        panel.add(canvas);
        playerChatText = new JTextField("");
        MidhedavaChatLineListener chatListener = new MidhedavaChatLineListener(client, playerChatText);
        playerChatText.addActionListener(chatListener);
        playerChatText.addKeyListener(chatListener);
        content.add(playerChatText);
        canvas.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                playerChatText.requestFocus();
            }

            public void focusLost(FocusEvent e) {
            }
        });
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent ev) {
                playerChatText.requestFocus();
            }

            @Override
            public void windowActivated(WindowEvent ev) {
                playerChatText.requestFocus();
            }

            @Override
            public void windowGainedFocus(WindowEvent ev) {
                playerChatText.requestFocus();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                requestQuit();
            }
        });
        quitDialog = buildQuitDialog();
        quitDialog.setVisible(false);
        pane.add(quitDialog, JLayeredPane.MODAL_LAYER);
        gameLog = new KTextEdit();
        gameLog.setPreferredSize(new Dimension(SCREEN_WIDTH, 171));
        if (System.getProperty("midhedava.onewindow") != null) {
            content.add(gameLog);
            frame.pack();
        } else if (System.getProperty("midhedava.onewindowtitle") != null || System.getProperty("midhedava.refactoringguiui") != null) {
            JLabel header = new JLabel();
            header.setText("Game Chat and Events Log");
            header.setFont(new java.awt.Font("Dialog", 3, 14));
            content.add(header);
            content.add(gameLog);
            frame.pack();
        } else {
            final JDialog dialog = new JDialog(frame, "Game chat and events log");
            content = dialog.getContentPane();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(gameLog);
            dialog.addFocusListener(new FocusListener() {

                public void focusGained(FocusEvent e) {
                    playerChatText.requestFocus();
                }

                public void focusLost(FocusEvent e) {
                }
            });
            dialog.pack();
            frame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentShown(ComponentEvent e) {
                    Rectangle bounds = frame.getBounds();
                    dialog.setLocation(bounds.x, bounds.y + bounds.height);
                    dialog.setVisible(true);
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    Rectangle bounds = frame.getBounds();
                    dialog.setLocation(bounds.x, bounds.y + bounds.height);
                }
            });
        }
        KeyListener keyListener = new GameKeyHandler();
        playerChatText.addKeyListener(keyListener);
        canvas.addKeyListener(keyListener);
        if (!midhedava.SCREEN_SIZE.equals("640x480")) {
            addEventLine("Using window size cheat: " + midhedava.SCREEN_SIZE, Color.RED);
        }
        frame.setLocation(new Point(20, 20));
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
        BufferStrategy strategy;
        canvas.createBufferStrategy(2);
        strategy = canvas.getBufferStrategy();
        screen = new GameScreen(client, strategy, SCREEN_WIDTH, SCREEN_HEIGHT);
        screen.setComponent(canvas);
        GameScreen.setDefaultScreen(screen);
        client.setScreen(screen);
        baseframe = new WtBaseframe(screen);
        canvas.addMouseListener(baseframe);
        canvas.addMouseMotionListener(baseframe);
        ground = new GroundContainer(this);
        baseframe.addChild(ground);
        settings = new SettingsPanel(this, ground);
        ground.addChild(settings);
        WtWindowManager windowManager = WtWindowManager.getInstance();
        windowManager.setDefaultProperties("corpse", false, 0, 190);
        windowManager.setDefaultProperties("chest", false, 100, 190);
        directionRelease = null;
        gameLoop();
        chatListener.save();
        logger.debug("Exit");
        System.exit(0);
    }

    /**
	 * Add a native in-window dialog to the screen.
	 * 
	 * @param comp
	 *            The component to add.
	 */
    public void addDialog(Component comp) {
        pane.add(comp, JLayeredPane.PALETTE_LAYER);
    }

    /**
	 * Build the in-window quit dialog [panel].
	 * 
	 * 
	 */
    protected Component buildQuitDialog() {
        InternalManagedDialog imd;
        Style style;
        JPanel panel;
        JButton b;
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(150, 75));
        style = WoodStyle.getInstance();
        b = new StyledJButton(style);
        b.setText("Yes");
        b.setBounds(30, 25, 40, 25);
        b.addActionListener(new QuitConfirmCB());
        panel.add(b);
        b = new StyledJButton(style);
        b.setText("No");
        b.setBounds(80, 25, 40, 25);
        b.addActionListener(new QuitCancelCB());
        panel.add(b);
        imd = new InternalManagedDialog("quit", "Quit");
        imd.setContent(panel);
        imd.setMinimizable(false);
        imd.setMovable(false);
        return imd.getDialog();
    }

    public void gameLoop() {
        final int frameLength = (int) (1000.0 / midhedava.FPS_LIMIT);
        int fps = 0;
        GameObjects gameObjects = client.getGameObjects();
        long oldTime = System.nanoTime();
        screen.clear();
        SoundMaster.play("harp-1.wav");
        long refreshTime = System.currentTimeMillis();
        long lastMessageHandle = refreshTime;
        gameRunning = true;
        while (gameRunning) {
            fps++;
            screen.nextFrame();
            long now = System.currentTimeMillis();
            long delta = now - refreshTime;
            refreshTime = now;
            logger.debug("Move objects");
            gameObjects.move(delta);
            StaticGameLayers gameLayers = client.getStaticGameLayers();
            if (gameLayers.changedArea()) {
                CollisionDetection cd = gameLayers.getCollisionDetection();
                if (cd != null) {
                    gameLayers.resetChangedArea();
                    settings.updateMinimap(cd, screen.expose().getDeviceConfiguration(), gameLayers.getArea());
                }
            }
            settings.setPlayer(User.get());
            if (frame.getState() != Frame.ICONIFIED) {
                logger.debug("Draw screen");
                screen.draw(baseframe);
            }
            logger.debug("Query network");
            if (client.loop(0)) {
                lastMessageHandle = System.currentTimeMillis();
            }
            if ((directionRelease != null) && directionRelease.hasExpired()) {
                client.removeDirection(directionRelease.getDirection(), directionRelease.isFacing());
                directionRelease = null;
            }
            if (System.nanoTime() - oldTime > 1000000000) {
                oldTime = System.nanoTime();
                logger.debug("FPS: " + Integer.toString(fps));
                long freeMemory = Runtime.getRuntime().freeMemory() / 1024;
                long totalMemory = Runtime.getRuntime().totalMemory() / 1024;
                logger.debug("Total/Used memory: " + totalMemory + "/" + (totalMemory - freeMemory));
                fps = 0;
            }
            if ((refreshTime - lastMessageHandle > 120000) || !client.getConnectionState()) {
                setOffline(true);
            } else {
                setOffline(false);
            }
            logger.debug("Start sleeping");
            long wait = frameLength + refreshTime - System.currentTimeMillis();
            if (wait > 0) {
                if (wait > 100) {
                    logger.info("Waiting " + wait + " ms");
                    wait = 100;
                }
                try {
                    Thread.sleep(wait);
                } catch (Exception e) {
                }
                ;
            }
            logger.debug("End sleeping");
        }
        logger.info("Request logout");
        client.logout();
        SoundSystem.get().exit();
    }

    /**
	 * Convert a keycode to the corresponding direction.
	 * 
	 * @param keyCode
	 *            The keycode.
	 * 
	 * @return The direction, or <code>null</code>.
	 */
    protected Direction keyCodeToDirection(int keyCode) {
        switch(keyCode) {
            case KeyEvent.VK_LEFT:
                return Direction.LEFT;
            case KeyEvent.VK_RIGHT:
                return Direction.RIGHT;
            case KeyEvent.VK_UP:
                return Direction.UP;
            case KeyEvent.VK_DOWN:
                return Direction.DOWN;
            case KeyEvent.VK_PAGE_UP:
                return Direction.UP_RIGHT;
            case KeyEvent.VK_END:
                return Direction.DOWN_LEFT;
            case KeyEvent.VK_PAGE_DOWN:
                return Direction.DOWN_RIGHT;
            case KeyEvent.VK_HOME:
                return Direction.UP_LEFT;
            default:
                return null;
        }
    }

    protected void onKeyPressed(KeyEvent e) {
        if (e.isShiftDown()) {
            return;
        }
        switch(e.getKeyCode()) {
            case KeyEvent.VK_L:
                if (e.isControlDown()) {
                    SwingUtilities.getRoot(gameLog).setVisible(true);
                }
                break;
            case KeyEvent.VK_R:
                if (e.isControlDown()) {
                    screen.clearTexts();
                }
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_END:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_HOME:
                Direction direction = keyCodeToDirection(e.getKeyCode());
                if (e.isAltGraphDown()) {
                    User user = User.get();
                    EntityView view = screen.getEntityViewAt(user.getX() + direction.getdx(), user.getY() + direction.getdy() + 1);
                    if (view != null) {
                        Entity entity = view.getEntity();
                        if (!entity.equals(user)) {
                            entity.onAction(entity.defaultAction());
                        }
                    }
                }
                processDirectionPress(direction, e.isControlDown());
        }
    }

    protected void onKeyReleased(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_PAGE_UP:
            case KeyEvent.VK_END:
            case KeyEvent.VK_PAGE_DOWN:
            case KeyEvent.VK_HOME:
                processDirectionRelease(keyCodeToDirection(e.getKeyCode()), e.isControlDown());
        }
    }

    /**
	 * Handle direction press actions.
	 * 
	 * @param direction
	 *            The direction.
	 * @param facing
	 *            If facing only.
	 */
    protected void processDirectionPress(Direction direction, boolean facing) {
        if (directionRelease != null) {
            if (directionRelease.check(direction, facing)) {
                logger.debug("Repeat suppressed");
                directionRelease = null;
                return;
            } else {
                client.removeDirection(directionRelease.getDirection(), directionRelease.isFacing());
                directionRelease = null;
            }
        }
        client.addDirection(direction, facing);
    }

    /**
	 * Handle direction release actions.
	 * 
	 * @param direction
	 *            The direction.
	 * @param facing
	 *            If facing only.
	 */
    protected void processDirectionRelease(Direction direction, boolean facing) {
        if (directionRelease != null) {
            if (directionRelease.check(direction, facing)) {
                return;
            } else {
                client.removeDirection(directionRelease.getDirection(), directionRelease.isFacing());
            }
        }
        directionRelease = new DelayedDirectionRelease(direction, facing);
    }

    protected void quitCancelCB() {
        quitDialog.setVisible(false);
    }

    protected void quitConfirmCB() {
        shutdown();
    }

    /**
	 * Save the current keyboard modifier (i.e. Alt/Ctrl/Shift) state.
	 * 
	 * @param ev
	 *            The keyboard event.
	 */
    protected void updateModifiers(KeyEvent ev) {
        altDown = ev.isAltDown();
        ctrlDown = ev.isControlDown();
        shiftDown = ev.isShiftDown();
    }

    /**
	 * Shutdown the client. Save state and tell the main loop to stop.
	 */
    protected void shutdown() {
        Log4J.startMethod(logger, "shutdown");
        gameRunning = false;
        WtWindowManager.getInstance().save();
        Log4J.finishMethod(logger, "shutdown");
    }

    /**
	 * Add a new window.
	 * 
	 * @param mw
	 *            A managed window.
	 * 
	 * @throws IllegalArgumentException
	 *             If an unsupported ManagedWindow is given.
	 */
    @Override
    public void addWindow(ManagedWindow mw) {
        if (mw instanceof InternalManagedDialog) {
            addDialog(((InternalManagedDialog) mw).getDialog());
        } else if (mw instanceof WtPanel) {
            ground.addChild((WtPanel) mw);
        } else {
            throw new IllegalArgumentException("Unsupport ManagedWindow type: " + mw.getClass().getName());
        }
    }

    /**
	 * Determine if the Alt key is held down.
	 * 
	 * @return Returns <code>true</code> if down.
	 */
    @Override
    public boolean isAltDown() {
        return altDown;
    }

    /**
	 * Determine if the <Ctrl> key is held down.
	 * 
	 * @return Returns <code>true</code> if down.
	 */
    @Override
    public boolean isCtrlDown() {
        return ctrlDown;
    }

    /**
	 * Determine if the <Shift> key is held down.
	 * 
	 * @return Returns <code>true</code> if down.
	 */
    @Override
    public boolean isShiftDown() {
        return shiftDown;
    }

    /**
	 * Sets the context menu. It is closed automatically one the user clicks
	 * outside of it.
	 */
    @Override
    public void setContextMenu(JPopupMenu contextMenu) {
        baseframe.setContextMenu(contextMenu);
    }

    /**
	 * Add an event line.
	 * 
	 */
    @Override
    public void addEventLine(String text) {
        gameLog.addLine(text);
    }

    /**
	 * Add an event line.
	 * 
	 */
    @Override
    public void addEventLine(String header, String text) {
        gameLog.addLine(header, text);
    }

    /**
	 * Add an event line.
	 * 
	 */
    @Override
    public void addEventLine(String text, Color color) {
        gameLog.addLine(text, color);
    }

    /**
	 * Add an event line.
	 * 
	 */
    @Override
    public void addEventLine(String header, String text, Color color) {
        gameLog.addLine(header, text, color);
    }

    /**
	 * Initiate outfit selection by the user.
	 */
    @Override
    public void chooseOutfit() {
        int outfit;
        RPObject player = client.getPlayer();
        if (player.has("outfit_org")) {
            outfit = player.getInt("outfit_org");
        } else {
            outfit = player.getInt("outfit");
        }
        OutfitDialog dialog = new OutfitDialog(frame, "Set outfit", outfit);
        dialog.setVisible(true);
    }

    @Override
    public void ManageGuilds() {
        GuildManager gm = new GuildManager();
        gm.setVisible(true);
    }

    /**
	 * Get the current game screen height.
	 * 
	 * @return The height.
	 */
    @Override
    public int getHeight() {
        return SCREEN_HEIGHT;
    }

    /**
	 * Get the entity inspector.
	 * 
	 * @return The inspector.
	 */
    @Override
    public Inspector getInspector() {
        return ground;
    }

    /**
	 * Get the game screen.
	 * 
	 * @return The game screen.
	 */
    @Override
    public GameScreen getScreen() {
        return screen;
    }

    /**
	 * Get the current game screen width.
	 * 
	 * @return The width.
	 */
    @Override
    public int getWidth() {
        return SCREEN_WIDTH;
    }

    /**
	 * Request quit confirmation from the user. This stops all player actions
	 * and shows a dialog in which the player can confirm that they really wants
	 * to quit the program. If so it flags the client for termination.
	 */
    @Override
    public void requestQuit() {
        client.stop();
        Dimension psize = quitDialog.getPreferredSize();
        quitDialog.setBounds((getWidth() - psize.width) / 2, (getHeight() - psize.height) / 2, psize.width, psize.height);
        quitDialog.validate();
        quitDialog.setVisible(true);
    }

    /**
	 * Set the input chat line text.
	 * 
	 * @param text
	 *            The text.
	 */
    @Override
    public void setChatLine(String text) {
        playerChatText.setText(text);
    }

    /**
	 * Set the offline indication state.
	 * 
	 * @param offline
	 *            <code>true</code> if offline.
	 */
    @Override
    public void setOffline(boolean offline) {
        screen.setOffline(offline);
    }

    protected class GameKeyHandler implements KeyListener {

        public void keyPressed(KeyEvent e) {
            updateModifiers(e);
            onKeyPressed(e);
        }

        public void keyReleased(KeyEvent e) {
            updateModifiers(e);
            onKeyReleased(e);
        }

        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == 27) {
                requestQuit();
            }
        }
    }

    protected class QuitCancelCB implements ActionListener {

        public void actionPerformed(ActionEvent ev) {
            quitCancelCB();
        }
    }

    protected class QuitConfirmCB implements ActionListener {

        public void actionPerformed(ActionEvent ev) {
            quitConfirmCB();
        }
    }

    public static void main(String args[]) {
        if (args.length > 0) {
            int i = 0;
            String port = null;
            String username = null;
            String password = null;
            String host = null;
            while (i != args.length) {
                if (args[i].equals("-u")) {
                    username = args[i + 1];
                } else if (args[i].equals("-p")) {
                    password = args[i + 1];
                } else if (args[i].equals("-h")) {
                    host = args[i + 1];
                } else if (args[i].equals("-port")) {
                    port = args[i + 1];
                }
                i++;
            }
            if ((username != null) && (password != null) && (host != null) && (port != null)) {
                MidhedavaClient client = MidhedavaClient.get();
                try {
                    client.connect(host, Integer.parseInt(port), true);
                    client.login(username, password);
                    new j2DClient(client);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }
        }
        System.out.println("Midhedava j2DClient\n");
        System.out.println("  games.midhedava.j2DClient -u username -p pass -h host -c character\n");
        System.out.println("Required parameters");
        System.out.println("* -h\tHost that is running Marauroa server");
        System.out.println("* -port\tport of the Marauroa server (try 32160)");
        System.out.println("* -u\tUsername to log into Marauroa server");
        System.out.println("* -p\tPassword to log into Marauroa server");
    }

    protected static class DelayedDirectionRelease {

        /**
		 * The maximum delay between auto-repeat release-press
		 */
        protected static final long DELAY = 50L;

        protected long expiration;

        protected Direction dir;

        protected boolean facing;

        public DelayedDirectionRelease(Direction dir, boolean facing) {
            this.dir = dir;
            this.facing = facing;
            expiration = System.currentTimeMillis() + DELAY;
        }

        /**
		 * Get the direction.
		 * 
		 * @return The direction.
		 */
        public Direction getDirection() {
            return dir;
        }

        /**
		 * Determine if the delay point has been reached.
		 * 
		 * @return <code>true</code> if the delay time has been reached.
		 */
        public boolean hasExpired() {
            return System.currentTimeMillis() >= expiration;
        }

        /**
		 * Determine if the facing only option was used.
		 * 
		 * @return <code>true</code> if facing only.
		 */
        public boolean isFacing() {
            return facing;
        }

        /**
		 * Check if a new direction matches the existing one, and if so, reset
		 * the expiration point.
		 * 
		 * @param dir
		 *            The direction.
		 * @param facing
		 *            The facing flag.
		 * 
		 * @return <code>true</code> if this is a repeat.
		 */
        public boolean check(Direction dir, boolean facing) {
            if (!this.dir.equals(dir)) {
                return false;
            }
            if (this.facing != facing) {
                return false;
            }
            long now = System.currentTimeMillis();
            if (now >= expiration) {
                return false;
            }
            expiration = now + DELAY;
            return true;
        }
    }

    public SettingsPanel getSettingsPanel() {
        return settings;
    }
}

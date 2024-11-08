package hogs.common;

import hogs.graphics.GraphicsEngine;
import hogs.graphics.Terrain;
import hogs.gui.GuiFrame;
import hogs.net.NetException;
import hogs.net.client.NetEngine;
import hogs.net.client.NetHandler;
import hogs.physics.Physics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JOptionPane;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLCapabilities;
import net.java.games.jogl.GLDrawableFactory;

/**
 * The top-level class for Hogs. This class initializes the network client, the
 * graphics and physics engines, and the GUI. After initializing all of the
 * components of the application, the run method runs the game appropriately.
 * 
 * <br>
 * <br>
 * This game supports team mode. Team mode will allow a user to join a specific
 * team to fight against another team. When a player joins a game, he specifies
 * for which team he would like to play. If the server is in team mode (i.e.,
 * all the other craft are on teams), the user joins that team. When the user is
 * the first player to join a server, he has the option as to whether the game
 * will be in individual mode or team mode. If the first player does not join a
 * team, then all subsequent joining players will not join a team, even if the
 * specify that they would like to play in team mode on the command line.
 * 
 * @author Bill Pijewski (wpijewsk)
 * @see hogs.common.Controller#run
 */
public class Controller {

    private GameState m_gamestate;

    private Physics m_physics;

    private GuiFrame m_window;

    private java.awt.Point m_curpos;

    private java.awt.Point m_middlepos;

    private StringBuffer m_chatbuffer;

    private NetEngine m_netengine;

    private ConcurrentLinkedQueue<ReceivedMessage> m_received_messages;

    private NetHandler m_nethandler;

    private String m_user;

    private GraphicsEngine m_graphics;

    private GLCanvas m_drawingpanel;

    private boolean isChatPaused;

    private boolean isRunning;

    private boolean m_shouldexit;

    private ArrayList<String> m_fragmsgs;

    private HashMap<Integer, String> m_customizedtaunts;

    private long m_lastbullet;

    /**
     * -h specifies the host to use <br>
     * -t specifies which team to play for, if any<br>
     * 
     * <br>
     * Both parameters are optional. By default, a controller tries to connect
     * to a server on a local machine and does not play on a team
     * 
     * @param args
     *            The command-line arguments
     */
    public static void main(String[] args) {
        String hostname = "localhost";
        String team = HogsConstants.TEAM_NONE;
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-t")) team = args[i + 1].trim();
            if (args[i].equals("-h")) hostname = args[i + 1].trim();
        }
        Controller game;
        try {
            game = new Controller(hostname, team, true);
            game.run();
        } catch (InternalException e) {
            System.err.println("InternalException: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Controller(String m_hostname, String team, boolean m_shouldexit) throws InternalException {
        m_received_messages = new ConcurrentLinkedQueue<ReceivedMessage>();
        m_fragmsgs = new ArrayList<String>();
        m_customizedtaunts = new HashMap<Integer, String>();
        m_nethandler = new CachingNetHandler();
        m_drawingpanel = GLDrawableFactory.getFactory().createGLCanvas(new GLCapabilities());
        m_user = System.getProperty("user.name");
        m_chatbuffer = new StringBuffer();
        this.m_shouldexit = m_shouldexit;
        isChatPaused = false;
        isRunning = true;
        m_lastbullet = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(HogsConstants.FRAGMSGS_FILE));
            String str;
            while ((str = in.readLine()) != null) {
                m_fragmsgs.add(str);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String newFile = PathFinder.getCustsFile();
        boolean exists = (new File(newFile)).exists();
        Reader reader = null;
        if (exists) {
            try {
                reader = new FileReader(newFile);
            } catch (FileNotFoundException e3) {
                e3.printStackTrace();
            }
        } else {
            Object[] options = { "Yes, create a .hogsrc file", "No, use default taunts" };
            int n = JOptionPane.showOptionDialog(m_window, "You do not have customized taunts in your home\n " + "directory.  Would you like to create a customizable file?", "Hogs Customization", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (n == 0) {
                try {
                    FileChannel srcChannel = new FileInputStream(HogsConstants.CUSTS_TEMPLATE).getChannel();
                    FileChannel dstChannel;
                    dstChannel = new FileOutputStream(newFile).getChannel();
                    dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                    srcChannel.close();
                    dstChannel.close();
                    reader = new FileReader(newFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    reader = new FileReader(HogsConstants.CUSTS_TEMPLATE);
                } catch (FileNotFoundException e3) {
                    e3.printStackTrace();
                }
            }
        }
        try {
            m_netengine = NetEngine.forHost(m_user, m_hostname, 1820, m_nethandler);
            m_netengine.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (NetException e) {
            e.printStackTrace();
        }
        m_gamestate = m_netengine.getCurrentState();
        m_gamestate.setInChatMode(false);
        m_gamestate.setController(this);
        try {
            readFromFile(reader);
        } catch (NumberFormatException e3) {
            e3.printStackTrace();
        } catch (IOException e3) {
            e3.printStackTrace();
        } catch (InternalException e3) {
            e3.printStackTrace();
        }
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice m_graphicsdevice = ge.getDefaultScreenDevice();
        m_window = new GuiFrame(m_drawingpanel, m_gamestate);
        m_graphics = null;
        try {
            m_graphics = new GraphicsEngine(m_drawingpanel, m_gamestate);
        } catch (InternalException e1) {
            e1.printStackTrace();
            System.exit(0);
        }
        m_drawingpanel.addGLEventListener(m_graphics);
        m_physics = new Physics();
        if (team == null) {
            team = HogsConstants.TEAM_NONE;
        }
        if (!(team.toLowerCase().equals(HogsConstants.TEAM_NONE) || team.toLowerCase().equals(HogsConstants.TEAM_RED) || team.toLowerCase().equals(HogsConstants.TEAM_BLUE))) {
            throw new InternalException("Invalid team name!");
        }
        String orig_team = team;
        Craft local_craft = m_gamestate.getLocalCraft();
        if (m_gamestate.getNumCrafts() == 0) {
            local_craft.setTeamname(team);
        } else if (m_gamestate.isInTeamMode()) {
            if (team == HogsConstants.TEAM_NONE) {
                int red_craft = m_gamestate.getNumOnTeam(HogsConstants.TEAM_RED);
                int blue_craft = m_gamestate.getNumOnTeam(HogsConstants.TEAM_BLUE);
                String new_team;
                if (red_craft > blue_craft) {
                    new_team = HogsConstants.TEAM_BLUE;
                } else if (red_craft < blue_craft) {
                    new_team = HogsConstants.TEAM_RED;
                } else {
                    new_team = Math.random() > 0.5 ? HogsConstants.TEAM_BLUE : HogsConstants.TEAM_RED;
                }
                m_gamestate.getLocalCraft().setTeamname(new_team);
            } else {
                local_craft.setTeamname(team);
            }
        } else {
            local_craft.setTeamname(HogsConstants.TEAM_NONE);
            if (orig_team != null) {
                m_window.displayText("You cannot join a team, this is an individual game.");
            }
        }
        if (!local_craft.getTeamname().equals(HogsConstants.TEAM_NONE)) {
            m_window.displayText("You are joining the " + local_craft.getTeamname() + " team.");
        }
        m_drawingpanel.setSize(m_drawingpanel.getWidth(), m_drawingpanel.getHeight());
        m_middlepos = new java.awt.Point(m_drawingpanel.getWidth() / 2, m_drawingpanel.getHeight() / 2);
        m_curpos = new java.awt.Point(m_drawingpanel.getWidth() / 2, m_drawingpanel.getHeight() / 2);
        GuiKeyListener k_listener = new GuiKeyListener();
        GuiMouseListener m_listener = new GuiMouseListener();
        m_window.addKeyListener(k_listener);
        m_drawingpanel.addKeyListener(k_listener);
        m_window.addMouseListener(m_listener);
        m_drawingpanel.addMouseListener(m_listener);
        m_window.addMouseMotionListener(m_listener);
        m_drawingpanel.addMouseMotionListener(m_listener);
        m_drawingpanel.addFocusListener(new FocusAdapter() {

            public void focusLost(FocusEvent evt) {
                m_window.setMouseTrapped(false);
                m_window.returnMouseToCenter();
            }
        });
        m_window.addFocusListener(new FocusAdapter() {

            public void focusLost(FocusEvent evt) {
                m_window.setMouseTrapped(false);
                m_window.returnMouseToCenter();
            }
        });
        m_window.requestFocus();
    }

    private void readFromFile(Reader reader) throws NumberFormatException, IOException, InternalException {
        String str;
        BufferedReader br = new BufferedReader(reader);
        while ((str = br.readLine()) != null) {
            if (str.equals("Taunts")) {
                for (int i = 0; i < 9; i++) {
                    str = br.readLine();
                    String[] tokens = str.split(":");
                    if (tokens.length != 2) {
                        throw new InternalException("Malformed taunt");
                    }
                    int num = Integer.parseInt(tokens[0].trim());
                    m_customizedtaunts.put(num, tokens[1].trim());
                }
            } else if (str.equals("Controls")) {
                str = br.readLine();
                String[] tokens = str.split(":");
                if (tokens.length != 2) {
                    throw new InternalException("Malformed mouse control");
                }
                m_gamestate.setInverted(Boolean.parseBoolean(tokens[1].trim()));
            } else if (str.equals("Colors")) {
                str = br.readLine();
                String[] tokens = str.split(" ");
                m_gamestate.getLocalCraft().setColor(new hogs.graphics.Color((float) Integer.parseInt(tokens[0]) / (float) 255.0f, (float) Integer.parseInt(tokens[1]) / (float) 255.0f, (float) Integer.parseInt(tokens[2]) / (float) 255.0f));
            } else {
                throw new InternalException("Malformed customization file");
            }
        }
    }

    /**
     * The main loop of the game. Does the following things:
     * 
     * <br>
     * 1. Processes network messages <br>
     * 2. Updates the physics model <br>
     * 3. Updates and displays the drawing window <br>
     * 4. Sleeps to keep graphics smooth
     */
    public void run() {
        m_window.displayText("Press SHIFT to see a list of controls.");
        m_gamestate.keyDown(HogsKey.K_SHOW_STATS);
        int numberOfMillisecondsInTheFuture = 3000;
        Date timeToRun = new Date(System.currentTimeMillis() + numberOfMillisecondsInTheFuture);
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                m_gamestate.keyUp(HogsKey.K_SHOW_STATS);
            }
        }, timeToRun);
        while (isRunning) {
            for (ReceivedMessage m : m_received_messages) {
                m.handle();
            }
            m_received_messages.clear();
            m_gamestate.setMousePositionChange(getMouseChange());
            m_gamestate.setChatBuffer(m_chatbuffer);
            m_physics.update(m_gamestate);
            m_window.update();
            try {
                if (m_netengine != null) {
                    m_netengine.sendUpdatedPosition();
                }
            } catch (NetException e1) {
                System.err.println("NetException: " + e1.getMessage());
                e1.printStackTrace();
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        m_window.dispose();
        m_netengine.shutdown("Leaving game");
        if (m_shouldexit) {
            System.exit(0);
        }
    }

    public void die() {
        isRunning = false;
    }

    /**
     * @author wpijewsk
     * 
     * A KeyListener that should only print once per key press (i.e. should not
     * repeat).
     * 
     * Be sure to run `xset r off` in a shell before running this program. You
     * can run `xset r on` to turn repeating back on.
     * 
     * It turns out that for most uses, you don't need to turn xset off.
     */
    private class GuiKeyListener extends KeyAdapter {

        public void keyPressed(KeyEvent e) {
            if (!m_gamestate.isInChatMode()) {
                HogsKey key = HogsKey.getKey(e.getKeyCode());
                if (HogsKey.isTauntKey(key)) {
                    int index = HogsKey.getTauntIndex(key);
                    try {
                        m_netengine.sendChatMessage(m_customizedtaunts.get(index));
                    } catch (NetException e2) {
                        e2.printStackTrace();
                    }
                    m_window.displayChatText(m_user, m_customizedtaunts.get(index));
                } else {
                    switch(key) {
                        case K_FIRE:
                            long current = System.currentTimeMillis();
                            if (m_lastbullet + HogsConstants.BULLET_DELAY < current) {
                                m_lastbullet = current;
                                Bullet newbullet = m_physics.fireLocalBullet(m_gamestate, false);
                                m_window.setMouseTrapped(true);
                                try {
                                    m_netengine.sendCreateBulletMessage(newbullet);
                                } catch (NetException e2) {
                                    e2.printStackTrace();
                                }
                            }
                            break;
                        case K_RELEASE_MOUSE:
                            m_window.setMouseTrapped(false);
                            break;
                        case K_SHOW_FRAME_RATE:
                            m_gamestate.setShowingFrameRate(!m_gamestate.isShowingFrameRate());
                            break;
                        case K_QUIT:
                            if (m_gamestate.isKeyDown(HogsKey.K_CTRL)) {
                                die();
                            }
                            break;
                        case K_OTHER:
                        case K_CHAT:
                            break;
                        default:
                            m_gamestate.keyDown(key);
                    }
                }
            }
        }

        public void keyReleased(KeyEvent e) {
            HogsKey key = HogsKey.getKey(e.getKeyCode());
            if (m_gamestate.isInChatMode()) {
                if (e.getKeyChar() == '\n') {
                    m_gamestate.setInChatMode(false);
                    try {
                        Process p = Runtime.getRuntime().exec("xset r off");
                        p.waitFor();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    } catch (InterruptedException e3) {
                        e3.printStackTrace();
                    }
                    try {
                        m_netengine.sendChatMessage(m_chatbuffer.toString());
                        m_window.displayChatText(m_gamestate.getLocalCraft().getHandle(), m_chatbuffer.toString());
                    } catch (NetException e1) {
                        e1.printStackTrace();
                    }
                    m_chatbuffer.delete(0, m_chatbuffer.length());
                } else {
                    if (key == HogsKey.K_PAUSE_CHAT) {
                        m_window.pauseChat();
                        m_gamestate.setInChatMode(false);
                    } else if (e.getKeyCode() == 8) {
                        if (m_chatbuffer.length() > 0) {
                            m_chatbuffer.deleteCharAt(m_chatbuffer.length() - 1);
                        }
                    } else {
                        m_chatbuffer.append(e.getKeyChar());
                    }
                }
            } else {
                switch(key) {
                    case K_CHAT:
                        m_gamestate.setInChatMode(true);
                        m_gamestate.clearKeyHash();
                        try {
                            Process p = Runtime.getRuntime().exec("xset r on");
                            p.waitFor();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    case K_FIRE:
                    case K_OTHER:
                        break;
                    default:
                        m_gamestate.keyUp(key);
                }
            }
        }
    }

    /**
     * Mouse listener
     */
    private class GuiMouseListener extends MouseAdapter implements MouseMotionListener {

        public void mouseDragged(MouseEvent e) {
            if (m_window.getMouseTrapped()) {
                updateMousePosition(e);
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (m_window.getMouseTrapped()) {
                updateMousePosition(e);
            }
        }

        public void mouseClicked(MouseEvent e) {
            long current = System.currentTimeMillis();
            if (m_lastbullet + HogsConstants.BULLET_DELAY < current) {
                m_lastbullet = current;
                Bullet newbullet = m_physics.fireLocalBullet(m_gamestate, !(e.getButton() == MouseEvent.BUTTON1));
                m_window.setMouseTrapped(true);
                try {
                    m_netengine.sendCreateBulletMessage(newbullet);
                } catch (NetException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * @author Bill Pijewski (wpijewsk)
     * 
     * Extends the NetHandler class, and overrides methods in that class to deal
     * with callbacks from the NetworkEngine
     */
    private class CachingNetHandler extends NetHandler {

        /**
         * @see NetHandler.handleKillPlayer
         */
        public void handleKillPlayer(int killedId, int objectId) {
            m_received_messages.add(new KillPlayerMessage(killedId, objectId));
        }

        /**
         * @see NetHandler.handleCreateBullet
         */
        public void handleCreateBullet(Bullet bullet) {
            m_received_messages.add(new CreateBulletMessage(bullet));
        }

        /**
         * @see NetHandler.handleChatText
         */
        public void handleChatText(int id, String string) {
            m_received_messages.add(new ChatMessage(id, string));
        }

        /**
         * @see NetHandler.handleNewPlayer
         */
        public void handleNewPlayer(Craft craft) {
            m_received_messages.add(new NewPlayerMessage(craft));
        }

        /**
         * @see NetHandler.handleDropPlayer
         */
        public void handleDropPlayer(int id, String reason) {
            m_received_messages.add(new DropPlayerMessage(m_gamestate.getCraft(id).getHandle(), reason));
        }

        /**
         * @see NetHandler.handleServerShutdown
         */
        public void handleServerShutdown(String reason) {
            m_received_messages.add(new ServerShutdownMessage(reason));
        }

        /**
         * @see NetHandler.handleRemoveBullet
         */
        public void handleRemoveBullet(int id) {
            m_received_messages.add(new RemoveBulletMessage(id));
        }

        /**
         * @see NetHandler.handleFatalError
         */
        public void handleFatalError(Throwable exn) {
            m_received_messages.add(new FatalErrorMessage(exn));
        }

        /**
         * @see NetHandler.handleMapChange
         */
        public void handleMapChange(Terrain m_terrain) {
            m_received_messages.add(new MapChangeMessage(m_terrain));
        }
    }

    /**
     * @author Bill Pijewski (wpijewsk)
     * 
     * A ReceivedMessage contains all of the relevant information about messages
     * received from the game server. Because the messages are made into message
     * objects, the Controller can process them all at once.
     */
    private interface ReceivedMessage {

        /**
         * Handles a message received from the game server
         */
        public void handle();
    }

    private class KillPlayerMessage implements ReceivedMessage {

        private int m_killedId;

        private int m_objectId;

        public KillPlayerMessage(int m_killedId, int m_objectId) {
            this.m_killedId = m_killedId;
            this.m_objectId = m_objectId;
        }

        public void handle() {
            String loser = m_gamestate.getCraft(m_killedId).getHandle();
            String winner = m_gamestate.getCraft(m_objectId).getHandle();
            m_gamestate.getCraft(m_killedId).incrementDeaths();
            if (m_gamestate.isInTeamMode() && m_gamestate.getCraft(m_killedId).getTeamname().equals(m_gamestate.getCraft(m_objectId).getTeamname())) {
                m_gamestate.getCraft(m_objectId).decrementFrags();
            } else {
                m_gamestate.getCraft(m_objectId).incrementFrags();
            }
            int randInt = (int) (m_fragmsgs.size() * Math.random());
            String frag_msg = new String(m_fragmsgs.get(randInt));
            frag_msg = frag_msg.replace("1", winner);
            frag_msg = frag_msg.replace("2", loser);
            m_window.displayText(frag_msg);
        }
    }

    private class CreateBulletMessage implements ReceivedMessage {

        private Bullet m_bullet;

        public CreateBulletMessage(Bullet m_bullet) {
            this.m_bullet = m_bullet;
        }

        public void handle() {
            m_gamestate.addBullet(m_bullet);
        }
    }

    private class ChatMessage implements ReceivedMessage {

        private int m_playerId;

        private String m_chatText;

        public ChatMessage(int m_playerId, String m_chatText) {
            this.m_playerId = m_playerId;
            this.m_chatText = m_chatText;
        }

        public void handle() {
            if (m_playerId == -1) {
                m_window.displayText(m_chatText);
            } else {
                String craft_name = m_gamestate.getCrafts()[m_playerId].getHandle();
                m_window.displayChatText(craft_name, m_chatText);
            }
        }
    }

    private class NewPlayerMessage implements ReceivedMessage {

        private Craft m_craft;

        public NewPlayerMessage(Craft m_craft) {
            this.m_craft = m_craft;
        }

        public void handle() {
            String craftname = m_craft.getHandle();
            m_window.displayText(craftname + " has joined the game");
        }
    }

    private class DropPlayerMessage implements ReceivedMessage {

        private String m_craftname;

        private String m_reason;

        public DropPlayerMessage(String m_craftname, String m_reason) {
            this.m_craftname = m_craftname;
            this.m_reason = m_reason;
        }

        public void handle() {
            m_window.displayText(m_craftname + " has dropped from the game");
        }
    }

    private class ServerShutdownMessage implements ReceivedMessage {

        private String m_reason;

        public ServerShutdownMessage(String m_reason) {
            this.m_reason = m_reason;
        }

        public void handle() {
            m_window.displayText("The server is shutting down.  Game Over.");
            int numberOfMillisecondsInTheFuture = 2000;
            Date timeToRun = new Date(System.currentTimeMillis() + numberOfMillisecondsInTheFuture);
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {

                public void run() {
                    die();
                }
            }, timeToRun);
        }
    }

    private class FatalErrorMessage implements ReceivedMessage {

        private Throwable m_reason;

        public FatalErrorMessage(Throwable m_reason) {
            this.m_reason = m_reason;
        }

        public void handle() {
            m_window.displayText("There was a fatal error: " + m_reason.getLocalizedMessage() + ".  Game Over.");
            int numberOfMillisecondsInTheFuture = 2000;
            Date timeToRun = new Date(System.currentTimeMillis() + numberOfMillisecondsInTheFuture);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {

                public void run() {
                    die();
                }
            }, timeToRun);
        }
    }

    private class RemoveBulletMessage implements ReceivedMessage {

        private int m_id;

        public RemoveBulletMessage(int m_id) {
            this.m_id = m_id;
        }

        public void handle() {
            m_gamestate.bulletExploded(m_id);
        }
    }

    private class MapChangeMessage implements ReceivedMessage {

        private Terrain m_terrain;

        public MapChangeMessage(Terrain m_terrain) {
            this.m_terrain = m_terrain;
        }

        public void handle() {
            m_window.displayText("Changing the map.  One moment...");
            m_gamestate.setTerrain(m_terrain);
            m_gamestate.getLocalCraft().randomizePosition(m_terrain);
            m_graphics.newMap();
        }
    }

    /**
     * @param e
     *            The most recent mouse position
     */
    void updateMousePosition(MouseEvent e) {
        m_curpos.setLocation(e.getX(), e.getY());
    }

    /**
     * @return A MousePositionChange, a class that
     */
    public MousePositionChange getMouseChange() {
        int window_width = m_drawingpanel.getWidth();
        int window_height = m_drawingpanel.getHeight();
        if (m_curpos.getX() > 0) {
            double delta_x = ((double) m_curpos.getX() - (double) m_middlepos.getX()) / (double) window_width;
            double delta_y = -((double) m_curpos.getY() - (double) m_middlepos.getY()) / (double) window_height;
            return new MousePositionChange(delta_x, delta_y);
        } else {
            return new MousePositionChange(0.0f, 0.0f);
        }
    }

    /**
     * @author Bill Pijewski (wpijewsk)
     * 
     * Describes the relative movement of the mouse. In the X dimension, -1.0 is
     * the entire screen's width left, and 1.0 is the entire screen's width
     * right. Likewise in the Y dimension, 1.0 is all the way up and -1.0 is all
     * the way down.
     */
    public class MousePositionChange {

        private final double xChange;

        private final double yChange;

        public MousePositionChange(double xChange, double yChange) {
            this.xChange = xChange;
            this.yChange = yChange;
        }

        public double getXChange() {
            return xChange;
        }

        public double getYChange() {
            return yChange;
        }

        public String toString() {
            return "Delta X: " + xChange + "\tDelta Y: " + yChange;
        }
    }

    public NetEngine getNetEngine() {
        return m_netengine;
    }

    public GuiFrame getWindow() {
        return m_window;
    }
}

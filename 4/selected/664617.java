package sourceforge.shinigami.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import sourceforge.shinigami.graphics.Texture;
import sourceforge.shinigami.gui.GameScreen;
import sourceforge.shinigami.io.MapFile;
import sourceforge.shinigami.io.SIOException;
import sourceforge.shinigami.maps.Map;
import sourceforge.shinigami.maps.Direction;
import sourceforge.shinigami.maps.MapScope;
import sourceforge.shinigami.maps.MapScope.PortalScope;
import sourceforge.shinigami.music.Sound;

/**
 * <b>
 * DO NOT COPY CODE FROM THIS CLASS					<br />
 * IT'S CODE IS HASTY, CONFUSE AND INCORRECT		<br />
 * </b>												<br />
 * 													<br />
 * SHORTCUT LIST:									<br />
 * 													<br />
 * p = Enters/leaves portal creation mode			<br />
 * r = Resets the tile entrance	settings			<br />
 * 													<br />
 * arrow keys = move the camera						<br />
 * shift + arrow keys = move the background image	<br />
 */
@Deprecated
public class MapGenerator {

    private static final String VERSION = "0.0.4";

    static OptionScreen menu;

    static ViewScreen mapW;

    static int tileWidth = Map.DEFAULT_TILE_WIDTH;

    static int tileHeight = Map.DEFAULT_TILE_HEIGHT;

    public static void main(String[] args) {
        mapW = new ViewScreen();
        menu = new OptionScreen();
    }

    private static class ViewScreen extends GameScreen implements KeyListener, MouseListener, MouseMotionListener {

        public ViewScreen() {
            super(640, 400, false);
        }

        {
            this.addKeyListener(this);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
            this.setRenderFramesPerSecond(10);
            this.setUpdateFramesPerSecond(20);
            this.start();
        }

        String mapName = "";

        Texture background = null;

        File bgfile = null;

        File bgmfile = null;

        int width = 0;

        int height = 0;

        int x = 0;

        int y = 0;

        int goingX = 0;

        int goingY = 0;

        int imX = 0;

        int imY = 0;

        int imGoingX = 0;

        int imGoingY = 0;

        Node[][][] nodes = null;

        LinkedList<GenPortal> portals = new LinkedList<GenPortal>();

        @Override
        protected void update() {
            if (width != 0 && height != 0) {
                x += goingX;
                y += goingY;
                imX += imGoingX;
                imY += imGoingY;
            }
        }

        @Override
        protected void render(Graphics2D g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 640, 400);
            if (background != null) background.render(g, x + imX, y + imY);
            g.setColor(Color.CYAN);
            for (int i = 0; i < width; i++) g.drawLine(x + i * tileWidth, y, x + i * tileWidth, y + height * tileHeight);
            for (int i = 0; i < height; i++) g.drawLine(x, y + i * tileHeight, x + width * tileWidth, y + i * tileHeight);
            g.drawRect(x, y, width * tileWidth, height * tileHeight);
            if (nodes != null) for (int ix = 0; ix < nodes.length; ix++) for (int iy = 0; iy < nodes[0].length; iy++) for (int id = 0; id < nodes[0][0].length; id++) nodes[ix][iy][id].render(g, x, y);
            for (GenPortal p : portals) p.render(g, x, y, settingPortal);
            if (this.settingPortal) {
                g.setColor(Color.GRAY);
                g.fillOval(this.mouseXNow - 5, this.mouseYNow - 5, 10, 10);
            }
            g.setColor(Color.GRAY);
            int ax1 = (pressX1 < pressX2) ? pressX1 : pressX2;
            int ax2 = (pressX1 >= pressX2) ? pressX1 : pressX2;
            int ay1 = (pressY1 < pressY2) ? pressY1 : pressY2;
            int ay2 = (pressY1 >= pressY2) ? pressY1 : pressY2;
            g.drawRect(ax1, ay1, ax2 - ax1, ay2 - ay1);
            if (this.shiftPressed && (nodes != null)) {
                g.setColor(Color.RED);
                g.drawString("Image: " + imX + "x" + imY, 10, 20);
            }
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, 640 - 1, 400 - 1);
        }

        boolean shiftPressed = false;

        int mouseXNow = 0;

        int mouseYNow = 0;

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftPressed = true; else if (e.getKeyCode() == KeyEvent.VK_UP) if (e.isShiftDown()) imGoingY = -1; else goingY = +3; else if (e.getKeyCode() == KeyEvent.VK_DOWN) if (e.isShiftDown()) imGoingY = +1; else goingY = -3; else if (e.getKeyCode() == KeyEvent.VK_LEFT) if (e.isShiftDown()) imGoingX = -1; else goingX = +3; else if (e.getKeyCode() == KeyEvent.VK_RIGHT) if (e.isShiftDown()) imGoingX = +1; else goingX = -3; else if (e.getKeyCode() == KeyEvent.VK_P) this.settingPortal = !this.settingPortal; else if (e.getKeyCode() == KeyEvent.VK_R) {
                if (nodes != null) {
                    for (int ix = 0; ix < nodes.length; ix++) for (int iy = 0; iy < nodes[0].length; iy++) for (int id = 0; id < nodes[0][0].length; id++) nodes[ix][iy][id].accessible = true;
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) shiftPressed = false;
            if ((e.getKeyCode() == KeyEvent.VK_UP) || (e.getKeyCode() == KeyEvent.VK_DOWN)) if (e.isShiftDown()) imGoingY = 0; else goingY = 0; else if ((e.getKeyCode() == KeyEvent.VK_LEFT) || (e.getKeyCode() == KeyEvent.VK_RIGHT)) if (e.isShiftDown()) imGoingX = 0; else goingX = 0;
        }

        boolean settingPortal = false;

        @Override
        public void mouseClicked(MouseEvent e) {
            if (nodes != null) {
                if (settingPortal) {
                    int tx = (e.getX() - x) / tileWidth;
                    int ty = (e.getY() - y) / tileHeight;
                    for (GenPortal p : portals) if ((p.TX == tx) && (p.TY == ty)) {
                        portals.remove(p);
                        return;
                    }
                    try {
                        String target = JOptionPane.showInputDialog("Target Map name: ");
                        int ttx = Integer.parseInt(JOptionPane.showInputDialog("X Position: "));
                        int tty = Integer.parseInt(JOptionPane.showInputDialog("Y Position: "));
                        portals.add(new GenPortal(tx, ty, target, ttx, tty));
                    } catch (NumberFormatException nfe) {
                        return;
                    }
                } else {
                    for (int ix = 0; ix < nodes.length; ix++) for (int iy = 0; iy < nodes[0].length; iy++) for (int id = 0; id < nodes[0][0].length; id++) nodes[ix][iy][id].onClick(e.getX() - x, e.getY() - y);
                }
            }
        }

        int pressX1 = 0, pressY1 = 0, pressX2 = 0, pressY2 = 0;

        @Override
        public void mousePressed(MouseEvent e) {
            this.pressX1 = e.getX();
            this.pressX2 = e.getX();
            this.pressY1 = e.getY();
            this.pressY2 = e.getY();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            this.pressX2 = e.getX();
            this.pressY2 = e.getY();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (pressX2 < pressX1) {
                int a = pressX2;
                pressX2 = pressX1;
                pressX1 = a;
            }
            if (pressY2 < pressY1) {
                int a = pressY2;
                pressY2 = pressY1;
                pressY1 = a;
            }
            if (nodes != null) {
                for (int ix = 0; ix < nodes.length; ix++) for (int iy = 0; iy < nodes[0].length; iy++) for (int id = 0; id < nodes[0][0].length; id++) nodes[ix][iy][id].onArea(pressX1 - x, pressY1 - y, pressX2 - x, pressY2 - y);
            }
            this.pressX1 = 0;
            this.pressX2 = 0;
            this.pressY1 = 0;
            this.pressY2 = 0;
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseXNow = e.getX();
            mouseYNow = e.getY();
        }

        private synchronized void updateState(String name, File bgfile, File bgm, int width, int height, int tw, int th) {
            if (mapName.contains(".") || mapName.contains("\\") || mapName.contains("/") || mapName.contains("#")) throw new RuntimeException("Map Name is Screwed Up");
            this.mapName = name;
            this.bgfile = bgfile;
            this.bgmfile = bgm;
            this.background = new Texture(bgfile);
            this.width = width;
            this.height = height;
            MapGenerator.tileWidth = tw;
            MapGenerator.tileHeight = th;
            if (width * tileWidth > 640) this.x = 0; else this.x = (320 - (width * tileWidth / 2));
            if (height * tileHeight > 400) this.y = 0; else this.y = (200 - (height * tileHeight / 2));
            this.nodes = new Node[width][height][4];
            for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) {
                this.nodes[x][y][Direction.NORTH.getID()] = new Node((tileWidth * x) + (tileWidth / 2), (tileHeight * y));
                this.nodes[x][y][Direction.WEST.getID()] = new Node((tileWidth * x), (tileHeight * y) + (tileHeight / 2));
                this.nodes[x][y][Direction.SOUTH.getID()] = new Node((tileWidth * x) + (tileWidth / 2), (tileHeight * (y + 1)));
                this.nodes[x][y][Direction.EAST.getID()] = new Node((tileWidth * (x + 1)), (tileHeight * y) + (tileHeight / 2));
            }
            portals = new LinkedList<GenPortal>();
        }

        private boolean createFile(File f, File image) {
            try {
                MapScope scope = new MapScope();
                scope.setMapName(mapName);
                scope.setSize(width, height);
                scope.setTileSize(tileWidth, tileHeight);
                scope.setBackgroundFile(this.bgfile);
                scope.setBackgroundAdjust(imX, imY);
                scope.setBGMFile(this.bgmfile);
                for (int i = 0; i < this.nodes.length; i++) for (int k = 0; k < this.nodes[0].length; k++) for (int m = 0; m < this.nodes[0][0].length; m++) scope.setPassage(i, k, m, nodes[i][k][m].accessible);
                int alt = 0;
                for (GenPortal p : portals) scope.addPortal(new PortalScope("p" + (alt++), p.TARGET, p.TX, p.TY, p.TARGET_X, p.TARGET_Y));
                scope.createFile(new MapFile(f.getPath()));
                return true;
            } catch (SIOException e) {
                return false;
            }
        }
    }

    @SuppressWarnings("serial")
    private static class OptionScreen extends FlowFrame implements ActionListener {

        protected OptionScreen() {
            super(415, 215);
        }

        JLabel l_mapName = new JLabel("Map name: ");

        JTextField tf_mapName = new JTextField();

        JLabel l_size = new JLabel("Map tiles: ");

        JTextField tf_width = new JTextField();

        JTextField tf_height = new JTextField();

        JLabel l_tipSize = new JLabel("<html><i>Amount of tiles on the map</i></html>");

        JLabel l_tileSize = new JLabel("Tile size: ");

        JTextField tf_tw = new JTextField("" + Map.DEFAULT_TILE_WIDTH);

        JTextField tf_th = new JTextField("" + Map.DEFAULT_TILE_HEIGHT);

        JLabel l_tipTileSize = new JLabel("<html><i>Pixel dimension of a tile</i></html>");

        JButton btn_update = new JButton("Update Info");

        JLabel l_imagePath = new JLabel("Image: ");

        JTextField tf_imagePath = new JTextField();

        JButton btn_findImage = new JButton("Find");

        JLabel l_bgmPath = new JLabel("BGM: ");

        JTextField tf_bgmPath = new JTextField();

        JButton btn_findBGM = new JButton("Find");

        JButton btn_play = new JButton("Play");

        JLabel l_saveFile = new JLabel("Save to: ");

        JTextField tf_savePath = new JTextField();

        JButton btn_findSave = new JButton("Find");

        JButton btn_generate = new JButton("Create File");

        JMenuBar menubar = new JMenuBar();

        JMenu menu_file = new JMenu("File");

        JMenu menu_help = new JMenu("Help");

        JMenuItem mi_open = new JMenuItem("Open Map");

        JMenuItem mi_about = new JMenuItem("About ShMG");

        private static final Color DISABLED_COLOR = new Color(1.0f, 0.7f, 0.6f);

        {
            this.setTitle("Map Generator for Shinigami Engine " + VERSION);
            this.setLocationRelativeTo(null);
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setMinimumPanelSize(352, 215);
            this.setJMenuBar(menubar);
            menubar.add(menu_file);
            menubar.add(menu_help);
            menu_file.add(mi_open);
            menu_help.add(mi_about);
            mi_open.addActionListener(this);
            mi_about.addActionListener(this);
            btn_update.addActionListener(this);
            btn_findImage.addActionListener(this);
            btn_findSave.addActionListener(this);
            btn_generate.addActionListener(this);
            btn_findBGM.addActionListener(this);
            btn_play.addActionListener(this);
            this.setVisible(true);
        }

        @Override
        protected void arrangeComponents(FlowManager m) {
            m.add(l_mapName, 70);
            m.add(tf_mapName, 325 + this.getWidthDifference());
            m.nextLine();
            m.add(l_imagePath, 70);
            m.add(tf_imagePath, 260 + this.getWidthDifference());
            m.add(btn_findImage, 60);
            m.nextLine();
            m.add(this.l_bgmPath, 70);
            m.add(this.tf_bgmPath, 196 + this.getWidthDifference());
            m.add(this.btn_play, 60);
            m.add(this.btn_findBGM, 60);
            m.nextLine();
            m.add(l_size, 70);
            m.add(tf_width, 50);
            m.add(tf_height, 50);
            m.add(l_tipSize, 200);
            m.nextLine();
            m.add(l_tileSize, 70);
            m.add(tf_tw, 50);
            m.add(tf_th, 50);
            m.add(l_tipTileSize, 200);
            m.nextLine();
            m.addY(12);
            m.add(l_saveFile, 70);
            m.add(tf_savePath, 260 + this.getWidthDifference());
            m.add(btn_findSave, 60);
            m.nextLine();
            m.addY(14);
            m.setAlignment(FlowManager.Alignment.CENTER);
            m.add(btn_update, 100);
            m.add(btn_generate, 100);
        }

        Color standardBtnBaground = this.btn_update.getBackground();

        @Override
        public void actionPerformed(ActionEvent e) {
            this.tf_height.setBackground(Color.WHITE);
            this.tf_width.setBackground(Color.WHITE);
            this.tf_bgmPath.setBackground(Color.WHITE);
            this.tf_imagePath.setBackground(Color.WHITE);
            this.tf_savePath.setBackground(Color.WHITE);
            this.tf_mapName.setBackground(Color.WHITE);
            this.tf_tw.setBackground(Color.WHITE);
            this.tf_th.setBackground(Color.WHITE);
            this.btn_generate.setBackground(this.standardBtnBaground);
            this.btn_update.setBackground(this.standardBtnBaground);
            if (e.getSource() == this.btn_findImage) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File e) {
                        if (e.isDirectory()) return true; else {
                            String name = e.getPath().toLowerCase();
                            return (name.matches(".*\\.png") || name.matches(".*\\.jpg") || name.matches(".*\\.jpeg") || name.matches(".*\\.gif"));
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Image files ( .png / .jpg / .jpeg / .gif )";
                    }
                });
                int result = fc.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) this.tf_imagePath.setText(fc.getSelectedFile().getPath());
            } else if (e.getSource() == this.btn_findSave) {
                if (mapW.mapName == "") {
                    this.tf_mapName.setBackground(DISABLED_COLOR);
                    this.btn_update.setBackground(DISABLED_COLOR);
                    return;
                }
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fc.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File e) {
                        if (e.isDirectory()) return true; else {
                            return e.getPath().toLowerCase().matches(".*\\.shmap ");
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Shinigami Map Files ( .shmap )";
                    }
                });
                int result = fc.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if (!f.isDirectory()) {
                        int option = JOptionPane.showConfirmDialog(null, "File already exists. Overwrite?");
                        if (option != JOptionPane.OK_OPTION) return;
                    }
                    String s = f.getPath();
                    if (!s.matches(".*\\.shmap")) s = s + ".shmap";
                    this.tf_savePath.setText(fc.getSelectedFile().getPath() + File.separator + mapW.mapName + ".shmap");
                }
            } else if (e.getSource() == this.btn_update) {
                boolean allRight = true;
                if (tf_mapName.getText().contains(" ") || tf_mapName.getText().equals("")) {
                    tf_mapName.setBackground(DISABLED_COLOR);
                    allRight = false;
                }
                int width = 0, height = 0;
                try {
                    width = Integer.parseInt(tf_width.getText());
                } catch (NumberFormatException exception) {
                    this.tf_width.setBackground(DISABLED_COLOR);
                    allRight = false;
                }
                try {
                    height = Integer.parseInt(tf_height.getText());
                } catch (NumberFormatException exception) {
                    this.tf_height.setBackground(DISABLED_COLOR);
                    allRight = false;
                }
                int tw = 0, th = 0;
                try {
                    tw = Integer.parseInt(tf_tw.getText());
                } catch (NumberFormatException exception) {
                    this.tf_tw.setBackground(DISABLED_COLOR);
                    allRight = false;
                }
                try {
                    th = Integer.parseInt(tf_th.getText());
                } catch (NumberFormatException exception) {
                    this.tf_th.setBackground(DISABLED_COLOR);
                    allRight = false;
                }
                try {
                    ImageIO.read(new File(tf_imagePath.getText()));
                } catch (IOException exception) {
                    this.tf_imagePath.setBackground(DISABLED_COLOR);
                    allRight = false;
                }
                if (!tf_bgmPath.getText().equals("")) {
                    try {
                        Sound.create(tf_bgmPath.getText());
                    } catch (Exception exception) {
                        this.tf_bgmPath.setBackground(DISABLED_COLOR);
                        allRight = false;
                    }
                }
                if (allRight) {
                    File sound = null;
                    if (!tf_bgmPath.getText().equals("")) sound = new File(tf_bgmPath.getText());
                    mapW.updateState(tf_mapName.getText(), new File(tf_imagePath.getText()), sound, width, height, tw, th);
                }
            } else if (e.getSource() == this.btn_generate) {
                tf_mapName.setEnabled(false);
                tf_width.setEnabled(false);
                tf_height.setEnabled(false);
                tf_savePath.setEnabled(false);
                tf_imagePath.setEnabled(false);
                tf_tw.setEnabled(false);
                tf_th.setEnabled(false);
                btn_update.setEnabled(false);
                btn_findImage.setEnabled(false);
                btn_findSave.setEnabled(false);
                btn_generate.setEnabled(false);
                if (mapW.createFile(new File(this.tf_savePath.getText()), new File(this.tf_imagePath.getText()))) {
                    JOptionPane.showMessageDialog(null, "File Created");
                } else this.tf_savePath.setBackground(DISABLED_COLOR);
                tf_mapName.setEnabled(true);
                tf_width.setEnabled(true);
                tf_height.setEnabled(true);
                tf_savePath.setEnabled(true);
                tf_imagePath.setEnabled(true);
                tf_tw.setEnabled(true);
                tf_th.setEnabled(true);
                btn_update.setEnabled(true);
                btn_findImage.setEnabled(true);
                btn_findSave.setEnabled(true);
                btn_generate.setEnabled(true);
            } else if (e.getSource() == mi_open) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File e) {
                        if (e.isDirectory()) return true; else return e.getPath().toLowerCase().matches(".*\\.shmap");
                    }

                    @Override
                    public String getDescription() {
                        return "Shinigami Map Files ( .shmap )";
                    }
                });
                int result = fc.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    MapScope s = MapScope.createScope(new MapFile(fc.getSelectedFile().getPath()));
                    this.tf_mapName.setText(s.getMapName());
                    this.tf_width.setText("" + s.getWidth());
                    this.tf_height.setText("" + s.getHeight());
                    this.tf_tw.setText("" + s.getTileWidth());
                    this.tf_th.setText("" + s.getTileHeight());
                    this.tf_imagePath.setText("" + s.getBackgroundFile().getPath());
                    this.tf_savePath.setText(fc.getSelectedFile().getPath());
                    mapW.updateState(s.getMapName(), s.getBackgroundFile(), s.getBGM(), s.getWidth(), s.getHeight(), s.getTileWidth(), s.getTileHeight());
                    for (int i = 0; i < mapW.nodes.length; i++) for (int k = 0; k < mapW.nodes[i].length; k++) for (int m = 0; m < mapW.nodes[i][k].length; m++) mapW.nodes[i][k][m].accessible = s.getPassage(i, k, m);
                    for (PortalScope ps : s.getPortals()) mapW.portals.add(new GenPortal(ps.TX, ps.TY, ps.TARGET, ps.TTX, ps.TTY));
                    mapW.imX = s.getBackgroundAdjustX();
                    mapW.imY = s.getBackgroundAdjustY();
                }
            } else if (e.getSource() == mi_about) {
                CREDITS.setVisible(true);
            } else if (e.getSource() == this.btn_findBGM) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.addChoosableFileFilter(new FileFilter() {

                    @Override
                    public boolean accept(File e) {
                        if (e.isDirectory()) return true; else {
                            String name = e.getPath().toLowerCase();
                            return (name.matches(".*\\.mid") || name.matches(".*\\.mp3"));
                        }
                    }

                    @Override
                    public String getDescription() {
                        return "Sound files ( .mp3 / .mid )";
                    }
                });
                int result = fc.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    this.tf_bgmPath.setText(fc.getSelectedFile().getPath());
                }
            } else if (e.getSource() == this.btn_play) {
                if (playingSound == null) {
                    playingSound = Sound.create(this.tf_bgmPath.getText());
                    playingSound.play();
                    this.btn_play.setText("Stop");
                } else {
                    playingSound.stop();
                    playingSound = null;
                    this.btn_play.setText("Play");
                }
            }
        }
    }

    static Sound playingSound = null;

    private static final JFrame CREDITS = new JFrame() {

        /** SERIAL MUMBO-JUMBO */
        private static final long serialVersionUID = -2450930699668902742L;

        {
            this.setTitle("ShMap Generator - Version: " + VERSION);
            this.setResizable(false);
            this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            this.getContentPane().add(new JLabel(new ImageIcon("Project Files/ShMap Generator.png")));
            this.pack();
            this.setLocationRelativeTo(null);
            this.setAlwaysOnTop(true);
            this.setEnabled(true);
            this.setVisible(false);
        }
    };

    private static class Node {

        boolean accessible = true;

        int centerx = 0;

        int centery = 0;

        private static final int RADIUS = 5;

        public Node(int centerx, int centery) {
            this.centerx = centerx;
            this.centery = centery;
        }

        public void render(Graphics g, int x, int y) {
            if (accessible) g.setColor(Color.GREEN); else g.setColor(Color.RED);
            g.fillOval(x + centerx - RADIUS, y + centery - RADIUS, RADIUS * 2, RADIUS * 2);
        }

        public void onClick(int x, int y) {
            if ((x > (centerx - RADIUS - 2)) && (x < (centerx + RADIUS + 2)) && (y > (centery - RADIUS - 2)) && (y < (centery + RADIUS + 2))) accessible = !accessible;
        }

        public void onArea(int stx, int sty, int endx, int endy) {
            if ((centerx > stx) && (centerx < endx) && (centery > sty) && (centery < endy)) accessible = !accessible;
        }
    }

    static class GenPortal {

        final int TX;

        final int TY;

        final String TARGET;

        final int TARGET_X;

        final int TARGET_Y;

        final String MESSAGE;

        Rectangle2D box = null;

        final Point START;

        static final int RADIUS = 20;

        static final Color COLOR = Color.BLUE;

        static final Color BOX = new Color(0.8f, 0.8f, 0.6f);

        public GenPortal(int tx, int ty, String target, int targetX, int targetY) {
            this.TX = tx;
            this.TY = ty;
            this.TARGET = target;
            this.TARGET_X = targetX;
            this.TARGET_Y = targetY;
            this.MESSAGE = TARGET + " @ " + TARGET_X + "x" + TARGET_Y;
            this.START = new Point((tx * tileWidth) + (tileWidth / 2), (ty * tileHeight) + (tileHeight / 2));
        }

        public void render(Graphics2D g, int adjustX, int adjustY, boolean show) {
            g.setColor(Color.BLACK);
            g.drawOval(START.x + adjustX - (RADIUS / 2), START.y + adjustY - (RADIUS / 2), RADIUS, RADIUS);
            g.setColor(COLOR);
            g.fillOval(START.x + adjustX - (RADIUS / 2), START.y + adjustY - (RADIUS / 2), RADIUS, RADIUS);
            if (show) {
                if (box == null) box = g.getFontMetrics().getStringBounds(MESSAGE, g);
                g.setColor(BOX);
                g.fillRect(START.x + adjustX, START.y + adjustY, (int) box.getWidth() + 6, (int) box.getHeight() + 6);
                g.setColor(Color.BLACK);
                g.drawRect(START.x + adjustX, START.y + adjustY, (int) box.getWidth() + 6, (int) box.getHeight() + 6);
                g.drawString(MESSAGE, START.x + adjustX + 3, START.y + adjustY + ((int) box.getHeight()));
            }
        }
    }
}

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.channels.FileChannel;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.Color;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

/**
 * 
 * @author xEnt
 *
 */
public class Main extends JPanel implements MouseListener, MouseMotionListener {

    public static int tileSize = 19;

    public int temp = tileSize;

    public Tile[] tiles = new Tile[48 * 48];

    public Tile[] oldTiles;

    public Graphics2D g2d;

    public Graphics g;

    public ZipFile tileArchive;

    public ByteBuffer data;

    String ts = "\\";

    Object[] Brushes = { "None", "East Wall", "East Fence", "North Wall", "North Fence", "Wooden Floor", "Water", "Grey Path", "Dark Red Tile", "White Tile", "Empty Tile(Black)", "Diagonal Wall /", "Diagonal Wall \\" };

    JLabel label2;

    JButton button1;

    public int[][] ColorMap;

    public Object SelectedBrush;

    public ByteBuffer out;

    public String ourFile;

    public int lol = 0;

    public int lastTile = 0;

    public JList list;

    public JLabel label1;

    public JRadioButton jbutt1;

    public JRadioButton jbutt2;

    public Main() {
        oldTiles = new Tile[2304];
        ColorMap = new int[255][2];
        addMouseListener(this);
        addMouseMotionListener(this);
        loadGUI();
        setLayout(null);
    }

    public void paintComponent(Graphics g) {
        try {
            super.paintComponent(g);
            g2d = (Graphics2D) g;
            drawTiles();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void loadGUI() {
        try {
            JButton mybtn = new JButton("Load Landscape");
            mybtn.setLocation(925, 27);
            mybtn.setSize(140, 25);
            mybtn.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    final JFileChooser fc = new JFileChooser();
                    JOptionPane.showMessageDialog(null, "Select your Landscape.rscd File please.");
                    int returnVal = fc.showOpenDialog(Main.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        unpack(file, false, false);
                        setTiles();
                        Graphics g = getGraphics();
                        paintComponent(g);
                    } else {
                    }
                }
            });
            super.add(mybtn);
            JButton mybtnn = new JButton("Revert Sector");
            mybtnn.setLocation(925, 111);
            mybtnn.setSize(140, 25);
            mybtnn.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    File f = new File("");
                    unpack(f, true, false);
                    setTiles();
                }
            });
            super.add(mybtnn);
            jbutt1 = new JRadioButton("Add", true);
            jbutt1.setLocation(925, 670);
            jbutt1.setSize(140, 20);
            jbutt1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (jbutt1.isSelected()) {
                        jbutt1.setSelected(true);
                        jbutt2.setSelected(false);
                    }
                }
            });
            super.add(jbutt1);
            jbutt2 = new JRadioButton("Remove");
            jbutt2.setLocation(925, 690);
            jbutt2.setSize(140, 20);
            jbutt2.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (jbutt2.isSelected()) {
                        jbutt2.setSelected(true);
                        jbutt1.setSelected(false);
                    }
                }
            });
            super.add(jbutt2);
            JButton mybtnnn = new JButton("Load Sector");
            mybtnnn.setLocation(925, 139);
            mybtnnn.setSize(140, 25);
            mybtnnn.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                }
            });
            super.add(mybtnnn);
            label1 = new JLabel("");
            label1.setLocation(450, 2);
            label1.setSize(100, 30);
            label1.setFont(new Font("Arial", Font.BOLD, 15));
            label1.setForeground(Color.YELLOW);
            super.add(label1);
            JLabel label = new JLabel("Brushes:");
            label.setLocation(925, 213);
            label.setSize(100, 30);
            label.setForeground(Color.YELLOW);
            label.setFont(new Font("Arial", Font.BOLD, 15));
            super.add(label);
            list = new JList(Brushes);
            JScrollPane pane = new JScrollPane(list);
            list.setLocation(925, 240);
            list.setSize(140, 420);
            list.addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent e) {
                    SelectedBrush = Brushes[list.getSelectedIndex()];
                }
            });
            super.add(list);
            list.setSelectedValue(Brushes[0], false);
            JButton mybtn1 = new JButton("Save Landscape");
            mybtn1.setLocation(925, 55);
            mybtn1.setSize(140, 25);
            mybtn1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    pack();
                    JOptionPane.showMessageDialog(null, "Landscape saved.");
                }
            });
            super.add(mybtn1);
            JButton mybtn2 = new JButton("Select Brush");
            mybtn2.setLocation(925, 83);
            mybtn2.setSize(140, 25);
            mybtn2.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    SelectedBrush = JOptionPane.showInputDialog(null, "Select a Brush", "Paint Landscape.", JOptionPane.QUESTION_MESSAGE, null, Brushes, null);
                }
            });
            super.add(mybtn2);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void unpack(File file, boolean revert, boolean newsect) {
        try {
            if (revert) {
                ZipEntry f = tileArchive.getEntry(ourFile);
                label1.setText(ourFile);
                if (f != null) {
                    data = streamToBuffer(new BufferedInputStream(tileArchive.getInputStream(f)));
                }
            } else {
                if (!newsect) tileArchive = new ZipFile(file);
                String s = (String) JOptionPane.showInputDialog(null, "Enter Map String (eg h0x50y50)", "Landscape Editor", JOptionPane.PLAIN_MESSAGE, null, null, "h0x50y50");
                ZipEntry e = tileArchive.getEntry(s);
                ourFile = s;
                label1.setText(ourFile);
                if (e != null) {
                    data = streamToBuffer(new BufferedInputStream(tileArchive.getInputStream(e)));
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void pack() {
        try {
            String s = (String) JOptionPane.showInputDialog(null, "Enter Path + Filename to save, Make sure theres no existing file in the same directory.", "Landscape Editor", JOptionPane.PLAIN_MESSAGE, null, null, "C:/" + ourFile);
            for (int i = 0; i < 48 * 48; i++) {
                out = tiles[i].pack();
                File file = new File(s);
                FileChannel wChannel = new FileOutputStream(file, true).getChannel();
                wChannel.write(out);
                wChannel.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static final ByteBuffer streamToBuffer(BufferedInputStream in) {
        try {
            byte[] buffer = new byte[in.available()];
            in.read(buffer, 0, buffer.length);
            return ByteBuffer.wrap(buffer);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public void drawTiles() {
        try {
            for (int i = 0; i < 48 * 48; i++) {
                g2d.setColor(Color.BLACK);
                g2d.draw(tiles[i].shape);
                if (tiles[i].groundOverlay == 3) {
                    g2d.setColor(new Color(139, 69, 19));
                }
                if (tiles[i].groundOverlay == 2) {
                    g2d.setColor(new Color(30, 144, 255));
                }
                if (tiles[i].groundOverlay == 6) {
                    g2d.setColor(new Color(153, 0, 0));
                }
                if (tiles[i].groundOverlay == 4) {
                    g2d.setColor(new Color(139, 69, 19));
                }
                if (tiles[i].groundOverlay == 0) {
                    g2d.setColor(new Color(102, 204, 0));
                }
                if (tiles[i].groundOverlay == 1 || tiles[i].groundOverlay == 5) {
                    g2d.setColor(Color.GRAY);
                }
                g2d.fill(tiles[i].shape);
                g2d.draw(tiles[i].shape);
                if (tiles[i].verticalWall == 1 || tiles[i].verticalWall == 15 || tiles[i].verticalWall == 7 || tiles[i].verticalWall == 6 || tiles[i].verticalWall == 14 || tiles[i].verticalWall == 5) {
                    if (tiles[i].verticalWall == 5) {
                        g2d.setColor(new Color(139, 69, 19));
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                    Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y, tiles[i].x, tiles[i].y);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.draw(line);
                }
                if (tiles[i].horizontalWall == 1 || tiles[i].horizontalWall == 17 || tiles[i].horizontalWall == 6 || tiles[i].horizontalWall == 16 || tiles[i].horizontalWall == 7 || tiles[i].horizontalWall == 5 || tiles[i].horizontalWall == 15) {
                    if (tiles[i].horizontalWall == 5) {
                        g2d.setColor(new Color(139, 69, 19));
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                    Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.draw(line);
                }
                if (tiles[i].diagonalWalls == 1) {
                    Line2D line = new Line2D.Double(tiles[i].x, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.draw(line);
                }
                if (tiles[i].diagonalWalls == 12001 || tiles[i].diagonalWalls == 12004) {
                    Line2D line = new Line2D.Double(tiles[i].x, tiles[i].y, tiles[i].x + 19, tiles[i].y + 19);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.draw(line);
                }
                drawGUI();
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    void drawGUI() {
        list.setVisible(false);
        list.setVisible(true);
    }

    public void setTiles() {
        try {
            int count = 0;
            int curX = 900;
            int curY = 10;
            int temp = tileSize;
            for (int i = 0; i < 48 * 48; i++) {
                lol++;
                if (count == 48) {
                    curX = curX - 19;
                    curY = curY - tileSize * 48;
                    count = 0;
                }
                tiles[i] = new Tile();
                tiles[i].y = curY + temp;
                tiles[i].x = curX;
                tiles[i] = tiles[i].unpack(data, tiles[i]);
                count++;
                temp = temp + tileSize;
                tiles[i].shape = new Rectangle(tiles[i].x, tiles[i].y, tileSize, tileSize);
                oldTiles[i] = tiles[i];
            }
            drawTiles();
        } catch (Exception e) {
            System.out.println(e);
            System.out.println(lol);
        }
    }

    public void updateTile(int i, Graphics gfx) {
        g2d.setColor(Color.BLACK);
        g2d.draw(tiles[i].shape);
        if (tiles[i].groundOverlay == 3) {
            g2d.setColor(new Color(139, 69, 19));
        }
        if (tiles[i].groundOverlay == 2) {
            g2d.setColor(new Color(30, 144, 255));
        }
        if (tiles[i].groundOverlay == 4) {
            g2d.setColor(new Color(139, 69, 19));
        }
        if (tiles[i].groundOverlay == 0) {
            g2d.setColor(new Color(34, 139, 34));
        }
        if (tiles[i].groundOverlay == 1) {
            g2d.setColor(Color.GRAY);
        }
        g2d.fill(tiles[i].shape);
        g2d.draw(tiles[i].shape);
        if (tiles[i].verticalWall == 1 || tiles[i].verticalWall == 15 || tiles[i].verticalWall == 7 || tiles[i].verticalWall == 14 || tiles[i].verticalWall == 5) {
            if (tiles[i].verticalWall == 5) {
                g2d.setColor(new Color(139, 69, 19));
            } else {
                g2d.setColor(Color.WHITE);
            }
            Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y, tiles[i].x, tiles[i].y);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(line);
        }
        if (tiles[i].horizontalWall == 1 || tiles[i].horizontalWall == 16 || tiles[i].horizontalWall == 7 || tiles[i].horizontalWall == 5 || tiles[i].horizontalWall == 15) {
            if (tiles[i].horizontalWall == 5) {
                g2d.setColor(new Color(139, 69, 19));
            } else {
                g2d.setColor(Color.WHITE);
            }
            Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(line);
        }
        if (tiles[i].diagonalWalls == 1) {
            Line2D line = new Line2D.Double(tiles[i].x, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(line);
        }
        if (tiles[i].diagonalWalls > 130 && tiles[i].diagonalWalls < 5000) {
            Line2D line = new Line2D.Double(tiles[i].x, tiles[i].y, tiles[i].x + 19, tiles[i].y + 19);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(line);
        }
    }

    public void onRightClick(Point p, Graphics gfx) {
        this.g2d = (Graphics2D) gfx;
        for (int i = 0; i < 48 * 48; i++) {
            if (tiles[i].shape.getBounds().contains(p)) {
                System.out.println("First Tile: " + tiles[i].groundTexture + "  Old: " + oldTiles[i].groundElevation);
                tiles[i] = oldTiles[i];
                updateTile(i, gfx);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public static boolean mousePressed = false;

    public void mouseDragged(MouseEvent evt) {
        mousePressed = true;
        Graphics gfx = super.getGraphics();
        if (evt.getButton() == MouseEvent.BUTTON2) {
            onRightClick(evt.getPoint(), gfx);
        } else {
            onClick(evt.getPoint(), gfx);
        }
    }

    public void mouseMoved(MouseEvent evt) {
    }

    public void mouseReleased(MouseEvent e) {
        mousePressed = false;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
        Graphics gfx = super.getGraphics();
        if (jbutt1.isSelected()) {
            if (SwingUtilities.isRightMouseButton(e)) {
                onRightClick(e.getPoint(), gfx);
            } else {
                onClick(e.getPoint(), gfx);
            }
        } else {
        }
    }

    public static void main(String[] args) {
        WindowUtilities.openInJFrame(new Main(), 1080, 980);
    }

    public void onClick(Point p, Graphics gfx) {
        try {
            this.g2d = (Graphics2D) gfx;
            for (int i = 0; i < 48 * 48; i++) {
                if (tiles[i].shape.getBounds().contains(p)) {
                    System.out.println(tiles[i].groundTexture);
                    if ((String) SelectedBrush == "Wooden Floor") {
                        tiles[i].groundOverlay = 3;
                        g2d.setColor(new Color(139, 69, 19));
                        this.g2d.fill(tiles[i].shape);
                        this.g2d.draw(tiles[i].shape);
                    }
                    if ((String) SelectedBrush == "North Wall") {
                        tiles[i].verticalWall = 1;
                        g2d.setColor(Color.WHITE);
                        Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y, tiles[i].x, tiles[i].y);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(line);
                    }
                    if ((String) SelectedBrush == "East Wall") {
                        tiles[i].horizontalWall = 1;
                        g2d.setColor(Color.WHITE);
                        Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(line);
                    }
                    if ((String) SelectedBrush == "Water") {
                        tiles[i].groundOverlay = 2;
                        g2d.setColor(new Color(30, 144, 255));
                        g2d.fill(tiles[i].shape);
                        g2d.draw(tiles[i].shape);
                    }
                    if ((String) SelectedBrush == "East Fence") {
                        tiles[i].horizontalWall = 5;
                        g2d.setColor(new Color(139, 69, 19));
                        Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(line);
                    }
                    if ((String) SelectedBrush == "North Fence") {
                        tiles[i].verticalWall = 5;
                        g2d.setColor(new Color(139, 69, 19));
                        Line2D line = new Line2D.Double(tiles[i].x + 19, tiles[i].y, tiles[i].x, tiles[i].y);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(line);
                    }
                    if ((String) SelectedBrush == "Grey Path") {
                        tiles[i].groundOverlay = 1;
                        g2d.setColor(Color.GRAY);
                        g2d.fill(tiles[i].shape);
                        g2d.draw(tiles[i].shape);
                    }
                    if ((String) SelectedBrush == "Diagonal Wall /") {
                        tiles[i].diagonalWalls = 1;
                        Line2D line = new Line2D.Double(tiles[i].x, tiles[i].y + 19, tiles[i].x + 19, tiles[i].y);
                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(line);
                    }
                    if ((String) SelectedBrush == "Diagonal Wall \\") {
                        tiles[i].diagonalWalls = 12001;
                        Line2D line = new Line2D.Double(tiles[i].x, tiles[i].y, tiles[i].x + 19, tiles[i].y + 19);
                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.draw(line);
                    }
                    if ((String) SelectedBrush == "Dark Red Tile") {
                        tiles[i].groundOverlay = 6;
                        g2d.setColor(new Color(153, 0, 0));
                        g2d.fill(tiles[i].shape);
                        g2d.draw(tiles[i].shape);
                    }
                    if ((String) SelectedBrush == "Yellow/Gold Tile") {
                        tiles[i].groundTexture = (byte) 130;
                        g2d.setColor(new Color(255, 204, 0));
                        g2d.fill(tiles[i].shape);
                        g2d.draw(tiles[i].shape);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

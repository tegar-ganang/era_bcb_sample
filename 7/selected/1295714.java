package editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener.*;
import java.awt.image.VolatileImage;

public class Map extends JPanel implements Runnable {

    Thread main;

    public static int maxWidth = 0;

    public static int maxHeight = 0;

    public static int fillx;

    public static EditorObject[] object = new EditorObject[99999];

    public static Image[] background_layer = new Image[2];

    public static EditorObject[] selectedObject;

    private boolean update = false;

    private Rectangle selectionRect = null;

    private EditorObject[] selectionObjects = null;

    private Point selectedPoint = null;

    private boolean clickingSelection = false;

    private static EditorObject[] objectTemp = null;

    private static boolean moving = false;

    MouseKlick mouseClickListener = new MouseKlick();

    MouseMotion mouseMotionListener = new MouseMotion();

    VolatileImage grid = this.createVolatileImage(maxWidth, maxHeight);

    VolatileImage objectLayer = this.createVolatileImage(maxWidth, maxHeight);

    public static int tile[] = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };

    public Map() {
        setPreferredSize(new Dimension(1000, 1000));
        this.setDoubleBuffered(true);
        this.addMouseListener(mouseClickListener);
        this.addMouseMotionListener(mouseMotionListener);
        main = new Thread(this);
        main.start();
    }

    public void run() {
        while (true) {
            try {
                main.sleep(20L);
            } catch (Exception e) {
            }
            if (getPreferredSize() != new Dimension(maxWidth, maxHeight)) {
                setPreferredSize(new Dimension(maxWidth, maxHeight));
            }
            MouseActions(new Point((int) mouseMotionListener.getX(), (int) mouseMotionListener.getY()));
            KeyAction();
            repaint();
        }
    }

    public int getTileAt(Point p) {
        boolean isTile = false;
        if (object[getObjectNumberAt(p)] != null) {
            for (int i = 0; i < tile.length; i++) {
                if (object[getObjectNumberAt(p)].tile == tile[i]) isTile = true;
            }
        }
        if (object[getObjectNumberAt(p)] != null && isTile == true) {
            return object[getObjectNumberAt(p)].tile;
        } else {
            return -1;
        }
    }

    public void sortTiles() {
        int a = 0;
        while (object[a] != null) {
            boolean isTile = false;
            for (int i = 0; i < tile.length; i++) {
                if (object[a].tile == tile[i]) isTile = true;
            }
            if (isTile == true) {
                object[a].tile = tile[7];
                Point[] p = new Point[] { new Point(object[a].position.x - 1, object[a].position.y), new Point(object[a].position.x, object[a].position.y - 1), new Point(object[a].position.x + 1, object[a].position.y) };
                int[] borderTile = new int[] { tile[4], tile[6], tile[5] };
                for (int i = 0; i < p.length; i++) {
                    if (getObjectNumberAt(p[i]) == -1 || getTileAt(p[i]) == -1) {
                        object[a].tile = borderTile[i];
                    }
                }
                Point[][] cornerPoint = new Point[][] { new Point[] { p[0], p[1] }, new Point[] { p[2], p[1] }, new Point[] { p[0], p[1] }, new Point[] { p[2], p[1] }, new Point[] { p[0], p[1] }, new Point[] { p[2], p[1] }, new Point[] { p[0], p[1] }, new Point[] { p[2], p[1] }, new Point[] { p[0], p[1] }, new Point[] { p[2], p[1] } };
                int[][] checkTileNumber = new int[][] { new int[] { -1, -1 }, new int[] { -1, -1 }, new int[] { tile[6], tile[4] }, new int[] { tile[6], tile[5] }, new int[] { tile[0], tile[4] }, new int[] { tile[1], tile[5] }, new int[] { tile[0], tile[0] }, new int[] { tile[1], tile[1] }, new int[] { tile[6], tile[0] }, new int[] { tile[6], tile[1] } };
                borderTile = new int[] { tile[0], tile[1], tile[3], tile[2], tile[3], tile[2], tile[3], tile[2], tile[3], tile[2] };
                for (int i = 0; i < cornerPoint.length; i++) {
                    if ((getObjectNumberAt(cornerPoint[i][0]) == checkTileNumber[i][0] || getTileAt(cornerPoint[i][0]) == checkTileNumber[i][0]) && (getObjectNumberAt(cornerPoint[i][1]) == checkTileNumber[i][1] || getTileAt(cornerPoint[i][1]) == checkTileNumber[i][1])) {
                        object[a].tile = borderTile[i];
                    }
                }
            }
            a++;
        }
    }

    public void setMapSize(int a, int b) {
        maxWidth = a;
        maxHeight = b;
        createGrid(a + 1, b + 1);
    }

    public void createGrid(int width, int height) {
        grid = this.getGraphicsConfiguration().createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
        Graphics2D g2d = grid.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, grid.getWidth(), grid.getHeight());
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.setColor(new Color(1f, 1f, 1f, 0.5f));
        for (int x = 0; x < width; x += 16) {
            g2d.drawLine(x - 1, 0, x - 1, height - 1);
        }
        for (int y = 0; y < height; y += 16) {
            g2d.drawLine(0, y - 1, width - 1, y - 1);
        }
    }

    public void renderObjectLayer() {
        objectLayer = this.getGraphicsConfiguration().createCompatibleVolatileImage(maxWidth * 16, maxHeight * 16, Transparency.TRANSLUCENT);
        Graphics2D g2d = objectLayer.createGraphics();
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, grid.getWidth(), grid.getHeight());
        g2d.setBackground(new Color(0, 0, 0, 0));
        int a = 0;
        while (object[a] != null) {
            object[a].draw(g2d, this);
            a++;
        }
    }

    private void MouseActions(Point p) {
        if (Editor.drawMode == Editor.DRAW && Editor.selectedObject != null) {
            if (mouseClickListener.getButton() == 1) {
                Point p16 = new Point(p.x / 16, p.y / 16);
                if (!Editor.selectedObject.name.equals("WorldTile")) {
                    object[getObjectNumberAt(p16)] = new EditorObject(Editor.selectedObject, p16);
                } else {
                    object[getObjectNumberAt(p16)] = new EditorObject(Editor.selectedObject, p16, Integer.parseInt("" + Editor.selectedObject.objectChar));
                }
            }
        }
        if (Toolbox.ToolboxTab.getSelectedIndex() == 0 && Editor.drawMode == Editor.SELECT) {
            if (mouseClickListener.getButtonState(1) == true) {
                if (object[getObjectNumberAt(new Point(p.x / 16, p.y / 16))] != null) {
                    if (object[getObjectNumberAt(new Point(p.x / 16, p.y / 16))].selected == true) {
                        clickingSelection = true;
                    }
                }
                if (clickingSelection == false) {
                    if (mouseClickListener.initialClick.x >= p.x && mouseClickListener.initialClick.y >= p.y) selectionRect = new Rectangle(p.x, p.y, mouseClickListener.initialClick.x - p.x, Math.abs(mouseClickListener.initialClick.y - p.y));
                    if (mouseClickListener.initialClick.x >= p.x && mouseClickListener.initialClick.y < p.y) selectionRect = new Rectangle(p.x, mouseClickListener.initialClick.y, mouseClickListener.initialClick.x - p.x, Math.abs(p.y - mouseClickListener.initialClick.y));
                    if (mouseClickListener.initialClick.x < p.x && mouseClickListener.initialClick.y >= p.y) selectionRect = new Rectangle(mouseClickListener.initialClick.x, p.y, p.x - mouseClickListener.initialClick.x, Math.abs(mouseClickListener.initialClick.y - p.y));
                    if (mouseClickListener.initialClick.x < p.x && mouseClickListener.initialClick.y < p.y) selectionRect = new Rectangle(mouseClickListener.initialClick.x, mouseClickListener.initialClick.y, p.x - mouseClickListener.initialClick.x, Math.abs(p.y - mouseClickListener.initialClick.y));
                } else {
                    moving = true;
                    int a = 0;
                    while (object[a] != null) {
                        if (object[a].selected) {
                            object[a].position = new Point(objectTemp[a].position.x + p.x / 16 - mouseClickListener.initialClick.x / 16, objectTemp[a].position.y - mouseClickListener.initialClick.y / 16 + p.y / 16);
                        }
                        a++;
                    }
                }
            } else {
                if (selectionRect != null) {
                    selectedPoint = new Point(selectionRect.x / 16, selectionRect.y / 16);
                    selectionObjects = new EditorObject[99999];
                    int a = 0;
                    while (object[a] != null) {
                        object[a].selected = false;
                        a++;
                    }
                    a = 0;
                    for (int x = selectionRect.x / 16; x < selectionRect.x / 16 + selectionRect.width / 16 + 1; x++) {
                        for (int y = selectionRect.y / 16; y < selectionRect.y / 16 + selectionRect.height / 16 + 1; y++) {
                            int n = getObjectNumberAt(new Point(x, y));
                            if (object[n] != null) {
                                selectionObjects[a] = object[n];
                                object[n].selected = true;
                                a++;
                            }
                        }
                    }
                }
                objectTemp = new EditorObject[99999];
                int a = 0;
                while (object[a] != null) {
                    objectTemp[a] = new EditorObject(new GameObject(object[a].name, object[a].objectChar), object[a].position);
                    a++;
                }
                selectionRect = null;
                clickingSelection = false;
            }
        }
        if (Toolbox.ToolboxTab.getSelectedIndex() == 1 && Editor.drawMode == Editor.SELECT) {
            if (mouseClickListener.getButtonState(1) == true) {
                if (new Rectangle(0, Editor.cameraPrefHeight - Editor.cameraTolerance / 2, maxWidth, Editor.cameraTolerance).contains(p)) {
                    Editor.cameraPrefHeight = Integer.parseInt(Toolbox.camPrefHeightSpinner.getValue().toString()) + p.y - mouseClickListener.initialClick.y;
                    update = true;
                }
            } else {
                if (update == true) {
                    Toolbox.camPrefHeightSpinner.setValue(Editor.cameraPrefHeight);
                    update = false;
                }
            }
        }
        if (Editor.drawMode == Editor.ERASE) {
            if (mouseClickListener.getButton() == 1) {
                Point p16 = new Point(p.x / 16, p.y / 16);
                int objectNumber = getNumberOfObjects();
                for (int i = getObjectNumberAt(p16); i < objectNumber; i++) {
                    object[i] = object[i + 1];
                }
                object[objectNumber] = null;
            }
        }
    }

    public int getNumberOfObjects() {
        int a = 0;
        while (object[a] != null) {
            a++;
        }
        return a;
    }

    public static int getObjectNumberAt(Point p) {
        int a = 0;
        while (object[a] != null) {
            if (object[a].position.equals(p)) {
                return a;
            }
            a++;
        }
        return a;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        super.paintComponent(g2d);
        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        g2d.setColor(Color.GRAY);
        g2d.fillRect(0, 0, maxWidth, maxHeight);
        if (Editor.bgLayerCheckBox.getState() == true) {
            try {
                for (int i = 0; i < background_layer.length; i++) {
                    for (int w = 0; w < ((Editor.loadedLevel.getWidth() * 16) / background_layer[i].getWidth(this)) + 1; w++) {
                        g2d.drawImage(background_layer[i], background_layer[i].getWidth(this) * w, background_layer[i].getHeight(this) - background_layer[i].getHeight(this) / (i + 1), this);
                    }
                }
            } catch (Exception e) {
            }
        }
        int a = 0;
        while (object[a] != null) {
            object[a].draw(g, this);
            a++;
        }
        if (Editor.gridCheckBox.getState() == true) {
            g2d.drawImage(grid, 0, 0, this);
        }
        if (selectionRect != null) {
            g2d.setColor(new Color(0.4f, 0.4f, 1f));
            g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
            g2d.setColor(new Color(0.2f, 0.2f, 1, 0.25f));
            g2d.fillRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
        }
        if (Editor.cameraCheckBox.getState() == true) {
            g2d.setColor(new Color(1.0f, 0f, 0f, 0.33f));
            g2d.fillRect(0, Editor.cameraPrefHeight - Editor.cameraTolerance / 2, maxWidth, Editor.cameraTolerance);
            g2d.setColor(Color.red);
            g2d.drawLine(0, Editor.cameraPrefHeight, maxWidth, Editor.cameraPrefHeight);
        }
    }

    public static void clear() {
        int a = 0;
        while (object[a] != null) {
            object[a] = null;
            a++;
        }
    }

    private void KeyAction() {
        if (Editor.keyPressed[23] && Editor.keyPressed[23]) {
        }
    }
}

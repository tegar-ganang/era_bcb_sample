import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.Random;

public class mainCanvas extends Canvas implements CommandListener {

    public static final int ROTATE_LEFT = 1;

    public static final int ROTATE_RIGHT = 2;

    public static final int LEFT = 3;

    public static final int RIGHT = 4;

    public static final int STEP = 5;

    public static final int DROP = 6;

    public static final int COLS = 12;

    public static final int ROWS = 18;

    private int areaWidth;

    private int areaHeight;

    private int blockWidth, blockHeight;

    private Brick brick;

    private Row rows[];

    private Random rand;

    private Tetris t;

    private Scoring score;

    private boolean showLost = false;

    private boolean showWon = false;

    public mainCanvas(Tetris t) {
        this.t = t;
        rand = new Random();
        score = new Scoring();
        newBrick();
        rows = new Row[ROWS];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new Row(i);
        }
        setFullScreenMode(true);
        Command exit = new Command("Beenden", Command.EXIT, 2);
    }

    public void keyPressed(int keyCode) {
        boolean useGameAction = false;
        System.err.println(keyCode);
        if (useGameAction) {
            switch(getGameAction(keyCode)) {
                case Canvas.UP:
                    brickTransition(ROTATE_LEFT);
                    break;
                case Canvas.DOWN:
                    brickTransition(DROP);
                    break;
                case Canvas.LEFT:
                    brickTransition(LEFT);
                    break;
                case Canvas.RIGHT:
                    brickTransition(RIGHT);
                    break;
            }
        } else {
            switch(keyCode) {
                case Canvas.KEY_NUM1:
                    brickTransition(ROTATE_LEFT);
                    break;
                case Canvas.KEY_NUM3:
                    brickTransition(ROTATE_RIGHT);
                    break;
                case Canvas.KEY_NUM4:
                    brickTransition(LEFT);
                    break;
                case Canvas.KEY_NUM6:
                    brickTransition(RIGHT);
                    break;
                case Canvas.KEY_NUM5:
                    brickTransition(STEP);
                    break;
                case Canvas.KEY_NUM7:
                    brickTransition(DROP);
                    break;
                case -6:
                    break;
                case -7:
                    break;
                case -5:
                    t.showInGameMenu();
                    break;
            }
        }
    }

    public synchronized void brickTimerStep() {
        brickTransition(STEP);
        repaint();
    }

    public synchronized void brickTransition(int type) {
        Brick temp = brick.clone();
        if (type == ROTATE_LEFT) {
            temp.rotate(true);
            if (!brickCollisionCheck(temp)) {
                brick.rotate(true);
            }
        } else if (type == ROTATE_RIGHT) {
            temp.rotate(false);
            if (!brickCollisionCheck(temp)) {
                brick.rotate(false);
            }
        } else if (type == LEFT) {
            temp.left();
            if (!brickCollisionCheck(temp)) {
                brick.left();
            }
        } else if (type == RIGHT) {
            temp.right();
            if (!brickCollisionCheck(temp)) {
                brick.right();
            }
        } else if (type == STEP) {
            temp.step();
            if (!brickCollisionCheck(temp)) {
                brick.step();
            } else {
                brick.step();
                addBrickToRows(brick);
                rowCompleteCheck();
                newBrick();
                if (brickCollisionCheck(brick)) {
                    t.endOfGame();
                }
            }
        } else if (type == DROP) {
            while (!brickCollisionCheck(brick)) {
                brick.step();
            }
            addBrickToRows(brick);
            rowCompleteCheck();
            newBrick();
            if (brickCollisionCheck(brick)) {
                t.endOfGame();
            }
        }
        temp = null;
        repaint();
    }

    public void newBrick() {
        brick = new Brick(Math.abs((int) rand.nextLong() % 7));
        brick.setPosition(((int) COLS / 2) - 2, 0);
    }

    public void addBrickToRows(Brick b) {
        int x, y;
        for (int i = 0; i < b.blocks.length; i++) {
            x = b.blocks[i].x;
            y = b.blocks[i].y;
            if (y <= 0) {
                return;
            }
            rows[y - 1].blocks[x] = b.blocks[i];
        }
    }

    public boolean brickCollisionCheck(Brick b) {
        int i, y, x;
        for (i = 0; i < b.blocks.length; i++) {
            y = b.blocks[i].y;
            x = b.blocks[i].x;
            if (y >= ROWS) {
                return true;
            }
            if (x < 0 || x >= COLS) {
                return true;
            }
            if (rows[y].blocks[x] != null) {
                return true;
            }
        }
        return false;
    }

    public void rowCompleteCheck() {
        int count = 0;
        for (int y = 0; y < rows.length; y++) {
            boolean hasnull = false;
            for (int x = 0; x < rows[y].blocks.length; x++) {
                if (rows[y].blocks[x] == null) {
                    hasnull = true;
                    break;
                }
            }
            if (!hasnull) {
                System.err.println("Row " + y + " complete!");
                for (int i = y; i > 0; i--) {
                    rows[i] = rows[i - 1];
                }
                rows[0] = new Row(0);
                count++;
            }
        }
        score.addLines(count);
        switch(count) {
            case 4:
                score.addScore(1200);
                break;
            case 3:
                score.addScore(300);
                break;
            case 2:
                score.addScore(100);
                break;
            case 1:
                score.addScore(40);
                break;
        }
        if (count > 1) {
            t.multiRowCompleted(count);
        }
    }

    public void paint_old(Graphics g) {
        int areaXoffset = 0;
        int areaYoffset = 0;
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());
        areaHeight = getHeight();
        if (areaHeight % ROWS != 0) {
            areaHeight -= areaHeight % ROWS;
        }
        blockWidth = blockHeight = areaHeight / ROWS;
        areaWidth = COLS * blockWidth;
        g.setColor(0xFFffFF);
        g.fillRect(areaXoffset, areaYoffset, areaWidth, areaHeight);
        if (!showLost && !showWon) {
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].blocks.length; x++) {
                    if (rows[y].blocks[x] == null) {
                        continue;
                    }
                    int color = rows[y].blocks[x].color;
                    int xpos = rows[y].blocks[x].x;
                    int ypos = rows[y].blocks[x].y;
                    g.setColor(color);
                    g.fillRect(areaXoffset + x * blockWidth, areaYoffset + y * blockHeight, blockWidth, blockHeight);
                }
            }
            for (int i = 0; i < brick.blocks.length; i++) {
                int color = brick.blocks[i].color;
                int x = brick.blocks[i].x;
                int y = brick.blocks[i].y;
                g.setColor(color);
                g.fillRect(areaXoffset + x * blockWidth, areaYoffset + y * blockHeight, blockWidth, blockHeight);
            }
        } else if (showLost) {
            g.setColor(0x000000);
            g.drawString("Verloren!", areaXoffset + blockWidth * (COLS / 2), areaYoffset + blockHeight * (ROWS / 2), Graphics.TOP | Graphics.LEFT);
        } else if (showWon) {
            g.setColor(0x000000);
            g.drawString("Gewonnen!", areaXoffset + blockWidth * (COLS / 2), areaYoffset + blockHeight * (ROWS / 2), Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0x999999);
        for (int i = 0; i < COLS; i++) {
            g.drawLine(areaXoffset + i * blockWidth, areaYoffset, areaXoffset + i * blockWidth, areaYoffset + areaHeight);
        }
        for (int i = 0; i < ROWS; i++) {
            g.drawLine(areaXoffset, areaYoffset + i * blockHeight, areaXoffset + areaWidth, areaYoffset + i * blockWidth);
        }
    }

    public void paint(Graphics g) {
        int areaXoffset = 0;
        int areaYoffset = 0;
        int scoreWidth, scoreHeight;
        int scoreXoffset;
        int scoreYoffset = 0;
        g.setColor(0x000000);
        g.fillRect(0, 0, getWidth(), getHeight());
        areaWidth = (getWidth() / 3) * 2;
        areaHeight = getHeight();
        System.err.println("Height: " + getHeight());
        int test_width, test_height;
        int block_width, block_height;
        test_width = areaWidth;
        block_width = test_width / COLS;
        block_height = block_width;
        test_height = block_height * ROWS;
        System.out.println("h: " + test_height + " w: " + test_width + " b: " + block_height);
        if (test_height > areaHeight) {
            test_height = areaHeight;
            block_height = test_height / ROWS;
            block_width = block_height;
            test_width = block_width * COLS;
            System.out.println("h: " + test_height + " w: " + test_width + " b: " + block_height);
        }
        System.out.println("h: " + test_height + " w: " + test_width + " b: " + block_height);
        areaHeight = block_height * ROWS;
        areaWidth = block_width * COLS;
        ;
        blockHeight = block_height;
        blockWidth = block_width;
        System.err.println(areaWidth + "x" + areaHeight);
        System.err.println(blockWidth + "x" + blockHeight);
        scoreXoffset = areaWidth;
        scoreWidth = getWidth() / 3;
        scoreHeight = areaHeight;
        areaYoffset = (getHeight() - areaHeight) / 2;
        scoreYoffset = areaYoffset;
        g.setColor(0xFFffFF);
        g.fillRect(areaXoffset, areaYoffset, areaWidth, areaHeight);
        g.setColor(0xFFffFF);
        g.fillRect(scoreXoffset, scoreYoffset, scoreWidth, scoreHeight);
        if (!showLost && !showWon) {
            for (int y = 0; y < rows.length; y++) {
                for (int x = 0; x < rows[y].blocks.length; x++) {
                    if (rows[y].blocks[x] == null) {
                        continue;
                    }
                    int color = rows[y].blocks[x].color;
                    int xpos = rows[y].blocks[x].x;
                    int ypos = rows[y].blocks[x].y;
                    g.setColor(color);
                    g.fillRect(areaXoffset + x * blockWidth, areaYoffset + y * blockHeight, blockWidth, blockHeight);
                }
            }
            for (int i = 0; i < brick.blocks.length; i++) {
                int color = brick.blocks[i].color;
                int x = brick.blocks[i].x;
                int y = brick.blocks[i].y;
                g.setColor(color);
                g.fillRect(areaXoffset + x * blockWidth, areaYoffset + y * blockHeight, blockWidth, blockHeight);
            }
        }
        g.setColor(0x999999);
        for (int i = 0; i <= COLS; i++) {
            g.drawLine(areaXoffset + i * blockWidth, areaYoffset, areaXoffset + i * blockWidth, areaYoffset + areaHeight);
        }
        for (int i = 0; i < ROWS; i++) {
            g.drawLine(areaXoffset, areaYoffset + i * blockHeight, areaXoffset + areaWidth, areaYoffset + i * blockHeight);
        }
        int fontAnchorX = areaXoffset + blockWidth * (COLS / 2);
        int fontAnchorY = areaYoffset + blockHeight * (ROWS / 2);
        if (showLost) {
            drawCenteredTextBox(g, fontAnchorX, fontAnchorY, "Verloren!");
        } else if (showWon) {
            drawCenteredTextBox(g, fontAnchorX, fontAnchorY, "Gewonnen!");
        }
        int fontHeight;
        fontHeight = g.getFont().getBaselinePosition();
        if (fontHeight <= 2) {
            fontHeight = g.getFont().getHeight();
        }
        g.setColor(0x000000);
        g.drawString(" Punkte: ", scoreXoffset, scoreYoffset, Graphics.TOP | Graphics.LEFT);
        g.drawString(" " + String.valueOf(score.getScore()), scoreXoffset, scoreYoffset + fontHeight, Graphics.TOP | Graphics.LEFT);
        g.drawString(" Lines: ", scoreXoffset, scoreYoffset + 40, Graphics.TOP | Graphics.LEFT);
        g.drawString(" " + String.valueOf(score.getLines()), scoreXoffset, scoreYoffset + 40 + fontHeight, Graphics.TOP | Graphics.LEFT);
    }

    public void commandAction(Command c, Displayable d) {
    }

    public void showLost() {
        showLost = true;
        repaint();
    }

    public void showWon() {
        showWon = true;
        repaint();
    }

    public void addRandomRows(int count) {
        Row r;
        for (int i = 0; i < count; i++) {
            r = new Row(ROWS - 1);
            for (int z = 0; z < r.blocks.length; z++) {
                if (Math.abs((int) rand.nextLong() % 6) != 0) {
                    int color = Brick.colors[Math.abs((int) rand.nextLong() % 7)];
                    r.blocks[z] = new Block(color, z, ROWS - 1);
                }
            }
            for (int y = 0; y < ROWS - 1; y++) {
                rows[y] = rows[y + 1];
            }
            rows[ROWS - 1] = r;
        }
        repaint();
    }

    public void drawCenteredTextBox(Graphics g, int fontAnchorX, int fontAnchorY, String s) {
        Font f = g.getFont();
        int fontHeight;
        fontHeight = f.getBaselinePosition();
        if (fontHeight <= 2) {
            fontHeight = f.getHeight();
        }
        g.setColor(0xFFFFFF);
        g.fillRect(fontAnchorX - f.stringWidth(s) / 2 - 5, fontAnchorY - 5, f.stringWidth(s) + 10, fontHeight + 10);
        g.setColor(0x999999);
        g.drawRect(fontAnchorX - f.stringWidth(s) / 2 - 5, fontAnchorY - 5, f.stringWidth(s) + 10, fontHeight + 10);
        g.setColor(0x000000);
        g.drawString(s, fontAnchorX, fontAnchorY, Graphics.TOP | Graphics.HCENTER);
    }

    public void bluetoothKeepAlive() {
        t.bluetoothKeepAlive();
    }
}

package cn.the.angry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javazoom.jl.player.Player;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;
import cn.the.angry.model.Actor;
import cn.the.angry.model.Bird;
import cn.the.angry.model.CircleBaseElement;
import cn.the.angry.model.Ground;
import cn.the.angry.model.Pig;
import cn.the.angry.model.RectBaseElement;
import cn.the.angry.resourcemanager.ResourceManager;
import cn.the.angry.sprite.Sprite;
import cn.the.angry.tests.AbstractTest;
import cn.the.angry.util.AngryPigDebugDraw;

public class MainTest extends AbstractTest {

    private static final float targetFPS = 60.0f;

    private final int fpsAverageCount = 100;

    private final long[] nanos;

    private final long nanoStart;

    private long frameCount = 0;

    private final long frameRatePeriod;

    private final AABB worldAABB;

    private final World mWorld;

    private final Pig[] pigs;

    private final int PIG_TOTAL_COUNT = 3;

    private int launch_pig = 0;

    private final Bird[] birds;

    private final int BIRD_TOTAL_COUNT = 5;

    private final RectBaseElement RectWood1;

    private final CircleBaseElement CircleWood2;

    private final RectBaseElement BattenWood3;

    private final RectBaseElement BattenWood4;

    private final RectBaseElement BattenWood5;

    private final RectBaseElement RectWood6;

    private final CircleBaseElement CircleWood7;

    private enum LauncherState {

        READY, BEFORE_LAUNCH, AFTER_LAUNCH, UNAVAILABLE, GAMEOVER_VICTORY, GAMEOVER_FAIL
    }

    ;

    private LauncherState state = LauncherState.READY;

    private final Point ORIBALLPOSTION = new Point(200, HEIGHT - 285);

    private final Point ROPEPOINT1 = new Point(ORIBALLPOSTION.x + 15, ORIBALLPOSTION.y);

    private final Point ROPEPOINT2 = new Point(ORIBALLPOSTION.x - 5, ORIBALLPOSTION.y);

    private final float GROUND_HEIGHT = HEIGHT - 200;

    private final AngryPigDebugDraw debugDraw;

    private final List<Vec2> smears = new ArrayList<Vec2>();

    private final Sprite movingCloudSprite;

    private final Object lock = new Object();

    private final int[] elementCount = new int[3];

    private final int PIG_COUNT_OFFSET = 0;

    private final int PIG_ALIVE_COUNT_OFFSET = 1;

    private final int BIRD_COUNT_OFFSET = 2;

    {
        elementCount[PIG_COUNT_OFFSET] = PIG_TOTAL_COUNT;
        elementCount[PIG_ALIVE_COUNT_OFFSET] = PIG_TOTAL_COUNT;
        elementCount[BIRD_COUNT_OFFSET] = BIRD_TOTAL_COUNT;
    }

    private int score = 0;

    public MainTest() {
        worldAABB = new AABB();
        worldAABB.lowerBound.set(-100, -100);
        worldAABB.upperBound.set(1500, 1000);
        debugDraw = new AngryPigDebugDraw(WIDTH, HEIGHT, 1.0f);
        mWorld = new World(worldAABB, new Vec2(0f, 40f), true);
        mWorld.setContactListener(new ConcreteContactListener());
        pigs = new Pig[PIG_TOTAL_COUNT];
        movingCloudSprite = new Sprite(ResourceManager.getMovingCloudImage());
        for (int i = 0; i < pigs.length; ++i) {
            pigs[i] = new Pig();
            pigs[i].setScore(5000);
        }
        pigs[0].getSprite().setCenterX(ORIBALLPOSTION.x);
        pigs[0].getSprite().setCenterY(ORIBALLPOSTION.y);
        pigs[1].getSprite().setCenterX(150);
        pigs[1].getSprite().setCenterY(590);
        pigs[2].getSprite().setCenterX(100);
        pigs[2].getSprite().setCenterY(590);
        birds = new Bird[BIRD_TOTAL_COUNT];
        birds[0] = new Bird();
        birds[0].createBird(mWorld, 770, GROUND_HEIGHT - birds[0].getSprite().getHeight() / 2, 2f, 0.8f, 0.2f);
        birds[1] = new Bird();
        birds[1].createBird(mWorld, 800, GROUND_HEIGHT - birds[0].getSprite().getHeight() / 2, 2f, 0.8f, 0.2f);
        birds[2] = new Bird();
        birds[2].createBird(mWorld, 830, GROUND_HEIGHT - birds[0].getSprite().getHeight() / 2, 2f, 0.8f, 0.2f);
        birds[3] = new Bird();
        birds[3].createBird(mWorld, 860, GROUND_HEIGHT - birds[0].getSprite().getHeight() / 2, 2f, 0.8f, 0.2f);
        birds[4] = new Bird();
        birds[4].createBird(mWorld, 890, GROUND_HEIGHT - birds[0].getSprite().getHeight() / 2, 2f, 0.8f, 0.2f);
        RectWood1 = new RectBaseElement(ResourceManager.getLargeWoodRectangleImages());
        RectWood1.create(mWorld, 700, 580);
        RectWood1.setScore(500);
        CircleWood2 = new CircleBaseElement(ResourceManager.getMiddleWoodBallImages());
        CircleWood2.create(mWorld, 720, 570, 2f, 0.8f, 0.2f, 10f);
        CircleWood2.setScore(300);
        BattenWood3 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood3.create(mWorld, 900, 575, MathUtils.PI / 2);
        BattenWood3.setScore(100);
        BattenWood4 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood4.create(mWorld, 930, 575, MathUtils.PI / 2);
        BattenWood4.setScore(100);
        BattenWood5 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood5.create(mWorld, 915, 550);
        BattenWood5.setScore(100);
        RectWood6 = new RectBaseElement(ResourceManager.getMiddleWoodRectangleImages());
        RectWood6.create(mWorld, 925, 540);
        RectWood6.setScore(300);
        CircleWood7 = new CircleBaseElement(ResourceManager.getLargeWoodBallImages());
        CircleWood7.create(mWorld, 935, 520, 2.0f, 0.8f, 0.2f, 18f);
        CircleWood7.setScore(500);
        new Ground(mWorld, 0, GROUND_HEIGHT, WIDTH, HEIGHT);
        nanos = new long[fpsAverageCount];
        long nanosPerFrameGuess = (long) (1000000000.0 / targetFPS);
        nanos[fpsAverageCount - 1] = System.nanoTime();
        for (int i = fpsAverageCount - 2; i >= 0; --i) {
            nanos[i] = nanos[i + 1] - nanosPerFrameGuess;
        }
        nanoStart = System.nanoTime();
        frameRatePeriod = (long) (1000000000.0 / targetFPS);
        add(panel);
    }

    private final JPanel panel = new JPanel() {

        private Graphics2D g2;

        @Override
        public void update(Graphics g) {
            paint(g);
        }

        int i = 0;

        @Override
        public void paint(Graphics g) {
            g2 = (Graphics2D) g;
            g2.drawImage(backgroundBufferImage, 0, 0, null);
            debugDraw.setGraphics(g2);
            Pig launchPig = null;
            if (state != LauncherState.UNAVAILABLE) launchPig = pigs[launch_pig]; else {
                if (launch_pig + 1 < PIG_TOTAL_COUNT) {
                    launchPig = pigs[launch_pig + 1];
                    final int x = ORIBALLPOSTION.x - launchPig.getSprite().getCenterX();
                    final int y = ORIBALLPOSTION.y - launchPig.getSprite().getCenterY();
                    final int z = (int) Math.sqrt(x * x + y * y);
                    launchPig.getSprite().setCenterX(launchPig.getSprite().getCenterX() + 5 * x / z);
                    launchPig.getSprite().setCenterY(launchPig.getSprite().getCenterY() + 5 * y / z);
                    if (launchPig.getSprite().getCenterX() >= ORIBALLPOSTION.x || launchPig.getSprite().getCenterY() <= ORIBALLPOSTION.y) {
                        state = LauncherState.READY;
                        launchPig.getSprite().setCenterX(ORIBALLPOSTION.x);
                        launchPig.getSprite().setCenterY(ORIBALLPOSTION.y);
                        ++launch_pig;
                    }
                }
            }
            if (launchPig != null && (state == LauncherState.BEFORE_LAUNCH || state == LauncherState.READY)) {
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(Color.BLACK);
                g2.drawLine(ROPEPOINT1.x, ROPEPOINT1.y, launchPig.getSprite().getCenterX(), launchPig.getSprite().getCenterY());
            }
            for (Pig pig : pigs) {
                pig.draw(g2);
            }
            for (final Bird bird : birds) {
                bird.draw(g2);
            }
            RectWood1.draw(g2);
            CircleWood2.draw(g2);
            BattenWood3.draw(g2);
            BattenWood4.draw(g2);
            BattenWood5.draw(g2);
            RectWood6.draw(g2);
            CircleWood7.draw(g2);
            synchronized (lock) {
                int size = smears.size();
                for (int i = 0; i < size; i++) {
                    for (Pig pig : pigs) {
                        if (pig.isBinding()) Sprite.draw(g2, movingCloudSprite.getCurFrame(), (int) smears.get(i).x, (int) smears.get(i).y);
                    }
                }
            }
            if (launchPig != null && (state == LauncherState.BEFORE_LAUNCH || state == LauncherState.READY)) {
                g2.drawLine(ROPEPOINT2.x, ROPEPOINT2.y, launchPig.getSprite().getCenterX(), launchPig.getSprite().getCenterY());
            }
            for (int i = 0; i < fpsAverageCount - 1; ++i) {
                nanos[i] = nanos[i + 1];
            }
            nanos[fpsAverageCount - 1] = System.nanoTime();
            float averagedFPS = (float) ((fpsAverageCount - 1) * 1000000000.0 / (nanos[fpsAverageCount - 1] - nanos[0]));
            ++frameCount;
            float totalFPS = (float) (frameCount * 1000000000 / (1.0 * (System.nanoTime() - nanoStart)));
            g2.drawString("100 Average FPS is " + averagedFPS, 15, 15);
            g2.drawString("Entire FPS is " + totalFPS, 15, 25);
            g2.drawString("Score: " + score, 15, 35);
            g2.drawImage(foregroundBufferImage, 0, 0, null);
            if (state == LauncherState.UNAVAILABLE || state == LauncherState.READY) {
                if (state == LauncherState.UNAVAILABLE && elementCount[BIRD_COUNT_OFFSET] > 0 && elementCount[PIG_COUNT_OFFSET] == 0) {
                    gameFail();
                } else if (elementCount[BIRD_COUNT_OFFSET] == 0) {
                    gameWin();
                }
            } else if (state == LauncherState.GAMEOVER_VICTORY || state == LauncherState.GAMEOVER_FAIL) {
                final int OUTER_WIDTH = 640, OUTER_HEIGHT = 480;
                final int INNER_WIDTH = 600, INNER_HEIGHT = 440;
                g2.setColor(new Color(255, 189, 49));
                g2.fillRoundRect((getWidth() - OUTER_WIDTH) / 2, (getHeight() - OUTER_HEIGHT) / 2, OUTER_WIDTH, OUTER_HEIGHT, 30, 30);
                g2.setColor(new Color(100, 209, 184));
                g2.fillRoundRect((getWidth() - INNER_WIDTH) / 2, (getHeight() - INNER_HEIGHT) / 2, INNER_WIDTH, INNER_HEIGHT, 30, 30);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Century Schoolbook L", Font.ITALIC, 50));
                final FontMetrics fm = g.getFontMetrics();
                final String VictoryText = "VICTORY!";
                final String ScoreText = "Score: " + score;
                final String FailText = "GG!";
                int height, width;
                BufferedImage image;
                if (state == LauncherState.GAMEOVER_VICTORY) {
                    height = fm.getHeight();
                    width = fm.stringWidth(VictoryText);
                    g2.drawString(VictoryText, (getWidth() - width) / 2, (getHeight() - height) / 2 - height);
                    width = fm.stringWidth(ScoreText);
                    g2.drawString(ScoreText, (getWidth() - width) / 2, (getHeight() - height) / 2 + height);
                    image = ResourceManager.getPigVictoryImage();
                } else {
                    height = fm.getHeight();
                    width = fm.stringWidth(FailText);
                    g2.drawString(FailText, (getWidth() - width) / 2, (getHeight() - height) / 2);
                    image = ResourceManager.getPigFailedImage();
                }
                g2.drawImage(image, (getWidth() - image.getWidth()) / 2, (getHeight() - height) / 2 + height + height, null);
            }
            g2.dispose();
        }
    };

    private final Runnable update = new Runnable() {

        long beforeTime;

        long afterTime;

        long overSleepTime = 0L;

        int i = 0;

        @Override
        public void run() {
            while (true) {
                long timeDiff = afterTime - beforeTime;
                long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;
                beforeTime = System.nanoTime();
                overSleepTime = 0L;
                mWorld.step(1.0f / 60f, 10);
                if (i++ % 2 == 0) panel.repaint();
                afterTime = System.nanoTime();
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
                    } catch (InterruptedException ex) {
                    }
                    overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
                }
            }
        }
    };

    public void startTest() {
        new Thread(update).start();
    }

    public void mouseOnPigAndDown(final MouseEvent e) {
        if (state == LauncherState.READY && pigs[launch_pig].getSprite().isCoordinateOnSprite(e.getX(), e.getY())) state = LauncherState.BEFORE_LAUNCH;
    }

    public void mouseOnPigAndUp(final MouseEvent e) {
        if (state == LauncherState.BEFORE_LAUNCH) {
            state = LauncherState.AFTER_LAUNCH;
            --elementCount[PIG_ALIVE_COUNT_OFFSET];
            final Pig pig = pigs[launch_pig];
            final int w = pig.getSprite().getWidth();
            final int h = pig.getSprite().getHeight();
            pig.createPig(mWorld, pig.getSprite().getX() + w / 2, pig.getSprite().getY() + h / 2, 2f, 8f, 0.1f, 50f);
            pig.getBody().setLinearVelocity(new Vec2((ORIBALLPOSTION.x - pig.getSprite().getCenterX()) * 150, (ORIBALLPOSTION.y - pig.getSprite().getCenterY()) * 150));
            new Thread() {

                @Override
                public void run() {
                    synchronized (lock) {
                        smears.clear();
                    }
                    int lastVecId = -1;
                    while (true) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        final int vx = (int) Math.abs(pig.getBody().getLinearVelocity().x);
                        final int vy = (int) Math.abs(pig.getBody().getLinearVelocity().y);
                        final float x = pig.getSprite().getX();
                        final float y = pig.getSprite().getY();
                        Vec2 lastVec = null;
                        if (lastVecId >= 0 && lastVecId < smears.size()) {
                            lastVec = smears.get(lastVecId);
                        }
                        if ((vx > 20 || vy > 20) && (lastVec == null || (lastVec != null && (x - lastVec.x) * (x - lastVec.x) + (y - lastVec.y) * (y - lastVec.y) > 1000))) {
                            lastVecId = smears.size();
                            smears.add(new Vec2(x, y));
                        }
                    }
                }

                ;
            }.start();
            new Thread() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    state = LauncherState.UNAVAILABLE;
                }

                ;
            }.start();
            new Thread() {

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        final Vec2 v = pig.getBody().getLinearVelocity();
                        if (Math.abs(v.x) < 1 && Math.abs(v.y) < 1) {
                            mWorld.destroyBody(pig.getBody());
                            pig.startBomb();
                            pig.kill();
                            --elementCount[PIG_COUNT_OFFSET];
                            return;
                        }
                    }
                }

                ;
            }.start();
        }
    }

    public void mouseOnPidAndDrag(final MouseEvent e) {
        if (state == LauncherState.BEFORE_LAUNCH) {
            int radius = 50;
            final Pig pig = pigs[launch_pig];
            int x = e.getX() - pig.getSprite().getWidth() / 2;
            int y = e.getY() - pig.getSprite().getHeight() / 2;
            final int z = (int) Math.sqrt(Math.pow(e.getX() - ORIBALLPOSTION.x, 2) + Math.pow(e.getY() - ORIBALLPOSTION.y, 2));
            if (Math.abs(x - ORIBALLPOSTION.x) < 100 && ORIBALLPOSTION.y < y) {
                radius = 15;
            } else radius = 50;
            if (z > radius) {
                x = radius * (x - ORIBALLPOSTION.x) / z + ORIBALLPOSTION.x;
                y = radius * (y - ORIBALLPOSTION.y) / z + ORIBALLPOSTION.y;
            }
            pig.getSprite().setCenterX(x);
            pig.getSprite().setCenterY(y);
        }
    }

    public static void bindMouseEvents(final MainTest test) {
        test.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(final MouseEvent e) {
                test.mouseOnPigAndDown(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                test.mouseOnPigAndUp(e);
            }
        });
        test.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(final MouseEvent e) {
                test.mouseOnPidAndDrag(e);
            }
        });
    }

    class ConcreteContactListener implements ContactListener {

        @Override
        public void add(ContactPoint point) {
            final int demageHp = (int) Math.sqrt((point.velocity.x * point.velocity.x) + (point.velocity.y * point.velocity.y));
            if (point.shape1.getBody().getUserData() != null && point.shape1.getBody().getUserData() instanceof Actor) {
                final Actor actor1 = ((Actor) point.shape1.getBody().getUserData());
                int realDemageHP;
                if ((realDemageHP = actor1.damage(demageHp)) > 0 && state != LauncherState.GAMEOVER_VICTORY) {
                    score += actor1.getScore() * realDemageHP / actor1.getFullHp();
                }
                if (actor1 instanceof Bird && ((Bird) actor1).isDead()) {
                    --elementCount[BIRD_COUNT_OFFSET];
                    point.shape1.getBody().setUserData(null);
                }
            }
            if (point.shape2.getBody().getUserData() != null && point.shape2.getBody().getUserData() instanceof Actor) {
                final Actor actor2 = ((Actor) point.shape2.getBody().getUserData());
                int realDemageHP;
                if ((realDemageHP = actor2.damage(demageHp)) > 0 && state != LauncherState.GAMEOVER_VICTORY) {
                    score += actor2.getScore() * realDemageHP / actor2.getFullHp();
                }
                if (actor2 instanceof Bird && ((Bird) actor2).isDead()) {
                    --elementCount[BIRD_COUNT_OFFSET];
                    point.shape2.getBody().setUserData(null);
                }
            }
        }

        @Override
        public void persist(ContactPoint point) {
        }

        @Override
        public void remove(ContactPoint point) {
        }

        @Override
        public void result(ContactResult point) {
        }
    }

    public void gameWin() {
        state = LauncherState.GAMEOVER_VICTORY;
        score += elementCount[PIG_ALIVE_COUNT_OFFSET] * 10000;
    }

    public void gameFail() {
        state = LauncherState.GAMEOVER_FAIL;
    }

    public static void main(String[] args) {
        final MainTest main = new MainTest();
        main.setVisible(true);
        bindMouseEvents(main);
        new Thread() {

            @Override
            public void run() {
                main.startTest();
            }
        }.start();
    }

    private static Player thePlayer;

    public static void loopMusic(final String filepath, final int loopTimes) {
        new Thread() {

            @Override
            public void run() {
                try {
                    final File file = new File(System.getProperty("user.dir") + filepath);
                    for (int i = 0; loopTimes == 0 || i < loopTimes; ++i) {
                        if (thePlayer != null) thePlayer.close();
                        final InputStream fileInputStream = new FileInputStream(file);
                        final InputStream inputStream = new BufferedInputStream(fileInputStream);
                        final Player player = new Player(inputStream);
                        thePlayer = player;
                        Thread.sleep(1000);
                        player.play();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ;
        }.start();
    }
}

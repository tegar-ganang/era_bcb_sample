package cn.the.angry.tests;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javazoom.jl.player.advanced.AdvancedPlayer;
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
import cn.the.angry.util.AngryPigDebugDraw;

public class SimulativeTest extends AbstractTest {

    private static final float targetFPS = 60.0f;

    private final int fpsAverageCount = 100;

    private final long[] nanos;

    private final long nanoStart;

    private long frameCount = 0;

    private final long frameRatePeriod;

    private boolean stop = false;

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

    private final RectBaseElement BattenWood7;

    private final CircleBaseElement CircleWood8;

    private enum LauncherState {

        READY, BEFORE_LAUNCH, AFTER_LAUNCH, UNAVAILABLE
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

    private final int[] elementCount = new int[2];

    private final int PIG_COUNT_OFFSET = 0;

    private final int BIRD_COUNT_OFFSET = 1;

    {
        elementCount[PIG_COUNT_OFFSET] = PIG_TOTAL_COUNT;
        elementCount[BIRD_COUNT_OFFSET] = BIRD_TOTAL_COUNT;
    }

    public SimulativeTest() {
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
        CircleWood2 = new CircleBaseElement(ResourceManager.getMiddleWoodBallImages());
        CircleWood2.create(mWorld, 720, 570, 2f, 0.8f, 0.2f, 10f);
        BattenWood3 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood3.create(mWorld, 900, 575, MathUtils.PI / 2);
        BattenWood4 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood4.create(mWorld, 930, 575, MathUtils.PI / 2);
        BattenWood5 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood5.create(mWorld, 915, 550);
        RectWood6 = new RectBaseElement(ResourceManager.getMiddleWoodRectangleImages());
        RectWood6.create(mWorld, 925, 540);
        BattenWood7 = new RectBaseElement(ResourceManager.getSmallWoodSquareImages());
        BattenWood7.create(mWorld, 930, 530);
        CircleWood8 = new CircleBaseElement(ResourceManager.getLargeWoodBallImages());
        CircleWood8.create(mWorld, 933, 510, 2.0f, 0.8f, 0.2f, 18f);
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
            BattenWood7.draw(g2);
            CircleWood8.draw(g2);
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
            g2.drawImage(foregroundBufferImage, 0, 0, null);
            if (state == LauncherState.UNAVAILABLE || state == LauncherState.READY) {
                if (state == LauncherState.UNAVAILABLE && elementCount[BIRD_COUNT_OFFSET] > 0 && elementCount[PIG_COUNT_OFFSET] == 0) {
                    stop = true;
                    new Thread() {

                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "失败", "title", JOptionPane.OK_OPTION);
                        }

                        ;
                    }.start();
                } else if (elementCount[BIRD_COUNT_OFFSET] == 0) {
                    stop = true;
                    new Thread() {

                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "胜利", "title", JOptionPane.OK_OPTION);
                        }

                        ;
                    }.start();
                }
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
                if (!stop) {
                    beforeTime = System.nanoTime();
                    overSleepTime = 0L;
                    mWorld.step(1.0f / 60f, 10);
                    if (i++ % 2 == 0) panel.repaint();
                    afterTime = System.nanoTime();
                }
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

    public static void bindMouseEvents(final SimulativeTest test) {
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
                ((Actor) point.shape1.getBody().getUserData()).damage(demageHp);
                if (point.shape1.getBody().getUserData() instanceof Bird && ((Bird) point.shape1.getBody().getUserData()).isDead()) {
                    --elementCount[BIRD_COUNT_OFFSET];
                    point.shape1.getBody().setUserData(null);
                }
            }
            if (point.shape2.getBody().getUserData() != null && point.shape2.getBody().getUserData() instanceof Actor) {
                ((Actor) point.shape2.getBody().getUserData()).damage(demageHp);
                if (point.shape2.getBody().getUserData() instanceof Bird && ((Bird) point.shape2.getBody().getUserData()).isDead()) {
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

    public static void main(String[] args) {
        final SimulativeTest main = new SimulativeTest();
        main.setVisible(true);
        bindMouseEvents(main);
        new Thread() {

            @Override
            public void run() {
                try {
                } catch (Exception e) {
                }
                main.startTest();
            }
        }.start();
    }

    public static void loopMusic(final String filepath) {
        new Thread() {

            @Override
            public void run() {
                try {
                    final File file = new File(System.getProperty("user.dir") + "/res/raw/title_theme.mp3");
                    while (true) {
                        final InputStream fileInputStream = new FileInputStream(file);
                        final InputStream inputStream = new BufferedInputStream(fileInputStream);
                        final AdvancedPlayer player = new AdvancedPlayer(inputStream);
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

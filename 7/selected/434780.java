package cn.the.angry.tests;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JPanel;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import cn.the.angry.model.Bird;
import cn.the.angry.model.CircleBaseElement;
import cn.the.angry.model.Ground;
import cn.the.angry.model.Pig;
import cn.the.angry.model.RectBaseElement;
import cn.the.angry.resourcemanager.ResourceManager;
import cn.the.angry.util.AngryPigDebugDraw;

public class BranchOfSimulativeTest extends AbstractTest {

    private static final float targetFPS = 60.0f;

    private final int fpsAverageCount = 100;

    private final long[] nanos;

    private final long nanoStart;

    private long frameCount = 0;

    private final long frameRatePeriod;

    private final AABB worldAABB;

    private final World mWorld;

    private final Pig pig1;

    private final Bird[] birds;

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

    public BranchOfSimulativeTest() {
        worldAABB = new AABB();
        worldAABB.lowerBound.set(-100, -100);
        worldAABB.upperBound.set(1500, 1000);
        debugDraw = new AngryPigDebugDraw(WIDTH, HEIGHT, 1.0f);
        mWorld = new World(worldAABB, new Vec2(0f, 40f), true);
        pig1 = new Pig();
        pig1.getSprite().setCenterX(ORIBALLPOSTION.x);
        pig1.getSprite().setCenterY(ORIBALLPOSTION.y);
        birds = new Bird[5];
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
        CircleWood2.create(mWorld, 711, 570, 2f, 0.8f, 0.2f, 10f);
        BattenWood3 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood3.create(mWorld, 900, 575, MathUtils.PI / 2);
        BattenWood4 = new RectBaseElement(ResourceManager.getBombImage());
        BattenWood4.getSprite().setUpdateTime(80);
        BattenWood4.getSprite().setActive(true);
        BattenWood4.create(mWorld, 930, 575, MathUtils.PI / 2);
        BattenWood5 = new RectBaseElement(ResourceManager.getShortWoodBattenImages());
        BattenWood5.create(mWorld, 915, 550);
        RectWood6 = new RectBaseElement(ResourceManager.getMiddleWoodRectangleImages());
        RectWood6.create(mWorld, 925, 540);
        BattenWood7 = new RectBaseElement(ResourceManager.getSmallWoodSquareImages());
        BattenWood7.create(mWorld, 930, 530);
        CircleWood8 = new CircleBaseElement(ResourceManager.getLargeWoodBallImages());
        CircleWood8.create(mWorld, 932, 510, 2.0f, 0.8f, 0.2f, 18f);
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

        @Override
        public void paint(Graphics g) {
            g2 = (Graphics2D) g;
            g2.drawImage(backgroundBufferImage, 0, 0, null);
            debugDraw.setGraphics(g2);
            mWorld.step(1.0f / 60f, 10);
            if (state == LauncherState.BEFORE_LAUNCH || state == LauncherState.READY) {
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(Color.BLACK);
                g2.drawLine(ROPEPOINT1.x, ROPEPOINT1.y, pig1.getSprite().getCenterX(), pig1.getSprite().getCenterY());
            }
            pig1.draw(g2);
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
            if (state == LauncherState.BEFORE_LAUNCH || state == LauncherState.READY) {
                g2.drawLine(ROPEPOINT2.x, ROPEPOINT2.y, pig1.getSprite().getCenterX(), pig1.getSprite().getCenterY());
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
            g2.dispose();
        }
    };

    private final Runnable update = new Runnable() {

        long beforeTime;

        long afterTime;

        long overSleepTime = 0L;

        @Override
        public void run() {
            while (true) {
                long timeDiff = afterTime - beforeTime;
                long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;
                beforeTime = System.nanoTime();
                overSleepTime = 0L;
                panel.repaint();
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
        if (state == LauncherState.READY && pig1.getSprite().isCoordinateOnSprite(e.getX(), e.getY())) state = LauncherState.BEFORE_LAUNCH;
    }

    public void mouseOnPigAndUp(final MouseEvent e) {
        if (state == LauncherState.BEFORE_LAUNCH) {
            state = LauncherState.AFTER_LAUNCH;
            final int w = pig1.getSprite().getWidth();
            final int h = pig1.getSprite().getHeight();
            pig1.createPig(mWorld, pig1.getSprite().getX() + w / 2, pig1.getSprite().getY() + h / 2, 2f, 8f, 0.5f, 50f);
            pig1.getBody().setLinearVelocity(new Vec2((ORIBALLPOSTION.x - pig1.getSprite().getCenterX()) * 150, (ORIBALLPOSTION.y - pig1.getSprite().getCenterY()) * 150));
        }
    }

    public void mouseOnPidAndDrag(final MouseEvent e) {
        if (state == LauncherState.BEFORE_LAUNCH) {
            int radius = 50;
            int x = e.getX() - pig1.getSprite().getWidth() / 2;
            int y = e.getY() - pig1.getSprite().getHeight() / 2;
            final int z = (int) Math.sqrt(Math.pow(e.getX() - ORIBALLPOSTION.x, 2) + Math.pow(e.getY() - ORIBALLPOSTION.y, 2));
            if (Math.abs(x - ORIBALLPOSTION.x) < 100 && ORIBALLPOSTION.y < y) {
                radius = 15;
            } else radius = 50;
            if (z > radius) {
                x = radius * (x - ORIBALLPOSTION.x) / z + ORIBALLPOSTION.x;
                y = radius * (y - ORIBALLPOSTION.y) / z + ORIBALLPOSTION.y;
            }
            pig1.getSprite().setCenterX(x);
            pig1.getSprite().setCenterY(y);
        }
    }

    public static void bindMouseEvents(final BranchOfSimulativeTest test) {
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

    public static void main(String[] args) {
        final BranchOfSimulativeTest main = new BranchOfSimulativeTest();
        main.setVisible(true);
        bindMouseEvents(main);
        new Thread() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
                main.startTest();
            }
        }.start();
    }
}

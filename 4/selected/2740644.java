package asteroids;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/*******************************************************************************
 * Main applet code.
 ******************************************************************************/
public class Asteroids extends JPanel implements Runnable, KeyListener {

    Thread loadThread;

    Thread loopThread;

    static final int DELAY = 50;

    static final int MAX_SHIPS = 3;

    static final int MAX_SHOTS = 6;

    static final int MAX_ROCKS = 10;

    static final int MAX_SCRAP = 20;

    static final int SCRAP_COUNT = 10;

    static final int HYPER_COUNT = 60;

    static final int STORM_PAUSE = 30;

    static final int UFO_PASSES = 3;

    static final int MIN_ROCK_SIDES = 5;

    static final int MAX_ROCK_SIDES = 9;

    static final int MIN_ROCK_SIZE = 20;

    static final int MAX_ROCK_SIZE = 40;

    static final int MIN_ROCK_SPEED = 3;

    static final int MAX_ROCK_SPEED = 12;

    static final int BIG_POINTS = 25;

    static final int SMALL_POINTS = 50;

    static final int UFO_POINTS = 250;

    static final int MISSLE_POINTS = 500;

    static final int NEW_SHIP_POINTS = 5000;

    static final int NEW_UFO_POINTS = 2750;

    Color asteroids_BG = new Color(0, 0, 126);

    Color ship_BG = new Color(255, 0, 0);

    int numStars;

    Point[] stars;

    int score;

    int highScore;

    int newShipScore;

    int newUfoScore;

    boolean loaded = false;

    boolean paused;

    boolean use_Color = true;

    boolean playing;

    boolean sound;

    boolean detail;

    boolean initComplete = false;

    boolean left = false;

    boolean right = false;

    boolean up = false;

    boolean down = false;

    AsteroidsSprite ship;

    AsteroidsSprite ufo;

    AsteroidsSprite missle;

    AsteroidsSprite[] photons = new AsteroidsSprite[MAX_SHOTS];

    AsteroidsSprite[] asteroids = new AsteroidsSprite[MAX_ROCKS];

    AsteroidsSprite[] explosions = new AsteroidsSprite[MAX_SCRAP];

    int shipsLeft;

    int shipCounter;

    int hyperCounter;

    int[] photonCounter = new int[MAX_SHOTS];

    int photonIndex;

    int ufoPassesLeft;

    int ufoCounter;

    int missleCounter;

    boolean[] asteroidIsSmall = new boolean[MAX_ROCKS];

    int asteroidsCounter;

    int asteroidsSpeed;

    int asteroidsLeft;

    int[] explosionCounter = new int[MAX_SCRAP];

    int explosionIndex;

    Clip crashSound;

    Clip explosionSound;

    Clip fireSound;

    Clip missleSound;

    Clip saucerSound;

    Clip thrustersSound;

    Clip warpSound;

    boolean thrustersPlaying;

    boolean saucerPlaying;

    boolean misslePlaying;

    Dimension offDimension;

    Image offImage;

    Graphics2D offGraphics;

    Dimension gameSize;

    Font font = new Font("Helvetica", Font.BOLD, 12);

    FontMetrics fm;

    int fontWidth;

    int fontHeight;

    public Asteroids() {
        String credit = "Asteroids, Copyright 1998 by Mike Hall. ";
        credit = credit + "Updated by Deane Richan 2003. ";
        credit = credit + "Edited slightly by John Edgar 2004";
        System.out.println(credit);
        addKeyListener(this);
    }

    public void init() {
        Dimension d;
        int i;
        d = size();
        AsteroidsSprite.width = d.width;
        AsteroidsSprite.height = d.height;
        numStars = AsteroidsSprite.width * AsteroidsSprite.height / 5000;
        stars = new Point[numStars];
        for (i = 0; i < numStars; i++) {
            stars[i] = new Point((int) (Math.random() * AsteroidsSprite.width), (int) (Math.random() * AsteroidsSprite.height));
        }
        ship = new AsteroidsSprite();
        ship.shape.addPoint(0, -10);
        ship.shape.addPoint(7, 10);
        ship.shape.addPoint(-7, 10);
        for (i = 0; i < MAX_SHOTS; i++) {
            photons[i] = new AsteroidsSprite();
            photons[i].shape.addPoint(1, 1);
            photons[i].shape.addPoint(1, -1);
            photons[i].shape.addPoint(-1, 1);
            photons[i].shape.addPoint(-1, -1);
        }
        ufo = new AsteroidsSprite();
        ufo.shape.addPoint(-15, 0);
        ufo.shape.addPoint(-10, -5);
        ufo.shape.addPoint(-5, -5);
        ufo.shape.addPoint(-5, -9);
        ufo.shape.addPoint(5, -9);
        ufo.shape.addPoint(5, -5);
        ufo.shape.addPoint(10, -5);
        ufo.shape.addPoint(15, 0);
        ufo.shape.addPoint(10, 5);
        ufo.shape.addPoint(-10, 5);
        missle = new AsteroidsSprite();
        missle.shape.addPoint(0, -4);
        missle.shape.addPoint(1, -3);
        missle.shape.addPoint(1, 3);
        missle.shape.addPoint(2, 4);
        missle.shape.addPoint(-2, 4);
        missle.shape.addPoint(-1, 3);
        missle.shape.addPoint(-1, -3);
        for (i = 0; i < MAX_ROCKS; i++) {
            asteroids[i] = new AsteroidsSprite();
        }
        for (i = 0; i < MAX_SCRAP; i++) {
            explosions[i] = new AsteroidsSprite();
        }
        fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
        fontWidth = fm.getMaxAdvance();
        fontHeight = fm.getHeight();
        highScore = 0;
        sound = true;
        detail = true;
        initGame();
        endGame();
        initComplete = true;
    }

    public boolean isPlaying() {
        return playing;
    }

    public Dimension getGameSize() {
        return gameSize;
    }

    public void setGameSize(Dimension size) {
        if (gameSize == size) {
            return;
        }
        if (isPlaying()) {
            pause();
            String msg = "Are you sure you want to end the current game and change the size?";
            int result = JOptionPane.showConfirmDialog(this, msg, "Asteroids", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                pause();
                return;
            }
        }
        gameSize = size;
        setSize(size);
        init();
    }

    @Override
    public Dimension getMinimumSize() {
        return gameSize;
    }

    @Override
    public Dimension getPreferredSize() {
        return gameSize;
    }

    public void initGame() {
        score = 0;
        shipsLeft = MAX_SHIPS;
        asteroidsSpeed = MIN_ROCK_SPEED;
        newShipScore = NEW_SHIP_POINTS;
        newUfoScore = NEW_UFO_POINTS;
        initShip();
        initPhotons();
        stopUfo();
        stopMissle();
        initAsteroids();
        initExplosions();
        playing = true;
        paused = false;
    }

    public void endGame() {
        playing = false;
        stopShip();
        stopUfo();
        stopMissle();
    }

    public void start() {
        if (loopThread == null) {
            loopThread = new Thread(this);
            loopThread.start();
        }
        if (!loaded && loadThread == null) {
            loadThread = new Thread(this);
            loadThread.start();
        }
    }

    public void stop() {
        if (loopThread != null) {
            loopThread.stop();
            loopThread = null;
        }
        if (loadThread != null) {
            loadThread.stop();
            loadThread = null;
        }
    }

    public void run() {
        int i, j;
        long startTime;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        startTime = System.currentTimeMillis();
        if (!loaded && Thread.currentThread() == loadThread) {
            loadSounds();
            loaded = true;
            loadThread.stop();
        }
        while (Thread.currentThread() == loopThread) {
            if (!paused) {
                updateShip();
                updatePhotons();
                updateUfo();
                updateMissle();
                updateAsteroids();
                updateExplosions();
                if (score > highScore) {
                    highScore = score;
                }
                if (score > newShipScore) {
                    newShipScore += NEW_SHIP_POINTS;
                    shipsLeft++;
                }
                if (playing && score > newUfoScore && !ufo.active) {
                    newUfoScore += NEW_UFO_POINTS;
                    ufoPassesLeft = UFO_PASSES;
                    initUfo();
                }
                if (asteroidsLeft <= 0) {
                    if (--asteroidsCounter <= 0) {
                        initAsteroids();
                    }
                }
            }
            repaint();
            try {
                startTime += DELAY;
                Thread.sleep(Math.max(0, startTime - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void loadSounds() {
        System.out.println(Asteroids.class.getClassLoader().getResource("asteroids/crash.au").toString());
        crashSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/crash.au"));
        explosionSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/explosion.au"));
        fireSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/fire.au"));
        missleSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/missle.au"));
        saucerSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/saucer.au"));
        thrustersSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/thrusters.au"));
        warpSound = getAudioClip(Asteroids.class.getClassLoader().getResource("asteroids/warp.au"));
        crashSound.start();
        crashSound.stop();
        explosionSound.start();
        explosionSound.stop();
        fireSound.start();
        fireSound.stop();
        missleSound.start();
        missleSound.stop();
        saucerSound.start();
        saucerSound.stop();
        thrustersSound.start();
        thrustersSound.stop();
        warpSound.start();
        warpSound.stop();
    }

    public Clip getAudioClip(URL url) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(tmp, stream);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            return clip;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }

    public void initShip() {
        ship.active = true;
        ship.angle = 0.0;
        ship.deltaAngle = 0.0;
        ship.currentX = 0.0;
        ship.currentY = 0.0;
        ship.deltaX = 0.0;
        ship.deltaY = 0.0;
        ship.render();
        if (loaded) {
            thrustersSound.stop();
        }
        thrustersPlaying = false;
        hyperCounter = 0;
    }

    public void updateShip() {
        double dx, dy, limit;
        if (!playing) {
            return;
        }
        if (left) {
            ship.angle += Math.PI / 16.0;
            if (ship.angle > 2 * Math.PI) {
                ship.angle -= 2 * Math.PI;
            }
        }
        if (right) {
            ship.angle -= Math.PI / 16.0;
            if (ship.angle < 0) {
                ship.angle += 2 * Math.PI;
            }
        }
        dx = -Math.sin(ship.angle);
        dy = Math.cos(ship.angle);
        limit = 0.8 * MIN_ROCK_SIZE;
        if (up) {
            if (ship.deltaX + dx > -limit && ship.deltaX + dx < limit) {
                ship.deltaX += dx;
            }
            if (ship.deltaY + dy > -limit && ship.deltaY + dy < limit) {
                ship.deltaY += dy;
            }
        }
        if (down) {
            if (ship.deltaX - dx > -limit && ship.deltaX - dx < limit) {
                ship.deltaX -= dx;
            }
            if (ship.deltaY - dy > -limit && ship.deltaY - dy < limit) {
                ship.deltaY -= dy;
            }
        }
        if (ship.active) {
            ship.advance();
            ship.render();
            if (hyperCounter > 0) {
                hyperCounter--;
            }
        } else if (--shipCounter <= 0) {
            if (shipsLeft > 0) {
                initShip();
                hyperCounter = HYPER_COUNT;
            } else {
                endGame();
            }
        }
    }

    public void stopShip() {
        ship.active = false;
        shipCounter = SCRAP_COUNT;
        if (shipsLeft > 0) {
            shipsLeft--;
        }
        if (loaded) {
            thrustersSound.stop();
        }
        thrustersPlaying = false;
    }

    public void initPhotons() {
        int i;
        for (i = 0; i < MAX_SHOTS; i++) {
            photons[i].active = false;
            photonCounter[i] = 0;
        }
        photonIndex = 0;
    }

    public void updatePhotons() {
        int i;
        for (i = 0; i < MAX_SHOTS; i++) {
            if (photons[i].active) {
                photons[i].advance();
                photons[i].render();
                if (--photonCounter[i] < 0) {
                    photons[i].active = false;
                }
            }
        }
    }

    public void initUfo() {
        double temp;
        ufo.active = true;
        ufo.currentX = -AsteroidsSprite.width / 2;
        ufo.currentY = Math.random() * AsteroidsSprite.height;
        ufo.deltaX = MIN_ROCK_SPEED + Math.random() * (MAX_ROCK_SPEED - MIN_ROCK_SPEED);
        if (Math.random() < 0.5) {
            ufo.deltaX = -ufo.deltaX;
            ufo.currentX = AsteroidsSprite.width / 2;
        }
        ufo.deltaY = MIN_ROCK_SPEED + Math.random() * (MAX_ROCK_SPEED - MIN_ROCK_SPEED);
        if (Math.random() < 0.5) {
            ufo.deltaY = -ufo.deltaY;
        }
        ufo.render();
        saucerPlaying = true;
        if (sound) {
            saucerSound.loop(Clip.LOOP_CONTINUOUSLY);
        }
        ufoCounter = (int) Math.floor(AsteroidsSprite.width / Math.abs(ufo.deltaX));
    }

    public void updateUfo() {
        int i, d;
        if (ufo.active) {
            ufo.advance();
            ufo.render();
            if (--ufoCounter <= 0) {
                if (--ufoPassesLeft > 0) {
                    initUfo();
                } else {
                    stopUfo();
                }
            } else {
                for (i = 0; i < MAX_SHOTS; i++) {
                    if (photons[i].active && ufo.isColliding(photons[i])) {
                        if (sound) {
                            crashSound.setMicrosecondPosition(0);
                            crashSound.start();
                        }
                        explode(ufo);
                        stopUfo();
                        score += UFO_POINTS;
                    }
                }
                d = (int) Math.max(Math.abs(ufo.currentX - ship.currentX), Math.abs(ufo.currentY - ship.currentY));
                if (ship.active && hyperCounter <= 0 && ufo.active && !missle.active && d > 4 * MAX_ROCK_SIZE && Math.random() < .03) {
                    initMissle();
                }
            }
        }
    }

    public void stopUfo() {
        ufo.active = false;
        ufoCounter = 0;
        ufoPassesLeft = 0;
        if (loaded) {
            saucerSound.stop();
        }
        saucerPlaying = false;
    }

    public void initMissle() {
        missle.active = true;
        missle.angle = 0.0;
        missle.deltaAngle = 0.0;
        missle.currentX = ufo.currentX;
        missle.currentY = ufo.currentY;
        missle.deltaX = 0.0;
        missle.deltaY = 0.0;
        missle.render();
        missleCounter = 3 * Math.max(AsteroidsSprite.width, AsteroidsSprite.height) / MIN_ROCK_SIZE;
        if (sound) {
            missleSound.loop(Clip.LOOP_CONTINUOUSLY);
        }
        misslePlaying = true;
    }

    public void updateMissle() {
        if (missle.active) {
            if (--missleCounter <= 0) {
                stopMissle();
            } else {
                guideMissle();
                missle.advance();
                missle.render();
                for (int i = 0; i < MAX_SHOTS; i++) {
                    if (photons[i].active && missle.isColliding(photons[i])) {
                        if (sound) {
                            crashSound.setMicrosecondPosition(0);
                            crashSound.start();
                        }
                        explode(missle);
                        stopMissle();
                        score += MISSLE_POINTS;
                    }
                }
                if (missle.active && ship.active && hyperCounter <= 0 && ship.isColliding(missle)) {
                    if (sound) {
                        crashSound.setMicrosecondPosition(0);
                        crashSound.start();
                    }
                    explode(ship);
                    stopShip();
                    stopUfo();
                    stopMissle();
                }
            }
        }
    }

    public void guideMissle() {
        double dx, dy, angle;
        if (!ship.active || hyperCounter > 0) {
            return;
        }
        dx = ship.currentX - missle.currentX;
        dy = ship.currentY - missle.currentY;
        if (dx == 0 && dy == 0) {
            angle = 0;
        }
        if (dx == 0) {
            if (dy < 0) {
                angle = -Math.PI / 2;
            } else {
                angle = Math.PI / 2;
            }
        } else {
            angle = Math.atan(Math.abs(dy / dx));
            if (dy > 0) {
                angle = -angle;
            }
            if (dx < 0) {
                angle = Math.PI - angle;
            }
        }
        missle.angle = angle - Math.PI / 2;
        missle.deltaX = MIN_ROCK_SIZE / 3 * -Math.sin(missle.angle);
        missle.deltaY = MIN_ROCK_SIZE / 3 * Math.cos(missle.angle);
    }

    public void stopMissle() {
        missle.active = false;
        missleCounter = 0;
        if (loaded) {
            missleSound.stop();
        }
        misslePlaying = false;
    }

    public void initAsteroids() {
        int i, j;
        int s;
        double theta, r;
        int x, y;
        for (i = 0; i < MAX_ROCKS; i++) {
            asteroids[i].shape = new Polygon();
            s = MIN_ROCK_SIDES + (int) (Math.random() * (MAX_ROCK_SIDES - MIN_ROCK_SIDES));
            for (j = 0; j < s; j++) {
                theta = 2 * Math.PI / s * j;
                r = MIN_ROCK_SIZE + (int) (Math.random() * (MAX_ROCK_SIZE - MIN_ROCK_SIZE));
                x = (int) -Math.round(r * Math.sin(theta));
                y = (int) Math.round(r * Math.cos(theta));
                asteroids[i].shape.addPoint(x, y);
            }
            int red = (int) (Math.random() * 255);
            int green = (int) (Math.random() * 255);
            int blue = (int) (Math.random() * 255);
            asteroids[i].colour = new Color(red, green, blue);
            asteroids[i].active = true;
            asteroids[i].angle = 0.0;
            asteroids[i].deltaAngle = (Math.random() - 0.5) / 10;
            if (Math.random() < 0.5) {
                asteroids[i].currentX = -AsteroidsSprite.width / 2;
                if (Math.random() < 0.5) {
                    asteroids[i].currentX = AsteroidsSprite.width / 2;
                }
                asteroids[i].currentY = Math.random() * AsteroidsSprite.height;
            } else {
                asteroids[i].currentX = Math.random() * AsteroidsSprite.width;
                asteroids[i].currentY = -AsteroidsSprite.height / 2;
                if (Math.random() < 0.5) {
                    asteroids[i].currentY = AsteroidsSprite.height / 2;
                }
            }
            asteroids[i].deltaX = Math.random() * asteroidsSpeed;
            if (Math.random() < 0.5) {
                asteroids[i].deltaX = -asteroids[i].deltaX;
            }
            asteroids[i].deltaY = Math.random() * asteroidsSpeed;
            if (Math.random() < 0.5) {
                asteroids[i].deltaY = -asteroids[i].deltaY;
            }
            asteroids[i].render();
            asteroidIsSmall[i] = false;
        }
        asteroidsCounter = STORM_PAUSE;
        asteroidsLeft = MAX_ROCKS;
        if (asteroidsSpeed < MAX_ROCK_SPEED) {
            asteroidsSpeed++;
        }
    }

    public void initSmallAsteroids(int n) {
        int count;
        int i, j;
        int s;
        double tempX, tempY;
        double theta, r;
        int x, y;
        count = 0;
        i = 0;
        tempX = asteroids[n].currentX;
        tempY = asteroids[n].currentY;
        do {
            if (!asteroids[i].active) {
                asteroids[i].shape = new Polygon();
                s = MIN_ROCK_SIDES + (int) (Math.random() * (MAX_ROCK_SIDES - MIN_ROCK_SIDES));
                for (j = 0; j < s; j++) {
                    theta = 2 * Math.PI / s * j;
                    r = (MIN_ROCK_SIZE + (int) (Math.random() * (MAX_ROCK_SIZE - MIN_ROCK_SIZE))) / 2;
                    x = (int) -Math.round(r * Math.sin(theta));
                    y = (int) Math.round(r * Math.cos(theta));
                    asteroids[i].shape.addPoint(x, y);
                }
                asteroids[i].active = true;
                asteroids[i].angle = 0.0;
                asteroids[i].deltaAngle = (Math.random() - 0.5) / 10;
                asteroids[i].currentX = tempX;
                asteroids[i].currentY = tempY;
                asteroids[i].deltaX = Math.random() * 2 * asteroidsSpeed - asteroidsSpeed;
                asteroids[i].deltaY = Math.random() * 2 * asteroidsSpeed - asteroidsSpeed;
                asteroids[i].render();
                asteroidIsSmall[i] = true;
                count++;
                asteroidsLeft++;
            }
            i++;
        } while (i < MAX_ROCKS && count < 2);
    }

    public void updateAsteroids() {
        int i, j;
        for (i = 0; i < MAX_ROCKS; i++) {
            if (asteroids[i].active) {
                asteroids[i].advance();
                asteroids[i].render();
                for (j = 0; j < MAX_SHOTS; j++) {
                    if (photons[j].active && asteroids[i].active && asteroids[i].isColliding(photons[j])) {
                        asteroidsLeft--;
                        asteroids[i].active = false;
                        photons[j].active = false;
                        if (sound) {
                            explosionSound.setMicrosecondPosition(0);
                            explosionSound.start();
                        }
                        explode(asteroids[i]);
                        if (!asteroidIsSmall[i]) {
                            score += BIG_POINTS;
                            initSmallAsteroids(i);
                        } else {
                            score += SMALL_POINTS;
                        }
                    }
                }
                if (ship.active && hyperCounter <= 0 && asteroids[i].active && asteroids[i].isColliding(ship)) {
                    if (sound) {
                        crashSound.setMicrosecondPosition(0);
                        crashSound.start();
                    }
                    explode(ship);
                    stopShip();
                    stopUfo();
                    stopMissle();
                }
            }
        }
    }

    public void initExplosions() {
        int i;
        for (i = 0; i < MAX_SCRAP; i++) {
            explosions[i].shape = new Polygon();
            explosions[i].active = false;
            explosionCounter[i] = 0;
        }
        explosionIndex = 0;
    }

    public void explode(AsteroidsSprite s) {
        s.render();
        int c = 2;
        if (detail || s.sprite.npoints < 6) {
            c = 1;
        }
        for (int i = 0; i < s.sprite.npoints; i += c) {
            explosionIndex++;
            if (explosionIndex >= MAX_SCRAP) {
                explosionIndex = 0;
            }
            explosions[explosionIndex].active = true;
            explosions[explosionIndex].shape = new Polygon();
            explosions[explosionIndex].shape.addPoint(s.shape.xpoints[i], s.shape.ypoints[i]);
            int j = i + 1;
            if (j >= s.sprite.npoints) {
                j -= s.sprite.npoints;
            }
            explosions[explosionIndex].shape.addPoint(s.shape.xpoints[j], s.shape.ypoints[j]);
            explosions[explosionIndex].angle = s.angle;
            explosions[explosionIndex].deltaAngle = (Math.random() * 2 * Math.PI - Math.PI) / 15;
            explosions[explosionIndex].currentX = s.currentX;
            explosions[explosionIndex].currentY = s.currentY;
            explosions[explosionIndex].deltaX = -s.shape.xpoints[i] / 5;
            explosions[explosionIndex].deltaY = -s.shape.ypoints[i] / 5;
            explosionCounter[explosionIndex] = SCRAP_COUNT;
        }
    }

    public void updateExplosions() {
        for (int i = 0; i < MAX_SCRAP; i++) {
            if (explosions[i].active) {
                explosions[i].advance();
                explosions[i].render();
                if (--explosionCounter[i] < 0) {
                    explosions[i].active = false;
                }
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        if (initComplete) {
            update(g);
        }
    }

    @Override
    public void update(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Dimension d = size();
        int i;
        int c;
        String s;
        if (offGraphics == null || d.width != offDimension.width || d.height != offDimension.height) {
            offDimension = d;
            offImage = createImage(d.width, d.height);
            offGraphics = (Graphics2D) offImage.getGraphics();
        }
        offGraphics.setColor(Color.black);
        offGraphics.fillRect(0, 0, d.width, d.height);
        if (detail) {
            offGraphics.setColor(Color.white);
            for (i = 0; i < numStars; i++) {
                offGraphics.drawLine(stars[i].x, stars[i].y, stars[i].x, stars[i].y);
            }
        }
        offGraphics.setColor(Color.white);
        for (i = 0; i < MAX_SHOTS; i++) {
            if (photons[i].active) {
                offGraphics.drawPolygon(photons[i].sprite);
            }
        }
        c = Math.min(missleCounter * 24, 255);
        offGraphics.setColor(new Color(c, c, c));
        if (missle.active) {
            offGraphics.drawPolygon(missle.sprite);
            offGraphics.drawLine(missle.sprite.xpoints[missle.sprite.npoints - 1], missle.sprite.ypoints[missle.sprite.npoints - 1], missle.sprite.xpoints[0], missle.sprite.ypoints[0]);
        }
        for (i = 0; i < MAX_ROCKS; i++) {
            if (asteroids[i].active) {
                if (detail) {
                    if (use_Color) {
                        offGraphics.setColor(asteroids[i].colour);
                    } else {
                        offGraphics.setColor(Color.black);
                    }
                    offGraphics.fillPolygon(asteroids[i].sprite);
                }
                offGraphics.setColor(Color.white);
                offGraphics.drawPolygon(asteroids[i].sprite);
                offGraphics.drawLine(asteroids[i].sprite.xpoints[asteroids[i].sprite.npoints - 1], asteroids[i].sprite.ypoints[asteroids[i].sprite.npoints - 1], asteroids[i].sprite.xpoints[0], asteroids[i].sprite.ypoints[0]);
            }
        }
        if (ufo.active) {
            if (detail) {
                offGraphics.setColor(Color.black);
                offGraphics.fillPolygon(ufo.sprite);
            }
            offGraphics.setColor(Color.white);
            offGraphics.drawPolygon(ufo.sprite);
            offGraphics.drawLine(ufo.sprite.xpoints[ufo.sprite.npoints - 1], ufo.sprite.ypoints[ufo.sprite.npoints - 1], ufo.sprite.xpoints[0], ufo.sprite.ypoints[0]);
        }
        c = 255 - (255 / HYPER_COUNT) * hyperCounter;
        if (ship.active) {
            if (detail && hyperCounter == 0) {
                if (use_Color) {
                    offGraphics.setColor(ship_BG);
                } else {
                    offGraphics.setColor(Color.black);
                }
                offGraphics.fillPolygon(ship.sprite);
            }
            offGraphics.setColor(new Color(c, c, c));
            offGraphics.drawPolygon(ship.sprite);
            offGraphics.drawLine(ship.sprite.xpoints[ship.sprite.npoints - 1], ship.sprite.ypoints[ship.sprite.npoints - 1], ship.sprite.xpoints[0], ship.sprite.ypoints[0]);
        }
        for (i = 0; i < MAX_SCRAP; i++) {
            if (explosions[i].active) {
                c = (255 / SCRAP_COUNT) * explosionCounter[i];
                offGraphics.setColor(new Color(c, c, c));
                offGraphics.drawPolygon(explosions[i].sprite);
            }
        }
        offGraphics.setFont(font);
        offGraphics.setColor(Color.white);
        offGraphics.drawString("Score: " + score, fontWidth, fontHeight);
        offGraphics.drawString("Ships: " + shipsLeft, fontWidth, d.height - fontHeight);
        s = "High: " + highScore;
        offGraphics.drawString(s, d.width - (fontWidth + fm.stringWidth(s)), fontHeight);
        if (!sound) {
            s = "Mute";
            offGraphics.drawString(s, d.width - (fontWidth + fm.stringWidth(s)), d.height - fontHeight);
        }
        if (!playing) {
            s = "A S T E R O I D S";
            offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2);
            s = "Copyright 1998 by Mike Hall";
            offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 2 + fontHeight);
            if (!loaded) {
                s = "Loading sounds...";
                offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
            } else {
                s = "Game Over";
                offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
                s = "'S' to Start";
                offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4 + fontHeight);
            }
        } else if (paused) {
            s = "Game Paused";
            offGraphics.drawString(s, (d.width - fm.stringWidth(s)) / 2, d.height / 4);
        }
        g2.drawImage(offImage, 0, 0, this);
    }

    public void toggleUseColor() {
        use_Color = !use_Color;
    }

    public boolean getUseColor() {
        return use_Color;
    }

    public void pause() {
        if (paused) {
            if (sound && misslePlaying) {
                missleSound.loop(Clip.LOOP_CONTINUOUSLY);
            }
            if (sound && saucerPlaying) {
                saucerSound.loop(Clip.LOOP_CONTINUOUSLY);
            }
            if (sound && thrustersPlaying) {
                thrustersSound.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } else {
            if (misslePlaying) {
                missleSound.stop();
            }
            if (saucerPlaying) {
                saucerSound.stop();
            }
            if (thrustersPlaying) {
                thrustersSound.stop();
            }
        }
        paused = !paused;
    }

    /**
	 * Invoked when a key has been pressed. See the class description for
	 * {@link KeyEvent} for a definition of a key pressed event.
	 * 
	 */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) {
        }
        if (key == KeyEvent.VK_LEFT) {
            left = true;
        }
        if (key == KeyEvent.VK_RIGHT) {
            right = true;
        }
        if (key == KeyEvent.VK_UP) {
            up = true;
        }
        if (key == KeyEvent.VK_DOWN) {
            down = true;
        }
        if ((up || down) && ship.active && !thrustersPlaying) {
            if (sound && !paused) {
                thrustersSound.loop(Clip.LOOP_CONTINUOUSLY);
            }
            thrustersPlaying = true;
        }
        if (key == KeyEvent.VK_SPACE && ship.active) {
            if (sound & !paused) {
                fireSound.setMicrosecondPosition(0);
                fireSound.start();
            }
            photonIndex++;
            if (photonIndex >= MAX_SHOTS) {
                photonIndex = 0;
            }
            photons[photonIndex].active = true;
            photons[photonIndex].currentX = ship.currentX;
            photons[photonIndex].currentY = ship.currentY;
            photons[photonIndex].deltaX = MIN_ROCK_SIZE * -Math.sin(ship.angle);
            photons[photonIndex].deltaY = MIN_ROCK_SIZE * Math.cos(ship.angle);
            photonCounter[photonIndex] = Math.min(AsteroidsSprite.width, AsteroidsSprite.height) / MIN_ROCK_SIZE;
        }
        if (key == KeyEvent.VK_H && ship.active && hyperCounter <= 0) {
            ship.currentX = Math.random() * AsteroidsSprite.width;
            ship.currentX = Math.random() * AsteroidsSprite.height;
            hyperCounter = HYPER_COUNT;
            if (sound & !paused) {
                warpSound.setMicrosecondPosition(0);
                warpSound.start();
            }
        }
        if (key == KeyEvent.VK_S && loaded && !playing) {
            initGame();
        }
        if (key == KeyEvent.VK_P) {
            pause();
        }
        if (key == KeyEvent.VK_M && loaded) {
            if (sound) {
                crashSound.stop();
                explosionSound.stop();
                fireSound.stop();
                missleSound.stop();
                saucerSound.stop();
                thrustersSound.stop();
                warpSound.stop();
            } else {
                if (misslePlaying && !paused) {
                    missleSound.loop(Clip.LOOP_CONTINUOUSLY);
                }
                if (saucerPlaying && !paused) {
                    saucerSound.loop(Clip.LOOP_CONTINUOUSLY);
                }
                if (thrustersPlaying && !paused) {
                    thrustersSound.loop(Clip.LOOP_CONTINUOUSLY);
                }
            }
            sound = !sound;
        }
        if (key == KeyEvent.VK_D) {
            detail = !detail;
        }
    }

    /**
	 * Invoked when a key has been released. See the class description for
	 * {@link KeyEvent} for a definition of a key released event.
	 * 
	 */
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            left = false;
        }
        if (key == KeyEvent.VK_RIGHT) {
            right = false;
        }
        if (key == KeyEvent.VK_UP) {
            up = false;
        }
        if (key == KeyEvent.VK_DOWN) {
            down = false;
        }
        if (!up && !down && thrustersPlaying) {
            thrustersSound.stop();
            thrustersPlaying = false;
        }
    }

    /**
	 * Invoked when a key has been typed. See the class description for
	 * {@link KeyEvent} for a definition of a key typed event.
	 * 
	 */
    public void keyTyped(KeyEvent e) {
    }
}

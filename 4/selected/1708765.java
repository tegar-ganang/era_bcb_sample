package org.chernovia.sims.ca.fishbowl.sphere;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.TextArea;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.Material;
import javax.media.j3d.PointArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import org.chernovia.lib.music.midi.JMIDI;
import org.chessworks.common.javatools.io.FileHelper;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.MultiTransformGroup;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class JSphereApp extends Applet {

    static final long serialVersionUID = 1;

    static boolean IS_APPLET = true;

    static MainFrame MF;

    static SimpleUniverse SU;

    static Canvas3D canvas;

    static TextArea StatusTxt;

    static BoundingSphere BallBounds;

    static Ball[] balls;

    static String WELCOME_MSG = "Welcome to JavaSphere 1.27, by John Chernoff (jachern@yahoo.com).  " + FileHelper.EOL + "Hit '?' for help." + FileHelper.EOL + "(note: before pressing any keys, first click inside the animation box)" + FileHelper.EOL;

    static String JSphereDir = "res/jsphere/";

    static URL HELPFILE;

    static String BallTexDir = JSphereDir + "textures/balls";

    static String WallTexDir = JSphereDir + "textures/walls";

    static final Color3f black = new Color3f(0.0f, 0.0f, 0.0f);

    static final Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

    static final Color3f red = new Color3f(1.0f, 0.0f, 0.0f);

    static final Color3f green = new Color3f(0.0f, 1.0f, 0.0f);

    static final Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);

    static final Point3d origin = new Point3d(0, 0, 0);

    static final String[] SPOT_STYLES = { "Dir", "Fav", "Origin" };

    static final String[] LIGHT_STYLES = { "White", "Multi-color", "Primary colors" };

    static final String[] TRACK_STYLES = { "ball -> another ball", "origin -> ball", "behind ball -> ball", "ball -> ball dir" };

    static final int SPHERE = 0, CUBE = 1, MIX = 2, BALLSHAPES = 3;

    static final int SPOT_DIR = 0, SPOT_FAV = 1, SPOT_ORIGIN = 2;

    static final int LIGHT_WHITE = 0, LIGHT_MULTI = 1, LIGHT_PRIMARY = 2;

    static final int NO_BALL_VIEW = -1, TRACK_BALL = 0, TRACK_ORIGIN = 1, TRACK_BACK = 2, TRACK_DIR = 3, MAX_TRACK = 4;

    static final int MAXBALLS = 9;

    static final float BOWLSIZE = 20f, NUMSHARD = BOWLSIZE * 2.5f, NUMDUST = 1000f;

    static final float SPOT_ATT_VAR = .025f, POINT_ATT_VAR = .025f;

    static float SPOT_ATT, SPOT_ATT2, SPOT_ATT3;

    static float POINT_ATT, POINT_ATT2, POINT_ATT3;

    static float MAX_SPOT_ATT, MAX_POINT_ATT;

    static float BALLSIZE, MAX_SIZE, SIZE_VAR;

    static float VIEW_DIST_FACTOR;

    static int MAXTEXTS = 9;

    static int BALLSHAPE, VERTICIES, VERT_VAR, MAX_VERT;

    static int SPEEDVAR, SHARDSIZE;

    static int BALLVIEW, BALLVIEW2, TRACK_STYLE;

    static int PAUSE, PAUSE_VAR, NUMBALLS;

    static int LIGHT_COL, SPOT_SPREAD, SPOT_MAX_SPREAD;

    static int SPOT_CON, SPOT_CON_VAR, SPOT_MAX_CON, SPOT_STYLE;

    static final int MAX_INST = 128, MAX_VOL = 100, COLL_BUFF = 12, MAX_DUR = 64;

    static final int PITCH_BASE = 33, PITCH_RANGE = 72;

    static int[] BALL_SOUNDS = { 0, 0, 0, 0, 0, 0, 0, 0 };

    static int[] collisionMat;

    static boolean RUNNING = false, FULLSCREEN = false, SPOTLIGHT, MULTICOL, FIXED_PITCH, SONIFY, CHORDS, WALL_SOUND, POST_MOVE, COMPLEX_BOUNCE, PITCH_DEBUG, GRAVITY, WALLS, SHARDS, DUST, BLK_BALL, BLK_WALL, AMBIENT_LIGHTS, PAUSED, HOLD_CHORD, FLYING;

    public static void factorySettings() {
        SPOTLIGHT = false;
        MULTICOL = true;
        FIXED_PITCH = true;
        SONIFY = false;
        CHORDS = false;
        WALL_SOUND = false;
        POST_MOVE = false;
        COMPLEX_BOUNCE = false;
        PITCH_DEBUG = false;
        GRAVITY = false;
        WALLS = false;
        SHARDS = false;
        DUST = true;
        BLK_BALL = false;
        BLK_WALL = true;
        AMBIENT_LIGHTS = false;
        PAUSED = false;
        HOLD_CHORD = false;
        FLYING = false;
        SPOT_ATT = .025f;
        SPOT_ATT2 = 0;
        SPOT_ATT3 = 0;
        POINT_ATT = .5f;
        POINT_ATT2 = 0;
        POINT_ATT3 = 0;
        MAX_SPOT_ATT = 2.5f;
        MAX_POINT_ATT = 2.5f;
        BALLSIZE = BOWLSIZE / 20;
        MAX_SIZE = BOWLSIZE / 5;
        SIZE_VAR = BOWLSIZE / 100;
        VIEW_DIST_FACTOR = 2.345f;
        if (IS_APPLET) BALLSHAPE = MIX; else BALLSHAPE = SPHERE;
        SPEEDVAR = 1;
        SHARDSIZE = 5;
        VERTICIES = 64;
        VERT_VAR = 16;
        MAX_VERT = 96;
        BALLVIEW = 0;
        BALLVIEW2 = 1;
        TRACK_STYLE = TRACK_BALL;
        PAUSE = 20;
        PAUSE_VAR = 10;
        NUMBALLS = 6;
        LIGHT_COL = LIGHT_PRIMARY;
        SPOT_SPREAD = 6;
        SPOT_MAX_SPREAD = 64;
        SPOT_CON = 0;
        SPOT_CON_VAR = 10;
        SPOT_MAX_CON = 120;
        SPOT_STYLE = SPOT_DIR;
    }

    public static void main(String[] args) {
        IS_APPLET = false;
        factorySettings();
        parseArgs(args);
        MF = new MainFrame(new JSphereApp(), 600, 400);
        MF.setTitle("JavaSphere");
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-mix")) BALLSHAPE = MIX; else if (args[i].equals("-cubes")) BALLSHAPE = CUBE; else if (args[i].equals("-spheres")) BALLSHAPE = SPHERE; else if (args[i].equals("-mix")) BALLSHAPE = MIX; else if (args[i].equals("-spot")) SPOTLIGHT = true; else if (args[i].equals("-point")) SPOTLIGHT = false; else if (args[i].equals("-walls")) WALLS = true; else if (args[i].equals("-nowalls")) WALLS = false; else if (args[i].equals("-stars")) DUST = true; else if (args[i].equals("-nostars")) DUST = false; else if (args[i].equals("-shards")) SHARDS = true; else if (args[i].equals("-noshards")) SHARDS = false; else if (args[i].equals("-bb")) BLK_BALL = true; else if (args[i].equals("-BB")) BLK_BALL = false; else if (args[i].equals("-bw")) BLK_WALL = true; else if (args[i].equals("-BW")) BLK_WALL = false; else if (args[i].equals("-MB")) {
                SPOT_ATT = .025f;
                POINT_ATT = .025f;
            } else if (args[i].equals("-mb")) {
                SPOT_ATT = 1f;
                POINT_ATT = 1f;
            } else if (args[i].equals("-primary")) {
                LIGHT_COL = LIGHT_PRIMARY;
            } else if (args[i].equals("-multicol")) {
                LIGHT_COL = LIGHT_MULTI;
            } else if (args[i].equals("-white")) {
                LIGHT_COL = LIGHT_WHITE;
            } else if (args[i].equals("-webify")) {
                renameTextures(BallTexDir, "tmp");
                renameTextures(WallTexDir, "tmp");
                renameTextures(BallTexDir, "texture");
                renameTextures(WallTexDir, "texture");
                System.exit(-1);
            } else if (i < args.length - 1) {
                if (args[i].equals("-BT")) BallTexDir = args[++i]; else if (args[i].equals("-WT")) WallTexDir = args[++i]; else usage();
            } else usage();
        }
    }

    private static void usage() {
        System.out.println("Usage: " + FileHelper.EOL + "-cubes : use cubes" + FileHelper.EOL + "-spheres : use spheres" + FileHelper.EOL + "-mix : use cubes and spheres" + FileHelper.EOL + "-spot : use spotlight" + FileHelper.EOL + "-point : use pointlight" + FileHelper.EOL + "-walls : show walls" + FileHelper.EOL + "-nowalls : hide walls" + FileHelper.EOL + "-shards : show shards" + FileHelper.EOL + "-noshards : hide shards" + FileHelper.EOL + "-stars : show dust/stars" + FileHelper.EOL + "-nostars : hide dust/stars" + FileHelper.EOL + "-primary : use primary color lights" + FileHelper.EOL + "-multicol : use multi-colored lights" + FileHelper.EOL + "-white : use white lights" + FileHelper.EOL + "-BB : bright balls" + FileHelper.EOL + "-bb : dark balls" + FileHelper.EOL + "-BW : bright walls/shards/dust" + FileHelper.EOL + "-bw : dark walls/shards/dust" + FileHelper.EOL + "-MB : maximum brightness" + FileHelper.EOL + "-mb : decreased brightness" + FileHelper.EOL + "-BT (dir) : set ball texture directory" + FileHelper.EOL + "-WT (dir) : set wall texture directory");
        System.exit(-1);
    }

    public JSphereApp() {
        balls = new Ball[MAXBALLS];
    }

    public boolean initMidi() {
        collisionMat = new int[MAXBALLS];
        boolean loaded;
        if (!JMIDI.isReady()) loaded = JMIDI.load(); else loaded = true;
        try {
            for (int i = 0; i < MAXBALLS; i++) JMIDI.setChannel(i, BALL_SOUNDS[i]);
        } catch (Exception augh) {
            blurb("MIDI error: make sure you've a soundbank installed!");
        }
        return loaded;
    }

    public BranchGroup createSceneGraph() {
        blurb("Loading scene...", false);
        BranchGroup objRoot = new BranchGroup();
        TransformGroup objScale = new TransformGroup();
        objRoot.addChild(objScale);
        BallBounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), BOWLSIZE * 2);
        for (int f = 0; f < MAXBALLS; f++) {
            Point3d fp = rndPt3d(BOWLSIZE - BALLSIZE);
            balls[f] = new Ball(fp, rndCol(), this);
            balls[f].initMatrix(MAXBALLS);
            if (f < NUMBALLS) {
                objScale.addChild(balls[f].BallTrans);
                objScale.addChild(balls[f].BallLight);
            }
            blurb(".", false);
        }
        blurb(".", false);
        if (AMBIENT_LIGHTS) {
            AmbientLight AL = new AmbientLight(true, rndCol());
            AL.setInfluencingBounds(BallBounds);
            objScale.addChild(AL);
        }
        if (WALLS) {
            TransformGroup[] WallGrp = loadWalls();
            for (int w = 0; w < WallGrp.length; w++) objScale.addChild(WallGrp[w]);
        } else {
            Color3f c = rndCol();
            if (SHARDS) for (int i = 0; i < NUMSHARD; i++) {
                Geometry[] G = getShards(BOWLSIZE, SHARDSIZE);
                if (MULTICOL) c = rndCol();
                objScale.addChild(new Shape3D(G[0], getShardApp(c)));
            }
            if (DUST) for (int i = 0; i < NUMDUST; i++) {
                Geometry G = getDust(BOWLSIZE);
                if (MULTICOL) c = rndCol();
                objScale.addChild(new Shape3D(G, getShardApp(c)));
            }
        }
        blurb(" loaded.");
        return objRoot;
    }

    public void setBallLights() {
        for (int i = 0; i < NUMBALLS; i++) {
            balls[i].setLightProperties();
        }
    }

    public boolean isTracking() {
        return (BALLVIEW >= 0);
    }

    public void track() {
        BALLVIEW = 0;
        BALLVIEW2 = 1;
        FLYING = false;
    }

    public void unTrack() {
        BALLVIEW = -1;
        BALLVIEW2 = -1;
    }

    public void cycleTrackBall() {
        do {
            if (++BALLVIEW2 == NUMBALLS) BALLVIEW2 = 0;
        } while (BALLVIEW == BALLVIEW2);
        blurb("ball #" + BALLVIEW + " tracking ball #" + BALLVIEW2);
    }

    public void cycleViewBall() {
        if (++BALLVIEW == NUMBALLS) BALLVIEW = 0;
        blurb("tracking ball #" + BALLVIEW);
    }

    public void reset() {
        reset(canvas);
    }

    public void reset(Canvas3D c) {
        JMIDI.silence();
        boolean OS = SONIFY;
        SONIFY = false;
        SU.removeAllLocales();
        SU.cleanup();
        SU = new SimpleUniverse(c);
        BranchGroup BG = createSceneGraph();
        BG.compile();
        SU.addBranchGraph(BG);
        unTrack();
        resetView();
        System.gc();
        SONIFY = OS;
    }

    public void start() {
        blurb("Starting...");
        RUNNING = true;
        while (RUNNING) {
            for (int i = 0; i < NUMBALLS; i++) {
                boolean bounced = balls[i].updateDir(balls);
                if (!POST_MOVE) balls[i].move();
                if (SONIFY) {
                    if (bounced) collisionMat[i] += COLL_BUFF; else {
                        if (!COMPLEX_BOUNCE) collisionMat[i] = 0; else if (collisionMat[i] > 0 && --collisionMat[i] == COLL_BUFF) {
                            collisionMat[i] = 0;
                        }
                    }
                }
            }
            if (POST_MOVE) {
                for (int i = 0; i < NUMBALLS; i++) balls[i].move();
            }
            if (isTracking()) {
                if (TRACK_STYLE == TRACK_ORIGIN) trackBallFromOrigin(balls[BALLVIEW]); else if (TRACK_STYLE == TRACK_BACK) trackBallFromBack(balls[BALLVIEW]); else if (TRACK_STYLE == TRACK_DIR) trackBallDir(balls[BALLVIEW]); else if (TRACK_STYLE == TRACK_BALL) {
                    trackBall(balls[BALLVIEW], balls[BALLVIEW2]);
                }
            } else if (FLYING) {
                slewView(new Vector3d(0, 0, -.01));
            }
            if (SONIFY) sonify();
            try {
                Thread.sleep(PAUSE);
            } catch (InterruptedException ignore) {
            }
            while (PAUSED) {
                Thread.yield();
            }
        }
    }

    public void stop() {
        RUNNING = false;
    }

    public void destroy() {
        SU.cleanup();
    }

    public void sonify() {
        if (CHORDS) {
            for (int i = 0; i < NUMBALLS; i++) {
                int[] oldchord = balls[i].getCurrentChord();
                int[] chord = balls[i].getNewChord();
                boolean chordChanged = false;
                for (int c = 0; c < 3; c++) {
                    if ((!isTracking() || BALLVIEW == i) && chord[c] != oldchord[c]) {
                        if (HOLD_CHORD) JMIDI.getChannel(i).noteOff(oldchord[c]); else if (!chordChanged) {
                            JMIDI.getChannel(i).allNotesOff();
                        }
                        JMIDI.getChannel(i).noteOn(chord[c], MAX_VOL);
                        chordChanged = true;
                    }
                }
            }
        } else {
            for (int i = 0; i < NUMBALLS; i++) {
                if (collisionMat[i] == COLL_BUFF) {
                    int p = balls[i].getCurrentPitch();
                    JMIDI.getChannel(i).noteOn(p, MAX_VOL);
                    balls[i].setPitch(p);
                }
            }
            for (int i = 0; i < NUMBALLS; i++) {
                if (collisionMat[i] == 0) {
                    JMIDI.getChannel(i).noteOff(balls[i].getLastPitch());
                }
            }
        }
    }

    public void trackBallFromOrigin(Ball ball) {
        Vector3d loc = ball.getLoc();
        Transform3D T = new Transform3D();
        if (T == null) return;
        T.lookAt(new Point3d(origin), new Point3d(loc), new Vector3d(0, BOWLSIZE / 2, 0));
        T.invert();
        setViewTrans(T);
    }

    public void trackBallFromBack(Ball ball) {
        Vector3d loc = ball.getLoc();
        Vector3d dir = ball.getDir();
        Transform3D T = new Transform3D();
        if (T == null) return;
        Point3d p1 = new Point3d(loc);
        p1.z += BOWLSIZE / 2;
        Point3d p2 = new Point3d(loc);
        p2.add(dir);
        T.lookAt(p1, p2, new Vector3d(0, BOWLSIZE / 2, 0));
        T.invert();
        setViewTrans(T);
    }

    public void trackBall(Ball ball, Ball ball2) {
        Transform3D T = getViewTrans();
        if (T == null) return;
        T.lookAt(new Point3d(ball.getLoc()), new Point3d(ball2.getLoc()), new Vector3d(0, BOWLSIZE / 2, 0));
        T.invert();
        setViewTrans(T);
    }

    public void trackBallDir(Ball ball) {
        Transform3D T = getViewTrans();
        if (T == null) return;
        Point3d lookpt = new Point3d(ball.getLoc());
        lookpt.add(ball.getDir());
        T.lookAt(new Point3d(ball.getLoc()), lookpt, new Vector3d(0, BOWLSIZE / 2, 0));
        T.invert();
        setViewTrans(T);
    }

    public void trackBallFav(Ball ball) {
        int b = ball.getFav();
        if (b == BALLVIEW) {
            if (b++ == NUMBALLS) b = 0;
        }
        Ball fav = balls[b];
        Transform3D T = getViewTrans();
        if (T == null) return;
        T.lookAt(new Point3d(ball.getLoc()), new Point3d(fav.getLoc()), new Vector3d(0, BOWLSIZE / 2, 0));
        T.invert();
        setViewTrans(T);
    }

    public void resetView() {
        Vector3d mv = new Vector3d(0, 0, BOWLSIZE * VIEW_DIST_FACTOR);
        Transform3D T = getViewTrans();
        Matrix3d m = new Matrix3d();
        m.setIdentity();
        T.set(m, mv, 1);
        setViewTrans(T);
    }

    public void updateView() {
        paintAll(getGraphics());
    }

    public void slewView(Vector3d sv) {
        Transform3D T = getViewTrans();
        T.transform(sv);
        Vector3d v = new Vector3d();
        Matrix3d m = new Matrix3d();
        T.get(m, v);
        v.add(sv);
        T.set(m, v, 1);
        setViewTrans(T);
    }

    public void rotateView(Matrix3d rotmat) {
        Transform3D T = getViewTrans();
        Vector3d v = new Vector3d();
        Matrix3d m = new Matrix3d();
        T.get(m, v);
        m.mul(rotmat);
        T.set(m, v, 1);
        setViewTrans(T);
    }

    public void clearViewRotation() {
        Matrix3d m = new Matrix3d();
        m.setIdentity();
        Transform3D T = getViewTrans();
        Vector3d v = new Vector3d();
        T.get(v);
        T.set(m, v, 1);
        setViewTrans(T);
    }

    public Transform3D getViewTrans() {
        Transform3D T = new Transform3D();
        try {
            MultiTransformGroup MTG = SU.getViewingPlatform().getMultiTransformGroup();
            MTG.getTransformGroup(0).getTransform(T);
        } catch (NullPointerException e) {
            return null;
        }
        return T;
    }

    public void setViewTrans(Transform3D T3D) {
        try {
            MultiTransformGroup MTG = SU.getViewingPlatform().getMultiTransformGroup();
            MTG.getTransformGroup(0).setTransform(T3D);
        } catch (NullPointerException ignore) {
        }
    }

    public static double rndDouble(double range) {
        return -range + (Math.random() * (range * 2));
    }

    public static Point3d rndPt3d(double range) {
        double x, y, z;
        x = rndDouble(range);
        y = rndDouble(range);
        z = rndDouble(range);
        return new Point3d(x, y, z);
    }

    public static Vector3d rndVec3d(double range) {
        double x, y, z;
        x = rndDouble(range);
        y = rndDouble(range);
        z = rndDouble(range);
        return new Vector3d(x, y, z);
    }

    public static Color3f rndCol() {
        float x = (float) (Math.random() * 1);
        float y = (float) (Math.random() * 1);
        float z = (float) (Math.random() * 1);
        return new Color3f(x, y, z);
    }

    public Texture getTexture(String texfile) {
        java.net.URL TexImage = null;
        try {
            TexImage = new java.net.URL("file:" + texfile);
        } catch (java.net.MalformedURLException ex) {
            blurb(ex.getMessage());
            System.exit(1);
        }
        TextureLoader tex = new TextureLoader(TexImage, this);
        return tex.getTexture();
    }

    public Texture getTexture(URL TexImage) {
        TextureLoader tex = new TextureLoader(TexImage, this);
        return tex.getTexture();
    }

    public static void renameTextures(String texdir, String prefix) {
        File F = new File(texdir);
        if (F.isDirectory()) {
            File[] texfiles = F.listFiles();
            for (int i = 0, n = 0; i < texfiles.length; i++) {
                String filename = texfiles[i].getName();
                String ext = filename.substring(filename.length() - 4);
                if (ext.equalsIgnoreCase(".jpg")) {
                    String newname = texdir + "/" + prefix + (n++) + ext;
                    System.out.println(filename + " -> " + newname);
                    File newfile = new File(newname);
                    if (!newfile.exists()) texfiles[i].renameTo(new File(newname)); else System.out.println("Oops: file already exists!");
                }
            }
        }
    }

    public Texture rndTex(String texdir) {
        URL url = null;
        try {
            if (IS_APPLET) {
                int r = (int) (Math.random() * MAXTEXTS) + 1;
                url = new URL(getCodeBase(), texdir + "/texture" + r + ".jpg");
            } else url = new URL(getDocumentBase(), texdir);
        } catch (MalformedURLException augh) {
            System.out.println("Bad URL: " + url);
            return null;
        }
        try {
            if (IS_APPLET) {
                return getTexture(url);
            } else {
                File F = new File(url.getFile());
                if (F.isDirectory()) {
                    File[] texfiles = F.listFiles();
                    int r = (int) (Math.random() * texfiles.length);
                    try {
                        return getTexture(texfiles[r].toURI().toURL());
                    } catch (MalformedURLException augh) {
                        System.out.println("Bad URL file: " + texfiles[r].toString());
                        return null;
                    }
                }
            }
        } catch (SecurityException augh) {
            System.out.println(augh.toString());
            return null;
        }
        return null;
    }

    public Geometry[] getShards(float range, float size) {
        TriangleArray[] TA = new TriangleArray[2];
        TA[0] = new TriangleArray(3, TriangleArray.COORDINATES | TriangleArray.NORMALS);
        TA[1] = new TriangleArray(3, TriangleArray.COORDINATES | TriangleArray.NORMALS);
        Point3d pt = rndPt3d(range);
        Point3d p1 = new Point3d(pt);
        Point3d p2 = new Point3d(pt.x + size, pt.y + size, pt.z);
        Point3d p3 = new Point3d(pt.x - size, pt.y + size, pt.z);
        Vector3d d1 = new Vector3d();
        Vector3d d2 = new Vector3d();
        d1.sub(p2, p1);
        d2.sub(p3, p1);
        Vector3d n1 = new Vector3d();
        n1.add(d1, d2);
        n1.scale(.75f);
        d1.sub(p1, p2);
        d2.sub(p3, p2);
        Vector3d n2 = new Vector3d();
        n2.add(d1, d2);
        n2.scale(.75f);
        d1.sub(p1, p3);
        d2.sub(p2, p3);
        Vector3d n3 = new Vector3d();
        n3.add(d1, d2);
        n3.scale(.75f);
        TA[0].setCoordinate(0, p1);
        TA[0].setNormal(0, new Vector3f(n1));
        TA[0].setCoordinate(1, p2);
        TA[0].setNormal(1, new Vector3f(n2));
        TA[0].setCoordinate(2, p3);
        TA[0].setNormal(2, new Vector3f(n3));
        Point3d np1 = new Point3d();
        np1.add(p1, n1);
        Point3d np2 = new Point3d();
        np2.add(p2, n2);
        Point3d np3 = new Point3d();
        np3.add(p3, n3);
        Vector3d v1 = new Vector3d();
        v1.sub(np1, p1);
        Vector3d v2 = new Vector3d();
        v2.sub(np2, p2);
        Vector3d v3 = new Vector3d();
        v3.sub(np3, p3);
        TA[1].setCoordinate(0, np1);
        TA[1].setNormal(0, new Vector3f(v1));
        TA[1].setCoordinate(1, np2);
        TA[1].setNormal(1, new Vector3f(v2));
        TA[1].setCoordinate(2, np3);
        TA[1].setNormal(2, new Vector3f(v3));
        return TA;
    }

    public Geometry getDust(double range) {
        PointArray PA = new PointArray(1, PointArray.COORDINATES | PointArray.NORMALS);
        Point3d p = rndPt3d(range);
        PA.setCoordinate(0, p);
        if (SPOTLIGHT) PA.setNormal(0, new Vector3f(0, 0, -1)); else PA.setNormal(0, new Vector3f(p));
        return PA;
    }

    public TransformGroup[] loadWalls() {
        int BW = 0, FW = 1, LW = 2, RW = 3, TW = 4;
        TransformGroup[] WallGrp = new TransformGroup[5];
        Box BackWall = new Box(BOWLSIZE, BOWLSIZE, .1f, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, getWallApp(null));
        Box FloorWall = new Box(BOWLSIZE, .1f, BOWLSIZE, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, getWallApp(null));
        Box LeftWall = new Box(.1f, BOWLSIZE, BOWLSIZE, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, getWallApp(null));
        Box RightWall = new Box(.1f, BOWLSIZE, BOWLSIZE, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, getWallApp(null));
        Box TopWall = new Box(BOWLSIZE, .1f, BOWLSIZE, Box.GENERATE_NORMALS | Box.GENERATE_TEXTURE_COORDS, getWallApp(null));
        for (int w = 0; w < WallGrp.length; w++) WallGrp[w] = new TransformGroup();
        WallGrp[BW].addChild(BackWall);
        WallGrp[FW].addChild(FloorWall);
        WallGrp[LW].addChild(LeftWall);
        WallGrp[RW].addChild(RightWall);
        WallGrp[TW].addChild(TopWall);
        Transform3D WallTrans = new Transform3D();
        WallTrans.set(new Vector3d(0, 0, -BOWLSIZE));
        WallGrp[BW].setTransform(WallTrans);
        WallTrans.set(new Vector3d(0, -BOWLSIZE, 0));
        WallGrp[FW].setTransform(WallTrans);
        WallTrans.set(new Vector3d(-BOWLSIZE, 0, 0));
        WallGrp[LW].setTransform(WallTrans);
        WallTrans.set(new Vector3d(BOWLSIZE, 0, 0));
        WallGrp[RW].setTransform(WallTrans);
        WallTrans.set(new Vector3d(0, BOWLSIZE, 0));
        WallGrp[TW].setTransform(WallTrans);
        return WallGrp;
    }

    public Appearance rndBallApp(Color3f lCol) {
        Appearance app = new Appearance();
        app.setTexture(rndTex(BallTexDir));
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.MODULATE);
        app.setTextureAttributes(texAttr);
        if (BLK_BALL) app.setMaterial(new Material(black, black, lCol, black, 100)); else app.setMaterial(new Material(lCol, rndCol(), rndCol(), black, 100));
        return app;
    }

    public Appearance getWallApp(String texfile) {
        Appearance app = new Appearance();
        if (texfile == null) app.setTexture(rndTex(WallTexDir)); else app.setTexture(rndTex(texfile));
        TextureAttributes texAttr = new TextureAttributes();
        texAttr.setTextureMode(TextureAttributes.MODULATE);
        app.setTextureAttributes(texAttr);
        if (BLK_WALL) {
            app.setMaterial(new Material(black, black, rndCol(), black, 100));
        } else {
            app.setMaterial(new Material(black, rndCol(), rndCol(), black, 64));
        }
        return app;
    }

    public Appearance getShardApp(Color3f col) {
        Color3f invcol = new Color3f(1 - col.x, 1 - col.y, 1 - col.z);
        Appearance app = new Appearance();
        ColoringAttributes ColApp = new ColoringAttributes();
        ColApp.setColor(col);
        app.setColoringAttributes(ColApp);
        if (BLK_WALL) {
            app.setMaterial(new Material(black, black, col, black, 100));
        } else {
            app.setMaterial(new Material(black, col, invcol, black, 100));
        }
        return app;
    }

    public void blurb(String txt) {
        blurb(txt, true);
    }

    public void blurb(String txt, boolean newline) {
        if (StatusTxt == null) System.out.println(txt); else if (newline) StatusTxt.append(txt + FileHelper.EOL); else StatusTxt.append(txt);
    }

    public void init() {
        System.out.println(WELCOME_MSG);
        try {
            if (IS_APPLET) {
                HELPFILE = new URL(getCodeBase().toString() + "./" + JSphereDir + "readme.txt");
                factorySettings();
            } else {
                HELPFILE = new URL(getDocumentBase().toString() + JSphereDir + "readme.txt");
            }
        } catch (MalformedURLException augh) {
        }
        if (IS_APPLET) {
            String ballDir = getParameter("BallTexDir");
            String wallDir = getParameter("WallTexDir");
            String numTex = getParameter("Textures");
            if (ballDir != null) BallTexDir = ballDir;
            if (wallDir != null) WallTexDir = wallDir;
            if (numTex != null) {
                MAXTEXTS = Integer.parseInt(numTex);
                if (MAXTEXTS < 1) MAXTEXTS = 1;
            }
        }
        setLayout(new BorderLayout());
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas = new Canvas3D(config);
        add("Center", canvas);
        StatusTxt = new TextArea();
        add("East", StatusTxt);
        blurb(WELCOME_MSG);
        setVisible(true);
        BranchGroup scene = createSceneGraph();
        SU = new SimpleUniverse(canvas);
        resetView();
        class KeyCmdListener extends KeyAdapter {

            public void keyPressed(KeyEvent e) {
                Matrix3d m = new Matrix3d();
                int c = e.getKeyCode();
                char C = e.getKeyChar();
                if (c == 27) {
                    JMIDI.silence();
                    unTrack();
                    resetView();
                } else if (c == 39) {
                    slewView(new Vector3d(1, 0, 0));
                } else if (c == 37) {
                    slewView(new Vector3d(-1, 0, 0));
                } else if (c == 38) {
                    slewView(new Vector3d(0, 1, 0));
                } else if (c == 40) {
                    slewView(new Vector3d(0, -1, 0));
                } else if (c == 155) {
                    slewView(new Vector3d(0, 0, 1));
                } else if (c == 12) {
                    slewView(new Vector3d(0, 0, -1));
                } else if (c == 33) {
                    m.rotX(.1);
                    rotateView(m);
                } else if (c == 36) {
                    m.rotX(-.1);
                    rotateView(m);
                } else if (c == 35) {
                    m.rotY(.1);
                    rotateView(m);
                } else if (c == 34) {
                    m.rotY(-.1);
                    rotateView(m);
                } else if (C == 'z') {
                    m.rotZ(.1);
                    rotateView(m);
                } else if (C == 'x') {
                    m.rotZ(-.1);
                    rotateView(m);
                } else if (c == 32) {
                    if (!isTracking()) track();
                    if (TRACK_STYLE == TRACK_BALL) cycleTrackBall(); else cycleViewBall();
                } else if (C == 'q') {
                    Ball.setSpeed(Ball.getSpeed() + SPEEDVAR);
                    blurb("Speed: " + Ball.getSpeed());
                } else if (C == 'a') {
                    Ball.setSpeed(Ball.getSpeed() - SPEEDVAR);
                    blurb("Speed: " + Ball.getSpeed());
                } else if (C == 'g') {
                    GRAVITY = !GRAVITY;
                    blurb("Gravity: " + GRAVITY);
                } else if (C == 'r') {
                    clearViewRotation();
                } else if (C == 't') {
                    if (!isTracking()) track();
                    if (++TRACK_STYLE >= MAX_TRACK) TRACK_STYLE = 0;
                    blurb("Track style: " + TRACK_STYLES[TRACK_STYLE]);
                    if (TRACK_STYLE == TRACK_BALL && BALLVIEW == BALLVIEW2) cycleTrackBall();
                } else if (C == ']') {
                    PAUSE += PAUSE_VAR;
                    if (PAUSE > 100000) PAUSE = 100000;
                    blurb("Pause: " + PAUSE);
                } else if (C == '[') {
                    PAUSE -= PAUSE_VAR;
                    if (PAUSE < 0) PAUSE = 0;
                    blurb("Pause: " + PAUSE);
                } else if (C == 'e' || C == 'E') {
                    BLK_BALL = !BLK_BALL;
                    blurb("Eightballs: " + BLK_BALL);
                    if (C == 'E') reset((Canvas3D) e.getComponent());
                } else if (C == 'w' || C == 'W') {
                    WALLS = !WALLS;
                    blurb("Walls: " + WALLS);
                    if (C == 'W') reset((Canvas3D) e.getComponent());
                } else if (C == 's' || C == 'S') {
                    SHARDS = !SHARDS;
                    blurb("Shards: " + SHARDS);
                    if (C == 'S') reset((Canvas3D) e.getComponent());
                } else if (C == 'd' || C == 'D') {
                    DUST = !DUST;
                    blurb("Dust: " + DUST);
                    if (C == 'D') reset((Canvas3D) e.getComponent());
                } else if (C == 'b' || C == 'B') {
                    BLK_WALL = !BLK_WALL;
                    blurb("Dark dust/shards/walls: " + BLK_WALL);
                    if (C == 'B') reset((Canvas3D) e.getComponent());
                } else if (C == 'c') {
                    MULTICOL = !MULTICOL;
                    blurb("Multiple Reflecting Colors: " + MULTICOL);
                } else if (C == 'C') {
                    BALLSHAPE++;
                    if (BALLSHAPE == BALLSHAPES) BALLSHAPE = SPHERE;
                    reset((Canvas3D) e.getComponent());
                } else if (C == 'v') {
                    VERTICIES += VERT_VAR;
                    if (VERTICIES > MAX_VERT) VERTICIES = VERT_VAR;
                    blurb("Verticies: " + VERTICIES);
                } else if (C == 'V') {
                    Ball.VISIBLE = !Ball.VISIBLE;
                    blurb("Visible Balls: " + Ball.VISIBLE);
                    reset((Canvas3D) e.getComponent());
                } else if (C == 'n' || C == 'N') {
                    BALLSIZE += SIZE_VAR;
                    if (BALLSIZE > MAX_SIZE) BALLSIZE = SIZE_VAR;
                    blurb("Ball Size: " + BALLSIZE);
                    if (C == 'N') reset((Canvas3D) e.getComponent());
                } else if (C == 'l' || C == 'L') {
                    SPOTLIGHT = !SPOTLIGHT;
                    blurb("Spotlight: " + SPOTLIGHT);
                    if (C == 'L') reset((Canvas3D) e.getComponent());
                } else if (C == 'i' || C == 'I') {
                    if (C == 'i') SPOT_CON += SPOT_CON_VAR; else SPOT_CON -= SPOT_CON_VAR;
                    if (SPOT_CON > SPOT_MAX_CON) SPOT_CON = 0; else if (SPOT_CON < 0) SPOT_CON = SPOT_MAX_CON;
                    blurb("Spotlight intensity: " + SPOT_CON);
                    setBallLights();
                } else if (C == 'k' || C == 'K') {
                    if (C == 'k') SPOT_SPREAD++; else SPOT_SPREAD--;
                    if (SPOT_SPREAD > SPOT_MAX_SPREAD) SPOT_SPREAD = 1; else if (SPOT_SPREAD < 1) SPOT_SPREAD = SPOT_MAX_SPREAD;
                    blurb("Spotlight spread angle factor: " + SPOT_SPREAD);
                    setBallLights();
                } else if (C == 'o') {
                    LIGHT_COL++;
                    if (LIGHT_COL >= LIGHT_STYLES.length) LIGHT_COL = 0;
                    blurb("Color style: " + LIGHT_STYLES[LIGHT_COL]);
                } else if (C == '*') {
                    SPOT_STYLE++;
                    if (SPOT_STYLE >= SPOT_STYLES.length) SPOT_STYLE = 0;
                    blurb("Spotlight style: " + SPOT_STYLES[SPOT_STYLE]);
                } else if (C == '>') {
                    if (SPOTLIGHT) {
                        SPOT_ATT += SPOT_ATT_VAR;
                        if (SPOT_ATT > MAX_SPOT_ATT) SPOT_ATT = 0;
                        blurb("Spotlight attenuation: " + SPOT_ATT);
                    } else {
                        POINT_ATT += POINT_ATT_VAR;
                        if (POINT_ATT > MAX_POINT_ATT) POINT_ATT = 0;
                        blurb("Pointlight attenuation: " + POINT_ATT);
                    }
                    setBallLights();
                } else if (C == '<') {
                    if (SPOTLIGHT) {
                        SPOT_ATT -= SPOT_ATT_VAR;
                        if (SPOT_ATT < 0) SPOT_ATT = MAX_SPOT_ATT;
                        blurb("Spotlight attenuation: " + SPOT_ATT);
                    } else {
                        POINT_ATT -= POINT_ATT_VAR;
                        if (POINT_ATT < 0) POINT_ATT = MAX_POINT_ATT;
                        blurb("Pointlight attenuation: " + POINT_ATT);
                    }
                    setBallLights();
                } else if (C == ',' && SONIFY) {
                    int b = BALLVIEW;
                    if (b < 0) b = 0;
                    JMIDI.getChannel(b).allNotesOff();
                    BALL_SOUNDS[b]--;
                    if (BALL_SOUNDS[b] < 0) BALL_SOUNDS[b] = MAX_INST;
                    JMIDI.setChannel(b, BALL_SOUNDS[b]);
                } else if (C == '.' && SONIFY) {
                    int b = BALLVIEW;
                    if (b < 0) b = 0;
                    JMIDI.getChannel(b).allNotesOff();
                    BALL_SOUNDS[b]++;
                    if (BALL_SOUNDS[b] > MAX_INST) BALL_SOUNDS[b] = 0;
                    JMIDI.setChannel(b, BALL_SOUNDS[b]);
                } else if (C == '/') {
                    FIXED_PITCH = !FIXED_PITCH;
                    if (FIXED_PITCH) {
                        for (int i = 0; i < NUMBALLS; i++) balls[i].setPitch();
                    }
                    blurb("Fixed ball pitch: " + FIXED_PITCH);
                } else if (C == '-') {
                    COMPLEX_BOUNCE = !COMPLEX_BOUNCE;
                    blurb("Complex bounce sounds: " + COMPLEX_BOUNCE);
                } else if (C == '=') {
                    POST_MOVE = !POST_MOVE;
                    blurb("Post-move: " + POST_MOVE);
                } else if (C == '\\') {
                    WALL_SOUND = !WALL_SOUND;
                    blurb("Wall bounce sounds: " + WALL_SOUND);
                } else if (C == '^') {
                    PITCH_DEBUG = !PITCH_DEBUG;
                    blurb("Pitch debug: " + PITCH_DEBUG);
                } else if (C == 'm') {
                    if (!SONIFY) SONIFY = initMidi(); else {
                        SONIFY = false;
                        JMIDI.unload();
                    }
                    blurb("Sonify: " + SONIFY);
                } else if (C == 'p') {
                    PAUSED = !PAUSED;
                } else if (C == 'Q' || C == 'X') {
                    SU.cleanup();
                    System.exit(0);
                } else if (C == 'f') {
                    FLYING = !FLYING;
                    blurb("Flying: " + FLYING);
                } else if (C == 'F') {
                    factorySettings();
                    reset((Canvas3D) e.getComponent());
                } else if (C == '#') {
                    CHORDS = !CHORDS;
                    blurb("Chords: " + CHORDS);
                } else if (C == '!') {
                    JMIDI.silence();
                } else if (C == '%') {
                    HOLD_CHORD = !HOLD_CHORD;
                    blurb("Sustained chords: " + HOLD_CHORD);
                } else if (C == '*') {
                    PITCH_DEBUG = !PITCH_DEBUG;
                    blurb("Pitch Debug: " + PITCH_DEBUG);
                } else if (C == '?') {
                    blurb(FileHelper.EOL + FileHelper.listFile(HELPFILE, FileHelper.EOL));
                } else if (c == 10) {
                    reset((Canvas3D) e.getComponent());
                } else if (C == '`') {
                    FULLSCREEN = !FULLSCREEN;
                    if (FULLSCREEN) remove(StatusTxt); else add(StatusTxt, "East");
                    updateView();
                } else if (c > 49 && c < 58) {
                    NUMBALLS = c - 48;
                    blurb("Balls: " + NUMBALLS);
                    reset((Canvas3D) e.getComponent());
                }
            }
        }
        KeyCmdListener KListener = new KeyCmdListener();
        canvas.addKeyListener(KListener);
        scene.compile();
        SU.addBranchGraph(scene);
    }
}

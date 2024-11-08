package engine3D;

import com.threed.jpct.*;
import com.threed.jpct.util.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Vector;
import java.awt.*;
import java.io.File;
import java.awt.Color;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Timer;

public class Engine3D extends Thread {

    public static final int NO_SHADOWS = 0;

    public static final int STATIC_SHADOWS = 1;

    public static final int DYNAMIC_SHADOWS_LOW = 256;

    public static final int DYNAMIC_SHADOWS_MEDIUM = 512;

    public static final int DYNAMIC_SHADOWS_HIGH = 1024;

    public static final int SAMPLINGMODE_GL_AA_2X = 20;

    public static final int SAMPLINGMODE_GL_AA_4X = 40;

    public static final int SAMPLINGMODE_HARDWARE_ONLY = 0;

    public static final int SAMPLINGMODE_NORMAL = 0;

    public static final int SAMPLINGMODE_OGSS = 1;

    public static final int SAMPLINGMODE_OGSS_FAST = 3;

    public static final int SAMPLINGMODE_OGUS = 2;

    public static final int DIR_NORTE = 90;

    public static final int DIR_NORESTE = 45;

    public static final int DIR_ESTE = 0;

    public static final int DIR_SURESTE = 315;

    public static final int DIR_SUR = 270;

    public static final int DIR_SUROESTE = 225;

    public static final int DIR_OESTE = 180;

    public static final int DIR_NOROESTE = 135;

    public static final int DEG_NORTE = 1;

    public static final int DEG_NORESTE = 2;

    public static final int DEG_ESTE = 3;

    public static final int DEG_SURESTE = 4;

    public static final int DEG_SUR = 5;

    public static final int DEG_SUROESTE = 6;

    public static final int DEG_OESTE = 7;

    public static final int DEG_NOROESTE = 8;

    public static final int ANIM_STAND = 0;

    public static final int ANIM_WALK = 1;

    public static final int ANIM_ATACK1 = 2;

    public static final int ANIM_ATACK2 = 3;

    public static final int ANIM_DEFEND = 4;

    public static final int ANIM_DEAD = 5;

    public static final int ACTION_MOVE_CHARACTER = 20;

    public static final int ACTION_ANIMATE_CHARACTER = 21;

    public static final int MODE_NOT_LOOP = 0;

    public static final int MODE_NOT_LOOP_NOT_WAIT = 1;

    public static final int MODE_LOOP = 2;

    public static final int MODE_LOOP_NOT_WAIT = 3;

    public static final boolean LOOP = true;

    public static final boolean NOT_LOOP = false;

    public static final float MAX_LOW_POSITION_CAMERA = 1.25f;

    public static final String GRP_FIELD_MOV = "celda.GIF";

    public static final String GRP_MINI_SELECTED = "celda.GIF";

    public static final String SIGNAL_MESH = "mSignal.3DS";

    public static final float GROUND_LEVEL = 0f;

    public static final float SPEED_RUN = 0.025f;

    public static final int SPEED_TURN = 10;

    static final String objetoEmisor = "Engine3D";

    static final String LOGFILENAME = "eLog.txt";

    static final Color colorWireFrame = Color.GREEN;

    static final int defaultFPS = 35;

    static final int minimumSleepValue = 5;

    static final int defaultMaxPoly = 50000;

    static final float animationSpeed = 0.01f;

    static final int maxAnimationSequences = 30;

    static final long microsegundo = 1000000;

    static final long milisegundo = 1000;

    private World world;

    private TextureManager texMan;

    private FrameBuffer buffer;

    private Canvas canvas;

    private int fpsValue;

    private long refreshTime;

    private boolean renderIt;

    private boolean closingEngine;

    private boolean motor3DDestroyed;

    private EngineLog eLog;

    private boolean wireframe;

    private Texture numbers;

    private boolean showFPS = false;

    private int triangles;

    private PaintListener paintListener;

    protected int shadowMode;

    protected boolean usingDinamicShadows;

    protected boolean usingStaticShadows;

    private Projector projector;

    private ShadowHelper shadowHelper;

    private HandlObj handlObj;

    private HandlEngine handlEngine3D;

    private MouseListenerEngine3D mouseListener;

    private ObjectManager oManager;

    private AnimationStack animStack;

    private DiskLoader diskLoader;

    protected float posCamX = 5;

    protected float posCamY = -7;

    protected float posCamZ = 0;

    protected float lookX = 5;

    protected float lookY = 0;

    protected float lookZ = 5;

    protected boolean camHasMoved;

    protected float rotCamValue = 270;

    protected int maxXCamPos;

    protected int maxYCamPos;

    protected int maxZCamPos;

    protected int mousePosX;

    protected int mousePosY;

    protected int mPosRX;

    protected int mPosRY;

    protected int poligonIDAimedFromMouse;

    protected Object3D object3DIDAimendFromMouse;

    protected String lastObjectAimed;

    private Object3D cursorMouse;

    private float offsettRot;

    public Engine3D(int width, int height, int FPS, int shadowsType, int frameBufferMode) {
        super("hebraMotor3D");
        fpsValue = FPS;
        shadowMode = shadowsType;
        if ((shadowMode == Engine3D.DYNAMIC_SHADOWS_LOW) || (shadowMode == Engine3D.DYNAMIC_SHADOWS_MEDIUM) || (shadowMode == Engine3D.DYNAMIC_SHADOWS_HIGH)) {
            usingDinamicShadows = true;
        } else if (shadowMode == Engine3D.STATIC_SHADOWS) {
            usingStaticShadows = true;
        }
        refreshTime = 0;
        poligonIDAimedFromMouse = -1;
        object3DIDAimendFromMouse = null;
        initEngine3D();
        eLog = new EngineLog(LOGFILENAME);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Hebra del engine3D creada.");
        world = new World();
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Instancia WORLD del mundo virtual creada.");
        texMan = TextureManager.getInstance();
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Gestor de texturas instanciado.");
        world.setAmbientLight(255, 255, 255);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Luz ambiental definida, valores m�ximos.");
        buffer = new FrameBuffer(width, height, frameBufferMode);
        String modeFrameBuffer = getStringFromRenderMethod(frameBufferMode);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "FrameBuffer creado y configurado para OPENGL en modo: " + modeFrameBuffer + ".");
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Desactivada renderizaci�n por software.");
        buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
        if (usingDinamicShadows) {
            projector = new Projector();
            Config.glShadowZBias = 0.0f;
            switch(shadowMode) {
                case (Engine3D.DYNAMIC_SHADOWS_LOW):
                    shadowHelper = new ShadowHelper(world, buffer, projector, Engine3D.DYNAMIC_SHADOWS_LOW);
                    break;
                case (Engine3D.DYNAMIC_SHADOWS_MEDIUM):
                    shadowHelper = new ShadowHelper(world, buffer, projector, Engine3D.DYNAMIC_SHADOWS_MEDIUM);
                    break;
                case (Engine3D.DYNAMIC_SHADOWS_HIGH):
                    shadowHelper = new ShadowHelper(world, buffer, projector, Engine3D.DYNAMIC_SHADOWS_HIGH);
                    break;
            }
            shadowHelper.setFiltering(true);
            shadowHelper.setAmbientLight(new Color(150, 150, 150));
            eLog.writeEngineLogWithEmisor(objetoEmisor, "Sombras din�micas iniciadas.");
        }
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Canvas asociado al FrameBuffer creado.");
        canvas = buffer.enableGLCanvasRenderer();
        paintListener = new PaintListener();
        buffer.setPaintListener(paintListener);
        buffer.setPaintListenerState(true);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "PaintListener creado, asociado a buffer y activado.");
        oManager = new ObjectManager(eLog);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Manejador de almacenamiento de texturas y mallas creado.");
        handlObj = new HandlObj(this, eLog);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Manejador de objetos del mundo virtual creado.");
        handlEngine3D = new HandlEngine(this);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Manjeador del engine3D creado.");
        animStack = new AnimationStack();
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Cola de animaciones y acciones del engine3D creado.");
        mouseListener = new MouseListenerEngine3D(this);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Listener de rat�n creado para el engine3D.");
        diskLoader = new DiskLoader(this);
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Cargador de archivos del engine3D creado.");
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Listos para arrancar hebra Engine3D.");
    }

    public void run() {
        World.setDefaultThread(Thread.currentThread());
        buffer.optimizeBufferAccess();
        renderIt = false;
        motor3DDestroyed = false;
        closingEngine = false;
        triangles = 0;
        eLog.writeEngineLogWithEmisor(objetoEmisor, "EJECUTANDO HEBRA ENGINE3D (RUN).");
        world.getCamera().setPosition(posCamX, posCamY, posCamZ);
        world.getCamera().lookAt(new SimpleVector(lookX, lookY, lookZ));
        Character3D character = null;
        if (usingDinamicShadows) {
            shadowHelper.setCullingMode(false);
            shadowHelper.setAmbientLight(new Color(150, 150, 150));
            projector.setPosition(4.5f, -25, -18);
            projector.lookAt(new SimpleVector(4.5f, 0, 5));
            projector.setFOV(0.5f);
            projector.setYFOV(0.5f);
        }
        while (!motor3DDestroyed) {
            while (renderIt) {
                if (camHasMoved) {
                    world.getCamera().setPosition(posCamX, posCamY, posCamZ);
                    world.getCamera().lookAt(new SimpleVector(lookX, lookY, lookZ));
                    camHasMoved = false;
                }
                renderScene();
                mousePosX = mPosRX;
                mousePosY = mPosRY;
                String lastObjectAimed = get3DObjectFromMouse(mousePosX, mousePosY);
                if (cursorMouse != null) {
                    offsettRot = offsettRot + 0.1f;
                    cursorMouse.setRotationMatrix(new Matrix());
                    cursorMouse.rotateY(offsettRot);
                }
                handlObj.incrementCharacterAnimations(animationSpeed);
                handlObj.incrementAnimatedObjectAnimations(animationSpeed);
                character = actualizeAnimationActions(character);
                long timeToRenderScene = paintListener.getTimeToDrawAImage();
                long timeToRenderASecondOfScenes = timeToRenderScene * fpsValue;
                long totalRefreshTime = timeToRenderASecondOfScenes / microsegundo;
                refreshTime = ((milisegundo - totalRefreshTime) / fpsValue);
                if (refreshTime >= minimumSleepValue) {
                    refreshTime = refreshTime - minimumSleepValue;
                } else {
                    refreshTime = 0;
                }
                try {
                    Thread.sleep(refreshTime);
                } catch (InterruptedException e) {
                    System.out.println("Error en c�lculo y aplicaci�n del tiempo refresco en el engine3D : " + e.getMessage());
                    eLog.writeEngineLogWithEmisor(objetoEmisor, "Error durmiendo la hebra del engine3D.");
                }
            }
        }
        eLog.writeEngineLog("Engine3D cerrado.");
        eLog.closeEngineLog();
    }

    public void destroyEngine3D() {
        eLog.writeEngineLogWithEmisor(objetoEmisor, "Recogida petici�n de cerrar engine3D. PROCESADO.");
        closingEngine = true;
        renderIt = false;
        if (buffer != null) {
            buffer.disableRenderer(buffer.getSamplingMode());
            buffer.dispose();
        }
        motor3DDestroyed = true;
    }

    private String getStringFromRenderMethod(int mode) {
        String modeIntoString = "<< modo no identificado >>";
        switch(mode) {
            case FrameBuffer.SAMPLINGMODE_GL_AA_2X:
                modeIntoString = "SAMPLINGMODE_GL_AA_2X";
                break;
            case FrameBuffer.SAMPLINGMODE_GL_AA_4X:
                modeIntoString = "SAMPLINGMODE_GL_AA_4X";
                break;
            case FrameBuffer.SAMPLINGMODE_HARDWARE_ONLY:
                modeIntoString = "SAMPLINGMODE_HARDWARE_ONLY (NORMAL)";
                break;
            case FrameBuffer.SAMPLINGMODE_OGSS:
                modeIntoString = "SAMPLINGMODE_OGSS";
                break;
            case FrameBuffer.SAMPLINGMODE_OGSS_FAST:
                modeIntoString = "SAMPLINGMODE_OGSS_FAST";
                break;
            case FrameBuffer.SAMPLINGMODE_OGUS:
                modeIntoString = "SAMPLINGMODE_OGUS";
                break;
        }
        return modeIntoString;
    }

    private void initEngine3D() {
        Config.maxPolysVisible = defaultMaxPoly;
        Config.maxAnimationSubSequences = maxAnimationSequences;
        Config.glTrilinear = true;
        Config.farPlane = 4000;
        Logger.setLogLevel(2);
    }

    private void renderScene() {
        synchronized (world) {
            if ((buffer != null) && (world != null) & (canvas != null)) {
                if (usingDinamicShadows) {
                    handlObj.hiddenSignalsToRender();
                    shadowHelper.updateShadowMap();
                    buffer.clear(java.awt.Color.BLACK);
                    handlObj.visibleSignalsToRender();
                    shadowHelper.drawScene();
                } else {
                    buffer.clear(java.awt.Color.GRAY);
                    world.renderScene(buffer);
                    if (!wireframe) {
                        world.draw(buffer);
                    } else {
                        world.drawWireframe(buffer, colorWireFrame);
                    }
                }
                if (showFPS) {
                    blitNumber(paintListener.getFPS(), 10, 10);
                    blitNumber(triangles, 10, 20);
                    blitNumber(handlEngine3D.mouseCellPos[0], 10, 30);
                    blitNumber(handlEngine3D.mouseCellPos[1], 30, 30);
                    blitNumber(this.poligonIDAimedFromMouse, 10, 40);
                    if (this.object3DIDAimendFromMouse != null) {
                        blitNumber(this.object3DIDAimendFromMouse.getID(), 10, 50);
                    }
                }
                buffer.update();
                buffer.displayGLOnly();
                canvas.repaint();
            }
        }
    }

    private String get3DObjectFromMouse(int mousePositionX, int mousePositionY) {
        String object3DName = null;
        SimpleVector ray = Interact2D.reproject2D3D(world.getCamera(), buffer, mousePositionX, mousePositionY);
        int[] res = Interact2D.pickPolygon(world.getVisibilityList(), ray, Interact2D.EXCLUDE_NOT_SELECTABLE);
        if (res != null) {
            Object3D pickedObj = world.getObject(Interact2D.getObjectID(res));
            object3DName = handlObj.getNameInWorldFromObject(pickedObj);
            poligonIDAimedFromMouse = Interact2D.getPolygonID(res);
            object3DIDAimendFromMouse = pickedObj;
        } else {
            poligonIDAimedFromMouse = -1;
            object3DIDAimendFromMouse = null;
        }
        return object3DName;
    }

    private void blitNumber(int number, int x, int y) {
        if (numbers != null) {
            String sNum = Integer.toString(number);
            for (int i = 0; i < sNum.length(); i++) {
                char cNum = sNum.charAt(i);
                int iNum = cNum - 48;
                buffer.blit(numbers, iNum * 5, 0, x, y, 5, 9, FrameBuffer.TRANSPARENT_BLITTING);
                x += 5;
            }
        }
    }

    private Character3D actualizeAnimationActions(Character3D lastCharacterMoved) {
        Character3D lastMoved = lastCharacterMoved;
        if ((lastCharacterMoved == null) || ((!lastCharacterMoved.isDoingPrimaryAnimation()) && (!lastCharacterMoved.isMoving()))) {
            if (animStack.hasActions()) {
                if (lastCharacterMoved != null) {
                    System.out.println(Boolean.toString(lastCharacterMoved.isDoingPrimaryAnimation()) + "-" + Boolean.toString(lastCharacterMoved.isMoving()));
                }
                LittleAction littleAction = animStack.getNewAnimationAction();
                String nameInWorld = littleAction.nameObjectInWorld;
                int typeAction = littleAction.action;
                if (typeAction == ACTION_ANIMATE_CHARACTER) {
                    if (handlObj.containsAnimatedCharacter(nameInWorld)) {
                        if (littleAction.value2 == MODE_LOOP) {
                            handlObj.setCharacterAnimationSequence(nameInWorld, littleAction.value1, true);
                        } else if (littleAction.value2 == MODE_LOOP_NOT_WAIT) {
                            handlObj.setCharacterAnimationSequenceNoWait(nameInWorld, littleAction.value1, true);
                        } else if (littleAction.value2 == MODE_NOT_LOOP) {
                            handlObj.setCharacterAnimationSequence(nameInWorld, littleAction.value1, false);
                        } else if (littleAction.value2 == MODE_NOT_LOOP_NOT_WAIT) {
                            handlObj.setCharacterAnimationSequenceNoWait(nameInWorld, littleAction.value1, false);
                        }
                        lastMoved = handlObj.getAnimatedCharacterFromMap(littleAction.nameObjectInWorld);
                    } else {
                        eLog.writeEngineLogWithEmisor(objetoEmisor, "Se est� intentado asignar secuencias de animaci�n a un objeto inexistente: " + nameInWorld);
                    }
                } else if (typeAction == ACTION_MOVE_CHARACTER) {
                    if (handlObj.containsAnimatedCharacter(nameInWorld)) {
                        Character3D animObj = handlObj.getAnimatedCharacterFromMap(nameInWorld);
                        if (!animObj.isMoving()) {
                            animObj.setNewDestiny(littleAction.value1, littleAction.value3, littleAction.value4);
                        }
                        lastMoved = handlObj.getAnimatedCharacterFromMap(littleAction.nameObjectInWorld);
                    } else {
                        eLog.writeEngineLogWithEmisor(objetoEmisor, "Se est� intentado animar un personaje inexistente: " + nameInWorld);
                    }
                }
                animStack.removeFirstAnimationActionFromList();
            }
        } else {
            System.out.println("ni entro");
        }
        return lastMoved;
    }

    public DiskLoader getDiskLoader() {
        return diskLoader;
    }

    public AnimationStack getAnimationStack() {
        return animStack;
    }

    public ShadowHelper getShadowHelper() {
        return shadowHelper;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public World getWorld() {
        return world;
    }

    public TextureManager getTextureMan() {
        return texMan;
    }

    public HandlEngine getHandlEngine() {
        return handlEngine3D;
    }

    public EngineLog getEngineLog() {
        return eLog;
    }

    public HandlObj getHaldlObj() {
        return handlObj;
    }

    public ObjectManager getObjectManager() {
        return oManager;
    }

    public void setFPSFontsNumberTexture(Texture numTex) {
        numbers = numTex;
    }

    public void setShowFPS(boolean show) {
        showFPS = show;
    }

    public void setRenderizar(boolean valor) {
        if (!closingEngine) {
            renderIt = valor;
        }
    }

    public void setModeWireframe(boolean valor) {
        wireframe = valor;
    }

    public void putOnWorld(Object3D object3D) {
        synchronized (world) {
            triangles = triangles + object3D.getMesh().getTriangleCount();
            object3D.setFiltering(Object3D.FILTERING_DISABLED);
            world.addObject(object3D);
        }
    }

    public void removeFromWorld(Object3D object3D) {
        synchronized (world) {
            triangles = triangles - object3D.getMesh().getTriangleCount();
            world.removeObject(object3D);
        }
    }

    public void moveCameraInVectorDir(SimpleVector vectorNorm, float dist) {
        if ((vectorNorm.x + lookX <= maxXCamPos) && (vectorNorm.x + lookX >= 0) && (vectorNorm.z + lookZ <= maxZCamPos) && (vectorNorm.z + lookZ >= 0)) {
            synchronized (world) {
                Camera cam = world.getCamera();
                cam.moveCamera(vectorNorm, dist);
                float desplInX = cam.getPosition().x - posCamX;
                float desplInY = cam.getPosition().y - posCamY;
                float desplInZ = cam.getPosition().z - posCamZ;
                posCamX = cam.getPosition().x;
                posCamY = cam.getPosition().y;
                posCamZ = cam.getPosition().z;
                lookX = lookX + desplInX;
                lookY = lookY + desplInY;
                lookZ = lookZ + desplInZ;
                camHasMoved = true;
            }
        }
    }

    public void moveCamera(float px, float py, float pz) {
        posCamX = px;
        if (posCamY + py < -1) {
            posCamY = py;
        }
        posCamZ = pz;
        camHasMoved = true;
    }

    public void displaceCamera(float dispX, float dispY, float dispZ) {
        if ((posCamY + dispY < MAX_LOW_POSITION_CAMERA) && (posCamY + dispY >= maxYCamPos)) {
            posCamY = posCamY + dispY;
        }
        posCamZ = posCamZ + dispZ;
        posCamX = posCamX + dispX;
        camHasMoved = true;
    }

    public float getCamX() {
        return posCamX;
    }

    public float getCamY() {
        return posCamY;
    }

    public float getCamZ() {
        return posCamZ;
    }

    public void rotCamera(boolean dir, float speed) {
        if (dir) {
            rotCamValue = rotCamValue + speed;
        } else {
            rotCamValue = rotCamValue - speed;
        }
        float dx2 = (lookX - posCamX) * (lookX - posCamX);
        float dz2 = (lookZ - posCamZ) * (lookZ - posCamZ);
        float dist = (float) java.lang.Math.sqrt(dx2 + dz2);
        float radioValue = dist;
        double rad = java.lang.Math.toRadians(rotCamValue);
        double cosAngle = java.lang.Math.cos(rad);
        double senAngle = java.lang.Math.sin(rad);
        float y = (float) senAngle * radioValue;
        float x = (float) cosAngle * radioValue;
        moveCamera(x + lookX, posCamY, y + lookZ);
        world.getCamera().lookAt(new SimpleVector(lookX, lookY, lookZ));
        camHasMoved = true;
    }

    public float getRotCamValue() {
        return rotCamValue;
    }

    public Object3D getCursorMouse() {
        return cursorMouse;
    }

    public void setCursorMouse(String cMouse) {
        cursorMouse = handlEngine3D.getSignalFromMap(cMouse);
        cursorMouse.setRotationMatrix(new Matrix());
        offsettRot = 0.0f;
    }

    public MouseListenerEngine3D getMouseListenerEngine3D() {
        return mouseListener;
    }
}

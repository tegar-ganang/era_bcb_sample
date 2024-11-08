package com.cirnoworks.fisce.nativegame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import me.fantasy2.BackScene;
import me.fantasy2.Cell;
import me.fantasy2.ClassFactory;
import me.fantasy2.GameFrame;
import me.fantasy2.Hal;
import me.fantasy2.UI;
import com.cirnoworks.cas.SoundManager;
import com.cirnoworks.cis.IFisceUI;
import com.cirnoworks.cis.InputStatus;
import com.cirnoworks.common.ResourceFetcher;

public class NativeGame implements Hal {

    private final IFisceUI graphic;

    private final ResourceFetcher rf;

    private int[] message;

    private GameFrame frame;

    private ClassFactory uiFactory;

    private ClassFactory sceneFactory;

    private UI ui;

    private SoundManager soundManager;

    private BackScene scene;

    private HashMap<String, byte[]> ses = new HashMap<String, byte[]>();

    private Exception exception;

    public NativeGame(SoundManager soundManager, IFisceUI graphic, ResourceFetcher rf, GameFactory gf, String param, ClassFactory uiFactory, ClassFactory sceneFactory) {
        super();
        this.soundManager = soundManager;
        this.graphic = graphic;
        this.rf = rf;
        this.uiFactory = uiFactory;
        this.sceneFactory = sceneFactory;
        this.frame = gf.getGameFrame(this, param);
    }

    public NativeGame(Exception e) {
        graphic = null;
        rf = null;
        this.exception = e;
    }

    /**
	 * @return the exception
	 */
    public Exception getException() {
        return exception;
    }

    public InputStream getInputStream(String fn) {
        try {
            URL u = rf.getResource(fn);
            if (u != null) return u.openStream();
        } catch (IOException e) {
        }
        return null;
    }

    public void addObj(Cell bo) {
    }

    public void removeObj(Cell bo) {
    }

    public void updateObj(Cell bo) {
        CellData cd = (CellData) bo.getAttachment();
        if (cd == null) {
            cd = new CellData(bo, graphic);
            bo.attach(cd);
        }
        cd.update();
        cd.draw();
    }

    public void removeAllObjs() {
    }

    public void display() {
    }

    public int getControlDir() {
        InputStatus is = graphic.getInputStatus();
        return is.dir;
    }

    public boolean isGameKeyDown(int gk) {
        InputStatus is = graphic.getInputStatus();
        return is.keyDown[gk] > 0;
    }

    public boolean getGameKeyEvent(int gk) {
        return false;
    }

    public void playBGM(int bgmId, boolean loop) {
    }

    public long getBGMPos() {
        return 0;
    }

    public long getPointerStat() {
        return 0;
    }

    public void setPointerDisplay(boolean diaplay) {
    }

    public GameFrame getFrame() {
        return frame;
    }

    public UI attachUI(Class<?> c) {
        if (ui != null) {
            ui.uiDestroy();
        }
        UI next = null;
        if (uiFactory != null) {
            next = (UI) uiFactory.getImpl(c);
        }
        ui = next;
        if (next != null) {
            next.uiInit();
        }
        return next;
    }

    public Object attachBackScene(Class c) {
        if (scene != null) {
            scene.sceneDestroy();
        }
        BackScene next = null;
        if (sceneFactory != null) {
            next = (BackScene) sceneFactory.getImpl(c);
        }
        scene = next;
        if (next != null) {
            next.sceneInit();
        }
        return next;
    }

    public void exitGame() {
        if (ui != null) {
            ui.uiDestroy();
            ui = null;
        }
        if (scene != null) {
            scene.sceneDestroy();
            scene = null;
        }
    }

    public void sendMessage(int[] message) {
        this.message = message;
    }

    public int[] getStatus() {
        int[] ret = message;
        message = null;
        return ret;
    }

    public void start() {
        frame.init(this);
    }

    public void dealOneFrame() {
        frame.dealOneFrame();
        if (ui != null) {
            ui.uiDealOneFrame();
        }
        if (scene != null) {
            scene.sceneDealOneFrame();
        }
    }

    public void render() {
        frame.render();
        if (ui != null) {
            ui.uiRender();
        }
        if (scene != null) {
            scene.sceneRender();
        }
    }

    /**
	 * @return the uiFactory
	 */
    public ClassFactory getUiFactory() {
        return uiFactory;
    }

    /**
	 * @param uiFactory
	 *            the uiFactory to set
	 */
    public void setUiFactory(ClassFactory uiFactory) {
        this.uiFactory = uiFactory;
    }

    /**
	 * @param sceneFactory
	 *            the sceneFactory to set
	 */
    public void setSceneFactory(ClassFactory sceneFactory) {
        this.sceneFactory = sceneFactory;
    }

    @Override
    public void setCustomMessage(String customMessage) {
        graphic.setCustomMessage(customMessage);
    }

    @Override
    public void playSE(String se) {
        if (!ses.containsKey(se)) {
            try {
                loadSE(se);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        soundManager.playse(ses.get(se));
    }

    @Override
    public void loadSE(String se) throws IOException {
        InputStream is = null;
        String fn = "/res/se/" + se + ".wav";
        URL u = rf.getResource(fn);
        if (u != null) is = u.openStream();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(131072);
            byte[] buf = new byte[65536];
            int read;
            while ((read = is.read(buf)) >= 0) {
                baos.write(buf, 0, read);
            }
            ses.put(se, baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("打开资源错误：" + fn, e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void hideUI() {
        if (ui != null) {
            ui.uiSetVisible(false);
        }
    }

    @Override
    public void showUI() {
        if (ui != null) {
            ui.uiSetVisible(true);
        }
    }
}

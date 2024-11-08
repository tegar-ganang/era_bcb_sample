package scenes;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.swing.Timer;
import org.mt4j.MTApplication;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.MultipleDragProcessor;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.math.Tools3D;
import org.mt4j.util.math.ToolsBuffers;
import org.mt4j.util.math.ToolsMath;
import org.mt4j.util.opengl.GLTexture;
import org.mt4j.util.opengl.GLTextureSettings;
import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;

public class FluidScene extends AbstractScene {

    public class GestureEventListener implements IGestureEventListener {

        public GestureEventListener(FluidScene this$0) {
            super();
            this.this$0 = this$0;
        }

        final FluidScene this$0;

        @Override
        public boolean processGestureEvent(MTGestureEvent ge) {
            DragEvent de = (DragEvent) ge;
            float x = de.getDragCursor().getCurrentEvtPosX();
            float y = de.getDragCursor().getCurrentEvtPosY();
            switch(de.getId()) {
                case 0:
                case 1:
                    int xIndex = (int) ToolsMath.map(x, 0.0F, MT4jSettings.getInstance().getWindowWidth(), 0.0F, 126F);
                    int yIndex = (int) ToolsMath.map(y, 0.0F, MT4jSettings.getInstance().getWindowHeight(), 0.0F, 126F);
                    if (xIndex < 0) xIndex = 0;
                    if (xIndex > 125) xIndex = 125;
                    if (yIndex < 0) yIndex = 0;
                    if (yIndex > 125) yIndex = 125;
                    extforce[xIndex][yIndex] = -3.92F;
                    break;
            }
            return false;
        }
    }

    public static void o(float u, float v, FloatBuffer buf, int index) {
        if (buf == null) {
            return;
        } else {
            buf.put(index * 2, u);
            buf.put(index * 2 + 1, v);
            return;
        }
    }

    private void s() {
        for (int j = 1; j < 125; j++) {
            for (int i = 1; i < 125; i++) {
                o((float) (i * texRatioXRect) + vn[i][j][0] * perturbXRect, (float) (j * texRatioYRect) + vn[i][j][2] * perturbYRect, texBuffers[j], i * 2);
                o((float) (i * texRatioXRect) + vn[i][j + 1][0] * perturbXRect, (float) ((j + 1) * texRatioYRect) + vn[i][j + 1][2] * perturbYRect, texBuffers[j], i * 2 + 1);
                o(envCenterX + vn[i][j][0] * envPerTurbXRect, envCenterY + vn[i][j][2] * envPerTurbYRect, texEnvBuffers[j], i * 2);
                o(envCenterX + vn[i][j + 1][0] * envPerTurbXRect, envCenterY + vn[i][j + 1][2] * envPerTurbYRect, texEnvBuffers[j], i * 2 + 1);
            }
        }
    }

    public FluidScene(MTApplication mtApplication, String name) {
        super(mtApplication, name);
        SPRING_CONSTANT = 1.07F;
        DAMPING_CONSTANT = 0.05F;
        perturbX = 0.1F;
        perturbY = 0.1F;
        envPerTurbX = 1.25F;
        envPerTurbY = 1.25F;
        dt = 0.232F;
        hh = new float[126][126];
        ff = new float[126][126];
        vv = new float[126][126];
        fn = new float[2][126][126][3];
        vn = new float[126][126][3];
        extforce = new float[126][126];
        zoom = 50F;
        eyex = 62F;
        eyey = 62F + zoom;
        eyez = 62F;
        atx = 62F;
        aty = 62F;
        atz = 62F;
        upx = 0.0F;
        upy = 0.0F;
        upz = -1F;
        phi = 90F;
        theta = 0.0F;
        waterImagePath = (new StringBuilder("advanced")).append(MTApplication.separator).append("water").append(MTApplication.separator).append("data").append(MTApplication.separator).toString();
        counter = 0;
        avg = new float[3];
        a = new float[3];
        b = new float[3];
        c = new float[3];
        pt0 = new float[3];
        pt1 = new float[3];
        pt2 = new float[3];
        pt3 = new float[3];
        n0 = new float[3];
        n1 = new float[3];
        app = mtApplication;
        setClear(false);
        hasMultiTexture = false;
        if (MT4jSettings.getInstance().isOpenGlMode()) {
            if (!Tools3D.isGLExtensionSupported(mtApplication, "GL_ARB_texture_rectangle")) {
                System.err.println((new StringBuilder("Your graphics card doesent meet the requirements for running the scene: ")).append(name).toString());
                return;
            }
            int maxTextureUnits[] = new int[1];
            ((PGraphicsOpenGL) app.g).gl.glGetIntegerv(34018, maxTextureUnits, 0);
            int nbTextureUnits = maxTextureUnits[0];
            if (Tools3D.isGLExtensionSupported(mtApplication, "GL_ARB_multitexture") && nbTextureUnits >= 3) hasMultiTexture = true;
        } else {
            System.err.println((new StringBuilder(String.valueOf(name))).append(" requires OpenGL renderer").toString());
            return;
        }
        pgl = (PGraphicsOpenGL) app.g;
        gl = pgl.gl;
        glu = pgl.glu;
        vertBuffers = new FloatBuffer[126];
        texBuffers = new FloatBuffer[126];
        texEnvBuffers = new FloatBuffer[126];
        for (int i = 0; i < vertBuffers.length; i++) {
            vertBuffers[i] = ToolsBuffers.createVector3Buffer(252);
            texBuffers[i] = ToolsBuffers.createFloatBuffer(504);
            texEnvBuffers[i] = ToolsBuffers.createFloatBuffer(504);
        }
        n();
        GLTextureSettings tp = new GLTextureSettings();
        tex = new GLTexture(app, (new StringBuilder(String.valueOf(waterImagePath))).append("3123371901_daf9cb8d6b_b3.jpg").toString(), tp);
        texRatioXRect = tex.width / 126;
        texRatioYRect = tex.height / 126;
        perturbXRect = perturbX * (float) tex.width;
        perturbYRect = perturbY * (float) tex.height;
        GLTextureSettings envTp = new GLTextureSettings();
        envTp.wrappingHorizontal = org.mt4j.util.opengl.GLTexture.WRAP_MODE.REPEAT;
        envTp.wrappingVertical = org.mt4j.util.opengl.GLTexture.WRAP_MODE.REPEAT;
        envTex = new GLTexture(app, (new StringBuilder(String.valueOf(waterImagePath))).append("Reflectg4.png").toString(), envTp);
        envCenterX = 0.5F;
        envCenterY = 0.5F;
        envPerTurbXRect = envPerTurbX;
        envPerTurbYRect = envPerTurbY;
        MTRectangle dummyWaterRectangle = new MTRectangle(0.0F, 0.0F, MT4jSettings.getInstance().getWindowWidth(), MT4jSettings.getInstance().getWindowHeight(), app);
        dummyWaterRectangle.setNoFill(true);
        dummyWaterRectangle.setNoStroke(true);
        dummyWaterRectangle.unregisterAllInputProcessors();
        MultipleDragProcessor drawProc = new MultipleDragProcessor(app);
        dummyWaterRectangle.registerInputProcessor(drawProc);
        dummyWaterRectangle.addGestureListener(MultipleDragProcessor.class, new GestureEventListener(this));
        getCanvas().addChild(dummyWaterRectangle);
    }

    public void drawAndUpdate(PGraphics graphics, long timeDelta) {
        counter += timeDelta;
        if (counter > 10) {
            counter -= 10;
            q((int) timeDelta);
        }
        clear(graphics);
        pgl.beginGL();
        gl.glMatrixMode(5889);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        glu.gluPerspective(60D, 1.0D, 1.0D, 1000D);
        gl.glMatrixMode(5888);
        gl.glLoadIdentity();
        glu.gluLookAt(eyex, eyey, eyez, atx, aty, atz, upx, upy, upz);
        z();
        t();
        s();
        v(gl);
        gl.glMatrixMode(5889);
        gl.glPopMatrix();
        gl.glMatrixMode(5888);
        pgl.endGL();
        super.drawAndUpdate(graphics, timeDelta);
    }

    private void q(int val) {
        u();
        m();
    }

    private void n() {
        for (int i = 0; i < 126; i++) {
            for (int j = 0; j < 126; j++) hh[i][j] = vv[i][j] = ff[i][j] = extforce[i][j] = 0.0F;
        }
    }

    private void u() {
        for (int i = 1; i < 125; i++) {
            for (int j = 1; j < 125; j++) {
                ff[i][j] = 0.0F;
                float n_this = hh[i][j];
                float n_adj = hh[i - 1][j];
                ff[i][j] += -SPRING_CONSTANT * (n_this - n_adj);
                ff[i][j] -= DAMPING_CONSTANT * (vv[i][j] - vv[i - 1][j]);
                n_adj = hh[i + 1][j];
                ff[i][j] += -SPRING_CONSTANT * (n_this - n_adj);
                ff[i][j] -= DAMPING_CONSTANT * (vv[i][j] - vv[i + 1][j]);
                n_adj = hh[i][j - 1];
                ff[i][j] += -SPRING_CONSTANT * (n_this - n_adj);
                ff[i][j] -= DAMPING_CONSTANT * (vv[i][j] - vv[i][j - 1]);
                n_adj = hh[i][j + 1];
                ff[i][j] += -SPRING_CONSTANT * (n_this - n_adj);
                ff[i][j] -= DAMPING_CONSTANT * (vv[i][j] - vv[i][j + 1]);
                ff[i][j] += extforce[i][j];
                extforce[i][j] = 0.0F;
            }
        }
    }

    private void m() {
        for (int i = 0; i < 126; i++) {
            for (int j = 0; j < 126; j++) {
                vv[i][j] += ff[i][j] * dt;
                hh[i][j] += vv[i][j];
                hh[i][j] += (double) hh[i][j] * -0.0135D;
            }
        }
        for (int j = 0; j < 125; j++) {
            for (int i = 0; i < 126; i++) {
                ToolsBuffers.setInBuffer(i, hh[i][j], j, vertBuffers[j], i * 2);
                ToolsBuffers.setInBuffer(i, hh[i][j + 1], j + 1, vertBuffers[j], i * 2 + 1);
            }
        }
    }

    private void v(GL gl) {
        gl.glPushMatrix();
        gl.glTranslatef(0.0F, 6F, 0.0F);
        gl.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        gl.glEnableClientState(32884);
        if (hasMultiTexture) {
            gl.glClientActiveTexture(33984);
            gl.glEnableClientState(32888);
            gl.glEnable(tex.getTextureTarget());
            gl.glBindTexture(tex.getTextureTarget(), tex.getTextureID());
            gl.glActiveTexture(33986);
            gl.glClientActiveTexture(33986);
            gl.glEnableClientState(32888);
            gl.glEnable(envTex.getTextureTarget());
            gl.glBindTexture(envTex.getTextureTarget(), envTex.getTextureID());
            gl.glTexEnvi(8960, 8704, 260);
            for (int j = 0; j < 125; j++) {
                FloatBuffer currBuff = vertBuffers[j];
                FloatBuffer texBuff = texBuffers[j];
                FloatBuffer texEnvBuf = texEnvBuffers[j];
                gl.glClientActiveTexture(33984);
                gl.glTexCoordPointer(2, 5126, 0, texBuff);
                gl.glClientActiveTexture(33986);
                gl.glTexCoordPointer(2, 5126, 0, texEnvBuf);
                gl.glVertexPointer(3, 5126, 0, currBuff);
                gl.glDrawArrays(5, 0, currBuff.capacity() / 3);
            }
            gl.glDisableClientState(32884);
            gl.glClientActiveTexture(33986);
            gl.glDisableClientState(32888);
            gl.glDisable(envTex.getTextureTarget());
            gl.glClientActiveTexture(33984);
            gl.glDisableClientState(32888);
            gl.glActiveTexture(33984);
            gl.glBindTexture(tex.getTextureTarget(), 0);
            gl.glDisable(tex.getTextureTarget());
        } else {
            gl.glEnableClientState(32888);
            gl.glEnable(tex.getTextureTarget());
            gl.glBindTexture(tex.getTextureTarget(), tex.getTextureID());
            for (int j = 0; j < 125; j++) {
                FloatBuffer currBuff = vertBuffers[j];
                FloatBuffer texBuff = texBuffers[j];
                gl.glTexCoordPointer(2, 5126, 0, texBuff);
                gl.glVertexPointer(3, 5126, 0, currBuff);
                gl.glDrawArrays(5, 0, currBuff.capacity() / 3);
            }
            gl.glDisableClientState(32884);
            gl.glDisableClientState(32888);
            gl.glActiveTexture(33984);
            gl.glBindTexture(tex.getTextureTarget(), 0);
            gl.glDisable(tex.getTextureTarget());
        }
        gl.glPopMatrix();
    }

    private void t() {
        for (int i = 0; i < 126; i++) {
            for (int j = 0; j < 126; j++) {
                avg[0] = avg[1] = avg[2] = 0.0F;
                if (j < 125 && i < 125) add(avg, fn[0][i][j], avg);
                if (j < 125 && i > 0) {
                    add(avg, fn[0][i - 1][j], avg);
                    add(avg, fn[1][i - 1][j], avg);
                }
                if (j > 0 && i < 125) {
                    add(avg, fn[0][i][j - 1], avg);
                    add(avg, fn[1][i][j - 1], avg);
                }
                if (j > 0 && i > 0) add(avg, fn[1][i - 1][j - 1], avg);
                norm(avg);
                copy(avg, vn[i][j]);
            }
        }
    }

    private void z() {
        for (int i = 0; i < 125; i++) {
            for (int j = 0; j < 125; j++) {
                pt0[0] = i;
                pt0[1] = hh[i][j];
                pt0[2] = j;
                pt1[0] = i + 1;
                pt1[1] = hh[i + 1][j];
                pt1[2] = j;
                pt2[0] = i;
                pt2[1] = hh[i][j + 1];
                pt2[2] = j + 1;
                pt3[0] = i + 1;
                pt3[1] = hh[i + 1][j + 1];
                pt3[2] = j + 1;
                sub(pt0, pt1, a);
                sub(pt2, pt1, b);
                sub(pt3, pt1, c);
                cross(a, b, n0);
                norm(n0);
                cross(b, c, n1);
                norm(n1);
                copy(n0, fn[0][i][j]);
                copy(n1, fn[1][i][j]);
            }
        }
    }

    private void cross(float a[], float b[], float result[]) {
        result[0] = a[1] * b[2] - a[2] * b[1];
        result[1] = a[2] * b[0] - a[0] * b[2];
        result[2] = a[0] * b[1] - a[1] * b[0];
    }

    private void add(float a[], float b[], float result[]) {
        result[0] = a[0] + b[0];
        result[1] = a[1] + b[1];
        result[2] = a[2] + b[2];
    }

    private void sub(float a[], float b[], float result[]) {
        result[0] = a[0] - b[0];
        result[1] = a[1] - b[1];
        result[2] = a[2] - b[2];
    }

    private void copy(float a[], float b[]) {
        b[0] = a[0];
        b[1] = a[1];
        b[2] = a[2];
    }

    private void norm(float v[]) {
        float vlen = ToolsMath.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] /= vlen;
        v[1] /= vlen;
        v[2] /= vlen;
    }

    public void init() {
        app.frameRate(43F);
        app.registerKeyEvent(this);
    }

    public void shutDown() {
        app.frameRate(MT4jSettings.getInstance().getMaxFrameRate());
        app.unregisterKeyEvent(this);
    }

    public void mouseEvent(MouseEvent e) {
        boolean _tmp = app.mousePressed;
    }

    public void keyEvent(KeyEvent e) {
        int evtID = e.getID();
        if (evtID != 401) return;
        switch(e.getKeyCode()) {
            case 67:
                n();
                break;
            case 70:
                System.out.println((new StringBuilder("FPS: ")).append(app.frameRate).toString());
                break;
            case 8:
                app.popScene();
                break;
            case 123:
                app.saveFrame();
                break;
            case 80:
                System.out.println((new StringBuilder("Spring Constant: ")).append(SPRING_CONSTANT).toString());
                System.out.println((new StringBuilder("Spring Damping: ")).append(DAMPING_CONSTANT).toString());
                System.out.println((new StringBuilder("Refraction Factor: ")).append(perturbX).toString());
                System.out.println((new StringBuilder("dt: ")).append(dt).toString());
                System.out.println((new StringBuilder("Reflection Factor: ")).append(envPerTurbX).toString());
                break;
        }
    }

    public boolean destroy() {
        boolean destroyed = super.destroy();
        if (destroyed) {
            tex.destroy();
            envTex.destroy();
        }
        return destroyed;
    }

    private MTApplication app;

    private final int TIMER_INTERVAL = 10;

    private final float M_PI = 3.141593F;

    private final int TANK_WIDTH = 124;

    private final int TANK_HEIGHT = 124;

    private final int TANK_DEPTH = 124;

    private final float WATER_LINE = 6F;

    private final int MESHSIZEX = 126;

    private final int MESHSIZEZ = 126;

    private float SPRING_CONSTANT;

    private float DAMPING_CONSTANT;

    private float perturbX;

    private float perturbY;

    private float envPerTurbX;

    private float envPerTurbY;

    private float dt;

    private float hh[][];

    private float ff[][];

    private float vv[][];

    private float fn[][][][];

    private float vn[][][];

    private float extforce[][];

    private final float gravity = 9.8F;

    private PGraphicsOpenGL pgl;

    private GL gl;

    private GLU glu;

    private float zoom;

    private float eyex;

    private float eyey;

    private float eyez;

    private float atx;

    private float aty;

    private float atz;

    private float upx;

    private float upy;

    private float upz;

    private float phi;

    private float theta;

    private GLTexture tex;

    private GLTexture envTex;

    private Timer timer;

    private FloatBuffer vertBuffers[];

    private FloatBuffer texBuffers[];

    private FloatBuffer texEnvBuffers[];

    private int texRatioXRect;

    private int texRatioYRect;

    private float perturbXRect;

    private float perturbYRect;

    private float envCenterX;

    private float envCenterY;

    private float envPerTurbXRect;

    private float envPerTurbYRect;

    private boolean hasMultiTexture;

    private String waterImagePath;

    private int counter;

    private float avg[];

    float a[];

    float b[];

    float c[];

    float pt0[];

    float pt1[];

    float pt2[];

    float pt3[];

    float n0[];

    float n1[];
}

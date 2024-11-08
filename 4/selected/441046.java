package octlight.renderer.lwjgl;

import net.java.games.jogl.GL;
import octlight.Camera;
import octlight.Viewer;
import octlight.base.Color;
import octlight.geometry.LineArray;
import octlight.geometry.PolygonArray;
import octlight.image.ChannelLayout;
import octlight.image.Image;
import octlight.material.Material;
import octlight.material.TextureMode;
import octlight.material.texture.Texture;
import octlight.math.Point3;
import octlight.math.Point4;
import octlight.math.Vector3;
import octlight.renderer.*;
import octlight.scene.GeometryLeaf;
import octlight.scene.Node;
import octlight.scene.RootNode;
import octlight.scene.light.DirectionalLight;
import octlight.scene.light.Light;
import octlight.scene.light.PointLight;
import octlight.scene.light.SpotLight;
import octlight.scene.visitor.NodeVisitor;
import octlight.util.BufferUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.Util;
import org.lwjgl.opengl.glu.GLU;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;

public class LwjglRenderer implements Renderer, LightHandler, TextureHandler, NodeVisitor {

    private Camera camera;

    private Viewer viewer;

    private RootNode scene;

    private int textureUnitCount = Util.glGetInteger(GL.GL_MAX_TEXTURE_UNITS);

    private LightList lightList = new LightList(8, this);

    private TextureList textureList = new TextureList(textureUnitCount, this);

    private int lastTextureCount;

    LwjglRenderer(RootNode scene) {
        this.scene = scene;
        GL11.glFrontFace(GL11.GL_CW);
        GL11.glCullFace(GL11.GL_FRONT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static void renderCamera(Camera camera) {
        GLU.gluPerspective(camera.getVerticalFOV(), camera.getAspectRatio(), 1, 10000);
    }

    private static void renderViewer(Viewer viewer) {
        GL11.glRotatef(viewer.getTilt(), -1, 0, 0);
        GL11.glRotatef(viewer.getDirection(), 0, 0, -1);
        Vector3 p = viewer.getPosition();
        GL11.glTranslatef(-p.getX(), -p.getY(), -p.getZ());
    }

    public void setViewer(Viewer viewer) {
        this.viewer = viewer;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    private static int getGlLight(int l) {
        switch(l) {
            case 0:
                return GL11.GL_LIGHT0;
            case 1:
                return GL11.GL_LIGHT1;
            case 2:
                return GL11.GL_LIGHT2;
            case 3:
                return GL11.GL_LIGHT3;
            case 4:
                return GL11.GL_LIGHT4;
            case 5:
                return GL11.GL_LIGHT5;
            case 6:
                return GL11.GL_LIGHT6;
            case 7:
                return GL11.GL_LIGHT7;
            default:
                throw new Error("Invalid Light");
        }
    }

    private void setLight(int id, Point3 pos, float w, float exp, float cutoff) {
        GL11.glLight(id, GL11.GL_POSITION, BufferUtil.asDirectFloatBuffer(new Point4(pos, w)));
        GL11.glLightf(id, GL11.GL_SPOT_EXPONENT, exp);
        GL11.glLightf(id, GL11.GL_SPOT_CUTOFF, cutoff);
    }

    public void setLight(int id, Light light) {
        Color color = light.getColor();
        FloatBuffer b = BufferUtil.asDirectFloatBuffer(color);
        int j = getGlLight(id);
        GL11.glLight(j, GL11.GL_DIFFUSE, b);
        GL11.glLight(j, GL11.GL_SPECULAR, b);
        if (light instanceof DirectionalLight) {
            DirectionalLight dl = (DirectionalLight) light;
            setLight(j, new Point3(dl.getDirection().getWorldDirection().scale(-1)), 0, 0, 180);
        } else if (light instanceof SpotLight) {
            SpotLight sl = (SpotLight) light;
            setLight(j, sl.getRay().getWorldLocation(), 1, sl.getConcentration(), sl.getSpread());
            Vector3 att = sl.getAttenuation();
            GL11.glLightf(j, GL11.GL_CONSTANT_ATTENUATION, att.getX());
            GL11.glLightf(j, GL11.GL_LINEAR_ATTENUATION, att.getY());
            GL11.glLightf(j, GL11.GL_QUADRATIC_ATTENUATION, att.getZ());
            GL11.glLight(j, GL11.GL_SPOT_DIRECTION, BufferUtil.asDirectFloatBuffer(sl.getRay().getWorldDirection()));
        } else if (light instanceof PointLight) {
            PointLight pl = (PointLight) light;
            setLight(j, pl.getLocation().getWorldLocation(), 1, 0, 180);
            Vector3 att = pl.getAttenuation();
            GL11.glLightf(j, GL11.GL_CONSTANT_ATTENUATION, att.getX());
            GL11.glLightf(j, GL11.GL_LINEAR_ATTENUATION, att.getY());
            GL11.glLightf(j, GL11.GL_QUADRATIC_ATTENUATION, att.getZ());
        }
    }

    public void enableLight(int id, boolean enable) {
        if (enable) GL11.glEnable(getGlLight(id)); else GL11.glDisable(getGlLight(id));
    }

    public void setAmbientLight(Color color) {
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, BufferUtil.asDirectFloatBuffer(color));
    }

    public void renderScene() {
        textureList.updateTextures(scene);
        lightList.clear();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        renderCamera(camera);
        renderViewer(viewer);
        GL11.glMatrixMode(GL.GL_MODELVIEW);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glLoadIdentity();
        scene.accept(this);
        Display.update();
    }

    private void renderGeometry(int type, PolygonArray geometry) {
        Material m = geometry.getMaterial();
        ArrayList<Material.TextureLayer> layers = new ArrayList<Material.TextureLayer>();
        if (m != null) {
            m.getTextureLayers(layers, textureUnitCount);
            for (int i = 0; i < layers.size(); i++) {
                GL13.glActiveTexture(GL.GL_TEXTURE0 + i);
                if (i >= lastTextureCount) GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, toGL(layers.get(i).getMode()));
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureList.getTextureId(layers.get(i).getTexture()));
            }
        }
        for (int i = layers.size(); i < lastTextureCount; i++) {
            GL13.glActiveTexture(GL.GL_TEXTURE0 + i);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        lastTextureCount = layers.size();
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, geometry.getVertices());
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glNormalPointer(0, geometry.getNormals());
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glTexCoordPointer(2, 0, geometry.getTextureCoords());
        GL11.glDrawElements(type, geometry.getIndices());
    }

    private void renderLines(LineArray lines) {
        for (int i = 0; i < lastTextureCount; i++) {
            GL13.glActiveTexture(GL.GL_TEXTURE0 + i);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }
        lastTextureCount = 0;
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, 0, lines.getVertices());
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glColorPointer(4, 0, lines.getColors());
        GL11.glDrawElements(GL11.GL_LINES, lines.getIndices());
        GL11.glEnable(GL11.GL_LIGHTING);
    }

    public void visit(GeometryLeaf geometryLeaf) {
        lightList.updateLightsFor(geometryLeaf);
        GL11.glLoadMatrix(geometryLeaf.getWorldTransform().asDirectFloatBuffer());
        if (geometryLeaf.getGeometry() instanceof PolygonArray) renderGeometry(GL11.GL_TRIANGLES, (PolygonArray) geometryLeaf.getGeometry()); else if (geometryLeaf.getGeometry() instanceof LineArray) renderLines((LineArray) geometryLeaf.getGeometry());
    }

    public void visit(Node node) {
        for (int i = 0; i < node.getLightCount(); i++) {
            lightList.addLight(node.getLight(i));
        }
        node.visitChildren(this);
        for (int i = 0; i < node.getLightCount(); i++) {
            lightList.removeLight(node.getLight(i));
        }
    }

    private static int getGLType(ChannelLayout channelLayout) {
        switch(channelLayout) {
            case RGB:
                return GL11.GL_RGB;
            case RGBA:
                return GL11.GL_RGBA;
            default:
                throw new RuntimeException("Unsupported channel layout used!");
        }
    }

    private static int toGL(TextureMode mode) {
        switch(mode) {
            case REPLACE:
                return GL.GL_REPLACE;
            case MODULATE:
                return GL.GL_MODULATE;
            case BLEND:
                return GL.GL_BLEND;
            default:
                throw new RuntimeException("Unsupported channel layout used!");
        }
    }

    public void loadTextures(Texture[] textures, int[] ids) {
        IntBuffer glIds = BufferUtils.createIntBuffer(ids.length);
        GL11.glGenTextures(glIds);
        glIds.get(ids);
        for (int i = 0; i < ids.length; i++) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ids[i]);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
            Image image = textures[i].getImage();
            byte[] data = image.getData();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.rewind();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, 3, image.getWidth(), image.getHeight(), 0, getGLType(image.getChannelLayout()), GL11.GL_UNSIGNED_BYTE, buffer);
        }
    }

    public void removeTextures(Collection<Integer> ids) {
        IntBuffer buffer = BufferUtils.createIntBuffer(ids.size());
        for (Integer i : ids) buffer.put(i);
        buffer.position(0);
        GL11.glDeleteTextures(buffer);
    }
}

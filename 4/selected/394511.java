package org.xith3d.demos.utils.phong;

import java.io.FileOutputStream;
import java.io.DataInputStream;
import org.xith3d.loaders.models.Model;
import org.xith3d.loaders.models.ModelLoader;
import org.xith3d.resources.ResLoc;
import org.xith3d.scenegraph.*;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import org.openmali.vecmath2.*;

/**
 * @author Abdul Bezrati (aka JavaCoolDude)
 */
public class JCD3DSFactory {

    private static String filename = "";

    public static void convertModel(URL url) throws Exception {
        Model model = ModelLoader.getInstance().loadModel(url);
        for (int i = 0; i < model.getShapesCount(); i++) {
            proccessShape3D(model.getShape(i));
        }
    }

    private static void proccessShape3D(Shape3D shape) {
        IndexedTriangleArray geometry = (IndexedTriangleArray) shape.getGeometry();
        int[] indices = geometry.getIndex();
        float[] texcoords = new float[geometry.getValidVertexCount() * 2];
        Point3f[] vertices = new Point3f[geometry.getValidVertexCount()];
        Vector3f[] normals = new Vector3f[vertices.length];
        TexCoord3f[] tangents = new TexCoord3f[vertices.length], binormals = new TexCoord3f[vertices.length], texcoords3f = new TexCoord3f[vertices.length];
        geometry.getTextureCoordinates(0, 0, texcoords);
        for (int i = 0; i < vertices.length; i++) {
            texcoords3f[i] = new TexCoord3f(texcoords[i * 2 + 0], texcoords[i * 2 + 1], 0);
            geometry.getCoordinate(i, vertices[i] = new Point3f());
            geometry.getNormal(i, normals[i] = new Vector3f());
        }
        TBNGenerator.generateTBN(indices, vertices, normals, tangents, binormals, texcoords3f);
        saveJCDModel(vertices, normals, tangents, binormals, texcoords3f, indices, vertices.length, filename);
    }

    private static void saveJCDModel(Tuple3f[] ve, Tuple3f[] no, TexCoord3f[] ta, TexCoord3f[] bi, TexCoord3f[] te, int[] indices, int validVertexCount, String name) {
        ByteBuffer modelInfo = ByteBuffer.allocate(ve.length * 60 + indices.length * 4 + 8);
        modelInfo.putInt(indices.length).putInt(validVertexCount);
        for (int i = 0; i < validVertexCount; i++) {
            modelInfo.putFloat(ve[i].getX()).putFloat(ve[i].getY()).putFloat(ve[i].getZ());
            modelInfo.putFloat(no[i].getX()).putFloat(no[i].getY()).putFloat(no[i].getZ());
            modelInfo.putFloat(ta[i].getS()).putFloat(ta[i].getT()).putFloat(ta[i].getP());
            modelInfo.putFloat(bi[i].getS()).putFloat(bi[i].getT()).putFloat(bi[i].getP());
            modelInfo.putFloat(te[i].getS()).putFloat(te[i].getT()).putFloat(te[i].getP());
        }
        int size = indices.length / 3;
        for (int i = 0; i < size; i++) modelInfo.putInt(indices[i * 3 + 0]).putInt(indices[i * 3 + 1]).putInt(indices[i * 3 + 2]);
        modelInfo.flip();
        try {
            new FileOutputStream(name + "JCD.jcd").getChannel().write(modelInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IndexedTriangleArray readJCDModel(String name, float scale) throws Exception {
        InputStream in = ResLoc.getResourceAsStream(name);
        ByteBuffer modelInfo = null;
        byte[] fileContents = null;
        try {
            DataInputStream dataStream = new DataInputStream(in);
            dataStream.readFully(fileContents = new byte[in.available()]);
            modelInfo = ByteBuffer.wrap(fileContents);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[] indices = new int[modelInfo.getInt()];
        Point3f[] vertices = new Point3f[modelInfo.getInt()];
        Vector3f[] normals = new Vector3f[vertices.length];
        TexCoord3f[] tangents = new TexCoord3f[vertices.length], binormals = new TexCoord3f[vertices.length], texcoords3f = new TexCoord3f[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = new Point3f(scale * modelInfo.getFloat(), scale * modelInfo.getFloat(), scale * modelInfo.getFloat());
            normals[i] = new Vector3f(modelInfo.getFloat(), modelInfo.getFloat(), modelInfo.getFloat());
            tangents[i] = new TexCoord3f(modelInfo.getFloat(), modelInfo.getFloat(), modelInfo.getFloat());
            binormals[i] = new TexCoord3f(modelInfo.getFloat(), modelInfo.getFloat(), modelInfo.getFloat());
            texcoords3f[i] = new TexCoord3f(modelInfo.getFloat(), modelInfo.getFloat(), modelInfo.getFloat());
        }
        for (int i = 0; i < indices.length; i++) indices[i] = modelInfo.getInt();
        IndexedTriangleArray geometry = new IndexedTriangleArray(vertices.length, indices.length);
        geometry.setValidVertexCount(vertices.length);
        geometry.setCoordinates(0, vertices);
        geometry.setValidIndexCount(indices.length);
        geometry.setTextureCoordinates(0, 0, texcoords3f);
        geometry.setTextureCoordinates(1, 0, tangents);
        geometry.setTextureCoordinates(2, 0, binormals);
        geometry.setIndex(indices);
        geometry.setNormals(0, normals);
        return (geometry);
    }
}

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.io.*;
import java.util.*;

/** Tube represents a "thick" curve.  It subclasses Curve, since it has
    all the properties of a an ordinary curve, but changes the way it is
    rendered.  The actual surface is created by extruding a circular profile
    along the curve.  The thickness of the Tube can be specified at each
    vertex. */
public class Tube extends Curve {

    double thickness[];

    int endsStyle;

    RenderingMesh cachedMesh;

    public static final int OPEN_ENDS = 0;

    public static final int CLOSED_ENDS = 1;

    public static final int FLAT_ENDS = 2;

    private static final int MAX_SUBDIVISIONS = 20;

    /** Create a tube, explicitly specifying all parameters. */
    public Tube(Vec3 v[], float smoothness[], double thickness[], int smoothingMethod, int endsStyle) {
        super(v, smoothness, smoothingMethod, endsStyle == CLOSED_ENDS);
        this.thickness = thickness;
        this.endsStyle = endsStyle;
    }

    /** Create a tube, explicitly specifying all parameters. */
    public Tube(MeshVertex v[], float smoothness[], double thickness[], int smoothingMethod, int endsStyle) {
        super(new Vec3[v.length], smoothness, smoothingMethod, endsStyle == CLOSED_ENDS);
        for (int i = 0; i < vertex.length; i++) vertex[i] = new MeshVertex(v[i]);
        this.thickness = thickness;
        this.endsStyle = endsStyle;
    }

    /** Create a tube based on a Curve. */
    public Tube(Curve c, double thickness[], int endsStyle) {
        super(new Vec3[c.vertex.length], c.smoothness, c.smoothingMethod, endsStyle == CLOSED_ENDS);
        for (int i = 0; i < vertex.length; i++) vertex[i].r = new Vec3(c.vertex[i].r);
        this.thickness = thickness;
        this.endsStyle = endsStyle;
    }

    /** Create a tube with uniform thickness and a smoothness of 1 at all vertices. */
    public Tube(Vec3 v[], double thickness, int smoothingMethod, int endsStyle) {
        super(v, new float[v.length], smoothingMethod, endsStyle == CLOSED_ENDS);
        this.thickness = new double[v.length];
        this.endsStyle = endsStyle;
        for (int i = 0; i < v.length; i++) {
            smoothness[i] = 1.0f;
            this.thickness[i] = thickness;
        }
    }

    /** Create an exact duplicate of this object. */
    public Object3D duplicate() {
        Curve c = (Curve) super.duplicate();
        double t[] = new double[thickness.length];
        System.arraycopy(thickness, 0, t, 0, t.length);
        Tube tube = new Tube(c, t, endsStyle);
        tube.copyTextureAndMaterial(this);
        return tube;
    }

    /** Make this object identical to another one. */
    public void copyObject(Object3D obj) {
        Tube t = (Tube) obj;
        super.copyObject(t);
        thickness = new double[t.thickness.length];
        System.arraycopy(t.thickness, 0, thickness, 0, thickness.length);
        endsStyle = t.endsStyle;
        copyTextureAndMaterial(obj);
    }

    /** Get the thickness of the tube at each vertex. */
    public double[] getThickness() {
        return thickness;
    }

    /** Set the thickness of the tube at each vertex. */
    public void setThickness(double thickness[]) {
        this.thickness = thickness;
        clearCachedMesh();
    }

    /** Set the position, smoothness, and thickness values for all points. */
    public void setShape(MeshVertex v[], float smoothness[], double thickness[]) {
        vertex = v;
        this.thickness = thickness;
        this.smoothness = smoothness;
        clearCachedMesh();
    }

    /** Get the ends style. */
    public int getEndsStyle() {
        return endsStyle;
    }

    /** Set the ends style.  This should be one of the following values:
      OPEN_ENDS, CLOSED_ENDS, or FLAT_ENDS. */
    public void setEndsStyle(int style) {
        endsStyle = style;
        closed = (style == CLOSED_ENDS);
        clearCachedMesh();
    }

    /** Determine whether this tube is a closed surface. */
    public boolean isClosed() {
        return (endsStyle != OPEN_ENDS || (thickness[0] == 0.0 && thickness[thickness.length - 1] == 0.0));
    }

    /** Make sure the ends style is consistent with the closed flag.  Generally,
      setEndsStyle() should be used instead of this method. */
    public void setClosed(boolean isClosed) {
        super.setClosed(isClosed);
        if (isClosed) endsStyle = CLOSED_ENDS; else if (endsStyle == CLOSED_ENDS) endsStyle = OPEN_ENDS;
    }

    /** Clear the cached mesh. */
    protected void clearCachedMesh() {
        super.clearCachedMesh();
        cachedMesh = null;
    }

    /** Subdivide the curve which defines this tube to the specified tolerance. */
    public Tube subdivideTube(double tol) {
        if (vertex.length < 3) return this;
        if (smoothingMethod == INTERPOLATING) return subdivideTubeInterp(tol);
        if (smoothingMethod == APPROXIMATING) return subdivideTubeApprox(tol);
        return this;
    }

    /** Subdivide the curve which defines this tube to the specified tolerance. */
    private Tube subdivideTubeApprox(double tol) {
        Tube t = this;
        MeshVertex newvert[];
        float news[];
        int i, j, p1, p2, p3, p4, count;
        int numParam = (texParam == null ? 0 : texParam.length);
        double newt[], param[][], newparam[][], paramTemp[] = new double[numParam], tol2 = tol * tol;
        boolean refine[], newrefine[];
        param = new double[t.vertex.length][numParam];
        for (i = 0; i < numParam; i++) {
            if (paramValue[i] instanceof VertexParameterValue) {
                double val[] = ((VertexParameterValue) paramValue[i]).getValue();
                for (j = 0; j < val.length; j++) param[j][i] = val[j];
            }
        }
        refine = new boolean[t.vertex.length];
        if (t.closed) {
            for (i = 0; i < refine.length; i++) refine[i] = true;
            count = refine.length;
        } else {
            for (i = 1; i < refine.length - 1; i++) refine[i] = true;
            count = refine.length - 1;
        }
        int iterations = 0;
        do {
            int len = t.vertex.length;
            newvert = new MeshVertex[len + count];
            news = new float[len + count];
            newt = new double[len + count];
            newparam = new double[len + count][numParam];
            newrefine = new boolean[len + count];
            for (i = 0, j = 0; j < len; j++) {
                p1 = j - 1;
                if (p1 < 0) p1 = (t.closed ? len - 1 : 0);
                p2 = j;
                p3 = j + 1;
                if (p3 >= len) p3 = (t.closed ? p3 - len : len - 1);
                if (!refine[j]) {
                    newvert[i] = t.vertex[j];
                    newt[i] = t.thickness[j];
                    news[i] = t.smoothness[j];
                    newparam[i] = param[j];
                } else {
                    newvert[i] = SplineMesh.calcApproxPoint(t.vertex, t.smoothness, param, paramTemp, p1, p2, p3);
                    newt[i] = calcApproxThickness(t.thickness, t.smoothness, p1, p2, p3);
                    news[i] = t.smoothness[j] * 2.0f;
                    if (news[i] > 1.0f) news[i] = 1.0f;
                    for (int k = 0; k < numParam; k++) newparam[i][k] = paramTemp[k];
                }
                i++;
                if (!refine[p2] && !refine[p3]) continue;
                newvert[i] = MeshVertex.blend(t.vertex[p2], t.vertex[p3], 0.5, 0.5);
                newt[i] = 0.5 * (t.thickness[p2] + t.thickness[p3]);
                news[i] = 1.0f;
                for (int k = 0; k < numParam; k++) newparam[i][k] = 0.5 * (param[p2][k] + param[p3][k]);
                if (newvert[i - 1].r.distance2(t.vertex[j].r) > tol2) {
                    if (newvert[i].r.distance2(newvert[i - 1].r) > tol2 && (i < 2 || newvert[i - 1].r.distance2(newvert[i - 2].r) > tol2)) {
                        newrefine[i] = newrefine[i - 1] = true;
                        if (i > 1) newrefine[i - 2] = true;
                    }
                }
                i++;
            }
            count = 0;
            for (j = 0; j < newrefine.length - 1; j++) if (newrefine[j] || newrefine[j + 1]) count++;
            if (t.closed && (newrefine[newrefine.length - 1] || newrefine[0])) count++;
            t = new Tube(newvert, news, newt, t.smoothingMethod, t.endsStyle);
            param = newparam;
            refine = newrefine;
            iterations++;
        } while (count > 0 && iterations < MAX_SUBDIVISIONS);
        t.copyTextureAndMaterial(this);
        for (i = 0; i < numParam; i++) {
            if (paramValue[i] instanceof VertexParameterValue) {
                double val[] = new double[t.vertex.length];
                for (j = 0; j < val.length; j++) val[j] = param[j][i];
                t.paramValue[i] = new VertexParameterValue(val);
            }
        }
        return t;
    }

    /** Subdivide the curve which defines this tube to the specified tolerance. */
    private Tube subdivideTubeInterp(double tol) {
        Tube t = this;
        MeshVertex newvert[];
        float news[];
        int i, j, p1, p2, p3, p4, count;
        int numParam = (texParam == null ? 0 : texParam.length);
        double newt[], param[][], newparam[][], paramTemp[] = new double[numParam], tol2 = tol * tol;
        boolean refine[], newrefine[];
        param = new double[t.vertex.length][numParam];
        for (i = 0; i < numParam; i++) {
            if (paramValue[i] instanceof VertexParameterValue) {
                double val[] = ((VertexParameterValue) paramValue[i]).getValue();
                for (j = 0; j < val.length; j++) param[j][i] = val[j];
            }
        }
        if (t.closed) refine = new boolean[t.vertex.length]; else refine = new boolean[t.vertex.length - 1];
        for (i = 0; i < refine.length; i++) refine[i] = true;
        count = refine.length;
        int iterations = 0;
        do {
            int len = t.vertex.length;
            newvert = new MeshVertex[len + count];
            news = new float[len + count];
            newt = new double[len + count];
            newparam = new double[len + count][numParam];
            newrefine = new boolean[len + count];
            for (i = 0, j = 0; j < len; j++) {
                newvert[i] = t.vertex[j];
                newt[i] = t.thickness[j];
                news[i] = t.smoothness[j] * 2.0f;
                if (news[i] > 1.0f) news[i] = 1.0f;
                newparam[i] = param[j];
                i++;
                if (j < refine.length && refine[j]) {
                    p1 = j - 1;
                    if (p1 < 0) p1 = (t.closed ? len - 1 : 0);
                    p2 = j;
                    p3 = j + 1;
                    if (p3 >= len) p3 = (t.closed ? p3 - len : len - 1);
                    p4 = j + 2;
                    if (p4 >= len) p4 = (t.closed ? p4 - len : len - 1);
                    newvert[i] = SplineMesh.calcInterpPoint(t.vertex, t.smoothness, param, paramTemp, p1, p2, p3, p4);
                    newt[i] = calcInterpThickness(t.thickness, t.smoothness, p1, p2, p3, p4);
                    news[i] = 1.0f;
                    for (int k = 0; k < numParam; k++) newparam[i][k] = paramTemp[k];
                    if (newvert[i].r.distance2(t.vertex[p2].r) > tol2 && newvert[i].r.distance2(t.vertex[p3].r) > tol2) {
                        Vec3 temp = t.vertex[p2].r.plus(t.vertex[p3].r).times(0.5);
                        if (temp.distance2(newvert[i].r) > tol2) {
                            newrefine[i] = true;
                            if (i > 0) newrefine[i - 1] = true;
                        }
                    }
                    i++;
                }
            }
            count = 0;
            for (j = 0; j < newrefine.length; j++) if (newrefine[j]) count++;
            t = new Tube(newvert, news, newt, t.smoothingMethod, t.endsStyle);
            param = newparam;
            refine = newrefine;
            iterations++;
        } while (count > 0 && iterations < MAX_SUBDIVISIONS);
        t.copyTextureAndMaterial(this);
        for (i = 0; i < numParam; i++) {
            if (paramValue[i] instanceof VertexParameterValue) {
                double val[] = new double[t.vertex.length];
                for (j = 0; j < val.length; j++) val[j] = param[j][i];
                t.paramValue[i] = new VertexParameterValue(val);
            }
        }
        return t;
    }

    public static double calcInterpThickness(double t[], float s[], int i, int j, int k, int m) {
        double w1, w2, w3, w4;
        w1 = -0.0625 * s[j];
        w2 = 0.5 - w1;
        w4 = -0.0625 * s[k];
        w3 = 0.5 - w4;
        return (w1 * t[i] + w2 * t[j] + w3 * t[k] + w4 * t[m]);
    }

    public static double calcApproxThickness(double t[], float s[], int i, int j, int k) {
        double w1 = 0.125 * s[j], w2 = 1.0 - 2.0 * w1;
        return (w1 * t[i] + w2 * t[j] + w1 * t[k]);
    }

    public boolean canSetTexture() {
        return true;
    }

    public int canConvertToTriangleMesh() {
        return APPROXIMATELY;
    }

    /** Get a rendering mesh representing the surface of this object at the
      specified accuracy. */
    public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info) {
        if (interactive && cachedMesh != null) return cachedMesh;
        Vector vert = new Vector(), norm = new Vector(), face = new Vector(), param = new Vector();
        subdivideSurface(tol, vert, norm, face, param);
        Vec3 v[] = new Vec3[vert.size()];
        for (int i = 0; i < v.length; i++) v[i] = ((MeshVertex) vert.elementAt(i)).r;
        Vec3 n[] = new Vec3[vert.size()];
        norm.copyInto(n);
        int numnorm = norm.size();
        RenderingTriangle tri[] = new RenderingTriangle[face.size()];
        for (int i = 0; i < tri.length; i++) {
            int f[] = (int[]) face.elementAt(i);
            if (f[0] >= numnorm || f[1] >= numnorm || f[2] >= numnorm) tri[i] = texMapping.mapTriangle(f[0], f[1], f[2], numnorm, numnorm, numnorm, v); else tri[i] = texMapping.mapTriangle(f[0], f[1], f[2], f[0], f[1], f[2], v);
        }
        RenderingMesh rend = new RenderingMesh(v, n, tri, texMapping, matMapping);
        if (paramValue != null) {
            ParameterValue tubeParamValue[] = new ParameterValue[paramValue.length];
            for (int i = 0; i < paramValue.length; i++) {
                if (paramValue[i] instanceof VertexParameterValue) {
                    double val[] = new double[v.length];
                    for (int j = 0; j < val.length; j++) val[j] = ((double[]) param.elementAt(j))[i];
                    tubeParamValue[i] = new VertexParameterValue(val);
                } else tubeParamValue[i] = paramValue[i];
            }
            rend.setParameters(tubeParamValue);
        }
        if (interactive) cachedMesh = rend;
        return rend;
    }

    /** When setting the texture, we need to clear the cached meshes. */
    public void setTexture(Texture tex, TextureMapping mapping) {
        super.setTexture(tex, mapping);
        cachedMesh = null;
        cachedWire = null;
    }

    /** Given a class object (for a class which implements ParameterValue), return whether this object
      allows parameter values of that type. */
    public boolean supportsParameterType(Class type) {
        return (ConstantParameterValue.class == type || VertexParameterValue.class == type);
    }

    /** Get a wireframe mesh representing the surface of this object at the
      specified accuracy. */
    public WireframeMesh getWireframeMesh() {
        if (cachedWire != null) return cachedWire;
        return (cachedWire = convertToTriangleMesh(ModellingApp.getPreferences().getInteractiveSurfaceError()).getWireframeMesh());
    }

    /** Get a triangle mesh which approximates the surface of this object at
      the specified accuracy. */
    public TriangleMesh convertToTriangleMesh(double tol) {
        Vector vert = new Vector(), norm = new Vector(), face = new Vector(), param = new Vector();
        subdivideSurface(tol, vert, norm, face, param);
        Vec3 v[] = new Vec3[vert.size()];
        for (int i = 0; i < v.length; i++) v[i] = ((MeshVertex) vert.elementAt(i)).r;
        Vec3 n[] = new Vec3[vert.size()];
        norm.copyInto(n);
        int f[][] = new int[face.size()][];
        face.copyInto(f);
        int numnorm = norm.size();
        TriangleMesh mesh = new TriangleMesh(v, f);
        mesh.copyTextureAndMaterial(this);
        if (paramValue != null) {
            ParameterValue tubeParamValue[] = new ParameterValue[paramValue.length];
            for (int i = 0; i < paramValue.length; i++) {
                if (paramValue[i] instanceof VertexParameterValue) {
                    double val[] = new double[v.length];
                    for (int j = 0; j < val.length; j++) val[j] = ((double[]) param.elementAt(j))[i];
                    tubeParamValue[i] = new VertexParameterValue(val);
                } else tubeParamValue[i] = paramValue[i];
            }
            mesh.setParameterValues(tubeParamValue);
        }
        TriangleMesh.Edge ed[] = mesh.getEdges();
        TriangleMesh.Face fc[] = mesh.getFaces();
        for (int i = 0; i < fc.length; i++) {
            if (fc[i].v1 >= numnorm) ed[fc[i].e2].smoothness = 0.0f;
            if (fc[i].v2 >= numnorm) ed[fc[i].e3].smoothness = 0.0f;
            if (fc[i].v3 >= numnorm) ed[fc[i].e1].smoothness = 0.0f;
        }
        return mesh;
    }

    /** This is a utility routine used by both getRenderingMesh() and convertToTriangleMesh().
      It subdivides the surface and fills in the vectors with lists of vertices, normals,
      faces, and parameter values. */
    private void subdivideSurface(double tol, Vector vert, Vector norm, Vector face, Vector param) {
        Tube t = subdivideTube(tol);
        Vec3 pathv[] = new Vec3[t.vertex.length];
        for (int i = 0; i < pathv.length; i++) pathv[i] = t.vertex[i].r;
        int numParam = (texParam == null ? 0 : texParam.length);
        double tubeParamVal[][] = new double[t.vertex.length][numParam];
        for (int i = 0; i < numParam; i++) {
            if (t.paramValue[i] instanceof VertexParameterValue) {
                double val[] = ((VertexParameterValue) t.paramValue[i]).getValue();
                for (int j = 0; j < tubeParamVal.length; j++) tubeParamVal[j][i] = val[j];
            } else {
                double val = t.paramValue[i].getAverageValue();
                for (int j = 0; j < tubeParamVal.length; j++) tubeParamVal[j][i] = val;
            }
        }
        double max = 0.0;
        for (int i = 0; i < t.thickness.length; i++) if (t.thickness[i] > max) max = t.thickness[i];
        double r = 0.7 * max;
        int n = 0;
        if (r > tol) n = (int) Math.ceil(Math.PI / (Math.acos(1.0 - tol / r)));
        if (n < 3) n = 3;
        Vec3 subdiv[], zdir[], updir[], xdir[];
        subdiv = new Curve(pathv, t.smoothness, t.getSmoothingMethod(), t.closed).subdivideCurve().getVertexPositions();
        xdir = new Vec3[subdiv.length];
        zdir = new Vec3[subdiv.length];
        updir = new Vec3[subdiv.length];
        xdir[0] = subdiv[1].minus(subdiv[0]);
        xdir[0].normalize();
        if (Math.abs(xdir[0].y) > Math.abs(xdir[0].z)) zdir[0] = xdir[0].cross(Vec3.vz()); else zdir[0] = xdir[0].cross(Vec3.vy());
        zdir[0].normalize();
        updir[0] = xdir[0].cross(zdir[0]);
        Vec3 dir1, dir2;
        double zfrac1, zfrac2, upfrac1, upfrac2;
        zfrac1 = xdir[0].dot(zdir[0]);
        zfrac2 = Math.sqrt(1.0 - zfrac1 * zfrac1);
        dir1 = zdir[0].minus(xdir[0].times(zfrac1));
        dir1.normalize();
        upfrac1 = xdir[0].dot(updir[0]);
        upfrac2 = Math.sqrt(1.0 - upfrac1 * upfrac1);
        dir2 = updir[0].minus(xdir[0].times(upfrac1));
        dir2.normalize();
        for (int i = 1; i < subdiv.length; i++) {
            if (i == subdiv.length - 1) {
                if (t.closed) xdir[i] = subdiv[0].minus(subdiv[subdiv.length - 2]); else xdir[i] = subdiv[subdiv.length - 1].minus(subdiv[subdiv.length - 2]);
            } else xdir[i] = subdiv[i + 1].minus(subdiv[i - 1]);
            xdir[i].normalize();
            dir1 = dir1.minus(xdir[i].times(xdir[i].dot(dir1)));
            dir1.normalize();
            dir2 = dir2.minus(xdir[i].times(xdir[i].dot(dir2)));
            dir2.normalize();
            zdir[i] = xdir[i].times(zfrac1).plus(dir1.times(zfrac2));
            updir[i] = xdir[i].times(upfrac1).plus(dir2.times(upfrac2));
        }
        double dtheta = 2.0 * Math.PI / n, theta = 0.0;
        for (int i = 0; i < pathv.length; i++) {
            int k = (pathv.length == subdiv.length ? i : 2 * i);
            Vec3 orig = pathv[i], z = zdir[k], up = updir[k];
            r = 0.5 * t.thickness[i];
            for (int j = 0; j < n; j++) {
                double sin = Math.sin(theta), cos = Math.cos(theta);
                Vec3 normal = new Vec3(cos * z.x + sin * up.x, cos * z.y + sin * up.y, cos * z.z + sin * up.z);
                norm.addElement(normal);
                MeshVertex mv = new MeshVertex(new Vec3(orig.x + r * normal.x, orig.y + r * normal.y, orig.z + r * normal.z));
                vert.addElement(mv);
                param.addElement(tubeParamVal[i]);
                theta += dtheta;
            }
        }
        for (int i = 0; i < pathv.length - 1; i++) {
            int k = i * n;
            for (int j = 0; j < n - 1; j++) {
                face.addElement(new int[] { k + j, k + j + 1, k + j + n });
                face.addElement(new int[] { k + j + 1, k + j + n + 1, k + j + n });
            }
            face.addElement(new int[] { k + n - 1, k, k + n + n - 1 });
            face.addElement(new int[] { k, k + n, k + n + n - 1 });
        }
        if (endsStyle == CLOSED_ENDS) {
            int k = (pathv.length - 1) * n;
            for (int j = 0; j < n - 1; j++) {
                face.addElement(new int[] { k + j, k + j + 1, j });
                face.addElement(new int[] { k + j + 1, j + 1, j });
            }
            face.addElement(new int[] { k + n - 1, k, n - 1 });
            face.addElement(new int[] { k, 0, n - 1 });
        } else if (endsStyle == FLAT_ENDS) {
            int k = vert.size();
            vert.addElement(new MeshVertex(t.vertex[0]));
            vert.addElement(new MeshVertex(t.vertex[t.vertex.length - 1]));
            param.addElement(tubeParamVal[0]);
            param.addElement(tubeParamVal[t.vertex.length - 1]);
            for (int i = 0; i < n - 1; i++) face.addElement(new int[] { i + 1, i, k });
            face.addElement(new int[] { 0, n - 1, k });
            k++;
            int j = n * (pathv.length - 1);
            for (int i = 0; i < n - 1; i++) face.addElement(new int[] { j + i, j + i + 1, k });
            face.addElement(new int[] { j + n - 1, j, k });
        }
    }

    public void edit(EditingWindow parent, ObjectInfo info, Runnable cb) {
        TubeEditorWindow ed = new TubeEditorWindow(parent, "Tube object '" + info.name + "'", info, cb);
        ed.setVisible(true);
    }

    /** Get a MeshViewer which can be used for viewing this mesh. */
    public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options) {
        return new TubeViewer(controller, options);
    }

    /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */
    public Tube(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException {
        super(in, theScene);
        short version = in.readShort();
        if (version < 0 || version > 1) throw new InvalidObjectException("");
        thickness = new double[vertex.length];
        if (version == 0) for (int i = 0; i < paramValue.length; i++) paramValue[i] = new VertexParameterValue(new double[vertex.length]);
        for (int i = 0; i < vertex.length; i++) {
            thickness[i] = in.readDouble();
            if (version == 0) for (int j = 0; j < paramValue.length; j++) ((VertexParameterValue) paramValue[j]).getValue()[i] = in.readDouble();
        }
        endsStyle = in.readInt();
    }

    public void writeToFile(DataOutputStream out, Scene theScene) throws IOException {
        super.writeToFile(out, theScene);
        out.writeShort(1);
        for (int i = 0; i < thickness.length; i++) out.writeDouble(thickness[i]);
        out.writeInt(endsStyle);
    }

    public Keyframe getPoseKeyframe() {
        return new NullKeyframe();
    }

    public void applyPoseKeyframe(Keyframe k) {
    }
}

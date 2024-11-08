package artofillusion;

import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.object.TriangleMesh.Edge;
import artofillusion.object.TriangleMesh.Face;
import artofillusion.object.TriangleMesh.Vertex;
import artofillusion.ui.*;
import buoy.event.*;
import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.Vector;
import artofillusion.*;

/** CreateFast3dTool is an EditingTool used for creating Curve objects. */
public class CreateFast3dTool extends EditingTool {

    Image icon, selectedIcon;

    static int counter = 1;

    Point lastPoint;

    Vector clickPoint, smoothness;

    int smoothing;

    CurveF3d theCurve, lastF3dCreated;

    CoordinateSystem coords;

    Scene scene;

    LayoutWindow lw;

    Vector PointList, ObjectsList;

    int NumberOfPoints = 0, AddedPoints = 0;

    TriMeshEditorWindow triEdit;

    TriangleMesh trianglemesh;

    boolean hideEdge[], hideFace[], showQuads;

    public static final int HANDLE_SIZE = 3;

    public CreateFast3dTool(LayoutWindow fr) {
        super(fr);
        lw = fr;
        icon = loadImage("Fast3d.gif");
        selectedIcon = loadImage("selected/Fast3d.gif");
        scene = theWindow.getScene();
        PointList = new Vector();
        ObjectsList = new Vector();
    }

    public void activate() {
        super.activate();
        theWindow.setHelpText(Translate.text("createFast3dTool.helpText"));
    }

    public void deactivate() {
        addToScene();
    }

    public int whichClicks() {
        return ALL_CLICKS;
    }

    public Image getIcon() {
        return icon;
    }

    public Image getSelectedIcon() {
        return selectedIcon;
    }

    public String getToolTipText() {
        return Translate.text("createFast3dTool.tipText");
    }

    public boolean hilightSelection() {
        return (clickPoint == null);
    }

    public void drawOverlay(Graphics g, ViewerCanvas view) {
        Scene theScene = ((LayoutWindow) theWindow).getScene();
        Camera cam = view.getCamera();
        Point p1, p2;
        int i;
        g.setPaintMode();
        g.setColor(Color.black);
        if (theCurve != null) Object3D.draw(g, cam, theCurve.getWireframeMesh(), theCurve.getBounds());
        g.setColor(Color.red);
    }

    public void mousePressed(WidgetMouseEvent e, ViewerCanvas view) {
        Graphics g = view.getComponent().getGraphics();
        Point p;
        lastPoint = e.getPoint();
        g.drawOval(e.getX(), e.getY(), 2, 2);
        AddCurvePoint(lastPoint);
        NumberOfPoints++;
        AddedPoints++;
        g.dispose();
    }

    public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view) {
        Graphics g = view.getComponent().getGraphics();
        Point p;
        lastPoint = e.getPoint();
        g.drawOval(e.getX(), e.getY(), 1, 1);
        if ((NumberOfPoints % 2) == 0) {
            AddCurvePoint(lastPoint);
            AddedPoints++;
        }
        NumberOfPoints++;
        g.dispose();
    }

    public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view) {
        Graphics g = view.getComponent().getGraphics();
        Camera cam = view.getCamera();
        Point p, dragPoint = e.getPoint();
        Vec3 vertex[], orig, ydir, zdir;
        float s[];
        int i;
        vertex = new Vec3[AddedPoints];
        s = new float[AddedPoints];
        orig = new Vec3();
        for (i = 0; i < PointList.size(); i++) {
            Point punto = (Point) PointList.elementAt(i);
            vertex[i] = cam.convertScreenToWorld(punto, ModellingApp.DIST_TO_SCREEN);
            s[i] = (float) 2;
            orig = orig.plus(vertex[i]);
        }
        orig = orig.times(1.0 / vertex.length);
        ydir = cam.getViewToWorld().timesDirection(Vec3.vy());
        zdir = cam.getViewToWorld().timesDirection(new Vec3(0.0, 0.0, -1.0));
        coords = new CoordinateSystem(orig, zdir, ydir);
        for (i = 0; i < PointList.size(); i++) {
            vertex[i] = coords.toLocal().times(vertex[i]);
            vertex[i].z = 0.0;
        }
        theCurve = new CurveF3d(vertex, true);
        setLastF3dCreated(theCurve);
        cam.setObjectTransform(coords.fromLocal());
        boolean selected[] = null;
        trianglemesh = (TriangleMesh) extrudeMesh(theCurve.convertToTriangleMesh(0.1), coords, new Vec3(0, 0, 1), 1, 0.0, true);
        trianglemesh.setSmoothingMethod(TriangleMesh.APPROXIMATING);
        addToScene();
        trianglemesh = TriangleMesh.subdivideLoop(trianglemesh, selected, Double.MAX_VALUE);
        addToScene();
        trianglemesh = null;
        view.drawImage(g);
        drawOverlay(g, view);
        PointList.clear();
        AddedPoints = 0;
        NumberOfPoints = 0;
        g.dispose();
    }

    public void keyPressed(KeyPressedEvent e, ViewerCanvas view) {
        if (e.getKeyCode() == KeyPressedEvent.VK_ENTER && theCurve != null) {
            theCurve.setClosed(e.isControlDown());
            addToScene();
        }
    }

    /** Add the curve to the scene. */
    private void addToScene() {
        boolean addCurve = (theCurve != null);
        if (addCurve) {
            ObjectInfo info = new ObjectInfo(theCurve, coords, "CurveF3d " + (counter++));
            info.addTrack(new PositionTrack(info), 0);
            info.addTrack(new RotationTrack(info), 1);
            UndoRecord undo = new UndoRecord(theWindow, false);
            undo.addCommandAtBeginning(UndoRecord.SET_SCENE_SELECTION, new Object[] { theWindow.getScene().getSelection() });
            ((LayoutWindow) theWindow).addObject(info, undo);
            theWindow.setUndoRecord(undo);
            ((LayoutWindow) theWindow).setSelection(theWindow.getScene().getNumObjects() - 1);
            lw.setLastFast3dInfo(info);
        }
        boolean addTri = (trianglemesh != null);
        if (addTri) {
            ObjectInfo info = new ObjectInfo(trianglemesh, coords, "MetaF3d " + (counter++));
            info.addTrack(new PositionTrack(info), 0);
            info.addTrack(new RotationTrack(info), 1);
            UndoRecord undo = new UndoRecord(theWindow, false);
            undo.addCommandAtBeginning(UndoRecord.SET_SCENE_SELECTION, new Object[] { theWindow.getScene().getSelection() });
            ((LayoutWindow) theWindow).addObject(info, undo);
            theWindow.setUndoRecord(undo);
            ((LayoutWindow) theWindow).setSelection(theWindow.getScene().getNumObjects() - 1);
            lw.setLastFast3dInfo(info);
        }
        clickPoint = null;
        smoothness = null;
        theCurve = null;
        coords = null;
        if (addCurve || addTri) theWindow.updateImage();
    }

    public void AddCurvePoint(Point p) {
        PointList.add(p);
    }

    public void setLastF3dCreated(CurveF3d f3d) {
        lw.setLastF3dCreated(f3d);
    }

    public CurveF3d getLastF3dCreated() {
        return lw.getLastF3dCreated();
    }

    public Object3D extrudeMesh(TriangleMesh profile, CoordinateSystem profCoords, Vec3 dir, int segments, double angle, boolean orient) {
        Vec3 v[] = new Vec3[segments + 1];
        float smooth[] = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = new Vec3(dir);
            v[i].scale(i * segments);
            smooth[i] = 1.0f;
        }
        Curve path = new Curve(v, smooth, Mesh.INTERPOLATING, false);
        return extrudeMesh(profile, path, profCoords, new CoordinateSystem(), angle, orient);
    }

    /** Extrude a triangle mesh into a solid object.
      
      @param profile     the TriangleMesh to extrude
      @param path        the path along which to extrude it
      @param profCoords  the coordinate system of the profile
      @param pathCoords  the coordinate system of the path
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */
    public Object3D extrudeMesh(TriangleMesh profile, Curve path, CoordinateSystem profCoords, CoordinateSystem pathCoords, double angle, boolean orient) {
        Vertex profVert[] = (Vertex[]) profile.getVertices();
        MeshVertex pathVert[] = path.getVertices();
        Edge profEdge[] = profile.getEdges();
        Face profFace[] = profile.getFaces();
        Vec3 profv[] = new Vec3[profVert.length], pathv[] = new Vec3[pathVert.length];
        Vec3 subdiv[], center, zdir[], updir[], t[], v[];
        float pathSmooth[] = path.getSmoothness();
        CoordinateSystem localCoords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
        Mat4 rotate;
        int numBoundaryEdges = 0, numBoundaryPoints = 0, i, j, k;
        int boundaryEdge[], boundaryPoint[];
        for (i = 0; i < profVert.length; i++) profv[i] = profCoords.fromLocal().timesDirection(profVert[i].r);
        for (i = 0; i < pathVert.length; i++) pathv[i] = pathCoords.fromLocal().timesDirection(pathVert[i].r);
        if (path.getSmoothingMethod() == Mesh.NO_SMOOTHING) for (i = 0; i < pathSmooth.length; i++) pathSmooth[i] = 0.0f;
        boolean onBound[] = new boolean[profv.length];
        for (i = 0; i < profEdge.length; i++) if (profEdge[i].f2 == -1) {
            numBoundaryEdges++;
            onBound[profEdge[i].v1] = onBound[profEdge[i].v2] = true;
        }
        for (i = 0; i < onBound.length; i++) if (onBound[i]) numBoundaryPoints++;
        boundaryEdge = new int[numBoundaryEdges];
        boundaryPoint = new int[numBoundaryPoints];
        for (i = 0, j = 0; i < profEdge.length; i++) if (profEdge[i].f2 == -1) boundaryEdge[j++] = i;
        for (i = 0, j = 0; i < onBound.length; i++) if (onBound[i]) boundaryPoint[j++] = i;
        boolean forward[] = new boolean[boundaryEdge.length];
        int edgeVertIndex[][] = new int[boundaryEdge.length][2];
        for (i = 0; i < boundaryEdge.length; i++) {
            Edge ed = profEdge[boundaryEdge[i]];
            Face fc = profFace[ed.f1];
            forward[i] = ((fc.v1 == ed.v1 && fc.v2 == ed.v2) || (fc.v2 == ed.v1 && fc.v3 == ed.v2) || (fc.v3 == ed.v1 && fc.v1 == ed.v2));
            for (j = 0; j < boundaryPoint.length; j++) {
                if (boundaryPoint[j] == ed.v1) edgeVertIndex[i][0] = j; else if (boundaryPoint[j] == ed.v2) edgeVertIndex[i][1] = j;
            }
        }
        int index[][];
        if (path.isClosed()) {
            index = new int[pathv.length + 1][boundaryPoint.length];
            for (i = 0; i < boundaryPoint.length; i++) {
                for (j = 0; j < pathv.length; j++) index[j][i] = j * boundaryPoint.length + i;
                index[j][i] = i;
            }
        } else {
            index = new int[pathv.length][boundaryPoint.length];
            for (i = 0; i < boundaryPoint.length; i++) {
                index[0][i] = boundaryPoint[i];
                index[pathv.length - 1][i] = boundaryPoint[i] + profv.length;
                for (j = 1; j < pathv.length - 1; j++) index[j][i] = (j - 1) * boundaryPoint.length + i + 2 * profv.length;
            }
        }
        subdiv = new Curve(pathv, pathSmooth, path.getSmoothingMethod(), path.isClosed()).subdivideCurve().getVertexPositions();
        t = new Vec3[subdiv.length];
        zdir = new Vec3[subdiv.length];
        updir = new Vec3[subdiv.length];
        t[0] = subdiv[1].minus(subdiv[0]);
        t[0].normalize();
        zdir[0] = Vec3.vz();
        updir[0] = Vec3.vy();
        Vec3 dir1, dir2;
        double zfrac1, zfrac2, upfrac1, upfrac2;
        zfrac1 = t[0].dot(zdir[0]);
        zfrac2 = Math.sqrt(1.0 - zfrac1 * zfrac1);
        dir1 = zdir[0].minus(t[0].times(zfrac1));
        dir1.normalize();
        upfrac1 = t[0].dot(updir[0]);
        upfrac2 = Math.sqrt(1.0 - upfrac1 * upfrac1);
        dir2 = updir[0].minus(t[0].times(upfrac1));
        dir2.normalize();
        for (i = 1; i < subdiv.length; i++) {
            if (i == subdiv.length - 1) {
                if (path.isClosed()) t[i] = subdiv[0].minus(subdiv[subdiv.length - 2]); else t[i] = subdiv[subdiv.length - 1].minus(subdiv[subdiv.length - 2]);
            } else t[i] = subdiv[i + 1].minus(subdiv[i - 1]);
            t[i].normalize();
            if (orient) {
                dir1 = dir1.minus(t[i].times(t[i].dot(dir1)));
                dir1.normalize();
                dir2 = dir2.minus(t[i].times(t[i].dot(dir2)));
                dir2.normalize();
                zdir[i] = t[i].times(zfrac1).plus(dir1.times(zfrac2));
                updir[i] = t[i].times(upfrac1).plus(dir2.times(upfrac2));
            } else {
                zdir[i] = zdir[i - 1];
                updir[i] = updir[i - 1];
            }
        }
        if (path.isClosed()) v = new Vec3[numBoundaryPoints * pathv.length]; else v = new Vec3[2 * profv.length + numBoundaryPoints * (pathv.length - 2)];
        Vector newEdge = new Vector(), newFace = new Vector();
        boolean angled = (profile.getSmoothingMethod() == Mesh.NO_SMOOTHING && path.getSmoothingMethod() != Mesh.NO_SMOOTHING);
        if (!path.isClosed()) {
            localCoords.setOrigin(pathv[0]);
            localCoords.setOrientation(zdir[0], updir[0]);
            for (i = 0; i < profv.length; i++) v[i] = localCoords.fromLocal().times(profv[i]);
            k = (pathv.length == subdiv.length ? pathv.length - 1 : 2 * (pathv.length - 1));
            localCoords.setOrigin(pathv[pathv.length - 1]);
            localCoords.setOrientation(zdir[k], updir[k]);
            if (angle != 0.0) {
                rotate = Mat4.axisRotation(t[k], angle);
                localCoords.transformAxes(rotate);
            }
            for (i = 0; i < profv.length; i++) v[i + profv.length] = localCoords.fromLocal().times(profv[i]);
            for (i = 0; i < profEdge.length; i++) {
                float smoothness = profEdge[i].smoothness;
                if (angled || profEdge[i].f2 == -1) smoothness = 0.0f;
                newEdge.addElement(new EdgeInfo(profEdge[i].v1, profEdge[i].v2, smoothness));
                newEdge.addElement(new EdgeInfo(profEdge[i].v1 + profv.length, profEdge[i].v2 + profv.length, smoothness));
            }
            for (i = 0; i < profFace.length; i++) {
                Face f = profFace[i];
                newFace.addElement(new int[] { f.v1, f.v2, f.v3 });
                newFace.addElement(new int[] { f.v1 + profv.length, f.v3 + profv.length, f.v2 + profv.length });
            }
        }
        for (i = 0; i < pathv.length; i++) {
            if (!path.isClosed() && i == pathv.length - 1) break;
            for (j = 0; j < boundaryEdge.length; j++) {
                int v1, v2;
                if (forward[j]) {
                    v1 = edgeVertIndex[j][0];
                    v2 = edgeVertIndex[j][1];
                } else {
                    v1 = edgeVertIndex[j][1];
                    v2 = edgeVertIndex[j][0];
                }
                newFace.addElement(new int[] { index[i][v1], index[i + 1][v1], index[i + 1][v2] });
                newFace.addElement(new int[] { index[i][v2], index[i][v1], index[i + 1][v2] });
                EdgeInfo ed1 = new EdgeInfo(index[i][v1], index[i + 1][v1], angled ? 0.0f : profVert[boundaryPoint[v1]].smoothness);
                newEdge.addElement(ed1);
                ed1 = new EdgeInfo(index[i][v2], index[i + 1][v2], angled ? 0.0f : profVert[boundaryPoint[v2]].smoothness);
                newEdge.addElement(ed1);
                ed1 = new EdgeInfo(index[i][v1], index[i + 1][v2], 1.0f);
                newEdge.addElement(ed1);
                if (path.isClosed() || i > 0) {
                    ed1 = new EdgeInfo(index[i][v1], index[i][v2], pathSmooth[i]);
                    newEdge.addElement(ed1);
                }
            }
            localCoords.setOrigin(pathv[i]);
            k = (pathv.length == subdiv.length ? i : 2 * i);
            localCoords.setOrientation(zdir[k], updir[k]);
            if (angle != 0.0) {
                rotate = Mat4.axisRotation(t[k], i * angle / (pathv.length - 1));
                localCoords.transformAxes(rotate);
            }
            for (j = 0; j < boundaryPoint.length; j++) v[index[i][j]] = localCoords.fromLocal().times(profv[boundaryPoint[j]]);
        }
        center = new Vec3();
        for (i = 0; i < v.length; i++) center.add(v[i]);
        center.scale(1.0 / v.length);
        for (i = 0; i < v.length; i++) v[i].subtract(center);
        int faces[][] = new int[newFace.size()][];
        for (i = 0; i < faces.length; i++) faces[i] = (int[]) newFace.elementAt(i);
        TriangleMesh mesh = new TriangleMesh(v, faces);
        Edge meshEdge[] = mesh.getEdges();
        for (i = 0; i < newEdge.size(); i++) {
            EdgeInfo info = (EdgeInfo) newEdge.elementAt(i);
            if (info.smoothness == 1.0f) continue;
            for (j = 0; j < meshEdge.length; j++) if ((meshEdge[j].v1 == info.v1 && meshEdge[j].v2 == info.v2) || (meshEdge[j].v1 == info.v2 && meshEdge[j].v2 == info.v1)) meshEdge[j].smoothness = info.smoothness;
        }
        mesh.setSmoothingMethod(Math.max(profile.getSmoothingMethod(), path.getSmoothingMethod()));
        mesh.makeRightSideOut();
        return mesh;
    }

    private static class EdgeInfo {

        int v1, v2;

        float smoothness;

        public EdgeInfo(int vert1, int vert2, float smooth) {
            v1 = vert1;
            v2 = vert2;
            smoothness = smooth;
        }
    }

    public void objectChanged(ObjectInfo oi) {
        oi.clearCachedMeshes();
        findQuads(oi);
    }

    private void findQuads(ObjectInfo oi) {
        TriangleMesh mesh = (TriangleMesh) oi.object;
        Vertex v[] = (Vertex[]) mesh.getVertices();
        Edge e[] = mesh.getEdges();
        Face f[] = mesh.getFaces();
        if (hideEdge == null || hideEdge.length != e.length) hideEdge = new boolean[e.length];
        if (hideFace == null) for (int i = 0; i < e.length; i++) hideEdge[i] = false; else for (int i = 0; i < e.length; i++) hideEdge[i] = (hideFace[e[i].f1] && (e[i].f2 == -1 || hideFace[e[i].f2]));
        if (!showQuads) return;
        boolean candidate[] = new boolean[e.length];
        Vec3 norm[] = new Vec3[f.length];
        for (int i = 0; i < f.length; i++) {
            Face fc = f[i];
            norm[i] = v[fc.v2].r.minus(v[fc.v1].r).cross(v[fc.v3].r.minus(v[fc.v1].r));
            double length = norm[i].length();
            if (length > 0.0) norm[i].scale(1.0 / length);
        }
        for (int i = 0; i < e.length; i++) candidate[i] = (e[i].f2 != -1 && norm[e[i].f1].dot(norm[e[i].f2]) > 0.99);
        class EdgeScore implements Comparable {

            public int edge;

            public double score;

            public EdgeScore(int edge, double score) {
                this.edge = edge;
                this.score = score;
            }

            public int compareTo(Object o) {
                double diff = score - ((EdgeScore) o).score;
                if (diff < 0.0) return -1;
                if (diff > 0.0) return 1;
                return 0;
            }
        }
        Vector scoreVec = new Vector(e.length);
        Vec3 temp0 = new Vec3(), temp1 = new Vec3(), temp2 = new Vec3();
        for (int i = 0; i < e.length; i++) {
            if (!candidate[i]) continue;
            Edge ed = e[i];
            int v1 = ed.v1, v2 = ed.v2, v3, v4;
            Face fc = f[ed.f1];
            if (fc.v1 != v1 && fc.v1 != v2) v3 = fc.v1; else if (fc.v2 != v1 && fc.v2 != v2) v3 = fc.v2; else v3 = fc.v3;
            fc = f[ed.f2];
            if (fc.v1 != v1 && fc.v1 != v2) v4 = fc.v1; else if (fc.v2 != v1 && fc.v2 != v2) v4 = fc.v2; else v4 = fc.v3;
            temp0.set(v[v1].r.minus(v[v2].r));
            temp0.normalize();
            temp1.set(v[v1].r.minus(v[v3].r));
            temp1.normalize();
            temp2.set(v[v1].r.minus(v[v4].r));
            temp2.normalize();
            if (Math.acos(temp0.dot(temp1)) + Math.acos(temp0.dot(temp2)) > Math.PI) continue;
            double dot = temp1.dot(temp2);
            double score = (dot > 0.0 ? dot : -dot);
            temp1.set(v[v2].r.minus(v[v3].r));
            temp1.normalize();
            temp2.set(v[v2].r.minus(v[v4].r));
            temp2.normalize();
            if (Math.acos(-temp0.dot(temp1)) + Math.acos(-temp0.dot(temp2)) > Math.PI) continue;
            dot = temp1.dot(temp2);
            score += (dot > 0.0 ? dot : -dot);
            scoreVec.addElement(new EdgeScore(i, score));
        }
        if (scoreVec.size() == 0) return;
        EdgeScore score[] = new EdgeScore[scoreVec.size()];
        scoreVec.copyInto(score);
        Arrays.sort(score);
        boolean hasHiddenEdge[] = new boolean[f.length];
        for (int i = 0; i < score.length; i++) {
            Edge ed = e[score[i].edge];
            if (hasHiddenEdge[ed.f1] || hasHiddenEdge[ed.f2]) continue;
            hideEdge[score[i].edge] = true;
            hasHiddenEdge[ed.f1] = hasHiddenEdge[ed.f2] = true;
        }
    }
}

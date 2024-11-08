package org.jbox2d.util.nonconvex;

import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

/**
 * 
 * @author ewjordan
 */
public class Polygon {

    static final float toiSlop = Settings.toiSlop;

    public static boolean B2_POLYGON_REPORT_ERRORS = true;

    static final float COLLAPSE_DIST_SQR = Settings.EPSILON * Settings.EPSILON;

    static int maxPolygonVertices = Settings.maxPolygonVertices;

    public int nVertices;

    public float[] x;

    public float[] y;

    boolean areaIsSet;

    float area;

    /**
	 * Check if the lines a0->a1 and b0->b1 cross.
	 * If they do, intersectionPoint will be filled
	 * with the point of crossing.
	 *
	 * Grazing lines should not return true, though
	 * this is not very robust.
	 * 
	 */
    static boolean intersect(final Vec2 a0, final Vec2 a1, final Vec2 b0, final Vec2 b1, Vec2 intersectionPoint) {
        if (a0 == b0 || a0 == b1 || a1 == b0 || a1 == b1) return false;
        float x1 = a0.x;
        float y1 = a0.y;
        float x2 = a1.x;
        float y2 = a1.y;
        float x3 = b0.x;
        float y3 = b0.y;
        float x4 = b1.x;
        float y4 = b1.y;
        if (MathUtils.max(x1, x2) < MathUtils.min(x3, x4) || MathUtils.max(x3, x4) < MathUtils.min(x1, x2)) return false;
        if (MathUtils.max(y1, y2) < MathUtils.min(y3, y4) || MathUtils.max(y3, y4) < MathUtils.min(y1, y2)) return false;
        float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3));
        float ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3));
        float denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (MathUtils.abs(denom) < Settings.EPSILON) {
            return false;
        }
        ua /= denom;
        ub /= denom;
        if ((0 < ua) && (ua < 1) && (0 < ub) && (ub < 1)) {
            intersectionPoint.x = (x1 + ua * (x2 - x1));
            intersectionPoint.y = (y1 + ua * (y2 - y1));
            return true;
        }
        return false;
    }

    boolean intersect(final Vec2 a0, final Vec2 a1, final Vec2 b0, final Vec2 b1) {
        Vec2 myVec = new Vec2(0.0f, 0.0f);
        return intersect(a0, a1, b0, b1, myVec);
    }

    public Polygon(float[] _x, float[] _y) {
        this(_x, _y, _x.length);
    }

    public Polygon(float[] _x, float[] _y, int nVert) {
        nVertices = nVert;
        x = new float[nVertices];
        y = new float[nVertices];
        for (int i = 0; i < nVertices; ++i) {
            x[i] = _x[i];
            y[i] = _y[i];
        }
        areaIsSet = false;
    }

    public Polygon(Vec2[] v) {
        this(v, v.length);
    }

    public Polygon(Vec2[] v, int nVert) {
        nVertices = nVert;
        x = new float[nVertices];
        y = new float[nVertices];
        for (int i = 0; i < nVertices; ++i) {
            x[i] = v[i].x;
            y[i] = v[i].y;
        }
        areaIsSet = false;
    }

    public Polygon(Triangle t) {
        nVertices = 3;
        x = new float[nVertices];
        y = new float[nVertices];
        for (int i = 0; i < nVertices; ++i) {
            x[i] = t.x[i];
            y[i] = t.y[i];
        }
    }

    public Polygon() {
        x = null;
        y = null;
        nVertices = 0;
        areaIsSet = false;
    }

    float getArea() {
        area = 0.0f;
        area += x[nVertices - 1] * y[0] - x[0] * y[nVertices - 1];
        for (int i = 0; i < nVertices - 1; ++i) {
            area += x[i] * y[i + 1] - x[i + 1] * y[i];
        }
        area *= .5f;
        areaIsSet = true;
        return area;
    }

    boolean isCCW() {
        return (getArea() > 0.0f);
    }

    void mergeParallelEdges(float tolerance) {
        if (nVertices <= 3) return;
        boolean[] mergeMe = new boolean[nVertices];
        int newNVertices = nVertices;
        for (int i = 0; i < nVertices; ++i) {
            int lower = (i == 0) ? (nVertices - 1) : (i - 1);
            int middle = i;
            int upper = (i == nVertices - 1) ? (0) : (i + 1);
            float dx0 = x[middle] - x[lower];
            float dy0 = y[middle] - y[lower];
            float dx1 = x[upper] - x[middle];
            float dy1 = y[upper] - y[middle];
            float norm0 = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
            float norm1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
            if (!(norm0 > 0.0f && norm1 > 0.0f) && newNVertices > 3) {
                mergeMe[i] = true;
                --newNVertices;
            }
            dx0 /= norm0;
            dy0 /= norm0;
            dx1 /= norm1;
            dy1 /= norm1;
            float cross = dx0 * dy1 - dx1 * dy0;
            float dot = dx0 * dx1 + dy0 * dy1;
            if (MathUtils.abs(cross) < tolerance && dot > 0 && newNVertices > 3) {
                mergeMe[i] = true;
                --newNVertices;
            } else {
                mergeMe[i] = false;
            }
        }
        if (newNVertices == nVertices || newNVertices == 0) {
            return;
        }
        float[] newx = new float[newNVertices];
        float[] newy = new float[newNVertices];
        int currIndex = 0;
        for (int i = 0; i < nVertices; ++i) {
            if (mergeMe[i] || newNVertices == 0 || currIndex == newNVertices) continue;
            assert (currIndex < newNVertices);
            newx[currIndex] = x[i];
            newy[currIndex] = y[i];
            ++currIndex;
        }
        x = newx;
        y = newy;
        nVertices = newNVertices;
    }

    public Vec2[] getVertexVecs() {
        Vec2[] out = new Vec2[nVertices];
        for (int i = 0; i < nVertices; ++i) {
            out[i] = new Vec2(x[i], y[i]);
        }
        return out;
    }

    public void set(final Polygon p) {
        if (nVertices != p.nVertices) {
            nVertices = p.nVertices;
            x = new float[nVertices];
            y = new float[nVertices];
        }
        for (int i = 0; i < nVertices; ++i) {
            x[i] = p.x[i];
            y[i] = p.y[i];
        }
        areaIsSet = false;
    }

    /**
	  * Assuming the polygon is simple, checks if it is convex.
	  */
    public boolean isConvex() {
        boolean isPositive = false;
        for (int i = 0; i < nVertices; ++i) {
            int lower = (i == 0) ? (nVertices - 1) : (i - 1);
            int middle = i;
            int upper = (i == nVertices - 1) ? (0) : (i + 1);
            float dx0 = x[middle] - x[lower];
            float dy0 = y[middle] - y[lower];
            float dx1 = x[upper] - x[middle];
            float dy1 = y[upper] - y[middle];
            float cross = dx0 * dy1 - dx1 * dy0;
            boolean newIsP = (cross >= 0) ? true : false;
            if (i == 0) {
                isPositive = newIsP;
            } else if (isPositive != newIsP) {
                return false;
            }
        }
        return true;
    }

    public static Vec2 polyCentroid(final Vec2[] vs, int count) {
        Vec2 c = new Vec2(0.0f, 0.0f);
        float area = 0.0f;
        final float inv3 = 1.0f / 3.0f;
        Vec2 pRef = new Vec2(0.0f, 0.0f);
        for (int i = 0; i < count; ++i) {
            Vec2 p1 = pRef;
            Vec2 p2 = vs[i];
            Vec2 p3 = i + 1 < count ? vs[i + 1] : vs[0];
            Vec2 e1 = p2.sub(p1);
            Vec2 e2 = p3.sub(p1);
            float D = Vec2.cross(e1, e2);
            float triangleArea = 0.5f * D;
            area += triangleArea;
            c.x += triangleArea * inv3 * (p1.x + p2.x + p3.x);
            c.y += triangleArea * inv3 * (p1.y + p2.y + p3.y);
        }
        c.x *= 1.0f / area;
        c.y *= 1.0f / area;
        return c;
    }

    /**
	 * Checks if polygon is valid for use in Box2d engine.
	 * Last ditch effort to ensure no invalid polygons are
	 * added to world geometry.
	 *
	 * Performs a full check, for simplicity, convexity,
	 * orientation, minimum angle, and volume.  This won't
	 * be very efficient, and a lot of it is redundant when
	 * other tools in this section are used.
	 */
    public boolean isUsable(boolean printErrors) {
        int error = -1;
        boolean noError = true;
        if (nVertices < 3 || nVertices > maxPolygonVertices) {
            noError = false;
            error = 0;
        }
        if (!isConvex()) {
            noError = false;
            error = 1;
        }
        if (!isSimple()) {
            noError = false;
            error = 2;
        }
        if (getArea() < Settings.EPSILON) {
            noError = false;
            error = 3;
        }
        Vec2[] normals = new Vec2[nVertices];
        Vec2[] vertices = new Vec2[nVertices];
        for (int i = 0; i < nVertices; ++i) {
            vertices[i] = new Vec2(x[i], y[i]);
            int i1 = i;
            int i2 = i + 1 < nVertices ? i + 1 : 0;
            Vec2 edge = new Vec2(x[i2] - x[i1], y[i2] - y[i1]);
            normals[i] = Vec2.cross(edge, 1.0f);
            normals[i].normalize();
        }
        for (int i = 0; i < nVertices; ++i) {
            int iminus = (i == 0) ? nVertices - 1 : i - 1;
            float cross = Vec2.cross(normals[iminus], normals[i]);
            cross = MathUtils.clamp(cross, -1.0f, 1.0f);
            float angle = (float) Math.asin(cross);
            if (angle <= Settings.angularSlop) {
                noError = false;
                error = 4;
                break;
            }
            for (int j = 0; j < nVertices; ++j) {
                if (j == i || j == (i + 1) % nVertices) {
                    continue;
                }
                float s = Vec2.dot(normals[i], vertices[j].sub(vertices[i]));
                if (s >= -Settings.linearSlop) {
                    noError = false;
                    error = 5;
                }
            }
            Vec2 centroid = polyCentroid(vertices, nVertices);
            Vec2 n1 = normals[iminus];
            Vec2 n2 = normals[i];
            Vec2 v = vertices[i].sub(centroid);
            Vec2 d = new Vec2();
            d.x = Vec2.dot(n1, v) - toiSlop;
            d.y = Vec2.dot(n2, v) - toiSlop;
            if ((d.x < 0.0f) || (d.y < 0.0f)) {
                noError = false;
                error = 6;
            }
        }
        if (!noError && printErrors) {
            System.out.println("Found invalid polygon, ");
            switch(error) {
                case 0:
                    System.out.println("must have between 3 and " + Settings.maxPolygonVertices + " vertices.");
                    break;
                case 1:
                    System.out.println("must be convex.\n");
                    break;
                case 2:
                    System.out.println("must be simple (cannot intersect itself).\n");
                    break;
                case 3:
                    System.out.println("area is too small.\n");
                    break;
                case 4:
                    System.out.println("sides are too close to parallel.\n");
                    break;
                case 5:
                    System.out.println("polygon is too thin.\n");
                    break;
                case 6:
                    System.out.println("core shape generation would move edge past centroid (too thin).\n");
                    break;
                default:
                    System.out.println("don't know why.\n");
                    assert false;
            }
        }
        return noError;
    }

    public boolean isUsable() {
        return isUsable(B2_POLYGON_REPORT_ERRORS);
    }

    public boolean isSimple() {
        for (int i = 0; i < nVertices; ++i) {
            int iplus = (i + 1 > nVertices - 1) ? 0 : i + 1;
            Vec2 a1 = new Vec2(x[i], y[i]);
            Vec2 a2 = new Vec2(x[iplus], y[iplus]);
            for (int j = i + 1; j < nVertices; ++j) {
                int jplus = (j + 1 > nVertices - 1) ? 0 : j + 1;
                Vec2 b1 = new Vec2(x[j], y[j]);
                Vec2 b2 = new Vec2(x[jplus], y[jplus]);
                if (intersect(a1, a2, b1, b2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	     * Tries to add a triangle to the polygon. Returns null if it can't connect
	     * properly, otherwise returns a pointer to the new Polygon. Assumes bitwise
	     * equality of joined vertex positions.
		 *
		 * Remember to delete the pointer afterwards.
		 * TODO: Make this return a Polygon instead
		 * of a pointer to a heap-allocated one.
		 *
		 * For internal use.
	     */
    public Polygon add(Triangle t) {
        int firstP = -1;
        int firstT = -1;
        int secondP = -1;
        int secondT = -1;
        for (int i = 0; i < nVertices; i++) {
            if (t.x[0] == x[i] && t.y[0] == y[i]) {
                if (firstP == -1) {
                    firstP = i;
                    firstT = 0;
                } else {
                    secondP = i;
                    secondT = 0;
                }
            } else if (t.x[1] == x[i] && t.y[1] == y[i]) {
                if (firstP == -1) {
                    firstP = i;
                    firstT = 1;
                } else {
                    secondP = i;
                    secondT = 1;
                }
            } else if (t.x[2] == x[i] && t.y[2] == y[i]) {
                if (firstP == -1) {
                    firstP = i;
                    firstT = 2;
                } else {
                    secondP = i;
                    secondT = 2;
                }
            } else {
            }
        }
        if (firstP == 0 && secondP == nVertices - 1) {
            firstP = nVertices - 1;
            secondP = 0;
        }
        if (secondP == -1) {
            return null;
        }
        int tipT = 0;
        if (tipT == firstT || tipT == secondT) tipT = 1;
        if (tipT == firstT || tipT == secondT) tipT = 2;
        float[] newx = new float[nVertices + 1];
        float[] newy = new float[nVertices + 1];
        int currOut = 0;
        for (int i = 0; i < nVertices; i++) {
            newx[currOut] = x[i];
            newy[currOut] = y[i];
            if (i == firstP) {
                ++currOut;
                newx[currOut] = t.x[tipT];
                newy[currOut] = t.y[tipT];
            }
            ++currOut;
        }
        Polygon result = new Polygon(newx, newy, nVertices + 1);
        return result;
    }

    /**
	     * Adds this polygon to a PolyDef.
	     */
    public void addTo(PolygonDef pd) {
        if (nVertices < 3) return;
        Vec2[] vecs = getVertexVecs();
        assert (nVertices <= Settings.maxPolygonVertices);
        int offset = 0;
        for (int i = 0; i < nVertices; ++i) {
            if (vecs[i].x == vecs[remainder(i + 1, nVertices)].x && vecs[i].y == vecs[remainder(i + 1, nVertices)].y) {
                offset++;
                continue;
            }
            pd.vertices.add(vecs[i]);
        }
    }

    /**
		 * Finds and fixes "pinch points," points where two polygon
		 * vertices are at the same point.
		 *
		 * If a pinch point is found, pin is broken up into poutA and poutB
		 * and true is returned; otherwise, returns false.
		 *
		 * Mostly for internal use.
		 * 
		 * O(N^2) time, which sucks...
		 */
    private static boolean resolvePinchPoint(final Polygon pin, Polygon poutA, Polygon poutB) {
        if (pin.nVertices < 3) return false;
        float tol = .001f;
        boolean hasPinchPoint = false;
        int pinchIndexA = -1;
        int pinchIndexB = -1;
        for (int i = 0; i < pin.nVertices; ++i) {
            for (int j = i + 1; j < pin.nVertices; ++j) {
                if (MathUtils.abs(pin.x[i] - pin.x[j]) < tol && MathUtils.abs(pin.y[i] - pin.y[j]) < tol && j != i + 1) {
                    pinchIndexA = i;
                    pinchIndexB = j;
                    hasPinchPoint = true;
                    break;
                }
            }
            if (hasPinchPoint) break;
        }
        if (hasPinchPoint) {
            int sizeA = pinchIndexB - pinchIndexA;
            if (sizeA == pin.nVertices) return false;
            float[] xA = new float[sizeA];
            float[] yA = new float[sizeA];
            for (int i = 0; i < sizeA; ++i) {
                int ind = remainder(pinchIndexA + i, pin.nVertices);
                xA[i] = pin.x[ind];
                yA[i] = pin.y[ind];
            }
            Polygon tempA = new Polygon(xA, yA, sizeA);
            poutA.set(tempA);
            int sizeB = pin.nVertices - sizeA;
            float[] xB = new float[sizeB];
            float[] yB = new float[sizeB];
            for (int i = 0; i < sizeB; ++i) {
                int ind = remainder(pinchIndexB + i, pin.nVertices);
                xB[i] = pin.x[ind];
                yB[i] = pin.y[ind];
            }
            Polygon tempB = new Polygon(xB, yB, sizeB);
            poutB.set(tempB);
        }
        return hasPinchPoint;
    }

    /**
	     * Triangulates a polygon using simple ear-clipping algorithm. Returns
	     * size of Triangle array unless the polygon can't be triangulated.
	     * This should only happen if the polygon self-intersects,
	     * though it will not _always_ return null for a bad polygon - it is the
	     * caller's responsibility to check for self-intersection, and if it
	     * doesn't, it should at least check that the return value is non-null
	     * before using. You're warned!
		 *
		 * Triangles may be degenerate, especially if you have identical points
		 * in the input to the algorithm.  Check this before you use them.
	     *
	     * This is totally unoptimized, so for large polygons it should not be part
	     * of the simulation loop.
	     *
	     * Returns:
	     * -1 if algorithm fails (self-intersection most likely)
	     * 0 if there are not enough vertices to triangulate anything.
	     * Number of triangles if triangulation was successful.
	     *
	     * results will be filled with results - ear clipping always creates vNum - 2
	     * or fewer (due to pinch point polygon snipping), so allocate an array of
		 * this size.
	     */
    public static int triangulatePolygon(float[] xv, float[] yv, int vNum, Triangle[] results) {
        if (vNum < 3) return 0;
        Polygon pA = new Polygon();
        Polygon pB = new Polygon();
        Polygon pin = new Polygon(xv, yv, vNum);
        if (resolvePinchPoint(pin, pA, pB)) {
            Triangle[] mergeA = new Triangle[pA.nVertices];
            Triangle[] mergeB = new Triangle[pB.nVertices];
            for (int i = 0; i < pA.nVertices; ++i) {
                mergeA[i] = new Triangle();
            }
            for (int i = 0; i < pB.nVertices; ++i) {
                mergeB[i] = new Triangle();
            }
            int nA = triangulatePolygon(pA.x, pA.y, pA.nVertices, mergeA);
            int nB = triangulatePolygon(pB.x, pB.y, pB.nVertices, mergeB);
            if (nA == -1 || nB == -1) {
                return -1;
            }
            for (int i = 0; i < nA; ++i) {
                results[i].set(mergeA[i]);
            }
            for (int i = 0; i < nB; ++i) {
                results[nA + i].set(mergeB[i]);
            }
            return (nA + nB);
        }
        Triangle[] buffer = new Triangle[vNum - 2];
        for (int i = 0; i < buffer.length; ++i) {
            buffer[i] = new Triangle();
        }
        int bufferSize = 0;
        float[] xrem = new float[vNum];
        float[] yrem = new float[vNum];
        for (int i = 0; i < vNum; ++i) {
            xrem[i] = xv[i];
            yrem[i] = yv[i];
        }
        int xremLength = vNum;
        while (vNum > 3) {
            int earIndex = -1;
            float earMaxMinCross = -1000.0f;
            for (int i = 0; i < vNum; ++i) {
                if (isEar(i, xrem, yrem, vNum)) {
                    int lower = remainder(i - 1, vNum);
                    int upper = remainder(i + 1, vNum);
                    Vec2 d1 = new Vec2(xrem[upper] - xrem[i], yrem[upper] - yrem[i]);
                    Vec2 d2 = new Vec2(xrem[i] - xrem[lower], yrem[i] - yrem[lower]);
                    Vec2 d3 = new Vec2(xrem[lower] - xrem[upper], yrem[lower] - yrem[upper]);
                    d1.normalize();
                    d2.normalize();
                    d3.normalize();
                    float cross12 = MathUtils.abs(Vec2.cross(d1, d2));
                    float cross23 = MathUtils.abs(Vec2.cross(d2, d3));
                    float cross31 = MathUtils.abs(Vec2.cross(d3, d1));
                    float minCross = MathUtils.min(cross12, MathUtils.min(cross23, cross31));
                    if (minCross > earMaxMinCross) {
                        earIndex = i;
                        earMaxMinCross = minCross;
                    }
                }
            }
            if (earIndex == -1) {
                if (B2_POLYGON_REPORT_ERRORS) {
                    Polygon dump = new Polygon(xrem, yrem, vNum);
                    System.out.println("Couldn't find an ear, dumping remaining poly:\n");
                    dump.printFormatted();
                    System.out.println("Please submit this dump to ewjordan at Box2d forums\n");
                }
                for (int i = 0; i < bufferSize; i++) {
                    results[i].set(buffer[i]);
                }
                if (bufferSize > 0) return bufferSize; else return -1;
            }
            --vNum;
            float[] newx = new float[vNum];
            float[] newy = new float[vNum];
            int currDest = 0;
            for (int i = 0; i < vNum; ++i) {
                if (currDest == earIndex) ++currDest;
                newx[i] = xrem[currDest];
                newy[i] = yrem[currDest];
                ++currDest;
            }
            int under = (earIndex == 0) ? (vNum) : (earIndex - 1);
            int over = (earIndex == vNum) ? 0 : (earIndex + 1);
            Triangle toAdd = new Triangle(xrem[earIndex], yrem[earIndex], xrem[over], yrem[over], xrem[under], yrem[under]);
            buffer[bufferSize] = toAdd;
            ++bufferSize;
            xrem = newx;
            yrem = newy;
        }
        Triangle toAdd = new Triangle(xrem[1], yrem[1], xrem[2], yrem[2], xrem[0], yrem[0]);
        buffer[bufferSize] = toAdd;
        ++bufferSize;
        assert (bufferSize == xremLength - 2);
        for (int i = 0; i < bufferSize; i++) {
            results[i].set(buffer[i]);
        }
        return bufferSize;
    }

    /**
		 * Turns a list of triangles into a list of convex polygons. Very simple
	     * method - start with a seed triangle, keep adding triangles to it until
	     * you can't add any more without making the polygon non-convex.
	     *
	     * Returns an integer telling how many polygons were created.  Will fill
	     * polys array up to polysLength entries, which may be smaller or larger
	     * than the return value.
	     * 
	     * Takes O(N*P) where P is the number of resultant polygons, N is triangle
	     * count.
	     * 
	     * The final polygon list will not necessarily be minimal, though in
	     * practice it works fairly well.
	     */
    public static int polygonizeTriangles(Triangle[] triangulated, int triangulatedLength, Polygon[] polys, int polysLength) {
        int polyIndex = 0;
        if (triangulatedLength <= 0) {
            return 0;
        } else {
            int[] covered = new int[triangulatedLength];
            for (int i = 0; i < triangulatedLength; ++i) {
                covered[i] = 0;
                if (((triangulated[i].x[0] == triangulated[i].x[1]) && (triangulated[i].y[0] == triangulated[i].y[1])) || ((triangulated[i].x[1] == triangulated[i].x[2]) && (triangulated[i].y[1] == triangulated[i].y[2])) || ((triangulated[i].x[0] == triangulated[i].x[2]) && (triangulated[i].y[0] == triangulated[i].y[2]))) {
                    covered[i] = 1;
                }
            }
            boolean notDone = true;
            while (notDone) {
                int currTri = -1;
                for (int i = 0; i < triangulatedLength; ++i) {
                    if (covered[i] != 0) continue;
                    currTri = i;
                    break;
                }
                if (currTri == -1) {
                    notDone = false;
                } else {
                    Polygon poly = new Polygon(triangulated[currTri]);
                    covered[currTri] = 1;
                    int index = 0;
                    for (int i = 0; i < 2 * triangulatedLength; ++i, ++index) {
                        while (index >= triangulatedLength) index -= triangulatedLength;
                        if (covered[index] != 0) {
                            continue;
                        }
                        Polygon newP = poly.add(triangulated[index]);
                        if (newP == null) {
                            continue;
                        }
                        if (newP.nVertices > maxPolygonVertices) {
                            newP = null;
                            continue;
                        }
                        if (newP.isConvex()) {
                            poly.set(newP);
                            newP = null;
                            covered[index] = 1;
                        } else {
                            newP = null;
                        }
                    }
                    if (polyIndex < polysLength) {
                        poly.mergeParallelEdges(Settings.angularSlop);
                        if (poly.nVertices >= 3) polys[polyIndex].set(poly);
                    }
                    if (poly.nVertices >= 3) polyIndex++;
                }
            }
        }
        return polyIndex;
    }

    /**
		 * Checks if vertex i is the tip of an ear in polygon defined by xv[] and
	     * yv[].
		 *
		 * Assumes clockwise orientation of polygon...ick
	     */
    private static boolean isEar(int i, float[] xv, float[] yv, int xvLength) {
        float dx0, dy0, dx1, dy1;
        dx0 = dy0 = dx1 = dy1 = 0;
        if (i >= xvLength || i < 0 || xvLength < 3) {
            return false;
        }
        int upper = i + 1;
        int lower = i - 1;
        if (i == 0) {
            dx0 = xv[0] - xv[xvLength - 1];
            dy0 = yv[0] - yv[xvLength - 1];
            dx1 = xv[1] - xv[0];
            dy1 = yv[1] - yv[0];
            lower = xvLength - 1;
        } else if (i == xvLength - 1) {
            dx0 = xv[i] - xv[i - 1];
            dy0 = yv[i] - yv[i - 1];
            dx1 = xv[0] - xv[i];
            dy1 = yv[0] - yv[i];
            upper = 0;
        } else {
            dx0 = xv[i] - xv[i - 1];
            dy0 = yv[i] - yv[i - 1];
            dx1 = xv[i + 1] - xv[i];
            dy1 = yv[i + 1] - yv[i];
        }
        float cross = dx0 * dy1 - dx1 * dy0;
        if (cross > 0) return false;
        Triangle myTri = new Triangle(xv[i], yv[i], xv[upper], yv[upper], xv[lower], yv[lower]);
        for (int j = 0; j < xvLength; ++j) {
            if (j == i || j == lower || j == upper) continue;
            if (myTri.containsPoint(xv[j], yv[j])) return false;
        }
        return true;
    }

    public static void reversePolygon(Polygon p) {
        reversePolygon(p.x, p.y, p.nVertices);
        if (p.areaIsSet) p.area *= -1;
    }

    public static void reversePolygon(float[] x, float[] y, int n) {
        if (n == 1) return;
        int low = 0;
        int high = n - 1;
        while (low < high) {
            float buffer = x[low];
            x[low] = x[high];
            x[high] = buffer;
            buffer = y[low];
            y[low] = y[high];
            y[high] = buffer;
            ++low;
            --high;
        }
    }

    /**
		 * Decomposes a non-convex polygon into a number of convex polygons, up
	     * to maxPolys (remaining pieces are thrown out, but the total number
		 * is returned, so the return value can be greater than maxPolys).
	     *
	     * Each resulting polygon will have no more than maxVerticesPerPolygon
	     * vertices (set to b2MaxPolyVertices by default, though you can change
		 * this).
	     * 
	     * Returns -1 if operation fails (usually due to self-intersection of
		 * polygon).
	     */
    public static int decomposeConvex(Polygon p, Polygon[] results, int maxPolys) {
        if (p.nVertices < 3) return 0;
        Triangle[] triangulated = new Triangle[p.nVertices - 2];
        for (int i = 0; i < triangulated.length; ++i) {
            triangulated[i] = new Triangle();
        }
        int nTri;
        if (p.isCCW()) {
            Polygon tempP = new Polygon();
            tempP.set(p);
            reversePolygon(tempP.x, tempP.y, tempP.nVertices);
            nTri = triangulatePolygon(tempP.x, tempP.y, tempP.nVertices, triangulated);
        } else {
            nTri = triangulatePolygon(p.x, p.y, p.nVertices, triangulated);
        }
        if (nTri < 1) {
            return -1;
        }
        int nPolys = polygonizeTriangles(triangulated, nTri, results, maxPolys);
        return nPolys;
    }

    /**
		 * Decomposes a polygon into convex polygons and adds all pieces to a b2BodyDef
	     * using a prototype b2PolyDef. All fields of the prototype are used for every
	     * shape except the vertices (friction, restitution, density, filter, etc).
	     * 
	     * If you want finer control, you'll have to add everything by hand.
	     * 
	     * This is the simplest method to add a complicated polygon to a body.
		 *
	     */
    public static void decomposeConvexAndAddTo(Polygon p, Body bd, PolygonDef prototype) {
        if (p.nVertices < 3) return;
        Polygon[] decomposed = new Polygon[p.nVertices - 2];
        for (int i = 0; i < decomposed.length; ++i) {
            decomposed[i] = new Polygon();
        }
        int nPolys = decomposeConvex(p, decomposed, p.nVertices - 2);
        PolygonDef[] pdarray = new PolygonDef[2 * p.nVertices];
        for (int i = 0; i < pdarray.length; ++i) {
            pdarray[i] = new PolygonDef();
        }
        int extra = 0;
        for (int i = 0; i < nPolys; ++i) {
            PolygonDef toAdd = pdarray[i + extra];
            toAdd.set(prototype);
            Polygon curr = decomposed[i];
            if (curr.nVertices == 3) {
                for (int j = 0; j < 3; ++j) {
                    int lower = (j == 0) ? (curr.nVertices - 1) : (j - 1);
                    int middle = j;
                    int upper = (j == curr.nVertices - 1) ? (0) : (j + 1);
                    float dx0 = curr.x[middle] - curr.x[lower];
                    float dy0 = curr.y[middle] - curr.y[lower];
                    float dx1 = curr.x[upper] - curr.x[middle];
                    float dy1 = curr.y[upper] - curr.y[middle];
                    float norm0 = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
                    float norm1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
                    if (!(norm0 > 0.0f && norm1 > 0.0f)) {
                        continue;
                    }
                    dx0 /= norm0;
                    dy0 /= norm0;
                    dx1 /= norm1;
                    dy1 /= norm1;
                    float cross = dx0 * dy1 - dx1 * dy0;
                    float dot = dx0 * dx1 + dy0 * dy1;
                    if (MathUtils.abs(cross) < Settings.angularSlop && dot > 0) {
                        float dx2 = curr.x[lower] - curr.x[upper];
                        float dy2 = curr.y[lower] - curr.y[upper];
                        float norm2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);
                        if (norm2 == 0.0f) {
                            continue;
                        }
                        dx2 /= norm2;
                        dy2 /= norm2;
                        float thisArea = curr.getArea();
                        float thisHeight = 2.0f * thisArea / norm2;
                        float buffer2 = dx2;
                        dx2 = dy2;
                        dy2 = -buffer2;
                        float newX1[] = { curr.x[middle] + dx2 * thisHeight, curr.x[lower], curr.x[middle] };
                        float newY1[] = { curr.y[middle] + dy2 * thisHeight, curr.y[lower], curr.y[middle] };
                        float newX2[] = { newX1[0], curr.x[middle], curr.x[upper] };
                        float newY2[] = { newY1[0], curr.y[middle], curr.y[upper] };
                        Polygon p1 = new Polygon(newX1, newY1, 3);
                        Polygon p2 = new Polygon(newX2, newY2, 3);
                        if (p1.isUsable()) {
                            p1.addTo(toAdd);
                            bd.createShape(toAdd);
                            ++extra;
                        } else if (B2_POLYGON_REPORT_ERRORS) {
                            System.err.println("Didn't add unusable polygon.  Dumping vertices:\n");
                            p1.print();
                        }
                        if (p2.isUsable()) {
                            p2.addTo(pdarray[i + extra]);
                            bd.createShape(pdarray[i + extra]);
                        } else if (B2_POLYGON_REPORT_ERRORS) {
                            System.err.println("Didn't add unusable polygon.  Dumping vertices:\n");
                            p2.print();
                        }
                        continue;
                    }
                }
            }
            if (decomposed[i].isUsable()) {
                decomposed[i].addTo(toAdd);
                bd.createShape(toAdd);
            } else if (B2_POLYGON_REPORT_ERRORS) {
                System.out.println("Didn't add unusable polygon.  Dumping vertices:\n");
                decomposed[i].print();
            }
        }
        return;
    }

    /**
		 * Find the convex hull of a point cloud using "Gift-wrap" algorithm - start
	     * with an extremal point, and walk around the outside edge by testing
	     * angles.
	     * 
	     * Runs in O(N*S) time where S is number of sides of resulting polygon.
	     * Worst case: point cloud is all vertices of convex polygon -> O(N^2).
	     * 
	     * There may be faster algorithms to do this, should you need one -
	     * this is just the simplest. You can get O(N log N) expected time if you
	     * try, I think, and O(N) if you restrict inputs to simple polygons.
	     * 
	     * Returns null if number of vertices passed is less than 3.
	     * 
		 * Results should be passed through convex decomposition afterwards
		 * to ensure that each shape has few enough points to be used in Box2d.
		 *
	     * FIXME?: May be buggy with colinear points on hull. Couldn't find a test
	     * case that resulted in wrong behavior. If one turns up, the solution is to
	     * supplement angle check with an equality resolver that always picks the
	     * longer edge. I think the current solution is working, though it sometimes
	     * creates an extra edge along a line.
	     */
    public static Polygon convexHull(Vec2[] v, int nVert) {
        float[] cloudX = new float[nVert];
        float[] cloudY = new float[nVert];
        for (int i = 0; i < nVert; ++i) {
            cloudX[i] = v[i].x;
            cloudY[i] = v[i].y;
        }
        Polygon result = convexHull(cloudX, cloudY, nVert);
        return result;
    }

    public static Polygon convexHull(float[] cloudX, float[] cloudY, int nVert) {
        assert (nVert > 2);
        int[] edgeList = new int[nVert];
        int numEdges = 0;
        float minY = Float.MAX_VALUE;
        int minYIndex = nVert;
        for (int i = 0; i < nVert; ++i) {
            if (cloudY[i] < minY) {
                minY = cloudY[i];
                minYIndex = i;
            }
        }
        int startIndex = minYIndex;
        int winIndex = -1;
        float dx = -1.0f;
        float dy = 0.0f;
        while (winIndex != minYIndex) {
            float newdx = 0.0f;
            float newdy = 0.0f;
            float maxDot = -2.0f;
            for (int i = 0; i < nVert; ++i) {
                if (i == startIndex) continue;
                newdx = cloudX[i] - cloudX[startIndex];
                newdy = cloudY[i] - cloudY[startIndex];
                float nrm = (float) Math.sqrt(newdx * newdx + newdy * newdy);
                nrm = (nrm == 0.0f) ? 1.0f : nrm;
                newdx /= nrm;
                newdy /= nrm;
                float newDot = newdx * dx + newdy * dy;
                if (newDot > maxDot) {
                    maxDot = newDot;
                    winIndex = i;
                }
            }
            edgeList[numEdges++] = winIndex;
            dx = cloudX[winIndex] - cloudX[startIndex];
            dy = cloudY[winIndex] - cloudY[startIndex];
            float nrm = (float) Math.sqrt(dx * dx + dy * dy);
            nrm = (nrm == 0.0f) ? 1.0f : nrm;
            dx /= nrm;
            dy /= nrm;
            startIndex = winIndex;
        }
        float[] xres = new float[numEdges];
        float[] yres = new float[numEdges];
        for (int i = 0; i < numEdges; i++) {
            xres[i] = cloudX[edgeList[i]];
            yres[i] = cloudY[edgeList[i]];
        }
        Polygon returnVal = new Polygon(xres, yres, numEdges);
        returnVal.mergeParallelEdges(Settings.angularSlop);
        return returnVal;
    }

    static boolean isRighter(float sinA, float cosA, float sinB, float cosB) {
        if (sinA < 0) {
            if (sinB > 0 || cosA <= cosB) return true; else return false;
        } else {
            if (sinB < 0 || cosA <= cosB) return false; else return true;
        }
    }

    private static final int remainder(int x, int modulus) {
        int rem = x % modulus;
        while (rem < 0) {
            rem += modulus;
        }
        return rem;
    }

    public static Polygon traceEdge(Polygon p) {
        PolyNode[] nodes = new PolyNode[p.nVertices * p.nVertices];
        int nNodes = 0;
        for (int i = 0; i < nodes.length; ++i) {
            nodes[i] = new PolyNode();
        }
        for (int i = 0; i < p.nVertices; ++i) {
            Vec2 pos = new Vec2(p.x[i], p.y[i]);
            nodes[i].position = pos.clone();
            ++nNodes;
            int iplus = (i == p.nVertices - 1) ? 0 : i + 1;
            int iminus = (i == 0) ? p.nVertices - 1 : i - 1;
            nodes[i].addConnection(nodes[iplus]);
            nodes[i].addConnection(nodes[iminus]);
        }
        boolean dirty = true;
        int counter = 0;
        while (dirty) {
            dirty = false;
            for (int i = 0; i < nNodes; ++i) {
                for (int j = 0; j < nodes[i].nConnected; ++j) {
                    for (int k = 0; k < nNodes; ++k) {
                        if (k == i || nodes[k] == nodes[i].connected[j]) continue;
                        for (int l = 0; l < nodes[k].nConnected; ++l) {
                            if (nodes[k].connected[l] == nodes[i].connected[j] || nodes[k].connected[l] == nodes[i]) continue;
                            Vec2 intersectPt = new Vec2();
                            boolean crosses = intersect(nodes[i].position, nodes[i].connected[j].position, nodes[k].position, nodes[k].connected[l].position, intersectPt);
                            if (crosses) {
                                dirty = true;
                                PolyNode connj = nodes[i].connected[j];
                                PolyNode connl = nodes[k].connected[l];
                                nodes[i].connected[j].removeConnection(nodes[i]);
                                nodes[i].removeConnection(connj);
                                nodes[k].connected[l].removeConnection(nodes[k]);
                                nodes[k].removeConnection(connl);
                                nodes[nNodes] = new PolyNode(intersectPt);
                                nodes[nNodes].addConnection(nodes[i]);
                                nodes[i].addConnection(nodes[nNodes]);
                                nodes[nNodes].addConnection(nodes[k]);
                                nodes[k].addConnection(nodes[nNodes]);
                                nodes[nNodes].addConnection(connj);
                                connj.addConnection(nodes[nNodes]);
                                nodes[nNodes].addConnection(connl);
                                connl.addConnection(nodes[nNodes]);
                                ++nNodes;
                                break;
                            }
                            if (dirty) break;
                        }
                        if (dirty) break;
                    }
                    if (dirty) break;
                }
                if (dirty) break;
            }
            ++counter;
        }
        boolean foundDupe = true;
        int nActive = nNodes;
        while (foundDupe) {
            foundDupe = false;
            for (int i = 0; i < nNodes; ++i) {
                if (nodes[i].nConnected == 0) continue;
                for (int j = i + 1; j < nNodes; ++j) {
                    if (nodes[j].nConnected == 0) continue;
                    Vec2 diff = nodes[i].position.sub(nodes[j].position);
                    if (diff.lengthSquared() <= COLLAPSE_DIST_SQR) {
                        if (nActive <= 3) return new Polygon();
                        --nActive;
                        foundDupe = true;
                        PolyNode inode = nodes[i];
                        PolyNode jnode = nodes[j];
                        int njConn = jnode.nConnected;
                        for (int k = 0; k < njConn; ++k) {
                            PolyNode knode = jnode.connected[k];
                            assert (knode != jnode);
                            if (knode != inode) {
                                inode.addConnection(knode);
                                knode.addConnection(inode);
                            }
                            knode.removeConnection(jnode);
                        }
                        jnode.nConnected = 0;
                    }
                }
            }
        }
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        int minYIndex = -1;
        for (int i = 0; i < nNodes; ++i) {
            if (nodes[i].position.y < minY && nodes[i].nConnected > 1) {
                minY = nodes[i].position.y;
                minYIndex = i;
                maxX = nodes[i].position.x;
            } else if (nodes[i].position.y == minY && nodes[i].position.x > maxX && nodes[i].nConnected > 1) {
                minYIndex = i;
                maxX = nodes[i].position.x;
            }
        }
        Vec2 origDir = new Vec2(1.0f, 0.0f);
        Vec2[] resultVecs = new Vec2[4 * nNodes];
        int nResultVecs = 0;
        PolyNode currentNode = nodes[minYIndex];
        PolyNode startNode = currentNode;
        assert (currentNode.nConnected > 0);
        PolyNode nextNode = currentNode.getRightestConnection(origDir);
        if (nextNode == null) {
            startNode = nextNode;
        }
        resultVecs[0] = startNode.position;
        ++nResultVecs;
        while (nextNode != startNode) {
            if (nResultVecs > 4 * nNodes) {
                assert (false);
            }
            resultVecs[nResultVecs++] = nextNode.position;
            PolyNode oldNode = currentNode;
            currentNode = nextNode;
            nextNode = currentNode.getRightestConnection(oldNode);
            if (nextNode == null) break;
        }
        float[] xres = new float[nResultVecs];
        float[] yres = new float[nResultVecs];
        for (int i = 0; i < nResultVecs; ++i) {
            xres[i] = resultVecs[i].x;
            yres[i] = resultVecs[i].y;
        }
        Polygon retval = new Polygon(xres, yres, nResultVecs);
        return retval;
    }

    public void print() {
        printFormatted();
    }

    void printFormatted() {
        System.out.printf("float xv[] = {");
        for (int i = 0; i < nVertices; ++i) {
            System.out.printf("%ff,", x[i]);
        }
        System.out.printf("};\nfloat yv[] = {");
        for (int i = 0; i < nVertices; ++i) {
            System.out.printf("%ff,", y[i]);
        }
        System.out.printf("};\n");
    }
}

class PolyNode {

    static final int MAX_CONNECTED = 32;

    int nConnected;

    boolean visited;

    Vec2 position;

    PolyNode[] connected = new PolyNode[MAX_CONNECTED];

    public PolyNode() {
        nConnected = 0;
        visited = false;
    }

    public PolyNode(Vec2 pos) {
        position = pos;
        nConnected = 0;
        visited = false;
    }

    void addConnection(PolyNode toMe) {
        assert (nConnected < MAX_CONNECTED);
        for (int i = 0; i < nConnected; ++i) {
            if (connected[i] == toMe) return;
        }
        connected[nConnected] = toMe;
        ++nConnected;
    }

    void removeConnection(PolyNode fromMe) {
        boolean isFound = false;
        int foundIndex = -1;
        for (int i = 0; i < nConnected; ++i) {
            if (fromMe == connected[i]) {
                isFound = true;
                foundIndex = i;
                break;
            }
        }
        assert (isFound);
        --nConnected;
        for (int i = foundIndex; i < nConnected; ++i) {
            connected[i] = connected[i + 1];
        }
    }

    void removeConnectionByIndex(int index) {
        --nConnected;
        for (int i = index; i < nConnected; ++i) {
            connected[i] = connected[i + 1];
        }
    }

    boolean isConnectedTo(PolyNode me) {
        boolean isFound = false;
        for (int i = 0; i < nConnected; ++i) {
            if (me == connected[i]) {
                isFound = true;
                break;
            }
        }
        return isFound;
    }

    PolyNode getRightestConnection(PolyNode incoming) {
        if (nConnected == 0) assert (false);
        if (nConnected == 1) {
            return incoming;
        }
        Vec2 inDir = position.sub(incoming.position);
        float inLength = inDir.normalize();
        assert (inLength > Settings.EPSILON);
        PolyNode result = null;
        for (int i = 0; i < nConnected; ++i) {
            if (connected[i] == incoming) continue;
            Vec2 testDir = connected[i].position.sub(position);
            float testLengthSqr = testDir.lengthSquared();
            testDir.normalize();
            assert (testLengthSqr >= Polygon.COLLAPSE_DIST_SQR);
            float myCos = Vec2.dot(inDir, testDir);
            float mySin = Vec2.cross(inDir, testDir);
            if (result != null) {
                Vec2 resultDir = result.position.sub(position);
                resultDir.normalize();
                float resCos = Vec2.dot(inDir, resultDir);
                float resSin = Vec2.cross(inDir, resultDir);
                if (Polygon.isRighter(mySin, myCos, resSin, resCos)) {
                    result = connected[i];
                }
            } else {
                result = connected[i];
            }
        }
        assert (result != null);
        return result;
    }

    PolyNode getRightestConnection(Vec2 incomingDir) {
        Vec2 diff = position.sub(incomingDir);
        PolyNode temp = new PolyNode(diff);
        PolyNode res = getRightestConnection(temp);
        assert (res != null);
        return res;
    }
}

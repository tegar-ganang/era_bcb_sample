package com.isa.jump.plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.UniqueCoordinateArrayFilter;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.model.Task;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;

public class GeoUtils {

    public static final int emptyBit = 0;

    public static final int pointBit = 1;

    public static final int lineBit = 2;

    public static final int polyBit = 3;

    public GeoUtils() {
    }

    public static double mag(Coordinate q) {
        return Math.sqrt(q.x * q.x + q.y * q.y);
    }

    public static Coordinate unitVec(Coordinate q) {
        double m = mag(q);
        if (m == 0) m = 1;
        return new Coordinate(q.x / m, q.y / m);
    }

    public static Coordinate vectorAdd(Coordinate q, Coordinate r) {
        return new Coordinate(q.x + r.x, q.y + r.y);
    }

    public static Coordinate vectorBetween(Coordinate q, Coordinate r) {
        return new Coordinate(r.x - q.x, r.y - q.y);
    }

    public static Coordinate vectorTimesScalar(Coordinate q, double m) {
        return new Coordinate(q.x * m, q.y * m);
    }

    public static double dot(Coordinate p, Coordinate q) {
        return p.x * q.x + p.y * q.y;
    }

    public static Coordinate rotPt(Coordinate inpt, Coordinate rpt, double theta) {
        double tr = Math.toRadians(theta);
        double ct = Math.cos(tr);
        double st = Math.sin(tr);
        double x = inpt.x - rpt.x;
        double y = inpt.y - rpt.y;
        double xout = rpt.x + x * ct + y * st;
        double yout = rpt.y + y * ct - st * x;
        return new Coordinate(xout, yout);
    }

    public static boolean pointToRight(Coordinate pt, Coordinate p1, Coordinate p2) {
        double a = p2.x - p1.x;
        double b = p2.y - p1.y;
        double c = p1.y * a - p1.x * b;
        double fpt = a * pt.y - b * pt.x - c;
        return (fpt < 0.0);
    }

    public static Coordinate perpendicularVector(Coordinate v1, Coordinate v2, double dist, boolean toLeft) {
        Coordinate v3 = vectorBetween(v1, v2);
        Coordinate v4 = new Coordinate();
        if (toLeft) {
            v4.x = -v3.y;
            v4.y = v3.x;
        } else {
            v4.x = v3.y;
            v4.y = -v3.x;
        }
        return vectorAdd(v1, vectorTimesScalar(unitVec(v4), dist));
    }

    public static double getBearing180(Coordinate startPt, Coordinate endPt) {
        Coordinate r = new Coordinate(endPt.x - startPt.x, endPt.y - startPt.y);
        double rMag = Math.sqrt(r.x * r.x + r.y * r.y);
        if (rMag == 0.0) {
            return 0.0;
        } else {
            double rCos = r.x / rMag;
            double rAng = Math.acos(rCos);
            if (r.y < 0.0) rAng = -rAng;
            return rAng * 360.0 / (2 * Math.PI);
        }
    }

    public static double getBearingRadians(Coordinate startPt, Coordinate endPt) {
        Coordinate r = new Coordinate(endPt.x - startPt.x, endPt.y - startPt.y);
        double rMag = Math.sqrt(r.x * r.x + r.y * r.y);
        if (rMag == 0.0) {
            return 0.0;
        } else {
            double rCos = r.x / rMag;
            double rAng = Math.acos(rCos);
            if (r.y < 0.0) rAng = -rAng;
            return rAng;
        }
    }

    public static double getBearing360(Coordinate startPt, Coordinate endPt) {
        double bearing = getBearing180(startPt, endPt);
        if (bearing < 0) {
            bearing = 360 + bearing;
        }
        return bearing;
    }

    public static double theta(Coordinate p1, Coordinate p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double t = ax + ay;
        if (t != 0.0) t = dy / t;
        if (dx < 0.0) t = 2.0 - t; else {
            if (dy < 0.0) t = 4.0 + t;
        }
        return (t * 90.0);
    }

    public static CoordinateList ConvexHullWrap(CoordinateList coords) {
        CoordinateList newcoords = new CoordinateList();
        int n = coords.size();
        int i, m;
        double t, minAngle, dist, distMax, v, vdist;
        Coordinate[] p = new Coordinate[n + 1];
        for (i = 0; i < n; i++) {
            p[i] = coords.getCoordinate(i);
        }
        int min = 0;
        for (i = 1; i < n; i++) {
            if (p[i].y < p[min].y) min = i;
        }
        p[n] = coords.getCoordinate(min);
        minAngle = 0.0;
        distMax = 0.0;
        for (m = 0; m < n; m++) {
            Coordinate temp = p[m];
            p[m] = p[min];
            p[min] = temp;
            min = n;
            v = minAngle;
            vdist = distMax;
            minAngle = 360.0;
            for (i = m + 1; i <= n; i++) {
                t = theta(p[m], p[i]);
                dist = p[m].distance(p[i]);
                if ((t > v) || ((t == v) && (dist > vdist))) {
                    if ((t < minAngle) || ((t == minAngle) && (dist > distMax))) {
                        min = i;
                        minAngle = t;
                        distMax = dist;
                    }
                }
            }
            if (min == n) {
                for (int j = 0; j <= m; j++) newcoords.add(p[j], true);
                if (!(p[0].equals2D(p[m]))) {
                    newcoords.add(p[0], true);
                }
                LinearRing lr = new GeometryFactory().createLinearRing(newcoords.toCoordinateArray());
                if (!clockwise(lr)) {
                    CoordinateList newcoordsCW = new CoordinateList();
                    for (int j = newcoords.size() - 1; j >= 0; j--) newcoordsCW.add(newcoords.getCoordinate(j));
                    return newcoordsCW;
                } else {
                    return newcoords;
                }
            }
        }
        return newcoords;
    }

    /**
     * @return - the distance from r to the line segment p0-p1
     */
    public static double getDistance(Coordinate r, Coordinate p0, Coordinate p1) {
        return Math.sqrt(getDistanceSqd(r, p0, p1, new Coordinate(0, 0)));
    }

    /**
     * @return - the coordinate on the line segment p0-p1 which is closest to r
     */
    public static Coordinate getClosestPointOnSegment(Coordinate r, Coordinate p0, Coordinate p1) {
        Coordinate coordOut = new Coordinate(0, 0);
        getDistanceSqd(r, p0, p1, coordOut);
        return coordOut;
    }

    /**
     * Find the perpendicular distance between Point R and the Line from P0 to P1.
     * Based on the parametric equation of a line: P(t) = P0 + tV.
     * First find the value of t such that R - P(t) is perpendicular to V where V
     * is the vector P1 - P0.  Given that the dot product of two vectors is zero 
     * when they are perpendicular:	  ( * is read as dot )
     *		(R-P(t)) * V = 0			  substituting P0 + tV for P(t) gives	
     *		(R - P0 - tV) * V = 0		  collecting term gives:
     *		(R-P0) * V = tV * V			  solving for t gives:
     *		t = ((R-P0) * V) / (V * V)	  If t is in the interval 0 to 1 then	
     * the intersection point is between P0 and P1, otherwise use the distance
     * formula.	Plugging in the value of t to the original equation gives the
     * vector from the line to R and we need only take the magnitude of it to
     * find the distance.
     * @param r - an arbitrary Coordinate
     * @param p0 - start point of line segment
     * @param p1 - end point of line segment
     * @param coordOut - pass pre-allocated. Returns point on p0-p1 closest to r 
     * (constrained to segment).
     * @return the distance squared from r to segment p0-p1.
    */
    public static double getDistanceSqd(final Coordinate r, final Coordinate p0, final Coordinate p1, final Coordinate coordOut) {
        double Xv = p1.x - p0.x;
        double Yv = p1.y - p0.y;
        double VdotV = Xv * Xv + Yv * Yv;
        double Xp0r = r.x - p0.x;
        double Yp0r = r.y - p0.y;
        if (VdotV == 0.0) {
            coordOut.x = p0.x;
            coordOut.y = p0.y;
            return Xp0r * Xp0r + Yp0r * Yp0r;
        }
        double t = (Xp0r * Xv + Yp0r * Yv) / VdotV;
        if (t <= 0.0) {
            coordOut.x = p0.x;
            coordOut.y = p0.y;
            return Xp0r * Xp0r + Yp0r * Yp0r;
        } else if (t >= 1.0) {
            coordOut.x = p1.x;
            coordOut.y = p1.y;
            double Xp1r = r.x - p1.x;
            double Yp1r = r.y - p1.y;
            return Xp1r * Xp1r + Yp1r * Yp1r;
        } else {
            double Xp = (p0.x + t * Xv) - r.x;
            double Yp = (p0.y + t * Yv) - r.y;
            coordOut.x = r.x + Xp;
            coordOut.y = r.y + Yp;
            return Xp * Xp + Yp * Yp;
        }
    }

    public static double distanceSqd(Coordinate r, Coordinate p) {
        double dx = r.x - p.x;
        double dy = r.y - p.y;
        return dx * dx + dy * dy;
    }

    /**
     * @return the nearest point from pt to the infinite line defined by p0-p1
     */
    public static Coordinate getClosestPointOnLine(Coordinate pt, Coordinate p0, Coordinate p1) {
        MathVector vpt = new MathVector(pt);
        MathVector vp0 = new MathVector(p0);
        MathVector vp1 = new MathVector(p1);
        MathVector v = vp0.vectorBetween(vp1);
        double vdotv = v.dot(v);
        if (vdotv == 0.0) {
            return p0;
        } else {
            double t = vp0.vectorBetween(vpt).dot(v) / vdotv;
            MathVector vt = v.scale(t);
            vpt = vp0.add(vt);
            return vpt.getCoord();
        }
    }

    /**
     * @return the point at distance d along vector from q to r
     */
    public static Coordinate along(double d, Coordinate q, Coordinate r) {
        double ux, uy, m;
        Coordinate n = (Coordinate) r.clone();
        ux = r.x - q.x;
        uy = r.y - q.y;
        m = Math.sqrt(ux * ux + uy * uy);
        if (m != 0) {
            ux = d * ux / m;
            uy = d * uy / m;
            n.x = q.x + ux;
            n.y = q.y + uy;
        }
        return n;
    }

    /**
     * @param distance = offset along LineString length
     * @param lineString
     * @return the Coordinate distance along LineString
     */
    public static Coordinate alongLineString(double distance, LineString lineString) {
        Coordinate[] coords = lineString.getCoordinates();
        int n = coords.length;
        double d = 0;
        Coordinate p1 = new Coordinate();
        Coordinate p2 = new Coordinate();
        for (int i = 1; i < n; i++) {
            p1 = coords[i - 1];
            p2 = coords[i];
            d += p1.distance(p2);
            if (d > distance) break;
        }
        if (d > distance) return along(d - distance, p2, p1); else return along(p1.distance(p2) + (distance - d), p1, p2);
    }

    /**
     * @param distance = offset along LineString length
     * @param lineString
     * @return index of line segment with coordinate >= distance d along LineString
     * or index of last segment if distance is greater than LineString length
     */
    public static int segmentAlong(double distance, LineString lineString) {
        Coordinate[] coords = lineString.getCoordinates();
        int n = coords.length;
        double d = 0;
        for (int i = 1; i < n; i++) {
            d += coords[i - 1].distance(coords[i]);
            if (d > distance) return i - 1;
        }
        return n - 2;
    }

    /**
     * @param index - index of Coordinate at which to stop computing length
     * @return the length of the LineString up to index
     */
    public static double lengthAtIndex(int index, LineString lineString) {
        Coordinate[] coords = lineString.getCoordinates();
        int n = coords.length;
        double d = 0;
        for (int i = 1; i < n; i++) {
            if ((i - 1) >= index) return d;
            d += coords[i - 1].distance(coords[i]);
        }
        return lineString.getLength();
    }

    /**
     * @return the angle between vectors p2-p1 and p2-p3  from 0 to 180. 
     * NOTE: this routine returns POSITIVE angles only
     */
    public static double interiorAngle(Coordinate p1, Coordinate p2, Coordinate p3) {
        Coordinate p = vectorBetween(p1, p2);
        Coordinate q = vectorBetween(p3, p2);
        double arg = dot(p, q) / (mag(p) * mag(q));
        if (arg < -1.0) arg = -1.0; else if (arg > 1.0) arg = 1.0;
        return Math.toDegrees(Math.acos(arg));
    }

    /**
     * @param ring - LinearRing represented as LineString to analyze
     * @return - Coordinate[] with first point of passed LineString in [0] 
     * followed by [1 to length] with x as distance and y as angle.
     * The angle will be the absolute bearing in the range 0-360.
     * The original LineString and Coordinate points are unmodified.
     */
    public static Coordinate[] getDistanceBearingArray(LineString ring) {
        Coordinate[] coords = new Coordinate[ring.getNumPoints()];
        Coordinate p1 = new Coordinate(ring.getCoordinateN(0));
        coords[0] = p1;
        for (int i = 1; i < coords.length; i++) {
            coords[i] = new Coordinate(ring.getCoordinateN(i));
            Coordinate p2 = coords[i];
            double angle = getBearing360(p1, p2);
            double distance = p1.distance(p2);
            p1.x = p2.x;
            p1.y = p2.y;
            coords[i].x = distance;
            coords[i].y = angle;
        }
        return coords;
    }

    /**
     * @param ring - LinearRing represented as LineString to analyze
     * @return - Coordinate[] array of with x as distance and y as 
     * interior angles in degrees 0 to +180.
     * The angles at each index in the array are the interior angles at the 
     * vertex position in the (closed polygon) ring.  Every array position is filled.
     * The distances are the distance at a vertex to the following point.  For [n-2] 
     * the distance is computed to the [n-1] position assuming the ring is closed.
     */
    public static Coordinate[] getDistanceAngleArray(final LineString ring) {
        int n = ring.getNumPoints();
        Coordinate[] coords = new Coordinate[n];
        for (int i = 0; i < coords.length; i++) {
            Coordinate pb = ring.getCoordinateN((i == 0) ? n - 2 : i - 1);
            Coordinate p = ring.getCoordinateN(i);
            Coordinate pn = ring.getCoordinateN((i == n - 1) ? 1 : i + 1);
            double angle = interiorAngle(pb, p, pn);
            double distance = p.distance(pn);
            coords[i] = new Coordinate(distance, angle, Double.NaN);
        }
        return coords;
    }

    /**
     * @param ring - a LineString representing a linear ring
     * @return - an array of Coordinate points with colinear points removed.
     * The original LineString and Coordinate points are unmodified.
     */
    public static LinearRing removeRedundantPoints(final LineString ring) {
        Coordinate[] coords = new Coordinate[ring.getNumPoints()];
        int n = coords.length;
        boolean[] remove = new boolean[n];
        for (int i = 0; i < n; i++) {
            coords[i] = new Coordinate(ring.getCoordinateN(i));
            remove[i] = false;
        }
        Coordinate p2 = null;
        Coordinate p3 = null;
        for (int i = 0; i < coords.length; i++) {
            Coordinate p1 = coords[i];
            if (i > 1) {
                double angle = interiorAngle(p1, p2, p3);
                boolean colinear = angle > 179;
                if (colinear) {
                    remove[i - 1] = colinear;
                    n--;
                }
            }
            p3 = p2;
            p2 = p1;
        }
        Coordinate[] newCoords = new Coordinate[n];
        int j = 0;
        for (int i = 0; i < coords.length; i++) {
            if (!remove[i]) newCoords[j++] = new Coordinate(coords[i]);
        }
        LinearRing linearRing = new LinearRing(newCoords, ring.getPrecisionModel(), ring.getSRID());
        return linearRing;
    }

    public static Geometry reducePoints(final Geometry geo, double tolerance) {
        CoordinateList coords = new CoordinateList();
        UniqueCoordinateArrayFilter filter = new UniqueCoordinateArrayFilter();
        geo.apply(filter);
        coords.add(filter.getCoordinates(), false);
        if ((geo instanceof Polygon) || (geo instanceof LinearRing)) {
            coords.add(coords.getCoordinate(0));
        }
        int maxIndex = coords.size() - 1;
        int temp = maxIndex;
        do {
            temp = maxIndex;
            int i = 0;
            do {
                Coordinate anchor = coords.getCoordinate(i);
                boolean pointDeleted = false;
                int k = maxIndex;
                do {
                    Coordinate floater = coords.getCoordinate(k);
                    double dmax = -1.0;
                    int j = k;
                    while (j > (i + 1)) {
                        j--;
                        Coordinate pt = coords.getCoordinate(j);
                        Coordinate cp = getClosestPointOnLine(pt, anchor, floater);
                        double d = pt.distance(cp);
                        if (d > dmax) {
                            dmax = d;
                            k = j;
                        }
                    }
                    if ((dmax < tolerance) && (dmax > -1.0) && (maxIndex > 1)) {
                        pointDeleted = true;
                        coords.remove(k);
                        maxIndex--;
                        k = maxIndex;
                    }
                } while (!(pointDeleted || (k <= (i + 1))));
                i++;
            } while (i <= (maxIndex - 2));
        } while (temp != maxIndex);
        if (geo instanceof LineString) {
            return new GeometryFactory().createLineString(coords.toCoordinateArray());
        } else if (geo instanceof LinearRing) {
            return new GeometryFactory().createLinearRing(coords.toCoordinateArray());
        } else if (geo instanceof Polygon) {
            return new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(coords.toCoordinateArray()), null);
        } else {
            return geo;
        }
    }

    public static boolean clockwise(final Geometry geo) {
        if ((geo instanceof Polygon) || (geo instanceof LinearRing)) {
            double t1, t2;
            double geoArea;
            Coordinate[] geoCoords = geo.getCoordinates();
            int maxIndex = geoCoords.length - 1;
            t1 = geoCoords[maxIndex].x * geoCoords[0].y;
            t2 = -geoCoords[0].x * geoCoords[maxIndex].y;
            for (int i = 0; i < maxIndex; i++) {
                t1 += (geoCoords[i].x * geoCoords[i + 1].y);
                t2 -= (geoCoords[i + 1].x * geoCoords[i].y);
            }
            geoArea = 0.5 * (t1 + t2);
            return (geoArea < 0);
        } else {
            return true;
        }
    }

    /**
     * Compute a partial area of a polygon represented by a 
     * @param start - start index of partial polygon.
     * @param num - number of vertices of partial polygon.
     * @param coords Coordinate[] representing polygon.
     * @return signed area 
     */
    public static double area(int start, int num, final Coordinate[] coords) {
        int n = coords.length - 1;
        int maxIndex = moduloAccess(start, num, n);
        double t1 = coords[maxIndex].x * coords[0].y;
        double t2 = -coords[0].x * coords[maxIndex].y;
        for (int i = 0; i < num; i++) {
            int m = moduloAccess(start, i, n);
            int m1 = (m == n) ? 0 : m + 1;
            t1 += (coords[m].x * coords[m1].y);
            t2 -= (coords[m1].x * coords[m].y);
        }
        return 0.5 * (t1 + t2);
    }

    /**
     * Compute Modulo index to a circular array starting at i adding offset to wrap around at n.
     * @param i - can be any value, even negative
     * @param offset - can be any value, even negative
     * @param n - the last allowable index
     * @return int position of (i + offset) modulo n
     */
    public static int moduloAccess(int i, int offset, int n) {
        int modulo = (i + offset) % (n + 1);
        if (modulo < 0) modulo = -modulo;
        return modulo;
    }

    public static Coordinate intersect(Coordinate P1, Coordinate P2, Coordinate P3, Coordinate P4) {
        Coordinate V = new Coordinate((P2.x - P1.x), (P2.y - P1.y));
        Coordinate W = new Coordinate((P4.x - P3.x), (P4.y - P3.y));
        double n = W.y * (P3.x - P1.x) - W.x * (P3.y - P1.y);
        double d = W.y * V.x - W.x * V.y;
        if (d != 0.0) {
            double t1 = n / d;
            Coordinate E = new Coordinate((P1.x + V.x * t1), (P1.y + V.y * t1));
            return E;
        } else {
            return null;
        }
    }

    /**
     * Warning: this method adds an Epsilon tolerance of .001 to the end of both segments.
     * This eliminates any doubt that abutting segments might not intersect (i.e. T intersections)
     * @param p0, p1 segment 1
     * @param p2, p3 segment 2
     * @return Coordinate  intersection point that lies on both line segments or null
     */
    public static Coordinate intersectSegments(Coordinate p0, Coordinate p1, Coordinate p2, Coordinate p3) {
        double Vx = (p1.x - p0.x);
        double Vy = (p1.y - p0.y);
        double Wx = (p3.x - p2.x);
        double Wy = (p3.y - p2.y);
        double d = Wy * Vx - Wx * Vy;
        if (d != 0.0) {
            double n1 = Wy * (p2.x - p0.x) - Wx * (p2.y - p0.y);
            double n2 = Vy * (p2.x - p0.x) - Vx * (p2.y - p0.y);
            double t1 = n1 / d;
            double t2 = n2 / d;
            double epsilon = 0.001;
            double lowbound = 0.0 - epsilon;
            double hibound = 1.0 + epsilon;
            boolean onp0p1 = (t1 >= lowbound) && (t1 <= hibound);
            boolean onp2p3 = (t2 >= lowbound) && (t2 <= hibound);
            if (onp0p1 && onp2p3) {
                return new Coordinate((p0.x + Vx * t1), (p0.y + Vy * t1));
            } else return null;
        } else {
            return null;
        }
    }

    public static Coordinate getCenter(Coordinate p1, Coordinate p2, Coordinate p3) {
        double x = p1.x + ((p2.x - p1.x) / 2.0);
        double y = p1.y + ((p2.y - p1.y) / 2.0);
        Coordinate p12 = new Coordinate(x, y);
        if (pointToRight(p3, p1, p2)) p1 = rotPt(p1, p12, -90.0); else p1 = rotPt(p1, p12, 90.0);
        x = p2.x + ((p3.x - p2.x) / 2.0);
        y = p2.y + ((p3.y - p2.y) / 2.0);
        Coordinate p23 = new Coordinate(x, y);
        if (pointToRight(p1, p3, p2)) p3 = rotPt(p3, p23, -90.0); else p3 = rotPt(p3, p23, 90.0);
        Coordinate center = intersect(p1, p12, p3, p23);
        if (center == null) return p2; else return center;
    }

    public static BitSet setBit(BitSet bitSet, Geometry geometry) {
        BitSet newBitSet = (BitSet) bitSet.clone();
        if (geometry.isEmpty()) newBitSet.set(emptyBit); else if (geometry instanceof Point) newBitSet.set(pointBit); else if (geometry instanceof MultiPoint) newBitSet.set(pointBit); else if (geometry instanceof LineString) newBitSet.set(lineBit); else if (geometry instanceof LinearRing) newBitSet.set(lineBit); else if (geometry instanceof MultiLineString) newBitSet.set(lineBit); else if (geometry instanceof Polygon) newBitSet.set(polyBit); else if (geometry instanceof MultiPolygon) newBitSet.set(polyBit); else if (geometry instanceof GeometryCollection) {
            GeometryCollection geometryCollection = (GeometryCollection) geometry;
            for (int i = 0; i < geometryCollection.getNumGeometries(); i++) newBitSet = setBit(newBitSet, geometryCollection.getGeometryN(i));
        }
        return newBitSet;
    }

    public static LineString MakeRoundCorner(Coordinate A, Coordinate B, Coordinate C, Coordinate D, double r, boolean arcOnly) {
        MathVector Gv = new MathVector();
        MathVector Hv;
        MathVector Fv;
        Coordinate E = intersect(A, B, C, D);
        if (E != null) {
            MathVector Ev = new MathVector(E);
            if (E.distance(B) > E.distance(A)) {
                Coordinate temp = A;
                A = B;
                B = temp;
            }
            if (E.distance(D) > E.distance(C)) {
                Coordinate temp = C;
                C = D;
                D = temp;
            }
            MathVector Av = new MathVector(A);
            MathVector Cv = new MathVector(C);
            double alpha = Ev.vectorBetween(Av).angleRad(Ev.vectorBetween(Cv)) / 2.0;
            double h1 = Math.abs(r / Math.sin(alpha));
            if ((h1 * h1 - r * r) >= 0) {
                double d1 = Math.sqrt(h1 * h1 - r * r);
                double theta = Math.PI / 2.0 - alpha;
                theta = theta * 2.0;
                Gv = Ev.add(Ev.vectorBetween(Av).unit().scale(d1));
                Hv = Ev.add(Ev.vectorBetween(Cv).unit().scale(d1));
                Fv = Ev.add(Ev.vectorBetween(Gv).rotateRad(alpha).unit().scale(h1));
                if (Math.abs(Fv.distance(Hv) - Fv.distance(Gv)) > 1.0) {
                    Fv = Ev.add(Ev.vectorBetween(Gv).rotateRad(-alpha).unit().scale(h1));
                    theta = -theta;
                }
                CoordinateList coordinates = new CoordinateList();
                if (!arcOnly) coordinates.add(C);
                Arc arc = new Arc(Fv.getCoord(), Hv.getCoord(), Math.toDegrees(theta));
                LineString lineString = arc.getLineString();
                coordinates.add(lineString.getCoordinates(), false);
                if (!arcOnly) coordinates.add(A);
                return new GeometryFactory().createLineString(coordinates.toCoordinateArray());
            }
        }
        return null;
    }

    public static boolean geometriesEqual(Geometry geo1, Geometry geo2) {
        if ((!(geo1 instanceof GeometryCollection)) && (!(geo2 instanceof GeometryCollection))) return geo1.equals(geo2);
        if ((!(geo1 instanceof GeometryCollection)) && ((geo2 instanceof GeometryCollection))) return false;
        if (((geo1 instanceof GeometryCollection)) && (!(geo2 instanceof GeometryCollection))) return false;
        int numGeos1 = ((GeometryCollection) geo1).getNumGeometries();
        int numGeos2 = ((GeometryCollection) geo2).getNumGeometries();
        if (numGeos1 != numGeos2) return false;
        for (int index = 0; index < numGeos1; index++) {
            Geometry internalGeo1 = ((GeometryCollection) geo1).getGeometryN(index);
            Geometry internalGeo2 = ((GeometryCollection) geo2).getGeometryN(index);
            if (!geometriesEqual(internalGeo1, internalGeo2)) return false;
        }
        return true;
    }

    ;

    public static double getDistanceFromPointToGeometry(Coordinate coord, Geometry geo) {
        double closestDist = Double.MAX_VALUE;
        for (int i = 0; i < geo.getNumGeometries(); i++) {
            double newDist;
            Geometry internalGeo = geo.getGeometryN(i);
            if (internalGeo instanceof Point) {
                newDist = coord.distance(internalGeo.getCoordinate());
                if (newDist < closestDist) closestDist = newDist;
            } else if (internalGeo instanceof LineString) {
                Coordinate[] coords = internalGeo.getCoordinates();
                for (int j = 0; j < coords.length - 1; j++) {
                    newDist = GeoUtils.getDistance(coord, coords[j], coords[j + 1]);
                    if (newDist < closestDist) closestDist = newDist;
                }
            } else if (internalGeo instanceof Polygon) {
                Geometry newGeo = internalGeo.getBoundary();
                newDist = getDistanceFromPointToGeometry(coord, newGeo);
                if (newDist < closestDist) closestDist = newDist;
            } else if (internalGeo instanceof MultiPoint) {
                Coordinate[] coords = internalGeo.getCoordinates();
                for (int k = 0; k < coords.length; k++) {
                    newDist = coord.distance(coords[k]);
                    if (newDist < closestDist) closestDist = newDist;
                }
            } else {
                for (int m = 0; m < internalGeo.getNumGeometries(); m++) {
                    newDist = getDistanceFromPointToGeometry(coord, internalGeo.getGeometryN(m));
                    if (newDist < closestDist) closestDist = newDist;
                }
            }
        }
        return closestDist;
    }

    public static boolean geometryIsSegmentOf(Geometry geo1, Geometry geo2) {
        if (geo1.getNumPoints() > geo2.getNumPoints()) return false;
        int numGeos1 = geo1.getNumGeometries();
        int numGeos2 = geo2.getNumGeometries();
        if ((numGeos1 == 1) && (numGeos2 == 1)) {
            Coordinate[] coords1 = geo1.getCoordinates();
            Coordinate[] coords2 = geo2.getCoordinates();
            int i1 = 0;
            int i2 = 0;
            while (i2 < coords2.length) {
                if (coords1[0].equals2D(coords2[i2])) break;
                i2++;
            }
            if (i2 == coords2.length) return false;
            while ((i1 < coords1.length) && (i2 < coords2.length)) {
                if (!coords1[i1].equals2D(coords2[i2])) return false;
                i1++;
                i2++;
            }
            return (i1 == coords1.length);
        } else {
            boolean foundMatch = false;
            for (int i = 0; i < numGeos1; i++) {
                foundMatch = false;
                for (int j = 0; j < numGeos2; j++) {
                    if (geometryIsSegmentOf(geo1.getGeometryN(i), geo2.getGeometryN(j))) {
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) return false;
            }
            return foundMatch;
        }
    }

    ;

    /**
     * @param startPt, endPt - coordinates of line to construct buffer arc from
     * @param bufferStartAngle - angle in degrees from perpendicular CCW around startPt
     * @param bufferEndAngle - angle in degrees from perpendicular CW around endPt
     * @param bufferDistance - radius of arc buffer
     * @param arcTolerance
     * @return Polygon representing the arc buffer
     */
    public static Geometry bufferArc(Coordinate startPt, Coordinate endPt, double bufferStartAngle, double bufferEndAngle, double bufferDistance, double arcTolerance) {
        Coordinate perp1 = perpendicularVector(startPt, endPt, bufferDistance, true);
        Coordinate startBuf = rotPt(perp1, startPt, -bufferStartAngle);
        Coordinate perp2 = perpendicularVector(endPt, startPt, bufferDistance, false);
        Arc arc1 = new Arc(startPt, startBuf, bufferStartAngle);
        Arc arc2 = new Arc(endPt, perp2, bufferEndAngle);
        arc1.setArcTolerance(arcTolerance);
        arc2.setArcTolerance(arcTolerance);
        CoordinateList polyCoords = new CoordinateList();
        polyCoords.add(startPt, true);
        polyCoords.add(arc1.getCoordinates().toCoordinateArray(), true);
        polyCoords.add(arc2.getCoordinates().toCoordinateArray(), true);
        polyCoords.add(endPt, true);
        polyCoords.add(startPt, true);
        return new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(polyCoords.toCoordinateArray()), null);
    }

    /**
     * Construct a perfectly orthogonal rectangle using the input's first two points,
     * and the distance between the second and third points as the length.
     * @param rectangle - rectangular polygon.
     * @param sideOne - the index of the side (numbered from 1 to 4) that is the front.
     * @return - a rectangle represented by a Coordinate[5];
     */
    public static Coordinate[] rectangleFromGeometry(Geometry rectangle, int sideOne) {
        if ((rectangle.getNumGeometries() > 1) || (rectangle instanceof MultiPolygon)) rectangle = rectangle.getGeometryN(0);
        Coordinate[] p;
        if (rectangle instanceof Polygon) {
            p = ((Polygon) rectangle).getExteriorRing().getCoordinates();
        } else p = rectangle.getCoordinates();
        if (!(p.length == 5)) return null;
        if (sideOne != 1) {
            sideOne = Math.max(1, Math.min(4, sideOne)) - 1;
            for (int j = 0; j < sideOne; j++) {
                int n = p.length - 2;
                Coordinate t = p[0];
                for (int i = 0; i < n; i++) {
                    p[i] = p[i + 1];
                }
                p[n] = t;
            }
            p[p.length - 1] = p[0];
        }
        Coordinate p2 = perpendicularVector(p[1], p[0], p[1].distance(p[2]), true);
        Coordinate p3 = perpendicularVector(p2, p[1], p[0].distance(p[1]), true);
        Coordinate[] rectangleCoords = { p[0], p[1], p2, p3, p[0] };
        return rectangleCoords;
    }

    /**
     * Create buffer arc polygons from the four sides of a rectangular polygon.  Note that 
     * the method does not depend on the polygon being perfectly rectangular, but will 
     * produce buffer arcs around an orthogalized version of the polygon using side one
     * and the length of side two.
     * @param rectangle - rectangular polygon.
     * @param frontAngle - angle in degrees of arc segments from front (side 1)
     * @param rearAngle - angle in degrees of arc segments from rear (side 3)
     * @param distances - array of 4 arc radii for front, right, rear, and left distances 
     * @param sideOne - the index of the side (numbered from 1 to 4) that is the front.
     * @return - a MultiPolygon with the non-zero buffer arc Polygons.  If only one distance is
     * non-zero, a Polygon will be returned.
     */
    public static Geometry rectangleBufferArcs(Geometry rectangle, double frontAngle, double rearAngle, double[] distances, int sideOne, double arcTolerance) {
        Coordinate[] p = rectangleFromGeometry(rectangle, sideOne);
        if (p == null) return null;
        int count = 0;
        for (int i = 0; i < distances.length; i++) {
            if (distances[i] != 0.0) count++;
        }
        Polygon[] arcs = new Polygon[count];
        count = 0;
        if (distances[0] != 0.0) arcs[count++] = (Polygon) bufferArc(p[0], p[1], frontAngle, frontAngle, distances[0], arcTolerance);
        if (distances[1] != 0.0) arcs[count++] = (Polygon) bufferArc(p[1], p[2], 90 - frontAngle, 90 - rearAngle, distances[1], arcTolerance);
        if (distances[2] != 0.0) arcs[count++] = (Polygon) bufferArc(p[2], p[3], rearAngle, rearAngle, distances[2], arcTolerance);
        if (distances[3] != 0.0) arcs[count++] = (Polygon) bufferArc(p[3], p[0], 90 - rearAngle, 90 - frontAngle, distances[3], arcTolerance);
        if (count == 1) return arcs[0]; else return new GeometryFactory().createMultiPolygon(arcs);
    }

    /**
     * SkyJUMP has modified core Task class to add getUnitsName() method. 
     * The following code allows this plugin to be used with other core branches
     * without having to implement SkyJUMP core changes.
     * @return "Meters", "Feet", or "Undefined"
     */
    public static String getTaskUnits(WorkbenchContext workbenchContext) {
        try {
            Task task = workbenchContext.getTask();
            Class<? extends Task> c = task.getClass();
            Method m = c.getMethod("getUnitsName", (Class[]) null);
            return (String) m.invoke(task, new Object[0]);
        } catch (Exception ex) {
        }
        return "Undefined";
    }

    public static void setTaskUnits(PlugInContext context, String units) {
        try {
            Task task = context.getWorkbenchContext().getTask();
            Class<?> c = context.getWorkbenchContext().getTask().getClass();
            Method m = c.getMethod("setUnitsName", new Class[] { String.class });
            Object[] parameters = new Object[] { units };
            m.invoke(task, parameters);
        } catch (Exception ex) {
        }
    }

    public static final String ASHS_ID = "ASHS_ID";

    public static Geometry clipToPolygon(Polygon poly, Geometry b) {
        Geometry intersection = null;
        try {
            intersection = poly.intersection(b);
        } catch (Exception ex) {
            System.out.println(poly);
            System.out.println(b);
        }
        return intersection;
    }

    /**
     * @param poly - the Polygon to be used for clipping.
     * @param ifc - a FeatureCollection that has been indexed. 
     * You can just pass: new IndexedFeatureCollection(fc) if you like.
     * @param copySchema - if true the output will have the same Schema as the input. 
     * If false it will have GEOMETRY and ASHS_ID only.
     * @return a FeatureCollection with the input clipped to poly.
     */
    public static FeatureCollection clipToPolygon(Polygon poly, IndexedFeatureCollection ifc, boolean copySchema) {
        FeatureSchema featureSchema = new FeatureSchema();
        if (copySchema) {
            FeatureSchema ifcSchema = ifc.getFeatureSchema();
            for (int i = 0; i < ifcSchema.getAttributeCount(); i++) {
                featureSchema.addAttribute(ifcSchema.getAttributeName(i), ifcSchema.getAttributeType(i));
            }
        } else {
            featureSchema.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
            featureSchema.addAttribute(ASHS_ID, AttributeType.STRING);
        }
        FeatureDataset overlay = new FeatureDataset(featureSchema);
        Envelope polyEnvelope = poly.getEnvelopeInternal();
        Geometry polyGeomEnvelope = poly.getEnvelope();
        for (Iterator j = ifc.query(polyEnvelope).iterator(); j.hasNext(); ) {
            Feature b = (Feature) j.next();
            if (!polyGeomEnvelope.intersects(b.getGeometry().getEnvelope())) continue;
            Geometry intersection = clipToPolygon(poly, b.getGeometry());
            if ((intersection == null) || intersection.isEmpty()) continue;
            addFeature(intersection, overlay, b, copySchema);
        }
        return overlay;
    }

    public static void addFeature(Geometry intersection, FeatureDataset overlay, Feature b, boolean copySchema) {
        if (intersection instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) intersection;
            for (int i = 0; i < gc.getNumGeometries(); i++) {
                addFeature(gc.getGeometryN(i), overlay, b, copySchema);
            }
            return;
        }
        Feature feature = new BasicFeature(overlay.getFeatureSchema());
        if (copySchema) {
            for (int i = 0; i < b.getSchema().getAttributeCount(); i++) {
                feature.setAttribute(i, b.getAttribute(i));
            }
        } else {
            feature.setAttribute(ASHS_ID, b.getAttribute(ASHS_ID));
        }
        feature.setGeometry(intersection);
        overlay.add(feature);
    }

    /**
     * @param geom Geometry to check
     * @return true if Geometry has multiple components. 
     * This includes MultiPolygon, MultiLineString, etc. and simple Polygons with holes.
     * Use with getNumComponents() and getComponentN().
     */
    public static boolean isMultiGeometry(Geometry geom) {
        return (geom instanceof Polygon) ? ((Polygon) geom).getNumInteriorRing() > 0 : geom.getNumGeometries() > 1;
    }

    /**
     * The purpose of this method is (along with getCompentN()) is to treat
     * Polygons with holes the same as a Multi-Geometry to make processsing them easier.
     * @param geom - Geometry used to determine the number of components.
     * @return - number of geometry components.
     */
    public static int getNumComponents(Geometry geom) {
        return (geom instanceof Polygon) ? ((Polygon) geom).getNumInteriorRing() + 1 : geom.getNumGeometries();
    }

    /**
     * The purpose of this method is (along with getNumComponents()) is to treat 
     * Polygons with holes the same as a Multi-Geometry to make processsing them easier.
     * @param geom - Geometry from which to extract the component.
     * @param n - index from 0 to number of components.
     * @return - the Nth component of passed Geometry.
     */
    public static Geometry getComponentN(Geometry geom, int n) {
        if (geom instanceof Polygon) {
            if (n == 0) {
                return ((Polygon) geom).getExteriorRing();
            } else {
                return ((Polygon) geom).getInteriorRingN(n - 1);
            }
        } else {
            return geom.getGeometryN(n);
        }
    }

    /**
     * @param geom1,geom2 - Geometry (supports all Geometry types including GeometryCollection, 
     * MultiPolygon, MultiPoint, and MultiLineString).
     * @param limitDistance - Distance at which search terminates. If you have no limit then call 
     * with limitDistance = Double.MAX_VALUE.  Method will check Envelope.distance(Envelope) to
     * determine if it is greater than limitDistance, and if so will return immediately.
     * @return - shortest distance between the two Geometry objects.  Will return distance  
     * between nearest edge of closed polys or GeometryCollections unlike JTS which returns zero 
     * for any point inside a poly.  Also, it is more efficient than JTS.
     */
    public static double distance(Geometry geom1, Geometry geom2, double limitDistance, final Coordinate coord1, final Coordinate coord2) {
        if (geom1.getEnvelopeInternal().distance(geom2.getEnvelopeInternal()) >= limitDistance) {
            return limitDistance;
        }
        if (isMultiGeometry(geom1)) {
            Coordinate newcoord1 = new Coordinate();
            Coordinate newcoord2 = new Coordinate();
            for (int i = 0; i < getNumComponents(geom1); i++) {
                double newDist;
                Geometry internalGeo = getComponentN(geom1, i);
                newDist = distance(internalGeo, geom2, limitDistance, newcoord1, newcoord2);
                if (newDist < limitDistance) {
                    coord1.setCoordinate(newcoord1);
                    coord2.setCoordinate(newcoord2);
                    limitDistance = newDist;
                }
            }
            return limitDistance;
        } else if (!isMultiGeometry(geom2)) {
            return getShortestDistance(geom1, geom2, coord1, coord2);
        }
        for (int i = 0; i < getNumComponents(geom2); i++) {
            Geometry internalGeo = getComponentN(geom2, i);
            Coordinate newcoord1 = new Coordinate();
            Coordinate newcoord2 = new Coordinate();
            double newDist;
            if ((internalGeo instanceof Point) || (internalGeo instanceof LineString)) {
                newDist = getShortestDistance(internalGeo, geom1, newcoord2, newcoord1);
                if (newDist < limitDistance) {
                    coord1.setCoordinate(newcoord1);
                    coord2.setCoordinate(newcoord2);
                    limitDistance = newDist;
                }
            } else if (internalGeo instanceof Polygon) {
                if (isMultiGeometry(internalGeo)) {
                    newDist = distance(internalGeo, geom1, limitDistance, newcoord2, newcoord1);
                } else {
                    newDist = getShortestDistance(internalGeo, geom1, newcoord2, newcoord1);
                }
                if (newDist < limitDistance) {
                    coord1.setCoordinate(newcoord1);
                    coord2.setCoordinate(newcoord2);
                    limitDistance = newDist;
                }
            } else {
                newDist = distance(geom1, internalGeo, limitDistance, newcoord1, newcoord2);
                if (newDist < limitDistance) {
                    coord1.setCoordinate(newcoord1);
                    coord2.setCoordinate(newcoord2);
                    limitDistance = newDist;
                }
            }
        }
        return limitDistance;
    }

    /**
     * @param geom1,geom2 - Geometry objects to compute distance between.
     * @param coord1,coord2 - preallocated Coordinate points that will return 
     * with the values of x and y that yielded the measured distance.
     * @return - shortest distance between the two inputs, or Double.NaN if either is null, 
     * or Double.NaN if either geometry has multiple geometries.
     */
    public static double getShortestDistance(Geometry geom1, Geometry geom2, final Coordinate coord1, final Coordinate coord2) {
        if (geom1 == null || geom2 == null) {
            return Double.NaN;
        }
        if (isMultiGeometry(geom1) || isMultiGeometry(geom2)) return Double.NaN;
        if (geom1.getEnvelopeInternal().intersects(geom2.getEnvelopeInternal())) {
            Coordinate[] coordsOne = geom1.getCoordinates();
            Coordinate[] coordsTwo = geom2.getCoordinates();
            for (int j = 0; j < coordsOne.length - 1; j++) {
                Coordinate p0 = coordsOne[j];
                Coordinate p1 = coordsOne[j + 1];
                for (int k = 0; k < coordsTwo.length - 1; k++) {
                    Coordinate p2 = coordsTwo[k];
                    Coordinate p3 = coordsTwo[k + 1];
                    Coordinate coord = GeoUtils.intersectSegments(p0, p1, p2, p3);
                    if (coord != null) {
                        coord1.x = coord.x;
                        coord1.y = coord.y;
                        coord2.x = coord.x;
                        coord2.y = coord.y;
                        return 0;
                    }
                }
            }
        }
        return distanceBetweenTwo(geom1, geom2, coord1, coord2);
    }

    /**
     * @param geom1,geom2 - Geometry objects to compute the distance between.
     * Objects with a dimension greater than one should be broken down before
     * calling this method (i.e. MultiPolygon, MultiLineString, GeometryCollection).
     * @param coord1, coord2 -  preallocated Coordinate points that will return 
     * with the values of x and y that yielded the measured distance.
     * @return - the distance between the two Geometry objects.
     */
    public static double distanceBetweenTwo(Geometry geom1, Geometry geom2, final Coordinate coord1, final Coordinate coord2) {
        Coordinate[] coordsOne = geom1.getCoordinates();
        Coordinate[] coordsTwo = geom2.getCoordinates();
        coord1.setCoordinate(coordsOne[0]);
        coord2.setCoordinate(coordsTwo[0]);
        if ((coordsOne.length == 1) && (coordsTwo.length == 1)) {
            return coord1.distance(coord2);
        }
        Coordinate coord = new Coordinate(0, 0);
        double distSqd = Double.MAX_VALUE;
        for (int j = 0; j < coordsOne.length; j++) {
            Coordinate pt0 = coordsOne[j];
            for (int k = 0; k < coordsTwo.length - 1; k++) {
                Coordinate pt1 = coordsTwo[k];
                Coordinate pt2 = coordsTwo[k + 1];
                double dist = GeoUtils.getDistanceSqd(pt0, pt1, pt2, coord);
                if (dist < distSqd) {
                    distSqd = dist;
                    coord1.x = pt0.x;
                    coord1.y = pt0.y;
                    coord2.x = coord.x;
                    coord2.y = coord.y;
                }
            }
        }
        for (int j = 0; j < coordsTwo.length; j++) {
            Coordinate pt0 = coordsTwo[j];
            for (int k = 0; k < coordsOne.length - 1; k++) {
                Coordinate pt1 = coordsOne[k];
                Coordinate pt2 = coordsOne[k + 1];
                double dist = GeoUtils.getDistanceSqd(pt0, pt1, pt2, coord);
                if (dist < distSqd) {
                    distSqd = dist;
                    coord1.x = coord.x;
                    coord1.y = coord.y;
                    coord2.x = pt0.x;
                    coord2.y = pt0.y;
                }
            }
        }
        return Math.sqrt(distSqd);
    }

    /**
     * @param features - input list of Feature items (overlaps will be combined).
     * @param featureSchema - FeatureSchema of returned Feature Collection.
     * @return <Feature> Collection of medial axis LineString Features with featureSchema.
     */
    public static Collection findMedialAxisCollection(Collection features, FeatureSchema featureSchema) {
        ArrayList<Feature> medialAxisLineStringFeatures = new ArrayList<Feature>();
        boolean allreadyAllLines = true;
        for (Iterator j = features.iterator(); j.hasNext(); ) {
            Feature f = (Feature) j.next();
            if (f.getGeometry().getNumPoints() != 2) {
                allreadyAllLines = false;
                break;
            }
        }
        if (allreadyAllLines) return features;
        Collection combinedFeatures = GeoUtils.combineOverlappingFeatures(features, featureSchema);
        for (Iterator j = combinedFeatures.iterator(); j.hasNext(); ) {
            CoordinateList coordinateList = new CoordinateList();
            Feature f = (Feature) j.next();
            Geometry geo = f.getGeometry();
            coordinateList.add(geo.getCoordinates(), true);
            CoordinateList coordList = GeoUtils.ConvexHullWrap(coordinateList);
            if (coordList.size() == coordinateList.size()) coordList = coordinateList; else {
                int sideOne = coordList.indexOf(coordinateList.getCoordinate(0));
                if (sideOne > 0) {
                    Coordinate[] p = coordList.toCoordinateArray();
                    for (int k = 0; k < sideOne; k++) {
                        int n = p.length - 2;
                        Coordinate t = p[0];
                        for (int i = 0; i < n; i++) p[i] = p[i + 1];
                        p[n] = t;
                    }
                    p[p.length - 1] = p[0];
                    coordList = new CoordinateList();
                    for (int k = 0; k < p.length; k++) coordList.add(p[k], true);
                }
            }
            LineString medialAxis = GeoUtils.findMedialAxis(coordList.toCoordinateArray());
            Feature newFeature = new BasicFeature(featureSchema);
            if (featureSchema.hasAttribute("ASHS_ID") && geo.getUserData() != null) {
                String id = (String) geo.getUserData();
                newFeature.setAttribute("ASHS_ID", id);
                geo.setUserData(null);
            }
            newFeature.setGeometry(medialAxis);
            medialAxisLineStringFeatures.add(newFeature);
        }
        return medialAxisLineStringFeatures;
    }

    /**
     * Find the medial axis of the array coordinates that form a convex hull.
     * The medial axis in this context means the longest bisecting line that falls
     * on the axis of maximal symmetry.
     * See ConvexHullWrap() in this file.
     * @param coords Coordinate[] of a convex hull
     * @return LineString representing the medial axis.
     */
    public static LineString findMedialAxis(Coordinate[] coords) {
        Coordinate p1 = null;
        int n = coords.length;
        n -= 1;
        int n2 = n / 2;
        Coordinate pn2 = coords[n2];
        Coordinate pn0 = coords[0];
        ;
        double minSymmetry = Double.MAX_VALUE;
        double symmetry = 0;
        if (n == 3) {
            for (int i = 0; i < n; i++) {
                int m = GeoUtils.moduloAccess(i, n2, n - 1);
                int m1 = GeoUtils.moduloAccess(m, 1, n - 1);
                p1 = new Coordinate((coords[m].x + coords[m1].x) / 2, (coords[m].y + coords[m1].y) / 2);
                symmetry = measureSymmetry(coords[i], p1, coords);
                if (symmetry < minSymmetry) {
                    minSymmetry = symmetry;
                    pn0 = coords[i];
                    pn2 = p1;
                }
            }
        } else if (n == 4) {
            for (int i = 0; i < n; i++) {
                int m = GeoUtils.moduloAccess(i, n2, n - 1);
                int m1 = GeoUtils.moduloAccess(m, 1, n - 1);
                int i1 = GeoUtils.moduloAccess(i, 1, n - 1);
                Coordinate p0 = new Coordinate((coords[i].x + coords[i1].x) / 2, (coords[i].y + coords[i1].y) / 2);
                p1 = new Coordinate((coords[m].x + coords[m1].x) / 2, (coords[m].y + coords[m1].y) / 2);
                symmetry = measureSymmetry(p0, p1, coords);
                symmetry -= p0.distance(p1) * .001;
                if (symmetry < minSymmetry) {
                    minSymmetry = symmetry;
                    pn0 = p0;
                    pn2 = p1;
                }
            }
        } else {
            for (int i = 0; i <= n; i++) {
                p1 = getPolarOpposite(i, true, coords);
                symmetry = measureSymmetry(coords[i], p1, coords);
                if (i == 0) symmetry -= coords[i].distance(p1) * .001;
                if (symmetry < minSymmetry) {
                    minSymmetry = symmetry;
                    pn0 = coords[i];
                    pn2 = p1;
                }
                p1 = getPolarOpposite(i, false, coords);
                symmetry = measureSymmetry(coords[i], p1, coords);
                if (i == 0) symmetry -= coords[i].distance(p1) * .001;
                if (symmetry < minSymmetry) {
                    minSymmetry = symmetry;
                    pn0 = coords[i];
                    pn2 = p1;
                }
                int m = GeoUtils.moduloAccess(i, n2, n - 1);
                p1 = coords[m];
                symmetry = measureSymmetry(coords[i], p1, coords);
                if (i == 0) symmetry -= coords[i].distance(p1) * .001;
                if (symmetry < minSymmetry) {
                    minSymmetry = symmetry;
                    pn0 = coords[i];
                    pn2 = p1;
                }
                m = GeoUtils.moduloAccess(i, n2, n - 1);
                int m1 = GeoUtils.moduloAccess(m, 1, n - 1);
                int i1 = GeoUtils.moduloAccess(i, 1, n - 1);
                Coordinate p0 = new Coordinate((coords[i].x + coords[i1].x) / 2, (coords[i].y + coords[i1].y) / 2);
                p1 = new Coordinate((coords[m].x + coords[m1].x) / 2, (coords[m].y + coords[m1].y) / 2);
                symmetry = measureSymmetry(p0, p1, coords);
                if (i == 0) symmetry -= coords[i].distance(p1) * .001;
                if (symmetry < minSymmetry) {
                    minSymmetry = symmetry;
                    pn0 = p0;
                    pn2 = p1;
                }
            }
        }
        Coordinate[] line = new Coordinate[2];
        line[0] = pn0;
        line[1] = pn2;
        return new GeometryFactory().createLineString(line);
    }

    /**
     * Evaluate the symmetry about the line from p0 to p1.
     * @return a double proportional to the amount of symmetry about the p0-p1 axis.  
     * Perfect symmetry scores 0.  Less perfect symmetry scores increase by the distance
     * difference between the left and right perpendicular distances to the axis 
     * starting from Pi and working in both directions.
     */
    private static double measureSymmetry(Coordinate p0, Coordinate p1, Coordinate[] coords) {
        if (p0.equals(p1)) return 1e9;
        double symmetryTotal = 0;
        int n = 20;
        double di = p0.distance(p1) / n;
        double dist = 1e9;
        double symmetry = 0;
        for (int k = 1; k < n; k++) {
            Coordinate pt = GeoUtils.along(di * k, p0, p1);
            Coordinate p2 = GeoUtils.perpendicularVector(pt, p0, dist, true);
            Coordinate p3 = GeoUtils.perpendicularVector(pt, p0, dist, false);
            Coordinate[] pts = intersections(p2, p3, coords);
            if (pts[0] != null && pts[1] != null) symmetry = Math.abs(pt.distance(pts[0]) - pt.distance(pts[1]));
            symmetryTotal += symmetry;
        }
        return Math.round(symmetryTotal * 1000) / 1000;
    }

    private static Coordinate[] intersections(Coordinate p0, Coordinate p1, Coordinate[] coords) {
        Coordinate[] line = new Coordinate[4];
        int lineCount = 0;
        int n = coords.length - 1;
        for (int k = 0; k < n; k++) {
            Coordinate p2 = coords[k];
            Coordinate p3 = coords[k + 1];
            Coordinate coord = intersectSegmentsExact(p0, p1, p2, p3);
            if (coord != null) {
                line[lineCount] = coord;
                lineCount++;
            }
        }
        return line;
    }

    public static Coordinate intersectSegmentsExact(Coordinate p0, Coordinate p1, Coordinate p2, Coordinate p3) {
        double Vx = (p1.x - p0.x);
        double Vy = (p1.y - p0.y);
        double Wx = (p3.x - p2.x);
        double Wy = (p3.y - p2.y);
        double d = Wy * Vx - Wx * Vy;
        if (d != 0.0) {
            double n1 = Wy * (p2.x - p0.x) - Wx * (p2.y - p0.y);
            double n2 = Vy * (p2.x - p0.x) - Vx * (p2.y - p0.y);
            double t1 = n1 / d;
            double t2 = n2 / d;
            boolean onp0p1 = (t1 >= 0.0) && (t1 <= 1.0);
            boolean onp2p3 = (t2 >= 0.0) && (t2 <= 1.0);
            if (onp0p1 && onp2p3) {
                return new Coordinate((p0.x + Vx * t1), (p0.y + Vy * t1));
            } else return null;
        } else {
            return null;
        }
    }

    /**
     * @param i - coords[i] is the point to find the polar opposite of 
     * @param coords - the Coordinate array of the convex hull to evaluate
     * @return - the Coordinate of the point that is the polar opposite of coords[i]
     * (snapped to nearest point or center)
     */
    public static Coordinate getPolarOpposite(int i, boolean center, Coordinate[] coords) {
        double perimiter = 0;
        int n = coords.length - 1;
        for (int k = 0; k < n; k++) {
            Coordinate p0 = coords[k];
            Coordinate p1 = coords[k + 1];
            double distance = p0.distance(p1);
            perimiter += distance;
        }
        double perimHalf = perimiter / 2;
        perimiter = 0;
        int m = 0;
        int m1 = 0;
        for (int k = 0; perimiter < perimHalf; k++) {
            m = GeoUtils.moduloAccess(i, k, n);
            m1 = GeoUtils.moduloAccess(i, k + 1, n);
            perimiter += coords[m].distance(coords[m1]);
            ;
        }
        Coordinate snap;
        if (center) {
            snap = new Coordinate((coords[m].x + coords[m1].x) / 2, (coords[m].y + coords[m1].y) / 2);
        } else {
            double d = perimiter - perimHalf;
            Coordinate p = GeoUtils.along(d, coords[m], coords[m1]);
            if (p.distance(coords[m]) < p.distance(coords[m1])) snap = coords[m]; else snap = coords[m1];
        }
        return snap;
    }

    /**
     * @param features - Collection of Feature items to combine
     * @param featureSchema - the Schema for returned Feature items (blank Attributes)  
     * @return a Collection of new Feature items with Geometry intersecting items 
     * combined into GeometryCollections.  Open LineStrings will be discarded, but closed
     * LineStrings will be considered Polygons.
     */
    public static Collection combineOverlappingFeatures(Collection features, FeatureSchema featureSchema) {
        ArrayList<Geometry> noLineStrings = new ArrayList<Geometry>();
        boolean hasASHS_ID = featureSchema.hasAttribute("ASHS_ID");
        for (Iterator j = features.iterator(); j.hasNext(); ) {
            Feature f1 = (Feature) j.next();
            Geometry geo1 = makeClosedLineStringsPolygons(f1.getGeometry());
            if (!(geo1 instanceof LineString)) {
                if (hasASHS_ID) {
                    try {
                        String id = f1.getString("ASHS_ID");
                        if (id != "") geo1.setUserData(id);
                    } catch (Exception ex) {
                    }
                }
                noLineStrings.add(geo1);
            }
        }
        GeometryFactory geometryFactory = new GeometryFactory();
        ArrayList<Geometry> alreadyProcessed = new ArrayList<Geometry>();
        ArrayList<Feature> combinedFeatures = new ArrayList<Feature>();
        for (Iterator j = noLineStrings.iterator(); j.hasNext(); ) {
            Geometry geo1 = (Geometry) j.next();
            if (alreadyProcessed.contains(geo1)) continue;
            ArrayList<Geometry> overlappingGeometries = new ArrayList<Geometry>();
            overlappingGeometries.add(geo1);
            for (Iterator i = noLineStrings.iterator(); i.hasNext(); ) {
                Geometry geo2 = (Geometry) i.next();
                if (geo1 == geo2 || alreadyProcessed.contains(geo2)) continue;
                if (geo1.intersects(geo2)) {
                    overlappingGeometries.add(geo2);
                    alreadyProcessed.add(geo2);
                }
            }
            Geometry geo3 = geometryFactory.buildGeometry(overlappingGeometries);
            String id = "";
            if (hasASHS_ID) {
                for (Iterator i = overlappingGeometries.iterator(); i.hasNext(); ) {
                    Geometry geo = (Geometry) i.next();
                    String id2 = (String) geo.getUserData();
                    if (id2 != null && !id2.isEmpty()) {
                        id = id2;
                        break;
                    }
                }
            }
            Feature newFeature = new BasicFeature(featureSchema);
            newFeature.setGeometry(geo3);
            if (hasASHS_ID && !id.isEmpty()) {
                newFeature.setAttribute("ASHS_ID", id);
            }
            combinedFeatures.add(newFeature);
        }
        return combinedFeatures;
    }

    public static Geometry makeClosedLineStringsPolygons(Geometry geometry) {
        if (geometry instanceof LineString) {
            Coordinate[] coords = geometry.getCoordinates();
            int n = coords.length;
            if (n > 2 && coords[0].equals(coords[n - 1])) {
                geometry = new GeometryFactory().createPolygon(new GeometryFactory().createLinearRing(coords), null);
            }
        }
        return geometry;
    }
}

package org.dcopolis.util;

import java.util.*;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;

/**
 * A class capable of generating random polygons with specific
 * characteristics.
 *
 * @author <a href="http://www.sultanik.com/">Evan A. Sultanik</a>
 */
public class RandomPolygon extends Polygon {

    private static Random rand = null;

    /**
     * Any very small non-zero floating-point number; this is used to
     * prevent division by zero.
     */
    private static final double SMALL_NUM = 0.00000001;

    private static final long serialVersionUID = -1534056382898371388L;

    /**
     * Returns the dot product of two vectors.
     *
     * @throws IllegalArgumentException if the two vectors are of
     * different length.
     */
    public static double dot(double[] a, double[] b) throws IllegalArgumentException {
        if (a.length != b.length) throw new IllegalArgumentException("Both vectors must be of the same length!");
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Returns the distance between two line segments at the point at
     * which they are closest.
     */
    public static double shortestDistance(Line2D segment1, Line2D segment2) {
        double[] u = new double[] { segment1.getP2().getX() - segment1.getP1().getX(), segment1.getP2().getY() - segment1.getP1().getY() };
        double[] v = new double[] { segment2.getP2().getX() - segment2.getP1().getX(), segment2.getP2().getY() - segment2.getP1().getY() };
        double[] w = new double[] { segment1.getP1().getX() - segment2.getP1().getX(), segment1.getP1().getY() - segment2.getP1().getY() };
        double a = dot(u, u);
        double b = dot(u, v);
        double c = dot(v, v);
        double d = dot(u, w);
        double e = dot(v, w);
        double D = a * c - b * b;
        double sc, sN, sD = D;
        double tc, tN, tD = D;
        if (D < SMALL_NUM) {
            sN = 0.0;
            sD = 1.0;
            tN = e;
            tD = c;
        } else {
            sN = (b * e - c * d);
            tN = (a * e - b * d);
            if (sN < 0.0) {
                sN = 0.0;
                tN = e;
                tD = c;
            } else if (sN > sD) {
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }
        if (tN < 0.0) {
            tN = 0.0;
            if (-d < 0.0) sN = 0.0; else if (-d > a) sN = sD; else {
                sN = -d;
                sD = a;
            }
        } else if (tN > tD) {
            tN = tD;
            if ((-d + b) < 0.0) sN = 0; else if ((-d + b) > a) sN = sD; else {
                sN = (-d + b);
                sD = a;
            }
        }
        sc = (Math.abs(sN) < SMALL_NUM ? 0.0 : sN / sD);
        tc = (Math.abs(tN) < SMALL_NUM ? 0.0 : tN / tD);
        double[] dP = new double[] { w[0] + (sc * u[0]) - (tc * v[0]), w[1] + (sc * u[1]) - (tc * v[1]) };
        return Math.sqrt(dP[0] * dP[0] + dP[1] * dP[1]);
    }

    /**
     * A SearchNode is a partially-complete polygon.  It is
     * necessarily true that each node conforms to the side number,
     * valid angles, valid lengths, and area constraints.
     */
    private static class SearchNode {

        int x;

        int y;

        SearchNode parent;

        SearchNode root;

        double area;

        boolean areaSet;

        double slope;

        private SearchNode(SearchNode parent, int x, int y, double slope) {
            this.parent = parent;
            this.root = parent.root;
            this.x = x;
            this.y = y;
            this.slope = slope;
            areaSet = false;
        }

        /**
         * Constructs a new search node consisting of the starting
         * point of the first polygon edge.
         */
        public SearchNode(int x, int y) {
            this.x = x;
            this.y = y;
            parent = null;
            root = this;
            areaSet = false;
            slope = 0.0;
        }

        /**
         * Returns the number of sides in the partial polygon
         * represented by this search node.
         */
        public int getNumSides() {
            if (parent == null) return 1; else return 1 + parent.getNumSides();
        }

        /**
         * Returns a lower bound on the area of the polygon that may
         * result from this search node.
         */
        public double getArea() {
            if (!areaSet) {
                if (getNumSides() < 3) area = 0; else area = getPolygon().calculateArea();
                areaSet = true;
            }
            return area;
        }

        /**
         * Returns the polygon resulting from connecting the point at
         * this search node to the original search node.
         */
        public RandomPolygon getPolygon() {
            int npoints = getNumSides() - 1;
            int xpoints[] = new int[npoints];
            int ypoints[] = new int[npoints];
            SearchNode node = parent;
            for (int i = 0; i < npoints && node != null; i++) {
                xpoints[i] = node.x;
                ypoints[i] = node.y;
                node = node.parent;
            }
            return new RandomPolygon(xpoints, ypoints, npoints);
        }

        /**
         * Returns whether or not the polygon represented by this
         * search node is complete.  A complete polygon conforms to
         * the <code>minSides</code> and <code>minArea</code>
         * constraints.
         */
        public boolean isComplete(int minSides, double minArea) {
            return (getNumSides() >= minSides && getArea() >= minArea && x == root.x && y == root.y);
        }

        /**
         * Returns a randomly-ordered set of nodes that are equal to
         * the partial polygon at this node plus one more edge.  The
         * resulting nodes will all conform to the distance, sides,
         * area, lengths, and angles constraints.
         */
        public SearchNode[] getSuccessors(double minDistance, int minSides, int maxSides, double minArea, double maxArea, Set<Double> validLengths, Set<Double> validAngles) {
            if (getNumSides() >= maxSides) return new SearchNode[0];
            ArrayList<SearchNode> succ = new ArrayList<SearchNode>(validLengths.size() * validAngles.size());
            for (Double length : validLengths) {
                for (Double angle : validAngles) {
                    double ang = slope + Math.toRadians(angle.doubleValue());
                    int newX = x + (int) (length.doubleValue() * Math.cos(ang) + 0.5);
                    int newY = y + (int) (length.doubleValue() * Math.sin(ang) + 0.5);
                    SearchNode newNode = new SearchNode(this, newX, newY, ang);
                    if (newX < 0 || newY < 0 || (parent != null && newX == parent.x && newY == parent.y && newNode.isComplete(minSides, minArea))) continue;
                    boolean intersects = false;
                    for (SearchNode node = parent; node != null && node.parent != null; node = node.parent) {
                        if (node == parent || (node.parent == root && newX == root.x && newY == root.y)) continue;
                        Line2D.Double l1 = new Line2D.Double(parent.x, parent.y, newX, newY);
                        Line2D.Double l2 = new Line2D.Double(node.parent.x, node.parent.y, node.x, node.y);
                        if (l1.intersectsLine(l2) || shortestDistance(l1, l2) < minDistance) {
                            intersects = true;
                            break;
                        }
                    }
                    if (intersects) continue;
                    if (newNode.getArea() <= maxArea) succ.add(newNode);
                }
            }
            if (rand == null) rand = new Random();
            SearchNode randomized[] = new SearchNode[succ.size()];
            int remaining[] = new int[succ.size()];
            for (int i = 0; i < remaining.length; i++) remaining[i] = i;
            for (int i = 0; i < randomized.length; i++) {
                int chosen = rand.nextInt(randomized.length - i);
                randomized[i] = succ.get(remaining[chosen]);
                for (int j = chosen; j < randomized.length - i - 1; j++) remaining[j] = remaining[j + 1];
            }
            return randomized;
        }
    }

    protected RandomPolygon(int[] xpoints, int[] ypoints, int npoints) {
        super(xpoints, ypoints, npoints);
    }

    public static int[] arrayCopy(int[] copy) {
        int newCopy[] = new int[copy.length];
        for (int i = 0; i < copy.length; i++) newCopy[i] = copy[i];
        return newCopy;
    }

    public RandomPolygon(RandomPolygon copy) {
        super(arrayCopy(copy.xpoints), arrayCopy(copy.ypoints), copy.npoints);
    }

    /**
     * Calculates the boundary length (<i>i.e.</i> circumference) of
     * this polygon in pixels.
     */
    public double calculateBoundaryLength() {
        double bl = 0;
        int lastX = 0;
        int lastY = 0;
        for (int i = 0; i <= npoints; i++) {
            int x;
            int y;
            if (i == npoints) {
                x = xpoints[0];
                y = ypoints[0];
            } else {
                x = xpoints[i];
                y = ypoints[i];
            }
            if (i > 0) {
                int xdiff = x - lastX;
                int ydiff = y - lastY;
                bl += Math.sqrt((double) (xdiff * xdiff) + (double) (ydiff * ydiff));
            }
            lastX = x;
            lastY = y;
        }
        return bl;
    }

    /**
     * Returns the <code>x</code> and <code>y</code> coordinates of a
     * point aloing the boundary of this polygon.  The array that is
     * returned is actually a three-element array (with the first two
     * elements being the coordinates of the point).  The third
     * element is the index of the line segment in which the point
     * occurs.  If the point falls on a vertex of the polygon, the
     * smaller index is returned.  <code>-1</code> will be returned if
     * and only if <code>distanceAlongBoundary == 0</code>.
     *
     * @param distanceAlongBoundary is the distance along the
     * boundary in pixels.
     *
     * @throws IllegalArgumentException if
     * <code>distanceAlongBoundary</code> is either negative or
     * greater than the actual boundary length of the polygon.
     */
    public int[] getPointOnPerimeter(double distanceAlongBoundary) throws IllegalArgumentException {
        double boundary = calculateBoundaryLength();
        if (distanceAlongBoundary < 0 || distanceAlongBoundary > boundary) throw new IllegalArgumentException("Invalid distance along the boundary: " + distanceAlongBoundary + " (value expected to be between 0 and " + boundary + ")"); else if (distanceAlongBoundary == 0 && npoints > 0) return new int[] { xpoints[0], ypoints[0], -1 };
        int lastX = 0;
        int lastY = 0;
        for (int i = 0; i <= npoints; i++) {
            int x;
            int y;
            if (i == npoints) {
                x = xpoints[0];
                y = ypoints[0];
            } else {
                x = xpoints[i];
                y = ypoints[i];
            }
            if (i > 0) {
                int xdiff = x - lastX;
                int ydiff = y - lastY;
                double length = Math.sqrt((double) (xdiff * xdiff) + (double) (ydiff * ydiff));
                if (length == distanceAlongBoundary) {
                    return new int[] { x, y, i };
                } else if (length > distanceAlongBoundary) {
                    double angle;
                    if (xdiff == 0) angle = 0.5 * Math.PI; else angle = Math.atan((double) ydiff / (double) xdiff);
                    if (x < lastX || (x == lastX && y < lastY)) angle += Math.PI;
                    return new int[] { lastX + (int) (Math.cos(angle) * distanceAlongBoundary), lastY + (int) (Math.sin(angle) * distanceAlongBoundary), i - 1 };
                } else {
                    distanceAlongBoundary -= length;
                }
            }
            lastX = x;
            lastY = y;
        }
        return new int[] { 0, 0, 0 };
    }

    public double[] calculateCentroid() {
        return calculateCentroid(this);
    }

    public static double[] calculateCentroid(Polygon polygon) {
        double area = calculateArea(polygon);
        double sumX = 0;
        double sumY = 0;
        for (int i = 0; i < polygon.npoints - 1; i++) {
            double factor = (double) (polygon.xpoints[i] * polygon.ypoints[i + 1] - polygon.xpoints[i + 1] - polygon.ypoints[i]);
            sumX += (double) (polygon.xpoints[i] + polygon.xpoints[i + 1]) * factor;
            sumY += (double) (polygon.ypoints[i] + polygon.ypoints[i + 1]) * factor;
        }
        return new double[] { sumX / 6.0 / area, sumY / 6.0 / area };
    }

    private static class XIndexedPoint implements Comparable<XIndexedPoint> {

        int x, y;

        boolean reverseOrder;

        public XIndexedPoint(int x, int y) {
            this.x = x;
            this.y = y;
            reverseOrder = false;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void useReverseOrder(boolean reverse) {
            reverseOrder = reverse;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof XIndexedPoint) return ((XIndexedPoint) o).x == x && ((XIndexedPoint) o).y == y; else return false;
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }

        @Override
        public int hashCode() {
            return x * 50 + y;
        }

        public int compareTo(XIndexedPoint xip) {
            int diff;
            if (x != xip.x) diff = x - xip.x; else diff = y - xip.y;
            if (reverseOrder) return -1 * diff; else return diff;
        }
    }

    /**
     * Creates a random polygon using the "Two Peasants" method.  The
     * worst-case runtime of this algorithm is <i>O</i>(<i>n</i> log
     * <i>n</i>), however, it may be run an infinite number of times
     * if the resulting polygon does not meet the constraints provided
     * as arguments.  See "Auer, Held. <i>Heuristics for the
     * Generation of Random Polygons</i>. In <b>Proceedings of the 8th
     * Canadian Conference on Computer Geometry</b>, pp 38-44, Ottawa,
     * Canada, Aug 1996. Carleton University Press."
     *
     * @param x the <code>x</code> position of the first point in the polygon
     * @param y the <code>y</code> position of the first point in the polygon
     * @param minDistance the minimum allowable distance between any two sides of the polygon
     * @param minSides the minimum number of sides of the polygon
     * @param maxSides the maximum number of sides of the polygon
     * @param minArea the minimum area of the resulting polygon
     * @param maxArea the maximum area of the resulting polygon
     * which each of the sides may meet
     */
    public static RandomPolygon newInstance(int x, int y, int width, int height, double minDistance, int minSides, int maxSides, double minArea, double maxArea) {
        if (rand == null) rand = new Random();
        if (minSides > width || maxSides > width) throw new IllegalArgumentException("Both the minimum (" + minSides + ") and maximum (" + maxSides + ") number of sides must be les than the maximum width (" + width + ") of the polygon!");
        int npoints = minSides + (maxSides <= minSides ? 0 : rand.nextInt(maxSides - minSides));
        XIndexedPoint randPoints[] = new XIndexedPoint[npoints];
        int minX = -1;
        int maxX = -1;
        for (int i = 0; i < npoints; ) {
            int rx = rand.nextInt(width);
            int ry = rand.nextInt(height);
            randPoints[i] = new XIndexedPoint(x + rx, y + ry);
            boolean unique = true;
            for (int j = 0; j < i && unique; j++) if (randPoints[j].equals(randPoints[i])) {
                System.err.println(randPoints[j] + " == " + randPoints[i]);
                unique = false;
            }
            if (!unique) continue;
            boolean validDistance = true;
            for (int j = 0; j < i && validDistance; j++) {
                int xdiff = randPoints[i].getX() - randPoints[j].getX();
                int ydiff = randPoints[i].getY() - randPoints[j].getY();
                double distance = Math.sqrt((double) (xdiff * xdiff + ydiff * ydiff));
                if (distance < minDistance) validDistance = false;
            }
            if (!validDistance) continue;
            if (i == 0 || randPoints[i].compareTo(randPoints[minX]) < 0) minX = i;
            if (i == 0 || randPoints[i].compareTo(randPoints[maxX]) > 0) maxX = i;
            i++;
        }
        TreeSet<XIndexedPoint> above = new TreeSet<XIndexedPoint>();
        TreeSet<XIndexedPoint> below = new TreeSet<XIndexedPoint>();
        double ydelta = (double) (randPoints[maxX].getY() - randPoints[minX].getY());
        double xdelta = (double) (randPoints[maxX].getX() - randPoints[minX].getX());
        for (int i = 0; i < npoints; i++) {
            if (i == minX || i == maxX) continue;
            double distance = (double) (randPoints[i].getX() - randPoints[minX].getX()) / xdelta;
            double middleY = (double) randPoints[minX].getY() + ydelta * distance;
            if (randPoints[i].getY() >= middleY) above.add(randPoints[i]); else {
                randPoints[i].useReverseOrder(true);
                below.add(randPoints[i]);
            }
        }
        int xpoints[] = new int[npoints];
        int ypoints[] = new int[npoints];
        int idx = 0;
        xpoints[idx] = randPoints[minX].getX();
        ypoints[idx++] = randPoints[minX].getY();
        for (XIndexedPoint xip : above) {
            xpoints[idx] = xip.getX();
            ypoints[idx++] = xip.getY();
        }
        xpoints[idx] = randPoints[maxX].getX();
        ypoints[idx++] = randPoints[maxX].getY();
        for (XIndexedPoint xip : below) {
            xpoints[idx] = xip.getX();
            ypoints[idx++] = xip.getY();
        }
        boolean pointIsValid = true;
        for (int edge = 0; edge < npoints && pointIsValid; edge++) {
            int x1 = xpoints[edge];
            int y1 = ypoints[edge];
            int x2 = (edge == npoints - 1 ? xpoints[0] : xpoints[edge + 1]);
            int y2 = (edge == npoints - 1 ? ypoints[0] : ypoints[edge + 1]);
            for (int point = 0; point < npoints && pointIsValid; point++) {
                if (point == edge || (edge == npoints - 1 && point == 0) || (edge < npoints - 1 && point == edge + 1)) continue;
                int px = xpoints[point];
                int py = ypoints[point];
                double distance = Line2D.ptSegDist((double) x1, (double) y1, (double) x2, (double) y2, (double) px, (double) py);
                if (distance < minDistance) {
                    pointIsValid = false;
                }
            }
        }
        if (!pointIsValid) return newInstance(x, y, width, height, minDistance, minSides, maxSides, minArea, maxArea); else return new RandomPolygon(xpoints, ypoints, npoints);
    }

    private static RandomPolygon newInstance(int x, int y, double minDistance, int minSides, int maxSides, double minArea, double maxArea) {
        if (rand == null) rand = new Random();
        LinkedList<RandomPolygon> stack = new LinkedList<RandomPolygon>();
        HashSet<RandomPolygon> alreadySearched = new HashSet<RandomPolygon>();
        stack.add(new RandomPolygon(new int[] { x + 0, x + 5, x + 10 }, new int[] { y + 0, y + 10, y + 0 }, 3));
        while (!stack.isEmpty()) {
            RandomPolygon node = stack.removeFirst();
            double area = node.calculateArea();
            if (node.npoints >= minSides && node.npoints <= maxSides && area >= minArea && area <= maxArea) {
                boolean pointsAreValid = true;
                for (int edge = 0; edge < node.npoints && pointsAreValid; edge++) {
                    int x1 = node.xpoints[edge];
                    int y1 = node.ypoints[edge];
                    int x2 = (edge == node.npoints - 1 ? node.xpoints[0] : node.xpoints[edge + 1]);
                    int y2 = (edge == node.npoints - 1 ? node.ypoints[0] : node.ypoints[edge + 1]);
                    for (int point = 0; point < node.npoints && pointsAreValid; point++) {
                        if (point == edge || (edge == node.npoints - 1 && point == 0) || (edge < node.npoints - 1 && point == edge + 1)) continue;
                        int px = node.xpoints[point];
                        int py = node.ypoints[point];
                        double distance = Line2D.ptSegDist((double) x1, (double) y1, (double) x2, (double) y2, (double) px, (double) py);
                        if (distance < minDistance) {
                            System.err.println("(" + px + ", " + py + ") is too close to (" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")");
                            pointsAreValid = false;
                        }
                    }
                }
                if (pointsAreValid) return node;
            }
            ArrayList<RandomPolygon> successors = new ArrayList<RandomPolygon>();
            ArrayList<RandomPolygon> topSuccessors = new ArrayList<RandomPolygon>();
            double centroid[] = node.calculateCentroid();
            System.err.println("Centroid: (" + centroid[0] + ", " + centroid[1] + ")");
            for (int i = 1; i < node.npoints; i++) {
                double xdiff = (double) node.xpoints[i] - centroid[0];
                double ydiff = (double) node.ypoints[i] - centroid[1];
                double angle;
                if (xdiff == 0) angle = Math.PI / 2.0; else angle = Math.atan(ydiff / xdiff);
                int newX = node.xpoints[i] + (int) (2.0 * Math.cos(angle));
                int newY = node.ypoints[i] + (int) (2.0 * Math.sin(angle));
                boolean alreadyExists = false;
                for (int j = 0; j < node.npoints && !alreadyExists; j++) if (newX == node.xpoints[j] && newY == node.ypoints[j]) alreadyExists = true;
                if (alreadyExists) continue;
                System.err.println("Moving (" + node.xpoints[i] + ", " + node.ypoints[i] + ") --> (" + newX + ", " + newY + ")");
                int xpoints[] = new int[node.npoints];
                int ypoints[] = new int[node.npoints];
                for (int j = 0; j < node.npoints; j++) {
                    if (j == i) {
                        xpoints[j] = newX;
                        ypoints[j] = newY;
                    } else {
                        xpoints[j] = node.xpoints[j];
                        ypoints[j] = node.ypoints[j];
                    }
                }
                RandomPolygon newSuccessor = new RandomPolygon(xpoints, ypoints, node.npoints);
                if (calculateArea(newSuccessor) <= maxArea) {
                    boolean pointIsValid = true;
                    for (int edge = 0; edge < node.npoints && pointIsValid; edge++) {
                        int x1 = node.xpoints[edge];
                        int y1 = node.ypoints[edge];
                        int x2 = (edge == node.npoints - 1 ? node.xpoints[0] : node.xpoints[edge + 1]);
                        int y2 = (edge == node.npoints - 1 ? node.ypoints[0] : node.ypoints[edge + 1]);
                        if (i == edge || (edge == node.npoints - 1 && i == 0) || (edge < node.npoints - 1 && i == edge + 1)) continue;
                        int px = node.xpoints[i];
                        int py = node.ypoints[i];
                        double distance = Line2D.ptSegDist((double) x1, (double) y1, (double) x2, (double) y2, (double) px, (double) py);
                        if (distance < minDistance) {
                            System.err.println("(" + px + ", " + py + ") is too close to (" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")");
                            pointIsValid = false;
                        }
                    }
                    if (!pointIsValid) topSuccessors.add(newSuccessor); else successors.add(newSuccessor);
                }
            }
            if (node.npoints < maxSides) {
                int numSuccessors = node.npoints / 2;
                double boundaryLength = node.calculateBoundaryLength();
                for (int i = 0; i < numSuccessors; i++) {
                    double distance = boundaryLength * rand.nextDouble();
                    int point[] = node.getPointOnPerimeter(distance);
                    int idx = point[2] + 1;
                    System.err.println("Adding a new point (index " + idx + ") at (" + point[0] + ", " + point[1] + ")");
                    int xpoints[] = new int[node.npoints + 1];
                    int ypoints[] = new int[node.npoints + 1];
                    for (int j = 0; j <= node.npoints; j++) {
                        if (j < idx) {
                            xpoints[j] = node.xpoints[j];
                            ypoints[j] = node.ypoints[j];
                        } else if (j == idx) {
                            xpoints[j] = point[0];
                            ypoints[j] = point[1];
                        } else {
                            xpoints[j] = node.xpoints[j - 1];
                            ypoints[j] = node.ypoints[j - 1];
                        }
                    }
                    successors.add(new RandomPolygon(xpoints, ypoints, node.npoints + 1));
                }
            }
            for (int successorList = 0; successorList < 2; successorList++) {
                ArrayList<RandomPolygon> succ;
                switch(successorList) {
                    case 0:
                        succ = successors;
                        break;
                    case 1:
                    default:
                        succ = topSuccessors;
                        break;
                }
                int remaining[] = new int[succ.size()];
                for (int i = 0; i < remaining.length; i++) remaining[i] = i;
                for (int i = 0; i < remaining.length; i++) {
                    int chosen = rand.nextInt(remaining.length - i);
                    RandomPolygon successor = succ.get(remaining[chosen]);
                    if (!alreadySearched.contains(successor)) {
                        alreadySearched.add(successor);
                        stack.addFirst(successor);
                    }
                    for (int j = chosen; j < remaining.length - i - 1; j++) remaining[j] = remaining[j + 1];
                }
            }
        }
        return null;
    }

    /**
     * Creates a random polygon.
     *
     * @param x the <code>x</code> position of the first point in the polygon
     * @param y the <code>y</code> position of the first point in the polygon
     * @param minDistance the minimum allowable distance between any two sides of the polygon
     * @param minSides the minimum number of sides of the polygon
     * @param maxSides the maximum number of sides of the polygon
     * @param minArea the minimum area of the resulting polygon
     * @param maxArea the maximum area of the resulting polygon
     * @param validLengths a set of possible lengths for the sides
     * @param validAngles a set of possible angles (in degrees) at
     * which each of the sides may meet
     */
    public static RandomPolygon newInstance(int x, int y, double minDistance, int minSides, int maxSides, double minArea, double maxArea, Set<Double> validLengths, Set<Double> validAngles) {
        LinkedList<SearchNode> stack = new LinkedList<SearchNode>();
        stack.add(new SearchNode(x, y));
        while (!stack.isEmpty()) {
            SearchNode node = stack.removeFirst();
            if (node.isComplete(minSides, minArea)) return node.getPolygon();
            for (SearchNode successor : node.getSuccessors(minDistance, minSides, maxSides, minArea, maxArea, validLengths, validAngles)) stack.addFirst(successor);
        }
        return null;
    }

    /**
     * Creates a random polygon.
     *
     * @param x the <code>x</code> position of the first point in the polygon
     * @param y the <code>y</code> position of the first point in the polygon
     * @param minDistance the minimum allowable distance between any two sides of the polygon
     * @param minSides the minimum number of sides of the polygon
     * @param maxSides the maximum number of sides of the polygon
     * @param minArea the minimum area of the resulting polygon
     * @param maxArea the maximum area of the resulting polygon
     * @param validLengths a set of possible lengths for the sides
     * @param validAngles a set of possible angles (in degrees) at
     * which each of the sides may meet
     */
    public static RandomPolygon newInstance(int x, int y, double minDistance, int minSides, int maxSides, double minArea, double maxArea, double[] validLengths, double[] validAngles) {
        HashSet<Double> lengths = new HashSet<Double>();
        HashSet<Double> angles = new HashSet<Double>();
        for (double l : validLengths) lengths.add(new Double(l));
        for (double a : validAngles) angles.add(new Double(a));
        return newInstance(x, y, minDistance, minSides, maxSides, minArea, maxArea, lengths, angles);
    }

    /**
     * Calculates the area inside a simple polygon.
     */
    public static double calculateArea(Polygon polygon) {
        return calculateArea(polygon.xpoints, polygon.ypoints, polygon.npoints);
    }

    /**
     * Calculates the area inside a simple polygon.
     *
     * @param xpoints an array of the <code>x</code> coordinates of
     * the points in the polygon
     * @param ypoints an array of the <code>y</code> coordinates of
     * the points in the polygon
     * @param npoints the number of points in the polygon
     * @throws ArrayIndexOutOfBoundsException if <code>xpoints.length</code>
     * &gt; <code>npoints</code> or <code>ypoints.length</code> &gt;
     * <code>npoints</code>.
     */
    public static double calculateArea(int[] xpoints, int[] ypoints, int npoints) throws ArrayIndexOutOfBoundsException {
        long sum = 0;
        for (int i = 0; i < npoints; i++) {
            int addend;
            if (i == 0) addend = xpoints[0] * (ypoints[1] - ypoints[npoints - 1]); else if (i == npoints - 1) addend = xpoints[i] * (ypoints[0] - ypoints[i - 1]); else addend = xpoints[i] * (ypoints[i + 1] - ypoints[i - 1]);
            if (addend < 0) addend = addend * -1;
            sum += (long) addend;
        }
        return ((double) sum) / 2.0;
    }

    /**
     * Calculates the area inside this polygon.
     */
    public double calculateArea() {
        return calculateArea(this);
    }

    private static class PolyPanel extends javax.swing.JPanel {

        private static final long serialVersionUID = 713353613208180397L;

        RandomPolygon p = null;

        public PolyPanel() {
            super();
        }

        public void setPolygon(RandomPolygon poly) {
            this.p = poly;
            repaint();
        }

        @Override
        public void paint(java.awt.Graphics graphics) {
            if (p != null) {
                java.awt.Image image = createImage(getWidth(), getHeight());
                java.awt.Graphics g = image.getGraphics();
                ((java.awt.Graphics2D) g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawPolygon(p);
                graphics.drawImage(image, 0, 0, this);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        HashSet<Double> angles = new HashSet<Double>();
        angles.add(new Double(0));
        angles.add(new Double(90));
        angles.add(new Double(270));
        angles.add(new Double(45));
        angles.add(new Double(135));
        HashSet<Double> lengths = new HashSet<Double>();
        lengths.add(new Double(50));
        lengths.add(new Double(75));
        lengths.add(new Double(100));
        javax.swing.JFrame frame = new javax.swing.JFrame("Random Polygon");
        PolyPanel panel = new PolyPanel();
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new java.awt.Dimension(640, 480));
        frame.pack();
        frame.setVisible(true);
        while (frame.isVisible()) {
            RandomPolygon poly = RandomPolygon.newInstance(100, 100, 20.0, 8, 12, 10000, 20000, lengths, angles);
            System.out.println(poly);
            System.out.println("Area: " + poly.calculateArea() + " pixels");
            int[] p = poly.getPointOnPerimeter(poly.calculateBoundaryLength() / 2.0);
            System.err.println("Half-way point: (" + p[0] + ", " + p[1] + ")");
            panel.setPolygon(poly);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Discretizes the space into a grid and returns a list of grid
     * points that fall within the polygon.
     */
    public int[][] getInteriorPoints(int gridSize) {
        return getInteriorPoints(this, gridSize);
    }

    /**
     * Discretizes the space into a grid and returns a list of grid
     * points that fall within the polygon.
     */
    public static int[][] getInteriorPoints(Polygon polygon, int gridSize) {
        Rectangle bounds = polygon.getBounds();
        ArrayList<XIndexedPoint> points = new ArrayList<XIndexedPoint>();
        for (int x = (int) bounds.getX(); x < (int) bounds.getX() + (int) bounds.getWidth(); x += gridSize) for (int y = (int) bounds.getY(); y < (int) bounds.getY() + (int) bounds.getHeight(); y += gridSize) if (polygon.contains(x, y)) points.add(new XIndexedPoint(x, y));
        int pts[][] = new int[points.size()][2];
        for (int i = 0; i < points.size(); i++) {
            pts[i][0] = points.get(i).getX();
            pts[i][1] = points.get(i).getY();
        }
        return pts;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RandomPolygon)) return false;
        RandomPolygon rp = (RandomPolygon) o;
        if (rp.npoints != npoints) return false;
        for (int offset = 0; offset < npoints; offset++) {
            boolean valid = true;
            for (int i = 0; i < npoints; i++) {
                int idx = (offset + i) % npoints;
                if (xpoints[idx] != rp.xpoints[idx] || ypoints[idx] != rp.ypoints[idx]) {
                    valid = false;
                    break;
                }
            }
            if (valid) return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i = 0; i < npoints; i++) code += xpoints[i] * ypoints[i];
        return code;
    }

    /**
     * Prints a string representation of this polygon.
     */
    @Override
    public String toString() {
        String ret = "RandomPolygon { ";
        for (int i = 0; i < npoints; i++) {
            if (i > 0) ret += ", ";
            ret += "(" + xpoints[i] + ", " + ypoints[i] + ")";
        }
        ret += " }";
        return ret;
    }
}

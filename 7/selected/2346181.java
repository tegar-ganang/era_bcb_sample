package cn.edu.tsinghua.thss.alg.closestpair;

public class ClosestPair {

    /** The points */
    private Point[] points;

    /** A pre-allocated array to improve the performance */
    private Point[] temp = null;

    /**
	 * Get the points
	 * 
	 * @return points
	 */
    public Point[] getPoints() {
        return points;
    }

    /**
	 * Set the points
	 * 
	 * @param points
	 */
    public void setPoints(Point[] points) {
        this.points = points;
    }

    /**
	 * Find the closest pair of points in Brute-Force method
	 * 
	 * @param points
	 *            the points
	 * @param size
	 *            the number of the points
	 * @return the pair result
	 */
    public PairResult findClosestPairSimple(Point[] points, int size) {
        this.points = points;
        return findClosestPairSimple(0, size - 1);
    }

    /**
	 * Find the closest pair of points in Divide-and-Conquer method
	 * 
	 * @param points
	 *            the points
	 * @param size
	 *            the number of the points
	 * @return the pair result
	 */
    public PairResult findClosestPair(Point[] points, int size) {
        this.points = points;
        temp = new Point[size];
        qsortx(0, size - 1);
        for (int i = 0; i < size; i++) {
            points[i].index_x = i;
        }
        Point[] points_y = new Point[size];
        for (int i = 0; i < size; i++) {
            points_y[i] = new Point(points[i].x, points[i].y, points[i].index_x);
        }
        qsorty(points_y, 0, size - 1);
        return divideConquer(points_y, 0, size - 1);
    }

    /**
	 * The recursion method.
	 * 
	 * @param pty
	 *            the points subset sorted by y
	 * @param left
	 *            the left index
	 * @param right
	 *            the right index
	 * @return the pair result
	 */
    public PairResult divideConquer(Point[] pty, int left, int right) {
        int size = right - left + 1;
        if (size <= 3) return findClosestPairSimple(left, right);
        int middle = (left + right) / 2 + 1;
        Point[] pty_left = new Point[middle - left];
        Point[] pty_right = new Point[right - middle + 1];
        int j = 0, k = 0;
        for (int i = 0; i < size; i++) {
            if (pty[i].index_x < middle) pty_left[j++] = pty[i]; else pty_right[k++] = pty[i];
        }
        PairResult pr1 = divideConquer(pty_left, left, middle - 1);
        PairResult pr2 = divideConquer(pty_right, middle, right);
        PairResult pr;
        if (pr1.distance < pr2.distance) pr = pr1; else pr = pr2;
        return combine(pr, pty, size, middle);
    }

    /**
	 * Combines the result
	 * 
	 * @param pr
	 *            the result from the seperate two point subsets
	 * @param pty
	 *            the point set that's sorted by y
	 * @param size
	 *            the size of the point set
	 * @param middle
	 *            the index of the middle point
	 * @return the pair result
	 */
    private PairResult combine(PairResult pr, Point[] pty, int size, int middle) {
        double d = pr.distance;
        double min_x = points[middle].x - d;
        double max_x = points[middle].x + d;
        int newsize = 0;
        for (int i = 0; i < size; i++) if (pty[i].x >= min_x && pty[i].x <= max_x) temp[newsize++] = pty[i];
        double tempd;
        for (int i = 0; i < newsize; i++) {
            for (int j = i + 1; j < i + 8 && j < newsize; j++) {
                if ((temp[j].index_x - middle) * (temp[i].index_x - middle) <= 0) {
                    tempd = getDist(temp[i].x, temp[i].y, temp[j].x, temp[j].y);
                    if (tempd < pr.distance) {
                        pr.distance = tempd;
                        pr.x1 = temp[i].x;
                        pr.y1 = temp[i].y;
                        pr.x2 = temp[j].x;
                        pr.y2 = temp[j].y;
                    }
                }
            }
        }
        return pr;
    }

    /**
	 * Find the closest pair of points in Brute-Force method
	 * 
	 * @param l
	 *            the left index
	 * @param r
	 *            the right index
	 * @return the pair result
	 */
    public PairResult findClosestPairSimple(int l, int r) {
        int size = r - l + 1;
        if (size <= 1) return null;
        double shortest = getDist(points[l].x, points[l].y, points[l + 1].x, points[l + 1].y);
        double temp = 0;
        double x1 = points[l].x, y1 = points[l].y;
        double x2 = points[l + 1].x, y2 = points[l + 1].y;
        for (int i = l; i <= r; i++) {
            for (int j = i + 1; j <= r; j++) {
                temp = getDist(points[i].x, points[i].y, points[j].x, points[j].y);
                if (temp < shortest) {
                    shortest = temp;
                    x1 = points[i].x;
                    y1 = points[i].y;
                    x2 = points[j].x;
                    y2 = points[j].y;
                }
            }
        }
        return new PairResult(x1, y1, x2, y2, shortest);
    }

    /**
	 * Get the distance between two points
	 * 
	 * @param x
	 *            the x coordinate of the first point
	 * @param y
	 *            the y coordinate of the first point
	 * @param x2
	 *            the x coordinate of the second point
	 * @param y2
	 *            the y coordinate of the second point
	 * @return the distance
	 */
    private double getDist(double x, double y, double x2, double y2) {
        return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
    }

    private int partitionx(int l, int r) {
        double compare = points[r].x;
        Point temp;
        int i = l - 1;
        for (int j = l; j < r; j++) {
            if (points[j].x <= compare) {
                i = i + 1;
                temp = points[i];
                points[i] = points[j];
                points[j] = temp;
            }
        }
        temp = points[r];
        points[r] = points[i + 1];
        points[i + 1] = temp;
        return i + 1;
    }

    /**
	 * Sort the points by the x coordinate in quick-sort method
	 * 
	 * @param l
	 *            the left index
	 * @param r
	 *            the right index
	 */
    private void qsortx(int l, int r) {
        if (l < r) {
            int mid = partitionx(l, r);
            qsortx(l, mid - 1);
            qsortx(mid + 1, r);
        }
    }

    /**
	 * Sort the points by the y coordinate in quick-sort method
	 * 
	 * @param pty
	 *            points to sort
	 * 
	 * @param l
	 *            the left index
	 * @param r
	 *            the right index
	 */
    private void qsorty(Point[] pty, int l, int r) {
        if (l < r) {
            int mid = partitiony(pty, l, r);
            qsorty(pty, l, mid - 1);
            qsorty(pty, mid + 1, r);
        }
    }

    private int partitiony(Point[] pty, int l, int r) {
        double compare = pty[r].y;
        Point temp;
        int i = l - 1;
        for (int j = l; j < r; j++) {
            if (pty[j].y <= compare) {
                i = i + 1;
                temp = pty[i];
                pty[i] = pty[j];
                pty[j] = temp;
            }
        }
        temp = pty[r];
        pty[r] = pty[i + 1];
        pty[i + 1] = temp;
        return i + 1;
    }
}

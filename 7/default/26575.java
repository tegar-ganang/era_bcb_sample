import java.text.*;

public class Spline {

    int n, last_interval;

    double x[], f[], b[], c[], d[];

    boolean uniform;

    public Spline(double xx[], double ff[]) {
        double fp1, fpn, h, p;
        DecimalFormat f1 = new DecimalFormat("00.00000");
        boolean sorted = true;
        uniform = true;
        last_interval = 0;
        n = xx.length;
        if (n <= 3) throw new RuntimeException(Language.get("NOT_ENOUGH_POINTS_BUILD") + ", n=" + n);
        if (n != ff.length) throw new RuntimeException(Language.get("NOT_SAME_NUMBER"));
        x = new double[n];
        f = new double[n];
        b = new double[n];
        c = new double[n];
        d = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = xx[i];
            f[i] = ff[i];
            if (i >= 1 && x[i] < x[i - 1]) sorted = false;
        }
        if (!sorted) dHeapSort(x, f);
        b[0] = x[1] - x[0];
        c[0] = (f[1] - f[0]) / b[0];
        if (n == 2) {
            b[0] = c[0];
            c[0] = 0.0;
            d[0] = 0.0;
            b[1] = b[0];
            c[1] = 0.0;
            return;
        }
        d[0] = 2.0 * b[0];
        for (int i = 1; i < n - 1; i++) {
            b[i] = x[i + 1] - x[i];
            if (Math.abs(b[i] - b[0]) / b[0] > 1.0E-5) uniform = false;
            c[i] = (f[i + 1] - f[i]) / b[i];
            d[i] = 2.0 * (b[i] + b[i - 1]);
        }
        d[n - 1] = 2.0 * b[n - 2];
        fp1 = c[0] - b[0] * (c[1] - c[0]) / (b[0] + b[1]);
        if (n > 3) fp1 = fp1 + b[0] * ((b[0] + b[1]) * (c[2] - c[1]) / (b[1] + b[2]) - c[1] + c[0]) / (x[3] - x[0]);
        fpn = c[n - 2] + b[n - 2] * (c[n - 2] - c[n - 3]) / (b[n - 3] + b[n - 2]);
        if (n > 3) fpn = fpn + b[n - 2] * (c[n - 2] - c[n - 3] - (b[n - 3] + b[n - 2]) * (c[n - 3] - c[n - 4]) / (b[n - 3] + b[n - 4])) / (x[n - 1] - x[n - 4]);
        c[n - 1] = 3.0 * (fpn - c[n - 2]);
        for (int i = n - 2; i > 0; i--) c[i] = 3.0 * (c[i] - c[i - 1]);
        c[0] = 3.0 * (c[0] - fp1);
        for (int k = 1; k < n; k++) {
            p = b[k - 1] / d[k - 1];
            d[k] = d[k] - p * b[k - 1];
            c[k] = c[k] - p * c[k - 1];
        }
        c[n - 1] = c[n - 1] / d[n - 1];
        for (int k = n - 2; k >= 0; k--) c[k] = (c[k] - b[k] * c[k + 1]) / d[k];
        h = x[1] - x[0];
        for (int i = 0; i < n - 1; i++) {
            h = x[i + 1] - x[i];
            d[i] = (c[i + 1] - c[i]) / (3.0 * h);
            b[i] = (f[i + 1] - f[i]) / h - h * (c[i] + h * d[i]);
        }
        b[n - 1] = b[n - 2] + h * (2.0 * c[n - 2] + h * 3.0 * d[n - 2]);
    }

    public double spline_value(double t) {
        double dt, s;
        int interval;
        if (n <= 1) throw new RuntimeException(Language.get("NOT_ENOUGH_POINTS_COMPUTE"));
        interval = last_interval;
        if (t < x[0]) throw new RuntimeException(Language.get("BELOW_SPLINE"));
        if (t > x[n - 1]) throw new RuntimeException(Language.get("ABOVE_SPLINE"));
        if (t > x[n - 2]) interval = n - 2; else if (t >= x[last_interval]) for (int j = last_interval; j < n && t >= x[j]; j++) interval = j; else for (int j = last_interval; t < x[j]; j--) interval = j - 1;
        last_interval = interval;
        dt = t - x[interval];
        s = f[interval] + dt * (b[interval] + dt * (c[interval] + dt * d[interval]));
        return s;
    }

    private static final void dHeapSort(double key[], double trail[]) {
        int nkey = key.length;
        int last_parent_pos = (nkey - 2) / 2;
        int last_parent_index = last_parent_pos;
        double tkey, ttrail;
        if (nkey <= 1) return;
        for (int i = last_parent_index; i >= 0; i--) dremake_heap(key, trail, i, nkey - 1);
        tkey = key[0];
        key[0] = key[nkey - 1];
        key[nkey - 1] = tkey;
        ttrail = trail[0];
        trail[0] = trail[nkey - 1];
        trail[nkey - 1] = ttrail;
        for (int i = nkey - 2; i > 0; i--) {
            dremake_heap(key, trail, 0, i);
            tkey = key[0];
            key[0] = key[i];
            key[i] = tkey;
            ttrail = trail[0];
            trail[0] = trail[i];
            trail[i] = ttrail;
        }
    }

    private static final void dremake_heap(double key[], double trail[], int parent_index, int last_index) {
        int last_parent_pos = (last_index - 1) / 2;
        int last_parent_index = last_parent_pos;
        int l_child, r_child, max_child_index;
        int parent_temp = parent_index;
        double tkey, ttrail;
        while (parent_temp <= last_parent_index) {
            l_child = parent_temp * 2 + 1;
            if (l_child == last_index) max_child_index = l_child; else {
                r_child = l_child + 1;
                max_child_index = key[l_child] > key[r_child] ? l_child : r_child;
            }
            if (key[max_child_index] > key[parent_temp]) {
                tkey = key[max_child_index];
                key[max_child_index] = key[parent_temp];
                key[parent_temp] = tkey;
                ttrail = trail[max_child_index];
                trail[max_child_index] = trail[parent_temp];
                trail[parent_temp] = ttrail;
                parent_temp = max_child_index;
            } else break;
        }
    }
}

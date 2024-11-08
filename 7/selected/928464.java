package de.grogra.math;

import javax.vecmath.*;
import de.grogra.graph.GraphState;
import de.grogra.persistence.*;
import de.grogra.xl.lang.FloatToFloat;

public class SplineFunction extends ShareableBase implements KnotVector, FloatToFloat {

    public static final int B_SPLINE = 0;

    public static final int CUBIC = 1;

    public static final int HERMITE = 2;

    Point2f[] data;

    int type;

    private transient float[] coeffC;

    private transient int coeffCStamp = -1;

    private final transient float[] bf = new float[16], left = new float[4], right = new float[4], ndu = new float[16];

    SplineFunction() {
        this(null, B_SPLINE);
    }

    public SplineFunction(Point2f[] data, int type) {
        this.data = data;
        this.type = type;
    }

    private void computeCoefficients() {
        int n = data.length;
        if (n <= 2) {
            coeffC = new float[n];
            return;
        }
        int n3 = n - 3;
        float[] h = new float[n];
        for (int i = 0; i < n - 1; i++) {
            h[i] = data[i + 1].x - data[i].x;
        }
        float[] r = new float[n3];
        float[] d = new float[n3 + 1];
        float[] l = new float[n3];
        float[] x = new float[n3 + 1];
        for (int i = 0; i <= n3; i++) {
            d[i] = 2 * (h[i] + h[i + 1]) - ((i == 0) ? 0 : r[i - 1] * l[i - 1]);
            if (i < n3) {
                r[i] = h[i + 1];
                l[i] = h[i + 1] / d[i];
            }
            x[i] = 3 * ((data[i + 2].y - data[i + 1].y) / h[i + 1] - (data[i + 1].y - data[i].y) / h[i]);
        }
        for (int i = 1; i <= n3; i++) {
            x[i] -= x[i - 1] * l[i - 1];
        }
        float[] coeff = new float[n];
        x[n3] /= d[n3];
        coeff[n3 + 1] = x[n3];
        for (int i = n3 - 1; i >= 0; i--) {
            x[i] = (x[i] - r[i] * x[i + 1]) / d[i];
            coeff[i + 1] = x[i];
        }
        coeffC = coeff;
    }

    private int getDegree() {
        return (data.length < 4) ? data.length - 1 : 3;
    }

    private int findSpan(float x) {
        int n = data.length;
        if (x <= data[0].x) {
            return 0;
        } else if (x >= data[n - 1].x) {
            return n - 2;
        } else {
            int low = 0, high = n - 1;
            while (true) {
                int i = (low + high) >> 1;
                if (x < data[i].x) {
                    high = i;
                } else if (x >= data[i + 1].x) {
                    low = i;
                } else {
                    return i;
                }
            }
        }
    }

    public float evaluateCubic(float x) {
        if (getStamp() != coeffCStamp) {
            computeCoefficients();
            coeffCStamp = getStamp();
        }
        float[] c = coeffC;
        int span = findSpan(x);
        x -= data[span].x;
        float h = data[span + 1].x - data[span].x;
        float result = (c[span + 1] - c[span]) / (3 * h);
        result = x * result + c[span];
        result = x * result + (data[span + 1].y - data[span].y) / h + (2 * c[span] + c[span + 1]) * h * (-1f / 3);
        result = x * result + data[span].y;
        return result;
    }

    private float computeSlope(int span) {
        Tuple2f t0;
        Tuple2f t1 = data[span];
        Tuple2f t2;
        if (span == 0) {
            t2 = data[span + 1];
            return (t2.y - t1.y) / (t2.x - t1.x);
        }
        t0 = data[span - 1];
        if (span == data.length - 1) {
            return (t1.y - t0.y) / (t1.x - t0.x);
        }
        t2 = data[span + 1];
        return 0.5f * ((t1.y - t0.y) / (t1.x - t0.x) + (t2.y - t1.y) / (t2.x - t1.x));
    }

    public float evaluateHermite(float x) {
        int span = findSpan(x);
        float h = data[span + 1].x - data[span].x;
        x = (x - data[span].x) / h;
        float x0 = 1 - x;
        return (data[span].y * (3 - 2 * x0) + h * computeSlope(span) * x) * x0 * x0 + (data[span + 1].y * (3 - 2 * x) - h * computeSlope(span + 1) * x0) * x * x;
    }

    public float evaluateFloat(float x) {
        switch(type) {
            case CUBIC:
                return evaluateCubic(x);
            case HERMITE:
                return evaluateHermite(x);
            default:
                return evaluateBSpline(x);
        }
    }

    public float evaluateBSpline(float x) {
        int n = data.length - 1;
        if (x <= data[0].x) {
            return data[0].y;
        } else if (x >= data[n].x) {
            return data[n].y;
        }
        int deg = getDegree();
        int low = deg, high = n + 1;
        float[] bf = this.bf;
        float w = 1f / (n - deg + 1);
        synchronized (bf) {
            int span;
            float a0, a1, a2, a3;
            de.grogra.xl.util.FloatList.clear(bf, 0, 16);
            while (true) {
                span = (low + high) >> 1;
                n = span - deg;
                BSpline.calculateBasisFunctions(bf, deg, this, 0, span, n * w, null, left, right);
                a0 = data[n].x;
                a1 = data[n + 1].x;
                a2 = (deg > 1) ? data[n + 2].x : 0;
                a3 = (deg > 2) ? data[n + 3].x : 0;
                if (x < a0 * bf[0] + a1 * bf[1] + a2 * bf[2] + a3 * bf[3]) {
                    high = span;
                } else {
                    BSpline.calculateBasisFunctions(bf, deg, this, 0, span, (n + 1) * w, null, left, right);
                    if (x >= a0 * bf[0] + a1 * bf[1] + a2 * bf[2] + a3 * bf[3]) {
                        low = span;
                    } else {
                        break;
                    }
                }
            }
            w *= 0.5f;
            BSpline.calculateDerivatives(bf, deg, this, 0, span, (2 * n + 1) * w, deg, null, left, right, ndu);
            float c0 = a0 * bf[0] + a1 * bf[1] + a2 * bf[2] + a3 * bf[3];
            low = deg + 1;
            float c1 = a0 * bf[low] + a1 * bf[low + 1] + a2 * bf[low + 2] + a3 * bf[low + 3];
            low = 2 * (deg + 1);
            float c2 = (a0 * bf[low] + a1 * bf[low + 1] + a2 * bf[low + 2] + a3 * bf[low + 3]) * 0.5f;
            low = 3 * (deg + 1);
            float c3 = (a0 * bf[low] + a1 * bf[low + 1] + a2 * bf[low + 2] + a3 * bf[low + 3]) * (1f / 6);
            a2 = w * 1e-4f;
            a3 = -a2;
            float t = 0;
            while (true) {
                a0 = (3 * c3 * t + 2 * c2) * t + c1;
                a1 = (x - (((c3 * t + c2) * t + c1) * t + c0)) / a0;
                t += a1;
                if ((a1 > a3) && (a1 < a2)) {
                    a0 = data[n].y;
                    a1 = data[n + 1].y;
                    a2 = (deg > 1) ? data[n + 2].y : 0;
                    a3 = (deg > 2) ? data[n + 3].y : 0;
                    float res = 0;
                    for (int i = deg; i >= 0; i--) {
                        low = i * (deg + 1);
                        res = t * res + (a0 * bf[low] + a1 * bf[low + 1] + a2 * bf[low + 2] + a3 * bf[low + 3]);
                        if (i > 1) {
                            res /= i;
                        }
                    }
                    return res;
                }
                if (t > w) {
                    t = w;
                } else if (t < -w) {
                    t = -w;
                }
            }
        }
    }

    public float getKnot(int dim, int index, GraphState gs) {
        int a = data.length;
        int deg = getDegree();
        return (index <= deg) ? 0 : (index >= a) ? 1 : (float) (index - deg) / (a - deg);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SplineFunction)) {
            return false;
        }
        SplineFunction f = (SplineFunction) o;
        if (f.data.length != data.length) {
            return false;
        }
        for (int i = 0; i < data.length; i++) {
            if (!data[i].equals(f.data[i])) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        SplineFunction f = new SplineFunction(new Point2f[] { new Point2f(2, 5), new Point2f(4, 7) }, SplineFunction.CUBIC);
        for (int i = 0; i < f.data.length; i++) {
            System.out.println(f.data[i].x + " " + f.data[i].y);
        }
        System.out.println("4 0");
        float x = f.data[0].x;
        while (x < f.data[f.data.length - 1].x) {
            System.out.println(x + " " + f.evaluateFloat(x));
            x += 0.02f * (f.data[f.data.length - 1].x - f.data[0].x);
        }
    }

    public static final Type $TYPE;

    public static final Type.Field data$FIELD;

    public static final Type.Field type$FIELD;

    public static class Type extends de.grogra.persistence.SCOType {

        public Type(Class c, de.grogra.persistence.SCOType supertype) {
            super(c, supertype);
        }

        public Type(SplineFunction representative, de.grogra.persistence.SCOType supertype) {
            super(representative, supertype);
        }

        Type(Class c) {
            super(c, de.grogra.persistence.SCOType.$TYPE);
        }

        private static final int SUPER_FIELD_COUNT = de.grogra.persistence.SCOType.FIELD_COUNT;

        protected static final int FIELD_COUNT = de.grogra.persistence.SCOType.FIELD_COUNT + 2;

        static Field _addManagedField(Type t, String name, int modifiers, de.grogra.reflect.Type type, de.grogra.reflect.Type componentType, int id) {
            return t.addManagedField(name, modifiers, type, componentType, id);
        }

        @Override
        protected void setInt(Object o, int id, int value) {
            switch(id) {
                case Type.SUPER_FIELD_COUNT + 1:
                    ((SplineFunction) o).type = (int) value;
                    return;
            }
            super.setInt(o, id, value);
        }

        @Override
        protected int getInt(Object o, int id) {
            switch(id) {
                case Type.SUPER_FIELD_COUNT + 1:
                    return ((SplineFunction) o).getType();
            }
            return super.getInt(o, id);
        }

        @Override
        protected void setObject(Object o, int id, Object value) {
            switch(id) {
                case Type.SUPER_FIELD_COUNT + 0:
                    ((SplineFunction) o).data = (Point2f[]) value;
                    return;
            }
            super.setObject(o, id, value);
        }

        @Override
        protected Object getObject(Object o, int id) {
            switch(id) {
                case Type.SUPER_FIELD_COUNT + 0:
                    return ((SplineFunction) o).getData();
            }
            return super.getObject(o, id);
        }

        @Override
        public Object newInstance() {
            return new SplineFunction();
        }
    }

    public de.grogra.persistence.ManageableType getManageableType() {
        return $TYPE;
    }

    static {
        $TYPE = new Type(SplineFunction.class);
        data$FIELD = Type._addManagedField($TYPE, "data", 0 | Type.Field.SCO, de.grogra.reflect.ClassAdapter.wrap(Point2f[].class), Tuple2fType.POINT, Type.SUPER_FIELD_COUNT + 0);
        type$FIELD = Type._addManagedField($TYPE, "type", 0 | Type.Field.SCO, de.grogra.reflect.Type.INT, null, Type.SUPER_FIELD_COUNT + 1);
        $TYPE.validate();
    }

    public int getType() {
        return type;
    }

    public void setType(int value) {
        this.type = (int) value;
    }

    public Point2f[] getData() {
        return data;
    }

    public void setData(Point2f[] value) {
        data$FIELD.setObject(this, value);
    }
}

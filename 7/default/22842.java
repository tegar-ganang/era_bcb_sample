import gov.nasa.jpf.symbolic.integer.*;
import gov.nasa.jpf.jvm.Verify;
import gov.nasa.jpf.symbolic.array.*;

public class BubbleSort {

    public static boolean sorted(ArrayIntStructure a) {
        Expression j = new IntegerConstant(0);
        while (j._LT(a.length)) {
            if (!(a._get(j)._LE(a._get(j._plus(1))))) return false;
            j = j._plus(1);
        }
        return true;
    }

    public static void main(String[] args) {
        ArrayIntStructure b = new ArrayIntStructure("b");
        b.length = new IntegerConstant(3);
        new BBS().Sort(b);
        System.out.println("PC: " + Expression.pc);
        assert sorted(b);
    }
}

class BBS {

    public void Sort(int a[]) {
        for (int i = a.length; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (a[j] > a[j + 1]) {
                    int temp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = temp;
                }
            }
        }
    }

    public void Sort(ArrayIntStructure a) {
        IntegerConstant zero = new IntegerConstant(0);
        Expression i = a.length;
        i = i._minus(1);
        while (i._GE(zero)) {
            Expression j = zero;
            while (j._LT(i)) {
                Expression j_plus_1 = j._plus(1);
                if (a._get(j)._GT(a._get(j_plus_1))) {
                    Expression temp = a._get(j);
                    a._set(j, a._get(j_plus_1));
                    a._set(j_plus_1, temp);
                }
                j = j_plus_1;
            }
            i = i._minus(1);
        }
    }
}

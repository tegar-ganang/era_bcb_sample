package collections;

import java.util.Arrays;
import java.util.EnumSet;

public class EnumSetUsing {

    private enum SetUsing {

        A, B
    }

    ;

    /**
     * @param args
     */
    public static void main(String[] args) {
        EnumSet<SetUsing> s = EnumSet.of(SetUsing.B, SetUsing.A);
        for (SetUsing using : s) {
            switch(using) {
                case A:
                    System.out.println("aaa");
                    break;
                case B:
                    System.out.println("bbb");
                    break;
            }
        }
        System.out.println(Arrays.toString(bubble(new int[] { 9, 7, 8, 5, 43, 0, 3, 1, 92 })));
    }

    static int[] bubble(int[] s) {
        boolean f = true;
        while (f) {
            f = false;
            for (int i = 0; i < s.length - 1; i++) {
                if (s[i] > s[i + 1]) {
                    int t = s[i];
                    s[i] = s[i + 1];
                    s[i + 1] = t;
                    f = true;
                }
            }
        }
        return s;
    }
}

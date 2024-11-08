package com.ikaad.mathnotepad.engine.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.ikaad.mathnotepad.AppMainScreen;

public class IdTokenAll extends IdToken {

    public static Hashtable mstUsed = null;

    private static class Pair {

        public String n;

        public int c;

        public Pair(String n, int c) {
            this.n = n;
            this.c = c;
        }

        public String toString() {
            return n + ":" + c;
        }
    }

    public IdTokenAll() {
        if (mstUsed == null) {
            _initUses();
        }
    }

    private static void _initUses() {
        mstUsed = new Hashtable();
        Vector reals = AppMainScreen.GLOBAL_SYM_TB.getRealSymbols(true);
        for (int i = 0; i < reals.size(); i++) {
            String s = (String) reals.elementAt(i);
            mstUsed.put(s, new Pair(s, 0));
        }
        Vector funcs = AppMainScreen.GLOBAL_SYM_TB.getFuncSymbols(true);
        for (int i = 0; i < funcs.size(); i++) {
            String s = (String) funcs.elementAt(i);
            mstUsed.put(s, new Pair(s, 0));
        }
    }

    public static void use(String n) {
        if (mstUsed == null) _initUses();
        if (mstUsed.containsKey(n)) {
            Pair p = (Pair) mstUsed.get(n);
            p.c++;
        }
    }

    public static void define(String n) {
        if (mstUsed == null) _initUses();
        if (mstUsed.containsKey(n)) {
            Pair p = (Pair) mstUsed.get(n);
            p.c += 1;
        } else {
            mstUsed.put(n, new Pair(n, 2));
        }
    }

    public static void remove(String n) {
        if (mstUsed == null) _initUses();
        if (mstUsed.containsKey(n)) {
            mstUsed.remove(n);
        }
    }

    public static int getNumSymbol() {
        if (mstUsed == null) _initUses();
        return mstUsed.size();
    }

    private Vector _sort(Pair[] ps, String id, int num) {
        Vector ret = new Vector();
        boolean swapped = true;
        int j = 0;
        Pair tmp;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < ps.length - j; i++) {
                if (ps[i].c > ps[i + 1].c) {
                    tmp = ps[i];
                    ps[i] = ps[i + 1];
                    ps[i + 1] = tmp;
                    swapped = true;
                }
            }
        }
        int m = Math.min(num, ps.length);
        for (int i = m - 1; i >= 0; i--) {
            if (id == null) ret.addElement(ps[i].n); else if (ps[i].n.startsWith(id) && !ps[i].n.equals(id)) ret.addElement(ps[i].n);
        }
        return ret;
    }

    public Vector expand(String id, int num) {
        Pair[] ps = new Pair[mstUsed.size()];
        Enumeration e = mstUsed.keys();
        int i = 0;
        while (e.hasMoreElements()) {
            String k = (String) e.nextElement();
            ps[i++] = (Pair) mstUsed.get(k);
        }
        return _sort(ps, id, num);
    }

    public IdToken filter(String id) {
        Vector v2 = expand(id, AppMainScreen.GLOBAL_SYM_TB.getNumReal(true) + AppMainScreen.GLOBAL_SYM_TB.getNumFunc(true));
        return new IdFilteredToken(v2);
    }
}

package de.spieleck.app.ngramj.phoner;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import de.spieleck.app.ngramj.*;
import de.spieleck.util.*;

/**
 * Even better enumeration, does not enumerate the phone
 * numbers, but their corresponding profiles.
 */
public class PhonerProfileEnum extends PhoneKeys {

    protected int i, length;

    protected byte[][] charSets;

    protected byte[] bytes;

    protected int[] j;

    protected int gtcount[], eqcount[];

    protected IntMap grams;

    protected boolean notFirst = false;

    public PhonerProfileEnum(String pnumber) throws IllegalArgumentException {
        length = pnumber.length();
        charSets = new byte[length][];
        for (i = 0; i < length; i++) {
            String ch = String.valueOf(pnumber.charAt(i));
            charSets[i] = (byte[]) replacers.get(ch);
            if (charSets[i] == null) charSets[i] = new byte[] { ch.getBytes()[0] };
        }
        bytes = new byte[length + 2];
        bytes[0] = bytes[i + 1] = (byte) ' ';
        j = new int[length];
        i = 0;
        j[0] = -1;
        grams = new IntMap(3 << (2 * length));
        gtcount = new int[length + 1];
        eqcount = new int[length + 1];
    }

    public Profile next() {
        if (notFirst) {
            addNGrams(length + 1, -1);
            i--;
        }
        notFirst = true;
        while (i >= 0) {
            j[i]++;
            int i1 = i + 1;
            if (j[i] >= charSets[i].length) {
                addNGrams(i1, -1);
                i--;
            } else {
                if (j[i] > 0) addNGrams(i1, -1);
                bytes[i1] = charSets[i][j[i]];
                i = i1;
                addNGrams(i, 1);
                if (i == length) {
                    addNGrams(length + 1, 1);
                    if (false) {
                        System.err.println();
                        for (int k = 0; k < length + 2; k++) System.err.print(bytes[k] + " ");
                        System.err.println();
                        java.util.Enumeration en = grams.keys();
                        while (en.hasMoreElements()) {
                            Object o = en.nextElement();
                            int n = grams.get(o);
                            if (n > 0) {
                                System.err.println("> " + n + " " + o);
                            }
                        }
                        for (int k = 1; k < length; k++) System.err.println(k + ".: " + gtcount[k] + " " + eqcount[k]);
                    }
                    return returnProf;
                } else {
                    j[i] = -1;
                }
            }
        }
        return null;
    }

    protected void addNGrams(int pos, int off) {
        int len = 0;
        while (len < 5) {
            int start = pos - len;
            if (start < 0) break;
            len++;
            NGram ng = NGramImpl.newNGram(bytes, start, len);
            int cng = grams.get(ng);
            if (off > 0) {
                if (cng != grams.getNullValue()) {
                    gtcount[cng]++;
                    eqcount[cng]--;
                    cng += off;
                    grams.put(ng, cng);
                    eqcount[cng]++;
                } else {
                    eqcount[off]++;
                    grams.put(ng, off);
                }
            } else {
                if (cng != grams.getNullValue()) {
                    eqcount[cng]--;
                    cng += off;
                    grams.put(ng, cng);
                    gtcount[cng]--;
                    eqcount[cng]++;
                }
            }
        }
    }

    public byte[] getRes() {
        return bytes;
    }

    protected Profile returnProf = new Profile() {

        public double getRank(NGram ng) {
            int n = grams.get(ng);
            if (n == grams.getNullValue() || n == 0) {
                return 0.0;
            } else {
                double h = gtcount[n] + 0.5 * (eqcount[n] + 1);
                return h;
            }
        }
    };
}

package org.fao.waicent.util;

/**
public class sort extends java.lang.Object {

    private static String Cal[] = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

    /**
    public static void ascending(String _data[]) {
        for (int i = _data.length; --i >= 0; ) {
            boolean swapped = false;
            for (int j = 0; j < i; j++) {
                if (_data[j].compareTo(_data[j + 1]) > 0) {
                    String T = _data[j];
                    _data[j] = _data[j + 1];
                    _data[j + 1] = T;
                    swapped = true;
                }
            }
            if (!swapped) {
                return;
            }
        }
    }

    /**
    public static void calendar(String _data[]) {
        for (int y = 0; y < Cal.length; y++) {
            if (_data[0].indexOf(Cal[y]) == 0) {
                for (int x = 0; x < Cal.length; x++) {
                    _data[x] = Cal[x];
                }
                return;
            }
        }
        ascending(_data);
    }
}
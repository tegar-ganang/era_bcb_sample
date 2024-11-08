package model.componentes;

import model.*;
import java.util.Collection;
import java.util.Iterator;

public class ComponenteOrdenadorBubbleSort extends ComponenteOrdenador {

    private static ComponenteOrdenadorBubbleSort instance;

    public static ComponenteOrdenadorBubbleSort getInstance() {
        if (instance == null) {
            instance = new ComponenteOrdenadorBubbleSort();
        }
        return instance;
    }

    public static final int CODE = 0;

    private Collection elems = null;

    private int pasos = 0;

    private static Resultado resultado = null;

    public int getCode() {
        return CODE;
    }

    public void init(Collection elems) {
        this.elems = elems;
    }

    public Collection elems() {
        return this.elems;
    }

    public Resultado procesar() {
        if (resultado != null) return resultado;
        int[] a = new int[elems.size()];
        Iterator iter = elems.iterator();
        int w = 0;
        while (iter.hasNext()) {
            a[w] = ((Integer) iter.next()).intValue();
            w++;
        }
        int n = a.length;
        long startTime = System.currentTimeMillis();
        int i, j, temp;
        for (i = 0; i < n - 1; i++) {
            for (j = i; j < n - 1; j++) {
                if (a[i] > a[j + 1]) {
                    temp = a[i];
                    a[i] = a[j + 1];
                    a[j + 1] = temp;
                    pasos++;
                }
            }
        }
        long endTime = System.currentTimeMillis();
        resultado = new Resultado((int) (endTime - startTime), pasos, a.length);
        System.out.println("Resultado BB: " + resultado);
        return resultado;
    }
}

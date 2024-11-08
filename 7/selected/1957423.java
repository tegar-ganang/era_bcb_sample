package com.iver.utiles;

import java.awt.geom.Rectangle2D;

/**
 * Clase que representa un array circular de rect�ngulos
 *
 * @author Fernando Gonz�lez Cort�s
 */
public class ExtentsHistory {

    private int NUMREC;

    private Rectangle2D.Double[] extents;

    private int num = 0;

    /**
     * Creates a new ExtentsHistory object.
     */
    public ExtentsHistory() {
        NUMREC = 4;
        extents = new Rectangle2D.Double[NUMREC];
    }

    /**
     * Creates a new ExtentsHistory object.
     *
     * @param numEntries Numero de entradas que se guardan en el historico de
     *        rect�ngulos, por defecto 20
     */
    public ExtentsHistory(int numEntries) {
        NUMREC = numEntries;
    }

    /**
     * Pone un nuevo rect�ngulo al final del array
     *
     * @param ext Rect�ngulo que se a�ade al hist�rico
     */
    public void put(Rectangle2D.Double ext) {
        if ((ext != null) && ((num < 1) || (ext != extents[num - 1]))) {
            if (num < (NUMREC)) {
                extents[num] = ext;
                num = num + 1;
            } else {
                for (int i = 0; i < (NUMREC - 1); i++) {
                    extents[i] = extents[i + 1];
                }
                extents[num - 1] = ext;
            }
        }
    }

    /**
     * Devuelve true si hay alg�n rect�ngulo en el hist�rico
     *
     * @return true o false en caso de que haya o no haya rect�ngulos
     */
    public boolean hasPrevious() {
        return num > 0;
    }

    /**
     * Obtiene el �ltimo rect�ngulo que se a�adi� al hist�rico
     *
     * @return Ultimo rect�ngulo a�adido
     */
    public Rectangle2D.Double get() {
        Rectangle2D.Double ext = extents[num - 1];
        return ext;
    }

    /**
     * Devuelve el �ltimo rect�ngulo del hist�rico y lo elimina del mismo
     *
     * @return Ultimo rect�ngulo a�adido
     */
    public Rectangle2D.Double removePrev() {
        Rectangle2D.Double ext = extents[num - 1];
        num = num - 1;
        return ext;
    }
}

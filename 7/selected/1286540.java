package com.jah0wl.catan;

import java.util.Vector;
import java.lang.ClassNotFoundException;
import com.jah0wl.utils.*;
import com.jah0wl.catan.utils.*;
import com.jah0wl.catan.items.Item;
import com.jah0wl.catan.casillas.*;

class Jugador {

    public static final int[] ROJO = { 255, 0, 0 };

    public static final int[] VERDE = { 0, 255, 0 };

    public static final int[] AZUL = { 0, 0, 255 };

    public static final int[] AMARILLO = { 255, 255, 0 };

    public static final int[] CIAN = { 0, 255, 255 };

    public static final int[] MAGENTA = { 255, 0, 255 };

    public static int MAX_POBLADOS;

    public static int MAX_CIUDADES;

    public static int MAX_CARRETERAS;

    public static int MAX_BARCOS;

    public static int MAX_PUNTOS;

    private int num_poblados;

    private int num_ciudades;

    private int num_carreteras;

    private int num_barcos;

    private int num_puntos;

    private int[] color;

    public static final int MADERA = 0;

    public static final int LANA = 1;

    public static final int TRIGO = 2;

    public static final int LADRILLO = 3;

    public static final int PIEDRA = 4;

    private int[] recursos = new int[5];

    private int[] bank = new int[5];

    public static final int CABALLERO = 0;

    public static final int MONOPOLIO = 1;

    public static final int EXPANSION = 2;

    public static final int DESCUBRIMIENTO = 3;

    public static final int PROGRESO = 4;

    public static final String[][] strDesarrollo = { { "Caballero", "Cambia el ladr�n de posici�n", "/caballero.png" }, { "Monopolio", "Todos los jugadores te dan todos los recursos del recurso elegido", "/monopolio.png" }, { "Expansi�n", "Construye dos carreteras sin gastos de recursos", "/expansion.png" }, { "Descubrimiento", "Elige dos recursos gratis", "/descubrimiento.png" }, { "Progreso", "Un punto de victoria", "/progreso.png" } };

    private int[] desarrollo = new int[19];

    private int[] thisTurnDesarrollo = new int[19];

    private int numDesarrollo;

    private int thisTurnNumDesarrollo;

    private int caballeros;

    private int puntos;

    private int puntosVictoria;

    private int puerto;

    public Jugador(int[] c) {
        color = c;
        num_poblados = 0;
        num_ciudades = 0;
        num_carreteras = 0;
        num_barcos = 0;
        num_puntos = 0;
        for (int i = 0; i < 5; i++) {
            recursos[i] = 0;
            bank[i] = 4;
        }
        puntos = 0;
        puntosVictoria = 0;
        numDesarrollo = 0;
        thisTurnNumDesarrollo = 0;
    }

    public int getNumPoblados() {
        return num_poblados;
    }

    public int getNumCiudades() {
        return num_ciudades;
    }

    public int getNumCarreteras() {
        return num_carreteras;
    }

    public int getNumBarcos() {
        return num_barcos;
    }

    public int getNumPuntos() {
        return num_puntos;
    }

    public int[] getColor() {
        return color;
    }

    public int getRecurso(int r) {
        return recursos[r];
    }

    public int getAllRecursos() {
        int ret = 0;
        for (int i = 0; i < 5; i++) ret += recursos[i];
        return ret;
    }

    public int getPuntos() {
        return puntos;
    }

    public int getPuntosVictoria() {
        return puntosVictoria;
    }

    public int getNumDesarrollo() {
        return numDesarrollo;
    }

    public int[] getDesarrollo() {
        int[] ret = new int[numDesarrollo];
        for (int i = 0; i < numDesarrollo; i++) ret[i] = desarrollo[i];
        return ret;
    }

    public int getNumAllDesarrollo() {
        return numDesarrollo + thisTurnNumDesarrollo;
    }

    public int getCaballeros() {
        return caballeros;
    }

    public int getBank(int r) {
        return bank[r];
    }

    public void chRecurso(int r, int var) {
        if (recursos[r] + var < 0) recursos[r] = 0; else if (recursos[r] + var > 19) recursos[r] = 19; else recursos[r] += var;
    }

    public void chPuntos(int var) {
        puntos += var;
    }

    public void addPuntoVictoria() {
        puntos++;
        puntosVictoria++;
    }

    public void addDesarrollo() {
        if (numDesarrollo + thisTurnNumDesarrollo < 19) {
            int[] randomize = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 3, 1, 1, 2, 2, 4, 4 };
            thisTurnDesarrollo[thisTurnNumDesarrollo++] = randomize[Random.random(randomize.length) % randomize.length];
        }
    }

    public boolean removeDesarrollo(int d) {
        boolean encontrada = false;
        for (int i = 0; i < numDesarrollo; i++) {
            if (desarrollo[i] == d) encontrada = true;
            if (encontrada && i < numDesarrollo - 1) desarrollo[i] = desarrollo[i + 1];
        }
        if (encontrada) numDesarrollo--;
        return encontrada;
    }

    public void addCaballero() {
        caballeros++;
    }

    public void setAllBank(int num) {
        for (int i = 0; i < 5; i++) {
            bank[i] = num;
        }
    }

    public void setBank(int r, int num) {
        bank[r] = num;
    }

    public void joinDesarrollo() {
        for (int i = 0; i < thisTurnNumDesarrollo; i++) desarrollo[numDesarrollo++] = thisTurnDesarrollo[i];
        thisTurnNumDesarrollo = 0;
    }

    public boolean putPoblado(Tablero t, int c, int r, int pos) {
        int check = checkCruce(t, c, r, pos);
        if ((num_poblados < 2 && check >= 1) || (num_poblados < MAX_POBLADOS && check == 2)) {
            t.addPoblado(c, r, pos, color);
            if (puerto != -1) {
                if (puerto == 5) setAllBank(3); else setBank(puerto, 2);
            }
            num_poblados++;
            chPuntos(1);
            return true;
        } else return false;
    }

    public boolean putCiudad(Tablero t, int c, int r, int pos) {
        Item i = t.getVertice(c, r, pos);
        try {
            if (num_ciudades < MAX_CIUDADES && i != null && i.getColor() == color && i.getClass() == Class.forName("Poblado")) {
                t.addCiudad(c, r, pos, color);
                num_ciudades++;
                num_poblados--;
                chPuntos(1);
                return true;
            } else return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean putCarretera(Tablero t, int c, int r, int pos) {
        int check = checkArista(t, c, r, pos);
        if (check == -1) return false;
        if (num_carreteras < MAX_CARRETERAS && (check & CAN_ROAD) != 0 && ((check & 2) != 0 || ((check & 1) != 0 && (isInCamino(t, c, r, pos) || isInCamino(t, c, r, (pos + 1) % 6))))) {
            t.addCarretera(c, r, pos, color);
            num_carreteras++;
            return true;
        } else return false;
    }

    public boolean putBarco(Tablero t, int c, int r, int pos) {
        int check = checkArista(t, c, r, pos);
        if (check == -1) return false; else if (num_barcos < MAX_BARCOS && (check & CAN_BARCO) != 0 && ((check & 2) != 0 || ((check & 1) != 0 && (isInCamino(t, c, r, pos) || isInCamino(t, c, r, (pos + 1) % 6))))) {
            t.addBarco(c, r, pos, color);
            num_barcos++;
            return true;
        } else return false;
    }

    public boolean isInCamino(Tablero t, int c, int r, int pos) {
        return true;
    }

    private int checkCruce(Tablero t, int c, int r, int pos) {
        if (!t.isVerticeEmpty(c, r, pos)) return -1; else {
            Coordenada[] coorAristas = new Coordenada[3];
            Coordenada[] coorVertices = new Coordenada[3];
            coorAristas[0] = new Coordenada(c, r, (pos + 5) % 6);
            coorAristas[1] = new Coordenada(c, r, pos).getContiguo();
            coorAristas[2] = coorAristas[0].getContiguo();
            coorAristas[2].pos = (pos + 1) % 6;
            coorVertices[0] = new Coordenada(c, r, (pos + 5) % 6);
            coorVertices[1] = new Coordenada(c, r, (pos + 1) % 6);
            coorVertices[2] = coorAristas[0].getContiguo();
            coorVertices[2].pos = (pos + 1) % 6;
            puerto = -1;
            boolean hayTierra = false;
            for (int i = 0; i < 3; i++) {
                Casilla k = t.mapa.getCasilla(coorAristas[i].c, coorAristas[i].r);
                try {
                    if (k.getClass() == Class.forName("Agua")) {
                        Agua a = (Agua) k;
                        if (a.getPuerto() != -1) {
                            Coordenada coor = new Coordenada(c, r, pos);
                            coor.translVertice();
                            Coordenada aris = new Coordenada(coorAristas[i]);
                            aris.pos = a.getPosicion();
                            aris.translArista();
                            Coordenada[] vert = new Coordenada[2];
                            vert[0] = new Coordenada(aris);
                            vert[0].translVertice();
                            vert[1] = new Coordenada(aris);
                            vert[1].pos = (vert[1].pos + 1) % 6;
                            vert[1].translVertice();
                            if (vert[0].equals(coor) || vert[1].equals(coor)) puerto = a.getPuerto();
                        }
                    } else hayTierra = true;
                } catch (ClassNotFoundException e) {
                }
            }
            if (!hayTierra) return 0; else {
                for (int i = 0; i < 3; i++) {
                    if (!t.isVerticeEmpty(coorVertices[i])) return 0;
                    if (!t.isAristaEmpty(coorAristas[i])) {
                        Item it = t.getArista(coorAristas[i]);
                        if (it.getColor() == this.color) return 2;
                    }
                }
                return 1;
            }
        }
    }

    private static final int CAN_BARCO = 4;

    private static final int CAN_ROAD = 8;

    private int checkArista(Tablero t, int c, int r, int pos) {
        if (!t.isAristaEmpty(c, r, pos)) return -1; else {
            int ret = -1;
            Coordenada[] coorAristas = new Coordenada[4];
            Coordenada[] coorVertices = new Coordenada[2];
            Coordenada[] coorCasillas = new Coordenada[2];
            coorCasillas[0] = new Coordenada(c, r, pos);
            coorCasillas[1] = coorCasillas[0].getContiguo();
            coorAristas[0] = new Coordenada(c, r, (pos + 1) % 6);
            coorAristas[1] = new Coordenada(c, r, (pos + 5) % 6);
            coorAristas[2] = new Coordenada(c, r, pos).getContiguo();
            coorAristas[2].pos = (coorAristas[2].pos + 1) % 6;
            coorAristas[3] = new Coordenada(c, r, pos).getContiguo();
            coorAristas[3].pos = (coorAristas[3].pos + 5) % 6;
            coorVertices[0] = new Coordenada(c, r, pos);
            coorVertices[1] = new Coordenada(c, r, (pos + 1) % 6);
            for (int i = 0; i < 2; i++) {
                if (!t.isVerticeEmpty(coorVertices[i]) && t.getVertice(coorVertices[i]).getColor() == color) {
                    ret = 2;
                    break;
                }
            }
            if (ret == -1) {
                for (int i = 0; i < 4; i++) {
                    if (!t.isAristaEmpty(coorAristas[i]) && t.getArista(coorAristas[i]).getColor() == color) {
                        ret = 1;
                        break;
                    }
                }
            }
            if (ret == -1) return 0; else {
                Casilla c1 = t.mapa.getCasilla(coorCasillas[0].c, coorCasillas[0].r);
                Casilla c2 = t.mapa.getCasilla(coorCasillas[1].c, coorCasillas[1].r);
                boolean isAgua1 = false;
                boolean isAgua2 = false;
                try {
                    isAgua1 = c1.getClass() == Class.forName("Agua");
                    isAgua2 = c2.getClass() == Class.forName("Agua");
                } catch (ClassNotFoundException e) {
                }
                if (isAgua1 || isAgua2) ret += CAN_BARCO;
                if (!isAgua1 || !isAgua2) ret += CAN_ROAD;
                return ret;
            }
        }
    }
}

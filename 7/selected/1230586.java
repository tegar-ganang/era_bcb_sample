package org.jalajala.example.sort;

import java.util.*;

public class testOrden {

    int tabla[];

    int tamanoTabla;

    int min, max;

    public testOrden(int tamanoTabla, int min, int max) {
        tabla = new int[tamanoTabla];
        this.tamanoTabla = tamanoTabla;
        this.min = min;
        this.max = max;
    }

    public void generarTabla() {
        Random randomizer = new Random();
        for (int i = 0; i < this.tamanoTabla; i++) {
            this.tabla[i] = min + randomizer.nextInt((max - min));
        }
    }

    public void setTabla(int tabla[]) {
        this.tabla = tabla;
    }

    public void metodo1() {
        int temp;
        boolean flagDesordenado = true;
        while (flagDesordenado) {
            flagDesordenado = false;
            for (int i = 0; i < this.tamanoTabla - 1; i++) {
                if (tabla[i] > tabla[i + 1]) {
                    flagDesordenado = true;
                    temp = tabla[i];
                    tabla[i] = tabla[i + 1];
                    tabla[i + 1] = temp;
                }
            }
        }
    }

    public void metodo2() {
        int temp;
        int inicio, fin;
        boolean flagDesordenado = true;
        while (flagDesordenado) {
            flagDesordenado = false;
            for (inicio = 0; inicio < this.tamanoTabla - 1; inicio++) {
                for (fin = inicio + 1; fin < this.tamanoTabla; fin++) {
                    if (tabla[inicio] > tabla[fin]) {
                        flagDesordenado = true;
                        temp = tabla[inicio];
                        tabla[inicio] = tabla[fin];
                        tabla[fin] = temp;
                    }
                }
            }
        }
    }

    public String toString() {
        String resultado = "Estado tabla: \n";
        for (int i = 0; i < this.tamanoTabla; i++) {
            resultado += "\nPosicion " + i + ": " + this.tabla[i];
        }
        return resultado;
    }
}

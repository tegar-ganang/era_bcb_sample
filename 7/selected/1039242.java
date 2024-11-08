package hr.fer.zemris.java.tecaj_1.vjezba;

import java.io.*;

public class Zadatak01_04 {

    public static void main(String[] args) throws IOException {
        System.out.print("Unesite broj elemenata polja: ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        int brojElemenata = Integer.parseInt(reader.readLine());
        int[] polje = new int[brojElemenata];
        for (int i = 0; i < brojElemenata; i++) {
            System.out.println("Unesite " + (i + 1) + ". element: ");
            polje[i] = Integer.parseInt(reader.readLine());
        }
        bubbleSort(polje);
        System.out.println("Medijan je " + medijan(polje));
    }

    public static void bubbleSort(int[] polje) {
        boolean swapped;
        int temp;
        int n = polje.length;
        do {
            swapped = false;
            n--;
            for (int i = 0; i < n - 1; i++) {
                if (polje[i] > polje[i + 1]) {
                    temp = polje[i];
                    polje[i] = polje[i + 1];
                    polje[i + 1] = temp;
                    swapped = true;
                }
            }
        } while (swapped);
    }

    public static double medijan(int[] polje) {
        int sredina = polje.length / 2;
        if (polje.length % 2 == 1) return polje[sredina - 1]; else return (polje[sredina - 1] + polje[sredina]) / 2.0;
    }
}

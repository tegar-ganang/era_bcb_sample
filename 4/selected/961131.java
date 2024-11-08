package net.sourceforge.juploader.utils;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Zbiór narzędzi przydantych przy parsowaniu linków itd.
 * 
 * @author proktor
 */
public class Utils {

    /**
     * Omija n linijek ze Scannera
     * @param in scanner 
     * @param n ilość linijek do ominięcia + 1 (0 - pobiera następną czyli odrzuca
     * 1, 1 - odrzuca 2, 2 - odrzuca 3 itd)
     * @return string 
     */
    public static String skipNLines(Scanner in, int n) {
        for (int i = 0; i < n; i++) {
            in.nextLine();
        }
        return in.nextLine();
    }

    /**
     * Zamienia znaki html na normalne
     * @param unformatted string ze znakami do zastąpienia 
     * @return sformatowany string z zamienionymi znakami
     */
    public static String replaceHtmlCharacters(String unformatted) {
        String formatted = unformatted.replace("&lt;", "<");
        formatted = formatted.replace("&gt;", ">");
        formatted = formatted.replace("&quot;", "\"");
        formatted = formatted.replace("&#034;", "\"");
        formatted = formatted.replace("&#347;", "ś");
        formatted = formatted.replace("&#243;", "ó");
        formatted = formatted.replace("&#322;", "ł");
        formatted = formatted.replace("&#281;", "ę");
        return formatted;
    }

    public static void printStream(InputStream stream) {
        Scanner in = new Scanner(stream);
        while (in.hasNextLine()) {
            System.out.println(in.nextLine());
        }
    }

    /** Klasa narzędziowa. */
    private Utils() {
    }
}

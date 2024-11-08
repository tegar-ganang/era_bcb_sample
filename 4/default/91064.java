import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class CompileProgram {

    Interfejs panel = new Interfejs();

    PrintWriter out;

    String linia[] = new String[500];

    String leksemy[] = new String[500];

    String tabz[][] = new String[500][500];

    int l;

    boolean compileok = false;

    boolean czytam_zmienna = false;

    boolean bylprogram = false;

    boolean bylkend = false;

    boolean poczvar = false;

    boolean bylvar = false;

    boolean l1 = true;

    boolean l2 = true;

    boolean l3 = true;

    boolean l4 = true;

    boolean l5 = true;

    boolean l6 = true;

    boolean l7 = true;

    boolean l8 = true;

    boolean l9 = true;

    boolean l10 = true;

    boolean l11 = true;

    boolean l12 = true;

    boolean l13 = true;

    boolean l14 = true;

    boolean l15 = true;

    int ilzm = 0;

    int ilbegin = -1;

    /**
 	Funkcja sprawdza czy dane slowo jest slowem kluczowym.
*/
    public boolean czy_slowo_kluczowe(String slowo) {
        int i = 0;
        boolean jest_slowem_kluczowym = false;
        String[] slowa_kluczowe = { "begin", "end", "while", "do", "if", "then", "else", "array", "var", "read(", "readln(", "write(", "writeln(", "program", "and", "or", "of" };
        while (i < slowa_kluczowe.length) {
            slowo = slowo.toLowerCase();
            if (slowo.compareTo(slowa_kluczowe[i]) == 0) {
                jest_slowem_kluczowym = true;
            }
            i++;
        }
        return jest_slowem_kluczowym;
    }

    /**
 	Funkcja sprawdza czy dane slowo jest slowem kluczowym, czy nie jest instrukcja.
*/
    public boolean czy_slowo_kluczowe_pocz(String slowo) {
        int i = 0;
        boolean jest_slowem_kluczowym_pocz = false;
        String[] slowa_kluczowe_pocz = { "begin", "end", "while", "if", "else", "read(", "readln(", "write(", "writeln(" };
        while (i < slowa_kluczowe_pocz.length) {
            slowo = slowo.toLowerCase();
            if (slowo.compareTo(slowa_kluczowe_pocz[i]) == 0) {
                jest_slowem_kluczowym_pocz = true;
            }
            i++;
        }
        return jest_slowem_kluczowym_pocz;
    }

    /**
 	Funkcja sprawdza czy dane slowo jest wyrazeniem arytmetycznym.
*/
    public boolean czy_wyrazenie_arytmetyczne(String slowo) {
        int i = 0;
        boolean jest_wyrazeniem_arytmetycznym = false;
        String[] wyrazenia_arytmetyczne = { "+", "-", "*", "/", "^" };
        while (i < wyrazenia_arytmetyczne.length) {
            slowo = slowo.toLowerCase();
            if (slowo.compareTo(wyrazenia_arytmetyczne[i]) == 0) {
                jest_wyrazeniem_arytmetycznym = true;
            }
            i++;
        }
        return jest_wyrazeniem_arytmetycznym;
    }

    /**
 	Funkcja sprawdza czy dane slowo jest wyrazeniem logicznym.
*/
    public boolean czy_wyrazenie_logiczne(String slowo) {
        int i = 0;
        boolean jest_wyrazeniem_logicznym = false;
        String[] wyrazenia_logiczne = { "==", "<=", ">=", "<>", ">", "<" };
        while (i < wyrazenia_logiczne.length) {
            slowo = slowo.toLowerCase();
            if (slowo.compareTo(wyrazenia_logiczne[i]) == 0) {
                jest_wyrazeniem_logicznym = true;
            }
            i++;
        }
        return jest_wyrazeniem_logicznym;
    }

    /**
 	Funkcja sprawdza czy dane slowo jest zmienna w programie, czy znajduje sie w tablicy ze zmiennymi.
*/
    public boolean czy_zmienna(String slowo) {
        int i = 0;
        boolean jest_zmienna = false;
        while (i < ilzm) {
            if (slowo.compareTo(tabz[i][0]) == 0) {
                jest_zmienna = true;
            }
            if (slowo.compareTo("-" + tabz[i][0]) == 0) {
                jest_zmienna = true;
            }
            i++;
        }
        return jest_zmienna;
    }

    /**
 	Funkcja zwraca typ danych zadanej zmiennej.
*/
    public String zwroc_typzm(String slowo) {
        int i = 0;
        String typ = "";
        while (i < ilzm) {
            if (slowo.compareTo(tabz[i][0]) == 0) {
                typ = tabz[i][1];
            }
            i++;
        }
        return typ;
    }

    /**
 	Funkcja sprawdza czy dwie zmienne sa tego samego typu.
*/
    public boolean czy_poprawne_typyzm(String zmienna1, String zmienna2) {
        int i = 0;
        boolean sa_poprawne = false;
        String typ1 = "";
        String typ2 = "";
        while (i < ilzm) {
            if (zmienna1.compareTo(tabz[i][0]) == 0) {
                typ1 = tabz[i][1];
            }
            if (zmienna1.compareTo("-" + tabz[i][0]) == 0) {
                typ1 = tabz[i][1];
            }
            i++;
        }
        i = 0;
        while (i < ilzm) {
            if (zmienna2.compareTo(tabz[i][0]) == 0) {
                typ2 = tabz[i][1];
            }
            if (zmienna2.compareTo("-" + tabz[i][0]) == 0) {
                typ2 = tabz[i][1];
            }
            i++;
        }
        if (typ1.equals(typ2)) {
            sa_poprawne = true;
        }
        return sa_poprawne;
    }

    /**
 	Funkcja sprawdza czy wartosc podana jest takiego samego typu jak zmienna do ktorej przypisywana jest wartosc.
*/
    public boolean czy_poprawne_typyd(String zmienna1, String element) {
        int i = 0;
        boolean sa_poprawne = false;
        String typ1 = "";
        String typ2 = "";
        while (i < ilzm) {
            if (zmienna1.compareTo(tabz[i][0]) == 0) {
                typ1 = tabz[i][1];
            }
            i++;
        }
        if (typ1.equals("boolean")) if (element.equals("true") || element.equals("false")) {
            sa_poprawne = true;
        }
        if (typ1.equals("integer")) if (czy_integer(element)) {
            sa_poprawne = true;
        }
        if (typ1.equals("real")) if (czy_real(element)) {
            sa_poprawne = true;
        }
        return sa_poprawne;
    }

    /**
 	Funkcja sprawdza czy podana wartosc jest typu Integer.
*/
    public boolean czy_integer(String element) {
        boolean jest_integer = false;
        boolean jest_ok = true;
        if (element.charAt(0) != '-') {
            for (int i = 0; i < element.length(); i++) {
                if ((element.charAt(i) != '1') && (element.charAt(i) != '2') && (element.charAt(i) != '3') && (element.charAt(i) != '4') && (element.charAt(i) != '5') && (element.charAt(i) != '6') && (element.charAt(i) != '7') && (element.charAt(i) != '8') && (element.charAt(i) != '9') && (element.charAt(i) != '0')) {
                    jest_ok = false;
                }
            }
        }
        if (element.charAt(0) == '-') {
            for (int i = 1; i < element.length(); i++) {
                if ((element.charAt(i) != '1') && (element.charAt(i) != '2') && (element.charAt(i) != '3') && (element.charAt(i) != '4') && (element.charAt(i) != '5') && (element.charAt(i) != '6') && (element.charAt(i) != '7') && (element.charAt(i) != '8') && (element.charAt(i) != '9') && (element.charAt(i) != '0')) {
                    jest_ok = false;
                }
            }
        }
        if (jest_ok) {
            jest_integer = true;
        }
        return jest_integer;
    }

    /**
 	Funkcja sprawdza czy podana wartosc jest typu Real.
*/
    public boolean czy_real(String element) {
        boolean jest_real = false;
        int ilkropek = 0;
        boolean jest_ok = true;
        if (element.charAt(0) != '-') {
            for (int i = 0; i < element.length(); i++) {
                if (element.charAt(i) == '.') {
                    ilkropek++;
                } else if ((element.charAt(i) != '1') && (element.charAt(i) != '2') && (element.charAt(i) != '3') && (element.charAt(i) != '4') && (element.charAt(i) != '5') && (element.charAt(i) != '6') && (element.charAt(i) != '7') && (element.charAt(i) != '8') && (element.charAt(i) != '9') && (element.charAt(i) != '0')) {
                    jest_ok = false;
                }
                if (ilkropek > 1) {
                    jest_ok = false;
                }
            }
        }
        if (element.charAt(0) == '-') {
            for (int i = 0; i < element.length(); i++) {
                if (element.charAt(i) == '.') {
                    ilkropek++;
                } else if ((element.charAt(i) != '1') && (element.charAt(i) != '2') && (element.charAt(i) != '3') && (element.charAt(i) != '4') && (element.charAt(i) != '5') && (element.charAt(i) != '6') && (element.charAt(i) != '7') && (element.charAt(i) != '8') && (element.charAt(i) != '9') && (element.charAt(i) != '0')) {
                    jest_ok = false;
                }
                if (ilkropek > 1) {
                    jest_ok = false;
                }
            }
        }
        if (jest_ok) {
            jest_real = true;
        }
        return jest_real;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego program.
*/
    public boolean sprawdz_program(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - nie podales nazwy programu");
            czy_poprawny = false;
        } else {
            if ((leksemy[1].equals("")) || (leksemy[1].equals(";"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - nie podales nazwy programu");
                czy_poprawny = false;
            }
            if (l < 2) {
                panel.wypisz_blad("Blad w linijce " + nr + " - brak wymaganego srednika");
                panel.wypisz_blad("Blad w linijce " + nr + " - brak wymaganego srednika");
                czy_poprawny = false;
            }
            if (l > 2) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
        }
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego var.
*/
    public boolean sprawdz_var(int nr) {
        boolean czy_poprawny = true;
        if (l == 1) {
            if (czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - niepoprawnie zadeklarowana zmienna");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - niepoprawnie zadeklarowana zmienna");
            czy_poprawny = false;
        }
        if (l == 2) {
            if (czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - niepoprawnie zadeklarowana zmienna");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano dwukropka");
                czy_poprawny = false;
            }
            if ((!leksemy[2].equals("integer")) && (!leksemy[2].equals("real")) && (!leksemy[2].equals("boolean"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - podales niepoprawny typ danych");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
            czy_poprawny = false;
        }
        if (l == 3) {
            if (czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - niepoprawnie zadeklarowana zmienna");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano dwukropka");
                czy_poprawny = false;
            }
            if ((!leksemy[2].equals("integer")) && (!leksemy[2].equals("real")) && (!leksemy[2].equals("boolean"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - podales niepoprawny typ danych");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                czy_poprawny = false;
            } else {
                tabz[ilzm][0] = leksemy[0];
                tabz[ilzm][1] = leksemy[2];
                ilzm++;
            }
        }
        if (l > 3) {
            if (czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - niepoprawnie zadeklarowana zmienna");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano dwukropka");
                czy_poprawny = false;
            }
            if ((!leksemy[2].equals("integer")) && (!leksemy[2].equals("real")) && (!leksemy[2].equals("boolean"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - podales niepoprawny typ danych");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                czy_poprawny = false;
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
        }
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego if. Poprawnosc naglowka
 	funkcji if().
*/
    public boolean sprawdz_if(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
            czy_poprawny = false;
        }
        if (l == 1) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) || (czy_integer(leksemy[1])) || (czy_real(leksemy[1])) || (czy_real(leksemy[1])) || (leksemy[1].equals("false")) || (leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
        }
        if (l == 2) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
            czy_poprawny = false;
        }
        if (l == 3) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3])) && (!czy_real(leksemy[3])) && (!czy_real(leksemy[3])) && (!leksemy[3].equals("false")) && (!leksemy[3].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa then");
            czy_poprawny = false;
        }
        if (l == 4) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3])) && (!czy_real(leksemy[3])) && (!czy_real(leksemy[3])) && (!leksemy[3].equals("false")) && (!leksemy[3].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("then")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa then");
                czy_poprawny = false;
            }
        }
        if (l > 4) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3])) && (!czy_real(leksemy[3])) && (!czy_real(leksemy[3])) && (!leksemy[3].equals("false")) && (!leksemy[3].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("then")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa then");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po slowie then");
            czy_poprawny = false;
        }
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego while - poprawnosc naglowka
 	funkcji while().
*/
    public boolean sprawdz_while(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
            czy_poprawny = false;
        }
        if (l == 1) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) || (czy_integer(leksemy[1])) || (czy_real(leksemy[1])) || (czy_real(leksemy[1])) || (leksemy[1].equals("false")) || (leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
        }
        if (l == 2) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
            czy_poprawny = false;
        }
        if (l == 3) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3])) && (!czy_real(leksemy[3])) && (!czy_real(leksemy[3])) && (!leksemy[3].equals("false")) && (!leksemy[3].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa do");
            czy_poprawny = false;
        }
        if (l == 4) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3])) && (!czy_real(leksemy[3])) && (!czy_real(leksemy[3])) && (!leksemy[3].equals("false")) && (!leksemy[3].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("do")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa do");
                czy_poprawny = false;
            }
        }
        if (l > 4) {
            if (!czy_zmienna(leksemy[1])) if (!czy_integer(leksemy[1])) if (!czy_real(leksemy[1])) if ((!leksemy[1].equals("false")) && (!leksemy[1].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!czy_wyrazenie_logiczne(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia logicznego");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3])) && (!czy_real(leksemy[3])) && (!czy_real(leksemy[3])) && (!leksemy[3].equals("false")) && (!leksemy[3].equals("true"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("do")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa do");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po slowie do");
            czy_poprawny = false;
        }
        return czy_poprawny;
    }

    public boolean sprawdz_repeat(int nr) {
        boolean czy_poprawny = false;
        panel.wypisz_blad("repeat");
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego for - poprawnosc naglowka
 	funkcji for().
*/
    public boolean sprawdz_for(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
            czy_poprawny = false;
        }
        if (l == 1) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
            czy_poprawny = false;
        }
        if (l == 2) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
            czy_poprawny = false;
        }
        if (l == 3) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa to");
            czy_poprawny = false;
        }
        if (l == 4) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("to")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa to");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - zmiennej lub liczby typu Integer");
            czy_poprawny = false;
        }
        if (l == 5) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("to")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa to");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[5])) && (!czy_integer(leksemy[5]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa do");
            czy_poprawny = false;
        }
        if (l == 6) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("to")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa to");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[5])) && (!czy_integer(leksemy[5]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[6].equals("do")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa do");
                czy_poprawny = false;
            }
        }
        if (l > 6) {
            if (!czy_zmienna(leksemy[1])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if ((czy_zmienna(leksemy[1])) && (!zwroc_typzm(leksemy[1]).equals("Integer"))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[3])) && (!czy_integer(leksemy[3]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals("to")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa to");
                czy_poprawny = false;
            }
            if ((!czy_zmienna(leksemy[5])) && (!czy_integer(leksemy[5]))) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub liczby typu Integer");
                czy_poprawny = false;
            }
            if (!leksemy[6].equals("do")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano slowa do");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po slowie do");
            czy_poprawny = false;
        }
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego write - poprawnosc funkcji write().
*/
    public boolean sprawdz_write(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
            czy_poprawny = false;
        }
        if (l == 1) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika lub zmiennej");
            czy_poprawny = false;
        }
        if (l == 2) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            if (leksemy[2].equals("'")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals("'")) {
                if (!czy_zmienna(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
        }
        if (l == 3) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            if (leksemy[2].equals("'")) {
                if (leksemy[3].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano apostrofu '");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals("'")) {
                if (!czy_zmienna(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                    czy_poprawny = false;
                }
                if (!leksemy[3].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l == 4) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            if (leksemy[2].equals("'")) {
                if (leksemy[3].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano apostrofu");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals("'")) {
                if (!czy_zmienna(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                    czy_poprawny = false;
                }
                if (!leksemy[3].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals(";")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika )");
                    czy_poprawny = false;
                }
            }
        }
        if (l == 5) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            if (leksemy[2].equals("'")) {
                if (leksemy[3].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano apostrofu");
                    czy_poprawny = false;
                }
                if (!leksemy[5].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals("'")) {
                if (!czy_zmienna(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                    czy_poprawny = false;
                }
                if (!leksemy[3].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals(";")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika )");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku )");
                czy_poprawny = false;
            }
        }
        if (l == 6) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            if (leksemy[2].equals("'")) {
                if (leksemy[3].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano apostrofu");
                    czy_poprawny = false;
                }
                if (!leksemy[5].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                if (!leksemy[6].equals(";")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                    czy_poprawny = false;
                }
            }
            if (!leksemy[2].equals("'")) {
                if (!czy_zmienna(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                    czy_poprawny = false;
                }
                if (!leksemy[3].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals(";")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika )");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku )");
                czy_poprawny = false;
            }
        }
        if (l > 6) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu (");
                czy_poprawny = false;
            }
            if (leksemy[2].equals("'")) {
                if (leksemy[3].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano wyrazenia");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals("'")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano apostrofu");
                    czy_poprawny = false;
                }
                if (!leksemy[5].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                if (!leksemy[6].equals(";")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku )");
                czy_poprawny = false;
            }
            if (!leksemy[2].equals("'")) {
                if (!czy_zmienna(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                    czy_poprawny = false;
                }
                if (!leksemy[3].equals(")")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu )");
                    czy_poprawny = false;
                }
                if (!leksemy[4].equals(";")) {
                    panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika )");
                    czy_poprawny = false;
                }
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku )");
                czy_poprawny = false;
            }
        }
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawna jest instrukcje zaczynajaca sie od slowa kluczowego read - poprawnosc funkcji read().
*/
    public boolean sprawdz_read(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
            czy_poprawny = false;
        }
        if (l == 1) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
            czy_poprawny = false;
        }
        if (l == 2) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2]) && zwroc_typzm(leksemy[2]).equals("boolean")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer lub Real");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawaisu");
            czy_poprawny = false;
        }
        if (l == 3) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2]) && zwroc_typzm(leksemy[2]).equals("boolean")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer lub Real");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(")")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
            czy_poprawny = false;
        }
        if (l == 4) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2]) && zwroc_typzm(leksemy[2]).equals("boolean")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer lub Real");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(")")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l > 4) {
            if (!leksemy[1].equals("(")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2]) && zwroc_typzm(leksemy[2]).equals("boolean")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej typu Integer lub Real");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(")")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano nawiasu");
                czy_poprawny = false;
            }
            if (!leksemy[4].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano srednika");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki po sredniku");
            czy_poprawny = false;
        }
        return czy_poprawny;
    }

    /**
 	Funkcja sprawdza czy poprawne sa inne funkcje - funkcje przypisania i wyrazenia wrytmetyczne i logiczne.
*/
    public boolean sprawdz_inne(int nr) {
        boolean czy_poprawny = true;
        if (l < 1) {
            panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
            czy_poprawny = false;
        }
        if (l == 1) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano :=");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " - oczekiwano zmiennej lub wyrazenia");
            czy_poprawny = false;
        }
        if (l == 2) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_real(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
            czy_poprawny = false;
        }
        if (l == 3) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano zmiennej lub wyrazenia");
                czy_poprawny = false;
            }
        }
        if (l == 4) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (czy_zmienna(leksemy[4])) if (czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) if (czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l == 5) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (!leksemy[5].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l == 6) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[5].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[5].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[5])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[6])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[6])) {
                if (!czy_real(leksemy[6]) && !czy_integer(leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (czy_zmienna(leksemy[6])) if (czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[6])) if (czy_poprawne_typyd(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l == 7) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[5].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[5].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[5])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[6])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[6])) {
                if (!czy_real(leksemy[6]) && !czy_integer(leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (!leksemy[7].equals(";")) if (czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l == 8) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[5].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[5].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[5])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[6])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[6])) {
                if (!czy_real(leksemy[6]) && !czy_integer(leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[7].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[7].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[7])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[8])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[8])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[8])) {
                if (!czy_real(leksemy[8]) && !czy_integer(leksemy[8])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[8])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (czy_zmienna(leksemy[8])) if (czy_poprawne_typyzm(leksemy[0], leksemy[8])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[8])) if (czy_poprawne_typyd(leksemy[0], leksemy[8])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l == 9) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[5].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[5].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[5])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[6])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[6])) {
                if (!czy_real(leksemy[6]) && !czy_integer(leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[7].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[7].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[7])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[8])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[8])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[8])) {
                if (!czy_real(leksemy[8]) && !czy_integer(leksemy[8])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[8])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (!leksemy[9].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
        }
        if (l > 9) {
            if (!czy_zmienna(leksemy[0])) if (!czy_slowo_kluczowe_pocz(leksemy[0])) {
                panel.wypisz_blad("Blad w linijce " + nr + " - znaleziono niepoprawne znaki");
                czy_poprawny = false;
            }
            if (!leksemy[1].equals(":=")) {
                panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano :=");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[2])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[2])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[2])) {
                if (!czy_real(leksemy[2]) && !czy_integer(leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[2])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[3].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[3].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[3])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[4])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[4])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[4])) {
                if (!czy_real(leksemy[4]) && !czy_integer(leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[4])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[5].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[5].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[5])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[6])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[6])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[6])) {
                if (!czy_real(leksemy[6]) && !czy_integer(leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[6])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (leksemy[7].equals(";")) {
                panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki po sredniku");
                czy_poprawny = false;
            }
            if (!leksemy[7].equals(";")) if (!czy_wyrazenie_arytmetyczne(leksemy[7])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika lub wyrazenia arytmetycznego");
                czy_poprawny = false;
            }
            if (czy_zmienna(leksemy[8])) if (!czy_poprawne_typyzm(leksemy[0], leksemy[8])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                czy_poprawny = false;
            }
            if (!czy_zmienna(leksemy[8])) {
                if (!czy_real(leksemy[8]) && !czy_integer(leksemy[8])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Znaleziono niepoprawne znaki");
                    czy_poprawny = false;
                } else if (!czy_poprawne_typyd(leksemy[0], leksemy[8])) {
                    panel.wypisz_blad("Blad w linijce " + nr + " Wystapilo pomieszanie typow");
                    czy_poprawny = false;
                }
            }
            if (!leksemy[9].equals(";")) if (czy_poprawne_typyzm(leksemy[0], leksemy[8])) {
                panel.wypisz_blad("Blad w linijce " + nr + " Oczekiwano srednika");
                czy_poprawny = false;
            }
            panel.wypisz_blad("Blad w linijce " + nr + " Wystapily niepoprawne znaki po sredniku");
            czy_poprawny = false;
        }
        return czy_poprawny;
    }

    /**
 	Funkcja umozliwia rozpocecie sprawdzania skladni. Zapisjue tekst programu do tablicy leksemow i wywoluje
 	odpowiednie funkcje sluzace do sprawdzania odpowiednich lini.
 */
    public void SprawdzSkladnie(String linijka, int nr) {
        String p = "";
        char z = ' ';
        l = -1;
        for (int i = 0; i < linijka.length(); i++) {
            z = linijka.charAt(i);
            switch(z) {
                case ' ':
                    l++;
                    leksemy[l] = p.toLowerCase();
                    p = "";
                    break;
                case '\n':
                    l++;
                    leksemy[l] = p.toLowerCase();
                    p = "";
                    break;
                case ';':
                    l++;
                    leksemy[l] = p.toLowerCase();
                    p = "";
                    l++;
                    leksemy[l] = ";";
                    i++;
                    break;
                default:
                    p = p + linijka.charAt(i);
                    break;
            }
        }
        if (!p.equals("")) {
            l++;
            leksemy[l] = p;
        }
        nr++;
        if (!bylprogram) {
            if (leksemy[0].equals("program")) {
                l1 = sprawdz_program(nr);
                if (l1) bylprogram = true;
            } else {
                panel.wypisz_blad("Nieprawidlowy poczatek programu");
                l11 = false;
            }
        } else {
            if (leksemy[0].equals("var")) {
                poczvar = true;
            } else if (leksemy[0].equals("begin")) {
                poczvar = false;
                bylvar = true;
                ilbegin++;
            } else if (poczvar) {
                l2 = sprawdz_var(nr);
            } else if (bylvar) {
                if (leksemy[0].equals("if")) {
                    l3 = sprawdz_if(nr);
                } else if (leksemy[0].equals("else")) {
                    l15 = true;
                } else if (leksemy[0].equals("while")) {
                    l4 = sprawdz_while(nr);
                } else if (leksemy[0].equals("for")) {
                    l5 = sprawdz_for(nr);
                } else if (leksemy[0].equals("repeat")) {
                    l6 = sprawdz_repeat(nr);
                } else if ((leksemy[0].equals("write")) || (leksemy[0].equals("writeln"))) {
                    l8 = sprawdz_write(nr);
                } else if ((leksemy[0].equals("read")) || (leksemy[0].equals("readln"))) {
                    l9 = sprawdz_read(nr);
                } else if (leksemy[0].equals("begin")) {
                    ilbegin++;
                } else if (leksemy[0].equals("end")) {
                    if (l == 0) {
                        panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano srednika");
                        l14 = false;
                    }
                    if (l == 2) {
                        if (!leksemy[1].equals(";")) {
                            panel.wypisz_blad("Blad w linijce " + nr + " - Oczekiwano srednika");
                            l14 = false;
                        }
                        panel.wypisz_blad("Blad w linijce " + nr + " - Znaleziono niepoprawne znaki po sredniku");
                        l14 = false;
                    }
                    ilbegin--;
                } else if (leksemy[0].equals("end.")) {
                    bylkend = true;
                } else if (leksemy[0].equals("")) {
                } else {
                    l10 = sprawdz_inne(nr);
                }
            }
        }
    }

    /**
 Konstruktor klasy CompileProgram, uruchamia on sprawdzanie poprawnosc kodu napisanego programu. Rozdziela tekst
 programu na kolejne linijki, zapisuje te linijki do tablicy leksemow. Nastepnie kolejno kazda linijke sprawdza
 czy jest ona poprawna, nastepnie wypisuje odpowiednie bledy na polu konola. Funkcja informuje czy caly program jest
 poprawny, czy nie.	
 */
    public CompileProgram(String str) {
        if (!str.equals("")) {
            String pom = "";
            char zn = ' ';
            int j = -1;
            for (int i = 0; i < str.length(); i++) {
                zn = str.charAt(i);
                if (zn == '\n') {
                    pom = pom + str.charAt(i);
                    j++;
                    linia[j] = pom;
                    pom = "";
                } else {
                    pom = pom + str.charAt(i);
                }
            }
            j++;
            linia[j] = pom;
            pom = "";
            for (int i = 0; i <= j; i++) {
                SprawdzSkladnie(linia[i], i);
            }
            int k = j + 1;
            if (ilbegin != 0) {
                for (int h = 1; h <= ilbegin; h++) panel.wypisz_blad("Blad w linijce " + k + " brakuje slowa end");
                l13 = false;
            }
            if (!bylkend) {
                l12 = false;
                panel.wypisz_blad("Nieprawidlowe zakonczenie programu");
            }
            panel.wypisz_blad("Zakonczono kompilacje");
        }
        if ((l1) && (l2) && (l3) && (l4) && (l5) && (l6) && (l7) && (l8) && (l9) && (l10) && (l11) && (l12) && (l13) && (l14) && (l15)) {
            compileok = true;
        }
    }

    public static void main(String args[]) {
        CompileProgram saf = new CompileProgram("program");
    }
}

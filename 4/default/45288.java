import java.awt.Color;
import java.util.ArrayList;

/**
 * Pasinterpreter - prosty interpreter j�zyka Pascal Copyright (C) 2007
 * Kinga Kosicka
 * 
 * Niniejszy program jest wolnym oprogramowaniem; mo�esz go rozprowadza� dalej
 * i/lub modyfikowa� na warunkach Powszechnej Licencji Publicznej GNU, wydanej
 * przez Fundacj� Wolnego Oprogramowania - wed�ug wersji 2-giej tej Licencji lub
 * kt�rej� z p�niejszych wersji.
 * 
 * Niniejszy program rozpowszechniany jest z nadziej�, i� b�dzie on u�yteczny -
 * jednak BEZ JAKIEJKOLWIEK GWARANCJI, nawet domy�lnej gwarancji PRZYDATNO�CI
 * HANDLOWEJ albo PRZYDATNO�CI DO OKRE�LONYCH ZASTOSOWA�. W celu uzyskania
 * bli�szych informacji - Powszechna Licencja Publiczna GNU.
 * 
 * Z pewno�ci� wraz z niniejszym programem otrzyma�e� te� egzemplarz Powszechnej
 * Licencji Publicznej GNU (GNU General Public License); je�li nie - napisz do
 * Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * 
 */
public class Leksyka {

    private static Pasinterpreter parent;

    public Leksyka(Pasinterpreter parent) {
        this.parent = parent;
    }

    public static boolean spr = false;

    public static boolean oki = false;

    public static final String[] operatory = new String[] { ":=", "=", ">", ">=", "<", "<=", "+", "-", "*", "/", "div", "mod", "<>", "(", ")", ".", ";" };

    public static final String[] slowaKluczowe = new String[] { "begin", "end", "program", "integer", "real", "while", "do", "if", "then", "else", "var", "writeln", "write", "read" };

    public static final String[] operatoryArytmetyczne = new String[] { "+", "-", "*", "/" };

    public static final String[] operatoryLogiczne = new String[] { ">", "<", "==", "<>", ">=", "=<" };

    public static boolean czySlowoKluczowe(String slowo) {
        for (String s : slowaKluczowe) if (s.equalsIgnoreCase(slowo)) return true;
        return false;
    }

    public static boolean czyOperator(String operator) {
        for (String o : operatory) if (o.equals(operator)) return true;
        return false;
    }

    public static boolean czyOperatorArytmetyczny(String s) {
        return (s.equals("+") || s.equals("-") || s.equals("/") || s.equals("*") || s.equals("mod") || s.equals("div"));
    }

    public static boolean czyOperatorLogiczny(String s) {
        return s.equals("==") || s.equals(">") || s.equals("<") || s.equals("<>") || s.equals("<=") || s.equals(">=");
    }

    public static boolean czyTyp(String s) {
        return s.equalsIgnoreCase("string") || s.equalsIgnoreCase("integer") || s.equalsIgnoreCase("real");
    }

    public static boolean czyCalkowita(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean czyRzeczywista(String s) {
        try {
            Float.parseFloat(s);
            if (s.indexOf(".") != -1) return true; else return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean czyString(String s) {
        if (s.length() < 3) return false;
        for (int i = 1; i < s.length() - 1; i++) if (s.charAt(i) == '"') return false;
        return (s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"');
    }

    public static boolean czyZnak(String s) {
        if (s.length() != 3) return false;
        return (s.charAt(0) == '\'' && s.charAt(1) != '\'' && s.charAt(2) == '\'');
    }

    public static boolean czyIdentyfikator(String s) {
        if (czySlowoKluczowe(s) || czyOperator(s) || czyRzeczywista(s) || czyCalkowita(s)) return false;
        for (String str : operatory) if (s.indexOf(str) != -1) return false;
        if (s.equals(";")) return false;
        if (s.length() == 0) return false;
        char c = s.charAt(0);
        if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') return false;
        if (s.indexOf("\'") != -1 || s.indexOf('!') != -1 || s.indexOf('@') != -1 || s.indexOf('$') != -1 || s.indexOf('^') != -1 || s.indexOf('*') != -1 || s.indexOf('(') != -1 || s.indexOf(')') != -1 || s.indexOf('~') != -1 || s.indexOf('#') != -1 || s.indexOf('%') != -1) return false;
        if (s.indexOf("{") != -1 || s.indexOf('}') != -1 || s.indexOf('"') != -1 || s.indexOf('.') != -1 || s.indexOf(',') != -1 || s.indexOf(":") != -1) return false;
        if (s.length() > 1 && s.indexOf(";") != -1) if (s.indexOf(";") != (s.length() - 1)) return false;
        return true;
    }

    public static boolean czyZmienna(String slowo) {
        if (parent.getZmienne().get(slowo) != null) return true;
        return false;
    }

    public static boolean czyInnyIdentyfikator(char c) {
        String temp = String.valueOf(c);
        if (czyOperator(temp)) return false;
        if (c == '\'' || c == '!' || c == '@' || c == '$' || c == '^' || c == '*' || c == '(' || c == ')' || c == '~' || c == '#' || c == '%') return false;
        if (c == '{' || c == '}' || c == '"' || c == '.' || c == ',' || c == ':') return false;
        return true;
    }

    public void sprawdzCudzyslow(String[] str, String line, ArrayList<String> leksemy) throws Exception {
        try {
            if (str[0].equalsIgnoreCase("write") || str[0].equalsIgnoreCase("writeln")) {
                int cudz = 0;
                for (int i = 0; i < line.length(); i++) if (line.charAt(i) == '\"') cudz++;
                if (!(cudz % 2 == 0)) if (cudz != 0) {
                    oki = false;
                    throw new Exception("ERROR: Napis jest niepoprawnie zako�czony");
                }
                if (line.indexOf("\"") != -1) {
                    int poz = line.indexOf("\"");
                    int p = 0;
                    for (int i = 0; i < leksemy.size(); i++) {
                        if (leksemy.get(i).equals("write")) {
                            if (leksemy.get(i + 1).contains("\"")) p = i;
                            parent.wynik.append("" + p);
                        }
                    }
                    String temp = "";
                    while (poz < line.length()) {
                        temp += line.charAt(poz);
                        poz++;
                    }
                }
                int o = 0;
                int z = 0;
                if (line.indexOf("\"") == -1) {
                    for (int i = 0; i < line.length(); i++) {
                        if (line.charAt(i) == '(') o++;
                        if (line.charAt(i) == ')') z++;
                    }
                    if (o != z) throw new Exception("ERROR: Liczba nawias�w otwieraj�cych oraz zamykaj�cych nie zgadza si�");
                    if (o == 0) {
                    }
                }
            }
        } catch (Exception e) {
            parent.wynik.append(e.toString());
        }
    }

    public void analizuj(String linia, ArrayList<String> leksemy) throws Exception {
        try {
            if (linia == null || linia.length() == 0) return;
            if (linia.equalsIgnoreCase("end;")) {
                leksemy.add("end");
                leksemy.add(";");
                return;
            }
            if (linia.indexOf("read") != -1 || linia.indexOf("write") != -1 || linia.indexOf("writeln") != -1) {
                String[] str = linia.split(" ");
                sprawdzCudzyslow(str, linia, leksemy);
                if (str.length == 1 && !(str[0].equalsIgnoreCase("write") || str[0].equalsIgnoreCase("read") || str[0].equalsIgnoreCase("writeln"))) throw new Exception("Error: B��d w sk�adni");
                if (str.length == 1) throw new Exception("Error: Brak agrumentu dla instrukcji " + str[0]);
                if (str[0].equalsIgnoreCase("read")) {
                    for (int i = 1; i < str.length; i++) {
                        if (str[i].indexOf("+") != -1 || str[i].indexOf("-") != -1 || str[i].indexOf("*") != -1 || str[i].indexOf("/") != -1 || str[i].indexOf("mod") != -1 || str[i].indexOf("div") != -1 || str[i].indexOf("=") != -1 || str[i].indexOf(":=") != -1) throw new Exception("Error: Read nie mo�e by� u�ywanie wraz z operatorami");
                        if (!czyOperator(str[i])) if (czyCalkowita(str[i])) throw new Exception("Error: Nieprawid�owy argument dla read");
                        if (czyIdentyfikator(str[i])) {
                            Zmienna z = parent.getZmienne().get(str[i]);
                            if (z == null) throw new Exception("Error: Brak zmiennej " + str[i]);
                        }
                    }
                }
            }
            String[] str = linia.split(" ");
            for (String bycmozeleksem : str) {
                if (czySlowoKluczowe(bycmozeleksem) || czyOperator(bycmozeleksem) || czyIdentyfikator(bycmozeleksem) || czyString(bycmozeleksem) || czyCalkowita(bycmozeleksem) || czyRzeczywista(bycmozeleksem)) {
                    leksemy.add(bycmozeleksem);
                } else {
                    analizujDalej(bycmozeleksem, leksemy);
                }
            }
            oki = true;
        } catch (Exception e) {
            parent.wynik.append(e.toString());
        }
    }

    public void analizujDalej(String slowo, ArrayList<String> lista) throws Exception {
        try {
            if (slowo.indexOf(":") != -1 && slowo.indexOf(":=") == -1) {
                if (zliczSredniki(slowo) == 1) {
                    rozdzielPoDwukropku(slowo, lista);
                } else {
                    String[] t = slowo.split(";");
                    for (String x : t) rozdzielPoDwukropku(x, lista);
                }
                return;
            }
            if (slowo.indexOf(":=") != -1 && slowo.length() > 2) {
                String[] tab = slowo.split(":=");
                if (czyIdentyfikator(tab[0]) && tab[0].length() > 0) {
                    lista.add(tab[0]);
                    lista.add(":=");
                    String temp = "";
                    int i = 0;
                    if (tab.length > 1) {
                        if (tab[1].length() > 0) {
                            while (i < tab[1].length()) {
                                char c = tab[1].charAt(i);
                                temp += c;
                                if (czyOperator(temp)) {
                                    if ((i + 1) < tab[1].length()) {
                                        if (czyOperator(String.valueOf(tab[1].charAt(i + 1))) || czyCalkowita(String.valueOf(tab[1].charAt(i + 1))) || czyRzeczywista(String.valueOf(tab[1].charAt(i + 1))) || czyIdentyfikator(String.valueOf(tab[1].charAt(i + 1)))) {
                                            lista.add(temp);
                                            temp = "";
                                            i++;
                                            continue;
                                        }
                                    }
                                }
                                if (czyIdentyfikator(temp)) {
                                    if ((i + 1) < tab[1].length()) {
                                        if (!czyIdentyfikator(temp + tab[1].charAt(i + 1))) {
                                            lista.add(temp);
                                            temp = "";
                                            i++;
                                            continue;
                                        }
                                    }
                                }
                                if (czyCalkowita(temp)) {
                                    if ((i + 1) < tab[1].length()) {
                                        if (!czyCalkowita(String.valueOf(tab[1].charAt(i + 1)))) {
                                            lista.add(temp);
                                            temp = "";
                                            i++;
                                            continue;
                                        }
                                    }
                                }
                                if (czyRzeczywista(temp)) {
                                    if ((i + 1) < tab[1].length()) {
                                        if (!czyRzeczywista(temp + tab[1].charAt(i + 1))) {
                                            lista.add(temp);
                                            temp = "";
                                            i++;
                                            continue;
                                        }
                                    }
                                }
                                if (czyString(temp)) {
                                    if ((i + 1) < tab[1].length()) {
                                        if (!czyString(temp + tab[1].charAt(i + 1))) {
                                            lista.add(temp);
                                            temp = "";
                                            i++;
                                            continue;
                                        }
                                    }
                                }
                                i++;
                            }
                            if (czyOperator(temp) || czyCalkowita(temp) || czyRzeczywista(temp) || czyString(temp) || czyIdentyfikator(temp) || temp.equals(";")) {
                                lista.add(temp);
                            }
                        }
                    }
                } else {
                    throw new Exception("ERROR: Wyra�enie " + tab[0] + " jest z�ym identyfikatorem");
                }
                return;
            }
            if (slowo.length() >= 2) {
                int i = 0;
                String temp = "";
                while (i < slowo.length()) {
                    char c = slowo.charAt(i);
                    temp += c;
                    if (czyOperator(temp)) {
                        if ((i + 1) < slowo.length()) {
                            if ((czyOperator(String.valueOf(slowo.charAt(i + 1))) && !czyOperator(temp + slowo.charAt(i + 1))) || czyCalkowita(String.valueOf(slowo.charAt(i + 1))) || czyRzeczywista(String.valueOf(slowo.charAt(i + 1))) || czyIdentyfikator(String.valueOf(slowo.charAt(i + 1)))) {
                                lista.add(temp);
                                temp = "";
                                i++;
                                continue;
                            }
                        }
                    }
                    if (czyIdentyfikator(temp)) {
                        if ((i + 1) < slowo.length()) {
                            if (czyCalkowita(String.valueOf(slowo.charAt(i + 1))) || !czyInnyIdentyfikator(slowo.charAt(i + 1))) {
                                lista.add(temp);
                                temp = "";
                                i++;
                                continue;
                            }
                        }
                    }
                    if (czyCalkowita(temp)) {
                        if ((i + 1) < slowo.length()) {
                            if (!czyCalkowita(String.valueOf(slowo.charAt(i + 1)))) {
                                lista.add(temp);
                                temp = "";
                            }
                        }
                    }
                    if (czyRzeczywista(temp)) {
                        if ((i + 1) < slowo.length()) {
                            if (!czyRzeczywista(temp + slowo.charAt(i + 1))) {
                                lista.add(temp);
                                temp = "";
                            }
                        }
                    }
                    i++;
                }
                if (czyOperator(temp) || czyCalkowita(temp) || czyRzeczywista(temp) || czyString(temp) || czyIdentyfikator(temp) || temp.equals(")")) {
                    lista.add(temp);
                }
                if (temp.equals(";")) {
                    lista.add(temp);
                    temp = "";
                }
            }
            spr = true;
        } catch (Exception e) {
            e.printStackTrace();
            parent.wynik.setForeground(Color.RED);
            parent.wynik.append(e.toString());
        }
    }

    private int zliczSredniki(String slowo) {
        int result = 0;
        for (char znak : slowo.toCharArray()) {
            if (znak == ';') result++;
        }
        return result;
    }

    private void rozdzielPoDwukropku(String slowo, ArrayList<String> lista) throws Exception {
        try {
            String[] tab = slowo.split(":");
            if (tab[1] != null && tab[1].length() > 0) {
                if (tab[1].indexOf(";") != -1) tab[1] = tab[1].substring(0, tab[1].indexOf(";"));
                if (!tab[1].equalsIgnoreCase("real") && !tab[1].equalsIgnoreCase("integer") && !tab[1].equalsIgnoreCase("string")) throw new Exception("Error: Niepoprawny typ danych " + tab[1]);
            }
            if (slowo.indexOf(",") != -1) {
                String[] zmienne = tab[0].split(",");
                for (String x : zmienne) {
                    lista.add(x);
                    Zmienna z = new Zmienna();
                    z.setName(x);
                    if (parent.getZmienne().get(x) != null) throw new Exception("Error: zmienna " + x + " mo�e by� zadeklarowana tylko raz");
                    if (tab[1].indexOf(";") == -1) {
                        if (!czyTyp(tab[1])) throw new Exception("Error: Nieznany typ " + tab[1]);
                        z.setType(tab[1]);
                    } else {
                        if (!czyTyp(tab[1].substring(0, tab[1].indexOf(";")))) throw new Exception("Error: Nieznany typ " + tab[1].substring(0, tab[1].indexOf(";")));
                        z.setType(tab[1].substring(0, tab[1].indexOf(";")));
                    }
                    parent.getZmienne().put(x, z);
                }
                lista.add(tab[1]);
                lista.add(";");
            } else {
                if (tab.length > 0) lista.add(tab[0]);
                if (parent.getZmienne().get(tab[0]) != null) throw new Exception("Error: Zmienn� mo�na zadeklarowa� tylko raz :" + tab[0]);
                Zmienna z = new Zmienna();
                z.setName(tab[0]);
                if (tab.length > 1) {
                    if (!czyTyp(tab[1])) throw new Exception("Error: Nieznany typ " + tab[1]);
                    lista.add(tab[1]);
                    z.setType(tab[1]);
                }
                parent.getZmienne().put(tab[0], z);
                lista.add(";");
            }
            parent.setPozycjaOstatniejZmiennej(lista.size() - 1);
        } catch (Exception e) {
            e.printStackTrace();
            parent.wynik.append(e.toString());
        }
    }
}

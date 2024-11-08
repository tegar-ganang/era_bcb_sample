import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scanner {

    private ProjectGUI parent;

    public Scanner(ProjectGUI parent) {
        this.parent = parent;
    }

    public static final String[] keyWords = new String[] { "boolean", "program", "begin", "end", "integer", "real", "while", "do", "var", "if", "then", "else", "read", "write", "writeln", "end." };

    public static final String[] operators = new String[] { "+", "-", "/", "*", "div", ":=", "=", ">", ">=", "<", "<=", "<>", "(", ")", ".", ";" };

    public static boolean isArithemiticalExpression(String s) {
        return (s.equals("+") || s.equals("-") || s.equals("/") || s.equals("*") || s.equals("div"));
    }

    public static boolean isLogicalOperator(String s) {
        return s.equals("=") || s.equals(">") || s.equals("<") || s.equals("<>") || s.equals("<=") || s.equals(">=");
    }

    public static boolean isType(String s) {
        return s.equalsIgnoreCase("char") || s.equalsIgnoreCase("string") || s.equalsIgnoreCase("boolean") || s.equalsIgnoreCase("integer") || s.equalsIgnoreCase("real");
    }

    /**
	 * Czy slowo kluczowe
	 * @param s
	 * @return
	 */
    public static boolean isKeyword(String s) {
        for (String str : keyWords) if (str.equalsIgnoreCase(s)) return true;
        return false;
    }

    public static boolean isBoolean(String s) {
        return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
    }

    /**
	 * Czy identyfikator
	 * @param s
	 * @return
	 */
    public static boolean isIdentifier(String s) {
        if (isKeyword(s) || isOperator(s) || isInteger(s) || isReal(s)) return false;
        for (String str : operators) if (s.indexOf(str) != -1) return false;
        if (s.equals(";")) return false;
        if (s.length() == 0) return false;
        char c = s.charAt(0);
        if (c == '0' || c == '1' || c == '2' || c == '3' || c == '4' || c == '5' || c == '6' || c == '7' || c == '8' || c == '9') return false;
        if (s.indexOf("\'") != -1 || s.indexOf('!') != -1 || s.indexOf('@') != -1 || s.indexOf('$') != -1 || s.indexOf('^') != -1 || s.indexOf('*') != -1 || s.indexOf('(') != -1 || s.indexOf(')') != -1 || s.indexOf('~') != -1 || s.indexOf('#') != -1 || s.indexOf('%') != -1) return false;
        if (s.indexOf("{") != -1 || s.indexOf('}') != -1 || s.indexOf('"') != -1 || s.indexOf('.') != -1 || s.indexOf(',') != -1 || s.indexOf(":") != -1) return false;
        if (s.length() > 1 && s.indexOf(";") != -1) if (s.indexOf(";") != (s.length() - 1)) return false;
        return true;
    }

    /**
	 * Czy operator
	 * @param s
	 * @return
	 */
    public static boolean isOperator(String s) {
        for (String str : operators) if (str.equalsIgnoreCase(s)) return true;
        return false;
    }

    /**
	 * Czy liczba calkowita
	 * @param s
	 * @return
	 */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Czy liczba rzeczywista
	 * @param s
	 * @return
	 */
    public static boolean isReal(String s) {
        try {
            Float.parseFloat(s);
            if (s.indexOf(".") != -1) return true; else return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Czy znak
	 */
    public static boolean isChar(String s) {
        if (s.length() != 3) return false;
        return (s.charAt(0) == '\'' && s.charAt(1) != '\'' && s.charAt(2) == '\'');
    }

    /**
	 * Czy lancuch znakow
	 */
    public static boolean isString(String s) {
        if (s.length() < 3) return false;
        for (int i = 1; i < s.length() - 1; i++) if (s.charAt(i) == '"') return false;
        return (s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"');
    }

    /**
	 * czy komentarz
	 */
    public static boolean isComment(String s) {
        return true;
    }

    /**
	 * Sprawdza czy znak moze nalezec do identyfikatora
	 * @param c
	 * @return
	 */
    public boolean isNextCharIdentifier(char c) {
        String temp = String.valueOf(c);
        if (isOperator(temp)) return false;
        if (c == '\'' || c == '!' || c == '@' || c == '$' || c == '^' || c == '*' || c == '(' || c == ')' || c == '~' || c == '#' || c == '%') return false;
        if (c == '{' || c == '}' || c == '"' || c == '.' || c == ',' || c == ':') return false;
        return true;
    }

    /**
	 * Skanuje jedna linie tekstu w poszukiwaniu lexemow
	 * @param line Linia wejsciowa
	 * @param lexems Lista lexemow
	 */
    public void scan(String line, ArrayList<String> lexems) throws Exception {
        if (line == null || line.length() == 0) return;
        if (line.equalsIgnoreCase("end;")) {
            lexems.add("end");
            lexems.add(";");
            return;
        }
        if (line.indexOf("read") != -1 || line.indexOf("READ") != -1 || line.indexOf("WRITE") != -1 || line.indexOf("WRITELN") != -1 || line.indexOf("write") != -1 || line.indexOf("writeln") != -1) {
            String[] str = line.split(" ");
            if (str[0].equalsIgnoreCase("write") || str[0].equalsIgnoreCase("writeln")) {
                lexems.add(str[0]);
                int cudz = 0;
                for (int i = 0; i < line.length(); i++) if (line.charAt(i) == '\"') cudz++;
                if (!(cudz % 2 == 0)) if (cudz != 0) throw new Exception("Napis jest niepoprawnie zakonczony");
                if (cudz == 0) {
                    if (str.length > 1) {
                        lexems.add(str[1]);
                        return;
                    }
                } else {
                    int poz = line.indexOf("\"");
                    String temp = "";
                    while (poz < line.length()) {
                        temp += line.charAt(poz);
                        ++poz;
                    }
                    lexems.add(temp);
                    return;
                }
                int o = 0;
                int z = 0;
                if (line.indexOf("\"") == -1) {
                    for (int i = 0; i < line.length(); i++) {
                        if (line.charAt(i) == '(') o++;
                        if (line.charAt(i) == ')') z++;
                    }
                    if (o != z) throw new Exception("Liczba nawiasow otwierajacych oraz zamykajacych nie zgadza sie");
                    if (o == 0) {
                    }
                }
            }
            if (str.length == 1 && !(str[0].equalsIgnoreCase("write") || str[0].equalsIgnoreCase("read") || str[0].equalsIgnoreCase("writeln"))) throw new Exception("Blad skladni - read|write|writeln[spacja]argument");
            if (str.length == 1) throw new Exception("Brak agrumentu dla instrukcji " + str[0]);
            if (str[0].equalsIgnoreCase("read")) {
                for (int i = 1; i < str.length; i++) {
                    if (str[i].indexOf("+") != -1 || str[i].indexOf("-") != -1 || str[i].indexOf("*") != -1 || str[i].indexOf("/") != -1 || str[i].indexOf("div") != -1 || str[i].indexOf("=") != -1 || str[i].indexOf(":=") != -1) throw new Exception("Nie mozna uzywac operatorow w instrukcji read");
                    if (!isOperator(str[i])) if (isInteger(str[i])) throw new Exception("Nieprawidlowy argument dla instrukcji read");
                    if (isIdentifier(str[i])) {
                        Variable v = parent.getVariables().get(str[i]);
                        if (v == null) throw new Exception("Brak zmiennej " + str[i]);
                    }
                }
            }
        }
        if (line.indexOf(":=") != -1) {
            String[] lee = line.split(":=");
            if (parent.getVariables().get(lee[0]) != null) {
                String type = parent.getVariables().get(lee[0]).getType();
                if (type.equalsIgnoreCase("string") && lee.length > 1) {
                    lexems.add(lee[0]);
                    lexems.add(":=");
                    int cudz = 0;
                    for (int g = 0; g < lee[1].length(); g++) if (lee[1].charAt(g) == '\"') cudz++;
                    if ((cudz % 2) != 0 && cudz != 0) throw new Exception("Ciag znakow jest niepoprawnie zakonczony");
                    if (cudz == 0) {
                        try {
                            type = parent.getVariables().get(lee[1]).getType();
                            if (!type.equalsIgnoreCase("string") || !type.equalsIgnoreCase("char")) throw new Exception("Nie mozna przypisac typu " + type + " do string");
                        } catch (Exception ex) {
                            throw new Exception("Brak zmiennej " + lee[1]);
                        }
                    }
                    lexems.add(lee[1]);
                    return;
                }
            }
        }
        String[] str = line.split(" ");
        for (String s : str) {
            if (isKeyword(s) || isOperator(s) || isIdentifier(s) || isString(s) || isInteger(s) || isReal(s) || isChar(s)) {
                lexems.add(s);
            } else {
                analize(s, lexems);
            }
        }
    }

    /**
	 * Analizuje fragment tekstu
	 * @param s
	 */
    public void analize(String s, ArrayList<String> a) throws Exception {
        if (s.indexOf(":") != -1 && s.indexOf(":=") == -1) {
            if (ileSrednikow(s) == 1) {
                rozdzielNaDwukropki(s, a);
            } else {
                String[] t = s.split(";");
                for (String x : t) rozdzielNaDwukropki(x, a);
            }
            return;
        }
        if (s.indexOf(":=") != -1 && s.length() > 2) {
            String[] t = s.split(":=");
            if (isIdentifier(t[0]) && t[0].length() > 0) {
                a.add(t[0]);
                a.add(":=");
                String temp = "";
                int i = 0;
                if (t.length > 1) {
                    if (t[1].length() > 0) {
                        while (i < t[1].length()) {
                            char c = t[1].charAt(i);
                            temp += c;
                            if (isOperator(temp)) {
                                if ((i + 1) < t[1].length()) {
                                    if (isOperator(String.valueOf(t[1].charAt(i + 1))) || isInteger(String.valueOf(t[1].charAt(i + 1))) || isReal(String.valueOf(t[1].charAt(i + 1))) || isIdentifier(String.valueOf(t[1].charAt(i + 1)))) {
                                        a.add(temp);
                                        temp = "";
                                        i++;
                                        continue;
                                    }
                                }
                            }
                            if (isIdentifier(temp)) {
                                if ((i + 1) < t[1].length()) {
                                    if (!isIdentifier(temp + t[1].charAt(i + 1))) {
                                        a.add(temp);
                                        temp = "";
                                        i++;
                                        continue;
                                    }
                                }
                            }
                            if (isInteger(temp)) {
                                if ((i + 1) < t[1].length()) {
                                    if (!isInteger(String.valueOf(t[1].charAt(i + 1)))) {
                                        a.add(temp);
                                        temp = "";
                                        i++;
                                        continue;
                                    }
                                }
                            }
                            if (isReal(temp)) {
                                if ((i + 1) < t[1].length()) {
                                    if (!isReal(temp + t[1].charAt(i + 1))) {
                                        a.add(temp);
                                        temp = "";
                                        i++;
                                        continue;
                                    }
                                }
                            }
                            if (isString(temp)) {
                                if ((i + 1) < t[1].length()) {
                                    if (!isString(temp + t[1].charAt(i + 1))) {
                                        a.add(temp);
                                        temp = "";
                                        i++;
                                        continue;
                                    }
                                }
                            }
                            i++;
                        }
                        if (isOperator(temp) || isInteger(temp) || isReal(temp) || isString(temp) || isIdentifier(temp) || temp.equals(";")) {
                            a.add(temp);
                        }
                    }
                }
            } else {
                throw new Exception("Wyrazenie " + t[0] + " nie jest poprawnym identyfikatorem");
            }
            return;
        }
        if (s.length() >= 2) {
            int i = 0;
            String temp = "";
            while (i < s.length()) {
                char c = s.charAt(i);
                temp += c;
                if (isOperator(temp)) {
                    if ((i + 1) < s.length()) {
                        if ((isOperator(String.valueOf(s.charAt(i + 1))) && !isOperator(temp + s.charAt(i + 1))) || isInteger(String.valueOf(s.charAt(i + 1))) || isReal(String.valueOf(s.charAt(i + 1))) || isIdentifier(String.valueOf(s.charAt(i + 1)))) {
                            a.add(temp);
                            temp = "";
                            i++;
                            continue;
                        }
                    }
                }
                if (isIdentifier(temp)) {
                    if ((i + 1) < s.length()) {
                        if (isInteger(String.valueOf(s.charAt(i + 1))) || !isNextCharIdentifier(s.charAt(i + 1))) {
                            a.add(temp);
                            temp = "";
                            i++;
                            continue;
                        }
                    }
                }
                if (isInteger(temp)) {
                    if ((i + 1) < s.length()) {
                        if (!isInteger(String.valueOf(s.charAt(i + 1)))) {
                            a.add(temp);
                            temp = "";
                        }
                    }
                }
                if (isReal(temp)) {
                    if ((i + 1) < s.length()) {
                        if (!isReal(temp + s.charAt(i + 1))) {
                            a.add(temp);
                            temp = "";
                        }
                    }
                }
                i++;
            }
            if (isOperator(temp) || isInteger(temp) || isReal(temp) || isString(temp) || isIdentifier(temp) || temp.equals(")")) {
                a.add(temp);
            }
            if (temp.equals(";")) {
                a.add(temp);
                temp = "";
            }
        }
    }

    private void rozdzielNaDwukropki(String s, ArrayList<String> a) throws Exception {
        String[] t = s.split(":");
        if (t[1] != null && t[1].length() > 0) {
            if (t[1].indexOf(";") != -1) t[1] = t[1].substring(0, t[1].indexOf(";"));
            if (!t[1].equalsIgnoreCase("real") && !t[1].equalsIgnoreCase("integer") && !t[1].equalsIgnoreCase("boolean") && !t[1].equalsIgnoreCase("string") && !t[1].equalsIgnoreCase("char")) throw new Exception("Niepoprawny typ danych " + t[1]);
        }
        if (s.indexOf(",") != -1) {
            String[] vbs = t[0].split(",");
            for (String x : vbs) {
                a.add(x);
                Variable v = new Variable();
                v.setName(x);
                if (parent.getVariables().get(x) != null) throw new Exception("Zmienna " + x + " zostala zadeklarowana dwukrotnie");
                if (t[1].indexOf(";") == -1) {
                    if (!isType(t[1])) throw new Exception("Nieznany typ " + t[1]);
                    v.setType(t[1]);
                } else {
                    if (!isType(t[1].substring(0, t[1].indexOf(";")))) throw new Exception("Nieznany typ " + t[1].substring(0, t[1].indexOf(";")));
                    v.setType(t[1].substring(0, t[1].indexOf(";")));
                }
                parent.getVariables().put(x, v);
            }
            a.add(t[1]);
            a.add(";");
        } else {
            if (t.length > 0) a.add(t[0]);
            if (parent.getVariables().get(t[0]) != null) throw new Exception("Zmienna " + t[0] + " zostala zadeklarowana dwukrotnie");
            Variable v = new Variable();
            v.setName(t[0]);
            if (t.length > 1) {
                if (!isType(t[1])) throw new Exception("Nieznany typ " + t[1]);
                a.add(t[1]);
                v.setType(t[1]);
            }
            parent.getVariables().put(t[0], v);
            a.add(";");
        }
        parent.setPosOfLastVariable(a.size() - 1);
    }

    private int ileSrednikow(String s) {
        int result = 0;
        for (char c : s.toCharArray()) {
            if (c == ';') result++;
        }
        return result;
    }
}

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;

/**
 * * This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
* 
* @author Tonk
*
*/
class Zmienna {

    int intValue;

    float floatValue;

    String stringValue;

    boolean boolValue;

    String type;

    String name;

    int getIntValue() {
        return intValue;
    }

    float getFloatValue() {
        return floatValue;
    }

    String getStringValue() {
        return stringValue;
    }

    boolean getBoolValue() {
        return boolValue;
    }

    String getType() {
        return type;
    }

    String getName() {
        return name;
    }

    String getTransValue() {
        if (type.equalsIgnoreCase("integer")) {
            return intValue + "";
        }
        if (type.equalsIgnoreCase("real")) {
            return floatValue + "";
        }
        if (type.equalsIgnoreCase("boolean")) {
            return boolValue + "";
        }
        if (type.equalsIgnoreCase("string")) {
            return stringValue;
        }
        return "NULL";
    }

    void makeValue(int val) {
        intValue = val;
    }

    void makeValue(float val) {
        floatValue = val;
    }

    void makeValue(String val) {
        stringValue = val;
    }

    void makeValue(boolean val) {
        boolValue = val;
    }

    void makeType(String val) {
        type = val;
        if (type.equalsIgnoreCase("integer")) {
            intValue = 0;
        }
        if (type.equalsIgnoreCase("real")) {
            floatValue = 0;
        }
        if (type.equalsIgnoreCase("boolean")) {
            boolValue = false;
        }
        if (type.equalsIgnoreCase("string")) {
            stringValue = "";
        }
    }

    public Zmienna(String name) {
        this.name = name;
    }
}

public class Interpreter {

    private List<String> lexems;

    int ilosc;

    boolean deklaracje = false;

    boolean blok = false;

    boolean wypisz1 = false, wypisz2 = false;

    boolean czytaj1 = false, czytaj2 = false;

    boolean makeAritmetic = false;

    String firstArg = "";

    String aritmeticLine = "";

    Zmienna[] zmienna = new Zmienna[100];

    int licznik = 0;

    String tempLine = "";

    String[] art = new String[100];

    int inArt = 0;

    BufferedReader read = null;

    int startX = -1;

    int endX = -1;

    public void zamien(int size) {
        for (int i = 0; i < size; i++) {
            if (lexems.get(i).equalsIgnoreCase("div")) {
                lexems.remove(i);
                lexems.add(i, "/");
            }
        }
    }

    public Interpreter(List<String> lexems) {
        this.lexems = lexems;
        ilosc = lexems.size();
        zamien(ilosc);
        read = new BufferedReader(new InputStreamReader(System.in));
    }

    int getZmiennaByName(String key) {
        for (int i = 0; i < licznik; i++) {
            if (key.equalsIgnoreCase(zmienna[i].getName())) {
                return i;
            }
        }
        return -1;
    }

    public void initLine(String line) {
        int start = licznik;
        String tempName = "";
        boolean endNow = false;
        String typ = "";
        for (int k = 1; k < line.length(); k++) {
            if (line.charAt(k) == 44) {
                zmienna[licznik] = new Zmienna(tempName);
                licznik++;
                tempName = "";
            } else {
                tempName = tempName + line.charAt(k);
            }
        }
        for (int i = start; i < licznik; i++) {
            zmienna[i].makeType(tempName);
        }
    }

    public void wypisz() {
        for (int i = 0; i < inArt; i++) {
            System.out.println(art[i]);
        }
    }

    public String makeLine() {
        String answer = "";
        for (int i = 0; i < inArt; i++) {
            boolean operFound = false;
            String temp = art[i];
            if (!temp.equalsIgnoreCase("-")) {
                temp = ujemne(temp);
                art[i] = temp;
            }
        }
        for (int i = 0; i < inArt; i++) {
            answer = answer + art[i];
        }
        return answer;
    }

    public boolean checkEnd(String line) {
        int nrAtr1 = getZmiennaByName(line);
        if (nrAtr1 != -1) {
            line = zmienna[nrAtr1].getTransValue();
        }
        boolean answer = false;
        try {
            int val = Integer.parseInt(line);
            answer = true;
        } catch (NumberFormatException e) {
        }
        if (!answer) {
            try {
                float val = Float.parseFloat(line);
                answer = true;
            } catch (NumberFormatException e) {
            }
        }
        return answer;
    }

    public String findInternal(String line) {
        String answer = "";
        startX = -1;
        endX = -1;
        boolean found = false;
        for (int i = 0; i < line.length(); i++) {
            String lex = line.charAt(i) + "";
            if (lex.equals("(")) {
                found = true;
                answer = "";
                startX = i;
            } else if (lex.equals(")")) {
                if (found) {
                    answer = answer + lex;
                    endX = i;
                }
                found = false;
            }
            if (found) {
                answer = answer + lex;
            }
        }
        if (answer.length() >= 1) {
            answer = answer.substring(1, answer.length() - 1);
        }
        return answer;
    }

    public String countInternal(String line, String type) {
        String tempArt = findInternal(line);
        String temp = "";
        if (tempArt.length() > 0) {
            String wynik = countAritmetic(tempArt, type);
            if ((startX != -1) && (endX != -1)) {
                temp = line.substring(0, startX) + wynik + line.substring(endX + 1, line.length());
            }
        } else {
            return line;
        }
        return temp;
    }

    public String countAritmetic(String line, String type) {
        boolean koniec = false;
        int kl = 0;
        while (!koniec) {
            String aa = makeLine();
            podzielNaNawiasy(line);
            aa = makeLine();
            oblicz(type);
            aa = makeLine();
            line = makeLine();
            koniec = checkEnd(line);
            if (koniec) {
                break;
            }
            aa = makeLine();
            podzielNaAryt(line);
            aa = makeLine();
            oblicz(type);
            aa = makeLine();
            usunNawiasyLiczby();
            line = makeLine();
            koniec = checkEnd(line);
            if (koniec) {
                break;
            }
            aa = makeLine();
            podzielNaMaleNawiasy(line);
            aa = makeLine();
            oblicz(type);
            aa = makeLine();
            usunNawiasyLiczby();
            aa = makeLine();
            oblicz(type);
            aa = makeLine();
            line = makeLine();
            podzielNaMaleNawiasy(line);
            aa = makeLine();
            oblicz(type);
            aa = makeLine();
            usunNawiasyLiczby();
            line = makeLine();
            koniec = checkEnd(line);
            if (koniec) {
                break;
            }
            if ((!szukaj("*")) && (!szukaj("/"))) {
                line = makeLine();
                podzielNaAryt(line);
                while (usunElementZnak("(")) {
                }
                ;
                while (usunElementZnak(")")) {
                }
                ;
                line = makeLine();
                makeDzialania(line);
                aa = makeLine();
                oblicz(type);
                line = makeLine();
            } else {
                line = makeLine();
                String internal = countInternal(line, type);
                if (internal.length() > 0) {
                    makeDzialaniaM(internal);
                }
                makeDzialaniaM(makeLine());
                aa = makeLine();
                oblicz(type);
                line = makeLine();
            }
            if (kl > 1000) {
                System.out.println("$$$" + makeLine());
                System.out.println("Abnormal program terminated.");
                break;
            }
            kl++;
        }
        kl = 0;
        String wynik = makeLine();
        return wynik;
    }

    public void makeDzialaniaM(String line) {
        podzielNaMno(line);
        String temp = "";
        boolean dziel = false;
        String wynik = "";
        for (int i = 0; i < inArt; i++) {
            if (art[i].equals("*")) {
                if (dziel) {
                    wynik = wynik + temp + ";";
                    temp = "";
                    dziel = false;
                } else if (!dziel) {
                    dziel = true;
                }
            }
            if (art[i].equals("/")) {
                if (dziel) {
                    wynik = wynik + temp + ";";
                    temp = "";
                    dziel = false;
                } else if (!dziel) {
                    dziel = true;
                }
            }
            temp = temp + art[i];
        }
        wynik = wynik + temp + ";";
        podzielNaAryt(wynik);
        wynik = makeLine();
        inArt = 0;
        String temp1 = "";
        String wynik2 = "";
        for (int i = 0; i < wynik.length(); i++) {
            if (((wynik.charAt(i) + "").equals("+")) || ((wynik.charAt(i) + "").equals("-"))) {
                wynik2 = wynik2 + wynik.charAt(i) + ";";
            } else {
                wynik2 = wynik2 + wynik.charAt(i);
            }
        }
        for (int i = 0; i < wynik2.length(); i++) {
            if ((wynik2.charAt(i) + "").equals(";")) {
                art[inArt] = temp1;
                temp1 = "";
                inArt++;
            } else {
                temp1 = temp1 + wynik2.charAt(i);
            }
        }
    }

    public void makeDzialania(String line) {
        podzielNaAryt(line);
        String temp = "";
        boolean dziel = false;
        String wynik = "";
        for (int i = 0; i < inArt; i++) {
            if (art[i].equals("+")) {
                if (dziel) {
                    wynik = wynik + temp + ";";
                    temp = "";
                    dziel = false;
                } else if (!dziel) {
                    dziel = true;
                }
            }
            if (art[i].equals("-")) {
                if (dziel) {
                    wynik = wynik + temp + ";";
                    temp = "";
                    dziel = false;
                } else if (!dziel) {
                    dziel = true;
                }
            }
            temp = temp + art[i];
        }
        wynik = wynik + temp + ";";
        inArt = 0;
        String temp1 = "";
        for (int i = 0; i < wynik.length(); i++) {
            if ((wynik.charAt(i) + "").equals(";")) {
                art[inArt] = temp1;
                temp1 = "";
                inArt++;
            } else {
                temp1 = temp1 + wynik.charAt(i);
            }
        }
    }

    public boolean podzielNaNawiasy(String line) {
        boolean answer = false;
        inArt = 0;
        int len = line.length();
        String temp = "";
        int countNawiasOpen = 0;
        int countNawiasClose = 0;
        for (int i = 0; i < len; i++) {
            if (line.charAt(i) == 40) {
                countNawiasOpen++;
            }
            if (line.charAt(i) == 41) {
                countNawiasClose++;
            }
            temp = temp + line.charAt(i);
            if (countNawiasOpen == countNawiasClose) {
                art[inArt] = temp;
                temp = "";
                inArt++;
                answer = true;
                countNawiasOpen = 0;
                countNawiasClose = 0;
            }
        }
        return answer;
    }

    public boolean szukaj(String znak) {
        for (int i = 0; i < inArt; i++) {
            int len = art[i].length();
            for (int k = 0; k < len; k++) {
                String z = art[i].charAt(k) + "";
                if (znak.equals(z)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean podzielNaAryt(String line) {
        boolean answer = false;
        inArt = 0;
        int len = line.length();
        String temp = "";
        boolean dod = false;
        for (int i = 0; i < len; i++) {
            dod = false;
            boolean make = false;
            if (line.charAt(i) == 43) {
                temp = temp;
                dod = true;
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
                art[inArt] = line.charAt(i) + "";
                inArt++;
            }
            if (isAryt(line.charAt(i) + "")) {
                int k = i;
                if (k == 0) {
                    if ((line.charAt(k) + "").equals("-")) {
                        make = true;
                    }
                }
                if (k == 0) {
                    if ((line.charAt(k) + "").equals("(")) {
                        make = true;
                    }
                }
                if (k >= 1) {
                    if (isAryt(line.charAt(k - 1) + "")) {
                        make = true;
                    }
                    if ((line.charAt(k - 1) + "").equals("(")) {
                        make = true;
                    }
                }
            }
            if (!make) {
                if (line.charAt(i) == 45) {
                    temp = temp;
                    dod = true;
                    if (temp.length() > 0) {
                        art[inArt] = temp;
                        temp = "";
                        inArt++;
                        answer = true;
                    }
                    art[inArt] = line.charAt(i) + "";
                    inArt++;
                }
            }
            if (line.charAt(i) == 40) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
            }
            if (line.charAt(i) == 41) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
            }
            if (!dod) {
                temp = temp + line.charAt(i);
            }
        }
        if (temp.length() > 0) {
            art[inArt] = temp;
            temp = "";
            inArt++;
        }
        return answer;
    }

    public boolean podzielNaMno(String line) {
        boolean answer = false;
        inArt = 0;
        int len = line.length();
        String temp = "";
        boolean dod = false;
        for (int i = 0; i < len; i++) {
            dod = false;
            if (line.charAt(i) == 42) {
                temp = temp;
                dod = true;
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
                art[inArt] = line.charAt(i) + "";
                inArt++;
            }
            if (line.charAt(i) == 47) {
                temp = temp;
                dod = true;
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
                art[inArt] = line.charAt(i) + "";
                inArt++;
            }
            if (line.charAt(i) == 40) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
            }
            if (line.charAt(i) == 41) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    answer = true;
                }
            }
            if (!dod) {
                temp = temp + line.charAt(i);
            }
        }
        if (temp.length() > 0) {
            art[inArt] = temp;
            temp = "";
            inArt++;
        }
        return answer;
    }

    public boolean usunNawiasyLiczby() {
        boolean answer = false;
        int intVal = 0;
        float intVal2 = 0;
        boolean intC = false;
        boolean intF = false;
        String line = makeLine();
        podzielNaWszystkieNawiasy(line);
        for (int i = 0; i < inArt; i++) {
            intC = false;
            intF = false;
            try {
                intVal = Integer.parseInt(art[i]);
                intF = true;
            } catch (NumberFormatException e) {
                intF = false;
            }
            if (!intF) {
                try {
                    intVal2 = Float.parseFloat(art[i]);
                    intC = true;
                } catch (NumberFormatException e) {
                    intC = false;
                }
            }
            if ((intF) || (intC)) {
                try {
                    if ((art[i - 1].equals("(")) && (art[i + 1].equals(")"))) {
                        usunElement(i + 1);
                        usunElement(i - 1);
                    }
                } catch (Exception e) {
                }
            }
        }
        return true;
    }

    public boolean usunElementZnak(String key) {
        boolean zmien = false;
        String line1 = makeLine();
        podzielNaWszystkieNawiasy(line1);
        for (int i = 0; i < inArt; i++) {
            if (art[i].equalsIgnoreCase(key)) {
                zmien = true;
            }
            if (zmien) {
                art[i] = art[i + 1];
            }
        }
        if (zmien) {
            inArt--;
        }
        return zmien;
    }

    public void usunElement(int nr) {
        boolean zmien = false;
        for (int i = 0; i < inArt; i++) {
            if (i == nr) {
                zmien = true;
            }
            if (zmien) {
                art[i] = art[i + 1];
            }
        }
        if (zmien) {
            inArt--;
        }
    }

    public boolean podzielNaWszystkieNawiasy(String line) {
        boolean answer = false;
        inArt = 0;
        int len = line.length();
        String temp = "";
        boolean dod = false;
        for (int i = 0; i < len; i++) {
            dod = false;
            if (line.charAt(i) == 40) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    inArt++;
                }
                temp = "";
                art[inArt] = line.charAt(i) + "";
                inArt++;
                dod = true;
            }
            if (line.charAt(i) == 41) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    inArt++;
                }
                temp = "";
                art[inArt] = line.charAt(i) + "";
                inArt++;
                dod = true;
            }
            if (!dod) {
                temp = temp + line.charAt(i);
            }
        }
        if (temp.length() > 0) {
            art[inArt] = temp;
            temp = "";
            inArt++;
        }
        return answer;
    }

    public boolean podzielNaMaleNawiasy(String line) {
        boolean answer = false;
        inArt = 0;
        int len = line.length();
        String temp = "";
        boolean dod = false;
        for (int i = 0; i < len; i++) {
            dod = false;
            if (line.charAt(i) == 40) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    art[inArt] = "(";
                    inArt++;
                    answer = true;
                    dod = true;
                }
            }
            if (line.charAt(i) == 41) {
                if (temp.length() > 0) {
                    art[inArt] = temp;
                    temp = "";
                    inArt++;
                    art[inArt] = ")";
                    inArt++;
                    answer = true;
                    dod = true;
                }
            }
            if (!dod) {
                temp = temp + line.charAt(i);
            }
        }
        if (temp.length() > 0) {
            art[inArt] = temp;
            temp = "";
            inArt++;
        }
        return answer;
    }

    public void obetnijZewnetrzne() {
        for (int i = 0; i < inArt; i++) {
            int len = art[i].length();
            if ((art[i].charAt(0) == 40) && (art[i].charAt(len - 1) == 41)) {
                art[i] = art[i].substring(1, len - 1);
            }
        }
    }

    public String obetnijZewnetrzne2(String line) {
        String answer = line;
        int len = line.length();
        if ((line.charAt(0) == 40) && (line.charAt(len - 1) == 41)) {
            answer = line.substring(1, len - 1);
        }
        return answer;
    }

    public boolean isAryt(String key) {
        if (key.equals("*")) {
            return true;
        }
        if (key.equals("-")) {
            return true;
        }
        if (key.equals("/")) {
            return true;
        }
        if (key.equals("+")) {
            return true;
        }
        return false;
    }

    public String ujemne(String line) {
        boolean formA = true;
        int val = 0;
        String endL = "";
        for (int i = 0; i < line.length(); i++) {
            if ((line.charAt(i) + "").equals("-")) {
                val++;
            } else {
                endL = endL + line.charAt(i);
            }
        }
        if (val == 0) {
            endL = line;
        }
        if ((val % 2) == 0) {
            endL = endL;
        }
        if ((val % 2) == 1) {
            endL = "-" + endL;
        }
        return endL;
    }

    public String licz(String art1, String art2, String oper, String type) {
        int nrAtr1 = getZmiennaByName(art1);
        int art1Val = 0;
        float art1ValF = 0;
        int nrAtr2 = getZmiennaByName(art2);
        int art2Val = 0;
        float art2ValF = 0;
        boolean error = false;
        boolean intC = false;
        boolean floatC = false;
        String wynik = "";
        boolean arg11 = false, arg12 = false, arg21 = false, arg22 = false;
        if (nrAtr1 == -1) {
            if (type.equalsIgnoreCase("Integer")) {
                try {
                    if (type.equalsIgnoreCase("Integer")) {
                        art1Val = Integer.parseInt(art1);
                        intC = true;
                        arg11 = true;
                    }
                } catch (NumberFormatException we) {
                    error = true;
                }
            }
            if (type.equalsIgnoreCase("Real")) {
                try {
                    if (type.equalsIgnoreCase("Real")) {
                        art1ValF = Float.parseFloat(art1);
                        arg12 = true;
                        floatC = true;
                    }
                } catch (NumberFormatException w) {
                    error = true;
                }
            }
        } else {
            if (zmienna[nrAtr1].getType().equalsIgnoreCase("integer")) {
                art1Val = zmienna[nrAtr1].getIntValue();
                intC = true;
                arg11 = true;
            }
            if (zmienna[nrAtr1].getType().equalsIgnoreCase("real")) {
                art1ValF = zmienna[nrAtr1].getFloatValue();
                floatC = true;
                arg12 = true;
            }
        }
        if (nrAtr2 == -1) {
            if (type.equalsIgnoreCase("Integer")) {
                try {
                    if (type.equalsIgnoreCase("Integer")) {
                        art2Val = Integer.parseInt(art2);
                        arg21 = true;
                        intC = true;
                    }
                } catch (NumberFormatException ew) {
                    error = true;
                }
            }
            if (type.equalsIgnoreCase("Real")) {
                try {
                    if (type.equalsIgnoreCase("Real")) {
                        art2ValF = Float.parseFloat(art2);
                        arg22 = true;
                        floatC = true;
                    }
                } catch (NumberFormatException we) {
                    error = true;
                }
            }
        } else {
            if (zmienna[nrAtr2].getType().equalsIgnoreCase("integer")) {
                art2Val = zmienna[nrAtr2].getIntValue();
                intC = true;
                arg21 = true;
            }
            if (zmienna[nrAtr2].getType().equalsIgnoreCase("real")) {
                art2ValF = zmienna[nrAtr2].getFloatValue();
                floatC = true;
                arg22 = true;
            }
        }
        if ((intC) && (floatC)) {
            intC = false;
            if (arg11) {
                art1ValF = Float.parseFloat(art1Val + "");
            }
            ;
            if (arg21) {
                art2ValF = Float.parseFloat(art2Val + "");
            }
            ;
        }
        if (!error) {
            try {
                if (oper.length() > 0) {
                    if (intC) {
                        if (oper.equalsIgnoreCase("+")) {
                            int tem = art1Val + art2Val;
                            wynik = tem + "";
                        }
                        if (oper.equalsIgnoreCase("-")) {
                            int tem = art1Val - art2Val;
                            wynik = tem + "";
                        }
                        if (oper.equalsIgnoreCase("*")) {
                            int tem = art1Val * art2Val;
                            wynik = tem + "";
                        }
                        if (oper.equalsIgnoreCase("/")) {
                            int tem = art1Val / art2Val;
                            wynik = tem + "";
                        }
                    } else if (floatC) {
                        if (oper.equalsIgnoreCase("+")) {
                            float tem = art1ValF + art2ValF;
                            wynik = tem + "";
                        }
                        if (oper.equalsIgnoreCase("-")) {
                            float tem = art1ValF - art2ValF;
                            wynik = tem + "";
                        }
                        if (oper.equalsIgnoreCase("*")) {
                            float tem = art1ValF * art2ValF;
                            wynik = tem + "";
                        }
                        if (oper.equalsIgnoreCase("/")) {
                            float tem = art1ValF / art2ValF;
                            wynik = tem + "";
                        }
                    }
                }
            } catch (ArithmeticException e) {
                System.out.println("Division By Zero");
            }
        }
        if (error) {
            if (oper.length() == 0) {
                int nrk = getZmiennaByName(art1);
                if (nrk != -1) {
                    wynik = zmienna[nrk].getTransValue();
                }
            }
        }
        return wynik;
    }

    public String oblicz(String type) {
        for (int i = 0; i < inArt; i++) {
            boolean operFound = false;
            String temp = art[i];
            if (!temp.equalsIgnoreCase("-")) {
                temp = ujemne(temp);
            }
            int len1 = temp.length();
            if (len1 > 0) {
                if ((temp.charAt(0) == 40) && (temp.charAt(len1 - 1) == 41)) {
                    temp = temp.substring(1, len1 - 1);
                }
            }
            String arg1 = "";
            String arg2 = "";
            String oper = "";
            int len = temp.length();
            boolean olej = false;
            for (int k = 0; k < len; k++) {
                if (operFound) {
                    arg2 = arg2 + temp.charAt(k);
                }
                if ((isAryt(temp.charAt(k) + "")) && (k != 0)) {
                    if (k == 0) {
                        if ((temp.charAt(k) + "").equals("-")) {
                            olej = true;
                        }
                    }
                    if (k >= 1) {
                        if (isAryt(temp.charAt(k - 1) + "")) {
                            olej = true;
                        }
                        if ((temp.charAt(k - 1) + "").equals("(")) {
                            olej = true;
                        }
                    }
                    if (!olej) {
                        operFound = true;
                        oper = temp.charAt(k) + "";
                    }
                    olej = false;
                }
                if (!operFound) {
                    arg1 = arg1 + temp.charAt(k);
                }
            }
            String wynik = licz(arg1, arg2, oper, type);
            if (wynik.length() > 0) {
                art[i] = wynik;
            }
            oper = "";
            arg1 = "";
            arg2 = "";
        }
        return "aaa";
    }

    public void executeMath(String line, String arg) {
        int nr = getZmiennaByName(arg);
        if (nr >= 0) {
            String type = zmienna[nr].getType();
            if (type.equalsIgnoreCase("String")) {
                int nrT = getZmiennaByName(line.substring(0, line.length() - 1));
                if (nrT == -1) {
                    zmienna[nr].makeValue(line.substring(1, line.length() - 1));
                } else {
                    zmienna[nr].makeValue(zmienna[nrT].getStringValue());
                }
            } else if (type.equalsIgnoreCase("boolean")) {
                int nrT = getZmiennaByName(line.substring(0, line.length() - 1));
                if (nrT == -1) {
                    if (line.substring(0, line.length() - 1).equalsIgnoreCase("true")) {
                        zmienna[nr].makeValue(true);
                    } else {
                        zmienna[nr].makeValue(false);
                    }
                } else {
                    zmienna[nr].makeValue(zmienna[nrT].getStringValue());
                }
            } else {
                String wynik = countAritmetic(line, type);
                int intValue = 0;
                float floatValue = 0;
                if (type.equalsIgnoreCase("integer")) {
                    try {
                        intValue = Integer.parseInt(wynik);
                        zmienna[nr].makeValue(intValue);
                    } catch (NumberFormatException e) {
                        intValue = 0;
                    }
                }
                if (type.equalsIgnoreCase("real")) {
                    try {
                        floatValue = Float.parseFloat(wynik);
                        zmienna[nr].makeValue(floatValue);
                    } catch (NumberFormatException e) {
                        floatValue = 0;
                    }
                }
            }
        }
    }

    public boolean isOper(String ch) {
        boolean answer = false;
        if (ch.equalsIgnoreCase(">")) {
            answer = true;
        }
        if (ch.equalsIgnoreCase("<")) {
            answer = true;
        }
        if (ch.equalsIgnoreCase("=")) {
            answer = true;
        }
        return answer;
    }

    public boolean ifwar(String line, String type) {
        boolean answer = false;
        String leftArt = "";
        String rightArt = "";
        String oper = "";
        boolean operfound = false;
        for (int i = 0; i < line.length(); i++) {
            if (isOper(line.charAt(i) + "")) {
                oper = oper + line.charAt(i);
                operfound = true;
            }
            if ((!isOper(line.charAt(i) + "")) && (operfound)) {
                rightArt = rightArt + line.charAt(i);
            }
            if ((!isOper(line.charAt(i) + "")) && (!operfound)) {
                leftArt = leftArt + line.charAt(i);
            }
        }
        executeMath(leftArt, "14ha");
        Float leftSide = zmienna[getZmiennaByName("14ha")].getFloatValue();
        executeMath(rightArt, "14ha");
        Float rightSide = zmienna[getZmiennaByName("14ha")].getFloatValue();
        if (oper.equals(">")) {
            if (leftSide > rightSide) {
                answer = true;
            }
        }
        if (oper.equals("<")) {
            if (leftSide < rightSide) {
                answer = true;
            }
        }
        if (oper.equals("=")) {
            if ((leftSide - rightSide) == 0) {
                answer = true;
            }
        }
        if (oper.equals(">=")) {
            if (leftSide >= rightSide) {
                answer = true;
            }
        }
        if (oper.equals("<=")) {
            if (leftSide <= rightSide) {
                answer = true;
            }
        }
        if (oper.equals("<>")) {
            if ((leftSide - rightSide) != 0) {
                answer = true;
            }
        }
        return answer;
    }

    public void go() {
        boolean ifblock = false;
        String warunekif = "";
        int lastOne = -1;
        boolean inIfBlock = false;
        boolean whileBlock = false;
        boolean inWhileBlock = false;
        boolean zostaw = false;
        int whileIndex = -1;
        for (int i = 0; i < ilosc; i++) {
            String lexem = lexems.get(i);
            if (lexem.equalsIgnoreCase("VAR")) {
                deklaracje = true;
            }
            if (lexem.equalsIgnoreCase("BEGIN")) {
                deklaracje = false;
                blok = true;
            }
            if (deklaracje) {
                if (!lexem.equalsIgnoreCase("var")) {
                    if (lexem.equalsIgnoreCase(";")) {
                        initLine(tempLine);
                        tempLine = "";
                    } else {
                        tempLine = tempLine + "," + lexem;
                    }
                }
            } else {
                if (!zostaw) {
                    if (lexem.equalsIgnoreCase("end")) {
                        if ((inWhileBlock) && (!inIfBlock)) {
                            i = whileIndex - 1;
                            whileIndex = -1;
                            lexem = "";
                            inWhileBlock = false;
                            whileBlock = false;
                            continue;
                        }
                        if ((inIfBlock) && (!inWhileBlock)) {
                            inIfBlock = false;
                            ifblock = false;
                        }
                    }
                }
                if (zostaw) {
                    if (lexem.equalsIgnoreCase("end")) {
                        zostaw = false;
                    } else {
                    }
                }
                if (lastOne == -1) {
                    lastOne = licznik;
                    zmienna[licznik] = new Zmienna("14ha");
                    zmienna[licznik].makeType("REAL");
                    licznik++;
                }
                if (!zostaw) {
                    if ((ifblock) && (lexem.equalsIgnoreCase("end"))) {
                        ifblock = false;
                    }
                    if ((ifblock) && (lexem.equalsIgnoreCase("then"))) {
                        ifblock = false;
                        if (ifwar(warunekif, zmienna[lastOne].getType())) {
                            inIfBlock = true;
                        } else {
                            zostaw = true;
                        }
                        warunekif = "";
                    }
                    if (ifblock) {
                        warunekif = warunekif + lexem;
                    }
                    if ((whileBlock) && (lexem.equalsIgnoreCase("end"))) {
                        whileBlock = false;
                    }
                    if ((whileBlock) && (lexem.equalsIgnoreCase("do"))) {
                        whileBlock = false;
                        if (ifwar(warunekif, zmienna[lastOne].getType())) {
                            inWhileBlock = true;
                        } else {
                            zostaw = true;
                        }
                        warunekif = "";
                    }
                    if (whileBlock) {
                        warunekif = warunekif + lexem;
                    }
                    if ((wypisz1) || (wypisz2)) {
                        int nr = getZmiennaByName(lexem);
                        if (nr >= 0) {
                            if (wypisz1) {
                                System.out.print(zmienna[nr].getTransValue());
                            }
                            if (wypisz2) {
                                System.out.println(zmienna[nr].getTransValue());
                            }
                        } else {
                            if (wypisz1) {
                                System.out.print(lexem.substring(1, lexem.length() - 1));
                            }
                            if (wypisz2) {
                                System.out.println(lexem.substring(1, lexem.length() - 1));
                            }
                        }
                        wypisz1 = false;
                        wypisz2 = false;
                    }
                    if ((czytaj1) || (czytaj2)) {
                        try {
                            int nr = getZmiennaByName(lexem);
                            if (nr >= 0) {
                                if ((czytaj1) || (czytaj2)) {
                                    if (zmienna[nr].getType().equalsIgnoreCase("INTEGER")) {
                                        try {
                                            int temp = Integer.parseInt(read.readLine());
                                            zmienna[nr].makeValue(temp);
                                        } catch (NumberFormatException e) {
                                            System.out.println("Invalid Number");
                                            break;
                                        }
                                    } else if (zmienna[nr].getType().equalsIgnoreCase("REAL")) {
                                        try {
                                            float temp = Float.parseFloat(read.readLine());
                                            zmienna[nr].makeValue(temp);
                                        } catch (NumberFormatException e) {
                                            System.out.println("Invalid Number");
                                            break;
                                        }
                                    } else if (zmienna[nr].getType().equalsIgnoreCase("BooLEAN")) {
                                        String temp = read.readLine();
                                        if (temp.equalsIgnoreCase("true")) {
                                            zmienna[nr].makeValue(true);
                                        } else {
                                            zmienna[nr].makeValue(false);
                                        }
                                    } else {
                                        String temp = read.readLine();
                                        zmienna[nr].makeValue(temp);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            System.out.println("Runtime error: Input operation failed");
                        }
                        czytaj1 = false;
                        czytaj2 = false;
                    }
                    if (makeAritmetic) {
                        if (!lexem.equals(";")) {
                            aritmeticLine = aritmeticLine + lexem;
                        } else {
                            executeMath(aritmeticLine, firstArg);
                            aritmeticLine = "";
                            firstArg = "";
                            makeAritmetic = false;
                        }
                    }
                    if (lexem.equalsIgnoreCase("IF")) {
                        ifblock = true;
                    }
                    if (lexem.equalsIgnoreCase("WHILE")) {
                        whileBlock = true;
                        whileIndex = i;
                    } else if (lexem.equalsIgnoreCase(":=")) {
                        firstArg = lexems.get(i - 1);
                        makeAritmetic = true;
                    } else if (lexem.equalsIgnoreCase("READ")) {
                        czytaj1 = true;
                    } else if (lexem.equalsIgnoreCase("READLN")) {
                        czytaj2 = true;
                    } else if (lexem.equalsIgnoreCase("WRITE")) {
                        wypisz1 = true;
                    } else if (lexem.equalsIgnoreCase("WRITELN")) {
                        wypisz2 = true;
                    }
                }
            }
        }
        try {
            System.out.println("");
            System.out.println("Koniec wykonania programu.");
            read.readLine();
        } catch (IOException e) {
        }
    }
}

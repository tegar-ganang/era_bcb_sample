package Strings.Letters;

import java.util.ArrayList;

/**
 * Program counting the letter from input strings. Encode line. Out top 5 the
 * letters in line.
 * 
 * @author Demonic
 * @version $Rev: $
 */
public class CountingLetters {

    char[] abcEng = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    int[] engIndexLetters = new int[5];

    int[] engCountLetters = new int[abcEng.length];

    public void workingLine(String line) {
        for (int i = 0; i < line.length(); i++) {
            for (int j = 0; j < abcEng.length; j++) {
                if (abcEng[j] == line.charAt(i)) {
                    engCountLetters[j]++;
                }
            }
        }
    }

    public ArrayList<String> showTopLetters() {
        int[] tempArray = new int[engCountLetters.length];
        char[] tempArrayLetters = new char[abcEng.length];
        ArrayList<String> resultTopFiveLetters = new ArrayList<String>();
        tempArray = engCountLetters.clone();
        tempArrayLetters = abcEng.clone();
        int tempCount;
        char tempLetters;
        for (int j = 0; j < (abcEng.length * abcEng.length); j++) {
            for (int i = 0; i < abcEng.length - 1; i++) {
                if (tempArray[i] > tempArray[i + 1]) {
                    tempCount = tempArray[i];
                    tempLetters = tempArrayLetters[i];
                    tempArray[i] = tempArray[i + 1];
                    tempArrayLetters[i] = tempArrayLetters[i + 1];
                    tempArray[i + 1] = tempCount;
                    tempArrayLetters[i + 1] = tempLetters;
                }
            }
        }
        for (int i = tempArrayLetters.length - 1; i > tempArrayLetters.length - 6; i--) {
            resultTopFiveLetters.add(tempArrayLetters[i] + ":" + tempArray[i]);
        }
        return resultTopFiveLetters;
    }

    public String encrypt(String line) {
        String encryptLine = "";
        int tempSymbol;
        byte[] arrayCrypt = new byte[line.length()];
        arrayCrypt = line.getBytes();
        for (int i = 0; i < arrayCrypt.length; i++) {
            tempSymbol = ((arrayCrypt[i] + 256) % 256);
            encryptLine += Integer.toString(tempSymbol);
            encryptLine += " ";
        }
        return encryptLine;
    }

    public String dectypt(String line) {
        String result = "";
        String temp = "";
        int tempNumber;
        char resultLine = 0;
        byte[] tempSymols = new byte[line.length()];
        for (int i = 0, j = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') {
                temp += line.charAt(i);
            } else {
                tempNumber = Integer.parseInt(temp);
                resultLine = (char) tempNumber;
                result += resultLine;
                j++;
                temp = "";
            }
        }
        return result;
    }
}

package freestyleLearningGroup.independent.util;

import java.util.Vector;

public class FLGUtilities {

    /**
     *   This methods performs lexicographically sorting of a given String array
     *   in ascending or descending order. Return value is a new String array
     *   containing the original Strings in sorted manner.
     *   @param <code>unsortedString</code>: String array to sort
     *   @param <code>ascending</code>: <code>true</code> for ascending order, 
                                        <code>false</code> for descending order
     *   @return new sorted <code>String[]<code>
     */
    public static String[] bubbleSort(String[] unsortedString, boolean ascending) {
        if (unsortedString.length < 2) return unsortedString;
        String[] sortedString = new String[unsortedString.length];
        for (int i = 0; i < unsortedString.length; i++) {
            sortedString[i] = unsortedString[i];
        }
        if (ascending) {
            for (int i = 0; i < sortedString.length - 1; i++) {
                for (int j = 1; j < sortedString.length - 1 - i; j++) if (sortedString[j + 1].compareToIgnoreCase(sortedString[j]) < 0) {
                    String swap = sortedString[j];
                    sortedString[j] = sortedString[j + 1];
                    sortedString[j + 1] = swap;
                }
            }
        } else {
            for (int i = sortedString.length - 2; i >= 0; i--) {
                for (int j = sortedString.length - 2 - i; j >= 0; j--) if (sortedString[j + 1].compareToIgnoreCase(sortedString[j]) > 0) {
                    String swap = sortedString[j];
                    sortedString[j] = sortedString[j + 1];
                    sortedString[j + 1] = swap;
                }
            }
        }
        return sortedString;
    }

    public static String replaceInString(String stringToReplaceIn, String stringToReplace, String replacedByString) {
        return replaceInString(stringToReplaceIn, stringToReplace, replacedByString, false);
    }

    public static String replaceInString(String stringToReplaceIn, String stringToReplace, String replacedByString, boolean replaceEveryOccurrence) {
        do {
            int stringToReplaceStartsAt = stringToReplaceIn.indexOf(stringToReplace);
            int stringToReplaceEndsAt = stringToReplaceStartsAt + stringToReplace.length();
            if (stringToReplaceStartsAt >= 0 && stringToReplaceEndsAt > stringToReplaceStartsAt) {
                String preceedingSubString = stringToReplaceIn.substring(0, stringToReplaceStartsAt);
                String succeedingSubString = stringToReplaceIn.substring(stringToReplaceEndsAt, stringToReplaceIn.length());
                stringToReplaceIn = preceedingSubString + replacedByString + succeedingSubString;
            }
        } while (stringToReplaceIn.indexOf(stringToReplace) >= 0 && replaceEveryOccurrence);
        return stringToReplaceIn;
    }

    public static boolean contains(String stringToSearchIn, String stringToSearchFor) {
        return contains(stringToSearchIn, stringToSearchFor, false);
    }

    public static boolean contains(String stringToSearchIn, String stringToSearchFor, boolean ignoreCase) {
        if (ignoreCase) {
            return (stringToSearchIn.toLowerCase().indexOf(stringToSearchFor.toLowerCase()) >= 0);
        } else {
            return (stringToSearchIn.indexOf(stringToSearchFor) >= 0);
        }
    }

    public static String getHTMLLinkHref(String htmlString) {
        String linkId = null;
        String tagBegin = "<a href=\"";
        String tagEnd = "</a>";
        int linkIx = htmlString.lastIndexOf(tagBegin);
        if ((linkIx >= 0) && (htmlString.substring(linkIx).indexOf(tagEnd) < 0)) {
            String tempString = htmlString.substring(linkIx);
            int ixBeginQuotMarks = tempString.indexOf("\"");
            tempString = tempString.substring(ixBeginQuotMarks + 1);
            int ixEndQuotMarks = tempString.indexOf("\"");
            linkId = tempString.substring(0, ixEndQuotMarks);
        }
        return linkId;
    }

    public static void main(String[] args) {
        String line = "";
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("D:\\Java\\FSL\\dev\\bin\\learningUnits\\testtest\\textStudy\\author_text2.html"));
            while ((line = br.readLine()) != null) {
                System.out.println("link in line:\n" + line);
                System.out.println(getHTMLLinkHref(line));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static Vector createVectorFromArray(Object[] array) {
        Vector vector = new Vector();
        for (int i = 0; i < array.length; i++) vector.add(array[i]);
        return vector;
    }

    public static String removeSpacesFromString(String stringWithSpaces) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < stringWithSpaces.length(); i++) {
            if (!(stringWithSpaces.charAt(i) == ' ')) sb.append(stringWithSpaces.charAt(i));
        }
        return new String(sb);
    }

    public static boolean equalsIgnoreLeadingNumbers(String string1, String string2) {
        String string1_withoutLeadingNumbers = string1.substring(indexOfFirstLetter(string1));
        String string2_withoutLeadingNumbers = string2.substring(indexOfFirstLetter(string2));
        return string1_withoutLeadingNumbers.equals(string2_withoutLeadingNumbers);
    }

    public static boolean equalsIgnoreCaseIgnoreLeadingNumbers(String string1, String string2) {
        String string1_withoutLeadingNumbers = string1.substring(indexOfFirstLetter(string1));
        String string2_withoutLeadingNumbers = string2.substring(indexOfFirstLetter(string2));
        return string1_withoutLeadingNumbers.equalsIgnoreCase(string2_withoutLeadingNumbers);
    }

    public static int indexOfFirstLetter(String string) {
        int index = 0;
        for (int i = 0; i < string.length(); i++) {
            if (isLetter(string.charAt(i))) {
                index = i;
                break;
            }
        }
        return index;
    }

    public static boolean isLetter(char character) {
        switch(character) {
            case 'a':
                return true;
            case 'b':
                return true;
            case 'c':
                return true;
            case 'd':
                return true;
            case 'e':
                return true;
            case 'f':
                return true;
            case 'g':
                return true;
            case 'h':
                return true;
            case 'i':
                return true;
            case 'j':
                return true;
            case 'k':
                return true;
            case 'l':
                return true;
            case 'm':
                return true;
            case 'n':
                return true;
            case 'o':
                return true;
            case 'p':
                return true;
            case 'q':
                return true;
            case 'r':
                return true;
            case 's':
                return true;
            case 't':
                return true;
            case 'u':
                return true;
            case 'v':
                return true;
            case 'w':
                return true;
            case 'x':
                return true;
            case 'y':
                return true;
            case 'z':
                return true;
            case 'A':
                return true;
            case 'B':
                return true;
            case 'C':
                return true;
            case 'D':
                return true;
            case 'E':
                return true;
            case 'F':
                return true;
            case 'G':
                return true;
            case 'H':
                return true;
            case 'I':
                return true;
            case 'J':
                return true;
            case 'K':
                return true;
            case 'L':
                return true;
            case 'M':
                return true;
            case 'N':
                return true;
            case 'O':
                return true;
            case 'P':
                return true;
            case 'Q':
                return true;
            case 'R':
                return true;
            case 'S':
                return true;
            case 'T':
                return true;
            case 'U':
                return true;
            case 'V':
                return true;
            case 'W':
                return true;
            case 'X':
                return true;
            case 'Y':
                return true;
            case 'Z':
                return true;
            default:
                return false;
        }
    }
}

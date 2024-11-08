package ExamplesJaCoP;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import JaCoP.constraints.Alldistinct;
import JaCoP.constraints.Sum;
import JaCoP.constraints.SumWeight;
import JaCoP.constraints.XneqC;
import JaCoP.core.IntDomain;
import JaCoP.core.IntVar;
import JaCoP.core.Store;
import JaCoP.core.Var;

/**
 * Cryptogram. It solves any cryptogram puzzle of the form like SEND+MORE=MONEY.
 *
 * @author  Radoslaw Szymanek
 */
public class Cryptogram extends Example {

    /**
	 * It specifies how many lines of expressions can be inputed in one
	 * execution.
	 */
    public int maxInputLines = 100;

    /**
	 * It specifies the base of the numerical system to be used in the calculations.
	 */
    public int base = 10;

    /**
	 * It specifies the file which contains the puzzle to be solved.
	 */
    public String filename;

    private static int[] createWeights(int length, int base) {
        int[] weights = new int[length];
        weights[length - 1] = 1;
        for (int i = length - 2; i >= 0; i--) weights[i] = weights[i + 1] * base;
        return weights;
    }

    @Override
    public void model() {
        String lines[] = new String[maxInputLines];
        lines[0] = "HERE+SHE=COMES";
        int noLines = 1;
        if (filename != null) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(filename));
                String str;
                while ((str = in.readLine()) != null) if (!str.trim().equals("")) {
                    int commentPosition = str.indexOf("//");
                    if (commentPosition == 0) continue; else str = str.substring(0, commentPosition);
                    lines[noLines] = str;
                    noLines++;
                }
                in.close();
            } catch (FileNotFoundException e) {
                System.err.println("File " + filename + " could not be found");
            } catch (IOException e) {
                System.err.println("Something is wrong with the file" + filename);
            }
        } else {
            System.out.println("No input file was supplied, using standard HERE+SHE=COMES problem");
        }
        store = new Store();
        ArrayList<ArrayList<String>> words = new ArrayList<ArrayList<String>>();
        for (int i = 0; i < noLines; i++) words.add(new ArrayList<String>());
        HashMap<String, IntVar> letters = new HashMap<String, IntVar>();
        for (int i = 0; i < noLines; i++) {
            Pattern pat = Pattern.compile("[=+]");
            String[] result = pat.split(lines[i]);
            for (int j = 0; j < result.length; j++) words.get(i).add(result[j]);
        }
        vars = new ArrayList<Var>();
        for (int i = 0; i < noLines; i++) for (int j = words.get(i).size() - 1; j >= 0; j--) for (int z = words.get(i).get(j).length() - 1; z >= 0; z--) {
            char[] currentChar = { words.get(i).get(j).charAt(z) };
            if (letters.get(new String(currentChar)) == null) {
                IntVar currentLetter = new IntVar(store, new String(currentChar), 0, base - 1);
                vars.add(currentLetter);
                letters.put(new String(currentChar), currentLetter);
            }
        }
        if (letters.size() > base) {
            System.out.println("Expressions contain more than letters than base of the number system used ");
            System.out.println("Base " + base);
            System.out.println("Letters " + letters);
            System.out.println("There can not be any solution");
        }
        store.impose(new Alldistinct(vars.toArray(new IntVar[0])));
        for (int currentLine = 0; currentLine < noLines; currentLine++) {
            int noWords = words.get(currentLine).size();
            IntVar[] fdv4words = new IntVar[noWords];
            IntVar[] terms = new IntVar[noWords - 1];
            for (int j = 0; j < noWords; j++) {
                String currentWord = words.get(currentLine).get(j);
                fdv4words[j] = new IntVar(store, currentWord, 0, IntDomain.MaxInt);
                if (j < noWords - 1) terms[j] = fdv4words[j];
                IntVar[] lettersWithinCurrentWord = new IntVar[currentWord.length()];
                for (int i = 0; i < currentWord.length(); i++) {
                    char[] currentChar = { currentWord.charAt(i) };
                    lettersWithinCurrentWord[i] = letters.get(new String(currentChar));
                }
                store.impose(new SumWeight(lettersWithinCurrentWord, createWeights(currentWord.length(), base), fdv4words[j]));
                store.impose(new XneqC(lettersWithinCurrentWord[0], 0));
            }
            store.impose(new Sum(terms, fdv4words[noWords - 1]));
        }
    }

    /**
	 * It executes the program to solve any cryptographic puzzle.
	 * @param args no arguments read.
	 */
    public static void main(String args[]) {
        Cryptogram example = new Cryptogram();
        example.model();
        if (example.searchMostConstrainedStatic()) System.out.println("\nSolution(s) found");
    }
}

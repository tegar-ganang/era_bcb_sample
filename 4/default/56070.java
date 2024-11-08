import java.util.*;
import java.io.*;

/**
 * Reads a formatted Fasta file and outputs the length of the gene sequences
 * and the number of sequences in array format
 * 
 * @author Andrew Warner 
 * @version 1.0
 */
public class BinningFasta {

    public BinningFasta() {
    }

    /**
     * Stands for "read remove gap length;" reads the length of the sequences
     * once all of the gaps are removed from the output file "fasta.dat"
     * (usually)
     * @return The length of each sequence after gaps are removed.  Returns
     * -1 if the file is formatted incorrectly
     * @param data the input file (usually fasta.dat)
     * @pre The input file is formatted correctly, the first line being the
     * number of sequences followed by the length of each sequence
     * @post The length of the sequences after remove gaps is returned
     */
    public static int readRGlength(File data) throws IOException {
        BufferedReader input;
        try {
            input = new BufferedReader(new FileReader(data));
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        String nextLine = input.readLine();
        StringTokenizer a = new StringTokenizer(nextLine);
        a.nextToken();
        if (a == null) {
            return -1;
        }
        String b = a.nextToken();
        if (b == null) {
            return -1;
        }
        input.close();
        return (new Integer(b)).intValue();
    }

    /**
     * @pre fasta is a correctly formatted fasta file (ie the first line is 
     * >geneName, the next line being the gene sequence for that gene,
     * the next line is >geneName, and the line after that the sequence for
     * that gene, etc
     * @post the given fasta file is now stored in the appropriate fasta
     * text file to run the binning program, and numbers.dat is formatted
     * correctly
     * @param fasta the fasta file to read from
     * @param outFile the file to then write the fasta file to
     * @param numbers is the file to write the number of gene sequences
     * and length to (usually numbers.dat)
     * @return An array such that [0] is the number of gene sequences and [1] 
     * is the length of the sequences
     */
    public static int[] readFile(File fasta, File outFile, File numbers) throws IOException {
        BufferedReader input;
        try {
            input = new BufferedReader(new FileReader(fasta));
        } catch (IOException e) {
            System.out.println("Input file not found.");
            return null;
        }
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(outFile));
        } catch (IOException e) {
            System.out.println("Error in writing output file.");
            return null;
        }
        ArrayList<String> data = new ArrayList<String>();
        int numSequences = 0;
        int lenSequence = 0;
        String nextLine = input.readLine();
        if (nextLine == null) {
            System.out.println("Empty Input File");
            return null;
        }
        String gene = new String("");
        while (nextLine != null) {
            if (nextLine.charAt(0) != '>') {
                System.out.println("Invalid input file.");
                return null;
            }
            data.add(nextLine);
            nextLine = input.readLine();
            if (nextLine == null) {
                System.out.println("Invalid input file. There is a gene name with no corresponding gene sequence");
                return null;
            }
            gene = nextLine;
            numSequences++;
            data.add(nextLine);
            nextLine = input.readLine();
        }
        lenSequence = gene.length();
        input.close();
        for (int i = 0; i < data.size(); i++) {
            out.write((String) data.get(i));
            out.newLine();
        }
        out.close();
        int[] retArray = { numSequences, lenSequence };
        writeNumbersDat(numbers, retArray);
        return retArray;
    }

    /**
     * Writes the file numbers.dat given the values to write and the values
     * for the file
     * @pre numbers is the file to write to and values[0] is the number of
     * sequences in the fasta file and values[1] is the length of each gene
     * @post numbers.dat is created appropriately for the batch file
     * @param numbers the file to write the number of gene sequences and 
     * length of each sequence to (usually numbers.dat)
     * @param values The data to be written to that file (see above description)
     */
    public static void writeNumbersDat(File numbers, int[] values) throws IOException {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(numbers));
        } catch (IOException e) {
            System.out.println("Invalid output file!!");
            return;
        }
        out.write("" + values[0] + ", " + values[1]);
        out.newLine();
        out.newLine();
        out.write("numbers.dat");
        out.close();
    }

    /**
     * Reads output files from the binning program and takes the appropriate
     * values and inputs them into an array list
     * @param input a valid output file from the binning program
     * @param narr the narrative file to write the binning data to
     * @param narr The narrative writer
     * @pre input is a valid output file from the binning program (usually
     * output.dat)
     * @post the greatest percentage difference that has only one plus all the
     * percentages greater than that (with greater bins) are inputted as strings
     * into the returned arraylist
     * @return an arraylist representation of the binning data
     */
    public static ArrayList<String> readBins(File data, NarrWriter narr) throws IOException {
        BufferedReader input;
        try {
            input = new BufferedReader(new FileReader(data));
        } catch (IOException e) {
            System.out.println("Invalid input file file won't open");
            return null;
        }
        String nextLine = input.readLine();
        if (nextLine == null) {
            System.out.println("Invalid input file first line null");
            return null;
        }
        String storage = nextLine;
        StringTokenizer a;
        String x;
        int value;
        while (nextLine != null) {
            a = new StringTokenizer(nextLine);
            a.nextToken();
            if (!a.hasMoreTokens()) {
                System.out.println("Invalid input file one line doesn't have 2 tokens");
                return null;
            }
            value = new Integer(a.nextToken()).intValue();
            if (value > 1) {
                break;
            }
            narr.println(nextLine);
            storage = nextLine;
            nextLine = input.readLine();
        }
        ArrayList<String> output = new ArrayList<String>();
        output.add(storage);
        while (nextLine != null) {
            output.add(nextLine);
            narr.println(nextLine);
            nextLine = input.readLine();
        }
        input.close();
        return output;
    }

    /**
     * Takes in a list of FredOutVal objects, chooses the first column from
     * the right with a non-zero likelihood (ie first it tries 1.05x, 1.1x,
     * then 1.25x, etc, and then sorts the objects by that value, using the 
     * greater likelihoods as tiebreakers, and outputs the best Fred Value
     * from that sort
     * @pre values is a list of appropriate fredoutvals and debug is a valid
     * output filename
     * @post the best FredOutVal is returned and the sorted list is written
     * to debug
     * @param values The output from a run of Fred's method
     * @param debug An output file to write the sorted list of values to, in
     * case the chosen value was not optimal and the user wants to go back
     * and see what other values might be used
     * @return The optimal choice for hill climbing.
     */
    public static FredOutVal chooseBest(ArrayList<FredOutVal> values, File debug) throws IOException {
        BufferedWriter out;
        try {
            out = new BufferedWriter(new FileWriter(debug));
        } catch (IOException e) {
            System.out.println("Error creating output file.");
            return null;
        }
        int startPer = MasterVariables.getSortPercentage();
        double[] storage;
        int i;
        double twoHits = (double) 2 / MasterVariables.getnrep();
        for (startPer = startPer; startPer > 0; startPer--) {
            for (i = 0; i < values.size(); i++) {
                storage = values.get(i).getPercentages();
                if (storage[startPer] > twoHits || storage[startPer] == twoHits) {
                    break;
                }
            }
            if (i != values.size()) {
                break;
            }
        }
        MasterVariables.setSortPercentage(startPer);
        Heapsorter<FredOutVal> h = new Heapsorter<FredOutVal>();
        h.heapSort(values);
        for (int j = 0; j < values.size(); j++) {
            out.write(values.get(j).toString());
            out.newLine();
        }
        out.close();
        return values.get(0);
    }

    /**
     * Verifies an input file, which must be of the form >nameOfGene followed
     * by the gene sequence on the next line
     */
    public static boolean verifyInputFile(File file) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            String line = input.readLine();
            while (line != null) {
                if (line.charAt(0) != '>') {
                    input.close();
                    return false;
                }
                line = input.readLine();
                if (line == null) {
                    input.close();
                    return false;
                }
                if (line.charAt(0) == '>') {
                    input.close();
                    return false;
                }
                line = input.readLine();
            }
            input.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

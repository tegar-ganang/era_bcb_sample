package ecosim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *  Reads a formatted Fasta file and outputs the length of the gene sequences
 *  and the number of sequences in array format
 *
 *  @author Andrew Warner
 */
public class BinningFasta {

    /**
     *  BinningFasta
     *
     *  @param masterVariables The MasterVariable.
     */
    public BinningFasta(MasterVariables masterVariables) {
        this.masterVariables = masterVariables;
    }

    /**
     *  Stands for "read remove gap length;" reads the length of the sequences
     *  once all of the gaps are removed from the output file "fasta.dat" (usually).
     *
     *  pre - The input file is formatted correctly, the first line being the
     *  number of sequences followed by the length of each sequence.
     *  post - The length of the sequences after remove gaps is returned.
     *
     *  @param data The input file (usually fasta.dat).
     *  @return The length of each sequence after gaps are removed.  Returns -1 if the file is formatted incorrectly.
     */
    public int readRGlength(File data) {
        int retVal = -1;
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(data));
            String nextLine = input.readLine();
            StringTokenizer a = new StringTokenizer(nextLine);
            a.nextToken();
            if (a != null) {
                retVal = (new Integer(a.nextToken())).intValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return retVal;
    }

    /**
     *  Converts a properly formated fasta file to the format that Ecotype Simulation
     *  requires (ie. the first line is >gene1Name, the next line is the gene1 sequence,
     *  followed by the >gene2Name, and gene2 sequence).  Also writes information to the
     *  numbers.dat file.
     *
     *  @param fasta The fasta file to read from.
     *  @param outFile The file to then write the fasta file to.
     *  @param numbersFile The file to write the number of gene sequences and length to (usually numbers.dat).
     *  @return An array such that [0] is the number of gene sequences and [1] is the length of the sequences
     */
    public int[] readFile(Fasta fasta, File outFile, File numbersFile) {
        Fasta outFasta = new Fasta(fasta);
        outFasta.save(outFile);
        int[] retArray = { fasta.size(), fasta.length() };
        writeNumbersDat(numbersFile, retArray);
        return retArray;
    }

    /**
     *  Writes the file numbers.dat given the values to write and the values for the file.
     *
     *  pre: numbers is the file to write to and values[0] is the number of sequences in the fasta file and values[1] is the length of each gene.
     *  post: numbers.dat is created appropriately for the batch file.
     *
     *  @param numbers The file to write the number of gene sequences and length of each sequence to (usually numbers.dat).
     *  @param values An array containing the data to be written to that file (see above description).
     */
    public void writeNumbersDat(File numbers, int[] values) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(numbers));
            out.write("" + values[0] + ", " + values[1]);
            out.newLine();
            out.newLine();
            out.write("numbers.dat");
        } catch (IOException e) {
            System.out.println("Invalid output file!!");
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.out.println("Error closing output file!");
                }
            }
        }
    }

    /**
     *  Reads output files from the binning program and takes the appropriate values and inputs them into an array list.
     *
     *  pre - input is a valid output file from the binning program (usually output.dat).
     *  post - the greatest percentage difference that has only one plus all the
     *  percentages greater than that (with greater bins) are inputted as strings
     *  into the returned arraylist.
     *
     *  @param data A valid output file from the binning program.
     *  @param narr The narrative file to write the binning data to.
     *  @return ArrayList<String> containing a representation of the binning data.
     */
    public ArrayList<String> readBins(File data, NarrWriter narr) {
        ArrayList<String> output = new ArrayList<String>();
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(data));
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
            output.add(storage);
            while (nextLine != null) {
                output.add(nextLine);
                narr.println(nextLine);
                nextLine = input.readLine();
            }
        } catch (IOException e) {
            System.out.println("Invalid input file file won't open");
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.out.println("Error closing input file!");
                }
            }
        }
        return output;
    }

    /**
     *  Takes in a list of FredOutVal objects, chooses the first column from
     *  the right with a non-zero likelihood (ie first it tries 1.05x, 1.1x,
     *  then 1.25x, etc, and then sorts the objects by that value, using the
     *  greater likelihoods as tiebreakers, and outputs the best Fred Value
     *  from that sort.
     *
     *  pre: values is a list of appropriate fredoutvals and debug is a valid output filename.
     *  post: the best FredOutVal is returned and the sorted list is written to debug.
     *
     *  @param values The output from a run of Fred's method.
     *  @param debug An output file to write the sorted list of values to, in
     *  case the chosen value was not optimal and the user wants to go back
     *  and see what other values might be used.
     *  @return FredOutVal containing the optimal choice for hill climbing.
     */
    public FredOutVal chooseBest(ArrayList<FredOutVal> values, File debug) {
        FredOutVal best = null;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(debug));
            int startPer = masterVariables.getSortPercentage();
            double[] storage;
            int i;
            double twoHits = (double) 2 / masterVariables.getnrep();
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
            masterVariables.setSortPercentage(startPer);
            Heapsorter<FredOutVal> h = new Heapsorter<FredOutVal>();
            h.heapSort(values);
            for (int j = 0; j < values.size(); j++) {
                out.write(values.get(j).toString());
                out.newLine();
            }
            best = values.get(0);
        } catch (IOException e) {
            System.out.println("Error creating output file.");
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.out.println("Error closing output file!");
                }
            }
        }
        return best;
    }

    private MasterVariables masterVariables;
}

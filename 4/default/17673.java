import java.io.*;
import java.util.*;
import org.biojava.bio.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.seq.db.*;
import org.biojava.bio.seq.io.*;
import org.biojava.bio.symbol.*;

public class CreateSyntheticGenome {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("CreateSyntheticGenome was not called properly. It should be called as:");
            System.err.println("\tjava CreateSyntheticGenome fasta_input_file slo_output_file mutations_file [read_length]");
        }
        String inputFile = args[0];
        String outputFile = args[1];
        String mutationFile = args[2];
        int readLength = 36;
        if (args.length > 3) {
            readLength = Integer.parseInt(args[3]);
        }
        int numberOfMutations = fileLineLength(mutationFile);
        HashMap[] mutationArray = new HashMap[numberOfMutations];
        populateMutationHash(mutationFile, mutationArray);
        for (int i = 1; i < mutationArray.length; i++) {
            String mLen1 = mutationArray[i - 1].get("mutation").toString();
            int mPos1 = Integer.parseInt(mutationArray[i - 1].get("position").toString());
            int mPos2 = Integer.parseInt(mutationArray[i].get("position").toString());
            if (mPos1 + readLength + mLen1.length() >= mPos2 - readLength) {
                System.err.println("ERROR: Mutation position + length of mutation must be more than readLength (" + readLength + ") bases apart from the next mutation position minus readLength.");
                System.err.println(mPos2 + " is too close to " + mPos1);
                System.exit(-1);
            }
        }
        Sequence fastaSequence = prepareFastaRead(inputFile);
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            System.exit(-1);
        }
        try {
            out.write("#QuerySequenceID\tQuerySeq\tTargetLocation\tTargetStrand\tTargetSequence\tQueryQualityScore\tQueryPrefixTags\tQuerySuffixTags\tMultiReads\tReadNum\tQueryUnconvertedCsFirstRead\tQueryUnconvertedCsSecondRead\n");
        } catch (IOException e) {
            System.exit(-1);
        }
        for (int i = 0; i < mutationArray.length; i++) {
            int mPosition = Integer.parseInt(mutationArray[i].get("position").toString());
            String mType = mutationArray[i].get("type").toString();
            String mOriginal = mutationArray[i].get("original").toString();
            String mMutation = mutationArray[i].get("mutation").toString();
            int readOffset = mPosition - readLength;
            int totalReadLength = 2 * readLength - 1;
            if (readOffset < 0) {
                totalReadLength += readOffset;
                readOffset = 0;
            }
            String seqRead = fastaSequence.subStr(readOffset + 1, readOffset + totalReadLength);
            seqRead = seqRead.toUpperCase();
            String referenceSeq = seqRead;
            String mutatedSeq = seqRead;
            int changePos = totalReadLength - readLength;
            if (mType.equals("i")) {
            } else if ((mType.equals("v") || mType.equals("d")) && mOriginal.equals(seqRead.substring(changePos, changePos + mOriginal.length()))) {
                if (mType.equals("v")) {
                    mutatedSeq = seqRead.substring(0, changePos) + mMutation + seqRead.substring(changePos + 1, totalReadLength);
                } else if (mType.equals("d")) {
                    totalReadLength += mMutation.length() - 1;
                    seqRead = fastaSequence.subStr(readOffset + 1, readOffset + totalReadLength);
                    seqRead = seqRead.toUpperCase();
                    referenceSeq = seqRead;
                    mutatedSeq = seqRead.substring(0, changePos) + mMutation + seqRead.substring(changePos + 1, totalReadLength);
                } else if (mType.equals("i")) {
                    totalReadLength += mMutation.length() - 1;
                    seqRead = fastaSequence.subStr(readOffset + 1, readOffset + totalReadLength);
                    seqRead = seqRead.toUpperCase();
                    mutatedSeq = seqRead.substring(1, changePos + 1) + mMutation + seqRead.substring(changePos + 1, totalReadLength);
                    referenceSeq = seqRead.substring(1, changePos + 1) + mOriginal + seqRead.substring(changePos + 1, totalReadLength);
                    readOffset++;
                } else {
                    System.err.println("Something is wrong, mType came up as " + mType + ".");
                    System.exit(1);
                }
            } else {
                System.err.println("Something is wrong. The mutation was supposed to change a \'" + mOriginal + "\' but instead, that position is a \'" + seqRead.substring(changePos, changePos + 1) + "\'.");
                System.exit(1);
            }
            for (int j = 0; j <= totalReadLength - readLength; j++) {
                try {
                    out.write("1:1:1:" + ((i + 1) * 1000 + j) + "\t" + mutatedSeq.substring(j, j + readLength) + "\tchr22:" + (readOffset + j) + "\tF\t" + referenceSeq.substring(j, j + readLength) + "\t0" + "\n");
                    out.write("1:1:1:" + ((i + 1) * 1000 + j) + "\t" + mutatedSeq.substring(j, j + readLength) + "\tchr22:" + (readOffset + j) + "\tR\t" + reverseString(referenceSeq.substring(j, j + readLength)) + "\t0" + "\n");
                } catch (IOException e) {
                    System.exit(-1);
                }
            }
        }
        try {
            out.close();
        } catch (IOException e) {
            System.exit(-1);
        }
        System.exit(0);
    }

    public static String reverseString(String source) {
        int i, len = source.length();
        StringBuffer dest = new StringBuffer(len);
        for (i = (len - 1); i >= 0; i--) dest.append(source.charAt(i));
        return dest.toString();
    }

    public static int fileLineLength(String file) {
        int counter = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            while ((in.readLine() != null)) {
                counter++;
            }
            in.close();
        } catch (IOException e) {
            return -1;
        }
        return counter;
    }

    public static boolean populateMutationHash(String mutationFile, HashMap[] mutationArray) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(mutationFile));
            String str;
            int counter = 0;
            while ((str = in.readLine()) != null) {
                String[] line = str.split("\\t");
                line[3] = line[3].toUpperCase();
                line[4] = line[4].toUpperCase();
                mutationArray[counter] = new HashMap<String, String>(5);
                mutationArray[counter].put("chromosome", new String(line[0]));
                mutationArray[counter].put("position", new String(line[1]));
                mutationArray[counter].put("type", new String(line[2]));
                mutationArray[counter].put("original", new String(line[3]));
                mutationArray[counter].put("mutation", new String(line[4]));
                counter++;
            }
            in.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static Sequence prepareFastaRead(String fastafile) {
        Sequence s = null;
        try {
            BufferedReader is = new BufferedReader(new FileReader(fastafile));
            SequenceIterator si = SeqIOTools.readFastaDNA(is);
            if (si.hasNext()) {
                s = si.nextSequence();
            }
        } catch (BioException ex) {
            ex.printStackTrace();
        } catch (NoSuchElementException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        return s;
    }

    public static void dumpMutationArray(HashMap[] mutationArray) {
        for (int i = 0; i < mutationArray.length; i++) {
            System.err.println("Mutation " + i);
            System.err.println("\t" + mutationArray[i].get("chromosome"));
            System.err.println("\t" + mutationArray[i].get("position"));
            System.err.println("\t" + mutationArray[i].get("type"));
            System.err.println("\t" + mutationArray[i].get("original"));
            System.err.println("\t" + mutationArray[i].get("mutation"));
        }
    }
}

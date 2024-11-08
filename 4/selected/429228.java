package misc;

import java.io.*;
import java.util.*;
import misc.*;

public class BlatReader extends MappingResultIteratorAdaptor {

    public static String methodName = "BLAT";

    private BufferedReader dataFile;

    private String nextLine = "";

    private ArrayList nextMappedReadRecords = new ArrayList();

    private String readID = null;

    private int bestScore = 0;

    private int readLen = 0;

    public BlatReader(String blatFilename) throws FileNotFoundException {
        dataFile = new BufferedReader(new FileReader(new File(blatFilename)));
        fetchNextLineStartWithNumber();
    }

    private void fetchNextMappedRead() {
        try {
            readID = Util.getIthField(nextLine, 9);
            bestScore = Integer.parseInt(Util.getIthField(nextLine, 0));
            readLen = Integer.parseInt(Util.getIthField(nextLine, 10));
            nextMappedReadRecords = new ArrayList();
            nextMappedReadRecords.add(getAlignmentRecord(nextLine));
            while (fetchNextLineStartWithNumber() != null) {
                String line = nextLine;
                String nextReadID = Util.getIthField(line, 9);
                if (nextReadID.equals(readID)) {
                    nextMappedReadRecords.add(getAlignmentRecord(line));
                    int score = Integer.parseInt(Util.getIthField(nextLine, 0));
                    if (score > bestScore) bestScore = score;
                } else {
                    break;
                }
            }
        } catch (Exception ex) {
            System.err.println("LINE: " + nextLine);
            ex.printStackTrace();
            System.exit(1);
        }
    }

    protected AlignmentRecord getAlignmentRecord(String line) throws Exception {
        StringTokenizer st = new StringTokenizer(line);
        String[] tokens = line.split("\t");
        String subTokens[];
        int idx = 0;
        float identity = Float.parseFloat(tokens[idx]) / readLen;
        idx += 7;
        idx++;
        String strand = tokens[idx].intern();
        idx += 4;
        idx++;
        String chr = tokens[idx].toLowerCase().intern();
        idx += 3;
        idx++;
        int numBlocks = Integer.parseInt(tokens[idx]);
        idx++;
        subTokens = tokens[idx].split(",");
        int blockSizes[] = new int[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            blockSizes[i] = Integer.parseInt(subTokens[i]);
        }
        idx++;
        subTokens = tokens[idx].split(",");
        int qStarts[] = new int[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            qStarts[i] = Integer.parseInt(subTokens[i]) + 1;
        }
        idx++;
        subTokens = tokens[idx].split(",");
        int tStarts[] = new int[numBlocks];
        for (int i = 0; i < numBlocks; i++) {
            tStarts[i] = Integer.parseInt(subTokens[i]) + 1;
        }
        idx += 2;
        return new AlignmentRecord(identity, numBlocks, qStarts, blockSizes, chr, tStarts, blockSizes.clone(), strand);
    }

    public String getReadID() {
        return readID;
    }

    public int getReadLength() {
        return readLen;
    }

    public float getBestIdentity() {
        return ((float) bestScore / (float) readLen);
    }

    public int getNumMatch() {
        return nextMappedReadRecords.size();
    }

    private String fetchNextLineStartWithNumber() {
        try {
            while (dataFile.ready()) {
                String line = dataFile.readLine();
                if (line.length() > 0 && Character.isDigit(line.charAt(0))) {
                    nextLine = line;
                    return nextLine;
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.getStackTrace());
            System.exit(1);
        }
        nextLine = null;
        return nextLine;
    }

    public boolean hasNext() {
        if (nextLine == null) {
            try {
                dataFile.close();
            } catch (IOException ex) {
                System.err.println(ex.getStackTrace());
                System.exit(1);
            }
            return false;
        } else {
            return true;
        }
    }

    public Object next() {
        fetchNextMappedRead();
        return nextMappedReadRecords;
    }

    public static void main(String args[]) throws IOException {
        for (BlatReader iterator = new BlatReader(args[0]); iterator.hasNext(); ) {
            ArrayList mappingRecords = (ArrayList) iterator.next();
            System.out.println(iterator.getReadID() + "\t" + iterator.getNumMatch() + "\t" + iterator.getBestIdentity());
        }
    }
}

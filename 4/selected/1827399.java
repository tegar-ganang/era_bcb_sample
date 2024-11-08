package edu.uga.dawgpack.allvalid.align;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import edu.uga.dawgpack.common.Utils;
import edu.uga.dawgpack.index.util.HugeByteArray;
import edu.uga.dawgpack.singlematch.align.AlignerConstants;
import edu.uga.dawgpack.singlematch.align.BBWTAligner;
import edu.uga.dawgpack.singlematch.align.StagedAlignment;
import edu.uga.dawgpack.test.DebugHelper;

/**
 * @author Juber Ahamad Patel
 * 
 */
public class Index extends edu.uga.dawgpack.singlematch.align.Index {

    public Index(int readLength, int mismatches, int gaps, int minDistance, int maxDistance, int occInterval, long cacheSize, HugeByteArray genomeText) {
        super(readLength, mismatches, gaps, minDistance, maxDistance, occInterval, cacheSize, genomeText);
    }

    public Index(int readLength, int mismatches, int gaps, int occInterval, long cacheSize) {
        super(readLength, mismatches, gaps, occInterval, cacheSize);
    }

    /**
	 * align the given batch of read pairs against the genome and return the matches
	 * 
	 * @param reads
	 *            a batch of reads
	 * @param saDensities
	 * @param mismatches
	 * @param gaps
	 * @return
	 * @throws InterruptedException
	 */
    public long alignPairs(byte[][] reads, BlockingQueue<long[][]> outQueue) throws InterruptedException {
        long[][] alignments = new long[reads.length][];
        int alignmentCounter = 0;
        long[] matchRanges;
        LongArrayList rangeList1;
        LongArrayList rangeList2;
        long total = 0;
        long matchCount;
        byte[] read1;
        byte[] read2;
        byte[] read1Reverse;
        byte[] read2Reverse;
        for (int i = 0; i < reads.length; i = i + 2) {
            rangeList1 = new LongArrayList(100);
            rangeList2 = new LongArrayList(100);
            read1 = reads[i];
            read2 = reads[i + 1];
            read1Reverse = Utils.reverseComplement(read1);
            read2Reverse = Utils.reverseComplement(read2);
            align(read1, rangeList1);
            rangeList1.add(Long.MAX_VALUE);
            align(read2Reverse, rangeList1);
            matchRanges = rangeList1.elements();
            matchRanges = Arrays.copyOf(matchRanges, rangeList1.size());
            matchCount = 0;
            for (int j = 1; j < matchRanges.length; j = j + 2) {
                if (matchRanges[j - 1] == Long.MAX_VALUE) j++;
                matchCount = matchCount + matchRanges[j] - matchRanges[j - 1] + 1;
            }
            if (matchCount > 50000) {
                alignments[alignmentCounter] = null;
                alignmentCounter++;
            } else {
                total += matchCount;
                alignments[alignmentCounter] = matchRanges;
                alignmentCounter++;
            }
            align(read2, rangeList2);
            rangeList2.add(Long.MAX_VALUE);
            align(read1Reverse, rangeList2);
            matchRanges = rangeList2.elements();
            matchRanges = Arrays.copyOf(matchRanges, rangeList2.size());
            matchCount = 0;
            for (int j = 1; j < matchRanges.length; j = j + 2) {
                if (matchRanges[j - 1] == Long.MAX_VALUE) j++;
                matchCount = matchCount + matchRanges[j] - matchRanges[j - 1] + 1;
            }
            if (matchCount > 50000) {
                alignments[alignmentCounter] = null;
                alignmentCounter++;
            } else {
                total += matchCount;
                alignments[alignmentCounter] = matchRanges;
                alignmentCounter++;
            }
        }
        outQueue.put(alignments);
        return total;
    }

    /**
	 * currently implementing two-mismatch, zero gaps search, extend afterwards
	 * 
	 * @param locations
	 * @param read
	 * @param mismatches
	 * @param gaps
	 */
    private void align(byte[] read, LongArrayList matchRanges) {
        long[][][] saRanges = new long[readLength][readLength][2];
        saRange(read, 0, read.length - 1, saRanges);
        long start = saRanges[0][readLength - 1][0];
        long end = saRanges[0][readLength - 1][1];
        if (start != AlignerConstants.INVALID) {
            matchRanges.add(start);
            matchRanges.add(end);
        }
        long[][][] rsaRanges = new long[readLength][readLength][2];
        rsaRange(read, 0, read.length - 1, saRanges, rsaRanges);
        int approach;
        for (int i = 0; i < readLength; i++) {
            approach = oneMismatchPlan[i];
            switch(approach) {
                case 1:
                    if (saRanges[i + 1][readLength - 1][0] != AlignerConstants.INVALID) combineBackward(read, i, saRanges[i + 1][readLength - 1][0], saRanges[i + 1][readLength - 1][1], matchRanges);
                    break;
                case 2:
                    if (saRanges[0][i - 1][0] != AlignerConstants.INVALID) combineForward(read, i, saRanges[0][i - 1][0], saRanges[0][i - 1][1], rsaRanges[0][i - 1][0], rsaRanges[0][i - 1][1], matchRanges);
            }
        }
        for (int i = 1; i < bidirectionalOutreach.length; i++) {
            rsaRange(read, i, bidirectionalOutreach[i], saRanges, rsaRanges);
        }
        for (int i = 0; i < readLength; i++) {
            for (int j = i + 1; j < readLength; j++) {
                approach = twoMismatchPlan[i][j];
                switch(approach) {
                    case 1:
                        if (saRanges[j + 1][readLength - 1][0] != AlignerConstants.INVALID) {
                            combineBackward(read, i, j, saRanges[j + 1][readLength - 1][0], saRanges[j + 1][readLength - 1][1], matchRanges);
                        }
                        break;
                    case 2:
                        if (saRanges[0][i - 1][0] != AlignerConstants.INVALID) {
                            combineForward(read, i, j, saRanges[0][i - 1][0], saRanges[0][i - 1][1], rsaRanges[0][i - 1][0], rsaRanges[0][i - 1][1], matchRanges);
                        }
                        break;
                    case 3:
                        if (saRanges[i + 1][j - 1][0] != AlignerConstants.INVALID) {
                            combineBidirectionalBF(read, i, j, saRanges[i + 1][j - 1][0], saRanges[i + 1][j - 1][1], rsaRanges[i + 1][j - 1][0], rsaRanges[i + 1][j - 1][1], matchRanges);
                        }
                        break;
                    case 4:
                        if (saRanges[i + 1][j - 1][0] != AlignerConstants.INVALID) {
                            combineBidirectionalFB(read, i, j, saRanges[i + 1][j - 1][0], saRanges[i + 1][j - 1][1], rsaRanges[i + 1][j - 1][0], rsaRanges[i + 1][j - 1][1], matchRanges);
                        }
                }
            }
        }
    }

    /**
	 * align the given batch of reads against the genome and test if the aligned positions tally in
	 * the actual genome text
	 * 
	 * @param reads
	 *            a batch of reads
	 * 
	 */
    public void alignAndCheckSanity(byte[][] reads) {
        long[] matchRanges;
        LongArrayList matchRangeList = null;
        long[] matchesArray;
        for (int i = 0; i < reads.length; i++) {
            try {
                matchRangeList = new LongArrayList(50);
                align(reads[i], matchRangeList);
            } catch (Throwable e) {
                System.out.println("problem alignining read");
                e.printStackTrace();
            }
            matchRanges = matchRangeList.elements();
            matchRanges = Arrays.copyOf(matchRanges, matchRangeList.size());
            LongArrayList matchesList = new LongArrayList();
            for (int j = 1; j < matchRanges.length; j = j + 2) {
                if (matchRanges[j] == AlignerConstants.INVALID) continue;
                for (long k = matchRanges[j - 1]; k <= matchRanges[j]; k++) {
                    matchesList.add(sa.get(k));
                }
            }
            matchesArray = matchesList.elements();
            matchesArray = Arrays.copyOf(matchesArray, matchesList.size());
            long problems = 0;
            problems = DebugHelper.validateAlignment(genomeText, reads[i], readLength, matchesArray, mismatches);
            if (matchesArray.length > 0 || problems > 0) {
                System.out.println("matches: " + matchesArray.length + " problems " + problems);
            }
        }
    }

    private int matched;

    public void alignAndSavePositions(byte[][] reads) throws IOException {
        long[] matchRanges1;
        long[] matchRanges2;
        LongArrayList matchRangeList1 = new LongArrayList(50);
        LongArrayList matchRangeList2 = new LongArrayList(50);
        LongArrayList matchesList1 = new LongArrayList(50);
        LongArrayList matchesList2 = new LongArrayList(50);
        long[] matchesArray1;
        long[] matchesArray2;
        FileWriter result = new FileWriter(new File("alignments.txt"));
        ObjectArrayList list = metadata.getStartingPositions().values();
        LongArrayList startingPositions = new LongArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            startingPositions.add((Long) (list.getQuick(i)));
        }
        startingPositions.sort();
        for (int i = 0; i < reads.length; i++) {
            result.write(Utils.decode(reads[i]));
            byte[] reverse = Utils.reverseComplement(reads[i]);
            try {
                matchRangeList1.clear();
                matchRangeList2.clear();
                align(reads[i], matchRangeList1);
                align(reverse, matchRangeList2);
            } catch (Throwable e) {
                System.out.println("problem alignining read");
                e.printStackTrace();
            }
            matchRanges1 = matchRangeList1.elements();
            matchRanges2 = matchRangeList2.elements();
            matchRanges1 = Arrays.copyOf(matchRanges1, matchRangeList1.size());
            matchRanges2 = Arrays.copyOf(matchRanges2, matchRangeList2.size());
            matchesList1.clear();
            matchesList2.clear();
            for (int j = 1; j < matchRanges1.length; j = j + 2) {
                if (matchRanges1[j] == AlignerConstants.INVALID) continue;
                for (long k = matchRanges1[j - 1]; k <= matchRanges1[j]; k++) {
                    matchesList1.add(sa.get(k));
                }
            }
            for (int j = 1; j < matchRanges2.length; j = j + 2) {
                if (matchRanges2[j] == AlignerConstants.INVALID) continue;
                for (long k = matchRanges2[j - 1]; k <= matchRanges2[j]; k++) {
                    matchesList2.add(sa.get(k));
                }
            }
            int size = matchesList1.size();
            int[] pos;
            result.write("\tpositive strand: ");
            for (int j = 0; j < matchesList1.size(); j++) {
                pos = Utils.toChromosomePosition(matchesList1.getQuick(j), startingPositions);
                if (pos[0] == 23) {
                    result.write("X:" + pos[1] + "  ");
                } else if (pos[0] == 24) {
                    result.write("Y:" + pos[1] + "  ");
                } else {
                    result.write(pos[0] + ":" + pos[1] + "  ");
                }
            }
            size = matchesList2.size();
            result.write("\tnegative strand: ");
            for (int j = 0; j < matchesList2.size(); j++) {
                pos = Utils.toChromosomePosition(matchesList2.getQuick(j), startingPositions);
                if (pos[0] == 23) {
                    result.write("X:" + pos[1] + "  ");
                } else if (pos[0] == 24) {
                    result.write("Y:" + pos[1] + "  ");
                } else {
                    result.write(pos[0] + ":" + pos[1] + "  ");
                }
            }
            result.write("\n");
            matched++;
        }
        result.close();
        System.exit(0);
    }

    public void alignAndSaveUnique(byte[][] reads, ObjectOutputStream out) throws IOException {
        AlignmentDetails alignmentDetails = null;
        int[] pos;
        ObjectArrayList list = metadata.getStartingPositions().values();
        LongArrayList startingPositions = new LongArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            startingPositions.add((Long) (list.getQuick(i)));
        }
        startingPositions.sort();
        for (int i = 0; i < reads.length; i++) {
            byte[] reverse = Utils.reverseComplement(reads[i]);
            try {
                alignmentDetails = alignForUnique(reads[i], reverse);
            } catch (Throwable e) {
                System.out.println("problem alignining read");
                e.printStackTrace();
            }
            if (alignmentDetails == null) continue;
            pos = Utils.toChromosomePosition(sa.get(alignmentDetails.getPosition()), startingPositions);
            alignmentDetails.setRead(reads[i]);
            alignmentDetails.setChr((int) pos[0]);
            alignmentDetails.setPosition(pos[1]);
            out.writeObject(alignmentDetails);
            matched++;
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, InterruptedException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document config = builder.parse(new File(args[0]));
        BBWTAligner aligner = new BBWTAligner(config);
        edu.uga.dawgpack.allvalid.align.Index index = (Index) aligner.testGetIndex();
        byte[] read = Utils.encode("tcttccaaaaaatacagtatctgtgaagttcaata".toCharArray());
        index.alignAndCheckSanity(new byte[][] { read });
    }
}

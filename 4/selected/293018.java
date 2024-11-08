package test;

import java.util.*;
import misc.*;
import java.io.*;
import java.util.Map.*;

/**
 * Test for the KS filtering idea, and for finding important parameters.
 * This class (TestKsFilter01) output intron informat for further computation
 *
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class TestKsFilter01 {

    private static Map mappingFilenameMethodMap = new LinkedHashMap();

    private static Map mappingMethodMap = null;

    private static String outFilename = null;

    private static int joinFactor = 0;

    private static float identityCutoff = 0.95F;

    private static int minimumOverlap = 6;

    private static void paraProc(String[] args) {
        int i;
        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-M")) {
                mappingFilenameMethodMap.put(args[i + 2], args[i + 1]);
                i += 2;
            } else if (args[i].equals("-O")) {
                outFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-J")) {
                joinFactor = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-ID")) {
                identityCutoff = Float.parseFloat(args[i + 1]);
                i++;
            } else if (args[i].equals("-min")) {
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        mappingMethodMap = Util.getMethodMap("misc.MappingResultIterator", System.getProperty("java.class.path"), "misc");
        if (mappingFilenameMethodMap.size() <= 0) {
            System.err.println("mapping method/filename (-M) not assigned, available methods:");
            for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                System.out.println(iterator.next());
            }
            System.exit(1);
        }
        for (Iterator methodIterator = mappingFilenameMethodMap.values().iterator(); methodIterator.hasNext(); ) {
            String mappingMethod = (String) methodIterator.next();
            if (mappingMethodMap.keySet().contains(mappingMethod) == false) {
                System.err.println("assigned mapping method (-M) not exists: " + mappingMethod + ", available methods:");
                for (Iterator iterator = mappingMethodMap.keySet().iterator(); iterator.hasNext(); ) {
                    System.out.println(iterator.next());
                }
                System.exit(1);
            }
        }
        if (outFilename == null) {
            System.err.println("output filename (-O) not assigned");
            System.exit(1);
        }
        System.out.println("program: TestKsFilter01");
        System.out.println("mapping method/filename (-M):");
        for (Iterator iterator = mappingFilenameMethodMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Entry) iterator.next();
            System.out.println("  " + entry.getValue() + " : " + entry.getKey());
        }
        System.out.println("output filename (-O): " + outFilename);
        System.out.println("block join factor (-J): " + joinFactor);
        System.out.println("identity cutoff (-ID): " + identityCutoff);
        System.out.println("minimum overlap (-min): " + minimumOverlap);
        System.out.println();
    }

    public static void main(String[] args) {
        paraProc(args);
        Map tmpIntronCntMap = new HashMap();
        Map tmpIntronSplicingPosMap = new HashMap();
        for (Iterator mriIterator = mappingFilenameMethodMap.entrySet().iterator(); mriIterator.hasNext(); ) {
            Map.Entry entry = (Entry) mriIterator.next();
            String mappingFilename = (String) entry.getKey();
            String mappingMethod = (String) entry.getValue();
            int mappedReadCnt = 0;
            int processedLines = 0;
            for (MappingResultIterator mappingResultIterator = Util.getMRIinstance(mappingFilename, mappingMethodMap, mappingMethod); mappingResultIterator.hasNext(); ) {
                mappedReadCnt++;
                ArrayList mappingRecords = (ArrayList) mappingResultIterator.next();
                processedLines += mappingRecords.size();
                if (mappingResultIterator.getBestIdentity() < identityCutoff) continue;
                ArrayList acceptedRecords = new ArrayList();
                for (int i = 0; i < mappingRecords.size(); i++) {
                    AlignmentRecord record = (AlignmentRecord) mappingRecords.get(i);
                    if (record.identity >= mappingResultIterator.getBestIdentity()) {
                        if (joinFactor > 0) record.nearbyJoin(joinFactor);
                        acceptedRecords.add(record);
                    }
                }
                if (acceptedRecords.size() > 1) continue;
                AlignmentRecord record = (AlignmentRecord) acceptedRecords.get(0);
                if (record.numBlocks == 1) continue;
                int minBlockSize = -1;
                for (int i = 0; i < record.numBlocks; i++) {
                    if (minBlockSize < 0 || minBlockSize > record.tBlockSizes[i]) {
                        minBlockSize = record.tBlockSizes[i];
                    }
                }
                if (minBlockSize < minimumOverlap) continue;
                for (int i = 0; i < record.numBlocks - 1; i++) {
                    int intronStart = record.tStarts[i] + record.tBlockSizes[i] - 1;
                    int intronStop = record.tStarts[i + 1];
                    int splicingPos = record.qStarts[i] + record.qBlockSizes[i] - 1;
                    GenomeInterval intron = new GenomeInterval(record.chr, intronStart, intronStop);
                    if (tmpIntronCntMap.containsKey(intron)) {
                        int val = ((Integer) tmpIntronCntMap.get(intron)).intValue() + 1;
                        tmpIntronCntMap.put(intron, val);
                    } else {
                        tmpIntronCntMap.put(intron, 1);
                    }
                    Map splicingMap;
                    if (tmpIntronSplicingPosMap.containsKey(intron)) {
                        splicingMap = (Map) tmpIntronSplicingPosMap.get(intron);
                    } else {
                        splicingMap = new TreeMap();
                        tmpIntronSplicingPosMap.put(intron, splicingMap);
                    }
                    if (splicingMap.containsKey(splicingPos)) {
                        int val = ((Integer) splicingMap.get(splicingPos)).intValue() + 1;
                        splicingMap.put(splicingPos, val);
                    } else {
                        splicingMap.put(splicingPos, 1);
                    }
                }
            }
            System.out.println(mappedReadCnt + " mapped reads (" + processedLines + " lines) in " + mappingFilename);
        }
        Map intronCntMap = new TreeMap(tmpIntronCntMap);
        Map intronSplicingPosMap = new TreeMap(tmpIntronSplicingPosMap);
        try {
            FileWriter fw = new FileWriter(outFilename);
            for (Iterator intronIterator = intronCntMap.keySet().iterator(); intronIterator.hasNext(); ) {
                GenomeInterval intron = (GenomeInterval) intronIterator.next();
                int readCnt = ((Integer) intronCntMap.get(intron)).intValue();
                Map splicingMap = (Map) intronSplicingPosMap.get(intron);
                fw.write(intron.getChr() + "\t" + intron.getStart() + "\t" + intron.getStop() + "\t" + readCnt + "\t" + splicingMap + "\n");
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

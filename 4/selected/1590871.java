package test;

import java.util.*;
import misc.*;
import java.io.*;
import java.util.Map.*;

/**
 * Read output of TestKsFilter01, do further filtering.
 *
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class TestKsFilter02 {

    private static String gffFilename = null;

    private static String modelFilename = null;

    private static String transcriptFilename = null;

    private static String ksTable1Filename = null;

    private static String ksTable2Filename = null;

    private static String inFilename = null;

    private static String outFilename = null;

    private static int minimumOverlap = 6;

    private static int readLength = 0;

    private static void paraProc(String[] args) {
        int i;
        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-model")) {
                modelFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-GFF")) {
                gffFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-transcript")) {
                transcriptFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-KS1")) {
                ksTable1Filename = args[i + 1];
                i++;
            } else if (args[i].equals("-KS2")) {
                ksTable2Filename = args[i + 1];
                i++;
            } else if (args[i].equals("-I")) {
                inFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-O")) {
                outFilename = args[i + 1];
                i++;
            } else if (args[i].equals("-min")) {
                minimumOverlap = Integer.parseInt(args[i + 1]);
                i++;
            } else if (args[i].equals("-L")) {
                readLength = Integer.parseInt(args[i + 1]);
                i++;
            }
        }
        if (gffFilename == null) {
            System.err.println("canonical GFF filename (-GFF) not assigned");
            System.exit(1);
        }
        if (modelFilename == null) {
            System.err.println("model filename (-model) not assigned");
            System.exit(1);
        }
        if (transcriptFilename == null) {
            System.err.println("transcript CGFF filename (-transcript) not assigned");
            System.exit(1);
        }
        if (inFilename == null) {
            System.err.println("input filename (-I) not assigned");
            System.exit(1);
        }
        if (outFilename == null) {
            System.err.println("output filename (-O) not assigned");
            System.exit(1);
        }
        System.out.println("program: TestKsFilter02");
        System.out.println("canonical GFF filename (-GFF): " + gffFilename);
        System.out.println("model filename (-model): " + modelFilename);
        System.out.println("transcript CGFF filename (-transcript): " + transcriptFilename);
        System.out.println("KS table 1 (-KS1): " + ksTable1Filename);
        System.out.println("KS table 2 (-KS2): " + ksTable2Filename);
        System.out.println("input filename (-I): " + inFilename);
        System.out.println("output filename (-O): " + outFilename);
        System.out.println("minimum overlap (-min): " + minimumOverlap);
        System.out.println("read length (-L): " + readLength);
        System.out.println();
    }

    public static void main(String[] args) {
        paraProc(args);
        CanonicalGFF cgff = new CanonicalGFF(gffFilename);
        CanonicalGFF geneModel = new CanonicalGFF(modelFilename);
        CanonicalGFF transcriptGff = new CanonicalGFF(transcriptFilename);
        TreeMap ksTable1 = getKsTable(ksTable1Filename);
        TreeMap ksTable2 = getKsTable(ksTable2Filename);
        Map intronReadCntMap = new TreeMap();
        Map intronSplicingPosMap = new TreeMap();
        try {
            BufferedReader fr = new BufferedReader(new FileReader(inFilename));
            while (fr.ready()) {
                String line = fr.readLine();
                if (line.startsWith("#")) continue;
                String tokens[] = line.split("\t");
                String chr = tokens[0];
                int start = Integer.parseInt(tokens[1]);
                int stop = Integer.parseInt(tokens[2]);
                GenomeInterval intron = new GenomeInterval(chr, start, stop);
                int readCnt = Integer.parseInt(tokens[3]);
                intronReadCntMap.put(intron, readCnt);
                String splicingMapStr = tokens[4];
                Map splicingMap = getSplicingMap(splicingMapStr);
                intronSplicingPosMap.put(intron, splicingMap);
            }
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        double[] hdCDF = getHdCdf(readLength, minimumOverlap);
        try {
            FileWriter fw = new FileWriter(outFilename);
            for (Iterator intronIterator = intronReadCntMap.keySet().iterator(); intronIterator.hasNext(); ) {
                GenomeInterval intron = (GenomeInterval) intronIterator.next();
                int readCnt = ((Integer) intronReadCntMap.get(intron)).intValue();
                TreeMap splicingMap = (TreeMap) intronSplicingPosMap.get(intron);
                Object ksInfoArray[] = distributionAccepter((TreeMap) splicingMap.clone(), readCnt, hdCDF, ksTable1, ksTable2);
                boolean ksAccepted = (Boolean) ksInfoArray[0];
                double testK = (Double) ksInfoArray[1];
                double standardK1 = (Double) ksInfoArray[2];
                double standardK2 = (Double) ksInfoArray[3];
                int positionCnt = splicingMap.size();
                Object modelInfoArray[] = getModelAgreedSiteCnt(intron, cgff, geneModel, transcriptGff);
                int modelAgreedSiteCnt = (Integer) modelInfoArray[0];
                int maxAgreedTransSiteCnt = (Integer) modelInfoArray[1];
                boolean containedBySomeGene = (Boolean) modelInfoArray[2];
                int numIntersectingGenes = (Integer) modelInfoArray[3];
                int distance = intron.getStop() - intron.getStart();
                fw.write(intron.getChr() + ":" + intron.getStart() + ".." + intron.getStop() + "\t" + distance + "\t" + readCnt + "\t" + splicingMap + "\t" + probabilityEvaluation(readLength, distance, readCnt, splicingMap, positionCnt) + "\t" + ksAccepted + "\t" + testK + "\t" + standardK1 + "\t" + standardK2 + "\t" + positionCnt + "\t" + modelAgreedSiteCnt + "\t" + maxAgreedTransSiteCnt + "\t" + containedBySomeGene + "\t" + numIntersectingGenes + "\n");
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static double probabilityEvaluation(int readLen, int distance, int readCnt, Map splicingMap, int positionCnt) {
        int temp, countValue = 0, sum = 0;
        for (Iterator iterator = splicingMap.keySet().iterator(); iterator.hasNext(); ) {
            Integer key = (Integer) iterator.next();
            Integer val = ((Integer) splicingMap.get(key)).intValue();
            temp = key * val;
            sum += temp;
            countValue += val;
        }
        double Xavg = (double) sum / (double) countValue;
        double L = (Xavg < (readLen - Xavg)) ? Xavg : (readLen - Xavg);
        double result = (double) distance * Math.pow(0.25, L);
        return result;
    }

    private static Object[] distributionAccepter(Map splicingMap, int readCnt, double[] hdCDF, TreeMap ksTable1, TreeMap ksTable2) {
        int sqrtReadCnt = 0;
        for (Iterator iterator = splicingMap.keySet().iterator(); iterator.hasNext(); ) {
            Object key = iterator.next();
            int cnt = ((Integer) splicingMap.get(key)).intValue();
            cnt = Math.round((float) Math.sqrt(cnt));
            sqrtReadCnt += cnt;
            splicingMap.put(key, cnt);
        }
        double[] edCDF = getEdCdf(readLength, sqrtReadCnt, splicingMap);
        double testK = 0;
        for (int i = 0; i < edCDF.length; i++) {
            if ((Math.abs(hdCDF[i] - edCDF[i])) > testK) testK = Math.abs(hdCDF[i] - edCDF[i]);
        }
        double standardK1;
        if (ksTable1.containsKey(sqrtReadCnt)) {
            standardK1 = ((Double) ksTable1.get(sqrtReadCnt)).doubleValue();
        } else {
            SortedMap headMap = (SortedMap) ksTable1.headMap(sqrtReadCnt);
            standardK1 = ((Double) headMap.get(headMap.lastKey())).doubleValue();
        }
        double standardK2;
        if (ksTable2.containsKey(sqrtReadCnt)) {
            standardK2 = ((Double) ksTable2.get(sqrtReadCnt)).doubleValue();
        } else {
            SortedMap headMap = (SortedMap) ksTable2.headMap(sqrtReadCnt);
            standardK2 = ((Double) headMap.get(headMap.lastKey())).doubleValue();
        }
        boolean ksAccepted;
        if (standardK1 >= testK) ksAccepted = true; else ksAccepted = false;
        Object ansArray[] = { ksAccepted, testK, standardK1, standardK2 };
        return ansArray;
    }

    private static Object[] getModelAgreedSiteCnt(GenomeInterval intron, CanonicalGFF cgff, CanonicalGFF model, CanonicalGFF transcriptome) {
        Set testIntervalSet = new HashSet();
        testIntervalSet.add(intron);
        Set hitModelRegions = model.getRelatedGenes(intron.getChr(), testIntervalSet, true, false, 1, false);
        boolean modelFrontMatch = false;
        boolean modelRearMatch = false;
        for (Iterator modelIterator = hitModelRegions.iterator(); modelIterator.hasNext(); ) {
            GenomeInterval modelRegion = (GenomeInterval) modelIterator.next();
            String modelID = (String) modelRegion.getUserObject();
            Set exonIntervals = (Set) model.geneExonRegionMap.get(modelID);
            for (Iterator exonIterator = exonIntervals.iterator(); exonIterator.hasNext(); ) {
                Interval exonInterval = (Interval) exonIterator.next();
                if (intron.getStop() == exonInterval.getStart()) modelRearMatch = true;
                if (intron.getStart() == exonInterval.getStop()) modelFrontMatch = true;
            }
        }
        int agreedModelSiteCnt = 0;
        if (modelRearMatch) agreedModelSiteCnt++;
        if (modelFrontMatch) agreedModelSiteCnt++;
        Set hitTranscriptRegions = transcriptome.getRelatedGenes(intron.getChr(), testIntervalSet, false, true, 1, false);
        int maxAgreedTransSiteCnt = 0;
        for (Iterator transcriptIterator = hitTranscriptRegions.iterator(); transcriptIterator.hasNext(); ) {
            GenomeInterval transcriptRegion = (GenomeInterval) transcriptIterator.next();
            String transcriptID = (String) transcriptRegion.getUserObject();
            Set exonIntervals = (Set) transcriptome.geneExonRegionMap.get(transcriptID);
            boolean transFrontMatch = false;
            boolean transRearMatch = false;
            for (Iterator exonIterator = exonIntervals.iterator(); exonIterator.hasNext(); ) {
                Interval exonInterval = (Interval) exonIterator.next();
                if (intron.getStop() == exonInterval.getStart()) transRearMatch = true;
                if (intron.getStart() == exonInterval.getStop()) transFrontMatch = true;
            }
            int agreedTransSiteCnt = 0;
            if (transRearMatch) agreedTransSiteCnt++;
            if (transFrontMatch) agreedTransSiteCnt++;
            if (maxAgreedTransSiteCnt < agreedTransSiteCnt) maxAgreedTransSiteCnt = agreedTransSiteCnt;
        }
        Set containingGeneRegions = cgff.getRelatedGenes(intron.getChr(), testIntervalSet, false, true, 1, false);
        boolean containedBySomeGene;
        if (containingGeneRegions.size() > 0) {
            containedBySomeGene = true;
        } else {
            containedBySomeGene = false;
        }
        Set intersectingGeneRegions = cgff.getRelatedGenes(intron.getChr(), testIntervalSet, false, false, 1, false);
        Object ansArray[] = { agreedModelSiteCnt, maxAgreedTransSiteCnt, containedBySomeGene, intersectingGeneRegions.size() };
        return ansArray;
    }

    private static double[] getEdCdf(int readLen, int readCnt, Map splicingMap) {
        double[] edCDF = new double[readLen + 2];
        double[] edPDF = new double[readLen + 2];
        for (int i = 0; i <= readLen + 1; i++) {
            if (splicingMap.containsKey(i)) {
                edPDF[i] = ((Integer) splicingMap.get(i)).doubleValue() / readCnt;
            } else {
                edPDF[i] = 0;
            }
        }
        edCDF[0] = edPDF[0];
        for (int i = 1; i <= readLen + 1; i++) {
            edCDF[i] = edCDF[i - 1] + edPDF[i];
        }
        return edCDF;
    }

    private static double[] getHdCdf(int readLen, int minBlock) {
        double[] hdCDF = new double[readLen + 2];
        double[] hdPDF = new double[readLen + 2];
        for (int i = 0; i <= minBlock - 1; i++) {
            hdPDF[i] = 0;
        }
        for (int i = minBlock; i <= (readLen - minBlock + 1); i++) {
            hdPDF[i] = 1.0 / (readLen - 2 * minBlock + 2);
        }
        for (int i = (readLen - minBlock + 2); i <= readLen + 1; i++) {
            hdPDF[i] = 0;
        }
        hdCDF[0] = hdPDF[0];
        for (int i = 1; i <= readLen + 1; i++) {
            hdCDF[i] = hdCDF[i - 1] + hdPDF[i];
        }
        return hdCDF;
    }

    private static TreeMap getSplicingMap(String splicingMapStr) {
        TreeMap ansMap = new TreeMap();
        String tokenLv1[] = splicingMapStr.split("[{},\\s]+");
        for (int idx1 = 0; idx1 < tokenLv1.length; idx1++) {
            if (tokenLv1[idx1].length() == 0) continue;
            String tokenLv2[] = tokenLv1[idx1].split("=");
            int location = Integer.parseInt(tokenLv2[0]);
            int freq = Integer.parseInt(tokenLv2[1]);
            ansMap.put(location, freq);
        }
        return ansMap;
    }

    private static TreeMap getKsTable(String filename) {
        TreeMap ansMap = new TreeMap();
        try {
            BufferedReader fr = new BufferedReader(new FileReader(filename));
            while (fr.ready()) {
                String line = fr.readLine();
                StringTokenizer st = new StringTokenizer(line);
                int readNum = Integer.parseInt(st.nextToken());
                double maxK = Double.parseDouble(st.nextToken());
                ansMap.put(readNum, maxK);
            }
            fr.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return ansMap;
    }
}

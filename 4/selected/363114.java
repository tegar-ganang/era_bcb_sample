package rnaseq;

import misc.*;
import java.util.*;
import java.io.*;

public class SpliceCounter implements ReadCounter {

    private boolean checkByContaining;

    private int minimumOverlap;

    private Map geneSpliceCntMap = new HashMap();

    private Map geneExonPairSplicingPosMap = new HashMap();

    public Map geneSpliceMapByModel = new HashMap();

    private CanonicalGFF geneModel = null;

    public SpliceCounter(CanonicalGFF geneModel, CanonicalGFF cgff, boolean checkByContaining, int minimumOverlap) {
        this.geneModel = geneModel;
        this.checkByContaining = checkByContaining;
        this.minimumOverlap = minimumOverlap;
        if (geneModel != null) {
            for (Iterator modelIterator = geneModel.geneExonRegionMap.keySet().iterator(); modelIterator.hasNext(); ) {
                String modelID = (String) modelIterator.next();
                GenomeInterval modelInterval = (GenomeInterval) geneModel.geneRegionMap.get(modelID);
                Set modelExonSet = (Set) geneModel.geneExonRegionMap.get(modelID);
                Set containingGenes = cgff.getRelatedGenes(modelInterval.getChr(), modelExonSet, true, true, 1, true);
                for (Iterator geneIterator = containingGenes.iterator(); geneIterator.hasNext(); ) {
                    GenomeInterval geneRegion = (GenomeInterval) geneIterator.next();
                    String geneID = (String) geneRegion.getUserObject();
                    modelSpliceCounting(modelExonSet, geneID, cgff);
                }
            }
        }
    }

    private void modelSpliceCounting(Set intervalSet, String geneID, CanonicalGFF cgff) {
        Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        Set hitExonInSerial = new TreeSet();
        int exonSn = 1;
        for (Iterator exonIterator = exonRegions.iterator(); exonIterator.hasNext(); ) {
            Interval exonInterval = (Interval) exonIterator.next();
            for (Iterator intervalIterator = intervalSet.iterator(); intervalIterator.hasNext(); ) {
                Interval interval = (Interval) intervalIterator.next();
                if (exonInterval.contain(interval.getStart(), interval.getStop())) {
                    hitExonInSerial.add(new Integer(exonSn));
                    break;
                }
            }
            exonSn++;
        }
        if (hitExonInSerial.size() <= 1) return;
        Set spliceSet;
        if (geneSpliceMapByModel.containsKey(geneID)) {
            spliceSet = (Set) geneSpliceMapByModel.get(geneID);
        } else {
            spliceSet = new TreeSet();
            geneSpliceMapByModel.put(geneID, spliceSet);
        }
        Integer exonNo[] = (Integer[]) hitExonInSerial.toArray(new Integer[hitExonInSerial.size()]);
        for (int i = 0; i < exonNo.length - 1; i++) {
            Interval exonPair = new Interval(exonNo[i], exonNo[i + 1]);
            spliceSet.add(exonPair);
        }
    }

    private void spliceCounting(AlignmentRecord record, Map geneSpliceCntMap, Number count, String geneID, CanonicalGFF cgff, boolean checkByContaining, int minimumOverlap) {
        Set exonRegions = (Set) cgff.geneExonRegionMap.get(geneID);
        Set hitExonInSerial = new TreeSet();
        Map exonPairSplicingPosRec = new HashMap();
        int exonSn = 1;
        int lastHitExon = -1, lastHitBlock = -1;
        for (Iterator exonIterator = exonRegions.iterator(); exonIterator.hasNext(); ) {
            Interval exonInterval = (Interval) exonIterator.next();
            for (int i = 0; i < record.numBlocks; i++) {
                Interval blockTarget = new Interval(record.tStarts[i], record.tStarts[i] + record.tBlockSizes[i] - 1);
                if (checkByContaining) {
                    if (exonInterval.contain(blockTarget.getStart(), blockTarget.getStop()) == false) continue;
                    if (blockTarget.length() < minimumOverlap) continue;
                } else {
                    if (exonInterval.intersect(blockTarget) == false) continue;
                    int intersectStart = (blockTarget.getStart() > exonInterval.getStart()) ? blockTarget.getStart() : exonInterval.getStart();
                    int intersectStop = (blockTarget.getStop() < exonInterval.getStop()) ? blockTarget.getStop() : exonInterval.getStop();
                    if ((intersectStop - intersectStart + 1) < minimumOverlap) continue;
                }
                hitExonInSerial.add(new Integer(exonSn));
                if (lastHitExon > 0 && ((lastHitBlock + 1) == i)) {
                    Interval exonPair = new Interval(lastHitExon, exonSn);
                    exonPairSplicingPosRec.put(exonPair, record.qStarts[lastHitBlock] + record.qBlockSizes[lastHitBlock] - 1);
                }
                lastHitExon = exonSn;
                lastHitBlock = i;
                break;
            }
            exonSn++;
        }
        if (hitExonInSerial.size() <= 1) return;
        Map spliceCntMap;
        if (geneSpliceCntMap.containsKey(geneID)) {
            spliceCntMap = (Map) geneSpliceCntMap.get(geneID);
        } else {
            spliceCntMap = new TreeMap();
            geneSpliceCntMap.put(geneID, spliceCntMap);
        }
        Integer exonNo[] = (Integer[]) hitExonInSerial.toArray(new Integer[hitExonInSerial.size()]);
        for (int i = 0; i < exonNo.length - 1; i++) {
            Interval exonPair = new Interval(exonNo[i], exonNo[i + 1]);
            if (spliceCntMap.containsKey(exonPair)) {
                float oldValue = ((Number) spliceCntMap.get(exonPair)).floatValue();
                spliceCntMap.put(exonPair, new Float(oldValue + count.floatValue()));
            } else {
                spliceCntMap.put(exonPair, count);
            }
        }
        Map exonPairSplicingPosMap;
        if (geneExonPairSplicingPosMap.containsKey(geneID)) {
            exonPairSplicingPosMap = (Map) geneExonPairSplicingPosMap.get(geneID);
        } else {
            exonPairSplicingPosMap = new TreeMap();
            geneExonPairSplicingPosMap.put(geneID, exonPairSplicingPosMap);
        }
        for (Iterator iterator = exonPairSplicingPosRec.keySet().iterator(); iterator.hasNext(); ) {
            Interval exonPair = (Interval) iterator.next();
            int splicingPos = ((Integer) exonPairSplicingPosRec.get(exonPair)).intValue();
            Map splicingPosMap;
            if (exonPairSplicingPosMap.containsKey(exonPair)) {
                splicingPosMap = (Map) exonPairSplicingPosMap.get(exonPair);
            } else {
                splicingPosMap = new TreeMap();
                exonPairSplicingPosMap.put(exonPair, splicingPosMap);
            }
            if (splicingPosMap.containsKey(splicingPos)) {
                int val = ((Integer) splicingPosMap.get(splicingPos)).intValue() + 1;
                splicingPosMap.put(splicingPos, val);
            } else {
                splicingPosMap.put(splicingPos, 1);
            }
        }
    }

    public void countReadUnique(String readID, AlignmentRecord record, Number cnt, String geneID, CanonicalGFF cgff) {
        spliceCounting(record, geneSpliceCntMap, cnt, geneID, cgff, checkByContaining, minimumOverlap);
    }

    public void countReadMulti(String readID, Collection recordCollection, Number cnt, String geneID, CanonicalGFF cgff) {
    }

    public void report(String filename, CanonicalGFF cgff) {
        try {
            FileWriter fw = new FileWriter(new File(filename));
            fw.write("#GeneID" + "\t" + "exonPair" + "\t" + "#reads" + "\t" + "jumping");
            if (geneModel == null) {
            } else {
                fw.write("\t" + "novel");
            }
            fw.write("\t" + "splicingPosFreq");
            fw.write("\t" + "format:.spliceCount" + "\n");
            for (Iterator iterator = cgff.geneLengthMap.keySet().iterator(); iterator.hasNext(); ) {
                String geneID = (String) iterator.next();
                Map spliceCntMap = (Map) geneSpliceCntMap.get(geneID);
                Map exonPairSplicePosMap = (Map) geneExonPairSplicingPosMap.get(geneID);
                if (spliceCntMap == null) continue;
                Set modelSpliceSet;
                if (geneModel == null) {
                    modelSpliceSet = null;
                } else {
                    modelSpliceSet = (Set) geneSpliceMapByModel.get(geneID);
                }
                for (Iterator exonPairIterator = spliceCntMap.keySet().iterator(); exonPairIterator.hasNext(); ) {
                    Interval exonPair = (Interval) exonPairIterator.next();
                    float uniqCnt = ((Number) spliceCntMap.get(exonPair)).floatValue();
                    fw.write(geneID + "\t");
                    fw.write(exonPair.getStart() + "<=>" + exonPair.getStop() + "\t");
                    fw.write(new Float(uniqCnt).toString() + "\t");
                    if (exonPair.getStop() - exonPair.getStart() > 1) {
                        fw.write("V");
                    } else {
                        fw.write(" ");
                    }
                    if (geneModel == null) {
                    } else {
                        if (modelSpliceSet != null && modelSpliceSet.contains(exonPair) == false) {
                            fw.write("\t" + "V");
                        } else {
                            fw.write("\t" + " ");
                        }
                    }
                    if (exonPairSplicePosMap.containsKey(exonPair)) {
                        fw.write("\t" + exonPairSplicePosMap.get(exonPair));
                    } else {
                        fw.write("\t" + " ");
                    }
                    fw.write("\n");
                }
            }
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}

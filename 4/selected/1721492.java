package uqdsd.infosec.analysis;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.ProgressMonitor;
import uqdsd.infosec.GlobalProperties;
import uqdsd.infosec.analysis.DepthFirstPathsAnalysis.ArcTree;
import uqdsd.infosec.model.DataFlow;
import uqdsd.infosec.model.visitors.LiftingVisitor;

/**
 * @author InfoSec Project (c) 2008 UQ
 *
 * Given an ArcTree from the directed paths analysis, will find the minimal 
 * cutsets.
 */
public class MinimalArcCutsets {

    private DepthFirstPathsAnalysis.ArcTree atree;

    private Map<Integer, ArcTree> atreeIndex = new HashMap<Integer, ArcTree>();

    private Map<Integer, Integer> occurances = new HashMap<Integer, Integer>();

    private List<Integer> radix = new ArrayList<Integer>();

    private LiftingVisitor lifting;

    private DefaultListModel candidates = new DefaultListModel();

    public MinimalArcCutsets(LiftingVisitor lifting, DepthFirstPathsAnalysis.ArcTree atree) {
        super();
        this.atree = atree;
        this.lifting = lifting;
    }

    public void start(ProgressMonitor progress) {
        candidates.clear();
        atreeIndex.clear();
        occurances.clear();
        radix.clear();
        occuranceCounter();
        buildRadixVector();
        int radixArray[] = new int[radix.size()];
        for (int ii = 0; ii < radix.size(); ++ii) {
            radixArray[ii] = radix.get(ii);
        }
        final int CARD_LIMIT = Integer.parseInt(GlobalProperties.getProperty("Cutset.CardinalityLimit"));
        int[] theset = new int[CARD_LIMIT];
        long magiclimit = (long) Math.pow(radix.size() + 1, CARD_LIMIT) - 1;
        int percentComplete = 0;
        boolean cardinalityDecided = false;
        for (long magic = 0; magic >= 0 && magic < magiclimit; ++magic) {
            boolean optout = false;
            long scratch = magic;
            int index = 0;
            if (magic % 1000 == 0) {
                int newPercentComplete = (int) (((double) magic / (double) magiclimit) * 100);
                if (newPercentComplete > percentComplete) {
                    percentComplete = newPercentComplete;
                    progress.setProgress(percentComplete);
                }
            }
            do {
                int mod = (int) scratch % (radix.size() + 1);
                if (mod == 0) {
                    optout = true;
                    break;
                } else {
                    --mod;
                }
                for (int i = 0; i < index; ++i) {
                    if (theset[i] >= mod) {
                        optout = true;
                        break;
                    }
                }
                if (optout) {
                    break;
                }
                theset[index++] = mod;
                scratch = scratch / (radix.size() + 1);
            } while (scratch > 0);
            if (optout) {
                continue;
            }
            if (atree.isHashedCutset(radixArray, theset, index)) {
                if (!cardinalityDecided) {
                    magiclimit = (long) Math.pow(radix.size(), index) - 1;
                    cardinalityDecided = true;
                }
                Vector<ArcTree> candidate = new Vector<ArcTree>();
                for (int i = 0; i < index; i++) {
                    candidate.add(atreeIndex.get(radix.get(theset[i])));
                }
                candidates.add(candidates.getSize(), candidate);
            }
        }
        progress.setProgress(100);
    }

    public DefaultListModel getCandidates() {
        return candidates;
    }

    private void occuranceCounter() {
        Enumeration<?> ii = atree.children();
        while (ii.hasMoreElements()) {
            DepthFirstPathsAnalysis.ArcTree child = (DepthFirstPathsAnalysis.ArcTree) ii.nextElement();
            occuranceCounter(child);
            atreeIndex.put(new Integer(child.getLookup()), child);
        }
    }

    private int occuranceCounter(DepthFirstPathsAnalysis.ArcTree atree) {
        int totalChildCount = 1;
        Enumeration<?> ii = atree.children();
        while (ii.hasMoreElements()) {
            DepthFirstPathsAnalysis.ArcTree child = (DepthFirstPathsAnalysis.ArcTree) ii.nextElement();
            totalChildCount += occuranceCounter(child);
            atreeIndex.put(new Integer(child.getLookup()), child);
        }
        Integer lookup = new Integer(atree.getLookup());
        if (lifting.lookupArc(lookup).getChannelInformation() != DataFlow.CONFIRMED_DATA) {
            Integer count = occurances.get(lookup);
            if (count == null) {
                count = new Integer(0);
            } else {
                occurances.remove(lookup);
            }
            count = new Integer(count + totalChildCount);
            occurances.put(lookup, count);
        }
        return totalChildCount;
    }

    private void buildRadixVector() {
        Iterator<Map.Entry<Integer, Integer>> ii = occurances.entrySet().iterator();
        while (ii.hasNext()) {
            Map.Entry<Integer, Integer> toInsert = ii.next();
            int toInsertCount = toInsert.getValue();
            int nextRadixCount = 0;
            int radixIndex = 0;
            boolean winner = false;
            Iterator<Integer> jj = radix.iterator();
            while (jj.hasNext()) {
                Integer nextRadix = jj.next();
                nextRadixCount = occurances.get(nextRadix);
                radixIndex++;
                if (nextRadixCount < toInsertCount) {
                    winner = true;
                    break;
                }
            }
            if (winner) {
                if (radixIndex == 0) {
                    radix.add(0, toInsert.getKey());
                } else {
                    radix.add(radixIndex - 1, toInsert.getKey());
                }
            } else {
                radix.add(toInsert.getKey());
            }
        }
    }
}

package org.jmlspecs.jml4.esc.prover.simplify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.jmlspecs.jml4.esc.PostProcessor;
import org.jmlspecs.jml4.esc.gc.lang.KindOfAssertion;
import org.jmlspecs.jml4.esc.prover.CachedVcs;
import org.jmlspecs.jml4.esc.prover.ProverAdapter;
import org.jmlspecs.jml4.esc.result.lang.Result;
import org.jmlspecs.jml4.esc.util.Utils;
import org.jmlspecs.jml4.esc.vc.lang.VC;
import org.jmlspecs.jml4.esc.vc.lang.VcProgram;
import org.jmlspecs.jml4.util.Logger;

public class SimplifyAdapter extends ProverAdapter {

    private static final boolean DEBUG = false;

    private final String simplifyCMD;

    public SimplifyAdapter(CompilerOptions options, ProblemReporter problemReporter) {
        this(options, problemReporter, null);
    }

    public SimplifyAdapter(CompilerOptions options, ProblemReporter problemReporter, CachedVcs cachedVcs) {
        super(options, problemReporter, cachedVcs);
        simplifyCMD = options.jmlSimplifyPath;
    }

    public Result[] prove(VcProgram vc) {
        SimplifyVisitor visitor = new SimplifyVisitor();
        String simplifyString = vc.accept(visitor);
        Result[] retVal = prove(simplifyString);
        return retVal;
    }

    public Result[] prove(VC vc, Map incarnations) {
        SimplifyVisitor visitor = new SimplifyVisitor();
        String simplifyString = vc.accept(visitor);
        return prove(simplifyString);
    }

    public Result[] prove(String simplifyString) {
        if (DEBUG) {
            Logger.println("=== simplify ===");
            Logger.println(simplifyString);
        }
        String response = proveWithSimplify(simplifyString);
        if (DEBUG) Logger.print(response);
        Result[] retVal = formatResponse(response, simplifyString);
        return retVal;
    }

    private String proveWithSimplify(String simplifyString) {
        StringBuffer buffer = new StringBuffer();
        try {
            Process process = Runtime.getRuntime().exec(simplifyCMD);
            OutputStream out = process.getOutputStream();
            String ubp = SimplifyBackgroundPredicate.get();
            out.write(ubp.getBytes());
            out.write(simplifyString.getBytes());
            out.close();
            InputStream in = process.getInputStream();
            BufferedReader bIn = new BufferedReader(new InputStreamReader(in));
            String line;
            while (null != (line = bIn.readLine())) buffer.append(line + "\n");
            bIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    private static final String INVALID_REPONSE = "1: Invalid.";

    private static final String LABELS_MARKER = "labels: (";

    private static final String VALID_RESPONSE = "1: Valid.";

    public Result[] formatResponse(String fromProver, String simplifyString) {
        List retVal = new ArrayList();
        String result;
        if (fromProver.indexOf(VALID_RESPONSE) > 0) {
            result = VALID;
            retVal.add(new Result(result));
        } else if (fromProver.indexOf(INVALID_REPONSE) > 0) {
            if (fromProver.indexOf(LABELS_MARKER) > 0) {
                int labelsStart = fromProver.indexOf(LABELS_MARKER) + LABELS_MARKER.length();
                int labelsEnd = fromProver.indexOf(")", labelsStart);
                String line = fromProver.substring(labelsStart, labelsEnd);
                String[] labels = line.split(" ");
                List whatList = new ArrayList();
                int what = -1;
                while ((what = getWhat(labels, what + 1)) < labels.length) {
                    whatList.add(new Integer(what));
                }
                int[] whats = new int[whatList.size()];
                int i = 0;
                for (Iterator iterator = whatList.iterator(); iterator.hasNext(); ) {
                    what = ((Integer) iterator.next()).intValue();
                    whats[i++] = what;
                }
                sortWhats(labels, whats, simplifyString);
                getProblems(simplifyString, labels, whats, retVal);
                if (retVal.size() == 0) {
                    getProblems(simplifyString, labels, retVal);
                }
            } else {
                result = "unknown\t0\t0";
                retVal.add(new Result(result));
            }
        } else {
            result = "Error in Simplify input\t0\t0";
            Logger.print(fromProver);
            retVal.add(new Result(result));
        }
        return (Result[]) retVal.toArray(Result.EMPTY);
    }

    private void sortWhats(String[] labels, int[] whats, String simplifyString) {
        int n = whats.length;
        boolean swapped;
        do {
            swapped = false;
            for (int i = 0; i < n - 1; i++) {
                int i0_pos = simplifyString.indexOf(labels[whats[i]]);
                int i1_pos = simplifyString.indexOf(labels[whats[i + 1]]);
                if (i0_pos > i1_pos) {
                    int temp = whats[i];
                    whats[i] = whats[i + 1];
                    whats[i + 1] = temp;
                    swapped = true;
                }
            }
        } while (swapped);
    }

    private void getProblems(String simplifyString, String[] labels, int[] whats, List retVal) {
        String string = simplifyString;
        for (int i = whats.length - 1; i >= 0; i--) {
            String label = labels[whats[i]];
            int posLabel = string.indexOf(label);
            Utils.assertTrue(posLabel >= 0, "problem not found");
            int regionStart = string.lastIndexOf('(', posLabel);
            Utils.assertTrue(regionStart >= 0, "region start not found");
            int regionEnd = findRegionEnd(string, posLabel);
            Utils.assertTrue(regionEnd >= 0, "region end not found");
            String region = string.substring(regionStart, regionEnd + 1);
            Utils.assertTrue(string.length() == region.length() + string.substring(0, regionStart).length() + string.substring(regionEnd + 1).length(), "lengths not correct");
            string = string.substring(0, regionStart) + string.substring(regionEnd + 1);
            Result result = findResult(labels, whats[i], region);
            if (result != null) retVal.add(result);
        }
    }

    private void getProblems(String simplifyString, String[] labels, List retVal) {
        for (int i = 0; i < labels.length; i++) {
            Result result = labelToResult(simplifyString, labels[i]);
            retVal.add(result);
        }
    }

    private Result labelToResult(String simplifyString, String label) {
        String sWhere = label.substring(label.indexOf('@') + 1, label.length() - 1);
        String result = "Assert" + "\t" + sWhere.replace('_', '\t');
        return new Result(result);
    }

    private int findRegionEnd(String string, int startPos) {
        int retVal = startPos;
        int openParens = 1;
        while (openParens > 0) {
            retVal++;
            char c = string.charAt(retVal);
            if (c == ')') openParens--; else if (c == '(') openParens++;
        }
        return retVal;
    }

    private Result findResult(String[] labels, int what, String region) {
        String sWhat = getLabelName(labels[what]);
        String sWhere = getWhere(labels, region);
        if (sWhere == null) return null;
        String result = sWhat + "\t" + sWhere.replace('_', '\t');
        return new Result(result);
    }

    private String getLabelName(String label) {
        String retVal = label.split("@")[0].substring(1);
        return retVal;
    }

    private static final Set WHATS = new HashSet();

    static {
        KindOfAssertion[] all = KindOfAssertion.all();
        for (int i = 0; i < all.length; i++) {
            WHATS.add(all[i].description);
        }
    }

    private int getWhat(String[] labels, int startingPoint) {
        for (int i = startingPoint; i < labels.length; i++) {
            String what = getLabelName(labels[i]);
            if (WHATS.contains(what) && !labels[i].endsWith("@0|")) {
                return i;
            }
        }
        return labels.length + 1;
    }

    private static final Set IGNORED_LABEL_NAMES = new HashSet();

    static {
        IGNORED_LABEL_NAMES.addAll(WHATS);
        IGNORED_LABEL_NAMES.add("Assume");
        IGNORED_LABEL_NAMES.add("and");
        IGNORED_LABEL_NAMES.add("implies");
    }

    private String getWhere(String[] labels, String region) {
        int sourceStart = Integer.MAX_VALUE;
        int sourceEnd = 0;
        for (int i = 0; i < labels.length; i++) {
            if (region.indexOf(labels[i]) < 0) continue;
            String[] label = labels[i].substring(1, labels[i].length() - 1).split("@");
            String name = label[0];
            if (IGNORED_LABEL_NAMES.contains(name)) continue;
            String second = label[1];
            String[] pos = second.split("_");
            Utils.assertTrue(pos.length == 2, "malformed label: '" + labels[i] + "'");
            int labelStart = PostProcessor.parseInt(pos[0], 0);
            int labelEnd = PostProcessor.parseInt(pos[1], labelStart);
            if (labelStart == 0 && labelEnd == 0) continue;
            Utils.assertTrue(labelStart != 0 && labelEnd != 0, "only 1 is 0: '" + region + "'");
            if (labelStart < sourceStart) sourceStart = labelStart;
            if (sourceEnd < labelEnd) sourceEnd = labelEnd;
        }
        if (sourceStart == Integer.MAX_VALUE && sourceEnd == 0) {
            return null;
        }
        return "" + sourceStart + "_" + sourceEnd;
    }
}

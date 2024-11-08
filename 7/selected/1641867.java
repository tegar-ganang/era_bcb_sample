package edu.dimacs.mms.boxer.util;

import java.util.*;
import edu.dimacs.mms.boxer.*;

/** A Scores instance contains scoring data (obtained with a
 * particular learner) for the entire suite. This is a handy class in 
 * applications that keep track on the learner's performance on
 * a dataset.
 */
public class Scores extends Vector<ScoreEntry[]> {

    /** Log-likelihood and lin-likelyhood values for each discrimination */
    public double[] logLik, linLik;

    public int likCnt[];

    public Scores(Suite suite) {
        super();
        int ndis = suite.disCnt();
        logLik = new double[suite.disCnt()];
        linLik = new double[suite.disCnt()];
        likCnt = new int[suite.disCnt()];
    }

    /** Updates the numbers (TP etc) needed to calculate recall and
     * precision. This method also resizes the logLik arrays if
     * needed; their content can then be updated separately. */
    public void evalScores(DataPoint x, Suite suite, double[][] prob) {
        int ndis = suite.disCnt();
        if (logLik.length < ndis) logLik = Arrays.copyOf(logLik, ndis);
        if (linLik.length < ndis) linLik = Arrays.copyOf(linLik, ndis);
        if (likCnt.length < ndis) likCnt = Arrays.copyOf(likCnt, ndis);
        for (int i = 0; i < ndis; i++) {
            int r = suite.getDisc(i).claCount();
            if (i == size()) {
                ScoreEntry[] q = new ScoreEntry[r];
                for (int j = 0; j < r; j++) q[j] = new ScoreEntry();
                add(q);
            } else if (elementAt(i).length < r) {
                int r0 = elementAt(i).length;
                ScoreEntry[] q = Arrays.copyOf(elementAt(i), r);
                for (int j = r0; j < r; j++) q[j] = new ScoreEntry();
                set(i, q);
            }
        }
        Discrimination.Cla[] chosen = x.interpretScores(prob, suite);
        if (Suite.verbosity > 1) System.out.print("Classifier assigned " + x.getName() + " to:");
        for (int did = 0; did < chosen.length; did++) {
            ScoreEntry[] q = elementAt(did);
            if (chosen[did] == null) continue;
            int cpos = chosen[did].getPos();
            q[cpos].chosenCnt++;
            Discrimination dis = suite.getDisc(did);
            Discrimination.Cla trueC = x.claForDisc(dis);
            boolean correct = (chosen[did] == trueC);
            if (correct) {
                q[cpos].tpCnt++;
            }
            if (Suite.verbosity > 1) System.out.print(" " + chosen[did] + (correct ? " (C)" : " (I);"));
        }
        for (Discrimination.Cla c : x.getClasses(suite)) {
            elementAt(suite.getDid(c))[c.getPos()].oracleCnt++;
        }
        if (Suite.verbosity > 1) System.out.println();
    }

    /** Print a report on the quality of our classifier's scoring so far 
     @param prefix Append it to each line, for easy retrieval with 'grep'*/
    public String scoringReport(Suite suite, String prefix) {
        int disCnt = suite.disCnt();
        StringBuffer b = new StringBuffer();
        for (int k = 0; k < disCnt; k++) {
            ScoreEntry[] q = elementAt(k);
            Discrimination dis = suite.getDisc(k);
            for (int i = 0; i < q.length; i++) {
                b.append(prefix + dis.getClaById(i) + ": " + q[i].report() + "\n");
            }
        }
        return b.toString();
    }

    public String wAvgRecallReport(Suite suite, String prefix) {
        int disCnt = suite.disCnt();
        StringBuffer b = new StringBuffer();
        for (int k = 0; k < disCnt; k++) {
            ScoreEntry[] q = elementAt(k);
            Discrimination dis = suite.getDisc(k);
            if (likCnt[k] == 0) continue;
            b.append(prefix + "[" + dis.getName() + "] " + wAvgRecall(q) + "\n");
        }
        return b.toString();
    }

    /** Computes average weighted recall for one discrimination - the
     * measure proposed by Paul Kantor, 2009-11-01. Weighting is done
     * by class size. In other words, it's the average likelyhood of
     * an example being assigned to its right class.
     */
    double wAvgRecall(ScoreEntry[] q) {
        int sumC = 0, sumTP = 0;
        for (ScoreEntry se : q) {
            sumC += se.oracleCnt;
            sumTP += se.tpCnt;
        }
        return ((double) sumTP) / sumC;
    }

    private String likReport1(Suite suite, String prefix, double lik[]) {
        StringBuffer b = new StringBuffer();
        for (int j = 0; j < lik.length; j++) {
            if (likCnt[j] == 0) continue;
            double w = lik[j] / likCnt[j];
            b.append(prefix + "[" + suite.getDisc(j).getName() + "] " + w + "\n");
        }
        return b.toString();
    }

    public String loglikReport(Suite suite, String prefix) {
        return likReport1(suite, prefix, logLik);
    }

    public String linlikReport(Suite suite, String prefix) {
        return likReport1(suite, prefix, linLik);
    }

    /** Reports log-likelyhoods and linear likelyhood (in this order)
     * on the same line */
    public String likReport2(Suite suite, String prefix) {
        StringBuffer b = new StringBuffer();
        for (int j = 0; j < likCnt.length; j++) {
            if (likCnt[j] == 0) continue;
            double wLin = linLik[j] / likCnt[j];
            double wLog = logLik[j] / likCnt[j];
            b.append(prefix + "[" + suite.getDisc(j).getName() + "] " + wLog + " " + wLin + "\n");
        }
        return b.toString();
    }

    public void deleteDiscr(int delDid) {
        removeElementAt(delDid);
        logLik = deleteElement(logLik, delDid);
        linLik = deleteElement(linLik, delDid);
        likCnt = deleteElement(likCnt, delDid);
    }

    /** Removes an element from an array */
    static double[] deleteElement(double a[], int del) {
        if (del >= a.length) throw new IllegalArgumentException("Del index " + del + " out of range");
        double b[] = Arrays.copyOf(a, a.length - 1);
        for (int i = del; i < b.length; i++) b[i] = a[i + 1];
        return b;
    }

    static int[] deleteElement(int a[], int del) {
        if (del >= a.length) throw new IllegalArgumentException("Del index " + del + " out of range");
        int b[] = Arrays.copyOf(a, a.length - 1);
        for (int i = del; i < b.length; i++) b[i] = a[i + 1];
        return b;
    }
}

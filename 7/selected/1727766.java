package de.fraunhofer.isst.axbench.timing.model.special;

import java.util.Map;
import de.fhg.isst.vts.vts_lib.tools.ChecksumOnlyStream;
import de.fraunhofer.isst.axbench.timing.model.Task;
import de.fraunhofer.isst.axbench.timing.model.Transaction;

public class TransactionResult extends Transaction {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7494732345359873922L;

    private TaskResult taur[] = null;

    private double tmax;

    public TransactionResult(int fac, double tmax, Transaction gammai, int p0) {
        super(gammai);
        this.tmax = tmax;
        Task[] tasks = gammai.tau;
        taur = new TaskResult[fac * tasks.length];
        double t = gammai.getT();
        int i = 0;
        for (int p = 0; p < fac; p++) {
            for (Task task : tasks) {
                taur[i] = new TaskResult(task, p * t, p + p0);
                i++;
            }
        }
        tau = taur;
    }

    public TransactionResult(TransactionResult toCopy) {
        super(toCopy);
        TaskResult taurx[] = toCopy.taur;
        taur = new TaskResult[taurx.length];
        tmax = toCopy.tmax;
        int i = 0;
        for (TaskResult tauinst : taurx) {
            taur[i] = tauinst.copy();
            i++;
        }
        tau = taur;
    }

    public void shiftTo(double shift) {
        for (TaskResult task : taur) {
            if (task != null) {
                task.setT0(task.getT0() + shift);
                task.o = task.getT0();
            }
        }
    }

    public void jitterShift() {
        int negativ = 0;
        for (TaskResult task : taur) {
            if (task != null) {
                if (task.getT0() >= 0.0) {
                    task.setT1(task.getT0());
                } else {
                    if (task.getT0() + task.j >= 0.0) {
                        task.setT1(0.0);
                    } else {
                        task.setT1(task.getT0());
                        negativ++;
                    }
                }
                task.o = task.getT1();
            }
        }
        if (negativ > 0) {
            TaskResult[] tau2 = taur;
            taur = new TaskResult[taur.length - negativ];
            for (int i = 0, j = 0; i < tau2.length; i++) {
                if (tau2[i].getT1() >= 0) {
                    taur[j] = tau2[i];
                    j++;
                }
            }
            tau = taur;
        }
        int ir = 0;
        while (taur[taur.length - 1 - ir].getT1() > tmax) {
            ir++;
        }
        if (ir > 0) {
            TaskResult[] tau2 = taur;
            taur = new TaskResult[tau2.length - ir];
            for (int i = 0; i < tau2.length - ir; i++) {
                taur[i] = tau2[i];
            }
            tau = taur;
        }
    }

    public void addsort(TransactionResult tr1) {
        TaskResult[] tau1 = taur;
        TaskResult[] tau2 = tr1.taur;
        if (tau1 == null) {
            taur = new TaskResult[tau2.length];
            for (int i = 0; i < tau2.length; i++) {
                taur[i] = tau2[i];
            }
        } else {
            taur = new TaskResult[tau1.length + tau2.length];
            int i1 = 0;
            int i2 = 0;
            int i = 0;
            while (i1 < tau1.length && i2 < tau2.length && i < taur.length) {
                if (tau1[i1].getT1() < tau2[i2].getT1()) {
                    taur[i] = tau1[i1];
                    i++;
                    i1++;
                } else {
                    if (tau1[i1].getT1() > tau2[i2].getT1()) {
                        taur[i] = tau2[i2];
                        i++;
                        i2++;
                    } else {
                        if (tau1[i1].p >= tau2[i2].p) {
                            taur[i] = tau1[i1];
                            i++;
                            i1++;
                        } else {
                            if (tau1[i1].p < tau2[i2].p) {
                                taur[i] = tau2[i2];
                                i++;
                                i2++;
                            }
                        }
                    }
                }
            }
            if (i1 < tau1.length) {
                for (int j = i1; j < tau1.length; j++, i++) {
                    taur[i] = tau1[j];
                }
            } else {
                for (int j = i2; j < tau2.length; j++, i++) {
                    taur[i] = tau2[j];
                }
            }
        }
        int ir = 0;
        while (taur[taur.length - 1 - ir].getT1() > tmax) {
            ir++;
        }
        if (ir > 0) {
            tau2 = taur;
            taur = new TaskResult[tau2.length - ir];
            for (int i = 0; i < tau2.length - ir; i++) {
                taur[i] = tau2[i];
            }
        }
        tau = taur;
    }

    public void sortandshift() {
        TaskResult taux;
        int i = 0;
        while (i < taur.length) {
            int j = i + 1;
            while (j < taur.length) {
                if (taur[j] != null) {
                    if (taur[i].getT1() + taur[i].c > taur[j].getT1()) {
                        if (taur[i].p > taur[j].p) {
                            if (taur[j].getT1() < 0) {
                                taur[j] = null;
                            } else {
                                taur[j].setT1(taur[i].getT1() + taur[i].c);
                            }
                            this.sort(j);
                        } else {
                            if (taur[i].getT1() == taur[j].getT1()) {
                                if ((taur[i].p == taur[j].p) && taur[i].name.equals(taur[j].name)) {
                                    if (taur[i].getInst() > taur[j].getInst()) {
                                        int ins0 = taur[i].getInst();
                                        taur[i].setInst(taur[j].getInst());
                                        taur[j].setInst(ins0);
                                        double t0 = taur[i].getT0();
                                        taur[i].setT0(taur[j].getT0());
                                        taur[j].setT0(t0);
                                    }
                                    taur[j].setT1(taur[j].getT1() + taur[i].c);
                                    this.sort(j);
                                } else {
                                    taur[j].setT1(taur[j].getT1() + taur[i].c);
                                    this.sort(j);
                                }
                            } else {
                                if (taur[i].p == taur[j].p) {
                                    if (taur[i].getInst() < taur[j].getInst()) {
                                        taur[j].setT1(taur[i].getT1() + taur[i].c);
                                        this.sort(j);
                                    } else {
                                        System.err.println(i + " tauri = " + taur[i]);
                                        System.err.println(j + " taurj = " + taur[j]);
                                    }
                                } else {
                                    double c0 = taur[i].c;
                                    taur[i].c = taur[j].getT1() - taur[i].getT1();
                                    double c2 = c0 - taur[i].c;
                                    double t2 = taur[j].getT1() + taur[j].c;
                                    taux = new TaskResult(taur[i], c2, t2);
                                    this.addsort(taux, j);
                                    j++;
                                }
                            }
                        }
                    } else {
                        j++;
                    }
                } else {
                    j++;
                }
            }
            if (taur[i] != null) {
                taur[i].setT2(taur[i].getT1());
                taur[i].setT1(Double.MAX_VALUE);
                taur[i].o = taur[i].getT2();
                taur[i].j = 0.0;
            }
            i++;
        }
        this.name = "Resultat als Transaktion";
        i = taur.length - 1;
        while (taur[i] == null) {
            i--;
        }
        int nullcount = taur.length - 1 - i;
        this.t = taur[i].getT2() + taur[i].c;
        if (nullcount > 0) {
            TaskResult[] tau2 = taur;
            taur = new TaskResult[tau2.length - nullcount];
            for (int ii = 0; ii < tau2.length - nullcount; ii++) {
                taur[ii] = tau2[ii];
            }
            tau = taur;
        }
    }

    private void sort(int jc) {
        TaskResult taux;
        int length = taur.length;
        for (int i = jc; i < length; i++) {
            if (taur[i] == null) {
                int i2;
                for (i2 = i; i2 < length - 1; i2++) {
                    taur[i2] = taur[i2 + 1];
                    taur[i2 + 1] = null;
                }
                length--;
            } else {
                for (int j = i + 1; j < length; j++) {
                    if (taur[j] != null) {
                        if (taur[i].getT1() > taur[j].getT1()) {
                            taux = taur[i];
                            taur[i] = taur[j];
                            taur[j] = taux;
                        } else {
                            if (taur[i].getT1() == taur[j].getT1()) {
                                if (taur[i].p < taur[j].p) {
                                    taux = taur[i];
                                    taur[i] = taur[j];
                                    taur[j] = taux;
                                } else {
                                    if (taur[i].p == taur[j].p) {
                                        if (taur[i].getInst() > taur[j].getInst()) {
                                            taux = taur[i];
                                            taur[i] = taur[j];
                                            taur[j] = taux;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addsort(TaskResult taux, int jc) {
        TaskResult[] tau1 = taur;
        taur = new TaskResult[tau1.length + 1];
        for (int j = 0; j < jc; j++) {
            taur[j] = tau1[j];
        }
        int i1 = jc;
        int i = jc;
        boolean weiter = true;
        while (i1 < tau1.length && i < taur.length && weiter) {
            if (tau1[i1].getT1() < taux.getT1()) {
                taur[i] = tau1[i1];
                i++;
                i1++;
            } else {
                if (tau1[i1].getT1() > taux.getT1()) {
                    taur[i] = taux;
                    i++;
                    weiter = false;
                } else {
                    if (tau1[i1].p > taux.p) {
                        taur[i] = tau1[i1];
                        i++;
                        i1++;
                    } else {
                        taur[i] = taux;
                        i++;
                        weiter = false;
                    }
                }
            }
        }
        if (i1 < tau1.length) {
            for (int j = i1; j < tau1.length; j++, i++) {
                taur[i] = tau1[j];
            }
        }
        tau = taur;
    }

    public String toString() {
        String ret = name + ": ";
        ret += "t (Periode) = " + t + "\n";
        for (int i = 0; i < tau.length; i++) {
            if (tau[i] != null) ret += tau[i].toString() + "\n";
        }
        return ret;
    }

    public String toTiKZ(int xstart, double xmax, double ymax, Map<String, String> assocol) {
        String res = "";
        res += "\\begin{tikzpicture}\n\t\\xyaxisXshifted{" + xstart + "}{" + xmax + "}{" + ymax + "}\n";
        for (int j = 0; j < taur.length; j++) {
            if (taur[j] != null) {
                if (taur[j].o >= xstart) {
                    if (taur[j].o < (xstart + xmax)) {
                        res += "\t\\obtask{" + taur[j].c + "}{" + taur[j].p + "}{" + assocol.get(taur[j].name) + "}{" + taur[j].o + "}\n";
                    }
                }
            }
        }
        res += "\\end{tikzpicture}\n";
        return res;
    }

    public String toTiKZInst(int xstart, double xmax, double ymax, Map<String, String> assocol) {
        String res = "";
        res += "\\begin{tikzpicture}\n\t\\xyaxisXshifted{" + xstart + "}{" + xmax + "}{" + ymax + "}\n";
        for (int j = 0; j < taur.length; j++) {
            if (taur[j] != null) {
                if (taur[j].o >= xstart) {
                    if (taur[j].o < (xstart + xmax)) {
                        res += "\t\\obtask{" + taur[j].c + "}{" + taur[j].p + "}{" + assocol.get(taur[j].name) + "}{" + taur[j].o + "}\n";
                        res += "\t\\node[above] at (" + ((taur[j].o / 10.) + 0.25) + "," + (taur[j].p / 10.) + ") {\\footnotesize{" + taur[j].getInst() + ".}};\n";
                    }
                }
            }
        }
        res += "\\end{tikzpicture}\n";
        return res;
    }

    public String toTiKZInst(double d, double xmax, double ymax, Map<String, String> assocol, Map<String, Boolean> associnst) {
        String res = "";
        res += "\\begin{tikzpicture}\n\t\\xyaxisXshifted{" + new Double(d).intValue() + "}{" + new Double(xmax).intValue() + "}{" + new Double(ymax).intValue() + "}\n";
        for (int j = 0; j < taur.length; j++) {
            if (taur[j] != null) {
                if (taur[j].o >= d) {
                    if (taur[j].o < (d + xmax)) {
                        res += "\t\\obtask{" + taur[j].c + "}{" + taur[j].p + "}{" + assocol.get(taur[j].name) + "}{" + taur[j].o + "}\n";
                        if (associnst.containsKey(taur[j].name)) {
                            res += "\t\\node[above] at (" + ((taur[j].o / 10.) + 0.25) + "," + (taur[j].p / 10.) + ") {\\footnotesize{" + taur[j].getInst() + ".}};\n";
                        }
                    }
                }
            }
        }
        res += "\\end{tikzpicture}\n";
        return res;
    }

    public TransactionResult copy() {
        TransactionResult copyres = new TransactionResult(this);
        return copyres;
    }

    public final long getChecksum() {
        double[][] results = toArray();
        ChecksumOnlyStream cSt = new ChecksumOnlyStream(results);
        return cSt.getChecksumAsLong();
    }

    @Deprecated
    private final double[][] toArray() {
        double[][] ret = new double[taur.length + 1][];
        ret[0] = new double[2];
        ret[0][0] = name.hashCode();
        ret[0][1] = t;
        for (int i = 1; i < taur.length; i++) {
            TaskResult tauI = taur[i];
            ret[i] = new double[9];
            ret[i][0] = tauI.name.hashCode();
            ret[i][1] = tauI.c;
            ret[i][2] = tauI.o;
            ret[i][3] = tauI.j;
            ret[i][4] = tauI.p;
            ret[i][5] = tauI.getT0();
            ret[i][6] = tauI.getT2();
            ret[i][7] = tauI.getInst();
            ret[i][8] = tauI.getInstpart();
        }
        return ret;
    }
}

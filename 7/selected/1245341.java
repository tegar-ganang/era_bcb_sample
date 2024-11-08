package fr.msimeon.mads.runModel.calc;

import java.util.ArrayList;
import org.eclipse.jface.text.IDocument;
import fr.msimeon.mads.runModel.math.Complex;
import fr.msimeon.mads.runModel.math.Polynomial;
import fr.msimeon.mads.runModel.math.RootException;

public class Functions {

    public static TimeSeries read(String vName, String table, String fSpec, int pLife) {
        TimeSeries res = new TimeSeries(pLife);
        res = InOut.importTS(vName, table, fSpec, pLife);
        return res;
    }

    public static double irr(ArrayList<TimeSeries> vars, ArrayList<String> varLabels, Double oCC, String label, int pLife, IDocument doc, Boolean isHtml, Boolean export, String fSpec, String worksheet, String[] headers) {
        double rate = Double.NEGATIVE_INFINITY;
        Object result = null;
        int nbVars = vars.size();
        headers[3] = label;
        if (nbVars == 1) {
            result = getIRR(vars.get(0).getValues());
            double npv = netPresentValue(vars.get(0).getValues(), oCC);
            InOut.listIRR(label, npv, oCC, result, doc, isHtml);
            if (export) InOut.exportIRR(headers, npv, oCC, result, fSpec, worksheet);
        } else {
            TimeSeries balance = new TimeSeries(pLife);
            double[] npv = new double[nbVars];
            for (int i = 0; i < nbVars; i++) {
                balance = balance.addValues(vars.get(i));
                npv[i] = netPresentValue(vars.get(i).getValues(), oCC);
            }
            double balNPV = netPresentValue(balance.getValues(), oCC);
            result = getIRR(balance.getValues());
            String[] vlabels = varLabels.toArray(new String[varLabels.size()]);
            if (doc != null) InOut.listIRR(label, npv, vlabels, oCC, balNPV, result, doc, isHtml);
            if (export) InOut.exportIRR(headers, npv, vlabels, oCC, balNPV, result, fSpec, worksheet);
        }
        if (result instanceof Double) rate = (Double) result;
        return rate;
    }

    static double netPresentValue(double[] var, Double oCC) {
        double npv = 0.0;
        double x = 1 / (1 + (oCC / 100));
        double factor = x;
        for (int i = 1; i < var.length; i++) {
            npv = npv + var[i] * factor;
            factor = factor * x;
        }
        return npv;
    }

    static Object getIRR(double[] var) {
        Complex[] zeros = null;
        int pLife = var.length - 1;
        int notNull = pLife;
        while ((var[notNull] == 0.0) && (notNull > 0)) {
            notNull -= 1;
        }
        double[] newVar = new double[notNull];
        for (int i = 0; i < notNull; i++) {
            newVar[i] = var[i + 1];
        }
        Polynomial poly = new Polynomial(newVar);
        try {
            zeros = poly.zeros();
        } catch (RootException e) {
            System.out.println(e.getMessage());
        }
        ArrayList<Double> realRates = new ArrayList<Double>();
        if (zeros != null) {
            for (int i = 0; i < zeros.length; i++) {
                if ((zeros[i].im() == 0.0) && (zeros[i].re() != 0.0)) {
                    Double IRR;
                    IRR = 100 * (1 - zeros[i].re()) / zeros[i].re();
                    realRates.add(IRR);
                }
            }
            if (realRates.size() == 1) {
                return realRates.get(0);
            } else if (realRates.size() > 1) {
                return "More than one rate found: rate of return not applicable";
            } else return "No rate of return could be found";
        } else return "No roots could be found: rate of return not applicable";
    }

    public static TimeSeries purchase(TimeSeries itemCons, TimeSeries itemProd, int pLife) {
        TimeSeries res = new TimeSeries(pLife);
        TimeSeries res2 = new TimeSeries(pLife);
        res = res.addValues(itemCons);
        res = res.substractValues(itemProd);
        res2 = (res.ifMinus()).shift(1);
        return res.addValues(res2);
    }

    public static TimeSeries resVal(TimeSeries itemInv, int life, double rvRate, int pLife) {
        TimeSeries res = new TimeSeries(pLife);
        double inv, resval;
        double rate = rvRate / 100;
        for (int i = 1; i <= pLife; i++) {
            inv = -itemInv.getValues()[i];
            if ((i + life) <= pLife) {
                res.addValue(inv * rate, i + life);
            } else {
                int left = life - (pLife - i + 1);
                double perCentLeft = (1.0 - rate) * ((double) left / (double) life);
                resval = inv * (rate + perCentLeft);
                res.addValue(resval, pLife);
            }
        }
        return res;
    }

    public static TimeSeries opCost(TimeSeries itemInv, int life, int delay, double rate, int pLife) {
        TimeSeries res = new TimeSeries(pLife);
        double inv;
        int index;
        for (int i = 1; i <= pLife; i++) {
            inv = itemInv.getValues()[i];
            index = Math.min(pLife, i + life - 1);
            for (int j = i + delay; j <= index; j++) {
                res.addValue((inv * (rate / 100)), j);
            }
        }
        return res;
    }

    public static TimeSeries debtService(TimeSeries itemLoan, int duration, int grace, int graceOnInt, double rate, boolean isEndYr, boolean isSeparateLoans, int pLife) {
        TimeSeries res = new TimeSeries(pLife);
        double amount;
        int graceI, graceP;
        double rateFactor = 1.0 + (rate / 100.0);
        int first = 1;
        int last = pLife;
        while ((first <= pLife) && (itemLoan.getValues()[first] == 0)) first++;
        while ((last >= 1) && (itemLoan.getValues()[last] == 0)) last--;
        if (grace < (last - first)) {
            grace = last - first;
            System.out.printf("Grace period set to %1d to fit loan installments%n", grace);
        }
        int repayP = duration - grace;
        if (repayP < 1) {
            System.out.printf("Grace and duration do not fit loan installments%n-->> results set to zero%n");
        }
        if (graceOnInt > grace) {
            graceOnInt = grace;
            System.out.printf("Grace period on interest set to %1d to fit grace%n", graceOnInt);
        }
        for (int i = first; i <= last; i++) {
            if (itemLoan.getValues()[i] < 0) {
                System.out.println("AMOUNT OF LOAN IS NEGATIVE IN YEAR " + i + "\n-->> LOAN WILL BE ASSUMED AS ZERO FOR YEAR " + i);
                return res;
            }
        }
        for (int i = first; i <= last; i++) {
            double[] debt = new double[pLife + 1];
            double[] outst = new double[pLife + 1];
            double PV;
            double PMT;
            int n, gI, gP;
            amount = itemLoan.getValues()[i];
            if (isSeparateLoans) {
                graceI = graceOnInt;
                graceP = grace;
            } else {
                graceI = graceOnInt - (i - first);
                if (graceI < 0) graceI = 0;
                graceP = grace - (i - first);
            }
            gI = i + graceI - 1;
            if (gI > pLife) gI = pLife;
            gP = i + graceP - 1;
            if (gP > pLife) gP = pLife;
            n = gP + repayP;
            if (n > pLife) n = pLife;
            for (int j = i; j < (gI + 1); j++) {
                outst[j] = amount * Math.pow(rateFactor, (j - i + 1));
            }
            for (int j = (gI + 1); j < (gP + 1); j++) {
                outst[j] = outst[j - 1];
                if (j == i) outst[j] = amount;
                debt[j] = outst[j] * rate / 100.0;
            }
            if (graceP > 0) {
                PV = outst[gP];
            } else PV = amount;
            if (rate == 0.0) {
                PMT = PV / repayP;
            } else PMT = PV * rate / 100 / (1 - Math.pow(rateFactor, -repayP));
            for (int j = (gP + 1); j <= n; j++) {
                debt[j] = PMT;
                PV = PV * rateFactor - PMT;
                outst[j] = PV;
            }
            if (isEndYr) {
                for (int j = pLife; j > 0; j--) {
                    debt[j] = debt[j - 1];
                    outst[j] = outst[j - 1];
                }
            }
            for (int j = 1; j <= pLife; j++) {
                res.addValue(debt[j], j);
            }
        }
        return res;
    }

    public static TimeSeries outstanding(TimeSeries itemLoan, double rate, boolean isEndYr, TimeSeries debtService, int pLife) {
        TimeSeries res = new TimeSeries(pLife);
        double[] outst = new double[pLife + 1];
        double rateFactor = 1 + (rate / 100);
        for (int i = 1; i <= pLife; i++) {
            if (isEndYr) {
                outst[i] = itemLoan.getValues()[i] + outst[i - 1] * rateFactor - debtService.getValues()[i];
            } else outst[i] = (outst[i - 1] + itemLoan.getValues()[i]) * rateFactor - debtService.getValues()[i];
            res.addValue(outst[i], i);
        }
        return res;
    }
}

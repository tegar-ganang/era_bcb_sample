package langnstats.project.languagemodel.srilm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import langnstats.project.languagemodel.srilm.SriLMParameters.DiscountingMethod;
import langnstats.project.lib.LanguageModel;
import langnstats.project.lib.WordType;
import langnstats.project.lib.crossvalidation.TrainTokens;

/**
 * No Training! Read the ARPA LM file and output 
 * @author qing
 *
 */
public class NGramLanguageModel implements LanguageModel {

    private Map<String, NGram> unigram = new HashMap<String, NGram>();

    private String[] history = null;

    private int historyLen = 0;

    private boolean sentStart = true;

    private int maxOrder = 0;

    private DiscountingMethod method;

    private int[] cutoff;

    private int[] maxdisc;

    /**
	 * 
	 */
    private static final long serialVersionUID = -3417866810770792647L;

    public String getDescription() {
        String str = "" + order + "GRAM Lanugage Model(" + method.name() + "):";
        for (int i = 0; i < order; i++) {
            str += "[" + i + ":" + cutoff[i] + "-" + maxdisc[i] + "]~";
        }
        return str;
    }

    private double predictSingle(String[] hist, int start, int len, String pred) {
        NGram base = unigram.get(hist[start]);
        if (base == null) {
            return -99;
        }
        double bow = 0;
        for (int i = start + 1; i < len; i++) {
            NGram nbase = base.getChildNGram(hist[i]);
            if (nbase == null) {
                bow += base.getDisc();
                double value = predictSingle(hist, i, len - i, pred);
                return bow + value;
            } else {
                base = nbase;
            }
        }
        return base.getProb();
    }

    private double[] predictAll(String[] hist, int len) {
        NGram base = unigram.get(hist[0]);
        if (base == null) {
            return null;
        }
        double bow = 0;
        for (int i = 1; i < len; i++) {
            NGram nbase = base.getChildNGram(hist[i]);
            if (nbase == null) {
                bow += base.getDisc();
                base = unigram.get(hist[i]);
                if (base == null) {
                    return null;
                }
            } else {
                base = nbase;
            }
        }
        double[] result = new double[WordType.vocabularySize()];
        for (int i = 0; i < WordType.vocabularySize(); i++) {
            String pred = WordType.values()[i].getOriginalTag();
            NGram ng = base.getChildNGram(pred);
            if (ng != null) {
                result[i] = bow + Math.exp(ng.getProb());
            } else {
                if (base.getOrder() < 2) {
                    ng = unigram.get(pred);
                    if (ng == null) {
                        result[i] = 1e-50;
                    } else {
                        result[i] = Math.exp(bow + base.getDisc() + ng.getProb());
                    }
                } else {
                    ng = unigram.get(hist[len - 1]);
                    if (ng == null) {
                        result[i] = 1e-50;
                    } else {
                        NGram nng = ng.getChildNGram(pred);
                        if (nng == null) {
                            nng = unigram.get(pred);
                            result[i] = Math.exp(bow + base.getDisc() + ng.getDisc() + nng.getProb());
                        } else {
                            result[i] = Math.exp(bow + base.getDisc() + nng.getProb());
                        }
                    }
                }
            }
        }
        double coll = 0;
        for (int i = 0; i < result.length; i++) {
            coll += result[i];
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= coll;
        }
        return result;
    }

    public double[] predict(WordType wordType) {
        if (maxOrder == 1) {
            double[] res = new double[WordType.vocabularySize()];
            double sum = 0;
            for (int i = 0; i < WordType.vocabularySize(); i++) {
                String pred = WordType.values()[i].getOriginalTag();
                res[i] = Math.exp(unigram.get(pred).getProb());
                sum += res[i];
            }
            for (int i = 0; i < WordType.vocabularySize(); i++) {
                res[i] /= sum;
            }
            return res;
        }
        if (history == null) {
            history = new String[maxOrder - 1];
        }
        if (wordType == WordType.PERIOD || wordType == null) {
            history[0] = "<s>";
            historyLen = 1;
        } else {
            for (int i = 0; i < historyLen - 1; i++) {
                history[i] = history[i + 1];
            }
            if (historyLen == history.length) {
                history[historyLen - 1] = wordType.getOriginalTag();
            } else {
                history[historyLen] = wordType.getOriginalTag();
                historyLen++;
            }
        }
        double[] res = predictAll(history, historyLen);
        return res;
    }

    private static String tempFile = "/tmp/lm_train";

    public void train(TrainTokens trainTokens) {
        try {
            SriLM sri = new SriLM(tempFile, order, cutoff, maxdisc, method);
            sri.train(trainTokens);
            this.loadLM(new File(tempFile + ".lm"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return;
    }

    public LanguageModel clone() {
        NGramLanguageModel lm = new NGramLanguageModel();
        lm.unigram = unigram;
        lm.method = method;
        lm.order = order;
        lm.cutoff = cutoff;
        lm.maxdisc = maxdisc;
        lm.maxOrder = maxOrder;
        return lm;
    }

    private NGramLanguageModel() {
    }

    private int order;

    public NGramLanguageModel(DiscountingMethod discount, int order, int[] cutoff, int[] maxdisc) {
        this.method = discount;
        this.order = order;
        this.cutoff = cutoff;
        this.maxdisc = maxdisc;
    }

    private NGram _searchNGram(NGram base, String[] wt, int start) {
        if (start == wt.length - 1) {
            return base.getChildNGram(wt[start]);
        } else {
            NGram n = base.getChildNGram(wt[start]);
            if (n != null) {
                return _searchNGram(n, wt, start + 1);
            } else {
                return base;
            }
        }
    }

    public NGram searchNGram(String[] wt) {
        if (wt.length == 1) {
            return unigram.get(wt[0]);
        } else {
            return _searchNGram(unigram.get(wt[0]), wt, 1);
        }
    }

    public NGram searchMGram(String[] wt) {
        if (wt.length == 2) {
            return unigram.get(wt[0]);
        } else {
            return _searchMGram(unigram.get(wt[0]), wt, 1);
        }
    }

    private NGram _searchMGram(NGram base, String[] wt, int start) {
        if (start == wt.length - 2) {
            return base.getChildNGram(wt[start]);
        } else {
            NGram n = base.getChildNGram(wt[start]);
            if (n != null) {
                return _searchMGram(n, wt, start + 1);
            } else {
                return base;
            }
        }
    }

    public NGram insertNgram(String[] wt, double prob, double bow) {
        if (wt.length == 1) {
            NGram n = new NGram();
            n.setOrder(1);
            n.setHistory(null);
            n.setDisc(bow);
            n.setProb(prob);
            unigram.put(wt[0], n);
            return n;
        }
        NGram base = searchMGram(wt);
        int order = base.getOrder();
        if (order == wt.length) {
            base.setDisc(bow);
            base.setProb(prob);
            return base;
        } else {
            if (order < wt.length - 1) {
                return null;
            } else {
                return base.putChildNGram(wt[order], prob, bow);
            }
        }
    }

    private void loadLM(File arpa) throws NumberFormatException, IOException {
        if (!arpa.exists()) {
            return;
        }
        BufferedReader rd = new BufferedReader(new FileReader(arpa.getAbsolutePath()));
        String str;
        int order = 100;
        String[] typeBuffer = null;
        while ((str = rd.readLine()) != null) {
            if (str.length() == 0) {
                continue;
            }
            if (str.charAt(0) == '\\') {
                String p = str.substring(1, 2);
                try {
                    order = Integer.parseInt(p);
                } catch (Throwable e) {
                    continue;
                }
                if (order == 0) continue;
                typeBuffer = new String[order];
                continue;
            }
            String[] slots = str.split("\\s+");
            if (slots.length > order) {
                double prob = Double.parseDouble(slots[0]);
                for (int i = 0; i < order; i++) {
                    typeBuffer[i] = slots[i + 1];
                }
                double bow = 0;
                if (slots.length > order + 1) {
                    bow = Double.parseDouble(slots[order + 1]);
                }
                insertNgram(typeBuffer, prob / log10e, bow / log10e);
            }
        }
        maxOrder = order;
    }

    static double log10e = Math.log10(Math.E);

    public NGramLanguageModel(File arpa) throws IOException {
        loadLM(arpa);
    }

    public static void main(String args[]) throws IOException {
        NGramLanguageModel ng = new NGramLanguageModel(new File(args[0]));
        BufferedReader rd = new BufferedReader(new FileReader(args[1]));
        String line;
        double sum = 0;
        int count = 0;
        while ((line = rd.readLine()) != null) {
            WordType curr = WordType.get(line);
            double[] res = ng.predict(curr);
            for (int i = 0; i < res.length; i++) {
                System.out.print(WordType.values()[i].getOriginalTag());
                System.out.print(":");
                System.out.print(res[i]);
                System.out.print("|");
                sum += Math.log(res[curr.getIndex()]);
                count += 1;
            }
            System.out.println(line);
        }
        System.out.println("PPL: " + 1 / (Math.exp(sum / count)));
    }

    public void prepare(WordType[] allWordType) {
    }

    public DiscountingMethod getMethod() {
        return method;
    }

    public void setMethod(DiscountingMethod method) {
        this.method = method;
    }
}

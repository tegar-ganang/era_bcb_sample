package agex.traders;

import agex.db.QuoteTO;
import agex.sim.SimClock;
import agex.to.OrderTO;
import agex.to.OrdersTO;

public class PriceOscillator extends MultiAssetTrader {

    private static final int SHORT_PERIOD = 10;

    private static final int LONG_PERIOD = 30;

    private static final int VOLUME = 10000;

    private double dias[][];

    public void setupTrader() {
        super.setupTrader();
        try {
            dias = new double[papers.length][LONG_PERIOD];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double preco;

    double last_preco;

    static int NUM_ESTADOS = 101;

    static int NUM_ACOES = 5;

    static double[] vetor_precos;

    public PriceOscillator() {
        super();
    }

    public OrdersTO doTrade() throws Exception {
        OrdersTO ordens = new OrdersTO();
        for (int i = 0; i < papers.length; i++) {
            ordens.add(doTradeByPaper(i, papers[i]));
        }
        return ordens;
    }

    static double last_osc = -1;

    private int currTime = -1;

    public OrderTO doTradeByPaper(int index, String idPaper) throws Exception {
        QuoteTO quote = getQuote(idPaper);
        currTime++;
        double preco = quote.getClose();
        if (currTime < LONG_PERIOD) {
            novo_dia(preco, dias[index]);
            return this.createSingleOrder(idPaper, 0, preco, true);
        } else {
            double shorter = media(dias[index], SHORT_PERIOD);
            double longer = media(dias[index], LONG_PERIOD);
            double osc = (shorter - longer) / shorter * 100;
            int v;
            boolean isBuy;
            if (last_osc < 0 && osc > 0) {
                v = VOLUME;
                isBuy = true;
            } else if (last_osc > 0 && osc < 0) {
                v = -1 * VOLUME;
                isBuy = false;
            } else {
                v = 0;
                isBuy = true;
            }
            last_osc = osc;
            novo_dia(preco, dias[index]);
            return this.createSingleOrder(idPaper, Math.abs(v), preco, isBuy);
        }
    }

    public static double media(double dias[], int ndias) {
        double t = 0;
        for (int i = dias.length - 1; i >= (dias.length - ndias); i--) t = t + dias[i];
        return (t / ndias);
    }

    public static void novo_dia(double p, double dias[]) {
        for (int i = 0; i < LONG_PERIOD - 1; i++) dias[i] = dias[i + 1];
        dias[LONG_PERIOD - 1] = p;
    }

    public static double media(double d[]) {
        double m = 0;
        for (int i = 0; i < LONG_PERIOD; i++) m += d[i];
        return m / 5;
    }
}

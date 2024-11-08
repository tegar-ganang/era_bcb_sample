package agex.traders;

import agex.db.QuoteTO;
import agex.finmath.Trend;
import agex.to.OrderTO;
import agex.to.OrdersTO;

public class MA extends MultiAssetTrader {

    public void setupTrader() {
        super.setupTrader();
        try {
            dias = new double[papers.length][PERIODO];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static final int MUITO_ALTO = 0;

    static final int ALTO = 1;

    static final int NEUTRO = 2;

    static final int BAIXO = 3;

    static final int MUITO_BAIXO = 4;

    static int NUM_ESTADOS = 101;

    static int NUM_ACOES = 5;

    static double[] vetor_precos;

    public MA() {
        super();
    }

    static final int PERIODO = 10;

    static final int VOLUME = 50000;

    private double dias[][];

    public OrdersTO doTrade() throws Exception {
        OrdersTO ordens = new OrdersTO();
        for (int i = 0; i < papers.length; i++) {
            ordens.add(doTradeByPaper(i, papers[i]));
        }
        return ordens;
    }

    private int currTime = -1;

    public OrderTO doTradeByPaper(int index, String idPaper) throws Exception {
        QuoteTO quote = getQuote(idPaper);
        currTime++;
        double preco = quote.getClose();
        if (currTime < PERIODO) {
            novo_dia(preco, dias[index]);
            return createSingleOrder(idPaper, 0, preco, true);
        } else {
            double m = media(dias[index]);
            int v;
            boolean isBuy;
            if (Trend.trend(dias[index]) > 0 && dias[index][PERIODO - 1] < m) {
                v = VOLUME;
                isBuy = true;
            } else if (Trend.trend(dias[index]) < 0 && m < dias[index][PERIODO - 1]) {
                v = -1 * VOLUME;
                isBuy = false;
            } else {
                v = 0;
                isBuy = true;
            }
            novo_dia(preco, dias[index]);
            return createSingleOrder(idPaper, Math.abs(v), preco, isBuy);
        }
    }

    public static void novo_dia(double p, double dias[]) {
        for (int i = 0; i < PERIODO - 1; i++) dias[i] = dias[i + 1];
        dias[PERIODO - 1] = p;
    }

    public static double media(double d[]) {
        double m = 0;
        for (int i = 0; i < PERIODO; i++) m += d[i];
        return m / PERIODO;
    }
}

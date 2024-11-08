package agex.traders;

import agex.db.QuoteTO;
import agex.finmath.Trend;
import agex.sim.SimClock;
import agex.to.OrderTO;
import agex.to.OrdersTO;

/**
 * Classe que implementa um agente baseado no indicador Price Volume Trend (PVT). Tal como 
 * descrito na p�gina: http://www.marketscreen.com/help/AtoZ/default.asp?hideHF=&Num=86
 * 
 */
public class PVT extends MultiAssetTrader {

    double preco;

    double last_preco;

    static final int PERIODO = 10;

    static final int VOLUME = 50000;

    private double dias[][];

    public void setupTrader() {
        super.setupTrader();
        try {
            dias = new double[papers.length][PERIODO];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static double[] vetor_precos;

    public PVT() {
        super();
    }

    private int currTime = -1;

    public OrdersTO doTrade() throws Exception {
        OrdersTO ordens = new OrdersTO();
        for (int i = 0; i < papers.length; i++) {
            ordens.add(doTradeByPaper(i, papers[i]));
        }
        return ordens;
    }

    public OrderTO doTradeByPaper(int index, String idPaper) throws Exception {
        QuoteTO quote = getQuote(idPaper);
        currTime++;
        double preco = quote.getClose();
        int volume = quote.getVolume();
        if (currTime < PERIODO) {
            novo_dia(preco, volume, dias[index]);
            return createSingleOrder(idPaper, 0, preco, true);
        } else {
            double m = media(dias[index]);
            int v;
            if (Trend.trend(dias[index]) > 0 && dias[index][PERIODO - 1] < m) v = VOLUME; else if (Trend.trend(dias[index]) < 0 && m < dias[index][PERIODO - 1]) v = -1 * VOLUME; else v = 0;
            novo_dia(preco, volume, dias[index]);
            return createSingleOrder(idPaper, Math.abs(v), preco, true);
        }
    }

    /**
 * Mantem uma lista do PVT de n dias para determinar sua tendencia
 * @param p
 * @param dias
 */
    public static void novo_dia(double p, int volume, double dias[]) {
        p = pvt(p, volume);
        for (int i = 0; i < PERIODO - 1; i++) dias[i] = dias[i + 1];
        dias[PERIODO - 1] = p;
    }

    private static double current_pvt = 0;

    private static double previous_price = 0;

    private static double pvt(double p, int volume) {
        if (previous_price != 0) {
            current_pvt = current_pvt + volume * ((previous_price - p) / previous_price);
            previous_price = p;
            return current_pvt;
        } else {
            previous_price = p;
            return current_pvt;
        }
    }

    public static double media(double d[]) {
        double m = 0;
        for (int i = 0; i < PERIODO; i++) m += d[i];
        return m / PERIODO;
    }
}

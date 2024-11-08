package agex.traders.monoasset;

import agex.db.QuoteTO;
import agex.sim.SimClock;
import agex.to.OrdersTO;

public class RSI extends MonoAssetTrader {

    private static final int PERIODO = 14;

    private static final int VOLUME = 50000;

    private double dias[];

    public void setupTrader() {
        super.setupTrader();
        try {
            dias = new double[PERIODO];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RSI() {
        super();
    }

    private int currTime = -1;

    public OrdersTO doTrade() throws Exception {
        QuoteTO quote = getQuote(idPaper);
        currTime++;
        if (quote == null) return null;
        double preco = quote.getClose();
        if (currTime < PERIODO) {
            novo_dia(preco, dias);
            return createOrdemTO(0, preco, true);
        } else {
            int v;
            if (ifr(dias) < 70) v = VOLUME; else if (ifr(dias) > 70) v = -1 * VOLUME; else v = 0;
            novo_dia(preco, dias);
            return createOrdemTO(Math.abs(v), dias[PERIODO - 1], v > 0 ? true : false);
        }
    }

    private double ifr(double dias[]) {
        return 100 - (100 / (1 + average_up(dias) / average_down(dias)));
    }

    private void novo_dia(double p, double dias[]) {
        for (int i = 0; i < PERIODO - 1; i++) dias[i] = dias[i + 1];
        dias[PERIODO - 1] = p;
    }

    private double average_up(double d[]) {
        double m = 0;
        int c = 0;
        for (int i = 0; i < PERIODO - 1; i++) if (d[i] < d[i + 1]) {
            m += (d[i + 1] - d[i]);
            c++;
        }
        return m / c;
    }

    private double average_down(double d[]) {
        double m = 0;
        int c = 0;
        for (int i = 0; i < PERIODO - 1; i++) if (d[i] > d[i + 1]) {
            m += (d[i] - d[i + 1]);
            c++;
        }
        return m / c;
    }
}

package agex.traders;

import agex.db.QuoteTO;
import agex.sim.SimClock;
import agex.to.OrderTO;
import agex.to.OrdersTO;

public class Sthocastic extends MultiAssetTrader {

    public void setupTrader() {
        super.setupTrader();
        try {
            dias = new double[papers.length][PERIODO];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double last_preco;

    static int NUM_ESTADOS = 101;

    static int NUM_ACOES = 5;

    public Sthocastic() {
        super();
    }

    static final int PERIODO = 14;

    static final int LIMITE = 70;

    static final int VOLUME = 10000;

    double dias[][];

    public OrdersTO doTrade() throws Exception {
        OrdersTO ordens = new OrdersTO();
        for (int i = 0; i < papers.length; i++) {
            ordens.add(doTradeByPaper(i, papers[i]));
        }
        return ordens;
    }

    double currTime = -1;

    public OrderTO doTradeByPaper(int index, String idPaper) throws Exception {
        QuoteTO preco = getQuote(idPaper);
        currTime++;
        double price = preco.getClose();
        if (currTime < PERIODO) {
            novo_dia(price, dias[index]);
            return createSingleOrder(idPaper, 0, price, true);
        } else {
            novo_dia(price, dias[index]);
            int v;
            double aux = k(dias[index]);
            if (aux > 80) v = VOLUME; else if (aux < 20) v = -1 * VOLUME; else v = 0;
            return createSingleOrder(idPaper, Math.abs(v), dias[index][PERIODO - 1], v > 0 ? true : false);
        }
    }

    public double k(double dias[]) {
        int size = dias.length;
        double A = max(dias);
        double B = min(dias);
        return 100 * (dias[size - 1] - B) / (A - B);
    }

    public void novo_dia(double p, double dias[]) {
        for (int i = 0; i < PERIODO - 1; i++) dias[i] = dias[i + 1];
        dias[PERIODO - 1] = p;
    }

    public double max(double d[]) {
        double m = d[0];
        for (int i = 0; i < PERIODO - 1; i++) if (m < d[i]) {
            m = d[i];
        }
        return m;
    }

    public double min(double d[]) {
        double m = d[0];
        for (int i = 0; i < PERIODO - 1; i++) if (m > d[i]) {
            m = d[i];
        }
        return m;
    }
}

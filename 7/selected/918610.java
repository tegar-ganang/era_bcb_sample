package agex.traders.monoasset;

import agex.db.QuoteTO;
import agex.sim.SimClock;
import agex.to.OrdersTO;

public class Sthocastic extends MonoAssetTrader {

    public void setupTrader() {
        super.setupTrader();
    }

    double last_preco;

    static final int MUITO_ALTO = 0;

    static final int ALTO = 1;

    static final int NEUTRO = 2;

    static final int BAIXO = 3;

    static final int MUITO_BAIXO = 4;

    static int NUM_ESTADOS = 101;

    static int NUM_ACOES = 5;

    public Sthocastic() {
        super();
    }

    public static String sOrdem(int ord) {
        switch(ord) {
            case MUITO_ALTO:
                return "COMPRA FORTE";
            case ALTO:
                return "COMPRA";
            case NEUTRO:
                return "NEUTRO";
            case BAIXO:
                return "VENDA";
            case MUITO_BAIXO:
                return "VENDA FORTE";
            default:
                return "ERRO";
        }
    }

    public static int getVolume(int decisao) {
        if (decisao == MUITO_ALTO) return 10000; else if (decisao == ALTO) return 5000; else if (decisao == NEUTRO) return 0; else if (decisao == BAIXO) return -5000; else if (decisao == MUITO_BAIXO) return -10000; else return 0;
    }

    static final int PERIODO = 14;

    static final int LIMITE = 70;

    static final int VOLUME = 10000;

    double dias[] = new double[PERIODO];

    public void init() throws Exception {
        dias = new double[PERIODO];
    }

    double currTime = -1;

    public OrdersTO doTrade() throws Exception {
        QuoteTO preco = getQuote(idPaper);
        currTime++;
        double price = preco.getClose();
        if (currTime < PERIODO) {
            novo_dia(price, dias);
            return createOrdemTO(0, price, true);
        } else {
            novo_dia(price, dias);
            int v;
            double aux = k(dias);
            if (aux > 80) v = VOLUME; else if (aux < 20) v = -1 * VOLUME; else v = 0;
            return createOrdemTO(Math.abs(v), dias[PERIODO - 1], v > 0 ? true : false);
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

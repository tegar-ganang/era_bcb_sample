package agex.traders.monoasset;

import agex.db.QuoteTO;
import agex.finmath.Trend;
import agex.sim.SimClock;
import agex.to.OrdersTO;

public class MA extends MonoAssetTrader {

    public void setupTrader() {
        super.setupTrader();
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    double preco;

    double last_preco;

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

    static final int PERIODO = 10;

    static final int VOLUME = 50000;

    private double dias[];

    public void init() throws Exception {
        dias = new double[PERIODO];
    }

    private int currTime = -1;

    public OrdersTO doTrade() throws Exception {
        QuoteTO quote = getQuote(idPaper);
        currTime++;
        double preco = quote.getClose();
        if (currTime < PERIODO) {
            novo_dia(preco, dias);
            return createOrdemTO(0, preco, true);
        } else {
            double m = media(dias);
            int v;
            if (Trend.trend(dias) > 0 && dias[PERIODO - 1] < m) v = VOLUME; else if (Trend.trend(dias) < 0 && m < dias[PERIODO - 1]) v = -1 * VOLUME; else v = 0;
            novo_dia(preco, dias);
            return createOrdemTO(Math.abs(v), preco, true);
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

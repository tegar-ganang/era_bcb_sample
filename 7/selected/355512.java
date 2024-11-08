package agex.traders.monoasset;

import agex.db.QuoteTO;
import agex.sim.SimClock;
import agex.to.OrdersTO;

public class MACD extends MonoAssetTrader {

    @Override
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

    public MACD() {
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

    static final int PERIODO = 26;

    static final int SHORT_PERIOD = 12;

    static final int SIGNAL = 9;

    static final int VOLUME = 50000;

    private double dias[];

    private double signalArray[];

    public void init() throws Exception {
        dias = new double[PERIODO];
        signalArray = new double[SIGNAL];
    }

    static boolean lastMacdUnderSignal = true;

    private int currTime = -1;

    public OrdersTO doTrade() throws Exception {
        QuoteTO quote = getQuote(idPaper);
        currTime++;
        double preco = quote.getClose();
        boolean MacdUnderSignal;
        if (currTime < PERIODO) {
            novo_dia(preco, dias);
            return createOrdemTO(0, preco, true);
        } else if (currTime < PERIODO + SIGNAL) {
            double longer = media(dias, PERIODO);
            double shorter = media(dias, SHORT_PERIOD);
            novo_dia(preco, dias);
            novo_dia(shorter - longer, signalArray);
            return createOrdemTO(0, preco, true);
        } else {
            double longer = media(dias, PERIODO);
            double shorter = media(dias, SHORT_PERIOD);
            double signal = media(signalArray, SIGNAL);
            if ((shorter - longer) > signal) MacdUnderSignal = false; else MacdUnderSignal = true;
            int v = 0;
            if (!MacdUnderSignal && lastMacdUnderSignal) {
                v = VOLUME;
            } else if (MacdUnderSignal && !lastMacdUnderSignal) {
                v = -1 * VOLUME;
            } else v = 0;
            lastMacdUnderSignal = MacdUnderSignal;
            novo_dia(preco, dias);
            novo_dia(shorter - longer, signalArray);
            return createOrdemTO(Math.abs(v), preco, true);
        }
    }

    public static double media(double dias[], int ndias) {
        double t = 0;
        for (int i = dias.length - 1; i >= (dias.length - ndias); i--) t = t + dias[i];
        return (t / ndias);
    }

    public static void novo_dia(double p, double dias[]) {
        int tam = dias.length;
        for (int i = 0; i < tam - 1; i++) dias[i] = dias[i + 1];
        dias[tam - 1] = p;
    }

    public static double media(double d[]) {
        return media(d, d.length);
    }
}

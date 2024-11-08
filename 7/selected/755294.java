package agex.traders;

import agex.db.QuoteTO;
import agex.finmath.Trend;
import agex.sim.SimClock;
import agex.to.OrderTO;
import agex.to.OrdersTO;

/**
 * Classe que implementa um agente baseado no indicador On Balance Volume (OBV) . Tal como 
 * descrito na p�gina: http://www.marketscreen.com/help/AtoZ/default.asp?hideHF=&Num=75
 * 
 * The OBV is in a rising trend when each new peak is higher 
 * than the previous peak and each new trough is higher than the previous trough (the lower of next two points). 
 * Likewise, the OBV is in a falling trend when each successive peak is lower than 
 * the previous peak and each successive trough is lower than the previous trough. 
 * When the OBV is moving sideways and is not making successive highs and lows, it is in a doubtful trend.
 * 
 * Once a trend is established, it remains in force until it is broken. There are two ways 
 * in which the OBV trend can be broken. The first occurs when the trend changes from a rising 
 * trend to a falling trend, or from a falling trend to a rising trend.
 * 
 * The second way the OBV trend can be broken is if the trend changes to a doubtful trend 
 * and remains doubtful for more than three days. Thus, if the security changes from a rising 
 * trend to a doubtful trend and remains doubtful for only two days before changing back to a rising 
 * trend, the OBV is consid-ered to have always been in a rising trend.
 * 
 * When the OBV changes to a rising or falling trend, a "breakout" has occurred. 
 * Since OBV breakouts normally precede price breakouts, investors should buy long on OBV 
 * upside breakouts. Likewise, investors should sell short when the OBV makes a downside breakout. 
 * Positions should be held until the trend changes (as explain-ed in the preceding paragraph).
 *
 * This method of analyzing On Balance Volume is designed for trading short-term cycles. 
 * According to Granville, investors must act quickly and decisively if they wish to profit 
 * from short-term OBV analysis.
 * 
 * Book: 
 * Granville's New Strategy of Daily Stock Market Timing for Maximum Profit 
 * # Publisher: Simon & Schuster (June 1976)
# Language: English
# ISBN-10: 0133634329
# ISBN-13: 978-0133634327
 * 
 */
public class OBV extends MultiAssetTrader {

    double preco;

    double last_preco;

    static final int PERIODO = 10;

    static final int VOLUME = 50000;

    private double dias[][];

    private static int HIGH_TREND = 0;

    private static int DOWN_TREND = 1;

    private static int DOUBT_TREND = 2;

    /**
	 *  The OBV is in a rising trend when each new peak is higher 
	 * than the previous peak and each new trough is higher than the previous trough 
	 * (the lower of next two points). 
	 * Likewise, the OBV is in a falling trend when each successive peak is lower than 
	 * the previous peak and each successive trough is lower than the previous trough. 
	 * When the OBV is moving sideways and is not making successive highs and lows, it is in a doubtful trend.
	 * @param index
	 * @return
	 */
    private static int OBVTrend(int index, double dias[][]) {
        boolean possibleHighTrend = true;
        boolean possibleDownTrend = true;
        boolean doubtTrend = false;
        int i = 0;
        while (i < dias[index].length - 3 && (possibleDownTrend || possibleHighTrend)) {
            if (dias[index][i] < dias[index][i + 2] && dias[index][i + 1] < dias[index][i + 3]) {
                possibleHighTrend = true;
            } else possibleHighTrend = false;
            if (dias[index][i] > dias[index][i + 2] && dias[index][i + 1] > dias[index][i + 3]) {
                possibleDownTrend = true;
            } else possibleDownTrend = false;
            i = i + 2;
        }
        if (!possibleDownTrend && !possibleHighTrend) return OBV.DOUBT_TREND;
        if (possibleDownTrend && !possibleHighTrend) return OBV.DOWN_TREND;
        if (!possibleDownTrend && possibleHighTrend) return OBV.HIGH_TREND;
        return OBV.DOUBT_TREND;
    }

    public static String obvStr(int index, double dias[][]) {
        if (OBVTrend(index, dias) == OBV.HIGH_TREND) return "HIGH";
        if (OBVTrend(index, dias) == OBV.DOWN_TREND) return "DOWN";
        if (OBVTrend(index, dias) == OBV.DOUBT_TREND) return "DOUBT";
        return "erro";
    }

    public static void main(String[] args) {
        double dias[][] = { { 1.0, 0.8, 1.2, 1.1, 1.5, 1.3, 1.8, 1.6 }, { 1.0, 1.4, 1.5, 1.3, 1.1, 1.9, 1.0, 1.2 }, { 1.0, 1.4, 0.9, 1.3, 0.7, 1.2, 0.5, 1.0 } };
        System.out.println("obv trend=" + obvStr(0, dias));
        System.out.println("obv trend=" + obvStr(1, dias));
        System.out.println("obv trend=" + obvStr(2, dias));
    }

    public void setupTrader() {
        super.setupTrader();
        try {
            dias = new double[papers.length][PERIODO];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static double[] vetor_precos;

    public OBV() {
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
        p = obv(p, volume);
        for (int i = 0; i < PERIODO - 1; i++) dias[i] = dias[i + 1];
        dias[PERIODO - 1] = p;
    }

    private static double current_obv = 0;

    private static double previous_price = 0;

    private static double obv(double price, int volume) {
        if (previous_price != 0) {
            if (price > previous_price) current_obv = current_obv + volume; else if (price < previous_price) current_obv = current_obv - volume;
            previous_price = price;
            return current_obv;
        } else {
            previous_price = price;
            current_obv = volume;
            return current_obv;
        }
    }

    public static double media(double d[]) {
        double m = 0;
        for (int i = 0; i < PERIODO; i++) m += d[i];
        return m / PERIODO;
    }
}

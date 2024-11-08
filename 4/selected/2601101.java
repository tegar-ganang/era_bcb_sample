package elaborazione.operativo.decoder;

import elaborazione.operativo.colospace.LSColorSpace;

/**
 * Permette di ricostruire un immagine a partire da dei blocchi di dimensione 
 * quadrata. Per il momento funziona solamente in modalit� sequanziale: cio� per
 * funzionare correttamente, una volta scelto un canale da ricostruire bisogna
 * attendere che la sua ricostruizione sia terminata per poter passare a ricostruirne
 * un altro.
 * @author Luca
 */
public class BlockJoiner {

    protected int h, w, accW;

    protected int wCount = 0;

    protected int hCount = 0;

    protected int squareDim;

    private boolean terminato = false;

    private int div = 0, accChannel = 0;

    private int maxValue = 255;

    private LSColorSpace colorSpace;

    protected int[][] img;

    /**
     * crea una nuova istanza di BlockJoiner
     * @param h l'altezza dell'immagine originale da ricostruire
     * @param w la larghezza dell'immagine originale da ricostruire
     * @param blockDim la radice quadrata della dimensione di un singlo blocco
     * da usare per la ricostruzione
     * @param space il color space dell'immagine da ricostruire
     */
    public BlockJoiner(int h, int w, int blockDim, LSColorSpace space) {
        squareDim = blockDim;
        this.h = h;
        this.w = w;
        accW = w;
        img = new int[h][w];
        colorSpace = space;
    }

    /**
     * Permette di aggiungere un blocco all'immagine da ricostruire
     * @return true nel caso in cui si possano aggiungere altri blocchi al canale
     * selzionato.false nel caso in cui non sia pi� possibile aggiungerne altri.
     */
    public boolean join(float[][] block) {
        int ii = -1, jj;
        int val;
        int wLimit = squareDim * div;
        for (int i = hCount; i < hCount + squareDim && i < h; i++) {
            jj = -1;
            ii++;
            for (int j = wCount; j < wCount + wLimit && j < accW; j++) {
                jj++;
                val = (int) ((block[ii][jj / div] + 128) + 0.5f);
                if (val > maxValue) val = maxValue; else if (val < 0) val = 0;
                img[i][j] = img[i][j] | (val << accChannel);
            }
        }
        wCount += wLimit;
        if (wCount >= accW) {
            hCount += squareDim;
            wCount = 0;
        }
        if (hCount < h) terminato = true; else terminato = false;
        return terminato;
    }

    /**permette di ottenere l'immagine rocostruita */
    public int[][] getRawData() {
        return img;
    }

    /**
     * Permette di impostare il canale dell'immagine che si deve ricostruire.
     */
    public void setChannel(int channel) {
        wCount = hCount = 0;
        maxValue = colorSpace.getMaxValue(channel);
        div = colorSpace.getVerticalSubSampleFactor(channel);
        accChannel = colorSpace.getChannelOffset(channel);
    }
}

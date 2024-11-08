package elaborazione.operativo.encoder;

import elaborazione.operativo.colospace.LSColorSpace;

/**
 * Classe che permette di ottenere i blocchetti NxN da una matrice
 * bidimensionale.
 * @author Luca Segafredo
 */
public class BlockGen {

    private int[][] raster;

    /**contatore che serve a mantenere traccia dei blocchi gia' generati */
    private int blockCounter;

    private int wCounter;

    private int hCounter;

    private int w;

    private int h;

    private int squareDim;

    float[][] block;

    private int accChannel = 0;

    public static final int Y_CHANNEL = 16;

    public static final int U_CHANNEL = 8;

    public static final int V_CHANNEL = 0;

    private int init = 0;

    private int jump = 1;

    private LSColorSpace space;

    /**
     * Creates a new instance of BlockGen
     */
    public BlockGen(int[][] data, int squareDim, LSColorSpace colorSpace) {
        raster = data;
        h = raster.length;
        w = raster[0].length;
        wCounter = 0;
        hCounter = 0;
        blockCounter = 0;
        this.squareDim = squareDim;
        block = new float[squareDim][squareDim];
        space = colorSpace;
    }

    /**permette di ottenere il blocco successivo
     *@return una array bidimensionale di float contenente il blocco successivo
     * di dati. <code>null</code> nel caso in cui si sia raggiunta la fine
     *dell'array dati.
     */
    public float[][] next() {
        if (wCounter < w) {
            estraiBlocco(block);
        } else {
            hCounter += squareDim;
            if (hCounter >= h) return null;
            wCounter = init;
            estraiBlocco(block);
        }
        wCounter += squareDim * jump;
        blockCounter++;
        return block;
    }

    public int getBlocchiEstratti() {
        return blockCounter;
    }

    private void estraiBlocco(float[][] quanto) {
        int ii = -1;
        int jj;
        for (int i = hCounter; i < h && i < hCounter + squareDim; i++) {
            ii++;
            jj = -1;
            for (int j = wCounter; j < w && j < wCounter + squareDim * jump; ) {
                jj++;
                quanto[ii][jj] = ((raster[i][j] >>> accChannel) & 0xff) - 128;
                j += jump;
            }
        }
    }

    public void setChannel(int channel) {
        accChannel = space.getChannelOffset(channel);
        jump = space.getVerticalSubSampleFactor(channel);
        hCounter = 0;
        wCounter = init = space.getVerticalInit(channel);
    }
}

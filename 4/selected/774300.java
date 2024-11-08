package elaborazione.operativo.decoder;

import elaborazione.operativo.tables.LSDequantizer;
import elaborazione.operativo.colospace.ColorSpaceManager;
import elaborazione.operativo.colospace.LSColorSpace;
import elaborazione.operativo.ImageInfo;
import elaborazione.operativo.tables.CHDEntry;
import elaborazione.operativo.tables.LSTables;
import elaborazione.operativo.transforms.DCT;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/**
 *Questa classe permette di decomprime un file immagine  di tipo ls, cioe compresso
 *con LSEncoder.
 * @author Luca Segafredo
 */
public class LSDecoder {

    private BlockJoiner joiner;

    float[][] qMatrix;

    private static int[][] zigzag = LSTables.ZIGZAG;

    private int DC;

    int imgW, imgH;

    private int prevDc = 0;

    private int codifica;

    private LSColorSpace colorSpace;

    private ArrayList<float[][]> qMatrices;

    private int channelIndex;

    private int channelsNumber;

    private int[][][] chdDCTables;

    private int[][][] chdACTables;

    private int[] minDcCodeLen, minAcCodeLen;

    /**
     *dato un blocco 8x8 lo inizializza a 0
     */
    private void resetBlock(float[][] block) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; ) {
                block[i][j++] = 0f;
            }
        }
    }

    /**
     * Permette di decodificare un file ls in un file raw.
     * @param inFile il file ls da decodificare
     * @throws java.io.IOException
     * @return un oggetto di tipo ImageInfo che contiene tutte le informazioni 
     * sull'immagine decodificata
     */
    public ImageInfo decode(String inFile) throws IOException {
        return decode(new File(inFile));
    }

    /**
     * Permette di decodificare un file ls in un file raw.
     * @param inFile il file ls da decodificare
     * @throws java.io.IOException
     * @return un oggetto di tipo ImageInfo che contiene tutte le informazioni 
     * sull'immagine decodificata
     */
    public ImageInfo decode(File inFile) throws IOException {
        prevDc = 0;
        ImageInputStream ris = ImageIO.createImageInputStream(inFile);
        it.tidalwave.imageio.io.RAWImageInputStream iis = new it.tidalwave.imageio.io.RAWImageInputStream(ris);
        iis.selectBitReader(-1, 262144);
        long start = System.currentTimeMillis();
        readHeader(iis);
        joiner = new BlockJoiner(imgH, imgW, 8, colorSpace);
        for (channelIndex = 0; channelIndex < channelsNumber; channelIndex++) {
            qMatrix = qMatrices.get(channelIndex);
            DCT.scaleQuantizationTable(qMatrix);
            joiner.setChannel(channelIndex + 1);
            decode(joiner, iis);
        }
        long end = System.currentTimeMillis();
        System.out.println("tempo totale di decodifica: " + (end - start) / 1000f + " sec");
        int[][] immagine = joiner.getRawData();
        colorSpace.convertToRGB(immagine);
        qMatrices = null;
        chdDCTables = chdACTables = null;
        minDcCodeLen = minAcCodeLen = null;
        return new ImageInfo(imgH, imgW, colorSpace, immagine, (int) inFile.length(), (end - start) / 1000f);
    }

    /**
     * legge l'header del file
     * @param iis
     * @throws java.io.IOException
     */
    private void readHeader(ImageInputStream iis) throws IOException {
        int L = (int) iis.readBits(8);
        int S = (int) iis.readBits(8);
        if (L != 76 || S != 83) {
            System.out.println("L: " + L + " S:" + S);
            System.out.println("TIPO FILE NON SUPPORTATO");
            throw new IOException("Tipo file non supportato");
        }
        codifica = (int) iis.readBits(8);
        colorSpace = ColorSpaceManager.getColorSpace(codifica);
        imgH = (int) iis.readBits(16);
        imgW = (int) iis.readBits(16);
        channelsNumber = colorSpace.getChannelsNumber();
        qMatrices = new ArrayList<float[][]>(channelsNumber);
        for (int i = 0; i < channelsNumber; i++) {
            qMatrices.add(LSDequantizer.readMatrix(iis));
        }
        minDcCodeLen = new int[channelsNumber];
        chdDCTables = new int[channelsNumber][][];
        for (int i = 0; i < channelsNumber; i++) {
            minDcCodeLen[i] = (int) iis.readBits(8);
            chdDCTables[i] = CHDEntry.readCHDDCTable(iis);
        }
        minAcCodeLen = new int[channelsNumber];
        chdACTables = new int[channelsNumber][][];
        for (int i = 0; i < channelsNumber; i++) {
            minAcCodeLen[i] = (int) iis.readBits(8);
            chdACTables[i] = CHDEntry.readCHDACTable(iis);
        }
        System.out.println("iis pos : " + iis.getStreamPosition() + " offset : " + iis.getBitOffset());
    }

    /**
     *Permette di decodificare uno stream di dati.
     * @param joiner L'oggetto che permette di ricostruire l'immagine originale
     * @param iis lo stream che permette di leggere i dati relativi all'immagine
     * codificata.
     */
    private void decode(BlockJoiner joiner, ImageInputStream iis) throws IOException {
        boolean continua = true;
        float[][] block = new float[8][8];
        float[][] out = new float[8][8];
        while (continua) {
            resetBlock(block);
            decodeDC(iis, block);
            decodeAC(iis, block);
            DCT.IDCT(block, qMatrix, out);
            continua = joiner.join(out);
        }
    }

    /**
     *effettua la decodifica di huffman con la tecnica CHD.
     *@return intero rappr la righa della tabella DC associata al codice letto,
     * in caso di decodifica dei DC.
     *un intero i cui primi 4 bit rapp la riga della tabella DC e i successivi 4 bit
     *il numero di zeri che precedeva il coeff AC compresso, nel caso della decodifica dedli AC.
     * @throws java.io.IOException
     */
    private static int fromCodetoRow(ImageInputStream iis, int minLen, int[][] table) throws IOException {
        int i = (int) iis.readBits(minLen);
        int salto;
        while (table[i][0] != 1) {
            salto = (int) iis.readBits(table[i][2]);
            i = table[i][1] + salto;
        }
        return table[i][1];
    }

    /**
     *decodifica il coefficente DC del blocco 8x8
     */
    private void decodeDC(ImageInputStream iis, float[][] block) throws IOException {
        int row = fromCodetoRow(iis, minDcCodeLen[channelIndex], chdDCTables[channelIndex]);
        int col = (int) iis.readBits(row);
        DC = decodeDC(row, col);
        block[0][0] = DC + prevDc;
        prevDc += DC;
    }

    /**
     *ritorna il valore DC originale, cioe non compresso, tramite una ricerca nella tabella
     *dei DC.
     */
    private static int decodeDC(int row, int col) {
        if ((col >>> row - 1) == 1) return col;
        return col - (1 << row) + 1;
    }

    /**
     *decodifica i coefficenti AC di un blocco 8x8
     */
    private void decodeAC(ImageInputStream iis, float[][] block) throws IOException {
        int zzIndex = 1, zeri, row, col;
        int entry;
        while (zzIndex < 64) {
            entry = fromCodetoRow(iis, minAcCodeLen[channelIndex], chdACTables[channelIndex]);
            zeri = entry >>> 4;
            row = entry & 0xf;
            if (zeri == 0 && row == 0) {
                break;
            }
            zzIndex += zeri;
            if (zzIndex < 64 && row != 0) {
                col = (int) iis.readBits(row);
                block[zigzag[zzIndex][0]][zigzag[zzIndex][1]] = decodeDC(row, col);
                zzIndex++;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        LSDecoder dec = new LSDecoder();
        ImageInfo info = dec.decode(new File("C:\\prova800.ls"));
    }
}

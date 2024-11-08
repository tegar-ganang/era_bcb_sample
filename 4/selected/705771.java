package elaborazione.operativo.encoder;

import elaborazione.operativo.tables.LSQuantizer;
import elaborazione.operativo.MathUtil;
import elaborazione.operativo.colospace.ColorSpaceManager;
import elaborazione.operativo.colospace.LSColorSpace;
import elaborazione.operativo.ImageInfo;
import elaborazione.operativo.transforms.DCT;
import elaborazione.operativo.colospace.TipoImmagineSconosciuto;
import elaborazione.operativo.tables.CHDEntry;
import elaborazione.operativo.tables.LSTables;
import it.tidalwave.imageio.io.RAWImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

/**
 *
 * Classe che permette di comprimere dei file immagine di tipo raw(sia gray scale che RGB)
 * in immagini di formato ls(Luca Segafredo).L'algoritmo di compressione usato &egrave;
 * e' la versione base dell'algoritmo usato dal JPEG. Quindi, dopo aver applicato
 * la DCT a un blocco 8x8,questo viene ispezionato tramite una scansione a zigzag.
 * Durante la scansione a zigzag, viene applicata una forma di RLE uguale a
 * quella descritta dallo standar JPEG. Una volta terminata la scansione
 * i coefficenti DC(o meglio DC-prevDC) e AC vengono compressi tramite l'uso di tabelle di Huffman
 * (sempre quelle descritte dalla specifica JPEG).
 * Nota: la tabelle di huffman che vengono salvate nell'intestazione non sono quelle
 * usate per la codifica, o meglio sono le stesse solo scritte in un altra forma in
 * modo da poter applicare, il piu' velocemente possibile, la tecnica di CHD(
 * <i>Clustered Huffman Decoding</i>) durante la fase di decodifica.
 * Ho scelto la CHD perche':<br>
 * 1) e' una delle tecnice migliori per la decodifica dei codici di Huffman,<br>
 * 2) e' molto semplice da implementare.<br>
 *Per una descrizione delle tabelle CHD si veda il file CHDEntry.java
 *<br>
 *Per la compressione delle immagini RGB viene prima fatta la conversione in YUV
 *e poi viene fatto il sotto campionamento YUV422.
 * @author Luca Segafredo
 */
public class LSEncoder {

    private static final int N = 8;

    private static final int N2 = 64;

    private static final String ERR_DIM = "ERRORE!!! --- La dimensione del file non corrisponde ai parametri inseriti!";

    private int prevDC = 0;

    private int[][] qMatrix;

    private int[][] zigzag;

    /**tabella dei coeff DC*/
    private int[][] dcHuffmanTable;

    /**tabella di huffman per i coefficenti AC*/
    private int[][][] acHuffmanTable;

    private int h, w, quality;

    private ArrayList<int[]> zzBlock;

    private BitSpirit bitSpirit;

    private String defaultColorFormat = "YUV422";

    private LSColorSpace colorSpace;

    /** Creates a new instance of LSEncoder */
    public LSEncoder() {
        zigzag = LSTables.ZIGZAG;
        zzBlock = new ArrayList<int[]>(64);
    }

    /**permette di ottenere un block generator per i dati raw contenuti nel file.
     * I dati con cui e' inizializzato il BlockGen sono gia' convertiti secondo
     * lo spazio di colore scelto.
     */
    private BlockGen getImageDataFromFile(File fileIn) throws IOException, TipoImmagineSconosciuto {
        RAWImageInputStream iis = new RAWImageInputStream(ImageIO.createImageInputStream(fileIn));
        iis.selectBitReader(-1, 262144);
        if (iis.length() != (h * w * colorSpace.getChannelsNumber())) throw new IOException(ERR_DIM);
        int[][] raster = colorSpace.convertFromRgb(iis, w, h);
        iis.close();
        return new BlockGen(raster, N, colorSpace);
    }

    /**permette di ottenere un block generator per i dati raw contenuti nell'array.
     * I dati con cui e' inizializzato il BlockGen sono gia' convertiti secondo
     * lo spazio di colore scelto.
     */
    private BlockGen getImageDataFromArray(int[][] imgData) {
        colorSpace.convertFromRgb(imgData);
        return new BlockGen(imgData, N, colorSpace);
    }

    /**
     * permette di comprimere un file immagine raw
     * @param codificaUscita la codifica con cui si vuole comprimere l'immagine.
     * Le codifiche disponibili sono selezionabili tramite
     * la classe ColorSpaceManager
     * @param codifica la codifica dell'immagine da comprimere.(Vedere classe Codifiche)
     * @param fileIn il file da comprimere
     * @param w la larghezza dell'immagine da comprimere
     * @param h l'altezza dell'immagine da comprimere
     * @param quality il fattore di qualita' della compressione.Max = 100, Min = 1;
     * Tutti i valori >100 o <1 vengono convertiti in 100.
     * @param outFile il file in cui salvare l'immagine compressa
     * @throws elaborazione.operativo.colospace.TipoImmagineSconosciuto
     * @throws java.io.IOException
     */
    public void encode(LSColorSpace inSpace, String fileIn, int w, int h, int quality, String outFile, LSColorSpace outSpace) throws IOException, TipoImmagineSconosciuto {
        encode(inSpace, new File(fileIn), w, h, quality, new File(outFile), outSpace);
    }

    /**
     * permette di comprimere un file immagine raw
     * @param codificaUscita la codifica con cui si vuole comprimere l'immagine.
     * Le codifiche disponibili sono selezionabili tramite
     * la classe ColorSpaceManager
     * @param codifica la codifica dell'immagine da comprimere.(Vedere classe Codifiche)
     * @param fileIn il file da comprimere
     * @param w la larghezza dell'immagine da comprimere
     * @param h l'altezza dell'immagine da comprimere
     * @param quality il fattore di qualita' della compressione.Max = 100, Min = 1;
     * Tutti i valori >100 o <1 vengono convertiti in 100.
     * @param outFile il file in cui salvare l'immagine compressa
     * @throws elaborazione.operativo.colospace.TipoImmagineSconosciuto
     * @throws java.io.IOException
     */
    public void encode(LSColorSpace codifica, File fileIn, int w, int h, int quality, File outFile, LSColorSpace codificaUscita) throws IOException, TipoImmagineSconosciuto {
        ImageOutputStream ios = ImageIO.createImageOutputStream(outFile);
        this.h = h;
        this.w = w;
        colorSpace = ColorSpaceManager.getColorSpaceForEncoding(codifica, codificaUscita);
        bitSpirit = new BitSpirit((int) fileIn.length() / 5);
        this.quality = quality;
        encode(ios, getImageDataFromFile(fileIn));
        ios.close();
    }

    /**
     *
     * @param rawData i raster che si vogliono coprimere
     * @param quality la qualit&agrve; con cui vuol comprimere l'immagine(85 di default).
     * @param fileOut il file su cui si vuole salvare l'immagine codificata
     * @param outSpace lo spazio di colore con cui si vuole comprimere l'immagine.
     * Le codifiche disponibili sono selezionabili tramite
     * la classe ColorSpaceManager
     * @throws java.io.IOException
     */
    public void encode(ImageInfo rawData, int quality, String fileOut, LSColorSpace outSpace) throws IOException {
        encode(rawData, quality, new File(fileOut), outSpace);
    }

    /**
     * @param rawData i raster che si vogliono coprimere
     * @param quality la qualit&agrve; con cui vuol comprimere l'immagine(85 di default).
     * @param fileOut il file su cui si vuole salvare l'immagine codificata
     * @param outSpace la codifica con cui si vuole comprimere l'immagine.
     * Le codifiche disponibili sono selezionabili tramite
     * la classe ColorSpaceManager
     * @throws java.io.IOException
     */
    public void encode(ImageInfo rawData, int quality, File fileOut, LSColorSpace outSpace) throws IOException {
        this.quality = quality;
        this.w = rawData.getWidth();
        this.h = rawData.getHeight();
        colorSpace = ColorSpaceManager.getColorSpaceForEncoding(rawData.getActualColorSpace(), outSpace);
        bitSpirit = new BitSpirit(rawData.getHeight() * rawData.getHeight() / 5);
        ImageOutputStream ios = ImageIO.createImageOutputStream(fileOut);
        encode(ios, getImageDataFromArray(rawData.getImageData()));
        ios.close();
    }

    /**
     * Dato un array con i coefficenti per i vari canali di colore,applica
     * l'algoritmo di compressione e salva il risultato sullo stream di output
     */
    private void encode(ImageOutputStream ios, BlockGen blocks) throws IOException {
        prevDC = 0;
        int[][][] matrices;
        System.out.print("codifica in corso..");
        long start = System.currentTimeMillis();
        int channelsNumber = colorSpace.getChannelsNumber();
        initHeader(ios);
        System.out.print(".");
        for (int channel = 1; channel <= channelsNumber; channel++) {
            dcHuffmanTable = colorSpace.getDCTable(channel);
            acHuffmanTable = colorSpace.getACTable(channel);
            qMatrix = colorSpace.getQuantizationMatrix(channel, quality);
            blocks.setChannel(channel);
            encode(blocks);
        }
        bitSpirit.flush(ios);
        long end = System.currentTimeMillis();
        System.out.print("codifica terminata in " + (end - start) / (float) 1000 + "sec \n");
        blocks = null;
        dcHuffmanTable = qMatrix = null;
        acHuffmanTable = null;
        colorSpace = null;
    }

    /** codifica di un array bidimensionale incapsulato in un oggetto BlockGen*/
    private void encode(BlockGen blockGen) throws IOException {
        float[][] blocco;
        float[][] out = new float[N][N];
        int bol = 0;
        while ((blocco = blockGen.next()) != null) {
            bol++;
            DCT.FDCT(blocco, out);
            LSQuantizer.quantize(out, qMatrix);
            zigzagScan(out);
            findCodeForDC(zzBlock.get(0)[1]);
            findCodeForAC(zzBlock);
        }
        System.out.println("Numero blocchi : " + bol);
    }

    /**
     *Inserisce in testa al file le informazioni sull'immagine compressa.
     */
    private void initHeader(ImageOutputStream ios) throws IOException {
        ios.writeBits(76, 8);
        ios.writeBits(83, 8);
        ios.writeBits(colorSpace.getCodifica(), 8);
        ios.writeBits(h, 16);
        ios.writeBits(w, 16);
        int channelsNumber = colorSpace.getChannelsNumber();
        for (int i = 1; i <= channelsNumber; i++) {
            LSQuantizer.write(colorSpace.getQuantizationMatrix(i, quality), ios);
        }
        for (int i = 1; i <= channelsNumber; i++) {
            CHDEntry.writeCHDDCTable(colorSpace.getDCCHDTable(i), colorSpace.getDCMinCodeLen(i), ios);
        }
        for (int i = 1; i <= channelsNumber; i++) {
            CHDEntry.writeCHDACTable(colorSpace.getACCHDTable(i), colorSpace.getACMinCodeLen(i), ios);
        }
    }

    /**
     * Permette di trovare il codice di huffman per il coefficente DC passato
     * @param diff
     * @throws java.io.IOException
     */
    private void findCodeForDC(int diff) throws IOException {
        int abs = MathUtil.abs(diff);
        int row;
        if (abs != 0) row = MathUtil.log2(abs) + 1; else row = 0;
        int codeLen = dcHuffmanTable[row][0];
        int code = (dcHuffmanTable[row][1]) << row;
        int col;
        if (diff >= 0) {
            col = diff;
        } else {
            col = ((diff - 1) << (32 - row)) >>> (32 - row);
        }
        bitSpirit.writeBits((code | col), codeLen + row);
    }

    /**
     *Permette di trovare il codice di huffman per il coefficente AC passato
     */
    private void findCodeForAC(ArrayList<int[]> ac) throws IOException {
        int z, x, row, codeLenAC, codeAC;
        for (int i = 1; i < ac.size(); i++) {
            z = ac.get(i)[0];
            x = ac.get(i)[1];
            if (x != 0) row = MathUtil.log2(MathUtil.abs(x)) + 1; else row = 0;
            int col;
            if (x >= 0) {
                col = x;
            } else {
                col = ((x - 1) << (32 - row)) >>> (32 - row);
            }
            codeLenAC = acHuffmanTable[z][row][0];
            codeAC = acHuffmanTable[z][row][1] << row;
            bitSpirit.writeBits((codeAC | col), codeLenAC + row);
        }
    }

    /**
     *effettua la scansione a zigzag della matrice e nello stesso tempo effettua
     *anche una compressione RLE sui coefficenti AC. Ogni coefficente infatti e'
     *rappresentato da una coppia in cui il secondo valore e' il valore del
     *coefficente non nullo ed il primo il numero di zeri consecutivi che
     * precedevano lo stesso. Il primo coefficente della lista e' (0,DIFF) dove
     *DIFF = DC - DCprec.
     *
     */
    private void zigzagScan(float[][] blocco) {
        int row, col;
        zzBlock.clear();
        int DC = MathUtil.round(blocco[0][0]);
        int diff = DC - prevDC;
        zzBlock.add(new int[] { 0, diff });
        prevDC = DC;
        int AC;
        int Z = 0;
        int z15 = 0;
        for (int i = 1; i < N2; i++) {
            row = zigzag[i][0];
            col = zigzag[i][1];
            AC = MathUtil.round(blocco[row][col]);
            if (AC != 0) {
                if (z15 > 0) while (z15 > 0) {
                    zzBlock.add(new int[] { 15, 0 });
                    z15--;
                }
                zzBlock.add(new int[] { Z, AC });
                Z = 0;
                z15 = 0;
            } else {
                Z++;
                if (Z >= 15) {
                    z15++;
                    Z = 0;
                }
            }
        }
        if (Z > 0 || z15 > 0) zzBlock.add(new int[] { 0, 0 });
    }
}

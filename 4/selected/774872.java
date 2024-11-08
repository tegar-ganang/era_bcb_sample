package org.phramer.v1.decoder.table;

import info.olteanu.utils.*;
import info.olteanu.utils.io.*;
import info.olteanu.utils.lang.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import org.phramer.lib.vocabulary.*;
import org.phramer.v1.decoder.*;
import org.phramer.v1.decoder.table.wordalignment.*;
import org.phramer.v1.decoder.token.*;

/** Support class for binary translation tables <br><br>
 * The binary TT format:
 * <pre>(A) tt.voce - vocabulary for E
 *   -- text file
 *   -- one line:
 * 		word count
 *   -- desc. sorted by count
 * (B) tt.vocf - vocabulary for F
 *   -- same as .voce
 * (C) tt.data - file that stores serialized entries for F
 *   -- binary file
 *   -- format:
 *      (1) F entry (q times - q determined by the file length)
 *      (1.a) n - number of E phrases paired with F
 *                type: byte8
 *      (1.b) logProbabilityTotal (1..n)
 *                type: float32
 *      (1.b) E phrase (1..n)
 *      (1.b.i)  m (eTokenized.length) - number of tokens in the E phrase
 *                type: byte8
 *      (1.b.ii) eTokenized[i] (0..m-1) - variable-length value for the
 *             index of the word in the vocabulary file. Indexed from 0
 *                type: unsignedIntVar40 (1 to 5 bytes. Low values - one byte)
 * (D) tt.idx - file that indexes the F phrases
 *   -- binary file
 *   -- format:
 *      (1) F phrase (q times - q determined by the file length)
 *            (same q as for tt.data)
 *      (1.a) Pointer into the tt.data file (position in the file - index from 0)
 *                type: int32
 *      (1.b) n (fTokenized.length) - number of tokens in the F phrase
 *                type: byte8
 *      (1.c) fTokenized[i] (0..n-1) - variable-length value for the
 *             index of the word in the vocabulary file. Indexed from 0
 *                type: unsignedIntVar40 (1 to 5 bytes. Low values - one byte)
 *
 * </pre>
 * When loading a TT, (A), (B) will be read into Vocabulary objects,
 * (C) will be stored in memory directly
 * and (D) will be parsed and mapped into a HashMap.
 *<br><br>
 * The files are already pre-filtered, pre-sorted.
 *
 */
public class TranslationTableToolsExtra {

    private static final Integer ZERO = new Integer(0);

    public static HashMap<ByteArrayHasher, MutableIntPair> readNioIndexB(String fileName, int sizeFile) throws IOException {
        HashMap<ByteArrayHasher, MutableIntPair> hash = new HashMap<ByteArrayHasher, MutableIntPair>();
        FileChannel fcIdx = new FileInputStream(fileName).getChannel();
        MutableIntPair lastPos = null;
        MappedByteBuffer map = fcIdx.map(FileChannel.MapMode.READ_ONLY, 0, new File(fileName).length());
        while (map.hasRemaining()) {
            int position = map.getInt();
            int n = map.get();
            ByteBuffer bb = ByteBuffer.allocate(LanguageConstants.SIZEOF_INT * n);
            for (int i = 0; i < n; i++) {
                int idxInVocabulary = NioBuffers.decodeVariableLengthInteger(map);
                bb.putInt(idxInVocabulary);
            }
            if (lastPos != null) lastPos.second = position;
            lastPos = new MutableIntPair(position, -1);
            hash.put(new ByteArrayHasher(bb.array()), lastPos);
        }
        fcIdx.close();
        if (lastPos != null) lastPos.second = sizeFile;
        System.err.println("Loaded " + hash.size() + " entries from " + fileName);
        return hash;
    }

    public static TableLine[] extract(byte[] bufB, EToken[] idToEToken) throws IOException {
        if (bufB.length == 0) return null;
        return extract(ByteBuffer.wrap(bufB), idToEToken, ZERO);
    }

    public static TableLine[] extract(ByteBuffer buf, EToken[] idToEToken, Integer pos) throws IOException {
        if (pos == null) return null;
        buf.position(pos);
        int n = buf.get();
        float[] prob = new float[n];
        for (int i = 0; i < prob.length; i++) prob[i] = buf.getFloat();
        TableLine[] line = new TableLine[n];
        for (int i = 0; i < line.length; i++) {
            int m = buf.get();
            EToken[] e = new EToken[m];
            for (int j = 0; j < e.length; j++) {
                int id = NioBuffers.decodeVariableLengthInteger(buf);
                e[j] = idToEToken[id];
            }
            line[i] = new TableLine(e, null, prob[i], null);
        }
        return line;
    }

    public static HashMap<Object, Integer> readNioIndex(String fileName, FToken[] idToFToken, EFProcessorIf processor, TranslationTable tt) throws IOException {
        HashMap<Object, Integer> hash = new HashMap<Object, Integer>();
        FileChannel fcIdx = new FileInputStream(fileName).getChannel();
        MappedByteBuffer map = fcIdx.map(FileChannel.MapMode.READ_ONLY, 0, new File(fileName).length());
        while (map.hasRemaining()) {
            int position = map.getInt();
            int n = map.get();
            FToken[] f = new FToken[n];
            for (int i = 0; i < f.length; i++) {
                int idxInVocabulary = NioBuffers.decodeVariableLengthInteger(map);
                f[i] = idToFToken[idxInVocabulary];
            }
            hash.put(processor.getKey(f, tt), position);
        }
        fcIdx.close();
        System.err.println("Loaded " + hash.size() + " entries from " + fileName);
        return hash;
    }

    public static int writeNioFiles(String[] f, SpecialTableLine[] lines, int posE, FileChannel fcE, FileChannel fcIdx, Vocabulary vF, Vocabulary vE) {
        try {
            {
                ByteBuffer bbuff = ByteBuffer.allocate(256);
                bbuff.putInt(posE);
                bbuff.put((byte) f.length);
                for (int i = 0; i < f.length; i++) NioBuffers.encodeVariableLengthInteger(bbuff, vF.get(f[i]));
                bbuff.flip();
                fcIdx.write(bbuff);
            }
            {
                ByteBuffer bbuff = ByteBuffer.allocate(1024);
                bbuff.put((byte) lines.length);
                for (int i = 0; i < lines.length; i++) bbuff.putFloat((float) lines[i].logProbabilityTotal);
                for (int i = 0; i < lines.length; i++) {
                    bbuff.put((byte) lines[i].e.length);
                    for (int j = 0; j < lines[i].e.length; j++) {
                        int id = vE.get(lines[i].e[j]);
                        NioBuffers.encodeVariableLengthInteger(bbuff, id);
                    }
                }
                bbuff.flip();
                posE += fcE.write(bbuff);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return posE;
    }

    public static void readVocab(String vocabFile, String encoding, Vocabulary v) throws IOException {
        BufferedReader inputFile = new BufferedReader(new InputStreamReader(new FileInputStream(vocabFile), encoding));
        String lineFile;
        while ((lineFile = inputFile.readLine()) != null) v.add(StringTools.substringBefore(lineFile, " "));
        inputFile.close();
    }

    public static interface LineCollector {

        public void collect(String[] f, SpecialTableLine[] lines);
    }

    public static class SpecialTableLine {

        private SpecialTableLine(String[] e, double logProbabilityTotal, double[] probs, Object alignment) {
            this.e = e;
            this.logProbabilityTotal = logProbabilityTotal;
            this.probs = probs;
            this.alignment = alignment;
        }

        public final String[] e;

        public final double logProbabilityTotal;

        public final double[] probs;

        public final Object alignment;
    }

    public static void parseSortedTextTranslationTable(LineCollector collector, BufferedReader inputFile, double logTableThreshold, double[] thresholdWeights, int tableLimit, int maxPhraseLength, boolean doSortAndPruning, MutableInt size, MutableInt sizeFinal) throws IOException, NumberFormatException {
        System.err.println("Load TT from sorted file...");
        String lastF = null, lineFile;
        ArrayList<TranslationTableTools.UnparsedTableLine> cummulated = new ArrayList<TranslationTableTools.UnparsedTableLine>();
        while ((lineFile = inputFile.readLine()) != null) {
            size.value++;
            String f = StringTools.substringBefore(lineFile, " ||| ");
            if (!f.equals(lastF)) commitVectorX(collector, cummulated, lastF, logTableThreshold, thresholdWeights, tableLimit, maxPhraseLength, sizeFinal, doSortAndPruning);
            double[] logProb = TranslationTableTools.getProbVector(lineFile);
            double logProbabilityTotal = PhramerTools.getWeight(thresholdWeights, logProb);
            if (logProbabilityTotal >= logTableThreshold) cummulated.add(new TranslationTableTools.UnparsedTableLine(lineFile, logProb, logProbabilityTotal));
            lastF = f;
        }
        commitVectorX(collector, cummulated, lastF, logTableThreshold, thresholdWeights, tableLimit, maxPhraseLength, sizeFinal, doSortAndPruning);
        inputFile.close();
    }

    private static void commitVectorX(LineCollector collector, ArrayList<TranslationTableTools.UnparsedTableLine> cummulated, String f, double logTableThreshold, double[] thresholdWeights, int tableLimit, int maxPhraseLength, MutableInt sizeFinal, boolean doSortAndPruning) {
        if (cummulated.size() > 0) {
            int len = StringTools.countTokensNormalized(f);
            if (len <= maxPhraseLength) {
                String[] fTokenized = StringTools.tokenize(f);
                SpecialTableLine[] vv;
                if (doSortAndPruning) {
                    Collections.sort(cummulated, new TranslationTableTools.UnparsedTableLineComparator());
                    vv = new SpecialTableLine[Math.min(cummulated.size(), tableLimit)];
                    for (int i = 0; i < vv.length; i++) vv[i] = insertOneFromUnparsedX(cummulated.get(i), sizeFinal);
                } else {
                    vv = new SpecialTableLine[cummulated.size()];
                    for (int i = 0; i < vv.length; i++) vv[i] = insertOneFromUnparsedX(cummulated.get(i), sizeFinal);
                }
                collector.collect(fTokenized, vv);
            }
            cummulated.clear();
        }
    }

    private static SpecialTableLine insertOneFromUnparsedX(TranslationTableTools.UnparsedTableLine l, MutableInt sizeFinal) {
        String lineFile = l.rawLineFile;
        lineFile = StringTools.substringAfter(lineFile, " ||| ");
        String e = StringTools.substringBefore(lineFile, " ||| ");
        lineFile = StringTools.substringAfter(lineFile, " ||| ");
        Object wordAlignment = null;
        if (lineFile.indexOf(" ||| ") != -1) {
            String wa = StringTools.substringBefore(lineFile, " ||| ");
            wordAlignment = WordAlignmentTools.encodeWordAlignment(wa);
        }
        sizeFinal.value++;
        return new SpecialTableLine(StringTools.tokenize(e), l.logProbabilityTotal, l.logProb, wordAlignment);
    }
}

package org.phramer.tools;

import info.olteanu.interfaces.*;
import info.olteanu.utils.*;
import info.olteanu.utils.chron.*;
import info.olteanu.utils.io.*;
import info.olteanu.utils.lang.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import org.phramer.*;
import org.phramer.lib.vocabulary.*;
import org.phramer.v1.decoder.*;
import org.phramer.v1.decoder.lm.*;
import org.phramer.v1.decoder.lm.ngram.*;
import org.phramer.v1.decoder.lm.preprocessor.*;
import org.phramer.v1.decoder.loader.*;
import org.phramer.v1.decoder.loader.custom.*;
import org.phramer.v1.decoder.math.*;
import org.phramer.v1.decoder.table.*;
import org.phramer.v1.decoder.token.*;

public class ConvertTT2BinaryNioBuffer implements LMLoader, TTLoader, TranslationTableToolsExtra.LineCollector {

    public LMLoader getCloneForConcurrencyLML() throws PhramerException {
        throw new PhramerException("Not implemented. Should be?");
    }

    public TTLoader getCloneForConcurrencyTTL() throws PhramerException {
        throw new PhramerException("Not implemented. Should be?");
    }

    private final Vocabulary vE, vF;

    private final String fileVocF, fileVocE;

    private boolean vLoaded;

    private FileOutputStream outE, outIdx;

    private FileChannel fcE, fcIdx;

    private int posE = 0;

    private int cntEntries = 0;

    public void collect(String[] f, TranslationTableToolsExtra.SpecialTableLine[] lines) {
        cntEntries++;
        posE = TranslationTableToolsExtra.writeNioFiles(f, lines, posE, fcE, fcIdx, vF, vE);
    }

    public LMPreprocessor loadPreprocessor(String lmFileName, int index) throws IOException, PhramerException {
        return new LMPreprocessorWord(StringFilter.VOID);
    }

    public LanguageModelIf loadLanguageModel(String lmURL, String encodingTextFile, int index) throws IOException, PhramerException {
        return new SimpleBackOffLM();
    }

    public TranslationTable loadTranslationTable(TokenBuilder tokenBuilder, String ttURL, String encodingTextFile, int ttLimit, int maxPhraseLength, double ttThreshold, double[] ttTresholdWeights, boolean storeDetails) throws IOException, PhramerException {
        MutableInt type = new MutableInt(0);
        MutableBool nio = new MutableBool();
        ttURL = LoaderSimpleImpl.parseMemoryTT(ttURL, type, nio);
        if (nio.value) throw new PhramerException("Cannot use java.nio.Buffer translation tables here!!!");
        if (!vLoaded) {
            System.err.println("Extracting vocabularies...");
            GetVocabulariesForTT.extractVocabulary(ttURL, fileVocF, fileVocE, maxPhraseLength, Integer.MAX_VALUE);
            TranslationTableToolsExtra.readVocab(fileVocF, encodingTextFile, vF);
            TranslationTableToolsExtra.readVocab(fileVocE, encodingTextFile, vE);
            vLoaded = true;
        }
        MutableInt size = new MutableInt(0);
        MutableInt sizeFinal = new MutableInt(0);
        Chronometer c = new Chronometer(true);
        TranslationTableToolsExtra.parseSortedTextTranslationTable(this, new BufferedReader(new InputStreamReader(IOTools.getInputStream(ttURL), encodingTextFile)), MathTools.numberToLog(ttThreshold), ttTresholdWeights, ttLimit, maxPhraseLength, true, size, sizeFinal);
        System.err.println("Translation table loaded in " + StringTools.formatDouble(c.getValue() * 0.001, "0.0#") + " secs. Kept " + sizeFinal.value + "/" + size.value + " = " + StringTools.formatDouble(100.0 * sizeFinal.value / size.value, "0.0") + "%");
        System.err.println("F entries: " + cntEntries);
        return null;
    }

    public ConvertTT2BinaryNioBuffer(String outFile, String encodingTextFile) throws IOException {
        fileVocF = outFile + ".vocf";
        fileVocE = outFile + ".voce";
        vE = new Vocabulary();
        vF = new Vocabulary();
        vLoaded = new File(fileVocF).exists() && new File(fileVocE).exists();
        if (vLoaded) {
            TranslationTableToolsExtra.readVocab(fileVocE, encodingTextFile, vE);
            TranslationTableToolsExtra.readVocab(fileVocF, encodingTextFile, vF);
        }
        outE = new FileOutputStream(outFile + ".data");
        fcE = outE.getChannel();
        outIdx = new FileOutputStream(outFile + ".idx");
        fcIdx = outIdx.getChannel();
    }

    public static void main(String[] args) throws Exception {
        LoaderSimpleImpl loader = new LoaderSimpleImpl();
        ConvertTT2BinaryNioBuffer my = new ConvertTT2BinaryNioBuffer(args[0], System.getProperty("file.encoding"));
        new PhramerConfig(StringTools.cutFirst(args, 1), new PhramerHelperCustom(loader, new TokenBuilderWordOnly(null), my, my, loader), null);
        my.close();
        System.out.println("End");
    }

    private static void testEncoding() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(1000);
        for (int i = 0; i < 1000000; i++) {
            NioBuffers.encodeVariableLengthInteger(b, i);
            b.flip();
            int v = NioBuffers.decodeVariableLengthInteger(b);
            if (v != i) throw new Error("Bad encoding for " + i);
            b.clear();
            if (i % 100000 == 0) {
                System.err.print(".");
                System.err.flush();
            }
        }
        for (int i = 0; i <= Integer.MAX_VALUE && i >= 0; i += 15) {
            NioBuffers.encodeVariableLengthInteger(b, i);
            b.flip();
            int v = NioBuffers.decodeVariableLengthInteger(b);
            if (v != i) throw new Error("Bad encoding for " + i + " (" + v + ")");
            b.clear();
            if (i % 10000000 == 0) {
                System.err.print(".");
                System.err.flush();
            }
        }
        System.err.println();
    }

    private void close() {
        try {
            fcE.close();
        } catch (IOException e) {
        }
        try {
            outE.close();
        } catch (IOException e) {
        }
        try {
            fcIdx.close();
        } catch (IOException e) {
        }
        try {
            outIdx.close();
        } catch (IOException e) {
        }
    }
}

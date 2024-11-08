package uk.ac.gla.terrier.structures.indexing;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import uk.ac.gla.terrier.structures.BlockDirectIndexInputStream;
import uk.ac.gla.terrier.structures.BlockLexiconInputStream;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.LexiconOutputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.Files;

/**
 * Builds an inverted index saving term-block information. It optionally saves
 * term-field information as well.
 * <p>
 * <b>Algorithm:</b>
 * <ol>
 * <li>While there are terms left:
 * <ol>
 * <li>Read M term ids from lexicon, in lexicographical order</li>
 * <li>Read the occurrences of these M terms into memory from the direct file</li>
 * <li>Write the occurrences of these M terms to the inverted file</li>
 * </ol>
 * <li>Rewrite the lexicon, removing block frequencies, and adding inverted
 * file offsets</li>
 * <li>Write the collection statistics</li>
 * </ol>
 * <p>
 * <b>Lexicon term selection:</b> There are two strategies of selecting the
 * number of terms to read from the lexicon. The trade-off here is to read a
 * small enough number of terms into memory such that the occurrences of all
 * those terms from the direct file can fit in memory. On the other hand, the
 * less terms that are read implies more iterations, which is I/O expensive, as
 * the entire direct file has to be read for every iteration.<br>
 * The two strategies are:
 * <ul>
 * <li>Read a fixed number of terms on each iterations - this corresponds to
 * the property <tt>invertedfile.processterms</tt></li>
 * <li>Read a fixed number of occurrences (pointers) on each iteration. The
 * number of pointers can be determined using the sum of frequencies of each
 * term from the lexicon. This corresponds to the property
 * <tt>invertedfile.processpointers</tt>. </li></ul>
 *  By default, the 2nd 
 * strategy is chosen, unless the <tt>invertedfile.processpointers</tt> has a
 * zero value specified.
 * <p>
 * <b>Properties:</b>
 * <ul>
 * <li><tt>invertedfile.processterms</tt> - the number of terms to process in
 * each iteration. Defaults to 25,000</li>
 * <li><tt>invertedfile.processpointers</tt> - the number of pointers to
 * process in each iteration. Defaults to 2,000,000, which specifies that
 * invertedfile.processterms should be read from the lexicon, regardless of the
 * number of pointers.</li>
 * </ul>
 * 
 * @author Douglas Johnson &amp; Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.40 $
 */
public class BlockInvertedIndexBuilder extends InvertedIndexBuilder {

    private static Logger logger = Logger.getRootLogger();

    protected String finalLexiconClass = "uk.ac.gla.terrier.structures.Lexicon";

    protected String finalLexiconInputStreamClass = "uk.ac.gla.terrier.structures.LexiconInputStream";

    /**
	 * Creates an instance of the BlockInvertedIndex class.
	 * @deprecated
	 */
    public BlockInvertedIndexBuilder() {
        this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
    }

    /**
	 * Creates an instance of the BlockInvertedIndex class using the given
	 * filename.
	 * 
	 * @param filename
	 *			the name of the inverted file
	 * @deprecated use this() or this(String, String) instead
	 */
    public BlockInvertedIndexBuilder(String filename) {
        super(filename);
        lexiconInputStream = BlockLexiconInputStream.class;
        lexiconOutputStream = LexiconOutputStream.class;
    }

    /**
	 * @deprecated
	 */
    public BlockInvertedIndexBuilder(String path, String prefix) {
        super(path, prefix);
        lexiconInputStream = BlockLexiconInputStream.class;
        lexiconOutputStream = LexiconOutputStream.class;
    }

    public BlockInvertedIndexBuilder(Index index) {
        super(index);
        lexiconInputStream = BlockLexiconInputStream.class;
        lexiconOutputStream = LexiconOutputStream.class;
    }

    /**
	 * This method creates the block html inverted index. The approach used is
	 * described briefly: for a group of M terms from the lexicon we build the
	 * inverted file and save it on disk. In this way, the number of times we
	 * need to read the direct file is related to the parameter M, and
	 * consequently to the size of the available memory.
	 */
    public void createInvertedIndex() {
        numberOfPointersPerIteration = Integer.parseInt(ApplicationSetup.getProperty("invertedfile.processpointers", "2000000"));
        processTerms = Integer.parseInt(ApplicationSetup.getProperty("invertedfile.processterms", "25000"));
        try {
            Runtime r = Runtime.getRuntime();
            logger.info("creating block inverted index");
            final String LexiconFilename = indexPathPrefix + ApplicationSetup.LEXICONSUFFIX;
            final String DocumentIndexFilename = indexPathPrefix + ApplicationSetup.DOC_INDEX_SUFFIX;
            DocumentIndex docIndex = new DocumentIndex(DocumentIndexFilename);
            final int numberOfDocuments = docIndex.getNumberOfDocuments();
            docIndex.close();
            long assumedNumberOfPointers = Long.parseLong(index.getIndexProperty("num.Pointers", "0"));
            long numberOfTokens = 0;
            long numberOfPointers = 0;
            BlockLexiconInputStream lexiconStream = (BlockLexiconInputStream) getLexInputStream(LexiconFilename);
            numberOfUniqueTerms = lexiconStream.numberOfEntries();
            DataOutputStream dos = new DataOutputStream(Files.writeFileStream(LexiconFilename.concat(".tmp2")));
            if (processTerms > numberOfUniqueTerms) processTerms = (int) numberOfUniqueTerms;
            long startProcessingLexicon = 0;
            long startTraversingDirectFile = 0;
            long startWritingInvertedFile = 0;
            long numberOfPointersThisIteration = 0;
            int i = 0;
            int iterationCounter = 0;
            String iteration_message_suffix = null;
            if (numberOfPointersPerIteration > 0) {
                if (assumedNumberOfPointers > 0) {
                    iteration_message_suffix = " of " + ((assumedNumberOfPointers % numberOfPointersPerIteration == 0) ? (assumedNumberOfPointers / numberOfPointersPerIteration) : 1 + (assumedNumberOfPointers / numberOfPointersPerIteration)) + " iterations";
                } else {
                    iteration_message_suffix = "";
                }
            } else {
                iteration_message_suffix = " of " + ((numberOfUniqueTerms % processTerms == 0) ? (numberOfUniqueTerms / processTerms) : 1 + (numberOfUniqueTerms / processTerms)) + " iterations";
            }
            if (numberOfPointersPerIteration == 0) {
                logger.warn("Using old-fashioned number of terms strategy. Please consider setting invertedfile.processpointers for forward compatible use");
            }
            while (i < numberOfUniqueTerms) {
                iterationCounter++;
                TIntIntHashMap codesHashMap = null;
                TIntArrayList[][] tmpStorage = null;
                IntLongTuple results = null;
                logger.info("Iteration " + iterationCounter + iteration_message_suffix);
                startProcessingLexicon = System.currentTimeMillis();
                if (numberOfPointersPerIteration > 0) {
                    logger.info("Scanning lexicon for " + numberOfPointersPerIteration + " pointers");
                    codesHashMap = new TIntIntHashMap();
                    ArrayList<TIntArrayList[]> tmpStorageStorage = new ArrayList<TIntArrayList[]>();
                    results = scanLexiconForPointers(numberOfPointersPerIteration, lexiconStream, codesHashMap, tmpStorageStorage);
                    tmpStorage = (TIntArrayList[][]) tmpStorageStorage.toArray(new TIntArrayList[0][0]);
                } else {
                    tmpStorage = new TIntArrayList[processTerms][];
                    codesHashMap = new TIntIntHashMap(processTerms);
                    results = scanLexiconForTerms(processTerms, lexiconStream, codesHashMap, tmpStorage);
                }
                processTerms = results.Terms;
                numberOfPointersThisIteration = results.Pointers;
                numberOfPointers += results.Pointers;
                i += processTerms;
                logger.info("time to process part of lexicon: " + ((System.currentTimeMillis() - startProcessingLexicon) / 1000D));
                InvertedIndexBuilder.displayMemoryUsage(r);
                startTraversingDirectFile = System.currentTimeMillis();
                traverseDirectFile(codesHashMap, tmpStorage);
                logger.info("time to traverse direct file: " + ((System.currentTimeMillis() - startTraversingDirectFile) / 1000D));
                InvertedIndexBuilder.displayMemoryUsage(r);
                startWritingInvertedFile = System.currentTimeMillis();
                numberOfTokens += writeInvertedFilePart(dos, tmpStorage, processTerms);
                logger.info("time to write inverted file: " + ((System.currentTimeMillis() - startWritingInvertedFile) / 1000D));
                InvertedIndexBuilder.displayMemoryUsage(r);
                logger.info("time to perform one iteration: " + ((System.currentTimeMillis() - startProcessingLexicon) / 1000D));
                logger.info("number of pointers processed: " + numberOfPointersThisIteration);
                tmpStorage = null;
                codesHashMap.clear();
                codesHashMap = null;
            }
            logger.info("Finished generating inverted file, rewriting lexicon");
            this.numberOfDocuments = numberOfDocuments;
            this.numberOfUniqueTerms = numberOfUniqueTerms;
            this.numberOfTokens = numberOfTokens;
            this.numberOfPointers = numberOfPointers;
            file.close();
            lexiconStream.close();
            dos.close();
            BlockLexiconInputStream lis = (BlockLexiconInputStream) getLexInputStream(LexiconFilename);
            LexiconOutputStream los = getLexOutputStream(LexiconFilename.concat(".tmp3"));
            DataInputStream dis = new DataInputStream(Files.openFileStream(LexiconFilename.concat(".tmp2")));
            while (lis.readNextEntryBytes() != -1) {
                los.writeNextEntry(lis.getTermCharacters(), lis.getTermId(), lis.getNt(), dis.readInt(), dis.readLong(), dis.readByte());
            }
            lis.close();
            los.close();
            dis.close();
            if (!Files.delete(LexiconFilename)) logger.error("delete file .lex failed!");
            if (!Files.delete(LexiconFilename.concat(".tmp2"))) logger.error("delete file .lex.tmp2 failed!");
            if (!Files.rename(LexiconFilename.concat(".tmp3"), LexiconFilename)) logger.error("rename file .lex.tmp3 to .lex failed!");
            index.addIndexStructure("lexicon", finalLexiconClass);
            index.addIndexStructureInputStream("lexicon", finalLexiconInputStreamClass);
            index.addIndexStructure("inverted", "uk.ac.gla.terrier.structures.BlockInvertedIndex", "uk.ac.gla.terrier.structures.Lexicon,java.lang.String,java.lang.String", "lexicon,path,prefix");
            index.addIndexStructureInputStream("inverted", "uk.ac.gla.terrier.structures.BlockInvertedIndexInputStream", "java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.LexiconInputStream", "path,prefix,lexicon-inputstream");
            index.setIndexProperty("num.inverted.fields.bits", "" + FieldScore.FIELDS_COUNT);
            index.setIndexProperty("num.Terms", "" + numberOfUniqueTerms);
            index.setIndexProperty("num.Tokens", "" + numberOfTokens);
            index.setIndexProperty("num.Pointers", "" + numberOfPointers);
            System.gc();
        } catch (IOException ioe) {
            logger.error("IOException occured during creating the inverted file. Stack trace follows.", ioe);
        }
    }

    /**
	 * Iterates through the lexicon, until it has reached the given number of
	 * pointers
	 * 
	 * @param PointersToProcess
	 *			Number of pointers to stop reading the lexicon after
	 * @param blexiconStream
	 *			the lexicon input stream to read
	 * @param codesHashMap
	 * @param tmpStorageStorage
	 * @return
	 */
    protected IntLongTuple scanLexiconForPointers(final long PointersToProcess, final LexiconInputStream blexiconStream, final TIntIntHashMap codesHashMap, final ArrayList<TIntArrayList[]> tmpStorageStorage) throws IOException {
        final BlockLexiconInputStream lexiconStream = (BlockLexiconInputStream) blexiconStream;
        int processTerms = 0;
        long numberOfPointersThisIteration = 0;
        long numberOfBlocksThisIteration = 0;
        int j = 0;
        while (numberOfPointersThisIteration < PointersToProcess) {
            if (lexiconStream.readNextEntry() == -1) break;
            processTerms++;
            TIntArrayList[] tmpArray = new TIntArrayList[5];
            final int tmpNT = lexiconStream.getNt();
            tmpArray[0] = new TIntArrayList(tmpNT);
            tmpArray[1] = new TIntArrayList(tmpNT);
            tmpArray[2] = new TIntArrayList(tmpNT);
            tmpArray[3] = new TIntArrayList(tmpNT);
            tmpArray[4] = new TIntArrayList(lexiconStream.getBlockFrequency());
            numberOfPointersThisIteration += tmpNT;
            numberOfBlocksThisIteration += lexiconStream.getBlockFrequency();
            tmpStorageStorage.add(tmpArray);
            codesHashMap.put(lexiconStream.getTermId(), j + 1);
            j++;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(numberOfPointersThisIteration + " pointers == " + processTerms + " terms == " + numberOfBlocksThisIteration + " blocks");
        }
        return new IntLongTuple(processTerms, numberOfPointersThisIteration);
    }

    /**
	 * Iterates through the lexicon, until it has reached the given number of
	 * terms
	 * 
	 * @param processTerms
	 *			Number of terms to stop reading the lexicon after
	 * @param blexiconStream
	 *			the lexicon input stream to read
	 * @param codesHashMap
	 * @param tmpStorageStorage
	 * @return
	 */
    protected IntLongTuple scanLexiconForTerms(final int processTerms, final LexiconInputStream blexiconStream, final TIntIntHashMap codesHashMap, TIntArrayList[][] tmpStorage) throws IOException {
        final BlockLexiconInputStream lexiconStream = (BlockLexiconInputStream) blexiconStream;
        int j = 0;
        long numberOfBlocksThisIteration = 0;
        long numberOfPointersThisIteration = 0;
        for (; j < processTerms; j++) {
            if (lexiconStream.readNextEntry() == -1) break;
            TIntArrayList[] tmpArray = new TIntArrayList[5];
            final int tmpNT = lexiconStream.getNt();
            tmpArray[0] = new TIntArrayList(tmpNT);
            tmpArray[1] = new TIntArrayList(tmpNT);
            tmpArray[2] = new TIntArrayList(tmpNT);
            tmpArray[3] = new TIntArrayList(tmpNT);
            tmpArray[4] = new TIntArrayList(lexiconStream.getBlockFrequency());
            numberOfPointersThisIteration += tmpNT;
            numberOfBlocksThisIteration += lexiconStream.getBlockFrequency();
            tmpStorage[j] = tmpArray;
            codesHashMap.put(lexiconStream.getTermId(), j + 1);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(numberOfPointersThisIteration + " pointers == " + j + " terms == " + numberOfBlocksThisIteration + " blocks");
        }
        return new IntLongTuple(j, numberOfPointersThisIteration);
    }

    /**
	 * Traverses the direct fies recording all occurrences of terms noted in
	 * codesHashMap into tmpStorage.
	 * 
	 * @param codesHashMap
	 *			contains the term ids that are being processed in this
	 *			iteration, as keys. Values are the corresponding index in
	 *			tmpStorage that information about this terms should be placed.
	 * @param tmpStorage
	 *			Records the occurrences information. First dimension is for
	 *			each term, as of the index given by codesHashMap; Second
	 *			dimension contains 5 TIntArrayLists : (document id, term
	 *			frequency, field score, block frequencies, block ids).
	 */
    protected void traverseDirectFile(TIntIntHashMap codesHashMap, TIntArrayList[][] tmpStorage) throws IOException {
        BlockDirectIndexInputStream directInputStream = index != null ? (BlockDirectIndexInputStream) index.getIndexStructureInputStream("direct") : new BlockDirectIndexInputStream(indexPath, indexPrefix);
        int[][] documentTerms = null;
        int p = 0;
        while ((documentTerms = directInputStream.getNextTerms()) != null) {
            p += directInputStream.getDocumentsSkipped();
            int[] documentTerms0 = documentTerms[0];
            int[] termfreqs = documentTerms[1];
            int[] htmlscores = documentTerms[2];
            int[] blockfreqs = documentTerms[3];
            int[] blockids = documentTerms[4];
            final int length = documentTerms0.length;
            int blockfreq;
            int blockidstart;
            int blockidend;
            for (int k = 0; k < length; k++) {
                int codePairIndex = codesHashMap.get(documentTerms0[k]);
                if (codePairIndex > 0) {
                    codePairIndex--;
                    TIntArrayList[] tmpMatrix = tmpStorage[codePairIndex];
                    tmpMatrix[0].add(p);
                    tmpMatrix[1].add(termfreqs[k]);
                    tmpMatrix[2].add(htmlscores[k]);
                    blockfreq = blockfreqs[k];
                    tmpMatrix[3].add(blockfreq);
                    blockidstart = 0;
                    if (k > 0) {
                        for (int l = 0; l < k; l++) blockidstart += blockfreqs[l];
                    }
                    blockidend = blockidstart + blockfreq;
                    for (int l = blockidstart; l < blockidend; l++) {
                        tmpMatrix[4].add(blockids[l]);
                    }
                }
            }
            p++;
        }
        directInputStream.close();
    }

    /**
	 * Writes the section of the inverted file
	 * 
	 * @param dos
	 *			a temporary data structure that contains the offsets in the
	 *			inverted index for each term.
	 * @param tmpStorage
	 *			Occurrences information, as described in traverseDirectFile().
	 *			This data is consumed by this method - once this method has
	 *			been called, all the data in tmpStorage will be destroyed.
	 * @param processTerms
	 *			The number of terms being processed in this iteration.
	 * @returns the number of tokens processed in this iteration
	 */
    protected long writeInvertedFilePart(final DataOutputStream dos, TIntArrayList[][] tmpStorage, final int processTerms) throws IOException {
        int frequency;
        long numTokens = 0;
        for (int j = 0; j < processTerms; j++) {
            frequency = 0;
            TIntArrayList[] tmpMatrix = tmpStorage[j];
            int[] tmpMatrix0 = tmpMatrix[0].toNativeArray();
            tmpMatrix[0] = null;
            int[] tmpMatrix1 = tmpMatrix[1].toNativeArray();
            tmpMatrix[1] = null;
            int[] tmpMatrix2 = tmpMatrix[2].toNativeArray();
            tmpMatrix[2] = null;
            int[] tmpMatrix3 = tmpMatrix[3].toNativeArray();
            tmpMatrix[3] = null;
            int[] tmpMatrix4 = tmpMatrix[4].toNativeArray();
            tmpMatrix[4] = null;
            tmpMatrix = null;
            tmpStorage[j] = null;
            int docid = tmpMatrix0[0];
            file.writeGamma(docid + 1);
            int termfreq = tmpMatrix1[0];
            frequency += termfreq;
            file.writeUnary(termfreq);
            file.writeBinary(FieldScore.FIELDS_COUNT, tmpMatrix2[0]);
            int blockfreq = tmpMatrix3[0];
            file.writeUnary(blockfreq + 1);
            int blockid = tmpMatrix4[0];
            file.writeGamma(blockid + 1);
            for (int l = 1; l < blockfreq; l++) {
                file.writeGamma(tmpMatrix4[l] - blockid);
                blockid = tmpMatrix4[l];
            }
            int blockindex = blockfreq;
            for (int k = 1; k < tmpMatrix0.length; k++) {
                file.writeGamma(tmpMatrix0[k] - docid);
                docid = tmpMatrix0[k];
                termfreq = tmpMatrix1[k];
                frequency += termfreq;
                file.writeUnary(termfreq);
                file.writeBinary(FieldScore.FIELDS_COUNT, tmpMatrix2[k]);
                blockfreq = tmpMatrix3[k];
                file.writeUnary(blockfreq + 1);
                blockid = tmpMatrix4[blockindex];
                file.writeGamma(blockid + 1);
                blockindex++;
                for (int l = 1; l < blockfreq; l++) {
                    file.writeGamma(tmpMatrix4[blockindex] - blockid);
                    blockid = tmpMatrix4[blockindex];
                    blockindex++;
                }
            }
            long endOffset = file.getByteOffset();
            byte endBitOffset = file.getBitOffset();
            endBitOffset--;
            if (endBitOffset < 0 && endOffset > 0) {
                endBitOffset = 7;
                endOffset--;
            }
            numTokens += frequency;
            dos.writeInt(frequency);
            dos.writeLong(endOffset);
            dos.writeByte(endBitOffset);
            tmpMatrix0 = tmpMatrix1 = tmpMatrix2 = tmpMatrix3 = tmpMatrix4 = null;
        }
        return numTokens;
    }
}

package edu.jhu.sa.util.suffix_array;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import edu.jhu.sa.util.sentence.Vocabulary;

/**
 *
 * 
 * @author Lane Schwartz
 */
public class MemoryMappedCorpusArray {

    private final Vocabulary vocabulary;

    private final MappedByteBuffer binaryCorpusBuffer;

    private final MappedByteBuffer binarySentenceBuffer;

    private final int numberOfWords;

    private final int numberOfSentences;

    public MemoryMappedCorpusArray(Vocabulary vocabulary, String binaryCorpusFileName, int binaryCorpusFileSize, String binarySentenceFileName, int binarySentenceFileSize) throws IOException {
        this.vocabulary = vocabulary;
        RandomAccessFile binarySentenceFile = new RandomAccessFile(binarySentenceFileName, "r");
        FileChannel binarySentenceFileChannel = binarySentenceFile.getChannel();
        this.binarySentenceBuffer = binarySentenceFileChannel.map(FileChannel.MapMode.READ_ONLY, 0, binarySentenceFileSize);
        RandomAccessFile binaryCorpusFile = new RandomAccessFile(binaryCorpusFileName, "r");
        FileChannel binaryCorpusFileChannel = binaryCorpusFile.getChannel();
        this.binaryCorpusBuffer = binaryCorpusFileChannel.map(FileChannel.MapMode.READ_ONLY, 0, binaryCorpusFileSize);
        this.numberOfWords = binaryCorpusFileSize;
        this.numberOfSentences = binarySentenceFileSize;
    }

    /**
	 * @return the number of words in the corpus. 
	 */
    public int size() {
        return numberOfWords;
    }

    /**
	 * @return the number of sentences in the corpus.
	 */
    public int getNumSentences() {
        return numberOfSentences;
    }

    public int getNumWords() {
        return numberOfWords;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    /**
	 * @returns the integer representation of the Word at the 
	 * specified position in the corpus.
	 */
    public int getWordID(int position) {
        return binaryCorpusBuffer.getInt(position * 4);
    }

    /**
	 * @return the position in the corpus of the first word of
	 * the specified sentence.  If the sentenceID is outside of the bounds 
	 * of the sentences, then it returns the last position in the corpus.
	 */
    public int getSentencePosition(int sentenceID) {
        if (sentenceID >= numberOfSentences) {
            return numberOfWords - 1;
        }
        return binarySentenceBuffer.getInt(sentenceID * 4);
    }
}

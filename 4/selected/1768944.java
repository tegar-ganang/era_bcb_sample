package joshua.sarray;

import joshua.corpus.SymbolTable;
import joshua.util.sentence.Vocabulary;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * 
 * @author Lane Schwartz
 */
public class MemoryMappedCorpusArray extends AbstractCorpus {

    private final IntBuffer binaryCorpusBuffer;

    private final IntBuffer binarySentenceBuffer;

    private final int numberOfWords;

    private final int numberOfSentences;

    /**
	 * Constructs a corpus array from a binary file.
	 * <p>
	 * The binary file contains both the vocabulary,
	 * which is read in first, and the corpus array data.
	 *  
	 * @param binaryFileName
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public MemoryMappedCorpusArray(String binaryFileName, String vocabFileName) throws IOException, ClassNotFoundException {
        this(Vocabulary.readExternal(vocabFileName), binaryFileName);
    }

    /**
	 * Constructs a corpus array from a binary file.
	 * <p>
	 * The binary file may or may not contain a vocabulary.
	 * The first integer in the file specifies a header length.
	 * If no vocabulary is contained, this value should be zero.
	 * <p>
	 * Even if the binary file contains a vocabulary, it is ignored,
	 * and the symbol table provided to the constructor is used instead.
	 * 
	 * @param symbolTable
	 * @param binaryFileName
	 * @throws IOException
	 */
    public MemoryMappedCorpusArray(SymbolTable symbolTable, String binaryFileName) throws IOException {
        super(symbolTable);
        IntBuffer tmp;
        RandomAccessFile binaryFile = new RandomAccessFile(binaryFileName, "r");
        FileChannel binaryChannel = binaryFile.getChannel();
        int headerSize = 0;
        tmp = binaryChannel.map(FileChannel.MapMode.READ_ONLY, headerSize, 4).asIntBuffer().asReadOnlyBuffer();
        this.numberOfSentences = tmp.get();
        this.binarySentenceBuffer = binaryChannel.map(FileChannel.MapMode.READ_ONLY, (headerSize + 4), 4 * numberOfSentences).asIntBuffer().asReadOnlyBuffer();
        tmp = binaryChannel.map(FileChannel.MapMode.READ_ONLY, (headerSize + 4 + 4 * numberOfSentences), 4).asIntBuffer().asReadOnlyBuffer();
        this.numberOfWords = tmp.get();
        this.binaryCorpusBuffer = binaryChannel.map(FileChannel.MapMode.READ_ONLY, (headerSize + 4 + 4 * numberOfSentences + 4), 4 * numberOfWords).asIntBuffer().asReadOnlyBuffer();
    }

    @Override
    public int getNumSentences() {
        return numberOfSentences;
    }

    @Override
    public int getSentenceIndex(int position) {
        int index = binarySearch(position);
        if (index >= 0) {
            return index;
        } else {
            return (index * (-1)) - 2;
        }
    }

    private int binarySearch(int value) {
        int low = 0;
        int high = numberOfSentences - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midValue = binarySentenceBuffer.get(mid);
            if (midValue < value) {
                low = mid + 1;
            } else if (midValue > value) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    @Override
    public int getSentencePosition(int sentenceID) {
        if (sentenceID >= numberOfSentences) {
            return numberOfWords;
        }
        return binarySentenceBuffer.get(sentenceID);
    }

    @Override
    public int getWordID(int position) {
        return binaryCorpusBuffer.get(position);
    }

    @Override
    public int size() {
        return numberOfWords;
    }

    public void write(String corpusFilename, String vocabFilename, String charset) throws IOException {
        throw new RuntimeException("Not yet implemented");
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        throw new RuntimeException("Not yet implemented");
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        throw new RuntimeException("Not yet implemented");
    }
}

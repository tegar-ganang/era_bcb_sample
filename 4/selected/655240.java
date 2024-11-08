package joshua.sarray;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import joshua.util.Cache;

public class MemoryMappedSuffixArray extends AbstractSuffixArray {

    private final IntBuffer binarySuffixBuffer;

    private final int size;

    public MemoryMappedSuffixArray(String suffixesFileName, String corpusFileName, String vocabFileName, int maxCacheSize) throws IOException, ClassNotFoundException {
        this(suffixesFileName, new MemoryMappedCorpusArray(corpusFileName, vocabFileName), maxCacheSize);
    }

    /** 
	 * Constructor takes a CorpusArray and creates a sorted
	 * suffix array from it.
	 * 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
    public MemoryMappedSuffixArray(String suffixesFileName, Corpus corpus, int maxCacheSize) throws IOException, ClassNotFoundException {
        super(corpus, new Cache<Pattern, MatchedHierarchicalPhrases>(maxCacheSize));
        RandomAccessFile binaryFile = new RandomAccessFile(suffixesFileName, "r");
        FileChannel binaryChannel = binaryFile.getChannel();
        IntBuffer tmp = binaryChannel.map(FileChannel.MapMode.READ_ONLY, 0, 4).asIntBuffer().asReadOnlyBuffer();
        size = tmp.get();
        if (size != corpus.size()) {
            throw new RuntimeException("Size of suffix array (" + size + ") size does not match size of corpus (" + corpus.size() + ")");
        }
        this.binarySuffixBuffer = binaryChannel.map(FileChannel.MapMode.READ_ONLY, 4, 4 * size).asIntBuffer().asReadOnlyBuffer();
    }

    @Override
    public int getCorpusIndex(int suffixIndex) {
        return binarySuffixBuffer.get(suffixIndex);
    }

    @Override
    public int size() {
        return size;
    }
}

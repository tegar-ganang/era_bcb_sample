package hpc.test;

import hpc.MergeOutputAdapterWriteableByteChannel;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Date;
import edu.cornell.lassp.houle.RngPack.RandomElement;
import edu.cornell.lassp.houle.RngPack.RandomShuffle;
import edu.cornell.lassp.houle.RngPack.Ranecu;
import edu.cornell.lassp.houle.RngPack.Ranmar;

public class MainTest {

    private static final String DEFAULT_OUTPUTFILE_NAME = "num.txt";

    private static final int DEFAULT_HOW_MANY_NUMBERS_TO_GENERATE = 1000;

    private static final int BLOCK_SIZE = 1000;

    public static final int DEFAULT_LOW_VALUE = 0;

    public static final int DEFAULT_HIGH_VALUE = Integer.MAX_VALUE;

    private int inputSize;

    private int low;

    private int high;

    /**
	 * Generate a file of random numbers into the file num.txt in the current
	 * directory. The domain for the numbers is 0 to 2<sup>31</sup>-1.
	 * 
	 * Supports one optional command-line argument: how many numbers to
	 * generate. If the argument is no supplied the program generates 1,000
	 * numbers by default.
	 * 
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        Integer inputSize = DEFAULT_HOW_MANY_NUMBERS_TO_GENERATE;
        if (args.length > 0) {
            inputSize = Integer.parseInt(args[0]);
        }
        MainTest test = new MainTest(inputSize);
        test.run();
    }

    public MainTest(int inputSize) {
        this(inputSize, DEFAULT_LOW_VALUE, DEFAULT_HIGH_VALUE);
    }

    public MainTest(int inputSize, int low, int high) {
        super();
        if (inputSize <= 0) throw new IllegalArgumentException("input size must be >0");
        this.inputSize = inputSize;
        this.low = low;
        this.high = high;
    }

    private void run() throws IOException {
        File destination = new File(DEFAULT_OUTPUTFILE_NAME);
        if (destination.exists()) destination.delete();
        ByteBuffer tempBuffer = ByteBuffer.allocate(16 * 1024);
        RandomAccessFile file = new RandomAccessFile(destination, "rw");
        MergeOutputAdapterWriteableByteChannel output = new MergeOutputAdapterWriteableByteChannel(tempBuffer, file.getChannel());
        try {
            RandomElement markov = new RandomShuffle(new Ranecu(new Date()), new Ranmar(new Date()), BLOCK_SIZE);
            final int HOW_MANY_BLOCKS = inputSize / BLOCK_SIZE;
            for (int i = 0; i < HOW_MANY_BLOCKS; i++) {
                for (int j = 0; j < BLOCK_SIZE; j++) {
                    int random = markov.choose(low, high);
                    output.put((long) random);
                }
            }
            final int REMAINDER = inputSize % BLOCK_SIZE;
            RandomElement markovRemainder = new RandomShuffle(new Ranecu(new Date()), new Ranmar(new Date()), REMAINDER);
            for (int i = 0; i < REMAINDER; i++) {
                int random = markovRemainder.choose(low, high);
                output.put((long) random);
            }
        } finally {
            if (null != output) output.close();
            if (null != file) file.close();
        }
    }
}

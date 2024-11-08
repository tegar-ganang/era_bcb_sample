package hzip.bw.compress;

import hzip.bw.Common;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.log4j.Logger;

public class BWCConfigurable extends MapReduceBase {

    private static final Logger logger = Logger.getLogger(BWCConfigurable.class);

    public static int MIN_ORDER_SIZE = 3;

    protected int blockSize = 0;

    protected int wordSize = 0;

    protected Path[] localFiles;

    protected RandomAccessFile raFile;

    protected FileChannel ioChannel;

    protected MappedByteBuffer buffer;

    protected String outputPath;

    FileSystem fs;

    protected int origFileSize;

    public void configure(JobConf job) {
        try {
            localFiles = DistributedCache.getLocalCacheFiles(job);
            File inFile = new File(localFiles[0].toUri().getPath());
            raFile = new RandomAccessFile(inFile, "r");
            ioChannel = raFile.getChannel();
            buffer = ioChannel.map(FileChannel.MapMode.READ_ONLY, 0L, ioChannel.size()).load();
            blockSize = job.getInt(Common.CONFIG_BLOCK_SIZE, -1);
            wordSize = job.getInt(Common.CONFIG_WORD_SIZE, 1);
            outputPath = job.getStrings(Common.CONFIG_OUTPUT_PATH)[0];
            origFileSize = job.getInt(Common.INPUT_FILE_SIZE, -1);
            fs = FileSystem.get(job);
        } catch (IOException e) {
            e.printStackTrace();
            buffer = null;
        }
    }

    public static final class IntArrayWritable extends ArrayWritable {

        public IntArrayWritable() {
            super(IntWritable.class);
        }
    }
}

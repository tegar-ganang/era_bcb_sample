package fi.tkk.ics.hadoop.bam.cli.plugins.chipster;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileAlreadyExistsException;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.LineReader;
import net.sf.samtools.util.BlockCompressedInputStream;
import net.sf.samtools.util.BlockCompressedStreamConstants;
import fi.tkk.ics.hadoop.bam.custom.hadoop.InputSampler;
import fi.tkk.ics.hadoop.bam.custom.hadoop.TotalOrderPartitioner;
import fi.tkk.ics.hadoop.bam.custom.jargs.gnu.CmdLineParser;
import fi.tkk.ics.hadoop.bam.custom.samtools.BlockCompressedOutputStream;
import static fi.tkk.ics.hadoop.bam.custom.jargs.gnu.CmdLineParser.Option.*;
import fi.tkk.ics.hadoop.bam.cli.CLIPlugin;
import fi.tkk.ics.hadoop.bam.cli.Utils;
import fi.tkk.ics.hadoop.bam.util.BGZFSplitFileInputFormat;
import fi.tkk.ics.hadoop.bam.util.Pair;
import fi.tkk.ics.hadoop.bam.util.Timer;
import fi.tkk.ics.hadoop.bam.util.WrapSeekable;

public final class SummarySort extends CLIPlugin {

    private static final List<Pair<CmdLineParser.Option, String>> optionDescs = new ArrayList<Pair<CmdLineParser.Option, String>>();

    private static final CmdLineParser.Option verboseOpt = new BooleanOption('v', "verbose"), outputDirOpt = new StringOption('o', "output-dir=PATH");

    public SummarySort() {
        super("summarysort", "sort summary file for zooming", "1.0", "WORKDIR INPATH", optionDescs, "Sorts the summary file in INPATH in a distributed fashion using " + "Hadoop. Output parts are placed in WORKDIR." + "\n\n" + "This is equivalent to one of the sorts done by the 'summarize' " + "plugin, if sorting is requested of it.");
    }

    static {
        optionDescs.add(new Pair<CmdLineParser.Option, String>(verboseOpt, "tell Hadoop jobs to be more verbose"));
        optionDescs.add(new Pair<CmdLineParser.Option, String>(outputDirOpt, "output complete summary files to the file PATH, " + "removing the parts from WORKDIR"));
    }

    @Override
    protected int run(CmdLineParser parser) {
        final List<String> args = parser.getRemainingArgs();
        if (args.isEmpty()) {
            System.err.println("summarysort :: WORKDIR not given.");
            return 3;
        }
        if (args.size() == 1) {
            System.err.println("summarysort :: INPATH not given.");
            return 3;
        }
        final String outS = (String) parser.getOptionValue(outputDirOpt);
        final Path wrkDir = new Path(args.get(0)), in = new Path(args.get(1)), out = outS == null ? null : new Path(outS);
        final boolean verbose = parser.getBoolean(verboseOpt);
        final Configuration conf = getConf();
        final Timer t = new Timer();
        try {
            @SuppressWarnings("deprecation") final int maxReduceTasks = new JobClient(new JobConf(conf)).getClusterStatus().getMaxReduceTasks();
            conf.setInt("mapred.reduce.tasks", Math.max(1, maxReduceTasks * 9 / 10));
            final Job job = sortOne(conf, in, wrkDir, "summarysort", "");
            System.out.printf("summarysort :: Waiting for job completion...\n");
            t.start();
            if (!job.waitForCompletion(verbose)) {
                System.err.println("summarysort :: Job failed.");
                return 4;
            }
            System.out.printf("summarysort :: Job complete in %d.%03d s.\n", t.stopS(), t.fms());
        } catch (IOException e) {
            System.err.printf("summarysort :: Hadoop error: %s\n", e);
            return 4;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (out != null) try {
            System.out.println("summarysort :: Merging output...");
            t.start();
            final FileSystem srcFS = wrkDir.getFileSystem(conf);
            final FileSystem dstFS = out.getFileSystem(conf);
            final OutputStream outs = dstFS.create(out);
            final FileStatus[] parts = srcFS.globStatus(new Path(wrkDir, in.getName() + "-[0-9][0-9][0-9][0-9][0-9][0-9]*"));
            {
                int i = 0;
                final Timer t2 = new Timer();
                for (final FileStatus part : parts) {
                    t2.start();
                    final InputStream ins = srcFS.open(part.getPath());
                    IOUtils.copyBytes(ins, outs, conf, false);
                    ins.close();
                    System.out.printf("summarysort :: Merged part %d in %d.%03d s.\n", ++i, t2.stopS(), t2.fms());
                }
            }
            for (final FileStatus part : parts) srcFS.delete(part.getPath(), false);
            outs.write(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);
            outs.close();
            System.out.printf("summarysort :: Merging complete in %d.%03d s.\n", t.stopS(), t.fms());
        } catch (IOException e) {
            System.err.printf("summarysort :: Output merging failed: %s\n", e);
            return 5;
        }
        return 0;
    }

    static Job sortOne(Configuration conf, Path inputFile, Path outputDir, String commandName, String samplingInfo) throws IOException, ClassNotFoundException, InterruptedException {
        conf.set(SortOutputFormat.OUTPUT_NAME_PROP, inputFile.getName());
        Utils.configureSampling(inputFile, conf);
        final Job job = new Job(conf);
        job.setJarByClass(Summarize.class);
        job.setMapperClass(Mapper.class);
        job.setReducerClass(SortReducer.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        job.setInputFormatClass(SortInputFormat.class);
        job.setOutputFormatClass(SortOutputFormat.class);
        FileInputFormat.setInputPaths(job, inputFile);
        FileOutputFormat.setOutputPath(job, outputDir);
        job.setPartitionerClass(TotalOrderPartitioner.class);
        final Timer t = new Timer();
        System.out.printf("%s :: Sampling%s...\n", commandName, samplingInfo);
        t.start();
        InputSampler.<LongWritable, Text>writePartitionFile(job, new InputSampler.SplitSampler<LongWritable, Text>(1 << 16, 10));
        System.out.printf("%s :: Sampling complete in %d.%03d s.\n", commandName, t.stopS(), t.fms());
        job.submit();
        return job;
    }
}

final class SortReducer extends Reducer<LongWritable, Text, NullWritable, Text> {

    @Override
    protected void reduce(LongWritable ignored, Iterable<Text> records, Reducer<LongWritable, Text, NullWritable, Text>.Context<LongWritable, Text, NullWritable, Text> ctx) throws IOException, InterruptedException {
        for (Text rec : records) ctx.write(NullWritable.get(), rec);
    }
}

final class SortInputFormat extends BGZFSplitFileInputFormat<LongWritable, Text> {

    @Override
    public RecordReader<LongWritable, Text> createRecordReader(InputSplit split, TaskAttemptContext ctx) throws InterruptedException, IOException {
        final RecordReader<LongWritable, Text> rr = new SortRecordReader();
        rr.initialize(split, ctx);
        return rr;
    }
}

final class SortRecordReader extends RecordReader<LongWritable, Text> {

    private final LongWritable key = new LongWritable();

    private final BlockCompressedLineRecordReader lineRR = new BlockCompressedLineRecordReader();

    @Override
    public void initialize(InputSplit spl, TaskAttemptContext ctx) throws IOException {
        lineRR.initialize(spl, ctx);
    }

    @Override
    public void close() throws IOException {
        lineRR.close();
    }

    @Override
    public float getProgress() {
        return lineRR.getProgress();
    }

    @Override
    public LongWritable getCurrentKey() {
        return key;
    }

    @Override
    public Text getCurrentValue() {
        return lineRR.getCurrentValue();
    }

    @Override
    public boolean nextKeyValue() throws IOException, CharacterCodingException {
        if (!lineRR.nextKeyValue()) return false;
        Text line = getCurrentValue();
        int tabOne = line.find("\t");
        int rid = Integer.parseInt(line.decode(line.getBytes(), 0, tabOne));
        int tabTwo = line.find("\t", tabOne + 1);
        int posBeg = tabOne + 1;
        int posEnd = tabTwo - 1;
        int pos = Integer.parseInt(line.decode(line.getBytes(), posBeg, posEnd - posBeg + 1));
        key.set((long) rid << 32 | pos);
        return true;
    }
}

final class BlockCompressedLineRecordReader extends RecordReader<NullWritable, Text> {

    private long start;

    private long pos;

    private long end;

    private BlockCompressedInputStream bin;

    private LineReader in;

    private int maxLineLength;

    private Text value = new Text();

    public void initialize(InputSplit genericSplit, TaskAttemptContext context) throws IOException {
        Configuration conf = context.getConfiguration();
        this.maxLineLength = conf.getInt("mapred.linerecordreader.maxlength", Integer.MAX_VALUE);
        FileSplit split = (FileSplit) genericSplit;
        start = (split.getStart()) << 16;
        end = (start + split.getLength()) << 16;
        final Path file = split.getPath();
        FileSystem fs = file.getFileSystem(conf);
        bin = new BlockCompressedInputStream(new WrapSeekable<FSDataInputStream>(fs.open(file), fs.getFileStatus(file).getLen(), file));
        in = new LineReader(bin, conf);
        if (start != 0) {
            bin.seek(start);
            in.readLine(new Text());
            start = bin.getFilePointer();
        }
        this.pos = start;
    }

    public boolean nextKeyValue() throws IOException {
        while (pos <= end) {
            int newSize = in.readLine(value, maxLineLength);
            if (newSize == 0) return false;
            pos = bin.getFilePointer();
            if (newSize < maxLineLength) return true;
        }
        return false;
    }

    @Override
    public NullWritable getCurrentKey() {
        return NullWritable.get();
    }

    @Override
    public Text getCurrentValue() {
        return value;
    }

    @Override
    public float getProgress() {
        if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (pos - start) / (float) (end - start));
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}

final class SortOutputFormat extends TextOutputFormat<NullWritable, Text> {

    public static final String OUTPUT_NAME_PROP = "hadoopbam.summarysort.output.name";

    @Override
    public RecordWriter<NullWritable, Text> getRecordWriter(TaskAttemptContext ctx) throws IOException {
        Path path = getDefaultWorkFile(ctx, "");
        FileSystem fs = path.getFileSystem(ctx.getConfiguration());
        return new TextOutputFormat.LineRecordWriter<NullWritable, Text>(new DataOutputStream(new BlockCompressedOutputStream(fs.create(path))));
    }

    @Override
    public Path getDefaultWorkFile(TaskAttemptContext context, String ext) throws IOException {
        String filename = context.getConfiguration().get(OUTPUT_NAME_PROP);
        String extension = ext.isEmpty() ? ext : "." + ext;
        int part = context.getTaskAttemptID().getTaskID().getId();
        return new Path(super.getDefaultWorkFile(context, ext).getParent(), filename + "-" + String.format("%06d", part) + extension);
    }

    @Override
    public void checkOutputSpecs(JobContext job) throws FileAlreadyExistsException, IOException {
    }
}

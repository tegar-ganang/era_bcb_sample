package fi.tkk.ics.hadoop.bam.cli.plugins;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ChecksumFileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
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
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import net.sf.picard.sam.ReservedTagConstants;
import net.sf.samtools.util.BlockCompressedStreamConstants;
import fi.tkk.ics.hadoop.bam.custom.hadoop.InputSampler;
import fi.tkk.ics.hadoop.bam.custom.hadoop.TotalOrderPartitioner;
import fi.tkk.ics.hadoop.bam.custom.jargs.gnu.CmdLineParser;
import fi.tkk.ics.hadoop.bam.custom.samtools.BAMFileWriter;
import fi.tkk.ics.hadoop.bam.custom.samtools.SamFileHeaderMerger;
import fi.tkk.ics.hadoop.bam.custom.samtools.SAMFileHeader;
import fi.tkk.ics.hadoop.bam.custom.samtools.SAMFileReader;
import fi.tkk.ics.hadoop.bam.custom.samtools.SAMRecord;
import static fi.tkk.ics.hadoop.bam.custom.jargs.gnu.CmdLineParser.Option.*;
import fi.tkk.ics.hadoop.bam.BAMInputFormat;
import fi.tkk.ics.hadoop.bam.BAMRecordReader;
import fi.tkk.ics.hadoop.bam.KeyIgnoringBAMOutputFormat;
import fi.tkk.ics.hadoop.bam.SAMRecordWritable;
import fi.tkk.ics.hadoop.bam.cli.CLIPlugin;
import fi.tkk.ics.hadoop.bam.cli.Utils;
import fi.tkk.ics.hadoop.bam.util.Pair;
import fi.tkk.ics.hadoop.bam.util.Timer;

public final class Sort extends CLIPlugin {

    private static final List<Pair<CmdLineParser.Option, String>> optionDescs = new ArrayList<Pair<CmdLineParser.Option, String>>();

    private static final CmdLineParser.Option verboseOpt = new BooleanOption('v', "verbose"), outputFileOpt = new StringOption('o', "output-file=PATH");

    public Sort() {
        super("sort", "BAM sorting and merging", "2.0", "WORKDIR INPATH [INPATH...]", optionDescs, "Merges together the BAM files in the INPATHs, sorting the result, " + "in a distributed fashion using Hadoop. Output parts are placed in " + "WORKDIR.");
    }

    static {
        optionDescs.add(new Pair<CmdLineParser.Option, String>(verboseOpt, "tell the Hadoop job to be more verbose"));
        optionDescs.add(new Pair<CmdLineParser.Option, String>(outputFileOpt, "output a complete BAM file to the file PATH, " + "removing the parts from WORKDIR"));
    }

    @Override
    protected int run(CmdLineParser parser) {
        final List<String> args = parser.getRemainingArgs();
        if (args.isEmpty()) {
            System.err.println("sort :: WORKDIR not given.");
            return 3;
        }
        if (args.size() == 1) {
            System.err.println("sort :: INPATH not given.");
            return 3;
        }
        final String wrkDir = args.get(0), out = (String) parser.getOptionValue(outputFileOpt);
        final List<String> strInputs = args.subList(1, args.size());
        final List<Path> inputs = new ArrayList<Path>(strInputs.size());
        for (final String in : strInputs) inputs.add(new Path(in));
        final boolean verbose = parser.getBoolean(verboseOpt);
        final String intermediateOutName = out == null ? inputs.get(0).getName() : out;
        final Configuration conf = getConf();
        conf.setStrings(INPUT_PATHS_PROP, strInputs.toArray(new String[0]));
        conf.set(SortOutputFormat.OUTPUT_NAME_PROP, intermediateOutName);
        final Path wrkDirPath = new Path(wrkDir);
        final Timer t = new Timer();
        try {
            for (final Path in : inputs) Utils.configureSampling(in, conf);
            @SuppressWarnings("deprecation") final int maxReduceTasks = new JobClient(new JobConf(conf)).getClusterStatus().getMaxReduceTasks();
            conf.setInt("mapred.reduce.tasks", Math.max(1, maxReduceTasks * 9 / 10));
            final Job job = new Job(conf);
            job.setJarByClass(Sort.class);
            job.setMapperClass(Mapper.class);
            job.setReducerClass(SortReducer.class);
            job.setMapOutputKeyClass(LongWritable.class);
            job.setOutputKeyClass(NullWritable.class);
            job.setOutputValueClass(SAMRecordWritable.class);
            job.setInputFormatClass(BAMInputFormat.class);
            job.setOutputFormatClass(SortOutputFormat.class);
            for (final Path in : inputs) FileInputFormat.addInputPath(job, in);
            FileOutputFormat.setOutputPath(job, wrkDirPath);
            job.setPartitionerClass(TotalOrderPartitioner.class);
            System.out.println("sort :: Sampling...");
            t.start();
            InputSampler.<LongWritable, SAMRecordWritable>writePartitionFile(job, new InputSampler.IntervalSampler<LongWritable, SAMRecordWritable>(0.01, 100));
            System.out.printf("sort :: Sampling complete in %d.%03d s.\n", t.stopS(), t.fms());
            job.submit();
            System.out.println("sort :: Waiting for job completion...");
            t.start();
            if (!job.waitForCompletion(verbose)) {
                System.err.println("sort :: Job failed.");
                return 4;
            }
            System.out.printf("sort :: Job complete in %d.%03d s.\n", t.stopS(), t.fms());
        } catch (IOException e) {
            System.err.printf("sort :: Hadoop error: %s\n", e);
            return 4;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (out != null) try {
            System.out.println("sort :: Merging output...");
            t.start();
            final Path outPath = new Path(out);
            final FileSystem srcFS = wrkDirPath.getFileSystem(conf);
            FileSystem dstFS = outPath.getFileSystem(conf);
            if (dstFS instanceof LocalFileSystem && dstFS instanceof ChecksumFileSystem) dstFS = ((LocalFileSystem) dstFS).getRaw();
            final BAMFileWriter w = new BAMFileWriter(dstFS.create(outPath), new File(""));
            w.setSortOrder(SAMFileHeader.SortOrder.coordinate, true);
            w.setHeader(getHeaderMerger(conf).getMergedHeader());
            w.close();
            final OutputStream outs = dstFS.append(outPath);
            final FileStatus[] parts = srcFS.globStatus(new Path(wrkDir, conf.get(SortOutputFormat.OUTPUT_NAME_PROP) + "-[0-9][0-9][0-9][0-9][0-9][0-9]*"));
            {
                int i = 0;
                final Timer t2 = new Timer();
                for (final FileStatus part : parts) {
                    t2.start();
                    final InputStream ins = srcFS.open(part.getPath());
                    IOUtils.copyBytes(ins, outs, conf, false);
                    ins.close();
                    System.out.printf("sort :: Merged part %d in %d.%03d s.\n", ++i, t2.stopS(), t2.fms());
                }
            }
            for (final FileStatus part : parts) srcFS.delete(part.getPath(), false);
            outs.write(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);
            outs.close();
            System.out.printf("sort :: Merging complete in %d.%03d s.\n", t.stopS(), t.fms());
        } catch (IOException e) {
            System.err.printf("sort :: Output merging failed: %s\n", e);
            return 5;
        }
        return 0;
    }

    private static final String INPUT_PATHS_PROP = "hadoopbam.sort.input.paths";

    private static SamFileHeaderMerger headerMerger = null;

    public static SamFileHeaderMerger getHeaderMerger(Configuration conf) throws IOException {
        if (headerMerger != null) return headerMerger;
        final List<SAMFileHeader> headers = new ArrayList<SAMFileHeader>();
        for (final String in : conf.getStrings(INPUT_PATHS_PROP)) {
            final Path p = new Path(in);
            final SAMFileReader r = new SAMFileReader(p.getFileSystem(conf).open(p));
            headers.add(r.getFileHeader());
            r.close();
        }
        return headerMerger = new SamFileHeaderMerger(SAMFileHeader.SortOrder.coordinate, headers, true);
    }
}

final class SortReducer extends Reducer<LongWritable, SAMRecordWritable, NullWritable, SAMRecordWritable> {

    @Override
    protected void reduce(LongWritable ignored, Iterable<SAMRecordWritable> records, Reducer<LongWritable, SAMRecordWritable, NullWritable, SAMRecordWritable>.Context<LongWritable, SAMRecordWritable, NullWritable, SAMRecordWritable> ctx) throws IOException, InterruptedException {
        for (SAMRecordWritable rec : records) ctx.write(NullWritable.get(), rec);
    }
}

final class SortInputFormat extends BAMInputFormat {

    @Override
    public RecordReader<LongWritable, SAMRecordWritable> createRecordReader(InputSplit split, TaskAttemptContext ctx) throws InterruptedException, IOException {
        final RecordReader<LongWritable, SAMRecordWritable> rr = new SortRecordReader();
        rr.initialize(split, ctx);
        return rr;
    }
}

final class SortRecordReader extends BAMRecordReader {

    private SamFileHeaderMerger headerMerger;

    @Override
    public void initialize(InputSplit spl, TaskAttemptContext ctx) throws IOException {
        super.initialize(spl, ctx);
        headerMerger = Sort.getHeaderMerger(ctx.getConfiguration());
    }

    @Override
    public boolean nextKeyValue() {
        if (!super.nextKeyValue()) return false;
        final SAMRecord r = getCurrentValue().get();
        final SAMFileHeader h = r.getHeader();
        if (headerMerger.hasMergedSequenceDictionary()) {
            final int ri = headerMerger.getMergedSequenceIndex(h, r.getReferenceIndex());
            r.setReferenceIndex(ri);
            if (r.getReadPairedFlag()) r.setMateReferenceIndex(headerMerger.getMergedSequenceIndex(h, r.getMateReferenceIndex()));
            getCurrentKey().set((long) ri << 32 | r.getAlignmentStart() - 1);
        }
        if (headerMerger.hasProgramGroupCollisions()) {
            final String pg = (String) r.getAttribute(ReservedTagConstants.PROGRAM_GROUP_ID);
            if (pg != null) r.setAttribute(ReservedTagConstants.PROGRAM_GROUP_ID, headerMerger.getProgramGroupId(h, pg));
        }
        if (headerMerger.hasReadGroupCollisions()) {
            final String rg = (String) r.getAttribute(ReservedTagConstants.READ_GROUP_ID);
            if (rg != null) r.setAttribute(ReservedTagConstants.READ_GROUP_ID, headerMerger.getProgramGroupId(h, rg));
        }
        getCurrentValue().set(r);
        return true;
    }
}

final class SortOutputFormat extends KeyIgnoringBAMOutputFormat<NullWritable> {

    public static final String OUTPUT_NAME_PROP = "hadoopbam.sort.output.name";

    @Override
    public RecordWriter<NullWritable, SAMRecordWritable> getRecordWriter(TaskAttemptContext context) throws IOException {
        if (super.header == null) super.header = Sort.getHeaderMerger(context.getConfiguration()).getMergedHeader();
        return super.getRecordWriter(context);
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

package mappers;

import java.io.IOException;
import java.util.Vector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import utils.ConvertFastaFile;
import common.FastaRecordWritable;
import common.GenomicRegionWritable;
import common.MultiMapResult;
import common.ReadInfoWritable;

public class CloudSMAP {

    public enum MAP_MODE {

        MAP, BISULFITE, PAIR_END
    }

    ;

    public enum INPUT_MODE {

        FASTA_FILE, FASTQ_FILE, FASTA_AND_PRB
    }

    ;

    public enum RUN_MODE {

        RUN_MODE_MISMATCH, RUN_MODE_WILDCARD, RUN_MODE_WEIGHT_MATRIX
    }

    ;

    static int READS_PER_ROUND = 10;

    public static INPUT_MODE inputmode = INPUT_MODE.FASTA_FILE;

    public static RUN_MODE runmode = RUN_MODE.RUN_MODE_MISMATCH;

    static class CloudSMAPMapper extends Mapper<IntWritable, FastaRecordWritable, Text, ReadInfoWritable> {

        private int seed_num = 3, seed_weight = 8, max_mismatches = 0;

        private String filename = new String();

        private int read_width = 0;

        Configuration conf = null;

        protected void setup(Context con) {
            conf = con.getConfiguration();
            max_mismatches = conf.getInt("max_mismatches", 0);
            seed_num = conf.getInt("seed_num", 3);
            seed_weight = conf.getInt("seed_weight", 8);
            filename = conf.get("read_file");
            read_width = con.getConfiguration().getInt("read_width", 0);
            Smap.VERBOSE = false;
        }

        String ambiguous_file = null;

        String fasta_suffix = "fa";

        public void map(IntWritable key, FastaRecordWritable value, Context context) throws IOException, InterruptedException {
            byte[] seq = new byte[value.m_sequence.getLength()];
            System.arraycopy(value.m_sequence.getBytes(), 0, seq, 0, value.m_sequence.getLength());
            int chrom_id = value.m_offset.get();
            try {
                Path thePath = new Path(filename);
                FileSystem fs = FileSystem.get(conf);
                SequenceFile.Reader theReader = null;
                System.out.println("key = " + key.toString());
                if (fs.exists(thePath)) {
                    theReader = new SequenceFile.Reader(fs, thePath, conf);
                    assert (theReader != null);
                    if (key.get() == 1) {
                        Text pseudokey = (Text) (theReader.getKeyClass().newInstance());
                        theReader.sync(theReader.getPosition() + 10);
                        if (!theReader.next(pseudokey)) {
                            IOUtils.closeStream(theReader);
                            return;
                        }
                    }
                    Smap smap = new Smap();
                    smap.initialize(read_width, max_mismatches, seed_num, seed_weight);
                    boolean stop = false;
                    int numread = 0;
                    do {
                        numread = smap.load_reads(max_mismatches, READS_PER_ROUND, theReader);
                        if (numread < READS_PER_ROUND) stop = true;
                        if (numread > 0) {
                            smap.execute(seq, chrom_id);
                            Vector<MultiMapResult> bests = smap.best_maps;
                            int score = max_mismatches;
                            for (int i = 0; i < bests.size(); ++i) if (!bests.elementAt(i).isEmpty()) {
                                bests.elementAt(i).sort();
                                for (int j = 0; j < bests.elementAt(i).mr.size(); ++j) if (j == 0 || bests.elementAt(i).mr.elementAt(j - 1).isSmaller(bests.elementAt(i).mr.elementAt(j))) {
                                    final int start = bests.elementAt(i).mr.elementAt(j).strand ? bests.elementAt(i).mr.elementAt(j).site : seq.length - bests.elementAt(i).mr.elementAt(j).site - Smap.read_width;
                                    score = bests.elementAt(i).score;
                                    context.write(new Text(smap.read_names.elementAt(smap.read_index.elementAt(i))), new ReadInfoWritable(smap.read_names.elementAt(smap.read_index.elementAt(i)), score, chrom_id, start, bests.elementAt(i).mr.elementAt(j).strand, !bests.elementAt(i).ambiguous()));
                                }
                            }
                        }
                    } while (stop == false);
                    IOUtils.closeStream(theReader);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class CloudSMAPReducer extends Reducer<Text, ReadInfoWritable, Text, GenomicRegionWritable> {

        GenomicRegionWritable fullalignment = new GenomicRegionWritable();

        int read_width = 0;

        String chrom_name = "";

        protected void setup(Context con) {
            read_width = con.getConfiguration().getInt("read_width", 0);
            chrom_name = con.getConfiguration().get("chrom_name");
        }

        public void reduce(Text key, Iterable<ReadInfoWritable> values, Context context) throws IOException, InterruptedException {
            int real_offset = 0;
            int min_score = read_width;
            boolean strand = true;
            String name = "";
            for (ReadInfoWritable value : values) {
                if (value.score.get() < min_score) {
                    real_offset = value.chrom.get() + value.site.get();
                    strand = value.strand.get();
                    min_score = value.score.get();
                    name = value.read_name.toString();
                }
            }
            fullalignment.set(chrom_name, real_offset, real_offset + read_width, name, min_score, strand);
            context.write(key, fullalignment);
        }
    }

    public static void main(String[] args) throws Exception {
        String refpath = null;
        String qrypath = null;
        String outpath = null;
        int seed_weight = 0;
        int seed_num = 0;
        int read_width = 25;
        int max_mismatches = 2;
        if (args.length < 7) {
            System.err.println("Usage: CloudSMAP refpath qrypath outpath readwidth maxmismatches seed_num seed_weight");
            return;
        } else {
            refpath = args[0];
            qrypath = args[1];
            outpath = args[2];
            read_width = Integer.parseInt(args[3]);
            max_mismatches = Integer.parseInt(args[4]);
            seed_num = Integer.parseInt(args[5]);
            seed_weight = Integer.parseInt(args[6]);
        }
        if (read_width > ConvertFastaFile.CHUNK_OVERLAP) {
            System.err.println("Increase CHUNK_OVERLAP for " + read_width + " length reads, and reconvert fasta file");
            return;
        }
        System.out.println("seed num = " + seed_num);
        System.out.println("seed weight = " + seed_weight);
        Configuration conf = new Configuration();
        conf.setInt("read_width", read_width);
        conf.setInt("max_mismatches", max_mismatches);
        conf.setInt("seed_num", seed_num);
        conf.setInt("seed_weight", seed_weight);
        conf.set("chrom_name", "chr8");
        conf.set("read_file", qrypath);
        conf.setLong("mapred.max.split.size", 140000);
        Job job = new Job(conf, "CloudSMAP");
        job.setJarByClass(CloudSMAP.class);
        SequenceFileInputFormat.addInputPath(job, new Path(refpath));
        SequenceFileOutputFormat.setOutputPath(job, new Path(outpath));
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ReadInfoWritable.class);
        job.setMapperClass(CloudSMAPMapper.class);
        job.setReducerClass(CloudSMAPReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(GenomicRegionWritable.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

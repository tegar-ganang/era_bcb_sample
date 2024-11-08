package mappers;

import java.io.IOException;
import java.net.URI;
import java.util.Vector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import common.FastaRecord;
import common.SAMRecordWritable;

class CloudSAMMapper extends Mapper<IntWritable, BytesWritable, Text, SAMRecordWritable> {

    private int seed_num = 3, seed_weight = 8, max_mismatches = 0;

    private int read_width = 0;

    private int readperround = 1000;

    private CloudAligner.MAP_MODE mapperMapmode;

    Configuration conf = null;

    SequenceFile.Reader refgenome;

    Aligner smap;

    Vector<byte[]> reads = new Vector<byte[]>();

    /**********************************
	 * Input for mapper: key: readid, value: read sequence
	 * output of mapper: key: readid % READPERPARTITION value: readid,readsequence
	 * (hash the records in an input split for a map)
	 * can have other hash like the first READPERPARTITION has the same key for the combiner
	 * using combiner
	 * 
	 */
    protected void setup(Context con) {
        conf = con.getConfiguration();
        max_mismatches = conf.getInt("max_mismatches", 0);
        seed_num = conf.getInt("seed_num", 3);
        seed_weight = conf.getInt("seed_weight", 8);
        read_width = con.getConfiguration().getInt("read_width", 0);
        int mapmode = conf.getInt("mapmode", 0);
        if (mapmode == 0) mapperMapmode = CloudAligner.MAP_MODE.MAP; else if (mapmode == 1) mapperMapmode = CloudAligner.MAP_MODE.BISULFITE; else mapperMapmode = CloudAligner.MAP_MODE.PAIR_END;
        System.out.println("CloudMapper Mapmode = " + mapperMapmode);
        readperround = conf.getInt("readperround", 1000);
        try {
            FileSystem fs;
            fs = FileSystem.get(URI.create(conf.get("ref_file")), conf);
            Path my_path = new Path(conf.get("ref_file"));
            if (fs.exists(my_path)) {
                refgenome = new SequenceFile.Reader(fs, my_path, conf);
                assert (refgenome != null);
            }
            smap = new Aligner(CloudAligner.INPUT_FORMAT.FASTA_FILE, CloudAligner.RUN_MODE.RUN_MODE_MISMATCH, mapperMapmode, false);
            smap.initialize(read_width, max_mismatches, seed_num, seed_weight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void map(IntWritable key, BytesWritable value, Context context) throws IOException, InterruptedException {
        FastaRecord record = new FastaRecord();
        record.fromBytes(value);
        byte[] seq = record.m_sequence;
        reads.add(seq);
        smap.read_names.add(key.toString());
    }

    protected void cleanup(Context context) throws IOException, InterruptedException {
        try {
            int numread = smap.load_reads(max_mismatches, readperround, reads);
            if (numread > 0) {
                for (int j = 0; j < smap.the_seeds.size(); ++j) {
                    context.progress();
                    smap.iterate_over_seeds(refgenome, j, smap.fast_reads, smap.max_mismatches);
                }
                smap.eliminate_ambigs(smap.max_mismatches, smap.fast_reads);
                Vector<SAMRecordWritable> results = new Vector<SAMRecordWritable>();
                smap.generateSAMResults(CloudAligner.INPUT_FORMAT.FASTA_FILE, results);
                for (int i = 0; i < results.size(); i++) context.write(new Text(smap.read_names.elementAt(smap.read_index.elementAt(i))), results.elementAt(i));
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

package mappers;

import java.io.IOException;
import java.net.URI;
import java.util.Vector;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import common.ReadRecordWritable;
import common.SAMRecordWritable;

public class CloudSAMMapperQ extends Mapper<Text, ReadRecordWritable, Text, SAMRecordWritable> {

    private int seed_num = 3, seed_weight = 8, max_mismatches = 0;

    private int read_width = 0;

    private int readperround = 1000;

    private CloudAligner.MAP_MODE mapperMapmode;

    Configuration conf = null;

    SequenceFile.Reader refgenome;

    Aligner smap;

    Vector<byte[]> reads = new Vector<byte[]>();

    Vector<byte[]> quals = new Vector<byte[]>();

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
            smap = new Aligner(CloudAligner.INPUT_FORMAT.FASTQ_FILE, CloudAligner.RUN_MODE.RUN_MODE_MISMATCH, mapperMapmode, true);
            smap.initialize(read_width, max_mismatches, seed_num, seed_weight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void map(Text key, ReadRecordWritable value, Context context) throws IOException, InterruptedException {
        byte[] seq = new byte[value.m_sequence.getLength()];
        byte[] qual = new byte[value.m_qsequence.getLength()];
        System.arraycopy(value.m_sequence.getBytes(), 0, seq, 0, value.m_sequence.getLength());
        System.arraycopy(value.m_qsequence.getBytes(), 0, qual, 0, value.m_qsequence.getLength());
        reads.add(seq);
        quals.add(qual);
        smap.read_names.add(key.toString());
    }

    protected void cleanup(Context context) throws IOException, InterruptedException {
        try {
            int numread = smap.load_qual_reads(max_mismatches, readperround, reads, quals);
            if (numread > 0) {
                for (int j = 0; j < smap.the_seeds.size(); ++j) {
                    context.progress();
                    smap.iterate_over_seeds(refgenome, j, smap.fast_reads_q, smap.max_match_score);
                }
                smap.eliminate_ambigs(smap.max_match_score, smap.fast_reads_q);
                Vector<SAMRecordWritable> results = new Vector<SAMRecordWritable>();
                smap.generateSAMResults(CloudAligner.INPUT_FORMAT.FASTQ_FILE, results);
                for (int i = 0; i < results.size(); i++) context.write(new Text(smap.read_names.elementAt(smap.read_index.elementAt(i))), results.elementAt(i));
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

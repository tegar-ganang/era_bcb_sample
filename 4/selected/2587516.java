package common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;

public class ReadInfoWritable implements Writable {

    public BytesWritable read = new BytesWritable();

    public BytesWritable qualities = new BytesWritable();

    public IntWritable chrom = new IntWritable(15);

    public IntWritable site = new IntWritable(32);

    public DoubleWritable score = new DoubleWritable(15);

    public BooleanWritable strand = new BooleanWritable(true);

    public BooleanWritable unique = new BooleanWritable(true);

    public ReadInfoWritable() {
    }

    public ReadInfoWritable(byte[] name, byte[] qual, double scr, int chr, int ste, boolean str, boolean unq) {
        read.set(new BytesWritable(name));
        if (qual != null) qualities.set(new BytesWritable(qual)); else qualities = null;
        chrom.set(chr);
        site.set(ste);
        score.set(scr);
        strand.set(str);
        unique.set(unq);
    }

    public ReadInfoWritable(double scr) {
        score.set(scr);
    }

    public void set(double scr, int chr, int ste, boolean str) {
        unique.set(scr < score.get() || (site.get() == ste && unique.get() && chrom.get() == chr));
        chrom.set(chr);
        site.set(ste);
        score.set(scr);
        strand.set(str);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        read.readFields(in);
        qualities.readFields(in);
        chrom.readFields(in);
        site.readFields(in);
        score.readFields(in);
        strand.readFields(in);
        unique.readFields(in);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        read.write(out);
        qualities.write(out);
        chrom.write(out);
        site.write(out);
        score.write(out);
        strand.write(out);
        unique.write(out);
    }
}

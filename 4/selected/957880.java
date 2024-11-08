package chaski.proc.preproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import jpfm.configurable.ConfigClass;
import jpfm.configurable.ConfigField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import chaski.utils.CommonFileOperations;
import static chaski.utils.CommonFileOperations.*;
import static chaski.utils.ProgramLogic.*;

/**
 * Preprocess the corpus by merge alignments and corpus into specific format
 * @author qing
 *
 */
@ConfigClass(value = "merge-alignment", help = "Merge moses alignment format and corpus into a single file")
public class Preprocess {

    private static final Log LOG = LogFactory.getLog(Preprocess.class);

    @ConfigField(name = "src", alias = { "merge.source", "source" }, required = true, help = "The path of source courpus file")
    public String source;

    @ConfigField(name = "tgt", alias = { "merge.target", "target" }, required = true, help = "The path of target courpus file")
    public String target;

    @ConfigField(name = "aln", alias = { "merge.align", "align" }, required = true, help = "The path of Moses-format alignment file")
    public String align;

    @ConfigField(name = "corpus", alias = { "merge.output", "output", "corpus" }, required = true, help = "The path of output corpus file")
    public String output;

    @ConfigField(name = "splits", alias = { "merge.split", "spl", "sp" }, required = false, defaultValue = "10000", help = "The number of sentences in a split")
    public int split;

    @ConfigField(name = "src-on-hdfs", alias = { "merge.src-on-hdfs", "shdfs" }, defaultValue = "false", help = "Whether the source corpus file is on HDFS. By default it is false, and the file is supposed to be on local FS")
    public boolean sourceOnHDFS;

    @ConfigField(name = "tgt-on-hdfs", alias = { "merge.tgt-on-hdfs", "thdfs" }, defaultValue = "false", help = "Whether the target corpus file is on HDFS. By default it is false, and the file is supposed to be on local FS")
    public boolean targetOnHDFS;

    @ConfigField(name = "aln-on-hdfs", alias = { "merge.aln-on-hdfs", "ahdfs" }, defaultValue = "false", help = "Whether the alignment file is on HDFS. By default it is false, and the file is supposed to be on local FS")
    public boolean alignOnHDFS;

    @ConfigField(name = "out-on-hdfs", alias = { "merge.out-on-hdfs", "ohdfs" }, defaultValue = "true", help = "Whether the output file will be on HDFS. By default it is true, and the output is supposed to be on HDFS")
    public boolean outputOnHDFS;

    @ConfigField(name = "overwrite", alias = { "merge.overwrite", "ow" }, defaultValue = "false", help = "Whether the output file should be overwritten, default is false and the program will die if it exists")
    public boolean overwrite;

    protected static String filterSpaces(String s) {
        String[] ss = s.split("\\s+");
        StringBuffer bf = new StringBuffer();
        for (int i = 0; i < ss.length; i++) {
            if (ss[i].length() > 0) {
                bf.append(ss[i].trim());
            }
            if (i < ss.length - 1) {
                bf.append(" ");
            }
        }
        return bf.toString();
    }

    protected static boolean mergeAlignment(InputStream srcSent, InputStream tarSent, InputStream align, OutputStream out) throws IOException {
        BufferedReader rdSrc = new BufferedReader(new InputStreamReader(srcSent));
        BufferedReader rdTar = new BufferedReader(new InputStreamReader(tarSent));
        BufferedReader rdAlign = new BufferedReader(new InputStreamReader(align));
        PrintWriter wr = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out), 65 * 1024 * 1024));
        int i = 0;
        boolean finished = false;
        while (true) {
            i += 1;
            String s = rdSrc.readLine();
            String t = rdTar.readLine();
            String a = rdAlign.readLine();
            if (s == null || t == null || a == null) {
                finished = true;
                break;
            }
            if (s.indexOf('|') >= 0 || t.indexOf('|') >= 0) {
                continue;
            }
            wr.print(String.format("%s {##} %s {##} %s\n", filterSpaces(s), filterSpaces(t), a));
        }
        wr.close();
        return finished;
    }

    /**
	 * The actual entry point
	 */
    public void execute() throws Exception {
        if (sourceOnHDFS) testFileExistOnHDFSOrDie(source, "File not found " + source + " on HDFS ", FileNotFoundException.class, LOG); else testFileExistOrDie(source, "File not found " + source + " on localFs ", FileNotFoundException.class, LOG);
        if (targetOnHDFS) testFileExistOnHDFSOrDie(target, "File not found " + target + " on HDFS ", FileNotFoundException.class, LOG); else testFileExistOrDie(target, "File not found " + target + " on localFs ", FileNotFoundException.class, LOG);
        if (alignOnHDFS) testFileExistOnHDFSOrDie(align, "File not found " + align + " on HDFS ", FileNotFoundException.class, LOG); else testFileExistOrDie(align, "File not found " + align + " on localFs ", FileNotFoundException.class, LOG);
        if (outputOnHDFS) {
            if (!overwrite) testFileNotExistOnHDFSOrDie(output, "File already exists " + output + " on HDFS ", IOException.class, LOG); else if (!testFileNotExistOnHDFS(output, "File already exists " + output + " on HDFS, will be overwritten ", LOG)) {
                deleteIfExists(output);
            }
        } else {
            if (!overwrite) testFileNotExistOrDie(output, "File already exists " + output + " on local FS ", IOException.class, LOG);
        }
        InputStream src = openFileForRead(source, sourceOnHDFS);
        InputStream tgt = openFileForRead(target, targetOnHDFS);
        InputStream aln = openFileForRead(align, alignOnHDFS);
        OutputStream oup = CommonFileOperations.openFileForWrite(output);
        mergeAlignment(src, tgt, aln, oup);
    }
}

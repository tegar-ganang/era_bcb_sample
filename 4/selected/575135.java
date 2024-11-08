package net.sf.sam.cleaner;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.sf.jcmdlineparser.ParserException;
import net.sf.jcmdlineparser.options.FileMultiOption;
import net.sf.jcmdlineparser.options.OptionBuilder;
import net.sf.jcmdlineparser.parser.CmdLineParser;
import net.sf.jcmdlineparser.parser.CmdLineParserImpl;
import net.sf.kerner.utils.Utils;
import net.sf.kerner.utils.counter.Counter;
import net.sf.sam.cleaner.alien.FileUtils;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model {

    private static final CmdLineParser parser = new CmdLineParserImpl();

    private static FileMultiOption samFilesOption;

    private static final Logger log = LoggerFactory.getLogger(Model.class);

    private static final ExecutorService exe = Executors.newFixedThreadPool(Utils.NUM_CPUS);

    public static void main(String[] args) {
        try {
            samFilesOption = new OptionBuilder().setDescription("input files").setLongIdentifier("input").newFileMultiOption();
            parser.registerOption(samFilesOption);
            args = parser.parse(args);
            log.info("Unknown options: " + Arrays.asList(args));
            log.warn("Remember: Input reads must be sorted by alignment start!");
            cleanFiles(samFilesOption.getValues());
        } catch (ParserException e) {
            log.warn(e.getLocalizedMessage());
            parser.printHelp("Usage");
            System.exit(1);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            System.exit(2);
        }
    }

    static void cleanFile(File file) {
        final Counter cnt = new Counter();
        final File out = new File(FileUtils.appendToFileName(file.getAbsolutePath(), ".cleaned"));
        final SAMFileReader reader = new SAMFileReader(file);
        final SAMRecordIterator it = reader.iterator();
        final SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader.getFileHeader(), true, out);
        if (!it.hasNext()) return;
        log.info("Cleaning file " + file + " to " + out.getName());
        SAMRecord last = it.next();
        writer.addAlignment(last);
        while (it.hasNext()) {
            final SAMRecord now = it.next();
            final int start1 = last.getAlignmentStart();
            final int start2 = now.getAlignmentStart();
            final int end1 = last.getAlignmentEnd();
            final int end2 = now.getAlignmentEnd();
            if (start1 == start2 && end1 == end2) {
                log.debug("Discarding record " + now.toString());
                cnt.count();
                continue;
            }
            writer.addAlignment(now);
            last = now;
        }
        writer.close();
        reader.close();
        log.info(file + " done, discarded " + cnt.getCount() + " reads");
        exe.shutdown();
    }

    static void cleanFiles(Collection<File> files) {
        for (File file : files) {
            exe.submit(new FileWorker(file));
        }
    }
}

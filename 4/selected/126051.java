package org.jcvi.fasta.fastq.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jcvi.common.command.CommandLineOptionBuilder;
import org.jcvi.common.command.CommandLineUtils;
import org.jcvi.common.core.seq.fastx.ExcludeFastXIdFilter;
import org.jcvi.common.core.seq.fastx.FastXFilter;
import org.jcvi.common.core.seq.fastx.IncludeFastXIdFilter;
import org.jcvi.common.core.seq.fastx.AcceptingFastXFilter;
import org.jcvi.common.core.seq.fastx.fasta.AbstractFastaVisitor;
import org.jcvi.common.core.seq.fastx.fasta.FastaFileParser;
import org.jcvi.common.core.seq.fastx.fasta.FastaRecord;
import org.jcvi.common.core.seq.fastx.fasta.FastaFileVisitor;
import org.jcvi.common.core.seq.fastx.fasta.nt.DefaultNucleotideSequenceFastaRecord;
import org.jcvi.common.core.seq.fastx.fasta.nt.NucleotideSequenceFastaRecord;
import org.jcvi.common.core.seq.fastx.fasta.qual.QualitySequenceFastaRecord;
import org.jcvi.common.core.seq.fastx.fasta.qual.QualityFastaRecordUtil;
import org.jcvi.common.core.seq.fastx.fastq.DefaultFastqRecord;
import org.jcvi.common.core.seq.fastx.fastq.FastqQualityCodec;
import org.jcvi.common.core.seq.fastx.fastq.FastqRecord;
import org.jcvi.common.core.symbol.Symbol;
import org.jcvi.common.core.symbol.Sequence;
import org.jcvi.common.core.symbol.qual.PhredQuality;
import org.jcvi.common.core.symbol.qual.QualitySequence;
import org.jcvi.common.core.symbol.residue.nt.Nucleotide;
import org.jcvi.common.core.symbol.residue.nt.NucleotideSequence;
import org.jcvi.common.io.idReader.DefaultFileIdReader;
import org.jcvi.common.io.idReader.IdReader;
import org.jcvi.common.io.idReader.IdReaderException;
import org.jcvi.common.io.idReader.StringIdParser;
import org.joda.time.Period;

/**
 * {@code SortedFasta2Fastq} is a another fasta to fastq conversion tool
 * which makes the assumption that the records in the given seq and qual fasta files
 * are in the same order, this allows the converter to skip the time consuming
 * index step.
 * 
 * @author dkatzel
 *
 *
 */
public class SortedFasta2Fastq {

    /**
     * This is our end of file token which tell us we are
     * done parsing by the time we get to this object in our quality queue.
     */
    private static final QualitySequenceFastaRecord QUALITY_END_OF_FILE = QualityFastaRecordUtil.buildFastaRecord("NULL", "", "");

    /**
     * This is our end of file token which tell us we are
     * done parsing by the time we get to this object in our seq queue.
     */
    private static final DefaultNucleotideSequenceFastaRecord SEQ_END_OF_FILE = new DefaultNucleotideSequenceFastaRecord("NULL", null, "A");

    private static final int DEFAULT_QUEUE_SIZE = 1000;

    private abstract static class BlockedFastaVisitor<T extends Symbol, E extends Sequence<T>, F extends FastaRecord<T, E>> extends Thread {

        final BlockingQueue<F> queue;

        final File file;

        private final FastXFilter filter;

        /**
         * @param file
         * @param queue
         */
        public BlockedFastaVisitor(File file, BlockingQueue<F> queue, FastXFilter filter) {
            this.file = file;
            this.queue = queue;
            this.filter = filter;
        }

        protected BlockingQueue<F> getQueue() {
            return queue;
        }

        protected FastXFilter getFilter() {
            return filter;
        }
    }

    private static class QualityBlockedFastaVisitor extends BlockedFastaVisitor<PhredQuality, QualitySequence, QualitySequenceFastaRecord> {

        /**
         * @param file
         * @param queue
         */
        public QualityBlockedFastaVisitor(File file, BlockingQueue<QualitySequenceFastaRecord> queue, FastXFilter filter) {
            super(file, queue, filter);
        }

        @Override
        public void run() {
            FastaFileVisitor qualVisitor = new AbstractFastaVisitor() {

                @Override
                public boolean visitRecord(String id, String comment, String entireBody) {
                    if (getFilter().accept(id)) {
                        try {
                            getQueue().put(QualityFastaRecordUtil.buildFastaRecord(id, comment, entireBody));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }

                @Override
                public void visitEndOfFile() {
                    try {
                        getQueue().put(QUALITY_END_OF_FILE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            try {
                FastaFileParser.parse(file, qualVisitor);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SequenceBlockedFastaVisitor extends BlockedFastaVisitor<Nucleotide, NucleotideSequence, NucleotideSequenceFastaRecord> {

        /**
         * @param file
         * @param queue
         */
        public SequenceBlockedFastaVisitor(File file, BlockingQueue<NucleotideSequenceFastaRecord> queue, FastXFilter filter) {
            super(file, queue, filter);
        }

        @Override
        public void run() {
            FastaFileVisitor seqVisitor = new AbstractFastaVisitor() {

                @Override
                public boolean visitRecord(String id, String comment, String entireBody) {
                    if (getFilter().accept(id)) {
                        try {
                            getQueue().put(new DefaultNucleotideSequenceFastaRecord(id, comment, entireBody));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }

                @Override
                public void visitEndOfFile() {
                    try {
                        getQueue().put(SEQ_END_OF_FILE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            try {
                FastaFileParser.parse(file, seqVisitor);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param args
     * @throws IdReaderException 
     * @throws FileNotFoundException 
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws IdReaderException, FileNotFoundException, InterruptedException {
        Options options = new Options();
        options.addOption(new CommandLineOptionBuilder("s", "input sequence FASTA file").longName("sequence").build());
        options.addOption(new CommandLineOptionBuilder("q", "input quality FASTA file").longName("quality").build());
        options.addOption(new CommandLineOptionBuilder("sanger", "should encode output fastq file in SANGER fastq file format (default is ILLUMINA 1.3+)").isFlag(true).build());
        options.addOption(new CommandLineOptionBuilder("b", "buffer size, the number of records to buffer while we parse," + "this higher this number is, the faster we can convert, but the more memory it takes" + " (default is " + DEFAULT_QUEUE_SIZE + ")").isFlag(true).build());
        options.addOption(new CommandLineOptionBuilder("o", "output fastq file").isRequired(true).build());
        options.addOption(CommandLineUtils.createHelpOption());
        OptionGroup group = new OptionGroup();
        group.addOption(new CommandLineOptionBuilder("i", "include file of ids to include").build());
        group.addOption(new CommandLineOptionBuilder("e", "exclude file of ids to exclude").build());
        options.addOptionGroup(group);
        try {
            CommandLine commandLine = CommandLineUtils.parseCommandLine(options, args);
            if (commandLine.hasOption("h")) {
                printHelp(options);
                System.exit(0);
            }
            boolean useSanger = commandLine.hasOption("sanger");
            final File qualFile = new File(commandLine.getOptionValue("q"));
            final File seqFile = new File(commandLine.getOptionValue("s"));
            final File idFile;
            final FastXFilter filter;
            if (commandLine.hasOption("i")) {
                idFile = new File(commandLine.getOptionValue("i"));
                Set<String> includeList = parseIdsFrom(idFile);
                if (commandLine.hasOption("e")) {
                    Set<String> excludeList = parseIdsFrom(new File(commandLine.getOptionValue("e")));
                    includeList.removeAll(excludeList);
                }
                filter = new IncludeFastXIdFilter(includeList);
            } else if (commandLine.hasOption("e")) {
                idFile = new File(commandLine.getOptionValue("e"));
                filter = new ExcludeFastXIdFilter(parseIdsFrom(idFile));
            } else {
                filter = AcceptingFastXFilter.INSTANCE;
            }
            final int bufferSize;
            if (commandLine.hasOption("b")) {
                bufferSize = Integer.parseInt(commandLine.getOptionValue("b"));
            } else {
                bufferSize = DEFAULT_QUEUE_SIZE;
            }
            final FastqQualityCodec fastqQualityCodec = useSanger ? FastqQualityCodec.SANGER : FastqQualityCodec.ILLUMINA;
            final BlockingQueue<QualitySequenceFastaRecord> qualityQueue = new ArrayBlockingQueue<QualitySequenceFastaRecord>(bufferSize);
            final BlockingQueue<NucleotideSequenceFastaRecord> sequenceQueue = new ArrayBlockingQueue<NucleotideSequenceFastaRecord>(bufferSize);
            final PrintWriter writer = new PrintWriter(commandLine.getOptionValue("o"));
            boolean done = false;
            QualityBlockedFastaVisitor qualVisitor = new QualityBlockedFastaVisitor(qualFile, qualityQueue, filter);
            SequenceBlockedFastaVisitor seqVisitor = new SequenceBlockedFastaVisitor(seqFile, sequenceQueue, filter);
            long startTime = System.currentTimeMillis();
            qualVisitor.start();
            seqVisitor.start();
            while (!done) {
                QualitySequenceFastaRecord qualityFasta = qualityQueue.take();
                NucleotideSequenceFastaRecord seqFasta = sequenceQueue.take();
                if (qualityFasta == QUALITY_END_OF_FILE) {
                    if (seqFasta == SEQ_END_OF_FILE) {
                        done = true;
                    } else {
                        throw new IllegalStateException("more seq records than qualities");
                    }
                } else {
                    if (seqFasta == SEQ_END_OF_FILE) {
                        throw new IllegalStateException("more quality records than sequences");
                    }
                    if (!seqFasta.getId().equals(qualityFasta.getId())) {
                        throw new IllegalStateException(String.format("seq and qual records are not in the same order: seq= %s qual = %s", seqFasta.getId(), qualityFasta.getId()));
                    }
                    FastqRecord fastq = new DefaultFastqRecord(seqFasta.getId(), seqFasta.getSequence(), qualityFasta.getSequence(), null);
                    writer.print(fastq.toFormattedString(fastqQualityCodec));
                }
            }
            writer.close();
            long endTime = System.currentTimeMillis();
            System.out.println(new Period(endTime - startTime));
        } catch (ParseException e) {
            printHelp(options);
            System.exit(1);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("sortedFasta2Fastq [OPTIONS] -s <seq file> -q <qual file> -o <fastq file>", "Parse a  sorted seq and qual file (ids in same order) and write the results out a fastq file. " + "This version should be orders of magnitude faster than fasta2Fastq because no indexing is required " + "and the seq and qual files are parsed at the same time in different threads.", options, "Created by Danny Katzel");
    }

    private static Set<String> parseIdsFrom(final File idFile) throws IdReaderException {
        IdReader<String> idReader = new DefaultFileIdReader<String>(idFile, new StringIdParser());
        Set<String> ids = new HashSet<String>();
        Iterator<String> iter = idReader.getIds();
        while (iter.hasNext()) {
            ids.add(iter.next());
        }
        return ids;
    }
}

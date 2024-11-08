package org.archive.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.archive.io.arc.ARCConstants;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCConstants;
import org.archive.io.warc.ExperimentalWARCWriter;
import org.archive.util.FileUtils;
import org.archive.util.anvl.ANVLRecord;

/**
 * Convert ARCs to (sortof) WARCs.
 * @author stack
 * @version $Date: 2007-03-09 23:57:28 +0000 (Fri, 09 Mar 2007) $ $Revision: 4977 $
 */
public class Arc2Warc {

    private static void usage(HelpFormatter formatter, Options options, int exitCode) {
        formatter.printHelp("java org.archive.io.arc.Arc2Warc " + "[--force] ARC_INPUT WARC_OUTPUT", options);
        System.exit(exitCode);
    }

    private static String getRevision() {
        return Warc2Arc.parseRevision("$Revision: 4977 $");
    }

    public void transform(final File arc, final File warc, final boolean force) throws IOException {
        FileUtils.isReadable(arc);
        if (warc.exists() && !force) {
            throw new IOException("Target WARC already exists. " + "Will not overwrite.");
        }
        ARCReader reader = ARCReaderFactory.get(arc, false, 0);
        transform(reader, warc);
    }

    protected void transform(final ARCReader reader, final File warc) throws IOException {
        ExperimentalWARCWriter writer = null;
        reader.setDigest(false);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(warc));
            final Iterator<ArchiveRecord> i = reader.iterator();
            ARCRecord firstRecord = (ARCRecord) i.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream((int) firstRecord.getHeader().getLength());
            firstRecord.dump(baos);
            ANVLRecord ar = new ANVLRecord(1);
            ar.addLabelValue("Filedesc", baos.toString());
            List<String> metadata = new ArrayList<String>(1);
            metadata.add(ar.toString());
            writer = new ExperimentalWARCWriter(null, bos, warc, reader.isCompressed(), null, metadata);
            writer.writeWarcinfoRecord(warc.getName(), "Made from " + reader.getReaderIdentifier() + " by " + this.getClass().getName() + "/" + getRevision());
            for (; i.hasNext(); ) {
                write(writer, (ARCRecord) i.next());
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                Logger l = Logger.getLogger(writer.getClass().getName());
                Level oldLevel = l.getLevel();
                l.setLevel(Level.WARNING);
                try {
                    writer.close();
                } finally {
                    l.setLevel(oldLevel);
                }
            }
        }
    }

    protected void write(final ExperimentalWARCWriter writer, final ARCRecord r) throws IOException {
        ANVLRecord ar = new ANVLRecord();
        String ip = (String) r.getHeader().getHeaderValue((ARCConstants.IP_HEADER_FIELD_KEY));
        if (ip != null && ip.length() > 0) {
            ar.addLabelValue(WARCConstants.NAMED_FIELD_IP_LABEL, ip);
        }
        writer.writeResourceRecord(r.getHeader().getUrl(), r.getHeader().getDate(), (r.getHeader().getContentBegin() > 0) ? WARCConstants.HTTP_RESPONSE_MIMETYPE : r.getHeader().getMimetype(), ar, r, r.getHeader().getLength());
    }

    /**
    * Command-line interface to Arc2Warc.
    *
    * @param args Command-line arguments.
    * @throws ParseException Failed parse of the command line.
    * @throws IOException
    * @throws java.text.ParseException
    */
    public static void main(String[] args) throws ParseException, IOException, java.text.ParseException {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Prints this message and exits."));
        options.addOption(new Option("f", "force", false, "Force overwrite of target file."));
        PosixParser parser = new PosixParser();
        CommandLine cmdline = parser.parse(options, args, false);
        List cmdlineArgs = cmdline.getArgList();
        Option[] cmdlineOptions = cmdline.getOptions();
        HelpFormatter formatter = new HelpFormatter();
        if (cmdlineArgs.size() <= 0) {
            usage(formatter, options, 0);
        }
        boolean force = false;
        for (int i = 0; i < cmdlineOptions.length; i++) {
            switch(cmdlineOptions[i].getId()) {
                case 'h':
                    usage(formatter, options, 0);
                    break;
                case 'f':
                    force = true;
                    break;
                default:
                    throw new RuntimeException("Unexpected option: " + +cmdlineOptions[i].getId());
            }
        }
        if (cmdlineArgs.size() != 2) {
            usage(formatter, options, 0);
        }
        (new Arc2Warc()).transform(new File(cmdlineArgs.get(0).toString()), new File(cmdlineArgs.get(1).toString()), force);
    }
}

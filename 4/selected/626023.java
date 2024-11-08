package de.andreavicentini.magicphoto.batch.problems;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import de.andreavicentini.magiclogger.service.LoggerService;
import de.andreavicentini.magicphoto.batch.DirectoryStructureComparator;
import de.andreavicentini.magicphoto.batch.IDirectoryStructureProcessor;
import de.andreavicentini.magicphoto.common.FileFilters;

public class MagicDirectoryStructureChecks implements Runnable {

    private final File source;

    private final File dest;

    public static void main(String[] args) throws IOException {
        new MagicDirectoryStructureChecks(new File(args[0]), new File(args[1])).run();
    }

    public MagicDirectoryStructureChecks(File source, File dest) {
        this.source = source;
        this.dest = dest;
    }

    public void run() {
        try {
            new DirectoryStructureComparator().compare(this.source, this.dest, new FileFilters.ExcludeOriginals(), new IDirectoryStructureProcessor.Composite(new IDirectoryStructureProcessor[] { new ComparationChecks.CheckSameDate(LoggerService.instance), new ComparationChecks.CheckMissing(LoggerService.instance) }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class CopyPlaceholder implements IDirectoryStructureProcessor {

        private final File input;

        public CopyPlaceholder(File input) {
            this.input = input;
        }

        public void processDirectory(File source, File dest) throws IOException {
            dest.mkdir();
            copy(this.input, new File(dest, "placeholder.jpg"));
        }

        public void processFile(File source, File dest) throws IOException {
        }
    }

    private static void copy(File source, File dest) throws FileNotFoundException, IOException {
        FileInputStream input = new FileInputStream(source);
        FileOutputStream output = new FileOutputStream(dest);
        System.out.println("Copying " + source + " to " + dest);
        IOUtils.copy(input, output);
        output.close();
        input.close();
        dest.setLastModified(source.lastModified());
    }
}

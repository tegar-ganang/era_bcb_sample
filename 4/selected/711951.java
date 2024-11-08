package de.andreavicentini.magicphoto.batch;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

public class MagicDirectoryStructurePreparation {

    private File input;

    public static void main(String[] args) throws IOException {
        new DirectoryStructureComparator().compare(new File(args[0]), new File(args[1]), new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory() && pathname.getName().equals("Originals")) return false;
                return true;
            }
        }, new IDirectoryStructureProcessor.Composite(new IDirectoryStructureProcessor[] { new CopyPlaceholder(new File("C:\\tmp\\cache\\18_50mm_gross.jpg")), new CopyFile("descript.ion"), new CopyFile("Workflow.ini") }));
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

    static class CopyFile implements IDirectoryStructureProcessor {

        private final String name;

        public CopyFile(String name) {
            this.name = name;
        }

        public void processDirectory(File source, File dest) throws IOException {
        }

        public void processFile(File source, File dest) throws IOException {
            if (!source.getName().equals(this.name)) return;
            if (dest.exists() && source.lastModified() == dest.lastModified() && source.length() == dest.length()) {
                System.out.println("Ignoring " + source + " because it's already copied");
                return;
            }
            copy(source, dest);
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

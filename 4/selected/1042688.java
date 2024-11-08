package de.andreavicentini.magicphoto.batch;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import de.andreavicentini.magicphoto.common.FileFilters;

public class MagicCopy {

    interface LineProcessor {

        void process(File input);
    }

    class Month {

        void addDirectory(File file) {
        }
    }

    class Explorer {

        private final File destination;

        private final File subdirectory;

        public Explorer(File root, File destination) {
            this.destination = destination;
            subdirectory = new File(this.destination, root.getName());
            subdirectory.mkdirs();
        }

        void process(File file) {
            if (file.isDirectory()) explore(file, new Explorer(file, this.subdirectory)); else try {
                copy(file, this.subdirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void copy(File fSource, File destination) throws IOException {
            File fDest = new File(destination, fSource.getName());
            internalCopy(fSource, fDest);
            resetTime(fSource, fDest);
        }

        private void resetTime(File source, File dest) {
            File file = new File(source.getParentFile() + File.separator + "Originals", source.getName());
            if (!file.exists()) return;
            dest.setLastModified(file.lastModified());
        }

        private void internalCopy(File fSource, File file) throws FileNotFoundException, IOException {
            if (fSource.getName().equals("Thums.db")) return;
            System.out.println("copying " + fSource + " in " + file);
            OutputStream o = new BufferedOutputStream(new FileOutputStream(file));
            InputStream i = new BufferedInputStream(new FileInputStream(fSource));
            byte[] b = new byte[8192];
            int n;
            while ((n = i.read(b)) > 0) o.write(b, 0, n);
            i.close();
            o.close();
        }
    }

    public static void main(String args[]) throws Exception {
        new MagicCopy().start(new File(args[0]), new File(args[1]));
    }

    private void start(File root, File destination) {
        explore(root, new Explorer(root, destination));
    }

    private void explore(File root, Explorer explorer) {
        File[] files = root.listFiles(new FileFilters.ExcludeOriginals());
        for (int i = 0; i < files.length; i++) explorer.process(files[i]);
    }
}

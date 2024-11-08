package tgreiner.amy.chess.tablebases;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

/**
 * The main class for generating table bases from the command line.
 *
 * @author <a href = "mailto:thorsten.greiner@googlemail.com">Thorsten Greiner</a>
 */
public final class Main {

    /**
     * This class cannot be instantiated.
     */
    private Main() {
    }

    /**
     * The main method.
     *
     * @param args the command line arguments
     * @throws IOException if an I/O error occures
     */
    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java tgreiner.amy.chess.tablebases.Main <tb>");
            System.exit(1);
        }
        Classifier c = new Classifier();
        Handle h = c.classify(args[0]);
        h.normalize();
        String tb = h.toString();
        IndexerFactory factory = new IndexerFactory();
        Indexer indexer = factory.createIndexer(h);
        if (indexer == null) {
            System.err.println("Unknown table base " + tb);
            System.exit(1);
        }
        int count = indexer.getPositionCount();
        File fWhite = new File("temp.tbw");
        File fBlack = new File("temp.tbb");
        RandomAccessFile fileWhite = new RandomAccessFile(fWhite, "rw");
        RandomAccessFile fileBlack = new RandomAccessFile(fBlack, "rw");
        fileWhite.setLength(count);
        fileBlack.setLength(count);
        MappedByteBuffer bufferWhite = fileWhite.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, count);
        MappedByteBuffer bufferBlack = fileBlack.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, count);
        Loader l = new Loader();
        Generator gen = new Generator(h, bufferWhite, bufferBlack, l.load());
        gen.generate();
        bufferWhite.force();
        bufferBlack.force();
        fileWhite.close();
        fileBlack.close();
        String whiteFileName = "tb" + File.separator + tb + ".tbw";
        String blackFileName = "tb" + File.separator + tb + ".tbb";
        fWhite.renameTo(new File(whiteFileName));
        fBlack.renameTo(new File(blackFileName));
    }
}

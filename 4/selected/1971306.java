package tgreiner.amy.chess.tablebases;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import org.apache.log4j.Logger;

/**
 * Loads a table base from disk.
 *
 * @author <a href = "mailto:thorsten.greiner@googlemail.com">Thorsten Greiner</a>
 */
public class Loader {

    /** The log4j Logger. */
    private static Logger log = Logger.getLogger(Loader.class);

    /** The classifier. */
    private Classifier classifier = new Classifier();

    /** The IndexerFactory. */
    private IndexerFactory factory = new IndexerFactory();

    /**
     * Probe for the existence of table base data files in the file
     * system.
     *
     * @param name the table base name
     * @param m the corresponding prober
     * @throws IOException if an I/O error occurs
     */
    private void probe(final String name, final TableBaseProber m) throws IOException {
        Handle h = classifier.classify(name);
        h.normalize();
        String tb = h.toString();
        if (log.isDebugEnabled()) {
            log.debug("Probing for " + tb);
        }
        File whiteFile = new File("tb" + File.separator + tb + ".tbw");
        File blackFile = new File("tb" + File.separator + tb + ".tbb");
        if (whiteFile.exists() && whiteFile.isFile() && blackFile.exists() && blackFile.isFile()) {
            Indexer indexer = factory.createIndexer(h);
            int count = indexer.getPositionCount();
            if (whiteFile.length() != count) {
                log.error("White database for " + name + " corrupted");
                return;
            }
            if (blackFile.length() != count) {
                log.error("Black database for " + name + " corrupted");
                return;
            }
            RandomAccessFile fileWhite = new RandomAccessFile(whiteFile, "rw");
            RandomAccessFile fileBlack = new RandomAccessFile(blackFile, "rw");
            MappedByteBuffer bufferWhite = fileWhite.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, count);
            MappedByteBuffer bufferBlack = fileBlack.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, count);
            m.registerTableBase(h, bufferWhite, bufferBlack);
            log.debug("Loaded " + name + ".");
        }
    }

    /**
     * Load table bases from disk.
     *
     * @return a TableBaseProber configured to use the table bases found
     * @throws IOException if there is a problem accessing the table base files.
     */
    public TableBaseProber load() throws IOException {
        TableBaseProber m = new TableBaseProber();
        probe("krk", m);
        probe("kqk", m);
        probe("kpk", m);
        probe("knnk", m);
        probe("kbbk", m);
        probe("krrk", m);
        probe("kqqk", m);
        probe("kbnk", m);
        probe("krnk", m);
        probe("krbk", m);
        probe("kqnk", m);
        probe("kqbk", m);
        probe("kqrk", m);
        probe("kqkq", m);
        probe("kqkr", m);
        probe("kqkb", m);
        probe("kqkn", m);
        probe("krkr", m);
        probe("krkb", m);
        probe("krkn", m);
        probe("kbkb", m);
        probe("kbkn", m);
        probe("knkn", m);
        probe("kqkp", m);
        probe("krkp", m);
        probe("kbkp", m);
        probe("knkp", m);
        probe("kqpk", m);
        probe("krpk", m);
        probe("kbpk", m);
        probe("knpk", m);
        probe("kpkp", m);
        return m;
    }
}

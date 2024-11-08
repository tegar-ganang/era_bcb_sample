package net.conquiris.search;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.IOException;
import net.conquiris.api.search.Reader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 * Index writer near-real-time unmanaged reader supplier implementation.
 * @author Andres Rodriguez
 */
final class IndexWriterReaderSupplier extends AbstractReaderSupplier {

    /** Index writer. */
    private final IndexWriter writer;

    /**
	 * Constructor.
	 * @param writer Index writer.
	 */
    IndexWriterReaderSupplier(IndexWriter writer) {
        this.writer = checkNotNull(writer, "The index writer must be provided");
    }

    @Override
    Reader doGet() throws IOException {
        try {
            final IndexReader reader = IndexReader.open(writer, true);
            return Reader.of(reader, true);
        } catch (IndexNotFoundException e) {
            return ReaderSuppliers.empty().get();
        }
    }
}

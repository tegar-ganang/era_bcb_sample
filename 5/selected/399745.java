package archlib.tarlib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import archlib.AbstractArchiveInputStream;
import archlib.ArchiveEntry;
import archlib.util.CustomLogger;

/**
 *
 * @author Vaman Kulkarni
 */
public class TarFileInputStream extends AbstractArchiveInputStream {

    private Logger log = CustomLogger.getLogger(TarFileInputStream.class);

    private TarInputStream tarIn = null;

    public TarFileInputStream(InputStream in) throws FileNotFoundException {
        tarIn = new TarInputStream(in);
    }

    @Override
    public <X extends InputStream> X getNativeInputStream(Class<X> streamClass) {
        return streamClass.cast(tarIn);
    }

    @Override
    public ArchiveEntry getNextEntry() throws IOException {
        return tarIn.getNextEntry();
    }
}

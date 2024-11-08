package tripleo.framework.svr.storage;

import tripleo.framework.fs.FileOpFailure;
import tripleo.framework.fs.ReadHandle;
import tripleo.framework.io.*;

class PartialStreamR implements FileReader {

    public PartialStreamR(ReadHandle aF, long l) {
        _carrier = aF;
        _availAtStart = l;
        _read = 0L;
    }

    public long available() {
        return _availAtStart - _read;
    }

    public ReadableByteStream read(int i) throws FileOpFailure {
        WritableByteStream wbs = ByteStreamFactory.allocMemoryWritable(i);
        wbs.write(_carrier.read(i));
        return wbs.reader();
    }

    private final long _availAtStart;

    private final ReadHandle _carrier;

    private long _read;
}

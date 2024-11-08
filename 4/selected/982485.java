package org.javaseis.util.access;

import org.javaseis.io.Seisio;
import org.javaseis.util.access.JavaSeisDescriptor;

public class JavaSeisWriter extends JavaSeisIO {

    public JavaSeisWriter(String directory) {
        super(directory);
        _io_mode = Seisio.MODE_READ_WRITE;
        _read_write = true;
        if (!_opened_before) _descr = new JavaSeisDescriptor();
    }
}

package org.javaseis.util.access;

import org.javaseis.io.Seisio;

public class JavaSeisReader extends JavaSeisIO {

    public JavaSeisReader(String directory) {
        super(directory);
        _io_mode = Seisio.MODE_READ_ONLY;
        _read_write = false;
        if (!_opened_before) {
            _status = ERROR;
            _error_message = "JavaSeisReader: JavaSeis path not found: " + directory;
        }
    }
}

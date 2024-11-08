package com.netx.basic.R1.io;

import java.io.File;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import com.netx.basic.R1.eh.Checker;
import com.netx.basic.R1.shared.Globals;
import com.netx.generics.R1.util.Tools;

public class ExtendedOutputStream extends OutputStream {

    private final String _path;

    private final BufferedOutputStream _out;

    private final FileLock _lock;

    private final boolean _std;

    private boolean _isOpen;

    public ExtendedOutputStream(OutputStream out, String path) {
        Checker.checkNull(out, "out");
        _path = path;
        _out = new BufferedOutputStream(out, Streams.getDefaultBufferSize());
        _lock = null;
        if (out == System.out || out == System.err) {
            _std = true;
        } else {
            _std = false;
        }
        _isOpen = true;
    }

    ExtendedOutputStream(File file, boolean append, boolean lock) throws FileLockedException, ReadWriteException {
        Checker.checkNull(file, "file");
        _path = file.getAbsolutePath();
        _std = false;
        if (Locks.isLocked(_path)) {
            throw new FileLockedException(_path, null);
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file, append);
        } catch (FileNotFoundException fnfe) {
            _ensureStreamIsClosed(out);
            throw Translator.translateFNFE(fnfe, _path);
        }
        _out = new BufferedOutputStream(out, Streams.getDefaultBufferSize());
        _isOpen = true;
        if (lock) {
            try {
                _lock = out.getChannel().tryLock();
                if (_lock == null) {
                    _ensureStreamIsClosed(out);
                    throw new FileLockedException(_path, null);
                }
                Locks.lock(_path);
            } catch (OverlappingFileLockException ofle) {
                _ensureStreamIsClosed(out);
                throw new FileLockedException(_path, ofle);
            } catch (FileLockedException fle) {
                throw fle;
            } catch (IOException io) {
                _ensureStreamIsClosed(out);
                throw Translator.translateIOE(io, _path);
            }
        } else {
            _lock = null;
        }
    }

    public String getPath() {
        return _path;
    }

    public void close() throws ReadWriteException {
        if (_isOpen) {
            try {
                _isOpen = false;
                _out.flush();
                if (_lock != null) {
                    _lock.release();
                    Locks.release(_path);
                }
                if (!_std) {
                    _out.close();
                }
            } catch (IOException io) {
                throw Translator.translateIOE(io, _path);
            }
        }
    }

    public void flush() throws ReadWriteException {
        _checkState();
        try {
            _out.flush();
        } catch (IOException io) {
            throw Translator.translateIOE(io, _path);
        }
    }

    public void write(byte[] b) throws ReadWriteException {
        _checkState();
        Checker.checkNull(b, "b");
        try {
            _out.write(b);
        } catch (IOException io) {
            throw Translator.translateIOE(io, _path);
        }
    }

    public void write(byte[] b, int off, int len) throws ReadWriteException {
        _checkState();
        Checker.checkNull(b, "b");
        try {
            _out.write(b, off, len);
        } catch (IOException io) {
            throw Translator.translateIOE(io, _path);
        }
    }

    public void write(int b) throws ReadWriteException {
        _checkState();
        try {
            _out.write(b);
        } catch (IOException io) {
            throw Translator.translateIOE(io, _path);
        }
    }

    public void finalize() {
        try {
            close();
        } catch (ReadWriteException ioe) {
            Tools.handleCriticalError(ioe);
        }
    }

    private void _ensureStreamIsClosed(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException io) {
                Globals.getLogger().warn("I/O error when attempting to close stream", io);
            }
        }
    }

    private void _checkState() {
        if (!_isOpen) {
            throw new IllegalStateException("this stream has been closed");
        }
    }
}

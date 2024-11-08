package net.cobra84.jstream.plugins;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.JFileChooser;
import net.cobra84.jstream.*;

public final class PluginI_File extends InputPlugin {

    private static final String NAME = "File input";

    private static final String AUTHOR = "Jeremy.J <joly.jeremy@gmail.com>";

    private static final String DESCRIPTION = null;

    private static final String VERSION = "1.2";

    public static final String INPUT_MODE_ALL = "all";

    public static final String INPUT_MODE_DELIMITER = "delimiter";

    public static final String INPUT_MODE_FIXED = "fixed";

    private static final int DEFAULT_READBUFFER_SIZE = 16777216;

    private FileInputStream _fileInputStream;

    private FileChannel _in;

    private boolean _endOfFile;

    private int _bytesReaded;

    private int _bytesRemaining;

    private int _readBufferSize;

    public PluginI_File() {
        super(NAME, AUTHOR, DESCRIPTION, VERSION);
        setProperty("inputMode", INPUT_MODE_ALL);
    }

    public PluginI_File(String filename) {
        super(NAME, AUTHOR, DESCRIPTION, VERSION);
        setProperty("filename", filename);
        setProperty("inputMode", INPUT_MODE_ALL);
    }

    public PluginI_File(String filename, String inputMode) {
        super(NAME, AUTHOR, DESCRIPTION, VERSION);
        setProperty("filename", filename);
        setProperty("inputMode", inputMode);
    }

    public void init() throws PluginException {
        if (!getProperty("inputMode").equals(INPUT_MODE_ALL) && !getProperty("inputMode").equals(INPUT_MODE_FIXED) && !getProperty("inputMode").equals(INPUT_MODE_DELIMITER)) {
            raiseException("Invalid input mode");
        }
        if (getProperty("filename") == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select input file");
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                setProperty("filename", fileChooser.getSelectedFile().getAbsolutePath());
            } else {
                raiseException("File selection canceled by user");
            }
        }
        try {
            _fileInputStream = new FileInputStream(getProperty("filename"));
        } catch (FileNotFoundException e) {
            raiseException(e.getMessage());
        }
        if (getProperty("readBufferSize") != null) {
            _readBufferSize = Integer.parseInt(getProperty("readBufferSize"));
        } else {
            _readBufferSize = DEFAULT_READBUFFER_SIZE;
        }
        _in = _fileInputStream.getChannel();
        _bytesRemaining = (int) new java.io.File(getProperty("filename")).length();
    }

    public JSBuffer read() throws PluginException {
        if (_endOfFile) return null;
        if (getProperty("inputMode").equals(INPUT_MODE_ALL)) {
            return readAll();
        } else if (getProperty("inputMode").equals(INPUT_MODE_FIXED)) {
            return readFixed();
        } else if (getProperty("inputMode").equals(INPUT_MODE_DELIMITER)) {
        }
        return null;
    }

    private JSBuffer readAll() throws PluginException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(_bytesRemaining);
        try {
            _in.read(buffer);
            _endOfFile = true;
        } catch (IOException e) {
            throw new PluginException(e.getMessage());
        }
        return new JSBuffer(buffer, _bytesRemaining);
    }

    private JSBuffer readFixed() throws PluginException {
        int bytesToRead = 0;
        if (_bytesRemaining >= _readBufferSize) {
            bytesToRead = _readBufferSize;
        } else {
            bytesToRead = _bytesRemaining;
            _endOfFile = true;
        }
        _bytesRemaining -= bytesToRead;
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytesToRead);
        try {
            _in.read(buffer);
        } catch (IOException e) {
            throw new PluginException(e.getMessage());
        }
        return new JSBuffer(buffer, bytesToRead);
    }

    public void unload() {
        try {
            _fileInputStream.close();
            _in.close();
        } catch (Exception e) {
        }
    }
}

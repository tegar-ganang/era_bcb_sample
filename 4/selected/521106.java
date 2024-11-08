package net.cobra84.jstream.plugins;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.channels.*;
import javax.swing.JFileChooser;
import net.cobra84.jstream.*;

public final class PluginO_File extends OutputPlugin {

    private static final String NAME = "File output";

    private static final String AUTHOR = "Jeremy.J <joly.jeremy@gmail.com>";

    private static final String DESCRIPTION = null;

    private static final String VERSION = "1.1";

    private FileChannel _fileChannel;

    public PluginO_File() {
        super(NAME, AUTHOR, DESCRIPTION, VERSION);
    }

    public PluginO_File(String filename) {
        super(NAME, AUTHOR, DESCRIPTION, VERSION);
        setProperty("filename", filename);
    }

    public void init() throws PluginException {
        if (getProperty("filename") == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select output file");
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                setProperty("filename", fileChooser.getSelectedFile().getAbsolutePath());
            } else {
                raiseException("File selection canceled by user");
            }
        }
        try {
            _fileChannel = new FileOutputStream(getProperty("filename"), false).getChannel();
        } catch (FileNotFoundException e) {
            raiseException(e.getMessage());
        }
    }

    public void write(JSBuffer jsBuffer) throws PluginException {
        try {
            _fileChannel.write(jsBuffer.getBuffer());
        } catch (Exception e) {
            raiseException(e.getMessage());
        }
    }

    public void unload() {
        try {
            _fileChannel.close();
        } catch (Exception e) {
        }
    }
}

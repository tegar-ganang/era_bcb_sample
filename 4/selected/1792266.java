package ch.amotta.qweely.wave;

import ch.amotta.qweely.wave.chunks.WAVEDataChunk;
import ch.amotta.qweely.wave.chunks.WAVEFormatChunk;

/**
 *
 * @author Alessandro
 */
public class WAVEPCMFile extends WAVEFile {

    private WAVESong _song;

    public WAVEPCMFile(String path) {
        super(path);
        registerChunk(new WAVEFormatChunk(this));
    }

    public WAVESong getSong() {
        if (_song == null) createSong();
        return _song;
    }

    private void createSong() {
        _song = new WAVESong();
        _song.setSampleRate(((WAVEFormatChunk) getChunk("fmt ")).getSampleRate());
        _song.setBitDepth(((WAVEFormatChunk) getChunk("fmt ")).getBitDepth());
        _song.setChannels(((WAVEDataChunk) (getChunk("data"))).getChannels());
    }
}

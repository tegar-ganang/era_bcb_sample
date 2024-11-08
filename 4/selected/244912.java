package net.sf.zcatalog.fs;

import net.sf.zcatalog.MiscUtils;
import net.sf.zcatalog.xml.jaxb.AudioMeta;
import net.sf.zcatalog.xml.jaxb.FileMeta;
import entagged.audioformats.*;
import entagged.audioformats.exceptions.*;
import net.sf.zcatalog.xml.jaxb.AudioChannelMode;

/**
 * Traverser for audio files.
 * @author Alessandro Zigliani
 * @since ZCatalog 0.9
 * @version 0.9
 */
class AudioFileTraverser extends FileTraverser {

    /**
	 * Creates the FileMeta object.
	 * If the {@link TraverserProfile} requires extracting metadata,
	 * the returned object is instance of {@link AudioMeta},
	 * otherwise it's simply instance of {@link FileMeta}.
	 * @param opt the traverser profile
	 * @return the FileMeta
	 */
    @Override
    protected final FileMeta createFileMeta(TraverserProfile opt) {
        FileMeta fm = super.createFileMeta(opt);
        if (opt.getAudioFileInfo(type)) {
            AudioMeta am = new AudioMeta();
            if (fillMetaData(am)) {
                copyFields(fm, am);
                return am;
            }
        }
        return fm;
    }

    /**
     * Read metadata from the audio file pointed by {@link #obj}.
     * @param am the {@link AudioMeta} instance to fill
     * @return true if metadata extraction was successfull
     */
    protected final boolean fillMetaData(AudioMeta am) {
        AudioFile entagF;
        Tag tag;
        String s;
        try {
            entagF = AudioFileIO.read(obj);
            tag = entagF.getTag();
        } catch (CannotReadException e) {
            return false;
        }
        am.setBitRate(entagF.getBitrate());
        am.setLength(entagF.getLength());
        am.setSampleRate(entagF.getSamplingRate());
        switch(entagF.getChannelNumber()) {
            case 1:
                am.setChMode(AudioChannelMode.MONO);
                break;
            case 2:
                am.setChMode(AudioChannelMode.STEREO);
                break;
            default:
                am.setChMode(AudioChannelMode.UNKNOWN);
                break;
        }
        am.setMime(s = type.getBaseType());
        if (s.compareTo("audio/mpeg") == 0) {
            am.setAlbum(MiscUtils.nullString(tag.getFirstAlbum()));
            am.setArtist(MiscUtils.nullString(tag.getFirstArtist()));
            am.setGenre(MiscUtils.nullString(tag.getFirstGenre()));
            am.setTitle(MiscUtils.nullString(tag.getFirstTitle()));
            am.setTrack(MiscUtils.nullString(tag.getFirstTrack()));
            am.setYear(MiscUtils.nullString(tag.getFirstYear()));
        }
        return true;
    }
}

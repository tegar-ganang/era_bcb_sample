package zcatalog.fs;

import zcatalog.xml.JAXBGlobals;
import javax.swing.ImageIcon;
import zcatalog.db.*;
import zcatalog.xml.jaxb.IconSize;
import zcatalog.xml.*;
import zcatalog.xml.jaxb.*;
import javax.xml.bind.*;
import java.io.*;
import java.util.*;
import entagged.audioformats.*;
import entagged.audioformats.generic.*;
import entagged.audioformats.exceptions.*;
import zcatalog.*;
import zcatalog.ui.Icons;

class ZCatAudioFile extends ZCatObject {

    protected FSAudioFile audioFile;

    protected ZCatAudioFile(FSAudioFile f) {
        this.audioFile = f;
    }

    @Override
    public String getName() {
        return audioFile.getFile().getName();
    }

    @Override
    protected Marshaller getMarshaller() throws JAXBException {
        return JAXBGlobals.FSAUDIOFILE_CONTEXT.createMarshaller();
    }

    @Override
    protected Object getJAXBObject() {
        return JAXBGlobals.OBJ_FACTORY.createAudioFile(audioFile);
    }

    @Override
    public String getXMLSchema() {
        return XMLResources.load("FSAudioFile.xsd");
    }

    protected static ZCatObject createFrom(TempFile fi) {
        FSAudioFile af;
        AudioFile entagF;
        Tag tag;
        TagField tagField;
        String s;
        try {
            entagF = AudioFileIO.read(new File(fi.file.getName()));
            tag = entagF.getTag();
        } catch (CannotReadException e) {
            return new ZCatFile(fi);
        }
        af = new FSAudioFile();
        af.setFile(fi.file);
        af.setBitRate(entagF.getBitrate());
        af.setLength(entagF.getLength());
        af.setSampleRate(entagF.getSamplingRate());
        switch(entagF.getChannelNumber()) {
            case 1:
                af.setChMode(AudioChannelMode.MONO);
                break;
            case 2:
                af.setChMode(AudioChannelMode.STEREO);
                break;
            default:
                af.setChMode(AudioChannelMode.UNKNOWN);
                break;
        }
        af.setMime(s = fi.type.getBaseType());
        if (s.compareTo("audio/mpeg") == 0) getMPEGInfo(af, tag);
        return new ZCatAudioFile(af);
    }

    protected static void getMPEGInfo(FSAudioFile af, Tag tag) {
        af.setAlbum(nulls(tag.getFirstAlbum()));
        af.setArtist(nulls(tag.getFirstArtist()));
        af.setGenre(nulls(tag.getFirstGenre()));
        af.setTitle(nulls(tag.getFirstTitle()));
        af.setTrack(nulls(tag.getFirstTrack()));
        af.setYear(nulls(tag.getFirstYear()));
    }

    private static String nulls(String s) {
        if (s != null && s.compareTo("") == 0) s = null;
        return s;
    }

    @Override
    public ImageIcon getIcon(IconSize size) {
        return Icons.getBundledIcon("audio/mpeg").getIcon(size);
    }
}

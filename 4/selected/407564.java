package net.sf.zcatalog.fs;

import entagged.audioformats.*;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.generic.*;
import net.sf.zcatalog.db.ZCatMetaRecipient;
import java.io.File;
import net.sf.zcatalog.xml.XMLMimeType;
import net.sf.zcatalog.xml.jaxb.*;
import net.sf.zcatalog.*;
import net.sf.zcatalog.db.ZCatDb;
import net.sf.zcatalog.db.ZCatObject;

/**
 *
 * @author Alessandro Zigliani
 */
class MountedFolderTraverser extends Traverser {

    int fileCount, imageCount, archiveCount, audioCount;

    FSEntry root_;

    EnumObserver obs;

    TraverserProfile profile;

    ZCatMetaRecipient recipient;

    MountedFolderTraverser() {
        fileCount = imageCount = archiveCount = audioCount = 0;
    }

    @Override
    public void enumerate(EnumObserver prgObserver) throws Exception {
        assert (root != null && root.exists() && root.canRead());
        obs = prgObserver;
        obs.notifyActionDescr("Step 1 - Recursively reading subtree");
        Meta fake = new Meta();
        FileMeta fileMeta = new FileMeta();
        FolderMeta folderMeta = new FolderMeta();
        FileCount fc = new FileCount();
        fake.setFolder(folderMeta);
        fake.setFile(fileMeta);
        folderMeta.setFileCount(fc);
        root_ = descend_step1(root, fake);
    }

    private final FSEntry descend_step1(File f, Meta parent) throws Exception {
        FSEntry res = new FSEntry(f);
        FileMeta fileMeta = res.getFile();
        FolderMeta folderMeta = res.getFolder();
        FileCount parentfc = parent.getFolder().getFileCount();
        FileMeta parentFileMeta = parent.getFile();
        long fileSize;
        ZCatPrimaryType zcatType = res.getZCatType();
        obs.notifyObjectName(res.getPath());
        if (fileMeta.isSetLink()) {
            fileCount++;
            if (zcatType == ZCatPrimaryType.FOLDER) return res;
        }
        fileSize = fileMeta.getDu();
        if (folderMeta != null) {
            File children[] = f.listFiles();
            FolderMeta fm = res.getFolder();
            FileCount fc = fm.getFileCount();
            fc.setDirs(0);
            fc.setFiles(0);
            fc.setLocalDirs(0);
            if (children != null) {
                int n = children.length;
                res.children = new FSEntry[n];
                fileCount += n;
                for (int i = 0; i < n; i++) {
                    res.children[i] = descend_step1(children[i], res);
                }
            }
            parentfc.setLocalDirs(parentfc.getLocalDirs() + 1);
            parentfc.setDirs(parentfc.getDirs() + fc.getDirs() + 1);
            parentfc.setFiles(parentfc.getFiles() + fc.getFiles());
            parentFileMeta.setDu(parentFileMeta.getDu() + fileSize);
            return res;
        } else switch(zcatType) {
            case AUDIO:
                audioCount++;
                break;
            case IMAGE:
                imageCount++;
                break;
            case ARCHIVE:
                archiveCount++;
                break;
            default:
        }
        parentfc.setFiles(parentfc.getFiles() + 1);
        parentFileMeta.setDu(parentFileMeta.getDu() + fileSize);
        return res;
    }

    @Override
    public void extractMetadata(ZCatDb db, EnumObserver prgObserver, TraverserProfile opt) throws Exception {
        this.recipient = db.newRecipient();
        this.obs = prgObserver;
        this.profile = opt;
        obs.notifyActionDescr("Step 2 - Extracting metadata from files.");
        obs.setBounds(0, fileCount);
        recipient.begin(root_);
        doCommon(root_);
        for (FSEntry f : root_.children) {
            descend_step2(root_, f);
        }
    }

    private void descend_step2(FSEntry parent, FSEntry child) throws Exception {
        XMLMimeType t = child.getMimeType();
        switch(child.getZCatType()) {
            case IMAGE:
                if (profile.checkImagePreview(t)) doImage(child);
                break;
            case AUDIO:
                if (profile.checkAudioFileInfo(t)) doAudio(child);
                break;
            case ARCHIVE:
                if (profile.checkArchiveAsFolder(t)) doArchive(child);
                break;
            default:
        }
        doCommon(child);
        recipient.put(parent, child);
        if (child.children != null) {
            for (FSEntry gf : child.children) {
                descend_step2(child, gf);
            }
        }
    }

    /** 
     * Strips the path part off the object's name.
     * @param m th
     */
    void doCommon(FSEntry m) {
        obs.notifyObjectName(m.jfile.getPath());
        m.setName(m.jfile.getName());
    }

    /**
     * Extracts and add {@link ImageMeta} to the {@link Meta} info
     * replacing the {@link FileMeta}.<br>
     * The processed image is the file whose address is returned by
     * <code> m.getName() </code> (in fact this method must be called 
     * <b>before</b>
     * {@link #doCommon(net.sf.zcatalog.xml.jaxb.Meta)}).<br>
     * 
     * @param m the Meta to update
     */
    static void doImage(FSEntry m) {
        GFLImage i;
        ImageMeta im;
        FileMeta fm = m.getFile();
        try {
            i = GFL.loadImageInfo(m.getPath());
            m.setThumb(i.getPreview());
            im = new ImageMeta();
            im.setMime(m.getMimeType().getBaseType());
            im.setRes(i.getResolution());
            copyFields(fm, im);
        } catch (Exception e) {
            e.printStackTrace();
            m.setThumb(null);
            return;
        }
        m.setFile(im);
    }

    /**
     * Extracts and add {@link ArchiveData} to the {@link Meta} object.
     * The files contained into the archive are then added as children 
     * of the archive itself. This means the file is regarded both as
     * a (compressed) folder and as a file from now on. <br>
     * The processed archive is the file whose address is returned by
     * <code> m.getName() </code> (in fact this method must be called 
     * <b>before</b>
     * {@link #doCommon(net.sf.zcatalog.xml.jaxb.Meta)}).<br>
     * 
     * @param m the Meta to update
     */
    static void doArchive(FSEntry m) {
    }

    /**
     * Extracts and add {@link AudioMeta} to the {@link Meta} object by
     * replacing the {@link FileMeta} with the more specialized version.
     * The processed archive is the file whose address is returned by
     * <code> m.getName() </code> (in fact this method must be called 
     * <b>before</b>
     * {@link #doCommon(net.sf.zcatalog.xml.jaxb.Meta)}).<br>
     * 
     * @param m the Meta to update
     */
    static void doAudio(FSEntry m) {
        FileMeta fm = m.getFile();
        AudioMeta am = new AudioMeta();
        AudioFile entagF;
        Tag tag;
        String s;
        try {
            entagF = AudioFileIO.read(m.getAsFile());
            tag = entagF.getTag();
        } catch (CannotReadException e) {
            return;
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
        am.setMime(s = m.getMimeType().getBaseType());
        if (s.compareTo("audio/mpeg") == 0) {
            am.setAlbum(nulls(tag.getFirstAlbum()));
            am.setArtist(nulls(tag.getFirstArtist()));
            am.setGenre(nulls(tag.getFirstGenre()));
            am.setTitle(nulls(tag.getFirstTitle()));
            am.setTrack(nulls(tag.getFirstTrack()));
            am.setYear(nulls(tag.getFirstYear()));
        }
        copyFields(fm, am);
        m.setFile(am);
    }

    private static String nulls(String s) {
        if (s != null && s.compareTo("") == 0) s = null;
        return s;
    }

    @Override
    public int getImageCount() {
        return imageCount;
    }

    @Override
    public int getArchiveCount() {
        return archiveCount;
    }

    @Override
    public int getAudioCount() {
        return audioCount;
    }

    @Override
    public int getUnsupportedCount() {
        return fileCount - (imageCount + archiveCount + audioCount);
    }

    @Override
    public ZCatObject commit(ProgressObserver obs) throws Exception {
        return recipient.commit(obs);
    }

    private static final void copyFields(FileMeta src, FileMeta dest) {
        dest.setDu(src.getDu());
        dest.setModTime(src.getModTime());
    }

    private static final void copyFields(FolderMeta src, FolderMeta dest) {
        dest.setFileCount(src.getFileCount());
    }
}

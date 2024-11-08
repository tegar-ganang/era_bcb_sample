package de.psychomatic.mp3db.gui.threads.backup.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import de.psychomatic.messagesystem.MessageEvent;
import de.psychomatic.messagesystem.MessageEventTypeEnum;
import de.psychomatic.messagesystem.MessageSenderIf;
import de.psychomatic.mp3db.core.dblayer.dao.AlbumDAO;
import de.psychomatic.mp3db.core.dblayer.dao.CdDAO;
import de.psychomatic.mp3db.core.dblayer.dao.CoveritemDAO;
import de.psychomatic.mp3db.core.dblayer.dao.GenericDAO;
import de.psychomatic.mp3db.core.dblayer.dao.MediafileDAO;
import de.psychomatic.mp3db.core.interfaces.AlbumIf;
import de.psychomatic.mp3db.core.interfaces.CdIf;
import de.psychomatic.mp3db.core.interfaces.CoveritemIf;
import de.psychomatic.mp3db.core.interfaces.MediafileIf;
import de.psychomatic.mp3db.core.utils.AbstractStatusRunnable;
import de.psychomatic.mp3db.core.utils.events.StatusEvent;
import de.psychomatic.mp3db.core.utils.events.StatusEvent.StatusEventType;
import de.psychomatic.mp3db.gui.Main;
import de.psychomatic.mp3db.gui.modules.WaitDialog;
import de.psychomatic.mp3db.gui.threads.backup.TypeConstants;
import de.psychomatic.mp3db.gui.utils.GuiStrings;

/**
 * Backupthread
 * @author Kykal
 */
public class BackupRunnable extends AbstractStatusRunnable {

    /**
     * Logger for this class
     */
    static final Logger LOG = Logger.getLogger(BackupRunnable.class);

    boolean _break;

    java.io.File _file;

    MessageSenderIf _messenger;

    WaitDialog _wd;

    private final Set<String> _usedFilenames;

    /**
     * Creates the backupthread
     * @param messenger Messagesender
     * @param file File to backup data
     * @param wd Waiting dialog
     */
    public BackupRunnable(final MessageSenderIf messenger, final java.io.File file) {
        _messenger = messenger;
        _file = file;
        _usedFilenames = new HashSet<String>();
    }

    void exportAlbum(final XMLStreamWriter writer, final AlbumIf album, final Integer id) throws XMLStreamException {
        writer.writeAttribute("id", id.toString());
        writer.writeAttribute("name", album.getAlbum());
        writer.writeAttribute("type", Integer.toString(album.getAlbumType().ordinal()));
    }

    void exportCd(final XMLStreamWriter writer, final CdIf cd, final Integer id) throws XMLStreamException {
        writer.writeAttribute("id", Integer.toString(id));
        writer.writeAttribute("name", cd.getCdName());
        writer.writeAttribute("md5", cd.getCdMd5());
    }

    void exportCoveritem(final XMLStreamWriter writer, final ZipOutputStream out, final CoveritemIf item, final Integer id) throws XMLStreamException, IOException {
        writer.writeStartElement(TypeConstants.XML_COVERITEM);
        writer.writeAttribute("id", id.toString());
        writer.writeAttribute("type", item.getCitype());
        String filename = RandomStringUtils.random(10, "abcdefghijklmnopqrstuvwxyz0123456789");
        while (_usedFilenames.contains(filename)) {
            filename = RandomStringUtils.random(10, "abcdefghijklmnopqrstuvwxyz0123456789");
        }
        out.putNextEntry(new ZipEntry(filename));
        IOUtils.write(item.getCidata(), out);
        out.closeEntry();
        writer.writeAttribute("filename", filename);
        _usedFilenames.add(filename);
        writer.writeEndElement();
    }

    void exportMediafile(final XMLStreamWriter writer, final MediafileIf mediafile, final Integer id) throws XMLStreamException {
        writer.writeAttribute("id", id.toString());
        writer.writeAttribute("artist", mediafile.getArtist() == null ? "" : mediafile.getArtist());
        writer.writeAttribute("title", mediafile.getTitle() == null ? "" : mediafile.getTitle());
        writer.writeAttribute("bitrate", Integer.toString(mediafile.getBitrate()));
        writer.writeAttribute("filesize", Long.toString(mediafile.getFilesize()));
        writer.writeAttribute("playtime", Integer.toString(mediafile.getPlaytime()));
        writer.writeAttribute("path", mediafile.getPath());
    }

    @Override
    public void reset() {
    }

    @Override
    public void run() {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Backupthread started");
            }
            if (_file.exists()) {
                _file.delete();
            }
            final ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(_file));
            zOut.setLevel(9);
            final File xmlFile = File.createTempFile("mp3db", ".xml");
            final OutputStream ost = new FileOutputStream(xmlFile);
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(ost, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("mp3db");
            writer.writeAttribute("version", Integer.toString(Main.ENGINEVERSION));
            final MediafileDAO mfDAO = new MediafileDAO();
            final AlbumDAO aDAO = new AlbumDAO();
            final CdDAO cdDAO = new CdDAO();
            final CoveritemDAO ciDAO = new CoveritemDAO();
            int itemCount = 0;
            try {
                itemCount += mfDAO.getCount();
                itemCount += aDAO.getCount();
                itemCount += cdDAO.getCount();
                itemCount += ciDAO.getCount();
                fireStatusEvent(new StatusEvent(this, StatusEventType.MAX_VALUE, itemCount));
            } catch (final Exception e) {
                LOG.error("Error getting size", e);
                fireStatusEvent(new StatusEvent(this, StatusEventType.MAX_VALUE, -1));
            }
            int cdCounter = 0;
            int mediafileCounter = 0;
            int albumCounter = 0;
            int coveritemCounter = 0;
            int counter = 0;
            final List<CdIf> data = cdDAO.getCdsOrderById();
            if (data.size() > 0) {
                final Map<Integer, Integer> albums = new HashMap<Integer, Integer>();
                final Iterator<CdIf> it = data.iterator();
                while (it.hasNext() && !_break) {
                    final CdIf cd = it.next();
                    final Integer cdId = Integer.valueOf(cdCounter++);
                    writer.writeStartElement(TypeConstants.XML_CD);
                    exportCd(writer, cd, cdId);
                    fireStatusEvent(new StatusEvent(this, StatusEventType.NEW_VALUE, ++counter));
                    final List<MediafileIf> files = cd.getMediafiles();
                    final Iterator<MediafileIf> mfit = files.iterator();
                    MediafileIf mf;
                    while (mfit.hasNext() && !_break) {
                        mf = mfit.next();
                        final Integer mfId = Integer.valueOf(mediafileCounter++);
                        writer.writeStartElement(TypeConstants.XML_MEDIAFILE);
                        exportMediafile(writer, mf, mfId);
                        fireStatusEvent(new StatusEvent(this, StatusEventType.NEW_VALUE, ++counter));
                        final AlbumIf a = mf.getAlbum();
                        if (a != null) {
                            Integer inte;
                            if (albums.containsKey(a.getAid())) {
                                inte = albums.get(a.getAid());
                                writeLink(writer, TypeConstants.XML_ALBUM, inte);
                            } else {
                                inte = Integer.valueOf(albumCounter++);
                                writer.writeStartElement(TypeConstants.XML_ALBUM);
                                exportAlbum(writer, a, inte);
                                fireStatusEvent(new StatusEvent(this, StatusEventType.NEW_VALUE, ++counter));
                                albums.put(a.getAid(), inte);
                                if (a.hasCoveritems() && !_break) {
                                    final List<CoveritemIf> covers = a.getCoveritems();
                                    final Iterator<CoveritemIf> coit = covers.iterator();
                                    while (coit.hasNext() && !_break) {
                                        final Integer coveritemId = Integer.valueOf(coveritemCounter++);
                                        exportCoveritem(writer, zOut, coit.next(), coveritemId);
                                        fireStatusEvent(new StatusEvent(this, StatusEventType.NEW_VALUE, ++counter));
                                    }
                                }
                                writer.writeEndElement();
                            }
                            GenericDAO.getEntityManager().close();
                        }
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();
                    writer.flush();
                    it.remove();
                    GenericDAO.getEntityManager().close();
                }
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            ost.flush();
            ost.close();
            if (_break) {
                zOut.close();
                _file.delete();
            } else {
                zOut.putNextEntry(new ZipEntry("mp3.xml"));
                final InputStream xmlIn = FileUtils.openInputStream(xmlFile);
                IOUtils.copy(xmlIn, zOut);
                xmlIn.close();
                zOut.close();
            }
            xmlFile.delete();
            fireStatusEvent(new StatusEvent(this, StatusEventType.FINISH));
        } catch (final Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error backup database", e);
            }
            fireStatusEvent(new StatusEvent(this, e, ""));
            _messenger.fireMessageEvent(new MessageEvent(this, "ERROR", MessageEventTypeEnum.ERROR, GuiStrings.getInstance().getString("error.backup"), e));
        }
    }

    @Override
    public void stopProcess() {
        _break = true;
    }

    public void writeLink(final XMLStreamWriter writer, final String type, final Integer id) throws XMLStreamException {
        writer.writeStartElement(TypeConstants.XML_LINK);
        writer.writeAttribute("type", type);
        writer.writeAttribute("targetid", id.toString());
        writer.writeEndElement();
    }
}

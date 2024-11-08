package so.n_3.musicbox.model;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 *
 * @author oasynnoum
 */
public class MusicBox {

    private static String playlistXmlPath;

    private static MusicBox instance = new MusicBox();

    private Vector<Playlist> playlists;

    private MusicBox() {
        this.playlists = new Vector<Playlist>();
    }

    public Playlist getDefaultPlaylist() {
        return this.playlists.get(0);
    }

    public static String getPlaylistXmlPath() {
        return MusicBox.playlistXmlPath;
    }

    public Music getDefaultMusic() {
        Playlist playlist = this.playlists.get(0);
        if (playlist == null) {
            return null;
        }
        return playlist.getMusic(0);
    }

    public Playlist getPlaylistById(String playlistId) {
        Playlist playlist = null;
        for (int i = 0; i < this.playlists.size(); i++) {
            Playlist _playlist = this.playlists.get(i);
            String _playlistId = _playlist.getId();
            if (_playlistId.equals(playlistId)) {
                playlist = _playlist;
                break;
            }
        }
        return playlist;
    }

    private static synchronized void initPlaylistXmlPath(ServletContext servletContext) {
        String tmporaryDirectoryPath = System.getProperty("java.io.tmpdir");
        File playlistXml = new File(tmporaryDirectoryPath + System.getProperty("file.separator") + "playlist.xml");
        if (!playlistXml.exists()) {
            try {
                File playlistTemplateXml = new File(servletContext.getRealPath("playlist.xml"));
                FileUtils.copyFile(playlistTemplateXml, playlistXml);
            } catch (IOException ex) {
                Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        MusicBox.playlistXmlPath = playlistXml.getAbsolutePath();
    }

    public static synchronized void init(ServletContext servletContext) {
        initPlaylistXmlPath(servletContext);
        init(playlistXmlPath);
    }

    public static synchronized void init(String playlistXmlPath) {
        MusicBox.playlistXmlPath = playlistXmlPath;
        File fileObject;
        try {
            MusicBox.instance.playlists.clear();
            fileObject = new File(MusicBox.playlistXmlPath);
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = docBuilder.parse(fileObject);
            NodeList playlistNodes = document.getElementsByTagName("playlist");
            for (int i = 0; i < playlistNodes.getLength(); i++) {
                Playlist playlist = new Playlist();
                Element playlistElem = (Element) playlistNodes.item(i);
                playlist.setId(playlistElem.getAttribute("id"));
                playlist.setName(playlistElem.getAttribute("name"));
                playlist.setUrl(playlistElem.getAttribute("url"));
                NodeList musicNodes = playlistElem.getElementsByTagName("music");
                for (int j = 0; j < musicNodes.getLength(); j++) {
                    Element musicElem = (Element) musicNodes.item(j);
                    Music music = new Music(musicElem.getAttribute("url"), Integer.parseInt(musicElem.getAttribute("order")));
                    playlist.addMusic(music);
                }
                MusicBox.instance.playlists.add(playlist);
            }
        } catch (SAXException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        }
    }

    public synchronized boolean createPlaylist(String method, String playlistName, String sourceDirectoryPath) {
        boolean result = false;
        try {
            File playlistXml = new File(MusicBox.playlistXmlPath);
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = docBuilder.parse(playlistXml);
            Element musicboxElem = (Element) document.getElementsByTagName("musicbox").item(0);
            Element playlistElem = document.createElement("playlist");
            playlistElem.setAttribute("id", Playlist.generateId());
            playlistElem.setAttribute("name", playlistName);
            if (method.equalsIgnoreCase("empty")) {
            } else if (method.equalsIgnoreCase("directory")) {
                playlistElem.setAttribute("url", sourceDirectoryPath);
                File sourceDirectory = new File(sourceDirectoryPath);
                File[] mp3Files = sourceDirectory.listFiles(MusicBox.getMP3FileFilter());
                for (int i = 0; i < mp3Files.length; i++) {
                    File mp3File = mp3Files[i];
                    Element musicElem = document.createElement("music");
                    musicElem.setAttribute("url", mp3File.getAbsolutePath());
                    musicElem.setAttribute("order", Integer.toString(i));
                    playlistElem.appendChild(musicElem);
                }
            }
            musicboxElem.appendChild(playlistElem);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(playlistXml));
            MusicBox.init(MusicBox.playlistXmlPath);
            result = true;
        } catch (Exception ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public synchronized Document getPlaylistXmlDocument() {
        Document document = null;
        try {
            File playlistXml = new File(MusicBox.playlistXmlPath);
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = docBuilder.parse(playlistXml);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        return document;
    }

    public synchronized boolean savePlaylistXmlDocument(Document playlistXmlDocument) {
        boolean result = false;
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(playlistXmlDocument), new StreamResult(new File(playlistXmlPath)));
            MusicBox.init(playlistXmlPath);
            result = true;
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(MusicBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public static FileFilter getMP3FileFilter() {
        return new FileFilter() {

            @Override
            public boolean accept(File path) {
                return path.getName().matches("^.*\\.mp3$");
            }
        };
    }

    public static MusicBox getInstance() {
        return MusicBox.instance;
    }
}

package org.xmlvm.demo.java.photovm.net;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.xmlvm.demo.java.photovm.data.Album;
import org.xmlvm.demo.java.photovm.data.Photo;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Bundles functionality to retrieve data from Google's Picasa GDATA API.
 * 
 */
public class PicasaRequest {

    private static final String REQUEST_BASE_URL = "http://picasaweb.google.com/data/feed/api/user/";

    private static final Namespace ATOM_NAMESPACE = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    private static final Namespace MEDARSS_NAMESPACE = Namespace.getNamespace("http://search.yahoo.com/mrss/");

    /**
   * Requests photos from the given album URL.
   */
    public static List<Photo> requestPhotos(String url) {
        List<Photo> photos = new ArrayList<Photo>();
        Element feed = getGdataFeedElement(url);
        List<Element> entries = feed.getChildren("entry", ATOM_NAMESPACE);
        for (Element entry : entries) {
            photos.add(parsePhoto(entry));
        }
        return photos;
    }

    /**
   * Requests a list of albums for the given user id. When the response is
   * returned, the result is parsed and returned from this method.
   * 
   * @param userId A valid picasa user id.
   * @return A list of albums from the public albums stream of the picasa user.
   */
    public static List<Album> requestAlbums(String userId) {
        List<Album> albums = new ArrayList<Album>();
        Element feed = getGdataFeedElement(REQUEST_BASE_URL + userId);
        List<Element> entries = feed.getChildren("entry", ATOM_NAMESPACE);
        for (Element entry : entries) {
            albums.add(parseAlbum(entry));
        }
        return albums;
    }

    /**
   * Parses a given XML photo element, extracts the necessary data and creates a
   * Photo object.
   */
    private static Photo parsePhoto(Element photo) {
        Photo result = new Photo();
        result.setTitle(photo.getChildText("title", ATOM_NAMESPACE));
        Element mediaGroup = photo.getChild("group", MEDARSS_NAMESPACE);
        List<Element> thumbnails = mediaGroup.getChildren("thumbnail", MEDARSS_NAMESPACE);
        Element thumbnail = thumbnails.get(1);
        result.setThumbnailUrl(thumbnail.getAttributeValue("url"));
        result.setThumbnailWidth(thumbnail.getAttributeValue("width"));
        result.setThumbnailHeight(thumbnail.getAttributeValue("height"));
        return result;
    }

    /**
   * Parses a given XML album element, extracts the necessary data and creates
   * an Album object.
   */
    private static Album parseAlbum(Element album) {
        Album result = new Album();
        result.setTitle(album.getChildText("title", ATOM_NAMESPACE));
        Element mediaGroup = album.getChild("group", MEDARSS_NAMESPACE);
        List<Element> thumbnails = mediaGroup.getChildren("thumbnail", MEDARSS_NAMESPACE);
        Element thumbnail = thumbnails.get(0);
        result.setCoverUrl(thumbnail.getAttributeValue("url"));
        List<Element> links = album.getChildren("link", ATOM_NAMESPACE);
        result.setPhotoRequestUrl(links.get(0).getAttributeValue("href"));
        result.setCoverWidth(thumbnail.getAttributeValue("width"));
        result.setCoverHeight(thumbnail.getAttributeValue("height"));
        return result;
    }

    /**
   * For a given GDATA URL, this function requests the XML document, parses it's
   * content and returns the XML feed element.
   */
    private static Element getGdataFeedElement(String urlStr) {
        try {
            URL url = new URL(urlStr);
            Document xmlDoc = new SAXBuilder().build(url.openStream());
            Element feed = xmlDoc.getRootElement();
            return feed;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

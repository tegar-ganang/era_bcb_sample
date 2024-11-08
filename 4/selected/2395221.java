package jeocache.util;

import jaxb.googleearth.DocumentType;
import jaxb.googleearth.FolderType;
import jaxb.googleearth.IconStyleIconType;
import jaxb.googleearth.IconStyleType;
import jaxb.googleearth.KmlType;
import jaxb.googleearth.LinkType;
import jaxb.googleearth.LookAtType;
import jaxb.googleearth.NetworkLinkType;
import jaxb.googleearth.ObjectFactory;
import jaxb.googleearth.PlacemarkType;
import jaxb.googleearth.PointType;
import jaxb.googleearth.RefreshModeEnum;
import jaxb.googleearth.StyleType;
import jaxb.googleearth.TimeStampType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import javax.swing.JTable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import jeocache.cont.AppGlobals;
import jeocache.cont.Caches;
import jeocache.cont.ExecResult;
import jeocache.cont.Home;
import jeocache.cont.Waypoint;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

public class KmlFile {

    public static ExecResult startEarth(File file) {
        int res = 0;
        if (AppGlobals.getPrefs().getProgramSettings().getGeneral().getGoogleearthPath() == null) {
            return new ExecResult(-1, "googleearth", "Unable to find an executable for googlearth. Please check your configuration settings.");
        }
        File exectest = new File(AppGlobals.getPrefs().getProgramSettings().getGeneral().getGoogleearthPath());
        if (!exectest.canExecute()) {
            return new ExecResult(-1, exectest.getPath(), "Unable to find an executable for " + exectest.getPath() + ". Please check your configuration settings.");
        }
        Runtime r = Runtime.getRuntime();
        StreamReader sr = new StreamReader();
        String cmd = AppGlobals.getPrefs().getProgramSettings().getGeneral().getGoogleearthPath() + " " + file.getPath();
        try {
            Process p = r.exec(cmd);
            sr.startReaders(p.getInputStream(), p.getErrorStream());
            res = p.waitFor();
        } catch (Exception e) {
            AppGlobals.addMessageText(e, AppGlobals.MsgPriority.ERROR);
            AppGlobals.addMessageTextMore(cmd);
            sr.stopReaders();
            return new ExecResult(-1, cmd, e.toString());
        }
        String buf = sr.stopReaders();
        return new ExecResult(res, cmd, buf);
    }

    public static void saveNetKmlFile(File netkmlfile, File datakmlfile, File flykmlfile, Home home) {
        saveFlyKmlFile(flykmlfile, home);
        ObjectFactory objFactory = new ObjectFactory();
        LinkType link1 = objFactory.createLinkType();
        link1.setHref(datakmlfile.getPath());
        link1.setRefreshMode(RefreshModeEnum.ON_INTERVAL);
        link1.setRefreshInterval((float) 2.0);
        NetworkLinkType nl1 = objFactory.createNetworkLinkType();
        nl1.setName("JeoCache Filter");
        nl1.setDescription("GoogleEarth LiveLink from JeoCache");
        nl1.setFlyToView(false);
        nl1.setRefreshVisibility(false);
        nl1.setUrl(link1);
        LinkType link2 = objFactory.createLinkType();
        link2.setHref(flykmlfile.getPath());
        NetworkLinkType nl2 = objFactory.createNetworkLinkType();
        nl2.setName("JeoCache FlyTo");
        nl2.setDescription("GoogleEarth LiveLink from JeoCache");
        nl2.setFlyToView(true);
        nl2.setRefreshVisibility(false);
        nl2.setUrl(link2);
        FolderType folder = objFactory.createFolderType();
        folder.setName("JeoCache LiveLink");
        folder.setDescription("GoogleEarth LiveLink from JeoCache");
        folder.getFeature().add(objFactory.createNetworkLink(nl1));
        folder.getFeature().add(objFactory.createNetworkLink(nl2));
        KmlType kml = objFactory.createKmlType();
        kml.setFeature(objFactory.createFolder(folder));
        FileOutputStream fos = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("jaxb.googleearth");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", true);
            fos = new FileOutputStream(netkmlfile);
            marshaller.marshal(objFactory.createKml(kml), fos);
        } catch (Exception e) {
            AppGlobals.addMessageText(e, AppGlobals.MsgPriority.ERROR);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static void exportToKml(JTable table, File file, Caches caches) {
        Caches export = GpxFile.getViewedCaches(table, caches);
        saveDataKmlFile(file, export);
    }

    public static void saveDataKmlFile(File file, Caches caches) {
        ObjectFactory objFactory = new ObjectFactory();
        HashSet cacheTypes = new HashSet();
        cacheTypes.add(AppGlobals.TYPE_TRADITIONAL);
        cacheTypes.add(AppGlobals.TYPE_MULTI);
        cacheTypes.add(AppGlobals.TYPE_VIRTUAL);
        cacheTypes.add(AppGlobals.TYPE_LETTER);
        cacheTypes.add(AppGlobals.TYPE_EVENT);
        cacheTypes.add(AppGlobals.TYPE_UNKNOWN);
        FolderType folder = objFactory.createFolderType();
        folder.setName("Geocaches from JeoCache");
        for (Waypoint w : caches.getWaypoints()) {
            PointType point = objFactory.createPointType();
            point.getCoordinates().add(w.getWpt().getLon().toString() + "," + w.getWpt().getLat().toString());
            TimeStampType time = objFactory.createTimeStampType();
            time.setWhen(w.getWpt().getTime());
            PlacemarkType place = objFactory.createPlacemarkType();
            place.setName(w.getShortName(caches));
            if (cacheTypes.contains(w.getWptCache().getType())) {
                place.setStyleUrl("#" + w.getWptCache().getType());
            } else {
                place.setStyleUrl("#" + AppGlobals.TYPE_UNKNOWN);
            }
            place.setTimePrimitive(objFactory.createTimeStamp(time));
            place.setGeometry(objFactory.createPoint(point));
            StringBuffer desc = new StringBuffer();
            desc.append("<a href=\"");
            desc.append(w.getWpt().getUrl());
            desc.append("\">");
            desc.append(w.getWptCache().getName());
            desc.append("</a>");
            desc.append("<i>by ");
            desc.append(w.getWptCache().getPlacedBy());
            desc.append("</i><p> ");
            desc.append(w.getWptCache().getContainer());
            desc.append(" ");
            desc.append(w.getWptCache().getType());
            desc.append(" (");
            desc.append(w.getWptCache().getDifficulty());
            desc.append("/");
            desc.append(w.getWptCache().getTerrain());
            desc.append(")<p>");
            desc.append(w.getWptCache().getShortDescription().get(0).getValue());
            desc.append("<p>");
            desc.append(w.getWptCache().getLongDescription().get(0).getValue());
            place.setDescription(desc.toString());
            folder.getFeature().add(objFactory.createPlacemark(place));
        }
        DocumentType doc = objFactory.createDocumentType();
        doc.getStyleSelector().add(createStyle(AppGlobals.TYPE_TRADITIONAL, "2"));
        doc.getStyleSelector().add(createStyle(AppGlobals.TYPE_MULTI, "3"));
        doc.getStyleSelector().add(createStyle(AppGlobals.TYPE_VIRTUAL, "4"));
        doc.getStyleSelector().add(createStyle(AppGlobals.TYPE_LETTER, "5"));
        doc.getStyleSelector().add(createStyle(AppGlobals.TYPE_EVENT, "6"));
        doc.getStyleSelector().add(createStyle(AppGlobals.TYPE_UNKNOWN, "8"));
        doc.getFeature().add(objFactory.createFolder(folder));
        KmlType kml = objFactory.createKmlType();
        kml.setFeature(objFactory.createDocument(doc));
        FileOutputStream fos = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("jaxb.googleearth");
            OutputFormat of = new OutputFormat();
            String[] cdata = new String[] { "http://earth.google.com/kml/2.1^description" };
            of.setCDataElements(cdata);
            of.setPreserveSpace(false);
            of.setIndenting(true);
            of.setIndent(2);
            XMLSerializer serializer = new XMLSerializer(of);
            fos = new FileOutputStream(file);
            serializer.setOutputByteStream(fos);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(objFactory.createKml(kml), serializer.asContentHandler());
        } catch (Exception e) {
            AppGlobals.addMessageText(e, AppGlobals.MsgPriority.ERROR);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private static JAXBElement<StyleType> createStyle(String styleName, String image) {
        ObjectFactory objFactory = new ObjectFactory();
        IconStyleIconType icon = objFactory.createIconStyleIconType();
        icon.setHref("http://www.geocaching.com/images/kml/" + image + ".png");
        IconStyleType iconStyle = objFactory.createIconStyleType();
        iconStyle.setIcon(icon);
        StyleType style = objFactory.createStyleType();
        style.setIconStyle(iconStyle);
        style.setId(styleName);
        return objFactory.createStyle(style);
    }

    private static void saveFlyKmlFile(File file, Home home) {
        ObjectFactory objFactory = new ObjectFactory();
        LookAtType la = objFactory.createLookAtType();
        la.setLatitude(home.lat);
        la.setLongitude(home.lon);
        PointType point = objFactory.createPointType();
        point.getCoordinates().add(Double.toString(home.lon) + "," + Double.toString(home.lat) + ",30000.0");
        PlacemarkType place = objFactory.createPlacemarkType();
        place.setName("JeoCache Reference");
        place.setLookAt(la);
        place.setGeometry(objFactory.createPoint(point));
        DocumentType doc = objFactory.createDocumentType();
        doc.getFeature().add(objFactory.createPlacemark(place));
        KmlType kml = objFactory.createKmlType();
        kml.setFeature(objFactory.createDocument(doc));
        FileOutputStream fos = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance("jaxb.googleearth");
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", true);
            fos = new FileOutputStream(file);
            marshaller.marshal(objFactory.createKml(kml), fos);
        } catch (Exception e) {
            AppGlobals.addMessageText(e, AppGlobals.MsgPriority.ERROR);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Copy a file. This exists because File.renameTo() does not work properly
     * in windows
     */
    public static boolean copyFile(File source, File dest) {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = new FileInputStream(source).getChannel();
            dstChannel = new FileOutputStream(dest).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (srcChannel != null) {
                    srcChannel.close();
                }
            } catch (IOException e) {
            }
            try {
                if (dstChannel != null) {
                    dstChannel.close();
                }
            } catch (IOException e) {
            }
        }
        return true;
    }
}

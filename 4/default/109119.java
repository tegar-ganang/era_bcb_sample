import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;

public class CaptureDetails {

    private DataStore store = null;

    public CaptureDetails() {
    }

    public void writeCaptureDetails(CaptureTask task) {
        try {
            store = DataStore.getInstance();
            String pathString = store.getProperty("capture.path.details");
            if (pathString.trim().equalsIgnoreCase("none")) return;
            File outputPath = null;
            String capFileName = task.getCurrentFileName();
            File capFile = new File(capFileName);
            if (pathString.trim().equalsIgnoreCase("same")) {
                outputPath = new File(task.getCurrentFileName() + ".xml");
            } else {
                outputPath = new File(pathString + File.separator + capFile.getName() + ".xml");
            }
            if (outputPath.getParentFile().exists() == false) {
                System.out.println("ERROR details path does not exist: " + outputPath.toString());
                return;
            }
            System.out.println("Writing Capture Details info to: " + outputPath.toString());
            String xmlData = "";
            ScheduleItem schItem = task.getScheduleItem();
            xmlData = getXMLData(capFile, task.getDeviceIndex(), schItem);
            int count = 1;
            while (outputPath.exists()) {
                String newPath = outputPath.getCanonicalPath();
                newPath = newPath.substring(0, newPath.length() - 4) + "-" + (count++) + ".xml";
                System.out.println("Details file already exists, creating new name : " + newPath);
                outputPath = new File(newPath);
            }
            task.getScheduleItem().addCaptureFile(outputPath);
            FileWriter writer = new FileWriter(outputPath);
            writer.write(xmlData);
            writer.close();
        } catch (Exception e) {
            System.out.println("Error writing Capture Details XML file.");
            e.printStackTrace();
        }
    }

    private String getXMLData(File capFile, int deviceIndex, ScheduleItem schItem) {
        GuideStore guide = GuideStore.getInstance();
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            DOMImplementation di = db.getDOMImplementation();
            Document doc = di.createDocument("", "capture", null);
            Element root = doc.getDocumentElement();
            Element elm = null;
            Text text = null;
            elm = doc.createElement("ws_cardID");
            text = doc.createTextNode(new Integer(deviceIndex).toString());
            elm.appendChild(text);
            root.appendChild(elm);
            elm = doc.createElement("ws_filename");
            text = doc.createTextNode(capFile.getName());
            elm.appendChild(text);
            root.appendChild(elm);
            elm = doc.createElement("ws_fullfilename");
            text = doc.createTextNode(capFile.getCanonicalPath());
            elm.appendChild(text);
            root.appendChild(elm);
            ScheduleItem item = schItem;
            elm = doc.createElement("ws_name");
            text = doc.createTextNode(item.getName());
            elm.appendChild(text);
            root.appendChild(elm);
            elm = doc.createElement("ws_start");
            text = doc.createTextNode(item.getStart().toString());
            elm.appendChild(text);
            root.appendChild(elm);
            elm = doc.createElement("ws_stop");
            text = doc.createTextNode(item.getStop().toString());
            elm.appendChild(text);
            root.appendChild(elm);
            elm = doc.createElement("ws_duration");
            text = doc.createTextNode(new Integer(item.getDuration()).toString());
            elm.appendChild(text);
            root.appendChild(elm);
            elm = doc.createElement("ws_channel");
            text = doc.createTextNode(item.getChannel());
            elm.appendChild(text);
            root.appendChild(elm);
            GuideItem guideItem = item.getCreatedFrom();
            if (guideItem != null) {
                Element epgItem = doc.createElement("epg_item");
                root.appendChild(epgItem);
                String epgChan = guide.getEpgChannelFromMap(item.getChannel());
                elm = doc.createElement("epg_title");
                text = doc.createTextNode(guideItem.getName());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_subtitle");
                text = doc.createTextNode(guideItem.getSubName());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_description");
                text = doc.createTextNode(guideItem.getDescription());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_channel");
                text = doc.createTextNode(epgChan);
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_start");
                text = doc.createTextNode(guideItem.getStart().toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_stop");
                text = doc.createTextNode(guideItem.getStop().toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_duration");
                text = doc.createTextNode(new Integer(guideItem.getDuration()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                for (int x = 0; x < guideItem.getCategory().size(); x++) {
                    String epgCat = guideItem.getCategory().get(x);
                    elm = doc.createElement("epg_category");
                    text = doc.createTextNode(epgCat);
                    elm.appendChild(text);
                    epgItem.appendChild(elm);
                }
                elm = doc.createElement("epg_language");
                text = doc.createTextNode(guideItem.getLanguage());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_ratings");
                text = doc.createTextNode(guideItem.getRatings());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_widescreen");
                text = doc.createTextNode(new Boolean(guideItem.getWidescreen()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_highdef");
                text = doc.createTextNode(new Boolean(guideItem.getHighDef()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_repeat");
                text = doc.createTextNode(new Boolean(guideItem.getRepeat()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_premiere");
                text = doc.createTextNode(new Boolean(guideItem.getPremiere()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_live");
                text = doc.createTextNode(new Boolean(guideItem.getLive()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_interactive");
                text = doc.createTextNode(new Boolean(guideItem.getInteractive()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_blackwhite");
                text = doc.createTextNode(new Boolean(guideItem.getBlackWhite()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_surround");
                text = doc.createTextNode(new Boolean(guideItem.getSurround()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_ac3");
                text = doc.createTextNode(new Boolean(guideItem.getAC3()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_lastchance");
                text = doc.createTextNode(new Boolean(guideItem.getLastChance()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_captions");
                text = doc.createTextNode(new Boolean(guideItem.getCaptions()).toString());
                elm.appendChild(text);
                epgItem.appendChild(elm);
                elm = doc.createElement("epg_url");
                text = doc.createTextNode(guideItem.getURL());
                elm.appendChild(text);
                epgItem.appendChild(elm);
            }
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer xformer = factory.newTransformer();
            Source source = new DOMSource(doc);
            Result result = new StreamResult(buff);
            xformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buff.toString();
    }
}

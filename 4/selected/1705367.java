package de.michabrandt.timeview.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.w3c.css.sac.InputSource;
import de.michabrandt.timeview.common.Dialog;
import de.michabrandt.timeview.common.DialogChannel;
import de.michabrandt.timeview.common.DialogUnit;
import de.michabrandt.timeview.parser.css.CSSParser;
import de.michabrandt.timeview.parser.css.CSSTempStyle;

/**
 * @author Micha
 *
 */
public class TimeViewXMLParser {

    private static CSSParser parser;

    public TimeViewXMLParser() {
    }

    public static Dialog ParseFile(InputStream is) {
        parser = new CSSParser();
        SAXBuilder builder = new SAXBuilder();
        builder.setIgnoringElementContentWhitespace(true);
        Document doc;
        try {
            doc = builder.build(is);
            Element root = doc.getRootElement();
            if (root == null) return null;
            Dialog dialog = new Dialog();
            ParseHeader(root, dialog);
            ParseChannels(root, dialog);
            ParseTracks(root, dialog);
            dialog.build();
            if (dialog.getChannels().isEmpty()) dialog = null;
            return dialog;
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static void ParseHeader(Element root, Dialog dialog) {
        Element head = root.getChild("head");
        if (head == null) return;
        Element styleElmt = head.getChild("style");
        if (styleElmt != null) ParseStyleSheet(styleElmt, dialog);
    }

    protected static void ParseChannels(Element root, Dialog dialog) {
        Element channels = root.getChild("channels");
        if (channels == null) return;
        Iterator i = channels.getChildren("channel").iterator();
        while (i.hasNext()) {
            DialogChannel dchannel = ParseChannel((Element) i.next());
            if (dchannel != null) dialog.getChannels().add(dchannel);
        }
    }

    protected static DialogChannel ParseChannel(Element channel) {
        String s_start;
        String s_end;
        String s_text;
        String s_id = channel.getAttributeValue("id");
        String s_class = channel.getAttributeValue("class");
        String s_label = channel.getAttributeValue("label");
        List tokens = channel.getChildren("token");
        if (tokens == null) return null;
        DialogChannel dchannel = new DialogChannel();
        dchannel.setId(s_id);
        dchannel.setCls(s_class);
        dchannel.setLabel(s_label);
        CSSTempStyle channelStyle = parser.getDefaultStyle();
        if (s_class != null && parser.getClassStyle(s_class) != null) channelStyle = CSSTempStyle.deriveStyle(channelStyle, parser.getClassStyle(s_class));
        if (s_id != null && parser.getIdStyle(s_id) != null) channelStyle = CSSTempStyle.deriveStyle(channelStyle, parser.getIdStyle(s_id));
        ListIterator i = tokens.listIterator();
        while (i.hasNext()) {
            Element unit = (Element) i.next();
            s_start = unit.getAttributeValue("start");
            s_end = unit.getAttributeValue("end");
            s_id = unit.getAttributeValue("id");
            s_class = unit.getAttributeValue("class");
            s_text = unit.getTextNormalize();
            try {
                float start = Float.parseFloat(s_start);
                float end = Float.parseFloat(s_end);
                if (start >= 0.0 && end > start && s_text.length() > 0 && !s_text.contains("[silence]")) {
                    DialogUnit dunit = new DialogUnit(start, end, s_text, s_id, s_class);
                    CSSTempStyle unitStyle = new CSSTempStyle(channelStyle);
                    if (parser.getElementStyle("token") != null) unitStyle = CSSTempStyle.deriveStyle(unitStyle, parser.getElementStyle("token"));
                    if (s_class != null && parser.getClassStyle(s_class) != null) unitStyle = CSSTempStyle.deriveStyle(unitStyle, parser.getClassStyle(s_class));
                    if (s_id != null && parser.getIdStyle(s_id) != null) unitStyle = CSSTempStyle.deriveStyle(unitStyle, parser.getIdStyle(s_id));
                    dunit.setStyle(unitStyle.getUnitStyle());
                    dchannel.getUnits().add(dunit);
                }
            } catch (NumberFormatException e) {
            }
        }
        if (dchannel.getUnits().isEmpty()) return null; else return dchannel;
    }

    private static void ParseTracks(Element root, Dialog dialog) {
        Element e_tracks = root.getChild("tracks");
        if (e_tracks == null) return;
        List tracklist = e_tracks.getChildren("track");
        if (tracklist == null) return;
        for (ListIterator i = tracklist.listIterator(); i.hasNext(); ) {
            Element track = (Element) i.next();
            String fg_id = track.getAttributeValue("fg");
            String bg_id = track.getAttributeValue("bg");
            String label = track.getAttributeValue("label");
            if (fg_id != null || bg_id != null) dialog.defineTrack(fg_id, bg_id, label);
        }
    }

    private static void ParseStyleSheet(Element styleElmt, Dialog dialog) {
        String styleText = styleElmt.getText();
        Reader reader = new StringReader(styleText);
        InputSource is = new InputSource(reader);
        parser.parseStyleSheet(is);
    }
}

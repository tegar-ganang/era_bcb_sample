package be.lassi.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import be.lassi.cues.Cue;
import be.lassi.cues.CueChannelLevel;
import be.lassi.cues.CueSubmasterLevel;
import be.lassi.cues.Cues;
import be.lassi.cues.LightCueDetail;
import be.lassi.domain.Channel;
import be.lassi.domain.Dimmer;
import be.lassi.domain.FrameProperties;
import be.lassi.domain.Group;
import be.lassi.domain.Level;
import be.lassi.domain.Show;
import be.lassi.domain.Submaster;

/**
 * Writes a <code>Show</code> object in xml format.
 */
public class XmlShowWriter {

    private final XmlWriter out;

    private final Show show;

    public XmlShowWriter(final Writer out, final Show show) {
        this.out = new XmlWriter(out);
        this.show = show;
    }

    /**
     * {@inheritDoc}
     */
    public void write() throws IOException {
        out.openElement("show");
        out.addAttribute("name", show.getName());
        writeFixtures();
        writePatch();
        writeSubmasters();
        writeGroups();
        writeCueLists();
        writeFrameProperties();
        out.closeElement();
    }

    private void writeComment(final String comment) throws IOException {
        out.openElement("comment");
        if (comment.length() > 0) {
            List<String> lines = new ArrayList<String>();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < comment.length(); i++) {
                Character c = comment.charAt(i);
                if (c == '\n') {
                    lines.add(b.toString());
                    b.setLength(0);
                } else {
                    b.append(c);
                }
            }
            lines.add(b.toString());
            for (String line : lines) {
                out.openElement("line");
                out.addAttribute("text", line);
                out.closeElement();
            }
        }
        out.closeElement();
    }

    private void writeCueList() throws IOException {
        out.openElement("cue-list");
        out.addAttribute("id", 1);
        out.addAttribute("name", "Single cuelist");
        Cues cues = show.getCues();
        writeCueListCues(cues);
        out.closeElement();
    }

    private void writeCueListCues(final Cues cues) throws IOException {
        for (Cue cue : cues) {
            out.openElement("cue");
            out.addAttribute("number", cue.getNumber());
            out.addAttribute("page", cue.getPage());
            out.addAttribute("prompt", "");
            out.addAttribute("description", cue.getDescription());
            writeComment(cue.getPrompt());
            if (cue.isLightCue()) {
                int lightCueIndex = cue.getLightCueIndex();
                LightCueDetail detail = show.getCues().getLightCues().getDetail(lightCueIndex);
                writeLightCueSubmasters(detail);
                writeLightCueChannels(detail);
            }
            out.closeElement();
        }
    }

    private void writeCueLists() throws IOException {
        out.openElement("cue-lists");
        writeCueList();
        out.closeElement();
    }

    private void writeFixtures() throws IOException {
        out.openElement("fixtures");
        for (Channel channel : show.getChannels()) {
            out.openElement("fixture");
            out.addAttribute("id", channel.getId() + 1);
            out.addAttribute("name", channel.getName());
            out.closeElement();
        }
        out.closeElement();
    }

    private void writeFrameProperties() throws IOException {
        out.openElement("windows");
        for (FrameProperties properties : show.getFrameProperties()) {
            out.openElement("window");
            out.addAttribute("id", properties.getId());
            out.addAttribute("x", properties.getBounds().x);
            out.addAttribute("y", properties.getBounds().y);
            out.addAttribute("width", properties.getBounds().width);
            out.addAttribute("height", properties.getBounds().height);
            out.addAttribute("visible", properties.isVisible());
            out.closeElement();
        }
        out.closeElement();
    }

    private void writeGroups() throws IOException {
        out.openElement("groups");
        for (Group group : show.getGroups()) {
            out.openElement("group");
            out.addAttribute("name", group.getName());
            writeComment("");
            out.openElement("fixtures");
            for (Channel channel : group.getChannels()) {
                out.openElement("fixture");
                out.addAttribute("id", channel.getId() + 1);
                out.closeElement();
            }
            out.closeElement();
            out.closeElement();
        }
        out.closeElement();
    }

    private void writeLightCueChannels(final LightCueDetail detail) throws IOException {
        out.openElement("scene");
        for (int i = 0; i < detail.getNumberOfChannels(); i++) {
            CueChannelLevel level = detail.getChannelLevel(i);
            if (level.getChannelLevelValue().isActive()) {
                out.openElement("set");
                out.addAttribute("fixture-id", i + 1);
                String value = level.isDerived() ? "derived" : "" + level.getChannelIntValue();
                out.addAttribute("attribute", "Intensity");
                out.addAttribute("value", value);
                out.closeElement();
            }
        }
        out.closeElement();
    }

    private void writeLightCueSubmasters(final LightCueDetail detail) throws IOException {
        out.openElement("submasters");
        for (int i = 0; i < detail.getNumberOfSubmasters(); i++) {
            CueSubmasterLevel level = detail.getSubmasterLevel(i);
            if (level.getLevelValue().isActive()) {
                out.openElement("submaster");
                out.addAttribute("id", i + 1);
                String value = level.isDerived() ? "derived" : "" + level.getIntValue();
                out.addAttribute("value", value);
                out.closeElement();
            }
        }
        out.closeElement();
    }

    private void writePatch() throws IOException {
        out.openElement("patch-lines");
        for (Dimmer dimmer : show.getDimmers()) {
            out.openElement("patch-line");
            out.addAttribute("id", dimmer.getId() + 1);
            out.addAttribute("name", dimmer.getName());
            if (dimmer.getChannelId() != -1) {
                out.addAttribute("fixture-id", dimmer.getChannelId() + 1);
            }
            out.closeElement();
        }
        out.closeElement();
    }

    private void writeSubmaster(final Submaster submaster) throws IOException {
        out.openElement("submaster");
        out.addAttribute("id", submaster.getId() + 1);
        out.addAttribute("name", submaster.getName());
        writeComment("");
        writeSubmasterLevels(submaster);
        out.closeElement();
    }

    private void writeSubmasterLevels(final Submaster submaster) throws IOException {
        out.openElement("scene");
        for (int channelIndex = 0; channelIndex < submaster.getNumberOfLevels(); channelIndex++) {
            Level level = submaster.getLevel(channelIndex);
            if (level.isActive()) {
                out.openElement("set");
                out.addAttribute("fixture-id", channelIndex + 1);
                out.addAttribute("attribute", "Intensity");
                out.addAttribute("value", level.getIntValue());
                out.closeElement();
            }
        }
        out.closeElement();
    }

    private void writeSubmasters() throws IOException {
        out.openElement("submasters");
        for (Submaster submaster : show.getSubmasters()) {
            writeSubmaster(submaster);
        }
        out.closeElement();
    }
}
